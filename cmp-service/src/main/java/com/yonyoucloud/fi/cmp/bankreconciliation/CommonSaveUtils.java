package com.yonyoucloud.fi.cmp.bankreconciliation;

import cn.hutool.core.util.NumberUtil;
import com.alibaba.fastjson.JSON;
import com.esotericsoftware.minlog.Log;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.dao.sql.SqlHelper;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.logger.business.CTMCMPBusinessLogService;
import com.yonyou.yonbip.ctm.remote.RemoteDubbo;
import com.yonyoucloud.ctm.stwb.reconcode.pubitf.ReconciliateCodeGenerator;
import com.yonyoucloud.fi.cmp.bankreconciliation.utils.CommonBankReconciliationProcessor;
import com.yonyoucloud.fi.cmp.billclaim.BillClaim;
import com.yonyoucloud.fi.cmp.billclaim.BillClaimItem;
import com.yonyoucloud.fi.cmp.cmpentity.AssociationStatus;
import com.yonyoucloud.fi.cmp.cmpentity.EntryType;
import com.yonyoucloud.fi.cmp.cmpentity.OppositeType;
import com.yonyoucloud.fi.cmp.cmpentity.ReconciliationDataSource;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.common.service.CtmCmpCheckRepeatDataService;
import com.yonyoucloud.fi.cmp.constant.IDomainConstant;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import com.yonyoucloud.fi.cmp.enums.SerialdealendState;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.consts.BankDealDetailConst;
import com.yonyoucloud.fi.cmp.util.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.core.base.ConditionOperator;
import org.imeta.core.lang.BooleanUtils;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author: liaojbo
 * @Date: 2024年08月13日 14:12
 * @Description:保存银行对账单、到账认领前，把对方单位字段拆分到对应的类型里，以支持数据权限； 不做数据是否存在对应档案的校验，进入这个方法前应该做过；
 * 改为统一入口，因此需要兼容原来的各种入参;
 * 到账认领入参只支持主表，目前没有单独更新子表的情况
 */

@Service
@Slf4j
public class CommonSaveUtils {

    public static void saveBillClaim(BillClaim billClaim) throws Exception {
        if (billClaim == null) {
            return;
        }
        List<BillClaim> billClaimList = new ArrayList<>();
        billClaimList.add(billClaim);
        saveBillClaim(billClaimList);
    }

    public static void saveBillClaim(List<BillClaim> billClaimList) throws Exception {
        if (billClaimList == null) {
            return;
        }
        for (BillClaim billClaim :
                billClaimList) {
            List<BillClaimItem> billClaimItemList = billClaim.items();
            if (billClaimItemList != null) {
                for (BillClaimItem billClaimItem :
                        billClaimItemList) {
                    Short oppositetype = billClaimItem.getOppositetype();
                    Long oppositeobjectid = NumberUtil.parseLong(billClaimItem.getOppositeobjectid(), null);
                    if (oppositetype != null) {
                        setOppositeobjectidToBillClaimItemField(billClaimItem, oppositetype, oppositeobjectid);
                    }
                }
            }
        }
        CmpMetaDaoHelper.insert(BillClaim.ENTITY_NAME, billClaimList);
    }


    public static void updateBillClaim(BillClaim billClaim) throws Exception {
        if (billClaim == null) {
            return;
        }
        List<BillClaim> billClaimList = new ArrayList<>();
        billClaimList.add(billClaim);
        updateBillClaim(billClaimList);
    }

    public static void updateBillClaim(List<BillClaim> billClaimList) throws Exception {
        if (billClaimList == null) {
            return;
        }
        for (BillClaim billClaim :
                billClaimList) {
            List<BillClaimItem> billClaimItemList = billClaim.items();
            if (billClaimItemList != null) {
                for (BillClaimItem billClaimItem :
                        billClaimItemList) {
                    Short oppositetype = billClaimItem.getOppositetype();
                    Long oppositeobjectid = NumberUtil.parseLong(billClaimItem.getOppositeobjectid(), null);
                    if (oppositetype != null) {
                        setOppositeobjectidToBillClaimItemField(billClaimItem, oppositetype, oppositeobjectid);
                    }
                }
            }
        }
        MetaDaoHelper.update(BillClaim.ENTITY_NAME, billClaimList);
    }

    //兼容BizObject的入参
    public static void updateBillClaim(BizObject billClaimBizObject) throws Exception {
        if (billClaimBizObject == null) {
            return;
        }
        List<BizObject> billClaimItemBizList = billClaimBizObject.get(BillClaim.ITEMS_KEY);
        if (billClaimItemBizList != null) {
            for (BizObject billClaimItemBiz :
                    billClaimItemBizList) {
                Short oppositetype = billClaimItemBiz.getShort("oppositetype");
                //空或解析不出来时，给null
                String oppositeobjectid = billClaimItemBiz.get("oppositeobjectid");
                if (oppositetype != null) {
                    setOppositeobjectidToBizField(billClaimItemBiz, oppositetype, oppositeobjectid);
                }
            }
        }
        MetaDaoHelper.update(BillClaim.ENTITY_NAME, billClaimBizObject);
    }


    public static void saveBankReconciliation(BankReconciliation bankReconciliation) throws Exception {
        if (bankReconciliation == null) {
            return;
        }
        List<BankReconciliation> bankReconciliationList = new ArrayList<>();
        bankReconciliationList.add(bankReconciliation);
        saveBankReconciliation(bankReconciliationList);
    }

    public static void saveBankReconciliation(List<BankReconciliation> bankReconciliationList) throws Exception {
        if (bankReconciliationList == null) {
            return;
        }
        for (BankReconciliation bankReconciliation :
                bankReconciliationList) {
            Short oppositetype = bankReconciliation.getOppositetype();
            Long oppositeobjectid = NumberUtil.parseLong(bankReconciliation.getOppositeobjectid(), null);
            if (oppositetype != null) {
                setOppositeobjectidToBankReconciliationField(bankReconciliation, oppositetype, oppositeobjectid);
            }
            //疑重判断 如果已经有了isrepeat字段且有值时，不再进行疑重判定，否则需要进行疑重判定
            if (!bankReconciliation.containsKey("isrepeat") || bankReconciliation.getIsRepeat() == null) {
                //疑重判断
                bankReconciliation.setIsRepeat((short) BankDealDetailConst.REPEAT_INIT);
                Map<String, Object> map = new HashMap<>();
                map.put("startDate", DateUtils.convertToStr(bankReconciliation.getTran_date(), "yyyy-MM-dd"));
                CtmCmpCheckRepeatDataService checkRepeatDataService = AppContext.getBean(CtmCmpCheckRepeatDataService.class);
                checkRepeatDataService.deal4FactorsBankDealDetail(Collections.singletonList(bankReconciliation), map);
            }
        }
        // 批量插入流水数据
        bankInsertDuplicateKey(bankReconciliationList);
    }

    // 在流水存储中触发唯一索引异常场景，改为单条处理，仅进行一次
    private static void bankInsertDuplicateKey(List<BankReconciliation> bankReconciliationList) {
        // 正常插入数据的集合
        List<BankReconciliation> normalBankReconciliationList = new ArrayList<>();
        // concat_info重复的集合
        List<BankReconciliation> concatInfoRepeatBankReconciliationList = new ArrayList<>();
        CtmCmpCheckRepeatDataService checkRepeatDataService = AppContext.getBean(CtmCmpCheckRepeatDataService.class);
        // 改为单条循环处理
        for (BankReconciliation bankReconciliation : bankReconciliationList) {
            try {
                // 缓存字段值，避免重复调用
                String concatInfo = bankReconciliation.getConcat_info();
                String uniqueNo = bankReconciliation.getUnique_no();
                // 检查 concat_info 是否为空，并设置8要素字段
                if (StringUtils.isEmpty(concatInfo)) {
                    checkRepeatDataService.fillBankReconciliationConcatInfo(bankReconciliation);
                    concatInfo = bankReconciliation.getConcat_info();
                }
                // 检查 unique_no 是否为空，为""时，为了unique_no支持唯一索引，设置为null
                if (StringUtils.isEmpty(uniqueNo)) {
                    uniqueNo = null;
                    bankReconciliation.setUnique_no(uniqueNo);
                }
                // 获取重复键信息
                String duplicateKey = duplicateKeyForSaveReconciliations(bankReconciliation);
                // 正常保存成功
                if (StringUtils.isEmpty(duplicateKey)) {
                    normalBankReconciliationList.add(bankReconciliation);
                    continue;
                }
                // 其他错误，跳过当前处理
                if (duplicateKey.equals("other")) {
                    continue;
                }
                // 处理unique_no冲突
                if (StringUtils.isNotEmpty(uniqueNo) && uniqueNo.startsWith(duplicateKey)) {
                    log.error("bankInsertDuplicateKey+ unique_no:{}重复,concat_info:{}", bankReconciliation.getUnique_no(), bankReconciliation.getConcatInfo());
                    continue;
                }
                // 处理concat_info冲突
                if (concatInfo.startsWith(duplicateKey)) {
                    updateConcatInfoAndInsertBankReconciliation(bankReconciliation);
                    concatInfoRepeatBankReconciliationList.add(bankReconciliation);
                    // concat_info冲突的数据更新后作为需要保存的数据记录
                    normalBankReconciliationList.add(bankReconciliation);
                }
            } catch (Exception e) {
                // 记录异常日志，避免单条数据错误影响整体流程
                log.error("bankInsertDuplicateKey+ unique_no:{},concat_info:{}，错误:{}", bankReconciliation.getUnique_no(), bankReconciliation.getConcatInfo(), e.getMessage());
            }
        }
        // 替换原始列表内容
        bankReconciliationList.clear();
        bankReconciliationList.addAll(normalBankReconciliationList);
    }

    /**
     * 更新concat_info和疑重字段，并进行单条保存
     */
    private static void updateConcatInfoAndInsertBankReconciliation(BankReconciliation bankReconciliation) throws Exception {
        try {
            bankReconciliation.setConcat_info(bankReconciliation.getConcatInfo().concat("|@" + DateUtils.getLongStringAllDate() + RemoteDubbo.get(ReconciliateCodeGenerator.class, IDomainConstant.MDD_DOMAIN_STWB).generate()));
            bankReconciliation.setIsRepeat((short) BankDealDetailConst.REPEAT_DOUBT);
            CmpMetaDaoHelper.insert(BankReconciliation.ENTITY_NAME, bankReconciliation);
        } catch (Exception e) {
            log.error("updateConcatInfoAndInsertBankReconciliation+ unique_no:{},concat_info:{},错误:{}", bankReconciliation.getUnique_no(), bankReconciliation.getConcatInfo(), e.getMessage());
        }
    }

    /**
     * 保存数据后如果是重复索引，返回重复索引内容
     */
    private static String duplicateKeyForSaveReconciliations(BankReconciliation bankReconciliation) {
        String duplicateKey = "";
        try {
            CmpMetaDaoHelper.insert(BankReconciliation.ENTITY_NAME, bankReconciliation);
        } catch (Exception e) {
            // 非唯一索引的数据库异常跳过此次处理
            if (!e.getMessage().contains("Duplicate")) {
                log.error("bankInsertDuplicateKey+ unique_no:{},concat_info:{},错误:{}", bankReconciliation.getUnique_no(), bankReconciliation.getConcatInfo(), e.getMessage());
                duplicateKey = "other";
                return duplicateKey;
            }
            // 解析异常内容，获取唯一索引数据
            String[] idxDuplicateKeyArray = e.getMessage().split("'");
            if (idxDuplicateKeyArray.length > 1) {
                duplicateKey = idxDuplicateKeyArray[1];
                return duplicateKey;
            }
        }
        return duplicateKey;
    }

    //兼容BizObject的入参
    public static void saveBankReconciliation(BizObject bankReconciliationBizObject) throws Exception {
        if (bankReconciliationBizObject != null) {
            Short oppositetype = bankReconciliationBizObject.getShort("oppositetype");
            String oppositeobjectId = bankReconciliationBizObject.get("oppositeobjectid");
            if (oppositetype != null) {
                setOppositeobjectidToBizField(bankReconciliationBizObject, oppositetype, oppositeobjectId);
            }
            CmpMetaDaoHelper.insert(BillClaim.ENTITY_NAME, bankReconciliationBizObject);
        }
    }

    public static void updateBankReconciliation(BankReconciliation bankReconciliation) throws Exception {
        if (bankReconciliation == null) {
            return;
        }
        List<BankReconciliation> bankReconciliationList = new ArrayList<>();
        bankReconciliationList.add(bankReconciliation);
        updateBankReconciliation(bankReconciliationList);
    }

    /**
     * @param bankReconciliation
     * @param confirmOrReject    确认或者拒绝：1是确认2是拒绝
     * @throws Exception
     */
    public static void updateBankReconciliationConfirmOrReject(BankReconciliation bankReconciliation, String confirmOrReject) throws Exception {
        if (bankReconciliation == null) {
            return;
        }
        List<BankReconciliation> bankReconciliationList = new ArrayList<>();
        bankReconciliationList.add(bankReconciliation);
        updateBankReconciliationConfirmOrReject(bankReconciliationList, confirmOrReject);
    }

    /**
     * 兼容泛型和BizObject、BankReconciliation的入参；写多个方法的话，类型擦除后，编译不通过
     */
    public static <T extends BizObject> void updateBankReconciliation(List<T> bankReconciliationList) throws Exception {
        if (bankReconciliationList == null) {
            return;
        }
        EntityTool.setUpdateStatus(bankReconciliationList);
        // 针对牧原客户字段被标品覆盖的情况，需要将客开相关字段给移除
        String removeKey = AppContext.getEnvConfig("cmp.bankreconciliation.removekey");
        List<String> removeKeyArray = null;
        if(StringUtils.isNotEmpty(removeKey)){
            removeKeyArray = Arrays.stream(removeKey.split( ",")).collect(Collectors.toList());
        }
        boolean mark = false;
        //实际类型是BizObject
        List<BizObject> bankReconciliationBizObjectList = (List<BizObject>) bankReconciliationList;
        for (BizObject bankReconciliationBizObject :
                bankReconciliationBizObjectList) {
            Short oppositetype = bankReconciliationBizObject.getShort("oppositetype");
            String oppositeobjectId = bankReconciliationBizObject.getString("oppositeobjectid");
            if (oppositetype != null) {
                setOppositeobjectidToBizField(bankReconciliationBizObject, oppositetype, oppositeobjectId);
            }
            if (CollectionUtils.isNotEmpty(removeKeyArray)) {
                for (String key : removeKeyArray) {
                    bankReconciliationBizObject.remove(key);
                }
            }
            bankReconciliationBizObject.setEntityStatus(EntityStatus.Update);
            //发布时进行乐观锁处理，即使用待认领金额和已认领金额进行发布过滤
            if("true".equals(bankReconciliationBizObject.get("pushFromOds"))){
                try {
                    //单独走更新逻辑,待认领金额和已认领金额必须是零,并且关联状态是未关联
                    int update = SqlHelper.update("com.yonyoucloud.fi.cmp.mapper.PullBankReconcliationMapper.updatePullBank", bankReconciliationBizObject);
                    if (update == 0){
                        mark = true;
                        Log.error(String.format("智能流水[%s]发布更新数据失败，存在并发操作，待认领金额和已认领金额存不同时为0！",bankReconciliationBizObject.get("bank_seq_no")));
                    }else {
                        List<BizObject> newList = new ArrayList<>();
                        bankReconciliationBizObject.remove("pubts");
                        newList.add(bankReconciliationBizObject);
                        CommonBankReconciliationProcessor.batchBizObjectBeforeUpdate(newList);
                        MetaDaoHelper.update(BankReconciliation.ENTITY_NAME, newList);
                    }
                }catch (Exception e){
                    mark = true;
                    Log.error(String.format("智能流水[%s]发布更新数据库异常：[%s]",bankReconciliationBizObject.get("bank_seq_no"),e.getMessage()));
                }
                bankReconciliationBizObject.remove("pushFromOds");
            }
        }
        if (mark){
            return;
        }

        CommonBankReconciliationProcessor.batchBizObjectBeforeUpdate(bankReconciliationBizObjectList);
        try {
            MetaDaoHelper.update(BankReconciliation.ENTITY_NAME, bankReconciliationBizObjectList);
        }catch (Exception e){
            log.error(String.format("执行=======================更新数据库，抛出异常，异常信息：%s",e.getMessage()));
            throw new CtmException(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540067B", "执行=======================更新数据库，抛出异常，异常信息：%s") /* "执行=======================更新数据库，抛出异常，异常信息：%s" */,e.getMessage()));
        }

    }


    /**
     * 回单关联状态的更新，也进行单独处理
     * @param bankReconciliationList
     * @reconciliationdatasource 1凭证；2日记账。
     * @param <T>
     * @throws Exception
     */
    public static <T extends BizObject> void updateBankReconciliation4ReceiptassociationStatus(List<BankReconciliation> bankReconciliationList) throws Exception {
        if (bankReconciliationList == null) {
            return;
        }
        List<BankReconciliation> updateBankReconciliations = new ArrayList<>();
        for(BankReconciliation bankReconciliation : bankReconciliationList){
            BankReconciliation bankReconciliationBizObject = new BankReconciliation();
            bankReconciliationBizObject.setId(bankReconciliation.getId());
            bankReconciliationBizObject.setReceiptId(bankReconciliation.getReceiptId());
            bankReconciliationBizObject.setReceiptassociation(bankReconciliation.getReceiptassociation());
            bankReconciliationBizObject.setPubts(bankReconciliation.getPubts());
            bankReconciliationBizObject.setEntityStatus(EntityStatus.Update);
            updateBankReconciliations.add(bankReconciliationBizObject);
        }
        MetaDaoHelper.update(BankReconciliation.ENTITY_NAME, updateBankReconciliations);
    }


    /**
     * 将银行流水对账的更新单据起一个方法，防止覆盖其他字段。
     * 需要做数据源区分，避免在日记账和凭证同时做对账的时候，数据相互覆盖*
     * @param bankReconciliationList
     * @reconciliationdatasource 1凭证；2日记账。
     * @param <T>
     * @throws Exception
     */
    public static <T extends BizObject> void updateBankReconciliation4Check(List<BankReconciliation> bankReconciliationList,Integer reconciliationdatasource) throws Exception {
        if (bankReconciliationList == null) {
            return;
        }
        List<BankReconciliation> updateBankReconciliations = new ArrayList<>();
        for(BankReconciliation bankReconciliation : bankReconciliationList){
            BankReconciliation bankReconciliationBizObject = new BankReconciliation();
            bankReconciliationBizObject.setId(bankReconciliation.getId());
            bankReconciliationBizObject.setPubts(bankReconciliation.getPubts());
            // 银行日记账
            if(ReconciliationDataSource.BankJournal.getValue()==reconciliationdatasource){
                bankReconciliationBizObject.setCheckdate(bankReconciliation.getCheckdate());
                bankReconciliationBizObject.setCheckman(bankReconciliation.getCheckman());
                bankReconciliationBizObject.setCheckflag(bankReconciliation.getCheckflag());
                bankReconciliationBizObject.setCheckno(bankReconciliation.getCheckno());
                bankReconciliationBizObject.setChecktime(bankReconciliation.getChecktime());
                bankReconciliationBizObject.setBankreconciliationsettingid(bankReconciliation.getBankreconciliationsettingid());
            }
            // 凭证
            if(ReconciliationDataSource.Voucher.getValue()==reconciliationdatasource){
                bankReconciliationBizObject.setOther_checkdate(bankReconciliation.getOther_checkdate());
                bankReconciliationBizObject.setOther_checkflag(bankReconciliation.getOther_checkflag());
                bankReconciliationBizObject.setOther_checkno(bankReconciliation.getOther_checkno());
                bankReconciliationBizObject.setOther_checktime(bankReconciliation.getOther_checktime());
                bankReconciliationBizObject.setGl_bankreconciliationsettingid(bankReconciliation.getGl_bankreconciliationsettingid());
            }

            bankReconciliationBizObject.setEntityStatus(EntityStatus.Update);
            updateBankReconciliations.add(bankReconciliationBizObject);
        }
        MetaDaoHelper.update(BankReconciliation.ENTITY_NAME, updateBankReconciliations);
    }

    public static <T extends BizObject> void updateBankReconciliationConfirmOrReject(List<T> bankReconciliationList,String confirmOrReject) throws Exception {
        if (bankReconciliationList == null) {
            return;
        }
        //实际类型是BizObject
        List<BizObject> bankReconciliationBizObjectList = (List<BizObject>) bankReconciliationList;
        for (BizObject bankReconciliationBizObject : bankReconciliationBizObjectList) {
            Short oppositetype = bankReconciliationBizObject.getShort("oppositetype");
            String oppositeobjectId = bankReconciliationBizObject.get("oppositeobjectid");
            if (oppositetype != null) {
                setOppositeobjectidToBizField(bankReconciliationBizObject, oppositetype, oppositeobjectId);
            }
            bankReconciliationBizObject.setEntityStatus(EntityStatus.Update);
        }
        CommonBankReconciliationProcessor.batchBizObjectBeforeUpdate(bankReconciliationBizObjectList, confirmOrReject);
        MetaDaoHelper.update(BankReconciliation.ENTITY_NAME, bankReconciliationBizObjectList);
        //处理特征
        checkCharacterDefForBankReconciliation(bankReconciliationBizObjectList);
    }

    /**
     * 只要执行过保存之后，特征就已经存入到数据库中，此时则不需要再使用Insert只需要update即可
     *
     * @param bankReconciliationBizObjectList
     */
    private static void checkCharacterDefForBankReconciliation(List<BizObject> bankReconciliationBizObjectList) {
        if (CollectionUtils.isNotEmpty(bankReconciliationBizObjectList)) {
            bankReconciliationBizObjectList.stream().forEach(bankrec -> {
                BizObject characterDef = bankrec.get("characterDef");
                if (characterDef != null) {
                    characterDef.setEntityStatus(EntityStatus.Update);
                }
            });
        }
    }

    public static void updateBankReconciliation(BankReconciliation bankReconciliation, String group) throws Exception {
        if (bankReconciliation == null) {
            return;
        }
        List<BankReconciliation> bankReconciliationList = new ArrayList<>();
        bankReconciliationList.add(bankReconciliation);
        updateBankReconciliation(bankReconciliationList, group);
    }

    //有的地方加了group，兼容一下
    public static void updateBankReconciliation(List<BankReconciliation> bankReconciliationList, String group) throws Exception {
        if (bankReconciliationList != null) {
            for (BankReconciliation bankReconciliation :
                    bankReconciliationList) {
                Short oppositetype = bankReconciliation.getOppositetype();
                Long oppositeobjectid = NumberUtil.parseLong(bankReconciliation.getOppositeobjectid(), null);
                if (oppositetype != null) {
                    setOppositeobjectidToBankReconciliationField(bankReconciliation, oppositetype, oppositeobjectid);
                }
            }
            if (group == null) {
                // TODO: 2024/8/13 group为null时，改为走不加group参数，验证下是否有影响
                CommonBankReconciliationProcessor.batchReconciliationBeforeUpdate(bankReconciliationList);
                MetaDaoHelper.update(BankReconciliation.ENTITY_NAME, bankReconciliationList);
            } else {
                CommonSaveUtils.updateBankReconciliation(bankReconciliationList, group);
                MetaDaoHelper.update(BankReconciliation.ENTITY_NAME, bankReconciliationList, group);
            }
        }
    }

    public static void setOppositeobjectidToBankReconciliationField(BankReconciliation bankReconciliation, Short oppositetype, Long oppositeobjectid) {
        if (bankReconciliation == null || oppositetype == null || oppositeobjectid == null) {
            return;
        }
        //if (oppositetype == OppositeType.Customer.getValue()) {
        //    bankReconciliation.setMerchant(oppositeobjectid);
        //    bankReconciliation.setVendor(null);
        //    bankReconciliation.setStaff(null);
        //} else if (oppositetype == OppositeType.Supplier.getValue()) {
        //    bankReconciliation.setMerchant(null);
        //    bankReconciliation.setVendor(oppositeobjectid);
        //    bankReconciliation.setStaff(null);
        //} else if (oppositetype == OppositeType.Employee.getValue()) {
        //    String oppositeobjectidStr = (oppositeobjectid != null) ? oppositeobjectid.toString() : null;
        //    bankReconciliation.setMerchant(null);
        //    bankReconciliation.setVendor(null);
        //    bankReconciliation.setStaff(oppositeobjectidStr);
        //}

        String oppositeobjectidStr = oppositeobjectid.toString();
        OppositeType oppositeTypeEnum = OppositeType.find(oppositetype);
        switch (oppositeTypeEnum) {
            case Customer:
                bankReconciliation.setMerchant(oppositeobjectid);
                bankReconciliation.setVendor(null);
                bankReconciliation.setStaff(null);
                bankReconciliation.setInnerorg(null);
                break;
            case Supplier:
                bankReconciliation.setMerchant(null);
                bankReconciliation.setVendor(oppositeobjectid);
                bankReconciliation.setStaff(null);
                bankReconciliation.setInnerorg(null);
                break;
            case Employee:
                bankReconciliation.setMerchant(null);
                bankReconciliation.setVendor(null);
                bankReconciliation.setStaff(oppositeobjectidStr);
                bankReconciliation.setInnerorg(null);
                break;
            case InnerOrg:
                bankReconciliation.setMerchant(null);
                bankReconciliation.setVendor(null);
                bankReconciliation.setStaff(null);
                bankReconciliation.setInnerorg(oppositeobjectidStr);
                break;
            default:
                break;
        }
    }

    public static void setOppositeobjectidToBillClaimItemField(BillClaimItem billClaimItem, Short oppositetype, Long oppositeobjectid) {
        if (billClaimItem == null || oppositetype == null || oppositeobjectid == null) {
            return;
        }
        String oppositeobjectidStr = oppositeobjectid.toString();
        OppositeType oppositeTypeEnum = OppositeType.find(oppositetype);
        switch (oppositeTypeEnum) {
            case Customer:
                billClaimItem.setMerchant(oppositeobjectid);
                billClaimItem.setVendor(null);
                billClaimItem.setStaff(null);
                billClaimItem.setInnerorg(null);
                break;
            case Supplier:
                billClaimItem.setMerchant(null);
                billClaimItem.setVendor(oppositeobjectid);
                billClaimItem.setStaff(null);
                billClaimItem.setInnerorg(null);
                break;
            case Employee:
                billClaimItem.setMerchant(null);
                billClaimItem.setVendor(null);
                billClaimItem.setStaff(oppositeobjectidStr);
                billClaimItem.setInnerorg(null);
                break;
            case InnerOrg:
                billClaimItem.setMerchant(null);
                billClaimItem.setVendor(null);
                billClaimItem.setStaff(null);
                billClaimItem.setInnerorg(oppositeobjectidStr);
                break;
            default:
                break;
        }
    }

    public static void setOppositeobjectidToBizField(BizObject bizObject, Short oppositetype, String oppositeobjectid) throws InstantiationException, IllegalAccessException {
        //oppositetype需要判空，否则在进行比较前拆箱时，会报空指针
        if (bizObject == null || oppositetype == null || oppositeobjectid == null) {
            return;
        }
        if (oppositetype == OppositeType.Customer.getValue()) {
            bizObject.set("merchant", oppositeobjectid);
            bizObject.set("vendor", null);
            bizObject.set("staff", null);
            bizObject.set("innerorg", null);
        } else if (oppositetype == OppositeType.Supplier.getValue()) {
            bizObject.set("merchant", null);
            bizObject.set("vendor", oppositeobjectid);
            bizObject.set("staff", null);
            bizObject.set("innerorg", null);
        } else if (oppositetype == OppositeType.Employee.getValue()) {
            String oppositeobjectidStr = (oppositeobjectid != null) ? oppositeobjectid.toString() : null;
            bizObject.set("merchant", null);
            bizObject.set("vendor", null);
            bizObject.set("staff", oppositeobjectidStr);
            bizObject.set("innerorg", null);
        } else if (oppositetype == OppositeType.InnerOrg.getValue()) {
            String oppositeobjectidStr = (oppositeobjectid != null) ? oppositeobjectid.toString() : null;
            bizObject.set("merchant", null);
            bizObject.set("vendor", null);
            bizObject.set("staff", null);
            bizObject.set("innerorg", oppositeobjectidStr);
        }
    }


    /**
     * 拼装疑重规则字段
     */
    private static void repeatCheck(BankReconciliation bankReconciliation) throws Exception {
        if (CtmDealDetailCheckMayRepeatUtils.isRepeatCheck) {
            QueryConditionGroup group = new QueryConditionGroup();
            if (bankReconciliation.getEntityStatus() == EntityStatus.Update) {
                group.addCondition(QueryCondition.name("id").not_eq(bankReconciliation.getId()));
            }
            // 增加疑重要素
            for (String repeatFactor : CtmDealDetailCheckMayRepeatUtils.repeatFactors) {
                Object repeatFactorValue = bankReconciliation.get(repeatFactor);
                if (null == repeatFactorValue) {
                    group.addCondition(QueryCondition.name(repeatFactor).is_null());
                } else {
                    group.addCondition(QueryCondition.name(repeatFactor).eq(repeatFactorValue));
                }
            }
            QueryConditionGroup repeatGroup = new QueryConditionGroup(ConditionOperator.or);
            repeatGroup.addCondition(QueryCondition.name("isrepeat").is_null());
            repeatGroup.addCondition(QueryCondition.name("isrepeat").is_not_null());

            group.addCondition(repeatGroup);
            QuerySchema querySchema = QuerySchema.create().addSelect("id").addCondition(group);
            List<BizObject> repeatList = MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME, querySchema, null);
            if (CollectionUtils.isNotEmpty(repeatList)) {
                bankReconciliation.setIsRepeat((short) BankDealDetailConst.REPEAT_DOUBT);
            }
        }
    }

    //region 银行流水主子表一致性
    public static <T extends BizObject> void insertBankReconciliationbusrelation_b(List<T> bankReconciliationbusrelationList) throws Exception {
        List<Long> relationIdList = new ArrayList<>();
        Map<Long, List<BizObject>> bankReconciliationbusrelatioMap = getBankReconciliationbusrelatioMap(bankReconciliationbusrelationList, EntityStatus.Insert, relationIdList);
        List<BizObject> bankReconciliationList = getBankReconciliationsById(relationIdList);
        if(CollectionUtils.isNotEmpty(bankReconciliationList)){
            //如果主表数据不为空，走最开始设计的逻辑，主子表一起更新
            for (BizObject bankReconciliation : bankReconciliationList) {
                bankReconciliation.set("associationstatus", AssociationStatus.Associated.getValue());
                //如果是挂账，完结状态肯定是“未完结”
                if (bankReconciliation.get("entrytype") != null && bankReconciliation.getShort("entrytype") == EntryType.Hang_Entry.getValue()) {
                    bankReconciliation.set("serialdealendstate", SerialdealendState.UNEND.getValue());
                } else {
                    if (!bankReconciliation.getBoolean("ispublish") /*&& bankReconciliation.get("BankReconciliationbusrelation_b") != null*/) {
                        bankReconciliation.set("serialdealendstate", SerialdealendState.END.getValue());
                    } else {
                        if (BigDecimal.ZERO.compareTo(bankReconciliation.getBigDecimal("amounttobeclaimed")) == 0) {
                            bankReconciliation.set("serialdealendstate", SerialdealendState.END.getValue());
                        } else {
                            bankReconciliation.set("serialdealendstate", SerialdealendState.UNEND.getValue());
                        }
                    }
                }
                bankReconciliation.put("BankReconciliationbusrelation_b", bankReconciliationbusrelatioMap.get(bankReconciliation.getId()));
                bankReconciliation.setEntityStatus(EntityStatus.Update);
            }
            updateBankReconciliationConsistency(bankReconciliationList);
        }else{
            //像智能流水，有特殊情况，插入子表的时候，主表还没有落库，单独操作子表
            List<BizObject> bankReconciliationRelationList = new ArrayList<>();
            for(BizObject item : bankReconciliationbusrelationList){
                bankReconciliationRelationList.add(item);
            }
            updateBankReconciliationRelationOnly(bankReconciliationRelationList);
        }
    }

    public static <T extends BizObject> void batchDeleteBankReconciliationbusrelation_b(List<T> bankReconciliationbusrelationList) throws Exception {
        List<Long> relationIdList = new ArrayList<>();
        Map<Long, List<BizObject>> bankReconciliationbusrelatioMap = getBankReconciliationbusrelatioMap(bankReconciliationbusrelationList, EntityStatus.Delete, relationIdList);
        List<BizObject> bankReconciliationList = getBankReconciliationsById(relationIdList);
        for (BizObject bankReconciliation : bankReconciliationList) {
            List<BizObject> relationFromDB = getBankReconciliationRelationListById(bankReconciliation.getId());
            List<BizObject> relationFromMap = bankReconciliationbusrelatioMap.get(bankReconciliation.getId());
            if (relationFromDB != null && relationFromMap != null && relationFromDB.size() == relationFromMap.size()) {
                bankReconciliation.set("associationstatus", AssociationStatus.NoAssociated.getValue());
                //modified by lichaor 20250626 按照邦龙的要求，只有在关联关系全部删完之后才更新成未完结，否则不动完结状态
                bankReconciliation.set("serialdealendstate", SerialdealendState.UNEND.getValue());
            } else {
                bankReconciliation.set("associationstatus", AssociationStatus.Associated.getValue());
            }
            bankReconciliation.set("serialdealendstate", SerialdealendState.UNEND.getValue());
            bankReconciliation.put("BankReconciliationbusrelation_b", bankReconciliationbusrelatioMap.get(bankReconciliation.getId()));
            bankReconciliation.setEntityStatus(EntityStatus.Update);
        }
        updateBankReconciliationConsistency(bankReconciliationList);
    }

    public static <T extends Long> void batchDeleteBankReconciliationbusrelationByIds(List<T> relationIds) throws Exception {
        List<BizObject> bankReconciliationRelationList = getBankReconciliationRelationsById(relationIds);
        batchDeleteBankReconciliationbusrelation_b(bankReconciliationRelationList);
    }

    /*public static String testSqlBuilder() throws Exception {
        BankReconciliation bankReconciliation = new BankReconciliation();
        bankReconciliation.setId(1L);
        bankReconciliation.setAssociationstatus(AssociationStatus.Associated.getValue());
        bankReconciliation.setSerialdealendstate(SerialdealendState.END.getValue());
        List<BankReconciliationbusrelation_b> bankReconciliationbusrelationList = new ArrayList<>();
        BankReconciliationbusrelation_b bankReconciliationbusrelation = new BankReconciliationbusrelation_b();
        bankReconciliationbusrelation.setId(1L);
        bankReconciliationbusrelation.setEntityStatus(EntityStatus.Delete);
        BankReconciliationbusrelation_b bankReconciliationbusrelation1 = new BankReconciliationbusrelation_b();
        bankReconciliationbusrelation1.setId(2L);
        bankReconciliationbusrelation1.setEntityStatus(EntityStatus.Delete);
        bankReconciliationbusrelationList.add(bankReconciliationbusrelation);
        bankReconciliationbusrelationList.add(bankReconciliationbusrelation1);
        bankReconciliation.setBankReconciliationbusrelation_b(bankReconciliationbusrelationList);
        bankReconciliation.setEntityStatus(EntityStatus.Update);

        MetaDaoHelper.update(BankReconciliation.ENTITY_NAME, bankReconciliation);
        return "success";
    }*/

    private static void updateBankReconciliationRelationOnly(List<BizObject> bankReconciliationRelationList) throws Exception {
        if (CollectionUtils.isNotEmpty(bankReconciliationRelationList)) {
            try {
                logStackTrack(bankReconciliationRelationList, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400679", "正常执行-只更新子表") /* "正常执行-只更新子表" */);
                MetaDaoHelper.update(BankReconciliationbusrelation_b.ENTITY_NAME, bankReconciliationRelationList);
            } catch (Exception e) {
                logStackTrack(bankReconciliationRelationList, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540067D", "catch中执行-只更新子表，异常") /* "catch中执行-只更新子表，异常" */);
            }
        }
    }

    private static void updateBankReconciliationConsistency(List<BizObject> bankReconciliationList) throws Exception {
        if (CollectionUtils.isNotEmpty(bankReconciliationList)) {
            try {
                bankReconciliationList.forEach(item -> {
                    item.remove("ispublish");
                    item.remove("amounttobeclaimed");
                    //item.remove("amountmoney");
                    item.remove("tran_amt");
                    item.remove("entrytype");
                });
                logStackTrack(bankReconciliationList, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540067C", "正常执行") /* "正常执行" */);
                CommonBankReconciliationProcessor.batchBizObjectBeforeUpdate(bankReconciliationList);
                MetaDaoHelper.update(BankReconciliation.ENTITY_NAME, bankReconciliationList);
            } catch (Exception e) {
                List<Long> idList = bankReconciliationList.stream().map(BizObject -> (Long) BizObject.getId()).collect(Collectors.toList());
                //如果更新失败，很可能是因为pubts，再查一次，再更新一次，还不对就抛出异常
                List<BizObject> bankReconciliationPubtsList = getBankReconciliationsById(idList);
                Map<Long, BizObject> bankReconciliationPubtsmap = bankReconciliationPubtsList.stream().collect(Collectors.toMap(BizObject::getId, o -> o));
                bankReconciliationList.stream().forEach(item -> {
                    item.set("pubts", bankReconciliationPubtsmap.get(item.getId()).get("pubts"));
                });
                logStackTrack(bankReconciliationList, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540067F", "catch中执行") /* "catch中执行" */);
                CommonBankReconciliationProcessor.batchBizObjectBeforeUpdate(bankReconciliationList);
                MetaDaoHelper.update(BankReconciliation.ENTITY_NAME, bankReconciliationList);
            }
        }
    }

    private static List<BizObject> getBankReconciliationsById(List<Long> relationIdList) throws Exception {
        List<BizObject> bankReconciliationList;
        QuerySchema querySchema = QuerySchema.create().addSelect("id,associationstatus,entrytype,ispublish,serialdealendstate,amounttobeclaimed,tran_amt,pubts");
        QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("id").in(relationIdList));
        querySchema.addCondition(group);
        bankReconciliationList = MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME, querySchema, null);
        return bankReconciliationList;
    }

    private static List<BizObject> getBankReconciliationRelationListById(Long relationId) throws Exception {
        List<BizObject> bankReconciliationRelationList;
        QuerySchema querySchema = QuerySchema.create().addSelect("id");
        QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("bankreconciliation").eq(relationId));
        querySchema.addCondition(group);
        bankReconciliationRelationList = MetaDaoHelper.queryObject(BankReconciliationbusrelation_b.ENTITY_NAME, querySchema, null);
        if (CollectionUtils.isEmpty(bankReconciliationRelationList)) {
            throw new Exception(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540067E", "没有找到银行流水处理子表信息") /* "没有找到银行流水处理子表信息" */);
        }
        return bankReconciliationRelationList;
    }

    private static <T extends Long> List<BizObject> getBankReconciliationRelationsById(List<T> relationIdList) throws Exception {
        List<BizObject> bankReconciliationList;
        QuerySchema querySchema = QuerySchema.create().addSelect("*");
        QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("id").in(relationIdList));
        querySchema.addCondition(group);
        bankReconciliationList = MetaDaoHelper.queryObject(BankReconciliationbusrelation_b.ENTITY_NAME, querySchema, null);
        if (CollectionUtils.isEmpty(bankReconciliationList)) {
            throw new Exception(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540067A", "没有找到银行流水处理子表relation信息") /* "没有找到银行流水处理子表relation信息" */);
        }
        return bankReconciliationList;
    }

    private static <T extends BizObject> Map<Long, List<BizObject>> getBankReconciliationbusrelatioMap(List<T> bankReconciliationbusrelationList, EntityStatus entityStatus, List<Long> relationIdList) throws Exception {
        Map<Long, List<BizObject>> bankReconciliationbusrelatioMap = new HashMap<>();
        for (BizObject item : bankReconciliationbusrelationList) {
            item.setEntityStatus(entityStatus);
            relationIdList.add(item.get("bankreconciliation"));
            Long mainId = item.get("bankreconciliation");
            List<BizObject> relationList = bankReconciliationbusrelatioMap.get(mainId);
            if (relationList == null) {
                relationList = new ArrayList<>();
            }
            relationList.add(item);
            //bankReconciliationbusrelatioMap.put(mainId, item.get("bankreconciliation"));
            bankReconciliationbusrelatioMap.put(mainId, relationList);
        }
        return bankReconciliationbusrelatioMap;
    }

    private static void logStackTrack(List<BizObject> bankReconciliationList, String type) {
        boolean logEnable = BooleanUtils.b(AppContext.getEnvConfig("cmp.bankreconciliation.consistency.log.enable", "false"));
        StringBuilder stackTrace = new StringBuilder();
        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
        for (StackTraceElement stackTraceElement : stackTraceElements) {
            stackTrace.append(String.format("%s.%s(%s:%d)%n",
                    stackTraceElement.getClassName(),
                    stackTraceElement.getMethodName(),
                    stackTraceElement.getFileName(),
                    stackTraceElement.getLineNumber()));

        }
        if (logEnable) {
            log.error("请求数据：" + JSON.toJSONString(bankReconciliationList) + "----" + type + ":流水主子表一致性请求堆栈：\n" + stackTrace.toString());
        }
        saveBusinessLogConsistency(bankReconciliationList, type, stackTrace.toString());
    }

    private static void saveBusinessLogConsistency(List<BizObject> bankReconciliationList, String type, String stackTrace) {
        CtmJSONObject logparam = new CtmJSONObject();
        logparam.put("bankinfo", bankReconciliationList);
        logparam.put("type", type);
        logparam.put("stackTrace", stackTrace);
        AppContext.getBean(CTMCMPBusinessLogService.class).saveBusinessLog(logparam, bankReconciliationList.get(0).getId().toString(), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400680", "银行流水一致性") /* "银行流水一致性" */, IServicecodeConstant.CMPBANKRECONCILIATION, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400680", "银行流水一致性") /* "银行流水一致性" */, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400680", "银行流水一致性") /* "银行流水一致性" */);
    }

    public static void repairBankReconciliationSatus(List<BankReconciliation> bankReconciliationList) throws Exception {
        List<Long> bankReconciliationIdList = bankReconciliationList.stream().map(BizObject -> (Long) BizObject.getId()).collect(Collectors.toList());
        QuerySchema querySchema = QuerySchema.create().addSelect("bankreconciliation");
        QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("bankreconciliation").in(bankReconciliationIdList));
        querySchema.addCondition(group);
        List<BankReconciliationbusrelation_b> bankReconciliationRelationList = MetaDaoHelper.queryObject(BankReconciliationbusrelation_b.ENTITY_NAME, querySchema, null);
        if (CollectionUtils.isNotEmpty(bankReconciliationRelationList)) {
            List<BizObject> needUpdateBankReconciliationList = new ArrayList<>();
            bankReconciliationRelationList.forEach(relation -> {
                BankReconciliation bankReconciliation = new BankReconciliation();
                bankReconciliation.setId(relation.getBankreconciliation());
                bankReconciliation.setAssociationstatus(AssociationStatus.Associated.getValue());
                needUpdateBankReconciliationList.add(bankReconciliation);
            });
            logStackTrack(needUpdateBankReconciliationList, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400681", "自动关联定时任务修复数据") /* "自动关联定时任务修复数据" */);
            CommonBankReconciliationProcessor.batchReconciliationBeforeUpdate(bankReconciliationList);
            MetaDaoHelper.update(BankReconciliation.ENTITY_NAME, needUpdateBankReconciliationList);
        }
    }
    //endregion
}