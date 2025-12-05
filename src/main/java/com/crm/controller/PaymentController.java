package com.crm.controller;

import com.crm.common.aop.Log;
import com.crm.common.result.Result;
import com.crm.enums.BusinessType;
import com.crm.query.PaymentApprovalQuery;
import com.crm.query.PaymentQuery;
import com.crm.service.PaymentService;
import com.crm.vo.PaymentVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 回款审核控制器（对齐 ContractController 格式规范）
 */
@Tag(name = "回款审核管理") // 接口文档标签，与合同管理的 @Tag 风格一致
@RestController
@RequestMapping("/approval/payment") // 保持原有接口路径，与前端请求对齐
@AllArgsConstructor // 构造器注入，替代 @Resource，与 ContractController 一致
public class PaymentController {

    // 服务层注入（构造器注入，通过 @AllArgsConstructor 自动生成）
    private final PaymentService paymentService;// 若需直接使用邮件服务，按此格式注入（按需保留）

    // 日志对象（与 ContractController 一致，使用 LoggerFactory）
    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);

    /**
     * 查询待审核回款列表
     * 对应前端：/approval/payment/pending（GET 方法）
     */
    @GetMapping("/pending")
    @Operation(summary = "待审核回款列表") // 接口文档摘要
    @Log(title = "待审核回款查询", businessType = BusinessType.SELECT) // 日志注解，对齐合同管理
    public Result<?> listPendingApproval() {
        log.info("查询待审核回款列表");
        return Result.ok(paymentService.listPendingApproval());
    }

    /**
     * 从合同生成回款数据
     * 对应前端：/approval/payment/generate（POST 方法）
     */
    @PostMapping("/generate")
    @Operation(summary = "从合同生成回款数据")
    @Log(title = "回款数据生成", businessType = BusinessType.INSERT)
    public Result<?> generatePayment() {
        log.info("开始从合同生成回款数据");
        boolean success = paymentService.generatePaymentFromContract();
        // 修复：业务逻辑无异常时，无论是否生成数据，均返回 200（code=0），用 msg 区分结果
        return success ? Result.ok("回款数据生成成功") : Result.ok("暂无待生成回款的合同");
    }

    /**
     * 回款审核操作（通过/驳回）
     * 对应前端：/approval/payment/approve（POST 方法）
     * 新增参数校验、备注传递，与合同审批接口格式一致
     */
    @PostMapping("/approve")
    @Operation(summary = "回款审核（通过/驳回）")
    @Log(title = "回款审核", businessType = BusinessType.INSERT_OR_UPDATE) // 业务类型：新增或更新
    public Result<?> approvalPayment(
            @Parameter(description = "回款审核参数") // 接口文档参数说明
            @RequestBody @Validated PaymentApprovalQuery approvalQuery) { // 请求体+参数校验，对齐合同审批
        log.info("回款审核操作：回款ID={}, 审核状态={}, 备注={}",
                approvalQuery.getId(), approvalQuery.getStatus(), approvalQuery.getRemark());
        // 服务层处理审核逻辑（含邮件发送），与合同审批接口一致：服务层内部处理异常
        paymentService.approvePayment(
                approvalQuery.getId(),
                approvalQuery.getStatus(),
                approvalQuery.getRemark()
        );
        return Result.ok("回款审核成功");
    }

    /**
     * 分页查询回款列表（修改后：对齐前端请求格式）
     * 对应前端：/approval/payment/getPage（GET 方法，参数放 QueryString）
     */
    @GetMapping("/getPage") // 路径改为 /getPage，与前端一致
    @Operation(summary = "回款列表-分页")
    @Log(title = "回款分页查询", businessType = BusinessType.SELECT)
// 参数改为 @ModelAttribute（接收 QueryString 参数），替代 @RequestBody
    public Result<?> getPaymentPage(
            @Parameter(description = "回款分页查询参数")
            @ModelAttribute @Validated PaymentQuery paymentQuery) { // 用 @ModelAttribute 接收 URL 参数
        log.info("分页查询回款列表：参数={}", paymentQuery);
        // 补充服务层调用，返回分页结果（之前遗漏了实际逻辑）
        return Result.ok(paymentService.getPage(paymentQuery));
    }

    // 编辑回显：根据ID查询回款详情
    @GetMapping("/getDetail/{id}")
    public Result<PaymentVO> getDetail(@PathVariable Integer id) {
        PaymentVO detail = paymentService.getDetailById(id);
        return detail != null ? Result.ok(detail) : Result.error("回款记录不存在");
    }

    // 提交编辑：修改回款金额
    @PostMapping("/updateAmount")
    public Result<Boolean> updateAmount(@RequestParam Integer id, @RequestParam Integer newAmount) {
        boolean success = paymentService.updatePaymentAmount(id, newAmount);
        return success ? Result.ok(true) : Result.error("编辑失败");
    }
}