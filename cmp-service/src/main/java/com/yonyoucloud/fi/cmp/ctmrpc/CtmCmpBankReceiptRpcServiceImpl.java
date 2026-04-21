package com.yonyoucloud.fi.cmp.ctmrpc;

import com.yonyou.iuap.yms.id.generator.YmsOidGenerator;
import com.yonyou.ucf.mdd.common.enums.OperationTypeEnum;
import com.yonyou.ucf.mdd.ext.api.IBillService;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyoucloud.ctm.stwb.unifiedsettle.vo.BankFlowInfoSettle;
import com.yonyoucloud.fi.cmp.bankelectronicreceipt.BankElectronicReceipt;
import com.yonyoucloud.fi.cmp.bankreceipt.service.BankReceiptLinkService;
import com.yonyoucloud.fi.cmp.bankreceipt.service.BankReceiptService;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.billclaim.BillClaimItem;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.api.bankreceipt.CtmCmpBankReceiptRpcService;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.dto.QueryReceiptByRelationBillInputDTO;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.dto.QueryReceiptByRelationBillReturnDTO;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.common.CommonRequestDataVo;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.receipt.BankResultVO;
import com.yonyoucloud.fi.cmp.util.CmpMetaDaoHelper;
import com.yonyoucloud.fi.cmp.util.StringUtils;
import com.yonyoucloud.fi.cmp.util.file.CooperationFileUtilService;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.receipt.BankReceiptRpcVo;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.receipt.RpcBankReceiptFileInfo;
import com.yonyoucloud.fi.cmp.util.threadpool.ThreadPoolBuilder;
import com.yonyoucloud.fi.cmp.util.threadpool.ThreadPoolUtil;
import cn.hutool.core.thread.BlockPolicy;
import com.yonyoucloud.fi.tmsp.openapi.ITmspBusiSysRegistrationRpcService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.core.base.ConditionOperator;
import org.imeta.core.base.ObjectUtils;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.yonyoucloud.ctm.stwb.unifiedsettle.pubitf.IBankTradeFlowInfoPubQueryService;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import cn.hutool.core.bean.BeanUtil;

/**
 * @author wangdengk
 * @desc 现金管理-银行交易回单 总账凭证提供接口
 * @date 2023/05/23 10:25
 */
@Slf4j
@Service
public class CtmCmpBankReceiptRpcServiceImpl implements CtmCmpBankReceiptRpcService {

    @Autowired
    private BankReceiptLinkService bankReceiptLinkService;
    @Autowired
    private BankReceiptService bankReceiptService;
    @Autowired
    private CooperationFileUtilService cooperationFileUtilService;
    @Autowired
    private YmsOidGenerator ymsOidGenerator;
    @Autowired
    private ITmspBusiSysRegistrationRpcService iTmspBusiSysRegistrationRpcService;
    @Autowired
    private IBankTradeFlowInfoPubQueryService iBankTradeFlowInfoPubQueryService;
    @Autowired
    private IBillService iBillService;

    //线程池
    static ExecutorService executorService = null;
    static String postfix_name = ".name";


    static {
        executorService = ThreadPoolBuilder.ioThreadPoolBuilder()
                .setThreadNamePrefix("cmp-CtmCmpBankReceiptRpcService-async-")
                .setQueueSize(100)
                .setDaemon(false)
                .setMaximumPoolSize(100)
                .setRejectHandler(new BlockPolicy())
                .builder(Integer.parseInt("300"), Integer.parseInt("100"));
    }

    @Override
    public QueryReceiptByRelationBillReturnDTO queryReceiptByRelationBill(QueryReceiptByRelationBillInputDTO queryReceiptByRelationBillInputDTO) throws Exception {
        ////拿到单据类型编码，去结算查他们的枚举id[不是平台的交易类型id]
        //TmspBusiSysRegistrationSubResp tmspBusiSysRegistrationSubResp = iTmspBusiSysRegistrationRpcService.queryBusiSysRegistrationSub(queryReceiptByRelationBillInputDTO.getBillTypeId(),"1");
        //String billTypeId = null;
        //if (tmspBusiSysRegistrationSubResp != null &&
        //        tmspBusiSysRegistrationSubResp.getData() != null &&
        //        !tmspBusiSysRegistrationSubResp.getData().isEmpty() &&
        //        tmspBusiSysRegistrationSubResp.getData().get(0) != null) {
        //    billTypeId = tmspBusiSysRegistrationSubResp.getData().get(0).getBillTypeId();
        //}
        //if (StringUtils.isEmpty(billTypeId)) {
        //    return new QueryReceiptByRelationBillReturnDTO();
        //}
        //结算接口：给他们传平台的单据类型编码，他们返回流水或认领单id
        BankFlowInfoSettle[] bankFlowInfoSettles = iBankTradeFlowInfoPubQueryService.queryBankTradeFlowInfoByBizInfo(queryReceiptByRelationBillInputDTO.getOrgId(), queryReceiptByRelationBillInputDTO.getBillTypeCode(), queryReceiptByRelationBillInputDTO.getBillIds());
        List<BankFlowInfoSettle> bankFlowInfoSettleList = Arrays.asList(bankFlowInfoSettles);
        //收集流水id
        List<String> reconciliationIdList = bankFlowInfoSettleList.stream()
        .flatMap(item -> Arrays.stream(item.getBankFlowIds()))
        .collect(Collectors.toList());
        //记录流水id和单据id的对应关系，一个单据可能对应多个流水，多个流水可能对应一个单据
        com.google.common.collect.ArrayListMultimap<String, String> reconciliationIdAndBillIdMap =
                com.google.common.collect.ArrayListMultimap.create();
        Arrays.stream(bankFlowInfoSettles)
                .filter(Objects::nonNull)
                .filter(settle -> settle.getBankFlowIds() != null)
                .forEach(settle ->
                        Arrays.stream(settle.getBankFlowIds())
                                .forEach(id -> reconciliationIdAndBillIdMap.put(id, settle.getBizId()))
                );
        if (reconciliationIdList == null || reconciliationIdList.size() == 0) {
            return new QueryReceiptByRelationBillReturnDTO();
        }
        List<com.yonyoucloud.fi.cmp.newapi.ctmrpc.dto.BankElectronicReceipt> bankElectronicReceiptList1 = getBankElectronicReceiptsByReconciliation(reconciliationIdList);

        List<com.yonyoucloud.fi.cmp.newapi.ctmrpc.dto.BankElectronicReceipt> bankElectronicReceiptList2 = getBankElectronicReceiptsByClaim(reconciliationIdList);

        // 空值检查和类型安全处理
        if (bankElectronicReceiptList1 == null) {
            bankElectronicReceiptList1 = new ArrayList<>();
        }
        if (bankElectronicReceiptList2 == null) {
            bankElectronicReceiptList2 = new ArrayList<>();
        }

        List<com.yonyoucloud.fi.cmp.newapi.ctmrpc.dto.BankElectronicReceipt> bankElectronicReceiptList = new ArrayList<>(
                CollectionUtils.union(bankElectronicReceiptList1, bankElectronicReceiptList2)
        );

        if (bankElectronicReceiptList.isEmpty()) {
            return new QueryReceiptByRelationBillReturnDTO();
        }
        bankElectronicReceiptList.forEach(receipt -> {
            receipt.setRelationBillIdList(reconciliationIdAndBillIdMap.get(receipt.getBankreconciliationid()));
        });
        QueryReceiptByRelationBillReturnDTO queryReceiptByRelationBillReturnDTO = new QueryReceiptByRelationBillReturnDTO();
        queryReceiptByRelationBillReturnDTO.setCount(bankElectronicReceiptList.size());
        queryReceiptByRelationBillReturnDTO.setData(bankElectronicReceiptList);
        return queryReceiptByRelationBillReturnDTO;
    }

    private static List<com.yonyoucloud.fi.cmp.newapi.ctmrpc.dto.BankElectronicReceipt> getBankElectronicReceiptsByReconciliation(List<String> reconciliationIdList) throws Exception {
        //根据流水id查关联的回单信息
        QuerySchema schema = getQuerySchema();
        QueryConditionGroup queryConditionGroup = new QueryConditionGroup(ConditionOperator.and);
        queryConditionGroup.appendCondition(QueryCondition.name(BankElectronicReceipt.BANKRECONCILIATIONID).in(reconciliationIdList));
        schema.addCondition(queryConditionGroup);
        List<Map<String, Object>> mapList = MetaDaoHelper.query(BankElectronicReceipt.ENTITY_NAME, schema, null);
        List<com.yonyoucloud.fi.cmp.newapi.ctmrpc.dto.BankElectronicReceipt> bankElectronicReceiptList = new ArrayList<>();

        if (CollectionUtils.isNotEmpty(mapList)) {
            for (Map<String, Object> map : mapList) {
                com.yonyoucloud.fi.cmp.newapi.ctmrpc.dto.BankElectronicReceipt receipt = getBankElectronicReceipt(map);
                bankElectronicReceiptList.add(receipt);
            }
        }
        return bankElectronicReceiptList;
    }

    private static com.yonyoucloud.fi.cmp.newapi.ctmrpc.dto.BankElectronicReceipt getBankElectronicReceipt(Map<String, Object> map) {
        //不规范的命名，框架转换不了，手动赋值下
        com.yonyoucloud.fi.cmp.newapi.ctmrpc.dto.BankElectronicReceipt receipt = BeanUtil.toBean(map, com.yonyoucloud.fi.cmp.newapi.ctmrpc.dto.BankElectronicReceipt.class);
        String postfix = "_name";
        receipt.setEnterpriseBankAccountName((String) map.get(BankElectronicReceipt.ENTERPRISE_BANK_ACCOUNT + postfix));
        return receipt;
    }

    private static  QuerySchema getQuerySchema() {
        QuerySchema schema = QuerySchema.create().addSelect("*");
        schema.addSelect(BankElectronicReceipt.ACCENTITY + postfix_name);
        schema.addSelect(BankElectronicReceipt.ENTERPRISE_BANK_ACCOUNT + postfix_name);
        schema.addSelect(BankElectronicReceipt.CURRENCY + postfix_name);
        return schema;
    }

    private static List<com.yonyoucloud.fi.cmp.newapi.ctmrpc.dto.BankElectronicReceipt> getBankElectronicReceiptsByClaim(List<String> claimIds) throws Exception {
        //根据认领单id查关联的回单信息
        //根据认领单子表查主表的id->银行对账单id->下载文件
        QuerySchema schema = QuerySchema.create();
        schema.addSelect("*");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("mainid").in(claimIds));
        schema.addCondition(conditionGroup);
        //认领单子表
        List<BillClaimItem> billClaimItems = MetaDaoHelper.queryObject(BillClaimItem.ENTITY_NAME, schema, null);
        List<com.yonyoucloud.fi.cmp.newapi.ctmrpc.dto.BankElectronicReceipt> bankElectronicReceipts = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(billClaimItems)) {
            Map<Long, List<BillClaimItem>> billClaimItemsMap = billClaimItems.stream().collect(Collectors.groupingBy(BillClaimItem::getMainid));
            //获取银行对账单ids
            List<String> bankbills = billClaimItems.stream().map(item -> String.valueOf(item.getBankbill())).collect(Collectors.toList());

            QuerySchema schema1 = getQuerySchema();
            QueryConditionGroup conditionGroup1 = new QueryConditionGroup(ConditionOperator.and);
            conditionGroup1.appendCondition(QueryCondition.name("bankreconciliationid").in(bankbills));
            schema1.addCondition(conditionGroup1);
            //电子回单
            List<Map<String, Object>> mapList = MetaDaoHelper.query(BankElectronicReceipt.ENTITY_NAME, schema1, null);

            if (CollectionUtils.isNotEmpty(mapList)) {
                for (Map<String, Object> map : mapList) {
                    com.yonyoucloud.fi.cmp.newapi.ctmrpc.dto.BankElectronicReceipt receipt = getBankElectronicReceipt(map);
                    bankElectronicReceipts.add(receipt);
                }
            }
        }
        return bankElectronicReceipts;
    }

    @Override
    public HashMap<String, List<BankResultVO>> queryBankReceiptByChecknos(String[] checknos) throws Exception {
        ConcurrentHashMap<String, List<BankResultVO>> resultObj = new ConcurrentHashMap<String, List<BankResultVO>>();
        // 第一步 根据勾兑号查询银行对账单数据  总账勾兑号取的是other_checkno 日记账是
        QuerySchema schema = QuerySchema.create().addSelect(" id,other_checkno");
        QueryConditionGroup conditionGroup = new QueryConditionGroup();
        conditionGroup.addCondition(QueryCondition.name("other_checkno").in(checknos));
        // 遍历组装数组
        schema.addCondition(conditionGroup);
        List<BankReconciliation> bankRecList =
                MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME, schema, null);
        if (CollectionUtils.isEmpty(bankRecList)) return null;
        List<String> conIds = new ArrayList<String>();
        Map<String, String> checknoMap = new HashMap<String, String>();
        for (BankReconciliation bankRec : bankRecList) {
            conIds.add(bankRec.getId().toString());
            checknoMap.put(bankRec.getId().toString(), bankRec.getOther_checkno());
        }
        // 第二步 根据银行对账单关联的回单id查询回单信息
        QuerySchema receiptSchema = QuerySchema.create()
                .addSelect(" id,bankreconciliationid,enterpriseBankAccount,currency,tran_amt,uniqueCode," +
                        "extendss,isdown,other_checkno,accentity ");
        QueryConditionGroup receiptGroup = new QueryConditionGroup();
        receiptGroup.addCondition(QueryCondition.name("bankreconciliationid").in(conIds));// 根据银行对账单关联id查询回单
        receiptSchema.addCondition(receiptGroup);
        List<BankElectronicReceipt> bankReceipts =
                MetaDaoHelper.queryObject(BankElectronicReceipt.ENTITY_NAME, receiptSchema, null);
        if (CollectionUtils.isEmpty(bankReceipts)) return null;
        // 组装返回结果集
        ThreadPoolUtil.executeByBatch(executorService, bankReceipts, 5, "总账凭证查询现金回单", (int fromIndex, int toIndex) -> {//@notranslate
            String builder = "";
            for (int t = fromIndex; t < toIndex; t++) {
                BankElectronicReceipt bankReceipt = bankReceipts.get(t);
                String bankrecId = bankReceipt.getBankreconciliationid(); // 获取银行对账单id
                // 从勾兑号Map集合中获取勾兑号信息
                String checkno = checknoMap.get(bankrecId);
                BankResultVO resultVO = new BankResultVO();
                copyProperties(resultVO, bankReceipt);
                resultObj.computeIfAbsent(checkno, k -> new ArrayList<>()).add(resultVO);
            }
            return builder;
        }, false);
        // 将 ConcurrentHashMap 转换为 HashMap 返回
        return new HashMap<>(resultObj);
    }


    /**
     * 复制银行对账单的属性给结果集赋值
     *
     * @param resultVO
     * @param bankReceipt
     * @return
     * @throws Exception
     */
    private void copyProperties(BankResultVO resultVO, BankElectronicReceipt bankReceipt) {
        resultVO.setEnterpriseBankAccount(bankReceipt.getEnterpriseBankAccount());
        resultVO.setCurrency(bankReceipt.getCurrency());
        resultVO.setOther_checkno(bankReceipt.getOther_checkno());
        resultVO.setIsdown(bankReceipt.getIsdown());
        resultVO.setAccentity(bankReceipt.getAccentity());
        resultVO.setExtendss(bankReceipt.getExtendss());
        if (bankReceipt.getExtendss() != null) {
            if (bankReceipt.getExtendss().contains("/")) {
                try {
                    byte[] bytes = bankReceiptService.downloadAccountReceipt(bankReceipt.getExtendss());
                    resultVO.setFileByte(bytes);
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            } else {
                byte[] bytes = cooperationFileUtilService.queryBytesbyFileid(bankReceipt.getExtendss());
                String url = cooperationFileUtilService.queryprivilegeRealDownloadUrl(bankReceipt.getExtendss());
                resultVO.setFileurl(url);
                resultVO.setFileByte(bytes);
            }
        }
        resultVO.setTran_amt(bankReceipt.getTran_amt());
        String uniqueCode = bankReceipt.getUniqueCode();
        if (StringUtils.isNotEmpty(uniqueCode)) {
            resultVO.setUniqueCode(uniqueCode);
        } else {
            //导入时没有唯一码，用id 做唯一码给档案
            resultVO.setUniqueCode(bankReceipt.getId().toString());
        }
    }

    /**
     * desc:银行交易回单联查根据银行交易回单，如果传过来的银行对账单主键有值直接查询银行对账单关联的回单，如果没有根据6要素直接查询匹配的回单
     * 6要素：收付方向、本方银行账号、对方银行账号、对方户名、金额、摘要
     *
     * @param params
     * @return
     * @throws Exception
     */
    @Override
    public List<Map<String, Object>> queryMathData(CommonRequestDataVo params) throws Exception {
        List<Map<String, Object>> maps = bankReceiptLinkService.queryMathData(params);
        return maps;
    }

    /**
     * desc:银行交易回单联查根据银行交易回单，如果传过来的银行对账单主键有值直接查询银行对账单关联的回单，如果没有根据6要素直接查询匹配的回单
     * 6要素：收付方向、本方银行账号、对方银行账号、对方户名、金额、摘要
     *
     * @param params
     * @return
     * @throws Exception
     */
    @Override
    public List<Map<String, Object>> queryMathDataForRpc(CommonRequestDataVo params) throws Exception {
        List<Map<String, Object>> maps = bankReceiptLinkService.queryMathData(params);
        return maps;
    }

    private HashMap<Long, List<String>> queryurlByBankreconciliationid(List<Long> bankIds,List<CommonRequestDataVo> params,boolean isClaim) throws Exception {
        // 传入进来的bankIds可能为流水id，也可能为认领单id
        //银行对账单ids
        HashMap<Long, List<String>> urlsMap = new HashMap<>();
        QuerySchema schema = QuerySchema.create();
        schema.addSelect("*");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("bankreconciliationid").in(bankIds));
        schema.addCondition(conditionGroup);
        List<BankElectronicReceipt> bankElectronicReceipts = MetaDaoHelper.queryObject(BankElectronicReceipt.ENTITY_NAME, schema, null);
        List<BankElectronicReceipt> filterBankElectronicReceipts = bankElectronicReceipts.stream().filter(item -> null != item.getExtendss()).collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(filterBankElectronicReceipts)) {
            Map<String, String> bankElectronicReceiptsMap = filterBankElectronicReceipts.stream().collect(Collectors.toMap(BankElectronicReceipt::getBankreconciliationid, BankElectronicReceipt::getExtendss));
            for (CommonRequestDataVo param : params) {
                    List<String> urlIdList = new ArrayList<>();
                String urlId = null;
                if(isClaim){
                    if (!ObjectUtils.isEmpty(param.getClaimId()) && bankElectronicReceiptsMap.containsKey(String.valueOf(param.getClaimId()))) {
                        urlId = bankElectronicReceiptsMap.get(String.valueOf(param.getClaimId()));
                    }
                }else {
                    if (!ObjectUtils.isEmpty(param.getBankId()) && bankElectronicReceiptsMap.containsKey(String.valueOf(param.getBankId()))) {
                        urlId = bankElectronicReceiptsMap.get(String.valueOf(param.getBankId()));
                    }
                }
                    urlIdList.add(urlId);
                    urlsMap.put(param.getDataSettledDistributeId(), urlIdList);
                }
            }
        return urlsMap;
    }

    private HashMap<Long, List<String>> queryurlByClaimId(List<Long> claimIds,List<CommonRequestDataVo> params) throws Exception {
        HashMap<Long, List<String>> urlsMap = new HashMap<>();
        //根据认领单子表查主表的id->银行对账单id->下载文件
        QuerySchema schema = QuerySchema.create();
        schema.addSelect("*");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("mainid").in(claimIds));
        schema.addCondition(conditionGroup);
        //认领单子表
        List<BillClaimItem> billClaimItems = MetaDaoHelper.queryObject(BillClaimItem.ENTITY_NAME, schema, null);
        if (CollectionUtils.isNotEmpty(billClaimItems)) {
            Map<Long, List<BillClaimItem>> billClaimItemsMap = billClaimItems.stream().collect(Collectors.groupingBy(BillClaimItem::getMainid));
            //获取银行对账单ids
            List<String> bankbills = billClaimItems.stream().map(item -> String.valueOf(item.getBankbill())).collect(Collectors.toList());

            QuerySchema schema1 = QuerySchema.create();
            schema1.addSelect("*");
            QueryConditionGroup conditionGroup1 = new QueryConditionGroup(ConditionOperator.and);
            conditionGroup1.appendCondition(QueryCondition.name("bankreconciliationid").in(bankbills));
            schema1.addCondition(conditionGroup1);
            //电子回单
            List<BankElectronicReceipt> bankElectronicReceipts = MetaDaoHelper.queryObject(BankElectronicReceipt.ENTITY_NAME, schema1, null);
            List<BankElectronicReceipt> filterBankElectronicReceipts = bankElectronicReceipts.stream().filter(item -> null != item.getExtendss()).collect(Collectors.toList());
            Map<String, String> bankElectronicReceiptsMap = filterBankElectronicReceipts.stream().collect(Collectors.toMap(BankElectronicReceipt::getBankreconciliationid, BankElectronicReceipt::getExtendss));
            for (CommonRequestDataVo param : params) {
                if (!ObjectUtils.isEmpty(param.getClaimId()) && billClaimItemsMap.containsKey(param.getClaimId())) {
                    List<BillClaimItem> billClaimItemsList = billClaimItemsMap.get(param.getClaimId());
                    List<String> urlList = new ArrayList<>();
                    for (BillClaimItem item : billClaimItemsList) {
                        String urlId = bankElectronicReceiptsMap.get(String.valueOf(item.getBankbill()));
                        urlList.add(urlId);
                        urlsMap.put(param.getDataSettledDistributeId(), urlList);
                    }
                }
            }
        }else{
            // 兼容处理，存在收付那种，认领单ids实际上是流水id的情况
            urlsMap = queryurlByBankreconciliationid(claimIds,params,true);
        }
        return urlsMap;
    }

    /**
     * 根据流水ID/认领单ID，返回回单URL
     *
     * @param params
     * @return
     * @throws Exception
     */
    @Override
    public HashMap<Long, List<String>> queryUrl(List<CommonRequestDataVo> params) throws Exception {
        HashMap<Long, List<String>> urlsMap = new HashMap<>();
        //银行对账单ids
        List<CommonRequestDataVo> filterBankIdList = params.stream().filter(item -> !ObjectUtils.isEmpty(item.getBankId())).collect(Collectors.toList());
        //认领单ids
        List<CommonRequestDataVo> filterClaimIdList = params.stream().filter(item -> !ObjectUtils.isEmpty(item.getClaimId())).collect(Collectors.toList());
        //银行对账单
        if (CollectionUtils.isNotEmpty(filterBankIdList)) {
            List<Long> bankIds = filterBankIdList.stream().map(item -> item.getBankId()).collect(Collectors.toList());//银行对账单ids
            HashMap<Long, List<String>> bankIdUrlsMap = queryurlByBankreconciliationid(bankIds, params, false);
            if(!bankIdUrlsMap.isEmpty()){
                urlsMap.putAll(bankIdUrlsMap);
            }
        }
        //认领单
        if (CollectionUtils.isNotEmpty(filterClaimIdList)) {
            List<Long> claimIds = filterClaimIdList.stream().map(item -> item.getClaimId()).collect(Collectors.toList());//认领单ids
            HashMap<Long, List<String>> claimIdUrlsMap = queryurlByClaimId(claimIds, params);
            urlsMap.putAll(claimIdUrlsMap);
        }
        return urlsMap;
    }

    /**
     * 新增银行回单
     *
     * @param bankReceipt 银行回单信息-这个回单信息和回单mdd实体一致
     * @param fileInfo    银行回单的pdf附件信息
     * @return {@link HashMap }<{@link String },{@link Object }>   回单新增的结果
     * @throws Exception
     */
    public HashMap<String, Object> addNewBankReceipt(BankReceiptRpcVo bankReceipt, RpcBankReceiptFileInfo fileInfo) throws Exception {
        // 先把这个附件上传到协同云
        if (fileInfo != null && fileInfo.getFileContent() != null) {
            String fileformat = fileInfo.getFileType();
            byte[] b = org.apache.commons.codec.binary.Base64.decodeBase64(fileInfo.getFileContent());
            String fileId = cooperationFileUtilService.uploadOfFileBytes(b, ymsOidGenerator.nextStrId() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00022", "银行电子回单") /* "银行电子回单" */ + fileformat);
            bankReceipt.setExtendss(fileId);
            bankReceipt.setIsdown(true);
            bankReceipt.setFilename(fileInfo.getFileName());
        }
        BizObject bizObject = BizObject.fromMap(bankReceipt);
        bizObject.setId(ymsOidGenerator.nextId());
        bizObject.setEntityStatus(EntityStatus.Insert);
        // 然后保存这个回款单
        CmpMetaDaoHelper.insert(BankElectronicReceipt.ENTITY_NAME, bizObject);
        return bizObject;
    }




}
