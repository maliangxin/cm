package com.yonyoucloud.fi.cmp.bankreconciliationsetting;

import cn.hutool.core.thread.BlockPolicy;
import com.yonyou.ucf.mdd.common.model.Pager;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.basecom.utils.HttpTookit;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.constant.IBussinessConstant;
import com.yonyoucloud.fi.cmp.util.threadpool.ThreadPoolBuilder;
import com.yonyoucloud.fi.cmp.util.threadpool.ThreadPoolUtil;
import org.imeta.orm.base.BizObject;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

@Component
public class BankReconciliationSettingQueryRule extends AbstractCommonRule {
    //线程池
    static ExecutorService executorService = null;

    static {
        executorService = ThreadPoolBuilder.ioThreadPoolBuilder()
                .setQueueSize(100)
                .setDaemon(false)
                .setMaximumPoolSize(100)
                .setRejectHandler(new BlockPolicy())
                .builder(10, 60,1000,"cmp-BankReconciliationSettingQueryRule-async-");
    }

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        List<BizObject> bills = getBills(billContext, map);
        Pager aReturn = (Pager) map.get("return");
        if( aReturn != null){
            List<Map<String,Object>> recordList = aReturn.getRecordList();
            ThreadPoolUtil.executeByBatch(executorService, recordList, 1, "银企对账设置-查询", (int fromIndex, int toIndex) -> {//@notranslate
                String builder = "";
                for (int t = fromIndex; t < toIndex; t++) {
                    Map<String , Object> rec = recordList.get(t);
                    CtmJSONObject params=new CtmJSONObject();
                    CtmJSONArray displayFields=new CtmJSONArray();
                    if(rec.get("assistaccounting") != null){
                        params.put("refCode",rec.get("doctype"));
                        params.put("refType","list");
                        displayFields.add("id");
                        displayFields.add("code");
                        displayFields.add("name");
                        params.put("displayFields",displayFields);
                        params.put("funcode","newvoucher");
                        params.put("pk_org",rec.get(IBussinessConstant.ACCENTITY));
                        if("dept" .equals(rec.get("doctype"))){
                            params.put("filterCondition","{\"accbook\":\""+rec.get("accbook") +"\"}");
                        }
                        List<Map<String, Object>> assistaccount = new ArrayList<Map<String, Object>>();
                        assistaccount=getMinPeriod(params);
                        for (Map<String, Object> ass:assistaccount) {
                            if(ass.get("id").equals(rec.get("assistaccounting")) ){
                                rec.put("assistaccounting",ass.get("name"));
                            }
                        }
                    }
                }
                return builder;
            }, false);
        }
        return new RuleExecuteResult();
    }


    public  List<Map<String, Object>> getMinPeriod(CtmJSONObject params)throws Exception {
        String serverUrl = AppContext.getEnvConfig("yzb.base.url");
        String BASE_URL_ACCOUNT_SETTLE = serverUrl + "/refbase_ctr/queryRefJSON";
        String thd_userId = AppContext.getCurrentUser().getYhtUserId();
        Map<String, String> header = new HashMap<>();
        header.put("Content-Type", "application/json");
        header.put("thd_userId", thd_userId);
        params.put("doctype", params.getString("refCode"));
        String str = HttpTookit.
                doPostWithJson(BASE_URL_ACCOUNT_SETTLE, CtmJSONObject.toJSONString(params), header,"UTF-8");
        CtmJSONObject result = CtmJSONObject.parseObject(str);
        Boolean successFlag = (Boolean)result.get("success");
        if(!successFlag){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101024"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041804B5","辅助核算出现异常，请稍后重试") /* "辅助核算出现异常，请稍后重试" */);
        }
        List<Map<String, Object>> data = null;
        String code = String.valueOf(result.get("code"));
        if("0".equals(code)){
            data= (List<Map<String, Object>>) result.get("data");
        }else{
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101025"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041804B6","辅助核算获取失败") /* "辅助核算获取失败" */);
        }
        return data;
    }
}
