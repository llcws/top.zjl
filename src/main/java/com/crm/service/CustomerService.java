package com.crm.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.crm.common.result.PageResult;
import com.crm.entity.Customer;
import com.baomidou.mybatisplus.extension.service.IService;
import com.crm.query.CustomerQuery;
import com.crm.query.CustomerTrendQuery;
import com.crm.query.IdQuery;
import com.crm.vo.CustomerVO;
import jakarta.servlet.http.HttpServletResponse;

import java.net.http.HttpResponse;
import java.rmi.ServerException;
import java.util.List;
import java.util.Map;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author crm
 * @since 2025-10-12
 */
public interface CustomerService extends IService<Customer> {
    //分页
    PageResult<CustomerVO> getPage(CustomerQuery query);
    //导出
    void exportCustomer(CustomerQuery query, HttpServletResponse response);
    //新增或修改
    void saveOrUpdate(CustomerVO customerVO) throws ServerException;
    //删除客户信息
    void removeCustomer(List<Integer> ids);
    //客户转入公海
    void customerToPublicPool(IdQuery idQuery) throws ServerException;
    //领取客户
    void publicPoolToPrivate(IdQuery idQuery) throws ServerException;

    Map<String, List> getCustomerTrend(CustomerTrendQuery query);
}