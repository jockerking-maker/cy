package com.hospital.drugmanagement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hospital.drugmanagement.entity.AuditRecord;
import com.hospital.drugmanagement.entity.DrugIn;
import com.hospital.drugmanagement.entity.PurchaseOrder;
import com.hospital.drugmanagement.entity.PurchaseOrderItem;
import com.hospital.drugmanagement.mapper.DrugInMapper;
import com.hospital.drugmanagement.mapper.PurchaseOrderMapper;
import com.hospital.drugmanagement.service.IAuditRecordService;
import com.hospital.drugmanagement.service.IPurchaseOrderItemService;
import com.hospital.drugmanagement.service.IPurchaseOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 采购订单业务实现：删除前校验关联入库记录，级联删除明细与审核记录。
 */
@Service
public class PurchaseOrderServiceImpl extends ServiceImpl<PurchaseOrderMapper, PurchaseOrder> implements IPurchaseOrderService {

    @Autowired
    private DrugInMapper drugInMapper;

    @Autowired
    private IPurchaseOrderItemService purchaseOrderItemService;

    @Autowired
    private IAuditRecordService auditRecordService;

    /** 返回不可删除的原因；返回 null 表示可以删除。 */
    @Override
    public String getDeleteBlockReason(Long orderId) {
        if (orderId == null) {
            return "采购单不存在";
        }

        PurchaseOrder order = getById(orderId);
        if (order == null) {
            return "采购单不存在";
        }

        Integer status = order.getStatus();
        if (status != null && status == 2) {
            return "已入库的采购单不能删除";
        }
        if (status != null && status == 1) {
            return "已审核的采购单不能删除，请先作废";
        }

        LambdaQueryWrapper<DrugIn> inWrapper = new LambdaQueryWrapper<>();
        inWrapper.eq(DrugIn::getOrderId, orderId);
        if (drugInMapper.selectCount(inWrapper) > 0) {
            return "存在关联入库记录，不能删除";
        }

        return null;
    }

    /** 级联删除采购单：明细、审核记录、主单（事务）。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteOrderCascade(Long orderId) {
        String blockReason = getDeleteBlockReason(orderId);
        if (blockReason != null) {
            throw new IllegalArgumentException(blockReason);
        }

        LambdaQueryWrapper<PurchaseOrderItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(PurchaseOrderItem::getOrderId, orderId);
        purchaseOrderItemService.remove(itemWrapper);

        LambdaQueryWrapper<AuditRecord> auditWrapper = new LambdaQueryWrapper<>();
        auditWrapper.eq(AuditRecord::getOrderId, orderId);
        auditRecordService.remove(auditWrapper);

        removeById(orderId);
    }

    /** 批量删除采购单，跳过不可删记录并返回 deleted/skipped 统计。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> batchDeleteOrders(List<Long> orderIds) {
        int deleted = 0;
        int skipped = 0;

        for (Long orderId : orderIds) {
            String blockReason = getDeleteBlockReason(orderId);
            if (blockReason != null) {
                skipped++;
                continue;
            }
            deleteOrderCascade(orderId);
            deleted++;
        }

        if (deleted == 0) {
            throw new IllegalArgumentException("没有可删除的采购单，仅可删除待审核、已取消或审核不通过的记录");
        }

        Map<String, Object> summary = new HashMap<>();
        summary.put("deleted", deleted);
        summary.put("skipped", skipped);
        return summary;
    }
}
