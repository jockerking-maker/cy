package com.hospital.drugmanagement.controller;

import com.hospital.drugmanagement.common.anno.RequireRole;
import com.hospital.drugmanagement.entity.PurchaseOrder;
import com.hospital.drugmanagement.entity.PurchaseOrderItem;
import com.hospital.drugmanagement.entity.SupplierInfo;
import com.hospital.drugmanagement.entity.DrugInfo;
import com.hospital.drugmanagement.entity.SysUser;
import com.hospital.drugmanagement.service.IPurchaseOrderService;
import com.hospital.drugmanagement.service.IPurchaseOrderItemService;
import com.hospital.drugmanagement.service.ISupplierInfoService;
import com.hospital.drugmanagement.service.DrugInfoService;
import com.hospital.drugmanagement.service.IAuditRecordService;
import com.hospital.drugmanagement.service.ISysUserService;
import com.hospital.drugmanagement.entity.AuditRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 采购订单 + 审核
 * <p>
 * 状态流转：0 待审核 → 1 已审核 → 2 已入库 / 3 已取消；审核记录写入 audit_record 表。
 */
@RestController
@RequestMapping("/api/purchase/order")
@CrossOrigin(origins = "*")
@RequireRole({"ADMIN", "PURCHASER", "AUDITOR", "WAREHOUSE"})
public class PurchaseOrderController {

    @Autowired
    private IPurchaseOrderService purchaseOrderService;
    
    @Autowired
    private IPurchaseOrderItemService purchaseOrderItemService;
    
    @Autowired
    private ISupplierInfoService supplierInfoService;
    
    @Autowired
    private DrugInfoService drugInfoService;
    
    @Autowired
    private IAuditRecordService auditRecordService;
    
    @Autowired
    private ISysUserService sysUserService;

    /** 查询采购单列表，含供应商名称。 */
    @GetMapping("/list")
    public Map<String, Object> list(
        @RequestParam(required = false) String orderNo,
        @RequestParam(required = false) Long supplierId,
        @RequestParam(required = false) Integer status
    ) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 构建查询条件
            com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<PurchaseOrder> queryWrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
            if (orderNo != null && !orderNo.isEmpty()) {
                queryWrapper.like(PurchaseOrder::getOrderNo, orderNo);
            }
            if (supplierId != null) {
                queryWrapper.eq(PurchaseOrder::getSupplierId, supplierId);
            }
            if (status != null) {
                queryWrapper.eq(PurchaseOrder::getStatus, status);
            }
            
            List<PurchaseOrder> orders = purchaseOrderService.list(queryWrapper);
            
            // 转换为包含供应商名称的列表
            List<Map<String, Object>> orderList = new java.util.ArrayList<>();
            for (PurchaseOrder order : orders) {
                Map<String, Object> orderMap = new java.util.HashMap<>();
                orderMap.put("orderId", order.getOrderId());
                orderMap.put("orderNo", order.getOrderNo());
                orderMap.put("supplierId", order.getSupplierId());
                
                // 获取供应商名称
                if (order.getSupplierId() != null) {
                    SupplierInfo supplier = supplierInfoService.getById(order.getSupplierId());
                    if (supplier != null) {
                        orderMap.put("supplierName", supplier.getSupplierName());
                    }
                }
                
                orderMap.put("orderDate", order.getOrderDate());
                orderMap.put("totalAmount", order.getTotalAmount());
                orderMap.put("status", order.getStatus());
                orderMap.put("remark", order.getRemark());
                orderMap.put("createTime", order.getCreateTime());
                orderList.add(orderMap);
            }
            
            result.put("code", 200);
            result.put("msg", "success");
            result.put("data", orderList);
            result.put("total", orderList.size());
        } catch (Exception e) {
            result.put("code", 500);
            result.put("msg", "查询失败：" + e.getMessage());
            result.put("data", null);
            result.put("total", 0);
        }
        return result;
    }

    /** 查询采购单详情：主单、明细、审核记录。 */
    @GetMapping("/{id}")
    public Map<String, Object> getById(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            PurchaseOrder order = purchaseOrderService.getById(id);
            if (order != null) {
                Map<String, Object> orderMap = new java.util.HashMap<>();
                orderMap.put("orderId", order.getOrderId());
                orderMap.put("orderNo", order.getOrderNo());
                orderMap.put("supplierId", order.getSupplierId());
                
                // 获取供应商名称
                if (order.getSupplierId() != null) {
                    SupplierInfo supplier = supplierInfoService.getById(order.getSupplierId());
                    if (supplier != null) {
                        orderMap.put("supplierName", supplier.getSupplierName());
                    }
                }
                
                orderMap.put("orderDate", order.getOrderDate());
                orderMap.put("totalAmount", order.getTotalAmount());
                orderMap.put("status", order.getStatus());
                orderMap.put("remark", order.getRemark());
                orderMap.put("createTime", order.getCreateTime());
                
                // 获取采购明细
                com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<PurchaseOrderItem> itemWrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
                itemWrapper.eq(PurchaseOrderItem::getOrderId, id);
                List<PurchaseOrderItem> items = purchaseOrderItemService.list(itemWrapper);
                
                List<Map<String, Object>> itemList = new java.util.ArrayList<>();
                for (PurchaseOrderItem item : items) {
                    Map<String, Object> itemMap = new java.util.HashMap<>();
                    itemMap.put("itemId", item.getItemId());
                    itemMap.put("drugId", item.getDrugId());
                    
                    // 获取药品信息
                    if (item.getDrugId() != null) {
                        DrugInfo drug = drugInfoService.getById(item.getDrugId());
                        if (drug != null) {
                            itemMap.put("drugName", drug.getDrugName());
                            itemMap.put("spec", drug.getSpec());
                            itemMap.put("unit", drug.getUnit());
                        }
                    }
                    
                    itemMap.put("purchaseNum", item.getPurchaseNum());
                    itemMap.put("purchasePrice", item.getPurchasePrice());
                    itemMap.put("amount", item.getAmount());
                    itemList.add(itemMap);
                }
                
                orderMap.put("items", itemList);
                
                // 获取审核记录
                List<AuditRecord> auditRecords = auditRecordService.getAuditRecordsByOrderId(id);
                orderMap.put("auditRecords", auditRecords);
                
                result.put("data", orderMap);
            }
            result.put("code", 200);
            result.put("msg", "success");
        } catch (Exception e) {
            result.put("code", 500);
            result.put("msg", "查询失败：" + e.getMessage());
            result.put("data", null);
        }
        return result;
    }

    /** 新建采购单及明细，校验供应商与单号唯一性。 */
    @PostMapping
    @RequireRole({"ADMIN", "PURCHASER"})
    public Map<String, Object> save(
            @RequestAttribute("currentUserId") Long currentUserId,
            @RequestBody Map<String, Object> purchaseOrderData) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 检查采购单号是否已存在
            String orderNo = (String) purchaseOrderData.get("orderNo");
            com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<PurchaseOrder> orderNoWrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
            orderNoWrapper.eq(PurchaseOrder::getOrderNo, orderNo);
            if (purchaseOrderService.count(orderNoWrapper) > 0) {
                result.put("code", 400);
                result.put("msg", "采购单号已存在");
                result.put("data", null);
                return result;
            }

            Long supplierId = Long.valueOf(purchaseOrderData.get("supplierId").toString());
            SupplierInfo supplier = supplierInfoService.getById(supplierId);
            if (supplier == null) {
                result.put("code", 400);
                result.put("msg", "供应商不存在");
                result.put("data", null);
                return result;
            }
            if (supplier.getStatus() == null || supplier.getStatus() != 1) {
                result.put("code", 400);
                result.put("msg", "供应商已禁用，无法创建采购单");
                result.put("data", null);
                return result;
            }
            
            // 创建采购单
            PurchaseOrder purchaseOrder = new PurchaseOrder();
            purchaseOrder.setOrderNo(orderNo);
            purchaseOrder.setSupplierId(supplierId);
            purchaseOrder.setOrderDate(java.time.LocalDateTime.parse((String) purchaseOrderData.get("orderDate")));
            purchaseOrder.setStatus((Integer) purchaseOrderData.get("status"));
            purchaseOrder.setRemark((String) purchaseOrderData.get("remark"));
            purchaseOrder.setTotalAmount(new java.math.BigDecimal(purchaseOrderData.get("totalAmount").toString()));
            purchaseOrder.setCreateUserId(currentUserId);
            
            boolean success = purchaseOrderService.save(purchaseOrder);
            
            // 保存采购明细
            if (success) {
                java.util.List<java.util.Map<String, Object>> items = (java.util.List<java.util.Map<String, Object>>) purchaseOrderData.get("items");
                if (items != null && !items.isEmpty()) {
                    for (java.util.Map<String, Object> itemData : items) {
                        com.hospital.drugmanagement.entity.PurchaseOrderItem item = new com.hospital.drugmanagement.entity.PurchaseOrderItem();
                        item.setOrderId(purchaseOrder.getOrderId());
                        item.setDrugId(Long.valueOf(itemData.get("drugId").toString()));
                        item.setPurchaseNum(Integer.valueOf(itemData.get("purchaseNum").toString()));
                        item.setPurchasePrice(new java.math.BigDecimal(itemData.get("purchasePrice").toString()));
                        item.setAmount(new java.math.BigDecimal(itemData.get("amount").toString()));
                        purchaseOrderItemService.save(item);
                    }
                }
            }
            
            result.put("code", success ? 200 : 500);
            result.put("msg", success ? "保存成功" : "保存失败");
            result.put("data", null);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("msg", "保存失败：" + e.getMessage());
            result.put("data", null);
        }
        return result;
    }

    /** 修改采购单基本信息。 */
    @PutMapping
    @RequireRole({"ADMIN", "PURCHASER"})
    public Map<String, Object> update(@RequestBody PurchaseOrder purchaseOrder) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 检查采购单号是否已存在（排除当前订单）
            com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<PurchaseOrder> orderNoWrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
            orderNoWrapper.eq(PurchaseOrder::getOrderNo, purchaseOrder.getOrderNo());
            orderNoWrapper.ne(PurchaseOrder::getOrderId, purchaseOrder.getOrderId());
            if (purchaseOrderService.count(orderNoWrapper) > 0) {
                result.put("code", 400);
                result.put("msg", "采购单号已存在");
                result.put("data", null);
                return result;
            }
            
            boolean success = purchaseOrderService.updateById(purchaseOrder);
            result.put("code", success ? 200 : 500);
            result.put("msg", success ? "更新成功" : "更新失败");
            result.put("data", null);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("msg", "更新失败：" + e.getMessage());
            result.put("data", null);
        }
        return result;
    }

    /** 级联删除采购单（含明细、审核记录），仅待审核等状态可删。 */
    @DeleteMapping("/{id}")
    @RequireRole({"ADMIN", "PURCHASER"})
    public Map<String, Object> delete(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            purchaseOrderService.deleteOrderCascade(id);
            result.put("code", 200);
            result.put("msg", "删除成功");
            result.put("data", null);
        } catch (IllegalArgumentException e) {
            result.put("code", 400);
            result.put("msg", e.getMessage());
            result.put("data", null);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("msg", "删除失败：" + e.getMessage());
            result.put("data", null);
        }
        return result;
    }

    /** 批量删除采购单，跳过不可删记录。 */
    @PostMapping("/batch-delete")
    @RequireRole({"ADMIN", "PURCHASER"})
    public Map<String, Object> batchDelete(@RequestBody Map<String, Object> data) {
        Map<String, Object> result = new HashMap<>();
        try {
            if (data.get("orderIds") == null) {
                result.put("code", 400);
                result.put("msg", "采购单 ID 列表不能为空");
                result.put("data", null);
                return result;
            }

            List<Long> orderIds = ((List<?>) data.get("orderIds")).stream()
                    .map(id -> Long.valueOf(id.toString()))
                    .collect(Collectors.toList());
            Map<String, Object> summary = purchaseOrderService.batchDeleteOrders(orderIds);
            int deleted = summary.get("deleted") != null ? Integer.parseInt(summary.get("deleted").toString()) : 0;
            int skipped = summary.get("skipped") != null ? Integer.parseInt(summary.get("skipped").toString()) : 0;
            String msg = String.format("批量删除完成：成功 %d 条", deleted);
            if (skipped > 0) {
                msg += String.format("，跳过 %d 条（不可删除）", skipped);
            }
            result.put("code", 200);
            result.put("msg", msg);
            result.put("data", summary);
        } catch (IllegalArgumentException e) {
            result.put("code", 400);
            result.put("msg", e.getMessage());
            result.put("data", null);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("msg", "批量删除失败：" + e.getMessage());
            result.put("data", null);
        }
        return result;
    }

    /** 审核采购单：通过 → 状态 1，驳回 → 状态 4，并写入审核记录。 */
    @PostMapping("/audit/{id}")
    @RequireRole({"ADMIN", "AUDITOR"})
    public Map<String, Object> audit(
            @PathVariable Long id,
            @RequestBody Map<String, Object> auditData,
            @RequestAttribute("currentUserId") Long userId) {
        Map<String, Object> result = new HashMap<>();
        try {
            SysUser user = sysUserService.getById(userId);
            if (user == null) {
                result.put("code", 404);
                result.put("msg", "用户不存在");
                result.put("data", null);
                return result;
            }

            PurchaseOrder order = purchaseOrderService.getById(id);
            if (order != null) {
                boolean passed = (Boolean) auditData.get("passed");
                String remark = (String) auditData.get("remark");

                AuditRecord auditRecord = new AuditRecord();
                auditRecord.setOrderId(id);
                auditRecord.setAuditUserId(userId);
                auditRecord.setAuditUserName(user.getRealName());
                auditRecord.setAuditResult(passed ? 1 : 2);
                auditRecord.setAuditRemark(remark);
                auditRecord.setAuditLevel(1);
                auditRecordService.saveAuditRecord(auditRecord);

                if (passed) {
                    order.setStatus(1);
                } else {
                    order.setStatus(4);
                }
                purchaseOrderService.updateById(order);

                result.put("code", 200);
                result.put("msg", "审核成功");
                result.put("data", null);
            } else {
                result.put("code", 404);
                result.put("msg", "采购单不存在");
                result.put("data", null);
            }
        } catch (Exception e) {
            result.put("code", 500);
            result.put("msg", "审核失败：" + e.getMessage());
            result.put("data", null);
        }
        return result;
    }
    
    /** 作废采购单（待审核/已审核状态可作废）。 */
    @PostMapping("/cancel/{id}")
    @RequireRole({"ADMIN", "PURCHASER"})
    public Map<String, Object> cancel(@PathVariable Long id, @RequestBody Map<String, Object> cancelData) {
        Map<String, Object> result = new HashMap<>();
        try {
            PurchaseOrder order = purchaseOrderService.getById(id);
            if (order != null) {
                // 检查订单状态是否可以作废
                if (order.getStatus() == 0 || order.getStatus() == 1) {
                    order.setStatus(3); // 已取消
                    purchaseOrderService.updateById(order);
                    result.put("code", 200);
                    result.put("msg", "作废成功");
                    result.put("data", null);
                } else {
                    result.put("code", 400);
                    result.put("msg", "该订单状态不允许作废");
                    result.put("data", null);
                }
            } else {
                result.put("code", 404);
                result.put("msg", "采购单不存在");
                result.put("data", null);
            }
        } catch (Exception e) {
            result.put("code", 500);
            result.put("msg", "作废失败：" + e.getMessage());
            result.put("data", null);
        }
        return result;
    }
}