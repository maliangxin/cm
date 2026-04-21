package com.yonyoucloud.fi.cmp.openingoutstanding;

import com.yonyou.iuap.yms.id.generator.YmsOidGenerator;
import com.yonyou.iuap.yms.lock.YmsLock;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.balanceadjust.service.impl.BalanceAdjustService;
import com.yonyoucloud.fi.cmp.bankaccountsetting.BankAccountSettingService;
import com.yonyoucloud.fi.cmp.bankreconciliationsetting.BankReconciliationSetting_b;
import com.yonyoucloud.fi.cmp.cmpentity.EnableStatus;
import com.yonyoucloud.fi.cmp.cmpentity.ReconciliationDataSource;
import com.yonyoucloud.fi.cmp.common.CtmErrorCode;
import com.yonyoucloud.fi.cmp.initdata.OpeningOutstanding;
import com.yonyoucloud.fi.cmp.initdata.OpeningOutstanding_b;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.Direction;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.bankAccountSetting.BankAccountSettingVO;
import com.yonyoucloud.fi.cmp.util.DateUtils;
import com.yonyoucloud.fi.cmp.util.EntityTool;
import com.yonyoucloud.fi.cmp.util.JedisLockUtils;
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

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.*;

@Service
@Transactional(rollbackFor = RuntimeException.class)
public class OpeningOutstandingServiceImpl implements OpeningOutstandingService {

    @Autowired
    private BalanceAdjustService balanceAdjustService;
    @Autowired
    private BankAccountSettingService bankAccountSettingService;
    @Autowired
    private YmsOidGenerator ymsOidGenerator;

    @Override
    public void updateOpeningOutstanding(CtmJSONObject params) throws Exception {
        YmsLock ymsLock = JedisLockUtils.lockBillWithOutTrace(params.get("id").toString());
        if (null == ymsLock) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101069"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B000B5", "该单据已锁定，请稍后重试！") /* "该单据已锁定，请稍后重试！" */);
        }
        try {
            BizObject bizObject = MetaDaoHelper.findById(OpeningOutstanding.ENTITY_NAME,params.get("id"));
            OpeningOutstanding openingOutstanding = new OpeningOutstanding();
            openingOutstanding.init(bizObject);
            openingOutstanding.setEnableStatus(EnableStatus.find(bizObject.get("enableStatus")));//灰度环境未知原因报错
            openingOutstanding.setBankinitoribalance(params.getBigDecimal("bankinitoribalance"));
            openingOutstanding.setBankdirection(Direction.find( NumberFormat.getInstance().parse(params.getString("bankdirection")) ));
            openingOutstanding.setDirection(Direction.find(NumberFormat.getInstance().parse(params.getString("direction"))));
            openingOutstanding.setCoinitloribalance(params.getBigDecimal("coinitloribalance"));
            EntityTool.setUpdateStatus(openingOutstanding);
            MetaDaoHelper.update(OpeningOutstanding.ENTITY_NAME,openingOutstanding);
        }catch (Exception e){
            throw e;
        }finally {
            JedisLockUtils.unlockBillWithOutTrace(ymsLock);
        }
    }

    @Override
    public List<Map<String, Object>> queryItemsByMainId(String mainId) throws Exception {
        try {
            QuerySchema schema = QuerySchema.create().addSelect("id","useOrg as useOrgId","useOrg.name as useOrgName",
                    "direction","coinitloribalance","bankdirection","bankinitoribalance");
            QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
            conditionGroup.appendCondition(QueryCondition.name("mainid").eq(mainId));
            schema.addCondition(conditionGroup);
            return MetaDaoHelper.query(BankReconciliationSetting_b.ENTITY_NAME, schema);
        } catch (Exception e) {
            QuerySchema schema = QuerySchema.create().addSelect("id","useOrg","useOrg.name",
                    "direction","coinitloribalance","bankdirection","bankinitoribalance");
            QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
            conditionGroup.appendCondition(QueryCondition.name("mainid").eq(mainId));
            schema.addCondition(conditionGroup);
            List<Map<String, Object>> result = MetaDaoHelper.query(OpeningOutstanding_b.ENTITY_NAME, schema);
            if(CollectionUtils.isNotEmpty(result)){
                for (Map<String, Object> openingOutstandingB : result) {
                    openingOutstandingB.put("useOrgId",openingOutstandingB.get("useOrg"));
                    openingOutstandingB.put("useOrgName",openingOutstandingB.get("useOrg_name"));
                }
            }
            return result;
        }
    }

    @Override
    public CtmJSONObject syncOpeningBalance(BizObject bizobject) throws Exception {
        CtmJSONObject result = new CtmJSONObject();
        BizObject  currentBill = MetaDaoHelper.findById(OpeningOutstanding.ENTITY_NAME,bizobject.getId(),3);
        if(currentBill == null){
            throw new CtmException(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_20D459FA0410000F", "对账账号【%s】关联的期初未达项数据不存在") /* "对账账号【%s】关联的期初未达项数据不存在" */,bizobject.getString("bankaccount_account")));
        }
        OpeningOutstanding openingOutstanding = new OpeningOutstanding();
        openingOutstanding.init(currentBill);
        //判断是否停用
        if (openingOutstanding.getEnableStatus().getValue() == EnableStatus.Disabled.getValue()){
            throw new CtmException(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_20D459FA04100010", "对账账号【%s】对账账户已停用") /* "对账账号【%s】对账账户已停用" */,bizobject.getString("bankaccount_account")));
        }
        //判断是否存在多个账户使用组织
        List<Map<String, Object>> userOrgMapList = getUseOrgList(openingOutstanding);
        if (userOrgMapList.size() > 1 ){
            throw new CtmException(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_20D459FA0410000C", "对账账号【%s】关联多个使用组织，需手动进行期初余额设置") /* "对账账号【%s】关联多个使用组织，需手动进行期初余额设置" */,bizobject.getString("bankaccount_account")));
        }
        if (userOrgMapList.size() == 0 ){
            throw new CtmException(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_20D459FA0410000E", "对账账号【%s】没有关联可用的账户使用组织") /* "对账账号【%s】没有关联可用的账户使用组织" */,bizobject.getString("bankaccount_account")));
        }
        //银行方期初余额，查询账户历史余额的日期为启用日期前一天
        Date enableDate= openingOutstanding.getEnableDate();
        String queryDateStr = DateUtils.dateFormat(DateUtils.dateAddDays(enableDate,-1),"yyyy-MM-dd");
        //查询银行账户历史余额
        BankAccountSettingVO bankAccountSettingVO = new BankAccountSettingVO();
        bankAccountSettingVO.setAccentity(userOrgMapList.get(0).get("useorg") + "");
        bankAccountSettingVO.setEnableDateStr(queryDateStr);
        bankAccountSettingVO.setBankaccount(openingOutstanding.getBankaccount());
        bankAccountSettingVO.setCurrency(openingOutstanding.getCurrency());
        bankAccountSettingVO.setBankreconciliationscheme(openingOutstanding.getBankreconciliationscheme());
        CtmJSONObject historyBalance = balanceAdjustService.getBankAccountHistoryBalance(bankAccountSettingVO);
        if (historyBalance.getBoolean("isEmptyBalance")){
            throw new CtmException(new CtmErrorCode("033-502-105064"), String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_22F1453204580007", "对账账号【%1$s】启用日期前一天【%2$s】未维护账户历史余额，无法获取对应数据") /* "对账账号【%1$s】启用日期前一天【%2$s】未维护账户历史余额，无法获取对应数据" */,bizobject.getString("bankaccount_account"),queryDateStr));
        }

        //企业方期初余额 银行日记账/凭证
        Map<String,Object> coinitloribalanceMap = new HashMap<>();
        if (openingOutstanding.getReconciliationdatasource().getValue() == ReconciliationDataSource.Voucher.getValue()){
            coinitloribalanceMap = bankAccountSettingService.getVoucherBalance(bankAccountSettingVO);
        }
        if (openingOutstanding.getReconciliationdatasource().getValue() == ReconciliationDataSource.BankJournal.getValue()){
            //日记账期初余额启用日期要重新赋值，接口里会再-1天
            bankAccountSettingVO.setEnableDateStr(DateUtils.dateFormat(openingOutstanding.getEnableDate(),DateUtils.DATE_TIME_PATTERN));
            coinitloribalanceMap = bankAccountSettingService.getCoinitloribalanceByAccentity(bankAccountSettingVO);
        }

        //期初未达余额子表信息
        List<OpeningOutstanding_b> openingOutstanding_bs = openingOutstanding.openingOutstanding_b();
        if (openingOutstanding_bs == null || openingOutstanding_bs.size() == 0){ //需要新增
            OpeningOutstanding_b openingOutstanding_b = new OpeningOutstanding_b();
            openingOutstanding_b.setMainid(openingOutstanding.getId());
            openingOutstanding_b.setUseOrg(userOrgMapList.get(0).get("useorg") + "");
            openingOutstanding_b.setId(ymsOidGenerator.nextId());
            openingOutstanding_b.put("bankdirection",historyBalance.getShort("direction"));
            openingOutstanding_b.setBankinitoribalance(historyBalance.getBigDecimal("bankye"));
            if (openingOutstanding.getReconciliationdatasource().getValue() == ReconciliationDataSource.Voucher.getValue()){
                openingOutstanding_b.setAccountBook(userOrgMapList.get(0).get("accbookname") + "");
            }
            if ("Debit".equals(coinitloribalanceMap.get("direction").toString())){
                openingOutstanding_b.put("direction",Direction.Debit.getValue());
            }else {
                openingOutstanding_b.put("direction",Direction.Credit.getValue());
            }
            openingOutstanding_b.setCoinitloribalance((BigDecimal) coinitloribalanceMap.get("coinitloribalance"));
            openingOutstanding_b.setEntityStatus(EntityStatus.Insert);
            MetaDaoHelper.insert(OpeningOutstanding_b.ENTITY_NAME, openingOutstanding_b);
        }else {
            OpeningOutstanding_b openingOutstanding_b = openingOutstanding_bs.get(0);
            openingOutstanding_b.put("bankdirection",historyBalance.getShort("direction"));
            openingOutstanding_b.setBankinitoribalance(historyBalance.getBigDecimal("bankye"));
            if ("Debit".equals(coinitloribalanceMap.get("direction").toString())){
                openingOutstanding_b.put("direction",Direction.Debit.getValue());
            }else {
                openingOutstanding_b.put("direction",Direction.Credit.getValue());
            }
            openingOutstanding_b.setCoinitloribalance((BigDecimal) coinitloribalanceMap.get("coinitloribalance"));
            openingOutstanding_b.setEntityStatus(EntityStatus.Update);
            MetaDaoHelper.update(OpeningOutstanding_b.ENTITY_NAME, openingOutstanding_b);
        }

        //更新主表
        openingOutstanding.put("bankdirection",historyBalance.getShort("direction"));
        openingOutstanding.setBankinitoribalance(historyBalance.getBigDecimal("bankye"));
        if ("Debit".equals(coinitloribalanceMap.get("direction").toString())){
            openingOutstanding.put("direction",Direction.Debit.getValue());
        }else {
            openingOutstanding.put("direction",Direction.Credit.getValue());
        }
        openingOutstanding.setCoinitloribalance((BigDecimal) coinitloribalanceMap.get("coinitloribalance"));
        openingOutstanding.setEntityStatus(EntityStatus.Update);
        MetaDaoHelper.update(OpeningOutstanding.ENTITY_NAME, openingOutstanding);

        result.put("dealSuccess",true);
        return result;
    }

    /**
     * 判断期初未达关联多少个账户使用组织
     * @param openingOutstanding 期初未达项
     * @return 使用组织个数
     * @throws Exception
     */
    private List<Map<String, Object>> getUseOrgList(OpeningOutstanding openingOutstanding) throws Exception{
        List<Map<String, Object>> useOrgMapList = new ArrayList<>();
        QuerySchema schema = QuerySchema.create().addSelect("id","useorg","useorg.name as useorg_name","currency","accbook_b as accbook","accbook_b.name as accbookname");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(
                QueryCondition.name("mainid").eq(openingOutstanding.getBankreconciliationscheme()),
                QueryCondition.name("bankaccount").eq(openingOutstanding.getBankaccount()),
                QueryCondition.name("currency").eq(openingOutstanding.getCurrency()),
                QueryCondition.name("enableStatus_b").eq(EnableStatus.Enabled.getValue())
        );
        schema.addCondition(conditionGroup);
        List<Map<String, Object>> list = MetaDaoHelper.query(BankReconciliationSetting_b.ENTITY_NAME, schema);
        List<String> useOrgList = new ArrayList<>();
        for (Map<String, Object> map : list){
            if (!useOrgList.contains(map.get("useorg") + "")){
                useOrgMapList.add(map);
                useOrgList.add(map.get("useorg") + "");
            }
        }
        return useOrgMapList;
    }

}
