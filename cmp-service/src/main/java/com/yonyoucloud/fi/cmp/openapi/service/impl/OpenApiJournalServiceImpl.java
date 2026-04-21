package com.yonyoucloud.fi.cmp.openapi.service.impl;

import cn.hutool.core.util.PageUtil;
import com.yonyou.iuap.org.dto.FinOrgDTO;
import com.yonyou.iuap.org.dto.FundsOrgDTO;
import com.yonyou.ucf.basedoc.model.CurrencyTenantDTO;
import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVO;
import com.yonyou.ucf.basedoc.model.rpcparams.CurrencyBdParams;
import com.yonyou.ucf.basedoc.model.rpcparams.EnterpriseParams;
import com.yonyou.ucf.basedoc.service.itf.IEnterpriseBankAcctService;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.orgs.FinOrgQueryServiceComponent;
import com.yonyou.yonbip.ctm.orgs.FundsOrgQueryServiceComponent;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.cmp.journal.Journal;
import com.yonyoucloud.fi.cmp.openapi.service.OpenApiJournalService;
import com.yonyoucloud.fi.cmp.util.DateUtils;
import com.yonyoucloud.fi.cmp.util.QueryBaseDocUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Slf4j
@Transactional(rollbackFor = RuntimeException.class)
public class OpenApiJournalServiceImpl implements OpenApiJournalService {

    @Autowired
    private BaseRefRpcService baseRefRpcService;
    @Autowired
    IEnterpriseBankAcctService iEnterpriseBankAcctService;
    @Autowired
    private FundsOrgQueryServiceComponent fundsOrgQueryServiceComponent;
    @Autowired
    FinOrgQueryServiceComponent finOrgQueryService;



    @Override
    public CtmJSONObject queryJournalByParam(CtmJSONObject param) throws Exception {

        CtmJSONArray cashAccountArray = param.getJSONArray("cashaccount");
        CtmJSONArray bankAccountArray = param.getJSONArray("bankaccount");
        CtmJSONArray accentityArray = param.getJSONArray("accentity");
        CtmJSONArray accentityRawArray = param.getJSONArray("accentityRaw");
        CtmJSONArray currencyArray = param.getJSONArray("currency");
        String dzstartdate = param.getString("dzstartdate");//登账开始日期
        String dzenddate   = param.getString("dzenddate");//登账结束日期
        // 校验 pageIndex 和 pageSize 为空和小于0时，默认为1 和 10
        int pageIndex   = param.getInteger("pageIndex") == null || param.getInteger("pageIndex") < 0 ? 1 : param.getInteger("pageIndex");
        int pageSize   = param.getInteger("pageSize") == null || param.getInteger("pageSize") < 0 ? 10 : param.getInteger("pageSize");
        // 校验 dzstartdate 和 dzenddate 格式是否正确
        try{
            Date date = StringUtils.isNotEmpty(dzstartdate) ? DateUtils.dateParse(dzstartdate, "yyyy-MM-dd") : null;
        } catch (Exception e){
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1E830D9005200007", "登账开始日期格式不合法！") /* "登账开始日期格式不合法！" */);
        }
        try{
            Date date = StringUtils.isNotEmpty(dzenddate) ? DateUtils.dateParse(dzenddate, "yyyy-MM-dd") : null;
        } catch (Exception e){
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1E830F4605200002", "登账结束日期格式不合法！") /* "登账结束日期格式不合法！" */);
        }
        //银行账户、现金账户、资金组织、核算会计主体必须输入其一
        if ((cashAccountArray == null || cashAccountArray.isEmpty()) &&
                (bankAccountArray == null || bankAccountArray.isEmpty()) &&
                (accentityArray == null || accentityArray.isEmpty()) &&
                (accentityRawArray == null || accentityRawArray.isEmpty())) {
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1E830F9005200005", "银行账户、现金账户、资金组织、核算会计主体必须输入其一！") /* "银行账户、现金账户、资金组织、核算会计主体必须输入其一！" */);
        }

        /** 币种编码或ID转成map */
        HashMap<String, CurrencyTenantDTO> currencyMap = buildCurrencyMap(currencyArray);

        // 新增：构建现金账户map
        HashMap<String, Map<String, Object>> cashAccountMap = buildCashAccountMap(cashAccountArray);

        // 新增：构建银行账户map
        HashMap<String, EnterpriseBankAcctVO> bankAccountMap = buildBankAccountMap(bankAccountArray);

        // 新增：构建资金组织map
        HashMap<String, FundsOrgDTO> accEntityMap = buildFundsOrgMap(accentityArray);

        // 新增：构建核算会计主体原始map
        HashMap<String, FinOrgDTO> accEntityRawMap = buildFinOrgMap(accentityRawArray);

        //银行账户、现金账户、资金组织、核算会计主体必须输入其一,翻译处理后的参数为空，结果也为空
        if (cashAccountMap.isEmpty() && bankAccountMap.isEmpty() && accEntityMap.isEmpty() && accEntityRawMap.isEmpty()) {
            CtmJSONObject result = new CtmJSONObject();
            CtmJSONObject resultChild = new CtmJSONObject();
            result.put("code", 200);
            resultChild.put("totalCount",0);
            resultChild.put("pageNum",0);
            resultChild.put("recordList", new ArrayList<>());
            result.put("message", "success");
            result.put("data", resultChild);
            return result;
        }

        //查询数据，注意穿透
        QuerySchema querySchema = QuerySchema.create().addSelect(
                "accentity, accentity.code as accentityCode, accentity.name as accentityName, parentAccentity, parentAccentity.code as parentAccentityCode, parentAccentity.name as parentAccentityName, customer, employee," +
                        "innerunit, supplier, dzdate, vouchdate, bankbilldate, billnodate, enableDate, paydate,                                                  " +
                        "checkdate, period, voucherPeriod, cashaccount, cashaccountno, bankaccount, bankaccountno, customerbankaccount,                          " +
                        "employeeaccount, innerunitbankaccount, otherbankaccount, othercashaccount, supplierbankaccount, creditoriSum, creditnatSum, debitoriSum," +
                        "debitnatSum, natbalance, oribalance, currency, currency.name as currencyName, natCurrency, exchangerate, banktype,                      " +
                        "bankbilltype, journaltype, projectCode, project, rptype, tradetype, settlemode, topbilltype,                                            " +
                        "billtype, paymentstatus, paystatus, payway, initflag, sealflag, insidecheckflag, checkflag,                                             " +
                        "noteno, billnum, transeqno, voucherNo, topsrcbillid, topsrcbillno, srcbillno, billno,                                                   " +
                        "checkno, batno, orderno, porderid, description, paymessage, bankreturnmsg, checkmatch,                                                  " +
                        "sourceheadclue, sourcelineclue, srcbillitemid, srcbillitemno, srcitem, direction, reconciliation, reconciliationdatasource,             " +
                        "refund, relevanceid, dept, operator, org, bankcheckno, bankreconciliationscheme, bankreconciliationsettingid,                           " +
                        "checkman, costproject, customerbankname, othername, othertitle, payman, settlestatus, supplierbankname, tallyed, topsrcitem,            "+
                        "bookkeeper, caobject, capBizObj, capBizObjbankaccount, id ");
        QueryConditionGroup group = new QueryConditionGroup(ConditionOperator.and);
        // 添加查询条件
        if (!cashAccountMap.isEmpty()) {
            group.addCondition(QueryCondition.name("cashaccount").in(cashAccountMap.keySet()));
        }
        if (!bankAccountMap.isEmpty()) {
            group.addCondition(QueryCondition.name("bankaccount").in(bankAccountMap.keySet()));
        }
        if (!accEntityMap.isEmpty()) {
            group.addCondition(QueryCondition.name("accentity").in(accEntityMap.keySet()));
        }
        if (!accEntityRawMap.isEmpty()) {
            group.addCondition(QueryCondition.name("accentityRaw").in(accEntityRawMap.keySet()));
        }
        if (!currencyMap.isEmpty()) {
            group.addCondition(QueryCondition.name("currency").in(currencyMap.keySet()));
        }
        if(StringUtils.isNotEmpty(dzstartdate)){
            group.addCondition(QueryCondition.name("dzdate").egt(dzstartdate));
        }
        if(StringUtils.isNotEmpty(dzenddate)){
            group.addCondition(QueryCondition.name("dzdate").elt(dzenddate));
        }
        querySchema.addCondition(group);
        QuerySchema countSchema = QuerySchema.create().addSelect("count(id)");
        countSchema.addCondition(group);
        Map<String, Object> bankReconCountMap = MetaDaoHelper.queryOne(Journal.ENTITY_NAME, countSchema);
        int totalSize = Integer.parseInt(bankReconCountMap.get("count").toString());
        querySchema.addPager(pageIndex, pageSize);
        List<Map<String, Object>> infoMapList = MetaDaoHelper.query(Journal.ENTITY_NAME, querySchema);
        int pageNum = PageUtil.totalPage(totalSize, pageSize);
        // 构建返回结果
        CtmJSONObject result = new CtmJSONObject();
        CtmJSONObject resultChild = new CtmJSONObject();
        result.put("code", 200);
        resultChild.put("totalCount",totalSize);
        resultChild.put("pageNum",pageNum);
        resultChild.put("recordList", infoMapList);
        result.put("message", "success");
        result.put("data", resultChild);
        return result;
    }

    /**
     * 构建资金组织map
     * @param accentityArray
     * @return
     * @throws Exception
     */
    private HashMap<String, FinOrgDTO> buildFinOrgMap(CtmJSONArray accentityArray) throws Exception {
        HashMap<String, FinOrgDTO> accentityMap = new HashMap<>();
        if(accentityArray == null || accentityArray.isEmpty()){
            return accentityMap;
        }
        List<FinOrgDTO> finOrgListById = finOrgQueryService.listByIds(accentityArray.toJavaList(String.class));

        List<FinOrgDTO> finOrgListByCode = finOrgQueryService.listByCodes(accentityArray.toJavaList(String.class));
        //构建map,根据id去重
        if (!(finOrgListById == null || finOrgListById.isEmpty())) {
            for (FinOrgDTO fundsOrgDTO : finOrgListById) {
                accentityMap.put(fundsOrgDTO.getId(), fundsOrgDTO);
            }
        }
        if (!(finOrgListByCode == null || finOrgListByCode.isEmpty())) {
            for (FinOrgDTO fundsOrgDTO : finOrgListByCode) {
                accentityMap.put(fundsOrgDTO.getId(), fundsOrgDTO);
            }
        }
        return accentityMap;
    }

    /**
     * 构建资金组织map
     * @param accentityArray
     * @return
     * @throws Exception
     */
    private HashMap<String, FundsOrgDTO> buildFundsOrgMap(CtmJSONArray accentityArray) throws Exception {
        HashMap<String, FundsOrgDTO> accentityMap = new HashMap<>();
        if(accentityArray == null || accentityArray.isEmpty()){
            return accentityMap;
        }
        List<FundsOrgDTO> fundsOrgListById = fundsOrgQueryServiceComponent.listByIds(accentityArray.toJavaList(String.class));

        List<FundsOrgDTO> fundsOrgListByCode = fundsOrgQueryServiceComponent.listByCodes(accentityArray.toJavaList(String.class));
        //构建map,根据id去重
        if (!(fundsOrgListById == null || fundsOrgListById.isEmpty())) {
            for (FundsOrgDTO fundsOrgDTO : fundsOrgListById) {
                accentityMap.put(fundsOrgDTO.getId(), fundsOrgDTO);
            }
        }
        if (!(fundsOrgListByCode == null || fundsOrgListByCode.isEmpty())) {
            for (FundsOrgDTO fundsOrgDTO : fundsOrgListByCode) {
                accentityMap.put(fundsOrgDTO.getId(), fundsOrgDTO);
            }
        }
        return accentityMap;
    }

    /**
     * 构建币种map
     *
     * @param currencyArray
     * @return
     * @throws Exception
     */
    private HashMap<String, CurrencyTenantDTO> buildCurrencyMap(CtmJSONArray currencyArray) throws Exception {
        HashMap<String, CurrencyTenantDTO> currencyMap = new HashMap<>();
        if(currencyArray == null || currencyArray.isEmpty()){
            return currencyMap;
        }
        CurrencyBdParams currencyBdParamsByCode = new CurrencyBdParams();
        currencyBdParamsByCode.setCodeList(currencyArray.toJavaList(String.class));
        List<CurrencyTenantDTO> currencylistByCode = baseRefRpcService.queryCurrencyByParams(currencyBdParamsByCode);

        CurrencyBdParams currencyBdParamsById = new CurrencyBdParams();
        currencyBdParamsById.setIdList(currencyArray.toJavaList(String.class));
        List<CurrencyTenantDTO> currencylistById = baseRefRpcService.queryCurrencyByParams(currencyBdParamsById);
        //构建map,根据id去重
        if (!(currencylistByCode == null || currencylistByCode.isEmpty())) {
            for (CurrencyTenantDTO currencyTenantDTO : currencylistByCode) {
                currencyMap.put(currencyTenantDTO.getId(), currencyTenantDTO);
            }
        }
        if (!(currencylistById == null || currencylistById.isEmpty())) {
            for (CurrencyTenantDTO currencyTenantDTO : currencylistById) {
                currencyMap.put(currencyTenantDTO.getId(), currencyTenantDTO);
            }
        }
        return currencyMap;
    }

    /**
     * 构建现金账户map
     * @param cashAccountArray
     * @return
     * @throws Exception
     */
    private HashMap<String, Map<String, Object>> buildCashAccountMap(CtmJSONArray cashAccountArray) throws Exception {
        HashMap<String, Map<String, Object>> cashAccountMap = new HashMap<>();
        if(cashAccountArray == null || cashAccountArray.isEmpty()){
            return cashAccountMap;
        }
        List<Map<String, Object> > cashAccountMapByAccount =QueryBaseDocUtils.queryEnterpriseCashAccountByAccounts(cashAccountArray.toJavaList(String.class));
        List<Map<String, Object> > cashAccountMapById =QueryBaseDocUtils.queryEnterpriseCashAccountByIds(cashAccountArray.toJavaList(String.class));
        //构建map,根据id去重
        if (!(cashAccountMapById == null || cashAccountMapById.isEmpty())) {
            for (Map<String, Object> account : cashAccountMapById) {
                cashAccountMap.put(account.get("id").toString(), account);
            }
        }
        //构建map,根据id去重
        if (!(cashAccountMapByAccount == null || cashAccountMapByAccount.isEmpty())) {
            for (Map<String, Object> account : cashAccountMapByAccount) {
                cashAccountMap.put(account.get("id").toString(), account);
            }
        }
        return cashAccountMap;
    }

    /**
     * 构建银行账户map
     * @param bankAccountArray
     * @return
     * @throws Exception
     */
    private HashMap<String, EnterpriseBankAcctVO> buildBankAccountMap(CtmJSONArray bankAccountArray) throws Exception {
        HashMap<String, EnterpriseBankAcctVO> bankAccountMap = new HashMap<>();
        if(bankAccountArray == null || bankAccountArray.isEmpty()){
            return bankAccountMap;
        }
        EnterpriseParams enterpriseParamsByCode = new EnterpriseParams();
        enterpriseParamsByCode.setCodeList(bankAccountArray.toJavaList(String.class));
        //启用状态集合:0-未启用  1-启用  2-停用
        enterpriseParamsByCode.setEnables(new ArrayList<>(Arrays.asList(0,1,2)));
        List<EnterpriseBankAcctVO> enterpriseBankAcctVOByCode = iEnterpriseBankAcctService.queryByCondition(enterpriseParamsByCode);

        EnterpriseParams enterpriseParamsById = new EnterpriseParams();
        enterpriseParamsById.setIdList(bankAccountArray.toJavaList(String.class));
        //启用状态集合:0-未启用  1-启用  2-停用
        enterpriseParamsById.setEnables(new ArrayList<>(Arrays.asList(0,1,2)));
        List<EnterpriseBankAcctVO> enterpriseBankAcctVOById = iEnterpriseBankAcctService.queryByCondition(enterpriseParamsById);

        //构建map,根据id去重
        if (!(enterpriseBankAcctVOByCode == null || enterpriseBankAcctVOByCode.isEmpty())) {
            for (EnterpriseBankAcctVO bankAcctVO : enterpriseBankAcctVOByCode) {
                bankAccountMap.put(bankAcctVO.getId(), bankAcctVO);
            }
        }
        if (!(enterpriseBankAcctVOById == null || enterpriseBankAcctVOById.isEmpty())) {
            for (EnterpriseBankAcctVO bankAcctVO : enterpriseBankAcctVOById) {
                bankAccountMap.put(bankAcctVO.getId(), bankAcctVO);
            }
        }
        return bankAccountMap;
    }

}
