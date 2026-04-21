package com.yonyoucloud.fi.cmp.ctmrpc.billclaim;

import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankreconciliationService;
import com.yonyoucloud.fi.cmp.billclaim.BillClaim;
import com.yonyoucloud.fi.cmp.billclaim.BillClaimItem;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.api.billclaim.CtmCmpBillClaimRpcService;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.common.CommonRequestDataVo;
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.billclaim.BillClaimVO;

/**
 * 根据条件查询认领单
 */
@Service
public class CtmCmpBillClaimRpcServiceImpl implements CtmCmpBillClaimRpcService {


    @Autowired
    BankreconciliationService bankreconciliationService;

    @Override
    public List<BillClaimVO> queryBillClaimByCondition(CommonRequestDataVo commonRequestDataVo) throws Exception {
        List<BillClaimVO> billClaimVos = new ArrayList();
        // 根据条件查询认领单
        QuerySchema query = QuerySchema.create().addSelect(ICmpConstant.SELECT_TOTAL_PARAM);
        QueryConditionGroup group = new QueryConditionGroup();
        if (commonRequestDataVo.getIds() != null && CollectionUtils.isNotEmpty(commonRequestDataVo.getIds())) {
            group.appendCondition(QueryConditionGroup.and(QueryCondition.name("id").in(commonRequestDataVo.getIds())));
        }
        if (commonRequestDataVo.getCode() != null) {
            group.appendCondition(QueryConditionGroup.and(QueryCondition.name("code").in(commonRequestDataVo.getCode())));
        }
        query.addCondition(group);
        List<BillClaim> billClaims = MetaDaoHelper.queryObject(BillClaim.ENTITY_NAME, query, null);

        if (CollectionUtils.isNotEmpty(billClaims)) {
            for (BillClaim billClaim : billClaims) {
                BillClaimVO billClaimVO = new BillClaimVO();
                billClaimVO.setSmartcheckno(billClaim.getSmartcheckno());
                billClaimVO.setCode(billClaim.getCode());
                billClaimVO.setId(billClaim.getId().toString());
                billClaimVO.setVouchdate(billClaim.getVouchdate());
                billClaimVO.setTotalamount(billClaim.getTotalamount());
                billClaimVO.setAccentity_name(billClaim.getAccentity());
                billClaimVos.add(billClaimVO);
            }
        }
        return billClaimVos;
    }

    /**
     * 不确定查询流水还是认领单还是参照关联
     *
     * @param commonRequestDataVo
     * @return
     * @throws Exception
     */
    @Override
    public List<BillClaimVO> queryBillClaimUncertain(CommonRequestDataVo commonRequestDataVo) throws Exception {
        List<BillClaimVO> billClaimVos = new ArrayList();
        // 根据条件查询认领单
        QuerySchema queryBankReconciliation = QuerySchema.create().addSelect(ICmpConstant.SELECT_TOTAL_PARAM);
        QueryConditionGroup groupBankReconciliation = new QueryConditionGroup();
        if (commonRequestDataVo.getIds() != null && CollectionUtils.isNotEmpty(commonRequestDataVo.getIds())) {
            groupBankReconciliation.appendCondition(QueryConditionGroup.and(QueryCondition.name("id").in(commonRequestDataVo.getIds())));
        }
        if (commonRequestDataVo.getCode() != null) {
            groupBankReconciliation.appendCondition(QueryConditionGroup.and(QueryCondition.name("code").in(commonRequestDataVo.getCode())));
        }
        queryBankReconciliation.addCondition(groupBankReconciliation);
        // 先查询流水
        // 再查询认领单
        // 最后查询参照关联
        List<BankReconciliation> bankReconciliations = MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME, queryBankReconciliation, null);
        if (CollectionUtils.isNotEmpty(bankReconciliations)) {
            for (BankReconciliation bankReconciliation : bankReconciliations) {
                BillClaimVO billClaimVO = new BillClaimVO();
                //财资统一对账码
                billClaimVO.setSmartcheckno(bankReconciliation.getSmartcheckno());
                //入账类型
                if (bankReconciliation.getEntrytype() != null) {
                    billClaimVO.setEntrytype(bankReconciliation.getEntrytype());
                }
                // 账户使用组织
                billClaimVO.setAccentity(bankReconciliation.getAccentity());
                // 退票状态 1-退票，0-未退票
                if (bankReconciliation.getRefundstatus() == null) {
                    billClaimVO.setRefundstatus((short)0);
                } else {
                    billClaimVO.setRefundstatus((short) (bankReconciliation.getRefundstatus() == 2 ? 1 : 0));
                }
                //银行账号
                billClaimVO.setBankaccount(bankReconciliation.getBankaccount());
                // 核算会计主体
                billClaimVO.setAccentityRaw(bankReconciliation.getAccentityRaw());
                if (bankReconciliation.getReceiptassociation() == 0 || bankReconciliation.getReceiptassociation() == 1) {
                    billClaimVO.setReceiptFileId(bankreconciliationService.getBankReceiptFileId(bankReconciliation));
                }
                // 交易日期
                billClaimVO.setTranDate(bankReconciliation.getTran_date());
                //交易时间
                billClaimVO.setTranTime(bankReconciliation.getTran_time());
                // 交易金额
                billClaimVO.setTotalamount(bankReconciliation.getTran_amt());
                // 流水id
                billClaimVO.setId(bankReconciliation.getId().toString());
                // 是否本单位
                billClaimVO.setBOurOrg(true);
                // 单据编码 - 银行流水号
                billClaimVO.setCode(bankReconciliation.getBank_seq_no());
                // 单据类型-121银行流水处理，122 认领单
                billClaimVO.setBilltype((short)121);
                billClaimVos.add(billClaimVO);
            }
        } else {

            // 根据条件查询认领单
            QuerySchema queryBillClaimRef = QuerySchema.create().addSelect(ICmpConstant.SELECT_TOTAL_PARAM);
            QueryConditionGroup groupBillClaimRef = new QueryConditionGroup();
            if (commonRequestDataVo.getIds() != null && CollectionUtils.isNotEmpty(commonRequestDataVo.getIds())) {
                groupBillClaimRef.appendCondition(QueryConditionGroup.and(QueryCondition.name("id").in(commonRequestDataVo.getIds())));
            }
            if (commonRequestDataVo.getCode() != null) {
                groupBillClaimRef.appendCondition(QueryConditionGroup.and(QueryCondition.name("code").in(commonRequestDataVo.getCode())));
            }
            queryBillClaimRef.addCondition(groupBillClaimRef);
            List<BillClaim> billClaims = MetaDaoHelper.queryObject(BillClaim.ENTITY_NAME, queryBillClaimRef, null);
            //判断是否是关联参照数据
            boolean refBillFlag = false;
            if (CollectionUtils.isEmpty(billClaims)) {
                // 根据条件查询认领单
                QuerySchema queryRef = QuerySchema.create().addSelect(ICmpConstant.SELECT_TOTAL_PARAM);
                QueryConditionGroup groupRef = new QueryConditionGroup();
                if (commonRequestDataVo.getIds() != null && CollectionUtils.isNotEmpty(commonRequestDataVo.getIds())) {
                    groupRef.appendCondition(QueryConditionGroup.and(QueryCondition.name("refbill").in(commonRequestDataVo.getIds())));
                }
                queryRef.addCondition(groupRef);
                billClaims = MetaDaoHelper.queryObject(BillClaim.ENTITY_NAME, queryRef, null);
                refBillFlag = true;
            }
            if (CollectionUtils.isNotEmpty(billClaims)) {
                for (BillClaim billClaim : billClaims) {
                    BillClaimVO billClaimVO = new BillClaimVO();
                    billClaimVO.setSmartcheckno(billClaim.getSmartcheckno());
                    billClaimVO.setCode(billClaim.getCode());
                    billClaimVO.setVouchdate(billClaim.getVouchdate());
                    // 实际认领账户
                    billClaimVO.setEnterpriseBankAccount(billClaim.getClaimaccount());
                    // 统收统支关系组
                    if(billClaim.getIncomeAndExpendRelationGroup() != null){
                        billClaimVO.setIncomeAndExpendRelationGroup(billClaim.getIncomeAndExpendRelationGroup().toString());
                    }
                    // 银行账户
                    billClaimVO.setBankaccount(billClaim.getBankaccount());
                    // 总金额
                    billClaimVO.setTotalamount(billClaim.getTotalamount());
                    // 账户使用组织
                    billClaimVO.setAccentity(billClaim.getAccentity());
                    // 核算会计主体
                    billClaimVO.setAccentityRaw(billClaim.getAccentityRaw());
                    // 实际认领单位
                    billClaimVO.setActualclaimaccentiry(billClaim.getActualclaimaccentiry());
                    // 业务模式
                    if(billClaim.getBusinessmodel() != null){
                        billClaimVO.setBusinessmodel(billClaim.getBusinessmodel());
                    }
                    //查找子表数据取出对账单id
                    QuerySchema querySchema = QuerySchema.create().addSelect("*");
                    QueryConditionGroup groupItem = QueryConditionGroup.and(QueryCondition.name("mainid").eq(billClaim.getId()));
                    querySchema.addCondition(groupItem);
                    List<BillClaimItem> list = MetaDaoHelper.queryObject(BillClaimItem.ENTITY_NAME, querySchema, null);
                    List<String> bankreconciliationids = new ArrayList<>();
                    for(BillClaimItem billClaimItem: list){
                        bankreconciliationids.add(billClaimItem.getBankbill().toString());
                    }
                    List<BankReconciliation> allBankReconciliations = new ArrayList<>();
                    if (CollectionUtils.isNotEmpty(bankreconciliationids)) {
                        QuerySchema queryByIds = QuerySchema.create().addSelect(ICmpConstant.SELECT_TOTAL_PARAM);
                        QueryConditionGroup idGroup = new QueryConditionGroup();
                        idGroup.appendCondition(QueryConditionGroup.and(QueryCondition.name("id").in(bankreconciliationids)));
                        queryByIds.addCondition(idGroup);
                        allBankReconciliations = MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME, queryByIds, null);
                    }
                    allBankReconciliations.stream()
                            .filter(item -> item.getTran_date() != null)
                            .max(Comparator.comparing(BankReconciliation::getTran_date))
                            .ifPresent(item -> billClaimVO.setTranDate(item.getTran_date()));
                    allBankReconciliations.stream()
                            .filter(item -> item.getTran_time() != null)
                            .max(Comparator.comparing(BankReconciliation::getTran_time))
                            .ifPresent(item -> billClaimVO.setTranTime(item.getTran_time()));
                    if (billClaim.getActualclaimaccentiry() == null) {
                        billClaimVO.setBOurOrg(true);
                        billClaimVO.setId(billClaim.getId().toString());
                    } else {
                        if (billClaim.getAccentity().equals(billClaim.getActualclaimaccentiry())) {
                            billClaimVO.setBOurOrg(true);
                            billClaimVO.setId(billClaim.getId().toString());
                        } else {
                            billClaimVO.setBOurOrg(false);
                            if (!refBillFlag){
                                billClaimVO.setId(billClaim.getId().toString());
                            }else {
                                if(commonRequestDataVo.getIds().contains(billClaim.getRefbill())){
                                    billClaimVO.setId(billClaim.getRefbill());
                                }else {
                                    billClaimVO.setId(billClaim.getId().toString());
                                }
                            }
                        }
                    }
                    // 单据类型-121银行流水处理，122 认领单
                    billClaimVO.setBilltype((short)122);
                    billClaimVos.add(billClaimVO);
                }
            } else {
                throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400590", "未查询到对应认领单或流水，请求id：") /* "未查询到对应认领单或流水，请求id：" */ + commonRequestDataVo.getIds().toString());
            }
        }
        return billClaimVos;
    }
}
