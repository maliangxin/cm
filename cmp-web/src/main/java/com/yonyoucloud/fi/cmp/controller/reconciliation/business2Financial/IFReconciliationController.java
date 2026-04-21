package com.yonyoucloud.fi.cmp.controller.reconciliation.business2Financial;

import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.ifreconciliation.IFReconciliationService;
import com.yonyou.ucf.mdd.ext.base.BaseController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Controller
@RequestMapping("/cm/ifRecon")
public class IFReconciliationController extends BaseController{
   @Autowired
   IFReconciliationService ifReconciliationService;

    @RequestMapping("/dataDetail")
    @ResponseBody
    public CtmJSONObject dataDetail(@RequestBody CtmJSONObject params,HttpServletRequest request, HttpServletResponse response){
        return ifReconciliationService.getReconciliationDataDetail(params);
    }

    @RequestMapping("/dataAll")
    @ResponseBody
    public CtmJSONObject dataAll(@RequestBody CtmJSONObject params,HttpServletRequest request, HttpServletResponse response){
        return ifReconciliationService.getReconciliationDataAll(params);
    }

/*    @RequestMapping("/getCode")
    @ResponseBody
    public CtmJSONObject getCode(@RequestBody CtmJSONArray params, HttpServletRequest request, HttpServletResponse response){
        return ifReconciliationService.getReconciliationCode(params);
    }*/

}
