package com.crm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.crm.common.exception.ServerException;
import com.crm.common.model.BaseServiceImpl;
import com.crm.common.result.PageResult;
import com.crm.entity.Product;
import com.crm.mapper.ProductMapper;
import com.crm.query.ProductQuery;
import com.crm.service.ProductService;
import io.micrometer.common.util.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author crm
 * @since 2025-10-12
 */
@Service
public class ProductServiceImpl extends BaseServiceImpl<ProductMapper, Product> implements ProductService {

    // 时间格式化器（解决前后端时间格式兼容问题）
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public PageResult<Product> getPage(ProductQuery query) {
        // 1.声明分页参数
        Page<Product> page = new Page<>(query.getPage(), query.getLimit());
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        // 2.添加查询条件（空值校验，避免like null报错）
        if (StringUtils.isNotBlank(query.getName())) {
            wrapper.like(Product::getName, query.getName());
        }
        if (query.getStatus() != null) {
            wrapper.eq(Product::getStatus, query.getStatus());
        }
        wrapper.orderByDesc(Product::getCreateTime);
        // 3.查询商品分页列表
        Page<Product> result = baseMapper.selectPage(page, wrapper);
        return new PageResult<>(result.getRecords(), page.getTotal());
    }

    /**
     * 新增/编辑商品（添加事务+空值校验+统一返回逻辑）
     */
    @Override
    @Transactional(rollbackFor = Exception.class) // 添加事务，异常回滚
    public void saveOrEdit(Product product) {
        // 基础空值校验
        if (product == null) {
            throw new ServerException("商品参数不能为空");
        }

        // 1. 新增商品：必须校验名称唯一性
        if (product.getId() == null) {
            // 商品名称空值校验
            if (StringUtils.isBlank(product.getName())) {
                throw new ServerException("商品名称不能为空");
            }
            LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<Product>()
                    .eq(Product::getName, product.getName().trim()); // 去空格，避免重复
            Product newProduct = baseMapper.selectOne(wrapper);
            if (newProduct != null) {
                throw new ServerException("商品名称已经存在,请勿重复添加");
            }
            // 补充默认值，避免数据库字段为空
            product.setCreateTime(LocalDateTime.now());
            product.setUpdateTime(LocalDateTime.now());
            product.setStatus(product.getStatus() == null ? 0 : product.getStatus()); // 默认未上架
            baseMapper.insert(product);
        }
        // 2. 编辑商品：先查询原商品信息
        else {
            Product oldProduct = baseMapper.selectById(product.getId());
            if (oldProduct == null) {
                throw new ServerException("商品不存在，无法编辑");
            }

            // 仅当商品名称发生修改且新名称非空时，才校验名称唯一性
            if (StringUtils.isNotBlank(product.getName())
                    && !oldProduct.getName().equals(product.getName().trim())) {
                LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<Product>()
                        .eq(Product::getName, product.getName().trim())
                        .ne(Product::getId, product.getId()); // 排除当前商品
                Product duplicateProduct = baseMapper.selectOne(wrapper);
                if (duplicateProduct != null) {
                    throw new ServerException("商品名称已经存在,请勿重复添加");
                }
                oldProduct.setName(product.getName().trim()); // 更新名称
            }

            // 仅更新传递的非空字段（避免覆盖原有值）
            if (product.getOnShelfTime() != null) {
                oldProduct.setOnShelfTime(product.getOnShelfTime());
            }
            if (product.getOffShelfTime() != null) {
                oldProduct.setOffShelfTime(product.getOffShelfTime());
            }
            if (product.getPrice() != null) {
                oldProduct.setPrice(product.getPrice());
            }
            if (product.getStock() != null) {
                oldProduct.setStock(product.getStock());
            }
            if (product.getStatus() != null) {
                oldProduct.setStatus(product.getStatus());
            }
            oldProduct.setUpdateTime(LocalDateTime.now()); // 更新时间设为当前，而非null

            // 执行更新（使用原对象，避免字段丢失）
            baseMapper.updateById(oldProduct);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchUpdateProductStatus() {
        LocalDateTime now = LocalDateTime.now();
        // 定时下架：下架时间 <= 当前时间，状态改为2（下架）
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<Product>()
                .le(Product::getOffShelfTime, now)
                .ne(Product::getStatus, 2); // 仅更新未下架的商品，减少更新量
        Product offProduct = new Product();
        offProduct.setStatus(2);
        offProduct.setUpdateTime(now); // 修复：设为当前时间，而非null
        baseMapper.update(offProduct, wrapper);

        // 定时上架：上架时间 < 当前时间，状态改为1（上架）
        wrapper.clear();
        wrapper.lt(Product::getOnShelfTime, now)
                .ne(Product::getStatus, 1); // 仅更新未上架的商品
        Product onProduct = new Product();
        onProduct.setStatus(1);
        onProduct.setUpdateTime(now); // 修复：设为当前时间，而非null
        baseMapper.update(onProduct, wrapper);
    }

    /**
     * 新增：专门用于更新上下架时间的接口（避免saveOrEdit冗余校验）
     * 前端可直接调用此接口，仅更新时间，跳过名称校验
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updateShelfTime(Long productId, String onShelfTime, String offShelfTime) {
        if (productId == null) {
            throw new ServerException("商品ID不能为空");
        }
        Product product = baseMapper.selectById(productId);
        if (product == null) {
            throw new ServerException("商品不存在");
        }
        // 时间字符串转LocalDateTime（兼容前端传递的字符串时间）
        if (StringUtils.isNotBlank(onShelfTime)) {
            try {
                product.setOnShelfTime(LocalDateTime.parse(onShelfTime, DATE_TIME_FORMATTER));
            } catch (Exception e) {
                throw new ServerException("上架时间格式错误，正确格式：yyyy-MM-dd HH:mm:ss");
            }
        }
        if (StringUtils.isNotBlank(offShelfTime)) {
            try {
                product.setOffShelfTime(LocalDateTime.parse(offShelfTime, DATE_TIME_FORMATTER));
            } catch (Exception e) {
                throw new ServerException("下架时间格式错误，正确格式：yyyy-MM-dd HH:mm:ss");
            }
        }
        product.setUpdateTime(LocalDateTime.now());
        baseMapper.updateById(product);
    }
}