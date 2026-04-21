package com.yonyoucloud.fi.cmp.fundcommon.check;

import com.yonyou.iuap.bizdoc.service.model.SettleMethodModel;
import com.yonyou.iuap.bizdoc.service.model.SettleMethodQueryParam;
import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVO;
import com.yonyou.ucf.basedoc.model.rpcparams.EnterpriseParams;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.cmp.cmpentity.AssociationStatus;
import com.yonyoucloud.fi.cmp.cmpentity.EventType;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import com.yonyoucloud.fi.cmp.util.basedoc.EnterpriseBankQueryService;
import com.yonyoucloud.fi.tmsp.openapi.ITmspRefRpcService;
import com.yonyoucloud.fi.tmsp.vo.TmspRequestParams;
import com.yonyoucloud.fi.tmsp.vo.VirtualAccountDTO;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.orm.base.BizObject;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;


/**
 * <h1>结算方式改为第三方</h1>
 *
 * @author Sun GuoCai
 * @version 1.0
 * @since 2023/3/21 15:57
 */
@Component("fundBillSettleModeThirdPartyCheckRule")
@RequiredArgsConstructor
public class FundBillSettleModeThirdPartyCheckRule extends AbstractCommonRule {
    private final BaseRefRpcService baseRefRpcService;
    private final EnterpriseBankQueryService enterpriseBankQueryService;
    private final ITmspRefRpcService tmspRefRpcService;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        BillDataDto dataDto = (BillDataDto) getParam(paramMap);
        CtmJSONObject item = CtmJSONObject.parseObject(dataDto.getItem());
        if (!ICmpConstant.SETTLE_MODE_NAME.equals(item.get(ICmpConstant.WORD_KEY))) {
            return new RuleExecuteResult();
        }
        BizObject bill = getBills(billContext, paramMap).get(0);
        String childrenFieldCheck = MetaDaoHelper.getChilrenField(billContext.getFullname());
        List<BizObject> linesCheck = (List) bill.get(childrenFieldCheck);
        for (BizObject subObj : linesCheck) {
            Object settleMode = subObj.get(ICmpConstant.SETTLE_MODE);
            if (!ValueUtils.isNotEmptyObj(settleMode)){

                boolean isBankReconciliation = bill.getShort("billtype") == EventType.CashMark.getValue() || bill.getShort("billtype") == EventType.BillClaim.getValue();// 是否是银行对账单或认领单
                Short associationStatus = subObj.getShort("associationStatus");
                if(!isBankReconciliation && !Objects.isNull(associationStatus) && associationStatus == AssociationStatus.NoAssociated.getValue()) {
                    subObj.set("enterprisebankaccount", null);
                    subObj.set("enterprisebankaccount_name", null);
                    subObj.set("enterprisebankaccount_account", null);
                    subObj.set("enterprisebankaccount_code", null);
                }
                subObj.set("thirdParVirtAccount", null);
                subObj.set("thirdParVirtAccount_name", null);
                subObj.set("thirdParVirtAccount_code", null);
                subObj.set("cashaccount", null);
                subObj.set("cashaccount_name", null);
                subObj.set("notetype", null);
                subObj.set("notetype_billtypeno", null);
                subObj.set("notetype_billtypename", null);
                subObj.set("noteno", null);
                subObj.set("noteno_noteno", null);
                subObj.set("noteDirection", null);
                subObj.set("notetextno", null);
                subObj.set("noteSum", null);
                subObj.set("checkPurpose", null);
                subObj.set("checkno", null);
                subObj.set("checkId", null);

//                subObj.set("incomeAndExpendBankAccount_account", null);
//                subObj.set("incomeAndExpendBankAccount", null);
//                subObj.set("incomeAndExpendRelationGroup_mainid_rsgroupname", null);
//                subObj.set("incomeAndExpendRelationGroup", null);
//                subObj.set("isIncomeAndExpenditure", false);
            } else {
                SettleMethodQueryParam settleMethodQueryParam = new SettleMethodQueryParam();
                settleMethodQueryParam.setId((Long) settleMode);
                settleMethodQueryParam.setIsEnabled(ICmpConstant.CONSTANT_ONE);
                settleMethodQueryParam.setTenantId(AppContext.getTenantId());
                List<SettleMethodModel> dataList = baseRefRpcService.querySettleMethods(settleMethodQueryParam);
                if (dataList != null && dataList.size() > ICmpConstant.CONSTANT_ZERO) {
                    // 第三方
                    if (dataList.get(ICmpConstant.CONSTANT_ZERO).getServiceAttr().equals(ICmpConstant.CONSTANT_TEN)) {
//                        EnterpriseParams enterpriseParams = new EnterpriseParams();
//                        List<String> currencyIDList = new ArrayList<>();
//                        currencyIDList.add(bill.get(ICmpConstant.CURRENCY));
//                        enterpriseParams.setCurrencyIDList(currencyIDList);
//                        List<EnterpriseBankAcctVO> virtualAccounts = enterpriseBankQueryService.getVirtualAccountInfo(enterpriseParams);
//                        if (CollectionUtils.isNotEmpty(virtualAccounts) && virtualAccounts.size() == 1) {
//                            EnterpriseBankAcctVO virtualAccount = virtualAccounts.get(ICmpConstant.CONSTANT_ZERO);
//                            subObj.set(ICmpConstant.THIRD_PAR_VIRT_ACCOUNT, virtualAccount.getId());
//                            subObj.set(ICmpConstant.THIRDPARVIRTACCOUNT_NAME, virtualAccount.getAcctName());
//                        }
                        subObj.set("enterprisebankaccount", null);
                        subObj.set("enterprisebankaccount_name", null);
                        subObj.set("enterprisebankaccount_account", null);
                        subObj.set("enterprisebankaccount_code", null);
                        subObj.set("cashaccount", null);
                        subObj.set("cashaccount_name", null);
                        subObj.set("notetype", null);
                        subObj.set("notetype_billtypeno", null);
                        subObj.set("notetype_billtypename", null);
                        subObj.set("noteno", null);
                        subObj.set("noteno_noteno", null);
                        subObj.set("noteDirection", null);
                        subObj.set("notetextno", null);
                        subObj.set("noteSum", null);
                        subObj.set("checkPurpose", null);
                        subObj.set("checkno", null);
                        subObj.set("checkId", null);
//                        subObj.set("isIncomeAndExpenditure", true);
                        subObj.set("settleCurrency", bill.get("currency"));
                        subObj.set("settleCurrency_name", bill.get("currency_name"));
                        subObj.set("settleCurrency_priceDigit", bill.get("currency_priceDigit"));
                        subObj.set("settleCurrency_moneyDigit", bill.get("currency_moneyDigit"));
                        subObj.set("settleCurrency_moneyDigit", bill.get("currency_moneyDigit"));
                        subObj.set("settleCurrency_moneyDigit", bill.get("currency_moneyDigit"));
                        subObj.set("swapOutExchangeRateEstimate", 1);
                        subObj.set("swapOutAmountEstimate", subObj.get("oriSum"));
                    } else if (dataList.get(ICmpConstant.CONSTANT_ZERO).getServiceAttr().equals(ICmpConstant.CONSTANT_ZERO)) {
                        subObj.set("thirdParVirtAccount", null);
                        subObj.set("thirdParVirtAccount_name", null);
                        subObj.set("thirdParVirtAccount_code", null);
                        subObj.set("cashaccount", null);
                        subObj.set("cashaccount_name", null);
                        subObj.set("notetype", null);
                        subObj.set("notetype_billtypeno", null);
                        subObj.set("notetype_billtypename", null);
                        subObj.set("noteno", null);
                        subObj.set("noteno_noteno", null);
                        subObj.set("noteDirection", null);
                        subObj.set("notetextno", null);
                        subObj.set("noteSum", null);
                        subObj.set("checkPurpose", null);
                        subObj.set("checkno", null);
                        subObj.set("checkId", null);
                    } else if (dataList.get(ICmpConstant.CONSTANT_ZERO).getServiceAttr().equals(ICmpConstant.CONSTANT_ONE)) {
                        subObj.set("enterprisebankaccount", null);
                        subObj.set("enterprisebankaccount_name", null);
                        subObj.set("enterprisebankaccount_account", null);
                        subObj.set("enterprisebankaccount_code", null);
                        subObj.set("thirdParVirtAccount", null);
                        subObj.set("thirdParVirtAccount_name", null);
                        subObj.set("thirdParVirtAccount_code", null);
                        subObj.set("notetype", null);
                        subObj.set("notetype_billtypeno", null);
                        subObj.set("notetype_billtypename", null);
                        subObj.set("noteno", null);
                        subObj.set("noteno_noteno", null);
                        subObj.set("noteDirection", null);
                        subObj.set("notetextno", null);
                        subObj.set("noteSum", null);
                        subObj.set("checkPurpose", null);
                        subObj.set("checkno", null);
                        subObj.set("checkId", null);
                        subObj.set("settleCurrency", bill.get("currency"));
                        subObj.set("settleCurrency_name", bill.get("currency_name"));
                        subObj.set("settleCurrency_priceDigit", bill.get("currency_priceDigit"));
                        subObj.set("settleCurrency_moneyDigit", bill.get("currency_moneyDigit"));
                        subObj.set("swapOutExchangeRateEstimate", 1);
                        subObj.set("swapOutAmountEstimate", subObj.get("oriSum"));
                    }else if (dataList.get(ICmpConstant.CONSTANT_ZERO).getServiceAttr().equals(ICmpConstant.CONSTANT_TWO)) {
                        subObj.set("enterprisebankaccount", null);
                        subObj.set("enterprisebankaccount_name", null);
                        subObj.set("enterprisebankaccount_account", null);
                        subObj.set("enterprisebankaccount_code", null);
                        subObj.set("thirdParVirtAccount", null);
                        subObj.set("thirdParVirtAccount_name", null);
                        subObj.set("thirdParVirtAccount_code", null);
                        subObj.set("cashaccount", null);
                        subObj.set("cashaccount_name", null);
                        subObj.set("checkPurpose", null);
                        subObj.set("checkno", null);
                        subObj.set("checkId", null);
                        subObj.set("settleCurrency", bill.get("currency"));
                        subObj.set("settleCurrency_name", bill.get("currency_name"));
                        subObj.set("settleCurrency_priceDigit", bill.get("currency_priceDigit"));
                        subObj.set("settleCurrency_moneyDigit", bill.get("currency_moneyDigit"));
                        subObj.set("swapOutExchangeRateEstimate", 1);
                        subObj.set("swapOutAmountEstimate", subObj.get("oriSum"));
                    }else if (dataList.get(ICmpConstant.CONSTANT_ZERO).getServiceAttr().equals(8)) {
                        boolean isBankReconciliation = bill.getShort("billtype") == EventType.CashMark.getValue() || bill.getShort("billtype") == EventType.BillClaim.getValue();// 是否是银行对账单或认领单
                        Short associationStatus = subObj.getShort("associationStatus");
                        if(!isBankReconciliation && associationStatus == AssociationStatus.NoAssociated.getValue()) {
                            subObj.set("enterprisebankaccount", null);
                            subObj.set("enterprisebankaccount_name", null);
                            subObj.set("enterprisebankaccount_account", null);
                            subObj.set("enterprisebankaccount_code", null);
                        }
                        subObj.set("thirdParVirtAccount", null);
                        subObj.set("thirdParVirtAccount_name", null);
                        subObj.set("thirdParVirtAccount_code", null);
                        subObj.set("notetype", null);
                        subObj.set("notetype_billtypeno", null);
                        subObj.set("notetype_billtypename", null);
                        subObj.set("noteno", null);
                        subObj.set("noteno_noteno", null);
                        subObj.set("noteDirection", null);
                        subObj.set("notetextno", null);
                        subObj.set("noteSum", null);
                        subObj.set("cashaccount", null);
                        subObj.set("cashaccount_name", null);
                        subObj.set("settleCurrency", bill.get("currency"));
                        subObj.set("settleCurrency_name", bill.get("currency_name"));
                        subObj.set("settleCurrency_priceDigit", bill.get("currency_priceDigit"));
                        subObj.set("settleCurrency_moneyDigit", bill.get("currency_moneyDigit"));
                        subObj.set("swapOutExchangeRateEstimate", 1);
                        subObj.set("swapOutAmountEstimate", subObj.get("oriSum"));
                    }

                    if (!dataList.get(ICmpConstant.CONSTANT_ZERO).getServiceAttr().equals(ICmpConstant.CONSTANT_ZERO)) {
//                        subObj.set("incomeAndExpendBankAccount_account", null);
//                        subObj.set("incomeAndExpendBankAccount", null);
//                        subObj.set("incomeAndExpendRelationGroup_mainid_rsgroupname", null);
//                        subObj.set("incomeAndExpendRelationGroup", null);
//                        subObj.set("isIncomeAndExpenditure", false);
                    }
                }
            }
        }
        return new RuleExecuteResult();
    }
}
