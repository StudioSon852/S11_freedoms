-- FREEDOMS SHOP - MySQL 8.0+ clean schema
-- Run this script on a local development database. It creates a new database.
-- It does not import SQL Server data from SD_11.
CREATE DATABASE IF NOT EXISTS freedoms_shop
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE freedoms_shop;

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS refunds;
DROP TABLE IF EXISTS order_status_history;
DROP TABLE IF EXISTS payments;
DROP TABLE IF EXISTS order_items;
DROP TABLE IF EXISTS orders;
DROP TABLE IF EXISTS held_invoice_items;
DROP TABLE IF EXISTS held_invoices;
DROP TABLE IF EXISTS variant_discounts;
DROP TABLE IF EXISTS coupons;
DROP TABLE IF EXISTS product_variants;
DROP TABLE IF EXISTS products;
DROP TABLE IF EXISTS colors;
DROP TABLE IF EXISTS sizes;
DROP TABLE IF EXISTS brands;
DROP TABLE IF EXISTS categories;
DROP TABLE IF EXISTS store_settings;
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS member_tiers;
DROP TABLE IF EXISTS roles;
SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE roles (
  role_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  role_code VARCHAR(30) NOT NULL UNIQUE,
  role_name VARCHAR(80) NOT NULL
) ENGINE=InnoDB;

CREATE TABLE member_tiers (
  tier_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tier_code VARCHAR(30) NOT NULL UNIQUE,
  tier_name VARCHAR(80) NOT NULL,
  min_spending DECIMAL(15,2) NOT NULL DEFAULT 0,
  discount_percent DECIMAL(5,2) NOT NULL DEFAULT 0,
  sort_order INT NOT NULL DEFAULT 0
) ENGINE=InnoDB;

CREATE TABLE users (
  user_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  role_id BIGINT NOT NULL,
  tier_id BIGINT NULL,
  email VARCHAR(190) NOT NULL UNIQUE,
  password_hash VARCHAR(100) NOT NULL,
  full_name VARCHAR(150) NOT NULL,
  phone VARCHAR(30) NULL,
  default_address VARCHAR(500) NULL,
  is_active BIT(1) NOT NULL DEFAULT b'1',
  total_spent DECIMAL(15,2) NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_users_role FOREIGN KEY (role_id) REFERENCES roles(role_id),
  CONSTRAINT fk_users_tier FOREIGN KEY (tier_id) REFERENCES member_tiers(tier_id),
  INDEX idx_users_phone(phone),
  INDEX idx_users_role(role_id)
) ENGINE=InnoDB;

CREATE TABLE categories (
  category_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  category_name VARCHAR(100) NOT NULL UNIQUE,
  is_active BIT(1) NOT NULL DEFAULT b'1'
) ENGINE=InnoDB;

CREATE TABLE brands (
  brand_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  brand_name VARCHAR(100) NOT NULL UNIQUE,
  is_active BIT(1) NOT NULL DEFAULT b'1'
) ENGINE=InnoDB;

CREATE TABLE sizes (
  size_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  size_label VARCHAR(20) NOT NULL UNIQUE,
  sort_order INT NOT NULL DEFAULT 0
) ENGINE=InnoDB;

CREATE TABLE colors (
  color_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  color_name VARCHAR(50) NOT NULL UNIQUE,
  color_hex VARCHAR(10) NULL
) ENGINE=InnoDB;

CREATE TABLE products (
  product_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  category_id BIGINT NULL,
  brand_id BIGINT NULL,
  product_name VARCHAR(180) NOT NULL,
  parent_sku VARCHAR(60) NULL UNIQUE,
  description TEXT NULL,
  image_url VARCHAR(500) NULL,
  is_featured BIT(1) NOT NULL DEFAULT b'0',
  is_active BIT(1) NOT NULL DEFAULT b'1',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES categories(category_id),
  CONSTRAINT fk_products_brand FOREIGN KEY (brand_id) REFERENCES brands(brand_id),
  INDEX idx_products_name(product_name),
  INDEX idx_products_active(is_active)
) ENGINE=InnoDB;

CREATE TABLE product_variants (
  variant_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  product_id BIGINT NOT NULL,
  size_id BIGINT NULL,
  color_id BIGINT NULL,
  sku VARCHAR(80) NOT NULL UNIQUE,
  unit_price DECIMAL(15,2) NOT NULL,
  stock_quantity INT NOT NULL DEFAULT 0,
  is_active BIT(1) NOT NULL DEFAULT b'1',
  CONSTRAINT fk_variants_product FOREIGN KEY (product_id) REFERENCES products(product_id),
  CONSTRAINT fk_variants_size FOREIGN KEY (size_id) REFERENCES sizes(size_id),
  CONSTRAINT fk_variants_color FOREIGN KEY (color_id) REFERENCES colors(color_id),
  INDEX idx_variants_product(product_id),
  INDEX idx_variants_stock(stock_quantity)
) ENGINE=InnoDB;

CREATE TABLE product_variant_images (
  image_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  variant_id BIGINT NOT NULL,
  image_url VARCHAR(600) NOT NULL,
  sort_order INT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_variant_images_variant FOREIGN KEY (variant_id) REFERENCES product_variants(variant_id),
  INDEX idx_variant_images_variant(variant_id,sort_order,image_id)
) ENGINE=InnoDB;

CREATE TABLE variant_discounts (
  variant_discount_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  product_variant_id BIGINT NOT NULL,
  sales_channel VARCHAR(20) NOT NULL DEFAULT 'BOTH',
  discount_type VARCHAR(20) NOT NULL,
  discount_value DECIMAL(15,2) NOT NULL,
  starts_at DATETIME NULL,
  ends_at DATETIME NULL,
  is_active BIT(1) NOT NULL DEFAULT b'1',
  reason VARCHAR(255) NULL,
  CONSTRAINT fk_variant_discounts_variant FOREIGN KEY (product_variant_id) REFERENCES product_variants(variant_id),
  INDEX idx_variant_discount_active(product_variant_id,is_active)
) ENGINE=InnoDB;

CREATE TABLE coupons (
  coupon_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  coupon_code VARCHAR(60) NOT NULL UNIQUE,
  sales_channel VARCHAR(20) NOT NULL DEFAULT 'BOTH',
  discount_type VARCHAR(20) NOT NULL,
  discount_value DECIMAL(15,2) NOT NULL,
  min_order_amount DECIMAL(15,2) NOT NULL DEFAULT 0,
  max_discount DECIMAL(15,2) NULL,
  usage_limit INT NULL,
  usage_count INT NOT NULL DEFAULT 0,
  starts_at DATETIME NULL,
  ends_at DATETIME NULL,
  is_active BIT(1) NOT NULL DEFAULT b'1',
  INDEX idx_coupon_active(coupon_code,is_active)
) ENGINE=InnoDB;

CREATE TABLE held_invoices (
  held_invoice_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  held_code VARCHAR(40) NOT NULL UNIQUE,
  created_by BIGINT NOT NULL,
  customer_id BIGINT NULL,
  customer_name VARCHAR(150) NOT NULL DEFAULT 'Khách lẻ',
  customer_phone VARCHAR(30) NULL,
  customer_note VARCHAR(500) NULL,
  coupon_code VARCHAR(60) NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
  converted_order_id BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_held_created_by FOREIGN KEY (created_by) REFERENCES users(user_id),
  CONSTRAINT fk_held_customer FOREIGN KEY (customer_id) REFERENCES users(user_id),
  INDEX idx_held_owner_status(created_by,status)
) ENGINE=InnoDB;

CREATE TABLE held_invoice_items (
  held_invoice_item_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  held_invoice_id BIGINT NOT NULL,
  product_variant_id BIGINT NOT NULL,
  product_name_snapshot VARCHAR(180) NOT NULL,
  variant_label_snapshot VARCHAR(100) NOT NULL,
  sku_snapshot VARCHAR(80) NOT NULL,
  unit_price DECIMAL(15,2) NOT NULL,
  quantity INT NOT NULL,
  CONSTRAINT fk_held_items_invoice FOREIGN KEY (held_invoice_id) REFERENCES held_invoices(held_invoice_id),
  CONSTRAINT fk_held_items_variant FOREIGN KEY (product_variant_id) REFERENCES product_variants(variant_id),
  INDEX idx_held_items_invoice(held_invoice_id)
) ENGINE=InnoDB;

CREATE TABLE orders (
  order_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_code VARCHAR(40) NOT NULL UNIQUE,
  customer_id BIGINT NULL,
  created_by BIGINT NULL,
  sales_channel VARCHAR(20) NOT NULL,
  order_status VARCHAR(30) NOT NULL,
  customer_name VARCHAR(150) NOT NULL,
  customer_phone VARCHAR(30) NOT NULL,
  shipping_address VARCHAR(500) NULL,
  customer_note VARCHAR(500) NULL,
  coupon_code VARCHAR(60) NULL,
  subtotal DECIMAL(15,2) NOT NULL,
  variant_discount_total DECIMAL(15,2) NOT NULL DEFAULT 0,
  member_discount_total DECIMAL(15,2) NOT NULL DEFAULT 0,
  coupon_discount_total DECIMAL(15,2) NOT NULL DEFAULT 0,
  shipping_fee DECIMAL(15,2) NOT NULL DEFAULT 0,
  total_amount DECIMAL(15,2) NOT NULL,
  spending_recorded BIT(1) NOT NULL DEFAULT b'0',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_orders_customer FOREIGN KEY (customer_id) REFERENCES users(user_id),
  CONSTRAINT fk_orders_staff FOREIGN KEY (created_by) REFERENCES users(user_id),
  INDEX idx_orders_customer(customer_id,created_at),
  INDEX idx_orders_status(order_status),
  INDEX idx_orders_channel(sales_channel)
) ENGINE=InnoDB;

ALTER TABLE held_invoices
  ADD CONSTRAINT fk_held_converted_order FOREIGN KEY (converted_order_id) REFERENCES orders(order_id);

CREATE TABLE order_items (
  order_item_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id BIGINT NOT NULL,
  product_variant_id BIGINT NOT NULL,
  product_name_snapshot VARCHAR(180) NOT NULL,
  variant_label_snapshot VARCHAR(100) NOT NULL,
  sku_snapshot VARCHAR(80) NOT NULL,
  unit_price DECIMAL(15,2) NOT NULL,
  quantity INT NOT NULL,
  variant_discount_amount DECIMAL(15,2) NOT NULL DEFAULT 0,
  member_discount_amount DECIMAL(15,2) NOT NULL DEFAULT 0,
  coupon_discount_amount DECIMAL(15,2) NOT NULL DEFAULT 0,
  line_total DECIMAL(15,2) NOT NULL,
  CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders(order_id),
  CONSTRAINT fk_order_items_variant FOREIGN KEY (product_variant_id) REFERENCES product_variants(variant_id),
  INDEX idx_order_items_order(order_id)
) ENGINE=InnoDB;

CREATE TABLE payments (
  payment_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id BIGINT NOT NULL,
  payment_method VARCHAR(30) NOT NULL,
  payment_status VARCHAR(30) NOT NULL,
  amount DECIMAL(15,2) NOT NULL,
  transaction_reference VARCHAR(100) NULL,
  note VARCHAR(500) NULL,
  paid_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_payments_order FOREIGN KEY (order_id) REFERENCES orders(order_id),
  UNIQUE KEY uq_payments_order(order_id),
  INDEX idx_payments_order(order_id),
  INDEX idx_payments_status(payment_status)
) ENGINE=InnoDB;

CREATE TABLE refunds (
  refund_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id BIGINT NOT NULL,
  payment_id BIGINT NOT NULL,
  requested_by BIGINT NULL,
  processed_by BIGINT NULL,
  amount DECIMAL(15,2) NOT NULL,
  refund_method VARCHAR(30) NOT NULL,
  status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
  reason VARCHAR(500) NULL,
  transaction_reference VARCHAR(120) NULL,
  requested_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  processed_at DATETIME NULL,
  CONSTRAINT fk_refund_order FOREIGN KEY (order_id) REFERENCES orders(order_id),
  CONSTRAINT fk_refund_payment FOREIGN KEY (payment_id) REFERENCES payments(payment_id),
  CONSTRAINT fk_refund_requested_by FOREIGN KEY (requested_by) REFERENCES users(user_id),
  CONSTRAINT fk_refund_processed_by FOREIGN KEY (processed_by) REFERENCES users(user_id),
  INDEX idx_refunds_status(status,requested_at),
  INDEX idx_refunds_payment_status(payment_id,status),
  INDEX idx_refunds_order_time(order_id,requested_at)
) ENGINE=InnoDB;

CREATE TABLE order_status_history (
  history_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id BIGINT NOT NULL,
  old_status VARCHAR(30) NULL,
  new_status VARCHAR(30) NOT NULL,
  changed_by BIGINT NULL,
  note VARCHAR(500) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_order_history_order FOREIGN KEY (order_id) REFERENCES orders(order_id),
  CONSTRAINT fk_order_history_user FOREIGN KEY (changed_by) REFERENCES users(user_id),
  INDEX idx_order_history(order_id,created_at)
) ENGINE=InnoDB;

CREATE TABLE store_settings (
  setting_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  setting_key VARCHAR(80) NOT NULL UNIQUE,
  setting_value VARCHAR(500) NULL
) ENGINE=InnoDB;

-- Seed reference data only. Demo users and products are created by Java startup seeder with BCrypt.
INSERT INTO roles(role_code,role_name) VALUES
 ('ADMIN','Quản trị viên'),('MANAGER','Quản lý'),('EMPLOYEE','Nhân viên'),('CUSTOMER','Khách hàng');

INSERT INTO member_tiers(tier_code,tier_name,min_spending,discount_percent,sort_order) VALUES
 ('NEW','Mới',0,0,1),
 ('SILVER','Bạc',100000,2,2),
 ('GOLD','Vàng',250000,3,3),
 ('PLATINUM','Bạch kim',500000,4,4),
 ('DIAMOND','Kim cương',1000000,5,5);

INSERT INTO categories(category_name) VALUES ('Giày chạy bộ'),('Giày bóng rổ'),('Giày thời trang'),('Giày tập luyện');
INSERT INTO brands(brand_name) VALUES ('Nike'),('Adidas'),('Freedoms');
INSERT INTO sizes(size_label,sort_order) VALUES ('38',1),('39',2),('40',3),('41',4),('42',5),('43',6),('44',7);
INSERT INTO colors(color_name,color_hex) VALUES ('Trắng','#FFFFFF'),('Đen','#000000'),('Đỏ','#FF0000'),('Xanh','#1E40AF');
INSERT INTO store_settings(setting_key,setting_value) VALUES
 ('STORE_NAME','FREEDOMS'),('STORE_TAGLINE','STEP INTO YOUR STYLE'),
 ('STORE_ADDRESS','249 Xã Đàn, Đống Đa, Hà Nội'),
 ('BANK_CODE','MB'),('BANK_ACCOUNT',''),('BANK_ACCOUNT_NAME','FREEDOMS SHOE STORE'),
 ('SHIPPING_FREE_THRESHOLD','1000000'),('SHIPPING_FALLBACK_HANOI','20000'),
 ('SHIPPING_FALLBACK_DANANG','32000'),('SHIPPING_FALLBACK_HCMC','38000'),
 ('SHIPPING_FALLBACK_OTHER','30000');

-- Demo products; variants have positive stock so online checkout and POS can be tested immediately.
INSERT INTO products(category_id,brand_id,product_name,parent_sku,description,image_url,is_featured,is_active) VALUES
 (1,1,'Freedoms Air Zoom Pegasus 41','FD-PEG41','Giày chạy bộ êm ái, đàn hồi tốt.','/img/hero-running.jpg',1,1),
 (2,2,'Freedoms Harden Vol 8','FD-HD8','Giày bóng rổ bám sân ổn định.','/img/category-basketball.jpg',1,1),
 (3,3,'Freedoms Street Classic','FD-STC01','Thiết kế thời trang dễ phối đồ.','/img/picture1.png',1,1);

INSERT INTO product_variants(product_id,size_id,color_id,sku,unit_price,stock_quantity,is_active) VALUES
 (1,3,1,'FD-PEG41-40-WHT',2500000,20,1),
 (1,4,2,'FD-PEG41-41-BLK',2600000,15,1),
 (2,4,3,'FD-HD8-41-RED',3200000,10,1),
 (3,3,2,'FD-STC01-40-BLK',1450000,25,1);

INSERT INTO variant_discounts(product_variant_id,sales_channel,discount_type,discount_value,reason,is_active)
VALUES (1,'BOTH','AMOUNT',200000,'Ưu đãi khai trương',1);

INSERT INTO coupons(coupon_code,sales_channel,discount_type,discount_value,min_order_amount,max_discount,usage_limit,usage_count,is_active)
VALUES ('FREEDOMS10','BOTH','PERCENT',10,1000000,300000,100,0,1);
INSERT INTO coupons(coupon_code,sales_channel,discount_type,discount_value,min_order_amount,max_discount,usage_limit,usage_count,is_active) VALUES
 ('WEBFREEDOMS5','ONLINE','PERCENT',5,500000,150000,100,0,1),
 ('POSFREEDOMS50000','OFFLINE','AMOUNT',50000,1000000,50000,100,0,1);

-- Login accounts are created by the application on first start using BCrypt.


-- Revenue dashboard view: only completed orders with a confirmed paid payment.
CREATE OR REPLACE VIEW v_revenue_daily AS
SELECT DATE(o.created_at) AS sale_date,
       o.sales_channel AS sales_channel,
       COUNT(o.order_id) AS order_count,
       SUM(o.total_amount) AS revenue
FROM orders o
JOIN payments p ON p.order_id = o.order_id
WHERE o.order_status = 'COMPLETED'
  AND p.payment_status = 'PAID'
GROUP BY DATE(o.created_at), o.sales_channel;
