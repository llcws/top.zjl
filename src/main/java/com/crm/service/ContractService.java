package com.crm.service;

import com.crm.common.result.PageResult;
import com.crm.entity.Contract;
import com.baomidou.mybatisplus.extension.service.IService;
import com.crm.query.ApprovalQuery;
import com.crm.query.ContractQuery;
import com.crm.query.IdQuery;
import com.crm.vo.ContractTrendPieVO;
import com.crm.vo.ContractVO;

import java.util.List;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author crm
 * @since 2025-10-12
 */
public interface ContractService extends IService<Contract> {

    /**
     * 合同列表 - 分页
     *
     * @param query
     * @return
     */
    PageResult<ContractVO> getPage(ContractQuery query);

    /**
     * 新增/修改合同
     *
     * @param contractVO
     */

    void saveOrUpdate(ContractVO contractVO);

    // 新增：按合同状态统计饼图数据
    List<ContractTrendPieVO> getContractStatusPieData();

    void startApproval(IdQuery idQuery) ;
    void approvalContract(ApprovalQuery query) ;

}