/**
 * Copyright (c) 2019 ucsmy.com, All rights reserved.
 */
package com.yonyoucloud.fi.cmp.report;

import com.yonyou.iuap.org.dto.FinOrgDTO;
import com.yonyou.ucf.basedoc.model.CurrencyTenantDTO;
import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVO;
import com.yonyou.ucf.basedoc.model.EnterpriseCashVO;
import com.yonyou.ucf.basedoc.model.rpcparams.CurrencyBdParams;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.basecom.service.ref.OrgRpcService;
import com.yonyoucloud.fi.cmp.cmpentity.CaObject;
import com.yonyoucloud.fi.cmp.constant.IBussinessConstant;
import com.yonyoucloud.fi.cmp.constant.ISchemaConstant;
import com.yonyoucloud.fi.cmp.journal.Journal;
import com.yonyoucloud.fi.cmp.settlementdetail.SettlementDetail;
import com.yonyoucloud.fi.cmp.util.*;
import com.yonyoucloud.fi.cmp.util.basedoc.VendorQueryService;
import com.yonyoucloud.upc.pub.api.vendor.vo.vendor.VendorVO;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QueryOrderby;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.*;

@Service
public class CMReportServiceImpl implements ICMReportService {
    @Autowired
    BaseRefRpcService baseRefRpcService;
    //@Autowired
//    OrgRpcService orgRpcService;
    @Autowired
    VendorQueryService vendorQueryService;

    /**
     * 根据日期，会计主体查询汇总数据
     *
     * @param accentity
     * @param currency
     * @param date
     * @return
     * @throws Exception
     */
    @Override
    public CtmJSONObject getSummaryData(String accentity, String currency, Date date) throws Exception {
        checkIsNotNull(accentity,currency, date);
        Map<String, SettlementDetail> settlementDetailMap = DailyCompute.imitateDailyCompute(accentity, currency, null, null, "2", "2", date);
        SettlementDetail accountSum = new SettlementDetail();
        for(SettlementDetail settlementDetail:settlementDetailMap.values()){
            accountSum.setTodayorimoney(BigDecimalUtils.safeAdd(accountSum.getTodayorimoney(),settlementDetail.getTodayorimoney()));
            accountSum.setTodaydebitorimoneysum(BigDecimalUtils.safeAdd(accountSum.getTodaydebitorimoneysum(),settlementDetail.getTodaydebitorimoneysum()));
            accountSum.setTodaycreditorimoneysum(BigDecimalUtils.safeAdd(accountSum.getTodaycreditorimoneysum(),settlementDetail.getTodaycreditorimoneysum()));
            accountSum.setTodaydebitnum(BigDecimalUtils.safeAdd(accountSum.getTodaydebitnum(),settlementDetail.getTodaydebitnum()));
            accountSum.setTodaycreditnum(BigDecimalUtils.safeAdd(accountSum.getTodaycreditnum(),settlementDetail.getTodaycreditnum()));
        }
        CtmJSONObject jsonObject = new CtmJSONObject();
        CtmJSONObject jsonObject_result = new CtmJSONObject();
        jsonObject.put("todayorimoney",accountSum.getTodayorimoney());
        jsonObject.put("todaydebitorimoneysum",accountSum.getTodaydebitorimoneysum());
        jsonObject.put("todaydebitnum",accountSum.getTodaydebitnum());

        jsonObject.put("todaycreditorimoneysum",accountSum.getTodaycreditorimoneysum());
        jsonObject.put("todaycreditnum",accountSum.getTodaycreditnum());
        return jsonObject;
    }

    /**
     * 根据日期，会计主体查询账户类型汇总数据
     *
     * @param accentity
     * @param currency
     * @param date
     * @return
     * @throws Exception
     */
    @Override
    public CtmJSONArray getAccountSum(String accentity, String currency, Date date) throws Exception {
        checkIsNotNull(accentity,currency, date);
        Map<String, SettlementDetail> settlementDetailMap = DailyCompute.imitateDailyCompute(accentity, currency, null, null, "2", "2", date);
        SettlementDetail currencyBankSum = new SettlementDetail();
        SettlementDetail currencyCashSum = new SettlementDetail();
        for(Map<String, Object> account:settlementDetailMap.values()){
            if((account.get("todaydebitorimoneysum") == null || BigDecimal.ZERO.compareTo((BigDecimal) account.get("todaydebitorimoneysum")) == 0)
                    && (account.get("todaycreditorimoneysum") == null || BigDecimal.ZERO.compareTo((BigDecimal) account.get("todaycreditorimoneysum")) == 0)
                    && (account.get("todayorimoney") == null || BigDecimal.ZERO.compareTo((BigDecimal) account.get("todayorimoney")) == 0)){
                continue;
            }
            SettlementDetail settlementDetail = new SettlementDetail();
            settlementDetail.init(account);
            //设置组织本币
            if(!StringUtils.isEmpty(settlementDetail.getBankaccount())){
                currencyBankSum.setTodaydebitorimoneysum(BigDecimalUtils.safeAdd(currencyBankSum.getTodaydebitorimoneysum(),settlementDetail.getTodaydebitorimoneysum()));
                currencyBankSum.setTodaycreditorimoneysum(BigDecimalUtils.safeAdd(currencyBankSum.getTodaycreditorimoneysum(),settlementDetail.getTodaycreditorimoneysum()));
                currencyBankSum.setTodayorimoney(BigDecimalUtils.safeAdd(currencyBankSum.getTodayorimoney(),settlementDetail.getTodayorimoney()));
                currencyBankSum.setTodaydebitnum(BigDecimalUtils.safeAdd(currencyBankSum.getTodaydebitnum(),settlementDetail.getTodaydebitnum()));
                currencyBankSum.setTodaycreditnum(BigDecimalUtils.safeAdd(currencyBankSum.getTodaycreditnum(),settlementDetail.getTodaycreditnum()));
            }else if(!StringUtils.isEmpty(settlementDetail.getCashaccount())){
                currencyCashSum.setTodaydebitorimoneysum(BigDecimalUtils.safeAdd(currencyCashSum.getTodaydebitorimoneysum(),settlementDetail.getTodaydebitorimoneysum()));
                currencyCashSum.setTodaycreditorimoneysum(BigDecimalUtils.safeAdd(currencyCashSum.getTodaycreditorimoneysum(),settlementDetail.getTodaycreditorimoneysum()));
                currencyCashSum.setTodayorimoney(BigDecimalUtils.safeAdd(currencyCashSum.getTodayorimoney(),settlementDetail.getTodayorimoney()));
                currencyCashSum.setTodaydebitnum(BigDecimalUtils.safeAdd(currencyCashSum.getTodaydebitnum(),settlementDetail.getTodaydebitnum()));
                currencyCashSum.setTodaycreditnum(BigDecimalUtils.safeAdd(currencyCashSum.getTodaycreditnum(),settlementDetail.getTodaycreditnum()));
            }
        }
        CtmJSONObject jsonObject_bank = new CtmJSONObject();
        CtmJSONObject jsonObject_cash = new CtmJSONObject();
        CtmJSONObject jsonObject_result = new CtmJSONObject();
        CtmJSONArray array = new CtmJSONArray();
        jsonObject_bank.put("itemParams","bank");
        jsonObject_bank.put("bank_todayorimoney",currencyBankSum.getTodayorimoney());
        jsonObject_bank.put("bank_todaydebitorimoneysum",currencyBankSum.getTodaydebitorimoneysum());
        jsonObject_bank.put("bank_todaycreditorimoneysum",currencyBankSum.getTodaycreditorimoneysum());
        jsonObject_bank.put("bank_todaydebitnum",currencyBankSum.getTodaydebitnum());
        jsonObject_bank.put("bank_todaycreditnum",currencyBankSum.getTodaycreditnum());

        jsonObject_cash.put("itemParams","cash");
        jsonObject_cash.put("cash_todayorimoney",currencyCashSum.getTodayorimoney());
        jsonObject_cash.put("cash_todaydebitorimoneysum",currencyCashSum.getTodaydebitorimoneysum());
        jsonObject_cash.put("cash_todaycreditorimoneysum",currencyCashSum.getTodaycreditorimoneysum());
        jsonObject_cash.put("cash_todaydebitnum",currencyCashSum.getTodaydebitnum());
        jsonObject_cash.put("cash_todaycreditnum",currencyCashSum.getTodaycreditnum());
        array.add(jsonObject_bank);
        array.add(jsonObject_cash);
        return array;
    }

    /**
     * 根据日期，会计主体,账户类型查询账户列表
     *
     * @param params
     * @return
     * @throws Exception
     */
    @Override
    public List<SettlementDetail> getAccountList(CtmJSONObject params) throws Exception {
        String accountType = params.getString("itemParams");//账户类型，现金
        String accentity = params.getString(IBussinessConstant.ACCENTITY);
        Date date = params.getDate("date");
        String currency = params.getString("currency");
        int pageNum = Integer.parseInt(params.getString("pageNum"));
        int pageSize = Integer.parseInt(params.getString("pageSize"));
        checkIsNotNull(accentity,currency, date);
        Map<String, SettlementDetail> settlementDetailMap = DailyCompute.imitateDailyCompute(accentity, currency, null, null, "2", "2", date);
        List<SettlementDetail> queryaccount = new ArrayList<SettlementDetail>();

        for(Map<String, Object> account:settlementDetailMap.values()) {
            if ((account.get("yesterdaylocalmoney") == null || BigDecimal.ZERO.compareTo((BigDecimal) account.get("yesterdaylocalmoney")) == 0)
                    && (account.get("yesterdayorimoney") == null || BigDecimal.ZERO.compareTo((BigDecimal) account.get("yesterdayorimoney")) == 0)
                    && (account.get("todaydebitlocalmoneysum") == null || BigDecimal.ZERO.compareTo((BigDecimal) account.get("todaydebitlocalmoneysum")) == 0)
                    && (account.get("todaydebitorimoneysum") == null || BigDecimal.ZERO.compareTo((BigDecimal) account.get("todaydebitorimoneysum")) == 0)
                    && (account.get("todaycreditlocalmoneysum") == null || BigDecimal.ZERO.compareTo((BigDecimal) account.get("todaycreditlocalmoneysum")) == 0)
                    && (account.get("todaycreditorimoneysum") == null || BigDecimal.ZERO.compareTo((BigDecimal) account.get("todaycreditorimoneysum")) == 0)
                    && (account.get("todaylocalmoney") == null || BigDecimal.ZERO.compareTo((BigDecimal) account.get("todaylocalmoney")) == 0)
                    && (account.get("todayorimoney") == null || BigDecimal.ZERO.compareTo((BigDecimal) account.get("todayorimoney")) == 0)) {
                continue;
            }
            SettlementDetail settlementDetail = new SettlementDetail();
            settlementDetail.init(account);
            if (!StringUtils.isEmpty(settlementDetail.getBankaccount())&&"bank".equals(accountType)) {
                EnterpriseBankAcctVO enterpriseBankAcctVO = baseRefRpcService.queryEnterpriseBankAccountById(settlementDetail.getBankaccount());
                if (enterpriseBankAcctVO!=null){
                    settlementDetail.put("bankName",enterpriseBankAcctVO.getName());
                }
                settlementDetail.put("accountId",settlementDetail.getBankaccount());
                queryaccount.add(settlementDetail);
            } else if (!StringUtils.isEmpty(settlementDetail.getCashaccount())&&"cash".equals(accountType)) {
                EnterpriseCashVO stringObjectMap =  baseRefRpcService.queryOneCashAcctByCondition(settlementDetail.getCashaccount());
                if (stringObjectMap!=null){
                    settlementDetail.put("bankName",stringObjectMap.getName());
                }
                settlementDetail.put("accountId",settlementDetail.getCashaccount());
                queryaccount.add(settlementDetail);
            }
        }
        return getDataByPage(queryaccount,pageNum,pageSize);
    }


    private List<SettlementDetail> getDataByPage(List<SettlementDetail> queryaccount,int pageNum,int pageSize){
        if (queryaccount==null||queryaccount.size()==0) return queryaccount;
        int totalCount = queryaccount.size();

        if (pageNum==1&&totalCount<pageSize){
          return queryaccount;
        } else if (totalCount%pageSize==0){//总计20条  每页10条  2页
            if (pageNum>totalCount/pageSize){
                return Collections.emptyList();
            }else {
                return queryaccount.subList((pageNum-1)*pageSize,pageNum*pageSize);
            }
        }else {
            if (pageNum>totalCount/pageSize+1){//总计16条  每页10条  2页
                return Collections.emptyList();
            }else {
                if (pageNum==totalCount/pageSize+1) return queryaccount.subList((pageNum-1)*pageSize,totalCount);
                return queryaccount.subList((pageNum-1)*pageSize,pageNum*pageSize);
            }
        }
    }

    /**
     * 根据账户id,收支类型,日期，会计主体查询账户收支详情
     *
     * @param params
     * @return
     * @throws Exception
     */
    @Override
    public List<CtmJSONObject> getAccountDetail(CtmJSONObject params) throws Exception {

        String accountId = params.getString("accountId");//账户Id
        String fundType = params.getString("fundType");//收支类型1：收入，2:支出
        String accentity = params.getString(IBussinessConstant.ACCENTITY);
        Date date = params.getDate("date");
        String currency = params.getString("currency");
        int pageNum = Integer.parseInt(params.getString("pageNum"));
        int pageSize = Integer.parseInt(params.getString("pageSize"));

        checkIsNotNull(accentity,currency, date);
        QuerySchema queryJournalForXZ = QuerySchema.create().addSelect("*");
        QueryConditionGroup condition = new QueryConditionGroup(ConditionOperator.and);

        List<QueryCondition> conditionList = new ArrayList<>();
        conditionList.add(QueryCondition.name("cashaccount").eq(accountId));
        conditionList.add(QueryCondition.name("bankaccount").eq(accountId));
        if (!conditionList.isEmpty()) {
            condition.addCondition(QueryConditionGroup.or(conditionList.toArray(new QueryCondition[0])));
        }
        condition.addCondition(QueryCondition.name(IBussinessConstant.ACCENTITY).eq(accentity),QueryCondition.name("currency").eq(currency));
        if ("1".equals(fundType)){//借
            condition.addCondition(QueryCondition.name("direction").eq("1"));
        }else {
            condition.addCondition(QueryCondition.name("direction").eq("2"));
        }
        condition.addCondition(QueryCondition.name("settlestatus").eq(2));
        condition.addCondition(QueryCondition.name("initflag").eq(0));
        condition.addCondition(QueryCondition.name("dzdate").eq(DateUtils.dateFormat(date,DateUtils.DATE_PATTERN)));
        queryJournalForXZ.addCondition(condition);
        queryJournalForXZ.addOrderBy(new QueryOrderby("id", "asc"));
        queryJournalForXZ.addPager(pageNum,pageSize);
        List<Map<String, Object>> queryJournal = MetaDaoHelper.query(Journal.ENTITY_NAME, queryJournalForXZ);

        List<CtmJSONObject> list = new ArrayList<>();
        // begin 效率优化，批量查询处理
        Map<Long, String> supplierMap = new HashMap<>();
        for (Map<String, Object> map:queryJournal){
            if (map.get("caobject")!=null && ((Integer)map.get("caobject") == CaObject.Supplier.getValue()) && map.get("supplier") != null){
                supplierMap.put(Long.valueOf(map.get("supplier").toString()), null);
            }
        }
        if (MapUtils.isNotEmpty(supplierMap)) {
            List<Long> supplierIds = new ArrayList<>(supplierMap.keySet());
            List<VendorVO> vendorFieldByIdList = vendorQueryService.getVendorFieldByIdList(supplierIds);
            if (CollectionUtils.isNotEmpty(vendorFieldByIdList)) {
                for (VendorVO vendorVO: vendorFieldByIdList) {
                    supplierMap.put(vendorVO.getId(), vendorVO.getName());
                }
            }
        }
        // end
        for (Map<String, Object> map:queryJournal){
            CtmJSONObject obj = new CtmJSONObject();
            if (map.get("caobject")!=null&&(Integer)map.get("caobject") == CaObject.Customer.getValue()){
                QuerySchema customerSchema = QuerySchema.create().addSelect("id,name,code");
                QueryConditionGroup customer_condition = new QueryConditionGroup();
                customer_condition.addCondition(QueryCondition.name("id").eq(map.get("customer")));
                customerSchema.addCondition(customer_condition);
                List<Map<String, Object>> stringObjectMap = MetaDaoHelper.query("aa.merchant.Merchant", customerSchema, ISchemaConstant.MDD_SCHEMA_PRODUCTCENTER);
                obj.put("caobject", CaObject.Customer.getName());
                if (stringObjectMap.size()>0){
                    obj.put("caobject_name",stringObjectMap.get(0).get("name"));
                }
            }else if (map.get("caobject")!=null&&(Integer)map.get("caobject") == CaObject.Supplier.getValue()){
                obj.put("caobject",CaObject.Supplier.getName());
                if (map.get("supplier") != null) {
                    obj.put("caobject_name", supplierMap.get(Long.valueOf(map.get("supplier").toString())));
                }
            }else if (map.get("caobject")!=null&&(Integer)map.get("caobject") == CaObject.Employee.getValue()){
                QuerySchema customerSchema = QuerySchema.create().addSelect("*");
                QueryConditionGroup customer_condition = new QueryConditionGroup();
                customer_condition.addCondition(QueryCondition.name("id").eq(map.get("employee")));
                customerSchema.addCondition(customer_condition);
                List<Map<String, Object>> stringObjectMap = MetaDaoHelper.query("bd.staff.Staff", customerSchema,ISchemaConstant.MDD_SCHEMA_STAFFCENTER);
                obj.put("caobject",CaObject.Employee.getName());
                if (stringObjectMap.size()>0){
                    obj.put("caobject_name",stringObjectMap.get(0).get("name"));
                }
            }else {
                obj.put("caobject", CaObject.Other.getName());
                obj.put("caobject_name","");
            }
            String bankName = "";
            if (map.get("bankaccount")!=null&&!map.get("bankaccount").equals("")){
                EnterpriseBankAcctVO bankaccount = baseRefRpcService.queryEnterpriseBankAccountById(map.get("bankaccount").toString());
                bankName= bankaccount.getName();
            }
            if (map.get("cashaccount")!=null&&!map.get("cashaccount").equals("")){
                EnterpriseCashVO cashaccount = baseRefRpcService.queryEnterpriseCashAcctById(map.get("cashaccount").toString());
                bankName= cashaccount.getName();
            }

            if ("1".equals(fundType)) {//借
                obj.put("numer",map.get("debitoriSum"));
            }else {
                obj.put("numer",map.get("creditoriSum"));
            }
            obj.put("bankName",bankName);
            obj.put("description",map.get("description"));
            list.add(obj);
        }

        return list;
    }

    /**
     * 获取租户下的所有币种
     *
     * @return
     */
    @Override
    public List<CurrencyTenantDTO>  getCurrenctData() throws Exception {
        QuerySchema schema = QuerySchema.create();
        schema.addSelect("*");
        //schema.appendQueryCondition(QueryCondition.name("tenant").eq(AppContext.getTenantId())) ;
//        List<Map<String, Object>> vos = MetaDaoHelper.query("bd.currencytenant.CurrencyTenantVO", schema,ISchemaConstant.MDD_SCHEMA_UCFBASEDOC) ;
        List<CurrencyTenantDTO> vos = baseRefRpcService.queryCurrencyByParams(new CurrencyBdParams());
        return vos;
    }

    @Override
    public CtmJSONObject getOwnerCurrency(CtmJSONObject params) throws Exception {
//        List<Map<String, Object>> stringObjectMap = QueryBaseDocUtils.queryAccRawEntityByAccEntityId(params.get(IBussinessConstant.ACCENTITY));
        FinOrgDTO finOrgDTO = AccentityUtil.getFinOrgDTOByAccentityId(params.get(IBussinessConstant.ACCENTITY).toString());
        CurrencyTenantDTO currencyTenantDTO = baseRefRpcService.queryCurrencyById(finOrgDTO.getCurrency());
        if (currencyTenantDTO!=null){
            CtmJSONObject jsonObject = new CtmJSONObject();
            jsonObject.put("id",currencyTenantDTO.getId());
            jsonObject.put("name",currencyTenantDTO.getName());
            jsonObject.put("code",currencyTenantDTO.getCode());
            return jsonObject;
        }
        return null;
    }


    private void checkIsNotNull(String accentity,String currency,Date date) throws ParseException {
        if (StringUtils.isEmpty(accentity)){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102266"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B000D6", "请选择会计主体") /* "请选择会计主体" */);
        }
        if (StringUtils.isEmpty(currency)){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102267"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B000D7", "请选择币种") /* "请选择币种" */);
        }
        if (date==null){
            date = new Date();
        }
    }


}
