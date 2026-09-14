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

-- ============ DMS 经销商备件补货渠道 ============
INSERT INTO oms_channel (code, name, type, status, created_at, updated_at)
SELECT 'DMS', '经销商DMS', 'B2B', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM oms_channel WHERE code = 'DMS');

INSERT INTO oms_shop (code, name, channel_code, default_warehouse_code, auto_audit, status, created_at, updated_at)
SELECT 'SHOP-DMS01', '经销商备件补货', 'DMS', 'WH-SH', 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM oms_shop WHERE code = 'SHOP-DMS01');

INSERT INTO oms_product (sku, name, type, category, spec, unit, price, status, created_at, updated_at)
SELECT 'P0001', '机油滤芯', 'NORMAL', '汽车备件', '保养件', '个', 35.00, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM oms_product WHERE sku = 'P0001');
INSERT INTO oms_inventory (warehouse_code, sku, qty_on_hand, qty_reserved, safety_qty, created_at, updated_at)
SELECT 'WH-SH', 'P0001', 200, 0, 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM oms_inventory WHERE warehouse_code = 'WH-SH' AND sku = 'P0001');

INSERT INTO oms_product (sku, name, type, category, spec, unit, price, status, created_at, updated_at)
SELECT 'P0002', '空气滤芯', 'NORMAL', '汽车备件', '保养件', '个', 68.00, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM oms_product WHERE sku = 'P0002');
INSERT INTO oms_inventory (warehouse_code, sku, qty_on_hand, qty_reserved, safety_qty, created_at, updated_at)
SELECT 'WH-SH', 'P0002', 200, 0, 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM oms_inventory WHERE warehouse_code = 'WH-SH' AND sku = 'P0002');

INSERT INTO oms_product (sku, name, type, category, spec, unit, price, status, created_at, updated_at)
SELECT 'P0003', '空调滤芯', 'NORMAL', '汽车备件', '保养件', '个', 98.00, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM oms_product WHERE sku = 'P0003');
INSERT INTO oms_inventory (warehouse_code, sku, qty_on_hand, qty_reserved, safety_qty, created_at, updated_at)
SELECT 'WH-SH', 'P0003', 200, 0, 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM oms_inventory WHERE warehouse_code = 'WH-SH' AND sku = 'P0003');

INSERT INTO oms_product (sku, name, type, category, spec, unit, price, status, created_at, updated_at)
SELECT 'P0004', '全合成机油 5W-30 4L', 'NORMAL', '汽车备件', '油液', '桶', 328.00, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM oms_product WHERE sku = 'P0004');
INSERT INTO oms_inventory (warehouse_code, sku, qty_on_hand, qty_reserved, safety_qty, created_at, updated_at)
SELECT 'WH-SH', 'P0004', 200, 0, 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM oms_inventory WHERE warehouse_code = 'WH-SH' AND sku = 'P0004');

INSERT INTO oms_product (sku, name, type, category, spec, unit, price, status, created_at, updated_at)
SELECT 'P0005', '刹车油 DOT4 1L', 'NORMAL', '汽车备件', '油液', '瓶', 80.00, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM oms_product WHERE sku = 'P0005');
INSERT INTO oms_inventory (warehouse_code, sku, qty_on_hand, qty_reserved, safety_qty, created_at, updated_at)
SELECT 'WH-SH', 'P0005', 200, 0, 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM oms_inventory WHERE warehouse_code = 'WH-SH' AND sku = 'P0005');

INSERT INTO oms_product (sku, name, type, category, spec, unit, price, status, created_at, updated_at)
SELECT 'P0006', '变速箱油 ATF 1L', 'NORMAL', '汽车备件', '油液', '瓶', 128.00, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM oms_product WHERE sku = 'P0006');
INSERT INTO oms_inventory (warehouse_code, sku, qty_on_hand, qty_reserved, safety_qty, created_at, updated_at)
SELECT 'WH-SH', 'P0006', 200, 0, 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM oms_inventory WHERE warehouse_code = 'WH-SH' AND sku = 'P0006');

INSERT INTO oms_product (sku, name, type, category, spec, unit, price, status, created_at, updated_at)
SELECT 'P0007', '冷却液 -35℃ 4L', 'NORMAL', '汽车备件', '油液', '桶', 88.00, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM oms_product WHERE sku = 'P0007');
INSERT INTO oms_inventory (warehouse_code, sku, qty_on_hand, qty_reserved, safety_qty, created_at, updated_at)
SELECT 'WH-SH', 'P0007', 200, 0, 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM oms_inventory WHERE warehouse_code = 'WH-SH' AND sku = 'P0007');

INSERT INTO oms_product (sku, name, type, category, spec, unit, price, status, created_at, updated_at)
SELECT 'P0008', '前刹车片', 'NORMAL', '汽车备件', '制动系统', '套', 320.00, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM oms_product WHERE sku = 'P0008');
INSERT INTO oms_inventory (warehouse_code, sku, qty_on_hand, qty_reserved, safety_qty, created_at, updated_at)
SELECT 'WH-SH', 'P0008', 200, 0, 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM oms_inventory WHERE warehouse_code = 'WH-SH' AND sku = 'P0008');

INSERT INTO oms_product (sku, name, type, category, spec, unit, price, status, created_at, updated_at)
SELECT 'P0009', '后刹车片', 'NORMAL', '汽车备件', '制动系统', '套', 260.00, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM oms_product WHERE sku = 'P0009');
INSERT INTO oms_inventory (warehouse_code, sku, qty_on_hand, qty_reserved, safety_qty, created_at, updated_at)
SELECT 'WH-SH', 'P0009', 200, 0, 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM oms_inventory WHERE warehouse_code = 'WH-SH' AND sku = 'P0009');

INSERT INTO oms_product (sku, name, type, category, spec, unit, price, status, created_at, updated_at)
SELECT 'P0010', '刹车盘', 'NORMAL', '汽车备件', '制动系统', '只', 450.00, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM oms_product WHERE sku = 'P0010');
INSERT INTO oms_inventory (warehouse_code, sku, qty_on_hand, qty_reserved, safety_qty, created_at, updated_at)
SELECT 'WH-SH', 'P0010', 200, 0, 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM oms_inventory WHERE warehouse_code = 'WH-SH' AND sku = 'P0010');

INSERT INTO oms_product (sku, name, type, category, spec, unit, price, status, created_at, updated_at)
SELECT 'P0011', '火花塞', 'NORMAL', '汽车备件', '发动机', '只', 60.00, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM oms_product WHERE sku = 'P0011');
INSERT INTO oms_inventory (warehouse_code, sku, qty_on_hand, qty_reserved, safety_qty, created_at, updated_at)
SELECT 'WH-SH', 'P0011', 200, 0, 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM oms_inventory WHERE warehouse_code = 'WH-SH' AND sku = 'P0011');

INSERT INTO oms_product (sku, name, type, category, spec, unit, price, status, created_at, updated_at)
SELECT 'P0012', '正时皮带', 'NORMAL', '汽车备件', '发动机', '条', 420.00, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM oms_product WHERE sku = 'P0012');
INSERT INTO oms_inventory (warehouse_code, sku, qty_on_hand, qty_reserved, safety_qty, created_at, updated_at)
SELECT 'WH-SH', 'P0012', 200, 0, 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM oms_inventory WHERE warehouse_code = 'WH-SH' AND sku = 'P0012');

INSERT INTO oms_product (sku, name, type, category, spec, unit, price, status, created_at, updated_at)
SELECT 'P0013', '水泵', 'NORMAL', '汽车备件', '发动机', '个', 480.00, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM oms_product WHERE sku = 'P0013');
INSERT INTO oms_inventory (warehouse_code, sku, qty_on_hand, qty_reserved, safety_qty, created_at, updated_at)
SELECT 'WH-SH', 'P0013', 200, 0, 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM oms_inventory WHERE warehouse_code = 'WH-SH' AND sku = 'P0013');

INSERT INTO oms_product (sku, name, type, category, spec, unit, price, status, created_at, updated_at)
SELECT 'P0014', '发电机', 'NORMAL', '汽车备件', '电器', '台', 1600.00, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM oms_product WHERE sku = 'P0014');
INSERT INTO oms_inventory (warehouse_code, sku, qty_on_hand, qty_reserved, safety_qty, created_at, updated_at)
SELECT 'WH-SH', 'P0014', 200, 0, 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM oms_inventory WHERE warehouse_code = 'WH-SH' AND sku = 'P0014');

INSERT INTO oms_product (sku, name, type, category, spec, unit, price, status, created_at, updated_at)
SELECT 'P0015', '蓄电池 60Ah', 'NORMAL', '汽车备件', '电器', '只', 680.00, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM oms_product WHERE sku = 'P0015');
INSERT INTO oms_inventory (warehouse_code, sku, qty_on_hand, qty_reserved, safety_qty, created_at, updated_at)
SELECT 'WH-SH', 'P0015', 200, 0, 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM oms_inventory WHERE warehouse_code = 'WH-SH' AND sku = 'P0015');

INSERT INTO oms_product (sku, name, type, category, spec, unit, price, status, created_at, updated_at)
SELECT 'P0016', '雨刮片 24寸', 'NORMAL', '汽车备件', '车身', '只', 55.00, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM oms_product WHERE sku = 'P0016');
INSERT INTO oms_inventory (warehouse_code, sku, qty_on_hand, qty_reserved, safety_qty, created_at, updated_at)
SELECT 'WH-SH', 'P0016', 200, 0, 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM oms_inventory WHERE warehouse_code = 'WH-SH' AND sku = 'P0016');

INSERT INTO oms_product (sku, name, type, category, spec, unit, price, status, created_at, updated_at)
SELECT 'P0017', '前减震器', 'NORMAL', '汽车备件', '底盘', '只', 780.00, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM oms_product WHERE sku = 'P0017');
INSERT INTO oms_inventory (warehouse_code, sku, qty_on_hand, qty_reserved, safety_qty, created_at, updated_at)
SELECT 'WH-SH', 'P0017', 200, 0, 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM oms_inventory WHERE warehouse_code = 'WH-SH' AND sku = 'P0017');

INSERT INTO oms_product (sku, name, type, category, spec, unit, price, status, created_at, updated_at)
SELECT 'P0018', '轮胎 225/55R18', 'NORMAL', '汽车备件', '轮胎', '条', 980.00, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM oms_product WHERE sku = 'P0018');
INSERT INTO oms_inventory (warehouse_code, sku, qty_on_hand, qty_reserved, safety_qty, created_at, updated_at)
SELECT 'WH-SH', 'P0018', 200, 0, 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM oms_inventory WHERE warehouse_code = 'WH-SH' AND sku = 'P0018');

INSERT INTO oms_product (sku, name, type, category, spec, unit, price, status, created_at, updated_at)
SELECT 'P0019', '大灯总成 左', 'NORMAL', '汽车备件', '车身', '个', 2600.00, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM oms_product WHERE sku = 'P0019');
INSERT INTO oms_inventory (warehouse_code, sku, qty_on_hand, qty_reserved, safety_qty, created_at, updated_at)
SELECT 'WH-SH', 'P0019', 200, 0, 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM oms_inventory WHERE warehouse_code = 'WH-SH' AND sku = 'P0019');

INSERT INTO oms_product (sku, name, type, category, spec, unit, price, status, created_at, updated_at)
SELECT 'P0020', '大灯总成 右', 'NORMAL', '汽车备件', '车身', '个', 2600.00, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM oms_product WHERE sku = 'P0020');
INSERT INTO oms_inventory (warehouse_code, sku, qty_on_hand, qty_reserved, safety_qty, created_at, updated_at)
SELECT 'WH-SH', 'P0020', 200, 0, 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM oms_inventory WHERE warehouse_code = 'WH-SH' AND sku = 'P0020');

INSERT INTO oms_product (sku, name, type, category, spec, unit, price, status, created_at, updated_at)
SELECT 'P0021', '前保险杠', 'NORMAL', '汽车备件', '车身', '个', 1300.00, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM oms_product WHERE sku = 'P0021');
INSERT INTO oms_inventory (warehouse_code, sku, qty_on_hand, qty_reserved, safety_qty, created_at, updated_at)
SELECT 'WH-SH', 'P0021', 200, 0, 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM oms_inventory WHERE warehouse_code = 'WH-SH' AND sku = 'P0021');

INSERT INTO oms_product (sku, name, type, category, spec, unit, price, status, created_at, updated_at)
SELECT 'P0022', '后视镜总成 左', 'NORMAL', '汽车备件', '车身', '个', 560.00, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM oms_product WHERE sku = 'P0022');
INSERT INTO oms_inventory (warehouse_code, sku, qty_on_hand, qty_reserved, safety_qty, created_at, updated_at)
SELECT 'WH-SH', 'P0022', 200, 0, 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM oms_inventory WHERE warehouse_code = 'WH-SH' AND sku = 'P0022');

INSERT INTO oms_product (sku, name, type, category, spec, unit, price, status, created_at, updated_at)
SELECT 'P0023', '离合器三件套', 'NORMAL', '汽车备件', '变速箱', '套', 1800.00, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM oms_product WHERE sku = 'P0023');
INSERT INTO oms_inventory (warehouse_code, sku, qty_on_hand, qty_reserved, safety_qty, created_at, updated_at)
SELECT 'WH-SH', 'P0023', 200, 0, 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM oms_inventory WHERE warehouse_code = 'WH-SH' AND sku = 'P0023');

INSERT INTO oms_product (sku, name, type, category, spec, unit, price, status, created_at, updated_at)
SELECT 'P0024', '氧传感器', 'NORMAL', '汽车备件', '发动机', '只', 320.00, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM oms_product WHERE sku = 'P0024');
INSERT INTO oms_inventory (warehouse_code, sku, qty_on_hand, qty_reserved, safety_qty, created_at, updated_at)
SELECT 'WH-SH', 'P0024', 200, 0, 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM oms_inventory WHERE warehouse_code = 'WH-SH' AND sku = 'P0024');

INSERT INTO oms_product (sku, name, type, category, spec, unit, price, status, created_at, updated_at)
SELECT 'P0025', '节气门总成', 'NORMAL', '汽车备件', '发动机', '个', 900.00, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM oms_product WHERE sku = 'P0025');
INSERT INTO oms_inventory (warehouse_code, sku, qty_on_hand, qty_reserved, safety_qty, created_at, updated_at)
SELECT 'WH-SH', 'P0025', 200, 0, 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM oms_inventory WHERE warehouse_code = 'WH-SH' AND sku = 'P0025');

INSERT INTO oms_product (sku, name, type, category, spec, unit, price, status, created_at, updated_at)
SELECT 'P0026', '燃油泵', 'NORMAL', '汽车备件', '发动机', '个', 1050.00, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM oms_product WHERE sku = 'P0026');
INSERT INTO oms_inventory (warehouse_code, sku, qty_on_hand, qty_reserved, safety_qty, created_at, updated_at)
SELECT 'WH-SH', 'P0026', 200, 0, 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM oms_inventory WHERE warehouse_code = 'WH-SH' AND sku = 'P0026');

INSERT INTO oms_product (sku, name, type, category, spec, unit, price, status, created_at, updated_at)
SELECT 'P0027', '空调压缩机', 'NORMAL', '汽车备件', '空调', '台', 3200.00, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM oms_product WHERE sku = 'P0027');
INSERT INTO oms_inventory (warehouse_code, sku, qty_on_hand, qty_reserved, safety_qty, created_at, updated_at)
SELECT 'WH-SH', 'P0027', 200, 0, 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM oms_inventory WHERE warehouse_code = 'WH-SH' AND sku = 'P0027');

INSERT INTO oms_product (sku, name, type, category, spec, unit, price, status, created_at, updated_at)
SELECT 'P0028', '冷媒 R134a', 'NORMAL', '汽车备件', '空调', '瓶', 68.00, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM oms_product WHERE sku = 'P0028');
INSERT INTO oms_inventory (warehouse_code, sku, qty_on_hand, qty_reserved, safety_qty, created_at, updated_at)
SELECT 'WH-SH', 'P0028', 200, 0, 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM oms_inventory WHERE warehouse_code = 'WH-SH' AND sku = 'P0028');

INSERT INTO oms_product (sku, name, type, category, spec, unit, price, status, created_at, updated_at)
SELECT 'P0029', '玻璃水 2L', 'NORMAL', '汽车备件', '油液', '瓶', 20.00, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM oms_product WHERE sku = 'P0029');
INSERT INTO oms_inventory (warehouse_code, sku, qty_on_hand, qty_reserved, safety_qty, created_at, updated_at)
SELECT 'WH-SH', 'P0029', 200, 0, 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM oms_inventory WHERE warehouse_code = 'WH-SH' AND sku = 'P0029');

INSERT INTO oms_product (sku, name, type, category, spec, unit, price, status, created_at, updated_at)
SELECT 'P0030', '车门密封条', 'NORMAL', '汽车备件', '车身', '条', 120.00, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM oms_product WHERE sku = 'P0030');
INSERT INTO oms_inventory (warehouse_code, sku, qty_on_hand, qty_reserved, safety_qty, created_at, updated_at)
SELECT 'WH-SH', 'P0030', 200, 0, 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM oms_inventory WHERE warehouse_code = 'WH-SH' AND sku = 'P0030');

