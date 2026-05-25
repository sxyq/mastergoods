package com.zhihuiji.backend.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "agent.llm")
public class AgentLlmProperties {
    private boolean enabled;
    private String baseUrl = "https://api.longcat.chat/anthropic";
    private String apiKey = "";
    private String model = "LongCat-Flash-Thinking-2601";
    private String anthropicVersion = "2023-06-01";
    private int maxTokens = 4096;
    private double temperature = 0.2;
    private boolean enableThinking = true;
    private int thinkingBudget = 2048;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getAnthropicVersion() {
        return anthropicVersion;
    }

    public void setAnthropicVersion(String anthropicVersion) {
        this.anthropicVersion = anthropicVersion;
    }

    public int getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(int maxTokens) {
        this.maxTokens = maxTokens;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public boolean isEnableThinking() {
        return enableThinking;
    }

    public void setEnableThinking(boolean enableThinking) {
        this.enableThinking = enableThinking;
    }

    public int getThinkingBudget() {
        return thinkingBudget;
    }

    public void setThinkingBudget(int thinkingBudget) {
        this.thinkingBudget = thinkingBudget;
    }
}
