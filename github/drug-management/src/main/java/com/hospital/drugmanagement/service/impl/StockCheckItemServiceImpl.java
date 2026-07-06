package com.hospital.drugmanagement.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hospital.drugmanagement.entity.StockCheckItem;
import com.hospital.drugmanagement.mapper.StockCheckItemMapper;
import com.hospital.drugmanagement.service.IStockCheckItemService;
import org.springframework.stereotype.Service;

/**
 * 库存盘点明细基础 Service。
 * <p>
 * 对应 stock_check_item 表，每条明细对应一个药品批次及其系统数量、实盘数量、盈亏差异。
 */
@Service
public class StockCheckItemServiceImpl extends ServiceImpl<StockCheckItemMapper, StockCheckItem> implements IStockCheckItemService {
}