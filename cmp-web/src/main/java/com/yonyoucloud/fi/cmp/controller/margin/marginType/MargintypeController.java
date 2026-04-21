package com.yonyoucloud.fi.cmp.controller.margin.marginType;


import com.yonyou.ucf.mdd.ext.base.BaseController;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.margintype.service.MargintypeService;
import com.yonyoucloud.fi.cmp.util.QueryBaseDocUtils;
import com.yonyoucloud.fi.cmp.util.StringUtils;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletResponse;
import java.util.Map;

@Controller
@RequestMapping("/margintype")
@Slf4j
@RequiredArgsConstructor
public class MargintypeController extends BaseController {

    private final MargintypeService margintypeService;

    /**
     * 启用
     *
     * @param bill
     * @param response
     */
    @PostMapping("/enable")
    public void publish(@RequestBody BillDataDto bill, HttpServletResponse response) throws Exception {
        if (!QueryBaseDocUtils.getPeriodByService()) {
            CtmJSONObject result = getResult();
            renderJson(response, ResultMessage.data(result));
        } else {
            Map<String, Object> params = (Map<String, Object>) bill.getData();
            String id = ValueUtils.isNotEmptyObj(params.get("id")) ? params.get("id").toString() : null;
            if (StringUtils.isEmpty(id)) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101212"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418018B", "ID不能为空！") /* "ID不能为空！" */);
            }
            String code = ValueUtils.isNotEmptyObj(params.get("code")) ? params.get("code").toString() : null;
            renderJson(response, ResultMessage.data(margintypeService.enable(Long.parseLong(id), code)));
        }
    }


    private CtmJSONObject getResult() {
        CtmJSONObject resultJSONObject = new CtmJSONObject();
        resultJSONObject.put("dealSucceed", false);
        resultJSONObject.put(ICmpConstant.MSG, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418018D", "未开通现金管理服务!") /* "未开通现金管理服务!" */);
        return resultJSONObject;
    }

    /**
     * 停用
     *
     * @param bill
     * @param response
     */
    @PostMapping("/unEnable")
    public void cancelPublish(@RequestBody BillDataDto bill, HttpServletResponse response) throws Exception {
        if (!QueryBaseDocUtils.getPeriodByService()) {
            CtmJSONObject result = getResult();
            renderJson(response, ResultMessage.data(result));
        } else {
            Map<String, Object> params = (Map<String, Object>) bill.getData();
            String id = ValueUtils.isNotEmptyObj(params.get("id")) ? params.get("id").toString() : null;
            if (StringUtils.isEmpty(id)) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101212"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418018B", "ID不能为空！") /* "ID不能为空！" */);
            }
            String code = ValueUtils.isNotEmptyObj(params.get("code")) ? params.get("code").toString() : null;
            renderJson(response, ResultMessage.data(margintypeService.unEnable(Long.parseLong(id), code)));
        }
    }


}


