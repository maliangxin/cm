package com.yonyoucloud.fi.cmp.event.listerEvent;

import com.yonyou.diwork.exception.BusinessException;
import com.yonyou.iuap.event.model.BusinessEvent;
import com.yonyou.iuap.event.rpc.IEventReceiveService;
import com.yonyou.ucf.mdd.ext.option.model.vo.EventResponseVO;
import com.yonyou.workbench.util.JsonUtils;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.voucher.CmpVoucherService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 老架构
 */
@Slf4j
@Component("cmpVoucherStatusEventListener")
public class CmpVoucherStatusEventListener implements IEventReceiveService {

	@Autowired
	CmpVoucherService cmpVoucherService;

	@Override
	public String onEvent(BusinessEvent businessEvent, String queueName) throws BusinessException {
		String userObjectStr = businessEvent.getUserObject();
		log.error("MQ-会计平台会写凭证状态消息{}", userObjectStr);//@notranslate
		try {
			CtmJSONObject voucherStatusInfo = CtmJSONObject.parseObject(userObjectStr);
			if ("cmp".equals(voucherStatusInfo.getString("systemcode"))) {
				cmpVoucherService.updateVoucherStatus(voucherStatusInfo);
			}
		} catch (Exception e) {
			log.error("处理凭证状态消息出错:", e);//@notranslate
			return JsonUtils.toJsonString(EventResponseVO.fail(e.getMessage()));
		}
		return  JsonUtils.toJsonString(EventResponseVO.success());
	}
}
