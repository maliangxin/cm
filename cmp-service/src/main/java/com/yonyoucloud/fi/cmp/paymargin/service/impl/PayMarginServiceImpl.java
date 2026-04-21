package com.yonyoucloud.fi.cmp.paymargin.service.impl;

import com.yonyou.diwork.service.IApplicationService;
import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.iuap.org.dto.FundsOrgDTO;
import com.yonyou.iuap.org.service.itf.core.IFundsOrgQueryService;
import com.yonyou.iuap.ucf.common.i18n.InternationalUtils;
import com.yonyou.ucf.mdd.ext.bill.rule.util.BillInfoUtils;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.dao.sql.SqlHelper;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyou.workbench.model.ApplicationVO;
import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.orgs.FundsOrgQueryServiceComponent;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetManagerService;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetPaymarginManagerService;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetVO;
import com.yonyoucloud.fi.cmp.cmpentity.FundSettleStatus;
import com.yonyoucloud.fi.cmp.cmpentity.OccupyBudget;
import com.yonyoucloud.fi.cmp.cmpentity.VoucherStatus;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.enums.BillNumberEnum;
import com.yonyoucloud.fi.cmp.enums.SettleFlagEnum;
import com.yonyoucloud.fi.cmp.event.vo.ResultBudget;
import com.yonyoucloud.fi.cmp.margincommon.service.MarginCommonService;
import com.yonyoucloud.fi.cmp.paymargin.PayMargin;
import com.yonyoucloud.fi.cmp.paymargin.service.PayMarginService;
import com.yonyoucloud.fi.cmp.util.*;
import com.yonyoucloud.fi.cmp.voucher.CmpVoucherService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * @BelongsProject: ctm-cmp
 * @BelongsPackage: com.yonyoucloud.fi.cmp.paymargin.service.impl
 * @Author: wenyuhao
 * @CreateTime: 2023-12-13  14:48
 * @Description: 支付保证金
 * @Version: 1.0
 */
@Service
@Slf4j
@Transactional
public class PayMarginServiceImpl implements PayMarginService {

    private final String PAYMARGINMAPPER = "com.yonyoucloud.fi.cmp.mapper.PayMarginMapper.";
    @Autowired
    private CmpBudgetManagerService cmpBudgetManagerService;
    @Autowired
    private CmpBudgetPaymarginManagerService cmpBudgetPaymarginManagerService;
    @Resource
    private IApplicationService appService;
    @Autowired
    CmpVoucherService cmpVoucherService;

    @Autowired
    private MarginCommonService marginCommonService;

    @Autowired
    private FundsOrgQueryServiceComponent fundsOrgQueryService;

    /**
     * @description: 支付保证金预计收回日期预警
     * @author: wenyuhao
     * @date: 2023/12/13 15:10
     * @param: [tenantId]
     * @return: java.util.Map<java.lang.String,java.lang.Object>
     **/
    @Override
    public Map<String, Object> expectedRetrievalDateWarning(String tenantId,String warnDays,String accentity) {
        CtmJSONArray data = new CtmJSONArray();
        Map<String, Object> result = new HashMap<>();
        int status = TaskUtils.TASK_BACK_SUCCESS;
        String msg = "";
        try {
            // 计算预警日期
            Date currentDate = BillInfoUtils.getBusinessDate();
            int days = 0;
            if (warnDays != null && !warnDays.isEmpty()) {
                try {
                    days = Integer.parseInt(warnDays);
                } catch (NumberFormatException e) {
                    log.warn("提醒天数格式不正确，使用默认值0: {}", warnDays);
                }
            }

            // 计算提醒截止日期（当前日期 + 提醒天数）
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(currentDate);
            calendar.add(Calendar.DAY_OF_YEAR, days);
            Date warnDate = calendar.getTime();

            String[] accentityArr = null;
            if (accentity != null && !accentity.isEmpty()) {
                accentityArr = accentity.split(";");
            }

            // 传递计算好的预警日期给SQL查询
            List<Map<String,Object>> payMarginList = queryPayMarginByCurrentDate(currentDate, warnDate, accentityArr);
            if(CollectionUtils.isNotEmpty(payMarginList)){
                CtmJSONObject object = new CtmJSONObject();
                StringBuffer sbuffer = new StringBuffer();

                for(Map<String,Object> payMargin: payMarginList){
                    String code = (String) payMargin.get("CODE");
                    Date expectedRetrievalDate = (Date) payMargin.get("expectedretrievaldate");
                    String accentityId = (String) payMargin.get("accentity");

                    // 获取公司名称
                    String companyName = com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540047A", "保证金虚拟户") /* "保证金虚拟户" */;
                    if (accentityId != null && !accentityId.isEmpty()) {
                        try {
                            // 根据资金组织ID获取公司名称
                            FundsOrgDTO fundsOrgDTO = fundsOrgQueryService.getById(accentityId);
                            if (fundsOrgDTO != null && StringUtils.isNotEmpty(fundsOrgDTO.getName())) {
                                companyName = fundsOrgDTO.getName();
                            }
                        } catch (Exception e) {
                            log.warn("获取资金组织名称失败，使用默认名称, accentityId: {}", accentityId, e);
                        }
                    }

                    // 格式化预计取回日期
                    String dateStr = "";
                    if (expectedRetrievalDate != null) {
                        // expectedRetrievalDate取出来为java.sql.Date，不支持直接toInstant
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                        LocalDate expectedLocalDate = LocalDate.parse(DateUtils.formatDate(expectedRetrievalDate),formatter);
                        dateStr = expectedLocalDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                    }

                    sbuffer.append(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540047C", "%s【%s】预计取回日期为：%s，请关注！") /* "%s【%s】预计取回日期为：%s，请关注！" */, companyName, code, dateStr)).append("<br>");
                }
                object.put("msg", sbuffer.toString());
                data.add(object);
            }
        } catch (Exception e){
            // 任务执行结果 0-失败
            status = TaskUtils.TASK_BACK_FAILURE;
            log.error(">>>>>查询支付保证金预计收回日期预警数据失败, e = {}", e.getMessage(), e);
            msg = e.getMessage();
        } finally {
            //执行结果： 0：失败；1：成功
            result.put("status", status);
            //业务方自定义结果集字段
            result.put("data", data);
            //异常信息
            result.put("msg", msg);
        }
        return result;
    }

    /**
     * @description: 根据当前日期和余额>0的条件查询支付保证金工作台数据
     * @author: wenyuhao
     * @date: 2023/12/13 19:20
     * @param: [nowDateStr]
     * @return: java.util.List<com.yonyoucloud.fi.cmp.paymargin.PayMargin>
     **/
    private List<Map<String, Object>> queryPayMarginByCurrentDate(Date currentDate, Date warnDate, String[] accentityArr) throws Exception{
        Map<String,Object> params=new HashMap<>();
        params.put("currentDate", currentDate);
        params.put("warnDate", warnDate);
        params.put("marginbalance",0);
        params.put("ytenantid", AppContext.getYTenantId());
        params.put("tenantid",AppContext.getTenantId());
        if (accentityArr != null && accentityArr.length > 0) {
            params.put("accentityArr", accentityArr);
        }
        List<Map<String, Object>>  payMarginList= SqlHelper.selectList(PAYMARGINMAPPER+"queryPayMarginByCurrentDate",params);
        return payMarginList;
    }

    @Override
    @Transactional(rollbackFor = RuntimeException.class)
    public String budgetCheckNew(CmpBudgetVO cmpBudgetVO) throws Exception {
        if (!cmpBudgetManagerService.isCanStart(IBillNumConstant.CMP_PAYMARGIN)) {
            CtmJSONObject resultBack = new CtmJSONObject();
            resultBack.put(ICmpConstant.CODE, true);
            return ResultMessage.data(resultBack);
        }
        String billnum = cmpBudgetVO.getBillno();

        String entityname = BillNumberEnum.find(billnum);
        if (com.yonyoucloud.fi.cmp.util.StringUtils.isEmpty(entityname)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100612"),InternationalUtils.getMessageWithDefault("UID:P_TLM-BE_19AF9FC204D000CF", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540047D", "请求参数缺失") /* "请求参数缺失" */));
        }

        //TODO 变更单据返回来源单据信息，ids 为便跟单据自己信息
        List<String> ids = cmpBudgetVO.getIds();
        List<BizObject> bizObjects = new ArrayList<>();
        if (ValueUtils.isNotEmptyObj(ids)) {
            bizObjects = queryBizObjsWarpParentInfo(ids);
        } else if (ValueUtils.isNotEmptyObj(cmpBudgetVO.getBizObj())) {
            BizObject bizObject = CtmJSONObject.parseObject(cmpBudgetVO.getBizObj(), BizObject.class);
            bizObjects.add(bizObject);
        } else {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100612"),InternationalUtils.getMessageWithDefault("UID:P_TLM-BE_19AF9FC204D000CF", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540047D", "请求参数缺失") /* "请求参数缺失" */));
        }
        //变更单据
        String changeBillno = cmpBudgetVO.getChangeBillno();
        if (!com.yonyoucloud.fi.cmp.util.StringUtils.isEmpty(changeBillno)) {
            if (CollectionUtils.isEmpty(bizObjects)) {
                CtmJSONObject resultBack = new CtmJSONObject();
                resultBack.put(ICmpConstant.CODE, true);
                resultBack.put("message", InternationalUtils.getMessageWithDefault("UID:P_TLM-BE_19AF9FC204D000D0", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540047B", "变更金额小于原金额,不需要校验!") /* "变更金额小于原金额,不需要校验!" */));
                return ResultMessage.data(resultBack);
            }
            //变更单据获取（融资登记单据类型）
            billnum = changeBillno;
        } else {
            //非变更单据 自己单据
            if (CollectionUtils.isEmpty(bizObjects)) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100612"),InternationalUtils.getMessageWithDefault("UID:P_TLM-BE_19AF9FC204D000CF", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540047D", "请求参数缺失") /* "请求参数缺失" */));
            }
        }
        return cmpBudgetPaymarginManagerService.budgetCheckNew(bizObjects, billnum, BudgetUtils.SUBMIT);
    }

    public List<BizObject> queryBizObjsWarpParentInfo(List<String> ids) throws Exception {
        // 根据id批量查询数据
        QuerySchema schema = QuerySchema.create().addSelect("*");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        // 只查询来源为现金的数据 只有这类数据需要升级
        conditionGroup.appendCondition(QueryCondition.name("id").in(ids));
        schema.addCondition(conditionGroup);
        return MetaDaoHelper.queryObject(PayMargin.ENTITY_NAME, schema, null);
    }

    /**
     * 当是否结算为否时，结算状态如果选择待结算，审批通过后自动将结算状态由待结算变为结算完成，结算成功日期取审批通过日期，
     * 取消审批时候清空结算成功日期与审批通过日期，结算状态变回待结算
     * 是否结算=否、结算状态为空的数据，审批通过后也应要更新为结算成功、以及结算成功时间置为审批通过时间
     * 需求:新增保证金单据不走结算，支持凭证生成
     *
     * @param payMargin
     * @throws Exception
     */
    @Override
    public void changeSettleFlagAfterAudit(PayMargin payMargin) throws Exception {
        if (payMargin.getSettleflag() == null) {
            return;
        }
        if (payMargin.getSettleflag() == SettleFlagEnum.NO.getValue() && (payMargin.getSettlestatus() == null || payMargin.getSettlestatus() == FundSettleStatus.WaitSettle.getValue())) {
            PayMargin update = new PayMargin();
            update.setId(payMargin.getId());
            update.setSettlestatus(FundSettleStatus.SettleSuccess.getValue());
            Date auditDate = payMargin.getAuditDate();
            if (auditDate == null) {
                auditDate = BillInfoUtils.getBusinessDate() == null ? new Date() : BillInfoUtils.getBusinessDate();
            }
            update.setSettlesuccesstime(auditDate);
            update.setEntityStatus(EntityStatus.Update);
            payMargin.setSettlestatus(FundSettleStatus.SettleSuccess.getValue());
            payMargin.setSettlesuccesstime(auditDate);
            MetaDaoHelper.update(PayMargin.ENTITY_NAME, update);
            PayMargin payMarginNew = MetaDaoHelper.findById(PayMargin.ENTITY_NAME, payMargin.getId());
            budgetAfterSettleStatusChange(payMarginNew,true);
        } else if (payMargin.getSettleflag() == SettleFlagEnum.NO.getValue() && payMargin.getSettlestatus() == FundSettleStatus.SettlementSupplement.getValue()) {
            PayMargin update = new PayMargin();
            update.setId(payMargin.getId());
            Date auditDate = payMargin.getAuditDate();
            if (auditDate == null) {
                auditDate = BillInfoUtils.getBusinessDate() == null ? new Date() : BillInfoUtils.getBusinessDate();
            }
            update.setSettlesuccesstime(auditDate);
            payMargin.setSettlesuccesstime(auditDate);
            update.setEntityStatus(EntityStatus.Update);
            MetaDaoHelper.update(PayMargin.ENTITY_NAME, update);
        }

        // 只有当是否结算为否时，审批通过才生成凭证，若是否结算为是，则走原先的逻辑
        if (payMargin.getSettleflag() == SettleFlagEnum.NO.getValue()) {
            marginCommonService.generateVoucher(payMargin);
        }
    }

    /**
     * r当是否结算为否时，结算状态如果选择待结算，审批通过后自动将结算状态由待结算变为结算完成，结算成功日期取审批通过日期，
     * 取消审批时候清空结算成功日期与审批通过日期，结算状态变回待结算
     *
     * @param payMargin
     * @throws Exception
     */
    @Override
    public void changeSettleFlagAfterUnAudit(PayMargin payMargin) throws Exception {
        if (payMargin.getSettlestatus() == null) {
            return;
        }
        if (payMargin.getSettlestatus() == FundSettleStatus.SettleSuccess.getValue()) {
            PayMargin update = new PayMargin();
            update.setId(payMargin.getId());
            update.setSettlestatus(FundSettleStatus.WaitSettle.getValue());
            update.setSettlesuccesstime(null);
            update.setAuditTime(null);
            update.setEntityStatus(EntityStatus.Update);
            MetaDaoHelper.update(PayMargin.ENTITY_NAME, update);
        } else if (payMargin.getSettlestatus() == FundSettleStatus.SettlementSupplement.getValue()) {
            PayMargin update = new PayMargin();
            update.setId(payMargin.getId());
            update.setSettlesuccesstime(null);
            update.setAuditTime(null);
            update.setEntityStatus(EntityStatus.Update);
            MetaDaoHelper.update(PayMargin.ENTITY_NAME, update);
        }
    }

    @Override
    public void budgetAfterSettleStatusChange(PayMargin payMargin, boolean checkResult) throws Exception {
        Short isOccupyBudget = payMargin.getIsOccupyBudget();
        Short settlestatus = payMargin.getSettlestatus();
        if (settlestatus != null  && cmpBudgetManagerService.isCanStart(IBillNumConstant.CMP_PAYMARGIN)) {
            PayMargin update = new PayMargin();
            update.setId(payMargin.getId());
            update.setEntityStatus(EntityStatus.Update);
            if (settlestatus == FundSettleStatus.SettleSuccess.getValue() || settlestatus == FundSettleStatus.SettlementSupplement.getValue()) {
                if (cmpBudgetPaymarginManagerService.budgetSuccess(payMargin, true, checkResult)) {
                    update.setIsOccupyBudget(OccupyBudget.ActualSuccess.getValue());
                }
                //结算状态更新为结算止付
                //是否占预算为预占成功时，删除预占。
            } else if (settlestatus == FundSettleStatus.SettleFailed.getValue() && isOccupyBudget!=null && isOccupyBudget == OccupyBudget.PreSuccess.getValue()) {
                ResultBudget resultBudget = cmpBudgetPaymarginManagerService.releaseBudget(payMargin, payMargin, IBillNumConstant.CMP_PAYMARGIN, BillAction.CANCEL_SUBMIT);
                if (resultBudget.isSuccess()) {
                    update.setIsOccupyBudget(OccupyBudget.UnOccupy.getValue());
                }
            } else if (settlestatus == FundSettleStatus.Refund.getValue() && isOccupyBudget!=null && isOccupyBudget == OccupyBudget.PreSuccess.getValue()) {
                //是否占预算为预占成功时，删除预占；
                ResultBudget resultBudget = cmpBudgetPaymarginManagerService.releaseBudget(payMargin, payMargin, IBillNumConstant.CMP_PAYMARGIN, BillAction.CANCEL_SUBMIT);
                if (resultBudget.isSuccess()) {
                    update.setIsOccupyBudget(OccupyBudget.UnOccupy.getValue());
                }
            } else if (settlestatus == FundSettleStatus.Refund.getValue() && isOccupyBudget!=null && isOccupyBudget == OccupyBudget.ActualSuccess.getValue()) {
                //是否占预算为实占成功时，删除实占。
                ResultBudget resultBudget = cmpBudgetPaymarginManagerService.gcExecuteTrueUnAudit(payMargin, payMargin, IBillNumConstant.CMP_PAYMARGIN, BillAction.CANCEL_SUBMIT);
                if (resultBudget.isSuccess()) {
                    update.setIsOccupyBudget(OccupyBudget.UnOccupy.getValue());
                }
            }
            if (update.getIsOccupyBudget() != null) {
                MetaDaoHelper.update(PayMargin.ENTITY_NAME, update);
            }
        }
    }
}
