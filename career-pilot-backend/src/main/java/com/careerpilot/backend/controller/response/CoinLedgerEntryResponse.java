package com.careerpilot.backend.controller.response;

import com.careerpilot.backend.entity.CoinLedgerEntry;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CoinLedgerEntryResponse {
    private int amount;
    private String reason;
    private String referenceId;
    private LocalDateTime createdAt;

    public static CoinLedgerEntryResponse from(CoinLedgerEntry entry) {
        CoinLedgerEntryResponse r = new CoinLedgerEntryResponse();
        r.setAmount(entry.getAmount());
        r.setReason(entry.getReason().toString());
        r.setReferenceId(entry.getReferenceId());
        r.setCreatedAt(entry.getCreatedAt());
        return r;
    }
}