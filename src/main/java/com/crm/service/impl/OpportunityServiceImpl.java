package com.crm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.crm.common.exception.ServerException;
import com.crm.common.result.PageResult;
import com.crm.entity.Customer;
import com.crm.entity.Opportunity;
import com.crm.mapper.CustomerMapper;
import com.crm.mapper.OpportunityMapper;
import com.crm.query.OpportunityQuery;
import com.crm.security.user.SecurityUser;
import com.crm.service.OpportunityService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.micrometer.common.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * 商机表 服务实现类
 * </p>
 *
 * @author crm
 * @since 2025-10-12
 */
@Service
public class OpportunityServiceImpl extends ServiceImpl<OpportunityMapper, Opportunity> implements OpportunityService {

    // 注入客户Mapper，用于查询转入商机状态的客户
    @Autowired
    private CustomerMapper customerMapper;

    /**
     * 商机列表分页查询
     */
    @Override
    public PageResult<Opportunity> getPage(OpportunityQuery query) {
        // 初始化分页对象（page:页码，limit:每页条数）
        Page<Opportunity> page = new Page<>(query.getPage(), query.getLimit());
        LambdaQueryWrapper<Opportunity> wrapper = new LambdaQueryWrapper<>();

        // 条件过滤：商机名称模糊查询
        if (StringUtils.isNotBlank(query.getName())) {
            wrapper.like(Opportunity::getName, query.getName());
        }
        // 条件过滤：客户ID精确查询
        if (query.getConsumerId() != null) {
            wrapper.eq(Opportunity::getConsumerId, query.getConsumerId());
        }
        // 条件过滤：产品ID精确查询
        if (query.getProductId() != null) {
            wrapper.eq(Opportunity::getProductId, query.getProductId());
        }

        // 过滤已逻辑删除的商机
        wrapper.eq(Opportunity::getDeleteFlag, 0);
        // 按创建时间倒序排列
        wrapper.orderByDesc(Opportunity::getCreateTime);

        // 执行分页查询
        Page<Opportunity> result = baseMapper.selectPage(page, wrapper);
        // 封装分页结果返回
        return new PageResult<>(result.getRecords(), result.getTotal());
    }

    /**
     * 新增/编辑商机
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveOrEdit(Opportunity opportunity) {
        if (opportunity == null) {
            throw new ServerException("商机信息不能为空");
        }

        // 新增商机逻辑
        if (opportunity.getId() == null) {
            // 校验商机名称必填
            if (StringUtils.isBlank(opportunity.getName())) {
                throw new ServerException("商机名称不能为空");
            }
            // 设置创建人ID（从当前登录用户获取）
            opportunity.setCreaterId(SecurityUser.getManagerId());
            // 设置创建时间
            opportunity.setCreateTime(LocalDateTime.now());
            // 设置逻辑删除标记（默认未删除）
            opportunity.setDeleteFlag((byte) 0);
            baseMapper.insert(opportunity);
        }
        // 编辑商机逻辑
        else {
            // 查询原商机信息
            Opportunity oldOpportunity = baseMapper.selectById(opportunity.getId());
            if (oldOpportunity == null || oldOpportunity.getDeleteFlag() == 1) {
                throw new ServerException("商机不存在或已被删除");
            }

            // 仅更新指定字段，保证数据安全性
            oldOpportunity.setName(opportunity.getName());
            oldOpportunity.setBudget(opportunity.getBudget());
            oldOpportunity.setConsumerId(opportunity.getConsumerId());
            oldOpportunity.setProductId(opportunity.getProductId());
            oldOpportunity.setExpectedCloseDate(opportunity.getExpectedCloseDate());
            oldOpportunity.setNextFollowTime(opportunity.getNextFollowTime());
            oldOpportunity.setRemark(opportunity.getRemark());
            oldOpportunity.setUpdateTime(LocalDateTime.now());

            baseMapper.updateById(oldOpportunity);
        }
    }

    /**
     * 逻辑删除商机
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteById(Integer id) {
        if (id == null) {
            throw new ServerException("商机ID不能为空");
        }

        // 查询商机是否存在
        Opportunity opportunity = baseMapper.selectById(id);
        if (opportunity == null || opportunity.getDeleteFlag() == 1) {
            throw new ServerException("商机不存在或已被删除");
        }

        // 执行逻辑删除
        opportunity.setDeleteFlag((byte) 1);
        opportunity.setUpdateTime(LocalDateTime.now());
        baseMapper.updateById(opportunity);
    }

    /**
     * 从单个客户创建商机（仅支持 follow_status=3 转入商机状态的客户）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createFromCustomer(Integer customerId) {
        // 1. 校验客户ID不能为空
        if (customerId == null) {
            throw new ServerException("客户ID不能为空");
        }

        // 2. 查询客户：强制过滤 follow_status=3（转入商机）的客户
        LambdaQueryWrapper<Customer> customerWrapper = new LambdaQueryWrapper<>();
        customerWrapper.eq(Customer::getId, customerId)
                .eq(Customer::getFollowStatus, 3) // 核心过滤条件
                .eq(Customer::getDeleteFlag, 0); // 过滤已删除的客户
        Customer customer = customerMapper.selectOne(customerWrapper);

        // 3. 校验客户是否存在且状态符合要求
        if (customer == null) {
            throw new ServerException("只能从【转入商机】状态的客户创建商机，该客户不存在或状态不符");
        }

        // 4. 构建商机对象（从客户数据映射）
        Opportunity opportunity = new Opportunity();
        // 商机名称：客户名称 + 固定后缀
        opportunity.setName(customer.getName() + "的商机");
        // 关联客户ID
        opportunity.setConsumerId(customer.getId());
        // 设置创建人（当前登录用户）
        opportunity.setCreaterId(SecurityUser.getManagerId());
        // 设置默认值：逻辑删除标记、创建时间
        opportunity.setDeleteFlag((byte) 0);
        opportunity.setCreateTime(LocalDateTime.now());

        // 5. 保存商机
        baseMapper.insert(opportunity);
    }

    /**
     * 批量从客户创建商机（仅支持 follow_status=3 转入商机状态的客户）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchCreateFromCustomer(Integer[] customerIds) {
        // 1. 校验客户ID数组不能为空
        if (customerIds == null || customerIds.length == 0) {
            throw new ServerException("请选择需要创建商机的客户");
        }

        // 2. 查询符合条件的客户（follow_status=3 + 未删除）
        LambdaQueryWrapper<Customer> customerWrapper = new LambdaQueryWrapper<>();
        customerWrapper.in(Customer::getId, customerIds)
                .eq(Customer::getFollowStatus, 3) // 核心过滤条件
                .eq(Customer::getDeleteFlag, 0); // 过滤已删除的客户
        List<Customer> customerList = customerMapper.selectList(customerWrapper);

        // 3. 校验有效客户数量
        if (customerList.isEmpty()) {
            throw new ServerException("所选客户中无【转入商机】状态的有效客户，无法创建商机");
        }
        if (customerList.size() != customerIds.length) {
            throw new ServerException("部分客户不是【转入商机】状态或已删除，仅为符合条件的客户创建商机");
        }

        // 4. 批量构建商机对象
        List<Opportunity> opportunityList = new ArrayList<>();
        for (Customer customer : customerList) {
            Opportunity opportunity = new Opportunity();
            opportunity.setName(customer.getName() + "的商机");
            opportunity.setConsumerId(customer.getId());
            opportunity.setCreaterId(SecurityUser.getManagerId());
            opportunity.setDeleteFlag((byte) 0);
            opportunity.setCreateTime(LocalDateTime.now());
            opportunityList.add(opportunity);
        }

        // 5. 批量保存商机
        this.saveBatch(opportunityList);
    }
}