package com.zhihuiji.core.database

import com.zhihuiji.core.database.entity.CustomerEntity
import com.zhihuiji.core.database.entity.FinanceRecordEntity
import com.zhihuiji.core.database.entity.PayOrderEntity
import com.zhihuiji.core.database.entity.ProductEntity
import com.zhihuiji.core.database.entity.PurchaseOrderEntity
import com.zhihuiji.core.database.entity.SaleOrderEntity
import com.zhihuiji.core.database.entity.SupplierEntity
import com.zhihuiji.core.model.CustomerDto
import com.zhihuiji.core.model.FinanceRecordDto
import com.zhihuiji.core.model.PayOrderDto
import com.zhihuiji.core.model.ProductDto
import com.zhihuiji.core.model.PurchaseOrderDto
import com.zhihuiji.core.model.SaleOrderDto
import com.zhihuiji.core.model.SupplierDto

fun ProductEntity.toDto() = ProductDto(
    id = id,
    code = code,
    name = name,
    category = category,
    unit = unit,
    salePrice = salePrice,
    purchasePrice = purchasePrice,
    stock = stock,
    safeStock = safeStock,
    status = status,
    syncStatus = syncStatus,
    syncVersion = syncVersion,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun ProductDto.toEntity(): ProductEntity? {
    val entityId = id ?: return null
    return ProductEntity(
        id = entityId,
        code = code,
        name = name,
        category = category,
        unit = unit,
        salePrice = salePrice,
        purchasePrice = purchasePrice,
        stock = stock,
        safeStock = safeStock,
        status = status,
        syncStatus = syncStatus,
        syncVersion = syncVersion,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}

fun CustomerEntity.toDto() = CustomerDto(
    id = id,
    name = name,
    phone = phone,
    level = level,
    address = address,
    notes = notes,
    balance = balance,
    status = status,
    syncStatus = syncStatus,
    syncVersion = syncVersion,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun CustomerDto.toEntity(): CustomerEntity? {
    val entityId = id ?: return null
    return CustomerEntity(
        id = entityId,
        name = name,
        phone = phone,
        level = level,
        address = address,
        notes = notes,
        balance = balance,
        status = status,
        syncStatus = syncStatus,
        syncVersion = syncVersion,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}

fun SupplierEntity.toDto() = SupplierDto(
    id = id,
    name = name,
    phone = phone,
    address = address,
    notes = notes,
    balance = balance,
    status = status,
    syncStatus = syncStatus,
    syncVersion = syncVersion,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun SupplierDto.toEntity(): SupplierEntity? {
    val entityId = id ?: return null
    return SupplierEntity(
        id = entityId,
        name = name,
        phone = phone,
        address = address,
        notes = notes,
        balance = balance,
        status = status,
        syncStatus = syncStatus,
        syncVersion = syncVersion,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}

fun SaleOrderEntity.toDto() = SaleOrderDto(
    id = id,
    orderNo = orderNo,
    customerId = customerId,
    customerName = customerName,
    items = emptyList(),
    subtotalAmount = subtotalAmount,
    discountAmount = discountAmount,
    totalAmount = totalAmount,
    paidAmount = paidAmount,
    notes = notes,
    status = status,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun SaleOrderDto.toEntity() = SaleOrderEntity(
    id = id,
    orderNo = orderNo,
    customerId = customerId,
    customerName = customerName,
    subtotalAmount = subtotalAmount,
    discountAmount = discountAmount,
    totalAmount = totalAmount,
    paidAmount = paidAmount,
    notes = notes,
    status = status,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun PurchaseOrderEntity.toDto() = PurchaseOrderDto(
    id = id,
    orderNo = orderNo,
    supplierName = supplierName,
    items = emptyList(),
    totalAmount = totalAmount,
    notes = notes,
    status = status,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun PurchaseOrderDto.toEntity() = PurchaseOrderEntity(
    id = id,
    orderNo = orderNo,
    supplierName = supplierName,
    totalAmount = totalAmount,
    notes = notes,
    status = status,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun PayOrderEntity.toDto() = PayOrderDto(
    id = id,
    orderNo = orderNo,
    supplierId = supplierId,
    supplierName = supplierName,
    amount = amount,
    method = method,
    referenceNo = referenceNo,
    notes = notes,
    status = status,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun PayOrderDto.toEntity() = PayOrderEntity(
    id = id,
    orderNo = orderNo,
    supplierId = supplierId,
    supplierName = supplierName,
    amount = amount,
    method = method,
    referenceNo = referenceNo,
    notes = notes,
    status = status,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun FinanceRecordEntity.toDto() = FinanceRecordDto(
    id = id,
    recordNo = recordNo,
    type = type,
    category = category,
    partnerName = partnerName,
    amount = amount,
    method = method,
    notes = notes,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun FinanceRecordDto.toEntity() = FinanceRecordEntity(
    id = id,
    recordNo = recordNo,
    type = type,
    category = category,
    partnerName = partnerName,
    amount = amount,
    method = method,
    notes = notes,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
