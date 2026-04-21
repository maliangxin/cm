package com.yonyoucloud.fi.cmp.inwardremittance;

import com.yonyou.iuap.ucf.common.i18n.MessageUtils;
import com.yonyou.iuap.yms.id.generator.YmsOidGenerator;
import com.yonyou.ucf.basedoc.model.BdCountryVO;
import com.yonyou.ucf.basedoc.model.CurrencyTenantDTO;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.yonbip.ctm.bankconnection.BankConnectionAdapterContext;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.logger.business.CTMCMPBusinessLogService;
import com.yonyou.yonbip.ctm.security.signature.CtmSignatureService;
import com.yonyoucloud.fi.cmp.bankaccountsetting.BankAccountSetting;
import com.yonyoucloud.fi.cmp.constant.IMsgConstant;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import com.yonyoucloud.fi.cmp.enums.InwardStatus;
import com.yonyoucloud.fi.cmp.exchangesettlement.ExchangeSettlementTradeCode;
import com.yonyoucloud.fi.cmp.util.*;
import com.yonyoucloud.fi.cmp.util.basedoc.CountryQueryService;
import com.yonyoucloud.fi.cmp.util.basedoc.CurrencyQueryService;
import com.yonyoucloud.fi.cmp.vo.InwardRemittanceDetailQueryRequestVO;
import com.yonyoucloud.fi.cmp.vo.InwardRemittanceListQueryRequestVO;
import com.yonyoucloud.fi.cmp.vo.InwardRemittanceResultQueryRequestVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.message.BasicNameValuePair;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 汇入汇款实现类
 *
 * @author lidchn 2023年2月8日14:16:28
 */
@Service
@Slf4j
@Transactional(rollbackFor = RuntimeException.class)
public class InwardRemittanceServiceImpl implements InwardRemittanceService {

    @Autowired
    private CTMCMPBusinessLogService ctmcmpBusinessLogService;
    @Autowired
    private CtmSignatureService signatureService;
    @Autowired
    YmsOidGenerator ymsOidGenerator;
    @Autowired
    CountryQueryService countryQueryService;
    @Autowired
    CurrencyQueryService currencyQueryService;

    @Autowired
    BankConnectionAdapterContext bankConnectionAdapterContext;

    @Override
    public CtmJSONObject inwardRemittanceSubmit(CtmJSONObject param) throws Exception {
        Map requestMap = (Map)((List)param.get("rows")).get(0);
        String inwardRemittanceCode = (String) requestMap.get("inwardremittancecode");
        param.put("tranSeqNo", ymsOidGenerator.nextStrId());
        CtmJSONObject placeOrderMsg = BankEnterpriseAssociation.buildReqDataSSFE1004(param);
        String placeOrderString = CtmJSONObject.toJSONString(placeOrderMsg);
        String signMsg = bankConnectionAdapterContext.chanPaySignMessage(placeOrderString);
        List<BasicNameValuePair> requestData = new ArrayList<>();
        requestData.add(new BasicNameValuePair("reqData", placeOrderString));
        requestData.add(new BasicNameValuePair("reqSignData", signMsg));
        CtmJSONObject result = new CtmJSONObject();
        CtmJSONObject responseBody;
        //if (HttpsUtils.isWhiteUrl(bankConnectionAdapterContext.getChanPayUri())) {
            result = HttpsUtils.doHttpsPostNew(INWARD_REMITTANCE_SUBMIT, requestData, bankConnectionAdapterContext.getChanPayUri());
        //}
        CtmJSONObject logJsonObject = new CtmJSONObject();
        logJsonObject.put("requestData", requestData);
        logJsonObject.put("responseData", result);
        ctmcmpBusinessLogService.saveBusinessLog(logJsonObject, "INWARD_REMITTANCE_SUBMIT", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00236", "汇入汇款确认提交") /* "汇入汇款确认提交" */, IServicecodeConstant.INWARD_REMITTANCE, IMsgConstant.INWARD_REMITTANCE, IMsgConstant.INWARD_REMITTANCE_SUBMIT);
        if (result.getInteger("code") == 1) {
            CtmJSONObject responseHead = result.getJSONObject("data").getJSONObject("response_head");
            String serviceStatus = responseHead.getString("service_status");
            if (("00").equals(serviceStatus)) {
                if (result.getJSONObject("data") == null || result.getJSONObject("data").getJSONObject("response_body") == null) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100288"),responseHead.get("service_resp_desc").toString());
                }
                responseBody = result.getJSONObject("data").getJSONObject("response_body");
                String entryStatus = (String) responseBody.get("entry_status");
                if (StringUtils.isEmpty(entryStatus)) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100289"),responseHead.get("service_resp_desc").toString());
                }
                InwardRemittance inwardRemittance;
                switch (entryStatus) {
                    case "01":
                        // 成功，更新单据状态
                        inwardRemittance = getInwardRemittance(inwardRemittanceCode);
                        inwardRemittance.setInwardstatus(InwardStatus.SUCCESS.getIndex());
                        break;
                    case "02":
                        // 处理中，更新单据状态
                        inwardRemittance = getInwardRemittance(inwardRemittanceCode);
                        inwardRemittance.setInwardstatus(InwardStatus.PROCESSING.getIndex());
                        break;
                    case "03":
                        // 失败，将失败状态入库，下次不再查询该笔数据
                        inwardRemittance = getInwardRemittance(inwardRemittanceCode);
                        inwardRemittance.setInwardstatus(InwardStatus.FAIL.getIndex());
                        break;
                    default:
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100290"),responseHead.get("service_resp_desc").toString());
                }
                // 提交子表数据保存入库
                List<InwardRemittance_b> list = inwardRemittance.get("InwardRemittance_b");
                if (list == null || list.size() == 0) {
                    // 之前未提交过，直接插入数据
                    InwardRemittance_b inwardRemittance_b = setInwardRemittance_b(null, requestMap);
                    inwardRemittance_b.setMainid(inwardRemittance.getId());
                    inwardRemittance_b.setEntityStatus(EntityStatus.Insert);
                    MetaDaoHelper.update(InwardRemittance_b.ENTITY_NAME, inwardRemittance_b);
                } else {
                    // 之前提交过，有数据，需覆盖更新
                    InwardRemittance_b inwardRemittance_b = setInwardRemittance_b(list.get(0), requestMap);
                    inwardRemittance_b.setEntityStatus(EntityStatus.Update);
                    MetaDaoHelper.update(InwardRemittance_b.ENTITY_NAME, inwardRemittance_b);
                }
                inwardRemittance.setEntityStatus(EntityStatus.Update);
                MetaDaoHelper.update(InwardRemittance.ENTITY_NAME, inwardRemittance);
            } else {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100291"),responseHead.get("service_resp_desc").toString());
            }
        } else {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100292"),MessageUtils.getMessage("P_YS_CTM_CM-BE_1736383130981892107") /* "汇入汇款确认提交，SSFE1004接口调用异常" */);
        }
        return responseBody;
    }

    /**
     * map转InwardRemittance_b子表实体类
     * @param inwardRemittance_b
     * @param map
     * @return
     */
    private InwardRemittance_b setInwardRemittance_b(InwardRemittance_b inwardRemittance_b, Map map) throws ParseException {
        if (inwardRemittance_b == null) {
            inwardRemittance_b = new InwardRemittance_b();
            inwardRemittance_b.setId(ymsOidGenerator.nextId());
        }
        inwardRemittance_b.setInwardsource(Short.parseShort(map.get("inwardsource").toString()));
        inwardRemittance_b.setNatureofpayment((String) map.get("natureofpayment"));
        inwardRemittance_b.setRefundflag((String) map.get("refundflag"));
        inwardRemittance_b.setRefundreason((String) map.get("refundreason"));
        inwardRemittance_b.setPurpose((String) map.get("purpose"));
        inwardRemittance_b.setDeclarationmark(Short.parseShort(map.get("declarationmark").toString()));
        if (!"4".equals(map.get("declarationmark").toString())) {
            inwardRemittance_b.setPayernation_code((String) map.get("payernationid"));
            inwardRemittance_b.setCollectionproperties(Short.parseShort(map.get("collectionproperties").toString()));
            inwardRemittance_b.setTransactioncode1(Long.parseLong((String) map.get("transactioncode1")));
            inwardRemittance_b.setAmount1(new BigDecimal(map.get("amount1").toString()));
            inwardRemittance_b.setTransactionpostscript1((String) map.get("transactionpostscript1"));
            if (map.get("transactioncode2") != null) {
                inwardRemittance_b.setTransactioncode2(Long.parseLong((String) map.get("transactioncode2")));
            }
            if (map.get("amount2") != null) {
                inwardRemittance_b.setAmount2(new BigDecimal(map.get("amount2").toString()));
            }
            inwardRemittance_b.setTransactionpostscript2((String) map.get("transactionpostscript2"));
            inwardRemittance_b.setApprovalno((String) map.get("approvalno"));
            inwardRemittance_b.setOverseaincometype((String) map.get("overseaincometype"));
            inwardRemittance_b.setIncometype((String) map.get("incometype"));
            inwardRemittance_b.setBodflag((String) map.get("bodflag"));
            inwardRemittance_b.setDeclarer((String) map.get("declarer"));
            inwardRemittance_b.setDeclarertel((String) map.get("declarertel"));
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            inwardRemittance_b.setDeclarationdate(sdf.parse((String) map.get("declarationdate")));
        }
        if (map.get("bankpayeeflag") != null) {
            inwardRemittance_b.setBankpayeeflag(Short.parseShort(map.get("bankpayeeflag").toString()));
        }
        if (map.get("trantype") != null) {
            inwardRemittance_b.setTrantype(Short.parseShort(map.get("trantype").toString()));
        }
        inwardRemittance_b.setPayeeaccountno((String) map.get("payeeaccountno"));
        inwardRemittance_b.setPayeeaccountname((String) map.get("payeeaccountname"));
        inwardRemittance_b.setPayeeaccountbankname((String) map.get("payeeaccountbankname"));
        inwardRemittance_b.setPayeeaccountbankno((String) map.get("payeeaccountbankno"));
        return inwardRemittance_b;
    }

    /**
     * 根据汇入汇款编号查询汇入汇款单
     * @param code
     * @return
     * @throws Exception
     */
    private InwardRemittance getInwardRemittance(String code) throws Exception {
        QueryConditionGroup condition = new QueryConditionGroup();
        condition.addCondition(QueryConditionGroup.and(QueryCondition.name("inwardremittancecode").eq(code)));
        QuerySchema querySchema = QuerySchema.create().addSelect("id");
        querySchema.addCondition(condition);
        List<Map<String, Object>> list = MetaDaoHelper.query(InwardRemittance.ENTITY_NAME, querySchema);
        // 正常都会有值的
        if (list == null || list.size() == 0) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100293"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00233", "单据已删除") /* "单据已删除" */);
        } else {
            // 汇入汇款编号全局唯一，可以保证只有一条数据
            String id = list.get(0).get("id").toString();
            InwardRemittance inwardRemittance = MetaDaoHelper.findById(InwardRemittance.ENTITY_NAME, id);
            inwardRemittance.setEntityStatus(EntityStatus.Update);
            return inwardRemittance;
        }
    }

    @Override
    public void inwardRemittanceResultQuery(CtmJSONObject param) throws Exception {
        if (param.get("data") instanceof List) {
            // 多条数据
            List list = (List) param.get("data");
            if (list.size() == 0) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100294"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00234", "请选择一条数据") /* "请选择一条数据" */);
            }
            for (Object obj : list) {
                Map paramMap = (Map) obj;
                doInwardRemittanceResultQuery(paramMap);
            }
        } else {
            // 单条数据，行按钮
            doInwardRemittanceResultQuery((Map) param.get("data"));
        }
    }


    public CtmJSONObject doInwardRemittanceResultQuery(Map param) throws Exception {
        // 单据状态校验
        if (param.get("inwardstatus") == null || InwardStatus.PROCESSING.getIndex() !=  Short.parseShort(param.get("inwardstatus").toString())) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100295"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00235", "单据【汇入状态】不为【处理中】，不能查询汇入状态") /* "单据【汇入状态】不为【处理中】，不能查询汇入状态" */);
        }
        // 汇入汇款编号
        InwardRemittanceResultQueryRequestVO requestVO = new InwardRemittanceResultQueryRequestVO();
        requestVO.setTran_seq_no(ymsOidGenerator.nextStrId());
        String inwardRemittanceCode = (String) (param.get("inwardremittancecode"));
        requestVO.setBank_ref_no(inwardRemittanceCode);
        CtmJSONObject placeOrderMsg = BankEnterpriseAssociation.buildReqDataSSFE3004(requestVO);
        String placeOrderString = CtmJSONObject.toJSONString(placeOrderMsg);
        String signMsg = bankConnectionAdapterContext.chanPaySignMessage(placeOrderString);
        List<BasicNameValuePair> requestData = new ArrayList<>();
        requestData.add(new BasicNameValuePair("reqData", placeOrderString));
        requestData.add(new BasicNameValuePair("reqSignData", signMsg));
        CtmJSONObject result = new CtmJSONObject();
        CtmJSONObject responseBody;
        //if (HttpsUtils.isWhiteUrl(bankConnectionAdapterContext.getChanPayUri())) {
            result = HttpsUtils.doHttpsPostNew(INWARD_REMITTANCE_RESULT_QUERY, requestData, bankConnectionAdapterContext.getChanPayUri());
        //}
        CtmJSONObject logJsonObject = new CtmJSONObject();
        logJsonObject.put("requestData", requestData);
        logJsonObject.put("responseData", result);
        ctmcmpBusinessLogService.saveBusinessLog(logJsonObject, "INWARD_REMITTANCE_RESULT_QUERY", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00237", "汇入汇款确认交易结果查询") /* "汇入汇款确认交易结果查询" */, IServicecodeConstant.INWARD_REMITTANCE, IMsgConstant.INWARD_REMITTANCE, IMsgConstant.INWARD_REMITTANCE_RESULT_QUERY);
        if (result.getInteger("code") == 1) {
            CtmJSONObject responseHead = result.getJSONObject("data").getJSONObject("response_head");
            String serviceStatus = responseHead.getString("service_status");
            if (("00").equals(serviceStatus)) {
                if (result.getJSONObject("data") == null || result.getJSONObject("data").getJSONObject("response_body") == null) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100296"),responseHead.get("service_resp_desc").toString());
                }
                responseBody = result.getJSONObject("data").getJSONObject("response_body");
                String entryStatus = (String) responseBody.get("entry_status");
                if (StringUtils.isEmpty(entryStatus)) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100297"),responseHead.get("service_resp_desc").toString());
                }
                InwardRemittance inwardRemittance;
                // 00：成功；01：失败；02：处理中
                switch (entryStatus) {
                    case "00":
                        // 成功，更新单据状态，此处考虑是否直接查询交易明细接口
                        inwardRemittance = getInwardRemittance(inwardRemittanceCode);
                        inwardRemittance.setInwardstatus(InwardStatus.SUCCESS.getIndex());
                        MetaDaoHelper.update(InwardRemittance.ENTITY_NAME, inwardRemittance);
                        break;
                    case "02":
                        // 处理中，不做任何处理
                        inwardRemittance = getInwardRemittance(inwardRemittanceCode);
                        inwardRemittance.setInwardstatus(InwardStatus.PROCESSING.getIndex());
                        MetaDaoHelper.update(InwardRemittance.ENTITY_NAME, inwardRemittance);
                        break;
                    case "01":
                        // 失败，将失败状态入库，下次不再查询该笔数据
                        inwardRemittance = getInwardRemittance(inwardRemittanceCode);
                        inwardRemittance.setInwardstatus(InwardStatus.FAIL.getIndex());
                        MetaDaoHelper.update(InwardRemittance.ENTITY_NAME, inwardRemittance);
                        break;
                    default:
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100298"),responseHead.get("service_resp_desc").toString());
                }
            } else {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100299"),responseHead.get("service_resp_desc").toString());
            }
        } else {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100300"),MessageUtils.getMessage("P_YS_CTM_CM-BE_1736383130981892101") /* "汇入汇款确认交易结果查询，SSFE3004接口调用异常" */);
        }
        return responseBody;
    }

    @Override
    public CtmJSONArray inwardRemittanceListQuery(CtmJSONObject param) throws Exception {
        // 校验企业银行账户，是否开通银企联
        QuerySchema querySettingSchema = QuerySchema.create().addSelect("id,customNo");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.addCondition(QueryConditionGroup.and(QueryCondition.name("enterpriseBankAccount").eq(param.get("bankaccount"))));
        conditionGroup.addCondition(QueryConditionGroup.and(QueryCondition.name("openFlag").eq(1)));
        conditionGroup.addCondition(QueryConditionGroup.and(QueryCondition.name("customNo").is_not_null()));
        querySettingSchema.addCondition(conditionGroup);
        List<Map<String, Object>> settingList = MetaDaoHelper.query(BankAccountSetting.ENTITY_NAME,querySettingSchema);
        if (settingList == null || settingList.size() == 0) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100301"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00232", "银行账户未开通银企联服务") /* "银行账户未开通银企联服务" */);
        }
        CtmJSONArray records = new CtmJSONArray();
        InwardRemittanceListQueryRequestVO requestVO = new InwardRemittanceListQueryRequestVO();
        Map<String, Object> enterprisebankaccount = QueryBaseDocUtils.queryEnterpriseBankAccountById(param.get("bankaccount"));
        requestVO.setRcv_acct_no((String) enterprisebankaccount.get("account"));
        List<Map<String, Object>> currencyList = QueryBaseDocUtils.queryCurrencyById(param.get("currency"));
        requestVO.setCurr_code((String) currencyList.get(0).get("code"));
        List dateList = (List) param.get("transferdate");
        requestVO.setBeg_date(((String) dateList.get(0)).replace("-", ""));
        requestVO.setEnd_date(((String) dateList.get(1)).replace("-", ""));
        // 分页查询，从零开始
        requestVO.setBeg_num("0");
        // 客户号拼接报文头
        if (param.get("customno") != null && !StringUtils.isEmpty(param.get("customno").toString())) {
            requestVO.setCustomNo(param.get("customno").toString());
        }
        if (Objects.isNull(requestVO.getCustomNo())) {
            requestVO.setCustomNo(settingList.get(0).get("customNo").toString());
        }
        records = doInwardRemittanceListQuery(requestVO, records, 0);
        if (records.size() > 0) {
            insertInwardRemittance(records, param.getString("accentity"), param.getString("bankaccount"));
        }
        return records;
    }

    @Override
    public void insertInwardRemittance(CtmJSONArray records, String accentity, String bankaccount) throws Exception {
        // 1.先遍历集合获取所有汇入汇款编号；2.查询数据库数据in条件；3.获取返回值existCodeList；4.在ids中update，不在则insert
        List<String> codeList = new ArrayList<>();
        Map<String, Map> existCodeList = new HashMap<>();
        for (Object recordObj : records) {
            CtmJSONObject record = (CtmJSONObject) recordObj;
            codeList.add(record.getString("bank_ref_no"));
        }
        QuerySchema querySchema = QuerySchema.create().addSelect("id, inwardremittancecode");
        QueryConditionGroup condition = new QueryConditionGroup();
        condition.addCondition(QueryConditionGroup.or(QueryCondition.name("inwardremittancecode").in(codeList)));
        querySchema.addCondition(condition);
        List<Map<String, Object>> existList = MetaDaoHelper.query(InwardRemittance.ENTITY_NAME, querySchema);
        for (Map<String, Object> existMap : existList) {
            existCodeList.put(existMap.get("inwardremittancecode").toString(), existMap);
        }
        // 遍历records数据 && 入库操作
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        List<InwardRemittance> updateList = new ArrayList<>();
        List<InwardRemittance> insertList = new ArrayList<>();
        for (Object recordObj : records) {
            CtmJSONObject record = (CtmJSONObject) recordObj;
            if (existCodeList.containsKey(record.getString("bank_ref_no"))) {
                Map currentMap = existCodeList.get(record.getString("bank_ref_no"));
                InwardRemittance inwardRemittance = MetaDaoHelper.findById(InwardRemittance.ENTITY_NAME, currentMap.get("id"));
                inwardRemittance.setAccentity(accentity);
                inwardRemittance.setBankaccount(bankaccount);
                inwardRemittance.setRemitteraddr(record.getString("orr_adr"));
                inwardRemittance.setTransferdate(sdf.parse(record.getString("tran_date")));
                if (record.getString("curr_code") != null && !"".equals(record.getString("curr_code"))) {
                    CurrencyTenantDTO currencyTenantDTO = currencyQueryService.findByCode(record.getString("curr_code"));
                    inwardRemittance.setCurrency(currencyTenantDTO.getId());
                }
                inwardRemittance.setRemittingbankaddr(record.getString("orb_brch_adr"));
                inwardRemittance.setRemittingbank(record.getString("orb_brch_name"));
                inwardRemittance.setAmount(new BigDecimal(record.getString("tran_amt")));
                inwardRemittance.setInwardremittancecode(record.getString("bank_ref_no"));
                // 待确认列表数据默认赋值
                inwardRemittance.setInwardstatus(InwardStatus.TO_BE_CONFIRMED.getIndex());
                inwardRemittance.setEntityStatus(EntityStatus.Update);
                updateList.add(inwardRemittance);
            } else {
                InwardRemittance inwardRemittance = new InwardRemittance();
                inwardRemittance.setId(ymsOidGenerator.nextId());
                inwardRemittance.setAccentity(accentity);
                inwardRemittance.setBankaccount(bankaccount);
                inwardRemittance.setRemitteraddr(record.getString("orr_adr"));
                inwardRemittance.setTransferdate(sdf.parse(record.getString("tran_date")));
                if (record.getString("curr_code") != null && !"".equals(record.getString("curr_code"))) {
                    CurrencyTenantDTO currencyTenantDTO = currencyQueryService.findByCode(record.getString("curr_code"));
                    inwardRemittance.setCurrency(currencyTenantDTO.getId());
                }
                inwardRemittance.setRemittingbankaddr(record.getString("orb_brch_adr"));
                inwardRemittance.setRemittingbank(record.getString("orb_brch_name"));
                inwardRemittance.setAmount(new BigDecimal(record.getString("tran_amt")));
                inwardRemittance.setInwardremittancecode(record.getString("bank_ref_no"));
                // 待确认列表数据默认赋值
                inwardRemittance.setInwardstatus(InwardStatus.TO_BE_CONFIRMED.getIndex());
                inwardRemittance.setEntityStatus(EntityStatus.Insert);
                insertList.add(inwardRemittance);
            }
        }
        // 批量操作数据，提高性能
        if (updateList.size() > 0) {
            MetaDaoHelper.update(InwardRemittance.ENTITY_NAME, updateList);
        }
        if (insertList.size() > 0) {
            CmpMetaDaoHelper.insert(InwardRemittance.ENTITY_NAME, insertList);
        }
    }


    /**
     * 递归调用查询下一页数据
     * @param requestVO
     * @param records
     * @return
     * @throws Exception
     */
    public CtmJSONArray doInwardRemittanceListQuery(InwardRemittanceListQueryRequestVO requestVO, CtmJSONArray records, int times) throws Exception {
        // 控制一下递归深度 100次
        if (times > 100) {
            return records;
        } else {
            times++;
        }
        requestVO.setTran_seq_no(ymsOidGenerator.nextStrId());
        requestVO.setBeg_num((new BigDecimal(requestVO.getBeg_num()).add(BigDecimal.TEN).toString()));
        CtmJSONObject placeOrderMsg = BankEnterpriseAssociation.buildReqDataSSFE3005(requestVO);
        String placeOrderString = CtmJSONObject.toJSONString(placeOrderMsg);
        String signMsg = bankConnectionAdapterContext.chanPaySignMessage(placeOrderString);
        List<BasicNameValuePair> requestData = new ArrayList<>();
        requestData.add(new BasicNameValuePair("reqData", placeOrderString));
        requestData.add(new BasicNameValuePair("reqSignData", signMsg));
        CtmJSONObject result = new CtmJSONObject();
        CtmJSONObject responseBody;
        //if (HttpsUtils.isWhiteUrl(bankConnectionAdapterContext.getChanPayUri())) {
            result = HttpsUtils.doHttpsPostNew(INWARD_REMITTANCE_LIST_QUERY, requestData, bankConnectionAdapterContext.getChanPayUri());
            //添加业务日志
            CtmJSONObject logJsonObject = new CtmJSONObject();
            logJsonObject.put("requestData", requestData);
            logJsonObject.put("responseData", result);
            ctmcmpBusinessLogService.saveBusinessLog(logJsonObject, "INWARD_REMITTANCE_LIST_QUERY", IMsgConstant.INWARD_REMITTANCE, IServicecodeConstant.INWARD_REMITTANCE, IMsgConstant.INWARD_REMITTANCE, IMsgConstant.INWARD_REMITTANCE_LIST_QUERY);
        //}
        if (result.getInteger("code") == 1) {
            CtmJSONObject responseHead = result.getJSONObject("data").getJSONObject("response_head");
            String serviceStatus = responseHead.getString("service_status");
            if (("00").equals(serviceStatus)) {
                responseBody = result.getJSONObject("data").getJSONObject("response_body");
                if (responseBody != null && responseBody.get("record") != null) {
                    records.addAll((CtmJSONArray) responseBody.get("record"));
                    if ("1".equals(responseBody.get("next_page"))) {
                        // 用于分页查询，下次查询时，如果有值则 赋值到下次查询的条件中
                        requestVO.setQuery_extend((String) responseBody.get("query_extend"));
                        records.addAll(doInwardRemittanceListQuery(requestVO, records, 0));
                    } else {
                        // next_page为0，没有下一页了，直接返回records
                        return records;
                    }
                }
            } else {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100302"),responseHead.get("service_resp_desc").toString());
            }
        } else {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100303"),MessageUtils.getMessage("P_YS_CTM_CM-BE_1736383130981892104") /* "汇入汇款待确认业务列表查询，SSFE3005接口调用异常" */);
        }
        return records;
    }

    @Override
    public CtmJSONArray inwardRemittanceDetailQuery(InwardRemittanceDetailQueryRequestVO requestVO, String accEntity, String bankAccount) {
        // 查询所有成功的汇入汇款数据
        CtmJSONArray records = new CtmJSONArray();
        try {
            // 分页查询，从零开始
            requestVO.setBeg_num("0");
            records = doInwardRemittanceDetailQuery(requestVO, records, 0);
            // 遍历records数据，入库操作
            for (Object recordObj : records) {
                CtmJSONObject record = (CtmJSONObject) recordObj;
                QuerySchema queryInwardRemittanceSchema = QuerySchema.create().addSelect("id");
                QueryConditionGroup conditionInwardRemittanceGroup = new QueryConditionGroup(ConditionOperator.and);
                // 根据汇入汇款编号查询
                conditionInwardRemittanceGroup.addCondition(QueryConditionGroup.and(QueryCondition.name("inwardremittancecode").eq(record.get("bank_ref_no"))));
                queryInwardRemittanceSchema.addCondition(conditionInwardRemittanceGroup);
                List<Map<String, Object>> inwardRemittanceList = MetaDaoHelper.query(InwardRemittance.ENTITY_NAME,queryInwardRemittanceSchema);
                InwardRemittance inwardRemittance;
                if (inwardRemittanceList == null || inwardRemittanceList.size() == 0) {
                    // 无该条数据，直接插入
                    inwardRemittance = new InwardRemittance();
                    inwardRemittance.setId(ymsOidGenerator.nextId());
                    inwardRemittance.setEntityStatus(EntityStatus.Insert);
                } else {
                    // 有数据，进行更新
                    inwardRemittance = MetaDaoHelper.findById(InwardRemittance.ENTITY_NAME, inwardRemittanceList.get(0).get("id"));
                    inwardRemittance.setEntityStatus(EntityStatus.Update);
                }
                inwardRemittance.setAccentity(accEntity);
                inwardRemittance.setBankaccount(bankAccount);
                inwardRemittance.setInwardremittancecode((String) record.get("bank_ref_no"));
                inwardRemittance.setRemitteraddr((String) record.get("rcv_acct_adr"));
                if (record.get("curr_code") != null && !"".equals(record.get("curr_code"))) {
                    String currencyCode = record.get("curr_code").toString();
                    CurrencyTenantDTO currencyTenantDTO = currencyQueryService.findByCode(currencyCode);
                    if (currencyTenantDTO != null) {
                        inwardRemittance.setCurrency(currencyTenantDTO.getId());
                    }
                }
                inwardRemittance.setAmount(record.get("tran_amt") == null ? null : new BigDecimal((String) record.get("tran_amt")));
                SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
                inwardRemittance.setTransferdate(record.get("tran_date") == null ? null : sdf.parse((String) record.get("tran_date")));
                inwardRemittance.setSwiftcode((String) record.get("orb_swift"));
                inwardRemittance.setRemittingbank((String) record.get("orb_brch_name"));
                inwardRemittance.setRemittingbankaddr((String) record.get("orb_brch_adr"));
                inwardRemittance.setRemitteraccount((String) record.get("orr_acct_no"));
                inwardRemittance.setRemitter((String) record.get("orr_acct_name"));
                inwardRemittance.setRemitteraddr((String) record.get("orr_adr"));
                inwardRemittance.setRealaccount((String) record.get("act_acct_no"));
                inwardRemittance.setRealamount(record.get("act_amt") == null ? null : new BigDecimal((String) record.get("act_amt")));
                inwardRemittance.setRealdate(record.get("entry_date") == null ? null : sdf.parse(record.get("entry_date").toString()));
                inwardRemittance.setForeignfeecurrency(record.get("fn_charge_curr") == null ? null : record.get("fn_charge_curr").toString());
                inwardRemittance.setForeignfeeamount(record.get("fn_charge_bearer") == null ? null : new BigDecimal(record.get("fn_charge_bearer").toString()));
                inwardRemittance.setForeigntelegramfeecurrency(record.get("fn_cable_curr") == null ? null : record.get("fn_cable_curr").toString());
                inwardRemittance.setForeigntelegramfee(record.get("fn_cable_amt") == null ? null : new BigDecimal(record.get("fn_cable_amt").toString()));
                inwardRemittance.setLocalfeecurrency(record.get("in_charge_curr") == null ? null : record.get("in_charge_curr").toString());
                inwardRemittance.setLocalfeeamount(record.get("in_charge_bearer") == null ? null : new BigDecimal(record.get("in_charge_bearer").toString()));
                inwardRemittance.setRemark((String) record.get("remark"));
                inwardRemittance.setPostscript((String) record.get("postscript"));
                inwardRemittance.setInwardstatus(InwardStatus.SUCCESS.getIndex());
                // TODO record.get("charges_option");费用收取方式，暂不保存该字段
                if (EntityStatus.Insert == inwardRemittance.getEntityStatus()) {
                    // 无该条数据，直接插入
                    CmpMetaDaoHelper.insert(InwardRemittance.ENTITY_NAME, inwardRemittance);
                } else {
                    // 有数据，进行更新
                    MetaDaoHelper.update(InwardRemittance.ENTITY_NAME, inwardRemittance);
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return records;
    }


    private CtmJSONArray doInwardRemittanceDetailQuery(InwardRemittanceDetailQueryRequestVO requestVO, CtmJSONArray records, int times) throws Exception {
        // 控制一下递归深度 100次
        if (times > 100) {
            return records;
        } else {
            times++;
        }
        requestVO.setTran_seq_no(ymsOidGenerator.nextStrId());
        // 每页10条，下一页起始条数加10
        requestVO.setBeg_num((new BigDecimal(requestVO.getBeg_num()).add(BigDecimal.TEN).toString()));
        CtmJSONObject placeOrderMsg = BankEnterpriseAssociation.buildReqDataSSFE3006(requestVO);
        String placeOrderString = CtmJSONObject.toJSONString(placeOrderMsg);
        String signMsg = bankConnectionAdapterContext.chanPaySignMessage(placeOrderString);
        List<BasicNameValuePair> requestData = new ArrayList<>();
        requestData.add(new BasicNameValuePair("reqData", placeOrderString));
        requestData.add(new BasicNameValuePair("reqSignData", signMsg));
        CtmJSONObject result = new CtmJSONObject();
        CtmJSONObject responseBody;
        //if (HttpsUtils.isWhiteUrl(bankConnectionAdapterContext.getChanPayUri())) {
            result = HttpsUtils.doHttpsPostNew(INWARD_REMITTANCE_DETAIL_QUERY, requestData, bankConnectionAdapterContext.getChanPayUri());
            //添加业务日志
            CtmJSONObject logJsonObject = new CtmJSONObject();
            logJsonObject.put("requestData", requestData);
            logJsonObject.put("responseData", result);
            ctmcmpBusinessLogService.saveBusinessLog(logJsonObject, "INWARD_REMITTANCE_DETAIL_QUERY", IMsgConstant.INWARD_REMITTANCE, IServicecodeConstant.INWARD_REMITTANCE, IMsgConstant.INWARD_REMITTANCE, IMsgConstant.INWARD_REMITTANCE_DETAIL_QUERY);
        //}
        if (result.getInteger("code") == 1) {
            CtmJSONObject responseHead = result.getJSONObject("data").getJSONObject("response_head");
            String serviceStatus = responseHead.getString("service_status");
            if (("00").equals(serviceStatus)) {
                responseBody = result.getJSONObject("data").getJSONObject("response_body");
                if (responseBody != null && responseBody.get("record") != null) {
                    records.addAll((CtmJSONArray) responseBody.get("record"));
                    if ("1".equals(responseBody.get("next_page"))) {
                        records.addAll(doInwardRemittanceDetailQuery(requestVO, records, times));
                    } else {
                        // next_page为0，没有下一页了，直接返回records
                        return records;
                    }
                }
            } else {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100304"),responseHead.get("service_resp_desc").toString());
            }
        } else {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100305"),MessageUtils.getMessage("P_YS_CTM_CM-BE_1736383130981892098") /* "汇入汇款业务明细查询，SSFE3006接口调用异常" */);
        }
        return records;
    }

    @Override
    public CtmJSONObject inwardRemittance_b(CtmJSONObject param) throws Exception {
        String mainId = param.getString("mainId");
        InwardRemittance inwardRemittance = MetaDaoHelper.findById(InwardRemittance.ENTITY_NAME, mainId);
        List<InwardRemittance_b> list = inwardRemittance.get("InwardRemittance_b");
        if (list == null || list.size() == 0) {
            // 之前未提交过，子表无数据，直接返回空
            return new CtmJSONObject();
        }
        InwardRemittance_b inwardRemittance_b = list.get(0);
        Long Transactioncode1 = inwardRemittance_b.getTransactioncode1();
        CtmJSONObject requestBody = CtmJSONObject.parseObject(CtmJSONObject.toJSONString(inwardRemittance_b));
        if (Transactioncode1 != null) {
            BizObject exchangeSettlementTradeCode1 = MetaDaoHelper.findById(ExchangeSettlementTradeCode.ENTITY_NAME, Transactioncode1);
            param.put("transactioncode1_code", exchangeSettlementTradeCode1.get("trade_code"));
            requestBody.put("transactioncode1_code", exchangeSettlementTradeCode1.get("trade_code"));
        }
        Long Transactioncode2 = inwardRemittance_b.getTransactioncode2();
        BizObject exchangeSettlementTradeCode2;
        if (Transactioncode2 != null) {
            exchangeSettlementTradeCode2 = MetaDaoHelper.findById(ExchangeSettlementTradeCode.ENTITY_NAME, Transactioncode2);
            requestBody.put("transactioncode2_code", exchangeSettlementTradeCode2.get("trade_code"));
        }
        String payernationid  = inwardRemittance_b.getPayernation_code();
        if (payernationid != null) {
            requestBody.put("payernationid", payernationid);
            BdCountryVO bdCountryVO = countryQueryService.findById(payernationid);
            requestBody.put("payernation_code", bdCountryVO.getName());
        }
        return requestBody;
    }

}
