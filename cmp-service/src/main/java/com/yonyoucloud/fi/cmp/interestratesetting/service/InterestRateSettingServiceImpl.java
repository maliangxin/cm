package com.yonyoucloud.fi.cmp.interestratesetting.service;

import com.yonyou.iuap.ucf.common.i18n.MessageUtils;
import com.yonyou.iuap.yms.id.generator.YmsOidGenerator;
import com.yonyou.ucf.basedoc.model.BankAcctCurrencyVO;
import com.yonyou.ucf.basedoc.model.CurrencyTenantDTO;
import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVO;
import com.yonyou.ucf.mdd.common.enums.OperationTypeEnum;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.dao.sql.SqlHelper;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.logger.business.CTMCMPBusinessLogService;
import com.yonyou.yonbip.ctm.util.lock.CtmLockTool;
import com.yonyou.yonbip.ctm.util.lock.LockStatus;
import com.yonyoucloud.fi.basecom.itf.IFIBillService;
import com.yonyoucloud.fi.cmp.cmpentity.OptionType;
import com.yonyoucloud.fi.cmp.cmpentity.VoucherStatus;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.IMsgConstant;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import com.yonyoucloud.fi.cmp.interestratesetting.InterestRateSetting;
import com.yonyoucloud.fi.cmp.util.AssertUtils;
import com.yonyoucloud.fi.cmp.util.DateUtils;
import com.yonyoucloud.fi.cmp.util.MetaDaoUtils;
import com.yonyoucloud.fi.cmp.util.basedoc.CurrencyQueryService;
import com.yonyoucloud.fi.cmp.util.basedoc.EnterpriseBankQueryService;
import com.yonyoucloud.fi.cmp.vo.WithholdingResultVO;
import com.yonyoucloud.fi.cmp.withholding.AgreeIRSettingGradeHistory;
import com.yonyoucloud.fi.cmp.withholding.AgreeIRSettingHistory;
import com.yonyoucloud.fi.cmp.withholding.WithholdingRuleSetting;
//import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.biz.base.Objectlizer;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.base.Json;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QueryOrderby;
import org.imeta.orm.schema.QuerySchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.yonyoucloud.fi.cmp.constant.ICmpConstant.ID;

/**
 * 银行利率设置相关接口实现*
 *
 * @author xuxbo
 * @date 2023/4/25 19:42
 */

@Service
//@Slf4j
@Transactional
public class InterestRateSettingServiceImpl implements InterestRateSettingService {
    private static final Logger logger = LoggerFactory.getLogger(InterestRateSettingServiceImpl.class);
    private static final String BILLNUM = "cmp_interestratesettinglist";
    private static final String FIELD_NAME = "interestratesettinglist";
    private static final String CMP_WITHHOLDINGRULESETTING = "cmp_withholdingrulesetting";
    private static final String CMP_INTERESTRATESETTING = "cmp_interestratesetting";
    private static final String CMP_AGREEIRSETTINGDETAIL = "cmp_agreeirsettingdetail";
    private static final String AGREE_NAME = "agreeIRSetting";
    private static final int NEW_SCALE = 6;
    private static final BigDecimal ZERO = new BigDecimal("0");
    private static final String RATE_SETTINGS = "yonbip_fi_ctmcmp—lock:interestratesettings_";//@notranslate
    private static final String RATE_AGREES = "yonbip_fi_ctmcmp—lock:agreeirsettings_";//@notranslate
    @Autowired
    YmsOidGenerator ymsOidGenerator;

    @Autowired
    EnterpriseBankQueryService enterpriseBankQueryService;

    @Autowired
    CurrencyQueryService currencyQueryService;

    @Autowired
    private IFIBillService ifiBillService;

    @Autowired
    private CTMCMPBusinessLogService ctmcmpBusinessLogService;


    /**
     * 银行利率设置提交保存接口实现*
     *
     * @param interestRateSetting
     * @return
     * @throws Exception
     */
    @Override
    public CtmJSONObject interestRateSettingSave(InterestRateSetting interestRateSetting) throws Exception {
        //校验必输性
        checkData(interestRateSetting);
        return doSetting(interestRateSetting);
    }

    /**
     * 银行利率设置提交保存接口实现*
     *
     * @param bill
     * @return
     * @throws Exception
     */
    @Override
    public CtmJSONObject agreeRateSettingSave(CtmJSONObject bill) throws Exception {
        //校验必输性
        //checkData(interestRateSetting);
        return doAgreeSetting(bill);
    }

    /**
     * 必输项校验*
     *
     * @param interestRateSetting
     * @throws Exception
     */
    private void checkData(InterestRateSetting interestRateSetting) throws Exception {
        AssertUtils.isNull(interestRateSetting, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180548","请求数据不可为空！") /* "请求数据不可为空！" */);
        AssertUtils.isEmpty(interestRateSetting.getInterestRateSettingList(), MessageUtils.getMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180549","银行账户利率设置数据不可为空") /* "银行账户利率设置数据不可为空" */));
        //存款利率变更方式
//        short depositChangeType = interestRateSetting.getDepositChangeType();
        if (ObjectUtils.isEmpty(interestRateSetting.get("depositChangeType"))) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101784"),MessageUtils.getMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418053D","存款利率变更方式不可为空！") /* "存款利率变更方式不可为空！" */));
        }
        //存款利率变更值
//        BigDecimal depositChangeValue = new BigDecimal(interestRateSetting.get("depositChangeValue").toString());
        if (ObjectUtils.isEmpty(interestRateSetting.get("depositChangeValue"))) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101785"),MessageUtils.getMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180541","存款利率变更值不可为空！") /* "存款利率变更值不可为空！" */));
        }
        //利率生效日期
        if (ObjectUtils.isEmpty(interestRateSetting.get("startDate"))) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101786"),MessageUtils.getMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180544","利率生效日期不可为空！") /* "利率生效日期不可为空！" */));
        }
    }

    /**
     * 利率设置*
     *
     * @param interestRateSetting
     * @return
     * @throws Exception
     */
    private CtmJSONObject doSetting(InterestRateSetting interestRateSetting) throws Exception {
        List<LinkedHashMap> bizObjects = interestRateSetting.get(FIELD_NAME);
        Map<Long, LinkedHashMap> bizObjectMap = new HashMap<>();
        for (LinkedHashMap linkedHashMap : bizObjects) {
            Long id = Long.parseLong(linkedHashMap.get(ICmpConstant.PRIMARY_ID).toString());
            bizObjectMap.put(id, linkedHashMap);
        }
        List<Long> ids = new ArrayList<>();
        List<String> args = new ArrayList<>();
        List<String> keys = new ArrayList<>();
        bizObjectMap.keySet().forEach(id -> {
            args.add(id.toString());
            ids.add(id);
            keys.add(RATE_SETTINGS + AppContext.getCurrentUser().getYTenantId() + id);
        });
        List<InterestRateSetting> bizObjectss =  interestRateSetting.get(FIELD_NAME);

        try {
            return CtmLockTool.executeInOneServiceExclusivelyBatchLock(keys, 600L, TimeUnit.SECONDS, (int status) -> {
                if (status == LockStatus.GETLOCK_FAIL) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101787"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418053F","单据正在操作，请稍后再试！") /* "单据正在操作，请稍后再试！" */);
                }
                //根据id 去库里查询最新的单据
                List<InterestRateSetting> interestRateSettingList = MetaDaoUtils.batchQueryBizObject(InterestRateSetting.ENTITY_NAME, ICmpConstant.PRIMARY_ID, ids);
                if (CollectionUtils.isEmpty(interestRateSettingList)) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101788"),MessageUtils.getMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180545","单据不存在或已被删除！") /* "单据不存在或已被删除！" */));
                }
                //获取数据的条数
                int size = interestRateSettingList.size();
                //需要更新的列表
                List<InterestRateSetting> interestRateSettings = new ArrayList<>();
                //返回结果实体
                WithholdingResultVO withholdingResultVO = new WithholdingResultVO();
                //遍历查询出的最新的单据列表
                for (InterestRateSetting bill:interestRateSettingList) {
                    try {
                        //校验：平台银行账户档案的状态，如果为停用，提交失败
                        checkEnable(bill);
                        //组装利率设置信息
                        Long mapId = Long.parseLong(bill.getId().toString());
                        assembledata(interestRateSetting, bill, interestRateSettings, bizObjectMap.get(mapId));
                    } catch (Exception e) {
                        if (size == 1) {
                            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101789"),e.getMessage());
                        }
                        // 组装错误信息
                        withholdingResultVO.getFailed().put(bill.getId().toString(), bill.getId());
                        //根据账号id和币种id查询 账号以及币种name
                        EnterpriseBankAcctVO enterpriseBankAcctVO = null;
                        CurrencyTenantDTO currencyTenantDTO = null;
                        enterpriseBankAcctVO = enterpriseBankQueryService.findByIdAndEnable(bill.getBankAccount());
                        //判断 如果为空 传入enable 为停用 重新查
                        //如果为空 说明未启用
//                        if (ObjectUtils.isEmpty(enterpriseBankAcctVO)) {
//                            Integer enable = 2;
//                            enterpriseBankAcctVO = enterpriseBankQueryService.findByIdAndEnable(bill.getBankAccount(),enable);
//                        }
                        currencyTenantDTO = currencyQueryService.findById(bill.getCurrency());
                        String bankaccount = enterpriseBankAcctVO.getAccount();
                        String currencyName = currencyTenantDTO.getName();
                        withholdingResultVO.getMessages().add(String.format(MessageUtils.getMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00256", "银行账号为【%s】，币种为【%s】%s！") /* "银行账号为【%s】，币种为【%s】%s！" */), bankaccount, currencyName, e.getMessage()));
                        withholdingResultVO.addFailCount();
                        continue;
                    }
                }
                if (CollectionUtils.isNotEmpty(interestRateSettings)) {
                    BillDataDto bill = new BillDataDto();
                    bill.setBillnum(CMP_INTERESTRATESETTING);
                    bill.setData(interestRateSettings);
                    // 通过save规则保存数据
                    ifiBillService.executeUpdate(OperationTypeEnum.SAVE.getValue(), bill);
                }
                //增加业务日志
                try {
                    CtmJSONObject jsonObject = new CtmJSONObject();
                    jsonObject.put("InterestRateSetting", interestRateSetting);
                    jsonObject.put("interestRateSettings", interestRateSettings);
                    ctmcmpBusinessLogService.saveBusinessLog(jsonObject, "", "", IServicecodeConstant.INTERESTRATESETTING,
                            IMsgConstant.CMP_INTERESTRATESETTING, IMsgConstant.CMP_INTERESTRATESETTING);
                } catch (Exception e) {
                    logger.info("============= insertOrUpdate ctmcmpBusinessLogService：" + e.getMessage());
                }

                withholdingResultVO.setCount(size);
                withholdingResultVO.setSucessCount(size - withholdingResultVO.getFailCount());
                return withholdingResultVO.getResult();
            });
        } catch (Exception e) {
            logger.error("interestsetting doSetting error", e);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101790"),e.getMessage());
        }
    }


    /**
     * 校验：平台银行账户档案的状态，如果为停用，提交失败 *
     *
     * @param checkSetting
     * @throws Exception
     */
    private void checkEnable(InterestRateSetting checkSetting) throws Exception {
        //银行账户id
        String bankaccountId = checkSetting.getBankAccount();
        //币种id
        String currencyId = checkSetting.getCurrency();
        EnterpriseBankAcctVO enterpriseBankAcctVO = enterpriseBankQueryService.findById(bankaccountId);
        //如果为空 说明未启用
        if (ObjectUtils.isEmpty(enterpriseBankAcctVO)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101791"),MessageUtils.getMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00255", "银行账户档案已停用，利率设置失败!") /* "银行账户档案已停用，利率设置失败!" */));
        } else {
            //银行账号的启停用
            int enable = enterpriseBankAcctVO.getEnable();
            int currencyEnable = 1;
            //查询币种的启停用状态
            List<BankAcctCurrencyVO> currencyList = enterpriseBankAcctVO.getCurrencyList();
            for (BankAcctCurrencyVO bankAcctCurrencyVO : currencyList) {
                if (bankAcctCurrencyVO.getCurrency().equals(currencyId)) {
                    //币种的启停用
                    currencyEnable = bankAcctCurrencyVO.getCurrencyEnable();
                }
            }
            if (ObjectUtils.isNotEmpty(enable) && (enable != 1 || currencyEnable != 1)) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101791"),MessageUtils.getMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00255", "银行账户档案已停用，利率设置失败!") /* "银行账户档案已停用，利率设置失败!" */));
            }
        }
    }


    /**
     * *
     *
     * @param interestRateSetting
     * @param childReteSetting
     * @param interestRateSettings
     * @param map
     * @throws Exception
     */
    private void assembledata(InterestRateSetting interestRateSetting, InterestRateSetting childReteSetting, List<InterestRateSetting> interestRateSettings, LinkedHashMap map) throws Exception {

        //校验单据是否是最新状态
        Date oldPubts = DateUtils.parseDate(map.get("pubts").toString(), DateUtils.DATE_TIME_FORMAT);
        AssertUtils.isTrue(oldPubts.before(childReteSetting.getPubts()), MessageUtils.getMessage("P_YS_CTM_STCT-BE_1611569242492108851") /* "单据不是最新状态" */);
        //存款利率变更方式
        short depositChangeType = interestRateSetting.getDepositChangeType();
        //存款利率变更值
        BigDecimal depositChangeValue = new BigDecimal(interestRateSetting.get("depositChangeValue").toString());
        // 基数100
        BigDecimal cardinalNumber = new BigDecimal(ICmpConstant.SELECT_HUNDRED_PARAM);
        //存款利率
        BigDecimal interestRate = childReteSetting.getInterestRate();
        switch (depositChangeType) {
            case 1:
                BigDecimal newValue = depositChangeValue.divide(cardinalNumber).add(new BigDecimal(ICmpConstant.SELECT_ONE_PARAM));
                interestRate = interestRate.multiply(newValue).setScale(NEW_SCALE, BigDecimal.ROUND_HALF_UP);
                break;
            case 2:
                interestRate = depositChangeValue.add(interestRate).setScale(NEW_SCALE, BigDecimal.ROUND_HALF_UP);
                break;
            case 3:
                interestRate = depositChangeValue.setScale(NEW_SCALE, BigDecimal.ROUND_HALF_UP);
                break;
            default:
                interestRate = childReteSetting.getInterestRate();
                break;
        }
        //判断 存款利率不能小于0
        AssertUtils.isTrue(interestRate.compareTo(ZERO) < 0, String.format(MessageUtils.getMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180540","存款利率【%s】调整后小于0，不允许调整！") /* "存款利率【%s】调整后小于0，不允许调整！" */), interestRate));
        //赋值
        childReteSetting.setInterestRate(interestRate);
        //透支利率变更方式 overdraftChangeType  透支利率变更值 overdraftChangeValue
        if (ObjectUtils.isNotEmpty(interestRateSetting.getOverdraftChangeType()) && ObjectUtils.isNotEmpty(interestRateSetting.get("overdraftChangeValue"))) {
            short overdraftChangeType = interestRateSetting.getOverdraftChangeType();
            BigDecimal overdraftChangeValue = new BigDecimal(interestRateSetting.get("overdraftChangeValue").toString());
            //透支利率
            BigDecimal overdraftRate = childReteSetting.getOverdraftRate();
            switch (overdraftChangeType) {
                case 1:
                    BigDecimal newValue = overdraftChangeValue.divide(cardinalNumber).add(new BigDecimal(ICmpConstant.SELECT_ONE_PARAM));
                    overdraftRate = overdraftRate.multiply(newValue).setScale(NEW_SCALE, BigDecimal.ROUND_HALF_UP);
                    break;
                case 2:
                    overdraftRate = overdraftChangeValue.add(overdraftRate).setScale(NEW_SCALE, BigDecimal.ROUND_HALF_UP);
                    break;
                case 3:
                    overdraftRate = overdraftChangeValue.setScale(NEW_SCALE, BigDecimal.ROUND_HALF_UP);
                    break;
                default:
                    overdraftRate = childReteSetting.getOverdraftRate();
                    break;
            }
            //判断 透支利率不能小于0
            AssertUtils.isTrue(overdraftRate.compareTo(ZERO) < 0, String.format(MessageUtils.getMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418054A","透支利率【%s】调整后小于0，不允许调整！") /* "透支利率【%s】调整后小于0，不允许调整！" */), overdraftRate));
            //赋值
            childReteSetting.setOverdraftRate(overdraftRate);
        }
        //计息天数
        if (ObjectUtils.isNotEmpty(interestRateSetting.getInterestDays())) {
            short interestDays = interestRateSetting.getInterestDays();
            childReteSetting.setInterestDays(interestDays);
        }
        //利率生效日期 必填
        Date startDate = DateUtils.parseDate(interestRateSetting.get("startDate").toString());
        //校验利率生效日期 校验利率生效日期不能 小于等于 该币种银行账户的上次预提结束日/上次结息结束日（存储在账户预提规则表）
        checkStartDate(startDate, childReteSetting);
        childReteSetting.setStartDate(startDate);
        childReteSetting.setEntityStatus(EntityStatus.Update);
        interestRateSettings.add(childReteSetting);
    }

    /**
     * 校验利率生效日期*
     *
     * @param startDate
     * @param childRateSetting
     */
    private void checkStartDate(Date startDate, InterestRateSetting childRateSetting) throws Exception {
        //根据 利率设置表中的 预提规则设置id（accountNumberId）查询预提规则设置表中的数据，取上次预提结束日和上次结息结束日 并分别判空以及比较
        Long accountNumberId = childRateSetting.getAccountNumberId();
        WithholdingRuleSetting withholdingRuleSetting = MetaDaoHelper.findById(WithholdingRuleSetting.ENTITY_NAME, accountNumberId);
        if (ObjectUtils.isEmpty(withholdingRuleSetting)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101792"),MessageUtils.getMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180547","预提规则数据查询失败！") /* "预提规则数据查询失败！" */));
        }
        //上次预提结束日
        Date lastInterestAccruedDate = withholdingRuleSetting.getLastInterestAccruedDate();
        if (ObjectUtils.isNotEmpty(lastInterestAccruedDate)) {
            if (!startDate.after(lastInterestAccruedDate)) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101793"),MessageUtils.getMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418053E","利率生效日期小于等于账户上次预提结束日，不允许调整!") /* "利率生效日期小于等于账户上次预提结束日，不允许调整!" */));
            }
        }
        //上次结息结束日
        Date lastInterestSettlementDate = withholdingRuleSetting.getLastInterestSettlementDate();
        if (ObjectUtils.isNotEmpty(lastInterestSettlementDate)) {
            if (!startDate.after(lastInterestSettlementDate)) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101794"),MessageUtils.getMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180542","利率生效日期小于等于账户上次结息结束日，不允许调整!") /* "利率生效日期小于等于账户上次结息结束日，不允许调整!" */));
            }
        }
    }

    /**
     * 协定存利率设置*
     *
     * @param AgreeSetting
     * @return
     * @throws Exception
     */
    private CtmJSONObject doAgreeSetting(Map<String,Object> AgreeSetting) throws Exception {
        //Map<String,Object> AgreeSetting =  bills;
        List<LinkedHashMap> bizObjects = (List<LinkedHashMap>) AgreeSetting.get(FIELD_NAME);
        Map<Long, LinkedHashMap> bizObjectMap = new HashMap<>();
        for (LinkedHashMap linkedHashMap : bizObjects) {
            Long id = Long.parseLong(linkedHashMap.get(ICmpConstant.PRIMARY_ID).toString());
            bizObjectMap.put(id, linkedHashMap);
        }
        List<Long> ids = new ArrayList<>();
        List<String> args = new ArrayList<>();
        List<String> keys = new ArrayList<>();
        bizObjectMap.keySet().forEach(id -> {
            args.add(id.toString());
            ids.add(id);
            keys.add(RATE_AGREES + AppContext.getCurrentUser().getYTenantId() + id);
        });

        try {
            return CtmLockTool.executeInOneServiceExclusivelyBatchLock(keys, 600L, TimeUnit.SECONDS, (int status) -> {
                if (status == LockStatus.GETLOCK_FAIL) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101787"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418053F","单据正在操作，请稍后再试！") /* "单据正在操作，请稍后再试！" */);
                }
                //获取数据的条数
                int size = bizObjects.size();
                //需要更新的列表
                List<WithholdingRuleSetting> withholdingRuleSettings = new ArrayList<>();
                //返回结果实体
                WithholdingResultVO withholdingResultVO = new WithholdingResultVO();
                //遍历查询出的最新的单据列表
                for (Map<String, Object> bill:bizObjects) {
                    try {
                        //校验：平台银行账户档案的状态，如果为停用，提交失败
                        //checkEnable(bill);
                        //组装利率设置信息
                        Long mapId = Long.parseLong(bill.get("id").toString());
                        assembleAgreedata(AgreeSetting, bill, withholdingRuleSettings, bizObjectMap.get(mapId));
                    } catch (Exception e) {
                        if (size == 1) {
                            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101795"),e.getMessage());
                        }
                        // 组装错误信息
                        withholdingResultVO.getFailed().put(bill.get("id").toString(), bill.get("id"));
                        //根据账号id和币种id查询 账号以及币种name
                        EnterpriseBankAcctVO enterpriseBankAcctVO = null;
                        CurrencyTenantDTO currencyTenantDTO = null;
                        enterpriseBankAcctVO = enterpriseBankQueryService.findByIdAndEnable(bill.get("bankAccount").toString());
                        //判断 如果为空 传入enable 为停用 重新查
                        //如果为空 说明未启用
//                        if (ObjectUtils.isEmpty(enterpriseBankAcctVO)) {
//                            Integer enable = 2;
//                            enterpriseBankAcctVO = enterpriseBankQueryService.findByIdAndEnable(bill.getBankAccount(),enable);
//                        }
                        currencyTenantDTO = currencyQueryService.findById(bill.get("currency").toString());
                        String bankaccount = enterpriseBankAcctVO.getAccount();
                        String currencyName = currencyTenantDTO.getName();
                        withholdingResultVO.getMessages().add(String.format(MessageUtils.getMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00256", "银行账号为【%s】，币种为【%s】%s！") /* "银行账号为【%s】，币种为【%s】%s！" */), bankaccount, currencyName, e.getMessage()));
                        withholdingResultVO.addFailCount();
                        continue;
                    }
                }
                if (CollectionUtils.isNotEmpty(withholdingRuleSettings)) {
                    BillDataDto bill = new BillDataDto();
                    bill.setBillnum(CMP_AGREEIRSETTINGDETAIL);
                    bill.setData(withholdingRuleSettings);
                    // 通过save规则保存数据
                    ifiBillService.executeUpdate(OperationTypeEnum.SAVE.getValue(), bill);
                }
                //增加业务日志
                try {
                    CtmJSONObject jsonObject = new CtmJSONObject();
                    jsonObject.put("InterestRateSetting", bizObjects);
                    jsonObject.put("interestRateSettings", withholdingRuleSettings);
                    ctmcmpBusinessLogService.saveBusinessLog(jsonObject, "", "", IServicecodeConstant.INTERESTRATESETTING,
                            IMsgConstant.CMP_INTERESTRATESETTING, IMsgConstant.CMP_INTERESTRATESETTING);
                } catch (Exception e) {
                    logger.info("============= insertOrUpdate ctmcmpBusinessLogService：" + e.getMessage());
                }

                withholdingResultVO.setCount(size);
                withholdingResultVO.setSucessCount(size - withholdingResultVO.getFailCount());
                return withholdingResultVO.getResult();
            });
        } catch (Exception e) {
            logger.error("agreeirsetting doSetting error", e);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101796"),e.getMessage());
        }
    }
    /**
     * *
     *
     * @param AgreeSetting
     * @param childReteSetting
     * @param withholdingRuleSettings
     * @param map
     * @throws Exception
     */
    private void assembleAgreedata(Map<String, Object> AgreeSetting, Map<String, Object> childReteSetting, List<WithholdingRuleSetting> withholdingRuleSettings, LinkedHashMap map) throws Exception {
        WithholdingRuleSetting withholdingRuleSetting = new WithholdingRuleSetting();
        withholdingRuleSetting.setId(Long.parseLong(childReteSetting.get("accountNumberId").toString()));
        List<AgreeIRSettingHistory> agreeIRSettingHistorys =  new ArrayList<>();
        Json jsondata = new Json(CtmJSONObject.toJSONString(AgreeSetting.get("agreeIRSettingGrade")));
        List<AgreeIRSettingGradeHistory> agreeIRSettingGradeHistorys = Objectlizer.decode(jsondata, AgreeIRSettingGradeHistory.ENTITY_NAME);
        //List<AgreeIRSettingGradeHistory> agreeIRSettingGradeHistorys = (List<AgreeIRSettingGradeHistory>) AgreeSetting.get("agreeIRSetting");
        AgreeIRSettingHistory agreeIRSettingHistory = new AgreeIRSettingHistory();
        agreeIRSettingHistory.setRateid(Long.parseLong(childReteSetting.get("id").toString()));
        agreeIRSettingHistory.setInterestDays(AgreeSetting.get("agreeinterestdays")==null ? null : AgreeSetting.get("agreeinterestdays").toString());
        agreeIRSettingHistory.setAgreeinterestmethod(AgreeSetting.get("agreeinterestmethod")==null ? null : Short.parseShort(AgreeSetting.get("agreeinterestmethod").toString()));
        agreeIRSettingHistory.setAgreerelymethod(AgreeSetting.get("agreerelymethod")==null ? null : Short.parseShort(AgreeSetting.get("agreerelymethod").toString()));
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        agreeIRSettingHistory.setStartDate(AgreeSetting.get("agreestartdate")==null ? null : sdf.parse(AgreeSetting.get("agreestartdate").toString()));
        agreeIRSettingHistory.setEndDate(AgreeSetting.get("agreeenddate")==null ? null :sdf.parse(AgreeSetting.get("agreeenddate").toString()));
        agreeIRSettingHistory.setOptionType(OptionType.Create.getValue());
        //agreeIRSettingHistory.setIsNew(true);
        agreeIRSettingHistory.setVersion(ymsOidGenerator.nextId());
        //agreeIRSettingHistory.setRemark(null);
        agreeIRSettingHistory.setCreateDate(new Date());
        agreeIRSettingHistory.setCreateTime(new Date());
        agreeIRSettingHistory.setCreator(AppContext.getCurrentUser().getName());
        agreeIRSettingHistory.setCreatorId(AppContext.getCurrentUser().getId());
        agreeIRSettingHistory.setAgreeIRSettingGradeHistory(agreeIRSettingGradeHistorys);
        //agreeIRSettingHistory.setMainid(Long.parseLong(childReteSetting.get("accountNumberId").toString()));
        agreeIRSettingHistory.setEntityStatus(EntityStatus.Insert);
        checkagreeData(agreeIRSettingHistory);
        agreeIRSettingHistorys.add(agreeIRSettingHistory);
        withholdingRuleSetting.setAgreeIRSettingHistory(agreeIRSettingHistorys);
        withholdingRuleSetting.setIssignagree(Short.valueOf("1"));//若新增协定存款利率，则设签约协定存款签约状态为1
        withholdingRuleSetting.setEntityStatus(EntityStatus.Update);
        withholdingRuleSettings.add(withholdingRuleSetting);
        //如果有结束日期为空的，则更为开始日期前一天
        setEndDate(agreeIRSettingHistory.getStartDate(), DateUtils.preDay(agreeIRSettingHistory.getStartDate()),Long.parseLong(childReteSetting.get("accountNumberId").toString()),AppContext.getCurrentUser().getYTenantId());
    }

    /**
     * 银行利率设置提交保存接口实现*
     *
     * @param agreeirsettingdetail
     * @return
     * @throws Exception
     */
    @Override
    public RuleExecuteResult agreeRateSettingSavedetail(CtmJSONObject agreeirsettingdetail) throws Exception {
        if (agreeirsettingdetail.get("agreeIRSettingHistory") == null){
            return new RuleExecuteResult();
        }
        long  ruleSettingId = Long.parseLong(agreeirsettingdetail.get("id").toString());
        Json agreeIRSettingHistorysjsondata = new Json(CtmJSONObject.toJSONString(agreeirsettingdetail.get("agreeIRSettingHistory")));
        List<AgreeIRSettingHistory> agreeIRSettingHistorys = Objectlizer.decode(agreeIRSettingHistorysjsondata, AgreeIRSettingHistory.ENTITY_NAME);
        for (AgreeIRSettingHistory agreeIRSettingHistory:agreeIRSettingHistorys) {
            agreeIRSettingHistory.setMainid(ruleSettingId);
            checkagreeData(agreeIRSettingHistory);
            Json agreeIRSettingGradeHistorysjsondata = new Json(CtmJSONObject.toJSONString(agreeIRSettingHistory.get("agreeIRSettingGradeHistory")));
            List<AgreeIRSettingGradeHistory> agreeIRSettingGradeHistorys = Objectlizer.decode(agreeIRSettingGradeHistorysjsondata, AgreeIRSettingGradeHistory.ENTITY_NAME);
            agreeIRSettingHistory.setCreateDate(new Date());
            agreeIRSettingHistory.setCreateTime(new Date());
            agreeIRSettingHistory.setCreator(AppContext.getCurrentUser().getName());
            agreeIRSettingHistory.setCreatorId(AppContext.getCurrentUser().getId());
            if (agreeIRSettingHistory.getEntityStatus() ==  EntityStatus.Update) {
                agreeIRSettingHistory.setOptionType(OptionType.Update.getValue());
                agreeIRSettingHistory.setModifyDate(new Date());
                agreeIRSettingHistory.setModifyTime(new Date());
                agreeIRSettingHistory.setModifier(AppContext.getCurrentUser().getName());
                agreeIRSettingHistory.setModifierId(AppContext.getCurrentUser().getId());
                if (agreeIRSettingHistory.getEndDate() == null){
                    agreeIRSettingHistory.setEndDate(null);
                }
            }
            else if (agreeIRSettingHistory.getEntityStatus() ==  EntityStatus.Insert) {
                agreeIRSettingHistory.setOptionType(OptionType.Create.getValue());
                setEndDate(agreeIRSettingHistory.getStartDate(), DateUtils.preDay(agreeIRSettingHistory.getStartDate()), ruleSettingId, AppContext.getCurrentUser().getYTenantId());
                //若新增协定存款利率，则设签约协定存款签约状态为1
                updateIssignagree(ruleSettingId);
            }
            agreeIRSettingHistory.setVersion(ymsOidGenerator.nextId());
            for (AgreeIRSettingGradeHistory agreeIRSettingGradeHistory:agreeIRSettingGradeHistorys) {
                agreeIRSettingGradeHistory.setCreateDate(new Date());
                agreeIRSettingGradeHistory.setCreateTime(new Date());
                agreeIRSettingGradeHistory.setCreator(AppContext.getCurrentUser().getName());
                agreeIRSettingGradeHistory.setCreatorId(AppContext.getCurrentUser().getId());
                if (agreeIRSettingGradeHistory.getEntityStatus() ==  EntityStatus.Update) {
                    agreeIRSettingGradeHistory.setModifyDate(new Date());
                    agreeIRSettingGradeHistory.setModifyTime(new Date());
                    agreeIRSettingGradeHistory.setModifier(AppContext.getCurrentUser().getName());
                    agreeIRSettingGradeHistory.setModifierId(AppContext.getCurrentUser().getId());
                }
            }
            agreeIRSettingHistory.setAgreeIRSettingGradeHistory(agreeIRSettingGradeHistorys);
        }
        agreeirsettingdetail.put("agreeIRSettingHistory",agreeIRSettingHistorys);
        BillDataDto bill = new BillDataDto();
        bill.setBillnum(CMP_AGREEIRSETTINGDETAIL);
        bill.setData(agreeirsettingdetail);
        // 通过save规则保存数据
        return ifiBillService.executeUpdate(OperationTypeEnum.SAVE.getValue(), bill);
    }

    /**
     * 银行利率设置删除接口实现*
     *
     * @param agreeirsettingdetail
     * @return
     * @throws Exception
     */
    @Override
    public RuleExecuteResult agreeRateSettingdelete(CtmJSONObject agreeirsettingdetail) throws Exception {
        BillDataDto bill = new BillDataDto();
        bill.setBillnum(CMP_AGREEIRSETTINGDETAIL);
        bill.setData(agreeirsettingdetail);
        // 通过save规则保存数据
        return ifiBillService.executeUpdate(OperationTypeEnum.SAVE.getValue(), bill);
    }

    /**
     * 更新账户利率变更的协定存款签约状态
     *
     * @param accountNumberId
     * @return
     * @throws Exception
     */
    public  void updateIssignagree(Long accountNumberId) throws Exception {
        QuerySchema querySchema = QuerySchema.create().addSelect("*");
        QueryConditionGroup queryConditionGroup = QueryConditionGroup.and(QueryCondition.name("accountNumberId").eq(accountNumberId));
        querySchema.addCondition(queryConditionGroup);
        List<InterestRateSetting> interestRates = MetaDaoHelper.queryObject(InterestRateSetting.ENTITY_NAME, querySchema,null);
        if(CollectionUtils.isEmpty(interestRates)){
            return;
        }
        InterestRateSetting interestRate = interestRates.get(0);
        interestRate.setEntityStatus(EntityStatus.Update);
        interestRate.setIssignagree(Short.valueOf("1"));
        MetaDaoHelper.update(InterestRateSetting.ENTITY_NAME, interestRate);

    }

    /**
     * 设置前一条协定存款利率的结束日期
     * @param endDate
     * @param mainid
     * @param ytenantId
     * @return
     * @throws Exception
     */
    public  void setEndDate(Date startDate,Date endDate,Long mainid,String ytenantId) throws Exception {
//        //设置结束日期为空的
//        Map<String, Object> params = new HashMap<>();
//        params.put("endDate", endDate);
//        params.put("mainid", mainid);
//        params.put("ytenantId", ytenantId);
//        SqlHelper.update("com.yonyoucloud.fi.cmp.voucher.updateagreeIRSettingEndDate", params);
        //设置上一条的结束日期
        QuerySchema querySchema = QuerySchema.create().addSelect("*");
        QueryConditionGroup queryConditionGroup = QueryConditionGroup.and(QueryCondition.name("mainid").eq(mainid));
        querySchema.addCondition(queryConditionGroup);
        querySchema.addOrderBy(new QueryOrderby("createTime", "desc"));
        List<AgreeIRSettingHistory>  agreeIRSettingHistorys = MetaDaoHelper.queryObject(AgreeIRSettingHistory.ENTITY_NAME, querySchema,null);
        if(CollectionUtils.isEmpty(agreeIRSettingHistorys)){
            return;
        }
        AgreeIRSettingHistory preHistoty = agreeIRSettingHistorys.get(0);
        //协定利率开始日期小于等于之前数据的结束日期时/之前数据的结束日期为空 需要之前的结束日期=新增开始日期-1
        boolean updateEndDate = false;
        Date clearStartDate = DateUtils.clearTime(startDate);
        if (preHistoty.getEndDate() == null){
            updateEndDate = true;
        } else {
            Date clearEndDate = DateUtils.clearTime(preHistoty.getEndDate());
            if(clearStartDate.compareTo(clearEndDate) <= 0){
                updateEndDate = true;
            }
        }
        if(updateEndDate){
            preHistoty.setEndDate(endDate);
            preHistoty.setEntityStatus(EntityStatus.Update);
            MetaDaoHelper.update(AgreeIRSettingHistory.ENTITY_NAME,preHistoty);
        }
    }

    public List<Map<String, Object>> checkEndDate(java.util.Date StartDate,Long id,Long mainid,int checktype) throws Exception {
        List<Map<String, Object>> checkagreeList = new ArrayList<>();
        QuerySchema querySchema = QuerySchema.create().addSelect("bankaccount.name as bankaccount_name,currency.name as currency_name");
        QueryConditionGroup queryConditionGroup = QueryConditionGroup.and(QueryCondition.name("id").eq(mainid));
        if (checktype == 1) {
            queryConditionGroup.addCondition(QueryCondition.name("lastInterestAccruedDate").egt(StartDate));
        } else
        if (checktype == 2) {
            queryConditionGroup.addCondition(QueryCondition.name("lastInterestSettlementDate").egt(StartDate));
        } else
        if (checktype == 3) {
            queryConditionGroup.addCondition(QueryCondition.name("agreeIRSettingHistory.id").not_eq(id));
            queryConditionGroup.addCondition(QueryConditionGroup.or(QueryCondition.name("agreeIRSettingHistory.startDate").egt(StartDate),
                    QueryConditionGroup.or(QueryCondition.name("agreeIRSettingHistory.endDate").egt(StartDate))));
        }
        if (checktype == 4) {//CM202400728：校验生效日必须大于上次协定存款的开始日期
            queryConditionGroup.addCondition(QueryCondition.name("agreeIRSettingHistory.id").not_eq(id));
            queryConditionGroup.addCondition(QueryCondition.name("agreeIRSettingHistory.startDate").egt(StartDate));
        }
        querySchema.addCondition(queryConditionGroup);
        checkagreeList = MetaDaoHelper.query(WithholdingRuleSetting.ENTITY_NAME, querySchema);
        return checkagreeList;
    }

    private void checkagreeData(AgreeIRSettingHistory agreeIRSettingHistory) throws Exception {
        //AssertUtils.isNull(agreeIRSettingHistory.agreeIRSettingGradeHistory(), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180548","请求数据不可为空！") /* "请求数据不可为空！" */);
        //计息天数
        if (ObjectUtils.isEmpty(agreeIRSettingHistory.getInterestDays())) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101601"),MessageUtils.getMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180589","计息天数不可为空！") /* "计息天数不可为空！" */));
        }
        //计息方式
//        BigDecimal depositChangeValue = new BigDecimal(interestRateSetting.get("depositChangeValue").toString());
        if (ObjectUtils.isEmpty(agreeIRSettingHistory.getAgreeinterestmethod())) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101797"),MessageUtils.getMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1CA67B0204780005","计息方式不可为空！") /* "计息方式不可为空！" */));
        }
        //靠档方式
        if (ObjectUtils.isEmpty(agreeIRSettingHistory.getAgreerelymethod())) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101798"),MessageUtils.getMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1CA67B3E04780000","靠档方式不可为空！") /* "靠档方式不可为空！" */));
        }
        //利率生效日期
        if (ObjectUtils.isEmpty(agreeIRSettingHistory.getStartDate())) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101786"),MessageUtils.getMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180544","利率生效日期不可为空！") /* "利率生效日期不可为空！" */));
        }
        List<Map<String, Object>> checkagreeList = new ArrayList<>();
        checkagreeList = checkEndDate(agreeIRSettingHistory.getStartDate(),agreeIRSettingHistory.getId(),agreeIRSettingHistory.getMainid(),1);
        //检查上次预提结束日
        if (checkagreeList.size()>0) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101799"),MessageFormat.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1CA67B9E04C00008","银行账户【{0}】、币种【{1}】，协定存款利率开始日期小于等于账户上次预提结束日，不允许调整！"),checkagreeList.get(0).get("bankaccount_name").toString(),checkagreeList.get(0).get("currency_name").toString() ));
        }
        checkagreeList = checkEndDate(agreeIRSettingHistory.getStartDate(),agreeIRSettingHistory.getId(),agreeIRSettingHistory.getMainid(),2);
        //检查上次结息结束日
        if (checkagreeList.size()>0) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101800"),MessageFormat.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1CA67C1604C00006","银行账户【{0}】、币种【{1}】，协定存款利率开始日期小于等于账户上次结息结束日，不允许调整！"),checkagreeList.get(0).get("bankaccount_name").toString(),checkagreeList.get(0).get("currency_name").toString() ));
        }
//        checkagreeList = checkEndDate(agreeIRSettingHistory.getStartDate(),agreeIRSettingHistory.getId(),agreeIRSettingHistory.getMainid(),3);
//        //检查结束日期合规
//        if (checkagreeList.size()>0) {
//            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101801"),MessageFormat.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1CA67C4604780007","银行账户【{0}】，协定存款利率开始日期介于已存在的协定存款利率记录中间，不允许调整！") ,checkagreeList.get(0).get("bankaccount_name").toString()));
//        }
        //CM202400728：检查开始日期合规：校验生效日必须大于等于上次协定存款的开始日期
        checkagreeList = checkEndDate(agreeIRSettingHistory.getStartDate(),agreeIRSettingHistory.getId(),agreeIRSettingHistory.getMainid(),4);
        if (checkagreeList.size()>0) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-105057"),
                    MessageFormat.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault(
                    "UID:P_CM-BE_22EF5D7604580004","银行账户【{0}】生效日必须大于上次协定存款的开始日期！") ,checkagreeList.get(0).get("bankaccount_name").toString()));
        }

    }

    @Override
    public void agreeRateSettingSavedetailList(CtmJSONObject bill) throws Exception {
        List<String> withholdingRuleSettingIds = null != bill.get("withholdingRuleSettingIds") ? (List<String>) bill.get("withholdingRuleSettingIds") : null;
        Date  startDate = null != bill.get("startDate") ? DateUtils.parseDate(bill.get("startDate").toString()) : null;
        List<Map<String, Object>> checkagreeList = new ArrayList<>();
        List<String> resultList = new ArrayList<>();
        if (null != withholdingRuleSettingIds && withholdingRuleSettingIds.size()>0){
            for (String id : withholdingRuleSettingIds) {
                QuerySchema querySchema = QuerySchema.create().addSelect("id");
                QueryConditionGroup queryConditionGroup = new QueryConditionGroup(ConditionOperator.and);
                queryConditionGroup.appendCondition(QueryCondition.name("mainid").in(id));
                querySchema.addCondition(queryConditionGroup);
                List<Map<String, Object>> agreeIRSettingHistorys = MetaDaoHelper.query(AgreeIRSettingHistory.ENTITY_NAME,querySchema);
                if (null != agreeIRSettingHistorys){
                   for (Map<String,Object> agreeIRSettingHistory : agreeIRSettingHistorys){
                       Long agreeId = null != agreeIRSettingHistory.get("id") ? Long.valueOf(agreeIRSettingHistory.get("id").toString()) : null;
                       checkagreeList = checkEndDate(startDate,agreeId,Long.valueOf(id),1);
                       if (checkagreeList.size()>0){
                           throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101799"),MessageFormat.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1CA67B9E04C00008","银行账户【{0}】、币种【{1}】，协定存款利率开始日期小于等于账户上次预提结束日，不允许调整！"),checkagreeList.get(0).get("bankaccount_name").toString(),checkagreeList.get(0).get("currency_name").toString() ));
                       }
                       checkagreeList = checkEndDate(startDate,agreeId,Long.valueOf(id),2);
                       //检查上次结息结束日
                       if (checkagreeList.size()>0) {
                           throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101800"),MessageFormat.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1CA67C1604C00006","银行账户【{0}】、币种【{1}】，协定存款利率开始日期小于等于账户上次结息结束日，不允许调整！"),checkagreeList.get(0).get("bankaccount_name").toString(),checkagreeList.get(0).get("currency_name").toString() ));
                       }
//                       checkagreeList = checkEndDate(startDate,agreeId,Long.valueOf(id),3);
//                       //检查结束日期合规
//                       if (checkagreeList.size()>0) {
//                           throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101801"),MessageFormat.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1CA67C4604780007","银行账户【{0}】，协定存款利率开始日期介于已存在的协定存款利率记录中间，不允许调整！") ,checkagreeList.get(0).get("bankaccount_name").toString()));
//                       }
                       //校验生效日必须大于上次协定存款的开始日期
                       checkagreeList = checkEndDate(startDate,agreeId,Long.valueOf(id),4);
                       if (checkagreeList.size()>0) {
                           throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-105057"),
                                   MessageFormat.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault(
                                           "UID:P_CM-BE_22EF5D7604580004","银行账户【{0}】生效日必须大于上次协定存款的开始日期！") ,checkagreeList.get(0).get("bankaccount_name").toString()));
                       }
                   }
                }
            }
        }
    }
}
