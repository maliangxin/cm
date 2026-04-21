package com.yonyoucloud.fi.cmp.transferaccount.service;

import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.ctm.stwb.datasettled.DataSettled;
import com.yonyoucloud.ctm.stwb.openapi.IOpenApiService;
import com.yonyoucloud.ctm.stwb.openapi.QuerySettledDetailModel;
import com.yonyoucloud.ctm.stwb.openapi.ResponseResult;
import com.yonyou.yonbip.ctm.remote.RemoteDubbo;
import com.yonyoucloud.fi.cm.transferaccount.TransferAccount;
import com.yonyoucloud.fi.cmp.constant.IDomainConstant;
import com.yonyoucloud.fi.cmp.util.business.StwbBillBuilder;
import lombok.extern.slf4j.Slf4j;
import org.imeta.orm.base.BizObject;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Description 转账单推送资金结算接口实现
 * @Author guanshaoting
 * @Date 2023/4/6
 **/
@Service
@Slf4j
public class TransferAccountPushServiceImpl implements ITransferAccountPushService {
    /**
     * 推送转账单数据
     * @param billList
     * @throws Exception
     */
    @Override
    public void pushBill(List<BizObject> billList, boolean bCheck, int settlemode, String checkBillNo) throws Exception {
        if (billList == null) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102209"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180386","推送单据不可为空") /* "推送单据不可为空" */);
        }
        //1.构建转账单推送信息
        TransferAccount transferAccount = (TransferAccount) billList.get(0);
        Map<String, Object> params = new HashMap<>();
        params.put("bCheck", bCheck);
        List<DataSettled> dataSettleds = StwbBillBuilder.builderTransferData(transferAccount, params, settlemode, checkBillNo);
        //2.推送单据至资金结算
        builtSystem(dataSettleds);
    }

    /**
     * 删除转账单数据
     * @param billList
     * @throws Exception
     */
    @Override
    public void deleteBill(List<BizObject> billList) throws Exception {
        if (billList == null) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102209"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180386","推送单据不可为空") /* "推送单据不可为空" */);
        }
        try {
            TransferAccount  transferAccount = (TransferAccount) billList.get(0);
//            //待结算数据id为空时，不调用待结算数据删除接口
//            if (transferAccount.getPaymentid() == null && transferAccount.getCollectid() == null) {
//                return;
//            }
            QuerySettledDetailModel querySettledDetailModel = new QuerySettledDetailModel();
            querySettledDetailModel.setBusinessId(transferAccount.getId().toString());//业务单据ID -> 转账单-ID
            List detailsIds = new ArrayList();
            detailsIds.add(transferAccount.getId().toString());
            querySettledDetailModel.setBusinessDetailsIds(detailsIds);//业务单据明细ID -> 转账单-ID
            querySettledDetailModel.setWdataorigin(8);// 来源业务系统，现金管理
            RemoteDubbo.get(IOpenApiService.class, IDomainConstant.MDD_DOMAIN_STWB).datasettledDelete(querySettledDetailModel);
        } catch (Exception e) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102210"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180387","删除结算单据失败：") /* "删除结算单据失败：" */ + e.getMessage());
        }

    }

    /**
     * 推送转账单数据
     * @param dataSettleds
     * @throws Exception
     */
    private void builtSystem(List<DataSettled> dataSettleds) {
        try {
            log.error("转账单推送结算单的入参:{}", CtmJSONObject.toJSONString(dataSettleds));
            ResponseResult responseResult = RemoteDubbo.get(IOpenApiService.class, IDomainConstant.MDD_DOMAIN_STWB).builtSystem(dataSettleds);
            if (responseResult.getCode() == 200) {
                log.info("转账单推送成功");
            } else {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102211"),responseResult.getMessage());
            }
        } catch (Exception e) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102212"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180388","推送结算单据失败：") /* "推送结算单据失败：" */+ e.getMessage());
        }
    }
}
