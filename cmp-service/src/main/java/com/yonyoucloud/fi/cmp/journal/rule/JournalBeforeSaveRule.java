package com.yonyoucloud.fi.cmp.journal.rule;

import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.ucf.basedoc.model.CurrencyTenantDTO;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.bill.rule.util.GetRoundModeUtils;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.basecom.precision.CheckPrecision;
import com.yonyoucloud.fi.basecom.precision.CheckPrecisionVo;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.basecom.utils.FIDubboUtils;
import com.yonyoucloud.fi.cmp.bankreconciliationsetting.BankReconciliationSetting;
import com.yonyoucloud.fi.cmp.cmpentity.AuditStatus;
import com.yonyoucloud.fi.cmp.cmpentity.EventSource;
import com.yonyoucloud.fi.cmp.cmpentity.EventType;
import com.yonyoucloud.fi.cmp.cmpentity.SettleStatus;
import com.yonyoucloud.fi.cmp.constant.IBussinessConstant;
import com.yonyoucloud.fi.cmp.constant.IDomainConstant;
import com.yonyoucloud.fi.cmp.ctmrpc.CtmCmpBankReconciliationSettingRpcServiceImpl;
import com.yonyoucloud.fi.cmp.journal.Journal;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.Direction;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.BankReconciliationSettingVO;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.receipt.PlanParam;
import com.yonyoucloud.fi.cmp.util.BigDecimalUtils;
import com.yonyoucloud.fi.cmp.util.DateUtils;
import com.yonyoucloud.fi.cmp.util.JedisLockUtils;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Administrator on 2019/4/16 0016.
 */
@Slf4j
@Component
public class JournalBeforeSaveRule extends AbstractCommonRule {

    private static final String BANKJOURNALWDLIST = "cmp_bankjournalwdlist";
    private static final String BANKJOURNALWD = "cmp_bankjournalwd";
    @Autowired
    BaseRefRpcService baseRefRpcService;
    @Autowired
    CtmCmpBankReconciliationSettingRpcServiceImpl cmpBankReconciliationSettingRpcService;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        List<BizObject> bills = getBills(billContext, map);
        BillDataDto billDataDto = (BillDataDto) getParam(map);
        // 导入
        boolean importFlag =  "import".equals(billDataDto.getRequestAction());
        if (bills != null && bills.size()>0) {
            for(BizObject bill:bills){
                Journal bizObject =  (Journal) bill;
                // OpenApi
                boolean openApiFlag = (bizObject.containsKey("_fromApi") && bizObject.get("_fromApi").equals(true)) || billDataDto.getFromApi();
                JedisLockUtils.isexistRjLock(bizObject.get(IBussinessConstant.ACCENTITY));
                try {
                    if(FIDubboUtils.isSingleOrg()){
                        BizObject singleOrg = FIDubboUtils.getSingleOrg();
                        if(singleOrg != null){
                            bizObject.set(IBussinessConstant.ACCENTITY,singleOrg.get("id"));
                            bizObject.set("accentity_name",singleOrg.get("name"));
                        }
                    }
                } catch (Exception e) {
                    log.error("单组织判断异常!", e);
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100850"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418024E","单组织判断异常！") /* "单组织判断异常！" */ + e.getMessage());
                }
                if (bizObject.getSettlemode() == null) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100851"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180251","结算方式不能为空！") /* "结算方式不能为空！" */);
                }
                if (ObjectUtils.isEmpty(bizObject.get("dzdate"))) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100852"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180255","交易日期不能为空！") /* "交易日期不能为空！" */);
                }
                if(BANKJOURNALWDLIST.equals(billContext.getBillnum())||BANKJOURNALWD.equals(billContext.getBillnum())){
                    if (importFlag || openApiFlag) {
                        if (bizObject.get("bankreconciliationscheme") == null) {
                            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100853"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180258","对账方案不能为空！") /* "对账方案不能为空！" */);
                        }
                        //导入进来的数据
                        checkImportDate(bizObject,map);
                        if(bizObject.getCheckflag()==null){
                            bizObject.setCheckflag(false);
                        }
                    }else{
                        if (bizObject.get("bankreconciliationscheme") == null) {
                            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100854"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00267", "对账方案不能为空！") /* "对账方案不能为空！" */);
                        }
                        //Long bankreconciliationscheme = Long.valueOf(billContext.getParameter("bankreconciliationscheme"));
                        //bizObject.set("bankreconciliationscheme",bankreconciliationscheme);
                    }


                    bizObject.setInitflag(true);
                    bizObject.setBilltype(EventType.InitDate);
                    bizObject.setSrcitem(EventSource.Cmpchase);

                    //单据已勾兑不允许修改
                    if(EntityStatus.Update.equals(bizObject.get("_status"))){
                        Long id = bizObject.get("id");
                        Journal journal = MetaDaoHelper.findById(Journal.ENTITY_NAME,id);
                        if(journal.getCheckflag() != null){
                            if(journal.getCheckflag()){
                                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100855"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418024D","该单据已勾兑完成，不允许修改") /* "该单据已勾兑完成，不允许修改" */);
                            }
                        }
                    }
                    BigDecimal debitoriSum = bizObject.getDebitoriSum();
                    BigDecimal creditoriSum = bizObject.getCreditoriSum();
                    if (debitoriSum == null && creditoriSum == null) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100856"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180254","借贷金额不能同时为空！") /* "借贷金额不能同时为空！" */);
                    }

                    if(debitoriSum!=null&&debitoriSum.compareTo(BigDecimal.ZERO)==-1){
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100857"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180256","借方金额不能小于0") /* "借方金额不能小于0" */);
                    }
                    if(creditoriSum!=null&&creditoriSum.compareTo(BigDecimal.ZERO)==-1){
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100858"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180259","贷方金额不能小于0") /* "贷方金额不能小于0" */);
                    }
                    checkDzDate(bizObject);
                }
                //结算方式
                BillContext billContextFinBank = new BillContext();
                billContextFinBank.setFullname("aa.settlemethod.SettleMethod");
                billContextFinBank.setDomain(IDomainConstant.MDD_DOMAIN_PRODUCTCENTER);
                QueryConditionGroup groupBank = QueryConditionGroup.and(QueryCondition.name("isEnabled").eq(1),QueryCondition.name("id").eq(bizObject.getSettlemode()),QueryCondition.name("tenant").eq(AppContext.getTenantId()));
                List<Map<String, Object>> dataList = MetaDaoHelper.queryAll(billContextFinBank,"id,serviceAttr,tenant",groupBank,null);
                if(dataList != null && dataList.size() > 0){
                    if (!dataList.get(0).get("serviceAttr").equals(0)) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100859"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418025B","结算方式业务属性必须为银行业务！") /* "结算方式业务属性必须为银行业务！" */);
                    }
                }
                // 根据会计主体获取会计账簿
                Date  dzdate = bizObject.get("dzdate");
                bizObject.setVouchdate(dzdate);
                if(bizObject.getDebitnatSum() == null){
                    bizObject.setDebitnatSum(BigDecimal.ZERO);
                }
                if(bizObject.getDebitoriSum() == null){
                    bizObject.setDebitoriSum(BigDecimal.ZERO);
                }
                if(bizObject.getCreditnatSum() == null){
                    bizObject.setCreditnatSum(BigDecimal.ZERO);
                }
                if(bizObject.getCreditoriSum() == null){
                    bizObject.setCreditoriSum(BigDecimal.ZERO);
                }
                if(bizObject.getOribalance() == null){
                    bizObject.setOribalance(BigDecimal.ZERO);
                }
                if(bizObject.getNatbalance() == null){
                    bizObject.setNatbalance(BigDecimal.ZERO);
                }

                //精度处理
                CurrencyTenantDTO currencyDTO = baseRefRpcService.queryCurrencyById(bizObject.get("currency"));
                RoundingMode moneyRound = GetRoundModeUtils.getCurrencyPriceRoundMode(bizObject.get("currency"), 1);
                CheckPrecisionVo checkPrecisionVo = new CheckPrecisionVo();
                checkPrecisionVo.setPrecisionId(bizObject.get("currency"));
                Map<String, BigDecimal> numericalMap = new HashMap<String, BigDecimal>();
                if(bizObject.getCreditoriSum().compareTo(BigDecimal.ZERO) > 0){
                    numericalMap.put("creditoriSum",bizObject.getCreditoriSum());
                    bizObject.setOribalance(bizObject.getCreditoriSum().setScale(currencyDTO.getMoneydigit(),moneyRound));
                }
                if(bizObject.getDebitoriSum().compareTo(BigDecimal.ZERO) > 0){
                    numericalMap.put("debitoriSum",bizObject.getDebitoriSum());
                    bizObject.setOribalance(bizObject.getDebitoriSum().setScale(currencyDTO.getMoneydigit(),moneyRound));
                }
                if(bizObject.getDebitoriSum().compareTo(BigDecimal.ZERO) <= 0&& bizObject.getCreditoriSum().compareTo(BigDecimal.ZERO) <= 0){
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100860"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418024F","借/贷原币金额不能同时为0！") /* "借/贷原币金额不能同时为0！" */);
                }
                if(bizObject.getDebitoriSum().compareTo(BigDecimal.ZERO) > 0&&bizObject.getCreditoriSum().compareTo(BigDecimal.ZERO) >0){
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100861"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180252","借/贷方原币金额不能同时大于0！") /* "借/贷方原币金额不能同时大于0！" */);
                }
                checkPrecisionVo.setNumericalMap(numericalMap);
                checkPrecisionVo.setEntityName(Journal.ENTITY_NAME);
                CheckPrecision.checkMoneyByCurrency(checkPrecisionVo);
                if(bizObject.getDebitoriSum().compareTo(BigDecimal.ZERO) > 0){
                    bizObject.setDirection(Direction.Debit);
                    bizObject.setDebitnatSum(BigDecimalUtils.safeMultiply(bizObject.getDebitoriSum().setScale(currencyDTO.getMoneydigit(),moneyRound),bizObject.getExchangerate(),4));
                    bizObject.setCreditnatSum(null);
                    bizObject.setCreditoriSum(null);
                }else if(bizObject.getCreditoriSum().compareTo(BigDecimal.ZERO) >0){
                    bizObject.setDirection(Direction.Credit);
                    bizObject.setCreditnatSum(BigDecimalUtils.safeMultiply(bizObject.getCreditoriSum().setScale(currencyDTO.getMoneydigit(),moneyRound),bizObject.getExchangerate(),4));
                    bizObject.setDebitoriSum(null);
                    bizObject.setDebitnatSum(null);
                }
                bizObject.setSettlestatus(SettleStatus.alreadySettled);
                bizObject.setAuditstatus(AuditStatus.Complete);
                bizObject.setNatbalance(BigDecimalUtils.safeMultiply(bizObject.getOribalance(),bizObject.getExchangerate(),4));
                //对所属组织进行赋值
                bizObject.setParentAccentity(baseRefRpcService.queryEnterpriseBankAccountById(bizObject.getBankaccount()).getOrgid());
            }
        }

        return new RuleExecuteResult();
    }


    private void checkDzDate(BizObject bizObject) throws Exception{
        String bankreconciliationschemename = bizObject.get("bankreconciliationschemename");
        Date dzDate = (Date)bizObject.get("dzdate"); //日记账日期

        BankReconciliationSetting bankReconciliationSetting = MetaDaoHelper.findById(BankReconciliationSetting.ENTITY_NAME,bizObject.get("bankreconciliationscheme"));

        if (bankReconciliationSetting == null) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100862"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180253","对账方案：%s 不存在，请检查后再保存。") /* "对账方案：%s 不存在，请检查后再保存。" */, bankreconciliationschemename));
        }

        Date enableDate = bankReconciliationSetting.getEnableDate();
        if (DateUtils.dateCompare(dzDate, enableDate) >= 0) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100863"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180257","日记账日期不能晚于方案启用日期，请修改以后再保存。") /* "日记账日期不能晚于方案启用日期，请修改以后再保存。" */);
        }

    }

    private   void checkImportDate(Journal journal,Map<String,Object> map) throws Exception{
        BillDataDto BillDataDto = (BillDataDto)map.get("param");
        Map<String, Object> map1 =  BillDataDto.getMapCondition();
        if(ValueUtils.isNotEmpty(map1)){
            String accentity = (String) map1.get(IBussinessConstant.ACCENTITY);
            String bankaccount = (String) map1.get("bankaccount");
            String currency = (String) map1.get("currency");
            String bankreconciliationscheme = map1.get("bankreconciliationscheme").toString();
            if(!bankreconciliationscheme.equals(journal.get("bankreconciliationscheme").toString())){
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100864"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418025A","导入对账方案与当前对账方案不匹配") /* "导入对账方案与当前对账方案不匹配" */);
            }
            //查询对账方案子表中的数据 按账户+ 币种 + 资金组织匹配 如果匹配不上 则提示
            PlanParam planParam = new PlanParam(bankaccount,currency,bankreconciliationscheme);
            List<BankReconciliationSettingVO> brSetting_bList = cmpBankReconciliationSettingRpcService.findUseOrg(planParam);
            if (CollectionUtils.isEmpty(brSetting_bList)) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100865"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:YS_CTM_CM-BE_LOCAL_00050029", "导入的资金组织与当前资金组织不一致!") /* "导入的资金组织与当前资金组织不一致!" */);
            }
            if (!bankaccount.equals(journal.getBankaccount())) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100866"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418024C","导入的银行账户与当前银行账户不一致!") /* "导入的银行账户与当前银行账户不一致!" */);
            }
            if (!currency.equals(journal.getCurrency())) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100867"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180250","导入的币种与当前币种不一致!") /* "导入的币种与当前币种不一致!" */);
            }
        }

    }


}


