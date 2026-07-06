package com.hospital.drugmanagement.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;

/**
 * 密码加密与校验工具。
 * <p>
 * 新用户使用 BCrypt 存储；历史数据仍兼容 MD5 加盐（drug_management_salt）明文比对。
 */
@Component
public class PasswordUtil {

    private static final String LEGACY_SALT = "drug_management_salt";

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public String encode(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    public boolean matches(String rawPassword, String storedPassword) {
        if (storedPassword == null || storedPassword.isEmpty()) {
            return false;
        }
        if (storedPassword.startsWith("$2a$") || storedPassword.startsWith("$2b$")) {
            return encoder.matches(rawPassword, storedPassword);
        }
        String legacyHash = DigestUtils.md5DigestAsHex((rawPassword + LEGACY_SALT).getBytes(StandardCharsets.UTF_8));
        return storedPassword.equals(legacyHash) || storedPassword.equals(rawPassword);
    }

    public boolean isLegacyPassword(String storedPassword) {
        return storedPassword != null
                && !storedPassword.startsWith("$2a$")
                && !storedPassword.startsWith("$2b$");
    }
}
