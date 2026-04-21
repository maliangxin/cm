package com.yonyoucloud.fi.cmp.transferaccount.service;

import com.yonyou.diwork.exception.BusinessException;
import com.yonyou.iuap.event.model.BusinessEvent;
import com.yonyou.iuap.event.rpc.IEventReceiveService;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.option.model.vo.EventResponseVO;
import com.yonyou.workbench.util.JsonUtils;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.ctm.stwb.openapi.DataSettledDetail;
import com.yonyoucloud.ctm.stwb.stwbentity.BusinessBillType;
import com.yonyoucloud.fi.cm.transferaccount.TransferAccount;
import com.yonyoucloud.fi.cmp.cmpentity.SettleStatus;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.imeta.orm.base.EntityStatus;
import org.springframework.stereotype.Service;


/**
 * 转账工作台-结算工作台_监听事件
 */
@Slf4j
@Service
public class BindingTransferaccountListener implements IEventReceiveService {

    static final String RELATION_SETTLE= "relationSettle";
    @Override
    public String onEvent(BusinessEvent businessEvent, String s) throws BusinessException {
        String callInfo = businessEvent.getUserObject();
        String action = businessEvent.getAction();
        try{
            DataSettledDetail dataSettledDetail = CtmJSONObject.parseObject(callInfo, DataSettledDetail.class);
                if (String.valueOf(BusinessBillType.TransferBill.getValue()).equals(dataSettledDetail.getBusinessBillType())) {
                    if(RELATION_SETTLE.equals(action)){
                        TransferAccount transferAccount = MetaDaoHelper.findById(TransferAccount.ENTITY_NAME, dataSettledDetail.getBusinessBillId());
                        if (transferAccount != null && SettleStatus.SettledRep != transferAccount.getSettlestatus()) {
                            transferAccount.setEntityStatus(EntityStatus.Update);
                            transferAccount.set("_entityName", TransferAccount.ENTITY_NAME);
                            if("2".equals(dataSettledDetail.getRecpaytype())){//  2 付款 1收款
                                transferAccount.setAssociationStatusPay(Boolean.TRUE);//付款是否关联
                                if(ObjectUtils.isNotEmpty(dataSettledDetail.getRelateBankCheckBillId())){
                                    transferAccount.setPaybankbill(dataSettledDetail.getRelateBankCheckBillId());//付款银行对账单id
                                }
                                if(ObjectUtils.isNotEmpty(dataSettledDetail.getRelateClaimBillId())){
                                    transferAccount.setPaybillclaim(dataSettledDetail.getRelateClaimBillId());//付款认领单id
                                }

                            } else {
                                transferAccount.setAssociationStatusCollect(Boolean.TRUE);//收款是否关联
                                if(ObjectUtils.isNotEmpty(dataSettledDetail.getRelateBankCheckBillId())){
                                    transferAccount.setCollectbankbill(dataSettledDetail.getRelateBankCheckBillId());//收款银行对账单id
                                    transferAccount.setSmartcheckno(dataSettledDetail.getCheckIdentificationCode());
                                }
                                if(ObjectUtils.isNotEmpty(dataSettledDetail.getRelateClaimBillId())){
                                    transferAccount.setCollectbillclaim(dataSettledDetail.getRelateClaimBillId());//收款认领单id
                                    transferAccount.setSmartcheckno(dataSettledDetail.getCheckIdentificationCode());
                                }
                            }
                            MetaDaoHelper.update(TransferAccount.ENTITY_NAME, transferAccount);
                        }
                    }
                }
        } catch (Exception e) {
            log.error("BindingTransferaccountListener.error:userObject = {}, e = {}", callInfo, e);
            return JsonUtils.toJsonString(EventResponseVO.fail(e.getMessage()));
        }

        return JsonUtils.toJsonString(EventResponseVO.success());
    }
}
