package com.yonyoucloud.fi.cmp.voucher;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.constant.ISystemCodeConstant;
import com.yonyoucloud.fi.cmp.util.QueryBaseDocUtils;
import org.imeta.orm.base.BizObject;

import java.time.Duration;
import java.util.Date;

/**
 * @InterfaceName CmpVoucherService
 * @Description 现金管理凭证服务接口
 * @Author tongyd
 * @Date 2019/7/5 11:03
 * @Version 1.0
 **/
public interface CmpVoucherService {
    static final Cache<String, Date> enabledBeginDataCache = Caffeine.newBuilder()
            .initialCapacity(100)
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(1))
            .softValues()
            .build();
    /*
     *@Author tongyd
     *@Description 生成凭证
     *@Date 2019/7/5 11:24
     *@Param [billTypeCode, bizobject]
     *@Return com.alibaba.fastjson.JSONObject
     **/
    void generateVoucher(BizObject bizObject) throws Exception;

    /**
     * 生成凭证
     * @param bizObject
     * @return
     * @throws Exception
     */
    CtmJSONObject generateVoucherWithResult(BizObject bizObject) throws Exception;

    CtmJSONObject generateRedVoucherWithResult(BizObject bizObject, String classifier) throws Exception;

    /**
     * 生成凭证-适配分布式事务
     * @param bizObject
     * @return
     * @throws Exception
     */
    CtmJSONObject generateVoucherWithResultTry(BizObject bizObject) throws Exception;

    /**
     * 查询总账模块启用日期
     *
     * @param accentity 会计主体id
     * @return
     * @throws Exception
     */
    default Date queryOrgPeriodBeginDateGL(String accentity) throws Exception {
        return enabledBeginDataCache.get(accentity, (k) -> {
            try {
                return QueryBaseDocUtils.queryOrgPeriodBeginDate(k, ISystemCodeConstant.ORG_MODULE_GL);
            } catch (Exception e) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101066"),"exception when queryOrgPeriodBeginDate by accentity " + k, e);
            }
        });
    }


    /**
     * 付款单、转账单支付生成凭证
     * @param bizObject
     * @return
     * @throws Exception
     */
    CtmJSONObject generateVoucherWithPay(BizObject bizObject) throws Exception;

    /**
     * 删除凭证
     * @param bizObject
     * @return
     * @throws Exception
     */
    CtmJSONObject deleteVoucherWithResult(BizObject bizObject) throws Exception;

    /**
     * 删除凭证，不抛异常版
     * @param bizObject
     * @return
     * @throws Exception
     */
    CtmJSONObject deleteVoucherWithResultWithOutException(BizObject bizObject) throws Exception;

    /**
     * 删除凭证-适配分布式事务
     * @param bizObject
     * @return
     * @throws Exception
     */
    CtmJSONObject deleteVoucherWithResultTry(BizObject bizObject) throws Exception;


    /*
     *@Author tongyd
     *@Description 更新单据凭证状态
     *@Date 2019/7/5 11:47
     *@Param [billTypeCode, billId, voucherStatus]
     *@Return java.lang.boolean
     **/
    boolean updateVoucherStatus(CtmJSONObject voucherStatusInfo) throws Exception;

    /**
     * 查询凭证ID
     * @param param
     * @return
     * @throws Exception
     */
    String queryVoucherId(CtmJSONObject param) throws Exception;

    /**
     * 检测凭证是否勾兑
     * @param jsonObject
     * @return
     */
    boolean isChecked(CtmJSONObject jsonObject) throws Exception;


}
