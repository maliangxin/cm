package com.yonyoucloud.fi.cmp.reconciliate;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.FilterVO;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.SimpleFilterVO;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.dao.meta.UiMetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.basecom.utils.FIDubboUtils;
import com.yonyoucloud.fi.cmp.bankreconciliationsetting.BankReconciliationSetting;
import com.yonyoucloud.fi.cmp.cmpcheck.service.CmpCheckService;
import com.yonyoucloud.fi.cmp.cmpentity.EnableStatus;
import com.yonyoucloud.fi.cmp.constant.IBussinessConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import com.yonyoucloud.fi.cmp.util.StringUtils;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import com.yonyoucloud.fi.cmp.common.CtmException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.BizObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
@Component
@Slf4j
public class ReconciliationVerification extends AbstractCommonRule {
    public static String CMP_BANKRECONCILIATIONSETREF = "cm_bankreconciliationsetref";
    public static String BD_ENTERPRISEBANKACCTREF = "ucfbasedoc.bd_enterprisebankacctref";
    public static String BANKRECONCILIATIONSCHEME = "bankreconciliationscheme";
    public static String CMP_BALANCEADJUST = "cmp_balanceadjust";
    public static String BANKRECONCILIATIONSETTING_B = "bankReconciliationSetting_b";
    public static String ENABLESTATUS = "enableStatus";

    @Autowired
    private CmpCheckService cmpCheckService;
    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        BillDataDto bill = (BillDataDto) getParam(paramMap);
        FilterVO filterVO = bill.getCondition();
        if(null == filterVO){
             filterVO = new FilterVO();
        }
        if (bill.getrefCode().equals(CMP_BANKRECONCILIATIONSETREF)){
			if (bill.getData()!=null) {
                List<Map<String,Object>> list = (List<Map<String,Object>>) bill.getData();
                String accentity;
                if (list.size() > 0){
                    accentity= (String) list.get(0).get(ICmpConstant.ACCENTITY);
                }else {
                    if (paramMap.get("requestData") != null){
                        CtmJSONObject c = CtmJSONObject.parseObject(paramMap.get("requestData").toString());
                        accentity = c.getString("accentity");
                    }else {
                        accentity = bill.getMasterOrgValue();
                    }
                }
                if(FIDubboUtils.isSingleOrg()){
                    BizObject singleOrg = FIDubboUtils.getSingleOrg();
                    if(singleOrg != null){
                        accentity = singleOrg.get("id");
                    }
                }
                if(StringUtils.isEmpty(accentity)){
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102594"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1BBBAC7004C00008","请先选择对账组织！") /* "请先选择对账组织！" */);
                }
                UiMetaDaoHelper.appendCondition(filterVO, IBussinessConstant.ACCENTITY, ICmpConstant.QUERY_EQ, accentity);
                if (!CMP_BALANCEADJUST.equals(billContext.getBillnum())) {
//                    UiMetaDaoHelper.appendCondition(filterVO, ENABLESTATUS, ICmpConstant.QUERY_EQ, EnableStatus.Enabled.getValue());
                }
            }
		}else if (bill.getrefCode().equals(BD_ENTERPRISEBANKACCTREF) || bill.getrefCode().equals("ucfbasedoc.bd_enterprisebankacct")){
            if (bill.getData()!=null) {
                List<Map<String,Object>> list = (List<Map<String,Object>>) bill.getData();
                String accentity;
                String bankreconciliationschemeid;
                boolean checkflag = false;
                if (list.size() > 0){
                    accentity= (String) list.get(0).get(ICmpConstant.ACCENTITY);
                    bankreconciliationschemeid= String.valueOf(list.get(0).get(BANKRECONCILIATIONSCHEME));
                    checkflag = list.get(0).get("checkflag") != null ?  (Boolean) list.get(0).get("checkflag") : false;
                }else {
                    if (paramMap.get("requestData") != null && paramMap.get("requestData") instanceof List && ((List) paramMap.get("requestData")).size() > 0) {
                        CtmJSONObject c = CtmJSONObject.parseObject(paramMap.get("requestData") != null ? paramMap.get("requestData").toString() : "");
                        accentity = c.getString("accentity");
                        bankreconciliationschemeid = c.getString("bankreconciliationscheme");
                        checkflag = c.getBoolean("checkflag");
                    } else {
                        accentity = "";
                        bankreconciliationschemeid = "";
                    }
                }
                if(FIDubboUtils.isSingleOrg()){
                    BizObject singleOrg = FIDubboUtils.getSingleOrg();
                    if(singleOrg != null){
                        accentity = singleOrg.get("id");
                    }
                }
                if(StringUtils.isEmpty(accentity)){
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102594"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1BBBAC7004C00008","请先选择对账组织！") /* "请先选择对账组织！" */);
                }
                if(StringUtils.isEmpty(bankreconciliationschemeid) || bankreconciliationschemeid.equals("null")){
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102595"),com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage("P_YS_FI_CM_0001009299") /* "请先选择对账方案！" */);
                }
                BizObject bizObject = MetaDaoHelper.findById(BankReconciliationSetting.ENTITY_NAME,Long.parseLong(bankreconciliationschemeid));
                if(ValueUtils.isEmpty(bizObject)){
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102596"),com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage("P_YS_FI_CM_0001009299") /* "请先选择对账方案！" */);
                }
                List<BizObject> bankList = bizObject.get(BANKRECONCILIATIONSETTING_B);
                List<Object> bankaccounts = new ArrayList<Object>();
                SimpleFilterVO filterOr = new SimpleFilterVO(ConditionOperator.or);
                for(BizObject bankReconciliationSetting_b :bankList){
                    //202505 增加是否勾对=否时的停用明细过滤
                    if (bankReconciliationSetting_b.getShort("enableStatus_b") == EnableStatus.Disabled.getValue()
                            && !checkflag){
                        continue;
                    }
                    String bankaccount = bankReconciliationSetting_b.get(ICmpConstant.BANKACCOUNT);
                    bankaccounts.add(bankaccount);
                    SimpleFilterVO filterAnd = new SimpleFilterVO(ConditionOperator.and);
                    String bankAccountCurrency = bankReconciliationSetting_b.get(ICmpConstant.CURRENCY);
                    filterAnd.addCondition(new SimpleFilterVO(ICmpConstant.PRIMARY_ID, ICmpConstant.QUERY_EQ, bankaccount));
                    filterAnd.addCondition(new SimpleFilterVO(ICmpConstant.CURRENCY_REf, ICmpConstant.QUERY_EQ, bankAccountCurrency));
                    filterOr.addCondition(filterAnd);
                }
                //为空时代表没有可以查询的数据
                if (bankaccounts.size() == 0){
                    SimpleFilterVO filterAnd = new SimpleFilterVO(ConditionOperator.and);
                    filterAnd.addCondition(new SimpleFilterVO(ICmpConstant.PRIMARY_ID, ICmpConstant.QUERY_EQ, "0"));
                    filterAnd.addCondition(new SimpleFilterVO(ICmpConstant.CURRENCY_REf, ICmpConstant.QUERY_EQ, "0"));
                    filterOr.addCondition(filterAnd);
                }
                //银行流水对账银行账户数据权限适配
                try {
                    String[] bankAccountPermissions = cmpCheckService.getBankAccountDataPermission(IServicecodeConstant.BANKRECONCILIATION);
                    if(bankAccountPermissions != null && bankAccountPermissions.length > 0){
                        UiMetaDaoHelper.appendCondition(filterVO, "id", ICmpConstant.QUERY_IN, Arrays.asList(bankAccountPermissions));
                    }
                }catch (Exception e){
                    log.error("获取数据权限时错误" + e);
                }
                filterVO.appendCondition(ConditionOperator.and, filterOr);
            }
        }
        bill.setCondition(filterVO);
        putParam(paramMap, bill);
        return new RuleExecuteResult();
    }
}
