package com.careerpilot.backend.aspect;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import com.careerpilot.backend.utils.PiiRedactionUtil;
import com.careerpilot.backend.utils.PiiRedactionUtil.RedactionResult;

@Aspect
@Component
public class PiiRedactionAspect {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Around("@annotation(com.careerpilot.backend.aspect.RedactPii)")
  public Object redactStringParams(ProceedingJoinPoint pjp) throws Throwable {
    Object[] args = pjp.getArgs();
    List<RedactionResult> results = new ArrayList<>();

    for (int i = 0; i < args.length; i++) {
      if (args[i] instanceof String s) {
        RedactionResult result = PiiRedactionUtil.redactWithIndex(s);
        args[i] = result.redactedContent();
        results.add(result);
      } else {
        results.add(null);
      }
    }

    Object response = pjp.proceed(args);

    boolean needsRestore = results.stream().anyMatch(r -> r != null);
    if (!needsRestore || response == null) {
      return response;
    }

    String json = objectMapper.writeValueAsString(response);
    for (RedactionResult result : results) {
      if (result != null) {
        json = result.restore(json);
      }
    }

    JavaType returnType = objectMapper.getTypeFactory()
        .constructType(((MethodSignature) pjp.getSignature()).getMethod().getGenericReturnType());
    return objectMapper.readValue(json, returnType);
  }
}
