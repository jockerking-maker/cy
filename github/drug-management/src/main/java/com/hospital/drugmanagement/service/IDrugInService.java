package com.hospital.drugmanagement.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hospital.drugmanagement.entity.DrugIn;

import java.util.Map;

public interface IDrugInService extends IService<DrugIn> {
    Map<String, Object> getDrugInList(int page, int size, String inNo, String drugName, Long warehouseId);
    boolean saveDrugIn(DrugIn drugIn);
    boolean replenishExistingBatch(Long stockId, Integer quantity, java.math.BigDecimal purchasePrice, Long createUserId);
    String generateBatchNo();
}