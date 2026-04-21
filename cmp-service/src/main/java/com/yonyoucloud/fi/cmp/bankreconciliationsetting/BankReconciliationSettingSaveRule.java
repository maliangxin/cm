package com.yonyoucloud.fi.cmp.bankreconciliationsetting;

import com.yonyou.iuap.yms.api.IYmsJdbcApi;
import com.yonyou.iuap.yms.id.generator.YmsOidGenerator;
import com.yonyou.ucf.basedoc.model.CurrencyTenantDTO;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.bankreconciliation.utils.CommonBankReconciliationProcessor;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.ucf.mdd.ext.filter.util.StringUtil;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.basecom.utils.FIDubboUtils;
import com.yonyoucloud.fi.cmp.cmpentity.EnableStatus;
import com.yonyoucloud.fi.cmp.constant.IBussinessConstant;
import com.yonyoucloud.fi.cmp.initdata.OpeningOutstanding;
import com.yonyoucloud.fi.cmp.journal.Journal;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.Direction;
import com.yonyoucloud.fi.cmp.util.CmpMetaDaoHelper;
import com.yonyoucloud.fi.cmp.util.DateUtils;
import com.yonyoucloud.fi.cmp.util.StringUtils;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import com.yonyoucloud.fi.cmp.util.business.CmpCommonUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.*;
@Slf4j
public class BankReconciliationSettingSaveRule extends AbstractCommonRule {
    @Autowired
    BaseRefRpcService baseRefRpcService;
    @Autowired
    private YmsOidGenerator ymsOidGenerator;
    @Resource
    @Qualifier("busiBaseDAO")
    private IYmsJdbcApi ymsJdbcApi;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        List<BizObject> bills = getBills(billContext, map);
        if (bills != null && bills.size() > 0) {
            BizObject bizObject = bills.get(0);
            //获取子表银行账户
            List<BankReconciliationSetting_b> bank_b = bizObject.get("bankReconciliationSetting_b");
            if (bank_b == null || bank_b.size() < 1) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101984"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041801DC","子表数据不能为空") /* "子表数据不能为空" */);
            }
            try {
                if (FIDubboUtils.isSingleOrg()) {
                    BizObject singleOrg = FIDubboUtils.getSingleOrg();
                    if (singleOrg != null) {
                        bizObject.set(IBussinessConstant.ACCENTITY, singleOrg.get("id"));
                        bizObject.set("accentity_name", singleOrg.get("name"));
                        for (BankReconciliationSetting_b bankVerification : bank_b) {
                            bankVerification.set("useorg",singleOrg.get("id"));
                            bankVerification.set("useorg_name",singleOrg.get("name"));
                        }
                        bizObject.set("bankReconciliationSetting_b",bank_b);
                    }
                }
            } catch (Exception e) {
                log.error("单组织判断异常!", e);
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101985"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041801D8","单组织判断异常！") /* "单组织判断异常！" */ + e.getMessage());
            }
            if (bizObject.get(IBussinessConstant.ACCENTITY) == null) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101986"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041801DA","会计主体不能为空！") /* "会计主体不能为空！" */);
            }

            Short reconciliationdatasource = bizObject.get("reconciliationdatasource");
            Set<String> bankcount = new HashSet<>();
            for (BankReconciliationSetting_b bankVerification : bank_b) {
                if (EntityStatus.Delete.equals(bankVerification.get("_status")) || EntityStatus.Unchanged.equals(bankVerification.get("_status"))) {
                    //明细删除时要联动删除关联的期初未达项
                    if (EntityStatus.Delete.equals(bankVerification.get("_status"))){
                        handleDeleteOpeningOutstanding(bankVerification);
                    }
                    continue;
                }
                if (StringUtils.isEmpty(bankVerification.get("bankaccount"))) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101987"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041801DF","银行账户不能为空") /* "银行账户不能为空" */);
                } else {
                    if (reconciliationdatasource == 2) {//日记账
                        if (StringUtils.isEmpty(bankVerification.get("currency"))) {
                            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101988"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041801D3","币种不能为空！") /* "币种不能为空！" */);
                        }
                        //银行账户+币种+授权使用组织重复
                        if(!bankcount.add(bankVerification.get("bankaccount").toString()+bankVerification.get("currency").toString()+bankVerification.getString("useorg"))){
                            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101989"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1B54484405A0001E","银行账户【%s】加币种【%s】加授权使用组织【%s】不可重复"),
                                    bankVerification.getString("bankaccount_name"),bankVerification.getString("currency_name"),bankVerification.getString("useorg_name")));
                        }
                    }
                }
            }
            checkBankCurrencyByStatus(bizObject);
            //凭证
            if (reconciliationdatasource == 1) {
                checkWebData(bank_b);  //检测页面数据是否重复
                //检测数据库数据是否重复
                QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("accbook").eq(bizObject.get("accbook")));
                QuerySchema schema = QuerySchema.create().addSelect("id");
                schema.addCondition(group);
                List<BankReconciliationSetting> list = MetaDaoHelper.query(BankReconciliationSetting.ENTITY_NAME, schema);
                if(ValueUtils.isNotEmpty(list) && list.size() > 0){
                    List<Map<String, Object>> records = new ArrayList<>();
                    List<Long> mainids = new ArrayList<>();
                    for (Map<String,Object> bankReconciliationSetting:list){
                        mainids.add((Long)bankReconciliationSetting.get("id"));
                    }
                    for (int i=0;i<bank_b.size();i++) {
                        if (EntityStatus.Delete.equals(bank_b.get(i).get("_status")) || EntityStatus.Unchanged.equals(bank_b.get(i).get("_status"))) {
                            continue;
                        }
                        group = QueryConditionGroup.and(QueryCondition.name("subject").eq(bank_b.get(i).get("subject")),QueryCondition.name("mainid").in(mainids));
                        if(EntityStatus.Update.equals(bank_b.get(i).get("_status"))){
                            group.addCondition(QueryConditionGroup.and(QueryCondition.name("mainid").not_eq(bank_b.get(i).get("mainid"))));
                        }
                        //过滤已停用的
                        group.addCondition(QueryCondition.name("enableStatus_b").not_eq(2));
                        schema = QuerySchema.create().addSelect("id,subject,assistaccountingtype,assistaccounting,bankaccount,currency,accbook_b");
                        schema.addCondition(group);
                        records = MetaDaoHelper.query(BankReconciliationSetting_b.ENTITY_NAME, schema);

                        if(ObjectUtils.isNotEmpty(bank_b.get(i).get("assistaccountingtype"))){
                            if(checkAssistaccountingType(bank_b.get(i),records)){
                                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101990"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041801D5","辅助核算类型不一致，请修改！") /* "辅助核算类型不一致，请修改！" */);
                            }
                        }
                        if(ObjectUtils.isEmpty(bank_b.get(i).get("assistaccountingtype"))&&ObjectUtils.isEmpty(bank_b.get(i).get("assistaccounting"))){
                            if (checkAssistaccounting(bank_b.get(i),records)) {
                                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101991"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041801D7","该科目已存在对应银行账户，不允许重复！") /* "该科目已存在对应银行账户，不允许重复！" */);
                            }
                        }
                        if (checkAssistaccounting(bank_b.get(i),records)) {
                            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101992"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041801DB","该科目+辅助核算已存在对应银行账户，不允许重复！") /* "该科目+辅助核算已存在对应银行账户，不允许重复！" */);
                        }
                        //凭证按照对账明细里的核算会计主体校验总账启用日期 20250304
                        //20260130,新增明细时只有不存在已启用的账号+币种数据时，才校验
                        boolean exists = hasEnabledMatchingBankSetting(bank_b, bank_b.get(i));
                        if(EntityStatus.Insert.equals(bank_b.get(i).get("_status")) && !exists && !CmpCommonUtil.checkDateByAccPeriod(bank_b.get(i).get("accentityRaw"),bizObject.get("enableDate"))){
                            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101993"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041801DD","方案启用日期早于总账启用日期，不允许添加。") /* "方案启用日期早于总账启用日期，不允许添加。" */);
                        }
                    }
                }else {
                    //第一次新增需要校验总账启用日期
                    for (int i=0;i<bank_b.size();i++) {
                        if (EntityStatus.Delete.equals(bank_b.get(i).get("_status")) || EntityStatus.Unchanged.equals(bank_b.get(i).get("_status"))) {
                            continue;
                        }
                        //凭证按照对账明细里的核算会计主体校验总账启用日期 20250304
                        if(EntityStatus.Insert.equals(bank_b.get(i).get("_status")) && !CmpCommonUtil.checkDateByAccPeriod(bank_b.get(i).get("accentityRaw"),bizObject.get("enableDate"))){
                            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101993"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041801DD","方案启用日期早于总账启用日期，不允许添加。") /* "方案启用日期早于总账启用日期，不允许添加。" */);
                        }
                    }
                }
            }else{
                //校验启用日期与现金启用日期，如果启用日期在现金启用日期之前，则不允许添加
                if(!CmpCommonUtil.checkDateByCmPeriod(bizObject.get(IBussinessConstant.ACCENTITY),bizObject.get("enableDate"))){
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101994"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041801D2","方案启用日期早于现金管理启用日期，不允许添加。") /* "方案启用日期早于现金管理启用日期，不允许添加。" */);
                }
            }
            //202404 变更操作时，处理期初未达数据
            if (bizObject.getShort("enableStatus") == EnableStatus.Enabled.getValue()){
                BankReconciliationSetting bankReconciliationSetting = new BankReconciliationSetting();
                bankReconciliationSetting.init(bizObject);
                List<String> keyList = new ArrayList<>();
                for (BankReconciliationSetting_b bankVerification : bank_b) {
                    //只处理变更新增的
                    if (!EntityStatus.Insert.equals(bankVerification.get("_status"))) {
                        continue;
                    }
                    //银行账号+币种相同只存一条期初未达
                    String key = bankVerification.getBankaccount() + bankVerification.getCurrency();
                    if(keyList.contains(key)){
                        continue;
                    }else {
                        keyList.add(key);
                    }
                    List<OpeningOutstanding> existList= getOldOpeningOutstanding(bankReconciliationSetting,bankVerification);
                    //未空时添加
                    if (CollectionUtils.isEmpty(existList)){
                        OpeningOutstanding openingOutstanding = new OpeningOutstanding();
                        openingOutstanding.setAccentity(bankReconciliationSetting.getAccentity());
                        openingOutstanding.setBankreconciliationscheme(bankReconciliationSetting.getId());
                        openingOutstanding.setBankreconciliationscheme_b(bankVerification.getId()+"");
                        openingOutstanding.setReconciliationdatasource(bankReconciliationSetting.getReconciliationdatasource());
                        openingOutstanding.setAccbook(bankVerification.getAccbook_b());
                        openingOutstanding.setEnableDate(bankReconciliationSetting.getEnableDate());
                        openingOutstanding.setEnableStatus(EnableStatus.Enabled);
                        openingOutstanding.setBankaccount(bankVerification.getBankaccount());
                        openingOutstanding.setCurrency(bankVerification.getCurrency());
                        openingOutstanding.setBankdirection(Direction.Debit);
                        openingOutstanding.setBankinitoribalance(BigDecimal.ZERO);
                        openingOutstanding.setId(ymsOidGenerator.nextId());
                        CmpMetaDaoHelper.insert(OpeningOutstanding.ENTITY_NAME, openingOutstanding);
                    }
                }
            }
        }

        return new RuleExecuteResult();
    }

    /**
     * 校验辅助项类型
     * @param bank
     * @return
     * @throws Exception
     */
    private boolean checkAssistaccountingType(BankReconciliationSetting_b bank,List<Map<String, Object>> records) throws Exception {
            if(records!=null){
                for (Map<String,Object> map:records){
                    String assistaccountingtype = "";
                    if(ObjectUtils.isNotEmpty(map.get("assistaccountingtype"))){
                        assistaccountingtype  = (String)map.get("assistaccountingtype");
                    }
                    if(ObjectUtils.isEmpty(bank.get("assistaccountingtype"))&&StringUtil.isEmpty(assistaccountingtype)){
                        continue;
                    }
                    if(bank.getAccbook_b().equals(map.get("accbook_b").toString()) && !assistaccountingtype.equals(bank.get("assistaccountingtype"))){
                        return true;
                    }
                }
            }

            return false;
    }

    /**
     * 校验辅助项
     * @param bank
     * @return
     * @throws Exception
     */
    private boolean checkAssistaccounting(BankReconciliationSetting_b bank,List<Map<String, Object>> records) throws Exception {
        Map<String,Object> bankmap = new HashMap<>();
        bankmap.put("subject", bank.get("subject"));
        bankmap.put("bankaccount",bank.get("bankaccount"));
        if(StringUtil.isNotEmpty(bank.get("assistaccountingtype"))){
            bankmap.put("assistaccountingtype",bank.get("assistaccountingtype"));
        }else{
            bankmap.put("assistaccountingtype","");
        }
        if(StringUtil.isNotEmpty(bank.get("assistaccounting"))){
            bankmap.put("assistaccounting", bank.get("assistaccounting"));
        }else{
            bankmap.put("assistaccounting","");
        }
        if(records!=null){
            for (Map<String,Object> map:records){
                if(map.get("assistaccountingtype")==null){
                    map.put("assistaccountingtype","");
                }
                if(map.get("assistaccounting")==null){
                    map.put("assistaccounting","");
                }
                if(bank.getAccbook_b().equals(map.get("accbook_b").toString()) && map.get("subject").equals(bankmap.get("subject"))&&
                        map.get("assistaccountingtype").equals(bankmap.get("assistaccountingtype"))&&map.get("assistaccounting").equals(bankmap.get("assistaccounting"))&&!map.get("bankaccount").equals(bankmap.get("bankaccount"))){
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * 校验页面数据是否重复，只有凭证数据源会走此逻辑
     * @param bank_b
     */
    private void checkWebData(List<BankReconciliationSetting_b> bank_b){

        ArrayList<Map<String, Object>> bankarray = new ArrayList<>();
        Map<String, List<BankReconciliationSetting_b>> bankAccountCurrencyMap = new HashMap<>();
        for(BankReconciliationSetting_b bank : bank_b){
            if (EntityStatus.Delete.equals(bank.get("_status"))) {
                continue;
            }
            // 构建唯一键：bankaccount + currency + userorg
            String key = bank.getBankaccount() + "_" + bank.getCurrency() + "_" + bank.getUseorg();
            bankAccountCurrencyMap.computeIfAbsent(key, k -> new ArrayList<>()).add(bank);

            //已停用数据不参与校验
            if (bank.getEnableStatus_b() == EnableStatus.Disabled.getValue()) {
                continue;
            }
            Map<String, Object> bankmap = new HashMap<>();
            if (StringUtil.isNotEmpty(bank.get("assistaccountingtype")) && StringUtil.isEmpty(bank.get("assistaccounting"))) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101995"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041801D9","已选择辅助核算类型，辅助核算项不允许为空！") /* "已选择辅助核算类型，辅助核算项不允许为空！" */);
            }
            //20250826增加了报告账簿逻辑，需要增加账簿的判断
            bankmap.put("accbook_b", bank.get("accbook_b"));
            bankmap.put("subject", bank.get("subject"));
            if(StringUtil.isNotEmpty(bank.get("assistaccountingtype"))){
                bankmap.put("assistaccountingtype",bank.get("assistaccountingtype"));
            }
            if(StringUtil.isNotEmpty(bank.get("assistaccounting"))){
                bankmap.put("assistaccounting", bank.get("assistaccounting"));
            }
            if(StringUtil.isNotEmpty(bank.get("bankaccount"))){
                bankmap.put("bankaccount",bank.get("bankaccount"));
            }
            if(StringUtil.isNotEmpty(bank.get("currency"))){
                bankmap.put("currency", bank.get("currency"));
            }
            //账户共享加入授权使用组织过滤
            if(StringUtil.isNotEmpty(bank.get("useorg"))){
                bankmap.put("useorg", bank.get("useorg"));
            }
            bankarray.add(bankmap);
        }

        // 新增校验：同一个 bankaccount + currency +userorg 只能有一个 accbook_b
        for (Map.Entry<String, List<BankReconciliationSetting_b>> entry : bankAccountCurrencyMap.entrySet()) {
            List<BankReconciliationSetting_b> items = entry.getValue();
            Set<String> accbookSet = new HashSet<>();
            for (BankReconciliationSetting_b item : items) {
                accbookSet.add(item.get("accbook_b").toString());
            }
            if (accbookSet.size() > 1) {
                throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_2233859204E80005", "同一个方案下，同一账户、币种和账户使用组织不能维护多个入账账簿。") /* "同一个方案下，同一账户、币种和账户使用组织不能维护多个入账账簿。" */);
            }
        }

        for (int i = 0; i < bankarray.size(); i++) {
            for (int j = bankarray.size() - 1; j > i; j--) {
                if (bankarray.get(i).equals(bankarray.get(j))) {
                    if (bankarray.remove(j) != null) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101996"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041801D6","子表数据重复，请重新输入") /* "子表数据重复，请重新输入" */);
                    }
                }
                Map<String, Object> map1 = bankarray.get(i);
                Map<String,Object> map2 = bankarray.get(j);
                if(ObjectUtils.isEmpty(map1.get("assistaccountingtype"))){
                    map1.put("assistaccountingtype","");
                }
                if(ObjectUtils.isEmpty(map2.get("assistaccountingtype"))){
                    map2.put("assistaccountingtype","");
                }
                if(ObjectUtils.isEmpty(map1.get("assistaccounting"))){
                    map1.put("assistaccounting","");
                }
                if(ObjectUtils.isEmpty(map2.get("assistaccounting"))){
                    map2.put("assistaccounting","");
                }
                //20250826增加了报告账簿逻辑，需要判断账簿相同
                if(map1.get("accbook_b").toString().equals(map2.get("accbook_b").toString()) && map1.get("subject").equals(map2.get("subject"))&&!map1.get("assistaccountingtype").equals(map2.get("assistaccountingtype"))){
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101990"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041801D5","辅助核算类型不一致，请修改！") /* "辅助核算类型不一致，请修改！" */);
                }
                if(map1.get("accbook_b").toString().equals(map2.get("accbook_b").toString()) && map1.get("subject").equals(map2.get("subject"))&&map1.get("assistaccountingtype").equals(map2.get("assistaccountingtype"))&&map1.get("assistaccounting").equals(map2.get("assistaccounting"))&&!map1.get("bankaccount").equals(map2.get("bankaccount"))){
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101992"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041801DB","该科目+辅助核算已存在对应银行账户，不允许重复！") /* "该科目+辅助核算已存在对应银行账户，不允许重复！" */);
                }

            }
        }

    }

    /**
     * 银行账号+币种 是否存在于其他已启用的对账方案
     * 1.原有判重逻辑调整（保存）：由银行账户+币种，不区分所属组织，增加判重维度；（即一个账户+币种只允许维护在一个相同数据源的对账方案内）
     * 2.对账设置保存时，首先判定该对账方案下子表行，是否存在银行账户+币种+授权使用组织3个维度完全一致，且启用状态='已启用、已保存'的数据，如一致则阻断提示：存在相同账户【XXX】、币种【YYY】、授权组织【ZZZ】的数据，请检查！”
     * 3.其次判定相同数据源且主表启用状态=“启用”的其他对账方案，是否存在对账数据源+银行账号+币种完全一致的数据，如存在则阻断提示：“系统中已存在对账方案【XXX】，所属组织【XXX】,数据源为【凭证】\【银行日记账】、账户【XXX】、币种【YYY】、状态非停用态，请检查！”*
     * 4.对账方案保存时，判定“对账数据源+银行账户+币种+授权使用组织”是否存在已停用的对账方案，如有则校验启用日期是否早于等于已停用对账方案的停用日期，否则阻断，提示："启用日期不能早于等于同一账户【XXX】、币种【YYY】、授权组织【ZZZ】的停用日期【YYYY-MM-DD】,请检查！"*
     * @param bizObject
     * @throws Exception
     */
    public  void checkBankCurrencyByStatus(BizObject bizObject) throws Exception{
        List<BankReconciliationSetting_b> addBank_bs = bizObject.get("bankReconciliationSetting_b");
        List<BankReconciliationSetting> bankReconciliationSettings =getOldBankReconciliation(bizObject);

        if (CollectionUtils.isEmpty(addBank_bs) || CollectionUtils.isEmpty(bankReconciliationSettings)) {
            return;
        }
        for (BankReconciliationSetting_b addBank_b:addBank_bs){
            if (StringUtil.isEmpty(addBank_b.getBankaccount()) || StringUtil.isEmpty(addBank_b.getCurrency())) {
                continue;
            }
            for(BankReconciliationSetting bankReconciliationSetting:bankReconciliationSettings){
                List<BankReconciliationSetting_b> bank_bs = bankReconciliationSetting.get("BankReconciliationSetting_b");
                if (CollectionUtils.isEmpty(bank_bs)) {
                    continue;
                }
                //判断是否有重复数据
                processBankSettingComparison(bizObject, addBank_b, bankReconciliationSetting, bank_bs);
            }
        }
    }

    //处理主要比较逻辑
    private void processBankSettingComparison(BizObject bizObject,BankReconciliationSetting_b addBank_b,BankReconciliationSetting bankReconciliationSetting,
                                              List<BankReconciliationSetting_b> bank_bs) throws Exception {
            for(BankReconciliationSetting_b bank_b :bank_bs){
            // 已停用子表明细处理
            if (bank_b.getEnableStatus_b() == EnableStatus.Disabled.getValue()){
                processDisabledBankSetting(bizObject, addBank_b, bankReconciliationSetting, bank_b);
                continue;
            }
            // 凭证数据源校验（reconciliationdatasource == 1）
            if (isSameBankAndCurrencyForVoucher(bizObject, addBank_b, bank_b)) {
                CurrencyTenantDTO currencyDTO = baseRefRpcService.queryCurrencyById(bank_b.getCurrency());
                throw new CtmException(
                        String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B9D63C04D0000C","账户【%s】、币种【%s】已维护入账科目信息，无法重复添加"),
                                addBank_b.get("bankaccount_name"),
                                currencyDTO.getName()
                        )
                );
            }
            // 银行日记账校验（reconciliationdatasource == 2）
            if (isSameBankAndCurrencyForJournal(bizObject, addBank_b, bank_b)) {
                CurrencyTenantDTO currencyDTO = baseRefRpcService.queryCurrencyById(bank_b.getCurrency());
                throw new CtmException(
                        new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101998"),
                        String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1B54484405A00021","存在相同账户【%s】、币种【%s】、授权组织【%s】的数据，请检查！"),
                        addBank_b.get("bankaccount_name"),
                        currencyDTO.getName(),
                        addBank_b.get("useorg_name"))
                );
            }
            // 通用校验：系统中已存在对账方案
            if (isSameDataSourceAndBank(bizObject, addBank_b, bankReconciliationSetting, bank_b)) {
                CurrencyTenantDTO currencyDTO = baseRefRpcService.queryCurrencyById(bank_b.getCurrency());
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101999"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1B54484405A0001D","系统中已存在对账方案【%s】，所属组织【%s】,数据源为【%s】、账户【%s】、币种【%s】、状态非停用态，请检查！"),
                                bankReconciliationSetting.getBankreconciliationschemename(),
                                bizObject.getString("accentity_name"),
                                bizObject.getShort("reconciliationdatasource") == 1 ?
                                com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1B54484405A00020","凭证") :
                                com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1B54484405A0001F","银行日记账"),
                                addBank_b.get("bankaccount_name"),
                                currencyDTO.getName())
                );
            }
        }
    }

    //处理已停用记录的特殊情况
    private void processDisabledBankSetting(BizObject bizObject,BankReconciliationSetting_b addBank_b,BankReconciliationSetting bankReconciliationSetting,
                                            BankReconciliationSetting_b bank_b) throws Exception {
        if (bankReconciliationSetting.getEnableStatus().getValue() != EnableStatus.Disabled.getValue()) {
            return;
        }
        if (StringUtil.isEmpty(bank_b.getBankaccount()) || StringUtil.isEmpty(bank_b.getCurrency())) {
            return;
        }
        if(bizObject.getShort("reconciliationdatasource") == bankReconciliationSetting.getReconciliationdatasource().getValue()
                && bank_b.getBankaccount().equals(addBank_b.getBankaccount())
                && bank_b.getCurrency().equals(addBank_b.getCurrency())
                && (addBank_b.getUseorg() == null || addBank_b.getUseorg().equals(bank_b.getUseorg()))){

            if (bizObject.getDate("enableDate").compareTo(bankReconciliationSetting.getStopDate()) <= 0) {
                    CurrencyTenantDTO currencyDTO = baseRefRpcService.queryCurrencyById(bank_b.getCurrency());
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101997"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1B54484405A00022","启用日期不能早于等于同一账户【%s】、币种【%s】、授权组织【%s】的停用日期【%s】,请检查！"),
                                addBank_b.get("bankaccount_name"),
                                currencyDTO.getName(),
                                addBank_b.get("useorg_name"),
                                DateUtils.parseDateToStr(bankReconciliationSetting.getStopDate(),"yyyy-MM-dd"))
                    );
                }
            }
    }

    //判断凭证数据源重复条件
    //校验相同账户、币种、授权组织、对账财务账簿、科目、辅助核算类型、辅助核算 不能完全一致；
    private boolean isSameBankAndCurrencyForVoucher(BizObject bizObject,BankReconciliationSetting_b addBank_b,BankReconciliationSetting_b bank_b) {
        if (bizObject.getShort("reconciliationdatasource") != 1 ||
                StringUtil.isEmpty(bank_b.getBankaccount()) ||
                StringUtil.isEmpty(bank_b.getCurrency())) {
            return false;
        }

        boolean assistAccountingTypeBothEmpty = (StringUtil.isEmpty(bank_b.get("assistaccountingtype")) &&
                StringUtil.isEmpty(addBank_b.get("assistaccountingtype")));
        boolean assistAccountingBothEmpty = (StringUtil.isEmpty(bank_b.get("assistaccounting")) &&
                StringUtil.isEmpty(addBank_b.get("assistaccounting")));

        return bank_b.getBankaccount().equals(addBank_b.getBankaccount()) &&
                bank_b.getCurrency().equals(addBank_b.getCurrency()) &&
                (addBank_b.getUseorg() == null || addBank_b.getUseorg().equals(bank_b.getUseorg())) &&
                bank_b.get("accbook_b").equals(addBank_b.get("accbook_b")) &&
                bank_b.get("subject").equals(addBank_b.get("subject")) &&
                (assistAccountingTypeBothEmpty ||
                        (ObjectUtils.isNotEmpty(bank_b.get("assistaccountingtype")) &&
                                bank_b.get("assistaccountingtype").equals(addBank_b.get("assistaccountingtype")))) &&
                (assistAccountingBothEmpty ||
                        (ObjectUtils.isNotEmpty(bank_b.get("assistaccounting")) &&
                                bank_b.get("assistaccounting").equals(addBank_b.get("assistaccounting"))));
            }

    //判断银行日记账重复条件
    //校验相同账户、币种、授权组织的数据
    private boolean isSameBankAndCurrencyForJournal(BizObject bizObject,BankReconciliationSetting_b addBank_b,BankReconciliationSetting_b bank_b) {
        return bizObject.getShort("reconciliationdatasource") == 2 &&
                StringUtil.isNotEmpty(bank_b.getBankaccount()) &&
                StringUtil.isNotEmpty(bank_b.getCurrency()) &&
                bank_b.getBankaccount().equals(addBank_b.getBankaccount()) &&
                bank_b.getCurrency().equals(addBank_b.getCurrency()) &&
                (addBank_b.getUseorg() == null || addBank_b.getUseorg().equals(bank_b.getUseorg()));
        }

    //判断通用重复条件
    private boolean isSameDataSourceAndBank(BizObject bizObject,BankReconciliationSetting_b addBank_b,BankReconciliationSetting bankReconciliationSetting,
                                            BankReconciliationSetting_b bank_b) {
        // 必须满足基础条件
        if (StringUtil.isEmpty(bank_b.getBankaccount()) || StringUtil.isEmpty(bank_b.getCurrency())) {
            return false;
        }
        // 检查对账数据源是否相同
        if (bizObject.getShort("reconciliationdatasource") != bankReconciliationSetting.getReconciliationdatasource().getValue()) {
            return false;
        }
        // 检查银行账户和币种是否相同
        if (!bank_b.getBankaccount().equals(addBank_b.getBankaccount()) ||
                !bank_b.getCurrency().equals(addBank_b.getCurrency())) {
            return false;
        }
        // 检查子表行的启用状态必须是已启用
        if (bank_b.getEnableStatus_b() != EnableStatus.Enabled.getValue()) {
            return false;
        }

        //1凭证；2银行日记账
        short dataSource = bizObject.getShort("reconciliationdatasource");
        // 数据源为2时，只需满足上述条件
        if (dataSource == 2) {
            return true;
        }
        // 数据源为1时，需额外检查凭证相关字段
        if (dataSource == 1) {
            // 检查账簿、科目、辅助核算类型和辅助核算是否都相同
            return Objects.equals(bank_b.get("accbook_b"), addBank_b.get("accbook_b")) &&
                    Objects.equals(bank_b.get("subject"), addBank_b.get("subject")) &&
                    Objects.equals(bank_b.get("assistaccountingtype"), addBank_b.get("assistaccountingtype")) &&
                    Objects.equals(bank_b.get("assistaccounting"), addBank_b.get("assistaccounting"));
        }
        return false;
    }


    /**
     * 按会计主体、数据源查询已启用的对账方案
     * @param bizObject
     * @return
     * @throws Exception
     */
    private List<BankReconciliationSetting> getOldBankReconciliation(BizObject bizObject) throws Exception{
        //按会计主体、数据源查询已启用的对账方案
        List<Object> statusList = new ArrayList<>();
        statusList.add(EnableStatus.Saved.getValue());
        statusList.add(EnableStatus.Enabled.getValue());
        statusList.add(EnableStatus.Disabled.getValue());
        QueryConditionGroup mainGroup = QueryConditionGroup.and(QueryCondition.name(IBussinessConstant.ACCENTITY).eq(bizObject.get(IBussinessConstant.ACCENTITY)),QueryCondition.name("reconciliationdatasource").eq(bizObject.get("reconciliationdatasource")),QueryCondition.name("enableStatus").in(statusList));
        if(ObjectUtils.isNotEmpty(bizObject.get("id"))&&EntityStatus.Update.equals(bizObject.get("_status"))){
            mainGroup.addCondition(QueryConditionGroup.and(QueryCondition.name("id").not_eq(bizObject.get("id"))));
        }
        QuerySchema mainSchema = QuerySchema.create().addSelect("*");
        mainSchema.addCondition(mainGroup);
        List<Map<String,Object>> bankReconciliationSettings = MetaDaoHelper.query(BankReconciliationSetting.ENTITY_NAME, mainSchema);
        List<BankReconciliationSetting> list = new ArrayList<>();

        if(bankReconciliationSettings!=null){
            for(Map<String,Object> map:bankReconciliationSettings){
                BankReconciliationSetting bankReconciliationSetting = new BankReconciliationSetting();
                bankReconciliationSetting.init(map);
                bankReconciliationSetting.setBankReconciliationSetting_b(getBank_b(bankReconciliationSetting));
                list.add(bankReconciliationSetting);
            }
        }
        return list;
    }

    /**
     * 获取子表数据
     * @param bankReconciliationSetting
     * @return
     * @throws Exception
     */
    private List<BankReconciliationSetting_b> getBank_b(BankReconciliationSetting bankReconciliationSetting) throws Exception{
        QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("mainid").eq(bankReconciliationSetting.getId()));
        QuerySchema schema = QuerySchema.create().addSelect("*");
        schema.addCondition(group);
        List<Map<String,Object>> bankReconciliationSetting_bs = MetaDaoHelper.query(BankReconciliationSetting_b.ENTITY_NAME, schema);
        List<BankReconciliationSetting_b> bank_bs = new ArrayList<>();
        for (Map<String,Object> map:bankReconciliationSetting_bs){
            BankReconciliationSetting_b bankReconciliationSetting_b = new BankReconciliationSetting_b();
            bankReconciliationSetting_b.init(map);
            bank_bs.add(bankReconciliationSetting_b);
        }
        return bank_bs;
    }


    /**
     * 根据对账方案，账号和币种查询存在的
     *
     */
    private List<OpeningOutstanding> getOldOpeningOutstanding(BankReconciliationSetting bankReconciliationSetting,BankReconciliationSetting_b bank_b) throws Exception {
        QueryConditionGroup group = QueryConditionGroup.and(
                QueryCondition.name("bankreconciliationscheme").eq(bankReconciliationSetting.getId()),
                QueryCondition.name("bankaccount").eq(bank_b.getBankaccount()),
                QueryCondition.name("currency").eq(bank_b.getCurrency())
        );
        QuerySchema schema = new QuerySchema().addSelect("*");
        schema.addCondition(group);
        return  MetaDaoHelper.queryObject(OpeningOutstanding.ENTITY_NAME, schema, null);
    }

    /**
     * 对账方案明细删除时，联动删除关联的期初未达项
     * @param bank_b
     */
    private void handleDeleteOpeningOutstanding(BankReconciliationSetting_b bank_b) throws Exception{
        //若同一个对账方案里不存在其他同账号和币种的对账方案明细，则删除对应的期初未达项
        QuerySchema schema = QuerySchema.create().addSelect("id");
        QueryConditionGroup groupItem = QueryConditionGroup.and(
                QueryCondition.name("bankaccount").eq(bank_b.getBankaccount()),
                QueryCondition.name("currency").eq(bank_b.getCurrency()),
                QueryCondition.name("mainid").eq(bank_b.getMainid()),
                QueryCondition.name("id").not_eq(bank_b.getId())
        );
        schema.addCondition(groupItem);
        List<Map<String, Object>> bankReconciliationSetting_bs = MetaDaoHelper.query(BankReconciliationSetting_b.ENTITY_NAME, schema);
        if (bankReconciliationSetting_bs.size() == 0){
            QueryConditionGroup group = QueryConditionGroup.and(
                    QueryCondition.name("bankreconciliationscheme").eq(bank_b.getMainid()),
                    QueryCondition.name("bankaccount").eq(bank_b.getBankaccount()),
                    QueryCondition.name("currency").eq(bank_b.getCurrency())
            );
            QuerySchema schemaOpeningOutstanding = new QuerySchema().addSelect("*");
            schemaOpeningOutstanding.addCondition(group);
            List<OpeningOutstanding> openingOutstandings = MetaDaoHelper.queryObject(OpeningOutstanding.ENTITY_NAME, schemaOpeningOutstanding, null);
            if (openingOutstandings != null && openingOutstandings.size() > 0) {
                MetaDaoHelper.delete(OpeningOutstanding.ENTITY_NAME, openingOutstandings);
                //删除关联的日记账或者流水期初数据
                deleteQcBill(bank_b);
            }
        }
    }

    /**
     * 删除关联的期初数据
     * @throws Exception
     */
    private void deleteQcBill(BankReconciliationSetting_b bank_b) throws Exception{
        QuerySchema schema = new QuerySchema().addSelect("id");
        QueryConditionGroup conditionGroup = QueryConditionGroup.and(
                QueryCondition.name("bankreconciliationscheme").eq(bank_b.getMainid()),
                QueryCondition.name("bankaccount").eq(bank_b.getBankaccount()),
                QueryCondition.name("currency").eq(bank_b.getCurrency())
        );
        schema.addCondition(conditionGroup);
        List<Journal> journalwds = MetaDaoHelper.queryObject(Journal.ENTITY_NAME, schema,null);
        List<BankReconciliation> bankReconciliationwds = MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME, schema,null);

        if(journalwds!=null&&journalwds.size()>0){
            MetaDaoHelper.delete(Journal.ENTITY_NAME,journalwds);
        }
        if(bankReconciliationwds!=null&&bankReconciliationwds.size()>0){
            CommonBankReconciliationProcessor.batchReconciliationBeforeDelete(bankReconciliationwds,ymsJdbcApi);
            MetaDaoHelper.delete(BankReconciliation.ENTITY_NAME,bankReconciliationwds);
        }
    }

    /**
     * 判断 BankReconciliationSetting_b 集合中是否存在与给定 item
     * 具有相同 bankaccount 和 currency，并且 enableStatus_b=1（已启用）的数据
     * @param totalItems 所有子表行信息
     * @param item 当前子表行数据
     * @return
     */
    private boolean hasEnabledMatchingBankSetting(List<BankReconciliationSetting_b> totalItems,
                                                  BankReconciliationSetting_b item) {
        if (totalItems == null){
            return false;
        }
        return totalItems.stream()
                .anyMatch(bankSetting ->
                        // 排除自身（通过ID判断是否为同一数据）
                        !Objects.equals(bankSetting.getId(), item.getId()) &&
                        // 状态为已启用 (enableStatus_b == 1)
                        bankSetting.getEnableStatus_b() == EnableStatus.Enabled.getValue() &&
                        // 银行账户相同
                        Objects.equals(bankSetting.getBankaccount(), item.getBankaccount()) &&
                        // 币种相同
                        Objects.equals(bankSetting.getCurrency(), item.getCurrency()));
    }
}
