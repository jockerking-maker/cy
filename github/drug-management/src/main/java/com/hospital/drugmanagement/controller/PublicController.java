package com.hospital.drugmanagement.controller;

import com.hospital.drugmanagement.entity.SystemNotice;
import com.hospital.drugmanagement.service.DashboardService;
import com.hospital.drugmanagement.service.SystemNoticeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 登录页等无需鉴权的公开接口。
 * <p>
 * 供未登录用户访问，例如登录页展示的系统概览与公告信息。
 */
@RestController
@RequestMapping("/api/public")
@CrossOrigin(origins = "*")
public class PublicController {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private SystemNoticeService systemNoticeService;

    /** 登录页数据：药品/预警/采购统计 + 最新公告（最多 5 条） */
    @GetMapping("/login-info")
    public Map<String, Object> getLoginInfo() {
        Map<String, Object> result = new HashMap<>();
        try {
            Map<String, Object> stats = dashboardService.getStats();
            Map<String, Object> summary = new HashMap<>();
            summary.put("drugCount", stats.getOrDefault("drugCount", 0));
            summary.put("warningCount", stats.getOrDefault("warningCount", 0));
            summary.put("purchaseCount", stats.getOrDefault("purchaseCount", 0));

            List<SystemNotice> notices = systemNoticeService.getNoticeList();
            List<Map<String, Object>> noticeItems = new ArrayList<>();
            if (notices != null) {
                int limit = Math.min(notices.size(), 5);
                for (int i = 0; i < limit; i++) {
                    SystemNotice notice = notices.get(i);
                    Map<String, Object> item = new HashMap<>();
                    item.put("noticeId", notice.getNoticeId());
                    item.put("title", notice.getTitle());
                    item.put("content", truncate(notice.getContent(), 120));
                    if (notice.getCreateTime() != null) {
                        item.put("createTime", notice.getCreateTime().format(DATE_TIME_FORMATTER));
                    }
                    noticeItems.add(item);
                }
            }

            Map<String, Object> data = new HashMap<>();
            data.put("summary", summary);
            data.put("notices", noticeItems);
            data.put("serverTime", LocalDateTime.now().format(DATE_TIME_FORMATTER));
            data.put("version", "v1.0");

            result.put("code", 200);
            result.put("msg", "success");
            result.put("data", data);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("msg", "获取登录页信息失败：" + e.getMessage());
            result.put("data", null);
        }
        return result;
    }

    private String truncate(String text, int maxLen) {
        if (text == null) {
            return "";
        }
        String trimmed = text.trim();
        if (trimmed.length() <= maxLen) {
            return trimmed;
        }
        return trimmed.substring(0, maxLen) + "…";
    }
}
