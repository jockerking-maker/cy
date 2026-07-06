package com.hospital.drugmanagement.controller;

import com.hospital.drugmanagement.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 首页仪表盘接口。
 * <p>
 * 提供首页统计卡片、药品分类饼图、库存预警柱状图等数据，需登录后访问。
 */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    /** 首页核心指标：药品总数、预警数、待审采购、库存价值、临期药品。 */
    @GetMapping("/stats")
    public Map<String, Object> getDashboardStats() {
        Map<String, Object> result = new HashMap<>();
        try {
            Map<String, Object> stats = dashboardService.getStats();
            result.put("code", 200);
            result.put("msg", "success");
            result.put("data", stats);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("msg", "获取统计数据失败：" + e.getMessage());
            result.put("data", null);
        }
        return result;
    }

    /** 按药品类型统计数量，供首页饼图使用。 */
    @GetMapping("/drugTypeStats")
    public Map<String, Object> getDrugTypeStats() {
        Map<String, Object> result = new HashMap<>();
        try {
            Map<String, Object> stats = dashboardService.getDrugTypeStats();
            result.put("code", 200);
            result.put("msg", "success");
            result.put("data", stats);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("msg", "获取药品分类统计失败：" + e.getMessage());
            result.put("data", null);
        }
        return result;
    }

    /** 库存预警分类统计：不足、过剩、近效期、已过期。 */
    @GetMapping("/stockWarningStats")
    public Map<String, Object> getStockWarningStats() {
        Map<String, Object> result = new HashMap<>();
        try {
            Map<String, Object> stats = dashboardService.getStockWarningStats();
            result.put("code", 200);
            result.put("msg", "success");
            result.put("data", stats);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("msg", "获取库存预警统计失败：" + e.getMessage());
            result.put("data", null);
        }
        return result;
    }
}
