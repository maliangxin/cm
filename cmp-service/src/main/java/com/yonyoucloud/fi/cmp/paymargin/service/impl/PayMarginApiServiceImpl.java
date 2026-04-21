package com.yonyoucloud.fi.cmp.paymargin.service.impl;

import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.iuap.org.dto.BizDeptDTO;
import com.yonyou.iuap.org.service.itf.core.IBizDeptQueryService;
import com.yonyou.ucf.basedoc.model.ExchangeRate;
import com.yonyou.ucf.basedoc.service.itf.IProjectService;
import com.yonyou.ucf.mdd.common.enums.OperationTypeEnum;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.api.service.ApiImportCommandService;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.CommonRuleUtils;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.exceptions.BusinessException;
import com.yonyou.ucf.mdd.ext.poi.importbiz.exception.TransferDataException;
import com.yonyou.ucf.transtype.model.BdTransType;
import com.yonyou.workbench.util.Lists;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.basecom.itf.IFIBillService;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.basecom.utils.FIDubboUtils;
import com.yonyoucloud.fi.cmp.cmpentity.*;
import com.yonyoucloud.fi.cmp.common.service.exchangerate.CmpExchangeRateUtils;
import com.yonyoucloud.fi.cmp.common.service.exchangerate.CmpExchangeRateVO;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.marginworkbench.MarginWorkbench;
import com.yonyoucloud.fi.cmp.paymargin.PayMargin;
import com.yonyoucloud.fi.cmp.paymargin.PayMarginCharacterDef;
import com.yonyoucloud.fi.cmp.paymargin.service.PayMarginApiService;
import com.yonyoucloud.fi.cmp.util.IdCreator;
import com.yonyoucloud.fi.cmp.util.basedoc.CustomerQueryService;
import com.yonyoucloud.fi.cmp.util.basedoc.TransTypeQueryService;
import com.yonyoucloud.fi.cmp.util.basedoc.VendorQueryService;
import com.yonyoucloud.iuap.upc.dto.AgentFinancialDTO;
import com.yonyoucloud.upc.pub.api.vendor.vo.vendor.VendorBankVO;
import org.apache.commons.lang3.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.imeta.biz.base.Objectlizer;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.yonyou.iuap.ucf.common.i18n.InternationalUtils;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
public class PayMarginApiServiceImpl implements PayMarginApiService {

    @Autowired
    TransTypeQueryService transTypeQueryService;

    public PayMarginApiServiceImpl(IFIBillService fiBillService,
                                   VendorQueryService vendorQueryService,
                                   BaseRefRpcService baseRefRpcService,IProjectService projectService,
                                   IBizDeptQueryService deptQueryService,CustomerQueryService customerQueryService){
        this.fiBillService = fiBillService;
        this.vendorQueryService = vendorQueryService;
        this.baseRefRpcService = baseRefRpcService;
        this.projectService = projectService;
        this.deptQueryService = deptQueryService;
        this.customerQueryService = customerQueryService;
    }

    // 交易类型编码
    private final static String WITHDRAW = "cmp_paymargin_withdraw";
    private final static String PAYMENT = "cmp_paymargin_payment";
    // 必填字段信息
    private final static Map<String, Function<PayMarginApiDto,Object>> NON_NULL_PROP = new HashMap<>();
    private final IFIBillService fiBillService;
    private final VendorQueryService vendorQueryService;
    private final BaseRefRpcService baseRefRpcService;
    private final IProjectService projectService;
    private final IBizDeptQueryService deptQueryService;
    private final CustomerQueryService customerQueryService;

    static {
        // 初始化必填字段信息
        NON_NULL_PROP.put("accentity",PayMarginApiDto::getAccentity_code);
        NON_NULL_PROP.put("vouchdate",PayMarginApiDto::getVouchdate);
        NON_NULL_PROP.put("tradetype_code",PayMarginApiDto::getTradetype_code);
        NON_NULL_PROP.put("marginbusinessno",PayMarginApiDto::getMarginbusinessno);
        NON_NULL_PROP.put("currency",PayMarginApiDto::getCurrency_code);
        NON_NULL_PROP.put("srcbillno",PayMarginApiDto::getSrcbillno);
        NON_NULL_PROP.put("oppositetype",PayMarginApiDto::getOppositetype);
        NON_NULL_PROP.put("marginamount",PayMarginApiDto::getMarginamount);
        NON_NULL_PROP.put("settleflag",PayMarginApiDto::getSettleflag);
    }

    @Override
    public Map<String, Object> saveBill(PayMarginApiDto param){
        validNonNull(param);// 非空校验
        validFormat(param);// 格式校验
        validBusiness(param);// 业务校验
        BillDataDto dto = convert(param);// 转换为DTO
        // 执行 save action
        PayMargin payMargin = executeSave(dto);
        // 返回结果
        Map<String,Object> resultMap = new HashMap<>();
        resultMap.put("id", payMargin.getId());
        resultMap.put("code", payMargin.getCode());
        resultMap.put("marginvirtualaccount",payMargin.getMarginvirtualaccount());
        return resultMap;
    }

    @Override
    public List<String> deleteBill(CtmJSONObject param){
        // 解析参数
        List<String> srcIds = extractIds(param);
        // 查询
        List<PayMargin> list = queryBySrcId(srcIds);
        Map<String, PayMargin> map = list.stream().collect(Collectors.toMap(PayMargin::getSrcbillno, x -> x));
        // 校验：是否存在，单据状态
        Map<String, Long> idMap = checkBeforeDelete(srcIds,map);
        BillDataDto billDataDto = convertDeleteDto(srcIds, idMap);
        // 执行删除
        RuleExecuteResult result = null;
        try {
            result = fiBillService.executeUpdate(OperationTypeEnum.DELETE.getValue(), billDataDto);
        } catch (Exception e) {
            log.error("PayMargin deleteBill error", e);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100909"),String.format("delete bill fail,message:%s",e.getMessage()));
        }
        if(result.getMsgCode() != 1){// 未正常删除
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100910"),"unexpected delete fail");
        }
        return srcIds;
    }

    @Override
    public void beforeSaveForOpenApi(PayMargin entity) {
        // 只处理新增数据的场景
        if(entity.getEntityStatus() != EntityStatus.Insert){
            return;
        }
        translateSupplyBankBySupply(entity);// 翻译供应商银行字段
        checkCustomer(entity);// 校验客户
        String accentity = entity.getAccentity();
        // 校验部门权限,如果没有权限则对应字段不存储
        if(!checkDeptRange(entity.getDept(),accentity)){
            entity.setDept(null);
        }
        if(!checkDeptRange(entity.getNewdept(),accentity)){
            entity.setNewdept(null);
        }
        // 校验项目权限，如果没有对应项目的权限则对应字段不存储
        if(!checkProjectRange(entity.getProject(), accentity)){
            entity.setProject(null);
        }
        if(!checkProjectRange(entity.getNewproject(), accentity)){
            entity.setNewproject(null);
        }
    }

    /**
     * 判断部门id是否符合权限范围,与页面参照过滤权限保持一致
     * @param deptId 部门Id
     * @param orgId 资金组织id
     * @return 是否在权限范围内
     * @see com.yonyoucloud.fi.cmp.auth.filter.DeptAuthFilter
     **/
    public boolean checkDeptRange(String deptId,String orgId) {
        if(StringUtils.isEmpty(deptId)){
            return true;
        }
        // 先查询部门ID直接所属的组织Id
        String parentOrgId = findOrgId(deptId);
        if(orgId.equals(parentOrgId)){
            return true;// 匹配到则直接返回
        }
        try{
            // 根据委托关系查找组织
            Set<String> orgIds = FIDubboUtils.getDelegateHasSelf(orgId) ;
            if(orgIds.contains(parentOrgId)){
                return true;// 如果部门在委托组织下则返回
            }
            //根据职能共享查找部门
            Set<String> shardDeptIds = FIDubboUtils.getDeptShare(orgIds.toArray(new String[0])) ;
            if(shardDeptIds.contains(deptId)){
                return true;// 如果共享部门可以匹配到则返回
            }
            //根据职能共享查找组织，并查询组织下所有部门
            Set<String> allOrgIds = FIDubboUtils.getOrgShareHasSelf(orgIds.toArray(new String[0]));
            return allOrgIds.contains(parentOrgId);// 判断是否在共享组织下
        }catch (Exception e){
            log.error("checkDeptRange error", e);
        }
        return false;
    }

    /**
     * 查询部门所属的组织id: 逐级查询上级部门，直到查询到第一个组织类型节点或者超过最大迭代次数
     */
    private String findOrgId(String deptId){
        String tenantId = InvocationInfoProxy.getTenantid();
        String currId = deptId;
        int time = 0;
        while(time < 6){
            BizDeptDTO deptDto = deptQueryService.getById(tenantId, currId);
            if(deptDto.getOrgtype() == 1){ // 1 组织, 2 部门
                return currId;
            }else{
                currId = deptDto.getParentorgid();
            }
            time++;
        }
        return currId;
    }

    /**
     * 判断给定组织是否有对应项目的权限
     * @param projectId 项目id
     * @param orgId 租户id
     * @return 是否有权限
     */
    private boolean checkProjectRange(String projectId,String orgId){
        if(StringUtils.isEmpty(projectId)){
            return true;
        }
        Map<String, Set<String>> map = null;
        try {
            map = projectService.queryOrgRangeSByProjectId(Lists.newArrayList(projectId));
        } catch (Exception e) {
            log.error("checkProjectRange fail,",e);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100911"),InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D98D0BE05E0004D", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D98D0BE05E0004D", "查询项目权限信息失败:") /* "查询项目权限信息失败:" */) /* "查询项目权限信息失败:" */ + e.getMessage());
        }
        Set<String> org1 = map.getOrDefault(projectId, Collections.emptySet());
        return org1.contains(orgId);
    }

    /**
     * 校验客户银行账户和客户是否匹配
     * @param entity 请求信息
     */
    private void checkCustomer(PayMargin entity){
        Long customer = entity.getCustomer();
        Long bankAccount = entity.getCustomerbankaccount();
        if(customer == null || bankAccount == null){
            return;
        }
        AgentFinancialDTO dto = null;
        try {
            dto = customerQueryService.getCustomerAccountByAccountId(bankAccount);
        } catch (Exception e) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100912"),e.getMessage(),e);
        }
        if(!dto.getMerchantId().equals(customer)){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100913"),InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1DABF73C0590000E", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1DABF73C0590000E", "客户银行账号未找到") /* "客户银行账号未找到" */));
        }
    }

    private void translateSupplyBankBySupply(PayMargin entity){
        if(MarginOppositeType.find(entity.getOppositetype()) != MarginOppositeType.Supplier) {
            return;
        }
        String bankAccountCode = entity.get("supplierbankaccount_code");
        Long supplier = entity.getSupplier();
        if (supplier == null || StringUtils.isEmpty(bankAccountCode)) {
            return;
        }
        try {
            // 先按账户账号查找
            Map<String, Object> condition = new HashMap<>();
            condition.put("vendor", supplier);
            condition.put("account", bankAccountCode);
            List<VendorBankVO> list = vendorQueryService.getVendorBanksByCondition(condition);
            if (list.isEmpty()) {
                // 按id格式再查一次
                VendorBankVO supplierbankaccount = vendorQueryService.getVendorBanksByAccountId(bankAccountCode);
                if(supplierbankaccount != null){
                    entity.setSupplierbankaccount(supplierbankaccount.getId());
                }else{
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100914"),InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D98D0BE05E00044", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D98D0BE05E00044", "未匹配到对应的供应商银行账户:") /* "未匹配到对应的供应商银行账户:" */) /* "未匹配到对应的供应商银行账户:" */ + bankAccountCode);
                }
            }else if (list.size() > 1) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100915"),InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D98D0BE05E00045", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D98D0BE05E00045", "匹配到多个供应商银行账户:") /* "匹配到多个供应商银行账户:" */) /* "匹配到多个供应商银行账户:" */ + bankAccountCode);
            }else{
                entity.setSupplierbankaccount(list.get(0).getId());
            }
        } catch (Exception e) {
            log.error("translateSupplyBankBySupply error", e);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100916"),InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D98D0BE05E00047", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D98D0BE05E00047", "供应商银行账户翻译出错") /* "供应商银行账户翻译出错" */) /* "供应商银行账户翻译出错" */,e);
        }
    }

    /**
     * 计算汇率，如果失败则抛异常。在通过OpenApi场景下导入时使用
     * @param payMargin 请求实体
     * @return 利率
     */
    @Override
    public CtmJSONObject getExchRate(PayMargin payMargin){
        String currency = payMargin.getCurrency();
        String natCurrency = payMargin.getNatCurrency();
        // 如果本币币种与保证金币种一致则汇率固定为1
        CtmJSONObject jsonObject = new CtmJSONObject();
        BigDecimal exchRate = BigDecimal.ONE;
        Short exchRateOp = 1;
        if(!currency.equals(natCurrency)){
            try {
                CmpExchangeRateVO exchangeRateVO = CmpExchangeRateUtils.getNewExchangeRateWithMode(currency, natCurrency, payMargin.getVouchdate(), payMargin.getExchangeratetype());
                exchRate = exchangeRateVO.getExchangeRate();
                exchRateOp = exchangeRateVO.getExchangeRateOps();
            } catch (Exception e) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100917"),InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B000DF", InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D98D0BE05E0004C", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D98D0BE05E0004C", "未取到汇率") /* "未取到汇率" */) /* "未取到汇率" */) /* "未取到汇率" */);
            }
        }
        jsonObject.put("exchRate", exchRate);
        jsonObject.put("exchRateOp", exchRateOp);
        return jsonObject;
    }

    private PayMargin executeSave(BillDataDto dto){
        try {
            ApiImportCommandService apiImportCommandService = AppContext.getApplicationContext().getBean(ApiImportCommandService.class);
            PayMargin entity = (PayMargin) apiImportCommandService.singleSave4Api(dto);
            CommonRuleUtils.cleanParent(entity);
            return entity;
        } catch (Exception e) {
            log.error("execute save action fail, error:",e);
            if(e instanceof TransferDataException){// 翻译异常
                String errorMsg = extractTranslateErrorMsg((TransferDataException) e);
                if(!StringUtils.isEmpty(errorMsg)){
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100918"),errorMsg);
                }
            }else if(e instanceof CtmException){
                throw (CtmException) e;// Ctm异常继续往上抛
            }
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100919"),"execute save action fail,message:" + e.getMessage());
        }
    }


    private BillDataDto convertDeleteDto(List<String> srcIds, Map<String, Long> idMap) {
        List<PayMargin> deleteList = new ArrayList<>();
        for (String srcBillNo : srcIds) {
            PayMargin payMargin = new PayMargin();
            payMargin.setId(idMap.get(srcBillNo));
            payMargin.setEntityStatus(EntityStatus.Delete);
            payMargin.set("_fromApi",true);
            deleteList.add(payMargin);
        }
        BillDataDto billDataDto = new BillDataDto(IBillNumConstant.CMP_PAYMARGIN);
        billDataDto.setData(deleteList);
        billDataDto.setFromApi(true);// 设置标志
        return billDataDto;
    }

    private List<String> extractIds(CtmJSONObject param){
        try{
            CtmJSONArray jsonArray = param.getJSONArray("srcBillNo");
            if(jsonArray == null){
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100920"),InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D98D0BE05E00046", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D98D0BE05E00046", "非法的请求参数格式") /* "非法的请求参数格式" */) /* "非法的请求参数格式" */);
            }
            return jsonArray.toJavaList(String.class);
        }catch (Exception e){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100920"),InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D98D0BE05E00046", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D98D0BE05E00046", "非法的请求参数格式") /* "非法的请求参数格式" */) /* "非法的请求参数格式" */);
        }
    }

    private String getFormatMultiErrorMsg(){
        return InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D98D0BE05E00043", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D98D0BE05E00043", "格式错误") /* "格式错误" */);
    }

    private String getNonNullMultiErrorMsg(){
        return InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D98D0BE05E00041", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D98D0BE05E00041", "必填") /* "必填" */);
    }

    /**
     * 关联校验
     */
    private void validFormat(PayMarginApiDto dto){
        if(!validDate(dto.getVouchdate())){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100921"),getFormatMultiErrorMsg() + ": vouchdate");
        }
        String tradeTypeCode = dto.getTradetype_code();
        try{
            BdTransType transType = transTypeQueryService.queryTransTypes(tradeTypeCode);
            String paymargin_ext = "";
            if (ObjectUtils.isNotEmpty(transType.getExtendAttrsJson())) {
                CtmJSONObject jsonObject = CtmJSONObject.parseObject(transType.getExtendAttrsJson());
                if (ObjectUtils.isNotEmpty(jsonObject.get("paymargin_ext"))) {
                    paymargin_ext = jsonObject.get("paymargin_ext").toString();
                }
            }
            if(!PAYMENT.equals(tradeTypeCode) && !WITHDRAW.equals(tradeTypeCode) && !PAYMENT.equals(paymargin_ext) && !WITHDRAW.equals(paymargin_ext)){
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100922"),getFormatMultiErrorMsg() + ": tradeTypeCode");
            }
        }catch (Exception ex){
            log.error("获取交易类型失");
        }

        if(!validDate(dto.getExpectedretrievaldate())){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100923"),getFormatMultiErrorMsg() + ": expectedretrievaldate");
        }
        MarginOppositeType oppositeType = MarginOppositeType.find(Integer.valueOf(dto.getOppositetype()));
        int settleFlag = dto.getSettleflag();
        if(oppositeType == null){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100924"),getFormatMultiErrorMsg() + ": oppositeType");
        }else if(settleFlag == 1 && oppositeType == MarginOppositeType.Other){
            if(StringUtils.isEmpty(dto.getOppositename())){
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100925"),getNonNullMultiErrorMsg() + ": oppositename");
            }
            if(StringUtils.isEmpty(dto.getOppositebankaccountname())){
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100926"),getNonNullMultiErrorMsg() + ": oppositebankaccountname");
            }
            if(StringUtils.isEmpty(dto.getOppositebankaccount())){
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100927"),getNonNullMultiErrorMsg() + ": oppositebankaccount");
            }
        }

        if(settleFlag != 0 && settleFlag != 1){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100928"),getFormatMultiErrorMsg() + ": settleFlag");
        }
        if(settleFlag == 1){
            if(StringUtils.isEmpty(dto.getSettlemode_code())){
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100929"),getNonNullMultiErrorMsg() + ": settlemode");
            }
            Short settleStatus = dto.getSettlestatus();
            if(settleStatus == null){
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100930"),getNonNullMultiErrorMsg() + ": settleStatus");
            }
            if(settleStatus != FundSettleStatus.WaitSettle.getValue()
                    && settleStatus != FundSettleStatus.SettlementSupplement.getValue()){
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100931"),getFormatMultiErrorMsg() + ": settleStatus");
            }
            if(StringUtils.isEmpty(dto.getEnterprisebankaccount_code())){
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100932"),getNonNullMultiErrorMsg() + ": enterprisebankaccount");
            }
            // 如果对方类型不是其它，且需要结算时该字段必填
            if(StringUtils.isEmpty(dto.getTargetId()) && oppositeType != MarginOppositeType.Other){
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100933"),getNonNullMultiErrorMsg() + ": targetId");
            }
        }
        Short paymentType = dto.getPaymenttype();
        if(paymentType != null && PaymentType.find(paymentType) == null){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100934"),getFormatMultiErrorMsg() + ": paymenttype");
        }
        Short paymentSettleMode = dto.getPaymentsettlemode();
        if(paymentSettleMode != null && PaymentSettlemode.find(paymentSettleMode) == null){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100935"),getFormatMultiErrorMsg() + ": paymentSettleMode");
        }
        Short afterNetDir = dto.getAfterNetDir();
        if(afterNetDir != null && PaymentType.find(afterNetDir) == null){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100936"),getFormatMultiErrorMsg() + ": afterNetDir");
        }
        Short conversionMarginFlag = dto.getConversionmarginflag();
        if(conversionMarginFlag != null){
            if(conversionMarginFlag != 0 && conversionMarginFlag != 1){
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100937"),getFormatMultiErrorMsg() + ": conversionmarginFlag");
            }
            if(conversionMarginFlag == 1 && StringUtils.isEmpty(dto.getNewmarginbusinessno())){
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100938"),getNonNullMultiErrorMsg() + ": newmarginbusinessno");
            }
        }
        BigDecimal conversionAmount = dto.getConversionamount();
        if(conversionAmount != null && conversionAmount.compareTo(BigDecimal.ZERO) < 1){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100939"),"conversionAmount" + InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D98D0BE05E0004A", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D98D0BE05E0004A", "不能小于零") /* "不能小于零" */) /* "不能小于零" */);
        }
        if(!validDate(dto.getNewexpectedretrievaldate())){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100940"),getFormatMultiErrorMsg() + ": newexpectedretrievaldate");
        }
    }

    /**
     * 业务校验，需要在 save action 之前执行的业务逻辑校验
     */
    private void validBusiness(PayMarginApiDto dto){
        // 唯一性校验
        EventSource eventSource = EventSource.ExternalSystem;
        // 检查"保证金原始业务号"是否有关联已经存在的保证金虚拟账户，如果有则按已存在的单据的事项来源进行校验
        Optional<Long> op = findMarginVirtualAccount(dto);
        if(op.isPresent()){
            String id = op.get().toString();
            MarginWorkbench marginWorkbench = null;
            try {
                marginWorkbench = MetaDaoHelper.findById(MarginWorkbench.ENTITY_NAME,id);
            } catch (Exception e) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100941"),InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D98D0BE05E00040", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D98D0BE05E00040", "查询保证金虚拟账户信息报错：") /* "查询保证金虚拟账户信息报错：" */) /* "查询保证金虚拟账户信息报错：" */ + e.getMessage());
            }
            if (ObjectUtils.isNotEmpty(marginWorkbench)) {
                eventSource = EventSource.find(marginWorkbench.getSrcItem());
            }
        }

        Integer existedCount = countBySrcNo(dto.getSrcbillno(), eventSource);
        if(existedCount > 0){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100942"),"srcBillNo" + InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D98D0BE05E00042", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D98D0BE05E00042", "必须保持唯一") /* "必须保持唯一" */) /* "必须保持唯一" */);
        }
    }

    /**
     * 把API的请求参数转换为MDD格式
     */
    private BillDataDto convert(PayMarginApiDto dto){
        PayMargin entity = new PayMargin();
        // 复制基本字段
        BeanUtils.copyProperties(dto,entity);
        // 不填时默认为“不转换”
        if(entity.getConversionmarginflag() == null){
            entity.setConversionmarginflag((short)0);
        }
        // 映射日期类型字段
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        try {
            entity.setVouchdate(dateFormat.parse(dto.getVouchdate()));
            String expectedRetrievalDate = dto.getExpectedretrievaldate();
            if(!StringUtils.isEmpty(expectedRetrievalDate)){
                entity.setExpectedretrievaldate(dateFormat.parse(expectedRetrievalDate));
            }
        } catch (ParseException e) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100943"),"unexpected date format");
        }
        try{
            BdTransType transType = transTypeQueryService.queryTransTypes(dto.getTradetype_code());
            entity.put("tradetype",transType.getId());
        }catch (Exception ex){
            log.error("来源api填充交易类型时，获取交易类型失败");
        }
        // 写入需要翻译的字段
        entity.put("accentity_code",dto.getAccentity_code());
        entity.put("tradetype_code",dto.getTradetype_code());
        entity.put("margintype_code",dto.getMargintype_code());
        entity.put("settlemode_code",dto.getSettlemode_code());
        entity.put("currency_code",dto.getCurrency_code());
        entity.put("project_code",dto.getProject_code());
        entity.put("dept_code",dto.getDept_code());
        entity.put("enterprisebankaccount_code",dto.getEnterprisebankaccount_code());
        fillTargetProp(dto, entity);
        entity.put("oppositebankNumber_code",dto.getOppositebankNumber_code());
        entity.put("oppositebankType_code",dto.getOppositebankType_code());
        entity.put("newmargintype_code",dto.getNewmargintype_code());
        entity.put("newproject_code",dto.getNewproject_code());
        entity.put("newdept_code",dto.getNewdept_code());

        String exchangeRateTypeCode = dto.getExchangeratetype_code();
        if(StringUtils.isEmpty(exchangeRateTypeCode)){
            exchangeRateTypeCode = "01";// 未填值时为默认汇率
        }
        entity.put("exchangeratetype_code", exchangeRateTypeCode);

        Optional<Long> marginVirtualAccountOp = findMarginVirtualAccount(dto);
        if(marginVirtualAccountOp.isPresent()){
            entity.setMarginvirtualaccount(marginVirtualAccountOp.get());
        }

        // 添加默认值
        entity.setInitflag((short)0);// 是否期初数据
        entity.setSrcitem(EventSource.ExternalSystem.getValue());// 事项来源
        entity.put("_status","Insert");
        dealCharacterDef(dto,entity);
        CtmJSONObject jsonObj = CtmJSONObject.toJSON(entity);
        BillDataDto billDataDto = new BillDataDto();
        billDataDto.setBillnum("cmp_paymargin");
        billDataDto.setData(jsonObj);
        billDataDto.setFromApi(true);// 设置标志
        return billDataDto;
    }

    private void dealCharacterDef(PayMarginApiDto dto,PayMargin payMargin){
        Map<String, Object> characterDef = dto.getCharacterDef();
        if(characterDef == null || characterDef.isEmpty()){
            return;
        }
        BizObject def = Objectlizer.convert(characterDef, PayMarginCharacterDef.ENTITY_NAME);
        if(StringUtils.isEmpty(payMargin.getId())){
            payMargin.put("id", String.valueOf(IdCreator.getInstance().nextId()));
        }
        //设置特征状态
        payMargin.put("characterDef", def);
    }

    /**
     * 取回时基于原始业务单号联动带出保证金虚拟账号
     */
    private Optional<Long> findMarginVirtualAccount(PayMarginApiDto dto){
        if(!dto.getTradetype_code().equals(WITHDRAW)){
            return Optional.empty();
        }
        String busNo = dto.getMarginbusinessno();
        QuerySchema schema = QuerySchema.create().addSelect("marginvirtualaccount");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("marginbusinessno").eq(busNo));
        schema.addCondition(conditionGroup);
        List<BizObject> bizObjects = null;
        try {
            bizObjects = MetaDaoHelper.queryObject(PayMargin.ENTITY_NAME, schema, null);
        } catch (Exception e) {
            log.error("querySrcNo fail,",e);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100944"),"query fail unexpected");
        }
        if(bizObjects.isEmpty()){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100945"),InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D98D0BE05E0004B", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D98D0BE05E0004B", "基于原始业务单号无法找到关联的保证金虚拟账户") /* "基于原始业务单号无法找到关联的保证金虚拟账户" */) /* "基于原始业务单号无法找到关联的保证金虚拟账户" */ + "，marginbusinessno: " + busNo);//@notranslate
        }
        Long acc = ((PayMargin) bizObjects.get(0)).getMarginvirtualaccount();
        return Optional.ofNullable(acc);
    }

    /**
     * 填充对方信息
     */
    private void fillTargetProp(PayMarginApiDto dto, PayMargin entity) {
        String targetProp = null;
        String targetBankAccountProp = null;
        MarginOppositeType oppType = MarginOppositeType.find(Integer.valueOf(dto.getOppositetype()));
        switch (oppType) {
            case Customer:
                targetProp = "customer";
                targetBankAccountProp = "customerbankaccount";
                break;
            case Supplier:
                targetProp = "supplier";
                targetBankAccountProp = "supplierbankaccount";
                break;
            case OwnOrg:
                targetProp = "ourname";
                targetBankAccountProp = "ourbankaccount";
                break;
            case CapBizObj:
                targetProp = "capBizObj";
                targetBankAccountProp = "capBizObjbankaccount";
                break;
            default:
                break;
        }
        // 联行号不存储
        if (!StringUtils.isEmpty(targetProp)) {
            entity.put(targetProp + "_code", dto.getTargetId());
        }
        if (!StringUtils.isEmpty(targetBankAccountProp)) {
            entity.put(targetBankAccountProp + "_code", dto.getTargetbankaccount());
        }
    }

    private Map<String,Long> checkBeforeDelete(List<String> srcIds,Map<String, PayMargin> map){
        Map<String,Long> idMap = new HashMap<>();
        List<String> nonExistSrcBillIds = new ArrayList<>();
        List<Long> nonDraftIds = new ArrayList<>();
        for(String srcBillNo : srcIds){
            PayMargin entity = map.get(srcBillNo);
            if(entity == null){
                nonExistSrcBillIds.add(srcBillNo);// id不存在
            }else if(entity.getVerifystate() != 0 && entity.getVerifystate() != 4 && entity.getVerifystate() != 3){
                nonDraftIds.add(entity.getId());// 不是草稿态
            }else{
                idMap.put(entity.getSrcbillno(), entity.getId());
            }
        }
        String errorMsg = "";
        if(!nonExistSrcBillIds.isEmpty()){
            errorMsg += InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D98D0BE05E00048", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D98D0BE05E00048", "以下单据不存在") /* "以下单据不存在" */) /* "以下单据不存在" */ + "srcBillNo:" + nonExistSrcBillIds + ";";
        }
        if(!nonDraftIds.isEmpty()){
            errorMsg += InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D98D0BE05E00049", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D98D0BE05E00049", "以下单据已经进行业务处理，无法删除:") /* "以下单据已经进行业务处理，无法删除:" */) /* "以下单据已经进行业务处理，无法删除:" */ + nonDraftIds + ";";
        }
        if(!StringUtils.isEmpty(errorMsg)){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100946"),errorMsg);
        }
        return idMap;
    }

    private List<PayMargin> queryBySrcId(List<String> srcBillIds) {
        // 根据id批量查询数据
        QuerySchema schema = QuerySchema.create().addSelect("id,srcbillno,verifystate");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("srcbillno").in(srcBillIds));
        conditionGroup.appendCondition(QueryCondition.name("srcitem").eq(EventSource.ExternalSystem.getValue()));
        schema.addCondition(conditionGroup);
        List<PayMargin> list = null;
        try {
            list = MetaDaoHelper.queryObject(PayMargin.ENTITY_NAME, schema, null);
        } catch (Exception e) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100947"),"query paymargin fail");
        }
        return list;
    }

    private Integer countBySrcNo(String srcBillNo,EventSource eventSource) {
        // 根据id批量查询数据
        QuerySchema schema = QuerySchema.create().addSelect("id");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("srcbillno").eq(srcBillNo));
        conditionGroup.appendCondition(QueryCondition.name("srcitem").eq(eventSource.getValue()));
        schema.addCondition(conditionGroup);
        List<BizObject> bizObjects = null;
        try {
            bizObjects = MetaDaoHelper.queryObject(PayMargin.ENTITY_NAME, schema, null);
        } catch (Exception e) {
            log.error("querySrcNo fail,",e);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100948"),"query fail unexpected");
        }
        return bizObjects.size();
    }

    /**
     * 必填校验
     */
    private void validNonNull(PayMarginApiDto dto){
        for(String key : NON_NULL_PROP.keySet()){
            Object value = NON_NULL_PROP.get(key).apply(dto);
            if(isNull(value)){
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100949"),key + "" + getNonNullMultiErrorMsg());
            }
        }
    }

    private boolean validDate(String str){
        if(StringUtils.isEmpty(str)){
            return true;
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        try{
            formatter.parse(str);
            return true;
        }catch (Exception e){
            return false;
        }
    }

    private boolean isNull(Object value){
        if(value == null){
            return true;
        }else if(value instanceof String){
            return StringUtils.isEmpty((String) value);
        }
        return false;
    }

    /**
     * 翻译报错信息解析,提取必要信息
     * @param transferDataException 翻译以后
     * @return 报错信息
     */
    private static String extractTranslateErrorMsg(TransferDataException transferDataException){
        try{
            String rawMsg = transferDataException.getMessage();
            CtmJSONArray array = CtmJSONArray.parseArray(rawMsg);
            String errorMsg = "";
            Iterator<Object> iterator = array.iterator();
            while(iterator.hasNext()){
                Map<String,String> next  = (Map<String,String>)iterator.next();
                String message = next.get("message");
                errorMsg += message + ";";
            }
            return errorMsg;
        }catch (Exception e){
            log.error("解析翻译信息出错：",e);
        }
        return "";
    }

}
