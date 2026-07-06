package com.hospital.drugmanagement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hospital.drugmanagement.entity.*;
import com.hospital.drugmanagement.mapper.*;
import com.hospital.drugmanagement.service.StockWarningService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * 库存预警业务实现。
 * <p>
 * 支持低库存、积压、临期、过期、滞销等预警类型；出入库后自动调用 {@link #checkAndCreateWarning} 检测。
 */
@Service
public class StockWarningServiceImpl extends ServiceImpl<StockWarningMapper, StockWarning> implements StockWarningService {

    @Autowired
    private DrugInfoMapper drugInfoMapper;

    @Autowired
    private DrugStockMapper drugStockMapper;

    @Autowired
    private WarehouseInfoMapper warehouseInfoMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private DrugInMapper drugInMapper;

    @Autowired
    private DrugOutMapper drugOutMapper;

    private static final Map<Integer, String> WARNING_TYPE_NAMES = new HashMap<>();
    static {
        WARNING_TYPE_NAMES.put(0, "低库存预警");
        WARNING_TYPE_NAMES.put(1, "库存积压预警");
        WARNING_TYPE_NAMES.put(2, "临期预警");
        WARNING_TYPE_NAMES.put(3, "过期预警");
        WARNING_TYPE_NAMES.put(4, "滞销预警");
    }

    private static final DateTimeFormatter WARNING_NO_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean createWarning(StockWarning stockWarning) {
        try {
            if (stockWarning.getDrugId() == null) {
                throw new IllegalArgumentException("药品 ID 不能为空");
            }
            if (stockWarning.getWarehouseId() == null) {
                throw new IllegalArgumentException("仓库 ID 不能为空");
            }

            stockWarning.setWarningNo(generateWarningNo());
            stockWarning.setCreateTime(LocalDateTime.now());

            if (stockWarning.getWarningTypeName() == null && stockWarning.getWarningType() != null) {
                stockWarning.setWarningTypeName(WARNING_TYPE_NAMES.getOrDefault(stockWarning.getWarningType(), "未知预警"));
            }

            if (stockWarning.getWarningLevel() == null) {
                stockWarning.setWarningLevel(0);
            }

            stockWarning.setHandleStatus(0);

            return save(stockWarning);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("创建预警记录失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean handleWarning(Long warningId, Long handleUserId, String handleRemark) {
        try {
            if (warningId == null) {
                throw new IllegalArgumentException("预警 ID 不能为空");
            }

            StockWarning stockWarning = getById(warningId);
            if (stockWarning == null) {
                throw new IllegalArgumentException("预警记录不存在");
            }
            if (stockWarning.getHandleStatus() == 1) {
                throw new IllegalArgumentException("该预警已处理");
            }

            Integer warningType = stockWarning.getWarningType();

            if (warningType != null && (warningType == 0 || warningType == 1)) {
                List<DrugStock> stockList = drugStockMapper.selectList(
                    new LambdaQueryWrapper<DrugStock>()
                        .eq(DrugStock::getDrugId, stockWarning.getDrugId())
                        .eq(DrugStock::getWarehouseId, stockWarning.getWarehouseId())
                );

                if (stockList != null && !stockList.isEmpty()) {
                    int totalStock = stockList.stream().mapToInt(s -> s.getStockNum() != null ? s.getStockNum() : 0).sum();
                    int totalLock = stockList.stream().mapToInt(s -> s.getLockNum() != null ? s.getLockNum() : 0).sum();
                    int availableNum = totalStock - totalLock;

                    DrugInfo drugInfo = drugInfoMapper.selectById(stockWarning.getDrugId());

                    if (warningType == 0 && drugInfo != null && drugInfo.getWarningNum() != null) {
                        if (availableNum < drugInfo.getWarningNum() && (handleRemark == null || handleRemark.trim().isEmpty())) {
                            throw new IllegalArgumentException("当前可用库存（" + availableNum + "）仍低于最低预警值（" + drugInfo.getWarningNum() + "），请填写处理说明后提交");
                        }
                    } else if (warningType == 1 && drugInfo != null && drugInfo.getMaxWarningNum() != null) {
                        if (availableNum > drugInfo.getMaxWarningNum() && (handleRemark == null || handleRemark.trim().isEmpty())) {
                            throw new IllegalArgumentException("当前可用库存（" + availableNum + "）仍高于最高预警值（" + drugInfo.getMaxWarningNum() + "），请填写处理说明后提交");
                        }
                    }
                }
            }

            if (warningType != null && (warningType == 2 || warningType == 3 || warningType == 4)
                    && (handleRemark == null || handleRemark.trim().isEmpty())) {
                throw new IllegalArgumentException("请填写处置说明（如：已下架、已报损出库、已优先出库等）");
            }

            stockWarning.setHandleStatus(1);
            stockWarning.setHandleUserId(handleUserId);
            stockWarning.setHandleTime(LocalDateTime.now());
            stockWarning.setHandleRemark(handleRemark);
            updateById(stockWarning);

            return true;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("处理预警失败：" + e.getMessage());
        }
    }

    /**
     * 出入库后自动检测：根据库存量与有效期生成或更新预警记录。
     */
    @Override
    public void checkAndCreateWarning(Long drugId, Long warehouseId) {
        try {
            DrugInfo drugInfo = drugInfoMapper.selectById(drugId);
            if (drugInfo == null) {
                return;
            }

            LambdaQueryWrapper<DrugStock> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(DrugStock::getDrugId, drugId)
                    .eq(DrugStock::getWarehouseId, warehouseId);
            List<DrugStock> stockList = drugStockMapper.selectList(queryWrapper);

            if (stockList == null || stockList.isEmpty()) {
                return;
            }

            int availableNum = stockList.stream()
                    .mapToInt(s -> {
                        int num = s.getStockNum() != null ? s.getStockNum() : 0;
                        int lock = s.getLockNum() != null ? s.getLockNum() : 0;
                        return num - lock;
                    })
                    .sum();
            Integer minWarningNum = drugInfo.getWarningNum();
            Integer maxWarningNum = drugInfo.getMaxWarningNum();

            if (minWarningNum != null && availableNum <= minWarningNum) {
                LambdaQueryWrapper<StockWarning> warningQuery = new LambdaQueryWrapper<>();
                warningQuery.eq(StockWarning::getDrugId, drugId)
                        .eq(StockWarning::getWarehouseId, warehouseId)
                        .eq(StockWarning::getWarningType, 0)
                        .eq(StockWarning::getHandleStatus, 0)
                        .apply("DATE(create_time) = CURDATE()");

                long count = count(warningQuery);
                if (count == 0) {
                    StockWarning stockWarning = new StockWarning();
                    stockWarning.setDrugId(drugId);
                    stockWarning.setWarehouseId(warehouseId);
                    stockWarning.setStockNum(availableNum);
                    stockWarning.setWarningType(0);
                    stockWarning.setMinWarningNum(minWarningNum);
                    stockWarning.setMaxWarningNum(maxWarningNum);
                    stockWarning.setWarningTypeName("低库存预警");

                    if (availableNum == 0) {
                        stockWarning.setWarningLevel(2);
                    } else if (availableNum < minWarningNum * 0.5) {
                        stockWarning.setWarningLevel(1);
                    } else {
                        stockWarning.setWarningLevel(0);
                    }

                    stockWarning.setSuggestion("建议立即采购补货，当前库存仅剩" + availableNum + "，低于预警值" + minWarningNum);
                    createWarning(stockWarning);
                }
            }

            if (maxWarningNum != null && availableNum >= maxWarningNum) {
                LambdaQueryWrapper<StockWarning> warningQuery = new LambdaQueryWrapper<>();
                warningQuery.eq(StockWarning::getDrugId, drugId)
                        .eq(StockWarning::getWarehouseId, warehouseId)
                        .eq(StockWarning::getWarningType, 1)
                        .eq(StockWarning::getHandleStatus, 0)
                        .apply("DATE(create_time) = CURDATE()");

                long count = count(warningQuery);
                if (count == 0) {
                    StockWarning stockWarning = new StockWarning();
                    stockWarning.setDrugId(drugId);
                    stockWarning.setWarehouseId(warehouseId);
                    stockWarning.setStockNum(availableNum);
                    stockWarning.setWarningType(1);
                    stockWarning.setMinWarningNum(minWarningNum);
                    stockWarning.setMaxWarningNum(maxWarningNum);
                    stockWarning.setWarningTypeName("库存积压预警");
                    stockWarning.setWarningLevel(1);
                    stockWarning.setSuggestion("建议减少采购或促销出库，当前库存" + availableNum + "超过上限" + maxWarningNum);
                    createWarning(stockWarning);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> checkAndCreateNearExpiryWarning() {
        Map<String, Object> summary = new HashMap<>();
        int checked = 0;
        int created = 0;
        int skipped = 0;
        try {
            LocalDate today = LocalDate.now();
            LocalDate thresholdDate = today.plusDays(180);

            LambdaQueryWrapper<DrugStock> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.isNotNull(DrugStock::getExpiryDate)
                    .gt(DrugStock::getStockNum, 0)
                    .apply("DATE(expiry_date) <= {0}", thresholdDate);
            List<DrugStock> stockList = drugStockMapper.selectList(queryWrapper);

            for (DrugStock stock : stockList) {
                checked++;
                LocalDate expiryDate = stock.getExpiryDate().toLocalDate();
                long daysToExpiry = ChronoUnit.DAYS.between(today, expiryDate);

                int warningType;
                int warningLevel;

                if (daysToExpiry <= 0) {
                    warningType = 3;
                    warningLevel = 2;
                } else if (daysToExpiry <= 30) {
                    warningType = 2;
                    warningLevel = 2;
                } else if (daysToExpiry <= 90) {
                    warningType = 2;
                    warningLevel = 1;
                } else {
                    warningType = 2;
                    warningLevel = 0;
                }

                if (shouldSkipNearExpiryWarning(stock, warningType, warningLevel)) {
                    skipped++;
                    continue;
                }

                StockWarning stockWarning = new StockWarning();
                stockWarning.setDrugId(stock.getDrugId());
                stockWarning.setWarehouseId(stock.getWarehouseId());
                stockWarning.setBatchNo(stock.getBatchNo());
                stockWarning.setStockNum(stock.getStockNum());
                stockWarning.setWarningType(warningType);
                stockWarning.setWarningTypeName(warningType == 3 ? "过期预警" : "临期预警");
                stockWarning.setWarningLevel(warningLevel);
                stockWarning.setDaysToExpiry((int) daysToExpiry);

                if (warningType == 3) {
                    stockWarning.setSuggestion("批次[" + formatBatchNo(stock.getBatchNo()) + "]已过期"
                            + Math.abs(daysToExpiry) + "天，建议立即下架处理");
                } else {
                    stockWarning.setSuggestion("批次[" + formatBatchNo(stock.getBatchNo()) + "]距过期仅剩"
                            + daysToExpiry + "天，建议优先出库");
                }

                createWarning(stockWarning);
                created++;
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("临期预警检查失败：" + e.getMessage(), e);
        }
        summary.put("checked", checked);
        summary.put("created", created);
        summary.put("skipped", skipped);
        return summary;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> checkAndCreateSlowMovingWarning() {
        Map<String, Object> summary = new HashMap<>();
        int checked = 0;
        int created = 0;
        int skipped = 0;
        try {
            LocalDateTime threshold = LocalDateTime.now().minusDays(90);

            LambdaQueryWrapper<DrugStock> stockQuery = new LambdaQueryWrapper<>();
            stockQuery.gt(DrugStock::getStockNum, 0);
            List<DrugStock> allStocks = drugStockMapper.selectList(stockQuery);

            for (DrugStock stock : allStocks) {
                checked++;
                if (hasRecentInOutActivity(stock, threshold)) {
                    skipped++;
                    continue;
                }

                if (shouldSkipSlowMovingWarning(stock)) {
                    skipped++;
                    continue;
                }

                StockWarning stockWarning = new StockWarning();
                stockWarning.setDrugId(stock.getDrugId());
                stockWarning.setWarehouseId(stock.getWarehouseId());
                stockWarning.setBatchNo(stock.getBatchNo());
                stockWarning.setStockNum(stock.getStockNum());
                stockWarning.setWarningType(4);
                stockWarning.setWarningTypeName("滞销预警");
                stockWarning.setWarningLevel(0);
                stockWarning.setSuggestion("批次[" + formatBatchNo(stock.getBatchNo())
                        + "]已90天无出入库记录，建议评估是否继续采购");
                createWarning(stockWarning);
                created++;
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("滞销预警检查失败：" + e.getMessage(), e);
        }
        summary.put("checked", checked);
        summary.put("created", created);
        summary.put("skipped", skipped);
        return summary;
    }

    private String generateWarningNo() {
        return "YJ" + LocalDateTime.now().format(WARNING_NO_FORMATTER)
                + ThreadLocalRandom.current().nextInt(100, 1000);
    }

    private boolean hasUnhandledWarning(Long drugId, Long warehouseId, String batchNo, int warningType) {
        LambdaQueryWrapper<StockWarning> warningQuery = new LambdaQueryWrapper<>();
        warningQuery.eq(StockWarning::getDrugId, drugId)
                .eq(StockWarning::getWarehouseId, warehouseId)
                .eq(StockWarning::getWarningType, warningType)
                .eq(StockWarning::getHandleStatus, 0);
        applyBatchNoCondition(warningQuery, batchNo);
        return count(warningQuery) > 0;
    }

    private void applyBatchNoCondition(LambdaQueryWrapper<StockWarning> warningQuery, String batchNo) {
        if (StringUtils.hasText(batchNo)) {
            warningQuery.eq(StockWarning::getBatchNo, batchNo);
        } else {
            warningQuery.and(wrapper -> wrapper.isNull(StockWarning::getBatchNo)
                    .or()
                    .eq(StockWarning::getBatchNo, ""));
        }
    }

    private StockWarning findLatestHandledWarning(Long drugId, Long warehouseId, String batchNo, int warningType) {
        LambdaQueryWrapper<StockWarning> warningQuery = new LambdaQueryWrapper<>();
        warningQuery.eq(StockWarning::getDrugId, drugId)
                .eq(StockWarning::getWarehouseId, warehouseId)
                .eq(StockWarning::getWarningType, warningType)
                .eq(StockWarning::getHandleStatus, 1)
                .orderByDesc(StockWarning::getHandleTime)
                .orderByDesc(StockWarning::getCreateTime)
                .last("LIMIT 1");
        applyBatchNoCondition(warningQuery, batchNo);
        return getOne(warningQuery, false);
    }

    /**
     * 临期/过期预警：已有未处理记录则跳过；已处理且紧急程度未升高则不再重复生成。
     */
    private boolean shouldSkipNearExpiryWarning(DrugStock stock, int warningType, int warningLevel) {
        if (hasUnhandledWarning(stock.getDrugId(), stock.getWarehouseId(), stock.getBatchNo(), warningType)) {
            return true;
        }

        if (warningType == 3) {
            return findLatestHandledWarning(stock.getDrugId(), stock.getWarehouseId(), stock.getBatchNo(), 3) != null;
        }

        StockWarning handledNearExpiry = findLatestHandledWarning(
                stock.getDrugId(), stock.getWarehouseId(), stock.getBatchNo(), 2);
        if (handledNearExpiry == null) {
            return false;
        }

        int handledLevel = handledNearExpiry.getWarningLevel() != null ? handledNearExpiry.getWarningLevel() : 0;
        return warningLevel <= handledLevel;
    }

    /**
     * 滞销预警：已有未处理记录则跳过；已处理后若无新的出入库活动则不再重复生成。
     */
    private boolean shouldSkipSlowMovingWarning(DrugStock stock) {
        if (hasUnhandledWarning(stock.getDrugId(), stock.getWarehouseId(), stock.getBatchNo(), 4)) {
            return true;
        }

        StockWarning handledWarning = findLatestHandledWarning(
                stock.getDrugId(), stock.getWarehouseId(), stock.getBatchNo(), 4);
        if (handledWarning == null) {
            return false;
        }

        LocalDateTime handleTime = handledWarning.getHandleTime() != null
                ? handledWarning.getHandleTime()
                : handledWarning.getCreateTime();
        if (handleTime == null) {
            return true;
        }

        return !hasInOutActivitySince(stock, handleTime);
    }

    private boolean hasInOutActivitySince(DrugStock stock, LocalDateTime since) {
        LambdaQueryWrapper<DrugIn> inQuery = new LambdaQueryWrapper<>();
        inQuery.eq(DrugIn::getDrugId, stock.getDrugId())
                .eq(DrugIn::getWarehouseId, stock.getWarehouseId())
                .apply("(COALESCE(in_date, DATE(create_time)) >= {0})", since.toLocalDate());
        if (StringUtils.hasText(stock.getBatchNo())) {
            inQuery.eq(DrugIn::getBatchNo, stock.getBatchNo());
        }
        if (drugInMapper.selectCount(inQuery) > 0) {
            return true;
        }

        LambdaQueryWrapper<DrugOut> outQuery = new LambdaQueryWrapper<>();
        outQuery.eq(DrugOut::getDrugId, stock.getDrugId())
                .eq(DrugOut::getWarehouseId, stock.getWarehouseId())
                .apply("(COALESCE(out_date, create_time) >= {0})", since);
        if (StringUtils.hasText(stock.getBatchNo())) {
            outQuery.eq(DrugOut::getBatchNo, stock.getBatchNo());
        }
        return drugOutMapper.selectCount(outQuery) > 0;
    }

    private boolean hasRecentInOutActivity(DrugStock stock, LocalDateTime threshold) {
        LambdaQueryWrapper<DrugIn> inQuery = new LambdaQueryWrapper<>();
        inQuery.eq(DrugIn::getDrugId, stock.getDrugId())
                .eq(DrugIn::getWarehouseId, stock.getWarehouseId())
                .apply("(COALESCE(in_date, DATE(create_time)) >= {0})", threshold.toLocalDate());
        if (StringUtils.hasText(stock.getBatchNo())) {
            inQuery.eq(DrugIn::getBatchNo, stock.getBatchNo());
        }
        if (drugInMapper.selectCount(inQuery) > 0) {
            return true;
        }

        LambdaQueryWrapper<DrugOut> outQuery = new LambdaQueryWrapper<>();
        outQuery.eq(DrugOut::getDrugId, stock.getDrugId())
                .eq(DrugOut::getWarehouseId, stock.getWarehouseId())
                .apply("(COALESCE(out_date, create_time) >= {0})", threshold);
        if (StringUtils.hasText(stock.getBatchNo())) {
            outQuery.eq(DrugOut::getBatchNo, stock.getBatchNo());
        }
        return drugOutMapper.selectCount(outQuery) > 0;
    }

    private String formatBatchNo(String batchNo) {
        return StringUtils.hasText(batchNo) ? batchNo : "未标注";
    }

    @Override
    public Map<String, Object> getWarningStats() {
        Map<String, Object> result = new HashMap<>();
        try {
            LambdaQueryWrapper<StockWarning> unhandledQuery = new LambdaQueryWrapper<>();
            unhandledQuery.eq(StockWarning::getHandleStatus, 0);
            result.put("unhandled", count(unhandledQuery));

            LambdaQueryWrapper<StockWarning> lowStockQuery = new LambdaQueryWrapper<>();
            lowStockQuery.eq(StockWarning::getWarningType, 0);
            result.put("lowStock", count(lowStockQuery));

            LambdaQueryWrapper<StockWarning> nearExpiryQuery = new LambdaQueryWrapper<>();
            nearExpiryQuery.in(StockWarning::getWarningType, 2, 3);
            result.put("nearExpiry", count(nearExpiryQuery));

            LambdaQueryWrapper<StockWarning> overstockQuery = new LambdaQueryWrapper<>();
            overstockQuery.eq(StockWarning::getWarningType, 1);
            result.put("overstock", count(overstockQuery));

            LambdaQueryWrapper<StockWarning> slowMovingQuery = new LambdaQueryWrapper<>();
            slowMovingQuery.eq(StockWarning::getWarningType, 4);
            result.put("slowMoving", count(slowMovingQuery));

            result.put("totalCount", count());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    @Override
    public Map<String, Object> getWarningTrend(int months) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<String> monthList = new ArrayList<>();
            List<Long> lowStockList = new ArrayList<>();
            List<Long> overstockList = new ArrayList<>();
            List<Long> nearExpiryList = new ArrayList<>();
            List<Long> expiredList = new ArrayList<>();
            List<Long> slowMovingList = new ArrayList<>();

            LocalDate today = LocalDate.now();

            for (int i = months - 1; i >= 0; i--) {
                LocalDate monthStart = today.minusMonths(i).withDayOfMonth(1);
                LocalDate monthEnd = monthStart.plusMonths(1);
                String monthLabel = monthStart.getYear() + "-" + String.format("%02d", monthStart.getMonthValue());
                monthList.add(monthLabel);

                LocalDateTime start = monthStart.atStartOfDay();
                LocalDateTime end = monthEnd.atStartOfDay();

                long lowCount = countWarningsByTypeAndMonth(0, start, end);
                long overCount = countWarningsByTypeAndMonth(1, start, end);
                long slowCount = countWarningsByTypeAndMonth(4, start, end);
                long nearFromWarning = countWarningsByTypeAndMonth(2, start, end);
                long expiredFromWarning = countWarningsByTypeAndMonth(3, start, end);

                long nearFromInventory = countNearExpiryInMonth(monthStart, monthEnd, today);
                long expiredFromInventory = countExpiredInMonth(monthStart, monthEnd, today);

                lowStockList.add(lowCount);
                overstockList.add(overCount);
                nearExpiryList.add(Math.max(nearFromWarning, nearFromInventory));
                expiredList.add(Math.max(expiredFromWarning, expiredFromInventory));
                slowMovingList.add(slowCount);
            }

            result.put("months", monthList);
            result.put("lowStock", lowStockList);
            result.put("overstock", overstockList);
            result.put("nearExpiry", nearExpiryList);
            result.put("expired", expiredList);
            result.put("slowMoving", slowMovingList);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    private long countWarningsByTypeAndMonth(int warningType, LocalDateTime start, LocalDateTime end) {
        LambdaQueryWrapper<StockWarning> query = new LambdaQueryWrapper<>();
        query.ge(StockWarning::getCreateTime, start)
                .lt(StockWarning::getCreateTime, end)
                .eq(StockWarning::getWarningType, warningType);
        return count(query);
    }

    private long countNearExpiryInMonth(LocalDate monthStart, LocalDate monthEnd, LocalDate today) {
        LocalDate nearThreshold = today.plusDays(180);
        LambdaQueryWrapper<DrugStock> query = new LambdaQueryWrapper<>();
        query.gt(DrugStock::getStockNum, 0)
                .isNotNull(DrugStock::getExpiryDate)
                .apply("DATE(expiry_date) >= {0}", monthStart)
                .apply("DATE(expiry_date) < {0}", monthEnd)
                .apply("DATE(expiry_date) > {0}", today)
                .apply("DATE(expiry_date) <= {0}", nearThreshold);
        return drugStockMapper.selectCount(query);
    }

    private long countExpiredInMonth(LocalDate monthStart, LocalDate monthEnd, LocalDate today) {
        LocalDate effectiveEnd = monthEnd.isBefore(today.plusDays(1)) ? monthEnd : today.plusDays(1);
        if (!monthStart.isBefore(effectiveEnd)) {
            return 0;
        }
        LambdaQueryWrapper<DrugStock> query = new LambdaQueryWrapper<>();
        query.gt(DrugStock::getStockNum, 0)
                .isNotNull(DrugStock::getExpiryDate)
                .apply("DATE(expiry_date) >= {0}", monthStart)
                .apply("DATE(expiry_date) < {0}", effectiveEnd)
                .apply("DATE(expiry_date) <= {0}", today);
        return drugStockMapper.selectCount(query);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean assignWarning(Long warningId, Long assignUserId) {
        try {
            StockWarning stockWarning = getById(warningId);
            if (stockWarning == null) {
                throw new IllegalArgumentException("预警记录不存在");
            }

            stockWarning.setAssignUserId(assignUserId);
            stockWarning.setAssignTime(LocalDateTime.now());
            updateById(stockWarning);
            return true;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("指派预警失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> batchHandleWarnings(List<Long> warningIds, Long handleUserId, String handleRemark) {
        int processed = 0;
        int skipped = 0;
        List<String> errors = new java.util.ArrayList<>();

        try {
            for (Long warningId : warningIds) {
                StockWarning stockWarning = getById(warningId);
                if (stockWarning == null || stockWarning.getHandleStatus() == 1) {
                    skipped++;
                    continue;
                }
                try {
                    handleWarning(warningId, handleUserId, handleRemark);
                    processed++;
                } catch (IllegalArgumentException e) {
                    String warningNo = stockWarning.getWarningNo() != null ? stockWarning.getWarningNo() : String.valueOf(warningId);
                    errors.add(warningNo + "：" + e.getMessage());
                }
            }

            if (processed == 0 && errors.isEmpty() && skipped > 0) {
                throw new IllegalArgumentException("所选预警均已处理，无需重复操作");
            }
            if (processed == 0 && !errors.isEmpty()) {
                throw new IllegalArgumentException(String.join("；", errors));
            }

            Map<String, Object> summary = new HashMap<>();
            summary.put("processed", processed);
            summary.put("skipped", skipped);
            summary.put("failed", errors.size());
            if (!errors.isEmpty()) {
                summary.put("errors", errors);
            }
            return summary;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("批量处理预警失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> batchDeleteHandledWarnings(List<Long> warningIds) {
        int deleted = 0;
        int skipped = 0;

        for (Long warningId : warningIds) {
            StockWarning stockWarning = getById(warningId);
            if (stockWarning == null) {
                skipped++;
                continue;
            }
            if (stockWarning.getHandleStatus() == null || stockWarning.getHandleStatus() != 1) {
                skipped++;
                continue;
            }
            removeById(warningId);
            deleted++;
        }

        if (deleted == 0) {
            throw new IllegalArgumentException("没有可删除的记录，仅可删除已处理的预警");
        }

        Map<String, Object> summary = new HashMap<>();
        summary.put("deleted", deleted);
        summary.put("skipped", skipped);
        return summary;
    }

    @Override
    public Page<StockWarning> pageList(Map<String, Object> params) {
        int page = params.get("page") != null ? Integer.parseInt(params.get("page").toString()) : 1;
        int size = params.get("size") != null ? Integer.parseInt(params.get("size").toString()) : 10;

        Page<StockWarning> stockWarningPage = new Page<>(page, size);
        LambdaQueryWrapper<StockWarning> queryWrapper = new LambdaQueryWrapper<>();

        if (params.get("drugId") != null) {
            queryWrapper.eq(StockWarning::getDrugId, Long.parseLong(params.get("drugId").toString()));
        }
        if (params.get("warehouseId") != null) {
            queryWrapper.eq(StockWarning::getWarehouseId, Long.parseLong(params.get("warehouseId").toString()));
        }
        if (params.get("handleStatus") != null) {
            queryWrapper.eq(StockWarning::getHandleStatus, Integer.parseInt(params.get("handleStatus").toString()));
        }
        if (params.get("warningType") != null) {
            queryWrapper.eq(StockWarning::getWarningType, Integer.parseInt(params.get("warningType").toString()));
        }
        if (params.get("warningLevel") != null) {
            queryWrapper.eq(StockWarning::getWarningLevel, Integer.parseInt(params.get("warningLevel").toString()));
        }

        queryWrapper.orderByDesc(StockWarning::getCreateTime);

        Page<StockWarning> resultPage = page(stockWarningPage, queryWrapper);

        for (StockWarning warning : resultPage.getRecords()) {
            DrugInfo drugInfo = drugInfoMapper.selectById(warning.getDrugId());
            if (drugInfo != null) {
                warning.setDrugName(drugInfo.getDrugName());
                warning.setDrugCode(drugInfo.getDrugCode());
            }

            WarehouseInfo warehouseInfo = warehouseInfoMapper.selectById(warning.getWarehouseId());
            if (warehouseInfo != null) {
                warning.setWarehouseName(warehouseInfo.getWarehouseName());
            }

            if (warning.getWarningTypeName() == null && warning.getWarningType() != null) {
                warning.setWarningTypeName(WARNING_TYPE_NAMES.getOrDefault(warning.getWarningType(), "未知预警"));
            }

            if (warning.getSuggestion() == null && warning.getWarningType() != null) {
                int availableNum = warning.getStockNum() != null ? warning.getStockNum() : 0;
                String batchLabel = StringUtils.hasText(warning.getBatchNo()) ? warning.getBatchNo() : "未标注";
                switch (warning.getWarningType()) {
                    case 0:
                        warning.setSuggestion("建议立即采购补货，当前库存仅剩" + availableNum + "，低于预警值" + (warning.getMinWarningNum() != null ? warning.getMinWarningNum() : "未知"));
                        break;
                    case 1:
                        warning.setSuggestion("建议减少采购或促销出库，当前库存" + availableNum + "超过上限" + (warning.getMaxWarningNum() != null ? warning.getMaxWarningNum() : "未知"));
                        break;
                    case 2:
                        warning.setSuggestion("批次[" + batchLabel + "]距过期仅剩" + (warning.getDaysToExpiry() != null ? warning.getDaysToExpiry() : "未知") + "天，建议优先出库");
                        break;
                    case 3:
                        warning.setSuggestion("批次[" + batchLabel + "]已过期，建议立即下架处理");
                        break;
                    case 4:
                        warning.setSuggestion("批次[" + batchLabel + "]已90天无出入库记录，建议评估是否继续采购");
                        break;
                }
            }

            if (warning.getHandleUserId() != null) {
                SysUser handleUser = sysUserMapper.selectById(warning.getHandleUserId());
                if (handleUser != null) {
                    warning.setHandleUserName(handleUser.getRealName());
                }
            }

            if (warning.getAssignUserId() != null) {
                SysUser assignUser = sysUserMapper.selectById(warning.getAssignUserId());
                if (assignUser != null) {
                    warning.setAssignUserName(assignUser.getRealName());
                }
            }
        }

        return resultPage;
    }
}
