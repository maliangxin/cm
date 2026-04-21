package com.yonyoucloud.fi.cmp.accountregularbalance.rule;

import com.yonyou.iuap.yms.lock.YmsLock;
import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVO;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.accountfixedbalance.AccountFixedBalance;
import com.yonyoucloud.fi.cmp.constant.IBussinessConstant;
import com.yonyoucloud.fi.cmp.enums.BalanceAccountDataSourceEnum;
import com.yonyoucloud.fi.cmp.util.JedisLockUtils;
import com.yonyoucloud.fi.cmp.util.basedoc.EnterpriseBankQueryService;
import com.yonyoucloud.fi.cmp.util.business.CmpCommonUtil;
import lombok.extern.slf4j.Slf4j;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Slf4j
@Component("beforeSaveFixedBalanceRule")
public class BeforeSaveFixedBalanceRule extends AbstractCommonRule {

    @Autowired
    EnterpriseBankQueryService enterpriseBankQueryService;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        List<AccountFixedBalance> bills = getBills(billContext, paramMap);
        BillDataDto billDataDto = (BillDataDto) getParam(paramMap);
        // 导入
        boolean importFlag =  "import".equals(billDataDto.getRequestAction());

        List<YmsLock> ymsLockList = new ArrayList<>();
        if (bills != null && bills.size()>0) {
            AccountFixedBalance balance = bills.get(0);

            if (importFlag) {
                balance.setDatasource(BalanceAccountDataSourceEnum.MANUAL_IMPORT.getCode());
            }else {
                balance.setDatasource(BalanceAccountDataSourceEnum.MANUAL_ENTRY.getCode());
            }
            if (balance.getIsconfirm() == null) {
                // 已确认状态赋默认值
                balance.setIsconfirm(false);
            }
            // 余额日期校验
            Date balanceData = balance.getBalancedate();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd HH:mm:ss");
            Date nowDate = new Date();
            if (balanceData.after(nowDate)) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100550"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_19C695180538000C","余额日期不能大于当前服务器日期，请检查") /* "余额日期不能大于当前服务器日期，请检查" */);
            }
            String enterpriseBankAccount = balance.getEnterpriseBankAccount();
            String currency = balance.getCurrency();
            String balanceDateStr = sdf.format(balanceData);
            String lockStr = "fixedBalanceLock_" + balance.getAccentity() + enterpriseBankAccount + currency + balanceDateStr;
            YmsLock ymsLock;
            if (importFlag) {
                if ((ymsLock= JedisLockUtils.lockRuleWithOutTrace(lockStr,paramMap))==null) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100323"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041803B7","导入数据重复，请检查后重试！") /* "导入数据重复，请检查后重试！" */);
                }
                ymsLockList.add(ymsLock);
            }
            // 查询企业银行账户实体
            EnterpriseBankAcctVO enterpriseBankAcctVO = enterpriseBankQueryService.findById(balance.getEnterpriseBankAccount());
            //此接口查询不到未启用的账户，故需要判空
            if (enterpriseBankAcctVO == null) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100321"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B000E3", "银行账号为:") /* "银行账号为:" */+balance.get("enterpriseBankAccount_account")+com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B000E2", "的账户未启用,请检查!") /* "的账户未启用,请检查!" */);
            }
            // 校验：银行账户启用，银行账户、币种要在对应会计主体下
            if(balance.get("enterpriseBankAccount")!=null){
                //银行账户
                Integer enable = enterpriseBankAcctVO.getEnable();
                if(1!=enable){
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100321"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B000E3", "银行账号为:") /* "银行账号为:" */+enterpriseBankAcctVO.getAccount()+com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B000E2", "的账户未启用,请检查!") /* "的账户未启用,请检查!" */);
                }
                CmpCommonUtil.checkBankAcctCurrency(balance.get("enterpriseBankAccount"), balance.get("currency"));
            }
            QuerySchema schema = QuerySchema.create().addSelect("*");
            QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
            if (balance.getAccentity() != null) {
                conditionGroup.appendCondition(QueryCondition.name(IBussinessConstant.ACCENTITY).eq(balance.getAccentity()));
            }
            conditionGroup.appendCondition(QueryCondition.name("enterpriseBankAccount").eq(balance.getEnterpriseBankAccount()));
            conditionGroup.appendCondition(QueryCondition.name("currency").eq(balance.getCurrency()));
            conditionGroup.appendCondition(QueryCondition.name("balancedate").eq(balance.getBalancedate()));
            schema.addCondition(conditionGroup);
            List<AccountFixedBalance> existBalances = MetaDaoHelper.queryObject(AccountFixedBalance.ENTITY_NAME, schema, null);
            existBalances.forEach(e -> {
                if (e.getIsconfirm()) {
                    if (importFlag) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100551"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_19DEAAC404180010","[%s]账户的日期[%s]的数据已进行确认，不允许导入，请检查！") /* "[%s]账户的日期[%s]的数据已进行确认，不允许导入，请检查！" */,enterpriseBankAcctVO.getAcctName(),sdf.format(balanceData)));
                    } else {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100552"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_19DEAAC404180011","系统中[%s]账号已存在相同币种、相同日期的数据，请检查！") /* "系统中[%s]账号已存在相同币种、相同日期的数据，请检查！" */,enterpriseBankAcctVO.getAcctName()));
                    }
                } else {
                    if (balance.get("_status") != null && EntityStatus.Insert.equals(balance.get("_status"))) {
                        if (importFlag) {
                            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100552"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_19DEAAC404180011","系统中[%s]账号已存在相同币种、相同日期的数据，请检查！") /* "系统中[%s]账号已存在相同币种、相同日期的数据，请检查！" */,enterpriseBankAcctVO.getAcctName()));
                        } else {
                            // 会计主体，银行账户，币种，余额日期，完全相同的数据，未确认的直接覆盖
                            try {
                                MetaDaoHelper.delete(AccountFixedBalance.ENTITY_NAME, Long.parseLong(e.getId().toString()));
                            } catch (Exception ex) {
                                log.error(ex.getMessage());
                            }
                        }
                    }
                }
            });
        }
        return new RuleExecuteResult();
    }

}
