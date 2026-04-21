package com.yonyoucloud.fi.cmp.bankreconciliation.ruleengine;

import com.yonyou.iuap.ruleengine.dto.relevant.RuleItemDto;
import com.yonyou.iuap.ruleengine.dto.relevant.TargetRuleInfoDto;
import com.yonyou.iuap.ruleengine.relevant.RelevantRuleExtService;
import com.yonyou.iuap.ruleengine.relevant.RelevantRuleLoadService;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.util.CmpIntComparatorUtil;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QuerySchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * @Description: 银行对账单规则引擎处理类
 * @Author: gengrong
 * @createTime: 2022/9/29
 * @version: 1.0
 */
public abstract class AbstractCmpRuleEngine {

    @Autowired
    protected RelevantRuleLoadService relevantRuleLoadService;
    @Autowired
    protected RelevantRuleExtService relevantRuleExtService;

    private static final Logger logger = LoggerFactory.getLogger(AbstractCmpRuleEngine.class);

    // 业务对象编码
    protected final String bizObj = "ctm-cmp.cmp_bankreconciliation";

    // 可执行的规则,支持多线程,使用ThreadLocal包装
    private static ThreadLocal<TreeMap<Integer,TargetRuleInfoDto>> localEecuteRuleMap = new ThreadLocal<TreeMap<Integer,TargetRuleInfoDto>>();

    // 加载规则
    public abstract void loadRule(boolean isReturnMsg) throws Exception;

    // 执行规则
    public abstract void executeRule(List<BankReconciliation> bankrecList, boolean isReturnMsg) throws Exception;

    protected TreeMap<Integer,TargetRuleInfoDto> getExecuteRuleMap(){
        if(localEecuteRuleMap.get() == null){
            TreeMap<Integer, TargetRuleInfoDto> executeRuleMap = new TreeMap<>(new CmpIntComparatorUtil());
            logger.error("线程"+Thread.currentThread().getId()+"添加变量");
            localEecuteRuleMap.set(executeRuleMap);
        }
        return localEecuteRuleMap.get();
    }

    protected void clearExecuteRuleMap(){
        logger.error("线程"+Thread.currentThread().getId()+"清空变量");
        localEecuteRuleMap.remove();
    }

    /**
     * @Describe 查询规则中条件对应的值
     * @Param bankrec 银行对账单，sources 规则条件，logMsgCode 日志信息
     * @Return
     */
    protected void dealSourcesValue(Map<Long, Map<String, Object>> sourcesMap, BankReconciliation bankrec,
                                    List<RuleItemDto> sources, StringBuilder logMsgCode) throws Exception {
        Map<String, Object> map_result = sourcesMap.get(bankrec.getId());
        for (RuleItemDto source : sources) {
            String code = source.getCode();
            String asCode = code.replace(".", "_");
            // 有些规则的条件依赖上一条规则的结果，这里如果取不到值重新从单据上取一下
            source.setValue(map_result.get(asCode) == null ? bankrec.get(asCode) : map_result.get(asCode));
            logMsgCode.append(source.getCode()).append("=").append(map_result.get(asCode)).append(", ");
        }
    }

    /**
     * @Describe 查询所有单据的规则条件值
     * @Param bankrecList 银行对账单 sourcesMap 存放单据id与结果集对应关系
     * @Return
     */
    protected void querySourcesValue(List<BankReconciliation> bankrecList, Map<Long, Map<String, Object>> sourcesMap)
            throws Exception {
        // 查询条件的值
        QuerySchema schema = QuerySchema.create().addSelect("id");
        // 主键集合
        List<String> bankrecIds = new ArrayList<>();
        for (BankReconciliation bankrec : bankrecList) {
            bankrecIds.add(bankrec.getId());
            for (TargetRuleInfoDto ruleInfoDto : getExecuteRuleMap().values()) {
                // 为规则数据项具体值赋值
                List<RuleItemDto> sources = ruleInfoDto.getSources();
                for (RuleItemDto source : sources) {
                    String code = source.getCode();
                    String asCode = code.replace(".", "_");
                    schema.addSelect(code + " as " + asCode);
                }
            }
        }
        schema.appendQueryCondition(QueryCondition.name("id").in(bankrecIds));
        List<Map<String, Object>> resultMap = MetaDaoHelper.query(BankReconciliation.ENTITY_NAME, schema, null);
        if (ValueUtils.isNotEmpty(resultMap)) {
            for (Map<String, Object> result : resultMap) {
                sourcesMap.put((Long) result.get("id"), result);
            }
        }
    }

    /**
     * @Describe 处理异常
     * @Param
     * @Return
     */
    protected void dealException(Logger logger, boolean isReturnMsg, Exception ex, String msg) throws Exception {
        // 不合规编码处理
        if (isReturnMsg) {
            if (ex != null) {
                logger.error(ex.toString());
                throw ex;
            } else {
                logger.error(msg);
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101649"),msg);
            }
        } else {
            logger.error(ex == null ? msg : ex.toString());
        }
    }
}
