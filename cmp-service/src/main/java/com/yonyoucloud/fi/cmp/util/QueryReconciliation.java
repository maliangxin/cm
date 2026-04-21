package com.yonyoucloud.fi.cmp.util;

import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.basecom.utils.HttpTookit;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.bankreconciliationsetting.BankReconciliationSetting_b;
import com.yonyoucloud.fi.cmp.journal.Journal;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QuerySchema;

import java.util.*;

public class QueryReconciliation {
    public static CtmJSONObject queryReconciliation(List<BankReconciliationSetting_b> bank_b, Long id, int reconciliationdatasource) throws Exception {
        //定义状态
        int state = 0;
        //根据数据进行查询
        Set bankaccount = new HashSet();
        if (bank_b != null && bank_b.size() > 0) {
        for (LinkedHashMap<String , Object> bankReconciliationSetting_b : bank_b) {
            //等于1  为凭证
            if (reconciliationdatasource == 1) {
                //查询 总账 银行对账单
                QuerySchema queryGl = QuerySchema.create().addSelect("bankaccount");
                queryGl.appendQueryCondition(QueryCondition.name("bankaccount").eq(bankReconciliationSetting_b.get("bankaccount")));
                queryGl.appendQueryCondition(QueryCondition.name("gl_bankreconciliationsettingid").eq(id.toString()));
                List<Map<String , Object>> bankReconciliationList2 = MetaDaoHelper.query(BankReconciliation.ENTITY_NAME, queryGl);
                if (bankReconciliationList2.size() > 0 && bankReconciliationList2 != null) {
                    bankaccount.add(bankReconciliationList2.get(0).get("bankaccount"));
                }
                //调用总账接口
                CtmJSONObject params = new CtmJSONObject();
                params.put("bankreconciliationsettingid", id);
                List<Map<String, Object>> reconciliationList = getReconciliationList(params);
                if (reconciliationList != null && reconciliationList.size() > 0) {
                    for (Map<String, Object> gl : reconciliationList) {
                        bankaccount.add(gl.get("bankaccount"));
                    }
                }
            } else if (reconciliationdatasource == 2) {//日记账
                QuerySchema queryCash = QuerySchema.create().addSelect("bankaccount");
                queryCash.appendQueryCondition(QueryCondition.name("bankaccount").eq(bankReconciliationSetting_b.get("bankaccount")));
                queryCash.appendQueryCondition(QueryCondition.name("bankreconciliationsettingid").eq(id.toString()));
                //查询 现金 银行对账单
                List<Map<String , Object>> bankReconciliationList = MetaDaoHelper.query(BankReconciliation.ENTITY_NAME, queryCash);
                if (bankReconciliationList.size() > 0 && bankReconciliationList != null) {
                    //将查询出来的账户放入集合中
                    bankaccount.add(bankReconciliationList.get(0).get("bankaccount"));
                }
                //CZFW-375938 银行对账单要判断去重，不能共用一个schema，会被插件CmpQuerySchemaExecutorPlugin添加错误的过滤
                QuerySchema queryCash_journal = QuerySchema.create().addSelect("bankaccount");
                queryCash_journal.appendQueryCondition(QueryCondition.name("bankaccount").eq(bankReconciliationSetting_b.get("bankaccount")));
                queryCash_journal.appendQueryCondition(QueryCondition.name("bankreconciliationsettingid").eq(id.toString()));
                //查询 现金 银行日记账
                List<Map<String , Object>> journalList = MetaDaoHelper.query(Journal.ENTITY_NAME, queryCash_journal);
                if (journalList.size() > 0 && journalList != null) {
                    //将查询出来的账户放入集合中
                    bankaccount.add(journalList.get(0).get("bankaccount"));
                }
            }
        }
      }else if(bank_b == null || bank_b.size() == 0 ){//证明该调用请求是删除
            //先查 现金 日记账
            QuerySchema queryJournal = QuerySchema.create().addSelect("*");
            queryJournal.appendQueryCondition(QueryCondition.name("bankreconciliationsettingid").eq(id.toString()));
            List<Journal> queryJournalList = MetaDaoHelper.query(Journal.ENTITY_NAME, queryJournal);
            List<BankReconciliation> bankReconciliationList = new ArrayList<>();
            if(queryJournalList.size() == 0 || queryJournalList == null){
                //现金日记账为空  查询 现金 银行对账单
                QuerySchema queryBankrec= QuerySchema.create().addSelect("*");
                queryBankrec.appendQueryCondition(QueryCondition.name("bankreconciliationsettingid").eq(id.toString()));
                bankReconciliationList = MetaDaoHelper.query(BankReconciliation.ENTITY_NAME,queryBankrec);
                if(bankReconciliationList == null || bankReconciliationList.size() == 0){
                    //现金银行对账单为空  查询 总账 银行对账单
                    QuerySchema queryGl = QuerySchema.create().addSelect("*");
                    queryGl.appendQueryCondition(QueryCondition.name("gl_bankreconciliationsettingid").eq(id.toString()));
                    bankReconciliationList= MetaDaoHelper.query(BankReconciliation.ENTITY_NAME,queryGl);
                 }
            }
            //调用总账接口查询
                CtmJSONObject params = new CtmJSONObject();
                params.put("bankreconciliationsettingid",id);
                List<Map<String , Object>> reconciliationList = getReconciliationList(params);
                if(reconciliationList != null && reconciliationList.size() > 0){
                    state = 1;
                }
            if((queryJournalList.size()+bankReconciliationList.size())>0){
                state = 1;
            }
        }

        if(bankaccount != null && bankaccount.size()>0){
            state = 1;
        }
        CtmJSONObject result = new CtmJSONObject();
        result.put("bankaccount" , bankaccount);
        result.put("state" , state);
        return result ;
    }
    public static List<Map<String , Object>> getReconciliationList(CtmJSONObject params)throws Exception {
        String serverUrl = AppContext.getEnvConfig("yzb.base.url");
        String BASE_URL_ACCOUNT_SETTLE = serverUrl + "/cash/queryvoucherbodybyschemaid";
        String thd_userId = AppContext.getCurrentUser().getYhtUserId();
        Map<String, String> header = new HashMap<>();
        header.put("Content-Type", "application/json");
        header.put("thd_userId", thd_userId);
        String str = HttpTookit.
                doPostWithJson(BASE_URL_ACCOUNT_SETTLE, CtmJSONObject.toJSONString(params), header,"UTF-8");
        CtmJSONObject result = CtmJSONObject.parseObject(str);
        Boolean successFlag = (Boolean)result.get("success");
       /* if(!successFlag && successFlag != null ){
            throw new Exception("查询总账失败");
        }
        String code = String.valueOf(result.get("code"));*/
        List<Map<String, Object>> data = null;
        if (successFlag) {
            data = (List<Map<String, Object>>) result.get("data");
        } else if (!successFlag && successFlag != null) {
            throw new Exception(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180738","查询总账失败") /* "查询总账失败" */);
        } else {
            throw new Exception(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180739","出现异常，请刷新") /* "出现异常，请刷新" */);
        }
        return data;
    }
}
