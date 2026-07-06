package com.hospital.drugmanagement.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hospital.drugmanagement.entity.SysOperationLog;
import com.hospital.drugmanagement.mapper.SysOperationLogMapper;
import com.hospital.drugmanagement.service.SysOperationLogService;
import org.springframework.stereotype.Service;

/**
 * 系统操作日志业务实现。
 * <p>
 * 记录用户关键操作（模块、方法、参数、IP），供审计与问题排查；参数超长时自动截断。
 */
@Service
public class SysOperationLogServiceImpl extends ServiceImpl<SysOperationLogMapper, SysOperationLog> implements SysOperationLogService {
    /** 写入一条操作日志。 */
    @Override
    public void saveLog(Long userId, String userName, String operation, String module, String description, String method, String params, String ip) {
        SysOperationLog log = new SysOperationLog();
        log.setUserId(userId);
        log.setUserName(userName);
        log.setOperation(operation);
        log.setModule(module);
        log.setDescription(description);
        log.setMethod(method);
        log.setParams(params != null && params.length() > 2000 ? params.substring(0, 2000) : params);
        log.setIp(ip);
        save(log);
    }
}
