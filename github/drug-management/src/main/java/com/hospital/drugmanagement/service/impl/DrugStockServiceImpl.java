package com.hospital.drugmanagement.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hospital.drugmanagement.entity.DrugStock;
import com.hospital.drugmanagement.mapper.DrugStockMapper;
import com.hospital.drugmanagement.service.IDrugStockService;
import org.springframework.stereotype.Service;

/**
 * 药品库存（批次）基础 Service。
 * <p>
 * 继承 MyBatis-Plus 通用 CRUD，复杂库存查询与汇总见 {@link StockServiceImpl}。
 */
@Service
public class DrugStockServiceImpl extends ServiceImpl<DrugStockMapper, DrugStock> implements IDrugStockService {
}