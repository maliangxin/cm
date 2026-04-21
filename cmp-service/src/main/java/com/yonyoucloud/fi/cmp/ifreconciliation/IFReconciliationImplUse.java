package com.yonyoucloud.fi.cmp.ifreconciliation;

import com.alibaba.fastjson.JSON;
import com.yonyou.iuap.org.dto.FinOrgDTO;
import com.yonyou.ucf.basedoc.model.CurrencyTenantDTO;
import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVO;
import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVOWithRange;
import com.yonyou.ucf.basedoc.model.EnterpriseCashVO;
import com.yonyou.ucf.basedoc.model.rpcparams.EnterpriseParams;
import com.yonyou.ucf.basedoc.service.itf.IEnterpriseBankAcctService;
import com.yonyou.ucf.mdd.common.utils.json.GsonHelper;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.json.JsonUtils;
import com.yonyou.yonbip.ctm.logger.business.CTMCMPBusinessLogService;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.cmp.cmpentity.EventSource;
import com.yonyoucloud.fi.cmp.constant.IBussinessConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.IMsgConstant;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import com.yonyoucloud.fi.cmp.enums.UpgradeSignEnum;
import com.yonyoucloud.fi.cmp.initdata.InitData;
import com.yonyoucloud.fi.cmp.journal.Journal;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.Direction;
import com.yonyoucloud.fi.cmp.util.AccentityUtil;
import com.yonyoucloud.fi.cmp.util.BigDecimalUtils;
import com.yonyoucloud.fi.cmp.util.StringUtils;
import com.yonyoucloud.fi.cmp.util.basedoc.EnterpriseBankQueryService;
import com.yonyoucloud.fi.evarc.detail.dto.v1.ExecutorDetailReportDTO;
import com.yonyoucloud.fi.evarc.detail.dto.v1.ReconcDetailResultDTO;
import com.yonyoucloud.fi.evarc.detail.dto.v1.ReconcProjectDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.schema.ConditionExpression;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @Description: 业财对账实现
 * @Author: wsl
 * @Creaed Date: 2019年12月31日
 * @Version:
 */
@Service
@Slf4j
public class IFReconciliationImplUse implements IFReconciliationService {

    @Autowired
    CTMCMPBusinessLogService businessLogService;
    @Autowired
    IEnterpriseBankAcctService iEnterpriseBankAcctService;
    @Autowired
    EnterpriseBankQueryService enterpriseBankQueryService;

    @Autowired
    BaseRefRpcService baseRefRpcService;
    //@Autowired
//    OrgRpcService orgRpcService;

    @Value("${mdd_schema.ucfbasedoc}")
    private String ucfbasedoc;

//    @Value(value = "iuap_cloud_basedoc")
//    private volatile String ucfbasedoc;

    @Override
    public CtmJSONObject getReconciliationDataDetail(CtmJSONObject obj) {
        FReconciliation fReconciliation = (FReconciliation) GsonHelper.FromJSon(JSON.toJSONString(obj), FReconciliation.class);
        try {
            //if(StringUtils.isEmpty(fReconciliation.getCheckcurrency())){
            FinOrgDTO finOrgDTO = AccentityUtil.getFinOrgDTOByAccentityId(fReconciliation.getAccbody());
            fReconciliation.setCheckcurrency(finOrgDTO.getCurrency());
            //}
            return createDataDetail(fReconciliation);
        } catch (Exception e) {
            log.error(e.getMessage());
            return returnFail();
        }
    }

    @Override
    public CtmJSONObject getReconciliationDataAll(CtmJSONObject obj) {
        FReconciliation fReconciliation = (FReconciliation) GsonHelper.FromJSon(JSON.toJSONString(obj), FReconciliation.class);
        try {
            if (StringUtils.isEmpty(fReconciliation.getCheckcurrency())) {
                FinOrgDTO finOrgDTO = AccentityUtil.getFinOrgDTOByAccentityId(fReconciliation.getAccbody());
                fReconciliation.setCheckcurrency(finOrgDTO.getCurrency());
            }
            return createDataAll(fReconciliation);
        } catch (Exception e) {
            log.error(e.getMessage());
            return returnFail();
        }
    }

    @Override
    public String getQuerydetailSql(CtmJSONObject params) throws Exception {
        String selectSql = null;//处理前的原始sql
        String resultsql = null;//多数据库翻译后的sql
        //获取擦寻条件
        String whereSql = params.getString("busi_queryschema");
        //获取相关查匈参数
        //对账规则id
        String reconcrule_id = params.getString("reconcrule_id");
        //associate_id	联查事务ID
        String associate_id = params.getString("associate_id");
        String accentity = params.getString("accentity_id");
        //ytenant_id 租户ID
        String ytenant_id = params.getString("ytenant_id");
        String startDate = params.getString("period_start_date");
        String endDate = params.getString("period_end_date");
        //当前对账明细 查看的币种
        String ori_currtype_id = params.getString("ori_currtype_id");
        String reconc_type = (String) params.getJSONArray("reconc_type").get(0);
        List<Short> direction = new ArrayList<>();
        if (IBussinessConstant.CREDIT.equals(reconc_type)) {
            direction.add(Direction.Credit.getValue());
        } else if (IBussinessConstant.DEBIT.equals(reconc_type)) {
            direction.add(Direction.Debit.getValue());
        } else {
            direction.add(Direction.Credit.getValue());
            direction.add(Direction.Debit.getValue());
        }
        String projectCode = null;
        List<LinkedHashMap> reconcproject = params.getObject("reconcproject", List.class);
        if (CollectionUtils.isNotEmpty(reconcproject)) {
            projectCode = (String) reconcproject.get(0).get("project_code");
        }
        // direction原来为['1','2']更改为('1','2')
        String directionString = direction.toString();
        String strLeft = directionString.replace("[", "(");
        String directionStr = strLeft.replace("]", ")");
        // 获取对账账户信息
        Map<String, Object> accountMap = getAccount(whereSql, projectCode, accentity);
        ArrayList<String> accountList = (ArrayList<String>) accountMap.get("accountList");
        if(CollectionUtils.isEmpty(accountList)){
            //添加业务日志
            CtmJSONObject logJsonObject = new CtmJSONObject();
            logJsonObject.put("requestData", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054003B8", "没有对账账户！") /* "没有对账账户！" */);
            businessLogService.saveBusinessLog(logJsonObject, "YCDZ-DETAIL", IMsgConstant.BANKRECONCILIATION, IServicecodeConstant.BANKRECONCILIATION, IMsgConstant.RECONCILIATE, IMsgConstant.RECONCILIATE);
            return "";
        }
        String accountType = (String) accountMap.get(ICmpConstant.ACCOUNTTYPE);
        ArrayList<String> bankAccountId = null;
        ArrayList<String> cashAccountId = null;
        ArrayList<String> accountStrList = new ArrayList();
        for (String account : accountList) {
            String accountStr = "'" + account + "'";
            accountStrList.add(accountStr);
        }
        if (accountType.equals(ICmpConstant.BANK_ACCOUNT)) {
            bankAccountId = accountStrList;
        } else {
            cashAccountId = accountStrList;
        }
        String bankAccount = null;
        String cashAccount = null;
        // 现金账号
        if (CollectionUtils.isNotEmpty(cashAccountId)) {
            String cashAccountStr = cashAccountId.toString();
            String cashLeft = cashAccountStr.replace("[", "(");
            cashAccount = cashLeft.replace("]", ")");
        }
        // 银行账号
        if (CollectionUtils.isNotEmpty(bankAccountId)) {
            String bankAccountStr = bankAccountId.toString();
            String bankLeft = bankAccountStr.replace("[", "(");
            bankAccount = bankLeft.replace("]", ")");
        }
        //拼接基础查询语句
        String groupSql = null;
        String whereCurrySql = null;
        if (accountType.equals(ICmpConstant.BANK_ACCOUNT)) {
            selectSql = "select " + "'" + reconcrule_id + "'" + "as reconcrule_id," + "'" + associate_id + "'" + " as associate_id,cmp_journal.srcbillitemid as bill_id,cmp_journal.vouchdate as busi_date," +
                    " cmp_journal.ytenant_id as ytenant_id,cmp_journal.srcbillno as bill_code," +
                    " cmp_journal.debitoriSum as debit_ori_amount,cmp_journal.debitnatSum as debit_amount,cmp_journal.creditoriSum as credit_ori_amount,cmp_journal.creditnatSum as credit_amount" +
                    " ,ib.billtype_id as bill_type_id,cmp_journal.tradetype as trans_type_id " +
                    " from cmp_journal cmp_journal left join " + ucfbasedoc + ".bd_transtype ib on cmp_journal.tradetype = ib.id " +
                    " where cmp_journal.initflag = 0 and  cmp_journal.ytenant_id=" + "'" + ytenant_id + "'" + " and  " + whereSql + " " + " and cmp_journal.dzdate >= str_to_date(" + "'" + startDate + "','%Y-%m-%d')" + " and cmp_journal.dzdate <= str_to_date(" + "'" + endDate + "','%Y-%m-%d')" +
                    " and cmp_journal.direction in " + directionStr + "and cmp_journal.bankaccount in" + bankAccount + " and cmp_journal.accentity_raw ='" + accentity + "' ";
        } else {
            selectSql = "select " + "'" + reconcrule_id + "'" + "as reconcrule_id," + "'" + associate_id + "'" + " as associate_id,cmp_journal.srcbillitemid as bill_id,cmp_journal.vouchdate as busi_date," +
                    " cmp_journal.ytenant_id as ytenant_id,cmp_journal.srcbillno as bill_code," +
                    " cmp_journal.debitoriSum as debit_ori_amount,cmp_journal.debitnatSum as debit_amount,cmp_journal.creditoriSum as credit_ori_amount,cmp_journal.creditnatSum as credit_amount" +
                    " ,ib.billtype_id as bill_type_id,cmp_journal.tradetype as trans_type_id " +
                    " from cmp_journal cmp_journal left join " + ucfbasedoc + ".bd_transtype ib on cmp_journal.tradetype = ib.id " +
                    " where cmp_journal.initflag = 0 and  cmp_journal.ytenant_id=" + "'" + ytenant_id + "'" + " and  " + whereSql + " " + " and cmp_journal.dzdate >= str_to_date(" + "'" + startDate + "','%Y-%m-%d')" + " and cmp_journal.dzdate <= str_to_date(" + "'" + endDate + "','%Y-%m-%d')" +
                    " and cmp_journal.direction in " + directionStr + "and cmp_journal.cashaccount in" + cashAccount + " and cmp_journal.accentity_raw ='" + accentity + "' ";
        }
        whereCurrySql = " and cmp_journal.currency ='" + ori_currtype_id + "' ";
        groupSql = " group by " + "cmp_journal.srcbillitemid,cmp_journal.vouchdate ," +
                "cmp_journal.ytenant_id,cmp_journal.srcbillno ,cmp_journal.debitoriSum ,cmp_journal.debitnatSum,cmp_journal.creditoriSum,cmp_journal.creditnatSum,ib.billtype_id,cmp_journal.tradetype " +
                "order by cmp_journal.vouchdate asc";
        if (ori_currtype_id != null) {
            selectSql = selectSql + whereCurrySql + groupSql;
        } else {
            selectSql = selectSql + groupSql;
        }
        //适配多数据库处理翻译sql
//        SqlSessionTemplate sqlSessionTemplate = AppContext.getSqlSession();
//        DataSource dataSource = sqlSessionTemplate.getConfiguration().getEnvironment().getDataSource();
//        Connection connection = DataSourceUtils.getConnection(dataSource);
//        resultsql = SqlTranslatorUtil.translate(selectSql,connection, DatabaseUtil.getDbType(connection.getMetaData()),null);

        log.error("selectSql==================" + selectSql);
//        log.error("resultsql==================" + resultsql);
        //添加业务日志
        CtmJSONObject logJsonObject = new CtmJSONObject();
        logJsonObject.put("requestData", selectSql);
        businessLogService.saveBusinessLog(logJsonObject, "YCDZ-DETAIL", IMsgConstant.BANKRECONCILIATION, IServicecodeConstant.BANKRECONCILIATION, IMsgConstant.RECONCILIATE, IMsgConstant.RECONCILIATE);

        return selectSql;
    }


    private List<Map<String, Object>> getData(FReconciliation fReconciliation) throws Exception {
        QueryConditionGroup queryConditionGroup = generateCommonCondition(fReconciliation);
        List<ConditionExpression> conditionExpressions = new ArrayList<>();
        if (fReconciliation.getDimlist() != null && fReconciliation.getDimlist().size() > 0) {
            for (Dimension dimension : fReconciliation.getDimlist()) {
                List<String> dimensionvalue = dimension.getDimensionvalue();
                if (fReconciliation.getChecktype().equals("cmp_bank")) {
                    if (dimensionvalue != null && dimensionvalue.size() > 0) {
                        conditionExpressions.add(QueryCondition.name("bankaccount").in(dimensionvalue));
                    } else {
                        conditionExpressions.add(QueryCondition.name("bankaccount").is_not_null());
                    }
                } else {
                    if (dimensionvalue != null && dimensionvalue.size() > 0) {
                        conditionExpressions.add(QueryCondition.name("cashaccount").in(dimensionvalue));
                    } else {
                        conditionExpressions.add(QueryCondition.name("cashaccount").is_not_null());
                    }
                }
            }
        }
        if (!conditionExpressions.isEmpty()) {
            queryConditionGroup.addCondition(QueryConditionGroup.or(conditionExpressions.toArray(new QueryCondition[0])));
        }
        QuerySchema querySchema = QuerySchema.create().addSelect("srcbillitemid,billnum,srcbillno,debitoriSum,debitnatSum,creditoriSum,creditnatSum,bankaccount,cashaccount,topsrcbillno,topsrcbillid,topsrcitem");
        querySchema.addCondition(queryConditionGroup);
        List<Map<String, Object>> query = MetaDaoHelper.query(Journal.ENTITY_NAME, querySchema);
        return query;
    }

    private CtmJSONObject createDataDetail(FReconciliation fReconciliation) throws Exception {
        CurrencyTenantDTO currencyOrgDTO = baseRefRpcService.queryCurrencyById(fReconciliation.getCheckcurrency());
        CtmJSONObject jsonObject = new CtmJSONObject();
        jsonObject.put("success", "true");
        jsonObject.put("message", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041804F9", "查询成功") /* "查询成功" */);
        CtmJSONArray jsonArray = new CtmJSONArray();
        List<Map<String, Object>> data = getData(fReconciliation);
        for (Map<String, Object> map : data) {
            CtmJSONObject obj = new CtmJSONObject();
            obj.put("billid", map.get("srcbillitemid"));
            if (map.get("billnum") == null) {
                obj.put("billcode", map.get("srcbillno"));
            } else {
                obj.put("billcode", map.get("billnum"));
            }
            //业财对账，费用、应收应付生成银行存款科目时，添加传输字段
            if (map.get("topsrcitem") != null) {
                String topsrcitem = map.get("topsrcitem").toString();
                if (!String.valueOf(EventSource.Cmpchase.getValue()).equals(topsrcitem) && !String.valueOf(EventSource.Drftchase.getValue()).equals(topsrcitem)) {
                    if (map.get("topsrcbillid") != null) {
                        obj.put("topsrcbillid", map.get("topsrcbillid"));
                    }
                    if (map.get("topsrcbillno") != null) {
                        obj.put("topsrcbillno", map.get("topsrcbillno"));
                    }
                }
            }
            List<Dimension> dimensions = new ArrayList<>();
            Dimension dimension = new Dimension();
            dimensions.add(dimension);
            List<String> strings = new ArrayList<>();
            if (fReconciliation.getChecktype().equals("cmp_bank")) {
                dimension.setDimensiondoc("finorgbankacct");
                strings.add(map.get("bankaccount").toString());
                dimension.setDimensionvalue(strings);
                obj.put("dimlist", dimensions);
            } else {
                dimension.setDimensiondoc("org_fin_cashacct");
                strings.add(map.get("cashaccount").toString());
                dimension.setDimensionvalue(strings);
                obj.put("dimlist", dimensions);
            }
            obj.put("local_debit", BigDecimalUtils.safeSubtract((BigDecimal) map.get("debitnatSum"), (BigDecimal) map.get("creditnatSum")).setScale(currencyOrgDTO.getMoneydigit(), RoundingMode.HALF_UP));
            jsonArray.add(obj);
        }
        jsonObject.put("data", jsonArray);
        return jsonObject;
    }

    private CtmJSONObject createDataAll(FReconciliation fReconciliation) throws Exception {
        CurrencyTenantDTO currencyOrgDTO = baseRefRpcService.queryCurrencyById(fReconciliation.getCheckcurrency());
        CtmJSONObject jsonObject = new CtmJSONObject();
        jsonObject.put("success", "true");
        jsonObject.put("message", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041804F9", "查询成功") /* "查询成功" */);
        BigDecimal local_debit = BigDecimal.ZERO;
        BigDecimal local_credit = BigDecimal.ZERO;
        BigDecimal original_debit = BigDecimal.ZERO;
        BigDecimal original_credit = BigDecimal.ZERO;

        List<Map<String, Object>> data = getData(fReconciliation);
        for (Map<String, Object> map : data) {
            local_debit = BigDecimalUtils.safeAdd(local_debit, (BigDecimal) map.get("debitnatSum"));
            local_credit = BigDecimalUtils.safeAdd(local_credit, (BigDecimal) map.get("creditnatSum"));
            original_debit = BigDecimalUtils.safeAdd(original_debit, (BigDecimal) map.get("debitoriSum"));
            original_credit = BigDecimalUtils.safeAdd(original_credit, (BigDecimal) map.get("creditoriSum"));
        }

        CtmJSONObject dataObj = new CtmJSONObject();
        dataObj.put("local_debit", BigDecimalUtils.safeSubtract(local_debit, local_credit).setScale(currencyOrgDTO.getMoneydigit(), RoundingMode.HALF_UP));
        /*dataObj.put("local_credit",local_credit.setScale(currencyOrgDTO.getMoneydigit(), RoundingMode.HALF_UP));
        dataObj.put("original_debit",original_debit.setScale(currencyOrgDTO.getMoneydigit(), RoundingMode.HALF_UP));
        dataObj.put("original_credit",original_credit.setScale(currencyOrgDTO.getMoneydigit(), RoundingMode.HALF_UP));*/
        jsonObject.put("data", dataObj);
        return jsonObject;
    }


    protected QueryConditionGroup generateCommonCondition(FReconciliation fReconciliation) throws Exception {
        QueryConditionGroup group = new QueryConditionGroup(ConditionOperator.and);
        //创建通用条件
//        if (!StringUtils.isEmpty(fReconciliation.getAccbook())){
//            group.addCondition(QueryCondition.name("busiaccbook").eq(fReconciliation.getAccbook()));
//        }
        if (!StringUtils.isEmpty(fReconciliation.getAccbody())) {
            group.addCondition(QueryCondition.name(IBussinessConstant.ACCENTITY).eq(fReconciliation.getAccbody()));
        }
        /*if (StringUtils.isEmpty(fReconciliation.getCheckcurrency())){
            List<Map<String, Object>> stringObjectMap = QueryBaseDocUtils.queryAccRawEntityByAccEntityId(fReconciliation.getAccbody());
            group.addCondition(QueryCondition.name("currency").eq(stringObjectMap.get(0).get("currency")));
        }else {
            group.addCondition(QueryCondition.name("currency").eq(fReconciliation.getCheckcurrency()));
        }*/
        //group.addCondition(QueryCondition.name("srcitem").eq(8));
        // 按照需求的要求修改，业财对账只返回已结算数据
        group.addCondition(QueryCondition.name("settlestatus").eq(2));
        group.addCondition(QueryCondition.name("initflag").eq(0));
        if ("settleDate".equals(fReconciliation.getBillDataType())) {
            group.addCondition(QueryCondition.name("dzdate").between(fReconciliation.getStartdate(), fReconciliation.getEnddate()));
        } else {
            group.addCondition(QueryCondition.name("vouchdate").between(fReconciliation.getStartdate(), fReconciliation.getEnddate()));
        }
        return group;
    }

    private CtmJSONObject returnFail() {
        CtmJSONObject jsonObject = new CtmJSONObject();
        jsonObject.put("success", "false");
        jsonObject.put("message", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041804F8", "查询失败") /* "查询失败" */);
        jsonObject.put("data", null);
        return jsonObject;
    }

    @Override
    public ReconcDetailResultDTO queryDetailSql(ExecutorDetailReportDTO reportDTO) {
        String selectSql = null;//处理前的原始sql
        String resultsql = null;//多数据库翻译后的sql
        ReconcDetailResultDTO resultDTO;
        try {
            //获取擦寻条件
            String whereSql = reportDTO.getBusiQueryschema();
            // 项目名称
            List<ReconcProjectDTO> reconcProjectDto = reportDTO.getReconcProjectDto();
            String projectCode = reconcProjectDto.get(0).getProjectCode();
            //获取相关查匈参数
            //对账规则id
            String reconcrule_id = reportDTO.getReconcruleId();
            //associate_id	联查事务ID
            String associate_id = reportDTO.getAssociateId();
            String accentity = reportDTO.getAccentityId();
            //ytenant_id 租户ID
            String ytenant_id = reportDTO.getYtenantId();
            String startDate = reportDTO.getPeriodStartDate();
            String endDate = reportDTO.getPeriodEndDate();
            //当前对账明细 查看的币种
            String ori_currtype_id = reportDTO.getOriCurrencyId();
            List<String> reconcType = reportDTO.getReconcType();
            String reconc_type = reconcType.get(0);
            List<Short> direction = new ArrayList<>();
            if (IBussinessConstant.CREDIT.equals(reconc_type)) {
                direction.add(Direction.Credit.getValue());
            } else if (IBussinessConstant.DEBIT.equals(reconc_type)) {
                direction.add(Direction.Debit.getValue());
            } else {
                direction.add(Direction.Credit.getValue());
                direction.add(Direction.Debit.getValue());
            }
            // direction原来为['1','2']更改为('1','2')
            String directionString = direction.toString();
            String strLeft = directionString.replace("[", "(");
            String directionStr = strLeft.replace("]", ")");

            Map<String, Object> accountMap = getAccount(whereSql, projectCode, accentity);
            ArrayList<String> accountList = (ArrayList<String>) accountMap.get("accountList");
            if(CollectionUtils.isEmpty(accountList)){
                CtmJSONObject logJsonObject = new CtmJSONObject();
                logJsonObject.put("requestData", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054003B6", "对账账户为空！") /* "对账账户为空！" */);
                businessLogService.saveBusinessLog(logJsonObject, "YCDZ-DETAIL", IMsgConstant.BANKRECONCILIATION, IServicecodeConstant.BANKRECONCILIATION, IMsgConstant.RECONCILIATE, IMsgConstant.RECONCILIATE);
                resultDTO = new ReconcDetailResultDTO(Boolean.FALSE, "ctmcmp", "", null);
                resultDTO.setMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054003B6", "对账账户为空！") /* "对账账户为空！" */);
                return resultDTO;
            }
            String accountType = (String) accountMap.get(ICmpConstant.ACCOUNTTYPE);
            ArrayList<String> bankAccountId = null;
            ArrayList<String> cashAccountId = null;
            ArrayList<String> accountStrList = new ArrayList();
            for (String account : accountList) {
                String accountStr = "'" + account + "'";
                accountStrList.add(accountStr);
            }
            if (accountType.equals(ICmpConstant.BANK_ACCOUNT)) {
                bankAccountId = accountStrList;
            } else {
                cashAccountId = accountStrList;
            }
            String bankAccount = null;
            String cashAccount = null;
            // 现金账号
            if (CollectionUtils.isNotEmpty(cashAccountId)) {
                String cashAccountStr = cashAccountId.toString();
                String cashLeft = cashAccountStr.replace("[", "(");
                cashAccount = cashLeft.replace("]", ")");
            }
            // 银行账号
            if (CollectionUtils.isNotEmpty(bankAccountId)) {
                String bankAccountStr = bankAccountId.toString();
                String bankLeft = bankAccountStr.replace("[", "(");
                bankAccount = bankLeft.replace("]", ")");
            }

            //拼接基础查询语句
            String groupSql = null;
            String whereCurrySql = null;
            if (accountType.equals(ICmpConstant.BANK_ACCOUNT)) {
                selectSql = "select " + "'" + reconcrule_id + "'" + "as reconcrule_id," + "'" + associate_id + "'" + " as associate_id, case cmp_journal.billtype when 23 then cmp_journal.srcbillitemno else cmp_journal.srcbillitemid end as bill_id,cmp_journal.vouchdate as busi_date," +
                        " cmp_journal.ytenant_id as ytenant_id,cmp_journal.srcbillno as bill_code," +
                        " cmp_journal.debitoriSum as debit_ori_amount,cmp_journal.debitnatSum as debit_amount,cmp_journal.creditoriSum as credit_ori_amount,cmp_journal.creditnatSum as credit_amount" +
                        " ,ib.billtype_id as bill_type_id,cmp_journal.tradetype as trans_type_id " +
                        " from cmp_journal cmp_journal left join " + ucfbasedoc + ".bd_transtype ib on cmp_journal.tradetype = ib.id " +
                        " where cmp_journal.initflag = 0 and  cmp_journal.ytenant_id=" + "'" + ytenant_id + "'" + " and  " + whereSql + " " + " and cmp_journal.dzdate >= str_to_date(" + "'" + startDate + "','%Y-%m-%d')" + " and cmp_journal.dzdate <= str_to_date(" + "'" + endDate + "','%Y-%m-%d')" +
                        " and cmp_journal.direction in " + directionStr + "and cmp_journal.bankaccount in" + bankAccount + " and cmp_journal.accentity_raw ='" + accentity + "' ";
            } else {
                selectSql = "select " + "'" + reconcrule_id + "'" + "as reconcrule_id," + "'" + associate_id + "'" + " as associate_id,case cmp_journal.billtype when 23 then cmp_journal.srcbillitemno else cmp_journal.srcbillitemid end as bill_id,cmp_journal.vouchdate as busi_date," +
                        " cmp_journal.ytenant_id as ytenant_id,cmp_journal.srcbillno as bill_code," +
                        " cmp_journal.debitoriSum as debit_ori_amount,cmp_journal.debitnatSum as debit_amount,cmp_journal.creditoriSum as credit_ori_amount,cmp_journal.creditnatSum as credit_amount" +
                        " ,ib.billtype_id as bill_type_id,cmp_journal.tradetype as trans_type_id " +
                        " from cmp_journal cmp_journal left join " + ucfbasedoc + ".bd_transtype ib on cmp_journal.tradetype = ib.id " +
                        " where cmp_journal.initflag = 0 and  cmp_journal.ytenant_id=" + "'" + ytenant_id + "'" + " and  " + whereSql + " " + " and cmp_journal.dzdate >= str_to_date(" + "'" + startDate + "','%Y-%m-%d')" + " and cmp_journal.dzdate <= str_to_date(" + "'" + endDate + "','%Y-%m-%d')" +
                        " and cmp_journal.direction in " + directionStr + "and cmp_journal.cashaccount in" + cashAccount + " and cmp_journal.accentity_raw ='" + accentity + "' ";
            }
            whereCurrySql = " and cmp_journal.currency ='" + ori_currtype_id + "' ";
            groupSql = " group by " + "bill_id,cmp_journal.vouchdate ," +
                    "cmp_journal.ytenant_id,cmp_journal.srcbillno ,cmp_journal.debitoriSum ,cmp_journal.debitnatSum,cmp_journal.creditoriSum,cmp_journal.creditnatSum,ib.billtype_id,cmp_journal.tradetype " +
                    "order by cmp_journal.vouchdate asc";
            if (ori_currtype_id != null) {
                selectSql = selectSql + whereCurrySql + groupSql;
            } else {
                selectSql = selectSql + groupSql;
            }
            //适配多数据库处理翻译sql
//            SqlSessionTemplate sqlSessionTemplate = AppContext.getSqlSession();
//            DataSource dataSource = sqlSessionTemplate.getConfiguration().getEnvironment().getDataSource();
//            Connection connection = DataSourceUtils.getConnection(dataSource);
//            resultsql = SqlTranslatorUtil.translate(selectSql,connection, DatabaseUtil.getDbType(connection.getMetaData()),null);

            log.error("selectSql==================" + selectSql);
//            log.error("resultsql==================" + resultsql);
            resultDTO = new ReconcDetailResultDTO(Boolean.TRUE, "ctmcmp", selectSql, null);
            resultDTO.setMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054003B7", "查询成功") /* "查询成功" */);
        } catch (Exception e) {
            resultDTO = new ReconcDetailResultDTO(Boolean.FALSE, "ctmcmp", selectSql, null);
            resultDTO.setMessage(e.getMessage());
        }
        CtmJSONObject logJsonObject = new CtmJSONObject();
        logJsonObject.put("requestData", selectSql);
        businessLogService.saveBusinessLog(logJsonObject, "YCDZ-DETAIL", IMsgConstant.BANKRECONCILIATION, IServicecodeConstant.BANKRECONCILIATION, IMsgConstant.RECONCILIATE, IMsgConstant.RECONCILIATE);
        return resultDTO;
    }

    @Override
    public Map<String, Object> getAccount(String busiQuery, String projectCode, String accentity) throws Exception {
        String[] accountStr = busiQuery.split(" ");
        Map<String, Object> accountMap = new HashMap<>();
        if (accountStr.length > 2) {
            String accountStr1 = accountStr[accountStr.length - 1];
            String[] accountStr3 = accountStr1.split(",");
            ArrayList<String> accountList = new ArrayList<>();
            ArrayList<String> accountList2 = new ArrayList<>();
            for (String s1 : accountStr3) {
                accountList.add(s1);
            }
            String accounts = accountList.toString().replace("'", "").replace("[", "").replace("]", "");
            if (ICmpConstant.BANKPROJECTCODE.equals(projectCode)) {
                accountMap.put(ICmpConstant.ACCOUNTTYPE, ICmpConstant.BANKACCOUNT);
            } else {
                accountMap.put(ICmpConstant.ACCOUNTTYPE, ICmpConstant.CASH_ACCOUNT_LOWER);
            }
            String[] accountStr2 = accounts.split(",");
            for (String account : accountStr2) {
                account = account.replace("(","").trim();
                account = account.replace(")","").trim();
                accountList2.add(account);
            }
            accountMap.put("accountList", accountList2);
            return accountMap;
        } else {
            //获取全部对账账户
            QuerySchema querySchema = QuerySchema.create().addSelect("accentityRaw,accentity");
            QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("accentityRaw").eq(accentity));
            querySchema.addCondition(group);
            List<Journal> journalList = MetaDaoHelper.queryObject(Journal.ENTITY_NAME, querySchema, null);
            List<String> accentityList = journalList.stream().map(Journal::getAccentity).collect(Collectors.toList());
            List<String> accentitys = accentityList.stream().distinct().collect(Collectors.toList());
            List<String> accountList = new ArrayList<>();
            //银行对账
            if (ICmpConstant.BANKPROJECTCODE.equals(projectCode)) {
                EnterpriseParams enterpriseParams = new EnterpriseParams();
                enterpriseParams.setOrgidList(accentitys);
                List<EnterpriseBankAcctVOWithRange> enterpriseBankAcctVOS = enterpriseBankQueryService.queryAllWithRange(enterpriseParams);
                accountList = enterpriseBankAcctVOS.stream().map(EnterpriseBankAcctVO::getId).collect(Collectors.toList());
                accountMap.put(ICmpConstant.ACCOUNTTYPE, ICmpConstant.BANKACCOUNT);
            } else {
                //现金对账
                EnterpriseParams enterpriseParams = new EnterpriseParams();
                enterpriseParams.setPageSize(4999);
                enterpriseParams.setOrgidList(accentitys);
                List<EnterpriseCashVO> cashAcctVOS = baseRefRpcService.queryEnterpriseCashAcctByCondition(enterpriseParams);
                accountList = cashAcctVOS.stream().map(EnterpriseCashVO::getId).collect(Collectors.toList());
                accountMap.put(ICmpConstant.ACCOUNTTYPE, ICmpConstant.CASH_ACCOUNT_LOWER);
            }
            //弥补没有发生额但是有账户期初的场景
            List<InitData> initList = null;
            if (ICmpConstant.BANKPROJECTCODE.equals(projectCode)) {
                QuerySchema schema = QuerySchema.create().addSelect("bankaccount");
                QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
                conditionGroup.appendCondition(QueryCondition.name("accentityRaw").eq(accentity));//所属组织
                conditionGroup.appendCondition(QueryCondition.name("upgradesign").in(UpgradeSignEnum.JUDGMENT.getValue(),UpgradeSignEnum.ADDNEW.getValue()));
                schema.addCondition(conditionGroup);
                initList = MetaDaoHelper.queryObject(InitData.ENTITY_NAME, schema, null);
                if(CollectionUtils.isNotEmpty(initList)){
                    for (InitData initData : initList){
                        accountList.add(initData.getBankaccount());
                    }
                }
            } else {
                QuerySchema schema = QuerySchema.create().addSelect("cashaccount");
                QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
                conditionGroup.appendCondition(QueryCondition.name("accentityRaw").eq(accentity));//所属组织
                schema.addCondition(conditionGroup);
                initList = MetaDaoHelper.queryObject(InitData.ENTITY_NAME, schema, null);
                if(CollectionUtils.isNotEmpty(initList)){
                    for (InitData initData : initList){
                        accountList.add(initData.getCashaccount());
                    }
                }
            }
            Set<String> accountSet = new HashSet<>(accountList);
            List<String> distinctAccountList = new ArrayList<>(accountSet);
            accountMap.put("accountList", distinctAccountList);
        }
        return accountMap;
    }
}
