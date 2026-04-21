package com.yonyoucloud.fi.cmp.bankreconciliation.processor;

import com.yonyou.yonbip.iuap.xport.importing.data.ImportBatchData;
import com.yonyou.yonbip.iuap.xport.importing.processor.ImportEventExtendProcessor;
import com.yonyou.yonbip.iuap.xport.importing.processor.pojo.ImportEventBO;
import com.yonyou.yonbip.iuap.xport.importing.processor.pojo.ImportSingleData;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.common.service.CtmCmpCheckRepeatDataService;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.consts.BankDealDetailConst;
import com.yonyoucloud.fi.cmp.util.DateUtils;
import com.yonyoucloud.fi.cmp.util.CtmDealDetailCheckMayRepeatUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.map.HashedMap;
import org.imeta.biz.base.Objectlizer;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.RoundingMode;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@Order
public class BankreconciliationImportEventExtendProcessor implements ImportEventExtendProcessor {
    @Autowired
    private CtmCmpCheckRepeatDataService cmpCheckRepeatDataService;

    @Override
    public void beforeSave(@NotNull ImportBatchData importBatchData, @NotNull ImportEventBO importEventBO) {
        try {
            String billNo = importEventBO.getImportContextDto().getBillno();
            if (!billNo.equalsIgnoreCase("cmp_bankreconciliation")) {
                return;
            }
            // 检测当前批次流水是否有8要素重复
            if (cmpCheckRepeatDataService.checkCurrentBatchIsRepeate(importBatchData))
            {
                return;
            }
            List<ImportSingleData> data = importBatchData.getData();
            if (CtmDealDetailCheckMayRepeatUtils.isRepeatCheck) {
                HashSet<String> repeatSet = new HashSet<>();
                data.forEach(e->{
                    Map<String,Object> map = e.getData();
                    BankReconciliation bankReconciliation = Objectlizer.convert(map, BankReconciliation.ENTITY_NAME);
                    String repeatInfo = this.formatConcatInfoDefineFactorsBankReconciliation(bankReconciliation);
                    boolean flag = repeatSet.add(repeatInfo);
                    if(flag){
                        try {
                            Map<String, Object> enterpriseInfo = new HashedMap<>();
                            enterpriseInfo.put("startDate", DateUtils.convertToStr(bankReconciliation.getTran_date(),"yyyy-MM-dd"));
                            cmpCheckRepeatDataService.deal4FactorsBankDealDetail(Collections.singletonList(bankReconciliation), enterpriseInfo);
                            map.put("isrepeat", bankReconciliation.getIsRepeat());
                        }catch (Exception ex){
                            log.error("疑重判定失败!",ex);
                            map.put("isrepeat",(short) BankDealDetailConst.REPEAT_INIT);
                        }
                    }else{
                        map.put("isrepeat",(short) BankDealDetailConst.REPEAT_DOUBT);
                    }
                });
                log.info("疑重判断逻辑结束");
            }else{
                data.forEach(e->{
                    Map<String,Object> map = e.getData();
                    map.put("isrepeat",(short) BankDealDetailConst.REPEAT_INIT);
                });
            }
        }catch (Exception e){
            log.error("疑重判断逻辑异常",e);
        }

    }

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
}
