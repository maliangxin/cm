package com.yonyoucloud.fi.cmp.bankreconciliation.service.stopuse;

import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVOWithRange;
import com.yonyou.ucf.basedoc.model.puborggroup.OrgRangeVO;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.bankreconciliation.StopUseStrategy;
import com.yonyoucloud.fi.cmp.bankreconciliationsetting.BankReconciliationSetting;
import com.yonyoucloud.fi.cmp.bankreconciliationsetting.BankReconciliationSetting_b;
import com.yonyoucloud.fi.cmp.cmpentity.EnableStatus;
import com.yonyoucloud.fi.cmp.cmpentity.ReconciliationDataSource;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.initdata.OpeningOutstanding;
import com.yonyoucloud.fi.cmp.util.EntityTool;
import com.yonyoucloud.fi.cmp.util.StringUtils;
import com.yonyoucloud.fi.cmp.util.basedoc.EnterpriseBankQueryService;
import lombok.extern.slf4j.Slf4j;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 子表停用策略
 *
 * @author xuwei
 * @date 2024/01/24
 */
@Service
@Slf4j
@Transactional(rollbackFor = RuntimeException.class)
public class ItemStrategy implements StopUseStrategy {

    @Autowired
    private EnterpriseBankQueryService enterpriseBankQueryService;

    /**
     * 子表状态修改
     * @param bizObject 主子表数据
     * @param mode      0-启用，1-取消启用，2-主表停用，3-子表停用 4-子表启用
     * @return
     * @throws Exception
     */
    @Override
    public String stopUse(BizObject bizObject, Short mode) throws Exception {
        String msg = "";
        switch (mode) {
            case 3:
                //子表停用
                msg = stopItem(bizObject);
                break;
            case 4:
                //子表启用
                msg = enableItem(bizObject);
                break;
            default:
                return msg;
        }
        return msg;
    }

    /**
     * 停用子表
     * @param bizObject
     * @return
     * @throws Exception
     */
    private String stopItem(BizObject bizObject) throws Exception{
        String msg;
        BankReconciliationSetting_b item = MetaDaoHelper.findById(BankReconciliationSetting_b.ENTITY_NAME, bizObject.get("id"));
        BankReconciliationSetting bankReconciliationSetting = MetaDaoHelper.findById(BankReconciliationSetting.ENTITY_NAME, item.getMainid(),3);

        String bankaccount = item.get(ICmpConstant.BANKACCOUNT);
        String currency = item.get(ICmpConstant.CURRENCY);
        String useorg = item.get("useorg");
        QuerySchema queryReconciliation = QuerySchema.create().addSelect("max(other_checkdate) as othercheckdate,max(checkdate) as checkdate ");
        QueryConditionGroup group = QueryConditionGroup.and(
                QueryCondition.name("bankaccount").eq(bankaccount),
                QueryCondition.name("currency").eq(currency),
                QueryCondition.name("accentity").eq(useorg)
        );
        //对账方案明细停用校验时，加上对账方案id
        if (bankReconciliationSetting.getReconciliationdatasource().getValue() == ReconciliationDataSource.Voucher.getValue()) {
            group.addCondition(QueryCondition.name("gl_bankreconciliationsettingid").eq(bankReconciliationSetting.getId().toString()));
            group.addCondition(QueryCondition.name("other_checkflag").eq(true));
        } else {
            group.addCondition(QueryCondition.name("bankreconciliationsettingid").eq(bankReconciliationSetting.getId().toString()));
            group.addCondition(QueryCondition.name("checkflag").eq(true));
        }
        queryReconciliation.addCondition(group);
        Map<String, Object> reconciliationMax = MetaDaoHelper.queryOne(BankReconciliation.ENTITY_NAME,queryReconciliation);
        Date maxDate;
        if (reconciliationMax != null){
            if (bankReconciliationSetting.getReconciliationdatasource().getValue() == ReconciliationDataSource.Voucher.getValue()) {
                maxDate = reconciliationMax.get("othercheckdate") != null ? (Date) reconciliationMax.get("othercheckdate") : null;
            } else {
                maxDate = reconciliationMax.get("checkdate") != null ? (Date) reconciliationMax.get("checkdate") : null;
            }
            //该方案存在已勾对数据的勾对日期晚于停用日期，请检查！
            if (maxDate!=null && maxDate.compareTo(bizObject.getDate("enableDate"))>0){
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101231"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A0508010B", "该方案存在已勾对数据的勾对日期晚于停用日期，请检查！") /* "该方案存在已勾对数据的勾对日期晚于停用日期，请检查！" */);
            }
        }
        //停用日期不可早于方案启用日期
        if (bankReconciliationSetting.getEnableDate().compareTo(bizObject.getDate("enableDate")) > 0){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101232"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A0508010A", "停用日期不可早于方案启用日期") /* "停用日期不可早于方案启用日期" */);
        }
        item.setEnableStatus_b(EnableStatus.Disabled.getValue());
        item.setEnableDate(bizObject.getDate("enableDate"));
        EntityTool.setUpdateStatus(item);
        MetaDaoHelper.update(BankReconciliationSetting_b.ENTITY_NAME, item);

        msg = com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041801CD", "停用成功");

        //若同一个对账方案里不存在其他同账号和币种的已启用对账方案明细，则停用对应的期初未达项
        //启用状态
        List<Object> statusList = new ArrayList<>();
        //启用的
        statusList.add(EnableStatus.Enabled.getValue());
        QuerySchema schema = QuerySchema.create().addSelect("id");
        QueryConditionGroup groupItem = QueryConditionGroup.and(
                QueryCondition.name("bankaccount").eq(item.getBankaccount()),
                QueryCondition.name("currency").eq(item.getCurrency()),
                QueryCondition.name("mainid").eq(item.getMainid()),
                QueryCondition.name("enableStatus_b").in(statusList),
                QueryCondition.name("id").not_eq(item.getId())
        );
        schema.addCondition(groupItem);
        List<Map<String, Object>> bankReconciliationSetting_bs = MetaDaoHelper.query(BankReconciliationSetting_b.ENTITY_NAME, schema);
        if (bankReconciliationSetting_bs.size() == 0){
            handleStopItemOpeningOutstanding(item);
        }
        return msg;
    }

    /**
     * 启用子表
     * @param bizObject
     * @return
     * @throws Exception
     */
    private String enableItem(BizObject bizObject) throws Exception{
        String msg ="";
        QuerySchema querySchema = QuerySchema.create().addSelect("id,useorg,useorg.name as useorg_name,bankaccount,bankaccount.account as bankaccount_account,currency,mainid");
        QueryConditionGroup condition = new QueryConditionGroup();
        condition.appendCondition(QueryCondition.name("id").eq(bizObject.get("id")));
        querySchema.addCondition(condition);
        //用来作为前端提示语信息返回
        List<BankReconciliationSetting_b> items = MetaDaoHelper.queryObject(BankReconciliationSetting_b.ENTITY_NAME, querySchema, null);

        BankReconciliationSetting_b item = MetaDaoHelper.findById(BankReconciliationSetting_b.ENTITY_NAME, bizObject.get("id"));
        BankReconciliationSetting bankReconciliationSetting = MetaDaoHelper.findById(BankReconciliationSetting.ENTITY_NAME, item.getMainid(),3);
        if (!item.getEnableStatus_b().equals(EnableStatus.Disabled.getValue())){
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1F19709E04300015", "子表不是停用状态") /* "子表不是停用状态" */);
        }
        //子表【启用】时， 校验相同对账数据源、相同账户、相同币种、相同授权使用组织是否存在启用状态的子表行，若存在则提示：“：XXX(代表组织名称)（银行账号6001）已经存在方案中，不能重复启用“
        //启用状态
        List<Object> statusList = new ArrayList<>();
        //启用的
        statusList.add(EnableStatus.Enabled.getValue());
        QuerySchema schema = QuerySchema.create().addSelect("id,useorg,useorg.name as useorg_name,bankaccount,bankaccount.account as bankaccount_account");
        QueryConditionGroup group = QueryConditionGroup.and(
                QueryCondition.name("bankaccount").eq(item.getBankaccount()),
                QueryCondition.name("currency").eq(item.getCurrency()),
                QueryCondition.name("useorg").eq(item.get("useorg")),
                QueryCondition.name("mainid.reconciliationdatasource").eq(bankReconciliationSetting.getReconciliationdatasource().getValue()),
                QueryCondition.name("mainid.accentity").eq(bankReconciliationSetting.getAccentity()),
                QueryCondition.name("enableStatus_b").in(statusList),
                QueryCondition.name("id").not_eq(item.getId())
        );
        //凭证数据源需要加上账簿，科目还有辅助核算项的判断
        if(bankReconciliationSetting.getReconciliationdatasource().getValue() == ReconciliationDataSource.Voucher.getValue()){
            group.appendCondition(QueryCondition.name("accbook_b").eq(item.getAccbook_b()));
            group.appendCondition(QueryCondition.name("subject").eq(item.getSubject()));
            if (StringUtils.isNotEmpty(item.getAssistaccounting())){
                group.appendCondition(QueryCondition.name("assistaccounting").eq(item.getAssistaccounting()));
            }else {
                group.appendCondition(QueryCondition.name("assistaccounting").is_null());
            }
            if(StringUtils.isNotEmpty(item.getAssistaccountingtype())){
                group.appendCondition(QueryCondition.name("assistaccountingtype").eq(item.getAssistaccountingtype()));
            }else {
                group.appendCondition(QueryCondition.name("assistaccountingtype").is_null());
            }
        }
        schema.addCondition(group);
        List<Map<String, Object>> bankReconciliationSetting_bs = MetaDaoHelper.query(BankReconciliationSetting_b.ENTITY_NAME, schema);
        if (bankReconciliationSetting_bs.size() > 0){
            throw new CtmException(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1F19709E04300016", "%s（%s）已经存在方案中，不能重复启用") /* "%s（%s）已经存在方案中，不能重复启用" */,
                    bankReconciliationSetting_bs.get(0).get("useorg_name"),bankReconciliationSetting_bs.get(0).get("bankaccount_account")));
        }

        //子表【启用】时，校验当前对账组织是否有当前行账户的使用权限，若没有使用权，则提示：“当前对账组织没有账户的使用权，不能启用”
        //查询授权使用组织
        EnterpriseBankAcctVOWithRange enterpriseBankAcctVoWithRange = enterpriseBankQueryService.queryEnterpriseBankAcctVOWithRangeById(item.getBankaccount());
        if (enterpriseBankAcctVoWithRange == null){
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1F1C970204900006", "银行账户已停用或者销户！") /* "银行账户已停用或者销户！" */);
        }
        List<OrgRangeVO> orgRangeVOList = enterpriseBankAcctVoWithRange.getAccountApplyRange();
        List<String> useOrgs = orgRangeVOList.stream().map(OrgRangeVO::getRangeOrgId).collect(Collectors.toList());
        if (!useOrgs.contains(bankReconciliationSetting.getAccentity())){
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1F19709E04300018", "当前对账组织没有账户的使用权，不能启用") /* "当前对账组织没有账户的使用权，不能启用" */);
        }
        if (!useOrgs.contains(item.getUseorg())){
            throw new CtmException(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1F3F59D60510000B", "当前%s没有账户的使用权，不能启用") /* "当前%s没有账户的使用权，不能启用" */,items.get(0).getString("useorg_name")));
        }

        //启用子表
        item.setEnableDate(null);
        item.setEnableStatus_b(EnableStatus.Enabled.getValue());
        EntityTool.setUpdateStatus(item);
        MetaDaoHelper.update(BankReconciliationSetting_b.ENTITY_NAME, item);

        //启用关联的期初未达项
        handleEnableItemOpeningOutstanding(item);

        msg = com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1F19709E04300017", "启用成功") /* "启用成功" */;
        return msg;
    }

    /**
     * 子表停用，联动处理关联的期初未达项
     * @param item 银企对账设置子表
     */
    private void handleStopItemOpeningOutstanding(BankReconciliationSetting_b item) throws Exception{
        QuerySchema schema = new QuerySchema().addSelect("*");
        QueryConditionGroup group = QueryConditionGroup.and(
                QueryCondition.name("bankreconciliationscheme").eq(item.getMainid()),
                QueryCondition.name("bankaccount").eq(item.getBankaccount()),
                QueryCondition.name("currency").eq(item.getCurrency())
        );
        schema.addCondition(group);
        List<OpeningOutstanding> oldList = MetaDaoHelper.queryObject(OpeningOutstanding.ENTITY_NAME, schema, null);
        if (oldList != null && oldList.size() != 0) {
            for (BizObject openingOutstanding : oldList){
                openingOutstanding.put("enableStatus",EnableStatus.Disabled.getValue());
                openingOutstanding.put("stopDate",item.getEnableDate());
                EntityTool.setUpdateStatus(openingOutstanding);
            }
            MetaDaoHelper.update(OpeningOutstanding.ENTITY_NAME, oldList);
        }
    }

    /**
     * 子表启用，联动处理关联的期初未达项
     * @param item 银企对账设置子表
     */
    private void handleEnableItemOpeningOutstanding(BankReconciliationSetting_b item) throws Exception{
        QuerySchema schema = new QuerySchema().addSelect("*");
        QueryConditionGroup group = QueryConditionGroup.and(
                QueryCondition.name("bankreconciliationscheme").eq(item.getMainid()),
                QueryCondition.name("enableStatus").eq(EnableStatus.Disabled.getValue()),
                QueryCondition.name("bankaccount").eq(item.getBankaccount()),
                QueryCondition.name("currency").eq(item.getCurrency())
        );
        schema.addCondition(group);
        List<OpeningOutstanding> oldList = MetaDaoHelper.queryObject(OpeningOutstanding.ENTITY_NAME, schema, null);
        BankReconciliationSetting bankReconciliationSetting = MetaDaoHelper.findById(BankReconciliationSetting.ENTITY_NAME, item.getMainid());
        if (oldList != null && oldList.size() != 0) {
            for (BizObject openingOutstanding : oldList){
                openingOutstanding.put("enableDate",bankReconciliationSetting.getEnableDate());
                openingOutstanding.put("enableStatus",EnableStatus.Enabled.getValue());
                openingOutstanding.put("stopDate",null);
                EntityTool.setUpdateStatus(openingOutstanding);
            }
            MetaDaoHelper.update(OpeningOutstanding.ENTITY_NAME, oldList);
        }
    }
}
