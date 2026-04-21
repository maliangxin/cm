package com.yonyoucloud.fi.cmp.ctmrpc;

import com.yonyou.cloud.yts.YtsContext;
import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.iuap.yms.id.generator.YmsOidGenerator;
import com.yonyou.ucf.mdd.common.enums.OperationTypeEnum;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bpm.model.VerifyState;
import com.yonyou.ucf.mdd.ext.bpm.service.ProcessService;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.dao.sql.SqlHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.ucf.mdd.ext.voucher.enums.Status;
import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.basecom.service.FIBillService;
import com.yonyoucloud.fi.cmp.api.ctmrpc.CtmCmpCheckManageRpcService;
import com.yonyoucloud.fi.cmp.checkmanage.CheckManage;
import com.yonyoucloud.fi.cmp.checkmanage.CheckManageDetail;
import com.yonyoucloud.fi.cmp.checkstock.CheckStock;
import com.yonyoucloud.fi.cmp.cmpentity.CheckManageEnum;
import com.yonyoucloud.fi.cmp.common.service.CmCommonService;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.IDomainConstant;
import com.yonyoucloud.fi.cmp.util.DateUtils;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import com.yonyoucloud.fi.cmp.util.business.CmpCommonUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * <h1>支票处置生成，删除接口</h1>
 *
 * @author yanxiaokai
 * @version 1.0
 * @since 2023-06-28 15:07
 */
@Slf4j
@Service
public class CtmCmpCheckManageRpcServiceImpl implements CtmCmpCheckManageRpcService{
    @Autowired
    YmsOidGenerator ymsOidGenerator;

    @Autowired
    private FIBillService fiBillService;

    @Autowired
    private ProcessService processService;

    @Autowired
    private CmCommonService commonService;

    private BillContext getBillContextByFundCollection() {
        BillContext billContext = new BillContext();
        billContext.setAction(ICmpConstant.SAVE);
        billContext.setbMain(true);
        billContext.setBillnum("cmp_checkdisposalcard");
        billContext.setBilltype("Voucher");
        billContext.setMddBoId("ctm-cmp.cmp_checkdisposallist");
        //billContext.setName(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807D2","资金收款单") /* "资金收款单" */);
        billContext.setName(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400787", "支票处置卡片") /* "支票处置卡片" */);
        billContext.setSupportBpm(true);
        billContext.setTenant(AppContext.getCurrentUser().getTenant());
        billContext.setFullname(CheckManage.ENTITY_NAME);
        billContext.setEntityCode("cmp_checkdisposalcard");
        return billContext;
    }

    /**
     * 支票处置生成统一接口
     *
     * @param checkManage
     * @return RuleExecuteResult
     * @throws Exception Exception
     */
    @Override
    public RuleExecuteResult checkSave(CheckManage checkManage) throws Exception {
        log.error("checkSave start");
        checkParams(checkManage);
        BillContext billContext = getBillContextByFundCollection();
/*        long id = ymsOidGenerator.nextId();
        checkManage.setId(ymsOidGenerator.nextId());
        checkManage.setYTenant(AppContext.getYTenantId());
        checkManage.setTenant(AppContext.getTenantId());*/
        long id = ymsOidGenerator.nextId();
        checkManage.setId(id);
        // 审批流状态
        checkManage.setAuditstatus(VerifyState.COMPLETED.getValue());
        checkManage.setVerifystate(VerifyState.COMPLETED.getValue());
        checkManage.setStatus(Status.confirmed.getValue());
        // checkManage.setIsWfControlled(false);
        // 单据日期
        checkManage.setBilldate(DateUtils.getCurrentDate("yyyy-MM-dd"));
        // 总计处置数量
        checkManage.setHandleNum(checkManage.CheckManageDetail().size());
        if (StringUtils.isEmpty(checkManage.getDescription())) {
            checkManage.setDescription(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400788", "作废") /* "作废" */);
        }
        checkManage.getGenerateType();
        // 目前支票处置交易类型只有一个，而且页面不展示，直接查询后赋值
        String billTypeId = null;
        try {
            BillContext bc = new BillContext();
            bc.setFullname("bd.bill.BillTypeVO");
            bc.setDomain(IDomainConstant.MDD_DOMAIN_UCFBASEDOC);
            QuerySchema schema = QuerySchema.create();
            schema.addSelect(ICmpConstant.PRIMARY_ID);
            QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
            conditionGroup.appendCondition(QueryCondition.name("form_id").eq("CM.cmp_checkdisposalcard"));
            schema.addCondition(conditionGroup);
            List<Map<String, Object>> list = MetaDaoHelper.query(bc, schema);
            if (CollectionUtils.isNotEmpty(list)) {
                Map<String, Object> objectMap = list.get(0);
                if (!ValueUtils.isNotEmptyObj(objectMap)) {
                    log.error("查询资金付款单交易类型失败！请检查数据！");
                }
                billTypeId = MapUtils.getString(objectMap, ICmpConstant.PRIMARY_ID);
            }
            Map<String, Object> tradetypeMap = commonService.queryTransTypeById(billTypeId, "0", "cmp_trade_disposal");
            if (ValueUtils.isNotEmptyObj(tradetypeMap)) {
                checkManage.setTradetype((String) tradetypeMap.get(ICmpConstant.PRIMARY_ID)); // "1738001320062746625"
            }
        } catch (Exception e) {
            log.error("未获取到默认的交易类型！, billTypeId = {}, e = {}", billTypeId, e.getMessage());
        }
        // 单据类型
        checkManage.setBilltype(CheckManageEnum.transfer.getValue());
        checkManage.setEntityStatus(EntityStatus.Insert);
        // 生成单据编号
        CmpCommonUtil.billCodeHandler(checkManage,CheckManage.ENTITY_NAME, "ctm-cmp.cmp_checkdisposallist");
        List<CheckManageDetail> checkManageDetailList = checkManage.CheckManageDetail();
        List<Short> collect = checkManageDetailList.stream().map(vo -> vo.getHandletypeDetail()).distinct().collect(Collectors.toList());
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < collect.size(); i++) {
            if (i < (collect.size() - 1)) {
                stringBuilder.append(collect.get(i)).append(",");
            } else {
                stringBuilder.append(collect.get(i));
            }
        }
        // 处置类型汇总
        checkManage.setHandletype(stringBuilder.toString());
        for (CheckManageDetail checkManageDetail : checkManageDetailList) {
            if (StringUtils.isEmpty(checkManageDetail.getHandlereason())) {
                checkManageDetail.setHandlereason(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400788", "作废") /* "作废" */);
            }
            CheckStock checkOne = MetaDaoHelper.findById(CheckStock.ENTITY_NAME, checkManageDetail.getCheckid());
            checkManageDetail.setCheckBillNo(checkOne.getCheckBillNo());
            checkManageDetail.setCheckBillStatus(checkOne.getCheckBillStatus());
            checkManageDetail.setCheckBillType(checkOne.getCheckBillType());
            checkManageDetail.setCheckBillDir(checkOne.getCheckBillDir());
            checkManageDetail.setCurrency(checkOne.getCurrency());
            checkManageDetail.setAmount(checkOne.getAmount());
            checkManageDetail.setDrawerAcctNo(checkOne.getDrawerAcctNo());
            checkManageDetail.setDrawerAcctName(checkOne.getDrawerAcctName());
            checkManageDetail.setPayBank(checkOne.getPayBank());
            checkManageDetail.setVouchdate(checkOne.getBusiDate());
            checkManageDetail.setMainid(id);
            checkManageDetail.setId(ymsOidGenerator.nextId());
            checkManageDetail.setEntityStatus(EntityStatus.Insert);
        }
        BizObject biz = new BizObject();
        CtmJSONObject jsonObject = CtmJSONObject.parseObject(CtmJSONObject.toJSONString(checkManage));
        biz.putAll(jsonObject);
        try {
            boolean isWfControlled = processService.bpmControl(billContext, biz);
            biz.put("isWfControlled",isWfControlled);
        } catch (Exception e) {
            biz.put("isWfControlled",false);
        }

        BillDataDto dataDto = new BillDataDto();
        dataDto.setBillnum("cmp_checkdisposalcard");
        dataDto.setData(CtmJSONObject.toJSONString(biz));
        RuleExecuteResult ruleExecuteResult = fiBillService.executeUpdate(OperationTypeEnum.SAVE.getValue(), dataDto);
        log.error("ruleExecuteResult=> data={}", CtmJSONObject.toJSONString(ruleExecuteResult));
        BizObject result = (BizObject) ruleExecuteResult.getData();
        Long resultId = result.getLong(ICmpConstant.PRIMARY_ID);
        log.error("resultId is:{}",resultId);
        Map<String, Object> map = new HashMap<>();
        map.put(ICmpConstant.PRIMARY_ID, resultId);
        map.put(ICmpConstant.FULL_NAME, CheckManage.ENTITY_NAME);
        map.put("params", biz);
        YtsContext.setYtsContext("SAVE_CHECK_MANAGE_BY_THIRD_PARTY_TRANSFER", map); // 为回滚操作使用
        log.error("checkSave end");
        Map<String, Object> resultResponse = new HashMap<>();
        resultResponse.put("billResult","success");
        resultResponse.put("message",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540078A", "保存成功") /* "保存成功" */);
        resultResponse.put("id",resultId);
        ruleExecuteResult.setOutParams(resultResponse);
        return ruleExecuteResult;

    }

    /**
     * 支票处置生成统一回滚接口
     *
     * @param checkManage
     * @return Object
     * @throws Exception Exception
     */
    @Override
    public Object checkSaveRollBack(CheckManage checkManage) throws Exception {
        log.error("checkSaveRollBack, data={}", CtmJSONObject.toJSONString(checkManage));
        Map<String, Object> map = (Map<String, Object>) YtsContext.getYtsContext("SAVE_CHECK_MANAGE_BY_THIRD_PARTY_TRANSFER");
        Long id = (Long) map.get(ICmpConstant.PRIMARY_ID);
        String yTenantId = InvocationInfoProxy.getTenantid();
        HashMap<String, Object> param = new HashMap<>();
        param.put("id",id);
        param.put("yTenantId",yTenantId);
        SqlHelper.delete("com.yonyoucloud.fi.cmp.mapper.CheckManageMapper.delCheckManage",param);
        log.error("checkSaveRollBack delCheckManage end, param={}", param);

        HashMap<String, Object> paramDeatil = new HashMap<>();
        paramDeatil.put("mainid",id);
        paramDeatil.put("yTenantId",yTenantId);
        SqlHelper.delete("com.yonyoucloud.fi.cmp.mapper.CheckManageMapper.delCheckManageDetail",paramDeatil);
        log.error("checkSaveRollBack delCheckManageDetail end, param={}", param);

        BizObject biz = (BizObject) map.get("params");
        List<CheckManageDetail> checkManageDetail = biz.getBizObjects("CheckManageDetail", CheckManageDetail.class);
        HashMap<String, Object> paramUpdateCheck = new HashMap<>();
        paramUpdateCheck.put("yTenantId",yTenantId);
        Long checkid = checkManageDetail.get(0).getLong("checkid");
        paramUpdateCheck.put("id",checkid);
        String checkBillStatus = checkManageDetail.get(0).getString("checkBillStatus");
        paramUpdateCheck.put("status",checkBillStatus);
        SqlHelper.update("com.yonyoucloud.fi.cmp.mapper.CheckManageMapper.updateCheckTable",paramUpdateCheck);
        log.error("checkSaveRollBack updateCheckTable end, param={}", param);

        CtmJSONObject result = new CtmJSONObject();
        result.put("code", 200);
        result.put("message", "delete success");
        result.put("data", biz);
        return result;
    }

    /**
     * 支票处置删除统一接口
     *
     * @param id
     * @return RuleExecuteResult
     * @throws Exception Exception
     */
    @Override
    public RuleExecuteResult checkDelete(Long id) throws Exception {
        if (id == null) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101469"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18905AFC04500010","支票处置单id不能为空！") /* "支票处置单id不能为空！" */);
        }
        BizObject bizObject = MetaDaoHelper.findById(CheckManage.ENTITY_NAME, id, 2);
        if (!ValueUtils.isNotEmptyObj(bizObject)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101470"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18905AFC04500013","根据id未查询到单据！") /* "根据id未查询到单据！" */);
        }
        Short verifystate = Short.parseShort(bizObject.get("verifystate").toString());//审批状态
        if (verifystate == VerifyState.SUBMITED.getValue() || verifystate == VerifyState.COMPLETED.getValue()) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101471"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18905AFC04500015","支票处置单审批中、审批通过,无法删除") /* "支票处置单审批中、审批通过,无法删除" */);
        }
        bizObject.setEntityStatus(EntityStatus.Delete);
        BillDataDto dataDto = new BillDataDto();
        CtmJSONObject json = new CtmJSONObject();
        json.put("id", id);
        dataDto.setData(json);
        dataDto.setBillnum("cmp_checkdisposalcard");
        RuleExecuteResult ruleExecuteResult = fiBillService.delete(dataDto);

        // 回滚处理
        Map<String, Object> rollBackMap = new HashMap<>();
        rollBackMap.put(ICmpConstant.FULL_NAME, CheckManage.ENTITY_NAME);
        rollBackMap.put("data", bizObject);
        YtsContext.setYtsContext("DELETE_CHECK_MANAGE_BY_THIRD_PARTY_TRANSFER", rollBackMap);

        /*List<CheckManageDetail> checkManageDetailList = bizObject.get("CheckManageDetail");
        for (CheckManageDetail manageDetail : checkManageDetailList) {
            // 支票处置单删除接口校验通过后，删除支票处置单，同时调用支票工作台-支票处置删除接口，恢复支票状态为处置前支票状态，清空支票处置信息
            CheckStock checkOne = MetaDaoHelper.findById(CheckStock.ENTITY_NAME,manageDetail.getCheckid());
            CheckManageDetail checkManageDetailQuery = MetaDaoHelper.findById(CheckManageDetail.ENTITY_NAME, manageDetail.getId());
            checkOne.setCheckBillStatus(checkManageDetailQuery.getCheckBillStatus()); // 支票处置子表第一次保存时的支票状态
            EntityTool.setUpdateStatus(checkOne);
            MetaDaoHelper.update(CheckStock.ENTITY_NAME,checkOne);
        }*/
        Map<String, Object> resultResponse = new HashMap<>();
        resultResponse.put("billResult","success");
        resultResponse.put("message",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400789", "删除成功") /* "删除成功" */);
        ruleExecuteResult.setOutParams(resultResponse);
        return ruleExecuteResult;
    }

    /**
     * 支票处置删除回滚接口
     *
     * @param id
     * @return Object
     * @throws Exception Exception
     */
    @Override
    public Object checkDeleteRollBack(Long id) throws Exception {
        log.error("checkDeleteRollBack=> data={}", id);
        Map<String, Object> map = (Map<String, Object>) YtsContext.getYtsContext("DELETE_CHECK_MANAGE_BY_THIRD_PARTY_TRANSFER");
        log.error("checkDeleteRollBack=> map={}", map.toString());
        BillDataDto dataDto = new BillDataDto();
        dataDto.setBillnum("cmp_checkdisposallist");
        dataDto.setAction(ICmpConstant.SAVE);
        dataDto.setData(map.get(ICmpConstant.DATA));
        return fiBillService.executeUpdate(OperationTypeEnum.SAVE.getValue(), dataDto).getData();
    }


    /**
     * 参数校验
     *
     * @param checkManage checkManage
     */
    private void checkParams(CheckManage checkManage) {
        if (StringUtils.isEmpty(checkManage.getAccentity())) {//会计主体
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100705"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041801A3","会计主体不能为空！") /* "会计主体不能为空！" */);
        }
        if (checkManage.getGenerateType() == null) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101472"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18905AFC04500011","请检查生成方式值，生成方式为必填项！") /* "请检查生成方式值，生成方式为必填项！" */);
        }
        if (!Pattern.matches("2|3",String.valueOf(checkManage.getGenerateType()))) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101473"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18905AFC04500012","请检查生成方式，生成方式只能是资金结算生成、转账工作台生成！") /* "请检查生成方式，生成方式只能是资金结算生成、转账工作台生成！" */);
        }
        if (StringUtils.isNotEmpty(checkManage.getDescription())) {
            if (checkManage.getDescription().length() > 200) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101474"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18905AFC04500014","请检查处置说明(汇总)，长度不能超过200！") /* "请检查处置说明(汇总)，长度不能超过200！" */);
            }
        }
        if (StringUtils.isEmpty(checkManage.getCreator())) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101475"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18905AFC04500016","请检查创建人，创建人为必填项！") /* "请检查创建人，创建人为必填项！" */);
        }
        if (checkManage.getCreatorId() == null) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101476"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18905AFC04500017","请检查创建人id，创建人id为必填项！") /* "请检查创建人id，创建人id为必填项！" */);
        }
        List<CheckManageDetail> checkManageDetailList = checkManage.CheckManageDetail();
        for (CheckManageDetail manageDetail : checkManageDetailList) {
            if (manageDetail.getHandletypeDetail() == null) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101477"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18905AFC04500018","请检查处置类型明细，处置类型明细为必填项！") /* "请检查处置类型明细，处置类型明细为必填项！" */);
            }
            if (!Pattern.matches("[1-3]",String.valueOf(manageDetail.getHandletypeDetail()))) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101478"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18905AFC04500019","请检查处置类型(明细)，处置类型(明细)只能是作废、挂失、退回！") /* "请检查处置类型(明细)，处置类型(明细)只能是作废、挂失、退回！" */);
            }
            if (manageDetail.getCheckid() == null) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101479"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18905AFC0450001A","请检查支票id，支票id为必填项！") /* "请检查支票id，支票id为必填项！" */);
            }
            if (StringUtils.isNotEmpty(manageDetail.getHandlereason())) {
                if (checkManage.getDescription().length() > 200) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101480"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18905AFC0450001B","请检查处置原因(明细)，长度不能超过200！") /* "请检查处置原因(明细)，长度不能超过200！" */);
                }
            }
        }
    }

}
