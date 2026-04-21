package com.yonyoucloud.fi.cmp.margintype.business;


import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.margintype.MarginType;
import lombok.extern.slf4j.Slf4j;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 保证金类型保存前规则*
 *
 */

@Slf4j
@Component
public class MarginTypeBeforeSaveRule extends AbstractCommonRule {

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        BizObject bill = getBills(billContext, map).get(0);
        QuerySchema querySchema = QuerySchema.create().addSelect("*");
        List<MarginType> list = MetaDaoHelper.queryObject(MarginType.ENTITY_NAME, querySchema, null);
        if (bill.get("_status").equals(EntityStatus.Insert)) {
            //保证金类型
            List<String> codes = list.stream().map(MarginType::getCode).collect(Collectors.toList());
            //保证金类型名称
            List<String> typeNames = list.stream().map(MarginType::getTypename).collect(Collectors.toList());
            if (codes.contains(bill.get("code"))) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102405"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault(
                        "UID:P_CM-BE_1942EAB204F00008", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400794", "类型编码已存在，不允许重复添加！") /* "类型编码已存在，不允许重复添加！" */) /* "类型编码已存在，不允许重复添加" */);
            }
            ;
            if (typeNames.contains(bill.get("typename"))) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102406"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault(
                        "UID:P_CM-BE_1942EC1204F00001", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400793", "类型名称已存在，不允许重复添加！") /* "类型名称已存在，不允许重复添加！" */) /* "类型名称已存在，不允许重复添加" */);
            }
        }
        if (bill.get("_status").equals(EntityStatus.Update)) {
            MarginType marginType = MetaDaoHelper.findById(MarginType.ENTITY_NAME, bill.get("id"));
            //保证金类型
            List<String> codes = list.stream().map(MarginType::getCode).collect(Collectors.toList());
            List<String> filterCodes = codes.stream().filter(item -> !item.equals(marginType.getCode())).collect(Collectors.toList());
            //保证金类型名称
            List<String> typeNames = list.stream().map(MarginType::getTypename).collect(Collectors.toList());
            List<String> filterTypeNames = typeNames.stream().filter(item -> !item.equals(marginType.getTypename())).collect(Collectors.toList());

            if (filterCodes.contains(bill.get("code"))) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102407"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault(
                        "UID:P_CM-BE_1942EAB204F00008", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400794", "类型编码已存在，不允许重复添加！") /* "类型编码已存在，不允许重复添加！" */) /* "类型编码已存在，不允许重复添加" */);
            }
            ;
            if (filterTypeNames.contains(bill.get("typename"))) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102408"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault(
                        "UID:P_CM-BE_1942EC1204F00001", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400793", "类型名称已存在，不允许重复添加！") /* "类型名称已存在，不允许重复添加！" */) /* "类型名称已存在，不允许重复添加" */);
            }
        }

        return new RuleExecuteResult();
    }
}
