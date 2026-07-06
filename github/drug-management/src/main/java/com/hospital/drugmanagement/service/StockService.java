package com.hospital.drugmanagement.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hospital.drugmanagement.entity.DrugStock;

import java.util.List;
import java.util.Map;

public interface StockService extends IService<DrugStock> {
    
    Map<String, Object> getStockList(int page, int size, String drugName, String drugCode, Long warehouseId, boolean warning);
    
    Map<String, Object> getGroupedStockList(int page, int size, String drugName, String drugCode, Long warehouseId, boolean warning, String sortField, String sortOrder);
    
    boolean checkStock(Long warehouseId, String range);
}
