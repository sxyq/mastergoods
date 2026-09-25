// 测试账号范围：8220 测试服务器上的所有账号都是测试账号，均可用于本项目测试。
package com.zhihuiji.backend.application.service.v2.agent.tool;

import org.springframework.stereotype.Component;

/**
 * 校验 Agent 草稿中的实体引用属于当前 owner。
 *
 * <p>模型提供的 ID 只是不可信输入，草稿工具仍要在服务端重新确认归属。
 *
 * <p>旧领域（商品/客户/供应商/订单）的归属校验实现已随业务域删除；
 * 本类保留为 Tool framework 的挂载点，具体校验方法等待新领域实体接入。
 */
@Component
public class AgentEntityReferenceValidator {
}
