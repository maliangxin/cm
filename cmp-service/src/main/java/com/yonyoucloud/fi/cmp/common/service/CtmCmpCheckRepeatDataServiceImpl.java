package com.yonyoucloud.fi.cmp.common.service;


import com.yonyou.iuap.ucf.common.i18n.InternationalUtils;
import com.yonyou.iuap.ucf.common.i18n.MessageUtils;
import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVO;
import com.yonyou.ucf.basedoc.model.rpcparams.EnterpriseParams;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyoucloud.fi.cmp.common.CtmErrorCode;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.yonbip.iuap.xport.common.error.ExcelErrorLocation;
import com.yonyou.yonbip.iuap.xport.importing.data.ImportBatchData;
import com.yonyou.yonbip.iuap.xport.importing.processor.pojo.ImportSingleData;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.cmp.bankdealdetail.BankDealDetail;
import com.yonyoucloud.fi.cmp.bankelectronicreceipt.BankElectronicReceipt;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.cmpentity.AssociationStatus;
import com.yonyoucloud.fi.cmp.cmpentity.DateOrigin;
import com.yonyoucloud.fi.cmp.cmpentity.EventType;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.consts.BankDealDetailConst;
import com.yonyoucloud.fi.cmp.util.DateUtils;
import com.yonyoucloud.fi.cmp.util.CtmDealDetailCheckMayRepeatUtils;
import com.yonyoucloud.fi.cmp.util.StringUtils;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import com.yonyoucloud.fi.cmp.util.cuckoo.CmpCuckooFilters;
import com.yonyoucloud.fi.cmp.weekday.DateUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.biz.base.Objectlizer;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 银企联下载数据验重服务实现类
 * @author liuwtr
 * @version V1.0
 * @date 2023/12/13 16:59
 * @Copyright yonyou
 *
 */
@Slf4j
@Service
public class CtmCmpCheckRepeatDataServiceImpl implements CtmCmpCheckRepeatDataService {
    @Autowired
    private BaseRefRpcService baseRefRpcService;
    @Autowired
    private CmpCuckooFilters cmpCuckooFilters;
    @Autowired
    private YQLInvalidateService yqlInvalidateService;

    @Override
    public  <T extends BizObject> List<T> checkRepeatData(List<T> downloadData, Short checkRepeatDataBillType) throws Exception{
        List<T> insertVos = new ArrayList<>();
        if(EventType.BankDealDetail.getValue() == checkRepeatDataBillType){
            insertVos = checkBankDealDetail(downloadData,checkRepeatDataBillType);
        }else if(EventType.CashMark.getValue() == checkRepeatDataBillType){
            insertVos = checkBankReconciliation(downloadData,checkRepeatDataBillType);
        }else if(EventType.BankElectronicReceipt.getValue() == checkRepeatDataBillType){
            insertVos = checkBankElectronicReceipt(downloadData,checkRepeatDataBillType);
        }
        return insertVos;
    }

    @Override
    public Map<String, List<BankDealDetail>> checkBankDealDetailRepeat(List<BankDealDetail> downloadData) throws Exception {
        Map<String, List<BankDealDetail>> returnMap = new HashMap<>();
        // 如果银企联返回数据为空，直接返回
        if (downloadData.isEmpty()) {
            return returnMap;
        }
        // 银企联下载数据去重并根据uniquNo查询
        // 1，银企联下载数据去重
        List<BankDealDetail> bankDealDetails = new ArrayList<>();
        Set<String> uniquNos = new HashSet<>();
        Set<String> newUniquNos = new HashSet<>();
        for (BizObject bizObject : downloadData) {
            BankDealDetail bankDealDetail = (BankDealDetail) bizObject;
            if(Objects.nonNull(bizObject.get("is_refund"))){
                bankDealDetail.set("is_refund",bizObject.getString("is_refund"));
            }
            // 流水号+交易日期+交易时间+金额+方向+本方账号+对方账号+对方户名
            String newUniquNo = formatConctaInfoBankDealDetail(bankDealDetail);
            bankDealDetail.fillConcatInfo(newUniquNo);
            if (StringUtils.isEmpty(bankDealDetail.getUnique_no())) {
                if (!newUniquNos.contains(newUniquNo)) {
                    newUniquNos.add(newUniquNo);
                    bankDealDetails.add(bankDealDetail);
                }
            } else {
                if (!uniquNos.contains(bankDealDetail.getUnique_no())) {
                    uniquNos.add(bankDealDetail.getUnique_no());
                    uniquNos.add(newUniquNo);
                    bankDealDetails.add(bankDealDetail);
                }
            }
        }
        uniquNos.addAll(newUniquNos);
        // 2,根据uniquNO查询数据库数据
        Map<String, BizObject> bizObjectMap = getExistDataByUniquNo(uniquNos, EventType.BankDealDetail.getValue());
        // 验重操作
        List<BankDealDetail> insertBizObjects = new ArrayList<>(); // 入库列表
        List<BankDealDetail> updateVoList = new ArrayList<>(); // 更新流水号/余额列表
        List<BankDealDetail> deleteVoList = new ArrayList<>();//状态为作废删除流水
        for (BankDealDetail bankDealDetail : bankDealDetails) {
            // 银行返回数据无唯一号
            if (StringUtils.isEmpty(bankDealDetail.getUnique_no())) {
                // 无唯一号 有字段拼接唯一号的情况，更新余额
                // 本方账号+交易日期+交易时间+金额+方向+流水号+对方账号+对方户名
//                String newUniquNo = formatConctaInfoBankDealDetail(bankDealDetail);
                String newUniquNo = bankDealDetail.getConcat_info();
                // 数据库存在该concat_info
                if (bizObjectMap.containsKey(newUniquNo)) {
                    if (bizObjectMap.get(newUniquNo).get("acctbal") == null ||
                            BigDecimal.ZERO.compareTo(bizObjectMap.get(newUniquNo).get("acctbal")) == 0) {
                        if (bankDealDetail.getAcctbal() != null) {
                            bizObjectMap.get(newUniquNo).set("acctbal", bankDealDetail.getAcctbal());
                        }
                        if((Objects.nonNull(bankDealDetail.get("is_refund")) && "3".equals(bankDealDetail.get("is_refund")))){
                            bizObjectMap.get(newUniquNo).setEntityStatus(EntityStatus.Delete);
                            deleteVoList.add((BankDealDetail) bizObjectMap.get(newUniquNo));
                        }else {
                            bizObjectMap.get(newUniquNo).setEntityStatus(EntityStatus.Update);
                            updateVoList.add((BankDealDetail) bizObjectMap.get(newUniquNo));
                        }
                    }
                } else {
                    // 无唯一号 无字段拼接唯一号，直接入库
                    bankDealDetail.fillConcatInfo(newUniquNo);
                    if(!(Objects.nonNull(bankDealDetail.get("is_refund")) && "3".equals(bankDealDetail.get("is_refund")))){
                        insertBizObjects.add(bankDealDetail);
                    }
                }
            } else {
                // 有唯一号 数据库中有唯一号
                if (bizObjectMap.containsKey(bankDealDetail.getUnique_no())) {
                    BankDealDetail dbBankDealDetail = (BankDealDetail) bizObjectMap.get(bankDealDetail.getUnique_no());
                    // 数据中存在该唯一号数据,判断流水号是否一致
                    boolean isUpdate = false;
                    if (bankDealDetail.getBankseqno() != null &&
                            bankDealDetail.getBankseqno().equals(dbBankDealDetail.get("bankseqno"))) {
                        //CZFW-366798【DSP支持问题】小核心撤销流水，没有通过唯一码将库中流水进行删除
                        if((Objects.nonNull(bankDealDetail.get("is_refund")) && "3".equals(bankDealDetail.get("is_refund")))){
                            dbBankDealDetail.setEntityStatus(EntityStatus.Delete);
                            deleteVoList.add((BankDealDetail) bizObjectMap.get(bankDealDetail.getUnique_no()));
                        }else {
                            // 流水号一致 判断数据库数据有无余额
                            if (dbBankDealDetail.get("acctbal") == null ||
                                    BigDecimal.ZERO.compareTo(dbBankDealDetail.get("acctbal")) == 0) {
                                if (bankDealDetail.getAcctbal() != null) {
                                    dbBankDealDetail.set("acctbal", bankDealDetail.getAcctbal());
                                    isUpdate = true;
                                }
                            }
                            // 判断对方账号、对方户名、摘要字段
                            if(StringUtils.isEmpty(dbBankDealDetail.getTo_acct_no())){
                                dbBankDealDetail.set("to_acct_no", bankDealDetail.getTo_acct_no());
                                isUpdate = true;
                            }
                            if(StringUtils.isEmpty(dbBankDealDetail.getTo_acct_name())){
                                dbBankDealDetail.set("to_acct_name", bankDealDetail.getTo_acct_name());
                                isUpdate = true;
                            }
                            if(StringUtils.isEmpty(dbBankDealDetail.getRemark())){
                                dbBankDealDetail.set("remark", bankDealDetail.getRemark());
                                isUpdate = true;
                            }
                            if (StringUtils.isEmpty(dbBankDealDetail.getTo_acct_bank())) {
                                dbBankDealDetail.set("to_acct_bank", bankDealDetail.getTo_acct_bank());
                                isUpdate = true;
                            }
                            if(StringUtils.isEmpty(dbBankDealDetail.getTo_acct_bank_name())){
                                dbBankDealDetail.set("to_acct_bank_name", bankDealDetail.getTo_acct_bank_name());
                                isUpdate = true;
                            }
                            if (bankDealDetail.getRefundFlag() != null) {
                                dbBankDealDetail.setRefundFlag(bankDealDetail.getRefundFlag());
                                isUpdate = true;
                            }
                            if (!StringUtils.isEmpty(bankDealDetail.getOriginBankseqno())) {
                                dbBankDealDetail.setOrignBankseqno(bankDealDetail.getOriginBankseqno());
                                isUpdate = true;
                            }
                            if(isUpdate){
                                if((Objects.nonNull(bankDealDetail.get("is_refund")) && "3".equals(bankDealDetail.get("is_refund")))){
                                    dbBankDealDetail.setEntityStatus(EntityStatus.Delete);
                                    deleteVoList.add((BankDealDetail) bizObjectMap.get(bankDealDetail.getUnique_no()));
                                }else {
                                    dbBankDealDetail.setEntityStatus(EntityStatus.Update);
                                    updateVoList.add((BankDealDetail) bizObjectMap.get(bankDealDetail.getUnique_no()));
                                }
                            }
                        }
                    } else {
                        // 流水号不一致 更新流水号
                        bizObjectMap.get(bankDealDetail.getUnique_no()).set("bankseqno", bankDealDetail.getBankseqno());
                        if((Objects.nonNull(bankDealDetail.get("is_refund")) && "3".equals(bankDealDetail.get("is_refund")))){
                            dbBankDealDetail.setEntityStatus(EntityStatus.Delete);
                            deleteVoList.add((BankDealDetail) bizObjectMap.get(bankDealDetail.getUnique_no()));
                        }else {
                            dbBankDealDetail.setEntityStatus(EntityStatus.Update);
                            updateVoList.add((BankDealDetail) bizObjectMap.get(bankDealDetail.getUnique_no()));
                        }
                    }

                } else {
                    //有唯一号，数据库中无该唯一号数据，判断字段拼接唯一号
                    //本方账号+交易日期+交易时间+金额+方向+流水号+对方账号+对方户名
                    String newUniquNo = formatConctaInfoBankDealDetail(bankDealDetail);
                    // 数据库中不存在该字段拼接唯一号
                    if(!bizObjectMap.containsKey(newUniquNo)){
                        bankDealDetail.fillConcatInfo(newUniquNo);
                        if(!(Objects.nonNull(bankDealDetail.get("is_refund")) && "3".equals(bankDealDetail.get("is_refund")))){
                            insertBizObjects.add( bankDealDetail);
                        }
                    } else {
                        // 退票，删除
                        if((Objects.nonNull(bankDealDetail.get("is_refund")) && "3".equals(bankDealDetail.get("is_refund")))){
                            bizObjectMap.get(newUniquNo).setEntityStatus(EntityStatus.Delete);
                            deleteVoList.add((BankDealDetail) bizObjectMap.get(newUniquNo));
                        }else {
                            // 数据库中存在该字段拼接唯一号,且库中唯一号为空，更新唯一号；否则入库
                            if(StringUtils.isEmpty(bizObjectMap.get(newUniquNo).get("unique_no"))){
                                bizObjectMap.get(newUniquNo).set("unique_no", bankDealDetail.getUnique_no());
                                bizObjectMap.get(newUniquNo).setEntityStatus(EntityStatus.Update);
                                updateVoList.add((BankDealDetail) bizObjectMap.get(newUniquNo));
                            } else {
                                insertBizObjects.add( bankDealDetail);
                            }
                        }
                    }
                }
            }
        }
        // 更新流水号，更新余额
        MetaDaoHelper.update(BankDealDetail.ENTITY_NAME, updateVoList);
        //流水状态=“已作废”，则直接删除该笔交易流水及对应的银行流水认领的数据
        // MetaDaoHelper.delete(BankDealDetail.ENTITY_NAME, deleteVoList);
        yqlInvalidateService.refundDelete(deleteVoList);

        returnMap.put("insertData", insertBizObjects);
        returnMap.put("updateData", updateVoList);
        return returnMap;
    }
//因为调整页面手动拉取计数的关系下面的方法他不用啦 请使用上面的这个方法
    private <T extends BizObject> List<T> checkBankDealDetail(List<T> downloadData, Short checkRepeatDataBillType) throws Exception{
        // 银企联下载数据去重并根据uniquNo查询
        // 1，银企联下载数据去重
        List<BankDealDetail> bankDealDetails = new ArrayList<>();
        Set<String> uniquNos = new HashSet<>();
        Set<String> newUniquNos = new HashSet<>();
        // 如果银企联返回数据为空，直接返回
        if(downloadData == null || downloadData.size() < 1){
            return downloadData;
        }
        for(BizObject bizObject : downloadData){
            BankDealDetail bankDealDetail = (BankDealDetail) bizObject;
            if(Objects.nonNull(bizObject.get("is_refund"))){
                bankDealDetail.set("is_refund",bizObject.getString("is_refund"));
            }
            // 流水号+交易日期+交易时间+金额+方向+本方账号+对方账号+对方户名
            String newUniquNo = formatConctaInfoBankDealDetail(bankDealDetail);
            bankDealDetail.fillConcatInfo(newUniquNo);
            if(StringUtils.isEmpty(bankDealDetail.getUnique_no())){
                if(!newUniquNos.contains(newUniquNo)){
                    newUniquNos.add(newUniquNo);
                    bankDealDetails.add(bankDealDetail);
                }
            }else {
                if (!uniquNos.contains(bankDealDetail.getUnique_no())) {
                    uniquNos.add(bankDealDetail.getUnique_no());
                    uniquNos.add(newUniquNo);
                    bankDealDetails.add(bankDealDetail);
                }
            }
        }
        uniquNos.addAll(newUniquNos);
        // 2,根据uniquNO查询数据库数据
        Map<String, BizObject> bizObjectMap = getExistDataByUniquNo(uniquNos, checkRepeatDataBillType);
        // 验重操作
        List<T> insertBizObjects = new ArrayList<>(); // 入库列表
        List<T> updateVoList = new ArrayList<>(); // 更新流水号/余额列表
        List<T> deleteVoList = new ArrayList<>();//状态为作废删除流水
        for (BankDealDetail bankDealDetail : bankDealDetails) {
            // 银行返回数据无唯一号
            if(StringUtils.isEmpty(bankDealDetail.getUnique_no())){
                // 无唯一号 有字段拼接唯一号的情况，更新余额
                // 本方账号+交易日期+交易时间+金额+方向+流水号+对方账号+对方户名
//                String newUniquNo = formatConctaInfoBankDealDetail(bankDealDetail);
                String newUniquNo = bankDealDetail.getConcat_info();
                // 数据库存在该concat_info
                if(bizObjectMap.containsKey(newUniquNo)){
                    if(bizObjectMap.get(newUniquNo).get("acctbal") == null ||
                            BigDecimal.ZERO.compareTo(bizObjectMap.get(newUniquNo).get("acctbal")) == 0){
                        if(bankDealDetail.getAcctbal() != null){
                            bizObjectMap.get(newUniquNo).set("acctbal", bankDealDetail.getAcctbal());
                        }
                    }
                    if((Objects.nonNull(bankDealDetail.get("is_refund")) && "3".equals(bankDealDetail.get("is_refund")))){
                        bizObjectMap.get(newUniquNo).setEntityStatus(EntityStatus.Delete);
                        deleteVoList.add((T) bizObjectMap.get(newUniquNo));
                    }else {
                        bizObjectMap.get(newUniquNo).setEntityStatus(EntityStatus.Update);
                        updateVoList.add((T) bizObjectMap.get(newUniquNo));
                    }
                }else {
                    // 无唯一号 无字段拼接唯一号，直接入库
                    bankDealDetail.fillConcatInfo(newUniquNo);
                    if(!(Objects.nonNull(bankDealDetail.get("is_refund")) && "3".equals(bankDealDetail.get("is_refund")))){
                        insertBizObjects.add((T) bankDealDetail);
                    }

                }
            } else {
                // 有唯一号 数据库中有唯一号
                if (bizObjectMap.containsKey(bankDealDetail.getUnique_no())) {
                    BankDealDetail dbBankDealDetail = (BankDealDetail) bizObjectMap.get(bankDealDetail.getUnique_no());
                    // 数据中存在该唯一号数据,判断流水号是否一致
                    boolean isUpdate = false;
                    if (bankDealDetail.getBankseqno() != null &&
                            dbBankDealDetail.get("bankseqno").equals(bankDealDetail.getBankseqno())) {
                        //CZFW-366798【DSP支持问题】小核心撤销流水，没有通过唯一码将库中流水进行删除
                        if((Objects.nonNull(bankDealDetail.get("is_refund")) && "3".equals(bankDealDetail.get("is_refund")))){
                            dbBankDealDetail.setEntityStatus(EntityStatus.Delete);
                            deleteVoList.add((T) bizObjectMap.get(bankDealDetail.getUnique_no()));
                        }else {
                            // 流水号一致 判断数据库数据有无余额
                            if (dbBankDealDetail.get("acctbal") == null ||
                                    BigDecimal.ZERO.compareTo(dbBankDealDetail.get("acctbal")) == 0) {
                                if (bankDealDetail.getAcctbal() != null) {
                                    dbBankDealDetail.set("acctbal", bankDealDetail.getAcctbal());
                                    isUpdate = true;
                                }
                            }
                            // 判断对方账号、对方户名、摘要字段
                            if( StringUtils.isNotEmpty(bankDealDetail.getTo_acct_no()) && !bankDealDetail.getTo_acct_no().equals(dbBankDealDetail.getTo_acct_no()) ){
                                dbBankDealDetail.set("to_acct_no", bankDealDetail.getTo_acct_no());
                                isUpdate = true;
                            }
                            if( StringUtils.isNotEmpty(bankDealDetail.getTo_acct_name()) && !bankDealDetail.getTo_acct_name().equals(dbBankDealDetail.getTo_acct_name())){
                                dbBankDealDetail.set("to_acct_name", bankDealDetail.getTo_acct_name());
                                isUpdate = true;
                            }
                            if( StringUtils.isNotEmpty(bankDealDetail.getRemark()) && !bankDealDetail.getRemark().equals(dbBankDealDetail.getRemark())){
                                dbBankDealDetail.set("remark", bankDealDetail.getRemark());
                                isUpdate = true;
                            }
                            // CZFW-403847【DSP支持问题】老师，您好，银行流水认领对方开户行、对方单位、对方账号、对方户名、对方开户行名缺失
                            // 更新对方开户行
                            if( StringUtils.isNotEmpty(bankDealDetail.getTo_acct_bank()) && !bankDealDetail.getTo_acct_bank().equals(dbBankDealDetail.getTo_acct_bank())){
                                dbBankDealDetail.set("to_acct_bank", bankDealDetail.getTo_acct_bank());
                                isUpdate = true;
                            }
                            if( StringUtils.isNotEmpty(bankDealDetail.getTo_acct_bank_name()) && !bankDealDetail.getTo_acct_bank_name().equals(dbBankDealDetail.getTo_acct_bank_name())){
                                dbBankDealDetail.set("to_acct_bank_name", bankDealDetail.getTo_acct_bank_name());
                                isUpdate = true;
                            }
                            if(isUpdate){
                                if((Objects.nonNull(bankDealDetail.get("is_refund")) && "3".equals(bankDealDetail.get("is_refund")))){
                                    dbBankDealDetail.setEntityStatus(EntityStatus.Delete);
                                    deleteVoList.add((T) bizObjectMap.get(bankDealDetail.getUnique_no()));
                                }else {
                                    dbBankDealDetail.setEntityStatus(EntityStatus.Update);
                                    updateVoList.add((T) bizObjectMap.get(bankDealDetail.getUnique_no()));
                                }
                            }
                        }
                    }else {
                        // 流水号不一致 更新流水号
                        bizObjectMap.get(bankDealDetail.getUnique_no()).set("bankseqno", bankDealDetail.getBankseqno());
                        if((Objects.nonNull(bankDealDetail.get("is_refund")) && "3".equals(bankDealDetail.get("is_refund")))){
                            dbBankDealDetail.setEntityStatus(EntityStatus.Delete);
                            deleteVoList.add((T) bizObjectMap.get(bankDealDetail.getUnique_no()));
                        }else {
                            dbBankDealDetail.setEntityStatus(EntityStatus.Update);
                            updateVoList.add((T) bizObjectMap.get(bankDealDetail.getUnique_no()));
                        }
                    }

                }else {
                    //有唯一号，数据库中无该唯一号数据，判断字段拼接唯一号
                    //本方账号+交易日期+交易时间+金额+方向+流水号+对方账号+对方户名
                    String newUniquNo = formatConctaInfoBankDealDetail(bankDealDetail);
                    // 数据库中不存在该字段拼接唯一号
                    if(!bizObjectMap.containsKey(newUniquNo)){
                        bankDealDetail.fillConcatInfo(newUniquNo);
                        if(!(Objects.nonNull(bankDealDetail.get("is_refund")) && "3".equals(bankDealDetail.get("is_refund")))){
                            insertBizObjects.add((T) bankDealDetail);
                        }
                    } else {
                        // 退票，删除
                        if((Objects.nonNull(bankDealDetail.get("is_refund")) && "3".equals(bankDealDetail.get("is_refund")))){
                            bizObjectMap.get(newUniquNo).setEntityStatus(EntityStatus.Delete);
                            deleteVoList.add((T) bizObjectMap.get(newUniquNo));
                        }else {
                            // 数据库中存在该字段拼接唯一号,且库中唯一号为空，更新唯一号；否则入库
                            if(StringUtils.isEmpty(bizObjectMap.get(newUniquNo).get("unique_no"))){
                                bizObjectMap.get(newUniquNo).set("unique_no", bankDealDetail.getUnique_no());
                                bizObjectMap.get(newUniquNo).setEntityStatus(EntityStatus.Update);
                                updateVoList.add((T) bizObjectMap.get(newUniquNo));
                            } else {
                                insertBizObjects.add((T) bankDealDetail);
                            }
                        }

                    }
                }
            }
        }
        // 更新流水号，更新余额
        MetaDaoHelper.update(BankDealDetail.ENTITY_NAME, updateVoList);
        //流水状态=“已作废”，则直接删除该笔交易流水及对应的银行流水认领的数据
        // MetaDaoHelper.delete(BankDealDetail.ENTITY_NAME, deleteVoList);
        yqlInvalidateService.refundDeleteByBizObj(deleteVoList);
        return insertBizObjects;
    }

    private <T extends BizObject> List<T> checkBankReconciliation(List<T> downloadData, Short checkRepeatDataBillType) throws Exception{
        // 银企联下载数据去重并根据uniquNo查询
        // 1，去重后数据
        List<BankReconciliation> bankReconciliations = new ArrayList<>();
        Set<String> uniquNos = new HashSet<>();
        Set<String> newUniquNos = new HashSet<>();
        // 如果银企联返回数据为空，直接返回
        if(downloadData == null || downloadData.size() < 1){
            return downloadData;
        }
        for(BizObject bizObject : downloadData){
            BankReconciliation bankReconciliation = (BankReconciliation) bizObject;
            if(Objects.nonNull(bizObject.get("is_refund"))){
                bankReconciliation.set("is_refund",bizObject.getString("is_refund"));
            }
            // 本方账号+交易日期+交易时间+金额+方向+流水号+对方账号+对方户名
            String newUniquNo = formatConctaInfoBankReconciliation(bankReconciliation);
            bankReconciliation.setConcat_info(newUniquNo);
            // 四元素外增加的判断疑重规则
            if (CtmDealDetailCheckMayRepeatUtils.isRepeatCheck) {
                // 如果设置疑重规则，同时生成疑重规则串：四要素+疑重规则串  本方账号|交易日期|交易金额|交易方向|疑重字段
                String concatInfoDefine= formatConcatInfoDefineFactorsBankReconciliation(bankReconciliation);
                bankReconciliation.setConcat_info_define(concatInfoDefine);
            }
            if(StringUtils.isEmpty(bankReconciliation.getUnique_no())) {
                if (!newUniquNos.contains(newUniquNo)) {
                    newUniquNos.add(newUniquNo);
                    bankReconciliations.add(bankReconciliation);
                }
            }else {
                if(!uniquNos.contains(bankReconciliation.getUnique_no())){
                    uniquNos.add(bankReconciliation.getUnique_no());
                    uniquNos.add(newUniquNo);
                    bankReconciliations.add(bankReconciliation);
                }
            }
        }
        uniquNos.addAll(newUniquNos);
        // 根据uniquNO查询数据库数据
        Map<String, BizObject> bizObjectMap = getExistDataByUniquNo(uniquNos, checkRepeatDataBillType);
        // 验重操作
        List<T> insertBizObjects = new ArrayList<>(); // 入库列表
        List<T> updateVoList = new ArrayList<>(); // 更新流水号/余额列表
        List<T> deleteVoList = new ArrayList<>();//状态为作废删除流水
        for (BankReconciliation bankReconciliation : bankReconciliations) {
            if (StringUtils.isEmpty(bankReconciliation.getUnique_no())) {
                // 无唯一号 本方账号+交易日期+交易时间+金额+方向+流水号+对方账号+对方户名
                String newUniquNo = formatConctaInfoBankReconciliation(bankReconciliation);
                if(bizObjectMap.containsKey(newUniquNo)){
                    // CZFW-423955【DSP支持问题】账户交易流水接口推送抹账标识到司库系统，但司库系统已获取的流水未自动删除，麻烦帮忙看一下
                    // 删除对余额是否为空的控制
                    if (bizObjectMap.get(newUniquNo).get("acctbal") == null ||
                            BigDecimal.ZERO.compareTo(bizObjectMap.get(newUniquNo).get("acctbal")) == 0) {
                        if (bankReconciliation.getAcct_bal() != null) {
                            bizObjectMap.get(newUniquNo).set("acct_bal", bankReconciliation.getAcct_bal());
                        }
                    }
                    if((Objects.nonNull(bankReconciliation.get("is_refund")) && "3".equals(bankReconciliation.get("is_refund")))){
                        bizObjectMap.get(newUniquNo).setEntityStatus(EntityStatus.Delete);
                        deleteVoList.add((T) bizObjectMap.get(newUniquNo));
                    }else {
                        bizObjectMap.get(newUniquNo).setEntityStatus(EntityStatus.Update);
                        updateVoList.add((T) bizObjectMap.get(newUniquNo));
                    }
                } else {
                    // 数据库中不存在该唯一号数据，直接入库
                    bankReconciliation.setConcat_info(newUniquNo);
                    if(!(Objects.nonNull(bankReconciliation.get("is_refund")) && "3".equals(bankReconciliation.get("is_refund")))){
                        insertBizObjects.add((T) bankReconciliation);
                    }
                }
            } else {
                // 有唯一号
                if (bizObjectMap.containsKey(bankReconciliation.getUnique_no())) {
                    // 数据中存在该唯一号数据,判断流水号是否一致
                    BankReconciliation dbBankReconciliation = (BankReconciliation) bizObjectMap.get(bankReconciliation.getUnique_no());
                    if (bankReconciliation.getBank_seq_no() != null &&
                            bankReconciliation.getBank_seq_no().equals(dbBankReconciliation.get("bank_seq_no"))) {
                        //CZFW-366798【DSP支持问题】小核心撤销流水，没有通过唯一码将库中流水进行删除
                        if((Objects.nonNull(bankReconciliation.get("is_refund")) && "3".equals(bankReconciliation.get("is_refund")))){
                            dbBankReconciliation.setEntityStatus(EntityStatus.Delete);
                            deleteVoList.add((T) dbBankReconciliation);
                        }else{
                            // 流水号一致 判断数据库数据有无余额
                            boolean isUpdate = false;
                            if (dbBankReconciliation.get("acct_bal") == null ||
                                    BigDecimal.ZERO.compareTo(dbBankReconciliation.get("acct_bal")) == 0) {
                                if (bankReconciliation.getAcct_bal() != null) {
                                    dbBankReconciliation.set("acct_bal", bankReconciliation.getAcct_bal());
                                    isUpdate = true;
                                }
                            }
                            // 未发布  未关联 才进行更新对方信息操作
                            if((dbBankReconciliation.getIspublish() == null
                                    || StringUtils.isEmpty(dbBankReconciliation.getIspublish().toString())
                                    || !dbBankReconciliation.getIspublish())
                                    && AssociationStatus.NoAssociated.getValue() == dbBankReconciliation.getAssociationstatus()){
                                // 判断对方账号、对方户名、摘要字段
                                if( StringUtils.isNotEmpty(bankReconciliation.getTo_acct_no()) && !bankReconciliation.getTo_acct_no().equals(dbBankReconciliation.getTo_acct_no()) ){
                                    dbBankReconciliation.set("to_acct_no", bankReconciliation.getTo_acct_no());
                                    isUpdate = true;
                                }
                                if( StringUtils.isNotEmpty(bankReconciliation.getTo_acct_name()) && !bankReconciliation.getTo_acct_name().equals(dbBankReconciliation.getTo_acct_name())){
                                    dbBankReconciliation.set("to_acct_name", bankReconciliation.getTo_acct_name());
                                    isUpdate = true;
                                }
                                if( StringUtils.isNotEmpty(bankReconciliation.getRemark()) && !bankReconciliation.getRemark().equals(dbBankReconciliation.getRemark())){
                                    dbBankReconciliation.set("remark", bankReconciliation.getRemark());
                                    isUpdate = true;
                                }
                                // CZFW-403847【DSP支持问题】老师，您好，银行流水认领对方开户行、对方单位、对方账号、对方户名、对方开户行名缺失
                                // 更新对方开户行
                                if( StringUtils.isNotEmpty(bankReconciliation.getTo_acct_bank()) && !bankReconciliation.getTo_acct_bank().equals(dbBankReconciliation.getTo_acct_bank())){
                                    dbBankReconciliation.set("to_acct_bank", bankReconciliation.getTo_acct_bank());
                                    isUpdate = true;
                                }
                                if( StringUtils.isNotEmpty(bankReconciliation.getTo_acct_bank_name()) && !bankReconciliation.getTo_acct_bank_name().equals(dbBankReconciliation.getTo_acct_bank_name())){
                                    dbBankReconciliation.set("to_acct_bank_name", bankReconciliation.getTo_acct_bank_name());
                                    isUpdate = true;
                                }
                            }
                            if(isUpdate){
                                if((Objects.nonNull(bankReconciliation.get("is_refund")) && "3".equals(bankReconciliation.get("is_refund")))){
                                    dbBankReconciliation.setEntityStatus(EntityStatus.Delete);
                                    deleteVoList.add((T) dbBankReconciliation);
                                }else {
                                    dbBankReconciliation.setEntityStatus(EntityStatus.Update);
                                    updateVoList.add((T) dbBankReconciliation);
                                }

                            }
                        }
                    } else {
                        // 流水号不一致 更新流水号
                        bizObjectMap.get(bankReconciliation.getUnique_no()).set("bank_seq_no", bankReconciliation.getBank_seq_no());
                        if((Objects.nonNull(bankReconciliation.get("is_refund")) && "3".equals(bankReconciliation.get("is_refund")))){
                            bizObjectMap.get(bankReconciliation.getUnique_no()).setEntityStatus(EntityStatus.Delete);
                            deleteVoList.add((T) bizObjectMap.get(bankReconciliation.getUnique_no()));
                        }else {
                            bizObjectMap.get(bankReconciliation.getUnique_no()).setEntityStatus(EntityStatus.Update);
                            updateVoList.add((T) bizObjectMap.get(bankReconciliation.getUnique_no()));
                        }

                    }

                } else {
                    //有唯一号，数据库中无该唯一号数据，判断字段拼接唯一号
                    //本方账号+交易日期+交易时间+金额+方向+流水号+对方账号+对方户名
                    String newUniquNo = formatConctaInfoBankReconciliation(bankReconciliation);;
                    // 数据库中不存在该字段拼接唯一号
                    if(!bizObjectMap.containsKey(newUniquNo)){
                        bankReconciliation.setConcat_info(newUniquNo);
                        if(!(Objects.nonNull(bankReconciliation.get("is_refund")) && "3".equals(bankReconciliation.get("is_refund")))){
                            insertBizObjects.add((T) bankReconciliation);
                        }
                    } else {
                        // 数据库中存在该字段拼接唯一号，更新唯一号
                        if((Objects.nonNull(bankReconciliation.get("is_refund")) && "3".equals(bankReconciliation.get("is_refund")))){
                            bizObjectMap.get(newUniquNo).setEntityStatus(EntityStatus.Delete);
                            deleteVoList.add((T) bizObjectMap.get(newUniquNo));
                        }else {
                            // 数据库中存在该字段拼接唯一号,且库中唯一号为空，更新唯一号；否则入库
                            if(StringUtils.isEmpty(bizObjectMap.get(newUniquNo).get("unique_no"))){
                                bizObjectMap.get(newUniquNo).set("unique_no", bankReconciliation.getUnique_no());
                                bizObjectMap.get(newUniquNo).setEntityStatus(EntityStatus.Update);
                                updateVoList.add((T) bizObjectMap.get(newUniquNo));
                            } else {
                                insertBizObjects.add((T) bankReconciliation);
                            }
                        }

                    }
                }
            }
        }
        // 更新流水号，更新余额
        MetaDaoHelper.update(BankReconciliation.ENTITY_NAME, updateVoList);
        //流水状态=“已作废”，则直接删除该笔交易流水及对应的银行流水认领的数据
        // MetaDaoHelper.delete(BankReconciliation.ENTITY_NAME, deleteVoList);
        yqlInvalidateService.refundDeleteByBizObj(deleteVoList);
        return insertBizObjects;
    }

    private <T extends BizObject> List<T> checkBankElectronicReceipt(List<T> downloadData, Short checkRepeatDataBillType) throws Exception{
        // 银企联下载数据去重并根据uniquNo查询
        // 1，去重后数据
        List<BankElectronicReceipt> bankElectronicReceipts = new ArrayList<>();
        Set<String> uniquNos = new HashSet<>();
        Set<String> newUniquNos = new HashSet<>();
        for(BizObject bizObject : downloadData){
            BankElectronicReceipt bankElectronicReceipt = (BankElectronicReceipt) bizObject;
            if(StringUtils.isEmpty(bankElectronicReceipt.getUniqueCode())){
                // 流水号+交易日期+交易时间+金额+方向+本方账号
                String newUniquNo = bankElectronicReceipt.getEnterpriseBankAccount()
                        + DateUtils.convertToStr(bankElectronicReceipt.getTranDate(), "yyyy-MM-dd")
                        + DateUtils.convertToStr(bankElectronicReceipt.getTranTime(), "yyyy-MM-dd HH:mm:ss")
                        +bankElectronicReceipt.getTran_amt()
                        +bankElectronicReceipt.getDc_flag().getValue()
                        +bankElectronicReceipt.getBankseqno();
                if(!newUniquNos.contains(newUniquNo)){
                    newUniquNos.add(newUniquNo);
                    bankElectronicReceipts.add(bankElectronicReceipt);
                }
            }else {
                if(!uniquNos.contains(bankElectronicReceipt.getUniqueCode())){
                    uniquNos.add(bankElectronicReceipt.getUniqueCode());
                    bankElectronicReceipts.add(bankElectronicReceipt);
                }
            }
        }
        uniquNos.addAll(newUniquNos);
        // 根据uniquNO查询数据库数据
        Map<String, BizObject> bizObjectMap = getExistDataByUniquNo(uniquNos, checkRepeatDataBillType);
        // 验重操作
        List<T> insertBizObjects = new ArrayList<>(); // 入库列表
//        List<BizObject> updateVoList = new ArrayList<>(); // 更新流水号/余额列表
        for(BankElectronicReceipt bankElectronicReceipt : bankElectronicReceipts){
            if(StringUtils.isEmpty(bankElectronicReceipt.getUniqueCode())){
                // 无唯一号
                String newUniquNo = bankElectronicReceipt.getEnterpriseBankAccount()
                        + DateUtils.convertToStr(bankElectronicReceipt.getTranDate(), "yyyy-MM-dd")
                        + DateUtils.convertToStr(bankElectronicReceipt.getTranTime(), "yyyy-MM-dd HH:mm:ss")
                        +bankElectronicReceipt.getTran_amt()
                        +bankElectronicReceipt.getDc_flag().getValue()
                        +bankElectronicReceipt.getBankseqno();
                if(!bizObjectMap.containsKey(newUniquNo)){
                    // 数据库中不存在该唯一号数据，直接入库
                    insertBizObjects.add((T) bankElectronicReceipt);
                }
            }else {
                // 有唯一号但数据库中没有数据
                if(!bizObjectMap.containsKey(bankElectronicReceipt.getUniqueCode())){
                    // 数据库中不存在该唯一号数据，直接入库
                    insertBizObjects.add((T) bankElectronicReceipt);
                }
            }
        }
//        // 更新流水号，更新余额
//        CommonSaveUtils.updateBankReconciliation(updateVoList);

        return insertBizObjects;
    }

    /**
     * 根据uniquNO查询数据库数据 组装map
     * @param uniquNos
     * @param checkRepeatDataBillType
     * @return
     * @throws Exception
     */
    private Map<String, BizObject> getExistDataByUniquNo(Set<String> uniquNos, Short checkRepeatDataBillType) throws Exception {
        //, 根据uniqueNo查询
        List<BizObject> bizObjects = new ArrayList<>();
        Map<String, BizObject> bizObjectMap = new HashMap<>();
        QuerySchema schema = QuerySchema.create();
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        schema.addCondition(conditionGroup);
        schema.distinct();
        if(EventType.BankDealDetail.getValue() == checkRepeatDataBillType){
            queryDataByCondition(uniquNos, checkRepeatDataBillType, bizObjects, bizObjectMap);
        }else if(EventType.CashMark.getValue() == checkRepeatDataBillType){
            queryDataByCondition(uniquNos, checkRepeatDataBillType, bizObjects, bizObjectMap);
        }else if(EventType.BankElectronicReceipt.getValue() == checkRepeatDataBillType){
            conditionGroup.appendCondition(QueryCondition.name("uniqueCode").in(uniquNos));
            schema.addSelect(" id,bankseqno,trandate,trantime,tran_amt,dc_flag,enterprisebankaccount,uniqueCode ");
            // 数据库中存在的数据
            bizObjects = MetaDaoHelper.queryObject(BankElectronicReceipt.ENTITY_NAME, schema, null);
            for(BizObject bizObject : bizObjects){
                BankElectronicReceipt bankElectronicReceipt = (BankElectronicReceipt) bizObject;
                bizObjectMap.put(bankElectronicReceipt.getUniqueCode(), bankElectronicReceipt);
            }
        }

        return bizObjectMap;
    }

    /**
     * 组装查询数据的的参数并整理返回的数据
     * @param uniqueNos 查重要素
     * @param checkRepeatDataBillType 操作类型
     * @param bizObjectMap 存储结果的map
     * @param <> 实际类型，必须是BizObject的子类且具有getUnique_no和getConcat_info方法
     */
    private static void queryDataByCondition(Set<String> uniqueNos, Short checkRepeatDataBillType,List<BizObject> bizObjects, Map<String, BizObject> bizObjectMap) throws Exception {
        // unique_no和concat_info 分开查询，保证走索引
        // unique_no 分开查询
        QuerySchema schema = QuerySchema.create();
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        schema.addCondition(conditionGroup);
        schema.distinct();
        // concat_info 分开查询
        QuerySchema concatInfoSchema = QuerySchema.create();
        QueryConditionGroup concatInfoConditionGroup = new QueryConditionGroup(ConditionOperator.and);
        concatInfoSchema.addCondition(concatInfoConditionGroup);
        concatInfoSchema.distinct();
        String smartClassify = AppContext.getEnvConfig("cmp.checkrepeat.mode","1");
        if("1".equals(smartClassify)){
            // unique_no和concat_info 分开查询，保证走索引
            conditionGroup.addCondition(QueryConditionGroup.and(QueryCondition.name("unique_no").in(uniqueNos)));
            concatInfoConditionGroup.addCondition(QueryConditionGroup.and(QueryCondition.name("concat_info").in(uniqueNos)));
        }else{
            conditionGroup.addCondition(QueryConditionGroup.and(QueryCondition.name("concat_info").in(uniqueNos)));
        }
        // 数据库中存在unique_no的数据
        queryDetailData(checkRepeatDataBillType, schema, bizObjects);
        // 数据库中存在concat_info的数据
        queryDetailData(checkRepeatDataBillType, concatInfoSchema, bizObjects);
        // 按照unique_no和concat_info整理数据到bizObjectMap中
        collectByUniqueNoAndConcatInfo(bizObjects, bizObjectMap);
    }

    /**
     * 按照unique_no和concat_info整理数据到bizObjectMap中
     * @param bizObjects 数据列表
     * @param bizObjectMap 存储结果的map
     * @param <T> 实际类型，必须是BizObject的子类且具有getUnique_no和getConcat_info方法
     */
    private static <T extends BizObject> void collectByUniqueNoAndConcatInfo(
            List<T> bizObjects, Map<String, BizObject> bizObjectMap) {
        String uniqueNo = null;
        String concatInfo = null;
        for (T bizObject : bizObjects) {
            if (bizObject instanceof BankDealDetail) {
                BankDealDetail item = (BankDealDetail) bizObject;
                uniqueNo = item.getUnique_no();
                concatInfo = item.getConcat_info();
            } else if (bizObject instanceof BankReconciliation) {
                BankReconciliation item = (BankReconciliation) bizObject;
                uniqueNo = item.getUnique_no();
                concatInfo = item.getConcat_info();
            } else {
                continue; // 忽略不支持的类型
            }
            if (!StringUtils.isEmpty(uniqueNo)) {
                bizObjectMap.put(uniqueNo, bizObject);
            }
            if (!StringUtils.isEmpty(concatInfo)) {
                bizObjectMap.put(concatInfo, bizObject);
            }
        }
    }

    /**
     * 获取数据库中存在的数据
     * @param schema
     * @param bizObjects
     * @return
     * @throws Exception
     */
    private static void queryDetailData(Short checkRepeatDataBillType,QuerySchema schema, List<BizObject> bizObjects) throws Exception {
        String selectFields = null;
        List<BizObject> fieldBizObjects = new ArrayList<>();
        if (EventType.BankDealDetail.getValue() == checkRepeatDataBillType) {
            selectFields = " id,bankseqno,tranDate,tranTime,tran_amt,dc_flag,enterpriseBankAccount,acctbal,unique_no,concat_info,to_acct_no,to_acct_name,to_acct_bank,remark ";
            schema.addSelect(selectFields);
            fieldBizObjects = MetaDaoHelper.queryObject(BankDealDetail.ENTITY_NAME, schema, null);
        } else if (EventType.CashMark.getValue() == checkRepeatDataBillType) {
            selectFields = " id,bank_seq_no,tran_date,tran_time,tran_amt,dc_flag,bankaccount,acct_bal,unique_no,concat_info,to_acct_no,to_acct_name,to_acct_bank,ispublish,associationstatus,currency,remark,to_acct_bank_name";
            schema.addSelect(selectFields);
            fieldBizObjects = MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME, schema, null);
        }
        if (CollectionUtils.isNotEmpty(fieldBizObjects)) {
            bizObjects.addAll(fieldBizObjects);
        }
    }

    /**
     * 为防止篡改8要素，单独封装一个方法
     * @param bankRecord
     */
    public void fillBankReconciliationConcatInfo(BankReconciliation bankRecord){
        String concatInfo = formatConctaInfoBankReconciliation(bankRecord);
        bankRecord.fillConcatInfo(concatInfo);
    }

    /**
     * 为防止篡改8要素，单独封装一个方法
     * @param bankDealDetail
     */
    public void fillBankDealDetailConcatInfo(BankDealDetail bankDealDetail){
        String concatInfo = formatConctaInfoBankDealDetail(bankDealDetail);
        bankDealDetail.fillConcatInfo(concatInfo);
    }

    /**
     * 格式化多要素信息
     * @param bankDealDetail
     * @return
     */
    @Override
    public String formatConctaInfoBankDealDetail(BankDealDetail bankDealDetail){

        Date tran_date = bankDealDetail.getTranDate();
        String tran_dateStr = null;
        if(tran_date != null){
            tran_dateStr = DateUtils.convertToStr(tran_date, "yyyy-MM-dd HH:mm:ss");
        }
        Date tran_time = bankDealDetail.getTranTime();
        String tran_timeStr = null;
        if(bankDealDetail.getTranTime() != null){
            tran_timeStr = DateUtils.convertToStr(tran_time, "yyyy-MM-dd HH:mm:ss");
        }
        String concatInfo = "";
        if(StringUtils.isEmpty(bankDealDetail.getBankseqno())){
            concatInfo = bankDealDetail.getEnterpriseBankAccount()+"|"
                    + tran_dateStr+"|"
                    + tran_timeStr+"|"
                    + bankDealDetail.getTran_amt().setScale(2,BigDecimal.ROUND_HALF_UP)+"|"
                    +bankDealDetail.getDc_flag().getValue()+"|"
                    +"null|"
                    +bankDealDetail.getTo_acct_no()+"|"
                    +bankDealDetail.getTo_acct_name();
        }else {
            concatInfo = bankDealDetail.getEnterpriseBankAccount()+"|"
                    + tran_dateStr+"|"
                    + tran_timeStr+"|"
                    + bankDealDetail.getTran_amt().setScale(2,BigDecimal.ROUND_HALF_UP)+"|"
                    +bankDealDetail.getDc_flag().getValue()+"|"
                    +bankDealDetail.getBankseqno()+"|"
                    +bankDealDetail.getTo_acct_no()+"|"
                    +bankDealDetail.getTo_acct_name();
        }
        log.error("=======formatConctaInfoBankDealDetail========concatInfo:"+concatInfo);
        return concatInfo;
    }

    /**
     * 格式化多要素信息
     * @param bankReconciliation
     * @return
     */
    @Override
    public String formatConctaInfoBankReconciliation(BankReconciliation bankReconciliation){

        Date tran_date = bankReconciliation.getTran_date();
        String tran_dateStr = null;
        if(tran_date != null){
            tran_dateStr = DateUtils.convertToStr(tran_date, "yyyy-MM-dd HH:mm:ss");
        }
        Date tran_time = bankReconciliation.getTran_time();
        String tran_timeStr = null;
        if(tran_time != null){
            tran_timeStr = DateUtils.convertToStr(tran_time, "yyyy-MM-dd HH:mm:ss");
        }
        String concatInfo = "";
        // 流水号为空
        if(StringUtils.isEmpty(bankReconciliation.getBank_seq_no())){
            concatInfo = bankReconciliation.getBankaccount()+"|"
                    + tran_dateStr+"|"
                    + tran_timeStr+"|"
                    + bankReconciliation.getTran_amt().setScale(2,BigDecimal.ROUND_HALF_UP)+"|"
                    +bankReconciliation.getDc_flag().getValue()+"|"
                    +"null|"
                    +bankReconciliation.getTo_acct_no()+"|"
                    +bankReconciliation.getTo_acct_name();
        } else {
            // 流水号不为空
            concatInfo = bankReconciliation.getBankaccount()+"|"
                    + tran_dateStr+"|"
                    + tran_timeStr+"|"
                    + bankReconciliation.getTran_amt().setScale(2,BigDecimal.ROUND_HALF_UP)+"|"
                    +bankReconciliation.getDc_flag().getValue()+"|"
                    +bankReconciliation.getBank_seq_no()+"|"
                    +bankReconciliation.getTo_acct_no()+"|"
                    +bankReconciliation.getTo_acct_name();
        }
        log.error("=======formatConctaInfoBankReconciliation========concatInfo:"+concatInfo);
        return concatInfo;
    }

    /**
     * 格式化多要素信息
     * 构造4要素 本方账号|交易日期|交易金额|交易方向 + 疑重规则配置的字段
     * bankaccount,tran_date,tran_amt,dc_flag
     * @param bankReconciliation 流水集合
     * @return String 疑重匹配串
     */
    public String formatConcatInfoDefineFactorsBankReconciliation(BankReconciliation bankReconciliation) {
        // 拼装疑重规则字段
        StringBuilder concatInfoDefine = new StringBuilder();
        // 增加疑重要素
        for (String repeatFactor: CtmDealDetailCheckMayRepeatUtils.repeatFactors) {
            Object repeatFactorValue = bankReconciliation.get(repeatFactor);
            if (null == repeatFactorValue) {
                concatInfoDefine.append("null|");
                continue;
            }
            try {
                if("tran_date".equals(repeatFactor)){
                    repeatFactorValue = DateUtils.convertToStr(bankReconciliation.getTran_date(), "yyyy-MM-dd HH:mm:ss");
                    concatInfoDefine.append(repeatFactorValue).append("|");
                    continue;
                }
                if("tran_time".equals(repeatFactor)){
                    repeatFactorValue = DateUtils.convertToStr(bankReconciliation.getTran_time(), "yyyy-MM-dd HH:mm:ss");
                    concatInfoDefine.append(repeatFactorValue).append("|");
                    continue;
                }
            } catch (Exception e) {
                log.error("=======formatConcatInfoDefineFactorsBankReconciliation==疑重规则要素,日期时间错误:"+repeatFactor);
                continue;
            }
            if("tran_amt".equals(repeatFactor)){
                repeatFactorValue = String.valueOf(bankReconciliation.getTran_amt().setScale(2, RoundingMode.HALF_UP));
                concatInfoDefine.append(repeatFactorValue).append("|");
                continue;
            }
            if("dc_flag".equals(repeatFactor)){
                repeatFactorValue = String.valueOf(bankReconciliation.getDc_flag().getValue());
                concatInfoDefine.append(repeatFactorValue).append("|");
                continue;
            }
            concatInfoDefine.append(repeatFactorValue).append("|");
        }
        concatInfoDefine.deleteCharAt(concatInfoDefine.length() - 1);

        log.error("=======formatConcatInfoDefineFactorsBankReconciliation========疑重规则要素:"+concatInfoDefine);
        return concatInfoDefine.toString();
    }

    /**
     * 4要素验重，更新返回流水状态、增加不存在流水进布隆过滤器
     * @param bankReconciliations 获取的流水集合
     * @param enterpriseInfo 查询流水的对象
     */
    @Override
    public void deal4FactorsBankDealDetail(List<BankReconciliation> bankReconciliations,Map<String, Object> enterpriseInfo) throws Exception {
        // 判断开启疑重开关
        if (!CtmDealDetailCheckMayRepeatUtils.isRepeatCheck) {
            return;
        }
        // 没有新增数据，不进行疑重处理
        if (CollectionUtils.isEmpty(bankReconciliations)) {
            return;
        }
        // 开始日期为当前-默认天数（35天）
        Date startDate = DateUtil.beforeDays(new Date(), CtmDealDetailCheckMayRepeatUtils.selectDayCount);

        // 疑重处理 先布隆 如果是疑重的再进行数据库校验
        // 布隆启动开关，启动后，先布隆，再数据库校验
        if (CtmDealDetailCheckMayRepeatUtils.isUseCuckoo) {
            getIsRepeatQueryCuckooFilter(bankReconciliations);
        }
        // 不启用布隆，直接数据库校验疑重
        getIsRepeatQuerySelectDate(bankReconciliations,startDate);
    }

    @Override
    public void checkRepeatInfo(BankReconciliation bankReconciliation) {
        if(bankReconciliation.getEntityStatus() == EntityStatus.Unchanged){
            return;
        }
        // 流水的【疑重标识】【正常】和【确认正常】不进行疑重
        if (bankReconciliation.getIsRepeat() != null && (bankReconciliation.getIsRepeat().intValue() == BankDealDetailConst.REPEAT_INIT || bankReconciliation.getIsRepeat().intValue() == BankDealDetailConst.REPEAT_NORMAL)) {
            return;
        }
        //开启疑重后的逻辑
        try{
            if (CtmDealDetailCheckMayRepeatUtils.isRepeatCheck) {
                //结算中心内部账户不进行判断;
                EnterpriseParams enterpriseParams = new EnterpriseParams();
                enterpriseParams.setId(bankReconciliation.getBankaccount());
                List<EnterpriseBankAcctVO> bankAccounts = baseRefRpcService.queryEnterpriseBankAccountByCondition(enterpriseParams);
                if (CollectionUtils.isNotEmpty(bankAccounts) && ValueUtils.isNotEmptyObj(bankAccounts.get(0).getAcctopentype()) && bankAccounts.get(0).getAcctopentype().equals(1)) {
                    //结算中心内部账户时： 疑重标识=正常
                    bankReconciliation.setIsRepeat((short) BankDealDetailConst.REPEAT_INIT);
                } else {
                    repeatCheck(bankReconciliation);
                }
            }else{
                bankReconciliation.setIsRepeat((short) BankDealDetailConst.REPEAT_INIT);
            }
        }catch (Exception e){
            log.error("疑重判定逻辑异常",e);
            bankReconciliation.setIsRepeat((short) BankDealDetailConst.REPEAT_INIT);
        }
    }

    /**
     * 根据数据库查询的数据进行判断疑似重复
     * 按照增加4要素+疑重规则字段的数据进行判断
     * @param bankReconciliations 获取的流水集合
     */
    private void getIsRepeatQuerySelectDate(List<BankReconciliation> bankReconciliations,Date startDate) {
        if (CtmDealDetailCheckMayRepeatUtils.isUseCuckoo) {
            //数据库查询只处理布隆过滤器确认为疑似重复的银行流水数据
            bankReconciliations = bankReconciliations.stream().filter(bankReconciliation -> bankReconciliation.getIsRepeat() == CtmDealDetailCheckMayRepeatUtils.MAY_REPEAT).collect(Collectors.toList());
        }
        // 校验全部数据
        Set<String> concatInfoDefines = Collections.emptySet();
        QueryConditionGroup group = new QueryConditionGroup(ConditionOperator.and);
        //查询数据库时增加一个日期限制,防止查询全库数据
        group.addCondition(QueryCondition.name("tran_date").between(startDate,new Date()));
//        group.addCondition(QueryCondition.name("tran_date").egt(startDate));

        //避免CmpQuerySchemaExecutorPlugin 过滤 拼接疑重过滤条件
        QueryConditionGroup repeatGroup = new QueryConditionGroup(ConditionOperator.or);
        repeatGroup.addCondition(QueryCondition.name("isrepeat").is_null());
        repeatGroup.addCondition(QueryCondition.name("isrepeat").is_not_null());
        group.addCondition(repeatGroup);

        QueryConditionGroup orGroup = new QueryConditionGroup(ConditionOperator.or);
        //拼接一个大的四要素查询逻辑,每个数据通过 or 拼接 (tran_date=? and bankaccount=? ...) or (tran_date=? and bankaccount=? ...)
        for (BankReconciliation reconciliation : bankReconciliations) {
            // 遍历需要新增入库的流水集合
            QueryConditionGroup checkGroup = new QueryConditionGroup();
            if(EntityStatus.Update == reconciliation.getEntityStatus()){
                checkGroup.addCondition(QueryCondition.name("id").not_eq(reconciliation.getId()));
            }
            for (String repeatFactor : CtmDealDetailCheckMayRepeatUtils.repeatFactors) {
                Object repeatFactorValue = reconciliation.get(repeatFactor);
                if (null == repeatFactorValue) {
                    checkGroup.addCondition(QueryCondition.name(repeatFactor).is_null());
                } else {
                    checkGroup.addCondition(QueryCondition.name(repeatFactor).eq(repeatFactorValue));
                }
            }
            orGroup.addCondition(checkGroup);
        }
        group.addCondition(orGroup);
        QuerySchema querySchema = QuerySchema.create().addSelect(CtmDealDetailCheckMayRepeatUtils.repeatAddFactors).addCondition(group);
        try {
            //将组装的数据拼接出疑重要素,放到Set集合中
            List<BankReconciliation> repeatList = MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME, querySchema, null);
            if(CollectionUtils.isNotEmpty(repeatList)){
                concatInfoDefines = repeatList.stream().map(this::formatConcatInfoDefineFactorsBankReconciliation).collect(Collectors.toSet());
            }
        }catch (Exception e){
            log.error("===初始化：=======bankReconciliation疑重4要素数据失败",e);
        }
        if(CollectionUtils.isNotEmpty(concatInfoDefines)){
            //concatInfoDefines 不为空则说明数据库存在存在疑重数据
            Set<String> finalConcatInfoDefines = concatInfoDefines;
            bankReconciliations.forEach(reconciliation -> {
                String concatInfoDefine = this.formatConcatInfoDefineFactorsBankReconciliation(reconciliation);
                // 四元素+疑重规则元素进行疑重：库内数据有设置疑重规则但没有包含拉取的流水为不重复
                if (finalConcatInfoDefines.contains(concatInfoDefine)) {
                    // 存在的数据进行更新疑重状态为【疑似重复】
                    reconciliation.setIsRepeat(CtmDealDetailCheckMayRepeatUtils.MAY_REPEAT);
                }else{
                    reconciliation.setIsRepeat((short) BankDealDetailConst.REPEAT_INIT);
                }
            });
        }else{
            //concatInfoDefines 为空则说明数据库不存在存在疑重数据（有可能不在当前时间范围内，也有可能数据已经被删除）将疑重标识修改为正常
            bankReconciliations.forEach(reconciliation -> {
                reconciliation.setIsRepeat((short) BankDealDetailConst.REPEAT_INIT);
            });
        }
    }

    /**
     * 根据布隆过滤器进行判断疑似重复
     * 4要素按照布隆过滤，如果重复后再按照增加疑重规则数据判断
     * @param bankReconciliations 获取的流水集合
     */
    private void getIsRepeatQueryCuckooFilter(List<BankReconciliation> bankReconciliations) {
        // 过滤器为null，不进行疑重处理
        if (null == this.cmpCuckooFilters) {
            return;
        }
        // 检查过滤器是否有初始化，如初始化后，需要重新填充数据
        if (!this.cmpCuckooFilters.initDataState.get()) {
            try {
                this.getFullDataSetCuckoo();
                this.cmpCuckooFilters.initDataState.getAndSet(true);
            } catch (Exception e) {
                log.error("===初始化：=======bankReconciliation疑重4要素数据失败");
                return;
            }
        }
        // 遍历需要新增入库的流水集合
        for (BankReconciliation reconciliation : bankReconciliations) {
            String concatInfoDefine = reconciliation.getConcat_info_define();
            if (StringUtils.isEmpty(concatInfoDefine)) {
                concatInfoDefine = this.formatConcatInfoDefineFactorsBankReconciliation(reconciliation);
            }
            // 通过过滤器校验四要素（自定义要素）验重
            if (!this.cmpCuckooFilters.mightContain(concatInfoDefine)) {
                // 增加不存在的数据进过滤器
                this.cmpCuckooFilters.putValue(concatInfoDefine);
                continue;
            }
            // 存在的数据进行更新疑重状态为【疑似重复】
            reconciliation.setIsRepeat(CtmDealDetailCheckMayRepeatUtils.MAY_REPEAT);
        }
    }

    /*
     * 查询流水表数据并进行布隆过滤器存储
     * type 1 布隆 2 Db
     */
    private Set<String> getFullDataSetCuckoo() throws Exception {
        // 全数据查询
        List<BankReconciliation> bizObjects;
        QuerySchema schema = QuerySchema.create();
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        // 默认查询35天的数据
        String startDate = DateUtil.getDateStr(DateUtil.beforeDays(new Date(), CtmDealDetailCheckMayRepeatUtils.selectDayCount));
        String endDate = String.valueOf(DateUtil.formatDate2String(new Date()));
        conditionGroup.appendCondition(QueryCondition.name("tran_date").between(startDate, endDate));
        // 增加查询全部疑重数据,为了避免被CmpQuerySchemaExecutorPlugin拦截，使or的方式
        QueryConditionGroup orIsrepeat = QueryConditionGroup.or(QueryCondition.name("isrepeat").is_null(),QueryCondition.name("isrepeat").is_not_null());
        conditionGroup.addCondition(orIsrepeat);
        schema.addCondition(conditionGroup);
        schema.distinct();
        schema.addSelect(CtmDealDetailCheckMayRepeatUtils.repeatAddFactors);
        // 数据库中存在的数据
        bizObjects = MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME, schema, null);
        Set<String> concatInfoDefines = new HashSet<>();
        for(BankReconciliation bankReconciliation : bizObjects){
            String concatInfoDefine = this.formatConcatInfoDefineFactorsBankReconciliation(bankReconciliation);
            if (concatInfoDefines.contains(concatInfoDefine)) {
                continue;
            }
            concatInfoDefines.add(concatInfoDefine);
            cmpCuckooFilters.putValue(concatInfoDefine);
        }
        return concatInfoDefines;
    }

    /**
     * 拼装疑重规则字段
     */
    private void repeatCheck(BankReconciliation bankReconciliation) throws Exception {
        QueryConditionGroup group = new QueryConditionGroup();
        boolean modifyFlag = false;
        //导入(com.yonyoucloud.fi.cmp.bankreconciliation.processor.BankreconciliationImportEventExtendProcessor)或者通过API过来的数据如果疑重标识字段有值则不进行疑重规则判断
        if(EntityStatus.Insert == bankReconciliation.getEntityStatus() && bankReconciliation.getDataOrigin() == DateOrigin.Created && bankReconciliation.getIsRepeat() != null){
            return;
        }
        if (EntityStatus.Update == bankReconciliation.getEntityStatus()) {
            //判断该数据是否被修改
            BankReconciliation oldData = this.queryById(bankReconciliation.getId());
            //更新的时候判断当前单据的疑重要素字段是否有更新,如果没有更新不会更新疑重标识字段
            for (String repeatFactor : CtmDealDetailCheckMayRepeatUtils.repeatFactors) {

                if("tran_date".equals(repeatFactor)){
                    String repeatFactorValue = DateUtils.convertToStr(bankReconciliation.getTran_date(), "yyyy-MM-dd");
                    String oldValue =  DateUtils.convertToStr(oldData.getTran_date(), "yyyy-MM-dd");
                    if (!Objects.equals(oldValue, repeatFactorValue)) {
                        modifyFlag = true;
                        break;
                    }
                }else if("tran_time".equals(repeatFactor)){
                    String repeatFactorValue = DateUtils.convertToStr(bankReconciliation.getTran_time(), "yyyy-MM-dd HH:mm:ss");
                    String oldValue =  DateUtils.convertToStr(oldData.getTran_time(), "yyyy-MM-dd");
                    if (!Objects.equals(oldValue, repeatFactorValue)) {
                        modifyFlag = true;
                        break;
                    }
                }else{
                    Object repeatFactorValue = bankReconciliation.get(repeatFactor);
                    Object oldValue = oldData.get(repeatFactor);
                    // Objects.equals 判断时如果两方内容一致，但是类型不一致时会认定两个值不相等；所以将其转为String后再进行比较
                    if(repeatFactorValue == null && oldValue == null){
                        modifyFlag = false;
                    }else if(repeatFactorValue != null && oldValue != null){
                        if(!String.valueOf(repeatFactorValue).equals(String.valueOf(oldValue))){
                            modifyFlag = true;
                            break;
                        }
                    }else{
                        modifyFlag = true;
                        break;
                    }
                }

            }
            if (!modifyFlag) {
                log.info("当前单据的疑重要素字段未发生变更,不更新疑重标识字段");
                return;
            } else {
                group.addCondition(QueryCondition.name("id").not_eq(bankReconciliation.getId()));
            }
        }
        //初始设置疑重标识字段为正常
        bankReconciliation.setIsRepeat((short) BankDealDetailConst.REPEAT_INIT);
        //疑重判断
        Map<String,Object> enterpriseInfo = new HashMap<>();
        enterpriseInfo.put("startDate",DateUtils.convertToStr(bankReconciliation.getTran_date(),"yyyy-MM-dd"));
        deal4FactorsBankDealDetail(Collections.singletonList(bankReconciliation),enterpriseInfo);
    }

    private BankReconciliation queryById(Long id) throws Exception {
        QueryConditionGroup repeatGroup = new QueryConditionGroup(ConditionOperator.or);
        repeatGroup.addCondition(QueryCondition.name("isrepeat").is_null());
        repeatGroup.addCondition(QueryCondition.name("isrepeat").is_not_null());

        QueryConditionGroup group = new QueryConditionGroup();
        group.addCondition(QueryCondition.name("id").eq(id));
        group.addCondition(repeatGroup);

        QuerySchema querySchema = QuerySchema.create().addSelect(CtmDealDetailCheckMayRepeatUtils.repeatAddFactors).addCondition(group);
        List<BankReconciliation> list = MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME,querySchema,null);
        if(CollectionUtils.isEmpty(list)){
            throw new CtmException(new CtmErrorCode("033-502-101609"), MessageUtils.getMessage(InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180585",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054007D5", "单据不存在或已被删除!") /* "单据不存在或已被删除!" */) /* "单据不存在或已被删除!" */));
        }
        return list.get(0);
    }

    @Override
    public boolean checkCurrentBatchIsRepeate(ImportBatchData importBatchData) {
        String repeat = AppContext.getEnvConfig("available.memory.repeat","true");
        if(!repeat.equals("true")){
            return true;
        }
        List<ImportSingleData> data = importBatchData.getData();
        Map<String,ImportSingleData> repeatMap = new HashMap<>();
        data.stream().forEach(e->{
            Map<String,Object> map = e.getData();
            BankReconciliation bankReconciliation = Objectlizer.convert(map, BankReconciliation.ENTITY_NAME);
            String concatInfo = this.formatConctaInfoBankReconciliation(bankReconciliation);
            if(repeatMap.containsKey(concatInfo)){
                Set<ExcelErrorLocation> errors = e.getErrors();
                ExcelErrorLocation excelErrorLocation = new ExcelErrorLocation();
                excelErrorLocation.setRow(e.getRowIndex());
                excelErrorLocation.setErrorMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054007D6", "流水八要素去重") /* "流水八要素去重" */);
                errors.add(excelErrorLocation);
            }else{
                repeatMap.put(concatInfo,e);
            }
        });
        return false;
    }
}
