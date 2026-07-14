package com.careerpilot.backend.service.impl;

import com.careerpilot.backend.controller.advice.PaymentException;
import com.careerpilot.backend.service.IPaymentProvider;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class PaymentProviderResolver {

    private final Map<String, IPaymentProvider> providers;

    public PaymentProviderResolver(Map<String, IPaymentProvider> providers) {
        this.providers = providers;
    }

    public IPaymentProvider resolve(String providerKey) {
        IPaymentProvider provider = providers.get(providerKey);
        if (provider == null) {
            throw new PaymentException.UnsupportedPaymentProviderException(
                    "No payment provider registered for: " + providerKey);
        }
        return provider;
    }
}