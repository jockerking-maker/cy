package com.hospital.drugmanagement.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hospital.drugmanagement.entity.WarehouseInfo;

public interface IWarehouseInfoService extends IService<WarehouseInfo> {

    /**
     * @return 不可删除时的原因；可删除时返回 null
     */
    String getDeleteBlockReason(Long warehouseId);

    String generateNextWarehouseCode();
}