package com.yonyoucloud.fi.cmp.denominationSetting.rule;

import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.denominationSetting.DenominationSetting;
import com.yonyoucloud.fi.cmp.denominationSetting.DenominationSetting_b;
import com.yonyoucloud.fi.cmp.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component("denominationsettingSaveRule")
public class DenominationsettingSaveRule extends AbstractCommonRule {
    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        List<BizObject> bills = getBills(billContext, map);
        if (bills != null && bills.size() > 0) {
            BizObject bizObject = bills.get(0);

            //获取子表数据
            List<DenominationSetting_b> denominationSetting_bs = bizObject.get("DenominationSetting_b");

            if (denominationSetting_bs == null || denominationSetting_bs.size() < 1) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100372"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041801AF","子表数据不能为空") /* "子表数据不能为空" */);
            }
            //币种
            String currency = bizObject.get("currency");

            if(EntityStatus.Insert.equals(bizObject.get("_status"))){
                checkCurrency(currency);
            }

            //基本单位
            String baseUnit = bizObject.get("baseUnit");


            if (StringUtils.isEmpty(baseUnit)) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100373"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041801AD","基本单位不能为空！") /* "基本单位不能为空！" */);
            }

            if (baseUnit.length() > 25) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100374"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041801B0","基本单位最大长度不能超过25个汉字或者字符") /* "基本单位最大长度不能超过25个汉字或者字符" */);
            }

            StringBuilder currencyDenomination = new StringBuilder();

            //获取子表银行账户

            for (DenominationSetting_b denominationSetting_b:denominationSetting_bs){
                if(EntityStatus.Delete.equals(denominationSetting_b.get("_status"))){
                    continue;
                }
                currencyDenomination.append(denominationSetting_b.getCurrencyDenomination());
                currencyDenomination.append(";");
            }

            //主表货币面额
            bizObject.set("currencyDenomination",currencyDenomination.toString());

            //校验子表数据
            checkData_b(denominationSetting_bs);

        }

        return new RuleExecuteResult();
    }

    /**
     * 校验币种
     * @param currency
     * @throws Exception
     */
    private void checkCurrency(String currency) throws Exception {
        if (StringUtils.isEmpty(currency)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100375"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041801B2","币种不能为空！") /* "币种不能为空！" */);
        }

       QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("currency").eq(currency));

       QuerySchema schema = QuerySchema.create().addSelect("id");
       schema.addCondition(group);

       List<Map<String,Object>> list = MetaDaoHelper.query(DenominationSetting.ENTITY_NAME, schema);

        if (CollectionUtils.isNotEmpty(list)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100376"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041801AE","该币种的货币面额已存在，不允许保存！") /* "该币种的货币面额已存在，不允许保存！" */);
        }
    }

    /**
     * 校验子表数据
     * @param denominationSetting_bs
     * @throws Exception
     */
    private void checkData_b(List<DenominationSetting_b> denominationSetting_bs) throws Exception{

        Set<String> currencyDenominations = new HashSet<>();
        //校验页面传过来的数据
        for(DenominationSetting_b denominationSetting_b : denominationSetting_bs){
            BigDecimal conversionUnit = denominationSetting_b.get("conversionUnit");
            String currencyDenomination = denominationSetting_b.get("currencyDenomination");

            if (currencyDenomination.length() > 25) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100377"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041801B1","子表货币面额项最大长度不能超过25个汉字或者字符") /* "子表货币面额项最大长度不能超过25个汉字或者字符" */);
            }
            if (conversionUnit.compareTo(BigDecimal.ZERO) <= 0) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100378"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041801B3","明细换算基本单位项必须大于0") /* "明细换算基本单位项必须大于0" */);
            }
            if(EntityStatus.Delete.equals(denominationSetting_b.get("_status"))){
                continue;
            }
            if (!currencyDenominations.add(denominationSetting_b.getCurrencyDenomination())) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100379"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041801AC","子表货币面额不可以重复！") /* "子表货币面额不可以重复！" */);
            }
        }

    }

}
