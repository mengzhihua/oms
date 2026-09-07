package com.oms.inventory.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.oms.common.BizException;
import com.oms.common.Csv;
import com.oms.common.R;
import com.oms.inventory.entity.Inventory;
import com.oms.inventory.entity.InventoryTxn;
import com.oms.inventory.mapper.InventoryMapper;
import com.oms.inventory.mapper.InventoryTxnMapper;
import com.oms.inventory.service.InventoryService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {
    private final InventoryMapper inventoryMapper;
    private final InventoryTxnMapper txnMapper;
    private final InventoryService inventoryService;

    @GetMapping("/page")
    public R<Page<Map<String, Object>>> page(@RequestParam(defaultValue = "1") long current,
                                             @RequestParam(defaultValue = "20") long size,
                                             @RequestParam(required = false) String warehouseCode,
                                             @RequestParam(required = false) String sku,
                                             @RequestParam(required = false) Boolean lowStock) {
        LambdaQueryWrapper<Inventory> qw = new LambdaQueryWrapper<>();
        qw.eq(StringUtils.isNotBlank(warehouseCode), Inventory::getWarehouseCode, warehouseCode)
                .like(StringUtils.isNotBlank(sku), Inventory::getSku, sku)
                .orderByAsc(Inventory::getWarehouseCode).orderByAsc(Inventory::getSku);
        if (Boolean.TRUE.equals(lowStock)) {
            qw.apply("qty_on_hand - qty_reserved <= safety_qty");
        }
        Page<Inventory> p = inventoryMapper.selectPage(new Page<>(current, size), qw);
        Page<Map<String, Object>> out = new Page<>(p.getCurrent(), p.getSize(), p.getTotal());
        List<Map<String, Object>> rows = new java.util.ArrayList<>();
        for (Inventory i : p.getRecords()) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", i.getId());
            m.put("warehouseCode", i.getWarehouseCode());
            m.put("sku", i.getSku());
            m.put("qtyOnHand", i.getQtyOnHand());
            m.put("qtyReserved", i.getQtyReserved());
            m.put("qtyAvailable", InventoryService.available(i));
            m.put("safetyQty", i.getSafetyQty());
            m.put("updatedAt", i.getUpdatedAt());
            rows.add(m);
        }
        out.setRecords(rows);
        return R.ok(out);
    }

    @GetMapping("/available")
    public R<Map<String, Integer>> available(@RequestParam String sku, @RequestParam(required = false) String shopCode) {
        Map<String, Integer> m = new HashMap<>();
        m.put("total", inventoryService.totalAvailable(sku));
        if (StringUtils.isNotBlank(shopCode)) {
            m.put("channel", inventoryService.channelAvailable(shopCode, sku));
        }
        return R.ok(m);
    }

    @Data
    public static class AdjustRequest {
        private String warehouseCode;
        private String sku;
        private Integer delta;
        private Integer safetyQty;
        private String remark;
    }

    /** 手工调整在库数量 / 安全库存 */
    @PostMapping("/adjust")
    public R<Inventory> adjust(@RequestBody AdjustRequest req) {
        if (StringUtils.isBlank(req.getWarehouseCode()) || StringUtils.isBlank(req.getSku())) {
            throw new BizException("仓库与 SKU 必填");
        }
        int delta = req.getDelta() == null ? 0 : req.getDelta();
        Inventory inv = inventoryService.adjust(req.getWarehouseCode(), req.getSku(), delta,
                StringUtils.isBlank(req.getRemark()) ? "手工调整" : req.getRemark());
        if (req.getSafetyQty() != null) {
            inv.setSafetyQty(req.getSafetyQty());
            inventoryMapper.updateById(inv);
        }
        return R.ok(inv);
    }

    @GetMapping("/txn/page")
    public R<Page<InventoryTxn>> txnPage(@RequestParam(defaultValue = "1") long current,
                                         @RequestParam(defaultValue = "20") long size,
                                         @RequestParam(required = false) String warehouseCode,
                                         @RequestParam(required = false) String sku,
                                         @RequestParam(required = false) String type,
                                         @RequestParam(required = false) String refNo) {
        LambdaQueryWrapper<InventoryTxn> qw = new LambdaQueryWrapper<>();
        qw.eq(StringUtils.isNotBlank(warehouseCode), InventoryTxn::getWarehouseCode, warehouseCode)
                .like(StringUtils.isNotBlank(sku), InventoryTxn::getSku, sku)
                .eq(StringUtils.isNotBlank(type), InventoryTxn::getType, type)
                .eq(StringUtils.isNotBlank(refNo), InventoryTxn::getRefNo, refNo)
                .orderByDesc(InventoryTxn::getId);
        return R.ok(txnMapper.selectPage(new Page<>(current, size), qw));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(@RequestParam(required = false) String warehouseCode) {
        LambdaQueryWrapper<Inventory> qw = new LambdaQueryWrapper<>();
        qw.eq(StringUtils.isNotBlank(warehouseCode), Inventory::getWarehouseCode, warehouseCode)
                .orderByAsc(Inventory::getWarehouseCode).orderByAsc(Inventory::getSku);
        String[] headers = {"仓库", "SKU", "在库", "预占", "可用", "安全库存"};
        return Csv.download("inventory.csv", headers, inventoryMapper.selectList(qw), i -> new Object[]{
                i.getWarehouseCode(), i.getSku(), i.getQtyOnHand(), i.getQtyReserved(), InventoryService.available(i), i.getSafetyQty()});
    }
}
