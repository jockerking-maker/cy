package com.hospital.drugmanagement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hospital.drugmanagement.entity.DrugOut;
import com.hospital.drugmanagement.entity.DrugInfo;
import com.hospital.drugmanagement.entity.DrugStock;
import com.hospital.drugmanagement.entity.WarehouseInfo;
import com.hospital.drugmanagement.entity.SysUser;
import com.hospital.drugmanagement.mapper.DrugOutMapper;
import com.hospital.drugmanagement.mapper.DrugInfoMapper;
import com.hospital.drugmanagement.mapper.DrugStockMapper;
import com.hospital.drugmanagement.mapper.WarehouseInfoMapper;
import com.hospital.drugmanagement.mapper.SysUserMapper;
import com.hospital.drugmanagement.service.IDrugOutService;
import com.hospital.drugmanagement.service.StockWarningService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 药品出库业务实现。
 * <p>
 * 出库前校验批次可用库存（stockNum - lockNum），扣减指定批次库存后触发预警检测。
 */
@Service
public class DrugOutServiceImpl extends ServiceImpl<DrugOutMapper, DrugOut> implements IDrugOutService {

    @Autowired
    private DrugOutMapper drugOutMapper;

    @Autowired
    private DrugInfoMapper drugInfoMapper;

    @Autowired
    private WarehouseInfoMapper warehouseInfoMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private DrugStockMapper drugStockMapper;

    @Autowired
    private StockWarningService stockWarningService;

    /** 分页查询出库单列表，填充药品名、仓库名、操作人。 */
    @Override
    public Map<String, Object> getDrugOutList(int page, int size, String outNo, String drugName, String outType) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 构建查询条件
            LambdaQueryWrapper<DrugOut> queryWrapper = new LambdaQueryWrapper<>();
            if (outNo != null && !outNo.isEmpty()) {
                queryWrapper.like(DrugOut::getOutNo, outNo);
            }
            if (outType != null && !outType.isEmpty()) {
                queryWrapper.eq(DrugOut::getOutType, outType);
            }
            queryWrapper.orderByDesc(DrugOut::getCreateTime);

            // 分页查询
            Page<DrugOut> pageInfo = new Page<>(page, size);
            Page<DrugOut> drugOutPage = drugOutMapper.selectPage(pageInfo, queryWrapper);

            // 构建返回数据，填充关联信息
            List<DrugOut> drugOutList = drugOutPage.getRecords();
            for (DrugOut drugOut : drugOutList) {
                // 获取药品信息
                DrugInfo drugInfo = drugInfoMapper.selectById(drugOut.getDrugId());
                if (drugInfo != null) {
                    drugOut.setDrugName(drugInfo.getDrugName());
                    drugOut.setSpec(drugInfo.getSpec());
                }
                // 获取仓库信息
                WarehouseInfo warehouseInfo = warehouseInfoMapper.selectById(drugOut.getWarehouseId());
                if (warehouseInfo != null) {
                    drugOut.setWarehouseName(warehouseInfo.getWarehouseName());
                }
                // 获取操作人信息
                if (drugOut.getCreateUserId() != null) {
                    SysUser user = sysUserMapper.selectById(drugOut.getCreateUserId());
                    if (user != null) {
                        drugOut.setOperatorName(user.getRealName());
                    }
                }
            }

            result.put("data", drugOutList);
            result.put("total", drugOutPage.getTotal());
        } catch (Exception e) {
            e.printStackTrace();
            result.put("data", null);
            result.put("total", 0);
        }
        return result;
    }

    /**
     * 保存出库单（事务）：校验可用库存 → 写出库记录 → 扣减批次库存 → 检查预警。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveDrugOut(DrugOut drugOut) {
        try {
            // 数据校验
            if (drugOut.getDrugId() == null) {
                throw new IllegalArgumentException("药品 ID 不能为空");
            }
            if (drugOut.getWarehouseId() == null) {
                throw new IllegalArgumentException("仓库 ID 不能为空");
            }
            if (drugOut.getOutNum() == null || drugOut.getOutNum() <= 0) {
                throw new IllegalArgumentException("出库数量必须大于 0");
            }
            if (!StringUtils.hasText(drugOut.getOutType())) {
                throw new IllegalArgumentException("出库类型不能为空");
            }
            if (drugOut.getSalePrice() == null || drugOut.getSalePrice().compareTo(java.math.BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("出库单价必须大于 0");
            }
            if (!StringUtils.hasText(drugOut.getBatchNo())) {
                throw new IllegalArgumentException("批次号不能为空");
            }

            validateEnabledWarehouse(drugOut.getWarehouseId());

            // 检查库存是否充足
            checkStock(drugOut);

            // 生成出库单号
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
            String outNo = "CK" + sdf.format(new Date());
            drugOut.setOutNo(outNo);
            
            // 设置出库时间
            drugOut.setOutDate(LocalDateTime.now());

            // 保存出库记录
            boolean saved = save(drugOut);
            if (saved) {
                // 更新库存
                updateStock(drugOut);
                
                // 检查并创建预警
                stockWarningService.checkAndCreateWarning(drugOut.getDrugId(), drugOut.getWarehouseId());
            }
            return saved;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("保存出库单失败：" + e.getMessage());
        }
    }

    /**
     * 检查库存是否充足
     */
    private void checkStock(DrugOut drugOut) {
        DrugStock stock = findStockByBatch(drugOut);
        if (stock == null) {
            throw new IllegalArgumentException("所选批次不存在、已过期或库存不足");
        }

        int availableNum = getAvailableNum(stock);
        if (availableNum <= 0) {
            throw new IllegalArgumentException("所选批次库存已全部锁定，无法出库");
        }
        if (availableNum < drugOut.getOutNum()) {
            throw new IllegalArgumentException(
                    "所选批次可用库存不足。当前可用：" + availableNum + "，申请出库：" + drugOut.getOutNum());
        }
    }

    /**
     * 更新库存（按用户选择的批次扣减）
     */
    private void updateStock(DrugOut drugOut) {
        DrugStock stock = findStockByBatch(drugOut);
        if (stock == null) {
            throw new RuntimeException("所选批次库存不存在");
        }

        int availableNum = getAvailableNum(stock);
        if (availableNum < drugOut.getOutNum()) {
            throw new RuntimeException("所选批次可用库存不足，出库失败");
        }

        stock.setStockNum(stock.getStockNum() - drugOut.getOutNum());
        stock.setUpdateTime(LocalDateTime.now());
        drugStockMapper.updateById(stock);
    }

    private DrugStock findStockByBatch(DrugOut drugOut) {
        LambdaQueryWrapper<DrugStock> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DrugStock::getDrugId, drugOut.getDrugId())
                .eq(DrugStock::getWarehouseId, drugOut.getWarehouseId())
                .eq(DrugStock::getBatchNo, drugOut.getBatchNo())
                .gt(DrugStock::getExpiryDate, LocalDate.now().atStartOfDay())
                .gt(DrugStock::getStockNum, 0);

        return drugStockMapper.selectOne(queryWrapper, false);
    }

    private int getAvailableNum(DrugStock stock) {
        int lockNum = stock.getLockNum() != null ? stock.getLockNum() : 0;
        return stock.getStockNum() - lockNum;
    }

    private void validateEnabledWarehouse(Long warehouseId) {
        WarehouseInfo warehouse = warehouseInfoMapper.selectById(warehouseId);
        if (warehouse == null) {
            throw new IllegalArgumentException("仓库不存在");
        }
        if (warehouse.getStatus() == null || warehouse.getStatus() != 1) {
            throw new IllegalArgumentException("仓库已禁用，无法出库");
        }
    }
}