package com.hospital.drugmanagement.dto;

import com.hospital.drugmanagement.entity.SysMenu;
import lombok.Data;

import java.util.List;

/**
 * 用户登录响应 DTO
 */
@Data
public class LoginResponse {
    /**
     * JWT Token
     */
    private String token;
    
    /**
     * 用户信息
     */
    private UserInfo userInfo;
    
    /**
     * 角色列表
     */
    private List<String> roles;
    
    /**
     * 菜单列表
     */
    private List<SysMenu> menus;
    
    // 手动添加 getter 和 setter 方法（避免 Lombok 未生效）
    public String getToken() {
        return token;
    }
    
    public void setToken(String token) {
        this.token = token;
    }
    
    public UserInfo getUserInfo() {
        return userInfo;
    }
    
    public void setUserInfo(UserInfo userInfo) {
        this.userInfo = userInfo;
    }
    
    public List<String> getRoles() {
        return roles;
    }
    
    public void setRoles(List<String> roles) {
        this.roles = roles;
    }
    
    public List<SysMenu> getMenus() {
        return menus;
    }
    
    public void setMenus(List<SysMenu> menus) {
        this.menus = menus;
    }
    
    /**
     * 用户信息内部类
     */
    @Data
    public static class UserInfo {
        private Long userId;
        private String username;
        private String realName;
        private String phone;
        private String email;
        private Integer status;
        
        // 手动添加 getter 和 setter 方法
        public Long getUserId() {
            return userId;
        }
        
        public void setUserId(Long userId) {
            this.userId = userId;
        }
        
        public String getUsername() {
            return username;
        }
        
        public void setUsername(String username) {
            this.username = username;
        }
        
        public String getRealName() {
            return realName;
        }
        
        public void setRealName(String realName) {
            this.realName = realName;
        }
        
        public String getPhone() {
            return phone;
        }
        
        public void setPhone(String phone) {
            this.phone = phone;
        }
        
        public String getEmail() {
            return email;
        }
        
        public void setEmail(String email) {
            this.email = email;
        }
        
        public Integer getStatus() {
            return status;
        }
        
        public void setStatus(Integer status) {
            this.status = status;
        }
    }
}
