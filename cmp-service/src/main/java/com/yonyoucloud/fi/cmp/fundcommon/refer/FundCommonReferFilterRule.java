package com.yonyoucloud.fi.cmp.fundcommon.refer;

import com.yonyou.cloud.middleware.rpc.RPCStubBeanFactory;
import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.iuap.org.dto.ConditionDTO;
import com.yonyou.iuap.org.dto.FundsOrgDTO;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.FilterVO;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.SimpleFilterVO;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.yonbip.ctm.cspl.capitalplanexecute.CapitalPlanExecuteService;
import com.yonyou.yonbip.ctm.orgs.FundsOrgQueryServiceComponent;
import com.yonyou.yonbip.ctm.remote.RemoteDubbo;
import com.yonyoucloud.ctm.stwb.incomeandexpenditure.Controlled;
import com.yonyoucloud.ctm.stwb.incomeandexpenditure.IncomeAndExpenditure;
import com.yonyoucloud.ctm.stwb.openapi.IOpenApiService;
import com.yonyoucloud.ctm.stwb.reqvo.IncomeAndExpenditureReqVO;
import com.yonyoucloud.ctm.stwb.respvo.IncomeAndExpenditureResVO;
import com.yonyoucloud.fi.basecom.constant.IBillConst;
import com.yonyoucloud.fi.basecom.utils.AuthUtil;
import com.yonyoucloud.fi.basecom.utils.FIDubboUtils;
import com.yonyoucloud.fi.cmp.autoparam.AutoConfig;
import com.yonyoucloud.fi.cmp.autoparam.service.AutoConfigService;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.common.digitalwallet.impl.FundcommonWalletHandler;
import com.yonyoucloud.fi.cmp.constant.*;
import com.yonyoucloud.fi.cmp.fundcollection.FundCollection;
import com.yonyoucloud.fi.cmp.fundcollection.FundCollection_b;
import com.yonyoucloud.fi.cmp.fundpayment.FundPayment;
import com.yonyoucloud.fi.cmp.fundpayment.FundPayment_b;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.Direction;
import com.yonyoucloud.fi.cmp.util.CmpMetaDaoHelper;
import com.yonyoucloud.fi.cmp.util.QueryBaseDocUtils;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import com.yonyoucloud.fi.tmsp.vo.FundBusinObjArchivesItemDTO;
import com.yonyoucloud.uretail.sys.auth.DataPermissionRequestDto;
import com.yonyoucloud.uretail.sys.auth.DataPermissionResponseDto;
import com.yonyoucloud.uretail.sys.auth.DataPermissionResultDto;
import com.yonyoucloud.uretail.sys.pubItf.IDataPermissionService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

import static com.yonyoucloud.fi.cmp.constant.IDomainConstant.MDD_DOMAIN_CTMCSPL;
import static com.yonyoucloud.fi.cmp.constant.IRefCodeConstant.UCFBASEDOC_BD_BANKDOTREF;

/**
 * 资金收付款单动态列权限过滤 - 针对资金收付款单表体行动态列
 *
 * @author maliang
 * @version 1.0
 * @since 2021-12-14 9:59
 */
@Slf4j
@Component
public class FundCommonReferFilterRule extends AbstractCommonRule {

    @Autowired
    AutoConfigService autoConfigService;

    @Autowired
    FundsOrgQueryServiceComponent fundsOrgQueryServiceComponent;

    @Autowired
    private FundcommonWalletHandler fundcommonWalletHandler;
    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        BillDataDto billDataDto = (BillDataDto) getParam(map);
        String key = billDataDto.getKey();
        if (!IBillNumConstant.FUND_COLLECTION.equals(billDataDto.getBillnum()) && !IBillNumConstant.FUND_PAYMENT.equals(billContext.getBillnum())) {
            return new RuleExecuteResult();
        }
        List<BizObject> bills = getBills(billContext, map);

        BizObject bill = null;
        BizObject billb;
        if (bills.size() > 0) {
            bill = bills.get(0);
        }
        boolean isfilter = "filter".equals(billDataDto.getExternalData());
        if (isfilter || bill == null) {//如果是过滤区或单据为空，直接跳过
            return new RuleExecuteResult();
        }
        // 针对特征表头参照客户供应商时，直接跳过。
        if ("finbd.bd_expenseitemref".equals(billDataDto.getrefCode())) {
            FilterVO treeCondition = ValueUtils.isNotEmptyObj(billDataDto.getTreeCondition())
                    ? billDataDto.getTreeCondition() : new FilterVO();
            FilterVO condition1 = billDataDto.getCondition();
            if (condition1 != null) {
                SimpleFilterVO[] simpleVOs = condition1.getSimpleVOs();
                if (simpleVOs != null) {
                    treeCondition.setSimpleVOs(simpleVOs);
                    billDataDto.setTreeCondition(treeCondition);
                }
            }
            return new RuleExecuteResult();
        }

        if ("ucf-org-center.bd_adminorgsharetreeref".equals(billDataDto.getrefCode())
                || "bd_adminorgsharetreeref".equals(billDataDto.getrefCode())) {
            bill.set("org", null);
            return new RuleExecuteResult();
        }

        // 汇率类型为本币汇率类型时，取会计主体作为组织参与过滤
        if ("ucfbasedoc.bd_exchangeratetyperef".equals(billDataDto.getrefCode())
                && !"swapOutExchangeRateType_name".equals(billDataDto.getKey())) {
            replaceAccentityUseAccentityRaw(bill, billDataDto);
        }

        if (bill.get("FundCollection_b") == null && bill.get("FundPayment_b") == null) {
            return new RuleExecuteResult();
        }
        // 资金收款单
        if (IBillNumConstant.FUND_COLLECTION.equals(billContext.getBillnum())) {
            billb = ((List<FundCollection_b>) bill.get("FundCollection_b")).get(0);
        } else {
            // 资金付款单
            billb = ((List<FundPayment_b>) bill.get("FundPayment_b")).get(0);
        }
        FilterVO condition = ValueUtils.isNotEmptyObj(billDataDto.getCondition()) ? billDataDto.getCondition() : new FilterVO();
        // 对客户供应商及员工的参照进行过滤
        if (IRefCodeConstant.AA_MERCHANTREF.equals(billDataDto.getrefCode()) || IRefCodeConstant.YSSUPPLIER_AA_VENDOR.equals(billDataDto.getrefCode()) ||
                IRefCodeConstant.BD_STAFF_REF.equals(billDataDto.getrefCode()) || IRefCodeConstant.DOMAIN_BD_STAFF_REF.equals(billDataDto.getrefCode())
                || IRefCodeConstant.BD_STAFF_LEAVE_REF.equals(billDataDto.getrefCode())) {
            DataPermissionRequestDto requestDto = new DataPermissionRequestDto();
            if (IBillNumConstant.FUND_COLLECTION.equals(billContext.getBillnum())) {// 资金收款单
                requestDto.setEntityUri(FundCollection.ENTITY_NAME);
                requestDto.setServiceCode(IServicecodeConstant.FUNDCOLLECTION);
            } else if (IBillNumConstant.FUND_PAYMENT.equals(billContext.getBillnum())) {// 资金付款单
                requestDto.setEntityUri(FundPayment.ENTITY_NAME);
                requestDto.setServiceCode(IServicecodeConstant.FUNDPAYMENT);
            }
            requestDto.setSysCode(ICmpConstant.CMP_MODUAL_NAME);
            requestDto.setYxyUserId(AppContext.getUserId().toString());
            requestDto.setYhtTenantId(InvocationInfoProxy.getTenantid());
            requestDto.setYxyTenantId(AppContext.getTenantId().toString());
            requestDto.setHaveDetail(true);
            RPCStubBeanFactory rpChainBeanFactory = new RPCStubBeanFactory(IDomainConstant.MDD_DOMAIN_AUTH, "c87e2267-1001-4c70-bb2a-ab41f3b81aa3", null, IDataPermissionService.class);
            rpChainBeanFactory.afterPropertiesSet();
            IDataPermissionService remoteBean = (IDataPermissionService) rpChainBeanFactory.getObject();
            // 当组织为空时显示当前会计主体 以及有核算委托关系的销售组织下的启用状态的部门。
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
                    condition.appendCondition(ConditionOperator.and, new SimpleFilterVO("id", ICmpConstant.QUERY_IN, ids));
                }
                condition.appendCondition(ConditionOperator.and, new SimpleFilterVO("merchantAppliedDetail.stopstatus", ICmpConstant.QUERY_IN, new Integer[]{0}));
                condition.appendCondition(ConditionOperator.and, new SimpleFilterVO("merchantAppliedDetail.merchantApplyRangeId.orgId", ICmpConstant.QUERY_IN, orgIds));
            }

            if (IRefCodeConstant.YSSUPPLIER_AA_VENDOR.equals(billDataDto.getrefCode())) { // 供应商
                requestDto.setFieldNameArgs(new String[]{IBussinessConstant.SUPPLIER});
                DataPermissionResponseDto dataPermission = remoteBean.getDataPermission(requestDto);
                List<DataPermissionResultDto> dataPermissionResultDtos = dataPermission.getResultDtos();
                for (DataPermissionResultDto resultDto : dataPermissionResultDtos) {
                    String[] ids = resultDto.getValues();
                    condition.appendCondition(ConditionOperator.and, new SimpleFilterVO("id", ICmpConstant.QUERY_IN, ids));
                }

                condition.appendCondition(ConditionOperator.and, new SimpleFilterVO("vendorextends.stopstatus", ICmpConstant.QUERY_EQ, 0));
                condition.appendCondition(ConditionOperator.and, new SimpleFilterVO("vendorApplyRange.org", ICmpConstant.QUERY_IN, orgIds));
            }

            if (IRefCodeConstant.BD_STAFF_REF.equals(billDataDto.getrefCode())
                    || IRefCodeConstant.DOMAIN_BD_STAFF_REF.equals(billDataDto.getrefCode())
                    || IRefCodeConstant.BD_STAFF_LEAVE_REF.equals(billDataDto.getrefCode())
            ) { // 员工
                requestDto.setFieldNameArgs(new String[]{ICmpConstant.EMPLOYEE});
                AutoConfig autoConfig = autoConfigService.getGlobalConfigEntity();
                if (!"operator_name".equals(key) && autoConfig != null && autoConfig.getAllowCrossOrgGetPerson() != null && autoConfig.getAllowCrossOrgGetPerson()) {
                    //最大查10000条
                    //List<FinOrgDTO> byCondition = orgService.getByCondition(InvocationInfoProxy.getTenantid().toString(), ConditionDTO.newOrgCondition());
                    List<FundsOrgDTO> byCondition = fundsOrgQueryServiceComponent.getByCondition(ConditionDTO.newOrgCondition());
                    ArrayList<String> idList = new ArrayList<>();
                    for (int i = 0; byCondition != null && i < byCondition.size(); i++) {
                        idList.add(byCondition.get(i).getId());
                    }
                    Map<String, Object> context = billContext.getContext();
                    context.put("staffCustomOrg", idList);
                } else {
                    DataPermissionResponseDto dataPermission = remoteBean.getDataPermission(requestDto);
                    List<DataPermissionResultDto> dataPermissionResultDtos = dataPermission.getResultDtos();
                    for (DataPermissionResultDto resultDto : dataPermissionResultDtos) {
                        String[] ids = resultDto.getValues();
                        condition.appendCondition(ConditionOperator.and, new SimpleFilterVO("id", ICmpConstant.QUERY_IN, ids));
                    }
                }
            }
        }

        //客户账户
        if (IRefCodeConstant.AA_MERCHANTBANKREF.equals(billDataDto.getrefCode()) || IRefCodeConstant.PRODUCTCENTER_AA_MERCHANTAGENTFINANCIALREF.equals(billDataDto.getrefCode())) {
            String currency = bill.get(IBussinessConstant.CURRENCY);
            String merchantId = billb.get("oppositeobjectid");
            if (merchantId == null) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100370"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418007F", "请先补充客户信息") /* "请先补充客户信息" */);
            }
            condition.appendCondition(ConditionOperator.and, new SimpleFilterVO("currency", ICmpConstant.QUERY_IN, new String[]{currency}));
            condition.appendCondition(ConditionOperator.and, new SimpleFilterVO("stopstatus", ICmpConstant.QUERY_EQ, 0));
            condition.appendCondition(ConditionOperator.and, new SimpleFilterVO("merchantId", ICmpConstant.QUERY_IN, new String[]{merchantId}));
            // 数币钱包过滤
            fundcommonWalletHandler.filterOppositeAccount(billDataDto, billb, Direction.Debit);
        }

        //供应商账户
        if (IRefCodeConstant.YSSUPPLIER_AA_VENDORBANKREF.equals(billDataDto.getrefCode())) {
            String currency = bill.get(IBussinessConstant.CURRENCY);
            String supplier = billb.get("oppositeobjectid");
            if (supplier == null) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100371"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180080", "请先补充供应商信息") /* "请先补充供应商信息" */);
            }
            condition.appendCondition(ConditionOperator.and, new SimpleFilterVO("currency", ICmpConstant.QUERY_IN, new String[]{currency}));
            condition.appendCondition(ConditionOperator.and, new SimpleFilterVO("stopstatus", ICmpConstant.QUERY_EQ, 0));
            condition.appendCondition(ConditionOperator.and, new SimpleFilterVO("vendor", ICmpConstant.QUERY_IN, new String[]{supplier}));
            // 数币钱包过滤
            fundcommonWalletHandler.filterOppositeAccount(billDataDto, billb, Direction.Debit);
        }
        if (IRefCodeConstant.BD_STAFF_BANKACCT_REF.equals(billDataDto.getrefCode())) { //员工账户
            String currency = bill.get(IBussinessConstant.CURRENCY);
            String employee = billb.get("oppositeobjectid");
            if (employee == null) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101862"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418007E", "请先补充员工信息") /* "请先补充员工信息" */);
            }
            bill.set("employee", employee);
            condition.appendCondition(ConditionOperator.and, new SimpleFilterVO("staff_id", ICmpConstant.QUERY_IN, new String[]{employee}));
            condition.appendCondition(ConditionOperator.and, new SimpleFilterVO("currency", ICmpConstant.QUERY_EQ, currency));
        }
        //资金业务对象过滤禁用状态数据
        if (IRefCodeConstant.TMSP_FUNDBUSINOBJ_REF.equals(billDataDto.getrefCode())) {
            condition.appendCondition(ConditionOperator.and, new SimpleFilterVO("enabled", ICmpConstant.QUERY_EQ, true));
        }
        // 银行网点参照过滤
        if (UCFBASEDOC_BD_BANKDOTREF.equals(billDataDto.getrefCode())) {
            condition.appendCondition(ConditionOperator.and, new SimpleFilterVO("enable", ICmpConstant.QUERY_EQ, 1));
        }
        //资金业务对象账户信息过滤禁用状态信息
        if (IRefCodeConstant.TMSP_FUNDBUSINOBJBANK_REF.equals(billDataDto.getrefCode())) {
            condition.appendCondition(FundBusinObjArchivesItemDTO.BENABLED, ICmpConstant.QUERY_EQ, true);
            condition.appendCondition(FundBusinObjArchivesItemDTO.CURRENCY, ICmpConstant.QUERY_EQ, billb.get("currency"));
            condition.appendCondition(FundBusinObjArchivesItemDTO.MAINID, ICmpConstant.QUERY_EQ, billb.get("oppositeobjectid"));
            //针对资金业务对象的银行账户中 授权会计主体 做过滤
            condition.appendCondition(FundBusinObjArchivesItemDTO.AUTHORIZEDACCCENTITY, ICmpConstant.QUERY_EQ, bill.get("accentity"));
            billDataDto.setCondition(condition);
            // 数币钱包过滤
            fundcommonWalletHandler.filterOppositeAccount(billDataDto, billb, Direction.Debit);
        }

        // 资金计划项目参照过滤
        if ("yonbip-fi-ctmcspl.cspl_plansummaryref".equals(billDataDto.getrefCode())) {
            String accentity = bill.get("accentity").toString();
            if (MapUtils.getString(bill, "currency") == null) {
                throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180078", "请先选择币种") /* "请先选择币种" */);
            }
            String currency = bill.get("currency").toString();
            Date expectdate = bill.get("vouchdate");

            String childrenFieldCheck = MetaDaoHelper.getChilrenField(billContext.getFullname());
            List<BizObject> linesCheck = (List) bill.get(childrenFieldCheck);
            String dept = null;
            if (CollectionUtils.isNotEmpty(linesCheck)) {
                BizObject bizObject = linesCheck.get(0);
                dept = bizObject.get("dept");
            }
            List<Long> resultData = QueryBaseDocUtils.queryPlanStrategyIsEnable(accentity, currency, expectdate, dept);

            if (CollectionUtils.isNotEmpty(resultData)) {
                condition.appendCondition(ConditionOperator.and, new SimpleFilterVO("id", ICmpConstant.QUERY_IN, resultData));
                condition.appendCondition(ConditionOperator.and, new SimpleFilterVO("isEnd", ICmpConstant.QUERY_EQ, 1));
            } else {
                condition.appendCondition(ConditionOperator.and, new SimpleFilterVO("id", ICmpConstant.QUERY_EQ, null));
            }

            // ReceiptType：receive("收款", (short) 1), pay("付款", (short) 2)；
            if (IBillNumConstant.FUND_COLLECTION.equals(billContext.getBillnum())) {// 资金收款单
                condition.appendCondition(ConditionOperator.and, new SimpleFilterVO("receiptType", ICmpConstant.QUERY_EQ, 1));
            } else if (IBillNumConstant.FUND_PAYMENT.equals(billContext.getBillnum())) {// 资金付款单
                condition.appendCondition(ConditionOperator.and, new SimpleFilterVO("receiptType", ICmpConstant.QUERY_EQ, 2));
            }
        }

        // 资金计划项目明细参照过滤
        if ("yonbip-fi-ctmcspl.cspl_plansummary_detailref".equals(billDataDto.getrefCode())) {
            String childrenFieldCheck = MetaDaoHelper.getChilrenField(billContext.getFullname());
            List<BizObject> linesCheck = (List) bill.get(childrenFieldCheck);
            if (CollectionUtils.isNotEmpty(linesCheck)) {
                BizObject bizObject = linesCheck.get(0);
                String fundPlanProject = bizObject.getString("fundPlanProject");
                if (ValueUtils.isEmpty(fundPlanProject)) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102394"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A050800F8", "请先选择资金计划项目!") /* "请先选择资金计划项目!" */);
                }
                long fundPlanProjectLong = Long.parseLong(fundPlanProject);
                Map<Long, List<Long>> fundPlanProjectDetailMap = RemoteDubbo.get(CapitalPlanExecuteService.class, MDD_DOMAIN_CTMCSPL)
                        .queryDetailBySummaryBIds(Collections.singletonList(fundPlanProjectLong));
                if (ValueUtils.isEmpty(fundPlanProjectDetailMap)) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102395"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A050800FA", "该资金计划项目下没有查询到计划明细!") /* "该资金计划项目下没有查询到计划明细!" */);
                }
                List<Long> fundPlanProjectDetailIdsList = fundPlanProjectDetailMap.get(fundPlanProjectLong);
                if (CollectionUtils.isNotEmpty(fundPlanProjectDetailIdsList)) {
                    condition.appendCondition(ConditionOperator.and, new SimpleFilterVO("id", ICmpConstant.QUERY_IN, fundPlanProjectDetailIdsList));
                } else {
                    condition.appendCondition(ConditionOperator.and, new SimpleFilterVO("id", ICmpConstant.QUERY_EQ, -1));
                }
            }
        }

        // 虚拟账户按币种和会计主体过滤
        if (IRefCodeConstant.UCFBASEDOC_BD_ENTERPRISEBANKACCT.equals(billDataDto.getrefCode()) && null != billDataDto.getKey() && billDataDto.getKey().equals("thirdParVirtAccount_name")) {
            String accentity = bill.get("accentity").toString();
            if (MapUtils.getString(bill, "currency") == null) {
                throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180078", "请先选择币种") /* "请先选择币种" */);
            }
            String currency = bill.get("currency").toString();
            condition.appendCondition(ConditionOperator.and, new SimpleFilterVO("orgid", ICmpConstant.QUERY_EQ, accentity));
            condition.appendCondition(ConditionOperator.and, new SimpleFilterVO(ICmpConstant.CURRENCY_REf, ICmpConstant.QUERY_EQ, currency));
            condition.appendCondition(ConditionOperator.and, new SimpleFilterVO("enable", ICmpConstant.QUERY_EQ, 1));
            condition.appendCondition(ConditionOperator.and, new SimpleFilterVO("acctopentype", ICmpConstant.QUERY_EQ, 3));
        }
        // 内部单位
        if (IRefCodeConstant.UCFBASEDOC_BD_ENTERPRISEBANKACCT.equals(billDataDto.getrefCode()) && "oppositeaccountname".equals(billDataDto.getKey())) {
            String oppositeObjectId = billb.getString("oppositeobjectid");
            if (!ValueUtils.isNotEmptyObj(oppositeObjectId)) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102396"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1945A97205580009", "请先选择当前明细行上的收付款单位名称！") /* "请先选择当前明细行上的收付款单位名称！" */);
            }
            bill.set(ICmpConstant.ACCENTITY, oppositeObjectId);
            condition.appendCondition(ConditionOperator.and, new SimpleFilterVO("orgid", "eq", oppositeObjectId));
            billDataDto.setMasterOrgValue(oppositeObjectId);
            Map<String, Object> map1 = new HashMap<>();
            map1.put("orgId", oppositeObjectId);
            billDataDto.setCustMap(map1);

            Object currency = bill.get(ICmpConstant.CURRENCY);
            condition.appendCondition(ConditionOperator.and, new SimpleFilterVO("currencyList.currency", "eq", currency));
            // 数币钱包过滤
            fundcommonWalletHandler.filterOppositeAccount(billDataDto, billb, Direction.Debit);
        }

        // 统收统支关系组参照过滤
        if ("yonbip-fi-ctmstwb.stwb_incomeandexpenditurelistref".equals(billDataDto.getrefCode())) {
            String childrenFieldCheck = MetaDaoHelper.getChilrenField(billContext.getFullname());
            List<BizObject> linesCheck = (List) bill.get(childrenFieldCheck);
            String currency = null;
            if (!linesCheck.isEmpty()) {
                BizObject bizObjectSub = linesCheck.get(0);
                currency = bizObjectSub.getString("settleCurrency");
            }
            // 收付类型【reauth】：1=>收款；2=>付款
            String recpaytype = null;
            String businessbilltype = null;
            if (IBillNumConstant.FUND_COLLECTION.equals(billContext.getBillnum())) {// 资金收款单
                recpaytype = "1";
                businessbilltype = "25";
            } else if (IBillNumConstant.FUND_PAYMENT.equals(billContext.getBillnum())) {// 资金付款单
                recpaytype = "2";
                businessbilltype = "26";
            }
            String accent = bill.getString(ICmpConstant.ACCENTITY);
            String tradeType = bill.getString(ICmpConstant.TRADE_TYPE);

            try {
                Set<Long> incomRefIdsList = RemoteDubbo.get(IOpenApiService.class,
                        IDomainConstant.MDD_DOMAIN_STWB).queryIncomRefList(accent, currency, recpaytype, businessbilltype, tradeType);
                if (CollectionUtils.isNotEmpty(incomRefIdsList)) {
                    condition.appendCondition(ConditionOperator.and, new SimpleFilterVO(ICmpConstant.PRIMARY_ID, "in", incomRefIdsList.toArray(new Long[0])));
                } else {
                    condition.appendCondition(ConditionOperator.and, new SimpleFilterVO("1", "eq", "2"));
                }
            } catch (Exception e) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102397"), e.getMessage());
            }
        }

        if (IRefCodeConstant.UCFBASEDOC_BD_ENTERPRISEBANKACCT.equals(billDataDto.getrefCode())
                && "enterprisebankaccount_name".equals(billDataDto.getKey())) {
            String childrenFieldCheck = MetaDaoHelper.getChilrenField(billContext.getFullname());
            List<BizObject> linesCheck = (List) bill.get(childrenFieldCheck);
            if (!linesCheck.isEmpty()) {
                String settleCurrency = linesCheck.get(0).getString("settleCurrency");
                bill.set("currency", settleCurrency);
                // 数币钱包过滤
                // 结算方式的业务属性为银行业务
                // 数币钱包进行过滤
                fundcommonWalletHandler.filterEnterpriseBankAccount(billDataDto, linesCheck.get(0), Direction.Credit);
            }

        }

        //成本中心需要根据会计主体进行过滤
        if ("finbd.bd_costcenterref".equals(billDataDto.getrefCode()) && "costcenter_name".equals(billDataDto.getKey())) {
            String dept = null;
            if (IBillNumConstant.FUND_COLLECTION.equals(billContext.getBillnum())) {// 资金收款单
                dept = ((List<FundCollection_b>) bill.get("FundCollection_b")).get(0).get("dept");
            } else if (IBillNumConstant.FUND_PAYMENT.equals(billContext.getBillnum())) {// 资金付款单
                dept = ((List<FundPayment_b>) bill.get("FundPayment_b")).get(0).get("dept");
            }
            if (dept != null) {
                condition.appendCondition(ConditionOperator.and, new SimpleFilterVO("relations.dept", "eq", dept));
            }
            condition.appendCondition(ConditionOperator.and, new SimpleFilterVO("accentity", "eq", bill.get("accentity")));
            condition.appendCondition(ConditionOperator.and, new SimpleFilterVO("effect", "eq", true));
            replaceAccentityUseAccentityRaw(bill, billDataDto);
        }

        // 统收统支关系账户参照过滤
        if (IRefCodeConstant.UCFBASEDOC_BD_ENTERPRISEBANKACCT.equals(billDataDto.getrefCode())
                && "incomeAndExpendBankAccount_account".equals(billDataDto.getKey())) {
            String childrenFieldCheck = MetaDaoHelper.getChilrenField(billContext.getFullname());
            List<BizObject> linesCheck = (List) bill.get(childrenFieldCheck);
            if (!linesCheck.isEmpty()) {
                String incomeAndExpendRelationGroup = linesCheck.get(0).getString("incomeAndExpendRelationGroup");
                if (!ValueUtils.isNotEmptyObj(incomeAndExpendRelationGroup)) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102398"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A050800FB", "请先选择当前明细行上的统收统支关系组！") /* "请先选择当前明细行上的统收统支关系组！" */);
                }
                try {
                    BizObject controlled = MetaDaoHelper.findById(Controlled.ENTITY_NAME, incomeAndExpendRelationGroup, 1);
                    QuerySchema querySchema = QuerySchema.create().addSelect("accentity");
                    querySchema.appendQueryCondition(QueryCondition.name("id").eq(controlled.get("mainid")));
                    List<Map<String, Object>> query = MetaDaoHelper.query(IncomeAndExpenditure.ENTITY_NAME, querySchema);
                    Object accentity = query.get(0).get("accentity");
                    IncomeAndExpenditureReqVO incomeAndExpenditureReqVO = new IncomeAndExpenditureReqVO();
                    List<String> controlId = new ArrayList<>();
                    controlId.add(incomeAndExpendRelationGroup);
                    incomeAndExpenditureReqVO.setControllId(controlId);
                    List<IncomeAndExpenditureResVO> incomeAndExpenditureResVOS = RemoteDubbo.get(IOpenApiService.class,
                            IDomainConstant.MDD_DOMAIN_STWB).queryControllMarginaccList(incomeAndExpenditureReqVO);
                    if (CollectionUtils.isNotEmpty(incomeAndExpenditureResVOS)) {
                        List<String> ids = new ArrayList<>();
                        for (IncomeAndExpenditureResVO incomeAndExpenditureResVO : incomeAndExpenditureResVOS) {
                            ids.add(incomeAndExpenditureResVO.getMarginaccount());
                        }
                        bill.set("accentity", accentity.toString());
                        billDataDto.setMasterOrgValue(accentity.toString());
                        Map<String, Object> map1 = new HashMap<>();
                        map1.put("orgId", accentity);
                        billDataDto.setCustMap(map1);
                        if (!linesCheck.isEmpty()) {
                            BizObject bizObjectSub = linesCheck.get(0);
                            bill.set("currency", bizObjectSub.getString("settleCurrency"));
                        }
                        condition.appendCondition(ConditionOperator.and, new SimpleFilterVO(ICmpConstant.PRIMARY_ID, "in", ids.toArray(new String[0])));
                    } else {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102399"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A050800F9", "根据统收统支关系组id，调用结算接口查询统收统支银行账号id为空！") /* "根据统收统支关系组id，调用结算接口查询统收统支银行账号id为空！" */);
                    }
                } catch (Exception e) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102400"), e.getMessage());
                }
            }
        }
        //税种
        if ("yonbip-fi-taxpubdoc.RefTable_839443ba26".equals(billDataDto.getrefCode())) {
            //CZFW-435791CM202400594【资金收款单】税种参照优化：税种档案过滤时，税率类型仅能参照到比例的，不需要参照出定额的，目前未支持定额场景
            condition.appendCondition(ConditionOperator.and, new SimpleFilterVO("taxRateType", ICmpConstant.QUERY_EQ, 2));
        }
        billDataDto.setCondition(condition);
        return new RuleExecuteResult();
    }

    /**
     * 将资金组织的id替换成会计主体的id
     *
     * @return
     */
    private Map<String, Object> replaceAccentityUseAccentityRaw(BizObject bill, BillDataDto billDataDto) {
        Object accentity = bill.getString("accentityRaw");
        if (accentity == null) {
            accentity = CmpMetaDaoHelper.getAccentityRaw(bill.getString("accentity"));
        }
        if (accentity == null) {
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400386", "没有获取资金组织以及会计主体") /* "没有获取资金组织以及会计主体" */);
        }
        bill.set("accentity", accentity.toString());
        billDataDto.setMasterOrgValue(accentity.toString());
        Map<String, Object> map = new HashMap<>();
        map.put("orgId", accentity.toString());
        billDataDto.setCustMap(map);
        return map;
    }

    private void filterDept(BizObject data, Object value, Set<String> filterOrgMC) throws Exception {
        /** 1）当前会计主体及通过核算委托关系的组织下的部门，以及有职能共享关系的业务单元下的部门
         2）如果有销售组织，就取销售组织下的部门，以及和他有职能共享关系的部门 */
        Set<String> orgids = new HashSet<String>();
        Set<String> deptids = new HashSet<String>();
        buildDeptInfo(orgids, deptids, data);

        if (!deptids.isEmpty() && deptids.contains(data.get("dept").toString())) {
            orgids.addAll(filterOrgMC);
            if (!orgids.contains(value)) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102401"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:YS_CTM_CM-BE_LOCAL_00050031", "该部门无权限，非当前资金组织及通过核算委托关系的组织下的部门，以及有职能共享关系的业务单元下的部门") /* "该部门无权限，非当前资金组织及通过核算委托关系的组织下的部门，以及有职能共享关系的业务单元下的部门" */);
            }
        } else {
            if (filterOrgMC != null && !filterOrgMC.contains(value)) {
                throw new CtmException(
                        com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418007C", "组织管控数据校验异常:") /* "组织管控数据校验异常:" */ + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418007B", "部门") /* "部门" */
                                + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:YS_CTM_CM-BE_LOCAL_00050030", "与资金组织业务组织不匹配") /* "与资金组织业务组织不匹配" */);
            }
        }
    }

    /**
     * 1）当前会计主体及通过核算委托关系的组织下的部门，以及有职能共享关系的业务单元下的部门
     * 2）如果有业务组织，就取业务组织下的部门，以及和他有职能共享关系的部门
     * 3)当前会计主体、业务组织（库存组织）对应的部门+与会计主体有核算委托关系的组织下的部门
     */
    private void buildDeptInfo(Set<String> orgids, Set<String> deptids, BizObject data) throws Exception {
        List<String> bizorgs = AuthUtil.getBizObjectAttr(data, IBillConst.ORG);
        Set<String> orgidts = null;
        Set<String> deptidts = null;
        if (bizorgs == null) {
            // 当组织为空时显示当前会计主体 以及有核算委托关系的销售组织下的启用状态的部门。
            List<String> accentity = AuthUtil.getBizObjectAttr(data, IBillConst.ACCENTITY);
            //根据委托关系
            orgidts = FIDubboUtils.getDelegateHasSelf(accentity.toArray(new String[0]));
            if (orgidts == null) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102402"), "keynull");
            }
            //根据职能共享
            deptidts = FIDubboUtils.getDeptShare(orgidts.toArray(new String[0]));
            orgidts = FIDubboUtils.getOrgShareHasSelf(orgidts.toArray(new String[0]));
            deptids.addAll(deptidts);
            orgids.addAll(orgidts);
        } else {
            //当前会计主体、业务组织（库存组织）对应的部门+与会计主体有核算委托关系的组织下的部门
            orgidts = FIDubboUtils.getOrgShareHasSelf(bizorgs.toArray(new String[0]));
            deptidts = FIDubboUtils.getDeptShare(bizorgs.toArray(new String[0]));

            orgids.addAll(orgidts);
            deptids.addAll(deptidts);
        }
    }

}
