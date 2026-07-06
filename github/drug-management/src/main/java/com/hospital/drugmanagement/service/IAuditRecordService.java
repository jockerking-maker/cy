package com.hospital.drugmanagement.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hospital.drugmanagement.entity.AuditRecord;

import java.util.List;

public interface IAuditRecordService extends IService<AuditRecord> {
    
    /**
     * 获取采购单的审核记录
     * @param orderId 采购单ID
     * @return 审核记录列表
     */
    List<AuditRecord> getAuditRecordsByOrderId(Long orderId);
    
    /**
     * 保存审核记录
     * @param auditRecord 审核记录
     * @return 是否保存成功
     */
    boolean saveAuditRecord(AuditRecord auditRecord);
}
