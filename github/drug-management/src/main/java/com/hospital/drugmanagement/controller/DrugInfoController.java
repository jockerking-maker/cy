package com.hospital.drugmanagement.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hospital.drugmanagement.common.anno.RequireRole;
import com.hospital.drugmanagement.entity.DrugInfo;
import com.hospital.drugmanagement.entity.DrugStock;
import com.hospital.drugmanagement.entity.StockWarning;
import com.hospital.drugmanagement.entity.SupplierInfo;
import com.hospital.drugmanagement.service.DrugInfoService;
import com.hospital.drugmanagement.service.ISupplierInfoService;
import com.hospital.drugmanagement.service.StockService;
import com.hospital.drugmanagement.service.StockWarningService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 药品信息管理接口。
 * <p>
 * 药品 CRUD、编码生成、预警阈值设置；ADMIN/PURCHASER/WAREHOUSE 可访问，增删改需 ADMIN 或 PURCHASER。
 */
@RestController
@RequestMapping("/api/drug")
@CrossOrigin(origins = "*")
@RequireRole({"ADMIN", "PURCHASER", "WAREHOUSE"})
public class DrugInfoController {

    @Autowired
    private DrugInfoService drugInfoService;

    @Autowired
    private ISupplierInfoService supplierInfoService;
    
    @Autowired
    private StockService stockService;
    
    @Autowired
    private StockWarningService stockWarningService;

    /** 分页查询药品列表，支持名称、编码、类型筛选。 */
    @GetMapping("/list")
    public Map<String, Object> list(
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(required = false) String drugName,
        @RequestParam(required = false) String drugCode,
        @RequestParam(required = false) String drugType
    ) {
        Map<String, Object> result = new HashMap<>();
        try {
            if (page < 1) page = 1;
            if (size < 1) size = 10;
            LambdaQueryWrapper<DrugInfo> queryWrapper = new LambdaQueryWrapper<>();
            if (drugName != null && !drugName.isEmpty()) {
                queryWrapper.like(DrugInfo::getDrugName, drugName);
            }
            if (drugCode != null && !drugCode.isEmpty()) {
                queryWrapper.like(DrugInfo::getDrugCode, drugCode);
            }
            if (drugType != null && !drugType.isEmpty()) {
                queryWrapper.eq(DrugInfo::getDrugType, drugType);
            }
            queryWrapper.orderByAsc(DrugInfo::getDrugCode);

            // 分页查询
            Page<DrugInfo> drugPage = drugInfoService.page(new Page<>(page, size), queryWrapper);
            
            result.put("code", 200);
            result.put("msg", "success");
            result.put("data", drugPage.getRecords());
            result.put("total", drugPage.getTotal());
        } catch (Exception e) {
            result.put("code", 500);
            result.put("msg", "查询失败：" + e.getMessage());
            result.put("data", null);
            result.put("total", 0);
        }
        return result;
    }

    /** 生成下一组药品编码（YP001）与批准文号。 */
    @GetMapping("/next-code")
    @RequireRole({"ADMIN", "PURCHASER"})
    public Map<String, Object> getNextCode(@RequestParam(required = false) String drugType) {
        Map<String, Object> result = new HashMap<>();
        try {
            result.put("code", 200);
            result.put("msg", "success");
            result.put("data", drugInfoService.generateNextCodes(drugType));
        } catch (Exception e) {
            result.put("code", 500);
            result.put("msg", "生成编码失败：" + e.getMessage());
            result.put("data", null);
        }
        return result;
    }

    /** 根据 ID 查询药品详情。 */
    @GetMapping("/{id}")
    public Map<String, Object> getById(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            DrugInfo drug = drugInfoService.getById(id);
            if (drug == null) {
                result.put("code", 404);
                result.put("msg", "药品不存在");
                result.put("data", null);
            } else {
                result.put("code", 200);
                result.put("msg", "success");
                result.put("data", drug);
            }
        } catch (Exception e) {
            result.put("code", 500);
            result.put("msg", "查询失败：" + e.getMessage());
            result.put("data", null);
        }
        return result;
    }

    /** 新增药品，校验编码/名称唯一性及供应商状态。 */
    @PostMapping
    @RequireRole({"ADMIN", "PURCHASER"})
    public Map<String, Object> save(@RequestBody DrugInfo drugInfo) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 确保 drugId 为 null，让数据库自动生成自增 ID
            drugInfo.setDrugId(null);
            
            // 检查药品编码是否已存在
            com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<DrugInfo> codeWrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
            codeWrapper.eq(DrugInfo::getDrugCode, drugInfo.getDrugCode());
            if (drugInfoService.exists(codeWrapper)) {
                result.put("code", 400);
                result.put("msg", "药品编码已存在");
                result.put("data", null);
                return result;
            }
            
            // 检查药品名称是否已存在
            com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<DrugInfo> nameWrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
            nameWrapper.eq(DrugInfo::getDrugName, drugInfo.getDrugName());
            if (drugInfoService.exists(nameWrapper)) {
                result.put("code", 400);
                result.put("msg", "药品名称已存在");
                result.put("data", null);
                return result;
            }

            Map<String, Object> supplierError = validateEnabledSupplier(drugInfo.getSupplierId());
            if (supplierError != null) {
                return supplierError;
            }
            
            boolean success = drugInfoService.save(drugInfo);
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

    /** 修改药品信息，校验编码/名称唯一性。 */
    @PutMapping
    @RequireRole({"ADMIN", "PURCHASER"})
    public Map<String, Object> update(@RequestBody DrugInfo drugInfo) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 检查药品编码是否已存在（排除当前药品）
            com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<DrugInfo> codeWrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
            codeWrapper.eq(DrugInfo::getDrugCode, drugInfo.getDrugCode());
            codeWrapper.ne(DrugInfo::getDrugId, drugInfo.getDrugId());
            if (drugInfoService.exists(codeWrapper)) {
                result.put("code", 400);
                result.put("msg", "药品编码已存在");
                result.put("data", null);
                return result;
            }
            
            // 检查药品名称是否已存在（排除当前药品）
            com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<DrugInfo> nameWrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
            nameWrapper.eq(DrugInfo::getDrugName, drugInfo.getDrugName());
            nameWrapper.ne(DrugInfo::getDrugId, drugInfo.getDrugId());
            if (drugInfoService.exists(nameWrapper)) {
                result.put("code", 400);
                result.put("msg", "药品名称已存在");
                result.put("data", null);
                return result;
            }

            DrugInfo existingDrug = drugInfoService.getById(drugInfo.getDrugId());
            if (existingDrug == null) {
                result.put("code", 404);
                result.put("msg", "药品不存在");
                result.put("data", null);
                return result;
            }
            if (drugInfo.getSupplierId() != null
                    && !drugInfo.getSupplierId().equals(existingDrug.getSupplierId())) {
                Map<String, Object> supplierError = validateEnabledSupplier(drugInfo.getSupplierId());
                if (supplierError != null) {
                    return supplierError;
                }
            }
            
            boolean success = drugInfoService.updateById(drugInfo);
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

    /** 删除药品（需无关联库存等业务约束时方可删除）。 */
    @DeleteMapping("/{id}")
    @RequireRole({"ADMIN", "PURCHASER"})
    public Map<String, Object> delete(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            DrugInfo drug = drugInfoService.getById(id);
            if (drug == null) {
                result.put("code", 404);
                result.put("msg", "药品不存在");
                result.put("data", null);
                return result;
            }
            boolean success = drugInfoService.removeById(id);
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

    /**
     * 更新药品预警阈值；若新阈值立即触发低/高库存，自动创建预警记录。
     */
    @PutMapping("/warning")
    @RequireRole({"ADMIN", "PURCHASER", "WAREHOUSE"})
    public Map<String, Object> updateWarning(@RequestBody Map<String, Object> data) {
        Map<String, Object> result = new HashMap<>();
        try {
            if (data.get("drugId") == null) {
                result.put("code", 400);
                result.put("msg", "药品 ID 不能为空");
                result.put("data", null);
                return result;
            }
            
            Long drugId = Long.valueOf(data.get("drugId").toString());
            Integer warningNum = data.get("warningNum") != null ? Integer.valueOf(data.get("warningNum").toString()) : null;
            Integer maxWarningNum = data.get("maxWarningNum") != null ? Integer.valueOf(data.get("maxWarningNum").toString()) : null;
            
            // 验证：最高预警值必须高于最低预警值
            if (warningNum != null && maxWarningNum != null && maxWarningNum <= warningNum) {
                result.put("code", 400);
                result.put("msg", "最高预警值必须高于最低预警值");
                result.put("data", null);
                return result;
            }
            
            DrugInfo drugInfo = drugInfoService.getById(drugId);
            if (drugInfo == null) {
                result.put("code", 404);
                result.put("msg", "药品不存在");
                result.put("data", null);
                return result;
            }
            
            if (warningNum != null) {
                drugInfo.setWarningNum(warningNum);
            }
            if (maxWarningNum != null) {
                drugInfo.setMaxWarningNum(maxWarningNum);
            }
            
            // 检查设置的预警值是否会立即触发预警
            List<DrugStock> stockList = stockService.list(new LambdaQueryWrapper<DrugStock>().eq(DrugStock::getDrugId, drugId));
            for (DrugStock stock : stockList) {
                int availableNum = stock.getStockNum() - (stock.getLockNum() != null ? stock.getLockNum() : 0);
                
                // 如果最低预警值高于可用库存，触发预警
                if (warningNum != null && availableNum < warningNum) {
                    // 检查是否已存在未处理的预警
                    Long existsCount = stockWarningService.count(new LambdaQueryWrapper<StockWarning>()
                        .eq(StockWarning::getDrugId, drugId)
                        .eq(StockWarning::getWarehouseId, stock.getWarehouseId())
                        .eq(StockWarning::getHandleStatus, 0));
                    
                    if (existsCount == 0) {
                        // 创建新的预警记录
                        StockWarning warning = new StockWarning();
                        warning.setWarningNo("WARN" + System.currentTimeMillis()); // 生成预警单号
                        warning.setDrugId(drugId);
                        warning.setWarehouseId(stock.getWarehouseId());
                        warning.setWarningType(0); // 0-最低预警
                        warning.setStockNum(availableNum);
                        warning.setMinWarningNum(warningNum);
                        warning.setWarningLevel(1); // 1-重要预警
                        warning.setHandleStatus(0); // 0-未处理
                        stockWarningService.save(warning);
                    }
                }
                
                // 如果最高预警值低于可用库存，触发预警
                if (maxWarningNum != null && availableNum > maxWarningNum) {
                    // 检查是否已存在未处理的预警
                    Long existsCount = stockWarningService.count(new LambdaQueryWrapper<StockWarning>()
                        .eq(StockWarning::getDrugId, drugId)
                        .eq(StockWarning::getWarehouseId, stock.getWarehouseId())
                        .eq(StockWarning::getHandleStatus, 0));
                    
                    if (existsCount == 0) {
                        // 创建新的预警记录
                        StockWarning warning = new StockWarning();
                        warning.setWarningNo("WARN" + System.currentTimeMillis()); // 生成预警单号
                        warning.setDrugId(drugId);
                        warning.setWarehouseId(stock.getWarehouseId());
                        warning.setWarningType(1); // 1-最高预警（库存积压）
                        warning.setStockNum(availableNum);
                        warning.setMaxWarningNum(maxWarningNum);
                        warning.setWarningLevel(1); // 1-重要预警
                        warning.setHandleStatus(0); // 0-未处理
                        stockWarningService.save(warning);
                    }
                }
            }
            
            boolean success = drugInfoService.updateById(drugInfo);
            result.put("code", success ? 200 : 500);
            result.put("msg", success ? "设置成功" : "设置失败");
            result.put("data", null);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("msg", "设置失败：" + e.getMessage());
            result.put("data", null);
        }
        return result;
    }

    /** 校验供应商是否存在且为启用状态。 */
    private Map<String, Object> validateEnabledSupplier(Long supplierId) {
        if (supplierId == null) {
            Map<String, Object> result = new HashMap<>();
            result.put("code", 400);
            result.put("msg", "请选择生产企业");
            result.put("data", null);
            return result;
        }

        SupplierInfo supplier = supplierInfoService.getById(supplierId);
        if (supplier == null) {
            Map<String, Object> result = new HashMap<>();
            result.put("code", 400);
            result.put("msg", "供应商不存在");
            result.put("data", null);
            return result;
        }
        if (supplier.getStatus() == null || supplier.getStatus() != 1) {
            Map<String, Object> result = new HashMap<>();
            result.put("code", 400);
            result.put("msg", "供应商已禁用，无法使用");
            result.put("data", null);
            return result;
        }
        return null;
    }
}
