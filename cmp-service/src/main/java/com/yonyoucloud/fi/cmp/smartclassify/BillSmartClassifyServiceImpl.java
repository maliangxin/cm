package com.yonyoucloud.fi.cmp.smartclassify;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.yonyou.iuap.yms.api.IYmsJdbcApi;
import com.yonyou.iuap.yms.param.SQLParameter;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.json.JSONObject;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankreconciliationPullService;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankreconciliationService;
import com.yonyoucloud.fi.cmp.cmpentity.OppositeType;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.IStwbConstantForCmp;
import com.yonyoucloud.fi.cmp.constant.MerchantConstant;
import com.yonyoucloud.fi.cmp.enums.BankReconciliationActions;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.exception.BankDealDetailException;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.exception.BankDealDetailExceptionCodeEnum;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.Direction;
import com.yonyoucloud.fi.cmp.util.DateUtils;
import com.yonyoucloud.fi.cmp.util.QueryBaseDocUtils;
import com.yonyoucloud.fi.cmp.util.TaskUtils;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import com.yonyoucloud.fi.cmp.util.basedoc.VendorQueryService;
import com.yonyoucloud.fi.cmp.util.threadpool.ThreadPoolBuilder;
import com.yonyoucloud.fi.cmp.util.threadpool.ThreadPoolUtil;
import com.yonyoucloud.iuap.upc.dto.MerchantDTO;
import com.yonyoucloud.upc.pub.api.vendor.vo.vendor.VendorVO;
import cn.hutool.core.thread.BlockPolicy;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QueryOrderby;
import org.imeta.orm.schema.QuerySchema;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutorService;

/**
 * @description: 单据智能分类具体接口
 * @author: wanxbo@yonyou.com
 * @date: 2022/7/18 15:04
 */
@Slf4j
@Service
public class BillSmartClassifyServiceImpl implements BillSmartClassifyService {
    protected IYmsJdbcApi ymsJdbcApi;
    @Resource(name = "busiBaseDAO")
    public void setYmsJdbcApi(IYmsJdbcApi ymsJdbcApi) {
        this.ymsJdbcApi = ymsJdbcApi;
    }

    @Autowired
    BankreconciliationService bankreconciliationService;

    static ExecutorService executorService = null;
    static {
        executorService = ThreadPoolBuilder.ioThreadPoolBuilder()
                .setThreadNamePrefix("cmp-autoClassify-async-")
                .setQueueSize(100)
                .setDaemon(false)
                .setMaximumPoolSize(100)
                .setRejectHandler(new BlockPolicy())
                .builder(Integer.parseInt("300"), Integer.parseInt("100"));
    }

    @Resource
    private BankreconciliationPullService bankreconciliationPullService;

    @Resource
    private VendorQueryService vendorQueryService;


    private static final Cache<String, BillSmartClassifyBO> periodCache = CacheBuilder.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(60))
            .concurrencyLevel(4)
            .maximumSize(1000)
            .softValues()
            .build();

    /**
     * 根据会计主体，对方账号，币种进行分类
     *
     * @param accentity     会计主体ID
     * @param toaccountno   对方账号
     * @param toaccountname 对方户名
     * @param currency      币种ID
     * @return 分类结果
     */
    @Override
    public BillSmartClassifyBO smartClassify(String accentity, String toaccountno, String toaccountname, String currency, short flag) throws Exception {
        log.error("BillSmartClassifyService#smartClassify parma, accentity:{}, toaccountno:{}, toaccountname:{}, currency:{}, flag:{}", accentity, toaccountno, toaccountname, currency, flag);
        String key = String.format("%s_%s_%s_%s_%s", accentity, toaccountno, toaccountname, currency, flag);
        log.error("BillSmartClassifyService#smartClassify parma, key:{}", key);
        BillSmartClassifyBO bo = periodCache.getIfPresent(key);
        if (bo != null) {
            log.error("BillSmartClassifyService#smartClassify bo, key:{},bo:{}", key, JSONObject.toJSONString(bo));
            return bo;
        }
        // 支出辨识顺序：内部单位->供应商->员工->客户
        if (flag == Direction.Debit.getValue()) {
            // 内部单位
            BillSmartClassifyBO classifyInnerOrgBO = classifyInnerOrg(toaccountno);
            if (classifyInnerOrgBO != null) {
                periodCache.put(key, classifyInnerOrgBO);
                return classifyInnerOrgBO;
            }

            // 供应商
            BillSmartClassifyBO classifySupplierBO = classifySupplier(accentity, toaccountno, toaccountname);
            if (classifySupplierBO != null) {
                periodCache.put(key, classifySupplierBO);
                return classifySupplierBO;
            }


            // 员工
            BillSmartClassifyBO classifyEmployeeBO = classifyInnerEmployee(toaccountno, accentity, toaccountname);
            if (classifyEmployeeBO != null) {
                periodCache.put(key, classifyEmployeeBO);
                return classifyEmployeeBO;
            }

            // 客户
            BillSmartClassifyBO classifyCustomerBO = classifyCustomer(accentity, toaccountno, toaccountname);
            if (classifyCustomerBO != null) {
                periodCache.put(key, classifyCustomerBO);
                return classifyCustomerBO;
            }

        } else {
            // 收入 内部单位->客户->员工->供应商
            // 内部单位
            BillSmartClassifyBO classifyInnerOrgBO = classifyInnerOrg(toaccountno);
            if (classifyInnerOrgBO != null) {
                periodCache.put(key, classifyInnerOrgBO);
                return classifyInnerOrgBO;
            }

            // 客户
            BillSmartClassifyBO classifyCustomerBO = classifyCustomer(accentity, toaccountno, toaccountname);
            if (classifyCustomerBO != null) {
                periodCache.put(key, classifyCustomerBO);
                return classifyCustomerBO;
            }

            // 员工
            BillSmartClassifyBO classifyEmployeeBO = classifyInnerEmployee(toaccountno, accentity, toaccountname);
            if (classifyEmployeeBO != null) {
                periodCache.put(key, classifyEmployeeBO);
                return classifyEmployeeBO;
            }

            // 供应商
            BillSmartClassifyBO classifySupplierBO = classifySupplier(accentity, toaccountno, toaccountname);
            if (classifySupplierBO != null) {
                periodCache.put(key, classifySupplierBO);
                return classifySupplierBO;
            }
        }

        return null;
    }

    /**
     * 客户辨识
     * @param accentity 资金组织
     * @param toaccountno 对方银行账号
     * @param toaccountname 对方账户名称
     * @return
     */
    private BillSmartClassifyBO classifyCustomer(String accentity, String toaccountno, String toaccountname) throws Exception {
        CtmJSONObject customerJson = getCustomer(accentity, toaccountno, toaccountname);
        if (Objects.equals(MerchantConstant.TRUE, customerJson.getString(MerchantConstant.CUSTOMERFLAG))) {
            return new BillSmartClassifyBO(OppositeType.Customer.getValue(), customerJson.getString(MerchantConstant.CUSTOMERID),
            customerJson.get(ICmpConstant.NAME) != null ? customerJson.get(ICmpConstant.NAME).toString() : null, customerJson.getString(MerchantConstant.CUSTOMERBANKID));
            // 匹配到多个时，按照【其他】处理，不再进行匹配
        } else if (Objects.equals(MerchantConstant.TRUE, customerJson.getString(MerchantConstant.OTHERFLAG))) {
            return new BillSmartClassifyBO(OppositeType.Other.getValue(), null, null);
        } else {
            //大北农需求（2611001828SR24091904173），设置yms 参数，如果是精准匹配=true，根据账号没有匹配到供应商或者客户信息，不用拿名称再次匹配匹配
            boolean isAccuratematching = Boolean.parseBoolean(AppContext.getEnvConfig("cmp.bankreconciliation.isAccuratematching","false"));
            if (StringUtils.isNotEmpty(toaccountname) && !isAccuratematching) {
                ArrayList<String> orgList = new ArrayList<String>();
                orgList.add(accentity);
                orgList.add(IStwbConstantForCmp.GLOBAL_ACCENTITY);
                List<MerchantDTO> customerMap = QueryBaseDocUtils.queryMerchantByNameAndOrg(toaccountname, orgList);
                if (CollectionUtils.isNotEmpty(customerMap)) {
                    for (MerchantDTO merchantDTO : customerMap) {
                        // 停用状态null 或者 停用状态为true
                        if (merchantDTO == null || null == merchantDTO.getDetailStopStatus() || merchantDTO.getDetailStopStatus()) {
                            continue;
                        }
                        return new BillSmartClassifyBO(OppositeType.Customer.getValue(), merchantDTO.getId().toString(), merchantDTO.getName());
                    }
                }
            }
        }
        return null;
    }

    /**
     * 员工辨识
     *
     * @param toaccountno   对方银行账号
     * @param accentity     资金组织
     * @param toaccountname
     * @return
     */
    private BillSmartClassifyBO classifyInnerEmployee(String toaccountno, String accentity, String toaccountname) throws Exception {
        Map<String, Object> employee = bankreconciliationPullService.getInnerEmployee(toaccountno, accentity);
        if (ValueUtils.isEmpty(employee)) {
            employee = bankreconciliationPullService.getInnerEmployeeByName(toaccountname, accentity);
        }
        if (ValueUtils.isEmpty(employee)) {
            return null;
        }

        return new BillSmartClassifyBO(OppositeType.Employee.getValue(), employee.get("id") != null ? employee.get("id").toString() : null,
                employee.get("name") != null ? employee.get("name").toString() : null, employee.get("staffBankAccount") != null ? employee.get("staffBankAccount").toString() : null);
    }

    /**
     * 供应商辨识
     * @param accentity 资金组织
     * @param toaccountno 对方银行账号
     * @param toaccountname 对方账户名称
     * @return BillSmartClassifyBO
     */
    private BillSmartClassifyBO classifySupplier(String accentity, String toaccountno, String toaccountname) throws Exception {
        //供应商，包含核算委托关系相关
        CtmJSONObject supplierJson = getSupplier(accentity, toaccountno, toaccountname);
        if (Objects.equals(MerchantConstant.TRUE, supplierJson.getString(MerchantConstant.VENDORFLAG))) {
            return new BillSmartClassifyBO(OppositeType.Supplier.getValue(), supplierJson.getString("supplierId"), supplierJson.get(ICmpConstant.NAME).toString()
                    , supplierJson.get(MerchantConstant.SUPPLIERBANKID) != null ? supplierJson.getString(MerchantConstant.SUPPLIERBANKID) : null);
        // 匹配到多个时，按照【其他】处理，不再进行匹配
        } else if (Objects.equals(MerchantConstant.TRUE, supplierJson.getString(MerchantConstant.OTHERFLAG))) {
            return new BillSmartClassifyBO(OppositeType.Other.getValue(), null, null);
        } else {
            //大北农需求（2611001828SR24091904173），设置yms 参数，如果是精准匹配=true，根据账号没有匹配到供应商或者客户信息，不用拿名称再次匹配匹配
            boolean isAccuratematching = Boolean.parseBoolean(AppContext.getEnvConfig("cmp.bankreconciliation.isAccuratematching","false"));
            if (toaccountname != null && !isAccuratematching) {
                // 接口返回启用的档案数据
                List<VendorVO> vendorFieldByName = vendorQueryService.getVendorFieldByName(toaccountname);
                if (CollectionUtils.isNotEmpty(vendorFieldByName)) {
                    if (StringUtils.isNotEmpty(accentity) && (vendorQueryService.judgeVendorOrg(vendorFieldByName.get(0).getId(), accentity)
                            || vendorQueryService.judgeVendorOrg(vendorFieldByName.get(0).getId(), IStwbConstantForCmp.GLOBAL_ACCENTITY))) {
                        return new BillSmartClassifyBO(OppositeType.Supplier.getValue(), vendorFieldByName.get(0).getId().toString(),
                                vendorFieldByName.get(0).getName());
                    } else if (StringUtils.isEmpty(accentity)) {
                        return new BillSmartClassifyBO(OppositeType.Supplier.getValue(), vendorFieldByName.get(0).getId().toString(),
                                vendorFieldByName.get(0).getName());
                    }
                }
            }
        }
        return null;
    }

    /**
     * 内部单位辨识
     * @param toaccountno 对方银行账号
     * @return
     * @throws Exception
     */
    private BillSmartClassifyBO classifyInnerOrg(String toaccountno) throws Exception {
        //内部单位
        Map<String, Object> bankAccount = bankreconciliationPullService.getBankAcctByAccount(toaccountno);
        if (ValueUtils.isEmpty(bankAccount)) {
            return null;
        }
        return new BillSmartClassifyBO(OppositeType.InnerOrg.getValue(), bankAccount.get("orgid") != null ? bankAccount.get("orgid").toString() : null,
                bankAccount.get("orgname") != null ? bankAccount.get("orgname").toString() : null, bankAccount.get("bankacct") != null ? bankAccount.get("bankacct").toString() : null);
    }



    /**
     * 获取供应商
     * 1 根据银行账号和账户名称查询供应商
     * 2 根据银行账号查询供应商
     * 3 根据账户名称查询供应商
     *
     * @param accentity     会计主体
     * @param toaccountno   银行账号
     * @param toaccountname 账户名称
     * @return
     */
    private CtmJSONObject getSupplier(String accentity, String toaccountno, String toaccountname) throws Exception {
        CtmJSONObject supplierJson = bankreconciliationPullService.getSupplier(accentity, toaccountno, toaccountname);
        Boolean isAccuratematching = Boolean.parseBoolean(AppContext.getEnvConfig("cmp.bankreconciliation.isAccuratematching","false"));
        if (Objects.equals(MerchantConstant.FALSE, supplierJson.getString(MerchantConstant.VENDORFLAG))) {
            //大北农需求（2611001828SR24091904173），设置yms 参数，如果是精准匹配=true，根据账号+名称没有匹配到供应商或者客户信息，不用拿账号、名称再次匹配匹配
            if (isAccuratematching) {
                return supplierJson;
            }
            supplierJson = bankreconciliationPullService.getSupplier(accentity, toaccountno);
        }
        if (Objects.equals(MerchantConstant.FALSE, supplierJson.getString(MerchantConstant.VENDORFLAG)) && !isAccuratematching) {
            supplierJson = bankreconciliationPullService.getSupplierByAccName(accentity, toaccountname);
        }
        return supplierJson;
    }

    /**
     * 获取客户
     * 1 按照银行账号和账户名称查询
     * 2 按照银行账号查询
     * 3 按照账户名称查询
     *
     * @param accentity     会计主体
     * @param toaccountno   银行账号
     * @param toaccountname 账户名称
     * @return
     * @throws Exception
     */
    private CtmJSONObject getCustomer(String accentity, String toaccountno, String toaccountname) throws Exception {
        CtmJSONObject customerJson = bankreconciliationPullService.getCustomer(accentity, toaccountno, toaccountname);
        //大北农需求（20251223），设置yms 参数，如果是精准匹配=true，根据账号+名称没有匹配到供应商或者客户信息，不用拿账号、名称再次匹配匹配
        Boolean isAccuratematching = Boolean.parseBoolean(AppContext.getEnvConfig("cmp.bankreconciliation.isAccuratematching","false"));
        log.error("BillSmartClassifyService#smartClassify getCustomer parma, accentity:{}, toaccountno:{}, toaccountname:{}", accentity, toaccountno, toaccountname);
        if (Objects.equals(MerchantConstant.FALSE, customerJson.getString(MerchantConstant.CUSTOMERFLAG))) {
            if (isAccuratematching) {
                log.error("BillSmartClassifyService#smartClassify getCustomer parma, accentity:{}, toaccountname:{}", accentity, toaccountname);
                return customerJson;
            }
            customerJson = bankreconciliationPullService.getCustomer(accentity, toaccountno);
            log.error("BillSmartClassifyService#smartClassify getCustomer parma, accentity:{}, toaccountno:{} ", accentity, toaccountno);
        }
        if (Objects.equals(MerchantConstant.FALSE, customerJson.getString(MerchantConstant.CUSTOMERFLAG)) && !isAccuratematching) {
            customerJson = bankreconciliationPullService.getCustomerByName(accentity, toaccountname);
            log.error("BillSmartClassifyService#smartClassify getCustomer parma, accentity:{}, toaccountname:{}", accentity, toaccountname);
        }
        return customerJson;
    }

    @Override
    public boolean autoClassify(JsonNode params) throws Exception {
        JsonNode dataRangeNode = params.get("dataRange");
        JsonNode startdateNode = params.get("startdate");
        JsonNode enddateNode = params.get("enddate");

        String dataRangeText = dataRangeNode != null ? dataRangeNode.asText() : null;
        String startdateText = startdateNode != null ? startdateNode.asText() : null;
        String enddateText = enddateNode != null ? enddateNode.asText() : null;

        boolean hasDataRange = dataRangeNode != null && StringUtils.isNotEmpty(dataRangeText);
        boolean hasStartdate = startdateNode != null && StringUtils.isNotEmpty(startdateText);
        boolean hasEnddate = enddateNode != null && StringUtils.isNotEmpty(enddateText);

        if (hasDataRange && (hasStartdate || hasEnddate)) {
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400761", "开始日期、结束日期不允许与日期范围同时有值") /* "开始日期、结束日期不允许与日期范围同时有值" */);
        }

        List<BankReconciliation> bankReconciliationList = getBankReconciliationList(params);

        ThreadPoolUtil.executeByBatch(executorService, bankReconciliationList, 5, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400572", "银行对账单自动辨识") /* "银行对账单自动辨识" */, (int fromIndex, int toIndex) -> {
            String builder = "";
            List<BankReconciliation> bankReconciliationBatch = new ArrayList<>();
            for (int t = fromIndex; t < toIndex; t++) {
                try {
                BankReconciliation bizObject = bankReconciliationList.get(t);
                BankReconciliation updateBankReconciliation = new BankReconciliation();
                updateBankReconciliation.setId(bizObject.getId());
                if (bizObject.getTo_acct_no() == null && bizObject.getTo_acct_name() == null) {
                    updateBankReconciliation.setOppositetype(OppositeType.Other.getValue());
                    updateBankReconciliation.setOppositeobjectid(null);
                    updateBankReconciliation.setOppositeobjectname(null);
                    bankReconciliationBatch.add(updateBankReconciliation);
                    continue;
                }
                BillSmartClassifyBO classifyBO = this.smartClassify(
                        bizObject.getAccentity(), bizObject.getTo_acct_no(), bizObject.getTo_acct_name(), bizObject.getCurrency(), bizObject.getDc_flag().getValue());
                if (classifyBO != null) {
                    updateBankReconciliation.setOppositetype(classifyBO.getOppositetype());
                    updateBankReconciliation.setOppositeobjectid(classifyBO.getOppositeobjectid() == null ? null : classifyBO.getOppositeobjectid());
                    updateBankReconciliation.setOppositeobjectname(classifyBO.getOppositeobjectname());
                    //银行对账单中存入对方银行账号的id（目前仅支持内部单位）
                    if (null != classifyBO.getOppositebankacctid()) {
                        updateBankReconciliation.setTo_acct(classifyBO.getOppositebankacctid());
                    }
                } else {
                    //未匹配则标记为其他类型
                    updateBankReconciliation.setOppositetype(OppositeType.Other.getValue());
                    updateBankReconciliation.setOppositeobjectid(null);
                    updateBankReconciliation.setOppositeobjectname(null);
                }
                bankReconciliationBatch.add(updateBankReconciliation);
                } catch (Exception e) {
                    // 不报错，继续处理，以后优化增加列表返回错误信息
                    log.error("autoClassify error{}", e.getMessage());
                }finally {
                    if(CollectionUtils.isNotEmpty(bankReconciliationBatch)){
                        //yms更新流水
                       this.batchUpdateBankReconciliationClassfy(bankReconciliationBatch);
                    }
                }
            }
            return builder;
        },false);

        return true;
    }

    public void batchUpdateBankReconciliationClassfy(List<BankReconciliation> bankReconciliationBatch) {
        try{

            String updateSql ="UPDATE cmp_bankreconciliation SET oppositetype=?,oppositeobjectid=?,oppositeobjectname=? where id=? ";

            List<SQLParameter> parameters = new ArrayList<>();
            for(BankReconciliation bankReconciliation:bankReconciliationBatch){
                SQLParameter sqlParameter = new SQLParameter();
                sqlParameter.addParam(bankReconciliation.getOppositetype());
                sqlParameter.addParam(bankReconciliation.getOppositeobjectid());
                sqlParameter.addParam(bankReconciliation.getOppositeobjectname());
                sqlParameter.addParam(Long.parseLong(bankReconciliation.getId().toString()));
                parameters.add(sqlParameter);
            }
            int[] result = ymsJdbcApi.batchUpdate(updateSql,parameters);
            log.error("更新结果,result={}",result);
        }catch (Exception e){
            log.error("批量修改ods流水状态异常",e);
            throw new BankDealDetailException(BankDealDetailExceptionCodeEnum.BANKRECONCILIATION_ODS_UPDATE_ERROR.getErrCode(),BankDealDetailExceptionCodeEnum.BANKRECONCILIATION_ODS_UPDATE_ERROR.getMsg(), e);
        }
    }
    /**
     * 获取需要自动辨识的银行对账单集合
     *
     * @param params
     * @return
     */
    private List<BankReconciliation> getBankReconciliationList(JsonNode params) throws Exception {
        CtmJSONObject paramMap = new CtmJSONObject();
        paramMap.put("daysinadvance",params.get("dataRange").asText());
        paramMap.put(TaskUtils.TASK_START_DATE,params.get(TaskUtils.TASK_START_DATE).asText());
        paramMap.put(TaskUtils.TASK_END_DATE,params.get(TaskUtils.TASK_END_DATE).asText());
        HashMap<String, String> querydate = TaskUtils.queryDateProcess(paramMap, "yyyy-MM-dd");
        Date startDate = DateUtils.dateParse(querydate.get(TaskUtils.TASK_START_DATE), DateUtils.DATE_PATTERN);
        Date endDate = DateUtils.dateParse(querydate.get(TaskUtils.TASK_END_DATE), DateUtils.DATE_PATTERN);
        QuerySchema querySchema = QuerySchema.create().addSelect("*");
        QueryConditionGroup group = QueryConditionGroup.and(
                QueryCondition.name("associationstatus").eq(0),
                QueryCondition.name("ispublish").eq(0),//未发布
                QueryCondition.name("checkflag").eq(0),//勾兑标识
                QueryCondition.name("other_checkflag").eq(0)
        );
        JsonNode oppositeTypeNode = params.get("oppositetype");
        if (hasValue(oppositeTypeNode)) {
            QueryConditionGroup oppositeTypeGroup = getQueryConditionOppositeTypeGroup(params);
            // 添加对方类型条件
            group.appendCondition(oppositeTypeGroup);
        }
        JsonNode accentityeNode = params.get("accentity");
        if (hasValue(accentityeNode)) {
            String accentity = params.get("accentity").asText();
            group.appendCondition(QueryCondition.name("accentity").in(Arrays.asList(accentity.split(";"))));
        }
        group.appendCondition(QueryCondition.name("tran_date").between(startDate, endDate));
        querySchema.addCondition(group);
        Integer pagesize = params.get("pagesize").asInt() == 0 ? 1000 : params.get("pagesize").asInt();
        querySchema.addPager(0, pagesize);
        //增加按照登账日期倒序查询
        querySchema.addOrderBy(new QueryOrderby("tran_date","desc"));
        try {
            List<BankReconciliation> bankReconciliationList = MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME, querySchema, null);
            // 流水正在处理中的需要过滤
            return bankreconciliationService.filterBankReconciliationByLockKey(bankReconciliationList, BankReconciliationActions.CounterpartyTypeRecognition);
        } catch (Exception e) {
            log.error("获取对账单列表错误" + e);
            return new ArrayList<>();
        }
    }

    /**
     * 获取对方类型条件
     *
     * @param params
     * @return QueryConditionGroup
     * @discription 根据指定的对方类型和空构造查询条件
     */
    private static @NotNull QueryConditionGroup getQueryConditionOppositeTypeGroup(JsonNode params) {
        String[] oppositeType = params.get("oppositetype").asText().split(";");
        List<Short> oppositeTypeList = new ArrayList<>();
        for (String type : oppositeType) {
            oppositeTypeList.add(Short.valueOf(type));
        }
        // 对方类型为空
        QueryConditionGroup NullGroup = QueryConditionGroup.and(
                QueryCondition.name("oppositetype").is_null());
        // 对方类型指定类型
        QueryConditionGroup ValueGroup = QueryConditionGroup.and(
                QueryCondition.name("oppositetype").in(oppositeTypeList));
        // 指定对方类型和为空的数据
        return QueryConditionGroup.or(NullGroup,ValueGroup);
    }

    private static boolean hasValue(JsonNode params) {
        return params != null && StringUtils.isNotEmpty(params.asText()) && !"null".equalsIgnoreCase(params.asText().trim());
    }
}
