package com.hospital.drugmanagement.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hospital.drugmanagement.dto.LoginResponse;
import com.hospital.drugmanagement.entity.SysUser;

/**
 * 系统用户 Service 接口
 */
public interface ISysUserService extends IService<SysUser> {
    
    /**
     * 用户登录
     * @param username 用户名
     * @param password 密码
     * @return 登录响应信息
     */
    LoginResponse login(String username, String password);
    
    /**
     * 获取当前用户信息
     * @param userId 用户 ID
     * @return 用户信息
     */
    LoginResponse.UserInfo getCurrentUserInfo(Long userId);
}