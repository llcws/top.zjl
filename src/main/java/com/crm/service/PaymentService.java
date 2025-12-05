package com.crm.service;

import com.crm.common.result.PageResult;
import com.crm.entity.Payment;
import com.baomidou.mybatisplus.extension.service.IService;
import com.crm.query.PaymentQuery;
import com.crm.vo.PaymentVO;

import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author crm
 * @since 2025-10-12
 */
public interface PaymentService extends IService<Payment> {
    // 从合同表生成回款数据
    boolean generatePaymentFromContract();

    // 回款审核（通过/驳回）
    boolean approvePayment(Integer id, Integer status, String remark);

    // 查询待审核回款列表
    List<Payment> listPendingApproval();

    PageResult<PaymentVO> getPage(PaymentQuery query);
    PaymentVO getDetailById(Integer id);
    boolean updatePaymentAmount(Integer id,Integer newAmount);
}
