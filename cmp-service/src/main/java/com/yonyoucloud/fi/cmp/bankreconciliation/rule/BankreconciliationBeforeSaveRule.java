package com.yonyoucloud.fi.cmp.bankreconciliation.rule;

import com.yonyou.iuap.ucf.common.i18n.InternationalUtils;
import com.yonyou.iuap.ucf.common.i18n.MessageUtils;
import com.yonyou.ucf.basedoc.model.CurrencyTenantDTO;
import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVO;
import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVOWithRange;
import com.yonyou.ucf.basedoc.model.puborggroup.OrgRangeVO;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.bill.rule.util.GetRoundModeUtils;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.yonbip.ctm.remote.RemoteDubbo;
import com.yonyoucloud.ctm.stwb.reconcode.pubitf.ReconciliateCodeGenerator;
import com.yonyoucloud.fi.basecom.precision.CheckPrecision;
import com.yonyoucloud.fi.basecom.precision.CheckPrecisionVo;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.basecom.utils.FIDubboUtils;
import com.yonyoucloud.fi.cmp.accountrealtimebalance.AccountRealtimeBalance;
import com.yonyoucloud.fi.cmp.auth.OrgDataPermissionService;
import com.yonyoucloud.fi.cmp.autoparam.service.AutoConfigService;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankreconciliationService;
import com.yonyoucloud.fi.cmp.bankreconciliation.CommonSaveUtils;
import com.yonyoucloud.fi.cmp.bankreconciliation.enums.BankreconciliationFieldNameEnum;
import com.yonyoucloud.fi.cmp.bankreconciliationsetting.BankReconciliationSetting;
import com.yonyoucloud.fi.cmp.cmpentity.AssociationStatus;
import com.yonyoucloud.fi.cmp.cmpentity.DateOrigin;
import com.yonyoucloud.fi.cmp.cmpentity.EventType;
import com.yonyoucloud.fi.cmp.cmpentity.OppositeType;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.common.service.CmCommonService;
import com.yonyoucloud.fi.cmp.common.service.CtmCmpCheckRepeatDataService;
import com.yonyoucloud.fi.cmp.constant.IBussinessConstant;
import com.yonyoucloud.fi.cmp.constant.IDomainConstant;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import com.yonyoucloud.fi.cmp.enums.ConfirmStatusEnum;
import com.yonyoucloud.fi.cmp.enums.OrgConfirmBillEnum;
import com.yonyoucloud.fi.cmp.enums.SerialdealendState;
import com.yonyoucloud.fi.cmp.event.sendEvent.ICmpSendEventService;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.consts.BankDealDetailConst;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailaccess.BankDealDetailAccessFacade;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailhandler.DefaultCommonProcessService;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.ruleconst.RuleCodeConst;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.Direction;
import com.yonyoucloud.fi.cmp.smartclassify.BillSmartClassifyBO;
import com.yonyoucloud.fi.cmp.smartclassify.BillSmartClassifyService;
import com.yonyoucloud.fi.cmp.util.DateUtils;
import com.yonyoucloud.fi.cmp.util.MerchantUtils;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import com.yonyoucloud.fi.cmp.util.basedoc.EnterpriseBankQueryService;
import com.yonyoucloud.fi.cmp.util.business.CmpCommonUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 银行对账单保存前规则-校验
 * 1 银行对账单保存
 * 2 银行对账单导入
 * 3 银行对账单OpenApi保存
 * 4 账户交易明细拉取写入
 */
@Slf4j
public class BankreconciliationBeforeSaveRule extends AbstractCommonRule {

    private static final String BANKRECONCILIATIONWDLIST = "cmp_bankreconciliationwdlist";
    private static final String BANKRECONCILIATIONWD = "cmp_bankreconciliationwd";
    private static final String BANKRECONCILIATION = "cmp_bankreconciliation";
    @Autowired
    private BaseRefRpcService baseRefRpcService;

    @Autowired
    private CtmCmpCheckRepeatDataService ctmCmpCheckRepeatDataService;

    @Autowired
    private ICmpSendEventService cmpSendEventService;

    @Autowired
    private EnterpriseBankQueryService enterpriseBankQueryService;

    //单据智能分类service
    @Autowired
    private BillSmartClassifyService billSmartClassifyService;

    @Autowired
    private AutoConfigService autoConfigService;

    @Autowired
    private CmCommonService cmCommonService;

    @Autowired
    private OrgDataPermissionService orgDataPermissionService;

    @Resource
    private BankDealDetailAccessFacade bankDealDetailAccessFacade;

    @Autowired
    private BankreconciliationService bankreconciliationService;

    @Autowired
    private DefaultCommonProcessService defaultCommonProcessService;

    

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        List<BizObject> bills = getBills(billContext, map);
        if (CollectionUtils.isEmpty(bills)) {
            return new RuleExecuteResult();
        }
        BillDataDto billDataDto = (BillDataDto) getParam(map);
        // 导入
        boolean importFlag =  "import".equals(billDataDto.getRequestAction());

        for (BizObject bill : bills) {
            BankReconciliation bizObject = (BankReconciliation) bill;
            if (importFlag && ("ok".equals(bizObject.get("_convert_BankReconciliationbusrelation_b")) || bizObject.get("BankReconciliationbusrelation_b") != null)){
                throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054004F8", "导入银行流水时，银行对账单子表不为空，请检查导入的excel银行对账单子表，银行对账单子表必须为空时才能导入！") /* "导入银行流水时，银行对账单子表不为空，请检查导入的excel银行对账单子表，银行对账单子表必须为空时才能导入！" */);
            }
            if (importFlag && ("ok".equals(bizObject.get("_convert_details")) || bizObject.get("details") != null)){
                throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054004F9", "导入银行流水时，银行对账单分派信息不为空，请检查导入的excel银行对账单分派信息，银行对账单分派信息必须为空时才能导入！") /* "导入银行流水时，银行对账单分派信息不为空，请检查导入的excel银行对账单分派信息，银行对账单分派信息必须为空时才能导入！" */);
            }
            // OpenApi
            boolean openApiFlag = (bizObject.containsKey("_fromApi") && bizObject.get("_fromApi").equals(true)) || billDataDto.getFromApi();
            try {
                if (FIDubboUtils.isSingleOrg()) {
                    BizObject singleOrg = FIDubboUtils.getSingleOrg();
                    if (singleOrg != null) {
                        bizObject.set(IBussinessConstant.ACCENTITY, singleOrg.get("id"));
                        bizObject.set("accentity_name", singleOrg.get("name"));
                    }
                }
            } catch (Exception e) {
                log.error("单组织判断异常!", e);
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102321"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418030B","单组织判断异常！") /* "单组织判断异常！" */ + e.getMessage());
            }

            if (bizObject.get("currency") == null) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102322"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180310","币种不能为空！") /* "币种不能为空！" */);
            }
            if (bizObject.get("bankaccount") == null) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102323"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180311","银行账户不能为空！") /* "银行账户不能为空！" */);
            }
            if (bizObject.get("eliminateStatus") != null) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102324"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1A0D7E7C05B80008","银行对账单已进行剔除处理，不能保存！") /* "银行对账单已进行剔除处理，不能保存！" */);
            }

            if (!BANKRECONCILIATION.equals(billContext.getBillnum())) {
                if (ObjectUtils.isEmpty(bizObject.get("dzdate"))) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102325"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041802F2","交易日期不能为空！") /* "交易日期不能为空！" */);
                }
                if (ObjectUtils.isEmpty(bizObject.get("tran_date"))) {
                    bizObject.setTran_date(bizObject.getDzdate());
                }
            }
            // 银行账户
            EnterpriseBankAcctVO enterpriseBankAcctVO= baseRefRpcService.queryEnterpriseBankAccountById(bizObject.getBankaccount());
            if (enterpriseBankAcctVO == null) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102326"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041802F9","银行账号为:") /* "银行账号为:" */ + bizObject.get("bankaccount_account") + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041802F8","的账户未启用,请检查!") /* "的账户未启用,请检查!" */);
            }

            // 银行对账单导入时校验交易日期
            Date tran_date = bizObject.getTran_date();
            String dateString = DateUtils.formatDate(tran_date);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            Optional.ofNullable(tran_date).ifPresent(tranData -> {
                if (LocalDate.parse(dateString.trim(),formatter).isAfter(LocalDate.now())) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102327"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180309","交易日期不能大于当前服务器日期！") /* "交易日期不能大于当前服务器日期！" */);
                }
            });
            // 校验银行账户与币种是否一致
            CmpCommonUtil.checkBankAcctCurrency(bizObject.getBankaccount(), bizObject.getCurrency());

            //银行对账单期初未达，不进行默认赋值
            if ( !(BANKRECONCILIATIONWDLIST.equals(billContext.getBillnum()) || BANKRECONCILIATIONWD.equals(billContext.getBillnum()))) {
                bizObject.setOrgid(enterpriseBankAcctVO.getOrgid());
            }
            // 校验授权使用组织的银行账户
            checkBankAccountOfOrg(bizObject);

            // 精度处理
            CurrencyTenantDTO currencyDTO = baseRefRpcService.queryCurrencyById(bizObject.getCurrency());
            Integer moneydigit = currencyDTO.getMoneydigit();
            RoundingMode moneyRound = GetRoundModeUtils.getCurrencyPriceRoundMode(bizObject.getCurrency(), 1);
            CheckPrecisionVo checkPrecisionVo = new CheckPrecisionVo();
            checkPrecisionVo.setPrecisionId(bizObject.getCurrency());

            // 交易金额、支出金额、收入金额均按币种精度处理
            if (bizObject.getTran_amt() != null) {
                bizObject.setTran_amt(bizObject.getTran_amt().setScale(moneydigit, moneyRound));
            }
            if (bizObject.getDebitamount() != null) {
                bizObject.setDebitamount(bizObject.getDebitamount().setScale(moneydigit, moneyRound));
            }
            if (bizObject.getCreditamount() != null) {
                bizObject.setCreditamount(bizObject.getCreditamount().setScale(moneydigit, moneyRound));
            }

            // 导入校验
            // 1、金额、支出金额、收入金额 三列有且只有一列有值
            if (importFlag || openApiFlag) {
                if ((bizObject.getTran_amt() != null && (bizObject.getDebitamount() != null || bizObject.getCreditamount() != null))
                        || (bizObject.getDebitamount() != null && (bizObject.getTran_amt() != null || bizObject.getCreditamount() != null))
                        || (bizObject.getCreditamount() != null && (bizObject.getDebitamount() != null || bizObject.getTran_amt() != null))) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102328"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180304","金额、支出金额和收入金额只能有一列有值") /* "金额、支出金额和收入金额只能有一列有值" */);
                }
                // 借贷方向为借，收入金额有值
                if (bizObject.getDc_flag() != null && (bizObject.getDc_flag().equals(Direction.Debit) && bizObject.getCreditamount() != null
                        || bizObject.getDc_flag().equals(Direction.Credit) && bizObject.getDebitamount() != null)) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102329"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180308","借贷方向和借贷金额不相符") /* "借贷方向和借贷金额不相符" */);
                }

            }
            if (BANKRECONCILIATION.equals(billContext.getBillnum()) && null == bizObject.getTran_amt()) {
                // 导入时判断 金额、支出金额、收入金额 三列有且只有一列有值
                if (importFlag || openApiFlag) {
                    // 支出金额有值
                    if (bizObject.getDebitamount() != null && bizObject.getCreditamount() == null) {
                        bizObject.setDc_flag(Direction.Debit);
                        bizObject.setTran_amt(bizObject.getDebitamount());
                    } else if (bizObject.getDebitamount() == null && bizObject.getCreditamount() != null) {
                        //收入金额有值
                        bizObject.setDc_flag(Direction.Credit);
                        bizObject.setTran_amt(bizObject.getCreditamount());
                    } else if (bizObject.getDebitamount() == null && bizObject.getCreditamount() == null) {
                        // 金额，借贷金额至少有一个值
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102330"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00244", "借贷金额均为空时,收付方向和交易金额不能为空") /* "借贷金额均为空时,收付方向和交易金额不能为空" */);
                    }
                    //借贷金额同时有，后续有判断，不做重复处理
                } else { //非导入，金额不能为空
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102331"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180313","交易金额不能为空") /* "交易金额不能为空" */);
                }
            }

            Map<String, BigDecimal> numericalMap = new HashMap<>();
            // 余额
            if (bizObject.getAcct_bal() != null) {
                numericalMap.put("acct_bal", bizObject.getAcct_bal());
                bizObject.setAcct_bal(bizObject.getAcct_bal().setScale(moneydigit, moneyRound));
            }
            // 交易时间、交易日期
            if (null != bizObject.getTran_time() && null != bizObject.getTran_date()) {
                LocalDate tranTime = LocalDate.parse(DateUtils.formatDate(bizObject.getTran_time()),formatter);
                LocalDate tranDate = LocalDate.parse(DateUtils.formatDate(bizObject.getTran_date()),formatter);
                if (!tranTime.equals(tranDate)) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102332"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1A0D7E7C05B80007","交易日期与交易时间不属于同一天，请检查！") /* "交易日期与交易时间不属于同一天，请检查！" */);
                }
            }
            // 借贷金额和交易金额校验
            if (bizObject.getDebitamount() == null && bizObject.getCreditamount() == null && !BANKRECONCILIATIONWDLIST.equals(billContext.getBillnum())) {
                // 通过交易金额和收付方向志来判断借贷金额
                if (bizObject.getDc_flag() == null || bizObject.getTran_amt() == null) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102330"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00244", "借贷金额均为空时,收付方向和交易金额不能为空") /* "借贷金额均为空时,收付方向和交易金额不能为空" */);
                }

                numericalMap.put("tran_amt", bizObject.getTran_amt());
                if (bizObject.getDc_flag().equals(Direction.Debit)) {
                    bizObject.setDebitamount(bizObject.getTran_amt());
                } else if (bizObject.getDc_flag().equals(Direction.Credit)) {
                    bizObject.setCreditamount(bizObject.getTran_amt());
                }
            }
            if (bizObject.getDebitamount() != null && bizObject.getCreditamount() != null) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102333"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1B54D3960430001E","收入支出金额只能同时一个有值，请检查！") /* "收入支出金额只能同时一个有值，请检查！" */);
            }

            if (bizObject.getDebitamount() == null && bizObject.getCreditamount() == null) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102334"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041802FE","借贷金额不能同时为空！") /* "借贷金额不能同时为空！" */);
            }
            // 通过借贷金额来判断
            if (bizObject.getDebitamount() != null) {
                if (BANKRECONCILIATION.equals(billContext.getBillnum())) {
                    if (bizObject.getDebitamount().compareTo(bizObject.getTran_amt()) != 0) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102335"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180306","金额与支出金额不相等！") /* "金额与支出金额不相等！" */);
                    }
                }
                numericalMap.put("debitamount", bizObject.getDebitamount());
                bizObject.setDc_flag(Direction.Debit);
            } else if (bizObject.getCreditamount() != null) {
                if (BANKRECONCILIATION.equals(billContext.getBillnum())) {
                    if (bizObject.getCreditamount().compareTo(bizObject.getTran_amt()) != 0) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102336"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("P_YS_FI_CM_1466225332922089505","金额与收入金额不相等！") /* "金额与收入金额不相等！" */);
                    }
                }
                numericalMap.put("creditamount", bizObject.getCreditamount());
                bizObject.setDc_flag(Direction.Credit);
            }


            // 新增时生成财资统一码
            if (EntityStatus.Insert.equals(bizObject.get("_status"))) {
                if (bizObject.get("isparsesmartcheckno") == null) {
                    bizObject.setIsparsesmartcheckno(false);
                }
                if (bizObject.get("smartcheckno") == null) {
                    // 未解析出财资统一码，生成财资统一码并进行设置
                    bizObject.setSmartcheckno(RemoteDubbo.get(ReconciliateCodeGenerator.class, IDomainConstant.MDD_DOMAIN_STWB).generate());
                }
            }

            checkPrecisionVo.setNumericalMap(numericalMap);
            checkPrecisionVo.setEntityName(BankReconciliation.ENTITY_NAME);
            CheckPrecision.checkMoneyByCurrency(checkPrecisionVo);
            // 导入
            if (importFlag || openApiFlag) {
                bizObject.setDataOrigin(DateOrigin.Created);
            } else {
                // 手工新增
                if (null == bizObject.getDataOrigin()) {
                    bizObject.setDataOrigin(DateOrigin.AddManually);
                }
            }
            Date dzdate = bizObject.getDzdate();
            // 期初未达项
            if (BANKRECONCILIATIONWDLIST.equals(billContext.getBillnum()) || BANKRECONCILIATIONWD.equals(billContext.getBillnum())) {
                //CZFW-371319 去掉日结校验
//                JedisLockUtils.isexistRjLock(bizObject.get(IBussinessConstant.ACCENTITY));
                // 导入
                if (importFlag || openApiFlag) {
                    if (bizObject.get("bankreconciliationscheme") == null) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102337"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041802F6","对账方案不能为空！") /* "对账方案不能为空！" */);
                    }
                    //导入进来的数据
                    checkImportDate(bizObject, map);
                    //CZFW-394284 导入数据需要给对账组织赋值;
                    BankReconciliationSetting bankReconciliationSetting = MetaDaoHelper.findById(BankReconciliationSetting.ENTITY_NAME,bizObject.get("bankreconciliationscheme"));
                    if (bankReconciliationSetting != null){
                        bizObject.setOrgid(bankReconciliationSetting.getAccentity());
                        bizObject.setOther_checkflag(false);
                        bizObject.setCheckflag(false);
                    }
                } else {
                    // 非导入
                    if (bizObject.get("bankreconciliationscheme") == null) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102337"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041802F6","对账方案不能为空！") /* "对账方案不能为空！" */);
                    }
                }
                // 单据已勾兑不允许修改
                if (EntityStatus.Update.equals(bizObject.get("_status"))) {
                    BankReconciliation oldBizObject = this.queryById(bizObject.getId());
                    if ((oldBizObject.getCheckflag() != null && oldBizObject.getCheckflag()) || (oldBizObject.getOther_checkflag() != null && oldBizObject.getOther_checkflag())) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102338"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041802FF","该单据已勾兑完成，不允许修改") /* "该单据已勾兑完成，不允许修改" */);
                    }
                }
                // 期初标识
                bizObject.setInitflag(true);
                // 校验对账日期
                checkDzDate(bizObject);
            }

            // 银行对账单更新
            if (EntityStatus.Update.equals(bizObject.get("_status"))) {
                BankReconciliation bankReconciliation = this.queryById(bizObject.getId());
                if (AssociationStatus.Associated.getValue()== bankReconciliation.getAssociationstatus()){
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-105053"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_22855CD405D00013", "该单据已关联，不允许修改！") /* "该单据已关联，不允许修改！" */);
                }
                if (SerialdealendState.END.getValue() == bankReconciliation.getSerialdealendstate()){
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-105054"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_22855CD405D00014", "该单据已完结状态，不允许修改！") /* "该单据已完结状态，不允许修改！" */);
                }
                if (bankReconciliation.getCheckflag() != null && bankReconciliation.getCheckflag()){
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-105055"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_22855CD405D00012", "该单据已勾兑，不允许修改！") /* "该单据已勾兑，不允许修改！" */);
                }
                if (bankReconciliation.getOther_checkflag() != null && bankReconciliation.getOther_checkflag()){
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-105056"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_22855CD405D00012", "该单据已勾兑，不允许修改！") /* "该单据已勾兑，不允许修改！" */);
                }

                if (bankReconciliation == null) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102339"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00245", "单据【[%s]】已删除，请刷新后重试") /* "单据【[%s]】已删除，请刷新后重试" */, bizObject.get("bank_seq_no").toString()));
                }
                Date currentPubts = bankReconciliation.getPubts();
                if (currentPubts != null && bizObject.get("pubts") != null) {
                    if (currentPubts.compareTo(bizObject.get("pubts")) != 0) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102340"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180300","当前单据不是最新状态，请刷新单据重新操作。") /* "当前单据不是最新状态，请刷新单据重新操作。" */);
                    }
                }
                // 修改保存操作
                // 银行对账单期初数据不参与验重
                if (!(BANKRECONCILIATIONWDLIST.equals(billContext.getBillnum()) || BANKRECONCILIATIONWD.equals(billContext.getBillnum()))) {
                    String concat_info = ctmCmpCheckRepeatDataService.formatConctaInfoBankReconciliation(bizObject);
                    // 前端不会传送unique_no，通过流水查询
                    QuerySchema querySchema = QuerySchema.create().addSelect("id,unique_no,concat_info");
                    querySchema.appendQueryCondition(QueryCondition.name("concat_info").eq(concat_info));
                    List<BizObject> bankReconciliations = MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME, querySchema, null);
                    if(!bankReconciliations.isEmpty()){
                        // CZFW-444987  【DSP支持问题】银行流水处理中批量修改时提示：数据重复，修改失败，麻烦老师看下是啥问题？
                        // 银行流水号为null, 8要素重复，但是存在全局唯一码，增加只要有unique_no 的银行流水，则不进行验重
                        // 初始化检查为false
                        for (BizObject bizObjectTmp : bankReconciliations) {
                            if (StringUtils.isEmpty(bizObjectTmp.get("unique_no"))) {
                                // 根据八要素拼接查询concat_info
                                if(!bizObjectTmp.get("id").equals(bizObject.getId())){
                                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102341"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1B14FEF605F0006B","修改后数据重复，保存失败！"));
                                }
                            }
                        }
                    } else {
                        bizObject.setConcat_info(concat_info);
                    }
                    // 银行对账单修改对方单位，对方类型，对方账号将对方账号Id清空
                    if (MerchantUtils.checkOppositeIsChanged(bizObject, bankReconciliation)) {
                        bizObject.setTo_acct(null);
                    }
                }
                //流水更新走智能流水
//                if(DealDetailUtils.isOpenIntelligentDealDetail()){
//                    if(TransactionSynchronizationManager.isActualTransactionActive()){
//                        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
//                            @Override
//                            public void afterCommit() {
//                                try{
//                                    BankReconciliation after = queryById(bizObject.getId());
//                                    after.setEntityStatus(EntityStatus.Update);
//                                    Long start = System.currentTimeMillis();
//                                    bankDealDetailAccessFacade.dealDetailAccessByImportNew(after);
//                                    log.error("【更新&导入-智能流水】耗时,{}ms",(System.currentTimeMillis()-start));
//                                }catch (Exception e){
//                                    log.error("【流水更新前置规则执行异常】",e);
//                                }
//                            }
//                        });
//                    }
//                }
                // 校验对账状态
                checkCheckFlagStatus(bankReconciliation);
                // 校验绝对不可以修改的银行对账单字段
                checkMustNotModifyFields(billDataDto);
                // 校验银企直连账户是否允许维护，判断银行账户是否为银企直连
                checkBankAccountModifyFields(bizObject, bankReconciliation, enterpriseBankAcctVO);
            } else {
                // 新增
                // 银行对账单期初数据不参与验重
                if (!(BANKRECONCILIATIONWDLIST.equals(billContext.getBillnum()) || BANKRECONCILIATIONWD.equals(billContext.getBillnum()))) {
                    // 新增保存 走验重操作
//                    if(DealDetailUtils.isOpenIntelligentDealDetail()){
//                        if(TransactionSynchronizationManager.isActualTransactionActive()){
//                            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
//                                @Override
//                                public void afterCommit() {
//                                    try{
//                                        BankReconciliation after = queryById(bizObject.getId());
//                                        after.setProcessstatus(DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_MATCH_NO_START.getStatus());
//                                        after.setEntityStatus(EntityStatus.Update);
//                                        Long start = System.currentTimeMillis();
//                                        bankDealDetailAccessFacade.dealDetailAccessByImportNew(after);
//                                        log.error("【保存&导入-智能流水】耗时,{}ms",(System.currentTimeMillis()-start));
//                                    }catch (Exception e){
//                                        log.error("【流水更新前置规则执行异常】",e);
//                                    }
//                                }
//                            });
//                        }
//                    }else {
//                        List<BizObject> insertVos = ctmCmpCheckRepeatDataService.checkRepeatData(bills, EventType.CashMark.getValue());
//                        if (insertVos.isEmpty()) {
//                            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102342"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1B14FEF605F0006A", "重复数据，新增/导入失败！"));
//                        }
//                    }
                    List<BizObject> insertVos = ctmCmpCheckRepeatDataService.checkRepeatData(bills, EventType.CashMark.getValue());
                    if (insertVos.isEmpty()) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102342"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1B14FEF605F0006A", "重复数据，新增/导入失败！"));
                    }
                }
            }
            // 银行对账单期初数据不参与疑重
            if (!(BANKRECONCILIATIONWDLIST.equals(billContext.getBillnum()) || BANKRECONCILIATIONWD.equals(billContext.getBillnum()))) {
                //开启疑重后的逻辑
                ctmCmpCheckRepeatDataService.checkRepeatInfo(bizObject);
            }else{
                //fix CZFW-393592 期初未达项-对账单期初未达，新增期初对账单数据，不走疑重逻辑，但是疑重标识应该赋值为‘正常’，而不是空
                bizObject.setIsRepeat((short) BankDealDetailConst.REPEAT_INIT);
            }
            // CZFW-101050, bug修复，银行对账单期初未达交易日期修改
            if (BANKRECONCILIATIONWDLIST.equals(billContext.getBillnum())) {
                bizObject.setTran_date(bizObject.getDzdate());
            } else {
                bizObject.setDzdate(bizObject.getTran_date());
            }

                //对方账号后去空格
                if (!StringUtils.isEmpty(bizObject.getTo_acct_no())) {
                    bizObject.setTo_acct_no(bizObject.getTo_acct_no().replaceAll(" ", ""));
                }

                // 智能辨识(新增时执行智能辨识，修改保存时不执行)
                String smartClassify = AppContext.getEnvConfig("cmp.smartClassify", "1");
                if ("1".equals(smartClassify) && !EntityStatus.Update.equals(bizObject.get("_status"))) {
                    //smartClassify(bizObject);
                    smartClassify(bizObject);
                    List<BankReconciliation> bankReconciliationList = new ArrayList<>();
                    bankReconciliationList.add(bizObject);
                    //一条银行流水-初始状态；
                    //调度器：控制执行规则-----
                    //1-对辨识规则进行分类；1，2,3。。。。===》大类内可以并行
                    //2-判断流水状态；选择执行大类-规则
                    //3-分配大类中的规则
                    //4-规则并发设定是否并行DISTRICT

                    //线程池：异步执行
                    //走智能流水的对方信息辨识
                    //defaultCommonProcessService.intelligentFlowIdentification(bankReconciliationList,Arrays.asList(RuleCodeConst.SYSTEM004));

                }
                // 银行对账单保存、修改、导入时根据银行账户、交易日期查历史余额，如果已确认则不能增改
                String bankaccount = bizObject.getBankaccount();
                bizObject.setBanktype(enterpriseBankAcctVO.getBank());
                if (!StringUtils.isEmpty(bankaccount) && tran_date != null) {
                    String dateStr = new SimpleDateFormat("yyyy-MM-dd").format(tran_date);
                    QuerySchema querySchema = QuerySchema.create().addSelect(QuerySchema.PARTITION_ALL);
                    querySchema.appendQueryCondition(QueryCondition.name("enterpriseBankAccount").eq(bankaccount));
                    querySchema.appendQueryCondition(QueryCondition.name("currency").eq(bizObject.getCurrency()));
                    querySchema.appendQueryCondition(QueryCondition.name("balancedate").between(dateStr, null));
                    querySchema.appendQueryCondition(QueryCondition.name("isconfirm").eq(true));
                    querySchema.addOrderBy("balancedate");
                    List<Map<String, Object>> historyBalance = MetaDaoHelper.query(AccountRealtimeBalance.ENTITY_NAME, querySchema);
                    if (CollectionUtils.isNotEmpty(historyBalance)) {
                        if (importFlag || openApiFlag) {
                            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101978"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181E1DC00440000C", "导入失败：银行账户[%s]交易日期[%s]的历史余额已经进行确认，当前对账单的交易日期等于或早于该日期，不能导入!") /* "导入失败：银行账户[%s]交易日期[%s]的历史余额已经进行确认，当前对账单的交易日期等于或早于该日期，不能导入!" */, enterpriseBankAcctVO.getAcctName(), historyBalance.get(historyBalance.size() - 1).get("balancedate")));
                        } else {
                            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101979"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181E1DC00440000D", "保存失败：银行账户[%s]交易日期[%s]的历史余额已经进行确认，当前对账单的交易日期等于或早于该日期，不能保存！") /* "保存失败：银行账户[%s]交易日期[%s]的历史余额已经进行确认，当前对账单的交易日期等于或早于该日期，不能保存！" */, enterpriseBankAcctVO.getAcctName(), historyBalance.get(historyBalance.size() - 1).get("balancedate")));
                        }
                    }
                }

                // 校验银企直连账户是否允许维护，判断银行账户是否为银企直连
                boolean isBankreconciliationCanUpdate = autoConfigService.getBankreconciliationCanUpdate();
                //20250807 流水期初未达跳过该参数校验-王东方确认
                if (!(BANKRECONCILIATIONWDLIST.equals(billContext.getBillnum()) || BANKRECONCILIATIONWD.equals(billContext.getBillnum())) && !isBankreconciliationCanUpdate && !StringUtils.isEmpty(bankaccount) && EntityStatus.Insert.equals(bizObject.get("_status"))) {
                    if (cmCommonService.getOpenFlag(bankaccount)) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102343"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1A0D7E7C05B80009", "银行账号[%s]已开通银企直联，不允许手工维护，请检查！") /* "银行账号[%s]已开通银企直联，不允许手工维护，请检查！" */, enterpriseBankAcctVO.getAccount()));
                    }
                }

                if (bizObject.getAssociationstatus() == null) {
                    bizObject.setAssociationstatus(AssociationStatus.NoAssociated.getValue());
                }

                String oppositeobjectId = bizObject.get("oppositeobjectid");
                if (null != oppositeobjectId) {
                    //将oppositeobjectid赋值给对应的客户、供应商或用户
                    CommonSaveUtils.setOppositeobjectidToBizField(bizObject, bizObject.get("oppositetype"), oppositeobjectId);
                }
                // 银行流水支持发送事件消息
                List<BankReconciliation> bankReconciliationList = new ArrayList();
                bankReconciliationList.add(bizObject);
                cmpSendEventService.sendEventByBankClaimBatch(bankReconciliationList, EntityStatus.Insert.name());
            }
        return new RuleExecuteResult();
    }

    /**
     * 校验勾兑状态
     * @param bankReconciliation
     */
    private void checkCheckFlagStatus(BankReconciliation bankReconciliation) {
        if (bankReconciliation.getCheckflag() || bankReconciliation.getOther_checkflag()) {
            throw new CtmException(String.format(InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1EC27F0A04280004",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_203ED7420478002B", "批量编辑失败：银行交易流水号：%s已进行对账，不允许编辑，请检查！") /* "批量编辑失败：银行交易流水号：%s已进行对账，不允许编辑，请检查！" */), bankReconciliation.getBank_seq_no()));
        }
    }


    /**
     * 校验一定不能修改的字段
     * @param billDataDto 入参
     * @throws Exception
     */
    private void checkMustNotModifyFields(BillDataDto billDataDto) throws Exception {
        if (billDataDto.getExternalData() == null) {
            log.error("checkMustNotModifyFields billDataDto is null!");
            return;
        }
        List<String> filterFields = bankreconciliationService.queryFilterFields();
        if (CollectionUtils.isEmpty(filterFields)) {
            log.error("未配置任何不允许批改的字段");
            return;
        }
        Map<String, Object> billDataMap;
        if (billDataDto.getExternalData() instanceof LinkedHashMap) {
            billDataMap = (LinkedHashMap<String, Object>) billDataDto.getExternalData();
        } else if (billDataDto.getExternalData() instanceof HashMap) {
            billDataMap = (HashMap<String, Object>) billDataDto.getExternalData();
            if (billDataMap == null || billDataMap.size() == 0) {
                log.error("checkMustNotModifyFields billDataDto.getExternalData() HashMap为空!");
                return;
            }
        } else {
            log.error("checkMustNotModifyFields billDataDto.getExternalData() 类型错误!");
            return;
        }
        for (String field : filterFields) {
            if (billDataMap.get(field) != null) {
                throw new CtmException(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_203ED7420478002C", "%s不允许修改！") /* "%s不允许修改！" */, field));
            }
        }
    }

    /**
     * 校验银企联账户某些字段是否允许维护
     * @param bizObject
     * @param bankReconciliation
     * @throws Exception
     */
    private void checkBankAccountModifyFields(BankReconciliation bizObject, BankReconciliation bankReconciliation, EnterpriseBankAcctVO enterpriseBankAcctVO) throws Exception {
        String bankaccount = bizObject.get("bankaccount");
        boolean isBankreconciliationCanUpdate = autoConfigService.getBankreconciliationCanUpdate();
        if (!isBankreconciliationCanUpdate && !StringUtils.isEmpty(bankaccount)) {
            // 银行账号开通银企直连并且来源为银企联
            if (cmCommonService.getOpenFlag(bankaccount) && bankReconciliation.getDataOrigin() == DateOrigin.DownFromYQL) {
                StringBuilder sb = new StringBuilder(String.format(InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1EC259D004280005",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_203ED74204780027", "银行账号[%s]已开通银企直联，不允许手工维护银企联传入的") /* "银行账号[%s]已开通银企直联，不允许手工维护银企联传入的" */), enterpriseBankAcctVO.getAccount()));
                sb.append("[");
                boolean flag = false;
                // 金额
                BigDecimal tranAmt = bizObject.get("tran_amt") != null ? (BigDecimal) bizObject.get("tran_amt") : BigDecimal.ZERO;
                if (tranAmt.compareTo(bankReconciliation.getTran_amt()) != 0) {
                    sb.append(BankreconciliationFieldNameEnum.TRAN_AMT.getName());
                    sb.append("、");//@notranslate
                    flag = true;
                }
                // 余额
                BigDecimal balance = bizObject.get("acct_bal") != null ? (BigDecimal) bizObject.get("acct_bal") : BigDecimal.ZERO;
                if (balance.compareTo(bankReconciliation.getAcct_bal() == null ? BigDecimal.ZERO : bankReconciliation.getAcct_bal()) != 0) {
                    sb.append(BankreconciliationFieldNameEnum.ACCT_BAL.getName());
                    sb.append("、");//@notranslate
                    flag = true;
                }
                // 收支方向
                if (bankReconciliation.getDc_flag().getValue() != bizObject.getDc_flag().getValue()) {
                    sb.append(BankreconciliationFieldNameEnum.DC_FLAG.getName());
                    sb.append("、");//@notranslate
                    flag = true;
                }
                // 交易日期
                if (!bankReconciliation.getTran_date().equals(bizObject.get("tran_date"))) {
                    sb.append(BankreconciliationFieldNameEnum.TRAN_DATE.getName());
                    sb.append("、");//@notranslate
                    flag = true;
                }
                // 交易流水号
                if (!Objects.equals(bankReconciliation.getBank_seq_no(), bizObject.get("bank_seq_no"))) {
                    sb.append(BankreconciliationFieldNameEnum.BANK_SEQ_NO.getName());
                    sb.append("、");//@notranslate
                    flag = true;
                }
                // 对方账号
                if (!Objects.equals(bankReconciliation.getTo_acct_no(),bizObject.get("to_acct_no"))) {
                    sb.append(BankreconciliationFieldNameEnum.TO_ACCT_NO.getName());
                    sb.append("、");//@notranslate
                    flag = true;
                }
                // 对方户名
                if (!Objects.equals(bankReconciliation.getTo_acct_name(),bizObject.get("to_acct_name"))) {
                    sb.append(BankreconciliationFieldNameEnum.TO_ACCT_NAME.getName());
                    sb.append("、");//@notranslate
                    flag = true;
                }
                // 对方开户行
                if (!Objects.equals(bankReconciliation.getTo_acct_bank(), bizObject.get("to_acct_bank"))) {
                    sb.append(BankreconciliationFieldNameEnum.TO_ACCT_BANK.getName());
                    sb.append("、");//@notranslate
                    flag = true;
                }
                // 对方开户行名
                if (!Objects.equals(bankReconciliation.getTo_acct_bank_name(), bizObject.get("to_acct_bank_name"))) {
                    sb.append(BankreconciliationFieldNameEnum.TO_ACCT_BANK_NAME.getName());
                    sb.append("、");//@notranslate
                    flag = true;
                }
                // 用途
                if (!Objects.equals(bankReconciliation.getUse_name(), bizObject.get("use_name"))) {
                    sb.append(BankreconciliationFieldNameEnum.USE_NAME.getName());
                    sb.append("、");//@notranslate
                    flag = true;
                }
                // 摘要
                if (!Objects.equals(bankReconciliation.getRemark(), bizObject.get("remark"))) {
                    sb.append(BankreconciliationFieldNameEnum.REMARK.getName());
                    sb.append("、");//@notranslate
                    flag = true;
                }
                if (flag) {
                    sb.deleteCharAt(sb.length() - 1);
                    sb.append("]");
                    sb.append(InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1EC26D4404280001",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_203ED7420478002A", "字段值，请检查！") /* "字段值，请检查！" */));
                    throw new CtmException(sb.toString());
                }
            }
        }
    }

    /**
     * 银行对账单对方单位辨识
     * @param bizObject 银行对账单
     * @throws Exception
     */
    private void smartClassify(BankReconciliation bizObject) throws Exception {
        checkDcFlag(bizObject);
        if (bizObject.getTo_acct_no() == null && bizObject.getTo_acct_name() == null) {
            handleOtherType(bizObject);
            return;
        }

        BillSmartClassifyBO classifyBO = billSmartClassifyService.smartClassify(
                bizObject.getAccentity(), bizObject.getTo_acct_no(), bizObject.getTo_acct_name(), bizObject.getCurrency(), bizObject.getDc_flag().getValue());

        // 未匹配则标记为其他类型
        if (classifyBO == null) {
            handleOtherType(bizObject);
            return;
        }

        // 对方类型
        bizObject.setOppositetype(classifyBO.getOppositetype());
        // 对方单位id
        bizObject.setOppositeobjectid(classifyBO.getOppositeobjectid());
        // 对方单位名称
        bizObject.setOppositeobjectname(classifyBO.getOppositeobjectname());
        // 对方账号id
        bizObject.setTo_acct(classifyBO.getOppositebankacctid() == null ? null : classifyBO.getOppositebankacctid());
    }

    /**
     * 处理对方类型为其他的数据
     * @param bizObject
     */
    private void handleOtherType(BankReconciliation bizObject) {
        bizObject.setOppositetype(OppositeType.Other.getValue());
        bizObject.setOppositeobjectid(null);
        bizObject.setOppositeobjectname(null);
    }

    /**
     * 校验收付方向
     * @param bizObject
     */
    private void checkDcFlag(BankReconciliation bizObject) {
        if (bizObject.getDc_flag() == null) {
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_203ED74204780028", "收付方向必填") /* "收付方向必填" */);
        }
        if (Direction.find(bizObject.getDc_flag().getValue()) == null) {
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_203ED74204780029", "收付方向的值非法") /* "收付方向的值非法" */);
        }

    }

    /**
     * 导入数据根据筛选条件过滤
     *
     * @param journal
     * @param map
     */
    private void checkImportDate(BankReconciliation journal, Map<String, Object> map) throws Exception {
        BillDataDto billDataDto = (BillDataDto) map.get("param");
        Map<String, Object> map1 = billDataDto.getMapCondition();
        if (ValueUtils.isNotEmpty(map1)) {
            String accentity = (String) map1.get(IBussinessConstant.ACCENTITY);
            String bankaccount = (String) map1.get("bankaccount");
            String currency = (String) map1.get("currency");
            String bankreconciliationscheme = map1.get("bankreconciliationscheme").toString();
            if (!bankreconciliationscheme.equals(journal.get("bankreconciliationscheme").toString())) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102344"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180301","导入对账方案与当前对账方案不匹配") /* "导入对账方案与当前对账方案不匹配" */);
            }
            if (!accentity.equals(journal.getAccentity())) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102345"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180303","导入的会计主体与当前会计主体不一致!") /* "导入的会计主体与当前会计主体不一致!" */);
            }
            if (!bankaccount.equals(journal.getBankaccount())) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102346"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180305","导入的银行账户与当前银行账户不一致!") /* "导入的银行账户与当前银行账户不一致!" */);
            }
            if (!currency.equals(journal.getCurrency())) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102347"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180307","导入的币种与当前币种不一致!") /* "导入的币种与当前币种不一致!" */);
            }
        }
    }

    /**
     * 数据重复校验
     * 修改校验规则：去掉会计主体，添加摘要字段，交易流水号和前面条件做成或，先判断交易流水，有重复的不判断其他了，交易流水号无重复，再判断其他
     *
     * @param bankReconciliation
     * @throws Exception
     */
    private void checkIsunique(BankReconciliation bankReconciliation) throws Exception {
        QuerySchema querySchema = QuerySchema.create();
        querySchema.addSelect("count(1) as  count");
        QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name(IBussinessConstant.ACCENTITY).eq(bankReconciliation.getAccentity()),
                QueryCondition.name("bankaccount").eq(bankReconciliation.getBankaccount()),
                QueryCondition.name("dzdate").eq(bankReconciliation.getDzdate()),
                QueryCondition.name("currency").eq(bankReconciliation.getCurrency()));
        if (null != bankReconciliation.getBank_seq_no()) {
            group.addCondition(QueryCondition.name("bank_seq_no").eq(bankReconciliation.getBank_seq_no()));
            //交易流水号有重复的，那么数据重复
            QueryConditionGroup groupOr = QueryConditionGroup.and(QueryCondition.name(IBussinessConstant.ACCENTITY).eq(bankReconciliation.getAccentity()),
                    QueryCondition.name("bankaccount").eq(bankReconciliation.getBankaccount()),
                    QueryCondition.name("dzdate").eq(bankReconciliation.getDzdate()),
                    QueryCondition.name("currency").eq(bankReconciliation.getCurrency()),
                    QueryCondition.name("bank_seq_no").eq(bankReconciliation.getBank_seq_no()));

            if (bankReconciliation.getDebitamount() != null && bankReconciliation.getDebitamount().compareTo(BigDecimal.ZERO) > 0) {
                groupOr.addCondition(QueryCondition.name("debitamount").eq(bankReconciliation.getDebitamount()));
            } else if (bankReconciliation.getCreditamount() != null && bankReconciliation.getCreditamount().compareTo(BigDecimal.ZERO) > 0) {
                groupOr.addCondition(QueryCondition.name("creditamount").eq(bankReconciliation.getCreditamount()));
            }
            querySchema.addCondition(groupOr);
            Map<String, Object> map = MetaDaoHelper.queryOne(BankReconciliation.ENTITY_NAME, querySchema);
            Long count = (Long) map.get("count");
            if (count > 0) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102348"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041802F7","导入数据重复!") /* "导入数据重复!" */);
            }
        }
        if (!StringUtils.isEmpty(bankReconciliation.getRemark())) {
            group.addCondition(QueryCondition.name("remark").eq(bankReconciliation.getRemark()));
        }
        if (bankReconciliation.getDebitamount() != null && bankReconciliation.getDebitamount().compareTo(BigDecimal.ZERO) > 0) {
            group.addCondition(QueryCondition.name("debitamount").eq(bankReconciliation.getDebitamount()));
        } else if (bankReconciliation.getCreditamount() != null && bankReconciliation.getCreditamount().compareTo(BigDecimal.ZERO) > 0) {
            group.addCondition(QueryCondition.name("creditamount").eq(bankReconciliation.getCreditamount()));
        }
        if (null != bankReconciliation.getTran_time()) {
            group.addCondition(QueryCondition.name("tran_time").eq(bankReconciliation.getTran_time()));
        }
        querySchema.addCondition(group);
        Map<String, Object> map = MetaDaoHelper.queryOne(BankReconciliation.ENTITY_NAME, querySchema);
        Long count = (Long) map.get("count");
        if (count > 0) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102348"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041802F7","导入数据重复!") /* "导入数据重复!" */);
        }

    }

    /**
     * 校验对账单日期
     *
     * @param bizObject
     * @throws Exception
     */
    private void checkDzDate(BizObject bizObject) throws Exception {
        String bankreconciliationschemename = bizObject.get("bankreconciliationschemename");
        // 日记账日期
        Date dzDate = bizObject.get("dzdate");
        BankReconciliationSetting bankReconciliationSetting = MetaDaoHelper.findById(BankReconciliationSetting.ENTITY_NAME, bizObject.get("bankreconciliationscheme"));
        if (bankReconciliationSetting == null) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102349"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418030C","对账方案：%s 不存在，请检查后再保存。") /* "对账方案：%s 不存在，请检查后再保存。" */, bankreconciliationschemename));
        }

        Date enableDate = bankReconciliationSetting.getEnableDate();
        if (DateUtils.dateCompare(dzDate, enableDate) >= 0) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102350"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418030F","交易日期不能晚于方案启用日期") /* "交易日期不能晚于方案启用日期" */);
        }

    }

    /**
     * 校验银行交易流水号
     *
     * @param bizObject
     */
    private void checkUpdateIsunique(BizObject bizObject) throws Exception {
        QuerySchema querySchema = QuerySchema.create();
        querySchema.addSelect("count(1) as  count");
        QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name(IBussinessConstant.ACCENTITY).eq(bizObject.get(IBussinessConstant.ACCENTITY)),
                QueryCondition.name("bankaccount").eq(bizObject.get("bankaccount")),
                QueryCondition.name("dzdate").eq(bizObject.get("dzdate")),
                QueryCondition.name("currency").eq(bizObject.get("currency")),
                QueryCondition.name("bank_seq_no").eq(bizObject.get("bank_seq_no")),
                QueryCondition.name("id").not_eq(bizObject.get("id")));
        BigDecimal debitamount = bizObject.get("debitamount");
        BigDecimal creditamount = bizObject.get("creditamount");
        if (debitamount != null && debitamount.compareTo(BigDecimal.ZERO) > 0) {
            group.addCondition(QueryCondition.name("debitamount").eq(debitamount));
        } else if (creditamount != null && creditamount.compareTo(BigDecimal.ZERO) > 0) {
            group.addCondition(QueryCondition.name("creditamount").eq(creditamount));
        }
        querySchema.addCondition(group);
        Map<String, Object> map = MetaDaoHelper.queryOne(BankReconciliation.ENTITY_NAME, querySchema);
        Long count = (Long) map.get("count");
        if (count > 0) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102351"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_19E5D5AE0418000F","修改后单据与已有单据重复，请检查后再保存") /* "修改后单据与已有单据重复，请检查后再保存" */);
        }
    }

    /**
     * 检查组织的银行账户
     * 授权使用组织&所属组织&确认状态赋值逻辑
     * 所属组织 = 账户的所属组织
     * 1，授权使用组织有值
     *      （1）授权使用组织在银行账户适用范围内，导入成功，授权使用组织 = 录入的授权使用组织，确认状态 = 已确认
     *      （2）授权使用组织不在银行账户适用范围内，导入失败，给提示信息
     * 2，授权使用组织无值
     *      （1）银行账户适用范围只有一条数据，导入成功，授权使用组织 = 适用范围内的组织，确认状态 = 已确认
     *      （2）银行账户使用范围有多条数据，导入成功，授权使用组织 = null，确认状态 = 未确认
     * @param bizObject 银行对账业务对象
     * @throws Exception 异常
     */
    public void checkBankAccountOfOrg(BankReconciliation bizObject) throws Exception {
        // 获取授权使用组织
        String accentity = bizObject.get(IBussinessConstant.ACCENTITY);
        // 获取授权使用组织编码
        String accentityCode = bizObject.getString(IBussinessConstant.ACCENTITY_CODE);
        // 获取银行账户
        String bankaccount = bizObject.get(IBussinessConstant.BANK_ACCOUNT);

        // 查询企业银行账户信息的授权使用组织范围
        EnterpriseBankAcctVOWithRange enterpriseBankAcctVoWithRange = enterpriseBankQueryService.queryEnterpriseBankAcctVOWithRangeById(bankaccount);
        List<OrgRangeVO> orgRangeVOS = enterpriseBankAcctVoWithRange != null ? enterpriseBankAcctVoWithRange.getAccountApplyRange() : Collections.emptyList();
        // 获取范围内的组织ID列表
        List<String> rangeOrgIds = orgRangeVOS.stream().map(OrgRangeVO::getRangeOrgId).collect(Collectors.toList());
        // 如果授权使用组织不为空且不在范围内的组织ID列表中，则抛出异常
        checkOrgAuth(bizObject, accentity, rangeOrgIds, accentityCode);
        // 如果有一个组织则赋上默认值
        if (rangeOrgIds.size() == 1) {
            accentity = rangeOrgIds.get(0);
        }
        if (StringUtils.isEmpty(accentity)) {
            // 设置为待确认
            bizObject.setConfirmstatus(ConfirmStatusEnum.Confirming.getIndex());
            bizObject.setConfirmbill(null);
        } else {
            // 设置为已确认
            bizObject.set(IBussinessConstant.ACCENTITY, accentity);
            bizObject.setConfirmstatus(ConfirmStatusEnum.Confirmed.getIndex());
            bizObject.setConfirmbill(OrgConfirmBillEnum.CMP_BANKRECONCILIATION.getIndex());
        }

    }

    /**
     * 银行账户与授权使用组织权限关系校验
     * @param bizObject 银行对账单实体
     * @param accentity 授权使用组织
     * @param rangeOrgIds 银行账户使用组织集合
     */
    private void checkOrgAuth(BankReconciliation bizObject, String accentity, List<String> rangeOrgIds, String accentityCode) throws Exception {
        // 如果是openApi则跳过校验
        if (bizObject.get("fromApi") != null && bizObject.get("fromApi").equals(true)) {
            return;
        }

        if (StringUtils.isNotEmpty(accentity)) {
            if (!rangeOrgIds.contains(accentity)) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102352"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1B54D39604300020", "银行账户和授权使用组织的关系不匹配，请检查后再保存") /* "银行账户和授权使用组织的关系不匹配，请检查后再保存" */);
            }
            Set<String> orgOwn = orgDataPermissionService.queryAuthorizedOrgByServiceCode(IServicecodeConstant.CMPBANKRECONCILIATION);
            if (orgOwn == null || orgOwn.isEmpty()) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102353"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1B54D39604300021", "请为此用户分配主组织权限！") /* "请为此用户分配主组织权限！" */);
            }

            if (!orgOwn.contains(accentity)) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102354"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A050800E6", "当前用户无组织【%s】的权限，请检查！") /* "当前用户无组织【%s】的权限，请检查！" */, accentityCode));
            }
        }
    }

    private BankReconciliation queryById(Long id) throws Exception {
        QueryConditionGroup repeatGroup = new QueryConditionGroup(ConditionOperator.or);
        repeatGroup.addCondition(QueryCondition.name("isrepeat").is_null());
        repeatGroup.addCondition(QueryCondition.name("isrepeat").is_not_null());

        QueryConditionGroup group = new QueryConditionGroup();
        group.addCondition(QueryCondition.name("id").eq(id));
        group.addCondition(repeatGroup);

        QuerySchema querySchema = QuerySchema.create().addSelect(QuerySchema.PARTITION_ALL).addCondition(group);
        List<BankReconciliation> list = MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME,querySchema,null);
        if(CollectionUtils.isEmpty(list)){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101609"),MessageUtils.getMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180585","单据不存在或已被删除!") /* "单据不存在或已被删除!" */));
        }
        return list.get(0);
    }

}
