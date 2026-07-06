package com.hospital.drugmanagement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hospital.drugmanagement.entity.DrugInfo;
import com.hospital.drugmanagement.entity.DrugStock;
import com.hospital.drugmanagement.entity.StockCheck;
import com.hospital.drugmanagement.entity.WarehouseInfo;
import com.hospital.drugmanagement.mapper.DrugInfoMapper;
import com.hospital.drugmanagement.mapper.DrugStockMapper;
import com.hospital.drugmanagement.mapper.StockCheckMapper;
import com.hospital.drugmanagement.mapper.WarehouseInfoMapper;
import com.hospital.drugmanagement.service.StockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * 库存查询与汇总业务实现。
 * <p>
 * 提供按批次明细列表、按药品+仓库汇总列表（含批次详情）、排序与预警筛选；盘点单创建入口 {@link #checkStock}。
 */
@Service
public class StockServiceImpl extends ServiceImpl<DrugStockMapper, DrugStock> implements StockService {

    @Autowired
    private DrugStockMapper drugStockMapper;

    @Autowired
    private DrugInfoMapper drugInfoMapper;

    @Autowired
    private WarehouseInfoMapper warehouseInfoMapper;

    @Autowired
    private StockCheckMapper stockCheckMapper;

    /** 按批次返回库存明细列表，支持仓库、药品名/编码、预警筛选。 */
    @Override
    public Map<String, Object> getStockList(int page, int size, String drugName, String drugCode, Long warehouseId, boolean warning) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> stockList = new ArrayList<>();

        // 构建查询条件
        LambdaQueryWrapper<DrugStock> queryWrapper = new LambdaQueryWrapper<>();
        if (warehouseId != null) {
            queryWrapper.eq(DrugStock::getWarehouseId, warehouseId);
        }

        // 执行分页查询
        Page<DrugStock> stockPage = drugStockMapper.selectPage(new Page<>(page, size), queryWrapper);
        List<DrugStock> stocks = stockPage.getRecords();

        // 处理查询结果
        for (DrugStock stock : stocks) {
            // 获取药品信息
            DrugInfo drugInfo = drugInfoMapper.selectById(stock.getDrugId());
            if (drugInfo == null) continue;

            // 获取仓库信息
            WarehouseInfo warehouseInfo = warehouseInfoMapper.selectById(stock.getWarehouseId());
            if (warehouseInfo == null) continue;

            // 根据药品名称筛选
            if (drugName != null && !drugName.trim().isEmpty()) {
                if (!drugInfo.getDrugName().contains(drugName)) {
                    continue;
                }
            }

            // 根据药品编码筛选
            if (drugCode != null && !drugCode.trim().isEmpty()) {
                if (!drugInfo.getDrugCode().contains(drugCode)) {
                    continue;
                }
            }

            // 检查是否只显示预警（库存不足或库存积压）
            if (warning && drugInfo.getWarningNum() != null) {
                int availableNum = (stock.getStockNum() != null ? stock.getStockNum() : 0)
                        - (stock.getLockNum() != null ? stock.getLockNum() : 0);
                int warningNum = drugInfo.getWarningNum();
                int maxWarningNum = drugInfo.getMaxWarningNum() != null ? drugInfo.getMaxWarningNum() : 0;
                boolean lowStock = availableNum <= warningNum;
                boolean overStock = maxWarningNum > 0 && stock.getStockNum() >= maxWarningNum;
                if (!lowStock && !overStock) {
                    continue;
                }
            }

            // 构建返回数据
            Map<String, Object> stockMap = new HashMap<>();
            stockMap.put("stockId", stock.getStockId());
            stockMap.put("drugId", drugInfo.getDrugId());
            stockMap.put("warehouseId", warehouseInfo.getWarehouseId());
            stockMap.put("drugCode", drugInfo.getDrugCode());
            stockMap.put("drugName", drugInfo.getDrugName());
            stockMap.put("spec", drugInfo.getSpec());
            stockMap.put("unit", drugInfo.getUnit());
            stockMap.put("warehouseName", warehouseInfo.getWarehouseName());
            stockMap.put("warehouseStatus", warehouseInfo.getStatus());
            stockMap.put("batchNo", stock.getBatchNo() != null ? stock.getBatchNo() : "");
            stockMap.put("productionDate", stock.getProductionDate() != null ? stock.getProductionDate() : null);
            stockMap.put("expiryDate", stock.getExpiryDate() != null ? stock.getExpiryDate() : null);
            stockMap.put("stockNum", stock.getStockNum());
            stockMap.put("lockNum", stock.getLockNum() != null ? stock.getLockNum() : 0);
            stockMap.put("warningNum", drugInfo.getWarningNum() != null ? drugInfo.getWarningNum() : 0);
            stockMap.put("maxWarningNum", drugInfo.getMaxWarningNum() != null ? drugInfo.getMaxWarningNum() : 0);
            stockMap.put("lastOperateTime", stock.getUpdateTime() != null ? stock.getUpdateTime() : stock.getCreateTime());

            stockList.add(stockMap);
        }

        result.put("data", stockList);
        result.put("total", (long) stockList.size());
        return result;
    }

    /** 按药品+仓库汇总库存（totalStockNum），附带各批次明细 batchDetails。 */
    @Override
    public Map<String, Object> getGroupedStockList(int page, int size, String drugName, String drugCode, Long warehouseId, boolean warning, String sortField, String sortOrder) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> groupedList = new ArrayList<>();

        LambdaQueryWrapper<DrugStock> queryWrapper = new LambdaQueryWrapper<>();
        if (warehouseId != null) {
            queryWrapper.eq(DrugStock::getWarehouseId, warehouseId);
        }
        List<DrugStock> allStocks = drugStockMapper.selectList(queryWrapper);

        Map<String, List<DrugStock>> groupedMap = new LinkedHashMap<>();
        for (DrugStock stock : allStocks) {
            DrugInfo drugInfo = drugInfoMapper.selectById(stock.getDrugId());
            if (drugInfo == null) continue;

            if (drugName != null && !drugName.trim().isEmpty()) {
                if (!drugInfo.getDrugName().contains(drugName)) continue;
            }
            if (drugCode != null && !drugCode.trim().isEmpty()) {
                if (!drugInfo.getDrugCode().contains(drugCode)) continue;
            }

            String key = stock.getDrugId() + "_" + stock.getWarehouseId();
            groupedMap.computeIfAbsent(key, k -> new ArrayList<>()).add(stock);
        }

        for (Map.Entry<String, List<DrugStock>> entry : groupedMap.entrySet()) {
            List<DrugStock> stocks = entry.getValue();
            DrugStock first = stocks.get(0);

            DrugInfo drugInfo = drugInfoMapper.selectById(first.getDrugId());
            WarehouseInfo warehouseInfo = warehouseInfoMapper.selectById(first.getWarehouseId());
            if (drugInfo == null || warehouseInfo == null) continue;

            int totalStockNum = 0;
            int totalLockNum = 0;
            List<Map<String, Object>> batchDetails = new ArrayList<>();

            for (DrugStock stock : stocks) {
                totalStockNum += stock.getStockNum() != null ? stock.getStockNum() : 0;
                totalLockNum += stock.getLockNum() != null ? stock.getLockNum() : 0;

                Map<String, Object> batch = new HashMap<>();
                batch.put("stockId", stock.getStockId());
                batch.put("batchNo", stock.getBatchNo() != null ? stock.getBatchNo() : "");
                batch.put("productionDate", stock.getProductionDate() != null ? stock.getProductionDate() : null);
                batch.put("expiryDate", stock.getExpiryDate() != null ? stock.getExpiryDate() : null);
                batch.put("stockNum", stock.getStockNum() != null ? stock.getStockNum() : 0);
                batch.put("lockNum", stock.getLockNum() != null ? stock.getLockNum() : 0);
                batch.put("availableNum", (stock.getStockNum() != null ? stock.getStockNum() : 0) - (stock.getLockNum() != null ? stock.getLockNum() : 0));
                batchDetails.add(batch);
            }

            int warningNum = drugInfo.getWarningNum() != null ? drugInfo.getWarningNum() : 0;
            int maxWarningNum = drugInfo.getMaxWarningNum() != null ? drugInfo.getMaxWarningNum() : 0;
            int availableNum = totalStockNum - totalLockNum;

            if (warning) {
                boolean lowStock = availableNum <= warningNum;
                boolean overStock = maxWarningNum > 0 && totalStockNum >= maxWarningNum;
                if (!lowStock && !overStock) {
                    continue;
                }
            }

            Map<String, Object> groupMap = new HashMap<>();
            groupMap.put("drugId", drugInfo.getDrugId());
            groupMap.put("warehouseId", warehouseInfo.getWarehouseId());
            groupMap.put("drugCode", drugInfo.getDrugCode());
            groupMap.put("drugName", drugInfo.getDrugName());
            groupMap.put("spec", drugInfo.getSpec());
            groupMap.put("unit", drugInfo.getUnit());
            groupMap.put("warehouseName", warehouseInfo.getWarehouseName());
            groupMap.put("warehouseStatus", warehouseInfo.getStatus());
            groupMap.put("totalStockNum", totalStockNum);
            groupMap.put("totalLockNum", totalLockNum);
            groupMap.put("availableNum", totalStockNum - totalLockNum);
            groupMap.put("warningNum", warningNum);
            groupMap.put("maxWarningNum", maxWarningNum);
            groupMap.put("purchasePrice", drugInfo.getPurchasePrice());
            groupMap.put("batchCount", stocks.size());
            groupMap.put("batchDetails", batchDetails);

            groupedList.add(groupMap);
        }

        sortGroupedList(groupedList, sortField, sortOrder);

        int total = groupedList.size();
        int fromIndex = (page - 1) * size;
        int toIndex = Math.min(fromIndex + size, total);
        List<Map<String, Object>> pagedList = fromIndex < total ? groupedList.subList(fromIndex, toIndex) : new ArrayList<>();

        result.put("data", pagedList);
        result.put("total", (long) total);
        return result;
    }

    private void sortGroupedList(List<Map<String, Object>> groupedList, String sortField, String sortOrder) {
        String field = (sortField == null || sortField.isEmpty()) ? "drugCode" : sortField;
        boolean desc = "desc".equalsIgnoreCase(sortOrder);

        Comparator<Map<String, Object>> comparator;
        switch (field) {
            case "drugName":
                comparator = Comparator.comparing(m -> String.valueOf(m.get("drugName")));
                break;
            case "warehouseName":
                comparator = Comparator.comparing(m -> String.valueOf(m.get("warehouseName")));
                break;
            case "totalStockNum":
                comparator = Comparator.comparingInt(m -> toInt(m.get("totalStockNum")));
                break;
            case "availableNum":
                comparator = Comparator.comparingInt(m -> toInt(m.get("availableNum")));
                break;
            case "batchCount":
                comparator = Comparator.comparingInt(m -> toInt(m.get("batchCount")));
                break;
            case "drugCode":
            default:
                comparator = Comparator.comparing(m -> String.valueOf(m.get("drugCode")));
                break;
        }

        if (desc) {
            comparator = comparator.reversed();
        }

        if (!"warehouseName".equals(field)) {
            comparator = comparator.thenComparing(m -> String.valueOf(m.get("warehouseName")));
        }
        if (!"drugCode".equals(field)) {
            comparator = comparator.thenComparing(m -> String.valueOf(m.get("drugCode")));
        }

        groupedList.sort(comparator);
    }

    private int toInt(Object value) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return 0;
    }

    /** 为指定仓库创建盘点单（状态：盘点中），明细生成逻辑待完善。 */
    @Override
    public boolean checkStock(Long warehouseId, String range) {
        try {
            // 创建库存盘点单
            StockCheck stockCheck = new StockCheck();
            stockCheck.setWarehouseId(warehouseId);
            stockCheck.setCheckTime(LocalDateTime.now());
            stockCheck.setStatus(0); // 0: 盘点中

            // 生成盘点单号
            String checkNo = "PC" + LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            stockCheck.setCheckNo(checkNo);

            // 保存盘点单
            stockCheckMapper.insert(stockCheck);

            // TODO: 根据盘点范围生成盘点明细
            // 这里可以根据实际需求实现生成盘点明细的逻辑

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
