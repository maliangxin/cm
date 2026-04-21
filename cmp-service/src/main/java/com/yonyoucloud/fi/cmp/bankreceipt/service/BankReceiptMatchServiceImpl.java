package com.yonyoucloud.fi.cmp.bankreceipt.service;

import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.iuap.yms.lock.YmsLock;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.dao.sql.SqlHelper;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.bankelectronicreceipt.BankElectronicReceipt;
import com.yonyoucloud.fi.cmp.cmpentity.EventType;
import com.yonyoucloud.fi.cmp.constant.IBussinessConstant;
import com.yonyoucloud.fi.cmp.journal.Journal;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.Direction;
import com.yonyoucloud.fi.cmp.util.BigDecimalUtils;
import com.yonyoucloud.fi.cmp.util.JedisLockUtils;
import com.yonyoucloud.fi.cmp.util.StringUtils;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import lombok.extern.slf4j.Slf4j;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.biz.base.Objectlizer;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.base.Json;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Service
@Transactional
@Slf4j
public class BankReceiptMatchServiceImpl implements BankCheckMatchService {
    private static final String DETAILPREX = "com.yonyoucloud.fi.cmp.mapper.UpdateCheckFlagMapper";
    private static  String JOURNAL = "journal";
    private static  String RECEIPT = "receipt";
    private static  String EQUALSACCOUNT="equalsaccount";

    //自动匹配
    @Override
    public CtmJSONObject automatch(CtmJSONObject params) throws Exception {
            CtmJSONObject reback = new CtmJSONObject();
            String key  = params.getString(IBussinessConstant.ACCENTITY);
            Boolean equalsaccount = params.getBoolean(EQUALSACCOUNT);
            Json json = new Json(params.getJSONArray(JOURNAL).toString());
            List<Journal> journalList = Objectlizer.decode(json, Journal.ENTITY_NAME);
            json = new Json(params.getJSONArray(RECEIPT).toString());
            List<BankElectronicReceipt> receiptList = Objectlizer.decode(json, BankElectronicReceipt.ENTITY_NAME);
            if((journalList == null || journalList.size() == 0)&&(receiptList == null || receiptList.size() == 0)){
                return amountMatch(params);
            }
            if (journalList == null || journalList.size() == 0){
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101650"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807B4","银行日记账未勾选数据") /* "银行日记账未勾选数据" */);
            }
            if (receiptList == null || receiptList.size() == 0){
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101651"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807B5","银行电子回单未勾选数据") /* "银行电子回单未勾选数据" */);
            }
            Integer  journalSize = journalList.size();
            Integer  receiptSize = receiptList.size();
            List<Journal> journals = new ArrayList<>();
            List<BankElectronicReceipt> receipts = new ArrayList<>();
            YmsLock ymsLock = null;
            try {
                //加锁
                ymsLock = JedisLockUtils.lockGlWithOutTrace(key);
                if (ymsLock == null) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101652"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800D0","该组织下正在匹配关联，请稍后再试！") /* "该组织下正在匹配关联，请稍后再试！" */);
                }
                for (Journal journal : journalList) {
                    if (journal.getCheckmatch()) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101653"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807AD","已经匹配过的的不能再次关联") /* "已经匹配过的的不能再次关联" */);
                    }
                    Direction direction = journal.getDirection();
                    BigDecimal jourAmount = BigDecimal.ZERO;
                    if (Direction.Debit.equals(direction)) {
                        jourAmount = journal.getDebitoriSum();
                    } else {
                        jourAmount = journal.getCreditoriSum();
                    }
                    Iterator<BankElectronicReceipt> iterator = receiptList.iterator();
                    while (iterator.hasNext()) {
                        BankElectronicReceipt receipt = iterator.next();
                        if (receipt.getCheckmatch()) {
                            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101653"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807AD","已经匹配过的的不能再次关联") /* "已经匹配过的的不能再次关联" */);
                        }
                        BigDecimal receiptAmount = receipt.getTran_amt();
                        Direction receiptDirection = receipt.getDc_flag();
                        if (jourAmount.compareTo(receiptAmount) != 0) {
                            continue;
                        }
                        if (direction.equals(receiptDirection)) {
                            continue;
                        }
                        /*
                         *  判断电子回单借贷关系  贷->客户 借->供应商*/
                        if (equalsaccount) {
                            if (Direction.Debit.equals(receiptDirection)) {
                                if (!journal.getSupplierbankaccount().equals(receipt.getTo_acct_no())) {
                                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101654"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807B7","对方账户不匹配") /* "对方账户不匹配" */);
                                }
                            } else {
                                if (!journal.getCustomerbankaccount().equals(receipt.getTo_acct_no())) {
                                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101654"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807B7","对方账户不匹配") /* "对方账户不匹配" */);
                                }
                            }
                        }
                        String checkmatch = UUID.randomUUID().toString();
                        journal.setCheckmatch(true);
                        journal.setRelevanceid(checkmatch);
                        journal.setEntityStatus(EntityStatus.Update);
                        receipt.setCheckmatch(true);
                        receipt.setRelevanceid(checkmatch);
                        receipt.setEntityStatus(EntityStatus.Update);
                        receipts.add(receipt);
                        iterator.remove();
                        journals.add(journal);
                        break;
                    }
                }
                if (journals.size() == 0) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101655"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807AE","没有可以匹配关联的数据") /* "没有可以匹配关联的数据" */);
                }
                MetaDaoHelper.update(Journal.ENTITY_NAME, journals);
                MetaDaoHelper.update(BankElectronicReceipt.ENTITY_NAME, receipts);
                reback.put("success", true);
                StringBuffer message = new StringBuffer(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807B0","业务单据已勾对笔数:") /* "业务单据已勾对笔数:" */ + journals.size() + "\r\n");
                message.append(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807B1","业务单据未勾对笔数:") /* "业务单据未勾对笔数:" */ + (journalSize - journals.size()) + "\r\n");
                message.append(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807A7","电子回单关联条数") /* "电子回单关联条数" */ + receipts.size() + "\r\n");
                message.append(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807A8","电子回单未关联条数") /* "电子回单未关联条数" */ + (receiptSize - receipts.size()) + "\r\n");
                reback.put("message", message);
            } catch (Exception e) {
                log.error("自动匹配失败--" +e.getMessage(),e);
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101656"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807AB","自动匹配失败") /* "自动匹配失败" */ + "---" + e.getMessage());
            }finally {
                //释放组织锁
                JedisLockUtils.unlockGlWithOutTrace(ymsLock);
            }
            return reback;
    }
    //手动匹配
    @Override
    public CtmJSONObject manualmatch(CtmJSONObject params)  {
            String key  = params.getString(IBussinessConstant.ACCENTITY);
            CtmJSONObject reback = new CtmJSONObject();
            Json json = new Json(params.getJSONArray(JOURNAL).toString());
            List<Journal> journalList = Objectlizer.decode(json, Journal.ENTITY_NAME);
            json = new Json(params.getJSONArray(RECEIPT).toString());
            List<BankElectronicReceipt> receiptList = Objectlizer.decode(json, BankElectronicReceipt.ENTITY_NAME);
            if((journalList == null || journalList.size() == 0)&&(receiptList == null || receiptList.size() == 0)){
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101657"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807B3","请选择关联数据") /* "请选择关联数据" */);
            }
            if (journalList == null || journalList.size() == 0){
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101650"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807B4","银行日记账未勾选数据") /* "银行日记账未勾选数据" */);
            }
            if (receiptList == null || receiptList.size() == 0){
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101651"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807B5","银行电子回单未勾选数据") /* "银行电子回单未勾选数据" */);
            }
            //一对一
            if(journalList.size()==1 && receiptList.size()==1){
                oneToOne(journalList,receiptList,reback,key);
            }else if(journalList.size()>1 && receiptList.size()==1){//多对一
                manyToOne(journalList,receiptList,reback, key);
            }else{
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101658"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807B8","选中数据暂不支持匹配关联，请重新选择") /* "选中数据暂不支持匹配关联，请重新选择" */);
            }
            return reback;
    }
    //取消匹配
    @Override
    public CtmJSONObject cancelmatch(CtmJSONObject params) {
        CtmJSONObject reback = new CtmJSONObject();
        Json json = new Json(params.getJSONArray(JOURNAL).toString());
        List<Journal> journalList = Objectlizer.decode(json, Journal.ENTITY_NAME);
        json = new Json(params.getJSONArray(RECEIPT).toString());
        List<BankElectronicReceipt> receiptList = Objectlizer.decode(json, BankElectronicReceipt.ENTITY_NAME);
        if ((journalList == null || journalList.size() == 0) && (receiptList == null || receiptList.size() == 0)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101659"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807BA","未选中任何数据") /* "未选中任何数据" */);
        }
        List<String> relevanceids = new ArrayList<>();
        if (journalList != null && journalList.size() > 0) {
            for (Journal journal : journalList) {
                if (!journal.getCheckmatch()) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101660"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807A9","未匹配关联的不需要取消匹配") /* "未匹配关联的不需要取消匹配" */);
                }
                if (!relevanceids.contains(journal.getRelevanceid())) {
                    relevanceids.add(journal.getRelevanceid());
                }
            }
        }
        if (receiptList != null && receiptList.size() > 0) {
            for (BankElectronicReceipt receipt : receiptList) {
                if (!receipt.getCheckmatch()) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101660"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807A9","未匹配关联的不需要取消匹配") /* "未匹配关联的不需要取消匹配" */);
                }
                if (!relevanceids.contains(receipt.getRelevanceid())) {
                    relevanceids.add(receipt.getRelevanceid());
                }
            }
        }
        if (ValueUtils.isEmpty(relevanceids)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101661"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807AF","取消匹配失败") /* "取消匹配失败" */);
        }
        try {
            Map<String, Object> param = new HashMap<>();
            param.put("relevanceids", relevanceids);
            param.put("ytenant_id", InvocationInfoProxy.getTenantid());
            SqlHelper.update(DETAILPREX + ".updateJournalcheckmatch", param);
            SqlHelper.update(DETAILPREX + ".updateBankElectronicReceiptcheckmatch", param);
            reback.put("success", true);
            reback.put("message", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807B2","取消匹配成功") /* "取消匹配成功" */);
        } catch (Exception e) {
            log.error("取消对账失败" + e.getMessage(), e);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101661"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807AF","取消匹配失败") /* "取消匹配失败" */ + "---" + e.getMessage());
        }
        return reback;
    }
    //一对一
    private void oneToOne(List<Journal>journalList , List<BankElectronicReceipt>receiptList,CtmJSONObject reback ,String key){
        Journal journal = journalList.get(0);
        BankElectronicReceipt receipt = receiptList.get(0);
        YmsLock ymsLock = null;
        try {
            //加锁
            ymsLock = JedisLockUtils.lockGlWithOutTrace(key);
            if (ymsLock == null) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101652"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800D0","该组织下正在匹配关联，请稍后再试！") /* "该组织下正在匹配关联，请稍后再试！" */);
            }
            if (journal.getCheckmatch()) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101653"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807AD","已经匹配过的的不能再次关联") /* "已经匹配过的的不能再次关联" */);
            }
            if (receipt.getCheckmatch()) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101653"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807AD","已经匹配过的的不能再次关联") /* "已经匹配过的的不能再次关联" */);
            }
            Direction direction = journal.getDirection();
            BigDecimal jourAmount = BigDecimal.ZERO;
            if (Direction.Debit.equals(direction)) {
                jourAmount = journal.getDebitoriSum();
            } else {
                jourAmount = journal.getCreditoriSum();
            }
            BigDecimal receiptAmount = receipt.getTran_amt();
            Direction receiptDirection = receipt.getDc_flag();
            if (jourAmount.compareTo(receiptAmount) != 0) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101662"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807B9","金额不相等，请重新选择") /* "金额不相等，请重新选择" */);
            }
            if (direction.equals(receiptDirection)) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101663"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807B6","借贷方不能相同，请重新选择") /* "借贷方不能相同，请重新选择" */);
            }
            String checkmatch = UUID.randomUUID().toString();
            journal.setCheckmatch(true);
            journal.setRelevanceid(checkmatch);
            journal.setEntityStatus(EntityStatus.Update);
            receipt.setEntityStatus(EntityStatus.Update);
            receipt.setCheckmatch(true);
            receipt.setRelevanceid(checkmatch);
            MetaDaoHelper.update(Journal.ENTITY_NAME, journal);
            MetaDaoHelper.update(BankElectronicReceipt.ENTITY_NAME, receipt);
            reback.put("success", true);
            reback.put("message", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807AA","匹配成功") /* "匹配成功" */);
        } catch (Exception e) {
            log.error("匹配关联失败--" + e.getMessage(),e);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101664"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B000A2", "匹配关联失败--") /* "匹配关联失败--" */ + e.getMessage());
        }finally {
            //释放组织锁
            JedisLockUtils.unlockGlWithOutTrace(ymsLock);
        }
    }
    //多对一
    private void manyToOne(List<Journal> journalList, List<BankElectronicReceipt> receiptList, CtmJSONObject reback ,String key) {
        YmsLock ymsLock = null;
        try{
            //加锁
            ymsLock = JedisLockUtils.lockGlWithOutTrace(key);
            if (ymsLock == null) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101652"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800D0","该组织下正在匹配关联，请稍后再试！") /* "该组织下正在匹配关联，请稍后再试！" */);
            }
            BankElectronicReceipt receipt = receiptList.get(0);
            if (receipt.getCheckmatch()) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101653"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807AD","已经匹配过的的不能再次关联") /* "已经匹配过的的不能再次关联" */);
            }
            BigDecimal journalSum = BigDecimal.ZERO;
            for (Journal journal : journalList) {
                if (journal.getCheckmatch()) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101653"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807AD","已经匹配过的的不能再次关联") /* "已经匹配过的的不能再次关联" */);
                }
                Direction direction = journal.getDirection();
                if (direction.equals(receipt.getDc_flag())) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101663"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807B6","借贷方不能相同，请重新选择") /* "借贷方不能相同，请重新选择" */);
                }
                BigDecimal jourAmount = BigDecimal.ZERO;
                if (Direction.Debit.equals(direction)){
                    jourAmount = journal.getDebitoriSum();
                }else{
                    jourAmount = journal.getCreditoriSum();
                }
                journalSum = BigDecimalUtils.safeAdd(journalSum, jourAmount);
            }
            if (journalSum.compareTo(receipt.getTran_amt()) != 0) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101662"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807B9","金额不相等，请重新选择") /* "金额不相等，请重新选择" */);
            }
            String checkmatch = UUID.randomUUID().toString();
            receipt.setCheckmatch(true);
            receipt.setRelevanceid(checkmatch);
            receipt.setEntityStatus(EntityStatus.Update);
                //验证过后 进行赋值
            List<Journal> journals = new ArrayList<>();
            for(Journal jo:journalList){
                jo.setCheckmatch(true);
                jo.setRelevanceid(checkmatch);
                jo.setEntityStatus(EntityStatus.Update);
                journals.add(jo);
            }
            MetaDaoHelper.update(Journal.ENTITY_NAME, journals);
            MetaDaoHelper.update(BankElectronicReceipt.ENTITY_NAME, receipt);
            reback.put("success", true);
            reback.put("message", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807AA","匹配成功") /* "匹配成功" */);
        } catch (Exception e) {
            log.error("匹配关联失败--" + e.getMessage(), e);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101665"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807AC","匹配关联失败--") /* "匹配关联失败--" */ + e.getMessage());
        } finally {
            //释放组织锁
            JedisLockUtils.unlockGlWithOutTrace(ymsLock);
        }
    }

    /**
     * 全局匹配
     * @param params  : accentity  filterCondition[] equalsaccount
     * @return
     */
    public CtmJSONObject amountMatch(CtmJSONObject params){
        CtmJSONObject reback = new CtmJSONObject();
        String key  = params.getString(IBussinessConstant.ACCENTITY);
        Boolean equalsaccount = params.getBoolean(EQUALSACCOUNT);
        List<Journal> journalList=new ArrayList<>();
        List<BankElectronicReceipt> receiptList=new ArrayList<>();
        Map<String,Object> filterCondition = (Map<String, Object>) params.get("filterCondition");
        YmsLock ymsLock = null;
        try {
            //日记账
            QuerySchema queryJournal = QuerySchema.create().addSelect("*");
            queryJournal.appendQueryCondition(QueryCondition.name(IBussinessConstant.ACCENTITY).eq(filterCondition.get(IBussinessConstant.ACCENTITY)));
            queryJournal.appendQueryCondition(QueryCondition.name("bankaccount").eq(filterCondition.get("bankaccount")));
            queryJournal.appendQueryCondition(QueryCondition.name("checkmatch").eq(0));
            if(!StringUtils.isEmpty((String) filterCondition.get("startdzdate"))){
                queryJournal.appendQueryCondition(QueryCondition.name("dzdate").egt(filterCondition.get("startdzdate")));
            }
            if(!StringUtils.isEmpty((String) filterCondition.get("enddzdate"))){
                queryJournal.appendQueryCondition(QueryCondition.name("dzdate").elt(filterCondition.get("enddzdate")));
            }
            List<Short> billtype = new ArrayList<>();
            billtype.add(EventType.ReceiveBill.getValue());
            billtype.add(EventType.PayMent.getValue());
            billtype.add(EventType.TransferAccount.getValue());
            billtype.add(EventType.SalaryPayment.getValue());
            queryJournal.appendQueryCondition(QueryCondition.name("billtype").in(billtype));
            List<Map<String,Object>> queryjour=MetaDaoHelper.query(Journal.ENTITY_NAME,queryJournal) ;
            for(Map<String,Object> map:queryjour){
                Journal journal = new Journal();
                Journal journal1 = CtmJSONObject.parseObject(CtmJSONObject.toJSONString(map), Journal.class);
                journal.setId(map.get("id"));
                journal.setDirection(journal1.getDirection());
                journal.setDebitoriSum(journal1.getDebitoriSum());
                journal.setCreditoriSum(journal1.getCreditoriSum());
                journal.setCheckmatch(journal1.getCheckmatch());
                journal.setSupplierbankaccount(journal1.getSupplierbankaccount());
                journal.setCustomerbankaccount(journal1.getCustomerbankaccount());
                journalList.add(journal);
            }
            //电子回单
            QuerySchema queryBankElectronicReceipt = QuerySchema.create().addSelect("*");
            queryBankElectronicReceipt.appendQueryCondition(QueryCondition.name(IBussinessConstant.ACCENTITY).eq(filterCondition.get(IBussinessConstant.ACCENTITY)));
            queryBankElectronicReceipt.appendQueryCondition(QueryCondition.name("enterpriseBankAccount").eq(filterCondition.get("bankaccount")));
            queryBankElectronicReceipt.appendQueryCondition(QueryCondition.name("checkmatch").eq(0));
            if(!StringUtils.isEmpty((String) filterCondition.get("startdzdate"))){
                queryBankElectronicReceipt.appendQueryCondition(QueryCondition.name("trandate").egt(filterCondition.get("startdzdate")));
            }
            if(!StringUtils.isEmpty((String) filterCondition.get("enddzdate"))){
                queryBankElectronicReceipt.appendQueryCondition(QueryCondition.name("trandate").elt(filterCondition.get("enddzdate")));
            }
            List<Map<String,Object>> querybnakrecpt = MetaDaoHelper.query(BankElectronicReceipt.ENTITY_NAME,queryBankElectronicReceipt,null);
            for(Map<String,Object> map:querybnakrecpt){
                BankElectronicReceipt bankElectronicReceipt = new BankElectronicReceipt();
                BankElectronicReceipt bankElectronicReceipt1 = CtmJSONObject.parseObject(CtmJSONObject.toJSONString(map), BankElectronicReceipt.class);
                bankElectronicReceipt.setId(map.get("id"));
                bankElectronicReceipt.setCheckmatch(bankElectronicReceipt1.getCheckmatch());
                bankElectronicReceipt.setTran_amt(bankElectronicReceipt1.getTran_amt());
                bankElectronicReceipt.setDc_flag(bankElectronicReceipt1.getDc_flag());
                bankElectronicReceipt.setTo_acct_no(bankElectronicReceipt1.getTo_acct_no());
                receiptList.add(bankElectronicReceipt);
            }
            Integer  journalSize = journalList.size();
            Integer  receiptSize = receiptList.size();
            List<Journal> journals = new ArrayList<>();
            List<BankElectronicReceipt> receipts = new ArrayList<>();
            //加锁
            ymsLock = JedisLockUtils.lockGlWithOutTrace(key);
            if (ymsLock == null) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101652"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800D0","该组织下正在匹配关联，请稍后再试！") /* "该组织下正在匹配关联，请稍后再试！" */);
            }
            for (Journal journal : journalList) {
                if (journal.getCheckmatch()) {
                    continue;
                }
                Direction direction = journal.getDirection();
                BigDecimal jourAmount = BigDecimal.ZERO;
                if (Direction.Debit.equals(direction)) {
                    jourAmount = journal.getDebitoriSum();
                } else {
                    jourAmount = journal.getCreditoriSum();
                }
                Iterator<BankElectronicReceipt> iterator = receiptList.iterator();
                while (iterator.hasNext()) {
                    BankElectronicReceipt receipt = iterator.next();
                    if (receipt.getCheckmatch()) {
                        continue;
                    }
                    BigDecimal receiptAmount = receipt.getTran_amt();
                    Direction receiptDirection = receipt.getDc_flag();
                    if (jourAmount.compareTo(receiptAmount) != 0) {
                        continue;
                    }
                    if (direction.equals(receiptDirection)) {
                        continue;
                    }
                    /*
                     *  判断电子回单借贷关系  贷->客户 借->供应商*/
                    if (equalsaccount) {
                        if (Direction.Debit.equals(receiptDirection)) {
                            if(journal.getSupplierbankaccount() == null ){
                                continue;
                            }
                            if (!journal.getSupplierbankaccount().equals(receipt.getTo_acct_no())) {
                                  continue;
                            }
                        } else {
                            if(journal.getCustomerbankaccount() == null ){
                                continue;
                            }
                            if (!journal.getCustomerbankaccount().equals(receipt.getTo_acct_no())) {
                                  continue;
                            }
                        }
                    }
                    String checkmatch = UUID.randomUUID().toString();
                    journal.setCheckmatch(true);
                    journal.setRelevanceid(checkmatch);
                    journal.setEntityStatus(EntityStatus.Update);
                    receipt.setCheckmatch(true);
                    receipt.setRelevanceid(checkmatch);
                    receipt.setEntityStatus(EntityStatus.Update);
                    receipts.add(receipt);
                    iterator.remove();
                    journals.add(journal);
                    break;
                }
            }
            if (journals.size() == 0) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101655"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807AE","没有可以匹配关联的数据") /* "没有可以匹配关联的数据" */);
            }
            MetaDaoHelper.update(Journal.ENTITY_NAME, journals);
            MetaDaoHelper.update(BankElectronicReceipt.ENTITY_NAME, receipts);
            reback.put("success", true);
            StringBuffer message = new StringBuffer(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807B0","业务单据已勾对笔数:") /* "业务单据已勾对笔数:" */ + journals.size() + "\r\n");
            message.append(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807B1","业务单据未勾对笔数:") /* "业务单据未勾对笔数:" */ + (journalSize - journals.size()) + "\r\n");
            message.append(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807A7","电子回单关联条数") /* "电子回单关联条数" */ + receipts.size() + "\r\n");
            message.append(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807A8","电子回单未关联条数") /* "电子回单未关联条数" */ + (receiptSize - receipts.size()) + "\r\n");
            reback.put("message", message);
        } catch (Exception e) {
            log.error("自动匹配失败--" + e.getMessage(), e);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101656"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807AB","自动匹配失败") /* "自动匹配失败" */ + "---" + e.getMessage());
        } finally {
            //释放组织锁
            JedisLockUtils.unlockGlWithOutTrace(ymsLock);
        }
        return reback;
    }

}
