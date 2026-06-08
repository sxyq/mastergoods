package com.zhihuiji.data.agent

import com.zhihuiji.core.database.dao.AgentAuditDao
import com.zhihuiji.core.database.entity.AgentAuditEntity
import com.zhihuiji.core.model.v2.agent.AgentAuditRecord
import com.zhihuiji.core.model.v2.agent.DraftAuditInfo
import com.zhihuiji.core.model.v2.agent.ErrorAuditInfo
import com.zhihuiji.core.model.v2.agent.SafetyAuditResult
import com.zhihuiji.core.model.v2.agent.ToolAuditRecord
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Singleton
class AgentAuditRepository @Inject constructor(
    private val auditDao: AgentAuditDao,
    private val json: Json,
) {

    suspend fun insertRecord(record: AgentAuditRecord) {
        auditDao.insert(record.toEntity(json))
    }

    fun observeRecent(limit: Int = 100): Flow<List<AgentAuditRecord>> {
        return auditDao.observeRecent(limit).map { list ->
            list.map { it.toRecord(json) }
        }
    }

    fun observeByConversation(conversationId: Long): Flow<List<AgentAuditRecord>> {
        return auditDao.observeByConversation(conversationId).map { list ->
            list.map { it.toRecord(json) }
        }
    }

    suspend fun deleteOlderThan(days: Int) {
        val cutoff = System.currentTimeMillis() - days * 24 * 60 * 60 * 1000L
        auditDao.deleteOlderThan(cutoff)
    }

    suspend fun count(): Int = auditDao.count()
}

private fun AgentAuditRecord.toEntity(json: Json): AgentAuditEntity = AgentAuditEntity(
    id = id,
    runId = runId,
    conversationId = conversationId,
    userMessage = userMessage,
    safetyPassed = safetyResult?.passed,
    safetyReason = safetyResult?.reason,
    toolsCalledJson = toolsCalled.takeIf { it.isNotEmpty() }?.let { json.encodeToString(it) },
    draftId = draftGenerated?.draftId,
    draftType = draftGenerated?.draftType,
    draftTitle = draftGenerated?.title,
    userConfirmed = draftGenerated?.userConfirmed,
    contextCompacted = contextCompacted,
    finalAnswerSummary = finalAnswerSummary,
    errorCode = errorInfo?.code,
    errorMessage = errorInfo?.message,
    timestamp = timestamp,
)

private fun AgentAuditEntity.toRecord(json: Json): AgentAuditRecord = AgentAuditRecord(
    id = id,
    runId = runId,
    conversationId = conversationId,
    userMessage = userMessage,
    safetyResult = safetyPassed?.let {
        SafetyAuditResult(
            passed = it,
            reason = safetyReason,
        )
    },
    toolsCalled = toolsCalledJson?.let {
        try {
            json.decodeFromString<List<ToolAuditRecord>>(it)
        } catch (_: Exception) {
            emptyList()
        }
    } ?: emptyList(),
    draftGenerated = draftType?.let {
        DraftAuditInfo(
            draftId = draftId,
            draftType = it,
            title = draftTitle ?: "",
            userConfirmed = userConfirmed,
        )
    },
    contextCompacted = contextCompacted,
    finalAnswerSummary = finalAnswerSummary,
    errorInfo = errorMessage?.let {
        ErrorAuditInfo(
            code = errorCode,
            message = it,
        )
    },
    timestamp = timestamp,
)
