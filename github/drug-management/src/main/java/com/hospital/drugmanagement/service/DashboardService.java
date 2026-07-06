package com.hospital.drugmanagement.service;

import java.util.Map;

public interface DashboardService {

    Map<String, Object> getStats();

    Map<String, Object> getDrugTypeStats();

    Map<String, Object> getStockWarningStats();
}
