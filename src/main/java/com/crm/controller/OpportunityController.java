package com.crm.controller;

import com.crm.common.aop.Log;
import com.crm.common.result.PageResult;
import com.crm.common.result.Result;
import com.crm.entity.Opportunity;
import com.crm.enums.BusinessType;
import com.crm.query.OpportunityQuery;
import com.crm.service.OpportunityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 商机管理前端控制器
 * </p>
 *
 * @author crm
 * @since 2025-10-12
 */
@Tag(name = "商机管理")
@RestController
@RequestMapping("opportunity")
@AllArgsConstructor
public class OpportunityController {

    private final OpportunityService opportunityService;

    /**
     * 分页查询商机列表
     */
    @PostMapping("page")
    @Operation(summary = "分页查询商机")
    @Log(title = "商机管理", businessType = BusinessType.SELECT)
    public Result<PageResult<Opportunity>> getPage(@RequestBody @Validated OpportunityQuery query) {
        return Result.ok(opportunityService.getPage(query));
    }

    /**
     * 根据ID查询商机详情
     * 修复：PostMapping + @PathVariable 不兼容，改为 @RequestBody 传参（符合项目POST传参风格）
     */
    @PostMapping("getById")
    @Operation(summary = "根据ID查询商机")
    @Log(title = "商机管理", businessType = BusinessType.SELECT)
    public Result<Opportunity> getById(@RequestBody IdQuery idQuery) {
        if (idQuery.getId() == null) {
            throw new com.crm.common.exception.ServerException("商机ID不能为空");
        }
        Opportunity opportunity = opportunityService.getById(idQuery.getId());
        if (opportunity == null || opportunity.getDeleteFlag() == 1) {
            throw new com.crm.common.exception.ServerException("商机不存在或已被删除");
        }
        return Result.ok(opportunity);
    }

    /**
     * 保存或修改商机
     */
    @PostMapping("saveOrEdit")
    @Operation(summary = "保存或修改商机")
    @Log(title = "商机管理", businessType = BusinessType.INSERT_OR_UPDATE)
    public Result saveOrEdit(@RequestBody @Validated Opportunity opportunity) {
        opportunityService.saveOrEdit(opportunity);
        return Result.ok("商机操作成功");
    }

    /**
     * 删除商机（逻辑删除）
     * 修复：PostMapping + @PathVariable 不兼容，改为 @RequestBody 传参
     */
    @PostMapping("remove")
    @Operation(summary = "删除商机")
    @Log(title = "商机管理", businessType = BusinessType.DELETE)
    public Result delete(@RequestBody IdQuery idQuery) {
        if (idQuery.getId() == null) {
            throw new com.crm.common.exception.ServerException("商机ID不能为空");
        }
        opportunityService.deleteById(idQuery.getId());
        return Result.ok("商机删除成功");
    }

    /**
     * 从单个客户创建商机（仅支持 follow_status=3 转入商机状态的客户）
     */
    @PostMapping("createFromCustomer")
    @Operation(summary = "从客户创建商机")
    @Log(title = "商机管理", businessType = BusinessType.INSERT)
    public Result createFromCustomer(@RequestBody IdQuery idQuery) {
        if (idQuery.getId() == null) {
            throw new com.crm.common.exception.ServerException("客户ID不能为空");
        }
        opportunityService.createFromCustomer(idQuery.getId());
        return Result.ok("商机创建成功");
    }

    /**
     * 批量从客户创建商机（仅支持 follow_status=3 转入商机状态的客户）
     */
    @PostMapping("batchCreateFromCustomer")
    @Operation(summary = "批量从客户创建商机")
    @Log(title = "商机管理", businessType = BusinessType.INSERT)
    public Result batchCreateFromCustomer(@RequestBody Integer[] customerIds) {
        opportunityService.batchCreateFromCustomer(customerIds);
        return Result.ok("批量创建商机成功");
    }

    // 补充ID查询参数类（若项目中未定义，需添加）
    static class IdQuery {
        private Integer id;

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }
    }
}