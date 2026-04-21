package com.yonyoucloud.fi.cmp.foreignpayment.service;

import com.google.common.collect.Lists;
import com.yonyou.ucf.mdd.ext.bpm.model.VerifyState;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.cmpentity.EventSource;
import com.yonyoucloud.fi.cmp.cmpentity.EventType;
import com.yonyoucloud.fi.cmp.foreignpayment.ForeignPayment;
import com.yonyoucloud.fi.cmp.fundcollection.FundCollection;
import com.yonyoucloud.fi.cmp.fundpayment.FundPayment;
import com.yonyoucloud.fi.cmp.payapplicationbill.PayApplicationBill;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.imeta.orm.schema.SimpleCondition;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.yonyoucloud.fi.cmp.constant.IBillNumConstant.*;
import static com.yonyoucloud.fi.cmp.constant.ICmpConstant.*;
import static com.yonyoucloud.fi.cmp.constant.ICmpConstant.ID;

@Service
@Slf4j
public class ForeignPaymentOpenApiServiceImpl implements ForeignPaymentOpenApiService {

    @Override
    public String queryBillByIdOrCode(String billNum, Long id, String code) throws Exception {
        QuerySchema querySchemaJ = QuerySchema.create().addSelect("*");
        if (ValueUtils.isNotEmptyObj(code)) {
            querySchemaJ.addCondition(QueryConditionGroup.and(QueryCondition.name("code").eq(code)));
        }
        if (ValueUtils.isNotEmptyObj(id)) {
            querySchemaJ.addCondition(QueryConditionGroup.and(QueryCondition.name("id").eq(id)));
        }
        List<BizObject> mapList = MetaDaoHelper.queryObject(ForeignPayment.ENTITY_NAME, querySchemaJ, null);
        if (CollectionUtils.isNotEmpty(mapList)) {
            return ResultMessage.data(mapList.get(0));
        } else {
            CtmJSONObject jsonObject = new CtmJSONObject();
            if (ValueUtils.isNotEmptyObj(id)) {
                jsonObject.put("message", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418066D", "单据不存在 id:") /* "单据不存在 id:" */
                        /* "单据不存在 id:" */ + id);
                jsonObject.put("id", id);
            }
            if (ValueUtils.isNotEmptyObj(code)) {
                jsonObject.put("code", code);
                jsonObject.put("message", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180670", "单据不存在 code:") /* "单据不存在 code:" */
                        /* "单据不存在 code:" */ + code);
            }
            return ResultMessage.data(999L, CtmJSONObject.toJSONString(jsonObject), new CtmJSONObject());
        }
    }

    @Override
    public CtmJSONObject deleteBillByIds(CtmJSONObject param) throws Exception {
        CtmJSONArray rows = param.getJSONArray("data");
        String billNum = param.getString(BILL_NUM);
        String fullName = ForeignPayment.ENTITY_NAME;
        List<String> messages = new ArrayList<>();
        int failedCount = 0;
        List<Object> ids = new ArrayList<>();
        Map<Long, BizObject> payBillMap = new HashMap<>(CONSTANT_EIGHT);
        for (int i = 0; i < rows.size(); i++) {
            CtmJSONObject rowData = rows.getJSONObject(i);
            Long id = rowData.getLong(ID);
            ids.add(id);
        }
        QuerySchema querySchema = QuerySchema.create().addSelect("id, verifystate,srcitem");
        QueryConditionGroup queryConditionGroup = new QueryConditionGroup(ConditionOperator.and);
        queryConditionGroup.appendCondition(QueryCondition.name(ID).in(ids));
        querySchema.addCondition(queryConditionGroup);
        List<BizObject> bizObjectList = MetaDaoHelper.queryObject(fullName, querySchema, null);
        for (BizObject bizObject : bizObjectList) {
            payBillMap.put(bizObject.getId(), bizObject);
        }
        List<Long> deleteIds = new ArrayList<>(rows.size());
        for (int i = 0; i < rows.size(); i++) {
            CtmJSONObject row = rows.getJSONObject(i);
            BizObject bizObject = payBillMap.get(row.getLong("id"));
            if (!ValueUtils.isNotEmptyObj(bizObject)) {
                messages.add(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180669", "单据不存在 id:") /* "单据不存在 id:" */ + rows.getJSONObject(i).getLong("id"));
                failedCount++;
                if (rows.size() == 1) {
                    return getJsonObject(rows, messages, failedCount);
                }
                continue;
            }

            if (ValueUtils.isNotEmptyObj(bizObject.get("verifystate"))) {
                short verifyState = Short.parseShort(bizObject.get("verifystate").toString());
                if (verifyState == VerifyState.SUBMITED.getValue()
                        || verifyState == VerifyState.COMPLETED.getValue()) {
                    messages.add(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418066F", "单据id: [%s]，当前单据状态不允许删除") /* "单据id: [%s]，当前单据状态不允许删除" */, rows.getJSONObject(i).getLong("id")));
                    failedCount++;
                    if (rows.size() == 1) {
                        return getJsonObject(rows, messages, failedCount);
                    }
                    continue;
                }
            }
            deleteIds.add(bizObject.getId());
        }
        if (ValueUtils.isNotEmptyObj(deleteIds)) {
            MetaDaoHelper.batchDelete(fullName,
                    Lists.newArrayList(new SimpleCondition("id", ConditionOperator.in, deleteIds.toArray(new Long[0]))));
        }
        return getJsonObject(rows, messages, failedCount);
    }

    private CtmJSONObject getJsonObject(CtmJSONArray rows, List<String> messages, int failedCount) {
        CtmJSONObject responseData = new CtmJSONObject();
        responseData.put("count", rows.size());
        responseData.put("successCount", rows.size() - failedCount);
        responseData.put("failCount", failedCount);
        responseData.put("messages", messages);
        responseData.put("infos", rows);
        return responseData;
    }
}
