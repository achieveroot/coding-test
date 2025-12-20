package com.seowon.coding.service;

import com.seowon.coding.domain.model.ProcessingStatus;
import com.seowon.coding.domain.repository.ProcessingStatusRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProcessService {

    private final ProcessingStatusRepository processingStatusRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void startJobRequiresNew(String jobId, int total) {
        ProcessingStatus ps = processingStatusRepository.findByJobId(jobId)
                .orElseGet(() -> ProcessingStatus.builder().jobId(jobId).build());
        ps.markRunning(total);
        processingStatusRepository.save(ps);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateProgressRequiresNew(String jobId, int processed, int total) {
        ProcessingStatus ps = processingStatusRepository.findByJobId(jobId)
                .orElseThrow(() -> new IllegalStateException("ProcessingStatus not found: " + jobId));

        ps.updateProgress(processed, total);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailedRequiresNew(String jobId) {
        ProcessingStatus ps = processingStatusRepository.findByJobId(jobId)
                .orElseThrow(() -> new IllegalStateException("ProcessingStatus not found: " + jobId));

        ps.markFailed();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markCompletedRequiresNew(String jobId) {
        ProcessingStatus ps = processingStatusRepository.findByJobId(jobId)
                .orElseThrow(() -> new IllegalStateException("ProcessingStatus not found: " + jobId));

        ps.markCompleted();
    }
}
