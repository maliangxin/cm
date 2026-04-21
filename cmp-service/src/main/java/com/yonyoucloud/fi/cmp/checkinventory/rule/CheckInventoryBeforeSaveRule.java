package com.yonyoucloud.fi.cmp.checkinventory.rule;

import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.bill.rule.util.BillInfoUtils;
import com.yonyou.ucf.mdd.ext.bpm.model.VerifyState;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.workbench.util.Lists;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.checkinventory.CheckInventory;
import com.yonyoucloud.fi.cmp.checkinventory.CheckInventory_b;
import com.yonyoucloud.fi.cmp.checkstock.CheckStock;
import com.yonyoucloud.fi.cmp.cmpentity.AuditStatus;
import com.yonyoucloud.fi.cmp.cmpentity.CheckResultStatus;
import com.yonyoucloud.fi.cmp.util.DateUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 支票盘点保存前规则
 */
@Slf4j
@Component("checkInventoryBeforeSaveRule")
public class CheckInventoryBeforeSaveRule extends AbstractCommonRule {
    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        BizObject bill = getBills(billContext, paramMap).get(0);
        if (bill.get("_status").equals(EntityStatus.Insert)) {
            String accentity = bill.get("accentity");
            Date inventorydate = bill.getDate("inventorydate");
            String accentity_name = bill.get("accentity_name");
            //保存时，校验会计主体的本次盘点日期不能早于上一笔审批流状态=审批完成的盘点单的盘点日期，否则错误性提示“保存失败，本次盘点日期不能早于上期盘点日期！”；
            QuerySchema querySchema = QuerySchema.create().addSelect("id").appendQueryCondition(
                    QueryCondition.name("accentity").eq(accentity),
                    QueryCondition.name("inventorydate").egt(inventorydate),
                    QueryCondition.name("verifystate").eq(VerifyState.COMPLETED.getValue())
            );
            List<CheckInventory> list = MetaDaoHelper.queryObject(CheckInventory.ENTITY_NAME,querySchema,null);
            if(CollectionUtils.isNotEmpty(list)){
                log.error("本次盘点日期不能早于上期盘点日期");
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102315"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("", "保存失败，本次盘点日期不能早于上期盘点日期！") /* "保存失败，本次盘点日期不能早于上期盘点日期！" */);
            }
            //保存时，校验会计主体是否有在途（审批状态不等于审批完成、流程终止）的重空凭证盘点单，若有，错误性提示“保存失败，会计主体[会计主体]存在在途的盘点单，请先处理在途的盘点单！”
            querySchema = QuerySchema.create().addSelect("id").appendQueryCondition(
                    QueryCondition.name("accentity").eq(accentity),
                    QueryCondition.name("verifystate").not_in(Arrays.asList(VerifyState.COMPLETED.getValue(),VerifyState.TERMINATED.getValue()))
            );
            list = MetaDaoHelper.queryObject(CheckInventory.ENTITY_NAME,querySchema,null);
            if(CollectionUtils.isNotEmpty(list)){
                log.error("会计主体:{}存在在途的盘点单",accentity_name);
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102316"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("", "保存失败，会计主体[%s]存在在途的盘点单，请先处理在途的盘点单！") /* "保存失败，本次盘点日期不能早于上期盘点日期！" */,accentity_name));
            }
            //获取子表数据
            List<CheckInventory_b> listb = bill.getBizObjects("CheckInventory_b", CheckInventory_b.class);
            if (listb == null || listb.size() < 1) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102317"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-FE_180E028A05780107", "明细具体信息数据区表格不能为空") /* "明细具体信息数据区表格不能为空" */);
            }
            for (CheckInventory_b checkInventoryB : listb) {
                if (Short.parseShort(checkInventoryB.getCheckresult()) != CheckResultStatus.InventoryProfit.getValue()) {
                    if (null != checkInventoryB.get("checkid")) {
                        String[] checkids = checkInventoryB.get("checkid").toString().split(",");
                        List<String> checkIdList = Lists.newArrayList(checkids);
                        List<Map<String, Object>> checkStockList = MetaDaoHelper.queryByIds(CheckStock.ENTITY_NAME, "*", checkIdList);
                        if (CollectionUtils.isEmpty(checkStockList)) {
                            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102318"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1889ECAE05B00003", "关联的支票单据不存在，请重新盘点") /* "关联的支票单据不存在，请重新盘点" */);
                        }
                        if (checkStockList.size() > 0) {
                            Date currentPubts = bill.getPubts();
                            if (currentPubts != null) {
                                long pubts = checkStockList.stream().filter(a -> !currentPubts.equals(a.get("pubts"))).count();
                                if (pubts > 0) {
                                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100716"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418048C", "当前单据不是最新状态，请刷新单据重新操作。") /* "当前单据不是最新状态，请刷新单据重新操作。" */);
                                }
                            }
                            //是当前日期才校验
                            if (inventorydate == null || DateUtils.dateBetween(inventorydate, new Date()) == 0) {
                                //校验支票状态不为已入库状态时，不允许盘点
                                List<Map<String, Object>> checkBillStatus = checkStockList.stream().filter(e -> Integer.parseInt(e.get("checkBillStatus").toString()) != 1).collect(Collectors.toList());
                                if (checkBillStatus.size() > 0) {
                                    StringBuilder checkStock = new StringBuilder();
                                    for (Map<String, Object> billStatus : checkBillStatus) {
                                        checkStock.append("【" + billStatus.get("checkBillNo") + "】,");//@notranslate
                                    }
                                    String substring = checkStock.substring(0, checkStock.length() - 1);
                                    //盘点数据中存在被使用/处置的支票【XXX】,【YYY】，需要重新进行盘点，请检查！
                                    //throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102319"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_185866AE05380005", "存在被使用/处置的支票，请关闭页面，重新盘点") /* "存在被使用/处置的支票，请关闭页面，重新盘点" */);
                                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102320"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A050800E5", "盘点数据中存在被使用/处置的支票") /* "盘点数据中存在被使用/处置的支票" */ + substring + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A050800E4", "需要重新进行盘点，请检查！") /* "需要重新进行盘点，请检查！" */);
                                }
                            }
                        }
                    }
                }
            }
        }
        return new RuleExecuteResult();
    }
}
