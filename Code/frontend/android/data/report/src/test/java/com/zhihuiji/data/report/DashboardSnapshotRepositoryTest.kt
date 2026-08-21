package com.zhihuiji.data.report

import com.zhihuiji.core.database.dao.DashboardSnapshotDao
import com.zhihuiji.core.database.entity.DashboardSnapshotEntity
import com.zhihuiji.core.model.SalesTrendPointReportDto
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class DashboardSnapshotRepositoryTest {
    @Test
    fun snapshotRoundTripIsScopedToTheLoggedInAccount() = runBlocking {
        val dao = FakeDashboardSnapshotDao()
        val repository = DashboardSnapshotRepository(
            dao = dao,
            scopeProvider = FixedScopeProvider("user:42"),
            json = Json { ignoreUnknownKeys = true },
        )
        val expected = DashboardSnapshot(
            salesAmount = 37.0,
            salesOrderCount = 2,
            receivableAmount = 0.0,
            receivableCustomerCount = 0,
            netCashFlow = 89.88,
            salesTrend = listOf(
                SalesTrendPointReportDto(
                    startAt = 1_720_000_000_000L,
                    endAt = 1_720_086_400_000L,
                    totalSalesAmount = 37.0,
                    totalOrderCount = 2,
                ),
            ),
            updatedAt = 1_720_000_000_000L,
        )

        repository.save("range:365", expected)

        assertNotNull(dao.snapshot)
        assertEquals("user:42:range:365", dao.snapshot?.scopeKey)
        assertEquals(expected, repository.load("range:365"))
        Unit
    }

    @Test
    fun snapshotSerializationAndDaoAccessRunOffTheCallerThread() = runBlocking {
        val dao = ThreadTrackingDashboardSnapshotDao()
        val repository = DashboardSnapshotRepository(
            dao = dao,
            scopeProvider = FixedScopeProvider("user:42"),
            json = Json { ignoreUnknownKeys = true },
        )
        val callerThread = Thread.currentThread().name
        val snapshot = DashboardSnapshot(
            salesAmount = 37.0,
            salesOrderCount = 2,
            receivableAmount = 0.0,
            receivableCustomerCount = 0,
            netCashFlow = 89.88,
            salesTrend = emptyList(),
            updatedAt = 1_720_000_000_000L,
        )

        repository.save("range:365", snapshot)
        repository.load("range:365")

        assertNotEquals(callerThread, dao.upsertThread)
        assertNotEquals(callerThread, dao.findThread)
    }

    private class FakeDashboardSnapshotDao : DashboardSnapshotDao {
        var snapshot: DashboardSnapshotEntity? = null

        override suspend fun find(scopeKey: String): DashboardSnapshotEntity? =
            snapshot?.takeIf { it.scopeKey == scopeKey }

        override suspend fun upsert(snapshot: DashboardSnapshotEntity) {
            this.snapshot = snapshot
        }

        override suspend fun clear() {
            snapshot = null
        }
    }

    private class ThreadTrackingDashboardSnapshotDao : DashboardSnapshotDao {
        var snapshot: DashboardSnapshotEntity? = null
        var findThread: String? = null
        var upsertThread: String? = null

        override suspend fun find(scopeKey: String): DashboardSnapshotEntity? {
            findThread = Thread.currentThread().name
            return snapshot?.takeIf { it.scopeKey == scopeKey }
        }

        override suspend fun upsert(snapshot: DashboardSnapshotEntity) {
            upsertThread = Thread.currentThread().name
            this.snapshot = snapshot
        }

        override suspend fun clear() {
            snapshot = null
        }
    }

    private class FixedScopeProvider(
        private val scope: String?,
    ) : DashboardSnapshotScopeProvider {
        override suspend fun currentScopePrefix(): String? = scope
    }
}
