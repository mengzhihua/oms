package com.oms.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.oms.basic.entity.RoutingRule;
import com.oms.basic.entity.Shop;
import com.oms.basic.entity.Warehouse;
import com.oms.basic.mapper.RoutingRuleMapper;
import com.oms.basic.mapper.ShopMapper;
import com.oms.inventory.entity.Inventory;
import com.oms.inventory.service.InventoryService;
import com.oms.order.entity.SalesOrder;
import com.oms.order.entity.SalesOrderItem;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 分仓路由：
 * 1. 按优先级匹配路由规则（渠道/店铺/省份/SKU 为空表示不限），命中规则的仓库排最前，并带出承运商
 * 2. 其后依次为店铺默认仓、所有启用仓库（按仓库优先级）
 * 3. 优先选择能整单满足的仓库；否则按候选顺序贪心拆分到多个仓库；仍不够则报缺货
 */
@Service
@RequiredArgsConstructor
public class RoutingService {
    private final RoutingRuleMapper ruleMapper;
    private final ShopMapper shopMapper;
    private final InventoryService inventoryService;

    @Data
    public static class Plan {
        /** warehouseCode -> (sku -> qty) */
        private LinkedHashMap<String, Map<String, Integer>> allocations = new LinkedHashMap<>();
        private String carrierCode;
        private List<String> shortage = new ArrayList<>();

        public boolean isSplit() {
            return allocations.size() > 1;
        }
    }

    public Plan plan(SalesOrder order, List<SalesOrderItem> items) {
        Map<String, Integer> need = new LinkedHashMap<>();
        for (SalesOrderItem it : items) {
            need.merge(it.getSku(), it.getQty(), Integer::sum);
        }
        Plan plan = new Plan();
        List<String> candidates = candidateWarehouses(order, need.keySet(), plan);
        Map<String, List<Inventory>> inv = inventoryService.bySkus(new ArrayList<>(need.keySet()));
        Map<String, Map<String, Integer>> avail = new HashMap<>();
        inv.forEach((sku, list) -> list.forEach(i ->
                avail.computeIfAbsent(i.getWarehouseCode(), k -> new HashMap<>()).put(sku, InventoryService.available(i))));

        for (String wh : candidates) {
            Map<String, Integer> a = avail.getOrDefault(wh, Collections.emptyMap());
            boolean full = need.entrySet().stream().allMatch(e -> a.getOrDefault(e.getKey(), 0) >= e.getValue());
            if (full) {
                plan.getAllocations().put(wh, new LinkedHashMap<>(need));
                return plan;
            }
        }
        Map<String, Integer> remain = new LinkedHashMap<>(need);
        for (String wh : candidates) {
            Map<String, Integer> a = avail.getOrDefault(wh, Collections.emptyMap());
            Map<String, Integer> take = new LinkedHashMap<>();
            for (Map.Entry<String, Integer> e : remain.entrySet()) {
                int can = Math.min(e.getValue(), a.getOrDefault(e.getKey(), 0));
                if (can > 0) {
                    take.put(e.getKey(), can);
                }
            }
            if (!take.isEmpty()) {
                plan.getAllocations().put(wh, take);
                take.forEach((sku, q) -> remain.merge(sku, -q, Integer::sum));
                remain.values().removeIf(v -> v <= 0);
            }
            if (remain.isEmpty()) {
                break;
            }
        }
        remain.forEach((sku, q) -> plan.getShortage().add(sku + " 缺 " + q));
        return plan;
    }

    private List<String> candidateWarehouses(SalesOrder order, Set<String> skus, Plan plan) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        List<RoutingRule> rules = ruleMapper.selectList(new LambdaQueryWrapper<RoutingRule>()
                .eq(RoutingRule::getStatus, 1).orderByAsc(RoutingRule::getPriority).orderByAsc(RoutingRule::getId));
        for (RoutingRule r : rules) {
            if (eq(r.getChannelCode(), order.getChannelCode()) && eq(r.getShopCode(), order.getShopCode())
                    && contains(r.getProvince(), order.getProvince())
                    && (isBlank(r.getSku()) || skus.contains(r.getSku()))) {
                out.add(r.getWarehouseCode());
                if (plan.getCarrierCode() == null && !isBlank(r.getCarrierCode())) {
                    plan.setCarrierCode(r.getCarrierCode());
                }
            }
        }
        Shop shop = shopMapper.selectOne(new LambdaQueryWrapper<Shop>().eq(Shop::getCode, order.getShopCode()));
        if (shop != null && !isBlank(shop.getDefaultWarehouseCode())) {
            out.add(shop.getDefaultWarehouseCode());
        }
        out.addAll(inventoryService.activeWarehouses().stream().map(Warehouse::getCode).collect(Collectors.toList()));
        return new ArrayList<>(out);
    }

    private static boolean eq(String ruleValue, String actual) {
        return isBlank(ruleValue) || ruleValue.equals(actual);
    }

    private static boolean contains(String ruleValue, String actual) {
        return isBlank(ruleValue) || (actual != null && actual.contains(ruleValue));
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
