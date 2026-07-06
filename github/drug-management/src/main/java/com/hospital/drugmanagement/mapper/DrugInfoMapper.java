package com.hospital.drugmanagement.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hospital.drugmanagement.entity.DrugInfo;
import org.apache.ibatis.annotations.Mapper;

@Mapper // 这个注解必须有
public interface DrugInfoMapper extends BaseMapper<DrugInfo> {
}