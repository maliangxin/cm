package com.yonyoucloud.fi.cmp.margincommon.service;

import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.ctm.stwb.openapi.IOpenApiService;
import com.yonyoucloud.ctm.stwb.openapi.QuerySettledDetailModel;
import com.yonyou.yonbip.ctm.remote.RemoteDubbo;
import com.yonyoucloud.fi.cmp.constant.IDomainConstant;
import com.yonyoucloud.fi.cmp.fundcommon.service.StwbBillCommonService;
import com.yonyoucloud.fi.cmp.receivemargin.ReceiveMargin;
import com.yonyoucloud.fi.cmp.stwb.IReceiveMarginPushStwbBillService;
import com.yonyoucloud.fi.cmp.util.business.StwbBillBuilder;
import lombok.extern.slf4j.Slf4j;
import org.imeta.orm.base.BizObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 保证金台账管理 推送资金结算*
 *
 * @author xuxbo
 * @date 2023/8/4 10:06
 */
@Slf4j
@Service
@Transactional
public class StwbReceiveMarginServiceImpl extends StwbBillCommonService implements IReceiveMarginPushStwbBillService {

    // 收到保证金
    private static final String CMP_RECEIVEMARGIN = "cmp_receivemargin";

    /**
     * 审核推送资金结算
     * * *
     * @param billList
     * @param bCheck
     * @throws Exception
     */
    @Override
    public void pushBill(List<BizObject> billList, boolean bCheck) throws Exception {
        if (billList == null) {
            return;
        }
        ReceiveMargin receiveMargin = (ReceiveMargin) billList.get(0);
        Map<String, Object> params = new HashMap<>();
        params.put("bCheck", bCheck);
        saveDataSettleds(StwbBillBuilder.builderReceivemargin(receiveMargin, params), CMP_RECEIVEMARGIN);
    }

    @Override
    public void pushBillSimple(BizObject bizobject) throws Exception {

    }

    /**
     * 弃审删除资金结算
     * * *
     * @param billList
     * @throws Exception
     */
    @Override
    public void deleteBill(List<BizObject> billList) throws Exception {

        if (billList == null) {
            return;
        }
        ReceiveMargin receiveMargin = (ReceiveMargin) billList.get(0);
        try {
            QuerySettledDetailModel querySettledDetailModel = new QuerySettledDetailModel();
            querySettledDetailModel.setBusinessId(receiveMargin.getId().toString());
            querySettledDetailModel.setWdataorigin(8);// 来源业务系统，现金管理
            List<String> ids = new ArrayList<>();
            ids.add(receiveMargin.getId().toString());
            querySettledDetailModel.setBusinessDetailsIds(ids);
            RemoteDubbo.get(IOpenApiService.class, IDomainConstant.MDD_DOMAIN_STWB).datasettledDelete(querySettledDetailModel);
        } catch (Exception e) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100347"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418076B","删除结算单据失败：") /* "删除结算单据失败：" */ + e.getMessage());
        }

    }
}
