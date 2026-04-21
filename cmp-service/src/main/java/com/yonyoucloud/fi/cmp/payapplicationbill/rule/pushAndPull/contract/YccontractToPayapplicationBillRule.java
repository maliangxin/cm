package com.yonyoucloud.fi.cmp.payapplicationbill.rule.pushAndPull.contract;

import com.yonyou.ucf.basedoc.model.BankdotVO;
import com.yonyou.ucf.basedoc.model.CurrencyTenantDTO;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.cmp.common.service.CmCommonService;
import com.yonyoucloud.fi.cmp.util.BigDecimalUtils;
import com.yonyoucloud.fi.cmp.util.QueryBaseDocUtils;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import com.yonyoucloud.fi.cmp.util.basedoc.VendorQueryService;
import com.yonyoucloud.upc.pub.api.vendor.vo.vendor.VendorBankVO;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.collections4.CollectionUtils;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.orm.base.BizObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import static com.yonyoucloud.fi.cmp.constant.ICmpConstant.CONSTANT_EIGHT;

/**
 * @author shangxd
 * @version 1.0
 * @since 2021-03-26 10:50
 * 采购合同根据付款协议到付款申请的一个推单的过程；([)付款申请：依据业务【采购，应付事项等】发起付款申请的一个模块)
 */
@Component("yccontractToPayapplicationBillRule")
public class YccontractToPayapplicationBillRule extends AbstractCommonRule {

    private final String CONTRACTAPPLY="contract-apply";
    @Autowired
    BaseRefRpcService baseRefRpcService;
    @Autowired
    CmCommonService cmCommonService;

    @Autowired
    VendorQueryService vendorQueryService;
    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        //1.推单方发送的原始数据为sourceDatas  公司框架提供 判断原始参数是否为空
        List<BizObject> sourceDatas = (List) paramMap.get("sourceDatas");
        if (CollectionUtils.isEmpty(sourceDatas)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100425"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041802DE","获取采购合同表体付款协议数据失败") /* "获取采购合同表体付款协议数据失败" */);
        }
        //2.根据生单规则转换后数据为omake  公司框架提供   判断转换后参数是否为空 初步映射 平台根据配置映射字段映射一部分值
        List<Map<String, Object>> omakes = (List) paramMap.get("omake");
        //根据双方约定规则过滤数据
        filterInfo( sourceDatas,  omakes);
        if (CollectionUtils.isEmpty(omakes)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100426"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041802DD","请选择要推单的协议行") /* "请选择要推单的协议行" */);
        }
        //3.迭代处理平台无法生成的字段信息
        Map<String, Object> map = omakes.get(0);
        List<Map<String,Object>>  omkcontractPrepayList=(List<Map<String,Object>>) map.get("contractPrepayList");//contractPrepayList是采购合同预付付款单
        List<Map<String,Object>>  contractMaterialList=(List<Map<String,Object>>) map.get("contractMaterialList");
        Map<String, Object> ContractBodyVODefineCharacter = new HashMap<>();
        if (ValueUtils.isNotEmptyObj(contractMaterialList)){
            ContractBodyVODefineCharacter = (Map<String, Object>) contractMaterialList.get(0).get("ContractBodyVODefineCharacter");
        }
        Iterator<Map<String, Object>> iter = omkcontractPrepayList.iterator();
        while (iter.hasNext()) {
            Map<String, Object> mapSub = iter.next();
            //accPayApplyMoney累计付款申请金额
            if (null == mapSub.get("accPayApplyMoney")) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100427"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041802E0","请accPayApplyMoney付默认值") /* "请accPayApplyMoney付默认值" */);
            }
            //#( payTaxMoney - accPayApplyMoney )payTaxMoney含税金额  含税金额减去累计付款申请金额不能小于0  否则不能生单
            BigDecimal payTaxMoney = (BigDecimal) mapSub.get("payTaxMoney");
            BigDecimal accPayApplyMoney = (BigDecimal) mapSub.get("accPayApplyMoney");
            BigDecimal temp = BigDecimalUtils.safeSubtract(payTaxMoney,accPayApplyMoney);
            if(0 >= temp.compareTo(BigDecimal.ZERO)){
                iter.remove();
                continue;
            }
            if (ValueUtils.isNotEmpty(ContractBodyVODefineCharacter)){
                mapSub.put("ContractBodyVODefineCharacter", ContractBodyVODefineCharacter);
            }
        }
        if (CollectionUtils.isEmpty(omkcontractPrepayList)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100428"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041802DC","没有符合条件的生单数据") /* "没有符合条件的生单数据" */);
        }
        //交易类型id
        Map<String, Object> tradetypeMap = cmCommonService.queryTransTypeById("FICM3", "0", CONTRACTAPPLY);
        if (ValueUtils.isEmpty(tradetypeMap)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100429"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041802E1","该单据未查询到相应的交易类型！") /* "该单据未查询到相应的交易类型！" */);
        }
        map.put("tradetype", tradetypeMap.get("id"));
        map.put("tradetype_name", tradetypeMap.get("name"));
        map.put("tradetype_code", tradetypeMap.get("code"));


        Object supplierId = map.get("supplier") !=null ? map.get("supplier") : map.get("supplierId");
        Map<String, Object> conditionSupplierId = new HashMap<>(CONSTANT_EIGHT);
        conditionSupplierId.put("vendor", supplierId);
        conditionSupplierId.put("defaultbank", true);
        Object currency = map.get("currency");
        if (ValueUtils.isNotEmptyObj(currency)) {
            conditionSupplierId.put("currency", currency);
        }
        conditionSupplierId.put("stopstatus", "0");
        List<VendorBankVO> bankAccounts = vendorQueryService.getVendorBanksByCondition(conditionSupplierId);
        if (bankAccounts.size() > 0) {
            map.put("supplierbankaccount", bankAccounts.get(0).getId());
            map.put("supplierbankaccount_accountname", bankAccounts.get(0).getAccountname());
            map.put("supplierbankaccount_account", bankAccounts.get(0).getAccount());
            map.put("supplierbankaccount_correspondentcode", bankAccounts.get(0).getCorrespondentcode());
            BankdotVO depositBank =baseRefRpcService.queryBankdotVOByBanddotId(bankAccounts.get(0).getOpenaccountbank());
            if (depositBank != null) {
                map.put("supplierbankaccount_openaccountbank_name", depositBank.getName()); // 供应商账户银行网点
            }else {
                map.put("supplierbankaccount_openaccountbank_name", null); // 供应商账户银行网点
            }
        }

        CurrencyTenantDTO currencyTenantDTO = baseRefRpcService.queryCurrencyById(map.get("currencyId").toString());
        if (currencyTenantDTO!=null) {
            map.put("currency_priceDigit", currencyTenantDTO.getPricedigit());
            map.put("currency_moneyDigit", currencyTenantDTO.getMoneydigit());
        }

        // 款项类型
        Map<String, Object> condition = new HashMap<>();
        condition.put("code", "5");//预付款
        List<Map<String, Object>> quickType = QueryBaseDocUtils.queryQuickTypeByCondition(condition);
        if (ValueUtils.isNotEmpty(quickType)) {
            map.put("quickType", quickType.get(0).get("id"));
            map.put("quickType_name", quickType.get(0).get("name"));
            map.put("quickType_code", quickType.get(0).get("code"));
        }
        //业务员
        Map<String, Object> queryStaffById = QueryBaseDocUtils.queryStaffById(map.get("purPersonId"));
        if(null != queryStaffById){
            map.put("operator_name", queryStaffById.get("name"));
        }

        //不是自动生单 autobill
        map.put("autobill", true);
        if (CollectionUtils.isEmpty(omakes)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100428"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041802DC","没有符合条件的生单数据") /* "没有符合条件的生单数据" */);
        }
        paramMap.put("omake", omakes);
        return new RuleExecuteResult();
    }

    /**
     * 根据标识过滤数据
     * @param sourceData 原始数据为sourceDatas
     * @param omakes   根据生单规则转换后数据为omake
     */
    public void filterInfo(List<BizObject> sourceData, List<Map<String, Object>> omakes){
        //1.判断是否选择了付款协议信息
        List<Map<String, Object>> contractPayTermList = sourceData.get(0).get("contractPayTermList");//contractPayTermList是付款协议信息
        if (Objects.isNull(contractPayTermList) || contractPayTermList.size() < 1) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100430"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041802E2","获取付款协议数据失败") /* "获取付款协议数据失败" */);
        }
        //根据双方约定标识过滤，获取采购合同需要处理的数据 dr逻辑删除的标记
        List<Map<String, Object>> selectData = contractPayTermList.stream()
                .filter(e -> Objects.nonNull(e.get("pushDown")) &&
                        MapUtils.getBoolean(e, "pushDown") &&
                        0 == MapUtils.getInteger(e, "dr")).collect(Collectors.toList());
        if (selectData.size() < 1) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100426"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041802DD","请选择要推单的协议行") /* "请选择要推单的协议行" */);
        }
        Integer payPeriod=MapUtils.getInteger(selectData.get(0),"payPeriod");//付款阶段 payPeriod是期号的数字
        Long contractPaytermId=MapUtils.getLong(selectData.get(0),"id");//关联关系
        //2.根据约定标识过滤付款协议信息
        List<Map<String,Object>> omkcontractPayTermList=(List<Map<String,Object>>)omakes.get(0).get("contractPayTermList");
        List<Map<String,Object>> selectomkcontractPayTermList= omkcontractPayTermList.stream().filter(e->contractPaytermId.equals(MapUtils.getLong(e,"id"))).collect(Collectors.toList());//根据约定条件pushDown得到的id，过滤数据信息
        //3.根据约定条件过滤采购合同预付付款单数据
        List<Map<String, Object>> contractPrepayList = (List<Map<String, Object>>) omakes.get(0).get("contractPrepayList");//contractPrepayList是采购合同预付付款单
        if (Objects.isNull(contractPrepayList) || contractPrepayList.size() < 1) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100431"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041802DF","没有符合条件的预付付款单数据") /* "没有符合条件的预付付款单数据" */);
        }
        List<Map<String, Object>> selectcontractPrepayList = contractPrepayList.stream()
                .filter(e -> payPeriod.equals(MapUtils.getInteger(e, "payPeriod"))
                        && contractPaytermId.equals(MapUtils.getLong(e, "contractPaytermId"))
                        && 0 == MapUtils.getInteger(e, "dr")).collect(Collectors.toList());//根据约定条件过滤采购合同预付付款单数据
        if (selectcontractPrepayList.size() < 1) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100431"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041802DF","没有符合条件的预付付款单数据") /* "没有符合条件的预付付款单数据" */);
        }
        //4.组装数据
        omakes.get(0).put("contractPayTermList",selectomkcontractPayTermList);
        omakes.get(0).put("contractPrepayList",selectcontractPrepayList);
    }


}
