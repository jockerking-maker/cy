package com.hospital.drugmanagement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hospital.drugmanagement.entity.SystemNotice;
import com.hospital.drugmanagement.mapper.SystemNoticeMapper;
import com.hospital.drugmanagement.service.SystemNoticeService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 系统公告业务实现。
 * <p>
 * 查询启用状态的公告列表，供登录页与首页展示。
 */
@Service
public class SystemNoticeServiceImpl extends ServiceImpl<SystemNoticeMapper, SystemNotice> implements SystemNoticeService {

    /** 查询启用公告，按创建时间倒序。 */
    @Override
    public List<SystemNotice> getNoticeList() {
        // 查询启用的公告，按创建时间倒序
        LambdaQueryWrapper<SystemNotice> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SystemNotice::getStatus, 1); // 只查询启用状态
        queryWrapper.orderByDesc(SystemNotice::getCreateTime); // 按创建时间倒序
        return this.list(queryWrapper);
    }
}
