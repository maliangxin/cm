package com.yonyoucloud.fi.cmp.intelligentdealdetail.common.service.impl;
import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.iuap.yms.id.generator.YmsOidGenerator;
import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVOWithRange;
import com.yonyou.ucf.basedoc.model.puborggroup.OrgRangeVO;
import com.yonyou.ucf.basedoc.model.rpcparams.EnterpriseParams;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.yonbip.ctm.accelerator.threadpool.CtmThreadPoolExecutor;
import com.yonyoucloud.fi.cmp.auth.OrgDataPermissionService;
import com.yonyoucloud.fi.cmp.bankidentify.BankIdentifyService;
import com.yonyoucloud.fi.cmp.bankidentifysetting.BankreconciliationIdentifySetting;
import com.yonyoucloud.fi.cmp.bankreconciliation.*;
import com.yonyoucloud.fi.cmp.bankreconciliation.service.autogenerateBill.BusinessGenerateFundNewService;
import com.yonyoucloud.fi.cmp.bankrecrule.ruleengine.imp.RuleStrategy;
import com.yonyoucloud.fi.cmp.bankrecrule.ruleengine.service.IdentifyingRuleStrategy;
import com.yonyoucloud.fi.cmp.cmpentity.EntryType;
import com.yonyoucloud.fi.cmp.cmpentity.OprType;
import com.yonyoucloud.fi.cmp.cmpentity.PublishStatus;
import com.yonyoucloud.fi.cmp.enums.ConfirmStatusEnum;
import com.yonyoucloud.fi.cmp.enums.OrgConfirmBillEnum;
import com.yonyoucloud.fi.cmp.enums.PublishedType;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.common.service.IBankReconciliationCommonService;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.correlationrule.impl.CommonRuleStrategy;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.correlationrule.service.PendingAccountRuleStrategy;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.consts.DealDetailBusinessCodeEnum;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailcontext.BankDealDetailContext;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.businesslog.RuleLogEnum;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.utils.DealDetailUtils;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.Direction;
import com.yonyoucloud.fi.cmp.util.CmpMetaDaoHelper;
import com.yonyoucloud.fi.cmp.util.DateUtils;
import com.yonyoucloud.fi.cmp.util.EntityTool;
import com.yonyoucloud.fi.cmp.util.basedoc.EnterpriseBankQueryService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
/**
 * @Author maliangn
 * @Date 2024/6/29
 * @Description 银行对账单相关公共逻辑
 * @Version 1.0
 */
@Service
@Slf4j
public class BankReconciliationCommonServiceImpl implements IBankReconciliationCommonService {

    @Autowired
    YmsOidGenerator ymsOidGenerator;
    @Autowired
    PendingAccountRuleStrategy pendingAccountRuleStrategy;
    @Autowired
    BankIdentifyService bankIdentifyService;
    /**
     * 执行提前入账辨识
     *
     * @param bankReconciliationList
     * @throws Exception
     */
    @Override
    public List<BankReconciliation> executeIdentificationAdvanceEnterAccount(List<BankReconciliation> bankReconciliationList, String ruleType, Map<String,String> ruleCodes, BankDealDetailContext context) throws Exception {


        List<BankReconciliation> list = new ArrayList<BankReconciliation>();
        //ruleType 是相关性规则的前缀
        for (Map.Entry<String, String> entry : ruleCodes.entrySet()) {
            //todo 需要根据rulecode 对应到相应的相关性规则的前缀
            boolean shouldBreak = false; // 添加一个标志位用于判断是否需要跳出循环
            for (BankReconciliation bankReconciliation : bankReconciliationList) {
                if (bankReconciliation.get("pendingAccountBreak") != null && bankReconciliation.get("pendingAccountBreak").equals("1")) { // break
                    shouldBreak = true; // 设置标志位为true，表示需要跳出循环
                    break; // 跳出内层循环
                }
                List<BankReconciliation> newlist = new ArrayList<BankReconciliation>();
                newlist.add(bankReconciliation);
                try {
                    list = getGenerateBillTaskListnew(newlist, CommonRuleStrategy.CMP_EARLY_RECORD_PREFIX, entry.getKey(), entry.getValue(),context);
                } catch (Exception e) {
                    log.error("挂账辨识中相关性规则执行异常",e);
                    DealDetailUtils.appendBusiCode(DealDetailBusinessCodeEnum.SYSTEM009_09S01.getCode(),bankReconciliation,entry.getKey());
                    //异常处理逻辑
                    bankReconciliation.set("executeStatusEnum","4");
                }
            }
            if (shouldBreak) break; // 根据标志位决定是否跳出外层循环
        }
        return bankReconciliationList;
    }

    private List<BankReconciliation> getGenerateBillTaskListnew(List<BankReconciliation> list, String ruleType, String ruleCode, String ruleId,BankDealDetailContext context) {
        try {
            // 分批处理银行对账单的数据 进行规则辨识
            context.setLogName(RuleLogEnum.RuleLogProcess.PENDING_ACCOUNT_IDENTIFY_NAME.getDesc());
            context.setOperationName(RuleLogEnum.RuleLogProcess.PENDING_ACCOUNT_IDENTIFY_START.getDesc());
            context.setResultSuccessLog(RuleLogEnum.RuleLogProcess.PENDING_ACCOUNT_IDENTIFY_ONE.getDesc());
            context.setResultFailLog(RuleLogEnum.RuleLogProcess.PENDING_ACCOUNT_IDENTIFY_TWO.getDesc());
            pendingAccountRuleStrategy.executeRule(list, ruleType, ruleCode,ruleId,context,null);

            BankReconciliation bankReconciliation = list.get(0);
//            short entryType = EntryType.Normal_Entry.getValue();
            Boolean isadvanceaccounts = false;//提前入账
            if (bankReconciliation.get("pendingAccountBreak") != null && bankReconciliation.get("pendingAccountBreak").equals("1")) {//如果是提前入账的话入账类型为挂账其他的为正常入账
                short entryType = EntryType.Hang_Entry.getValue();
                isadvanceaccounts = true;
                bankReconciliation.setEntrytype(entryType);
                bankReconciliation.setVirtualEntryType(entryType);//virtualentrytype
            }
//            bankReconciliation.setEntrytype(entryType);
//            bankReconciliation.setVirtualEntryType(entryType);//virtualentrytype
            bankReconciliation.setIsadvanceaccounts(isadvanceaccounts);
            if (isadvanceaccounts) {
                bankReconciliation.setAssociationcount(new Short("1"));//业务关联次数---走提前入账生成默认改为1次，用于第二次手动生单前规则BankSetEntrytypePullBeforeRule修改入账类型为冲挂账
            }

        } catch (Exception e) {
            log.error("挂账辨识中相关性规则执行异常",e);
            DealDetailUtils.appendBusiCode(DealDetailBusinessCodeEnum.SYSTEM009_09S01.getCode(),list.get(0),ruleCode);
        }
        return list;
    }




    /**
     * 重新查询银行对账单数据
     *
     * @param list
     * @throws Exception
     */
    @Override
    public List<BankReconciliation> getRuleAfterBankData(List<BankReconciliation> list) throws Exception {
        QuerySchema schema = QuerySchema.create().addSelect("*");
        List<Long> idList = list.stream().map(item -> (Long) item.get("id")).collect(Collectors.toList());
        schema.appendQueryCondition(QueryCondition.name("id").in(idList));
        List<BankReconciliation> resultList = MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME, schema, null);
        return resultList;
    }

    @Override
    public Map<String, String> getRuleCodes(String ruleType) throws Exception {
        Map<String,String> ruleCodes = new HashMap<>();
        List<BankreconciliationIdentifySetting> BankreconciliationIdentifySettings = bankIdentifyService.querySettingsByCode(ruleType);
        //拿到启用的辨识规则  循环遍历 BankreconciliationIdentifySettings 找到BankreconciliationIdentifySetting里getEnablestatus等于1的 规则编码和id
        for (BankreconciliationIdentifySetting setting : BankreconciliationIdentifySettings) {
            if (setting.getEnablestatus() == 1) {
                String ruleCode = setting.getCode();
                String id = setting.getId().toString();
                ruleCodes.put(ruleCode,id);
            }
        }
        return ruleCodes;
    }

    @Override
    public Map<Short,Map<Integer,BankreconciliationIdentifySetting>> getRuleByOrder(String ruleType) throws Exception {
        Map<Short,Map<Integer,BankreconciliationIdentifySetting>> resultMap = new HashMap<>();
        Map<Integer,BankreconciliationIdentifySetting> ruleCodesForCredit = new HashMap<>();
        Map<Integer,BankreconciliationIdentifySetting> ruleCodesForDebit = new HashMap<>();
        List<BankreconciliationIdentifySetting> BankreconciliationIdentifySettings = bankIdentifyService.querySettingsByCode(ruleType);
        //拿到启用的辨识规则  循环遍历 BankreconciliationIdentifySettings 找到BankreconciliationIdentifySetting里getEnablestatus等于1的 规则编码和id
        for (BankreconciliationIdentifySetting setting : BankreconciliationIdentifySettings) {
            if (setting.getEnablestatus() == 1 && setting.getDc_flag() == Direction.Credit.getValue()) {
                Integer ruleCode = setting.getExcutelevel();
                ruleCodesForCredit.put(ruleCode,setting);
            }else if (setting.getEnablestatus() == 1 && setting.getDc_flag() == Direction.Debit.getValue()){
                Integer ruleCode = setting.getExcutelevel();
                ruleCodesForDebit.put(ruleCode,setting);
            }
        }
        resultMap.put(Direction.Credit.getValue(),ruleCodesForCredit);
        resultMap.put(Direction.Debit.getValue(),ruleCodesForDebit);
        return resultMap;
    }


    /**
     * 更新发布处理记录子表信息*
     * @param bankReconciliation
     * @throws Exception
     */
    @Override
    public void updateBankreconciliationDetail(BankReconciliation bankReconciliation) throws Exception{
        //根据主表id查询到所有的数据，（发布状态是已生效）
        QuerySchema queryDataSchema = QuerySchema.create().addSelect("*");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.addCondition(QueryCondition.name("mainid").eq(bankReconciliation.getId()));
        conditionGroup.addCondition(QueryCondition.name("publishstatus").eq(PublishStatus.Effective.getValue()));
        queryDataSchema.addCondition(conditionGroup);
        List<BankReconciliationDetail> bankReconciliationDetailList = MetaDaoHelper.queryObject(BankReconciliationDetail.ENTITY_NAME, queryDataSchema, null);
        //循环遍历 修改发布状态
        for (BankReconciliationDetail bankReconciliationDetail : bankReconciliationDetailList) {
            bankReconciliationDetail.setPublishstatus(PublishStatus.Voided.getValue());
            EntityTool.setUpdateStatus(bankReconciliationDetail);
            MetaDaoHelper.update(BankReconciliationDetail.ENTITY_NAME, bankReconciliationDetail);
        }

    }

    /**
     * 插入银行交易流水 发布处理记录子表信息*
     * @param bankReconciliation
     * @param action
     * @param returnReason
     */
    @Override
    public void insertBankreconciliationDetailNew(BankReconciliation bankReconciliation, String action, String returnReason, Long claimid) throws Exception{
        String accentity = bankReconciliation.getAccentity();
        if(ObjectUtils.isNotEmpty(action)){
            //子表字段赋值
            BankReconciliationDetail bankReconciliationDetail = new BankReconciliationDetail();
            //处理类型
            bankReconciliationDetail.setOprtype(action);
            //处理日期
            bankReconciliationDetail.setOprdate(DateUtils.getNow());
            //处理时间 DateUtils.getNowDate()
            bankReconciliationDetail.setOprtime(DateUtils.getNow());
            //发布或者认领的时候，只需要新增子表即可
            if (action.equals(OprType.Claim.getValue())) {
                //认领用户
                bankReconciliationDetail.setClaimor(InvocationInfoProxy.getUserid());
                //认领单id
                bankReconciliationDetail.setClaimid(claimid);
            } else if (action.equals(OprType.Publish.getValue())) {
                Short publishedtype = bankReconciliation.getPublished_type();
                if(ObjectUtils.isEmpty(publishedtype)){
                    //如果发布对象为空  默认为按照组织发布
                    publishedtype = PublishedType.ORG.getCode();
//                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101680"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1C35250C04480009", "发布对象为空，不允许操作！") /* "发布对象为空，不允许操作！" */);
                }
                //处理日期
                bankReconciliationDetail.setOprdate(bankReconciliation.getPublish_time());
                //处理时间
                bankReconciliationDetail.setOprtime(bankReconciliation.getPublish_time());
                //已发布组织
                bankReconciliationDetail.setPublished_org(accentity);
                //根据发布对象赋值不同的字段
                // 发布对象枚举值
                if (publishedtype == PublishedType.ROLE.getCode()) {
                    //已发布角色
                    bankReconciliationDetail.setPublished_role(bankReconciliation.getPublished_role());
                } else if (publishedtype == PublishedType.ROLE.getCode()) {
                    //已发布部门
                    bankReconciliationDetail.setPublished_dept(bankReconciliation.getPublished_dept());
                } else if (publishedtype == PublishedType.USER.getCode()) {
                    //已发布用户
                    bankReconciliationDetail.setPublished_user(bankReconciliation.getPublished_user());
                } else if (publishedtype == PublishedType.EMPLOYEE.getCode()) {
                    //已发布员工
                    bankReconciliationDetail.setEmployee_financial(bankReconciliation.getEmployee_financial());
                }
                //发布状态
                bankReconciliationDetail.setPublishstatus(PublishStatus.Effective.getValue());
            } else if (action.equals(OprType.Return.getValue())) { //退回的时候 不仅需要记录退回记录 还要更新原来的记录为 已失效
                //退回意见
                bankReconciliationDetail.setReturn_reason(returnReason);
                updateBankreconciliationDetail(bankReconciliation);
            }

            //处理人
            bankReconciliationDetail.setOperator(AppContext.getUserId());
            //自动处理
            bankReconciliationDetail.setIs_autoopr(Short.valueOf("0"));

            bankReconciliationDetail.setId(ymsOidGenerator.nextId());
            bankReconciliationDetail.setMainid(bankReconciliation.getId());
            bankReconciliationDetail.setEntityStatus(EntityStatus.Insert);
            CmpMetaDaoHelper.insert(BankReconciliationDetail.ENTITY_NAME, bankReconciliationDetail);
        }

    }

    /**
     * 插入银行交易流水 发布处理记录子表信息,处理手工发布到用户，角色，部门，人员
     * @param bankReconciliation 流水处理信息
     * @param publishedType 发布类型
     * @param params 具体参数
     * @throws Exception
     */
    @Override
    public void handlePublishToOthers(BankReconciliation bankReconciliation,Short publishedType, Map<String, Object> params) throws Exception {
        //前端传递的参数
        List<Map<String,String>> pushToData = (List<Map<String, String>>) params.get("publishToData");
        //发布到用户
        List<BankReconciliationPublishedUser> pushUserList = new ArrayList<>();
        //发布到角色
        List<BankReconciliationPublishedRole> pushRoleList = new ArrayList<>();
        //发布到部门
        List<BankReconciliationPublishedDept> pushDeptList = new ArrayList<>();
        //发布到员工
        List<BankReconciliationPublishedStaff> pushStaffList = new ArrayList<>();
        //发布到指定组织
        List<BankReconciliationPublishedAssignOrg> pushAssignOrgList = new ArrayList<>();

        //发布操作具体明细
        List<BankReconciliationDetail> pushDetailList = new ArrayList<>();
        for(Map<String,String> map : pushToData){
            //发布信息赋值
            //子表字段赋值
            BankReconciliationDetail bankReconciliationDetail = new BankReconciliationDetail();
            //发布组织一定有值
            bankReconciliationDetail.setPublished_org(bankReconciliation.getAccentity());
            //处理类型
            bankReconciliationDetail.setOprtype(OprType.Publish.getValue());
            //处理日期
            bankReconciliationDetail.setOprdate(DateUtils.getNow());
            //处理时间 DateUtils.getNowDate()
            bankReconciliationDetail.setOprtime(DateUtils.getNow());
            //处理日期
            bankReconciliationDetail.setOprdate(bankReconciliation.getPublish_time());
            //处理时间
            bankReconciliationDetail.setOprtime(bankReconciliation.getPublish_time());
            //发布状态
            bankReconciliationDetail.setPublishstatus(PublishStatus.Effective.getValue());
            //处理人
            bankReconciliationDetail.setOperator(AppContext.getUserId());
            //自动处理=0；手动处理流程
            bankReconciliationDetail.setIs_autoopr(Short.valueOf("0"));
            bankReconciliationDetail.setId(ymsOidGenerator.nextId());
            bankReconciliationDetail.setMainid(bankReconciliation.getId());
            bankReconciliationDetail.setEntityStatus(EntityStatus.Insert);

            //发布到用户
            if (publishedType == PublishedType.USER.getCode()){
                bankReconciliationDetail.setPublished_user(map.get("id"));
                BankReconciliationPublishedUser user = new BankReconciliationPublishedUser();
                user.setMainid(bankReconciliation.getId());
                user.setUser(map.get("id"));
                user.setUser_name(map.get("name"));
                user.setId(ymsOidGenerator.nextId());
                user.setEntityStatus(EntityStatus.Insert);
                pushUserList.add(user);
            }

            //发布到角色
            if (publishedType == PublishedType.ROLE.getCode()){
                bankReconciliationDetail.setPublished_role(map.get("id"));
                BankReconciliationPublishedRole role = new BankReconciliationPublishedRole();
                role.setMainid(bankReconciliation.getId());
                role.setRole(map.get("id"));
                role.setRole_name(map.get("name"));
                role.setId(ymsOidGenerator.nextId());
                role.setEntityStatus(EntityStatus.Insert);
                pushRoleList.add(role);
            }

            //发布到部门
            if (publishedType == PublishedType.DEPT.getCode()){
                bankReconciliationDetail.setPublished_dept(map.get("id"));
                BankReconciliationPublishedDept dept = new BankReconciliationPublishedDept();
                dept.setMainid(bankReconciliation.getId());
                dept.setDept(map.get("id"));
                dept.setDept_name(map.get("name"));
                dept.setId(ymsOidGenerator.nextId());
                dept.setEntityStatus(EntityStatus.Insert);
                pushDeptList.add(dept);
            }

            //发布到员工
            if (publishedType == PublishedType.EMPLOYEE.getCode()){
                bankReconciliationDetail.setEmployee_financial(map.get("id"));
                BankReconciliationPublishedStaff staff = new BankReconciliationPublishedStaff();
                staff.setMainid(bankReconciliation.getId());
                staff.setStaff(map.get("id"));
                staff.setStaff_name(map.get("name"));
                staff.setId(ymsOidGenerator.nextId());
                staff.setEntityStatus(EntityStatus.Insert);
                pushStaffList.add(staff);
            }

            //发布到指定组织
            if (publishedType == PublishedType.ASSIGN_ORG.getCode()){
                bankReconciliationDetail.setPublished_assignorg(map.get("id"));
                BankReconciliationPublishedAssignOrg assignOrg = new BankReconciliationPublishedAssignOrg();
                assignOrg.setMainid(bankReconciliation.getId());
                assignOrg.setAssignorg(map.get("id"));
                assignOrg.setAssignorg_name(map.get("name"));
                assignOrg.setId(ymsOidGenerator.nextId());
                assignOrg.setEntityStatus(EntityStatus.Insert);
                pushAssignOrgList.add(assignOrg);
            }

            pushDetailList.add(bankReconciliationDetail);

        }
        //发布明细信息
        if (pushDetailList.size() > 0){
            CmpMetaDaoHelper.insert(BankReconciliationDetail.ENTITY_NAME,pushDetailList);
        }
        //发布到用户子表
        if (pushUserList.size() > 0){
            CmpMetaDaoHelper.insert(BankReconciliationPublishedUser.ENTITY_NAME,pushUserList);
        }
        //发布到角色子表
        if (pushRoleList.size() > 0){
            CmpMetaDaoHelper.insert(BankReconciliationPublishedRole.ENTITY_NAME,pushRoleList);
        }
        //发布到部门子表
        if (pushDeptList.size() > 0){
            CmpMetaDaoHelper.insert(BankReconciliationPublishedDept.ENTITY_NAME,pushDeptList);
        }
        //发布到员工子表
        if (pushStaffList.size() > 0){
            CmpMetaDaoHelper.insert(BankReconciliationPublishedStaff.ENTITY_NAME,pushStaffList);
        }
        //发布到指定组织子表
        if (pushAssignOrgList.size() > 0){
            CmpMetaDaoHelper.insert(BankReconciliationPublishedAssignOrg.ENTITY_NAME,pushAssignOrgList);
        }
    }
}