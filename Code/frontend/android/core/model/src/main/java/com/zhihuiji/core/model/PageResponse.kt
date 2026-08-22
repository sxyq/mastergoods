package com.zhihuiji.core.model

import kotlinx.serialization.Serializable

/** Spring Data Page JSON returned by paginated V2 endpoints. */
@Serializable
data class PageResponse<T>(
    val content: List<T> = emptyList(),
    val number: Int = 0,
    val size: Int = 0,
    val totalElements: Long = 0L,
    val totalPages: Int = 0,
    val numberOfElements: Int = 0,
    val first: Boolean = true,
    val last: Boolean = true,
    val empty: Boolean = true,
)
