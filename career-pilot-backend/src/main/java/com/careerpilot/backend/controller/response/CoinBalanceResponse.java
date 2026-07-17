package com.careerpilot.backend.controller.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CoinBalanceResponse {
    private int balance;
}