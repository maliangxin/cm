package com.yonyoucloud.fi.cmp.billclaim.service;

import com.google.common.collect.Lists;
import com.qcloud.cos.utils.Jackson;
import com.yonyou.business_flow.dto.DomainMakeBillRuleModel;
import com.yonyou.iuap.bizdoc.service.model.SettleMethodModel;
import com.yonyou.iuap.bizdoc.service.model.SettleMethodQueryParam;
import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.iuap.org.dto.OrgUnitDTO;
import com.yonyou.iuap.org.service.itf.core.IOrgUnitQueryService;
import com.yonyou.iuap.ucf.common.i18n.InternationalUtils;
import com.yonyou.iuap.yms.lock.YmsLock;
import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVO;
import com.yonyou.ucf.basedoc.model.rpcparams.EnterpriseParams;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.service.BillBpmService;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyou.ucf.mdd.rule.api.RuleOperatorProxy;
import com.yonyou.yonbip.ctm.accelerator.threadpool.CtmThreadPoolExecutor;
import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.logger.business.CTMCMPBusinessLogService;
import com.yonyou.yonbip.ctm.remote.RemoteDubbo;
import com.yonyou.yonbip.ctm.report.util.ReportStringUtils;
import com.yonyou.ypd.bizflow.dto.ConvertParam;
import com.yonyou.ypd.bizflow.dto.ConvertResult;
import com.yonyou.ypd.bizflow.dto.ConvertedBill;
import com.yonyou.ypd.bizflow.service.BusinessConvertService;
import com.yonyoucloud.ctm.stwb.api.settlebench.SettleBenchBRPCService;
import com.yonyoucloud.ctm.stwb.openapi.DataSettledDetail;
import com.yonyoucloud.ctm.stwb.openapi.DataSettledDistribute;
import com.yonyoucloud.ctm.stwb.reconcode.pubitf.ReconciliateCodeGenerator;
import com.yonyoucloud.ctm.stwb.reqvo.SettleDeatailRelBankBillReqVO;
import com.yonyoucloud.ctm.stwb.respvo.ResultStrRespVO;
import com.yonyoucloud.ctm.stwb.stwbentity.WSettlementResult;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.cmp.autoparam.AutoConfig;
import com.yonyoucloud.fi.cmp.autoparam.service.AutoConfigService;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliationDetail;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliationbusrelation_b;
import com.yonyoucloud.fi.cmp.bankreconciliation.CommonSaveUtils;
import com.yonyoucloud.fi.cmp.billclaim.BillClaim;
import com.yonyoucloud.fi.cmp.billclaim.BillClaimBusVoucherInfo;
import com.yonyoucloud.fi.cmp.billclaim.BillClaimItem;
import com.yonyoucloud.fi.cmp.billclaim.BillClaimItemVO;
import com.yonyoucloud.fi.cmp.cmpentity.*;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.IDomainConstant;
import com.yonyoucloud.fi.cmp.constant.IMsgConstant;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import com.yonyoucloud.fi.cmp.enums.BusinessModel;
import com.yonyoucloud.fi.cmp.enums.ConfirmStatusEnum;
import com.yonyoucloud.fi.cmp.enums.FundSplitMethod;
import com.yonyoucloud.fi.cmp.enums.OrgConfirmBillEnum;
import com.yonyoucloud.fi.cmp.internaltransferprotocol.InternalTransferProtocol;
import com.yonyoucloud.fi.cmp.internaltransferprotocol.InternalTransferProtocolVO;
import com.yonyoucloud.fi.cmp.internaltransferprotocol.TransfereeInformation;
import com.yonyoucloud.fi.cmp.internaltransferprotocol.service.InternalTransferProtocolService;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.api.bankrecilication.CtmcmpReWriteBusRpcService;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.Direction;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.common.CommonRequestDataVo;
import com.yonyoucloud.fi.cmp.util.*;
import com.yonyoucloud.fi.cmp.util.basedoc.EnterpriseBankQueryService;
import com.yonyoucloud.fi.cmp.util.business.StwbBillBuilder;
import com.yonyoucloud.fi.cmp.util.process.ProcessUtil;
import com.yonyoucloud.fi.cmp.vo.BillClaimResultVO;
import com.yonyoucloud.fi.stct.api.openapi.internaltransfer.StctInernalTransferService;
import com.yonyoucloud.fi.stct.api.openapi.internaltransfer.dto.req.CreateInternalTransferReq;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.imeta.biz.base.Objectlizer;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.base.Json;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.imeta.orm.schema.SimpleCondition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @description: 认领单业务操作service类
 * @author: wanxbo@yonyou.com
 * @date: 2022/4/21 10:02
 */
@Slf4j
@Service
public class BillClaimServiceImpl implements BillClaimService {

    @Autowired
    private CtmThreadPoolExecutor ctmThreadPoolExecutor;
    @Autowired
    CtmcmpReWriteBusRpcService ctmcmpReWriteBusRpcService;
    @Autowired
    BaseRefRpcService baseRefRpcService;
    @Autowired
    private AutoConfigService autoConfigService;

    @Autowired
    private InternalTransferProtocolService internalTransferProtocolService;

    @Autowired
    SettleBenchBRPCService settleBenchBRPCService;

    @Autowired
    private BillBpmService billBpmService;

    @Autowired
    private CTMCMPBusinessLogService ctmcmpBusinessLogService;
    @Autowired
    private IOrgUnitQueryService iOrgUnitQueryService;


    /**
     * 取消认领具体实现
     *
     * @param id 认领单ID
     * @return 取消认领结果
     */
    @Override
    @Transactional
    public CtmJSONObject cancelClaim(Long id) throws Exception {
        //加锁
        String key = ICmpConstant.MY_BILL_CLAIM_LIST + id;
        YmsLock ymsLock = JedisLockUtils.lockBillWithOutTrace(key);
        if (null == ymsLock) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101175"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418065F", "该数据正在处理，请稍后重试！") /* "该数据正在处理，请稍后重试！" */);
        }

        CtmJSONObject result = new CtmJSONObject();
        result.put("dealSucceed", false);
        try {
            BillClaim billClaim = MetaDaoHelper.findById(BillClaim.ENTITY_NAME, id);
            if (null == billClaim) {
//                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101176"),MessageUtils.getMessage("P_YS_FI_FA_0000033364") /* "单据不存在 id:" */ + id);
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101177"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("P_YS_FI_FA_0000033364", "单据不存在 id:") /* "单据不存在 id:" */ + id);
            }
            if (billClaim.getAssociationstatus() == AssociationStatus.Associated.getValue()) {
//                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101178"),MessageUtils.getMessage("P_YS_FI_CM_1466225332922089511") /* "认领单已关联，不能取消认领" */);
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101179"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("P_YS_FI_CM_1466225332922089511", "认领单已关联，不能取消认领") /* "认领单已关联，不能取消认领" */);
            }
            //认领状态非'已保存、已驳回、流程终止'，按照现有取消认领逻辑进行提示，提示语中的字段调整；如业务关联状态改为“流程完结状态”，业务关联状态枚举值名字调整"
            if (!(billClaim.getRecheckstatus() == RecheckStatus.Saved.getValue() || billClaim.getRecheckstatus() == RecheckStatus.Rejected.getValue() || billClaim.getRecheckstatus() == RecheckStatus.Terminated.getValue())) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101180"), String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A05080104", "认领单单据状态【%s】，不能取消认领，请检查！"),
                        RecheckStatus.find(billClaim.getRecheckstatus()).getName()));
            }
            if (billClaim.getRefassociationstatus() != null && billClaim.getRefassociationstatus() == AssociationStatus.Associated.getValue()) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101181"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180663", "认领单已关联，不能取消认领") /* "认领单已关联，不能取消认领" */);
            }
            // 判断现金参数
            Boolean isRecheck = autoConfigService.getIsRecheck();
            if (isRecheck && null != billClaim.getRecheckstatus() && billClaim.getRecheckstatus() == RecheckStatus.Reviewed.getValue()) {
                String code = billClaim.get(ICmpConstant.CODE).toString();
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101182"), String.format(InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1991730E05900000", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A050800FF", "取消认领失败：【%s】认领单已复核，不能取消认领，请检查！") /* "取消认领失败：【%s】认领单已复核，不能取消认领，请检查！" */) /* "取消认领失败：【%s】认领单已复核，不能取消认领，请检查！" */, code));
            }

            //处理认领单明细
            List<BillClaimItem> itemList = billClaim.items();

            //查询明细所关联的所有对账单
//            Long[] bankBillIds = (Long[]) itemList.stream().map(BillClaimItem :: getBankbill).collect(Collectors.toList()).toArray();
//            List<BankReconciliation> bankBillList = MetaDaoHelper.queryByIds(BankReconciliation.ENTITY_NAME, "*", bankBillIds);
            List<BankReconciliation> bankBillList = new ArrayList<>();
            for (BillClaimItem item : itemList) {
                //获取关联银行对账单
                BankReconciliation bankReconciliation = MetaDaoHelper.findById(BankReconciliation.ENTITY_NAME, item.getBankbill());
                if (bankReconciliation == null) {
//                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101183"),MessageUtils.getMessage("P_YS_FI_CM_1466225332922089490") /* "银行对账单信息不存在 id:" */ + item.getBankbill());
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101184"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("P_YS_FI_CM_1466225332922089490", "银行对账单信息不存在 id:") /* "银行对账单信息不存在 id:" */ + item.getBankbill());
                }
                //认领金额  = 认领金额 - 明细认领金额
                BigDecimal claimAmount = bankReconciliation.getClaimamount().subtract(item.getClaimamount());
                if (claimAmount.compareTo(BigDecimal.ZERO) > 0) {
                    bankReconciliation.setClaimamount(claimAmount);
                } else {
                    bankReconciliation.setClaimamount(BigDecimal.ZERO);
                }
                //待认领金额 = 待认领金额 + 明细认领金额
                bankReconciliation.setAmounttobeclaimed(bankReconciliation.getAmounttobeclaimed().add(item.getClaimamount()));
                //待认领状态
                bankReconciliation.setBillclaimstatus(BillClaimStatus.ToBeClaim.getValue());
                //财资统一对账码 非解析过来的要清除 && CZFW-355570 非提前入账的
                // 未解析出财资统一码，生成财资统一码并进行设置
                if (!bankReconciliation.getIsparsesmartcheckno() && !bankReconciliation.getIsadvanceaccounts()) {
                    bankReconciliation.setSmartcheckno(RemoteDubbo.get(ReconciliateCodeGenerator.class, IDomainConstant.MDD_DOMAIN_STWB).generate());
                }
                bankBillList.add(bankReconciliation);
            }
            // 修改完成
            CommonSaveUtils.updateBankReconciliation(bankBillList);
            //删除银行流水对应的
            MetaDaoHelper.batchDelete(BankReconciliationDetail.ENTITY_NAME, Lists.newArrayList(new SimpleCondition(
                    "claimid", ConditionOperator.eq, billClaim.getId())));
            //认领单删除
            MetaDaoHelper.delete(BillClaim.ENTITY_NAME, billClaim);

            result.put(ICmpConstant.MSG, ResultMessage.success());
            result.put("dealSucceed", true);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101185"), e.getMessage());
        } finally {
            JedisLockUtils.unlockBillWithOutTrace(ymsLock);
        }
        return result;
    }

    /**
     * 查询关联银行对账单的认领详情
     *
     * @param bankbillid 对账单ID
     * @return 认领详情列表
     */
    @Override
    public List<BillClaimItemVO> queryBillClaimInfo(Long bankbillid) throws Exception {
        //结果集合
        List<BillClaimItemVO> itemVOList = new ArrayList<>();
        //根据银行对账单ID查询
        QuerySchema queryIsExist = QuerySchema.create().addSelect("*");
        QueryConditionGroup conditionGroup = new QueryConditionGroup();
        conditionGroup.addCondition(QueryCondition.name("bankbill").eq(bankbillid));
        queryIsExist.addCondition(conditionGroup);
        List<Map<String, Object>> claimItems = MetaDaoHelper.query(BillClaimItem.ENTITY_NAME, queryIsExist, null);

        //根据子单查询主单信息
        for (Map<String, Object> map : claimItems) {
            BillClaimItemVO itemVO = new BillClaimItemVO();
            QuerySchema querySchema = QuerySchema.create().addSelect("vouchdate,code,totalamount,claimstaff,remark,claimtype,accentity,accentity.name,project,project.name,dept,dept.name");
            //todo
            QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("id").eq(map.get("mainid").toString()));
            querySchema.addCondition(group);
            List<Map<String, Object>> billClaimMap = MetaDaoHelper.query(BillClaim.ENTITY_NAME, querySchema);

            if (billClaimMap == null || billClaimMap.size() == 0) {
//                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101186"),MessageUtils.getMessage("P_YS_FI_CM_1466225332922089493") /* "认领单信息不存在" */);
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101187"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("P_YS_FI_CM_1466225332922089493", "认领单信息不存在") /* "认领单信息不存在 " */);
            }
            Map<String, Object> billclaim = billClaimMap.get(0);
            //主表信息赋值
            itemVO.setVouchdate(billclaim.get("vouchdate").toString());
            itemVO.setCode(billclaim.get("code").toString());
            itemVO.setTotalamount(new BigDecimal(billclaim.get("totalamount").toString()));
            itemVO.setClaimstaff(billclaim.get("claimstaff").toString());
            itemVO.setRemark(billclaim.get("remark").toString());
            //转义枚举
            Short claimType = Short.parseShort(billclaim.get("claimtype").toString());
            BillClaimType billClaimType = BillClaimType.find(claimType);
            itemVO.setClaimtype(billClaimType.getName());
            if (billclaim.containsKey("accentity_name")) {
                itemVO.setAccentity_name(billclaim.get("accentity_name").toString());
            }
            if (billclaim.containsKey("project_name")) {
                itemVO.setProject_name(billclaim.get("project_name").toString());
            }
            if (billclaim.containsKey("dept_name")) {
                itemVO.setDept_name(billclaim.get("dept_name").toString());
            }

            //子单信息
            itemVO.setClaimamount(new BigDecimal(map.get("claimamount").toString()));

            itemVOList.add(itemVO);
        }

        return itemVOList;
    }

    /**
     * 校验是否是认领的同一个交易日期
     *
     * @param id 对账单id
     * @return 校验结果
     */
    @Override
    public CtmJSONObject checkIsSameTransDate(Long id) throws Exception {
        CtmJSONObject result = new CtmJSONObject();
        //根据认领单主表ID查询
        QuerySchema queryIsExist = QuerySchema.create().addSelect("*");
        QueryConditionGroup conditionGroup = new QueryConditionGroup();
        conditionGroup.addCondition(QueryCondition.name("mainid").eq(id));
        queryIsExist.addCondition(conditionGroup);
        List<BillClaimItem> claimItems = MetaDaoHelper.queryObject(BillClaimItem.ENTITY_NAME, queryIsExist, null);

        if (claimItems == null || claimItems.size() == 0) {
            result.put("checkResult", false);
            return result;
        }
        //校验是否是同一交易日期
        Date flagDate = claimItems.get(0).getTran_date();
        boolean isSame = true;
        for (BillClaimItem item : claimItems) {
            if (item.getTran_date().compareTo(flagDate) != 0) {
                isSame = false;
            }
        }
        result.put("checkResult", isSame);
        return result;
    }

    /**
     * 我的认领 复核*
     *
     * @param billClaimResultes
     * @return
     * @throws Exception
     */
    @Override
    public CtmJSONObject recheck(List<BillClaim> billClaimResultes) throws Exception {
        int size = billClaimResultes.size();
        BillClaimResultVO billClaimResultVO = new BillClaimResultVO();
        for (BizObject bizObject : billClaimResultes) {
            BillClaim billClaim = MetaDaoHelper.findById(BillClaim.ENTITY_NAME, bizObject.getId(), 1);
            String code = bizObject.get(ICmpConstant.CODE);
            if (null == billClaim) {
                billClaimResultVO.getFailed().put(bizObject.getId().toString(), bizObject.getId().toString());
                billClaimResultVO.getMessages().add(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_198FEE9A05980001", "单据【%s】已删除，请刷新后重试！") /* "单据【%s】已删除，请刷新后重试！" */, code));
                billClaimResultVO.addFailCount();
                continue;
            }
            String key = "recheck".concat(billClaim.getId().toString());
            YmsLock ymsLock = JedisLockUtils.lockBillWithOutTrace(key);
            try {
                if (ymsLock == null) {
                    billClaimResultVO.getFailed().put(bizObject.getId().toString(), bizObject.getId().toString());
                    billClaimResultVO.getMessages().add(String.format(InternationalUtils.getMessageWithDefault("UID:P_CM-BE_198FF42605980009", "单据【%s】已锁定，请稍后重试！") /* "单据【%s】已锁定，请稍后重试！" */, code));//@notranslate
                    billClaimResultVO.addFailCount();
                    continue;
                }
                if (null != billClaim.getAssociationstatus() && billClaim.getAssociationstatus() == AssociationStatus.Associated.getValue()) {
                    billClaimResultVO.getFailed().put(bizObject.getId().toString(), bizObject.getId().toString());
                    billClaimResultVO.getMessages().add(String.format(InternationalUtils.getMessageWithDefault("UID:P_CM-BE_198FF7C804400005", "复核失败：【%s】认领单已关联，不允许复核，请检查！") /* "复核失败：【%s】认领单已关联，不允许复核，请检查！" */, code));//@notranslate
                    billClaimResultVO.addFailCount();
                } else if (null != billClaim.getRecheckstatus() && billClaim.getRecheckstatus() == RecheckStatus.Reviewed.getValue()) {
                    billClaimResultVO.getFailed().put(bizObject.getId().toString(), bizObject.getId().toString());
                    billClaimResultVO.getMessages().add(String.format(InternationalUtils.getMessageWithDefault("UID:P_CM-BE_198FF9B604400008", "复核失败：【%s】认领单已复核，不允许再次复核，请检查！") /* "复核失败：【%s】认领单已复核，不允许再次复核，请检查！" */, code));//@notranslate
                    billClaimResultVO.addFailCount();
                } else if (null != billClaim.getRecheckstatus() && billClaim.getRecheckstatus() != RecheckStatus.NotReviewed.getValue()) {
                    billClaimResultVO.getFailed().put(bizObject.getId().toString(), bizObject.getId().toString());
                    billClaimResultVO.getMessages().add(String.format("复核失败：【%s】认领单不是待复核状态，不允许复核，请检查！", code));//@notranslate
                    billClaimResultVO.addFailCount();
                } else {
                    billClaim.setRecheckstatus(RecheckStatus.Reviewed.getValue());
                    billClaim.setRecheckstaff(AppContext.getCurrentUser().getName());
                    billClaim.setRecheckdate(new Date());
                    billClaim.setEntityStatus(EntityStatus.Update);
                    CommonSaveUtils.updateBillClaim(billClaim);

                }

            } catch (Exception e) {
                String message = e.getMessage();
                log.error("================recheck circulate :" + message, e);
                String resultMsg = ValueUtils.isNotEmptyObj(message) ? (message.length() > 600 ? message.substring(600) : message) : null;
                billClaimResultVO.getFailed().put(billClaim.getId().toString(), billClaim.getId().toString());
                billClaimResultVO.getMessages().add(String.format(InternationalUtils.getMessageWithDefault("UID:P_CM-BE_19929C4805900003", "单据【%s】复核失败，原因为【%s】！") /* ""单据【%s】复核失败，原因为【%s】！" */, code, resultMsg));//@notranslate
                billClaimResultVO.addFailCount();
            } finally {
                JedisLockUtils.unlockBillWithOutTrace(ymsLock);
            }
        }
        //用于记录批量复核失败条数
        int failSize = org.apache.commons.collections4.CollectionUtils.isEmpty(billClaimResultVO.getMessages()) ? 0 : billClaimResultVO.getMessages().size();
        String message = null;
        if (size == 1) {
            if (failSize == 0) {
                message = InternationalUtils.getMessageWithDefault("UID:P_CM-BE_198FFED004400000", "我的认领复核成功！") /* "我的认领复核成功！" */;//@notranslate
            } else {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101188"), billClaimResultVO.getMessages().get(0));
            }
        } else {
            message = String.format(InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1990000604400009", "共：%s张单据；%s张复核成功；%s张复核失败！") /* "共：%s张单据；%s张复核成功；%s张复核失败！" */, size, size - failSize, failSize);//@notranslate
        }
        billClaimResultVO.setMessage(message);
        billClaimResultVO.setCount(size);
        billClaimResultVO.setSucessCount(size - billClaimResultVO.getFailCount());

        return billClaimResultVO.getResult();
    }


    /**
     * 我的认领 取消复核*
     *
     * @param billClaimResultes
     * @return
     * @throws Exception
     */
    @Override
    public CtmJSONObject unRecheck(List<BillClaim> billClaimResultes) throws Exception {
        int size = billClaimResultes.size();
        BillClaimResultVO billClaimResultVO = new BillClaimResultVO();
        for (BizObject bizObject : billClaimResultes) {
            BillClaim billClaim = MetaDaoHelper.findById(BillClaim.ENTITY_NAME, bizObject.getId(), 1);
            String code = bizObject.get(ICmpConstant.CODE);
            if (null == billClaim) {
                billClaimResultVO.getFailed().put(bizObject.getId().toString(), bizObject.getId().toString());
                billClaimResultVO.getMessages().add(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_198FEE9A05980001", "单据【%s】已删除，请刷新后重试！") /* "单据【%s】已删除，请刷新后重试！" */, code));
                billClaimResultVO.addFailCount();
                continue;
            }
            String key = "unrecheck".concat(billClaim.getId().toString());
            YmsLock ymsLock = JedisLockUtils.lockBillWithOutTrace(key);
            try {
                if (ymsLock == null) {
                    billClaimResultVO.getFailed().put(bizObject.getId().toString(), bizObject.getId().toString());
                    billClaimResultVO.getMessages().add(String.format(InternationalUtils.getMessageWithDefault("UID:P_CM-BE_198FF42605980009", "单据【%s】已锁定，请稍后重试！") /* "单据【%s】已锁定，请稍后重试！" */, code));//@notranslate
                    billClaimResultVO.addFailCount();
                    continue;
                }
                if ((null != billClaim.getAssociationstatus() && billClaim.getAssociationstatus() == AssociationStatus.Associated.getValue()) ||
                        (null != billClaim.getRefassociationstatus() && billClaim.getRefassociationstatus() == AssociationStatus.Associated.getValue())) {
                    billClaimResultVO.getFailed().put(bizObject.getId().toString(), bizObject.getId().toString());
                    billClaimResultVO.getMessages().add(String.format(InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1990044604500006", "取消复核失败：【%s】认领单已关联，不允许取消复核，请检查！") /* "取消复核失败：【%s】认领单已关联，不允许取消复核，请检查！" */, code));//@notranslate
                    billClaimResultVO.addFailCount();
                } else if (null != billClaim.getRecheckstatus() && billClaim.getRecheckstatus() == RecheckStatus.NotReviewed.getValue()) {
                    billClaimResultVO.getFailed().put(bizObject.getId().toString(), bizObject.getId().toString());
                    billClaimResultVO.getMessages().add(String.format(InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1990060A04500001", "取消复核失败：【%s】认领单未复核，不允许取消复核，请检查！") /* "取消复核失败：【%s】认领单未复核，不允许取消复核，请检查！" */, code));//@notranslate
                    billClaimResultVO.addFailCount();
                } else if (null != billClaim.getRecheckstatus() && billClaim.getRecheckstatus() != RecheckStatus.Reviewed.getValue()) {
                    billClaimResultVO.getFailed().put(bizObject.getId().toString(), bizObject.getId().toString());
                    billClaimResultVO.getMessages().add(String.format("取消复核失败：【%s】认领单不是认领成功，不允许取消复核，请检查！", code));//@notranslate
                    billClaimResultVO.addFailCount();
                } else {
                    billClaim.setRecheckstatus(RecheckStatus.NotReviewed.getValue());
                    billClaim.setRecheckstaff(null);
                    billClaim.setRecheckdate(null);
                    billClaim.setEntityStatus(EntityStatus.Update);
                    CommonSaveUtils.updateBillClaim(billClaim);

                }

            } catch (Exception e) {
                String message = e.getMessage();
                log.error("================recheck circulate :" + message, e);
                String resultMsg = ValueUtils.isNotEmptyObj(message) ? (message.length() > 600 ? message.substring(600) : message) : null;
                billClaimResultVO.getFailed().put(billClaim.getId().toString(), billClaim.getId().toString());
                billClaimResultVO.getMessages().add(String.format(InternationalUtils.getMessageWithDefault("UID:P_CM-BE_19929C1004500005", "单据【%s】取消复核失败，原因为【%s】！") /* "单据【%s】取消复核失败，原因为【%s】！" */, code, resultMsg));//@notranslate
                billClaimResultVO.addFailCount();
            } finally {
                JedisLockUtils.unlockBillWithOutTrace(ymsLock);
            }
        }
        //用于记录批量取消复核失败条数
        int failSize = org.apache.commons.collections4.CollectionUtils.isEmpty(billClaimResultVO.getMessages()) ? 0 : billClaimResultVO.getMessages().size();
        String message = null;
        if (size == 1) {
            if (failSize == 0) {
                message = InternationalUtils.getMessageWithDefault("UID:P_CM-BE_199006B604500004", "我的认领取消复核成功！") /* "我的认领取消复核成功！" */;//@notranslate
            } else {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101189"), billClaimResultVO.getMessages().get(0));
            }
        } else {
            message = String.format(InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1990071805900000", "共：%s张单据；%s张取消复核成功；%s张取消复核失败！") /* "共：%s张单据；%s张取消复核成功；%s张取消复核失败！" */, size, size - failSize, failSize);//@notranslate
        }
        billClaimResultVO.setMessage(message);
        billClaimResultVO.setCount(size);
        billClaimResultVO.setSucessCount(size - billClaimResultVO.getFailCount());

        return billClaimResultVO.getResult();
    }

    /**
     * 切块逻辑以及生成内转单
     * 1认领单的银行账户是内部账户 不切分 不生成结算中心内转单
     * 2内转单生成逻辑为：
     * //资金切块 是否启用内转协议进行资金切分 资金切分方式=‘内部账户划转’
     * //资金切分方式为空需要判断 认领账户为内部账户生成结算中心内转单
     *
     * @param dataSettledDetail
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = RuntimeException.class)
    public void billclaimFundSegmentation(DataSettledDetail dataSettledDetail) {
        log.error("切块方法进入1");
        try {
            if (String.valueOf(WSettlementResult.AllSuccess.getValue()).equals(dataSettledDetail.getWsettlementResult())) {// 全部成功
                log.error("切块方法进入2");
                List<DataSettledDistribute> dataSettledDistributeList = dataSettledDetail.getDataSettledDistribute();
                if (CollectionUtils.isEmpty(dataSettledDistributeList)) {
                    log.error("切块方法进入3结束");
                    return;
                }
                if (Objects.isNull(dataSettledDetail.getRelateClaimBillId())) {
                    return;
                }
                doFundSegmentation(dataSettledDetail.getRelateClaimBillId().toString());
            }
        } catch (Exception e) {
            log.error("error日志：切块失败！！" + e.getMessage());
        }

    }

    /**
     * 简强切块逻辑以及生成内转单
     * 1认领单的银行账户是内部账户 不切分 不生成结算中心内转单
     * 2内转单生成逻辑为：
     * //资金切块 是否启用内转协议进行资金切分 资金切分方式=‘内部账户划转’
     * //资金切分方式为空需要判断 认领账户为内部账户生成结算中心内转单
     *
     * @param claimId
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = RuntimeException.class)
    public void billclaimFundSegmentationSimple(String claimId) {
        log.error("切块方法进入1");
        try {
            doFundSegmentation(claimId);
        } catch (Exception e) {
            log.error("error日志：切块失败！！" + e.getMessage());
        }
    }

    private boolean doFundSegmentation(String claimId) throws Exception {
        List<String> billClaimIds = new ArrayList<>();
        //0926通过主表认领的来取ID
        if (claimId != null) {
            log.error("切块方法进入4");
            billClaimIds.add(claimId);
            if (billClaimIds.size() > 0) {
                log.error("切块方法进入5");
                QuerySchema qs = QuerySchema.create().addSelect("*");
                QueryConditionGroup conditionGroup = new QueryConditionGroup();
                conditionGroup.addCondition(QueryConditionGroup.and(
                        QueryConditionGroup.or(QueryCondition.name("id").in(billClaimIds),
                                QueryCondition.name("refbill").in(billClaimIds))));
                qs.addCondition(conditionGroup);
                List<BillClaim> billClaimList = MetaDaoHelper.queryObject(BillClaim.ENTITY_NAME, qs, null);
                if (billClaimList != null && billClaimList.size() > 0) {
                    log.error("切块方法进入6");
                    for (BillClaim billClaim : billClaimList) {
                        //认领单的银行账户是内部账户 不切分
                        if (billClaim.getBankaccount() != null) {
                            EnterpriseParams enterpriseParams = new EnterpriseParams();
                            enterpriseParams.setId(billClaim.getBankaccount());
                            List<EnterpriseBankAcctVO> bankAccounts = baseRefRpcService.queryEnterpriseBankAccountByCondition(enterpriseParams);
                            if (!bankAccounts.isEmpty()) {
                                if (ValueUtils.isNotEmptyObj(bankAccounts.get(0).getAcctopentype()) && bankAccounts.get(0).getAcctopentype().equals(1)) {
                                    return true;
                                }
                            }
                        }
                        QuerySchema queryItemSchema = QuerySchema.create().addSelect("id,bankbill");
                        queryItemSchema.addCondition(QueryConditionGroup.and(QueryCondition.name("mainid").eq(billClaim.getString("id"))));
                        List<BillClaimItem> billClaimItems = MetaDaoHelper.queryObject(BillClaimItem.ENTITY_NAME, queryItemSchema, null);
                        List bankBillIds = billClaimItems.stream().map(BillClaimItem::getBankbill).collect(Collectors.toList());
                        QuerySchema queryBankSchema = QuerySchema.create().addSelect("id,isimputation");
                        queryBankSchema.addCondition(QueryConditionGroup.and(QueryCondition.name("id").in(bankBillIds)));
                        List<BankReconciliation> bankReconciliations = MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME, queryBankSchema, null);
                        List<BankReconciliation> noimputations = bankReconciliations.stream().filter(bankReconciliation -> !bankReconciliation.getIsimputation()).collect(Collectors.toList());
                        //认领单关联的银行对账单不包含 不触发归集 的数据
                        //资金切块 是否启用内转协议进行资金切分 资金切分方式=‘内部账户划转’
                        //资金切分方式为空需要判断 认领账户为内部账户生成结算中心内转单
                        String claimaccount = billClaim.getClaimaccount();//认领账户
                        if ((noimputations == null || noimputations.isEmpty())
                                && ((billClaim.getFundsplitmethod() == null && isInnerBankAccount(claimaccount)) //资金切分方式为空且认领账户为内部账户
                                || (billClaim.getFundsplitmethod() != null && FundSplitMethod.InnerAccount_Trans.getCode() == billClaim.getFundsplitmethod()))) {
                            //普通结算 内部账户记账为否
                            log.error("切块方法进入8");
                            //普通结算下内部账户是否记账只有为否生成结算中心特转付
                            if (billClaim.getBusinessmodel() != null && BusinessModel.General_Settlement.getCode() == billClaim.getBusinessmodel()
                                    && (billClaim.getIsinneraccounting() == null || !billClaim.getIsinneraccounting())) {
                                log.error("切块方法进入9");
//                                    internalAccountWriting(billClaim);
                                if (billClaim.getDirection() != null && Direction.Credit.getValue() == billClaim.getDirection()) {
                                    log.error("切块方法进入10生成特转付");
                                    billClaimGenerateCreateInternalTransfer(billClaim, 2);
                                } else if (billClaim.getDirection() != null && Direction.Debit.getValue() == billClaim.getDirection()) {
                                    log.error("切块方法进入10生成特转收");
                                    billClaimGenerateCreateInternalTransfer(billClaim, 1);
                                }
                            } else if (billClaim.getBusinessmodel() != null && BusinessModel.Unify_InOut.getCode() == billClaim.getBusinessmodel()) {
                                log.error("切块方法进入11");
                                //统收统支 内部账户是否记账为是生成正常内转单
                                if (billClaim.getIsinneraccounting() != null && billClaim.getIsinneraccounting()) {
                                    log.error("切块方法进入12--正常生内转单");
                                    billClaimGenerateCreateInternalTransfer(billClaim, 3);
                                    //统收统支 内部账户是否记账为否生成特转付  内部账户是否记账为空的话不处理
                                } else if (billClaim.getIsinneraccounting() != null && !billClaim.getIsinneraccounting()) {
                                    log.error("切块方法进入13");
//                                        internalAccountWriting(billClaim);
                                    if (billClaim.getDirection() != null && Direction.Credit.getValue() == billClaim.getDirection()) {
                                        log.error("切块方法进入14--特转付");
                                        billClaimGenerateCreateInternalTransfer(billClaim, 2);
                                    } else if (billClaim.getDirection() != null && Direction.Debit.getValue() == billClaim.getDirection()) {
                                        log.error("切块方法进入14--特转收");
                                        billClaimGenerateCreateInternalTransfer(billClaim, 1);
                                    }
                                }
                            }
                        }
                        log.error("billClaim according to transferProtocol generator bill, accent={}, isFundSplit={}, claimAccount={}",
                                billClaim.getAccentity(), billClaim.getIsfundsplit(), billClaim.getClaimaccount());
                        //实际切块代码逻辑
                        if (getInnerTransSplitFlag(billClaim.getAccentity()) && billClaim.getIsfundsplit() != null && billClaim.getIsfundsplit()
                                && billClaim.getClaimaccount() != null && billClaim.getDirection() == Direction.Credit.getValue()) {
                            log.error("切块方法进入15");
                            billClaimGenerateInternalTransferProtocol(billClaim);
                            log.error("切块方法j结束15");
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * 判断银行账户是否是内部账户
     *
     * @return
     */
    public boolean isInnerBankAccount(String id) throws Exception {
        boolean isInnerAccount = false;
        EnterpriseParams enterpriseParams = new EnterpriseParams();
        enterpriseParams.setId(id);
        List<EnterpriseBankAcctVO> bankAccounts = baseRefRpcService.queryEnterpriseBankAccountByCondition(enterpriseParams);
        if (!bankAccounts.isEmpty()) {
            if (ValueUtils.isNotEmptyObj(bankAccounts.get(0).getAcctopentype()) && bankAccounts.get(0).getAcctopentype().equals(1)) {
                isInnerAccount = true;
            }
        }
        return isInnerAccount;
    }


    //现金管理  认领单推内转协议接
    private void billClaimGenerateInternalTransferProtocol(BillClaim billClaim) {
        try {
            InternalTransferProtocolVO internalTransferProtocolVO = new InternalTransferProtocolVO();
            if (billClaim.getActualclaimaccentiry() != null) {
                internalTransferProtocolVO.setAccentity(billClaim.getActualclaimaccentiry());
            } else {
                internalTransferProtocolVO.setAccentity(billClaim.getAccentity());
            }
            internalTransferProtocolVO.setContractNo(billClaim.getIntransagreementnumber());
            internalTransferProtocolVO.setProject(billClaim.getProject());
            internalTransferProtocolVO.setSplitAmount(billClaim.getTotalamount());
            internalTransferProtocolVO.setCurrency(billClaim.getCurrency());
            internalTransferProtocolVO.setEnterpriseBankAccount(billClaim.getClaimaccount());
            internalTransferProtocolVO.setSrcBillCode(billClaim.getCode());
            internalTransferProtocolVO.setSrcBillId(billClaim.getId().toString());
            internalTransferProtocolService.internalTransferBillGeneratesFundPaymentBill(internalTransferProtocolVO);
        } catch (Exception e) {
            log.error("结算成功进行资金切块,认领单推内转协议接失败:认领单编码 = {}, e = {}", billClaim.getCode(), e.getMessage());
        }
    }

    //认领单生成内部转账单

    /**
     * @param billClaim
     * @param tranType  1=特转收 2=特转付 3=正常生内转单
     */
    private void billClaimGenerateCreateInternalTransfer(BillClaim billClaim, int tranType) {
        try {
            CreateInternalTransferReq createInternalTransferReq = new CreateInternalTransferReq();
//            if (billClaim.getActualclaimaccentiry() != null) {
//                createInternalTransferReq.setSettlementCenter(billClaim.getActualclaimaccentiry());
//            } else {
//                createInternalTransferReq.setSettlementCenter(billClaim.getAccentity());
//            }
            //通过内部账户查询结算中心
            log.error("结算成功进行资金切块,认领单生成内部转账单billClaim:" + CtmJSONObject.toJSONString(billClaim));
            Map<String, Object> claimAccount = QueryBaseDocUtils.queryEnterpriseBankAccountById(billClaim.getClaimaccount());
            Map<String, Object> impInnerAccount = QueryBaseDocUtils.queryEnterpriseBankAccountById(billClaim.getImpinneraccount());
            log.error("结算成功进行资金切块,认领单生成内部转账单claimAccount:" + CtmJSONObject.toJSONString(claimAccount));
            log.error("结算成功进行资金切块,认领单生成内部转账单impInnerAccount:" + CtmJSONObject.toJSONString(impInnerAccount));
            if (claimAccount != null) {
                log.error("claimAccount.get(\"settleorgid\"):" + claimAccount.get("settleorgid"));
                createInternalTransferReq.setSettlementCenter(claimAccount.get("settleorgid") == null ? "" : claimAccount.get("settleorgid").toString());
            } else {
                log.error("结算成功进行资金切块,认领单生成内部转账单失败:认领单编码 = {}, e = {}", billClaim.getCode(), "根据认领账户未查到结算中心");
            }
            createInternalTransferReq.setSettlementStatus("0");
            createInternalTransferReq.setOriginalCurrency(Long.valueOf(billClaim.getCurrency()));
            createInternalTransferReq.setOriginalCurrencyAmt(billClaim.getTotalamount());
            createInternalTransferReq.setDataSource("CM");
            createInternalTransferReq.setSourceId(billClaim.getId());
            createInternalTransferReq.setBillType("cmp_billclaim");
            createInternalTransferReq.setBillNo(billClaim.getCode());
            createInternalTransferReq.setMainBillNo(billClaim.getCode());
            createInternalTransferReq.setNotes(billClaim.getRemark());
            createInternalTransferReq.setProject(billClaim.getProject());
            //入账类型
            createInternalTransferReq.setEntrytype(billClaim.getEntrytype());
            //结算中心受理类型 --自动受理
            createInternalTransferReq.setSettlementCenterAcceptType((short) 2);
            if (tranType == 1) {
                createInternalTransferReq.setReceiverinterAcctCode("0");
                if (impInnerAccount != null) {
                    createInternalTransferReq.setPayerinterAcctCode(impInnerAccount.get("account") == null ? "" : impInnerAccount.get("account").toString());
                }
            } else if (tranType == 2) {
                createInternalTransferReq.setReceiverinterAcctCode(claimAccount.get("account") == null ? "" : claimAccount.get("account").toString());
                createInternalTransferReq.setPayerinterAcctCode("0");
            } else if (tranType == 3) {
                createInternalTransferReq.setReceiverinterAcctCode(claimAccount.get("account") == null ? "" : claimAccount.get("account").toString());
                if (impInnerAccount != null) {
                    createInternalTransferReq.setPayerinterAcctCode(impInnerAccount.get("account") == null ? "" : impInnerAccount.get("account").toString());
                }
            }
            createInternalTransferReq.setOccupiedFlag("1");
            // 适配结算中心 批量生成内转单接口
            List<CreateInternalTransferReq> createInternalTransferReqs = new ArrayList<>();
            createInternalTransferReqs.add(createInternalTransferReq);
            CtmJSONObject sourceData = new CtmJSONObject();
            sourceData.put("sourceData", billClaim);
            RemoteDubbo.get(StctInernalTransferService.class, IDomainConstant.MDD_DOMAIN_STCT).createStctInternalTransfersUseSource(createInternalTransferReqs, sourceData);
//            RemoteDubbo.get(StctInernalTransferService.class, IDomainConstant.MDD_DOMAIN_STCT).createStctInternalTransfer(createInternalTransferReq);
        } catch (Exception e) {
            log.error("结算成功进行资金切块,认领单生成内部转账单失败e.getMessage():认领单编码 = {}, e = {}", billClaim.getCode(), e.getMessage());
            log.error("结算成功进行资金切块,认领单生成内部转账单失败e:", e);
        }
    }

    //是否启用内转协议进行资金切分 参数是否启用
    public Boolean getInnerTransSplitFlag(String accentity) throws Exception {
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

    @Override
    public CtmJSONObject billClaimProjectEditAfter(CtmJSONObject param) throws Exception {
        CtmJSONObject data = new CtmJSONObject();
        QuerySchema qs = QuerySchema.create().addSelect("*");
        QueryConditionGroup conditionGroup = new QueryConditionGroup();
        if (ValueUtils.isNotEmptyObj(param.get("actualclaimaccentiry"))) {
            conditionGroup.addCondition(QueryCondition.name("accentity").eq(param.get("actualclaimaccentiry")));
        } else {
            conditionGroup.addCondition(QueryCondition.name("accentity").eq(param.get(ICmpConstant.ACCENTITY)));
        }
        conditionGroup.addCondition(QueryCondition.name("project").eq(param.get(ICmpConstant.PROJECT)));
        conditionGroup.addCondition(QueryCondition.name("currency").eq(param.get(ICmpConstant.CURRENCY)));
        conditionGroup.addCondition(QueryCondition.name("isDiscard").eq((short) 0));
        conditionGroup.addCondition(QueryCondition.name("isEnabledType").eq(IsEnable.ENABLE.getValue()));
        // 当前只查询为父级的内转协议
        conditionGroup.addCondition(QueryCondition.name("isParent").eq(1));
        qs.addCondition(conditionGroup);
        List<InternalTransferProtocol> internalTransferProtocolList = MetaDaoHelper.queryObject(InternalTransferProtocol.ENTITY_NAME, qs, null);
        if (internalTransferProtocolList != null && internalTransferProtocolList.size() > 0) {
            InternalTransferProtocol internalTransferProtocol = internalTransferProtocolList.get(0);
            data.put("intransagreementnumber", internalTransferProtocol.getCode());
            data.put("intransagreementversion", internalTransferProtocol.getVersionNo());
            QuerySchema querySchema = QuerySchema.create().addSelect("*");
            QueryConditionGroup queryConditionGroup = new QueryConditionGroup();
            queryConditionGroup.addCondition(QueryCondition.name("mainid").eq(internalTransferProtocol.getId()));
            querySchema.addCondition(queryConditionGroup);
            List<TransfereeInformation> transfereeInformations = MetaDaoHelper.queryObject(TransfereeInformation.ENTITY_NAME, querySchema, null);
//       TODO枚举报错     if (ValueUtils.isNotEmptyObj(transfereeInformations.get(0).getAcctOpenType()) && AcctOpenType.BANK_ACCOUNT_OPENING.getValue().shortValue() == transfereeInformations.get(0).getAcctOpenType()) {

            if (ValueUtils.isNotEmptyObj(transfereeInformations.get(0).getAcctOpenType()) && 0 == transfereeInformations.get(0).getAcctOpenType()) {
                data.put("fundsplitmethod", FundSplitMethod.BankAccount_Trans.getCode());
            }
//            if (ValueUtils.isNotEmptyObj(transfereeInformations.get(0).getAcctOpenType()) && AcctOpenType.SETTLEMENT_CENTER_ACCOUNT_OPENING.getValue().shortValue() == transfereeInformations.get(0).getAcctOpenType()) {

            if (ValueUtils.isNotEmptyObj(transfereeInformations.get(0).getAcctOpenType()) && 1 == transfereeInformations.get(0).getAcctOpenType()) {
                data.put("fundsplitmethod", FundSplitMethod.InnerAccount_Trans.getCode());
            }
            data.put("isfundsplit", true);
        } else {
            data.put("intransagreementnumber", "");
            data.put("intransagreementversion", "");
            data.put("fundsplitmethod", "");
            data.put("isfundsplit", false);
        }
        return data;
    }

    @Override
    public CtmJSONObject confirm(List<BankReconciliation> billList, String accentity) throws Exception {

        CtmJSONObject result = new CtmJSONObject();
        List<String> messages = new ArrayList<>();
        if (billList == null || billList.size() == 0) {
            throw new Exception(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00180", "请选择单据！") /* "请选择单据！" */);
        }
        int i = 0;
        CtmJSONObject failed = new CtmJSONObject();
        List<BankReconciliation> successList = new ArrayList<>();
        for (BankReconciliation bankReconciliation : billList) {
            if (bankReconciliation.getAccentity() != null) {
                failed.put(bankReconciliation.getId().toString(), bankReconciliation.getId().toString());
                if (bankReconciliation.getBank_seq_no() != null) {
                    messages.add("银行流水号:" + bankReconciliation.getBank_seq_no() + "账户使用组织不为空，不允许进行该操作，请确认！");//@notranslate
                } else {
                    messages.add("银行流水号:[]账户使用组织不为空，不允许进行该操作，请确认！");//@notranslate
                }
                i++;
                continue;
            }
            // 清空授权使用组织
            bankReconciliation.setAccentity(accentity);
            if (AccentityUtil.getFinOrgDTOByAccentityId(accentity) != null) {
                bankReconciliation.setAccentityRaw(AccentityUtil.getFinOrgDTOByAccentityId(accentity).getId());
            }
            // 确认状态 = 已确认
            bankReconciliation.setConfirmstatus(ConfirmStatusEnum.Confirmed.getIndex());
            // 授权组织确认单据-到账认领中心
            bankReconciliation.setConfirmbill(OrgConfirmBillEnum.CMP_BILLCLAIMCENTER.getIndex());
            bankReconciliation.setBank_seq_no(bankReconciliation.getBank_seq_no());
            bankReconciliation.setEntityStatus(EntityStatus.Update);
            successList.add(bankReconciliation);
        }
        // 修改完成
        CommonSaveUtils.updateBankReconciliation(successList);
        String message = String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00183", "共：[%s]张单据；[%s]张确认成功；[%s]张确认失败！") /* "共：[%s]张单据；[%s]张确认成功；[%s]张确认失败！" */, billList.size(), (billList.size() - i), i);
        result.put("msg", message);
        result.put("msgs", messages);
        result.put("messages", messages);
        result.put("count", billList.size());
        result.put("sucessCount", successList.size());
        result.put("failCount", i);
        if (failed.size() > 0) {
            result.put("failed", failed);
        }
        return result;
    }

    @Override
    public CtmJSONObject confirmFromBank(List<BankReconciliation> billList, String accentity) throws Exception {
        CtmJSONObject result = new CtmJSONObject();
        List<String> messages = new ArrayList<>();
        if (billList == null || billList.size() == 0) {
            throw new Exception(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00180", "请选择单据！") /* "请选择单据！" */);
        }
        int i = 0;
        CtmJSONObject failed = new CtmJSONObject();
        List<BankReconciliation> successList = new ArrayList<>();
        for (BankReconciliation bankReconciliation : billList) {
            //校验所选数据是否发布=否、日记账是否已勾对=否、总账是否已勾对=否、业务关联状态=未关联、退票状态=空
            // CZFW-522262 去掉退票状态的限定
            /*if (bankReconciliation.getIspublish() || (bankReconciliation.getAssociationstatus() != null && bankReconciliation.getAssociationstatus() != 0)
                    || bankReconciliation.getRefundstatus() != null) {*/
            if (bankReconciliation.getIspublish() || (bankReconciliation.getAssociationstatus() != null && bankReconciliation.getAssociationstatus() != 0)
            ) {
                failed.put(bankReconciliation.getId().toString(), bankReconciliation.getId().toString());
                /*
                if (bankReconciliation.getBank_seq_no() != null) {
                    messages.add("银行流水号:" + bankReconciliation.getBank_seq_no() + "包含退票/疑似退票、已发布、已关联单据或已经参与对账的数据，请检查！");
                     }
                  else {
                    messages.add("银行流水号:[]包含退票/疑似退票、已发布、已关联单据或已经参与对账的数据，请检查！");
                    messages.add("银行流水号:包含已发布、已关联单据或已经参与对账的数据，请检查！");
                 }
                 */
                messages.add(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_229A0A6605600005", "银行流水号:[%s]包含已发布、已关联单据或已经参与对账的数据，请检查！")/*银行流水号:[%s]包含已发布、已关联单据或已经参与对账的数据，请检查！*/, bankReconciliation.getBank_seq_no() == null ? "" : bankReconciliation.getBank_seq_no()));

                i++;
                continue;
            }
            // 清空授权使用组织
            bankReconciliation.setAccentity(accentity);
            if (AccentityUtil.getFinOrgDTOByAccentityId(accentity) != null) {
                bankReconciliation.setAccentityRaw(AccentityUtil.getFinOrgDTOByAccentityId(accentity).getId());
            }
            // 确认状态 = 已确认
            bankReconciliation.setConfirmstatus(ConfirmStatusEnum.Confirmed.getIndex());
            // 授权组织确认单据-银行流水认领
            bankReconciliation.setConfirmbill(OrgConfirmBillEnum.CMP_BANKRECONCILIATION.getIndex());
            bankReconciliation.setEntityStatus(EntityStatus.Update);
            bankReconciliation.setBank_seq_no(bankReconciliation.getBank_seq_no());
            successList.add(bankReconciliation);
        }
        // 修改完成
        CommonSaveUtils.updateBankReconciliation(successList);
        String message = String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00183", "共：[%s]张单据；[%s]张确认成功；[%s]张确认失败！") /* "共：[%s]张单据；[%s]张确认成功；[%s]张确认失败！" */, billList.size(), (billList.size() - i), i);
        result.put("msg", message);
        result.put("msgs", messages);
        result.put("messages", messages);
        result.put("count", billList.size());
        result.put("sucessCount", successList.size());
        result.put("failCount", i);
        if (failed.size() > 0) {
            result.put("failed", failed);
        }
        return result;
    }

    @Override
    public CtmJSONObject asyncConfirmFromBank(String uid, CtmJSONArray row, String accentity) throws Exception {
        //1. 构建进度条信息
        //构建进度条信息
        CtmJSONObject responseMsg = new CtmJSONObject();
        ProcessUtil.initProcess(uid, row.size());
        //2. 异步处理
        //1. 先加锁，再校验
        ctmThreadPoolExecutor.getThreadPoolExecutor().submit(() -> {
            asyncConfirm(uid, row, accentity);
        });
        responseMsg.put("message", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540048E", "确认使用组织") /* "确认使用组织" */);
        return responseMsg;
    }


    public void asyncConfirm(String uid, CtmJSONArray row, String accentity) {
        CtmJSONObject lockResult = new CtmJSONObject();
        try {
            Map<String, Integer> hang = new HashMap<>();
            for (int i = 0; i < row.size(); i++) {
                Map rowObj = (Map) row.get(i);
                hang.put((String) rowObj.get("id"), i + 1);
            }
            lockResult = CacheUtils.lockBill(BankReconciliation.ENTITY_NAME, row);
            CtmJSONArray hasLockRowData = lockResult.getJSONArray("hasLockRowData");
            CtmJSONArray lockRowData = lockResult.getJSONArray("lockRowData");
            for (Object hasLockRowDatum : hasLockRowData) {
                CtmJSONObject hasLockCtm = (CtmJSONObject) hasLockRowDatum;
                Integer index = hang.get(hasLockCtm.getString("id"));
                String bankaccount_name = hasLockCtm.getString("bankaccount_name");
                ProcessUtil.addMessage(uid, String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1E0CF14A05C00006", "第%s条、银行账号：%s 已被锁定，请刷新后重试！"), index, bankaccount_name));
            }
            Json json = new Json(lockRowData.toString());
            List<BankReconciliation> billList = Objectlizer.decode(json, BankReconciliation.ENTITY_NAME);
            List<BankReconciliation> successList = new ArrayList<>();
            for (BankReconciliation bankReconciliation : billList) {
                //校验所选数据是否发布=否、日记账是否已勾对=否、总账是否已勾对=否、业务关联状态=未关联、退票状态=空
                // CZFW-522262 去掉退票状态的限定
                if (bankReconciliation.getIspublish() || (bankReconciliation.getAssociationstatus() != null && bankReconciliation.getAssociationstatus() != 0)
                        ) {
                    //ProcessUtil.addMessage(uid, String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1E7E988E05200003", "第%s条、银行账号：%s 包含退票/疑似退票、已发布、已关联单据或已经参与对账的数据，请检查！"), hang.get(bankReconciliation.getId().toString()), bankReconciliation.getString("bankaccount_name")));
                    ProcessUtil.addMessage(uid,String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_229A0A6605600005" , "银行流水号:[%s]包含已发布、已关联单据或已经参与对账的数据，请检查！")/*银行流水号:[%s]包含已发布、已关联单据或已经参与对账的数据，请检查！*/,bankReconciliation.getBank_seq_no() ));
                    continue;
                }
                // 清空授权使用组织
                bankReconciliation.setAccentity(accentity);
                if (AccentityUtil.getFinOrgDTOByAccentityId(accentity) != null) {
                    bankReconciliation.setAccentityRaw(AccentityUtil.getFinOrgDTOByAccentityId(accentity).getId());
                }
                // 确认状态 = 已确认
                bankReconciliation.setConfirmstatus(ConfirmStatusEnum.Confirmed.getIndex());
                // 授权组织确认单据-银行流水认领
                bankReconciliation.setConfirmbill(OrgConfirmBillEnum.CMP_BANKRECONCILIATION.getIndex());
                bankReconciliation.setEntityStatus(EntityStatus.Update);
                successList.add(bankReconciliation);
                ProcessUtil.refreshProcess(uid, true, 1);
            }
            // 修改完成
            CommonSaveUtils.updateBankReconciliation(successList);
            //ProcessUtil.completedResetCount(uid);
        } catch (Exception e) {
            log.error("asyncConfirmFromBank", e);
            ProcessUtil.addMessage(uid, e.getMessage());
        } finally {
            CacheUtils.unlockBill(BankReconciliation.ENTITY_NAME, lockResult.getJSONArray("lockRowData"));
            ProcessUtil.completedResetCount(uid);
        }
    }


    @Override
    public CtmJSONObject asyncCancelConfirmFormBank(String uid, CtmJSONArray row) throws Exception {
        //1. 构建进度条信息
        //构建进度条信息
        CtmJSONObject responseMsg = new CtmJSONObject();
        ProcessUtil.initProcess(uid, row.size());
        //2. 异步处理
        //1. 先加锁，再校验
        ctmThreadPoolExecutor.getThreadPoolExecutor().submit(() -> {
            asyncCancelConfirm(uid, row);
        });
        responseMsg.put("message", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400490", "清空使用组织") /* "清空使用组织" */);
        return responseMsg;
    }


    public void asyncCancelConfirm(String uid, CtmJSONArray row) {
        CtmJSONObject lockResult = new CtmJSONObject();
        try {
            Map<String, Integer> hang = new HashMap<>();
            for (int i = 0; i < row.size(); i++) {
                Map rowObj = (Map) row.get(i);
                hang.put((String) rowObj.get("id"), i + 1);
            }
            lockResult = CacheUtils.lockBill(BankReconciliation.ENTITY_NAME, row);
            CtmJSONArray hasLockRowData = lockResult.getJSONArray("hasLockRowData");
            CtmJSONArray lockRowData = lockResult.getJSONArray("lockRowData");
            for (Object hasLockRowDatum : hasLockRowData) {
                CtmJSONObject hasLockCtm = (CtmJSONObject) hasLockRowDatum;
                Integer index = hang.get(hasLockCtm.getString("id"));
                String bankaccount_name = hasLockCtm.getString("bankaccount_name");
                ProcessUtil.addMessage(uid, String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1E0CF14A05C00006", "第%s条、银行账号：%s 已被锁定，请刷新后重试！"), index, bankaccount_name));
            }
            Json json = new Json(lockRowData.toString());
            List<BankReconciliation> billList = Objectlizer.decode(json, BankReconciliation.ENTITY_NAME);
            List<BankReconciliation> successList = new ArrayList<>();
            for (BankReconciliation bankReconciliation : billList) {
                if (bankReconciliation.getConfirmstatus() != null
                        && ConfirmStatusEnum.Confirming.getIndex().equals(bankReconciliation.getConfirmstatus())) {
                    ProcessUtil.addMessage(uid, String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1E0CF1BC05C00007", "第%s条、银行账号：%s 未确认账户使用组织，无需取消确认！"), hang.get(bankReconciliation.getId().toString()), bankReconciliation.getString("bankaccount_name")));
                    continue;
                }
                if (bankReconciliation.getConfirmbill() != null
                        && OrgConfirmBillEnum.CMP_BILLCLAIMCENTER.getIndex().equals(bankReconciliation.getConfirmbill())) {
                    ProcessUtil.addMessage(uid, String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1E0CF1EA05800002", "第%s条、银行账号：%s 的账户使用组织是通过'到账认领'节点完成的确认，如需取消，请通过该节点取消！"), hang.get(bankReconciliation.getId().toString()), bankReconciliation.getString("bankaccount_name")));
                    continue;
                }
                //先校验所选数据是否 确认状态=已确认、是否发布=否、日记账是否已勾对=否、总账是否已勾对=否、业务关联状态=未关联、退票状态=空
                if (!ConfirmStatusEnum.Confirmed.getIndex().equals(bankReconciliation.getConfirmstatus())
                        || bankReconciliation.getIspublish()
                        || bankReconciliation.getCheckflag()
                        || bankReconciliation.getOther_checkflag()
                        || (bankReconciliation.getAssociationstatus() != null && bankReconciliation.getAssociationstatus() != 0)
                        || bankReconciliation.getRefundstatus() != null) {
                    ProcessUtil.addMessage(uid, String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1E7E991005200009", "第%s条、银行账号：%s 包含退票/疑似退票、未确认、已发布、已关联单据或已经参与对账的数据，请检查！"), hang.get(bankReconciliation.getId().toString()), bankReconciliation.getString("bankaccount_name")));
                    continue;
                }
                // 清空授权使用组织
                bankReconciliation.setAccentity(null);
                //清空对应会计主体
                bankReconciliation.setAccentityRaw(null);
                // 确认状态 = 未确认
                bankReconciliation.setConfirmstatus(ConfirmStatusEnum.Confirming.getIndex());
                // 授权组织确认单据-null
                bankReconciliation.setConfirmbill(null);
                bankReconciliation.setEntityStatus(EntityStatus.Update);
                successList.add(bankReconciliation);
                ProcessUtil.refreshProcess(uid, true, 1);
            }
            // 修改完成
            CommonSaveUtils.updateBankReconciliation(successList);
            //ProcessUtil.completedResetCount(uid);
        } catch (Exception e) {
            log.error("asyncCancelConfirmFormBank", e);
            ProcessUtil.addMessage(uid, e.getMessage());
        } finally {
            CacheUtils.unlockBill(BankReconciliation.ENTITY_NAME, lockResult.getJSONArray("lockRowData"));
            ProcessUtil.completedResetCount(uid);
        }
    }


    @Override
    public CtmJSONObject cancelConfirm(List<BankReconciliation> billList) throws Exception {
        CtmJSONObject result = new CtmJSONObject();
        List<String> messages = new ArrayList<>();
        if (billList == null || billList.size() == 0) {
            throw new Exception(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00180", "请选择单据！") /* "请选择单据！" */);
        }
        int i = 0;
        CtmJSONObject failed = new CtmJSONObject();
        List<BankReconciliation> successList = new ArrayList<>();
        for (BankReconciliation bankReconciliation : billList) {
            //判断认领金额
            if (bankReconciliation.getAmounttobeclaimed() != null && bankReconciliation.getTran_amt() != null
                    && (bankReconciliation.getAmounttobeclaimed().compareTo(bankReconciliation.getTran_amt()) < 0)) {
                failed.put(bankReconciliation.getId().toString(), bankReconciliation.getId().toString());
                if (bankReconciliation.getBank_seq_no() != null) {
                    messages.add(String.format(InternationalUtils.getMessageWithDefault("UID:P_CM-BE_20C73D4405080002", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540048F", "银行流水号:[%s]已发生后续业务，不允许进行该操作，请确认！") /* "银行流水号:[%s]已发生后续业务，不允许进行该操作，请确认！" */) /* "银行流水号:%s]已发生后续业务，不允许进行该操作，请确认！" */, bankReconciliation.getBank_seq_no()));
                } else {
                    messages.add(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540048C", "银行流水号:[]已发生后续业务，不允许进行该操作，请确认！") /* "银行流水号:[]已发生后续业务，不允许进行该操作，请确认！" */);
                }
                i++;
                continue;
            } else if (bankReconciliation.getAssociationstatus() == AssociationStatus.Associated.getValue()) {
                failed.put(bankReconciliation.getId().toString(), bankReconciliation.getId().toString());
                if (bankReconciliation.getBank_seq_no() != null) {
                    messages.add(String.format(InternationalUtils.getMessageWithDefault("UID:P_CM-BE_20C73D4405080002", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540048F", "银行流水号:[%s]已发生后续业务，不允许进行该操作，请确认！") /* "银行流水号:[%s]已发生后续业务，不允许进行该操作，请确认！" */) /* "银行流水号:%s]已发生后续业务，不允许进行该操作，请确认！" */, bankReconciliation.getBank_seq_no()));
                } else {
                    messages.add(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540048C", "银行流水号:[]已发生后续业务，不允许进行该操作，请确认！") /* "银行流水号:[]已发生后续业务，不允许进行该操作，请确认！" */);
                }
                i++;
                continue;
            }
            if (bankReconciliation.getConfirmbill() != null
                    && OrgConfirmBillEnum.CMP_BANKRECONCILIATION.getIndex().equals(bankReconciliation.getConfirmbill())) {
                failed.put(bankReconciliation.getId().toString(), bankReconciliation.getId().toString());
                if (bankReconciliation.getBank_seq_no() != null) {
                    messages.add(String.format(InternationalUtils.getMessageWithDefault("UID:P_CM-BE_20C73C3605D80008", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400493", "银行流水号:[%s]的账户使用组织是通过'银行流水认领'节点完成的确认，如需取消，请通过该节点取消！") /* "银行流水号:[%s]的账户使用组织是通过'银行流水认领'节点完成的确认，如需取消，请通过该节点取消！" */) /* "银行流水号:%s]的账户使用组织是通过'银行流水认领'节点完成的确认，如需取消，请通过该节点取消!" */, bankReconciliation.getBank_seq_no()));
                } else {
                    messages.add(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400494", "银行流水号:[]的账户使用组织是通过'银行流水认领'节点完成的确认，如需取消，请通过该节点取消！") /* "银行流水号:[]的账户使用组织是通过'银行流水认领'节点完成的确认，如需取消，请通过该节点取消！" */);
                }
                i++;
                continue;
            }
            if (bankReconciliation.getConfirmstatus() != null
                    && ConfirmStatusEnum.Confirming.getIndex().equals(bankReconciliation.getConfirmstatus())) {
                failed.put(bankReconciliation.getId().toString(), bankReconciliation.getId().toString());
                if (bankReconciliation.getBank_seq_no() != null) {
                    messages.add(String.format(InternationalUtils.getMessageWithDefault("UID:P_CM-BE_2068C1EA05080007", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400498", "银行流水号:[%s]未确认账户使用组织，无需取消确认！") /* "银行流水号:[%s]未确认账户使用组织，无需取消确认！" */) /* "银行流水号:%s]未确认账户使用组织，无需取消确认！" */, bankReconciliation.getBank_seq_no()));
                } else {
                    messages.add(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540048B", "银行流水号:[]未确认账户使用组织，无需取消确认！") /* "银行流水号:[]未确认账户使用组织，无需取消确认！" */);
                }
                i++;
                continue;
            }
            // 清空授权使用组织
            bankReconciliation.setAccentity(null);
            //清空对应会计主体
            bankReconciliation.setAccentityRaw(null);
            // 确认状态 = 未确认
            bankReconciliation.setConfirmstatus(ConfirmStatusEnum.Confirming.getIndex());
            // 授权组织确认单据-null
            bankReconciliation.setConfirmbill(null);
            bankReconciliation.setEntityStatus(EntityStatus.Update);
            successList.add(bankReconciliation);
        }
        // 修改完成
        CommonSaveUtils.updateBankReconciliation(successList);
        String message = String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00183", "共：[%s]张单据；[%s]张取消确认成功；[%s]张取消确认失败！") /* "共：[%s]张单据；[%s]张取消确认成功；[%s]张取消确认失败！" */, billList.size(), (billList.size() - i), i);
        result.put("msg", message);
        result.put("msgs", messages);
        result.put("messages", messages);
        result.put("count", billList.size());
        result.put("sucessCount", successList.size());
        result.put("failCount", i);
        if (failed.size() > 0) {
            result.put("failed", failed);
        }
        return result;
    }

    @Override
    public CtmJSONObject cancelConfirmFormBank(List<BankReconciliation> billList) throws Exception {
        CtmJSONObject result = new CtmJSONObject();
        List<String> messages = new ArrayList<>();
        if (billList == null || billList.size() == 0) {
            throw new Exception(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00180", "请选择单据！") /* "请选择单据！" */);
        }
        int i = 0;
        CtmJSONObject failed = new CtmJSONObject();
        List<BankReconciliation> successList = new ArrayList<>();
        for (BankReconciliation bankReconciliation : billList) {
            if (bankReconciliation.getConfirmbill() != null
                    && OrgConfirmBillEnum.CMP_BILLCLAIMCENTER.getIndex().equals(bankReconciliation.getConfirmbill())) {
                failed.put(bankReconciliation.getId().toString(), bankReconciliation.getId().toString());
                if (bankReconciliation.getBank_seq_no() != null) {
                    messages.add(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_2068C06405A80008", "银行流水号:%s的账户使用组织是通过'到账认领'节点完成的确认，如需取消，请通过该节点取消！") /* "银行流水号:%s的账户使用组织是通过'到账认领'节点完成的确认，如需取消，请通过该节点取消！" */, bankReconciliation.getBank_seq_no()));
                } else {
                    messages.add(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400492", "银行流水号:[]的账户使用组织是通过'到账认领'节点完成的确认，如需取消，请通过该节点取消！") /* "银行流水号:[]的账户使用组织是通过'到账认领'节点完成的确认，如需取消，请通过该节点取消！" */);
                }
                i++;
                continue;
            }
            if (bankReconciliation.getConfirmstatus() != null
                    && ConfirmStatusEnum.Confirming.getIndex().equals(bankReconciliation.getConfirmstatus())) {
                failed.put(bankReconciliation.getId().toString(), bankReconciliation.getId().toString());
                if (bankReconciliation.getBank_seq_no() != null) {
                    messages.add(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_2068C1EA05080007", "银行流水号:[%s]未确认账户使用组织，无需取消确认！") /* "银行流水号:%s]未确认账户使用组织，无需取消确认！" */, bankReconciliation.getBank_seq_no()));
                } else {
                    messages.add(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540048B", "银行流水号:[]未确认账户使用组织，无需取消确认！") /* "银行流水号:[]未确认账户使用组织，无需取消确认！" */);
                }
                i++;
                continue;
            }
            //先校验所选数据是否 确认状态=已确认、是否发布=否、日记账是否已勾对=否、总账是否已勾对=否、业务关联状态=未关联、退票状态=空
            if (!ConfirmStatusEnum.Confirmed.getIndex().equals(bankReconciliation.getConfirmstatus())
                    || bankReconciliation.getIspublish()
                    || bankReconciliation.getCheckflag()
                    || bankReconciliation.getOther_checkflag()
                    || (bankReconciliation.getAssociationstatus() != null && bankReconciliation.getAssociationstatus() != 0)
                    || bankReconciliation.getRefundstatus() != null) {
                failed.put(bankReconciliation.getId().toString(), bankReconciliation.getId().toString());
                if (bankReconciliation.getBank_seq_no() != null) {
                    messages.add(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_2068C38405A80004", "银行流水号:%s包含退票/疑似退票、未确认、已发布、已关联单据或已经参与对账的数据，请检查！") /* "银行流水号:%s包含退票/疑似退票、未确认、已发布、已关联单据或已经参与对账的数据，请检查！" */, bankReconciliation.getBank_seq_no()));
                } else {
                    messages.add(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400491", "银行流水号:[]包含退票/疑似退票、未确认、已发布、已关联单据或已经参与对账的数据，请检查！") /* "银行流水号:[]包含退票/疑似退票、未确认、已发布、已关联单据或已经参与对账的数据，请检查！" */);
                }
                i++;
                continue;
            }
            // 清空授权使用组织
            bankReconciliation.setAccentity(null);
            //清空对应会计主体
            bankReconciliation.setAccentityRaw(null);
            // 确认状态 = 未确认
            bankReconciliation.setConfirmstatus(ConfirmStatusEnum.Confirming.getIndex());
            // 授权组织确认单据-null
            bankReconciliation.setConfirmbill(null);
            bankReconciliation.setEntityStatus(EntityStatus.Update);
            successList.add(bankReconciliation);
        }
        // 修改完成
        CommonSaveUtils.updateBankReconciliation(successList);
        String message = String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00183", "共：[%s]张单据；[%s]张取消确认成功；[%s]张取消确认失败！") /* "共：[%s]张单据；[%s]张取消确认成功；[%s]张取消确认失败！" */, billList.size(), (billList.size() - i), i);
        result.put("msg", message);
        result.put("msgs", messages);
        result.put("messages", messages);
        result.put("count", billList.size());
        result.put("sucessCount", successList.size());
        result.put("failCount", i);
        if (failed.size() > 0) {
            result.put("failed", failed);
        }
        return result;
    }

    /**
     * 取消单据关联
     *
     * @param id
     * @return
     * @throws Exception
     */
    @Override
    @Transactional
    public CtmJSONObject cancelCorrelate(Long id) throws Exception {

        String key = ICmpConstant.MY_BILL_CLAIM_LIST + id;
        YmsLock ymsLock = JedisLockUtils.lockBillWithOutTrace(key);
        if (null == ymsLock) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101190"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00143", "该数据正在处理，请稍后重试！") /* "该数据正在处理，请稍后重试！" */);
        }
        SettleDeatailRelBankBillReqVO settleDeatailRelBankBillReqVO = new SettleDeatailRelBankBillReqVO();
        CtmJSONObject result = new CtmJSONObject();
        result.put("dealSucceed", false);
        try {
            BillClaim billClaim = MetaDaoHelper.findById(BillClaim.ENTITY_NAME, id);
            if (billClaim == null) {
                result.put("dealSucceed", false);
                result.put(ICmpConstant.MSG, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400499", "未查询到对应认领单") /* "未查询到对应认领单" */);
            } else {
                //CZFW-392651 RPT0341:”资金切分=‘是’，且实际结算主体结算状态=‘结算成功’时，不允许取消关联“。提示“已触发后续的资金切分流程，暂不支持取消关联，请检查！”
                if (!Objects.isNull(billClaim.getIsfundsplit()) && billClaim.getIsfundsplit() && !Objects.isNull(billClaim.getSettlestatus()) && billClaim.getSettlestatus() == FundSettleStatus.SettleSuccess.getValue()) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101191"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1DAAC8E005800002", "已触发后续的资金切分流程，暂不支持取消关联，请检查！") /* "已触发后续的资金切分流程，暂不支持取消关联，请检查！" */);
                }
                // 根据认领单查询银行对账单
                String bankreconId = billClaim.items().get(0).getBankbill().toString();
                BankReconciliation bankReconciliation = MetaDaoHelper.findById(BankReconciliation.ENTITY_NAME, bankreconId);
                // 判断银行对账单关联类型
                List<BankReconciliationbusrelation_b> bankReconciliationbusrelation_bs = bankReconciliation.BankReconciliationbusrelation_b();
                if (bankReconciliationbusrelation_bs == null || bankReconciliationbusrelation_bs.size() == 0) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101192"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A05080102", "取消单据关联失败：当前认领单对应的银行流水未关联资金结算明细单据！") /* "取消单据关联失败：当前认领单对应的银行流水未关联资金结算明细单据！" */);
                } else if (bankReconciliationbusrelation_bs.size() == 1) {
                    StringBuilder stringBuilder = new StringBuilder();
                    BankReconciliationbusrelation_b bankReconciliationbusrelation_b = bankReconciliationbusrelation_bs.get(0);
                    // 判断关联单据
                    if (bankReconciliationbusrelation_b.getBilltype() == EventType.StwbSettleMentDetails.getValue()) {
                        // 自动关联 或 手工关联
                        if (bankReconciliationbusrelation_b.getRelationtype() != Relationtype.AutoAssociated.getValue() &&
                                bankReconciliationbusrelation_b.getRelationtype() != Relationtype.ManualAssociated.getValue()) {
                            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101193"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A05080103", "取消单据关联失败：当前认领单对应的银行流水关联类型非自动关联/手工关联！") /* "取消单据关联失败：当前认领单对应的银行流水关联类型非自动关联/手工关联！" */);
                        }
                    } else {
                        String resultMessage = "";
                        if (bankReconciliationbusrelation_b.getRelationtype() == Relationtype.MakeBillAssociated.getValue()) {
                            resultMessage = String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_210488CE04880014", "当前认领单关联的银行交易流水号[%s]，通过生单操作，生成了下游单据，请先删除下游单据！") /* "当前认领单关联的银行交易流水号[%s]，通过生单操作，生成了下游单据，请先删除下游单据！" */, bankReconciliation.getBank_seq_no());
                        }
                        if (bankReconciliationbusrelation_b.getRelationtype() == Relationtype.ThreePartyAssociated.getValue()) {
                            resultMessage = String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_210488CE04880015", "当前认领单关联的银行交易流水号[%s]，通过三方对账关联，关联了下游非资金结算明细单据，如需取消关联，请先删除下游单据！") /* "当前认领单关联的银行交易流水号[%s]，通过三方对账关联，关联了下游非资金结算明细单据，如需取消关联，请先删除下游单据！" */, bankReconciliation.getBank_seq_no());
                        }
                        if (bankReconciliationbusrelation_b.getRelationtype() == Relationtype.ManualAssociated.getValue()) {
                            resultMessage = String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_210488CE04880016", "当前认领单关联的银行交易流水号[%s]，下游非资金结算明细单据，如需取消关联，请先删除下游单据！") /* "当前认领单关联的银行交易流水号[%s]，下游非资金结算明细单据，如需取消关联，请先删除下游单据！" */, bankReconciliation.getBank_seq_no());
                        }
                        if (bankReconciliationbusrelation_b.getRelationtype() == Relationtype.AutoAssociated.getValue()) {
                            resultMessage = String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_210488CE04880017", "当前认领单关联的银行交易流水号[%s]，通过自动关联，关联了下游非资金结算明细单据，如需取消关联，请先删除下游单据！") /* "当前认领单关联的银行交易流水号[%s]，通过自动关联，关联了下游非资金结算明细单据，如需取消关联，请先删除下游单据！" */, bankReconciliation.getBank_seq_no());
                        }
                        throw new CtmException(resultMessage);
                    }
                    settleDeatailRelBankBillReqVO.setSettleBenchB_id(bankReconciliation.BankReconciliationbusrelation_b().get(0).getBillid().toString());
                    settleDeatailRelBankBillReqVO.setBankCheck_id(bankReconciliation.getId().toString());
                    // 先删除我们的关系，删除成功之后再调用结算接口取消关联，结算那边需要同步调整。只调整V5。
                    CommonRequestDataVo json = new CommonRequestDataVo();
                    // 业务单据id
                    json.setStwbbusid(bankReconciliationbusrelation_bs.get(0).getBillid());
                    // 认领单id
                    json.setClaimId(id);
                    ctmcmpReWriteBusRpcService.resDelDataForRpc(json);
                } else {
                    String resultMessage = "";
                    Map<Long, BankReconciliationbusrelation_b> bankReconciliationbusrelationBMap = bankReconciliationbusrelation_bs.stream().collect(Collectors.toMap(k1 -> k1.getBillid(), k2 -> k2, (k1, k2) -> k1));
                    QuerySchema schema = QuerySchema.create().addSelect("id");
                    schema.appendQueryCondition(QueryCondition.name("relateClaimBillId").eq(id));
                    //获取结算单子表信息
                    List<Map<String, Object>> settleBenchBId = MetaDaoHelper.query("stwb.settlebench.SettleBench_b", schema);
                    if (settleBenchBId != null && settleBenchBId.size() > 0) {
                        Long ibillid = (Long) settleBenchBId.get(0).get("id");
                        BankReconciliationbusrelation_b bankReconciliationbus = bankReconciliationbusrelationBMap.get(ibillid);
                        // 自动关联 或 手工关联
                        if (bankReconciliationbus != null && (bankReconciliationbus.getRelationtype() == Relationtype.AutoAssociated.getValue() ||
                                bankReconciliationbus.getRelationtype() == Relationtype.ManualAssociated.getValue()) &&
                                (bankReconciliationbus.getBilltype() == EventType.StwbSettleMentDetails.getValue())) {
                            settleDeatailRelBankBillReqVO.setSettleBenchB_id(bankReconciliationbus.getBillid().toString());
                            // 先删除我们的关系，删除成功之后再调用结算接口取消关联，结算那边需要同步调整。只调整V5。
                            CommonRequestDataVo json = new CommonRequestDataVo();
                            // 业务单据id
                            json.setStwbbusid(bankReconciliationbus.getBillid());
                            // 认领单id
                            json.setClaimId(id);

                            ctmcmpReWriteBusRpcService.resDelDataForRpc(json);
                            // 判断关联单据
                        }
                    }

                    if (StringUtils.isEmpty(settleDeatailRelBankBillReqVO.getSettleBenchB_id())) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101194"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A05080100", "取消单据关联失败：当前认领单对应的银行流水关联单据非资金结算明细单据，不允许取消单据关联！") /* "取消单据关联失败：当前认领单对应的银行流水关联单据非资金结算明细单据，不允许取消单据关联！" */);
                    }
                }

                settleDeatailRelBankBillReqVO.setClaim_id(billClaim.getId().toString());
                if (bankReconciliation.getRefundstatus() != null && bankReconciliation.getRefundstatus().equals(RefundStatus.Refunded.getValue())) {
                    //传入退票状态和退票金额
                    settleDeatailRelBankBillReqVO.setIsrefund(String.valueOf(bankReconciliation.getRefundstatus()));
                    settleDeatailRelBankBillReqVO.setRefundAmt(bankReconciliation.getTran_amt());
                    settleDeatailRelBankBillReqVO.setIsrefund("1");//退票状态 1- 退票
                } else { //非退票.必传，不然结算会报错
                    settleDeatailRelBankBillReqVO.setIsrefund(String.valueOf(0));
                    settleDeatailRelBankBillReqVO.setRefundAmt(new BigDecimal(String.valueOf(0)));
                }
                //CZFW-394170【DSP支持问题】单据取消关联，显示未与结算明细关联，无法取消。多个的时候找不明白结算明细主键 所以这里不传了 结算会同步修改不用这个字段查询了 直接用认领单主键查询
                settleDeatailRelBankBillReqVO.setSettleBenchB_id(null);
                CtmJSONObject logParams = new CtmJSONObject();
                logParams.put("billClaim", billClaim);
                ctmcmpBusinessLogService.saveBusinessLog(logParams, billClaim.getCode(), "认领单", IServicecodeConstant.BILLCLAIMCARD, IMsgConstant.BILLCLAIMCARD, "认领单取消单据关联");//@notranslate
                ResultStrRespVO resultStrRespVO = settleBenchBRPCService.cancelRelationSettleBench(settleDeatailRelBankBillReqVO);
                result.put("dealSucceed", true);
                result.put("data", resultStrRespVO);
            }
        } catch (Exception e) {
            result.put("dealSucceed", false);
            result.put(ICmpConstant.MSG, e.getMessage());
            throw new CtmException(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B7D3E404300E4B", "取消单据关联失败：%s") /* "取消单据关联失败：%s" */, e.getMessage()));
        } finally {
            JedisLockUtils.unlockBillWithOutTrace(ymsLock);
        }
        return result;
    }

    @Override
    public void massClaim(BillContext billContext, BillDataDto billDataDto, String optType) throws Exception {
        // 触发保存与提交
        if ("saveAndSubmit".equals(optType)) {
            RuleExecuteResult ruleExecuteResult = billBpmService.doSaveAndSubmitActionWithAssignCheck(billDataDto);
//            RuleExecuteResult ruleExecuteResult_ = new RuleOperatorProxy("submit").executeRule(billContext, billDataDto);
            if (ruleExecuteResult != null) {
                log.info(ruleExecuteResult.toString());
            }
            return;
        }

        // 触发保存
        RuleExecuteResult ruleExecuteResult = new RuleOperatorProxy("save").executeRule(billContext, billDataDto);
        if (ruleExecuteResult != null) {
            log.info(ruleExecuteResult.toString());
        }
    }

    @Override
    public Map<String, Object> supportTransferRuleAfterAddRule(List<BizObject> bills) throws Exception {
        if (bills.size() == 1) {
            BizObject bizObject = new BizObject();
            bizObject.init(bills.get(0));
            ConvertParam convertParam = StwbBillBuilder.buildConvertParam(bizObject, "banktoclaim", "ctm-cmp.cmp_bankreconciliation", null);
            //调用转换规则服务 开始转换单据
            DomainMakeBillRuleModel makeBillRuleModel = AppContext.getBean(BusinessConvertService.class).queryMakeBillRule(convertParam);
            if (Objects.isNull(makeBillRuleModel)) {
                throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540048D", "调用转换规则服务，转换单据失败！") /* "调用转换规则服务，转换单据失败！" */);
            }
            ConvertResult convert = AppContext.getBean(BusinessConvertService.class).convert(convertParam, makeBillRuleModel);
            if (Objects.isNull(convert) || com.yonyou.cloud.utils.CollectionUtils.isEmpty(convert.getConvertedBillList())) {
                throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540048D", "调用转换规则服务，转换单据失败！") /* "调用转换规则服务，转换单据失败！" */);
            }
            return convert.getConvertedBillList().get(0).getTargetData();
        } else {
            List<BillClaimItem> billClaimItems = new ArrayList<>();
            BillClaim billClaim = new BillClaim();
            billClaim.setEntityStatus(EntityStatus.Insert);
            Map<String, Object> originBillClaimCharacterDef = null;
            Map<String, Boolean> ignoreCharacDefMap = new HashMap<>();
            for (Map bizObject : bills) {
                BizObject bankObject = new BizObject();
                bankObject.init(bizObject);
                BillClaimItem billClaimItem = new BillClaimItem();
                ConvertParam convertParam = StwbBillBuilder.buildConvertParam(bankObject, "banktoclaim", "ctm-cmp.cmp_bankreconciliation", null);
                //调用转换规则服务 开始转换单据
                DomainMakeBillRuleModel makeBillRuleModel = AppContext.getBean(BusinessConvertService.class).queryMakeBillRule(convertParam);
                if (Objects.isNull(makeBillRuleModel)) {
                    throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540048D", "调用转换规则服务，转换单据失败！") /* "调用转换规则服务，转换单据失败！" */);
                }
                ConvertResult convert = AppContext.getBean(BusinessConvertService.class).convert(convertParam, makeBillRuleModel);
                if (Objects.isNull(convert) || com.yonyou.cloud.utils.CollectionUtils.isEmpty(convert.getConvertedBillList())) {
                    throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540048D", "调用转换规则服务，转换单据失败！") /* "调用转换规则服务，转换单据失败！" */);
                }
                Map<String, Object> targetData = convert.getConvertedBillList().get(0).getTargetData();
                billClaimItem.init((Map<String, Object>) ((List) targetData.get("items")).get(0));
                billClaimItems.add(billClaimItem);

                Map<String, Object> billClaimCharacterDef = (Map<String, Object>) targetData.get("billClaimCharacterDef");
                if (originBillClaimCharacterDef == null) {
                    originBillClaimCharacterDef = billClaimCharacterDef;
                } else {
                    if (billClaimCharacterDef != null) {
                        for (Map.Entry<String, Object> entry : billClaimCharacterDef.entrySet()) {
                            String key = entry.getKey();
                            String value = Jackson.toJsonString(entry.getValue());
                            String originValue = Jackson.toJsonString(originBillClaimCharacterDef.get(key));
                            if (!value.equals(originValue)) {
                                ignoreCharacDefMap.put(key, true);
                            }
                        }
                    }
                }
            }
            Map<String, Object> resultBillClaimCharacterDef = new HashMap<>();
            if (originBillClaimCharacterDef != null) {
                for (Map.Entry<String, Object> entry : originBillClaimCharacterDef.entrySet()) {
                    String key = entry.getKey();
                    Object value = entry.getValue();
                    if (ignoreCharacDefMap.get(key) == null) {
                        resultBillClaimCharacterDef.put(key, value);
                    }
                }
            }
            billClaim.set("billClaimCharacterDef", resultBillClaimCharacterDef);
            billClaim.setItems(billClaimItems);
            return billClaim;
        }
    }


    @Override
    public void handResult(ConvertResult convertResult, Map<String, Object> paramMap, Map<String, String> param, String bustype) throws Exception {
        List<Map<String, String>> requestDataList = (List<Map<String, String>>) paramMap.get("requestData");

        //可能有重复id，用Set去下重，防止重复查询
        Set<String> billClaimIdSet = new HashSet<>();
        Set<String> bankAccountIdSet = new HashSet<>();
        Set<String> orgIdSet = new HashSet<>();
        for (Map<String, String> requestData : requestDataList) {
            String billClaimId = requestData.get("billClaimId");
            billClaimIdSet.add(billClaimId);
        }
        Map<String, BillClaim> idToBillClaimMap = new HashMap<>();
        for (String billClaimId : billClaimIdSet) {
            //暂无支持3层查询的批量方法，改动有风险。所以批量生单的场景先不优化，保持原逻辑
            BillClaim billClaim = MetaDaoHelper.findById(BillClaim.ENTITY_NAME, billClaimId, 3);
            idToBillClaimMap.put(billClaimId, billClaim);
            // 收集企业银行账号ID
            if (billClaim != null && !com.yonyou.cloud.utils.StringUtils.isEmpty(billClaim.getBankaccount())) {
                bankAccountIdSet.add(billClaim.getBankaccount());
            }
        }
        // 批量查询结算方式（参数固定为system_0001，只需查询一次）
        List<SettleMethodModel> settleMethodList = querySettleMethodList();

        // 批量查询企业银行账号
        Map<String, EnterpriseBankAcctVO> bankAccountMap = queryBankAccountMap(bankAccountIdSet);

        // 收集所有orgId用于批量查询
        for (ConvertedBill convertedBill : convertResult.getConvertedBillList()) {
            Object orgObj = convertedBill.getTargetData().get("org");
            if (orgObj != null) {
                orgIdSet.add(orgObj.toString());
            }
        }
        // 批量查询业务单元
        Map<String, BizObject> orgMap = queryOrgMap(orgIdSet);
        if (requestDataList.size() <= 1) {
            // 单条时，需要判断是单条生单还是批量生单，因为参数来源不同
            String billClaimId = this.getBillClaimId(param, requestDataList);
            String billClaimPubTs = this.getBillClaimPubTs(param, requestDataList);
            BillClaim billClaim = MetaDaoHelper.findById(BillClaim.ENTITY_NAME, billClaimId, 3);
            this.handleOneData(convertResult.getTargetData(), bustype, billClaimId, requestDataList.get(0).get("billClaimPubTs"), billClaim, settleMethodList, bankAccountMap, orgMap);
            return;
        }

        // 批量生单多条数据时，直接基于转换的数据进行重组。
        // 记录转换后的数据
        Map<String, ConvertedBill> orderIdToTargetDataMap = this.getOrderIdMapping(convertResult.getConvertedBillList());
        //批量生单时，id不会重复。单条多子表时，会有重复id，用Set去下重，防止重复查询
        Map<String, ConvertResult.DimensionData> dimensionKeyMap = this.getDimensionKeyMap(convertResult.getDimensionKeys());

        List<ConvertedBill> newBillList = new ArrayList<>();
        List<ConvertResult.DimensionData> newDimensionKeys = new ArrayList<>();
        for (Map<String, String> requestData : requestDataList) {
            String orderId = requestData.get("id");
            // 复制转换的数据
            String srcObjStr = CtmJSONObject.toJSONString(orderIdToTargetDataMap.get(orderId));
            ConvertedBill orderConvertedBill = CtmJSONObject.parseObject(srcObjStr, ConvertedBill.class);
            if (orderConvertedBill != null) {
                String billClaimId = requestData.get("billClaimId");
                BillClaim billClaim = idToBillClaimMap.get(billClaimId);
                this.handleOneData(orderConvertedBill.getTargetData(), bustype, billClaimId, requestData.get("billClaimPubTs"), billClaim, settleMethodList, bankAccountMap, orgMap);
                newBillList.add(orderConvertedBill);
                // 处理分单规则名称
                ConvertResult.DimensionData dimensionDataCopy = this.handleDimensionKey(orderConvertedBill, dimensionKeyMap, billClaim.getCode());
                newDimensionKeys.add(dimensionDataCopy);
            }
        }
        // 去除重复的 DimensionData，保持原有顺序
        List<ConvertResult.DimensionData> uniqueDimensionKeys = new ArrayList<>(new LinkedHashSet<>(newDimensionKeys));
        convertResult.setDimensionKeys(uniqueDimensionKeys);
        convertResult.setConvertedBillList(newBillList);
        convertResult.setSuccessCount(newBillList.size());
        convertResult.setSucessCount(newBillList.size());
        // 取第一条数据作为返回
        convertResult.setTargetData(newBillList.get(0).getTargetData());
    }

    /**
     * 查询结算方式列表
     */
    private List<SettleMethodModel> querySettleMethodList() throws Exception {
        SettleMethodQueryParam settleMethodQueryParam = new SettleMethodQueryParam();
        settleMethodQueryParam.setCode("system_0001");
        settleMethodQueryParam.setIsEnabled(1);
        settleMethodQueryParam.setTenantId(AppContext.getTenantId());
        settleMethodQueryParam.setServiceAttr(0);
        return baseRefRpcService.querySettleMethods(settleMethodQueryParam);
    }

    /**
     * 批量查询企业银行账号
     */
    private Map<String, EnterpriseBankAcctVO> queryBankAccountMap(Set<String> bankAccountIdSet) throws Exception {
        Map<String, EnterpriseBankAcctVO> bankAccountMap = new HashMap<>();
        if (CollectionUtils.isEmpty(bankAccountIdSet)) {
            return bankAccountMap;
        }
        List<String> idList = new ArrayList<>(bankAccountIdSet);
        List<EnterpriseBankAcctVO> bankAccountList = EnterpriseBankQueryService.findByIdList(idList);
        if (CollectionUtils.isNotEmpty(bankAccountList)) {
            for (EnterpriseBankAcctVO vo : bankAccountList) {
                if (vo != null && vo.getId() != null) {
                    bankAccountMap.put(vo.getId(), vo);
                }
            }
        }
        return bankAccountMap;
    }

    /**
     * 批量查询业务单元
     */
    private Map<String, BizObject> queryOrgMap(Set<String> orgIdSet) throws Exception {
        Map<String, BizObject> orgMap = new HashMap<>();
        if (CollectionUtils.isEmpty(orgIdSet)) {
            return orgMap;
        }
        //QuerySchema querySchema = new QuerySchema().addSelect("id,code,name");
        //QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        //conditionGroup.appendCondition(QueryCondition.name("id").in(new ArrayList<>(orgIdSet)));
        //querySchema.addCondition(conditionGroup);
        //List<BizObject> bizObjects = MetaDaoHelper.queryObject("org.func.BaseOrg", querySchema, "ucf-org-center");
        //if (CollectionUtils.isNotEmpty(bizObjects)) {
        //    for (BizObject bizObject : bizObjects) {
        //        String id = bizObject.getString("id");
        //        if (id != null) {
        //            orgMap.put(id, bizObject);
        //        }
        //    }
        //}
        // 调用iOrgUnitQueryService.listByIds批量查询业务单元
        List<Integer> statusList = new ArrayList<>();
        //只查启用的
        statusList.add(1);
        //对orgIdSet进行分组，每组大小为1000，批量查询
        List<OrgUnitDTO> orgUnitDTOList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(orgIdSet)) {
            String tenantId = InvocationInfoProxy.getTenantid();
            if (StringUtils.isNotEmpty(tenantId)) {
                try {
                    List<String> orgIdList = new ArrayList<>(orgIdSet);
                    int batchSize = 1000;
                    for (int i = 0; i < orgIdList.size(); i += batchSize) {
                        List<String> batch = orgIdList.subList(i, Math.min(i + batchSize, orgIdList.size()));
                        List<OrgUnitDTO> batchResult = iOrgUnitQueryService.listByIds(tenantId, batch, statusList);
                        if (CollectionUtils.isNotEmpty(batchResult)) {
                            orgUnitDTOList.addAll(batchResult);
                        }
                    }
                } catch (Exception e) {
                    log.error("批量查询业务单元失败, orgIdSet: {}", orgIdSet, e);
                }
            } else {
                log.error("tenantId为空，跳过业务单元查询");
            }
        }
        if (CollectionUtils.isNotEmpty(orgUnitDTOList)) {
            for (OrgUnitDTO orgUnitDTO : orgUnitDTOList) {
                if (orgUnitDTO != null && orgUnitDTO.getId() != null) {
                    // 将OrgUnitDTO转换为BizObject
                    BizObject bizObject = new BizObject();
                    bizObject.set("id", orgUnitDTO.getId());
                    bizObject.set("code", orgUnitDTO.getCode());
                    bizObject.set("name", orgUnitDTO.getName());
                    orgMap.put(orgUnitDTO.getId(), bizObject);
                }
            }
        }
        return orgMap;
    }

    private String getBillClaimPubTs(Map<String, String> param, List<Map<String, String>> requestDataList) {
        String billclaimpubts = ReportStringUtils.paramIsNull(MapUtils.getString(param, "billclaimpubts"));
        Boolean isBatch = this.getIsBatch(param);
        return isBatch ? requestDataList.get(0).get("billClaimPubTs") : billclaimpubts;
    }

    private String getBillClaimId(Map<String, String> param, List<Map<String, String>> requestDataList) {
        String billclaimid = ReportStringUtils.paramIsNull(MapUtils.getString(param, "billclaimid"));
        Boolean isBatch = this.getIsBatch(param);
        return isBatch ? requestDataList.get(0).get("billClaimId") : billclaimid;
    }

    private Boolean getIsBatch(Map<String, String> param) {
        Boolean isBatch = MapUtils.getBoolean(param, "isBatchConvertBill");
        return isBatch != null && isBatch;
    }

    private ConvertResult.DimensionData handleDimensionKey(ConvertedBill orderConvertedBill, Map<String, ConvertResult.DimensionData> dimensionKeyMap, String billClaimCode) {
        // 设置 dimensionKeys
        String oldGroupKey = orderConvertedBill.getGroupKey();

        // 复制一个DimensionData
        String srcObjStr = CtmJSONObject.toJSONString(dimensionKeyMap.get(oldGroupKey));
        ConvertResult.DimensionData dimensionDataCopy = CtmJSONObject.parseObject(srcObjStr, ConvertResult.DimensionData.class);

        // 给复制的设置新值
        String currentKey = billClaimCode + "+" + oldGroupKey;
        dimensionDataCopy.setName(currentKey);
        dimensionDataCopy.setKey(billClaimCode + "+" + dimensionDataCopy.getKey());
        dimensionDataCopy.setTargetData(orderConvertedBill.getTargetData());

        // 重新设置新的groupKey
        orderConvertedBill.setGroupKey(currentKey);

        return dimensionDataCopy;
    }

    private Map<String, ConvertResult.DimensionData> getDimensionKeyMap(List<ConvertResult.DimensionData> dimensionKeys) {
        return dimensionKeys
                .stream()
                .collect(Collectors.toMap(
                        ConvertResult.DimensionData::getName,
                        Function.identity(),
                        (existing, replacement) -> existing
                ));
    }

    private Map<String, ConvertedBill> getOrderIdMapping(List<ConvertedBill> convertedBillList) {
        // 先去重再转换
        List<ConvertedBill> uniqueBills = convertedBillList.stream()
                .collect(Collectors.toMap(
                        bill -> bill.getTargetData().get("sourceId").toString(),
                        Function.identity(),
                        (existing, replacement) -> existing
                ))
                .values()
                .stream()
                .collect(Collectors.toList());

        return uniqueBills.stream()
                .collect(Collectors.toMap(
                        convertedBill -> convertedBill.getTargetData().get("sourceId").toString(),
                        Function.identity()
                ));
    }

    /**
     * 处理单条认领单数据，设置目标数据的认领单关联信息、金额、结算方式、银行账户等字段
     *
     * @param targetData 目标数据Map，用于存储处理后的数据
     * @param bustype 交易类型
     * @param billclaimid 认领单ID
     * @param billclaimpubts 认领单发布时间戳
     * @param billClaim 认领单实体对象
     * @param settleMethodList 结算方式列表，批量查询结果（可为null，为null时内部查询）
     * @param bankAccountMap 企业银行账号映射表，key为账号ID，value为账号对象（可为null）
     * @param orgMap 业务单元映射表，key为组织ID，value为组织对象（可为null）
     * @throws Exception 处理过程中的异常
     */
    private void handleOneData(Map<String, Object> targetData, String bustype, String billclaimid, String billclaimpubts, BillClaim billClaim,
                               List<SettleMethodModel> settleMethodList, Map<String, EnterpriseBankAcctVO> bankAccountMap, Map<String, BizObject> orgMap) throws Exception {
        //传递关联的认领单和认领单pubts，用来做关联回调处理
        targetData.put("billclaimid", billclaimid);
        targetData.put("billclaimpubts", billclaimpubts);
        //srcClaimld,收款单要求该字段
        targetData.put("srcClaimId", billclaimid);
        targetData.put("pubts", billclaimpubts);
        //收款单子表数据
        List<Map<String, Object>> bodyItem = (List<Map<String, Object>>) targetData.get("bodyItem");
        if (bodyItem == null || bodyItem.isEmpty()) {
            return;
        }
        List<SettleMethodModel> dataList = settleMethodList;

        //将收款金额替换为本次收款金额
        List<BillClaimBusVoucherInfo> busVoucherInfoList = billClaim.busVoucherInfoList();

        //主表企业银行账号赋值（从传入的批量查询结果中获取）
        EnterpriseBankAcctVO bankAcctVO = null;
        if (!com.yonyou.cloud.utils.StringUtils.isEmpty(billClaim.getBankaccount())) {
            bankAcctVO = bankAccountMap != null ? bankAccountMap.get(billClaim.getBankaccount()) : null;
            if (bankAcctVO != null) {
                targetData.put("enterpriseBankAccount", bankAcctVO.getId());
                targetData.put("enterpriseBankAccountName", bankAcctVO.getName());
            }
        }

        for (BillClaimBusVoucherInfo busVoucherInfo : busVoucherInfoList) {
            for (Map<String, Object> itemMap : bodyItem) {
                if (itemMap.get("srcBillRowId") != null && busVoucherInfo.getBillitmeid().equals(itemMap.get("srcBillRowId").toString())) {
                    itemMap.put("oriTaxIncludedAmount", busVoucherInfo.getClaimamount());
                    //CZFW-510440【预发】【收付】收款单，流水关联合同，生成收款单，不应该赋值预占金额为生单金额
                    if (Objects.nonNull(billClaim.getBusvouchercorr_billtype()) && !billClaim.getBusvouchercorr_billtype().equals(Short.valueOf("3"))) {
                        //CZFW-353732 修改预占用金额
                        itemMap.put("oriOccupyAmount", busVoucherInfo.getClaimamount());
                    }
                    if (Objects.nonNull(billClaim.getBusvouchercorr_billtype())&&billClaim.getBusvouchercorr_billtype().equals(Short.valueOf("1"))) {
                        //BIP-BUG-00082307 添加应核销金额
                        itemMap.put("verifyAmount", busVoucherInfo.getClaimamount());
                    }
                    //已结算补单标识传递为1
                    itemMap.put("blnSettlePatch", 1);
                    //CZFW-481612 认领单财资统一对账码传递到下游
                    itemMap.put("checkNo", billClaim.getSmartcheckno());
                    //结算方式,默认银行转账
                    if (!CollectionUtils.isEmpty(dataList)) {
                        SettleMethodModel settlementWayMap = dataList.get(0);
                        itemMap.put("settleMode", settlementWayMap.getId());
                        itemMap.put("settleModeCode", settlementWayMap.getCode());
                        itemMap.put("settleModeName", settlementWayMap.getName());
                        itemMap.put("settleModeServiceAttr", settlementWayMap.getServiceAttr());
                    }
                    if (bankAcctVO != null) {
                        itemMap.put("enterpriseBankAccount", bankAcctVO.getId());
                        itemMap.put("enterpriseBankAccountName", bankAcctVO.getName());
                    }
                }
            }
        }
        //只推单已关联的明细数据
        Iterator<Map<String, Object>> iterator = bodyItem.iterator();
        while (iterator.hasNext()) {
            Map<String, Object> itemData = iterator.next();
            boolean isContainItem = false;
            for (BillClaimBusVoucherInfo busVoucherInfo : busVoucherInfoList) {
                if (itemData.get("srcBillRowId") != null && busVoucherInfo.getBillitmeid().equals(itemData.get("srcBillRowId").toString())) {
                    isContainItem = true;
                }
            }
            if (!isContainItem) {
                iterator.remove();
            }
        }

        targetData.put("bodyItem", bodyItem);

        //清除上游单据业务流id，bizFlowId
        targetData.put("bizflowId", null);

        targetData.put("testAmount", "100");
        //交易类型配置
        if (bustype != null) {
            targetData.put("bustype", bustype);
        }
        //移动端业务单元没有值，手动添加orgName（从传入的批量查询结果中获取）
        if (targetData.get("org") != null) {
            String org = targetData.get("org").toString();
            BizObject orgBizObject = orgMap != null ? orgMap.get(org) : null;
            if (orgBizObject != null) {
                targetData.put("orgName", orgBizObject.getString("name"));
                targetData.put("orgCode", orgBizObject.getString("code"));
            }
        }

        //CZFW-403365 移动端mdf2ynf时，业务流那边需要业务方传递一个参数来判断是否合并子表数据
        targetData.put("mergeDetailAddChildData", true);
    }

}
