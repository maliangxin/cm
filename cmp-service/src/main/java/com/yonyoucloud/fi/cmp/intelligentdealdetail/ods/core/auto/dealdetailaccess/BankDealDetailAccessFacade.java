package com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailaccess;

import com.google.common.collect.Maps;
import com.yonyou.iuap.yms.cache.YMSRedisTemplate;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.bankunion.BankUnionRequestBodyRecord;
import com.yonyoucloud.fi.cmp.bankunion.BankUnionResponse;
import com.yonyoucloud.fi.cmp.bankunion.BankUnionResponseRecord;
import com.yonyoucloud.fi.cmp.cmpentity.AssociationStatus;
import com.yonyoucloud.fi.cmp.cmpentity.ReFundType;
import com.yonyoucloud.fi.cmp.enums.SerialdealendState;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.consts.BankDealDetailConst;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.consts.DealDetailEnumConst;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailaccess.impl.*;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailaccess.model.BankDealDetailModel;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailaccess.model.BankDealDetailODSModel;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailaccess.model.NoticeBankDealDetailModel;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailaccess.model.YQLDataAccessModel;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetaildao.IBankDealDetailAccessDao;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.exception.BankDealDetailException;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.utils.DealDetailUtils;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @Author guoyangy
 * @Date 2024/6/08:55
 * @Description 银行流水接入门面类
 * @Version 1.0
 */
@Service
@Slf4j
public class BankDealDetailAccessFacade implements ApplicationContextAware {
    public static YQLDealDetailAccessImpl yqlDealDetailAccess;
    public static ManualDealDetailAccessImpl manualDealDetailAccess;
    public static SignalDealDetailAccessImpl signalDealDetailAccess;
    public static SignalDealDetailAccessImplNew signalDealDetailAccessNew;
    public static NoticeDealDetailAccessImpl noticeDealDetailAccess;

    /**
     * 接入从银企联拉取的流水信息
     *
     * @param dataAccessModel
     */
    public void dealDetailAccessByYQL(YQLDataAccessModel dataAccessModel) {
        if (null == dataAccessModel) {
            log.error("【银企联流水接入】,dataAccessModel=null，结束！");
            return;
        }
        long start = System.currentTimeMillis();
        try {
            yqlDealDetailAccess.dealDetailAccess(dataAccessModel);
            log.info("【银企联流水接入完成】traceid={},耗时{}ms", DealDetailUtils.getTraceId(), (System.currentTimeMillis() - start));
        } catch (Exception e) {
            DealDetailUtils.recordException(DealDetailUtils.getTraceId(), dataAccessModel.getRequestSeqNo(), e);
            throw e;
        }
    }

    /**
     * 接入手工流水信息
     *
     * @param bankReconciliationList
     */
    public String dealDetailAccessByManual(List<BankReconciliation> bankReconciliationList) {
        if (CollectionUtils.isEmpty(bankReconciliationList)) {
            log.error("【流水手工处理】待处理流水为空，处理结束");
            return com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400523", "【流水手工处理】待处理流水为空，处理结束") /* "【流水手工处理】待处理流水为空，处理结束" */;
        }
        String requestNo = UUID.randomUUID().toString();
        try {
            /**
             * step1:流水处理
             * */
            long start = System.currentTimeMillis();
            bankReconciliationList = bankReconciliationList.stream().distinct().collect(Collectors.toList());
            //已关联
            List<BankReconciliation> associationstatusList = bankReconciliationList.stream().filter(Objects::nonNull).filter(bankReconciliation -> bankReconciliation.getAssociationstatus() != null && bankReconciliation.getAssociationstatus() == AssociationStatus.Associated.getValue()).collect(Collectors.toList());
            if (!CollectionUtils.isEmpty(associationstatusList)) {
                String repeatBankSeqNos = associationstatusList.stream().filter(Objects::nonNull).filter(bankReconciliation -> StringUtils.isNotEmpty(bankReconciliation.getBank_seq_no())).map(item -> item.getBank_seq_no()).collect(Collectors.joining("】、【"));//@notranslate
                String message = com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400517", "交易流水号【") /* "交易流水号【" */ + repeatBankSeqNos + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400526", "】为已关联流水，不允许进行该操作，请检查！") /* "】为已关联流水，不允许进行该操作，请检查！" */;
                log.error(message);
                throw new BankDealDetailException(message);
            }
            //发布状态校验
            List<BankReconciliation> ispublishList = bankReconciliationList.stream().filter(Objects::nonNull).filter(bankReconciliation -> bankReconciliation.getIspublish() != null && bankReconciliation.getIspublish()).collect(Collectors.toList());
            if (!CollectionUtils.isEmpty(ispublishList)) {
                String repeatBankSeqNos = ispublishList.stream().filter(Objects::nonNull).filter(bankReconciliation -> StringUtils.isNotEmpty(bankReconciliation.getBank_seq_no())).map(item -> item.getBank_seq_no()).collect(Collectors.joining("】、【"));//@notranslate
                String message = com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400517", "交易流水号【") /* "交易流水号【" */ + repeatBankSeqNos + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400528", "】为发布状态流水，不允许进行该操作，请检查！") /* "】为发布状态流水，不允许进行该操作，请检查！" */;
                log.error(message);
                throw new BankDealDetailException(message);
            }
            //总账是否勾兑
            List<BankReconciliation> other_checkflagList = bankReconciliationList.stream().filter(Objects::nonNull).filter(bankReconciliation -> bankReconciliation.getOther_checkflag() != null && bankReconciliation.getOther_checkflag()).collect(Collectors.toList());
            if (!CollectionUtils.isEmpty(other_checkflagList)) {
                String repeatBankSeqNos = other_checkflagList.stream().filter(Objects::nonNull).filter(bankReconciliation -> StringUtils.isNotEmpty(bankReconciliation.getBank_seq_no())).map(item -> item.getBank_seq_no()).collect(Collectors.joining("】、【"));//@notranslate
                String message = com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400517", "交易流水号【") /* "交易流水号【" */ + repeatBankSeqNos + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540052B", "】为总账已勾兑流水，不允许进行该操作，请检查！") /* "】为总账已勾兑流水，不允许进行该操作，请检查！" */;
                log.error(message);
                throw new BankDealDetailException(message);
            }
            //日记账是否勾兑
            List<BankReconciliation> checkflagList = bankReconciliationList.stream().filter(Objects::nonNull).filter(bankReconciliation -> bankReconciliation.getCheckflag() != null && bankReconciliation.getCheckflag()).collect(Collectors.toList());
            if (!CollectionUtils.isEmpty(checkflagList)) {
                String repeatBankSeqNos = checkflagList.stream().filter(Objects::nonNull).filter(bankReconciliation -> StringUtils.isNotEmpty(bankReconciliation.getBank_seq_no())).map(item -> item.getBank_seq_no()).collect(Collectors.joining("】、【"));//@notranslate
                String message = com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400517", "交易流水号【") /* "交易流水号【" */ + repeatBankSeqNos + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540052D", "】为日记账已勾兑流水，不允许进行该操作，请检查！") /* "】为日记账已勾兑流水，不允许进行该操作，请检查！" */;
                log.error(message);
                throw new BankDealDetailException(message);
            }
            //疑重判定逻辑
            List<BankReconciliation> isrepeatList = bankReconciliationList.stream().filter(Objects::nonNull).filter(bankReconciliation -> bankReconciliation.getIsRepeat() != null && (bankReconciliation.getIsRepeat() == (short) BankDealDetailConst.REPEAT_DOUBT || bankReconciliation.getIsRepeat() == (short) BankDealDetailConst.REPEAT_CONFIRM)).collect(Collectors.toList());
            if (!CollectionUtils.isEmpty(isrepeatList)) {
                String repeatBankSeqNos = isrepeatList.stream().filter(Objects::nonNull).filter(bankReconciliation -> StringUtils.isNotEmpty(bankReconciliation.getBank_seq_no())).map(item -> item.getBank_seq_no()).collect(Collectors.joining("】、【"));//@notranslate
                String message = com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400517", "交易流水号【") /* "交易流水号【" */ + repeatBankSeqNos + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400516", "】为疑似重复/重复流水，不允许进行该操作，请检查！") /* "】为疑似重复/重复流水，不允许进行该操作，请检查！" */;
                log.error(message);
                throw new BankDealDetailException(message);
            }
            //退票
            List<BankReconciliation> refundstatusList = bankReconciliationList.stream().filter(Objects::nonNull).filter(bankReconciliation -> bankReconciliation.getRefundstatus() != null && bankReconciliation.getRefundstatus() == ReFundType.SUSPECTEDREFUND.getValue()).collect(Collectors.toList());
            if (!CollectionUtils.isEmpty(refundstatusList)) {
                String repeatBankSeqNos = refundstatusList.stream().filter(Objects::nonNull).filter(bankReconciliation -> StringUtils.isNotEmpty(bankReconciliation.getBank_seq_no())).map(item -> item.getBank_seq_no()).collect(Collectors.joining("】、【"));//@notranslate
                String message = com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400517", "交易流水号【") /* "交易流水号【" */ + repeatBankSeqNos + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540051B", "】为疑似退票流水，不允许进行该操作，请检查！") /* "】为疑似退票流水，不允许进行该操作，请检查！" */;
                log.error(message);
                throw new BankDealDetailException(message);
            }
            //流水处理完结状态
            List<BankReconciliation> serialdealendstateList = bankReconciliationList.stream().filter(Objects::nonNull).filter(bankReconciliation -> bankReconciliation.getSerialdealendstate() != null && bankReconciliation.getSerialdealendstate() == SerialdealendState.END.getValue()).collect(Collectors.toList());
            if (!CollectionUtils.isEmpty(serialdealendstateList)) {
                String repeatBankSeqNos = serialdealendstateList.stream().filter(Objects::nonNull).filter(bankReconciliation -> StringUtils.isNotEmpty(bankReconciliation.getBank_seq_no())).map(item -> item.getBank_seq_no()).collect(Collectors.joining("】、【"));//@notranslate
                String message = com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400517", "交易流水号【") /* "交易流水号【" */ + repeatBankSeqNos + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400520", "】为流水处理完结状态流水，不允许进行该操作，请检查！") /* "】为流水处理完结状态流水，不允许进行该操作，请检查！" */;
                log.error(message);
                throw new BankDealDetailException(message);
            }

            //手工辨识，不区分辨识状态，只区分单据状态,此处则区分单据状态
            int originCount = bankReconciliationList.size();
            BankDealDetailModel bankDealDetailModel = new BankDealDetailModel();
            bankDealDetailModel.setStreamResponseRecordList(bankReconciliationList);

            bankDealDetailModel.setRequestSeqNo(requestNo);
            //将数据存入到Redis
//            YMSRedisTemplate redisTemplate = AppContext.getBean(YMSRedisTemplate.class);
//            List<String> bankReconciliationIds = new ArrayList<>();
//            bankReconciliationList.stream().forEach(bankReconciliation -> {
//                String id = bankReconciliation.getId().toString();
//                bankReconciliationIds.add(id);
//                redisTemplate.opsForValue().setIfAbsent(id,"1",180, TimeUnit.SECONDS);
//            });

            manualDealDetailAccess.dealDetailAccess(bankDealDetailModel);
            log.error("【流水手工处理】完成，耗时{}ms,traceid={}", (System.currentTimeMillis() - start), DealDetailUtils.getTraceId());
            /**
             * step2:流水处理结果统计
             * */
            //定时器
            //processTimer(bankReconciliationIds,redisTemplate);

            IBankDealDetailAccessDao bankDealDetailAccessDao = AppContext.getBean(IBankDealDetailAccessDao.class);
            List<BankDealDetailODSModel> bankDealDetailODSModelList = bankDealDetailAccessDao.batchQueryODSByTranceidAndRequestseqno(DealDetailUtils.getTraceId(), requestNo);
            if (CollectionUtils.isEmpty(bankDealDetailODSModelList)) {
                throw new BankDealDetailException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400529", "ods流水数据为空") /* "ods流水数据为空" */);
            }
            if (bankDealDetailODSModelList.size() != originCount) {
                throw new BankDealDetailException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540052C", "ods记录流水和实际处理流水数量不一致") /* "ods记录流水和实际处理流水数量不一致" */);
            }
            List<Long> bankReconciliationIdList = bankDealDetailODSModelList.stream().map(BankDealDetailODSModel::getMainid).collect(Collectors.toList());
            //批量查询流水表
            List<BankReconciliation> bankReconciliations = bankDealDetailAccessDao.batchQueryBankReconciliationByIds(bankReconciliationIdList);
            if (CollectionUtils.isEmpty(bankReconciliations)) {
                throw new BankDealDetailException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540052E", "流水业务表未查询到流水信息") /* "流水业务表未查询到流水信息" */);
            }
            Map<Long, BankReconciliation> bankReconciliationMapNotFinish = bankReconciliations.stream().collect(Collectors.toMap(k -> k.getId(), item -> item));
            Map<Short, List<BankReconciliation>> bankReconciliationMap = bankReconciliations.stream().collect(Collectors.groupingBy(BankReconciliation::getProcessstatus));
            List<String> needUpdateBankReconciliationList = new ArrayList<>();
            //非终态：流水自动处理已完成,待手工处理
            Optional.ofNullable(bankReconciliationMap.get(DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_PROCESS_SUCC.getStatus())).ifPresent(reconciliationList -> {
                reconciliationList.stream().filter(Objects::nonNull).forEach(bankReconciliation -> {
                    String bank_seq_no = bankReconciliation.getBank_seq_no() != null ? bankReconciliation.getBank_seq_no() : "";
                    String message = com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400517", "交易流水号【") /* "交易流水号【" */ + bank_seq_no + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400519", "】,未匹配到有效的关联/生单/发布规则，需人工进行关联/生单/发布处理；") /* "】,未匹配到有效的关联/生单/发布规则，需人工进行关联/生单/发布处理；" */;
                    needUpdateBankReconciliationList.add(message);
                    bankReconciliationMapNotFinish.remove(bankReconciliation.getId());
                });
            });
            Optional.ofNullable(bankReconciliationMap.get(DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_PROCESS_FINISH.getStatus())).ifPresent(reconciliationList -> {
                reconciliationList.stream().filter(Objects::nonNull).forEach(bankReconciliation -> {
                    String bank_seq_no = bankReconciliation.getBank_seq_no() != null ? bankReconciliation.getBank_seq_no() : "";
                    String message = com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400517", "交易流水号【") /* "交易流水号【" */ + bank_seq_no + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540051C", "】已完成发布/业务关联/业务生单，请检查！") /* "】已完成发布/业务关联/业务生单，请检查！" */;
                    needUpdateBankReconciliationList.add(message);
                    bankReconciliationMapNotFinish.remove(bankReconciliation.getId());
                });
            });
            Optional.ofNullable(bankReconciliationMap.get(DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_MATCH_PENDING.getStatus())).ifPresent(reconciliationList -> {
                reconciliationList.stream().filter(Objects::nonNull).forEach(bankReconciliation -> {
                    String bank_seq_no = bankReconciliation.getBank_seq_no() != null ? bankReconciliation.getBank_seq_no() : "";
                    String message = com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400517", "交易流水号【") /* "交易流水号【" */ + bank_seq_no + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400522", "】需人工进行退票/关联确认；") /* "】需人工进行退票/关联确认；" */;
                    needUpdateBankReconciliationList.add(message);
                    bankReconciliationMapNotFinish.remove(bankReconciliation.getId());
                });

            });
            Optional.ofNullable(bankReconciliationMap.get(DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_PROCESS_GENERATEBILL_PENDING.getStatus())).ifPresent(reconciliationList -> {
                reconciliationList.stream().filter(Objects::nonNull).forEach(bankReconciliation -> {
                    String bank_seq_no = bankReconciliation.getBank_seq_no() != null ? bankReconciliation.getBank_seq_no() : "";
                    String message = com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400517", "交易流水号【") /* "交易流水号【" */ + bank_seq_no + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400525", "】需人工进行生单确认；") /* "】需人工进行生单确认；" */;
                    needUpdateBankReconciliationList.add(message);
                    bankReconciliationMapNotFinish.remove(bankReconciliation.getId());
                });

            });
            Optional.ofNullable(bankReconciliationMap.get(DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_MATCH_ERROR.getStatus())).ifPresent(reconciliationList -> {
                reconciliationList.stream().filter(Objects::nonNull).forEach(bankReconciliation -> {
                    String bank_seq_no = bankReconciliation.getBank_seq_no() != null ? bankReconciliation.getBank_seq_no() : "";
                    String message = com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400517", "交易流水号【") /* "交易流水号【" */ + bank_seq_no + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400527", "】处理失败，请稍后重试。") /* "】处理失败，请稍后重试。" */;
                    needUpdateBankReconciliationList.add(message);
                    bankReconciliationMapNotFinish.remove(bankReconciliation.getId());
                });

            });
            //
            Optional.ofNullable(bankReconciliationMap.get(DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_PROCESS_GENERATEBILL_FAIL.getStatus())).ifPresent(reconciliationList -> {
                reconciliationList.stream().filter(Objects::nonNull).forEach(bankReconciliation -> {
                    String bank_seq_no = bankReconciliation.getBank_seq_no() != null ? bankReconciliation.getBank_seq_no() : "";
                    String message = com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400517", "交易流水号【") /* "交易流水号【" */ + bank_seq_no + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400518", "】处理失败:") /* "】处理失败:" */ + DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_PROCESS_GENERATEBILL_FAIL.getDesc();
                    needUpdateBankReconciliationList.add(message);
                    bankReconciliationMapNotFinish.remove(bankReconciliation.getId());
                });

            });
            Optional.ofNullable(bankReconciliationMap.get(DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_PROCESS_PUBLISH_FAIL.getStatus())).ifPresent(reconciliationList -> {
                reconciliationList.stream().filter(Objects::nonNull).forEach(bankReconciliation -> {
                    String bank_seq_no = bankReconciliation.getBank_seq_no() != null ? bankReconciliation.getBank_seq_no() : "";
                    String message = com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400517", "交易流水号【") /* "交易流水号【" */ + bank_seq_no + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400518", "】处理失败:") /* "】处理失败:" */ + DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_PROCESS_PUBLISH_FAIL.getDesc();
                    needUpdateBankReconciliationList.add(message);
                    bankReconciliationMapNotFinish.remove(bankReconciliation.getId());
                });

            });

            Optional.ofNullable(bankReconciliationMap.get(DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_PROCESS_CREDENTIALT_FAIL.getStatus())).ifPresent(reconciliationList -> {
                reconciliationList.stream().filter(Objects::nonNull).forEach(bankReconciliation -> {
                    String bank_seq_no = bankReconciliation.getBank_seq_no() != null ? bankReconciliation.getBank_seq_no() : "";
                    String message = com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400517", "交易流水号【") /* "交易流水号【" */ + bank_seq_no + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400518", "】处理失败:") /* "】处理失败:" */ + DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_PROCESS_CREDENTIALT_FAIL.getDesc();
                    needUpdateBankReconciliationList.add(message);
                    bankReconciliationMapNotFinish.remove(bankReconciliation.getId());
                });

            });
            // 将枚举转换成Map，key是code，value是description
            Map<Short, String> statusMap = Arrays.stream(DealDetailEnumConst.DealDetailProcessStatusEnum.values())
                    .collect(Collectors.toMap(DealDetailEnumConst.DealDetailProcessStatusEnum::getStatus, DealDetailEnumConst.DealDetailProcessStatusEnum::getDesc));
            if (bankReconciliationMapNotFinish != null && bankReconciliationMapNotFinish.size() > 0) {
                Iterator<Map.Entry<Long, BankReconciliation>> iterator = bankReconciliationMapNotFinish.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<Long, BankReconciliation> entry = iterator.next();
                    String bank_seq_no = entry.getValue().getBank_seq_no() != null ? entry.getValue().getBank_seq_no() : "";
                    String messageResult=com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540051E", "智能流水执行异常，请前往[业务日志]节点，查看具体原因！") /* "智能流水执行异常，请前往[业务日志]节点，查看具体原因！" */;
                    String message = com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400517", "交易流水号【") /* "交易流水号【" */ + bank_seq_no + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540051F", "】处理结果:") /* "】处理结果:" */ + statusMap.get(entry.getValue().getProcessstatus());
                    if (statusMap.get(entry.getValue().getProcessstatus()) == null){
                        message = com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400517", "交易流水号【") /* "交易流水号【" */ + bank_seq_no + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540051F", "】处理结果:") /* "】处理结果:" */+messageResult;
                    }
                    needUpdateBankReconciliationList.add(message);
                }
            }
            String result = String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400524", "共勾选%s笔流水通过规则进行自动处理：") /* "共勾选%s笔流水通过规则进行自动处理：" */, originCount);
            Map<String, Object> resultMap = new HashMap<>();
            resultMap.put("resultMessage", result);
            resultMap.put("resultList", needUpdateBankReconciliationList);
            return ResultMessage.data(resultMap);
        } catch (Exception e) {
            DealDetailUtils.recordException(DealDetailUtils.getTraceId(), requestNo, e);
            throw e;
        }
    }


    /**
     * 阻断主线程，轮训获取结果
     * @param bankReconciliationIds
     * @param redisTemplate
     */
    private void processTimer(List<String> bankReconciliationIds, YMSRedisTemplate redisTemplate) {
        // 创建一个定时任务调度器，设置每秒执行一次任务
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        // 创建一个 CountDownLatch 用于主线程等待任务执行
        final CountDownLatch latch = new CountDownLatch(1);
        // 定义一个计数器来模拟条件
        final int[] counter = {0};
        // 定义一个最大执行次数
        final int MAX_EXECUTIONS = 10;
        Runnable task = new Runnable() {
            @SneakyThrows
            @Override
            public void run() {
                counter[0]++;
                List<String> list = redisTemplate.opsForValue().multiGet(bankReconciliationIds);
                // 判断查询的集合是否为空
                if (null == list && list.isEmpty() || counter[0] >= MAX_EXECUTIONS) {
                    // 执行主线程任务并设置 CountDownLatch 来唤醒主线程
                    latch.countDown(); // 主线程继续执行
                } else {
                    log.error(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540052F", "集合不为空，继续等待...") /* "集合不为空，继续等待..." */);
                }
            }
        };
        // 安排任务每秒执行一次
        scheduler.scheduleAtFixedRate(task, 0, 1, TimeUnit.SECONDS);
        // 阻塞主线程，直到 CountDownLatch 被释放
        try {
            latch.await();
        } catch (InterruptedException e) {
            log.error("等待任务执行完毕被中断");
        }
        // 关闭调度器
        scheduler.shutdown();
    }

    /**
     * 导入走单条导入模式，在mdd前置规则中接入ods
     */
    public void dealDetailAccessByImport(BankReconciliation bankReconciliation) {
        String requestNo = UUID.randomUUID().toString();
        try {
            BankDealDetailModel bankDealDetailModel = new BankDealDetailModel();
            bankDealDetailModel.setStreamResponseRecordList(Arrays.asList(bankReconciliation));
            bankDealDetailModel.setRequestSeqNo(requestNo);
            signalDealDetailAccess.dealDetailAccess(bankDealDetailModel);
        } catch (Exception e) {
            DealDetailUtils.recordException(DealDetailUtils.getTraceId(), requestNo, e);
            throw e;
        }
    }

    /**
     * 导入走单条导入模式，在mdd前置规则中接入ods
     */
    public void dealDetailAccessByImportNew(BankReconciliation bankReconciliation) {
        String requestNo = UUID.randomUUID().toString();
        try {
            BankDealDetailModel bankDealDetailModel = new BankDealDetailModel();
            bankDealDetailModel.setStreamResponseRecordList(Arrays.asList(bankReconciliation));
            bankDealDetailModel.setRequestSeqNo(requestNo);
            signalDealDetailAccessNew.dealDetailAccess(bankDealDetailModel);
        } catch (Exception e) {
            DealDetailUtils.recordException(DealDetailUtils.getTraceId(), requestNo, e);
            throw e;
        }
    }

    /**
     * 到账通知&openapi
     */
    public BankUnionResponse dealDetailAccessByNotice(List<BankUnionRequestBodyRecord> bankUnionRequests) {
        BankUnionResponse response = new BankUnionResponse();
        String requestSeqNo = UUID.randomUUID().toString();
        try {
            //step1:构建流水接入参数
            NoticeBankDealDetailModel bankDealDetailModel = new NoticeBankDealDetailModel();
            bankDealDetailModel.setStreamResponseRecordList(bankUnionRequests);
            bankDealDetailModel.setRequestSeqNo(requestSeqNo);
            //step2:调用ods接入接口
            noticeDealDetailAccess.dealDetailAccess(bankDealDetailModel);
            //step3:收集执行结果
            List<BankUnionResponseRecord> bankUnionResponseFailRecords = bankDealDetailModel.getResponseParseFailRecords();
            if (CollectionUtils.isEmpty(bankUnionResponseFailRecords)) {
                log.error("====全部成功========");
                response.setCode("1");
                response.setMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400530", "全部成功") /* "全部成功" */);
                List<BankUnionResponseRecord> responseRecords = new ArrayList<>();
                for (BankUnionRequestBodyRecord requestBodyRecord : bankUnionRequests) {
                    BankUnionResponseRecord responseRecord = new BankUnionResponseRecord();
                    responseRecord.setUnique_no(requestBodyRecord.getUnique_no());
                    responseRecord.setCode("000000");
                    responseRecord.setMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540051A", "入库成功！") /* "入库成功！" */);
                    responseRecords.add(responseRecord);
                }
                response.setData(responseRecords);
            } else {
                if (bankUnionResponseFailRecords.size() == bankUnionRequests.size()) {
                    log.error("====全部失败========");
                    response.setCode("2");
                    response.setMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540051D", "全部失败") /* "全部失败" */);
                    response.setData(bankUnionResponseFailRecords);
                } else {
                    log.error("====部分成功========");
                    response.setCode("3");
                    response.setMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400521", "部分成功") /* "部分成功" */);
                    List<BankUnionResponseRecord> bankUnionResponseRecords = new ArrayList<>();
                    //记录失败的
                    bankUnionResponseRecords.addAll(bankUnionResponseFailRecords);
                    Map<String, BankUnionRequestBodyRecord> bankUnionRequestBodyRecordMap = Maps.uniqueIndex(bankUnionRequests, BankUnionRequestBodyRecord::getUnique_no);
                    //将失败流水从请求入参集合中删除，删除后剩余的就是执行成功的
                    for (BankUnionResponseRecord responseRecord : bankUnionResponseFailRecords) {
                        bankUnionRequestBodyRecordMap.remove(responseRecord.getUnique_no());
                    }
                    //执行成功流水
                    Collection<BankUnionRequestBodyRecord> successRequestList = bankUnionRequestBodyRecordMap.values();
                    List<BankUnionResponseRecord> succList = new ArrayList<>();
                    for (BankUnionRequestBodyRecord bodyRecord : successRequestList) {
                        BankUnionResponseRecord responseRecord = new BankUnionResponseRecord();
                        responseRecord.setUnique_no(bodyRecord.getUnique_no());
                        responseRecord.setCode("000000");
                        responseRecord.setMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540051A", "入库成功！") /* "入库成功！" */);
                        succList.add(responseRecord);
                    }
                    //记录成功的
                    bankUnionResponseRecords.addAll(succList);
                    response.setData(bankUnionResponseRecords);
                }
            }
        } catch (Exception e) {
            log.error("流水入ods异常", e);
            List<BankUnionResponseRecord> responseRecords = new ArrayList<>();
            for (BankUnionRequestBodyRecord bodyRecord : bankUnionRequests) {
                BankUnionResponseRecord responseRecord = new BankUnionResponseRecord();
                responseRecord.setUnique_no(bodyRecord.getUnique_no());
                responseRecord.setCode("010011");
                responseRecord.setMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540052A", "流水入ODS异常") /* "流水入ODS异常" */);
                responseRecords.add(responseRecord);
            }
            response.setCode("2");
            response.setMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540051D", "全部失败") /* "全部失败" */);
            response.setData(responseRecords);
            DealDetailUtils.recordException(DealDetailUtils.getTraceId(), requestSeqNo, e);
        }
        return response;
    }


    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        yqlDealDetailAccess = applicationContext.getBean("yQLDealDetailAccessImpl", YQLDealDetailAccessImpl.class);
        manualDealDetailAccess = applicationContext.getBean("manualDealDetailAccessImpl", ManualDealDetailAccessImpl.class);
        signalDealDetailAccess = applicationContext.getBean("signalDealDetailAccessImpl", SignalDealDetailAccessImpl.class);
        signalDealDetailAccessNew = applicationContext.getBean("signalDealDetailAccessImplNew", SignalDealDetailAccessImplNew.class);
        noticeDealDetailAccess = applicationContext.getBean("noticeDealDetailAccessImpl", NoticeDealDetailAccessImpl.class);
    }
}
