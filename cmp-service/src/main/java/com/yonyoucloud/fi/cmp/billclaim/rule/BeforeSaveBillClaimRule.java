package com.yonyoucloud.fi.cmp.billclaim.rule;

import com.google.common.collect.Lists;
import com.yonyou.iuap.bd.base.BdRestSingleton;
import com.yonyou.iuap.bd.pub.param.ConditionVO;
import com.yonyou.iuap.bd.pub.param.Operator;
import com.yonyou.iuap.bd.pub.param.Page;
import com.yonyou.iuap.bd.pub.util.Condition;
import com.yonyou.iuap.bd.pub.util.Sorter;
import com.yonyou.iuap.bd.staff.dto.Staff;
import com.yonyou.iuap.bd.staff.service.itf.IStaffService;
import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.iuap.yms.api.IYmsJdbcApi;
import com.yonyou.iuap.yms.id.generator.YmsOidGenerator;
import com.yonyou.iuap.yms.param.SQLParameter;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.yonbip.ctm.remote.RemoteDubbo;
import com.yonyoucloud.ctm.stwb.openapi.IOpenApiService;
import com.yonyoucloud.ctm.stwb.reconcode.pubitf.ReconciliateCodeGenerator;
import com.yonyoucloud.ctm.stwb.reqvo.IncomeAndExpenditureReqVO;
import com.yonyoucloud.ctm.stwb.respvo.IncomeAndExpenditureResVO;
import com.yonyoucloud.fi.cmp.autoparam.AutoConfig;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliationDetail;
import com.yonyoucloud.fi.cmp.billclaim.BillClaim;
import com.yonyoucloud.fi.cmp.billclaim.BillClaimItem;
import com.yonyoucloud.fi.cmp.cmpentity.*;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.constant.IDomainConstant;
import com.yonyoucloud.fi.cmp.enums.BusinessModel;
import com.yonyoucloud.fi.cmp.enums.FundSplitMethod;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.common.service.IBankReconciliationCommonService;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.Direction;
import com.yonyoucloud.fi.cmp.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.imeta.orm.schema.SimpleCondition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @description: 认领单保存前规则
 * @author: wanxbo@yonyou.com
 * @date: 2022/4/18 15:49
 */

@Slf4j
@Component("beforeSaveBillClaimRule")
public class BeforeSaveBillClaimRule extends AbstractCommonRule {

    @Autowired
    YmsOidGenerator ymsOidGenerator;

    @Resource
    IOpenApiService iOpenApiService;

    @Autowired
    private IBankReconciliationCommonService iBankReconciliationCommonService;

    @Autowired
    @Qualifier("busiBaseDAO")
    protected IYmsJdbcApi ymsJdbcApi;
    public void setYmsJdbcApi(IYmsJdbcApi ymsJdbcApi) {
        this.ymsJdbcApi = ymsJdbcApi;
    }

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        BizObject bill = getBills(billContext, map).get(0);
        //认领类型
        Short billClaimType = bill.getShort("claimtype");

        //调用资金结算财资统一对账码接口生成
        String smartcheckno = RemoteDubbo.get(ReconciliateCodeGenerator.class, IDomainConstant.MDD_DOMAIN_STWB).generate();
        //校验账单认领详情
        List<BizObject> billClaimItems = bill.getBizObjects("items", BizObject.class);
        if (CollectionUtils.isNotEmpty(billClaimItems)) {
            HashSet<Object> refundstatusSet = new HashSet<>();
            for (BizObject item : billClaimItems) {
                Long bankBillId = item.get("bankbill");
                //判断银行对账单状态
                BankReconciliation bankReconciliation = MetaDaoHelper.findById(BankReconciliation.ENTITY_NAME, bankBillId);
                // todo  （1）流水pubts是否一致   （2）金额校验负数
                if (bankReconciliation == null) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100904"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805AF", "银行对账单信息不存在 id:") /* "银行对账单信息不存在 id:" */ + bankBillId);
                }
                //最多有null-初始状态和2-退票两种状态，1-疑似退票的数据不会被发布
                refundstatusSet.add(bankReconciliation.getRefundstatus());
                if (refundstatusSet.size() > 1) {
                    throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540055F", "勾选数据中，存在正常状态流水和退票状态的流水，不支持合并认领，请检查！") /* "勾选数据中，存在正常状态流水和退票状态的流水，不支持合并认领，请检查！" */);
                }

                //判断认领状态
                if (!bankReconciliation.getIspublish()) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100905"), String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805B2", "银行对账单[%s]不是已发布状态") /* "银行对账单[%s]不是已发布状态" */, bankBillId));
                }

                // 认领账户逻辑
                // 是否启用内转协议进行资金切分 启用，且为收入
                if (getInnerTransSplitFlag(bankReconciliation.getAccentity()) && bankReconciliation.getDc_flag().equals(Direction.Credit)) {
                    // 业务模式为普通结算或统收统支
                    if (bill.getShort("businessmodel") != null &&
                            (bill.getShort("businessmodel") == BusinessModel.General_Settlement.getCode()
                                    || bill.getShort("businessmodel") == BusinessModel.Unify_InOut.getCode())) {
                        // 是否切分  切分方式
                        if (bill.get("isfundsplit") != null && bill.getBoolean("isfundsplit") &&
                                bill.getShort("fundsplitmethod") != null && bill.getShort("fundsplitmethod") == FundSplitMethod.InnerAccount_Trans.getCode()
                                && getIsNeedCashSweepBeforeFundSegmentation(bankReconciliation.getAccentity())) {
                            if (!bankReconciliation.getIsimputation()) {
                                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100906"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A050800D0", "依据参数“资金切分是否需要先完成资金归集”设置，银行对账单需先完成资金归集，请检查！") /* "依据参数“资金切分是否需要先完成资金归集”设置，银行对账单需先完成资金归集，请检查！" */);
                            }
                        }
                    }

                }

                // 业务模式为统收统支模式时 统收统支关系组赋值
                if (bill.getShort("businessmodel") != null && bill.getShort("businessmodel") == BusinessModel.Unify_InOut.getCode()) {
                    // 获取统收统支关系组
                    IncomeAndExpenditureReqVO incomeAndExpenditureReqVO = new IncomeAndExpenditureReqVO();
                    List<String> accentityList = new ArrayList<>();
                    List<String> controlList = new ArrayList<>();
                    accentityList.add(bill.get("accentity"));
                    controlList.add(bill.get("actualclaimaccentiry"));
                    incomeAndExpenditureReqVO.setActualAccentity(accentityList);
                    incomeAndExpenditureReqVO.setCurrency(bill.get("currency"));
                    incomeAndExpenditureReqVO.setMarginaccount(bill.getString("bankaccount"));
                    incomeAndExpenditureReqVO.setControllId(controlList);
                    if (Short.valueOf(bill.get("direction") + "") == Direction.Credit.getValue()) {
                        incomeAndExpenditureReqVO.setReauth("1");// 收付类型 1收 2付
                    } else if (Short.valueOf(bill.get("direction") + "") == Direction.Debit.getValue()) {
                        incomeAndExpenditureReqVO.setReauth("2");// 收付类型 1收 2付
                    }
                    List<IncomeAndExpenditureResVO> incomeAndExpenditureResVOS = iOpenApiService.queryControllList(incomeAndExpenditureReqVO);
//                 统收统支关系组赋值
                    if (incomeAndExpenditureResVOS != null && incomeAndExpenditureResVOS.size() == 1) {
                        bill.set("incomeAndExpendRelationGroup", incomeAndExpenditureResVOS.get(0).getId());
                    }
                }

                // 判断业务模式，统收统支和资金代理生成虚拟id
                if (bill.getShort("businessmodel") != null &&
                        (bill.getShort("businessmodel") == BusinessModel.FundCenter_Agent.getCode()
                                || bill.getShort("businessmodel") == BusinessModel.Unify_InOut.getCode())) {
                    bill.set("refbill", ymsOidGenerator.nextId());
                }

                // 1230需求：认领单保存时，如实际认领单位、认领账户为空，则实际认领单位=会计主体，认领账户=银行账户
                if (StringUtils.isEmpty(bill.get("claimaccount"))) {
                    bill.set("claimaccount", bill.getString("bankaccount"));
                }

                // 1230需求：认领单保存时，如实际认领单位、认领账户为空，则实际认领单位=会计主体，认领账户=银行账户
                if (StringUtils.isEmpty(bill.get("actualclaimaccentiry"))) {
                    bill.set("actualclaimaccentiry", bill.get("accentity"));
                }

                // 认领为流水处理支持方式为null时设置为【生单或关联】
                if (!bankReconciliation.getIsparsesmartcheckno()) {
                    bankReconciliation.setIsparsesmartcheckno(BooleanUtils.toBoolean(ReconciliationSupportWayEnum.GENERATION_OR_ASSOCIATION.getValue()));
                }

                //财资统一对账码处理
                //合并场景：合并后生成一个新对账，同时更新交易流水上的统一对账码；新对账码传递至下游
                if (BillClaimType.Merge.getValue() == billClaimType) {
                    bankReconciliation.setSmartcheckno(smartcheckno);
                    bill.set("smartcheckno", smartcheckno);
                }
                //拆分场景：如银行流水有码，则以流水的为准；如无，认领时生成一个码，同时回写交易流水上的对账码，后面部分认领时以该码为准，传递下游
                //整单场景：如银行流水有码，则以流水的为准；如无，则认领时生成一个码
                if ((BillClaimType.Whole.getValue() == billClaimType || BillClaimType.Part.getValue() == billClaimType || BillClaimType.Batch.getValue() == billClaimType)) {
                    if (StringUtils.isEmpty(bankReconciliation.getSmartcheckno())) {
                        bankReconciliation.setSmartcheckno(smartcheckno);
                        bill.set("smartcheckno", smartcheckno);
                    } else {
                        bill.set("smartcheckno", bankReconciliation.getSmartcheckno());
                    }
                }
                //待认领金额
                BigDecimal claimamount = item.get("claimamount");

                //根据银行对账单ID查询是否存在已认领的值
                QuerySchema queryIsExist = QuerySchema.create().addSelect("*");
                QueryConditionGroup conditionGroup = new QueryConditionGroup();
                conditionGroup.addCondition(QueryCondition.name("bankbill").eq(bankBillId));
                conditionGroup.addCondition(QueryCondition.name("mainid").not_eq(bill.get("id")));
                queryIsExist.addCondition(conditionGroup);
                List<Map<String, Object>> claimItems = MetaDaoHelper.query(BillClaimItem.ENTITY_NAME, queryIsExist, null);
                if (CollectionUtils.isNotEmpty(claimItems)) {
                    for (Map<String, Object> claimItem : claimItems) {
                        claimamount = claimamount.add(new BigDecimal(claimItem.get("claimamount").toString()));
                    }
                } else {
                    //处理银行对账单
                    handleBankReconciliationInfo(bankReconciliation, claimamount, bill.getLong("id"),billClaimType);
                    continue;
                }

                // 若是整单认领或者合并认领,账单认领过则返回错误
                if (BillClaimType.Merge.getValue() == billClaimType || BillClaimType.Whole.getValue() == billClaimType) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100907"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805B0", "银行对账单已被认领") /* "银行对账单已被认领" */);
                }

                // 若是部分认领，判断之前是否也是部分认领
                if (BillClaimType.Part.getValue() == billClaimType) {
                    Long billClaimId = (Long) claimItems.get(0).get("mainid");
                    BillClaim billClaim = MetaDaoHelper.findById(BillClaim.ENTITY_NAME, billClaimId);
                    if (billClaim != null && Short.parseShort(billClaim.get("claimtype").toString()) != BillClaimType.Part.getValue()) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100907"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805B0", "银行对账单已被认领") /* "银行对账单已被认领" */);
                    }
                }

                //整单认领和合并认领，认领金额要和待认领相等
                if (bankReconciliation.getTran_amt().compareTo(claimamount) < 0) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100908"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805B1", "待认领金额不匹配") /* "待认领金额不匹配" */);
                }

                //处理银行对账单
                handleBankReconciliationInfo(bankReconciliation, claimamount, bill.getLong("id"), billClaimType);
                //CZFW-483403【DSP支持问题】收款单最后一个审批人审批报错：推送结算检查失败：待结算数据接入异常:第1行对方类型为客户,对方银行账户未在客户档案中<br>，R5没有这个检验，私有化部署的，R6是否可以将这个检验去掉呢？或者在修改对方单位的时候把客户银行账户ID的字段清空，
                boolean isnotSame =
                        (Objects.isNull(bankReconciliation.getOppositeobjectname()) && !Objects.isNull(item.get("oppositeobjectname"))) ||
                                (!Objects.isNull(bankReconciliation.getOppositeobjectname()) && Objects.isNull(item.get("oppositeobjectname"))) ||
                                (Objects.isNull(bankReconciliation.getOppositetype()) && !Objects.isNull(item.get("oppositetype"))) ||
                                (!Objects.isNull(bankReconciliation.getOppositetype()) && Objects.isNull(item.get("oppositetype")));
                if (isnotSame ||
                        (!Objects.isNull(bankReconciliation.getOppositeobjectname()) && !Objects.isNull(item.get("oppositeobjectname")) && !bankReconciliation.getOppositeobjectname().equals(item.get("oppositeobjectname"))) ||
                        (!Objects.isNull(bankReconciliation.getOppositetype()) && !Objects.isNull(item.get("oppositetype")) && !bankReconciliation.getOppositetype().equals(item.get("oppositetype")))) {
                    item.set("toAcct", null);
                }
            }
        }
        IStaffService staffService = BdRestSingleton.getInst(AppContext.getYTenantId(), "diwork",
                AppContext.getCurrentUser().getYhtUserId()).getBdRestService().getStaffService();
        Condition condition = new Condition();
        List<ConditionVO> conditionVOList = new ArrayList<>(1);
        ConditionVO conditionVO = new ConditionVO("user_id", AppContext.getCurrentUser().getYhtUserId(), Operator.EQUAL);
        conditionVOList.add(conditionVO);
        condition.setConditionList(conditionVOList);
        Page<Staff> pageList = staffService.pagination(condition, new Sorter(), 1, 1);
        // 对应员工
        if (null != pageList && null != pageList.getContent() && pageList.getContent().size() > 0 && null != pageList.getContent().get(0) && Objects.isNull(bill.get("claimperson"))) {
            bill.set("claimperson", pageList.getContent().get(0).getId());
        }
        bill.set("recheckstatus", RecheckStatus.Saved.getValue());
        return new RuleExecuteResult();
    }

    /**
     * 处理银行对账单 认领金额，认领状态
     *
     * @param bankReconciliation 银行对账单信息
     * @param claimamount        认领金额
     */
    private void handleBankReconciliationInfo(BankReconciliation bankReconciliation, BigDecimal claimamount, Long claimid,Short billClaimType) throws Exception {
        String sql;
        //银行对账单状态更新 认领金额要相加
        SQLParameter params = new SQLParameter();
        //设置待认领金额
        BigDecimal amounttobeclaimed;
        short billclaimstatus;
        params.addParam(claimamount);
        if(billClaimType == BillClaimType.Part.getValue()){
            amounttobeclaimed = bankReconciliation.getTran_amt().subtract(claimamount);
            // 待认领金额为0，则认完成
            if (amounttobeclaimed.compareTo(BigDecimal.ZERO) == 0) {
                billclaimstatus = BillClaimStatus.Claimed.getValue();
            } else {
                billclaimstatus = BillClaimStatus.ToBeClaim.getValue();
            }
            params.addParam(amounttobeclaimed);
            params.addParam(billclaimstatus);
            params.addParam(bankReconciliation.getId().toString());
            params.addParam(InvocationInfoProxy.getTenantid());
            sql = "update cmp_bankreconciliation set claimamount = ?,amounttobeclaimed = ?, billclaimstatus = ? where id = ? and ytenant_id = ?";
        }else {
            billclaimstatus = BillClaimStatus.Claimed.getValue();
            amounttobeclaimed = new BigDecimal(0);
            params.addParam(amounttobeclaimed);
            params.addParam(billclaimstatus);
            params.addParam(bankReconciliation.getId().toString());
            params.addParam(InvocationInfoProxy.getTenantid());
            params.addParam(claimamount);
            sql = "update cmp_bankreconciliation set claimamount = ?,amounttobeclaimed = ?, billclaimstatus = ? where id = ? and ytenant_id = ? and tran_amt - ? = 0";
        }
        int count = ymsJdbcApi.update(sql,params);
        if(count == 0){
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400560", "当前流水可能存在重复认领，请刷新后重新操作！") /* "当前流水可能存在重复认领，请刷新后重新操作！" */);
        }

        //认领后回写银行流水子表新增一条信息
        QuerySchema querySchema = QuerySchema.create().addSelect("id");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("mainid").eq(bankReconciliation.getId()));
        conditionGroup.appendCondition(QueryCondition.name("claimid").eq(claimid));
        querySchema.addCondition(conditionGroup);
        List<BankReconciliationDetail> idList = MetaDaoHelper.queryObject(BankReconciliationDetail.ENTITY_NAME, querySchema, null);
        if (idList != null && idList.size() > 0) {
            MetaDaoHelper.batchDelete(BankReconciliationDetail.ENTITY_NAME, Lists.newArrayList(new SimpleCondition("id",
                    ConditionOperator.in, idList.stream().map(BankReconciliationDetail::getId).collect(Collectors.toList()))));
        }
        iBankReconciliationCommonService.insertBankreconciliationDetailNew(bankReconciliation,
                OprType.Claim.getValue(), null, claimid);
    }

    //是否启用内转协议进行资金切分 参数是否启用
    private Boolean getInnerTransSplitFlag(String accentity) throws Exception {
        //根据会计主体查询配置的现金参数-是否启用内转协议进行资金切分参数
        QuerySchema querySchema1 = QuerySchema.create().addSelect("*");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("accentity.code").eq("global00"));
        querySchema1.addCondition(conditionGroup);
        List<AutoConfig> configList = MetaDaoHelper.queryObject(AutoConfig.ENTITY_NAME, querySchema1, null);
        if (configList == null || configList.size() == 0) {
            return Boolean.FALSE;
        } else {
            return configList.get(0).getIsEnableInterTransAgreeFundSplitting() == null ? Boolean.FALSE : configList.get(0).getIsEnableInterTransAgreeFundSplitting();
        }
    }

    //资金切分是否需要先完成资金归集 参数是否启用
    private Boolean getIsNeedCashSweepBeforeFundSegmentation(String accentity) throws Exception {
        //资金切分是否需要先完成资金归集
        QuerySchema querySchema1 = QuerySchema.create().addSelect("*");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("accentity.code").eq("global00"));
        querySchema1.addCondition(conditionGroup);
        List<AutoConfig> configList = MetaDaoHelper.queryObject(AutoConfig.ENTITY_NAME, querySchema1, null);
        if (configList == null || configList.size() == 0) {
            return Boolean.FALSE;
        } else {
            return configList.get(0).getIsNeedCashSweepBeforeFundSegmentation();
        }
    }
}
