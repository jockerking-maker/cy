package com.hospital.drugmanagement.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.hospital.drugmanagement.entity.DrugLock;

import java.util.Map;

public interface DrugLockService extends IService<DrugLock> {
    
    /**
     * 锁定药品
     */
    boolean lockDrug(DrugLock drugLock);
    
    /**
     * 解锁药品
     */
    boolean unlockDrug(Long lockId, Long unlockUserId);
    
    /**
     * 分页查询锁定记录
     */
    Page<DrugLock> pageList(Map<String, Object> params);
}
