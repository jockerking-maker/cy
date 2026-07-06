package com.hospital.drugmanagement.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hospital.drugmanagement.entity.SysRole;
import com.hospital.drugmanagement.mapper.SysRoleMapper;
import com.hospital.drugmanagement.service.ISysRoleService;
import org.springframework.stereotype.Service;

/**
 * 系统角色基础 Service。
 * <p>
 * 对应 sys_role 表，角色的增删改查及权限分配由 {@link com.hospital.drugmanagement.controller.SysRoleController} 处理。
 */
@Service
public class SysRoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole> implements ISysRoleService {
}
