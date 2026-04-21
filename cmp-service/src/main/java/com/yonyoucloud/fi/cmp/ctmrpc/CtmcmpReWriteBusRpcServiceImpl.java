package com.yonyoucloud.fi.cmp.ctmrpc;

import com.yonyou.cloud.utils.StringUtils;
import com.yonyou.cloud.yts.YtsContext;
import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.iuap.ucf.common.i18n.InternationalUtils;
import com.yonyou.iuap.yms.api.IYmsJdbcApi;
import com.yonyou.iuap.yms.id.generator.YmsOidGenerator;
import com.yonyou.iuap.yms.lock.YmsLock;
import com.yonyou.iuap.yms.param.SQLParameter;
import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVO;
import com.yonyou.ucf.basedoc.model.rpcparams.EnterpriseParams;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.yonbip.ctm.accelerator.threadpool.CtmThreadPoolExecutor;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.logger.business.CTMCMPBusinessLogService;
import com.yonyou.yonbip.ctm.remote.RemoteDubbo;
import com.yonyoucloud.ctm.stwb.reconcode.pubitf.ReconciliateCodeGenerator;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.cmp.autocorrsetting.CorrOperationService;
import com.yonyoucloud.fi.cmp.autocorrsetting.ReWriteBusCorrDataService;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankPublishSendMsgService;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliationbusrelation_b;
import com.yonyoucloud.fi.cmp.bankreconciliation.CommonSaveUtils;
import com.yonyoucloud.fi.cmp.bankreconciliation.utils.CommonParametersUtils;
import com.yonyoucloud.fi.cmp.billclaim.BillClaim;
import com.yonyoucloud.fi.cmp.billclaim.BillClaimItem;
import com.yonyoucloud.fi.cmp.cmpentity.*;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.IDomainConstant;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import com.yonyoucloud.fi.cmp.enums.ConfirmStatusEnum;
import com.yonyoucloud.fi.cmp.enums.SerialdealendState;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetaildao.IBankDealDetailAccessDao;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.api.bankrecilication.CtmcmpReWriteBusRpcService;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.Direction;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.bankreconciliation.CorrDataEntity;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.billclaim.BillClaimItemCheckEntityParam;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.billclaim.CorrDataEntityParam;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.common.CommonRequestDataVo;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.common.CommonResponseDataVo;
import com.yonyoucloud.fi.cmp.util.CmpMetaDaoHelper;
import com.yonyoucloud.fi.cmp.util.EntityTool;
import com.yonyoucloud.fi.cmp.util.JedisLockUtils;
import com.yonyoucloud.fi.cmp.util.basedoc.EnterpriseBankQueryService;
import com.yonyoucloud.fi.cmp.virtualFlowRuleConfig.VirtualFlowRuleConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QueryOrderby;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 资金结算 -->现金管理接口
 * 1，删除回写关联关系
 */
@Service
@Slf4j
@Transactional(rollbackFor = RuntimeException.class)
public class CtmcmpReWriteBusRpcServiceImpl implements CtmcmpReWriteBusRpcService {

    @Autowired
    @Qualifier("busiBaseDAO")
    protected IYmsJdbcApi ymsJdbcApi;
    private final Short delcorrStatus = 0;
    private final Short delcorrRowStatus = 1;
    @Autowired
    YmsOidGenerator ymsOidGenerator;
    @Autowired
    CorrOperationService corrOperationService;
    @Autowired
    ReWriteBusCorrDataService reWriteBusCorrDataService;
    @Autowired
    BaseRefRpcService baseRefRpcService;
    @Autowired
    EnterpriseBankQueryService enterpriseBankQueryService;

    @Autowired
    private BankPublishSendMsgService bankPublishSendMsgService;
    @Resource
    private IBankDealDetailAccessDao bankDealDetailAccessDao;
    @Autowired
    CTMCMPBusinessLogService ctmcmpBusinessLogService;

    @Autowired
    private CtmThreadPoolExecutor executorServicePool;

    /**
     * 自动、手工生单 -- 回写对账单关联关系
     *
     * @param corrDataEntitiesParam
     * @throws Exception
     */
    @Override
    public String batchReWriteBankRecilicationForRpc(List<CorrDataEntityParam> corrDataEntitiesParam) throws Exception {
        if (CollectionUtils.isEmpty(corrDataEntitiesParam)) {
            return null;
        }
        log.error("batchReWriteBankRecilicationForRpc param is " + CtmJSONObject.toJSONString(corrDataEntitiesParam));
        try {
            //todo 1.关联次数 2.财资统一对账码 3.避免并发操作，需要加锁
            CtmJSONObject result = new CtmJSONObject();
            if (corrDataEntitiesParam.isEmpty()) return null; //需回写关联数据为空.
            List<CorrDataEntity> corrDataEntities = new ArrayList();
            List<String> bankreconciliationIds = new ArrayList<>();
            for (CorrDataEntityParam entityParam : corrDataEntitiesParam) {
                CorrDataEntity entity = new CorrDataEntity();
                if (entityParam.getBankReconciliationId() != null) {
                    bankreconciliationIds.add(entityParam.getBankReconciliationId().toString());
                }
                BeanUtils.copyProperties(entityParam, entity);
                corrDataEntities.add(entity);
            }
            //用来封装智能对账勾兑码，存储单据ID和对应的智能对账码
            List<CtmJSONObject> smartCheckResult = new ArrayList<>();
            //调用资金结算财资统一对账码接口生成
            StringBuilder smartcheckno = new StringBuilder(RemoteDubbo.get(ReconciliateCodeGenerator.class, IDomainConstant.MDD_DOMAIN_STWB).generate());

            List<String> busid_4_rollback = new ArrayList<>();
            //优化效率，根据coorDataEntities一次拿流水
            Map<Long, BankReconciliation> bankReconciliationMap = getBankReconciliationMap(bankreconciliationIds);
            for (CorrDataEntity corrDataEntity : corrDataEntities) {
                if (corrDataEntity.getBillClaimItemId() != null) {
                    YmsLock ymsLock = null;
                    try {
                        ymsLock = JedisLockUtils.lockBillWithOutTrace("rewrite" + corrDataEntity.getBillClaimItemId().toString());
                        if (null == ymsLock) {
                            log.error("lock erro, lock key:" + corrDataEntity.getBillClaimItemId().toString());
                            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101672"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418003B", "该数据正在处理，请稍后重试！") /* "该数据正在处理，请稍后重试！" */);
                        } else {
                            log.error("lock success, lock key:" + corrDataEntity.getBillClaimItemId().toString());
                        }
                        // 认领单id不为空时，进入认领单逻辑
                        // relationType： 1代表对账单关联，2代表认领单关联，3代表参照关联
                        int relationType = this.handleBillclaimRelation(corrDataEntity, smartcheckno, busid_4_rollback);
                        if (relationType == 3) {
                            // 如果是参照单据id，不回写银行对账单关联关系
                            result.put("smartcheckno", corrDataEntity.getSmartcheckno());
                        } else {
                            result.put("smartcheckno", smartcheckno.toString());
                        }
                        result.put("busid", corrDataEntity.getBusid());
                        smartCheckResult.add(result);
                    } finally {
                        JedisLockUtils.unlockBillWithOutTrace(ymsLock);
                    }
                } else if (corrDataEntity.getBankReconciliationId() != null) {
                    YmsLock ymsLock = null;
                    try {
                        ymsLock = JedisLockUtils.lockBillWithOutTrace("rewrite" + corrDataEntity.getBankReconciliationId().toString());
                        if (null == ymsLock) {
                            log.error("lock error, lock key:" + corrDataEntity.getBankReconciliationId().toString());
                            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101672"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418003B", "该数据正在处理，请稍后重试！"));
                        } else {
                            log.error("lock success, lock key:" + corrDataEntity.getBankReconciliationId().toString());
                        }
                        this.handleBankreconciliationRelation(corrDataEntity, smartcheckno, busid_4_rollback, bankReconciliationMap);
                        result = new CtmJSONObject();
                        result.put("busid", corrDataEntity.getBusid());
                        String smartchecknoStr = smartcheckno.toString();
                        result.put("smartcheckno", smartchecknoStr);
                        result.put("iscorrsuccess", true);
                        smartCheckResult.add(result);
                    } finally {
                        JedisLockUtils.unlockBillWithOutTrace(ymsLock);
                    }
                }
            }
            //todo 分布式事务的数据往上下文需要补充
            if (CollectionUtils.isNotEmpty(busid_4_rollback)) {
                YtsContext.setYtsContext("bankReconciliationbusrelation_bs_ids", busid_4_rollback);//装填需回滚数据
            }
            return smartCheckResult.toString();
        } catch (Exception e) {
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21A7F36205380017", "正向回写逻辑异常，异常原因：") /* "正向回写逻辑异常，异常原因：" */ + e.getMessage(), e);
        }
    }

    /**
     * 根据流水id查询流水详情
     *
     * @param bankreconciliationIds
     * @return
     */
    private Map<Long, BankReconciliation> getBankReconciliationMap(List<String> bankreconciliationIds) throws Exception {
        Map<Long, BankReconciliation> bankReconciliationMap = new HashMap<>();
        List<BankReconciliation> bankReconciliations = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(bankreconciliationIds)) {
            QuerySchema schema = QuerySchema.create().addSelect("*");
            QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
            conditionGroup.appendCondition(QueryCondition.name("id").in(bankreconciliationIds));
            bankReconciliations = MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME, schema, null);
        }
        if (CollectionUtils.isNotEmpty(bankReconciliations)) {
            for (BankReconciliation bankReconciliation : bankReconciliations) {
                bankReconciliationMap.put(bankReconciliation.getId(), bankReconciliation);
            }
        }
        return bankReconciliationMap;
    }


    /**
     * 处理认领单id，同时区分该id对应的是对账单关联还是认领单关联还是参照关联
     *
     * @param corrDataEntity
     * @param smartCheckNo
     * @return
     * @throws Exception
     */
    private int handleBillclaimRelation(CorrDataEntity corrDataEntity, StringBuilder smartCheckNo, List<String> busid_4_rollback) throws Exception {
        int relationType = 2;
        try {
            // 1代表对账单关联，2代表认领单关联，3代表参照关联
            // 认领单判断是否是部分认领，然后获取已经存在的智能勾兑码
            // 先根据认领单id查询认领单，如果没有则考虑是参照关联
            // 查询优先级是 认领单关联、流水关联、参照关联
            BillClaim billClaim = MetaDaoHelper.findById(BillClaim.ENTITY_NAME, corrDataEntity.getBillClaimItemId());

            // 1.处理认领单关联的场景
            if (billClaim != null) {
                // 区分认领单的完结类型
                billClaim.setClaimcompletetype(ClaimCompleteType.RecePayGen.getValue());
                QuerySchema querySchema1 = QuerySchema.create().addSelect("*");
                QueryConditionGroup group1 = QueryConditionGroup.and(QueryCondition.name("mainid").eq(billClaim.getId()));
                querySchema1.addCondition(group1);
                List<BillClaimItem> items = MetaDaoHelper.queryObject(BillClaimItem.ENTITY_NAME, querySchema1, null);
                Short claimtype = billClaim.getClaimtype();
                if (claimtype == BillClaimType.Whole.getValue()) {//整单认领
                    checkBankreconciliationClaim4MergeAndWholeClaim(items, corrDataEntity, busid_4_rollback);
                } else if (claimtype == BillClaimType.Merge.getValue()) {//合并认领
                    checkBankreconciliationClaim4MergeAndWholeClaim(items, corrDataEntity, busid_4_rollback);
                } else if (claimtype == BillClaimType.Part.getValue()) {//部分认领
                    checkBankreconciliationClaim4PartClaim(items, corrDataEntity, busid_4_rollback);
                } else if (claimtype == BillClaimType.Batch.getValue()) {
                    //todo 暂时无相关逻辑 - 重点测试
                    checkBankreconciliationClaim4BatchClaim(items, corrDataEntity, busid_4_rollback);
                } else {
                    throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21A7F3620538001B", "非法的认领类型，不允许关联！") /* "非法的认领类型，不允许关联！" */);
                }
                billClaim.setAssociationstatus(AssociationStatus.Associated.getValue());
                billClaim.setAssociatedoperator(InvocationInfoProxy.getUserid());
                billClaim.setAssociateddate(new Date());
                billClaim.setEntityStatus(EntityStatus.Update);
                try {
                    if (!checkPubts(corrDataEntity)) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102578"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B000D4", "当前数据已被修改") /* "当前数据已被修改" */);
                    }
                } catch (Exception e) {
                    String checkSql = "update cmp_billclaim set pubts = pubts, associationstatus = 1 where associationstatus = 0 and id = ? and ytenant_id = ?";
                    SQLParameter params = new SQLParameter();
                    params.addParam(billClaim.getId().toString());
                    params.addParam(InvocationInfoProxy.getTenantid());
                    int count = ymsJdbcApi.update(checkSql, params);
                    if (count == 0) {
                        throw new CtmException(String.format("当前认领单%s不是最新状态，请刷新后重试！", billClaim.getCode())); //@notranslate
                    }
                }
                CommonSaveUtils.updateBillClaim(billClaim);
                //todo 分布式事务的数据往上下文需要补充
                YtsContext.setYtsContext("billClaimId", billClaim.getId());//装填需回滚数据
                if (StringUtils.isNotBlank(billClaim.getSmartcheckno())) {
                    smartCheckNo.setLength(0);
                    smartCheckNo.append(billClaim.getSmartcheckno());
                }
                //业务日志记录
                CtmJSONObject logparam = new CtmJSONObject();
                logparam.put("message", "生单关联：关联关系回写到认领单");//@notranslate
                logparam.put("corrData", corrDataEntity);
                logparam.put("claimInfo", billClaim);
                ctmcmpBusinessLogService.saveBusinessLog(logparam, billClaim.getCode(), "银行流水关联关系回写", IServicecodeConstant.CMPBANKRECONCILIATION, "银行流水认领", "认领单关联关系回写");//@notranslate
            } else {
                // 2.处理流水关联的场景
                //BankReconciliation bankReconciliation = MetaDaoHelper.findById(BankReconciliation.ENTITY_NAME, corrDataEntity.getBillClaimItemId());
                BankReconciliation bankReconciliation = corrDataEntity.getExtendFields() != null && corrDataEntity.getExtendFields().get("bankReconciliation") != null ? (BankReconciliation) corrDataEntity.getExtendFields().get("bankReconciliation") : null;
                if (bankReconciliation == null) {
                    bankReconciliation = MetaDaoHelper.findById(BankReconciliation.ENTITY_NAME, corrDataEntity.getBillClaimItemId());
                }
                if (bankReconciliation != null) {
                    // 若通过认领单ID未查询到认领单数据，则走银行对账单ID逻辑
                    // 银行对账单ID不存在则将认领单ID赋值给银行对账单ID
                    corrDataEntity.setBankReconciliationId(corrDataEntity.getBillClaimItemId());
                    // 认领单ID置为null
                    corrDataEntity.setBillClaimItemId(null);
                    this.handleBankreconciliationRelation(corrDataEntity, smartCheckNo, busid_4_rollback, new HashMap<>());
                } else {
                    // 3.处理参照管理的场景，流水查不到则为参照关联，参照关联则无需处理
                    QuerySchema querySchema = QuerySchema.create().addSelect("*");
                    QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("refbill").eq(corrDataEntity.getBillClaimItemId().toString()));
                    querySchema.addCondition(group);
                    List<BillClaim> list = MetaDaoHelper.queryObject(BillClaim.ENTITY_NAME, querySchema, null);
                    // 设置参照关联状态为已关联
                    if (CollectionUtils.isNotEmpty(list)) {
                        relationType = 3;
                        list.get(0).setRefassociationstatus(AssociationStatus.Associated.getValue());
                        list.get(0).setEntityStatus(EntityStatus.Update);
                        try {
                            if (!checkPubts(corrDataEntity)) {
                                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102578"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B000D4", "当前数据已被修改") /* "当前数据已被修改" */);
                            }
                        } catch (Exception e) {
                            String checkSql = "update cmp_billclaim set pubts = pubts,refassociationstatus = 1 where (refassociationstatus = 0 or refassociationstatus is null) and id = ? and ytenant_id = ?";
                            SQLParameter params = new SQLParameter();
                            params.addParam(list.get(0).getId().toString());
                            params.addParam(InvocationInfoProxy.getTenantid());
                            int count = ymsJdbcApi.update(checkSql, params);
                            if (count == 0) {
                                throw new CtmException(String.format("当前认领单%s不是最新状态，请刷新后重试！", list.get(0).getCode())); //@notranslate
                            }
                        }
                        CommonSaveUtils.updateBillClaim(list.get(0));
                        YtsContext.setYtsContext("billClaimId", list.get(0).getId());//装填需回滚数据
                        YtsContext.setYtsContext("refbill", "1");//装填需回滚数据
                        return relationType;
                    } else {
                        throw new CtmException("未查询到关联的认领单或流水信息，不允许关联！");//@notranslate
                    }
                }
            }
        } catch (Exception e) {
            log.error("回写认领单数据错误：：" + e.getMessage(), e);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101227"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B000C5", "回写认领单数据错误：") /* "回写认领单数据错误：" */ + e.getMessage());
        }
        return relationType;
    }

    /**
     * 处理合并认领和整单认领场景
     *
     * @param items
     * @throws Exception
     */
    private void checkBankreconciliationClaim4MergeAndWholeClaim(List<BillClaimItem> items, CorrDataEntity corrDataEntity, List<String> busid_4_rollback) throws Exception {
        List<String> bankReconciliationbusrelation_bs_rollback_list = new ArrayList();
        for (BillClaimItem claimItem : items) {
            BankReconciliation bankReconciliation = MetaDaoHelper.findById(BankReconciliation.ENTITY_NAME, claimItem.getBankbill());
            if (bankReconciliation != null) {
                // 回写时候要关注该数据是否取消发布
                if (!bankReconciliation.getIspublish()) {
                    throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21A7F3620538001E", "当前流水已取消发布，不允许关联！") /* "当前流水已取消发布，不允许关联！" */);
                }
                // 校验认领单子表金额和流水金额不一致
                if (claimItem.getClaimamount().abs().compareTo(bankReconciliation.getTran_amt().abs()) != 0) {
                    throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21A7F36205380020", "合并或整单认领下，认领单子表金额和流水金额不一致，请检查数据！") /* "合并或整单认领下，认领单子表金额和流水金额不一致，请检查数据！" */);
                }
                //已完结的流水，不能继续关联
                if (bankReconciliation.getSerialdealendstate() == SerialdealendState.END.getValue()) {
                    throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21A7F36205380018", "当前流水已完结，不允许重复关联") /* "当前流水已完结，不允许重复关联" */);
                }

                // 流水认领完结方式
                bankReconciliation.setSerialdealtype(ClaimCompleteType.RecePayGen.getValue());
                bankReconciliation.setSerialdealendstate(SerialdealendState.END.getValue());
                bankReconciliation.setAssociationstatus(AssociationStatus.Associated.getValue());
                bankReconciliation.setEntityStatus(EntityStatus.Update);
                CommonSaveUtils.updateBankReconciliation(bankReconciliation);//回写流水数据

                corrDataEntity.setBankReconciliationId(bankReconciliation.getId());
                List<BankReconciliationbusrelation_b> bankReconciliationbusrelation_bs = new ArrayList();
                initBankReconciliationbusrelation_b(bankReconciliationbusrelation_bs, corrDataEntity);
                //生单避免重复，唯一性索引赋值，ordernum=1
                for (BankReconciliationbusrelation_b b : bankReconciliationbusrelation_bs) {
                    //避免重复生单，唯一性索引，生单序号ordernum；正常入账：ordernum=1;挂账：ordernum=1;冲挂账：ordernum=2;
                    Short virtualEntrytype = bankReconciliation.getVirtualEntryType() == null ? EntryType.Normal_Entry.getValue() : bankReconciliation.getVirtualEntryType();
                    String ordernum = null;
                    if (virtualEntrytype == EntryType.CrushHang_Entry.getValue()) {
                        ordernum = "2";
                    } else {
                        ordernum = "1";
                    }
                    b.setOrdernum(ordernum);
                    b.setAmountmoney(bankReconciliation.getTran_amt());
                }
                CmpMetaDaoHelper.insert(BankReconciliationbusrelation_b.ENTITY_NAME, bankReconciliationbusrelation_bs);//写入关联信息
                //发布待办消息：对方类型为客户或者供应商时,待办消息推送已办理
                this.handleConfirmMsg(bankReconciliation);
                bankReconciliationbusrelation_bs.stream().forEach(e -> {
                    bankReconciliationbusrelation_bs_rollback_list.add(e.getId().toString());
                });

            } else {
                throw new CtmException("根据流水id未查询到对应流水，不允许关联！");//@notranslate
            }
        }
        busid_4_rollback.addAll(bankReconciliationbusrelation_bs_rollback_list);
    }

    /**
     * 判断关联的流水类型
     *
     * @param relationId
     * @return
     * @throws Exception
     */
    public int checkRelationType(String relationId) throws Exception {
        // 1-代表认领单关联 2-流水关联 3-代表参照关联
        BillClaim billClaim = MetaDaoHelper.findById(BillClaim.ENTITY_NAME, relationId);
        // 1.处理认领单关联的场景
        if (billClaim != null) {
            return 1;
        } else {
            // 2.处理流水关联的场景
            BankReconciliation bankReconciliation = MetaDaoHelper.findById(BankReconciliation.ENTITY_NAME, relationId);
            if (bankReconciliation != null) {
                return 2;
            } else {
                return 3;
            }
        }
    }


    /**
     * 处理批量认领场景
     * 批量认领可能为部分认领，也可能为整单认领
     *
     * @param items
     * @throws Exception
     */
    private void checkBankreconciliationClaim4BatchClaim(List<BillClaimItem> items, CorrDataEntity corrDataEntity, List<String> busid_4_rollback) throws Exception {
        for (BillClaimItem claimItem : items) {
            BankReconciliation bankReconciliation = MetaDaoHelper.findById(BankReconciliation.ENTITY_NAME, claimItem.getBankbill());
            // 如果认领单子表金额和流水金额不一致，则认为是部分认领，否则为整单或合并认领
            if (claimItem.getClaimamount().abs().compareTo(bankReconciliation.getTran_amt()) != 0) {
                checkBankreconciliationClaim4PartClaim(items, corrDataEntity, busid_4_rollback);
            } else {
                checkBankreconciliationClaim4MergeAndWholeClaim(items, corrDataEntity, busid_4_rollback);
            }
        }
    }


    private static final BigDecimal ZERO = new BigDecimal(0);

    /**
     * 处理对账单的认领金额
     *
     * @param items
     * @throws Exception
     */
    private void checkBankreconciliationClaim4PartClaim(List<BillClaimItem> items, CorrDataEntity corrDataEntity, List<String> busid_4_rollback) throws Exception {
        List<String> bankReconciliationbusrelation_bs_rollback_list = new ArrayList();
        BillClaimItem claimItem = items.get(0);
        BankReconciliation bankReconciliation = MetaDaoHelper.findById(BankReconciliation.ENTITY_NAME, claimItem.getBankbill());
        if (bankReconciliation != null) {
            // 回写时候要关注该数据是否取消发布
            if (!bankReconciliation.getIspublish()) {
                throw new CtmException("当前对账单已取消发布，不允许关联！");//@notranslate
            }
            // 对账单待认领金额不为0，一定是未完结 - 如果不加锁的话，存在并发问题，查到的待认领金额可能是错的
            SerialdealendState serialdealendState = SerialdealendState.END;
            if (bankReconciliation.getAmounttobeclaimed().compareTo(ZERO) != 0) {
                serialdealendState = SerialdealendState.UNEND;
            } else {
                // 如果为0，则根据对账单id查询所有被认领的认领单信息，是否全部关联，为了防止并发，需要加锁，同一个认领单对应的对账单回写需要加锁控制
                QuerySchema querySchema = QuerySchema.create().addSelect("mainid");
                QueryConditionGroup group1 = QueryConditionGroup.and(QueryCondition.name("bankbill").eq(claimItem.getBankbill()));
                querySchema.addCondition(group1);
                List<BillClaimItem> itemsBybankbill = MetaDaoHelper.queryObject(BillClaimItem.ENTITY_NAME, querySchema, null);
                List<Long> ids = new ArrayList<>();
                for (BillClaimItem billClaimItem : itemsBybankbill) {
                    ids.add(billClaimItem.getMainid());
                }
                QuerySchema queryBillClaimSchema = QuerySchema.create().addSelect("*");
                QueryConditionGroup queryBillClaimgGroup = QueryConditionGroup.and(QueryCondition.name("id").in(ids));
                queryBillClaimSchema.addCondition(queryBillClaimgGroup);
                List<BillClaim> billClaims = MetaDaoHelper.queryObject(BillClaim.ENTITY_NAME, queryBillClaimSchema, null);
                for (BillClaim billClaim : billClaims) {
                    // 部分认领的数据，所有认领单除当前认领外其他认领单都已经关联，
                    if (!billClaim.getId().equals(claimItem.getMainid()) && (billClaim.getAssociationstatus() == null
                            || billClaim.getAssociationstatus() == AssociationStatus.NoAssociated.getValue())) {
                        serialdealendState = SerialdealendState.UNEND;
                        break;
                    }
                }
            }
            // 流水认领完结方式
            if (serialdealendState.getValue() == SerialdealendState.END.getValue()) {
                bankReconciliation.setSerialdealtype(ClaimCompleteType.RecePayGen.getValue());
            }
            //关联关系写入
            List<BankReconciliationbusrelation_b> bankReconciliationbusrelation_bs = new ArrayList();
            corrDataEntity.setBankReconciliationId(bankReconciliation.getId());
            initBankReconciliationbusrelation_b(bankReconciliationbusrelation_bs, corrDataEntity);
            //生单避免重复，唯一性索引赋值，ordernum=认领单id
            for (BankReconciliationbusrelation_b b : bankReconciliationbusrelation_bs) {
                b.setOrdernum(corrDataEntity.getBillClaimItemId() != null ? corrDataEntity.getBillClaimItemId().toString() : null);
            }
            CmpMetaDaoHelper.insert(BankReconciliationbusrelation_b.ENTITY_NAME, bankReconciliationbusrelation_bs);//写入关联信息
            //主表更新
            bankReconciliation.setSerialdealendstate(serialdealendState.getValue());
            bankReconciliation.setEntityStatus(EntityStatus.Update);
            bankReconciliation.setAssociationstatus(AssociationStatus.Associated.getValue());
            CommonSaveUtils.updateBankReconciliation(bankReconciliation);//回写银行对账单数据
            //发布待办消息：对方类型为客户或者供应商时,待办消息推送已办理
            this.handleConfirmMsg(bankReconciliation);
            // 设置回滚数据
            bankReconciliationbusrelation_bs.stream().forEach(e -> {
                bankReconciliationbusrelation_bs_rollback_list.add(e.getId().toString());
            });
            busid_4_rollback.addAll(bankReconciliationbusrelation_bs_rollback_list);
        } else {
            throw new CtmException("根据流水id未查询到对应流水，不允许关联！");//@notranslate
        }
    }

    private void initBankReconciliationbusrelation_b(List<BankReconciliationbusrelation_b> bankReconciliationbusrelation_bs, CorrDataEntity corrDataEntity) {
        BankReconciliationbusrelation_b bankReconciliationbusrelation_b = new BankReconciliationbusrelation_b();
        bankReconciliationbusrelation_b.setAccentity(corrDataEntity.getAccentity());
        bankReconciliationbusrelation_b.setBillnum(corrDataEntity.getBillNum());
        bankReconciliationbusrelation_b.setBillid(corrDataEntity.getBusid());
        bankReconciliationbusrelation_b.setBilltype(Short.valueOf(corrDataEntity.getBillType()));
        bankReconciliationbusrelation_b.setAmountmoney(corrDataEntity.getOriSum());
        bankReconciliationbusrelation_b.setBankreconciliation(corrDataEntity.getBankReconciliationId());
        bankReconciliationbusrelation_b.setBillcode(corrDataEntity.getCode());
        bankReconciliationbusrelation_b.setDept(corrDataEntity.getDept());
        bankReconciliationbusrelation_b.setProject(corrDataEntity.getProject());
        bankReconciliationbusrelation_b.setRelationtype(Relationtype.MakeBillAssociated.getValue());
        if (corrDataEntity.getAuto()) {
            bankReconciliationbusrelation_b.setRelationstatus(Relationstatus.Confirm.getValue());
        } else {
            bankReconciliationbusrelation_b.setRelationstatus(Relationstatus.Confirmed.getValue());
        }
        bankReconciliationbusrelation_b.setVouchdate(corrDataEntity.getVouchdate());
        bankReconciliationbusrelation_b.setSrcbillid(corrDataEntity.getMainid());
        bankReconciliationbusrelation_b.setIsadvanceaccounts(corrDataEntity.getIsadvanceaccounts());
        bankReconciliationbusrelation_b.setAssociationcount(corrDataEntity.getAssociationcount());
        bankReconciliationbusrelation_b.setId(ymsOidGenerator.nextId());
        bankReconciliationbusrelation_b.setEntityStatus(EntityStatus.Insert);
        bankReconciliationbusrelation_bs.add(bankReconciliationbusrelation_b);
    }

    /**
     * 处理对账单的关联关系
     *
     * @param corrDataEntity
     * @throws Exception
     */
    private void handleBankreconciliationRelation(CorrDataEntity corrDataEntity, StringBuilder smartcheckno, List<String> busid_4_rollback, Map<Long, BankReconciliation> bankReconciliationMap) throws Exception {

        if (corrDataEntity.getBankReconciliationId() == null) {
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21A7F3620538001A", "流水id为空，无法回写关联关系") /* "流水id为空，无法回写关联关系" */);//UID:P_CM-BE_1FED0A0204F80005
        }
        //智能勾兑码赋值,对账单上存在银行对账码，则智能勾兑码=银行对账码
        BankReconciliation bankReconciliation = null;
        if (bankReconciliationMap.get(corrDataEntity.getBankReconciliationId()) != null) {
            bankReconciliation = bankReconciliationMap.get(corrDataEntity.getBankReconciliationId());
        } else {
            bankReconciliation = MetaDaoHelper.findById(BankReconciliation.ENTITY_NAME, corrDataEntity.getBankReconciliationId());
        }
        // 先校验单据状态
        short billType = StringUtils.isEmpty(corrDataEntity.getBillType()) ? -1 : Short.parseShort(corrDataEntity.getBillType());
        if (EventType.StwbSettleMentDetails.getValue() != billType) {
            this.checkDataStatus(bankReconciliation, corrDataEntity);
        }
        // 仅关联一定有财资码且使用流水的码
        if (bankReconciliation != null && bankReconciliation.getIsparsesmartcheckno() != null && bankReconciliation.getIsparsesmartcheckno()) {
            smartcheckno.setLength(0);
            smartcheckno.append(bankReconciliation.getSmartcheckno());
        }
        // 其次使用请求参数中的财资统一码
        else if (StringUtils.isNotEmpty(corrDataEntity.getSmartcheckno())) {
            smartcheckno.setLength(0);
            smartcheckno.append(corrDataEntity.getSmartcheckno());
            // 流水和请求参数都没有财资统一码，使用新生成的财资码
            if (bankReconciliation != null && StringUtils.isEmpty(bankReconciliation.getSmartcheckno())) {
                bankReconciliation.setIsparsesmartcheckno(false);
                bankReconciliation.setSmartcheckno(smartcheckno.toString());
            }
            // 生单或关联，流水财资码不为空时，使用流水的财资码
        } else if (bankReconciliation != null && StringUtils.isNotEmpty(bankReconciliation.getSmartcheckno())) {
            smartcheckno.setLength(0);
            smartcheckno.append(bankReconciliation.getSmartcheckno());
        } else {
            bankReconciliation.setIsparsesmartcheckno(false);
            bankReconciliation.setSmartcheckno(smartcheckno.toString());
        }
        // 处理关联关系
        List<BankReconciliationbusrelation_b> bankReconciliationbusrelation_bs = new ArrayList();//对账单关联实体
        handleBankReconciliationbusrelation(bankReconciliationbusrelation_bs, corrDataEntity);

        // 提前入账场景处理，如果冲挂账或者正常入账，流水完结状态设置为已完结
        // 虚拟入账类型代表本次生单的入账类型，为空代表正常入账，否则取实际的入账类型
        // 手工场景下：在挂账时，流水的虚拟入账类型为挂账，传递给下游，下游回写时需要把入账类型赋值上，当为冲挂账时，该字段设置为冲挂账传给下游，回写的时候设置为冲挂账
        Short virtualEntrytype = bankReconciliation.getVirtualEntryType() == null ? EntryType.Normal_Entry.getValue() : bankReconciliation.getVirtualEntryType();
        if (corrDataEntity.getIsAuto() && bankReconciliation.getEntrytype() == null
                && corrDataEntity.getIsadvanceaccounts() != null && corrDataEntity.getIsadvanceaccounts()) {
            //只有自动提前入账生单才会进此逻辑
            virtualEntrytype = EntryType.Hang_Entry.getValue();
            bankReconciliation.setVirtualEntryType(virtualEntrytype);
            bankReconciliation.setIsadvanceaccounts(true);
        }
        if (virtualEntrytype == EntryType.CrushHang_Entry.getValue()) {// 冲挂账，流水完结状态设置为已完结，关联次数为2
            bankReconciliation.setAssociationcount(AssociationCount.Second.getValue());
            bankReconciliation.setSerialdealendstate(SerialdealendState.END.getValue());
        } else if (virtualEntrytype == EntryType.Hang_Entry.getValue()) {// 挂账，关联次数为1，是否提前入账为是
            bankReconciliation.setAssociationcount(AssociationCount.First.getValue());
            bankReconciliation.setIsadvanceaccounts(true);
        } else if (virtualEntrytype == EntryType.Normal_Entry.getValue()) {// 正常入账，流水完结状态设置为已完结
            bankReconciliation.setSerialdealendstate(SerialdealendState.END.getValue());
        }
        //避免重复生单，唯一性索引，生单序号ordernum；正常入账：ordernum=1;挂账：ordernum=1;冲挂账：ordernum=2;
        String ordernum = null;
        if (virtualEntrytype == EntryType.CrushHang_Entry.getValue()) {
            ordernum = "2";
        } else {
            ordernum = "1";
        }
        bankReconciliation.setEntrytype(virtualEntrytype);
        // 自动生单场景处理 - 流水有自动场景，认领单没有，入账类型默认为正常入账
        //todo 自动生单需要确认是否只查询入账类型为空或正常入账的
        boolean isAuto = corrDataEntity.getIsAuto();
        // 如果是自动生单，则是否已自动生单肯定为是

        if (isAuto) {
            //自动生单 - 待确认   手动生单 - 已确认
            bankReconciliation.setRelationstatus(Relationstatus.Confirm.getValue());
            bankReconciliation.setIsautocreatebill(true);
        } else {
            // 智能对账：设置智能勾兑号,自动生单不添加勾兑号;手动生单设置调用方入账类型，自动生单正常入账
            bankReconciliation.setRelationstatus(Relationstatus.Confirmed.getValue());

            // 下拨付款单或归集收款单生成虚拟流水，该流水只能做关联，不能做生单
            handleVirtualflow(bankReconciliation, corrDataEntity);

        }
        // 处理流水的关联状态等信息
        bankReconciliation.setAssociationstatus(AssociationStatus.Associated.getValue());
        bankReconciliation.setSerialdealtype(ClaimCompleteType.RecePayGen.getValue());
        if (isAuto && EventType.StwbSettleMentDetails.getValue() == billType) {
            //关联确认方法执行，单独走这一套
            bankReconciliation.setRelationstatus(Relationstatus.Confirmed.getValue());
            bankReconciliation.setIsautocreatebill(false);
            bankReconciliation.setSerialdealtype(ClaimCompleteType.RecePayAssociated.getValue());
            //组织待确认
            if (StringUtils.isEmpty(bankReconciliation.getAccentity())) {
                bankReconciliation.setAccentity(corrDataEntity.getAccentity());
                bankReconciliation.setConfirmstatus(ConfirmStatusEnum.RelationConfirmed.getIndex());
            }
        }
        bankReconciliation.setEntityStatus(EntityStatus.Update);
        // 由于上面已经进行更新
        bankReconciliation.setPubts(null);
        CommonSaveUtils.updateBankReconciliation(bankReconciliation);//回写银行对账单数据
        //生单避免重复，唯一性索引赋值
        for (BankReconciliationbusrelation_b b : bankReconciliationbusrelation_bs) {
            b.setOrdernum(ordernum);
        }
        CmpMetaDaoHelper.insert(BankReconciliationbusrelation_b.ENTITY_NAME, bankReconciliationbusrelation_bs);//写入关联信息
        // 发布待办消息：对方类型为客户或者供应商时,待办消息推送已办理
        // 未发布的流水不用处理待办消息
//        this.handleConfirmMsg(bankReconciliation);

        List<String> bankReconciliationbusrelation_bs_ids = new ArrayList<>();
        bankReconciliationbusrelation_bs.stream().forEach(e -> {
            bankReconciliationbusrelation_bs_ids.add(e.getId().toString());
        });
        busid_4_rollback.addAll(bankReconciliationbusrelation_bs_ids);//装填需回滚数据
        // 业务日志记录启动线程处理，一个耗时8毫米，500条耗时4秒
        BankReconciliation finalBankReconciliation = bankReconciliation;
        executorServicePool.getThreadPoolExecutor().submit(() -> {
            //业务日记记录
            CtmJSONObject logparam = new CtmJSONObject();
            logparam.put("message", "生单关联：关联关系回写到银行流水");//@notranslate
            logparam.put("corrData", corrDataEntity);
            logparam.put("bankreconciliationInfo", finalBankReconciliation);
            ctmcmpBusinessLogService.saveBusinessLog(logparam, finalBankReconciliation != null ? finalBankReconciliation.getBank_seq_no() : corrDataEntity.getCode(),
                    "银行流水关联关系回写", IServicecodeConstant.CMPBANKRECONCILIATION, "银行流水认领", "银行流水关联关系回写");//@notranslate
        });
    }

    /**
     * 处理虚拟对账单逻辑
     *
     * @param bankReconciliation
     * @param corrDataEntity
     * @throws Exception
     */
    private void handleVirtualflow(BankReconciliation bankReconciliation, CorrDataEntity corrDataEntity) throws Exception {
        String billtype = corrDataEntity.getBillType();
        if (billtype.equals(String.valueOf(EventType.Disburse_payment_slip.getValue())) || billtype.equals(String.valueOf(EventType.Collect_receipts.getValue()))) {
            Boolean flag = bankReconciliation.getIsvirtualflow() == null ? false : bankReconciliation.getIsvirtualflow();
            if (flag) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102581"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418059D", "该单据是虚拟流水单，请重新选择") /* "该单据是虚拟流水单，请重新选择" */);
            }
            //判断是否开启虚拟对账单设置
            Boolean check = check(bankReconciliation);
            if (check) {
                checkIsunique(bankReconciliation);
                createPushBill(bankReconciliation, billtype);
            }
        }
    }

    /**
     * 处理关联关系
     *
     * @param bankReconciliationbusrelation_bs
     * @param corrDataEntity
     */
    private void handleBankReconciliationbusrelation(List<BankReconciliationbusrelation_b> bankReconciliationbusrelation_bs, CorrDataEntity corrDataEntity) {
        BankReconciliationbusrelation_b existsBankReconciliationbusrelationB = this.queryBankReconciliationBusByBillId(corrDataEntity.getBankReconciliationId(), corrDataEntity.getBusid());
        Boolean confirmedWhenAuto = false;
        if (corrDataEntity.getExtendFields() != null) {
            Object relationStatusFlag = corrDataEntity.getExtendFields().get("relationStatus_flag");
            if (null != relationStatusFlag) {
                Short relationstatusFlag = Short.parseShort(relationStatusFlag + "");
                if (null != relationstatusFlag && relationstatusFlag == Relationstatus.Confirmed.getValue()) {
                    confirmedWhenAuto = true;
                }
            }
        }
        if (existsBankReconciliationbusrelationB != null) {
            //更新确认状态
            if (corrDataEntity.getAuto() && confirmedWhenAuto) {
                bankDealDetailAccessDao.updateBankReconciliationBusRelationStatus(Relationstatus.Confirmed.getValue(), Relationtype.AutoAssociated.getValue(), existsBankReconciliationbusrelationB.getId());
            }
        } else {
            //组装关联实体
            bankReconciliationbusrelation_bs.add(getBankReconciliationbusrelation_b(corrDataEntity, confirmedWhenAuto));
        }
    }


    /**
     * 校验当前对账单状态
     *
     * @param bankReconciliation
     */
    private void checkDataStatus(BankReconciliation bankReconciliation, CorrDataEntity corrDataEntity) throws Exception {

        // 1.流水生单时，需要校验发布状态，如果为发布
        // 2.如果不是发布，则需要判断当前流水是否为提前入账，如果不为提前入账，则关联信息不可以有多条
        if (bankReconciliation.getIspublish() != null && bankReconciliation.getIspublish()) {
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21A7F3620538001D", "当前流水已发布，请确认流水最新状态！对应流水银行交易流水号：") /* "当前流水已发布，请确认流水最新状态！对应流水银行交易流水号：" */ + bankReconciliation.getBank_seq_no());// UID:P_CM-BE_1FED089C04F80005
        } else {
            // 后面做了状态的校验，所以这里可以注释掉
//            // 如果提前入账为空或者非提前入账
//            if (bankReconciliation.getIsadvanceaccounts() == null || !bankReconciliation.getIsadvanceaccounts()) {
//                //部分认领处理TODO:已关联已确认
//                QueryConditionGroup group = new QueryConditionGroup(ConditionOperator.and);
//                group.addCondition(QueryConditionGroup.and(QueryCondition.name("bankreconciliation").eq(bankReconciliation.getId())));
//                QuerySchema querySchema = QuerySchema.create().addSelect(ICmpConstant.SELECT_TOTAL_PARAM);
//                querySchema.addCondition(group);
//                //查询银行对账单子表（对账单业务单据关联表）
//                List<BankReconciliationbusrelation_b> bankReconciliationbusrelation_bs = MetaDaoHelper.queryObject(BankReconciliationbusrelation_b.ENTITY_NAME, querySchema, null);
//                if (CollectionUtils.isNotEmpty(bankReconciliationbusrelation_bs)) {
//                    throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21A7F36205380019", "当前流水已关联，不允许重复关联！流水对应流水号为：") /* "当前流水已关联，不允许重复关联！流水对应流水号为：" */ + bankReconciliation.getBank_seq_no());// UID:P_CM-BE_1FED082805280009
//                }
//            }
        }

        if (corrDataEntity.getOriSum().compareTo(bankReconciliation.getTran_amt().abs()) != 0) {
            throw new CtmException("流水关联回写时，关联回写的金额与流水金额不一致！对应流水银行交易流水号：" + bankReconciliation.getBank_seq_no());//UID:P_CM-BE_1FED07CE05280003//@notranslate
        }

        //todo 关联状态需要注意提前入账场景的状态判断，如果是冲挂账，则可以进行关联
        if (bankReconciliation.getAssociationstatus() != null && bankReconciliation.getAssociationstatus() == AssociationStatus.Associated.getValue()
                && (bankReconciliation.getEntrytype() == null || bankReconciliation.getEntrytype() == EntryType.Normal_Entry.getValue())) {
            throw new CtmException("当前流水已关联，不允许重复关联！流水对应流水号为：" + bankReconciliation.getBank_seq_no());// UID:P_CM-BE_1FED082805280009//@notranslate
        }

        //已完结的流水，不能继续关联
        if (bankReconciliation.getSerialdealendstate() != null && bankReconciliation.getSerialdealendstate() == SerialdealendState.END.getValue()) {
            throw new CtmException("当前流水已完结，不允许重复关联");//UID:P_CM-BE_1FED072004F80001//@notranslate
        }

        // 针对上游传pubts的情况还是进行pubts的校验，如果pubts校验没通过，则根据状态来校验
        try {
            String checkSql = null;
            if (bankReconciliation.getEntrytype() == null || bankReconciliation.getEntrytype() == EntryType.Normal_Entry.getValue()) {
                checkSql = "update cmp_bankreconciliation set pubts = pubts, associationstatus = 1,serialdealendstate = 1 where associationstatus = 0 and serialdealendstate = 0 and ispublish <> 1 and id = ? and ytenant_id = ?";
            } else if (bankReconciliation.getEntrytype() == EntryType.CrushHang_Entry.getValue()) {
                checkSql = "update cmp_bankreconciliation set pubts = pubts, serialdealendstate = 1 where associationstatus = 1 and serialdealendstate = 0 and ispublish <> 1 and id = ? and ytenant_id = ?";
            } else {
                checkSql = "update cmp_bankreconciliation set pubts = pubts, serialdealendstate = 1 where serialdealendstate = 0 and ispublish <> 1 and id = ? and ytenant_id = ?";
            }
            SQLParameter params = new SQLParameter();
            params.addParam(bankReconciliation.getId().toString());
            params.addParam(InvocationInfoProxy.getTenantid());
            int count = ymsJdbcApi.update(checkSql, params);
            if (count == 0) {
                throw new CtmException(String.format("当前流水%s不是最新状态，请刷新后重试！", bankReconciliation.getBank_seq_no())); //@notranslate
            }
        } catch (Exception e) {
            throw e;
        }
    }

    /**
     * 发布待办消息：待办消息推送为已办理
     *
     * @param bankReconciliation
     */
    private void handleConfirmMsg(BankReconciliation bankReconciliation) {
        try {
            bankPublishSendMsgService.handleConfirmMsg(bankReconciliation);
        } catch (Exception e) {
            log.error("BankPublishSendMsgService handleConfirmMsg error:{}", e.getMessage());
        }
    }

    /**
     * @param corrDataEntity
     * @return
     * @throws Exception
     * @deprecated (use com.yonyoucloud.fi.cmp.ctmrpc.CtmcmpReWriteBusRpcServiceImpl.batchReWriteBankRecilicationForRpc instead)
     */
    @Override
    @Deprecated
    public String reWriteBus(CorrDataEntity corrDataEntity) throws Exception {
        List<String> logMarkList = new ArrayList<>();
        try {
            //步骤1
            logMarkList.add("手动生单创建关联关系第一步：银行对账单的唯一标识ID为：" + corrDataEntity.getBankReconciliationId());//@notranslate
            List<CorrDataEntity> corrDataEntities = new ArrayList<CorrDataEntity>();
            Long bid = corrDataEntity.getBankReconciliationId();
            int flag = 1;//区分统收统支和之前普通结算，如果list是null，则为统收统支和代理结算中心模式
            if (corrDataEntity.getBillClaimItemId() != null) {
                List<BillClaimItem> list;
                QuerySchema querySchema = QuerySchema.create().addSelect("*");
                QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("mainid").eq(corrDataEntity.getBillClaimItemId()));
                querySchema.addCondition(group);
                list = MetaDaoHelper.queryObject(BillClaimItem.ENTITY_NAME, querySchema, null);
                // 统收统支或代理结算 使用参照单据id的情况
                if (list == null || list.size() == 0) {
                    logMarkList.add("手动生单创建关联关系第二步：银行对账单的唯一标识ID为：" + corrDataEntity.getBankReconciliationId() + ";此时的flag的值为：" + flag);//@notranslate
                    flag = 2;
                    List<BillClaim> billClaims;
                    QuerySchema querySchema2 = QuerySchema.create().addSelect("id");
                    QueryConditionGroup group2 = QueryConditionGroup.and(QueryCondition.name("refbill").eq(corrDataEntity.getBillClaimItemId()));
                    querySchema2.addCondition(group2);
                    billClaims = MetaDaoHelper.queryObject(BillClaim.ENTITY_NAME, querySchema2, null);
                    if (billClaims != null && billClaims.size() > 0) {
                        QuerySchema querySchema3 = QuerySchema.create().addSelect("*");
                        QueryConditionGroup group3 = QueryConditionGroup.and(QueryCondition.name("mainid").eq(billClaims.get(0).getId()));
                        querySchema3.addCondition(group3);
                        list = MetaDaoHelper.queryObject(BillClaimItem.ENTITY_NAME, querySchema3, null);
                    }
                }
                if (list != null && list.size() > 0) {
                    if (flag == 1) {
                        logMarkList.add("手动生单创建关联关系第二步：银行对账单的唯一标识ID为：" + corrDataEntity.getBankReconciliationId() + ";此时的flag的值为：" + flag);//@notranslate
                    } else {
                        logMarkList.add("手动生单创建关联关系第三步：银行对账单的唯一标识ID为：" + corrDataEntity.getBankReconciliationId() + ";此时的list大小为：" + list.size());//@notranslate
                    }
                    for (BillClaimItem billClaimItem : list) {
                        CorrDataEntity corrData = new CorrDataEntity();
                        BeanUtils.copyProperties(corrDataEntity, corrData);
                        List<BankReconciliation> bankReconciliations;
                        corrData.setBankReconciliationId(billClaimItem.getBankbill());
                        corrData.setAuto(false);
                        corrDataEntities.add(corrData);
                    }
                }
            } else {
                corrDataEntities.add(corrDataEntity);
            }
            int ordernum = 1;
            for (CorrDataEntity corr : corrDataEntities) {
                if (flag == 1) {
                    logMarkList.add("手动生单创建关联关系第三步：银行对账单的唯一标识ID为：" + corrDataEntity.getBankReconciliationId() + ";修改银行对账单的状态，此时flag值为：" + flag);//@notranslate
                    reWriteBusCorrDataService.reWriteBankReconciliationData(corr, false);
                    corrOperationService.corrOpration(corr, ordernum);
                    ordernum++;
                } else {
                    logMarkList.add("手动生单创建关联关系第四步：银行对账单的唯一标识ID为：" + corrDataEntity.getBankReconciliationId() + ";修改银行对账单的状态，此时flag值为：" + flag);//@notranslate
                }
                if (corr.getBillClaimItemId() != null) {
                    if (flag == 1) {
                        logMarkList.add("手动生单创建关联关系第四步：银行对账单的唯一标识ID为：" + corrDataEntity.getBankReconciliationId() + ";修改认领单的状态，此时flag值为：" + flag);//@notranslate
                    } else {
                        logMarkList.add("手动生单创建关联关系第五步：银行对账单的唯一标识ID为：" + corrDataEntity.getBankReconciliationId() + ";修改银行对账单的状态，此时flag值为：" + flag);//@notranslate
                    }
                    Short associationStatus = 1;
                    reWriteBusCorrDataService.reWriteBillClaimData(corr.getBillClaimItemId(), associationStatus,
                            corrDataEntity.getSmartcheckno(), ClaimCompleteType.RecePayGen.getValue());
                }
            }
            if (logMarkList.size() != 4) {
                log.error(logMarkList.toString());
            }
        } catch (Exception e) {
            throw new Exception("回写关联关系异常，异常原因：" + e.getMessage(), e);//@notranslate
        }
        return "success";
    }

    @Override
    public String reWriteBusForRpc(CorrDataEntityParam corrDataEntity) throws Exception {
        List<CorrDataEntityParam> corrDataEntities = new ArrayList<CorrDataEntityParam>();
        Long bid = corrDataEntity.getBankReconciliationId();
        int flag = 1;//区分统收统支和之前普通结算，如果list是null，则为统收统支和代理结算中心模式
        if (corrDataEntity.getBillClaimItemId() != null) {
            List<BillClaimItem> list;
            QuerySchema querySchema = QuerySchema.create().addSelect("*");
            QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("mainid").eq(corrDataEntity.getBillClaimItemId()));
            querySchema.addCondition(group);
            list = MetaDaoHelper.queryObject(BillClaimItem.ENTITY_NAME, querySchema, null);
            // 统收统支或代理结算 使用参照单据id的情况
            if (list == null || list.size() == 0) {
                flag = 2;
                List<BillClaim> billClaims;
                QuerySchema querySchema2 = QuerySchema.create().addSelect("id");
                QueryConditionGroup group2 = QueryConditionGroup.and(QueryCondition.name("refbill").eq(corrDataEntity.getBillClaimItemId()));
                querySchema2.addCondition(group2);
                billClaims = MetaDaoHelper.queryObject(BillClaim.ENTITY_NAME, querySchema2, null);
                if (billClaims != null && billClaims.size() > 0) {
                    QuerySchema querySchema3 = QuerySchema.create().addSelect("*");
                    QueryConditionGroup group3 = QueryConditionGroup.and(QueryCondition.name("mainid").eq(billClaims.get(0).getId()));
                    querySchema3.addCondition(group3);
                    list = MetaDaoHelper.queryObject(BillClaimItem.ENTITY_NAME, querySchema3, null);
                }
            }
            if (list != null && list.size() > 0) {
                for (BillClaimItem billClaimItem : list) {
                    CorrDataEntityParam corrData = new CorrDataEntityParam();
                    BeanUtils.copyProperties(corrDataEntity, corrData);
                    List<BankReconciliation> bankReconciliations;
                    corrData.setBankReconciliationId(billClaimItem.getBankbill());
                    corrData.setAuto(false);
                    corrDataEntities.add(corrData);
                }
            }
        } else {
            corrDataEntities.add(corrDataEntity);
        }
        int ordernum = 1;
        for (CorrDataEntityParam corrRpc : corrDataEntities) {
            CorrDataEntity corr = new CorrDataEntity();
            BeanUtils.copyProperties(corrRpc, corr);
            if (flag == 1) {
                corrOperationService.corrOpration(corr, ordernum);
                ordernum++;
                reWriteBusCorrDataService.reWriteBankReconciliationData(corr, false);
            }
//            corrOperationService.corrOpration(corr);
//            reWriteBusCorrDataService.reWriteBankReconciliationData(corr,false);
            if (corr.getBillClaimItemId() != null) {
                Short associationStatus = 1;
                reWriteBusCorrDataService.reWriteBillClaimData(corr.getBillClaimItemId(), associationStatus,
                        corrDataEntity.getSmartcheckno(), ClaimCompleteType.RecePayGen.getValue());
            }
        }

        return "success";
    }

    /**
     * 自动生单 -- 回写对账单关联关系
     *
     * @param corrDataEntities
     * @throws Exception
     */
    @Override
    public String batchReWriteBankRecilicationCorrelation(List<CorrDataEntity> corrDataEntities) throws Exception {
        //用来封装智能对账勾兑码，存储单据ID和对应的智能对账码
        List<CtmJSONObject> smartCheckResult = new ArrayList<>();
        try {
            log.error(">>>>>回写对账单关联关系入参：{}", corrDataEntities.toString());
            List<Long> ids = new ArrayList();//存储对账单id
            List<BankReconciliationbusrelation_b> bankReconciliationbusrelation_bs = new ArrayList();//对账单关联实体
            if (corrDataEntities.isEmpty()) return null; //需回写关联数据为空.
            //用来封装智能对账勾兑码，存储单据ID和对应的智能对账码
            int inoutflag = 1; // 代理结算中心&统收统支模式标识
            //智能对账,关联数据添加勾兑码
//        String smartcheckno = UUID.randomUUID().toString().replace("-", "");
            //调用资金结算财资统一对账码接口生成
            String smartcheckno = RemoteDubbo.get(ReconciliateCodeGenerator.class, IDomainConstant.MDD_DOMAIN_STWB).generate();

            if (corrDataEntities.get(0).getBillClaimItemId() != null) {
                if (!checkPubts(corrDataEntities.get(0)))
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102578"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B000D4", "当前数据已被修改") /* "当前数据已被修改" */);
                //认领单判断是否是部分认领，然后获取已经存在的智能勾兑码
                BillClaim billClaim = MetaDaoHelper.findById(BillClaim.ENTITY_NAME, corrDataEntities.get(0).getBillClaimItemId());
                if (billClaim == null) {
                    inoutflag = 2;
                    List<BillClaim> list;
                    QuerySchema querySchema = QuerySchema.create().addSelect("*");
                    QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("refbill").eq(corrDataEntities.get(0).getBillClaimItemId()));
                    querySchema.addCondition(group);
                    list = MetaDaoHelper.queryObject(BillClaim.ENTITY_NAME, querySchema, null);
                    if (list != null && list.size() > 0)
                        billClaim = list.get(0);
                } else {
                    //根据id查询是否有参照单据id字段
                    if (StringUtils.isNotBlank(billClaim.getRefbill())) {
                        // 参照单据id不是空
                        inoutflag = 2;
                    }
                }
                if (billClaim != null) {
                    QuerySchema querySchema1 = QuerySchema.create().addSelect("*");
                    QueryConditionGroup group1 = QueryConditionGroup.and(QueryCondition.name("mainid").eq(billClaim.getId()));
                    querySchema1.addCondition(group1);
                    List<BillClaimItem> items = MetaDaoHelper.queryObject(BillClaimItem.ENTITY_NAME, querySchema1, null);
                    //部分认领时校验
                    if (billClaim != null && BillClaimType.Part.getValue() == billClaim.getClaimtype()) {
                        BillClaimItem claimItem = items.get(0);
                        if (claimItem != null) {
                            //获取关联银行对账单
                            BankReconciliation bankReconciliation = MetaDaoHelper.findById(BankReconciliation.ENTITY_NAME, claimItem.getBankbill());
                            if (bankReconciliation != null && StringUtils.isNotBlank(bankReconciliation.getSmartcheckno())) {
                                smartcheckno = bankReconciliation.getSmartcheckno();
                            }
                        }

                    }
                    //智能勾兑码赋值，整单认领和部分认领的时候取值，对账单上存在银行对账码，则智能勾兑码=银行对账码
                    if (billClaim != null && (BillClaimType.Part.getValue() == billClaim.getClaimtype() || BillClaimType.Whole.getValue() == billClaim.getClaimtype())) {
                        BillClaimItem claimItem = items.get(0);
                        if (claimItem != null) {
                            //获取关联银行对账单
                            BankReconciliation bankReconciliation = MetaDaoHelper.findById(BankReconciliation.ENTITY_NAME, claimItem.getBankbill());
                            //财资统一对账码取值调整
                            if (bankReconciliation != null && StringUtils.isNotBlank(bankReconciliation.getSmartcheckno())) {
                                smartcheckno = bankReconciliation.getSmartcheckno();
                            }
                        }
                    }

                    corrDataEntities.get(0).setSmartcheckno(smartcheckno);

                    //认领单回写
                    YtsContext.setYtsContext("rollbackbillclaimid", billClaim.getId());//装填需回滚数据
                    reWriteBusCorrDataService.reWriteBillClaimData(corrDataEntities.get(0).getBillClaimItemId(),
                            AssociationStatus.Associated.getValue(), corrDataEntities.get(0).getSmartcheckno(), ClaimCompleteType.RecePayGen.getValue());
                    if (inoutflag == 2 && "119".equals(corrDataEntities.get(0).getBillType())) {
                        // 如果是参照单据id，不回写银行对账单关联关系
                        CtmJSONObject result = new CtmJSONObject();
                        result.put("busid", corrDataEntities.get(0).getBusid());
                        result.put("smartcheckno", smartcheckno);
                        smartCheckResult.add(result);

                        return smartCheckResult.toString();
                    }
                    //组装需回写对账单数据
                    List<CorrDataEntity> corrDatas = new ArrayList<CorrDataEntity>();
                    List<BillClaimItem> list;
                    QuerySchema querySchema = QuerySchema.create().addSelect("*");
                    QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("mainid").eq(billClaim.getId()));
                    querySchema.addCondition(group);
                    list = MetaDaoHelper.queryObject(BillClaimItem.ENTITY_NAME, querySchema, null);
                    if (list != null && list.size() > 0) {
                        for (BillClaimItem billClaimItem : list) {//循环取出所有对账单id，拼装后进行回写
                            CorrDataEntity corrData = new CorrDataEntity();
                            BeanUtils.copyProperties(corrDataEntities.get(0), corrData);
                            List<BankReconciliation> bankReconciliations;
                            corrData.setBankReconciliationId(billClaimItem.getBankbill());
                            corrData.setAuto(false);
                            corrDatas.add(corrData);
                        }
                    }
                    corrDataEntities = corrDatas;
                } else {
                    // 若通过认领单ID未查询到认领单数据，则走银行对账单ID逻辑
                    // 认领单ID赋值给银行对账单ID
                    corrDataEntities.get(0).setBankReconciliationId(corrDataEntities.get(0).getBillClaimItemId());
                    // 认领单ID置为null
                    corrDataEntities.get(0).setBillClaimItemId(null);
                }
            }

            //智能勾兑码赋值
            if (corrDataEntities.get(0).getBankReconciliationId() != null) {
                BankReconciliation bankReconciliation = MetaDaoHelper.findById(BankReconciliation.ENTITY_NAME, corrDataEntities.get(0).getBankReconciliationId());
                //202428财资统一对账码调整
                if (bankReconciliation != null && StringUtils.isNotBlank(bankReconciliation.getSmartcheckno())) {
                    smartcheckno = bankReconciliation.getSmartcheckno();
                }
            }

            Boolean isAuto = corrDataEntities.get(0).getAuto();
            Map pubtsmap = new HashMap();
            for (CorrDataEntity corr : corrDataEntities) {
                // 如果是自动生单，则需要判断是否已存在关联单据
                boolean repeat = false;
                if (isAuto) {
                    CtmJSONObject conditioon = new CtmJSONObject();
                    conditioon.put("bankreconciliation", corr.getBankReconciliationId());
                    conditioon.put(ICmpConstant.BILLTYPE, corr.getBillType());
                    repeat = checkRepeatRelationsByCondition(conditioon);
                }
                //组装关联实体
                if (!repeat) {
                    bankReconciliationbusrelation_bs.add(getBankReconciliationbusrelation_b(corr));
                }
                /**
                 * 写入需判断对账单的pubts
                 * 1，如为认领单不需要校验对账单pubts
                 * 2，资金收付款单不需要校验对账单pubts
                 */
                if (corr.getBillClaimItemId() == null && !Short.valueOf(corr.getBillType()).equals(EventType.FundPayment.getValue()) && !Short.valueOf(corr.getBillType()).equals(EventType.FundCollection.getValue())) {
                    pubtsmap.put(corr.getBankReconciliationId(), corr.getBankReconciliationPubts());
                }
                //写入对账单id
                ids.add(corr.getBankReconciliationId());
            }
            //查询对账单
            SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            QuerySchema querySchema2 = QuerySchema.create().addSelect("*");
            QueryConditionGroup group2 = QueryConditionGroup.and(QueryCondition.name("id").in(ids));
            querySchema2.addCondition(group2);
            List<BankReconciliation> bankReconciliations = MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME, querySchema2, null);
            for (int i = 0; i < bankReconciliations.size(); i++) {
                Date date = bankReconciliations.get(i).getPubts();
                // 如果是自动生单，则是否已自动生单肯定为是
                if (isAuto) {
                    bankReconciliations.get(i).setIsautocreatebill(true);
                }
                BankReconciliation bankReconciliation1 = bankReconciliations.get(i);
                String pus = sf.format(date);
                //处理银行对账单前，对单据进行判断是否为最新状态
                if (pubtsmap.get(bankReconciliations.get(i).getId()) != null && !pubtsmap.get(bankReconciliations.get(i).getId()).equals(pus)) {
                    log.error("bank_seq_no is " + bankReconciliations.get(i).getBank_seq_no() + ", pus is " + pus + ", other is " + pubtsmap.get(bankReconciliations.get(i).getId()));
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102579"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B000D2", "单据已被修改"));
                }
                if (isAuto) {//自动生单 - 待确认   手动生单 - 已确认
                    bankReconciliations.get(i).setRelationstatus(Relationstatus.Confirm.getValue());
                    bankReconciliations.get(i).setAutoassociation(corrDataEntities.get(0).getAuto());
//                bankReconciliations.get(i).setAssociationstatus(AssociationStatus.Associated.getValue());
                } else {
                    //                下拨付款单或归集收款单生成虚拟流水
                    String billtype = corrDataEntities.get(0).getBillType();
                    if (billtype.equals(String.valueOf(EventType.Disburse_payment_slip.getValue())) || billtype.equals(String.valueOf(EventType.Collect_receipts.getValue()))) {
                        Boolean flag = bankReconciliation1.getIsvirtualflow() == null ? false : bankReconciliation1.getIsvirtualflow();
                        if (flag) {
                            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102581"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418059D", "该单据是虚拟流水单，请重新选择") /* "该单据是虚拟流水单，请重新选择" */);
                        }
                        checkIsunique(bankReconciliation1);
                        //判断是否开启
                        Boolean check = check(bankReconciliation1);
                        if (check) {
                            createPushBill(bankReconciliation1, billtype);
                        }
//                    if(!check){
                        bankReconciliations.get(i).setAutoassociation(corrDataEntities.get(0).getAuto());
//                    bankReconciliations.get(i).setAssociationstatus(AssociationStatus.Associated.getValue());
                        bankReconciliations.get(i).setRelationstatus(Relationstatus.Confirmed.getValue());
//                    }
                    } else {
                        bankReconciliations.get(i).setRelationstatus(Relationstatus.Confirmed.getValue());
                        bankReconciliations.get(i).setAutoassociation(corrDataEntities.get(0).getAuto());
//                    bankReconciliations.get(i).setAssociationstatus(AssociationStatus.Associated.getValue());
                    }
                }
                //智能对账：设置智能勾兑号,不区分自动生单;手动生单设置调用方入账类型，自动生单正常入账
                if (StringUtils.isEmpty(bankReconciliations.get(i).getSmartcheckno())) {
                    bankReconciliations.get(i).setSmartcheckno(smartcheckno);
                    // 财资统一码为空，流水支持处理方式 设置为 “生单或关联”
                    bankReconciliations.get(i).setIsparsesmartcheckno(BooleanUtils.toBoolean(ReconciliationSupportWayEnum.GENERATION_OR_ASSOCIATION.getValue()));
                }
                if (!isAuto) {
                    bankReconciliations.get(i).setEntrytype(bankReconciliations.get(i).getVirtualEntryType());
                } else {
                    bankReconciliations.get(i).setEntrytype(EntryType.Normal_Entry.getValue());
                }
                //提前入场场景处理
                Short associationcount = bankReconciliations.get(i).getAssociationcount();
                //关联次数不为空的时候直接将关联次数设置为2
                if (null != associationcount) {
                    bankReconciliations.get(i).setAssociationcount(AssociationCount.Second.getValue());
                } else {
                    bankReconciliations.get(i).setAssociationcount(AssociationCount.First.getValue());
                }
                bankReconciliations.get(i).setEntityStatus(EntityStatus.Update);

                //智能到账，对账单完结状态修改
                Short entrytype = bankReconciliations.get(i).getEntrytype();
                //如果为已关联 且 待认领金额为0 且 入账类型为一般入账或者冲挂账，则设置流水完结状态为已完结
                //未发布的直接修改
                if (!bankReconciliations.get(i).getIspublish()) {
                    if (bankReconciliations.get(i).getAmounttobeclaimed().compareTo(BigDecimal.ZERO) == 0 &&
                            (entrytype == null || entrytype == EntryType.CrushHang_Entry.getValue() || entrytype == EntryType.Normal_Entry.getValue())) {
//                    bankReconciliations.get(i).setSerialdealendstate(SerialdealendState.END.getValue());

                    }
                }
                //已发布的,判断是否存在其他未完结的认领单。存在则对账单完结状态不修改
                if (bankReconciliations.get(i).getIspublish()) {
                    QuerySchema billclaimSchema = QuerySchema.create().addSelect("id");
                    QueryConditionGroup billclaimGroup = QueryConditionGroup.and(
                            QueryCondition.name("id").not_eq(corrDataEntities.get(0).getBillClaimItemId()),
                            QueryCondition.name("items.bankbill").eq(bankReconciliations.get(i).getId()),
                            QueryCondition.name("associationstatus").not_eq(AssociationStatus.Associated.getValue())
                    );
                    billclaimSchema.addCondition(billclaimGroup);
                    List<BillClaim> billClaimList = MetaDaoHelper.queryObject(BillClaim.ENTITY_NAME, billclaimSchema, null);
                    if (CollectionUtils.isEmpty(billClaimList)) {
                        if (bankReconciliations.get(i).getAmounttobeclaimed().compareTo(BigDecimal.ZERO) == 0 &&
                                (entrytype == null || entrytype == EntryType.CrushHang_Entry.getValue() || entrytype == EntryType.Normal_Entry.getValue())) {
//                        bankReconciliations.get(i).setSerialdealendstate(SerialdealendState.END.getValue());
                        }
                    }
                }
                bankReconciliations.get(i).setSerialdealtype(ClaimCompleteType.RecePayGen.getValue());
                //发布待办消息：对方类型为客户或者供应商时,待办消息推送已办理
                handleConfirmMsg(bankReconciliations.get(i));
            }

            CommonSaveUtils.updateBankReconciliation(bankReconciliations);//回写银行对账单数据
            YtsContext.setYtsContext("bankReconciliationIds", ids);//装填需回滚数据

            //智能对账：封装回调勾兑号结果
            CtmJSONObject result = new CtmJSONObject();
            if (bankReconciliationbusrelation_bs.size() > 0) {
//            CmpMetaDaoHelper.insert(BankReconciliationbusrelation_b.ENTITY_NAME, bankReconciliationbusrelation_bs);//写入关联信息
                CommonSaveUtils.insertBankReconciliationbusrelation_b(bankReconciliationbusrelation_bs);
                YtsContext.setYtsContext("batchCorrData", bankReconciliationbusrelation_bs);//装填需回滚数据
                List corrDataIds = new ArrayList<>();
                bankReconciliationbusrelation_bs.stream().forEach(e -> {
                    corrDataIds.add(e.getId());
                });
                YtsContext.setYtsContext("corrDataIds", corrDataIds);//装填需回滚数据
                result.put("iscorrsuccess", true);
            }

            result.put("busid", corrDataEntities.get(0).getBusid());
            result.put("smartcheckno", smartcheckno);
            smartCheckResult.add(result);
        } catch (Exception e) {
            throw new Exception(InternationalUtils.getMessageWithDefault("UID:P_CM-BE_20093A2C04F80009", "回写关联关系异常！") + e.getMessage(), e);//@notranslate
        }
        return smartCheckResult.toString();
    }


    /**
     * 批量回写关联关系回滚接口
     *
     * @param corrDataEntities
     */
    @Override
    public void rollBackBatchReWriteBankRecilicationCorrelation(List<CorrDataEntity> corrDataEntities) throws Exception {
        try {
            //获取需要回滚的银行对账单 -- 关联关系表数据
            List corrDataIds = (List) YtsContext.getYtsContext("corrDataIds");
            if (Objects.isNull(corrDataIds) || corrDataIds.isEmpty()) return; //需回滚的数据为空
            //查询对账单
            QuerySchema querySchema1 = QuerySchema.create().addSelect("*");
            QueryConditionGroup group1 = QueryConditionGroup.and(QueryCondition.name("id").in(corrDataIds));
            querySchema1.addCondition(group1);
            List<BankReconciliationbusrelation_b> listbs = MetaDaoHelper.queryObject(BankReconciliationbusrelation_b.ENTITY_NAME, querySchema1, null);
            //删除关联表数据
            listbs.stream().forEach(e -> {
                e.setEntityStatus(EntityStatus.Delete);
            });
            //MetaDaoHelper.delete(BankReconciliationbusrelation_b.ENTITY_NAME, listbs);
            //数据库修改对账单数据
            //查询对账单
            List<Long> bankReconciliationIds = (List<Long>) YtsContext.getYtsContext("bankReconciliationIds");
            QuerySchema querySchema2 = QuerySchema.create().addSelect("*");
            QueryConditionGroup group2 = QueryConditionGroup.and(QueryCondition.name("id").in(bankReconciliationIds));
            querySchema2.addCondition(group2);
            List<BankReconciliation> bankReconciliations = MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME, querySchema2, null);
            //修改需回滚的对账单数据 -- 改为未关联
            bankReconciliations.stream().forEach(e -> {
                e.setEntityStatus(EntityStatus.Update);
                e.setRelationstatus(null);
                e.setEntrytype(null);
                e.setVirtualEntryType(null);
                if (AssociationStatus.Associated.getValue() == e.getAssociationstatus()) {
                    e.setAssociationstatus(AssociationStatus.NoAssociated.getValue());
                    e.setSerialdealtype(null);
                    //将银行对账单确认状态置空
                    e.setRelationstatus(null);
                    //关联次数清空
                    e.setAssociationcount(null);
                    e.setSerialdealendstate(null);
                }

            });
            CommonSaveUtils.updateBankReconciliation(bankReconciliations);
            CommonSaveUtils.batchDeleteBankReconciliationbusrelation_b(listbs);

            //回滚认领单
            if (YtsContext.getYtsContext("rollbackbillclaimid") != null) {
                Long billcalimid = (Long) YtsContext.getYtsContext("rollbackbillclaimid");
                BillClaim billClaim = MetaDaoHelper.findById(BillClaim.ENTITY_NAME, billcalimid);
                if (billClaim == null) {
                    List<BillClaim> list;
                    QuerySchema querySchema = QuerySchema.create().addSelect("*");
                    QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("refbill").eq(billcalimid));
                    querySchema.addCondition(group);
                    list = MetaDaoHelper.queryObject(BillClaim.ENTITY_NAME, querySchema, null);
                    if (list != null && list.size() >= 0)
                        billClaim = list.get(0);
                }
                billClaim.setAssociationstatus(AssociationStatus.NoAssociated.getValue());
                billClaim.setClaimcompletetype(null);
                billClaim.setAssociatedoperator(null);
                billClaim.setAssociateddate(null);
                billClaim.setRefassociationstatus(AssociationStatus.NoAssociated.getValue());
                billClaim.setEntityStatus(EntityStatus.Update);
                CommonSaveUtils.updateBillClaim(billClaim);
            }
        } catch (Exception e) {
            throw new CtmException("关联关系回写逻辑回滚异常，异常原因：" + e.getMessage(), e);// UID:P_CM-BE_1FED094604F80007//@notranslate
        }

    }

    /**
     * 批量回写关联关系回滚接口
     *
     * @param corrDataEntities
     */
    @Override
    public void rollBackBatchReWriteBankRecilicationForRpc(List<CorrDataEntityParam> corrDataEntities) throws Exception {

        /**
         *  1.如果billClaimIds不为空，代表是关联的是认领单
         *  2.
         *  3.对于新增的银行对账单关联关系表数据要进行删除
         *  4.
         */
        try {
            if (YtsContext.getYtsContext("billClaimId") != null) {
                Long billcalimid = (Long) YtsContext.getYtsContext("billClaimId");
                BillClaim billClaim = MetaDaoHelper.findById(BillClaim.ENTITY_NAME, billcalimid);
                // 如果没查到数据，可能是为参照关联，参照关联需要将参照关联状态改成未关联
                if (billClaim != null) {
                    if ("1".equals(YtsContext.getYtsContext("refbill"))) {
                        billClaim.setRefassociationstatus(AssociationStatus.NoAssociated.getValue());
                    }
                    billClaim.setAssociationstatus(AssociationStatus.NoAssociated.getValue());
                    billClaim.setClaimcompletetype(null);
                    billClaim.setAssociatedoperator(null);
                    billClaim.setAssociateddate(null);
                    billClaim.setEntityStatus(EntityStatus.Update);
                    CommonSaveUtils.updateBillClaim(billClaim);
                }
            }

            //获取需要回滚的银行对账单 -- 关联关系表数据
            List bankReconciliationbusrelation_bs_ids = (List) YtsContext.getYtsContext("bankReconciliationbusrelation_bs_ids");
            if (bankReconciliationbusrelation_bs_ids.isEmpty()) return; //需回滚的数据为空
            //查询对账单
            QuerySchema querySchema1 = QuerySchema.create().addSelect("*");
            QueryConditionGroup group1 = QueryConditionGroup.and(QueryCondition.name("id").in(bankReconciliationbusrelation_bs_ids));
            querySchema1.addCondition(group1);
            List<BankReconciliationbusrelation_b> listbs = MetaDaoHelper.queryObject(BankReconciliationbusrelation_b.ENTITY_NAME, querySchema1, null);
            //删除关联表数据
            List<Long> bankReconciliationbusrelation_ids = new ArrayList<>();
            listbs.stream().forEach(e -> {
                e.setEntityStatus(EntityStatus.Delete);
                bankReconciliationbusrelation_ids.add(e.getBankreconciliation());
            });
            //MetaDaoHelper.delete(BankReconciliationbusrelation_b.ENTITY_NAME, listbs);

            //数据库修改对账单数据
            //查询对账单
            QuerySchema querySchema2 = QuerySchema.create().addSelect("*");
            QueryConditionGroup group2 = QueryConditionGroup.and(QueryCondition.name("id").in(bankReconciliationbusrelation_ids));
            querySchema2.addCondition(group2);
            List<BankReconciliation> bankReconciliations = MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME, querySchema2, null);
            //修改需回滚的对账单数据 -- 改为未关联
            bankReconciliations.stream().forEach(e -> {
                e.setEntityStatus(EntityStatus.Update);
                e.setRelationstatus(null);
                e.setVirtualEntryType(null);
                /*e.setAssociationstatus(AssociationStatus.NoAssociated.getValue());*/
                //智能到账，完结状态要改成未完结
                /*e.setSerialdealendstate(SerialdealendState.UNEND.getValue());*/
                e.setSerialdealtype(null);
                e.setGenertbilltype(null);
            });
            CommonSaveUtils.updateBankReconciliation(bankReconciliations);
            CommonSaveUtils.batchDeleteBankReconciliationbusrelation_b(listbs);
        } catch (Exception e) {
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21A7F3620538001C", "关联关系回写逻辑回滚异常，异常原因：") /* "关联关系回写逻辑回滚异常，异常原因：" */ + e.getMessage(), e);//UID:P_CM-BE_1FED094604F80007
        }
    }

    /**
     * 删除关联关系，全部调用
     *
     * @param json
     * @return
     */
    @Override
    @Transactional
    public CommonResponseDataVo resDelDataForRpc(CommonRequestDataVo json) throws Exception {
        log.error("resDelDataForRpc param is " + CtmJSONObject.toJSONString(json));
        CommonResponseDataVo resJson = new CommonResponseDataVo();
        Long busid = json.getBusid() == null ? null : Long.valueOf(json.getBusid());//银行对账单id
        Long stwbbusid = json.getStwbbusid();//业务单据id 资金结算单
        Long claimid = json.getClaimId() == null ? null : Long.valueOf(json.getClaimId());//认领单id
        if (busid == null && claimid == null) {
            throw new CtmException(InternationalUtils.getMessageWithDefault("UID:P_CM-BE_2009378204F80000", "删除关联关系失败，流水或认领单id为空！"));//@notranslate
        }
        List<BankReconciliationbusrelation_b> bs = new ArrayList<>();
        try {
            //关联认领单
            if (claimid != null) {
                // 生成新的财资统一码
                String smartCheckNo = RemoteDubbo.get(ReconciliateCodeGenerator.class, IDomainConstant.MDD_DOMAIN_STWB).generate();
                //回写认领单
                QuerySchema bquery = QuerySchema.create().addSelect("*");
                QueryConditionGroup querygroup = QueryConditionGroup.and(QueryCondition.name("id").eq(claimid));
                bquery.addCondition(querygroup);
                List<BillClaim> billClaims = MetaDaoHelper.queryObject(BillClaim.ENTITY_NAME, bquery, null);
                if (CollectionUtils.isEmpty(billClaims)) {
                    QuerySchema bquery2 = QuerySchema.create().addSelect("*");
                    QueryConditionGroup querygroup2 = QueryConditionGroup.and(QueryCondition.name("refbill").eq(claimid));
                    bquery2.addCondition(querygroup2);
                    billClaims = MetaDaoHelper.queryObject(BillClaim.ENTITY_NAME, bquery2, null);
                    //更新实际认领单位认领完结状态
                    if (CollectionUtils.isNotEmpty(billClaims)) {
                        //CZFW-455483【DSP支持问题】结算中心代理模式下的认领单生成资金收付款单据并且流程走完后，又将资金收付款单据撤回来，此时单据还在但是认领单还有生单按钮，会导致重复生单（见附件）
                        billClaims.get(0).setRefassociationstatus(null);
                        // 结算中心代理模式不更新认领单的财资统一码
                        billClaims.get(0).setSettlestatus(null);
                        billClaims.get(0).setEntityStatus(EntityStatus.Update);
                        CommonSaveUtils.updateBillClaim(billClaims.get(0));
                        resJson.setCode("200");
                        resJson.setMessage(InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805A0", "执行成功") /* "执行成功" */);//@notranslate
                        return resJson;
                    } else {
                        // 传递的认领单ID未查询到对应数据，则进入银行对账单ID逻辑， 将认领单ID传递给银行对账单ID逻辑
                        this.handleBusId(claimid, bs, stwbbusid, json, null);
                    }
                } else {
                    //首先判断认领单的认领类型
                    //如果是部分认领 首先通过认领单子表获取到银行对账单，然后获取银行对账单的关联关系，查询关联关系表中数据的billid，判断是否包含stwbbusid
                    //如果包含，则删除关联stwbbusid的关联关系数据，并且修改银行对账单和认领单的关联状态为未关联，清空勾兑码；如果不是，则直接返回。
                    BillClaim billClaim = billClaims.get(0);
                    if (billClaim.getClaimtype() != null && billClaim.getClaimtype() == BillClaimType.Part.getValue()) {
                        handleDeleteClaim4PartClaim(billClaim, stwbbusid, bs, smartCheckNo);
                    } else {
                        handleDeleteClaim4OtherClaim(billClaim, stwbbusid, bs, smartCheckNo);
                    }
                    //业务日志
                    CtmJSONObject logparam = new CtmJSONObject();
                    logparam.put("message", "认领单下游单据关联关系删除");//@notranslate
                    logparam.put("reqVo", json);
                    logparam.put("billclaimInfo", billClaim);
                    ctmcmpBusinessLogService.saveBusinessLog(logparam, billClaim.getCode(), "认领单关联关系删除", IServicecodeConstant.CMPBANKRECONCILIATION, "银行流水认领", "认领单关联关系删除接口调用");//@notranslate
                }
            } else {
                //银行对账单
                this.handleBusId(busid, bs, stwbbusid, json, null);
            }
        } catch (Exception e) {
            log.error("删除回写报错：：" + e, e);
            throw new CtmException(InternationalUtils.getMessageWithDefault("UID:P_CM-BE_200935B004F80005", "删除关联关系报错!") + e.getMessage(), e);//@notranslate
        }
        resJson.setCode("200");
        resJson.setMessage(InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805A0", "执行成功"));//@notranslate
        return resJson;
    }

    /**
     * @param billClaim
     * @param stwbbusid
     * @param bs
     * @param smartCheckNo
     * @throws Exception
     */
    private void handleDeleteClaim4PartClaim(BillClaim billClaim, Long stwbbusid, List<BankReconciliationbusrelation_b> bs, String smartCheckNo) throws Exception {
        try {
            QuerySchema querySchema = QuerySchema.create().addSelect("*");
            QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("mainid").eq(billClaim.getId()));
            querySchema.addCondition(group);
            List<BillClaimItem> claimItemList = MetaDaoHelper.queryObject(BillClaimItem.ENTITY_NAME, querySchema, null);
            if (CollectionUtils.isNotEmpty(claimItemList)) {
                //银行对账单id
                Long bankreconciliationId = claimItemList.get(0).getBankbill();
                //根据银行对账单id查询到银行对账单的关联关系数据
                QuerySchema querySchema1 = QuerySchema.create().addSelect("*");//业务单据子表id、单据类型
                QueryConditionGroup group1 = QueryConditionGroup.and(QueryConditionGroup.and(QueryCondition.name("billid").eq(stwbbusid)));
                querySchema1.addCondition(group1);
                List<BankReconciliationbusrelation_b> bankReconciliationbusrelation_bs = MetaDaoHelper.queryObject(BankReconciliationbusrelation_b.ENTITY_NAME, querySchema1, null);
                List<String> deleteIds = new ArrayList<>();
                if (CollectionUtils.isNotEmpty(bankReconciliationbusrelation_bs)) {
                    //判断银行对账单关联关系数据中的billid中是否包含stwbbusid，如果包含，则进行删除次关联关系，并且
                    for (BankReconciliationbusrelation_b b : bankReconciliationbusrelation_bs) {
                        b.setEntityStatus(EntityStatus.Delete);
                        bs.add(b);
                        deleteIds.add(b.getId().toString());
                        //同时修改认领单的关联状态
                        billClaim.setAssociationstatus(delcorrStatus);
                        billClaim.setClaimcompletetype(null);
                        billClaim.setAssociatedoperator(null);
                        billClaim.setAssociateddate(null);
                        billClaim.setSettlestatus(null);
                        billClaim.setEntityStatus(EntityStatus.Update);
                        //智能对账：认领单，智能勾兑码重新生成
                        YtsContext.setYtsContext("billclaim" + billClaim.getId(), billClaim.getSmartcheckno());
                        billClaim.setSmartcheckno(smartCheckNo);
                    }
                    //修改 银行对账单 的关联状态为未关联，清空勾兑码
                    QuerySchema querySchema2 = QuerySchema.create().addSelect("*");
                    QueryConditionGroup group2 = QueryConditionGroup.and(QueryCondition.name("id").eq(bankreconciliationId));
                    querySchema2.addCondition(group2);
                    List<BankReconciliation> bankReconciliations = MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME, querySchema2, null);
                    if (CollectionUtils.isNotEmpty(bankReconciliations)) {
                        //部分认领，只要删除一个，则银行对账单一定是未完结
                        bankReconciliations.get(0).setSerialdealendstate(SerialdealendState.UNEND.getValue());
                        bankReconciliations.get(0).setSerialdealtype(null);
                        bankReconciliations.get(0).setGenertbilltype(null);
                        bankReconciliations.get(0).setEntityStatus(EntityStatus.Update);
                        //根据银行对账单id查询到银行对账单的关联关系数据
                        QuerySchema query = QuerySchema.create().addSelect("*");//业务单据子表id、单据类型
                        QueryConditionGroup gro = QueryConditionGroup.and(QueryCondition.name("bankreconciliation").eq(bankreconciliationId));
                        query.addCondition(gro);
                        List<BankReconciliationbusrelation_b> db_bs = MetaDaoHelper.queryObject(BankReconciliationbusrelation_b.ENTITY_NAME, query, null);
                        if (!db_bs.isEmpty()) {// 筛选出不在 deleteList 中的数据
                            List<BankReconciliationbusrelation_b> remainingList = db_bs.stream()
                                    .filter(b -> !deleteIds.contains(b.getId().toString()))
                                    .collect(Collectors.toList());
                            if (remainingList.isEmpty()) {
                                bankReconciliations.get(0).setAssociationstatus(delcorrStatus);
                                //智能对账：银行对账单，智能勾兑码删除
                                YtsContext.setYtsContext("bank" + bankReconciliations.get(0).getId(), bankReconciliations.get(0).getSmartcheckno());
                                bankReconciliations.get(0).setSmartcheckno(smartCheckNo);
                                //将银行对账单确认状态置空
                                bankReconciliations.get(0).setRelationstatus(null);
                            }
                        }
                        YtsContext.setYtsContext("corrData", bs);
                        CommonSaveUtils.updateBankReconciliation(bankReconciliations.get(0));
                        CommonSaveUtils.updateBillClaim(billClaim);
                        MetaDaoHelper.delete(BankReconciliationbusrelation_b.ENTITY_NAME, bs);
                    }
                } else {
                    throw new CtmException(InternationalUtils.getMessageWithDefault("UID:P_CM-BE_200936F004F80009", "根据单据id未查询到对应关联关系，请检查数据！"));//@notranslate
                }
            }
        } catch (Exception e) {
            log.error("处理部分认领类型异常！", e);
            throw new CtmException(InternationalUtils.getMessageWithDefault("UID:P_CM-BE_2009365204F80007", "处理部分认领类型异常！") + e.getMessage(), e);//@notranslate
        }
    }

    /**
     * 处理其他认领类型
     *
     * @param billClaim
     * @param stwbbusid
     * @param bs
     * @param smartCheckNo
     * @throws Exception
     */
    private void handleDeleteClaim4OtherClaim(BillClaim billClaim, Long stwbbusid, List<BankReconciliationbusrelation_b> bs, String smartCheckNo) throws Exception {
        try {
            //如果是整单认领和合并认领，首先通过认领单子表获取到银行对账单，然后获取银行对账单的关联关系，然后查询关联关系数据中的第一条数据中的业务单据类型billtype
            //如果是资金结算单，则进行全部删除关联关系数据，并修改认领单的关联状态为未关联，清空勾兑码；如果不是，则直接返回。
            //根据认领单id查认领单子表id和以及子表中存的银行对账单id
            List<BankReconciliation> brs = new ArrayList<>();
            QuerySchema querySchema = QuerySchema.create().addSelect("*");
            QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("mainid").eq(billClaim.getId()));
            querySchema.addCondition(group);
            List<BillClaimItem> claimItemList = MetaDaoHelper.queryObject(BillClaimItem.ENTITY_NAME, querySchema, null);
            if (CollectionUtils.isNotEmpty(claimItemList)) {
                for (BillClaimItem item : claimItemList) {
                    //银行对账单id
                    Long bankreconciliationId = item.getBankbill();
                    //根据银行对账单id查询到银行对账单的关联关系数据
                    QuerySchema querySchema1 = QuerySchema.create().addSelect("*");//业务单据子表id、单据类型
                    QueryConditionGroup group1 = QueryConditionGroup.and(QueryCondition.name("billid").eq(stwbbusid), QueryCondition.name("bankreconciliation").eq(bankreconciliationId));
                    querySchema1.addCondition(group1);
                    List<BankReconciliationbusrelation_b> bankReconciliationbusrelation_bs = MetaDaoHelper.queryObject(BankReconciliationbusrelation_b.ENTITY_NAME, querySchema1, null);
                    if (CollectionUtils.isNotEmpty(bankReconciliationbusrelation_bs)) {
                        for (BankReconciliationbusrelation_b b : bankReconciliationbusrelation_bs) {
                            b.setEntityStatus(EntityStatus.Delete);
                            bs.add(b);
                            // 修改 认领单 的关联状态为未关联，清空勾兑码
                            billClaim.setAssociationstatus(AssociationStatus.NoAssociated.getValue());
                            billClaim.setClaimcompletetype(null);
                            billClaim.setAssociatedoperator(null);
                            billClaim.setAssociateddate(null);
                            billClaim.setEntityStatus(EntityStatus.Update);
                            //智能对账：认领单，智能勾兑码删除
                            YtsContext.setYtsContext("billclaim" + billClaim.getId(), billClaim.getSmartcheckno());
                            billClaim.setSmartcheckno(smartCheckNo);
                        }
                        //修改 银行对账单 的关联状态为未关联，清空勾兑码
                        QuerySchema querySchema2 = QuerySchema.create().addSelect("*");
                        QueryConditionGroup group2 = QueryConditionGroup.and(QueryCondition.name("id").eq(bankreconciliationId));
                        querySchema2.addCondition(group2);
                        List<BankReconciliation> bankReconciliations = MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME, querySchema2, null);
                        for (BankReconciliation bankReconciliation : bankReconciliations) {
                            Short entryType = bankReconciliation.getEntrytype();
                            //认领单删除,银行对账单一定是未完结
                            bankReconciliation.setSerialdealendstate(SerialdealendState.UNEND.getValue());
                            bankReconciliation.setSerialdealtype(null);
                            bankReconciliation.setGenertbilltype(null);
                            if (entryType != null && EntryType.CrushHang_Entry.getValue() == entryType) {
                                bankReconciliation.setAssociationstatus(AssociationStatus.Associated.getValue());
                            } else {
                                bankReconciliation.setAssociationstatus(AssociationStatus.NoAssociated.getValue());
                                //财资统一对账码非解析来的重新生成
                                if (!bankReconciliation.getIsparsesmartcheckno()) {
                                    bankReconciliation.setSmartcheckno(smartCheckNo);
                                }
                                bankReconciliation.setSerialdealtype(null);
                                //将银行对账单确认状态置空
                                bankReconciliation.setRelationstatus(null);
                                //关联次数清空
                                bankReconciliation.setAssociationcount(null);
                            }
                            bankReconciliation.setEntityStatus(EntityStatus.Update);
                            //智能对账：银行对账单，智能勾兑码删除
                            YtsContext.setYtsContext("bank" + bankReconciliations.get(0).getId(), bankReconciliations.get(0).getSmartcheckno());
                            YtsContext.setYtsContext("corrData", bs);
                            brs.add(bankReconciliation);
                        }
                    }
                }
            }
            CommonSaveUtils.updateBankReconciliation(brs);
            MetaDaoHelper.delete(BankReconciliationbusrelation_b.ENTITY_NAME, bs);
            CommonSaveUtils.updateBillClaim(billClaim);
        } catch (Exception e) {
            log.error("处理其他认领类型异常！", e);
            throw new CtmException(InternationalUtils.getMessageWithDefault("UID:P_CM-BE_2009392004280007", "处理其他认领类型异常！") + e.getMessage(), e);//@notranslate
        }
    }

    /**
     * 处理银行流水关联关系
     *
     * @param bankreconciliationId
     * @param bs
     * @param stwbbusid
     * @return
     * @throws Exception
     */
    private boolean handleBusId(Long bankreconciliationId, List<BankReconciliationbusrelation_b> bs, Long stwbbusid, CommonRequestDataVo json, Map<Long, BankReconciliation> originBankReconciliationMap) throws Exception {
        List<BankReconciliation> originBankReconciliation = null;
        if (MapUtils.isEmpty(originBankReconciliationMap)) {
            //查询操作之前的流水完结状态
            QuerySchema querySchemaBank = QuerySchema.create().addSelect("*");
            QueryConditionGroup groupBank = QueryConditionGroup.and(QueryCondition.name("id").eq(bankreconciliationId));
            querySchemaBank.addCondition(groupBank);
            originBankReconciliation = MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME, querySchemaBank, null);
        } else {
            originBankReconciliation = new ArrayList<>();
            originBankReconciliation.add(originBankReconciliationMap.get(bankreconciliationId));
        }
        QuerySchema querySchema1 = QuerySchema.create().addSelect("*");
        QueryConditionGroup group1 = QueryConditionGroup.and(QueryCondition.name("bankreconciliation").eq(bankreconciliationId));
        querySchema1.addCondition(group1);
        List<BankReconciliationbusrelation_b> bankReconciliationbusrelation_bs = MetaDaoHelper.queryObject(BankReconciliationbusrelation_b.ENTITY_NAME, querySchema1, null);
        List<BankReconciliationbusrelation_b> deleteList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(bankReconciliationbusrelation_bs)) {
            for (BankReconciliationbusrelation_b b : bankReconciliationbusrelation_bs) {
                if (json.getQueryDataForMap() != null && CommonParametersUtils.BANK_RECONCILIATION_DELETE_VALUE.equals(json.getQueryDataForMap().get(CommonParametersUtils.BANK_RECONCILIATION_DELETE_KEY))) {
                    b.setEntityStatus(EntityStatus.Delete);
                    deleteList.add(b);
                } else if (stwbbusid.equals(b.getBillid())) {
                    b.setEntityStatus(EntityStatus.Delete);
                    deleteList.add(b);
                }
            }
            YtsContext.setYtsContext("corrData", bs);
        } else {
            return true;
        }

        //修改资金付款单明细表关联状态
        //CZFW-370193问题修复，注释下边代码。搞不懂为啥会在外领域单据的删除接口来处理资金收付款单关联关系
//        updateFundPaymentItemsByBankReconciliationId(busid);
//        updateFundCollectionItemsByBankReconciliationId(busid);

        if (CollectionUtils.isNotEmpty(originBankReconciliation)) {
            BankReconciliation bankReconciliation = originBankReconciliation.get(0);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("update cmp_bankreconciliation set pubts=pubts");
            //取消关联时，判定流水上的确认状态，如为“关联确认”，则清空确认状态、账户使用组织
            if (bankReconciliation.getConfirmstatus() != null && ConfirmStatusEnum.RelationConfirmed.getIndex().equals(bankReconciliation.getConfirmstatus())) {
//                bankReconciliation.setConfirmstatus(ConfirmStatusEnum.Confirming.getIndex());
//                bankReconciliation.setAccentity(null);
//                //对方类型重置为其他
//                bankReconciliation.setOppositetype(OppositeType.Other.getValue());
//                bankReconciliation.setOppositeobjectid(null);
//                bankReconciliation.setOppositeobjectname(null);
//                bankReconciliation.setRelationstatus(null);

                stringBuilder.append(",confirmstatus=" + ConfirmStatusEnum.Confirming.getIndex());
                stringBuilder.append(",accentity=null");
                stringBuilder.append(",oppositetype=" + OppositeType.Other.getValue());
                stringBuilder.append(",oppositeobjectid=null");
                stringBuilder.append(",oppositeobjectname=null");
                stringBuilder.append(",relationstatus=null");
            }
            //除了资金收付款单其他领域没有回写associationcount业务关联次数这里兼容下如果为空的话兼容下
            if (bankReconciliation.getEntrytype() != null) {
                if (bankReconciliation.getEntrytype() == EntryType.Normal_Entry.getValue()) {//正常入账
//                    bankReconciliation.setAssociationstatus(AssociationStatus.NoAssociated.getValue());
//                    bankReconciliation.setVirtualEntryType(null);
//                    bankReconciliation.setSerialdealtype(null);
//                    bankReconciliation.setGenertbilltype(null);
//                    bankReconciliation.setEntrytype(null);
//                    bankReconciliation.setRelationstatus(null);

                    stringBuilder.append(",associationstatus=" + AssociationStatus.NoAssociated.getValue());
                    stringBuilder.append(",virtualEntryType=null");
                    stringBuilder.append(",serialdealtype=null");
                    stringBuilder.append(",genertbilltype=null");
                    stringBuilder.append(",entrytype=null");
                    stringBuilder.append(",relationstatus=null");
                } else if (bankReconciliation.getEntrytype() == EntryType.Hang_Entry.getValue()) {//挂账
//                    bankReconciliation.setAssociationstatus(AssociationStatus.NoAssociated.getValue());
//                    bankReconciliation.setIsadvanceaccounts(false);
//                    bankReconciliation.setSerialdealtype(null);
//                    bankReconciliation.setVirtualEntryType(null);
//                    bankReconciliation.setGenertbilltype(null);
//                    bankReconciliation.setEntrytype(null);
//                    bankReconciliation.setAssociationcount(null);
//                    bankReconciliation.setRelationstatus(null);

                    stringBuilder.append(",associationstatus=" + AssociationStatus.NoAssociated.getValue());
                    stringBuilder.append(",isadvanceaccounts=false");
                    stringBuilder.append(",serialdealtype=null");
                    stringBuilder.append(",virtualEntryType=null");
                    stringBuilder.append(",genertbilltype=null");
                    stringBuilder.append(",entrytype=null");
                    stringBuilder.append(",associationcount=null");
                    stringBuilder.append(",relationstatus=null");

                } else if (bankReconciliation.getEntrytype() == EntryType.CrushHang_Entry.getValue()) {//冲挂账
                    // 如果当前流水为冲挂账，但是删除的单据为挂账，则需要报错
                    if (json.getQueryDataForMap() != null) {
                        Map<String, Object> queryDataForMap = json.getQueryDataForMap();
                        if (queryDataForMap.get("entrytype") != null) {
                            Short entryType = Short.valueOf(queryDataForMap.get("entrytype").toString());
                            if (entryType == EntryType.Hang_Entry.getValue()) {
                                throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21A7F3620538001F", "当前流水为冲挂账，但是删除的单据为挂账，请检查数据！若流水为提前入账且已发布，请先取消发布！") /* "当前流水为冲挂账，但是删除的单据为挂账，请检查数据！若流水为提前入账且已发布，请先取消发布！" */);
                            }
                        }
                    }
//                    bankReconciliation.setAssociationcount(null);
//                    bankReconciliation.setVirtualEntryType(EntryType.Hang_Entry.getValue());
//                    bankReconciliation.setEntrytype(EntryType.Hang_Entry.getValue());
//                    bankReconciliation.setSerialdealtype(null);
//                    bankReconciliation.setAssociationcount(AssociationCount.First.getValue());
//                    bankReconciliation.setRelationstatus(null);
                    stringBuilder.append(",associationcount=null");
                    stringBuilder.append(",virtualEntryType=" + EntryType.Hang_Entry.getValue());
                    stringBuilder.append(",entrytype=" + EntryType.Hang_Entry.getValue());
                    stringBuilder.append(",serialdealtype=null");
                    stringBuilder.append(",associationcount=" + AssociationCount.First.getValue());
                    stringBuilder.append(",relationstatus=null");
                }
            } else {
//                bankReconciliation.setAssociationstatus(AssociationStatus.NoAssociated.getValue());
//                bankReconciliation.setVirtualEntryType(null);
//                bankReconciliation.setSerialdealtype(null);
//                bankReconciliation.setGenertbilltype(null);
//                bankReconciliation.setEntrytype(null);
//                bankReconciliation.setRelationstatus(null);
                stringBuilder.append(",associationstatus=" + AssociationStatus.NoAssociated.getValue());
                stringBuilder.append(",virtualEntryType=null");
                stringBuilder.append(",serialdealtype=null");
                stringBuilder.append(",genertbilltype=null");
                stringBuilder.append(",entrytype=null");
                stringBuilder.append(",relationstatus=null");
            }

            // 智能流水场景
//            Short processstatus = bankReconciliation.getProcessstatus();
//            if (DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_PROCESS_FINISH.getStatus().equals(processstatus) || DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_MATCH_PENDING.getStatus().equals(processstatus)) {
//                //bankReconciliation.setProcessstatus(DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_MATCH_NO_START.getStatus());
//                stringBuilder.append(",processstatus=" + DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_MATCH_NO_START.getStatus());
//            }

            //智能对账：银行对账单，智能勾兑码删除
            //财资统一对账码非解析来的可以清空
            if (!bankReconciliation.getIsparsesmartcheckno()) {
                String smartCheckNo = RemoteDubbo.get(ReconciliateCodeGenerator.class, IDomainConstant.MDD_DOMAIN_STWB).generate();
                bankReconciliation.setSmartcheckno(smartCheckNo);
                stringBuilder.append(",smartcheckno='" + smartCheckNo + "'");
            }
            //bankReconciliation.setEntityStatus(EntityStatus.Update);
            //bankReconciliation.setSerialdealendstate(SerialdealendState.UNEND.getValue());
            stringBuilder.append(",serialdealendstate=" + SerialdealendState.UNEND.getValue());
            // MetaDaoHelper.update(BankReconciliation.ENTITY_NAME, bankReconciliation);
            stringBuilder.append(" where associationstatus = " + AssociationStatus.Associated.getValue());
            stringBuilder.append(" and id = ? and ytenant_id = ?");
            SQLParameter params = new SQLParameter();
            params.addParam(bankReconciliation.getId().toString());
            params.addParam(InvocationInfoProxy.getTenantid());
            //删除子表
            MetaDaoHelper.delete(BankReconciliationbusrelation_b.ENTITY_NAME, deleteList);
            //更新主表状态
            int count = ymsJdbcApi.update(stringBuilder.toString(), params);
            if (count == 0 && bankReconciliation.getAssociationstatus() == AssociationStatus.Associated.getValue()) {
                throw new CtmException(String.format("当前银行流水号为%s不是最新状态，请刷新后重试！", bankReconciliation.getBank_seq_no())); //@notranslate
            }
            YtsContext.setYtsContext("bank" + originBankReconciliation.get(0).getId(), originBankReconciliation.get(0).getSmartcheckno());
            YtsContext.setYtsContext("bankSerialdealendstate" + originBankReconciliation.get(0).getId(), originBankReconciliation.get(0).getSerialdealendstate());
            //业务日志
            CtmJSONObject logparam = new CtmJSONObject();
            logparam.put("message", "银行流水下游单据关联关系删除");//@notranslate
            logparam.put("reqVo", json);
            logparam.put("bankreconciliationInfo", bankReconciliation);
            ctmcmpBusinessLogService.saveBusinessLog(logparam, bankReconciliation.getBank_seq_no(), "银行流水关联关系删除", IServicecodeConstant.CMPBANKRECONCILIATION, "银行流水认领", "银行流水关联关系删除接口调用");//@notranslate
        }
        return false;
    }

    @Override
    public CommonResponseDataVo rollbackDelDataForRpc(CommonRequestDataVo json) throws Exception {
        CommonResponseDataVo resJson = new CommonResponseDataVo();
        //兼容各领域回传的对账单id或者认领单id为空的情况
        Long busid = json.getBusid() != null ? Long.valueOf(json.getBusid()) : null;
        Long claimid = json.getClaimId() != null ? Long.valueOf(json.getClaimId()) : null;

        //Long busid = Long.valueOf(json.getBusid());
        //Long claimid = Long.valueOf(json.getClaimid());
        if (busid == null && claimid == null) {
            resJson.setCode("410");//请求参数为空
            return resJson;
        }
        try {
            List<BankReconciliationbusrelation_b> listbs = (List<BankReconciliationbusrelation_b>) YtsContext.getYtsContext("corrData");

            if (claimid != null) {//关联认领单

                //回写认领单
                List<BillClaim> billClaims;
                QuerySchema bquery = QuerySchema.create().addSelect("*");
                QueryConditionGroup querygroup = QueryConditionGroup.and(QueryCondition.name("id").eq(claimid));
                bquery.addCondition(querygroup);
                billClaims = MetaDaoHelper.queryObject(BillClaim.ENTITY_NAME, bquery, null);
                if (billClaims != null && billClaims.size() > 0) {
                    billClaims.get(0).setAssociationstatus(delcorrRowStatus);
                    billClaims.get(0).setEntityStatus(EntityStatus.Update);
                    //智能对账：回滚认领单的智能勾兑码
                    String smartcheckno = YtsContext.getYtsContext("billclaim" + billClaims.get(0).getId()).toString();
                    billClaims.get(0).setSmartcheckno(smartcheckno);
                    CommonSaveUtils.updateBillClaim(billClaims.get(0));
                } else {
                    // 结算中心代理模式下 CZFW-191992
                    QuerySchema bquery2 = QuerySchema.create().addSelect("*");
                    QueryConditionGroup querygroup2 = QueryConditionGroup.and(QueryCondition.name("refbill").eq(claimid));
                    bquery2.addCondition(querygroup2);
                    billClaims = MetaDaoHelper.queryObject(BillClaim.ENTITY_NAME, bquery2, null);
                    if (billClaims != null && billClaims.size() > 0) {
                        billClaims.get(0).setRefassociationstatus(AssociationStatus.NoAssociated.getValue());
                        billClaims.get(0).setEntityStatus(EntityStatus.Update);
                        //智能对账：回滚认领单的智能勾兑码
                        String smartcheckno = YtsContext.getYtsContext("billclaim" + billClaims.get(0).getId()).toString();
                        billClaims.get(0).setSmartcheckno(smartcheckno);
                        CommonSaveUtils.updateBillClaim(billClaims.get(0));
                    }
                }

                //查找子表数据取出对账单id
                List<BillClaimItem> list;
                QuerySchema querySchema = QuerySchema.create().addSelect("*");
                QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("mainid").eq(billClaims.get(0).getId()));
                querySchema.addCondition(group);
                list = MetaDaoHelper.queryObject(BillClaimItem.ENTITY_NAME, querySchema, null);
                if (list != null && list.size() > 0) {
                    for (BillClaimItem bill : list) {
                        busid = bill.getBankbill();
                        QuerySchema querySchema2 = QuerySchema.create().addSelect("*");
                        QueryConditionGroup group2 = QueryConditionGroup.and(QueryCondition.name("id").eq(busid));
                        querySchema2.addCondition(group2);
                        List<BankReconciliation> bankReconciliations = MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME, querySchema2, null);
                        if (bankReconciliations != null && bankReconciliations.size() > 0) {
                            /*bankReconciliations.get(0).setAssociationstatus(delcorrRowStatus);*/
                            bankReconciliations.get(0).setEntityStatus(EntityStatus.Update);
                            //智能对账：回滚银行对账单的智能勾兑码
                            String smartcheckno = YtsContext.getYtsContext("bank" + bankReconciliations.get(0).getId()) == null ? null : YtsContext.getYtsContext("bank" + bankReconciliations.get(0).getId()).toString();
                            Short serialdealendstate = YtsContext.getYtsContext("bankSerialdealendstate" + bankReconciliations.get(0).getId()) == null ?
                                    SerialdealendState.UNEND.getValue() : Short.parseShort(YtsContext.getYtsContext("bankSerialdealendstate" + bankReconciliations.get(0).getId()).toString());
                            bankReconciliations.get(0).setSmartcheckno(smartcheckno);
                            /*bankReconciliations.get(0).setSerialdealendstate(serialdealendstate);*/
                            CommonSaveUtils.updateBankReconciliation(bankReconciliations.get(0));
                        }
                    }
                }
            } else {

                QuerySchema querySchema2 = QuerySchema.create().addSelect("*");
                QueryConditionGroup group2 = QueryConditionGroup.and(QueryCondition.name("id").eq(busid));
                querySchema2.addCondition(group2);
                List<BankReconciliation> bankReconciliations = MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME, querySchema2, null);
                if (bankReconciliations != null && bankReconciliations.size() > 0) {
                    /*bankReconciliations.get(0).setAssociationstatus(delcorrRowStatus);*/
                    Short entryType = bankReconciliations.get(0).getEntrytype();
                    if (entryType != null && EntryType.CrushHang_Entry.getValue() == entryType) {
                        bankReconciliations.get(0).setEntrytype(EntryType.Hang_Entry.getValue());
                    } else {
                        bankReconciliations.get(0).setEntrytype(null);
                        bankReconciliations.get(0).setVirtualEntryType(null);
                    }
                    bankReconciliations.get(0).setEntityStatus(EntityStatus.Update);
                    //智能对账：回滚银行对账单的智能勾兑码
                    String smartcheckno = YtsContext.getYtsContext("bank" + bankReconciliations.get(0).getId()).toString();
                    Short serialdealendstate = YtsContext.getYtsContext("bankSerialdealendstate" + bankReconciliations.get(0).getId()) == null ?
                            SerialdealendState.UNEND.getValue() : Short.parseShort(YtsContext.getYtsContext("bankSerialdealendstate" + bankReconciliations.get(0).getId()).toString());
                    bankReconciliations.get(0).setSmartcheckno(smartcheckno);
                    /*bankReconciliations.get(0).setSerialdealendstate(serialdealendstate);*/
                    CommonSaveUtils.updateBankReconciliation(bankReconciliations.get(0));
                }
            }
            if (listbs != null && listbs.size() > 0) {
                BankReconciliationbusrelation_b bankReconciliationbusrelation_b = listbs.get(0);
                bankReconciliationbusrelation_b.setId(ymsOidGenerator.nextId());
                bankReconciliationbusrelation_b.setEntityStatus(EntityStatus.Insert);
                //CmpMetaDaoHelper.insert(BankReconciliationbusrelation_b.ENTITY_NAME, bankReconciliationbusrelation_b);
                List<BizObject> relationList = new ArrayList<>();
                relationList.add(bankReconciliationbusrelation_b);
                CommonSaveUtils.insertBankReconciliationbusrelation_b(relationList);
            }
        } catch (Exception e) {
            log.error("删除回写报错：：" + e);
            resJson.setCode("411");
            resJson.setMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418059F", "执行程序出错") /* "执行程序出错" */);
            return resJson;
        }
        resJson.setCode("200");
        resJson.setMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805A0", "执行成功") /* "执行成功" */);
        return resJson;
    }

    @Override
    public CommonResponseDataVo checkBillClaimAmountForRpc(List<CorrDataEntityParam> corrDataEntities) throws Exception {
        CommonResponseDataVo resJson = new CommonResponseDataVo();
        for (CorrDataEntityParam corrDataEntity : corrDataEntities) {
            BillClaim billClaim = MetaDaoHelper.findById(BillClaim.ENTITY_NAME, corrDataEntity.getBillClaimItemId());

            if (billClaim != null) {
                // 在认领单查询到数据，表示走的是认领单逻辑
                this.checkBillClaimAmount(billClaim, corrDataEntity);
            } else {
                // 反之走的是银行对账单的逻辑
                this.checkBankReconciliation(corrDataEntity);
            }
        }

        resJson.setCode("200");
        resJson.setMessage("success");
        return resJson;
    }

    private void checkBillClaimAmount(BillClaim billClaim, CorrDataEntityParam corrDataEntity) throws Exception {
        QuerySchema querySchema1 = QuerySchema.create().addSelect("*");
        QueryConditionGroup group1 = QueryConditionGroup.and(QueryCondition.name("mainid").eq(billClaim.getId()));
        querySchema1.addCondition(group1);
        List<BillClaimItem> items = MetaDaoHelper.queryObject(BillClaimItem.ENTITY_NAME, querySchema1, null);

        // 应收收款单校验逻辑,总金额相等，并且明细金额相等
        if (corrDataEntity.getBillType() != null
                && (corrDataEntity.getBillType().equals(EventType.ear_collection.getValue() + "")
                || corrDataEntity.getBillType().equals(EventType.eap_apRefund.getValue() + ""))) {
            //先校验总金额
            if (corrDataEntity.getOriSum() == null || corrDataEntity.getOriSum().compareTo(billClaim.getTotalamount().abs()) != 0) {
//                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102583"),MessageUtils.getMessage("P_YS_CTM_CM-BE_1575973060325933146") /* "单据金额合计与认领单金额合计不等" */);
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102584"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1811A04805B00038", "单据金额合计与认领单金额合计不等") /* "单据金额合计与认领单金额合计不等" */);
            }
            if (corrDataEntity.getBillClaimItemCheckEntities().size() == 0) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102585"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B000D3", "认领单明细信息必传") /* "认领单明细信息必传" */);
            }
            List<BillClaimItemCheckEntityParam> checkEntities = corrDataEntity.getBillClaimItemCheckEntities();
            for (BillClaimItemCheckEntityParam entity : checkEntities) {
                BillClaimItem item = new BillClaimItem();
                for (BillClaimItem i : items) {
                    Long itemId = i.getId(); //待比较明细IDId
                    if (itemId.equals(entity.getBillItemId())) {
                        item = i;
                    }
                }
                //平台弱类型问题，加一层转化
                BigDecimal claimAmount = item == null ? BigDecimal.ZERO : item.getClaimamount();
                // start wangdengk 20230811 需求变更调整去掉应收应付拆单场景下子表明细金额和认领单金额不相等的校验
//                    if (item == null || claimAmount.compareTo(entity.getSumAmount()) != 0) {
////                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102586"),MessageUtils.getMessage("P_YS_CTM_CM-BE_1575973060325933233") /* "单据明细金额与认领单明细金不等" */);
//                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102587"),"单据明细金额与认领单明细金不等");
//                    }
                // end wangdengk 20230811 需求变更调整去掉应收应付拆单场景下子表明细金额和认领单金额不相等的校验
                //查询银行对账单关联关系，并修改
                QuerySchema querySchema = QuerySchema.create().addSelect("*");
                QueryConditionGroup group = QueryConditionGroup.and(
                        QueryCondition.name("bankreconciliation").eq(item.getBankbill()),
                        QueryCondition.name("billcode").eq(corrDataEntity.getCode())
                );
                querySchema.addCondition(group);
                List<BankReconciliationbusrelation_b> bankReconciliationbusrelation_bs = MetaDaoHelper.queryObject(BankReconciliationbusrelation_b.ENTITY_NAME, querySchema, null);
                for (BankReconciliationbusrelation_b b : bankReconciliationbusrelation_bs) {
                    b.setDept(corrDataEntity.getDept());
                    b.setProject(corrDataEntity.getProject());
                    b.setVouchdate(corrDataEntity.getVouchdate());
                    EntityTool.setUpdateStatus(b);
                    MetaDaoHelper.update(BankReconciliationbusrelation_b.ENTITY_NAME, b);
                }
            }
        }
    }

    private void checkBankReconciliation(CorrDataEntityParam corrDataEntity) throws Exception {
        // 应收收款单,应付付款退款单校验逻辑,总金额相等
        if (corrDataEntity.getBillType() != null && this.isNeedCheckType(corrDataEntity.getBillType())) {
            BankReconciliation bankReconciliation = MetaDaoHelper.findById(BankReconciliation.ENTITY_NAME, corrDataEntity.getBillClaimItemId());
            //先校验总金额
            if (corrDataEntity.getOriSum() == null || corrDataEntity.getOriSum().compareTo(bankReconciliation.getTran_amt().abs()) != 0) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102588"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A0508010D", "单据金额合计与银行对账单金额合计不等") /* "单据金额合计与银行对账单金额合计不等" */ /* "单据金额合计与银行对账单金额合计不等" */);
            }

            if (corrDataEntity.getBillClaimItemCheckEntities().size() == 0) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102589"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A0508010C", "银行对账单明细信息必传") /* "银行对账单明细信息必传" */ /* "银行对账单明细信息必传" */);
            }
        }
    }

    private boolean isNeedCheckType(String billType) {
        // 需要校验 应收收款单,应付付款退款单
        return billType.equals(EventType.ear_collection.getValue() + "")
                || billType.equals(EventType.eap_apRefund.getValue() + "");
    }

    private BankReconciliationbusrelation_b getBankReconciliationbusrelation_b(CorrDataEntity corr) {
        return this.getBankReconciliationbusrelation_b(corr, false);
    }

    private BankReconciliationbusrelation_b getBankReconciliationbusrelation_b(CorrDataEntity corr, boolean confirmedwhenAuto) {
        BankReconciliationbusrelation_b bankReconciliationbusrelation_b = new BankReconciliationbusrelation_b();
        bankReconciliationbusrelation_b.setAccentity(corr.getAccentity());
        bankReconciliationbusrelation_b.setBillnum(corr.getBillNum());
        bankReconciliationbusrelation_b.setBillid(corr.getBusid());
        bankReconciliationbusrelation_b.setBilltype(Short.valueOf(corr.getBillType()));
        bankReconciliationbusrelation_b.setAmountmoney(corr.getOriSum());
        bankReconciliationbusrelation_b.setBankreconciliation(corr.getBankReconciliationId());
        bankReconciliationbusrelation_b.setBillcode(corr.getCode());
        bankReconciliationbusrelation_b.setDept(corr.getDept());
        bankReconciliationbusrelation_b.setProject(corr.getProject());
        short billType = StringUtils.isEmpty(corr.getBillType()) ? -1 : Short.parseShort(corr.getBillType());
        if (EventType.StwbSettleMentDetails.getValue() == billType && corr.getAuto()) {
            bankReconciliationbusrelation_b.setRelationtype(Relationtype.AutoAssociated.getValue());
        } else if (EventType.StwbSettleMentDetails.getValue() == billType && !corr.getAuto()) {
            bankReconciliationbusrelation_b.setRelationtype(Relationtype.ManualAssociated.getValue());
        } else {
            bankReconciliationbusrelation_b.setRelationtype(Relationtype.MakeBillAssociated.getValue());
        }
        if (corr.getAuto()) {
            if (confirmedwhenAuto) {
                bankReconciliationbusrelation_b.setRelationstatus(Relationstatus.Confirmed.getValue());
            } else {
                bankReconciliationbusrelation_b.setRelationstatus(Relationstatus.Confirm.getValue());
            }
        } else {
            bankReconciliationbusrelation_b.setRelationstatus(Relationstatus.Confirmed.getValue());
        }
        bankReconciliationbusrelation_b.setVouchdate(corr.getVouchdate());
        bankReconciliationbusrelation_b.setSrcbillid(corr.getMainid());
        bankReconciliationbusrelation_b.setIsadvanceaccounts(corr.getIsadvanceaccounts());
        bankReconciliationbusrelation_b.setAssociationcount(corr.getAssociationcount());
        bankReconciliationbusrelation_b.setId(ymsOidGenerator.nextId());
        bankReconciliationbusrelation_b.setEntityStatus(EntityStatus.Insert);
        return bankReconciliationbusrelation_b;
    }

    /**
     * 校验pubts
     *
     * @param corr
     * @return
     * @throws Exception
     */
    public boolean checkPubts(CorrDataEntity corr) throws Exception {
        if (corr.getBillClaimItemId() == null) return true;
        SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        List<BillClaim> list;
        QuerySchema querySchema = QuerySchema.create().addSelect("*");
        QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("id").eq(corr.getBillClaimItemId()));
        querySchema.addCondition(group);
        list = MetaDaoHelper.queryObject(BillClaim.ENTITY_NAME, querySchema, null);
        if (list == null || list.size() == 0) {
            group = QueryConditionGroup.and(QueryCondition.name("refbill").eq(corr.getBillClaimItemId()));
            querySchema.addCondition(group);
            list = MetaDaoHelper.queryObject(BillClaim.ENTITY_NAME, querySchema, null);

            if (list == null || list.size() == 0) {
                // 表示没有查询到认领单，则进入银行对账单逻辑

                // 认领单ID赋值给银行对账单ID
                corr.setBankReconciliationId(corr.getBillClaimItemId());

                QuerySchema querySchema2 = QuerySchema.create().addSelect("*");
                QueryConditionGroup group2 = QueryConditionGroup.and(QueryCondition.name("id").eq(corr.getBankReconciliationId()));
                querySchema2.addCondition(group2);
                List<BankReconciliation> bankList = MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME, querySchema2, null);

                // 银行认领单校验pubts
                Date date = bankList.get(0).getPubts();
                String pus = sf.format(date);
                return pus.equals(corr.getBankReconciliationPubts());
            }
        }

        Date date = list.get(0).getPubts();
        String pus = sf.format(date);
        return pus.equals(corr.getBankReconciliationPubts());
    }

    /**
     * 判断该单据是否已生成虚拟流水
     *
     * @param bankReconciliation
     * @throws Exception
     */
    private void checkIsunique(BankReconciliation bankReconciliation) throws Exception {
        QuerySchema querySchema = QuerySchema.create();
        querySchema.addSelect("count(1) as  count");
        String virtualflowid = String.valueOf(bankReconciliation.getVirtualflowid() == null ? bankReconciliation.getId() : bankReconciliation.getVirtualflowid());
        querySchema.appendQueryCondition(QueryCondition.name("virtualflowid").eq(virtualflowid));
        Map<String, Object> map = MetaDaoHelper.queryOne(BankReconciliation.ENTITY_NAME, querySchema);
        Long count = (Long) map.get("count");
        if (count > 0) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102590"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041803CD", "该对账单已生成虚拟流水，请重新选择") /* "该对账单已生成虚拟流水，请重新选择" */);
        }
    }

    /**
     * 生成虚拟流水
     *
     * @param pushBankReconciliation
     * @param billtype
     * @throws Exception
     */
    private void createPushBill(BankReconciliation pushBankReconciliation, String billtype) throws Exception {
        // 万能接口查询银行信息
        EnterpriseBankAcctVO bankaccount = baseRefRpcService.queryEnterpriseBankAccountById(pushBankReconciliation.getBankaccount());
        BankReconciliation bankReconciliation = new BankReconciliation();
        EnterpriseParams enterpriseParams = new EnterpriseParams();
        enterpriseParams.setAccount(pushBankReconciliation.getTo_acct_no());
        List<EnterpriseBankAcctVO> enterpriseBankAccounts = enterpriseBankQueryService.query(enterpriseParams);
        if (enterpriseBankAccounts != null && enterpriseBankAccounts.size() > 0) {
            //      对方银行账号
            bankReconciliation.setBankaccount(enterpriseBankAccounts.get(0).getId());
            //      会计主体
            bankReconciliation.setAccentity(enterpriseBankAccounts.get(0).getOrgid());
        }
        //      本方银行账号
        bankReconciliation.setTo_acct_no(bankaccount.getAccount());
        //      本方账户名称
        bankReconciliation.setTo_acct_name(bankaccount.getName());
        //      本方开户行
        bankReconciliation.setTo_acct_bank(bankaccount.getBankName());
        //      本方开户行名
        bankReconciliation.setTo_acct_bank_name(bankaccount.getBankNumberName());
        //      借贷方向及金额
        if (billtype.equals(String.valueOf(EventType.Disburse_payment_slip.getValue()))) {
            bankReconciliation.setDc_flag(Direction.Credit);
            bankReconciliation.setCreditamount(pushBankReconciliation.getTran_amt());
        } else {
            bankReconciliation.setDc_flag(Direction.Debit);
            bankReconciliation.setDebitamount(pushBankReconciliation.getTran_amt());
        }
        //      数据来源
        bankReconciliation.setDataOrigin(DateOrigin.AddManually);
        //      事项来源
        bankReconciliation.setSrcitem(EventSource.Cmpchase);
        //      银行交易流水号
        String bankNum = String.valueOf(new StringBuffer("XN").append(new SimpleDateFormat("yyyyMMdd").format(new Date())).append(this.getL6()));
        bankReconciliation.setBank_seq_no(bankNum);
        //      虚拟标记
        bankReconciliation.setIsvirtualflow(true);
        //      币种
        bankReconciliation.setCurrency(pushBankReconciliation.getCurrency());
        //     交易日期
        bankReconciliation.setTran_date(pushBankReconciliation.getTran_date());
        //     对方类型
        bankReconciliation.setOppositetype(pushBankReconciliation.getOppositetype());
        EnterpriseParams enterpriseParams1 = new EnterpriseParams();
        enterpriseParams1.setAccount(bankReconciliation.getTo_acct_no());
        List<EnterpriseBankAcctVO> enterpriseBankAccount = enterpriseBankQueryService.query(enterpriseParams1);
        if (enterpriseBankAccount != null && enterpriseBankAccount.size() > 0) {
            //     对方单位
            bankReconciliation.setOppositeobjectid(enterpriseBankAccount.get(0).getOrgid());
            //     对方单位名称
            bankReconciliation.setOppositeobjectname(enterpriseBankAccount.get(0).getOrgidName());
        }
        if (pushBankReconciliation.get("remark") != null) {
            //      摘要
            bankReconciliation.setRemark(pushBankReconciliation.get("remark").toString());
        }
        if (pushBankReconciliation.get("use_name") != null) {
            //      用途
            bankReconciliation.setUse_name(pushBankReconciliation.get("use_name").toString());
        }
        //      是否自动生单
        bankReconciliation.setIsautocreatebill(false);
        //      自动关联标志
        bankReconciliation.setAutoassociation(false);
        //     创建时间
        bankReconciliation.setCreateTime(pushBankReconciliation.getCreateTime());
        //     交易金额
        bankReconciliation.setTran_amt(pushBankReconciliation.getTran_amt());
        //     雪花id
        bankReconciliation.setId(ymsOidGenerator.nextId());
        //     关联虚拟流水id
        bankReconciliation.setVirtualflowid(pushBankReconciliation.getId().toString());
        // 未解析出财资统一码，生成财资统一码并进行设置
        bankReconciliation.setSmartcheckno(RemoteDubbo.get(ReconciliateCodeGenerator.class, IDomainConstant.MDD_DOMAIN_STWB).generate());
        bankReconciliation.setIsparsesmartcheckno(false);
        // 所属组织
        bankReconciliation.setOrgid(bankaccount.getOrgid());
        // 确认状态
        if (org.apache.commons.lang3.StringUtils.isNotEmpty(bankReconciliation.getAccentity())) {
            bankReconciliation.setConfirmstatus(ConfirmStatusEnum.Confirmed.getIndex());
        }
        // 对账单日期
        bankReconciliation.setDzdate(pushBankReconciliation.getDzdate() == null ? pushBankReconciliation.getTran_date() : pushBankReconciliation.getDzdate());

        //  入库
        CommonSaveUtils.saveBankReconciliation(bankReconciliation);
    }

    /**
     * 是否开启虚拟对账单设置
     *
     * @param bs
     * @return
     * @throws Exception
     */
    private Boolean check(BankReconciliation bs) throws Exception {
        EnterpriseBankAcctVO bankaccount = baseRefRpcService.queryEnterpriseBankAccountById(bs.getBankaccount());
        //      根据银行类别在虚拟流水规则找是否启用该规则
        QuerySchema querySchema = QuerySchema.create().addSelect("*");
        QueryConditionGroup condition = QueryConditionGroup.and(QueryCondition.name("banktype").eq(bankaccount.getBank()));
        querySchema.addCondition(condition);
        Map<String, Object> virtualFlowRulelist = MetaDaoHelper.queryOne(VirtualFlowRuleConfig.ENTITY_NAME, querySchema);
        if (MapUtils.isNotEmpty(virtualFlowRulelist)) {
            // 规则启用
            if (Short.valueOf(virtualFlowRulelist.get("isEnable").toString()).equals(IsEnable.ENABLE.getValue())) {
                return true;
            }
        }
        return false;
    }

    public String getL6() throws Exception {
        String ytenantId = AppContext.getYTenantId();
        QuerySchema schema = QuerySchema.create().addSelect("bank_seq_no");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
//        conditionGroup.appendCondition(QueryCondition.name(IBussinessConstant.ACCENTITY).eq(accentity));
        conditionGroup.appendCondition(QueryCondition.name("ytenantId").eq(ytenantId));
        conditionGroup.appendCondition(QueryCondition.name("isvirtualflow").eq(true));
        schema.addOrderBy(new QueryOrderby("createTime", "desc"));
        schema.addCondition(conditionGroup);
        Map<String, Object> list = MetaDaoHelper.queryOne(BankReconciliation.ENTITY_NAME, schema);
        String equipmentNo = null;
        if (MapUtils.isNotEmpty(list)) {
            String seqNo = list.get("bank_seq_no").toString().substring(2, 10);
            //  同一天从库里最大的开始加
            if (new SimpleDateFormat("yyyyMMdd").format(new Date()).equals(seqNo)) {
                equipmentNo = list.get("bank_seq_no").toString().substring(11);
            } else {
                // 不同天默认从000001开始
                equipmentNo = "000001";
            }
            // 字符串数字解析为整数
            int no = Integer.parseInt(equipmentNo);
            // 编号自增1
            int newEquipment = ++no;
            equipmentNo = String.format("%06d", newEquipment);
        } else {
            equipmentNo = "000001";
        }
        return equipmentNo;
    }

    /**
     * 根据主表id、单据类型查询是否存在重复生单的数据，大于1则表示存在重复数据
     *
     * @param condition
     * @return
     */
    public boolean checkRepeatRelationsByCondition(CtmJSONObject condition) throws Exception {
        QuerySchema querySchema1 = QuerySchema.create().addSelect("*");//业务单据子表id、单据类型
        QueryConditionGroup group1 = QueryConditionGroup.and(QueryCondition.name("bankreconciliation").eq(condition.get("bankreconciliation")));
        group1.addCondition(QueryCondition.name(ICmpConstant.BILLTYPE).eq(condition.get(ICmpConstant.BILLTYPE)));
        querySchema1.addCondition(group1);
        List<BankReconciliationbusrelation_b> bankReconciliationbusrelation_bs = MetaDaoHelper.queryObject(BankReconciliationbusrelation_b.ENTITY_NAME, querySchema1, null);
        return bankReconciliationbusrelation_bs.size() >= 1;
    }

    /**
     * 查询关联明细
     *
     * @param bankReconciliationId 流水id
     * @param billDetailId         结算明细主键id
     */
    private BankReconciliationbusrelation_b queryBankReconciliationBusByBillId(Long bankReconciliationId, Long billDetailId) {
        List<BankReconciliationbusrelation_b> list = bankDealDetailAccessDao.queryBankReconciliationBusByBillIdAndBankReconciliationId(bankReconciliationId, billDetailId);
        if (CollectionUtils.isEmpty(list)) {
            return null;
        }
        return list.get(0);
    }

    /**
     * 批量删除流水的关联关系
     *
     * @param commonRequestDataVos
     * @return
     * @throws Exception
     */
    @Transactional
    public CommonResponseDataVo batchResDelDataForBankReconciliation(List<CommonRequestDataVo> commonRequestDataVos) throws Exception {
        CommonResponseDataVo resJson = new CommonResponseDataVo();
        try {
            log.error("resDelDataForRpc param is " + CtmJSONObject.toJSONString(commonRequestDataVos));
            List<Long> bankreconciliationids = new ArrayList<>();
            for (CommonRequestDataVo commonRequestDataVo : commonRequestDataVos) {
                if (commonRequestDataVo.getBusid() != null) {
                    bankreconciliationids.add(Long.valueOf(commonRequestDataVo.getBusid()));
                } else {
                    throw new CtmException(InternationalUtils.getMessageWithDefault("UID:P_CM-BE_2009378204F80000", "删除关联关系失败，流水或认领单id为空！"));//@notranslate
                }
            }
            if (CollectionUtils.isNotEmpty(bankreconciliationids)) {
                QuerySchema querySchemaBank = QuerySchema.create().addSelect("*");
                QueryConditionGroup groupBank = QueryConditionGroup.and(QueryCondition.name("id").in(bankreconciliationids));
                querySchemaBank.addCondition(groupBank);
                List<BankReconciliation> originBankReconciliation = MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME, querySchemaBank, null);
                Map<Long, BankReconciliation> originBankReconciliationMap = new HashMap<>();
                for (BankReconciliation bankReconciliation : originBankReconciliation) {
                    originBankReconciliationMap.put(bankReconciliation.getId(), bankReconciliation);
                }
                // 传递的认领单ID未查询到对应数据，则进入银行对账单ID逻辑， 将认领单ID传递给银行对账单ID逻辑
                for (CommonRequestDataVo json : commonRequestDataVos) {
                    Long busid = json.getBusid() == null ? null : Long.valueOf(json.getBusid());//银行对账单id
                    Long stwbbusid = json.getStwbbusid();//业务单据id 资金结算单
                    List<BankReconciliationbusrelation_b> bs = new ArrayList<>();
                    this.handleBusId(busid, bs, stwbbusid, json, originBankReconciliationMap);
                }
            }
        } catch (Exception e) {
            log.error("删除回写报错：：" + e, e);
            throw new CtmException(InternationalUtils.getMessageWithDefault("UID:P_CM-BE_200935B004F80005", "删除关联关系报错!") + e.getMessage(), e);//@notranslate
        }
        resJson.setCode("200");
        resJson.setMessage(InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805A0", "执行成功"));//@notranslate
        return resJson;
    }


    /**
     * 批量删除认领单的关联关系
     *
     * @param commonRequestDataVos
     * @return
     * @throws Exception
     */
    @Transactional
    public CommonResponseDataVo batchResDelDataForBillclaim(List<CommonRequestDataVo> commonRequestDataVos) throws Exception {
        CommonResponseDataVo resJson = new CommonResponseDataVo();
        try {
            log.error("resDelDataForRpc param is " + CtmJSONObject.toJSONString(commonRequestDataVos));
            List<Long> claimIds = new ArrayList<>();
            for (CommonRequestDataVo commonRequestDataVo : commonRequestDataVos) {
                if (commonRequestDataVo.getClaimId() != null) {
                    claimIds.add(commonRequestDataVo.getClaimId());
                } else {
                    throw new CtmException(InternationalUtils.getMessageWithDefault("UID:P_CM-BE_2009378204F80000", "删除关联关系失败，流水或认领单id为空！"));//@notranslate
                }
            }
            if (CollectionUtils.isNotEmpty(claimIds)) {
                QuerySchema bquery = QuerySchema.create().addSelect("*");
                QueryConditionGroup querygroup = QueryConditionGroup.and(QueryCondition.name("id").in(claimIds));
                bquery.addCondition(querygroup);
                List<BillClaim> billClaims = MetaDaoHelper.queryObject(BillClaim.ENTITY_NAME, bquery, null);
                Map<Long, BillClaim> originBillClaimMap = new HashMap<>();
                if (CollectionUtils.isEmpty(billClaims)) {
                    QuerySchema bquery2 = QuerySchema.create().addSelect("*");
                    QueryConditionGroup querygroup2 = QueryConditionGroup.and(QueryCondition.name("refbill").in(claimIds));
                    bquery2.addCondition(querygroup2);
                    billClaims = MetaDaoHelper.queryObject(BillClaim.ENTITY_NAME, bquery2, null);
                    //更新实际认领单位认领完结状态
                    if (CollectionUtils.isNotEmpty(billClaims)) {
                        for (BillClaim billClaim : billClaims) {
                            //CZFW-455483【DSP支持问题】结算中心代理模式下的认领单生成资金收付款单据并且流程走完后，又将资金收付款单据撤回来，此时单据还在但是认领单还有生单按钮，会导致重复生单（见附件）
                            billClaim.setRefassociationstatus(null);
                            // 结算中心代理模式不更新认领单的财资统一码
                            billClaim.setSettlestatus(null);
                            billClaim.setEntityStatus(EntityStatus.Update);
                        }
                        CommonSaveUtils.updateBillClaim(billClaims);
                        resJson.setCode("200");
                        resJson.setMessage(InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805A0", "执行成功") /* "执行成功" */);//@notranslate
                        return resJson;
                    } else {
                        // 传递的认领单ID未查询到对应数据，则进入银行对账单ID逻辑， 将认领单ID传递给银行对账单ID逻辑
                        this.batchResDelDataForBankReconciliation(commonRequestDataVos);
                    }
                } else {
                    for (BillClaim billClaim : billClaims) {
                        originBillClaimMap.put(billClaim.getId(), billClaim);
                    }
                    for (CommonRequestDataVo commonRequestDataVo : commonRequestDataVos) {
                        //首先判断认领单的认领类型
                        //如果是部分认领 首先通过认领单子表获取到银行对账单，然后获取银行对账单的关联关系，查询关联关系表中数据的billid，判断是否包含stwbbusid
                        //如果包含，则删除关联stwbbusid的关联关系数据，并且修改银行对账单和认领单的关联状态为未关联，清空勾兑码；如果不是，则直接返回。
                        BillClaim billClaim = originBillClaimMap.get(commonRequestDataVo.getClaimId());
                        Long stwbbusid = commonRequestDataVo.getStwbbusid();//业务单据id 资金结算单
                        List<BankReconciliationbusrelation_b> bs = new ArrayList<>();
                        //调用资金结算财资统一对账码接口生成
                        StringBuilder smartcheckno = new StringBuilder(RemoteDubbo.get(ReconciliateCodeGenerator.class, IDomainConstant.MDD_DOMAIN_STWB).generate());
                        if (billClaim.getClaimtype() != null && billClaim.getClaimtype() == BillClaimType.Part.getValue()) {
                            handleDeleteClaim4PartClaim(billClaim, stwbbusid, bs, smartcheckno.toString());
                        } else {
                            handleDeleteClaim4OtherClaim(billClaim, stwbbusid, bs, smartcheckno.toString());
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("删除回写报错：：" + e, e);
            throw new CtmException(InternationalUtils.getMessageWithDefault("UID:P_CM-BE_200935B004F80005", "删除关联关系报错!") + e.getMessage(), e);//@notranslate
        }
        resJson.setCode("200");
        resJson.setMessage(InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805A0", "执行成功"));//@notranslate
        return resJson;
    }


}