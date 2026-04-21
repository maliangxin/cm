package com.yonyoucloud.fi.cmp.bankrecrule.ruleengine.imp;

import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.iuap.log.model.BusinessArrayObject;
import com.yonyou.iuap.log.rpc.IBusinessLogService;
import com.yonyou.iuap.log.util.BusiObjectBuildUtil;
import com.yonyou.iuap.ruleengine.dto.relevant.RuleItemDto;
import com.yonyou.iuap.ruleengine.dto.relevant.TargetRuleInfoDto;
import com.yonyou.iuap.ruleengine.relevant.RelevantRuleExecService;
import com.yonyou.iuap.ruleengine.relevant.RelevantRuleLoadService;
import com.yonyou.iuap.yms.lock.YmsLock;
import com.yonyou.ucf.mdd.ext.bill.rule.util.BillInfoUtils;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.bankreconciliation.CommonSaveUtils;
import com.yonyoucloud.fi.cmp.bankreconciliation.ruleengine.CmpRuleBusiLog;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import com.yonyoucloud.fi.cmp.util.EntityTool;
import com.yonyoucloud.fi.cmp.util.IdCreator;
import com.yonyoucloud.fi.cmp.util.JedisLockUtils;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;
import java.util.stream.Collectors;

/**
 * desc: 规则执行策略抽象类
 * author:wangqiangac
 * date:2023/5/25 11:48
 */
public abstract class RuleStrategy {
    protected final String bizObjectCode = "ctm-cmp.cmp_bankreconciliation";
    // 相关性规则编码前缀 辨识规则、冻结规则、生单规则
    public static final String CMP_IDENTIFICATION_PREFIX = "CMP_Identification_"; // 辨识规则
    public static final String CMP_FREEZE_PREFIX = "CMP_Freeze_"; // 冻结规则
    public static final String CMP_RECEIPT_ASSOCIATION_PREFIX = "CMP_ReceiptAssociation_"; // 回单关联规则
    public static final String CMP_COLLECTION_ASSOCIATION_PREFIX = "CMP_CollectionAssociation_"; // 关联收款业务单据规则
    public static final String CMP_PAYMENT_ASSOCIATION_PREFIX = "CMP_PaymentAssociation_"; // 关联付款业务单据规则
    public static final String CMP_COLLECTION_RECEIPT_PREFIX = "CMP_CollectionReceipt_"; // 收款生单规则
    public static final String CMP_PAYMENT_RECEIPT_PREFIX = "CMP_PaymentReceipt_"; // 付款生单规则
    public static final String CMP_GENERATE_PREFIX = "CMP_Generate_"; // 付款生单规则
    public static final String CMP_EARLY_RECORD_PREFIX = "CMP_EarlyRecord_"; // 提前入账规则
    public static final String CMP_RECONCILIATION_PREFIX = "CMP_Reconciliation_"; // 对账规则
    @Autowired
    public RelevantRuleExecService relevantRuleExecService;
    @Autowired
    public RelevantRuleLoadService relevantRuleLoadService;
    @Autowired
    public IBusinessLogService businessLogService;

    /**
     * 根据规则前缀获取对应的相关性规则并根据编码的数字排序后返回
     *
     * @param prefix 规则前缀
     * @return 返回按照规则后缀数字排序后的规则Map
     */
    public Map<Integer, TargetRuleInfoDto> loadRule(String prefix) {
        String tenantId = InvocationInfoProxy.getTenantid();
        Map<Object, TargetRuleInfoDto> ruleInfoMap = relevantRuleLoadService.loadByBizObjectCode(bizObjectCode, tenantId);
        Map<Integer, TargetRuleInfoDto> executeRuleMap = new HashMap<>();
        for (TargetRuleInfoDto ruleInfoDto : ruleInfoMap.values()) {
            String ruleCode = ruleInfoDto.getCode();
            //根据编码判断是否是辨识规则
            if (ruleCode.startsWith(prefix)) {
                // 获取后缀，即规则执行序号
                try {
                    String suffixNum = ruleCode.substring(prefix.length());
                    Integer codeNum = Integer.parseInt(suffixNum);
                    executeRuleMap.put(codeNum, ruleInfoDto);
                } catch (NumberFormatException e) {//规则命名格式不对无法转数字直接跳过忽略
                    continue;
                }
            }
        }
        return executeRuleMap.entrySet().stream().sorted(Map.Entry.comparingByKey()).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
    }


    /**
     * 执行规则
     *
     * @param list 银行对账单集合
     * @throws Exception 异常
     */
    public abstract void executeRule(List<BankReconciliation> list, String rulePrefix) throws Exception;

    /**
     * 根据executeRuleMap将所有规则中的source的code查询
     *
     * @param list
     * @param executeRuleMap
     * @return
     * @throws Exception
     */
    public Map<Long, Map<String, Object>> querySourcesValue(List<BankReconciliation> list, Map<Integer, TargetRuleInfoDto> executeRuleMap) throws Exception {
        Map<Long, Map<String, Object>> sourcesMap = new HashMap<>();
        QuerySchema schema = QuerySchema.create().addSelect("id , bank_seq_no");
        List<Object> ids = list.stream().map(BankReconciliation::getId).collect(Collectors.toList());
        for (Map.Entry<Integer, TargetRuleInfoDto> entry : executeRuleMap.entrySet()) {
            TargetRuleInfoDto ruleInfoDto = entry.getValue();
            // 为规则数据项具体值赋值
            List<RuleItemDto> sources = ruleInfoDto.getSources();
            for (RuleItemDto source : sources) {
                String code = source.getCode();
                String asCode = code.replace(".", "_");
                schema.addSelect(code + " as " + asCode);
            }
        }
        schema.appendQueryCondition(QueryCondition.name("id").in(ids));
        List<Map<String, Object>> resultMap = MetaDaoHelper.query(BankReconciliation.ENTITY_NAME, schema, null);
        if (resultMap != null && resultMap.size() > 0) {
            for (Map<String, Object> result : resultMap) {
                sourcesMap.put((Long) result.get("id"), result);
            }
        }
        return sourcesMap;
    }

    /**
     * 给规则的sources设置银行对账单的value
     *
     * @param sourcesMap
     * @param bankReconciliation
     * @param sources
     */
    public void dealSourcesValue(Map<Long, Map<String, Object>> sourcesMap, BankReconciliation bankReconciliation, List<RuleItemDto> sources) {
        Map<String, Object> map_result = sourcesMap.get(bankReconciliation.getId());
        for (RuleItemDto source : sources) {
            String code = source.getCode();
            String asCode = code.replace(".", "_");
            // 有些规则的条件依赖上一条规则的结果，这里如果取不到值重新从单据上取一下
            Object value = map_result.get(asCode) == null ? bankReconciliation.get(asCode) : map_result.get(asCode);
            source.setValue(value);
        }
    }

    /**
     * 修改银行对账单
     *
     * @param bankReconciliation
     * @throws Exception
     */
    public void updateBankReconciliation(BankReconciliation bankReconciliation) throws Exception {
        QuerySchema schema = QuerySchema.create().addSelect("pubts");
        schema.appendQueryCondition(QueryCondition.name("id").eq(bankReconciliation.getId()));
        List<Map<String, Object>> resultMap = MetaDaoHelper.query(BankReconciliation.ENTITY_NAME, schema, null);
        bankReconciliation.set("pubts", resultMap.get(0).get("pubts"));
        // 加锁
        YmsLock ymsLock = JedisLockUtils.lockBillWithOutTrace(bankReconciliation.getId().toString());
        EntityTool.setUpdateStatus(bankReconciliation);
        BillInfoUtils.setEditAuditInfo(bankReconciliation);
        CommonSaveUtils.updateBankReconciliation(bankReconciliation);
        // 释放锁
        JedisLockUtils.unlockBillWithOutTrace(ymsLock);
    }

    /**
     * 银行对账单执行相关性规则在平台业务日志节点记录业务日志
     *
     * @param cmpRuleBusiLog
     */
    public void sendBusinessLog(CmpRuleBusiLog cmpRuleBusiLog) {
        String serviceCode = IServicecodeConstant.CMPBANKRECONCILIATION;
        String typeCode = "";
        String typeName = com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400770", "相关性规则执行") /* "相关性规则执行" */;
        // 操作分类
        String operationName = com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400771", "规则执行") /* "规则执行" */;
        List<Object> listRuleBusinessLog = new ArrayList<>();
        listRuleBusinessLog.add(cmpRuleBusiLog);
        // 构建业务日志实体
        BusinessArrayObject businessArrayObject = BusiObjectBuildUtil.buildArrayObjectByField(CmpRuleBusiLog.idField, CmpRuleBusiLog.codeField,
                CmpRuleBusiLog.nameField, serviceCode, null, typeCode, typeName, listRuleBusinessLog);
        businessArrayObject.setOperationName(operationName);
        // 保存业务日志
        businessLogService.saveBusinessLog(businessArrayObject);
    }
    /**
     * @Describe 处理特征字段结果集
     * @Param
     * @Return
     */
    public void checkCharacterDef(BankReconciliation bankrec, Object key, Object value) {
        String characterDef_id = bankrec.get("characterDef_id");
        BizObject characterDef = new BizObject();
        if(characterDef_id != null){
            if(bankrec.get("characterDef") != null){
                characterDef = bankrec.get("characterDef");
            }else{
                //设置特征状态
                for(Map.Entry<String,Object> entry:bankrec.entrySet()){
                    if(entry.getKey().startsWith("characterDef_") && !entry.getKey().equals("characterDef_pubts")){
                        characterDef.set(entry.getKey().replace("characterDef_",""), entry.getValue());
                    }
                }
            }
            characterDef.setEntityStatus(EntityStatus.Update);
            characterDef.set(key.toString(), value);
            bankrec.put("characterDef", characterDef);
        }else{
            characterDef = bankrec.get("characterDef");
            if(characterDef == null){
                characterDef = new BizObject();
                characterDef.put("id", String.valueOf(IdCreator.getInstance().nextId()));
                characterDef.setEntityStatus(EntityStatus.Insert);
                characterDef.set(key.toString(), value);
                bankrec.put("characterDef", characterDef);
            }else{
                characterDef.set(key.toString(), value);
                bankrec.put("characterDef", characterDef);
            }
        }
    }
}
