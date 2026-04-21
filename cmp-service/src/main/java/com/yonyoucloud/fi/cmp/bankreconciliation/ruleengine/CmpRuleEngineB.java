package com.yonyoucloud.fi.cmp.bankreconciliation.ruleengine;

import com.google.gson.internal.LinkedTreeMap;
import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.iuap.ruleengine.dto.relevant.RuleExtParamDto;
import com.yonyou.iuap.ruleengine.dto.relevant.RuleItemDto;
import com.yonyou.iuap.ruleengine.dto.relevant.TargetRuleInfoDto;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliationDetail;
import com.yonyoucloud.fi.cmp.constant.CmpRuleEngineTypeConstant;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @Description: 银行对账单冻结规则实现类
 * @Author: gengrong
 * @createTime: 2022/9/29
 * @version: 1.0
 */
@Slf4j
@Component
@Scope("prototype")
public class CmpRuleEngineB extends AbstractCmpRuleEngine {

    private static final Logger logger = LoggerFactory.getLogger(AbstractCmpRuleEngine.class);

    // 提示语
    private final String noRuleMsg = com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805D8","回单冻结失败，失败原因：未找到编码以[cmp_freeze_]开头的冻结规则，") /* "回单冻结失败，失败原因：未找到编码以[cmp_freeze_]开头的冻结规则，" */ +
            com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805D9","无法冻结回单信息，请检查冻结规则是否存在或者是否启用！") /* "无法冻结回单信息，请检查冻结规则是否存在或者是否启用！" */;

    @Override
    public void loadRule(boolean isReturnMsg) throws Exception {
        String tenantId = InvocationInfoProxy.getTenantid();
        // 先清空当前线程使用的线程变量再重新获取
        clearExecuteRuleMap();
        TreeMap<Integer, TargetRuleInfoDto> executeRuleMap = getExecuteRuleMap();
        Map<Object, TargetRuleInfoDto> ruleInfoMap = relevantRuleLoadService.loadByBizObjectCode(bizObj, tenantId);
        for (TargetRuleInfoDto ruleInfoDto : ruleInfoMap.values()) {
            String ruleCode = ruleInfoDto.getCode();
            //根据编码判断是否是冻结规则
            if (ruleCode.startsWith(CmpRuleEngineTypeConstant.freeze_prefix)) {
                // 冻结规则只有一条，找到则不继续寻找
                executeRuleMap.put(1, ruleInfoDto);
                break;
            }
        }
        if (executeRuleMap.size() == 0) {
            // 异常返回清空当前线程使用的线程变量
            clearExecuteRuleMap();
            logger.error(noRuleMsg);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102199"),noRuleMsg);
        }
    }

    @Override
    public void executeRule(List<BankReconciliation> bankrecList, boolean isReturnMsg) throws Exception {
        try {
            // 存放单据id与结果集对应关系
            Map<Long, Map<String, Object>> sourcesMap = new HashMap<>();
            // 获取对账单id集合
            List ids = bankrecList.stream().map(BankReconciliation::getId).collect(Collectors.toList());
            Map<Long, Map<String, String>> newBankRecDetailMap = new HashMap<>();

            // 获取未作废的子表数据
            QuerySchema querySchema = QuerySchema.create().addSelect("*");
            QueryConditionGroup group2 = QueryConditionGroup.and(
                    QueryCondition.name("mainid").in(ids));
            querySchema.addCondition(group2);
            List<BankReconciliationDetail> bankRecDetail = MetaDaoHelper.queryObject(BankReconciliationDetail.ENTITY_NAME, querySchema, null);
            if (bankRecDetail.size() > 0 || CollectionUtils.isNotEmpty(bankRecDetail)) {
                // 根据主表id对子表数据进行分组并转成Map
                Map<Long, List<BankReconciliationDetail>> bankRecDetailMap = bankRecDetail.stream().collect(Collectors.groupingBy(BankReconciliationDetail::getMainid));
                // 拼接子表对接人id(autheduser),以逗号分隔，区分财务对接人和业务对接人
                Set set = bankRecDetailMap.keySet();
                for (Object key : set) {
                    Map<String, String> valueMap = new HashMap<>();
                    List<BankReconciliationDetail> detailList = bankRecDetailMap.get(key);
                    StringBuilder financeSb = new StringBuilder();
                    StringBuilder operationSb = new StringBuilder();
                    for (BankReconciliationDetail detail : detailList) {
                        //拼接财务对接人id
                        if (detail.getOprtype().equals("1") || detail.getOprtype().equals("2")) {
                            financeSb.append(detail.getAutheduser()).append(",");
                        } else//拼接业务对接人id
                        {
                            operationSb.append(detail.getAutheduser()).append(",");
                        }
                    }
                    if (financeSb.length() > 0) {
                        valueMap.put("counterpart", financeSb.deleteCharAt(financeSb.length() - 1).toString());
                    } else {
                        valueMap.put("counterpart", "");
                    }
                    if (operationSb.length() > 0) {
                        valueMap.put("busscounterpart", operationSb.deleteCharAt(operationSb.length() - 1).toString());
                    } else {
                        valueMap.put("busscounterpart", "");
                    }
                    newBankRecDetailMap.put((Long) key, valueMap);
                }
            }


            // 先做条件处理，减少查询
            super.querySourcesValue(bankrecList, sourcesMap);
            TreeMap<Integer, TargetRuleInfoDto> executeRuleMap = getExecuteRuleMap();
            for (BankReconciliation bankrec : bankrecList) {
                //回写主表财务对接人记录字段counterpart和业务对接人记录字段busscounterpart
                if (newBankRecDetailMap.containsKey(bankrec.getId())) {
                    bankrec.setCounterpart(newBankRecDetailMap.get(bankrec.getId()).get("counterpart"));
                    bankrec.setBusscounterpart(newBankRecDetailMap.get(bankrec.getId()).get("busscounterpart"));
                }
                logger.error("银行对账单冻结规则-执行单据交易流水号：" + bankrec.getBank_seq_no());
                // 执行规则
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
                        logger.error("银行对账单冻结规则-" + ruleInfoDto.getCode() + " 条件：" + logSourceMsgCode);
                        LinkedTreeMap<String, Object> resultMap = (LinkedTreeMap) resultObj;
                        String returnType = (String) resultMap.get("returnType");
                        // 判断返回值returnType为script，则表示自定义脚本，一次性返回多个值
                        if ("script".equals(returnType)) {
                            resultMap.remove("returnType");
                            resultMap.forEach((key, value) -> {
                                // 业务处理
                                // 冻结状态 frozenstatus 0正常、1在解冻、2已冻结
                                bankrec.set(key, value);
                                logTargetMsgCode.append(key).append(" = ").append(value).append(", ");
                            });
                            logger.info("银行对账单冻结规则-" + ruleInfoDto.getCode() + " 结果：" + logTargetMsgCode);
                            // 脚本会返回所有结果，无需多次执行
                            break;
                        }
                    } else { // 返回结构是一个值，则可能是规则配置
                        // 业务处理
                        bankrec.set(ruleItemDto.getCode(), resultObj);
                        logger.error("银行对账单冻结规则-" + ruleInfoDto.getCode() + " 结果：" + ruleItemDto.getCode() + "=" + resultObj + ".");
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
