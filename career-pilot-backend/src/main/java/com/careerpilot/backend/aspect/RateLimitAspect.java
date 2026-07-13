package com.careerpilot.backend.aspect;

import com.careerpilot.backend.annotation.RateLimit;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.function.Supplier;

@Aspect
@Component
public class RateLimitAspect {

    private static final String RATE_LIMIT_PREFIX = "ratelimit:";

    private final ProxyManager<String> proxyManager;
    private final ExpressionParser parser = new SpelExpressionParser();
    private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    public RateLimitAspect(ProxyManager<String> proxyManager) {
        this.proxyManager = proxyManager;
    }

    @Before("@annotation(rateLimit)")
    public void checkRateLimit(JoinPoint joinPoint, RateLimit rateLimit) {
        String key = resolveKey(joinPoint, rateLimit);
        String bucketId = RATE_LIMIT_PREFIX + key;

        Bandwidth limit = Bandwidth.builder()
                .capacity(rateLimit.capacity())
                .refillGreedy(rateLimit.refillTokens(), Duration.ofSeconds(rateLimit.refillSeconds()))
                .build();


        Supplier<BucketConfiguration> configSupplier = ()-> BucketConfiguration.builder().addLimit(limit).build();
        Bucket bucket = proxyManager.builder().build(bucketId, configSupplier);

        if (!bucket.tryConsume(1)) {
            throw new RateLimitExceededException("Rate limit exceeded. Try again later.");
        }
    }

    private String resolveKey(JoinPoint joinPoint, RateLimit rateLimit) {
        if (!rateLimit.key().isBlank()) {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            String[] paramNames = parameterNameDiscoverer.getParameterNames(method);
            Object[] args = joinPoint.getArgs();

            StandardEvaluationContext context = new StandardEvaluationContext();
            if (paramNames != null) {
                for (int i = 0; i < paramNames.length && i < args.length; i++) {
                    context.setVariable(paramNames[i], args[i]);
                }
            }

            Expression expression = parser.parseExpression(rateLimit.key());
            Object value = expression.getValue(context);
            return method.getName() + ":" + (value != null ? value : "");
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            return auth.getName();
        }
        return ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                .getRequest().getRemoteAddr();
    }
}
