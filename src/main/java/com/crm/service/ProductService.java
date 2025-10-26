package com.crm.service;

import com.crm.common.result.PageResult;
import com.crm.entity.Product;
import com.baomidou.mybatisplus.extension.service.IService;
import com.crm.query.CustomerQuery;
import com.crm.query.IdQuery;
import com.crm.query.ProductQuery;
import com.crm.vo.CustomerVO;
import com.crm.vo.ProductVO;
import jakarta.servlet.http.HttpServletResponse;

import java.rmi.ServerException;
import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author crm
 * @since 2025-10-12
 */
public interface ProductService extends IService<Product> {
    /**
     * 分页查询
     * @param query
     * @return
     */
    PageResult<Product> getPage(ProductQuery query);
    /**
     * 商品新增或者修改
     * @param product
     */
    void saveOrEdit(Product product);

    /**
     * 批量修改商品状态
     */
    void batchUpdateProductStatus();




}
