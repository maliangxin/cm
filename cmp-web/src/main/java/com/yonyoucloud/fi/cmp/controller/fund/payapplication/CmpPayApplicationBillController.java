package com.yonyoucloud.fi.cmp.controller.fund.payapplication;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.iuap.event.model.BusinessEventBuilder;
import com.yonyou.iuap.yonscript.support.utils.cryptogram.Base64Utils;
import com.yonyou.ucf.mdd.ext.base.BaseController;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.controller.Authentication;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.dao.sql.SqlHelper;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliationbusrelation_b;
import com.yonyoucloud.fi.cmp.cmpentity.FundSettleStatus;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.json.JsonUtils;
import com.yonyou.yonbip.ctm.util.JSONBuilderUtils;
import com.yonyoucloud.fi.basecom.util.auth.CMPDiworkPermission;
import com.yonyoucloud.fi.basecom.utils.HttpTookit;
import com.yonyoucloud.fi.cmp.cmpentity.VoucherStatus;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import com.yonyoucloud.fi.cmp.event.sendEvent.ICmpSendEventService;
import com.yonyoucloud.fi.cmp.fundcollection.FundCollection;
import com.yonyoucloud.fi.cmp.fundcollection.FundCollection_b;
import com.yonyoucloud.fi.cmp.fundcommon.service.IFundCommonService;
import com.yonyoucloud.fi.cmp.fundpayment.FundPayment;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.api.payapplybill.CtmPayApplicationBillService;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.PurchaseOrderVo;
import com.yonyoucloud.fi.cmp.payapplicationbill.service.CmpPayApplicationBillService;
import com.yonyoucloud.fi.cmp.stwb.StwbBillService;
import com.yonyoucloud.fi.cmp.task.payapplybill.ChangePayApplyDateDays;
import com.yonyoucloud.fi.cmp.util.*;
import com.yonyoucloud.fi.cmp.vo.ResultMessageVO;
import lombok.extern.slf4j.Slf4j;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QueryOrderby;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;
import java.util.stream.Collectors;

import static com.yonyoucloud.fi.cmp.constant.ICmpConstant.CONSTANT_EIGHT;

/**
 * <h1>付款申请单Handler</h1>
 *
 * @author GuoCai Sun
 * @version 1.0
 * @since 2020-11-18 10:09
 */
@Controller
@RequestMapping("/cmp/pay-application-bill")
@Slf4j
public class CmpPayApplicationBillController extends BaseController {
    private CmpPayApplicationBillService cmpPayApplicationBillService;
    private ChangePayApplyDateDays changePayApplyDateDays;
    private IFundCommonService fundCommonService;
    private ICmpSendEventService cmpSendEventService;
    private CtmPayApplicationBillService payApplicationBillService;

    @Autowired
    public void setCmpPayApplicationBillService(CmpPayApplicationBillService cmpPayApplicationBillService) {
        this.cmpPayApplicationBillService = cmpPayApplicationBillService;
    }

    @Autowired
    public void setChangePayApplyDateDays(ChangePayApplyDateDays changePayApplyDateDays) {
        this.changePayApplyDateDays = changePayApplyDateDays;
    }

    @Autowired
    public void setFundCommonService(IFundCommonService fundCommonService) {
        this.fundCommonService = fundCommonService;
    }

    @Autowired
    public void setCmpSendEventService(ICmpSendEventService cmpSendEventService) {
        this.cmpSendEventService = cmpSendEventService;
    }

    @Autowired
    public void setPayApplicationBillService(CtmPayApplicationBillService payApplicationBillService) {
        this.payApplicationBillService = payApplicationBillService;
    }

    @Qualifier("stwbCollectionBillServiceImpl")
    @Autowired
    StwbBillService collStwbBillService;

    @Qualifier("stwbPaymentBillServiceImpl")
    @Autowired
    private StwbBillService payStwbBillService;

    /**
     * <h2>手动关闭付款申请单</h2>
     *
     * @param bill:     入参
     * @param response: 响应体
     * @author Sun GuoCai
     * @since 2020/11/18 10:51
     */
    @PostMapping("/closed")
    @CMPDiworkPermission({IServicecodeConstant.PAYAPPLICATIONBILL})
    public void updatePayApplicationBillStatusByClosed(@RequestBody BillDataDto bill, HttpServletResponse response) throws Exception {
        Map<String, Object> params = (Map<String, Object>) bill.getData();
        String ids = ValueUtils.isNotEmptyObj(params.get("ids")) ? params.get("ids").toString() : null;
        if (!ValueUtils.isNotEmptyObj(ids)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100105"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418005F", "操作的单据ID不能为空！") /* "操作的单据ID不能为空！" */);
        }
        String closedStatus = ValueUtils.isNotEmptyObj(params.get("closedStatus")) ? params.get("closedStatus").toString() : null;
        if (!ValueUtils.isNotEmptyObj(closedStatus)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100106"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418005E", "参数错误！") /* "参数错误！" */);
        }
        ResultMessageVO result = cmpPayApplicationBillService.updatePayApplicationBillStatusByClosed(ids, closedStatus);
        log.info("closed pay Apply bill. response parameter : {}", result);
        renderJson(response, ResultMessage.data(result));
    }


    @PostMapping("deletePayApplyBillByIds")
    public void deletePayApplyBillByIds(@RequestBody BillDataDto param, HttpServletResponse response) throws Exception {
        ResultMessageVO jsonObject = cmpPayApplicationBillService.deletePayApplyBillByIds(param);
        renderJson(response, ResultMessage.data(jsonObject));
    }

    @PostMapping("/updatePaymentDaysTask")
    @Authentication(value = false, readCookie = true)
    public void updateDistanceProposePaymentDateDaysTask(@RequestBody(required = false) Map<String, Object> paramMap, HttpServletRequest request, HttpServletResponse response) throws Exception {
        String tenantId = Optional.ofNullable(request.getHeader("tenantId")).orElse("");
        String userId = Optional.ofNullable(request.getHeader("userId")).orElse("");
        String logId = Optional.ofNullable(request.getHeader("logId")).orElse("");
        if (null == paramMap) {
            paramMap = new HashMap<>();
        }
        paramMap.put("tenantId", tenantId);
        paramMap.put("userId", userId);
        paramMap.put("logId", logId);
        Map<String, Object> result = changePayApplyDateDays.updateDistanceProposePaymentDateDaysTask(paramMap);
        log.info("updateDistanceProposePaymentDateDaysTask. response parameter : {}", result);
        renderJson(response, JSONBuilderUtils.mapToString(result));
    }

    @PostMapping("/getObjectContent")
    public void getObjectContent(@RequestBody CtmJSONObject param, HttpServletResponse response) throws Exception {
        log.info("getObjectContent, param = {}", param);
        if (!ValueUtils.isNotEmptyObj(param.get("type"))) {
            renderJson(response, ResultMessage.data("type field is not empty!"));
        }
        String type = String.valueOf(param.get("type"));
        String fullName = String.valueOf(param.get("fullName"));
        if ("query".equals(type)) {
            String domain = String.valueOf(param.get("domain"));
            String select = String.valueOf(param.get("select"));
            int pageIndex = Integer.parseInt(param.get("pageIndex").toString());
            int pageSize = Integer.parseInt(param.get("pageSize").toString());
            BillContext context = new BillContext();
            context.setFullname(fullName);
            context.setDomain(domain);
            QuerySchema schema = QuerySchema.create();
            schema.addSelect(select);
            CtmJSONArray conditionArray = param.getJSONArray("conditionArray");
            if (ValueUtils.isNotEmptyObj(conditionArray)) {
                for (int i = 0; i < conditionArray.size(); i++) {
                    CtmJSONObject jsonObj = conditionArray.getJSONObject(i);
                    String condition = jsonObj.getString("condition");
                    String key1 = jsonObj.getString("key1");
                    Object value1 = jsonObj.get("value1");
                    if ("eq".equals(condition)) {
                        schema.appendQueryCondition(QueryCondition.name(key1).eq(value1));
                    }
                    if ("like".equals(condition)) {
                        schema.appendQueryCondition(QueryCondition.name(key1).like(value1));
                    }
                    if ("not_eq".equals(condition)) {
                        schema.appendQueryCondition(QueryCondition.name(key1).not_eq(value1));
                    }
                    if ("between".equals(condition)) {
                        Object value2 = jsonObj.get("value2");
                        schema.appendQueryCondition(QueryCondition.name(key1).between(value1, value2));
                    }
                    if ("in".equals(condition)) {
                        String[] split = String.valueOf(value1).split(",");
                        schema.appendQueryCondition(QueryCondition.name(key1).in((Object) split));
                    }
                }
            }
            schema.addPager(pageIndex, pageSize);
            CtmJSONObject orderBy = param.getJSONObject("orderBy");
            if (ValueUtils.isNotEmptyObj(orderBy)) {
                schema.addOrderBy(new QueryOrderby(String.valueOf(orderBy.get("id")), String.valueOf(orderBy.get("order"))));
            }
            log.info("getObjectContent, schema = {}", schema);
            List<Map<String, Object>> result = MetaDaoHelper.query(context, schema);
            renderJson(response, ResultMessage.data(result));
        } else {
            Object id = param.get("id");
            int depth = Integer.parseInt(param.get("depth").toString());
            BizObject bizObject = MetaDaoHelper.findById(fullName, id, depth);
            renderJson(response, ResultMessage.data(bizObject));
        }
    }

    @PostMapping("/updateFundBillVoucherNo")
    public void updateFundBillVoucherNo(@RequestBody CtmJSONObject param, HttpServletResponse response) throws Exception {
        log.info("getObjectContent, param = {}", param);
        String tableName = String.valueOf(param.get("tableName"));
        String voucherNo = String.valueOf(param.get("voucherNo"));
        Long id = Long.valueOf(String.valueOf(param.get("id")));
        int nogen = Integer.parseInt(param.get("nogen").toString());
        Map<String, Object> params = new HashMap<>();
        params.put("tableName", tableName);
        params.put("id", id);
        params.put("voucherNo", voucherNo);
        if (nogen == 0) {
            params.put("voucherstatus", VoucherStatus.Empty.getValue());
        } else if (nogen == 1) {
            params.put("voucherstatus", VoucherStatus.Created.getValue());
        }
        int updateCount = SqlHelper.update("com.yonyoucloud.fi.cmp.voucher.updateVoucherStatus", params);
        renderJson(response, ResultMessage.data(updateCount));
    }

    @PostMapping("/updateJournalBillVoucherNo")
    public void updateJournalBillVoucherNo(@RequestBody CtmJSONObject param, HttpServletResponse response) throws Exception {
        log.info("updateJournalBillVoucherNo, param = {}", param);
        String voucherNo = String.valueOf(param.get("voucherNo"));
        String voucherPeriod = String.valueOf(param.get("voucherPeriod"));
        Long srcbillitemid = Long.valueOf(String.valueOf(param.get("srcbillitemid")));
        Map<String, Object> params = new HashMap<>();
        params.put("id", srcbillitemid);
        params.put("voucherNo", voucherNo);
        params.put("voucherPeriod", voucherPeriod);
        int updateCount = SqlHelper.update("com.yonyoucloud.fi.cmp.voucher.updateVoucherNoOfJournal", params);
        renderJson(response, ResultMessage.data(updateCount));
    }

    @PostMapping("/queryFundBillVoucherNo")
    public void queryFundBillVoucherNo(@RequestBody CtmJSONObject param, HttpServletResponse response) {
        String billtypecode = String.valueOf(param.get("billtypecode"));
        String id = String.valueOf(param.get("id"));
        // String upuUrl = AppContext.getEnvConfig("fiotp.servername");
        Map<String, Object> params = new HashMap<>(CONSTANT_EIGHT);
        List<Map<String, Object>> data = new ArrayList<>(CONSTANT_EIGHT);
        Map<String, Object> map = new HashMap<>(CONSTANT_EIGHT);
        map.put("billid", id);
        map.put("billtypecode", billtypecode);
        data.add(map);
        params.put("data", data);
        String json = JsonUtils.toJSON(params);
        String serverUrl = AppContext.getEnvConfig("fiotp.servername");
        String responseStr = HttpTookit.doPostWithJson(serverUrl + "/exchanger/linkvoucher", json, new HashMap<>());
        CtmJSONObject resultJson = CtmJSONObject.parseObject(responseStr);
        if (resultJson.getBoolean("success")) {
            CtmJSONArray jsonArray = resultJson.getJSONArray("data");
            if (jsonArray.size() > 0) {
                CtmJSONObject jsonObject = jsonArray.getJSONObject(0);
                renderJson(response, ResultMessage.data(jsonObject.get("voucher_no")));
            }
        } else {
            log.error("by mq message update fund payment or collection bill voucherNo fail, billId = {}, billTypeCode = {}"
                    , id, billtypecode);
            renderJson(response, ResultMessage.error(resultJson.getString("message")));
        }
    }


    @PostMapping("/getbillcodesbyvoucherids")
    public void getbillcodesbyvoucherids(@RequestBody CtmJSONObject param, HttpServletResponse response) throws JsonProcessingException {
        List<String> ids = (List<String>) param.get("ids");
        if (!ValueUtils.isNotEmptyObj(ids)) {
            renderJson(response, ResultMessage.data("ids field is not empty!"));
        }
        if (ValueUtils.isNotEmptyObj(ids)) {
            CtmJSONObject json = new CtmJSONObject();
            json.put("ids", ids);
            String serverUrl = AppContext.getEnvConfig("yzb.base.url");
            String responseStr = HttpTookit.doPostWithJson(serverUrl + "/voucher/getbillcodesbyvoucherids", json.toString(), new HashMap<>());
            CtmJSONObject jsonObject = CtmJSONObject.parseObject(responseStr);
            CtmJSONArray jsonResult = jsonObject.getJSONArray("data");
            Map<String, Object> map = new HashMap<>();
            for (int i = 0; i < jsonResult.size(); i++) {
                CtmJSONObject resultJSONObject = jsonResult.getJSONObject(i);
                map.put(String.valueOf(resultJSONObject.get("id")), resultJSONObject.get("displayname"));
            }
            renderJson(response, ResultMessage.data(map));
        }
    }

    @PostMapping("/beforeCopy")
    public void beforeCopy(@RequestBody CtmJSONObject param, HttpServletResponse response) throws Exception {
        String accentity = String.valueOf(param.get("accentity"));
        cmpPayApplicationBillService.payapplicationCopyBill(accentity);
        renderJson(response, ResultMessage.success());
    }


//    @PostMapping("/updateByParam")
//    public void updatePayApplicationBillByReqParam(@RequestBody CtmJSONObject params, HttpServletResponse response) throws Exception {
//        if (!ValueUtils.isNotEmptyObj(params)) {
//            renderJson(response, ResultMessage.error("not param!"));
//            return;
//        }
//        String yhtTenantId = String.valueOf(params.get("yhtTenantId"));
//        if (!ValueUtils.isNotEmptyObj(yhtTenantId)) {
//            renderJson(response, ResultMessage.error("not yhtTenantId!"));
//            return;
//        }
//        String yTenantId = InvocationInfoProxy.getTenantid();
//        if (!yTenantId.equals(yhtTenantId)) {
//            renderJson(response, ResultMessage.error("not authentication!"));
//            return;
//        }
//
//        String date = DateUtils.convertToStr(new Date(), "yyyyMMdd");
//        String md5Str = SHA512Util.getSHA512Str(date + "yonbip-fi-ctmcmp");
//        String signature = String.valueOf(params.get("signature"));
//        if (!md5Str.equalsIgnoreCase(signature)) {
//            renderJson(response, ResultMessage.error("not pass!"));
//            return;
//        }
//        String operator = String.valueOf(params.get("operator"));
//        if (!ValueUtils.isNotEmptyObj(operator)) {
//            renderJson(response, ResultMessage.error("not operator!"));
//            return;
//        }
//
//        String executeSql = String.valueOf(params.get("executeSql"));
//        String sql = new String(Base64Utils.decode(executeSql));
//        if (!ValueUtils.isNotEmptyObj(executeSql)) {
//            renderJson(response, ResultMessage.error("not executeSql!"));
//            return;
//        }
//
//        if (executeSql.contains(";")) {
//            renderJson(response, ResultMessage.error("executeSql assemble failure!"));
//            return;
//        }
//        CommonSqlExecutor metaDaoSupport = new CommonSqlExecutor(AppContext.getSqlSession());
//        if ("update".equals(operator) && sql.startsWith("update")) {
//            metaDaoSupport.executeSql(sql);
//            renderJson(response, ResultMessage.success("update success!"));
//        } else if ("delete".equals(operator) && sql.startsWith("delete")) {
//            metaDaoSupport.executeSql(sql);
//            renderJson(response, ResultMessage.success("delete success!"));
//        } else if ("insert".equals(operator) && sql.startsWith("insert")) {
//            metaDaoSupport.executeInsertSql(sql);
//            renderJson(response, ResultMessage.success("insert success!"));
//        } else {
//            renderJson(response, ResultMessage.error("executeSql is wrongful!"));
//            return;
//        }
//    }


    @PostMapping("/rePushSettledata")
    public void rePushSettledata(@RequestBody CtmJSONObject params, HttpServletResponse response) throws Exception {
        if (!ValueUtils.isNotEmptyObj(params)) {
            renderJson(response, ResultMessage.error("not param!"));
            return;
        }
        String billIdsStr = String.valueOf(params.get("ids"));
        String subIdsStr = String.valueOf(params.get("subIds"));
        String billnum = String.valueOf(params.get("billnum"));
        String[] billIds = new String[0];
        String[] subIds = new String[0];
        if (StringUtils.isNotEmpty(billIdsStr)) {
            billIds = billIdsStr.split(",");
        }
        if (StringUtils.isNotEmpty(subIdsStr)) {
            subIds = subIdsStr.split(",");
        }
        if (billIds.length > 5) {
            renderJson(response, ResultMessage.error("ids too much!"));
            return;
        }
        if (billIds.length == 0) {
            renderJson(response, ResultMessage.error("ids is empty!"));
            return;
        }
        if ("cmp_fundpayment".equals(billnum)) {
            for (int i = 0; i < billIds.length; i++) {
                FundPayment currentBill = MetaDaoHelper.findById(FundPayment.ENTITY_NAME, billIds[i]);
                List<BizObject> currentBillList = new ArrayList<>();
                if (!changeDetailSettleStatus(currentBill, billnum, subIds)) {
                    continue;
                }
                currentBillList.add(currentBill);
                payStwbBillService.pushBill(currentBillList, false);
            }
        } else if ("cmp_fundcollection".equals(billnum)) {
            for (int i = 0; i < billIds.length; i++) {
                FundCollection currentBill = MetaDaoHelper.findById(FundCollection.ENTITY_NAME, billIds[i]);
                List<BizObject> currentBillList = new ArrayList<>();
                if (!changeDetailSettleStatus(currentBill, billnum, subIds)) {
                    continue;
                }
                currentBillList.add(currentBill);
                collStwbBillService.pushBill(currentBillList, false);
            }
        }
        renderJson(response, ResultMessage.success("rePushSettledata success!"));
    }

    private boolean changeDetailSettleStatus(BizObject bizObject, String billnum, String[] subIds) {
        List<BizObject> subBizObjects = null;
        if ("cmp_fundpayment".equals(billnum)) {
            subBizObjects = bizObject.getBizObjects("FundPayment_b", BizObject.class);
        } else if ("cmp_fundcollection".equals(billnum)) {
            subBizObjects = bizObject.getBizObjects("FundCollection_b", BizObject.class);
        }
        if (subBizObjects == null || subBizObjects.isEmpty()) {
            return false;
        }
        List<String> subIdList = Arrays.asList(subIds);
        if (!subIdList.isEmpty()) {
            subBizObjects = subBizObjects.stream().filter(subItem -> subIdList.contains(bizObject.getId().toString())).collect(Collectors.toList());
        }
        for (BizObject subBizObject : subBizObjects) {
            subBizObject.set("settlestatus", FundSettleStatus.SettlementSupplement.getValue());
        }
        if ("cmp_fundpayment".equals(billnum)) {
            bizObject.set("FundPayment_b", subBizObjects);
        } else if ("cmp_fundcollection".equals(billnum)) {
            bizObject.set("FundCollection_b", subBizObjects);
        }
        return true;
    }

    @PostMapping("/generatorFundCollectionBill")
    public void generatorFundCollectionBill(@RequestBody CtmJSONObject params, HttpServletResponse response) {
        Object id = params.get("id");
        if (!ValueUtils.isNotEmptyObj(id)) {
            renderJson(response, ResultMessage.error("not id!"));
            return;
        }
        String primaryId = String.valueOf(id);
        CtmJSONObject jsonObject = cmpPayApplicationBillService.generatorFundCollectionBill(primaryId);
        renderJson(response, ResultMessage.data(jsonObject));
    }

    @GetMapping("/post/{subId}/{entityNameSub:.+}/{entityNameMaster:.+}")
    public void billPostSub(@PathVariable Long subId, @PathVariable String entityNameSub, @PathVariable String entityNameMaster, HttpServletResponse response) throws Exception {
        BizObject bizObject = MetaDaoHelper.findById(entityNameSub, subId, 2);
        fundCommonService.generateVoucher(bizObject, entityNameMaster,false);
        renderJson(response, ResultMessage.success());
    }

    @GetMapping("/post/{mainId}/{entityNameMaster:.+}")
    public void billPostAll(@PathVariable Long mainId, @PathVariable String entityNameMaster, HttpServletResponse response) throws Exception {
        BizObject bizObject = MetaDaoHelper.findById(entityNameMaster, mainId, 3);
        bizObject.set("_entityName", entityNameMaster);
        cmpSendEventService.sendEvent(bizObject);
        renderJson(response, ResultMessage.success());
    }

    @GetMapping("/cancelPost/{mainId}/{entityNameMaster:.+}")
    public void cancelBillPost(@PathVariable Long mainId, @PathVariable String entityNameMaster, HttpServletResponse response) throws Exception {
        BizObject bizObject = MetaDaoHelper.findById(entityNameMaster, mainId, 3);
        bizObject.set("_entityName", entityNameMaster);
        cmpSendEventService.deleteEvent(bizObject);
        renderJson(response, ResultMessage.success());
    }

    @PostMapping("/sendMsg")
    public void sendMsg(@RequestBody CtmJSONObject params, HttpServletResponse response) throws Exception {
        BusinessEventBuilder businessEventBuilder = new BusinessEventBuilder();
        businessEventBuilder.setSourceId(params.get("sourceId").toString());
        businessEventBuilder.setEventType(params.get("eventType").toString());
        businessEventBuilder.setUserObject(params.get("message"));
        businessEventBuilder.setBillId(params.get("id").toString());
        businessEventBuilder.setBillCode(params.get("code").toString());
        SendEventMessageUtils.sendEventMessageEosByParams(businessEventBuilder);
    }

    @PostMapping("/queryPayApplicationBillByPurchaseOrderId")
    public void queryPayApplicationBillByPurchaseOrderId(@RequestBody CtmJSONObject params, HttpServletResponse response) throws Exception {
        String orderIdsStr = params.getString("orderIds");
        PurchaseOrderVo purchaseOrderVo = new PurchaseOrderVo();
        if (ValueUtils.isNotEmptyObj(orderIdsStr)) {
            String[] orderIds = orderIdsStr.split(",");
            purchaseOrderVo.setOrderIds(Arrays.asList(orderIds));
        }
        String orderItemIdsStr = params.getString("orderItemIds");
        if (ValueUtils.isNotEmptyObj(orderItemIdsStr)) {
            String[] orderItemIds = orderItemIdsStr.split(",");
            purchaseOrderVo.setOrderItemIds(Arrays.asList(orderItemIds));
        }
        renderJson(response, ResultMessage.data(payApplicationBillService.queryPayApplicationBillByPurchaseOrderId(purchaseOrderVo)));
    }
}
