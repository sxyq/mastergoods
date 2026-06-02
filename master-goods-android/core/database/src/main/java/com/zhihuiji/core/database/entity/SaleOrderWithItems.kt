package com.zhihuiji.core.database.entity

import androidx.room.Embedded
import androidx.room.Relation

data class SaleOrderWithItems(
    @Embedded val order: SaleOrderEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "orderId",
    )
    val items: List<SaleOrderItemEntity>,
)
