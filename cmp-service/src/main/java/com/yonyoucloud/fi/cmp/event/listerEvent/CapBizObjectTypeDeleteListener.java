package com.yonyoucloud.fi.cmp.event.listerEvent;

import com.yonyou.diwork.exception.BusinessException;
import com.yonyou.iuap.event.model.BusinessEvent;
import com.yonyou.iuap.event.rpc.IEventReceiveService;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.filter.util.StringUtil;
import com.yonyou.ucf.mdd.ext.option.model.vo.EventResponseVO;
import com.yonyou.workbench.util.JsonUtils;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import lombok.extern.slf4j.Slf4j;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 资金业务对象类型删除校验
 * @author xuxbo
 * @date 2022/11/1 15:16
 */

@Slf4j
@Service
public class CapBizObjectTypeDeleteListener implements IEventReceiveService {
    @Autowired
    private CapBizManager capBizManager;

    private static List<String> getEientyName(){
        List<String> list = new ArrayList<>();
        list.add("cmp.fundcollection.FundCollection_b");
        list.add("cmp.fundpayment.FundPayment_b");
        return list;

    }

    /**
     * 事件源*
     */
    private static final String SOURCE_ID = "tmps-refer";

    /**
     * 事件类型*
     */
    private static final String EVENT_TYPE = "fund-biz-obj-type-delete-resp";

    @Override
    public String onEvent(BusinessEvent businessEvent, String s) throws BusinessException {

        if (ICmpConstant.CMDOMAIN.equals(s)) {
            String deleteStr = businessEvent.getUserObject();
            try {
                CtmJSONObject json = CtmJSONObject.parseObject(deleteStr);
                if (json.get(ICmpConstant.DATA) != null && json.get(ICmpConstant.DATA) instanceof ArrayList) {
                    ArrayList array = (ArrayList) json.get(ICmpConstant.DATA);
                    json.put("referenceSide", ICmpConstant.CMDOMAIN);
                    if (array.size() > 0){
                        LinkedHashMap jsonObject = (LinkedHashMap) array.get(0);
                        //资金业务对象类型id
                        String fundbusinobjtypeid = jsonObject.get(ICmpConstant.ID).toString();
                        if (StringUtil.isNotEmpty(fundbusinobjtypeid)) {
                            List<String> caobjList = getEientyName();
                            for (String caobj : caobjList) {
                                // 资金收、付款单子表
                                QuerySchema querySchema = QuerySchema.create().addSelect(ICmpConstant.ID);
                                QueryConditionGroup queryConditionGroup = QueryConditionGroup.and(
                                        QueryCondition.name("fundbusinobjtypeid").eq(fundbusinobjtypeid));
                                querySchema.addCondition(queryConditionGroup);
                                List<Map<String, Object>> list = MetaDaoHelper.query(caobj, querySchema, null);
                                if (ValueUtils.isNotEmpty(list)) {
                                    json.put("canDeleted", false);
                                    capBizManager.sendToTmsp(SOURCE_ID, EVENT_TYPE, json);
                                    return JsonUtils.toJsonString(EventResponseVO.success());
                                }
                            }
                        }
                        json.put("canDeleted", true);
                        capBizManager.sendToTmsp(SOURCE_ID, EVENT_TYPE, json);
                        return JsonUtils.toJsonString(EventResponseVO.success());
                    }

                }

            } catch (Exception e) {
                log.error("CapBizObjectDeleteListener.error:userObject = {}, e = {}", deleteStr, e);
                return JsonUtils.toJsonString(EventResponseVO.fail(e.getMessage()));
            }
        }
        return JsonUtils.toJsonString(EventResponseVO.success());
    }
}
