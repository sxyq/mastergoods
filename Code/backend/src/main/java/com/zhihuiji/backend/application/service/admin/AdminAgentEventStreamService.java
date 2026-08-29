package com.zhihuiji.backend.application.service.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhihuiji.backend.api.dto.admin.AdminAgentDtos;
import com.zhihuiji.backend.infrastructure.security.admin.AdminPrincipal;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.Locale;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Persistent Agent event stream for administrator observability.
 *
 * <p>The initial replay is authorized and loaded by the controller before this
 * service is called. Subsequent polls use the same detail service and sequence
 * cursor, so reconnects and multiple application instances do not depend on an
 * in-memory event bus.
 */
@Service
public class AdminAgentEventStreamService {
    private static final long STREAM_TIMEOUT_MS = 30_000L;
    private static final long POLL_INTERVAL_MS = 750L;
    private static final int STREAM_THREADS = 2;

    private final AdminAgentDetailService detailService;
    private final ObjectMapper objectMapper;
    private final ScheduledExecutorService pollExecutor;

    @Autowired
    public AdminAgentEventStreamService(AdminAgentDetailService detailService, ObjectMapper objectMapper) {
        this.detailService = detailService;
        this.objectMapper = objectMapper;
        this.pollExecutor = Executors.newScheduledThreadPool(STREAM_THREADS, namedThreadFactory("admin-agent-stream"));
    }

    @PreDestroy
    public void shutdown() {
        pollExecutor.shutdownNow();
    }

    /**
     * Opens a stream after the controller has performed the first authorized
     * query. A terminal event completes immediately; otherwise the stream keeps
     * polling persisted events until the emitter lifecycle ends.
     */
    public SseEmitter open(
        AdminPrincipal principal,
        String runId,
        Integer afterSequence,
        boolean includeContent,
        Long ownerUserId,
        Long storeId,
        AdminAgentDtos.EventPage initialPage
    ) {
        SseEmitter emitter = new SseEmitter(STREAM_TIMEOUT_MS);
        AtomicBoolean closed = new AtomicBoolean(false);
        AtomicInteger cursor = new AtomicInteger(Math.max(0, afterSequence == null ? 0 : afterSequence));
        AtomicReference<ScheduledFuture<?>> scheduled = new AtomicReference<>();

        Runnable cancelPoll = () -> {
            ScheduledFuture<?> task = scheduled.getAndSet(null);
            if (task != null) {
                task.cancel(false);
            }
        };
        Runnable markClosed = () -> {
            if (closed.compareAndSet(false, true)) {
                cancelPoll.run();
            } else {
                cancelPoll.run();
            }
        };
        emitter.onCompletion(markClosed);
        emitter.onTimeout(() -> {
            markClosed.run();
            completeQuietly(emitter);
        });
        emitter.onError(ignored -> markClosed.run());

        try {
            if (sendPage(emitter, initialPage, cursor)) {
                markClosed.run();
                completeQuietly(emitter);
                return emitter;
            }
            ScheduledFuture<?> task = pollExecutor.scheduleWithFixedDelay(
                () -> poll(emitter, closed, cursor, scheduled, principal, runId, includeContent, ownerUserId, storeId),
                POLL_INTERVAL_MS,
                POLL_INTERVAL_MS,
                TimeUnit.MILLISECONDS
            );
            if (!scheduled.compareAndSet(null, task)) {
                task.cancel(false);
            }
            if (closed.get()) {
                cancelPoll.run();
            }
        } catch (Exception ex) {
            markClosed.run();
            emitter.completeWithError(ex);
        }
        return emitter;
    }

    private void poll(
        SseEmitter emitter,
        AtomicBoolean closed,
        AtomicInteger cursor,
        AtomicReference<ScheduledFuture<?>> scheduled,
        AdminPrincipal principal,
        String runId,
        boolean includeContent,
        Long ownerUserId,
        Long storeId
    ) {
        if (closed.get()) {
            cancel(scheduled);
            return;
        }
        try {
            AdminAgentDtos.EventPage page = detailService.events(
                principal, runId, cursor.get(), includeContent, ownerUserId, storeId
            );
            if (sendPage(emitter, page, cursor)) {
                if (closed.compareAndSet(false, true)) {
                    cancel(scheduled);
                    completeQuietly(emitter);
                }
            }
        } catch (Exception ex) {
            if (closed.compareAndSet(false, true)) {
                cancel(scheduled);
                emitter.completeWithError(ex);
            }
        }
    }

    /** Sends only events newer than the cursor; sorted persistence makes this a deduplication boundary. */
    private boolean sendPage(SseEmitter emitter, AdminAgentDtos.EventPage page, AtomicInteger cursor) throws IOException {
        if (page == null || page.items() == null) {
            return false;
        }
        emitter.send(SseEmitter.event()
            .name("stream_integrity")
            .data(objectMapper.writeValueAsString(Map.of(
                "event_integrity", page.eventIntegrity(),
                "after_sequence", cursor.get()
            ))));
        boolean terminal = false;
        for (AdminAgentDtos.Event event : page.items()) {
            if (event == null || event.sequence() <= cursor.get()) {
                continue;
            }
            emitter.send(SseEmitter.event()
                .id(Long.toString(event.sequence()))
                .name(event.eventType())
                .data(objectMapper.writeValueAsString(event)));
            cursor.set(event.sequence() > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) event.sequence());
            if (isTerminalEvent(event.eventType())) {
                terminal = true;
                break;
            }
        }
        return terminal;
    }

    private void cancel(AtomicReference<ScheduledFuture<?>> scheduled) {
        ScheduledFuture<?> task = scheduled.getAndSet(null);
        if (task != null) {
            task.cancel(false);
        }
    }

    private void completeQuietly(SseEmitter emitter) {
        try {
            emitter.complete();
        } catch (IllegalStateException ignored) {
            // The client may have closed the connection between the poll and completion.
        }
    }

    /** Shared terminal-event predicate for the persistent stream and controller fallback. */
    public static boolean isTerminalEvent(String eventType) {
        if (eventType == null) return false;
        String value = eventType.toLowerCase(Locale.ROOT);
        return value.equals("run_completed") || value.equals("run_failed")
            || value.equals("run_blocked") || value.equals("run_cancelled")
            || value.equals("run_exhausted") || value.endsWith(".completed")
            || value.endsWith(".failed") || value.endsWith(".blocked")
            || value.endsWith(".cancelled") || value.endsWith(".exhausted")
            || value.endsWith("_completed") || value.endsWith("_failed")
            || value.endsWith("_blocked") || value.endsWith("_cancelled")
            || value.endsWith("_exhausted");
    }

    private static ThreadFactory namedThreadFactory(String prefix) {
        AtomicInteger sequence = new AtomicInteger(1);
        return runnable -> {
            Thread thread = new Thread(runnable, prefix + "-" + sequence.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        };
    }
}
