package com.careerpilot.backend.event;

import com.careerpilot.backend.service.impl.CvOptimizationJobExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class CvOptimizationEventListener {

    private final CvOptimizationJobExecutor cvOptimizationJobExecutor;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCvOptimizationRequested(CvOptimizationRequestedEvent event) {
        log.info("After commit, kicking off CV optimization job {}", event.jobId());
        cvOptimizationJobExecutor.executeOptimization(
            event.jobId(), event.userId(), event.rawCvText(), event.job(), event.coinCost());
    }
}
