//package com.yonyoucloud.fi.cmp.bankreconciliation.utils;
//
//import com.esotericsoftware.minlog.Log;
//import com.yonyou.ucf.mdd.ext.core.AppContext;
//import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
//import com.yonyou.ucf.mdd.ext.dao.sql.SqlHelper;
//import com.yonyou.yonbip.ctm.json.CtmJSONObject;
//import com.yonyou.yonbip.ctm.logger.business.CTMCMPBusinessLogService;
//import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
//import com.yonyoucloud.fi.cmp.bankreconciliation.OperateSourceEnum;
//import com.yonyoucloud.fi.cmp.billclaim.BillClaim;
//import com.yonyoucloud.fi.cmp.cmpentity.OppositeType;
//import com.yonyoucloud.fi.cmp.common.CtmException;
//import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
//import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
//import com.yonyoucloud.fi.cmp.util.CmpMetaDaoHelper;
//import com.yonyoucloud.fi.cmp.util.EntityTool;
//import com.yonyoucloud.fi.cmp.util.StringUtils;
//import lombok.extern.slf4j.Slf4j;
//import org.apache.commons.collections4.CollectionUtils;
//import org.imeta.orm.base.BizObject;
//import org.imeta.orm.base.EntityStatus;
//
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.List;
//import java.util.stream.Collectors;
//
///**
// * 流水处理修改的统一处理类
// *
// * @author ml
// */
//@Slf4j
//public class BankReconciliationOperateUtils {
//
//    static String ENABLE = "1";
//
//    /**
//     * 按明确的键值更新
//     *
//     * @param bankReconciliation
//     * @throws Exception
//     */
//    public static void updateBankReconciliation4Certain(BankReconciliation bankReconciliation) throws Exception {
//        if (bankReconciliation == null) {
//            return;
//        }
//        EntityTool.setUpdateStatus(bankReconciliation);
//        MetaDaoHelper.update(BankReconciliation.ENTITY_NAME, bankReconciliation);
//        recordLog(bankReconciliation);
//    }
//
//    /**
//     * 更新流水集合
//     *
//     * @param updateBankReconciliations
//     * @throws Exception
//     */
//    public static void updateBankReconciliationList4Certain(List<BankReconciliation> updateBankReconciliations) throws Exception {
//        if (CollectionUtils.isNotEmpty(updateBankReconciliations)) {
//            EntityTool.setUpdateStatus(updateBankReconciliations);
//            MetaDaoHelper.update(BankReconciliation.ENTITY_NAME, updateBankReconciliations);
//            for(BankReconciliation bankReconciliation : updateBankReconciliations){
//                recordLog(bankReconciliation);
//            }
//        }
//    }
//
//    //兼容BizObject的入参
//    public static void saveBankReconciliation(BizObject bankReconciliationBizObject) throws Exception {
//        if (bankReconciliationBizObject != null) {
//            Short oppositetype = bankReconciliationBizObject.getShort("oppositetype");
//            String oppositeobjectId = bankReconciliationBizObject.get("oppositeobjectid");
//            if (oppositetype != null) {
//                setOppositeobjectidToBizField(bankReconciliationBizObject, oppositetype, oppositeobjectId);
//            }
//            CmpMetaDaoHelper.insert(BillClaim.ENTITY_NAME, bankReconciliationBizObject);
//        }
//    }
//
//    /**
//     * 回单关联状态的更新，也进行单独处理
//     *
//     * @param bankReconciliationList
//     * @param <T>
//     * @throws Exception
//     * @reconciliationdatasource 1凭证；2日记账。
//     */
//    public static <T extends BizObject> void updateBankReconciliation4ReceiptassociationStatus(List<BankReconciliation> bankReconciliationList) throws Exception {
//        if (bankReconciliationList == null) {
//            return;
//        }
//        List<BankReconciliation> updateBankReconciliations = new ArrayList<>();
//        for (BankReconciliation bankReconciliation : bankReconciliationList) {
//            BankReconciliation bankReconciliationBizObject = new BankReconciliation();
//            bankReconciliationBizObject.setId(bankReconciliation.getId());
//            bankReconciliationBizObject.setReceiptId(bankReconciliation.getReceiptId());
//            bankReconciliationBizObject.setReceiptassociation(bankReconciliation.getReceiptassociation());
//            bankReconciliationBizObject.setPubts(bankReconciliation.getPubts());
//            bankReconciliationBizObject.setEntityStatus(EntityStatus.Update);
//            updateBankReconciliations.add(bankReconciliationBizObject);
//        }
//        MetaDaoHelper.update(BankReconciliation.ENTITY_NAME, updateBankReconciliations);
//        for(BankReconciliation bankReconciliation : updateBankReconciliations){
//            recordLog(bankReconciliation);
//        }
//    }
//
//    /**
//     * 修改流水记录业务日志
//     * @param bankReconciliations
//     * @throws Exception
//     */
//    public static void updateBankReconciliation(List<BankReconciliation> bankReconciliations) throws Exception {
//        if (CollectionUtils.isEmpty(bankReconciliations)) {
//            return;
//        }
//        EntityTool.setUpdateStatus(bankReconciliations);
//        updateBankReconciliationAndRemoveKeys(bankReconciliations);
//        for(BankReconciliation bankReconciliation : bankReconciliations){
//            recordLog(bankReconciliation);
//        }
//    }
//
//    /**
//     * 兼容泛型和BizObject、BankReconciliation的入参；写多个方法的话，类型擦除后，编译不通过
//     */
//    public static <T extends BizObject> void updateBankReconciliationAndRemoveKeys(BankReconciliation bankReconciliation) throws Exception {
//        if (bankReconciliation == null) {
//            return;
//        }
//        List<BankReconciliation> bankReconciliationList = new ArrayList<>();
//        bankReconciliationList.add(bankReconciliation);
//        updateBankReconciliationAndRemoveKeys(bankReconciliationList);
//        recordLog(bankReconciliation);
//    }
//
//    /**
//     * 兼容泛型和BizObject、BankReconciliation的入参；写多个方法的话，类型擦除后，编译不通过
//     */
//    public static <T extends BizObject> void updateBankReconciliationAndRemoveKeys(List<T> bankReconciliationList) throws Exception {
//        if (CollectionUtils.isEmpty(bankReconciliationList)) {
//            return;
//        }
//        EntityTool.setUpdateStatus(bankReconciliationList);
//        // 针对牧原客户字段被标品覆盖的情况，需要将客开相关字段给移除
//        String removeKey = AppContext.getEnvConfig("cmp.bankreconciliation.removekey");
//        List<String> removeKeyArray = null;
//        if (StringUtils.isNotEmpty(removeKey)) {
//            removeKeyArray = Arrays.stream(removeKey.split(",")).collect(Collectors.toList());
//        }
//        boolean mark = false;
//        //实际类型是BizObject
//        List<BizObject> bankReconciliationBizObjectList = (List<BizObject>) bankReconciliationList;
//        for (BizObject bankReconciliationBizObject :
//                bankReconciliationBizObjectList) {
//            Short oppositetype = bankReconciliationBizObject.getShort("oppositetype");
//            String oppositeobjectId = bankReconciliationBizObject.get("oppositeobjectid");
//            if (oppositetype != null) {
//                setOppositeobjectidToBizField(bankReconciliationBizObject, oppositetype, oppositeobjectId);
//            }
//            if (CollectionUtils.isNotEmpty(removeKeyArray)) {
//                for (String key : removeKeyArray) {
//                    bankReconciliationBizObject.remove(key);
//                }
//            }
//            //发布时进行乐观锁处理，即使用待认领金额和已认领金额进行发布过滤
//            if ("true".equals(bankReconciliationBizObject.get("pushFromOds"))) {
//                try {
//                    //单独走更新逻辑,待认领金额和已认领金额必须是零,并且关联状态是未关联
//                    int update = SqlHelper.update("com.yonyoucloud.fi.cmp.mapper.PullBankReconcliationMapper.updatePullBank", bankReconciliationBizObject);
//                    if (update == 0) {
//                        mark = true;
//                        Log.error(String.format("智能流水[%s]发布更新数据失败，存在并发操作，待认领金额和已认领金额存不同时为0！", bankReconciliationBizObject.get("bank_seq_no")));
//                    }
//                } catch (Exception e) {
//                    mark = true;
//                    Log.error(String.format("智能流水[%s]发布更新数据库异常：[%s]", bankReconciliationBizObject.get("bank_seq_no"), e.getMessage()));
//                }
//                bankReconciliationBizObject.remove("pushFromOds");
//            }
//        }
//        if (mark) {
//            return;
//        }
//        CommonBankReconciliationProcessor.batchBizObjectBeforeUpdate(bankReconciliationBizObjectList);
//        try {
//            MetaDaoHelper.update(BankReconciliation.ENTITY_NAME, bankReconciliationBizObjectList);
//        } catch (Exception e) {
//            log.error(String.format("执行=======================更新数据库，抛出异常，异常信息：%s", e.getMessage()));
//            throw new CtmException(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540067B", "执行=======================更新数据库，抛出异常，异常信息：%s") /* "执行=======================更新数据库，抛出异常，异常信息：%s" */, e.getMessage()));
//        }
//
//    }
//
//    /**
//     * 设置对方信息
//     * @param bizObject
//     * @param oppositetype
//     * @param oppositeobjectid
//     * @throws InstantiationException
//     * @throws IllegalAccessException
//     */
//    public static void setOppositeobjectidToBizField(BizObject bizObject, Short oppositetype, String oppositeobjectid) throws InstantiationException, IllegalAccessException {
//        //oppositetype需要判空，否则在进行比较前拆箱时，会报空指针
//        if (bizObject == null || oppositetype == null || oppositeobjectid == null) {
//            return;
//        }
//        if (oppositetype == OppositeType.Customer.getValue()) {
//            bizObject.set("merchant", oppositeobjectid);
//            bizObject.set("vendor", null);
//            bizObject.set("staff", null);
//            bizObject.set("innerorg", null);
//        } else if (oppositetype == OppositeType.Supplier.getValue()) {
//            bizObject.set("merchant", null);
//            bizObject.set("vendor", oppositeobjectid);
//            bizObject.set("staff", null);
//            bizObject.set("innerorg", null);
//        } else if (oppositetype == OppositeType.Employee.getValue()) {
//            String oppositeobjectidStr = (oppositeobjectid != null) ? oppositeobjectid.toString() : null;
//            bizObject.set("merchant", null);
//            bizObject.set("vendor", null);
//            bizObject.set("staff", oppositeobjectidStr);
//            bizObject.set("innerorg", null);
//        } else if (oppositetype == OppositeType.InnerOrg.getValue()) {
//            String oppositeobjectidStr = (oppositeobjectid != null) ? oppositeobjectid.toString() : null;
//            bizObject.set("merchant", null);
//            bizObject.set("vendor", null);
//            bizObject.set("staff", null);
//            bizObject.set("innerorg", oppositeobjectidStr);
//        }
//    }
//
//    /**
//     * 记录业务日志及调用链
//     *
//     * @param bankReconciliation
//     */
//    private static void recordLog(BankReconciliation bankReconciliation) {
//        try {
//            //业务日记记录
//            CtmJSONObject logparam = new CtmJSONObject();
//            // 增加调用链的开关，针对项目上调试时，想知道完整调用链时打开
//            if (ENABLE.equals(AppContext.cache().get("log.bankreconciliation.stacktrace"))) {
//                StringBuilder sb = new StringBuilder();
//                StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
//                // 从第 3 层开始打印，跳过 getStackTrace, updateBankReconciliation4Certain 自身
//                for (int i = 3; i < Math.min(stackTrace.length, 20); i++) {
//                    StackTraceElement element = stackTrace[i];
//                    // 可选：过滤掉一些框架包，只看业务包
//                    if (element.getClassName().startsWith("com.yonyoucloud.fi.cmp")) {
//                        sb.append("   -> ").append(element.toString()).append("\n");
//                    }
//                    logparam.put("stackTrace", sb.toString());//@notranslate
//                }
//            }
//            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
//            if (stackTrace.length > 3) {
//                String callerMethodName = stackTrace[3].getMethodName();
//                String callerClassName = stackTrace[3].getClassName();
//                logparam.put("callerMethodName ", callerMethodName);
//                logparam.put("callerClassName ", callerClassName);
//            }
//            logparam.put("bankReconciliation", bankReconciliation);
//            OperateSourceEnum operateSourceEnum = null;
//            if(bankReconciliation.get("operateSourceEnum") != null){
//                operateSourceEnum = (OperateSourceEnum)bankReconciliation.get("operateSourceEnum");
//            } else {
//                operateSourceEnum = OperateSourceEnum.DEFAULT_UPDATE;
//            }
//            AppContext.getBean(CTMCMPBusinessLogService.class).saveBusinessLog(logparam, ""+bankReconciliation.getBank_seq_no()+bankReconciliation.getId(),
//                    operateSourceEnum.getName(), IServicecodeConstant.CMPBANKRECONCILIATION, ICmpConstant.CMPBANKRECONCILIATION_RESID, operateSourceEnum.getNameResid());//@notranslate
//        } catch (Exception e) {
//            log.error("saveBusinessLog error", e);
//        }
//    }
//
//
//}
