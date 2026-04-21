package com.yonyoucloud.fi.cmp.bankaccountsetting.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.yonyou.iuap.yms.id.generator.YmsOidGenerator;
import com.yonyou.iuap.yms.lock.YmsLock;
import com.yonyou.ucf.basedoc.model.CurrencyTenantDTO;
import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVO;
import com.yonyou.ucf.basedoc.model.rpcparams.EnterpriseParams;
import com.yonyou.ucf.mdd.ext.bill.rule.util.GetRoundModeUtils;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyou.yonbip.ctm.accelerator.threadpool.CtmThreadPoolExecutor;
import com.yonyou.yonbip.ctm.bankconnection.BankConnectionAdapterContext;
import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.logger.business.CTMCMPBusinessLogService;
import com.yonyou.yonbip.ctm.util.JSONBuilderUtils;
import com.yonyoucloud.fi.basecom.constant.SystemConst;
import com.yonyoucloud.fi.basecom.service.UkeyPayService;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.cmp.balanceadjustresult.BalanceAdjustResult;
import com.yonyoucloud.fi.cmp.bankaccountsetting.BankAccountSetting;
import com.yonyoucloud.fi.cmp.bankaccountsetting.BankAccountSettingService;
import com.yonyoucloud.fi.cmp.bankaccountsetting.bo.BankAccountSettingBO;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.bankreconciliationsetting.BankReconciliationSetting;
import com.yonyoucloud.fi.cmp.bankreconciliationsetting.BankReconciliationSetting_b;
import com.yonyoucloud.fi.cmp.cmpentity.EnableStatus;
import com.yonyoucloud.fi.cmp.cmpentity.ReconciliationDataSource;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.constant.EmpowerConstand;
import com.yonyoucloud.fi.cmp.constant.IMsgConstant;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import com.yonyoucloud.fi.cmp.enums.AcctopenTypeEnum;
import com.yonyoucloud.fi.cmp.https.service.HttpsService;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.api.bankrecilication.CtmCmpBankReconciliationSettingRpcService;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.Direction;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.BankReconciliationSettingVO;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.bankAccountSetting.BankAccountSettingVO;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.common.CommonRequestDataVo;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.receipt.PlanParam;
import com.yonyoucloud.fi.cmp.util.*;
import com.yonyoucloud.fi.cmp.util.basedoc.EnterpriseBankQueryService;
import com.yonyoucloud.fi.cmp.util.business.CmpCommonUtil;
import com.yonyoucloud.fi.tmsp.openapi.ITmspUnitFundAttributesRpcService;
import com.yonyoucloud.fi.tmsp.vo.request.TmspUnitFundAttributesVO;
import com.yonyoucloud.fi.tmsp.vo.response.TmspUnitFundAttributesResp;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.sql.DataSource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by sz on 2019/4/20 0020.
 */
@Service
@Slf4j
public class BankAccountSettingServiceImpl implements BankAccountSettingService {

    @Autowired
    private HttpsService httpsService;
    private static final String BILLNUM  ="bankAccountSetting";
    private static final String DEFAULT_CUSTOMNO  ="nocert";
    @Autowired
    BaseRefRpcService baseRefRpcService;
    @Autowired
    YmsOidGenerator ymsOidGenerator;
    @Autowired
    private CTMCMPBusinessLogService ctmcmpBusinessLogService;
    @Autowired
    BankConnectionAdapterContext bankConnectionAdapterContext;

    @Autowired
    UkeyPayService ukeyPayService;
    @Autowired
    EnterpriseBankQueryService enterpriseBankQueryService;
    @Autowired
    private CtmThreadPoolExecutor executorServicePool;

    @Resource
    private CtmCmpBankReconciliationSettingRpcService reconciliationSettingRpcService;

    @Resource
    private ITmspUnitFundAttributesRpcService iTmspUnitFundAttributesRpcService;

    @Override
    public List<Map<String,Object>> getUseOrgAndMny(String openingOutstandingId, BankAccountSettingVO bankAccountSettingVO) throws Exception{
        List<Map<String,Object>> result = new ArrayList<>();
        String reconciliationdatasource = bankAccountSettingVO.getReconciliationdatasource();
        if(StringUtils.isEmpty(reconciliationdatasource)){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100076"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A05080031", "对账数据源必填，请检查数据") /* "对账数据源必填，请检查数据" */);
        }
        //1,对账数据源值为2 表示是银行日记账
        if("2".equals(reconciliationdatasource)){
            String accentityName = bankAccountSettingVO.getAccentityName();
            if(StringUtils.isEmpty(accentityName)){
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100077"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A0508002F", "对账数据源为日记账时,所属组织名称必填，请检查数据") /* "对账数据源为日记账时,所属组织名称必填，请检查数据" */);
            }
            Map<String, Object> coinitloribalanceByAccentity = getCoinitloribalanceByAccentity(bankAccountSettingVO);
            coinitloribalanceByAccentity.put("useOrgId",bankAccountSettingVO.getAccentity());
            coinitloribalanceByAccentity.put("useOrgName",bankAccountSettingVO.getAccentityName());
            result.add(coinitloribalanceByAccentity);
            return result;
        }
        //2,对账数据源值为2 表示 凭证，先获取授权使用组织数据，再查询凭证
        boolean empty = StringUtils.isEmpty(openingOutstandingId);
        if(!empty){
            //2.1 来源入口为 1-表示：期初未达数据,查询期初未达子表数据，不用查询凭证数据了，他的数据就是根据凭证数据查询存储下来的
            QuerySchema schema = QuerySchema.create().addSelect("id","useOrg as useOrgId","useOrg.name as useOrgName",
                    "direction","coinitloribalance","bankdirection","bankinitoribalance");
            QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
            conditionGroup.appendCondition(QueryCondition.name("mainid").eq(openingOutstandingId));
            schema.addCondition(conditionGroup);
            return MetaDaoHelper.query(BankReconciliationSetting_b.ENTITY_NAME, schema);
        }
        //2.2 来源入口为 2-表示 余额调节表数据，通过查询设置方案，获取有哪些授权组织
        PlanParam params = new PlanParam(bankAccountSettingVO.getBankaccount(),bankAccountSettingVO.getCurrency(),bankAccountSettingVO.getBankreconciliationscheme().toString());
        List<BankReconciliationSettingVO> useOrg = reconciliationSettingRpcService.findUseOrg(params);
        if(CollectionUtils.isNotEmpty(useOrg)){
            for (int i = 0; i < useOrg.size(); i++) {
                BankReconciliationSettingVO settingVO = useOrg.get(i);
                String useOrgId = settingVO.getUseOrg();
                String useOrgName = settingVO.getUseOrgName();
                bankAccountSettingVO.setUseOrg(useOrgId);
                Map<String, Object> voucherBalance = getVoucherBalance(bankAccountSettingVO);
                voucherBalance.put("useOrgId",useOrgId);
                voucherBalance.put("useOrgName",useOrgName);
                result.add(voucherBalance);
            }
        }
        return result;
    }
    /**
     * * 查询日记账
     * @param bankAccountSettingVO
     * @return
     * @throws Exception
     */
    @Override
    public Map<String, Object> getCoinitloribalanceByAccentity(BankAccountSettingVO bankAccountSettingVO) throws Exception {
        String enableDateStr = bankAccountSettingVO.getEnableDateStr();
        Date enableDate;
        if(StringUtils.isEmpty(enableDateStr)){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100078"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A05080032", "没有启用日期不能为空,请检查数据!") /* "没有启用日期不能为空,请检查数据!" */);
        }else{
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(DateUtils.DATE_TIME_PATTERN);
            enableDate = simpleDateFormat.parse(enableDateStr);
        }
        commonCheck(bankAccountSettingVO);
        //根据所属组织进行查询
        return CmpCommonUtil.getCoinitloribalance(bankAccountSettingVO.getAccentity(),bankAccountSettingVO.getBankaccount(),bankAccountSettingVO.getCurrency(),enableDate);
    }

    private void commonCheck(BankAccountSettingVO bankAccountSettingVO) {
        String bankaccount = bankAccountSettingVO.getBankaccount();
        Long bankreconciliationscheme = bankAccountSettingVO.getBankreconciliationscheme();
        String currency = bankAccountSettingVO.getCurrency();
        if(StringUtils.isEmpty(bankaccount)){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100079"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A05080030", "银行账户id不能为空,请检查数据!") /* "银行账户id不能为空,请检查数据!" */);
        }
        if(StringUtils.isEmpty(currency)){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100080"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A0508002D", "币种id不能为空,请检查数据!") /* "币种id不能为空,请检查数据!" */);
        }
        if(bankreconciliationscheme == null){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100081"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A0508002E", "方案id不能为空,请检查数据!") /* "方案id不能为空,请检查数据!" */);
        }
    }

    /**
     * * 查询总账服务
     * @param bankAccountSettingVO
     * @return
     * @throws Exception
     */
    @Override
    public Map<String,Object> getVoucherBalance(BankAccountSettingVO bankAccountSettingVO) throws Exception{
        commonCheck(bankAccountSettingVO);
        //根据授权组织id进行查询
        String accentity = bankAccountSettingVO.getUseOrg();
        String bankaccount = bankAccountSettingVO.getBankaccount();
        Long bankreconciliationscheme = bankAccountSettingVO.getBankreconciliationscheme();
        String currency = bankAccountSettingVO.getCurrency();
        CtmJSONObject ret = CmpCommonUtil.getVoucherBalance(null,true,accentity,bankaccount,bankreconciliationscheme,currency,true);

        //币种获取
        CurrencyTenantDTO currencyDTO = baseRefRpcService.queryCurrencyById(currency);
        RoundingMode moneyRound = GetRoundModeUtils.getCurrencyPriceRoundMode(currency, 1);
        BigDecimal coinitloribalance  = ret.getBigDecimal("subjectBalance").setScale(currencyDTO.getMoneydigit(),moneyRound);

        Map<String,Object> result = new HashMap<>();
        if(coinitloribalance.compareTo(BigDecimal.ZERO)>=0){
            result.put("direction", Direction.Debit);
        }else{
            result.put("direction", Direction.Credit);
        }
        result.put("coinitloribalance",coinitloribalance.abs());
        return result;
    }
    @Override
    public String accountQyTy(JsonNode param) throws Exception {
        BankAccountSetting accountSetting = MetaDaoHelper.findById(BankAccountSetting.ENTITY_NAME, param.get("id").asLong());
        accountSetting.setOpenFlag(param.get("openFlag").asBoolean());
        accountSetting.setCustomNo(param.get("customNo") == null?null:param.get("customNo").asText());
        if (accountSetting.getOpenFlag()) {
            accountSetting.setEmpower(EmpowerConstand.EMPOWER_QUERYANDPAY);
        } else {
            accountSetting.setEmpower(null);
            // 停用银企联，清空启用日期
            accountSetting.setEnableDate(null);
        }
        EntityTool.setUpdateStatus(accountSetting);
        MetaDaoHelper.update(BankAccountSetting.ENTITY_NAME, accountSetting);
        return ResultMessage.data(param);
    }

    //停用启用电票服务
    @Override
    //接收前端数据
    public String accountQyTyT(JsonNode param) throws Exception {
        //get(0)
        BankAccountSetting accountSetting = MetaDaoHelper.findById(BankAccountSetting.ENTITY_NAME, param.get("id").asLong());
        accountSetting.setOpenTicketService(param.get("openTicketService").asBoolean());
        //accountSetting.setCustomNo(param.getString("customNo"));
        EntityTool.setUpdateStatus(accountSetting);
        //更改数据库的值，先改数据库，后改页面;
        MetaDaoHelper.update(BankAccountSetting.ENTITY_NAME, accountSetting);
        //将数据返回给前端页面，保持继续执行其他内容
        return ResultMessage.data(param);
    }

    @Override
    public Boolean getOpenFlagByBankAccountId(String bankAccountId) throws Exception {
        QuerySchema schema = QuerySchema.create().addSelect("openFlag,empower");
        QueryConditionGroup conditionGroup = QueryConditionGroup.and(
                QueryCondition.name("enterpriseBankAccount").eq(bankAccountId));
        schema.addCondition(conditionGroup);
        Map<String, Object> setting = MetaDaoHelper.queryOne(BankAccountSetting.ENTITY_NAME, schema);
        if (ValueUtils.isNotEmpty(setting) && EmpowerConstand.EMPOWER_QUERYANDPAY.equals(setting.get("empower"))) {
            return (boolean) setting.get("openFlag");
        }
        return false;
    }

    @Override
    public Boolean getOpenFlagByBankAccountIdOfQuery(String bankAccountId) throws Exception {
        QuerySchema schema = QuerySchema.create().addSelect("openFlag,empower");
        QueryConditionGroup conditionGroup = QueryConditionGroup.and(
                QueryCondition.name("enterpriseBankAccount").eq(bankAccountId));
        schema.addCondition(conditionGroup);
        Map<String, Object> setting = MetaDaoHelper.queryOne(BankAccountSetting.ENTITY_NAME, schema);
        if (ValueUtils.isNotEmpty(setting) && (EmpowerConstand.EMPOWER_ONLYQUERY.equals(setting.get("empower")) || EmpowerConstand.EMPOWER_QUERYANDPAY.equals(setting.get("empower")))) {
            return (boolean) setting.get("openFlag");
        }
        return false;
    }

    @Override
    public String updateCustomNo(CtmJSONObject param) throws Exception{
        //data: selectRows.map(row => ({
        //        id: row.id,
        //        accentity: row.accentity,
        //        enterpriseBankAccount: row.enterpriseBankAccount
        //  }))
        int totalNum = 0;
        int failNum = 0;
        List<String> failMsg = new ArrayList<>();
        try {
            CtmJSONArray datalist = param.getJSONArray("data");
            totalNum = datalist.size();
            List<TmspUnitFundAttributesVO> tmspUnitFundAttributesVOs = new ArrayList<>();
            List<String> inputBankAccountList = new ArrayList<>();
            Map<String, String> idToAccount = new HashMap<>();
            for (int i = 0; i < datalist.size(); i++) {
                CtmJSONObject item = datalist.getJSONObject(i);
                String accentity = item.getString("accentity");
                String enterpriseBankAccount = item.getString("enterpriseBankAccount");
                String enterpriseBankAccount_account = item.getString("enterpriseBankAccount_account");
                idToAccount.put(enterpriseBankAccount, enterpriseBankAccount_account);
                inputBankAccountList.add(enterpriseBankAccount_account);
                TmspUnitFundAttributesVO tmspUnitFundAttributesVO = new TmspUnitFundAttributesVO();
                tmspUnitFundAttributesVO.setAccentity(accentity);
                tmspUnitFundAttributesVO.setBankAccount(enterpriseBankAccount);
                tmspUnitFundAttributesVOs.add(tmspUnitFundAttributesVO);
            }
            log.error("更新客户号入参：" + tmspUnitFundAttributesVOs.toString());
            TmspUnitFundAttributesResp tmspUnitFundAttributesResp = null;
            try {
                tmspUnitFundAttributesResp = iTmspUnitFundAttributesRpcService.queryCustomerNumberBatch(tmspUnitFundAttributesVOs);
            } catch (Exception e) {
                throw new CtmException(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054007B4", "账户%s更新客户号失败:") /* "账户%s更新客户号失败:" */, inputBankAccountList.toString()) + e.getMessage(), e);
            }

            // 没有toSting方法，打印时自己序列化为JSON字符串
            log.error("更新客户号出参：{}", CtmJSONObject.toJSONString(tmspUnitFundAttributesResp));


            List<TmspUnitFundAttributesResp.Data> respDataList = tmspUnitFundAttributesResp.getData();
            Map<String, String> bankAccountToCustomNo = new HashMap<>();
            for (int i = 0; i < respDataList.size(); i++) {
                TmspUnitFundAttributesResp.Data respData = respDataList.get(i);
                //String accentity = respData.getAccentity();
                String customNo = respData.getCustomerNumber();
                String bankAccount = respData.getBankAccount();
                //String bankCustomerCode = respData.getBankCustomerCode();
                bankAccountToCustomNo.put(bankAccount, customNo);
            }
            List<BankAccountSetting> bankAccountSettings = new ArrayList<>();
            for (int i = 0; i < datalist.size(); i++) {
                CtmJSONObject item = datalist.getJSONObject(i);
                String id = item.getString("id");
                String bankAccount = item.getString("enterpriseBankAccount");
                String customNo  = bankAccountToCustomNo.get(bankAccount);
                if (StringUtils.isEmpty(customNo)) {
                    failMsg.add(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054007B0", "账户[%s]更新客户号[%s]失败，获取的客户号为空，请联系银企联获取正确的客户号，并在【银企联连接配置】或【业务单元资金属性】维护") /* "账户[%s]更新客户号[%s]失败，获取的客户号为空，请联系银企联获取正确的客户号，并在【银企联连接配置】或【业务单元资金属性】维护" */, idToAccount.get(bankAccount), customNo));
                    failNum++;
                } else if (customNo.length() > 6) {
                    failMsg.add(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054007B1", "账户[%s]更新客户号[%s]失败，获取的客户号超过了6位，请联系银企联获取正确的客户号，并在【银企联连接配置】或【业务单元资金属性】维护") /* "账户[%s]更新客户号[%s]失败，获取的客户号超过了6位，请联系银企联获取正确的客户号，并在【银企联连接配置】或【业务单元资金属性】维护" */, idToAccount.get(bankAccount), customNo));
                    failNum++;
                } else {
                    BankAccountSetting bankAccountSetting = new BankAccountSetting();
                    bankAccountSetting.setId(Long.parseLong(id.toString()));
                    bankAccountSetting.setCustomNo(customNo);
                    bankAccountSettings.add(bankAccountSetting);
                    //String enterpriseBankAccount = item.getString("enterpriseBankAccount");
                }
            }
            CmpMetaDaoHelper.update(BankAccountSetting.ENTITY_NAME, bankAccountSettings);
        } catch (Exception e) {
            failMsg.add(e.getMessage());
            failNum = totalNum;
        }
        String failMsgFinal = "";
        if (CollectionUtils.isNotEmpty(failMsg)) {
            failMsgFinal = String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054007B2", "失败原因：[%s]") /* "失败原因：[%s]" */,  failMsg);
        }
        return ResultMessage.data(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054007B3", "更新客户号，失败%d条，成功%d条。%s") /* "更新客户号，失败%d条，成功%d条。%s" */, failNum, totalNum - failNum, failMsgFinal));

        //log.error("----------银企联账号客户号下载-------------CHAN_PAY_URI参数:"+bankConnectionAdapterContext.getChanPayUri());
        //log.error("----------银企联账号-----------------------------customNo:"+param.get("customNo"));
        //CtmJSONObject postMsg = buildQueryMsg(param);
        //log.error("----------银企联账号客户号下载-------------postMsg参数:"+postMsg.toString());
        //log.error("----------银企联账号客户号下载-------------CHAN_PAY_URI参数:"+bankConnectionAdapterContext.getChanPayUri());
        //CtmJSONObject postResult = httpsService.doHttpsPost(ITransCodeConstant.CUST_NO_QUERY, postMsg, bankConnectionAdapterContext.getChanPayUri(), null);
        //log.error("----------银企联账号客户号下载-------------postResult结果:"+postResult.toString());
        //CtmJSONObject logData = new CtmJSONObject();
        //logData.put("request", postMsg);
        //logData.put("response", postResult);
        //ctmcmpBusinessLogService.saveBusinessLog(logData, (String) param.get("customNo"), "", IServicecodeConstant.BANKACCOUNTSETTING, IMsgConstant.BANK_ACCOUNT_SETTING, IMsgConstant.UPDATE_CUSTOM_NO);
        //StringBuilder payMessage = new StringBuilder();
        //String res;
        //if ("0000".equals(postResult.getString("code"))) {
        //    if (postResult.get("data")!=null){
        //        //若当前无锁
        //            //加分布式锁3分钟 避免客户重复点击操作 造成cpu压力过大
        //            YmsLock ymsLock = JedisLockUtils.lockBillWithOutTraceByTime(BILLNUM + "updateCustomNoAsync", 120);
        //            if (ymsLock != null) {
        //                String result = null;
        //                try {
        //                    result = updateCustomNoAsync( postResult, param, payMessage);
        //                } catch (Exception e) {
        //                    log.error("updateCustomNoAsync:" +  e.getMessage(), e);
        //                    res = ResultMessage.error("updateCustomNoAsync:" +  e.getMessage());
        //                } finally {
        //                    // 20241129：释放锁。从更新所有数据修改为更新指定行数据后，数据量已经可控，取消多线程与定时锁
        //                    JedisLockUtils.unlockBillWithOutTrace(ymsLock);
        //                }
        //                res = result;
        //            } else {
        //                res = ResultMessage.error(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00103", "正在加载银企联账户数据,请稍后重试!") /* "正在加载银企联账户数据,请稍后重试!" */);
        //            }
        //    } else {
        //        res = ResultMessage.error(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805BD","更新客户号失败！银企联未返回data数据信息，请联系支持顾问人员。") /* "更新客户号失败！银企联未返回data数据信息，请联系支持顾问人员。" */);
        //    }
        //} else {
        //    res = ResultMessage.error(YQLUtils.getYQLErrorMsq((CtmJSONObject) postMsg.get("request_head")));
        //}
        ////前端不返回，执行结果写到业务日志中
        ////string转map，否则写不进去
        //Gson gson = new Gson();
        //Map<String, Object> map = gson.fromJson(res, new TypeToken<Map<String, Object>>(){}.getType());
        //ctmcmpBusinessLogService.saveBusinessLog(map, (String) param.get("customNo"), "updateResult", IServicecodeConstant.BANKACCOUNTSETTING, IMsgConstant.BANK_ACCOUNT_SETTING, IMsgConstant.UPDATE_CUSTOM_NO);
        //return res;
    }

    private String updateCustomNoAsync(CtmJSONObject postResult,CtmJSONObject param,StringBuilder payMessage) throws Exception {
        CtmJSONObject responseHead = postResult.getJSONObject("data").getJSONObject("response_head");
        String serviceStatus = responseHead.getString("service_status");
        if (("00").equals(serviceStatus)) {
            String successMssage = updateCustomNoSuccess( postResult, param);
            log.error("----------银企联账号客户号下载-------------successMssage:"+successMssage);
            return successMssage;
        } else {
            String failMessage = updateCustomNoFail(payMessage,responseHead);
            log.error("----------银企联账号客户号下载-------------failMessage:"+failMessage);
            return failMessage;
        }
    }

    private String updateCustomNoSuccess(CtmJSONObject postResult,CtmJSONObject param) throws Exception {
        CtmJSONObject responseBody = postResult.getJSONObject("data").getJSONObject("response_body");
        CtmJSONArray records =null;
        CtmJSONObject recordOne =null;
        int backNum = responseBody.getInteger("back_num");
        if (backNum>1){
            records = responseBody.getJSONArray("record");
        } else {
            recordOne = responseBody.getJSONObject("record");
        }
        String cust_no = null;
        if (responseBody.get("cust_no") != null) {
            cust_no = responseBody.get("cust_no").toString();
        }
        if((records!=null||recordOne!=null) && cust_no != null){
            //返回数据不为空时 信息处理
            List<String> ids = JSONBuilderUtils.stringToBeanList(param.get("ids").toString(), String.class);
            // 获取启用银企联的帐号,指定id集合 &＆　开通银企联服务
            QuerySchema schema = QuerySchema.create().addSelect("*");
            QueryConditionGroup conditionGroup = new QueryConditionGroup();
            conditionGroup.addCondition(QueryConditionGroup.and(QueryCondition.name("openFlag").eq("1")));
            conditionGroup.addCondition(QueryConditionGroup.and(QueryCondition.name("id").in(ids)));
            //仅仅查询无客户号的数据进行后续更新，存在客户号的可能后续换客户号
//            conditionGroup.addCondition(QueryConditionGroup.and(QueryCondition.name("customNo").is_null()));
            schema.addCondition(conditionGroup);
            List<BankAccountSetting> list = MetaDaoHelper.queryObject(BankAccountSetting.ENTITY_NAME, schema, null);
            if (list == null || list.size() < 1) {
                return ResultMessage.error(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805B4","未查询到已启用的银企联账号，请先启用或者账户同步企业银行账户") /* "未查询到已启用的银企联账号，请先启用或者账户同步企业银行账户" */);
            }
            //此map用于后续 批量更新
            Map<String,BankAccountSetting> toUpdateMap = new HashMap<>();
            List<String> accountIds = new ArrayList<>();
            for (BankAccountSetting map:list){
                accountIds.add(map.get("enterpriseBankAccount").toString());
                toUpdateMap.put(map.get("enterpriseBankAccount").toString(),map);
            }
            // 处理方案 按照500条分组查询，后续分组处理，先一次处理
            List<EnterpriseBankAcctVO> enterpriseBankAcctVOs = queryBankAcctVOs(accountIds);
            //存储最终需要更新的list
            List<String> messages = new ArrayList<>();
            //比对获取需要更新的数据
            List <BankAccountSetting> updateList = getUpdateCustomVos( backNum, enterpriseBankAcctVOs, records, recordOne,toUpdateMap, cust_no, messages);
            int successSize = updateList.size();
            CtmJSONObject retParam = new CtmJSONObject();
            retParam.put("updateTime", new Date());
            //处理返回数据,totalCount-前端勾选的总数，updateIds，实际更新数据ids
            retParam.put("rowCount", ids.size());
            retParam.put("cust_no", cust_no);
            retParam.put("backNum", backNum);
            retParam.put("queryCount", list.size());
            if (updateList.size()>0){
                //批量更新数据
                updateDataForCustomNo(updateList);
                StringBuilder message = new StringBuilder();
                message.append(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805B5","共返回[%s]条签约银行账户，[%s]条匹配更新客户号成功，[%s]条匹配更新客户号失败！") /* "共返回[%s]条签约银行账户，[%s]条匹配更新客户号成功，[%s]条匹配更新客户号失败！" */, list.size(), successSize, list.size() - successSize));

                retParam.put("updateIds", updateList.stream().map(BizObject::getId).collect(Collectors.toList()));
                retParam.put("message", message);
                retParam.put("messages", messages);
                retParam.put("sucessCount", successSize);
                retParam.put("failCount", list.size() - successSize);
                return ResultMessage.data(retParam);
            }
            StringBuilder message = new StringBuilder();
            message.append(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805B5","共返回[%s]条签约银行账户，[%s]条匹配更新客户号成功，[%s]条匹配更新客户号失败！") /* "共返回[%s]条签约银行账户，[%s]条匹配更新客户号成功，[%s]条匹配更新客户号失败！" */, list.size(), 0, list.size()));
            retParam.put("message", message);
            retParam.put("sucessCount", 0);
            retParam.put("failCount", list.size());
            return ResultMessage.data(retParam);
        } else {
            return ResultMessage.error(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805BC","客户号[%s]未查询到签约账号，请联系银企联支持顾问人员进行签约。") /* "客户号[%s]未查询到签约账号，请联系银企联支持顾问人员进行签约。" */, param.get("customNo")));
        }
    }

    private void updateDataForCustomNo( List <BankAccountSetting> updateList) throws Exception {
        EntityTool.setUpdateStatus(updateList);
        MetaDaoHelper.update(BankAccountSetting.ENTITY_NAME,updateList);
    }

    private  List<EnterpriseBankAcctVO>  queryBankAcctVOs( List<String> accountIds) throws Exception {
        EnterpriseParams enterpriseParams = new EnterpriseParams();
        List<EnterpriseBankAcctVO> enterpriseBankAcctVOs = new ArrayList<>();
        // 拼接银行账户查询过滤，有权限，已启用开通银企联的账号
        List<String> queryAccList = new ArrayList<>(accountIds);

        if(CollectionUtils.isNotEmpty(queryAccList)) {
            enterpriseParams.setIdList(queryAccList);
        }
        int pageSize = 500;
        int totalSize = queryAccList.size();
        int totalPage = Math.floorDiv(totalSize, pageSize) + 1;
        for (int i = 1; i<=totalPage; i++) {
            enterpriseParams.setPageIndex(i);
            enterpriseParams.setPageSize(pageSize);
            int beginIndex = (i-1) * pageSize;
            int lastIndex = i * pageSize;
            if (lastIndex >= totalSize) {
                lastIndex = totalSize;
            }
            List<String> subStrings = queryAccList.subList(beginIndex, lastIndex);
            enterpriseParams.setIdList(subStrings);

            enterpriseBankAcctVOs.addAll(baseRefRpcService.queryEnterpriseBankAcctByCondition(enterpriseParams));
            if (lastIndex >= totalSize) {
                break;
            }
        }
        return enterpriseBankAcctVOs;
    }

    private List <BankAccountSetting> getUpdateCustomVos(int backNum,List<EnterpriseBankAcctVO> enterpriseBankAcctVOs,CtmJSONArray records,CtmJSONObject recordOne,Map<String,BankAccountSetting> toUpdateMap,String cust_no,List<String> messages){
        List <BankAccountSetting> updateList = new ArrayList<>();
        boolean successflag;
        // 记录acct_no匹配上但acct_name未匹配上的数据，写入业务日志中
        CtmJSONObject logData = new CtmJSONObject();
        if (backNum>1){
            // ※※※ enterpriseBankAcctVOs为已选中的数据，records为银企联返回的数据，双重for循环嵌套效率较低，修改为一层for循环衔接Map查找
            Map<String, String> recordsMap = new HashMap();

            for (int i = 0; i < records.size(); i++) {
                // 银企联返回的acct_no不会重复，可以直接放如HashMap中，便于查找，提升查询效率
                recordsMap.put(records.getJSONObject(i).getString("acct_no"), records.getJSONObject(i).getString("acct_name"));
            }
            for (EnterpriseBankAcctVO enterpriseBankAcctVO : enterpriseBankAcctVOs) {
                successflag = false;
                // 先判断acct_no是否匹配
                if (recordsMap.containsKey(enterpriseBankAcctVO.getAccount())) {
                    // 判断acct_name是否匹配
                    if (enterpriseBankAcctVO.getAcctName().equals(recordsMap.get(enterpriseBankAcctVO.getAccount()))) {
                        BankAccountSetting updateVo = toUpdateMap.get(enterpriseBankAcctVO.getId());
                        updateVo.setCustomNo(cust_no);
                        updateList.add(updateVo);
                        successflag = true;
                    } else {
                        String logStr = String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1E7B129405200011", "银行账号：【%s】匹配失败，本地开户名：【%s】，银企联开户名：【%s】") /* "银行账号：【%s】匹配失败，本地开户名：【%s】，银企联开户名：【%s】" */, enterpriseBankAcctVO.getAccount(), enterpriseBankAcctVO.getAcctName(), recordsMap.get(enterpriseBankAcctVO.getAccount()));
                        logData.put(enterpriseBankAcctVO.getAccount(), logStr);
                    }
                    if (!successflag) {
                        messages.add(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805BB","签约银行账户[%s]，开户名[%s]在企业银行账户档案中未查询到对应的银行账户，请客户检查签约信息。") /* "签约银行账户[%s]，开户名[%s]在企业银行账户档案中未查询到对应的银行账户，请客户检查签约信息。" */, enterpriseBankAcctVO.getAccount(), enterpriseBankAcctVO.getAcctName()));
                    }
                }
            }

        }else{
            for (EnterpriseBankAcctVO enterpriseBankAcctVO : enterpriseBankAcctVOs) {
                successflag = false;
                if (recordOne.getString("acct_no").equals(enterpriseBankAcctVO.getAccount())) {
                    if (recordOne.getString("acct_name").equals(enterpriseBankAcctVO.getAcctName())) {
                        BankAccountSetting updateVo = toUpdateMap.get(enterpriseBankAcctVO.getId());
                        updateVo.setCustomNo(cust_no);
                        updateList.add(updateVo);
                        successflag = true;
                    } else {
                        String logStr = String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1E7B129405200011", "银行账号：【%s】匹配失败，本地开户名：【%s】，银企联开户名：【%s】") /* "银行账号：【%s】匹配失败，本地开户名：【%s】，银企联开户名：【%s】" */, enterpriseBankAcctVO.getAccount(), enterpriseBankAcctVO.getAcctName(), recordOne.get(enterpriseBankAcctVO.getAcctName()));
                        logData.put(enterpriseBankAcctVO.getAccount(), logStr);
                    }
                }
                if (!successflag) {
                    messages.add(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805BB","签约银行账户[%s]，开户名[%s]在企业银行账户档案中未查询到对应的银行账户，请客户检查签约信息。") /* "签约银行账户[%s]，开户名[%s]在企业银行账户档案中未查询到对应的银行账户，请客户检查签约信息。" */, recordOne.getString("acct_no"), recordOne.getString("acct_name")));
                }
            }
        }
        // ※※※ 生产环境发现大量客户问题，原因是acct_no与银行账号匹配而acct_name与开户名不匹配导致（既银行填写的开户名，与BIP企业银行账户节点录入的开户名不一致导致），增加单独的业务日志，提醒客户及时修改错误数据
        if (!StringUtils.isEmpty(logData.toString())) {
            ctmcmpBusinessLogService.saveBusinessLog(logData, "", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1E7B129405200012", "更新客户号匹配失败") /* "更新客户号匹配失败" */, IServicecodeConstant.BANKACCOUNTSETTING, IMsgConstant.BANK_ACCOUNT_SETTING, IMsgConstant.UPDATE_CUSTOM_NO);
        }
        return updateList;
    }

    private String updateCustomNoFail(StringBuilder payMessage,CtmJSONObject responseHead){
        if (responseHead.containsKey("service_resp_code")) {
            payMessage.append(
                    com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805B6","【") /* "【" */);
            payMessage.append(responseHead.get("service_resp_code"));
            payMessage.append(
                    com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805B8","】") /* "】" */);
        }
        if (responseHead.containsKey("service_resp_desc")) {
            payMessage.append(responseHead.get("service_resp_desc"));
        }
        return ResultMessage.error(payMessage.toString());
    }

    /**
     * 同步客户号
     * @return
     */
    @Override
    public int syncAccount() {
        int count = 0;
        YmsLock ymsLock = null;
        try {
            if((ymsLock=JedisLockUtils.lockWithOutTrace(BILLNUM))==null){
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100082"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800D1","该组织下正在初始化数据，请稍后再试！") /* "该组织下正在初始化数据，请稍后再试！" */);
            }
            long tenantId = AppContext.getTenantId();
            QuerySchema querySchema = QuerySchema.create().addSelect("*");
            QueryConditionGroup condition = new QueryConditionGroup();
            querySchema.addCondition(condition);
            //查询银企联账号list
            List<BankAccountSetting> list = MetaDaoHelper.queryObject(BankAccountSetting.ENTITY_NAME, querySchema, null);
            //库中已经存在的账户集合
            Set<String> enterpriseBankAccountIds = new HashSet<>();
            if(!list.isEmpty()){
                for (BankAccountSetting setting : list) {
                    enterpriseBankAccountIds.add((String) setting.get("enterpriseBankAccount"));
                }
            }
            //企业银行账户
            List<EnterpriseBankAcctVO> enterpriseBankAccounts =queryBaseBankAccount();
            //新增逻辑处理
            List<BankAccountSetting> insertBankAccountSettings = buildInerstList(enterpriseBankAccounts,enterpriseBankAccountIds );
            //新增
            if (insertBankAccountSettings.size() > 0) {
                CmpMetaDaoHelper.insert(BankAccountSetting.ENTITY_NAME, insertBankAccountSettings);
                count = count + insertBankAccountSettings.size();
            }
            //修改 对已存在的账户进行更新 这里耗时80%以上 走异步
            executorServicePool.getThreadPoolExecutor().submit(() -> {
                try {
                    updateAccStatus(list, enterpriseBankAccounts);
                } catch (Exception e) {
                    log.error("初始化数据异常!"+e.getMessage(), e);
                }
            });
//            count = count;
        }catch (Exception e){
            log.error("初始化数据异常!"+e.getMessage(), e);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100083"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00103", "正在加载银企联账户数据,请稍后重试!") /* "正在加载银企联账户数据,请稍后重试!" */);
        }finally {
            JedisLockUtils.unlockWithOutTrace(ymsLock);
        }
        return count;
    }

    private List<BankAccountSetting> queryBankAccountSettingLsit(QuerySchema querySchema) throws Exception {
        //银企联账号list
        List<BankAccountSetting> list = MetaDaoHelper.queryObject(BankAccountSetting.ENTITY_NAME, querySchema, null);
        return list;
    }

    private List<EnterpriseBankAcctVO> queryBaseBankAccount() throws Exception {
        List<EnterpriseBankAcctVO> result = new ArrayList<>();
        EnterpriseParams bankAccountParams = new EnterpriseParams();
        bankAccountParams.setAcctopentype(AcctopenTypeEnum.BankAccount.getValue());
        bankAccountParams.setEnables(Arrays.asList(0, 1, 2));
        List<EnterpriseBankAcctVO> bankAccountList = enterpriseBankQueryService.queryAll(bankAccountParams);
        result.addAll(bankAccountList);
        //账户开户类型为“银行开户”，“财务公司”，产品咨询weixi老师
        EnterpriseParams financialCompanyParams = new EnterpriseParams();
        financialCompanyParams.setAcctopentype(AcctopenTypeEnum.FinancialCompany.getValue());
        financialCompanyParams.setEnables(Arrays.asList(0, 1, 2));
        List<EnterpriseBankAcctVO> financialCompanyList = enterpriseBankQueryService.queryAll(financialCompanyParams);
        result.addAll(financialCompanyList);
        return result;
    }


    private List<BankAccountSetting> buildInerstList(List<EnterpriseBankAcctVO> enterpriseBankAccounts,Set<String> enterpriseBankAccountIds ){
        long tenantId = AppContext.getTenantId();
        List<BankAccountSetting> insertBankAccountSettings = new ArrayList<>();
        //新增逻辑处理
        for(EnterpriseBankAcctVO enterpriseBankAccount : enterpriseBankAccounts) {
            if(enterpriseBankAccountIds.contains(enterpriseBankAccount.getId()) || enterpriseBankAccount.getEnable() != 1){
                continue;
            }
            BankAccountSetting bankaccountSetting = new BankAccountSetting();
            bankaccountSetting.setAccentity(enterpriseBankAccount.getOrgid());
            bankaccountSetting.setEnterpriseBankAccount(enterpriseBankAccount.getId());
            //针对国机修改 公有云还原
            bankaccountSetting.setAccStatus((String.valueOf(enterpriseBankAccount.getEnable() == 1 ? 0 : 1)));
//            bankaccountSetting.setAccStatus("0");
            bankaccountSetting.setOpenFlag(false);
            bankaccountSetting.setTenant(tenantId);
            bankaccountSetting.setEntityStatus(EntityStatus.Insert);
            bankaccountSetting.setId(ymsOidGenerator.nextId());
            insertBankAccountSettings.add(bankaccountSetting);
        }
        return insertBankAccountSettings;
    }

    private void updateAccStatus(List<BankAccountSetting> list, List<EnterpriseBankAcctVO> enterpriseBankAccounts) throws Exception {
        /**
         *企业银行账户 enable:1 启用 ；enable:2 停用
         * 银企联账户：accStatus：0启用； 1停用
         */
        if(CollectionUtils.isEmpty(list) || CollectionUtils.isEmpty(enterpriseBankAccounts)){
            return ;
        }

        List<BankAccountSettingBO> bankAccountSettingBOList = new ArrayList<>();
        for(BankAccountSetting item : list){
            if(null == item.getAccStatus()){
                item.setAccStatus("99");
            }

            BankAccountSettingBO bankAccountSettingBO = new BankAccountSettingBO();
            bankAccountSettingBO.setAccount(item.getEnterpriseBankAccount());
            bankAccountSettingBO.setStatus(item.getAccStatus());
            bankAccountSettingBOList.add(bankAccountSettingBO);
        }
        List<BankAccountSettingBO> enterpriseBankAccountVOList = new ArrayList<>();
        for (EnterpriseBankAcctVO enterpriseBankAccount : enterpriseBankAccounts) {
            String flag = "";
            if(null != enterpriseBankAccount.getEnable()){
                if(enterpriseBankAccount.getEnable() == 1){
                    flag = "0";
                }else{
                    flag = "1";
                }
            }
            BankAccountSettingBO bankAccountSettingBO = new BankAccountSettingBO();
            bankAccountSettingBO.setAccount(enterpriseBankAccount.getId()+"");
            bankAccountSettingBO.setStatus(flag);
            enterpriseBankAccountVOList.add(bankAccountSettingBO);
        }
        //取差集 以企业银行账号为准
        Collection<BankAccountSettingBO> subCollection = CollectionUtils.subtract(enterpriseBankAccountVOList,bankAccountSettingBOList);
        if (subCollection.size() > 0) {
            //todo 先不改了，界面不报错，只是每次只能同步一批，后面的批次会报错
            updateBatch(subCollection);
        }
    }

    /**
     * 高效率执行批量更新
     * */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void updateBatch(Collection<BankAccountSettingBO> bankAccountSettingBOS){

        if (!CollectionUtils.isEmpty(bankAccountSettingBOS)) {
            SqlSessionTemplate sqlSessionTemplate = AppContext.getSqlSession();
            Connection connection = null;
            PreparedStatement preparedStatement = null;
            try {
                DataSource dataSource = sqlSessionTemplate.getConfiguration().getEnvironment().getDataSource();
                connection = DataSourceUtils.getConnection(dataSource);
                connection.setAutoCommit(false);
                String updateSql = "update cmp_bankaccountsetting set accStatus = ? where enterprisebankaccount=?";
                preparedStatement = connection.prepareStatement(updateSql);
                int size = bankAccountSettingBOS.size();
                Iterator<BankAccountSettingBO> iterator = bankAccountSettingBOS.iterator();
                int i = 0;
                while(iterator.hasNext()){
                    BankAccountSettingBO bankAccountSettingBO = iterator.next();
                    String accStatus = bankAccountSettingBO.getStatus();
                    String enterprisebankaccount = bankAccountSettingBO.getAccount();
                    preparedStatement.setString(1, accStatus);
                    preparedStatement.setString(2, enterprisebankaccount);
                    //添加批量sql
                    preparedStatement.addBatch();
                    //每400条执行一次，防止内存堆栈溢出
                    if (i > 0 && i % 400 == 0) {
                        preparedStatement.executeBatch();
                        preparedStatement.clearBatch();
                    }
                    i++;
                }
                //最后执行剩余不足400条的
                preparedStatement.executeBatch();
                //执行完手动提交事务
                connection.commit();
                //在把自动提交事务打开
                connection.setAutoCommit(true);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            } finally {
                try{
                    if (null != preparedStatement) {
                        preparedStatement.close();
                    }
                    if (null != connection) {
                        connection.close();
                    }
                }catch (SQLException e){
                    log.error(e.getMessage(),e);
                }
            }
        }
    }

    private CtmJSONObject buildQueryMsg(CtmJSONObject params) throws Exception {
        String ukeyType = ukeyPayService.getUkeyType();
        String customNo = null;
        if (SystemConst.CATYPE_CFCA.equals(ukeyType) || SystemConst.CATYPE_NJCA.equals(ukeyType)) {
            customNo = (String) params.get("customNo");
            if(DEFAULT_CUSTOMNO.equals(customNo)){
                customNo = AppContext.getEnvConfig("CHAN_PAY_CUSTOMNO");
            }
        } else {
            customNo = AppContext.getEnvConfig("CUSTOMNO_PRIVATE");
            if (StringUtils.isEmpty(customNo)) {
                customNo = AppContext.getEnvConfig("CustomNo_TianWei");
            }
            if (customNo == null) {
                //TODO 多语资源需要抽取
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100084"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("", "若客户环境为专属化且使用天威CA厂商时，请先联系运维在YMS控制台配置银企联商户号，参数为【CUSTOMNO_PRIVATE】。") /* "若客户环境为专属化且使用天威CA厂商时，请先联系运维在YMS控制台配置银企联商户号，参数为【CustomNo_TianWei】。" */);
            }
        }
        CtmJSONObject requestHead = buildRequloadestHead("01a21a001", customNo);
        CtmJSONObject requestBody = new CtmJSONObject();
        CtmJSONObject queryMsg = new CtmJSONObject();
        queryMsg.put("request_head", requestHead);
        queryMsg.put("request_body", requestBody);
        return queryMsg;
    }

    private CtmJSONObject buildRequloadestHead(String transCode, String customNo) throws Exception{
        CtmJSONObject requestHead = new CtmJSONObject();
        requestHead.put("version", "1.0.0");
        requestHead.put("request_seq_no", DigitalSignatureUtils.buildRequestNum(customNo));
        requestHead.put("cust_no", customNo);
        requestHead.put("cust_chnl", AppContext.getBean(BankConnectionAdapterContext.class).getChanPayCustomChanel());
        LocalDateTime dateTime = LocalDateTime.now();
        requestHead.put("request_date", DateTimeFormatter.ofPattern(DateUtils.YYYYMMDD).format(dateTime));
        requestHead.put("request_time", DateTimeFormatter.ofPattern(DateUtils.HHMMSS).format(dateTime));
        requestHead.put("oper_sign", null);
        requestHead.put("tran_code", transCode);
        return requestHead;
    }

    @Override
    public String getOpenFlag(String accountId) throws Exception {
        CtmJSONObject result = new CtmJSONObject();
        result.put("openFlag", false);
        QuerySchema schema = QuerySchema.create().addSelect("enterpriseBankAccount,openFlag,empower");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("enterpriseBankAccount").eq(accountId));
        schema.addCondition(conditionGroup);
        List<HashMap<String, Object>> bankAccountSettinges = MetaDaoHelper.query(BankAccountSetting.ENTITY_NAME, schema);
        if(!CollectionUtils.isEmpty(bankAccountSettinges)){
            HashMap<String, Object> bankAccountSetting = bankAccountSettinges.get(0);
            if((Boolean) bankAccountSetting.get("openFlag") && (EmpowerConstand.EMPOWER_QUERYANDPAY).equals(bankAccountSetting.get("empower"))){
                result.put("openFlag", bankAccountSetting.get("openFlag"));
            }
        }
        return ResultMessage.data(result);
    }

    /**
     * 银企联账号-直连启用日期设置
     * @param param
     * @return
     * @throws Exception
     */
    @Override
    public String accountEnableDateSet(JsonNode param) throws Exception {
        ObjectNode result = JSONBuilderUtils.createJson();

        ArrayNode billids = param.withArray("ids");
        Date enableDate = null;
        if (param.get("enableDate")!=null && !("null").equals(param.get("enableDate").asText())) {
            String a = param.get("enableDate").toString().substring(1, param.get("enableDate").toString().length()-1);
            enableDate = DateUtils.parseUTCDateToDate(a);
        }
        if (billids.isEmpty()) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100085"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00102", "操作的单据ID不能为空！") /* "操作的单据ID不能为空！" */);
        }
        List<Object> ids = JSONBuilderUtils.jsonListToBeanList(billids,Object.class);
        //查询全部需要设置启用日期的银企联账号
        QuerySchema querySchema = QuerySchema.create().addSelect("*");
        QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("id").in(ids));
        querySchema.addCondition(group);
        List<BankAccountSetting> bankAccountSettings = MetaDaoHelper.queryObject(BankAccountSetting.ENTITY_NAME,querySchema,null);
        if (bankAccountSettings.isEmpty()){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100086"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00101", "修改的数据不存在！") /* "修改的数据不存在！" */);
        }
        for (BankAccountSetting item:bankAccountSettings){
            item.setEnableDate(enableDate);
            item.setEntityStatus(EntityStatus.Update);
        }
        MetaDaoHelper.update(BankAccountSetting.ENTITY_NAME,bankAccountSettings);

        result.put("code", 200);
        result.put("msg","success");
        return ResultMessage.data(result);
    }

    /**
     * 查询银企联直连账号启用日期
     * @param param
     * @return
     * @throws Exception
     */
    @Override
    public String queryEnableDate(JsonNode param) throws Exception {
        ObjectNode result = JSONBuilderUtils.createJson();

        Long accountId = param.get("accountId").asLong();
        QuerySchema querySchema = QuerySchema.create().addSelect("*");
        QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("enterpriseBankAccount").eq(accountId));
        querySchema.addCondition(group);
        List<BankAccountSetting> bankAccountSettings = MetaDaoHelper.queryObject(BankAccountSetting.ENTITY_NAME,querySchema,null);
        if (!bankAccountSettings.isEmpty()){
            if (bankAccountSettings.get(0).getEnableDate() != null){
                result.put("enableDate",DateUtils.dateFormat(bankAccountSettings.get(0).getEnableDate(),"yyyy-MM-dd"));
            }
        }
        result.put("code", 200);
        result.put("msg","success");
        return ResultMessage.data(result);
    }

    /**
     * 获取客户号
     * @param bankAccountId
     * @return
     * @throws Exception
     */
    @Override
    public String getCustomNoByBankAccountId(String bankAccountId) throws Exception {
        QuerySchema schema = QuerySchema.create().addSelect("customNo");
        QueryConditionGroup conditionGroup = QueryConditionGroup.and(
                QueryCondition.name("enterpriseBankAccount").eq(bankAccountId), QueryCondition.name("openFlag").eq("1"),
                QueryCondition.name("customNo").is_not_null());
        schema.addCondition(conditionGroup);
        Map<String, Object> setting = MetaDaoHelper.queryOne(BankAccountSetting.ENTITY_NAME, schema);
        if (ValueUtils.isEmpty(setting)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100087"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805B9","无法获取企业银行账户相关联的银企联客户号，请在银企联账户设置功能节点维护") /* "无法获取企业银行账户相关联的银企联客户号，请在银企联账户设置功能节点维护" */);
        }
        return setting.get("customNo").toString();
    }

    /**
     * 获取客户号
     * @param bankAccountId
     * @return
     * @throws Exception
     */
    @Override
    public HashMap<String,String> batchGetCustomNoByBankAccountId(List<String> bankAccountId) throws Exception {
        HashMap<String,String> result = new HashMap<>();
        QuerySchema schema = QuerySchema.create().addSelect("enterpriseBankAccount,customNo");
        QueryConditionGroup conditionGroup = QueryConditionGroup.and(
                QueryCondition.name("enterpriseBankAccount").in(bankAccountId), QueryCondition.name("openFlag").eq("1"),
                QueryCondition.name("customNo").is_not_null());
        schema.addCondition(conditionGroup);
        List<Map<String, Object>> settingList = MetaDaoHelper.query(BankAccountSetting.ENTITY_NAME,schema);
        if (ValueUtils.isEmpty(settingList)) {
            return result;
        }
        for (Map<String, Object> item:settingList){
            result.put(item.get("enterpriseBankAccount").toString(),item.get("customNo").toString());
        }
        return result;
    }

    @Override
    public String getCustomNoAndCheckByBankAccountId(String bankAccountId, Object customNo) throws Exception {
        String customNoByBankAccId = getCustomNoByBankAccountId(bankAccountId);
//        String ukeyType = ukeyPayService.getUkeyType();
//        if(SystemConst.CATYPE_CFCA.equals(ukeyType) || SystemConst.CATYPE_NJCA.equals(ukeyType)) {
//            if(AppContext.getEnvConfig("ischeckCustno","1").equals("1")){
//                if (customNo == null || !customNoByBankAccId.equals(customNo.toString())) {
//                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100088"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_182AE45E04280006","U盾信息与银企联账号节点维护的客户号不一致，请检查后重试！") /* "U盾信息与银企联账号节点维护的客户号不一致，请检查后重试！" */);
//                }
//            }
//        }
        return customNoByBankAccId;
    }

    @Override
    public String getCustomNoAndCheckUKeyByBankAccountId(Boolean isNeedCheckUkey, String bankAccountId, Object customNo) throws Exception {
        String customNoByBankAccId = getCustomNoByBankAccountId(bankAccountId);
        if (isNeedCheckUkey == null) {
            isNeedCheckUkey = false;
        }
        if (!isNeedCheckUkey) {
            // 不需要校验则直接返回银行账号的客户号
            return customNoByBankAccId;
        }

        String ukeyType = ukeyPayService.getUkeyType();
        if(SystemConst.CATYPE_CFCA.equals(ukeyType) || SystemConst.CATYPE_NJCA.equals(ukeyType)) {
            if (DEFAULT_CUSTOMNO.equals(customNo)) {
                customNo = AppContext.getEnvConfig("CHAN_PAY_CUSTOMNO");
            }
            if (customNo == null || !customNoByBankAccId.equals(customNo.toString())) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100088"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_182AE45E04280006","U盾信息与银企联账号节点维护的客户号不一致，请检查后重试！") /* "U盾信息与银企联账号节点维护的客户号不一致，请检查后重试！" */);
            }
        }
        return customNoByBankAccId;
    }


    @Override
    public Map<String , Boolean> getOpenFlagReMap(CommonRequestDataVo commonQueryData) throws Exception {
        Map<String , Boolean> result = new HashMap<>();
        QuerySchema schema = QuerySchema.create().addSelect("enterpriseBankAccount,openFlag,empower");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("enterpriseBankAccount").in(commonQueryData.getIds()));
        schema.addCondition(conditionGroup);
        List<HashMap<String, Object>> bankAccountSettinges = MetaDaoHelper.query(BankAccountSetting.ENTITY_NAME, schema);
        if(!CollectionUtils.isEmpty(bankAccountSettinges)){
            //根据账户id查询账号信息
            EnterpriseParams params = new EnterpriseParams();
            params.setIdList(commonQueryData.getIds());
            List<EnterpriseBankAcctVO>  accounts = baseRefRpcService.queryEnterpriseBankAccountByCondition(params);
            for(EnterpriseBankAcctVO vo : accounts){
                for(HashMap<String, Object> map : bankAccountSettinges){
                    if(vo.getId().equals(map.get("enterpriseBankAccount"))){
                        if((Boolean) map.get("openFlag") && (EmpowerConstand.EMPOWER_QUERYANDPAY).equals(map.get("empower"))){
                            result.put(vo.getAccount(),true);
                        }else
                            result.put(vo.getAccount(),false);
                    }
                    continue;
                }
            }
        }
        return result;
    }


    /**
     * 根据银行账户id查询对应的银企联设置信息
     * @param enterpriseBankAccounts
     * @return
     * @throws Exception
     */
    public List<Map<String, Object>> queryBankAccountSettingByBankAccounts(List<String> enterpriseBankAccounts) throws Exception {
        List<Map<String, Object>> retData = new ArrayList<>();
        int batchcount = 200;
        int listSize = enterpriseBankAccounts.size();
        int totalTask = (listSize % batchcount == 0 ? listSize / batchcount : (listSize / batchcount)+1);
        for (int i = 0; i < totalTask; i++) {
            int fromIndex = i * batchcount;
            int toIndex = i * batchcount + batchcount;
            if (i + 1 == totalTask) {
                toIndex = listSize;
            }
            QuerySchema schema = QuerySchema.create().addSelect(" id,tenant,accentity,openFlag,enterpriseBankAccount,enterpriseBankAccount.acctName as acctName,enterpriseBankAccount.name as name," + "enterpriseBankAccount.account as account, enterpriseBankAccount.bankNumber.bank as bank," + "customNo,enterpriseBankAccount.id as bankId,enterpriseBankAccount.enable as enable");// 判断银行账户表是否为空故多差一个id
            QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
            conditionGroup.appendCondition(QueryCondition.name("openFlag").eq("1"));
            conditionGroup.appendCondition(QueryCondition.name("enterpriseBankAccount").in(enterpriseBankAccounts.subList(fromIndex,toIndex)));
            conditionGroup.appendCondition(QueryCondition.name("customNo").is_not_null());
            schema.addCondition(conditionGroup);
            List<Map<String, Object>> tmpData = MetaDaoHelper.query(BankAccountSetting.ENTITY_NAME, schema);
            retData.addAll(tmpData);
        }
        return retData;
    }

    @Override
    public List<Map<String,Object>> listBankAccountSettingByOpenFlag(Boolean openFlag) throws Exception{
        QuerySchema schema = QuerySchema.create();
        schema.addSelect("*");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("openFlag").eq(openFlag));
        conditionGroup.appendCondition(QueryCondition.name("accentity").is_not_null());
        schema.addCondition(conditionGroup);
        List<Map<String,Object>> bankAccountSettings = MetaDaoHelper.query(BankAccountSetting.ENTITY_NAME,schema,"");
        return bankAccountSettings;
    }

    @Override
    public List<String> queryBankAccountSettingByFlag() throws Exception{
        List<Map<String,Object>> bankAccountSettingList = listBankAccountSettingByOpenFlag(true);
        List<String> bankAccountList=new ArrayList<>();
        if(CollectionUtils.isNotEmpty(bankAccountSettingList)){
            for (Map<String,Object> bankAccountSetting : bankAccountSettingList) {
                if(bankAccountSetting.get("enterpriseBankAccount")!=null){
                    bankAccountList.add(bankAccountSetting.get("enterpriseBankAccount").toString());
                }
            }
        }
        return bankAccountList;
    }

    @Override
    public List<String> queryBankAccountSettingByDirect() throws Exception {
        QuerySchema schema = QuerySchema.create();
        schema.addSelect("enterpriseBankAccount");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("openFlag").eq(true));
        conditionGroup.appendCondition(QueryCondition.name("empower").eq(EmpowerConstand.EMPOWER_QUERYANDPAY));
        conditionGroup.appendCondition(QueryCondition.name("accentity").is_not_null());
        schema.addCondition(conditionGroup);
        List<Map<String, Object>> bankAccountSettingList = MetaDaoHelper.query(BankAccountSetting.ENTITY_NAME, schema, "");
        List<String> bankAccountList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(bankAccountSettingList)) {
            for (Map<String, Object> bankAccountSetting : bankAccountSettingList) {
                if (bankAccountSetting.get("enterpriseBankAccount") != null) {
                    bankAccountList.add(bankAccountSetting.get("enterpriseBankAccount").toString());
                }
            }
        }
        return bankAccountList;
    }



    @Override
    public CtmJSONObject checkReconciliationInfo(CtmJSONObject param) throws Exception {
        CtmJSONObject result = new CtmJSONObject();
        BankReconciliationSetting_b item = MetaDaoHelper.findById(BankReconciliationSetting_b.ENTITY_NAME, param.get("id"));
        //为空时默认返回false
        if (item == null){
            result.put("isContainChecked",false);
            return result;
        }
        BankReconciliationSetting bankReconciliationSetting = MetaDaoHelper.findById(BankReconciliationSetting.ENTITY_NAME, item.getMainid(),3);
        //判断删除时，若相同账户、币种已存在启用态的子表行时，则允许删除
        QuerySchema totalSchema = QuerySchema.create().addSelect("*");
        QueryConditionGroup totalGroup = QueryConditionGroup.and(QueryCondition.name("mainid").eq(item.getMainid()));
        totalSchema.addCondition(totalGroup);
        List<BankReconciliationSetting_b> totalItems = MetaDaoHelper.queryObject(BankReconciliationSetting_b.ENTITY_NAME,totalSchema,null);
        boolean exists = hasEnabledMatchingBankSetting(totalItems, item);
        if (exists){
            result.put("isContainChecked",false);
            return result;
        }

        //账户是否已经勾对判断依据：条件1：已勾对的数据上的ID与当前方案ID一样，条件2：已勾对的账户与当前行账户一致，时，
        // 条件1、2均满足时，代表账户已勾对，任意一个条件不满足时，账户未勾对
        QuerySchema schema = QuerySchema.create().addSelect("count(1) as  count");
        QueryConditionGroup bankGroup = QueryConditionGroup.and(QueryCondition.name("bankaccount").eq(item.getBankaccount()), QueryCondition.name("currency").eq(item.getCurrency()));
        if (bankReconciliationSetting.getReconciliationdatasource().getValue() == ReconciliationDataSource.Voucher.getValue()) {
            bankGroup.addCondition(QueryCondition.name("gl_bankreconciliationsettingid").eq(item.getMainid()));
            bankGroup.addCondition(QueryCondition.name("other_checkflag").eq(true));
        } else {
            bankGroup.addCondition(QueryCondition.name("bankreconciliationsettingid").eq(item.getMainid()));
            bankGroup.addCondition(QueryCondition.name("checkflag").eq(true));
        }
        schema.addCondition(bankGroup);

        Map<String, Object> map = MetaDaoHelper.queryOne(BankReconciliation.ENTITY_NAME, schema);
        Long count = (Long) map.get("count");
        //判断是否包含已勾兑数据
        result.put("isContainChecked",count>0);

        //需要校验是否生成了余额调节表
        QuerySchema querySchema = QuerySchema.create().addSelect("id");
        QueryConditionGroup group = QueryConditionGroup.and(
                QueryCondition.name("currency").eq(item.getCurrency()),//币种
                QueryCondition.name("bankaccount").eq(item.getBankaccount()),//银行账号
                QueryCondition.name("bankreconciliationscheme").eq(bankReconciliationSetting.getId())//对账方案id
        );
        querySchema.addCondition(group);
        List<BalanceAdjustResult> checkList =  MetaDaoHelper.queryObject(BalanceAdjustResult.ENTITY_NAME, querySchema, null);
        result.put("isGenerateBalance",checkList.size() >0);

        return result;
    }

    /**
     * 判断 BankReconciliationSetting_b 集合中是否存在与给定 item
     * 具有相同 bankaccount 和 currency，并且 enableStatus_b=1（已启用）的数据
     * @param totalItems 所有子表行信息
     * @param item 当前子表行数据
     * @return
     */
    private boolean hasEnabledMatchingBankSetting(List<BankReconciliationSetting_b> totalItems,
                                                  BankReconciliationSetting_b item) {
        if (totalItems == null){
            return false;
        }
        return totalItems.stream()
                .anyMatch(bankSetting ->
                // 排除自身（通过ID判断是否为同一数据）
                !Objects.equals(bankSetting.getId(), item.getId()) &&
                // 状态为已启用 (enableStatus_b == 1)
                bankSetting.getEnableStatus_b() == EnableStatus.Enabled.getValue() &&
                // 银行账户相同
                Objects.equals(bankSetting.getBankaccount(), item.getBankaccount()) &&
                // 币种相同
                Objects.equals(bankSetting.getCurrency(), item.getCurrency()));
    }

}
