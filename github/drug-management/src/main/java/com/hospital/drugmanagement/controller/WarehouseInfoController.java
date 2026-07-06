package com.hospital.drugmanagement.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hospital.drugmanagement.common.anno.RequireRole;
import com.hospital.drugmanagement.entity.WarehouseInfo;
import com.hospital.drugmanagement.service.IWarehouseInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 仓库信息管理接口。
 * <p>
 * 仓库 CRUD、编码自动生成；删除前校验是否仍有库存/锁定/进行中盘点。
 */
@RestController
@RequestMapping("/api/warehouse")
@CrossOrigin(origins = "*")
@RequireRole({"ADMIN", "WAREHOUSE"})
public class WarehouseInfoController {

    @Autowired
    private IWarehouseInfoService warehouseInfoService;

    /**
     * 获取仓库列表
     */
    @GetMapping("/list")
    public Map<String, Object> list(
            @RequestParam(required = false) String warehouseName,
            @RequestParam(required = false) Integer status
    ) {
        Map<String, Object> result = new HashMap<>();
        try {
            LambdaQueryWrapper<WarehouseInfo> queryWrapper = new LambdaQueryWrapper<>();
            if (warehouseName != null && !warehouseName.isEmpty()) {
                queryWrapper.like(WarehouseInfo::getWarehouseName, warehouseName);
            }
            if (status != null) {
                queryWrapper.eq(WarehouseInfo::getStatus, status);
            }

            queryWrapper.orderByAsc(WarehouseInfo::getWarehouseCode);
            List<WarehouseInfo> warehouses = warehouseInfoService.list(queryWrapper);
            result.put("code", 200);
            result.put("msg", "success");
            result.put("data", warehouses);
            result.put("total", warehouses.size());
        } catch (Exception e) {
            result.put("code", 500);
            result.put("msg", "查询失败：" + e.getMessage());
            result.put("data", null);
            result.put("total", 0);
        }
        return result;
    }

    /** 生成下一个仓库编码（WH001）。 */
    @GetMapping("/next-code")
    public Map<String, Object> nextCode() {
        Map<String, Object> result = new HashMap<>();
        try {
            result.put("code", 200);
            result.put("msg", "success");
            result.put("data", warehouseInfoService.generateNextWarehouseCode());
        } catch (Exception e) {
            result.put("code", 500);
            result.put("msg", "生成仓库编码失败：" + e.getMessage());
            result.put("data", null);
        }
        return result;
    }

    /**
     * 获取仓库详情
     */
    @GetMapping("/{id:\\d+}")
    public Map<String, Object> getById(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            WarehouseInfo warehouse = warehouseInfoService.getById(id);
            if (warehouse == null) {
                result.put("code", 404);
                result.put("msg", "仓库不存在");
                result.put("data", null);
            } else {
                result.put("code", 200);
                result.put("msg", "success");
                result.put("data", warehouse);
            }
        } catch (Exception e) {
            result.put("code", 500);
            result.put("msg", "查询失败：" + e.getMessage());
            result.put("data", null);
        }
        return result;
    }

    /**
     * 新增仓库
     */
    @PostMapping
    public Map<String, Object> save(@RequestBody WarehouseInfo warehouseInfo) {
        Map<String, Object> result = new HashMap<>();
        try {
            if (warehouseInfo.getWarehouseCode() == null || warehouseInfo.getWarehouseCode().trim().isEmpty()) {
                result.put("code", 400);
                result.put("msg", "仓库编码不能为空");
                result.put("data", null);
                return result;
            }
            String warehouseCode = warehouseInfo.getWarehouseCode().trim();
            warehouseInfo.setWarehouseCode(warehouseCode);

            LambdaQueryWrapper<WarehouseInfo> duplicateQuery = new LambdaQueryWrapper<>();
            duplicateQuery.eq(WarehouseInfo::getWarehouseCode, warehouseCode);
            if (warehouseInfoService.count(duplicateQuery) > 0) {
                String suggestedCode = warehouseInfoService.generateNextWarehouseCode();
                result.put("code", 400);
                result.put("msg", "仓库编码 " + warehouseCode + " 已存在，请改用其他编码（建议使用 " + suggestedCode + "）");
                result.put("data", suggestedCode);
                return result;
            }

            boolean success = warehouseInfoService.save(warehouseInfo);
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

    /**
     * 修改仓库
     */
    @PutMapping
    public Map<String, Object> update(@RequestBody WarehouseInfo warehouseInfo) {
        Map<String, Object> result = new HashMap<>();
        try {
            boolean success = warehouseInfoService.updateById(warehouseInfo);
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

    /**
     * 删除仓库
     */
    @DeleteMapping("/{id:\\d+}")
    public Map<String, Object> delete(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            WarehouseInfo warehouse = warehouseInfoService.getById(id);
            if (warehouse == null) {
                result.put("code", 404);
                result.put("msg", "仓库不存在");
                result.put("data", null);
                return result;
            }
            String blockReason = warehouseInfoService.getDeleteBlockReason(id);
            if (blockReason != null) {
                result.put("code", 400);
                result.put("msg", blockReason);
                result.put("data", null);
                return result;
            }
            boolean success = warehouseInfoService.removeById(id);
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
