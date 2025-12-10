package com.crm.controller;

import com.crm.common.aop.Log;
import com.crm.common.exception.ServerException;
import com.crm.common.result.PageResult;
import com.crm.common.result.Result;
import com.crm.entity.Customer;
import com.crm.enums.BusinessType;
import com.crm.query.CustomerQuery;
import com.crm.query.CustomerTrendQuery;
import com.crm.query.IdQuery;
import com.crm.service.CustomerService;
import com.crm.vo.CustomerVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author crm
 * @since 2025-10-12
 */
@Tag(name = "客户管理")
@RestController
@RequestMapping("customer")
@AllArgsConstructor
public class CustomerController {
    private final CustomerService customerService;

    // ========== 新增：前端下拉框用的列表接口 ==========
    @PostMapping("list")
    @Operation(summary = "客户列表-不分页（下拉框用）")
    @Log(title = "客户列表-不分页", businessType = BusinessType.SELECT)
    public Result<List<CustomerVO>> getList(@RequestBody(required = false) CustomerQuery query) {
        // 1. 处理空参数：如果前端没传参，初始化默认查询条件
        if (query == null) {
            query = new CustomerQuery();
        }
        // 2. 设置分页参数为“查所有”：页码1，每页条数设为极大值（如10000）
        query.setPageNum(1);
        query.setPageSize(10000);
        // 3. 复用现有分页查询方法
        PageResult<CustomerVO> pageResult = customerService.getPage(query);
        // 4. 只返回数据列表（前端下拉框只需list，不需要分页信息）
        return Result.ok(pageResult.getList());
    }

    // ========== 以下是你原有代码，无需修改 ==========
    @PostMapping("page")
    @Operation(summary = "客户列表-分页")
    @Log(title = "客户列表-分页参数", businessType = BusinessType.SELECT)
    public Result<PageResult<CustomerVO>> getPage(@RequestBody CustomerQuery query) {
        return Result.ok(customerService.getPage(query));
    }

    @PostMapping("export")
    @Operation(summary = "客户列表-导出")
    @Log(title = "客户列表-导出参数", businessType = BusinessType.EXPORT)
    public void exportCustomer(@RequestBody CustomerQuery query, HttpServletResponse response) {
        customerService.exportCustomer(query, response);
    }

    @PostMapping("saveOrUpdate")
    @Operation(summary = "保存或更新客户")
    @Log(title = "保存或更新客户参数", businessType = BusinessType.INSERT)
    public Result<String> saveOrUpdate(@RequestBody CustomerVO customerVO) throws java.rmi.ServerException {
        customerService.saveOrUpdate(customerVO);
        return Result.ok();
    }

    @PostMapping("remove")
    @Operation(summary = "删除客户信息")
    @Log(title = "删除客户信息参数", businessType = BusinessType.DELETE)
    public Result removeCustomer(@RequestBody List<Integer> ids){
        if(ids.isEmpty()){
            throw new ServerException("请选择要删除的客户信息");
        }
        customerService.removeCustomer(ids);
        return Result.ok();
    }

    @PostMapping("toPublic")
    @Operation(summary = "转为公海客户")
    @Log(title = "转为公海客户参数", businessType = BusinessType.UPDATE)
    public Result customerToPublicPool(@RequestBody @Validated IdQuery idQuery) throws java.rmi.ServerException {
        customerService.customerToPublicPool(idQuery);
        return Result.ok();
    }

    @PostMapping("toPrivate")
    @Operation(summary = "领取客户")
    @Log(title = "领取客户参数", businessType = BusinessType.UPDATE)
    public Result  publicPoolToPrivate(@RequestBody @Validated IdQuery idQuery) throws java.rmi.ServerException {
        customerService.publicPoolToPrivate(idQuery);
        return Result.ok();
    }

    @PostMapping("/trendData")
    @Operation(summary = "客户变化趋势数据")
    @Log(title = "客户变化趋势", businessType = BusinessType.SELECT)
    public Result<Map<String, List>> getCustomerTrendData(@RequestBody CustomerTrendQuery query) {
        return Result.ok(customerService.getCustomerTrend(query));
    }

}