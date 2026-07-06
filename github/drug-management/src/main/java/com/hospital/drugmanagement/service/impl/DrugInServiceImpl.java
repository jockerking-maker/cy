package com.hospital.drugmanagement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hospital.drugmanagement.entity.DrugIn;
import com.hospital.drugmanagement.entity.DrugInfo;
import com.hospital.drugmanagement.entity.DrugStock;
import com.hospital.drugmanagement.entity.PurchaseOrder;
import com.hospital.drugmanagement.entity.PurchaseOrderItem;
import com.hospital.drugmanagement.entity.WarehouseInfo;
import com.hospital.drugmanagement.entity.SysUser;
import com.hospital.drugmanagement.mapper.DrugInMapper;
import com.hospital.drugmanagement.mapper.DrugInfoMapper;
import com.hospital.drugmanagement.mapper.DrugStockMapper;
import com.hospital.drugmanagement.mapper.WarehouseInfoMapper;
import com.hospital.drugmanagement.mapper.SysUserMapper;
import com.hospital.drugmanagement.service.IDrugInService;
import com.hospital.drugmanagement.service.IPurchaseOrderItemService;
import com.hospital.drugmanagement.service.IPurchaseOrderService;
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
 * 药品入库业务实现。
 * <p>
 * 核心流程：校验 → 写入 drug_in → 更新 drug_stock → 触发库存预警 → 回写采购单状态。
 * 入库与库存更新在同一事务中完成，保证数据一致性。
 */
@Service
public class DrugInServiceImpl extends ServiceImpl<DrugInMapper, DrugIn> implements IDrugInService {

    @Autowired
    private DrugInMapper drugInMapper;

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

    @Autowired
    private IPurchaseOrderService purchaseOrderService;

    @Autowired
    private IPurchaseOrderItemService purchaseOrderItemService;

    /** 分页查询入库单列表，填充药品名、仓库名、操作人。 */
    @Override
    public Map<String, Object> getDrugInList(int page, int size, String inNo, String drugName, Long warehouseId) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 构建查询条件
            LambdaQueryWrapper<DrugIn> queryWrapper = new LambdaQueryWrapper<>();
            if (inNo != null && !inNo.isEmpty()) {
                queryWrapper.like(DrugIn::getInNo, inNo);
            }
            if (warehouseId != null) {
                queryWrapper.eq(DrugIn::getWarehouseId, warehouseId);
            }
            queryWrapper.orderByDesc(DrugIn::getCreateTime);

            // 分页查询
            Page<DrugIn> pageInfo = new Page<>(page, size);
            Page<DrugIn> drugInPage = drugInMapper.selectPage(pageInfo, queryWrapper);

            // 构建返回数据，填充关联信息
            List<DrugIn> drugInList = drugInPage.getRecords();
            for (DrugIn drugIn : drugInList) {
                // 获取药品信息
                DrugInfo drugInfo = drugInfoMapper.selectById(drugIn.getDrugId());
                if (drugInfo != null) {
                    drugIn.setDrugName(drugInfo.getDrugName());
                    drugIn.setSpec(drugInfo.getSpec());
                }
                // 获取仓库信息
                WarehouseInfo warehouseInfo = warehouseInfoMapper.selectById(drugIn.getWarehouseId());
                if (warehouseInfo != null) {
                    drugIn.setWarehouseName(warehouseInfo.getWarehouseName());
                }
                // 获取操作人信息
                if (drugIn.getCreateUserId() != null) {
                    SysUser user = sysUserMapper.selectById(drugIn.getCreateUserId());
                    if (user != null) {
                        drugIn.setOperatorName(user.getRealName());
                    }
                }
            }

            result.put("data", drugInList);
            result.put("total", drugInPage.getTotal());
        } catch (Exception e) {
            e.printStackTrace();
            result.put("data", null);
            result.put("total", 0);
        }
        return result;
    }

    /** 按日期生成批次号，格式 PCyyyyMMdd001。 */
    @Override
    public String generateBatchNo() {
        String today = LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "PC" + today;

        LambdaQueryWrapper<DrugIn> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.likeRight(DrugIn::getBatchNo, prefix)
                .orderByDesc(DrugIn::getBatchNo)
                .last("LIMIT 1");

        DrugIn lastIn = drugInMapper.selectOne(queryWrapper);
        int sequence = 1;
        if (lastIn != null && lastIn.getBatchNo() != null) {
            try {
                String seqStr = lastIn.getBatchNo().substring(prefix.length());
                sequence = Integer.parseInt(seqStr) + 1;
            } catch (Exception e) {
                sequence = 1;
            }
        }
        return prefix + String.format("%03d", sequence);
    }

    /**
     * 保存入库单（事务）：写入库记录、累加库存、检查预警、更新关联采购单状态。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveDrugIn(DrugIn drugIn) {
        try {
            // 数据校验
            if (drugIn.getDrugId() == null) {
                throw new IllegalArgumentException("药品 ID 不能为空");
            }
            if (drugIn.getWarehouseId() == null) {
                throw new IllegalArgumentException("仓库 ID 不能为空");
            }
            if (drugIn.getQuantity() == null || drugIn.getQuantity() <= 0) {
                throw new IllegalArgumentException("入库数量必须大于 0");
            }
            if (drugIn.getPurchasePrice() == null || drugIn.getPurchasePrice().compareTo(java.math.BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("入库单价必须大于 0");
            }
            if (!StringUtils.hasText(drugIn.getBatchNo())) {
                throw new IllegalArgumentException("批次号不能为空");
            }
            if (drugIn.getProductionDate() == null) {
                throw new IllegalArgumentException("生产日期不能为空");
            }
            if (drugIn.getExpiryDate() == null) {
                throw new IllegalArgumentException("过期日期不能为空");
            }
            if (drugIn.getExpiryDate().isBefore(drugIn.getProductionDate())) {
                throw new IllegalArgumentException("过期日期不能早于生产日期");
            }
            if (drugIn.getExpiryDate().isBefore(LocalDate.now())) {
                throw new IllegalArgumentException("过期日期不能早于当前日期");
            }

            validateEnabledWarehouse(drugIn.getWarehouseId());
            validatePurchaseOrderLink(drugIn);

            // 生成入库单号
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
            String inNo = "RK" + sdf.format(new Date());
            drugIn.setInNo(inNo);
            
            // 设置入库时间
            drugIn.setInDate(LocalDateTime.now());

            // 保存入库记录
            boolean saved = save(drugIn);
            if (saved) {
                // 更新库存
                updateStock(drugIn);
                
                // 检查并创建预警
                stockWarningService.checkAndCreateWarning(drugIn.getDrugId(), drugIn.getWarehouseId());

                // 关联采购单时，检查是否已全部入库完成
                checkAndUpdatePurchaseOrderStatus(drugIn.getOrderId());
            }
            return saved;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("保存入库单失败：" + e.getMessage());
        }
    }

    /**
     * 更新库存
     */
    private void updateStock(DrugIn drugIn) {
        // 查询是否存在相同批次的库存记录
        LambdaQueryWrapper<DrugStock> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DrugStock::getDrugId, drugIn.getDrugId())
                .eq(DrugStock::getWarehouseId, drugIn.getWarehouseId())
                .eq(DrugStock::getBatchNo, drugIn.getBatchNo());
        
        DrugStock stock = drugStockMapper.selectOne(queryWrapper);
        
        if (stock != null) {
            // 如果存在，增加库存数量
            stock.setStockNum(stock.getStockNum() + drugIn.getQuantity());
            stock.setUpdateTime(LocalDateTime.now());
            drugStockMapper.updateById(stock);
        } else {
            // 如果不存在，创建新的库存记录
            DrugStock newStock = new DrugStock();
            newStock.setDrugId(drugIn.getDrugId());
            newStock.setWarehouseId(drugIn.getWarehouseId());
            newStock.setBatchNo(drugIn.getBatchNo());
            newStock.setStockNum(drugIn.getQuantity());
            newStock.setProductionDate(drugIn.getProductionDate() != null ? 
                drugIn.getProductionDate().atStartOfDay() : null);
            newStock.setExpiryDate(drugIn.getExpiryDate() != null ? 
                drugIn.getExpiryDate().atStartOfDay() : null);
            drugStockMapper.insert(newStock);
        }
    }

    /** 对已有批次追加补货入库，复用 {@link #saveDrugIn} 流程。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean replenishExistingBatch(Long stockId, Integer quantity, java.math.BigDecimal purchasePrice, Long createUserId) {
        if (stockId == null) {
            throw new IllegalArgumentException("库存批次不能为空");
        }
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("入库数量必须大于 0");
        }

        DrugStock stock = drugStockMapper.selectById(stockId);
        if (stock == null) {
            throw new IllegalArgumentException("库存批次不存在");
        }
        if (!StringUtils.hasText(stock.getBatchNo())) {
            throw new IllegalArgumentException("该批次号无效，请前往入库管理新建入库");
        }
        if (stock.getProductionDate() == null || stock.getExpiryDate() == null) {
            throw new IllegalArgumentException("该批次缺少生产日期或效期，请前往入库管理完整登记");
        }
        if (stock.getExpiryDate().toLocalDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("该批次已过期，不能追加补货");
        }

        java.math.BigDecimal price = purchasePrice;
        if (price == null || price.compareTo(java.math.BigDecimal.ZERO) <= 0) {
            DrugInfo drugInfo = drugInfoMapper.selectById(stock.getDrugId());
            if (drugInfo != null && drugInfo.getPurchasePrice() != null
                    && drugInfo.getPurchasePrice().compareTo(java.math.BigDecimal.ZERO) > 0) {
                price = drugInfo.getPurchasePrice();
            } else {
                throw new IllegalArgumentException("请填写入库单价");
            }
        }

        DrugIn drugIn = new DrugIn();
        drugIn.setDrugId(stock.getDrugId());
        drugIn.setWarehouseId(stock.getWarehouseId());
        drugIn.setQuantity(quantity);
        drugIn.setBatchNo(stock.getBatchNo());
        drugIn.setPurchasePrice(price);
        drugIn.setProductionDate(stock.getProductionDate().toLocalDate());
        drugIn.setExpiryDate(stock.getExpiryDate().toLocalDate());
        drugIn.setInType("补货入库");
        drugIn.setRemark("库存不足补货（追加至现有批次）");
        drugIn.setCreateUserId(createUserId);
        return saveDrugIn(drugIn);
    }

    private void validateEnabledWarehouse(Long warehouseId) {
        WarehouseInfo warehouse = warehouseInfoMapper.selectById(warehouseId);
        if (warehouse == null) {
            throw new IllegalArgumentException("仓库不存在");
        }
        if (warehouse.getStatus() == null || warehouse.getStatus() != 1) {
            throw new IllegalArgumentException("仓库已禁用，无法入库");
        }
    }

    private void validatePurchaseOrderLink(DrugIn drugIn) {
        if (drugIn.getOrderId() == null) {
            return;
        }

        PurchaseOrder order = purchaseOrderService.getById(drugIn.getOrderId());
        if (order == null) {
            throw new IllegalArgumentException("关联的采购单不存在");
        }
        if (order.getStatus() == null || order.getStatus() != 1) {
            throw new IllegalArgumentException("只能关联「已审核」状态的采购单");
        }

        LambdaQueryWrapper<PurchaseOrderItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(PurchaseOrderItem::getOrderId, drugIn.getOrderId())
                .eq(PurchaseOrderItem::getDrugId, drugIn.getDrugId());
        if (purchaseOrderItemService.count(itemWrapper) == 0) {
            throw new IllegalArgumentException("该药品不在所选采购单明细中");
        }
    }

    /**
     * 采购单所有明细药品均入库完成后，状态更新为「已入库」
     */
    private void checkAndUpdatePurchaseOrderStatus(Long orderId) {
        if (orderId == null) {
            return;
        }

        PurchaseOrder order = purchaseOrderService.getById(orderId);
        if (order == null || order.getStatus() == null || order.getStatus() != 1) {
            return;
        }

        List<PurchaseOrderItem> items = purchaseOrderItemService.list(
                new LambdaQueryWrapper<PurchaseOrderItem>().eq(PurchaseOrderItem::getOrderId, orderId));
        if (items.isEmpty()) {
            return;
        }

        for (PurchaseOrderItem item : items) {
            LambdaQueryWrapper<DrugIn> inWrapper = new LambdaQueryWrapper<>();
            inWrapper.eq(DrugIn::getOrderId, orderId)
                    .eq(DrugIn::getDrugId, item.getDrugId());
            List<DrugIn> inboundRecords = list(inWrapper);
            int receivedQty = inboundRecords.stream()
                    .mapToInt(record -> record.getQuantity() != null ? record.getQuantity() : 0)
                    .sum();
            int requiredQty = item.getPurchaseNum() != null ? item.getPurchaseNum() : 0;
            if (receivedQty < requiredQty) {
                return;
            }
        }

        order.setStatus(2);
        purchaseOrderService.updateById(order);
    }
}