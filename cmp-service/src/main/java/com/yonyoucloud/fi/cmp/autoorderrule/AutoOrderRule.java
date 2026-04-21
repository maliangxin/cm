package com.yonyoucloud.fi.cmp.autoorderrule;

import com.yonyou.iuap.yms.id.generator.YmsOidGenerator;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.cmpentity.IsEnable;
import com.yonyoucloud.fi.cmp.util.CmpMetaDaoHelper;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 到账认领 - 自动生单规则
 * @author msc
 */
@Component
public class AutoOrderRule extends AbstractCommonRule {
    @Autowired
    YmsOidGenerator ymsOidGenerator;
    /**
     * 自动生单规则页面 -- 保存前规则
     * 生单规则页面为 可编辑列表（EditVoucherList）
     * EditVoucherList类型保存时：数据结构为list - 平台保存暂不支持此格式
     * 手动将数据保存
     * @param billContext
     * @param map
     * @return
     * @throws Exception
     */
    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        /**
         * 将数据从 -- billContext内取出
         */
        List<BizObject> bills = getBills(billContext, map);
        String billNum = billContext.getBillnum();
        List<Autoorderrule> insertlist = new ArrayList<Autoorderrule>();//新增数据
        List<Autoorderrule> updatelist = new ArrayList<Autoorderrule>();//需要修改的数据
        List<Autoorderrule> deletelist = new ArrayList<Autoorderrule>();//需要删除的数据
        List<Autoorderrule_b> autoorderrule_bs = new ArrayList<Autoorderrule_b>();//新增字表数据
        if(!bills.isEmpty()){
            List<BizObject>  bizObjects = bills.get(0).get(billNum);//取出需要操作的数据 -- AutoOrderRuleList
            /**
             * 将数据存入AutoOrderRule实体内，按照操作不同，存入不同list
             */
            for (BizObject bizObject:bizObjects) {
                Autoorderrule autoorderrule = new Autoorderrule();
                if(bizObject.get("application")!=null){
                    autoorderrule.setApplication(Short.valueOf(bizObject.get("application")));//所属应用
                }
                if(bizObject.get("direction")!=null){
                    autoorderrule.setDirection(Short.valueOf(bizObject.get("direction")));//借贷方向
                }
                if(bizObject.get("busDocumentType")!=null){
                    autoorderrule.setBusDocumentType(Short.valueOf(bizObject.get("busDocumentType")));//业务单据类型
                }
                if(bizObject.get("otherType")!=null){
                    autoorderrule.setOtherType(Short.valueOf(bizObject.get("otherType")));//对方类型
                }
                if(bizObject.get("sensitiveWordsType")!=null){
                    autoorderrule.setSensitiveWordsType(Short.valueOf(bizObject.get("sensitiveWordsType")));//敏感词规则
                }
                if(bizObject.get("sensitiveWords")!=null){
                    autoorderrule.setSensitiveWords(bizObject.get("sensitiveWords"));//敏感词
                }
                if(bizObject.get("ruleExecuteTime")!=null){
                    autoorderrule.setRuleExecuteTime(bizObject.getShort("ruleExecuteTime"));//执行时机
                }
                if(bizObject.get("id") != null && bizObject.get("_status").equals("Update")){//修改操作
                    autoorderrule.setId(Long.parseLong(bizObject.get("id")));
                    autoorderrule.setEntityStatus(EntityStatus.Update);
                    updatelist.add(autoorderrule);
                }else if(bizObject.get("id")!=null && bizObject.get("_status").equals("Delete")){//删除操作
                    autoorderrule.setId(Long.parseLong(bizObject.get("id")));
                    autoorderrule.setEntityStatus(EntityStatus.Delete);
                    deletelist.add(autoorderrule);
                }else{//新增操作
                    autoorderrule.setId(ymsOidGenerator.nextId());
                    autoorderrule.setEntityStatus(EntityStatus.Insert);
                    Autoorderrule_b autoorderrule_b = new Autoorderrule_b();
                    autoorderrule_b.setMainid(autoorderrule.getId());
                    autoorderrule_b.setId(ymsOidGenerator.nextId());
                    autoorderrule_b.setEntityStatus(EntityStatus.Insert);
                    autoorderrule_bs.add(autoorderrule_b);
                    autoorderrule.setDetailid(autoorderrule_b.getId());
                    autoorderrule.setIsEnable(IsEnable.DISENABLE.getValue());
                    insertlist.add(autoorderrule);
                }
            }
            MetaDaoHelper.update(Autoorderrule.ENTITY_NAME,updatelist);
            MetaDaoHelper.delete(Autoorderrule.ENTITY_NAME,deletelist);
            CmpMetaDaoHelper.insert(Autoorderrule.ENTITY_NAME,insertlist);
            CmpMetaDaoHelper.insert(Autoorderrule_b.ENTITY_NAME,autoorderrule_bs);
        }
        RuleExecuteResult ruleExecuteResult = new RuleExecuteResult();
        ruleExecuteResult.setCancel(true);
        return ruleExecuteResult;
    }
}
