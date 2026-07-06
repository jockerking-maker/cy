package com.hospital.drugmanagement.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.hospital.drugmanagement.entity.SystemNotice;

import java.util.List;

/**
 * 系统公告 Service 接口
 */
public interface SystemNoticeService extends IService<SystemNotice> {
    
    /**
     * 获取公告列表（按创建时间倒序）
     * @return 公告列表
     */
    List<SystemNotice> getNoticeList();
}
