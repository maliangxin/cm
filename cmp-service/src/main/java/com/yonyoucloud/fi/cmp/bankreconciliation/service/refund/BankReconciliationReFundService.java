package com.yonyoucloud.fi.cmp.bankreconciliation.service.refund;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.yonbip.ctm.util.JSONBuilderUtils;
import com.yonyoucloud.ctm.stwb.pubitf.settle.settlebench.ISettlementBenchPubService;
import com.yonyoucloud.ctm.stwb.pubitf.settle.settlebench.vo.reqvo.RefundVO;
import com.yonyoucloud.fi.cmp.autorefundcheckrule.AutoRefundCheckRule;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.bankreconciliation.CommonSaveUtils;
import com.yonyoucloud.fi.cmp.bankreconciliation.RefundAutoCheckRuleService;
import com.yonyoucloud.fi.cmp.bankreconciliation.utils.CommonBankReconciliationProcessor;
import com.yonyoucloud.fi.cmp.billclaim.BillClaimItem;
import com.yonyoucloud.fi.cmp.cmpentity.OppositeType;
import com.yonyoucloud.fi.cmp.cmpentity.ReFundType;
import com.yonyoucloud.fi.cmp.cmpentity.RefundStatus;
import com.yonyoucloud.fi.cmp.util.DateUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QueryOrderby;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;


/**
 * 本类用于退票相关接口，包含退票确认、拒绝，确认之后发布消息到事件中心等
 *
 * @author maliangn
 */
@Service
@Slf4j
@Transactional
public class BankReconciliationReFundService {
    public static final String ENABLE = "1";

    public static final String SPILT_STR = ",";
    @Autowired
    private ISettlementBenchPubService iSettleBenchPubService;

    @Resource
    private RefundAutoCheckRuleService refundAutoCheckRuleService;

    /**
     * 确认退票
     *
     * @param ids
     * @return
     * @throws Exception
     */
    @Transactional(rollbackFor = RuntimeException.class)
    public JsonNode confirmRefund(List ids) throws Exception {
        ObjectNode result = JSONBuilderUtils.createJson();
        List<String> messages = new ArrayList<>();
        try {
            //查询需要退票的对账单
            List<BankReconciliation> bankReconciliations;
            QuerySchema querySchema = QuerySchema.create().addSelect("*");
            QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("id").in(ids));
            querySchema.addCondition(group);
            bankReconciliations = MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME, querySchema, null);
            //修改银行对账单
            for (int i = 0; i < bankReconciliations.size(); i++) {
                bankReconciliations.get(i).setEntityStatus(EntityStatus.Update);
                bankReconciliations.get(i).setRefundstatus(RefundStatus.Refunded.getValue());
                //退票确认人赋值
                bankReconciliations.get(i).setRefundconfirmstaff(AppContext.getCurrentUser().getName());
            }
            this.refundSettlebench(bankReconciliations);
            CommonSaveUtils.updateBankReconciliationConfirmOrReject(bankReconciliations,"2");
            messages.add(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B0024D", "确认成功") /* "确认成功" */);//确认成功
        } catch (Exception e) {
            messages.clear();
            messages.add(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B0024F", "退票确认失败，失败原因为：") /* "退票确认失败，失败原因为：" */ + e.getMessage());//退票确认失败，失败原因为：
        }
        result.putPOJO("messages", messages);
        return result;
    }

    /**
     * 拒绝退票
     * @param ids
     * @return
     * @throws Exception
     */
    public JsonNode refuseRefund(List ids) throws Exception {
        ObjectNode result = JSONBuilderUtils.createJson();
        List<String> messages = new ArrayList<>();
        try {
            //查询需要退票的对账单
            List<BankReconciliation> bankReconciliations;
            QuerySchema querySchema = QuerySchema.create().addSelect("*");
            QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("id").in(ids));
            querySchema.addCondition(group);
            bankReconciliations = MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME, querySchema, null);
            Set<String> usedBankReconciliationIds = new HashSet<>();
            Map<String, BankReconciliation> bankReconciliationMap = bankReconciliations.stream()
                    .collect(Collectors.toMap(item -> item.getId().toString(), Function.identity()));
            bankReconciliations.stream().forEach(item -> {
                if (usedBankReconciliationIds.contains(item.getId())) {
                    return;
                }
                //记录拒绝退票的id(即原来退票关联的id),加在原来的拒绝id后面
                BankReconciliation bankReconciliationOrigin = item;
                String refundrelationid = bankReconciliationOrigin.getRefundrelationid();
                if (refundrelationid == null) {
                    return;
                }
                BankReconciliation bankReconciliationRelation = bankReconciliationMap.get(refundrelationid);
                if (bankReconciliationRelation != null) {
                    String originRefundRejectRelationId = bankReconciliationOrigin.getRefundrejectrelationid();
                    String relationRefundRejectRelationId = bankReconciliationRelation.getRefundrejectrelationid();
                    String refundrejectrelationidOriNew = (originRefundRejectRelationId == null ? "" : (originRefundRejectRelationId + SPILT_STR)) + bankReconciliationRelation.getId();
                    String refundrejectrelationidRelNew = (relationRefundRejectRelationId == null ? "" : (relationRefundRejectRelationId + SPILT_STR)) + bankReconciliationOrigin.getId();
                    bankReconciliationOrigin.setRefundrejectrelationid(refundrejectrelationidOriNew);
                    bankReconciliationRelation.setRefundrejectrelationid(refundrejectrelationidRelNew);
                    usedBankReconciliationIds.add(bankReconciliationOrigin.getId().toString());
                    usedBankReconciliationIds.add(bankReconciliationRelation.getId().toString());
                }
            });
            //修改银行对账单
            for (int i = 0; i < bankReconciliations.size(); i++) {
                bankReconciliations.get(i).setEntityStatus(EntityStatus.Update);
                bankReconciliations.get(i).setRefundstatus(null);
                bankReconciliations.get(i).setRefundrelationid(null);
                //智能到账，退票自动辨识自动设置为false
                bankReconciliations.get(i).setRefundauto(null);
            }
            CommonSaveUtils.updateBankReconciliationConfirmOrReject(bankReconciliations,"2");
            messages.add(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B0024E", "拒绝成功") /* "拒绝成功" */);//拒绝成功
        } catch (Exception e) {
            messages.clear();
            messages.add(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00250", "退票拒绝失败，失败原因为：") /* "退票拒绝失败，失败原因为：" */ + e.getMessage());//退票拒绝失败，失败原因为：
        }
        result.putPOJO("messages", messages);
        return result;
    }


    /**
     * 广播退票单据信息，针对不同的场景，广播出去
     * 由消息改为RPC接口
     *
     * @param bankReconciliations
     * @throws Exception
     */
    public void refundSettlebench(List<BankReconciliation> bankReconciliations) throws Exception {
        // 过滤银行对账单，过滤出来蓝单中存在关联关系的单据，将单据推送至资金结算
        List<BankReconciliation> newBankReconciliations = filterBill(bankReconciliations);
        List fundpaymentids = new ArrayList();
        // 判断单据是否被认领，如果被认领需要查询对应的认领单信息，如果未被认领则使用银行对账单单据id
        for (BankReconciliation e : newBankReconciliations) {
            //SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            //BizObject bizObject = new BizObject();
            ////未认领
            //if (e.getBillclaimstatus().shortValue() == 0) {
            //    bizObject.put("bankrconid", e.getId());
            //    bizObject.put("refundSum", e.getTran_amt());//退票金额
            //    bizObject.put("billflag", "B");//银行对账单
            //    if (null != e.getTran_time()) {
            //        bizObject.put("refundDate", outputFormat.format(e.getTran_time()));//退票时间
            //    } else {
            //        bizObject.put("refundDate", outputFormat.format(e.getTran_date()));//退票时间
            //    }
            //} else {
            //    bizObject.put("claimids", queryClaimids(e.getId()));
            //    bizObject.put("billflag", "C");//认领单
            //    bizObject.put("refundSum", e.getTran_amt());//退票金额
            //    if (null != e.getTran_time()) {
            //        bizObject.put("refundDate", outputFormat.format(e.getTran_time()));//退票时间
            //    } else {
            //        bizObject.put("refundDate", outputFormat.format(e.getTran_date()));//退票时间
            //    }
            //}
            //SendEventMessageUtils.sendEventMessageEos(bizObject, IEventCenterConstant.CMP_BANKRECONCILIATION, IEventCenterConstant.CMP_BANKRECONCILIATION_REFUND);
            RefundVO refundVO = new RefundVO();
            //未认领
            if (e.getBillclaimstatus().shortValue() == 0) {
                refundVO.setBankrconid(e.getId());
                refundVO.setRedunfSum(e.getTran_amt());//退票金额
                //新的rpc接口不需要了
                //refundVO.put("billflag", "B");//银行对账单
                if (null != e.getTran_time()) {
                    refundVO.setRefundDate(e.getTran_time());//退票时间
                } else {
                    refundVO.setRefundDate(e.getTran_date());//退票时间
                }
            } else {
                refundVO.setClaimids(queryClaimids(e.getId()));
                //新的rpc接口不需要了
                //refundVO.put("billflag", "C");//认领单
                refundVO.setRedunfSum(e.getTran_amt());//退票金额
                if (null != e.getTran_time()) {
                    refundVO.setRefundDate(e.getTran_time());//退票时间
                } else {
                    refundVO.setRefundDate(e.getTran_date());//退票时间
                }
            }
            log.error("退票接口refundSettlebench入参：" + refundVO);
            iSettleBenchPubService.refundSettlebench(refundVO);
            log.error("退票接口refundSettlebench调用结束");
        }
        /*if(fundpaymentids.size()>1){
            QuerySchema querySchema = QuerySchema.create().addSelect("*");
            QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("id").in(fundpaymentids));
            querySchema.addCondition(group);
            List<FundPayment_b> fds = MetaDaoHelper.query(FundPayment_b.ENTITY_NAME,querySchema,null);
            fds.stream().forEach(e->{
                e.setFundSettlestatus(FundSettleStatus.Refund);
                e.setRefundSum();
            });
        }*/
    }

    /**
     * 过滤银行对账单，过滤出来蓝单中存在关联关系的单据，将单据推送至资金结算
     *
     * @param bankReconciliations
     * @return
     */
    private List<BankReconciliation> filterBill(List<BankReconciliation> bankReconciliations) {
        List<BankReconciliation> newBankReconciliations = new ArrayList<>();
        bankReconciliations.stream().forEach(e -> {
            if (e.getDc_flag().getValue() == 1 && e.getTran_amt().compareTo(BigDecimal.ZERO) > 0) { //方向为借，金额为正
                if (e.getAssociationstatus() == 1) { //只处理已关联的单据
                    newBankReconciliations.add(e);
                }
            }
        });
        return newBankReconciliations;
    }


    /**
     * 根据银行对账单的id查询对应认领单明细对应的认领单id
     *
     * @param id
     * @return
     * @throws Exception
     */
    private List<Long> queryClaimids(Object id) throws Exception {

        List<Long> claimids = new ArrayList<>();
        QuerySchema queryIsExist = QuerySchema.create().addSelect("mainid");
        QueryConditionGroup conditionGroup = new QueryConditionGroup();
        conditionGroup.addCondition(QueryCondition.name("bankbill").eq(id));
        queryIsExist.addCondition(conditionGroup);
        List<Map<String, Object>> claimItems = MetaDaoHelper.query(BillClaimItem.ENTITY_NAME, queryIsExist, null);
        if (claimItems.size() > 0) {
            claimItems.stream().forEach(e -> {
                claimids.add(Long.parseLong(e.get("mainid").toString()));
            });
        }
        return claimids;
    }

    /**
     * 退票辨识流程
     *
     * @param newBankRecords
     * @throws Exception
     */
    public void checkRefundBankReconciliation(List<BankReconciliation> newBankRecords) throws Exception {
        // 退票辨识  RPT0210退票辨识优化
        String refundCheckFlag = AppContext.getEnvConfig("cmp.refundCheckFlag", "0");
        if (ENABLE.equals(refundCheckFlag)) {
            List<String> bankaccountIds = new ArrayList<>();
            newBankRecords.stream().forEach(e -> {
                bankaccountIds.add(e.getBankaccount());
            });
            /**
             * 1,查询辨识规则
             * 2,根据辨识规则获取对应【匹配数据集合】
             * 3,退票辨识
             */
            // 1,退票辨识规则
            AutoRefundCheckRule refundCheckRule = refundAutoCheckRuleService.queryRuleInfo(null);
            if (refundCheckRule == null) {
                log.error("====退票辨识：=======退票辨识规则为空=========跳过退票辨识==========");
            } else {
                // 2,根据退票规则参数查询匹配数据集合
                List<BankReconciliation> matchList = getBankReconciliations(refundCheckRule, bankaccountIds);//匹配数据集合
                //如果待监测数据或待匹配数据为空，则不必执行此任务
                if (CollectionUtils.isEmpty(matchList) || CollectionUtils.isEmpty(matchList)) {
                    log.error("====退票辨识：=======匹配集合或检测集合数据为空=========跳过退票辨识==========");
                } else {
                    log.error("====退票辨识：=======检测集合:" + newBankRecords.size());
                    log.error("====退票辨识：=======匹配集合:" + matchList.size());
                    List<BankReconciliation> updateBankrecons = this.matchReFundData(newBankRecords, matchList);
                    log.error("====退票辨识：=======待更新集合:" + updateBankrecons.size());
                    if (CollectionUtils.isNotEmpty(updateBankrecons)) {
                        CommonBankReconciliationProcessor.batchReconciliationBeforeUpdate(updateBankrecons);
                        MetaDaoHelper.update(BankReconciliation.ENTITY_NAME, updateBankrecons);
                    }
                }
            }
        }

    }

    /**
     * 根据规则设置的银行类别，日期范围
     * 获取匹配数据集合
     * 匹配对方账号不为空，退票状态为空，日期范围内的数据 默认1天指今天和昨天
     *
     * @return
     * @throws Exception
     */
    List<BankReconciliation> getBankReconciliations(AutoRefundCheckRule refundCheckRule, List<String> bankaccountIds) throws Exception {
        if (CollectionUtils.isEmpty(bankaccountIds)) {
            return new ArrayList<>();
        }
        List<BankReconciliation> list;
        Integer dataRange = refundCheckRule.getDaterange();
        if (dataRange == null) {
            dataRange = 1;
        }
        // 银行类别
        String banktype = refundCheckRule.getBanktype();
        SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd");
        // 下面查询不能替换成*，因为特征字段在copy时报错
        QuerySchema querySchema = QuerySchema.create().addSelect("*");
        //默认为查询监测数据列表
        QueryConditionGroup group1 = QueryConditionGroup.and(
                QueryCondition.name("refundstatus").is_null(),
                QueryCondition.name("tran_date").between(sf.format(DateUtils.dateAddDays(DateUtils.getNow(), -dataRange)), sf.format(DateUtils.getNow())),
                //退票检测，不检测内部单
                QueryCondition.name("oppositetype").not_eq(OppositeType.InnerOrg.getValue()),
                QueryCondition.name("accentity").is_not_null()
        );
        QueryConditionGroup group2 = QueryConditionGroup.and(
                QueryCondition.name("refundstatus").is_null(),
                QueryCondition.name("tran_date").between(sf.format(DateUtils.dateAddDays(DateUtils.getNow(), -dataRange)), sf.format(DateUtils.getNow())),
                //退票检测，不检测内部单
                QueryCondition.name("oppositetype").is_null(),
                QueryCondition.name("accentity").is_not_null()
        );
//        if (StringUtils.isNotEmpty(banktype)) {
//            group1.addCondition(QueryCondition.name("banktype").eq(banktype));
//            group2.addCondition(QueryCondition.name("banktype").eq(banktype));
//        }

        QueryConditionGroup group = QueryConditionGroup.or(group1, group2);
        // 添加账户过滤的逻辑
        group.addCondition(QueryCondition.name("bankaccount").in(bankaccountIds));
        querySchema.addCondition(group);
        // 按照交易时间进行排序，取最新的3000笔
        querySchema.addOrderBy(new QueryOrderby("tran_date", "desc"));
        querySchema.addPager(0, 3000);
        list = MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME, querySchema, null);
        return list;
    }

    /**
     * 退票辨识
     *
     * @param checkList
     * @param matchList
     * @return
     */
    public List<BankReconciliation> matchReFundData(List<BankReconciliation> checkList, List<BankReconciliation> matchList) throws Exception {
        List<BankReconciliation> updateList = new ArrayList<>();
        Map<String, Boolean> matchedData = new HashMap();
        Map<String, List<BankReconciliation>> bankAccountCheckGroup = checkList.stream()
                .collect(Collectors.groupingBy(BankReconciliation::getBankaccount));
        Map<String, List<BankReconciliation>> bankAccountMatchGroup = matchList.stream()
                .collect(Collectors.groupingBy(BankReconciliation::getBankaccount));

        for (Map.Entry<String, List<BankReconciliation>> entry : bankAccountCheckGroup.entrySet()) {
            String bankAccount = entry.getKey();
            List<BankReconciliation> bankAccountChecks = entry.getValue();
            List<BankReconciliation> bankAccountMatchs = bankAccountMatchGroup.get(bankAccount);
            if (CollectionUtils.isNotEmpty(bankAccountMatchs)) {
                /*
                 * 匹配逻辑 ----
                 * 循环监测对账单 -- 匹配待确认数据
                 * 根据已匹配对账单map  判断是否已匹配过
                 * 如未匹配过 进行匹配操作
                 * 如匹配到 将双方对账单数据 存入updateList  修改退票状态
                 * 同时将双方id 记录到已匹配对账单map内
                 * O(n2)
                 */
                /**
                 * 1. 先按照收款 匹配 付款
                 * 2.
                 */
                for (int i = 0; i < bankAccountChecks.size(); i++) {
                    log.error("开始匹配第" + i + "条数据==============unique_no：" + bankAccountChecks.get(i).getUnique_no() + "=====bankseqno：" + bankAccountChecks.get(i).getBank_seq_no());
                    // 查看退票字段和原交易流水号字段
                    /**
                     * Ⅰ.银企联返回“退票、原交易流水号”字段同时有值时，依据该交易流水号、本方账号、金额、交易日期查找系统中银行交易明细的交易流水号，
                     * 查找成功后，自动标识两笔流水的退票状态='退票'，且支持在退票确认节点查询；
                     * 查找不成功，则标记该笔数据的退票状态为”疑似退票“
                     */
                    String refundCode = checkRefundByYQL(bankAccountChecks.get(i));
                    if (refundCode == RefundCodeEnum.REFUND_SUCCESS.getCode() &&
                            refundCode == RefundCodeEnum.SUSPECTEDREFUND_SUCCESS.getCode()) {
                        continue;
                    }
                    //循环匹配数据
                    for (int j = 0; j < bankAccountMatchs.size(); j++) {
                        if (BooleanUtils.isTrue(matchedData.get(bankAccountMatchs.get(j).getId()))) {
                            continue;
                        }
                        if (bankAccountChecks.get(i).checkRefund(bankAccountMatchs.get(j))) {
                            bankAccountMatchs.get(j).setEntityStatus(EntityStatus.Update);
                            updateList.add(bankAccountMatchs.get(j));
                            matchedData.put(bankAccountMatchs.get(j).getId(), true);
                        }
                    }
                }
            }
        }
        return updateList;
    }

    /**
     * @param bankReconciliation
     * @return
     * @throws Exception
     */
    public String checkRefundByYQL(BankReconciliation bankReconciliation) throws Exception {
        // 如果银企联退票辨识为true，则根据来源交易流水号进行查询，查询到，则退票状态置为退票，否则置为疑似退票
        if (BooleanUtils.isTrue(bankReconciliation.getRefundFlag())) {
            if (StringUtils.isNotEmpty(bankReconciliation.getOriginBankseqno())) {
                QuerySchema querySchema = QuerySchema.create().addSelect("*");
                QueryConditionGroup group = QueryConditionGroup.and(
                        QueryCondition.name("bank_seq_no").eq(bankReconciliation.getOriginBankseqno()),
                        QueryCondition.name("tran_date").eq(bankReconciliation.getTran_date()),
                        QueryCondition.name("bankaccount").eq(bankReconciliation.getBankaccount()),
                        QueryCondition.name("tran_amt").eq(bankReconciliation.getTran_amt())
                );
                querySchema.addCondition(group);
                List<BankReconciliation> list = MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME, querySchema, null);
                if (CollectionUtils.isNotEmpty(list)) {
                    // 匹配到数据
                    bankReconciliation.setRefundstatus(ReFundType.REFUND.getValue());
                    return RefundCodeEnum.REFUND_SUCCESS.getCode();
                }
            } else {
                bankReconciliation.setRefundstatus(ReFundType.SUSPECTEDREFUND.getValue());
                return RefundCodeEnum.SUSPECTEDREFUND_SUCCESS.getCode();
            }
        }
        return RefundCodeEnum.REFUND_NONE.getCode();
    }


}