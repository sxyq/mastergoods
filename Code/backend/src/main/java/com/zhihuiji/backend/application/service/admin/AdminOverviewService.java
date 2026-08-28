package com.zhihuiji.backend.application.service.admin;

import com.zhihuiji.backend.api.dto.admin.AdminOverviewDtos;
import com.zhihuiji.backend.api.dto.admin.AdminScopeDtos;
import com.zhihuiji.backend.infrastructure.repository.admin.AdminOverviewQueryRepository;
import com.zhihuiji.backend.infrastructure.repository.admin.AdminScopeQuery;
import com.zhihuiji.backend.infrastructure.security.admin.AdminAuthorizationService;
import com.zhihuiji.backend.infrastructure.security.admin.AdminDataScope;
import com.zhihuiji.backend.infrastructure.security.admin.AdminPermission;
import com.zhihuiji.backend.infrastructure.security.admin.AdminPrincipal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Owner-bound platform metrics for API-ADM-02. */
@Service
public class AdminOverviewService {
    private static final Duration DEFAULT_WINDOW = Duration.ofDays(30);
    private static final Duration MAX_WINDOW = Duration.ofDays(90);
    private static final Duration TREND_BUCKET = Duration.ofDays(1);

    private final AdminAuthorizationService authorizationService;
    private final AdminOverviewQueryRepository overviewQueryRepository;

    public AdminOverviewService(
        AdminAuthorizationService authorizationService,
        AdminOverviewQueryRepository overviewQueryRepository
    ) {
        this.authorizationService = authorizationService;
        this.overviewQueryRepository = overviewQueryRepository;
    }

    @Transactional(readOnly = true)
    public AdminOverviewDtos.OverviewResponse overview(
        AdminPrincipal principal,
        Instant requestedFrom,
        Instant requestedTo,
        Long requestedOwnerUserId,
        Long requestedStoreId
    ) {
        AdminDataScope scope = authorizationService.authorize(
            principal,
            AdminPermission.DASHBOARD_READ,
            requestedOwnerUserId,
            requestedStoreId
        );
        if (!scope.allOwners() && !scope.storeIds().isEmpty()) {
            throw new IllegalStateException("Agent store scope is unavailable in persisted run audits");
        }
        TimeRange range = timeRange(requestedFrom, requestedTo);
        AdminScopeQuery queryScope = AdminScopeQuery.from(scope);
        long fromAt = range.from().toEpochMilli();
        long toAt = range.to().toEpochMilli();
        List<AdminOverviewDtos.Metric> metrics = List.of(
            new AdminOverviewDtos.Metric(
                "users",
                overviewQueryRepository.countUsers(
                    queryScope.allOwners(), queryScope.ownerUserIds(), queryScope.storeIds(), queryScope.allStores()
                ),
                "count"
            ),
            new AdminOverviewDtos.Metric(
                "stores",
                overviewQueryRepository.countStores(
                    queryScope.allOwners(), queryScope.ownerUserIds(), queryScope.storeIds(), queryScope.allStores()
                ),
                "count"
            ),
            new AdminOverviewDtos.Metric(
                "agent_runs",
                overviewQueryRepository.countAgentRuns(
                    queryScope.allOwners(), queryScope.ownerUserIds(), fromAt, toAt
                ),
                "count"
            ),
            new AdminOverviewDtos.Metric(
                "agent_tool_calls",
                overviewQueryRepository.sumAgentToolCount(
                    queryScope.allOwners(), queryScope.ownerUserIds(), fromAt, toAt
                ),
                "count"
            )
        );
        return new AdminOverviewDtos.OverviewResponse(
            range.from(),
            range.to(),
            metrics,
            trend(queryScope, range),
            false,
            "COMPLETE",
            Instant.now(),
            AdminScopeDtos.Scope.from(scope)
        );
    }

    private List<AdminOverviewDtos.TrendPoint> trend(AdminScopeQuery scope, TimeRange range) {
        List<AdminOverviewDtos.TrendPoint> points = new ArrayList<>();
        Instant bucket = range.from();
        while (bucket.isBefore(range.to())) {
            Instant next = bucket.plus(TREND_BUCKET);
            if (next.isAfter(range.to())) {
                next = range.to();
            }
            points.add(new AdminOverviewDtos.TrendPoint(
                bucket,
                overviewQueryRepository.countAgentRuns(
                    scope.allOwners(),
                    scope.ownerUserIds(),
                    bucket.toEpochMilli(),
                    next.toEpochMilli()
                )
            ));
            bucket = next;
        }
        return points;
    }

    private TimeRange timeRange(Instant from, Instant to) {
        Instant normalizedTo = to == null ? Instant.now() : to;
        Instant normalizedFrom = from == null ? normalizedTo.minus(DEFAULT_WINDOW) : from;
        if (!normalizedFrom.isBefore(normalizedTo)) {
            throw new IllegalArgumentException("from must be before to");
        }
        if (Duration.between(normalizedFrom, normalizedTo).compareTo(MAX_WINDOW) > 0) {
            throw new IllegalArgumentException("time range is too wide");
        }
        return new TimeRange(normalizedFrom, normalizedTo);
    }

    private record TimeRange(Instant from, Instant to) {}
}
