package com.hospital.drugmanagement.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hospital.drugmanagement.entity.PurchaseOrder;

import java.util.List;
import java.util.Map;

public interface IPurchaseOrderService extends IService<PurchaseOrder> {

    String getDeleteBlockReason(Long orderId);

    void deleteOrderCascade(Long orderId);

    Map<String, Object> batchDeleteOrders(List<Long> orderIds);
}