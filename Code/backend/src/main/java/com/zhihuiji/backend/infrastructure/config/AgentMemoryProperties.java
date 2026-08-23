package com.zhihuiji.backend.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Agent 长期记忆相关配置。
 *
 * <p>记忆默认关闭自动学习，避免在用户未授权的情况下写入跨会话记忆。
 * 通过 {@code agent.memory.enabled=true} 显式开启。
 */
@Component
@ConfigurationProperties(prefix = "agent.memory")
public class AgentMemoryProperties {

    /** 是否启用记忆自动提取与召回，默认关闭。 */
    private boolean enabled = false;

    /** 单次召回的默认条数上限。 */
    private int defaultRecallLimit = 5;

    /** 单次召回的硬上限，避免过大注入挤占上下文。 */
    private int maxRecallLimit = 10;

    /** 记忆 summary 字段最大字符数。 */
    private int maxSummaryLength = 500;

    /** 记忆 recall_text 字段最大字符数。 */
    private int maxRecallTextLength = 4000;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getDefaultRecallLimit() {
        return defaultRecallLimit;
    }

    public void setDefaultRecallLimit(int defaultRecallLimit) {
        this.defaultRecallLimit = defaultRecallLimit;
    }

    public int getMaxRecallLimit() {
        return maxRecallLimit;
    }

    public void setMaxRecallLimit(int maxRecallLimit) {
        this.maxRecallLimit = maxRecallLimit;
    }

    public int getMaxSummaryLength() {
        return maxSummaryLength;
    }

    public void setMaxSummaryLength(int maxSummaryLength) {
        this.maxSummaryLength = maxSummaryLength;
    }

    public int getMaxRecallTextLength() {
        return maxRecallTextLength;
    }

    public void setMaxRecallTextLength(int maxRecallTextLength) {
        this.maxRecallTextLength = maxRecallTextLength;
    }
}
