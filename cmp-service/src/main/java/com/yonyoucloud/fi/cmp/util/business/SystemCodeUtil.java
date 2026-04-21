package com.yonyoucloud.fi.cmp.util.business;

import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.cmpentity.EventSource;
import com.yonyoucloud.fi.cmp.cmpentity.EventType;
import com.yonyoucloud.fi.cmp.constant.CrossBankType;
import com.yonyoucloud.fi.cmp.constant.ISystemCodeConstant;
import com.yonyoucloud.fi.cmp.util.TypeUtils;
import org.apache.commons.lang3.StringUtils;
import org.imeta.orm.base.BizObject;

import java.util.HashMap;
import java.util.Map;

/**
 * @author maliang
 * @version V1.0
 * @date 2021/7/19 15:18
 * @Copyright yonyou
 */
public class SystemCodeUtil {

    //商业银行联行号码
    static Map numMap = new HashMap();
    static{
        // 313 城市商业银行，314、317 农村商业银行，402 农村信用联社
        numMap.put("313","313");
        numMap.put("314","314");
        numMap.put("317","317");
        numMap.put("402","402");
    }
    /**
     * 根据事项来源判断单据的系统编码是应收-fiar，应付-fiap，现金-cmp，费用管理-fier
     * @param bizObject
     * @return
     * @throws CtmException
     */
    public static String getSystemCode(BizObject bizObject) throws CtmException {
        Short srcItem = TypeUtils.castToShort(bizObject.get("srcitem"));
        // 薪资支付单添加 2020/07/23
        Short billType = TypeUtils.castToShort(bizObject.get("billtype"));
        if(srcItem.equals(EventSource.SystemOut.getValue())){
            return ISystemCodeConstant.FIER;
        }
        if (srcItem.equals(EventSource.Cmpchase.getValue()) || billType.equals((short)15) || srcItem.equals(EventSource.Drftchase.getValue())) {
            return ISystemCodeConstant.CMP;
        }
        // 转账单导入
        if (srcItem.equals(EventSource.ManualImport.getValue()) && billType.equals(EventType.TransferAccount.getValue())){
            return ISystemCodeConstant.CMP;
        }

        // 应收应付单据事项类型判断
        String systemCode = "";
        if (billType.equals(EventType.ReceiveBill.getValue())) {
            systemCode = ISystemCodeConstant.FIAR;
        }else if (billType.equals(EventType.OtherAREvent.getValue())) {
            systemCode = ISystemCodeConstant.FIAR;
        }else if (billType.equals(EventType.OtherAPEvent.getValue())) {
            systemCode = ISystemCodeConstant.FIAP;
        }else if (billType.equals(EventType.PayMent.getValue())) {
            systemCode = ISystemCodeConstant.FIAP;
        }else if (billType.equals(EventType.ArRefund.getValue())) {
            systemCode = ISystemCodeConstant.FIAR;
        }else if (billType.equals(EventType.ApRefund.getValue())) {
            systemCode = ISystemCodeConstant.FIAP;
        }
        return systemCode;
    }

    /*
     * @Author majfd
     * @Description 根据联行号获取跨行标识
     * @Param [payLineNumber, recLineNumber] 01:本地本行；02:异地本行；03:本地他
     * 行；04:异地他行；05:国外他行；06: 国外本行
     * @Return java.lang.String
     **/
    public static String getToBankType(Object payLineNumber, Object recLineNumber) {
        if (payLineNumber == null || recLineNumber == null) {
            return CrossBankType.LL;
        }
        String payNumber = (String) payLineNumber;
        String recNumber = (String) recLineNumber;
        if (StringUtils.isBlank(payNumber) || StringUtils.isBlank(recNumber)) {
            return CrossBankType.LL;
        }
        if (payNumber.length() < 7 || recNumber.length() < 7) {
            return CrossBankType.LL;
        }
        // 313 城市商业银行，314、317 农村商业银行，402 农村信用联社
        if (numMap.containsKey(payNumber.substring(0,3)) && numMap.containsKey(recNumber.substring(0,3))) {
            return CrossBankType.UN;
        }
        String payBankTypeCode = payNumber.substring(0, 3);
        String recBankTypeCode = recNumber.substring(0, 3);
        String payCityCode = payNumber.substring(3, 7);
        String recCityCode = recNumber.substring(3, 7);
        if (payBankTypeCode.equals(recBankTypeCode)) {
            if (payCityCode.equals(recCityCode)) {
                return CrossBankType.LL;
            } else {
                return CrossBankType.OL;
            }
        } else {
            if (payCityCode.equals(recCityCode)) {
                return CrossBankType.LO;
            } else {
                return CrossBankType.OO;
            }
        }
    }




}
