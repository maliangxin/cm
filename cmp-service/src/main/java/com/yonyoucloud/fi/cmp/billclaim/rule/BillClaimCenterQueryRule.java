package com.yonyoucloud.fi.cmp.billclaim.rule;

import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVOWithRange;
import com.yonyou.ucf.basedoc.model.rpcparams.EnterpriseParams;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.FilterCommonVO;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.FilterVO;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.SimpleFilterVO;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.ctm.stwb.openapi.IOpenApiService;
import com.yonyoucloud.ctm.stwb.reqvo.IncomeAndExpenditureReqVO;
import com.yonyoucloud.ctm.stwb.respvo.IncomeAndExpenditureResVO;
import com.yonyoucloud.fi.basecom.check.AuthCheckCommonUtil;
import com.yonyoucloud.fi.cmp.api.ctmrpc.CmpMetaDaoHelperRpcService;
import com.yonyoucloud.fi.cmp.autoparam.service.AutoConfigService;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.util.basedoc.EnterpriseBankQueryService;
import com.yonyoucloud.fi.stct.api.openapi.IBusinessDelegationApiService;
import com.yonyoucloud.fi.stct.api.openapi.common.dto.Result;
import com.yonyoucloud.fi.stct.api.openapi.request.QueryDelegationReqVo;
import com.yonyoucloud.fi.stct.api.openapi.vo.businessDelegation.BusinessDelegationVo;
import lombok.extern.slf4j.Slf4j;
import org.imeta.core.base.ConditionOperator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;

@Slf4j
@Component("billClaimCenterQueryRule")
public class BillClaimCenterQueryRule extends AbstractCommonRule {

    @Autowired
    CmpMetaDaoHelperRpcService service;
    @Resource
    IOpenApiService iOpenApiService;
    @Resource
    IBusinessDelegationApiService iBusinessDelegationApiService;
    @Autowired
    AutoConfigService autoConfigService;
    @Autowired
    EnterpriseBankQueryService enterpriseBankQueryService;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        BillDataDto billDataDto = (BillDataDto) getParam(paramMap);
        FilterVO filterVO = billDataDto.getCondition();
        List<String> selectList = new ArrayList<>();
        boolean hasCurrency = false;
        boolean hasAccount = false;
        if (null != filterVO) {
            FilterCommonVO[] commonVOs = filterVO.getCommonVOs();
            if (commonVOs != null) {
                //遍历查询条件
                for (FilterCommonVO vo : commonVOs) {
                    //会计主体
//                    if (ICmpConstant.ACCENTITY.equals(vo.getItemName())) {
//                        selectList = (List<String>) vo.getValue1();
//                        vo.setValue1(null);
//                        vo.setItemName(null);
//                    }else if (ICmpConstant.CURRENCY.equals(vo.getItemName())) {
//                        hasCurrency = true;
//                    }else if (ICmpConstant.BANKACCOUNT.equals(vo.getItemName())) {
//                        hasAccount = true;
//                    }
                }
            }
        }
        /**
         * 1,判定参数“认领时是否启用统收统支模式”，“认领时是否启用结算中心代理模式”是否为“是”
         * 2,根据选中会计主体查询对应上级会计主体
         * 3,将查询到的会计主体追加到选中的会计主体中，进行扩展查询
         */
        List<String> accentityList = new ArrayList<>();
        if(selectList.size() < 1){
            Set<String> orgsSet = AuthCheckCommonUtil.getAuthOrgs(billContext);
            accentityList = new ArrayList<>(orgsSet);
        }else {
            accentityList = selectList;
        }
        Boolean agentFlag = false;
        Boolean inoutFlag = false;
        if(inoutFlag){
            // 查询统收统支关系：下级查上级
            IncomeAndExpenditureReqVO incomeAndExpenditureReqVO = new IncomeAndExpenditureReqVO();
            incomeAndExpenditureReqVO.setControllId(accentityList);
            // 1230需求，切块支持资金付款&代理付款单
//            incomeAndExpenditureReqVO.setReauth("1");// 1收 2付
            List<IncomeAndExpenditureResVO> incomeAndExpenditureResVOS = iOpenApiService.queryRecControllList(incomeAndExpenditureReqVO);
            List<String> expendIds = new ArrayList<>();
            Set<String> currencySet = new HashSet<>();
            Set<String> accountSet = new HashSet<>();
            if(incomeAndExpenditureResVOS != null && incomeAndExpenditureResVOS.size() > 0){
                for(IncomeAndExpenditureResVO incomeAndExpenditureResVO : incomeAndExpenditureResVOS){
                    expendIds.add(incomeAndExpenditureResVO.getActualAccentity());
                    if(incomeAndExpenditureResVO.getCurrency() != null){
                        currencySet.add(incomeAndExpenditureResVO.getCurrency());
                    }
                    if(incomeAndExpenditureResVO.getMarginaccount() != null){
                        accountSet.add(incomeAndExpenditureResVO.getMarginaccount());
                    }
                }
            }
            accentityList.addAll(expendIds);
            // 适配账户共享
            EnterpriseParams enterpriseParams = new EnterpriseParams();
            enterpriseParams.setOrgidList(new ArrayList<>(accentityList));
            List<EnterpriseBankAcctVOWithRange> enterpriseBankAcctVOS =  enterpriseBankQueryService.queryAllWithRange(enterpriseParams);
            List<String> accounts = new ArrayList<>();
            for(EnterpriseBankAcctVOWithRange enterpriseBankAcctVOWithRange : enterpriseBankAcctVOS){
                accounts.add(enterpriseBankAcctVOWithRange.getId());
            }
            accounts.addAll(accountSet);
//            filterVO.appendCondition(ConditionOperator.and, new SimpleFilterVO("bankaccount", ICmpConstant.QUERY_IN, accounts));
//            if (null != filterVO) {
//                FilterCommonVO[] commonVOs = filterVO.getCommonVOs();
//                if (commonVOs != null) {
//                    //遍历查询条件
//                    for (FilterCommonVO vo : commonVOs) {
//                        //会计主体
//                        if (ICmpConstant.ACCENTITY.equals(vo.getItemName())) {
//                            selectList.addAll(expendIds);
//                            vo.setValue1(selectList);
//                        }
//                    }
//                }
//            }
        }
        if(agentFlag) {
            //查询资金业务委托关系  会计主体查询结算中心
            QueryDelegationReqVo queryDelegationReqVo = new QueryDelegationReqVo();
            queryDelegationReqVo.setAccentitys(accentityList);
            Result result = iBusinessDelegationApiService.queryBusinessDelegation(queryDelegationReqVo);
            List<String> expendIds = new ArrayList<>();
            if (result.getData() != null && result.getCode() != 404) {
                List<BusinessDelegationVo> businessDelegationVos = (List<BusinessDelegationVo>) result.getData();
                if (businessDelegationVos != null && businessDelegationVos.size() > 0) {
                    for (BusinessDelegationVo businessDelegationVo : businessDelegationVos) {
                        if(businessDelegationVo.getSettlementCenter() != null){
                            expendIds.add(businessDelegationVo.getSettlementCenter());
                        }
                    }
                }
            }
            accentityList.addAll(expendIds);
            // 适配账户共享
            EnterpriseParams enterpriseParams = new EnterpriseParams();
            enterpriseParams.setOrgidList(new ArrayList<>(accentityList));
            List<EnterpriseBankAcctVOWithRange> enterpriseBankAcctVOS =  enterpriseBankQueryService.queryAllWithRange(enterpriseParams);
            List<String> accounts = new ArrayList<>();
            for(EnterpriseBankAcctVOWithRange enterpriseBankAcctVOWithRange : enterpriseBankAcctVOS){
                accounts.add(enterpriseBankAcctVOWithRange.getId());
            }
//            filterVO.appendCondition(ConditionOperator.and, new SimpleFilterVO("bankaccount", ICmpConstant.QUERY_IN, accounts));
//            if (null != filterVO) {
//                FilterCommonVO[] commonVOs = filterVO.getCommonVOs();
//                if (commonVOs != null) {
//                    //遍历查询条件
//                    for (FilterCommonVO vo : commonVOs) {
//                        //会计主体
//                        if (ICmpConstant.ACCENTITY.equals(vo.getItemName())) {
//                            selectList.addAll(expendIds);
//                            vo.setValue1(selectList);
//                        }
//                    }
//                }
//            }
        }

//        putParam(paramMap, billDataDto);
        return new RuleExecuteResult();
    }
}
