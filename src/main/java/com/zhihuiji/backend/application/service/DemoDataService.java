package com.zhihuiji.backend.application.service;

import com.zhihuiji.backend.api.common.PayOrderStatus;
import com.zhihuiji.backend.api.common.PaymentType;
import com.zhihuiji.backend.api.common.PurchaseOrderStatus;
import com.zhihuiji.backend.domain.entity.CustomerEntity;
import com.zhihuiji.backend.domain.entity.PayOrderEntity;
import com.zhihuiji.backend.domain.entity.PaymentEntity;
import com.zhihuiji.backend.domain.entity.ProductEntity;
import com.zhihuiji.backend.domain.entity.PurchaseOrderEntity;
import com.zhihuiji.backend.domain.entity.PurchaseOrderItemEntity;
import com.zhihuiji.backend.domain.entity.SaleOrderEntity;
import com.zhihuiji.backend.domain.entity.SaleOrderItemEntity;
import com.zhihuiji.backend.domain.entity.SupplierEntity;
import com.zhihuiji.backend.domain.entity.UserEntity;
import com.zhihuiji.backend.infrastructure.repository.AgentNotificationRepository;
import com.zhihuiji.backend.infrastructure.repository.AgentTaskRepository;
import com.zhihuiji.backend.infrastructure.repository.CustomerRepository;
import com.zhihuiji.backend.infrastructure.repository.InventoryAdjustmentRepository;
import com.zhihuiji.backend.infrastructure.repository.PayOrderRepository;
import com.zhihuiji.backend.infrastructure.repository.PaymentRepository;
import com.zhihuiji.backend.infrastructure.repository.ProductRepository;
import com.zhihuiji.backend.infrastructure.repository.PurchaseOrderItemRepository;
import com.zhihuiji.backend.infrastructure.repository.PurchaseOrderRepository;
import com.zhihuiji.backend.infrastructure.repository.SaleOrderItemRepository;
import com.zhihuiji.backend.infrastructure.repository.SaleOrderRepository;
import com.zhihuiji.backend.infrastructure.repository.SessionRepository;
import com.zhihuiji.backend.infrastructure.repository.SupplierRepository;
import com.zhihuiji.backend.infrastructure.repository.UserRepository;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile("local")
public class DemoDataService {
    private static final String DEMO_OWNER_PHONE = "13800138111";
    private static final List<String> DEMO_USER_PHONES = List.of(
        "13800138111",
        "13800138112",
        "13800138113",
        "13800138114"
    );

    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final SupplierRepository supplierRepository;
    private final SaleOrderRepository saleOrderRepository;
    private final SaleOrderItemRepository saleOrderItemRepository;
    private final PaymentRepository paymentRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseOrderItemRepository purchaseOrderItemRepository;
    private final InventoryAdjustmentRepository inventoryAdjustmentRepository;
    private final PayOrderRepository payOrderRepository;
    private final AgentTaskRepository agentTaskRepository;
    private final AgentNotificationRepository agentNotificationRepository;
    private final SaleOrderService saleOrderService;
    private final PurchaseOrderService purchaseOrderService;
    private final PayOrderService payOrderService;
    private final PasswordEncoder passwordEncoder;
    private final EntityManager entityManager;

    public DemoDataService(
        UserRepository userRepository,
        SessionRepository sessionRepository,
        ProductRepository productRepository,
        CustomerRepository customerRepository,
        SupplierRepository supplierRepository,
        SaleOrderRepository saleOrderRepository,
        SaleOrderItemRepository saleOrderItemRepository,
        PaymentRepository paymentRepository,
        PurchaseOrderRepository purchaseOrderRepository,
        PurchaseOrderItemRepository purchaseOrderItemRepository,
        InventoryAdjustmentRepository inventoryAdjustmentRepository,
        PayOrderRepository payOrderRepository,
        AgentTaskRepository agentTaskRepository,
        AgentNotificationRepository agentNotificationRepository,
        SaleOrderService saleOrderService,
        PurchaseOrderService purchaseOrderService,
        PayOrderService payOrderService,
        PasswordEncoder passwordEncoder,
        EntityManager entityManager
    ) {
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
        this.productRepository = productRepository;
        this.customerRepository = customerRepository;
        this.supplierRepository = supplierRepository;
        this.saleOrderRepository = saleOrderRepository;
        this.saleOrderItemRepository = saleOrderItemRepository;
        this.paymentRepository = paymentRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.purchaseOrderItemRepository = purchaseOrderItemRepository;
        this.inventoryAdjustmentRepository = inventoryAdjustmentRepository;
        this.payOrderRepository = payOrderRepository;
        this.agentTaskRepository = agentTaskRepository;
        this.agentNotificationRepository = agentNotificationRepository;
        this.saleOrderService = saleOrderService;
        this.purchaseOrderService = purchaseOrderService;
        this.payOrderService = payOrderService;
        this.passwordEncoder = passwordEncoder;
        this.entityManager = entityManager;
    }

    @Transactional
    public SeedResult seed(boolean reset) {
        if (reset) {
            clearAll();
        }
        if (!reset && userRepository.count() > 0 && productRepository.count() > 0) {
            return snapshot(false);
        }

        long now = System.currentTimeMillis();
        createUsers(now);
        Long ownerUserId = requireDemoOwnerUserId();
        Map<String, SupplierEntity> suppliers = createSuppliers(ownerUserId, now);
        Map<String, CustomerEntity> customers = createCustomers(ownerUserId, now);
        Map<String, ProductEntity> products = createProducts(ownerUserId, now);

        createPurchaseOrders(ownerUserId, suppliers, products);
        createSaleOrders(ownerUserId, customers, products);
        createPayOrders(ownerUserId, suppliers);
        patchInventoryForAnomalies(ownerUserId, products, now);
        return snapshot(true);
    }

    private SeedResult snapshot(boolean mutated) {
        return new SeedResult(
            mutated,
            userRepository.count(),
            productRepository.count(),
            customerRepository.count(),
            supplierRepository.count(),
            saleOrderRepository.count(),
            purchaseOrderRepository.count(),
            payOrderRepository.count(),
            List.of(
                new DemoAccount("13800138111", "123456", "系统管理员"),
                new DemoAccount("13800138112", "123456", "仓库经理"),
                new DemoAccount("13800138113", "123456", "门店店员")
            )
        );
    }

    private void clearAll() {
        List<Long> demoUserIds = demoOwnerUserIds();
        if (!demoUserIds.isEmpty()) {
            agentNotificationRepository.deleteAllByOwnerUserIdIn(demoUserIds);
            agentTaskRepository.deleteAllByOwnerUserIdIn(demoUserIds);
        }
        inventoryAdjustmentRepository.deleteAll();
        paymentRepository.deleteAll();
        saleOrderItemRepository.deleteAll();
        saleOrderRepository.deleteAll();
        purchaseOrderItemRepository.deleteAll();
        purchaseOrderRepository.deleteAll();
        payOrderRepository.deleteAll();
        sessionRepository.deleteAll();
        customerRepository.deleteAll();
        supplierRepository.deleteAll();
        productRepository.deleteAll();
        userRepository.deleteAll();
        entityManager.flush();
        entityManager.clear();
    }

    private List<Long> demoOwnerUserIds() {
        List<Long> ids = new ArrayList<>(DEMO_USER_PHONES.size());
        for (String phone : DEMO_USER_PHONES) {
            userRepository.findByPhone(phone)
                .map(UserEntity::getId)
                .ifPresent(ids::add);
        }
        return ids;
    }

    private void createUsers(long now) {
        createUser("13800138111", "123456", "系统管理员", 1, now);
        createUser("13800138112", "123456", "仓库经理", 1, now);
        createUser("13800138113", "123456", "门店店员", 1, now);
        createUser("13800138114", "123456", "停用账号", 0, now);
    }

    private void createUser(String phone, String password, String nickname, int status, long now) {
        UserEntity user = new UserEntity();
        user.setPhone(phone);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setNickname(nickname);
        user.setStatus(status);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        userRepository.save(user);
    }

    private Long requireDemoOwnerUserId() {
        return userRepository.findByPhone(DEMO_OWNER_PHONE)
            .map(UserEntity::getId)
            .orElseThrow(() -> new IllegalStateException("demo owner is missing"));
    }

    private Map<String, SupplierEntity> createSuppliers(Long ownerUserId, long now) {
        Map<String, SupplierEntity> map = new HashMap<>(3);
        map.put("supplier-a", createSupplier(ownerUserId, "供应商A", "13900010001", "华东供货中心", "常规补货", 12800.0, now));
        map.put("supplier-b", createSupplier(ownerUserId, "供应商B", "13900010002", "工业配件市场", "账期 15 天", 8600.0, now));
        map.put("supplier-c", createSupplier(ownerUserId, "供应商C", "13900010003", "包装耗材仓", "包装材料", 3200.0, now));
        return map;
    }

    private SupplierEntity createSupplier(
        Long ownerUserId,
        String name,
        String phone,
        String address,
        String notes,
        double balance,
        long now
    ) {
        SupplierEntity supplier = new SupplierEntity();
        supplier.setOwnerUserId(ownerUserId);
        supplier.setName(name);
        supplier.setPhone(phone);
        supplier.setAddress(address);
        supplier.setNotes(notes);
        supplier.setBalance(balance);
        supplier.setStatus(1);
        supplier.setSyncStatus(0);
        supplier.setSyncVersion(1L);
        supplier.setCreatedAt(now);
        supplier.setUpdatedAt(now);
        return supplierRepository.save(supplier);
    }

    private Map<String, CustomerEntity> createCustomers(Long ownerUserId, long now) {
        Map<String, CustomerEntity> map = new HashMap<>(3);
        map.put("customer-a", createCustomer(ownerUserId, "门店客户A", "13700020001", "静安门店", "核心连锁门店", 15800.0, now));
        map.put("customer-b", createCustomer(ownerUserId, "客户B", "13700020002", "嘉定工业园", "工业客户", 9200.0, now));
        map.put("customer-c", createCustomer(ownerUserId, "客户C", "13700020003", "闵行仓配点", "账期客户", 4100.0, now));
        return map;
    }

    private CustomerEntity createCustomer(
        Long ownerUserId,
        String name,
        String phone,
        String address,
        String notes,
        double balance,
        long now
    ) {
        CustomerEntity customer = new CustomerEntity();
        customer.setOwnerUserId(ownerUserId);
        customer.setName(name);
        customer.setPhone(phone);
        customer.setLevel(2);
        customer.setAddress(address);
        customer.setNotes(notes);
        customer.setBalance(balance);
        customer.setStatus(1);
        customer.setSyncStatus(0);
        customer.setSyncVersion(1L);
        customer.setCreatedAt(now);
        customer.setUpdatedAt(now);
        return customerRepository.save(customer);
    }

    private Map<String, ProductEntity> createProducts(Long ownerUserId, long now) {
        Map<String, ProductEntity> map = new HashMap<>(5);
        map.put("sensor-s7", createProduct(ownerUserId, "S7-0021", "工业传感器 S7", "工业件", "个", 58.0, 35.0, 22.0, 30.0, now));
        map.put("glove-a12", createProduct(ownerUserId, "A12-0045", "绝缘手套 A12", "劳保", "只", 48.0, 26.0, 24.0, 18.0, now));
        map.put("box-xl", createProduct(ownerUserId, "XL-0012", "包装纸箱 XL", "耗材", "箱", 12.0, 5.0, 12.0, 20.0, now));
        map.put("scanner-q9", createProduct(ownerUserId, "Q9-1008", "扫码枪 Q9", "设备", "台", 168.0, 120.0, 31.0, 12.0, now));
        map.put("tape-b3", createProduct(ownerUserId, "B3-0811", "封箱胶带 B3", "耗材", "卷", 8.0, 3.0, 64.0, 30.0, now));
        return map;
    }

    private ProductEntity createProduct(
        Long ownerUserId,
        String code,
        String name,
        String category,
        String unit,
        double salePrice,
        double purchasePrice,
        double stock,
        double safeStock,
        long now
    ) {
        ProductEntity product = new ProductEntity();
        product.setOwnerUserId(ownerUserId);
        product.setCode(code);
        product.setName(name);
        product.setCategory(category);
        product.setUnit(unit);
        product.setSalePrice(salePrice);
        product.setPurchasePrice(purchasePrice);
        product.setStock(stock);
        product.setSafeStock(safeStock);
        product.setStatus(1);
        product.setSyncStatus(0);
        product.setSyncVersion(1L);
        product.setCreatedAt(now);
        product.setUpdatedAt(now);
        return productRepository.save(product);
    }

    private void createPurchaseOrders(Long ownerUserId, Map<String, SupplierEntity> suppliers, Map<String, ProductEntity> products) {
        var received = purchaseOrderService.createForOwner(
            ownerUserId,
            new PurchaseOrderService.CreatePurchaseOrderCommand(
                suppliers.get("supplier-a").getId(),
                suppliers.get("supplier-a").getName(),
                List.of(
                    new PurchaseOrderService.PurchaseItemDraft(products.get("sensor-s7").getId(), null, null, 20.0, 34.0),
                    new PurchaseOrderService.PurchaseItemDraft(products.get("scanner-q9").getId(), null, null, 8.0, 118.0)
                ),
                2,
                null,
                "四月第一批补货",
                PurchaseOrderStatus.RECEIVED.code()
            )
        );
        backdatePurchaseOrder(ownerUserId, received.order().getId(), nowMinusDays(12), nowMinusDays(12));

        var draft = purchaseOrderService.createForOwner(
            ownerUserId,
            new PurchaseOrderService.CreatePurchaseOrderCommand(
                suppliers.get("supplier-c").getId(),
                suppliers.get("supplier-c").getName(),
                List.of(
                    new PurchaseOrderService.PurchaseItemDraft(products.get("box-xl").getId(), null, null, 18.0, 4.6),
                    new PurchaseOrderService.PurchaseItemDraft(products.get("tape-b3").getId(), null, null, 40.0, 2.8)
                ),
                null,
                null,
                "包装耗材待到货",
                PurchaseOrderStatus.DRAFT.code()
            )
        );
        backdatePurchaseOrder(ownerUserId, draft.order().getId(), nowMinusDays(5), nowMinusDays(5));
    }

    private void createSaleOrders(Long ownerUserId, Map<String, CustomerEntity> customers, Map<String, ProductEntity> products) {
        var so1 = saleOrderService.createForOwner(
            ownerUserId,
            new SaleOrderService.CreateSaleOrderCommand(
                customers.get("customer-a").getId(),
                customers.get("customer-a").getName(),
                List.of(
                    new SaleOrderService.SaleItemDraft(products.get("sensor-s7").getId(), 6.0, 58.0),
                    new SaleOrderService.SaleItemDraft(products.get("glove-a12").getId(), 4.0, 48.0)
                ),
                "门店补货单",
                30.0
            )
        );
        backdateSaleOrder(ownerUserId, so1.order().getId(), nowMinusDays(18), nowMinusDays(18));
        saleOrderService.addPaymentForOwner(ownerUserId, so1.order().getId(), 220.0, 1, "POS-A001");
        backdateLatestPayment(ownerUserId, so1.order().getId(), nowMinusDays(17));

        var so2 = saleOrderService.createForOwner(
            ownerUserId,
            new SaleOrderService.CreateSaleOrderCommand(
                customers.get("customer-b").getId(),
                customers.get("customer-b").getName(),
                List.of(
                    new SaleOrderService.SaleItemDraft(products.get("scanner-q9").getId(), 3.0, 168.0),
                    new SaleOrderService.SaleItemDraft(products.get("sensor-s7").getId(), 5.0, 60.0)
                ),
                "工业客户项目补货",
                0.0
            )
        );
        backdateSaleOrder(ownerUserId, so2.order().getId(), nowMinusDays(9), nowMinusDays(9));
        saleOrderService.addPaymentForOwner(ownerUserId, so2.order().getId(), 300.0, 2, "WX-B221");
        backdateLatestPayment(ownerUserId, so2.order().getId(), nowMinusDays(8));

        var so3 = saleOrderService.createForOwner(
            ownerUserId,
            new SaleOrderService.CreateSaleOrderCommand(
                customers.get("customer-c").getId(),
                customers.get("customer-c").getName(),
                List.of(
                    new SaleOrderService.SaleItemDraft(products.get("glove-a12").getId(), 7.0, 49.0),
                    new SaleOrderService.SaleItemDraft(products.get("box-xl").getId(), 10.0, 12.0)
                ),
                "账期客户日常出货",
                0.0
            )
        );
        backdateSaleOrder(ownerUserId, so3.order().getId(), nowMinusDays(3), nowMinusDays(3));

        saleOrderService.cancelForOwner(ownerUserId, so3.order().getId());
        markRefundAsHistorical(ownerUserId, so3.order().getId(), nowMinusDays(2));
    }

    private void createPayOrders(Long ownerUserId, Map<String, SupplierEntity> suppliers) {
        PayOrderEntity draft = payOrderService.createForOwner(
            ownerUserId,
            new PayOrderService.CreateCommand(
                suppliers.get("supplier-b").getId(),
                null,
                5600.0,
                4,
                "BANK-PO-001",
                "工业配件账期付款待审核",
                PayOrderStatus.DRAFT.code()
            )
        );
        backdatePayOrder(ownerUserId, draft.getId(), nowMinusDays(21));

        PayOrderEntity paid = payOrderService.createForOwner(
            ownerUserId,
            new PayOrderService.CreateCommand(
                suppliers.get("supplier-a").getId(),
                null,
                4200.0,
                4,
                "BANK-PO-002",
                "已付款对账单",
                PayOrderStatus.PAID.code()
            )
        );
        backdatePayOrder(ownerUserId, paid.getId(), nowMinusDays(6));
    }

    private void patchInventoryForAnomalies(Long ownerUserId, Map<String, ProductEntity> products, long now) {
        ProductEntity glove = reloadProduct(ownerUserId, products.get("glove-a12").getId());
        glove.setStock(8.0);
        glove.setSafeStock(18.0);
        glove.setUpdatedAt(now);
        glove.setSyncVersion(glove.getSyncVersion() + 1);
        productRepository.save(glove);

        ProductEntity box = reloadProduct(ownerUserId, products.get("box-xl").getId());
        box.setStock(12.0);
        box.setSafeStock(20.0);
        box.setUpdatedAt(now);
        box.setSyncVersion(box.getSyncVersion() + 1);
        productRepository.save(box);
    }

    private ProductEntity reloadProduct(Long ownerUserId, Long productId) {
        return productRepository.findByIdAndOwnerUserId(productId, ownerUserId).orElseThrow();
    }

    private void backdateSaleOrder(Long ownerUserId, Long orderId, long createdAt, long updatedAt) {
        SaleOrderEntity order = saleOrderRepository.findByIdAndOwnerUserId(orderId, ownerUserId).orElseThrow();
        order.setCreatedAt(createdAt);
        order.setUpdatedAt(updatedAt);
        saleOrderRepository.save(order);
        List<SaleOrderItemEntity> items = saleOrderItemRepository.findByOwnerUserIdAndOrderId(ownerUserId, orderId);
        items.forEach(item -> item.setCreatedAt(createdAt));
        saleOrderItemRepository.saveAll(items);
    }

    private void backdateLatestPayment(Long ownerUserId, Long orderId, long createdAt) {
        List<PaymentEntity> payments = paymentRepository.findByOwnerUserIdAndOrderId(ownerUserId, orderId);
        if (payments.isEmpty()) {
            return;
        }
        PaymentEntity latest = payments.get(payments.size() - 1);
        for (int index = payments.size() - 1; index >= 0; index--) {
            PaymentEntity payment = payments.get(index);
            if (Objects.equals(payment.getType(), PaymentType.RECEIVE.code())) {
                latest = payment;
                break;
            }
        }
        latest.setCreatedAt(createdAt);
        paymentRepository.save(latest);
    }

    private void markRefundAsHistorical(Long ownerUserId, Long orderId, long createdAt) {
        List<PaymentEntity> payments = paymentRepository.findByOwnerUserIdAndOrderId(ownerUserId, orderId);
        List<PaymentEntity> refunds = new ArrayList<>();
        for (PaymentEntity payment : payments) {
            if (Objects.equals(payment.getType(), PaymentType.REFUND.code())) {
                payment.setCreatedAt(createdAt);
                refunds.add(payment);
            }
        }
        if (!refunds.isEmpty()) {
            paymentRepository.saveAll(refunds);
        }
        SaleOrderEntity order = saleOrderRepository.findByIdAndOwnerUserId(orderId, ownerUserId).orElseThrow();
        order.setCreatedAt(nowMinusDays(4));
        order.setUpdatedAt(createdAt);
        saleOrderRepository.save(order);
    }

    private void backdatePurchaseOrder(Long ownerUserId, Long orderId, long createdAt, long updatedAt) {
        PurchaseOrderEntity order = purchaseOrderRepository.findByIdAndOwnerUserId(orderId, ownerUserId).orElseThrow();
        order.setCreatedAt(createdAt);
        order.setUpdatedAt(updatedAt);
        purchaseOrderRepository.save(order);
        List<PurchaseOrderItemEntity> items = purchaseOrderItemRepository.findByOwnerUserIdAndOrderId(ownerUserId, orderId);
        items.forEach(item -> item.setCreatedAt(createdAt));
        purchaseOrderItemRepository.saveAll(items);
    }

    private void backdatePayOrder(Long ownerUserId, Long orderId, long createdAt) {
        PayOrderEntity order = payOrderRepository.findByIdAndOwnerUserId(orderId, ownerUserId).orElseThrow();
        order.setCreatedAt(createdAt);
        order.setUpdatedAt(createdAt);
        payOrderRepository.save(order);
    }

    private long nowMinusDays(long days) {
        return LocalDateTime.now()
            .minusDays(days)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli();
    }

    public record SeedResult(
        boolean mutated,
        long userCount,
        long productCount,
        long customerCount,
        long supplierCount,
        long saleOrderCount,
        long purchaseOrderCount,
        long payOrderCount,
        List<DemoAccount> demoAccounts
    ) {}

    public record DemoAccount(String phone, String password, String nickname) {}
}
