package com.careerpilot.backend.security.jwt;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class RefreshTokenService {

  private static final String TOKEN_PREFIX = "refresh:";
  private static final String USER_PREFIX = "user_refresh:";
  private static final long REFRESH_EXPIRY_MS = 7 * 24 * 60 * 60 * 1000L;

  private final RedisTemplate<String, String> redisTemplate;

  public RefreshTokenService(RedisTemplate<String, String> redisTemplate) {
    this.redisTemplate = redisTemplate;
  }

  public String generateRefreshToken(String email) {
    String token = UUID.randomUUID().toString();
    redisTemplate.opsForValue().set(TOKEN_PREFIX + token, email, REFRESH_EXPIRY_MS, TimeUnit.MILLISECONDS);
    redisTemplate.opsForSet().add(USER_PREFIX + email, token);
    redisTemplate.expire(USER_PREFIX + email, REFRESH_EXPIRY_MS, TimeUnit.MILLISECONDS);
    return token;
  }

  public Optional<String> validateAndRotate(String refreshToken) {
    String key = TOKEN_PREFIX + refreshToken;
    String email = redisTemplate.opsForValue().getAndDelete(key);
    if (email == null)
      return Optional.empty();
    redisTemplate.opsForSet().remove(USER_PREFIX + email, refreshToken);
    return Optional.of(email);
  }

  public void revokeAllForUser(String email) {
    Set<String> tokens = redisTemplate.opsForSet().members(USER_PREFIX + email);
    if (tokens != null) {
      for (String t : tokens) {
        redisTemplate.delete(TOKEN_PREFIX + t);
      }
    }
    redisTemplate.delete(USER_PREFIX + email);
  }
}
