package com.crm.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.crm.entity.Contract;
import com.crm.entity.Customer;
import com.crm.entity.Payment;
import com.crm.mapper.ContractMapper;
import com.crm.mapper.CustomerMapper;
import com.crm.mapper.PaymentMapper;
import com.crm.query.PaymentQuery;
import com.crm.service.PaymentService;
import com.crm.common.result.PageResult; // 仅保留一次项目自定义 PageResult 导入
import com.crm.vo.PaymentVO;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author crm
 * @since 2025-10-12
 */
@Service
@Transactional
public class PaymentServiceImpl extends ServiceImpl<PaymentMapper, Payment> implements PaymentService {

    @Autowired
    private ContractMapper contractMapper;

    @Autowired
    private PaymentMapper paymentMapper;

    @Autowired
    private CustomerMapper customerMapper;

    @Override
    public boolean generatePaymentFromContract() {
        // 1. 查询未生成回款的合同
        List<Contract> contracts = contractMapper.selectUnGeneratedPaymentContracts();
        if (CollectionUtils.isEmpty(contracts)) {
            return false;
        }

        // 2. 批量生成回款数据（金额：合同金额×100 转分存储）
        List<Payment> payments = contracts.stream().map(contract -> {
            Payment payment = new Payment();
            payment.setContractId(contract.getId());
            payment.setCustomerId(contract.getCustomerId());
            payment.setContractNumber(contract.getNumber());
            payment.setContractName(contract.getName());
            // 合同金额（元）转分存储（确保 contract.getAmount() 不为 null）
            payment.setNumber(contract.getAmount() != null ? contract.getAmount().multiply(new BigDecimal(100)).intValue() : 0);
            // 修复："admin" 转 Integer 报错，改为默认创建人ID 1
            payment.setCreaterId(1);
            payment.setStatus(0); // 0-待审核
            payment.setPaymentMethod(1); // 默认支付方式（1=银行转账）
            payment.setPaymentTime(LocalDateTime.now());
            payment.setDeleteFlag(0); // 未删除
            return payment;
        }).collect(Collectors.toList());

        // 3. 批量插入回款表
        return saveBatch(payments);
    }

    @Override
    public boolean approvePayment(Integer id, Integer status, String remark) {
        // 1. 校验回款记录是否存在
        Payment payment = getById(id);
        if (payment == null || payment.getDeleteFlag() == 1) {
            return false;
        }

        // 2. 更新审核状态（1-通过，2-驳回）+ 更新时间
        payment.setStatus(status);
        payment.setUpdateTime(LocalDateTime.now());
        return updateById(payment);
    }

    @Override
    public List<Payment> listPendingApproval() {
        return paymentMapper.selectPendingApproval();
    }

    @Override
    public PageResult<PaymentVO> getPage(PaymentQuery query) {
        // 1. 构建分页对象（页码、每页条数与前端参数对齐）
        Page<PaymentVO> page = new Page<>(query.getPage(), query.getLimit());

        // 2. 构建 MPJ 关联查询条件（使用 getter 方法引用，适配项目环境）
        MPJLambdaWrapper<Payment> wrapper = new MPJLambdaWrapper<>();

        // 2.1 筛选条件（空条件不生效，避免误过滤）
        if (StringUtils.hasText(query.getContractNumber())) {
            wrapper.like(Payment::getContractNumber, query.getContractNumber());
        }
        if (query.getStatus() != null) {
            wrapper.eq(Payment::getStatus, query.getStatus());
        }
        if (query.getCustomerId() != null) {
            wrapper.eq(Payment::getCustomerId, query.getCustomerId());
        }
        if (StringUtils.hasText(query.getContractName())) {
            wrapper.like(Payment::getContractName, query.getContractName());
        }
        if (query.getPaymentMethod() != null) {
            wrapper.eq(Payment::getPaymentMethod, query.getPaymentMethod());
        }

        // 2.2 关联客户表+数据过滤+排序（字段引用正确）
        wrapper.selectAll(Payment.class) // 查询回款表所有字段
                .selectAs(Customer::getName, PaymentVO::getCustomerName) // 客户名称映射到 VO
                .leftJoin(Customer.class, Customer::getId, Payment::getCustomerId) // 关联条件：客户ID匹配
                .eq(Payment::getDeleteFlag, 0) // 过滤未删除数据
                .orderByDesc(Payment::getCreateTime); // 按创建时间倒序

        // 3. 执行分页关联查询
        IPage<PaymentVO> iPage = paymentMapper.selectJoinPage(page, PaymentVO.class, wrapper);

        // 4. VO 数据格式化（分转元、状态/支付方式文本转换）
        List<PaymentVO> formattedList = iPage.getRecords().stream()
                .peek(vo -> {
                    // 金额：分转元（保留2位小数）
                    vo.setAmount(vo.getNumber() != null ? String.format("%.2f", vo.getNumber() / 100.0) : "0.00");
                    // 审核状态文本
                    vo.setStatusText(switch (vo.getStatus()) {
                        case 0 -> "待审核";
                        case 1 -> "审核通过";
                        case 2 -> "审核驳回";
                        default -> "未知状态";
                    });
                    // 支付方式文本
                    vo.setPaymentMethodText(switch (vo.getPaymentMethod()) {
                        case 1 -> "银行转账";
                        case 2 -> "支付宝";
                        case 3 -> "微信支付";
                        default -> "未知方式";
                    });
                })
                .collect(Collectors.toList());

        // 5. 正确封装返回结果（匹配自定义 PageResult 构造器）
        return new PageResult<>(formattedList, iPage.getTotal());
    }

    // 查询回款详情（修复关联字段和映射错误）
    @Override
    public PaymentVO getDetailById(Integer id) {
        MPJLambdaWrapper<Payment> wrapper = new MPJLambdaWrapper<>();
        wrapper.selectAll(Payment.class)
                .selectAs(Customer::getName, PaymentVO::getCustomerName) // 客户名称映射到 VO 的 customerName
                .leftJoin(Customer.class, Customer::getId, Payment::getCustomerId) // 关联条件：Customer.id = Payment.customerId
                .eq(Payment::getId, id) // 匹配回款ID
                .eq(Payment::getDeleteFlag, 0); // 过滤未删除数据
        return paymentMapper.selectJoinOne(PaymentVO.class, wrapper);
    }

    // 编辑回款金额（逻辑正常）
    @Override
    public boolean updatePaymentAmount(Integer id, Integer newAmount) {
        Payment payment = getById(id);
        if (payment == null || payment.getDeleteFlag() == 1) {
            return false;
        }
        payment.setNumber(newAmount); // 金额以分为单位存储
        payment.setUpdateTime(LocalDateTime.now());
        return updateById(payment);
    }
}