package com.hospital.drugmanagement.common.anno;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口级 RBAC 注解：标注在 Controller 类或方法上，限制可访问的角色编码。
 * <p>
 * 由 {@link com.hospital.drugmanagement.interceptor.AuthInterceptor} 在 JWT 校验通过后进一步校验。
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireRole {
    String[] value() default {};
}
