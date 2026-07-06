package com.hospital.drugmanagement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hospital.drugmanagement.entity.DrugLock;
import com.hospital.drugmanagement.entity.DrugStock;
import com.hospital.drugmanagement.mapper.DrugLockMapper;
import com.hospital.drugmanagement.mapper.DrugStockMapper;
import com.hospital.drugmanagement.service.DrugLockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Map;

/**
 * 库存锁定业务实现。
 * <p>
 * 出库前可锁定指定批次数量（增加 lockNum）；解锁时恢复可用库存。锁定/解锁与库存更新在同一事务中完成。
 */
@Service
public class DrugLockServiceImpl extends ServiceImpl<DrugLockMapper, DrugLock> implements DrugLockService {

    @Autowired
    private DrugStockMapper drugStockMapper;

    /** 锁定或部分解锁库存：lockNum &gt; 0 为锁定，&lt; 0 为直接扣减锁定量。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean lockDrug(DrugLock drugLock) {
        try {
            if (drugLock.getDrugId() == null) {
                throw new IllegalArgumentException("药品 ID 不能为空");
            }
            if (drugLock.getWarehouseId() == null) {
                throw new IllegalArgumentException("仓库 ID 不能为空");
            }
            if (drugLock.getLockNum() == null) {
                throw new IllegalArgumentException("锁定数量不能为空");
            }

            LambdaQueryWrapper<DrugStock> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(DrugStock::getDrugId, drugLock.getDrugId())
                    .eq(DrugStock::getWarehouseId, drugLock.getWarehouseId());
            if (drugLock.getBatchNo() != null && !drugLock.getBatchNo().isEmpty()) {
                queryWrapper.eq(DrugStock::getBatchNo, drugLock.getBatchNo());
            }
            DrugStock stock = drugStockMapper.selectOne(queryWrapper);

            if (stock == null) {
                throw new IllegalArgumentException("该药品在该仓库没有对应批次的库存");
            }

            int availableNum = stock.getStockNum() - stock.getLockNum();

            if (drugLock.getLockNum() > 0) {
                if (availableNum < drugLock.getLockNum()) {
                    throw new IllegalArgumentException("可用库存不足，当前可用库存为：" + availableNum);
                }

                SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
                String lockNo = "SD" + sdf.format(new Date());
                drugLock.setLockNo(lockNo);
                drugLock.setLockTime(LocalDateTime.now());
                drugLock.setUnlockNum(0);
                drugLock.setStatus(0);

                boolean saved = save(drugLock);
                if (saved) {
                    stock.setLockNum(stock.getLockNum() + drugLock.getLockNum());
                    stock.setUpdateTime(LocalDateTime.now());
                    drugStockMapper.updateById(stock);
                }
                return saved;
            } else if (drugLock.getLockNum() < 0) {
                if (Math.abs(drugLock.getLockNum()) > stock.getLockNum()) {
                    throw new IllegalArgumentException("解锁数量不能超过当前锁定数量：" + stock.getLockNum());
                }

                stock.setLockNum(stock.getLockNum() + drugLock.getLockNum());
                stock.setUpdateTime(LocalDateTime.now());
                drugStockMapper.updateById(stock);

                return true;
            } else {
                return true;
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("锁定药品失败：" + e.getMessage());
        }
    }

    /** 按锁定单 ID 全部解锁，并同步减少 drug_stock.lock_num。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean unlockDrug(Long lockId, Long unlockUserId) {
        try {
            if (lockId == null) {
                throw new IllegalArgumentException("锁定 ID 不能为空");
            }

            // 查询锁定记录
            DrugLock drugLock = getById(lockId);
            if (drugLock == null) {
                throw new IllegalArgumentException("锁定记录不存在");
            }
            if (drugLock.getStatus() != 0) {
                throw new IllegalArgumentException("该锁定记录已解锁或已取消");
            }

            // 更新锁定记录
            drugLock.setStatus(1); // 已解锁
            drugLock.setUnlockUserId(unlockUserId);
            drugLock.setUnlockTime(LocalDateTime.now());
            drugLock.setUnlockNum(drugLock.getLockNum()); // 全部解锁
            updateById(drugLock);

            LambdaQueryWrapper<DrugStock> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(DrugStock::getDrugId, drugLock.getDrugId())
                    .eq(DrugStock::getWarehouseId, drugLock.getWarehouseId());
            if (drugLock.getBatchNo() != null && !drugLock.getBatchNo().isEmpty()) {
                queryWrapper.eq(DrugStock::getBatchNo, drugLock.getBatchNo());
            }
            DrugStock stock = drugStockMapper.selectOne(queryWrapper);
            
            if (stock != null) {
                stock.setLockNum(Math.max(0, stock.getLockNum() - drugLock.getLockNum()));
                stock.setUpdateTime(LocalDateTime.now());
                drugStockMapper.updateById(stock);
            }

            return true;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("解锁药品失败：" + e.getMessage());
        }
    }

    /** 分页查询锁定记录，支持按药品、仓库、状态筛选。 */
    @Override
    public Page<DrugLock> pageList(Map<String, Object> params) {
        int page = params.get("page") != null ? Integer.parseInt(params.get("page").toString()) : 1;
        int size = params.get("size") != null ? Integer.parseInt(params.get("size").toString()) : 10;
        
        Page<DrugLock> drugLockPage = new Page<>(page, size);
        LambdaQueryWrapper<DrugLock> queryWrapper = new LambdaQueryWrapper<>();
        
        if (params.get("drugId") != null) {
            queryWrapper.eq(DrugLock::getDrugId, Long.parseLong(params.get("drugId").toString()));
        }
        if (params.get("warehouseId") != null) {
            queryWrapper.eq(DrugLock::getWarehouseId, Long.parseLong(params.get("warehouseId").toString()));
        }
        if (params.get("status") != null) {
            queryWrapper.eq(DrugLock::getStatus, Integer.parseInt(params.get("status").toString()));
        }
        
        queryWrapper.orderByDesc(DrugLock::getLockTime);
        
        return page(drugLockPage, queryWrapper);
    }
}
