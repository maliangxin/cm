package com.yonyoucloud.fi.cmp.intelligentdealdetail.dealdetailimport;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.yonbip.iuap.xport.common.error.ExcelErrorLocation;
import com.yonyou.yonbip.iuap.xport.importing.data.ImportBatchData;
import com.yonyou.yonbip.iuap.xport.importing.processor.ImportEventExtendProcessor;
import com.yonyou.yonbip.iuap.xport.importing.processor.pojo.ImportEventBO;
import com.yonyou.yonbip.iuap.xport.importing.processor.pojo.ImportSingleData;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.common.service.CtmCmpCheckRepeatDataService;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.repetition.service.IRepetitionService;
import lombok.extern.slf4j.Slf4j;
import org.imeta.biz.base.Objectlizer;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import java.util.*;
/**
 * @Author guoyangy
 * @Date 2024/10/17 10:41
 * @Description todo
 * @Version 1.0
 */
@Slf4j
@Service
public class DealDetailImportEventExtendProcessor implements ImportEventExtendProcessor {
    @Autowired
    private CtmCmpCheckRepeatDataService cmpCheckRepeatDataService;
    @Override
    public void beforeSave(@NotNull ImportBatchData importBatchData, @NotNull ImportEventBO importEventBO) {
        try{
            // 非交易流水不进行校验
            String billNo = importEventBO.getImportContextDto().getBillno();
            if (!billNo.equalsIgnoreCase("cmp_bankdealdetail")) {
                return;
            }
            // 是否使用交易明细对象检测重复，现在使用是银行流水对象
            /*if (cmpCheckRepeatDataService.checkCurrentBatchIsRepeate(importBatchData)) {
                return;
            }*/
            log.error("【导入前置规则】内存去重完成");
        }catch (Exception e){
            log.error("【导入前置规则】内存去重异常",e);
        }
    }
}
