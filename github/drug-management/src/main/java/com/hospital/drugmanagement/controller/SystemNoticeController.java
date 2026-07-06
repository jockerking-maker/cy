package com.hospital.drugmanagement.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hospital.drugmanagement.common.anno.RequireRole;
import com.hospital.drugmanagement.entity.SystemNotice;
import com.hospital.drugmanagement.service.SystemNoticeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 系统公告接口。
 * <p>
 * 公告列表对所有登录用户开放；新建/编辑/删除仅 ADMIN。
 */
@RestController
@RequestMapping("/api/notice")
@CrossOrigin(origins = "*")
public class SystemNoticeController {

    @Autowired
    private SystemNoticeService systemNoticeService;

    /**
     * 获取公告列表
     */
    @GetMapping("/list")
    public Map<String, Object> getNoticeList() {
        Map<String, Object> result = new HashMap<>();
        try {
            List<SystemNotice> noticeList = systemNoticeService.getNoticeList();
            result.put("code", 200);
            result.put("msg", "success");
            result.put("data", noticeList);
            result.put("total", noticeList != null ? noticeList.size() : 0);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("msg", "查询失败：" + e.getMessage());
            result.put("data", null);
        }
        return result;
    }

    /**
     * 新建公告
     */
    @PostMapping
    @RequireRole("ADMIN")
    public Map<String, Object> saveNotice(
            @RequestAttribute("currentUserId") Long currentUserId,
            @RequestBody Map<String, Object> data) {
        Map<String, Object> result = new HashMap<>();
        try {
            String title = data.get("title") != null ? data.get("title").toString() : "";
            String content = data.get("content") != null ? data.get("content").toString() : "";
            String createUserName = data.get("createUserName") != null ? data.get("createUserName").toString() : "";

            if (title.isEmpty() || content.isEmpty()) {
                result.put("code", 400);
                result.put("msg", "公告标题和内容不能为空");
                result.put("data", null);
                return result;
            }

            SystemNotice notice = new SystemNotice();
            notice.setTitle(title);
            notice.setContent(content);
            notice.setCreateUserId(currentUserId);
            notice.setCreateUserName(createUserName);
            notice.setStatus(1); // 启用状态

            boolean success = systemNoticeService.save(notice);
            result.put("code", success ? 200 : 500);
            result.put("msg", success ? "创建成功" : "创建失败");
            result.put("data", null);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("msg", "创建失败：" + e.getMessage());
            result.put("data", null);
        }
        return result;
    }

    /**
     * 编辑公告
     */
    @PutMapping
    @RequireRole("ADMIN")
    public Map<String, Object> updateNotice(@RequestBody Map<String, Object> data) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long noticeId = data.get("noticeId") != null ? Long.valueOf(data.get("noticeId").toString()) : null;
            String title = data.get("title") != null ? data.get("title").toString() : "";
            String content = data.get("content") != null ? data.get("content").toString() : "";

            if (noticeId == null || title.isEmpty() || content.isEmpty()) {
                result.put("code", 400);
                result.put("msg", "公告 ID、标题和内容不能为空");
                result.put("data", null);
                return result;
            }

            SystemNotice notice = systemNoticeService.getById(noticeId);
            if (notice == null) {
                result.put("code", 404);
                result.put("msg", "公告不存在");
                result.put("data", null);
                return result;
            }

            notice.setTitle(title);
            notice.setContent(content);

            boolean success = systemNoticeService.updateById(notice);
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
     * 删除公告
     */
    @DeleteMapping("/{id}")
    @RequireRole("ADMIN")
    public Map<String, Object> deleteNotice(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            SystemNotice notice = systemNoticeService.getById(id);
            if (notice == null) {
                result.put("code", 404);
                result.put("msg", "公告不存在");
                result.put("data", null);
                return result;
            }

            boolean success = systemNoticeService.removeById(id);
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
