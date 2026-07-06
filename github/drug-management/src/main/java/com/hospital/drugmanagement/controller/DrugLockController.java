package com.hospital.drugmanagement.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hospital.drugmanagement.common.anno.RequireRole;
import com.hospital.drugmanagement.entity.DrugLock;
import com.hospital.drugmanagement.service.DrugLockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 药品库存锁定接口：锁定/解锁指定批次库存，防止出库超卖。
 */
@RestController
@RequestMapping("/api/drug-lock")
@CrossOrigin(origins = "*")
@RequireRole({"ADMIN", "WAREHOUSE"})
public class DrugLockController {

    @Autowired
    private DrugLockService drugLockService;

    /**
     * 分页查询锁定记录
     */
    @GetMapping("/list")
    public Map<String, Object> list(
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(required = false) Long drugId,
        @RequestParam(required = false) Long warehouseId,
        @RequestParam(required = false) Integer status
    ) {
        Map<String, Object> result = new HashMap<>();
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("page", page);
            params.put("size", size);
            if (drugId != null) params.put("drugId", drugId);
            if (warehouseId != null) params.put("warehouseId", warehouseId);
            if (status != null) params.put("status", status);
            
            Page<DrugLock> drugLockPage = drugLockService.pageList(params);
            
            result.put("code", 200);
            result.put("msg", "success");
            result.put("data", drugLockPage.getRecords());
            result.put("total", drugLockPage.getTotal());
        } catch (Exception e) {
            result.put("code", 500);
            result.put("msg", "查询失败：" + e.getMessage());
            result.put("data", null);
            result.put("total", 0);
        }
        return result;
    }

    /**
     * 锁定药品
     */
    @PostMapping("/lock")
    public Map<String, Object> lock(
            @RequestAttribute("currentUserId") Long currentUserId,
            @RequestBody Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();
        try {
            DrugLock drugLock = new DrugLock();
            drugLock.setDrugId(Long.valueOf(params.get("drugId").toString()));
            drugLock.setWarehouseId(Long.valueOf(params.get("warehouseId").toString()));
            
            if (params.containsKey("batchNo") && params.get("batchNo") != null) {
                drugLock.setBatchNo(params.get("batchNo").toString());
            }
            
            // 优先使用前端传递的差值，如果没有则自己计算
            Integer deltaLockNum = null;
            if (params.containsKey("deltaLockNum") && params.get("deltaLockNum") != null) {
                // 使用前端传递的差值
                deltaLockNum = Integer.valueOf(params.get("deltaLockNum").toString());
            } else {
                // 自己计算差值
                Integer targetLockNum = Integer.valueOf(params.get("lockNum").toString());
                Integer currentLockNum = 0;
                if (params.containsKey("currentLockNum") && params.get("currentLockNum") != null) {
                    currentLockNum = Integer.valueOf(params.get("currentLockNum").toString());
                }
                deltaLockNum = targetLockNum - currentLockNum;
            }
            
            // 设置实际要增加/减少的锁定数量
            drugLock.setLockNum(deltaLockNum);
            drugLock.setLockReason(params.get("lockReason") != null ? params.get("lockReason").toString() : "");
            drugLock.setLockUserId(currentUserId);
            
            System.out.println("锁定参数：targetLockNum=" + params.get("lockNum") + 
                             ", currentLockNum=" + params.get("currentLockNum") + 
                             ", deltaLockNum=" + deltaLockNum);
            
            boolean success = drugLockService.lockDrug(drugLock);
            result.put("code", success ? 200 : 500);
            result.put("msg", success ? "锁定成功" : "锁定失败");
            result.put("data", null);
        } catch (IllegalArgumentException e) {
            result.put("code", 400);
            result.put("msg", "参数错误：" + e.getMessage());
            result.put("data", null);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("msg", "锁定失败：" + e.getMessage());
            result.put("data", null);
        }
        return result;
    }

    /**
     * 解锁药品
     */
    @PostMapping("/unlock/{lockId}")
    public Map<String, Object> unlock(
        @PathVariable Long lockId,
        @RequestAttribute("currentUserId") Long currentUserId
    ) {
        Map<String, Object> result = new HashMap<>();
        try {
            boolean success = drugLockService.unlockDrug(lockId, currentUserId);
            result.put("code", success ? 200 : 500);
            result.put("msg", success ? "解锁成功" : "解锁失败");
            result.put("data", null);
        } catch (IllegalArgumentException e) {
            result.put("code", 400);
            result.put("msg", "参数错误：" + e.getMessage());
            result.put("data", null);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("msg", "解锁失败：" + e.getMessage());
            result.put("data", null);
        }
        return result;
    }
}
