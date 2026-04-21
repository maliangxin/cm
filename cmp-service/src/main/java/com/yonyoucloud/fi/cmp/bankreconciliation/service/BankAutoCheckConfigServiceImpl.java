package com.yonyoucloud.fi.cmp.bankreconciliation.service;

import com.yonyou.iuap.yms.id.generator.YmsOidGenerator;
import com.yonyou.iuap.yms.lock.YmsLock;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.yonbip.ctm.logger.business.CTMCMPBusinessLogService;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.bankautocheckconfig.BankAutoCheckConfig;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankAutoCheckConfigService;
import com.yonyoucloud.fi.cmp.constant.IMsgConstant;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import com.yonyoucloud.fi.cmp.util.CmpMetaDaoHelper;
import com.yonyoucloud.fi.cmp.util.JedisLockUtils;
import lombok.extern.slf4j.Slf4j;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * @description: 银行对账，自动对账方案设置具体实现
 * @author: wanxbo@yonyou.com
 * @date: 2022/11/10 19:02
 */

@Slf4j
@Service
@Transactional(rollbackFor = RuntimeException.class)
public class BankAutoCheckConfigServiceImpl implements BankAutoCheckConfigService {

    @Resource
    private YmsOidGenerator ymsOidGenerator;

    @Autowired
    private CTMCMPBusinessLogService ctmcmpBusinessLogService;

    /**
     * 查询当前租户的自动对账方案
     * @param params
     * @return
     */
    @Override
    public BankAutoCheckConfig queryConfigInfo(CtmJSONObject params) throws Exception {
        String ytenantId = AppContext.getYTenantId();
        String userId = AppContext.getCurrentUser().getYhtUserId();
        QuerySchema schema = QuerySchema.create().addSelect("*");
        QueryConditionGroup conditionGroup = new QueryConditionGroup();
        conditionGroup.addCondition(
                QueryConditionGroup.and(QueryCondition.name("ytenantId").eq(ytenantId)),
                QueryConditionGroup.and(QueryCondition.name("userId").eq(userId))
                );
        schema.addCondition(conditionGroup);
        List<Map<String, Object>> configs = MetaDaoHelper.query(BankAutoCheckConfig.ENTITY_NAME,schema);
        if (configs != null) {
            for (Map<String, Object> map : configs) {
                BankAutoCheckConfig bankAutoCheckConfig = new BankAutoCheckConfig();
                bankAutoCheckConfig.init(map);
                return bankAutoCheckConfig;
            }
        }else { //增加了用户级的自动对账方案，若没有则查询下租户级的，赋值给用户级
            QuerySchema schemaTenant = QuerySchema.create().addSelect("*");
            QueryConditionGroup conditionGroupTenant = new QueryConditionGroup();
            conditionGroupTenant.addCondition(
                    QueryConditionGroup.and(QueryCondition.name("ytenantId").eq(ytenantId))
            );
            schemaTenant.addCondition(conditionGroupTenant);
            BankAutoCheckConfig tenantConfig = MetaDaoHelper.queryOne(BankAutoCheckConfig.ENTITY_NAME,schema);
            if (tenantConfig != null) {
                BankAutoCheckConfig userBankAutoCheckConfig = new BankAutoCheckConfig();
                userBankAutoCheckConfig.init(tenantConfig);
                userBankAutoCheckConfig.setUserId(userId);
                userBankAutoCheckConfig.setId(ymsOidGenerator.nextId());
                userBankAutoCheckConfig.setEntityStatus(EntityStatus.Insert);
                MetaDaoHelper.insert(BankAutoCheckConfig.ENTITY_NAME,userBankAutoCheckConfig);
                //业务日志记录
                CtmJSONObject logParams = new CtmJSONObject();
                logParams.put("msg",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540043F", "存在租户级不存在用户级的，设置同步到用户级") /* "存在租户级不存在用户级的，设置同步到用户级" */);
                logParams.put("insertConfig",userBankAutoCheckConfig);
                ctmcmpBusinessLogService.saveBusinessLog(logParams, userId, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540043D", "自动对账设置") /* "自动对账设置" */, IServicecodeConstant.BANKRECONCILIATION, IMsgConstant.RECONCILIATE, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400440", "自动对账设置新增（同步租户级）") /* "自动对账设置新增（同步租户级）" */);
                return userBankAutoCheckConfig;
            }
        }
        return null;
    }

    @Override
    public String updateConfigInfo(CtmJSONObject params) throws Exception{
        String key = "BankAutoConfigKey：" + AppContext.getYTenantId();//@notranslate
        //根据租户ID锁定，只能一个租户同时操作
        YmsLock ymsLock = JedisLockUtils.lockBillWithOutTrace(key);
        if (null == ymsLock) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101486"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180795","该单据已锁定，请稍后重试！") /* "该单据已锁定，请稍后重试！" */);
        }
        try {
            Long id = params.getLong("id");
            String userId = AppContext.getCurrentUser().getYhtUserId();
            if (id != null){ //有id为更新
                BankAutoCheckConfig config = MetaDaoHelper.findById(BankAutoCheckConfig.ENTITY_NAME,id);
                //业务日志记录
                CtmJSONObject logParams = new CtmJSONObject();
                logParams.put("msg",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400441", "自动对账设置用户级更新") /* "自动对账设置用户级更新" */);
                logParams.put("beforeUpdateConfig",CtmJSONObject.toJSONString(config));
                config.setEntityStatus(EntityStatus.Update);
                //更新数据
                initConfigInfo(params,config);
                MetaDaoHelper.update(BankAutoCheckConfig.ENTITY_NAME,config);
                logParams.put("afterUpdateConfig",CtmJSONObject.toJSONString(config));
                ctmcmpBusinessLogService.saveBusinessLog(logParams, userId, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540043D", "自动对账设置") /* "自动对账设置" */, IServicecodeConstant.BANKRECONCILIATION, IMsgConstant.RECONCILIATE, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540043E", "自动对账设置更新") /* "自动对账设置更新" */);
            }else { //无id为新增
                BankAutoCheckConfig config = new BankAutoCheckConfig();
                config.setId(ymsOidGenerator.nextId());
                config.setUserId(userId);
                config.setEntityStatus(EntityStatus.Insert);
                //拼装数据
                initConfigInfo(params,config);
                CmpMetaDaoHelper.insert(BankAutoCheckConfig.ENTITY_NAME,config);
                //业务日志记录
                CtmJSONObject logParams = new CtmJSONObject();
                logParams.put("msg",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540043B", "自动对账设置用户级新增") /* "自动对账设置用户级新增" */);
                logParams.put("insertConfig",config);
                ctmcmpBusinessLogService.saveBusinessLog(logParams, userId, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540043D", "自动对账设置") /* "自动对账设置" */, IServicecodeConstant.BANKRECONCILIATION, IMsgConstant.RECONCILIATE, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540043C", "自动对账设置新增") /* "自动对账设置新增" */);
            }
        }catch (Exception e){
            throw e;
        }finally {
            JedisLockUtils.unlockBillWithOutTrace(ymsLock);
        }
        return "success";
    }

    /**
     * 组装自动对账方案
     * @param params
     * @param config
     */
    private void initConfigInfo(CtmJSONObject params,BankAutoCheckConfig config){
        //是否本方账号相同
        Boolean accountFlag = ("1").equals(params.getString("accountflag")) || ("true").equals(params.getString("accountflag")) ? true : false;
        config.setAccountflag(accountFlag);
        //是否币种相同 currencyflag
        Boolean currencyFlag = ("1").equals(params.getString("currencyflag")) || ("true").equals(params.getString("currencyflag")) ? true : false;
        config.setCurrencyflag(currencyFlag);
        //是否金额相同 amountflag
        Boolean amountFlag = ("1").equals(params.getString("amountflag")) || ("true").equals(params.getString("amountflag")) ? true : false;
        config.setAmountflag(amountFlag);
        //是否借贷方向相反 directionflag
        Boolean directionFlag = ("1").equals(params.getString("directionflag")) || ("true").equals(params.getString("directionflag")) ? true : false;
        config.setDirectionflag(directionFlag);
        //浮动天数
        config.setChangedays(params.getInteger("changedays"));
        //是否对方账号相同 toaccountflag
        Boolean toaccountFlag = ("1").equals(params.getString("toaccountflag")) || ("true").equals(params.getString("toaccountflag")) ? true : false;
        config.setToaccountflag(toaccountFlag);
        //摘要匹配方式
        config.setRemarkmatch(params.getShort("remarkmatch"));
        //是否银行对账码相同 bankchecknoflag
        Boolean bankchecknoFlag = ("1").equals(params.getString("bankchecknoflag")) || ("true").equals(params.getString("bankchecknoflag"))  ? true : false;
        config.setBankchecknoflag(bankchecknoFlag);
        //是否按关键要素对账
        config.setKeyElementMatchFlag(params.getShort("keyElementMatchFlag"));
        //关键要素-票据号匹配方式
        config.setNotenoMatchMethod(params.getShort("notenoMatchMethod"));
        //关键要素-对方名称匹配方式
        if (params.get("othernameMatchMethod") != null){
            config.setOthernameMatchMethod(params.getShort("othernameMatchMethod"));
        }
        //关键要素-相同数据匹配方式:0不匹配；1最近日期匹配；2最远日期匹配；3随机匹配
        config.setSamedataMatchMethod(params.getShort("samedataMatchMethod"));
    }
}
