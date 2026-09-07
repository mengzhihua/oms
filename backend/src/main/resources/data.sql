-- 演示数据（幂等：已存在则跳过）
-- ===================== 渠道 / 店铺 =====================
INSERT INTO oms_channel (code, name, type, status, created_at, updated_at)
SELECT 'TMALL', '天猫', 'PLATFORM', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM oms_channel WHERE code = 'TMALL');

INSERT INTO oms_channel (code, name, type, status, created_at, updated_at)
SELECT 'JD', '京东', 'PLATFORM', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM oms_channel WHERE code = 'JD');

INSERT INTO oms_channel (code, name, type, status, created_at, updated_at)
SELECT 'DOUYIN', '抖音', 'PLATFORM', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM oms_channel WHERE code = 'DOUYIN');

INSERT INTO oms_channel (code, name, type, status, created_at, updated_at)
SELECT 'OFFLINE', '线下/经销', 'OFFLINE', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM oms_channel WHERE code = 'OFFLINE');

INSERT INTO oms_channel (code, name, type, status, created_at, updated_at)
SELECT 'API', 'API 直连', 'API', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM oms_channel WHERE code = 'API');

INSERT INTO oms_shop (code, name, channel_code, default_warehouse_code, auto_audit, status, created_at, updated_at)
SELECT 'SHOP-TM01', '天猫旗舰店', 'TMALL', 'WH-SH', 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM oms_shop WHERE code = 'SHOP-TM01');

INSERT INTO oms_shop (code, name, channel_code, default_warehouse_code, auto_audit, status, created_at, updated_at)
SELECT 'SHOP-JD01', '京东自营店', 'JD', 'WH-BJ', 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM oms_shop WHERE code = 'SHOP-JD01');

INSERT INTO oms_shop (code, name, channel_code, default_warehouse_code, auto_audit, status, created_at, updated_at)
SELECT 'SHOP-DY01', '抖音小店', 'DOUYIN', 'WH-SH', 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM oms_shop WHERE code = 'SHOP-DY01');

INSERT INTO oms_shop (code, name, channel_code, default_warehouse_code, auto_audit, status, created_at, updated_at)
SELECT 'SHOP-OFF01', '华南经销商', 'OFFLINE', 'WH-GZ', 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM oms_shop WHERE code = 'SHOP-OFF01');

-- ===================== 仓库 / 承运商 =====================
INSERT INTO oms_warehouse (code, name, type, wms_code, province, city, priority, status, created_at, updated_at)
SELECT 'WH-SH', '上海中心仓', 'OWN', 'WH01', '上海市', '上海市', 10, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM oms_warehouse WHERE code = 'WH-SH');

INSERT INTO oms_warehouse (code, name, type, wms_code, province, city, priority, status, created_at, updated_at)
SELECT 'WH-BJ', '北京华北仓', 'OWN', 'WH02', '北京市', '北京市', 20, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM oms_warehouse WHERE code = 'WH-BJ');

INSERT INTO oms_warehouse (code, name, type, wms_code, province, city, priority, status, created_at, updated_at)
SELECT 'WH-GZ', '广州华南仓', '3PL', 'WH03', '广东省', '广州市', 30, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM oms_warehouse WHERE code = 'WH-GZ');

INSERT INTO oms_carrier (code, name, type, tms_code, status, created_at, updated_at)
SELECT 'SF', '顺丰速运', 'EXPRESS', 'SF', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM oms_carrier WHERE code = 'SF');

INSERT INTO oms_carrier (code, name, type, tms_code, status, created_at, updated_at)
SELECT 'JDL', '京东物流', 'EXPRESS', 'JD', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM oms_carrier WHERE code = 'JDL');

INSERT INTO oms_carrier (code, name, type, tms_code, status, created_at, updated_at)
SELECT 'ZTO', '中通快递', 'EXPRESS', 'ZTO', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM oms_carrier WHERE code = 'ZTO');

INSERT INTO oms_carrier (code, name, type, tms_code, status, created_at, updated_at)
SELECT 'SELF', '自营车队', 'SELF', 'SELF', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM oms_carrier WHERE code = 'SELF');

-- ===================== 客户 / 商品 =====================
INSERT INTO oms_customer (code, name, type, level, phone, province, city, status, created_at, updated_at)
SELECT 'C001', '张三', 'PERSON', 'VIP', '13800000001', '广东省', '深圳市', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM oms_customer WHERE code = 'C001');

INSERT INTO oms_customer (code, name, type, level, phone, province, city, status, created_at, updated_at)
SELECT 'C002', '李四', 'PERSON', 'NORMAL', '13800000002', '北京市', '北京市', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM oms_customer WHERE code = 'C002');

INSERT INTO oms_customer (code, name, type, level, phone, province, city, status, created_at, updated_at)
SELECT 'C003', '华南经销有限公司', 'COMPANY', 'KA', '020-88888888', '广东省', '广州市', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM oms_customer WHERE code = 'C003');

INSERT INTO oms_product (sku, name, type, category, spec, unit, barcode, price, weight_kg, status, created_at, updated_at)
SELECT 'SKU001', '无线蓝牙耳机', 'NORMAL', '数码', '黑色', '个', '6901000000011', 199.00, 0.2, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM oms_product WHERE sku = 'SKU001');

INSERT INTO oms_product (sku, name, type, category, spec, unit, barcode, price, weight_kg, status, created_at, updated_at)
SELECT 'SKU002', '智能手环', 'NORMAL', '数码', '标准版', '个', '6901000000028', 299.00, 0.1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM oms_product WHERE sku = 'SKU002');

INSERT INTO oms_product (sku, name, type, category, spec, unit, barcode, price, weight_kg, status, created_at, updated_at)
SELECT 'SKU003', '便携充电宝 10000mAh', 'NORMAL', '数码', '白色', '个', '6901000000035', 129.00, 0.3, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM oms_product WHERE sku = 'SKU003');

INSERT INTO oms_product (sku, name, type, category, spec, unit, barcode, price, weight_kg, status, created_at, updated_at)
SELECT 'SKU004', 'USB-C 数据线 1m', 'NORMAL', '配件', '黑色', '条', '6901000000042', 29.00, 0.05, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM oms_product WHERE sku = 'SKU004');

INSERT INTO oms_product (sku, name, type, category, spec, unit, barcode, price, weight_kg, status, created_at, updated_at)
SELECT 'SKU005', '收纳包', 'NORMAL', '配件', '灰色', '个', '6901000000059', 39.00, 0.1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM oms_product WHERE sku = 'SKU005');

INSERT INTO oms_product (sku, name, type, category, spec, unit, barcode, price, weight_kg, status, created_at, updated_at)
SELECT 'BUNDLE-01', '出行数码套装', 'BUNDLE', '套装', '耳机+充电宝+数据线', '套', '6901000000066', 329.00, 0.55, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM oms_product WHERE sku = 'BUNDLE-01');

INSERT INTO oms_bundle_item (bundle_sku, sku, qty, created_at, updated_at)
SELECT 'BUNDLE-01', 'SKU001', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM oms_bundle_item WHERE bundle_sku = 'BUNDLE-01' AND sku = 'SKU001');

INSERT INTO oms_bundle_item (bundle_sku, sku, qty, created_at, updated_at)
SELECT 'BUNDLE-01', 'SKU003', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM oms_bundle_item WHERE bundle_sku = 'BUNDLE-01' AND sku = 'SKU003');

INSERT INTO oms_bundle_item (bundle_sku, sku, qty, created_at, updated_at)
SELECT 'BUNDLE-01', 'SKU004', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM oms_bundle_item WHERE bundle_sku = 'BUNDLE-01' AND sku = 'SKU004');

-- ===================== 分仓路由规则 =====================
INSERT INTO oms_routing_rule (name, priority, channel_code, shop_code, province, sku, warehouse_code, carrier_code, status, created_at, updated_at)
SELECT '华南就近发货', 10, NULL, NULL, '广东', NULL, 'WH-GZ', 'SF', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM oms_routing_rule WHERE name = '华南就近发货');

INSERT INTO oms_routing_rule (name, priority, channel_code, shop_code, province, sku, warehouse_code, carrier_code, status, created_at, updated_at)
SELECT '京东订单走北京仓', 20, 'JD', NULL, NULL, NULL, 'WH-BJ', 'JDL', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM oms_routing_rule WHERE name = '京东订单走北京仓');

INSERT INTO oms_routing_rule (name, priority, channel_code, shop_code, province, sku, warehouse_code, carrier_code, status, created_at, updated_at)
SELECT '默认上海仓', 100, NULL, NULL, NULL, NULL, 'WH-SH', 'ZTO', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM oms_routing_rule WHERE name = '默认上海仓');

-- ===================== 库存 =====================
INSERT INTO oms_inventory (warehouse_code, sku, qty_on_hand, qty_reserved, safety_qty, created_at, updated_at)
SELECT 'WH-SH', 'SKU001', 500, 0, 10, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM oms_inventory WHERE warehouse_code = 'WH-SH' AND sku = 'SKU001');

INSERT INTO oms_inventory (warehouse_code, sku, qty_on_hand, qty_reserved, safety_qty, created_at, updated_at)
SELECT 'WH-SH', 'SKU002', 300, 0, 10, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM oms_inventory WHERE warehouse_code = 'WH-SH' AND sku = 'SKU002');

INSERT INTO oms_inventory (warehouse_code, sku, qty_on_hand, qty_reserved, safety_qty, created_at, updated_at)
SELECT 'WH-SH', 'SKU003', 400, 0, 10, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM oms_inventory WHERE warehouse_code = 'WH-SH' AND sku = 'SKU003');

INSERT INTO oms_inventory (warehouse_code, sku, qty_on_hand, qty_reserved, safety_qty, created_at, updated_at)
SELECT 'WH-SH', 'SKU004', 1000, 0, 10, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM oms_inventory WHERE warehouse_code = 'WH-SH' AND sku = 'SKU004');

INSERT INTO oms_inventory (warehouse_code, sku, qty_on_hand, qty_reserved, safety_qty, created_at, updated_at)
SELECT 'WH-SH', 'SKU005', 200, 0, 10, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM oms_inventory WHERE warehouse_code = 'WH-SH' AND sku = 'SKU005');

INSERT INTO oms_inventory (warehouse_code, sku, qty_on_hand, qty_reserved, safety_qty, created_at, updated_at)
SELECT 'WH-BJ', 'SKU001', 200, 0, 10, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM oms_inventory WHERE warehouse_code = 'WH-BJ' AND sku = 'SKU001');

INSERT INTO oms_inventory (warehouse_code, sku, qty_on_hand, qty_reserved, safety_qty, created_at, updated_at)
SELECT 'WH-BJ', 'SKU002', 150, 0, 10, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM oms_inventory WHERE warehouse_code = 'WH-BJ' AND sku = 'SKU002');

INSERT INTO oms_inventory (warehouse_code, sku, qty_on_hand, qty_reserved, safety_qty, created_at, updated_at)
SELECT 'WH-BJ', 'SKU003', 100, 0, 10, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM oms_inventory WHERE warehouse_code = 'WH-BJ' AND sku = 'SKU003');

INSERT INTO oms_inventory (warehouse_code, sku, qty_on_hand, qty_reserved, safety_qty, created_at, updated_at)
SELECT 'WH-BJ', 'SKU004', 500, 0, 10, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM oms_inventory WHERE warehouse_code = 'WH-BJ' AND sku = 'SKU004');

INSERT INTO oms_inventory (warehouse_code, sku, qty_on_hand, qty_reserved, safety_qty, created_at, updated_at)
SELECT 'WH-GZ', 'SKU001', 100, 0, 10, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM oms_inventory WHERE warehouse_code = 'WH-GZ' AND sku = 'SKU001');

INSERT INTO oms_inventory (warehouse_code, sku, qty_on_hand, qty_reserved, safety_qty, created_at, updated_at)
SELECT 'WH-GZ', 'SKU003', 80, 0, 10, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM oms_inventory WHERE warehouse_code = 'WH-GZ' AND sku = 'SKU003');

INSERT INTO oms_inventory (warehouse_code, sku, qty_on_hand, qty_reserved, safety_qty, created_at, updated_at)
SELECT 'WH-GZ', 'SKU004', 300, 0, 10, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM oms_inventory WHERE warehouse_code = 'WH-GZ' AND sku = 'SKU004');

INSERT INTO oms_inventory (warehouse_code, sku, qty_on_hand, qty_reserved, safety_qty, created_at, updated_at)
SELECT 'WH-GZ', 'SKU005', 50, 0, 10, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM oms_inventory WHERE warehouse_code = 'WH-GZ' AND sku = 'SKU005');

INSERT INTO oms_channel_stock_policy (shop_code, sku, warehouse_code, mode, ratio, fixed_qty, status, created_at, updated_at)
SELECT 'SHOP-DY01', NULL, NULL, 'RATIO', 50, 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM oms_channel_stock_policy WHERE shop_code = 'SHOP-DY01' AND sku IS NULL);

INSERT INTO oms_channel_stock_policy (shop_code, sku, warehouse_code, mode, ratio, fixed_qty, status, created_at, updated_at)
SELECT 'SHOP-JD01', NULL, 'WH-BJ', 'RATIO', 100, 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM oms_channel_stock_policy WHERE shop_code = 'SHOP-JD01' AND sku IS NULL);
