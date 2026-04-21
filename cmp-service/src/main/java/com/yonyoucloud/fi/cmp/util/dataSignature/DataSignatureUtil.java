package com.yonyoucloud.fi.cmp.util.dataSignature;

import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.security.signature.CtmSignatureService;
import com.yonyoucloud.fi.cmp.constant.IBussinessConstant;
import com.yonyoucloud.fi.cmp.util.StringUtils;
import com.yonyoucloud.fi.cmp.util.dataSignature.entity.DataSignatureEntity;

import java.text.DecimalFormat;

/**
 * 数据库防篡改的的签名验签类
 * @author maliangn
 */
public class DataSignatureUtil {


    /**
     * 对数据进行签名，使用天威签名，而不是其他签名
     * @param dataSignatureEntity
     * @return
     * @throws Exception
     */
    public static String signMsg(DataSignatureEntity dataSignatureEntity) throws Exception {
        DecimalFormat decimalFormat = new DecimalFormat("0.00#");
        String oriSum = decimalFormat.format(dataSignatureEntity.getTradeAmount());
        CtmJSONObject oriJson = new CtmJSONObject();
        oriJson.put("oppositeObjectName", dataSignatureEntity.getOpoppositeObjectName());
        oriJson.put("oppositeAccountName", dataSignatureEntity.getOppositeAccountName());
        oriJson.put(IBussinessConstant.ORI_SUM, oriSum);
        return AppContext.getBean(CtmSignatureService.class).iTrusSignMessage(oriJson.toString());
    }


    /**
     * 对数据进行验签
     * @param dataSignatureEntity
     * @return
     * @throws Exception
     */
    public static void unSignMsg(DataSignatureEntity dataSignatureEntity) throws Exception {
        DecimalFormat decimalFormat = new DecimalFormat("0.00#");
        String oriSum = decimalFormat.format(dataSignatureEntity.getTradeAmount());
        CtmJSONObject oriJson = new CtmJSONObject();
        oriJson.put("oppositeObjectName", dataSignatureEntity.getOpoppositeObjectName());
        oriJson.put("oppositeAccountName", dataSignatureEntity.getOppositeAccountName());
        oriJson.put(IBussinessConstant.ORI_SUM, oriSum);
        if(!StringUtils.isEmpty(dataSignatureEntity.getOriSign()) && dataSignatureEntity.getOriSign().length() < 350){
            if (!AppContext.getBean(CtmSignatureService.class).iTrusVerifySignature(oriJson.toString(),dataSignatureEntity.getOriSign())) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101065"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("", "签名验签失败！") /* "资金付款单签名验签失败！" *//*资金付款单签名验签失败！*/);
            }
        }
    }


}
