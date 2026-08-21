package com.zhihuiji.backend.application.service.v2.agent.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.zhihuiji.backend.domain.entity.CustomerEntity;
import com.zhihuiji.backend.domain.entity.product.ProductEntity;
import com.zhihuiji.backend.domain.entity.PurchaseOrderEntity;
import com.zhihuiji.backend.domain.entity.SaleOrderEntity;
import com.zhihuiji.backend.domain.entity.SupplierEntity;
import com.zhihuiji.backend.infrastructure.repository.CustomerRepository;
import com.zhihuiji.backend.infrastructure.repository.product.ProductRepository;
import com.zhihuiji.backend.infrastructure.repository.PurchaseOrderRepository;
import com.zhihuiji.backend.infrastructure.repository.SaleOrderRepository;
import com.zhihuiji.backend.infrastructure.repository.SupplierRepository;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 校验 Agent 草稿中的实体引用属于当前 owner。
 *
 * <p>模型提供的 ID 只是不可信输入，草稿工具仍要在服务端重新确认归属。
 */
@Component
public class AgentEntityReferenceValidator {

    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final SupplierRepository supplierRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final SaleOrderRepository saleOrderRepository;

    @Autowired
    public AgentEntityReferenceValidator(
        ProductRepository productRepository,
        CustomerRepository customerRepository,
        SupplierRepository supplierRepository,
        PurchaseOrderRepository purchaseOrderRepository,
        SaleOrderRepository saleOrderRepository
    ) {
        this.productRepository = productRepository;
        this.customerRepository = customerRepository;
        this.supplierRepository = supplierRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.saleOrderRepository = saleOrderRepository;
    }

    public boolean productBelongsToOwner(Long ownerUserId, Long productId) {
        return ownerUserId != null && productId != null && productId > 0L
            && productRepository.findByIdAndOwnerUserId(productId, ownerUserId).isPresent();
    }

    public boolean customerMatches(Long ownerUserId, Long customerId, String customerName) {
        if (ownerUserId == null || customerId == null || customerId <= 0L) {
            return false;
        }
        return customerRepository.findByIdAndOwnerUserId(customerId, ownerUserId)
            .map(customer -> sameName(customer, customerName))
            .orElse(false);
    }

    public boolean supplierMatches(Long ownerUserId, Long supplierId, String supplierName) {
        if (ownerUserId == null || supplierId == null || supplierId <= 0L) {
            return false;
        }
        return supplierRepository.findByIdAndOwnerUserId(supplierId, ownerUserId)
            .map(supplier -> sameName(supplier, supplierName))
            .orElse(false);
    }

    public boolean purchaseOrderBelongsToOwner(Long ownerUserId, Long purchaseOrderId) {
        return ownerUserId != null && purchaseOrderId != null && purchaseOrderId > 0L
            && purchaseOrderRepository.findByIdAndOwnerUserId(purchaseOrderId, ownerUserId).isPresent();
    }

    public boolean saleOrderBelongsToOwner(Long ownerUserId, Long saleOrderId) {
        return ownerUserId != null && saleOrderId != null && saleOrderId > 0L
            && saleOrderRepository.findByIdAndOwnerUserId(saleOrderId, ownerUserId).isPresent();
    }

    private boolean sameName(CustomerEntity customer, String name) {
        return StringUtils.hasText(name) && normalize(customer.getName()).equals(normalize(name));
    }

    private boolean sameName(SupplierEntity supplier, String name) {
        return StringUtils.hasText(name) && normalize(supplier.getName()).equals(normalize(name));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
