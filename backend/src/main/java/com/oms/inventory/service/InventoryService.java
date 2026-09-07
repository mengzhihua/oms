package com.oms.inventory.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.oms.basic.entity.Warehouse;
import com.oms.basic.mapper.WarehouseMapper;
import com.oms.common.BizException;
import com.oms.inventory.entity.ChannelStockPolicy;
import com.oms.inventory.entity.Inventory;
import com.oms.inventory.entity.InventoryTxn;
import com.oms.inventory.mapper.ChannelStockPolicyMapper;
import com.oms.inventory.mapper.InventoryMapper;
import com.oms.inventory.mapper.InventoryTxnMapper;
import com.oms.system.auth.CurrentUser;
import com.oms.system.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 库存中心：多仓可用库存 = 在库(qty_on_hand) - 预占(qty_reserved)。
 * 所有变动均写 oms_inventory_txn 流水。
 */
@Service
@RequiredArgsConstructor
public class InventoryService {
    public static final String TXN_SYNC = "SYNC";
    public static final String TXN_ADJUST = "ADJUST";
    public static final String TXN_RESERVE = "RESERVE";
    public static final String TXN_RELEASE = "RELEASE";
    public static final String TXN_DEDUCT = "DEDUCT";
    public static final String TXN_RETURN = "RETURN";

    private final InventoryMapper inventoryMapper;
    private final InventoryTxnMapper txnMapper;
    private final ChannelStockPolicyMapper policyMapper;
    private final WarehouseMapper warehouseMapper;

    public Inventory find(String warehouseCode, String sku) {
        return inventoryMapper.selectOne(new LambdaQueryWrapper<Inventory>()
                .eq(Inventory::getWarehouseCode, warehouseCode).eq(Inventory::getSku, sku));
    }

    public int available(String warehouseCode, String sku) {
        Inventory inv = find(warehouseCode, sku);
        return inv == null ? 0 : available(inv);
    }

    public static int available(Inventory inv) {
        return n(inv.getQtyOnHand()) - n(inv.getQtyReserved());
    }

    /** 全部仓库可用库存合计 */
    public int totalAvailable(String sku) {
        return inventoryMapper.selectList(new LambdaQueryWrapper<Inventory>().eq(Inventory::getSku, sku))
                .stream().mapToInt(InventoryService::available).sum();
    }

    /** 按 sku 分组的各仓库存 */
    public Map<String, List<Inventory>> bySkus(List<String> skus) {
        if (skus.isEmpty()) {
            return java.util.Collections.emptyMap();
        }
        return inventoryMapper.selectList(new LambdaQueryWrapper<Inventory>().in(Inventory::getSku, skus))
                .stream().collect(Collectors.groupingBy(Inventory::getSku));
    }

    /** WMS 全量同步：直接设置在库数量 */
    @Transactional
    public Inventory sync(String warehouseCode, String sku, int qtyOnHand, String refNo) {
        Inventory inv = getOrCreate(warehouseCode, sku);
        int delta = qtyOnHand - n(inv.getQtyOnHand());
        inv.setQtyOnHand(qtyOnHand);
        inventoryMapper.updateById(inv);
        log(inv, TXN_SYNC, delta, refNo, "WMS 库存同步");
        return inv;
    }

    /** 手工调整在库数量（正负） */
    @Transactional
    public Inventory adjust(String warehouseCode, String sku, int delta, String remark) {
        Inventory inv = getOrCreate(warehouseCode, sku);
        int after = n(inv.getQtyOnHand()) + delta;
        if (after < n(inv.getQtyReserved())) {
            throw new BizException("调整后在库数量不能小于已预占数量");
        }
        inv.setQtyOnHand(after);
        inventoryMapper.updateById(inv);
        log(inv, TXN_ADJUST, delta, null, remark);
        return inv;
    }

    @Transactional
    public void reserve(String warehouseCode, String sku, int qty, String refNo) {
        Inventory inv = find(warehouseCode, sku);
        if (inv == null || available(inv) < qty) {
            throw new BizException("仓库 " + warehouseCode + " SKU " + sku + " 可用库存不足，需要 " + qty
                    + "，可用 " + (inv == null ? 0 : available(inv)));
        }
        inv.setQtyReserved(n(inv.getQtyReserved()) + qty);
        inventoryMapper.updateById(inv);
        log(inv, TXN_RESERVE, qty, refNo, "订单预占");
    }

    @Transactional
    public void release(String warehouseCode, String sku, int qty, String refNo) {
        Inventory inv = find(warehouseCode, sku);
        if (inv == null) {
            return;
        }
        inv.setQtyReserved(Math.max(0, n(inv.getQtyReserved()) - qty));
        inventoryMapper.updateById(inv);
        log(inv, TXN_RELEASE, -qty, refNo, "释放预占");
    }

    /** 发货扣减：同时减少预占与在库 */
    @Transactional
    public void deduct(String warehouseCode, String sku, int qty, String refNo) {
        Inventory inv = getOrCreate(warehouseCode, sku);
        inv.setQtyReserved(Math.max(0, n(inv.getQtyReserved()) - qty));
        inv.setQtyOnHand(Math.max(0, n(inv.getQtyOnHand()) - qty));
        inventoryMapper.updateById(inv);
        log(inv, TXN_DEDUCT, -qty, refNo, "发货扣减");
    }

    /** 退货入库回增 */
    @Transactional
    public void restock(String warehouseCode, String sku, int qty, String refNo) {
        Inventory inv = getOrCreate(warehouseCode, sku);
        inv.setQtyOnHand(n(inv.getQtyOnHand()) + qty);
        inventoryMapper.updateById(inv);
        log(inv, TXN_RETURN, qty, refNo, "退货入库");
    }

    /**
     * 渠道可售库存：按店铺策略计算（sku 级策略优先于店铺级；无策略则全部仓可用库存 100%）。
     * 同时扣除安全库存。
     */
    public int channelAvailable(String shopCode, String sku) {
        List<ChannelStockPolicy> policies = policyMapper.selectList(new LambdaQueryWrapper<ChannelStockPolicy>()
                .eq(ChannelStockPolicy::getShopCode, shopCode).eq(ChannelStockPolicy::getStatus, 1)
                .and(w -> w.eq(ChannelStockPolicy::getSku, sku).or().isNull(ChannelStockPolicy::getSku).or().eq(ChannelStockPolicy::getSku, "")));
        ChannelStockPolicy p = policies.stream().filter(x -> sku.equals(x.getSku())).findFirst()
                .orElse(policies.stream().findFirst().orElse(null));
        LambdaQueryWrapper<Inventory> qw = new LambdaQueryWrapper<Inventory>().eq(Inventory::getSku, sku);
        if (p != null && p.getWarehouseCode() != null && !p.getWarehouseCode().isEmpty()) {
            qw.eq(Inventory::getWarehouseCode, p.getWarehouseCode());
        }
        int base = inventoryMapper.selectList(qw).stream()
                .mapToInt(i -> Math.max(0, available(i) - n(i.getSafetyQty()))).sum();
        if (p == null) {
            return base;
        }
        if ("FIXED".equals(p.getMode())) {
            return Math.min(base, n(p.getFixedQty()));
        }
        return base * (p.getRatio() == null ? 100 : p.getRatio()) / 100;
    }

    public List<Warehouse> activeWarehouses() {
        return warehouseMapper.selectList(new LambdaQueryWrapper<Warehouse>()
                .eq(Warehouse::getStatus, 1).orderByAsc(Warehouse::getPriority).orderByAsc(Warehouse::getId));
    }

    private Inventory getOrCreate(String warehouseCode, String sku) {
        Inventory inv = find(warehouseCode, sku);
        if (inv == null) {
            inv = new Inventory();
            inv.setWarehouseCode(warehouseCode);
            inv.setSku(sku);
            inv.setQtyOnHand(0);
            inv.setQtyReserved(0);
            inv.setSafetyQty(0);
            inventoryMapper.insert(inv);
        }
        return inv;
    }

    private void log(Inventory inv, String type, int qty, String refNo, String remark) {
        InventoryTxn t = new InventoryTxn();
        t.setWarehouseCode(inv.getWarehouseCode());
        t.setSku(inv.getSku());
        t.setType(type);
        t.setQty(qty);
        t.setOnHandAfter(inv.getQtyOnHand());
        t.setReservedAfter(inv.getQtyReserved());
        t.setRefNo(refNo);
        t.setRemark(remark);
        User u = CurrentUser.get();
        t.setOperator(u != null ? u.getUsername() : "system");
        txnMapper.insert(t);
    }

    private static int n(Integer v) {
        return v == null ? 0 : v;
    }
}
