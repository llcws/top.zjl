package com.crm.service;

import com.crm.common.result.PageResult;
import com.crm.entity.Department;
import com.baomidou.mybatisplus.extension.service.IService;
import com.crm.query.DepartmentQuery;
import com.crm.query.IdQuery;

import java.rmi.ServerException;
import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author crm
 * @since 2025-10-12
 */
public interface DepartmentService extends IService<Department> {

    /**
     * 获取部门的分页列表
     *
     * @param query 查询参数
     * @return 部门分页列表
     */
    PageResult<Department> getPage(DepartmentQuery query);

    /**
     * 部门列表 - 不分页
     *
     * @return
     */
    List<Department> getList();

    /**
     * 保存或编辑部门
     * @param department
     */
    void saveOrEditDepartment(Department department) throws ServerException;

    /**
     * 删除部门
     * @param query
     */
    void removeDepartment(IdQuery query) throws ServerException;
}