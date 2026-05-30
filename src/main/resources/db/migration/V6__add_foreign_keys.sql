ALTER TABLE sale_order_items ADD CONSTRAINT fk_sale_order_items_order
    FOREIGN KEY (orderId) REFERENCES sale_orders(id) ON DELETE CASCADE;

ALTER TABLE sale_order_items ADD CONSTRAINT fk_sale_order_items_product
    FOREIGN KEY (productId) REFERENCES products(id) ON DELETE SET NULL;

ALTER TABLE purchase_order_items ADD CONSTRAINT fk_purchase_order_items_order
    FOREIGN KEY (orderId) REFERENCES purchase_orders(id) ON DELETE CASCADE;

ALTER TABLE purchase_order_items ADD CONSTRAINT fk_purchase_order_items_product
    FOREIGN KEY (productId) REFERENCES products(id) ON DELETE SET NULL;

ALTER TABLE payments ADD CONSTRAINT fk_payments_order
    FOREIGN KEY (orderId) REFERENCES sale_orders(id) ON DELETE CASCADE;

ALTER TABLE stock_adjustments ADD CONSTRAINT fk_stock_adjustments_product
    FOREIGN KEY (productId) REFERENCES products(id) ON DELETE CASCADE;

ALTER TABLE sale_orders ADD CONSTRAINT fk_sale_orders_customer
    FOREIGN KEY (customerId) REFERENCES customers(id) ON DELETE SET NULL;

ALTER TABLE purchase_orders ADD CONSTRAINT fk_purchase_orders_supplier
    FOREIGN KEY (supplierId) REFERENCES suppliers(id) ON DELETE SET NULL;

ALTER TABLE pay_orders ADD CONSTRAINT fk_pay_orders_supplier
    FOREIGN KEY (supplierId) REFERENCES suppliers(id) ON DELETE SET NULL;
