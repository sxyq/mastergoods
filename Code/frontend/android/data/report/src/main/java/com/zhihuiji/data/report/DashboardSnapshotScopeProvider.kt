package com.zhihuiji.data.report

import com.zhihuiji.core.datastore.SessionStore
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

interface DashboardSnapshotScopeProvider {
    suspend fun currentScopePrefix(): String?
}

@Singleton
class SessionDashboardSnapshotScopeProvider @Inject constructor(
    private val sessionStore: SessionStore,
) : DashboardSnapshotScopeProvider {
    override suspend fun currentScopePrefix(): String? =
        sessionStore.userId.first()?.let { "user:$it" }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class DashboardSnapshotScopeModule {
    @Binds
    abstract fun bindDashboardSnapshotScopeProvider(
        implementation: SessionDashboardSnapshotScopeProvider,
    ): DashboardSnapshotScopeProvider
}
