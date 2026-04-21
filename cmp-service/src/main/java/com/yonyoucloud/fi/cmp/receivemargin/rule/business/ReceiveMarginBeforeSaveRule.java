package com.yonyoucloud.fi.cmp.receivemargin.rule.business;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.yonyou.iuap.yms.lock.YmsScopeLockManager;
import com.yonyou.ucf.basedoc.model.CurrencyTenantDTO;
import com.yonyou.ucf.basedoc.model.ExchangeRateTypeVO;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.ucf.transtype.model.BdTransType;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.cmp.cmpentity.MarginFlag;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.common.service.exchangerate.CmpExchangeRateUtils;
import com.yonyoucloud.fi.cmp.common.service.exchangerate.CmpExchangeRateVO;
import com.yonyoucloud.fi.cmp.constant.IBussinessConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.marginworkbench.MarginWorkbench;
import com.yonyoucloud.fi.cmp.receivemargin.ReceiveMargin;
import com.yonyoucloud.fi.cmp.util.AccentityUtil;
import com.yonyoucloud.fi.cmp.util.QueryBaseDocUtils;
import com.yonyoucloud.fi.cmp.util.basedoc.TransTypeQueryService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 收到保证金保存前规则*
 *
 * @author xuxbo
 * @date 2023/8/3 10:09
 */

@Slf4j
@Component
public class ReceiveMarginBeforeSaveRule extends AbstractCommonRule {

    public static final String OLD_RECEIVEMARGIN = "OLD_RECEIVEMARGIN";
    Map<String, ExchangeRateTypeVO> defExchangeRateTypeMap = new HashMap<>();// 组织默认汇率类型
    Map<String, ExchangeRateTypeVO> exchangeRateTypeMap = new HashMap<>();// 汇率类型档案
    Map<String, String> natCurrencyMap = new HashMap<>();// 组织本币
    Map<String, CurrencyTenantDTO> currencyCacheMap = new HashMap<>();// 币种档案
    @Autowired
    private BaseRefRpcService baseRefRpcService;
    @Autowired
    TransTypeQueryService transTypeQueryService;

    @Autowired
    @Qualifier("ymsGlobalScopeLockManager")
    protected YmsScopeLockManager ymsScopeLockManager;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        List<BizObject> bills = getBills(billContext, map);
        BillDataDto billDataDto = (BillDataDto) getParam(map);
        boolean importFlag = "import".equals(billDataDto.getRequestAction());
        for (BizObject bizObject : bills) {
            boolean openApiFlag = (!bizObject.containsKey("_fromApi") || bizObject.get("_fromApi").equals(false)) && !billDataDto.getFromApi();
            if (importFlag || openApiFlag) {
                initSaveData(bizObject, billContext.getBillnum());
            }
            
            ReceiveMargin receiveMargin = (ReceiveMargin) bizObject;
            //和马良沟通的结果，在保存前，根据原始业务号是否==单据编号来塞入手动/自动的变量
            String marginbusinessno = receiveMargin.getMarginbusinessno();
            String code = receiveMargin.getCode();
            if(!Strings.isNullOrEmpty(code) && code.equals(marginbusinessno)){
                bizObject.set("autoBusinessNo",true);
            }else{
                bizObject.set("autoBusinessNo",false);
            }
            if (ObjectUtils.isEmpty(receiveMargin.getSettleflag())) {
                receiveMargin.setSettleflag((short) 0);
            }
            if (ObjectUtils.isNotEmpty(receiveMargin.getId())) {
                ReceiveMargin oldReceiveMargin = MetaDaoHelper.findById(ReceiveMargin.ENTITY_NAME, receiveMargin.getId(), 1);
                receiveMargin.put(OLD_RECEIVEMARGIN, oldReceiveMargin);
            }
            checkReceiveMarginVirtualAccountBalance(receiveMargin);
        }
        return new RuleExecuteResult();
    }


    private void checkReceiveMarginVirtualAccountBalance(ReceiveMargin receiveMargin) throws Exception {
        //校验保证金虚拟户的可用余额
        BdTransType transType = transTypeQueryService.findById(receiveMargin.getTradetype());
        String receiveMargin_ext = "";
        if (ObjectUtils.isNotEmpty(transType.getExtendAttrsJson())) {
            CtmJSONObject jsonObject = CtmJSONObject.parseObject(transType.getExtendAttrsJson());
            if (ObjectUtils.isNotEmpty(jsonObject.get("receivemargin_ext"))) {
                receiveMargin_ext = jsonObject.get("receivemargin_ext").toString();
            }
        }
        //退还保证金的时候 需要校验虚拟户的金额
        //1.先判断是否转换保证金  非转换的 校验取回金额
        //2.转换的需要校验转换金额+取回金额
        if (transType.getCode().equals("cmp_receivemargin_return") || receiveMargin_ext.equals("cmp_receivemargin_return")) {
            Short conversionmarginflag = receiveMargin.getConversionmarginflag();
            BigDecimal sumamount = new BigDecimal("0.0");
            BigDecimal marginamount = new BigDecimal("0.0");
            BigDecimal conversionamount = new BigDecimal("0.0");
            if (receiveMargin.getEntityStatus() == EntityStatus.Insert) {
                marginamount = receiveMargin.getMarginamount();
                conversionamount = receiveMargin.getConversionamount();
            } else if (receiveMargin.getEntityStatus() == EntityStatus.Update) {
                ReceiveMargin receiveMargin_old = MetaDaoHelper.findById(ReceiveMargin.ENTITY_NAME, receiveMargin.getId(), 1);
                //保存前金额
                BigDecimal marginamount_old = receiveMargin_old.getMarginamount();
                marginamount = receiveMargin.getMarginamount().subtract(marginamount_old);
                if (receiveMargin_old.getConversionmarginflag() == 1 && receiveMargin.getConversionmarginflag() == 1) {
                    BigDecimal conversionamount_old = receiveMargin_old.getConversionamount();
                    conversionamount = receiveMargin.getConversionamount().subtract(conversionamount_old);
                } else if (receiveMargin_old.getConversionmarginflag() == 0 && receiveMargin.getConversionmarginflag() == 1){
                    conversionamount = receiveMargin.getConversionamount();
                }
            }

            if (ObjectUtils.isEmpty(marginamount)) {
                marginamount = new BigDecimal("0.0");
            }
            if (ObjectUtils.isEmpty(conversionamount)) {
                conversionamount = new BigDecimal("0.0");
            }
            //是否转换
            if (conversionmarginflag == 1) {
                // 相加
                sumamount = marginamount.add(conversionamount);
            } else {
                sumamount = marginamount;
            }
            MarginWorkbench marginWorkbench = new MarginWorkbench();
            if (ObjectUtils.isEmpty(receiveMargin.getMarginvirtualaccount())) {
                String marginbusinessno = receiveMargin.getMarginbusinessno();
                QuerySchema querySchema = QuerySchema.create().addSelect("*");
                QueryConditionGroup queryConditionGroup = new QueryConditionGroup(ConditionOperator.and);
                queryConditionGroup.addCondition(QueryCondition.name(ICmpConstant.MARGINBUSINESSNO).eq(marginbusinessno));
                queryConditionGroup.addCondition(QueryCondition.name("marginFlag").eq(MarginFlag.RecMargin.getValue()));
                querySchema.appendQueryCondition(queryConditionGroup);
                List<MarginWorkbench> marginWorkbenchList = MetaDaoHelper.queryObject(MarginWorkbench.ENTITY_NAME, querySchema, null);
                if (ObjectUtils.isNotEmpty(marginWorkbenchList)) {
                    marginWorkbench = marginWorkbenchList.get(0);
                }
            } else {
                String id = receiveMargin.getMarginvirtualaccount().toString();
                marginWorkbench = MetaDaoHelper.findById(MarginWorkbench.ENTITY_NAME,id);
            }
            if (ObjectUtils.isEmpty(marginWorkbench)) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100982"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1CA6198604C00004", "退还保证金的时候未查询到虚拟户信息，请检查！") /* "退还保证金的时候未查询到虚拟户信息，请检查！" */);
            }
            //针对虚拟户上锁，直到事务结束，避免并发
            if (!ymsScopeLockManager.tryTxScopeLock(marginWorkbench.getId().toString())) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101500"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_19096F5A04280012", "已有他人操作同一保证金虚拟账户，请稍候重试！") /* "已有他人操作同一保证金虚拟账户，请稍候重试！" */);
            }
            //校验金额
            BigDecimal marginAvailableBalance = marginWorkbench.getMarginAvailableBalance();
            BigDecimal difference = marginAvailableBalance.subtract(sumamount);
            if (difference.compareTo(BigDecimal.ZERO) < 0) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100170"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1CA34D0604C00009", "该单据保证金发生额大于“保证金可用余额”，请检查！") /* "该单据保证金发生额大于“保证金可用余额”，请检查！" */);
            }
        }
    }

    private void initSaveData(BizObject bizObject, String billNum) throws Exception {
        // 初始化币种信息
        initSaveDataOfCurrency(bizObject);
        // 汇率类型
        ExchangeRateTypeVO exchangeRateType = null;
        if (bizObject.get("exchangeratetype") == null) {
            if (defExchangeRateTypeMap != null && defExchangeRateTypeMap.get(bizObject.get(IBussinessConstant.ACCENTITY)) != null) {
                exchangeRateType = defExchangeRateTypeMap.get(bizObject.get(IBussinessConstant.ACCENTITY));
            } else {
                exchangeRateType = CmpExchangeRateUtils.getNewExchangeRateType(bizObject.get(IBussinessConstant.ACCENTITY), false);
                defExchangeRateTypeMap.put(bizObject.get(IBussinessConstant.ACCENTITY), exchangeRateType);
                exchangeRateTypeMap.put(exchangeRateType.getId(), exchangeRateType);
            }
        } else {
            if (exchangeRateTypeMap != null && exchangeRateTypeMap.get(bizObject.get("exchangeratetype")) != null) {
                exchangeRateType = exchangeRateTypeMap.get(bizObject.get("exchangeratetype"));
            } else {
                Map<String, Object> oriExchangeRateType = QueryBaseDocUtils.queryExchangeRateTypeById(bizObject.get("exchangeratetype")).get(0);
                ObjectMapper mapper = com.yonyou.yonbip.ctm.json.ObjectMapperUtils.objectMapper;
                mapper.convertValue(oriExchangeRateType, ExchangeRateTypeVO.class);
                exchangeRateTypeMap.put(bizObject.get("exchangeratetype"), exchangeRateType);
            }
        }
        if (exchangeRateType != null) {
            bizObject.set("exchangeratetype", exchangeRateType.getId());
            bizObject.set("exchangeratetype_digit", exchangeRateType.getDigit());
            bizObject.set("exchangeratetype_name", exchangeRateType.getName());
        }
        // 汇率（取汇率表中报价日期小于等于单据日期的值）
        if (bizObject.get("natCurrency").equals(bizObject.get("currency"))) {
            bizObject.set(ICmpConstant.EXCHRATE, new BigDecimal("1"));
        }

        boolean notDefineExchangeRateType = bizObject.get("exchangeratetype_code") == null || (bizObject.get("exchangeratetype_code") != null && !bizObject.get("exchangeratetype_code").toString().equals("02"));
        //补充汇率折算方式
        if (bizObject.get("exchRateOps") == null && notDefineExchangeRateType ) {
            CmpExchangeRateVO mainCmpExchangeRateVO = CmpExchangeRateUtils.getNewExchangeRateWithMode(bizObject.get(ICmpConstant.CURRENCY),
                    bizObject.get(ICmpConstant.NATCURRENCY), bizObject.get(ICmpConstant.VOUCHDATE), bizObject.get("exchangeratetype"));
            bizObject.set("exchRateOps", mainCmpExchangeRateVO.getExchangeRateOps());
            bizObject.set("exchRate", mainCmpExchangeRateVO.getExchangeRate());
        }
        if(!notDefineExchangeRateType){
            bizObject.set("exchRateOps", 1);
        }
    }

    /**
     * 初始化币种信息
     *
     * @param bizObject
     * @throws Exception
     */
    private void initSaveDataOfCurrency(BizObject bizObject) throws Exception {
        // 组织本币
        String natCurrency = null;
        if (natCurrencyMap != null && natCurrencyMap.get(bizObject.get("currency")) != null) {
            natCurrency = natCurrencyMap.get(bizObject.get(IBussinessConstant.ACCENTITY));
        } else {
            natCurrency = AccentityUtil.getNatCurrencyIdByAccentityId(bizObject.get(IBussinessConstant.ACCENTITY));
            if (StringUtils.isEmpty(natCurrency)) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100959"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180807", "会计主体[") /* "会计主体[" */ + bizObject.get("accentity_name") + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180806", "]组织本币币种为空!") /* "]组织本币币种为空!" */);
            }
        }
        // 本币
        CurrencyTenantDTO natCurrencyTenantDTO = getCurrencyMapByID(natCurrency);
        if (natCurrencyTenantDTO != null) {
            bizObject.set("natCurrency", natCurrencyTenantDTO.getId());
            bizObject.set("natCurrency_name", natCurrencyTenantDTO.getName());
            bizObject.set("natCurrency_priceDigit", natCurrencyTenantDTO.getPricedigit());
            bizObject.set("natCurrency_moneyDigit", natCurrencyTenantDTO.getMoneydigit());
        }
        // 币种
        CurrencyTenantDTO currencyTenantDTO = getCurrencyMapByID(bizObject.get("currency"));
        if (currencyTenantDTO != null) {
            bizObject.set("currency", currencyTenantDTO.getId());
            bizObject.set("currency_name", currencyTenantDTO.getName());
            bizObject.set("currency_priceDigit", currencyTenantDTO.getPricedigit());
            bizObject.set("currency_moneyDigit", currencyTenantDTO.getMoneydigit());
        }
    }

    private CurrencyTenantDTO getCurrencyMapByID(String currency) throws Exception {
        if (currencyCacheMap != null && currencyCacheMap.get(currency) != null) {
            return currencyCacheMap.get(currency);
        } else {
            CurrencyTenantDTO currencyTenantDTO = baseRefRpcService.queryCurrencyById(currency);
            if (currencyTenantDTO != null) {
                currencyCacheMap.put(currency, currencyTenantDTO);
                return currencyCacheMap.get(currency);
            }
        }
        return null;
    }
}
