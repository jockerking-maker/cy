package com.hospital.drugmanagement.controller;

import com.hospital.drugmanagement.common.anno.RequireRole;
import com.hospital.drugmanagement.entity.SupplierInfo;
import com.hospital.drugmanagement.service.ISupplierInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 供应商信息管理接口。
 * <p>
 * 供应商 CRUD 及名称/编码/营业执照唯一性校验；ADMIN/PURCHASER/AUDITOR 可查询，增删改需 ADMIN 或 PURCHASER。
 */
@RestController
@RequestMapping("/api/supplier")
@CrossOrigin(origins = "*")
@RequireRole({"ADMIN", "PURCHASER", "AUDITOR"})
public class SupplierInfoController {

    @Autowired
    private ISupplierInfoService supplierInfoService;

    /** 查询供应商列表，支持名称、联系人、状态筛选。 */
    @GetMapping("/list")
    public Map<String, Object> list(
        @RequestParam(required = false) String supplierName,
        @RequestParam(required = false) String contactPerson,
        @RequestParam(required = false) Integer status
    ) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 构建查询条件
            com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SupplierInfo> queryWrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
            if (supplierName != null && !supplierName.isEmpty()) {
                queryWrapper.like(SupplierInfo::getSupplierName, supplierName);
            }
            if (contactPerson != null && !contactPerson.isEmpty()) {
                queryWrapper.like(SupplierInfo::getContactPerson, contactPerson);
            }
            if (status != null) {
                queryWrapper.eq(SupplierInfo::getStatus, status);
            }
            
            queryWrapper.orderByAsc(SupplierInfo::getSupplierCode);
            List<SupplierInfo> suppliers = supplierInfoService.list(queryWrapper);
            result.put("code", 200);
            result.put("msg", "success");
            result.put("data", suppliers);
            result.put("total", suppliers.size());
        } catch (Exception e) {
            result.put("code", 500);
            result.put("msg", "查询失败：" + e.getMessage());
            result.put("data", null);
            result.put("total", 0);
        }
        return result;
    }

    /** 根据 ID 查询供应商详情。 */
    @GetMapping("/{id}")
    public Map<String, Object> getById(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            SupplierInfo supplier = supplierInfoService.getById(id);
            if (supplier == null) {
                result.put("code", 404);
                result.put("msg", "供应商不存在");
                result.put("data", null);
            } else {
                result.put("code", 200);
                result.put("msg", "success");
                result.put("data", supplier);
            }
        } catch (Exception e) {
            result.put("code", 500);
            result.put("msg", "查询失败：" + e.getMessage());
            result.put("data", null);
        }
        return result;
    }

    /** 新增供应商，校验名称/编码/营业执照唯一性。 */
    @PostMapping
    @RequireRole({"ADMIN", "PURCHASER"})
    public Map<String, Object> save(@RequestBody SupplierInfo supplierInfo) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 检查供应商名称是否已存在
            com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SupplierInfo> nameWrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
            nameWrapper.eq(SupplierInfo::getSupplierName, supplierInfo.getSupplierName());
            if (supplierInfoService.count(nameWrapper) > 0) {
                result.put("code", 400);
                result.put("msg", "供应商名称已存在");
                result.put("data", null);
                return result;
            }
            
            // 检查供应商编码是否已存在
            com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SupplierInfo> codeWrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
            codeWrapper.eq(SupplierInfo::getSupplierCode, supplierInfo.getSupplierCode());
            if (supplierInfoService.count(codeWrapper) > 0) {
                result.put("code", 400);
                result.put("msg", "供应商编码已存在");
                result.put("data", null);
                return result;
            }
            
            // 检查营业执照号是否已存在
            com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SupplierInfo> licenseWrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
            licenseWrapper.eq(SupplierInfo::getLicenseNo, supplierInfo.getLicenseNo());
            if (supplierInfoService.count(licenseWrapper) > 0) {
                result.put("code", 400);
                result.put("msg", "营业执照号已存在");
                result.put("data", null);
                return result;
            }
            
            boolean success = supplierInfoService.save(supplierInfo);
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

    /** 修改供应商信息。 */
    @PutMapping
    @RequireRole({"ADMIN", "PURCHASER"})
    public Map<String, Object> update(@RequestBody SupplierInfo supplierInfo) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 检查供应商名称是否已存在（排除当前供应商）
            com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SupplierInfo> nameWrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
            nameWrapper.eq(SupplierInfo::getSupplierName, supplierInfo.getSupplierName());
            nameWrapper.ne(SupplierInfo::getSupplierId, supplierInfo.getSupplierId());
            if (supplierInfoService.count(nameWrapper) > 0) {
                result.put("code", 400);
                result.put("msg", "供应商名称已存在");
                result.put("data", null);
                return result;
            }
            
            // 检查供应商编码是否已存在（排除当前供应商）
            com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SupplierInfo> codeWrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
            codeWrapper.eq(SupplierInfo::getSupplierCode, supplierInfo.getSupplierCode());
            codeWrapper.ne(SupplierInfo::getSupplierId, supplierInfo.getSupplierId());
            if (supplierInfoService.count(codeWrapper) > 0) {
                result.put("code", 400);
                result.put("msg", "供应商编码已存在");
                result.put("data", null);
                return result;
            }
            
            // 检查营业执照号是否已存在（排除当前供应商）
            com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SupplierInfo> licenseWrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
            licenseWrapper.eq(SupplierInfo::getLicenseNo, supplierInfo.getLicenseNo());
            licenseWrapper.ne(SupplierInfo::getSupplierId, supplierInfo.getSupplierId());
            if (supplierInfoService.count(licenseWrapper) > 0) {
                result.put("code", 400);
                result.put("msg", "营业执照号已存在");
                result.put("data", null);
                return result;
            }
            
            boolean success = supplierInfoService.updateById(supplierInfo);
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

    /** 删除供应商。 */
    @DeleteMapping("/{id}")
    @RequireRole({"ADMIN", "PURCHASER"})
    public Map<String, Object> delete(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            SupplierInfo supplier = supplierInfoService.getById(id);
            if (supplier == null) {
                result.put("code", 404);
                result.put("msg", "供应商不存在");
                result.put("data", null);
                return result;
            }
            boolean success = supplierInfoService.removeById(id);
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