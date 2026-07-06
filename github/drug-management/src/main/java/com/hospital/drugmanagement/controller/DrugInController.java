package com.hospital.drugmanagement.controller;

import com.hospital.drugmanagement.common.anno.RequireRole;
import com.hospital.drugmanagement.entity.DrugIn;
import com.hospital.drugmanagement.service.IDrugInService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
/**
 * 药品入库接口：采购入库、补货入库、批次号生成。
 * <p>
 * 仅 ADMIN、WAREHOUSE 可访问；入库逻辑在 {@link com.hospital.drugmanagement.service.impl.DrugInServiceImpl} 中事务处理。
 */
@RestController
@RequestMapping("/api/drug/in")
@CrossOrigin(origins = "*")
@RequireRole({"ADMIN", "WAREHOUSE"})
public class DrugInController {

    @Autowired
    private IDrugInService drugInService;

    /** 生成下一个入库批次号（PCyyyyMMdd001）。 */
    @GetMapping("/next-batch-no")
    public Map<String, Object> nextBatchNo() {
        Map<String, Object> result = new HashMap<>();
        try {
            String batchNo = drugInService.generateBatchNo();
            result.put("code", 200);
            result.put("msg", "success");
            result.put("data", batchNo);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("msg", "生成批次号失败：" + e.getMessage());
            result.put("data", null);
        }
        return result;
    }

    /**
     * 获取入库单列表
     */
    @GetMapping("/list")
    public Map<String, Object> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String inNo,
            @RequestParam(required = false) String drugName,
            @RequestParam(required = false) Long warehouseId
    ) {
        Map<String, Object> result = new HashMap<>();
        try {
            Map<String, Object> data = drugInService.getDrugInList(page, size, inNo, drugName, warehouseId);
            result.put("code", 200);
            result.put("msg", "success");
            result.put("data", data.get("data"));
            result.put("total", data.get("total"));
        } catch (Exception e) {
            result.put("code", 500);
            result.put("msg", "查询失败：" + e.getMessage());
            result.put("data", null);
            result.put("total", 0);
        }
        return result;
    }

    /**
     * 获取入库单详情
     */
    @GetMapping("/{id}")
    public Map<String, Object> getById(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            DrugIn drugIn = drugInService.getById(id);
            if (drugIn == null) {
                result.put("code", 404);
                result.put("msg", "入库单不存在");
                result.put("data", null);
            } else {
                result.put("code", 200);
                result.put("msg", "success");
                result.put("data", drugIn);
            }
        } catch (Exception e) {
            result.put("code", 500);
            result.put("msg", "查询失败：" + e.getMessage());
            result.put("data", null);
        }
        return result;
    }

    /**
     * 新增入库单
     */
    @PostMapping
    public Map<String, Object> save(
            @RequestAttribute("currentUserId") Long currentUserId,
            @RequestBody Map<String, Object> drugInData) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 创建入库单对象
            DrugIn drugIn = new DrugIn();
            drugIn.setDrugId(Long.valueOf(drugInData.get("drugId").toString()));
            drugIn.setWarehouseId(Long.valueOf(drugInData.get("warehouseId").toString()));
            drugIn.setQuantity(Integer.valueOf(drugInData.get("quantity").toString()));
            drugIn.setBatchNo((String) drugInData.get("batchNo"));
            drugIn.setPurchasePrice(new java.math.BigDecimal(drugInData.get("purchasePrice").toString()));
            drugIn.setProductionDate(java.time.LocalDate.parse((String) drugInData.get("productionDate")));
            drugIn.setExpiryDate(java.time.LocalDate.parse((String) drugInData.get("expiryDate")));
            
            // 设置关联采购单 ID（可选）
            if (drugInData.get("orderId") != null) {
                drugIn.setOrderId(Long.valueOf(drugInData.get("orderId").toString()));
            }
            
            // 设置备注（可选）
            if (drugInData.get("inType") != null) {
                drugIn.setInType((String) drugInData.get("inType"));
            }

            if (drugInData.get("remark") != null) {
                drugIn.setRemark((String) drugInData.get("remark"));
            }
            
            drugIn.setCreateUserId(currentUserId);
            
            boolean success = drugInService.saveDrugIn(drugIn);
            result.put("code", success ? 200 : 500);
            result.put("msg", success ? "保存成功" : "保存失败");
            result.put("data", null);
        } catch (IllegalArgumentException e) {
            result.put("code", 400);
            result.put("msg", "参数错误：" + e.getMessage());
            result.put("data", null);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("msg", "保存失败：" + e.getMessage());
            result.put("data", null);
        }
        return result;
    }

    /**
     * 向现有批次追加补货（沿用批次号、生产日期、效期）
     */
    @PostMapping("/replenish")
    public Map<String, Object> replenish(
            @RequestAttribute("currentUserId") Long currentUserId,
            @RequestBody Map<String, Object> data) {
        Map<String, Object> result = new HashMap<>();
        try {
            if (data.get("stockId") == null) {
                result.put("code", 400);
                result.put("msg", "库存批次不能为空");
                result.put("data", null);
                return result;
            }
            if (data.get("quantity") == null) {
                result.put("code", 400);
                result.put("msg", "入库数量不能为空");
                result.put("data", null);
                return result;
            }

            Long stockId = Long.valueOf(data.get("stockId").toString());
            Integer quantity = Integer.valueOf(data.get("quantity").toString());
            java.math.BigDecimal purchasePrice = data.get("purchasePrice") != null
                    ? new java.math.BigDecimal(data.get("purchasePrice").toString())
                    : null;

            boolean success = drugInService.replenishExistingBatch(stockId, quantity, purchasePrice, currentUserId);
            result.put("code", success ? 200 : 500);
            result.put("msg", success ? "补货成功" : "补货失败");
            result.put("data", null);
        } catch (IllegalArgumentException e) {
            result.put("code", 400);
            result.put("msg", e.getMessage());
            result.put("data", null);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("msg", "补货失败：" + e.getMessage());
            result.put("data", null);
        }
        return result;
    }

    /**
     * 删除入库单
     */
    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            DrugIn drugIn = drugInService.getById(id);
            if (drugIn == null) {
                result.put("code", 404);
                result.put("msg", "入库单不存在");
                result.put("data", null);
                return result;
            }
            boolean success = drugInService.removeById(id);
            result.put("code", success ? 200 : 500);
            result.put("msg", success ? "删除成功" : "删除失败");
            result.put("data", null);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("msg", "删除失败：" + e.getMessage());
            result.put("data", null);
        }
        return result;
    }
}
