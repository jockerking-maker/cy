package com.hospital.drugmanagement.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hospital.drugmanagement.entity.SysOperationLog;

public interface SysOperationLogService extends IService<SysOperationLog> {
    void saveLog(Long userId, String userName, String operation, String module, String description, String method, String params, String ip);
}
