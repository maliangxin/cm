package com.yonyoucloud.fi.cmp.internaltransferprotocol.rule.check;

import com.yonyou.ucf.basedoc.model.CurrencyTenantDTO;
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
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.internaltransferprotocol.enums.TransferOutAccountAllocation;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import com.yonyoucloud.fi.cmp.util.business.CmpCommonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.orm.base.BizObject;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * <h1>EnterpriseBankAccountNameCheck</h1>
 *
 * @author Sun GuoCai
 * @version 1.0
 * @since 2023-09-08 16:28
 */
@Component("protocolBillEnterpriseBankAccountNameCheckRule")
@Slf4j
@RequiredArgsConstructor
public class ProtocolBillEnterpriseBankAccountNameCheckRule extends AbstractCommonRule {

    private final BaseRefRpcService baseRefRpcService;



    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        BillDataDto dataDto = (BillDataDto) getParam(paramMap);
        CtmJSONObject item = CtmJSONObject.parseObject(dataDto.getItem());
        BizObject bizObject = getBills(billContext, paramMap).get(0);
        String itemKey = item.getString(ICmpConstant.WORD_KEY);
        if (ICmpConstant.ENTERPRISE_BANK_ACCOUNT_LOWER_NAME.equals(itemKey)) {
            return handleEnterpriseBankAccountLowerName(bizObject);
        }
        if (ICmpConstant.ENTERPRISEBANKACCOUNT_ACCOUNT.equals(itemKey)) {
            return handleEnterpriseBankAccountAccount(dataDto, bizObject,billContext);
        }
        return new RuleExecuteResult();
    }

    private RuleExecuteResult handleEnterpriseBankAccountLowerName(BizObject bizObject) throws Exception {
        String enterpriseBankAccountName = bizObject.get(ICmpConstant.ENTERPRISE_BANK_ACCOUNT_LOWER_NAME);
        if (enterpriseBankAccountName == null) {
            bizObject.set(ICmpConstant.ACCT_OPEN_TYPE, null);
            return new RuleExecuteResult();
        }
        String enterpriseBankAccount = bizObject.get(ICmpConstant.ENTERPRISE_BANK_ACCOUNT_LOWER);
        if (enterpriseBankAccount == null) {
            bizObject.set(ICmpConstant.ACCT_OPEN_TYPE, null);
            return new RuleExecuteResult();
        }
        EnterpriseParams enterpriseParams = new EnterpriseParams();
        enterpriseParams.setId(enterpriseBankAccount);
        List<EnterpriseBankAcctVO> bankAccounts = baseRefRpcService.queryEnterpriseBankAccountByCondition(enterpriseParams);
        if (!bankAccounts.isEmpty()){
            CmpCommonUtil.queryBankAcctVOByParams(bizObject, bankAccounts);
        }else {
            return new RuleExecuteResult();
        }
        String currency = bizObject.get("currency");
        CurrencyTenantDTO currencyTenantDTO = baseRefRpcService.queryCurrencyById(currency);
        bizObject.set("currency",currencyTenantDTO.getId());
        bizObject.set("currency_name",currencyTenantDTO.getName());
        bizObject.set("currency_priceDigit",currencyTenantDTO.getPricedigit());
        bizObject.set("currency_moneyDigit",currencyTenantDTO.getMoneydigit());
        return new RuleExecuteResult();
    }

    private RuleExecuteResult handleEnterpriseBankAccountAccount(BillDataDto dataDto, BizObject bizObject,BillContext billContext) throws Exception {
        CtmJSONObject item = CtmJSONObject.parseObject(dataDto.getItem());
        Integer location = item.getInteger("location");
        String childrenFieldCheck = MetaDaoHelper.getChilrenField(billContext.getFullname());
        List<BizObject> linesCheck = bizObject.get(childrenFieldCheck);
        BizObject subObj = linesCheck.get(location);
        String enterpriseBankAccountAccount = subObj.get(ICmpConstant.ENTERPRISEBANKACCOUNT_ACCOUNT);
        if (enterpriseBankAccountAccount == null) {
            subObj.set(ICmpConstant.ENTERPRISE_BANK_ACCOUNT, null);
            return new RuleExecuteResult();
        }
        String enterpriseBankAccount = subObj.get(ICmpConstant.ENTERPRISE_BANK_ACCOUNT);
        if (enterpriseBankAccount == null) {
            subObj.set(ICmpConstant.ENTERPRISE_BANK_ACCOUNT, null);
            return new RuleExecuteResult();
        }
        if (!ValueUtils.isNotEmptyObj(bizObject.get(ICmpConstant.CURRENCY))){
            subObj.set(ICmpConstant.ENTERPRISE_BANK_ACCOUNT, null);
            subObj.set(ICmpConstant.ENTERPRISEBANKACCOUNT_ACCOUNT, null);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100242"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1932EBC204080083", "请先填写表头上的币种！") /* "请先填写表头上的币种！" */);
        }
        Short transferOutAccountAllocation = bizObject.getShort("transferOutAccountAllocation");
        if (transferOutAccountAllocation == TransferOutAccountAllocation.SETUP_ACCOUNT_MANUALLY.getValue()
                && !ValueUtils.isNotEmptyObj(bizObject.get(ICmpConstant.ENTERPRISE_BANK_ACCOUNT_LOWER))){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100243"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1932EBC20408007F", "请先填写表头上的银行账户！") /* "请先填写表头上的银行账户！" */);
        }

        String enterpriseBankAccountMain = bizObject.get(ICmpConstant.ENTERPRISE_BANK_ACCOUNT_LOWER);
        if (ValueUtils.isNotEmptyObj(enterpriseBankAccountMain) && enterpriseBankAccountMain.equals(enterpriseBankAccount)){
            //2024-04-08 适配河北建工场景 如果YMS不包含当前参数 或者 有参数 标识不允许相同
            String isSameAccount = AppContext.getEnvConfig(ICmpConstant.INTERNALTRANS_YMS_SAMEACCOUNT);
            if (StringUtils.isEmpty(isSameAccount) || !Boolean.parseBoolean(isSameAccount)) {
                subObj.set(ICmpConstant.ENTERPRISE_BANK_ACCOUNT, null);
                subObj.set(ICmpConstant.ENTERPRISEBANKACCOUNT_ACCOUNT, null);
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100244"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1932EBC204080081", "明细行上的银行账号不能与表头上的银行账号相等！") /* "明细行上的银行账号不能与表头上的银行账号相等！" */);
            }
        }
        EnterpriseParams enterpriseParams = new EnterpriseParams();
        enterpriseParams.setId(enterpriseBankAccount);
        List<EnterpriseBankAcctVO> bankAccounts = baseRefRpcService.queryEnterpriseBankAccountByCondition(enterpriseParams);
        if (!bankAccounts.isEmpty()){
            EnterpriseBankAcctVO enterpriseBankAcctVO = bankAccounts.get(0);
            Integer acctOpenType = enterpriseBankAcctVO.getAcctopentype();
            Short acctOpenTypeMain = bizObject.getShort(ICmpConstant.ACCT_OPEN_TYPE);
            if (!ValueUtils.isNotEmptyObj(acctOpenType)){
                subObj.set(ICmpConstant.ENTERPRISE_BANK_ACCOUNT, null);
                subObj.set(ICmpConstant.ENTERPRISEBANKACCOUNT_ACCOUNT, null);
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100245"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1932EBC204080080", "明细行上填写的银行账号对应的开户类型为空！") /* "明细行上填写的银行账号对应的开户类型为空！" */);
            }
            // 转出方和转入方的账户，需要银行类别同时为商业银行账户或结算中心账户。否则提示：转出方和转入方账户商业银行或结算中心账户的银行类别需要一致
            CmpCommonUtil.verifyAcctOpenTypeIsSame(subObj, acctOpenType, acctOpenTypeMain);
        }
        return new RuleExecuteResult();
    }
}
