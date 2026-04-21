package com.yonyoucloud.fi.cmp.flowhandlesetting.business;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.flowhandlesetting.Flowhandlesetting;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author guoxh
 */
@Slf4j
@Component("flowHandleSettingBeforeSaveRule")
public class FlowHandleSettingBeforeSaveRule extends AbstractCommonRule {
    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        List<BizObject> bills = getBills(billContext, paramMap);
        if (CollectionUtils.isEmpty(bills)) {
            new RuleExecuteResult();
        }
        BizObject bizObject = bills.get(0);
        if (bizObject.containsKey("code") && StringUtils.isEmpty(bizObject.getString("code"))) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102012"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1C4CD0DA05180008", "流程编码不能为空"));
        }
        if (bizObject.containsKey("name") && StringUtils.isEmpty(bizObject.getString("name"))) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102013"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1C4CD10E05180009", "流程名称不能为空"));
        }
        boolean flag = false;
        if (bizObject.getEntityStatus().equals(EntityStatus.Insert)) {
            flag = this.checkValueExist("code", bizObject.getString("code"), null);
            if (flag) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102014"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1C4CD34A04D80003", "流程编码[%s]已存在"), bizObject.getString("code")));
            }
            flag = this.checkValueExist("name", bizObject.getString("name"), null);
            if (flag) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102015"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1C4CD37404D80003", "流程名称[%s]已存在"), bizObject.getString("name")));
            }
        } else if (bizObject.getEntityStatus().equals(EntityStatus.Update)) {
            flag = this.checkValueExist("code", bizObject.getString("code"), bizObject.getId());
            if (flag) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102014"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1C4CD34A04D80003", "流程编码[%s]已存在"), bizObject.getString("code")));
            }
            flag = this.checkValueExist("name", bizObject.getString("name"), bizObject.getId());
            if (flag) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102015"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1C4CD37404D80003", "流程名称[%s]已存在"), bizObject.getString("name")));
            }
        }
        flag = checkSortUnique(bizObject);
        if (flag) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102377"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1C4CD3AA05180002", "当前流程环节和适用对象下已存在执行顺序[%s],请检查后重试！"), bizObject.getString("sortNum")));
        }
        bizObject.set("accentity", "666666");
        if(bizObject.getInteger("flowType") == 1){
            Short isArtiConfirm = bizObject.getShort("isArtiConfirm1");
            bizObject.set("isArtiConfirm", isArtiConfirm);
            Short isRandomAutoConfirm = bizObject.getShort("isRandomAutoConfirm1");
            bizObject.set("isRandomAutoConfirm", isRandomAutoConfirm);
            String msgType = bizObject.getString("msgType1");
            bizObject.set("msgType", msgType);
        }else if(bizObject.getInteger("flowType") == 2){
            Short isArtiConfirm = bizObject.getShort("isArtiConfirm2");
            bizObject.set("isArtiConfirm", isArtiConfirm);
            Short isRandomAutoConfirm = bizObject.getShort("isRandomAutoConfirm2");
            bizObject.set("isRandomAutoConfirm", isRandomAutoConfirm);
            String msgType = bizObject.getString("msgType2");
            bizObject.set("msgType", msgType);
        }else if(bizObject.getInteger("flowType") == 3){
            Short isArtiConfirm = bizObject.getShort("isArtiConfirm3");
            bizObject.set("isArtiConfirm", isArtiConfirm);
            Short isRandomAutoConfirm = bizObject.getShort("isRandomAutoConfirm3");
            bizObject.set("isRandomAutoConfirm", isRandomAutoConfirm);
            String msgType = bizObject.getString("msgType3");
            bizObject.set("msgType", msgType);
        }else if(bizObject.getInteger("flowType") == 4){
            String msgType = bizObject.getString("msgType4");
            bizObject.set("msgType", msgType);
        }
        return new RuleExecuteResult();
    }

    /**
     * @param field
     * @param value
     * @param excludeId
     * @return
     * @throws Exception
     */
    private boolean checkValueExist(String field, String value, Long excludeId) throws Exception {
        QuerySchema querySchema = QuerySchema.create().addSelect("id")
                .appendQueryCondition(
                        QueryCondition.name(field).eq(value)
                );
        if (excludeId != null) {
            querySchema.appendQueryCondition(
                    QueryCondition.name("id").not_eq(excludeId)
            );
        }
        List<BizObject> list = MetaDaoHelper.query(Flowhandlesetting.ENTITY_NAME, querySchema);
        return CollectionUtils.isNotEmpty(list);
    }

    public boolean checkSortUnique(BizObject bizObject) throws Exception {
        if (!bizObject.containsKey("object") || bizObject.get("object") == null) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102378"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1C4CD42804D80002", "适用对象不能为空"));
        }
        if (!bizObject.containsKey("flowType") || bizObject.get("flowType") == null) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102379"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1C4CD3FE05180009", "流程处理环节不能为空"));
        }
        if (!bizObject.containsKey("sortNum") || bizObject.get("sortNum") == null) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102380"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1C523D0E05580005", "执行优先级不能为空"));
        }
        QuerySchema querySchema = QuerySchema.create().addSelect("id")
                .appendQueryCondition(
                        QueryCondition.name("flowType").eq(bizObject.get("flowType")),
                        QueryCondition.name("object").eq(bizObject.get("object")),
                        QueryCondition.name("sortNum").eq(bizObject.get("sortNum"))
                );
        if (bizObject.getEntityStatus().equals(EntityStatus.Update)) {
            querySchema.appendQueryCondition(
                    QueryCondition.name("id").not_eq(bizObject.getId())
            );
        }
        List<Map<String, Object>> list = MetaDaoHelper.query(Flowhandlesetting.ENTITY_NAME, querySchema, null);
        return CollectionUtils.isNotEmpty(list);
    }
}
