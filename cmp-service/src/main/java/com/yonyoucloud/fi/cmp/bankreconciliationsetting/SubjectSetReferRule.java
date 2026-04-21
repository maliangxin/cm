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
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.constant.IBussinessConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.util.AccentityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.BizObject;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @description: 银企对账设置，账户科目设置弹框参照过滤rule
 * @author: wanxbo@yonyou.com
 * @date: 2025/4/7 14:11
 */

@Component
@Slf4j
@RequiredArgsConstructor
public class SubjectSetReferRule  extends AbstractCommonRule {
    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        String billnum = billContext.getBillnum();
        BillDataDto bill = (BillDataDto) getParam(paramMap);
        if(null != bill.getKey()  && "cmp_bankreconciliationsetting_subjectset".equals(billnum)) {
            List<Map<String,Object>> list = (List<Map<String,Object>>) bill.getData();
            Map<String, Object> subjectData = list.get(0) != null ? list.get(0) : new HashMap<>();
            //科目
            if ("fiepub.fiepub_accsubjectoidref".equals(bill.getRefCode())) {
                if (subjectData.get("accbook_b") == null) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100697"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041802A1", "业务账簿不可为空!") /* "业务账簿不可为空!" */);
                }
                CtmJSONObject json = new CtmJSONObject();
                json.put("accbookId", subjectData.get("accbook_b"));
                String serverUrl = AppContext.getEnvConfig("yzb.base.url");
                String BASE_URL_ACCOUNT_SETTLE = serverUrl + "/cash/getAccsubjectChartByAccbook";
                String thd_userId = AppContext.getCurrentUser().getYhtUserId();
                Map<String, String> header = new HashMap<>();
                header.put("Content-Type", "application/json");
                header.put("thd_userId", thd_userId);
                String str = HttpTookit.
                        doPostWithJson(BASE_URL_ACCOUNT_SETTLE, CtmJSONObject.toJSONString(json), header, "UTF-8");
                CtmJSONObject result = CtmJSONObject.parseObject(str);
                Boolean successFlag = (Boolean) result.get("success");
                if (!successFlag) {
                    throw new Exception(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041802A0", "根据账簿查询科目表异常，请稍后重试！") /* "根据账簿查询科目表异常，请稍后重试！" */);
                }
                CtmJSONObject data = result.getJSONObject("data");
                String accsubjectchart = data.getString("accsubjectchart");
                if (StringUtils.isNotEmpty(accsubjectchart)) {
                    FilterVO filterVO = bill.getCondition();
                    if (null == filterVO) {
                        filterVO = new FilterVO();
                    }
                    UiMetaDaoHelper.appendCondition(filterVO, "accsubjectchart", ICmpConstant.QUERY_EQ, accsubjectchart);  // 科目表
                    UiMetaDaoHelper.appendCondition(filterVO, "leaf", ICmpConstant.QUERY_EQ, true);
                    bill.setCondition(filterVO);
                    //科目表oid参照
                    bill.setTreeCondition(filterVO);
                    putParam(paramMap, bill);
                }
            }

            if ("fiepub.fiepub_accountbookref".equals(bill.getRefCode())) {
                if (subjectData.get("accentity") == null) {
                    throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1F314EF805B80004", "账户使用组织为空！") /* "账户使用组织为空！" */);
                }
                String accentity = subjectData.get("accentity").toString();
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
                List<String> accountingclassifiList = new ArrayList<>();
                accountingclassifiList.add("1");//主账簿
                accountingclassifiList.add("2");//报告账簿
                bill.getCondition().appendCondition(ConditionOperator.and,new SimpleFilterVO(IBussinessConstant.ACCENTITY, ICmpConstant.QUERY_EQ, accentity));
                bill.getCondition().appendCondition(ConditionOperator.and,new SimpleFilterVO("accountingclassifi", ICmpConstant.QUERY_IN, accountingclassifiList));
            }
        }

        return new RuleExecuteResult();
    }
}
