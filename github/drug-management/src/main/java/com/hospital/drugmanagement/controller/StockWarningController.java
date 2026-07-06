package com.hospital.drugmanagement.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hospital.drugmanagement.common.anno.RequireRole;
import com.hospital.drugmanagement.entity.StockWarning;
import com.hospital.drugmanagement.service.StockWarningService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 库存预警接口。
 * <p>
 * 低库存、积压、临期、过期、滞销等预警的查询、处理、指派、批量操作及定时检查。
 */
@RestController
@RequestMapping("/api/stock-warning")
@CrossOrigin(origins = "*")
@RequireRole({"ADMIN", "WAREHOUSE"})
public class StockWarningController {

    @Autowired
    private StockWarningService stockWarningService;

    /** 分页查询预警列表，支持药品、仓库、处理状态、预警类型/级别筛选。 */
    @GetMapping("/list")
    public Map<String, Object> list(
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(required = false) Long drugId,
        @RequestParam(required = false) Long warehouseId,
        @RequestParam(required = false) Integer handleStatus,
        @RequestParam(required = false) Integer warningType,
        @RequestParam(required = false) Integer warningLevel
    ) {
        Map<String, Object> result = new HashMap<>();
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("page", page);
            params.put("size", size);
            if (drugId != null) params.put("drugId", drugId);
            if (warehouseId != null) params.put("warehouseId", warehouseId);
            if (handleStatus != null) params.put("handleStatus", handleStatus);
            if (warningType != null) params.put("warningType", warningType);
            if (warningLevel != null) params.put("warningLevel", warningLevel);

            Page<StockWarning> stockWarningPage = stockWarningService.pageList(params);

            result.put("code", 200);
            result.put("msg", "success");
            result.put("data", stockWarningPage.getRecords());
            result.put("total", stockWarningPage.getTotal());
        } catch (Exception e) {
            result.put("code", 500);
            result.put("msg", "查询失败：" + e.getMessage());
            result.put("data", null);
            result.put("total", 0);
        }
        return result;
    }

    /** 处理单条预警，填写处理备注。 */
    @PostMapping("/handle/{warningId}")
    public Map<String, Object> handle(
        @PathVariable Long warningId,
        @RequestAttribute("currentUserId") Long currentUserId,
        @RequestBody Map<String, Object> data
    ) {
        Map<String, Object> result = new HashMap<>();
        try {
            String handleRemark = data.get("handleRemark") != null ? data.get("handleRemark").toString() : null;
            boolean success = stockWarningService.handleWarning(warningId, currentUserId, handleRemark);
            result.put("code", success ? 200 : 500);
            result.put("msg", success ? "处理成功" : "处理失败");
            result.put("data", null);
        } catch (IllegalArgumentException e) {
            result.put("code", 400);
            result.put("msg", e.getMessage());
            result.put("data", null);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("msg", "处理失败：" + e.getMessage());
            result.put("data", null);
        }
        return result;
    }

    /** 删除已处理的预警记录。 */
    @PostMapping("/delete")
    public Map<String, Object> delete(@RequestBody Map<String, Object> data) {
        Map<String, Object> result = new HashMap<>();
        try {
            if (data.get("warningId") == null) {
                result.put("code", 400);
                result.put("msg", "预警 ID 不能为空");
                result.put("data", null);
                return result;
            }

            Long warningId = Long.valueOf(data.get("warningId").toString());
            StockWarning stockWarning = stockWarningService.getById(warningId);
            if (stockWarning == null) {
                result.put("code", 404);
                result.put("msg", "预警记录不存在");
                result.put("data", null);
                return result;
            }
            if (stockWarning.getHandleStatus() == null || stockWarning.getHandleStatus() != 1) {
                result.put("code", 400);
                result.put("msg", "仅可删除已处理的预警，请先处理后再删除");
                result.put("data", null);
                return result;
            }
            boolean success = stockWarningService.removeById(warningId);
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

    /** 批量删除已处理的预警。 */
    @PostMapping("/batch-delete")
    public Map<String, Object> batchDelete(@RequestBody Map<String, Object> data) {
        Map<String, Object> result = new HashMap<>();
        try {
            if (data.get("warningIds") == null) {
                result.put("code", 400);
                result.put("msg", "预警 ID 列表不能为空");
                result.put("data", null);
                return result;
            }

            List<Long> warningIds = ((List<?>) data.get("warningIds")).stream()
                    .map(id -> Long.valueOf(id.toString()))
                    .collect(Collectors.toList());
            Map<String, Object> summary = stockWarningService.batchDeleteHandledWarnings(warningIds);
            int deleted = summary.get("deleted") != null ? Integer.parseInt(summary.get("deleted").toString()) : 0;
            int skipped = summary.get("skipped") != null ? Integer.parseInt(summary.get("skipped").toString()) : 0;
            String msg = String.format("批量删除完成：成功 %d 条", deleted);
            if (skipped > 0) {
                msg += String.format("，跳过 %d 条（未处理或不存在）", skipped);
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

    /** 预警统计概览（未处理/已处理数量等）。 */
    @GetMapping("/stats")
    public Map<String, Object> stats() {
        Map<String, Object> result = new HashMap<>();
        try {
            Map<String, Object> stats = stockWarningService.getWarningStats();
            result.put("code", 200);
            result.put("msg", "success");
            result.put("data", stats);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("msg", "获取统计失败：" + e.getMessage());
            result.put("data", null);
        }
        return result;
    }

    /** 近 N 个月预警趋势数据。 */
    @GetMapping("/trend")
    public Map<String, Object> trend(@RequestParam(defaultValue = "6") int months) {
        Map<String, Object> result = new HashMap<>();
        try {
            Map<String, Object> trend = stockWarningService.getWarningTrend(months);
            result.put("code", 200);
            result.put("msg", "success");
            result.put("data", trend);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("msg", "获取趋势失败：" + e.getMessage());
            result.put("data", null);
        }
        return result;
    }

    /** 将预警指派给指定处理人。 */
    @PostMapping("/assign")
    public Map<String, Object> assign(@RequestBody Map<String, Object> data) {
        Map<String, Object> result = new HashMap<>();
        try {
            if (data.get("warningId") == null) {
                result.put("code", 400);
                result.put("msg", "预警 ID 不能为空");
                result.put("data", null);
                return result;
            }
            if (data.get("assignUserId") == null) {
                result.put("code", 400);
                result.put("msg", "指派人 ID 不能为空");
                result.put("data", null);
                return result;
            }

            Long warningId = Long.valueOf(data.get("warningId").toString());
            Long assignUserId = Long.valueOf(data.get("assignUserId").toString());
            boolean success = stockWarningService.assignWarning(warningId, assignUserId);
            result.put("code", success ? 200 : 500);
            result.put("msg", success ? "指派成功" : "指派失败");
            result.put("data", null);
        } catch (IllegalArgumentException e) {
            result.put("code", 400);
            result.put("msg", e.getMessage());
            result.put("data", null);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("msg", "指派失败：" + e.getMessage());
            result.put("data", null);
        }
        return result;
    }

    /** 批量处理预警。 */
    @PostMapping("/batch-handle")
    public Map<String, Object> batchHandle(
            @RequestAttribute("currentUserId") Long currentUserId,
            @RequestBody Map<String, Object> data) {
        Map<String, Object> result = new HashMap<>();
        try {
            if (data.get("warningIds") == null) {
                result.put("code", 400);
                result.put("msg", "预警 ID 列表不能为空");
                result.put("data", null);
                return result;
            }

            List<Long> warningIds = ((List<?>) data.get("warningIds")).stream()
                    .map(id -> Long.valueOf(id.toString()))
                    .collect(Collectors.toList());
            String handleRemark = data.get("handleRemark") != null ? data.get("handleRemark").toString() : null;
            Map<String, Object> summary = stockWarningService.batchHandleWarnings(warningIds, currentUserId, handleRemark);
            int processed = summary.get("processed") != null ? Integer.parseInt(summary.get("processed").toString()) : 0;
            int skipped = summary.get("skipped") != null ? Integer.parseInt(summary.get("skipped").toString()) : 0;
            int failed = summary.get("failed") != null ? Integer.parseInt(summary.get("failed").toString()) : 0;
            String msg = String.format("批量处理完成：成功 %d 条", processed);
            if (skipped > 0) {
                msg += String.format("，跳过 %d 条", skipped);
            }
            if (failed > 0) {
                msg += String.format("，失败 %d 条", failed);
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
            result.put("msg", "批量处理失败：" + e.getMessage());
            result.put("data", null);
        }
        return result;
    }

    /** 手动触发临期/过期预警扫描。 */
    @PostMapping("/check-near-expiry")
    public Map<String, Object> checkNearExpiry() {
        Map<String, Object> result = new HashMap<>();
        try {
            Map<String, Object> summary = stockWarningService.checkAndCreateNearExpiryWarning();
            result.put("code", 200);
            result.put("msg", String.format("临期预警检查完成，扫描 %s 条库存，新增 %s 条，跳过 %s 条",
                    summary.get("checked"), summary.get("created"), summary.get("skipped")));
            result.put("data", summary);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("msg", "临期预警检查失败：" + e.getMessage());
            result.put("data", null);
        }
        return result;
    }

    /** 手动触发滞销预警扫描（90 天无出入库活动）。 */
    @PostMapping("/check-slow-moving")
    public Map<String, Object> checkSlowMoving() {
        Map<String, Object> result = new HashMap<>();
        try {
            Map<String, Object> summary = stockWarningService.checkAndCreateSlowMovingWarning();
            result.put("code", 200);
            result.put("msg", String.format("滞销预警检查完成，扫描 %s 条库存，新增 %s 条，跳过 %s 条",
                    summary.get("checked"), summary.get("created"), summary.get("skipped")));
            result.put("data", summary);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("msg", "滞销预警检查失败：" + e.getMessage());
            result.put("data", null);
        }
        return result;
    }
}
