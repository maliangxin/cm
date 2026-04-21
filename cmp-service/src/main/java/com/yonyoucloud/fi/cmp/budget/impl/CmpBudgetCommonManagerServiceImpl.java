package com.yonyoucloud.fi.cmp.budget.impl;

import com.yonyou.cloud.utils.CollectionUtils;
import com.yonyou.epmp.busi.service.BusiSystemConfigService;
import com.yonyou.epmp.control.dto.ControlDetailVO;
import com.yonyou.epmp.control.service.ExecdataControlService;
import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.iuap.ucf.common.i18n.InternationalUtils;
import com.yonyou.iuap.yms.id.generator.YmsOidGenerator;
import com.yonyou.ucf.mdd.ext.bpm.model.VerifyState;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cm.transferaccount.TransferAccount;
import com.yonyoucloud.fi.cmp.budget.BudgetDirect;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetCommonManagerService;
import com.yonyoucloud.fi.cmp.cmpentity.OccupyBudget;
import com.yonyoucloud.fi.cmp.cmpentity.SettleStatus;
import com.yonyoucloud.fi.cmp.common.service.CmCommonService;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.enums.BillMapEnum;
import com.yonyoucloud.fi.cmp.enums.CharacterDefEnum;
import com.yonyoucloud.fi.cmp.event.vo.ResultBudget;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.constants.CommonConstant;
import com.yonyoucloud.fi.cmp.util.BillAction;
import com.yonyoucloud.fi.cmp.util.BudgetUtils;
import com.yonyoucloud.fi.cmp.util.RestTemplateUtils;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.orm.base.BizObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@Transactional(rollbackFor = RuntimeException.class)
public class CmpBudgetCommonManagerServiceImpl implements CmpBudgetCommonManagerService {
    @Value("${billdetail-url}")
    private String billDetailUrl;
    @Autowired
    private CmCommonService cmCommonService;
    @Autowired
    private BusiSystemConfigService busiSystemConfigService;
    @Autowired
    private ExecdataControlService execdataControlService;
    @Autowired
    private YmsOidGenerator ymsOidGenerator;

    @Override
    public Map<String, Object> buildBudgetParam(BizObject bizObject, int addOrReduce, String preemptionOrExecFlag, String billAction, String billCode, String serviceCode) throws Exception {
        Map<String, Object> budgetParam = new HashMap<>();
        //初始化
        Object id = ValueUtils.isNotEmptyObj(bizObject.getId()) ? bizObject.getId() : ymsOidGenerator.nextId();
        budgetParam.putAll(bizObject);
        budgetParam.put(SIGNATURE, null);
        budgetParam.put(SERVICE_CODE, serviceCode);
        budgetParam.put(BILL_ID, id);
        budgetParam.put(BILL_LINE_ID, id);
        budgetParam.put(BILL_NO, bizObject.getString("code"));
        budgetParam.put(BILL_CODE, billCode);
        budgetParam.put(ACTION, billAction);
        budgetParam.put("requsetbilltradetype", bizObject.get("tradetype").toString());//交易类型，用来拼接unquieRequestId
        budgetParam.put("requsetbillaction", preemptionOrExecFlag);//预占或实占动作，用来拼接unquieRequestId
        budgetParam.put("requsetbillid", id);//时间戳，用来拼接unquieRequestId
        budgetParam.put("requsetbillpubts", ValueUtils.isNotEmptyObj(bizObject.get("pubts")) ?bizObject.get("pubts").toString() : ymsOidGenerator.nextId());//时间戳，用来拼接unquieRequestId
        budgetParam.put(TRANSAC_CODE, cmCommonService.getDefaultTransTypeCode(bizObject.getString("tradetype")));
        budgetParam.put(ADD_OR_REDUCE, addOrReduce);
        budgetParam.put(LINE_NO, id);
        String characterDefKey = "characterDef";//这个需要看元数据字段上面特征字段是不是这个,目前来看主表都是这个,设置一个默认值
        CharacterDefEnum enumByBillCode = CharacterDefEnum.getEnumByBillCode(billCode);
        if (enumByBillCode != null) {
            characterDefKey = enumByBillCode.getCharacterDefName();
        }
        Map characterDefMap = bizObject.get(characterDefKey);
        String characterDefKey_ = characterDefKey + "_";
        if (characterDefMap != null && !characterDefMap.isEmpty()) {
            characterDefMap.forEach((key, value) -> budgetParam.put(characterDefKey_ + key, value));
        }
        budgetParam.put(IS_OFFSET, addOrReduce == 1 ? true : false);
        if (PRE.equals(preemptionOrExecFlag)) {
            budgetParam.put(PREEMPTION_OR_EXECFLAG, 0);
        } else if (IMPLEMENT.equals(preemptionOrExecFlag)) {
            budgetParam.put(PREEMPTION_OR_EXECFLAG, 1);
        }
        return budgetParam;
    }

    @Override
    public String queryBudgetDetail(CtmJSONObject json) {
        try {
            String billNum = json.getString("billno");
            Long id = json.getLong("id");
            if (id == null || StringUtils.isEmpty(billNum)) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100235"),InternationalUtils.getMessageWithDefault("UID:P_GRM-BE_1811A1FC05B00042", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18AD061204380012", "请求参数缺失") /* "请求参数缺失" */) /* "请求参数缺失" */);
            }
            BillMapEnum billMapEnum = BillMapEnum.getEnumByBillNum(billNum);
            if (billMapEnum == null) {
                log.error("根据billNum没有匹配上billMapEnum");
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100235"),InternationalUtils.getMessageWithDefault("UID:P_GRM-BE_1811A1FC05B00042", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18AD061204380012", "请求参数缺失") /* "请求参数缺失" */) /* "请求参数缺失" */);
            }
            CtmJSONObject resultBack = new CtmJSONObject();
            resultBack.put(ICmpConstant.CODE, false);
            BizObject bizObject = MetaDaoHelper.findById(billMapEnum.getEntityName(), id);
            Short isOccupyBudget = bizObject.getShort("isOccupyBudget");
            String preemptionOrExecFlag = "";
            String billAction = BillAction.SUBMIT;
            if (isOccupyBudget != null && isOccupyBudget == OccupyBudget.PreSuccess.getValue()) {
                preemptionOrExecFlag = PRE;
            } else if (isOccupyBudget != null && isOccupyBudget == OccupyBudget.ActualSuccess.getValue()) {
                preemptionOrExecFlag = IMPLEMENT;
            } else {
                billAction = BillAction.QUERY;
            }
            int addOrReduce = 0;//新增
            Map[] bills = new Map[1];
            Map jo = buildBudgetParam(bizObject, addOrReduce, preemptionOrExecFlag, billAction, billMapEnum.getBillCode(), billMapEnum.getServiceCode());
            bills[0] = jo;
            CtmJSONArray requestJs = new CtmJSONArray();
            CtmJSONObject request = new CtmJSONObject();
            request.put("bills", bills);//bills	Y	业务单据数组	Array
            request.put("busiBillId", id);//busiBillId	Y	单据主键	String
            request.put("busiSysCode", BudgetUtils.SYSCODE);//busiSysCode	Y	业务系统编码	String
            request.put("ytenantId", InvocationInfoProxy.getTenantid());//ytenantId	Y	租户ID	String
            requestJs.add(request);
            CtmJSONObject result = RestTemplateUtils.doPostByJSONArray(billDetailUrl, requestJs);
            log.error("result ", result.toString());
            if (ICmpConstant.REQUEST_SUCCESS_STATUS_CODE.equals(result.getString("code"))) {
            } else {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100340"),result.getString("message"));
            }
            resultBack.put(ICmpConstant.CODE, true);
            return ResultMessage.data(resultBack);
        } catch (Exception e) {
            log.error("execute ", e);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100234"),InternationalUtils.getMessageWithDefault("UID:P_GRM-BE_18113A5204B801BF", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18AD061204380011", "预算系统返回:") /* "预算系统返回:" */) /* "预算系统返回:" */ + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = RuntimeException.class)
    public String budgetCheckNew(List<BizObject> bizObjects, String billCode, String billAction) {
        try {
            CtmJSONObject resultBack = new CtmJSONObject();
            resultBack.put(ICmpConstant.CODE, false);
            String opportunity = getOpportunity(PRE, billCode);
            //operateFlag 0代表控制并记录数据，1代表记录数据不控制，2控制不记录数据
            int operateFlag = 2;
            List<Map<String, Object>> bills = new ArrayList<>();
            for (BizObject bizObject : bizObjects) {
                Object verifystate = bizObject.get("verifystate");
                BillMapEnum billMapEnum = BillMapEnum.getEnumByBillNum(billCode);
                if (billMapEnum == null) {
                    log.error("根据billNum没有匹配上billMapEnum");
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100235"),InternationalUtils.getMessageWithDefault("UID:P_GRM-BE_1811A1FC05B00042", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18AD061204380012", "请求参数缺失") /* "请求参数缺失" */) /* "请求参数缺失" */);
                }
                if (verifystate == null) {
                    verifystate = 0;
                }
                BizObject oldBill = MetaDaoHelper.findById(billMapEnum.getEntityName(), bizObject.getId(), 2);
                if (Short.parseShort(verifystate.toString()) == VerifyState.SUBMITED.getValue()) {
                    Map<String, Object> bill = buildBudgetParam(bizObject, BudgetDirect.ADD.getIndex(), PRE, opportunity, billCode, billMapEnum.getServiceCode());
                    bills.add(bill);
                    Map<String, Object> billReduce = buildBudgetParam(oldBill, BudgetDirect.REDUCE.getIndex(), PRE, opportunity, billCode, billMapEnum.getServiceCode());
                    bills.add(billReduce);
                } else {
                    Map<String, Object> bill = buildBudgetParam(bizObject, BudgetDirect.ADD.getIndex(), PRE, opportunity, billCode, billMapEnum.getServiceCode());
                    bills.add(bill);
                }
            }
            Map[] preBills = bills.toArray(new Map[0]);
            ControlDetailVO[] controlDetailVOS = initControlDetailVOs(preBills, opportunity, operateFlag);
            //4 控制接口(预占、实占)
            log.error("budgetCheck controlDetailVOS={}", CtmJSONObject.toJSONString(controlDetailVOS));
            Map map = execdataControlService.control(controlDetailVOS);
            String resultStr = CtmJSONObject.toJSONString(map);
            CtmJSONObject result = CtmJSONObject.parseObject(resultStr);
            log.error("execdataControlService.control, result={}", result.toString());
            doCheckResult(resultBack, result);
            log.error("execdataControlService.doCheckResult, result={}", resultBack);
            return ResultMessage.data(resultBack);
        } catch (Exception e) {
            log.error("execute ", e);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100234"),InternationalUtils.getMessageWithDefault("UID:P_GRM-BE_18113A5204B801BF", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400808", "预算系统返回:") /* "预算系统返回:" */) /* "预算系统返回:" */ + e.getMessage());
        }
    }


    @Override
    @Transactional(rollbackFor = RuntimeException.class)
    public ResultBudget budget(BizObject bizObject, String billAction, String billCode, String serviceCode, boolean cancel) throws Exception {
        try {
            log.error("budget...............");
            // 审批流提交动作匹配预算动作
            String opportunity = getOpportunity(PRE, billCode);
            short budgetDirect = cancel ? BudgetDirect.REDUCE.getIndex() : BudgetDirect.ADD.getIndex();
            // 拼装报文
            Map<String, Object> budgetParam = buildBudgetParam(bizObject, budgetDirect, PRE, billAction, billCode, serviceCode);
            ControlDetailVO[] controlDetailVOS = new ControlDetailVO[]{initControlDetailVO(budgetParam, opportunity, 0)};
            // 控制接口(预占、实占)
            log.error("预算预占请求报文：{}", CtmJSONObject.toJSONString(controlDetailVOS[0]));
            CtmJSONObject result = new CtmJSONObject(execdataControlService.control(controlDetailVOS));
            log.error("预算预占响应报文:{} ", CtmJSONObject.toJSONString(result));
            return doResult(result);
        } catch (Exception e) {
            log.error("预算预占 execute error:{} ", e.getMessage(), e);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100234"),InternationalUtils.getMessageWithDefault("UID:P_GRM-BE_18113A5204B801BF", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18AD061204380011", "预算系统返回:") /* "预算系统返回:" */) /* "预算系统返回:" */ + e.getMessage());
        }
    }

    @Override
    public ResultBudget implement(BizObject bizObject, String billCode, String serviceCode, boolean cancel) throws Exception {
        try {
            log.error("implement...........");
            String preAction = getOpportunity(PRE, billCode);
            String implementAction = getOpportunity(IMPLEMENT, billCode);
            // 未配置预占动作，则无需释放预占，直接实占
            ControlDetailVO[] controlDetailVOS;
            ControlDetailVO controlDetailVPre;
            ControlDetailVO controlDetailVImplement;
            Short isOccupyBudget = bizObject.getShort("isOccupyBudget");

            if (cancel) {
                Map implementBill;
                if (bizObject instanceof TransferAccount) {
                    TransferAccount transferAccount = (TransferAccount) bizObject;
                    // 同名账户划转撤回审批结算状态为已结算-动作为取消结算
                    if (transferAccount.getSettlestatus().equals(SettleStatus.alreadySettled)) {
                        implementBill = buildBudgetParam(bizObject, BudgetDirect.REDUCE.getIndex(), IMPLEMENT, BillAction.CANCEL_SETTLE, billCode, serviceCode);
                    } else {
                        implementBill = buildBudgetParam(bizObject, BudgetDirect.REDUCE.getIndex(), IMPLEMENT, BillAction.CANCEL_SUBMIT, billCode, serviceCode);
                    }
                } else {
                    implementBill = buildBudgetParam(bizObject, BudgetDirect.REDUCE.getIndex(), IMPLEMENT, BillAction.CANCEL_SUBMIT, billCode, serviceCode);
                }
                controlDetailVImplement = initControlDetailVO(implementBill, implementAction, 0);
                log.error("释放预算实占请求报文：{}", CtmJSONObject.toJSONString(controlDetailVImplement));
                controlDetailVOS = new ControlDetailVO[]{controlDetailVImplement};
            } else {
                if (isOccupyBudget == null || isOccupyBudget == OccupyBudget.UnOccupy.getValue()) {
                    Map implementBill = buildBudgetParam(bizObject, BudgetDirect.ADD.getIndex(), IMPLEMENT, BillAction.SETTLE_SUCCESS, billCode, serviceCode);
                    controlDetailVImplement = initControlDetailVO(implementBill, implementAction, 0);
                    log.error("预算实占请求报文:{}", CtmJSONObject.toJSONString(controlDetailVImplement));
                    controlDetailVOS = new ControlDetailVO[]{controlDetailVImplement};
                } else {
                    Map preBill = buildBudgetParam(bizObject, BudgetDirect.REDUCE.getIndex(), PRE, BillAction.SETTLE_SUCCESS, billCode, serviceCode);
                    controlDetailVPre = initControlDetailVO(preBill, preAction, 0);
                    log.error("预算实占释放预占请求报文：{}", CtmJSONObject.toJSONString(controlDetailVPre));
                    Map implementBill = buildBudgetParam(bizObject, BudgetDirect.ADD.getIndex(), IMPLEMENT, BillAction.SETTLE_SUCCESS, billCode, serviceCode);
                    controlDetailVImplement = initControlDetailVO(implementBill, implementAction, 0);
                    log.error("预算实占请求报文：{} ", CtmJSONObject.toJSONString(controlDetailVImplement));
                    controlDetailVOS = new ControlDetailVO[]{controlDetailVPre, controlDetailVImplement};
                }
            }
            // 控制接口(预占、实占)
            CtmJSONObject result = new CtmJSONObject(execdataControlService.control(controlDetailVOS));
            log.error("预算实占响应报文：{} ", CtmJSONObject.toJSONString(result));
            return doResult(result);
        } catch (Exception e) {
            log.error("execute ", e);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100234"),InternationalUtils.getMessageWithDefault("UID:P_GRM-BE_18113A5204B801BF", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18AD061204380011", "预算系统返回:") /* "预算系统返回:" */) /* "预算系统返回:" */ + e.getMessage());
        }
    }

    @Override
    public ResultBudget implementOnly(BizObject bizObject, String billCode, String serviceCode) throws Exception {
        try {
            log.error("implementOnly...........");
            String implementAction = getOpportunity(IMPLEMENT, billCode);
            // 未配置预占动作，则无需释放预占，直接实占
            ControlDetailVO[] controlDetailVOS;
            ControlDetailVO controlDetailVImplement;
            Map implementBill = buildBudgetParam(bizObject, BudgetDirect.ADD.getIndex(), IMPLEMENT, BillAction.APPROVE_PASS, billCode, serviceCode);
            controlDetailVImplement = initControlDetailVO(implementBill, implementAction, 0);
            log.error("implementOnly 预算实占请求报文：{}", CtmJSONObject.toJSONString(controlDetailVImplement));
            controlDetailVOS = new ControlDetailVO[]{controlDetailVImplement};
            // 控制接口(预占、实占)
            CtmJSONObject result = new CtmJSONObject(execdataControlService.control(controlDetailVOS));
            log.error("implementOnly预算实占响应报文：{}", CtmJSONObject.toJSONString(result));
            return doResult(result);
        } catch (Exception e) {
            log.error("implementOnly execute error:{} ", e.getMessage(), e);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100234"),InternationalUtils.getMessageWithDefault("UID:P_GRM-BE_18113A5204B801BF", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18AD061204380011", "预算系统返回:") /* "预算系统返回:" */) /* "预算系统返回:" */ + e.getMessage());
        }
    }

    /**
     * 根据业务系统注册页面配置的预占时机和执行时机
     *
     * @param action
     * @return
     */
    public String getOpportunity(String action, String billCode) throws Exception {
        String opportunity = "";
        // 查询接口
        List<Map> objects = (List<Map>) busiSystemConfigService.getBillAction(InvocationInfoProxy.getTenantid(), BudgetUtils.SYSCODE, billCode);
        //获取平台配置
        if (CollectionUtils.isNotEmpty(objects)) {
            opportunity = (String) objects.get(0).get(action);
        } else {
            log.error("getBillAction not support budget pre ");
        }
        return opportunity;
    }

//    @Override
//    public ControlDetailVO initControlDetailVO(Map[] bills, String action, int operateFlag) throws Exception {
//        ControlDetailVO controlDetailVO = new ControlDetailVO();
//        //担保变更预占
//        controlDetailVO.setBills(bills);//业务单据数组
//        controlDetailVO.setBusiSysCode(BudgetUtils.SYSCODE);
//        controlDetailVO.setYtenantId(InvocationInfoProxy.getTenantid());
//        //TODO 拼接UniqueId
//        controlDetailVO.setRequestUniqueId(UUID.randomUUID().toString());//请求唯一标识 请求唯一标识，同一标识数据幂等处理，防止重复提交数据。同时 数据处理完之后返回消息中会携带返回此标识，可明确成功数据。  业务系统（id或code）+单据类型（id或code）+交易类型（id或code）+单据id+单据动作code+pubts
//        controlDetailVO.setOperateFlag(operateFlag);//0代表控制并记录数据，1代表记录数据不控制，2控制不记录数据
////        controlDetailVO.setLedgerFlag(1);//ledgerFlag，0代表不记录台账，1代表记录台账,默认为0，如果需要保存台账相关信息，可以传1
//        controlDetailVO.setAction(action);//动作类型
//        /**
//         * 预算控制执行开启-业务参数-[临时]预算转移特殊逻辑(仅指定项目才需开启)=是
//         * 如付款申请单与付款单均占用预算，且只占一份预算需转移是使用，目前现金管理无此类逻辑，domainRelease设置为true走旧逻辑
//         */
//        controlDetailVO.setDomainRelease(true);
//        return controlDetailVO;
//
//    }

    @Override
    public ControlDetailVO initControlDetailVO(Map bill, String action, int operateFlag) throws Exception {
        if(null != bill.get("signature")){
            bill.put("signature", null);
        }
        ControlDetailVO controlDetailVO = new ControlDetailVO();
        //担保变更预占
        controlDetailVO.setBills(new Map[]{bill});//业务单据数组
        controlDetailVO.setBusiSysCode(BudgetUtils.SYSCODE);
        controlDetailVO.setYtenantId(InvocationInfoProxy.getTenantid());
        //拼接UniqueId
        //业务系统（id或code）+单据类型（id或code）+交易类型（id或code）+单据id+单据动作code+pubts
        controlDetailVO.setOperateFlag(operateFlag);//0代表控制并记录数据，1代表记录数据不控制，2控制不记录数据
        if(operateFlag == 2){//控制不记录数据，不需要传真实requestUniqueId
            controlDetailVO.setRequestUniqueId(UUID.randomUUID().toString());
        }else{
            String requestUniqueId = ICmpConstant.APPCODE + bill.get("serviceCode").toString() + bill.get("requsetbilltradetype").toString() +
                    bill.get("requsetbillid").toString() + bill.get("requsetbillaction").toString() + bill.get("addOrReduce").toString() + bill.get("requsetbillpubts").toString();
            controlDetailVO.setRequestUniqueId(requestUniqueId);
        }
//        controlDetailVO.setLedgerFlag(1);//ledgerFlag，0代表不记录台账，1代表记录台账,默认为0，如果需要保存台账相关信息，可以传1
        controlDetailVO.setAction(action);//动作类型
        /**
         * 预算控制执行开启-业务参数-[临时]预算转移特殊逻辑(仅指定项目才需开启)=是
         * 如付款申请单与付款单均占用预算，且只占一份预算需转移是使用，目前现金管理无此类逻辑，domainRelease设置为true走旧逻辑
         */
        controlDetailVO.setServiceCode(bill.get("serviceCode").toString());
        controlDetailVO.setDomainRelease(true);
        return controlDetailVO;

    }

    /**
     * *
     *
     * @param bills
     */
    @Override
    public ControlDetailVO[] initControlDetailVOs( Map[] bills, String action, int operateFlag) throws Exception {
        ControlDetailVO[] retVos = new ControlDetailVO[bills.length];
        for (int i = 0; i < bills.length; i++) {
            if(null != bills[i].get("signature")){
                bills[i].put("signature", null);
            }
            ControlDetailVO controlDetailVO = new ControlDetailVO();
            //担保变更预占
            Map<String, Object>[] newbills = new Map[1];
            newbills[0] = bills[i];
            controlDetailVO.setBills(newbills);//业务单据数组
            controlDetailVO.setBusiSysCode(BudgetUtils.SYSCODE);
            controlDetailVO.setYtenantId(InvocationInfoProxy.getTenantid());
            controlDetailVO.setOperateFlag(operateFlag);//0代表控制并记录数据，1代表记录数据不控制，2控制不记录数据
            if(operateFlag == 2){//控制不记录数据，不需要传真实requestUniqueId
                controlDetailVO.setRequestUniqueId(UUID.randomUUID().toString());
            }else{
                //拼接UniqueId
                //业务系统（id或code）+单据类型（id或code）+交易类型（id或code）+单据id+单据动作code+pubts
                String requestUniqueId = ICmpConstant.APPCODE +  bills[i].get("serviceCode").toString() + bills[i].get("requsetbilltradetype").toString() +
                        bills[i].get("requsetbillid").toString() + bills[i].get("requsetbillaction").toString()
                        + bills[i].get("addOrReduce").toString() + bills[i].get("requsetbillpubts").toString()
                        + bills[i].get("action").toString();
                controlDetailVO.setRequestUniqueId(requestUniqueId);
            }
            controlDetailVO.setAction(action);//动作类型
            /**
             * 预算控制执行开启-业务参数-[临时]预算转移特殊逻辑(仅指定项目才需开启)=是
             * 如付款申请单与付款单均占用预算，且只占一份预算需转移是使用，目前现金管理无此类逻辑，domainRelease设置为true走旧逻辑
             */
            controlDetailVO.setServiceCode(bills[i].get("serviceCode").toString());
            controlDetailVO.setDomainRelease(true);
            retVos[i] = controlDetailVO;
        }
        return retVos;
    }

    //预占校验结果返回
    @Override
    public String doCheckResult(CtmJSONObject resultBack, CtmJSONObject result) {
        if (CommonConstant.SC_OK.equals(result.getString(ICmpConstant.CODE))) {
            CtmJSONObject data = result.getJSONObject("data");
            CtmJSONArray billInfos = data.getJSONArray("billInfo");
            CtmJSONArray matchInfos = data.getJSONArray("matchInfo");
            boolean isSuccess = false;
            if (billInfos != null) {
                // isMatch为是否匹配上方案（true为是，false为否），isOffset为是否冲抵单据（true为是，false为否）
                //大于1 笔 有预支、执行
                for (int i = 0; i < billInfos.size(); i++) {
                    CtmJSONObject billInfo = billInfos.getJSONObject(i);
                    boolean isMatch = billInfo.getBooleanValue("isMatch");
                    if (isMatch) {
                        isSuccess = true;
                    }
                }
            }
            if (!isSuccess) {//所有数据无匹配
                resultBack.put(ICmpConstant.CODE, true);
                resultBack.put("message", InternationalUtils.getMessageWithDefault("UID:P_GRM-BE_18113A5204B801C3", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400809", "无匹配") /* "无匹配" */) /* "无匹配" */);
                return ResultMessage.data(resultBack);
            }
            if (matchInfos != null) {
                boolean isHashWarning = false;
                boolean isHasForce = false;
                StringBuffer sb = new StringBuffer();
                StringBuffer errsb = new StringBuffer();
                for (int ii = 0; ii < matchInfos.size(); ii++) {
                    CtmJSONObject matchInfo = matchInfos.getJSONObject(ii);
                    //控制类型（0:正常更新预占/执行数；1：强控超预算；2：预警控制超预算；3：超预警提示）
                    Integer ctrlType = matchInfo.getInteger("ctrlType");
                    if (ctrlType != null && (ctrlType == 1)) {
                        //预算系统返回的提示信息
                        // throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100389"),matchInfo.with("waning").asText());
                        errsb.append(matchInfo.getString("warning") + "\n");
                        isHasForce = true;
                    } else if (ctrlType != null && (ctrlType == 3 || ctrlType == 2)) {
                        //预算系统返回的提示信息
                        sb.append(matchInfo.getString("warning") + "\n");
                        isHashWarning = true;
                    }
                }
                if(isHasForce){
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100390"), errsb.toString());
                }
                if (isHashWarning) {
                    resultBack.put(ICmpConstant.CODE, false);
                    resultBack.put("message", sb.toString());
                    return ResultMessage.data(resultBack);
                } else {
                    resultBack.put(ICmpConstant.CODE, true);
                }
            }
        } else {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100343"),result.getString("message"));
        }
        return null;
    }

    /**
     * isMatch为true且ctrlType!=1时，代表占用成功
     * isMatch为是否匹配上方案（true为是，false为否）
     *
     * @param result
     * @return
     */
    @Override
    public ResultBudget doResult(CtmJSONObject result) {
        if (CommonConstant.SC_OK.equals(result.getString("code"))) {
            CtmJSONObject data = result.getJSONObject("data");
            CtmJSONArray billInfos = data.getJSONArray("billInfo");
            CtmJSONArray matchInfos = data.getJSONArray("matchInfo");
            if (CollectionUtils.isEmpty(billInfos)) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100345"),result.getString("message"));
            }
            if (billInfos.size() > 1) {
                List<String> ids = new ArrayList();
                //大于1笔
                for (int i = 0; i < billInfos.size(); i++) {
                    Map billInfo = (Map) billInfos.get(i);
                    boolean isMatch = (Boolean) billInfo.get("isMatch");
                    if (matchInfos == null) {
                        return new ResultBudget(OccupyBudget.UnOccupy.getValue(), false);
                    }
                    if (isMatch) {
                        String detaiId = StringUtils.isNotEmpty(billInfo.get("lineRequestUniqueId").toString())?billInfo.get("lineRequestUniqueId").toString():billInfo.get("lineNo").toString();
                        ids.add(detaiId);
                        // 仅抛错，判断ctrlType==1
                        doMatchInfos(matchInfos);
                    }
                }
                return new ResultBudget(OccupyBudget.PreSuccess.getValue(), true, ids);
            } else {
                Map billInfo = (Map) billInfos.get(0);
                boolean isMatch = (Boolean) billInfo.get("isMatch");
                if (isMatch) {
//                    // 先判断是否isMatch，没匹配上matchInfos为空
//                    Integer ctrlType = matchInfos.getJSONObject(0).getInteger("ctrlType");
//                    /**
//                     *  控制类型
//                     *  0:正常更新预占/执行数；1：强控超预算（刚性规则时超控制百分比，此时为强控下超预算，一般需要终止制单操作）；2：超预算（预警规则时超控制百分比）；3：超提示（刚性规则时或预警规则时超提示百分比）
//                     */
//                    if (ctrlType != null && ctrlType == 1) {
//                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100385"), matchInfos.getJSONObject(0).getString("warning"));
//                    } else {
//                        // 为0则直接表示成功
//                        return new ResultBudget(OccupyBudget.PreSuccess.getValue(), true);
//                    }
                    // 仅抛错，判断ctrlType==1
                    String msg = getMatchInfosMsg(matchInfos);
                    if(StringUtils.isNotEmpty( msg)){
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100385"), msg);
                    }
                    // 若无抛错，则返回成功
                    return new ResultBudget(OccupyBudget.PreSuccess.getValue(), true);
                } else {
                    // 未匹配上，直接返回失败
                    return new ResultBudget(OccupyBudget.UnOccupy.getValue(), false);
                }
            }
        } else {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100345"),result.getString("message"));
        }
    }

    private String getMatchInfosMsg(CtmJSONArray matchInfos) {
        StringBuilder sb = new StringBuilder();
        if (matchInfos != null && matchInfos.size() > 0) {
            //控制类型（0:正常更新预占/执行数；1：强控超预算；2：预警控制超预算；3：超预警提示）
            for (int i = 0; i < matchInfos.size(); i++) {
                CtmJSONObject matchInfo = matchInfos.getJSONObject(i);
                Integer ctrlType = matchInfo.getInteger("ctrlType");
                if (ctrlType != null && (ctrlType == 1)) {
                    //预算系统返回的提示信息
                    sb.append(matchInfo.getString("warning")).append("\r\n");
                }
            }
        }
        return sb.toString();
    }

    /**
     * isMatch为true且ctrlType!=1时，代表占用成功
     * isMatch为是否匹配上方案（true为是，false为否）
     *
     * @param result
     * @return
     */
    public ResultBudget doResultSoft(CtmJSONObject result) {
        if (CommonConstant.SC_OK.equals(result.getString("code"))) {
            CtmJSONObject data = result.getJSONObject("data");
            CtmJSONArray billInfos = data.getJSONArray("billInfo");
            CtmJSONArray matchInfos = data.getJSONArray("matchInfo");
            if (billInfos.size() > 1) {
                List<String> ids = new ArrayList();
                //大于1笔
                for (int i = 0; i < billInfos.size(); i++) {
                    Map billInfo = (Map) billInfos.get(i);
                    boolean isMatch = (Boolean) billInfo.get("isMatch");
                    if (matchInfos == null) {
                        return new ResultBudget(OccupyBudget.UnOccupy.getValue(), false);
                    }
                    if (isMatch) {
                        ids.add(billInfo.get("billDetailId").toString());
                        // 仅抛错，判断ctrlType==1
                        if (!doMatchInfosSoft(matchInfos)) {
                            return new ResultBudget(OccupyBudget.UnOccupy.getValue(), false);
                        }

                    }
                }
                return new ResultBudget(OccupyBudget.PreSuccess.getValue(), true, ids);
            } else {
                Map billInfo = (Map) billInfos.get(0);
                boolean isMatch = (Boolean) billInfo.get("isMatch");
                if (isMatch) {
//                    // 先判断是否isMatch，没匹配上matchInfos为空
//                    Integer ctrlType = matchInfos.getJSONObject(0).getInteger("ctrlType");
//                    /**
//                     *  控制类型
//                     *  0:正常更新预占/执行数；1：强控超预算（刚性规则时超控制百分比，此时为强控下超预算，一般需要终止制单操作）；2：超预算（预警规则时超控制百分比）；3：超提示（刚性规则时或预警规则时超提示百分比）
//                     */
//                    if (ctrlType != null && ctrlType == 1) {
//                        log.error("预算系统返回的提示信息: {}", matchInfos.getJSONObject(0).getString("warning"));
//                        return new ResultBudget(OccupyBudget.PreSuccess.getValue(), false);
//                    } else {
//                        // 为0则直接表示成功
//                        return new ResultBudget(OccupyBudget.PreSuccess.getValue(), true);
//                    }
                    String msg = getMatchInfosMsg(matchInfos);
                    if(StringUtils.isNotEmpty( msg)){
                        log.error("预算系统返回的提示信息: {}", matchInfos.getJSONObject(0).getString("warning"));
                        return new ResultBudget(OccupyBudget.PreSuccess.getValue(), false);
                    }
                    // 若无抛错，则返回成功
                    return new ResultBudget(OccupyBudget.PreSuccess.getValue(), true);
                } else {
                    // 未匹配上，直接返回失败
                    return new ResultBudget(OccupyBudget.UnOccupy.getValue(), false);
                }
            }
        } else {
            log.error("预算系统返回的结果: {}", result.getString("message"));
            return new ResultBudget(OccupyBudget.UnOccupy.getValue(), false);
        }
    }

    /**
     * 匹配 规则信息
     * 拼接预算报错提示语
     *
     * @param matchInfos
     * @return
     */
    @Override
    public void doMatchInfos(CtmJSONArray matchInfos) {
        if (matchInfos != null && matchInfos.size() > 0) {
            //控制类型（0:正常更新预占/执行数；1：强控超预算；2：预警控制超预算；3：超预警提示）
            for (int i = 0; i < matchInfos.size(); i++) {
                CtmJSONObject matchInfo = matchInfos.getJSONObject(i);
                Integer ctrlType = matchInfo.getInteger("ctrlType");
                if (ctrlType != null && (ctrlType == 1)) {
                    //预算系统返回的提示信息
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100346"),matchInfo.getString("warning"));
                }
            }
        }
    }

    /**
     * 匹配 规则信息
     * 拼接预算报错提示语
     *
     * @param matchInfos
     * @return
     */
    public boolean doMatchInfosSoft(CtmJSONArray matchInfos) {
        if (matchInfos != null && matchInfos.size() > 0) {
            //控制类型（0:正常更新预占/执行数；1：强控超预算；2：预警控制超预算；3：超预警提示）
            for (int i = 0; i < matchInfos.size(); i++) {
                CtmJSONObject matchInfo = matchInfos.getJSONObject(i);
                Integer ctrlType = matchInfo.getInteger("ctrlType");
                if (ctrlType != null && (ctrlType == 1)) {
                    //预算系统返回的提示信息
                    log.error("预算系统返回的提示信息: {}", matchInfo.getString("warning"));
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public CtmJSONObject toJsonObj(Map<String, Object> map) {
        CtmJSONObject resultJson = new CtmJSONObject();
        Iterator it = map.keySet().iterator();
        while (it.hasNext()) {
            String key = (String) it.next();
            resultJson.put(key, map.get(key));
        }
        return resultJson;
    }
}
