package com.yonyoucloud.fi.cmp.bankreconciliation.service.autogenerateBill;

import cn.hutool.core.date.StopWatch;
import com.yonyou.business_flow.dto.DomainMakeBillRuleModel;
import com.yonyou.cloud.annotation.IrisReference;
import com.yonyou.iuap.yms.lock.YmsLock;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ypd.bill.basic.service.api.IYpdBillInsertService;
import com.yonyou.ypd.bizflow.dto.ConvertParam;
import com.yonyou.ypd.bizflow.dto.ConvertResult;
import com.yonyou.ypd.bizflow.dto.ConvertedBill;
import com.yonyou.ypd.bizflow.service.BusinessConvertService;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.cmpentity.AssociationStatus;
import com.yonyoucloud.fi.cmp.cmpentity.ClaimCompleteType;
import com.yonyoucloud.fi.cmp.cmpentity.EntryType;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.enums.EarapBizflowEnum;
import com.yonyoucloud.fi.cmp.enums.SerialdealendState;
import com.yonyoucloud.fi.cmp.util.JedisLockUtils;
import com.yonyoucloud.fi.cmp.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author guoxh
 * @date 2024-11-05
 * @desc 银行流水处理自动生成财务会计应收应付单据
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class BusinessGenerateEarapbillService {
    private static final String EARP_DOMAIN = "yonbip-fi-earapbill";

    private final BusinessConvertService businessConvertService;

    @IrisReference(appCode = EARP_DOMAIN)
    private IYpdBillInsertService ypdBillInsertService;

    /**
     * 生成应收应付三个单据
     *
     * @param bankReconciliation
     * @param earpBizflowEnum
     */
    @Transactional(rollbackFor = RuntimeException.class, propagation = Propagation.REQUIRES_NEW)
    public void doGenerateBillBySingle(BankReconciliation bankReconciliation, EarapBizflowEnum earpBizflowEnum) {
        YmsLock ymsLock = JedisLockUtils.lockBillWithOutTrace(bankReconciliation.getId().toString());
        try {
            if (ymsLock == null) {
                log.error("应付相关单据-账户交易流水自动生单（智能认领），获取锁失败，对应流水号：" + bankReconciliation.getBank_seq_no());
                return;
            }
            StopWatch stopWatch = new StopWatch(earpBizflowEnum.getRuleCode());
            ConvertParam convertParam = this.forConvertParam(bankReconciliation, earpBizflowEnum);

            stopWatch.start(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054006FD", "query 查询单据转换规则") /* "query 查询单据转换规则" */);
            //查询单据转换规则
            DomainMakeBillRuleModel domainMakeBillRuleModel = businessConvertService.queryMakeBillRule(convertParam);
            stopWatch.stop();

            stopWatch.start(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054006FF", "convert 根据转单规则生成付款单实体") /* "convert 根据转单规则生成付款单实体" */);
            //根据转单规则生成付款单实体
            ConvertResult result = businessConvertService.convert(convertParam, domainMakeBillRuleModel);
            stopWatch.stop();
            log.error("stopwatch is :{}", stopWatch.prettyPrint(TimeUnit.MILLISECONDS));
            List<ConvertedBill> billList = result.getConvertedBillList();

            try {
                stopWatch.start(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054006FC", "根据银行流水id查询数据，当前流水对应流水号:") /* "根据银行流水id查询数据，当前流水对应流水号:" */+bankReconciliation.getBank_seq_no());
                //加锁成功
                BankReconciliation b = MetaDaoHelper.findById(BankReconciliation.ENTITY_NAME, bankReconciliation.getId());
                stopWatch.stop();
                if (!b.getPubts().equals(bankReconciliation.getPubts())) {
                    log.error("银行流水id:{} 对应的pubts发生了变化，不再执行后续生单逻辑", b.getString("id"));
                    return;
                }

                // 针对大北农的频繁发布的操作，看一下发布状态，精准捕捉一下
                if (b.getIspublish()) {
                    log.error("当前流水已发布，无法进行生单！对应流水id为" + bankReconciliation.getId() + "对应流水银行流水号为：" + bankReconciliation.getBank_seq_no());
                    throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400702", "当前流水已发布，无法进行生单！对应流水id为") /* "当前流水已发布，无法进行生单！对应流水id为" */ + bankReconciliation.getId() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400701", "对应流水银行流水号为：") /* "对应流水银行流水号为：" */ + bankReconciliation.getBank_seq_no());
                }

                stopWatch.start(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400703", "rpc 调用应收应付生成单据") /* "rpc 调用应收应付生成单据" */);
                for (ConvertedBill convertedBill : billList) {
                    Map<String, Object> targetData = convertedBill.getTargetData();
                    //交易类型
                    if (!StringUtils.isEmpty(bankReconciliation.getTradetype())) {
                        //自动生单如果不传交易类型的话 收付那边的逻辑是取默认交易类型
                        targetData.put("bustype", bankReconciliation.getTradetype());
                    }
                    //自动生单转换后单据后在单据对象的里加一个参数autoPushBill，标识一下是自动生单保存，不然收付方无法区分是手动的还是自动的，到下游单据时会放在extendData中
                    targetData.put("autoPushBill", true);
                    convertedBill.setTargetData(targetData);
                    // 在这里会调用接口 CtmcmpReWriteBusRpcService#batchReWriteBankRecilicationForRpc，会更新流水的状态，刷新pubts
                    // 逆流程则是调用CtmcmpReWriteBusRpcService#resDelDataForRpc接口 删除关联关系
                    try {
                        ypdBillInsertService.insertBill(convertedBill);
                        bankReconciliation.setAssociationstatus(AssociationStatus.Associated.getValue());
                        bankReconciliation.setSerialdealendstate(SerialdealendState.END.getValue());
                        bankReconciliation.setSerialdealtype(ClaimCompleteType.RecePayGen.getValue());
                        bankReconciliation.setEntrytype(EntryType.Normal_Entry.getValue());
                    }catch (Exception e){
                        log.error("银行流水id:{}调用应收应付生成单据：{}",bankReconciliation.getId().toString(),e.getMessage(),e);
                        throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054006FE", "调用收付生单接口异常，异常原因：") /* "调用收付生单接口异常，异常原因：" */+e.getMessage(),e);
                    }
                    stopWatch.stop();
                }
                stopWatch.start(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400700", "更新银行流水生单状态") /* "更新银行流水生单状态" */);
                // 上面流程中，收付调用了现金的接口，CtmcmpReWriteBusRpcService#batchReWriteBankRecilicationForRpc，会更新流水的状态，刷新pubts，所以这里需要重新查询一下
                b = MetaDaoHelper.findById(BankReconciliation.ENTITY_NAME, bankReconciliation.getId());

                // 针对大北农的频繁发布的操作，看一下发布状态，精准捕捉一下
                if (b.getIspublish()) {
                    log.error("当前流水已发布，无法进行生单！对应流水id为" + bankReconciliation.getId() + "对应流水银行流水号为：" + bankReconciliation.getBank_seq_no());
                    throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400702", "当前流水已发布，无法进行生单！对应流水id为") /* "当前流水已发布，无法进行生单！对应流水id为" */ + bankReconciliation.getId() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400701", "对应流水银行流水号为：") /* "对应流水银行流水号为：" */ + bankReconciliation.getBank_seq_no());
                }
                stopWatch.stop();
                log.error("genearte earapbill stopwatch is :{}", stopWatch.prettyPrint(TimeUnit.MILLISECONDS));
            } catch (Exception e) {
                log.error("银行流水id:{}自动生单失败：{}",bankReconciliation.getId().toString(),e.getMessage(),e);
                throw e;
            }
        }catch (Exception e){
            log.error("银行流水id:{}执行生单规则:{}失败,失败原因:{}",bankReconciliation.getId().toString(),earpBizflowEnum.getRuleCode(),e.getMessage(),e);
            throw new CtmException(e.getMessage());
        } finally {
            if (ymsLock != null) {
                JedisLockUtils.unlockBillWithOutTrace(ymsLock);
            }
        }
    }


    /**
     * 获取转单规则实体
     *
     * @param bankReconciliation
     * @return
     */
    private ConvertParam forConvertParam(BankReconciliation bankReconciliation, EarapBizflowEnum earpBizflowEnum) throws Exception {
        List<Map<String, Object>> mapList = MetaDaoHelper.queryById(BankReconciliation.ENTITY_NAME, ICmpConstant.SELECT_TOTAL_PARAM, bankReconciliation.getId());
        log.error("mapList:{}", mapList);
        ConvertParam convertParam = new ConvertParam();
        convertParam.setBillNum(earpBizflowEnum.getBillNum());
        convertParam.setTenantId(bankReconciliation.getYtenantId());
        convertParam.setSourceBills(mapList);
        convertParam.setChildIds(Collections.singletonList(bankReconciliation.getString("id")));
        convertParam.setMakeBillRuleCode(earpBizflowEnum.getRuleCode());
        convertParam.setSubId(earpBizflowEnum.getSubId());
        convertParam.setDomain(earpBizflowEnum.getDomain());
        convertParam.setSourceIds(Collections.singletonList(bankReconciliation.getId().toString()));
        convertParam.setShowConvertedBill(false); //不自动保存
        return convertParam;
    }
}
