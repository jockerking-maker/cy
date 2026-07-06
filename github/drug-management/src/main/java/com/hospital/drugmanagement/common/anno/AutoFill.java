package com.hospital.drugmanagement.common.anno;

import com.hospital.drugmanagement.common.constant.FillTypeEnum;
import java.lang.annotation.*;

/**
 * 自定义字段自动填充注解
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AutoFill {
    // 填充类型
    FillTypeEnum value();
}