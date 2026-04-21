package com.yonyoucloud.fi.cmp.event.listerEvent;

import com.yonyou.cloud.yts.util.JsonUtils;
import com.yonyou.iuap.BusinessException;
import com.yonyou.iuap.event.model.BusinessEvent;
import com.yonyou.iuap.event.rpc.IEventReceiveService;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.filter.util.StringUtil;
import com.yonyou.ucf.mdd.ext.option.model.vo.EventResponseVO;
import com.yonyou.yonbip.ctm.json.CtmJSONArray;
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
import java.util.List;
import java.util.Map;


/**
 * 第三方虚拟账户删除校验
 */
@Slf4j
@Service
public class ThreVirtualAccDeleteListener implements IEventReceiveService {


    @Autowired
    private CapBizManager capBizManager;

    private static List<String> getThirObjName() {
        List<String> list = new ArrayList<>();
        list.add("cmp.fundcollection.FundCollection_b");
        list.add("cmp.fundpayment.FundPayment_b");
        list.add("cm.transferaccount.TransferAccount");
        return list;
    }


    private static final String SOURCE_ID = "tmps-refer";
    private static final String EVENT_TYPE = "thre-virtual-account-delete-resp";

    @Override
    public String onEvent(BusinessEvent businessEvent, String s) throws BusinessException {

        if (ICmpConstant.CMDOMAIN.equals(s)) {
            String deleteStr = businessEvent.getUserObject();
            try {
                CtmJSONObject json = CtmJSONObject.parseObject(deleteStr);
                if (json.get(ICmpConstant.DATA) != null && (json.get(ICmpConstant.DATA) instanceof CtmJSONArray || json.get(ICmpConstant.DATA) instanceof ArrayList)) {
                    CtmJSONArray array = json.getJSONArray(ICmpConstant.DATA);
                    json.put("referenceSide", ICmpConstant.CMDOMAIN);
                    if (array.size() > 0) {
                        CtmJSONObject o = (CtmJSONObject) array.getJSONObject(0);
                        String thirdBizObj = o.getString(ICmpConstant.ID);
                        if (StringUtil.isNotEmpty(thirdBizObj)) {
                            List<String> thirObjList = getThirObjName();
                            //待结算，结算明细，结算变更，手工结算结果修正
                            for(String capObj:thirObjList){
                                QuerySchema querySchema = QuerySchema.create();
                                querySchema.addSelect("1");
                                if("cm.transferaccount.TransferAccount".equals(capObj)){
                                    querySchema.addCondition(QueryConditionGroup.and(QueryCondition.name("payVirtualAccount").eq(thirdBizObj)));
                                    querySchema.addCondition(QueryConditionGroup.or(QueryCondition.name("collVirtualAccount").eq(thirdBizObj)));
                                }else{
                                    querySchema.addCondition(QueryConditionGroup.and(QueryCondition.name("thirdParVirtAccount").eq(thirdBizObj)));
                                }
                                List<Map<String,Object>> list= MetaDaoHelper.query(capObj, querySchema, null);
                                if(ValueUtils.isNotEmpty(list)){
                                    json.put("canDeleted",false);
                                    capBizManager.sendToTmsp(SOURCE_ID,EVENT_TYPE,json);
                                    return JsonUtils.toJsonString(EventResponseVO.success());
                                }
                            }

                        }
                    }
                    json.put("canDeleted", true);
                    capBizManager.sendToTmsp(SOURCE_ID, EVENT_TYPE, json);
                    return JsonUtils.toJsonString(EventResponseVO.success());
                }

            } catch (Exception e) {
                log.error("ThreVirtualAccDeleteListener.error:userObject = {}, e = {}", deleteStr, e);
                return JsonUtils.toJsonString(EventResponseVO.fail(e.getMessage()));
            }
        }
        return JsonUtils.toJsonString(EventResponseVO.success());
    }
}
