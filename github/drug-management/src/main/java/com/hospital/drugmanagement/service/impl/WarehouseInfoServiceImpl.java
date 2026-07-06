package com.hospital.drugmanagement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hospital.drugmanagement.entity.DrugLock;
import com.hospital.drugmanagement.entity.DrugStock;
import com.hospital.drugmanagement.entity.StockCheck;
import com.hospital.drugmanagement.entity.WarehouseInfo;
import com.hospital.drugmanagement.mapper.DrugLockMapper;
import com.hospital.drugmanagement.mapper.DrugStockMapper;
import com.hospital.drugmanagement.mapper.StockCheckMapper;
import com.hospital.drugmanagement.mapper.WarehouseInfoMapper;
import com.hospital.drugmanagement.service.IWarehouseInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 仓库信息业务实现。
 * <p>
 * 删除前校验是否仍有库存/锁定/进行中盘点；自动生成仓库编码（WH001）。
 */
@Service
public class WarehouseInfoServiceImpl extends ServiceImpl<WarehouseInfoMapper, WarehouseInfo> implements IWarehouseInfoService {

    private static final String WAREHOUSE_CODE_PREFIX = "WH";

    @Autowired
    private DrugStockMapper drugStockMapper;

    @Autowired
    private DrugLockMapper drugLockMapper;

    @Autowired
    private StockCheckMapper stockCheckMapper;

    /** 返回不可删除的原因；返回 null 表示可以删除。 */
    @Override
    public String getDeleteBlockReason(Long warehouseId) {
        LambdaQueryWrapper<DrugStock> stockWrapper = new LambdaQueryWrapper<>();
        stockWrapper.eq(DrugStock::getWarehouseId, warehouseId)
                .and(w -> w.gt(DrugStock::getStockNum, 0).or().gt(DrugStock::getLockNum, 0));
        if (drugStockMapper.selectCount(stockWrapper) > 0) {
            return "该仓库仍有库存或锁定药品，无法删除，请先清空库存或改为禁用";
        }

        LambdaQueryWrapper<DrugLock> lockWrapper = new LambdaQueryWrapper<>();
        lockWrapper.eq(DrugLock::getWarehouseId, warehouseId)
                .eq(DrugLock::getStatus, 0);
        if (drugLockMapper.selectCount(lockWrapper) > 0) {
            return "该仓库仍有进行中的库存锁定，无法删除";
        }

        LambdaQueryWrapper<StockCheck> checkWrapper = new LambdaQueryWrapper<>();
        checkWrapper.eq(StockCheck::getWarehouseId, warehouseId)
                .eq(StockCheck::getStatus, 0);
        if (stockCheckMapper.selectCount(checkWrapper) > 0) {
            return "该仓库有进行中的盘点单，无法删除";
        }

        return null;
    }

    /** 生成下一个不重复的仓库编码，格式 WH001。 */
    @Override
    public String generateNextWarehouseCode() {
        LambdaQueryWrapper<WarehouseInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(WarehouseInfo::getWarehouseCode, WAREHOUSE_CODE_PREFIX)
                .orderByDesc(WarehouseInfo::getWarehouseCode)
                .last("LIMIT 1");

        WarehouseInfo last = getOne(wrapper, false);
        int nextSeq = 1;
        if (last != null && last.getWarehouseCode() != null && last.getWarehouseCode().startsWith(WAREHOUSE_CODE_PREFIX)) {
            try {
                nextSeq = Integer.parseInt(last.getWarehouseCode().substring(WAREHOUSE_CODE_PREFIX.length())) + 1;
            } catch (NumberFormatException ignored) {
                nextSeq = (int) count() + 1;
            }
        }

        for (int i = 0; i < 1000; i++) {
            String candidate = nextSeq <= 999
                    ? String.format("%s%03d", WAREHOUSE_CODE_PREFIX, nextSeq)
                    : WAREHOUSE_CODE_PREFIX + nextSeq;
            LambdaQueryWrapper<WarehouseInfo> check = new LambdaQueryWrapper<>();
            check.eq(WarehouseInfo::getWarehouseCode, candidate);
            if (count(check) == 0) {
                return candidate;
            }
            nextSeq++;
        }
        throw new IllegalStateException("无法生成可用仓库编码");
    }
}
