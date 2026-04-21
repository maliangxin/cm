package com.yonyoucloud.fi.cmp.controller.bankunion;

import com.yonyou.ucf.mdd.ext.base.BaseController;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.bankdealdetail.BankUnionService;
import com.yonyoucloud.fi.cmp.bankunion.BankUnionRequest;
import com.yonyoucloud.fi.cmp.bankunion.BankUnionResponse;
import com.yonyoucloud.fi.cmp.constant.ITransCodeConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.List;


/**
 *  交易明细保存OpenAPI
 *  牧原项目  到账通知接口
 */
@Controller
@RequestMapping("/bankUnion")
@Slf4j
public class BankUnionController extends BaseController{

	@Autowired
	BankUnionService bankUnionService;

	/**
	 * 银企联调用的保存接口
	 * @param bankUnionRequest
	 * @param request
	 * @param response
	 * 000000	成功	操作成功
	 * 010011	失败，需要重试	失败需要重试
	 * 010012	失败，检查数据准确性	失败，检查数据准确性，重试
	 * 010013	失败，检查交易编码	失败，检查交易编码，重试
	 * 010014	失败，根据实际情况 重试
	 * 010100	失败，不需要重试	失败不需要重试
	 * @throws Exception
	 */
	@RequestMapping("/save")
	public void saveBankDealDetail(@RequestBody BankUnionRequest bankUnionRequest, HttpServletRequest request, HttpServletResponse response) throws Exception {
		BankUnionResponse bankUnionResponse = null;
		try {
			//TODO 根据交易编码处理不同的银企联交易
			Instant start = Instant.now();
			int backNum = Integer.valueOf(bankUnionRequest.getRequest_body().getBack_num());
			if(backNum > 0){
				String transCode= bankUnionRequest.getRequest_head().getTran_code();
				switch (transCode) {
					case ITransCodeConstant.RECEIVE_ACCOUNT_TRANSACTION_DETAIL:
						bankUnionResponse = bankUnionService.insertTransactionDetail4BankUnion(bankUnionRequest.getRequest_body().getRecord());
					default:
				}
				Instant end = Instant.now();
				long l = Duration.between(start, end).toMillis();
				log.debug("BankUnionController接收交易明细耗时(ms):"+l);
				if(bankUnionResponse == null){
					bankUnionResponse = new BankUnionResponse();
					bankUnionResponse.setCode("010013");
					bankUnionResponse.setMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B9A43A05D8007E", "失败，检查交易编码，重试！") /* "失败，检查交易编码，重试！" */);
				}
				log.error("BankUnionController接收交易明细返回结果:"+CtmJSONObject.toJSONString(bankUnionResponse));
				renderJson(response, CtmJSONObject.toJSONString(bankUnionResponse));
			}else {
				bankUnionResponse = new BankUnionResponse();
				bankUnionResponse.setCode("3");
				bankUnionResponse.setMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B9A43A05D8007F", "请求数据不能为空！") /* "请求数据不能为空！" */);
				renderJson(response, CtmJSONObject.toJSONString(bankUnionResponse));
			}

		} catch (Exception e) {
			log.error(e.getMessage(), e);
			bankUnionResponse = new BankUnionResponse();
			bankUnionResponse.setCode("010014");
			bankUnionResponse.setMessage(e.getMessage());
			renderJson(response, CtmJSONObject.toJSONString(bankUnionResponse));
		}
	}
}
