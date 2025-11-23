package com.crm.controller;

import com.crm.common.aop.Log;
import com.crm.common.result.PageResult;
import com.crm.common.result.Result;
import com.crm.enums.BusinessType;
import com.crm.enums.ContractStatusEnum;
import com.crm.query.ApprovalQuery;
import com.crm.query.ContractQuery;
import com.crm.query.IdQuery;
import com.crm.service.ContractService;
import com.crm.vo.ContractTrendPieVO;
import com.crm.vo.ContractVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "合同管理")
@RestController
@RequestMapping("contract")
@AllArgsConstructor
public class ContractController {
    private final ContractService contractService;
    private static final Logger log = LoggerFactory.getLogger(ContractController.class);

    @PostMapping("page")
    @Operation(summary = "合同列表-分页")
    @Log(title = "合同列表-分页参数", businessType = BusinessType.SELECT)
    public Result<PageResult<ContractVO>> getPage(
            @Parameter(description = "分页查询参数") @RequestBody @Validated ContractQuery contractQuery) {
        return Result.ok(contractService.getPage(contractQuery));
    }

    @PostMapping("saveOrUpdate")
    @Operation(summary = "新增/修改合同信息")
    @Log(title = "新增/修改合同信息参数", businessType = BusinessType.INSERT)
    public Result saveOrUpdate(@RequestBody @Validated ContractVO customerVO) {
        if (customerVO.getId() == null && customerVO.getStatus() == null) {
            customerVO.setStatus(ContractStatusEnum.INIT.getValue());
        }
        contractService.saveOrUpdate(customerVO);
        boolean isNew = customerVO.getId() == null;
        return Result.ok(isNew ? "新增成功" : "修改成功");
    }

    @PostMapping("/statusPieData")
    @Operation(summary = "合同状态分布统计（饼图）")
    public Result<List<ContractTrendPieVO>> getContractStatusPieData() {
        return Result.ok(contractService.getContractStatusPieData());
    }

    @PostMapping("/startApproval")
    @Operation(summary = "启动合同审批")
    @Log(title = "启动合同审批", businessType = BusinessType.INSERT_OR_UPDATE)
    public Result startApproval(@RequestBody @Validated IdQuery idQuery) {
        // 移除 ServerException，直接调用服务层（服务层内部处理异常）
        contractService.startApproval(idQuery);
        return Result.ok();
    }

    @PostMapping("/approvalContract")
    @Operation(summary = "合同审批")
    @Log(title = "合同审批", businessType = BusinessType.INSERT_OR_UPDATE)
    public Result approvalContract(@RequestBody @Validated ApprovalQuery query) {
        // 移除 ServerException，直接调用服务层（服务层内部处理异常）
        contractService.approvalContract(query);
        return Result.ok();
    }
}