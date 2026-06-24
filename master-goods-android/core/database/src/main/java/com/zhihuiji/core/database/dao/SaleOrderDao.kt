package com.zhihuiji.core.database.dao

import androidx.room.*
import com.zhihuiji.core.database.entity.SaleOrderEntity
import com.zhihuiji.core.database.entity.SaleOrderItemEntity
import com.zhihuiji.core.database.entity.SaleOrderWithItems
import kotlinx.coroutines.flow.Flow

@Dao
interface SaleOrderDao {
    @Query("SELECT * FROM sale_orders ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<SaleOrderEntity>>

    @Query("""
        SELECT * FROM sale_orders
        WHERE (:keyword IS NULL OR orderNo LIKE '%' || :keyword || '%' OR customerName LIKE '%' || :keyword || '%')
          AND (:status IS NULL OR status = :status)
          AND (:minTotalAmount IS NULL OR totalAmount >= :minTotalAmount)
          AND (:maxTotalAmount IS NULL OR totalAmount <= :maxTotalAmount)
          AND (:createdAfter IS NULL OR createdAt >= :createdAfter)
          AND (:createdBefore IS NULL OR createdAt <= :createdBefore)
          AND (:productKeyword IS NULL OR EXISTS (
              SELECT 1 FROM sale_order_items item
              WHERE item.orderId = sale_orders.id
                AND (item.productCode LIKE '%' || :productKeyword || '%' OR item.productName LIKE '%' || :productKeyword || '%')
          ))
          AND (:paymentStatus IS NULL OR
              (:paymentStatus = 0 AND paidAmount + 0.000001 < totalAmount) OR
              (:paymentStatus = 1 AND paidAmount + 0.000001 >= totalAmount))
        ORDER BY updatedAt DESC
    """)
    fun search(
        keyword: String?,
        status: Int?,
        minTotalAmount: Double?,
        maxTotalAmount: Double?,
        createdAfter: Long?,
        createdBefore: Long?,
        productKeyword: String?,
        paymentStatus: Int?,
    ): Flow<List<SaleOrderEntity>>

    @Query("SELECT * FROM sale_orders WHERE id = :id")
    suspend fun findById(id: Long): SaleOrderEntity?

    @Transaction
    @Query("SELECT * FROM sale_orders WHERE id = :id")
    suspend fun findWithItemsById(id: Long): SaleOrderWithItems?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: SaleOrderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<SaleOrderEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertItems(entities: List<SaleOrderItemEntity>)

    @Query("DELETE FROM sale_order_items WHERE orderId = :orderId")
    suspend fun deleteItemsByOrderId(orderId: Long)

    @Query("DELETE FROM sale_order_items WHERE id = :id")
    suspend fun deleteItemById(id: Long)

    @Query("DELETE FROM sale_order_items WHERE orderId IN (:orderIds)")
    suspend fun deleteItemsByOrderIds(orderIds: List<Long>)

    @Transaction
    suspend fun replaceOrderGraph(order: SaleOrderEntity, items: List<SaleOrderItemEntity>) {
        upsert(order)
        deleteItemsByOrderId(order.id)
        if (items.isNotEmpty()) {
            upsertItems(items)
        }
    }

    @Transaction
    suspend fun replaceOrderGraphs(orders: List<SaleOrderEntity>, items: List<SaleOrderItemEntity>) {
        if (orders.isEmpty()) return
        upsertAll(orders)
        val orderIds = ArrayList<Long>(orders.size)
        for (order in orders) {
            orderIds.add(order.id)
        }
        deleteItemsByOrderIds(orderIds)
        if (items.isNotEmpty()) {
            upsertItems(items)
        }
    }

    @Query("DELETE FROM sale_orders WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM sale_orders")
    suspend fun clear()
}
