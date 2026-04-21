package com.yonyoucloud.fi.cmp.fundcommon.service;

import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.budget.impl.CmpBudgetCommonManagerServiceImpl;
import com.yonyoucloud.fi.cmp.cmpentity.VoucherStatus;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.common.service.CmCommonService;
import com.yonyoucloud.fi.cmp.event.vo.ResultBudget;
import com.yonyoucloud.fi.cmp.voucher.CmpVoucherService;
import lombok.extern.slf4j.Slf4j;
import org.imeta.orm.base.BizObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
public class FundCommonCancelSettleServiceImpl {

    @Autowired
    private CmCommonService commonService;
    @Autowired
    private CmpVoucherService cmpVoucherService;
    @Autowired
    private CmpBudgetCommonManagerServiceImpl cmpBudgetCommonManagerService;

    public boolean isSettleSuccessToPost(String accentity) throws Exception {
        Map<String, Object> autoConfig = commonService.queryAutoConfigByAccentity(accentity);
        boolean isSettleSuccessToPost;
        if (autoConfig != null) {
            // 资金收付款单如果在现金参数里：是否结算成功时做账为是时，则先不过账，最后统一过账
            if (autoConfig.get("isSettleSuccessToPost") != null) {
                isSettleSuccessToPost = (Boolean) autoConfig.get("isSettleSuccessToPost");
            } else {
                Map<String, Object> autoConfigTenant = commonService.queryAutoConfigTenant();
                isSettleSuccessToPost = autoConfigTenant != null && autoConfigTenant.get("isSettleSuccessToPost") != null && (Boolean) autoConfigTenant.get("isSettleSuccessToPost");
            }
        } else {
            Map<String, Object> autoConfigTenant = commonService.queryAutoConfigTenant();
            isSettleSuccessToPost = autoConfigTenant != null && autoConfigTenant.get("isSettleSuccessToPost") != null && (Boolean) autoConfigTenant.get("isSettleSuccessToPost");
        }
        return isSettleSuccessToPost;
    }

    public boolean dealEaaiVoucherInfo(BizObject bizObject) throws Exception {
        //检查事项分录和凭证是否联动删除（1.如果生成事项分录时点为审批通过，则不联动删除 2.如果生成事项分录的时点为结算成功，则联动删除事项分录和凭证
        //判断过账状态，如果是“已过账”，则进行取消过账处理，删除事项分录，会计凭证，如果是“过账中”，则返回“XXX单据过帐中，待过账完成后进行’取消结算‘操作”
        boolean delResult = false;
        boolean isSettleSuccessToPost = isSettleSuccessToPost(bizObject.getString("accentity"));
        if (isSettleSuccessToPost) {
            if (VoucherStatus.POSTING.getValue() == bizObject.getShort("voucherstatus")) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-105004"), String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_2088597A04700009", "资金付款单:[%s]单据过帐中，待过账完成后进行’取消结算‘操作") /* "XXX单据过帐中，待过账完成后进行’取消结算‘操作" */, bizObject.getString("code")));
            } else {
                bizObject.set("voucherstatus_original", bizObject.get("voucherstatus"));
                CtmJSONObject deleteResult = cmpVoucherService.deleteVoucherWithResultTry(bizObject);
                if (!deleteResult.getBoolean("dealSucceed")) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-105005"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418008F", "删除凭证失败：") /* "删除凭证失败：" */ + deleteResult.get("message"));
                }
                delResult = true;
            }
            bizObject.set("voucherNo", null);
            bizObject.set("voucherPeriod", null);
            bizObject.set("voucherId", null);
        }
        return delResult;
    }
//
//    @Deprecated
//    public void dealBuddget(String serviceCode, BizObject bizObject) throws Exception {
//        //预算占用状态回滚（1.如果未占用，不需要额外操作 2.如果已占用，需要回滚状态为预占用）
//        //释放实占
//        ResultBudget resultBudgetReleaseImplement = cmpBudgetCommonManagerService.implement(bizObject, bizObject.getString("code"), serviceCode, true);
//        log.error("resultBudget:{}", resultBudgetReleaseImplement);
//        if (resultBudgetReleaseImplement.isSuccess()) {
//            //执行预占
//            ResultBudget resultBudget = cmpBudgetCommonManagerService.budget(bizObject, "cancelSettle", bizObject.getString("code"), serviceCode, false);
//            if (!resultBudget.isSuccess()) {
//                log.error("单据编码：" + bizObject.getString("code") + "执行预占失败");
//                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-105006"), String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_20885B2A04700004", "单据编码：[%s]执行预占失败") /* "单据编码：[%s]执行预占失败" */, bizObject.getString("code")));
//            }
//        } else {
//            log.error("单据编码" + bizObject.getString("code") + "释放实占失败");
//            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-105007"), String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_20885BB404700006", "单据编码：[%s]释放实占失败") /* "单据编码：[%s]释放实占失败" */, bizObject.getString("code")));
//        }
//    }
}
