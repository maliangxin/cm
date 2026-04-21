package com.yonyoucloud.fi.cmp.balanceadjust.service.impl;

import com.yonyou.iuap.yms.id.generator.YmsOidGenerator;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.FilterCommonVO;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.FilterVO;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.balanceadjustresult.BalanceAdjustResult;
import com.yonyoucloud.fi.cmp.balanceadjustresult.BalanceAdjustResultSerevice;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.bankvouchercheck.service.BankVoucherCheckService;
import com.yonyoucloud.fi.cmp.bankvouchercheck.vo.BankAccountInfoQueryVO;
import com.yonyoucloud.fi.cmp.bankvouchercheck.vo.BankAccountInfoVO;
import com.yonyoucloud.fi.cmp.bankvouchercheck.vo.BankVoucherInfoQueryVO;
import com.yonyoucloud.fi.cmp.cmpentity.ReconciliationDataSource;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.journal.Journal;
import com.yonyoucloud.fi.cmp.journal.rule.JournalQueryRule;
import com.yonyoucloud.fi.cmp.util.threadpool.MyTask;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.imeta.biz.base.Objectlizer;
import org.imeta.orm.base.Json;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Slf4j
public class BalanceBatchOperationServiceImpl implements BalanceBatchOperationService {


    private static final String ITEM_NAME = "itemName";

    private static final String VALUE1 = "value1";

    private static final String VALUE2 = "value2";

    private static final String COMMON_VOS = "commonVOs";
    private static final String ACCENTITY_BANKACCOUNT_CURRENCY = "accBankCurrencyKey";
    // 定义常量来代替硬编码的字符串
    private static final String ACCENTITY = "accentity";
    private static final String BANKACCOUNT = "bankaccount";
    private static final String CURRENCY = "currency";
    private static final String RECONCILIATION_SCHEME = "reconciliationScheme";
    private static final String BANKRECONCILIATION_SCHEME = "bankreconciliationscheme";
    private static final String DZDATE = "dzdate";
    //银行账户名称
    private static final String BANKACCOUNT_NAME = "bankaccount_name";
    //币种名称
    private static final String CURRENCY_NAME = "currency_name";
    //对账组织名称
    private static final String ACCENTITY_NAME = "accentity_name";
    private static final String ACCENTITY_CODE = "accentity_code";
    private static final String RECONCILIATION_SCHEME_NAME = "reconciliationScheme_name";
    private static final String BANKACCOUNT_ACCOUNT = "bankaccount_account";
    private static final String FILTER_ARGS = "filterArgs";
    private static final String PARAM_OBJ = "paramObj";
    private static final String MAKE_TIME = "makeTime";
    private static final String MARK_LINKED_HASH_MAP = "true";
    private static String PARAM_BATCH_THREAD_SEPARATOR = "#";


    @Resource
    private BalanceAdjustServiceImpl balanceAdjustService;

    @Resource
    private BankVoucherCheckService bankVoucherCheckService;

    @Resource
    private BalanceAdjustResultSerevice balanceAdjustResultSerevice;

    @Resource
    private YmsOidGenerator ymsOidGenerator;
    @Resource
    private BalanceBatchCommonService balanceBatchCommonService;


    public BalanceBatchOperationServiceImpl(BalanceAdjustServiceImpl balanceAdjustService) {
        this.balanceAdjustService = balanceAdjustService;
    }


    @Override
    public List<CtmJSONObject> queryBatchConfirmedBalances(FilterVO filterVO,short reconciliationDataSource) throws Exception {
        // 输入参数校验
        if (Objects.isNull(filterVO)) {
            throw new CtmException("请求对象不能为空");//@notranslate
        }
        // 创建查询数据，组装接口合适的数据
        List<CtmJSONObject> commonVOs = beforeQueryCommonVOs(filterVO,reconciliationDataSource);
        if (CollectionUtils.isEmpty(commonVOs)) {
            throw new CtmException("未查询到用于查询待生成余额调节表的数据！");//@notranslate
        }
        try {
            // 使用线程池分批处理任务
            List<Object> ctmJSONObjects = balanceBatchCommonService.executeByBatchNotShutDown(commonVOs, "查询待生成余额调节表", queryBatchProcessor(commonVOs,reconciliationDataSource));//@notranslate
            // 处理返回结果
            return getCtmJSONObjects(ctmJSONObjects);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 批量保存余额调节表信息
     * 该方法用于批量处理和保存余额调节表数据，通过多线程方式执行批量操作
     *
     * @param data 包含余额调节表信息的CtmJSONObject列表，每个元素代表一个待处理的余额调节表请求对象
     * @return 处理结果的CtmJSONObject列表，包括成功或失败的响应信息
     * @throws Exception 如果请求对象为空或处理过程中发生异常，则抛出异常
     */
    @Override
    public CtmJSONObject saveBatchBalances(CtmJSONObject data) throws Exception {
        // 检查请求对象是否为空
        if (CollectionUtils.isEmpty(data)) {
            throw new CtmException("请求对象不能为空!");//@notranslate
        }
        // 从配置中获取每次批量处理的数量，并确保其为有效整数
        //int batchCount = getValidBatchCount("cmp.balanceBatch.batchCount", "10");
        try {
            List<CtmJSONObject> params = beforeSaveBalanceForParams(data);
            if (CollectionUtils.isEmpty(params)) {
                throw new CtmException("处理后的参数列表不能为空!");//@notranslate
            }
            // 使用线程池执行批量操作，不关闭线程池
            List<Object> ctmJSONObjects = balanceBatchCommonService.executeByBatchNotShutDown(params, "生成余额调节表", createBatchProcessor(params));//@notranslate
            //将线程池的多次执行结果进行平铺
            List<CtmJSONObject> ctmJSONObjectsFail = getCtmJSONObjects(ctmJSONObjects);
            // 获取差异数据，用于页面展示
            String difference = getDifference(params, ctmJSONObjectsFail);
            CtmJSONObject ctmJSONObject = new CtmJSONObject();
            ctmJSONObject.put("difference", difference);
            ctmJSONObject.put("resultParams", ctmJSONObjectsFail);
            return ctmJSONObject;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw e;
        }
    }


    @Override
    public CtmJSONObject queryBatchBalances(CtmJSONObject params) {
        return null;
    }

    /**
     * 创建一个查询批次处理器任务
     * 该方法用于生成一个MyTask任务，该任务会处理给定的CtmJSONObject列表中的指定批次，
     * 并对每个CtmJSONObject执行查询操作，然后合并查询结果
     *
     * @param commonVOs 包含查询请求信息的CtmJSONObject列表
     * @return 返回一个MyTask任务，该任务接受起始索引和结束索引作为参数，并返回查询结果列表
     */
    private MyTask queryBatchProcessor(List<CtmJSONObject> commonVOs,short reconciliationDataSource) {
        return (int fromIndex, int toIndex) -> {
            List<CtmJSONObject> batchResults = new ArrayList<>();
            // 遍历当前批次的业务对象，查询余额调节表
            for (int t = fromIndex; t < Math.min(toIndex, commonVOs.size()); t++) {
                CtmJSONObject enterpriseBalanceVO = commonVOs.get(t);
                try {
                    //初始化入参,获取查询凭证的信息
                    BankVoucherInfoQueryVO bankVoucherInfoQueryVO = innitBankAccountInfoForQuery(enterpriseBalanceVO);
                    if (bankVoucherInfoQueryVO != null){
                        bankVoucherInfoQueryVO.setReconciliationDataSource(reconciliationDataSource);
                    }
                    //查询关联的凭证或银行日记账数据
                    List<Journal> voucherList = new ArrayList<>();
                    if (reconciliationDataSource == ReconciliationDataSource.Voucher.getValue()){
                        voucherList = bankVoucherCheckService.getVoucherByBankAccountInfo(bankVoucherInfoQueryVO);
                    }
                    if (reconciliationDataSource == ReconciliationDataSource.BankJournal.getValue()){
                        voucherList = bankVoucherCheckService.getJournalByBankAccountInfo(bankVoucherInfoQueryVO);
                    }
                    //查询关联的银行流水数据
                    List<BankReconciliation> bankReconciliationList = bankVoucherCheckService.getBankReconciliationByBankAccountInfo(bankVoucherInfoQueryVO);
                    String uncheckflag = "0";
                    if (!CollectionUtils.isEmpty(voucherList) || !CollectionUtils.isEmpty(bankReconciliationList)) {
                        uncheckflag = "1";
                    }
                    CtmJSONObject responseMsg = balanceAdjustService.query(enterpriseBalanceVO, false);
                    responseMsg.put("accbookids", bankVoucherInfoQueryVO.getAccbookids());
                    responseMsg.put("journalyedetailinfo",CtmJSONObject.toJSONString(responseMsg.get("voucherDetailInfoList")));
                    //处理返回值和数据
                    mergeResponseData(responseMsg, enterpriseBalanceVO, uncheckflag);
                    batchResults.add(responseMsg);
                } catch (Exception e) {
                    log.error("查询余额调节表失败: {}", enterpriseBalanceVO.toString(), e);
                    // 根据业务需求决定是否继续执行后续查询
                    throw e;
                }
            }
            return batchResults;
        };
    }

    /**
     * 创建批量处理器任务
     * 该方法用于生成一个批量处理任务，该任务会处理给定参数列表中的数据
     * 每个参数代表一个需要处理的请求对象，包含必要的请求参数和配置
     *
     * @param params 请求对象列表，每个对象包含需要处理的参数和配置
     * @return 返回一个MyTask对象，该对象是一个批量处理器，用于执行批量处理任务
     */
    private MyTask createBatchProcessor(List<CtmJSONObject> params) {
        return (fromIndex, toIndex) -> {
            List<CtmJSONObject> ctmJSONObjectList = new ArrayList<>();
            // 遍历当前批次的请求对象
            for (int t = fromIndex; t < Math.min(toIndex, params.size()); t++) {
                CtmJSONObject obj = params.get(t);
                // 将请求对象中的参数转换为Json对象
                Json json = new Json(CtmJSONObject.toJSONString(obj.getJSONObject(PARAM_OBJ)));
                String filterArgs = obj.getString(FILTER_ARGS);
                CtmJSONObject ctmJson = obj.getJSONObject(PARAM_OBJ);
                String bankYe = ctmJson.getString("bankye");
                try {
                    // 验证银行余额格式是否正确
                    BigDecimal bigBankYe = new BigDecimal(bankYe);
                    // 解析Json对象为BalanceAdjustResult实体列表
                    List<BalanceAdjustResult> balanceAdjustResultList = Objectlizer.decode(json, BalanceAdjustResult.ENTITY_NAME);
                    if (CollectionUtils.isEmpty(balanceAdjustResultList)) {
                        // 如果解析结果为空，则记录错误信息并继续处理下一个请求对象
                        handleException(ctmJson, ctmJSONObjectList, "saveBatchBalances接口出现异常{}:后端接收参数为空！");//@notranslate
                        continue;
                    }
                    try {
                        //调用服务层方法添加余额调节表信息
                        balanceAdjustResultSerevice.add(balanceAdjustResultList.get(0), filterArgs, ctmJson);
                    } catch (Exception e) {
                        // 如果添加过程中发生异常，则记录错误信息
                        handleException(ctmJson, ctmJSONObjectList, e.getMessage());
                    }
                } catch (Exception e) {
                    // 如果银行余额格式不正确，则记录错误信息
                    handleException(ctmJson, ctmJSONObjectList, "银行对账单余额录入格式错误!");//@notranslate
                }
            }
            return ctmJSONObjectList;
        };
    }

    /**
     * 保存余额调节表信息之前的参数处理
     *
     * @param obj
     * @return
     */
    private List<CtmJSONObject> beforeSaveBalanceForParams(CtmJSONObject obj) throws Exception {
        CtmJSONArray jsonArray = obj.getJSONArray(PARAM_OBJ);
        if (jsonArray == null) {
            return Collections.emptyList();
        }
        //获取入参的map组成的集合
        List<Map<String, String>> paramObjForSave = jsonArray.stream()
                .filter(Objects::nonNull)
                .filter(item -> item instanceof Map)
                .map(item -> (Map<String, String>) item)
                .filter(map -> !CollectionUtils.isEmpty(map))
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(paramObjForSave)) {
            throw new CtmException("获取的入参paramsObj为空!");//@notranslate
        }
        beforeFiterArgs(obj, paramObjForSave);
        String filterArgs = CtmJSONObject.toJSONString(obj.getJSONObject(FILTER_ARGS));
        return paramObjForSave.stream()
                .filter(objPrams -> !CollectionUtils.isEmpty(objPrams))
                .map(objPrams -> {
                    //创建需要返回的json对象
                    CtmJSONObject ctmJSONObject = new CtmJSONObject();
                    //创建PARAM_OBJ
                    ctmJSONObject.put(PARAM_OBJ, objPrams);
                    ctmJSONObject.put(FILTER_ARGS, filterArgs);
                    return ctmJSONObject;
                }).collect(Collectors.toList());

    }

    private void beforeFiterArgs(CtmJSONObject objPrams, List<Map<String, String>> maps) {
        if (CollectionUtils.isEmpty(maps)) {
            throw new CtmException("获取的入参paramsObj为空!");//@notranslate
        }
        List<Map<String, Object>> commonVOs = new ArrayList<>();
        maps.stream().filter(map -> !CollectionUtils.isEmpty(map))
                .forEach(map -> {
                    // 添加字段到 commonVOs
                    addFieldIfNotEmpty(commonVOs, ACCENTITY, map.get(ACCENTITY), MARK_LINKED_HASH_MAP);
                    addFieldIfNotEmpty(commonVOs, BANKACCOUNT, map.get(BANKACCOUNT), MARK_LINKED_HASH_MAP);
                    addFieldIfNotEmpty(commonVOs, CURRENCY, map.get(CURRENCY), MARK_LINKED_HASH_MAP);
                    addFieldIfNotEmpty(commonVOs, JournalQueryRule.BANKRECONCILIATIONSCHEME, map.get(JournalQueryRule.BANKRECONCILIATIONSCHEME), MARK_LINKED_HASH_MAP);
                    addFieldIfNotEmpty(commonVOs, DZDATE, map.get(DZDATE), MARK_LINKED_HASH_MAP);
                    //addFieldIfNotEmpty(commonVOs, BANKACCOUNT_ACCOUNT, map.get(BANKACCOUNT_ACCOUNT), MARK_LINKED_HASH_MAP);
                    addFieldIfNotEmpty(commonVOs, MAKE_TIME, map.get(DZDATE), MARK_LINKED_HASH_MAP);
                    addFieldIfNotEmpty(commonVOs, "schemeName", "默认方案", MARK_LINKED_HASH_MAP);//@notranslate
                    addFieldIfNotEmpty(commonVOs, "isDefault", true, MARK_LINKED_HASH_MAP);
                });

        CtmJSONObject jsonObject = objPrams.getJSONObject(FILTER_ARGS);
        if (jsonObject == null) {
            throw new CtmException("获取的入参filterArgs为空!");//@notranslate
        }
        Object condition = jsonObject.get("condition");
        if (condition == null || !(condition instanceof Map)) {
            throw new CtmException("获取的入参filterArgs中的commonVos为空!");//@notranslate
        }
        ((Map<String, Object>) condition).put(COMMON_VOS, commonVOs);
    }


    /**
     * 合并保存余额调节表的响应数据
     * 该方法用于将企业余额信息中的特定字段值合并到响应消息中
     * 主要针对账户实体、银行账户、货币、对账方案等信息进行合并
     *
     * @param responseMsg       响应消息对象，用于存储合并后的数据
     * @param enterpriseBalance 企业余额视图对象，包含需要合并的数据
     */
    @Override
    public void mergeResponseData(CtmJSONObject responseMsg, CtmJSONObject enterpriseBalance, String uncheckflag) {
        if (enterpriseBalance == null || enterpriseBalance.get(COMMON_VOS) == null) {
            return;
        }
        Object commonVosObj = enterpriseBalance.get(COMMON_VOS);
        if (!(commonVosObj instanceof List<?>)) {
            return;
        }
        // 过滤并转换COMMON_VOS中的每个元素为Map<String, String>类型
        List<Map<String, String>> params = ((List<?>) commonVosObj).stream().filter(Objects::nonNull).filter(obj -> obj instanceof Map).map(object -> (Map<String, String>) object).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(params)) {
            return;
        }
        // 遍历每个过滤后的Map对象，根据itemName将value添加到responseMsg中
        params.stream().filter(Objects::nonNull).forEach(enterpriseBalanceVO -> {
            String itemName = enterpriseBalanceVO.get(ITEM_NAME);
            String value = enterpriseBalanceVO.get(VALUE1);
            switch (itemName) {
                case ACCENTITY:
                    responseMsg.put(ACCENTITY, value);
                    break;
                case BANKACCOUNT:
                    responseMsg.put(BANKACCOUNT, value);
                    break;
                case CURRENCY:
                    responseMsg.put(CURRENCY, value);
                    break;
                case BANKRECONCILIATION_SCHEME:
                    responseMsg.put(BANKRECONCILIATION_SCHEME, value);
                    break;
                case DZDATE:
                    responseMsg.put(DZDATE, value);
                    break;
                case ACCENTITY_NAME:
                    responseMsg.put(ACCENTITY_NAME, value);
                    break;
                case ACCENTITY_CODE:
                    responseMsg.put(ACCENTITY_CODE, value);
                    break;
                case CURRENCY_NAME:
                    responseMsg.put(CURRENCY_NAME, value);
                    break;
                case BANKACCOUNT_NAME:
                    responseMsg.put(BANKACCOUNT_NAME, value);
                    break;
                case RECONCILIATION_SCHEME_NAME:
                    responseMsg.put(RECONCILIATION_SCHEME_NAME, value);
                    break;
                case BANKACCOUNT_ACCOUNT:
                    responseMsg.put(BANKACCOUNT_ACCOUNT, value);
                    break;
                default:
                    // 记录未知的 ITEM_NAME，便于调试
                    break;
            }
        });
        //生成UUId
        responseMsg.put("id", ymsOidGenerator.nextId());
        responseMsg.put("uncheckflag", uncheckflag);
        //余额调节表获取余额逻辑调整：直联账户余额为空时，银行账户余额为空，调整余额为空，且设置为不平
        if (responseMsg.getBoolean("isEmptyBalance")){
            responseMsg.put("bankye", null);
            responseMsg.put("banktzye", null);
        }
    }

    /**
     * 根据业务对象列表创建通用视图对象列表(入参的转换)
     * 此方法旨在将一组业务对象转换为一组通用JSON对象，以便在不同的上下文中重用和共享这些数据
     *
     * @param filterVO 业务对象列表，不能为空
     * @param reconciliationDataSource 对账数据源 1凭证；2银行日记账
     * @return 返回一个通用视图对象列表，如果输入为空列表则返回null
     * @throws Exception 如果转换过程中发生错误，则抛出异常
     */
    private List<CtmJSONObject> beforeQueryCommonVOs(FilterVO filterVO,short reconciliationDataSource) throws Exception {
        // 检查传入的参数是否为 null，如果为 null 则直接返回 null
        if (Objects.isNull(filterVO)) {
            return null;
        }
        // 初始化返回结果列表
        List<CtmJSONObject> ctmJSONObjectList = new ArrayList<>();
        // 初始化银行账户信息查询参数对象
        List<String> uniKeys = new ArrayList<>();
        BankAccountInfoQueryVO bankAccountInfoVO = innitBankAccountInfo(filterVO,uniKeys);
        bankAccountInfoVO.setReconciliationDataSource(reconciliationDataSource);
        // 调用服务查询对账方案关联的银行账户+币种+对账组织
        List<BankAccountInfoVO> bankAccountInfoVOList = bankVoucherCheckService.queryBankAccountInfo(bankAccountInfoVO);
        // 检查查询结果是否为空，如果为空则抛出异常
        if (CollectionUtils.isEmpty(bankAccountInfoVOList)) {
            throw new CtmException("所传参数没有查询对账方案关联的银行账户+币种+对账组织！");//@notranslate
        }
        // 遍历查询结果，将信息封装到业务对象中
        bankAccountInfoVOList.stream().filter(Objects::nonNull)
                .filter(bankAccountInfo->uniKeys.contains(bankAccountInfo.getAccentity()+PARAM_BATCH_THREAD_SEPARATOR+bankAccountInfo.getBankaccount()+PARAM_BATCH_THREAD_SEPARATOR+bankAccountInfo.getCurrency()))
                .map(bankAccountInfo -> {
                    // 创建新的业务对象实例
                    CtmJSONObject ctmJSONObject = new CtmJSONObject();
                    List<Map<String, Object>> commonVOs = new ArrayList<>();
                    // 添加字段到 commonVOs
                    addFieldIfNotEmpty(commonVOs, ACCENTITY, bankAccountInfo.getAccentity(), null);
                    addFieldIfNotEmpty(commonVOs, BANKACCOUNT, bankAccountInfo.getBankaccount(), null);
                    addFieldIfNotEmpty(commonVOs, CURRENCY, bankAccountInfo.getCurrency(), null);
                    addFieldIfNotEmpty(commonVOs, JournalQueryRule.BANKRECONCILIATIONSCHEME, bankAccountInfo.getReconciliationScheme(), null);
                    addFieldIfNotEmpty(commonVOs, DZDATE, bankAccountInfoVO.getCheckEndDate(), null);
                    addFieldIfNotEmpty(commonVOs, ACCENTITY_NAME, bankAccountInfo.getAccentity_name(), null);
                    addFieldIfNotEmpty(commonVOs, ACCENTITY_CODE, bankAccountInfo.getAccentity_code(), null);
                    addFieldIfNotEmpty(commonVOs, CURRENCY_NAME, bankAccountInfo.getCurrency_name(), null);
                    addFieldIfNotEmpty(commonVOs, BANKACCOUNT_NAME, bankAccountInfo.getBankaccount_name(), null);
                    addFieldIfNotEmpty(commonVOs, RECONCILIATION_SCHEME_NAME, bankAccountInfo.getReconciliationScheme_name(), null);
                    addFieldIfNotEmpty(commonVOs, BANKACCOUNT_ACCOUNT, bankAccountInfo.getBankaccount_account(), null);
                    ctmJSONObject.put(COMMON_VOS, commonVOs);
                    return ctmJSONObject;
                }).forEach(ctmJSONObjectList::add);
        // 返回封装了查询结果的业务对象列表
        return ctmJSONObjectList;
    }


    /**
     * 添加字段到通用VO列表中，仅在值不为空时添加
     * 此方法用于构建通用VO列表，每个VO由一个名称-值对组成此方法确保只有当值不为空字符串时，才将其添加到列表中
     *
     * @param commonVOs 通用VO列表，存储构建的VO对象
     * @param itemName  项名称，表示VO中的名称字段
     * @param value     项的值，仅当此值不为空时才添加到VO中
     * @param mark      标记字段，决定是否使用LinkedHashMap
     */
    @Override
    public void addFieldIfNotEmpty(List<Map<String, Object>> commonVOs, String itemName, Object value, String mark) {
        // 检查值是否为空，仅当值不为空时才继续处理
        if (value != null) {
            Map<String, Object> map = new HashMap<>();
            // 根据mark的值决定是否使用LinkedHashMap，如果mark不为空，则使用LinkedHashMap保持插入顺序
            if (!StringUtils.isEmpty(mark)) {
                map = new LinkedHashMap<>();
            }
            // 将项名称和值添加到map中
            map.put(ITEM_NAME, itemName);
            // 如果是MAKE_TIME(业务日期)，则将值添加到VALUE2字段中，否则添加到VALUE1字段中
            // value2存储的是截止时间 总账会拼接小于
            if (MAKE_TIME.equals(itemName)) {
                map.put(VALUE2, value);
            } else {
                map.put(VALUE1, value);
            }
            // 将构建好的VO（map）添加到commonVOs列表中
            commonVOs.add(map);
        }
    }


    /**
     * 初始化银行账户信息查询对象
     * 该方法从给定的业务对象列表中提取相关信息，以构建一个BankAccountInfoQueryVO对象
     * 主要处理包括资金组织、对账截止日期、交易开始日期、业务开始日期、银行账户列表、对账方案列表、货币列表和银行类型列表
     *
     * @param filterVO 业务对象列表，用于提取构建查询对象所需的信息
     * @return 返回一个初始化后的BankAccountInfoQueryVO对象，包含从业务对象列表中提取的信息
     * @throws Exception 如果必要的信息缺失，如请求对象为空、资金组织为空或对账截止日期为空，则抛出异常
     */
    private BankAccountInfoQueryVO innitBankAccountInfo(FilterVO filterVO,List<String> uniKeys) throws Exception {
        // 提取并验证资金组织信息
        FilterCommonVO[] commonVOs = filterVO.getCommonVOs();
        List<String> accentityList = getValueToList(commonVOs, ACCENTITY);
        List<String> accentityBankaccountCurrency = getValueToList(commonVOs, ACCENTITY_BANKACCOUNT_CURRENCY);
        if (!CollectionUtils.isEmpty(accentityBankaccountCurrency)){
            uniKeys.addAll(accentityBankaccountCurrency);
        }
        // 提取并验证对账截止日期信息
        //对账截止日期
        String checkEndDate = getValueToString(commonVOs, "check_end_date");
        if (StringUtils.isEmpty(checkEndDate)) {
            throw new CtmException("对账截止日期必填，请检查！");//@notranslate
        }
        // 提取交易开始日期信息
        String tranStartDate = getValueToString(commonVOs, "tranStartDate");
        // 提取业务开始日期信息
        String businessStartDate = getValueToString(commonVOs, "businessStartDate");
        // 提取银行账户列表信息
        List<String> bankAccountList = getValueToList(commonVOs, BANKACCOUNT);
        // 提取对账方案列表信息
        List<String> reconciliationSchemeList = getValueToList(commonVOs, RECONCILIATION_SCHEME);
        // 提取币种信息
        List<String> currencyList = getValueToList(commonVOs, CURRENCY);
        // 提取银行类别列表信息
        List<String> banktypeList = getValueToList(commonVOs, "banktype");
        // 构建并返回BankAccountInfoQueryVO对象
        BankAccountInfoQueryVO infoQueryVO = new BankAccountInfoQueryVO();
        infoQueryVO.setAccentityList(accentityList);
        infoQueryVO.setCheckEndDate(checkEndDate);
        infoQueryVO.setTranStartDate(tranStartDate);
        infoQueryVO.setBusinessStartDate(businessStartDate);
        infoQueryVO.setBankAccountList(bankAccountList);
        infoQueryVO.setReconciliationSchemeList(reconciliationSchemeList);
        infoQueryVO.setCurrencyList(currencyList);
        infoQueryVO.setBanktypeList(banktypeList);
        return infoQueryVO;
    }

    private BankVoucherInfoQueryVO innitBankAccountInfoForQuery(CtmJSONObject enterpriseBalanceVO) {
        if (CollectionUtils.isEmpty(enterpriseBalanceVO) || enterpriseBalanceVO.get(COMMON_VOS) == null) {
            throw new CtmException("获取的入参paramsObj为空!");//@notranslate
        }
        Object commonVosObj = enterpriseBalanceVO.get(COMMON_VOS);
        if (!(commonVosObj instanceof List<?>)) {
            return null;
        }
        // 过滤并转换COMMON_VOS中的每个元素为Map<String, String>类型
        List<Map<String, String>> params = ((List<?>) commonVosObj).stream().filter(Objects::nonNull).filter(obj -> obj instanceof Map).map(object -> (Map<String, String>) object).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(params)) {
            return null;
        }
        // 构建并返回BankAccountInfoQueryVO对象
        BankVoucherInfoQueryVO infoQueryVO = new BankVoucherInfoQueryVO();
        // 遍历每个过滤后的Map对象，根据itemName将value添加到responseMsg中
        params.stream().filter(Objects::nonNull).forEach(enterpriseBalance -> {
            String itemName = enterpriseBalance.get(ITEM_NAME);
            String value = enterpriseBalance.get(VALUE1);
            switch (itemName) {
                case ACCENTITY:
                    infoQueryVO.setAccentity(value);
                    break;
                case BANKACCOUNT:
                    infoQueryVO.setBankaccount(value);
                    break;
                case CURRENCY:
                    infoQueryVO.setCurrency(value);
                    break;
                case BANKRECONCILIATION_SCHEME:
                    infoQueryVO.setReconciliationScheme(value);
                    break;
                case DZDATE:
                    infoQueryVO.setCheckEndDate(value);
                    break;
                case BANKACCOUNT_ACCOUNT:
                    infoQueryVO.setBankaccount_account(value);
                    break;
                default:
                    // 记录未知的 ITEM_NAME，便于调试
                    break;
            }
        });
        return infoQueryVO;
    }


    /**
     * 根据指定的键从FilterCommonVO数组中提取对应的值，并返回一个字符串列表
     * 此方法主要用于从一个FilterCommonVO对象数组中，根据给定的键提取出所有匹配的值并组成一个列表
     * 它通过流处理方式，首先过滤掉空对象，然后根据键值映射对应的值，再次过滤掉映射后可能产生的空值，最后收集结果
     *
     * @param commonVOs FilterCommonVO对象数组，代表一组过滤条件和对应的值
     * @param key       指定的键，用于从FilterCommonVO对象中提取对应的值
     * @return 返回一个字符串列表，包含所有与指定键匹配的值
     */
    private List<String> getValueToList(FilterCommonVO[] commonVOs, String key) {
        if (commonVOs == null || key == null) {
            return Collections.emptyList();
        }
        return Arrays.stream(commonVOs)
                .filter(Objects::nonNull)
                .filter(item -> key.equals(item.getItemName()))
                //映射每个非null的FilterCommonVO对象到其值，如果其项名称与给定的键匹配
                .flatMap(entry -> {
                    // 判断 entry 的值是 List<String> 还是 String
                    if (entry.getValue1() instanceof List) {
                        return ((List<String>) entry.getValue1()).stream(); // 如果是 List<String> 展开
                    } else if (entry.getValue1() instanceof String) {
                        return Stream.of((String) entry.getValue1()); // 如果是 String，单独放入流中
                    } else {
                        return Stream.empty(); // 如果值不是 List 或 String，返回空流
                    }
                })
                .collect(Collectors.toList());
    }

    /**
     * 根据键获取并转换过滤器通用视图数组中的值为字符串
     * 该方法主要用于在给定的FilterCommonVO数组中查找第一个匹配给定键的值，并将其转换为字符串
     * 如果没有找到匹配的值，或者匹配的值为空，则返回null
     *
     * @param commonVOs FilterCommonVO数组，包含待处理的过滤器通用视图对象
     * @param key       要匹配的键，用于在FilterCommonVO对象中查找对应的值
     * @return 对应键的值转换后的字符串，如果没有找到或值为空，则返回null
     */
    private String getValueToString(FilterCommonVO[] commonVOs, String key) {
        //对账组织
        return Arrays.stream(commonVOs)
                .filter(Objects::nonNull)
                .filter(item -> key.equals(item.getItemName()))
                //映射每个非null的FilterCommonVO对象到其值，如果其项名称与给定的键匹配
                .flatMap(entry -> {
                    // 判断 entry 的值是 List<String> 还是 String
                    if (entry.getValue1() instanceof List) {
                        return ((List<String>) entry.getValue1()).stream(); // 如果是 List<String> 展开
                    } else if (entry.getValue1() instanceof String) {
                        return Stream.of((String) entry.getValue1()); // 如果是 String，单独放入流中
                    } else {
                        return Stream.empty(); // 如果值不是 List 或 String，返回空流
                    }
                })
                .findFirst().orElse(null);
    }


    /**
     * 将线程池的多次执行结果进行平铺
     *
     * @param ctmJSONObjects
     * @return
     */
    private static @NotNull List<CtmJSONObject> getCtmJSONObjects(List<Object> ctmJSONObjects) {
        List<CtmJSONObject> CtmJSONObjectResults = new ArrayList<>();
        if (!CollectionUtils.isEmpty(ctmJSONObjects)) {
            // 过滤并展平列表
            List<CtmJSONObject> collect = ctmJSONObjects.stream()
                    .filter(Objects::nonNull)
                    .filter(obj -> obj instanceof List)
                    .flatMap(list -> ((List<CtmJSONObject>) list).stream())
                    .collect(Collectors.toList());
            if (!CollectionUtils.isEmpty(collect)) {
                CtmJSONObjectResults.addAll(collect);
            }
        }
        return CtmJSONObjectResults;
    }

    /**
     * 封装保存余额调节表的返回参数
     *
     * @param obj
     * @param ctmJSONObjectList
     * @param message
     */
    private void handleException(CtmJSONObject obj, List<CtmJSONObject> ctmJSONObjectList, String message) {
        obj.put("failMark", false);
        obj.put("failMsg", message);
        ctmJSONObjectList.add(obj);
    }


    /**
     * 获取有效的批量处理数量
     * 该方法首先从环境变量中获取指定的批量处理数量配置，如果未找到或值无效，则使用默认值
     * 无效的情况包括非数字字符串或数字小于等于0
     *
     * @param batchStr     批量处理数量的环境变量名称
     * @param defaultValue 如果环境变量中未找到指定配置，使用的默认值
     * @return 有效的批量处理数量如果输入无效，返回默认值10
     */
    private static int getValidBatchCount(String batchStr, String defaultValue) {
        // 从环境变量中获取批量处理数量的配置，如果不存在，则使用默认值
        String batchCountStr = AppContext.getEnvConfig(batchStr.toLowerCase(), defaultValue);
        try {
            // 将获取到的字符串转换为整数
            if (StringUtils.isBlank(batchCountStr)) {
                throw new IllegalArgumentException("批量处理数量必须不为空！");//@notranslate
            }
            int batchCount = Integer.parseInt(batchCountStr);
            // 如果转换后的整数小于等于0，则抛出异常，因为批量处理数量必须大于0
            if (batchCount <= 0) {
                throw new IllegalArgumentException("批量处理数量必须大于0");//@notranslate
            }
            // 如果转换成功且值有效，返回该值
            return batchCount;
        } catch (NumberFormatException e) {
            log.warn("无效的批量处理数量配置: {}, 使用默认值10", batchCountStr);
            return 100;
        }
    }


    /**
     * 获取确认生成余额调节表的差异数据
     * 该方法用于根据传入的业务对象列表和查询结果列表，计算并返回差异数据
     *
     * @param params
     * @param ctmJSONObjectsFail
     * @return
     */
    private String getDifference(List<CtmJSONObject> params, List<CtmJSONObject> ctmJSONObjectsFail) {
        if (CollectionUtils.isEmpty(params)) {
            throw new CtmException("创建余额调节表参数为空!");//@notranslate
        }
        if (CollectionUtils.isEmpty(ctmJSONObjectsFail)) {
            ctmJSONObjectsFail = new ArrayList<>();
        }
        // 提前过滤非空且包含 dzdate 的 createParams
        String dzdate = filterValidCreateParamsDate(params);
        if (StringUtils.isEmpty(dzdate)) {
            throw new CtmException("创建余额调节表参数中无有效 dzdate!");//@notranslate
        }
        String difference = "";
        //全部失败 本次生成余额调节表：全部失败；具体失败原因请详见本页面的失败原因字段
        if (params.size() == ctmJSONObjectsFail.size()){
            difference = com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1F4103F00508000C", "本次生成余额调节表：全部失败；具体失败原因请详见本页面的失败原因字段") /* "本次生成余额调节表：全部失败；具体失败原因请详见本页面的失败原因字段" */;
        }
        //全部成功 本次生成余额调节表：全部成功；请前往“余额调节表”节点进行查看
        if (ctmJSONObjectsFail.size() == 0){
            difference = com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1F4103F00508000D", "本次生成余额调节表：全部成功；请前往“余额调节表”节点进行查看") /* "本次生成余额调节表：全部成功；请前往“余额调节表”节点进行查看" */;
        }
        //存在部分成功 本次生成余额调节表：成功*条，失败*条；具体失败原因请详见本页面的失败原因字段，已生成成功的余额调节表，请前往“余额调节表”节点进行查看
        if (ctmJSONObjectsFail.size() > 0 && params.size() > ctmJSONObjectsFail.size()){
            difference = String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1F4103F00508000B", "本次生成余额调节表：成功%s条，失败%s条；具体失败原因请详见本页面的失败原因字段，已生成成功的余额调节表，请前往“余额调节表”节点进行查看") /* "本次生成余额调节表：成功%s条，失败%s条；具体失败原因请详见本页面的失败原因字段，已生成成功的余额调节表，请前往“余额调节表”节点进行查看" */,params.size() - ctmJSONObjectsFail.size(), ctmJSONObjectsFail.size());
        }

        return difference;
    }


    /**
     * 提取公共的过滤和格式化操作，用于将CtmJSONObject对象按业务截止时间是否为空进行过滤，此字段不能为空
     *
     * @param createParams
     * @return
     */
    private String filterValidCreateParamsDate(List<CtmJSONObject> createParams) {
        return createParams.stream()
                .filter(Objects::nonNull)
                .map(ctmJSONObject -> {
                    Object paramObj = ctmJSONObject.get("paramObj");
                    if (paramObj != null && paramObj instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, String> paramMap = (Map<String, String>) paramObj;
                        return paramMap;
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .map(paramMap -> {
                    if (paramMap.containsKey(DZDATE)) {
                        return paramMap.get(DZDATE);
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);  // 获取 date 属性
    }
}
