package com.hospital.drugmanagement.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hospital.drugmanagement.entity.DrugOut;

import java.util.Map;

public interface IDrugOutService extends IService<DrugOut> {
    Map<String, Object> getDrugOutList(int page, int size, String outNo, String drugName, String outType);
    boolean saveDrugOut(DrugOut drugOut);
}