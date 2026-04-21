package com.yonyoucloud.fi.cmp.accountdetailexclusion.service;

import com.yonyou.ucf.basedoc.model.ExchangeRateTypeVO;
import com.yonyou.ucf.basedoc.service.itf.IExchangeRateTypeService;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.accountdetailexclusion.AccountDetailExclusion_b;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.bankreconciliation.CommonSaveUtils;
import com.yonyoucloud.fi.cmp.cmpentity.CullingStatus;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.Direction;
import com.yonyoucloud.fi.cmp.util.QueryBaseDocUtils;
import com.yonyoucloud.fi.cmp.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * IAccountDetailExclusionCommonService
 *
 * @author jpk
 * @version 1.0
 */
@Service
@Slf4j
@Transactional(rollbackFor = RuntimeException.class)
@RequiredArgsConstructor
public class AccountDetailExclusionServiceImpl implements IAccountDetailExclusionService {
    @Autowired
    private IExchangeRateTypeService exchangeRateTypeService;

    @Override
    public void updateBankreconciliationExclusion(Long accountDetailExclusionId, CullingStatus cullingStatus) throws Exception {
        QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name(ICmpConstant.MAINID).eq(accountDetailExclusionId));
        QuerySchema schema = QuerySchema.create().addSelect(ICmpConstant.BANK_RECONCILIATION_ID);
        schema.addCondition(group);
        List<Map<String, Object>> accountDetailList = MetaDaoHelper.query(AccountDetailExclusion_b.ENTITY_NAME, schema);
        if (accountDetailList != null && accountDetailList.size() > 0) {
            Long[] ids = accountDetailList.stream().map(e -> Long.parseLong(e.get(ICmpConstant.BANK_RECONCILIATION_ID).toString())).toArray(Long[]::new);
            QuerySchema bankschema = QuerySchema.create().addSelect("*");
            QueryConditionGroup condition = new QueryConditionGroup();
            condition.addCondition(QueryConditionGroup.and(QueryCondition.name(ICmpConstant.ID).in(ids)));
            bankschema.addCondition(condition);
            List<BankReconciliation> bankInfos = MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME, bankschema, null);
            bankInfos.stream().forEach(e -> {
                e.setEliminateStatus(cullingStatus.getValue());
                e.setEntityStatus(EntityStatus.Update);
            });
            CommonSaveUtils.updateBankReconciliation(bankInfos, null);
        }
    }


    /**
     * 计算收入支出剔除总额
     *
     * @param param
     * @return
     * @throws Exception
     */
    @Override
    public CtmJSONObject calculateExcludingAmount(CtmJSONObject param, HttpServletResponse response) throws Exception {
        CtmJSONObject jsonObject = new CtmJSONObject();
        BigDecimal incomeexclusionamount = BigDecimal.ZERO;//收入剔除总额
        BigDecimal payexclusionamount = BigDecimal.ZERO;//支出剔除总额
        BigDecimal convertincomeexclusionamount = BigDecimal.ZERO;// 折算收入剔除总额
        BigDecimal convertpayexclusionamount = BigDecimal.ZERO;// 折算支出剔除总额
        try {
            String targetCurrency = (String) param.get("convertcurrency");
            ArrayList commonVOs = (ArrayList) param.get("rows");
            if (commonVOs != null && commonVOs.size() > 0) {
                ExchangeRateTypeVO defaultExchangeRateType = exchangeRateTypeService.getDefaultExchangeRateType();
                String exchangeRateTypeId = defaultExchangeRateType.getId();

                for (Object commonVO : commonVOs) {
                    LinkedHashMap<String, Object> listLinkedHashMap = (LinkedHashMap<String, Object>) commonVO;
                    BigDecimal tran_amt = new BigDecimal(String.valueOf(listLinkedHashMap.get("tran_amt")));
                    BigDecimal eliminate_amt = new BigDecimal(String.valueOf(listLinkedHashMap.get("eliminate_amt")));
                    BigDecimal after_eliminate_amt = tran_amt.subtract(eliminate_amt);
                    listLinkedHashMap.put("after_eliminate_amt", after_eliminate_amt);

                    String sourceCurrency = String.valueOf(listLinkedHashMap.get("currency"));

                    BigDecimal exchRate = BigDecimal.ONE;
                    if (!sourceCurrency.equals(targetCurrency)) {
                        // 根据交易日期获取汇率
                        String dateStr = (String) (Optional.ofNullable(listLinkedHashMap.get("tran_date")).orElse(""));
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                        LocalDate quotationDate = StringUtils.isEmpty(dateStr) ? LocalDate.now() : LocalDate.parse(dateStr, formatter);

                        Map<String, Object> queryCondition = new HashMap<>();
                        queryCondition.put("exchangeRateType", exchangeRateTypeId);
                        queryCondition.put("sourceCurrencyId", sourceCurrency);
                        queryCondition.put("targetCurrencyId", targetCurrency);
                        queryCondition.put("quotationDate", quotationDate);
                        queryCondition.put("dr", 0);
                        queryCondition.put("enable", 1);
                        List<Map<String, Object>> queryResult = QueryBaseDocUtils.queryExchangeRateByCondition(queryCondition);
                        if (queryResult.isEmpty()) {
                            // 当正向没有，反查间接汇率
                            Map<String, Object> indirectQueryCondition = new HashMap<>();
                            indirectQueryCondition.put("exchangeRateType", exchangeRateTypeId);
                            indirectQueryCondition.put("sourceCurrencyId", targetCurrency);
                            indirectQueryCondition.put("targetCurrencyId", sourceCurrency);
                            indirectQueryCondition.put("quotationDate", quotationDate);
                            indirectQueryCondition.put("dr", 0);
                            indirectQueryCondition.put("enable", 1);
                            List<Map<String, Object>> indirectQueryResult = QueryBaseDocUtils.queryExchangeRateByCondition(indirectQueryCondition);
                            if (indirectQueryResult.isEmpty()) {
                                exchRate = BigDecimal.ZERO;
                            } else {
                                Map<String, Object> indirectExchRateInfo = indirectQueryResult.get(0);
                                exchRate = (BigDecimal) indirectExchRateInfo.get("indirectExchangeRate");
                            }
                        } else {
                            Map<String, Object> exchRateInfo = queryResult.get(0);
                            exchRate = (BigDecimal) exchRateInfo.get("exchangeRate");
                        }
                    }
                    BigDecimal convert_eliminate_amt = eliminate_amt.multiply(exchRate);

                    Short dc_flag = Short.parseShort(String.valueOf(listLinkedHashMap.get("dc_flag")));
                    if (dc_flag == Direction.Credit.getValue()) {
                        incomeexclusionamount = incomeexclusionamount.add(eliminate_amt);
                        convertincomeexclusionamount = convertincomeexclusionamount.add(convert_eliminate_amt);
                    } else {
                        payexclusionamount = payexclusionamount.add(eliminate_amt);
                        convertpayexclusionamount = convertpayexclusionamount.add(convert_eliminate_amt);
                    }
                }
            }
            jsonObject.put("incomeexclusionamount", incomeexclusionamount);//收入剔除总额
            jsonObject.put("payexclusionamount", payexclusionamount);//支出剔除总额
            jsonObject.put("convertincomeexclusionamount", convertincomeexclusionamount);//折算收入剔除总额
            jsonObject.put("convertpayexclusionamount", convertpayexclusionamount);//折算支出剔除总额
            jsonObject.put("rows", commonVOs);
            jsonObject.put(ICmpConstant.MSG, ICmpConstant.SUCCESS);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return jsonObject;
    }


}
