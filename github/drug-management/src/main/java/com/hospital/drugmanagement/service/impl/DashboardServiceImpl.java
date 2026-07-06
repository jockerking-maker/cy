package com.hospital.drugmanagement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hospital.drugmanagement.entity.DrugInfo;
import com.hospital.drugmanagement.entity.DrugStock;
import com.hospital.drugmanagement.entity.PurchaseOrder;
import com.hospital.drugmanagement.service.DashboardService;
import com.hospital.drugmanagement.mapper.DrugInfoMapper;
import com.hospital.drugmanagement.mapper.DrugStockMapper;
import com.hospital.drugmanagement.mapper.PurchaseOrderMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 首页仪表盘业务实现。
 * <p>
 * 汇总药品总数、预警数量、待审采购单、库存总价值、临期药品及分类/预警统计，供首页图表展示。
 */
@Service
public class DashboardServiceImpl implements DashboardService {

    @Autowired
    private DrugInfoMapper drugInfoMapper;

    @Autowired
    private DrugStockMapper drugStockMapper;

    @Autowired
    private PurchaseOrderMapper purchaseOrderMapper;

    /** 首页核心指标：药品数、预警数、待审采购、库存价值、30 天内临期药品列表。 */
    @Override
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();

        Long drugCount = drugInfoMapper.selectCount(null);
        stats.put("drugCount", drugCount != null ? drugCount : 0);

        List<DrugStock> allStock = drugStockMapper.selectList(null);
        long warningCount = 0;
        if (allStock != null) {
            for (DrugStock stock : allStock) {
                int availableNum = stock.getStockNum() - stock.getLockNum();
                DrugInfo drugInfo = drugInfoMapper.selectById(stock.getDrugId());
                if (drugInfo != null && drugInfo.getWarningNum() != null) {
                    if (availableNum < drugInfo.getWarningNum()) {
                        warningCount++;
                    }
                }
            }
        }
        stats.put("warningCount", warningCount);

        LambdaQueryWrapper<PurchaseOrder> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(PurchaseOrder::getStatus, 0);
        Long purchaseCount = purchaseOrderMapper.selectCount(queryWrapper);
        stats.put("purchaseCount", purchaseCount != null ? purchaseCount : 0);

        BigDecimal totalValue = BigDecimal.ZERO;
        if (allStock != null) {
            for (DrugStock stock : allStock) {
                DrugInfo drugInfo = drugInfoMapper.selectById(stock.getDrugId());
                if (drugInfo != null && drugInfo.getPurchasePrice() != null) {
                    BigDecimal price = drugInfo.getPurchasePrice();
                    BigDecimal stockValue = BigDecimal.valueOf(stock.getStockNum()).multiply(price);
                    totalValue = totalValue.add(stockValue);
                }
            }
        }
        stats.put("stockValue", totalValue);

        List<Map<String, Object>> nearExpiryDrugs = new ArrayList<>();
        LocalDate now = LocalDate.now();
        LocalDate nearExpiryDate = now.plusDays(30);
        if (allStock != null) {
            for (DrugStock stock : allStock) {
                if (stock.getExpiryDate() != null) {
                    LocalDate expiryDate = stock.getExpiryDate().toLocalDate();
                    if (!expiryDate.isBefore(now) && !expiryDate.isAfter(nearExpiryDate)) {
                        DrugInfo drugInfo = drugInfoMapper.selectById(stock.getDrugId());
                        if (drugInfo != null) {
                            Map<String, Object> drugMap = new HashMap<>();
                            drugMap.put("drugId", drugInfo.getDrugId());
                            drugMap.put("drugName", drugInfo.getDrugName());
                            drugMap.put("spec", drugInfo.getSpec());
                            drugMap.put("batchNo", stock.getBatchNo());
                            drugMap.put("expiryDate", expiryDate);
                            drugMap.put("stockNum", stock.getStockNum());
                            drugMap.put("remainingDays", java.time.temporal.ChronoUnit.DAYS.between(now, expiryDate));
                            nearExpiryDrugs.add(drugMap);
                        }
                    }
                }
            }
        }
        stats.put("nearExpiryDrugs", nearExpiryDrugs);

        return stats;
    }

    /** 按药品类型（西药/中药等）统计数量，供饼图使用。 */
    @Override
    public Map<String, Object> getDrugTypeStats() {
        Map<String, Object> result = new HashMap<>();
        List<DrugInfo> allDrugs = drugInfoMapper.selectList(null);
        Map<String, Long> typeCount = new HashMap<>();
        if (allDrugs != null) {
            for (DrugInfo drug : allDrugs) {
                String type = drug.getDrugType() != null ? drug.getDrugType() : "未分类";
                typeCount.put(type, typeCount.getOrDefault(type, 0L) + 1);
            }
        }
        List<Map<String, Object>> typeStats = new ArrayList<>();
        for (Map.Entry<String, Long> entry : typeCount.entrySet()) {
            Map<String, Object> item = new HashMap<>();
            item.put("name", entry.getKey());
            item.put("value", entry.getValue());
            typeStats.add(item);
        }
        result.put("typeStats", typeStats);
        return result;
    }

    /** 库存预警分类统计：不足、过剩、近效期、已过期。 */
    @Override
    public Map<String, Object> getStockWarningStats() {
        Map<String, Object> result = new HashMap<>();
        List<DrugStock> allStock = drugStockMapper.selectList(null);
        LocalDate now = LocalDate.now();

        long lowStockCount = 0;
        long overStockCount = 0;
        long nearExpiryCount = 0;
        long expiredCount = 0;

        if (allStock != null) {
            for (DrugStock stock : allStock) {
                DrugInfo drugInfo = drugInfoMapper.selectById(stock.getDrugId());
                if (drugInfo == null) continue;

                int availableNum = stock.getStockNum() - stock.getLockNum();
                if (drugInfo.getWarningNum() != null && availableNum < drugInfo.getWarningNum()) {
                    lowStockCount++;
                }
                if (drugInfo.getMaxWarningNum() != null && availableNum > drugInfo.getMaxWarningNum()) {
                    overStockCount++;
                }

                if (stock.getExpiryDate() != null) {
                    LocalDate expiryDate = stock.getExpiryDate().toLocalDate();
                    if (expiryDate.isBefore(now)) {
                        expiredCount++;
                    } else if (!expiryDate.isAfter(now.plusDays(30))) {
                        nearExpiryCount++;
                    }
                }
            }
        }

        List<String> categories = new ArrayList<>();
        categories.add("库存不足");
        categories.add("库存过剩");
        categories.add("近效期");
        categories.add("已过期");

        List<Long> values = new ArrayList<>();
        values.add(lowStockCount);
        values.add(overStockCount);
        values.add(nearExpiryCount);
        values.add(expiredCount);

        result.put("categories", categories);
        result.put("values", values);
        result.put("lowStockCount", lowStockCount);
        result.put("overStockCount", overStockCount);
        result.put("nearExpiryCount", nearExpiryCount);
        result.put("expiredCount", expiredCount);
        return result;
    }
}
