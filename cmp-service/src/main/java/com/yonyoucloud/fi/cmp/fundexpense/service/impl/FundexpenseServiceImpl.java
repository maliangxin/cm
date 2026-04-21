package com.yonyoucloud.fi.cmp.fundexpense.service.impl;

import com.yonyou.cloud.utils.CollectionUtils;
import com.yonyou.epmp.busi.service.BusiSystemConfigService;
import com.yonyou.epmp.control.dto.ControlDetailVO;
import com.yonyou.epmp.control.service.ExecdataControlService;
import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.iuap.ucf.common.i18n.InternationalUtils;
import com.yonyou.iuap.yms.id.generator.YmsOidGenerator;
import com.yonyou.ucf.basedoc.model.ExchangeRateWithMode;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetCommonManagerService;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.budget.BudgetDirect;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetManagerService;
import com.yonyoucloud.fi.cmp.cmpentity.OccupyBudget;
import com.yonyoucloud.fi.cmp.common.service.CmCommonService;
import com.yonyoucloud.fi.cmp.common.service.exchangerate.CmpExchangeRateUtils;
import com.yonyoucloud.fi.cmp.common.service.exchangerate.CmpExchangeRateVO;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import com.yonyoucloud.fi.cmp.event.vo.ResultBudget;
import com.yonyoucloud.fi.cmp.fundexpense.Fundexpense_b;
import com.yonyoucloud.fi.cmp.fundexpense.service.FundexpenseService;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.constants.CommonConstant;
import com.yonyoucloud.fi.cmp.util.BillAction;
import com.yonyoucloud.fi.cmp.util.BudgetUtils;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.imeta.orm.base.BizObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.*;

@Slf4j
@Service
@Transactional(rollbackFor = RuntimeException.class)
public class FundexpenseServiceImpl implements FundexpenseService {
    // 预占
    public static final String PRE = "pre";
    // 实占
    public static final String IMPLEMENT = "implement";
    public static final String CHARACTERDEF = "characterDef_";
    public static final String CHARACTERDEFB = "characterDefb_";
    public static final String FUNDEXPENSE_B = "Fundexpense_b__";


    @Resource
    private BusiSystemConfigService busiSystemConfigService;
    @Autowired
    private YmsOidGenerator ymsOidGenerator;
    @Autowired
    private ExecdataControlService execdataControlService;
    @Autowired
    private CmCommonService cmCommonService;
    @Autowired
    private CmpBudgetCommonManagerService cmpBudgetCommonManagerService;

    @Override
    public CtmJSONObject queryExchangeRate(CtmJSONObject param) throws Exception {
        CtmJSONObject result = new CtmJSONObject();
        BigDecimal exchRate = BigDecimal.ZERO;
        String expensenatCurrency = param.getString("expensenatCurrency");
        String natCurrency = param.getString("natCurrency");
        if (!natCurrency.equals(expensenatCurrency) && param.getString("exchangeRateType") != null && param.getDate("expensedate") != null) {
            String exchangeRateType = param.getString("exchangeRateType");
            Date expensedate = param.getDate("expensedate");
            CmpExchangeRateVO cmpExchangeRateVO = CmpExchangeRateUtils.getNewExchangeRateWithMode(expensenatCurrency, natCurrency, expensedate,exchangeRateType);
            exchRate = cmpExchangeRateVO.getExchangeRate();
        }
        result.put("exchRate", exchRate);
        return result;
    }

    @Override
    public ResultBudget fundCollectionEmployActualOccupySuccessAudit(BizObject bizObject,  Fundexpense_b fundexpense_b, String billCode, String billAction) {
        try {
            String preAction;
            String implementAction;
            // 查询接口
            List<Map> objects = (List<Map>) busiSystemConfigService.getBillAction(InvocationInfoProxy.getTenantid(), BudgetUtils.SYSCODE, billCode);
            //获取平台配置
            if(CollectionUtils.isNotEmpty(objects)){
                implementAction = (String) objects.get(0).get("implement");
            }else {
                // 不能直接赋值implement，得看客户是否配置了实占规则，如果没配置需要直接返回
                log.error("getBillAction ","not support budget implement! ");
                return new ResultBudget(OccupyBudget.UnOccupy.getValue(),false);
            }
            preAction = (String) objects.get(0).get(PRE);
            // 未配置预占动作，则无需释放预占，直接实占
            ControlDetailVO[] controlDetailVOS;
            ControlDetailVO controlDetailVPre ;
            ControlDetailVO controlDetailVImplement ;
            if (!StringUtils.isEmpty(preAction)) {
                Map preBill = initFundCollectionBill(bizObject,fundexpense_b,BudgetDirect.REDUCE.getIndex(),billCode,PRE, BillAction.APPROVE_PASS);
                controlDetailVPre = cmpBudgetCommonManagerService.initControlDetailVO(preBill,preAction,0);
                log.error("预算实占释放预占请求报文： " + controlDetailVPre);
                Map implementBill = initFundCollectionBill(bizObject,fundexpense_b,BudgetDirect.ADD.getIndex(),billCode,IMPLEMENT,BillAction.APPROVE_PASS);
                controlDetailVImplement = cmpBudgetCommonManagerService.initControlDetailVO(implementBill,implementAction,0);
                log.error("预算实占请求报文： " + controlDetailVImplement);
                controlDetailVOS = new ControlDetailVO[]{controlDetailVPre,controlDetailVImplement};
            } else {
                controlDetailVOS = new ControlDetailVO[1];
                Map implementBill = initFundCollectionBill(bizObject,fundexpense_b,BudgetDirect.ADD.getIndex(),billCode,IMPLEMENT,BillAction.APPROVE_PASS);
                ControlDetailVO controlDetailVO = cmpBudgetCommonManagerService.initControlDetailVO(implementBill, implementAction, 0);
                controlDetailVOS[0] = controlDetailVO;
                log.error("预算实占请求报文： " + controlDetailVOS[0]);
            }
            // 控制接口(预占、实占)
            CtmJSONObject result = new CtmJSONObject(execdataControlService.control(controlDetailVOS));
            log.error("预算实占响应报文： ",CtmJSONObject.toJSONString(result));
            return doResult(result);
        }catch (Exception e){
            log.error("execute ",e);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100234"),InternationalUtils.getMessageWithDefault("UID:P_GRM-BE_18113A5204B801BF",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18AD061204380011","预算系统返回:") /* "预算系统返回:" */) /* "预算系统返回:" */+e.getMessage());
        }
    }

    /**
     * 报文拼接
     * @param bizObject
     * @param addOrReduce 撤回需要删除
     * @param billCode
     */
    private Map<String, Object> initFundCollectionBill(BizObject bizObject, Fundexpense_b fundexpense_b, int addOrReduce, String billCode, String preemptionOrExecFlag, String billAction) throws Exception{
        Map<String, Object> jo = new HashMap<>();
        jo.putAll(bizObject);
        //初始化
        jo.put("signature", null);// 签名字段不传，且包含特殊字符可能被预算中台拦截
        jo.put("serviceCode", IServicecodeConstant.FUNDCOLLECTION);
        jo.put("billId",ValueUtils.isNotEmptyObj(bizObject.getId()) ? bizObject.getId() : ymsOidGenerator.nextId());//单据id
        jo.put("billNo",bizObject.getString("code"));//单据编号
        jo.put("billCode",billCode);//单据类型唯一标识
        jo.put("action", billAction);//动作
        jo.put("requsetbilltradetype", bizObject.get("tradetype").toString());//交易类型，用来拼接unquieRequestId
        jo.put("requsetbillaction", preemptionOrExecFlag);//预占或实占动作，用来拼接unquieRequestId
        jo.put("requsetbillid", ValueUtils.isNotEmptyObj(fundexpense_b.getId()) ? fundexpense_b.getId().toString() : ymsOidGenerator.nextId());//id，用来拼接unquieRequestId
        jo.put("requsetbillpubts", ValueUtils.isNotEmptyObj(bizObject.get("pubts")) ?bizObject.get("pubts").toString() : ymsOidGenerator.nextId());//时间戳，用来拼接unquieRequestId
        jo.put("transacCode",cmCommonService.getDefaultTransTypeCode(bizObject.getString("tradetype")));//交易类型唯一标识
        jo.put("addOrReduce",addOrReduce);//1-删除,0-新增
        jo.put("lineNo", ValueUtils.isNotEmptyObj(fundexpense_b.get("lineno"))
                ? fundexpense_b.get("lineno") instanceof BigDecimal ? ((BigDecimal)fundexpense_b.get("lineno")).intValue() : fundexpense_b.get("lineno").toString()
                : ymsOidGenerator.nextId());//单据行id 默认1
        jo.put("lineRequestUniqueId", ValueUtils.isNotEmptyObj(fundexpense_b.getId())
                ? fundexpense_b.getId()
                : ymsOidGenerator.nextId());//单据行id 默认1
        // 特征字段拼接 例：characterDef_test0919
        jo.put("billLineId", fundexpense_b.getId());
        Map characterDefMap = bizObject.get("characterDef");
        if (characterDefMap != null && !characterDefMap.isEmpty()) {
            characterDefMap.forEach((key, value) -> jo.put(CHARACTERDEF + key, value));
        }
        // 是否冲抵（逆向）单据，true为是冲抵（逆向）单据，false为不是冲抵（逆向）单据，默认为false
        jo.put("isOffset", addOrReduce==1?true:false);
        if(PRE.equals(preemptionOrExecFlag)){
            //预占
            jo.put("preemptionOrExecFlag",0);//单据编号
        }else{
            //执行
            jo.put("preemptionOrExecFlag",1);//单据编号
        }
        if (fundexpense_b != null) {
            CtmJSONObject jsonObject_b = toJsonObj(fundexpense_b);
            jsonObject_b.entrySet().stream().forEach(e -> {
                // 签名字段不用传预算，且包含特殊字符已被预算拦截校验
                if (!e.getKey().contains("signature")) {
                    jo.put(FUNDEXPENSE_B+e.getKey(), e.getValue());
                }
            });
            // FundPayment_b__characterDef_test0919
            if (fundexpense_b.get("characterDefb") instanceof Map) {
                Map subCharacterDefMap = fundexpense_b.get("characterDefb");
                if (subCharacterDefMap != null && !subCharacterDefMap.isEmpty()) {
                    subCharacterDefMap.forEach((key, value) -> jo.put(FUNDEXPENSE_B + CHARACTERDEFB + key, value));
                }
            }
        }
        return jo;
    }

    @Override
    public ResultBudget gcExecuteBatchSubmit(BizObject bizObject, List<Fundexpense_b> fundexpense_bs, String billCode, String billAction) throws Exception {
        try {
            // 审批流提交动作匹配预算动作
            String action;
            // 查询接口
            List<Map> objects = (List<Map>) busiSystemConfigService.getBillAction(InvocationInfoProxy.getTenantid(), BudgetUtils.SYSCODE, billCode);
            //获取平台配置
            if (CollectionUtils.isNotEmpty(objects)) {
                action = (String) objects.get(0).get(PRE);
            } else {
                log.error("getBillAction ", "not support budget pre ");
                return new ResultBudget(OccupyBudget.UnOccupy.getValue(), false);
            }
            String preemptionOrExecFlag = PRE;
            if (preemptionOrExecFlag == null) {
                log.error("getBillAction ", "not support budget pre ");
                return new ResultBudget(OccupyBudget.UnOccupy.getValue(), false);
            }
            // 拼装报文
            int size = fundexpense_bs.size();
            Map<String, Object>[] bills = new Map[size];
            for (int i = 0; i < size; i++) {
                Map copiedMap = new HashMap<>(initFundPaymentBill(bizObject, fundexpense_bs.get(i), BudgetDirect.ADD.getIndex(), billCode, preemptionOrExecFlag, billAction));
                bills[i] = copiedMap;
            }
            ControlDetailVO[] controlDetailVOS = cmpBudgetCommonManagerService.initControlDetailVOs(bills, action, 0);
            // 控制接口(预占、实占)
            log.error("预算预占请求报文：" + controlDetailVOS);
            CtmJSONObject result = new CtmJSONObject(execdataControlService.control(controlDetailVOS));
            log.error("预算预占响应报文 ", CtmJSONObject.toJSONString(result));
            return doResult(result);
        } catch (Exception e) {
            log.error("execute ", e);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100234"),InternationalUtils.getMessageWithDefault("UID:P_GRM-BE_18113A5204B801BF", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18AD061204380011", "预算系统返回:") /* "预算系统返回:" */) /* "预算系统返回:" */ + e.getMessage());
        }
    }

    @Override
    public ResultBudget gcExecuteBatchUnSubmit(BizObject bizObject, List<Fundexpense_b> fundexpense_bs, String billCode, String billAction) throws Exception {
        try {
            // 审批流提交动作匹配预算动作
            String action;
            // 查询接口
            List<Map> objects = (List<Map>) busiSystemConfigService.getBillAction(InvocationInfoProxy.getTenantid(), BudgetUtils.SYSCODE, billCode);
            //获取平台配置
            if(CollectionUtils.isNotEmpty(objects)){
                action = (String) objects.get(0).get(PRE);
            }else {
                log.error("getBillAction ","not support budget pre ");
                return new ResultBudget(OccupyBudget.UnOccupy.getValue(),false);
            }
            String preemptionOrExecFlag = PRE;
            if(preemptionOrExecFlag==null){
                log.error("getBillAction ","not support budget pre ");
                return new ResultBudget(OccupyBudget.UnOccupy.getValue(),false);
            }
            // 拼装报文
            int size = fundexpense_bs.size();
            Map<String, Object>[] bills = new Map[size];
            for (int i = 0; i < size; i++) {
                Map copiedMap = new HashMap<>(initFundPaymentBill(bizObject,fundexpense_bs.get(i),BudgetDirect.ADD.getIndex(), billCode,preemptionOrExecFlag,billAction));
                bills[i] = copiedMap;
            }
            ControlDetailVO[] controlDetailVOS = cmpBudgetCommonManagerService.initControlDetailVOs(bills, action, 0);
            // 控制接口(预占、实占)
            log.error("预算预占请求报文：" + controlDetailVOS[0]);
            CtmJSONObject result = new CtmJSONObject(execdataControlService.control(controlDetailVOS));
            log.error("预算预占响应报文 ",CtmJSONObject.toJSONString(result));
            return doResult(result);
        }catch (Exception e){
            log.error("execute ",e);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100234"),InternationalUtils.getMessageWithDefault("UID:P_GRM-BE_18113A5204B801BF",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18AD061204380011","预算系统返回:") /* "预算系统返回:" */) /* "预算系统返回:" */+e.getMessage());
        }
    }


    /**
     * *
     *
     * @param bills
     */
//    private ControlDetailVO cmpBudgetCommonManagerService.initControlDetailVO(Map[] bills, String action, int operateFlag) throws Exception {
//        ControlDetailVO controlDetailVO = new ControlDetailVO();
//        //担保变更预占
//        controlDetailVO.setBills(bills);//业务单据数组
//        controlDetailVO.setBusiSysCode(BudgetUtils.SYSCODE);
//        controlDetailVO.setYtenantId(InvocationInfoProxy.getTenantid());
//        //请求唯一标识 请求唯一标识，同一标识数据幂等处理，防止重复提交数据。同时 数据处理完之后返回消息中会携带返回此标识，可明确成功数据。  业务系统（id或code）+单据类型（id或code）+交易类型（id或code）+单据id+单据动作code+pubts
//        //TODO 拼接UniqueId
//        controlDetailVO.setRequestUniqueId(UUID.randomUUID().toString());
//        controlDetailVO.setOperateFlag(operateFlag);//0代表控制并记录数据，1代表记录数据不控制，2控制不记录数据
////        controlDetailVO.setLedgerFlag(1);//ledgerFlag，0代表不记录台账，1代表记录台账,默认为0，如果需要保存台账相关信息，可以传1
//        controlDetailVO.setAction(action);//动作类型
//        /**
//         * 预算控制执行开启-业务参数-[临时]预算转移特殊逻辑(仅指定项目才需开启)=是
//         * 如付款申请单与付款单均占用预算，且只占一份预算需转移是使用，目前现金管理无此类逻辑，domainRelease设置为true走旧逻辑
//         */
//        controlDetailVO.setServiceCode(IServicecodeConstant.FUNDPAYMENT);
//        controlDetailVO.setDomainRelease(true);
//        return controlDetailVO;
//
//    }

    /**
     * 报文拼接
     *
     * @param bizObject
     * @param addOrReduce 撤回需要删除
     * @param billCode
     */
    private Map<String, Object> initFundPaymentBill(BizObject bizObject, Fundexpense_b fundexpense_b, int addOrReduce, String billCode, String preemptionOrExecFlag, String billAction) throws Exception {
        Map<String, Object> jo = new HashMap<>();
        jo.putAll(bizObject);
        //初始化
        jo.put("signature", null);// 签名字段不传，且包含特殊字符可能被预算中台拦截
        jo.put("serviceCode", IServicecodeConstant.FUNDPAYMENT);
        jo.put("billId", ValueUtils.isNotEmptyObj(bizObject.getId()) ? bizObject.getId() : ymsOidGenerator.nextId());//单据id
        jo.put("billNo", bizObject.getString("code"));//单据编号
        jo.put("billCode", billCode);//单据类型唯一标识
        jo.put("action", billAction);//动作
        jo.put("requsetbilltradetype", bizObject.get("tradetype").toString());//交易类型，用来拼接unquieRequestId
        jo.put("requsetbillaction", preemptionOrExecFlag);//预占或实占动作，用来拼接unquieRequestId
        jo.put("requsetbillid", ValueUtils.isNotEmptyObj(fundexpense_b.getId()) ? fundexpense_b.getId().toString() : ymsOidGenerator.nextId());//id，用来拼接unquieRequestId
        jo.put("requsetbillpubts", ValueUtils.isNotEmptyObj(bizObject.get("pubts")) ?bizObject.get("pubts").toString() : ymsOidGenerator.nextId());//时间戳，用来拼接unquieRequestId
        jo.put("transacCode", cmCommonService.getDefaultTransTypeCode(bizObject.getString("tradetype")));//交易类型唯一标识
        jo.put("addOrReduce", addOrReduce);//1-删除,0-新增
        jo.put("lineNo", ValueUtils.isNotEmptyObj(fundexpense_b.getId())
                ? fundexpense_b.getId()
                : ymsOidGenerator.nextId());//单据行id 默认1
        jo.put("billLineId", fundexpense_b.getId());
        // 特征字段拼接 例：characterDef_test0919
        if (bizObject.get("characterDef") != null && bizObject.get("characterDef") instanceof Map) {
            Map characterDefMap = bizObject.get("characterDef");
            if (!characterDefMap.isEmpty()) {
                characterDefMap.forEach((key, value) -> jo.put(CHARACTERDEF + key, value));
            }
        }
        // 是否冲抵（逆向）单据，true为是冲抵（逆向）单据，false为不是冲抵（逆向）单据，默认为false
        jo.put("isOffset", addOrReduce == 1 ? true : false);
        if (PRE.equals(preemptionOrExecFlag)) {
            //预占
            jo.put("preemptionOrExecFlag", 0);//单据编号
        } else {
            //执行
            jo.put("preemptionOrExecFlag", 1);//单据编号
        }
        if (fundexpense_b != null) {
            CtmJSONObject jsonObject_b = toJsonObj(fundexpense_b);
            jsonObject_b.entrySet().stream().forEach(e -> {
                // 签名字段不用传预算，且包含特殊字符已被预算拦截校验
                if (!e.getKey().contains("signature")) {
                    jo.put(FUNDEXPENSE_B + e.getKey(), e.getValue());
                }
            });
            // FundPayment_b__characterDef_test0919
            if (fundexpense_b.get("characterDefb") instanceof Map) {
                Map subCharacterDefMap = fundexpense_b.get("characterDefb");
                if (subCharacterDefMap != null && !subCharacterDefMap.isEmpty()) {
                    subCharacterDefMap.forEach((key, value) -> jo.put(FUNDEXPENSE_B + CHARACTERDEFB + key, value));
                }
            }
        }
        return jo;
    }

    private CtmJSONObject toJsonObj(Map<String, Object> map) {
        CtmJSONObject resultJson = new CtmJSONObject();
        Iterator it = map.keySet().iterator();
        while (it.hasNext()) {
            String key = (String) it.next();
            resultJson.put(key, map.get(key));
        }
        return resultJson;
    }

    /**
     * isMatch为true且ctrlType!=1时，代表占用成功
     * isMatch为是否匹配上方案（true为是，false为否）
     *
     * @param result
     * @return
     */
    private ResultBudget doResult(CtmJSONObject result) {
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
//                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101233"),matchInfos.getJSONObject(0).getString("warning"));
//                    } else {
//                        // 为0则直接表示成功
//                        return new ResultBudget(OccupyBudget.PreSuccess.getValue(), true);
//                    }
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
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101234"),result.getString("message"));
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
     * 匹配 规则信息
     * 拼接预算报错提示语
     * @param matchInfos
     * @return
     */
    private void doMatchInfos(CtmJSONArray matchInfos){
        if (matchInfos != null && matchInfos.size() > 0) {
            //控制类型（0:正常更新预占/执行数；1：强控超预算；2：预警控制超预算；3：超预警提示）
            for(int i = 0;i<matchInfos.size();i++){
                CtmJSONObject matchInfo =  matchInfos.getJSONObject(i);
                Integer ctrlType = matchInfo.getInteger("ctrlType");
                if (ctrlType != null && (ctrlType == 1)) {
                    //预算系统返回的提示信息
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101235"),matchInfo.getString("warning"));
                }
            }
        }
    }

}
