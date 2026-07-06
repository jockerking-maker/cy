package com.hospital.drugmanagement.service;

import com.baomidou.mybatisplus.extension.service.IService; // 必须导入这个
import com.hospital.drugmanagement.entity.DrugInfo;

import java.util.Map;

/**
 * 药品信息Service接口
 * 继承MyBatis-Plus的IService，自动获得所有基础CRUD方法（无需手动定义）
 */
public interface DrugInfoService extends IService<DrugInfo> {

    /**
     * 生成下一个药品编码与批准文号
     */
    Map<String, String> generateNextCodes(String drugType);
}