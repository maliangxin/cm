package com.yonyoucloud.fi.cmp.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.iuap.message.platform.client.MessagePlatformClient;
import com.yonyou.iuap.message.platform.rpc.IBizMessageService;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cm.transferaccount.TransferAccount;
import com.yonyoucloud.fi.cmp.cmpentity.*;
import com.yonyoucloud.fi.cmp.constant.IBussinessConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.salarypay.Salarypay;
import lombok.extern.slf4j.Slf4j;
import org.imeta.orm.base.BizObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @ClassName SendBizMessageUtils
 * @Description 发送业务消息工具类
 * @Author tongyd
 * @Date 2019/6/18 15:47
 * @Version 1.0
 **/
@Slf4j
@Component
public class SendBizMessageUtils {


    @Autowired
    MessagePlatformClient client;
    /*
     *@Author tongyd
     *@Description 发送业务消息
     *@Date 2019/6/18 16:42
     *@Param [bizObject, billNo, action]
     *@Return void
     **/
    public static void sendBizMessage(BizObject bizObject, String billNo, String action) throws JsonProcessingException {
        List<LinkedHashMap<String, String>> sendBizObjects = new ArrayList<>();
        LinkedHashMap<String, String> sendBizObject = new LinkedHashMap(bizObject.size());
        for (Map.Entry<String, Object> entry : bizObject.entrySet()) {
            sendBizObject.put(entry.getKey(), CtmJSONObject.toJSONString(entry.getValue()));
        }
        sendBizObjects.add(sendBizObject);

        try {
            //todo 1015支持
            IBizMessageService messageService = AppContext.getBean(IBizMessageService.class);
            messageService.doBizMessage("diwork", InvocationInfoProxy.getTenantid(), sendBizObjects, billNo, action);
        } catch (Exception e) {
            log.error("发送业务消息异常：", e);
        }
    }

    /**
     * 发送业务消息
     * @param bizObject
     * @param billNo
     * @param action
     * @throws JsonProcessingException
     */
    public static void sendBizMessage(CtmJSONObject bizObject, String billNo, String action) throws JsonProcessingException {
        List<LinkedHashMap<String, String>> sendBizObjects = new ArrayList<>();
        LinkedHashMap<String, String> sendBizObject = new LinkedHashMap(bizObject.size());
        List<String> moneyField = new ArrayList<String>();
        moneyField.add(IBussinessConstant.ORI_SUM);
        moneyField.add(IBussinessConstant.NAT_SUM);
        for (Map.Entry<String, Object> entry : bizObject.entrySet()) {
            if (entry.getValue() == null) {
                continue;
            }
            if(moneyField.contains(entry.getKey())) {
                sendBizObject.put(entry.getKey(), CtmJSONObject.toJSONString(setScaleMoneyDigit(entry.getValue(), bizObject.get(ICmpConstant.CURRENCY_PRICEDIGIT))));
            }else {
                sendBizObject.put(entry.getKey(), CtmJSONObject.toJSONString(entry.getValue()));
            }
        }
        sendBizObjects.add(sendBizObject);
        try {
            //todo 1015支持
            IBizMessageService messageService = AppContext.getBean(IBizMessageService.class);
            messageService.doBizMessage("diwork", InvocationInfoProxy.getTenantid(), sendBizObjects, billNo, action);
        } catch (Exception e) {
            log.error("发送业务消息异常：", e);
        }
    }

    /*
     *@Author tongyd
     *@Description 发送业务消息
     *@Date 2019/6/18 16:42
     *@Param [bizObject, billNo, action]
     *@Return void
     **/
    public static void sendBizMessageBySalaryFailData(BizObject bizObject, String billNo, String action) throws JsonProcessingException {
    	List<String> field = new ArrayList<String>();
    	field.add("voucherstatus");
    	field.add("settlestatus");
    	field.add("srcitem");
    	field.add("billtype");
    	field.add("auditstatus");
    	field.add("paystatus");
    	field.add("tradestatus");
        List<LinkedHashMap<String, String>> sendBizObjects = new ArrayList<>();
        LinkedHashMap<String, String> sendBizObject = new LinkedHashMap(bizObject.size());
        for (Map.Entry<String, Object> entry : bizObject.entrySet()) {
        	if(entry.getKey().equals("Salarypay_b")) {
        		List<LinkedHashMap<String, Object>> sendBodyBizObjects = new ArrayList<>();
        		List<BizObject> bodyBizObjectList = (List<BizObject>)entry.getValue();
        		for (int i = 0; i < bodyBizObjectList.size(); i++) {
        			LinkedHashMap<String, Object> sendBodyBizObject = new LinkedHashMap(bizObject.size());
        			for (Map.Entry<String, Object> bodyentry : bodyBizObjectList.get(i).entrySet()) {
        				if(field.contains(bodyentry.getKey())) {
        					sendBodyBizObject.put(bodyentry.getKey(), CtmJSONObject.toJSONString(translateEnumValue(bodyentry.getKey(), bodyentry.getValue())));
        				}else {
        					sendBodyBizObject.put(bodyentry.getKey(), CtmJSONObject.toJSONString(bodyentry.getValue()));
        				}
            		}
        			sendBodyBizObjects.add(sendBodyBizObject);
				}
        		sendBizObject.put(entry.getKey(), CtmJSONObject.toJSONString(sendBodyBizObjects));
        	}else if (field.contains(entry.getKey())) {
        		sendBizObject.put(entry.getKey(), CtmJSONObject.toJSONString(translateEnumValue(entry.getKey(), entry.getValue())));
        	}else {
        		sendBizObject.put(entry.getKey(), CtmJSONObject.toJSONString(entry.getValue()));
        	}
        }
        sendBizObjects.add(sendBizObject);
        try {
            //todo 1015支持
            IBizMessageService messageService = AppContext.getBean(IBizMessageService.class);
            messageService.doBizMessage("diwork", InvocationInfoProxy.getTenantid(), sendBizObjects, billNo, action);
        } catch (Exception e) {
            log.error("发送业务消息异常：", e);
        }
    }

	/*
     *@Author tongyd
     *@Description 发送业务消息
     *@Date 2019/6/18 16:42
     *@Param [payBills, billNo, action]
     *@Return void
     **/
//    public static void sendPayBizMessage(List<PayBill> payBills, String billNo, String action) throws JsonProcessingException {
//        List<LinkedHashMap<String, String>> sendBizObjects = new ArrayList<>();
//        for (PayBill payBill : payBills) {
//            LinkedHashMap<String, String> sendBizObject = new LinkedHashMap(payBill.size());
//            for (Map.Entry<String, Object> entry : payBill.entrySet()) {
//                sendBizObject.put(entry.getKey(), CtmJSONObject.toJSONString(entry.getValue()));
//            }
//            sendBizObjects.add(sendBizObject);
//        }
//        try {
//            IBizMessageService messageService = AppContext.getBean(IBizMessageService.class);
//            messageService.doBizMessage("diwork", InvocationInfoProxy.getTenantid(), sendBizObjects, billNo, action);
//        } catch (Exception e) {
//            log.error("发送业务消息异常：", e);
//        }
//    }

    public static void sendTsBizMessage(List<TransferAccount> transferAccounts, String billNo, String action) throws JsonProcessingException {
        List<LinkedHashMap<String, String>> sendBizObjects = new ArrayList<>();
        for (TransferAccount transferAccount : transferAccounts) {
            LinkedHashMap<String, String> sendBizObject = new LinkedHashMap(transferAccount.size());
            for (Map.Entry<String, Object> entry : transferAccount.entrySet()) {
                sendBizObject.put(entry.getKey(), CtmJSONObject.toJSONString(entry.getValue()));
            }
            sendBizObjects.add(sendBizObject);
        }
        try {
            //todo 1015支持
            IBizMessageService messageService = AppContext.getBean(IBizMessageService.class);
            messageService.doBizMessage("diwork", InvocationInfoProxy.getTenantid(), sendBizObjects, billNo, action);
        } catch (Exception e) {
            log.error("发送业务消息异常：", e);
        }
    }

    public static void sendBizMessage(List<Salarypay> bizObject, String billNo, String action) throws JsonProcessingException {
    	List<String> field = new ArrayList<String>();
    	field.add("voucherstatus");
    	field.add("settlestatus");
    	field.add("srcitem");
    	field.add("billtype");
    	field.add("auditstatus");
    	field.add("paystatus");
    	field.add("tradestatus");
        List<LinkedHashMap<String, String>> sendBizObjects = new ArrayList<>();
        for (BizObject transferAccount : bizObject) {
            LinkedHashMap<String, String> sendBizObject = new LinkedHashMap(transferAccount.size());
            for (Map.Entry<String, Object> entry : transferAccount.entrySet()) {
            	if(field.contains(entry.getKey())) {
            		sendBizObject.put(entry.getKey(), CtmJSONObject.toJSONString(translateEnumValue(entry.getKey(), entry.getValue())));
            	}else {
            		sendBizObject.put(entry.getKey(), CtmJSONObject.toJSONString(entry.getValue()));
            	}
            }
            sendBizObjects.add(sendBizObject);
        }
        try {
            //todo 1015支持
            IBizMessageService messageService = AppContext.getBean(IBizMessageService.class);
            messageService.doBizMessage("diwork", InvocationInfoProxy.getTenantid(), sendBizObjects, billNo, action);
        } catch (Exception e) {
            log.error("发送业务消息异常：", e);
        }
    }

    private static String translateEnumValue(String key, Object obj) {
    	if(obj == null) {
    		return null;
    	}
    	Integer value = null;
    	if(obj instanceof Integer) {
    		value = (Integer)obj;
    	}else {
    		value = new Integer((short)obj);
    	}
    	switch(key) {
    	case "voucherstatus":
    		return VoucherStatus.find(value).getName();
		case "settlestatus":
			return SettleStatus.find(value).getName();
    	case "srcitem":
    		return EventSource.find(value).getName();
    	case "billtype":
    		return EventType.find(value).getName();
    	case "auditstatus":
    		return AuditStatus.find(value).getName();
    	case "paystatus":
    		return PayStatus.find(value).getName();
    	case "tradestatus":
    		return PaymentStatus.find(value).getName();
    	default:
            return obj.toString();
        }
    }

    /**
     * 金额字段精度显示处理
     * @param obj
     * @param meneyDigit
     * @return
     */
    private static Object setScaleMoneyDigit(Object obj, Object meneyDigit) {
        if(obj == null || "".equals(obj)) {
            return null;
        }
        int digit = 2;
        if (meneyDigit != null && !"".equals(obj)) {
            if(meneyDigit instanceof Integer) {
                digit = ((Integer) meneyDigit).intValue();
            } else {
                digit = new Integer(meneyDigit.toString()).intValue();
            }
        }
        return new BigDecimal(obj.toString()).setScale(digit, RoundingMode.HALF_UP);
    }

    /*
     *@Description 发送业务消息(新架构使用)
     **/
    public static void sendBizMessageNew(BizObject bizObject, String billNo, String action, String busObjectCode) throws JsonProcessingException {
        List<LinkedHashMap<String, String>> sendBizObjects = new ArrayList<>();
        LinkedHashMap<String, String> sendBizObject = new LinkedHashMap(bizObject.size());
        for (Map.Entry<String, Object> entry : bizObject.entrySet()) {
            sendBizObject.put(entry.getKey(), CtmJSONObject.toJSONString(entry.getValue()));
        }
        sendBizObjects.add(sendBizObject);
        try {
            //todo 1015支持
            IBizMessageService messageService = AppContext.getBean(IBizMessageService.class);
            messageService.doBizMessage("diwork", InvocationInfoProxy.getTenantid(), sendBizObjects, billNo, action,
                    AppContext.getCurrentUser().getId().toString(), AppContext.getCurrentUser().getYhtAccessToken(),
                    busObjectCode);
        } catch (Exception e) {
            log.error("发送业务消息异常：", e);
        }
    }
}
