package com.hospital.drugmanagement.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.hospital.drugmanagement.entity.StockWarning;

import java.util.List;
import java.util.Map;

public interface StockWarningService extends IService<StockWarning> {

    boolean createWarning(StockWarning stockWarning);

    boolean handleWarning(Long warningId, Long handleUserId, String handleRemark);

    void checkAndCreateWarning(Long drugId, Long warehouseId);

    Page<StockWarning> pageList(Map<String, Object> params);

    Map<String, Object> checkAndCreateNearExpiryWarning();

    Map<String, Object> checkAndCreateSlowMovingWarning();

    Map<String, Object> getWarningStats();

    Map<String, Object> getWarningTrend(int months);

    boolean assignWarning(Long warningId, Long assignUserId);

    Map<String, Object> batchHandleWarnings(List<Long> warningIds, Long handleUserId, String handleRemark);

    Map<String, Object> batchDeleteHandledWarnings(List<Long> warningIds);
}
