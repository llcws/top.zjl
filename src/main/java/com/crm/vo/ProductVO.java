package com.crm.vo;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * @description:
 * @author: ycshang
 * @create: 2025-10-25 10:15
 **/
@Data
public class ProductVO {
    @JsonProperty("pId")
    private Integer pId;
    @JsonProperty("pName")
    private String pName;
    @JsonProperty("totalPrice")
    private BigDecimal totalPrice;
    private Integer count;
    private BigDecimal price;
}