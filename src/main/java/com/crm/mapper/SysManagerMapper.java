package com.crm.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.crm.entity.SysManager;
import com.crm.query.SysManagerQuery;
import com.crm.vo.SysManagerVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * 管理员表 Mapper 接口
 * </p>
 */
public interface SysManagerMapper extends BaseMapper<SysManager> {

    default SysManager getByAccount(String account) {
        return this.selectOne(new LambdaQueryWrapper<SysManager>().eq(SysManager::getAccount, account));
    }

    List<SysManagerVO> getManagerPage(Page<SysManagerVO> page, @Param("query") SysManagerQuery query);

    /**
     * 根据ID查询管理员邮箱（用于邮件通知）
     */
    default String getEmailById(Integer managerId) {
        SysManager manager = this.selectById(managerId);
        return manager != null ? manager.getEmail() : null;
    }
}