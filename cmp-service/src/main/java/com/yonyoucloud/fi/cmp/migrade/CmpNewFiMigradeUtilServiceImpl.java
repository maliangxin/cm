package com.yonyoucloud.fi.cmp.migrade;

import com.google.common.collect.Lists;
import com.yonyou.diwork.service.IApplicationService;
import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.ucf.CharacterDTO;
import com.yonyou.ucf.basedoc.model.BankdotVO;
import com.yonyou.ucf.basedoc.model.rpcparams.BankBdRequestParams;
import com.yonyou.ucf.mdd.ext.bpm.model.VerifyState;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.dubbo.DubboReferenceUtils;
import com.yonyou.ucf.transtype.model.BdTransType;
import com.yonyou.ucf.transtype.service.itf.ITransTypeService;
import com.yonyou.ucf.userdef.api.ICharaterServiceRPC;
import com.yonyou.workbench.model.ApplicationVO;
import com.yonyou.yht.sdkutils.HttpTookit;
import com.yonyou.yonbip.ctm.constant.CtmConstants;
import com.yonyou.yonbip.ctm.workbench.ICtmApplicationService;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.basecom.service.ref.CustomerRpcService;
import com.yonyoucloud.fi.basecom.service.ref.SupplierRpcService;
import com.yonyoucloud.fi.basecom.util.HttpTookitYts;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.cmpentity.CaObject;
import com.yonyoucloud.fi.cmp.cmpentity.EventSource;
import com.yonyoucloud.fi.cmp.common.service.CmCommonService;
import com.yonyoucloud.fi.cmp.constant.IBussinessConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.event.sendEvent.ICmpSendEventService;
import com.yonyoucloud.fi.cmp.fundpayment.FundPayment;
import com.yonyoucloud.fi.cmp.payapplicationbill.PayApplicationBill;
import com.yonyoucloud.fi.cmp.paybill.PayBill;
import com.yonyoucloud.fi.cmp.receivebill.ReceiveBill;
import com.yonyoucloud.fi.cmp.util.DateUtils;
import com.yonyoucloud.fi.cmp.util.QueryBaseDocUtils;
import com.yonyoucloud.fi.cmp.util.basedoc.VendorQueryService;
import com.yonyoucloud.iuap.upc.dto.AgentFinancialDTO;
import com.yonyoucloud.iuap.upc.dto.MerchantDTO;
import com.yonyoucloud.upc.pub.api.vendor.vo.vendor.VendorBankVO;
import com.yonyoucloud.upc.pub.api.vendor.vo.vendor.VendorVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CmpNewFiMigradeUtilServiceImpl implements CmpNewFiMigradeUtilService{

    @Autowired
    BaseRefRpcService baseRefRpcService;

    @Autowired
    CustomerRpcService customerRpcService;

    @Autowired
    SupplierRpcService supplierRpcService;

    @Autowired
    VendorQueryService vendorQueryService;

    @Autowired
    ITransTypeService iTransTypeService;

    @Resource
    private IApplicationService appService;

    @Autowired
    private CmCommonService commonService;

    @Resource
    private ICmpSendEventService cmpSendEventService;

    //付款工作台 其他付款 sourceid-11530911359601201  资金付 其他付款 sourceid-2553204791808512
    //付款工作台 销售退款 sourceid-11530911359601211  资金付 销售退款 sourceid-2553205983597056
    //付款工作台 报销付款 sourceid-11530911359601221  资金付 报销付款 sourceid-2553206910816768
    //付款工作台 采购付款 sourceid-11530911359601241  资金付 采购付款 sourceid-2553208922362368

    //收款工作台 其他收款 sourceid-11530911359601231  资金付 其他收款 sourceid-2571644308067072
    //收款工作台 销售退款 sourceid-11530911359601251  资金付 销售退款 sourceid-2571644887618304
    //收款工作台 采购退款 sourceid-11530911359601261  资金付 采购退款 sourceid-2571645514993920
    //收款工作台 杂项收款 sourceid-12480184231733761  资金付 杂项收款 sourceid-2571646385967872

    private static Map<String,String>  sourceidMap = new HashMap<>();
    static {
        sourceidMap.put("11530911359601201", "2553204791808512");
        sourceidMap.put("11530911359601211", "2553205983597056");
        sourceidMap.put("11530911359601221", "2553206910816768");
        sourceidMap.put("11530911359601241", "2553208922362368");

        sourceidMap.put("11530911359601231", "2571644308067072");
        sourceidMap.put("11530911359601251", "2571644887618304");
        sourceidMap.put("11530911359601261", "2571645514993920");
        sourceidMap.put("12480184231733761", "2571646385967872");
    }

    /*特征分配字段*/
    private static final String U8C_DOMAIN = "u8c-userdefine";// 李梦云提供
    private static final String CHARS_ASSIGN_URI = "/bill/characterassign/batchCharactersAssignEntitys";
    private static final String GET_ASSIGN_RESULT_URI = AppContext.getEnvConfig("domain.iuap-metadata-extend") + "/bill/task/getAssignResult";
    private static final String OK_CODE = "200";
    private ICharaterServiceRPC charaterService = DubboReferenceUtils.getDubboService(ICharaterServiceRPC.class, U8C_DOMAIN, null, 1000);

    @Override
    public Map<Long, MerchantDTO> buildCustomerMap(List<Long> customerIdList) throws Exception {
        //存储客户的缓存数据
        Map<Long,MerchantDTO> merchantDTOHap = new HashMap<>();
        if(customerIdList!=null && !customerIdList.isEmpty()){
            List<MerchantDTO>  customerList = QueryBaseDocUtils.getMerchantByIds(customerIdList);
            if(customerList!=null && !customerList.isEmpty()){
                for(MerchantDTO vo : customerList){
                    merchantDTOHap.put(vo.getId(),vo);
                }
            }
        }
        return merchantDTOHap;
    }

    @Override
    public Map<Long, VendorVO> buildSupplierMap(List<Long> supplierIdList) throws Exception {
        //供应商信息缓存
        Map<Long,VendorVO> supplierMap = new HashMap<>();
        if(supplierIdList!=null && supplierIdList.size()>0){
            List<VendorVO> supplierList = vendorQueryService.getVendorFieldByIdList(supplierIdList);
            if(supplierList!=null && !supplierList.isEmpty()){
                for(VendorVO vo : supplierList){
                    supplierMap.put(vo.getId(),vo);
                }
            }

        }
        return supplierMap;
    }

    @Override
    public Map<String,Map<String, Object>> buildEmployeeMap(List<Object> employeeIdList) throws Exception {
        Map<String,Map<String, Object>> employeeMap = new HashMap<>();
        //人员 queryStaffByIds
        if(employeeIdList!=null && employeeIdList.size()>0){
            List<Map<String, Object>> employeeList = QueryBaseDocUtils.queryStaffByIds(employeeIdList);
            //缓存人员信息
            if(employeeList!=null && employeeList.size()>0){
                for(Map<String, Object> vo: employeeList){
                    employeeMap.put(vo.get("id").toString(),vo);
                }
            }
        }
        return employeeMap;
    }

    @Override
    public Map<Long, AgentFinancialDTO> buildCustomerBankMap(List<Long> customerbankaccountIdList) throws Exception {
        Map<Long,AgentFinancialDTO> agentFinancialDTOMap = new HashMap<>();
        if(customerbankaccountIdList!=null && !customerbankaccountIdList.isEmpty()){
            List<AgentFinancialDTO> customerBankAccountList = QueryBaseDocUtils.queryCustomerBankAccountByIds(customerbankaccountIdList);
            if(customerBankAccountList!=null && !customerBankAccountList.isEmpty()){
                for(AgentFinancialDTO vo : customerBankAccountList){
                    agentFinancialDTOMap.put(vo.getId(),vo);
                }
            }
        }
        return agentFinancialDTOMap;
    }

    @Override
    public Map<Long, VendorBankVO> buildsupplierBankMap(List<Long> supplierbankaccountIdList) throws Exception {
        Map<Long,VendorBankVO> supplierBankVoMap = new HashMap<>();
        if(supplierbankaccountIdList!=null && !supplierbankaccountIdList.isEmpty()){
            String fields [] = {"id","account","accountname","openaccountbank","openaccountbank","correspondentcode","bank"};
            List<VendorBankVO> supplierBankVos = supplierRpcService.getVendorBanksByIdList(supplierbankaccountIdList,fields);
            //供应商账户信息缓存
            if(supplierBankVos!=null && !supplierBankVos.isEmpty()){
                for(VendorBankVO vo : supplierBankVos){
                    supplierBankVoMap.put(vo.getId(),vo);
                }
            }
        }
        return supplierBankVoMap;
    }

    @Override
    public Map<String, Map<String, Object>> bulidEmployeeBankMap(List<Object> employeeankAccountIdList) throws Exception {
        //缓存人员银行账户信息
        Map<String,Map<String, Object>> employeeBankMap = new HashMap<>();
        if(employeeankAccountIdList!=null && !employeeankAccountIdList.isEmpty()){
            List<Map<String, Object>> employeeBankList = QueryBaseDocUtils.queryStaffBankAccountByIds(employeeankAccountIdList);
            if(employeeBankList!=null && !employeeBankList.isEmpty()){
                for(Map<String, Object> vo : employeeBankList){
                    employeeBankMap.put(vo.get("id").toString(),vo);
                }
            }
        }

        return employeeBankMap;
    }

    @Override
    public Map<String, BankdotVO> bankdotMap(List<String> ids) throws Exception {
        Map<String,BankdotVO> bankdotNameMap = new HashMap<>();
        if(ids!=null && !ids.isEmpty()){
            //如果当前集合ids大于50 分组为50个一组分批查询
            List<List<String>>  groupDataList= groupData(ids, 50);
            for(List<String> idListnow : groupDataList){
                BankBdRequestParams bdRequestParams = new BankBdRequestParams();
                bdRequestParams.setIds(idListnow);
                List<BankdotVO> bankList = baseRefRpcService.queryBankDtoByParam(bdRequestParams);
                if(bankList!=null && !bankList.isEmpty()){
                    for(BankdotVO vo: bankList){
                        bankdotNameMap.put(vo.getId(),vo);
                    }
                }
            }
        }
        return bankdotNameMap;
    }

    @Override
    public List<List<String>> groupData(List<String> listNow, int groupNum) {
        List<List<String>> targetList = new CopyOnWriteArrayList<>();
        if(listNow.size()>groupNum){
            int size = listNow.size();
            int remainder = size % groupNum;
            int sum = size / groupNum;
            for (int i = 0; i<sum; i++) {
                List<String> subList;
                subList = listNow.subList(i * groupNum, (i + 1) * groupNum);
                targetList.add(subList);
            }
            if (remainder > 0) {
                List<String> subList;
                subList = listNow.subList(size - remainder, size);
                targetList.add(subList);
            }
        }else{
            targetList.add(listNow);
        }
        return targetList;
    }


    @Override
    public Date periodBeginDate(String schema_fullname) throws Exception {
        //请求arap接口 如果返回数据正确则取应收应付返回数据 如果catch到错误走旧有逻辑
        Date returnDate = new Date();
        try {
            String url = AppContext.getEnvConfig("domain.url", null);
            if(url!=null){
                String reUrl = url+"/yonbip-fi-opswise/fi/api/getPeriod?domainKey=yonbip-fi-ervn";
                Map<String, String> requestHeader = new HashMap<>();
                String responseStr = HttpTookit.doGet(reUrl, null, requestHeader);
                CtmJSONObject resultJson = CtmJSONObject.parseObject(responseStr);
                if(ICmpConstant.REQUEST_SUCCESS_STATUS_CODE.equals(resultJson.getString(ICmpConstant.CODE))){
                    returnDate = DateUtils.parseDate(resultJson.getString("data"));
                }else{
                    returnDate = getPeriodBeginDateForAccentity(schema_fullname);
                }
            }
        }catch(Exception e){
            returnDate = getPeriodBeginDateForAccentity(schema_fullname);
        }
        return returnDate;
    }


    private Date getPeriodBeginDateForAccentity(String schema_fullname) throws Exception {
        QuerySchema queryAccentity= QuerySchema.create().addSelect("accentity");
        QueryConditionGroup conditionGroupAccentity = new QueryConditionGroup(ConditionOperator.and);
        conditionGroupAccentity.appendCondition(QueryCondition.name("srcitem").eq(EventSource.Cmpchase.getValue()));
        conditionGroupAccentity.addCondition(conditionGroupAccentity);
        queryAccentity.setLimitCount(1);
        List<Map<String, Object>> accentityData = MetaDaoHelper.query(schema_fullname ,queryAccentity);
        Date beginDate = new Date();
        if(!accentityData.isEmpty()){
            Map<String, Object> periodvo = QueryBaseDocUtils.queryPeriodByAccbodyAndDate(accentityData.get(0).get("accentity").toString(),new Date());
            if(periodvo.get("begindate")!=null){
                beginDate = (Date) periodvo.get("begindate");
            }
        }
        return beginDate;
    }

    @Override
    public Map<String ,Boolean>  checkBeforeUpgrade(String schema_fullname) throws Exception {

        /**
         * 先进行数据升级校验 有特殊情况不允许升级
         * 1.有银企联支付的在途数据
         */
        Map<String , Boolean> resultMap = new HashMap<>();
        checkBeforeUpgradePaystatus(schema_fullname,resultMap);
        if(!resultMap.get("PayStatus")){
            return resultMap;
        }
        //2.升级月份前的没有生成凭证的 已结算完成数据(凭证校验只看应收应付)
        checkBeforeUpgradeVouchSatus(schema_fullname,resultMap);
        if(!resultMap.get("VouchSatus")){
            return resultMap;
        }
        //3.交易类型校验 查询资金收付 与 收付工作台的非预制交易类型 是否做到了name，code 一一对应
        checkBeforeUpgradeTransType(schema_fullname, resultMap);
        if(schema_fullname.equals(PayBill.ENTITY_NAME) && !resultMap.get("CheckPayTransType")){
            return resultMap;
        }
        if(schema_fullname.equals(ReceiveBill.ENTITY_NAME)&& !resultMap.get("CheckRecTransType")){
            return resultMap;
        }
        //4付款申请单审批过程中的单据 不能升级
        checkBeforeUpgradePayapplicationApprove(schema_fullname, resultMap);
        if(schema_fullname.equals(PayBill.ENTITY_NAME)&&!resultMap.get("CheckPayapplicationApprove")){
            return resultMap;
        }
        //5收付款工作台自制单据，在审批中时不能升级
        checkBeforeUpgradeCmpBillApprove(schema_fullname,resultMap);
        if(!resultMap.get("CheckCmpBillApprove")){
            return resultMap;
        }
        return resultMap;
    }

    private void checkBeforeUpgradePaystatus(String schema_fullname,Map<String , Boolean> resultMap) throws Exception {
        if(schema_fullname.equals(PayBill.ENTITY_NAME)){
            QuerySchema queryPaystatus = QuerySchema.create().addSelect("id,code");
            QueryConditionGroup queryConditionPaystatus = new QueryConditionGroup();
            queryConditionPaystatus.addCondition(QueryCondition.name("auditstatus").eq("1"), QueryCondition.name("paystatus").in("1","2","4","5","6"));//付款 预下单成功、预下单失败、支付失败、支付中、支付不明
            List<Map> resListPaystatus = MetaDaoHelper.query(PayBill.ENTITY_NAME, queryPaystatus.addCondition(queryConditionPaystatus));
            if(resListPaystatus!=null && !resListPaystatus.isEmpty()){
                resultMap.put("PayStatus",false);
            }else
                resultMap.put("PayStatus",true);
        }else{
            resultMap.put("PayStatus",true);
        }

    }

    private void checkBeforeUpgradeVouchSatus(String schema_fullname,Map<String , Boolean> resultMap) throws Exception {
        Date periodBeginDate = periodBeginDate(PayBill.ENTITY_NAME);
        QuerySchema queryVouchSatus = QuerySchema.create().addSelect("id,code");
        QueryConditionGroup queryConditionVouchSatus = new QueryConditionGroup();
        queryConditionVouchSatus.addCondition
                (QueryCondition.name("auditstatus").eq("1"), QueryCondition.name("settlestatus").eq("2"),QueryCondition.name("srcitem").eq(EventSource.Cmpchase.getValue()));
        String[] vouchSatus = new String[]{"2", "3"};
        queryConditionVouchSatus.addCondition(QueryCondition.name("voucherstatus").in((Object) vouchSatus));
        queryConditionVouchSatus.addCondition(QueryCondition.name("vouchdate").lt(periodBeginDate));
        List<Map> resListVouchSatus = MetaDaoHelper.query(schema_fullname, queryVouchSatus.addCondition(queryConditionVouchSatus));
        if(resListVouchSatus!=null && !resListVouchSatus.isEmpty()){
            resultMap.put("VouchSatus",false);
        }else{
            resultMap.put("VouchSatus",true);
        }
    }

    @Override
    public void checkBeforeUpgradeTransType(String schema_fullname,Map<String , Boolean> resultMap) throws Exception {
        String ytenantid = InvocationInfoProxy.getTenantid().toString();
        //查询资金收付的交易类型 只看非预制数据
        //付款 CM.cmp_payment
        Map<String,String> transTypes_cmp_paymentMap = iTransTypeService.getTransTypesByFormId(ytenantid, "CM.cmp_payment")
                .stream().filter(transType -> transType.getPreset()==false).collect(Collectors.toMap(BdTransType::getCode, BdTransType::getName));
        //资金付款 CM.cmp_fundpayment
        Map<String,String> transTypes_cmp_fundpaymentMap = iTransTypeService.getTransTypesByFormId(ytenantid, "CM.cmp_fundpayment")
                .stream().filter(transType -> transType.getPreset()==false).collect(Collectors.toMap(BdTransType::getCode, BdTransType::getName));
        //收款 CM.cmp_receivebill
        Map<String,String> transTypes_cmp_receivebillMap = iTransTypeService.getTransTypesByFormId(ytenantid, "CM.cmp_receivebill")
                .stream().filter(transType -> transType.getPreset()==false).collect(Collectors.toMap(BdTransType::getCode, BdTransType::getName));
        //资金收款 CM.cmp_fundcollection
        Map<String,String> transTypes_cmp_fundcollectionMap = iTransTypeService.getTransTypesByFormId(ytenantid, "CM.cmp_fundcollection")
                .stream().filter(transType -> transType.getPreset()==false).collect(Collectors.toMap(BdTransType::getCode, BdTransType::getName));
        //一一对比 不一致报错
        resultMap.put("CheckPayTransType",true);
        resultMap.put("CheckRecTransType",true);
        for(String code : transTypes_cmp_paymentMap.keySet()){
            if(!transTypes_cmp_paymentMap.get(code).equals(transTypes_cmp_fundpaymentMap.get(code))){
                resultMap.put("CheckPayTransType",false);
                return ;
            }
        }

        for(String code : transTypes_cmp_receivebillMap.keySet()){
            if(!transTypes_cmp_receivebillMap.get(code).equals(transTypes_cmp_fundcollectionMap.get(code))){
                resultMap.put("CheckRecTransType",false);
                return ;
            }
        }
    }



    private void checkBeforeUpgradePayapplicationApprove(String schema_fullname,Map<String , Boolean> resultMap) throws Exception {
        QueryConditionGroup queryCondition = new QueryConditionGroup();
        QuerySchema query = QuerySchema.create().addSelect("id,code");
        queryCondition.addCondition(QueryCondition.name("verifystate").eq(VerifyState.SUBMITED.getValue()));
        List<Map> resList = MetaDaoHelper.query(PayApplicationBill.ENTITY_NAME, query.addCondition(queryCondition));
        if(!CollectionUtils.isEmpty(resList)){
            resultMap.put("CheckPayapplicationApprove",false);
        }else
            resultMap.put("CheckPayapplicationApprove",true);
    }

    private void checkBeforeUpgradeCmpBillApprove(String schema_fullname,Map<String , Boolean> resultMap) throws Exception {
        QueryConditionGroup queryCondition = new QueryConditionGroup();
        QuerySchema query = QuerySchema.create().addSelect("id,code");
        queryCondition.addCondition(QueryCondition.name("verifystate").eq(VerifyState.SUBMITED.getValue()));
        List<Map> resList = MetaDaoHelper.query(schema_fullname, query.addCondition(queryCondition));
        if(!CollectionUtils.isEmpty(resList)){
            resultMap.put("CheckCmpBillApprove",false);
        }else
            resultMap.put("CheckCmpBillApprove",true);
    }

    @Override
    public Map<String, Map<String, BdTransType>> getAllTransTypeMap() throws Exception {
        String ytenantid = InvocationInfoProxy.getTenantid().toString();
        //付款 CM.cmp_payment
        List<BdTransType> bdTransTypes_cmp_payment= iTransTypeService.getTransTypesByFormId(ytenantid, "CM.cmp_payment");
        //资金付款 CM.cmp_fundpayment
        List<BdTransType> bdTransTypes_cmp_fundpayment= iTransTypeService.getTransTypesByFormId(ytenantid, "CM.cmp_fundpayment");
        //收款 CM.cmp_receivebill
        List<BdTransType> bdTransTypes_cmp_receivebill= iTransTypeService.getTransTypesByFormId(ytenantid, "CM.cmp_receivebill");
        //资金收款 CM.cmp_fundcollection
        List<BdTransType> bdTransTypes_cmp_fundcollection= iTransTypeService.getTransTypesByFormId(ytenantid, "CM.cmp_fundcollection");
        //收付款工作台 id 与 交易类型对应
        Map <String , BdTransType> transTypeMapOld = new HashMap<>();
        for(BdTransType payment : bdTransTypes_cmp_payment){
            transTypeMapOld.put(payment.getId(),payment);
        }
        for(BdTransType receivebill : bdTransTypes_cmp_receivebill){
            transTypeMapOld.put(receivebill.getId(),receivebill);
        }
        //资金收付款 code 与 交易类型对应
        Map <String , BdTransType> transTypeMapNew = new HashMap<>();
        for(BdTransType fundpayment : bdTransTypes_cmp_fundpayment){
            transTypeMapNew.put(fundpayment.getCode(),fundpayment);
            //默认类型 也存sourceId
            if(fundpayment.getPreset()){
                transTypeMapNew.put(fundpayment.getSourceId(),fundpayment);
            }
        }
        for(BdTransType fundcollection : bdTransTypes_cmp_fundcollection){
            transTypeMapNew.put(fundcollection.getCode(),fundcollection);
            //默认类型 也存sourceId
            if(fundcollection.getPreset()){
                transTypeMapNew.put(fundcollection.getSourceId(),fundcollection);
            }
        }
        Map<String, Map<String, BdTransType>> resultMap = new HashMap<>();
        resultMap.put("transTypeMapOld",transTypeMapOld);
        resultMap.put("transTypeMapNew",transTypeMapNew);
        return resultMap;
    }

    @Override
    public String queryBdTransType(String id,Map<String, Map<String, BdTransType>> paramMap) throws Exception {
        String resultId = "";
        //收付款工作台 id 与 交易类型对应
        Map <String , BdTransType> transTypeMapOld = paramMap.get("transTypeMapOld");
        //资金收付款 code 与 交易类型对应
        Map <String , BdTransType> transTypeMapNew = paramMap.get("transTypeMapNew");

        //默认类型 可能会改name code,用sourceid进行对应
        if(transTypeMapOld.get(id).getPreset()){
            resultId = transTypeMapNew.get(sourceidMap.get(transTypeMapOld.get(id).getSourceId())).getId();
        }else{
            //自定义类型 name code 一定对应相同
            resultId = transTypeMapNew.get(transTypeMapOld.get(id).getCode()).getId();
        }
        return resultId;
    }

    @Override
    public List<String> assignCharMetaInfo(List<String> nameList, String newUri) throws Exception {
        if (nameList.isEmpty()) {
            return null;
        }

        List<Map<String, Object>> entityList = Lists.newArrayList();
        List<String> entityArr = Lists.newArrayList();
        entityArr.add(newUri);
        List<CharacterDTO> charList = charaterService.getSimpleCharacterDTOByCharacterCodes(nameList);
        Map<String, Object> entityParam = new HashMap<>();
        charList.forEach(characterDTO -> {
            Map<String, Object> character = new HashMap<>();
            character.put("characterCode", characterDTO.getCode());
            character.put("characterId", characterDTO.getId());
            character.put("entityUris", entityArr);
            entityList.add(character);
        });
        entityParam.put("characters", entityList);

        return batchCharactersAssignEntitys(entityParam);
    }

    private List<String> batchCharactersAssignEntitys(Map param) {
        String ytenantId = AppContext.getYTenantId();
        String resultJson = "";
        int tryNum = 0;
        String upuUrl = AppContext.getEnvConfig("domain.iuap-metadata-extend") + CHARS_ASSIGN_URI;
        while (tryNum < 5) {
            try {
                resultJson = HttpTookit.doPostWithJson(upuUrl, CtmJSONObject.toJSONString(param), null);
                CtmJSONObject result = CtmJSONObject.parseObject(resultJson);
                if (result.getJSONObject("data").getInteger("failCount") == 0) {
                    log.error("当前租户{}特征分配成功", ytenantId);
                    CtmJSONArray infoArray = result.getJSONObject("data").getJSONArray("infos");
                    List<LinkedHashMap> infoList = infoArray.toJavaList(LinkedHashMap.class);
                    List<String> resultList = new ArrayList<>();
                    for(LinkedHashMap map : infoList){
                        resultList.add(map.get("taskId").toString());
                    }
                    return resultList;
                } else {
                    if (tryNum++ > 3) {
                        String errMsg = MapUtils.getString(result.getJSONObject("data"),"messages");
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101667"),errMsg);
                    }
//                    else {
//                        Thread.sleep(2000);
//                    }
                }
            } catch (Exception e1) {
                log.error("租户【" + ytenantId + "】现金管理特征分配报错:", e1);
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101668"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1DBABB5405900032", "调用平台特征分配接口异常") /* "调用平台特征分配接口异常" */, e1);
            }
        }
        return null;
    }

    @Override
    public Map<String, Boolean> queryCharactersAssignResult(List<String> taskIdList) {
        Map<String, Boolean> resultMap = new HashMap<>();
        for(String taskId : taskIdList){
            String url = GET_ASSIGN_RESULT_URI + "?taskId=" + taskId;
            String resultJson = HttpTookit.doGet(url, null);
            log.error("【老升新】【特征分配】getAssignResult：{}", resultJson);

            CtmJSONObject resultJsonObj = CtmJSONObject.parseObject(resultJson);
            if (resultJsonObj == null) {
                log.error("【老升新】【特征分配】taskId:{}, fail to getAssignResult：{}", taskId, resultJson);
                resultMap.put(taskId,false);
            }else{
                CtmJSONObject taskJsonObj = resultJsonObj.getJSONObject("data").getJSONObject("taskPO");
                resultMap.put(taskId,OK_CODE.equals(taskJsonObj.getString("status")));
            }
        }
        return resultMap;
    }

    @Override
    public void afterPushSettleVoucher(BizObject bizBill,String bodyFullName) throws Exception {
        //查询是否启用事项中心
        boolean enableEVNT = AppContext.getBean(ICtmApplicationService.class).isEnableApp(CtmConstants.YONBIP_FI_EEAC_EVNT_APP_CODE);
        if (!enableEVNT) {
            return;
        }
        // 资金收付款单如果在现金参数里：是否结算成功时做账为是时，则先不过账，最后统一过账
        Map<String, Object> autoConfig = commonService.queryAutoConfigByAccentity(bizBill.getString(ICmpConstant.ACCENTITY));
        boolean isSettleSuccessToPost;
        if(autoConfig != null ){
            // 资金收付款单如果在现金参数里：是否结算成功时做账为是时，则先不过账，最后统一过账
            if(autoConfig.get("isSettleSuccessToPost") != null){
                isSettleSuccessToPost = (Boolean) autoConfig.get("isSettleSuccessToPost");
            }else{
                Map<String, Object> autoConfigTenant = commonService.queryAutoConfigTenant();
                isSettleSuccessToPost = autoConfigTenant != null && autoConfigTenant.get("isSettleSuccessToPost") != null && (Boolean) autoConfigTenant.get("isSettleSuccessToPost");
            }
        }else{
            Map<String, Object> autoConfigTenant = commonService.queryAutoConfigTenant();
            isSettleSuccessToPost = autoConfigTenant != null && autoConfigTenant.get("isSettleSuccessToPost") != null && (Boolean) autoConfigTenant.get("isSettleSuccessToPost");
        }
        if (!isSettleSuccessToPost){
            // 查询参数 凭证处理
            List<BizObject> bizBillBList = bizBill.get(bodyFullName);
            for(BizObject bizBillBody : bizBillBList){
                short caobject = Short.parseShort(bizBillBody.get("caobject").toString());
                if (caobject == CaObject.Customer.getValue()) {
                    bizBillBody.put(IBussinessConstant.CUSTOMER, bizBillBody.get("oppositeobjectid"));
                } else if (caobject == CaObject.Supplier.getValue()) {
                    bizBillBody.put(IBussinessConstant.SUPPLIER, bizBillBody.get("oppositeobjectid"));
                } else if (caobject == CaObject.Employee.getValue()) {
                    bizBillBody.put(IBussinessConstant.EMPLOYEE, bizBillBody.get("oppositeobjectid"));
                } else if (caobject == CaObject.CapBizObj.getValue()) {
                    bizBillBody.put(IBussinessConstant.FUNDBUSINOBJ, bizBillBody.get("oppositeobjectid"));
                }
            }
            bizBill.set("_entityName", FundPayment.ENTITY_NAME);
            CtmJSONObject billClue = new CtmJSONObject();
            billClue.put("classifier", null);
            billClue.put("srcBusiId", bizBill.getId().toString());
            cmpSendEventService.sendSimpleEvent(bizBill, billClue);
        }
    }

}
