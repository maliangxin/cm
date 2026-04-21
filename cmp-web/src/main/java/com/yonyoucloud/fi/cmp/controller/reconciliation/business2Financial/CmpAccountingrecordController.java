package com.yonyoucloud.fi.cmp.controller.reconciliation.business2Financial;


import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.iuap.ucf.common.i18n.MessageUtils;
import com.yonyou.ucf.mdd.ext.base.BaseController;
import com.yonyoucloud.fi.cmp.ifreconciliation.IFReconciliationService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 财务对账明细接口
 */
@Controller
@RequestMapping("/accountingrecord")
@Slf4j
public class CmpAccountingrecordController extends BaseController {

    @Autowired
    private IFReconciliationService iFReconciliationService;

    @RequestMapping("/querydetail")
    public void accountingrecordQuerydetail(@RequestBody CtmJSONObject param, HttpServletRequest request,
                                            HttpServletResponse response) throws Exception {
        log.error("accountingrecordQuerydetail==================：" + param.toString());
        try {
            if (param == null || param.get("period_start_date") == null || param.get("period_end_date") == null) {
                Date now = new Date();
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                String today = dateFormat.format(now);
                param.put("period_start_date", today);//
                param.put("period_end_date", today);//
            }
            String sql  = iFReconciliationService.getQuerydetailSql(param);
            CtmJSONObject responseMsg = new CtmJSONObject();
            responseMsg.put("code", 200);
            responseMsg.put("message", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418010F","操作成功！") /* "操作成功！" */);
            CtmJSONObject data = new CtmJSONObject();
            data.put("schema","ctmcmp");
            if(StringUtils.isEmpty(sql)){
                data.put("status",0);
            } else {
                data.put("status",1);
            }
            data.put("sql",sql);
            responseMsg.put("data", data);
            renderJson(response, CtmJSONObject.toJSONString(responseMsg));
        } catch (Exception e) {
            CtmJSONObject errorMsg = new CtmJSONObject();
            log.error("accountingrecordQuerydetail==================:" + e.getMessage(),e);
            errorMsg.put("result",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B9A43A05D80081", "执行失败") /* "执行失败" */);
            errorMsg.put("status",0);
            errorMsg.put("taskid",param.getString("taskid"));
            errorMsg.put("errormsg",e.getMessage());
            errorMsg.put("errorcode","10000");
            renderJson(response, CtmJSONObject.toJSONString(errorMsg));
        }
    }

}
