package com.yonyoucloud.fi.cmp.voucher;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.yonyou.cloud.yts.YtsContext;
import com.yonyou.diwork.service.IApplicationService;
import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.dao.sql.SqlHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.ucf.mdd.ext.util.BillContextUtils;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyou.workbench.model.ApplicationVO;
import com.yonyou.yonbip.ctm.constant.CtmConstants;
import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.logger.business.CTMCMPBusinessLogService;
import com.yonyou.yonbip.ctm.workbench.ICtmApplicationService;
import com.yonyoucloud.fi.basecom.util.HttpTookitYts;
import com.yonyoucloud.fi.basecom.utils.HttpTookit;
import com.yonyoucloud.fi.cm.transferaccount.TransferAccount;
import com.yonyoucloud.fi.cmp.accrualsWithholding.AccrualsWithholding;
import com.yonyoucloud.fi.cmp.arap.ErrorCode;
import com.yonyoucloud.fi.cmp.arap.HttpLogEntity;
import com.yonyoucloud.fi.cmp.arap.StringConst;
import com.yonyoucloud.fi.cmp.batchtransferaccount.BatchTransferAccount;
import com.yonyoucloud.fi.cmp.cmpentity.EventSource;
import com.yonyoucloud.fi.cmp.cmpentity.EventType;
import com.yonyoucloud.fi.cmp.cmpentity.VoucherStatus;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.common.service.CmCommonService;
import com.yonyoucloud.fi.cmp.constant.*;
import com.yonyoucloud.fi.cmp.currencyexchange.CurrencyExchange;
import com.yonyoucloud.fi.cmp.event.sendEvent.ICmpSendEventService;
import com.yonyoucloud.fi.cmp.exchangegainloss.ExchangeGainLoss;
import com.yonyoucloud.fi.cmp.exchangegainloss.ExchangeGainLoss_b;
import com.yonyoucloud.fi.cmp.foreignpayment.ForeignPayment;
import com.yonyoucloud.fi.cmp.fundcollection.FundCollection;
import com.yonyoucloud.fi.cmp.fundcollection.FundCollection_b;
import com.yonyoucloud.fi.cmp.fundexpense.Fundexpense;
import com.yonyoucloud.fi.cmp.fundpayment.FundPayment;
import com.yonyoucloud.fi.cmp.fundpayment.FundPayment_b;
import com.yonyoucloud.fi.cmp.journalbill.JournalBill;
import com.yonyoucloud.fi.cmp.paybill.PayBill;
import com.yonyoucloud.fi.cmp.paybill.PayBillb;
import com.yonyoucloud.fi.cmp.paymargin.PayMargin;
import com.yonyoucloud.fi.cmp.receivebill.ReceiveBill;
import com.yonyoucloud.fi.cmp.receivebill.ReceiveBill_b;
import com.yonyoucloud.fi.cmp.receivemargin.ReceiveMargin;
import com.yonyoucloud.fi.cmp.salarypay.Salarypay;
import com.yonyoucloud.fi.cmp.util.QueryBaseDocUtils;
import com.yonyoucloud.fi.cmp.util.TypeUtils;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import com.yonyoucloud.fi.cmp.util.business.CmpCommonUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.imeta.biz.base.BizException;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.Duration;
import java.util.*;

import static com.yonyoucloud.fi.cmp.constant.IBillNumConstant.*;

/**
 * @ClassName CmpVoucherServiceImpl
 * @Description 现金管理凭证服务实现
 * @Author tongyd
 * @Date 2019/7/5 14:26
 * @Version 1.0
 **/
@Service
@Transactional(rollbackFor = RuntimeException.class)
@Slf4j
public class CmpVoucherServiceImpl implements CmpVoucherService {

    private static final String TRY_BATCH_DEL_URL = "/exchanger/batchdelvoucher/try";
    private static final String TRY_CREATE_VOUCH_URL = "/exchanger/input/try";
    public static final String FUND_PAYMENT_FULLNAME = "cmp.fundpayment.FundPayment";
    public static final String FUND_COLLECTION_FULLNAME = "cmp.fundcollection.FundCollection";


    @Resource
    private ICmpSendEventService cmpSendEventService;

    @Resource
    private CmCommonService commonService;

    @Resource
    private IApplicationService appService;

    @Resource
    private CTMCMPBusinessLogService ctmcmpBusinessLogService;

    private static final Cache<String, String> transTypeCodeCache = Caffeine.newBuilder()
            .initialCapacity(100)
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(1))
            .softValues()
            .build();

    private static final Cache<String, Date> enabledBeginDataCache = Caffeine.newBuilder()
            .initialCapacity(100)
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(1))
            .softValues()
            .build();


    /**
     * 查询交易类型档案
     *
     * @param transtype 交易类型id
     * @return
     * @throws Exception
     */
    private String getTransTypeCodeById(String transtype) throws Exception {

        return transTypeCodeCache.get(transtype, (k) -> {
            Map<String, Object> transtypeMap;
            try {
                transtypeMap = QueryBaseDocUtils.queryTransTypeById(k);
            } catch (Exception e) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100643"),"exception when query trans type by id " + k, e);
            }
            if (transtypeMap == null) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100644"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805CB", "根据交易类型主键查询交易类型档案异常！") /* "根据交易类型主键查询交易类型档案异常！" */);
            }
            return transtypeMap.get("code").toString();
        });
    }

//    /**
//     * 查询总账模块启用日期  - 支持跨组织核算，取消总账期初的校验
//     *
//     * @param accentity 资金组织id
//     * @return
//     * @throws Exception
//     */
//    private Date queryOrgPeriodBeginDateGL(String accentity) throws Exception {
//        return enabledBeginDataCache.get(accentity, (k) -> {
//            try {
//                return QueryBaseDocUtils.queryOrgPeriodBeginDate(k, ISystemCodeConstant.ORG_MODULE_GL);
//            } catch (Exception e) {
//                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100645"),"exception when queryOrgPeriodBeginDate by accentity " + k, e);
//            }
//        });
//    }

    @Override
    public void generateVoucher(BizObject bizObject) throws Exception {
        //已经生成凭证的单据不生成凭证
        Short voucherStatus = -1;
        if (bizObject.get("voucherstatus") != null) {
            voucherStatus = Short.valueOf(bizObject.get("voucherstatus").toString());
        }
        if (voucherStatus.equals(VoucherStatus.Created.getValue())
                || voucherStatus.equals(VoucherStatus.Received.getValue())) {
            return;
        }
        //来源费用的单据不在现金管理生成凭证
        Short srcItem = -1;
        if (bizObject.get("srcitem") != null) {
            srcItem = Short.valueOf(bizObject.get("srcitem").toString());
        }
        if (srcItem.equals(EventSource.SystemOut.getValue())) {
            return;
        }
        String systemCode = getSystemCode(bizObject);
        // 来源于应收和应付的单据不在现金生成凭证
        if ("fiar".equals(systemCode) || "fiap".equals(systemCode)) {
            return;
        }

        //总账模块启用会计期间开始日期大于单据日期不生成凭证 - 支持跨组织核算，取消总账期初的校验
//        Date enabledBeginData = queryOrgPeriodBeginDateGL(bizObject.get(IBussinessConstant.ACCENTITY));
//        if (enabledBeginData == null) {
//            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100646"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805CD","总账会计期间查询异常") /* "总账会计期间查询异常" */);
//        }
//        if (enabledBeginData.compareTo(bizObject.get("vouchdate")) > 0) {
//            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100647"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805CE","总账模块启用会计期间开始日期大于单据日期") /* "总账模块启用会计期间开始日期大于单据日期" */);
//        }

        CtmJSONObject billInfo = new CtmJSONObject();
        billInfo.put("reqid", UUID.randomUUID().toString());
        billInfo.put("action", "add");
        billInfo.put("billid", bizObject.getId().toString());
        billInfo.put("billno", bizObject.get("code"));
        String billTypeCode = getBillTypeCode(bizObject, systemCode);
        billInfo.put("billtypecode", billTypeCode);
        if (bizObject.get("tradetype") != null) {
            billInfo.put("tradetypecode", getTransTypeCodeById(bizObject.get("tradetype").toString()));
        }
        billInfo.put("systemcode", systemCode);
        billInfo.put("dessystemcode", "figl");
        billInfo.put("desbilltypecode", "C0");
        billInfo.put("async", "true"); // 异步生成凭证
        billInfo.put("bankcheckno", bizObject.get("bankcheckno"));
        if (srcItem.equals(EventSource.Cmpchase.getValue()) && "arap_paybill_PayBill".equals(billTypeCode)) {
            CtmJSONObject billPath = new CtmJSONObject();
            billPath.put("billtype", "voucher");
            billPath.put("billid", bizObject.getId());
            billPath.put("billno", "cmp_payment");
            billPath.put("serviceCode", "ficmp0009");
            billInfo.put("extbillpath", billPath.toString());
        } else if (srcItem.equals(EventSource.Cmpchase.getValue()) && "arap_receivebill_ReceiveBill".equals(billTypeCode)) {
            //因单据表体员工、业务员字段与凭证预制实体不匹配，所以进行代码赋值处理
            List<ReceiveBill_b> bList = bizObject.get("ReceiveBill_b");
            if (CollectionUtils.isNotEmpty(bList)) {
                for (ReceiveBill_b receiveBill_b : bList) {
                    receiveBill_b.set("employee", receiveBill_b.getOperator()); //员工
                    receiveBill_b.set("operator", receiveBill_b.getOperatornew()); //业务员
                }
            }
            CtmJSONObject billPath = new CtmJSONObject();
            billPath.put("billtype", "voucher");
            billPath.put("billid", bizObject.getId());
            billPath.put("billno", IBillNumConstant.RECEIVE_BILL);
            billPath.put("serviceCode", "ficmp0003");
            billInfo.put("extbillpath", billPath.toString());
        }
        Map<String, String> params = new HashMap<>();
        params.put("billinfo", billInfo.toString());
        if (ExchangeGainLoss.ENTITY_NAME.equals(billTypeCode)) {
            CtmJSONObject jsonObject = new CtmJSONObject();
            jsonObject.put(IBussinessConstant.ACCENTITY, bizObject.get(IBussinessConstant.ACCENTITY));
            jsonObject.put("balancezero", bizObject.get("balancezero"));
            jsonObject.put("code", bizObject.get("code"));
            jsonObject.put("vouchdate", bizObject.get("vouchdate"));
            jsonObject.put("tradetype", bizObject.get("tradetype"));
            jsonObject.put("creator", bizObject.get("creator"));
            jsonObject.put("createTime", bizObject.get("createTime"));
            jsonObject.put("createDate", bizObject.get("createDate"));
            jsonObject.put("modifier", bizObject.get("modifier"));
            jsonObject.put("modifyTime", bizObject.get("modifyTime"));
            jsonObject.put("modifyDate", bizObject.get("modifyDate"));
            jsonObject.put("id", bizObject.get("id"));
            jsonObject.put("pubts", bizObject.get("pubts"));
            jsonObject.put("srcitem", bizObject.get("srcitem"));
            jsonObject.put("billtype", bizObject.get("billtype"));
            jsonObject.put("exchangeRateType", bizObject.get("exchangeRateType"));
            List<Map<String, Object>> mapList = new ArrayList<Map<String, Object>>();
            List<ExchangeGainLoss_b> exchangeGainLoss_bList = bizObject.get("exchangeGainLoss_b");
            for (ExchangeGainLoss_b exchangeGainLoss_b : exchangeGainLoss_bList) {
                Map<String, Object> map = new HashMap<String, Object>();
                map.put("currency", exchangeGainLoss_b.getCurrency());
                map.put("bankaccount", exchangeGainLoss_b.getBankaccount());
                map.put("cashaccount", exchangeGainLoss_b.getCashaccount());
                map.put("localbalance", exchangeGainLoss_b.getLocalbalance());
                //map.put("oribalance",exchangeGainLoss_b.getOribalance());
                map.put("exchangerate", exchangeGainLoss_b.getExchangerate());
                map.put("exchangerateOps", exchangeGainLoss_b.getExchangerateOps());
                map.put("adjustlocalbalance", exchangeGainLoss_b.getAdjustlocalbalance());
                map.put("adjustbalance", exchangeGainLoss_b.getAdjustbalance());
                map.put("id", exchangeGainLoss_b.getId());
                map.put("mainid", exchangeGainLoss_b.getMainid());
                mapList.add(map);
            }
            jsonObject.put("exchangeGainLoss_b", mapList);
            //JsonFormatter formatter = new JsonFormatter(BizContext.getMetaRepository());
            //String json= formatter.toJson(bizObject,ExchangeGainLoss.ENTITY_NAME,true, SerializeAssoExpandType.IgnoreCompPKEqualFK).toString();
            //CtmJSONObject billData = CtmJSONObject.parseObject(json);
            params.put("billdata", CtmJSONObject.toJSONString(jsonObject));
        } else {
//            JsonConfig jsonConfig = new JsonConfig();
//            jsonConfig.registerJsonValueProcessor(java.util.Date.class, new JsonDateValueProcessor());
//            jsonConfig.registerJsonValueProcessor(java.sql.Timestamp.class, new JsonTimestampValueProcessor());
//            jsonConfig.registerJsonValueProcessor(java.sql.Date.class, new JsonDateValueProcessor());
            /**
             * begin yangjn 20210805 修改自由自定义项相关数据
             */
            generateVoucherAddDefines(bizObject, billTypeCode);
            /**
             *  end yangjn
             */
            params.put("billdata",  CtmJSONObject.toJSONString(bizObject));
        }
        log.error("generator voucher input param, yTenantId = {}, code = {}, params = {}",
                InvocationInfoProxy.getTenantid(), bizObject.get("code"), CtmJSONObject.toJSONString(params));
        Map<String, String> requestHeader = new HashMap<>();
        requestHeader.put("ytsEnable", "true");
        requestHeader.put("ytsMode", "tcc");
        String serverUrl = AppContext.getEnvConfig("fiotp.servername");
        String responseStr = HttpTookitYts.doPostWithJson(serverUrl + TRY_CREATE_VOUCH_URL, CtmJSONObject.toJSONString(params), requestHeader);
        CtmJSONObject resultJson = CtmJSONObject.parseObject(responseStr);
        int code = resultJson.getIntValue("code");
        if (code==1) {
            bizObject.set("voucherstatus", VoucherStatus.Received.getValue());
        } else {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100648"),resultJson.getJSONObject("result").getString("message"));
        }
    }

    @Override
    public CtmJSONObject generateRedVoucherWithResult(BizObject bizObject, String classifier) throws Exception {

        CtmJSONObject generateResult = new CtmJSONObject();
        //已经生成凭证的单据不生成凭证
        Short voucherStatus = -1;
        if (bizObject.get("voucherstatus") != null) {
            voucherStatus = Short.valueOf(bizObject.get("voucherstatus").toString());
        }
        if (voucherStatus.equals(VoucherStatus.Created.getValue())
                || voucherStatus.equals(VoucherStatus.Received.getValue())) {
            generateResult.put("dealSucceed", true);
            return generateResult;
        }
        //来源费用的单据不在现金管理生成凭证
        Short srcItem = -1;
        if (bizObject.get("srcitem") != null) {
            srcItem = Short.valueOf(bizObject.get("srcitem").toString());
        }
        if (srcItem.equals(EventSource.SystemOut.getValue())) {
            generateResult.put("dealSucceed", true);
            return generateResult;
        }
        String systemCode = "";
        if (!(ForeignPayment.ENTITY_NAME.equals(bizObject.get("_entityName")) || PayMargin.ENTITY_NAME.equals(bizObject.getEntityName()) || ReceiveMargin.ENTITY_NAME.equals(bizObject.getEntityName()))) {
            systemCode = getSystemCode(bizObject);
        }
        // 来源于应收和应付的单据不在现金生成凭证
        if ("fiar".equals(systemCode) || "fiap".equals(systemCode)) {
            generateResult.put("dealSucceed", true);
            return generateResult;
        }

        //总账模块启用会计期间开始日期大于单据日期不生成凭证 - 支持跨组织核算，取消总账期初的校验
//        Date enabledBeginData = queryOrgPeriodBeginDateGL(bizObject.get(IBussinessConstant.ACCENTITY));
//        if (enabledBeginData == null) {
//            generateResult.put("dealSucceed", true);
//            return generateResult;
//        }
//        if (enabledBeginData.compareTo(bizObject.get("vouchdate")) > 0) {
//            generateResult.put("dealSucceed", true);
//            return generateResult;
//        }

        CtmJSONObject billInfo = new CtmJSONObject();
        billInfo.put("reqid", UUID.randomUUID().toString());
        billInfo.put("action", "add");
        billInfo.put("billid", bizObject.getId().toString());
        billInfo.put("billno", bizObject.get("code"));

        String billTypeCode = "";
        if (!(ForeignPayment.ENTITY_NAME.equals(bizObject.get("_entityName")) || PayMargin.ENTITY_NAME.equals(bizObject.getEntityName()) || ReceiveMargin.ENTITY_NAME.equals(bizObject.getEntityName()))) {
            billTypeCode = getBillTypeCode(bizObject, systemCode);
        }
        billInfo.put("billtypecode", billTypeCode);
        billInfo.put("tradetypecode", getTransTypeCodeById(bizObject.get("tradetype").toString()));
        billInfo.put("systemcode", systemCode);
        billInfo.put("dessystemcode", "figl");
        billInfo.put("desbilltypecode", "C0");
        billInfo.put("async", "true"); // 异步生成凭证
        if (srcItem.equals(EventSource.Cmpchase.getValue()) && "FICM2".equals(billTypeCode)) {
            CtmJSONObject billPath = new CtmJSONObject();
            billPath.put("billtype", "voucher");
            billPath.put("billid", bizObject.getId());
            billPath.put("billno", "cmp_payment");
            billPath.put("serviceCode", "ficmp0009");
            billInfo.put("extbillpath", billPath.toString());
        } else if (srcItem.equals(EventSource.Cmpchase.getValue()) && "FICM1".equals(billTypeCode)) {
            //因单据表体员工、业务员字段与凭证预制实体不匹配，所以进行代码赋值处理
            //因单据表体员工、业务员字段与凭证预制实体不匹配，所以进行代码赋值处理
            List<ReceiveBill_b> bList = bizObject.get("ReceiveBill_b");
            if (CollectionUtils.isNotEmpty(bList)) {
                for (ReceiveBill_b receiveBill_b : bList) {
                    receiveBill_b.set("employee", receiveBill_b.getOperator()); //员工
                    receiveBill_b.set("operator", receiveBill_b.getOperatornew()); //业务员
                }
            }
            CtmJSONObject billPath = new CtmJSONObject();
            billPath.put("billtype", "voucher");
            billPath.put("billid", bizObject.getId());
            billPath.put("billno", IBillNumConstant.RECEIVE_BILL);
            billPath.put("serviceCode", "ficmp0003");
            billInfo.put("extbillpath", billPath.toString());
        } else if (srcItem.equals(EventSource.Cmpchase.getValue()) && Salarypay.ENTITY_NAME.equals(billTypeCode)) {
            CtmJSONObject billPath = new CtmJSONObject();
            billPath.put("billtype", "voucher");
            billPath.put("billid", bizObject.getId());
            billPath.put("billno", "cmp_salarypay");
            billPath.put("serviceCode", IServicecodeConstant.SALARYPAY);
            billInfo.put("extbillpath", billPath.toString());
        } else if (srcItem.equals(EventSource.Cmpchase.getValue()) && FundCollection.ENTITY_NAME.equals(billTypeCode)) {
            CtmJSONObject billPath = new CtmJSONObject();
            billPath.put("billtype", "voucher");
            billPath.put("billid", bizObject.getId());
            billPath.put("billno", FUND_COLLECTION);
            billPath.put("serviceCode", "ficmp0024");
            billInfo.put("extbillpath", billPath.toString());
        } else if (srcItem.equals(EventSource.Cmpchase.getValue()) && FundPayment.ENTITY_NAME.equals(billTypeCode)) {
            CtmJSONObject billPath = new CtmJSONObject();
            billPath.put("billtype", "voucher");
            billPath.put("billid", bizObject.getId());
            billPath.put("billno", FUND_PAYMENT);
            billPath.put("serviceCode", "ficmp0026");
            billInfo.put("extbillpath", billPath.toString());
        } else if (srcItem.equals(EventSource.Cmpchase.getValue()) && JournalBill.ENTITY_NAME.equals(billTypeCode)) {
            CtmJSONObject billPath = new CtmJSONObject();
            billPath.put("billtype", "voucher");
            billPath.put("billid", bizObject.getId());
            billPath.put("billno", JOURNAL_BILL);
            billPath.put("serviceCode", ICmpConstant.JOURNAL_BILL_SERVICE_CODE);
            billInfo.put("extbillpath", billPath.toString());
        }
        Map<String, String> params = new HashMap<>();
        params.put("billinfo",CtmJSONObject.toJSONString(billInfo));
        if (!CmpCommonUtil.getNewFiFlag()) {
            // 设置单据附件数: Sun GuoCai 2023/7/3
            commonService.getFilesCount(bizObject);
        }
        if (ExchangeGainLoss.ENTITY_NAME.equals(billTypeCode)) {
            CtmJSONObject jsonObject = new CtmJSONObject();
            jsonObject.put(IBussinessConstant.ACCENTITY, bizObject.get(IBussinessConstant.ACCENTITY));
            jsonObject.put("balancezero", bizObject.get("balancezero"));
            jsonObject.put("code", bizObject.get("code"));
            jsonObject.put("vouchdate", bizObject.get("vouchdate"));
            jsonObject.put("tradetype", bizObject.get("tradetype"));
            jsonObject.put("creator", bizObject.get("creator"));
            jsonObject.put("createTime", bizObject.get("createTime"));
            jsonObject.put("createDate", bizObject.get("createDate"));
            jsonObject.put("modifier", bizObject.get("modifier"));
            jsonObject.put("modifyTime", bizObject.get("modifyTime"));
            jsonObject.put("modifyDate", bizObject.get("modifyDate"));
            jsonObject.put("id", bizObject.get("id"));
            jsonObject.put("pubts", bizObject.get("pubts"));
            jsonObject.put("srcitem", bizObject.get("srcitem"));
            jsonObject.put("billtype", bizObject.get("billtype"));
            jsonObject.put("natCurrency", bizObject.get("natCurrency"));
            jsonObject.put("exchangeRateType", bizObject.get("exchangeRateType"));
            jsonObject.put("filesCount", bizObject.get("filesCount"));
            List<Map<String, Object>> mapList = new ArrayList<Map<String, Object>>();
            List<ExchangeGainLoss_b> exchangeGainLoss_bList = bizObject.get("exchangeGainLoss_b");
            for (ExchangeGainLoss_b exchangeGainLoss_b : exchangeGainLoss_bList) {
                Map<String, Object> map = new HashMap<String, Object>();
                map.put("currency", exchangeGainLoss_b.getCurrency());
                map.put("bankaccount", exchangeGainLoss_b.getBankaccount());
                map.put("cashaccount", exchangeGainLoss_b.getCashaccount());
                map.put("localbalance", exchangeGainLoss_b.getLocalbalance());
                //map.put("oribalance",exchangeGainLoss_b.getOribalance());
                map.put("exchangerate", exchangeGainLoss_b.getExchangerate());
                map.put("exchangerateOps", exchangeGainLoss_b.getExchangerateOps());
                map.put("adjustlocalbalance", exchangeGainLoss_b.getAdjustlocalbalance());
                map.put("adjustbalance", exchangeGainLoss_b.getAdjustbalance());
                map.put("id", exchangeGainLoss_b.getId());
                map.put("mainid", exchangeGainLoss_b.getMainid());
                mapList.add(map);
            }
            jsonObject.put("exchangeGainLoss_b", mapList);
            params.put("billdata", CtmJSONObject.toJSONString(jsonObject));
        } else {
//            JsonConfig jsonConfig = new JsonConfig();
//            jsonConfig.registerJsonValueProcessor(java.util.Date.class, new JsonDateValueProcessor());
//            jsonConfig.registerJsonValueProcessor(java.sql.Timestamp.class, new JsonTimestampValueProcessor());
//            jsonConfig.registerJsonValueProcessor(java.sql.Date.class, new JsonDateValueProcessor());
            /**
             * begin yangjn 20210805 修改自由自定义项相关数据
             */
            generateVoucherAddDefines(bizObject, billTypeCode);
            /**
             *  end yangjn
             */
            params.put("billdata",  CtmJSONObject.toJSONString(bizObject));
        }
        //财务新架构标识
        if (CmpCommonUtil.getNewFiFlag()) {
            boolean enableEVNT = AppContext.getBean(ICtmApplicationService.class).isEnableApp(CtmConstants.YONBIP_FI_EEAC_EVNT_APP_CODE);
            if (!enableEVNT) {
                log.error("客户环境未安装事项中台服务");
                bizObject.set("voucherstatus", VoucherStatus.NONCreate.getValue());
                generateResult.put("dealSucceed", true);
                generateResult.put("genVoucher", false);
                generateResult.put("code", 1);
            } else {
                try {
                    CtmJSONObject billClue = new CtmJSONObject();
                    billClue.put("classifier", classifier);
                    billClue.put("srcBusiId", bizObject.getId().toString());
                    cmpSendEventService.sendSimpleEvent(bizObject, billClue);
                    bizObject.set("voucherstatus", VoucherStatus.POSTING.getValue());
                    generateResult.put("dealSucceed", true);

                } catch (Exception e) {
                    generateResult.put("dealSucceed", false);
                    generateResult.put("message", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00006", "生成凭证失败！请检查数据！") /* "生成凭证失败！请检查数据！" */);
                    log.error("send event generate voucher fail!, bizObject = {}, e ={}", bizObject, e.getMessage());
                }
            }
        } else {
            log.error("generator voucher input param, yTenantId = {}, code = {}, params = {}",
                    InvocationInfoProxy.getTenantid(), bizObject.get("code"), CtmJSONObject.toJSONString(params));

            // 设置单据附件数: Sun GuoCai 2023/7/3
            commonService.getFilesCount(bizObject);

            Map<String, String> requestHeader = new HashMap<>();
            requestHeader.put("ytsEnable", "true");
            requestHeader.put("ytsMode", "tcc");
            String serverUrl = AppContext.getEnvConfig("fiotp.servername");
            String responseStr = HttpTookitYts.doPostWithJson(serverUrl + TRY_CREATE_VOUCH_URL, CtmJSONObject.toJSONString(params), requestHeader);
            CtmJSONObject resultJson = CtmJSONObject.parseObject(responseStr);
            //同步收付款单 单据  在收付那边  某些场景  不需要生成凭证
            boolean genVoucher = resultJson.get("genVoucher") == null ? Boolean.TRUE : resultJson.getBoolean("genVoucher");
            generateResult.put("genVoucher", genVoucher);
            int code = resultJson.getIntValue("code");
            if (1 == code) {
                bizObject.set("voucherstatus", VoucherStatus.Received.getValue());
                generateResult.put("dealSucceed", true);
            } else if (0 == code){
                generateResult.put("dealSucceed", false);
                generateResult.put("message", resultJson.getString("message"));
            }
        }
        return generateResult;
    }

    @Override
    public CtmJSONObject generateVoucherWithResult(BizObject bizObject) throws Exception {
        return generateRedVoucherWithResult(bizObject, null);
    }

    /**
     * 生成凭证-适配分布式事务
     *
     * @param bizObject
     * @return
     * @throws Exception
     */
    @Override
    public CtmJSONObject generateVoucherWithResultTry(BizObject bizObject) throws Exception {
        CtmJSONObject generateResult = new CtmJSONObject();
        //已经生成凭证的单据不生成凭证
        Short voucherStatus = -1;
        if (bizObject.get("voucherstatus") != null) {
            voucherStatus = Short.valueOf(bizObject.get("voucherstatus").toString());
        }
        if (voucherStatus.equals(VoucherStatus.Created.getValue())
                || voucherStatus.equals(VoucherStatus.Received.getValue())) {
            generateResult.put("dealSucceed", true);
            generateResult.put("genVoucher", false);
            generateResult.put("code", 0);
            return generateResult;
        }
        //来源费用的单据不在现金管理生成凭证
        Short srcItem = -1;
        if (bizObject.get("srcitem") != null) {
            srcItem = Short.valueOf(bizObject.get("srcitem").toString());
        }
        if (srcItem == EventSource.ManualImport.getValue()) {
            srcItem = EventSource.Cmpchase.getValue();
            bizObject.set("srcitem", srcItem);
        }
        if (srcItem.equals(EventSource.SystemOut.getValue())) {
            generateResult.put("dealSucceed", true);
            return generateResult;
        }
        String systemCode = getSystemCode(bizObject);
        // 来源于应收和应付的单据不在现金生成凭证
        if ("fiar".equals(systemCode) || "fiap".equals(systemCode)) {
            generateResult.put("dealSucceed", true);
            return generateResult;
        }
        CtmJSONObject billInfo = new CtmJSONObject();
        billInfo.put("reqid", UUID.randomUUID().toString());
        billInfo.put("action", "add");
        billInfo.put("billid", bizObject.getId().toString());
        billInfo.put("billno", bizObject.get("code"));
        String billTypeCode = getBillTypeCode(bizObject, systemCode);
        billInfo.put("billtypecode", billTypeCode);
        billInfo.put("tradetypecode", getTransTypeCodeById(bizObject.get("tradetype").toString()));
        billInfo.put("systemcode", systemCode);
        billInfo.put("dessystemcode", "figl");
        billInfo.put("desbilltypecode", "C0");
        billInfo.put("async", "true"); // 异步生成凭证
        if (srcItem.equals(EventSource.Cmpchase.getValue()) && "FICM2".equals(billTypeCode)) {
            CtmJSONObject billPath = new CtmJSONObject();
            billPath.put("billtype", "voucher");
            billPath.put("billid", bizObject.getId());
            billPath.put("billno", "cmp_payment");
            billPath.put("serviceCode", "ficmp0009");
            billInfo.put("extbillpath", billPath.toString());
        } else if (srcItem.equals(EventSource.Cmpchase.getValue()) && "FICM1".equals(billTypeCode)) {
            //因单据表体员工、业务员字段与凭证预制实体不匹配，所以进行代码赋值处理
            //因单据表体员工、业务员字段与凭证预制实体不匹配，所以进行代码赋值处理
            List<ReceiveBill_b> bList = bizObject.get("ReceiveBill_b");
            if (CollectionUtils.isNotEmpty(bList)) {
                for (ReceiveBill_b receiveBill_b : bList) {
                    receiveBill_b.set("employee", receiveBill_b.getOperator()); //员工
                    receiveBill_b.set("operator", receiveBill_b.getOperatornew()); //业务员
                }
            }
            CtmJSONObject billPath = new CtmJSONObject();
            billPath.put("billtype", "voucher");
            billPath.put("billid", bizObject.getId());
            billPath.put("billno", IBillNumConstant.RECEIVE_BILL);
            billPath.put("serviceCode", "ficmp0003");
            billInfo.put("extbillpath", billPath.toString());
        } else if (srcItem.equals(EventSource.Cmpchase.getValue()) && Salarypay.ENTITY_NAME.equals(billTypeCode)) {
            CtmJSONObject billPath = new CtmJSONObject();
            billPath.put("billtype", "voucher");
            billPath.put("billid", bizObject.getId());
            billPath.put("billno", "cmp_salarypay");
            billPath.put("serviceCode", IServicecodeConstant.SALARYPAY);
            billInfo.put("extbillpath", billPath.toString());
        } else if (srcItem.equals(EventSource.Cmpchase.getValue()) &&  FUND_COLLECTION.equals(billTypeCode)) {
            CtmJSONObject billPath = new CtmJSONObject();
            billPath.put("billtype", "voucher");
            billPath.put("billid", bizObject.getId());
            billPath.put("billno", FUND_COLLECTION);
            billPath.put("serviceCode", "ficmp0024");
            billInfo.put("extbillpath", billPath.toString());
        } else if (srcItem.equals(EventSource.Cmpchase.getValue()) && FUND_PAYMENT.equals(billTypeCode)) {
            CtmJSONObject billPath = new CtmJSONObject();
            billPath.put("billtype", "voucher");
            billPath.put("billid", bizObject.getId());
            billPath.put("billno", FUND_PAYMENT);
            billPath.put("serviceCode", "ficmp0026");
            billInfo.put("extbillpath", billPath.toString());
        }else if (srcItem.equals(EventSource.StwbSettlement.getValue()) &&  FUND_COLLECTION.equals(billTypeCode)) {
            CtmJSONObject billPath = new CtmJSONObject();
            billPath.put("billtype", "voucher");
            billPath.put("billid", bizObject.getId());
            billPath.put("billno", FUND_COLLECTION);
            billPath.put("serviceCode", "ficmp0024");
            billInfo.put("extbillpath", billPath.toString());
        } else if (srcItem.equals(EventSource.StwbSettlement.getValue()) && FUND_PAYMENT.equals(billTypeCode)) {
            CtmJSONObject billPath = new CtmJSONObject();
            billPath.put("billtype", "voucher");
            billPath.put("billid", bizObject.getId());
            billPath.put("billno", FUND_PAYMENT);
            billPath.put("serviceCode", "ficmp0026");
            billInfo.put("extbillpath", billPath.toString());
        }else if(srcItem.equals(EventSource.StwbSettlement.getValue()) && "fundexpense".equals(billTypeCode)){
            CtmJSONObject billPath = new CtmJSONObject();
            billPath.put("billtype", "voucher");
            billPath.put("billid", bizObject.getId());
            billPath.put("billno", "fundexpense");
            billPath.put("serviceCode", "fundexpense");
            billInfo.put("extbillpath", billPath.toString());
        }
        Map<String, String> params = new HashMap<>();
        params.put("billinfo", billInfo.toString());
        if (!CmpCommonUtil.getNewFiFlag()) {
            // 设置单据附件数: Sun GuoCai 2023/7/3
            commonService.getFilesCount(bizObject);
        }
        if (ExchangeGainLoss.ENTITY_NAME.equals(billTypeCode)) {
            CtmJSONObject jsonObject = new CtmJSONObject();
            jsonObject.put(IBussinessConstant.ACCENTITY, bizObject.get(IBussinessConstant.ACCENTITY));
            jsonObject.put("balancezero", bizObject.get("balancezero"));
            jsonObject.put("code", bizObject.get("code"));
            jsonObject.put("vouchdate", bizObject.get("vouchdate"));
            jsonObject.put("tradetype", bizObject.get("tradetype"));
            jsonObject.put("creator", bizObject.get("creator"));
            jsonObject.put("createTime", bizObject.get("createTime"));
            jsonObject.put("createDate", bizObject.get("createDate"));
            jsonObject.put("modifier", bizObject.get("modifier"));
            jsonObject.put("modifyTime", bizObject.get("modifyTime"));
            jsonObject.put("modifyDate", bizObject.get("modifyDate"));
            jsonObject.put("id", bizObject.get("id"));
            jsonObject.put("pubts", bizObject.get("pubts"));
            jsonObject.put("srcitem", bizObject.get("srcitem"));
            jsonObject.put("billtype", bizObject.get("billtype"));
            jsonObject.put("natCurrency", bizObject.get("natCurrency"));
            jsonObject.put("exchangeRateType", bizObject.get("exchangeRateType"));
            jsonObject.put("filesCount", bizObject.get("filesCount"));
            List<Map<String, Object>> mapList = new ArrayList<Map<String, Object>>();
            List<ExchangeGainLoss_b> exchangeGainLoss_bList = bizObject.get("exchangeGainLoss_b");
            for (ExchangeGainLoss_b exchangeGainLoss_b : exchangeGainLoss_bList) {
                Map<String, Object> map = new HashMap<String, Object>();
                map.put("currency", exchangeGainLoss_b.getCurrency());
                map.put("bankaccount", exchangeGainLoss_b.getBankaccount());
                map.put("cashaccount", exchangeGainLoss_b.getCashaccount());
                map.put("localbalance", exchangeGainLoss_b.getLocalbalance());
                //map.put("oribalance",exchangeGainLoss_b.getOribalance());
                map.put("exchangerate", exchangeGainLoss_b.getExchangerate());
                map.put("exchangerateOps", exchangeGainLoss_b.getExchangerateOps());
                map.put("adjustlocalbalance", exchangeGainLoss_b.getAdjustlocalbalance());
                map.put("adjustbalance", exchangeGainLoss_b.getAdjustbalance());
                map.put("id", exchangeGainLoss_b.getId());
                map.put("mainid", exchangeGainLoss_b.getMainid());
                mapList.add(map);
            }
            jsonObject.put("exchangeGainLoss_b", mapList);
            params.put("billdata", CtmJSONObject.toJSONString(jsonObject));
        } else {
//            JsonConfig jsonConfig = new JsonConfig();
//            jsonConfig.registerJsonValueProcessor(java.util.Date.class, new JsonDateValueProcessor());
//            jsonConfig.registerJsonValueProcessor(java.sql.Timestamp.class, new JsonTimestampValueProcessor());
//            jsonConfig.registerJsonValueProcessor(java.sql.Date.class, new JsonDateValueProcessor());
            /**
             * begin yangjn 20210805 修改自由自定义项相关数据
             */
            generateVoucherAddDefines(bizObject, billTypeCode);
            /**
             *  end yangjn
             */
            params.put("billdata",  CtmJSONObject.toJSONString(bizObject));
        }
        //财务新架构标识
        if (CmpCommonUtil.getNewFiFlag()) {
            boolean enableEVNT = AppContext.getBean(ICtmApplicationService.class).isEnableApp(CtmConstants.YONBIP_FI_EEAC_EVNT_APP_CODE);
            if (!enableEVNT) {
                log.error("客户环境未安装事项中台服务");
                bizObject.set("voucherstatus", VoucherStatus.NONCreate.getValue());
                generateResult.put("dealSucceed", false);
                generateResult.put("genVoucher", false);
                generateResult.put("code", 1);
            } else {
                try {
                    CtmJSONObject billClue = new CtmJSONObject();
                    billClue.put("classifier", null);
                    billClue.put("srcBusiId", bizObject.getId().toString());
                    cmpSendEventService.sendSimpleEvent(bizObject, billClue);
                    bizObject.set("voucherstatus", VoucherStatus.POSTING.getValue());
                    generateResult.put("dealSucceed", true);
                    generateResult.put("code", 2);
                } catch (Exception e) {
                    generateResult.put("dealSucceed", false);
                    generateResult.put("code", 0);
                    generateResult.put("message", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805CC", "生成凭证失败！请检查数据！") /* "生成凭证失败！请检查数据！" */);
                    log.error("send event generate voucher fail!, bizObject = {}, e ={}", bizObject, e.getMessage());
                }
            }
        } else {
            Map<String, String> requestHeader = new HashMap<>();
            requestHeader.put("ytsEnable", "true");
            requestHeader.put("ytsMode", "tcc");
            String serverUrl = AppContext.getEnvConfig("fiotp.servername");
            log.error("send voucher data, data = {}", CtmJSONObject.toJSONString(params));
            String responseStr = HttpTookitYts.doPostWithJson(serverUrl + TRY_CREATE_VOUCH_URL, CtmJSONObject.toJSONString(params), requestHeader);
            CtmJSONObject resultJson = CtmJSONObject.parseObject(responseStr);
            //同步收付款单 单据  在收付那边  某些场景  不需要生成凭证
            boolean genVoucher = resultJson.get("genVoucher") == null ? Boolean.TRUE : resultJson.getBoolean("genVoucher");
            generateResult.put("genVoucher", genVoucher);
            int code = resultJson.getIntValue("code");
            if (0 == code) {
                generateResult.put("dealSucceed", false);
                generateResult.put("message", ValueUtils.isNotEmptyObj(resultJson.getJSONObject("result")) ?
                        resultJson.getJSONObject("result").getString("message") : resultJson.get("message"));
            } else if (1 == code) {
                bizObject.set("voucherstatus", VoucherStatus.Received.getValue());
                generateResult.put("dealSucceed", true);
            }
            generateResult.put("code", code);
        }
        return generateResult;
    }

    @Override
    public CtmJSONObject generateVoucherWithPay(BizObject bizObject) throws Exception {
        CtmJSONObject generateResult = new CtmJSONObject();
        //已经生成凭证的单据不生成凭证
        Short voucherStatus = -1;
        if (bizObject.get("voucherstatus") != null) {
            voucherStatus = Short.valueOf(bizObject.get("voucherstatus").toString());
        }
        if (voucherStatus.equals(VoucherStatus.Created.getValue())
                || voucherStatus.equals(VoucherStatus.Received.getValue())) {
            generateResult.put("dealSucceed", true);
            return generateResult;
        }
        //来源费用的单据不在现金管理生成凭证
        Short srcItem = -1;
        if (bizObject.get("srcitem") != null) {
            srcItem = Short.valueOf(bizObject.get("srcitem").toString());
        }
        if (srcItem.equals(EventSource.SystemOut.getValue())) {
            generateResult.put("dealSucceed", true);
            return generateResult;
        }
        String systemCode = getSystemCode(bizObject);
        // 来源于应收和应付的单据不在现金生成凭证
        if ("fiar".equals(systemCode) || "fiap".equals(systemCode)) {
            generateResult.put("dealSucceed", true);
            return generateResult;
        }

        //总账模块启用会计期间开始日期大于单据日期不生成凭证  - 支持跨组织核算，取消总账期初的校验
//        Date enabledBeginData = queryOrgPeriodBeginDateGL(bizObject.get(IBussinessConstant.ACCENTITY));
//        if (enabledBeginData == null) {
//            generateResult.put("dealSucceed", true);
//            return generateResult;
//        }
//        if (enabledBeginData.compareTo(bizObject.get("vouchdate")) > 0) {
//            generateResult.put("dealSucceed", true);
//            return generateResult;
//        }
        if (!CmpCommonUtil.getNewFiFlag()) {
            // 设置单据附件数: Sun GuoCai 2023/7/3
            commonService.getFilesCount(bizObject);
        }
        CtmJSONObject billInfo = new CtmJSONObject();
        billInfo.put("reqid", UUID.randomUUID().toString());
        billInfo.put("action", "add");
        billInfo.put("billid", bizObject.getId().toString());
        billInfo.put("billno", bizObject.get("code"));
        String billTypeCode = getBillTypeCode(bizObject, systemCode);
        billInfo.put("billtypecode", billTypeCode);
        billInfo.put("tradetypecode", getTransTypeCodeById(bizObject.get("tradetype").toString()));
        billInfo.put("systemcode", systemCode);
        billInfo.put("dessystemcode", "figl");
        billInfo.put("desbilltypecode", "C0");
        billInfo.put("async", "true"); // 异步生成凭证
        if (srcItem.equals(EventSource.Cmpchase.getValue()) && "arap_paybill_PayBill".equals(billTypeCode)) {
            CtmJSONObject billPath = new CtmJSONObject();
            billPath.put("billtype", "voucher");
            billPath.put("billid", bizObject.getId());
            billPath.put("billno", "cmp_payment");
            billPath.put("serviceCode", "ficmp0009");
            billInfo.put("extbillpath", billPath.toString());
        } else if (srcItem.equals(EventSource.Cmpchase.getValue()) && "arap_receivebill_ReceiveBill".equals(billTypeCode)) {
            CtmJSONObject billPath = new CtmJSONObject();
            billPath.put("billtype", "voucher");
            billPath.put("billid", bizObject.getId());
            billPath.put("billno", IBillNumConstant.RECEIVE_BILL);
            billPath.put("serviceCode", "ficmp0003");
            billInfo.put("extbillpath", billPath.toString());
        } else if (srcItem.equals(EventSource.Cmpchase.getValue()) && Salarypay.ENTITY_NAME.equals(billTypeCode)) {
            CtmJSONObject billPath = new CtmJSONObject();
            billPath.put("billtype", "voucher");
            billPath.put("billid", bizObject.getId());
            billPath.put("billno", "cmp_salarypay");
            billPath.put("serviceCode", IServicecodeConstant.SALARYPAY);
            billInfo.put("extbillpath", billPath.toString());
        }
        Map<String, String> params = new HashMap<>();
        params.put("billinfo", billInfo.toString());

//        JsonConfig jsonConfig = new JsonConfig();
//        jsonConfig.registerJsonValueProcessor(java.util.Date.class, new JsonDateValueProcessor());
//        jsonConfig.registerJsonValueProcessor(java.sql.Timestamp.class, new JsonTimestampValueProcessor());
//        jsonConfig.registerJsonValueProcessor(java.sql.Date.class, new JsonDateValueProcessor());
        /**
         * begin yangjn 20210805 修改自由自定义项相关数据
         */
        generateVoucherAddDefines(bizObject, billTypeCode);
        /**
         *  end yangjn
         */
        params.put("billdata",  CtmJSONObject.toJSONString(bizObject));
        //财务新架构标识
        if (CmpCommonUtil.getNewFiFlag()) {
            boolean enableEVNT = AppContext.getBean(ICtmApplicationService.class).isEnableApp(CtmConstants.YONBIP_FI_EEAC_EVNT_APP_CODE);
            if (!enableEVNT) {
                log.error("客户环境未安装事项中台服务");
                bizObject.set("voucherstatus", VoucherStatus.NONCreate.getValue());
                generateResult.put("dealSucceed", false);
                generateResult.put("genVoucher", false);
                generateResult.put("code", 1);
            } else {
                try {
                    CtmJSONObject billClue = new CtmJSONObject();
                    billClue.put("classifier", null);
                    billClue.put("srcBusiId", bizObject.getId().toString());
                    cmpSendEventService.sendSimpleEvent(bizObject, billClue);
                    bizObject.set("voucherstatus", VoucherStatus.POSTING.getValue());
                    generateResult.put("dealSucceed", true);
                } catch (Exception e) {
                    generateResult.put("dealSucceed", false);
                    generateResult.put("message", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805CC", "生成凭证失败！请检查数据！") /* "生成凭证失败！请检查数据！" */);
                    log.error("send event generate voucher fail!, bizObject = {}, e ={}", bizObject, e.getMessage());
                }
            }
        } else {
            log.error("generator voucher input param, yTenantId = {}, code = {}, params = {}",
                    InvocationInfoProxy.getTenantid(), bizObject.get("code"), CtmJSONObject.toJSONString(params));

            // 设置单据附件数: Sun GuoCai 2023/7/3
            commonService.getFilesCount(bizObject);

            Map<String, String> requestHeader = new HashMap<>();
            requestHeader.put("ytsEnable", "true");
            requestHeader.put("ytsMode", "tcc");
            String serverUrl = AppContext.getEnvConfig("fiotp.servername");
            String responseStr = HttpTookitYts.doPostWithJson(serverUrl + TRY_CREATE_VOUCH_URL, CtmJSONObject.toJSONString(params), requestHeader);
            CtmJSONObject resultJson = CtmJSONObject.parseObject(responseStr);
            int code = resultJson.getIntValue("code");
            if (1 == code) {
                bizObject.set("voucherstatus", VoucherStatus.Received.getValue());
                generateResult.put("dealSucceed", true);
            } else if (0 == code){
                generateResult.put("dealSucceed", false);
//                generateResult.put("message", resultJson.getJSONObject("result").getString("message"));
            }
        }
        return generateResult;
    }

    /**
     * 自由自定义项生成凭证时，修改数据结构
     *
     * @param bizObject
     * @param billTypeCode
     * @Author yangjn
     * @Date 20210813
     */
    private void generateVoucherAddDefines(BizObject bizObject, String billTypeCode) {
        // 表头处理
        if (bizObject.get("defines") != null) {
            //说明存在配置的自由自定义项
            bizObject.put("headfree", bizObject.get("defines"));
        }
        //表体处理 现在只有收付款工作台 有表体
        if ("FICM1".equals(billTypeCode) || "FICM2".equals(billTypeCode)) {
            if ("FICM1".equals(billTypeCode)) {//收款
                List<ReceiveBill_b> receive_bodyList = bizObject.getBizObjects("ReceiveBill_b", ReceiveBill_b.class);
                for (ReceiveBill_b body : receive_bodyList) {
                    if (body.get("defines") != null) {
                        body.put("bodyfree", body.get("defines"));
                    }
                }
            } else if ("FICM2".equals(billTypeCode)) {//付款
                List<PayBillb> pay_bodyList = bizObject.getBizObjects("PayBillb", PayBillb.class);
                for (PayBillb body : pay_bodyList) {
                    if (body.get("defines") != null) {
                        body.put("bodyfree", body.get("defines"));
                    }
                }
            }
        }
        if (FUND_COLLECTION.equals(billTypeCode) || (FUND_PAYMENT.equals(billTypeCode))) {
            if (FUND_COLLECTION.equals(billTypeCode)) {//收款
                List<FundCollection_b> receive_bodyList = bizObject.getBizObjects("FundCollection_b", FundCollection_b.class);
                for (FundCollection_b body : receive_bodyList) {
                    if (body.get("defines") != null) {
                        body.put("bodyfree", body.get("defines"));
                    }
                }
            } else {//付款
                List<FundPayment_b> pay_bodyList = bizObject.getBizObjects("FundPayment_b", FundPayment_b.class);
                for (FundPayment_b body : pay_bodyList) {
                    if (body.get("defines") != null) {
                        body.put("bodyfree", body.get("defines"));
                    }
                }
            }
        }
    }

    /**
     * 老架构删除凭证
     *
     * @param bizObject
     * @return
     * @throws Exception
     */
    @Override
    public CtmJSONObject deleteVoucherWithResult(BizObject bizObject) throws Exception {
        CtmJSONObject deleteResult = new CtmJSONObject();
        // 不生成或不过账，取消过账时直接返回  2022/9/28 10:50
        short voucherstatus = Short.parseShort(bizObject.get("voucherstatus").toString());
       /* if (voucherstatus == VoucherStatus.Empty.getValue()) {
            bizObject.set("voucherstatus", VoucherStatus.Empty.getValue());
            deleteResult.put("voucherstatus", VoucherStatus.Empty.getValue());
            deleteResult.put("dealSucceed", true);
            return deleteResult;
        }*/

        //来源费用的单据不在现金管理删除凭证
        Short srcItem = -1;
        if (bizObject.get("srcitem") != null) {
            srcItem = Short.valueOf(bizObject.get("srcitem").toString());
        }
        //逻辑修改，来源费用的单据，线下支付时同步凭证状态，取消线下支付时，凭证状态设为未生成，费用删除凭证异常，则回滚事务
        if (srcItem.equals(EventSource.SystemOut.getValue())) {
            bizObject.set("voucherstatus", VoucherStatus.Empty.getValue());
            deleteResult.put("voucherstatus", VoucherStatus.Empty.getValue());
            deleteResult.put("dealSucceed", true);
            return deleteResult;
        }
        String systemCode = "";
        if (!(("cmp.foreignpayment.ForeignPayment").equals(bizObject.get("_entityName")) || bizObject.getEntityName().equals("cmp.paymargin.PayMargin") || bizObject.getEntityName().equals("cmp.receivemargin.ReceiveMargin"))) {
            systemCode = getSystemCode(bizObject);
        }
        // 来源于应收和应付的单据不在现金生成凭证
        if ("fiar".equals(systemCode) || "fiap".equals(systemCode)) {
            deleteResult.put("dealSucceed", true);
            return deleteResult;
        }

        //总账模块启用会计期间开始日期大于单据日期不删除凭证  - 支持跨组织核算，取消总账期初的校验
//        Date enabledBeginData = queryOrgPeriodBeginDateGL(bizObject.get(IBussinessConstant.ACCENTITY));
//        if (enabledBeginData == null) {
//            deleteResult.put("dealSucceed", true);
//            return deleteResult;
//        }
//        if (enabledBeginData.compareTo(bizObject.get("vouchdate")) > 0) {
//            deleteResult.put("dealSucceed", true);
//            return deleteResult;
//        }
        String billId = bizObject.getId().toString();
        if (JournalBill.ENTITY_NAME.equals(bizObject.getEntityName())) {
            billId = ((JournalBill) bizObject).JournalBill_b().get(0).getId().toString(); // 日记账录入采用明细的id
        }
        CtmJSONObject billInfo = new CtmJSONObject();
        billInfo.put("reqid", UUID.randomUUID().toString());
        billInfo.put("action", "del");
        billInfo.put("billid", billId);
        billInfo.put("billno", bizObject.get("code"));
        String billTypeCode = "";
        if (!(("cmp.foreignpayment.ForeignPayment").equals(bizObject.get("_entityName")) || bizObject.getEntityName().equals("cmp.paymargin.PayMargin") || bizObject.getEntityName().equals("cmp.receivemargin.ReceiveMargin"))) {
            billTypeCode = getBillTypeCode(bizObject, systemCode);
        }
        billInfo.put("billtypecode", billTypeCode);
        billInfo.put("tradetypecode", getTransTypeCodeById(bizObject.get("tradetype").toString()));
        billInfo.put("systemcode", systemCode);
        billInfo.put("dessystemcode", "figl");
        billInfo.put("desbilltypecode", "C0");
        Map<String, String> params = new HashMap<>();
        params.put("billinfo", billInfo.toString());

//        JsonConfig jsonConfig = new JsonConfig();
//        jsonConfig.registerJsonValueProcessor(java.util.Date.class, new JsonDateValueProcessor());
//        jsonConfig.registerJsonValueProcessor(java.sql.Timestamp.class, new JsonTimestampValueProcessor());
//        jsonConfig.registerJsonValueProcessor(java.sql.Date.class, new JsonDateValueProcessor());
        params.put("billdata",  CtmJSONObject.toJSONString(bizObject));
        BatchDelVoucherDTO voucherDTO = new BatchDelVoucherDTO();
        if (!(("cmp.foreignpayment.ForeignPayment").equals(bizObject.get("_entityName")) || bizObject.getEntityName().equals("cmp.paymargin.PayMargin") || bizObject.getEntityName().equals("cmp.receivemargin.ReceiveMargin"))) {
            voucherDTO = BatchDelVoucherDTO.buildDTO(billId, getBillTypeCode(bizObject, systemCode), systemCode);
        }
        String requestParam = CtmJSONObject.toJSONString(voucherDTO);
        Map<String, String> header = new HashMap<>();
        header.put("ytsEnable", "true");
        header.put("ytsMode", "tcc");
        String serverUrl = AppContext.getEnvConfig("fiotp.servername");
        StringBuffer stringBuffer = new StringBuffer("HTTPPOST");
        stringBuffer.append("\n");
        stringBuffer.append(serverUrl).append(TRY_BATCH_DEL_URL);
        stringBuffer.append("\n");
        stringBuffer.append(CtmJSONObject.toJSONString(requestParam));
        //财务新架构标识
        if (CmpCommonUtil.getNewFiFlag()) {
            try {
                boolean enableEVNT = AppContext.getBean(ICtmApplicationService.class).isEnableApp(CtmConstants.YONBIP_FI_EEAC_EVNT_APP_CODE);
                if (!enableEVNT) {
                    log.error("客户环境未安装事项中台服务");
                }else{
                    cmpSendEventService.deleteEvent(bizObject);
                }
                bizObject.set("voucherstatus", VoucherStatus.Empty.getValue());
                deleteResult.put("voucherstatus", VoucherStatus.Empty.getValue());
                deleteResult.put("dealSucceed", true);
                ctmcmpBusinessLogService.saveBusinessLog(bizObject, bizObject.get("code"), bizObject.get("code"), IServicecodeConstant.PAYMARGIN,com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054004FB", "单据删除凭证") /* "单据删除凭证" */, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054004FC", "删除成功！") /* "删除成功！" */);
            } catch (Exception e) {
                deleteResult.put("dealSucceed", false);
                deleteResult.put("message", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418008F","删除凭证失败：") /* "删除凭证失败：" */ + e.getMessage());
                log.error("send event generate voucher fail!, bizObject = {}, e ={}", bizObject, e.getMessage());
                ctmcmpBusinessLogService.saveBusinessLog(bizObject, bizObject.get("code"), bizObject.get("code"), IServicecodeConstant.PAYMARGIN,com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054004FB", "单据删除凭证") /* "单据删除凭证" */, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054004FA", "删除失败！") /* "删除失败！" */);
            }
        } else {
            log.error("generator voucher input param, yTenantId = {}, code = {}, params = {}",
                    InvocationInfoProxy.getTenantid(), bizObject.get("code"), CtmJSONObject.toJSONString(params));
            //TODO 调整代码需修改
            HttpLogEntity entity = HttpLogEntity.build(billId, bizObject.get("code").toString(), stringBuffer.toString(), "StringConst.BATCH_DEL_VOUCHER", YtsContext.currentGtxId());
            try {
                String responseStr = HttpTookitYts.doPostWithJson(serverUrl + TRY_BATCH_DEL_URL, requestParam, header);
                CtmJSONObject resultJson = CtmJSONObject.parseObject(responseStr);
                if (resultJson.getBoolean("success")) {
                    bizObject.set("voucherstatus", VoucherStatus.Empty.getValue());
                    deleteResult.put("voucherstatus", VoucherStatus.Empty.getValue());
                    deleteResult.put("dealSucceed", true);
                } else {
                    deleteResult.put("dealSucceed", false);
                    deleteResult.put("message", resultJson.getString("message"));
                }
            } catch (CtmException CtmException) {
                entity.setResponse(CtmException.getMessage());
                throw CtmException;
            } catch (Exception e) {
                log.error("batch_del_voucher error!, e = {}", e.getMessage());
                entity.setResponse(e.getMessage());
                throw new BizException(ErrorCode.VOUCHER_DEL_BATCH_ERROR.getCode(), ErrorCode.VOUCHER_DEL_BATCH_ERROR.getMessage());
            } finally {
                entity.setResponseTime(Calendar.getInstance().getTime());
                //BizUtils.asyncSaveHttpLog(entity);
            }
        }
        return deleteResult;
    }

    @Override
    public CtmJSONObject deleteVoucherWithResultWithOutException(BizObject bizObject) throws Exception {
        CtmJSONObject deleteResult = new CtmJSONObject();
        // 不生成或不过账，取消过账时直接返回  2022/9/28 10:50
        short voucherstatus = Short.parseShort(bizObject.get("voucherstatus").toString());
        /*if (voucherstatus == VoucherStatus.Empty.getValue() || voucherstatus == VoucherStatus.NONCreate.getValue()) {
            bizObject.set("voucherstatus", VoucherStatus.Empty.getValue());
            deleteResult.put("dealSucceed", true);
            return deleteResult;
        }*/
        //来源费用的单据不在现金管理删除凭证
        Short srcItem = -1;
        if (bizObject.get("srcitem") != null) {
            srcItem = Short.valueOf(bizObject.get("srcitem").toString());
        }
        //逻辑修改，来源费用的单据，线下支付时同步凭证状态，取消线下支付时，凭证状态设为未生成，费用删除凭证异常，则回滚事务
        if (srcItem.equals(EventSource.SystemOut.getValue())) {
            bizObject.set("voucherstatus", VoucherStatus.Empty.getValue());
            deleteResult.put("dealSucceed", true);
            return deleteResult;
        }
        String systemCode = "";
        if (!(("cmp.foreignpayment.ForeignPayment").equals(bizObject.get("_entityName")) || bizObject.getEntityName().equals("cmp.paymargin.PayMargin") || bizObject.getEntityName().equals("cmp.receivemargin.ReceiveMargin"))) {
            systemCode = getSystemCode(bizObject);
        }
        // 来源于应收和应付的单据不在现金生成凭证
        if ("fiar".equals(systemCode) || "fiap".equals(systemCode)) {
            deleteResult.put("dealSucceed", true);
            return deleteResult;
        }
        CtmJSONObject billInfo = new CtmJSONObject();
        billInfo.put("reqid", UUID.randomUUID().toString());
        billInfo.put("action", "del");
        billInfo.put("billid", bizObject.getId().toString());
        billInfo.put("billno", bizObject.get("code"));
        String billTypeCode = "";
        if (!(("cmp.foreignpayment.ForeignPayment").equals(bizObject.get("_entityName")) || bizObject.getEntityName().equals("cmp.paymargin.PayMargin") || bizObject.getEntityName().equals("cmp.receivemargin.ReceiveMargin"))) {
            billTypeCode = getBillTypeCode(bizObject, systemCode);
        }
        billInfo.put("billtypecode", billTypeCode);
        billInfo.put("tradetypecode", getTransTypeCodeById(bizObject.get("tradetype").toString()));
        billInfo.put("systemcode", systemCode);
        billInfo.put("dessystemcode", "figl");
        billInfo.put("desbilltypecode", "C0");
        Map<String, String> params = new HashMap<>();
        params.put("billinfo", billInfo.toString());
        params.put("billdata",  CtmJSONObject.toJSONString(bizObject));
        BatchDelVoucherDTO voucherDTO = new BatchDelVoucherDTO();
        if (!(("cmp.foreignpayment.ForeignPayment").equals(bizObject.get("_entityName")) || bizObject.getEntityName().equals("cmp.paymargin.PayMargin") || bizObject.getEntityName().equals("cmp.receivemargin.ReceiveMargin"))) {
            voucherDTO = BatchDelVoucherDTO.buildDTO(bizObject.getId().toString(), getBillTypeCode(bizObject, systemCode), systemCode);
        }
        String requestParam = CtmJSONObject.toJSONString(voucherDTO);
        Map<String, String> header = new HashMap<>();
        header.put("ytsEnable", "true");
        header.put("ytsMode", "tcc");
        String serverUrl = AppContext.getEnvConfig("fiotp.servername");
        StringBuffer stringBuffer = new StringBuffer("HTTPPOST");
        stringBuffer.append("\n");
        stringBuffer.append(serverUrl).append(TRY_BATCH_DEL_URL);
        stringBuffer.append("\n");
        stringBuffer.append(CtmJSONObject.toJSONString(requestParam));
        //财务新架构标识
        if (CmpCommonUtil.getNewFiFlag()) {
            boolean enableEVNT = AppContext.getBean(ICtmApplicationService.class).isEnableApp(CtmConstants.YONBIP_FI_EEAC_EVNT_APP_CODE);
            if (!enableEVNT) {
                log.error("客户环境未安装事项中台服务");
                bizObject.set("voucherstatus", VoucherStatus.Empty.getValue());
                deleteResult.put("dealSucceed", true);
            }else{
                deleteResult = cmpSendEventService.deleteEventWithoutException(bizObject);
                if (deleteResult.get("dealSucceed") != null && (boolean) deleteResult.get("dealSucceed")) {
                    bizObject.set("voucherstatus", VoucherStatus.Empty.getValue());
                }
            }
            return deleteResult;
        } else {
            log.error("generator voucher input param, yTenantId = {}, code = {}, params = {}",
                    InvocationInfoProxy.getTenantid(), bizObject.get("code"), CtmJSONObject.toJSONString(params));
            //TODO 调整代码需修改
            HttpLogEntity entity = HttpLogEntity.build(bizObject.getId().toString(), bizObject.get("code").toString(), stringBuffer.toString(), "StringConst.BATCH_DEL_VOUCHER", YtsContext.currentGtxId());
            try {
                String responseStr = HttpTookitYts.doPostWithJson(serverUrl + TRY_BATCH_DEL_URL, requestParam, header);
                CtmJSONObject resultJson = CtmJSONObject.parseObject(responseStr);
                if (resultJson.getBoolean("success")) {
                    bizObject.set("voucherstatus", VoucherStatus.Empty.getValue());
                    deleteResult.put("dealSucceed", true);
                } else {
                    deleteResult.put("dealSucceed", false);
                    deleteResult.put("message", resultJson.getString("message"));
                }
            } catch (CtmException CtmException) {
                entity.setResponse(CtmException.getMessage());
                throw CtmException;
            } catch (Exception e) {
                log.error("batch_del_voucher error!, e = {}", e.getMessage());
                entity.setResponse(e.getMessage());
                throw new BizException(ErrorCode.VOUCHER_DEL_BATCH_ERROR.getCode(), ErrorCode.VOUCHER_DEL_BATCH_ERROR.getMessage());
            } finally {
                entity.setResponseTime(Calendar.getInstance().getTime());
                //BizUtils.asyncSaveHttpLog(entity);
            }
        }
        return deleteResult;
    }

    /**
     * 删除凭证-适配分布式事务
     *
     * @param bizObject
     * @return
     * @throws Exception
     */
    @Override
    public CtmJSONObject deleteVoucherWithResultTry(BizObject bizObject) throws Exception {
        CtmJSONObject deleteResult = new CtmJSONObject();

        //来源费用的单据不在现金管理删除凭证
        Short srcItem = -1;
        if (bizObject.get("srcitem") != null) {
            srcItem = Short.valueOf(bizObject.get("srcitem").toString());
        }
        //逻辑修改，来源费用的单据，线下支付时同步凭证状态，取消线下支付时，凭证状态设为未生成，费用删除凭证异常，则回滚事务
        if (srcItem.equals(EventSource.SystemOut.getValue())) {
            bizObject.set("voucherstatus", VoucherStatus.Empty.getValue());
            deleteResult.put("dealSucceed", true);
            return deleteResult;
        }

        String systemCode = getSystemCode(bizObject);
        // 来源于应收和应付的单据不在现金生成凭证
        if ("fiar".equals(systemCode) || "fiap".equals(systemCode)) {
            deleteResult.put("dealSucceed", true);
            return deleteResult;
        }

        //总账模块启用会计期间开始日期大于单据日期不删除凭证  - 支持跨组织核算，取消总账期初的校验
//        Date enabledBeginData = queryOrgPeriodBeginDateGL(bizObject.get(IBussinessConstant.ACCENTITY));
//        if (enabledBeginData == null) {
//            deleteResult.put("dealSucceed", true);
//            return deleteResult;
//        }
//        if (enabledBeginData.compareTo(bizObject.get("vouchdate")) > 0) {
//            deleteResult.put("dealSucceed", true);
//            return deleteResult;
//        }

        // 不生成或不过账，取消过账时直接返回 2022/9/28 10:50
        short voucherstatus = Short.parseShort(bizObject.get("voucherstatus_original").toString());
        /*if (voucherstatus == VoucherStatus.Empty.getValue() || voucherstatus == VoucherStatus.NONCreate.getValue()) {
            bizObject.set("voucherstatus", VoucherStatus.Empty.getValue());
            deleteResult.put("dealSucceed", true);
            return deleteResult;
        }*/

        BatchDelVoucherDTO voucherDTO =
                BatchDelVoucherDTO.buildDTO(bizObject.getId().toString(), getBillTypeCode(bizObject, systemCode), systemCode);
        String requestParam = CtmJSONObject.toJSONString(voucherDTO);
        Map<String, String> header = new HashMap<>();
        header.put("ytsEnable", "true");
        header.put("ytsMode", "tcc");
        String serverUrl = AppContext.getEnvConfig("fiotp.servername");
        StringBuffer stringBuffer = new StringBuffer("HTTPPOST");
        stringBuffer.append("\n");
        stringBuffer.append(serverUrl).append(TRY_BATCH_DEL_URL);
        stringBuffer.append("\n");
        stringBuffer.append(CtmJSONObject.toJSONString(requestParam));
        //财务新架构标识
        if (CmpCommonUtil.getNewFiFlag()) {
            boolean enableEVNT = AppContext.getBean(ICtmApplicationService.class).isEnableApp(CtmConstants.YONBIP_FI_EEAC_EVNT_APP_CODE);
            if (!enableEVNT) {
                log.error("客户环境未安装事项中台服务");
                bizObject.set("voucherstatus", VoucherStatus.Empty.getValue());
            }else {
                cmpSendEventService.deleteEvent(bizObject);
            }
            deleteResult.put("dealSucceed", true);
            return deleteResult;
        } else {
            HttpLogEntity entity = HttpLogEntity.build(bizObject.getId().toString(), bizObject.get("code").toString(), stringBuffer.toString(), StringConst.BATCH_DEL_VOUCHER, YtsContext.currentGtxId());
            try {
                String responseStr = HttpTookitYts.doPostWithJson(serverUrl + TRY_BATCH_DEL_URL, requestParam, header);
                CtmJSONObject resultJson = CtmJSONObject.parseObject(responseStr);
                if (resultJson.getBoolean("success")) {
                    bizObject.set("voucherstatus", VoucherStatus.Empty.getValue());
                    deleteResult.put("dealSucceed", true);
                } else {
                    deleteResult.put("dealSucceed", false);
                    deleteResult.put("message", resultJson.getString("message"));
                }
                return deleteResult;
            } catch (CtmException CtmException) {
                entity.setResponse(CtmException.getMessage());
                throw CtmException;
            } catch (Exception e) {
                log.error("batch_del_voucher error!, e = {}", e.getMessage());
                entity.setResponse(e.getMessage());
                throw new BizException(ErrorCode.VOUCHER_DEL_BATCH_ERROR.getCode(), ErrorCode.VOUCHER_DEL_BATCH_ERROR.getMessage());
            } finally {
                entity.setResponseTime(Calendar.getInstance().getTime());
                //BizUtils.asyncSaveHttpLog(entity);
            }
        }
    }

    @Override
    public boolean updateVoucherStatus(CtmJSONObject voucherStatusInfo) throws Exception {
        log.error("by mq message update fund payment or collection bill voucherNo, voucherStatusInfo = {}", CtmJSONObject.toJSONString(voucherStatusInfo));
        String tableName = "";
        String billTypeCode = voucherStatusInfo.getString("billtypecode");
        if ("arap_paybill_PayBill".equals(billTypeCode) || "FICM2".equals(billTypeCode)) {
            tableName = "cmp_paybill";
        } else if ("arap_receivebill_ReceiveBill".equals(billTypeCode) || "FICM1".equals(billTypeCode)) {
            tableName = IBillNumConstant.RECEIVE_BILL;
        } else if (TransferAccount.ENTITY_NAME.equals(billTypeCode)) {
            tableName = "cm_transfer_account";
        } else if (CurrencyExchange.ENTITY_NAME.equals(billTypeCode)) {
            tableName = "cmp_currencyexchange";
        } else if (ExchangeGainLoss.ENTITY_NAME.equals(billTypeCode)) {
            tableName = "cmp_exchangegainloss";
        } else if (Salarypay.ENTITY_NAME.equals(billTypeCode)) {
            tableName = "cmp_salarypay";
        } else if (FUND_COLLECTION.equals(billTypeCode)) {
            tableName = "stwb_settleapply";
            FundCollection fundCollection = new FundCollection();
            fundCollection.setId(voucherStatusInfo.get("billid"));
            VoucherStatus voucherStatus;
            fundCollection.setVoucherId(String.valueOf(voucherStatusInfo.get("voucherid")));
            if(voucherStatusInfo.getString("midvoucherstate").equals("nogen")){
                voucherStatus = VoucherStatus.NONCreate;
            } else {
                voucherStatus = VoucherStatus.Created;
            }
            fundCollection.setVoucherstatus(voucherStatus);
            fundCollection.setEntityStatus(EntityStatus.Update);
            MetaDaoHelper.update(FUND_COLLECTION_FULLNAME,fundCollection);
        } else if (FUND_PAYMENT.equals(billTypeCode)) {
            tableName = "stwb_settleapply";
            FundPayment fundPayment = new FundPayment();
            fundPayment.setId(voucherStatusInfo.get("billid"));
            VoucherStatus voucherStatus;
            fundPayment.setVoucherId(String.valueOf(voucherStatusInfo.get("voucherid")));
            if(voucherStatusInfo.getString("midvoucherstate").equals("nogen")){
                voucherStatus = VoucherStatus.NONCreate;
            } else {
                voucherStatus = VoucherStatus.Created;
            }
            fundPayment.setVoucherstatus(voucherStatus);
            fundPayment.setEntityStatus(EntityStatus.Update);
            MetaDaoHelper.update(FUND_PAYMENT_FULLNAME,fundPayment);
        }
        if (StringUtils.isEmpty(tableName)) {
            return false;
        }
        // 凭证状态 --success - nogen 模板或生效条件不生成
        String tenantId = null;
        if (!"fier".equals(voucherStatusInfo.get("systemCode"))) { // 费用项目没有tenantid
            tenantId = voucherStatusInfo.getString("tenantid");
        }

        Map<String, Object> params = new HashMap<>();
        params.put("tableName", tableName);
        params.put("id", voucherStatusInfo.get("billid"));
        params.put("tenantId", tenantId);
        params.put("voucherId", String.valueOf(voucherStatusInfo.get("voucherid")));
        getVoucherNoByID(params, voucherStatusInfo, tableName, tenantId);

        log.error("by mq message update fund payment or collection bill voucherNo params, billId = {}, params={}",
                voucherStatusInfo.get("billid"), params);
        if (voucherStatusInfo.getString("midvoucherstate").equals("nogen")) {
            params.put("voucherstatus", VoucherStatus.NONCreate.getValue());
        } else {
            params.put("voucherstatus", VoucherStatus.Created.getValue());
        }
        try {
            if (!tableName.equals("stwb_settleapply")) {
                int updateCount = SqlHelper.update("com.yonyoucloud.fi.cmp.voucher.updateVoucherStatus", params);
                return updateCount > 0;
            }
        } catch (Exception e) {
            log.error("更新凭证状态异常", e);
        }
        return false;
    }

    public void getVoucherNoByID(Map<String, Object> paramsMap, CtmJSONObject voucherStatusInfo, String tableName, String tenantId) {
        CtmJSONObject jsonObj = new CtmJSONObject();
        String voucherId = String.valueOf(voucherStatusInfo.get("voucherid"));
        Object id = voucherStatusInfo.get("billid");
        try {
            List<String> ids = new ArrayList<>(16);
            if (ValueUtils.isNotEmptyObj(voucherId)) {
                ids.add(voucherId);
            }
            if (ValueUtils.isNotEmptyObj(ids)) {
                CtmJSONObject json = new CtmJSONObject();
                json.put("ids", ids);
                String serverUrl = AppContext.getEnvConfig("yzb.base.url");
                String responseStr = HttpTookit.doPostWithJson(serverUrl + "/voucher/getbillcodesbyvoucherids", CtmJSONObject.toJSONString(json), new HashMap<>());
                log.error("by mq message update fund payment or collection bill voucherNo response message, tableName = {}, voucherId = {}, responseStr = {}",
                        tableName, voucherId, responseStr);
                CtmJSONObject jsonObject = CtmJSONObject.parseObject(responseStr);
                CtmJSONArray jsonResult = jsonObject.getJSONArray("data");
                if (jsonResult.size() > 0) {
                    CtmJSONObject resultJSONObject = jsonResult.getJSONObject(0);
                    if (ValueUtils.isNotEmptyObj(resultJSONObject.get("displayname1"))) {
                        jsonObj.put("zh_CN", resultJSONObject.get("displayname1"));
                    }
                    if (ValueUtils.isNotEmptyObj(resultJSONObject.get("displayname2"))) {
                        jsonObj.put("en_US", resultJSONObject.get("displayname2"));
                    }
                    if (ValueUtils.isNotEmptyObj(resultJSONObject.get("displayname3"))) {
                        jsonObj.put("zh_TW", resultJSONObject.get("displayname3"));
                    }
                    paramsMap.put("voucherPeriod", resultJSONObject.get("period"));
                    paramsMap.put("voucherNo", CtmJSONObject.toJSONString(jsonObj));
                    if (!"stwb_settleapply".equals(tableName) && ValueUtils.isNotEmptyObj(jsonObj.get("zh_CN"))) {
                        Map<String, Object> params = new HashMap<>();
                        params.put("voucherNo", jsonObj.get("zh_CN"));
                        params.put("id", id);
                        params.put("voucherPeriod", resultJSONObject.get("period"));
                        params.put("tenantId", tenantId);
                        log.error("update journal voucherNo, id = {}, tenantId = {}, params = {}", id, tenantId, params);
                        try {
                            SqlHelper.update("com.yonyoucloud.fi.cmp.voucher.updateVoucherNoOfJournal", params);
                        } catch (Exception e) {
                            log.error("update journal voucherNo fail,params = {}, e= {}", params, e);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("by mq message update fund payment or collection bill voucherNo fail, voucherId = {}, e = {}", voucherId, e);
        }
    }


    @Override
    public String queryVoucherId(CtmJSONObject param) throws Exception {
        BillContext billContext = BillContextUtils.getBillContext(param.getString("billnum"));
        BizObject bizObject = MetaDaoHelper.findById(billContext.getFullname(), param.getLong("id"));
        bizObject.set("_entityName", billContext.getFullname());
        // 对于收款单做结算的单据，需替换成原arap的元数据结构
        if (ReceiveBill.ENTITY_NAME.equals(bizObject.getEntityName())) {
            bizObject.set("_entityName", ReceiveBill.ENTITY_NAME);
        }
        if (PayBill.ENTITY_NAME.equals(bizObject.getEntityName())) {
            bizObject.set("_entityName", PayBill.ENTITY_NAME);
        }
        CtmJSONObject billInfo = new CtmJSONObject();
        String reqid = UUID.randomUUID().toString();
        billInfo.put("reqid", reqid);
        billInfo.put("action", "link");
        Short srcItem = -1;
        String systemCode = getSystemCode(bizObject);
        if (bizObject.get("srcitem") != null) {
            srcItem = Short.valueOf(bizObject.get("srcitem").toString());
        }
        if (srcItem.equals(EventSource.SystemOut.getValue())) {
            billInfo.put("billid", bizObject.get("srcbillid").toString());
            billInfo.put("billtypecode", getCostBillTypeCode(bizObject));
            billInfo.put("systemcode", "fier");
        } else {
            billInfo.put("billid", bizObject.getId().toString());
            billInfo.put("billtypecode", getBillTypeCode(bizObject, systemCode));
            billInfo.put("systemcode", systemCode);
        }
        if ("fiar".equals(systemCode) || "fiap".equals(systemCode)) {
            billInfo.put("billid", bizObject.get("srcbillid").toString());
            billInfo.put("billtypecode", getBillTypeCode(bizObject, systemCode));
            billInfo.put("systemcode", systemCode);
        }
        billInfo.put("tradetypecode", getTransTypeCodeById(bizObject.get("tradetype").toString()));
        Map<String, String> params = new HashMap();
        params.put("billinfo", billInfo.toString());

        String serverUrl = AppContext.getEnvConfig("fiotp.servername");
        CtmJSONObject responseJson = CtmJSONObject.parseObject(HttpTookit.doPost(serverUrl + "/exchanger/linkvoucher", params, new HashMap<>()));
        return ResultMessage.data(responseJson);
    }

    /**
     * 获取费用单据类型编码
     *
     * @param bizObject
     * @return
     */
    private Object getCostBillTypeCode(BizObject bizObject) throws Exception {
        QuerySchema schema = QuerySchema.create().fullname("bd.bill.TransType");
        schema.addSelect("id", "billtype_id");
        schema.appendQueryCondition(QueryCondition.name("id").eq(bizObject.get("tradetype")));
        List<Map<String, Object>> data = MetaDaoHelper.query(schema.fullname(), schema, ISchemaConstant.MDD_SCHEMA_TRANSTYPE);
        if (data != null && data.size() == 1) {
            if ("FICO2".equals(data.get(0).get("billtype_id"))) {
                return "bx";
            } else if ("FICO4".equals(data.get(0).get("billtype_id"))) {
                return "jk";
            }
        }
        return "";
    }

    /*
     *@Author tongyd
     *@Description 获取来源系统编码
     *@Date 2019/7/11 20:53
     *@Param [billTypeCode, srcItem]
     *@Return java.lang.String
     **/
    private String getSystemCode(BizObject bizObject) throws CtmException {
        String systemCode = "cmp";
        try {
            Object srcItemObj = bizObject.get("srcitem");
            Object billTypeObj = bizObject.get("billtype");

            Short srcItem = null;
            if (srcItemObj != null) {
                try {
                    srcItem = TypeUtils.castToShort(srcItemObj);
                } catch (NumberFormatException e) {
                    log.warn("srcitem cannot be cast to Short, srcitem: {}", srcItemObj);
                    // 如果srcitem无法转换为Short，保持为null
                }
            }

            // 使用字符串比较而不是直接转换为Short，避免NumberFormatException
            String billTypeStr = billTypeObj != null ? billTypeObj.toString() : null;

            if (srcItem != null && srcItem.equals(EventSource.SystemOut.getValue())) {
                return "fier";
            }

            if (srcItem != null && (srcItem.equals(EventSource.Cmpchase.getValue()) ||
                    "15".equals(billTypeStr) ||
                    srcItem.equals(EventSource.Drftchase.getValue()))) {
                return "cmp";
            }

            // 转账单导入
            if (srcItem != null && srcItem.equals(EventSource.ManualImport.getValue()) &&
                    EventType.TransferAccount.getValue() == Short.parseShort(billTypeStr != null ? billTypeStr : "0")) {
                return "cmp";
            }
            if (billTypeStr != null) {
                try {
                    // 尝试转换为Short进行比较，但如果失败则使用字符串比较
                    Short billType = TypeUtils.castToShort(billTypeObj);
                    if (billType.equals(EventType.ReceiveBill.getValue())) {
                        systemCode = "fiar";
                    } else if (billType.equals(EventType.OtherAREvent.getValue())) {
                        systemCode = "fiar";
                    } else if (billType.equals(EventType.OtherAPEvent.getValue())) {
                        systemCode = "fiap";
                    } else if (billType.equals(EventType.PayMent.getValue())) {
                        systemCode = "fiap";
                    } else if (billType.equals(EventType.ArRefund.getValue())) {
                        systemCode = "fiar";
                    } else if (billType.equals(EventType.ApRefund.getValue())) {
                        systemCode = "fiap";
                    } else if(billType.equals(EventType.Unified_Synergy.getValue())){
                        systemCode = "cmp";
                    }
                } catch (NumberFormatException e) {
                    // 当billtype无法转换为Short时，使用字符串比较
                    log.warn("billtype cannot be cast to Short, using string comparison. billtype: {}", billTypeStr);
                    if (Short.toString(EventType.ReceiveBill.getValue()).equals(billTypeStr)) {
                        systemCode = "fiar";
                    } else if (Short.toString(EventType.OtherAREvent.getValue()).equals(billTypeStr)) {
                        systemCode = "fiar";
                    } else if (Short.toString(EventType.OtherAPEvent.getValue()).equals(billTypeStr)) {
                        systemCode = "fiap";
                    } else if (Short.toString(EventType.PayMent.getValue()).equals(billTypeStr)) {
                        systemCode = "fiap";
                    } else if (Short.toString(EventType.ArRefund.getValue()).equals(billTypeStr)) {
                        systemCode = "fiar";
                    } else if (Short.toString(EventType.ApRefund.getValue()).equals(billTypeStr)) {
                        systemCode = "fiap";
                    } else if(Short.toString(EventType.Unified_Synergy.getValue()).equals(billTypeStr)){
                        systemCode = "cmp";
                    }
                }
            }
        }catch (Exception e){
            log.error("getSystemCode error{} ",e.getMessage(),e);
            return systemCode;
        }
        return systemCode;
    }

    /**
     * @return java.lang.String
     * @Author tongyd
     * @Description 获取单据类型编码
     * @Date 2019/8/1
     * @Param [billTypeCode, bizObject]
     **/
    private String getBillTypeCode(BizObject bizObject, String systemCode) throws CtmException {
        String billTypeCode = "";

        // 安全地获取 billtype 值，避免 NumberFormatException
        Object billTypeObj = bizObject.get("billtype");
        Short billType = null;

        if (billTypeObj != null) {
            try {
                // 尝试转换为 Short
                billType = TypeUtils.castToShort(billTypeObj);
            } catch (NumberFormatException e) {
                // 当无法转换为 Short 时，记录日志并继续处理
                log.warn("billtype cannot be cast to Short, billtype: {}", billTypeObj);
            }
        }

        String entityName = bizObject.getEntityName();

        // 只有当 billType 成功转换为 Short 时才进行 EventType 相关的比较
        if (billType != null) {
            if (systemCode.startsWith("fia") && billType.equals(EventType.ReceiveBill.getValue())) {//来源为收付fiar.fiap时
                billTypeCode = "arap_receivebill_ReceiveBill";
                return billTypeCode;
            } else if (billType.equals(EventType.OtherAREvent.getValue())) {
                billTypeCode = "arap_oar_OarMain";
                return billTypeCode;
            } else if (billType.equals(EventType.OtherAPEvent.getValue())) {
                billTypeCode = "arap_oap_OapMain";
                return billTypeCode;
            } else if (billType.equals(EventType.ArRefund.getValue())) {
                billTypeCode = "arap_paybill_PayBill";
                return billTypeCode;
            } else if (billType.equals(EventType.ApRefund.getValue())) {
                billTypeCode = "arap_receivebill_ReceiveBill";
                return billTypeCode;
            }
            //    else if(billType.equals(EventType.Unified_Synergy.getValue())){
            //        billTypeCode = "cmp_fund_payment_delegation";
            //        return billTypeCode;
            //    }
            else if (systemCode.startsWith("fia") && billType.equals(EventType.PayMent.getValue())) {
                billTypeCode = "arap_paybill_PayBill";
                return billTypeCode;
            }
        }

        // 原有的实体名称检查逻辑保持不变
        if (entityName.equals(ReceiveBill.ENTITY_NAME)) {
            billTypeCode = "FICM1";
        } else if (entityName.equals(PayBill.ENTITY_NAME)) {
            billTypeCode = "FICM2";
        } else if (entityName.equals(TransferAccount.ENTITY_NAME)) {
            billTypeCode = TransferAccount.ENTITY_NAME;
        } else if (entityName.equals(BatchTransferAccount.ENTITY_NAME)) {
            billTypeCode = BatchTransferAccount.ENTITY_NAME;
        } else if (entityName.equals(CurrencyExchange.ENTITY_NAME)) {
            billTypeCode = CurrencyExchange.ENTITY_NAME;
        } else if (entityName.equals(ExchangeGainLoss.ENTITY_NAME)) {
            billTypeCode = ExchangeGainLoss.ENTITY_NAME;
        } else if (entityName.equals(Salarypay.ENTITY_NAME)) {
            billTypeCode = Salarypay.ENTITY_NAME;
        } else if (entityName.equals(FundPayment.ENTITY_NAME)) {
            billTypeCode = FUND_PAYMENT;
        } else if (entityName.equals(FundCollection.ENTITY_NAME)) {
            billTypeCode = FUND_COLLECTION;
        } else if (entityName.equals(AccrualsWithholding.ENTITY_NAME)) {
            billTypeCode = ICmpConstant.BANK_INTEREST;
        } else if (entityName.equals(Fundexpense.ENTITY_NAME)) {
            billTypeCode = "fundexpense";
        } else if (entityName.equals(JournalBill.ENTITY_NAME)) {
            billTypeCode = JOURNAL_BILL;
        }

        return billTypeCode;
    }

    /**
     * 老架构调用总账接口校验凭证信息，如果是新架构不需要校验
     * @param param
     * @return
     * @throws Exception
     */
    @Override
    public boolean isChecked(CtmJSONObject param) throws Exception {
        //财务新架构标识
        if (CmpCommonUtil.getNewFiFlag()) {
            return false;
        }
        String voucherId = null;
        BillContext billContext = BillContextUtils.getBillContext(param.getString("billnum"));
        BizObject bizObject = MetaDaoHelper.findById(billContext.getFullname(), param.getLong("id"));
        bizObject.set("_entityName", billContext.getFullname());
        // 对于收款单做结算的单据，需替换成原arap的元数据结构
        if (ReceiveBill.ENTITY_NAME.equals(bizObject.getEntityName())) {
            bizObject.set("_entityName", ReceiveBill.ENTITY_NAME);
        }
        if (PayBill.ENTITY_NAME.equals(bizObject.getEntityName())) {
            bizObject.set("_entityName", PayBill.ENTITY_NAME);
        }
        //总账模块启用会计期间开始日期大于单据日期不删除凭证  - 支持跨组织核算，取消总账期初的校验
//        Date enabledBeginData = queryOrgPeriodBeginDateGL(bizObject.get(IBussinessConstant.ACCENTITY));
//        if (enabledBeginData == null) {
//            return false;
//        }
//        if (enabledBeginData.compareTo(bizObject.get("vouchdate")) > 0) {
//            return false;
//        }
        CtmJSONObject billInfo = new CtmJSONObject();
        String reqid = UUID.randomUUID().toString();
        billInfo.put("reqid", reqid);
        billInfo.put("action", "link");
        Short srcItem = -1;
        String systemCode = getSystemCode(bizObject);
        if (bizObject.get("srcitem") != null) {
            srcItem = Short.valueOf(bizObject.get("srcitem").toString());
        }
        if (srcItem.equals(EventSource.SystemOut.getValue())) {
            billInfo.put("billid", bizObject.get("srcbillid").toString());
            billInfo.put("billtypecode", getCostBillTypeCode(bizObject));
            billInfo.put("systemcode", "fier");
        } else {
            billInfo.put("billid", bizObject.getId().toString());
            billInfo.put("billtypecode", getBillTypeCode(bizObject, systemCode));
            billInfo.put("systemcode", systemCode);
        }
        try {
            billInfo.put("tradetypecode", getTransTypeCodeById(bizObject.get("tradetype").toString()));
        } catch (Exception e) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100649"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805CF", "未获取到对应的交易类型") /* "未获取到对应的交易类型" */);
        }
        Map<String, String> params = new HashMap();
        params.put("billinfo", billInfo.toString());

        String serverUrl = AppContext.getEnvConfig("fiotp.servername");
        String yzb_serverUrl = AppContext.getEnvConfig("yzb.base.url");
        CtmJSONObject responseJson = CtmJSONObject.parseObject(HttpTookit.doPost(serverUrl + "/exchanger/linkvoucher", params, new HashMap<>()));


        if (responseJson.get("data") != null && (responseJson.get("data") instanceof ArrayList || responseJson.get("data") instanceof CtmJSONArray)) {
            CtmJSONArray array = responseJson.getJSONArray("data");
            if (array.size() > 0) {
                CtmJSONObject o = array.getJSONObject(0);
                if (o.getString("voucherid") != null) {
                    voucherId = o.getString("voucherid");
                }
                if (voucherId != null) {
                    CtmJSONObject jsonobject = new CtmJSONObject();
                    jsonobject.put("voucherid", voucherId);
                    Map<String, String> header = new HashMap<>();
                    header.put("Content-Type", "application/json");
                    CtmJSONObject checkd = CtmJSONObject.parseObject(HttpTookit.doPostWithJson(yzb_serverUrl + "/cash/isChecked", CtmJSONObject.toJSONString(jsonobject), new HashMap<>()));
                    if (checkd != null) {
                        return checkd.getBoolean("data").booleanValue();
                    }
                }
            }
        }


        return false;
    }

}
