package com.zhihuiji.backend.application.service;

import com.zhihuiji.backend.domain.entity.AgentNotificationEntity;
import com.zhihuiji.backend.domain.entity.AgentTaskEntity;
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
        Map<String, SupplierEntity> suppliers = createSuppliers(now);
        Map<String, CustomerEntity> customers = createCustomers(now);
        Map<String, ProductEntity> products = createProducts(now);

        createPurchaseOrders(suppliers, products);
        createSaleOrders(customers, products);
        createPayOrders(suppliers);
        patchInventoryForAnomalies(products, now);
        createWarmAgentArtifacts();
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
        agentNotificationRepository.deleteAll();
        agentTaskRepository.deleteAll();
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

    private Map<String, SupplierEntity> createSuppliers(long now) {
        Map<String, SupplierEntity> map = new HashMap<>();
        map.put("supplier-a", createSupplier("供应商A", "13900010001", "华东供货中心", "常规补货", 12800.0, now));
        map.put("supplier-b", createSupplier("供应商B", "13900010002", "工业配件市场", "账期 15 天", 8600.0, now));
        map.put("supplier-c", createSupplier("供应商C", "13900010003", "包装耗材仓", "包装材料", 3200.0, now));
        return map;
    }

    private SupplierEntity createSupplier(
        String name,
        String phone,
        String address,
        String notes,
        double balance,
        long now
    ) {
        SupplierEntity supplier = new SupplierEntity();
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

    private Map<String, CustomerEntity> createCustomers(long now) {
        Map<String, CustomerEntity> map = new HashMap<>();
        map.put("customer-a", createCustomer("门店客户A", "13700020001", "静安门店", "核心连锁门店", 15800.0, now));
        map.put("customer-b", createCustomer("客户B", "13700020002", "嘉定工业园", "工业客户", 9200.0, now));
        map.put("customer-c", createCustomer("客户C", "13700020003", "闵行仓配点", "账期客户", 4100.0, now));
        return map;
    }

    private CustomerEntity createCustomer(
        String name,
        String phone,
        String address,
        String notes,
        double balance,
        long now
    ) {
        CustomerEntity customer = new CustomerEntity();
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

    private Map<String, ProductEntity> createProducts(long now) {
        Map<String, ProductEntity> map = new HashMap<>();
        map.put("sensor-s7", createProduct("S7-0021", "工业传感器 S7", "工业件", "个", 58.0, 35.0, 22.0, 30.0, now));
        map.put("glove-a12", createProduct("A12-0045", "绝缘手套 A12", "劳保", "只", 48.0, 26.0, 24.0, 18.0, now));
        map.put("box-xl", createProduct("XL-0012", "包装纸箱 XL", "耗材", "箱", 12.0, 5.0, 12.0, 20.0, now));
        map.put("scanner-q9", createProduct("Q9-1008", "扫码枪 Q9", "设备", "台", 168.0, 120.0, 31.0, 12.0, now));
        map.put("tape-b3", createProduct("B3-0811", "封箱胶带 B3", "耗材", "卷", 8.0, 3.0, 64.0, 30.0, now));
        return map;
    }

    private ProductEntity createProduct(
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

    private void createPurchaseOrders(Map<String, SupplierEntity> suppliers, Map<String, ProductEntity> products) {
        var received = purchaseOrderService.create(
            new PurchaseOrderService.CreatePurchaseOrderCommand(
                suppliers.get("supplier-a").getName(),
                List.of(
                    new PurchaseOrderService.PurchaseItemDraft(products.get("sensor-s7").getId(), null, null, 20.0, 34.0),
                    new PurchaseOrderService.PurchaseItemDraft(products.get("scanner-q9").getId(), null, null, 8.0, 118.0)
                ),
                "四月第一批补货",
                PurchaseOrderService.STATUS_RECEIVED
            )
        );
        backdatePurchaseOrder(received.order().getId(), nowMinusDays(12), nowMinusDays(12));

        var draft = purchaseOrderService.create(
            new PurchaseOrderService.CreatePurchaseOrderCommand(
                suppliers.get("supplier-c").getName(),
                List.of(
                    new PurchaseOrderService.PurchaseItemDraft(products.get("box-xl").getId(), null, null, 18.0, 4.6),
                    new PurchaseOrderService.PurchaseItemDraft(products.get("tape-b3").getId(), null, null, 40.0, 2.8)
                ),
                "包装耗材待到货",
                PurchaseOrderService.STATUS_DRAFT
            )
        );
        backdatePurchaseOrder(draft.order().getId(), nowMinusDays(5), nowMinusDays(5));
    }

    private void createSaleOrders(Map<String, CustomerEntity> customers, Map<String, ProductEntity> products) {
        var so1 = saleOrderService.create(
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
        backdateSaleOrder(so1.order().getId(), nowMinusDays(18), nowMinusDays(18));
        saleOrderService.addPayment(so1.order().getId(), 220.0, 1, "POS-A001");
        backdateLatestPayment(so1.order().getId(), nowMinusDays(17));

        var so2 = saleOrderService.create(
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
        backdateSaleOrder(so2.order().getId(), nowMinusDays(9), nowMinusDays(9));
        saleOrderService.addPayment(so2.order().getId(), 300.0, 2, "WX-B221");
        backdateLatestPayment(so2.order().getId(), nowMinusDays(8));

        var so3 = saleOrderService.create(
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
        backdateSaleOrder(so3.order().getId(), nowMinusDays(3), nowMinusDays(3));

        saleOrderService.cancel(so3.order().getId());
        markRefundAsHistorical(so3.order().getId(), nowMinusDays(2));
    }

    private void createPayOrders(Map<String, SupplierEntity> suppliers) {
        PayOrderEntity draft = payOrderService.create(
            new PayOrderService.CreateCommand(
                suppliers.get("supplier-b").getId(),
                null,
                5600.0,
                4,
                "BANK-PO-001",
                "工业配件账期付款待审核",
                PayOrderService.STATUS_DRAFT
            )
        );
        backdatePayOrder(draft.getId(), nowMinusDays(21));

        PayOrderEntity paid = payOrderService.create(
            new PayOrderService.CreateCommand(
                suppliers.get("supplier-a").getId(),
                null,
                4200.0,
                4,
                "BANK-PO-002",
                "已付款对账单",
                PayOrderService.STATUS_PAID
            )
        );
        backdatePayOrder(paid.getId(), nowMinusDays(6));
    }

    private void patchInventoryForAnomalies(Map<String, ProductEntity> products, long now) {
        ProductEntity glove = reloadProduct(products.get("glove-a12").getId());
        glove.setStock(8.0);
        glove.setSafeStock(18.0);
        glove.setUpdatedAt(now);
        glove.setSyncVersion(glove.getSyncVersion() + 1);
        productRepository.save(glove);

        ProductEntity box = reloadProduct(products.get("box-xl").getId());
        box.setStock(12.0);
        box.setSafeStock(20.0);
        box.setUpdatedAt(now);
        box.setSyncVersion(box.getSyncVersion() + 1);
        productRepository.save(box);
    }

    private ProductEntity reloadProduct(Long productId) {
        return productRepository.findById(productId).orElseThrow();
    }

    private void createWarmAgentArtifacts() {
        AgentTaskEntity task = new AgentTaskEntity();
        task.setTaskType("anomaly_watch");
        task.setTitle("历史异常巡检");
        task.setTriggerSource("scheduler");
        task.setStatus("completed");
        task.setProgress(100);
        task.setInputText("demo seed anomaly watch");
        task.setResultJson("{\"title\":\"历史异常巡检\",\"subtitle\":\"demo\",\"summary\":\"seeded\"}");
        task.setCreatedAt(nowMinusDays(1));
        task.setUpdatedAt(nowMinusDays(1));
        task.setCompletedAt(nowMinusDays(1));
        task = agentTaskRepository.save(task);

        AgentNotificationEntity notification = new AgentNotificationEntity();
        notification.setTaskId(task.getId());
        notification.setTitle("历史异常巡检已完成");
        notification.setBody("这是用于演示通知链路的种子数据。");
        notification.setLevel("info");
        notification.setIsRead(false);
        notification.setIsDelivered(false);
        notification.setCreatedAt(nowMinusDays(1));
        agentNotificationRepository.save(notification);
    }

    private void backdateSaleOrder(Long orderId, long createdAt, long updatedAt) {
        SaleOrderEntity order = saleOrderRepository.findById(orderId).orElseThrow();
        order.setCreatedAt(createdAt);
        order.setUpdatedAt(updatedAt);
        saleOrderRepository.save(order);
        List<SaleOrderItemEntity> items = saleOrderItemRepository.findByOrderId(orderId);
        items.forEach(item -> item.setCreatedAt(createdAt));
        saleOrderItemRepository.saveAll(items);
    }

    private void backdateLatestPayment(Long orderId, long createdAt) {
        List<PaymentEntity> payments = paymentRepository.findByOrderId(orderId);
        if (payments.isEmpty()) {
            return;
        }
        PaymentEntity latest = payments.stream()
            .filter(payment -> Objects.equals(payment.getType(), SaleOrderService.PAYMENT_TYPE_RECEIVE))
            .reduce((left, right) -> right)
            .orElse(payments.get(payments.size() - 1));
        latest.setCreatedAt(createdAt);
        paymentRepository.save(latest);
    }

    private void markRefundAsHistorical(Long orderId, long createdAt) {
        List<PaymentEntity> payments = paymentRepository.findByOrderId(orderId);
        payments.stream()
            .filter(payment -> Objects.equals(payment.getType(), SaleOrderService.PAYMENT_TYPE_REFUND))
            .forEach(payment -> payment.setCreatedAt(createdAt));
        paymentRepository.saveAll(payments);
        SaleOrderEntity order = saleOrderRepository.findById(orderId).orElseThrow();
        order.setCreatedAt(nowMinusDays(4));
        order.setUpdatedAt(createdAt);
        saleOrderRepository.save(order);
    }

    private void backdatePurchaseOrder(Long orderId, long createdAt, long updatedAt) {
        PurchaseOrderEntity order = purchaseOrderRepository.findById(orderId).orElseThrow();
        order.setCreatedAt(createdAt);
        order.setUpdatedAt(updatedAt);
        purchaseOrderRepository.save(order);
        List<PurchaseOrderItemEntity> items = purchaseOrderItemRepository.findByOrderId(orderId);
        items.forEach(item -> item.setCreatedAt(createdAt));
        purchaseOrderItemRepository.saveAll(items);
    }

    private void backdatePayOrder(Long orderId, long createdAt) {
        PayOrderEntity order = payOrderRepository.findById(orderId).orElseThrow();
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
