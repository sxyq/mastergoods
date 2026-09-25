// 测试账号范围：8220 测试服务器上的所有账号都是测试账号，均可用于本项目测试。
package com.zhihuiji.backend.api.dto.agent;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.util.List;

public final class AnswerDtos {
    private AnswerDtos() {}

    @JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
    public record AgentAnswerDto(
        String query,
        String intent,
        String answer,
        List<String> highlights,
        List<String> columns,
        List<List<String>> rows,
        List<String> suggestedActions
    ) {}
}
