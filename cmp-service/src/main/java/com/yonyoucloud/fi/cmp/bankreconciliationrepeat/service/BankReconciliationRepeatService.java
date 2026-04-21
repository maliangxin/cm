package com.yonyoucloud.fi.cmp.bankreconciliationrepeat.service;

import com.google.common.collect.Lists;
import com.yonyou.diwork.ott.exexutors.RobotExecutors;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.template.CommonOperator;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.dao.sql.SqlHelper;
import com.yonyou.yonbip.ctm.accelerator.threadpool.CtmThreadPoolExecutor;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.logger.business.CTMCMPBusinessLogService;
import com.yonyoucloud.fi.cmp.bankreceipt.dto.TenantDTO;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.bankreconciliation.utils.CommonBankReconciliationProcessor;
import com.yonyoucloud.fi.cmp.bankreconciliationrepeat.dao.BankReconciliationRepeatDAO;
import com.yonyoucloud.fi.cmp.cmpentity.AssociationStatus;
import com.yonyoucloud.fi.cmp.cmpentity.DispatchFinanceStatus;
import com.yonyoucloud.fi.cmp.cmpentity.ReceiptassociationStatus;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.IMsgConstant;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.consts.BankDealDetailConst;
import com.yonyoucloud.fi.cmp.util.StringUtils;
import com.yonyoucloud.fi.cmp.util.process.ProcessUtil;
import com.yonyoucloud.fi.cmp.weekday.DateUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.map.HashedMap;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class BankReconciliationRepeatService {
    @Autowired
    private CtmThreadPoolExecutor ctmThreadPoolExecutor;
    @Autowired
    private CTMCMPBusinessLogService ctmcmpBusinessLogService;
    @Autowired
    private BankReconciliationRepeatDAO bankReconciliationRepeatDAO;

    public CtmJSONObject changeRepeatStatus(CtmJSONObject params) {
        CtmJSONObject responseMsg = new CtmJSONObject();
        List<Map> list = (List<Map>) params.get("data");
        if (CollectionUtils.isEmpty(list)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101750"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_MDD-BACK_189A3F320450003A", "请至少选择一条数据！") /* "请至少勾选一条数据！" */);
        }
        String uid = params.getString("uid");
        String status = params.getString("status");
        int listSize = list.size();
        //构建进度条信息
        ProcessUtil.initProcess(uid, listSize);
        ctmThreadPoolExecutor.getThreadPoolExecutor().submit(() -> {
            try {
                if ("normal".equals(status)) {
                    this.confirmNormal(list, uid);
                } else if ("repeat".equals(status)) {
                    this.confirmRepeat(list, uid);
                } else {
                    this.cancelConfirm(list, uid);
                }
                //ProcessUtil.completedResetCount(uid);
            } catch (Exception e) {
                log.error("changeRepeatStatus", e);
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101751"),e.getMessage());
            } finally {
                ProcessUtil.completedResetCount(uid);
            }
        });
        responseMsg.put("message", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054003BF", "更新疑重状态开始") /* "更新疑重状态开始" */);
        return responseMsg;
    }

    public void confirmNormal(List<Map> list, String uid) throws Exception {
        // 存储旧数据的pubts
        Map<String, Date> pubtsMap = new HashMap<>();
        String[] ids = new String[list.size()];
        for (int i = 0; i < list.size(); i++) {
            Map map = list.get(i);
            ids[i] = (String) map.get("id");
            pubtsMap.put(ids[i], DateUtil.parseTime2Date((String) map.get("pubts")));
        }

        List<BankReconciliation> bankReconciliations = getByIds(ids);
        if (ids.length == 1 && CollectionUtils.isEmpty(bankReconciliations)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101752"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1C4CCFD804D80004", "此单据已经被修改，请刷新后重新操作！") /* "此单据已经被修改，请刷新后重新操作！" */);
        }
        if (ids.length > 1 && bankReconciliations.size() != ids.length) {
            List<String> noExistsIds = bankReconciliations.stream().filter(item -> !Arrays.asList(ids).contains(item.getId())).map(item -> item.getString("id")).collect(Collectors.toList());
            List<Map> noExistsList = list.stream().filter(item -> noExistsIds.contains(item.get("id").toString())).collect(Collectors.toList());
            for (int i = 0; i < noExistsList.size(); i++) {
                Map noExistsMap = noExistsList.get(i);
                if (!noExistsMap.containsKey("accentity_name") || StringUtils.isEmpty(noExistsMap.get("accentity_name").toString())) {
                    ProcessUtil.addMessage(uid, String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D5CDD7C04D00002", "第%s行、银行账号：%s 已被修改，请刷新后重试！"), (i + 1) + "", noExistsMap.get("bankaccount_account")));
                } else {
                    ProcessUtil.addMessage(uid, String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D5CDD0804980002", "第%s行、账户使用组织：%s、银行账号：%s 已被修改，请刷新后重试！"), (i + 1) + "", noExistsMap.get("accentity_name"), noExistsMap.get("bankaccount_account")));
                }
            }
        }

        List<Long> maindIds = new ArrayList<>();
        for (int i = 0; i < bankReconciliations.size(); i++) {
            BankReconciliation entity = bankReconciliations.get(i);
            // 判断pubts
            if (entity.getPubts() != null && pubtsMap.get(entity.getId()) != null && entity.getPubts().compareTo(pubtsMap.get(entity.getId())) != 0) {
                if (StringUtils.isEmpty(entity.get("accentity_name"))) {
                    ProcessUtil.addMessage(uid, String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D5CDD7C04D00002", "第%s行、银行账号：%s 已被修改，请刷新后重试！"), (i + 1) + "", entity.get("bankaccount_account")));
                } else {
                    ProcessUtil.addMessage(uid, String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D5CDD0804980002", "第%s行、账户使用组织：%s、银行账号：%s 已被修改，请刷新后重试！"), (i + 1) + "", entity.get("accentity_name"), entity.get("bankaccount_account")));
                }
                if (bankReconciliations.size() == 1) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101752"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1C4CCFD804D80004", "此单据已经被修改，请刷新后重新操作！") /* "此单据已经被修改，请刷新后重新操作！" */);
                }
            }
            try {
                //DO 判断逻辑
                if (entity.getIsRepeat() != BankDealDetailConst.REPEAT_DOUBT) {
                    if (bankReconciliations.size() == 1) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101753"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D5CE46A04980005", "当前数据疑重标识不等于疑似重复，确认正常失败！"));
                    }
                    if (StringUtils.isEmpty(entity.get("accentity_name"))) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101754"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D5CE13A04D00003", "第%s行、银行账号：%s 疑重标识不等于疑似重复，确认正常失败！"), (i + 1) + "", entity.get("bankaccount_account")));
                    } else {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101755"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D5CE16A04980008", "第%s行、账户使用组织：%s、银行账号：%s 疑重标识不等于疑似重复，确认正常失败！"), (i + 1) + "", entity.get("accentity_name"), entity.get("bankaccount_account")));
                    }
                }
                entity.setIsRepeat((short) BankDealDetailConst.REPEAT_NORMAL);
                entity.setEntityStatus(EntityStatus.Update);
                //如果数据来源于银企联,需要更新账户交易流水表的数据
//                if (entity.getDataOrigin() != null && DateOrigin.DownFromYQL.getValue() == entity.getDataOrigin().getValue()) {
//                    BankDealDetail bankDealDetail = new BankDealDetail();
//                    bankDealDetail.setId(entity.getId());
//                    bankDealDetail.setRepetition_status(BankDealDetailConst.REPEAT_DOUBT);
//                    bankDealDetail.setEntityStatus(EntityStatus.Update);
//
//                    bankDealDetails.add(bankDealDetail);
//                }
                // 外部扩展校验逻辑
                repeatExtendHandle(entity,"confirmNormal");

                maindIds.add(entity.getId());
                ctmcmpBusinessLogService.saveBusinessLog(entity, entity.getBank_seq_no(), "", IServicecodeConstant.doubtfulhandling, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054003BE", "疑重确认") /* "疑重确认" */, IMsgConstant.CONFIRM_NORMAL);
                if (list.size() > 1 && !"bankReconciliationRepeat".equals(uid)) {
                    ProcessUtil.refreshProcess(uid, true, 1);
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                if (bankReconciliations.size() == 1) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101756"),e.getMessage());
                }
                ProcessUtil.addMessage(uid, e.getMessage());
            }
        }
        if (CollectionUtils.isNotEmpty(bankReconciliations)) {
            CommonBankReconciliationProcessor.batchReconciliationBeforeUpdate(bankReconciliations);
            MetaDaoHelper.update(BankReconciliation.ENTITY_NAME, bankReconciliations);
        }
//        if (CollectionUtils.isNotEmpty(bankDealDetails)) {
//            MetaDaoHelper.update(BankDealDetail.ENTITY_NAME, bankReconciliations);
//        }
    }

    public void confirmRepeat(List<Map> list, String uid) throws Exception {
        // 存储旧数据的pubts
        Map<String, Date> pubtsMap = new HashMap<>();
        String[] ids = new String[list.size()];
        for (int i = 0; i < list.size(); i++) {
            Map map = list.get(i);
            ids[i] = (String) map.get("id");
            pubtsMap.put(ids[i], DateUtil.parseTime2Date((String) map.get("pubts")));
        }
        List<BankReconciliation> bankReconciliations = getByIds(ids);
        if (ids.length == 1 && CollectionUtils.isEmpty(bankReconciliations)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101752"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1C4CCFD804D80004", "此单据已经被修改，请刷新后重新操作！") /* "此单据已经被修改，请刷新后重新操作！" */);
        }
        if (ids.length > 1 && bankReconciliations.size() != ids.length) {
            List<String> noExistsIds = bankReconciliations.stream().filter(item -> !Arrays.asList(ids).contains(item.getId())).map(item -> item.getString("id")).collect(Collectors.toList());
            List<Map> noExistsList = list.stream().filter(item -> noExistsIds.contains(item.get("id").toString())).collect(Collectors.toList());
            for (int i = 0; i < noExistsList.size(); i++) {
                Map noExistsMap = noExistsList.get(i);
                if (!noExistsMap.containsKey("accentity_name") || StringUtils.isEmpty(noExistsMap.get("accentity_name").toString())) {
                    ProcessUtil.addMessage(uid, String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D5CDD7C04D00002", "第%s行、银行账号：%s 已被修改，请刷新后重试！"), (i + 1) + "", noExistsMap.get("bankaccount_account")));
                } else {
                    ProcessUtil.addMessage(uid, String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D5CDD0804980002", "第%s行、账户使用组织：%s、银行账号：%s 已被修改，请刷新后重试！"), (i + 1) + "", noExistsMap.get("accentity_name"), noExistsMap.get("bankaccount_account")));
                }
            }
        }
        List<Long> maindIds = new ArrayList<>();
        for (int i = 0; i < bankReconciliations.size(); i++) {
            BankReconciliation entity = bankReconciliations.get(i);
            // 判断pubts
            if (entity.getPubts() != null && pubtsMap.get(entity.getId()) != null && entity.getPubts().compareTo(pubtsMap.get(entity.getId())) != 0) {
                if (bankReconciliations.size() == 1) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101752"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1C4CCFD804D80004", "此单据已经被修改，请刷新后重新操作！") /* "此单据已经被修改，请刷新后重新操作！" */);
                }
                if (StringUtils.isEmpty(entity.get("accentity_name"))) {
                    ProcessUtil.addMessage(uid, String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D5CDD7C04D00002", "第%s行、银行账号：%s 已被修改，请刷新后重试！"), (i + 1) + "", entity.get("bankaccount_account")));
                } else {
                    ProcessUtil.addMessage(uid, String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D5CDD0804980002", "第%s行、账户使用组织：%s、银行账号：%s 已被修改，请刷新后重试！"), (i + 1) + "", entity.get("accentity_name"), entity.get("bankaccount_account")));
                }
                continue;
            }
            try {
                //DO 判断逻辑
                if (entity.getIsRepeat() != BankDealDetailConst.REPEAT_DOUBT) {
                    if (bankReconciliations.size() == 1) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101757"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D5CE2E404980008", "当前数据疑重标识不等于疑似重复，确认重复失败！"));
                    }
                    if (StringUtils.isEmpty(entity.get("accentity_name"))) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101758"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D5CE5AC04980009", "第%s行、银行账号：%s 疑重标识不等于疑似重复，确认重复失败！"), (i + 1) + "", entity.get("bankaccount_account")));
                    } else {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101759"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D5CE5FE04980008", "第%s行、账户使用组织：%s、银行账号：%s 疑重标识不等于疑似重复，确认重复失败！"), (i + 1) + "", entity.get("accentity_name"), entity.get("bankaccount_account")));
                    }
                }
                entity.setIsRepeat((short) BankDealDetailConst.REPEAT_CONFIRM);
                entity.setEntityStatus(EntityStatus.Update);
                //如果数据来源于银企联,需要更新账户交易流水表的数据
//                if (entity.getDataOrigin() != null && DateOrigin.DownFromYQL.getValue() == entity.getDataOrigin().getValue()) {
//                    BankDealDetail bankDealDetail = new BankDealDetail();
//                    bankDealDetail.setId(entity.getId());
//                    bankDealDetail.setRepetition_status(BankDealDetailConst.REPEAT_CONFIRM);
//                    bankDealDetail.setEntityStatus(EntityStatus.Update);
//
//                    bankDealDetails.add(bankDealDetail);
//                }
                // 外部扩展校验逻辑
                repeatExtendHandle(entity,"confirmRepeat");

                maindIds.add(entity.getId());
                ctmcmpBusinessLogService.saveBusinessLog(entity, entity.getBank_seq_no(), "", IServicecodeConstant.doubtfulhandling, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054003BE", "疑重确认") /* "疑重确认" */, IMsgConstant.CONFIRM_REPEAT);
                if (list.size() > 1 && !"bankReconciliationRepeat".equals(uid)) {
                    ProcessUtil.refreshProcess(uid, true, 1);
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                if (bankReconciliations.size() == 1) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101760"),e.getMessage());
                }
                ProcessUtil.addMessage(uid, e.getMessage());
            }
        }
        if (CollectionUtils.isNotEmpty(bankReconciliations)) {
            CommonBankReconciliationProcessor.batchReconciliationBeforeUpdate(bankReconciliations);
            MetaDaoHelper.update(BankReconciliation.ENTITY_NAME, bankReconciliations);
        }
//        if (CollectionUtils.isNotEmpty(bankDealDetails)) {
//            MetaDaoHelper.update(BankDealDetail.ENTITY_NAME, bankReconciliations);
//        }
    }

    public void cancelConfirm(List<Map> list, String uid) throws Exception {
        // 存储旧数据的pubts
        Map<String, Date> pubtsMap = new HashMap<>();
        String[] ids = new String[list.size()];
        for (int i = 0; i < list.size(); i++) {
            Map map = list.get(i);
            ids[i] = (String) map.get("id");
            pubtsMap.put(ids[i], DateUtil.parseTime2Date((String) map.get("pubts")));
        }
        List<BankReconciliation> bankReconciliations = getByIds(ids);
        if (ids.length == 1 && CollectionUtils.isEmpty(bankReconciliations)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101752"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1C4CCFD804D80004", "此单据已经被修改，请刷新后重新操作！") /* "此单据已经被修改，请刷新后重新操作！" */);
        }
        if (ids.length > 1 && bankReconciliations.size() != ids.length) {
            List<String> noExistsIds = bankReconciliations.stream().filter(item -> !Arrays.asList(ids).contains(item.getId())).map(item -> item.getString("id")).collect(Collectors.toList());
            List<Map> noExistsList = list.stream().filter(item -> noExistsIds.contains(item.get("id").toString())).collect(Collectors.toList());
            for (int i = 0; i < noExistsList.size(); i++) {
                Map noExistsMap = noExistsList.get(i);
                if (!noExistsMap.containsKey("accentity_name") || StringUtils.isEmpty(noExistsMap.get("accentity_name").toString())) {
                    ProcessUtil.addMessage(uid, String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D5CDD7C04D00002", "第%s行、银行账号：%s 已被修改，请刷新后重试！"), (i + 1) + "", noExistsMap.get("bankaccount_account")));
                } else {
                    ProcessUtil.addMessage(uid, String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D5CDD0804980002", "第%s行、账户使用组织：%s、银行账号：%s 已被修改，请刷新后重试！"), (i + 1) + "", noExistsMap.get("accentity_name"), noExistsMap.get("bankaccount_account")));
                }
            }
        }

        List<Long> maindIds = new ArrayList<>();
        for (int i = 0; i < bankReconciliations.size(); i++) {
            BankReconciliation entity = bankReconciliations.get(i);
            // 判断pubts
            if (entity.getPubts() != null && pubtsMap.get(entity.getId()) != null && entity.getPubts().compareTo(pubtsMap.get(entity.getId())) != 0) {
                if (bankReconciliations.size() == 1) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101752"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1C4CCFD804D80004", "此单据已经被修改，请刷新后重新操作！") /* "此单据已经被修改，请刷新后重新操作！" */);
                }
                if (StringUtils.isEmpty(entity.get("accentity_name"))) {
                    ProcessUtil.addMessage(uid, String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D5CDD7C04D00002", "第%s行、银行账号：%s 已被修改，请刷新后重试！"), (i + 1) + "", entity.get("bankaccount_account")));
                } else {
                    ProcessUtil.addMessage(uid, String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D5CDD0804980002", "第%s行、账户使用组织：%s、银行账号：%s 已被修改，请刷新后重试！"), (i + 1) + "", entity.get("accentity_name"), entity.get("bankaccount_account")));
                }
                continue;
            }
            try {
                // 疑重状态 == 正常/疑似重复 || 发布状态=是 || 业务关联状态=是 ||退票状态=是 || 回单关联状态=是 || 分配财务人员状态=是 || 三方平台同步状态=是 || 对账状态=是
                boolean flag = entity.getIsRepeat() == BankDealDetailConst.REPEAT_DOUBT || entity.getIsRepeat() == BankDealDetailConst.REPEAT_INIT ||
                        //发布状态
                        entity.getIspublish() ||
                        //业务关联状态
                        (entity.getAssociationstatus() != null && entity.getAssociationstatus() == AssociationStatus.Associated.getValue()) ||
                        //退票状态
                        (entity.getRefundstatus() != null) ||
                        //回单关联状态
                        (entity.getReceiptassociation() != null && entity.getReceiptassociation() != ReceiptassociationStatus.NoAssociated.getValue()) ||
                        //分配财务人员状态
                        (entity.getDistributestatus() != null && entity.getDistributestatus() != DispatchFinanceStatus.Not.getValue())
                        // 三方平台同步状态 目前没有用起来,暂时不用处理
                        //||(entity.getTripleSynchronStatus() != null && (entity.getTripleSynchronStatus() != TripleSynchronStatus.AlreadyAuto.getValue() || entity.getTripleSynchronStatus() != TripleSynchronStatus.AlreadyManual.getValue()))
                        // 对账状态 (日记账是否已勾对 和 总账是否已勾对)
                        || entity.getCheckflag() || entity.getOther_checkflag();
                if (flag) {
                    if (bankReconciliations.size() == 1) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101761"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D5CE6A604980004", "当前数据疑重标识=确认重复或者疑重标识=确认正常且该条数据无后续操作时才可取消疑重确认，取消疑重确认失败！"));
                    }
                    if (StringUtils.isEmpty(entity.get("accentity_name"))) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101762"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D5CDA0604D00008", "第%s行、银行账号：%s 疑重标识=确认重复或者疑重标识=确认正常且该条数据无后续操作时才可取消疑重确认，取消疑重确认失败！") /* 第%s行、银行账号：%s 疑重标识=确认重复或者疑重标识=确认正常且该条数据无后续操作时才可取消疑重确认，取消疑重确认失败！ */, (i + 1) + "", entity.get("bankaccount_account")));
                    } else {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101763"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D5CDB0A04980003", "第%s行、账户使用组织：%s、银行账号：%s 疑重标识=确认重复或者疑重标识=确认正常且该条数据无后续操作时才可取消疑重确认，取消疑重确认失败！") /* 第%s行、账户使用组织：%s、银行账号：%s 疑重标识=确认重复或者疑重标识=确认正常且该条数据无后续操作时才可取消疑重确认，取消疑重确认失败！*/, (i + 1) + "", entity.get("accentity_name"), entity.get("bankaccount_account")));
                    }
                }
                entity.setIsRepeat((short) BankDealDetailConst.REPEAT_DOUBT);
                entity.setEntityStatus(EntityStatus.Update);
                //如果数据来源于银企联,需要更新账户交易流水表的数据
//                if (entity.getDataOrigin() != null && DateOrigin.DownFromYQL.getValue() == entity.getDataOrigin().getValue()) {
//                    BankDealDetail bankDealDetail = new BankDealDetail();
//                    bankDealDetail.setId(entity.getId());
//                    bankDealDetail.setRepetition_status(BankDealDetailConst.REPEAT_DOUBT);
//                    bankDealDetail.setEntityStatus(EntityStatus.Update);
//
//                    bankDealDetails.add(bankDealDetail);
//                }
                // 外部扩展校验逻辑
                repeatExtendHandle(entity,"cancelConfirm");

                maindIds.add(entity.getId());
                ctmcmpBusinessLogService.saveBusinessLog(entity, entity.getBank_seq_no(), "", IServicecodeConstant.doubtfulhandling, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054003BE", "疑重确认") /* "疑重确认" */, IMsgConstant.CANCLE_REPEAT);
                if (list.size() > 1 && !"bankReconciliationRepeat".equals(uid)) {
                    ProcessUtil.refreshProcess(uid, true, 1);
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                if (bankReconciliations.size() == 1) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101764"),e.getMessage());
                }
                ProcessUtil.addMessage(uid, e.getMessage());
            }
        }
        if (CollectionUtils.isNotEmpty(bankReconciliations)) {
            CommonBankReconciliationProcessor.batchReconciliationBeforeUpdate(bankReconciliations);
            MetaDaoHelper.update(BankReconciliation.ENTITY_NAME, bankReconciliations);
        }
//        if (CollectionUtils.isNotEmpty(bankDealDetails)) {
//            MetaDaoHelper.update(BankDealDetail.ENTITY_NAME, bankReconciliations);
//        }
    }

    private List<BankReconciliation> getByIds(String[] ids) throws Exception {
        QuerySchema querySchema = QuerySchema.create().addSelect(ICmpConstant.SELECT_TOTAL_PARAM + ",accentity.name,bankaccount.account")
                .appendQueryCondition(QueryCondition.name("id").in(ids))
                .appendQueryCondition(QueryCondition.name("isrepeat").is_not_null());
        List<BankReconciliation> list = MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME, querySchema, null);
        return list == null ? Collections.emptyList() : list;
    }

    public void bankReconciliationRepeatCheckStatus() {
        String QUERY_ALL_TENANT = "com.yonyoucloud.fi.cmp.mapper.TenantMapper.queryAllTenant";
        log.error("开始执行银行流水升级");
        // 查询所有的租户信息
        List<TenantDTO> tenantDTOList = SqlHelper.selectList(QUERY_ALL_TENANT);

        for (TenantDTO dto : tenantDTOList) {
            RobotExecutors.runAs(dto.getYtenantId(), () -> {
                try {
                    Integer bankReconciliationCount = bankReconciliationRepeatDAO.selectBankReconciliationRepeatCount(dto.getYtenantId());
                    if (bankReconciliationCount != null && bankReconciliationCount == 0) {
                        log.info("当前租户{}的银行流水处理数据已经完成升级", dto.getYtenantId());
                    } else {
                        List<BankReconciliation> bankReconciliations = bankReconciliationRepeatDAO.selectBankReconciliationRepeatData(dto.getYtenantId());
                        List<List<BankReconciliation>> partition = Lists.partition(bankReconciliations, 1000);
                        for(List<BankReconciliation> part:partition){
                            bankReconciliationRepeatDAO.updateBankReconciliationRepeat(part,dto.getYtenantId());
                        }
                    }
                } catch (Exception e) {
                    log.error("租户执行更新数据失败{},错误原因为:", dto.getYtenantId(), e);
                }

            }, ctmThreadPoolExecutor.getThreadPoolExecutor());
        }
    }

    private void repeatExtendHandle(BankReconciliation bankReconciliation,String action) throws Exception {
        BillDataDto bill = new BillDataDto("cmp_bankreconciliation_repeat_list");
        bill.setData(bankReconciliation);
        Map<String,Object> custMap = new HashedMap<>();
        custMap.put("action",action);
        bill.setCustMap(custMap);
        RuleExecuteResult result = new CommonOperator("repeatCheck").execute(bill);
        if (result.getMsgCode() != 1) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101765"),result.getMessage());
        }
    }

}