package com.yonyoucloud.fi.cmp.intelligentdealdetail.correlationrule.service;

import com.yonyou.iuap.ruleengine.dto.relevant.RuleExtParamDto;
import com.yonyou.iuap.ruleengine.dto.relevant.RuleItemDto;
import com.yonyou.iuap.ruleengine.dto.relevant.TargetRuleInfoDto;
import com.yonyou.iuap.yms.id.generator.YmsOidGenerator;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyoucloud.fi.cmp.bankidentify.BankIdentifyService;
import com.yonyoucloud.fi.cmp.bankreconciliation.*;
import com.yonyoucloud.fi.cmp.bankreconciliation.ruleengine.CmpRuleBusiLog;
import com.yonyoucloud.fi.cmp.bankreconciliation.ruleengine.CmpRuleInfo;
import com.yonyoucloud.fi.cmp.cmpentity.EntryType;
import com.yonyoucloud.fi.cmp.cmpentity.OppositeType;
import com.yonyoucloud.fi.cmp.cmpentity.OprType;
import com.yonyoucloud.fi.cmp.cmpentity.PublishStatus;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.enums.PublishedType;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.correlationrule.impl.CommonRuleStrategy;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.consts.DealDetailBusinessCodeEnum;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailcontext.BankDealDetailContext;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailhandler.functional.IDealDetailCallBack;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.businesslog.CmpCheckAndProcessRuleLogProcessor;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.businesslog.CmpRuleCheckLog;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.businesslog.CmpRuleModuleLog;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.businesslog.RuleLogEnum;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.utils.DealDetailUtils;
import com.yonyoucloud.fi.cmp.util.CmpMetaDaoHelper;
import com.yonyoucloud.fi.cmp.util.DateUtils;
import com.yonyoucloud.fi.cmp.util.SendBizMessageUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.imeta.orm.base.EntityStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 自动发布 相关性规则辨识*
 *
 * @author xuxbo
 * @date 2024/6/27 20:46
 */
@Slf4j
@Component
public class AutoPublishRuleStrategy extends CommonRuleStrategy {

    @Autowired
    YmsOidGenerator ymsOidGenerator;

    @Autowired
    BankIdentifyService bankIdentifyService;

    @Autowired
    private BankPublishSendMsgService bankPublishSendMsgService;

    @Override
    public void executeRule(List<BankReconciliation> list, String ruleType, String ruleCode, String ruleId, BankDealDetailContext context, Boolean continueOrNot) throws Exception {
        try {
            //        Map<Integer, TargetRuleInfoDto> executeRuleMap = super.loadRule(ruleType);
            Map<Integer, TargetRuleInfoDto> executeRuleMap = bankIdentifyService.loadRuleBySettingId(ruleId);
            if (ObjectUtils.isEmpty(executeRuleMap)) {
                log.error("发布对象辨识规则中,配置的相关性规则查询为空！！");
                DealDetailUtils.appendBusiCode(DealDetailBusinessCodeEnum.SYSTEM008_08YO3.getCode(), list.get(0), ruleCode);
                return;
            }
            Map<Long, Map<String, Object>> sourcesMap = super.querySourcesValue(list, executeRuleMap);//提前查询所有对账单的source对应的值
            BankReconciliation bankReconciliation = list.get(0);
            //业务日志初始化及赋值
            CmpRuleCheckLog cmpRuleCheckLog = CmpCheckAndProcessRuleLogProcessor.buildCmpRuleBusiLog(bankReconciliation, context, context.getLogName());
            // 命中标识
            boolean hitFlag = false;
            // 获取并排序 keys
            List<Integer> sortedKeys = new ArrayList<>(executeRuleMap.keySet());
            Collections.sort(sortedKeys);  // 默认升序
            for (Integer orderKey : sortedKeys) {
                TargetRuleInfoDto ruleInfoDto = executeRuleMap.get(orderKey);
                RuleExtParamDto ruleExtParamDto = new RuleExtParamDto();// 执行规则参数
                ruleExtParamDto.setRuleId(ruleInfoDto.getId());
                List<RuleItemDto> sources = ruleInfoDto.getSources();
                super.dealSourcesValue(sourcesMap, bankReconciliation, sources);
                ruleExtParamDto.setSources(sources);
                CmpRuleModuleLog cmpRuleModuleLog = CmpCheckAndProcessRuleLogProcessor.buildCmpRuleInfoAndReturnRuleTargets(cmpRuleCheckLog, ruleInfoDto, sources, RuleLogEnum.RuleLogProcess.PUBLISH_OBJ_NAME.getDesc(), ruleCode);
                HashMap<String, Object> ruleTargets = cmpRuleModuleLog.getModuleName_rule_info().getTargets();
                CmpCheckAndProcessRuleLogProcessor.executeRuleStepLog(cmpRuleModuleLog, context.getOperationName() + ruleInfoDto.getCode(), null);
                Map<String, Object> assign = relevantRuleExecService.assign(ruleExtParamDto);

                if (assign != null) {
                    for (Map.Entry<String, Object> assignEntry : assign.entrySet()) {
                        String[] assignsKey = assignEntry.getKey().split("\\.");
                        String key = assignsKey[assignsKey.length - 1];
                        if (assignEntry.getKey().contains("characterDef")) {//处理特征
                            this.checkCharacterDef(bankReconciliation, key, assignEntry.getValue());
                        }
                        //修改发布对象类型，将object转换成short类型
                        if ("published_type".equals(key) && assignEntry.getValue() != null && assignEntry.getValue() instanceof String) {
                            bankReconciliation.set(key, Short.parseShort((String) assignEntry.getValue()));
                        } else {
                            bankReconciliation.set(key, assignEntry.getValue());
                        }
                        ruleTargets.put(key, assignEntry.getValue());
                    }
                    ruleTargets.put("result", "Hit");
                    hitFlag = true;
                    bankReconciliation.put("pushFromOds","true");
                    CmpCheckAndProcessRuleLogProcessor.executeRuleStepLog(cmpRuleModuleLog, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400694", "已匹配上") /* "已匹配上" */ + ruleInfoDto.getCode(), null);
                    break;
                } else {
                    ruleTargets.put("result", "notHit");
                    CmpCheckAndProcessRuleLogProcessor.executeRuleStepLog(cmpRuleModuleLog, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400695", "未匹配上") /* "未匹配上" */ + ruleInfoDto.getCode(), null);
                }
            }
            //没有匹配上相关性规则 或者 授权使用组织是否为空不进行发布  这两种情况是没有命中的情况 可以认为没有匹配上发布辨识规则
            if (!hitFlag) {
                bankReconciliation.set("break", "0");
                if (!hitFlag) {
                    log.error("发布对象辨识规则中,配置的相关性规则未匹配成功！");
                    DealDetailUtils.appendBusiCode(DealDetailBusinessCodeEnum.SYSTEM008_08YO2.getCode(), bankReconciliation, ruleCode);
                    CmpCheckAndProcessRuleLogProcessor.executeNoRuleLog(bankReconciliation, context.getLogName(), DealDetailBusinessCodeEnum.SYSTEM008_08YO2.getDesc(), context);
                }
                if (ObjectUtils.isEmpty(bankReconciliation.getAccentity())) {
                    log.error("该流水授权使用组织为空不能进行发布！");
                    DealDetailUtils.appendBusiCode(DealDetailBusinessCodeEnum.SYSTEM008_08YO4.getCode(), bankReconciliation, ruleCode);
                    CmpCheckAndProcessRuleLogProcessor.executeNoRuleLog(bankReconciliation, context.getLogName(), DealDetailBusinessCodeEnum.SYSTEM008_08YO4.getDesc(), context);
                }
            } else {
                // 2更新过之后的 bankReconciliation  get到相关的发布信息
                // 3更新主表信息  （是否发布 发布组织(会计主体 不需要记录了)（发布组织主子表一定有值） 等等） 生成子表信息 ；；然后传递给郭扬 进行入库
                setBankReconciliationValue(bankReconciliation, ruleCode);
//            bankReconciliation.set("executeStatusEnum","1");
                bankReconciliation.set("break", "1");
            }
        } catch (Exception e) {
            //todo 智能流水加日志记录
            log.error("发布对象辨识中相关性规则执行异常", e);
        }

    }

    public String setBankReconciliationValue(BankReconciliation bankReconciliation, String ruleCode) throws Exception {
        // 2更新过之后的 bankReconciliation  get到相关的发布信息
        //需要获取发布对象  授权使用组织
        short publishedtype = bankReconciliation.getPublished_type();
        if (ObjectUtils.isEmpty(publishedtype)) {
            publishedtype = PublishedType.ORG.getCode();
        }
        // 如果是发布到组织或者指定组织，则需要判断账户使用组织是否为空，如果为空，则需要结束流程
        if (!(publishedtype == PublishedType.ORG.getCode())) {
            if (bankReconciliation.getAccentity() == null) {
                bankReconciliation.setPublished_type(null);
                bankReconciliation.setPublished_dept(null);
                return "0";
            }
        }
        bankReconciliation.setPublished_type(publishedtype);
        String accentity = bankReconciliation.getAccentity();
        //3更新主表信息  （是否发布 发布组织(会计主体 不需要记录了)（发布组织主子表一定有值） 等等） 生成子表信息 ；；然后传递给郭扬 进行入库
        bankReconciliation.setIspublish(true);
        //发布对象辨识规则编号
        bankReconciliation.setPublishrulescode(ruleCode);
        //发布对象辨识时间
        bankReconciliation.setPublishdate(DateUtils.getNow());
        //流水自动发布
        bankReconciliation.setSerialauto(true);
        //发布人
        bankReconciliation.setPublishman(AppContext.getUserId());
        //发布时间
        bankReconciliation.setPublish_time(DateUtils.getNow());
        // 待认领金额
        bankReconciliation.setAmounttobeclaimed(bankReconciliation.getTran_amt());
        //发布待办消息
        try {
            bankPublishSendMsgService.sendPublishMsgToCreateToDo(bankReconciliation);
        } catch (Exception e) {
            log.error("BankPublishSendMsgService sendPublishMsgToCreateToDo error:{}", e.getMessage());
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-105063"), String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_22EE50E004600009", "发布待办消息失败！,错误信息：%s") /* "发布待办消息失败！,错误信息：%s" */,e.getMessage()));
        }
        //子表字段赋值
        BankReconciliationDetail bankReconciliationDetail = new BankReconciliationDetail();
        //处理类型
        bankReconciliationDetail.setOprtype(OprType.Publish.getValue());
        //处理日期
        bankReconciliationDetail.setOprdate(bankReconciliation.getPublish_time());
        //处理时间
        bankReconciliationDetail.setOprtime(bankReconciliation.getPublish_time());
        //处理人
        bankReconciliationDetail.setOperator(AppContext.getUserId());
        //自动处理
        bankReconciliationDetail.setIs_autoopr(Short.valueOf("1"));
        //已发布组织
        bankReconciliationDetail.setPublished_org(accentity);
        //发布到用户
        List<BankReconciliationPublishedUser> pushUserList = new ArrayList<>();
        //发布到角色
        List<BankReconciliationPublishedRole> pushRoleList = new ArrayList<>();
        //发布到部门
        List<BankReconciliationPublishedDept> pushDeptList = new ArrayList<>();
        //发布到员工
        List<BankReconciliationPublishedStaff> pushStaffList = new ArrayList<>();
        String action = "releaseBody";
        //根据发布对象赋值不同的字段
        // 发布对象枚举值
        if (publishedtype == PublishedType.ROLE.getCode()) {
            //已发布角色
            bankReconciliationDetail.setPublished_role(ObjectUtils.isEmpty(bankReconciliation.get("published_role")) ? null : bankReconciliation.get("published_role").toString());
            action = "PublishRole";
            BankReconciliationPublishedRole role = new BankReconciliationPublishedRole();
            role.setMainid(bankReconciliation.getId());
            role.setRole(ObjectUtils.isEmpty(bankReconciliation.get("published_role")) ? null : bankReconciliation.get("published_role").toString());
            role.setId(ymsOidGenerator.nextId());
            role.setEntityStatus(EntityStatus.Insert);
            pushRoleList.add(role);
        } else if (publishedtype == PublishedType.DEPT.getCode()) {
            //已发布部门
            bankReconciliationDetail.setPublished_dept(ObjectUtils.isEmpty(bankReconciliation.get("published_dept")) ? null : bankReconciliation.get("published_dept").toString());
            action = "PublishDept";
            BankReconciliationPublishedDept dept = new BankReconciliationPublishedDept();
            dept.setMainid(bankReconciliation.getId());
            dept.setDept(ObjectUtils.isEmpty(bankReconciliation.get("published_dept")) ? null : bankReconciliation.get("published_dept").toString());
            dept.setId(ymsOidGenerator.nextId());
            dept.setEntityStatus(EntityStatus.Insert);
            pushDeptList.add(dept);
        } else if (publishedtype == PublishedType.USER.getCode()) {
            //已发布用户
            bankReconciliationDetail.setPublished_user(ObjectUtils.isEmpty(bankReconciliation.get("published_user")) ? null : bankReconciliation.get("published_user").toString());
            action = "PublishUser";
            BankReconciliationPublishedUser user = new BankReconciliationPublishedUser();
            user.setMainid(bankReconciliation.getId());
            user.setUser(ObjectUtils.isEmpty(bankReconciliation.get("published_user")) ? null : bankReconciliation.get("published_user").toString());
            user.setId(ymsOidGenerator.nextId());
            user.setEntityStatus(EntityStatus.Insert);
            pushUserList.add(user);
        } else if (publishedtype == PublishedType.EMPLOYEE.getCode()) {
            //已发布员工
            bankReconciliationDetail.setEmployee_financial(ObjectUtils.isEmpty(bankReconciliation.get("employee_financial")) ? null : bankReconciliation.get("employee_financial").toString());
            action = "PublishStaff";
            BankReconciliationPublishedStaff staff = new BankReconciliationPublishedStaff();
            staff.setMainid(bankReconciliation.getId());
            staff.setStaff(ObjectUtils.isEmpty(bankReconciliation.get("employee_financial")) ? null : bankReconciliation.get("employee_financial").toString());
            staff.setId(ymsOidGenerator.nextId());
            staff.setEntityStatus(EntityStatus.Insert);
            pushStaffList.add(staff);
        }
        //发布的时候如果提前入账为否----》正常  取消发布设置入账类型为空
        //发布的时候如果是提前入账为是-----》冲挂账  取消的时候入账类型变为挂账
        if (bankReconciliation.getIsadvanceaccounts()) {
            bankReconciliation.setEntrytype(EntryType.CrushHang_Entry.getValue());
            bankReconciliation.setVirtualEntryType(EntryType.CrushHang_Entry.getValue());
        } else {
            bankReconciliation.setEntrytype(EntryType.Normal_Entry.getValue());
            bankReconciliation.setVirtualEntryType(EntryType.Normal_Entry.getValue());
        }
        //发布状态
        bankReconciliationDetail.setPublishstatus(PublishStatus.Effective.getValue());
        bankReconciliationDetail.setId(ymsOidGenerator.nextId());
        bankReconciliationDetail.setMainid(bankReconciliation.getId());
        bankReconciliationDetail.setEntityStatus(EntityStatus.Insert);
        List<BankReconciliationDetail> detailList = new ArrayList<BankReconciliationDetail>();
        detailList.add(bankReconciliationDetail);
        //事务提交之后再发送消息 待修改
        String finalAction = action;
        //todo 发布完事之后，通过事件消息通知
//        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
//            @SneakyThrows
//            @Override
//            public void afterCommit() {
//                try {
//                    SendBizMessageUtils.sendBizMessage(bankReconciliation, "cmp_bankreconciliation", finalAction);
//                } catch (Exception e) {
//                    log.error("消息发送失败！",e);
//                }
//            }
//        });
        IDealDetailCallBack callback = () -> {
            try {
                SendBizMessageUtils.sendBizMessageNew(bankReconciliation, "cmp_bankreconciliation", finalAction, "ctm-cmp.cmp_bankreconciliation");
//                SendBizMessageUtils.sendBizMessage(bankReconciliation, "cmp_bankreconciliation", finalAction);
                CmpMetaDaoHelper.insert(BankReconciliationDetail.ENTITY_NAME, detailList);
                //发布到用户子表
                if (pushUserList.size() > 0) {
                    CmpMetaDaoHelper.insert(BankReconciliationPublishedUser.ENTITY_NAME, pushUserList);
                }
                //发布到角色子表
                if (pushRoleList.size() > 0) {
                    CmpMetaDaoHelper.insert(BankReconciliationPublishedRole.ENTITY_NAME, pushRoleList);
                }
                //发布到部门子表
                if (pushDeptList.size() > 0) {
                    CmpMetaDaoHelper.insert(BankReconciliationPublishedDept.ENTITY_NAME, pushDeptList);
                }
                //发布到员工子表
                if (pushStaffList.size() > 0) {
                    CmpMetaDaoHelper.insert(BankReconciliationPublishedStaff.ENTITY_NAME, pushStaffList);
                }
                log.error("发布流水消息消息发送成功！{}", bankReconciliation.getId().toString());
            } catch (Exception e) {
                log.error("消息发送失败！", e);
            }
        };
        DealDetailUtils.addCallBackToBankReconciliation(bankReconciliation, callback);
        return "1";
    }
}
