package com.yonyoucloud.fi.cmp.controller.billclaim.bankreconciliation;

//import com.fasterxml.jackson.databind.ObjectMapper;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.iuap.yms.id.generator.YmsOidGenerator;
import com.yonyou.ucf.basedoc.model.CurrencyTenantDTO;
import com.yonyou.ucf.mdd.ext.base.BaseController;
import com.yonyou.ucf.mdd.ext.bill.rule.util.GetRoundModeUtils;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.json.ObjectMapperUtils;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.basecom.util.auth.CMPDiworkPermission;
import com.yonyoucloud.fi.basecom.utils.HttpTookit;
import com.yonyoucloud.fi.cmp.bankaccountsetting.BankAccountSettingService;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.bankreconciliation.service.stopuse.ItemStrategy;
import com.yonyoucloud.fi.cmp.bankreconciliation.service.stopuse.MainStrategy;
import com.yonyoucloud.fi.cmp.bankreconciliation.service.stopuse.StopUse;
import com.yonyoucloud.fi.cmp.bankreconciliationsetting.BankReconciliationSetting;
import com.yonyoucloud.fi.cmp.bankreconciliationsetting.BankReconciliationSettingSaveRule;
import com.yonyoucloud.fi.cmp.bankreconciliationsetting.BankReconciliationSetting_b;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.Direction;
import com.yonyoucloud.fi.cmp.cmpentity.EnableStatus;
import com.yonyoucloud.fi.cmp.cmpentity.ReconciliationDataSource;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import com.yonyoucloud.fi.cmp.initdata.OpeningOutstanding;
import com.yonyoucloud.fi.cmp.journal.Journal;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.api.bankrecilication.CtmCmpBankReconciliationSettingRpcService;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.BankReconciliationPlanVO;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.bankAccountSetting.BankAccountSettingVO;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.receipt.ReconciliationPlanParam;
import com.yonyoucloud.fi.cmp.openingoutstanding.OpeningOutstandingService;
import com.yonyoucloud.fi.cmp.util.EntityTool;
import com.yonyoucloud.fi.cmp.util.QueryReconciliation;
import com.yonyoucloud.fi.cmp.util.business.CmpCommonUtil;

import lombok.extern.slf4j.Slf4j;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.biz.base.Objectlizer;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.Json;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.ParseException;
import java.util.*;

@Controller
@RequestMapping("/bankreconciliationsetting")
//@Authentication(value = false, readCookie = true)
@Slf4j
public class BankReconciliationSettingController extends BaseController {
    @Autowired
    BankReconciliationSettingSaveRule bankReconciliationSettingSaveRule;

    @Autowired
    OpeningOutstandingService openingOutstandingService;
    @Autowired
    YmsOidGenerator ymsOidGenerator;
    @Autowired
    BaseRefRpcService baseRefRpcService;
    @Autowired
    private MainStrategy mainStrategy;
    @Autowired
    private ItemStrategy itemStrategy;
    @Autowired
    private BankAccountSettingService bankAccountSettingService;
    @Autowired
    CtmCmpBankReconciliationSettingRpcService ctmCmpBankReconciliationSettingRpcService;
    //private static ObjectMapper objectMapper = com.yonyou.yonbip.ctm.json.ObjectMapperUtils.objectMapper;

    @RequestMapping("/queryAssistaccountingType")
    @CMPDiworkPermission(IServicecodeConstant.BANKRECONCILIATIONSETTING)
    public void queryAssistaccountingType(@RequestBody CtmJSONObject params, HttpServletResponse response) throws Exception {
        log.error("params:", CtmJSONObject.toJSONString(params));
        List<Map<String, Object>> assistaccount = new ArrayList<Map<String, Object>>();
        assistaccount = getMinPeriod(params);
        CtmJSONArray enumArray = new CtmJSONArray();
        CtmJSONObject cEnumString = new CtmJSONObject();
        for (Map<String, Object> ass : assistaccount) {
            CtmJSONObject map = new CtmJSONObject();
            String valuename;
            if (ass.containsKey("account")) {
                valuename = ass.get("name") + "【" + ass.get("account") + "】";//@notranslate
            } else if (ass.containsKey("code")) {
                valuename = ass.get("name") + "【" + ass.get("code") + "】";//@notranslate
            } else {
                valuename = (String) ass.get("name");
            }
            map.put("nameType", "text");
            map.put("value", valuename);
            map.put("key", (String) ass.get("id"));
            enumArray.add(map);
            cEnumString.put((String) ass.get("id"), valuename);
        }
        CtmJSONObject data = new CtmJSONObject();
        data.put("enumArray", enumArray);
        data.put("cEnumString", cEnumString);
        renderJson(response, ResultMessage.data(data));
    }

    public List<Map<String, Object>> getMinPeriod(CtmJSONObject params) throws Exception {
        String serverUrl = AppContext.getEnvConfig("yzb.base.url");
        String BASE_URL_ACCOUNT_SETTLE = serverUrl + "/refbase_ctr/queryRefJSON";
        String thd_userId = AppContext.getCurrentUser().getYhtUserId();
        Map<String, String> header = new HashMap<>();
        header.put("Content-Type", "application/json");
        header.put("thd_userId", thd_userId);
        params.put("doctype", params.getString("refCode"));
        String str = HttpTookit.
                doPostWithJson(BASE_URL_ACCOUNT_SETTLE, CtmJSONObject.toJSONString(params), header, "UTF-8");
        CtmJSONObject result = CtmJSONObject.parseObject(str);
        Boolean successFlag = (Boolean) result.get("success");
        if (!successFlag) {
            throw new Exception(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041801C3", "查询辅助核算失败，请稍后重试") /* "查询辅助核算失败，请稍后重试" */);
        }
        List<Map<String, Object>> data = null;
        String code = String.valueOf(result.get("code"));
        if ("0".equals(code)) {
            data = new ArrayList<Map<String, Object>>();
            data = (List<Map<String, Object>>) result.get("data");
        } else {
            throw new Exception(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041801C9", "辅助核算获取失败") /* "辅助核算获取失败" */);
        }
        return data;
    }

    @RequestMapping("/queryReconcilia")
    @CMPDiworkPermission(IServicecodeConstant.BANKRECONCILIATIONSETTING)
    public void queryReconcilia(@RequestBody CtmJSONObject params, HttpServletResponse response) throws Exception {
        Long id = Long.valueOf(params.get("id").toString());
        List<BankReconciliationSetting_b> bank_b = (List) params.get("bank_b");
        int reconciliationdatasource = (int) params.get("source");
        CtmJSONObject data = QueryReconciliation.queryReconciliation(bank_b, id, reconciliationdatasource);
        renderJson(response, ResultMessage.data(data));
    }

    /**
     * 根据科目id查询是否在对账方案中引用
     *
     * @param subject
     */
    @ResponseBody
    @RequestMapping("/queryHaveDateBySubjectid")
    public CtmJSONObject queryHaveDateBySubjectid(@RequestParam(value = "subject") String subject) {
        CtmJSONObject json = new CtmJSONObject();
        try {
            Boolean flag = false;
            QuerySchema querySchema = QuerySchema.create().addSelect("subject");
            QueryConditionGroup queryCondition = QueryConditionGroup.and(QueryCondition.name("subject").eq(subject));
            querySchema.addCondition(queryCondition);
            List<Map<String, Object>> list = MetaDaoHelper.query(BankReconciliationSetting_b.ENTITY_NAME, querySchema);
            if (list.size() > 0) {
                //被引用  不能删除
                flag = true;
            }
            json.put("code", 200);
            json.put("message", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041801CC", "查询对账方案成功") /* "查询对账方案成功" */);
            json.put("flag", flag);
            json.put("success", true);
        } catch (Exception e) {
            log.error("未知错误：" + e.getMessage(), e);
            json.put("code", 999);
            json.put("message", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041801CE", "查询对账方案失败") /* "查询对账方案失败" */);
            json.put("success", false);
        }
        return json;
    }

    @RequestMapping("/queryBankAccountById")
    public void query(@RequestBody CtmJSONObject param, HttpServletRequest request, HttpServletResponse response) throws Exception {
        Long id = param.getLong("reconciliationdatasourceid");
        QuerySchema schema = QuerySchema.create().addSelect("bankaccount,bankaccount.name as name,currency,currency.name as currencyName");
        schema.appendQueryCondition(QueryCondition.name("mainid").eq(id));
        List<Map<String, Object>> bankAccountList = new ArrayList<Map<String, Object>>();
        bankAccountList = MetaDaoHelper.query(BankReconciliationSetting_b.ENTITY_NAME, schema);
        CtmJSONObject json = new CtmJSONObject();
        json.put("bankAccountList", bankAccountList);
        renderJson(response, ResultMessage.data(json));
    }

    //修改启用状态
    @RequestMapping("/updateStatus")
    @CMPDiworkPermission(IServicecodeConstant.BANKRECONCILIATIONSETTING)
    public void updateStatus(@RequestBody CtmJSONObject param, HttpServletResponse response) throws Exception {
        Json json = new Json(CtmJSONObject.toJSONString(param.getJSONObject("data")));
        List<BizObject> list = Objectlizer.decode(json, BankReconciliationSetting.ENTITY_NAME);
        BizObject bizObject = list.get(0);
        Short mode = param.getShort("mode");
        String msg;
        //3子表停用，4子表启用
        if (mode == 3 || mode == 4) {
            //子表停用策略
            StopUse stopUse = new StopUse(itemStrategy);
            msg = stopUse.stopUse(bizObject, mode);
        } else {
            //主表停用策略
            StopUse stopUse = new StopUse(mainStrategy);
            msg = stopUse.stopUse(bizObject, mode);
        }
        CtmJSONObject result = new CtmJSONObject();
        result.put("msg", msg);
        renderJson(response, ResultMessage.data(result));
    }

    /**
     * 校验对账方案明细关联勾对信息
     *
     * @param param
     * @param response
     * @throws Exception
     */
    @RequestMapping("/checkReconciliationInfo")
    @CMPDiworkPermission(IServicecodeConstant.BANKRECONCILIATIONSETTING)
    public void checkReconciliationInfo(@RequestBody CtmJSONObject param, HttpServletResponse response) throws Exception {
        renderJson(response, ResultMessage.data(bankAccountSettingService.checkReconciliationInfo(param)));
    }

    /**
     * 期初未达项操作
     *
     * @param id 对账方案id
     */
    private List<OpeningOutstanding> operationQc(Long id) throws Exception {
        BankReconciliationSetting bankReconciliationSetting = MetaDaoHelper.findById(BankReconciliationSetting.ENTITY_NAME, id);
        if (bankReconciliationSetting == null) {
            return new ArrayList<>();
        }
        List<OpeningOutstanding> openingOutstandings = new ArrayList<>();
        List<BankReconciliationSetting_b> bank_bs = bankReconciliationSetting.get("bankReconciliationSetting_b");
        //先清理旧明细
        List<OpeningOutstanding> oldOpeningOutstandings = getOldOpeningOutstanding(bankReconciliationSetting);
        if (oldOpeningOutstandings != null && oldOpeningOutstandings.size() != 0) {
            MetaDaoHelper.delete(OpeningOutstanding.ENTITY_NAME, oldOpeningOutstandings);
        }
        if (bank_bs != null) {
            for (BankReconciliationSetting_b bank_b : bank_bs) {
                OpeningOutstanding openingOutstanding = new OpeningOutstanding();
                openingOutstanding.setAccentity(bankReconciliationSetting.getAccentity());
                openingOutstanding.setBankreconciliationscheme(id);
                openingOutstanding.setReconciliationdatasource(bankReconciliationSetting.getReconciliationdatasource());
                openingOutstanding.setAccbook(bankReconciliationSetting.getAccbook());
                openingOutstanding.setEnableDate(bankReconciliationSetting.getEnableDate());
                openingOutstanding.setEnableStatus(EnableStatus.Enabled);
                openingOutstanding.setBankaccount(bank_b.getBankaccount());
                openingOutstanding.setCurrency(bank_b.getCurrency());
                Map<String, Object> journalResult = new HashMap<>();
                if (bankReconciliationSetting.getReconciliationdatasource().getValue() == ReconciliationDataSource.BankJournal.getValue()) {
                    journalResult = CmpCommonUtil.getCoinitloribalance(bankReconciliationSetting.getAccentity(), bank_b.getBankaccount(), bank_b.getCurrency(), bankReconciliationSetting.getEnableDate());
                } else {
                    journalResult = getVoucherBalance(bankReconciliationSetting.getAccentity(), bank_b.getBankaccount(), bankReconciliationSetting.getId(), bank_b.getCurrency());
                }
                if (journalResult != null) {
                    openingOutstanding.setDirection((Direction) journalResult.get("direction"));
                    openingOutstanding.setCoinitloribalance((BigDecimal) journalResult.get("coinitloribalance"));
                }
                openingOutstanding.setBankdirection(Direction.Credit);
                openingOutstanding.setBankinitoribalance(BigDecimal.ZERO);
                openingOutstanding.setId(ymsOidGenerator.nextId());
                openingOutstandings.add(openingOutstanding);
            }
        }
        return openingOutstandings;

    }


    /**
     * 期初余额设置确认操作
     *
     * @param params
     * @param response
     * @throws Exception
     */
    @RequestMapping("/update/opening/outstanding")
    @CMPDiworkPermission(IServicecodeConstant.OPENINGOUTSTANDING)
    public void updateOpeningOutstanding(@RequestBody CtmJSONObject params, HttpServletResponse response) throws Exception {
        CtmJSONObject data = new CtmJSONObject();
        checkIsNull(params);
        openingOutstandingService.updateOpeningOutstanding(params);
        renderJson(response, ResultMessage.success());
    }

    /**
     * 校验入参信息是否为空
     *
     * @param params
     * @return
     */
    public void checkIsNull(CtmJSONObject params) throws ParseException {
        Object bankinitoribalance = params.get("bankinitoribalance");
        Object bankdirection = params.get("bankdirection");
        Object direction = params.get("direction");
        Object coinitloribalance = params.get("coinitloribalance");
        if (Objects.isNull(bankinitoribalance)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101806"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041801C0", "银行方期初原币余额不能为空") /* "银行方期初原币余额不能为空" */);
        } else if (BigDecimal.ZERO.compareTo(params.getBigDecimal("bankinitoribalance")) > 0) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101807"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041801C2", "银行方期初原币余额不能小于0") /* "银行方期初原币余额不能小于0" */);
        }
        if (Objects.isNull(bankdirection)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101808"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041801C5", "银行方余额方向") /* "银行方余额方向" */);
        }
        if (Objects.isNull(direction)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101809"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041801C7", "企业方余额方向不能为空") /* "企业方余额方向不能为空" */);
        }
        if (Objects.isNull(coinitloribalance)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101810"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041801C8", "企业方期初原币余额不能为空") /* "企业方期初原币余额不能为空" */);
        } else if (BigDecimal.ZERO.compareTo(params.getBigDecimal("coinitloribalance")) > 0) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101811"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041801CB", "企业方期初原币余额不能小于0") /* "企业方期初原币余额不能小于0" */);
        }
    }

    /**
     * 根据总账余额信息
     *
     * @param bankAccountSettingVO
     * @param response
     */
    @RequestMapping("/getVoucherBalanceByuseOrg")
    public void getVoucherBalanceByuseOrg(BankAccountSettingVO bankAccountSettingVO, HttpServletResponse response) throws Exception {
        Map<String, Object> journalResult = null;
        journalResult = bankAccountSettingService.getVoucherBalance(bankAccountSettingVO);
        renderJson(response, ResultMessage.data(journalResult));
    }

    /**
     * 查询日记账余额信息
     *
     * @param bankAccountSettingVO
     * @param response
     */
    @RequestMapping("/getCoinitloribalanceByAccentity")
    public void getCoinitloribalanceByAccentity(BankAccountSettingVO bankAccountSettingVO, HttpServletResponse response) throws Exception {
        Map<String, Object> journalResult = null;
        journalResult = bankAccountSettingService.getCoinitloribalanceByAccentity(bankAccountSettingVO);
        renderJson(response, ResultMessage.data(journalResult));
    }

    /**
     * * 查询企业日记账余额:授权使用组织和余额
     *
     * @param openingOutstandingId 期初未达id
     * @param bankAccountSettingVO
     * @param response
     */
    @RequestMapping("/getUseOrgAndMny")
    public void getUseOrgAndMny(String openingOutstandingId, BankAccountSettingVO bankAccountSettingVO, HttpServletResponse response) throws Exception {
        List<Map<String, Object>> journalResult = null;
        journalResult = bankAccountSettingService.getUseOrgAndMny(openingOutstandingId, bankAccountSettingVO);
        renderJson(response, ResultMessage.data(journalResult));
    }

    private Map<String, Object> getVoucherBalance(String accentity, String bankaccount, Long bankreconciliationscheme, String currency) throws Exception {
        CtmJSONObject ret = CmpCommonUtil.getVoucherBalance(null, true, accentity, bankaccount, bankreconciliationscheme, currency, true);

        //币种获取
        CurrencyTenantDTO currencyDTO = baseRefRpcService.queryCurrencyById(currency);
        RoundingMode moneyRound = GetRoundModeUtils.getCurrencyPriceRoundMode(currency, 1);
        BigDecimal coinitloribalance = ret.getBigDecimal("subjectBalance").setScale(currencyDTO.getMoneydigit(), moneyRound);
        Map<String, Object> result = new HashMap<>();

        if (coinitloribalance.compareTo(BigDecimal.ZERO) <= 0) {
            result.put("direction", Direction.Credit);
        } else {
            result.put("direction", Direction.Debit);
        }
        result.put("coinitloribalance", coinitloribalance.abs());
        return result;
    }

    /**
     * 修改期初未达项老数据
     *
     * @param bankReconciliationSetting
     * @return
     * @throws Exception
     */
    private List<OpeningOutstanding> getOldOpeningOutstanding(BankReconciliationSetting bankReconciliationSetting) throws Exception {
        QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("bankreconciliationscheme").eq(bankReconciliationSetting.getId()));
        QuerySchema schema = new QuerySchema().addSelect("*");
        schema.addCondition(group);
        List<OpeningOutstanding> oldList = MetaDaoHelper.queryObject(OpeningOutstanding.ENTITY_NAME, schema, null);
        return oldList;
    }

    private void updateOpeningOutstandingStatus(BankReconciliationSetting bankReconciliationSetting, EnableStatus enableStatus) throws Exception {
        List<OpeningOutstanding> list = getOldOpeningOutstanding(bankReconciliationSetting);
        List<BizObject> updateList = new ArrayList<>();
        for (BizObject openingOutstanding : list) {
            openingOutstanding.set("enableStatus", enableStatus.getValue());
            EntityTool.setUpdateStatus(openingOutstanding);
            updateList.add(openingOutstanding);
        }
        MetaDaoHelper.update(OpeningOutstanding.ENTITY_NAME, updateList);
    }

    /**
     * 校验方案下的账户是否已对账
     *
     * @param id
     * @throws Exception
     */
    private void checkCheckflag(Long id) throws Exception {
        BankReconciliationSetting bankReconciliationSetting = MetaDaoHelper.findById(BankReconciliationSetting.ENTITY_NAME, id);

        List<BankReconciliationSetting_b> bank_bs = bankReconciliationSetting.get("bankReconciliationSetting_b");

        for (BizObject bankReconciliationSetting_b : bank_bs) {
            String bankaccount = bankReconciliationSetting_b.get(ICmpConstant.BANKACCOUNT);
            String currency = bankReconciliationSetting_b.get(ICmpConstant.CURRENCY);

            //校验日记账-----------------------
            QuerySchema schema = QuerySchema.create().addSelect("count(1) as  count");
            QueryConditionGroup JouralGroup = QueryConditionGroup.and(QueryCondition.name("bankaccount").eq(bankaccount), QueryCondition.name("currency").eq(currency), QueryCondition.name("checkflag").eq(true));
            schema.addCondition(JouralGroup);
            if (bankReconciliationSetting.getReconciliationdatasource().getValue() == ReconciliationDataSource.BankJournal.getValue()) {
                Map<String, Object> map = MetaDaoHelper.queryOne(Journal.ENTITY_NAME, schema);
                Long count = (Long) map.get("count");
                if (count > 0) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101815"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041801BE", "该对账方案存在已勾兑的数据，不允许取消启用。") /* "该对账方案存在已勾兑的数据，不允许取消启用。" */);
                }
            }
            //校验日记账-----------------------

            //校验对账单-----------------------
            schema = QuerySchema.create().addSelect("count(1) as  count");
            QueryConditionGroup bankGroup = QueryConditionGroup.and(QueryCondition.name("bankaccount").eq(bankaccount), QueryCondition.name("currency").eq(currency));

            if (bankReconciliationSetting.getReconciliationdatasource().getValue() == ReconciliationDataSource.Voucher.getValue()) {
                bankGroup.addCondition(QueryCondition.name("other_checkflag").eq(true));
            } else {
                bankGroup.addCondition(QueryCondition.name("checkflag").eq(true));
            }
            schema.addCondition(bankGroup);

            Map<String, Object> map = MetaDaoHelper.queryOne(BankReconciliation.ENTITY_NAME, schema);
            Long count = (Long) map.get("count");
            if (count > 0) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101815"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041801BE", "该对账方案存在已勾兑的数据，不允许取消启用。") /* "该对账方案存在已勾兑的数据，不允许取消启用。" */);
            }
            //校验对账单-----------------------
        }


    }

    /**
     * 银企对账设置查询
     *
     * @param useOrg      使用组织id
     * @param bankAccount 银行账户id
     * @param currency    币种id
     * @return BankReconciliationPlanVO 对账方案设置对象
     */
    @GetMapping("/find")
    public void find(@RequestParam String useOrg, @RequestParam String bankAccount, @RequestParam String currency, HttpServletResponse response) throws Exception {
        ReconciliationPlanParam param = new ReconciliationPlanParam(useOrg, bankAccount, currency);
        BankReconciliationPlanVO bankReconciliationPlanVO;
        bankReconciliationPlanVO = ctmCmpBankReconciliationSettingRpcService.find(param);
        renderJson(response, ResultMessage.data(bankReconciliationPlanVO));
    }

}


