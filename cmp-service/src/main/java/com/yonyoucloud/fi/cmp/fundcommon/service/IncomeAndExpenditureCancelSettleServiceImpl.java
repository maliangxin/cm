package com.yonyoucloud.fi.cmp.fundcommon.service;

import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.yonbip.ctm.error.CtmErrorCode;
import com.yonyoucloud.ctm.stwb.cancelsettle.vo.BillBaseNode;
import com.yonyoucloud.ctm.stwb.cancelsettle.vo.BillDetailNode;
import com.yonyoucloud.fi.cmp.common.service.cancelsettle.CancelSettlementServiceEnum;
import com.yonyoucloud.fi.cmp.common.service.cancelsettle.ICmpOperationService;
import com.yonyoucloud.fi.cmp.constant.IDomainConstant;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.orm.base.BizObject;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class IncomeAndExpenditureCancelSettleServiceImpl implements ICmpOperationService {

    public static final String FUNDPAYMENT_ENTITY_NAME = "cmp.fundpayment.FundPayment";
    public static final String FUNDPAYMENT_B_ENTITY_NAME = "cmp.fundpayment.FundPayment_b";
    public static final String FUNDCOLLECTION_ENTITY_NAME = "cmp.fundcollection.FundCollection";
    public static final String FUNDCOLLECTION_B_ENTITY_NAME = "cmp.fundcollection.FundCollection_b";

    @Override
    public boolean handleCancelSettle(CancelSettlementServiceEnum serviceEnum, BillBaseNode billBaseNode, String reason) throws Exception {
        log.error("统一结算单取消结算handleCancelSettle（统收统支协同单），入参：serviceEnum:{},billBaseNode:{},reason:{}", serviceEnum, billBaseNode, reason);
        //回滚资金收/付款的结算状态为“结算中”
        BizObject bizObject = MetaDaoHelper.findById(FUNDPAYMENT_ENTITY_NAME, billBaseNode.getBillId(), 2);
        if (bizObject == null) {
            return AppContext.getBean(FundCollectionCancelSettleServiceImpl.class).handleCancelSettle(serviceEnum, billBaseNode, reason);
        } else {
            return AppContext.getBean(FundPaymentCancelSettleServiceImpl.class).handleCancelSettle(serviceEnum, billBaseNode, reason);
        }
    }


    @Override
    public BillDetailNode buildCancelSettleNode(String billTypeId, List<String> billDetailIds) {
        log.error("统一结算单取消结算buildCancelSettleNode（统收统支协同单），入参：billTypeId:{},billDetailIds:{}", billTypeId, billDetailIds);
        BillDetailNode billDetailNode = BillDetailNode.builder().domainKey(IDomainConstant.MDD_DOMAIN_CMP).build();
        try {
            if (CollectionUtils.isEmpty(billDetailIds)) {
                throw new com.yonyou.yonbip.ctm.error.CtmException(new CtmErrorCode("033-502-105002"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_208855DE04700003", "取消结算,未返回单据明细ID，请联系研发及时处理！！"));
            }
            //获取单据id
            BizObject bizObject = MetaDaoHelper.findById(FUNDPAYMENT_B_ENTITY_NAME, billDetailIds.get(0));
            if (bizObject != null && bizObject.get("bizobjtype").toString().contains("fundcollection")) {
                //说明是统收统支-资金收款单
                return AppContext.getBean(FundCollectionCancelSettleServiceImpl.class).buildCancelSettleNode(billTypeId, billDetailIds);
            } else {
                return AppContext.getBean(FundPaymentCancelSettleServiceImpl.class).buildCancelSettleNode(billTypeId, billDetailIds);
            }
        } catch (Exception ex) {
            log.error("构造billDetailNode报错：" + ex.getMessage());
            billDetailNode.setCheckStatus(false);
            billDetailNode.setCheckMsg(ex.getMessage());
            return billDetailNode;
        }
    }
}
