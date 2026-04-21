package com.yonyoucloud.fi.cmp.bankreconciliation.ruleengine;

import com.google.gson.internal.LinkedTreeMap;
import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.iuap.ruleengine.dto.relevant.RuleExtParamDto;
import com.yonyou.iuap.ruleengine.dto.relevant.RuleItemDto;
import com.yonyou.iuap.ruleengine.dto.relevant.TargetRuleInfoDto;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.constant.CmpRuleEngineTypeConstant;
import lombok.extern.slf4j.Slf4j;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * @Description: 银行对帐单生单规则
 * @Author: gengrong
 * @createTime: 2022/9/29
 * @version: 1.0
 */
@Slf4j
@Component
@Scope("prototype")
public class CmpRuleEngineC extends AbstractCmpRuleEngine {

    private static final Logger logger = LoggerFactory.getLogger(AbstractCmpRuleEngine.class);

    // 提示语
    private final String noRuleMsg = com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807A4","回单生单失败，失败原因：未找到编码以[cmp_generate_]开头的生单规则，") /* "回单生单失败，失败原因：未找到编码以[cmp_generate_]开头的生单规则，" */ +
            com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807A5","无法确定生单信息，请检查生单规则是否存在或者是否启用！") /* "无法确定生单信息，请检查生单规则是否存在或者是否启用！" */;

    @Override
    public void loadRule(boolean isReturnMsg) throws Exception {
        String tenantId = InvocationInfoProxy.getTenantid();
        Map<Object, TargetRuleInfoDto> ruleInfoMap = relevantRuleLoadService.loadByBizObjectCode(bizObj, tenantId);
        // 先清空当前线程使用的线程变量再重新获取
        clearExecuteRuleMap();
        TreeMap<Integer, TargetRuleInfoDto> executeRuleMap = getExecuteRuleMap();
        for (TargetRuleInfoDto ruleInfoDto : ruleInfoMap.values()) {
            String ruleCode = ruleInfoDto.getCode();
            //根据编码判断是否是冻结规则
            if (ruleCode.startsWith(CmpRuleEngineTypeConstant.generate_prefix)) {
                // 生单规则只有一条，找到则不继续寻找
                executeRuleMap.put(1, ruleInfoDto);
                logger.error("线程"+Thread.currentThread().getId()+"变量存值");
                break;
            }
        }
        if (executeRuleMap.size() == 0) {
            // 异常返回清空当前线程使用的线程变量
            clearExecuteRuleMap();
            logger.error(noRuleMsg);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102113"),noRuleMsg);
        }
    }

    @Override
    public void executeRule(List<BankReconciliation> bankrecList, boolean isReturnMsg) throws Exception {
        try {
            // 存放单据id与结果集对应关系
            Map<Long, Map<String, Object>> sourcesMap = new HashMap<>();
            // 先做条件处理，减少查询
            super.querySourcesValue(bankrecList, sourcesMap);
            // 获取执行rule
            TreeMap<Integer, TargetRuleInfoDto> executeRuleMap = getExecuteRuleMap();
            for (BankReconciliation bankrec : bankrecList) {
                logger.error("银行对账单生单规则-执行单据交易流水号：" + bankrec.getBank_seq_no());
                // 执行规则
                logger.error("线程"+Thread.currentThread().getId()+"变量取值");
                TargetRuleInfoDto ruleInfoDto = executeRuleMap.get(1);
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
                // 处理结果
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
                        logger.error("银行对账单生单规则-" + ruleInfoDto.getCode() + " 条件：" + logSourceMsgCode);
                        LinkedTreeMap<String, Object> resultMap = (LinkedTreeMap) resultObj;
                        String returnType = (String) resultMap.get("returnType");
                        // 判断返回值returnType为script，则表示自定义脚本，一次性返回多个值
                        if ("script".equals(returnType)) {
                            resultMap.remove("returnType");
                            resultMap.forEach((key, value) -> {
                                // 业务处理
                                bankrec.set(key, value);
                                logTargetMsgCode.append(key).append(" = ").append(value).append(", ");
                            });
                            logger.error("银行对账单生单规则-" + ruleInfoDto.getCode() + " 结果：" + logTargetMsgCode);
                            // 脚本会返回所有结果，无需多次执行
                            break;
                        }
                    } else { // 返回结构是一个值，则可能是规则配置
                        // 业务处理
                        bankrec.set(ruleItemDto.getCode(), resultObj);
                        logger.error("银行对账单生单规则-" + ruleInfoDto.getCode() + " 结果：" + ruleItemDto.getCode() + "=" + resultObj + ".");
                    }
                }
            }
        } catch (Exception ex) {
            super.dealException(logger, isReturnMsg, ex, null);
        } finally {
            clearExecuteRuleMap();
        }
    }
}
