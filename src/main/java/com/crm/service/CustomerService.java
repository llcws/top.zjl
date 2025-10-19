package com.crm.service;

import com.crm.common.result.PageResult;
import com.crm.entity.Customer;
import com.baomidou.mybatisplus.extension.service.IService;
import com.crm.query.CustomerQuery;
import com.crm.vo.CustomerVO;
import jakarta.servlet.http.HttpServletResponse;

import java.net.http.HttpResponse;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author crm
 * @since 2025-10-12
 */
public interface CustomerService extends IService<Customer> {

    PageResult<CustomerVO> getPage(CustomerQuery query);
    /**
     * 导出
     * @param query
     */
    void exportCustomer(CustomerQuery query, HttpServletResponse httpResponse);

    /**
     * 保存或更新
     * @param customerVO
     */
    void saveOrUpdate(CustomerVO customerVO);
}