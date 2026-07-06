package com.hospital.drugmanagement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hospital.drugmanagement.entity.AuditRecord;
import com.hospital.drugmanagement.mapper.AuditRecordMapper;
import com.hospital.drugmanagement.service.IAuditRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 审核记录业务实现。
 * <p>
 * 记录采购单等多级审批流程，供采购单详情页展示审批历史。
 */
@Service
public class AuditRecordServiceImpl extends ServiceImpl<AuditRecordMapper, AuditRecord> implements IAuditRecordService {

    @Autowired
    private AuditRecordMapper auditRecordMapper;

    /** 按采购单 ID 查询审核记录，按审批层级升序、时间降序排列。 */
    @Override
    public List<AuditRecord> getAuditRecordsByOrderId(Long orderId) {
        LambdaQueryWrapper<AuditRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AuditRecord::getOrderId, orderId);
        queryWrapper.orderByAsc(AuditRecord::getAuditLevel);
        queryWrapper.orderByDesc(AuditRecord::getAuditTime);
        return auditRecordMapper.selectList(queryWrapper);
    }

    /** 保存一条审核记录（通过/驳回/提交等）。 */
    @Override
    public boolean saveAuditRecord(AuditRecord auditRecord) {
        return auditRecordMapper.insert(auditRecord) > 0;
    }
}
