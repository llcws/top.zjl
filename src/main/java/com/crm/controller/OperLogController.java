package com.crm.controller;

import com.crm.common.aop.Log;
import com.crm.common.result.PageResult;
import com.crm.common.result.Result;
import com.crm.entity.OperLog;
import com.crm.enums.BusinessType;
import com.crm.query.OperLogQuery;
import com.crm.service.OperLogService;
import io.swagger.annotations.Api;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * <p>
 * 操作日志记录 前端控制器
 * </p>
 */
@Api(tags = "操作⽇志")
@RestController
@AllArgsConstructor
@RequestMapping("/operLog")
public class OperLogController {
    private final OperLogService operLogService;
    @PostMapping("page")
    @Operation(summary = "分⻚查询")
    public Result<PageResult<OperLog>> page(@RequestBody OperLogQuery query) {
        return Result.ok(operLogService.page(query));
    }
}
