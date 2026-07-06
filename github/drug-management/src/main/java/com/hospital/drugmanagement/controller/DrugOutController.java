package com.hospital.drugmanagement.controller;

import com.hospital.drugmanagement.common.anno.RequireRole;
import com.hospital.drugmanagement.entity.DrugOut;
import com.hospital.drugmanagement.service.IDrugOutService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 药品出库接口：领用、销售、报损等出库类型，按批次扣减库存。
 */
@RestController
@RequestMapping("/api/drug/out")
@CrossOrigin(origins = "*")
@RequireRole({"ADMIN", "WAREHOUSE"})
public class DrugOutController {

    @Autowired
    private IDrugOutService drugOutService;

    /**
     * 获取出库单列表
     */
    @GetMapping("/list")
    public Map<String, Object> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String outNo,
            @RequestParam(required = false) String drugName,
            @RequestParam(required = false) String outType
    ) {
        Map<String, Object> result = new HashMap<>();
        try {
            Map<String, Object> data = drugOutService.getDrugOutList(page, size, outNo, drugName, outType);
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
     * 获取出库单详情
     */
    @GetMapping("/{id}")
    public Map<String, Object> getById(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            DrugOut drugOut = drugOutService.getById(id);
            if (drugOut == null) {
                result.put("code", 404);
                result.put("msg", "出库单不存在");
                result.put("data", null);
            } else {
                result.put("code", 200);
                result.put("msg", "success");
                result.put("data", drugOut);
            }
        } catch (Exception e) {
            result.put("code", 500);
            result.put("msg", "查询失败：" + e.getMessage());
            result.put("data", null);
        }
        return result;
    }

    /**
     * 新增出库单
     */
    @PostMapping
    public Map<String, Object> save(
            @RequestAttribute("currentUserId") Long currentUserId,
            @RequestBody Map<String, Object> drugOutData) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 创建出库单对象
            DrugOut drugOut = new DrugOut();
            drugOut.setDrugId(Long.valueOf(drugOutData.get("drugId").toString()));
            drugOut.setWarehouseId(Long.valueOf(drugOutData.get("warehouseId").toString()));
            drugOut.setOutNum(Integer.valueOf(drugOutData.get("outNum").toString()));
            drugOut.setOutType((String) drugOutData.get("outType"));

            Object batchNoObj = drugOutData.get("batchNo");
            if (batchNoObj == null || batchNoObj.toString().trim().isEmpty()) {
                result.put("code", 400);
                result.put("msg", "参数错误：批次号不能为空");
                result.put("data", null);
                return result;
            }
            drugOut.setBatchNo(batchNoObj.toString().trim());
            
            // 设置出库单价
            Object salePriceObj = drugOutData.get("salePrice");
            if (salePriceObj != null) {
                if (salePriceObj instanceof Number) {
                    drugOut.setSalePrice(new java.math.BigDecimal(((Number) salePriceObj).doubleValue()));
                } else {
                    drugOut.setSalePrice(new java.math.BigDecimal(salePriceObj.toString()));
                }
            }
            
            // 设置备注（可选）
            if (drugOutData.get("remark") != null) {
                drugOut.setRemark((String) drugOutData.get("remark"));
            }
            
            drugOut.setCreateUserId(currentUserId);
            
            boolean success = drugOutService.saveDrugOut(drugOut);
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
     * 删除出库单
     */
    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            DrugOut drugOut = drugOutService.getById(id);
            if (drugOut == null) {
                result.put("code", 404);
                result.put("msg", "出库单不存在");
                result.put("data", null);
                return result;
            }
            boolean success = drugOutService.removeById(id);
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
