package com.yonyoucloud.fi.cmp.currencyexchange.rule;

import com.yonyou.ucf.basedoc.model.CurrencyTenantDTO;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.billclaim.BillClaim;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.basecom.utils.CurrencyUtil;
import com.yonyoucloud.fi.cmp.cmpentity.AssociationStatus;
import com.yonyoucloud.fi.cmp.cmpentity.Bsflag;
import com.yonyoucloud.fi.cmp.common.service.CmCommonService;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.Direction;
import com.yonyoucloud.fi.cmp.util.AccentityUtil;
import com.yonyoucloud.fi.cmp.util.DateUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.yonyoucloud.fi.cmp.constant.ICmpConstant.ID;
import static com.yonyoucloud.fi.cmp.constant.ICmpConstant.VOUCHDATE;

/**
 * @description:
 * @author: wanxbo@yonyou.com
 * @date: 2023/7/12 14:15
 */
@Slf4j
@Component("bankToCurrencyExchangeRule")
public class BankToCurrencyExchangeRule extends AbstractCommonRule {

    @Autowired
    private BaseRefRpcService baseRefRpcService;

    @Autowired
    CmCommonService cmCommonService;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        //经测试不能用对象接收 对接平台与页面都会报异常
        List<Map<String, Object>> omakes = (List) paramMap.get(ICmpConstant.TARLIST);
        Iterator<Map<String, Object>> iterator = omakes.iterator();

        while (iterator.hasNext()) {
            Map<String, Object> map = iterator.next();
//            if (map.get("to_acct_no") != null){
//                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102381"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_189F8A9805C00032","不满足生单条件：对方账号应为空") /* "不满足生单条件：对方账号应为空" */);
//            }
            //flag 0买入外汇；1卖出外汇；2外币兑换
            Short flag = Short.valueOf(map.get("flag").toString());
            // 会计主体默认币种
            String currencyOrg = AccentityUtil.getNatCurrencyIdByAccentityId(map.get("accentity").toString());
            //会计主体本币精度处理
            CurrencyTenantDTO currencyOrgDTO = baseRefRpcService.queryCurrencyById(currencyOrg);
            //会计主体本币名称
            String currencyOrgName = currencyOrgDTO.getName();
            //转单传入的币种
            String currency = map.get("currency").toString();
            String currency_name = map.get("currency_name").toString();

            //借贷方向 1借（支出）2贷（收入）
            Short dc_flag = Short.valueOf(map.get("dc_flag").toString());
            BigDecimal tranAmt = new BigDecimal(map.get("tran_amt").toString());

            //认领单，对账单本方银行账户
            String bankaccount = map.get("bankaccount") != null ? map.get("bankaccount").toString():null;
            String bankaccount_name = map.get("bankaccount_name") != null ? map.get("bankaccount_name").toString():null;

            //币种校验，币种金额赋值
            if (Bsflag.Buy.getValue() == flag){ //买入外汇
                //当借贷方向=借（付），银行对账单/认领单的币种应为该会计主体的本位币；
                if (Direction.Debit.getValue() == dc_flag){
                    if (!currency.equals(currencyOrg)){
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102382"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:YS_CTM_CM-BE_LOCAL_00050039", "不满足生单条件：买入外汇，收付方向为支出时，生单币种应为该资金组织对应会计主体的本位币") /* "不满足生单条件：买入外汇，收付方向为支出时，生单币种应为该资金组织对应会计主体的本位币" */);
                    }
                    //卖出金额
                    map.put("sellamount",tranAmt);
                    //卖出本币金额
                    map.put("sellloaclamount",tranAmt);
                    //卖出账户
                    map.put("sellbankaccount",bankaccount);
                    map.put("sellbankaccount_name",bankaccount_name); //卖出汇率
                    Map<String, Object>  defaultExchangeRateType = cmCommonService.getDefaultExchangeRateType(map.get("accentity").toString());
                    if (defaultExchangeRateType != null){
                        Double currencyRateNew = CurrencyUtil.getCurrencyRateNew(null, defaultExchangeRateType.get(ID).toString(), currency, currencyOrg, (Date) map.get(VOUCHDATE), 6);
                        if (!(currencyRateNew == null || currencyRateNew == 0.0d)) {
                            //卖出汇率
                            map.put("sellrate",currencyRateNew);
                            //卖出本币金额 卖出金额*卖出汇率
                            map.put("sellloaclamount",tranAmt.multiply(new BigDecimal(currencyRateNew)).setScale(currencyOrgDTO.getMoneydigit(), currencyOrgDTO.getMoneyrount()));
                        }
                    }
                }
                //当借贷方向=贷（收），银行对账单/认领单的币种应为该会计主体的非本位币；
                if (Direction.Credit.getValue() == dc_flag){
                    if (currency.equals(currencyOrg)){
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102383"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:YS_CTM_CM-BE_LOCAL_00050035", "不满足生单条件：买入外汇，收付方向为收入时，生单币种应为该资金组织对应会计主体的非本位币") /* "不满足生单条件：买入外汇，收付方向为收入时，生单币种应为该资金组织对应会计主体的非本位币" */);
                    }
                    //买入币种，当借贷方向=贷，根据银行对账单/认领单.币种进行赋值；
                    map.put("purchaseCurrency",currency);
                    map.put("purchaseCurrency_name",currency_name);
                    //买入金额
                    map.put("purchaseamount",tranAmt);
                    //买入账户
                    map.put("purchasebankaccount",bankaccount);
                    map.put("purchasebankaccount_name",bankaccount_name);
                    //买入汇率
                    Map<String, Object>  defaultExchangeRateType = cmCommonService.getDefaultExchangeRateType(map.get("accentity").toString());
                    if (defaultExchangeRateType != null){
                        Double currencyRateNew = CurrencyUtil.getCurrencyRateNew(null, defaultExchangeRateType.get(ID).toString(), currency, currencyOrg, (Date) map.get(VOUCHDATE), 6);
                        if (!(currencyRateNew == null || currencyRateNew == 0.0d)) {
                            //卖出汇率
                            map.put("purchaserate",currencyRateNew);
                            //卖出本币金额 卖出金额*卖出汇率
                            map.put("purchaselocalamount",tranAmt.multiply(new BigDecimal(currencyRateNew)).setScale(currencyOrgDTO.getMoneydigit(), currencyOrgDTO.getMoneyrount()));
                        }
                    }
                }
                //卖出币种，当前会计主体本位币
                map.put("sellCurrency",currencyOrg);
                map.put("sellCurrency_name",currencyOrgName);
            }else if(Bsflag.Sell.getValue() == flag){//卖出外汇
                //当借贷方向=借（付），银行对账单/认领单的币种应为该会计主体的非本位币；
                if (Direction.Debit.getValue() == dc_flag){
                    if (currency.equals(currencyOrg)){
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102384"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:YS_CTM_CM-BE_LOCAL_00050038", "不满足生单条件：卖出外汇，收付方向为支出时，生单币种应为该资金组织对应会计主体的非本位币") /* "不满足生单条件：卖出外汇，收付方向为支出时，生单币种应为该资金组织对应会计主体的非本位币" */);
                    }
                    //卖出币种，根据银行对账单/认领单.币种进行赋值
                    map.put("sellCurrency",currency);
                    map.put("sellCurrency_name",currency_name);
                    //卖出金额
                    map.put("sellamount",tranAmt);
                    //卖出账户
                    map.put("sellbankaccount",bankaccount);
                    map.put("sellbankaccount_name",bankaccount_name);
                }
                //当借贷方向=贷（收），银行对账单/认领单的币种应为该会计主体的本位币
                if (Direction.Credit.getValue() == dc_flag){
                    if (!currency.equals(currencyOrg)){
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102385"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:YS_CTM_CM-BE_LOCAL_00050036", "不满足生单条件：卖出外汇，收付方向为收入时，生单币种应为该资金组织对应会计主体的本位币") /* "不满足生单条件：卖出外汇，收付方向为收入时，生单币种应为该资金组织对应会计主体的本位币" */);
                    }
                    //买入金额
                    map.put("purchaseamount",tranAmt);
                    //买入本币金额
                    map.put("purchaselocalamount",tranAmt);
                    //买入账户
                    map.put("purchasebankaccount",bankaccount);
                    map.put("purchasebankaccount_name",bankaccount_name);
                }
                //买入币种，会计主体本位币
                map.put("purchaseCurrency",currencyOrg);
                map.put("purchaseCurrency_name",currencyOrgName);
            }else if (Bsflag.Exchange.getValue() == flag){//外币兑换
                //当借贷方向=借（付），银行对账单/认领单的币种应为该会计主体的非本位币
                if (Direction.Debit.getValue() == dc_flag){
                    if (currency.equals(currencyOrg)){
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102386"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:YS_CTM_CM-BE_LOCAL_00050037", "不满足生单条件：外币兑换，收付方向为支出时，生单币种应为该资金组织对应会计主体的非本位币") /* "不满足生单条件：外币兑换，收付方向为支出时，生单币种应为该资金组织对应会计主体的非本位币" */);
                    }
                    //借，卖出币种
                    map.put("sellCurrency",currency);
                    map.put("sellCurrency_name",currency_name);
                    //卖出金额
                    map.put("sellamount",tranAmt);
                    //卖出账户
                    map.put("sellbankaccount",bankaccount);
                    map.put("sellbankaccount_name",bankaccount_name);
                    //卖出汇率
                    Map<String, Object>  defaultExchangeRateType = cmCommonService.getDefaultExchangeRateType(map.get("accentity").toString());
                    if (defaultExchangeRateType != null){
                        Double currencyRateNew = CurrencyUtil.getCurrencyRateNew(null, defaultExchangeRateType.get(ID).toString(), currency, currencyOrg, (Date) map.get(VOUCHDATE), 6);
                        if (!(currencyRateNew == null || currencyRateNew == 0.0d)) {
                            //卖出汇率
                            map.put("sellrate",currencyRateNew);
                            //卖出本币金额 卖出金额*卖出汇率
                            map.put("sellloaclamount",tranAmt.multiply(new BigDecimal(currencyRateNew)).setScale(currencyOrgDTO.getMoneydigit(), currencyOrgDTO.getMoneyrount()));
                        }
                    }
                }
                //当借贷方向=贷（收），银行对账单/认领单的币种应为该会计主体的非本位币；
                if (Direction.Credit.getValue() == dc_flag){
                    if (currency.equals(currencyOrg)){
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102387"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:YS_CTM_CM-BE_LOCAL_00050040", "不满足生单条件：外币兑换，收付方向为收入时，生单币种应为该资金组织对应会计主体的非本位币") /* "不满足生单条件：外币兑换，收付方向为收入时，生单币种应为该资金组织对应会计主体的非本位币" */);
                    }
                    //贷，买入币种
                    map.put("purchaseCurrency",currency);
                    map.put("purchaseCurrency_name",currency_name);
                    //买入金额
                    map.put("purchaseamount",tranAmt);
                    //买入账户
                    map.put("purchasebankaccount",bankaccount);
                    map.put("purchasebankaccount_name",bankaccount_name);
                    //买入汇率
                    Map<String, Object>  defaultExchangeRateType = cmCommonService.getDefaultExchangeRateType(map.get("accentity").toString());
                    if (defaultExchangeRateType != null){
                        Double currencyRateNew = CurrencyUtil.getCurrencyRateNew(null, defaultExchangeRateType.get(ID).toString(), currency, currencyOrg, (Date) map.get(VOUCHDATE), 6);
                        if (!(currencyRateNew == null || currencyRateNew == 0.0d)) {
                            //卖出汇率
                            map.put("purchaserate",currencyRateNew);
                            //卖出本币金额 卖出金额*卖出汇率
                            map.put("purchaselocalamount",tranAmt.multiply(new BigDecimal(currencyRateNew)).setScale(currencyOrgDTO.getMoneydigit(), currencyOrgDTO.getMoneyrount()));
                        }
                    }
                }
            }else {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102388"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_189F8A9805C0002B","转单规则交易类型配置错误") /* "转单规则交易类型配置错误" */);
            }

            //买入关联对账单/认领单赋值 16对账单 80认领单
            String datasource = map.get("datasource")!=null ?map.get("datasource").toString() : null;
            //对账单/认领单id
            String billid = map.get("billid")!=null ?map.get("billid").toString() : null;
            //对账单/认领单流水号或code
            String code = map.get("code")!=null ?map.get("code").toString() : null;
            //对账单，认领单来源时间戳
            String sourcePuts;
            if (map.get("sourceMainPubts") instanceof Date) {
                sourcePuts = DateUtils.dateFormat((Date) map.get("sourceMainPubts"), DateUtils.DATE_TIME_PATTERN);
            } else {
                sourcePuts = ((Timestamp) map.get("sourceMainPubts")).toLocalDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            }
            //支出 （借）
            if (Direction.Debit.getValue() == dc_flag){
                //卖出金额
                map.put("sellamount",tranAmt);
                if ("16".equals(datasource)){ //对账单，卖出关联对账单
                    map.put("collectbankbill",billid);
                    map.put("collectbankbill_bank_seq_no",code);
                }
                if ("80".equals(datasource)){ //认领单，卖出关联认领单
                    map.put("collectbillclaim",billid);
                    map.put("collectbillclaim_code",code);
                }
                //CZFW-310410 设置付款方已关联
                map.put("associationStatusPay",AssociationStatus.Associated.getValue());

                //设置收方向时间戳
                map.put("collectSourcePubts", sourcePuts);
            }
            //收入（贷）
            if (Direction.Credit.getValue() == dc_flag){
                //买入金额
                map.put("purchaseamount",tranAmt);
                if ("16".equals(datasource)){ //对账单,买入关联对账单
                    map.put("paybankbill",billid);
                    map.put("paybankbill_bank_seq_no",code);
                }
                if ("80".equals(datasource)){ //认领单，买入认领单
                    map.put("paybillclaim",billid);
                    map.put("paybillclaim_code",code);
                }
                //CZFW-310410 设置收款方已关联
                map.put("associationStatusCollect", AssociationStatus.Associated.getValue());
                //设置付方向时间戳
                map.put("paySourcePubts", sourcePuts);
            }

        }
        //转单数据回填
        paramMap.put(ICmpConstant.TARLIST, omakes);

        return new RuleExecuteResult(paramMap);
    }
}
