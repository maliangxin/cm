package com.yonyoucloud.fi.cmp.ctmrpc;

import com.yonyou.cloud.yts.YtsContext;
import com.yonyou.ucf.mdd.common.enums.OperationTypeEnum;
import com.yonyou.ucf.mdd.common.model.ResultList;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyoucloud.fi.basecom.service.FIBillService;
import com.yonyoucloud.fi.cmp.autoparam.service.AutoConfigService;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.bankreconciliation.CommonSaveUtils;
import com.yonyoucloud.fi.cmp.billclaim.BillClaim;
import com.yonyoucloud.fi.cmp.billclaim.BillClaimItem;
import com.yonyoucloud.fi.cmp.cmpentity.BillClaimStatus;
import com.yonyoucloud.fi.cmp.cmpentity.BillClaimType;
import com.yonyoucloud.fi.cmp.cmpentity.RecheckStatus;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.enums.ConfirmStatusEnum;
import com.yonyoucloud.fi.cmp.event.utils.DetermineUtils;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.api.bankrecilication.CmpBillClaimCenterRpcService;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.claimcenter.TotalClaimConfigVO;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.claimcenter.TotalClaimRequestVO;
import com.yonyoucloud.fi.cmp.util.EntityTool;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

/**
 * <h1>CmpBillClaimCenterRpcServiceImpl</h1>
 *
 * @author Sun GuoCai
 * @version 1.0
 * @since 2024-04-22 15:42
 */
@Slf4j
@Service
@AllArgsConstructor
public class CmpBillClaimCenterRpcServiceImpl implements CmpBillClaimCenterRpcService {

    private final AutoConfigService autoConfigService;

    private final FIBillService fiBillService;

    /**
     * <h2>提供到账认领中心，批量认领保存RPC接口，供项目客开调用</h2>
     *
     * @param totalClaimRequestVOList : 入参
     * @return java.util.Map<java.lang.String, java.lang.Object>
     * @author Sun GuoCai
     * @date 2024/4/22 16:00
     */
    @Override
    @Transactional
    public Map<String, Object> totalClaimByBankSeqNo(List<TotalClaimRequestVO> totalClaimRequestVOList) throws Exception {
        DetermineUtils.isTure(ValueUtils.isNotEmptyObj(totalClaimRequestVOList)).throwMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400617", "请求参数不能为空！") /* "请求参数不能为空！" */);
        List<BillClaim> billClaimList = new ArrayList<>();
        Boolean isRecheck = autoConfigService.getIsRecheck();
        List<Object> messages = Collections.synchronizedList(new ArrayList<>());  // 失败原因列表
        int count = 0;
        int failCount = 0;
        ResultList result = new ResultList();
        List<String> bankReconciliationIdList = new ArrayList<>();
        List<String> billClaimIdList = new ArrayList<>();
        for (TotalClaimRequestVO totalClaimRequestVO : totalClaimRequestVOList) {
            DetermineUtils.isTure(ValueUtils.isNotEmptyObj(totalClaimRequestVO.getId())).throwMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400619", "银行交易流水ID不能为空！") /* "银行交易流水ID不能为空！" */);
            DetermineUtils.isTure(ValueUtils.isNotEmptyObj(totalClaimRequestVO.getRemark())).throwMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540061A", "认领说明不能为空！") /* "认领说明不能为空！" */);
            DetermineUtils.isTure(ValueUtils.isNotEmptyObj(totalClaimRequestVO.getClaimstaff())).throwMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540061C", "认领人不能为空！") /* "认领人不能为空！" */);

            BizObject bankReconciliationMap = MetaDaoHelper.findById(BankReconciliation.ENTITY_NAME, totalClaimRequestVO.getId());
            if (ValueUtils.isEmpty(bankReconciliationMap)) {
                count += 1;
                failCount += 1;
                String messageError = String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540061E", "银行交易流水ID： %s 未查询到数据！请检查") /* "银行交易流水ID： %s 未查询到数据！请检查" */, totalClaimRequestVO.getId());
                messages.add(messageError);
            } else {
                bankReconciliationIdList.add(totalClaimRequestVO.getId());
                BillClaim billClaim = new BillClaim();
                // 是否需要复核
                if (isRecheck) {
                    billClaim.set("recheckstatus", RecheckStatus.NotReviewed.getValue());
                } else {
                    billClaim.set("recheckstatus", RecheckStatus.Reviewed.getValue());
                    billClaim.set("recheckstaff", totalClaimRequestVO.getClaimstaff());
                    billClaim.set("recheckdate", new Date());
                }
                //认领日期
                billClaim.setVouchdate(new Date());
                // 认领人
                billClaim.set("claimstaff", totalClaimRequestVO.getClaimstaff());
                // 认领说明
                billClaim.set("remark", totalClaimRequestVO.getRemark());
                // 手工生单类型
                billClaim.set("manualgenertbilltype", totalClaimRequestVO.getManualgenertbilltype());
                // 认领类型
                billClaim.set("claimtype", BillClaimType.Whole.getValue());
                // 认领金额
                billClaim.set("totalamount", bankReconciliationMap.get("amounttobeclaimed"));
                // 会计主体
                billClaim.set("accentity", bankReconciliationMap.get("accentity"));
                billClaim.set("actualsettleaccentity", bankReconciliationMap.get("accentity"));

                // 业务单元
                billClaim.set("org", bankReconciliationMap.get("accentity"));
                billClaim.set("direction", bankReconciliationMap.get("dc_flag"));
                // 所属组织
                billClaim.set("affiliatedorgid", bankReconciliationMap.get("orgid"));
                // 币种
                billClaim.set("currency", bankReconciliationMap.get("currency"));
                // 银行账户
                billClaim.set("bankaccount", bankReconciliationMap.get("bankaccount"));

                // 对方账号
                billClaim.set("toaccountno", bankReconciliationMap.get("to_acct_no"));
                billClaim.set("toaccountname", bankReconciliationMap.get("to_acct_name"));
                billClaim.set("toaccountbank", bankReconciliationMap.get("to_acct_bank"));
                billClaim.set("toaccountbankname", bankReconciliationMap.get("to_acct_bank_name"));
                billClaim.set("entrytype", bankReconciliationMap.get("entrytype"));
                billClaim.set("isinneraccounting", bankReconciliationMap.get("isinneraccounting"));
                billClaim.set("impinneraccount", bankReconciliationMap.get("impinneraccount"));
                billClaim.set("_status", EntityStatus.Insert);
                // 提前入账
                billClaim.set("earlyentry", bankReconciliationMap.get("isadvanceaccounts"));
                // 统收统支标识
                billClaim.set("isincomeandexpenditure", false);
                billClaim.set("vouchdate", ValueUtils.isNotEmptyObj(bankReconciliationMap.get("vouchdate"))
                        ? bankReconciliationMap.get("vouchdate") : new Date());

                BillClaimItem item = new BillClaimItem();
                item.set("bankbill", bankReconciliationMap.get("id"));
                // 会计主体
                item.set("accentity", bankReconciliationMap.get(" accentity"));
                // 银行账户
                item.set("bankaccount", bankReconciliationMap.get("bankaccount"));
                // 交易日期
                item.set("tran_date", bankReconciliationMap.get("tran_date"));
                item.set("tran_amt", bankReconciliationMap.get("tran_amt"));
                // 币种
                item.set("currency", bankReconciliationMap.get("currency"));
                // 待认领金额
                item.set("amounttobeclaimed", bankReconciliationMap.get("amounttobeclaimed"));
                // 借贷方向
                item.set("direction", bankReconciliationMap.get("dc_flag"));
                // 对方账号
                item.set("to_acct_no", bankReconciliationMap.get("to_acct_no"));
                // 对方类型
                item.set("oppositetype", bankReconciliationMap.get("oppositetype"));
                // 对方单位
                item.set("oppositeobjectname", bankReconciliationMap.get("oppositeobjectname"));
                // 对方单位
                item.set("oppositeobjectid", bankReconciliationMap.get("oppositeobjectid"));
                // 对方账号
                item.set("to_acct_no", bankReconciliationMap.get("to_acct_no"));
                item.set("to_acct_name", bankReconciliationMap.get("to_acct_name"));
                item.set("to_acct_bank", bankReconciliationMap.get("to_acct_bank"));
                item.set("to_acct_bank_name", bankReconciliationMap.get("to_acct_bank_name"));
                // 备注
                item.set("remark", bankReconciliationMap.get("remark"));
                // 认领金额
                item.set("claimamount", bankReconciliationMap.get("amounttobeclaimed"));
                item.set("totalamount", bankReconciliationMap.get("amounttobeclaimed"));
                item.set("refassociationstatus", totalClaimRequestVO.getRefAssociationStatus()!=null
                        ? totalClaimRequestVO.getRefAssociationStatus() : 0);
                item.set("_status", EntityStatus.Insert);
                billClaim.set("items", item);
                billClaimList.add(billClaim);
            }
        }

        Map<String, Object> data = new HashMap<>();
        if (CollectionUtils.isEmpty(billClaimList)) {
            result.setCount(count);
            result.setFailCount(failCount);
            result.setInfos(messages);
            data.put("data", result);
        } else {
            BillDataDto dataDto = new BillDataDto();
            dataDto.setData(billClaimList);
            dataDto.setAction(ICmpConstant.SAVE);
            dataDto.setBillnum(IBillNumConstant.CMP_BILLCLAIM_CARD);
            result = fiBillService.batchSave(dataDto);
            List<Object> infos = result.getInfos();
            List<Object> infosNew = new ArrayList<>();
            if (CollectionUtils.isNotEmpty(infos)) {
                for (Object info : infos) {
                    Map<String, Object> infoMap = (Map<String, Object>) info;
                    Map<String, Object> infoMapNew = new HashMap<>();
                    billClaimIdList.add(infoMap.get("id").toString());
                    infoMapNew.put("id", infoMap.get("id"));
                    infoMapNew.put("code", infoMap.get("code"));
                    infoMapNew.put("accentity", infoMap.get("accentity"));
                    infoMapNew.put("totalamount", infoMap.get("totalamount"));
                    infosNew.add(infoMapNew);
                }
                result.setInfos(infosNew);
            }

            result.setCount(result.getCount() + count);
            result.setFailCount(result.getFailCount() + failCount);
            if (CollectionUtils.isNotEmpty(messages)) {
                result.getMessages().addAll(messages);
            }
            data.put("data", result);
        }
        rollBackData(billClaimIdList, bankReconciliationIdList, false);

        return data;
    }

    /**
     * <h2>提供到账认领中心，批量认领保存RPC接口，供项目客开调用</h2>
     *
     * @param totalClaimRequestVOList : 入参
     * @param totalClaimConfigVO
     * @return java.util.Map<java.lang.String, java.lang.Object>
     * @author Sun GuoCai
     * @date 2024/4/22 16:00
     */
    @Override
    @Transactional
    public Map<String, Object> totalClaimByBankSeqNoNew(List<TotalClaimRequestVO> totalClaimRequestVOList,
                                                        TotalClaimConfigVO totalClaimConfigVO) throws Exception {
        DetermineUtils.isTure(ValueUtils.isNotEmptyObj(totalClaimRequestVOList)).throwMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400617", "请求参数不能为空！") /* "请求参数不能为空！" */);
        boolean saveAndCommit =
                Objects.isNull(totalClaimConfigVO) || Objects.isNull(totalClaimConfigVO.getAutocommit()) || totalClaimConfigVO.getAutocommit().equals("0");
        List<BillClaim> billClaimList = new ArrayList<>();
        Boolean isRecheck = autoConfigService.getIsRecheck();
        List<Object> messages = Collections.synchronizedList(new ArrayList<>());  // 失败原因列表
        int count = 0;
        int failCount = 0;
        ResultList result = new ResultList();
        List<String> bankReconciliationIdList = new ArrayList<>();
        List<String> billClaimIdList = new ArrayList<>();
        for (TotalClaimRequestVO totalClaimRequestVO : totalClaimRequestVOList) {
            DetermineUtils.isTure(ValueUtils.isNotEmptyObj(totalClaimRequestVO.getId())).throwMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400619", "银行交易流水ID不能为空！") /* "银行交易流水ID不能为空！" */);
            DetermineUtils.isTure(ValueUtils.isNotEmptyObj(totalClaimRequestVO.getRemark())).throwMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540061A", "认领说明不能为空！") /* "认领说明不能为空！" */);
            DetermineUtils.isTure(ValueUtils.isNotEmptyObj(totalClaimRequestVO.getClaimstaff())).throwMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540061C", "认领人不能为空！") /* "认领人不能为空！" */);

            BizObject bankReconciliationMap = MetaDaoHelper.findById(BankReconciliation.ENTITY_NAME, totalClaimRequestVO.getId());
            if (ValueUtils.isEmpty(bankReconciliationMap)) {
                count += 1;
                failCount += 1;
                String messageError = String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540061E", "银行交易流水ID： %s 未查询到数据！请检查") /* "银行交易流水ID： %s 未查询到数据！请检查" */, totalClaimRequestVO.getId());
                messages.add(messageError);
            } else if (ValueUtils.isNotEmpty(bankReconciliationMap) && bankReconciliationMap.get(
                    "claimamount") != null && ValueUtils.isNotEmpty(bankReconciliationMap.get(
                    "claimamount").toString()) && bankReconciliationMap.getBigDecimal(
                    "claimamount").compareTo(BigDecimal.ZERO) > 0) {
                count += 1;
                failCount += 1;
                String messageError = String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400618", "银行交易流水ID： %s 已有认领数据！请检查") /* "银行交易流水ID： %s 已有认领数据！请检查" */, totalClaimRequestVO.getId());
                messages.add(messageError);
            } else if (ValueUtils.isNotEmpty(bankReconciliationMap) && bankReconciliationMap.get(
                    "confirmstatus") != null && ValueUtils.isNotEmpty(bankReconciliationMap.get(
                    "confirmstatus").toString()) && !ConfirmStatusEnum.Confirmed.getIndex().equals(bankReconciliationMap.getString(
                    "confirmstatus"))) {
                count += 1;
                failCount += 1;
                String messageError = String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540061B", "银行交易流水ID： %s 使用组织待确认！请检查") /* "银行交易流水ID： %s 使用组织待确认！请检查" */, totalClaimRequestVO.getId());
                messages.add(messageError);
            } else if (ValueUtils.isNotEmpty(bankReconciliationMap) && bankReconciliationMap.get(
                    "ispublish") != null && !bankReconciliationMap.getBoolean("ispublish")) {
                count += 1;
                failCount += 1;
                String messageError = String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540061D", "银行交易流水ID： %s 不是已发布状态！请检查") /* "银行交易流水ID： %s 不是已发布状态！请检查" */, totalClaimRequestVO.getId());
                messages.add(messageError);
            } else {
                bankReconciliationIdList.add(totalClaimRequestVO.getId());
                BillClaim billClaim = new BillClaim();
                // 是否需要复核
                if (isRecheck) {
                    billClaim.set("recheckstatus", RecheckStatus.NotReviewed.getValue());
                } else {
                    billClaim.set("recheckstatus", RecheckStatus.Reviewed.getValue());
                    billClaim.set("recheckstaff", totalClaimRequestVO.getClaimstaff());
                    billClaim.set("recheckdate", new Date());
                }
                //认领日期
                billClaim.setVouchdate(new Date());
                // 认领人
                billClaim.set("claimstaff", totalClaimRequestVO.getClaimstaff());
                // 认领说明
                billClaim.set("remark", totalClaimRequestVO.getRemark());
                // 手工生单类型
                billClaim.set("manualgenertbilltype", totalClaimRequestVO.getManualgenertbilltype());
                // 认领类型
                billClaim.set("claimtype", BillClaimType.Whole.getValue());
                // 认领金额
                billClaim.set("totalamount", bankReconciliationMap.get("amounttobeclaimed"));
                // 会计主体
                billClaim.set("accentity", bankReconciliationMap.get("accentity"));
                billClaim.set("actualsettleaccentity", bankReconciliationMap.get("accentity"));

                // 业务单元
                billClaim.set("org", bankReconciliationMap.get("accentity"));
                billClaim.set("direction", bankReconciliationMap.get("dc_flag"));
                // 所属组织
                billClaim.set("affiliatedorgid", bankReconciliationMap.get("orgid"));
                // 币种
                billClaim.set("currency", bankReconciliationMap.get("currency"));
                // 银行账户
                billClaim.set("bankaccount", bankReconciliationMap.get("bankaccount"));

                // 对方账号
                billClaim.set("toaccountno", bankReconciliationMap.get("to_acct_no"));
                billClaim.set("toaccountname", bankReconciliationMap.get("to_acct_name"));
                billClaim.set("toaccountbank", bankReconciliationMap.get("to_acct_bank"));
                billClaim.set("toaccountbankname", bankReconciliationMap.get("to_acct_bank_name"));
                billClaim.set("entrytype", bankReconciliationMap.get("entrytype"));
                billClaim.set("isinneraccounting", bankReconciliationMap.get("isinneraccounting"));
                billClaim.set("impinneraccount", bankReconciliationMap.get("impinneraccount"));
                billClaim.set("_status", EntityStatus.Insert);
                // 提前入账
                billClaim.set("earlyentry", bankReconciliationMap.get("isadvanceaccounts"));
                // 统收统支标识
                billClaim.set("isincomeandexpenditure", false);

                BillClaimItem item = new BillClaimItem();
                item.set("bankbill", bankReconciliationMap.get("id"));
                // 会计主体
                item.set("accentity", bankReconciliationMap.get(" accentity"));
                // 银行账户
                item.set("bankaccount", bankReconciliationMap.get("bankaccount"));
                // 交易日期
                item.set("tran_date", bankReconciliationMap.get("tran_date"));
                item.set("tran_amt", bankReconciliationMap.get("tran_amt"));
                // 币种
                item.set("currency", bankReconciliationMap.get("currency"));
                // 待认领金额
                item.set("amounttobeclaimed", bankReconciliationMap.get("amounttobeclaimed"));
                // 借贷方向
                item.set("direction", bankReconciliationMap.get("dc_flag"));
                // 对方账号
                item.set("to_acct_no", bankReconciliationMap.get("to_acct_no"));
                // 对方类型
                item.set("oppositetype", bankReconciliationMap.get("oppositetype"));
                // 对方单位
                item.set("oppositeobjectname", bankReconciliationMap.get("oppositeobjectname"));
                // 对方单位
                item.set("oppositeobjectid", bankReconciliationMap.get("oppositeobjectid"));
                // 对方账号
                item.set("to_acct_no", bankReconciliationMap.get("to_acct_no"));
                item.set("to_acct_name", bankReconciliationMap.get("to_acct_name"));
                item.set("to_acct_bank", bankReconciliationMap.get("to_acct_bank"));
                item.set("to_acct_bank_name", bankReconciliationMap.get("to_acct_bank_name"));
                // 备注
                item.set("remark", bankReconciliationMap.get("remark"));
                // 认领金额
                item.set("claimamount", bankReconciliationMap.get("amounttobeclaimed"));
                item.set("totalamount", bankReconciliationMap.get("amounttobeclaimed"));
                item.set("refassociationstatus", totalClaimRequestVO.getRefAssociationStatus()!=null
                        ? totalClaimRequestVO.getRefAssociationStatus() : 0);
                item.set("_status", EntityStatus.Insert);
                billClaim.set("items", item);
                billClaimList.add(billClaim);
            }
        }

        Map<String, Object> data = new HashMap<>();
        if (CollectionUtils.isEmpty(billClaimList)) {
            result.setCount(count);
            result.setFailCount(failCount);
            result.setInfos(messages);
            data.put("data", result);
        } else {
            BillDataDto dataDto = new BillDataDto();
            dataDto.setData(billClaimList);
            dataDto.setAction(ICmpConstant.SAVE);
            dataDto.setBillnum(IBillNumConstant.CMP_BILLCLAIM_CARD);
            result = fiBillService.batchSave(dataDto);
            List<Object> infos = result.getInfos();
            List<Object> infosNew = new ArrayList<>();
            if (CollectionUtils.isNotEmpty(infos)) {
                for (Object info : infos) {
                    Map<String, Object> infoMap = (Map<String, Object>) info;
                    Map<String, Object> infoMapNew = new HashMap<>();
                    billClaimIdList.add(infoMap.get("id").toString());
                    infoMapNew.put("id", infoMap.get("id"));
                    infoMapNew.put("code", infoMap.get("code"));
                    infoMapNew.put("accentity", infoMap.get("accentity"));
                    infoMapNew.put("totalamount", infoMap.get("totalamount"));
                    infosNew.add(infoMapNew);
                }
                result.setInfos(infosNew);
            }
            if (saveAndCommit && CollectionUtils.isNotEmpty(billClaimIdList)) {
                List<BillClaim> billClaims = MetaDaoHelper.queryByIds(BillClaim.ENTITY_NAME, "*", billClaimIdList);
                dataDto.setData(billClaims);
                dataDto.setAction(OperationTypeEnum.SUBMIT.getValue());
                result = fiBillService.batchsubmit(dataDto);
                List<Object> submitInfos = result.getInfos();
                List<Object> submitIinfosNew = new ArrayList<>();
                if (CollectionUtils.isNotEmpty(submitInfos)) {
                    for (Object info : submitInfos) {
                        Map<String, Object> infoMap = (Map<String, Object>) info;
                        Map<String, Object> infoMapNew = new HashMap<>();
                        billClaimIdList.add(infoMap.get("id").toString());
                        infoMapNew.put("id", infoMap.get("id"));
                        infoMapNew.put("code", infoMap.get("code"));
                        infoMapNew.put("accentity", infoMap.get("accentity"));
                        infoMapNew.put("totalamount", infoMap.get("totalamount"));
                        submitIinfosNew.add(infoMapNew);
                    }
                    result.setInfos(submitIinfosNew);
                }
            }
            result.setCount(result.getCount() + count);
            result.setFailCount(result.getFailCount() + failCount);
            if (CollectionUtils.isNotEmpty(messages)) {
                result.getMessages().addAll(messages);
            }
            data.put("data", result);
        }
        rollBackData(billClaimIdList, bankReconciliationIdList, saveAndCommit);

        return data;
    }

    private static void rollBackData(List<String> billClaimIdList, List<String> bankReconciliationIdList,
                                     boolean saveAndCommit) {
        Map<String,Object> DELETE_ALL_BILL_CLAIM = new HashMap<>();
        DELETE_ALL_BILL_CLAIM.put("saveAndCommit", saveAndCommit);
        if (CollectionUtils.isNotEmpty(billClaimIdList)) {
            DELETE_ALL_BILL_CLAIM.put("billClaimIdList", billClaimIdList);
        }
        if (CollectionUtils.isNotEmpty(bankReconciliationIdList)) {
            DELETE_ALL_BILL_CLAIM.put("bankReconciliationIdList", bankReconciliationIdList);
        }
        if (ValueUtils.isNotEmptyObj(DELETE_ALL_BILL_CLAIM)){
            YtsContext.setYtsContext("DELETE_ALL_BILL_CLAIM", DELETE_ALL_BILL_CLAIM);
        }
    }

    /**
     * <h2>批量认领回滚RPC接口</h2>
     *
     * @return java.lang.Object
     * @author Sun GuoCai
     * @date 2024/4/22 16:17
     */
    @Override
    public Object cancelTotalClaimByBankSeqNo(List<TotalClaimRequestVO> totalClaimRequestVOList) {

        try {
            Map<String,Object> DELETE_ALL_BILL_CLAIM = (Map<String, Object>) YtsContext.getYtsContext("DELETE_ALL_BILL_CLAIM");
            List<String> billClaimIdList = (List<String>) DELETE_ALL_BILL_CLAIM.get("billClaimIdList");
            if (CollectionUtils.isNotEmpty(billClaimIdList)){
                List<BillClaim> billClaimList = MetaDaoHelper.queryByIds(BillClaim.ENTITY_NAME, "*", billClaimIdList);
                MetaDaoHelper.delete(BillClaim.ENTITY_NAME, billClaimList);
            }
            List<String> bankReconciliationIdList = (List<String>) DELETE_ALL_BILL_CLAIM.get("bankReconciliationIdList");
            if (CollectionUtils.isNotEmpty(bankReconciliationIdList)){
                List<BankReconciliation> bankReconciliationList = MetaDaoHelper.queryByIds(BankReconciliation.ENTITY_NAME, "*", bankReconciliationIdList);
                handleBankReconciliationInfo(bankReconciliationList);
            }
        } catch (Exception e) {
            return ResultMessage.error("fail!");
        }
        return ResultMessage.data("success");
    }

    /*
     * <h2>批量认领回滚RPC接口</h2>
     *
     * @return java.lang.Object
     * @author zhoulyu
     * @date 2024/11/04 16:17
     */
    @Override
    public Object cancelTotalClaimByBankSeqNoNew(List<TotalClaimRequestVO> totalClaimRequestVOList,
                                                 TotalClaimConfigVO totalClaimConfigVO) {

        try {
            Map<String,Object> DELETE_ALL_BILL_CLAIM = (Map<String, Object>) YtsContext.getYtsContext("DELETE_ALL_BILL_CLAIM");
            List<String> billClaimIdList = (List<String>) DELETE_ALL_BILL_CLAIM.get("billClaimIdList");
            boolean saveAndCommit = (boolean) DELETE_ALL_BILL_CLAIM.get("saveAndCommit");
            if(saveAndCommit && CollectionUtils.isNotEmpty(billClaimIdList)){
                List<BillClaim> billClaimList = MetaDaoHelper.queryByIds(BillClaim.ENTITY_NAME, "*", billClaimIdList);
                BillDataDto dataDto = new BillDataDto();
                dataDto.setData(billClaimList);
                dataDto.setAction(OperationTypeEnum.UNSUBMIT.getValue());
                dataDto.setBillnum(IBillNumConstant.CMP_BILLCLAIM_CARD);
                fiBillService.batchunaudit(dataDto);
            }
            if (CollectionUtils.isNotEmpty(billClaimIdList)){
                List<BillClaim> billClaimList = MetaDaoHelper.queryByIds(BillClaim.ENTITY_NAME, "*", billClaimIdList);
                MetaDaoHelper.delete(BillClaim.ENTITY_NAME, billClaimList);
            }
            List<String> bankReconciliationIdList = (List<String>) DELETE_ALL_BILL_CLAIM.get("bankReconciliationIdList");
            if (CollectionUtils.isNotEmpty(bankReconciliationIdList)){
                List<BankReconciliation> bankReconciliationList = MetaDaoHelper.queryByIds(BankReconciliation.ENTITY_NAME, "*", bankReconciliationIdList);
                handleBankReconciliationInfo(bankReconciliationList);
            }
        } catch (Exception e) {
            return ResultMessage.error("fail!");
        }
        return ResultMessage.data("success");
    }

    private void handleBankReconciliationInfo(List<BankReconciliation> bankReconciliationList) throws Exception {
        for (BankReconciliation bankReconciliation : bankReconciliationList) {
            //设置待认领金额
            bankReconciliation.setAmounttobeclaimed(bankReconciliation.getAmounttobeclaimed());
            // 待认领金额为0，则认完成
            bankReconciliation.setBillclaimstatus(BillClaimStatus.ToBeClaim.getValue());
            //银行对账单状态更新 认领金额要相加
            bankReconciliation.setClaimamount(BigDecimal.ZERO);
        }
        EntityTool.setUpdateStatus(bankReconciliationList);
        CommonSaveUtils.updateBankReconciliation(bankReconciliationList);
    }

}