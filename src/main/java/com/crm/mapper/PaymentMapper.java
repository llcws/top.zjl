package com.crm.mapper;

import com.crm.entity.Payment;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.github.yulichang.base.MPJBaseMapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author crm
 * @since 2025-10-12
 */
public interface PaymentMapper extends MPJBaseMapper<Payment> {
    @Select("select * from t_payment where status =0 AND delete_flag=0")
    List<Payment> selectPendingApproval();

}
