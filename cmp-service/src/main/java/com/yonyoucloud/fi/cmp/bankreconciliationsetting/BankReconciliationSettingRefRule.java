package com.yonyoucloud.fi.cmp.bankreconciliationsetting;

import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.ucf.mdd.common.model.Pager;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.basecom.utils.HttpTookit;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.util.JsonUtils;
import org.imeta.orm.base.BizObject;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * 科目参照和辅助核算类型参照规则
 */
@Component
public class BankReconciliationSettingRefRule extends AbstractCommonRule {
    public  static  String  FINBD_BD_MULTIDIMENSION_EXTREF="finbd.bd_multidimension_extref"; //旧辅助项
    public  static  String  FIEPUB_EPUB_MULTIDIMENSION_EXTREF="fiepub.epub_multidimensionextref"; //新辅助项
    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        String billnum = billContext.getBillnum();
        List<BizObject> bills = getBills(billContext, map);
        if (bills != null && bills.size() > 0) {
            BizObject  bizObject =  bills.get(0);
            BillDataDto bill = (BillDataDto) getParam(map);
            if(bill.getrefCode().equals(FINBD_BD_MULTIDIMENSION_EXTREF) || bill.getrefCode().equals(FIEPUB_EPUB_MULTIDIMENSION_EXTREF)){
                String subject = null;
                if ("cmp_bankreconciliationsetting".equals(billnum)) {
                    List<Map<String,Object>> bankReconciliationSetting_b =  bizObject.get("bankReconciliationSetting_b");
                    subject = (String) bankReconciliationSetting_b.get(0).get("subject");
                }
                if ("cmp_bankreconciliationsetting_subjectset".equals(billnum)) {
                    List<Map<String,Object>> list = (List<Map<String,Object>>) bill.getData();
                    Map<String, Object> subjectData = list.get(0) != null ? list.get(0) : new HashMap<>();
                    subject = subjectData.get("subject") != null ? (String) subjectData.get("subject") : null;
                    }
                Pager pager = (Pager) map.get("return");
                if (subject == null || "".equals(subject)) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101146"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041804C5","科目不可为空，请重新选择科目") /* "科目不可为空，请重新选择科目" */);
                }
                CtmJSONObject json = new CtmJSONObject();
                json.put("subjectId",subject);
                String serverUrl = AppContext.getEnvConfig("yzb.base.url");
                String BASE_URL_ACCOUNT_SETTLE = serverUrl + "/cash/getmultidimensionbysubject";
                String thd_userId = AppContext.getCurrentUser().getYhtUserId();
                Map<String, String> header = new HashMap<>();
                header.put("Content-Type", "application/json");
                header.put("thd_userId", thd_userId);
                header.put("locale", InvocationInfoProxy.getLocale());
                String str = HttpTookit.
                        doPostWithJson(BASE_URL_ACCOUNT_SETTLE, CtmJSONObject.toJSONString(json), header,"UTF-8");
                CtmJSONObject result = CtmJSONObject.parseObject(str);
                Boolean successFlag = (Boolean) result.get("success");
                if (!successFlag) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101147"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041804C6","查询辅助核算类型失败，请稍后重试") /* "查询辅助核算类型失败，请稍后重试" */);
                }
                List<Map<String, Object>> data = new ArrayList<Map<String, Object>>();;
                String code = String.valueOf(result.get("code"));
                if ("200".equals(code)) {
                    //todo 315不合release
                    CtmJSONArray jsonArray = result.getJSONArray("data");
                    for(Object o : jsonArray) {
                        data.add(JsonUtils.ConvertObject2Map(o));
                    }
                    //data = JsonUtils.parse(CtmJSONObject.toJSONString(result.getJSONArray("data")), List.class);
                } else {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101148"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041804C4","辅助核算类型获取失败!") /* "辅助核算类型获取失败!" */);
                }
                pager.setRecordList(data);
                pager.setRecordCount(data.size());
            }
        }
        return new RuleExecuteResult();
    }

}

