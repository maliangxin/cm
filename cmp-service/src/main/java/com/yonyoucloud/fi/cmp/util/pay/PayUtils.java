package com.yonyoucloud.fi.cmp.util.pay;

import com.yonyou.iuap.bizdoc.service.model.SettleMethodModel;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyoucloud.fi.cmp.salarypay.Salarypay;
import com.yonyoucloud.fi.cmp.util.basedoc.SettleMethodQueryService;
import com.yonyoucloud.fi.cmp.common.CtmException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PayUtils {

    public static void checkSettleType(List<Salarypay> salarypays) throws Exception{
        List<Long> ids = new ArrayList<>();
        for(Salarypay salarypay : salarypays){
            Long settlemode = salarypay.getSettlemode();
            if(settlemode != null){
                ids.add(settlemode);
            }
        }
        Map condtion = new HashMap();
        condtion.put("ids",ids);
        List<Map<String, Object>> settleMethodModels = AppContext.getBean(SettleMethodQueryService.class).querySettleMethodByCondition(condtion,"id,directConnection");
        for(Map<String, Object> map : settleMethodModels){
           Object directConnection = map.get("directConnection");
           if(directConnection == null || (directConnection != null && "0".equals(directConnection.toString()))){
               throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100382"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A05080074", "当前单据的结算方式为非直联，无法进行直联支付！") /* "当前单据的结算方式为非直联，无法进行直联支付！" */);
           }
        }

    }



}
