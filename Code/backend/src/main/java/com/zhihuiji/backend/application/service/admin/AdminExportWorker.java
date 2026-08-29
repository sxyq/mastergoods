package com.zhihuiji.backend.application.service.admin;

import com.zhihuiji.backend.domain.entity.AdminExportJobEntity;
import com.zhihuiji.backend.infrastructure.repository.admin.AdminExportJobRepository;
import java.util.List;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Claims and processes bounded export jobs outside the HTTP request thread. */
@Component
public class AdminExportWorker {
    private final AdminExportJobRepository jobRepository;
    private final AdminExportService exportService;

    public AdminExportWorker(AdminExportJobRepository jobRepository, AdminExportService exportService) {
        this.jobRepository = jobRepository;
        this.exportService = exportService;
    }

    @Scheduled(
        fixedDelayString = "${admin.export.worker.fixed-delay-ms:1000}",
        initialDelayString = "${admin.export.worker.initial-delay-ms:5000}"
    )
    @Transactional
    public void processPending() {
        List<AdminExportJobEntity> jobs = jobRepository.findTop20ByStatusOrderByCreatedAtAsc("PENDING");
        if (jobs == null) return;
        for (AdminExportJobEntity job : jobs) {
            if (job == null || job.getExportId() == null) continue;
            if (jobRepository.claimPending(job.getExportId()) == 1) {
                exportService.processClaimed(job.getExportId());
            }
        }
    }
}
