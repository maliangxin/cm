//package com.yonyoucloud.fi.cmp.controller.test;
//import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
//import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailaccess.BankDealDetailAccessFacade;
//import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailhandler.match.business.BankRefundMatchHandler;
//import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.compensate.impl.BankDealDetailCompensate;
//import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.compensate.impl.BankDealDetailOdsCompensate;
//import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.Direction;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//import javax.annotation.Resource;
//import java.math.BigDecimal;
//import java.util.ArrayList;
//import java.util.List;
///**
// * @Author guoyangy
// * @Date 2024/8/26 16:29
// * @Description todo
// * @Version 1.0
// */
//@RestController
//@RequestMapping("/test/")
//public class TestController {
//    @Resource
//    private BankDealDetailAccessFacade bankDealDetailAccessFacade;
//    @Resource
//    private BankDealDetailOdsCompensate bankDealDetailOdsCompensate;
//    @Resource
//    private BankDealDetailCompensate bankDealDetailCompensate;
//    @RequestMapping("ods/compensate")
//    public String test(){
//        bankDealDetailOdsCompensate.compensate();
//        return "ods succ";
//    }
//    @RequestMapping("compensate")
//    public String compensate(){
//        bankDealDetailCompensate.compensate();
//        return "ods succ";
//    }
//
//    @Autowired
//    private BankRefundMatchHandler bankRefundMatchHandler;
//    @RequestMapping("refund")
//    public String refund(){
//        BankReconciliation bankReconciliation = new BankReconciliation();
//        bankReconciliation.setDc_flag(Direction.Credit);
//        bankReconciliation.setBankaccount("111");
//        bankReconciliation.setCurrency("CNY");
//        bankReconciliation.setTran_amt(new BigDecimal(100));
//        bankReconciliation.setId(1111L);
//        BankReconciliation bankReconciliation2 = new BankReconciliation();
//        bankReconciliation2.setDc_flag(Direction.Debit);
//        bankReconciliation2.setBankaccount("111");
//        bankReconciliation2.setCurrency("CNY");
//        bankReconciliation2.setTran_amt(new BigDecimal(100));
//        bankReconciliation2.setId(1112L);
//        List<BankReconciliation> bankReconciliationList = new ArrayList<>();
//        bankReconciliationList.add(bankReconciliation);
//        bankReconciliationList.add(bankReconciliation2);
//
//        return "succ";
//    }
//}