package com.careerpilot.backend.service;

import com.careerpilot.backend.controller.advice.AuthException.OtpCooldownException;
import com.careerpilot.backend.controller.advice.AuthException.OtpExpiredException;

public interface IOtpService {
  void sendPhoneOtp(String phoneNumber) throws OtpCooldownException, OtpExpiredException;

  String verifyPhoneOtp(String phoneNumber, String code);
}
