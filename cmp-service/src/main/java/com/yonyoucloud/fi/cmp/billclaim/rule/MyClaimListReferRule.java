package com.yonyoucloud.fi.cmp.billclaim.rule;

import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.iuap.org.service.itf.core.IFuncTypeQueryService;
import com.yonyou.ucf.basedoc.model.BankVO;
import com.yonyou.ucf.basedoc.model.ResultPager;
import com.yonyou.ucf.basedoc.model.rpcparams.project.ProjectQueryParam;
import com.yonyou.ucf.basedoc.service.itf.IProjectService;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.FilterCommonVO;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.FilterVO;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.SimpleFilterVO;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.ctm.stwb.paramsetting.pubitf.ISettleParamPubQueryService;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.ctm.stwb.openapi.IOpenApiService;
import com.yonyoucloud.ctm.stwb.reqvo.IncomeAndExpenditureReqVO;
import com.yonyoucloud.ctm.stwb.respvo.IncomeAndExpenditureResVO;
import com.yonyoucloud.fi.basecom.check.AuthCheckCommonUtil;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.cmp.auth.OrgDataPermissionService;
import com.yonyoucloud.fi.cmp.autoparam.AutoConfig;
import com.yonyoucloud.fi.cmp.autoparam.service.AutoConfigService;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.bankreconciliation.service.count.BillCountUtil;
import com.yonyoucloud.fi.cmp.billclaim.BillClaim;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.IRefCodeConstant;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import com.yonyoucloud.fi.cmp.enums.BusinessModel;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.Direction;
import com.yonyoucloud.fi.cmp.util.StringUtils;
import com.yonyoucloud.fi.cmp.util.basedoc.EnterpriseBankQueryService;
import com.yonyoucloud.fi.stct.api.openapi.IBusinessDelegationApiService;
import com.yonyoucloud.fi.stct.api.openapi.common.dto.Result;
import com.yonyoucloud.fi.stct.api.openapi.request.QueryDelegationReqVo;
import com.yonyoucloud.fi.stct.api.openapi.vo.businessDelegation.BusinessDelegationVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @description: 我的认领列表 参照前规则
 * @author: wanxbo@yonyou.com
 * @date: 2022/5/27 10:44
 */
@Slf4j
@Component
public class MyClaimListReferRule extends AbstractCommonRule {
    //参照billno
    public static String STWB_SETTLEBENCHREF = "stwb.stwb_settlebenchref";
    //结算中心code
    public static String SETTLEMENT_CODE = "settlementorg";
    @Autowired
    private ISettleParamPubQueryService settleParamPubQueryService;
    @Resource
    IFuncTypeQueryService iFuncTypeQueryService;
    @Resource
    IOpenApiService iOpenApiService;
    @Resource
    IBusinessDelegationApiService iBusinessDelegationApiService;
    @Autowired
    AutoConfigService autoConfigService;
    @Resource
    OrgDataPermissionService orgDataPermissionService;
    @Autowired
    EnterpriseBankQueryService enterpriseBankQueryService;
    @Autowired
    BillCountUtil billCountUtil;
    @Autowired
    IProjectService projectService;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        String billnum = billContext.getBillnum();
        BillDataDto bill = (BillDataDto) getParam(map);
        String serviceCode = AppContext.getThreadContext("serviceCode");
        if ("filter".equals(bill.getExternalData())) {
            if ("ucfbasedoc.bd_enterprisebankacct".equals(bill.getrefCode()) && ("cmp_billclaimcard".equals(billnum) || "cmp_mybillclaimlist".equals(billnum))) {
                //获取授权使用组织
                FilterVO filterVO = bill.getCondition();
                if ("claimaccount".equals(bill.getKey()) && bill.getrefCode().equals(IRefCodeConstant.UCFBASEDOC_BD_ENTERPRISEBANKACCT)) {//实际认领账户
                    // 根据实际认领单位过滤
                    SimpleFilterVO[] commonVOs = filterVO.getSimpleVOs();
                    String actualclaimaccentiry = null;
                    for (SimpleFilterVO vo : commonVOs) {
                        if ("actualclaimaccentiry".equals(vo.getField()) && !Objects.isNull(vo.getValue1()) && vo.getValue1() instanceof String) {
                            actualclaimaccentiry = (String) vo.getValue1();
                            vo.setField(null);
                            vo.setValue1(null);
                        }
                    }
                    if (!Objects.isNull(actualclaimaccentiry)) {
                        if (null == filterVO) {
                            filterVO = new FilterVO();
                        }
                        filterVO.appendCondition(ConditionOperator.and, new SimpleFilterVO("orgid", ICmpConstant.QUERY_EQ,
                                actualclaimaccentiry));
                        bill.setCondition(filterVO);
                    }
                }
                if (null != filterVO) {
                    SimpleFilterVO[] commonVOs = filterVO.getSimpleVOs();
                    Boolean hasOrgid = false;// 所属组织
                    Boolean hasAccentity = false; // 授权使用组织
                    List<String> accentityList = new ArrayList<>();
                    List<String> orgList = new ArrayList<>();
                    for (SimpleFilterVO vo : commonVOs) {
                        if ("orgid".equals(vo.getField()) && ((List) vo.getValue1()).size() > 0) {
                            hasOrgid = true;
                            orgList = (List) vo.getValue1();
                            vo.setField(null);
                            vo.setValue1(null);
                        }
                        if ("accentity".equals(vo.getField()) && ((List) vo.getValue1()).size() > 0) {
                            hasAccentity = true;
                            accentityList = (List) vo.getValue1();
                            vo.setField(null);
                            vo.setValue1(null);
                        }
                    }
                    if (hasOrgid && hasAccentity) {
                        for (SimpleFilterVO vo : commonVOs) {
                            if (vo.getField() == null) {
                                vo.setConditions(null);
                            }
                        }
                        List<String> accountByAccentity = enterpriseBankQueryService.getAccountsByAccentity(orgList);
                        List<String> accountByOrg = enterpriseBankQueryService.getAccounts(accentityList);
                        accountByAccentity.addAll(accountByOrg);
                        bill.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("id", ICmpConstant.QUERY_IN, accountByAccentity));
                    } else if (hasOrgid && !hasAccentity) {
                        for (SimpleFilterVO vo : commonVOs) {
                            if (vo.getField() == null) {
                                vo.setConditions(null);
                            }
                        }
                        List<String> accountByAccentity = enterpriseBankQueryService.getAccountsByAccentity(orgList);
                        bill.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("id", ICmpConstant.QUERY_IN, accountByAccentity));
                    } else if (!hasOrgid && hasAccentity) {
                        for (SimpleFilterVO vo : commonVOs) {
                            if (vo.getField() == null) {
                                vo.setConditions(null);
                            }
                        }
                        List<String> accountByAccentity = enterpriseBankQueryService.getAccounts(accentityList);
                        bill.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("id", ICmpConstant.QUERY_IN, accountByAccentity));
                    } else {
                        // 获取授权使用组织
                        Set<String> orgs = orgDataPermissionService.queryAuthorizedOrgByServiceCode(serviceCode);
                        List<String> accounts = enterpriseBankQueryService.getAccounts(orgs);
                        bill.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("id", ICmpConstant.QUERY_IN, accounts));
                    }
                }
            }
        }
        // 资金结算工作台参照
        if (bill.getrefCode().equals(STWB_SETTLEBENCHREF)) {
            if (bill.getData() != null) {
                FilterVO filterVO = bill.getCondition();
                FilterCommonVO[] filterCommonVOS = filterVO.getCommonVOs();
                //兜底异常治理，为空时直接返回
                if (filterCommonVOS == null || filterCommonVOS.length == 0) {
                    return new RuleExecuteResult();
                }
                FilterCommonVO exchangePaymentAmount = Arrays.stream(filterCommonVOS)
                        .filter(item -> "exchangePaymentAmount".equals(item.getItemName())).findFirst().orElse(null);

                // 直连结算成功
                SimpleFilterVO settlesuccess = new SimpleFilterVO(ConditionOperator.and);
                //是否是直联
                settlesuccess.addCondition(new SimpleFilterVO("isdirectacc", "eq", "1"));
                //结算明细状态 3：结算成功，7部分成功
                settlesuccess.addCondition(new SimpleFilterVO("statementdetailstatus", "in", new String[]{"3", "7"}));
                //直联只能关联结算成功的，取实际结算金额
                if (exchangePaymentAmount != null) {
                    settlesuccess.addCondition(new SimpleFilterVO("actualExchangePaymentAmount", "between", exchangePaymentAmount.getValue1(), exchangePaymentAmount.getValue2()));
                }
                //是否已关联账单
                settlesuccess.addCondition(new SimpleFilterVO("isRelateCheckBill", "eq", "0"));
                //结算单状态 4：结算中 5：已办结
                settlesuccess.addCondition(new SimpleFilterVO("mainid.statementstatus", "in", new String[]{"4", "5"}));

                // 非直连 结算中
                SimpleFilterVO settleing = new SimpleFilterVO(ConditionOperator.and);
                //是否是直联
                settleing.addCondition(new SimpleFilterVO("isdirectacc", "eq", "0"));
                if (exchangePaymentAmount != null) {
                    SimpleFilterVO settleingOrAll = new SimpleFilterVO(ConditionOperator.or);
                    //结算明细状态 2：结算中 ,金额取预估结算金额
                    SimpleFilterVO settleingOr1 = new SimpleFilterVO(ConditionOperator.and);
                    settleingOr1.addCondition(new SimpleFilterVO("statementdetailstatus", "eq", "2"));
                    settleingOr1.addCondition(new SimpleFilterVO("exchangePaymentAmount", "between", exchangePaymentAmount.getValue1(), exchangePaymentAmount.getValue2()));
                    settleingOrAll.addCondition(settleingOr1);

                    //结算明细状态 3：结算成功，7部分成功 ,金额取实际结算金额actualExchangePaymentAmount
                    SimpleFilterVO settleingOr2 = new SimpleFilterVO(ConditionOperator.and);
                    settleingOr2.addCondition(new SimpleFilterVO("statementdetailstatus", "in", new String[]{"3", "7"}));
                    settleingOr2.addCondition(new SimpleFilterVO("actualExchangePaymentAmount", "between", exchangePaymentAmount.getValue1(), exchangePaymentAmount.getValue2()));
                    settleingOrAll.addCondition(settleingOr2);

                    settleing.addCondition(settleingOrAll);
                } else {
                    //结算明细状态 2：结算中  3：结算成功，7部分成功
                    settleing.addCondition(new SimpleFilterVO("statementdetailstatus", "in", new String[]{"2", "3", "7"}));
                }
                //是否已关联账单
                settleing.addCondition(new SimpleFilterVO("isRelateCheckBill", "eq", "0"));
                //结算单状态 4：结算中 5：已办结
                settleing.addCondition(new SimpleFilterVO("mainid.statementstatus", "in", new String[]{"4", "5"}));

                // 修改后 - 过滤掉exchangePaymentAmount项并重新组合数组
                filterCommonVOS = Arrays.stream(filterCommonVOS)
                        .filter(item -> !"exchangePaymentAmount".equals(item.getItemName()))
                        .toArray(FilterCommonVO[]::new);
                filterVO.setCommonVOs(filterCommonVOS);
                bill.setCondition(filterVO);

                bill.getCondition().appendCondition(ConditionOperator.or, new SimpleFilterVO[]{settlesuccess, settleing});
            }
        }//投融资四个单据，要过滤已关联数据
        else if (bill.getrefCode().equals("yonbip-fi-ctmtlm.tlm_payinterestref") ||
                bill.getrefCode().equals("yonbip-fi-ctmtlm.tlm_financepayfeeRef") || bill.getrefCode().equals("yonbip-fi-ctmtlm.tlm_financeinStatementRef")) {
            if (bill.getData() != null) {
                bill.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("isRelateCheckBill", ICmpConstant.QUERY_NEQ, "1"));
            }
            //融入单据是小写s，其他单据是大写。auditStatus == 1 代表的是保存态数据
            if (bill.getrefCode().equals("yonbip-fi-ctmtlm.tlm_financeinStatementRef")) {
                bill.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("auditstatus", ICmpConstant.QUERY_EQ, "1"));
            } else {
                bill.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("auditStatus", ICmpConstant.QUERY_EQ, "1"));
            }
        } else if (bill.getrefCode().equals("yonbip-fi-ctmtlm.tlm_repaymentRef")) {
            if (bill.getData() != null) {
                bill.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("associateStatement", ICmpConstant.QUERY_NEQ, "1"));
                bill.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("auditStatus", ICmpConstant.QUERY_EQ, "1"));
            }
        } else if (bill.getrefCode().equals("yonbip-fi-ctmtlm.tlm_purchasePayRef")) {
            if (bill.getData() != null) {
                bill.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("purchaseType", ICmpConstant.QUERY_EQ, "1"));
            }
        } else if ("actualclaimaccentiry_name".equals(bill.getKey()) && IRefCodeConstant.FUNDS_ORGTREE.equals(bill.getrefCode())) {
            //我的认领使用的实际认领单位[资金组织]过滤
            //实际认领单位过滤
            /**
             * 判断参数开启状态
             * 1，判断会计主体是否为结算中心
             * 2，结算中心-->资金业务委托关系
             * 3，非结算中心--->统收统支关系设置
             */
            List<BillClaim> billClaim = (List<BillClaim>) bill.getData();
            String accentity = billClaim.get(0).getAccentity();
            /**
             * 判断业务单元是否有结算中心职能
             * tenantId
             * orgId
             * funcType 职能类型code（会计主体：financeorg  结算中心：settlementorg）
             *
             */
            Set<String> orgsSet = AuthCheckCommonUtil.getAuthOrgs(billContext);
            List<String> expendIds = new ArrayList<>();
//            expendIds.addAll(orgsSet);
            Boolean agentFlag = autoConfigService.getEnableBizDelegationMode();// 是否开启结算中心代理模式
            Boolean inoutFlag = autoConfigService.getUnifiedIEModelWhenClaim();// 是否开启统收统支模式
            if (agentFlag && inoutFlag) {
                Boolean settlementFlag = iFuncTypeQueryService.orgHasFunc(InvocationInfoProxy.getTenantid().toString(), accentity, "settlementorg");
                if (settlementFlag) {
                    //查询资金业务委托关系 结算中心查会计主体
                    QueryDelegationReqVo queryDelegationReqVo = new QueryDelegationReqVo();
                    queryDelegationReqVo.setSettlementCenter(accentity);
                    queryDelegationReqVo.setEnableoutagestatus(0);
                    Result result = iBusinessDelegationApiService.queryBusinessDelegation(queryDelegationReqVo);
                    if (result.getData() != null && result.getCode() != 404) {
                        List<BusinessDelegationVo> businessDelegationVos = (List<BusinessDelegationVo>) result.getData();
//                        List<String> expendIds = new ArrayList<>();
//                        expendIds.add(accentity);
                        if (businessDelegationVos != null && businessDelegationVos.size() > 0) {
                            for (BusinessDelegationVo businessDelegationVo : businessDelegationVos) {
                                expendIds.add(businessDelegationVo.getAccentity());
                            }
                        }
                        FilterVO filterVO = bill.getTreeCondition();
                        if (null == filterVO) {
                            filterVO = new FilterVO();
                        }
                        if (expendIds.size() == 0) {
                            expendIds.add(accentity);
                        }
                        expendIds = getAthorAcctitys(expendIds);
                        filterVO.appendCondition(ConditionOperator.and, new SimpleFilterVO("id", ICmpConstant.QUERY_IN, expendIds));
                        bill.setTreeCondition(filterVO);
                    } else {
                        FilterVO filterVO = bill.getTreeCondition();
                        if (null == filterVO) {
                            filterVO = new FilterVO();
                        }
                        if (expendIds.size() == 0) {
                            expendIds.add(accentity);
                        }
                        expendIds = getAthorAcctitys(expendIds);
                        filterVO.appendCondition(ConditionOperator.and, new SimpleFilterVO("id", ICmpConstant.QUERY_IN, expendIds));
                        bill.setTreeCondition(filterVO);
                    }
                } else {
                    // 查询统收统支关系：上级查下级
                    // 根据 实际结算主体+币种+收付授权(1收2付)+统收统支银行账户+启用 查询 受控主体
                    expendIds = this.getAcc(billClaim, bill);

                    FilterVO filterVO = bill.getTreeCondition();
                    if (null == filterVO) {
                        filterVO = new FilterVO();
                    }
                    if (expendIds.size() == 0) {
                        expendIds.add(accentity);
                    }
                    expendIds = getAthorAcctitys(expendIds);
                    filterVO.appendCondition(ConditionOperator.and, new SimpleFilterVO("id", ICmpConstant.QUERY_IN, expendIds));
                    bill.setTreeCondition(filterVO);
                }
            } else if (agentFlag && !inoutFlag) {
                Boolean settlementFlag = iFuncTypeQueryService.orgHasFunc(InvocationInfoProxy.getTenantid().toString(), accentity, "settlementorg");
                if (settlementFlag) {
                    //查询资金业务委托关系 结算中心查会计主体
                    QueryDelegationReqVo queryDelegationReqVo = new QueryDelegationReqVo();
                    queryDelegationReqVo.setSettlementCenter(accentity);
                    queryDelegationReqVo.setEnableoutagestatus(0);
                    Result result = iBusinessDelegationApiService.queryBusinessDelegation(queryDelegationReqVo);
                    if (result.getData() != null && result.getCode() != 404) {
//                        List<String> expendIds = new ArrayList<>();
//                        expendIds.add(accentity);
                        List<BusinessDelegationVo> businessDelegationVos = (List<BusinessDelegationVo>) result.getData();
                        if (businessDelegationVos != null && businessDelegationVos.size() > 0) {
                            for (BusinessDelegationVo businessDelegationVo : businessDelegationVos) {
                                expendIds.add(businessDelegationVo.getAccentity());
                            }
                        }
                        FilterVO filterVO = bill.getTreeCondition();
                        if (null == filterVO) {
                            filterVO = new FilterVO();
                        }
                        if (expendIds.size() == 0) {
                            expendIds.add(accentity);
                        }
                        expendIds = getAthorAcctitys(expendIds);
                        filterVO.appendCondition(ConditionOperator.and, new SimpleFilterVO("id", ICmpConstant.QUERY_IN, expendIds));
                        bill.setTreeCondition(filterVO);
                    } else {
                        FilterVO filterVO = bill.getTreeCondition();
                        if (null == filterVO) {
                            filterVO = new FilterVO();
                        }
                        if (expendIds.size() == 0) {
                            expendIds.add(accentity);
                        }
                        expendIds = getAthorAcctitys(expendIds);
                        filterVO.appendCondition(ConditionOperator.and, new SimpleFilterVO("id", ICmpConstant.QUERY_IN, expendIds));
                        bill.setTreeCondition(filterVO);
                    }
                } else {
                    FilterVO filterVO = bill.getTreeCondition();
                    if (null == filterVO) {
                        filterVO = new FilterVO();
                    }
                    if (expendIds.size() == 0) {
                        expendIds.add(accentity);
                    }
                    expendIds = getAthorAcctitys(expendIds);
                    filterVO.appendCondition(ConditionOperator.and, new SimpleFilterVO("id", ICmpConstant.QUERY_IN, expendIds));
                    bill.setTreeCondition(filterVO);
                }
            } else if (!agentFlag && inoutFlag) {
                // 查询统收统支关系：上级查下级
                // 根据 实际结算主体+币种+收付授权(1收2付)+统收统支银行账户+启用 查询 受控主体
                expendIds = this.getAcc(billClaim, bill);

                FilterVO filterVO = bill.getTreeCondition();
                if (null == filterVO) {
                    filterVO = new FilterVO();
                }
                if (expendIds.size() == 0) {
                    expendIds.add(accentity);
                }
                expendIds = getAthorAcctitys(expendIds);
                filterVO.appendCondition(ConditionOperator.and, new SimpleFilterVO("id", ICmpConstant.QUERY_IN, expendIds));
                bill.setTreeCondition(filterVO);
            } else {
                expendIds.addAll(orgsSet);
                FilterVO filterVO = bill.getTreeCondition();
                if (null == filterVO) {
                    filterVO = new FilterVO();
                }
                expendIds = getAthorAcctitys(expendIds);
                filterVO.appendCondition(ConditionOperator.and, new SimpleFilterVO("id", ICmpConstant.QUERY_IN, expendIds));
                bill.setTreeCondition(filterVO);
            }

        } else if ("actualclaimaccentiry".equals(bill.getKey()) && IRefCodeConstant.FUNDS_ORGTREE.equals(bill.getrefCode())) {
            //实际认领单位过滤
            FilterVO filterVO = bill.getTreeCondition();
            Set<String> orgSet = orgDataPermissionService.queryAuthorizedOrgByServiceCode(serviceCode);
            if (!orgSet.isEmpty() && orgSet.size() > 0) {
                filterVO.appendCondition(ConditionOperator.and, new SimpleFilterVO("id", ICmpConstant.QUERY_IN, orgSet));
                bill.setTreeCondition(filterVO);
            } else {
                filterVO.appendCondition(ConditionOperator.and, new SimpleFilterVO("id", ICmpConstant.QUERY_IN,
                        new HashSet<>()));
                bill.setTreeCondition(filterVO);
            }
        } else if ("claimaccount_name".equals(bill.getKey()) && bill.getrefCode().equals(IRefCodeConstant.UCFBASEDOC_BD_ENTERPRISEBANKACCT)) {//认领账户
            // 根据实际认领单位过滤
            List<BillClaim> billClaim = (List<BillClaim>) bill.getData();
            if (billClaim.get(0).getActualclaimaccentiry() != null) {
                FilterVO filterVO = bill.getCondition();
                if (null == filterVO) {
                    filterVO = new FilterVO();
                }
                filterVO.appendCondition(ConditionOperator.and, new SimpleFilterVO("orgid", ICmpConstant.QUERY_EQ,
                        billClaim.get(0).getActualclaimaccentiry()));
                bill.setCondition(filterVO);
            }
            // 根据资金切分方式过滤
            BaseRefRpcService baseRefRpcService = AppContext.getBean(BaseRefRpcService.class);
            BankVO bankVO = baseRefRpcService.queryBankTypeByCode("system-001");
            // 是否启用内转协议进行资金切分 是否启用
            if (getInterTransAgreeFundSplitting()) {
                // 业务模式未结算中心代理 只能参照到内部账户
                if (billClaim.get(0).getBusinessmodel() != null && billClaim.get(0).getBusinessmodel() == BusinessModel.FundCenter_Agent.getCode()) {
                    if (bankVO != null) {
                        FilterVO filterVO = bill.getCondition();
                        if (null == filterVO) {
                            filterVO = new FilterVO();
                        }
                        filterVO.appendCondition(ConditionOperator.and, new SimpleFilterVO("bank", ICmpConstant.QUERY_EQ, bankVO.getId()));
                        // TODO 内部银行账户的开户行字段，要等于单据会计主体字段

                        QuerySchema querySchema = QuerySchema.create().addSelect("*");
                        QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("settlementCenter").eq(billClaim.get(0).getAccentity()));
                        querySchema.addCondition(group);
                        List<BizObject> list = MetaDaoHelper.queryObject("stct.internalaccount.InternalAccount", querySchema, "yonbip-fi-ctmstct");
                        List<String> internalAccountIdList = new ArrayList<>();
                        list.forEach(e -> internalAccountIdList.add(e.get("sourceAccountId")));
                        filterVO.appendCondition(ConditionOperator.and, new SimpleFilterVO("id", ICmpConstant.QUERY_IN, internalAccountIdList));

                        bill.setCondition(filterVO);
                    }
                } else {
                    if (billClaim.get(0).getFundsplitmethod() != null && billClaim.get(0).getFundsplitmethod() == 1) {
                        //资金切分方式=内部账户划转 参照内部账户档案
                        // 查询结算中心 system-001
                        if (bankVO != null) {
                            FilterVO filterVO = bill.getCondition();
                            if (null == filterVO) {
                                filterVO = new FilterVO();
                            }
                            filterVO.appendCondition(ConditionOperator.and, new SimpleFilterVO("bank", ICmpConstant.QUERY_EQ, bankVO.getId()));
                            bill.setCondition(filterVO);
                        }
                    } else if (billClaim.get(0).getFundsplitmethod() != null && billClaim.get(0).getFundsplitmethod() == 2) {
                        //资金切分方式=银行账户划转 参照银行档案
                        if (bankVO != null) {
                            FilterVO filterVO = bill.getCondition();
                            if (null == filterVO) {
                                filterVO = new FilterVO();
                            }
                            filterVO.appendCondition(ConditionOperator.and, new SimpleFilterVO("bank", ICmpConstant.QUERY_NEQ, bankVO.getId()));
                            bill.setCondition(filterVO);
                        }
                    }
                }
            } else {
                // 业务模式未结算中心代理 只能参照到内部账户
                if (billClaim.get(0).getBusinessmodel() != null && billClaim.get(0).getBusinessmodel() == BusinessModel.FundCenter_Agent.getCode()) {
                    if (bankVO != null) {
                        FilterVO filterVO = bill.getCondition();
                        if (null == filterVO) {
                            filterVO = new FilterVO();
                        }
                        filterVO.appendCondition(ConditionOperator.and, new SimpleFilterVO("bank", ICmpConstant.QUERY_EQ, bankVO.getId()));
                        bill.setCondition(filterVO);
                    }
                }
            }

        } else if (BillClaim.ENTITY_NAME.equals(bill.getFullname()) && "project_name".equals(bill.getKey()) && "ucfbasedoc.bd_projectNewRef".equals(bill.getrefCode())) {//项目过滤
            if (bill.getExternalData() != null && CtmJSONObject.toJSON(bill.getExternalData()).getJSONArray("orgIds") != null) {
                // 批量认领时，bill.getExternalData()不为null，传递了所有的账户使用组织id(已去重)
                List<String> orgIds = CtmJSONObject.toJSON(bill.getExternalData()).getJSONArray("orgIds").toJavaList(String.class);
                // 采用orgIds组装项目查询参数，获取满足条件的项目id集合
                String[] fields = {"id"};
                List<List<String>> projectIds = new ArrayList<>();
                for (String orgId : orgIds) {
                    List<String> dataList = this.queryProjectByMcOrgId(orgId, fields);
                    projectIds.add(dataList);
                }
                // 取交集
                List<String> queryProjectIds = this.retainList(projectIds, true);
                // 构建项目ids条件
                FilterVO filterVO = bill.getCondition();
                if (null == filterVO) {
                    filterVO = new FilterVO();
                }
                filterVO.appendCondition(ConditionOperator.and, new SimpleFilterVO("id", ICmpConstant.QUERY_IN, queryProjectIds));
                bill.setCondition(filterVO);
            } else {
                List<BillClaim> billClaim = (List<BillClaim>) bill.getData();
                if (CollectionUtils.isNotEmpty(billClaim)) {
                    String actualclaimaccentiry = billClaim.get(0).getActualclaimaccentiry();
                    if (!StringUtils.isEmpty(actualclaimaccentiry)) {
                        Map<String, Object> custMap = bill.getCustMap();
                        bill.setMasterOrgValue(actualclaimaccentiry);
                        if (!custMap.isEmpty()) {
                            for (String key : custMap.keySet()) {
                                if (ICmpConstant.ORGID.equals(key)) {
                                    custMap.put(key, actualclaimaccentiry);
                                }
                            }
                        }
                    }
                }
            }
            //实际组织有值时，使用实际组织过滤
            //原逻辑看不懂，不知道啥用处。先不动。直接复制原逻辑，并替换其中的组织
            List<BillClaim> billClaim = (List<BillClaim>) bill.getData();
            if (CollectionUtils.isNotEmpty(billClaim)) {
                String actualclaimaccentiry = billClaim.get(0).getActualclaimaccentiry();
                if (!StringUtils.isEmpty(actualclaimaccentiry)) {
                    // 批量认领时，bill.getExternalData()不为null，传递了所有的账户使用组织id(已去重)
                    String[] fields = {"id"};
                    List<List<String>> projectIds = new ArrayList<>();
                    List<String> dataList = this.queryProjectByMcOrgId(actualclaimaccentiry, fields);
                    projectIds.add(dataList);
                    // 取交集
                    List<String> queryProjectIds = this.retainList(projectIds, true);
                    // 构建项目ids条件
                    FilterVO filterVO = bill.getCondition();
                    if (null == filterVO) {
                        filterVO = new FilterVO();
                    }
                    filterVO.appendCondition(ConditionOperator.and, new SimpleFilterVO("id", ICmpConstant.QUERY_IN, queryProjectIds));
                    bill.setCondition(filterVO);
                }
            }
        } else if (bill.getrefCode().equals("stwb.stwb_settlebenchbref")) {
            //手工退票，资金结算明细过滤 是否关联银行对账单=是，结算明细状态=结算成功/部分成功
            if (bill.getData() != null) {
                //收付方向为付款
                bill.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("mainid.receipttype", "eq", "2"));
                bill.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("isRelateCheckBill", "eq", "1"));
                bill.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("statementdetailstatus", "in", new String[]{"3", "7"}));
                //批量处理退票=是
                bill.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("batchDetail.reFund", "neq", true));
                //批量处理支付状态=支付成功
                bill.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("batchDetail.payState", "eq", "4"));
            }
        }

        //银行流水指定组织发布，按照结算平台-统收统支关系设置，结算中心-资金结算委托关系 来查询银行账户使用组织对应的指定组织信息
        if ("PublishAssignOrg".equals(bill.getKey()) && IRefCodeConstant.FUNDS_ORGTREE.equals(bill.getrefCode())
                && (IBillNumConstant.BANKRECONCILIATION.equals(billnum) || IBillNumConstant.BANKRECONCILIATIONLIST.equals(billnum))) {
            if (bill.getData() != null) {
                // 统收统支模式or代理结算模式
                Boolean agentFlag = autoConfigService.getEnableBizDelegationMode();
                Boolean inoutFlag = autoConfigService.getUnifiedIEModelWhenClaim();
                List<String> orgList = new ArrayList<>();
                List<BankReconciliation> bankReconciliationList = (List<BankReconciliation>) bill.getData();
                List<String> accentityList = new ArrayList<>();
                if (bankReconciliationList != null) {
                    orgList.add(bankReconciliationList.get(0).getAccentity());
                    accentityList.add(bankReconciliationList.get(0).getAccentity());
                }
                //根据统收统支或者代理结算模式查询值,上级查询下级
                List<String> otherAccentityList = billCountUtil.getControllAccentityList(orgList, inoutFlag, agentFlag);
                accentityList.addAll(otherAccentityList);
                bill.getTreeCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("id", "in", accentityList));
            }
        }

        return new RuleExecuteResult();
    }

    private List<String> getAcc(List<BillClaim> billClaim, BillDataDto bill) throws Exception {
        if (bill.getExternalData() != null && CtmJSONObject.toJSON(bill.getExternalData()).getJSONArray("bodyItems") != null) {
            // 批量认领时，bill.getExternalData()不为null，bodyItems子表存在时，需要处理统收统支模式
            List<BillClaim> bodyItems = CtmJSONObject.toJSON(bill.getExternalData()).getJSONArray("bodyItems").toJavaList(BillClaim.class);
            Map<String, List<BillClaim>> groupMap = bodyItems.stream().collect(Collectors.groupingBy(this::getKey));
            if (groupMap.size() >= 1) {
                // 每一条非重复数据都需要去处理
                List<List<String>> tainList = new ArrayList<>();
                for (Map.Entry<String, List<BillClaim>> entry : groupMap.entrySet()) {
                    BillClaim bodyItem = entry.getValue().get(0);
                    List<String> oneAcc = this.getOne(bodyItem.getAccentity(), bodyItem.getCurrency(), bodyItem.getBankaccount(), bodyItem.getDirection());
                    tainList.add(oneAcc);
                }

                // 然后取交集
                return this.retainList(tainList, false);
            }
            // 分组数据=1时，表示，批量认领的数据都是同一组织，同一币种，同一收支方向，同一账户，不需要批量处理, 进行单条处理即可
        }

        BillClaim bodyItem = billClaim.get(0);
        return this.getOne(bodyItem.getAccentity(), bodyItem.getCurrency(), bodyItem.getBankaccount(), bodyItem.getDirection());
    }

    private String getKey(BillClaim bodyItem) {
        // 返回分组key
        return bodyItem.getCurrency() + "_" + bodyItem.getBankaccount() + "_" + bodyItem.getDirection();
    }

    private List<String> getOne(String acc, String currency, String bankAccount, Short direction) throws Exception {
        // 获取一类数据的结果
        IncomeAndExpenditureReqVO incomeAndExpenditureReqVO = new IncomeAndExpenditureReqVO();
        List<String> accList = new ArrayList<>();
        accList.add(acc);
        incomeAndExpenditureReqVO.setActualAccentity(accList);
        incomeAndExpenditureReqVO.setCurrency(currency);
        incomeAndExpenditureReqVO.setMarginaccount(bankAccount);

        if (direction == Direction.Credit.getValue()) {
            incomeAndExpenditureReqVO.setReauth("1");// 收付类型 1收 2付
        } else if (direction == Direction.Debit.getValue()) {
            incomeAndExpenditureReqVO.setReauth("2");// 收付类型 1收 2付
        }
        List<IncomeAndExpenditureResVO> incomeAndExpenditureResVOS = iOpenApiService.queryControllList(incomeAndExpenditureReqVO);

        List<String> expendIds = new ArrayList<>();
        if (incomeAndExpenditureResVOS != null && incomeAndExpenditureResVOS.size() > 0) {
            for (IncomeAndExpenditureResVO incomeAndExpenditureResVO : incomeAndExpenditureResVOS) {
                expendIds.add(incomeAndExpenditureResVO.getControllId());
            }
        }

        return expendIds;
    }

    private List<String> retainList(List<List<String>> ids, boolean addBasicParam) {
        List<String> resIds = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(ids)) {
            resIds = ids.get(0);
            for (int i = 1; i < ids.size(); i++) {
                resIds.retainAll(ids.get(i));
            }

            // 取交集为空的情况下，默认一个值，查询不到
            if (addBasicParam && CollectionUtils.isEmpty(resIds)) {
                resIds.add("-1");
            }
        }
        return resIds;
    }

    private List<String> queryProjectByMcOrgId(String orgId, String[] fields) throws Exception {
        List<String> mcOrgIds = new ArrayList<>();
        mcOrgIds.add(orgId);

        ProjectQueryParam projectQueryParam = new ProjectQueryParam();
        projectQueryParam.setMcOrgIds(mcOrgIds);
        // 放开查询范围
        projectQueryParam.setPageSize(5000);
        ResultPager resultPager = projectService.queryProjectListByPage(projectQueryParam, fields);
        List<Map<String, String>> recordList = resultPager.getRecordList();
        return recordList == null ? new ArrayList<>() : recordList.stream().map(v -> v.get("id")).collect(Collectors.toList());
    }

    //是否启用内转协议进行资金切分 参数是否启用
    private Boolean getInterTransAgreeFundSplitting() throws Exception {
        QuerySchema querySchema1 = QuerySchema.create().addSelect("*");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("accentity.code").eq("global00"));
        querySchema1.addCondition(conditionGroup);
        List<AutoConfig> configList = MetaDaoHelper.queryObject(AutoConfig.ENTITY_NAME, querySchema1, null);
        if (configList == null || configList.size() == 0) {
            return Boolean.FALSE;
        } else {
            return configList.get(0).getIsUnifiedIEModelWhenClaim() == null ? Boolean.FALSE : configList.get(0).getIsEnableInterTransAgreeFundSplitting();
        }
    }

    private List<String> getAthorAcctitys(List<String> expendIds) throws Exception {
        if (expendIds == null || expendIds.size() == 0) {
            return expendIds;
        }
        Set<String> orgSet = orgDataPermissionService.queryAuthorizedOrgByServiceCode(IServicecodeConstant.BILLCLAIMCARD);
        for (int i = expendIds.size() - 1; i >= 0; i--) {
            if (!orgSet.contains(expendIds.get(i))) {
                expendIds.remove(i);
            }
        }
        if (expendIds.size() == 0) {
            expendIds.add("0");
        }
        return expendIds;
    }
}
