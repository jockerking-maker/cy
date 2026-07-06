package com.hospital.drugmanagement.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hospital.drugmanagement.entity.PurchaseOrderItem;
import com.hospital.drugmanagement.mapper.PurchaseOrderItemMapper;
import com.hospital.drugmanagement.service.IPurchaseOrderItemService;
import org.springframework.stereotype.Service;

/**
 * 采购单明细基础 Service。
 * <p>
 * 对应 purchase_order_item 表，明细的增删改查由采购单 Controller 编排，删除逻辑见 {@link PurchaseOrderServiceImpl}。
 */
@Service
public class PurchaseOrderItemServiceImpl extends ServiceImpl<PurchaseOrderItemMapper, PurchaseOrderItem> implements IPurchaseOrderItemService {
}