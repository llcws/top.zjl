package com.crm.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import com.alibaba.excel.annotation.write.style.ContentRowHeight;
import com.crm.converter.ProductConverter;
import com.crm.utils.DateUtils;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@ExcelIgnoreUnannotated
@ContentRowHeight(105)
@ColumnWidth(24)
public class ProductVO {

    @Schema(description = "商品主键")
    private Integer id;

    @Schema(description = "商品名称")
    @NotBlank(message = "商品名称不能为空")
    @ExcelProperty("商品名称")
    private String name;

    @Schema(description = "商品编码")
    @NotBlank(message = "商品编码不能为空")
    @ExcelProperty("商品编码")
    private String code;

    @Schema(description = "商品价格")
    @NotNull(message = "商品价格不能为空")
    @DecimalMin(value = "0.01", message = "商品价格不能小于0.01")
    @ExcelProperty("商品价格")
    private BigDecimal price;

    @Schema(description = "商品库存")
    @NotNull(message = "商品库存不能为空")
    @ExcelProperty("库存数量")
    private Integer stock;

    @Schema(description = "商品销量")
    @ExcelProperty("累计销量")
    private Integer sales;

    @Schema(description = "商品状态 0-下架 1-上架 2-预售")
    @ExcelProperty(value = "商品状态", converter = ProductConverter.class)
    private Integer status;

    @Schema(description = "商品图片URL")
    private String imageUrl;

    @Schema(description = "商品描述")
    private String description;

    @Schema(description = "定时上架时间")
    @JsonFormat(pattern = DateUtils.DATE_TIME_PATTERN)
    @ExcelProperty("定时上架时间")
    private LocalDateTime scheduledUpTime;

    @Schema(description = "定时下架时间")
    @JsonFormat(pattern = DateUtils.DATE_TIME_PATTERN)
    @ExcelProperty("定时下架时间")
    private LocalDateTime scheduledDownTime;

    @Schema(description = "创建人ID")
    private Integer createrId;

    @Schema(description = "创建人名称")
    private String createrName;

    @Schema(description = "最后编辑人ID")
    private Integer editorId;

    @Schema(description = "最后编辑人名称")
    private String editorName;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = DateUtils.DATE_TIME_PATTERN)
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @JsonFormat(pattern = DateUtils.DATE_TIME_PATTERN)
    private LocalDateTime updateTime;
}