package com.yonyoucloud.fi.cmp.bankreconciliation.service.stopuse;

import com.yonyou.iuap.yms.id.generator.YmsOidGenerator;
import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVOWithRange;
import com.yonyou.ucf.basedoc.model.puborggroup.OrgRangeVO;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.cmpentity.AssociationStatus;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.bankreconciliation.StopUseStrategy;
import com.yonyoucloud.fi.cmp.bankreconciliationsetting.BankReconciliationSetting;
import com.yonyoucloud.fi.cmp.bankreconciliationsetting.BankReconciliationSetting_b;
import com.yonyoucloud.fi.cmp.cmpentity.EnableStatus;
import com.yonyoucloud.fi.cmp.cmpentity.ReconciliationDataSource;
import com.yonyoucloud.fi.cmp.common.service.CmCommonService;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.initdata.OpeningOutstanding;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.Direction;
import com.yonyoucloud.fi.cmp.util.CmpMetaDaoHelper;
import com.yonyoucloud.fi.cmp.util.EntityTool;
import com.yonyoucloud.fi.cmp.util.StringUtils;
import com.yonyoucloud.fi.cmp.util.basedoc.EnterpriseBankQueryService;
import lombok.extern.slf4j.Slf4j;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 主表停用策略
 *
 * @author xuwei
 * @date 2024/01/24
 */
@Service
@Slf4j
@Transactional(rollbackFor = RuntimeException.class)
public class MainStrategy implements StopUseStrategy {

    @Autowired
    private BaseRefRpcService baseRefRpcService;
    @Autowired
    private YmsOidGenerator ymsOidGenerator;

    @Autowired
    private CmCommonService cmCommonService;
    @Autowired
    private EnterpriseBankQueryService enterpriseBankQueryService;


    @Override
    public String stopUse(BizObject bizObject, Short mode) throws Exception {

        //需要配置查询深度，不然子表数据无法带出
        BankReconciliationSetting bankReconciliationSetting = MetaDaoHelper.findById(BankReconciliationSetting.ENTITY_NAME, bizObject.get("id"),3);
        EnableStatus enableStatus = bankReconciliationSetting.getEnableStatus();
        String msg = "";
        Date currentPubts = bankReconciliationSetting.getPubts();

        if (currentPubts != null) {
            if (currentPubts.compareTo(bizObject.get("pubts")) != 0) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102238"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041801CF", "当前单据不是最新状态，请刷新单据重新操作。"));
            }
        }
        switch (mode) {
            //启用
            case 0:
                if (EnableStatus.Enabled.equals(enableStatus)) {
                    throw new Exception(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041801BF", "已启用或者已停用方案不允许再启用。"));
                }
                //期初未达项操作.保存态到启用态
                if (EnableStatus.Saved.equals(bankReconciliationSetting.getEnableStatus())){
                    operationQc(bankReconciliationSetting.getId());
                }
                CtmJSONObject result =  handleItemsEnable(bizObject,bankReconciliationSetting);
                bankReconciliationSetting.setEnableStatus(EnableStatus.Enabled);
                bankReconciliationSetting.setStopDate(null);
                msg = com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041801C1", "启用成功");
                if (result.getInteger("successCount") == 0){
                    //共处理X条数据，成功0条，失败X条；失败原因：
                    throw new CtmException(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1F2C64CC04B8001A", "共处理%s条数据，成功%s条，失败%s条；\r\n失败原因：%s") /* "共处理%s条数据，成功%s条，失败%s条；\r\n失败原因：%s" */,result.getInteger("totalCount"),result.getInteger("successCount"),result.getInteger("failCount") ,result.getString("failMsg")));
                }
                if (result.getInteger("failCount") > 0){
                    msg = String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1F2C64CC04B8001A", "共处理%s条数据，成功%s条，失败%s条；\r\n失败原因：%s") /* "共处理%s条数据，成功%s条，失败%s条；\r\n失败原因：%s" */,result.getInteger("totalCount"),result.getInteger("successCount"),result.getInteger("failCount") ,result.getString("failMsg"));
                }
                if (result.getInteger("failCount") == 0) {
                    msg = String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1F2C64CC04B8001D", "共处理%s条数据，成功%s条，失败%s条；") /* "共处理%s条数据，成功%s条，失败%s条；" */,result.getInteger("totalCount"),result.getInteger("successCount"),result.getInteger("failCount"));
                }
                break;
            //取消启用
            case 1:
                if (EnableStatus.Saved.equals(enableStatus) || EnableStatus.Disabled.equals(enableStatus)) {
                    throw new Exception(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041801C4", "未启用或者已停用方案不允许取消启用。"));
                }
                //取消启用时,校验该方案是否已对账，如果有对账单据则不允许取消启用
                checkCheckflag(bankReconciliationSetting.getId());
                updateOpeningOutstandingStatus(bankReconciliationSetting, EnableStatus.Saved,null);
                bankReconciliationSetting.setEnableStatus(EnableStatus.Saved);
                handleEnableStatus(bankReconciliationSetting, EnableStatus.Saved.getValue());
                msg = com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041801C6", "取消启用成功");
                break;
            //停用
            case 2:
                if (EnableStatus.Saved.equals(enableStatus) || EnableStatus.Disabled.equals(enableStatus)) {
                    throw new Exception(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041801CA", "未启用或者已停用方案不允许停用。"));
                }
                //停用日期不可早于方案启用日期
                if (bankReconciliationSetting.getEnableDate().compareTo(bizObject.getDate("enableDate")) > 0){
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102239"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A050800C8", "停用日期不可早于方案启用日期") /* "停用日期不可早于方案启用日期" */);
                }
                //停用
                checkMaxCheckDate(bankReconciliationSetting.getId(),bizObject.getDate("enableDate"));
                updateOpeningOutstandingStatus(bankReconciliationSetting, EnableStatus.Disabled,bizObject.getDate("enableDate"));
                bankReconciliationSetting.setEnableStatus(EnableStatus.Disabled);
                bankReconciliationSetting.setStopDate(bizObject.getDate("enableDate"));
                handleEnableStatus(bankReconciliationSetting, EnableStatus.Disabled.getValue());
                msg = com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041801CD", "停用成功");
                break;
            default:
        }

        EntityTool.setUpdateStatus(bankReconciliationSetting);
        MetaDaoHelper.update(BankReconciliationSetting.ENTITY_NAME, bankReconciliationSetting);

        return msg;
    }

    private void handleEnableStatus(BankReconciliationSetting bankReconciliationSetting, short value) throws Exception {
        List<BankReconciliationSetting_b> items = bankReconciliationSetting.get("bankReconciliationSetting_b");
        for (BankReconciliationSetting_b item : items) {
            item.setEnableStatus_b(value);
            if (EnableStatus.Disabled.getValue() == value){
                item.setEnableDate(bankReconciliationSetting.getStopDate());
            }else {
                item.setEnableDate(null);
            }
            EntityTool.setUpdateStatus(item);
        }
        MetaDaoHelper.update(BankReconciliationSetting_b.ENTITY_NAME, items);
    }

    /**
     * 支持对账方案停用重新启用
     * @param bankReconciliationSetting
     * @throws Exception
     */
    private CtmJSONObject handleItemsEnable(BizObject bizObject,BankReconciliationSetting bankReconciliationSetting) throws Exception {
        CtmJSONObject result = new CtmJSONObject();
        int successCount = 0;
        int failCount = 0;
        StringBuilder failMsg = new StringBuilder();
        List<String> notAuthedBankInfo = new ArrayList<>();
        QuerySchema querySchema = QuerySchema.create().addSelect("id,useorg,useorg.name as useorg_name,bankaccount,bankaccount.account as bankaccount_account,currency,mainid," +
                "accbook_b,subject,assistaccountingtype,assistaccounting");
        QueryConditionGroup condition = new QueryConditionGroup();
        condition.appendCondition(QueryCondition.name("mainid").eq(bankReconciliationSetting.getId()));
        querySchema.addCondition(condition);
        List<BankReconciliationSetting_b> items = MetaDaoHelper.queryObject(BankReconciliationSetting_b.ENTITY_NAME, querySchema, null);
        for (BankReconciliationSetting_b item : items) {
            boolean isSuccess = true;
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
                failMsg.append(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1F19709E04300016", "%s（%s）已经存在方案中，不能重复启用") /* "%s（%s）已经存在方案中，不能重复启用" */,
                        bankReconciliationSetting_bs.get(0).get("useorg_name"),bankReconciliationSetting_bs.get(0).get("bankaccount_account"))+";\r\n");
                isSuccess = false;
            }

            //子表【启用】时，校验当前对账组织是否有当前行账户的使用权限，若没有使用权，则提示：“当前对账组织没有账户的使用权，不能启用”
            //查询授权使用组织
            if (isSuccess){
                EnterpriseBankAcctVOWithRange enterpriseBankAcctVoWithRange = enterpriseBankQueryService.queryEnterpriseBankAcctVOWithRangeById(item.getBankaccount());
                if (enterpriseBankAcctVoWithRange == null){
                        failMsg.append(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1F2C64CC04B80019", "银行账号（%s）已停用或者销户,不可启用") /* "银行账号（%s）已停用或者销户,不可启用" */, item.getString("bankaccount_account"))+";\r\n");
                        isSuccess = false;
                }else {
                    List<OrgRangeVO> orgRangeVOList = enterpriseBankAcctVoWithRange.getAccountApplyRange();
                    List<String> useOrgs = orgRangeVOList.stream().map(OrgRangeVO::getRangeOrgId).collect(Collectors.toList());
                    if (!useOrgs.contains(bankReconciliationSetting.getAccentity())){
                        notAuthedBankInfo.add(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1F2C64CC04B8001B", "银行账号%s") /* "银行账号%s" */, item.getString("bankaccount_account")));
                        isSuccess = false;
                    }
                    if (!useOrgs.contains(item.get("useorg").toString())){
                        failMsg.append(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1F3F5A3405100007", "%s没有银行账户%s的使用权") /* "%s没有银行账户%s的使用权" */, item.get("useorg_name").toString(),item.getString("bankaccount_account"))+";\r\n");
                        isSuccess = false;
                    }
                }
            }

            //启用子表，若有冲突则设置为停用
            if (isSuccess){
                item.setEnableDate(null);
                item.setEnableStatus_b(EnableStatus.Enabled.getValue());
                EntityTool.setUpdateStatus(item);
                MetaDaoHelper.update(BankReconciliationSetting_b.ENTITY_NAME, item);
                successCount++;
                //启用期初未达项,只有主表是停用态才走该方法
                if(EnableStatus.Disabled.equals(bankReconciliationSetting.getEnableStatus())){
                    handleEnableItemOpeningOutstanding(item,bankReconciliationSetting);
                }
            }else {
                failCount++;
            }
        }
        if (notAuthedBankInfo.size() > 0){
            failMsg.append(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1F2C64CC04B8001C", "%s没有%s的使用权,不能启用") /* "%s没有%s的使用权,不能启用" */,bizObject.getString("accentity_name"), String.join("、", notAuthedBankInfo)));//@notranslate
        }
        result.put("successCount",successCount);
        result.put("failCount",failCount);
        result.put("totalCount",items.size());
        result.put("failMsg",failMsg.toString());
        return result;
    }
    /**
     * 期初未达项操作
     *
     * @param id 对账方案id
     */
    private List<OpeningOutstanding> operationQc(Long id) throws Exception {
        BankReconciliationSetting bankReconciliationSetting = MetaDaoHelper.findById(BankReconciliationSetting.ENTITY_NAME, id,3);
        if (bankReconciliationSetting == null) {
            return new ArrayList<>();
        }
        List<OpeningOutstanding> openingOutstandings = new ArrayList<>();
        List<BankReconciliationSetting_b> bank_bs = bankReconciliationSetting.get("bankReconciliationSetting_b");

        //2020506 修改启用逻辑，不清空原来数据
        List<OpeningOutstanding> oldOpeningOutstandings = getOldOpeningOutstanding(bankReconciliationSetting);
        if (oldOpeningOutstandings != null && oldOpeningOutstandings.size() != 0) {
            for (BizObject openingOutstanding : oldOpeningOutstandings){
                openingOutstanding.put("enableDate",new Date());
                openingOutstanding.put("enableStatus",EnableStatus.Enabled.getValue());
                openingOutstanding.put("stopDate",null);
                EntityTool.setUpdateStatus(openingOutstanding);
            }
            MetaDaoHelper.update(OpeningOutstanding.ENTITY_NAME, oldOpeningOutstandings);
            return oldOpeningOutstandings;
        }

        //没有老数据才走下边逻辑
        if (bank_bs != null) {
            //银行账号+币种相同只存一条期初未达
            List<String> keyList = new ArrayList<>();
            for (BankReconciliationSetting_b bank_b : bank_bs) {
                String key = bank_b.getBankaccount() + bank_b.getCurrency();
                if(keyList.contains(key)){
                    continue;
                }else {
                    keyList.add(key);
                }
                OpeningOutstanding openingOutstanding = new OpeningOutstanding();
                openingOutstanding.setAccentity(bankReconciliationSetting.getAccentity());
                openingOutstanding.setBankreconciliationscheme(id);
                openingOutstanding.setBankreconciliationscheme_b(bank_b.getId().toString());
                openingOutstanding.setReconciliationdatasource(bankReconciliationSetting.getReconciliationdatasource());
                openingOutstanding.setAccbook(bankReconciliationSetting.getAccbook());
                openingOutstanding.setEnableDate(bankReconciliationSetting.getEnableDate());
                openingOutstanding.setEnableStatus(EnableStatus.Enabled);
                openingOutstanding.setBankaccount(bank_b.getBankaccount());
                openingOutstanding.setCurrency(bank_b.getCurrency());
                //需求设计：企业方余额方向：不赋值；企业方期初余额：不赋值
//                Map<String, Object> voucherInitBalMes = cmCommonService.getVoucherInitBalMes(bankReconciliationSetting.getAccentity(),
//                        bank_b.getBankaccount(),
//                        bank_b.getMainid(),
//                        bank_b.getCurrency(),
//                        openingOutstanding.getReconciliationdatasource().getValue());
//                openingOutstanding.setDirection(Direction.find(Short.valueOf(voucherInitBalMes.get("direction").toString())));
//                openingOutstanding.setCoinitloribalance(new BigDecimal(voucherInitBalMes.get("coinitloribalance").toString()));

                openingOutstanding.setBankdirection(Direction.Debit);
                openingOutstanding.setBankinitoribalance(BigDecimal.ZERO);
                openingOutstanding.setId(ymsOidGenerator.nextId());
                openingOutstandings.add(openingOutstanding);
            }
            MetaDaoHelper.insert(OpeningOutstanding.ENTITY_NAME, openingOutstandings);
        }
        return openingOutstandings;
    }

    /**
     * 修改期初未达项老数据
     *
     * @param bankReconciliationSetting
     * @return
     * @throws Exception
     */
    private List<OpeningOutstanding> getOldOpeningOutstanding(BankReconciliationSetting bankReconciliationSetting) throws Exception {
        QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("bankreconciliationscheme").eq(bankReconciliationSetting.getId()));
        QuerySchema schema = new QuerySchema().addSelect("*");
        schema.addCondition(group);
        List<OpeningOutstanding> oldList = MetaDaoHelper.queryObject(OpeningOutstanding.ENTITY_NAME, schema, null);
        return oldList;
    }


    /**
     * 校验方案下的账户是否已对账
     *
     * @param id
     * @throws Exception
     */
    private void checkCheckflag(Long id) throws Exception {
        BankReconciliationSetting bankReconciliationSetting = MetaDaoHelper.findById(BankReconciliationSetting.ENTITY_NAME, id);

        List<BankReconciliationSetting_b> bank_bs = bankReconciliationSetting.get("bankReconciliationSetting_b");

        for (BizObject bankReconciliationSetting_b : bank_bs) {
            String bankaccount = bankReconciliationSetting_b.get(ICmpConstant.BANKACCOUNT);
            String currency = bankReconciliationSetting_b.get(ICmpConstant.CURRENCY);

            //校验日记账-----------------------
            QuerySchema schema = QuerySchema.create().addSelect("count(1) as  count");
            QueryConditionGroup JouralGroup = QueryConditionGroup.and(QueryCondition.name("bankaccount").eq(bankaccount), QueryCondition.name("currency").eq(currency), QueryCondition.name("checkflag").eq(true));
            schema.addCondition(JouralGroup);
            //是否已勾对只用校验对账单即可
//            if (bankReconciliationSetting.getReconciliationdatasource().getValue() == ReconciliationDataSource.BankJournal.getValue()) {
//                Map<String, Object> map = MetaDaoHelper.queryOne(Journal.ENTITY_NAME, schema);
//                Long count = (Long) map.get("count");
//                if (count > 0) {
//                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102240"),"该对账方案存在已勾对的数据，不允许取消启用。");
//                }
//            }
            //校验日记账-----------------------

            //校验对账单-----------------------
            schema = QuerySchema.create().addSelect("count(1) as  count");
            QueryConditionGroup bankGroup = QueryConditionGroup.and(QueryCondition.name("bankaccount").eq(bankaccount), QueryCondition.name("currency").eq(currency));

            if (bankReconciliationSetting.getReconciliationdatasource().getValue() == ReconciliationDataSource.Voucher.getValue()) {
                bankGroup.addCondition(QueryCondition.name("gl_bankreconciliationsettingid").eq(id));
                bankGroup.addCondition(QueryCondition.name("other_checkflag").eq(true));
            } else {
                bankGroup.addCondition(QueryCondition.name("bankreconciliationsettingid").eq(id));
                bankGroup.addCondition(QueryCondition.name("checkflag").eq(true));
            }
            schema.addCondition(bankGroup);

            Map<String, Object> map = MetaDaoHelper.queryOne(BankReconciliation.ENTITY_NAME, schema);
            Long count = (Long) map.get("count");
            if (count > 0) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102241"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A050800C6", "该对账方案存在已勾对的数据，不允许取消启用。") /* "该对账方案存在已勾对的数据，不允许取消启用。" */);
            }
            //校验对账单-----------------------
        }
    }

    /**
     * 停用时校验方案停用日期是否小于最大勾对日期
     * @throws Exception
     */
    private void checkMaxCheckDate(Long id,Date stopDate) throws Exception{
        BankReconciliationSetting bankReconciliationSetting = MetaDaoHelper.findById(BankReconciliationSetting.ENTITY_NAME, id,3);
        List<BankReconciliationSetting_b> bank_bs = bankReconciliationSetting.get("bankReconciliationSetting_b");
        for (BizObject bankReconciliationSetting_b : bank_bs) {
            String bankaccount = bankReconciliationSetting_b.get(ICmpConstant.BANKACCOUNT);
            String currency = bankReconciliationSetting_b.get(ICmpConstant.CURRENCY);
            String useorg = bankReconciliationSetting_b.get("useorg");
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
                if (maxDate!=null && maxDate.compareTo(stopDate)>0){
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102242"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A050800C7", "该方案存在已勾对数据的勾对日期晚于停用日期，请检查！") /* "该方案存在已勾对数据的勾对日期晚于停用日期，请检查！" */);
                }
            }
        }

    }

    private void updateOpeningOutstandingStatus(BankReconciliationSetting bankReconciliationSetting, EnableStatus enableStatus,Date stopDate) throws Exception {
        List<OpeningOutstanding> list = getOldOpeningOutstanding(bankReconciliationSetting);
        for (BizObject openingOutstanding : list) {
            openingOutstanding.set("enableStatus", enableStatus.getValue());
            if (stopDate == null){
                openingOutstanding.set("stopDate",bankReconciliationSetting.getEnableDate());
            }else {
                openingOutstanding.set("stopDate",stopDate);
            }
            EntityTool.setUpdateStatus(openingOutstanding);
        }
        MetaDaoHelper.update(OpeningOutstanding.ENTITY_NAME, list);
    }

    /**
     * 子表启用，联动处理关联的期初未达项
     * @param item 银企对账设置子表
     */
    private void handleEnableItemOpeningOutstanding(BankReconciliationSetting_b item,BankReconciliationSetting bankReconciliationSetting) throws Exception{
        QuerySchema schema = new QuerySchema().addSelect("*");
        QueryConditionGroup group = QueryConditionGroup.and(
                QueryCondition.name("bankreconciliationscheme").eq(item.getMainid()),
                QueryCondition.name("enableStatus").eq(EnableStatus.Disabled.getValue()),
                QueryCondition.name("bankaccount").eq(item.getBankaccount()),
                QueryCondition.name("currency").eq(item.getCurrency())
        );
        schema.addCondition(group);
        List<OpeningOutstanding> oldList = MetaDaoHelper.queryObject(OpeningOutstanding.ENTITY_NAME, schema, null);
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
