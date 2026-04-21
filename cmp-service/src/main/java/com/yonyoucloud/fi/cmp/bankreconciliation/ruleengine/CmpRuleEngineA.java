package com.yonyoucloud.fi.cmp.bankreconciliation.ruleengine;

import com.google.gson.internal.LinkedTreeMap;
import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.iuap.ruleengine.dto.relevant.RuleExtParamDto;
import com.yonyou.iuap.ruleengine.dto.relevant.RuleItemDto;
import com.yonyou.iuap.ruleengine.dto.relevant.TargetRuleInfoDto;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankreconciliationService;
import com.yonyoucloud.fi.cmp.constant.CmpRuleEngineTypeConstant;
import com.yonyoucloud.fi.cmp.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * @Description: 银行对账单辨识规则实现类
 * @Author: gengrong
 * @createTime: 2022/9/29
 * @version: 1.0
 */
@Slf4j
@Component
@Scope("prototype")
public class CmpRuleEngineA extends AbstractCmpRuleEngine {
    @Autowired
    private BankreconciliationService bankreconciliationService;

    private static final Logger logger = LoggerFactory.getLogger(AbstractCmpRuleEngine.class);

    // 提示语
    private final String noRuleMsg = com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B001AF", "回单获取失败，失败原因：回单自动获取时未找到以[cmp_identification_]开头的辨识规则，") /* "回单获取失败，失败原因：回单自动获取时未找到以[cmp_identification_]开头的辨识规则，" */ +
            com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B001AD", "无法辨识回单信息，请检查辨识规则是否存在或者是否启用！") /* "无法辨识回单信息，请检查辨识规则是否存在或者是否启用！" */;
    private final String errorRuleCodeMsg = com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B001AE", "辨识规则编码不符合规范:前缀为[cmp_identification_],后加三位数字") /* "辨识规则编码不符合规范:前缀为[cmp_identification_],后加三位数字" */;

    @Override
    public void loadRule(boolean isReturnMsg) throws Exception {
        String tenantId = InvocationInfoProxy.getTenantid();
        // 异常返回清空当前线程使用的线程变量
        clearExecuteRuleMap();
        TreeMap<Integer, TargetRuleInfoDto> executeRuleMap = getExecuteRuleMap();
        Map<Object, TargetRuleInfoDto> ruleInfoMap = relevantRuleLoadService.loadByBizObjectCode(bizObj, tenantId);
        for (TargetRuleInfoDto ruleInfoDto : ruleInfoMap.values()) {
            String ruleCode = ruleInfoDto.getCode();
            //根据编码判断是否是辨识规则
            if (ruleCode.startsWith(CmpRuleEngineTypeConstant.identification_prefix)) {
                try {
                    // 获取后缀，即规则执行序号
                    String suffixNum = ruleCode.substring(CmpRuleEngineTypeConstant.identification_prefix.length());
                    Integer codeNum = Integer.parseInt(suffixNum);
                    executeRuleMap.put(codeNum, ruleInfoDto);
                } catch (NumberFormatException e) {
                    super.dealException(logger, isReturnMsg, null, ruleCode + errorRuleCodeMsg);
                }
            }
        }
        if (executeRuleMap.size() == 0) {
            // 异常返回清空当前线程使用的线程变量
            clearExecuteRuleMap();
            logger.error(noRuleMsg);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100249"),noRuleMsg);
        }
    }

    @Override
    public void executeRule(List<BankReconciliation> bankrecList, boolean isReturnMsg) throws Exception {
        try {
            // 存放单据id与结果集对应关系
            Map<Long, Map<String, Object>> sourcesMap = new HashMap<>();
            // 先做条件处理，减少查询
            super.querySourcesValue(bankrecList, sourcesMap);
            TreeMap<Integer, TargetRuleInfoDto> executeRuleMap = getExecuteRuleMap();
            for (BankReconciliation bankrec : bankrecList) {
                logger.error("银行对账单辨识规则-执行单据交易流水号：" + bankrec.getBank_seq_no());
                // 循环执行所有规则
                for (TargetRuleInfoDto ruleInfoDto : executeRuleMap.values()) {
                    // 执行规则参数
                    RuleExtParamDto ruleExtParamDto = new RuleExtParamDto();
                    // 规则id
                    ruleExtParamDto.setRuleId(ruleInfoDto.getId());
                    // 为规则数据项具体值赋值
                    List<RuleItemDto> sources = ruleInfoDto.getSources();
                    // 日志信息
                    StringBuilder logSourceMsgCode = new StringBuilder();
                    StringBuilder logTargetMsgCode = new StringBuilder();

                    // 处理条件
                    super.dealSourcesValue(sourcesMap, bankrec, sources, logSourceMsgCode);
                    // 循环执行结果
                    for (RuleItemDto ruleItemDto : ruleInfoDto.getTargets()) {
                        ruleExtParamDto.setTarget(ruleItemDto);
                        // 条件
                        ruleExtParamDto.setSources(ruleInfoDto.getSources());
                        Object resultObj = relevantRuleExtService.assign(ruleExtParamDto);
                        if (resultObj == null) {
                            continue;
                        }
                        // 如果返回map结构，则可能为银行对账单自定义脚本
                        if (resultObj instanceof LinkedTreeMap) {
                            logger.error("银行对账单辨识规则-" + ruleInfoDto.getCode() + " 条件：" + logSourceMsgCode);
                            LinkedTreeMap<String, Object> resultMap = (LinkedTreeMap) resultObj;
                            String returnType = (String) resultMap.get("returnType");
                            // 判断返回值returnType为script，则表示自定义脚本，一次性返回多个值
                            if ("script".equals(returnType)) {
                                resultMap.remove("returnType");
                                for (Map.Entry<String, Object> entry : resultMap.entrySet()) {
                                    checkCharacterDef(bankrec, entry.getKey(), entry.getValue(), sourcesMap);
                                    // 业务处理
                                    bankrec.set(entry.getKey(), entry.getValue());
                                    sourcesMap.get(bankrec.getId()).put(entry.getKey(), entry.getValue());
                                    logTargetMsgCode.append(entry.getKey()).append(" = ").append(entry.getValue()).append(", ");
                                }
                                logger.info("银行对账单辨识规则-" + ruleInfoDto.getCode() + " 结果：" + logTargetMsgCode);
                                // 脚本会返回所有结果，无需多次执行
                                break;
                            }
                        } else { // 返回结构是一个值，则可能是规则配置
                            // 判断结果是否为特征字段
                            checkCharacterDef(bankrec, ruleItemDto.getCode(), resultObj, sourcesMap);
                            // 业务处理
                            bankrec.set(ruleItemDto.getCode(), resultObj);
                            logger.error("银行对账单辨识规则-" + ruleInfoDto.getCode() + " 结果：" + ruleItemDto.getCode() + "=" + resultObj + ".");
                        }
                    }
                }
                // 规则执行完毕后业务处理
                dealCounterpart(bankrec);
            }
        } catch (Exception ex) {
            super.dealException(logger, isReturnMsg, ex, null);
        } finally {
            clearExecuteRuleMap();
        }
    }

    /**
     * @Describe 处理特征字段结果集
     * @Param
     * @Return
     */
    private void checkCharacterDef(BankReconciliation bankrec, Object key, Object value, Map<Long, Map<String, Object>> sourcesMap) {
        BizObject characterDef = bankrec.get("characterDef");
        if (characterDef == null) {
            characterDef = new BizObject();
        }
        // 结果项为特征
        if (key != null && key.toString().startsWith("characterDef.")) {
            String defName = key.toString().substring(13);
            characterDef.set(defName, value);
            sourcesMap.get(bankrec.getId()).put(key.toString().replace(".", "_"), value);
            characterDef.setEntityStatus(EntityStatus.Update);
        }
    }

    /**
     * @Describe 处理对接人
     * @Param
     * @Return
     */
    private void dealCounterpart(BankReconciliation bankrec) throws Exception {
        String counterpart = bankrec.getCounterpart();
        String busscounterpart = bankrec.getBusscounterpart();
        bankrec.setCounterpart(null);
        bankrec.setBusscounterpart(null);
        // 更新过后单据已不是最新状态，需重新查询
        BankReconciliation bankrec_new = MetaDaoHelper.findById(BankReconciliation.ENTITY_NAME, bankrec.getId().toString(), 3);
        // 财务对接人
        if (!StringUtils.isEmpty(counterpart)) {
            String[] users = counterpart.split(",");
            bankreconciliationService.dispatchOne(bankrec_new, users);
        }
        // 业务对接人
        if (!StringUtils.isEmpty(busscounterpart)) {
            String[] users = busscounterpart.split(",");
            bankreconciliationService.dispatchBussiness(bankrec.getId().toString(), users, true);
        }
    }

}
