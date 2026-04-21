package com.yonyoucloud.fi.cmp.checkStockApply.rule;


import com.yonyou.iuap.yms.id.generator.YmsOidGenerator;
import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVO;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.cmp.autoparam.AutoConfig;
import com.yonyoucloud.fi.cmp.autoparam.service.AutoConfigService;
import com.yonyoucloud.fi.cmp.checkStock.service.CheckStatusService;
import com.yonyoucloud.fi.cmp.checkstock.CheckStock;
import com.yonyoucloud.fi.cmp.checkstockapply.CheckStockApply;
import com.yonyoucloud.fi.cmp.checkstockapply.CmpBusiType;
import com.yonyoucloud.fi.cmp.checkstockapply.RecCheckTemp;
import com.yonyoucloud.fi.cmp.cmpentity.AuditStatus;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.CmpCheckDir;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.CmpCheckStatus;
import com.yonyoucloud.fi.cmp.util.CmpMetaDaoHelper;
import com.yonyoucloud.fi.cmp.util.QueryBaseDocUtils;
import lombok.extern.slf4j.Slf4j;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.util.*;

/**
 * 支票入库审核前规则
 */
@Slf4j
@Component
public class CheckStockApplyBeforeAuditRule extends AbstractCommonRule {

    @Autowired
    BaseRefRpcService baseRefRpcService;
    @Autowired
    AutoConfigService autoConfigService;
    @Autowired
    YmsOidGenerator ymsOidGenerator;
    @Autowired
    CheckStatusService checkStatusService;
    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        List<BizObject> bills = getBills(billContext, map);
        List<CheckStockApply> list = new ArrayList();
        for (BizObject bizobject : bills) {
            log.info("CheckStockApplyBeforeAuditRule bizObject, id = {}, pubTs = {}", bizobject.getId(), bizobject.getPubts());
            CheckStockApply currentBill = MetaDaoHelper.findById(CheckStockApply.ENTITY_NAME, bizobject.getId());
            log.info("CheckStockApplyBeforeAuditRule currentBill, id = {}, pubTs = {}", currentBill.getId(), currentBill.getPubts());
            if (currentBill == null) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101109"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418050D","单据不存在 id:") /* "单据不存在 id:" */ + bizobject.getId());
            }
            Date currentPubts = bizobject.getPubts();
            if (currentPubts != null) {
                if (!currentPubts.equals(currentBill.getPubts())) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101110"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418050F","当前单据不是最新状态，请刷新单据重新操作。") /* "当前单据不是最新状态，请刷新单据重新操作。" */);
                }
            }
            list.add(currentBill);
        }
        // 根据支票入库单，生成支票
        List<CheckStock> checkStocks = buildCheckStorkByApply(list);
        // 支票号校验
        checkCheckStocks(checkStocks);
        List<CheckStockApply> updateRow = getUpdateApplyList(list, AuditStatus.Complete);
        checkStatusService.recordCheckStatusByInsert(checkStocks);
        CmpMetaDaoHelper.insert(CheckStock.ENTITY_NAME, checkStocks);
        for (BizObject bizobject : bills) {
            bizobject.set("auditstatus", AuditStatus.Complete.getValue());
        }
        return new RuleExecuteResult();
    }

    private List<CheckStock> buildCheckStorkByApply(List<CheckStockApply> checkStorkApplyRows) throws Exception {
        List<CheckStock> stockList = new ArrayList<>();
        for (CheckStockApply apply : checkStorkApplyRows) {
            short chequeType = apply.getChequeType();
            List<CheckStock> stocks = null;
            if (CmpBusiType.Black.getValue() == chequeType) {
                stocks = buildBlack(apply);
            } else {
                stocks = buildRec(apply);
            }
            stockList.addAll(stocks);
        }
        return stockList;
    }

    private List<CheckStock> buildBlack(CheckStockApply apply) throws Exception {
        List<CheckStock> stockList = new ArrayList<>();
        Integer stockNum = apply.getStockNum();
        short isused = 1;
        AutoConfig autoConfig = autoConfigService.queryAutoConfigByAccentity(apply.getAccentity());
        if(autoConfig != null){
            isused = autoConfig.getCheckStockIsUse()?(short) 0: (short) 1;
        }
        for (int i = 0; i < stockNum; i++) {
            CheckStock stock = new CheckStock();
            stock.setId(ymsOidGenerator.nextId());
            stock.setAccentity(apply.getAccentity());
            stock.setBillType(apply.getBillType());
            stock.setBusiDate(apply.getVouchdate());
            stock.setCheckBillDir(CmpCheckDir.Cash.getValue());
//            stock.setCheckBillStatus(CmpCheckStatus.InStock.getValue());
            stock.setCheckBillType(apply.getCheckBillType());
            stock.setCheckBookNo(apply.getCheckBookNo());
            stock.setCreateDate(new Date());
            stock.setCreater(apply.getCreator());
            stock.setCreatorId(apply.getCreatorId());
            stock.setCurrency(apply.getCurrency());
            EnterpriseBankAcctVO enterpriseBankAcctVO = baseRefRpcService.queryEnterpriseBankAccountById(apply.getAccount());
            stock.setCustNo(apply.getOrg());
            stock.setPayBank(enterpriseBankAcctVO.getBankNumber());
            stock.setInputBillNo(apply.getCode());
            stock.setIsLock("0");
            stock.setCheckBillStatus(CmpCheckStatus.InStock.getValue());
            stock.setDrawerAcct(apply.getAccount());
            stock.setDrawerAcctNo(enterpriseBankAcctVO.getAccount());
            stock.setDrawerAcctName(enterpriseBankAcctVO.getName());
            String checkBillNo = getCheckBillNoByNum(apply, i);
            stock.setCheckBillNo(checkBillNo);
            stock.setIsUsed(isused);
            stock.setMainid(apply.getId());
            stockList.add(stock);
        }
        return stockList;
    }

    private List<CheckStock> buildRec(CheckStockApply apply) throws Exception {
        Long id = (Long) apply.get("id");
        List<RecCheckTemp> tempList = getCheckTempById(id);
        List<CheckStock> stockList = new ArrayList<>();
        for (RecCheckTemp temp : tempList) {
            CheckStock stock = new CheckStock();
            stock.setId(ymsOidGenerator.nextId());
            stock.setAccentity(apply.getAccentity());
            stock.setBillType(apply.getBillType());
            stock.setBusiDate(temp.getDrawerDate());
            stock.setCheckBillDir(CmpCheckDir.Com.getValue());
            stock.setCheckBillStatus(CmpCheckStatus.InStock.getValue());
            stock.setCheckBillType(temp.getCheckBillType());
            stock.setCreateDate(new Date());
            stock.setCreater(apply.getCreator());
            stock.setCreatorId(apply.getCreatorId());
            stock.setCurrency(apply.getCurrency());
            stock.setCustNo(apply.getOrg());
            stock.setPayBank(temp.getPayBank());
            stock.setAmount(temp.getAmount());
            stock.setCheckBillNo(temp.getCheckBillNo());
            stock.setDrawerDate(temp.getDrawerDate());
            stock.setDrawerAcctNo(temp.getDrawerAcctNo());
            stock.setPayBank(temp.getPayBank());
            List<Map<String, Object>> orgMVById = QueryBaseDocUtils.getOrgMVById(apply.getOrg());/* 暂不修改 已经登记*/
            if (null != orgMVById && orgMVById.size() > 0) {
                stock.setPayeeName((String) orgMVById.get(0).get("name"));
            }
            stock.setEnableEndorse(temp.getEnableEndorse());
            stock.setMainid(temp.get("id"));
            stock.setInputBillNo(apply.getCode());
            stock.setIsLock("0");
            stock.setCheckBillStatus(CmpCheckStatus.InStock.getValue());
            //是否可使用状态，不受参数影响，默认为是
            stock.setIsUsed(Short.valueOf("1"));
            stock.setMainid(apply.getId());
            stockList.add(stock);
        }
        return stockList;
    }


    private List<RecCheckTemp> getCheckTempById(Object id) throws Exception {
        QuerySchema querySchema = QuerySchema.create().addSelect("*");
        QueryConditionGroup queryConditionGroup = new QueryConditionGroup(ConditionOperator.and);
        queryConditionGroup.appendCondition(QueryCondition.name("mainid").eq(id));
        querySchema.addCondition(queryConditionGroup);
        return MetaDaoHelper.queryObject(RecCheckTemp.ENTITY_NAME, querySchema, null);
    }

    private String getCheckBillNoByNum(CheckStockApply apply, int i) {
        String startNo = apply.getStartNo();
        String[] split = startNo.replaceAll("\\D", ",").split(",");
        String end = split[split.length - 1];
        BigInteger num = new BigInteger(end);
        BigInteger iBig = new BigInteger(String.valueOf(i));
        String val = String.valueOf(num.add(iBig));
        if (startNo.length() >= val.length()) {
            String begin = startNo.substring(0, startNo.indexOf(String.valueOf(num)));
            return begin + val;
        } else {
            return val;
        }
    }

    private void checkCheckStocks(List<CheckStock> checkStocks) throws Exception {
        List<String> checkBillNoList = new ArrayList<>();
        checkStocks.forEach(stock -> checkBillNoList.add(stock.getCheckBillNo()));
        HashSet<String> hashSet = new HashSet<>(checkBillNoList);
        if (checkBillNoList.size() != hashSet.size()) {
            //存在相同的支票编号！
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101111"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418050E","审核失败：存在相同的支票编号！") /* "审核失败：存在相同的支票编号！" */);
        }
        List<CheckStock> checkNos = getCheckNos();
        for (CheckStock s:checkNos) {
            for(String no:checkBillNoList){
                if (s.getCheckBillNo().equals(no)){
                    //存在相同的支票编号！
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101111"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418050E","审核失败：存在相同的支票编号！") /* "审核失败：存在相同的支票编号！" */);
                }
            }
        }
    }

    private List<CheckStock> getCheckNos() throws Exception {
        QuerySchema querySchema = QuerySchema.create().addSelect("*");
        QueryConditionGroup queryConditionGroup = new QueryConditionGroup(ConditionOperator.and);
        queryConditionGroup.appendCondition(QueryCondition.name("checkBillStatus").in((Object) "1,2,3,4,5,6,7".split(",")));
        querySchema.addCondition(queryConditionGroup);
        return MetaDaoHelper.queryObject(CheckStock.ENTITY_NAME, querySchema, null);
    }

    private List<CheckStockApply> getUpdateApplyList(List<CheckStockApply> checkStorkApplyRows, AuditStatus status) {
        if (null == checkStorkApplyRows || checkStorkApplyRows.size() < 0) {
            return null;
        }
        List<CheckStockApply> updateList = new ArrayList<>();
        for (CheckStockApply apply : checkStorkApplyRows) {
            CheckStockApply update = new CheckStockApply();
            update.setEntityStatus(EntityStatus.Update);
            update.setId(apply.getId());
            update.setAuditstatus(status.getValue());
            updateList.add(update);
        }
        return updateList;
    }

}
