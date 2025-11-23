package com.crm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.crm.common.exception.ServerException;
import com.crm.common.result.PageResult;
import com.crm.convert.ContractConvert;
import com.crm.entity.*;
import com.crm.enums.ContractStatusEnum;
import com.crm.mapper.*;
import com.crm.query.ApprovalQuery;
import com.crm.query.ContractQuery;
import com.crm.query.IdQuery;
import com.crm.security.user.SecurityUser;
import com.crm.service.ContractService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.crm.vo.ContractTrendPieVO;
import com.crm.vo.ContractVO;
import com.crm.vo.ProductVO;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import jakarta.annotation.Resource;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import static com.crm.utils.NumberUtils.generateContractNumber;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author crm
 * @since 2025-10-12
 */
@Slf4j
@Service
@AllArgsConstructor
public class ContractServiceImpl extends ServiceImpl<ContractMapper, Contract> implements ContractService {
    private final ProductMapper productMapper;
    private final ContractProductMapper contractProductMapper;
    private final ContractMapper baseMapper;
    @Autowired
    private ApprovalMapper approvalMapper;

    @Override
    public PageResult<ContractVO> getPage(ContractQuery query) {
        Page<ContractVO> page = new Page<>(query.getPage(), query.getLimit());
//        条件查询
        MPJLambdaWrapper<Contract> wrapper = new MPJLambdaWrapper<>();
        if (StringUtils.isNotBlank(query.getName())) {
            wrapper.like(Contract::getName, query.getName());
        }
        if (query.getStatus() != null) {
            wrapper.eq(Contract::getStatus, query.getStatus());
        }
        if (query.getCustomerId() != null) {
            wrapper.eq(Contract::getCustomerId, query.getCustomerId());
        }
        if (StringUtils.isNotBlank(query.getNumber())) {
            wrapper.like(Contract::getNumber, query.getNumber());
        }
        // 只查询目前登录的员工签署的合同信息
        Integer managerId = SecurityUser.getManagerId();
        wrapper.selectAll(Contract.class)
                .selectAs(Customer::getName, ContractVO::getCustomerName)
                .leftJoin(Customer.class, Customer::getId, Contract::getCustomerId)
                .eq(Contract::getOwnerId, managerId).orderByDesc(Contract::getCreateTime);
        Page<ContractVO> result = baseMapper.selectJoinPage(page, ContractVO.class, wrapper);
//        查询合同签署的商品信息
        if (!result.getRecords().isEmpty()) {
            result.getRecords().forEach(contractVO -> {
                List<ContractProduct> contractProducts = contractProductMapper.selectList(new LambdaQueryWrapper<ContractProduct>().eq(ContractProduct::getCId, contractVO.getId()));
                contractVO.setProducts(ContractConvert.INSTANCE.toProductVOList(contractProducts));
            });
        }
        return new PageResult<>(result.getRecords(), page.getTotal());
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveOrUpdate(ContractVO contractVO) {

        boolean isNew = contractVO.getId() == null;

        // 校验合同名称重复
        if (isNew && baseMapper.exists(new LambdaQueryWrapper<Contract>().eq(Contract::getName, contractVO.getName()))) {
            throw new ServerException("合同名称已存在，请勿重复添加");
        }

        // 转换并保存合同
        Contract contract = ContractConvert.INSTANCE.toContract(contractVO);
        contract.setCreaterId(SecurityUser.getManagerId());
        contract.setOwnerId(SecurityUser.getManagerId());
        if (isNew) {
            contract.setNumber(generateContractNumber());
            baseMapper.insert(contract);
        } else {
            Contract dbContract = baseMapper.selectById(contract.getId());
            if (dbContract == null) throw new ServerException("合同不存在");
            if (dbContract.getStatus() == 1) throw new ServerException("该合同已审核通过，请勿修改");
            baseMapper.updateById(contract);
        }


        // 处理合同商品明细
        handleContractProducts(contract.getId(), contractVO.getProducts());

    }

    @Override
    public List<ContractTrendPieVO> getContractStatusPieData() {
        // 获取当前登录用户ID，确保数据权限
        Integer managerId = SecurityUser.getManagerId();
        // 调用Mapper方法按状态统计数量（传入用户ID）
        List<ContractTrendPieVO> pieData = baseMapper.countByStatus(managerId);

        // 计算总数量和占比（基于数量计算）
        int total = pieData.stream()
                .mapToInt(ContractTrendPieVO::getCount)
                .sum();

        pieData.forEach(item -> {
            // 计算占比（数量/总数量*100）
            item.setProportion(total > 0 ? (double) item.getCount() / total * 100 : 0);
        });

        return pieData;
    }
    @Autowired
    private SysManagerMapper sysManagerMapper;
    @Autowired
    private EmailService emailService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approvalContract(ApprovalQuery query) {
        Contract contract = baseMapper.selectById(query.getId());
        if (contract == null) {
            throw new ServerException("合同不存在");
        }
        // 校验合同状态是否为"审核中"
        if (!ContractStatusEnum.PENDING.getValue().equals(contract.getStatus())) {
            throw new ServerException("该合同已审核通过，请勿重复操作");
        }

        // 构建审核内容与目标状态
        String approvalContent = query.getType() == 0
                ? "合同审核通过：" + query.getApprovalContent()
                : "合同审核未通过：" + query.getApprovalContent();
        Integer contractStatus = query.getType() == 0
                ? ContractStatusEnum.APPROVED.getValue()
                : ContractStatusEnum.REJECTED.getValue();

        // 保存审核记录
        Approval approval = new Approval();
        approval.setType(0);
        approval.setStatus(query.getType());
        approval.setCreaterId(SecurityUser.getManagerId());
        approval.setContractId(contract.getId());
        approval.setComment(approvalContent);
        approvalMapper.insert(approval);

        // 更新合同状态与审核内容
        contract.setStatus(contractStatus);
        contract.setApprovalContent(approvalContent);
        contract.setApproverId(SecurityUser.getManagerId());
        contract.setApprovalTime(LocalDateTime.now());
        baseMapper.updateById(contract);

        // 获取创建合同的销售ID（creater_id对应销售管理员ID）
        Integer salesId = contract.getCreaterId();
        if (salesId == null) {
            log.warn("合同[ID:{}]未关联创建人，无法发送审核通知", contract.getId());
            return;
        }

        // 查询销售的邮箱信息
        SysManager sales = sysManagerMapper.selectById(salesId);
        if (sales == null || StringUtils.isBlank(sales.getEmail())) {
            log.warn("销售[ID:{}]未设置邮箱，无法发送审核通知", salesId);
            return;
        }

        // 发送邮件通知（同时支持通过和未通过）
        try {
            String emailSubject = "合同审核通知";
            String emailContent;
            if (query.getType() == 0) { // 0 表示审核通过
                emailContent = String.format(
                        "您好，%s！\n\n您创建的合同【%s】（编号：%s）已审核通过。\n审核意见：%s\n\n请及时跟进后续流程。",
                        sales.getNickname(),
                        contract.getName(),
                        contract.getNumber(),
                        query.getApprovalContent()
                );
            } else { // 非0 表示审核未通过
                emailContent = String.format(
                        "您好，%s！\n\n您创建的合同【%s】（编号：%s）审核未通过。\n审核意见：%s\n\n请查看并处理后重新提交。",
                        sales.getNickname(),
                        contract.getName(),
                        contract.getNumber(),
                        query.getApprovalContent()
                );
            }
            emailService.sendSimpleEmail(sales.getEmail(), emailSubject, emailContent);
            log.info("合同[ID:{}]审核{}，已通知销售[ID:{}]",
                    contract.getId(),
                    query.getType() == 0 ? "通过" : "未通过",
                    salesId);
        } catch (Exception e) {
            // 邮件发送失败不影响主流程，但记录日志
            log.error("合同[ID:{}]审核通知邮件发送失败", contract.getId(), e);
        }
    }

    // startApproval 方法调整（替换硬编码1）
    @Override
    public void startApproval(IdQuery idQuery) {
        Contract contract = baseMapper.selectById(idQuery.getId());
        if (contract == null) {
            throw new ServerException("合同不存在");
        }
        // 状态变更为：审核中（枚举值）
        contract.setStatus(ContractStatusEnum.PENDING.getValue());
        baseMapper.updateById(contract);
    }

    @Resource
    private ContractMapper contractMapper;



    // 生成当天24小时时间轴（00:00至23:00）
    private List<String> getHourData() {
        List<String> hours = new ArrayList<>();
        for (int i = 0; i < 24; i++) {
            hours.add(String.format("%02d:00", i));
        }
        return hours;
    }

    private void handleContractProducts(Integer contractId, List<ProductVO> newProductList) {
        log.info("接收到的产品ID列表：{}", newProductList.stream().map(ProductVO::getPId).toList());
        if (newProductList == null) return;

        List<ContractProduct> oldProducts = contractProductMapper.selectList(
                new LambdaQueryWrapper<ContractProduct>().eq(ContractProduct::getCId, contractId)
        );

        // === 1. 新增商品 ===
        List<ProductVO> newAdded = newProductList.stream()
                .filter(np -> oldProducts.stream().noneMatch(op -> op.getPId().equals(np.getPId())))
                .toList();
        for (ProductVO p : newAdded) {
            Product product = checkAndGetProduct(p.getPId(), p.getCount());
            decreaseStock(product, p.getCount());
            ContractProduct cp = buildContractProduct(contractId, product, p.getCount());
            contractProductMapper.insert(cp);
        }

        // === 2. 修改数量 ===
        List<ProductVO> changed = newProductList.stream()
                .filter(np -> oldProducts.stream()
                        .anyMatch(op -> op.getPId().equals(np.getPId()) && !op.getCount().equals(np.getCount())))
                .toList();
        for (ProductVO p : changed) {
            ContractProduct old = oldProducts.stream()
                    .filter(op -> op.getPId().equals(p.getPId()))
                    .findFirst().orElseThrow();

            Product product = checkAndGetProduct(p.getPId(), 0);
            int diff = p.getCount() - old.getCount();

            // 库存调整
            if (diff > 0) decreaseStock(product, diff);
            else if (diff < 0) increaseStock(product, -diff);

            // 更新合同商品
            old.setCount(p.getCount());
            old.setPrice(product.getPrice());
            old.setTotalPrice(product.getPrice().multiply(BigDecimal.valueOf(p.getCount())));
            contractProductMapper.updateById(old);
        }

        // === 3. 删除商品 ===
        List<ContractProduct> removed = oldProducts.stream()
                .filter(op -> newProductList.stream().noneMatch(np -> np.getPId().equals(op.getPId())))
                .toList();
        for (ContractProduct rm : removed) {
            Product product = productMapper.selectById(rm.getPId());
            if (product != null) increaseStock(product, rm.getCount());
            contractProductMapper.deleteById(rm.getId());
        }
    }


    private Product checkAndGetProduct(Integer productId, int needCount) {
        Product product = productMapper.selectById(productId);
        if (product == null) throw new ServerException("商品不存在");
        if (needCount > 0 && product.getStock() < needCount) {
            throw new ServerException("商品库存不足");
        }
        return product;
    }

    private void decreaseStock(Product product, int count) {
        product.setStock(product.getStock() - count);
        product.setSales(product.getSales() + count);
        productMapper.updateById(product);
    }

    private void increaseStock(Product product, int count) {
        product.setStock(product.getStock() + count);
        product.setSales(product.getSales() - count);
        productMapper.updateById(product);
    }

    private ContractProduct buildContractProduct(Integer contractId, Product product, int count) {
        ContractProduct cp = new ContractProduct();
        cp.setCId(contractId);
        cp.setPId(product.getId());
        cp.setPName(product.getName());
        cp.setCount(count);
        cp.setPrice(product.getPrice());
        cp.setTotalPrice(product.getPrice().multiply(BigDecimal.valueOf(count)));
        log.info("即将入库的商品ID：{}", cp.getPId());
        return cp;
    }


}