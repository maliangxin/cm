package com.yonyou.yonbip.ctm.cmp.service.impl;

import com.google.common.collect.Lists;
import com.yonyou.iuap.ucf.common.i18n.MessageUtils;
import com.yonyou.ucf.mdd.common.enums.OperationTypeEnum;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.template.CommonOperator;
import com.yonyou.ucf.mdd.ext.bpm.model.VerifyState;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.basecom.itf.IFIBillService;
import com.yonyoucloud.fi.cm.transferaccount.TransferAccount;
import com.yonyoucloud.fi.cmp.accountrealtimebalance.AccountRealtimeBalance;
import com.yonyoucloud.fi.cmp.api.openapi.OpenApiService;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.initdata.InitData;
import com.yonyoucloud.fi.cmp.journal.JournalService;
import com.yonyoucloud.fi.cmp.paybill.PayBill;
import com.yonyoucloud.fi.cmp.receivebill.ReceiveBill;
import com.yonyoucloud.fi.cmp.salarypay.SalaryPayService;
import com.yonyoucloud.fi.cmp.salarypay.Salarypay;
import com.yonyoucloud.fi.cmp.settlement.Settlement;
import com.yonyoucloud.fi.cmp.util.CommonSqlExecutor;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import com.yonyoucloud.fi.cmp.voucher.CmpVoucherService;
import lombok.extern.slf4j.Slf4j;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.schema.*;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static com.yonyou.yonbip.ctm.error.CommonCtmErrorCode.ILLEGAL_ARGUMENT;
import static com.yonyou.yonbip.ctm.error.CommonCtmErrorCode.REMOTE_SERVICE_REST_EXCEPTION;
import static com.yonyoucloud.fi.cmp.constant.ICmpConstant.CONSTANT_EIGHT;
import static com.yonyoucloud.fi.cmp.constant.ICmpConstant.ID;

/**
 * Created by Administrator on 2019/4/20 0020.
 */
@Service
@Slf4j
public class OpenApiServiceImpl implements OpenApiService {


    @Autowired
    private IFIBillService fiBillService;

    @Autowired
    private SalaryPayService salaryPayService;

    @Autowired
    private CmpVoucherService cmpVoucherService;

    @Autowired
    private JournalService journalService;

    private static final String CHECKMAPPER = "com.yonyoucloud.fi.cmp.mapper.CheckMapper";

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(OpenApiServiceImpl.class);

    @Override
    public CtmJSONObject savePayment(BillDataDto bill) throws Exception {
        RuleExecuteResult e = (new CommonOperator(OperationTypeEnum.SAVE)).execute(bill);
        /*if (e.Result != 1) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102307"),e.message);
        }*/
        if (e.getMsgCode() != 1) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102308"),e.getMessage());
        } else {
            return null;
        }
    }

    @Override
    public List querystatusByIds(CtmJSONObject param) throws Exception {
        String pkstr = param.getString("srcpks");
        String[] pks = pkstr.split(pkstr);
        QuerySchema querySchema = QuerySchema.create().addSelect("*");
        QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("srcbillid").in(pks));
        querySchema.addCondition(group);
        List result = MetaDaoHelper.query(PayBill.ENTITY_NAME, querySchema);
        return result;
    }

    @Override
    public List deleteByIds(CtmJSONObject param) throws Exception {
        String pkstr = param.getString("srcpks");
        String[] pks = pkstr.split(",");
        QuerySchema querySchemaPayBill = QuerySchema.create().addSelect("*");
        QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("srcbillid").in(pks));
        querySchemaPayBill.addCondition(group);
        List<Map<String, Object>> result = MetaDaoHelper.query(PayBill.ENTITY_NAME, querySchemaPayBill);
        Set<String> srcBillidSet = new HashSet<String>();
        for (Map<String, Object> map : result) {
            PayBill payBill = new PayBill();
            payBill.init(map);
            if (payBill.getPaystatus().getValue() != 0 && payBill.getPaystatus().getValue() != 4
                    && payBill.getPaystatus().getValue() != 2) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100310"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180294","该单据已支付，不能修改或删除单据！") /* "该单据已支付，不能修改或删除单据！" */);
            }
            //已经日结的单据不能做修改删除
            QuerySchema querySchema = QuerySchema.create().addSelect("1");
            QueryConditionGroup group1 = QueryConditionGroup.and(QueryCondition.name("accentity").eq(map.get("accentity")), QueryCondition.name("settleflag").eq(true), QueryCondition.name("settlementdate").eq(map.get("vouchdate")));
            querySchema.addCondition(group1);
            List<Settlement> settlementList = MetaDaoHelper.query(Settlement.ENTITY_NAME, querySchema);
            if (ValueUtils.isNotEmpty(settlementList) && settlementList.size() > 0) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100311"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418029A","该单据已日结，不能修改或删除单据！") /* "该单据已日结，不能修改或删除单据！" */);
            }
            BillDataDto bill = new BillDataDto();
            bill.setBillnum("cmp_payment");
            bill.setData(payBill);
            Map<String, Object> partParam = new HashMap<String, Object>();
            partParam.put("outsystem", "1");
            bill.setPartParam(partParam);
            RuleExecuteResult e = (new CommonOperator(OperationTypeEnum.DELETE)).execute(bill);
            /*if (e.Result != 1) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102309"),e.message);
            }*/
            if (e.getMsgCode() != 1) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102310"),e.getMessage());
            }
        }
        MetaDaoHelper.batchDelete(PayBill.ENTITY_NAME, Lists.newArrayList(new SimpleCondition("srcbillid", ConditionOperator.in, pks)));
        return null;
    }

    @Override
    public String insertReceiveBill(CtmJSONObject param) throws Exception {
        CtmJSONArray billData = param.getJSONArray("data");
        BillDataDto billDataDto = new BillDataDto(IBillNumConstant.RECEIVE_BILL);
        for (int i = 0; i < billData.size(); i++) {
            billDataDto.setData(CtmJSONObject.toJSONString(billData.getJSONObject(i)));
            fiBillService.executeUpdate(OperationTypeEnum.SAVE.getValue(), billDataDto);
        }
        return ResultMessage.success();
    }

    @Override
    public String queryReceiveBillStatusByIds(CtmJSONObject param) throws Exception {
        List<String> srcBillId = param.getObject("srcpks", List.class);
        QuerySchema querySchemaReceiveBill = QuerySchema.create().addSelect("*");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("srcbillid").in(srcBillId));
        querySchemaReceiveBill.addCondition(conditionGroup);
        List<ReceiveBill> receiveBills = MetaDaoHelper.queryObject(ReceiveBill.ENTITY_NAME, querySchemaReceiveBill, null);
        return ResultMessage.data(receiveBills);
    }

    @Override
    public String deleteReceiveBillByIds(CtmJSONObject param) throws Exception {
        List<String> srcBillId = param.getObject("srcpks", List.class);
        QuerySchema querySchemaReceiveBill = QuerySchema.create().addSelect("*");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("srcbillid").in(srcBillId));
        querySchemaReceiveBill.addCondition(conditionGroup);
        List<ReceiveBill> receiveBills = MetaDaoHelper.queryObject(ReceiveBill.ENTITY_NAME, querySchemaReceiveBill, null);
        BillDataDto billDataDto = new BillDataDto(IBillNumConstant.RECEIVE_BILL);
        // begin_zhengweih_20201019_商业汇票删除收款单提示已经审批不能删除。
        Map<String, Object> partParam = new HashMap<String, Object>();
        partParam.put("outsystem", "1");
        billDataDto.setPartParam(partParam);
//        log.info("**********外部系统删除收款单，参数={}，参数={}", srcBillId, receiveBills);
        // end_zhengweih_20201019_商业汇票删除收款单提示已经审批不能删除。
        for (ReceiveBill receiveBill : receiveBills) {
            billDataDto.setData(receiveBill);
            fiBillService.executeUpdate(OperationTypeEnum.DELETE.getValue(), billDataDto);
        }
        return ResultMessage.success();
    }


    @Override
    public String deleteTransfer(CtmJSONObject param) throws Exception {
        String pkstr = param.getString("srcpks");
        String[] pks = pkstr.split(",");
        QuerySchema querySchemaTrans = QuerySchema.create().addSelect("*");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("srcbillid").in(pks));
        querySchemaTrans.addCondition(conditionGroup);
        List<TransferAccount> result = MetaDaoHelper.queryObject(TransferAccount.ENTITY_NAME, querySchemaTrans, null);
        BillDataDto billDataDto = new BillDataDto("cm_transfer_account");
        // 从外部系统删除转账单
        Map<String, Object> partParam = new HashMap<String, Object>();
        partParam.put("outsystem", "1");
        billDataDto.setPartParam(partParam);
        for (TransferAccount transferAccount : result) {
            billDataDto.setData(transferAccount);
            fiBillService.executeUpdate(OperationTypeEnum.DELETE.getValue(), billDataDto);
        }
        return ResultMessage.success();
    }

    @Override
    public String insertSalaryPay(CtmJSONObject param) throws Exception {
        // TODO Auto-generated method stub
        String decodeData = new String(Base64.getMimeDecoder().decode(param.get("data").toString()), "UTF-8");
        log.info("接收薪资系统传输数据decodeData：" + decodeData);
        BillDataDto bill = new BillDataDto("cmp_salarypay");
        bill.setData(decodeData);
        fiBillService.executeUpdate(OperationTypeEnum.SAVE.getValue(), bill);
        return ResultMessage.success();
    }

    @Override
    public String querySalaryPayStatusByIds(CtmJSONObject param) throws Exception {
        // TODO Auto-generated method stub
        List<String> srcBillId = param.getObject("srcpks", List.class);
        log.info("------外部服务调用薪资支付单查询接口begin------------");
        log.info("接收薪资系统传输数据srcBillId：" + srcBillId);
        String sendPayStatus = salaryPayService.sendPayStatus(srcBillId);
        log.info("返回薪资系统传输数据：" + sendPayStatus);
        log.info("---------外部服务调用薪资支付单查询接口end------------");
        return ResultMessage.data(sendPayStatus);
    }

    @Override
    @Transactional
    public String deleteSalaryPayByIds(CtmJSONObject param) throws Exception {
        // TODO Auto-generated method stub
        List<String> srcBillId = param.getObject("srcpks", List.class);
        log.info("---------外部服务调用薪资支付单删除接口begin------------");
        log.info("接收薪资系统传输数据srcBillId：" + srcBillId);
        QuerySchema querySchema = QuerySchema.create().addSelect("*");
        QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("srcbillid").in(srcBillId), QueryCondition.name("invalidflag").eq(0));
        querySchema.addCondition(group);
        List<Salarypay> receiveBills = MetaDaoHelper.queryObject(Salarypay.ENTITY_NAME, querySchema, null);
        BillDataDto billDataDto = new BillDataDto("cmp_salarypay");
        Map<String, Object> partParam = new HashMap<String, Object>();
        partParam.put("outsystem", "1");
        billDataDto.setPartParam(partParam);
        if (receiveBills != null && receiveBills.size() > 0) {
            for (Salarypay receiveBill : receiveBills) {
                billDataDto.setData(receiveBill);
                fiBillService.executeUpdate(OperationTypeEnum.DELETE.getValue(), billDataDto);
            }
        }
        log.info("---------外部服务调用薪资支付单删除接口end------------");
        return ResultMessage.success();
    }

    @Override
    @Transactional
    public String salaryPayCreate(CtmJSONObject param) throws Exception {
        // TODO Auto-generated method stub
        BillDataDto billDataDto = new BillDataDto("cmp_salarypay");
        String srcbillno = param.getString("billnum");
        Map<String, Object> partParam = new HashMap<String, Object>();
        partParam.put("outsystem", "1");
        partParam.put("srcbillno", srcbillno);
        billDataDto.setPartParam(partParam);
        log.info("---------外部服务调用薪资支付单插入接口begin------------");
        if (param.get("data") instanceof ArrayList) {
            ArrayList listarr = (ArrayList) param.get("data");
            for (int i = 0; i < listarr.size(); i++) {
                String decodeData = new String(Base64.getMimeDecoder().decode(listarr.get(i).toString()), "UTF-8");
                log.info("接收薪资系统传输数据decodeData：" + decodeData);
                billDataDto.setData(decodeData);
                fiBillService.executeUpdate(OperationTypeEnum.SAVE.getValue(), billDataDto);
            }
        } else if (param.get("data") instanceof CtmJSONArray) {
            CtmJSONArray billData = (CtmJSONArray) param.get("data");
            for (int i = 0; i < billData.size(); i++) {
                String decodeData = new String(Base64.getMimeDecoder().decode(billData.get(i).toString()), "UTF-8");
                log.info("接收薪资系统传输数据decodeData：" + decodeData);
                billDataDto.setData(decodeData);
                fiBillService.executeUpdate(OperationTypeEnum.SAVE.getValue(), billDataDto);
            }
        }
        log.info("---------外部服务调用薪资支付单插入接口end------------");
        return ResultMessage.success();
    }

    /**
     * <h2>根据账号查询期初数据</h2>
     *
     * @param param: 入参
     * @return java.lang.String
     * @author Sun GuoCai
     * @since 2021/3/31 19:28
     */
    @Override
    public String queryInitDataByAccountNo(CtmJSONObject param) throws Exception {
        try {
            String account = param.getString("account");
            if (!ValueUtils.isNotEmptyObj(account)) {
                return ResultMessage.error(ILLEGAL_ARGUMENT.build(), MessageUtils.getMessage("P_YS_FI_CM_0001237407") /* "根据账号查询期初数据失败!银行账号不能为空！" */);
            }
            String currency = param.getString("currency");
            QuerySchema querySchema = QuerySchema.create().addSelect("cobookoribalance,cobooklocalbalance,currency");
            QueryConditionGroup condition = new QueryConditionGroup();
            condition.addCondition(QueryConditionGroup.or(QueryCondition.name("cashaccount").eq(account),
                    QueryCondition.name("bankaccount").eq(account)));
            if (ValueUtils.isNotEmptyObj(currency)) {
                condition.addCondition(QueryConditionGroup.and(QueryCondition.name("currency").eq(currency)));
            }
            querySchema.addCondition(condition);
            List<Map<String, Object>> mapList = MetaDaoHelper.query(InitData.ENTITY_NAME, querySchema);
            return ResultMessage.data(mapList);
        } catch (Exception e) {
            return ResultMessage.error(REMOTE_SERVICE_REST_EXCEPTION.build(), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180297","根据账号查询期初数据失败!") /* "根据账号查询期初数据失败!" */);
        }
    }

    /**
     * <h2>根据传入付款工作台id修改单据凭证状态（报销凭证生成时调用）</h2>
     *
     * @param param: 入参
     * @return java.lang.String
     * @since 2021/4/26 19:28
     */
    @Override
    public String updatePayBillVoucherStatus(CtmJSONObject param) throws Exception {
        log.error("----------报账同步凭证状态到付款工作台----------");
        String srcbillid = param.getString("srcbillid");
        if (!ValueUtils.isNotEmptyObj(srcbillid)) {
            log.error("报账同步凭证状态到付款工作台：来源单据主键不能为空！");
            return ResultMessage.error(ILLEGAL_ARGUMENT.build(), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180298","来源单据主键不能为空！") /* "来源单据主键不能为空！" */);
        }
        String midvoucherstate = param.getString("midvoucherstate");
        if(!ValueUtils.isNotEmptyObj(midvoucherstate)){
            midvoucherstate = "success";
        }
        String sql ="select pb.id from cmp_paybill pb where pb.srcbillid = #{0}";
        Map<String,Object> parameters=new HashMap<>();
        parameters.put("0", srcbillid);
        CommonSqlExecutor metaDaoSupport = new CommonSqlExecutor(AppContext.getSqlSession());
        List<Map<String,Object>> list= metaDaoSupport.executeSelectSql(sql,parameters);
        if(list == null || list.size() == 0){
            log.error("报账同步凭证状态到付款工作台：根据来源单据主键查询付款单结果为空！");
            return ResultMessage.error(ILLEGAL_ARGUMENT.build(), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180293","根据来源单据主键查询付款单结果为空！") /* "根据来源单据主键查询付款单结果为空！" */);
        }
        Map<String, Object> paybill = list.get(0);
        CtmJSONObject voucherStatusInfo = new CtmJSONObject();
        voucherStatusInfo.put("billtypecode", "FICM2");
        voucherStatusInfo.put("midvoucherstate", midvoucherstate);
        voucherStatusInfo.put("billid", paybill.get("id"));
        voucherStatusInfo.put("systemCode", "fier"); // 友报账 费用管理过来的数据
        cmpVoucherService.updateVoucherStatus(voucherStatusInfo);
        return ResultMessage.success();
    }

    /**
     * 收款工作台中收款单审核 支持批量
     *
     * @param param 入参
     * @return java.lang.String
     * @throws Exception
     */
    @Override
    @Transactional
    public CtmJSONObject auditReceiveBill(CtmJSONObject param) throws Exception {
        CtmJSONArray rows = param.getJSONArray("data");
        //接收传入的id 用来判断
        Long id_judge = rows.getJSONObject(0).getLong("id");
        List<String> messages = new ArrayList<>();
        int failedCount = 0;
        //接受id的集合
        List<Object> ids = new ArrayList<>();
        //接受code的集合
        List<Object> codes = new ArrayList<>();
        //如果id不为空，则按照id进行查询并审核
        if (ValueUtils.isNotEmptyObj(id_judge)){
            for (int i = 0; i < rows.size(); i++) {
                CtmJSONObject rowData = rows.getJSONObject(i);
                Long id = rowData.getLong("id");
                ids.add(id);
            }
            QuerySchema querySchema = QuerySchema.create().addSelect("*");
            QueryConditionGroup queryConditionGroup = new QueryConditionGroup(ConditionOperator.and);
            queryConditionGroup.appendCondition(QueryCondition.name("id").in(ids));
            querySchema.addCondition(queryConditionGroup);
            List<Map<String, Object>> receiveBillList = MetaDaoHelper.query(ReceiveBill.ENTITY_NAME, querySchema);
            //判断 查询到的收款单是否为空
            if(receiveBillList == null || receiveBillList.size()==0 ){
                messages.add(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00073", "请选择单据！") /* "请选择单据！" */);
                failedCount = rows.size();
            }
            for (Map<String, Object> map: receiveBillList) {
                try {
                    map = receiveBillList.get(0);
                    ReceiveBill receiveBill = new ReceiveBill();
                    receiveBill.init(map);
                    BillDataDto bill = new BillDataDto();
                    bill.setBillnum("cmp_receivebill");
                    bill.setData(receiveBill);
                    Map<String, Object> partParam = new HashMap<String, Object>();
                    partParam.put("outsystem", "1");
                    bill.setPartParam(partParam);
                    fiBillService.executeUpdate(OperationTypeEnum.AUDIT.getValue(), bill);
                }catch (Exception e){
                    failedCount++;
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102311"),e.getMessage());
                }
            }
            return getJsonObject(rows, messages, failedCount);
        }else { //如果id为空，code不为空，则按照code进行查询并审核
            for (int i = 0; i < rows.size(); i++) {
                CtmJSONObject rowData = rows.getJSONObject(i);
                String code = rowData.getString("code");
                codes.add(code);
            }

            for (int j = 0; j < codes.size(); j++) {
                //通过code 去查询id
                String code1 = (String) codes.get(j);
                QuerySchema querySchema = QuerySchema.create().addSelect("id");
                QueryConditionGroup queryConditionGroup = new QueryConditionGroup(ConditionOperator.and);
                queryConditionGroup.appendCondition(QueryCondition.name("code").in(code1));
                querySchema.addCondition(queryConditionGroup);
                List<BizObject> bizObjectList = MetaDaoHelper.queryObject(ReceiveBill.ENTITY_NAME, querySchema, null);

                if (bizObjectList == null || bizObjectList.size() == 0){
                    messages.add(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00073", "请选择单据！") /* "请选择单据！" */);
                    failedCount++;
                }
                //判断哪个code 对应的id是不唯一的
                if (bizObjectList.size() > 1){
                    //todo 多语待修改
                    messages.add(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00073", "请选择单据！") /* "请选择单据！" */ + "code:" + codes.get(j));
                    failedCount++;
                }
                BizObject bizObject = bizObjectList.get(0);
                String nameStr = "id";
                String strId = String.valueOf(bizObject.getId());
                List<Map<String, Object>> receiveBillList = queryByIds(nameStr,strId);
                //判断 查询到的收款单是否为空
                if(receiveBillList == null || receiveBillList.size()==0 ){
                    messages.add(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00073", "请选择单据！") /* "请选择单据！" */);
                    failedCount++;
                }
                try {
                    Map<String,Object> map = receiveBillList.get(0);
                    ReceiveBill receiveBill = new ReceiveBill();
                    receiveBill.init(map);
                    BillDataDto bill = new BillDataDto();
                    bill.setBillnum("cmp_receivebill");
                    bill.setData(receiveBill);
                    Map<String, Object> partParam = new HashMap<String, Object>();
                    partParam.put("outsystem", "1");
                    bill.setPartParam(partParam);
                    fiBillService.executeUpdate(OperationTypeEnum.AUDIT.getValue(), bill);
                }catch (Exception e){
                    failedCount++;
                    //messages.add(e.getMessage());
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102312"),e.getMessage());
                }
            }
            return getJsonObject(rows, messages, failedCount);
        }

    }

    /**
     * 收款工作台中收款单弃审
     *
     * @param param 入参
     * @return java.lang.String
     * @throws Exception
     */
    @Override
    public String unauditReveiveBill(CtmJSONObject param) throws Exception {
        String srcbillid = param.getString("srcbillid");
        if (!ValueUtils.isNotEmptyObj(srcbillid)) {
            log.error("来源单据主键不能为空！");
            return ResultMessage.error(ILLEGAL_ARGUMENT.build(), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00074", "来源单据主键不能为空！") /* "来源单据主键不能为空！" */);
        }
        //通过srcbillid查询表单
        String nameStr = "srcbillid";
        List<Map<String, Object>> receiveBillList = queryByIds(nameStr,srcbillid);
        //判断 查询到的收款单是否为空
        if(receiveBillList == null || receiveBillList.size()==0 ){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102313"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00073", "请选择单据！") /* "请选择单据！" */);
        }
        for (Map<String, Object> map: receiveBillList) {
            try {
                ReceiveBill receiveBill = new ReceiveBill();
                receiveBill.init(map);
                BillDataDto bill = new BillDataDto();
                bill.setBillnum("cmp_receivebill");
                bill.setData(receiveBill);
                Map<String, Object> partParam = new HashMap<String, Object>();
                partParam.put("outsystem", "1");
                bill.setPartParam(partParam);
                fiBillService.executeUpdate(OperationTypeEnum.UNAUDIT.getValue(), bill);
            }catch (Exception e){
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102314"),e.getMessage());
            }
        }
        return ResultMessage.success();
    }

    /**
     * 据单据编号:code or 单据id:id or 来源单据id：srcbillid 查询收款单详细信息
     *
     * @param billNum
     * @param id
     * @param code
     * @param srcbillid
     * @return
     * @throws Exception
     */
    @Override
    public String queryReceiveBillByIdOrCodeOrSrcbillid(String billNum, Long id, String code, String srcbillid) throws Exception {
        String fullName = ReceiveBill.ENTITY_NAME;
        if (ValueUtils.isNotEmptyObj(id) && !ValueUtils.isNotEmptyObj(code) && !ValueUtils.isNotEmptyObj(srcbillid)){
            //todo
            BizObject bizObject = MetaDaoHelper.findById(fullName, id, 2);
            if (!ValueUtils.isNotEmptyObj(bizObject)) {
                return ResultMessage.error(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180292","单据不存在 id:") /* "单据不存在 id:" */
                        /* "单据不存在 id:" */ + id);
            }
            return ResultMessage.data(bizObject);
        }
        if (!ValueUtils.isNotEmptyObj(id) && ValueUtils.isNotEmptyObj(code) && !ValueUtils.isNotEmptyObj(srcbillid)){
            QuerySchema querySchema = QuerySchema.create().addSelect("id");
            querySchema.addCondition(QueryConditionGroup.and(QueryCondition.name("code").eq(code)));
            List<Map<String, Object>> mapList = MetaDaoHelper.query(fullName, querySchema);
            if (!ValueUtils.isNotEmptyObj(mapList)) {
                return ResultMessage.error(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180295","单据不存在 code:") /* "单据不存在 code:" */
                        /* "单据不存在 code:" */ + code);
            }
            Object mainId = mapList.get(0).get("id");
            BizObject bizObject = MetaDaoHelper.findById(fullName, mainId, 2);
            if (!ValueUtils.isNotEmptyObj(bizObject)) {
                return ResultMessage.error(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180292","单据不存在 id:") /* "单据不存在 id:" */
                        /* "单据不存在 id:" */ + id);
            }
            return ResultMessage.data(bizObject);
        }
        if (!ValueUtils.isNotEmptyObj(id) && !ValueUtils.isNotEmptyObj(code) && ValueUtils.isNotEmptyObj(srcbillid)){
            QuerySchema querySchema = QuerySchema.create().addSelect("id");
            querySchema.addCondition(QueryConditionGroup.and(QueryCondition.name("srcbillid").eq(srcbillid)));
            List<Map<String, Object>> mapList = MetaDaoHelper.query(fullName, querySchema);
            if (!ValueUtils.isNotEmptyObj(mapList)) {
                return ResultMessage.error(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180295","单据不存在 code:") /* "单据不存在 code:" */
                        /* "单据不存在 code:" */ + code);
            }
            Object mainId = mapList.get(0).get("id");
            BizObject bizObject = MetaDaoHelper.findById(fullName, mainId, 2);
            if (!ValueUtils.isNotEmptyObj(bizObject)) {
                return ResultMessage.error(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180292","单据不存在 id:") /* "单据不存在 id:" */
                        /* "单据不存在 id:" */ + id);
            }
            return ResultMessage.data(bizObject);
        }
        return ResultMessage.error("");
    }

    /**
     * 根据id删除薪资支付单
     * @param param 入参
     * @return
     * @throws Exception
     */
    @Override
    public CtmJSONObject deleteSalaryPayById(CtmJSONObject param) throws Exception {
        log.info("---------外部服务调用薪资支付单删除接口begin，request data ：{}------------",CtmJSONObject.toJSONString(param));
        CtmJSONArray rows = param.getJSONArray("data");
        int failedCount = 0;
        List<String> messages = new ArrayList<>();
        List<Object> ids = new ArrayList<>();
        Map<Long, BizObject> salarypayMap = new HashMap<>(CONSTANT_EIGHT);
        for (int i = 0; i < rows.size(); i++) {
            CtmJSONObject rowData = rows.getJSONObject(i);
            Long id = rowData.getLong(ID);
            ids.add(id);
        }
        //verifystate 审批流控制
        QuerySchema querySchema = QuerySchema.create().addSelect("id,verifystate");
        QueryConditionGroup queryConditionGroup = new QueryConditionGroup(ConditionOperator.and);
        queryConditionGroup.appendCondition(QueryCondition.name(ID).in(ids));
        querySchema.addCondition(queryConditionGroup);
        List<BizObject> bizObjectList = MetaDaoHelper.queryObject(Salarypay.ENTITY_NAME, querySchema, null);

        for (BizObject bizObject: bizObjectList) {
            salarypayMap.put(bizObject.getId(), bizObject);
        }
        List<Long> deleteIds = new ArrayList<>(rows.size());
        for (int i = 0; i < rows.size(); i++) {
            CtmJSONObject row = rows.getJSONObject(i);
            BizObject bizObject = salarypayMap.get(row.getLong("id"));
            if (!ValueUtils.isNotEmptyObj(bizObject)){
                messages.add(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00076", "单据不存在 id:") /* "单据不存在 id:" */ + rows.getJSONObject(i).getLong("id"));
                failedCount++;
                if (rows.size() == 1){
                    return getJsonObject(rows,messages,failedCount);
                }
                continue;
            }
            if (ValueUtils.isNotEmptyObj(bizObject.get("verifystate"))){
                short verifyState = Short.parseShort(bizObject.get("verifystate").toString());
                if (verifyState == VerifyState.SUBMITED.getValue()
                        || verifyState == VerifyState.COMPLETED.getValue()){
                    messages.add(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00075", "单据id: [%s]，当前单据状态不允许删除") /* "单据id: [%s]，当前单据状态不允许删除" */, rows.getJSONObject(i).getLong("id")));
                    failedCount++;
                    if (rows.size() == 1){
                        return getJsonObject(rows, messages, failedCount);
                    }
                    continue;
                }
            }
            deleteIds.add(bizObject.getId());
        }
        if (ValueUtils.isNotEmptyObj(deleteIds)){
            MetaDaoHelper.batchDelete(Salarypay.ENTITY_NAME,
                    Lists.newArrayList(new SimpleCondition("id", ConditionOperator.in, deleteIds.toArray(new Long[0]))));
        }
        log.info("---------外部服务调用薪资支付单删除接口end------------");
        return getJsonObject(rows, messages, failedCount);

    }

    /**
     * 批量查询期初余额
     * @param param
     * @return
     */
    @Override
    public List<Map<String, Object>> queryBatchInitData(List<Map<String, Object>> param) throws Exception{
        QuerySchema querySchema = QuerySchema.create().addSelect("cobookoribalance,cobooklocalbalance,currency");
        for(Map<String, Object> item:param){
            QueryConditionGroup condition = new QueryConditionGroup();
            condition.addCondition(QueryConditionGroup.or(QueryCondition.name("bankaccount").eq(item.get("bankaccount"))));
            condition.addCondition(QueryConditionGroup.and(QueryCondition.name("currency").eq(item.get("currency"))));
            querySchema.addCondition(condition);
            List<Map<String, Object>> mapList = MetaDaoHelper.query(InitData.ENTITY_NAME, querySchema);
            if(!mapList.isEmpty()){
                item.put("balance",mapList.get(0).get("cobookoribalance"));
            }
        }

        return param;
    }
    /**
     * 查询银行实时余额 - 数据库里，非银企联实时余额
     * @param param
     * @return
     */
    @Override
    public List<Map<String, Object>> queryRealtimeBalance(List<Map<String, Object>> param) throws Exception{
        QuerySchema querySchema = QuerySchema.create().addSelect("avlbal");
        querySchema.addOrderBy(new QueryOrderby("balancedate","desc"));
        querySchema.addOrderBy(new QueryOrderby("pubts","desc"));
        querySchema.addPager(0,1);
        for(Map<String, Object> item:param){
            QueryConditionGroup condition = new QueryConditionGroup();
            condition.addCondition(QueryConditionGroup.or(QueryCondition.name("enterpriseBankAccount").eq(item.get("bankaccount"))));
            condition.addCondition(QueryConditionGroup.and(QueryCondition.name("currency").eq(item.get("currency"))));
            querySchema.addCondition(condition);
            List<Map<String, Object>> mapList = MetaDaoHelper.query(AccountRealtimeBalance.ENTITY_NAME, querySchema);
            if(!mapList.isEmpty()){
                item.put("avlbal",mapList.get(0).get("avlbal"));
            }
        }

        return param;
    }

    /**
     * 根据收款单某个字段（id、srcbillid、单据编号 等）进行条件查询的方法
     *
     * @param name 入参
     * @return
     */
    public List<Map<String, Object>> queryByIds(String nameStr, String name) throws Exception{
        String[] ids = name.split(",");
        QuerySchema querySchema = QuerySchema.create().addSelect("*");
        QueryConditionGroup queryConditionGroup = new QueryConditionGroup(ConditionOperator.and);
        queryConditionGroup.appendCondition(QueryCondition.name(nameStr).in(ids));
        querySchema.addCondition(queryConditionGroup);
        List<Map<String, Object>> resultList = MetaDaoHelper.query(ReceiveBill.ENTITY_NAME, querySchema);
        return resultList;
    }

    /**
     * 设置OpenAPI的返回信息
     * @param rows 入参信息
     * @param messages 错误信息
     * @param failedCount 失败行数
     * @return
     */
    private CtmJSONObject getJsonObject(CtmJSONArray rows, List<String> messages, int failedCount) {
        CtmJSONObject responseData = new CtmJSONObject();
        responseData.put("count", rows.size());
        responseData.put("successCount", rows.size() - failedCount);
        responseData.put("failCount", failedCount);
        responseData.put("messages", messages);
        responseData.put("infos", rows);
        return responseData;
    }

}
