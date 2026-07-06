package com.hospital.drugmanagement.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hospital.drugmanagement.entity.SupplierInfo;
import com.hospital.drugmanagement.mapper.SupplierInfoMapper;
import com.hospital.drugmanagement.service.ISupplierInfoService;
import org.springframework.stereotype.Service;

/**
 * 供应商信息基础 Service。
 * <p>
 * 继承 MyBatis-Plus 通用 CRUD，供应商列表与校验逻辑主要在 {@link com.hospital.drugmanagement.controller.SupplierInfoController} 中。
 */
@Service
public class SupplierInfoServiceImpl extends ServiceImpl<SupplierInfoMapper, SupplierInfo> implements ISupplierInfoService {
}