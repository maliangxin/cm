package com.yonyoucloud.fi.cmp.intelligentdealdetail.converts;

import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.iuap.yms.id.generator.YmsOidGenerator;
import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVOWithRange;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyoucloud.fi.cmp.bankdealdetail.BankDealDetail;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.consts.DealDetailEnumConst;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailaccess.model.BankDealDetailODSModel;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.exception.BankDealDetailException;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.utils.DealDetailUtils;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.Direction;
import com.yonyoucloud.fi.cmp.util.DateUtils;
import com.yonyoucloud.fi.cmp.util.basedoc.EnterpriseBankQueryService;
import lombok.extern.slf4j.Slf4j;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class BankdealDetailOdsConvertManager {
    Map<String, String> convertBankReconciliationTOBankDealDetailMapping = new HashMap();

    Map<String, String> convertOdsTOBankReconciliationMapping = new HashMap();
    @Autowired
    YmsOidGenerator ymsOidGenerator;
    @Autowired
    private EnterpriseBankQueryService enterpriseBankQueryService;
    /**
     * 将银行对账单数据转为账户交易明细
     *
     * @param bankReconciliationList
     * @return
     */
    public List<BankDealDetail> convertBankReconciliationTOBankDealDetail(List<BankReconciliation> bankReconciliationList) {
        List<BankDealDetail> bankDealDetailList = new ArrayList<>();
        try {
            for (BankReconciliation bankReconciliation : bankReconciliationList) {
                BankDealDetail bankDealDetail = new BankDealDetail();
                convertBankReconciliationTOBankDealDetailMapping.entrySet().stream().forEach(entry -> {
                    String detailField = entry.getKey();
                    String reconciliationField = entry.getValue();
                    if (bankReconciliation.get(reconciliationField) != null) {
                        bankDealDetail.put(detailField, bankReconciliation.get(reconciliationField));
                    }
                });
                //银行类别
                try {
                    EnterpriseBankAcctVOWithRange enterpriseBankAcctVoWithRange = enterpriseBankQueryService.queryEnterpriseBankAcctVOWithRangeById(bankReconciliation.getBankaccount());
                    bankDealDetail.setBanktype(enterpriseBankAcctVoWithRange.getBank());
                } catch (Exception e) {
                    log.error("获取银行类别异常：",e);
                }
                //创建日期
                bankDealDetail.setCreateDate(new Date());
                bankDealDetail.setEntityStatus(EntityStatus.Insert);
                bankDealDetailList.add(bankDealDetail);
            }
            bankDealDetailList.stream().forEach(e -> {
                if(e.get("rate") != null){
                    e.setRate(new BigDecimal(e.get("rate").toString()));
                }
            });
        }catch (Exception e){
            throw new BankDealDetailException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054003BC", "ods转交易明细异常，异常原因：") /* "ods转交易明细异常，异常原因：" */+e.getMessage(),e);
        }

        return bankDealDetailList;
    }

    /**
     * 将ods数据转为银行对账单数据
     *
     * @param bankDealDetailResponseRecords
     * @return
     */
    public List<BankReconciliation> convertOdsTOBankReconciliation(List<BankDealDetailODSModel> bankDealDetailResponseRecords) {
        List<BankReconciliation> returnBankReconciliationList = new ArrayList<>();
        try {
            if (CollectionUtils.isEmpty(bankDealDetailResponseRecords)) {
                return null;
            }
            int originCount = bankDealDetailResponseRecords.size();
            /**
             * step1:如果ods对应的流水在流水库存在，则从DB查询
             * */
            this.firstGetBankReconciliationFromDBByMainId(returnBankReconciliationList, bankDealDetailResponseRecords, originCount);

            /**
             * step2:如果流水在DB中不存在，则将BankDealDetailODSModel转流水
             * */
            if (CollectionUtils.isEmpty(bankDealDetailResponseRecords)) {
                return returnBankReconciliationList;
            }
            this.getBankReconciliationFromOdsModel(returnBankReconciliationList, bankDealDetailResponseRecords);

            if (returnBankReconciliationList.size() != originCount) {
                throw new BankDealDetailException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054003BA", "【ods转流水】流水数量处理有误") /* "【ods转流水】流水数量处理有误" */);
            }
            // ods没有dzdate，在这里需要赋值一下
            returnBankReconciliationList.stream().forEach(e->{
                try {
                    Date tranDate = DateUtils.dateParse(e.get("tran_date"), DateUtils.YYYYMMDD);
                    e.setDzdate(tranDate);
                }catch (Exception e2){
                    throw new BankDealDetailException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054003B9", "流水dzdate格式转换异常，异常原因：") /* "流水dzdate格式转换异常，异常原因：" */+e2.getMessage(),e2);
                }
            });
        }catch (Exception e){
            throw new BankDealDetailException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054003BB", "ods转流水异常，异常原因：") /* "ods转流水异常，异常原因：" */+e.getMessage(),e);
        }
        return returnBankReconciliationList;
    }

    private void getBankReconciliationFromOdsModel(List<BankReconciliation> returnBankReconciliationList, List<BankDealDetailODSModel> bankDealDetailResponseRecords) {
        List<Map> bankDealDetailODSModels = new ArrayList<>();
        bankDealDetailResponseRecords.stream().forEach(e -> {
            bankDealDetailODSModels.add(this.objectToMap(e));
        });

        for (Map bankDealDetailODSModel : bankDealDetailODSModels) {
            BankReconciliation bankReconciliation = new BankReconciliation();
            convertOdsTOBankReconciliationMapping.entrySet().stream().forEach(entry -> {
                String odsField = entry.getKey();
                String reconciliationField = entry.getValue();
                if (bankDealDetailODSModel.get(odsField) != null) {
                    bankReconciliation.put(reconciliationField, bankDealDetailODSModel.get(odsField));
                }
            });
            //解析财资统一对账码
            DealDetailUtils.setSmartCheckNoInfo(bankReconciliation);
            bankReconciliation.put(DealDetailEnumConst.ODSID, bankDealDetailODSModel.get("id"));
            bankReconciliation.setId(ymsOidGenerator.nextId());
            if (bankReconciliation.getDc_flag().getValue()== Direction.Credit.getValue()){
                //收入金额
                bankReconciliation.setCreditamount(bankReconciliation.getTran_amt());
            }else {
                //支出金额
                bankReconciliation.setDebitamount(bankReconciliation.getTran_amt());
            }
            //第三方流水号
            bankReconciliation.setThirdserialno(bankReconciliation.getBank_seq_no());
            bankReconciliation.setYtenantId(InvocationInfoProxy.getTenantid().toString());
            //创建日期
            bankReconciliation.setCreateTime(new Date());
            //银行类别
            try {
                //解析财资统一对账码
                DealDetailUtils.setSmartCheckNoInfo(bankReconciliation);
                //银行类别
                EnterpriseBankAcctVOWithRange enterpriseBankAcctVoWithRange = enterpriseBankQueryService.queryEnterpriseBankAcctVOWithRangeById(bankReconciliation.getBankaccount());
                bankReconciliation.setBanktype(enterpriseBankAcctVoWithRange.getBank());
            } catch (Exception e) {
               log.error("获取银行类别异常：",e);
            }

            bankReconciliation.setEntityStatus(EntityStatus.Insert);
            returnBankReconciliationList.add(bankReconciliation);
        }
    }

    private void firstGetBankReconciliationFromDBByMainId(List<BankReconciliation> returnBankReconciliationList, List<BankDealDetailODSModel> bankDealDetailResponseRecords, int originCount) {
        /**
         * step1: 从流水库查询 如果存在则直接获取
         * */
        List<Long> bankDealDetailIds = bankDealDetailResponseRecords.stream().map(BankDealDetailODSModel::getMainid).collect(Collectors.toList());
        QuerySchema querySchema = new QuerySchema().addSelect("*");
        QueryConditionGroup conditionGroup = QueryConditionGroup.and(org.imeta.orm.schema.QueryCondition.name("id").in(bankDealDetailIds));
        querySchema.addCondition(conditionGroup);
        List<BankReconciliation> bankReconciliations = null;
        try {
            bankReconciliations = MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME, querySchema, null);
            //根据流水id去重
            if (!CollectionUtils.isEmpty(bankReconciliations)) {
                Map<Long, BankReconciliation> bankReconciliationMap = bankReconciliations.stream().collect(Collectors.toMap(BankReconciliation::getId, Function.identity(), (k1, k2) -> k1));
                Collection<BankReconciliation> collection = bankReconciliationMap.values();
                Set<Long> existsBankReconciliationIds = bankReconciliationMap.keySet();
                returnBankReconciliationList.addAll(collection);
                //移除ods
                Iterator<BankDealDetailODSModel> iterator = bankDealDetailResponseRecords.iterator();
                while (iterator.hasNext()) {
                    BankDealDetailODSModel odsModel = iterator.next();
                    if (existsBankReconciliationIds.contains(odsModel.getMainid())) {
                        iterator.remove();
                    }
                }
            }
            if (returnBankReconciliationList.size() + bankDealDetailResponseRecords.size() != originCount) {
                throw new BankDealDetailException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054003BA", "【ods转流水】流水数量处理有误") /* "【ods转流水】流水数量处理有误" */);
            }
        } catch (Exception e) {
            log.error("【ods转流水】查询流水异常", e);
            throw new BankDealDetailException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054003BD", "【ods转流水】查询流水异常,") /* "【ods转流水】查询流水异常," */ + e.getMessage());
        }

    }

    public Map<String, Object> objectToMap(Object obj) {
        Map<String, Object> map = new HashMap<>();
        try {
            // 获取对象的类
            Class<?> clazz = obj.getClass();
            // 获取类中声明的所有字段
            Field[] fields = clazz.getDeclaredFields();
            for (Field field : fields) {
                // 设置字段可访问，以便于读取私有字段
                field.setAccessible(true);
                // 获取字段名和值
                String fieldName = field.getName();
                Object fieldValue = field.get(obj);
                // 将字段名和值放入Map中
                map.put(fieldName, fieldValue);
            }
        } catch (Exception e) {
            log.error("转换map异常：{}", e);
        }
        return map;
    }

    @PostConstruct
    public void init() {
        // 获取MyClass的Class对象
        Class<?> clazz = BaseConvertModel.class;
        String value = null;
        // 获取myField成员变量
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            DictColumn dictColumn = field.getAnnotation(DictColumn.class);
            if (null == dictColumn) {
                continue;
            }
            if (dictColumn.detailField() != null && dictColumn.reconciliationField() != null) {
                convertBankReconciliationTOBankDealDetailMapping.put(dictColumn.detailField(), dictColumn.reconciliationField());
            }
            if (dictColumn.reconciliationField() != null && dictColumn.odsField() != null) {
                convertOdsTOBankReconciliationMapping.put(dictColumn.odsField(), dictColumn.reconciliationField());
            }
        }
    }
}
