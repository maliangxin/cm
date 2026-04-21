package com.yonyoucloud.fi.cmp.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.yonyou.iuap.fileservice.sdk.module.pojo.CooperationFileInfo;
import com.yonyou.iuap.fileservice.sdk.service.CooperationFileDownloadService;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.yonbip.ctm.bankconnection.BankConnectionAdapterContext;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.cmpentity.Bsflag;
import com.yonyoucloud.fi.cmp.cmpentity.BusinessType;
import com.yonyoucloud.fi.cmp.cmpentity.PricingParty;
import com.yonyoucloud.fi.cmp.currencyexchange.CurrencyExchange;
import com.yonyoucloud.fi.cmp.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ObjectUtils;

import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 银企联报文拼接工具类
 */
@Slf4j
public class BankEnterpriseAssociation {

    // 结售汇交易结果查询 - SSFE3001
    static final String CURRENCY_EXCHANGE_RESULT_QUERY = "SSFE3001";
    // 即期结售汇交易提交 - SSFE1002
    static final String CURRENCY_EXCHANGE_SUBMIT = "SSFE1002";
    // 即期结售汇交割 - SSFE1003
    static final String CURRENCY_EXCHANGE_DELIVERY = "SSFE1003";
    // 即期结售汇询价 - SSFE1001
    static final String CURRENCY_EXCHANGE_RATE_QUERY = "SSFE1001";
    // 汇入汇款确认提交SSFE1004
    static final String INWARD_REMITTANCE_SUBMIT = "SSFE1004";
    // 汇入汇款确认交易结果查询SSFE3004
    static final String INWARD_REMITTANCE_RESULT_QUERY = "SSFE3004";
    // 汇入汇款待确认业务列表查询SSFE3005
    static final String INWARD_REMITTANCE_LIST_QUERY = "SSFE3005";
    // SSFE3005最大查询条数
    static final String MAX_QUERY_NUM = "10";
    // 汇入汇款业务明细查询SSFE3006
    static final String INWARD_REMITTANCE_DETAIL_QUERY = "SSFE3006";
    // 汇率查询
    static final String RATE_QUERY = "SSFE3012";
    // 即期外汇买卖SSFE1018(提交接口的copy)
    static final String CURRENCY_EXCHANGE_SUBMIT_EACH = "SSFE1018";
    //附件上传
    static final String UPLOAD_FILE = "11SC01";
    // 日元币种编码
    static final String JAPAN_CURRENCY = "JPY";
    // 意大利币种编码
    static final String ITALY_CURRENCY = "ITL";

    /**
     * @return com.yonyou.yonbip.ctm.json.CtmJSONObject
     * @Description 构建请求报文头
     * @Date 2022年12月22日20:23:08
     **/
    public static CtmJSONObject buildRequestHead(String transCode, String operator, String customNo, String requestseqno, String signature) {
        CtmJSONObject requestHead = new CtmJSONObject();
        requestHead.put("version", "2.0.1");
        //请求流水号
        requestHead.put("request_seq_no", requestseqno);
        //客户号，必填，客户在银企联云系统的唯一标识
        requestHead.put("cust_no", customNo);
        //渠道号，必填，接入方编码，由服务方提供
        BankConnectionAdapterContext bankConnectionAdapterContext = AppContext.getBean(BankConnectionAdapterContext.class);
        requestHead.put("cust_chnl", AppContext.getBean(BankConnectionAdapterContext.class).getChanPayCustomChanel());
        LocalDateTime dateTime = LocalDateTime.now();
        //请求日期，必，将式为: YYyyMWdd
        requestHead.put("request_date", DateTimeFormatter.ofPattern(DateUtils.YYYYMMDD).format(dateTime));
        //请求间，以，将式为HHmmss
        requestHead.put("request_time", DateTimeFormatter.ofPattern(DateUtils.HHMMSS).format(dateTime));
        requestHead.put("oper", operator);
        requestHead.put("oper_sign", signature);
        //交易码，必填
        requestHead.put("tran_code", transCode);
        return requestHead;
    }

    public static CtmJSONObject buildRequestHeadNew(String transCode, String operator, String customNo, String requestseqno, String signature) {
        CtmJSONObject requestHead = new CtmJSONObject();
        requestHead.put("version", "2.0.1");
        //请求流水号
        requestHead.put("request_seq_no", requestseqno);
        //客户号，必填，客户在银企联云系统的唯一标识
        requestHead.put("cust_no", customNo);
        //渠道号，必填，接入方编码，由服务方提供
        BankConnectionAdapterContext bankConnectionAdapterContext = AppContext.getBean(BankConnectionAdapterContext.class);
        requestHead.put("cust_chnl", AppContext.getBean(BankConnectionAdapterContext.class).getChanPayCustomChanel());
        LocalDateTime dateTime = LocalDateTime.now();
        //请求日期，必，将式为: YYyyMWdd
        requestHead.put("request_date", DateTimeFormatter.ofPattern(DateUtils.YYYYMMDD).format(dateTime));
        //请求间，以，将式为HHmmss
        requestHead.put("request_time", DateTimeFormatter.ofPattern(DateUtils.HHMMSS).format(dateTime));
        requestHead.put("oper", operator);
        requestHead.put("oper_sign", signature);
        //交易码，必填
        requestHead.put("tran_code", transCode);
        return requestHead;
    }

    /**
     * 【结售汇直联】外部接口，结售汇交易结果查询，SSFE3001，报文拼接
     *
     * @param params
     * @param currencyExchange
     * @return
     * @throws Exception
     */
    public static CtmJSONObject buildReqDataSSFE3001(CtmJSONObject params, CurrencyExchange currencyExchange) throws Exception {
        // TODO 客户号赋默认值，后续删除
        if (StringUtils.isEmpty(params.getString("customNo"))) {
            params.put("customNo", AppContext.getEnvConfig("CUSTNOZL"));
        }
        CtmJSONObject requestHead = BankEnterpriseAssociation.buildRequestHeadNew(CURRENCY_EXCHANGE_RESULT_QUERY, params.getString("operator"), params.getString("customNo"), params.getString("tran_seq_no"), params.getString("signature"));
        CtmJSONObject requestBody = new CtmJSONObject();
        requestBody.put("fxtn_ar_id", currencyExchange.getContractNo());
        requestBody.put("batch_no", currencyExchange.getBatchno());
        CtmJSONObject confirmPlaceOrderMsg = new CtmJSONObject();
        confirmPlaceOrderMsg.put("request_head", requestHead);
        confirmPlaceOrderMsg.put("request_body", requestBody);
        return confirmPlaceOrderMsg;
    }

    /**
     * 币种精度特殊处理：日元、意大利里拉；只保留整数，其他币种2位小数
     * @param currency
     * @return
     */
    private static int getDigital(String currency) {
        if (JAPAN_CURRENCY.equals(currency) || ITALY_CURRENCY.equals(currency)) {
            return 0;
        } else {
            return 2;
        }
    }

    /**
     * 【结售汇直联】外部接口，即期结售汇交易提交，SSFE1002，报文拼接
     *
     * @param params
     * @param currencyExchange
     * @return
     * @throws Exception
     */
    public static CtmJSONObject buildReqDataSSFE1002(CtmJSONObject params, CurrencyExchange currencyExchange) {
        CtmJSONObject requestHead;
        CtmJSONObject requestBody = new CtmJSONObject();
        //申报额度编号	foreign_limit_no	Varchar(50)	O	部分银行售汇需要（兴业）
        requestBody.put("foreign_limit_no", currencyExchange.getForeignlimitno());
        requestBody.put("tran_seq_no", params.getString("tran_seq_no"));
        requestBody.put("batch_no", params.getString("batch_no"));
        //结汇=卖出外汇；售汇=买入外汇
        if(currencyExchange.getFlag().getValue() == Bsflag.Sell.getValue()){
            requestBody.put("trade_flag", "0");
        }else if (currencyExchange.getFlag().getValue() == Bsflag.Buy.getValue()){
            requestBody.put("trade_flag", "1");
        }else if (currencyExchange.getFlag().getValue() == Bsflag.Exchange.getValue()){
            requestBody.put("trade_flag", "2");
        }
        /**
         * 若计价方为买入方：
         *      买入金额-外币兑换工作台.买入金额，卖出金额-空
         * 若计价方为卖出方：
         *      卖出金额-外币兑换工作台.卖出金额，买入金额-空
         */
        if (PricingParty.Buy.getValue() == currencyExchange.getPricingParty()) {
            requestBody.put("cny_amt", currencyExchange.getPurchaseamount().setScale(getDigital(params.getString("buy_curr")), RoundingMode.DOWN).toString());
            requestHead = BankEnterpriseAssociation.buildRequestHeadNew(CURRENCY_EXCHANGE_SUBMIT, params.getString("operator"), params.getString("customNo"), params.getString("tran_seq_no"), params.getString("signature"));
        } else if (PricingParty.Sell.getValue() == currencyExchange.getPricingParty()) {
            requestBody.put("for_amt", currencyExchange.getSellamount().setScale(getDigital(params.getString("sell_curr")), RoundingMode.DOWN).toString());
            requestHead = BankEnterpriseAssociation.buildRequestHeadNew(CURRENCY_EXCHANGE_SUBMIT, params.getString("operator"), params.getString("customNo"), params.getString("tran_seq_no"), params.getString("signature"));
        } else {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100240"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041803B2","方向不允许为空。") /* "方向不允许为空。" */);
        }
        // 业务类型：0-线上自助-对应-BR，1-银行审核-对应-OS
        if (currencyExchange.getBusinessType() != null && currencyExchange.getBusinessType() == BusinessType.BankMargin.getValue()) {
            requestBody.put("bus_type", "BR");
        } else {
            requestBody.put("bus_type", "OS");
        }
        // 延时交割标志：true-延时交割-1，false-实时交割-0
        requestBody.put("delay_flag", currencyExchange.getIsDelayed() ? "1" : "0");
        // 重新查询参照，取返回值
        requestBody.put("sell_acct_no", params.getString("sell_acct_no"));
        requestBody.put("buy_acct_no", params.getString("buy_acct_no"));
        requestBody.put("sell_curr", params.getString("sell_curr"));
        requestBody.put("buy_curr", params.getString("buy_curr"));
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        if (currencyExchange.getDelayedDate() != null) {
            String deliveryDate = sdf.format(currencyExchange.getDelayedDate());
            requestBody.put("delivery_date", deliveryDate);
        } else {
            requestBody.put("delivery_date", currencyExchange.getExpectedDeliveryDate() != null ?
                    sdf.format(currencyExchange.getExpectedDeliveryDate()):new Date());
        }
        // delay_type 0 T+0 1 T+1 2 T+2
        //用来计算交割日期，交割日期=申请日期+交割类型；遇节假日顺延至下一个工作日，申请日期默认为当前日期
        requestBody.put("delay_type", currencyExchange.getDeliverytime());

        // S:实时委托(按交割日当天汇率)-0; G:挂单委托; X:询价委托-1;
        if (currencyExchange.getDelegationType() != null) {
            if (currencyExchange.getDelegationType() == 0) {
                requestBody.put("delegation_type", "S");
            } else if (currencyExchange.getDelegationType() == 1) {
                requestBody.put("delegation_type", "X");
            } else {
                requestBody.put("delegation_type", null);
            }
        } else {
            requestBody.put("delegation_type", null);
        }

        // Map selfInquiry_detail
        Map<String, String> selfInquiry_detail = new HashMap();
        // O:线上询价-0; U:线下询价-1
        if (currencyExchange.getInquiryType() != null) {
            if (currencyExchange.getInquiryType() == 0) {
                selfInquiry_detail.put("inquiry_type", "O");
            } else if (currencyExchange.getInquiryType() == 1) {
                selfInquiry_detail.put("inquiry_type", "U");
            } else {
                selfInquiry_detail.put("inquiry_type", null);
            }
        } else {
            selfInquiry_detail.put("inquiry_type", null);
        }
        selfInquiry_detail.put("inquiry_id", currencyExchange.getInquiryNo());
        if (currencyExchange.getInquiryExchangerate() != null) {
            selfInquiry_detail.put("deal_rate", currencyExchange.getInquiryExchangerate().toString());
        } else {
            // 国机需求，软通接口必输，取财务公司汇率为成交汇率，成交汇率又可编辑，故直接取成交汇率
            selfInquiry_detail.put("deal_rate", currencyExchange.getExchangerate().toString());
        }
        requestBody.put("selfInquiry_detail", selfInquiry_detail);

        // Map selfPend_detail;暂不支持挂单
        Map<String, String> selfPend_detail = new HashMap();
        selfPend_detail.put("pend_end_date", null);
        selfPend_detail.put("pend_rate", null);
        selfPend_detail.put("loss_rate", null);
        requestBody.put("selfPend_detail", selfPend_detail);

        // 1-缴纳保证金（冻结账户可用余额）-0，2-扣减金融市场授信额度-1;delay_flag=1时上送
        if (currencyExchange.getCollateralOccupation() != null) {
            if (currencyExchange.getCollateralOccupation() == 0) {
                requestBody.put("qualified_pledge_type", "1");
            } else if (currencyExchange.getCollateralOccupation() == 1) {
                requestBody.put("qualified_pledge_type", "2");
            } else {
                requestBody.put("qualified_pledge_type", null);
            }
        } else {
            requestBody.put("qualified_pledge_type", null);
        }
        // 0-冻结该笔交易外币账户可用余额作为保证金-0，1-冻结该笔交易人民币账户可用余额作为保证金-1;当合格质押品占用类型值为0时，该字段必输;delay_flag=1时上送
        if (currencyExchange.getMarginAccountFlag() != null) {
            if (currencyExchange.getMarginAccountFlag() == 0) {
                requestBody.put("accno_tp_code", "0");
            } else if (currencyExchange.getMarginAccountFlag() == 1) {
                requestBody.put("accno_tp_code", "1");
            } else {
                requestBody.put("accno_tp_code", null);
            }
        } else {
            requestBody.put("accno_tp_code", null);
        }
        requestBody.put("deposit_acct_no", params.getString("deposit_acct_no"));
        if (currencyExchange.getIsCheckAccount()) {
            requestBody.put("is_check_acct", "Y");
            requestBody.put("check_acct_no", params.getString("sell_acct_no"));
        } else {
            requestBody.put("is_check_acct", "N");
            requestBody.put("check_acct_no", null);
        }

        // Map dec_info
        Map<String, String> dec_info = new HashMap();
        // 0-经常项目 1-资本项目 2-其他
        if (currencyExchange.getProjectType() != null) {
            if (currencyExchange.getProjectType() == 0) {
                dec_info.put("pro_code", "0");
            } else if (currencyExchange.getProjectType() == 1) {
                dec_info.put("pro_code", "1");
            } else {
                dec_info.put("pro_code", null);
            }
        } else {
            dec_info.put("pro_code", null);
        }
        dec_info.put("trade_code", params.getString("trade_code"));
        dec_info.put("st_code", params.getString("st_code"));
        dec_info.put("source_code", params.getString("source_code"));
        dec_info.put("for_ex_useof", params.getString("for_ex_useof"));
        dec_info.put("for_ex_useof_det", currencyExchange.getSettlePurposeDetail());
        dec_info.put("for_ex_num", currencyExchange.getSafeApprovalNo());
        dec_info.put("dec_tel", currencyExchange.getDeclarerPhone());
        dec_info.put("dec_name", currencyExchange.getDeclarer());
        // 取发送接口日期
        dec_info.put("dec_date", LocalDateUtil.getNowString());
        requestBody.put("dec_info", dec_info);

        requestBody.put("remark", currencyExchange.getDescription());
        //附件上传
        if(ObjectUtils.isNotEmpty(params.get("fileInfo"))){
            requestBody.put("file_info", params.get("fileInfo"));
        }else {
            requestBody.put("file_info", currencyExchange.getFileName());
        }
        requestBody.put("obmdef1", params.getString("obmdef1"));
        requestBody.put("obmdef2", params.getString("obmdef2"));
        requestBody.put("obmdef3", params.getString("obmdef3"));
        requestBody.put("extend_data", null);
        CtmJSONObject confirmPlaceOrderMsg = new CtmJSONObject();
        confirmPlaceOrderMsg.put("request_head", requestHead);
        confirmPlaceOrderMsg.put("request_body", requestBody);
        return confirmPlaceOrderMsg;
    }

    /**
     * 【结售汇直联】外部接口，即期结售汇交割，SSFE1003，报文拼接
     *
     * @param vo
     * @param currencyExchange
     * @return
     * @throws Exception
     */
    public static CtmJSONObject buildReqDataSSFE1003(ExchangeDeliveryRequestVO vo, CurrencyExchange currencyExchange) throws JsonProcessingException {
        // TODO 客户号赋默认值，后续删除
        if (StringUtils.isEmpty(vo.getCustomNo())) {
            vo.setCustomNo(AppContext.getEnvConfig("CUSTNODB"));
        }
        CtmJSONObject requestHead = BankEnterpriseAssociation.buildRequestHeadNew(CURRENCY_EXCHANGE_DELIVERY, vo.getOperator(), vo.getCustomNo(), vo.getTran_seq_no(), vo.getSignature());
        vo.setBatch_no(currencyExchange.getBatchno());
        vo.setFxtn_ar_id(currencyExchange.getContractNo());
        // 实体类转JSONObject
        CtmJSONObject requestBody = CtmJSONObject.parseObject(CtmJSONObject.toJSONString(vo));
        CtmJSONObject confirmPlaceOrderMsg = new CtmJSONObject();
        confirmPlaceOrderMsg.put("request_head", requestHead);
        confirmPlaceOrderMsg.put("request_body", requestBody);
        return confirmPlaceOrderMsg;
    }

    /**
     * 【结售汇直联】外部接口，即期结售汇询价，SSFE1001，报文拼接
     *
     * @param params
     * @return
     */
    public static CtmJSONObject buildReqDataSSFE1001(CtmJSONObject params) throws Exception {
        //请求头
        CtmJSONObject requestHead = BankEnterpriseAssociation.buildRequestHeadNew(CURRENCY_EXCHANGE_RATE_QUERY, params.getString("operator"), params.getString("customNo"), params.getString("requestseqno"), params.getString("signature"));

        //请求体
        CtmJSONObject requestBody = new CtmJSONObject();
        CtmJSONObject data = params.getJSONObject("data");
        //交易流水号
        requestBody.put("tran_seq_no", params.getString("tran_seq_no"));
        //业务批次号
        requestBody.put("batch_no", params.getString("batch_no"));
        //交易类型
        String trade_flag = params.getString("trade_flag");
        requestBody.put("trade_flag",trade_flag);
        //延时交割标志
        String deliverytime = data.getString("deliverytime");
        if (!"0".equals(deliverytime)){
            // 1 延时交割
            requestBody.put("delay_flag","1");
        }else{
            // 0 不延时交割
            requestBody.put("delay_flag","0");
        }
        //卖出账号
        requestBody.put("sell_acct_no",params.getString("sell_acct_no"));
        //买入账号
        requestBody.put("buy_acct_no",params.getString("buy_acct_no"));
        //卖出币种
        requestBody.put("sell_curr",params.getString("sell_curr"));
        //买入币种
        requestBody.put("buy_curr",params.getString("buy_curr"));
        if (PricingParty.Buy.getValue() == Short.parseShort(data.getString("pricingParty"))) {
            String purchaseamount = new BigDecimal(data.getString("purchaseamount")).setScale(getDigital(params.getString("buy_curr")), RoundingMode.DOWN).toString();
            requestBody.put("cny_amt", purchaseamount);
        } else if (PricingParty.Sell.getValue() == Short.parseShort(data.getString("pricingParty"))) {
            String sellamount = new BigDecimal(data.getString("sellamount")).setScale(getDigital(params.getString("sell_curr")), RoundingMode.DOWN).toString();
            requestBody.put("for_amt", sellamount);
        } else {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100240"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041803B2","方向不允许为空。") /* "方向不允许为空。" */);
        }
        //延期交割是，以下字段需要上送
        if ("1".equals(requestBody.getString("delay_flag"))){
            //交割日期
            String delayedDate = data.getString("delayedDate").replace("-","");
            String delayedDate1 = delayedDate.substring(0,8);
            requestBody.put("delivery_date",delayedDate1);
            //合格质押品占用类型
            requestBody.put("qualified_pledge_type",data.getString("collateralOccupation"));
            //保证金账户选择标志
            requestBody.put("accno_tp_code",data.getString("marginAccountFlag"));
            //保证金账号
            requestBody.put("deposit_acct_no",params.getString("deposit_acct_no"));
        }
        //项目类别
        requestBody.put("pro_code",data.getString("projectType"));
        //交易编码
        requestBody.put("trade_code",data.getString("transactionCode_code"));
        //统计代码
        requestBody.put("st_code",data.getString("statisticalCode"));
        //结售汇用途代码
        requestBody.put("for_ex_useof",data.getString("statisticalCode"));
        //结售汇详细用途
        requestBody.put("for_ex_useof_det",data.getString("settlePurposeDetail"));
        //外汇局批件号
        requestBody.put("for_ex_num",data.getString("safeApprovalNo"));
        //备注
        requestBody.put("remark",data.getString("description"));
        //文件信息
        requestBody.put("file_info",data.getString("fileName"));
        requestBody.put("is_use_coupon", null);
        requestBody.put("coupon_id", null);
        requestBody.put("cp_ersion", null);
        CtmJSONObject confirmPlaceOrderMsg = new CtmJSONObject();
        confirmPlaceOrderMsg.put("request_head", requestHead);
        confirmPlaceOrderMsg.put("request_body", requestBody);
        return confirmPlaceOrderMsg;
    }

    /**
     * 【结售汇直联】外部接口，汇入汇款确认提交，SSFE1004，报文拼接
     * @param params
     * @return
     * @throws Exception
     */
    public static CtmJSONObject buildReqDataSSFE1004(CtmJSONObject params) throws Exception {
        // TODO 客户号赋默认值，后续删除
        if (StringUtils.isEmpty(params.getString("customNo"))) {
            params.put("customNo", AppContext.getEnvConfig("CUSTNODB"));
        }
        // 报文头拼接
        CtmJSONObject requestHead = BankEnterpriseAssociation.buildRequestHead(INWARD_REMITTANCE_SUBMIT, params.getString("operator"), params.getString("customNo"), params.getString("tranSeqNo"), params.getString("signature"));
        Map paramMap = (Map) ((List)params.get("rows")).get(0);
        InwardRemittanceSubmitRequestVO requestVO =  CtmJSONObject.parseObject(CtmJSONObject.toJSONString(params), InwardRemittanceSubmitRequestVO.class);
        // 汇入汇款编号
        requestVO.setBank_ref_no((String) paramMap.get("inwardremittancecode"));
        // 流水号
        requestVO.setTran_seq_no((String) params.get("tranSeqNo"));
        // 汇入来源：1-境外;2-境内 系统默认为1-境外，可以修改
        requestVO.setRemt_form(paramMap.get("inwardsource").toString());
        // 款项性质：G：货物贸易S：服务贸易C：资本项目O：其他 系统默认为G-货物贸易，可修改
        requestVO.setIrt_type(paramMap.get("natureofpayment").toString());
        // 是否为国外退汇 Y是 N否 默认N-否，可修改
        requestVO.setRefund_flag(paramMap.get("refundflag").toString());
        // 退汇原因
        if (paramMap.get("refundreason") != null) {
            requestVO.setRefund_reason((String) paramMap.get("refundreason"));
        }
        // 用途
        if (paramMap.get("purpose") != null) {
            requestVO.setUse_desc((String) paramMap.get("purpose"));
        }
        // 申报标识：1-跨境申报 2-境内申报 3-外汇账户（内结售汇） 4-不申报(默认)
        requestVO.setRet_type(paramMap.get("declarationmark").toString());
        // 申报标识为不申报时，申报信息不用上送
        if (!"4".equals(paramMap.get("declarationmark").toString())) {
            // 申报信息-子类
            InwardRemittanceSubmitRequestVO.Dec_info decInfo = new InwardRemittanceSubmitRequestVO().new Dec_info();
            // 付款人常驻国家(地区)代码
            assert paramMap.get("payernation_code") != null;
            decInfo.setPayernation_code((String) paramMap.get("payernation_code"));
            // 收款性质：1：货到收汇 2：预收款 3：退款 4：其他
            assert paramMap.get("collectionproperties") != null;
            decInfo.setAmt_type(paramMap.get("collectionproperties").toString());
            // 交易编码
            decInfo.setTrans_code1((String) paramMap.get("transactioncode1_code"));
            // 交易金额
            decInfo.setTrans_amt1(paramMap.get("amount1") == null ? null : paramMap.get("amount1").toString());
            // 交易附言
            decInfo.setTrans_remark1((String) paramMap.get("transactionpostscript1"));
            // 交易编码2
            decInfo.setTrans_code2((String) paramMap.get("transactioncode2_code"));
            // 交易金额2
            if (paramMap.get("amount2") != null) {
                decInfo.setTrans_amt2(paramMap.get("amount2").toString());
            }
            // 交易附言2
            decInfo.setTrans_remark2((String) paramMap.get("transactionpostscript2"));
            // 外汇局批件号/备案号/业务编号
            if (paramMap.get("approvalno") != null) {
                decInfo.setFor_ex_num((String) paramMap.get("approvalno"));
            }
            // 境外收入类型 && 境内收入类型
            if ("1".equals(paramMap.get("declarationmark"))) {
                // 境外收入类型
                if (paramMap.get("overseaincometype") != null) {
                    decInfo.setRetin_type(paramMap.get("overseaincometype").toString());
                }
            } else if ("2".equals(paramMap.get("declarationmark"))) {
                // 境内收入类型
                if (paramMap.get("incometype") != null) {
                    decInfo.setRetin_type(paramMap.get("incometype").toString());
                } else {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100241"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041803B4","申报标识为境内申报时，境内收入类型必输") /* "申报标识为境内申报时，境内收入类型必输" */);
                }
            } else if ("3".equals(paramMap.get("declarationmark"))) {
                // TODO
            }
            // 是否为保税货物项下收汇：Y-是 N-否 申报标识栏位选择“2:境内申报”需要上送
            if (paramMap.get("bodflag") != null) {
                decInfo.setBod_flag(paramMap.get("bodflag").toString());
            }
            // 申报人
            if (paramMap.get("declarer") != null) {
                decInfo.setDec_name(paramMap.get("declarer").toString());
            }
            // 申报人电话
            if (paramMap.get("declarertel") != null) {
                decInfo.setDec_tel(paramMap.get("declarertel").toString());
            }
            // 申报日期 格式yyyyMMdd
            if (paramMap.get("declarationdate") != null) {
                decInfo.setDec_date((paramMap.get("declarationdate").toString()).replace("-", ""));
            }
            requestVO.setDec_info(decInfo);
        }

        // 结汇/现汇信息 - 子类
        InwardRemittanceSubmitRequestVO.Re_info re_info = new InwardRemittanceSubmitRequestVO().new Re_info();
        // 是否本行收款人：0-本行收款人; 1-他行收款人
        if (paramMap.get("bankpayeeflag") != null) {
            re_info.setBoc_flag(paramMap.get("bankpayeeflag").toString());
        }
        // 交易类型：0-现汇;1-结汇
        if (paramMap.get("trantype") != null) {
            re_info.setTran_type(paramMap.get("trantype").toString());
        }
        // 收款人账号
        if (paramMap.get("payeeaccountno") != null) {
            re_info.setPayee_acct_no((String) paramMap.get("payeeaccountno"));
        }
        // 收款人名称
        if (paramMap.get("payeeaccountname") != null) {
            re_info.setPayee_name((String) paramMap.get("payeeaccountname"));
        }
        // 收款行行名
        if (paramMap.get("payeeaccountname") != null) {
            re_info.setRebk_name((String) paramMap.get("payeeaccountname"));
        }
        // 收款行行号
        if (paramMap.get("payeeaccountbankno") != null) {
            re_info.setRebk_no((String) paramMap.get("payeeaccountbankno"));
        }
        requestVO.setRe_info(re_info);
        // 实体类转JSONObject
        CtmJSONObject requestBody = CtmJSONObject.parseObject(CtmJSONObject.toJSONString(requestVO));
        CtmJSONObject confirmPlaceOrderMsg = new CtmJSONObject();
        confirmPlaceOrderMsg.put("request_head", requestHead);
        confirmPlaceOrderMsg.put("request_body", requestBody);
        return confirmPlaceOrderMsg;
    }

    /**
     * 【结售汇直联】外部接口，汇入汇款确认交易结果查询，SSFE3004，报文拼接
     *
     * @param vo
     * @return
     */
    public static CtmJSONObject buildReqDataSSFE3004(InwardRemittanceResultQueryRequestVO vo) throws Exception {
        // TODO 客户号赋默认值，后续删除
        if (StringUtils.isEmpty(vo.getCustomNo())) {
            vo.setCustomNo(AppContext.getEnvConfig("CUSTNODB"));
        }
        CtmJSONObject requestHead = BankEnterpriseAssociation.buildRequestHead(INWARD_REMITTANCE_RESULT_QUERY, vo.getOperator(), vo.getCustomNo(), vo.getTran_seq_no(), vo.getSignature());
        CtmJSONObject requestBody = CtmJSONObject.parseObject(CtmJSONObject.toJSONString(vo));
        CtmJSONObject confirmPlaceOrderMsg = new CtmJSONObject();
        confirmPlaceOrderMsg.put("request_head", requestHead);
        confirmPlaceOrderMsg.put("request_body", requestBody);
        return confirmPlaceOrderMsg;
    }

    /**
     * 【结售汇直联】外部接口，汇入汇款待确认业务列表查询，SSFE3005，报文拼接
     *
     * @param vo
     * @return
     */
    public static CtmJSONObject buildReqDataSSFE3005(InwardRemittanceListQueryRequestVO vo) throws Exception {
        // TODO 客户号赋默认值，后续删除
        if (StringUtils.isEmpty(vo.getCustomNo())) {
            vo.setCustomNo(AppContext.getEnvConfig("CUSTNODB"));
        }
        CtmJSONObject requestHead = BankEnterpriseAssociation.buildRequestHead(INWARD_REMITTANCE_LIST_QUERY, vo.getOperator(), vo.getCustomNo(), vo.getTran_seq_no(), vo.getSignature());
        // 最大查询条数，赋默认值
        vo.setQuery_num(MAX_QUERY_NUM);
        CtmJSONObject requestBody = CtmJSONObject.parseObject(CtmJSONObject.toJSONString(vo));
        CtmJSONObject confirmPlaceOrderMsg = new CtmJSONObject();
        confirmPlaceOrderMsg.put("request_head", requestHead);
        confirmPlaceOrderMsg.put("request_body", requestBody);
        return confirmPlaceOrderMsg;
    }

    /**
     * 【结售汇直联】外部接口，汇入汇款业务明细查询，SSFE3006，报文拼接
     *
     * @param vo
     * @return
     */
    public static CtmJSONObject buildReqDataSSFE3006(InwardRemittanceDetailQueryRequestVO vo) throws Exception {
        CtmJSONObject requestHead = BankEnterpriseAssociation.buildRequestHead(INWARD_REMITTANCE_DETAIL_QUERY, vo.getOperator(), vo.getCustomNo(), vo.getTran_seq_no(), vo.getSignature());
        // 最大查询条数，赋默认值
        vo.setQuery_num(MAX_QUERY_NUM);
        CtmJSONObject requestBody = CtmJSONObject.parseObject(CtmJSONObject.toJSONString(vo));
        CtmJSONObject confirmPlaceOrderMsg = new CtmJSONObject();
        confirmPlaceOrderMsg.put("request_head", requestHead);
        confirmPlaceOrderMsg.put("request_body", requestBody);
        return confirmPlaceOrderMsg;
    }

    public static CtmJSONObject buildReqDataSSFE3012(FinanceCompanyRateQueryRequestVO vo) throws Exception {
        CtmJSONObject requestHead = BankEnterpriseAssociation.buildRequestHeadNew(RATE_QUERY, vo.getOperator(), vo.getCustomNo(), vo.getTran_seq_no(), vo.getSignature());
        CtmJSONObject requestBody = CtmJSONObject.parseObject(CtmJSONObject.toJSONString(vo));
        CtmJSONObject confirmPlaceOrderMsg = new CtmJSONObject();
        confirmPlaceOrderMsg.put("request_head", requestHead);
        confirmPlaceOrderMsg.put("request_body", requestBody);
        return confirmPlaceOrderMsg;
    }


    /**
     * 组装附件上传请求参数
     * @param params
     * @param currencyExchange
     * @return
     */
    public static CtmJSONObject buildReqData11SC01(CtmJSONObject params, CurrencyExchange currencyExchange, CooperationFileInfo fileInfo){
        if (StringUtils.isEmpty(params.getString("customNo"))) {
            params.put("customNo", AppContext.getEnvConfig("CUSTNOZL"));
        }
        CtmJSONObject requestHead = BankEnterpriseAssociation.buildRequestHeadNew(UPLOAD_FILE, params.getString("operator"), params.getString("customNo"), params.getString("tran_seq_no"), params.getString("signature"));
        CtmJSONObject requestBody = new CtmJSONObject();
        requestBody.put("tran_seq_no", params.getString("tran_seq_no"));
        requestBody.put("batch_no", params.getString("batch_no"));
        CooperationFileDownloadService cooperationFileDownloadservice = AppContext.getBean(CooperationFileDownloadService.class);
        InputStream fileStream = null;
        byte[] fileByte = new byte[0];
        try {
            fileStream = cooperationFileDownloadservice.downLoadFile(fileInfo.getFileId());
            fileByte = IOUtils.toByteArray(fileStream);
        } catch (IOException e) {
            log.error("文件下载出错！",e);
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054007A7", "文件下载出错！") /* "文件下载出错！" */);
        }
        String baseData = DatatypeConverter.printBase64Binary(fileByte);
        String fileType = fileInfo.getExtension().substring(1);
        String fileName = fileInfo.getName();
        CtmJSONObject fileNameObj = new CtmJSONObject();
        fileNameObj.put("fileName",fileName);
        //银行账户 = 本方账号
        if (Bsflag.Sell == currencyExchange.getFlag()) {
            //卖出外汇。本方账号=买入银行账户
            requestBody.put("acct_no",params.getString("buy_acct_no"));
            requestBody.put("acct_name",params.getString("buy_acct_no_name"));
        } else if (Bsflag.Buy == currencyExchange.getFlag()) {
            //买入外汇。本方账号=卖出银行账户
            requestBody.put("acct_no",params.getString("sell_acct_no"));
            requestBody.put("acct_name",params.getString("sell_acct_no_name"));
        }

        requestBody.put("file_type",fileType);
        requestBody.put("file_content",baseData);
        requestBody.put("extend_data",fileNameObj);

        CtmJSONObject uploadFileOrderMsg = new CtmJSONObject();
        uploadFileOrderMsg.put("request_head", requestHead);
        uploadFileOrderMsg.put("request_body", requestBody);
        return uploadFileOrderMsg;
    }
}
