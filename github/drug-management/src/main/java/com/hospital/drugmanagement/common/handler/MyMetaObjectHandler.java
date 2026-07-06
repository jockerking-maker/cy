package com.hospital.drugmanagement.common.handler;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.hospital.drugmanagement.common.anno.AutoFill;
import com.hospital.drugmanagement.common.constant.FillTypeEnum;
import org.apache.ibatis.reflection.MetaObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.lang.reflect.Field;
import java.time.LocalDateTime;

/**
 * MyBatis-Plus字段自动填充处理器（修复版 + 手动日志）
 */
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {
    // 手动创建日志对象（替代@Slf4j）
    private static final Logger log = LoggerFactory.getLogger(MyMetaObjectHandler.class);

    @Override
    public void insertFill(MetaObject metaObject) {
        log.info("开始插入填充...");
        fillField(metaObject, FillTypeEnum.CREATE_TIME);
        fillField(metaObject, FillTypeEnum.UPDATE_TIME);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        log.info("开始更新填充...");
        fillField(metaObject, FillTypeEnum.UPDATE_TIME);
    }

    private void fillField(MetaObject metaObject, FillTypeEnum fillType) {
        Object originalObject = metaObject.getOriginalObject();
        if (originalObject == null) {
            log.warn("原始实体对象为空，跳过自动填充");
            return;
        }
        Class<?> entityClass = originalObject.getClass();

        Field[] fields = entityClass.getDeclaredFields();
        for (Field field : fields) {
            try {
                AutoFill autoFill = field.getAnnotation(AutoFill.class);
                if (autoFill != null && autoFill.value() == fillType) {
                    field.setAccessible(true);
                    if (field.get(originalObject) == null) {
                        String fieldName = field.getName();
                        LocalDateTime now = LocalDateTime.now();
                        this.setFieldValByName(fieldName, now, metaObject);
                        log.debug("自动填充字段[{}]为：{}", fieldName, now);
                    }
                }
            } catch (IllegalAccessException e) {
                log.error("自动填充字段失败：{}", e.getMessage(), e);
            }
        }
    }
}