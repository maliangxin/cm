package com.yonyoucloud.fi.cmp.bill;

import com.yonyou.yonbip.iuap.xport.importing.data.ImportBatchData;
import com.yonyou.yonbip.iuap.xport.importing.processor.importRuleCheck.ImportDataSaveStrategyExtend;
import com.yonyou.yonbip.iuap.xport.importing.processor.pojo.ImportEventBO;
import com.yonyou.yonbip.iuap.xport.importing.processor.pojo.ImportEventContext;
import org.springframework.stereotype.Component;

/**
 * <h1>手动打开平台实现的状态判断</h1>
 *
 * 导入时进入到保存规则中的数据中主表的_status无论实际是新增还是更新都是Insert，或者子表中没有_status字段
 * 由于部分业务是自己根据规则计算的新增还是更新状态，导入框架没有办法默认打开平台实现的规则判断，否则这部分业务会在导入时报错: 缺少唯一性校验规则；
 * 手动打开平台实现的状态判断的方式：
 * a. 实现接口com.yonyou.yonbip.iuap.xport.importing.processor.importRuleCheck.ImportDataSaveStrategyExtend，并声明为Spring的bean；
 * b. 如果使用平台的判定规则，所有方法按照默认返回即可，不需要实现业务逻辑，不要修改方法的返回值；
 * c. 如果需要使用自定义的规则，根据条件将需要自定义实现的单据的isSkipCheck方法的返回值改为false，并实现doImportDataSaveStrategyCheck方法；
 *
 * @author Sun GuoCai
 * @version 1.0
 * @since 2024-07-09 19:14
 */
@Component("cmpImportDataSaveStrategyExtend")
public class CmpImportDataSaveStrategyExtend implements ImportDataSaveStrategyExtend {
    @Override
    public void doImportDataSaveStrategyCheck(ImportBatchData batchData, ImportEventContext event) {

    }

    @Override
    public boolean isSkipCheck(ImportBatchData batchData, ImportEventContext event) {
        return ImportDataSaveStrategyExtend.super.isSkipCheck(batchData, event);
    }

    @Override
    public void doImportDataSaveStrategyCheck(ImportBatchData batchData, ImportEventBO event) {
        ImportDataSaveStrategyExtend.super.doImportDataSaveStrategyCheck(batchData, event);
    }

    @Override
    public boolean isSkipCheck(ImportBatchData batchData, ImportEventBO event) {
        return ImportDataSaveStrategyExtend.super.isSkipCheck(batchData, event);
    }
}
