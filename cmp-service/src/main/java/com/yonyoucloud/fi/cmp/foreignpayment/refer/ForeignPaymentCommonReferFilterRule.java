package com.yonyoucloud.fi.cmp.foreignpayment.refer;

import com.yonyou.cloud.middleware.rpc.RPCStubBeanFactory;
import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.ucf.mdd.common.model.ref.RefEntity;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.FilterVO;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.SimpleFilterVO;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.basecom.check.AuthCheckCommonUtil;
import com.yonyoucloud.fi.basecom.constant.IBillConst;
import com.yonyoucloud.fi.basecom.utils.FIDubboUtils;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.common.service.CmCommonService;
import com.yonyoucloud.fi.cmp.constant.*;
import com.yonyoucloud.fi.cmp.foreignpayment.ForeignPayment;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import com.yonyoucloud.fi.tmsp.vo.FundBusinObjArchivesItemDTO;
import com.yonyoucloud.uretail.sys.auth.DataPermissionRequestDto;
import com.yonyoucloud.uretail.sys.auth.DataPermissionResponseDto;
import com.yonyoucloud.uretail.sys.auth.DataPermissionResultDto;
import com.yonyoucloud.uretail.sys.pubItf.IDataPermissionService;
import org.imeta.core.base.ConditionOperator;
import org.imeta.core.base.ObjectUtils;
import org.imeta.orm.base.BizObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

import static com.yonyoucloud.fi.cmp.constant.IRefCodeConstant.UCFBASEDOC_BD_BANKDOTREF;

/**
 * 保证金相关 公共参照过滤规则*
 *
 * @author xuxbo
 * @date 2023/8/16 15:59
 */

@Component
public class ForeignPaymentCommonReferFilterRule extends AbstractCommonRule {

    @Autowired
    CmCommonService commonService;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        BillDataDto billDataDto = (BillDataDto) getParam(map);
        if (!IBillNumConstant.CMP_FOREIGNPAYMENT.equals(billDataDto.getBillnum())) {
            return new RuleExecuteResult();
        }
        List<BizObject> bills = getBills(billContext, map);
        BizObject bill = null;
        if (bills.size() > 0) {
            bill = bills.get(0);
        }
        boolean isfilter = "filter".equals(billDataDto.getExternalData());
        if (isfilter) {//如果是过滤区，直接跳过
            return new RuleExecuteResult();
        }
        if ("finbd.bd_expenseitemref".equals(billDataDto.getrefCode())) {
            if("tree".equalsIgnoreCase(billDataDto.getDataType())){
                if(billDataDto.getTreeCondition() == null){
                    FilterVO conditon = new FilterVO();
                    conditon.appendCondition(ConditionOperator.and, new SimpleFilterVO("propertybusiness", "eq", "1"));
                    billDataDto.setTreeCondition(conditon);
                }else{
                    billDataDto.getTreeCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("propertybusiness", "eq", "1"));
                }
            }else{
                if(billDataDto.getCondition() == null){
                    FilterVO conditon = new FilterVO();
                    conditon.appendCondition(ConditionOperator.and, new SimpleFilterVO("propertybusiness", "eq", "1"));
                    billDataDto.setCondition(conditon);
                }else{
                    billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("propertybusiness", "eq", "1"));
                }
            }
            return new RuleExecuteResult();
        }
        if ("ourname_name".equals(billDataDto.getKey())) {

            RefEntity refentity = billDataDto.getRefEntity();
            // 查询有权限的组织
            Set<String> orgsSet = AuthCheckCommonUtil.getAuthOrgs(billContext);
            List<String> expendIds = new ArrayList<>();
            expendIds.addAll(orgsSet);
            if (billDataDto.getTreeCondition() == null) {
                FilterVO filterVO = new FilterVO();
                if (IRefCodeConstant.FUNDS_ORGTREE.equals(refentity.code)) {
                    filterVO.appendCondition(ConditionOperator.and, new SimpleFilterVO("id", ICmpConstant.QUERY_IN, expendIds));
                    billDataDto.setTreeCondition(filterVO);
                }
            } else {
                billDataDto.getTreeCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("id", ICmpConstant.QUERY_IN, expendIds));
            }
        }
        // 对客户供应商的参照进行过滤
        if (IRefCodeConstant.AA_MERCHANTREF.equals(billDataDto.getrefCode()) || IRefCodeConstant.YSSUPPLIER_AA_VENDOR.equals(billDataDto.getrefCode())) {
            DataPermissionRequestDto requestDto = new DataPermissionRequestDto();
            if (IBillNumConstant.CMP_FOREIGNPAYMENT.equals(billContext.getBillnum())) {// 外汇付款
                requestDto.setEntityUri(ForeignPayment.ENTITY_NAME);
                requestDto.setServiceCode(IServicecodeConstant.FOREIGNPAYMENT);
            }
            requestDto.setSysCode(ICmpConstant.CMP_MODUAL_NAME);
            requestDto.setYxyUserId(AppContext.getUserId().toString());
            requestDto.setYhtTenantId(InvocationInfoProxy.getTenantid());
            requestDto.setYxyTenantId(AppContext.getTenantId().toString());
            requestDto.setHaveDetail(true);
            RPCStubBeanFactory rpChainBeanFactory = new RPCStubBeanFactory("iuap-apcom-auth", "c87e2267-1001-4c70-bb2a-ab41f3b81aa3", null, IDataPermissionService.class);
            rpChainBeanFactory.afterPropertiesSet();
            IDataPermissionService remoteBean = (IDataPermissionService) rpChainBeanFactory.getObject();
            Set<String> orgidts = new HashSet<>();// 控制组织
            String accentity = bill.get(IBillConst.ACCENTITY);
            orgidts.add(accentity);
            // 查询核算委托关系的组织
            Set<String> orgidtsTmp = FIDubboUtils.getDelegateHasSelf(accentity);
            orgidts.addAll(orgidtsTmp);
            String org = bill.get(IBillConst.ORG);
            if (org != null) {
                orgidts.add(org);
                orgidts.remove(accentity);
                if (org.equals(accentity)) {
                    orgidts.clear();
                    orgidts.add(accentity);
                }
            }
            String[] orgIds = orgidts.stream().toArray(String[]::new);
            if (IRefCodeConstant.AA_MERCHANTREF.equals(billDataDto.getrefCode())) { // 客户
                requestDto.setFieldNameArgs(new String[]{ICmpConstant.CUSTOMER});
                DataPermissionResponseDto dataPermission = remoteBean.getDataPermission(requestDto);
                List<DataPermissionResultDto> dataPermissionResultDtos = dataPermission.getResultDtos();
                for (DataPermissionResultDto resultDto : dataPermissionResultDtos) {
                    String[] ids = resultDto.getValues();
                    billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("id", ICmpConstant.QUERY_IN, ids));
                }
                billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("merchantAppliedDetail.stopstatus", ICmpConstant.QUERY_IN, new Integer[]{0}));
                billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("merchantAppliedDetail.merchantApplyRangeId.orgId", ICmpConstant.QUERY_IN, orgIds));
            }

            if (IRefCodeConstant.YSSUPPLIER_AA_VENDOR.equals(billDataDto.getrefCode())) { // 供应商
                requestDto.setFieldNameArgs(new String[]{IBussinessConstant.SUPPLIER});
                DataPermissionResponseDto dataPermission = remoteBean.getDataPermission(requestDto);
                List<DataPermissionResultDto> dataPermissionResultDtos = dataPermission.getResultDtos();
                FilterVO conditon = new FilterVO();
                if (billDataDto.getCondition() == null) {
                    for (DataPermissionResultDto resultDto : dataPermissionResultDtos) {
                        String[] ids = resultDto.getValues();
                        conditon.appendCondition(ConditionOperator.and, new SimpleFilterVO("id", ICmpConstant.QUERY_IN, ids));
                    }
                    conditon.appendCondition(ConditionOperator.and, new SimpleFilterVO("vendorextends.stopstatus", ICmpConstant.QUERY_EQ, 0));
                    billDataDto.setCondition(conditon);
                } else {
                    for (DataPermissionResultDto resultDto : dataPermissionResultDtos) {
                        String[] ids = resultDto.getValues();
                        billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("id", ICmpConstant.QUERY_IN, ids));
                    }
                    billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("vendorextends.stopstatus", ICmpConstant.QUERY_EQ, 0));
                }
                billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("vendorApplyRange.org", ICmpConstant.QUERY_IN, orgIds));
            }

            if (IRefCodeConstant.BD_STAFF_REF.equals(billDataDto.getrefCode()) || IRefCodeConstant.DOMAIN_BD_STAFF_REF.equals(billDataDto.getrefCode())) { // 员工
                requestDto.setFieldNameArgs(new String[]{ICmpConstant.EMPLOYEE});
                DataPermissionResponseDto dataPermission = remoteBean.getDataPermission(requestDto);
                List<DataPermissionResultDto> dataPermissionResultDtos = dataPermission.getResultDtos();
                for (DataPermissionResultDto resultDto : dataPermissionResultDtos) {
                    String[] ids = resultDto.getValues();
                    billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("id", ICmpConstant.QUERY_IN, ids));
                }
            }

        }
        if (IRefCodeConstant.AA_MERCHANTBANKREF.equals(billDataDto.getrefCode()) || IRefCodeConstant.PRODUCTCENTER_AA_MERCHANTAGENTFINANCIALREF.equals(billDataDto.getrefCode()) ) { //客户账户
            String currency = bill.get(IBussinessConstant.CURRENCY);
            String merchantId = null;
            if (IBillNumConstant.CMP_FOREIGNPAYMENT.equals(billContext.getBillnum())) {
                if (!ObjectUtils.isEmpty(bill.get("customer"))){
                    merchantId = bill.get("customer").toString();
                }
            }
            if (merchantId == null) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100370"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418007F","请先补充客户信息") /* "请先补充客户信息" */);
            }
//            billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("currency", ICmpConstant.QUERY_IN, new String[]{currency}));
            billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("stopstatus", ICmpConstant.QUERY_EQ, 0));
            billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("merchantId", ICmpConstant.QUERY_IN, new String[]{merchantId}));
        }

        if (IRefCodeConstant.YSSUPPLIER_AA_VENDORBANKREF.equals(billDataDto.getrefCode())) { //供应商账户
            String currency = bill.get(IBussinessConstant.CURRENCY);
            String supplier = null;
            if (IBillNumConstant.CMP_FOREIGNPAYMENT.equals(billContext.getBillnum())) {
                if (!ObjectUtils.isEmpty(bill.get("supplier"))){
                    supplier = bill.get("supplier").toString();
                }
            }
            if (supplier == null) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100371"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180080","请先补充供应商信息") /* "请先补充供应商信息" */);
            }
            FilterVO conditon = new FilterVO();
            if (billDataDto.getCondition() == null) {
//                conditon.appendCondition(ConditionOperator.and, new SimpleFilterVO("currency", ICmpConstant.QUERY_IN, new String[]{currency}));
                conditon.appendCondition(ConditionOperator.and, new SimpleFilterVO("stopstatus", ICmpConstant.QUERY_EQ, 0));
                conditon.appendCondition(ConditionOperator.and, new SimpleFilterVO("vendor", ICmpConstant.QUERY_IN, new String[]{supplier}));
                billDataDto.setCondition(conditon);
            } else {
//                billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("currency", ICmpConstant.QUERY_IN, new String[]{currency}));
                billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("stopstatus", ICmpConstant.QUERY_EQ, 0));
                billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("vendor", ICmpConstant.QUERY_IN, new String[]{supplier}));
            }
        }

        if (IRefCodeConstant.BD_STAFF_BANKACCT_REF.equals(billDataDto.getrefCode())) { //员工账户
            String currency = bill.get(IBussinessConstant.CURRENCY);
            String employee = null;
            if (IBillNumConstant.CMP_FOREIGNPAYMENT.equals(billContext.getBillnum())) {
                if (!ObjectUtils.isEmpty(bill.get("employee"))){
                    employee = bill.get("employee").toString();
                }
            }
            if (employee == null) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101862"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418007E","请先补充员工信息") /* "请先补充员工信息" */);
            }
            bill.set("employee", employee);
            FilterVO conditon = new FilterVO();
            if (billDataDto.getCondition() == null) {
                conditon.appendCondition(ConditionOperator.and, new SimpleFilterVO("staff_id", ICmpConstant.QUERY_IN, new String[]{employee}));
//                conditon.appendCondition(ConditionOperator.and, new SimpleFilterVO("currency", ICmpConstant.QUERY_EQ, currency));
                billDataDto.setCondition(conditon);
            } else {
                billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("staff_id", ICmpConstant.QUERY_IN, new String[]{employee}));
//                billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("currency", ICmpConstant.QUERY_EQ, currency));
            }

        }

        //资金业务对象过滤禁用状态数据
        if(IRefCodeConstant.TMSP_FUNDBUSINOBJ_REF.equals(billDataDto.getrefCode())){
            FilterVO conditon = new FilterVO();
            if (billDataDto.getCondition() == null) {
                conditon.appendCondition(ConditionOperator.and, new SimpleFilterVO("enabled", ICmpConstant.QUERY_EQ,true));
                billDataDto.setCondition(conditon);
            } else {
                billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("enabled", ICmpConstant.QUERY_EQ,true));
            }
        }
        // 银行网点参照过滤
        if(UCFBASEDOC_BD_BANKDOTREF.equals(billDataDto.getrefCode())){
            FilterVO conditon = new FilterVO();
            if (billDataDto.getCondition() == null) {
                conditon.appendCondition(ConditionOperator.and, new SimpleFilterVO("enable", ICmpConstant.QUERY_EQ,1));
                billDataDto.setCondition(conditon);
            } else {
                billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("enable", ICmpConstant.QUERY_EQ,1));
            }
        }
        //资金业务对象账户信息过滤禁用状态信息
        if(IRefCodeConstant.TMSP_FUNDBUSINOBJBANK_REF.equals(billDataDto.getrefCode())){
            FilterVO conditon = new FilterVO();
            if (billDataDto.getCondition() == null) {
                conditon.appendCondition(FundBusinObjArchivesItemDTO.BENABLED, ICmpConstant.QUERY_EQ, true);
//                conditon.appendCondition(FundBusinObjArchivesItemDTO.CURRENCY, ICmpConstant.QUERY_EQ,bill.get("currency"));
                conditon.appendCondition(FundBusinObjArchivesItemDTO.MAINID, ICmpConstant.QUERY_EQ,bill.get("capBizObj"));
                //针对资金业务对象的银行账户中 授权会计主体 做过滤
                conditon.appendCondition(FundBusinObjArchivesItemDTO.AUTHORIZEDACCCENTITY, ICmpConstant.QUERY_EQ, bill.get("accentity"));
                billDataDto.setCondition(conditon);
            } else {
                billDataDto.getCondition().appendCondition(FundBusinObjArchivesItemDTO.BENABLED, ICmpConstant.QUERY_EQ, true);
//                billDataDto.getCondition().appendCondition(FundBusinObjArchivesItemDTO.CURRENCY, ICmpConstant.QUERY_EQ,bill.get("currency"));
                billDataDto.getCondition().appendCondition(FundBusinObjArchivesItemDTO.MAINID, ICmpConstant.QUERY_EQ,bill.get("capBizObj"));
                //针对资金业务对象的银行账户中 授权会计主体 做过滤
                billDataDto.getCondition().appendCondition(FundBusinObjArchivesItemDTO.AUTHORIZEDACCCENTITY, ICmpConstant.QUERY_EQ, bill.get("accentity"));
            }

        }

        return new RuleExecuteResult();
    }
}
