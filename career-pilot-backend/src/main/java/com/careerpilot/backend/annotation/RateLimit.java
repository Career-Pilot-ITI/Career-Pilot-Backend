package com.careerpilot.backend.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    long capacity() default 5;

    long refillTokens() default 5;

    long refillSeconds() default 60;

    String key() default "";
}
