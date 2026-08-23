package com.zhihuiji.backend.infrastructure.repository;

import com.zhihuiji.backend.domain.entity.AgentMemoryEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

/**
 * Agent 长期记忆仓储。
 *
 * <p>所有查询带 {@code owner_user_id}，召回时按 owner/store 隔离；记忆删除后
 * 不能继续被召回。第一版使用数据库文本检索（LIKE），后续可在此接口增加向量检索。
 *
 * <p>不保存凭据、完整认证载荷；实体展示名需要脱敏或使用最小展示字段。
 */
public interface AgentMemoryRepository extends JpaRepository<AgentMemoryEntity, Long> {

    /**
     * 按 owner + store 召回有效记忆（第一版用数据库 LIKE 文本检索）。
     *
     * <p>recall_text 为检索目标字段；status='active'；store 限定为当前门店。
     * 不限 store 的召回使用 {@link #findActiveByOwner(Long, Pageable)}。
     *
     * @param ownerUserId 归属用户 ID
     * @param storeId 当前门店 ID
     * @param query 检索关键词（可为 null 表示不限关键词）
     * @param pageable 分页
     * @return 召回的记忆列表
     */
    @Query("""
        SELECT m
          FROM AgentMemoryEntity m
         WHERE m.ownerUserId = :ownerUserId
           AND m.storeId = :storeId
           AND m.status = 'active'
           AND (:query IS NULL OR LOWER(m.recallText) LIKE LOWER(CONCAT('%', :query, '%')))
         ORDER BY m.updatedAt DESC, m.id DESC
        """)
    List<AgentMemoryEntity> findActiveByOwnerAndStore(
        @Param("ownerUserId") Long ownerUserId,
        @Param("storeId") Long storeId,
        @Param("query") String query,
        Pageable pageable
    );

    /**
     * 不限 store 召回 owner 的有效记忆（用于无门店上下文或全局召回）。
     *
     * @param ownerUserId 归属用户 ID
     * @param query 检索关键词（可为 null）
     * @param pageable 分页
     * @return 召回的记忆列表
     */
    @Query("""
        SELECT m
          FROM AgentMemoryEntity m
         WHERE m.ownerUserId = :ownerUserId
           AND m.status = 'active'
           AND (:query IS NULL OR LOWER(m.recallText) LIKE LOWER(CONCAT('%', :query, '%')))
         ORDER BY m.updatedAt DESC, m.id DESC
        """)
    List<AgentMemoryEntity> findActiveByOwner(
        @Param("ownerUserId") Long ownerUserId,
        @Param("query") String query,
        Pageable pageable
    );

    /**
     * 按来源会话查找记忆（用于审计、记忆溯源与按会话删除）。
     *
     * @param ownerUserId 归属用户 ID
     * @param conversationId 来源会话 ID
     * @return 记忆列表
     */
    List<AgentMemoryEntity> findByOwnerUserIdAndSourceConversationIdOrderByCreatedAtDescIdDesc(
        Long ownerUserId,
        Long conversationId
    );

    /**
     * 用户删除指定记忆（按 owner 隔离）。
     *
     * @param id 记忆 ID
     * @param ownerUserId 归属用户 ID
     * @return 删除条数
     */
    long deleteByIdAndOwnerUserId(Long id, Long ownerUserId);

    /**
     * 更新记忆状态（soft delete 或归档）。
     *
     * @param id 记忆 ID
     * @param ownerUserId 归属用户 ID
     * @param status 新状态
     * @return 更新条数
     */
    @Modifying
    @Transactional
    @Query("""
        UPDATE AgentMemoryEntity m
           SET m.status = :status,
               m.updatedAt = :updatedAt
         WHERE m.id = :id
           AND m.ownerUserId = :ownerUserId
        """)
    int updateStatus(
        @Param("id") Long id,
        @Param("ownerUserId") Long ownerUserId,
        @Param("status") String status,
        @Param("updatedAt") Long updatedAt
    );

    /**
     * 更新最后访问时间（用于召回审计与 TTL）。
     *
     * @param id 记忆 ID
     * @param ownerUserId 归属用户 ID
     * @param accessedAt 访问时间戳
     * @return 更新条数
     */
    @Modifying
    @Transactional
    @Query("""
        UPDATE AgentMemoryEntity m
           SET m.lastAccessedAt = :accessedAt,
               m.updatedAt = :updatedAt
         WHERE m.id = :id
           AND m.ownerUserId = :ownerUserId
        """)
    int updateLastAccessed(
        @Param("id") Long id,
        @Param("ownerUserId") Long ownerUserId,
        @Param("accessedAt") Long accessedAt,
        @Param("updatedAt") Long updatedAt
    );

    /**
     * 按 ID 与 owner 查找单条记忆（用于查看详情）。
     */
    Optional<AgentMemoryEntity> findByIdAndOwnerUserId(Long id, Long ownerUserId);
}
