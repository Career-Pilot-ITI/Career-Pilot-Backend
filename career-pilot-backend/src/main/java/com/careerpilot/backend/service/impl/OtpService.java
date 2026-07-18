package com.careerpilot.backend.service.impl;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.careerpilot.backend.controller.advice.AuthException.InvalidOtpException;
import com.careerpilot.backend.controller.advice.AuthException.OtpCooldownException;
import com.careerpilot.backend.controller.advice.AuthException.OtpExpiredException;
import com.careerpilot.backend.controller.advice.AuthException.OtpLockoutException;
import com.careerpilot.backend.controller.advice.AuthException.OtpResendLimitException;
import com.careerpilot.backend.service.IOtpService;
import com.careerpilot.backend.utils.WireWebService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OtpService implements IOtpService {

  private final RedisTemplate<String, String> redisTemplate;
  private final ObjectMapper objectMapper;
  private final WireWebService wireWebService;

  @Value("${app.otp.provider:simulated}")
  private String otpProvider;

  private static final Logger log = LoggerFactory.getLogger(OtpService.class);

  private static final String OTP_PREFIX = "otp:";
  private static final String COOLDOWN_PREFIX = "otp_cooldown:";
  private static final String LOCKOUT_PREFIX = "otp_lockout:";
  private static final String FAILED_PREFIX = "otp_failed:";
  private static final String RESEND_PREFIX = "otp_resend:";
  private static final long OTP_TTL_SECONDS = 300;
  private static final long COOLDOWN_TTL_SECONDS = 60;
  private static final long LOCKOUT_TTL_SECONDS = 900;
  private static final long FAILED_TTL_SECONDS = 900;
  private static final long RESEND_WINDOW_SECONDS = 900;
  private static final int MAX_ATTEMPTS = 3;
  private static final int MAX_RESENDS = 3;
  private static final int MAX_FAILED_ATTEMPTS = 5;

  @Override
  public void sendPhoneOtp(String phoneNumber) throws OtpCooldownException {
    if (redisTemplate.hasKey(LOCKOUT_PREFIX + phoneNumber)) {
      throw new OtpLockoutException("Too many failed attempts. Try again later.");
    }

    if (redisTemplate.hasKey(COOLDOWN_PREFIX + phoneNumber)) {
      throw new OtpCooldownException(
          "Please wait " + COOLDOWN_TTL_SECONDS + " seconds before sending another OTP.");
    }

    Long resendCount = redisTemplate.opsForValue().increment(RESEND_PREFIX + phoneNumber);
    if (resendCount == null) {
      resendCount = 1L;
    }
    redisTemplate.expire(RESEND_PREFIX + phoneNumber, RESEND_WINDOW_SECONDS, TimeUnit.SECONDS);
    if (resendCount > MAX_RESENDS) {
      throw new OtpResendLimitException("Maximum resend limit reached. Try again later.");
    }

    String code = String.valueOf(ThreadLocalRandom.current().nextInt(100000, 999999));

    try {
      String otpData = objectMapper.writeValueAsString(Map.of(
          "code", code,
          "attempts", 0,
          "expiresAt", System.currentTimeMillis() + OTP_TTL_SECONDS * 1000));

      redisTemplate.opsForValue().set(OTP_PREFIX + phoneNumber, otpData, OTP_TTL_SECONDS, TimeUnit.SECONDS);
      redisTemplate.opsForValue().set(COOLDOWN_PREFIX + phoneNumber, "1", COOLDOWN_TTL_SECONDS, TimeUnit.SECONDS);
    } catch (Exception e) {
      throw new RuntimeException("Failed to store OTP", e);
    }

    if ("wireweb".equalsIgnoreCase(otpProvider)) {
      wireWebService.sendOtp(phoneNumber, code);
    } else {
      log.info("OTP for {}: {}", phoneNumber, code);
    }
  }

  @Override
  public String verifyPhoneOtp(String phoneNumber, String code) {
    if (redisTemplate.hasKey(LOCKOUT_PREFIX + phoneNumber)) {
      throw new OtpLockoutException("Too many failed attempts. Try again later.");
    }

    String key = OTP_PREFIX + phoneNumber;
    String stored = redisTemplate.opsForValue().get(key);

    if (stored == null) {
      throw new OtpExpiredException("OTP has expired or was not requested.");
    }

    Map<String, Object> otpData;
    try {
      otpData = objectMapper.readValue(stored, new TypeReference<Map<String, Object>>() {
      });
    } catch (Exception e) {
      redisTemplate.delete(key);
      throw new RuntimeException("Failed to parse OTP data", e);
    }

    int attempts = (int) otpData.getOrDefault("attempts", 0);

    if (attempts >= MAX_ATTEMPTS) {
      redisTemplate.delete(key);
      throw new InvalidOtpException("Too many failed attempts. Request a new OTP.");
    }

    if (!code.equals(otpData.get("code"))) {
      otpData.put("attempts", attempts + 1);
      try {
        redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(otpData),
            redisTemplate.getExpire(key, TimeUnit.SECONDS), TimeUnit.SECONDS);
      } catch (Exception e) {
        throw new RuntimeException("Failed to update OTP attempts", e);
      }

      Long failedCount = redisTemplate.opsForValue().increment(FAILED_PREFIX + phoneNumber);
      redisTemplate.expire(FAILED_PREFIX + phoneNumber, FAILED_TTL_SECONDS, TimeUnit.SECONDS);
      if (failedCount != null && failedCount >= MAX_FAILED_ATTEMPTS) {
        redisTemplate.opsForValue().set(LOCKOUT_PREFIX + phoneNumber, "1",
            LOCKOUT_TTL_SECONDS, TimeUnit.SECONDS);
        redisTemplate.delete(FAILED_PREFIX + phoneNumber);
        redisTemplate.delete(RESEND_PREFIX + phoneNumber);
      }

      throw new InvalidOtpException("Invalid OTP code.");
    }

    redisTemplate.delete(key);
    redisTemplate.delete(FAILED_PREFIX + phoneNumber);
    redisTemplate.delete(LOCKOUT_PREFIX + phoneNumber);
    redisTemplate.delete(RESEND_PREFIX + phoneNumber);
    return phoneNumber;
  }
}
