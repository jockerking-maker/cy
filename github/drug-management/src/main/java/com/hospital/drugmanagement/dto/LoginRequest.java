package com.hospital.drugmanagement.dto;

import lombok.Data;

/**
 * 用户登录请求 DTO
 */
@Data
public class LoginRequest {
    /**
     * 用户名
     */
    private String username;
    
    /**
     * 密码
     */
    private String password;
    
    // 手动添加 getter 和 setter 方法（避免 Lombok 未生效）
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
}
