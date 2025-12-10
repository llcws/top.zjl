package com.crm.service;

import com.crm.entity.Opportunity;
import com.baomidou.mybatisplus.extension.service.IService;
import com.crm.common.result.PageResult;
import com.crm.query.OpportunityQuery;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author crm
 * @since 2025-10-12
 */
public interface OpportunityService extends IService<Opportunity> {
    /**
     * 分页查询商机
     */
    PageResult<Opportunity> getPage(OpportunityQuery query);

    /**
     * 保存或更新商机
     */
    void saveOrEdit(Opportunity opportunity);

    /**
     * 删除商机
     */
    void deleteById(Integer id);
    /**
     * 从客户创建商机
     */
    void createFromCustomer(Integer customerId);
    /**
     * 批量从客户创建商机
     */
    void batchCreateFromCustomer(Integer[] customerIds);
}