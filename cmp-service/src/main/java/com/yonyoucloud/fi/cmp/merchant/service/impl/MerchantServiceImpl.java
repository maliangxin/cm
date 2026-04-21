package com.yonyoucloud.fi.cmp.merchant.service.impl;

import com.yonyou.diwork.exception.BusinessException;
import com.yonyou.iuap.ucf.common.i18n.MessageUtils;
import com.yonyou.ucf.basedoc.model.BdCountryVO;
import com.yonyou.ucf.basedoc.model.rpcparams.country.CountryQueryParam;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.cmp.bankdealdetail.BankDealDetail;
import com.yonyoucloud.fi.cmp.cmpentity.CaObject;
import com.yonyoucloud.fi.cmp.cmpentity.MerchantFlag;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.MerchantConstant;
import com.yonyoucloud.fi.cmp.enums.MerchantOperateEnum;
import com.yonyoucloud.fi.cmp.merchant.MerchantService;
import com.yonyoucloud.fi.cmp.util.*;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 同步 客户和供应商操作
 *
 * @author miaowb
 */
@Service
public class MerchantServiceImpl implements MerchantService {
    @Autowired
    BaseRefRpcService baseRefRpcService;
    @Override
    public String synMerchant(CtmJSONObject params) throws Exception {
        CtmJSONObject dataObj = params.getJSONObject(MerchantConstant.DATA);
        CtmJSONObject custInfo = getCustInfo(dataObj);
        MerchantUtils.checkParam4SaveCust(custInfo);

        // 客户
        BillDataDto billDataDto = new BillDataDto();
        billDataDto.setBillnum(MerchantConstant.AA_MERCHANT);
        billDataDto.setData(custInfo);
        MerchantResult merchantResult = MerchantUtils.operateMerchant(billDataDto, MerchantOperateEnum.SAVE);
        if (merchantResult.getCode() != 200) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100790"),merchantResult.getMessage());
        }
        CtmJSONObject resultObj = CtmJSONObject.parseObject(CtmJSONObject.toJSONString(merchantResult.getData()));
        if (!resultObj.getJSONArray(MerchantConstant.MESSAGES).isEmpty()) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100791"),resultObj.getJSONArray(MerchantConstant.MESSAGES).getString(0));
        }
        updateBankDealDetai(dataObj);
        return ResultMessage.success();
    }

    /**
     * 组装客户信息
     *
     * @param dataObj
     * @return
     * @throws Exception
     */
    private CtmJSONObject getCustInfo(CtmJSONObject dataObj) throws Exception {
        CtmJSONObject jsonObject = new CtmJSONObject();
        jsonObject.put(MerchantConstant.CREATEORG, dataObj.getString(MerchantConstant.ACCENTITY));
        jsonObject.put(MerchantConstant.BELONGORG, dataObj.getString(MerchantConstant.ACCENTITY));
        jsonObject.put(MerchantConstant.CODE, dataObj.getString(MerchantConstant.MERCHANT_CODE));
        jsonObject.put(MerchantConstant.CUSTOMERCLASS,
                dataObj.getString(MerchantConstant.MERCHANTCUSTOMERCLASS));
        jsonObject.put(MerchantConstant.ERPCODE, MerchantUtils.getUUID());
        jsonObject.put(MerchantConstant.STATUS, MerchantConstant.INSERT);
        jsonObject.put(MerchantConstant.NAME, dataObj.getString(MerchantConstant.MERCHANT_NAME));
        jsonObject.put(MerchantConstant.DATA_FROM, MerchantConstant.CM);

        CtmJSONObject financialObj = new CtmJSONObject();
        financialObj.put(MerchantConstant.ERPCODE, dataObj.getString(MerchantConstant.TO_ACCT_NO));
        financialObj.put(MerchantConstant.COUNTRY, dataObj.getString(MerchantConstant.MERCHANTCOUNTRY));
        financialObj.put(MerchantConstant.CURRENCY, dataObj.getString(MerchantConstant.CURRENCY));
        financialObj.put(MerchantConstant.ACCOUNTTYPE, dataObj.getString(MerchantConstant.MERCHANTACCOUNTTYPE));

        String openBank = dataObj.getString(MerchantConstant.MERCHANTOPENBANK);
        financialObj.put(MerchantConstant.OPENBANK, openBank);

        // 银行网点获取银行类别信息
        String openBankName = dataObj.getString(MerchantConstant.MERCHANTOPENBANK_NAME);
        Map<String, Object> bankInfo = MerchantUtils.getBankInfo(openBankName);
        if (MapUtils.isNotEmpty(bankInfo)) {
            financialObj.put(MerchantConstant.BANK, bankInfo.get(MerchantConstant.BANKTYPEID));
        }
        financialObj.put(MerchantConstant.BANKACCOUNT, dataObj.getString(MerchantConstant.TO_ACCT_NO));
        financialObj.put(MerchantConstant.BANKACCOUNTNAME, dataObj.getString(MerchantConstant.TO_ACCT_NAME));
        financialObj.put(MerchantConstant.ISDEFAULT, true);
        financialObj.put(MerchantConstant.STATUS, MerchantConstant.INSERT);

        CtmJSONArray financialArray = new CtmJSONArray();
        financialArray.add(financialObj);
        jsonObject.put(MerchantConstant.MERCHANTAGENTFINANCIALINFOS, financialArray);

        return jsonObject;
    }

    /**
     * 修改交易明细 客商状态
     *
     * @param dataObj
     */
    private void updateBankDealDetai(CtmJSONObject dataObj) throws Exception {
        // 同步客商档案成功 变更交易明细状态 会计主体+对方账号+对方户名
        QueryConditionGroup condition = new QueryConditionGroup();
        condition.addCondition(QueryConditionGroup.and(
                QueryCondition.name(MerchantConstant.TO_ACCT_NO).eq(dataObj.getString(MerchantConstant.TO_ACCT_NO))));
        condition.addCondition(QueryConditionGroup.and(QueryCondition.name(MerchantConstant.TO_ACCT_NAME)
                .eq(dataObj.getString(MerchantConstant.TO_ACCT_NAME))));
        //condition.addCondition(QueryConditionGroup.and(QueryCondition.name(MerchantConstant.MERCHANT_FLAG).is_null()));
        condition.addCondition(QueryConditionGroup.and(
                QueryCondition.name(MerchantConstant.ACCENTITY).eq(dataObj.getString(MerchantConstant.ACCENTITY))));
        QuerySchema querySchemaMin = QuerySchema.create().addSelect(ICmpConstant.SELECT_TOTAL_PARAM);
        querySchemaMin.addCondition(condition);
        List<BankDealDetail> bankDealDetailList = MetaDaoHelper.queryObject(BankDealDetail.ENTITY_NAME, querySchemaMin,
                null);
        if (CollectionUtils.isNotEmpty(bankDealDetailList)) {
            for (BankDealDetail bankDealDetail : bankDealDetailList) {
                bankDealDetail.setMerchant_flag(MerchantFlag.EXIST);
                EntityTool.setUpdateStatus(bankDealDetail);
                MetaDaoHelper.update(BankDealDetail.ENTITY_NAME, bankDealDetail);
            }
        }
    }

    @Override
    public CtmJSONObject getCountryByName(CtmJSONObject params) throws Exception {
        String name = params.getString(MerchantConstant.NAME);
        CountryQueryParam countryQueryParam = new CountryQueryParam();
        countryQueryParam.setName(name);
        BdCountryVO countryVO = baseRefRpcService.queryCountryByCondition(countryQueryParam);
        return CtmJSONObject.parseObject(CtmJSONObject.toJSONString(countryVO));
    }

    @Override
    public CtmJSONObject checkMerchant(CtmJSONObject params) throws Exception {
        CtmJSONObject jsonObj = new CtmJSONObject();
        String id = params.getString(MerchantConstant.ID);
        List<Map<String, Object>> bankDealDetail = MetaDaoHelper.queryById(BankDealDetail.ENTITY_NAME,
                ICmpConstant.SELECT_TOTAL_PARAM, id);
        if (CollectionUtils.isEmpty(bankDealDetail)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100792"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180262","数据不存在，请刷新重试") /* "数据不存在，请刷新重试" */);
        }
        Map<String, Object> bankDealDetailMap = bankDealDetail.get(0);
        String accName = bankDealDetailMap.getOrDefault(MerchantConstant.TO_ACCT_NAME, "").toString();

        String accNo = bankDealDetailMap.getOrDefault(MerchantConstant.TO_ACCT_NO, "").toString();
        String currency = bankDealDetailMap.getOrDefault(MerchantConstant.CURRENCY, "").toString();
        String accentity = bankDealDetailMap.get(MerchantConstant.ACCENTITY).toString();
        if (StringUtils.isEmpty(accentity) || StringUtils.isEmpty(accName) || StringUtils.isEmpty(accNo)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100793"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180261","数据不符合条件，无法同步") /* "数据不符合条件，无法同步" */);
        }
        // 查询客户名称是否存在
        MerchantRequst requst = new MerchantRequst(accentity, accName, accNo);
        CtmJSONObject cust2Check = MerchantUtils.cust2Check(requst);
        if (MerchantConstant.TRUE.equals(cust2Check.getString(MerchantConstant.CUSTOMERFLAG))) {
            // 冗余 客户名称存在 且账号存在 无需同步 银行交易明细设置merchant_flag设置为2(已同步)
            BankDealDetail bankDeal = new BankDealDetail();
            bankDeal.setId(bankDealDetail.get(0).get(MerchantConstant.ID));
            bankDeal.setMerchant_flag(MerchantFlag.EXIST);
            EntityTool.setUpdateStatus(bankDeal);
            MetaDaoHelper.update(BankDealDetail.ENTITY_NAME, bankDeal);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100794"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180260","该客户信息已经存在,无需同步") /* "该客户信息已经存在,无需同步" */);
        }
        // 查询供应商是否存在
        CtmJSONObject vendor2Check = MerchantUtils.vendor2Check(requst);
        if (MerchantConstant.TRUE.equals(vendor2Check.getString(MerchantConstant.VENDORFLAG))) {
            // 冗余 客户名称存在 且账号存在 无需同步 银行交易明细设置merchant_flag设置为2(已同步)
            BankDealDetail bankDeal = new BankDealDetail();
            bankDeal.setId(bankDealDetail.get(0).get(MerchantConstant.ID));
            bankDeal.setMerchant_flag(MerchantFlag.EXIST);
            EntityTool.setUpdateStatus(bankDeal);
            MetaDaoHelper.update(BankDealDetail.ENTITY_NAME, bankDeal);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100795"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180263","该供应商信息已经存在,无需同步") /* "该供应商信息已经存在,无需同步" */);
        }
        // 返回客户名称和银行网点信息
        jsonObj.put(MerchantConstant.TO_ACCT_NAME, accName);
        // 开户行名 作为银行网点 去获取类别信息
        String openBankName = bankDealDetailMap.getOrDefault(MerchantConstant.TO_ACCT_BANK_NAME, "").toString();
        if(!StringUtils.isEmpty(openBankName)){
            Map<String, Object> bankInfo = MerchantUtils.getBankInfo(openBankName);
            if (MapUtils.isNotEmpty(bankInfo)) {
                jsonObj.put(MerchantConstant.BANKDOTID, bankInfo.get(MerchantConstant.BANKDOTID));
                jsonObj.put(MerchantConstant.BANKDOTNAME, openBankName);
            }
        }else{
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054007D4", "对方开户行名不能为空") /* "对方开户行名不能为空" */);
        }
        // 客户和供应商
        jsonObj.put(MerchantConstant.MERCHANT_FLAG, CaObject.Customer.getValue());
        return jsonObj;
    }

}
