package com.yonyoucloud.fi.cmp.controller.billclaim.bankreconciliation;


import com.yonyou.diwork.permission.annotations.DiworkPermission;
import com.yonyou.iuap.tenant.sdk.UserCenter;
import com.yonyou.uap.tenant.service.itf.ITenantRoleUserService;
import com.yonyou.uap.tenantauth.entity.TenantRole;
import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVO;
import com.yonyou.ucf.mdd.ext.base.BaseController;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyoucloud.fi.cmp.bankidentify.BankIdentifyService;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.basecom.util.auth.CMPDiworkPermission;
import com.yonyoucloud.fi.cmp.accountrealtimebalance.AccountRealtimeBalance;
import com.yonyoucloud.fi.cmp.autoparam.service.AutoConfigService;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliationDetail;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankreconciliationService;
import com.yonyoucloud.fi.cmp.bankreconciliation.enums.BankreconciliationActionEnum;
import com.yonyoucloud.fi.cmp.bankreconciliation.service.count.BillCountServiceImpl;
import com.yonyoucloud.fi.cmp.common.service.CmCommonService;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import com.yonyoucloud.fi.cmp.controller.intelligentClaim.SchedulingTaskController;
import com.yonyoucloud.fi.cmp.fcdsusesetting.service.IFcdsUseSettingInnerService;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailaccess.BankDealDetailAccessFacade;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.common.CheckRuleCommonUtils;
import com.yonyoucloud.fi.cmp.openapi.service.OpenApiExternalService;
import com.yonyoucloud.fi.cmp.util.bankreconciliation.checkandfilter.BankreconciliationUtils;
import com.yonyoucloud.fi.cmp.util.QueryBaseDocUtils;
import com.yonyoucloud.fi.cmp.util.StringUtils;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.text.SimpleDateFormat;
import java.util.*;

@Controller
@RequestMapping("/bankreconciliationlist")
@Slf4j
@RequiredArgsConstructor
public class BankReconciliationListController extends BaseController {

    private final OpenApiExternalService openApiExternalService;

    private final BankreconciliationService bankreconciliationService;

    private final BillCountServiceImpl countService;

    private final IFcdsUseSettingInnerService fcdsUseSettingInnerService;

    @Autowired
    private ITenantRoleUserService tenantRoleUserService;
    @Autowired
    BaseRefRpcService baseRefRpcService;
    @Autowired
    AutoConfigService autoConfigService;
    @Autowired
    CmCommonService cmCommonService;
    @Resource
    private BankDealDetailAccessFacade bankDealDetailAccessFront;

    @Resource
    private BankIdentifyService bankIdentifyService;

    /**
     * 发布
     *
     * @param bill
     * @param response
     */
    @PostMapping("/publish")
    public void publish(@RequestBody BillDataDto bill, HttpServletResponse response) throws Exception {
        if (!QueryBaseDocUtils.getPeriodByService()) {
            CtmJSONObject result = getResult();
            renderJson(response, ResultMessage.data(result));
        } else {
            Map<String, Object> params = (Map<String, Object>) bill.getData();
            String id = ValueUtils.isNotEmptyObj(params.get("id")) ? params.get("id").toString() : null;
            if (StringUtils.isEmpty(id)) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101212"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418018B", "ID不能为空！") /* "ID不能为空！" */);
            }
            String bankSeqNo = ValueUtils.isNotEmptyObj(params.get("bank_seq_no")) ? params.get("bank_seq_no").toString() : null;
//                if (StringUtils.isEmpty(bankSeqNo)) {
//                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101213"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418018A", "银行交易流水号不能为空！") /* "银行交易流水号不能为空！" */);
//                }
            renderJson(response, ResultMessage.data(bankreconciliationService.publish(Long.parseLong(id), bankSeqNo, params)));
        }
    }

    /**
     * 获取用户岗位
     *
     * @param params
     * @param response
     */
    @PostMapping("/getRoleType")
    @CMPDiworkPermission(IServicecodeConstant.CMPBANKRECONCILIATION)
    public void getRoleType(@RequestBody Map<String, Object> params, HttpServletResponse response) throws Exception {
        List<TenantRole> roles = tenantRoleUserService.findRolesByUserId(AppContext.getCurrentUser().getYhtUserId(), AppContext.getYTenantId().toString(), "diwork");
        String roleCodeLine = "";
        if (null != roles) {
            for (TenantRole role : roles) {
                roleCodeLine += role.getRoleCode() + "----";
            }
        }
        renderJson(response, ResultMessage.data(roleCodeLine));
    }

    /**
     * 退回
     *
     * @param params
     * @param response
     */
    @PostMapping("/returnBill")
    public void returnBill(@RequestBody Map<String, Object> params, HttpServletResponse response) throws Exception {
        if (!QueryBaseDocUtils.getPeriodByService()) {
            CtmJSONObject result = getResult();
            CtmJSONArray returnJSONArray = new CtmJSONArray();
            returnJSONArray.add(result);
            renderJson(response, ResultMessage.data(returnJSONArray));
        } else {
            CtmJSONArray returnJSONArray = new CtmJSONArray();
            String returnreason = ValueUtils.isNotEmptyObj(params.get("returnreason")) ? params.get("returnreason").toString() : null;

            List<Map<String, Object>> dataList;
            if (params.get("dataList") instanceof List) {
                dataList = (List<Map<String, Object>>) params.get("dataList");
            } else {
                dataList = new ArrayList<>();
                dataList.add((Map<String, Object>) params.get("dataList"));
            }
            for (Map<String, Object> data : dataList) {
                String id = ValueUtils.isNotEmptyObj(data.get("id")) ? data.get("id").toString() : null;
                String bankSeqNo = ValueUtils.isNotEmptyObj(data.get("bank_seq_no")) ? data.get("bank_seq_no").toString() : null;
                CtmJSONObject returnBill = bankreconciliationService.returnBill(Long.parseLong(id), bankSeqNo, returnreason);
                returnJSONArray.add(returnBill);
            }

            renderJson(response, ResultMessage.data(returnJSONArray));
        }
    }

    private CtmJSONObject getResult() {
        CtmJSONObject resultJSONObject = new CtmJSONObject();
        resultJSONObject.put("dealSucceed", false);
        resultJSONObject.put(ICmpConstant.MSG, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418018D", "未开通现金管理服务!") /* "未开通现金管理服务!" */);
        return resultJSONObject;
    }

    /**
     * 取消发布
     *
     * @param bill
     * @param response
     */
    @PostMapping("/cancelPublish")
    public void cancelPublish(@RequestBody BillDataDto bill, HttpServletResponse response) throws Exception {
        if (!QueryBaseDocUtils.getPeriodByService()) {
            CtmJSONObject result = getResult();
            renderJson(response, ResultMessage.data(result));
        } else {
            Map<String, Object> params = (Map<String, Object>) bill.getData();
            String id = ValueUtils.isNotEmptyObj(params.get("id")) ? params.get("id").toString() : null;
            if (StringUtils.isEmpty(id)) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101212"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418018B", "ID不能为空！") /* "ID不能为空！" */);
            }
            String bankSeqNo = ValueUtils.isNotEmptyObj(params.get("bank_seq_no")) ? params.get("bank_seq_no").toString() : null;
//                if (StringUtils.isEmpty(bankSeqNo)) {
//                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101213"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418018A", "银行交易流水号不能为空！") /* "银行交易流水号不能为空！" */);
//                }
            renderJson(response, ResultMessage.data(bankreconciliationService.cancelPublish(Long.parseLong(id), bankSeqNo)));
        }
    }

    /**
     * 查询认领情况
     *
     * @param bill
     * @param response
     */
    @PostMapping("/findClaim")
    public void findClaimes(@RequestBody BillDataDto bill, HttpServletResponse response) throws Exception {
        Map<String, Object> params = (Map<String, Object>) bill.getData();
        String id = ValueUtils.isNotEmptyObj(params.get("id")) ? params.get("id").toString() : null;
        if (!ValueUtils.isNotEmptyObj(id)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101212"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418018B", "ID不能为空！") /* "ID不能为空！" */);
        }
        renderJson(response, ResultMessage.data(bankreconciliationService.findClaimes(Long.parseLong(id))));
    }

    /**
     * 取消发布
     *
     * @param bill
     * @param response
     */
    @PostMapping("/getPeriodByaccentity")
    @CMPDiworkPermission(IServicecodeConstant.CMPBANKRECONCILIATION)
    public void getPeriodByaccentity(@RequestBody BillDataDto bill, HttpServletResponse response) throws Exception {
        renderJson(response, ResultMessage.data(QueryBaseDocUtils.getPeriodByService()));
    }

    /**
     * 分配业务人员
     *
     * @param bill
     * @param response
     */
    @PostMapping("/dispatchbussiness")
    public void dispatchBussiness(@RequestBody BillDataDto bill, HttpServletResponse response) throws Exception {
        if (!QueryBaseDocUtils.getPeriodByService()) {
            CtmJSONObject result = getResult();
            renderJson(response, ResultMessage.data(result));
        } else {
            List<Map<String, Object>> params = (List<Map<String, Object>>) bill.getData();
            String id = ValueUtils.isNotEmptyObj(params.get(0).get("id")) ? params.get(0).get("id").toString() : null;
            if (StringUtils.isEmpty(id)) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101212"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418018B", "ID不能为空！") /* "ID不能为空！" */);
            }
            String bankSeqNo = ValueUtils.isNotEmptyObj(params.get(0).get("bank_seq_no")) ? params.get(0).get("bank_seq_no").toString() : null;
//                if (StringUtils.isEmpty(bankSeqNo)) {
//                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101213"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418018A", "银行交易流水号不能为空！") /* "银行交易流水号不能为空！" */);
//                }
            String[] userids = bill.getIds().split(",");
            if (userids.length < 1 || "".equals(userids[0])) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101215"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418018C", "分配业务人员不能为空！") /* "分配业务人员不能为空！" */);
            }
            renderJson(response, ResultMessage.data(bankreconciliationService.dispatchBussiness(id, userids, false)));
        }
    }

    /**
     * 分配业务人员
     *
     * @param bill
     * @param response
     */
    @PostMapping("/dispatchbatchbussiness")
    public void dispatchBatchBussiness(@RequestBody BillDataDto bill, HttpServletResponse response) throws Exception {
        if (!QueryBaseDocUtils.getPeriodByService()) {
            CtmJSONObject result = getResult();
            renderJson(response, ResultMessage.data(result));
        } else {
            String[] userids = bill.getIds().split(",");
            if (userids.length < 1 || "".equals(userids[0])) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101215"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418018C", "分配业务人员不能为空！") /* "分配业务人员不能为空！" */);
            }
            List<String> ids = new ArrayList<>();
            List<Map<String, Object>> params = (List<Map<String, Object>>) bill.getData();
            for (Map<String, Object> param : params) {
                String id = ValueUtils.isNotEmptyObj(param.get("id")) ? param.get("id").toString() : null;
                if (StringUtils.isEmpty(id)) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101212"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418018B", "ID不能为空！") /* "ID不能为空！" */);
                }
                String bankSeqNo = ValueUtils.isNotEmptyObj(param.get("bank_seq_no")) ? param.get("bank_seq_no").toString() : null;
//                    if (StringUtils.isEmpty(bankSeqNo)) {
//                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101213"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418018A", "银行交易流水号不能为空！") /* "银行交易流水号不能为空！" */);
//                    }
                ids.add(id);
            }
            renderJson(response, ResultMessage.data(bankreconciliationService.dispatchBatchBussiness(ids, userids, false)));
        }
    }

    /**
     * 取消分配
     *
     * @param bill
     * @param response
     */
    @PostMapping("/cancelDispatch")
    public void cancelDispatch(@RequestBody BillDataDto bill, HttpServletResponse response) throws Exception {
        if (!QueryBaseDocUtils.getPeriodByService()) {
            CtmJSONObject result = getResult();
            renderJson(response, ResultMessage.data(result));
        } else {
            Map<String, Object> params = (Map<String, Object>) bill.getData();
            String id = ValueUtils.isNotEmptyObj(params.get("id")) ? params.get("id").toString() : null;
            if (StringUtils.isEmpty(id)) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101216"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B001E4", "ID不能为空！") /* "ID不能为空！" */);
            }
            String bankSeqNo = ValueUtils.isNotEmptyObj(params.get("bank_seq_no")) ? params.get("bank_seq_no").toString() : null;
//                if (StringUtils.isEmpty(bankSeqNo)) {
//                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101217"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B001E3", "银行交易流水号不能为空！") /* "银行交易流水号不能为空！" */);
//                }
            renderJson(response, ResultMessage.data(bankreconciliationService.cancelDispatch(id, bankSeqNo)));
        }
    }

    /**
     * 分配财务人员
     *
     * @param bill
     * @param response
     */
    @PostMapping("/batchDispatch")
    public void batchDispatch(@RequestBody BillDataDto bill, HttpServletResponse response) throws Exception {
        if (!QueryBaseDocUtils.getPeriodByService()) {
            CtmJSONObject result = getResult();
            renderJson(response, ResultMessage.data(result));
        } else {
            List<Map<String, Object>> mapList = (List<Map<String, Object>>) bill.getData();
            if (mapList == null || mapList.size() < 1) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101218"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418018E", "请至少选择一条数据!") /* "请至少选择一条数据!" */);
            }
            String[] ids = bill.getIds().split(",");
            if (ids == null || ids.length < 1 || ValueUtils.isEmpty(ids[0])) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101219"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418018F", "分配财务对接人不能为空！") /* "分配财务对接人不能为空！" */);
            }
            String[] billIds = new String[mapList.size()];
            for (int i = 0; i < mapList.size(); i++) {
                billIds[i] = (String) mapList.get(i).get("id");
            }
            QuerySchema queryInitDataSchema = QuerySchema.create().addSelect("*");
            QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
            conditionGroup.addCondition(QueryCondition.name("id").in(billIds));
            queryInitDataSchema.addCondition(conditionGroup);
            List<BankReconciliation> bankReconciliationList = MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME, queryInitDataSchema, null);
            renderJson(response, ResultMessage.data(bankreconciliationService.batchDispatch(bankReconciliationList, ids)));
        }
    }

    /**
     * 获取对接人名称
     *
     * @param bill
     * @param response
     */
    @RequestMapping("/getAutheduserName")
    @CMPDiworkPermission(IServicecodeConstant.CMPBANKRECONCILIATION)
    public void getAutheduserName(@RequestBody BillDataDto bill, HttpServletResponse response) throws Exception {
        if (bill.getId() == null || bill.getId() == "") {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101212"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418018B", "ID不能为空！") /* "ID不能为空！" */);
        }
        // 获取分配信息
        QuerySchema queryIsExist = QuerySchema.create().addSelect("*");
        QueryConditionGroup conditionGroup = new QueryConditionGroup();
        conditionGroup.addCondition(QueryCondition.name("mainid").eq(bill.getId()));
        queryIsExist.addCondition(conditionGroup);
        List<BankReconciliationDetail> bankReconciliationDetails = MetaDaoHelper.queryObject(BankReconciliationDetail.ENTITY_NAME, queryIsExist, null);
        List<String> userIds = new ArrayList<>();
        for (BankReconciliationDetail bankReconciliationDetail : bankReconciliationDetails) {
            userIds.add(bankReconciliationDetail.getAutheduser());
        }
        // 用户名
        String userNames = UserCenter.getUsersByUserIdsInTenant(AppContext.getCurrentUser().getYTenantId(), userIds);

        CtmJSONObject jsonObject = CtmJSONObject.parseObject(userNames);
        CtmJSONArray arrays = CtmJSONArray.parseArray(CtmJSONObject.toJSONString(jsonObject.get("users")));
        Map<String, Object> result = new HashMap<>();
        for (int i = 0; i < arrays.size(); i++) {
            CtmJSONObject jsonObject1 = arrays.getJSONObject(i);
            result.put(jsonObject1.getString("userId"), jsonObject1.getString("userName"));
        }
        CtmJSONObject resultJSONObject = new CtmJSONObject();
        resultJSONObject.put("result", result);
        renderJson(response, CtmJSONObject.toJSONString(resultJSONObject));
    }

    /**
     * 回单关联
     *
     * @param param
     * @param request
     * @param response
     */
    @PostMapping("/receiptassociation")
    @CMPDiworkPermission(IServicecodeConstant.CMPBANKRECONCILIATION)
    public void receiptassociation(@RequestBody CtmJSONObject param, HttpServletRequest request, HttpServletResponse response) throws Exception {
        String id = ValueUtils.isNotEmptyObj(param.get("id")) ? param.get("id").toString() : null;
        String bankelectronicreceiptid = ValueUtils.isNotEmptyObj(param.get("bankelectronicreceiptid")) ? param.get("bankelectronicreceiptid").toString() : null;
        if (StringUtils.isEmpty(id) || StringUtils.isEmpty(bankelectronicreceiptid)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101212"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418018B", "ID不能为空！") /* "ID不能为空！" */);
        }
        renderJson(response, ResultMessage.data(bankreconciliationService.receiptassociation(Long.parseLong(id), Long.parseLong(bankelectronicreceiptid))));
    }

    /**
     * 取消回单关联
     *
     * @param param
     * @param request
     * @param response
     */
    @PostMapping("/cancelReceiptassociation")
    @DiworkPermission(IServicecodeConstant.CMPBANKRECONCILIATION)
    public void cancelReceiptassociation(@RequestBody CtmJSONObject param, HttpServletRequest request, HttpServletResponse response) throws Exception {
        String id = ValueUtils.isNotEmptyObj(param.get("id")) ? param.get("id").toString() : null;
        if (StringUtils.isEmpty(id)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101212"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418018B", "ID不能为空！") /* "ID不能为空！" */);
        }
        renderJson(response, ResultMessage.data(bankreconciliationService.cancelReceiptassociation(Long.parseLong(id))));
    }

    /**
     * 批量取消回单关联，暂时只有后端接口，供修数据使用。等有需求后再加前端按钮；需要限制条数吗？会发事件太多影响下游吗？
     *
     * @param param
     * @param request
     * @param response
     */
    @PostMapping("/cancelReceiptassociationBatch")
    @DiworkPermission(IServicecodeConstant.CMPBANKRECONCILIATION)
    public void cancelReceiptassociationBatch(@RequestBody CtmJSONObject param, HttpServletRequest request, HttpServletResponse response) throws Exception {
        CtmJSONObject result = new CtmJSONObject();
        try {
            List<String> ids = (ArrayList) param.get("ids");
            for (String id : ids) {
                bankreconciliationService.cancelReceiptassociation(Long.parseLong(id));
            }
        } catch (Exception e) {
            result.put("associationSucceed", false);
            result.put(ICmpConstant.MSG, e.getMessage());
        }
        result.put("associationSucceed", true);
        result.put(ICmpConstant.MSG, ResultMessage.success());
        renderJson(response, ResultMessage.data(result));
}

    /**
     * 批量更新对方账户等信息
     *
     * @param params
     * @param response
     */
    @PostMapping("/batchUpdateBankReconciliation")
    public void batchUpdateBankReconciliation(@RequestBody Map<String, Object> params, HttpServletResponse response) throws Exception {
        bankreconciliationService.batchUpdateBankReconciliation(params);
    }

    /**
     * 批量更新对方账户等信息
     *
     * @param params
     * @param response
     */
    @PostMapping("/batchEditCheck")
    public void batchEditCheck(@RequestBody Map<String, Object> params, HttpServletResponse response) throws Exception{
        StringBuilder strMessage = new StringBuilder("");
        List<Long> ids = (ArrayList) params.get("ids");
        QuerySchema schema = QuerySchema.create().addSelect("*");
        QueryConditionGroup queryConditionGroup = new QueryConditionGroup(ConditionOperator.and);
        queryConditionGroup.appendCondition(QueryCondition.name("id").in(ids));
        schema.addCondition(queryConditionGroup);
        List<BankReconciliation> bankReconciliationList = MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME, schema, null);
        if (bankReconciliationList != null && bankReconciliationList.size() > 0) {
            for (BankReconciliation bankReconciliation : bankReconciliationList) {
                String errorMessage = BankreconciliationUtils.checkDataLegal(bankReconciliation, BankreconciliationActionEnum.BATCHMODIFY);
                strMessage.append(errorMessage);
                // 银行对账单保存、修改、导入时根据银行账户、交易日期查历史余额，如果已确认则不能增改
                String bankaccount = bankReconciliation.getBankaccount();
                Date tran_date = bankReconciliation.getTran_date();
                EnterpriseBankAcctVO enterpriseBankAcctVO = baseRefRpcService.queryEnterpriseBankAccountById(bankReconciliation.getBankaccount());
                if (!com.yonyoucloud.fi.cmp.util.StringUtils.isEmpty(bankaccount) && tran_date != null) {
                    String dateStr = new SimpleDateFormat("yyyy-MM-dd").format(tran_date);
                    QuerySchema querySchema = QuerySchema.create().addSelect(QuerySchema.PARTITION_ALL);
                    querySchema.appendQueryCondition(QueryCondition.name("enterpriseBankAccount").eq(bankaccount));
                    querySchema.appendQueryCondition(QueryCondition.name("balancedate").between(dateStr, null));
                    querySchema.appendQueryCondition(QueryCondition.name("isconfirm").eq(true));
                    querySchema.addOrderBy("balancedate");
                    List<Map<String, Object>> historyBalance = MetaDaoHelper.query(AccountRealtimeBalance.ENTITY_NAME, querySchema);
                    if (org.apache.commons.collections4.CollectionUtils.isNotEmpty(historyBalance)) {
                        strMessage.append(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B9A43A05D8007B", "银行账户[%s]交易日期[%s]的历史余额已经进行确认，不允许编辑，请检查!") /* "银行账户[%s]交易日期[%s]的历史余额已经进行确认，不允许编辑，请检查!" */, enterpriseBankAcctVO.getAcctName(), historyBalance.get(historyBalance.size() - 1).get("balancedate")));
                    }
                }
            }
        }
        renderJson(response, ResultMessage.data(strMessage.toString()));
    }

    /**
     * 业务处理生单资金调度类生单在前端beforeBatchpush事件中发请求根据提前入账判断入账类型的值
     * //第一次提前入账只能生成收付款单，之后做第二次可以生成其他类型，提前入账肯定为是 然后赋值为冲挂账
     * //如果是正常生单，入账类型为正常入账
     *
     * @param params
     * @param response
     */
    @PostMapping(value = "/dealVirtualEntryType")
    @CMPDiworkPermission(IServicecodeConstant.DLLIST)
    public void dealVirtualEntryType(@RequestBody CtmJSONObject params, HttpServletResponse response) throws Exception{
        bankreconciliationService.dealVirtualEntryType(params);
        renderJson(response, ResultMessage.data("success"));
    }

    /**
     * 查询银行对账单汇总信息
     *
     * @param param
     * @param request
     * @param response
     * @throws Exception
     */
    @PostMapping("/queryBankSummaryInformation")
    public void queryBankSummaryInformation(@RequestBody CtmJSONObject param, HttpServletRequest request, HttpServletResponse response) throws Exception {
        CtmJSONObject jsonObject = bankreconciliationService.queryBankSummaryInformation(param, response);
        renderJson(response, ResultMessage.data(jsonObject));
    }

    /**
     * 银行流水认领、认领中心、我的认领统计区查询
     *
     * @param params
     * @param response
     */
    @PostMapping(value = "/getCount")
    public void getCount(@RequestBody CtmJSONObject params, HttpServletResponse response) throws Exception {
        renderJson(response, ResultMessage.data(countService.getCount(params)));
    }

    /**
     * 取消验重
     *
     * @param bill
     * @param response
     */
    @PostMapping("/cancleRepeat")
    public void cancleRepeat(@RequestBody BillDataDto bill, HttpServletResponse response) throws Exception {
        Map<String, Object> params = (Map<String, Object>) bill.getData();
        String id = ValueUtils.isNotEmptyObj(params.get("id")) ? params.get("id").toString() : null;
        if (StringUtils.isEmpty(id)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101212"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418018B", "ID不能为空！") /* "ID不能为空！" */);
        }
        renderJson(response, ResultMessage.data(bankreconciliationService.cancleRepeat(Long.parseLong(id))));
    }

    /**
     * 取消单据关联
     *
     * @param bill
     * @param response
     */
    @PostMapping("/cancelCorrelate")
    public void cancelCorrelate(@RequestBody BillDataDto bill, HttpServletResponse response) throws Exception {
        Map<String, Object> params = (Map<String, Object>) bill.getData();
        String id = ValueUtils.isNotEmptyObj(params.get("id")) ? params.get("id").toString() : null;
        CtmJSONObject resultStrRespVO = bankreconciliationService.cancelCorrelate(Long.valueOf(id));
        renderJson(response, ResultMessage.data(resultStrRespVO));
    }


    /**
     * 手动执行辨识匹配规则*
     *
     * @param params
     * @param response
     */
    @PostMapping("/identifiMatchRule")
    public void identifiMatchRule(@RequestBody CtmJSONObject params, HttpServletResponse response) throws Exception {
        //校验简强开关
        try {
            SchedulingTaskController.checkSmartFlowSwitch();
            CheckRuleCommonUtils.checkBankReconciliationIdentifyType(bankIdentifyService,fcdsUseSettingInnerService);
        }catch (Exception e){
            log.error("账户交易流水查询辨识失败，失败原因："+e.getMessage(),e);
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B9A43A05D8007A", "账户交易流水查询辨识失败，失败原因：初始化数据异常：") /* "账户交易流水查询辨识失败，失败原因：初始化数据异常：" */+e.getMessage());
        }

        CtmJSONArray rows = params.getJSONArray("rows");
        if (rows == null || rows.size() == 0) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101221"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1C3796A404880005", "数据不能为空！")/* "数据不能为空！" */);
        }
        List<Object> ids = new ArrayList<>();
        if (rows.size() == 1) {
            ids.add(rows.getJSONObject(0).get("id"));
        } else {
            //循环拿到rows中的id，放到一个集合里
            for (int i = 0; i < rows.size(); i++) {
                CtmJSONObject row = rows.getJSONObject(i);
                ids.add(row.get("id"));
            }
        }
        //根据主表id查询到所有的数据
        QuerySchema queryDataSchema = QuerySchema.create().addSelect("*");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.addCondition(QueryCondition.name("id").in(ids));
        queryDataSchema.addCondition(conditionGroup);
        List<BankReconciliation> bankReconciliationList = MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME, queryDataSchema, null);
        List<BizObject> bizObjects = new ArrayList<>(bankReconciliationList);
        BankreconciliationUtils.checkDataLegalList(bizObjects, BankreconciliationActionEnum.IDENTIFIMATCHRULE);
        //进入流水处理流程入口
        //manualBankDealDetailMatchAndProcess.manualBankDealDetail(bankReconciliationList);
        String result = bankDealDetailAccessFront.dealDetailAccessByManual(bankReconciliationList);
        renderJson(response, result);
    }
}
