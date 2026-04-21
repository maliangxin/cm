package com.yonyoucloud.fi.cmp.bankreconciliationsetting;

import com.yonyou.iuap.org.dto.FinOrgDTO;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.FilterVO;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.SimpleFilterVO;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.UiMetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.basecom.utils.FIDubboUtils;
import com.yonyoucloud.fi.basecom.utils.HttpTookit;
import com.yonyoucloud.fi.cmp.constant.IBussinessConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.util.AccentityUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.BizObject;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 根据业务账簿过滤科目
 */
@Slf4j
@Component
public class SubjectRef extends AbstractCommonRule {
    public  static  String  BD_ACCSUBJECT_GRID_CM="yonyoufi.bd_accsubject_grid_cm";
    public  static  String  BD_ACCSUBJECT_GRID="fiepub.fiepub_accsubjectref";
    //科目oid参照
    public  static  String  BD_ACCSUBJECT_GRID_OID="fiepub.fiepub_accsubjectoidref";
    public  static  String  FIEPUB_ACCOUNTBOOKREF="fiepub.fiepub_accountbookref"; //新总账账簿
    public  static  String  YONYOUFI_BD_ACCBOOK="bd_accbook"; //旧总账账簿

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        List<BizObject> bills = getBills(billContext, map);
        if (bills != null && bills.size() > 0) {
            BizObject  bizObject =  bills.get(0);
            BillDataDto bill = (BillDataDto) getParam(map);
            if (bill.getrefCode().equals(BD_ACCSUBJECT_GRID_CM) || bill.getrefCode().equals(BD_ACCSUBJECT_GRID) || bill.getrefCode().equals(BD_ACCSUBJECT_GRID_OID)){
                ArrayList itemList = bizObject.get("bankReconciliationSetting_b");
                BankReconciliationSetting_b item = (BankReconciliationSetting_b) itemList.get(0);
                String accbookB = item.get("accbook_b");
                if (StringUtils.isEmpty(accbookB)) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100697"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041802A1", "业务账簿不可为空!") /* "业务账簿不可为空!" */);
                }
                CtmJSONObject json = new CtmJSONObject();
                json.put("accbookId",accbookB);
                String serverUrl = AppContext.getEnvConfig("yzb.base.url");
                String BASE_URL_ACCOUNT_SETTLE = serverUrl + "/cash/getAccsubjectChartByAccbook";
                String thd_userId = AppContext.getCurrentUser().getYhtUserId();
                Map<String, String> header = new HashMap<>();
                header.put("Content-Type", "application/json");
                header.put("thd_userId", thd_userId);
                String str = HttpTookit.
                        doPostWithJson(BASE_URL_ACCOUNT_SETTLE, CtmJSONObject.toJSONString(json), header,"UTF-8");
                CtmJSONObject result = CtmJSONObject.parseObject(str);
                Boolean successFlag = (Boolean) result.get("success");
                if (!successFlag) {
                    throw new Exception(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041802A0","根据账簿查询科目表异常，请稍后重试！") /* "根据账簿查询科目表异常，请稍后重试！" */);
                }
                CtmJSONObject data = result.getJSONObject("data");
                String accsubjectchart = data.getString("accsubjectchart");
                if(StringUtils.isNotEmpty(accsubjectchart)){
                    FilterVO filterVO = bill.getCondition();
                    if(null == filterVO){
                        filterVO = new FilterVO();
                    }
                    UiMetaDaoHelper.appendCondition(filterVO, "accsubjectchart", ICmpConstant.QUERY_EQ, accsubjectchart);  // 科目表
                    UiMetaDaoHelper.appendCondition(filterVO, "leaf", ICmpConstant.QUERY_EQ, true);
                    //CZFW-397119；RPT0374；去掉银企对账设置科目银行存款的限制
//                    if (bill.getrefCode().equals(BD_ACCSUBJECT_GRID_CM)){
//                        UiMetaDaoHelper.appendCondition(filterVO, "cashCategory", ICmpConstant.QUERY_EQ, "Bank");  //只查询现金分类为银行的科目
//                    } else {
//                        UiMetaDaoHelper.appendCondition(filterVO, "cashcategory", ICmpConstant.QUERY_EQ, "Bank");  //只查询现金分类为银行的科目
//                    }
                    bill.setCondition(filterVO);
                    //科目表oid参照
                    if ( bill.getrefCode().equals(BD_ACCSUBJECT_GRID_OID)) {
                        bill.setTreeCondition(filterVO);
                    }
                    putParam(map, bill);
                 }
            } else if(bill.getrefCode().equals(FIEPUB_ACCOUNTBOOKREF)){
                ArrayList itemList = bizObject.get("bankReconciliationSetting_b");
                BankReconciliationSetting_b item = (BankReconciliationSetting_b) itemList.get(0);
                String accentity = item.getUseorg();
                //适配单组织
                if(FIDubboUtils.isSingleOrg()){
                    BizObject singleOrg = FIDubboUtils.getSingleOrg();
                    if(singleOrg!=null){
                        accentity = singleOrg.get("id");
                    }
                }
                //资金组织适配，根据资金组织查询会计主体，再传递给账簿查询
                try {
                    FinOrgDTO finOrgDTO = AccentityUtil.getFinOrgDTOByAccentityId(accentity);
                    if (finOrgDTO != null ){
                        accentity = finOrgDTO.getId();
                    }
                }catch (Exception e){
                    log.error("根据资金组织查询会计主体错误,errorMsg:{}",e.getMessage());
                }
                bill.getCondition().appendCondition(ConditionOperator.and,new SimpleFilterVO(IBussinessConstant.ACCENTITY, ICmpConstant.QUERY_EQ, accentity));
                List<String> accountingclassifiList = new ArrayList<>();
                accountingclassifiList.add("1");//主账簿
                accountingclassifiList.add("2");//报告账簿
                bill.getCondition().appendCondition(ConditionOperator.and,new SimpleFilterVO("accountingclassifi", ICmpConstant.QUERY_IN, accountingclassifiList));
            } else if(bill.getrefCode().equals(YONYOUFI_BD_ACCBOOK)) {
                bill.getCondition().appendCondition(ConditionOperator.and,new SimpleFilterVO("pk_org_id", ICmpConstant.QUERY_EQ, bizObject.get(IBussinessConstant.ACCENTITY)));
                bill.getCondition().appendCondition(ConditionOperator.and,new SimpleFilterVO("booktypecode", ICmpConstant.QUERY_EQ, "mainbook"));
            }
        }
        return new RuleExecuteResult();
    }
}

