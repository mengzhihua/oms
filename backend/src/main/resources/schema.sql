-- ===================== 基础数据 =====================
CREATE TABLE IF NOT EXISTS oms_channel (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  code VARCHAR(32) NOT NULL,
  name VARCHAR(128) NOT NULL,
  type VARCHAR(32) NOT NULL,
  remark VARCHAR(255),
  status INT DEFAULT 1,
  created_at TIMESTAMP,
  updated_at TIMESTAMP,
  CONSTRAINT uk_channel_code UNIQUE (code)
);

CREATE TABLE IF NOT EXISTS oms_shop (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  code VARCHAR(32) NOT NULL,
  name VARCHAR(128) NOT NULL,
  channel_code VARCHAR(32) NOT NULL,
  default_warehouse_code VARCHAR(32),
  auto_audit INT DEFAULT 0,
  contact VARCHAR(64),
  phone VARCHAR(32),
  status INT DEFAULT 1,
  created_at TIMESTAMP,
  updated_at TIMESTAMP,
  CONSTRAINT uk_shop_code UNIQUE (code)
);

CREATE TABLE IF NOT EXISTS oms_customer (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  code VARCHAR(32) NOT NULL,
  name VARCHAR(128) NOT NULL,
  type VARCHAR(16) DEFAULT 'PERSON',
  level VARCHAR(16) DEFAULT 'NORMAL',
  phone VARCHAR(32),
  email VARCHAR(128),
  province VARCHAR(32),
  city VARCHAR(32),
  address VARCHAR(255),
  status INT DEFAULT 1,
  created_at TIMESTAMP,
  updated_at TIMESTAMP,
  CONSTRAINT uk_customer_code UNIQUE (code)
);

CREATE TABLE IF NOT EXISTS oms_product (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  sku VARCHAR(64) NOT NULL,
  name VARCHAR(128) NOT NULL,
  type VARCHAR(16) DEFAULT 'NORMAL',
  category VARCHAR(64),
  spec VARCHAR(128),
  unit VARCHAR(16),
  barcode VARCHAR(64),
  price DECIMAL(12,2) DEFAULT 0,
  weight_kg DECIMAL(10,3),
  status INT DEFAULT 1,
  created_at TIMESTAMP,
  updated_at TIMESTAMP,
  CONSTRAINT uk_product_sku UNIQUE (sku)
);

CREATE TABLE IF NOT EXISTS oms_bundle_item (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  bundle_sku VARCHAR(64) NOT NULL,
  sku VARCHAR(64) NOT NULL,
  qty INT NOT NULL,
  created_at TIMESTAMP,
  updated_at TIMESTAMP,
  CONSTRAINT uk_bundle_item UNIQUE (bundle_sku, sku)
);

CREATE TABLE IF NOT EXISTS oms_warehouse (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  code VARCHAR(32) NOT NULL,
  name VARCHAR(128) NOT NULL,
  type VARCHAR(16) DEFAULT 'OWN',
  wms_code VARCHAR(32),
  province VARCHAR(32),
  city VARCHAR(32),
  address VARCHAR(255),
  contact VARCHAR(64),
  phone VARCHAR(32),
  priority INT DEFAULT 100,
  status INT DEFAULT 1,
  created_at TIMESTAMP,
  updated_at TIMESTAMP,
  CONSTRAINT uk_warehouse_code UNIQUE (code)
);

CREATE TABLE IF NOT EXISTS oms_carrier (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  code VARCHAR(32) NOT NULL,
  name VARCHAR(128) NOT NULL,
  type VARCHAR(16) DEFAULT 'EXPRESS',
  tms_code VARCHAR(32),
  contact VARCHAR(64),
  phone VARCHAR(32),
  status INT DEFAULT 1,
  created_at TIMESTAMP,
  updated_at TIMESTAMP,
  CONSTRAINT uk_carrier_code UNIQUE (code)
);

-- 分仓路由规则：按优先级匹配（渠道/店铺/省份/SKU 任意为空表示不限），命中后指定发货仓与承运商
CREATE TABLE IF NOT EXISTS oms_routing_rule (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(128) NOT NULL,
  priority INT DEFAULT 100,
  channel_code VARCHAR(32),
  shop_code VARCHAR(32),
  province VARCHAR(32),
  sku VARCHAR(64),
  warehouse_code VARCHAR(32) NOT NULL,
  carrier_code VARCHAR(32),
  status INT DEFAULT 1,
  created_at TIMESTAMP,
  updated_at TIMESTAMP
);

-- ===================== 库存中心 =====================
CREATE TABLE IF NOT EXISTS oms_inventory (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  warehouse_code VARCHAR(32) NOT NULL,
  sku VARCHAR(64) NOT NULL,
  qty_on_hand INT DEFAULT 0,
  qty_reserved INT DEFAULT 0,
  safety_qty INT DEFAULT 0,
  created_at TIMESTAMP,
  updated_at TIMESTAMP,
  CONSTRAINT uk_inventory UNIQUE (warehouse_code, sku)
);

CREATE TABLE IF NOT EXISTS oms_inventory_txn (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  warehouse_code VARCHAR(32) NOT NULL,
  sku VARCHAR(64) NOT NULL,
  type VARCHAR(16) NOT NULL,
  qty INT NOT NULL,
  on_hand_after INT,
  reserved_after INT,
  ref_no VARCHAR(64),
  remark VARCHAR(255),
  operator VARCHAR(64),
  created_at TIMESTAMP,
  updated_at TIMESTAMP
);
CREATE INDEX idx_inv_txn_ref ON oms_inventory_txn (ref_no);

-- 渠道库存策略：店铺可售库存 = 指定仓（为空表示全部仓）可用库存 × ratio% 或固定值
CREATE TABLE IF NOT EXISTS oms_channel_stock_policy (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  shop_code VARCHAR(32) NOT NULL,
  sku VARCHAR(64),
  warehouse_code VARCHAR(32),
  mode VARCHAR(16) DEFAULT 'RATIO',
  ratio INT DEFAULT 100,
  fixed_qty INT DEFAULT 0,
  status INT DEFAULT 1,
  created_at TIMESTAMP,
  updated_at TIMESTAMP
);

-- ===================== 订单中心 =====================
CREATE TABLE IF NOT EXISTS oms_sales_order (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  order_no VARCHAR(32) NOT NULL,
  parent_order_no VARCHAR(32),
  channel_code VARCHAR(32) NOT NULL,
  shop_code VARCHAR(32) NOT NULL,
  channel_order_no VARCHAR(64),
  source VARCHAR(16) DEFAULT 'MANUAL',
  customer_code VARCHAR(32),
  receiver_name VARCHAR(64) NOT NULL,
  receiver_phone VARCHAR(32) NOT NULL,
  province VARCHAR(32),
  city VARCHAR(32),
  district VARCHAR(32),
  address VARCHAR(255) NOT NULL,
  status VARCHAR(16) NOT NULL,
  pay_status VARCHAR(16) DEFAULT 'PAID',
  priority INT DEFAULT 0,
  order_time TIMESTAMP,
  pay_time TIMESTAMP,
  goods_amount DECIMAL(12,2) DEFAULT 0,
  freight DECIMAL(12,2) DEFAULT 0,
  discount DECIMAL(12,2) DEFAULT 0,
  pay_amount DECIMAL(12,2) DEFAULT 0,
  warehouse_code VARCHAR(32),
  carrier_code VARCHAR(32),
  tracking_no VARCHAR(64),
  wms_order_no VARCHAR(64),
  tms_order_no VARCHAR(64),
  buyer_remark VARCHAR(255),
  seller_remark VARCHAR(255),
  hold_reason VARCHAR(255),
  cancel_reason VARCHAR(255),
  audited_by VARCHAR(64),
  audited_at TIMESTAMP,
  pushed_at TIMESTAMP,
  shipped_at TIMESTAMP,
  signed_at TIMESTAMP,
  completed_at TIMESTAMP,
  created_at TIMESTAMP,
  updated_at TIMESTAMP,
  CONSTRAINT uk_sales_order_no UNIQUE (order_no)
);
CREATE INDEX idx_so_status ON oms_sales_order (status);
CREATE INDEX idx_so_channel_order ON oms_sales_order (shop_code, channel_order_no);

CREATE TABLE IF NOT EXISTS oms_sales_order_item (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  order_no VARCHAR(32) NOT NULL,
  sku VARCHAR(64) NOT NULL,
  product_name VARCHAR(128),
  bundle_sku VARCHAR(64),
  qty INT NOT NULL,
  price DECIMAL(12,2) DEFAULT 0,
  amount DECIMAL(12,2) DEFAULT 0,
  reserved_qty INT DEFAULT 0,
  shipped_qty INT DEFAULT 0,
  created_at TIMESTAMP,
  updated_at TIMESTAMP
);
CREATE INDEX idx_soi_order ON oms_sales_order_item (order_no);

CREATE TABLE IF NOT EXISTS oms_order_log (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  order_no VARCHAR(32) NOT NULL,
  action VARCHAR(32) NOT NULL,
  from_status VARCHAR(16),
  to_status VARCHAR(16),
  operator VARCHAR(64),
  remark VARCHAR(500),
  created_at TIMESTAMP,
  updated_at TIMESTAMP
);
CREATE INDEX idx_order_log_order ON oms_order_log (order_no);

-- ===================== 售后 =====================
CREATE TABLE IF NOT EXISTS oms_return_order (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  return_no VARCHAR(32) NOT NULL,
  order_no VARCHAR(32) NOT NULL,
  type VARCHAR(16) NOT NULL,
  status VARCHAR(16) NOT NULL,
  channel_code VARCHAR(32),
  shop_code VARCHAR(32),
  customer_code VARCHAR(32),
  reason VARCHAR(255),
  refund_amount DECIMAL(12,2) DEFAULT 0,
  warehouse_code VARCHAR(32),
  return_tracking_no VARCHAR(64),
  exchange_order_no VARCHAR(32),
  remark VARCHAR(255),
  audited_by VARCHAR(64),
  audited_at TIMESTAMP,
  received_at TIMESTAMP,
  refunded_at TIMESTAMP,
  completed_at TIMESTAMP,
  created_at TIMESTAMP,
  updated_at TIMESTAMP,
  CONSTRAINT uk_return_no UNIQUE (return_no)
);
CREATE INDEX idx_return_order ON oms_return_order (order_no);

CREATE TABLE IF NOT EXISTS oms_return_item (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  return_no VARCHAR(32) NOT NULL,
  sku VARCHAR(64) NOT NULL,
  product_name VARCHAR(128),
  qty INT NOT NULL,
  received_qty INT DEFAULT 0,
  amount DECIMAL(12,2) DEFAULT 0,
  exchange_sku VARCHAR(64),
  created_at TIMESTAMP,
  updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS oms_refund (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  refund_no VARCHAR(32) NOT NULL,
  return_no VARCHAR(32),
  order_no VARCHAR(32) NOT NULL,
  amount DECIMAL(12,2) NOT NULL,
  method VARCHAR(16) DEFAULT 'ORIGINAL',
  status VARCHAR(16) NOT NULL,
  paid_at TIMESTAMP,
  remark VARCHAR(255),
  operator VARCHAR(64),
  created_at TIMESTAMP,
  updated_at TIMESTAMP,
  CONSTRAINT uk_refund_no UNIQUE (refund_no)
);

-- ===================== 集成日志 =====================
CREATE TABLE IF NOT EXISTS oms_integration_log (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  direction VARCHAR(8) NOT NULL,
  target VARCHAR(16) NOT NULL,
  action VARCHAR(32) NOT NULL,
  ref_no VARCHAR(64),
  request_body TEXT,
  response_body TEXT,
  success INT DEFAULT 1,
  error_msg VARCHAR(500),
  created_at TIMESTAMP,
  updated_at TIMESTAMP
);
CREATE INDEX idx_integration_ref ON oms_integration_log (ref_no);

-- ===================== 系统 =====================
CREATE TABLE IF NOT EXISTS oms_user (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(64) NOT NULL,
  password VARCHAR(255) NOT NULL,
  real_name VARCHAR(64),
  role VARCHAR(16) NOT NULL,
  status INT DEFAULT 1,
  last_login_at TIMESTAMP,
  created_at TIMESTAMP,
  updated_at TIMESTAMP,
  CONSTRAINT uk_user_name UNIQUE (username)
);

CREATE TABLE IF NOT EXISTS oms_sequence (
  prefix VARCHAR(16) NOT NULL,
  day_key VARCHAR(8) NOT NULL,
  seq_value INT NOT NULL,
  PRIMARY KEY (prefix, day_key)
);

CREATE TABLE IF NOT EXISTS oms_op_log (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(64),
  method VARCHAR(8),
  path VARCHAR(255),
  query VARCHAR(255),
  http_status INT,
  cost_ms INT,
  client_ip VARCHAR(64),
  created_at TIMESTAMP
);
CREATE INDEX idx_op_log_created ON oms_op_log (created_at);
