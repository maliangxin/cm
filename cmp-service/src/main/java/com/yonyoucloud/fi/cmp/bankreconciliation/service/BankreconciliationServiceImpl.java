package com.yonyoucloud.fi.cmp.bankreconciliation.service;


import com.yonyou.iuap.bd.base.BdRestSingleton;
import com.yonyou.iuap.bd.pub.param.ConditionVO;
import com.yonyou.iuap.bd.pub.param.Operator;
import com.yonyou.iuap.bd.pub.param.Page;
import com.yonyou.iuap.bd.pub.util.Condition;
import com.yonyou.iuap.bd.pub.util.Sorter;
import com.yonyou.iuap.bd.staff.dto.Staff;
import com.yonyou.iuap.bd.staff.service.itf.IStaffService;
import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.iuap.event.model.BusinessEvent;
import com.yonyou.iuap.event.model.BusinessEventBuilder;
import com.yonyou.iuap.event.service.EventService;
import com.yonyou.iuap.org.dto.BaseDeptDTO;
import com.yonyou.iuap.org.service.itf.core.IBizDeptQueryService;
import com.yonyou.iuap.ucf.common.i18n.MessageUtils;
import com.yonyou.iuap.yms.id.generator.YmsOidGenerator;
import com.yonyou.iuap.yms.lock.YmsLock;
import com.yonyou.uap.tenant.service.itf.ITenantRoleUserService;
import com.yonyou.uap.tenantauth.entity.TenantRole;
import com.yonyou.ucf.basedoc.model.CurrencyTenantDTO;
import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVO;
import com.yonyou.ucf.basedoc.model.ProjectDTO;
import com.yonyou.ucf.basedoc.service.itf.IProjectService;
import com.yonyou.ucf.mdd.ext.bill.rule.util.BillInfoUtils;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.dao.sql.SqlHelper;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyou.yonbip.ctm.CtmAppContext;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.logger.business.CTMCMPBusinessLogService;
import com.yonyoucloud.ctm.stwb.api.settlebench.SettleBenchBRPCService;
import com.yonyoucloud.ctm.stwb.openapi.IOpenApiService;
import com.yonyoucloud.ctm.stwb.reqvo.IncomeAndExpenditureReqVO;
import com.yonyoucloud.ctm.stwb.reqvo.SettleDeatailRelBankBillReqVO;
import com.yonyoucloud.ctm.stwb.respvo.IncomeAndExpenditureResVO;
import com.yonyoucloud.ctm.stwb.respvo.ResultStrRespVO;
import com.yonyoucloud.ctm.stwb.unifiedsettle.pubitf.IBankTradeFlowInfoPubQueryService;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.cmp.accountrealtimebalance.AccountRealtimeBalance;
import com.yonyoucloud.fi.cmp.bankelectronicreceipt.BankElectronicReceipt;
import com.yonyoucloud.fi.cmp.bankreconciliation.*;
import com.yonyoucloud.fi.cmp.bankreconciliation.enums.BankreconciliationActionEnum;
import com.yonyoucloud.fi.cmp.bankreconciliation.utils.CommonParametersUtils;
import com.yonyoucloud.fi.cmp.billclaim.BillClaim;
import com.yonyoucloud.fi.cmp.billclaim.BillClaimItem;
import com.yonyoucloud.fi.cmp.billclaim.BillClaimItemVO;
import com.yonyoucloud.fi.cmp.billclaim.service.BillClaimService;
import com.yonyoucloud.fi.cmp.cmpentity.*;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.common.service.CtmCmpCheckRepeatDataService;
import com.yonyoucloud.fi.cmp.constant.*;
import com.yonyoucloud.fi.cmp.enums.BankReconciliationActions;
import com.yonyoucloud.fi.cmp.enums.PublishedType;
import com.yonyoucloud.fi.cmp.event.constant.IEventCenterConstant;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.common.service.IBankReconciliationCommonService;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.consts.BankDealDetailConst;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.consts.DealDetailEnumConst;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.api.bankrecilication.CtmcmpReWriteBusRpcService;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.Direction;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.bankreconciliation.BankReconciliationVo;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.bankreconciliation.BankReconciliationbusrelationVo;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.common.CommonRequestDataVo;
import com.yonyoucloud.fi.cmp.reconciliate.vo.BankReceiptInfoVO;
import com.yonyoucloud.fi.cmp.reconciliate.vo.BankReceiptToVoucherVO;
import com.yonyoucloud.fi.cmp.util.*;
import com.yonyoucloud.fi.cmp.util.bankreconciliation.checkandfilter.BankreconciliationUtils;
import com.yonyoucloud.fi.cmp.util.basedoc.CurrencyQueryService;
import com.yonyoucloud.fi.stct.api.openapi.IBusinessDelegationApiService;
import com.yonyoucloud.fi.stct.api.openapi.common.dto.Result;
import com.yonyoucloud.fi.stct.api.openapi.request.QueryDelegationReqVo;
import com.yonyoucloud.fi.stct.api.openapi.vo.businessDelegation.BusinessDelegationVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.mapstruct.ap.internal.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = RuntimeException.class)
public class BankreconciliationServiceImpl implements BankreconciliationService {

    private final BillClaimService billClaimService;

    private final CTMCMPBusinessLogService ctmcmpBusinessLogService;
    @Autowired
    BaseRefRpcService baseRefRpcService;
    @Autowired
    YmsOidGenerator ymsOidGenerator;
    @Autowired
    private ITenantRoleUserService tenantRoleUserService;
    @Autowired
    private EventService eventService;
    @Autowired
    SettleBenchBRPCService settleBenchBRPCService;
    @Resource
    IBusinessDelegationApiService iBusinessDelegationApiService;
    @Resource
    IOpenApiService iOpenApiService;

    @Autowired
    CtmcmpReWriteBusRpcService ctmcmpReWriteBusRpcService;

    @Autowired
    private IBankReconciliationCommonService iBankReconciliationCommonService;
    @Autowired
    private IBizDeptQueryService bizDeptQueryService;
    @Autowired
    private IProjectService projectService;
    @Autowired
    private CurrencyQueryService currencyQueryService;
    @Autowired
    private BankPublishSendMsgService bankPublishSendMsgService;
    @Autowired
    private CtmCmpCheckRepeatDataService ctmCmpCheckRepeatDataService;
    @Autowired
    private IBankTradeFlowInfoPubQueryService iBankTradeFlowInfoPubQueryService;


    /**
     * 流水发布处理接口
     *
     * @param id        流水id
     * @param bankSeqNo 流水号
     * @param params    publishType：发布类型（枚举类PublishedType）；publishToData：发布到的参照数据
     */
    @Override
    public CtmJSONObject publish(Long id, String bankSeqNo, Map<String, Object> params) throws Exception {
        String key = ICmpConstant.CMPBANKRECONCILIATIONLIST + id;
        YmsLock ymsLock = JedisLockUtils.lockBillWithOutTrace(key);
        if (null == ymsLock) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101672"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418003B", "该数据正在处理，请稍后重试！") /* "该数据正在处理，请稍后重试！" */);
        }
        CtmJSONObject result = new CtmJSONObject();
        result.put("dealSucceed", false);
        try {
            BankReconciliation bankReconciliation = MetaDaoHelper.findById(BankReconciliation.ENTITY_NAME, id);
            if (null == bankReconciliation) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101957"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180035", "银行交易流水号：") /* "银行交易流水号：" */ + (bankSeqNo == null ? "[]" : bankSeqNo) + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418003F", ",不存在！请刷新后重试。") /* ",不存在！请刷新后重试。" */);
            }
            if (bankReconciliation.getSerialdealtype() != null && ClaimCompleteType.NoProcess.getValue() ==
                    (bankReconciliation.getSerialdealtype())) {
                throw new CtmException(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1E9838EA05A80003", "交易流水号【%s】已标记无需处理，不允许进行该操作，请检查！"), bankSeqNo/* "交易流水号【%s】已标记无需处理，不允许进行该操作，请检查！" */));
            }
            //业务关联状态=‘未关联’，且，是否发布=‘否’
            if (bankReconciliation.getIsadvanceaccounts() && bankReconciliation.getEntrytype() == EntryType.Hang_Entry.getValue()) {
                //如果是提前入账挂账的话也能发布
            } else {
                if (null == bankReconciliation.getAssociationstatus() || bankReconciliation.getAssociationstatus() != 0) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101957"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180035", "银行交易流水号：") /* "银行交易流水号：" */ + (bankReconciliation.getBank_seq_no() == null ? "[]" : bankReconciliation.getBank_seq_no()) + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180041", ",不是未关联状态，请刷新后重试！") /* ",不是未关联状态，请刷新后重试！" */);
                }
            }
            if (bankReconciliation.getIspublish()) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101957"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180035", "银行交易流水号：") /* "银行交易流水号：" */ + (bankReconciliation.getBank_seq_no() == null ? "[]" : bankReconciliation.getBank_seq_no()) + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180043", ",已发布！") /* ",已发布！" */);
            }
            if ((ObjectUtils.isNotEmpty(bankReconciliation.getCheckflag()) && bankReconciliation.getCheckflag()) || (ObjectUtils.isNotEmpty(bankReconciliation.getOther_checkflag()) && bankReconciliation.getOther_checkflag())) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101957"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180035", "银行交易流水号：") /* "银行交易流水号：" */ + (bankReconciliation.getBank_seq_no() == null ? "[]" : bankReconciliation.getBank_seq_no()) + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180048", ",已勾对单据不允许发布！！") /* ",已勾对单据不允许发布！！" */);
            }
//            if (ObjectUtils.isNotEmpty(bankReconciliation.getAutobill()) && bankReconciliation.getAutobill()) {
//                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101957"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180035","银行交易流水号：") /* "银行交易流水号：" */ + bankReconciliation.getBank_seq_no() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418004B",",已生单单据不允许发布！") /* ",已生单单据不允许发布！" */);
//            }
            //疑似退票，不能发布
            if (bankReconciliation.getRefundstatus() != null && bankReconciliation.getRefundstatus() == RefundStatus.MaybeRefund.getValue()) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101958"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00142", "银行交易流水号：") /* "银行交易流水号：" */ + (bankReconciliation.getBank_seq_no() == null ? "[]" : bankReconciliation.getBank_seq_no()) + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00141", "当前银行对账单为疑似退票，请先进行退票确认。") /* "当前银行对账单为疑似退票，请先进行退票确认。" */);
            }
            //已同步至三方对账 不能删除
            if (bankReconciliation.getTripleSynchronStatus() == TripleSynchronStatus.AlreadyManual.getValue() || bankReconciliation.getTripleSynchronStatus() == TripleSynchronStatus.AlreadyAuto.getValue()) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101959"), String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418004E", "银行交易流水号[%s]已同步至三方对账，无法发布！") /* "银行交易流水号[%s]已同步至三方对账，无法发布！" */, (bankReconciliation.getBank_seq_no() == null ? "[]" : bankReconciliation.getBank_seq_no())));
            }
            if (null != bankReconciliation.getFrozenstatus() && bankReconciliation.getFrozenstatus() == FrozenStatus.Unfreezing.getValue()) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101960"), String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1818746205B0000C", "银行交易流水号：%s,在解冻中不允许发布！") /* "银行交易流水号：%s,在解冻中不允许发布！" */, (bankReconciliation.getBank_seq_no() == null ? "[]" : bankReconciliation.getBank_seq_no())));
//                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101958"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00142", "银行交易流水号：") /* "银行交易流水号：" */ + bankReconciliation.getBank_seq_no() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00146", ",在解冻中不允许发布！") /* ",在解冻中不允许发布！" */);
            }
            //财资统一对账码是解析过来的，不允许发布
            if (bankReconciliation.getIsparsesmartcheckno()) {
                throw new CtmException(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1F3C4D0605100002", "银行交易流水号[%s]流水支持处理方式为“仅关联”，请与相应收付单据进行关联，避免重复生单！") /* "银行交易流水号[%s]流水支持处理方式为“仅关联”，请与相应收付单据进行关联，避免重复生单！" */, (bankReconciliation.getBank_seq_no() == null ? "[]" : bankReconciliation.getBank_seq_no())));
            }
            //发布的时候如果提前入账为否----》正常  取消发布设置入账类型为空
            //发布的时候如果是提前入账为是-----》冲挂账  取消的时候入账类型变为挂账
            if (bankReconciliation.getIsadvanceaccounts()) {
                bankReconciliation.setEntrytype(EntryType.CrushHang_Entry.getValue());
                bankReconciliation.setVirtualEntryType(EntryType.CrushHang_Entry.getValue());
            } else {
                bankReconciliation.setEntrytype(EntryType.Normal_Entry.getValue());
                bankReconciliation.setVirtualEntryType(EntryType.Normal_Entry.getValue());
            }
            bankReconciliation.setIspublish(true);
            bankReconciliation.setClaimamount(BigDecimal.ZERO);
            bankReconciliation.setAmounttobeclaimed(bankReconciliation.getTran_amt());
            bankReconciliation.setProcessstatus(DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_PROCESS_FINISH.getStatus());
            bankReconciliation.setPublishman(AppContext.getCurrentUser().getId());
            bankReconciliation.setPublish_time(new Date());
            //智能到账，发布对象类型解析
            Short publishType = params.get("publishType") != null ? Short.parseShort(params.get("publishType").toString()) : PublishedType.ORG.getCode();
            bankReconciliation.setPublished_type(publishType);
            bankReconciliation.setPublished_type(publishType);
            bankReconciliation.setPublish_time(new Date());
            bankReconciliation.setSerialauto(false);
            EntityTool.setUpdateStatus(bankReconciliation);
            bankReconciliation.setBank_seq_no(bankReconciliation.getBank_seq_no());
            bankReconciliation.setOperateSourceEnum(OperateSourceEnum.PUBLISH);
            // 修改完成
            CommonSaveUtils.updateBankReconciliation(bankReconciliation);

            //按组织发布保持之前逻辑
            if (PublishedType.ORG.getCode() == publishType) {
                iBankReconciliationCommonService.insertBankreconciliationDetailNew(bankReconciliation, OprType.Publish.getValue(), null, null);
            } else {
                if (StringUtils.isEmpty(bankReconciliation.getAccentity())) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101962"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A05080083", "账户使用组织为空，请先进行确认。") /* "账户使用组织为空，请先进行确认。" */);
                }
                //智能到账，发布到用户，角色，部门，员工
                iBankReconciliationCommonService.handlePublishToOthers(bankReconciliation, publishType, params);
            }
            ctmcmpBusinessLogService.saveBusinessLog(bankReconciliation, bankReconciliation.getBank_seq_no(), "", IServicecodeConstant.CMPBANKRECONCILIATION, IServicecodeConstant.CMPBANKRECONCILIATION, IMsgConstant.CMDPUBLISH);
            result.put(ICmpConstant.MSG, ResultMessage.success());
            result.put("dealSucceed", true);
            result.put("data", bankReconciliation);
            //发布待办消息：对方类型为客户或者供应商时，发送信息到客户专员；按员工发布直接发送给对应员工
            try {
                bankPublishSendMsgService.sendPublishMsgToCreateToDo(bankReconciliation);
            } catch (Exception e) {
                log.error("BankPublishSendMsgService sendPublishMsgToCreateToDo error:{}", e.getMessage());
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-105063"), String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_22EE50E004600009", "发布待办消息失败！,错误信息：%s") /* "发布待办消息失败！,错误信息：%s" */,e.getMessage()));
            }

        } catch (Exception e) {
            result.put(ICmpConstant.MSG, e.getMessage());
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-105063"), String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_22FB795C0530000D", "银行流水发布异常：%s") /* "银行流水发布异常：%s" */,e.getMessage()));
        } finally {
            JedisLockUtils.unlockBillWithOutTrace(ymsLock);
        }
        return result;
    }

    /**
     * 取消疑重
     *
     * @param id
     * @return
     * @throws Exception
     */
    @Override
    public CtmJSONObject cancleRepeat(Long id) throws Exception {

        String key = ICmpConstant.CMPBANKRECONCILIATIONLIST + id;
        YmsLock ymsLock = JedisLockUtils.lockBillWithOutTrace(key);
        if (null == ymsLock) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101190"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00143", "该数据正在处理，请稍后重试！") /* "该数据正在处理，请稍后重试！" */);
        }
        CtmJSONObject result = new CtmJSONObject();
        result.put("dealSucceed", false);
        try {
            BankReconciliation bankReconciliation = MetaDaoHelper.findById(BankReconciliation.ENTITY_NAME, id);
            bankReconciliation.setIsRepeat((short) BankDealDetailConst.REPEAT_CONFIRM);
            bankReconciliation.setBank_seq_no(bankReconciliation.getBank_seq_no());
            EntityTool.setUpdateStatus(bankReconciliation);
            // 修改完成
            CommonSaveUtils.updateBankReconciliation(bankReconciliation);
            ctmcmpBusinessLogService.saveBusinessLog(bankReconciliation, bankReconciliation.getBank_seq_no(), IMsgConstant.CANCLE_REPEAT, IServicecodeConstant.CMPBANKRECONCILIATION, IServicecodeConstant.CMPBANKRECONCILIATION, IMsgConstant.CANCLE_REPEAT);
            result.put(ICmpConstant.MSG, ResultMessage.success());
            result.put("dealSucceed", true);
            result.put("data", bankReconciliation);
        } catch (Exception e) {
            result.put(ICmpConstant.MSG, e.getMessage());
        } finally {
            JedisLockUtils.unlockBillWithOutTrace(ymsLock);
        }
        return result;
    }

    @Override
    public CtmJSONObject cancelPublish(Long id, String bankSeqNo) throws Exception {
        String key = ICmpConstant.CMPBANKRECONCILIATIONLIST + id;
        YmsLock ymsLock = JedisLockUtils.lockBillWithOutTrace(key);
        if (null == ymsLock) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101190"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00143", "该数据正在处理，请稍后重试！") /* "该数据正在处理，请稍后重试！" */);
        }
        CtmJSONObject result = new CtmJSONObject();
        result.put("dealSucceed", false);
        try {
            BankReconciliation bankReconciliation = MetaDaoHelper.findById(BankReconciliation.ENTITY_NAME, id);
            if (null == bankReconciliation) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101957"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180035", "银行交易流水号：") /* "银行交易流水号：" */ + (bankSeqNo == null ? "[]" : bankSeqNo) + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418003F", ",不存在！请刷新后重试。") /* ",不存在！请刷新后重试。" */);
            }
            if (null != bankReconciliation.getClaimamount() && bankReconciliation.getClaimamount().compareTo(BigDecimal.ZERO) != 0) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101957"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180035", "银行交易流水号：") /* "银行交易流水号：" */ + (bankReconciliation.getBank_seq_no() == null ? "[]" : bankReconciliation.getBank_seq_no()) + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180034", ",已认领金额不为0，不可取消发布！") /* ",已认领金额不为0，不可取消发布！" */);
            }

            if (!bankReconciliation.getIspublish()) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101957"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180035", "银行交易流水号：") /* "银行交易流水号：" */ + (bankReconciliation.getBank_seq_no() == null ? "[]" : bankReconciliation.getBank_seq_no()) + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418003C", ",单据未发布") /* ",单据未发布" */);
            }

            /**
             * 校验认领单参照关联状态
             * 1，根据银行对账单id查询认领单子表，获取认领单主表id
             * 2，根据认领单主表id获取认领单信息，查看参照关联状态
             */
            List<BillClaim> list;
            QuerySchema querySchema = QuerySchema.create().addSelect("refassociationstatus");
            QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("items.bankbill").eq(bankReconciliation.getId()));
            querySchema.addCondition(group);
            list = MetaDaoHelper.queryObject(BillClaim.ENTITY_NAME, querySchema, null);
            if (list != null && list.size() > 0) {
                if (list.get(0).getRefassociationstatus() != null &&
                        AssociationStatus.Associated.getValue() == list.get(0).getRefassociationstatus()) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101963"), String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A05080085", "银行交易流水号：[%s],已认领生成下游业务，不可取消发布！") /* "银行交易流水号：[%s],已认领生成下游业务，不可取消发布！" */, bankReconciliation.getBank_seq_no() == null ? "[]" : bankReconciliation.getBank_seq_no()));
                }
            }

            //发布的时候如果提前入账为否----》正常  取消发布设置入账类型为空
            //发布的时候如果是提前入账为是-----》冲挂账  取消的时候入账类型变为挂账
            if (bankReconciliation.getIsadvanceaccounts()) {
                bankReconciliation.setEntrytype(EntryType.Hang_Entry.getValue());
                bankReconciliation.setVirtualEntryType(EntryType.Hang_Entry.getValue());
            } else {
                bankReconciliation.setEntrytype(null);
                bankReconciliation.setVirtualEntryType(null);
            }

            //清除发布到用户组织等信息
            clearPublishInfo(id);
            bankReconciliation.setIspublish(false);
            bankReconciliation.setClaimamount(BigDecimal.ZERO);
            bankReconciliation.setAmounttobeclaimed(BigDecimal.ZERO);
            bankReconciliation.setProcessstatus(DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_MATCH_NO_START.getStatus());
            bankReconciliation.setPublishman(null);
            bankReconciliation.setPublish_time(null);
            bankReconciliation.setPublished_type(null);
            bankReconciliation.setPublishrulescode(null);
            bankReconciliation.setPublishdate(null);
            bankReconciliation.setSerialauto(null);
            bankReconciliation.setProcessstatus(DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_MATCH_NO_START.getStatus());
            //财资统一对账码 非解析过来的要清除
            if (!bankReconciliation.getIsparsesmartcheckno()) {
                bankReconciliation.setSmartcheckno(bankReconciliation.getSmartcheckno());
            }
            bankReconciliation.setBillclaimstatus(BillClaimStatus.ToBeClaim.getValue());
            // 智能到账新增逻辑 取消发布的时候更新子表，修改发布状态为已作废
            iBankReconciliationCommonService.updateBankreconciliationDetail(bankReconciliation);
            EntityTool.setUpdateStatus(bankReconciliation);
            bankReconciliation.setBank_seq_no(bankReconciliation.getBank_seq_no());
            bankReconciliation.setOperateSourceEnum(OperateSourceEnum.CANCEL_PUBLISH);
            // 修改完成
            CommonSaveUtils.updateBankReconciliation(bankReconciliation);
//            ctmcmpBusinessLogService.saveBusinessLog(bankReconciliation, ""+bankReconciliation.getBank_seq_no()+bankReconciliation.getId(), IMsgConstant.CMDNOPUBLISH, IServicecodeConstant.CMPBANKRECONCILIATION, IServicecodeConstant.CMPBANKRECONCILIATION, IMsgConstant.CMDNOPUBLISH);
            result.put(ICmpConstant.MSG, ResultMessage.success());
            result.put("dealSucceed", true);
            result.put("data", bankReconciliation);

            //发布待办消息：发送待办取消接口
            try {
                bankPublishSendMsgService.handleDeleteMsg(bankReconciliation);
            } catch (Exception e) {
                log.error("BankPublishSendMsgService handleDeleteMsg error:{}", e.getMessage());
            }
        } catch (Exception e) {
            result.put(ICmpConstant.MSG, e.getMessage());
        } finally {
            JedisLockUtils.unlockBillWithOutTrace(ymsLock);
        }
        return result;
    }

    /**
     * 清除发布到用户组织等信息
     *
     * @param id
     * @throws Exception
     */
    private void clearPublishInfo(Long id) throws Exception {
        BankReconciliation bankReconciliation = MetaDaoHelper.findById(BankReconciliation.ENTITY_NAME, id, 3);
        //智能到账，清空已发布到的用户，角色，部门，员工信息
        if (bankReconciliation.getPublished_type() != null && bankReconciliation.getPublished_type() == PublishedType.USER.getCode()) {
            List<BankReconciliationPublishedUser> publishedUserList = bankReconciliation.bankReconciliationPublishedUser();
            if (!CollectionUtils.isEmpty(publishedUserList)) {
                MetaDaoHelper.delete(BankReconciliationPublishedUser.ENTITY_NAME, publishedUserList);
            }
        }
        //角色
        if (bankReconciliation.getPublished_type() != null && bankReconciliation.getPublished_type() == PublishedType.ROLE.getCode()) {
            List<BankReconciliationPublishedRole> publishedRoleList = bankReconciliation.bankReconciliationPublishedRole();
            if (!CollectionUtils.isEmpty(publishedRoleList)) {
                MetaDaoHelper.delete(BankReconciliationPublishedRole.ENTITY_NAME, publishedRoleList);
            }
        }
        //部门
        if (bankReconciliation.getPublished_type() != null && bankReconciliation.getPublished_type() == PublishedType.DEPT.getCode()) {
            List<BankReconciliationPublishedDept> publishedDeptList = bankReconciliation.bankReconciliationPublishedDept();
            if (!CollectionUtils.isEmpty(publishedDeptList)) {
                MetaDaoHelper.delete(BankReconciliationPublishedDept.ENTITY_NAME, publishedDeptList);
            }
        }
        //员工
        if (bankReconciliation.getPublished_type() != null && bankReconciliation.getPublished_type() == PublishedType.EMPLOYEE.getCode()) {
            List<BankReconciliationPublishedStaff> publishedStaffList = bankReconciliation.bankReconciliationPublishedStaff();
            if (!CollectionUtils.isEmpty(publishedStaffList)) {
                MetaDaoHelper.delete(BankReconciliationPublishedStaff.ENTITY_NAME, publishedStaffList);
            }
        }
        //指定组织
        if (bankReconciliation.getPublished_type() != null && bankReconciliation.getPublished_type() == PublishedType.ASSIGN_ORG.getCode()) {
            List<BankReconciliationPublishedAssignOrg> publishedAssignOrgList = bankReconciliation.bankReconciliationPublishedAssignOrg();
            if (!CollectionUtils.isEmpty(publishedAssignOrgList)) {
                MetaDaoHelper.delete(BankReconciliationPublishedAssignOrg.ENTITY_NAME, publishedAssignOrgList);
            }
        }
    }

    @Override
    public CtmJSONObject returnBack(List<BankReconciliation> billList, String returnreason) throws Exception {
        CtmJSONObject result = new CtmJSONObject();
        List<String> messages = new ArrayList<>();
        if (billList == null || billList.size() == 0) {
            throw new Exception(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00180", "请选择单据！") /* "请选择单据！" */);
        }
        int i = 0;
        CtmJSONObject failed = new CtmJSONObject();
        List<BankReconciliation> successList = new ArrayList<>();
        QuerySchema querySchema = QuerySchema.create().addSelect(ICmpConstant.SELECT_TOTAL_PARAM);
        QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("id").in(billList.stream().map(BankReconciliation::getId).collect(Collectors.toList())));
        querySchema.addCondition(group);
        List<BankReconciliation> bankReconciliations = MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME, querySchema, null);
        for (BankReconciliation bankReconciliation : bankReconciliations) {
            //退回时，判定交易金额是否等于待认领金额;如不相等，说明已经进行部分认领或全部认领，此时不允许退回，阻断，提示“银行交易流水号XXXYYY已认领，不支持退回，请检查!"
            if (bankReconciliation.getAmounttobeclaimed() != null && bankReconciliation.getTran_amt() != null
                    && (bankReconciliation.getAmounttobeclaimed().compareTo(bankReconciliation.getTran_amt()) < 0)) {
                failed.put(bankReconciliation.getId().toString(), bankReconciliation.getId().toString());
                messages.add(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400581", "银行流水号:") /* "银行流水号:" */ + bankReconciliation.getBank_seq_no() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400580", "已认领，不支持退回，请检查!") /* "已认领，不支持退回，请检查!" */);
                i++;
                continue;
            }
            // 智能到账新增逻辑 退回的时候更新子表
            iBankReconciliationCommonService.insertBankreconciliationDetailNew(bankReconciliation,
                    OprType.Return.getValue(), returnreason, null);
            //发布的时候如果提前入账为否----》正常  取消发布设置入账类型为空
            //发布的时候如果是提前入账为是-----》冲挂账  取消的时候入账类型变为挂账
            if (bankReconciliation.getIsadvanceaccounts()) {
                bankReconciliation.setEntrytype(EntryType.Hang_Entry.getValue());
                bankReconciliation.setVirtualEntryType(EntryType.Hang_Entry.getValue());
            } else {
                bankReconciliation.setEntrytype(null);
                bankReconciliation.setVirtualEntryType(null);
            }
            //清除发布到用户组织等信息
            clearPublishInfo(bankReconciliation.getId());
            bankReconciliation.setIspublish(false);
            bankReconciliation.setClaimamount(BigDecimal.ZERO);
            bankReconciliation.setAmounttobeclaimed(BigDecimal.ZERO);
            bankReconciliation.setProcessstatus(DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_MATCH_NO_START.getStatus());
            bankReconciliation.setPublishman(null);
            bankReconciliation.setPublish_time(null);
            bankReconciliation.setPublished_type(null);
            bankReconciliation.setPublishrulescode(null);
            bankReconciliation.setPublishdate(null);
            bankReconciliation.setSerialauto(null);
            //财资统一对账码 非解析过来的要清除
            if (!bankReconciliation.getIsparsesmartcheckno()) {
                bankReconciliation.setSmartcheckno(bankReconciliation.getSmartcheckno());
            }
//            EntityTool.setUpdateStatus(bankReconciliation);
            // 修改完成
            CommonSaveUtils.updateBankReconciliation(bankReconciliation);
            ctmcmpBusinessLogService.saveBusinessLog(bankReconciliation, bankReconciliation.getBank_seq_no(), "退回", IServicecodeConstant.CMPBANKRECONCILIATION, IServicecodeConstant.CMPBANKRECONCILIATION, IMsgConstant.CMDNOPUBLISH);//@notranslate
            successList.add(bankReconciliation);
            SendBizMessageUtils.sendBizMessageNew(bankReconciliation, "cmp_bankreconciliation", "return", "ctm-cmp.cmp_bankreconciliation");
            //发布待办消息：发送待办取消接口
            try {
                bankPublishSendMsgService.handleDeleteMsg(bankReconciliation);
            } catch (Exception e) {
                log.error("BankPublishSendMsgService handleDeleteMsg error:{}", e.getMessage());
            }
        }
        String message = String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540057F", "共：[%s]张单据；[%s]张退回成功；[%s]张退回失败！") /* "共：[%s]张单据；[%s]张退回成功；[%s]张退回失败！" */, billList.size(), (billList.size() - i), i);
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
    public List<BillClaimItemVO> findClaimes(Long id) throws Exception {
        return billClaimService.queryBillClaimInfo(id);
    }

    @Override
    public CtmJSONObject returnBill(Long id, String bankSeqNo, String returnreason) throws Exception {
        String key = ICmpConstant.CMPBANKRECONCILIATIONLIST + id;
        CtmJSONObject result = new CtmJSONObject();
        result.put("dealSucceed", false);
        YmsLock ymsLock = JedisLockUtils.lockBillWithOutTrace(key);
        try {
            if (null == ymsLock) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101190"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00143", "该数据正在处理，请稍后重试！") /* "该数据正在处理，请稍后重试！" */);
            }
            BankReconciliation bankReconciliation = MetaDaoHelper.findById(BankReconciliation.ENTITY_NAME, id);
            if (null == bankReconciliation) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101957"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180035", "银行交易流水号：") /* "银行交易流水号：" */ + (bankSeqNo == null ? "[]" : bankSeqNo) + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418003F", ",不存在！请刷新后重试。") /* ",不存在！请刷新后重试。" */);
            }

            String errMessage = BankreconciliationUtils.checkDataLegal(bankReconciliation, BankreconciliationActionEnum.BATCHRETURNBILL);
            if (Strings.isNotEmpty(errMessage)) {
                throw new CtmException(errMessage);
            }
            String roleType = ICmpConstant.SYNTHESIZE_POST;
            List<TenantRole> roles = tenantRoleUserService.findRolesByUserId(AppContext.getCurrentUser().getYhtUserId(), AppContext.getYTenantId().toString(), "diwork");
            String roleCodeLine = "";
            if (null != roles) {
                for (TenantRole role : roles) {
                    roleCodeLine += role.getRoleCode() + "----";
                }
            }
            if (roleCodeLine.contains(ICmpConstant.FINANCIAL_POST)) {
                roleType = ICmpConstant.FINANCIAL_POST;
            }
            if (roleCodeLine.contains(ICmpConstant.BUSINESS_POST)) {
                roleType = ICmpConstant.BUSINESS_POST;
            }

            if (ICmpConstant.FINANCIAL_POST.equals(roleType)) {
                if (null == bankReconciliation.getDistributestatus() || DispatchFinanceStatus.Not.getValue() == bankReconciliation.getDistributestatus()) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101957"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180035", "银行交易流水号：") /* "银行交易流水号：" */ + (bankSeqNo == null ? "[]" : bankSeqNo) + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418003E", "未分配，不能退回！") /* "未分配，不能退回！" */);
                }
                if (null != bankReconciliation.getPublishdistributestatus() && PublishDistributeStatus.Not.getValue() != bankReconciliation.getPublishdistributestatus()) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101957"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180035", "银行交易流水号：") /* "银行交易流水号：" */ + (bankSeqNo == null ? "[]" : bankSeqNo) + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180038", "已分配业务人员，不能退回！") /* "已分配业务人员，不能退回！" */);
                }
            } else if (ICmpConstant.BUSINESS_POST.equals(roleType)) {
                if (null == bankReconciliation.getPublishdistributestatus() || PublishDistributeStatus.Not.getValue() == bankReconciliation.getPublishdistributestatus()) {
                    if (null != bankReconciliation.getDistributestatus() && DispatchFinanceStatus.Not.getValue() != bankReconciliation.getDistributestatus() && roleCodeLine.contains(ICmpConstant.FINANCIAL_POST)) {
                        roleType = ICmpConstant.FINANCIAL_POST;
                    } else {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101957"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180035", "银行交易流水号：") /* "银行交易流水号：" */ + (bankSeqNo == null ? "[]" : bankSeqNo) + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418003E", "未分配，不能退回！") /* "未分配，不能退回！" */);
                    }
                }
                if (null == bankReconciliation.getDistributestatus() || DispatchFinanceStatus.Not.getValue() == bankReconciliation.getDistributestatus()) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101957"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180035", "银行交易流水号：") /* "银行交易流水号：" */ + (bankSeqNo == null ? "[]" : bankSeqNo) + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180040", "未分配财务人员，不能退回！") /* "未分配财务人员，不能退回！" */);
                }
            }

            QuerySchema queryInitDataSchema = QuerySchema.create().addSelect("*");
            QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
            conditionGroup.addCondition(QueryCondition.name("mainid").eq(bankReconciliation.getId()));
            conditionGroup.addCondition(QueryCondition.name("autheduser").eq(AppContext.getCurrentUser().getYhtUserId()));
            conditionGroup.addCondition(QueryCondition.name("return_reason").is_null());
            queryInitDataSchema.addCondition(conditionGroup);
            List<BankReconciliationDetail> bankReconciliationDetailList = MetaDaoHelper.queryObject(BankReconciliationDetail.ENTITY_NAME, queryInitDataSchema, null);

            if (null != bankReconciliationDetailList && bankReconciliationDetailList.size() > 0) {
                for (BankReconciliationDetail bankReconciliationDetail : bankReconciliationDetailList) {
//                    bankReconciliationDetail.setAutheduser(AppContext.getCurrentUser().getYhtUserId());
//                    bankReconciliationDetail.setOprdate(new Date());
//                    bankReconciliationDetail.setOperator(AppContext.getCurrentUser().getId());
                    bankReconciliationDetail.setReturn_reason(returnreason);
                    bankReconciliationDetail.setReturndate(new Date());
                    if (ICmpConstant.BUSINESS_POST.equals(roleType)) {//业务人员退回时，要设置财务人员为空，防止只有一个业务人员退回记录时，综合岗看不到数据
                        bankReconciliationDetail.setEmployee_financial(null);
                    }
                    EntityTool.setUpdateStatus(bankReconciliationDetail);
                    MetaDaoHelper.update(BankReconciliationDetail.ENTITY_NAME, bankReconciliationDetail);
                }
            } else {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101957"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180035", "银行交易流水号：") /* "银行交易流水号：" */ + (bankSeqNo == null ? "[]" : bankSeqNo) + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180050", "没有当前用户的分派信息") /* "没有当前用户的分派信息" */);
            }
            fetchDistributestatus(roleType, bankReconciliation);
//            EntityTool.setUpdateStatus(bankReconciliation);
            // 分配功能，本期先不做修改
            CommonSaveUtils.updateBankReconciliation(bankReconciliation);
            result.put(ICmpConstant.MSG, ResultMessage.success());
            result.put("dealSucceed", true);
            result.put("data", bankReconciliation);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            result.put(ICmpConstant.MSG, e.getMessage());
            result.put("bank_seq_no", bankSeqNo);
        } finally {
            JedisLockUtils.unlockBillWithOutTrace(ymsLock);
        }
        return result;
    }

    private void fetchDistributestatus(String roleType, BankReconciliation bankReconciliation) throws Exception {
        QuerySchema queryInitDataSchema = QuerySchema.create().addSelect("*");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.addCondition(QueryCondition.name("mainid").eq(bankReconciliation.getId()));
        conditionGroup.addCondition(QueryCondition.name("autheduser").is_not_null());
        conditionGroup.addCondition(QueryCondition.name("return_reason").is_null());
        queryInitDataSchema.addCondition(conditionGroup);
        List<BankReconciliationDetail> bankReconciliationDetailList = MetaDaoHelper.queryObject(BankReconciliationDetail.ENTITY_NAME, queryInitDataSchema, null);

        if (null == bankReconciliationDetailList || bankReconciliationDetailList.size() == 0) {
            bankReconciliation.setIsreturned(true);
            if (ICmpConstant.FINANCIAL_POST.equals(roleType)) {
                bankReconciliation.setDistributestatus(DispatchFinanceStatus.Not.getValue());
            } else if (ICmpConstant.BUSINESS_POST.equals(roleType)) {
                bankReconciliation.setPublishdistributestatus(PublishDistributeStatus.Not.getValue());
            }
        } else {

            List<String> userIds = new ArrayList<>();
            for (BankReconciliationDetail bankReconciliationDetail : bankReconciliationDetailList) {
                userIds.add(bankReconciliationDetail.getAutheduser());
            }
            boolean undistributed = true;
            out:
            for (BankReconciliationDetail bankReconciliationDetail : bankReconciliationDetailList) {
                String oprtype = bankReconciliationDetail.getOprtype();
                if ((OprType.AutoFinance.getValue().equals(oprtype) || OprType.ManualFinance.getValue().equals(oprtype)) && ICmpConstant.FINANCIAL_POST.equals(roleType)) {
                    undistributed = false;
                    break out;
                } else if ((OprType.AutoBusiness.getValue().equals(oprtype) || OprType.ManualBusiness.getValue().equals(oprtype)) && ICmpConstant.BUSINESS_POST.equals(roleType)) {
                    undistributed = false;
                    break out;
                }
            }
            if (undistributed) {
                bankReconciliation.setIsreturned(true);
                if (ICmpConstant.FINANCIAL_POST.equals(roleType)) {
                    bankReconciliation.setDistributestatus(DispatchFinanceStatus.Not.getValue());
                } else if (ICmpConstant.BUSINESS_POST.equals(roleType)) {
                    bankReconciliation.setPublishdistributestatus(PublishDistributeStatus.Not.getValue());
                    updateDispatchFinancialData(bankReconciliation, false);
                }
            }
        }
    }

    /**
     * 【分配业务人员】
     *
     * @param id      银行对账单ID
     * @param userids 对接人
     * @param isAuto  true自动分配业务人员(自动分配业务对接人) false手工分配业务人员(通过分配业务人员按钮)
     * @return
     * @throws Exception
     */
    @Override
    public CtmJSONObject dispatchBussiness(String id, String[] userids, boolean isAuto) throws Exception {
        BankReconciliation bankReconciliation = MetaDaoHelper.findById(BankReconciliation.ENTITY_NAME, id, 3);
        String bankSeqNo = bankReconciliation.getBank_seq_no();
        CtmJSONObject result = new CtmJSONObject();
        result.put("dealSucceed", false);
        checkBeforeDispatchBussiness(bankReconciliation, bankSeqNo);
        setBankReconciliation(bankReconciliation, isAuto);
        setDispatchDetail(bankReconciliation, userids, isAuto);
        updateDispatchFinancialData(bankReconciliation, true);
        EntityTool.setUpdateStatus(bankReconciliation);
        // 分配功能，本期先不做修改
        CommonSaveUtils.updateBankReconciliation(bankReconciliation);
        ctmcmpBusinessLogService.saveBusinessLog(bankReconciliation, bankReconciliation.getBank_seq_no(), "", IServicecodeConstant.CMPBANKRECONCILIATION, IServicecodeConstant.CMPBANKRECONCILIATION, IMsgConstant.CMDPUBLISHDISPATCH);
        result.put(ICmpConstant.MSG, ResultMessage.success());
        result.put("dealSucceed", true);
        result.put("data", bankReconciliation);
        return result;
    }

    /**
     * 分配业务人员后,给分配的财务人员记录上加一个假的业务人员;反之清空假数据，用来配合数据权限
     *
     * @param bankReconciliation
     */
    private void updateDispatchFinancialData(BankReconciliation bankReconciliation, boolean tobussines) throws Exception {
        QuerySchema queryFinancialSchema = QuerySchema.create().addSelect("*");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        String[] oprtypes = new String[]{OprType.ManualFinance.getValue(), OprType.AutoFinance.getValue()};
        conditionGroup.addCondition(QueryCondition.name("oprtype").in((Object) oprtypes));
        conditionGroup.addCondition(QueryCondition.name("mainid").eq(bankReconciliation.getId()));
        queryFinancialSchema.addCondition(conditionGroup);
        List<BankReconciliationDetail> financialDetails = MetaDaoHelper.queryObject(BankReconciliationDetail.ENTITY_NAME, queryFinancialSchema, null);
        if (financialDetails != null && !financialDetails.isEmpty()) {
            for (int i = 0; i < financialDetails.size(); i++) {
                if (tobussines) {
                    financialDetails.get(i).setEmployee_business("0");
                } else {
                    financialDetails.get(i).setEmployee_business(null);
                }
            }
            EntityTool.setUpdateStatus(financialDetails);
            MetaDaoHelper.update(BankReconciliationDetail.ENTITY_NAME, financialDetails);
        }
    }


    /**
     * 【分配业务人员】 回写分配信息子表
     *
     * @param bankReconciliation
     * @param userids
     */
    private void setDispatchDetail(BankReconciliation bankReconciliation, String[] userids, boolean isAuto) throws Exception {
        if (userids == null || userids.length == 0) {
            return;
        }
        List<String> userList = Arrays.asList(userids);
        List<String> unchanged_unreturn = new ArrayList<>();
        // 处理原分配信息中，保留 退回 或 非退回且存在于新人员中 的分配信息，其余删除
        // 查询子表--操作类型为手工分配业务人员和自动分配业务人员的数据
        List<BankReconciliationDetail> details = bankReconciliation.details();
        List<BankReconciliationDetail> bankReconciliationDetails = new ArrayList<>();
        if (details != null && details.size() > 0) {
            for (BankReconciliationDetail detail : details) {
                if (OprType.AutoBusiness.getValue().equals(detail.getOprtype()) || OprType.ManualBusiness.getValue().equals(detail.getOprtype())) {
                    bankReconciliationDetails.add(detail);
                }
            }
        }
        List<BankReconciliationDetail> deleteList = new ArrayList<>();
        if (bankReconciliationDetails != null && bankReconciliationDetails.size() > 0) {
            for (BankReconciliationDetail detail : bankReconciliationDetails) {
                if ((!StringUtils.isEmpty(detail.getReturn_reason()) && null != detail.getReturndate()) || (!ObjectUtils.isEmpty(detail.getOprtype()) && detail.getOprtype().equals(OprType.Publish.getValue()))
                        || (!ObjectUtils.isEmpty(detail.getOprtype()) && detail.getOprtype().equals(OprType.Claim.getValue()))) {
                    continue;
                } else {
                    // 未退回
                    if (userList.contains(detail.getAutheduser())) {
                        // 存在于新分配的业务人员中,保留不更新,将其标记为unchanged_unreturn
                        unchanged_unreturn.add(detail.getAutheduser());
                    } else {
                        // 不存在于新分配的业务人员中，删除
                        deleteList.add(detail);
                    }
                }
//                if (StringUtils.isEmpty(detail.getReturn_reason()) && null == detail.getReturndate()) {
//                    // 未退回
//                    if (userList.contains(detail.getAutheduser())) {
//                        // 存在于新分配的业务人员中,保留不更新,将其标记为unchanged_unreturn
//                        unchanged_unreturn.add(detail.getAutheduser());
//                    } else {
//                        // 不存在于新分配的业务人员中，删除
//                        deleteList.add(detail);
//                    }
//                }
            }
        }
        if (deleteList.size() > 0) {
            MetaDaoHelper.delete(BankReconciliationDetail.ENTITY_NAME, deleteList);
        }
        // 插入新的分配信息
        List<BankReconciliationDetail> insertList = new ArrayList<>();
        IStaffService staffService = BdRestSingleton.getInst(AppContext.getYTenantId(), "diwork",
                AppContext.getCurrentUser().getYhtUserId()).getBdRestService().getStaffService();
        for (String userid : userids) {
            if (unchanged_unreturn.contains(userid)) {
                // 未退回且存在于新的分配信息中的无需插入
                continue;
            }
            BankReconciliationDetail detail = new BankReconciliationDetail();
            // id
            detail.setId(ymsOidGenerator.nextId());
            // mainid
            detail.setMainid(bankReconciliation.getId());
            // 操作日期
            detail.setOprdate(new Date());
            // 操作人
            detail.setOperator(isAuto ? 0L : AppContext.getCurrentUser().getId());
            // 操作类型
            detail.setOprtype(isAuto ? OprType.AutoBusiness.getValue() : OprType.ManualBusiness.getValue());
            // 对接人
            detail.setAutheduser(userid);

            Condition condition = new Condition();
            List<ConditionVO> conditionVOList = new ArrayList<>(1);
            ConditionVO conditionVO = new ConditionVO("user_id", userid, Operator.EQUAL);
            conditionVOList.add(conditionVO);
            condition.setConditionList(conditionVOList);
            Page<Staff> pageList = staffService.pagination(condition, new Sorter(), 1, 1);
            // 对应员工
            if (null != pageList && null != pageList.getContent() && pageList.getContent().size() > 0 && null != pageList.getContent().get(0)) {
                detail.setEmployee_business(pageList.getContent().get(0).getId());
            }
            detail.setEmployee_financial("0");//分配业务人员子表的财务人员数据为假数据，若为空会导致所有财务人员都可以看到
            // 退回日期
            detail.setReturndate(null);
            // 退回原因
            detail.setReturn_reason(null);
            insertList.add(detail);
        }
        if (insertList.size() > 0) {
            CmpMetaDaoHelper.insert(BankReconciliationDetail.ENTITY_NAME, insertList);
        }
    }

    /**
     * 【分配业务人员】 回写银行对账单
     *
     * @param bankReconciliation
     * @param isAuto
     */
    private void setBankReconciliation(BankReconciliation bankReconciliation, boolean isAuto) {
        // 发布状态 -> 否
//        bankReconciliation.setIspublish(false);
        // 发布分派状态 -> 是
        bankReconciliation.setPublishdistributestatus(isAuto ? PublishDistributeStatus.Auto.getValue() : PublishDistributeStatus.Manual.getValue());
        // 认领金额
//        bankReconciliation.setClaimamount(bankReconciliation.getTran_amt());
        // 待认领金额
//        bankReconciliation.setAmounttobeclaimed(BigDecimal.ZERO);
        // 认领状态
//        bankReconciliation.setBillclaimstatus(BillClaimStatus.Claimed.getValue());
    }

    /**
     * 【分配业务人员】 前校验
     *
     * @param bankReconciliation
     * @param bankSeqNo
     */
    private void checkBeforeDispatchBussiness(BankReconciliation bankReconciliation, String bankSeqNo) {
        // 无需处理（即回单处理标识=“无需回单中台处理、需人工确认”
        Short billprocessflag = bankReconciliation.getBillprocessflag();
        if (billprocessflag != null && (BillProcessFlag.NoNeedDeal.getValue() == billprocessflag || BillProcessFlag.ArtificialDeal.getValue() == billprocessflag)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101964"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180036", "分配业务人员失败：银行交易流水号：") /* "分配业务人员失败：银行交易流水号：" */ + (bankSeqNo == null ? "[]" : bankSeqNo) + BillProcessFlag.find(billprocessflag).getName() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180055", "，不能分配业务人员！") /* "，不能分配业务人员！" */);
        }

        Short associationstatus = bankReconciliation.getAssociationstatus();
        // 已关联的不允许分配业务人员
        if (associationstatus != null && associationstatus == AssociationStatus.Associated.getValue()) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101964"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180036", "分配业务人员失败：银行交易流水号：") /* "分配业务人员失败：银行交易流水号：" */ + (bankSeqNo == null ? "[]" : bankSeqNo) + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180057", "已关联，不能分配业务人员！") /* "已关联，不能分配业务人员！" */);
        }

        // 未分配财务人员的不能分配业务人员
        Short distributestatus = bankReconciliation.getDistributestatus();
        if (distributestatus != null && DispatchFinanceStatus.Not.getValue() == distributestatus) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101964"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180036", "分配业务人员失败：银行交易流水号：") /* "分配业务人员失败：银行交易流水号：" */ + (bankSeqNo == null ? "[]" : bankSeqNo) + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180037", "未分配给财务人员，不能分配业务人员！") /* "未分配给财务人员，不能分配业务人员！" */);
        }

        Short frozenstatus = bankReconciliation.getFrozenstatus();
        // 在解冻的不允许分配业务人员
        if (frozenstatus != null && FrozenStatus.Unfreezing.getValue() == frozenstatus) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101964"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180036", "分配业务人员失败：银行交易流水号：") /* "分配业务人员失败：银行交易流水号：" */ + (bankSeqNo == null ? "[]" : bankSeqNo) + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418003D", "在解冻处理流程中，不能分配业务人员！") /* "在解冻处理流程中，不能分配业务人员！" */);
        }
    }

    /**
     * 批量分配业务人员
     *
     * @param ids     银行对账单ID
     * @param userids 对接人
     * @param isAuto  true自动分配业务人员(自动分配业务对接人) false手工分配业务人员(通过分配业务人员按钮)
     * @return
     */
    @Override
    public CtmJSONObject dispatchBatchBussiness(List<String> ids, String[] userids, boolean isAuto) {
        CtmJSONObject result = new CtmJSONObject();
        if (ids == null || ids.size() == 0) {
            return result;
        }
        result.put("dealSucceed", false);
        int faildCount = 0;
        int successCount = 0;
        int totalCount = ids.size();
        StringBuffer errorSB = new StringBuffer();
        List<BankReconciliation> bankReconciliationList = new ArrayList<>();
        for (String id : ids) {
            BankReconciliation bankReconciliation = null;
            String bankSeqNo = null;
            try {
                bankReconciliation = MetaDaoHelper.findById(BankReconciliation.ENTITY_NAME, id, 3);
                if (bankReconciliation == null) {
                    faildCount++;
                    errorSB.append(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B0014B", "分配财务人员失败：未查询到银行对账单，请刷新后重试！\n") /* "分配财务人员失败：未查询到银行对账单，请刷新后重试！\n" */);
                    continue;
                }
                String errMessage = BankreconciliationUtils.checkDataLegal(bankReconciliation, BankreconciliationActionEnum.DISPATCHBATCHBUSSINESS);
                if (Strings.isNotEmpty(errMessage)) {
                    faildCount++;
                    errorSB.append(errMessage);
                    continue;
                }
                bankSeqNo = bankReconciliation.getBank_seq_no();
                checkBeforeDispatchBussiness(bankReconciliation, bankSeqNo);
                setBankReconciliation(bankReconciliation, isAuto);
                setDispatchDetail(bankReconciliation, userids, isAuto);
                EntityTool.setUpdateStatus(bankReconciliation);
                // 分配功能，本期先不做修改
                CommonSaveUtils.updateBankReconciliation(bankReconciliation);
                ctmcmpBusinessLogService.saveBusinessLog(bankReconciliation, bankReconciliation.getBank_seq_no(), "", IServicecodeConstant.CMPBANKRECONCILIATION, IServicecodeConstant.CMPBANKRECONCILIATION, IMsgConstant.CMDPUBLISHDISPATCH);
                bankReconciliationList.add(bankReconciliation);
                successCount++;
            } catch (Exception e) {
                faildCount++;
                errorSB.append(e.getMessage());
                errorSB.append("\n");
            }
        }
        result.put(ICmpConstant.MSG, ResultMessage.success());
        result.put("dealSucceed", true);
        result.put("faildCount", faildCount);
        result.put("errorMsg", errorSB);
        result.put("successCount", successCount);
        result.put("totalCount", totalCount);
        result.put("data", bankReconciliationList);
        return result;
    }

    /**
     * 【取消分配】
     *
     * @param id 银行对账单ID
     * @return
     */
    @Override
    public CtmJSONObject cancelDispatch(String id, String bankSeqNo) {
        String key = ICmpConstant.CMPBANKRECONCILIATIONLIST + id;
        YmsLock ymsLock = JedisLockUtils.lockBillWithOutTrace(key);
        if (ymsLock == null) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101190"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00143", "该数据正在处理，请稍后重试！") /* "该数据正在处理，请稍后重试！" */);
        }
        CtmJSONObject result = new CtmJSONObject();
        result.put("dealSucceed", false);
        try {
            BankReconciliation bankReconciliation = MetaDaoHelper.findById(BankReconciliation.ENTITY_NAME, id, 3);
            if (null == bankReconciliation) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101958"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00142", "银行交易流水号：") /* "银行交易流水号：" */ + (bankSeqNo == null ? "[]" : bankSeqNo) + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B0014F", ",不存在！请刷新后重试。") /* ",不存在！请刷新后重试。" */);
            }
            checkBeforeCancelDispatch(bankReconciliation, bankSeqNo);
            deleteDispatchDetail(bankReconciliation);
            EntityTool.setUpdateStatus(bankReconciliation);
            // 分配功能，本期先不做修改
            CommonSaveUtils.updateBankReconciliation(bankReconciliation);
            result.put(ICmpConstant.MSG, ResultMessage.success());
            result.put("dealSucceed", true);
            result.put("data", bankReconciliation);
        } catch (Exception e) {
            result.put(ICmpConstant.MSG, e.getMessage());
        } finally {
            JedisLockUtils.unlockBillWithOutTrace(ymsLock);
        }
        return result;
    }

    /**
     * 【取消分配】 删除分配信息
     *
     * @param bankReconciliation
     */
    private void deleteDispatchDetail(BankReconciliation bankReconciliation) throws Exception {
        List<BankReconciliationDetail> deleteDetails = new ArrayList<>();
        List<BankReconciliationDetail> bankReconciliationDetails = new ArrayList<>();
        List<BankReconciliationDetail> details = bankReconciliation.details();
        if (details == null || details.size() == 0) {
            return;
        }
        boolean returnBuss = false;
        if (PublishDistributeStatus.Not.getValue() == bankReconciliation.getPublishdistributestatus()) {
            // 未分配业务人员 清空操作类型为“自动分配财务人员、手工分配财务人员”的记录（除退回记录）
            bankReconciliationDetails = details;
            // 未分配业务人员，即取消的分配财务人员，则取消分配后分配财务人员为未分配财务人员、分配业务人员也为未分配业务人员
            bankReconciliation.setDistributestatus((short) 0);
            bankReconciliation.setPublishdistributestatus(PublishDistributeStatus.Not.getValue());
        } else {
            // 已分配业务人员 清空操作类型为“自动分配业务人员、手工分配业务人员”的记录（除退回记录）
            for (BankReconciliationDetail detail : details) {
                if (OprType.AutoBusiness.getValue().equals(detail.getOprtype()) || OprType.ManualBusiness.getValue().equals(detail.getOprtype())) {
                    bankReconciliationDetails.add(detail);
                }
            }
            // 已分配业务人员,则取消分配后分配业务人员状态为未分配业务人员
            bankReconciliation.setPublishdistributestatus(PublishDistributeStatus.Not.getValue());
            returnBuss = true;
        }

        if (bankReconciliationDetails == null || bankReconciliationDetails.size() == 0) {
            return;
        }

        // 删除分配信息(除退回记录)
        for (BankReconciliationDetail detail : bankReconciliationDetails) {
            if ((!StringUtils.isEmpty(detail.getReturn_reason()) && null != detail.getReturndate()) || (!ObjectUtils.isEmpty(detail.getOprtype()) && detail.getOprtype().equals(OprType.Publish.getValue()))
                    || (!ObjectUtils.isEmpty(detail.getOprtype()) && detail.getOprtype().equals(OprType.Claim.getValue()))) {
                continue;
            }
            deleteDetails.add(detail);
        }
        if (deleteDetails.size() > 0) {
            MetaDaoHelper.delete(BankReconciliationDetail.ENTITY_NAME, deleteDetails);
        }
        if (returnBuss) {
            updateDispatchFinancialData(bankReconciliation, false);
        }
    }

    /**
     * 【取消分配】 取消前校验
     *
     * @param bankReconciliation
     */
    private void checkBeforeCancelDispatch(BankReconciliation bankReconciliation, String bankSeqNo) throws Exception {
        // 当前登录用户是否属于财务人员（操作类型为自动分配财务人员或手工分配财务人员），如果属于财务人员，当分配业务人员状态为未分配业务人员或空时，不允许取消分配
        String roleType = ICmpConstant.BUSINESS_POST;
        List<TenantRole> roles = tenantRoleUserService.findRolesByUserId(AppContext.getCurrentUser().getYhtUserId(), AppContext.getYTenantId().toString(), "diwork");
        String roleCodeLine = "";
        if (null != roles) {
            for (TenantRole role : roles) {
                roleCodeLine += role.getRoleCode() + "----";
            }
        }
        if (roleCodeLine.contains(ICmpConstant.FINANCIAL_POST)) {
            roleType = ICmpConstant.FINANCIAL_POST;
        }
        if (roleCodeLine.contains(ICmpConstant.SYNTHESIZE_POST)) {
            roleType = ICmpConstant.SYNTHESIZE_POST;
        }

        if (roleType.equals(ICmpConstant.FINANCIAL_POST)) {
            String yhtUserId = AppContext.getCurrentUser().getYhtUserId();
            List<BankReconciliationDetail> details = bankReconciliation.details();
            if (details != null && details.size() > 0) {
                for (BankReconciliationDetail detail : details) {
                    if (yhtUserId.equals(detail.getAutheduser()) && (OprType.AutoFinance.getValue().equals(detail.getOprtype()) || OprType.ManualFinance.getValue().equals(detail.getOprtype())) && detail.getReturndate() == null) {
                        if (bankReconciliation.getPublishdistributestatus() == null || PublishDistributeStatus.Not.getValue() == bankReconciliation.getPublishdistributestatus()) {
                            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101958"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00142", "银行交易流水号：") /* "银行交易流水号：" */ + (bankSeqNo == null ? "[]" : bankSeqNo) + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00150", "未分配业务人员，不能取消分配！") /* "未分配业务人员，不能取消分配！" */);
                        }
                    }
                }
            }
        }

        // 在解冻的不允许取消分配
        Short frozenstatus = bankReconciliation.getFrozenstatus();
        if (frozenstatus != null && FrozenStatus.Unfreezing.getValue() == frozenstatus) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101958"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00142", "银行交易流水号：") /* "银行交易流水号：" */ + (bankSeqNo == null ? "[]" : bankSeqNo) + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00147", "在解冻处理流程中，不能取消分配！") /* "在解冻处理流程中，不能取消分配！" */);
        }

        // 是否已分配
        Short distributestatus = bankReconciliation.getDistributestatus();
        Short publishdistributestatus = bankReconciliation.getPublishdistributestatus();
        if ((distributestatus == null && publishdistributestatus == null) || (DispatchFinanceStatus.Not.getValue() == distributestatus && PublishDistributeStatus.Not.getValue() == publishdistributestatus)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101958"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00142", "银行交易流水号：") /* "银行交易流水号：" */ + (bankSeqNo == null ? "[]" : bankSeqNo) + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B0014A", "未分配，不能取消分配！") /* "未分配，不能取消分配！" */);
        }

        // 是否已关联、已生单
        Short associationstatus = bankReconciliation.getAssociationstatus();
        if (AssociationStatus.Associated.getValue() == associationstatus) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101958"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00142", "银行交易流水号：") /* "银行交易流水号：" */ + (bankSeqNo == null ? "[]" : bankSeqNo) + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B0014D", "已关联，不能取消分配！") /* "已关联，不能取消分配！" */);
        }
    }

    /**
     * 银行对账单分配财务人员
     *
     * @param bankReconciliationList 银行对账单列表
     * @param ids                    财务对接人列表
     * @return
     * @throws Exception
     */
    @Override
    public CtmJSONObject batchDispatch(List<BankReconciliation> bankReconciliationList, String[] ids) throws Exception {
        CtmJSONObject result = new CtmJSONObject();
        List<String> messages = new ArrayList<>();

        if (bankReconciliationList == null || bankReconciliationList.size() < 1) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101965"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418004C", "银行对账单不能为空!") /* "银行对账单不能为空!" */);
        }
        if (ids == null || ids.length < 1 || ValueUtils.isEmpty(ids[0])) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101966"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418004D", "分配财务对接人不能为空！") /* "分配财务对接人不能为空！" */);
        }
        // 对接人列表转list
        List<String> userIds = new ArrayList<>();
        Collections.addAll(userIds, ids);

        int i = 0;
        CtmJSONObject failed = new CtmJSONObject();
        for (BankReconciliation bankReconciliation : bankReconciliationList) {
            // 加锁
            YmsLock ymsLock = JedisLockUtils.lockBillWithOutTrace(bankReconciliation.getId().toString());
            if (null == ymsLock) {
                failed.put(bankReconciliation.getId().toString(), bankReconciliation.getId().toString());
                messages.add(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418003B", "该数据正在处理，请稍后重试！") /* "该数据正在处理，请稍后重试！" */ + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180035", "银行交易流水号：") /* "银行交易流水号：" */ + (bankReconciliation.getBank_seq_no() == null ? "[]" : bankReconciliation.getBank_seq_no()));
                i++;
                continue;
            }
            String errMessage = BankreconciliationUtils.checkDataLegal(bankReconciliation, BankreconciliationActionEnum.DISPATCHBATCHFINANCE);
            if (Strings.isNotEmpty(errMessage)) {
                failed.put(bankReconciliation.getId().toString(), bankReconciliation.getId().toString());
                messages.add(errMessage);
                i++;
                continue;
            }
            // 校验关联状态
            // 回单处理标识=“无需回单中台处理”，则不允许进行分配财务人员操作
            if (bankReconciliation.getBillprocessflag() != null && BillProcessFlag.NoNeedDeal.getValue() == bankReconciliation.getBillprocessflag()) {
                failed.put(bankReconciliation.getId().toString(), bankReconciliation.getId().toString());
                messages.add(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180039", "分配财务人员失败，") /* "分配财务人员失败，" */ + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180035", "银行交易流水号：") /* "银行交易流水号：" */ + (bankReconciliation.getBank_seq_no() == null ? "[]" : bankReconciliation.getBank_seq_no()) + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180052", "无需回单中台处理，不能分配财务人员！") /* "无需回单中台处理，不能分配财务人员！" */);
                i++;
                continue;
            }
            if (null != bankReconciliation.getAssociationstatus() && bankReconciliation.getAssociationstatus() != 0) {
                failed.put(bankReconciliation.getId().toString(), bankReconciliation.getId().toString());
                messages.add(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180039", "分配财务人员失败，") /* "分配财务人员失败，" */ + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180035", "银行交易流水号：") /* "银行交易流水号：" */ + (bankReconciliation.getBank_seq_no() == null ? "[]" : bankReconciliation.getBank_seq_no()) + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180054", "已关联，不能分配财务人员！") /* "已关联，不能分配财务人员！" */);
                i++;
                continue;
            }
            // 校验是否关联业务人员 自动或手动
            if (bankReconciliation.getPublishdistributestatus() == 1 || bankReconciliation.getPublishdistributestatus() == 2) {
                failed.put(bankReconciliation.getId().toString(), bankReconciliation.getId().toString());
                messages.add(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180039", "分配财务人员失败，") /* "分配财务人员失败，" */ + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180035", "银行交易流水号：") /* "银行交易流水号：" */ + (bankReconciliation.getBank_seq_no() == null ? "[]" : bankReconciliation.getBank_seq_no()) + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180056", "已分配业务人员，不能分配财务人员！") /* "已分配业务人员，不能分配财务人员！" */);
                i++;
                continue;
            }
            // 0正常 1在解冻 2已冻结
            if (null != bankReconciliation.getFrozenstatus() && bankReconciliation.getFrozenstatus() == 1) {
                failed.put(bankReconciliation.getId().toString(), bankReconciliation.getId().toString());
                messages.add(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180039", "分配财务人员失败，") /* "分配财务人员失败，" */ + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180035", "银行交易流水号：") /* "银行交易流水号：" */ + (bankReconciliation.getBank_seq_no() == null ? "[]" : bankReconciliation.getBank_seq_no()) + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418003A", ",在解冻处理流程中，不能分配财务人员！") /* ",在解冻处理流程中，不能分配财务人员！" */);
                i++;
                continue;
            }
            /**
             * 1，根据银行对账单查询原分配信息
             * 2，已退回的分配记录保留，重新插入一条分配记录
             * 3，未退回，且对接人在新对接人列表中，保留数据不更新
             * 4，未退回，且对接人不在新对接人列表中，删除数据
             * 5，根据新对接人列表 插入新的分配记录
             * 6，回写银行对账单分派信息
             */
            commonDeal(bankReconciliation, ids, Boolean.FALSE);
            //6，回写银行对账单分派信息-手工分配财务人员
            bankReconciliation.setDistributestatus(DispatchFinanceStatus.Manual.getValue());
            //7，组别辨识状态-手工辨识
            bankReconciliation.setGroupidfstatus((short) 2);
            EntityTool.setUpdateStatus(bankReconciliation);
            // 分配功能，本期先不做修改
            CommonSaveUtils.updateBankReconciliation(bankReconciliation);
            // 释放锁
            JedisLockUtils.unlockBillWithOutTrace(ymsLock);
        }
        String message = String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00144", "共操作单据[%s]个，成功[%s]个，失败[%s]个") /* "共操作单据[%s]个，成功[%s]个，失败[%s]个" */, bankReconciliationList.size(), bankReconciliationList.size() - i, i);
        result.put("msg", message);
        result.put("msgs", messages);
        result.put("messages", messages);
        result.put("count", bankReconciliationList.size());
        result.put("sucessCount", bankReconciliationList.size() - i);
        result.put("failCount", i);
        if (failed.size() > 0) {
            result.put("failed", failed);
        }
        return result;
    }

    /**
     * 单条银行对账单分派--自动任务使用
     *
     * @param bankReconciliation 银行对账单
     * @param ids                财务对接人id
     */
    @Override
    public CtmJSONObject dispatchOne(BankReconciliation bankReconciliation, String[] ids) throws Exception {
        CtmJSONObject result = new CtmJSONObject();
        // 加锁
        YmsLock ymsLock = JedisLockUtils.lockBillWithOutTrace(bankReconciliation.getId().toString());
        if (null == ymsLock) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101672"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418003B", "该数据正在处理，请稍后重试！") /* "该数据正在处理，请稍后重试！" */);
        }
        if (bankReconciliation == null) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101967"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B0014E", "银行对账单不能为空!") /* "银行对账单不能为空!" */);
        }
        if (ids == null || ids.length < 1 || ValueUtils.isEmpty(ids[0])) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101968"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00152", "分配财务对接人不能为空！") /* "分配财务对接人不能为空！" */);
        }
        // 对接人列表转list
        List<String> userIds = new ArrayList<>();
        Collections.addAll(userIds, ids);

        if (null != bankReconciliation.getAssociationstatus() && bankReconciliation.getAssociationstatus() != 0) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101958"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00142", "银行交易流水号：") /* "银行交易流水号：" */ + (bankReconciliation.getBank_seq_no() == null ? "[]" : bankReconciliation.getBank_seq_no()) + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00145", "已关联，不能分配财务人员！") /* "已关联，不能分配财务人员！" */);
        }
        // 回单处理标识=“无需回单中台处理”，则不允许进行分配财务人员操作
        if (bankReconciliation.getBillprocessflag() != null && BillProcessFlag.NoNeedDeal.getValue() == bankReconciliation.getBillprocessflag()) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101958"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00142", "银行交易流水号：") /* "银行交易流水号：" */ + (bankReconciliation.getBank_seq_no() == null ? "[]" : bankReconciliation.getBank_seq_no()) + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00148", "无需回单中台处理，不能分配财务人员！") /* "无需回单中台处理，不能分配财务人员！" */);
        }
        // 校验是否关联业务人员 自动或手动
        if (bankReconciliation.getPublishdistributestatus() == 1 || bankReconciliation.getPublishdistributestatus() == 2) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101958"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00142", "银行交易流水号：") /* "银行交易流水号：" */ + (bankReconciliation.getBank_seq_no() == null ? "[]" : bankReconciliation.getBank_seq_no()) + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00149", "已分配业务人员，不能分配财务人员！") /* "已分配业务人员，不能分配财务人员！" */);
        }
        // 0正常 1在解冻 2已冻结
        if (null != bankReconciliation.getFrozenstatus() && bankReconciliation.getFrozenstatus() == 1) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101958"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00142", "银行交易流水号：") /* "银行交易流水号：" */ + (bankReconciliation.getBank_seq_no() == null ? "[]" : bankReconciliation.getBank_seq_no()) + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B0014C", ",在解冻处理流程中，不能分配财务人员！") /* ",在解冻处理流程中，不能分配财务人员！" */);
        }
        commonDeal(bankReconciliation, ids, Boolean.TRUE);
        //6，回写银行对账单分派信息-自动分配财务人员
        bankReconciliation.setDistributestatus(DispatchFinanceStatus.Auto.getValue());
        //7，组别辨识状态-自动辨识
        bankReconciliation.setGroupidfstatus((short) 1);
        EntityTool.setUpdateStatus(bankReconciliation);
        // 分配功能，本期先不做修改
        CommonSaveUtils.updateBankReconciliation(bankReconciliation);

        // 释放锁
        JedisLockUtils.unlockBillWithOutTrace(ymsLock);
        result.put("msg", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00151", "分配财务人员成功!") /* "分配财务人员成功!" */);
        result.put("resule", Boolean.TRUE);
        return result;
    }

    /**
     * 分配财务人员公共逻辑
     *
     * @param bankReconciliation 银行对账单
     * @param ids                前端勾选财务对接人id
     */
    private void commonDeal(BankReconciliation bankReconciliation, String[] ids, Boolean isAuto) throws Exception {
        /**
         * 1，根据银行对账单查询原分配信息
         * 2，已退回的分配记录保留，如果新列表中仍然存在该对接人，重新插入一条分配记录
         * 3，未退回，且对接人不在新对接人列表中，删除数据
         * 4，未退回，且对接人在新对接人列表中，保留数据不更新
         * 5，根据新对接人列表 插入新的分配记录
         * 6，回写银行对账单分派信息
         */
        // 对接人列表转list
        List<String> userIds = new ArrayList<>();
        Collections.addAll(userIds, ids);
        // 1,根据银行对账单查询对应分配信息
        QuerySchema queryIsExist = QuerySchema.create().addSelect("*");
        QueryConditionGroup conditionGroup = new QueryConditionGroup();
        conditionGroup.addCondition(QueryCondition.name("mainid").eq(bankReconciliation.getId()));
        queryIsExist.addCondition(conditionGroup);
        List<BankReconciliationDetail> bankReconciliationDetails = MetaDaoHelper.queryObject(BankReconciliationDetail.ENTITY_NAME, queryIsExist, null);
        // 如果分配信息为空，则为第一次分配，直接根据ids插入分配记录
        if (bankReconciliationDetails == null || bankReconciliationDetails.size() < 1) {
            //5，根据新对接人列表 插入新的分配记录
            insertBankReconciliationDetail(bankReconciliation, Arrays.asList(ids), isAuto);
        } else {
            // 循环已存在分配信息，判断是否已退回，对接人是否存在新分配人列表
            // 需要删除的分配信息
            List<BankReconciliationDetail> delList = new ArrayList<>();
            for (BankReconciliationDetail bankReconciliationDetail : bankReconciliationDetails) {
                // 退回日期或退回原因都为空未被退回数据
                if ((!StringUtils.isEmpty(bankReconciliationDetail.getReturn_reason()) && null != bankReconciliationDetail.getReturndate()) || (!ObjectUtils.isEmpty(bankReconciliationDetail.getOprtype()) && bankReconciliationDetail.getOprtype().equals(OprType.Publish.getValue()))
                        || (!ObjectUtils.isEmpty(bankReconciliationDetail.getOprtype()) && bankReconciliationDetail.getOprtype().equals(OprType.Claim.getValue()))) {
                    continue;
                }
                if (bankReconciliationDetail.getReturndate() == null
                        && ValueUtils.isEmpty(bankReconciliationDetail.getReturn_reason())) {
                    if (!userIds.contains(bankReconciliationDetail.getAutheduser())) {
                        //分配信息未被退回且对接人不在新对接人数组中应当删除
                        delList.add(bankReconciliationDetail);
                    } else {
                        // 分配信息未退回且对接人在新对接人数组中，保留记录不更新
                        // 新对接人名单中删除当前对接人
                        userIds.remove(bankReconciliationDetail.getAutheduser());
                    }
                }
            }
            // 删除已有分配记录
            MetaDaoHelper.delete(BankReconciliationDetail.ENTITY_NAME, delList);
            // 插入新分配信息
            //5，根据新对接人列表 插入新的分配记录
            if (userIds != null && userIds.size() > 0) {
                insertBankReconciliationDetail(bankReconciliation, userIds, isAuto);
            }
        }

    }

    /**
     * 插入分配信息
     *
     * @param bankReconciliation 银行对账单
     * @param userIds            财务对接人id
     * @param isAuto             是否自动任务
     */
    private void insertBankReconciliationDetail(BankReconciliation bankReconciliation, List<String> userIds, Boolean isAuto) throws Exception {
        IStaffService staffService = BdRestSingleton.getInst(AppContext.getYTenantId(), "diwork",
                AppContext.getCurrentUser().getYhtUserId()).getBdRestService().getStaffService();
        for (int i = 0; i < userIds.size(); i++) {
            BankReconciliationDetail bankReconciliationDetail = new BankReconciliationDetail();
            bankReconciliationDetail.setMainid(bankReconciliation.getId());
            // 对接人
            bankReconciliationDetail.setAutheduser(userIds.get(i));

            Condition condition = new Condition();
            List<ConditionVO> conditionVOList = new ArrayList<>(1);
            ConditionVO conditionVO = new ConditionVO("user_id", userIds.get(i), Operator.EQUAL);
            conditionVOList.add(conditionVO);
            condition.setConditionList(conditionVOList);
            Page<Staff> pageList = staffService.pagination(condition, new Sorter(), 1, 1);
            // 对应员工
            if (null != pageList && null != pageList.getContent() && pageList.getContent().size() > 0 && null != pageList.getContent().get(0)) {
                bankReconciliationDetail.setEmployee_financial(pageList.getContent().get(0).getId());
            }

            // 操作日期
            bankReconciliationDetail.setOprdate(new Date());
            if (isAuto) {
                // 操作人-系统
                bankReconciliationDetail.setOperator(0L);
                // 操作类型-自动分配财务人员
                bankReconciliationDetail.setOprtype(OprType.AutoFinance.getValue());
            } else {
                // 操作人
                bankReconciliationDetail.setOperator(AppContext.getCurrentUser().getId());
                // 操作类型-手工分配财务人员
                bankReconciliationDetail.setOprtype(OprType.ManualFinance.getValue());
            }
            // 退回意见
            bankReconciliationDetail.setReturn_reason(null);
            // 退回日期
            bankReconciliationDetail.setReturndate(null);

            long id = ymsOidGenerator.nextId();
            bankReconciliationDetail.setId(id);
            bankReconciliationDetail.setEntityStatus(EntityStatus.Insert);
            CmpMetaDaoHelper.insert(BankReconciliationDetail.ENTITY_NAME, bankReconciliationDetail);
        }
    }

    /**
     * 手动回单关联
     *
     * @param id
     * @param bankelectronicreceiptid
     * @return
     * @throws Exception
     */
    @Override
    public CtmJSONObject receiptassociation(Long id, Long bankelectronicreceiptid) throws Exception {
        String key = ICmpConstant.CMPBANKRECONCILIATIONLIST + id;
        YmsLock ymsLock = JedisLockUtils.lockBillWithOutTrace(key);
        if (null == ymsLock) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101672"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418003B", "该数据正在处理，请稍后重试！") /* "该数据正在处理，请稍后重试！" */);
        }
        CtmJSONObject result = new CtmJSONObject();
        result.put("associationSucceed", false);
        BankReconciliation bankReconciliationById = null;
        String urlId = null;
        try {
            BankReconciliation bankReconciliation = MetaDaoHelper.findById(BankReconciliation.ENTITY_NAME, id);
            if (null == bankReconciliation) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101970"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("P_YS_CTM_CM-BE_1713352554276454502", "查询不到对应单据,请确认单据是否存在或刷新后重新操作!") /* "查询不到对应单据,请确认单据是否存在或刷新后重新操作!" */);
            }
            List<BizObject> bizObjects = new ArrayList<>();
            bizObjects.add(bankReconciliation);
            BankreconciliationUtils.checkDataLegalList(bizObjects, BankreconciliationActionEnum.RECEIPTASSOCIATION);
            //关联状态=‘未关联’ 0自动关联 1手工关联 4未关联
            if (null != bankReconciliation.getReceiptassociation() && (bankReconciliation.getReceiptassociation() == 0 || bankReconciliation.getReceiptassociation() == 1)) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101971"), String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18657A4804680012", "回单关联失败：银行对账单流水号[%s]，关联状态为已关联，不能关联银行交易回单！") /* "回单关联失败：银行对账单流水号[%s]，关联状态为已关联，不能关联银行交易回单！" */, bankReconciliation.getBank_seq_no()));
            }
            bankReconciliation.setReceiptassociation(ReceiptassociationStatus.ManualAssociated.getValue());
            bankReconciliation.setReceiptId(bankelectronicreceiptid.toString());
            BankElectronicReceipt bankElectronicReceipt = MetaDaoHelper.findById(BankElectronicReceipt.ENTITY_NAME, bankelectronicreceiptid);
            if (null == bankElectronicReceipt) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101973"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18657A480468000F", "查询不到对应银行交易回单,请确认单据是否存在或刷新后重新操作!") /* "查询不到对应银行交易回单,请确认单据是否存在或刷新后重新操作!" */);
            }
            //关联状态=‘未关联’ 0自动关联 1手工关联 4未关联
            if (null != bankElectronicReceipt.getAssociationstatus() && (bankElectronicReceipt.getAssociationstatus() == 0 || bankElectronicReceipt.getAssociationstatus() == 1)) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101974"), String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18657A4804680010", "回单关联失败：银行交易回单流水号[%s]，关联状态为已关联，不能关联银行交易回单！") /* "回单关联失败：银行交易回单流水号[%s]，关联状态为已关联，不能关联银行交易回单！" */, bankElectronicReceipt.getBankseqno()));
            }
            bankElectronicReceipt.setAssociationstatus(ReceiptassociationStatus.ManualAssociated.getValue());
            bankElectronicReceipt.setBankreconciliationid(String.valueOf(id));
            EntityTool.setUpdateStatus(bankElectronicReceipt);
            MetaDaoHelper.update(BankElectronicReceipt.ENTITY_NAME, bankElectronicReceipt);
            EntityTool.setUpdateStatus(bankReconciliation);
            List bankReconciliationList = new ArrayList<>();
            bankReconciliationList.add(bankReconciliation);
            // 待修改
            CommonSaveUtils.updateBankReconciliation4ReceiptassociationStatus(bankReconciliationList);
            result.put(ICmpConstant.MSG, ResultMessage.success());
            result.put("associationSucceed", true);
            bankReconciliationById = MetaDaoHelper.findById(BankReconciliation.ENTITY_NAME, bankReconciliation.getId());
            urlId = getBankReceiptFileId(bankReconciliationById);
            if (ObjectUtils.isNotEmpty(urlId)) {
                BankreconciliationService bankreconciliationService = CtmAppContext.getBean(BankreconciliationService.class);
                LogUtil.saveBankelereceiptSendFileEventlogByDto(bankReconciliationById, urlId, 18); // 修改这里

                bankreconciliationService.sendEventOfFileidInFinal(bankReconciliation, urlId);
            }
            //凭证关联银行电子回单功能；电子回单下载过文件且银行对账单和总账凭证已勾对，要发送关联事件
            if (bankElectronicReceipt.getIsdown() && bankReconciliation.getOther_checkflag()) {
                handleBankReceiptCorrEvent(bankElectronicReceipt, bankReconciliation);
            }
        } catch (Exception e) {
            result.put(ICmpConstant.MSG, e.getMessage());
        } finally {
            LogUtil.saveBankelereceiptSendFileEventlogByDto(bankReconciliationById, urlId, 19); // 修改这里
            this.sendEventOfFileidInFinal(bankReconciliationById, urlId);
            JedisLockUtils.unlockBillWithOutTrace(ymsLock);
        }
        return result;
    }

    @Override
    public CtmJSONObject cancelReceiptassociation(Long id) throws Exception {
        String key = ICmpConstant.CMPBANKRECONCILIATIONLIST + id;
        YmsLock ymsLock = JedisLockUtils.lockBillWithOutTrace(key);
        if (null == ymsLock) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101672"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418003B", "该数据正在处理，请稍后重试！") /* "该数据正在处理，请稍后重试！" */);
        }
        CtmJSONObject result = new CtmJSONObject();
        result.put("associationSucceed", false);
        try {
            BankReconciliation bankReconciliation = MetaDaoHelper.findById(BankReconciliation.ENTITY_NAME, id);
            List<BizObject> bizObjects = new ArrayList<>();
            bizObjects.add(bankReconciliation);
            BankreconciliationUtils.checkDataLegalList(bizObjects, BankreconciliationActionEnum.NORECEIPTASSOCIATION);
            if (null == bankReconciliation) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101975"), MessageUtils.getMessage("P_YS_CTM_CM-BE_1713352554276454502") /* "查询不到对应单据,请确认单据是否存在或刷新后重新操作!" */);
            }
            //关联状态=‘关联’
            if (null != bankReconciliation.getReceiptassociation() && bankReconciliation.getReceiptassociation() == 4) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101976"), String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18657A4804680011", "回单取消关联失败：银行对账单回单流水号[%s]，回单关联状态为未关联，不能取消关联回单！") /* "回单取消关联失败：银行对账单回单流水号[%s]，回单关联状态为未关联，不能取消关联回单！" */, bankReconciliation.getBank_seq_no()));
            }
            String url = getBankReceiptFileId(bankReconciliation);
            if (ObjectUtils.isNotEmpty(url)) {
                BankreconciliationService bankreconciliationService = CtmAppContext.getBean(BankreconciliationService.class);
                bankreconciliationService.cancelUrl(bankReconciliation.getId(), url);
            }
            Short associationStatus = 4;
            QuerySchema querySchema = QuerySchema.create().addSelect("*");
            QueryConditionGroup queryCondition = new QueryConditionGroup();
            queryCondition.addCondition(QueryConditionGroup.and(QueryCondition.name("bankreconciliationid").eq(String.valueOf(id))));
            querySchema.addCondition(queryCondition);
            List<BankElectronicReceipt> bankElectronicReceiptList = MetaDaoHelper.queryObject(BankElectronicReceipt.ENTITY_NAME, querySchema, null);
            if (null != bankElectronicReceiptList && bankElectronicReceiptList.size() > 0) {
                //正常一个流水只会关联一个回单，之前会有关联多个回单的历史数据，所以这里遍历取消下
                for (BankElectronicReceipt bankElectronicReceipt : bankElectronicReceiptList) {
                    bankElectronicReceipt.setAssociationstatus(ReceiptassociationStatus.NoAssociated.getValue());
                    bankElectronicReceipt.setBankreconciliationid("");
                    EntityTool.setUpdateStatus(bankElectronicReceipt);
                    MetaDaoHelper.update(BankElectronicReceipt.ENTITY_NAME, bankElectronicReceipt);
                    //凭证关联银行电子回单功能；电子回单下载过文件且银行对账单和总账凭证已勾对，要发送取消关联事件
                    if (bankElectronicReceipt.getIsdown() && bankReconciliation.getOther_checkflag()) {
                        handleBankReceiptCancelEvent(bankElectronicReceipt, bankReconciliation);
                    }
                }
            }

            Map<String, Object> bankReconparam = new HashMap<>();
            bankReconparam.put("id", id);
            bankReconparam.put("concat_info_4", "");
            bankReconparam.put("ytenant_id", InvocationInfoProxy.getTenantid());
            SqlHelper.update("com.yonyoucloud.fi.cmp.bankreconciliation.rule.BankReconciliationMapper.updateBankReconciliationReceiptassociationNew", bankReconparam);

            result.put(ICmpConstant.MSG, ResultMessage.success());
            result.put("associationSucceed", true);
        } catch (Exception e) {
            result.put(ICmpConstant.MSG, e.getMessage());
        } finally {
            JedisLockUtils.unlockBillWithOutTrace(ymsLock);
        }
        return result;
    }

    /**
     * 处理电子回单和对账单取消关联后，给凭证发送和电子回单取消关联事件
     *
     * @param bankElectronicReceipt 电子回单
     * @param bankReconciliation    银行对账单
     */
    private void handleBankReceiptCancelEvent(BankElectronicReceipt bankElectronicReceipt, BankReconciliation bankReconciliation) throws Exception {
        QuerySchema querySchema = QuerySchema.create().addSelect("*");
        List<Short> receiptassociationList = new ArrayList<>();
        receiptassociationList.add(ReceiptassociationStatus.ManualAssociated.getValue());
        receiptassociationList.add(ReceiptassociationStatus.AutomaticAssociated.getValue());
        QueryConditionGroup group = QueryConditionGroup.and(
                //不为当前对账单
                QueryCondition.name("id").not_eq(bankReconciliation.getId()),
                //其他对方勾对的数据
                QueryCondition.name("other_checkno").eq(bankReconciliation.getOther_checkno()),
                //回单处理标识为已关联
                QueryCondition.name("receiptassociation").in(receiptassociationList)
        );
        querySchema.addCondition(group);
        List<BankReconciliation> list = MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME, querySchema, null);
        if (list.isEmpty()) {
            //消息队列 data
            List<BankReceiptToVoucherVO> bankReceiptToVoucherVOList = new ArrayList<>();
            //事件发送信息实体
            BankReceiptToVoucherVO bankReceiptToVoucherVO = new BankReceiptToVoucherVO();
            //取消关联
            bankReceiptToVoucherVO.setActionType("0");
            bankReceiptToVoucherVO.setCheckNo(bankReconciliation.getOther_checkno());
            List<BankReceiptInfoVO> bankReceiptInfoVOList = new ArrayList<>();
            BankReceiptInfoVO bankReceiptInfoVO = new BankReceiptInfoVO();
            bankReceiptInfoVO.setBankElectronicReceiptId(bankElectronicReceipt.getId().toString());
            bankReceiptInfoVO.setBankReconciliationId(bankReconciliation.getId().toString());
            bankReceiptInfoVO.setExtendss(bankElectronicReceipt.getExtendss());
            bankReceiptInfoVOList.add(bankReceiptInfoVO);
            bankReceiptToVoucherVO.setBankReceiptInfoVOList(bankReceiptInfoVOList);

            bankReceiptToVoucherVOList.add(bankReceiptToVoucherVO);

            //推送事件中心
            //事件中心，发送的数据包装类
            BizObject userObject = new BizObject();
            //数据类型，凭证回单关联
            userObject.put("datatype", "voucher");
            //对账单生单数据
            userObject.put("datalist", bankReceiptToVoucherVOList);
            //记录业务日志
            CtmJSONObject logparam = new CtmJSONObject();
            logparam.put("userObject", userObject);
            ctmcmpBusinessLogService.saveBusinessLog(logparam, bankReconciliation.getOther_checkno(), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400579", "银行流水和银行回单取消关联时") /* "银行流水和银行回单取消关联时" */, IServicecodeConstant.BANKRECONCILIATION, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540057B", "银企对账") /* "银企对账" */, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540057A", "发送银行回单和凭证关联事件") /* "发送银行回单和凭证关联事件" */);
            //发送消息到事件中心
            SendEventMessageUtils.sendEventMessageEos(userObject, IEventCenterConstant.CMP_BANKRECONCILIATION, IEventCenterConstant.CMP_BANKELECTRONICRECEIPTURL_GL);
        }
    }

    /**
     * 处理回单文件和凭证关联的消息发送
     *
     * @param bankElectronicReceipt
     * @param bankReconciliation
     * @throws Exception
     */
    public void handleBankReceiptCorrEvent(BankElectronicReceipt bankElectronicReceipt, BankReconciliation bankReconciliation) throws Exception {
        //消息队列 data
        List<BankReceiptToVoucherVO> bankReceiptToVoucherVOList = new ArrayList<>();
        //事件发送信息实体
        BankReceiptToVoucherVO bankReceiptToVoucherVO = new BankReceiptToVoucherVO();
        //发送关联信息
        bankReceiptToVoucherVO.setActionType("1");
        bankReceiptToVoucherVO.setCheckNo(bankReconciliation.getOther_checkno());
        List<BankReceiptInfoVO> bankReceiptInfoVOList = new ArrayList<>();
        BankReceiptInfoVO bankReceiptInfoVO = new BankReceiptInfoVO();
        bankReceiptInfoVO.setBankElectronicReceiptId(bankElectronicReceipt.getId().toString());
        bankReceiptInfoVO.setBankReconciliationId(bankReconciliation.getId().toString());
        bankReceiptInfoVO.setExtendss(bankElectronicReceipt.getExtendss());
        bankReceiptInfoVOList.add(bankReceiptInfoVO);
        bankReceiptToVoucherVO.setBankReceiptInfoVOList(bankReceiptInfoVOList);

        bankReceiptToVoucherVOList.add(bankReceiptToVoucherVO);

        //推送事件中心
        //事件中心，发送的数据包装类
        BizObject userObject = new BizObject();
        //数据类型，凭证回单关联
        userObject.put("datatype", "voucher");
        //对账单生单数据
        userObject.put("datalist", bankReceiptToVoucherVOList);
        //记录业务日志
        CtmJSONObject logparam = new CtmJSONObject();
        logparam.put("userObject", userObject);
        ctmcmpBusinessLogService.saveBusinessLog(logparam, bankReconciliation.getOther_checkno(), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400582", "银行流水关联银行回单时") /* "银行流水关联银行回单时" */, IServicecodeConstant.BANKRECONCILIATION, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540057B", "银企对账") /* "银企对账" */, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540057A", "发送银行回单和凭证关联事件") /* "发送银行回单和凭证关联事件" */);
        //发送消息到事件中心
        SendEventMessageUtils.sendEventMessageEos(userObject, IEventCenterConstant.CMP_BANKRECONCILIATION, IEventCenterConstant.CMP_BANKELECTRONICRECEIPTURL_GL);
    }

    @Override
    public void batchUpdateBankReconciliation(Map<String, Object> params) throws Exception {
        try {
            boolean repeatCheckFlag = false;
            for (String key : params.keySet()) {
                if (CtmDealDetailCheckMayRepeatUtils.repeatFactors.contains(key)) {
                    repeatCheckFlag = true;
                    break;
                }
            }
            Short oppositetype = ValueUtils.isNotEmptyObj(params.get("oppositetype")) ? Short.parseShort(params.get("oppositetype").toString()) : null;
            String to_acct_no = ValueUtils.isNotEmptyObj(params.get("to_acct_no")) ? params.get("to_acct_no").toString() : null;
            String to_acct_name = ValueUtils.isNotEmptyObj(params.get("to_acct_name")) ? params.get("to_acct_name").toString() : null;
            String to_acct_bank = ValueUtils.isNotEmptyObj(params.get("to_acct_bank")) ? params.get("to_acct_bank").toString() : null;
            String to_acct_bank_name = ValueUtils.isNotEmptyObj(params.get("to_acct_bank_name")) ? params.get("to_acct_bank_name").toString() : null;
            String use_name = ValueUtils.isNotEmptyObj(params.get("use_name")) ? params.get("use_name").toString() : null;
            String remark = ValueUtils.isNotEmptyObj(params.get("remark")) ? params.get("remark").toString() : null;
            List<Long> ids = (ArrayList) params.get("ids");
            QuerySchema schema = QuerySchema.create().addSelect("*");
            QueryConditionGroup queryConditionGroup = new QueryConditionGroup(ConditionOperator.and);
            queryConditionGroup.appendCondition(QueryCondition.name("id").in(ids));
            schema.addCondition(queryConditionGroup);
            BankReconciliation paramBankReconciliation = new BankReconciliation();
            paramBankReconciliation.setOppositetype(oppositetype);
            paramBankReconciliation.setTo_acct_no(to_acct_no);
            List<BankReconciliation> bankReconciliationList = MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME, schema, null);
            if (bankReconciliationList != null && bankReconciliationList.size() > 0) {
                for (BankReconciliation bankReconciliation : bankReconciliationList) {
                    // 对方类型、对方账号任何一个修改清空对方账户Id
                    paramBankReconciliation.setOppositeobjectid(bankReconciliation.getOppositeobjectid());
                    // 对方类型不输入值
                    if (oppositetype == null) {
                        paramBankReconciliation.setOppositetype(bankReconciliation.getOppositetype());
                    }
                    // 对方账号不输入值
                    if (to_acct_no == null) {
                        paramBankReconciliation.setTo_acct_no(bankReconciliation.getTo_acct_no());
                    }
                    if (MerchantUtils.checkOppositeIsChanged(paramBankReconciliation, bankReconciliation)) {
                        bankReconciliation.setTo_acct(null);
                    }
                    if (oppositetype != null) {
                        bankReconciliation.setOppositetype(oppositetype);
                    }
                    if (to_acct_no != null) {
                        bankReconciliation.setTo_acct_no(to_acct_no);
                    }
                    if (to_acct_name != null) {
                        bankReconciliation.setTo_acct_name(to_acct_name);
                    }
                    if (to_acct_bank != null) {
                        bankReconciliation.setTo_acct_bank(to_acct_bank);
                    }
                    if (to_acct_bank_name != null) {
                        bankReconciliation.setTo_acct_bank_name(to_acct_bank_name);
                    }
                    if (use_name != null) {
                        bankReconciliation.setUse_name(use_name);
                    }
                    if (remark != null) {
                        bankReconciliation.setRemark(remark);
                    }
                    // 银行对账单保存、修改、导入时根据银行账户、交易日期查历史余额，如果已确认则不能增改
                    String bankaccount = bankReconciliation.getBankaccount();
                    Map<String, Object> bankAccountObject = QueryBaseDocUtils.queryEnterpriseBankAccountById(bankaccount);
                    bankReconciliation.setBanktype(bankAccountObject.get("bank").toString());
                    Date tran_date = bankReconciliation.getTran_date();
                    EnterpriseBankAcctVO enterpriseBankAcctVO = baseRefRpcService.queryEnterpriseBankAccountById(bankReconciliation.getBankaccount());
                    if (enterpriseBankAcctVO == null) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101977"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00243", "企业银行账户未启用，不能进行保存！") /* "企业银行账户未启用，不能进行保存！" */);
                    }
                    if (!StringUtils.isEmpty(bankaccount) && tran_date != null) {
                        String dateStr = new SimpleDateFormat("yyyy-MM-dd").format(tran_date);
                        QuerySchema querySchema = QuerySchema.create().addSelect(QuerySchema.PARTITION_ALL);
                        querySchema.appendQueryCondition(QueryCondition.name("enterpriseBankAccount").eq(bankaccount));
                        querySchema.appendQueryCondition(QueryCondition.name("balancedate").between(dateStr, null));
                        querySchema.appendQueryCondition(QueryCondition.name("isconfirm").eq(true));
                        querySchema.addOrderBy("balancedate");
                        List<Map<String, Object>> historyBalance = MetaDaoHelper.query(AccountRealtimeBalance.ENTITY_NAME, querySchema);
                        if (org.apache.commons.collections4.CollectionUtils.isNotEmpty(historyBalance)) {
                            if (Objects.equals(bankReconciliation.get("_fromApi"), true)) {
                                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101978"), String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181E1DC00440000C", "导入失败：银行账户[%s]交易日期[%s]的历史余额已经进行确认，当前对账单的交易日期等于或早于该日期，不能导入!") /* "导入失败：银行账户[%s]交易日期[%s]的历史余额已经进行确认，当前对账单的交易日期等于或早于该日期，不能导入!" */, enterpriseBankAcctVO.getAcctName(), historyBalance.get(historyBalance.size() - 1).get("balancedate")));
                            } else {
                                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101979"), String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181E1DC00440000D", "保存失败：银行账户[%s]交易日期[%s]的历史余额已经进行确认，当前对账单的交易日期等于或早于该日期，不能保存！") /* "保存失败：银行账户[%s]交易日期[%s]的历史余额已经进行确认，当前对账单的交易日期等于或早于该日期，不能保存！" */, enterpriseBankAcctVO.getAcctName(), historyBalance.get(historyBalance.size() - 1).get("balancedate")));
                            }
                        }
                    }
//                    EntityTool.setUpdateStatus(bankReconciliation);
                    if (repeatCheckFlag) {
                        Map<String, Object> enterpriseInfo = new HashMap<>();
                        enterpriseInfo.put("startDate", DateUtils.convertToStr(bankReconciliation.getTran_date(), "yyyy-MM-dd HH:mm:ss"));
                        ctmCmpCheckRepeatDataService.deal4FactorsBankDealDetail(Collections.singletonList(bankReconciliation), enterpriseInfo);
                    }
                    // 待修改
                    CommonSaveUtils.updateBankReconciliation(bankReconciliation);
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101980"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18FB486805080014", "银行对账单批量编辑异常:") /* "银行对账单批量编辑异常:" */ + e.getMessage());
        }
    }

    /**
     * 业务处理生单资金调度类生单在前端beforeBatchpush事件中发请求根据提前入账判断入账类型的值
     * //第一次提前入账只能生成收付款单，之后做第二次可以生成其他类型，提前入账肯定为是 然后赋值为冲挂账
     * //如果是正常生单，入账类型为正常入账
     *
     * @param params
     * @throws Exception
     */
    @Override
    public void dealVirtualEntryType(CtmJSONObject params) throws Exception {
        boolean isadvanceaccounts = (boolean) params.get("isadvanceaccounts");
        String ids = (String) params.get("ids");
        QuerySchema schema = QuerySchema.create().addSelect(" * ");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("id").in(ids.split(",")));
        schema.addCondition(conditionGroup);
        List<BankReconciliation> bankre = MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME, schema, null);
        if (isadvanceaccounts) {
            bankre.stream().forEach(e -> {
                e.setVirtualEntryType(EntryType.CrushHang_Entry.getValue());
                e.setIsadvanceaccounts(isadvanceaccounts);
                e.setEntityStatus(EntityStatus.Update);
            });
        } else {
            bankre.stream().forEach(e -> {
                e.setVirtualEntryType(EntryType.Normal_Entry.getValue());
                e.setIsadvanceaccounts(isadvanceaccounts);
                e.setEntityStatus(EntityStatus.Update);
            });
        }
        CommonSaveUtils.updateBankReconciliation(bankre, null);
    }

    /**
     * 银行对账单列表汇总信息
     *
     * @param param
     * @return
     * @throws Exception
     */
    @Override
    public CtmJSONObject queryBankSummaryInformation(CtmJSONObject param, HttpServletResponse response) throws Exception {
        CtmJSONObject jsonObject = new CtmJSONObject();
        BigDecimal debitamount = BigDecimal.ZERO;//借方金额
        BigDecimal creditamount = BigDecimal.ZERO;//贷方金额
        Integer debitanum = 0;
        Integer creditanum = 0;
        Integer moneydigit = 0;
        String formatStr = "0.00";
        LinkedHashMap<String, List<Object>> dataMap = new LinkedHashMap<String, List<Object>>();
        ArrayList commonVOs = new ArrayList();
        try {
            dataMap = (LinkedHashMap<String, List<Object>>) param.get("condition");
            commonVOs = (ArrayList) dataMap.get("commonVOs");
            QuerySchema querySchema = QuerySchema.create().addSelect(" id , dc_flag ,tran_amt ,currency");
            QueryConditionGroup group = new QueryConditionGroup();
            if (commonVOs != null) {
                //遍历查询条件
                for (Object commonVO : commonVOs) {
                    LinkedHashMap<String, String> listLinkedHashMap = (LinkedHashMap<String, String>) commonVO;
                    //资金组织
                    if (!"schemeName".equals(listLinkedHashMap.get("itemName")) && !"isDefault".equals(listLinkedHashMap.get("itemName"))) {
                        if ("tran_date".equals(listLinkedHashMap.get("itemName")) && listLinkedHashMap.get("value2") != null) {
                            group.addCondition(QueryCondition.name(listLinkedHashMap.get("itemName")).egt(listLinkedHashMap.get("value1")));
                            group.addCondition(QueryCondition.name(listLinkedHashMap.get("itemName")).elt(listLinkedHashMap.get("value2")));
                        } else if ("tran_amt".equals(listLinkedHashMap.get("itemName")) && listLinkedHashMap.get("value2") != null) {
                            group.addCondition(QueryCondition.name(listLinkedHashMap.get("itemName")).egt(listLinkedHashMap.get("value1")));
                            group.addCondition(QueryCondition.name(listLinkedHashMap.get("itemName")).elt(listLinkedHashMap.get("value2")));
                        } else {
                            group.addCondition(QueryCondition.name(listLinkedHashMap.get("itemName")).in(listLinkedHashMap.get("value1")));
                        }
                    }
                }
                // 权限控制
                Set<String> orgsSet = BillInfoUtils.getOrgPermissions("cmp_bankreconciliationlist");
                if (orgsSet != null && orgsSet.size() > 0) {
                    String[] orgs = orgsSet.toArray(new String[orgsSet.size()]);
                    group.appendCondition(QueryCondition.name(IBussinessConstant.ACCENTITY).in(orgs));
                }
                querySchema.addCondition(group);
                List<BankReconciliation> bankReconciliationList = MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME, querySchema, null);
                if (bankReconciliationList != null && bankReconciliationList.size() > 0) {
                    //精度处理
                    CurrencyTenantDTO currencyDTO = baseRefRpcService.queryCurrencyById((String) bankReconciliationList.get(0).get("currency"));
                    moneydigit = currencyDTO.getMoneydigit();
                    if (moneydigit > 2) {
                        for (int i = 0; i < moneydigit - 2; i++) {
                            formatStr = formatStr + "0";
                        }
                    }
                    List<BankReconciliation> debitlist = bankReconciliationList.stream().filter(bankElectronicReceipt -> bankElectronicReceipt.getDc_flag().equals(Direction.Debit)).collect(Collectors.toList());
                    List<BankReconciliation> creditlist = bankReconciliationList.stream().filter(bankElectronicReceipt -> bankElectronicReceipt.getDc_flag().equals(Direction.Credit)).collect(Collectors.toList());
                    if (debitlist != null && debitlist.size() > 0) {
                        debitanum = debitlist.size();//借方笔数
                        debitamount = debitlist.stream().map(BankReconciliation::getTran_amt).reduce(BigDecimal.ZERO, BigDecimal::add);

                    }
                    if (creditlist != null && creditlist.size() > 0) {
                        creditanum = creditlist.size();//贷方笔数
                        creditamount = creditlist.stream().map(BankReconciliation::getTran_amt).reduce(BigDecimal.ZERO, BigDecimal::add);
                    }

                }

            }
            jsonObject.put("debitanum", debitanum);//借方笔数
            jsonObject.put("creditanum", creditanum);//贷方笔数
            jsonObject.put("debitamount", new DecimalFormat(formatStr).format(debitamount));//借方金额
            jsonObject.put("creditamount", new DecimalFormat(formatStr).format(creditamount));//贷方金额
            jsonObject.put("amountnum", debitanum + creditanum);//总计
            jsonObject.put("amountsum", new DecimalFormat(formatStr).format(debitamount.add(creditamount)));//总计金额
            jsonObject.put(ICmpConstant.MSG, ICmpConstant.SUCCESS);
        } catch (Exception e) {
            log.error("银行对账单汇总信息查询失败");
        }
        return jsonObject;
    }

    /**
     * 根据银行对账单id查询交易回单对应回单文件id
     *
     * @param bankReconciliation
     * @return
     * @throws Exception
     */
    public String getBankReceiptFileId(BankReconciliation bankReconciliation) throws Exception {
        QuerySchema schema = QuerySchema.create();
        schema.addSelect("*");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("bankreconciliationid").eq(bankReconciliation.getId().toString()));
        schema.addCondition(conditionGroup);
        schema.addPager(0, 1);
        List<BankElectronicReceipt> bankElectronicReceipts = MetaDaoHelper.queryObject(BankElectronicReceipt.ENTITY_NAME, schema, null);
        if (!CollectionUtils.isEmpty(bankElectronicReceipts)) {
            BankElectronicReceipt bankElectronicReceipt = bankElectronicReceipts.get(0);
            String extendss = bankElectronicReceipt.getExtendss();
            if (ObjectUtils.isNotEmpty(extendss)) {
                return extendss;
            }
        }
        return null;
    }

    /**
     * 发送银行对账单关联交易回单文件id事件
     *
     * @param bankReconciliation
     * @param urlId
     * @throws Exception
     */
    @Override
    public void sendEventOfFileidInFinal(BankReconciliation bankReconciliation, String urlId) throws Exception {
        if (bankReconciliation != null && org.apache.commons.lang3.StringUtils.isNotEmpty(urlId)) {
            this.sendEventOfFileid(bankReconciliation.getId(), urlId);
        }
    }

    /**
     * 发送银行对账单关联交易回单文件id事件
     *
     * @param id
     * @param urlId
     * @throws Exception
     */
    @Override
    public void sendEventOfFileid(Long id, String urlId) throws Exception {
        if (id == null || StringUtils.isEmpty(urlId)) {
            return;
        }
        CommonRequestDataVo vo = new CommonRequestDataVo();
        BusinessEventBuilder businessEventBuilder = new BusinessEventBuilder();

        QuerySchema schema = QuerySchema.create();
        schema.addSelect("*");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("bankbill").eq(id));
        schema.addCondition(conditionGroup);
        List<BillClaimItem> billClaimItems = MetaDaoHelper.queryObject(BillClaimItem.ENTITY_NAME, schema, null);
        if (!CollectionUtils.isEmpty(billClaimItems)) {
            List<Long> claimIds = billClaimItems.stream().map(BillClaimItem::getMainid).collect(Collectors.toList());
            vo.setClaimIds(claimIds);
        }
        // 事件源编码 ，事件类型编码，租户id
        businessEventBuilder.setSourceId(IEventCenterConstant.CMP_BANKRECONCILIATION);
        businessEventBuilder.setEventType(IEventCenterConstant.CTM_CMP_BANKELECTRONICRECEIPTURL);
        businessEventBuilder.setBillId(String.valueOf(id));
        businessEventBuilder.setTenantCode(InvocationInfoProxy.getTenantid());
        businessEventBuilder.setAction("receiptassociation");
        // 事件体
        vo.setBankId(id);
//        vo.setClaimId(id);
        vo.setUrl(urlId);
        businessEventBuilder.setUserObject(vo);
        //如果需要租户默认token，tenantCode要传入yht租户id
        BusinessEvent businessEvent = businessEventBuilder.build();
        //记录业务日志
        CtmJSONObject logparam = new CtmJSONObject();
        logparam.put("userObject", businessEvent);
        ctmcmpBusinessLogService.saveBusinessLog(logparam, id.toString(), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400582", "银行流水关联银行回单时") /* "银行流水关联银行回单时" */, IServicecodeConstant.BANKRECONCILIATION, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400586", "银行流水关联银行回单") /* "银行流水关联银行回单" */, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400587", "发送银行对账单关联交易回单文件id事件") /* "发送银行对账单关联交易回单文件id事件" */);
        eventService.fireLocalEvent(businessEvent);

    }

    @Override
    public void cancelUrl(Long id, String urlId) throws Exception {
        CommonRequestDataVo vo = new CommonRequestDataVo();
        BusinessEventBuilder businessEventBuilder = new BusinessEventBuilder();

        QuerySchema schema = QuerySchema.create();
        schema.addSelect("*");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("bankbill").eq(id));
        schema.addCondition(conditionGroup);
        List<BillClaimItem> billClaimItems = MetaDaoHelper.queryObject(BillClaimItem.ENTITY_NAME, schema, null);
        if (!CollectionUtils.isEmpty(billClaimItems)) {
            List<Long> claimIds = billClaimItems.stream().map(BillClaimItem::getMainid).collect(Collectors.toList());
            vo.setClaimIds(claimIds);
        }
        // 事件源编码 ，事件类型编码，租户id
        businessEventBuilder.setSourceId(IEventCenterConstant.CMP_BANKRECONCILIATION);
        businessEventBuilder.setEventType(IEventCenterConstant.CTM_CMP_BANKELECTRONICRECEIPTURL);
        businessEventBuilder.setBillId(String.valueOf(id));
        businessEventBuilder.setTenantCode(InvocationInfoProxy.getTenantid());
        businessEventBuilder.setAction("cancelReceiptassociation");
        // 事件体
        vo.setBankId(id);
        vo.setClaimId(id);
        vo.setUrl(urlId);
        businessEventBuilder.setUserObject(vo);
        //如果需要租户默认token，tenantCode要传入yht租户id
        BusinessEvent businessEvent = businessEventBuilder.build();
        eventService.fireLocalEvent(businessEvent);

    }

    /**
     * 取消单据关联
     *
     * @param id
     * @return
     * @throws Exception
     */
    @Override
    public CtmJSONObject cancelCorrelate(Long id) throws Exception {

        String key = ICmpConstant.CMPBANKRECONCILIATIONLIST + id;
        YmsLock ymsLock = JedisLockUtils.lockBillWithOutTrace(key);
        if (null == ymsLock) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101190"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00143", "该数据正在处理，请稍后重试！") /* "该数据正在处理，请稍后重试！" */);
        }
        CtmJSONObject result = new CtmJSONObject();
        result.put("dealSucceed", false);
        try {
            BankReconciliation bankReconciliation = MetaDaoHelper.findById(BankReconciliation.ENTITY_NAME, id);
            if (bankReconciliation.getIspublish() != null && bankReconciliation.getIspublish()) {
                throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21EA3E1A05200002", "取消单据关联失败：当前流水已发布，不允许从流水处理节点取消单据关联！"));
            }
            SettleDeatailRelBankBillReqVO settleDeatailRelBankBillReqVO = new SettleDeatailRelBankBillReqVO();
            // 判断银行对账单关联类型
            List<BankReconciliationbusrelation_b> bankReconciliationbusrelation_bs = bankReconciliation.BankReconciliationbusrelation_b();
            if (bankReconciliationbusrelation_bs == null || bankReconciliationbusrelation_bs.size() == 0) {
                throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21EA3DE205200005", "取消单据关联失败：当前流水未关联资金结算明细！不允许取消单据关联！"));
            }
            List<BankReconciliationbusrelation_b> bankReconciliationbusrelationList = bankReconciliation.BankReconciliationbusrelation_b();
            boolean stwbbillFlag = false;
            for (BankReconciliationbusrelation_b bankReconciliationbus : bankReconciliationbusrelationList) {
                // 自动关联 或 手工关联
                // 判断关联单据
                if (bankReconciliationbus.getBilltype() == EventType.StwbSettleMentDetails.getValue()) {
                    settleDeatailRelBankBillReqVO.setSettleBenchB_id(bankReconciliationbus.getBillid().toString());
                    // 只有 自动关联 或 手工关联类型 才允许手动取消关联
                    if (bankReconciliationbus.getRelationtype() != Relationtype.AutoAssociated.getValue() && bankReconciliationbus.getRelationtype() != Relationtype.ManualAssociated.getValue()) {
                        throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21EA3DA804A00005", "取消单据关联失败：当前流水不是通过自动关联或手工关联的结算明细！请检查！"));
                    }
                    stwbbillFlag = true;
                }
                if (!stwbbillFlag) {
                    throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21EA3B7404A00008", "取消单据关联失败：当前流水关联的单据非资金结算明细，不允许取消单据关联！"));
                }
            }
            settleDeatailRelBankBillReqVO.setBankCheck_id(bankReconciliation.getId().toString());
            if (bankReconciliation.getRefundstatus() != null && bankReconciliation.getRefundstatus().equals(RefundStatus.Refunded.getValue())) {
                //传入退票状态和退票金额
                settleDeatailRelBankBillReqVO.setIsrefund(String.valueOf(bankReconciliation.getRefundstatus()));
                settleDeatailRelBankBillReqVO.setRefundAmt(bankReconciliation.getTran_amt());
                settleDeatailRelBankBillReqVO.setIsrefund("1");//退票状态 1- 退票
            } else { //非退票.必传，不然结算会报错
                settleDeatailRelBankBillReqVO.setIsrefund(String.valueOf(0));
                settleDeatailRelBankBillReqVO.setRefundAmt(new BigDecimal(String.valueOf(0)));
            }

            // 先删除我们的关系，删除成功之后再调用结算接口取消关联，结算那边需要同步调整。只调整V5。
            CommonRequestDataVo json = new CommonRequestDataVo();
            // 业务单据id
            json.setStwbbusid(bankReconciliation.BankReconciliationbusrelation_b().get(0).getBillid());
            // 银行流水id
            json.setBusid(bankReconciliation.BankReconciliationbusrelation_b().get(0).getBankreconciliation().toString());
            if (json.getQueryDataForMap() == null) {
                Map<String, Object> map = new HashMap<>();
                map.put(CommonParametersUtils.BANK_RECONCILIATION_DELETE_KEY, CommonParametersUtils.BANK_RECONCILIATION_DELETE_VALUE);
                json.setQueryDataForMap(map);
            } else {
                json.getQueryDataForMap().put(CommonParametersUtils.BANK_RECONCILIATION_DELETE_KEY, CommonParametersUtils.BANK_RECONCILIATION_DELETE_VALUE);
            }
            ctmcmpReWriteBusRpcService.resDelDataForRpc(json);

            settleDeatailRelBankBillReqVO.setSettleBenchB_id(bankReconciliation.BankReconciliationbusrelation_b().get(0).getBillid().toString());
            CtmJSONObject logParams = new CtmJSONObject();
            logParams.put("bankReconciliation", bankReconciliation);
            ctmcmpBusinessLogService.saveBusinessLog(logParams, bankReconciliation.getBank_seq_no(), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400583", "银行流水") /* "银行流水" */, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540057E", "银行流水处理") /* "银行流水处理" */, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400584", "取消单据关联") /* "取消单据关联" */, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400585", "银行流水取消单据关联") /* "银行流水取消单据关联" */);
            ResultStrRespVO resultStrRespVO = settleBenchBRPCService.cancelRelationSettleBench(settleDeatailRelBankBillReqVO);
            // 如果是自动或手动关联的结算明细，则需要删除对应的关联关系
            // 自动关联 或 手工关联
            result.put("dealSucceed", true);
            result.put("data", resultStrRespVO);
        } catch (Exception e) {
            result.put("dealSucceed", false);
            result.put(ICmpConstant.MSG, e.getMessage());
            throw new CtmException(e.getMessage(), e);
        } finally {
            JedisLockUtils.unlockBillWithOutTrace(ymsLock);
        }
        return result;
    }

    private List<String> getAccentityList(List<String> orgList, Boolean inoutFlag, Boolean agentFlag) throws Exception {
        List<String> accentityList = new ArrayList<>();
        accentityList = orgList;
        if (inoutFlag) {
            // 查询统收统支关系：下级查上级
            IncomeAndExpenditureReqVO incomeAndExpenditureReqVO = new IncomeAndExpenditureReqVO();
            incomeAndExpenditureReqVO.setControllId(orgList);
            List<IncomeAndExpenditureResVO> incomeAndExpenditureResVOS = iOpenApiService.queryRecControllList(incomeAndExpenditureReqVO);
            List<String> expendIds = new ArrayList<>();
            if (incomeAndExpenditureResVOS != null && incomeAndExpenditureResVOS.size() > 0) {
                for (IncomeAndExpenditureResVO incomeAndExpenditureResVO : incomeAndExpenditureResVOS) {
                    expendIds.add(incomeAndExpenditureResVO.getActualAccentity());
                }
            }
            accentityList.addAll(expendIds);
        }
        if (agentFlag) {
            //查询资金业务委托关系  资金组织查询结算中心
            QueryDelegationReqVo queryDelegationReqVo = new QueryDelegationReqVo();
            queryDelegationReqVo.setAccentitys(orgList);
            queryDelegationReqVo.setEnableoutagestatus(0);
            Result result = iBusinessDelegationApiService.queryBusinessDelegation(queryDelegationReqVo);
            Set<String> expendIds = new HashSet<>();
            if (result.getData() != null && result.getCode() != 404) {
                List<BusinessDelegationVo> businessDelegationVos = (List<BusinessDelegationVo>) result.getData();
                if (businessDelegationVos != null && businessDelegationVos.size() > 0) {
                    for (BusinessDelegationVo businessDelegationVo : businessDelegationVos) {
                        if (businessDelegationVo.getSettlementCenter() != null) {
                            expendIds.add(businessDelegationVo.getSettlementCenter());
                        }
                    }
                }
            }
            accentityList.addAll(expendIds);
        }
        return accentityList;
    }


    /**
     * 将BankReconciliation转成BankReconciliationVo
     *
     * @param bankReconciliation
     * @return
     */
    public BankReconciliationVo convertBankReconciliation2BankReconciliationVO(BankReconciliation bankReconciliation) {
        BankReconciliationVo bankReconciliationVo = new BankReconciliationVo();
        bankReconciliationVo.setTran_date(bankReconciliation.getTran_date());
        bankReconciliationVo.setTran_amt(bankReconciliation.getTran_amt());
        bankReconciliationVo.setCurrency(bankReconciliation.getCurrency());
        bankReconciliationVo.setBankaccount(bankReconciliation.getBankaccount());
        bankReconciliationVo.setOrgid(bankReconciliation.getOrgid());
        bankReconciliationVo.setAccentity(bankReconciliation.getAccentity());
        bankReconciliationVo.setId(bankReconciliation.getId() == null ? "" : bankReconciliation.getId().toString());
        // 对方信息
        bankReconciliationVo.setTo_acct(bankReconciliation.getTo_acct());
        bankReconciliationVo.setTo_acct_no(bankReconciliation.getTo_acct_no());
        bankReconciliationVo.setTo_acct_name(bankReconciliation.getTo_acct_name());
        bankReconciliationVo.setOppositeobjectid(bankReconciliation.getOppositeobjectid());
        // 对方类型
        bankReconciliationVo.setOppositeType(bankReconciliation.getOppositetype());
        bankReconciliationVo.setImpinneraccount(bankReconciliation.getImpinneraccount());
        bankReconciliationVo.setIsinneraccounting(bankReconciliation.getIsinneraccounting());
        bankReconciliationVo.setDc_flag(bankReconciliation.getDc_flag().getValue());
        bankReconciliationVo.setRemark(bankReconciliation.getRemark());
        bankReconciliationVo.setRemark01(bankReconciliation.getRemark01());
        bankReconciliationVo.setUse_name(bankReconciliation.getUse_name());
        bankReconciliationVo.setCreditamount(bankReconciliation.getCreditamount());
        bankReconciliationVo.setDebitamount(bankReconciliation.getDebitamount());
        bankReconciliationVo.setBankType(bankReconciliation.getBanktype());
        // 银行交易流水号
        bankReconciliationVo.setBankSeqNo(bankReconciliation.getBank_seq_no());
        return bankReconciliationVo;
    }

    /**
     * 将BankReconciliationbusrelation_b转成BankReconciliationbusrelationVo
     *
     * @param bankRecRel
     * @return
     */
    public BankReconciliationbusrelationVo convertBankRecRel2BankRelVO(BankReconciliationbusrelation_b bankRecRel) {
        BankReconciliationbusrelationVo bankRecRelVo = new BankReconciliationbusrelationVo();
        bankRecRelVo.setId(bankRecRel.getId().toString());
        bankRecRelVo.setBankreconciliation(bankRecRel.getBankreconciliation().toString());
        bankRecRelVo.setBilltype(bankRecRel.getBilltype());
        bankRecRelVo.setVouchdate(bankRecRel.getVouchdate());
        bankRecRelVo.setSrcbillid(bankRecRel.getSrcbillid() == null ? null : bankRecRel.getSrcbillid().toString());
        bankRecRelVo.setBillid(bankRecRel.getBillid() == null ? null : bankRecRel.getBillid().toString());
        bankRecRelVo.setBillcode(bankRecRel.getBillcode());
        bankRecRelVo.setAccentity(bankRecRel.getAccentity());
        bankRecRelVo.setDept(bankRecRel.getDept());
        bankRecRelVo.setProject(bankRecRel.getProject());
        bankRecRelVo.setAmountmoney(bankRecRel.getAmountmoney());
        bankRecRelVo.setRelationstatus(bankRecRel.getRelationstatus());
        bankRecRelVo.setRelationtype(bankRecRel.getRelationtype());
        return bankRecRelVo;
    }

    @Override
    public Map<String, Object> querySub(String id) throws Exception {
        QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("bankbill").eq(id));
        QuerySchema schema = QuerySchema.create().addSelect("*");
        schema.addCondition(group);
        List<Map<String, Object>> result = MetaDaoHelper.query(BillClaimItem.ENTITY_NAME, schema);
        if (result != null && result.size() > 0) {
            for (Map<String, Object> resultMap : result) {
                String mainId = resultMap.get("mainid").toString();
                BillClaim billClaim = MetaDaoHelper.findById(BillClaim.ENTITY_NAME, mainId);
                // 按业务逻辑，子表在同一个认领单主表下，循环赋值主表字段值，前端展示
                resultMap.put("mainid", billClaim.getId().toString());
                resultMap.put("mainid_code", billClaim.getCode());
                resultMap.put("mainid_vouchdate", billClaim.getVouchdate());
                resultMap.put("mainid_claimstaff", billClaim.getClaimstaff());
                if (billClaim.getDept() != null && !"".equals(billClaim.getDept())) {
                    BaseDeptDTO deptDTO = bizDeptQueryService.getById(AppContext.getYhtTenantId().toString(), billClaim.getDept());
                    resultMap.put("mainid_dept_name", deptDTO.getName());
                }
                if (billClaim.getProject() != null && !"".equals(billClaim.getProject())) {
                    ProjectDTO projectDTO = projectService.getByIdObj(billClaim.getProject());
                    resultMap.put("mainid_project_name", projectDTO.getName().replaceAll("\\\"", ""));
                }
                if (billClaim.getActualclaimaccentiry() != null && !"".equals(billClaim.getActualclaimaccentiry())) {
                    List<Map<String, Object>> accentityObj = QueryBaseDocUtils.getOrgMVById(billClaim.getActualclaimaccentiry());
                    resultMap.put("accentity_name", (String) accentityObj.get(0).get("name"));
                }
                if (billClaim.getCurrency() != null && !"".equals(billClaim.getCurrency())) {
                    CurrencyTenantDTO currencyTenantDTO = currencyQueryService.findById(billClaim.getCurrency());
                    resultMap.put("currency_name", currencyTenantDTO.getName());
                    resultMap.put("currency_moneyDigit", currencyTenantDTO.getMoneydigit());
                    resultMap.put("totalamount", new BigDecimal(resultMap.get("totalamount").toString()).setScale(currencyTenantDTO.getMoneydigit()));
                    resultMap.put("claimamount", new BigDecimal(resultMap.get("claimamount").toString()).setScale(currencyTenantDTO.getMoneydigit()));
                }
                resultMap.put("mainid_claimtype", billClaim.getClaimtype());
                resultMap.put("mainid_remark", billClaim.getRemark());
            }
        }
        Map<String, Object> retMap = new HashMap<>();
        retMap.put("result", result);
        return retMap;
    }

    @Override
    public BankReconciliation queryById(Long id) throws Exception {
        QueryConditionGroup group = new QueryConditionGroup();
        group.addCondition(QueryCondition.name("id").eq(id));
        QueryConditionGroup repeatGroup = QueryConditionGroup.or(QueryCondition.name("isrepeat").is_not_null(), QueryCondition.name("isrepeat").is_null());
        group.addCondition(repeatGroup);
        QuerySchema querySchema = QuerySchema.create().addSelect(ICmpConstant.SELECT_TOTAL_PARAM).addCondition(group);
        List<BankReconciliation> list = MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME, querySchema, null);
        if (CollectionUtils.isEmpty(list)) {
            return null;
        }
        return list.get(0);
    }

    @Override
    public List<BankReconciliation> queryByIds(List<Long> ids) throws Exception {
        QueryConditionGroup group = new QueryConditionGroup();
        group.addCondition(QueryCondition.name("id").in(ids));
        QueryConditionGroup repeatGroup = QueryConditionGroup.or(QueryCondition.name("isrepeat").is_not_null(), QueryCondition.name("isrepeat").is_null());
        group.addCondition(repeatGroup);
        QuerySchema querySchema = QuerySchema.create().addSelect(ICmpConstant.SELECT_TOTAL_PARAM).addCondition(group);
        return MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME, querySchema, null);
    }

    /**
     * 查询银行流水认领批改过滤的字段
     *
     * @return
     */
    @Override
    public List<String> queryFilterFields() {
        String defaultFiterFields = "bankaccount_name,accentityRaw_code,accentityRaw_name,accentityRaw,bankaccount,bankaccount_account," +
                "currency,currency_name,orgid,orgid_name,distributestatus,publishdistributestatus,entrytype,confirmstatus,bankcheckno," +
                "associationstatus,ispublish,claimamount,amounttobeclaimed,receiptassociation,dataOrigin,refundstatus,oper,checkflag,checkdate," +
                "autobill,other_checkflag,smartcheckno,isparsesmartcheckno,isimputation,eliminateStatus,manualgenertbilltype,isinneraccounting," +
                "eliminate_amt,after_eliminate_amt,impinneraccount_name,earlyentryflag,unique_no,refundrulescode,refundauto,publishrulescode," +
                "publishdate,serialauto,rejectflag,billrulescode,billrulesdate,datasourcename,datasourcesystem,datasourcesystemcode,billoperator," +
                "billoperator_name,billassociationdate,autodealstate,serialdealendstate,isautosubmit,serialdealtype,dealuser,dealuser_name,dealdate," +
                "published_type,tripleSynchronStatus,thirdserialno,frozenstatus,modifier,modifyTime,creator,createTime,isvirtualflow,other_checkdate," +
                "other_checkno,rpaimport,originbankseqno,debitamount,obversion_debitamount,creditamount,obversion_debitamount,value_date," +
                "dzdate,accentity_code,isrepeat,banktype_name,isreturned,rejectflag,cash_flag,refundflag,detailReceiptRelationCode,orgid_code";
        String ymsFilterFields = AppContext.getEnvConfig("bankreconciliation_batchmodify_fields", defaultFiterFields);
        return Arrays.asList(ymsFilterFields.split(","));
    }

    /**
     * 根据锁过滤流水，避免并发的场景
     *
     * @param bankReconciliationList
     * @return
     */
    @Override
    public List<BankReconciliation> filterBankReconciliationByLockKey(List<BankReconciliation> bankReconciliationList, BankReconciliationActions bankReconciliationActions) {
        List<BankReconciliation> realBankReconciliation = new ArrayList<>();
        if (AppContext.getEnvConfig("cmp.bankReconciliation.lock", "0").equals("1")) {
            if (CollectionUtils.isNotEmpty(bankReconciliationList)) {
                // 自动关联逻辑，添加流水正在处理中的过滤
                for (BankReconciliation bankReconciliation : bankReconciliationList) {
                    if (AppContext.cache().exists(bankReconciliation.getId().toString())) {
                        //记录业务日志
                        CtmJSONObject logparam = new CtmJSONObject();
                        logparam.put("bankreconciliationinfo", bankReconciliation);
                        logparam.put("bank_seq_no", bankReconciliation.getBank_seq_no());
                        ctmcmpBusinessLogService.saveBusinessLog(logparam, logparam.getString("bank_seq_no"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540057E", "银行流水处理") /* "银行流水处理" */, IServicecodeConstant.CMPBANKRECONCILIATION, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540057C", "流水执行状态:") /* "流水执行状态:" */ + AppContext.cache().get(bankReconciliation.getId().toString()), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540057D", "流水执行状态") /* "流水执行状态" */);
                        log.error("当前流水正在处理中，需要跳过，对应流水id" + bankReconciliation.getId() + "，对应交易流水号：" + bankReconciliation.getBank_seq_no());
                    } else {
                        realBankReconciliation.add(bankReconciliation);
                    }
                }
                List<String> lockKeyValues = new ArrayList<>();
                realBankReconciliation.stream().forEach(e -> {
                    lockKeyValues.add(e.getId().toString());
                    lockKeyValues.add(bankReconciliationActions.getName());
                });
                AppContext.cache().mset(lockKeyValues.toArray(new String[lockKeyValues.size()]));
            }
        } else {
            realBankReconciliation = bankReconciliationList;
        }
        return realBankReconciliation;
    }
}
