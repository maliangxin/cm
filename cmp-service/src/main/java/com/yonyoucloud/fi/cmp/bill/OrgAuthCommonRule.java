//package com.yonyoucloud.fi.cmp.bill;
//
//import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
//import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.FilterVO;
//import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.SimpleFilterVO;
//import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
//import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
//import com.yonyou.ucf.mdd.ext.model.BillContext;
//import com.yonyoucloud.fi.basecom.check.AuthCheckCommonUtil;
//import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
//import com.yonyoucloud.fi.cmp.constant.IBussinessConstant;
//import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
//import org.imeta.core.base.ConditionOperator;
//import org.springframework.stereotype.Component;
//
//import java.util.HashSet;
//import java.util.Map;
//import java.util.Set;
//
///**
// * @author yangjn
// * @date 2021/4/25 19:57、
// * 现金管理通用查询过滤-根据资金组织权限查询数据
// *
// *
// */
//@Component
//public class OrgAuthCommonRule extends AbstractCommonRule {
//    //不需要走权限控制的业务单据
//    static Set excludeSet = new HashSet();
//
//    static{
//        excludeSet.add(IBillNumConstant.AUTO_ORDER_RULE_DEBIT);
//        excludeSet.add(IBillNumConstant.AUTO_ORDER_RULE_CREDIT);
//        excludeSet.add(IBillNumConstant.CMP_AUTOCORR_LIST);
//        excludeSet.add(IBillNumConstant.DENOMINATION_SETTING_LIST);
//        excludeSet.add(IBillNumConstant.CMP_AUTOORDERRULE_LIST);
//        excludeSet.add(IBillNumConstant.VIRTUALFLOWRULECONFIGCARD_LIST);
//        excludeSet.add(IBillNumConstant.VIRTUALFLOWRULECONFIGCARD);
//    }
//
//
//    @Override
//    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
//        BillDataDto billDataDto = (BillDataDto) getParam(map);
//        FilterVO filterVO = new FilterVO();
//        if(billDataDto.getCondition() != null) {
//            filterVO = billDataDto.getCondition();
//        }
//        boolean isAuthContro = true;
//        //如果单据没有资金组织，则不走权限控制
//        if(excludeSet.contains(billContext.getBillnum())){
//            return new RuleExecuteResult();
//        }
//        //客户只安装资金结算，不购买现金管理，直联银行设置节点查询，不进行权限校验
//        if (IBillNumConstant.BANKACCOUNTSETTINGLIST.equals(billContext.getBillnum())) {
//            if (IServicecodeConstant.STWB_BANKACCOUNTSETTING.equals(billDataDto.getParameter("serviceCode"))){
//                isAuthContro = false;
//            }
//        }
//        // 权限控制
//        if(!IBillNumConstant.DENOMINATION_SETTING_LIST.equals(billContext.getBillnum()) && !IBillNumConstant.AUTO_CORRSETTING.equals(billContext.getBillnum())&&isAuthContro){
//            Set<String> orgsSet = AuthCheckCommonUtil.getAuthOrgs(billContext);
//            if(orgsSet!=null && orgsSet.size()>0) {
//                String[] orgs = orgsSet.toArray(new String[orgsSet.size()]);
//                SimpleFilterVO orgFilter = new SimpleFilterVO(IBussinessConstant.ACCENTITY, "in", orgs);
//
//                filterVO.appendCondition(ConditionOperator.and, orgFilter);
//            }
//        }
//        billDataDto.setCondition(filterVO);
//        putParam(map, billDataDto);
//        return new RuleExecuteResult();
//
//    }
//}
