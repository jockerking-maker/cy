package com.hospital.drugmanagement.controller;

import com.hospital.drugmanagement.common.anno.RequireRole;
import com.hospital.drugmanagement.service.StockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 库存管理接口。
 * <p>
 * 按药品+仓库汇总查询库存、预警库存列表、创建盘点单入口；ADMIN/WAREHOUSE 可访问。
 */
@RestController
@RequestMapping("/api/stock")
@CrossOrigin(origins = "*")
@RequireRole({"ADMIN", "WAREHOUSE"})
public class StockController {

    @Autowired
    private StockService stockService;

    /**
     * 获取库存汇总列表（按药品+仓库分组，含批次明细）。
     */
    @GetMapping("/list")
    public Map<String, Object> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String drugName,
            @RequestParam(required = false) String drugCode,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false, defaultValue = "false") boolean warning,
            @RequestParam(required = false, defaultValue = "drugCode") String sortField,
            @RequestParam(required = false, defaultValue = "asc") String sortOrder
    ) {
        Map<String, Object> result = new HashMap<>();
        try {
            Map<String, Object> stockData = stockService.getGroupedStockList(page, size, drugName, drugCode, warehouseId, warning, sortField, sortOrder);
            result.put("code", 200);
            result.put("msg", "success");
            result.put("data", stockData.get("data"));
            result.put("total", stockData.get("total"));
        } catch (Exception e) {
            result.put("code", 500);
            result.put("msg", "查询失败：" + e.getMessage());
            result.put("data", null);
            result.put("total", 0);
        }
        return result;
    }

    /**
     * 获取库存详情
     */
    @GetMapping("/{id}")
    public Map<String, Object> getById(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 这里可以根据实际需求实现获取库存详情的逻辑
            result.put("code", 200);
            result.put("msg", "success");
            result.put("data", null);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("msg", "查询失败：" + e.getMessage());
            result.put("data", null);
        }
        return result;
    }

    /**
     * 库存预警列表
     */
    @GetMapping("/warning/list")
    public Map<String, Object> warningList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String drugName,
            @RequestParam(required = false) String drugCode,
            @RequestParam(required = false) Long warehouseId
    ) {
        Map<String, Object> result = new HashMap<>();
        try {
            Map<String, Object> stockData = stockService.getStockList(page, size, drugName, drugCode, warehouseId, true);
            result.put("code", 200);
            result.put("msg", "success");
            result.put("data", stockData.get("data"));
            result.put("total", stockData.get("total"));
        } catch (Exception e) {
            result.put("code", 500);
            result.put("msg", "查询失败：" + e.getMessage());
            result.put("data", null);
            result.put("total", 0);
        }
        return result;
    }

    /** 创建盘点单（快捷入口，明细生成见 StockCheckController）。 */
    @PostMapping("/check")
    public Map<String, Object> checkStock(@RequestBody Map<String, Object> data) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long warehouseId = data.get("warehouseId") != null ? Long.valueOf(data.get("warehouseId").toString()) : null;
            String range = data.get("range") != null ? data.get("range").toString() : "all";

            boolean success = stockService.checkStock(warehouseId, range);
            result.put("code", success ? 200 : 500);
            result.put("msg", success ? "创建盘点单成功" : "创建盘点单失败");
            result.put("data", null);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("msg", "创建盘点单失败：" + e.getMessage());
            result.put("data", null);
        }
        return result;
    }
}
