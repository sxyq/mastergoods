package com.zhihuiji.backend.application.service;

import com.zhihuiji.backend.api.common.IdGenerator;
import com.zhihuiji.backend.api.common.OrderStatus;
import com.zhihuiji.backend.api.common.PayOrderStatus;
import com.zhihuiji.backend.api.common.PaymentType;
import com.zhihuiji.backend.api.common.PurchaseOrderStatus;
import com.zhihuiji.backend.domain.entity.AccountEntity;
import com.zhihuiji.backend.domain.entity.CustomerEntity;
import com.zhihuiji.backend.domain.entity.FinanceRecordEntity;
import com.zhihuiji.backend.domain.entity.InventorySnapshotEntity;
import com.zhihuiji.backend.domain.entity.PayOrderEntity;
import com.zhihuiji.backend.domain.entity.PaymentEntity;
import com.zhihuiji.backend.domain.entity.product.ProductEntity;
import com.zhihuiji.backend.domain.entity.PurchaseOrderEntity;
import com.zhihuiji.backend.domain.entity.PurchaseOrderItemEntity;
import com.zhihuiji.backend.domain.entity.SaleOrderEntity;
import com.zhihuiji.backend.domain.entity.SaleOrderItemEntity;
import com.zhihuiji.backend.domain.entity.SupplierEntity;
import com.zhihuiji.backend.domain.entity.UserEntity;
import com.zhihuiji.backend.infrastructure.repository.AccountRepository;
import com.zhihuiji.backend.infrastructure.repository.CustomerRepository;
import com.zhihuiji.backend.infrastructure.repository.FinanceRecordRepository;
import com.zhihuiji.backend.infrastructure.repository.InventoryAdjustmentRepository;
import com.zhihuiji.backend.infrastructure.repository.InventorySnapshotRepository;
import com.zhihuiji.backend.infrastructure.repository.PayOrderRepository;
import com.zhihuiji.backend.infrastructure.repository.PaymentRepository;
import com.zhihuiji.backend.infrastructure.repository.product.ProductRepository;
import com.zhihuiji.backend.infrastructure.repository.PurchaseOrderItemRepository;
import com.zhihuiji.backend.infrastructure.repository.PurchaseOrderRepository;
import com.zhihuiji.backend.infrastructure.repository.SaleOrderItemRepository;
import com.zhihuiji.backend.infrastructure.repository.SaleOrderRepository;
import com.zhihuiji.backend.infrastructure.repository.SessionRepository;
import com.zhihuiji.backend.infrastructure.repository.SupplierRepository;
import com.zhihuiji.backend.infrastructure.repository.UserRepository;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LegacySQLiteImportService {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final ZoneId ZONE_ID = ZoneId.of("Asia/Shanghai");

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
    private final PayOrderRepository payOrderRepository;
    private final FinanceRecordRepository financeRecordRepository;
    private final AccountRepository accountRepository;
    private final InventorySnapshotRepository inventorySnapshotRepository;
    private final InventoryAdjustmentRepository inventoryAdjustmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final IdGenerator idGenerator;

    public LegacySQLiteImportService(
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
        PayOrderRepository payOrderRepository,
        FinanceRecordRepository financeRecordRepository,
        AccountRepository accountRepository,
        InventorySnapshotRepository inventorySnapshotRepository,
        InventoryAdjustmentRepository inventoryAdjustmentRepository,
        PasswordEncoder passwordEncoder,
        IdGenerator idGenerator
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
        this.payOrderRepository = payOrderRepository;
        this.financeRecordRepository = financeRecordRepository;
        this.accountRepository = accountRepository;
        this.inventorySnapshotRepository = inventorySnapshotRepository;
        this.inventoryAdjustmentRepository = inventoryAdjustmentRepository;
        this.passwordEncoder = passwordEncoder;
        this.idGenerator = idGenerator;
    }

    @Transactional
    public ImportResult importIntoFirstAccount(ImportRequest request) {
        validateRequest(request);
        Path dbPath = Path.of(request.legacyDbPath()).toAbsolutePath().normalize();
        if (!Files.exists(dbPath)) {
            throw new IllegalArgumentException("旧版数据库不存在: " + dbPath);
        }
        if (!Files.isReadable(dbPath)) {
            throw new IllegalArgumentException("旧版数据库不可读: " + dbPath);
        }

        UserEntity user = userRepository.findByPhone(request.phone().trim()).orElseGet(() -> createUser(request));
        if (Boolean.TRUE.equals(request.resetOwnedData())) {
            clearOwnedData(user.getId(), true);
        }

        return importIntoOwner(user.getId(), user.getPhone(), user.getNickname(), dbPath);
    }

    @Transactional
    public ImportResult importIntoExistingOwner(Long ownerUserId, ExistingOwnerImportRequest request) {
        if (ownerUserId == null) {
            throw new IllegalArgumentException("ownerUserId 不能为空");
        }
        validateExistingOwnerRequest(request);
        Path dbPath = Path.of(request.legacyDbPath()).toAbsolutePath().normalize();
        if (!Files.exists(dbPath)) {
            throw new IllegalArgumentException("旧版数据库不存在: " + dbPath);
        }
        if (!Files.isReadable(dbPath)) {
            throw new IllegalArgumentException("旧版数据库不可读: " + dbPath);
        }
        UserEntity owner = userRepository.findById(ownerUserId)
            .orElseThrow(() -> new IllegalArgumentException("目标账号不存在"));
        if (Boolean.TRUE.equals(request.resetOwnedData())) {
            clearOwnedData(ownerUserId, false);
        }
        return importIntoOwner(ownerUserId, owner.getPhone(), owner.getNickname(), dbPath);
    }

    private UserEntity createUser(ImportRequest request) {
        long now = System.currentTimeMillis();
        UserEntity user = new UserEntity();
        user.setPhone(request.phone().trim());
        user.setPasswordHash(passwordEncoder.encode(request.password().trim()));
        user.setNickname(request.nickname().trim());
        user.setStatus(1);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        return userRepository.save(user);
    }

    private ImportResult importIntoOwner(
        Long ownerUserId,
        String phone,
        String nickname,
        Path dbPath
    ) {
        ImportCounters counters = new ImportCounters();
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath)) {
            Map<Long, ImportedAccount> accountMap = importAccounts(connection, ownerUserId, counters);
            CompanyImportContext companyContext = importCompanies(connection, ownerUserId, counters);
            ProductImportContext productContext = importProducts(connection, ownerUserId, counters);
            importSaleOrders(connection, ownerUserId, productContext, companyContext, accountMap, counters);
            importPurchaseOrders(connection, ownerUserId, productContext, companyContext, accountMap, counters);
            importFinanceRecords(connection, ownerUserId, companyContext, accountMap, counters);
        } catch (SQLException exception) {
            throw new IllegalStateException("导入旧版 SQLite 失败: " + exception.getMessage(), exception);
        }

        return new ImportResult(
            ownerUserId,
            phone,
            nickname,
            dbPath.toString(),
            counters.accounts,
            counters.customers,
            counters.suppliers,
            counters.products,
            counters.saleOrders,
            counters.saleOrderItems,
            counters.payments,
            counters.purchaseOrders,
            counters.purchaseOrderItems,
            counters.payOrders,
            counters.financeRecords,
            counters.inventorySnapshots
        );
    }

    private void clearOwnedData(Long ownerUserId, boolean clearSessions) {
        paymentRepository.deleteAll(paymentRepository.findAllByOwnerUserId(ownerUserId));
        saleOrderItemRepository.deleteAll(saleOrderItemRepository.findAllByOwnerUserIdOrderByCreatedAtAsc(ownerUserId));
        saleOrderRepository.deleteAll(saleOrderRepository.findAllByOwnerUserId(ownerUserId));
        purchaseOrderItemRepository.deleteAll(purchaseOrderItemRepository.findAllByOwnerUserIdOrderByCreatedAtAsc(ownerUserId));
        purchaseOrderRepository.deleteAll(purchaseOrderRepository.findAllByOwnerUserId(ownerUserId));
        payOrderRepository.deleteAll(payOrderRepository.findAllByOwnerUserId(ownerUserId));
        financeRecordRepository.deleteAll(financeRecordRepository.findAllByOwnerUserIdOrderByUpdatedAtAscIdAsc(ownerUserId));
        inventorySnapshotRepository.deleteAll(inventorySnapshotRepository.findAllByOwnerUserIdOrderBySnapshotDateAscIdAsc(ownerUserId));
        inventoryAdjustmentRepository.deleteAll(inventoryAdjustmentRepository.findByOwnerUserIdOrderByCreatedAtAsc(ownerUserId));
        accountRepository.deleteAll(accountRepository.findAllByOwnerUserIdOrderByUpdatedAtAscIdAsc(ownerUserId));
        productRepository.deleteAll(productRepository.findAllByOwnerUserId(ownerUserId));
        customerRepository.deleteAll(customerRepository.findAllByOwnerUserId(ownerUserId));
        supplierRepository.deleteAll(supplierRepository.findAllByOwnerUserId(ownerUserId));
        if (clearSessions) {
            sessionRepository.deleteByUserId(ownerUserId);
        }
    }

    private Map<Long, ImportedAccount> importAccounts(Connection connection, Long ownerUserId, ImportCounters counters) throws SQLException {
        Map<Long, ImportedAccount> accountMap = new HashMap<>(64);
        Set<String> usedAccountCodes = new HashSet<>(64);
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("select * from accts where is_del = 0 order by id")) {
            while (rs.next()) {
                long legacyId = rs.getLong("id");
                String legacySyncGroup = rs.getString("sync_g");
                String legacyName = rs.getString("name");
                String createdAtText = rs.getString("create_at");
                String revisedAtText = rs.getString("revise_at");
                AccountEntity account = new AccountEntity();
                account.setOwnerUserId(ownerUserId);
                account.setCode(
                    ensureUniqueIdentifier(
                        buildLegacyCode("ACCT", legacyId, legacySyncGroup),
                        "ACCT",
                        legacyId,
                        usedAccountCodes
                    )
                );
                account.setName(nonBlank(legacyName, "旧版账户-" + legacyId));
                account.setType(normalizeAccountType(rs.getInt("tye")));
                account.setBalance(rs.getDouble("cur_amt"));
                account.setIsDefault(rs.getInt("seq") == 1 || rs.getInt("is_sys") == 1);
                account.setStatus(rs.getInt("is_stop") == 1 ? 0 : 1);
                account.setSortOrder(rs.getInt("seq"));
                account.setNotes(blankToNull(rs.getString("remark")));
                account.setCreatedAt(parseTimestampOrNow(createdAtText));
                account.setUpdatedAt(parseTimestampOrFallback(revisedAtText, account.getCreatedAt()));
                account = accountRepository.save(account);
                accountMap.put(legacyId, new ImportedAccount(account.getId(), account.getName(), account.getType()));
                counters.accounts++;
            }
        }
        return accountMap;
    }

    private CompanyImportContext importCompanies(Connection connection, Long ownerUserId, ImportCounters counters) throws SQLException {
        Map<Long, ImportedCompany> customerMap = new HashMap<>(128);
        Map<Long, ImportedCompany> supplierMap = new HashMap<>(128);
        Set<String> usedCustomerPhones = new HashSet<>(128);
        Set<String> usedSupplierPhones = new HashSet<>(128);
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("select * from companies where is_del = 0 order by id")) {
            while (rs.next()) {
                long oldCompanyId = rs.getLong("id");
                String legacyName = rs.getString("name");
                String createdAtText = rs.getString("create_at");
                String revisedAtText = rs.getString("revise_at");
                String linkman = rs.getString("linkman");
                String mobile = rs.getString("mobile");
                String tel = rs.getString("tel");
                String addr = rs.getString("addr");
                String remark = rs.getString("remark");
                boolean supplier = isSupplierCompany(rs.getInt("tye"), rs.getInt("company_type_id"), legacyName);
                String name = nonBlank(legacyName, supplier ? "旧版供应商-" + oldCompanyId : "旧版客户-" + oldCompanyId);
                long createdAt = parseTimestampOrNow(createdAtText);
                long updatedAt = parseTimestampOrFallback(revisedAtText, createdAt);
                String contactName = blankToNull(linkman);
                String contactPhone = blankToNull(nonBlank(mobile, tel));
                String address = blankToNull(addr);
                String notes = blankToNull(remark);
                double balance = rs.getDouble("cur_amt");
                int status = rs.getInt("is_stop") == 1 ? 0 : 1;

                if (supplier) {
                    SupplierEntity entity = new SupplierEntity();
                    entity.setOwnerUserId(ownerUserId);
                    entity.setName(name);
                    entity.setPhone(normalizeLegacyPartnerPhone(contactPhone, "LS", oldCompanyId, usedSupplierPhones));
                    entity.setAddress(address);
                    entity.setNotes(notes);
                    entity.setContactName(contactName);
                    entity.setContactPhone(contactPhone);
                    entity.setBalance(balance);
                    entity.setStatus(status);
                    entity.setSyncStatus(0);
                    entity.setSyncVersion(1L);
                    entity.setCreatedAt(createdAt);
                    entity.setUpdatedAt(updatedAt);
                    entity = supplierRepository.save(entity);
                    supplierMap.put(oldCompanyId, new ImportedCompany(entity.getId(), entity.getName()));
                    counters.suppliers++;
                } else {
                    CustomerEntity entity = new CustomerEntity();
                    entity.setOwnerUserId(ownerUserId);
                    entity.setName(name);
                    entity.setPhone(normalizeLegacyPartnerPhone(contactPhone, "LC", oldCompanyId, usedCustomerPhones));
                    entity.setLevel(Math.max(1, rs.getInt("prc_level")));
                    entity.setAddress(address);
                    entity.setNotes(notes);
                    entity.setContactName(contactName);
                    entity.setContactPhone(contactPhone);
                    entity.setBalance(balance);
                    entity.setStatus(status);
                    entity.setSyncStatus(0);
                    entity.setSyncVersion(1L);
                    entity.setCreatedAt(createdAt);
                    entity.setUpdatedAt(updatedAt);
                    entity = customerRepository.save(entity);
                    customerMap.put(oldCompanyId, new ImportedCompany(entity.getId(), entity.getName()));
                    counters.customers++;
                }
            }
        }
        return new CompanyImportContext(customerMap, supplierMap);
    }

    private ProductImportContext importProducts(Connection connection, Long ownerUserId, ImportCounters counters) throws SQLException {
        Map<Long, ImportedProduct> productMap = new HashMap<>(128);
        Set<String> usedProductCodes = new HashSet<>(128);
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("select * from products where is_del = 0 order by id")) {
            while (rs.next()) {
                long legacyId = rs.getLong("id");
                String legacyCode = rs.getString("code");
                String syncGroup = rs.getString("sync_g");
                String legacyName = rs.getString("name");
                String createdAtText = rs.getString("create_at");
                String revisedAtText = rs.getString("revise_at");
                ProductEntity entity = new ProductEntity();
                entity.setOwnerUserId(ownerUserId);
                entity.setCode(
                    ensureUniqueIdentifier(
                        nonBlank(legacyCode, buildLegacyCode("PROD", legacyId, syncGroup)),
                        "PROD",
                        legacyId,
                        usedProductCodes
                    )
                );
                entity.setName(nonBlank(legacyName, "旧版商品-" + legacyId));
                entity.setCategory(normalizeCategory(rs.getInt("ptype_id")));
                entity.setUnit(nonBlank(rs.getString("unit"), "件"));
                entity.setSalePrice(firstPositive(rs.getDouble("sale_prc"), rs.getDouble("trade_prc"), rs.getDouble("prc4")));
                entity.setPurchasePrice(firstPositive(rs.getDouble("pur_prc"), rs.getDouble("last_prc"), rs.getDouble("init_prc")));
                entity.setStock(rs.getDouble("cur_stock"));
                entity.setSafeStock(Math.max(0.0, rs.getDouble("min_stock")));
                entity.setStatus(rs.getInt("is_stop") == 1 ? 0 : 1);
                entity.setSyncStatus(0);
                entity.setSyncVersion(1L);
                entity.setCreatedAt(parseTimestampOrNow(createdAtText));
                entity.setUpdatedAt(parseTimestampOrFallback(revisedAtText, entity.getCreatedAt()));
                entity = productRepository.save(entity);
                productMap.put(
                    legacyId,
                    new ImportedProduct(entity.getId(), entity.getCode(), entity.getName(), entity.getPurchasePrice(), entity.getStock(), entity.getUpdatedAt())
                );
                counters.products++;

                InventorySnapshotEntity snapshot = new InventorySnapshotEntity();
                snapshot.setOwnerUserId(ownerUserId);
                snapshot.setProductId(entity.getId());
                snapshot.setProductCode(entity.getCode());
                snapshot.setProductName(entity.getName());
                snapshot.setQuantity(entity.getStock());
                snapshot.setUnitCost(entity.getPurchasePrice());
                snapshot.setTotalValue(entity.getStock() * entity.getPurchasePrice());
                snapshot.setSnapshotDate(normalizeDayMillis(entity.getUpdatedAt()));
                snapshot.setCreatedAt(entity.getUpdatedAt());
                inventorySnapshotRepository.save(snapshot);
                counters.inventorySnapshots++;
            }
        }
        return new ProductImportContext(productMap);
    }

    private void importSaleOrders(
        Connection connection,
        Long ownerUserId,
        ProductImportContext productContext,
        CompanyImportContext companyContext,
        Map<Long, ImportedAccount> accountMap,
        ImportCounters counters
    ) throws SQLException {
        Map<Long, ImportedSaleOrder> saleOrderMap = new HashMap<>(128);
        Set<String> usedSaleOrderNos = new HashSet<>(128);
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("select * from sales where is_del = 0 order by id")) {
            while (rs.next()) {
                long oldSaleId = rs.getLong("id");
                ImportedCompany customer = companyContext.customerByOldId.get(rs.getLong("company_id"));
                long createdAt = parseTimestampOrFallback(rs.getString("create_at"), parseDateOrNow(rs.getString("opt_on")));
                long updatedAt = parseTimestampOrFallback(rs.getString("revise_at"), createdAt);
                double gross = positiveOrZero(rs.getDouble("bill_amt"));
                double discounted = firstPositive(rs.getDouble("disc_amt"), gross, 0.0);
                double express = positiveOrZero(rs.getDouble("express_amt"));
                double deduction = positiveOrZero(rs.getDouble("deduction_amt"));
                double total = Math.max(0.0, discounted + express - deduction);
                double paid = Math.min(total, positiveOrZero(rs.getDouble("pay_amt")));

                SaleOrderEntity entity = new SaleOrderEntity();
                entity.setId(idGenerator.nextId());
                entity.setOwnerUserId(ownerUserId);
                entity.setOrderNo(
                    ensureUniqueIdentifier(
                        nonBlank(rs.getString("code"), "LEGACY-SALE-" + oldSaleId),
                        "SALE",
                        oldSaleId,
                        usedSaleOrderNos
                    )
                );
                entity.setCustomerId(customer == null ? null : customer.id());
                entity.setCustomerName(customer == null ? null : customer.name());
                entity.setSubtotalAmount(gross);
                entity.setDiscountAmount(Math.max(0.0, gross + express - total));
                entity.setTotalAmount(total);
                entity.setPaidAmount(paid);
                entity.setNotes(blankToNull(rs.getString("remark")));
                entity.setStatus(resolveSaleOrderStatus(total, paid));
                entity.setSyncStatus(0);
                entity.setSyncVersion(1L);
                entity.setCreatedAt(createdAt);
                entity.setUpdatedAt(updatedAt);
                saleOrderRepository.save(entity);
                saleOrderMap.put(oldSaleId, new ImportedSaleOrder(entity.getId(), entity.getCustomerId(), entity.getCustomerName(), entity.getCreatedAt(), entity.getOrderNo()));
                counters.saleOrders++;

                if (paid > 0.0) {
                    ImportedAccount account = accountMap.get(rs.getLong("acct_id"));
                    PaymentEntity payment = new PaymentEntity();
                    payment.setId(idGenerator.nextId());
                    payment.setOwnerUserId(ownerUserId);
                    payment.setOrderId(entity.getId());
                    payment.setAmount(paid);
                    payment.setMethod(account == null ? 1 : normalizeMethodFromAccountType(account.type()));
                    payment.setReferenceNo(entity.getOrderNo());
                    payment.setType(PaymentType.RECEIVE.code());
                    payment.setCreatedAt(createdAt);
                    paymentRepository.save(payment);
                    counters.payments++;
                }
            }
        }

        try (PreparedStatement statement = connection.prepareStatement("select * from saleitems where is_del = 0 order by id");
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                ImportedSaleOrder order = saleOrderMap.get(rs.getLong("sale_id"));
                ImportedProduct product = productContext.productByOldId.get(rs.getLong("product_id"));
                if (order == null || product == null) {
                    continue;
                }
                SaleOrderItemEntity item = new SaleOrderItemEntity();
                item.setId(idGenerator.nextId());
                item.setOwnerUserId(ownerUserId);
                item.setOrderId(order.id());
                item.setProductId(product.id());
                item.setProductCode(product.code());
                item.setProductName(product.name());
                item.setCustomerId(order.customerId());
                item.setCustomerName(order.customerName());
                item.setQuantity(positiveOrZero(rs.getDouble("qty")));
                item.setUnitPrice(firstPositive(rs.getDouble("prc"), rs.getDouble("oprc"), 0.0));
                item.setAmount(firstPositive(rs.getDouble("amt"), item.getQuantity() * item.getUnitPrice(), 0.0));
                item.setCreatedAt(order.createdAt());
                saleOrderItemRepository.save(item);
                counters.saleOrderItems++;
            }
        }
    }

    private void importPurchaseOrders(
        Connection connection,
        Long ownerUserId,
        ProductImportContext productContext,
        CompanyImportContext companyContext,
        Map<Long, ImportedAccount> accountMap,
        ImportCounters counters
    ) throws SQLException {
        Map<Long, ImportedPurchaseOrder> purchaseOrderMap = new HashMap<>(128);
        Set<String> usedPurchaseOrderNos = new HashSet<>(128);
        Set<String> usedPayOrderNos = new HashSet<>(128);
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("select * from purs where is_del = 0 order by id")) {
            while (rs.next()) {
                long oldPurchaseId = rs.getLong("id");
                ImportedCompany supplier = companyContext.supplierByOldId.get(rs.getLong("company_id"));
                long createdAt = parseTimestampOrFallback(rs.getString("create_at"), parseDateOrNow(rs.getString("opt_on")));
                long updatedAt = parseTimestampOrFallback(rs.getString("revise_at"), createdAt);
                double gross = positiveOrZero(rs.getDouble("bill_amt"));
                double discounted = firstPositive(rs.getDouble("disc_amt"), gross, 0.0);
                double express = positiveOrZero(rs.getDouble("express_amt"));
                double deduction = positiveOrZero(rs.getDouble("deduction_amt"));
                double total = Math.max(0.0, discounted + express - deduction);
                double paid = Math.min(total, positiveOrZero(rs.getDouble("pay_amt")));
                int status = rs.getInt("is_calc") == 1 ? PurchaseOrderStatus.RECEIVED.code() : PurchaseOrderStatus.DRAFT.code();

                PurchaseOrderEntity entity = new PurchaseOrderEntity();
                entity.setId(idGenerator.nextId());
                entity.setOwnerUserId(ownerUserId);
                entity.setOrderNo(
                    ensureUniqueIdentifier(
                        nonBlank(rs.getString("code"), "LEGACY-PURCHASE-" + oldPurchaseId),
                        "PUR",
                        oldPurchaseId,
                        usedPurchaseOrderNos
                    )
                );
                entity.setSupplierId(supplier == null ? null : supplier.id());
                entity.setSupplierName(supplier == null ? "旧版供应商" : supplier.name());
                entity.setTotalAmount(total);
                entity.setPaidAmount(paid);
                entity.setReceivedAmount(status == PurchaseOrderStatus.RECEIVED.code() ? total : 0.0);
                entity.setNotes(blankToNull(rs.getString("remark")));
                entity.setStatus(status);
                entity.setSyncStatus(0);
                entity.setSyncVersion(1L);
                entity.setCreatedAt(createdAt);
                entity.setUpdatedAt(updatedAt);
                purchaseOrderRepository.save(entity);
                purchaseOrderMap.put(oldPurchaseId, new ImportedPurchaseOrder(entity.getId(), entity.getSupplierId(), entity.getSupplierName(), entity.getCreatedAt()));
                counters.purchaseOrders++;

                if (paid > 0.0) {
                    ImportedAccount account = accountMap.get(rs.getLong("acct_id"));
                    PayOrderEntity payOrder = new PayOrderEntity();
                    payOrder.setId(idGenerator.nextId());
                    payOrder.setOwnerUserId(ownerUserId);
                    payOrder.setOrderNo(
                        ensureUniqueIdentifier(
                            "LEGACY-POUT-" + oldPurchaseId,
                            "POUT",
                            oldPurchaseId,
                            usedPayOrderNos
                        )
                    );
                    payOrder.setSupplierId(entity.getSupplierId());
                    payOrder.setSupplierName(entity.getSupplierName());
                    payOrder.setAmount(paid);
                    payOrder.setMethod(account == null ? 1 : normalizeMethodFromAccountType(account.type()));
                    payOrder.setReferenceNo(entity.getOrderNo());
                    payOrder.setNotes("由旧版采购单付款信息迁移");
                    payOrder.setAccountId(account == null ? null : account.id());
                    payOrder.setStatus(PayOrderStatus.PAID.code());
                    payOrder.setCreatedAt(createdAt);
                    payOrder.setUpdatedAt(updatedAt);
                    payOrder.setSyncStatus(0);
                    payOrder.setSyncVersion(1L);
                    payOrderRepository.save(payOrder);
                    counters.payOrders++;
                }
            }
        }

        try (PreparedStatement statement = connection.prepareStatement("select * from puritems where is_del = 0 order by id");
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                ImportedPurchaseOrder order = purchaseOrderMap.get(rs.getLong("pur_id"));
                ImportedProduct product = productContext.productByOldId.get(rs.getLong("product_id"));
                if (order == null || product == null) {
                    continue;
                }
                PurchaseOrderItemEntity item = new PurchaseOrderItemEntity();
                item.setId(idGenerator.nextId());
                item.setOwnerUserId(ownerUserId);
                item.setOrderId(order.id());
                item.setProductId(product.id());
                item.setProductCode(product.code());
                item.setProductName(product.name());
                item.setQuantity(positiveOrZero(rs.getDouble("qty")));
                item.setUnitCost(firstPositive(rs.getDouble("prc"), rs.getDouble("oprc"), product.purchasePrice()));
                item.setAmount(firstPositive(rs.getDouble("amt"), item.getQuantity() * item.getUnitCost(), 0.0));
                item.setCreatedAt(order.createdAt());
                purchaseOrderItemRepository.save(item);
                counters.purchaseOrderItems++;
            }
        }
    }

    private void importFinanceRecords(
        Connection connection,
        Long ownerUserId,
        CompanyImportContext companyContext,
        Map<Long, ImportedAccount> accountMap,
        ImportCounters counters
    ) throws SQLException {
        Set<String> usedRecordNos = new HashSet<>(128);
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("select * from funds where is_del = 0 order by id")) {
            while (rs.next()) {
                long legacyId = rs.getLong("id");
                String legacyCode = rs.getString("code");
                String createAtText = rs.getString("create_at");
                String optOnText = rs.getString("opt_on");
                String reviseAtText = rs.getString("revise_at");
                double income = positiveOrZero(rs.getDouble("in_amt"));
                double expense = positiveOrZero(rs.getDouble("out_amt"));
                double amount = income > 0.0 ? income : expense;
                if (amount <= 0.0) {
                    continue;
                }
                FinanceRecordEntity entity = new FinanceRecordEntity();
                entity.setId(idGenerator.nextId());
                entity.setOwnerUserId(ownerUserId);
                entity.setRecordNo(
                    ensureUniqueIdentifier(
                        nonBlank(legacyCode, "LEGACY-FUND-" + legacyId),
                        "FUND",
                        legacyId,
                        usedRecordNos
                    )
                );
                entity.setType(income > 0.0 ? 1 : 2);
                entity.setCategory(resolveFundCategory(rs.getInt("tye"), entity.getType()));
                entity.setPartnerName(resolvePartnerName(rs.getLong("company_id"), companyContext));
                entity.setAmount(amount);
                ImportedAccount account = accountMap.get(rs.getLong("acct_id"));
                entity.setMethod(account == null ? 1 : normalizeMethodFromAccountType(account.type()));
                entity.setNotes(blankToNull(rs.getString("remark")));
                entity.setCreatedAt(parseTimestampOrFallback(createAtText, parseDateOrNow(optOnText)));
                entity.setUpdatedAt(parseTimestampOrFallback(reviseAtText, entity.getCreatedAt()));
                entity.setSyncStatus(0);
                entity.setSyncVersion(1L);
                financeRecordRepository.save(entity);
                counters.financeRecords++;
            }
        }
    }

    private boolean isSupplierCompany(int tye, int companyTypeId, String name) {
        if (tye == 2 || companyTypeId == 2) {
            return true;
        }
        String normalized = safeLower(name);
        return normalized.contains("供应商");
    }

    private int resolveSaleOrderStatus(double total, double paid) {
        if (total <= 0.000001 || paid + 0.000001 >= total) {
            return OrderStatus.COMPLETED.code();
        }
        return OrderStatus.CONFIRMED.code();
    }

    private int normalizeAccountType(int legacyType) {
        return switch (legacyType) {
            case 2 -> 2;
            case 4 -> 4;
            default -> 1;
        };
    }

    private int normalizeMethodFromAccountType(int accountType) {
        return switch (accountType) {
            case 2 -> 2;
            case 4 -> 3;
            default -> 1;
        };
    }

    private String resolveFundCategory(int legacyType, int financeType) {
        if (financeType == 1) {
            return switch (legacyType) {
                case 2 -> "收款";
                case 3 -> "转入";
                default -> "收入";
            };
        }
        return switch (legacyType) {
            case 1 -> "付款";
            case 3 -> "转出";
            default -> "支出";
        };
    }

    private String resolvePartnerName(long oldCompanyId, CompanyImportContext companyContext) {
        ImportedCompany customer = companyContext.customerByOldId.get(oldCompanyId);
        if (customer != null) {
            return customer.name();
        }
        ImportedCompany supplier = companyContext.supplierByOldId.get(oldCompanyId);
        return supplier == null ? null : supplier.name();
    }

    private String normalizeCategory(int legacyType) {
        return legacyType > 0 ? "分类-" + legacyType : "默认分类";
    }

    private long normalizeDayMillis(long millis) {
        return LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(millis), ZONE_ID)
            .toLocalDate()
            .atStartOfDay(ZONE_ID)
            .toInstant()
            .toEpochMilli();
    }

    private long parseDateOrNow(String raw) {
        if (raw == null || raw.isBlank()) {
            return System.currentTimeMillis();
        }
        try {
            return LocalDate.parse(raw.trim(), DATE_FORMATTER).atStartOfDay(ZONE_ID).toInstant().toEpochMilli();
        } catch (DateTimeParseException ignored) {
            return System.currentTimeMillis();
        }
    }

    private long parseTimestampOrNow(String raw) {
        if (raw == null || raw.isBlank()) {
            return System.currentTimeMillis();
        }
        try {
            return LocalDateTime.parse(raw.trim(), DATE_TIME_FORMATTER).atZone(ZONE_ID).toInstant().toEpochMilli();
        } catch (DateTimeParseException ignored) {
            return parseDateOrNow(raw);
        }
    }

    private long parseTimestampOrFallback(String raw, long fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return LocalDateTime.parse(raw.trim(), DATE_TIME_FORMATTER).atZone(ZONE_ID).toInstant().toEpochMilli();
        } catch (DateTimeParseException ignored) {
            try {
                return LocalDate.parse(raw.trim(), DATE_FORMATTER).atStartOfDay(ZONE_ID).toInstant().toEpochMilli();
            } catch (DateTimeParseException ignoredAgain) {
                return fallback;
            }
        }
    }

    private double positiveOrZero(double value) {
        return value < 0.0 ? 0.0 : value;
    }

    private double firstPositive(double primary, double secondary, double fallback) {
        if (primary > 0.0) {
            return primary;
        }
        if (secondary > 0.0) {
            return secondary;
        }
        return Math.max(0.0, fallback);
    }

    private String buildLegacyCode(String prefix, long id, String syncG) {
        String normalizedSync = blankToNull(syncG);
        if (normalizedSync != null) {
            return prefix + "-" + normalizedSync.substring(0, Math.min(normalizedSync.length(), 16)).toUpperCase(Locale.ROOT);
        }
        return prefix + "-" + id;
    }

    private String safeLower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String blankToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeLegacyPartnerPhone(String phone, String prefix, long legacyId, Set<String> usedPhones) {
        String normalized = blankToNull(phone);
        if (normalized == null) {
            normalized = prefix + "-" + legacyId;
        }
        if (usedPhones.add(normalized)) {
            return normalized;
        }
        String deduplicated = prefix + "-" + legacyId;
        if (usedPhones.add(deduplicated)) {
            return deduplicated;
        }
        long collisionIndex = 2L;
        while (true) {
            String candidate = deduplicated + "-" + collisionIndex;
            if (usedPhones.add(candidate)) {
                return candidate;
            }
            collisionIndex++;
        }
    }

    private String ensureUniqueIdentifier(String rawValue, String prefix, long legacyId, Set<String> usedValues) {
        String normalized = blankToNull(rawValue);
        if (normalized == null) {
            normalized = prefix + "-" + legacyId;
        }
        if (usedValues.add(normalized)) {
            return normalized;
        }
        String deduplicated = prefix + "-" + legacyId;
        if (usedValues.add(deduplicated)) {
            return deduplicated;
        }
        long collisionIndex = 2L;
        while (true) {
            String candidate = deduplicated + "-" + collisionIndex;
            if (usedValues.add(candidate)) {
                return candidate;
            }
            collisionIndex++;
        }
    }

    private String nonBlank(String preferred, String fallback) {
        String normalized = blankToNull(preferred);
        return normalized == null ? fallback : normalized;
    }

    private void validateRequest(ImportRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("导入请求不能为空");
        }
        if (request.phone() == null || request.phone().trim().isBlank()) {
            throw new IllegalArgumentException("phone 不能为空");
        }
        if (request.password() == null || request.password().trim().isBlank()) {
            throw new IllegalArgumentException("password 不能为空");
        }
        if (request.nickname() == null || request.nickname().trim().isBlank()) {
            throw new IllegalArgumentException("nickname 不能为空");
        }
        if (request.legacyDbPath() == null || request.legacyDbPath().trim().isBlank()) {
            throw new IllegalArgumentException("legacyDbPath 不能为空");
        }
    }

    private void validateExistingOwnerRequest(ExistingOwnerImportRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("导入请求不能为空");
        }
        if (request.legacyDbPath() == null || request.legacyDbPath().trim().isBlank()) {
            throw new IllegalArgumentException("legacyDbPath 不能为空");
        }
    }

    public record ImportRequest(
        String phone,
        String password,
        String nickname,
        String legacyDbPath,
        Boolean resetOwnedData
    ) {}

    public record ExistingOwnerImportRequest(
        String legacyDbPath,
        Boolean resetOwnedData
    ) {}

    public record ImportResult(
        Long userId,
        String phone,
        String nickname,
        String legacyDbPath,
        int accounts,
        int customers,
        int suppliers,
        int products,
        int saleOrders,
        int saleOrderItems,
        int payments,
        int purchaseOrders,
        int purchaseOrderItems,
        int payOrders,
        int financeRecords,
        int inventorySnapshots
    ) {}

    private record ImportedCompany(Long id, String name) {}

    private record ImportedProduct(Long id, String code, String name, Double purchasePrice, Double stock, Long updatedAt) {}

    private record ImportedSaleOrder(Long id, Long customerId, String customerName, Long createdAt, String orderNo) {}

    private record ImportedPurchaseOrder(Long id, Long supplierId, String supplierName, Long createdAt) {}

    private record ImportedAccount(Long id, String name, int type) {}

    private record CompanyImportContext(
        Map<Long, ImportedCompany> customerByOldId,
        Map<Long, ImportedCompany> supplierByOldId
    ) {}

    private record ProductImportContext(Map<Long, ImportedProduct> productByOldId) {}

    private static final class ImportCounters {
        int accounts;
        int customers;
        int suppliers;
        int products;
        int saleOrders;
        int saleOrderItems;
        int payments;
        int purchaseOrders;
        int purchaseOrderItems;
        int payOrders;
        int financeRecords;
        int inventorySnapshots;
    }
}
