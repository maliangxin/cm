package com.yonyoucloud.fi.cmp.autocorrsetting;

import com.yonyou.iuap.ucf.common.i18n.MessageUtils;
import com.yonyou.iuap.yms.id.generator.YmsOidGenerator;
import com.yonyou.iuap.yms.lock.YmsLock;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.basecom.itf.IFIBillService;
import com.yonyoucloud.fi.cmp.autocorrsettings.Autocorrsetting;
import com.yonyoucloud.fi.cmp.util.CmpMetaDaoHelper;
import com.yonyoucloud.fi.cmp.util.JedisLockUtils;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class AutoCorrSaveRule extends AbstractCommonRule {

    @Autowired
    private IFIBillService fiBillService;
    @Autowired
    YmsOidGenerator ymsOidGenerator;
    /**
     * Service规则服务于 billNum = cmp_autocorrsetting
     * 单据类型 billType = EditVoucherList可编辑列表
     * 当单据类型为可编辑列表时，传至后端生成的结构，与卡片不同
     * 通用save方法的保存无法识别字段；
     *
     * 故，新增此规则将数据结构进行处理，提取出单据字段进行保存/修改。
     * 阻断规则链，返回。
     * @param billContext
     * @param map
     * @return
     * @throws Exception
     */
    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {

        RuleExecuteResult ruleExecuteResult = new RuleExecuteResult();
        List<BizObject> bills = getBills(billContext, map);
        List<Autocorrsetting> autocorrsettings = new ArrayList<>();
        List<Autocorrsetting> updateautocorrsettings = new ArrayList<>();
        String key = billContext.getTenant()+"";
        Boolean isNotAllow = false;
        YmsLock ymsLock = JedisLockUtils.lockBillWithOutTrace(key);
        if(null == ymsLock){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100452"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00071", "该数据正在处理，请稍后重试！") /* "该数据正在处理，请稍后重试！" */);
        }
        try{
        if (bills != null && bills.size() > 0) {
            /**
             * 循环将业务单据数据去除，放入业务单据实体内，进行操作。
             */
            List<BizObject>  bizObjects = bills.get(0).get("cmp_autocorrsetting");
            for(BizObject bizObject : bizObjects) {
                Autocorrsetting autocorrsetting = new Autocorrsetting();
                if (bizObject.get("exorder") != null) {//执行顺序号
                    autocorrsetting.setExorder(Integer.valueOf(bizObject.get("exorder")));
                }
                if (bizObject.get("bankRecCode") != null) {//对账单编码
                    autocorrsetting.setBankRecCode(Short.valueOf(bizObject.get("bankRecCode")));
                }
                if (bizObject.get("busDocumentType") != null) {//业务单据类型
                    autocorrsetting.setBusDocumentType(Short.valueOf(bizObject.get("busDocumentType")));
                }

                if (bizObject.get("billabstract") != null) {//摘要
                    autocorrsetting.setBillabstract(Short.valueOf(bizObject.get("billabstract")));
                }
                if (bizObject.get("direction") != null) {//对账单方向
                    autocorrsetting.setDirection(Short.valueOf(bizObject.get("direction")));
                }
                if (bizObject.get("money") != null) {//金额
                    autocorrsetting.setMoney(Short.valueOf(bizObject.get("money")));
                }
                if (bizObject.get("ourBankNum") != null) {//己方银行账号
                    autocorrsetting.setOurBankNum(Short.valueOf(bizObject.get("ourBankNum")));
                }
                if (bizObject.get("othBankNum") != null) {//对方银行账号
                    autocorrsetting.setOthBankNum(Short.valueOf(bizObject.get("othBankNum")));
                }
                if (bizObject.get("othBankNumName") != null) {//对方户名
                    autocorrsetting.setOthBankNumName(Short.valueOf(bizObject.get("othBankNumName")));
                }

                if (bizObject.get("floatDays") != null) {//浮动天数
                    autocorrsetting.setFloatDays(new BigDecimal(bizObject.get("floatDays").toString()));
                }
                if (bizObject.get("id") != null&&bizObject.get("_status").equals("Update")) {//通过单据数据中id是否为空判断，是执行修改/新增.
                    QuerySchema querySchema1 = QuerySchema.create().addSelect("*");
                    QueryConditionGroup group1 = QueryConditionGroup.and(QueryCondition.name("direction").eq(autocorrsetting.getDirection()));
                    querySchema1.addCondition(group1);
                    List<Autocorrsetting> ags = MetaDaoHelper.queryObject(Autocorrsetting.ENTITY_NAME,querySchema1,null);
                    for (Autocorrsetting ag:ags) {
                        if(ag.getBusDocumentType() == autocorrsetting.getBusDocumentType()||ag.getExorder() == autocorrsetting.getExorder()){
                            isNotAllow = true;
                        }
                    }
                    autocorrsetting.setEntityStatus(EntityStatus.Update);
                    autocorrsetting.setId(Long.parseLong(bizObject.get("id")));
                    updateautocorrsettings.add(autocorrsetting);
                } else if (bizObject.get("_status").equals("Delete")){
                    autocorrsetting.setEntityStatus(EntityStatus.Delete);
                    autocorrsetting.setId(Long.parseLong(bizObject.get("id")));
                    MetaDaoHelper.delete(Autocorrsetting.ENTITY_NAME,autocorrsetting);
                }else{
                    QuerySchema querySchema1 = QuerySchema.create().addSelect("*");
                    QueryConditionGroup group1 = QueryConditionGroup.and(QueryCondition.name("direction").eq(autocorrsetting.getDirection()));
                    querySchema1.addCondition(group1);
                    List<Autocorrsetting> ags = MetaDaoHelper.queryObject(Autocorrsetting.ENTITY_NAME,querySchema1,null);
                    for (Autocorrsetting ag:ags) {
                        if(ag.getBusDocumentType() == autocorrsetting.getBusDocumentType()||ag.getExorder() == autocorrsetting.getExorder()){
                            isNotAllow = true;
                        }
                    }
                        autocorrsetting.setId(ymsOidGenerator.nextId());
                        autocorrsetting.setEntityStatus(EntityStatus.Insert);
                        autocorrsettings.add(autocorrsetting);
                    }
                }
            //将修改、新增列表执行不同的操作
            CmpMetaDaoHelper.insert(Autocorrsetting.ENTITY_NAME,autocorrsettings);
            MetaDaoHelper.update(Autocorrsetting.ENTITY_NAME,updateautocorrsettings);
        }
        }catch (CtmException e){
            ruleExecuteResult.setData(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00070", "该数据正在处理，请稍后重试！") /* "该数据正在处理，请稍后重试！" */);
            ruleExecuteResult.setMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00070", "该数据正在处理，请稍后重试！") /* "该数据正在处理，请稍后重试！" */);
        }finally{
            JedisLockUtils.unlockBillWithOutTrace(ymsLock);
        }
        if(isNotAllow == true){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100453"),MessageUtils.getMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00072", "该数据正在编辑中，请刷新后重试！") /* "该数据正在编辑中，请刷新后重试！" */) );
        }
        //阻断规则链
        ruleExecuteResult.setCancel(true);
        return ruleExecuteResult;
    }

}
