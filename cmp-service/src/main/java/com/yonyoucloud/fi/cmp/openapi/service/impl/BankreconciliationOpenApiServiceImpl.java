package com.yonyoucloud.fi.cmp.openapi.service.impl;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.PageUtil;
import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.iuap.ucf.common.i18n.MessageUtils;
import com.yonyou.iuap.yms.id.generator.YmsOidGenerator;
import com.yonyou.iuap.yms.lock.YmsLock;
import com.yonyou.uap.billcode.BillCodeComponentParam;
import com.yonyou.uap.billcode.BillCodeObj;
import com.yonyou.uap.billcode.service.IBillCodeComponentService;
import com.yonyou.ucf.basedoc.model.CurrencyTenantDTO;
import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVO;
import com.yonyou.ucf.basedoc.model.rpcparams.CurrencyBdParams;
import com.yonyou.ucf.basedoc.model.rpcparams.EnterpriseParams;
import com.yonyou.ucf.mdd.common.enums.OperationTypeEnum;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.api.service.ApiImportCommandService;
import com.yonyou.ucf.mdd.ext.bill.biz.BillBiz;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.CommonRuleUtils;
import com.yonyou.ucf.mdd.ext.bpm.model.VerifyState;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.json.JsonUtils;
import com.yonyou.yonbip.ctm.orgs.FundsOrgQueryServiceComponent;
import com.yonyoucloud.fi.basecom.itf.IFIBillService;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.cmp.apicorr.APICorrService;
import com.yonyoucloud.fi.cmp.autoparam.service.AutoConfigService;
import com.yonyoucloud.fi.cmp.bankelectronicreceipt.BankElectronicReceipt;
import com.yonyoucloud.fi.cmp.bankreconciliation.*;
import com.yonyoucloud.fi.cmp.billclaim.BillClaim;
import com.yonyoucloud.fi.cmp.billclaim.BillClaimCharacterDef;
import com.yonyoucloud.fi.cmp.billclaim.BillClaimItem;
import com.yonyoucloud.fi.cmp.cmpentity.*;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.common.service.CtmCmpCheckRepeatDataService;
import com.yonyoucloud.fi.cmp.constant.CmpRuleEngineTypeConstant;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.enums.BusinessModel;
import com.yonyoucloud.fi.cmp.enums.ConfirmStatusEnum;
import com.yonyoucloud.fi.cmp.enums.OrgConfirmBillEnum;
import com.yonyoucloud.fi.cmp.event.utils.DetermineUtils;
import com.yonyoucloud.fi.cmp.https.service.HttpsService;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.consts.BankDealDetailConst;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.Direction;
import com.yonyoucloud.fi.cmp.openapi.service.BankreconciliationOpenApiService;
import com.yonyoucloud.fi.cmp.smartclassify.BillSmartClassifyBO;
import com.yonyoucloud.fi.cmp.smartclassify.BillSmartClassifyService;
import com.yonyoucloud.fi.cmp.util.*;
import com.yonyoucloud.fi.cmp.util.bankreconciliation.checkandfilter.BankreconciliationUtils;
import com.yonyoucloud.fi.cmp.vo.Result;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.imeta.biz.base.Objectlizer;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QueryOrderby;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author qihaoc
 * @Description: 银行对账单单据对外OpenAPI接口
 * @date 2022/10/5 14:24
 */
@Service
@Slf4j
@Transactional(rollbackFor = RuntimeException.class)
public class BankreconciliationOpenApiServiceImpl implements BankreconciliationOpenApiService {

    private final int NO_DATA_ERROR = 8;
    private final String DATA_TYPE_DOUBLE = "Double";
    private final String DATA_TYPE_FLOAT = "Float";
    private final String DATA_TYPE_BIGDECIMAL = "BigDecimal";
    private final String DATA_TYPE_INTEGER = "Integer";
    private final String DATA_TYPE_LONG = "Long";
    private final String DATA_TYPE_SHORT = "Short";
    private static final String FLAG = "flag";
    private static final String SUCCESS = "success";
    private static final String FAIL = "fail";

    @Autowired
    YmsOidGenerator ymsOidGenerator;

    @Autowired
    IBankrecRuleEngineService ruleEngineService;

    @Autowired
    private BaseRefRpcService baseRefRpcService;

    @Autowired
    private IFIBillService fiBillService;

    @Autowired
    private APICorrService apiCorrService;

    @Autowired
    private HttpsService httpsService;

    @Autowired
    private IBillCodeComponentService billCodeComponentService;

    @Autowired
    private FundsOrgQueryServiceComponent fundsOrgQueryServiceComponent;



    //单据智能分类service
    @Autowired
    private BillSmartClassifyService billSmartClassifyService;
    @Autowired
    private CtmCmpCheckRepeatDataService cmpCheckRepeatDataService;

    @Autowired
    AutoConfigService autoConfigService;

    @Override
    public Result<Object> insert(CtmJSONObject param) throws Exception {
        YmsLock ymsLock = null;
        try {
            log.error("新增接口入参 param:" + CtmJSONObject.toJSONString(param));
            Short isAutoPublish = 0;
            //是否自动发布
            if(!StringUtils.isEmpty(param.getString("isautoublish"))){
                isAutoPublish = Short.valueOf(param.getString("isautoublish"));
            }

            /** 获取字段属性 */
            String bankaccount = param.getString("bankaccount");
            String statementno = param.getString("statementno");
            String bank_seq_no = param.getString("bank_seq_no");
            String thirdserialno = param.getString("thirdserialno");
            String tranDate = param.getString("tran_date");
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
            Date tran_date = null;
            try {
                tran_date = simpleDateFormat.parse(tranDate);
            } catch (Exception e) {
                simpleDateFormat = new SimpleDateFormat("yyyyMMdd");
                try{
                    tran_date = simpleDateFormat.parse(tranDate);
                } catch (Exception ex) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101349"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180841", "对账单日期转化异常！") /* "对账单日期转化异常！" */);
                }
            }
            Date tran_time = null;
            SimpleDateFormat simpleDateFormat4time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            if (param.get("tran_time") != null && !StringUtils.isEmpty(param.getString("tran_time")) && param.getDate("tran_time") != null) {
                try {
                    tran_time = simpleDateFormat4time.parse(param.getString("tran_time"));
                } catch (Exception e) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101622"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00225", "对账单交易时间转化异常！") /* "对账单交易时间转化异常！" */);
                }
            }
            Short dc_flag = param.getShort("dc_flag");
            BigDecimal tran_amt = param.getBigDecimal("tran_amt");
            BigDecimal acct_bal = param.getBigDecimal("acct_bal");
            String to_acct_no = param.getString("to_acct_no");
            String to_acct_name = param.getString("to_acct_name");
            String to_acct_bank = param.getString("to_acct_bank");
            String to_acct_bank_name = param.getString("to_acct_bank_name");
            String use_name = param.getString("use_name");
            String remark = param.getString("remark");
            String bankcheckno = param.getString("bankcheckno");
            //代表账户使用组织，非必填字段
            String accentity = param.getString("accentity");
            String currency = param.getString("currency");
            /** 新加的字段 */
            Short billprocessflag = param.getShort("billprocessflag");
            String enteraccounttype = param.getString("enteraccounttype");
            String enteraccountcode = param.getString("enteraccountcode");
            String enteraccountname = param.getString("enteraccountname");
            String note = param.getString("note");


            BankReconciliation bankReconciliation = new BankReconciliation();
            String detailReceiptRelationCode = param.getString("detailReceiptRelationCode");
            bankReconciliation.setDetailReceiptRelationCode(detailReceiptRelationCode);
            String uniqueNo = param.getString("unique_no");
            if (StringUtils.isNotEmpty(uniqueNo)) {
                bankReconciliation.setUnique_no(uniqueNo);
            }
            ///** 判空操作 */
            //if (accentity == null) {
            //    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101350"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180847", "会计主体编码不能为空！") /* "会计主体编码不能为空！" */);
            //}
            if (bankaccount == null) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101351"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180848", "银行账户不能为空！") /* "银行账户不能为空！" */);
            }
//        if (bank_seq_no == null) {
//            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101352"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180849", "银行交易流水号不能为空！") /* "银行交易流水号不能为空！" */);
//        }
            if (thirdserialno == null) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101623"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418084B", "第三方流水号不能为空！") /* "第三方流水号不能为空！" */);
            }
            if (tran_date == null) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101353"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418084C", "交易日期不能为空！") /* "交易日期不能为空！" */);
            }
            if (dc_flag == null) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101354"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418084D", "借贷标不能为空！") /* "借贷标不能为空！" */);
            }
            if (tran_amt == null) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101624"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418084E", "交易金额不能为空！") /* "交易金额不能为空！" */);
            }
            //公有云 余额不校验
//        if (acct_bal == null) {
//            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101625"),MessageUtils.getMessage("P_YS_FI_CM_0000026272") /* "余额不能为空！" */);
//        }
            if (currency == null) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101355"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418083E", "币种不能为空！") /* "币种不能为空！" */);
            }

            /** 字段赋对应固定值 */
            bankReconciliation.setInitflag(false);//是否期初：否
            bankReconciliation.setLibraryflag(false);//是否来自事项库：否
            bankReconciliation.setCheckflag(false);//是否勾对：否
            bankReconciliation.setDataOrigin(DateOrigin.Created);//事项来源：外部导入
            bankReconciliation.setOther_checkflag(false);//其他模块是否已勾对：否
            bankReconciliation.setAutobill(false);//是否已自动生单：否
            bankReconciliation.setBillclaimstatus((short) 0);//认领状态：否
            bankReconciliation.setAutoassociation(false);//自动关联标志
            //mark by lichaor 20240408 和马良沟通过了，openApi进来的给什么参数就设置什么状态，这个例外处理，不去掉
            bankReconciliation.setAssociationstatus((short) 0);//业务关联状态：未关联
            bankReconciliation.setSerialdealtype(null);
            bankReconciliation.setIsautocreatebill(false);//是否自动生单：否
            bankReconciliation.setIschoosebill(false);//是否选择生单单据：否
            /** 新加的字段 */
            if (billprocessflag == null) {
                bankReconciliation.setBillprocessflag(BillProcessFlag.NeedDeal.getValue());
            } else if (billprocessflag.equals((short) 0) || billprocessflag.equals((short) 1) || billprocessflag.equals((short) 2)) {
                bankReconciliation.setBillprocessflag(billprocessflag);
            } else {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101626"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00228", "回单处理标识错误！") /* "回单处理标识错误！" */);
            }

            /** 币种名称转成ID */
            CurrencyBdParams currencyBdParams = new CurrencyBdParams();
            currencyBdParams.setCode(currency);
            List<CurrencyTenantDTO> currencylist = baseRefRpcService.queryCurrencyByParams(currencyBdParams);
            if (currencylist == null || currencylist.size() == 0) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101627"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00220", "币种编码不存在！") /* "币种编码不存在！" */);
            }
            for (CurrencyTenantDTO currencyTenantDTO : currencylist) {
                if (currency.equals(currencyTenantDTO.getCode())) {
                    bankReconciliation.setCurrency(currencyTenantDTO.getId());//设置币种ID
                    break;
                }
            }
            if (bankReconciliation.getCurrency() == null) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101627"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00220", "币种编码不存在！") /* "币种编码不存在！" */);
            }


            /** 银行账户名称转成ID */
            EnterpriseParams enterpriseParams = new EnterpriseParams();
//        enterpriseParams.setName(bankaccount);
            enterpriseParams.setAccount(bankaccount);
            List<EnterpriseBankAcctVO> enterpriselist = baseRefRpcService.queryEnterpriseBankAccountByCondition(enterpriseParams);
            if (enterpriselist == null || enterpriselist.size() == 0) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101629"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B0022B", "当前银行账户不存在！") /* "当前银行账户不存在！" */);
            }
            if (enterpriselist.size() > 1) {
                throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054004BA", "依据银行账号匹配到多个满足条件的银行账户档案，保存失败，请检查！") /* "依据银行账号匹配到多个满足条件的银行账户档案，保存失败，请检查！" */);
            }
            //for (EnterpriseBankAcctVO enterpriseBankAcctVO : enterpriselist) {
            //    if (bankaccount.equals(enterpriseBankAcctVO.getAccount())) {
            EnterpriseBankAcctVO enterpriseBankAcctVO = enterpriselist.get(0);
            bankReconciliation.setBankaccount(enterpriseBankAcctVO.getId());//设置银行账户ID
            // 所属组织
            bankReconciliation.setOrgid(enterpriseBankAcctVO.getOrgid());
            // 确认组织节点 银行对账单
            bankReconciliation.setConfirmbill(OrgConfirmBillEnum.CMP_BANKRECONCILIATION.getIndex());
            // 确认状态
            bankReconciliation.setConfirmstatus(ConfirmStatusEnum.Confirmed.getIndex());
            //break;
            //    }
            //}
            //if (bankReconciliation.getBankaccount() == null) {
            //    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101630"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00222", "银行账户名称不存在或银行账户与会计主体不匹配！") /* "银行账户名称不存在或银行账户与会计主体不匹配！" */);
            //}

            String accentityId = null;
            if (!StringUtils.isEmpty(accentity)) {
                accentityId = QueryBaseDocUtils.getAccentityIdByCode(accentity);
            }
            BankreconciliationUtils.getAndSetAuthoruseaccentityRelationCol(accentityId, bankReconciliation.getBankaccount(), bankReconciliation);


            /** 其他必填信息 */
            bankReconciliation.setBank_seq_no(bank_seq_no);//银行交易流水号
            bankReconciliation.setThirdserialno(thirdserialno);//第三方流水号
            bankReconciliation.setDzdate(tran_date);
            bankReconciliation.setTran_date(tran_date);//交易日期
            bankReconciliation.setTran_time(tran_time);//交易时间
            if (dc_flag == (short) 1) {
                bankReconciliation.setDc_flag(Direction.Debit);//借
                bankReconciliation.setDebitamount(tran_amt);//借方金额等于交易金额
            } else if (dc_flag == (short) 2) {
                bankReconciliation.setDc_flag(Direction.Credit);//贷
                bankReconciliation.setCreditamount(tran_amt);//贷方金额等于交易金额
            } else {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101631"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00229", "借贷标错误，只能为(short)1或(short)2！") /* "借贷标错误，只能为(short)1或(short)2！" */);
            }
            //bankReconciliation.setTran_amt(tran_amt);//交易金额
            bankReconciliation.setAcct_bal(acct_bal);//余额

            /** 额外非必填信息 */
            if (statementno != null) {
                bankReconciliation.setStatementno(statementno);//对账单行号
            }
            if (to_acct_no != null) {
                bankReconciliation.setTo_acct_no(to_acct_no);//对方账号
            }
            if (to_acct_name != null) {
                bankReconciliation.setTo_acct_name(to_acct_name);//对方户名
            }
            if (to_acct_bank != null) {
                bankReconciliation.setTo_acct_bank(to_acct_bank);//对方开户行
            }
            if (to_acct_bank_name != null) {
                bankReconciliation.setTo_acct_bank_name(to_acct_bank_name);//对方开户行名
            }
            if (use_name != null) {
                bankReconciliation.setUse_name(use_name);//用途
            }
            if (remark != null) {
                bankReconciliation.setRemark(remark);//摘要
            }
            if (bankcheckno != null) {
                bankReconciliation.setBankcheckno(bankcheckno);//银行对账编号
            }
            /** 新加的字段 */
            if (enteraccounttype != null) {
                bankReconciliation.setEnteraccounttype(enteraccounttype);//入账方类型
            }
            if (enteraccountcode != null) {
                bankReconciliation.setEnteraccountcode(enteraccountcode);//入账方编码
            }
            if (enteraccountname != null) {
                bankReconciliation.setEnteraccountname(enteraccountname);//入账方名称
            }
            if (note != null) {
                bankReconciliation.setNote(note);//备注
            }

            // 按照平台保存前后规则进行保存
            bankReconciliation.setEntityStatus(EntityStatus.Insert);
            // 从openApi导入
            bankReconciliation.set("_fromApi", true);
            bankReconciliation.set("fromApi", true);
            //特征
            if (param.getObject("characterDef", LinkedHashMap.class) != null) {
                BizObject bizObject = Objectlizer.convert(param.getObject("characterDef", LinkedHashMap.class), CmpBankReconciliationCharacterDef.ENTITY_NAME);
                bizObject.put("id", String.valueOf(IdCreator.getInstance().nextId()));
                //设置特征状态
                bizObject.setEntityStatus(EntityStatus.Insert);
                bankReconciliation.put("characterDef", bizObject);
            }

            Set<Map.Entry<String, Object>> paramEntries = param.entrySet();
            CtmJSONObject attrMap = param.getJSONObject("extAttrMap");
            for (Map.Entry<String, Object> paramEntry : paramEntries) {
                if (paramEntry.getKey().startsWith("extend")) {
                    if (attrMap != null && attrMap.get(paramEntry.getKey()) != null) {
                        if (DATA_TYPE_DOUBLE.equals(attrMap.getString(paramEntry.getKey()))) {
                            bankReconciliation.put(paramEntry.getKey(), param.getDouble(paramEntry.getKey()));
                        } else if (DATA_TYPE_FLOAT.equals(attrMap.getString(paramEntry.getKey()))) {
                            bankReconciliation.put(paramEntry.getKey(), param.getFloat(paramEntry.getKey()));
                        } else if (DATA_TYPE_BIGDECIMAL.equals(attrMap.getString(paramEntry.getKey()))) {
                            bankReconciliation.put(paramEntry.getKey(), param.getBigDecimal(paramEntry.getKey()));
                        } else if (DATA_TYPE_INTEGER.equals(attrMap.getInteger(paramEntry.getKey()))) {
                            bankReconciliation.put(paramEntry.getKey(), param.getInteger(paramEntry.getKey()));
                        } else if (DATA_TYPE_LONG.equals(attrMap.getString(paramEntry.getKey()))) {
                            bankReconciliation.put(paramEntry.getKey(), param.getLong(paramEntry.getKey()));
                        } else if (DATA_TYPE_SHORT.equals(attrMap.getString(paramEntry.getKey()))) {
                            bankReconciliation.put(paramEntry.getKey(), param.getShort(paramEntry.getKey()));
                        } else {
                            if (null != paramEntry.getValue() && paramEntry.getValue() instanceof Double) {
                                bankReconciliation.put(paramEntry.getKey(), param.getDouble(paramEntry.getKey()));
                            } else if (null != paramEntry.getValue() && paramEntry.getValue() instanceof Integer) {
                                bankReconciliation.put(paramEntry.getKey(), param.getInteger(paramEntry.getKey()));
                            } else if (null != paramEntry.getValue() && paramEntry.getValue() instanceof Long) {
                                bankReconciliation.put(paramEntry.getKey(), param.getLong(paramEntry.getKey()));
                            } else if (null != paramEntry.getValue() && paramEntry.getValue() instanceof Float) {
                                bankReconciliation.put(paramEntry.getKey(), param.getFloat(paramEntry.getKey()));
                            } else if (null != paramEntry.getValue() && paramEntry.getValue() instanceof Short) {
                                bankReconciliation.put(paramEntry.getKey(), param.getShort(paramEntry.getKey()));
                            } else if (null != paramEntry.getValue() && paramEntry.getValue() instanceof Date) {
                                bankReconciliation.put(paramEntry.getKey(), param.getDate(paramEntry.getKey()));
                            } else {
                                bankReconciliation.put(paramEntry.getKey(), paramEntry.getValue());
                            }
                        }
                    } else {
                        if (null != paramEntry.getValue() && paramEntry.getValue() instanceof Double) {
                            bankReconciliation.put(paramEntry.getKey(), param.getDouble(paramEntry.getKey()));
                        } else if (null != paramEntry.getValue() && paramEntry.getValue() instanceof Integer) {
                            bankReconciliation.put(paramEntry.getKey(), param.getInteger(paramEntry.getKey()));
                        } else if (null != paramEntry.getValue() && paramEntry.getValue() instanceof Long) {
                            bankReconciliation.put(paramEntry.getKey(), param.getLong(paramEntry.getKey()));
                        } else if (null != paramEntry.getValue() && paramEntry.getValue() instanceof Float) {
                            bankReconciliation.put(paramEntry.getKey(), param.getFloat(paramEntry.getKey()));
                        } else if (null != paramEntry.getValue() && paramEntry.getValue() instanceof Short) {
                            bankReconciliation.put(paramEntry.getKey(), param.getShort(paramEntry.getKey()));
                        } else if (null != paramEntry.getValue() && paramEntry.getValue() instanceof Date) {
                            bankReconciliation.put(paramEntry.getKey(), param.getDate(paramEntry.getKey()));
                        } else {
                            bankReconciliation.put(paramEntry.getKey(), paramEntry.getValue());
                        }
                    }
                }
            }
            String key = "" + bankReconciliation.getAccentity() + bankReconciliation.getBankaccount() +
                    simpleDateFormat.format(tran_date) + tran_amt + dc_flag + bankReconciliation.getCurrency() + bankReconciliation.getBank_seq_no();
            if ((ymsLock=JedisLockUtils.lockBillWithOutTrace(key))==null) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101632"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00223", "该数据正在处理，请稍后重试！") /* "该数据正在处理，请稍后重试！" */);
            }
            //shi否自动发布
            if(isAutoPublish != null && isAutoPublish == 1){
                bankReconciliation.setIspublish(true);
                bankReconciliation.setPublish_time(new Date());
                //待认领金额
                bankReconciliation.setAmounttobeclaimed(tran_amt);
                bankReconciliation.setClaimamount(BigDecimal.ZERO);
                // 入账类型
                bankReconciliation.setEntrytype(EntryType.Normal_Entry.getValue());
            }
            log.error("新增数据：", CtmJSONObject.toJSONString(bankReconciliation));
            BillDataDto billDataDto = new BillDataDto(IBillNumConstant.BANKRECONCILIATION);
            Map<String, Object> partParam = new HashMap<String, Object>();
            //需回单中台处理，则添加外部标识，以用来执行规则引擎的规则
            if (bankReconciliation.getBillprocessflag() == BillProcessFlag.NeedDeal.getValue()) {
                partParam.put("outsystem", "1");
            }
            billDataDto.setPartParam(partParam);
            AccentityUtil.setAccentityRawToDtofromCtmJSONObject(param, bankReconciliation);
            billDataDto.setData(bankReconciliation);
            RuleExecuteResult ruleExecuteResult = fiBillService.executeUpdate(OperationTypeEnum.SAVE.getValue(), billDataDto);
            log.error("新增后返回数据：", CtmJSONObject.toJSONString(null == ruleExecuteResult ? "" : ruleExecuteResult.getData()));
            return Result.ok(null == ruleExecuteResult ? null : ruleExecuteResult.getData());
        } catch (Exception e) {
            throw new CtmException(e.getMessage(), e);
        } finally {
            JedisLockUtils.unlockBillWithOutTrace(ymsLock);
        }
    }



//    @Override
//    public Result<Object> insertForBatch(JSONObject params) throws Exception {
//        log.error("压测数据新增接口入参 param:" + params.toJSONString());
//        String note = params.getString("note");
//        // 添加备注作为查询条件
//        if (note == null || "".equals(note)) {
//            return Result.dateError(MessageUtils.getMessage("P_YS_CTM_CM-BE_1633196756466401283") /* "备注作为查询条件不可为空！" */);
//        }
//        //查询复制数据对账单
//        List<BankReconciliation> orignPushData = getBankReconciliationList(params);
//        // 如果未查询出需要执行的数据，返回信息
//        if (orignPushData == null || orignPushData.size() == 0) {
//            return Result.dateError(MessageUtils.getMessage("P_YS_CTM_CM-BE_1633196756466401292") /* "未查询到生单需要的基础数据！" */);
//        }
//        String insertCountStr = StringUtils.isEmpty(params.getString("insertCount")) ? "" + 20 : params.getString("insertCount");
//        int insertCount = Integer.parseInt(insertCountStr);
//
//        //插入任务个数上限300
//        if (insertCount > 300) {
//            insertCount = 300;
//        }
//        final String tenantId = AppContext.getTenantId().toString();
//        for (int j = 0; j < insertCount; j++) {
//            //根据规则匹配，并推送事件中心
//            executorServicePool.getThreadPoolExecutor().submit(() -> {
//                executePushBill(orignPushData, tenantId);
//            });
//        }
//        return Result.ok(MessageUtils.getMessage("P_YS_CTM_CM-BE_1633196756466401280") /* "线程池批量数据插入处理，请稍后查看数据！" */);
//    }

    private void executePushBill(List<BankReconciliation> orignPushData, String tenantId) {
        //循环插入数据，每次插入200行，计算循环次数
        int loopCount = 10;
        int singleInsert = 100;
        for (int i = 0; i < loopCount; i++) {
            List<BankReconciliation> insertNewBills = new ArrayList<BankReconciliation>();
            int j = 0, k = 0;
            while (j < singleInsert) {
                BankReconciliation newBill = (BankReconciliation) (orignPushData.get(k)).clone();
                newBill.setId(ymsOidGenerator.nextId());
                newBill.setBank_seq_no(newBill.getBank_seq_no() + "_" + newBill.getId());
                newBill.setNote(newBill.getNote() + "_" + newBill.getId());
                if (orignPushData.get(k).get("characterDef") != null) {
                    CmpBankReconciliationCharacterDef oldDef = orignPushData.get(k).get("characterDef");
                    CmpBankReconciliationCharacterDef def = (CmpBankReconciliationCharacterDef) oldDef.clone();
                    def.setId(ymsOidGenerator.nextId());
                    def.setEntityStatus(EntityStatus.Insert);
                    newBill.set("characterDef", def);
                }
                newBill.setEntityStatus(EntityStatus.Insert);
                insertNewBills.add(newBill);
                //以orignPushData为基础数据，遍历复制
                if (++k == orignPushData.size()) {
                    k = 0;
                }
                j++;
            }
            try {
                CommonSaveUtils.saveBankReconciliation(insertNewBills);
            } catch (Exception e) {
                log.error("压测数据新增接口子线程任务异常信息:" + e.getMessage());
                log.error("压测数据新增接口子线程任务异常:", e);
            }
        }
        InvocationInfoProxy.reset();
    }

    @Override
    public Result<Object> delete(CtmJSONObject param) throws Exception {
        CtmJSONArray idJsonArr = param.getJSONArray("idList");
        CtmJSONArray bankSeqNoJsonArr = param.getJSONArray("bankSeqNoList");
        QuerySchema schema = QuerySchema.create().addSelect("*");
        QueryConditionGroup condition = new QueryConditionGroup();
        // 参数校验
        if ((idJsonArr == null || idJsonArr.isEmpty()) && (bankSeqNoJsonArr == null || bankSeqNoJsonArr.isEmpty())) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101633"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00221", "参数idList和bankSeqNoList不可都为空！") /* "参数idList和bankSeqNoList不可都为空！" */);
        }
        if (idJsonArr != null && !idJsonArr.isEmpty()) {
            List<String> idList = idJsonArr.toJavaList(String.class) ;
            condition.addCondition(QueryConditionGroup.and(QueryCondition.name("id").in(idList)));
        }
        if (bankSeqNoJsonArr != null && !bankSeqNoJsonArr.isEmpty()) {
            List<String> seqNoList = bankSeqNoJsonArr.toJavaList(String.class);
            condition.addCondition(QueryConditionGroup.and(QueryCondition.name("bank_seq_no").in(seqNoList)));
        }
        AccentityUtil.addQueryConditionToGroupFromCtmJSONObject(param, condition);
        QueryConditionGroup repeatGroup = new QueryConditionGroup(ConditionOperator.or);
        repeatGroup.addCondition(QueryCondition.name("isrepeat").is_null());
        repeatGroup.addCondition(QueryCondition.name("isrepeat").is_not_null());
        condition.addCondition(repeatGroup);
        schema.addCondition(condition);
        // 获取执行数据
        List<BankReconciliation> bankVOList = MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME, schema, null);
        if (bankVOList == null || CollectionUtils.isEmpty(bankVOList)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101634"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00224", "没有找到符合条件的银行对账单！") /* "没有找到符合条件的银行对账单！" */);
        }
        log.error("银行对账单执行删除数据集:" + CtmJSONObject.toJSONString(bankVOList));
        // 执行删除逻辑
        BillDataDto billDataDto = new BillDataDto(IBillNumConstant.BANKRECONCILIATION);
        Map<String, Object> partParam = new HashMap<String, Object>();
        partParam.put("outsystem", "1");
        billDataDto.setPartParam(partParam);
        List<RuleExecuteResult> ruleExecuteResultList = new ArrayList<>();
        for (BankReconciliation bankReconciliation : bankVOList) {
            bankReconciliation.setEntityStatus(EntityStatus.Delete);
            bankReconciliation.set("_fromApi", true);//来源是openAPI
            billDataDto.setData(bankReconciliation);
            ruleExecuteResultList.add(fiBillService.executeUpdate(OperationTypeEnum.DELETE.getValue(), billDataDto));
        }
        return Result.ok(ruleExecuteResultList);
    }

    @Override
    public HashMap<String, Object> executeRule(CtmJSONObject param) throws Exception {
        CtmJSONArray idJsonArr = param.getJSONArray("idList");
        CtmJSONArray bankSeqNoJsonArr = param.getJSONArray("bankSeqNoList");
        QuerySchema schema = QuerySchema.create().addSelect("*");
        QueryConditionGroup condition = new QueryConditionGroup();
        // 参数校验
        if ((idJsonArr == null || idJsonArr.isEmpty()) && (bankSeqNoJsonArr == null || bankSeqNoJsonArr.isEmpty())) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101633"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00221", "参数idList和bankSeqNoList不可都为空！") /* "参数idList和bankSeqNoList不可都为空！" */);
        }
        if (idJsonArr != null && !idJsonArr.isEmpty()) {
            log.error("idList:" + idJsonArr.toString());
            ArrayList<String> list = new ArrayList<>();
            for (int i = 0; i < idJsonArr.size(); i++) {
                // 假设 getString 是 CtmJSONArray 提供的方法来获取字符串
                list.add(idJsonArr.getString(i));
            }
            String[] idArray = list.toArray(new String[0]);
            condition.appendCondition(QueryCondition.name("id").in((Object) idArray));
        }
        if (bankSeqNoJsonArr != null && !bankSeqNoJsonArr.isEmpty()) {
            log.error("bankSeqNoList:" + bankSeqNoJsonArr.toString());
            String[] bankSeqNoArray = bankSeqNoJsonArr.toArray(new String[0]);
            condition.appendCondition(QueryCondition.name("bank_seq_no").in((Object) bankSeqNoArray));
        }
        schema.addCondition(condition);
        // 获取执行数据
        List<BankReconciliation> bankVOList = MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME, schema, null);
        if (bankVOList == null || CollectionUtils.isEmpty(bankVOList)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101634"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00224", "没有找到符合条件的银行对账单！") /* "没有找到符合条件的银行对账单！" */);
        }
        log.error("银行对账单执行辨识规则数据集:" + CtmJSONObject.toJSONString(bankVOList));
        // 执行辨识规则和业务处理
        ruleEngineService.executeRuleEngine(bankVOList, CmpRuleEngineTypeConstant.cmp_identification, false);
        HashMap<String, Object> ret = new HashMap<String, Object>();
        return ret;
    }

    @Override
    public CtmJSONObject querylist(CtmJSONObject param) throws Exception {

        // 交易日期日期区间参数互斥
        String tran_date = param.getString("tran_date");
        String begindate = param.getString("begindate");
        String enddate = param.getString("enddate");
        String updatebegintime = param.getString("updatebegintime");
        String updateendtime = param.getString("updateendtime");

        String creatBegintime = param.getString("creat_begintime");
        String creatEndtime = param.getString("creat_endtime");
        String bankaccounts = param.getString("bankaccounts");
        String bankaccount = param.getString("bankaccount");
        String bankaccount_account = param.getString("bankaccount_account");
        String creat_begintime = param.getString("creat_begintime");
        String creat_endtime = param.getString("creat_endtime");
        if (!StringUtils.isEmpty(creat_begintime) && !StringUtils.isEmpty(creat_endtime)){
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date beginTime = sdf.parse(creat_begintime);
            Date endTime = sdf.parse(creat_endtime);
            if (beginTime.after(endTime)) {
                // creat_begintime 在 creat_endtime 之后
                throw  new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054004AD", "创建时间（截止）需晚于创建时间（开始），请检查!") /* "创建时间（截止）需晚于创建时间（开始），请检查!" */);
            }
        }
        if (!StringUtils.isEmpty(bankaccounts) && (!StringUtils.isEmpty(bankaccount) || (!StringUtils.isEmpty(bankaccounts) && !StringUtils.isEmpty(bankaccount_account)))){
            throw  new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054004B0", "本方银行账号,与本方银行账号(批量)不允许同时有值，请检查!") /* "本方银行账号,与本方银行账号(批量)不允许同时有值，请检查!" */);
        }
        if (StringUtils.isEmpty(creatBegintime) && !StringUtils.isEmpty(creatEndtime)){
            throw  new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054004B2", "创建时间(开始)与创建时间(结束)需同时有值或为空!") /* "创建时间(开始)与创建时间(结束)需同时有值或为空!" */);
        }
        if (!StringUtils.isEmpty(creatBegintime) && StringUtils.isEmpty(creatEndtime)){
            throw  new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054004B2", "创建时间(开始)与创建时间(结束)需同时有值或为空!") /* "创建时间(开始)与创建时间(结束)需同时有值或为空!" */);
        }
        // 交易日期与 开始结束日期不能同时有值
        if (!StringUtils.isEmpty(tran_date) && (!StringUtils.isEmpty(begindate) || !StringUtils.isEmpty(enddate))){
            throw  new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054004B7", "交易日期与开始日期、结束日期不允许同时有值，请检查!") /* "交易日期与开始日期、结束日期不允许同时有值，请检查!" */);
        }
        if (StringUtils.isEmpty(tran_date) && !StringUtils.isEmpty(begindate) && StringUtils.isEmpty(enddate)){
            throw  new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1B14FEF605F0007A", "开始日期与结束日期需同时有值或为空!") /* "开始日期与结束日期需同时有值或为空!" */);
        }
        if (StringUtils.isEmpty(tran_date) && StringUtils.isEmpty(begindate) && !StringUtils.isEmpty(enddate)){
            throw  new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1B14FEF605F0007A", "开始日期与结束日期需同时有值或为空!") /* "开始日期与结束日期需同时有值或为空!" */);
        }
        if (!StringUtils.isEmpty(begindate) && !StringUtils.isEmpty(enddate)){
            DateTime begindateparse = DateUtil.parse(begindate);
            DateTime enddateparse = DateUtil.parse(enddate);
            int compare = DateUtil.compare(begindateparse, enddateparse);
            if (compare > 0){
                throw  new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1B14FEF605F0007C", "结束日期应晚于等于开始日期!") /* "结束日期应晚于等于开始日期!" */);
            }
        }

        //202407增加财资统一对账码信息返回 smartcheckno
        // 增加返回isparsesmartcheckno、isrepeat
        QuerySchema querySchema = QuerySchema.create().addSelect("id,accentity,pubts,accentity.code as accentity_code,accentity.name as accentity_name," +
                "currency,currency.name as currency_name,bankaccount,bankaccount.acctName as bankaccount_acctName,bankaccount.account as bankaccount_account," +
                "bank_seq_no,tran_date,tran_time,remark01,dc_flag,tran_amt,to_acct_no,to_acct_name,to_acct_bank,to_acct_bank_name,use_name,remark,bankcheckno," +
                "interest,dataOrigin,currency.code as currency_code, unique_no,acct_bal,characterDef,smartcheckno,isparsesmartcheckno,isrepeat,createTime,createDate,oppositeobjectid," +
                "associationstatus,serialdealendstate,serialdealtype,bankaccount.bankNumber as bankNumber,bankaccount.bankNumber.name as bankNumberName");

        QueryConditionGroup group = new QueryConditionGroup(ConditionOperator.and);
        // 与列表查询保持一致，过滤掉期初未达项设置的期初数据
        group.addCondition(QueryCondition.name("initflag").eq(0));
        //根据api查询条件拼装
        //借贷方向
        if (param.getInteger("dc_flag") != null) {
            group.addCondition(QueryCondition.name("dc_flag").eq(param.getInteger("dc_flag")));
        }
        //会计主体
        if (param.getString("accentity") != null) {
            group.addCondition(QueryCondition.name("accentity").eq(param.getString("accentity")));
        } else { //未传id，则根据会计主体编码查询
            if (param.getString("accentity_code") != null) {
                group.addCondition(QueryCondition.name("accentity.code").eq(param.getString("accentity_code")));
            }
        }
        //币种
        if (param.getString("currency") != null) {
            group.addCondition(QueryCondition.name("currency").eq(param.getString("currency")));
        } else { //未传id，则根据币种名称
            if (param.getString("currency_name") != null) {
                group.addCondition(QueryCondition.name("currency.name").eq(param.getString("currency_name")));
            }
        }
        //本方银行账户
        if (param.getString("bankaccount") != null && !Objects.nonNull(param.getString("bankaccounts"))) {
            group.addCondition(QueryCondition.name("bankaccount").eq(param.getString("bankaccount")));
        } else if (!Objects.nonNull(param.getString("bankaccounts")) && param.getString("bankaccount") == null){ //未传id，则根据本行银行账户的账号
            if (param.getString("bankaccount_account") != null) {
                group.addCondition(QueryCondition.name("bankaccount.account").eq(param.getString("bankaccount_account")));
            }
        }
        //银行交易流水号
        if (param.getString("bank_seq_no") != null) {
            group.addCondition(QueryCondition.name("bank_seq_no").eq(param.getString("bank_seq_no")));
        }
        if (Objects.nonNull(param.getString("bankaccounts")) && param.getString("bankaccount") == null && param.getString("bankaccount_account") == null){
            List<String> list = Arrays.stream(param.getString("bankaccounts").split(","))
                    .map(String::trim) // 去除字符串首尾的空格
                    .collect(Collectors.toList());
            group.addCondition(QueryCondition.name("bankaccount.account").in(list));
        }

        //交易日期
        if (tran_date!= null) {
            group.addCondition(QueryCondition.name("tran_date").eq(tran_date));
        }
        if (tran_date == null && !StringUtils.isEmpty(begindate)) {
            group.addCondition(QueryCondition.name("tran_date").between(begindate,enddate));
        }
        if (!StringUtils.isEmpty(creatBegintime) && !StringUtils.isEmpty(creatEndtime)){
            group.addCondition(QueryCondition.name("createTime").between(creatBegintime,creatEndtime));
        }
        // 增加接收疑似重复字段，并根据疑似值返回对应的数据，默认返回0：正常 3:确认正常
        String isrepeats = param.getString("isrepeat");
        if (StringUtils.isNotEmpty(isrepeats)) {
            Short[] isrepeatShorts = Arrays.stream(isrepeats.split(","))
                    .map(s -> Short.parseShort(s.trim()))
                    .toArray(Short[]::new);
            group.addCondition(QueryCondition.name("isrepeat").in(isrepeatShorts));
        } else {
            group.addCondition(QueryCondition.name("isrepeat").in(BankDealDetailConst.REPEAT_INIT, BankDealDetailConst.REPEAT_NORMAL));
        }

        if (!StringUtils.isEmpty(updatebegintime) && !StringUtils.isEmpty(updateendtime)){
            group.addCondition(QueryCondition.name("pubts").between(updatebegintime,updateendtime));
        }else if (!StringUtils.isEmpty(updatebegintime) || !StringUtils.isEmpty(updateendtime)){
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054004B5", "修改时间(开始)、修改时间(结束)需同时有值或同时为空!") /* "修改时间(开始)、修改时间(结束)需同时有值或同时为空!" */);
        }

        //特征字段
        if (param.getObject("characterDef", LinkedHashMap.class) != null) {
            LinkedHashMap<String,Object> bizObject = param.getObject("characterDef", LinkedHashMap.class);
            //设置特征条件
            for (String key: bizObject.keySet()) {
                if(bizObject.get(key) != null && bizObject.get(key) != ""){
                    group.addCondition(QueryCondition.name("characterDef."+key).eq(bizObject.get(key)));
                }
            }
        }

        AccentityUtil.addQueryConditionToGroupFromCtmJSONObject(param, group);
        querySchema.addCondition(group);
        //对账单日期倒序
        querySchema.addOrderBy(new QueryOrderby("tran_date", "desc"));
        querySchema.addOrderBy(new QueryOrderby("id","asc"));
        QuerySchema countSchema = QuerySchema.create().addSelect("count(id)");
        countSchema.addCondition(group);
        Map<String,Object> bankReconCountMap = MetaDaoHelper.queryOne(BankReconciliation.ENTITY_NAME, countSchema);
        int totalSize = Integer.parseInt(bankReconCountMap.get("count").toString());
        querySchema.addPager(param.getInteger("pageIndex"), param.getInteger("pageSize"));
        List<Map<String, Object>> infoMapList = MetaDaoHelper.query(BankReconciliation.ENTITY_NAME, querySchema);
        int pageNum = PageUtil.totalPage(totalSize, param.getInteger("pageSize"));

        //解析查询数据
        List<CtmJSONObject> resultList = new ArrayList<>();
        List<String> bankrecIdList = new ArrayList<>();
        if (!infoMapList.isEmpty()) {
            for (Map<String, Object> b : infoMapList) {
                bankrecIdList.add(b.get("id").toString());
            }
            QuerySchema queryElecSchema = QuerySchema.create().addSelect(" bankreconciliationid,remark01 ");
            QueryConditionGroup queryConditionGroup = QueryConditionGroup.and(QueryCondition.name("bankreconciliationid").in(bankrecIdList));
            queryElecSchema.addCondition(queryConditionGroup);
            queryElecSchema.addPager(param.getInteger("pageIndex"), param.getInteger("pageSize"));
            List<BankElectronicReceipt> bankElectronicReceiptlist = MetaDaoHelper.queryObject(BankElectronicReceipt.ENTITY_NAME, queryElecSchema, null);
            HashMap<String, String> idRemarkMap = new HashMap<>();
            if (!bankElectronicReceiptlist.isEmpty()) {
                for (BankElectronicReceipt receipt : bankElectronicReceiptlist) {
                    idRemarkMap.put(receipt.getBankreconciliationid(), receipt.getRemark01());
                }
            }
            for (Map<String, Object> b : infoMapList) {
                b.put("remak01", idRemarkMap.get(b.get("id").toString()));
                CtmJSONObject r = CtmJSONObject.parseObject(CtmJSONObject.toJSONString(b));
                resultList.add(r);
            }
        }
        CtmJSONObject result = new CtmJSONObject();
        CtmJSONObject resultChild = new CtmJSONObject();
        result.put("code", 200);
        resultChild.put("totalCount",totalSize);
        resultChild.put("pageNum",pageNum);
//        resultChild.put("beginPage",param.getInteger("pageIndex"));
//        resultChild.put("endPage",pageNum);
        resultChild.put("recordList", resultList);
        result.put("message", "success");
        result.put("data", resultChild);
        return result;
    }

    @Override
    public CtmJSONObject queryClaimCenterList(CtmJSONObject param) throws Exception {
        QuerySchema querySchema = QuerySchema.create().addSelect("id,accentity,accentity.code as accentity_code,accentity.name as accentity_name,tran_amt , " +
                "bankaccount,bankaccount.acctName as bankaccount_acctName,bankaccount.account as bankaccount_account,tran_date,dc_flag," + "currency,currency.name as currency_name," +
                "amounttobeclaimed,to_acct_no,to_acct_name,to_acct_bank,to_acct_bank_name,remark,createTime,createDate");

        QueryConditionGroup group = new QueryConditionGroup(ConditionOperator.and);
        //根据api查询条件拼装
        //会计主体
        if (param.getString("accentity") != null) {
            group.addCondition(QueryCondition.name("accentity").eq(param.getString("accentity")));
        } else { //未传id，则根据会计主体编码查询
            if (param.getString("accentity_code") != null) {
                group.addCondition(QueryCondition.name("accentity.code").eq(param.getString("accentity_code")));
            }
        }
        //本方银行账户
        if (param.getString("bankaccount") != null) {
            group.addCondition(QueryCondition.name("bankaccount").eq(param.getString("bankaccount")));
        } else { //未传id，则根据本行银行账户的账号
            if (param.getString("bankaccount_account") != null) {
                group.addCondition(QueryCondition.name("bankaccount.account").eq(param.getString("bankaccount_account")));
            }
        }
        //交易日期
        if (param.getString("tran_date") != null) {
            group.addCondition(QueryCondition.name("tran_date").eq(param.getString("tran_date")));
        }
        //借贷方向
        if (param.getInteger("dc_flag") != null) {
            group.addCondition(QueryCondition.name("dc_flag").eq(param.getInteger("dc_flag")));
        }
        //已发布待认领的
        group.addCondition(QueryCondition.name("billclaimstatus").eq(BillClaimStatus.ToBeClaim.getValue()));
        group.addCondition(QueryCondition.name("ispublish").eq(true));
        AccentityUtil.addQueryConditionToGroupFromCtmJSONObject(param, group);
        querySchema.addCondition(group);
        //对账单日期倒序
        querySchema.addOrderBy(new QueryOrderby("tran_date", "desc"));
        querySchema.addOrderBy(new QueryOrderby("id","asc"));
        querySchema.addPager(param.getInteger("pageIndex"), param.getInteger("pageSize"));
        List<Map<String, Object>> infoMapList = MetaDaoHelper.query(BankReconciliation.ENTITY_NAME, querySchema);

        //解析查询数据
        List<CtmJSONObject> resultList = new ArrayList<>();
        for (Map<String, Object> b : infoMapList) {
            CtmJSONObject r = CtmJSONObject.parseObject(CtmJSONObject.toJSONString(b));
            resultList.add(r);
        }
        CtmJSONObject result = new CtmJSONObject();
        result.put("code", 200);
        result.put("message", "success");

        CtmJSONObject recordList = new CtmJSONObject();
        recordList.put("recordList", resultList);
        result.put("data", recordList);

        return result;
    }

    /**
     * 新增关联关系
     *
     * @param param
     * @return
     */
    @Override
    public ArrayList<HashMap<String, String>> insertrelation(CtmJSONArray param) {
        if (param.size() > 500) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100740"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:YS_CTM_CM-BE_LOCAL_00050003", "一次处理银行对账单个数不能超过500!") /* "一次处理银行对账单个数不能超过500!" */);
        }
        ArrayList<HashMap<String, String>> retMsgList = new ArrayList<HashMap<String, String>>();
        List<CtmJSONObject> paramList = param.toJavaList(CtmJSONObject.class);
        for (int i = 0; i < paramList.size(); i++) {
            CtmJSONObject bankbill = paramList.get(i);
            String bank_seq_no = bankbill.getString("bank_seq_no");
            try {
                HashMap<String, String> msg = insertRelationByBankBill(bankbill);
                retMsgList.add(msg);
            } catch (Exception e) {
                log.error("自动关联出错:{}", bank_seq_no, e);
                HashMap<String, String> retMsg = new HashMap<String, String>();
                retMsg.put("bank_seq_no", bank_seq_no);
                retMsg.put(FLAG, FAIL);
                retMsg.put("msg", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:YS_CTM_CM-BE_LOCAL_00050004", "自动关联出错！错误信息如下：") /* "自动关联出错！错误信息如下：" */ + e.getMessage());
                retMsgList.add(retMsg);
            }
        };
        return retMsgList;
    }

    private HashMap<String, String> insertRelationByBankBill(CtmJSONObject bankbill) throws Exception {
        HashMap<String, String> retMsg = new HashMap<String, String>();
        CtmJSONObject busrelations = bankbill.getJSONObject("BankReconciliationbusrelation_b");
        String bank_seq_no = bankbill.getString("bank_seq_no");
        if (busrelations == null || busrelations.isEmpty()) {
            retMsg.put("bank_seq_no", bank_seq_no);
            retMsg.put(FLAG, FAIL);
            retMsg.put("msg", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:YS_CTM_CM-BE_LOCAL_00050007", "银行对账单关联信息不能为空!") /* "银行对账单关联信息不能为空!" */);
            return retMsg;
        }
        List<BankReconciliation> bankList = queryBankReconciliationData(bankbill);
        if (bankList == null || bankList.size() != 1) {
            retMsg.put("bank_seq_no", bank_seq_no);
            retMsg.put(FLAG, FAIL);
            retMsg.put("msg", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_186AE19C04C00008", "未找到的唯一的未发布的银行对账单！") /* "未找到的唯一的未发布的银行对账单！" */);
            return retMsg;
        }
        BankReconciliation bankReconciliation = bankList.get(0);
        String key = "API_insertrelation_" + bankReconciliation.getId().toString();
        YmsLock ymsLock = null;
        try {
            if ((ymsLock=JedisLockUtils.lockBillWithOutTrace(key))==null) {
                retMsg.put("bank_seq_no", bank_seq_no);
                retMsg.put(FLAG, FAIL);
                retMsg.put("msg", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00223", "该数据正在处理，请稍后重试！") /* "该数据正在处理，请稍后重试！" */);
                return retMsg;
            }
            List<BankReconciliationbusrelation_b> busrelationbList = queryBankReconciliationRelationData("" + bankReconciliation.getId());
            boolean delRelation = false;
            if (busrelationbList != null && !busrelationbList.isEmpty()) {
                //关联信息中有非异构系统关联信息
                for (int i = 0; i < busrelationbList.size(); i++) {
                    if (StringUtils.isEmpty(busrelationbList.get(i).getOutbilltypename())) {
                        retMsg.put("bank_seq_no", bank_seq_no);
                        retMsg.put(FLAG, FAIL);
                        retMsg.put("msg", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:YS_CTM_CM-BE_LOCAL_00050001", "已在BIP系统完成与业务单据的关联，不允许再次关联，请检查！") /* "已在BIP系统完成与业务单据的关联，不允许再次关联，请检查！" */);
                        return retMsg;
                    } else {
                        busrelationbList.get(i).setEntityStatus(EntityStatus.Delete);
                    }
                }
                //关联信息全部是异构系统关联数据
                delRelation = true;
            }
            apiCorrService.insertRelation4BankBill(bankReconciliation, delRelation, busrelationbList, busrelations);
        } catch (Exception e) {
            log.error("自动关联出错:{}", bank_seq_no, e);
            retMsg.put("bank_seq_no", bank_seq_no);
            retMsg.put(FLAG, FAIL);
            retMsg.put("msg", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:YS_CTM_CM-BE_LOCAL_00050004", "自动关联出错！错误信息如下：") /* "自动关联出错！错误信息如下：" */ + e.getMessage());
            return retMsg;
        } finally {
            JedisLockUtils.unlockBillWithOutTrace(ymsLock);
        }
        retMsg.put("bank_seq_no", bank_seq_no);
        retMsg.put(FLAG, SUCCESS);
        retMsg.put("msg", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:YS_CTM_CM-BE_LOCAL_00050005", "新增关联信息成功！") /* "新增关联信息成功！" */);
        return retMsg;
    }


    private List<BankReconciliation> queryBankReconciliationData(CtmJSONObject param) throws Exception {
        QuerySchema querySchema = QuerySchema.create().addSelect("*");

        QueryConditionGroup group = new QueryConditionGroup(ConditionOperator.and);
        group.addCondition(QueryCondition.name("bank_seq_no").eq(param.getString("bank_seq_no")));
        //根据api查询条件拼装
        //借贷方向
        if (param.getInteger("dc_flag") != null) {
            group.addCondition(QueryCondition.name("dc_flag").eq(param.getInteger("dc_flag")));
        }
        //会计主体
        if (param.getString("accentity") != null) {
            group.addCondition(QueryCondition.name("accentity").eq(param.getString("accentity")));
        } else { //未传id，则根据会计主体编码,名称查询
            if (param.getString("accentity_code") != null) {
                group.addCondition(QueryCondition.name("accentity.code").eq(param.getString("accentity_code")));
            } else if (param.getString("accentity_name") != null) {
                group.addCondition(QueryCondition.name("accentity.name").eq(param.getString("accentity_name")));
            }
        }
        //币种
        if (param.getString("currency") != null) {
            group.addCondition(QueryCondition.name("currency").eq(param.getString("currency")));
        } else { //未传id，则根据币种名称
            if (param.getString("currency_name") != null) {
                group.addCondition(QueryCondition.name("currency.name").eq(param.getString("currency_name")));
            }
        }
        //本方银行账户
        if (param.getString("bankaccount") != null) {
            group.addCondition(QueryCondition.name("bankaccount").eq(param.getString("bankaccount")));
        } else { //未传id，则根据本行银行账户的账号
            if (param.getString("bankaccount_account") != null) {
                group.addCondition(QueryCondition.name("bankaccount.account").eq(param.getString("bankaccount_account")));
            }
        }
        //银行交易流水号
        if (param.getString("bank_seq_no") != null) {
            group.addCondition(QueryCondition.name("bank_seq_no").eq(param.getString("bank_seq_no")));
        }
        //交易日期
        if (param.getString("tran_date") != null) {
            group.addCondition(QueryCondition.name("tran_date").eq(param.getString("tran_date")));
        }
        //交易金额
        if (param.getString("tran_amt") != null) {
            group.addCondition(QueryCondition.name("tran_amt").eq(param.getBigDecimal("tran_amt")));
        }
        group.addCondition(QueryCondition.name("ispublish").eq(false));
        querySchema.addCondition(group);
        List<BankReconciliation> infoMapList = MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME, querySchema, null);
        return infoMapList;
    }

    private List<BankReconciliationbusrelation_b> queryBankReconciliationRelationData(String bankreconciliationId) throws Exception {
        QuerySchema querySchema = QuerySchema.create().addSelect("*");
        QueryConditionGroup group = new QueryConditionGroup(ConditionOperator.and);
        group.addCondition(QueryCondition.name("bankreconciliation").eq(bankreconciliationId));
        querySchema.addCondition(group);
        List<BankReconciliationbusrelation_b> infoMapList = MetaDaoHelper.queryObject(BankReconciliationbusrelation_b.ENTITY_NAME, querySchema, null);
        return infoMapList;
    }

    @Override
    @Transactional(rollbackFor = RuntimeException.class)
    public HashMap<String,Object> claimToReceipt(CtmJSONObject param) throws Exception {
        HashMap<String, Object> ret = new HashMap<String, Object>();
        /**
         * 1，根据id查询银行对账单
         * 2，判断银行对账单能够进行认领操作
         * 3，银行对账单整单认领生成认领单
         * 4，认领单调用单据转换规则生成来款记录
         */
        CtmJSONArray idJsonArr = param.getJSONArray("idList");
        QuerySchema schema = QuerySchema.create().addSelect("*");
        QueryConditionGroup condition = new QueryConditionGroup();
        if (idJsonArr != null && !idJsonArr.isEmpty()) {
            condition.appendCondition(QueryCondition.name("id").in(idJsonArr));
        }
        schema.addCondition(condition);
        // 获取执行数据
        List<BankReconciliation> bankVOList = MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME, schema, null);
        if (bankVOList == null || CollectionUtils.isEmpty(bankVOList)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101634"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00224", "没有找到符合条件的银行对账单！") /* "没有找到符合条件的银行对账单！" */);
        }
        if(log.isInfoEnabled()) {
            log.info("认领数据集:" + CtmJSONObject.toJSONString(bankVOList));
        }
        List<BankReconciliation> successList = new ArrayList<>();
        List<String> receiptDatas = new ArrayList<>();
        List<BillClaim> billClaimList = new ArrayList<>();
        for(BankReconciliation bankReconciliation : bankVOList){
            try {
                BillClaim billClaim = new BillClaim();
                //2,校验银行对账单能否进行认领操作
                if((bankReconciliation.getAssociationstatus() != null && bankReconciliation.getAssociationstatus() != 1) &&
                        bankReconciliation.getBillclaimstatus() == 0 && bankReconciliation.getAmounttobeclaimed() != null &&
                        (bankReconciliation.getAmounttobeclaimed().compareTo(bankReconciliation.getTran_amt()) == 0)){
                    //3，银行对账单整单认领生成认领单
                    bankreconciliationToClaim(bankReconciliation,billClaim);
//                    //4，认领单调用单据转换规则生成来款记录
//                    String bizObject = claimPullAndPushToReceipt(billClaim, bankReconciliation);
//                    receiptDatas.add(bizObject);
                    billClaimList.add(billClaim);
                    // 成功数据
                    successList.add(bankReconciliation);
                }else {
                    log.info("【"+bankReconciliation.getBank_seq_no()+"】不能进行整单认领操作,请检查认领状态,关联状态及认领金额！");
                    ret.put("message","【"+bankReconciliation.getBank_seq_no()+com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054004B8", "】不能进行整单认领操作,请检查认领状态,关联状态及认领金额！") /* "】不能进行整单认领操作,请检查认领状态,关联状态及认领金额！" */);//@notranslate
                }
            }catch (Exception e){
                log.info("【"+bankReconciliation.getBank_seq_no()+"】认领并生成来款记录失败！");
                throw new Exception("【"+bankReconciliation.getBank_seq_no()+com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054004BB", "】认领并生成来款记录失败！") /* "】认领并生成来款记录失败！" */+e.getMessage());//@notranslate
            }
        }

        // 事务提交
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @SneakyThrows
            @Override
            public void afterCommit() {
                try {
                    BankReconciliation bankReconciliation = new BankReconciliation();
                    // 生成来款记录
                    //4，认领单调用单据转换规则生成来款记录
                    for(BillClaim billClaim : billClaimList){
                        billClaim = MetaDaoHelper.findById(BillClaim.ENTITY_NAME, billClaim.getId(), 3);
                        String bizObject = claimPullAndPushToReceipt(billClaim, bankReconciliation);
                        receiptDatas.add(bizObject);
                    }
                } catch (Exception e) {
                    throw new Exception(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054004B1", "事务提交后，调用单据转换规则及来款记录保存失败！") /* "事务提交后，调用单据转换规则及来款记录保存失败！" */+e.getMessage(),e);
                }
            }
        });

        if(successList.size() > 0){
            ret.put("message",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054004B6", "认领并生成来款记录成功") /* "认领并生成来款记录成功" */);
            ret.put("data", receiptDatas);
        }
        return ret;
    }

    private void bankreconciliationToClaim(BankReconciliation bankReconciliation, BillClaim claim) throws Exception {
        try {
            //整单认领 银行对账单单-->认领单
            // 主表
            // 认领类型
            claim.setClaimtype(BillClaimType.Whole.getValue());
            // 认领金额
            claim.setTotalamount(bankReconciliation.getAmounttobeclaimed());
            // 会计主体
            claim.setAccentity(bankReconciliation.getAccentity());
            claim.setAccentityRaw(bankReconciliation.getAccentityRaw());
            // 业务单元
            claim.setOrg(bankReconciliation.getAccentity());
            // 币种
            claim.setCurrency(bankReconciliation.getCurrency());
            // 银行账户
            claim.setBankaccount(bankReconciliation.getBankaccount());
            // 对方账号
            claim.setToaccountno(bankReconciliation.getTo_acct_no());
            claim.setToaccountname(bankReconciliation.getTo_acct_name());
            claim.setToaccountbank(bankReconciliation.getTo_acct_bank());
            claim.setToaccountbankname(bankReconciliation.getTo_acct_bank_name());
            // 入账类型
            claim.setEntrytype(bankReconciliation.getEntrytype());
            // 内部账户是否记账
            claim.setIsinneraccounting(bankReconciliation.getIsinneraccounting());
            // 归集内部账户
            claim.setImpinneraccount(bankReconciliation.getImpinneraccount());
            // 认领说明
            claim.setRemark(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054004B9", "来款记录接口自动认领") /* "来款记录接口自动认领" */);
            // 认领日期
            claim.setClaimdate(new Date());
            // 收付类型
            claim.setDirection(bankReconciliation.getDc_flag().getValue());
            // id
            claim.setId(ymsOidGenerator.nextId());
            claim.setCreateDate(new Date());
            claim.setCreateTime(new Date());
//        // code
            BillCodeComponentParam billCodeComponentParam = new BillCodeComponentParam(
                    CmpBillCodeMappingConfUtils.getBillCode("cmp_billclaimcard"),
                    "cmp_billclaimcard",
                    AppContext.getTenantId().toString(),
                    claim.getAccentity().toString(),
                    BillClaim.ENTITY_NAME,
                    new BillCodeObj[]{new BillCodeObj(claim)});
            String[] codelist = billCodeComponentService.getBatchBillCodes(billCodeComponentParam);
            claim.setCode(codelist != null?codelist[0]:null);
            // 追加字段 cmp_billclaim
            // claimstaff,creator,creatorId,vouchdate,status,actualclaimaccentiry,actualclaimaccentiry_raw,actualsettleaccentity,
            // businessmodel,claimaccount,earlyentry,claimperson,affiliatedorgid,auditstatus
            // verifystate
            claim.setCreator(InvocationInfoProxy.getUsername());
            claim.setCreatorId(Long.parseLong(InvocationInfoProxy.getIdentityId()));
            claim.setClaimperson(InvocationInfoProxy.getIdentityId());
            claim.setClaimstaff(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054004B3", "默认用户") /* "默认用户" */);
            claim.setVouchdate(new Date());
            claim.setStatus(Short.parseShort("1"));
            claim.setActualclaimaccentiry(bankReconciliation.getAccentity());
            claim.setactualclaimaccentiryRaw(bankReconciliation.getAccentityRaw());
            claim.setActualsettleaccentity(bankReconciliation.getAccentity());
            claim.setBusinessmodel(BusinessModel.General_Settlement.getCode());
            claim.setClaimaccount(bankReconciliation.getBankaccount());
            claim.setEarlyentry(Boolean.FALSE);
            claim.set("auditstatus", AuditStatus.Complete.getValue());
            claim.setVerifystate(VerifyState.COMPLETED.getValue());
            // 子表
            BillClaimItem billClaimItem = new BillClaimItem();
            billClaimItem.setId(ymsOidGenerator.nextId());
            // 主表id
            billClaimItem.setMainid(claim.getId());
            // 银行对账单id
            billClaimItem.setBankbill(bankReconciliation.getId());
            // 会计主体
            billClaimItem.setAccentity(bankReconciliation.getAccentity());
            billClaimItem.setAccentityRaw(bankReconciliation.getAccentityRaw());
            // 银行账户
            billClaimItem.setBankaccount(bankReconciliation.getBankaccount());
            // 交易日期
            billClaimItem.setTran_date(bankReconciliation.getTran_date());
            billClaimItem.setTran_amt(bankReconciliation.getTran_amt());
            // 币种
            billClaimItem.setCurrency(bankReconciliation.getCurrency());
            // 待认领金额
            billClaimItem.setAmounttobeclaimed(bankReconciliation.getAmounttobeclaimed());
            // 收付方向
            billClaimItem.setDirection(bankReconciliation.getDc_flag().getValue());
            // 对方类型
            billClaimItem.setOppositetype(bankReconciliation.getOppositetype());
            // 对方单位
            billClaimItem.setOppositeobjectid(bankReconciliation.getOppositeobjectid());
            billClaimItem.setOppositeobjectname(bankReconciliation.getOppositeobjectname());
            // 对方账号
            billClaimItem.setTo_acct_no(bankReconciliation.getTo_acct_no());
            billClaimItem.setTo_acct_name(bankReconciliation.getTo_acct_name());
            billClaimItem.setTo_acct_bank(bankReconciliation.getTo_acct_bank());
            billClaimItem.setTo_acct_bank_name(bankReconciliation.getTo_acct_bank_name());
            // 备注
            billClaimItem.setRemark(bankReconciliation.getRemark());
            // 认领金额
            billClaimItem.setClaimamount(bankReconciliation.getAmounttobeclaimed());
            billClaimItem.setTotalamount(bankReconciliation.getAmounttobeclaimed());
            billClaimItem.setEntrytype(bankReconciliation.getEntrytype());
            billClaimItem.setEntityStatus(EntityStatus.Insert);
            List<BillClaimItem> billClaimItems = new ArrayList<>();
            billClaimItems.add(billClaimItem);
            claim.setItems(billClaimItems);
            claim.setEntityStatus(EntityStatus.Insert);
            // 我的认领保存支持客开扩展
            BillContext billContext = new BillContext();
            billContext.setBillnum("cmp_billclaimcard");
            Map<String, Object> paramMap = new HashMap<>();
            paramMap.put("billclaim", claim);
            billContext.setAction("billclaim_extend");
            BillBiz.executeRule("billclaim_extend", billContext, paramMap);
            if(paramMap.get("isExecute") != null && paramMap.get("billclaim") != null){
                claim = (BillClaim) paramMap.get("claim");
            }
            //入库操作
            CommonSaveUtils.saveBillClaim(claim);

            // 银行对账单(到账认领中心) 认领状态修改
            bankReconciliation.setBillclaimstatus(BillClaimStatus.Claimed.getValue());
            bankReconciliation.setAmounttobeclaimed(BigDecimal.ZERO);
            bankReconciliation.setClaimamount(claim.getTotalamount());
            bankReconciliation.setEntityStatus(EntityStatus.Update);
            CommonSaveUtils.updateBankReconciliation(bankReconciliation, null);
        }catch (Exception e){
            throw new Exception(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1B14FEF605F0007B", "银行对账单整单认领失败！") /* "银行对账单整单认领失败！" */+e.getMessage());
        }

    }

    @Override
    public String claimPullAndPushToReceipt(BillClaim claim, BankReconciliation bankReconciliation) throws Exception {
        // 付款方向单据不生成来款记录
        if(claim.getDirection() != null && Direction.Debit.getValue() == claim.getDirection()){
            throw new Exception(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054004AC", "支出方向单据暂不支持生成来款记录！") /* "支出方向单据暂不支持生成来款记录！" */);
        }
        BillClaimItem billClaimItem = new BillClaimItem();
        billClaimItem.setClaimamount(claim.items().get(0).getTotalamount());
        List<BillClaimItem> billClaimItems = new ArrayList<>();
        // 对方单位
        if(claim.items().get(0).getOppositeobjectid() != null){
            billClaimItem.setOppositeobjectid(claim.items().get(0).getOppositeobjectid());
        }
        billClaimItems.add(billClaimItem);
        claim.setItems(billClaimItems);

        List<BizObject> billClaims = new ArrayList<>();
        billClaims.add(claim);
        Long[] childIds = new Long[1];
        childIds[0] = claim.getId();
        CtmJSONObject requestObject = new CtmJSONObject();
        requestObject.put("code","billclaim_to_receipt");
        requestObject.put("childIds",childIds);
        requestObject.put("isMainSelect", 1);
        requestObject.put("data",billClaims);
        requestObject.put("sourceData",billClaims);
        Map<String, Object> externalMap = new HashMap<>();
        externalMap.put("isUseBusiQuery", Boolean.TRUE);
        requestObject.put("externalDataMap", externalMap);
        try {
            log.error("===================认领单生成来款记录==========================>"+requestObject.toString());//@notranslate
//            CtmJSONObject result = HttpsUtils.doHttpsPost("", requestData, PropertyUtil.getPropertyByKey("domain.yonbip-mkt-mkc2b")+"/bill/pullVoucher?code=billclaim_to_receipt&targetBillNo=payment_voucher_detail_receipt&domain=marketingbill&terminalType=1&serviceCode=ficmp0034&isNewTpl=true&sbillno=cmp_mybillclaimlist");
            CtmJSONObject result = httpsService.doHttpsPostForXinLianXin("", requestObject, AppContext.getEnvConfig("domain.yonbip-mkt-mkc2b")+"/bill/pullVoucher?code=billclaim_to_receipt&targetBillNo=payment_voucher_detail_receipt&domain=marketingbill&terminalType=1&serviceCode=ficmp0034&isNewTpl=true&sbillno=cmp_mybillclaimlist");
            log.error("===================认领单生成来款记录==========================>"+CtmJSONObject.toJSONString(result));//@notranslate
            if(!"200".equals(result.getJSONObject("result").get("code").toString())){
                throw new Exception(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054004AB", "单据转换规则调用失败！") /* "单据转换规则调用失败！" */+CtmJSONObject.toJSONString(result));
            }
            // 保存操作
            CtmJSONObject requestObject2 = new CtmJSONObject();
            requestObject2.put("data",result.getJSONObject("result").getJSONObject("data").get("targetData"));
            requestObject2.put("billnum","payment_voucher_detail_receipt");

            CtmJSONObject saveResult = httpsService.doHttpsPostForXinLianXin("", requestObject2, AppContext.getEnvConfig("domain.yonbip-mkt-mkc2b")+"/bill/save?cmdname=cmdSave&terminalType=1&serviceCode=UPaymentReceipt&sbillno=payment_voucher_list_receipt");
            if(!"200".equals(saveResult.getJSONObject("result").get("code").toString())){
                throw new Exception(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054004AF", "单据转换规则调用成功，来款记录保存失败！") /* "单据转换规则调用成功，来款记录保存失败！" */+CtmJSONObject.toJSONString(saveResult));
            }
        }catch (Exception e){
            throw new Exception(e.getMessage(),e);
        }
        return "success";
    }

    @Override
    public CtmJSONObject batchUpdate(CtmJSONArray jsonArray) throws Exception {
        CtmJSONObject result = new CtmJSONObject();
        // json转对象
        List<CtmJSONObject> paramList = jsonArray.toJavaList(CtmJSONObject.class);
        List<CtmJSONObject> dataList = new ArrayList<>();
        for (CtmJSONObject param : paramList) {
            if(param.get("id") == null && param.get("unique_no") == null) {
                throw new Exception(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054004BC", "参数错误！id或unique_no不能同时为空！") /* "参数错误！id或unique_no不能同时为空！" */);
            }
            BankReconciliation bankReconciliation = null;
            if(param.get("id") != null){
                bankReconciliation = MetaDaoHelper.findById(BankReconciliation.ENTITY_NAME,param.get("id"));
            }else{
                QuerySchema schema = QuerySchema.create().addSelect("*");
                QueryConditionGroup condition = new QueryConditionGroup();
                condition.addCondition(QueryCondition.name("unique_no").eq(param.get("unique_no")));
                schema.addCondition(condition);
                // 获取执行数据
                List<BankReconciliation> bankVOList = MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME, schema, null);
                if(bankVOList!=null && bankVOList.size()>0){
                    bankReconciliation = bankVOList.get(0);
                }
            }
            CtmJSONObject data = new CtmJSONObject();
            if (bankReconciliation == null) {
                data.put("id",param.get("id"));
                data.put("bankseqno",param.get("bank_seq_no"));
                if(StringUtils.isNotEmpty(param.getString("unique_no"))){
                    data.put("unique_no",param.getString("unique_no"));
                }
                data.put("code",100);
                data.put("message",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054004B4", "银行流水id对应数据信息不存在") /* "银行流水id对应数据信息不存在" */);
                dataList.add(data);
                continue;
            }

            //流水处理完结方式。1：收付单据关联，2：收付单据生单，3：业务凭据关联 4 异构系统处理
            if (param.getShort("serialdealtype") != null && (param.getShort("serialdealtype") == 1
                    || param.getShort("serialdealtype") == 2 || param.getShort("serialdealtype") == 3 || param.getShort("serialdealtype") == 4)) {
                bankReconciliation.setSerialdealtype(param.getShort("serialdealtype"));
            }

            // 流水处理完结方式为 4：异构系统处理时，跳过更新校验
            if(param.getShort("serialdealtype") != null && param.getShort("serialdealtype") == 4){
                // 业务关联状态为 未关联时，赋值流水处理完结方式为空
                if(param.getShort("associationstatus") != null && param.getShort("associationstatus") == 0){
                    bankReconciliation.setSerialdealtype(null);
                }
            }else{
                //更新时判定银行流水认领是否发生后续业务，即单据字段（收付单据关联（业务关联）为'已关联'或总账是否已勾对='是'或日记账是否已勾对=’是‘）
                if (bankReconciliation.getAssociationstatus() == AssociationStatus.Associated.getValue()
                        || bankReconciliation.getCheckflag() || bankReconciliation.getOther_checkflag()) {
                    data.put("id",param.get("id"));
                    data.put("code",100);
                    data.put("bankseqno",bankReconciliation.getBank_seq_no());
                    data.put("message",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054004AE", "交易流水已发生后续业务，无法进行更新，请检查！") /* "交易流水已发生后续业务，无法进行更新，请检查！" */);
                    dataList.add(data);
                    continue;
                }
            }
            // 如果完结状态为未完结时，流水处理方式置空
            if(param.getShort("serialdealendstate") != null && param.getShort("serialdealendstate") == 0){
                bankReconciliation.setSerialdealtype(null);
            }
            //银行交易流水号
            if (param.getString("bank_seq_no") != null) {
                bankReconciliation.setBank_seq_no(param.getString("bank_seq_no"));
            }
            //第三方流水号
            if (param.getString("thirdserialno") != null) {
                bankReconciliation.setThirdserialno(param.getString("thirdserialno"));
            }
            //交易日期
            if (param.getDate("tran_date") != null) {
                bankReconciliation.setTran_date(param.getDate("tran_date"));
                bankReconciliation.setDzdate(param.getDate("tran_date"));
            }
            //交易时间
            if (param.getDate("tran_time") != null) {
                bankReconciliation.setTran_time(param.getDate("tran_time"));
            }
            //借贷方向
            if (param.getShort("dc_flag") != null && ( param.getShort("dc_flag") == Direction.Credit.getValue()
                    || param.getShort("dc_flag") == Direction.Debit.getValue()) ) {
                bankReconciliation.setDc_flag(Direction.find(param.getShort("dc_flag")));
                if (param.getShort("dc_flag") == Direction.Debit.getValue()){
                    bankReconciliation.setDebitamount(bankReconciliation.getTran_amt());
                    bankReconciliation.setCreditamount(null);
                }
                if (param.getShort("dc_flag") == Direction.Credit.getValue()){
                    bankReconciliation.setCreditamount(bankReconciliation.getTran_amt());
                    bankReconciliation.setDebitamount(null);
                }
            }
            //交易金额
            if (param.getBigDecimal("tran_amt") != null) {
                bankReconciliation.setTran_amt(param.getBigDecimal("tran_amt"));
                if (bankReconciliation.getDc_flag().getValue() == Direction.Debit.getValue()){
                    bankReconciliation.setDebitamount(bankReconciliation.getTran_amt());
                    bankReconciliation.setCreditamount(null);
                }
                if (bankReconciliation.getDc_flag().getValue() == Direction.Credit.getValue()){
                    bankReconciliation.setCreditamount(bankReconciliation.getTran_amt());
                    bankReconciliation.setDebitamount(null);
                }
            }
            //余额
            if (param.getBigDecimal("acct_bal") != null) {
                bankReconciliation.setAcct_bal(param.getBigDecimal("acct_bal"));
            }
            //对方户名
            if (param.getString("to_acct_name") != null) {
                bankReconciliation.setTo_acct_name(param.getString("to_acct_name"));
            }
            //对方账号，输入对方账号需要再次进行辨识
            if (param.getString("to_acct_no") != null) {
                bankReconciliation.setTo_acct_no(param.getString("to_acct_no"));
                BillSmartClassifyBO classifyBO = billSmartClassifyService.smartClassify(
                        bankReconciliation.getAccentity(), param.getString("to_acct_no"), bankReconciliation.getTo_acct_name(), bankReconciliation.getCurrency(),bankReconciliation.getDc_flag().getValue());
                if (classifyBO != null) {
                    bankReconciliation.setOppositetype(classifyBO.getOppositetype());
                    bankReconciliation.setOppositeobjectid(classifyBO.getOppositeobjectid() == null ? null : classifyBO.getOppositeobjectid().toString());
                    bankReconciliation.setOppositeobjectname(classifyBO.getOppositeobjectname());
                    //银行对账单中存入对方银行账号的id（目前仅支持内部单位）
                    if (null != classifyBO.getOppositebankacctid()) {
                        bankReconciliation.setTo_acct(classifyBO.getOppositebankacctid());
                    }
                } else {
                    //未匹配则标记为其他类型
                    bankReconciliation.setOppositetype(OppositeType.Other.getValue());
                    bankReconciliation.setOppositeobjectid(null);
                    bankReconciliation.setOppositeobjectname(null);
                }
            }
            //对方开户行
            if (param.getString("to_acct_bank") != null) {
                bankReconciliation.setTo_acct_bank(param.getString("to_acct_bank"));
            }
            //对方开户行名
            if (param.getString("to_acct_bank_name") != null) {
                bankReconciliation.setTo_acct_bank_name(param.getString("to_acct_bank_name"));
            }
            //用途
            if (param.getString("use_name") != null) {
                bankReconciliation.setUse_name(param.getString("use_name"));
            }
            //摘要
            if (param.getString("remark") != null) {
                bankReconciliation.setRemark(param.getString("remark"));
            }
            //流水处理完结状态；0未完结；1已完结；2无需处理
            if (param.getShort("serialdealendstate") != null && (param.getShort("serialdealendstate") == 0
                    || param.getShort("serialdealendstate") == 1 || param.getShort("serialdealendstate") == 2)) {
                //mark by lichaor 20240408 和马良沟通过了，openApi进来的给什么参数就设置什么状态，这个例外处理，不去掉
                bankReconciliation.setSerialdealendstate(param.getShort("serialdealendstate"));
            }
            //业务关联状态。0：未关联，1：已关联
            if (param.getShort("associationstatus") != null && (param.getShort("associationstatus") == 0
                    || param.getShort("associationstatus") == 1)) {
                //mark by lichaor 20240408 和马良沟通过了，openApi进来的给什么参数就设置什么状态，这个例外处理，不去掉
                bankReconciliation.setAssociationstatus(param.getShort("associationstatus"));
            }
            //特征
            if (param.getObject("characterDef", LinkedHashMap.class) != null) {
                BizObject bizObject = Objectlizer.convert(param.getObject("characterDef", LinkedHashMap.class), CmpBankReconciliationCharacterDef.ENTITY_NAME);
                //设置特征状态
                //原来没有值的时候，新增
                if (bankReconciliation.getCharacterDef() == null) {
                    bizObject.setId(String.valueOf(ymsOidGenerator.nextId()));
                    bizObject.setEntityStatus(EntityStatus.Insert);
                } else {
                    bizObject.setEntityStatus(EntityStatus.Update);
                }
                bankReconciliation.put("characterDef", bizObject);
            }
            //财资统一对账码
            if (StringUtils.isNotEmpty(bankReconciliation.getSmartcheckno())){
                data.put("smartcheckno",bankReconciliation.getSmartcheckno());
            }
            //  ②  入参中增加”流水支持处理方式“，非必输，为空时默认为‘1’
            // isparsesmartcheckno 流水支持处理方式：0 仅关联 1 生单和关联；为空时默认为1，为'0'时，该笔流水不允许做生单操作，只允许与收付单据进行关联
            if (param.getShort("isparsesmartcheckno") != null && (param.getShort("isparsesmartcheckno") == 0
                    || param.getShort("isparsesmartcheckno") == 1)) {
                bankReconciliation.setIsparsesmartcheckno(BooleanUtils.toBoolean(param.getShort("isparsesmartcheckno")));
            } else {
                // 未解析出财资统一码，生成财资统一码并进行设置
                bankReconciliation.setIsparsesmartcheckno(BooleanUtils.toBoolean(ReconciliationSupportWayEnum.GENERATION_OR_ASSOCIATION.getValue()));
            }

            bankReconciliation.setEntityStatus(EntityStatus.Update);
            //疑重判断逻辑
            cmpCheckRepeatDataService.checkRepeatInfo(bankReconciliation);
            CommonSaveUtils.updateBankReconciliation(bankReconciliation);
            data.put("code",200);
            data.put("id",bankReconciliation.getId().toString());
            if(StringUtils.isNotEmpty(param.getString("unique_no"))){
                data.put("unique_no",param.getString("unique_no"));
            }
            dataList.add(data);
        }

        result.put("code",200);
        result.put("message",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054004BD", "更新成功") /* "更新成功" */);
        result.put("data",dataList);
        return result;
    }

    @Override
    public CtmJSONObject batchClaim(CtmJSONObject jsonObj) throws Exception {
        // 认领单状态 枚举值，0否，1是；0生成保存态的认领单，1生成认领单后自动提交
        String oprType = jsonObj.getString("autocommit");
        DetermineUtils.isTure(ValueUtils.isNotEmptyObj(oprType)).throwMessage(
                MessageUtils.getMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_227D61A205D00004","认领单状态不能为空！") /* "认领单状态不能为空！" */));

        if("0".equals(oprType)){
            oprType = "save";
        } else if("1".equals(oprType)){
            oprType = "saveAndCommit";
        } else{
            DetermineUtils.isTure(ValueUtils.isNotEmptyObj(oprType)).throwMessage(
                    MessageUtils.getMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_227D61D604C00006","不支持的认领单状态，请检查！") /* "不支持的认领单状态，请检查！" */));
        }

        CtmJSONArray jsonArray = jsonObj.getJSONArray("data");
        // 认领单数组
        List<CtmJSONObject> dataList = jsonArray.toJavaList(CtmJSONObject.class);

        // 失败原因列表
        Map<Integer, Object> errorMsgMap = new HashMap<>();
        List<Object> dealDataList = Collections.synchronizedList(new ArrayList<>());

        for (int m = 0; m < dataList.size(); m++) {
            CtmJSONObject claimObj = dataList.get(m);
            BillClaim billClaim = new BillClaim();
            StringBuilder errorMsg = new StringBuilder();
            // 表头入参校验
            if(ValueUtils.isEmpty(claimObj)){
                errorMsg.append(MessageUtils.getMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_227D61FC05D00007","认领参数不能为空！") /* "认领参数不能为空！" */));
            }
            // 认领日期
            String vouchdate = claimObj.getString("vouchdate");
            if(StringUtils.isEmpty(vouchdate)){
                errorMsg.append(MessageUtils.getMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_227D621805D00005","认领日期不能为空！") /* "认领日期不能为空！" */));
            }
            // 认领说明
            String remark = claimObj.getString("remark");
            if(StringUtils.isEmpty(remark)){
                errorMsg.append(MessageUtils.getMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540061A","认领说明不能为空！") /* "认领说明不能为空！" */));
            }
            // 银行流水信息子表
            CtmJSONArray detailList = claimObj.getJSONArray("data");
            if(ValueUtils.isEmpty(detailList)){
                errorMsg.append(MessageUtils.getMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_227D625605D00008","认领明细不能为空！") /* "认领明细不能为空！" */));
            }
            if(errorMsg.length() > 0){
                errorMsgMap.put(m+1, errorMsg.toString());
                continue;
            }

            /**
             * 1，根据银行流水id查询银行对账单
             * 2，判断银行对账单能够进行认领操作
             * 3，组装认领单数据
             * 4. 走规则链，保存提交认领单数据
             */
            List<Long> bankBillIds = new ArrayList<>();
            for (int i = 0; i < detailList.size(); i++) {
                CtmJSONObject detailObj = detailList.getJSONObject(i);
                // 银行流水ID
                Long bankbill = detailObj.getLong("id");
                bankBillIds.add(bankbill);
            }
            QuerySchema schema = QuerySchema.create().addSelect("*");
            QueryConditionGroup condition = new QueryConditionGroup();
            if (bankBillIds != null && !bankBillIds.isEmpty()) {
                condition.appendCondition(QueryCondition.name("id").in(bankBillIds));
            }
            schema.addCondition(condition);
            // 获取银行流水数据
            List<BankReconciliation> bankVOList = MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME, schema, null);

            // 检查银行流水数据有效性
            String bodyErrorMsg = checkBankReconciliationValid(bankVOList, bankBillIds);
            if(StringUtils.isNotEmpty(bodyErrorMsg)){
                errorMsgMap.put(m+1, bodyErrorMsg);
                continue;
            }
            // 组装收款认领数据（组装过程中检查认领表体数据有效性）
            String bodyErrorMsg2 = bankReconciliationToClaim(bankVOList, claimObj, billClaim);
            if(StringUtils.isNotEmpty(bodyErrorMsg2)){
                errorMsgMap.put(m+1, bodyErrorMsg2);
                continue;
            }

            RuleExecuteResult dealResult = executeBillClaimRule(billClaim,oprType);
            int dealResultCode = dealResult.getCode();
            Object dealData = dealResult.getData();
            if(dealResultCode != 0){
                List<Object> dealMsg = dealResult.getMessages();
                if(CollectionUtils.isEmpty(dealMsg)){
//                    errorMsg.append("第"+(m+1)+"条认领保存失败");
//                    messages.add(errorMsg.toString());
                    errorMsgMap.put(m+1, MessageUtils.getMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_227D616C04C00005","认领处理失败!") /* "认领处理失败!" */));
                }else{
//                    messages.addAll(dealMsg);
                    errorMsgMap.put(m+1, dealMsg);
                }
            }else{
                dealDataList.add(dealData);
            }
        }
        // 组装API返回信息
        CtmJSONObject result = constructBillClaimAPIResult(errorMsgMap,dealDataList);
        return result;

    }

    private CtmJSONObject constructBillClaimAPIResult(Map<Integer, Object> errorMsgMap, List<Object> dealDataList) {
        CtmJSONObject result = new CtmJSONObject();
        Map<String, Object> resultMap = new HashMap<>();

        List<Map<String, Object>> faillist = new ArrayList<>();
        if (errorMsgMap != null && errorMsgMap.size() > 0) {
            for (Map.Entry<Integer, Object> entry : errorMsgMap.entrySet()) {
                Integer dataIndex = entry.getKey();
                Object errorMsg = entry.getValue() == null ? MessageUtils.getMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_227D62C805D00006","认领处理失败") /* "认领处理失败" */) : entry.getValue();
                Map<String, Object> failItem = new HashMap<>();
                failItem.put("failmessage", String.format(MessageUtils.getMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_227D662E04C00006","第%s条数据处理失败：%s") /* "第%s条数据处理失败：%s" */) , dataIndex, errorMsg));
                faillist.add(failItem);
            }
        }

        List<Map<String, Object>> successlist = new ArrayList<>();
        if(dealDataList != null && dealDataList.size() > 0){
            SimpleDateFormat dateToStrFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            for (Object billData : dealDataList) {
                if (billData != null) {
                    Map<String, Object> headResult = new HashMap<>();
                    List<Object> itemlistResult = new ArrayList<>();
                    headResult.put("items", itemlistResult);
                    successlist.add(headResult);
                    if (billData instanceof BillClaim) {
                        BillClaim billClaim = (BillClaim) billData;
                        headResult.put("id", billClaim.get("id"));
                        headResult.put("code", billClaim.get("code"));
                        headResult.put("pubts", billClaim.get("pubts")==null?null:dateToStrFormat.format(billClaim.get("pubts")) );

                        List<BillClaimItem> items = billClaim.items();
                        if (items != null && items.size() > 0) {
                            for (int i = 0; i < items.size(); i++) {
                                Map<String, Object> item = new HashMap<>();
                                item.put("id", items.get(i).getString("id"));
                                item.put("bank_seq_id", items.get(i).getString("bankbill"));
                                itemlistResult.add(item);
                            }
                            headResult.put("items", itemlistResult);
                        }
                    } else if (billData instanceof HashMap) {
                        Map<String, Object> billClaim = (Map) billData;
                        headResult.put("id", billClaim.get("id"));
                        headResult.put("code", billClaim.get("code"));
                        headResult.put("pubts", billClaim.get("pubts")==null?null:dateToStrFormat.format(billClaim.get("pubts")) );
                        Object items = billClaim.get("items");
                        if (items instanceof List) {
                            List<Object> itemlist = (List) items;
                            if (items != null && itemlist.size() > 0) {
                                for (int i = 0; i < itemlist.size(); i++) {
                                    Object billItem = itemlist.get(i);
                                    Map<String, Object> item = new HashMap<>();
                                    if (billItem instanceof Map) {
                                        Map<String, Object> itemTemp = (Map) billItem;
                                        item.put("id", itemTemp.get("id"));
                                        item.put("bank_seq_id", itemTemp.get("bankbill"));
                                        itemlistResult.add(item);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        resultMap.put("successlist", successlist);
        resultMap.put("faillist", faillist);
        result.put("data", resultMap);
//        }
        return result;
    }
    private RuleExecuteResult executeBillClaimRule(BizObject billClaim, String optType) throws Exception {
        RuleExecuteResult ruleExecuteResult = new RuleExecuteResult();
        try {
            BillDataDto billDataDto = new BillDataDto("cmp_billclaimcard");
            billDataDto.setData(billClaim);
            billDataDto.setAction(ICmpConstant.SAVE);
            billDataDto.setBillnum(IBillNumConstant.CMP_BILLCLAIM_CARD);
            ApiImportCommandService apiImportCommandService = AppContext.getApplicationContext().getBean(ApiImportCommandService.class);
            Object result = apiImportCommandService.singleSave4Api(billDataDto);
            BizObject bizObject  = null;
            if (result instanceof BizObject) {
                bizObject = (BizObject) result;
                Map<String, Object> params = new HashMap<>(bizObject);
                CommonRuleUtils.cleanParent(params);
                bizObject = new BizObject(params);
                ruleExecuteResult.setData(bizObject);
            } else if (result instanceof HashMap) {
                CommonRuleUtils.cleanParent((Map<String, Object>) result);
                bizObject = new BizObject((Map<String, Object>) result);
            }

            if ("saveAndCommit".equals(optType)) {
                BillContext billContext = new BillContext("cmp_billclaimcard", "cmp.billclaim.BillClaim");
                billContext.setSupportBpm(true);
                billContext.setTenant(InvocationInfoProxy.getYxyTenantid());
                billContext.setName(MessageUtils.getMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400654","我的认领") /* "我的认领" */));
                billContext.setCardKey("cmp_billclaimcard");
                billContext.setSubid("CM");
                billContext.setBilltype("Voucher");
                billContext.setEntityCode("cmp_billclaimcard");
                billContext.setMddBoId("ctm-cmp.cmp_billclaimcard");

                billDataDto.setData( JsonUtils.toJSON(bizObject));
                Map<String, Object> params = new HashMap<>();
                params.put("param", billDataDto);
                // 提交
                billContext.setBillnum(IBillNumConstant.CMP_BILLCLAIM_CARD);
                billContext.setAction("submit");
                ruleExecuteResult = BillBiz.executeRule("submit",billContext,params);
                if(ruleExecuteResult.getCode() == 0 && ruleExecuteResult.getData() == null){
                    // 开启复核时，走提交没有返回data数据，但提交会更新数据（pubts），再重新查一次数据后返回
                    BizObject dbBillClaim = MetaDaoHelper.findById(BillClaim.ENTITY_NAME,bizObject.getId());
                    ruleExecuteResult.setData(dbBillClaim);
                }
            }else{
                ruleExecuteResult.setCode(0);
                ruleExecuteResult.setData(bizObject);
            }
        } catch (Exception e) {
            ruleExecuteResult.setCode(-1);
            ruleExecuteResult.setMessage(String.format(MessageUtils.getMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_227D668004C00004","执行认领单规则链报错：%s") /* "执行认领单规则链报错：%s" */), e.getMessage()));
//            throw e;
        }
        return ruleExecuteResult;

    }

    /**
     * 检查银行流水数据有效性
     * 1. API入参的银行流水均存在
     * 2. 各条银行流水，账户使用组织(accentity)、币种(currency)、入账类型(virtualentrytype)、收付方向(dc_flag) 需一致
     * 3. 根据认领类型判断表体认领金额是否有效：表体多行，只能是合并认领，合并认领不支持流水部分认领，整单认领、部分认领时，本次认领金额应小于待认领金额
     * @param bankVOList
     */
    private String checkBankReconciliationValid(List<BankReconciliation> bankVOList,List<Long> bankBillIds){
        StringBuilder errorMsg = new StringBuilder();
        // 1.  API入参的银行流水均存在
        if (CollectionUtils.isEmpty(bankVOList)) {
            errorMsg.append(String.format(MessageUtils.getMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_227D66A405D00008","以下银行交易流水ID未查询到数据：%s，请检查") /* "以下银行交易流水ID未查询到数据：%s，请检查" */),
                    bankBillIds.stream().map(String::valueOf).collect(Collectors.joining(", "))));
        }
        List<Long> dbBankBillIds = new ArrayList<>();
        for (BankReconciliation bankReconciliationVO : bankVOList) {
            dbBankBillIds.add(Long.valueOf(bankReconciliationVO.get("id").toString()));
        }
        bankBillIds.removeAll(dbBankBillIds);
        if (bankBillIds.size() > 0) {
            errorMsg.append(String.format(MessageUtils.getMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_227D66A405D00008","以下银行交易流水ID未查询到数据：%s，请检查") /* "以下银行交易流水ID未查询到数据：%s，请检查" */),
                    bankBillIds.stream().map(String::valueOf).collect(Collectors.joining(", "))));
        }
        if(errorMsg.length() > 0){
            return errorMsg.toString();
        }
        // 2. 检查表体各行银行流水数据一致性：账户使用组织、币种、入账类型、收付方向
        if (bankVOList.size() > 1) {
            // 获取第一条数据的字段值作为基准
            Map<String, Object> firstMap = bankVOList.get(0);
            String firstAccentity = (String) firstMap.get("accentity");
            String firstCurrency = (String) firstMap.get("currency");
            String firstVirtualEntryType = (String) firstMap.get("virtualentrytype");
            String firstDcFlag = firstMap.get("dc_flag").toString();

            // 检查每一条数据的一致性：账户使用组织、币种、入账类型、收付方向
            for (int i = 1; i < bankVOList.size(); i++) {
                Map<String, Object> currentMap = bankVOList.get(i);
                String currentAccentity = (String) currentMap.get("accentity");
                String currentCurrency = (String) currentMap.get("currency");
                String currentVirtualEntryType = (String) currentMap.get("virtualentrytype");
                Integer currentDcFlag = null;
                Object currentDcFlagObj = currentMap.get("dc_flag");
                if (currentDcFlagObj instanceof Integer) {
                    currentDcFlag = (Integer) currentDcFlagObj;
                } else if (currentDcFlagObj instanceof Number) {
                    currentDcFlag = ((Number) currentDcFlagObj).intValue();
                }

                // 检查账户使用组织
                if (!Objects.equals(firstAccentity, currentAccentity)) {
                    errorMsg.append(MessageUtils.getMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_227D645C04C00009","存在多条流水数据，账户使用组织不一致，保存失败，请检查！") /* "存在多条流水数据，账户使用组织不一致，保存失败，请检查！" */));
                    continue;
                }
                // 检查币种
                if (!Objects.equals(firstCurrency, currentCurrency)) {
                    errorMsg.append(MessageUtils.getMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_227D647C04C00006","存在多条流水数据，币种不一致，保存失败，请检查！") /* "存在多条流水数据，币种不一致，保存失败，请检查！" */));
                    continue;
                }
                // 检查入账类型
                if (!Objects.equals(firstVirtualEntryType, currentVirtualEntryType)) {
                    errorMsg.append(MessageUtils.getMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_227D649C05D00008","存在多条流水数据，入账类型不一致，保存失败，请检查！") /* "存在多条流水数据，入账类型不一致，保存失败，请检查！" */));
                    continue;
                }
                // 检查收付方向
                if (!Objects.equals(firstDcFlag, currentDcFlag)) {
                    errorMsg.append(MessageUtils.getMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_227D64C005D00008","存在多条流水数据，收付方向不一致，保存失败，请检查！") /* "存在多条流水数据，收付方向不一致，保存失败，请检查！" */));
                }
            }
        }
        return errorMsg.toString();
    }

    private String bankReconciliationToClaim(List<BankReconciliation> bankReconciliationList,CtmJSONObject claimObj, BillClaim billClaim) throws Exception {

        StringBuilder errorMsg = new StringBuilder();

        // 业务模式 默认为”普通结算“   1 普通结算  2 统收统支   3 结算中心代理
        billClaim.setBusinessmodel(new Short("1"));
        //认领日期
        billClaim.setVouchdate(DateUtils.strToDate(claimObj.getString("vouchdate")));
        // 认领说明
        billClaim.set("remark", claimObj.getString("remark"));
        // 财资统一对账码（智能对账勾兑码）	以传值为准，传值为空时，取流水上的财资统一对账码,多条流水时，如不一致 默认为空
        billClaim.set("smartcheckno", claimObj.getString("smartcheckno"));
        // 手工生单类型
        billClaim.set("manualgenertbilltype", claimObj.getString("manualgenertbilltype"));
        // 业务单元
        billClaim.set("orgcode", claimObj.getString("orgcode"));
        billClaim.set("org", claimObj.getString("org"));
        // 部门
        billClaim.set("deptcode", claimObj.getString("deptcode"));
        billClaim.set("dept", claimObj.getString("dept"));
        // 项目
        billClaim.set("projectcode", claimObj.getString("projectcode"));
        billClaim.set("project", claimObj.getString("project"));
        // 款项类型
        List<Map<String, Object>> quickTypeMap = QueryBaseDocUtils.getQuickTypeByCode(Collections.singletonList(claimObj.getString("quicktypecode")));

        if(quickTypeMap != null && quickTypeMap.size() > 0){
            billClaim.set("quicktype", MapUtils.getLong(quickTypeMap.get(0), "id"));
        }
        // 创建人 没传时，设置默认用户，
        String userId = InvocationInfoProxy.getUserid();
        //billClaim.set("userId", claimObj.getString("userId") == null ? InvocationInfoProxy.getUserid() : claimObj.getString("userId"));
        billClaim.set("userId", InvocationInfoProxy.getUserid());
        // 认领人  根据创建人查找对应的员工档案，查找成功进行自动赋值；如查找不到，则为空
//        String userId =  cmCommonService.getUserIdByYhtUserId(userYhtId);
        billClaim.set("claimstaff",InvocationInfoProxy.getUsername());
        // 业务凭据关联状态 默认为”否“
        billClaim.set("busvouchercorr_iscorr", Boolean.FALSE);
        // 统收统支标识
        billClaim.set("isincomeandexpenditure", Boolean.FALSE);

        //特征
        if (claimObj.getObject("billClaimCharacterDef", LinkedHashMap.class) != null) {
            BizObject bizObject = Objectlizer.convert(claimObj.getObject("billClaimCharacterDef", LinkedHashMap.class), BillClaimCharacterDef.ENTITY_NAME);
            bizObject.put("id", String.valueOf(IdCreator.getInstance().nextId()));
            //设置特征状态
            bizObject.setEntityStatus(EntityStatus.Insert);
            billClaim.put("billClaimCharacterDef", bizObject);
        }
        billClaim.set("_status", EntityStatus.Insert);


        CtmJSONArray detailList = claimObj.getJSONArray("data");

        // 认领类型（表体数据情况汇总）
        BillClaimType billClaimType = null;
        // 认领类型 :如有多条，则为”合并认领“；如有一条且交易金额=本次认领金额，则为”整单认领“;如有一条且交易金额>本次认领金额，则为”部分认领“
        if (detailList.size() > 1) {
            billClaimType = BillClaimType.Merge;
        }

        // 是否复核
        Boolean isRecheck = autoConfigService.getIsRecheck();
        // 是否需要复核，无论是否需要复核，保存时复核状态均为待复核，等提交或审批通过时，规则会自动更新该状态，区别是，需要复核人，需要赋值复核人、复核日期
        billClaim.set("recheckstatus", RecheckStatus.NotReviewed.getValue());
        if (isRecheck) {
            // 复核人；文本，默认当前用户(名)
            billClaim.set("recheckstaff", InvocationInfoProxy.getUsername());
            billClaim.set("recheckdate", new Date());
        }

        // 组装认领单表体数据
        contructBillClaimItems(bankReconciliationList, billClaim, detailList,errorMsg,billClaimType);

        return errorMsg.toString();
    }

    /**
     * 构造认领单表体数据，表体数据构造完成后，给表头取表体值的字段赋值
     * @param bankReconciliationList    银行流水（从数据库查询出来的数据）
     * @param billClaim     认领单数据（已赋值部分表头字段）
     * @param detailList    表体数据（API入参）
     * @param errorMsg  数据拼接过程中记录的错误信息
     * @param billClaimType 认领类型：整单认领、部分认领、合并认领
     */
    private void contructBillClaimItems(List<BankReconciliation> bankReconciliationList, BillClaim billClaim,CtmJSONArray detailList,
                                        StringBuilder errorMsg,  BillClaimType billClaimType){
        List<BillClaimItem> billClaimList = new ArrayList<>();
        // 将银行流水数据转为Map结构，key为银行流水ID，方便取数
        Map<String, BankReconciliation> dbBankReconciliationMap = bankReconciliationList.stream().collect(Collectors.toMap(bankReconciliation -> bankReconciliation.get("id").toString(), bankReconciliation -> bankReconciliation));

        // 实际结算主体 取流水上的核算会计主体字段（依赖与账户使用组织的，使用组织一致，核算组织也一致）
        String actualsettleaccentity = null;
        // 认领总金额
        BigDecimal totalClaimAmount = BigDecimal.ZERO;
        /**
         *  表体判断：
         *  1. 有效流水：流水是否发布=是；2、流水的待认领金额大于0 3、流水的账户使用组织不能为空；
         *  2. 合并认领、全部认领，本次认领金额应与待认领金额保持一致
         *  3. 部分或全部认领时，本次认领金额 <= 待认领金额根据
         */
        for (int i = 0; i < detailList.size(); i++) {
            CtmJSONObject detailObj = detailList.getJSONObject(i);
            // 银行流水ID
            String bankbill = detailObj.getString("id");

            BankReconciliation bankReconciliationMap = dbBankReconciliationMap.get(bankbill);

            // 流水是否发布
            Boolean ispublish = bankReconciliationMap.getIspublish();
            // 待认领金额大于0
            BigDecimal amounttobeclaimed = bankReconciliationMap.get("amounttobeclaimed") == null ?
                    new BigDecimal(0) : bankReconciliationMap.get("amounttobeclaimed");
            // 交易金额
            BigDecimal tran_amt = bankReconciliationMap.get("tran_amt") == null ?
                    new BigDecimal(0) : bankReconciliationMap.get("tran_amt");
            // 本次认领金额
            if(detailObj.getBigDecimal("claimamount") == null || detailObj.getBigDecimal("claimamount").compareTo(new BigDecimal(0)) == 0){
                errorMsg.append(String.format(MessageUtils.getMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_227D66FA05D00001","银行交易流水ID【%s】认领失败，请检查是否满足认领条件（本次认领金额不能为空或0）！") /* "银行交易流水ID【%s】认领失败，请检查是否满足认领条件（本次认领金额不能为空或0）！" */),
                        bankbill));
            }
            BigDecimal claimamount = detailObj.getBigDecimal("claimamount") == null ? new BigDecimal(0) : detailObj.getBigDecimal("claimamount");
            // 账户使用组织
            String accentityTemp = bankReconciliationMap.get("accentity") ;
            if (!ispublish || amounttobeclaimed == null || amounttobeclaimed.compareTo(new BigDecimal(0)) <= 0 || StringUtils.isEmpty(accentityTemp)) {
                errorMsg.append(String.format(MessageUtils.getMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_227D672804C00000","银行交易流水ID【%s】认领失败，请检查是否满足认领条件（账户使用组织不能为空、是否发布=是、待认领金额大于0）！") /* "银行交易流水ID【%s】认领失败，请检查是否满足认领条件（账户使用组织不能为空、是否发布=是、待认领金额大于0）！" */),
                        bankbill));
                break;
            }
            // 校验表体行数据： 本次认领金额应等于待认领金额
            if (claimamount.compareTo(amounttobeclaimed) > 0) {
                errorMsg.append(String.format(MessageUtils.getMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_227D677605D00001","银行交易流水ID【%s】认领失败，本次认领金额【%s】需小于等于待认领金额【%s】，保存失败，请检查!") /* "银行交易流水ID【%s】认领失败，本次认领金额【%s】需小于等于待认领金额【%s】，保存失败，请检查!" */),
                        bankbill, claimamount, amounttobeclaimed));
                break;
            }

            if(billClaimType == null){
                // 待判断是部分认领或全部认领
                if (claimamount.compareTo(amounttobeclaimed) < 0) {
                    billClaimType = BillClaimType.Part;
                }else if (claimamount.compareTo(amounttobeclaimed) == 0) {
                    billClaimType = BillClaimType.Whole;
                }
            }
            // 合并认领
            if(billClaimType.getValue() == BillClaimType.Merge.getValue()){
                if (claimamount.compareTo(tran_amt) != 0) {
                    errorMsg.append(String.format(MessageUtils.getMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_227D674C04C00004","银行交易流水ID【%s】认领失败，合并认领时本次认领金额应与交易金额一致，请检查!") /* "银行交易流水ID【%s】认领失败，合并认领时本次认领金额应与交易金额一致，请检查!" */), bankbill, claimamount, amounttobeclaimed));
                    break;
                }
            }


            BillClaimItem item = new BillClaimItem();
            item.set("bankbill", bankReconciliationMap.get("id"));
            // 账户使用组织
            item.set("accentity", bankReconciliationMap.get("accentity"));
            // 银行账户
            item.set("bankaccount", bankReconciliationMap.get("bankaccount"));
            // 交易日期
            item.set("tran_date", bankReconciliationMap.get("tran_date"));
            // 交易金额
            item.set("tran_amt", bankReconciliationMap.get("tran_amt"));
            // 币种
            item.set("currency", bankReconciliationMap.get("currency"));
            // 待认领金额
            item.set("amounttobeclaimed", bankReconciliationMap.get("amounttobeclaimed"));
            // 入账类型   流水上的挂账类型=01 正常入账，则赋值为“正常入账”；如流水上的挂账类型=02 挂账，则赋值为“03 冲挂账”
            Short entryType = bankReconciliationMap.getEntrytype();
            Short entryTypeTemp = 1;
            if(entryType ==2){
                entryTypeTemp = 3;
            }
            item.set("entrytype", entryTypeTemp);

            // 收付方向
            item.set("direction", bankReconciliationMap.get("dc_flag"));
            // 对方类型
            item.set("oppositetype", bankReconciliationMap.get("oppositetype"));
            // 对方单位
            item.set("oppositeobjectname", bankReconciliationMap.get("oppositeobjectname"));
            // 对方单位
            item.set("oppositeobjectid", bankReconciliationMap.get("oppositeobjectid"));
            // 对方账号
            item.set("to_acct_no", bankReconciliationMap.get("to_acct_no"));
            // 对方户名
            item.set("to_acct_name", bankReconciliationMap.get("to_acct_name"));
            // 对方开户行
            item.set("to_acct_bank", bankReconciliationMap.get("to_acct_bank"));
            // 对方开户行名
            item.set("to_acct_bank_name", bankReconciliationMap.get("to_acct_bank_name"));
            // 备注
            item.set("remark", bankReconciliationMap.get("remark"));
            // 认领金额
            item.set("claimamount", claimamount);
            // 认领合计金额
            item.set("totalamount", claimamount);

            // 银行流水字段值，用流水认领明细中转下
            // 内部账户是否记账
            item.put("isinneraccounting", bankReconciliationMap.getIsinneraccounting());
            // 归集内部账户
            item.put("impinneraccount", bankReconciliationMap.getImpinneraccount());
            // 提前入账
            item.put("earlyentry", bankReconciliationMap.getEarlyEntryFlag());

            totalClaimAmount = totalClaimAmount.add(claimamount);
            item.set("_status", EntityStatus.Insert);
            // 核算会计主体
            actualsettleaccentity = bankReconciliationMap.get("accentityRaw");
            billClaimList.add(item);
            billClaim.set("items", billClaimList);
        }
        if(billClaimList.size() == detailList.size()){
            // 处理表体字段赋值表头
            dealBodyValToHead(billClaim, actualsettleaccentity, totalClaimAmount, billClaimType);
        }

    }

    /**
     * 表头取表体数据
     * 1.部分字段：取表体第一行值
     * 2.部分字段 ：多条流水时，如不同，默认为空，若相同，才赋值
     * @param billClaim
     */
    private void dealBodyValToHead(BillClaim billClaim, String actualsettleaccentity,BigDecimal totalClaimAmount, BillClaimType billClaimType){


        List<BillClaimItem> billClaimItems =   billClaim.items();
        //
        // 认领类型
        billClaim.set("claimtype", billClaimType.getValue());
        // 实际结算主体：取流水上的核算会计主体字段（依赖与账户使用组织的，使用组织一致，核算组织也一致）
        billClaim.set("actualsettleaccentity", actualsettleaccentity);
        // 认领总金额  表体汇总表头
        billClaim.set("totalamount", totalClaimAmount);
        // 账户使用组织，从表体获取（表体一定一致），表体多流水校验了一致性
        billClaim.set("accentity", billClaimItems.get(0).getAccentity());
        // 实际认领组织	 同表体账户使用组织
        billClaim.set("actualclaimaccentiry", billClaimItems.get(0).getAccentity());

        // 币种 从表体获取（表体一定一致），表体多流水校验了一致性
        billClaim.set("currency", billClaimItems.get(0).getCurrency());
        // 收付方向  和表体一致   1支出，2收入
        billClaim.set("direction", billClaimItems.get(0).getDirection());
        // 入账类型
        billClaim.set("entrytype", billClaimItems.get(0).getEntrytype());

        // key 为认领单表头字段 value为认领单明细字段
        Map<String,String> headAndBodyFieldMap = new HashMap<>();
        // 以下字段统一逻辑：多条流水时，如不同，默认为空，如相同，表体值赋值表头
        // 实际认领组织账号 默认取”银行账号“
        headAndBodyFieldMap.put("claimaccount", "bankaccount");
        // （银行流水信息）账户所属组织   取流水上的所属组织
        headAndBodyFieldMap.put("affiliatedorgid", "accentity");
        // （银行流水信息）银行账号 默认取”银行账号“
        headAndBodyFieldMap.put("bankaccount", "bankaccount");
        // （银行流水信息）对方账号
        headAndBodyFieldMap.put("toaccountno", "to_acct_no");
        // （银行流水信息）对方户名
        headAndBodyFieldMap.put("toaccountname", "to_acct_name");
        // （银行流水信息）对方开户行
        headAndBodyFieldMap.put("toaccountbank", "to_acct_bank");
        // （银行流水信息）对方开户行名
        headAndBodyFieldMap.put("toaccountbankname", "to_acct_bank_name");
        // 内部账户是否记账
        headAndBodyFieldMap.put("isinneraccounting", "isinneraccounting");
        // 归集内部账户
        headAndBodyFieldMap.put("impinneraccount", "impinneraccount");
        // 提前入账
        headAndBodyFieldMap.put("earlyentry", "earlyentry");

        if(ValueUtils.isEmpty(billClaim.getSmartcheckno())){
            // 财资统一对账码（智能对账勾兑码）	以传值为准，传值为空时， 多条流水时，如不一致 默认为空
            headAndBodyFieldMap.put("smartcheckno", "smartcheckno");
        }
        Iterator<String> it = headAndBodyFieldMap.keySet().iterator();
        while(it.hasNext()){
            String headField = it.next();
            String bodyField = headAndBodyFieldMap.get(headField);
            Set<Object> vals =  getFieldVal(billClaimItems,bodyField);
            if(vals != null && vals.size() > 0){
                Object[] valsTemp =  vals.toArray();
                if(valsTemp != null && valsTemp.length == 1){
                    billClaim.set(headField,valsTemp[0]);
                }
            }
        }
    }

    /**
     * 获取表体指定字段列值集合（去重）
     * @param billClaimItems
     * @param bodyField
     * @return
     */
    private Set<Object> getFieldVal(List<BillClaimItem> billClaimItems, String bodyField){
        Set<Object> valSet = new HashSet<Object>();
        for (BillClaimItem billClaimItem : billClaimItems) {
            valSet.add(billClaimItem.get(bodyField));
        }
        return valSet;
    }

}
