package com.hospital.drugmanagement.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hospital.drugmanagement.entity.StockCheck;
import com.hospital.drugmanagement.mapper.StockCheckMapper;
import com.hospital.drugmanagement.service.IStockCheckService;
import org.springframework.stereotype.Service;

/**
 * 库存盘点单基础 Service。
 * <p>
 * 对应 stock_check 表；创建盘点、完成盘点及库存调整逻辑在 {@link com.hospital.drugmanagement.controller.StockCheckController} 中实现。
 */
@Service
public class StockCheckServiceImpl extends ServiceImpl<StockCheckMapper, StockCheck> implements IStockCheckService {
}