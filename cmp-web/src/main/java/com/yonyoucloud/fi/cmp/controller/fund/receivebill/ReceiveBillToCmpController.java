package com.yonyoucloud.fi.cmp.controller.fund.receivebill;

import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.ucf.mdd.ext.base.BaseController;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.controller.Authentication;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.receivebill.ReceiveBill;
import com.yonyoucloud.fi.cmp.receivebill.service.ReceiveBillService;
import lombok.extern.slf4j.Slf4j;
import org.imeta.biz.base.Objectlizer;
import org.imeta.orm.base.Json;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * @author sz@yonyou.com
 * @version 1.0
 */
@Controller
@Slf4j
@RequestMapping("/receivebill")
@Authentication(readCookie = true)
public class ReceiveBillToCmpController extends BaseController {

    @Autowired
    ReceiveBillService receiveBillService;

    @RequestMapping("/settle")
    public void settle(@RequestBody BillDataDto bill, HttpServletRequest request, HttpServletResponse response) throws Exception {
        List<ReceiveBill> receiveBillList = null;
        CtmJSONObject param = getJsonObject(bill);
        String billnum = bill.getBillnum();
        Json json;
        if (IBillNumConstant.RECEIVE_BILL.equals(billnum)) {
            json = new Json(CtmJSONObject.toJSONString(param.getJSONObject("data")));
        } else {
            json = new Json(CtmJSONObject.toJSONString(param.getJSONArray("data")));
        }
        receiveBillList = Objectlizer.decode(json, ReceiveBill.ENTITY_NAME);
        CtmJSONObject result = receiveBillService.settle(receiveBillList);
        renderJson(response, ResultMessage.data(result));
        //service已释放锁
//        finally {
//            if(receiveBillList !=null){
//                receiveBillList.stream().forEach(receiveBill ->{
//                    JedisLockUtils.unlockBillWithOutTrace(receiveBill.getId().toString());
//                });
//            }
//        }
    }


    @NotNull
    private CtmJSONObject getJsonObject(@RequestBody BillDataDto bill) {
        Map<String, Object> map = new HashMap<>();
        map.put("billnum", bill.getBillnum());
        map.put("data", bill.getData());
        return new CtmJSONObject(map);
    }

    @RequestMapping("/unSettle")
    public void unSettle(@RequestBody BillDataDto bill, HttpServletRequest request, HttpServletResponse response) throws Exception {
        List<ReceiveBill> receiveBillList = null;
        CtmJSONObject param = getJsonObject(bill);
        String billnum = param.getString("billnum");
        Json json = null;
        if (billnum != null && IBillNumConstant.RECEIVE_BILL.equals(billnum)) {
            json = new Json(CtmJSONObject.toJSONString(param.getJSONObject("data")));
        } else {
            json = new Json(CtmJSONObject.toJSONString(param.getJSONArray("data")));
        }
        receiveBillList = Objectlizer.decode(json, ReceiveBill.ENTITY_NAME);
        CtmJSONObject result = receiveBillService.unSettle(receiveBillList);
        renderJson(response, ResultMessage.data(result));
//        finally {
//            if(receiveBillList !=null){
//                receiveBillList.stream().forEach(receiveBill ->{
//                    JedisLockUtils.unlockBillWithOutTrace(receiveBill.getId().toString());
//                });
//            }
//        }

    }

    @RequestMapping("/receiveBillSp")
    public void receiveBillSp(@RequestBody BillDataDto bill, HttpServletRequest request, HttpServletResponse response) throws Exception {
        List<ReceiveBill> receiveBillList = null;
        CtmJSONObject param = getJsonObject(bill);
        Json json = new Json(CtmJSONObject.toJSONString(param.getJSONArray("data")));
        receiveBillList = Objectlizer.decode(json, ReceiveBill.ENTITY_NAME);
        CtmJSONObject result = receiveBillService.receiveBillSp(receiveBillList);
        renderJson(response, ResultMessage.data(result));
//        finally {
//            if(receiveBillList !=null){
//                receiveBillList.stream().forEach(receiveBill ->{
//                    JedisLockUtils.unlockBillWithOutTrace(receiveBill.getId().toString());
//                });
//            }
//        }
    }

    @RequestMapping("/receiveBillQxsp")
    public void receiveBillQxsp(@RequestBody BillDataDto bill, HttpServletRequest request, HttpServletResponse response) throws Exception {
        List<ReceiveBill> receiveBillList = null;
        CtmJSONObject param = getJsonObject(bill);
        Json json = new Json(CtmJSONObject.toJSONString(param.getJSONArray("data")));
        receiveBillList = Objectlizer.decode(json, ReceiveBill.ENTITY_NAME);
        CtmJSONObject result = receiveBillService.receiveBillQxsp(receiveBillList);
        renderJson(response, ResultMessage.data(result));
//        finally {
//            if(receiveBillList !=null){
//                receiveBillList.stream().forEach(receiveBill ->{
//                    JedisLockUtils.unlockBillWithOutTrace(receiveBill.getId().toString());
//                });
//            }
//        }
    }

    @RequestMapping("/receiveBillBodySp")
    public void receiveBillBodySp(@RequestBody BillDataDto bill, HttpServletRequest request, HttpServletResponse response) throws Exception {
        CtmJSONObject param = getJsonObject(bill);
        Json json = new Json(CtmJSONObject.toJSONString(param.getJSONObject("data")));
        List<ReceiveBill> receiveBillList = Objectlizer.decode(json, ReceiveBill.ENTITY_NAME);
        CtmJSONObject result = receiveBillService.receiveBillSp(receiveBillList);
        renderJson(response, ResultMessage.data(result.get("msg")));
    }

    @RequestMapping("/receiveBillBodyQxsp")
    public void receiveBillBodyQxsp(@RequestBody BillDataDto bill, HttpServletRequest request, HttpServletResponse response) throws Exception {
        CtmJSONObject param = getJsonObject(bill);
        Json json = new Json(CtmJSONObject.toJSONString(param.getJSONObject("data")));
        List<ReceiveBill> receiveBillList = Objectlizer.decode(json, ReceiveBill.ENTITY_NAME);
        CtmJSONObject result = receiveBillService.receiveBillQxsp(receiveBillList);
        renderJson(response, ResultMessage.data(result));
    }


}
