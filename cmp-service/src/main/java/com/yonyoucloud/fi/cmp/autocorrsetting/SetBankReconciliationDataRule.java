package com.yonyoucloud.fi.cmp.autocorrsetting;

import com.yonyou.ucf.mdd.common.model.Pager;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliationbusrelation_b;
import com.yonyoucloud.fi.cmp.cmpentity.Relationtype;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QueryOrderby;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 自动关联确认数据查询后置规则
 */
@Component("setBankReconciliationDataRule")
public class SetBankReconciliationDataRule extends AbstractCommonRule {

    /**
     * 取出银行对账单集合
     * 将每个银行对账单的ID放入idlist
     * 将每个银行对账单，已id为key，对账单数据为value存入map。
     * 取出当前对账单list中 所有对账单对应的关联表数据
     * 循环关联表，每个数据通过map，找到对应的对账单，添加至对账单数据格式内。
     * @param billContext
     * @param map
     * @return
     * @throws Exception
     */
    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {

        //Long ytenantid = AppContext.getTenantId();
        //Long userid = AppContext.getUserId();
        if(map.get("return")!=null && map.get("return") instanceof Pager && ((Pager)(map.get("return"))).getRecordList().size()>0){
            //Short toconfim = 1;//待确认
            Pager pager= (Pager)(map.get("return"));
            List<Map<String,Object>> list = pager.getRecordList();
            List delData = new ArrayList();
            for(int i = 0; i<list.size();i++){
                Long id = (Long)list.get(i).get("id");
                QuerySchema querySchema = QuerySchema.create().addSelect("bankreconciliation,billtype,vouchdate,srcbillid,billid,billcode,accentity,accentity.code,accentity.name,dept,dept.code,dept.name,project,project.code,project.name,amountmoney,relationstatus,relationtype,billnum,id");
                QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("bankreconciliation").eq(id));
                querySchema.addCondition(group);
                querySchema.addOrderBy(new QueryOrderby("vouchdate","desc"));
                List<BankReconciliationbusrelation_b> bankReconciliationbusrelation_bs = MetaDaoHelper.queryObject(BankReconciliationbusrelation_b.ENTITY_NAME,querySchema,null);
                //过滤掉没有关联数据，关联数据为已确认的对账单
                if(bankReconciliationbusrelation_bs != null&&bankReconciliationbusrelation_bs.size()>0){
                    for (int j = 0; j<bankReconciliationbusrelation_bs.size();j++){
                        if(bankReconciliationbusrelation_bs.get(j).getRelationtype()!= Relationtype.AutoAssociated.getValue()){
                            bankReconciliationbusrelation_bs.remove(j);
                        }
                    }
                    list.get(i).put("BankReconciliationbusrelation_b",bankReconciliationbusrelation_bs);
                }else{
                    delData.add(list.get(i));
                }
            }
            if(delData!=null&&delData.size()>0){
                for(int i = 0; i<delData.size();i++){
                    list.remove(delData.get(i));
                }
            }
            pager.setRecordList(list);
            map.put("return",pager);
        }
        return new RuleExecuteResult();
    }
}
