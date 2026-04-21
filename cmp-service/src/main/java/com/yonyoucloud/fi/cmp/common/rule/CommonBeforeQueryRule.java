package com.yonyoucloud.fi.cmp.common.rule;

import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVOWithRange;
import com.yonyou.ucf.basedoc.model.rpcparams.EnterpriseParams;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.FilterCommonVO;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.FilterVO;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.SimpleFilterVO;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.yonbip.ctm.ctmpub.bam.api.openapi.IAccountInfoQueryApiService;
import com.yonyou.yonbip.ctm.ctmpub.bam.model.request.BamAccountInfoQueryReq;
import com.yonyou.yonbip.ctm.ctmpub.bam.model.response.BamResult;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.yonbip.ctm.remote.RemoteDubbo;
import com.yonyoucloud.fi.cmp.auth.OrgDataPermissionService;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.IDomainConstant;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.consts.BankDealDetailConst;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import com.yonyoucloud.fi.cmp.util.basedoc.EnterpriseBankQueryService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;

/**
 * @Author ningff
 * @Date 2024/4/1 18:43
 * @DESCRIPTION cmp_dllist
 */
@Slf4j
@Component("commonBeforeQueryRule")
public class CommonBeforeQueryRule extends AbstractCommonRule {

    @Resource
    OrgDataPermissionService orgDataPermissionService;

    @Autowired
    EnterpriseBankQueryService enterpriseBankQueryService;


    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        BillDataDto billDataDto = (BillDataDto) getParam(map);
        FilterVO filterVO = ValueUtils.isNotEmptyObj(billDataDto.getCondition()) ? billDataDto.getCondition() : new FilterVO();
        FilterCommonVO[] commonVOs = filterVO.getCommonVOs();
        if(IBillNumConstant.BANKRECONCILIATIONREPEATLIST.equals(billDataDto.getBillnum())){
            //疑重单据增加过滤条件
            QueryConditionGroup repeatGroupFather = new QueryConditionGroup(ConditionOperator.and);
            QueryConditionGroup repeatGroup = new QueryConditionGroup(ConditionOperator.or);
            repeatGroup.addCondition(QueryCondition.name("isrepeat").is_not_null());
            repeatGroup.addCondition(QueryCondition.name("isrepeat").is_null());
            // appendCondition vs addCondition
            repeatGroupFather.addCondition(repeatGroup);
            if(filterVO.getQueryConditionGroup() != null){
                filterVO.getQueryConditionGroup().appendCondition(repeatGroupFather);
            }else{
                filterVO.setQueryConditionGroup(repeatGroupFather);
            }

        }
        if(IBillNumConstant.BANKRECONCILIATIONLIST.equals(billDataDto.getBillnum()) && "export".equalsIgnoreCase(billDataDto.getRequestAction())){
            //导出逻辑
            if(billDataDto.getExternalData() != null) {
                Map<String, Object> extendData = (Map<String, Object>) billDataDto.getExternalData();
                if (extendData.containsKey("use4Export") && "true".equalsIgnoreCase(extendData.get("use4Export").toString())) {
                    QueryConditionGroup repeatGroupFather = new QueryConditionGroup(ConditionOperator.and);
                    QueryConditionGroup repeatGroup = new QueryConditionGroup(ConditionOperator.or);
                    repeatGroup.addCondition(QueryCondition.name("isrepeat").is_not_null());
                    repeatGroup.addCondition(QueryCondition.name("isrepeat").is_null());
                    repeatGroupFather.addCondition(repeatGroup);
                    if(filterVO.getQueryConditionGroup() == null) {
                        filterVO.setQueryConditionGroup(repeatGroupFather);
                    }else{
                        filterVO.getQueryConditionGroup().addCondition(repeatGroupFather);
                    }
                }
            }
        }
        billDataDto.setCondition(filterVO);
        boolean flag = Objects.nonNull(commonVOs) &&
                (IBillNumConstant.BANKRECONCILIATIONLIST.equals(billDataDto.getBillnum()) || IBillNumConstant.BANKRECONCILIATIONREPEATLIST.equals(billDataDto.getBillnum()));
        if (flag) {
            Boolean isOrg = false;
            Object cashDirectLink = "";
            Object enterCountry = "";
            List<String> authorizedOrgList = null;//授权使用组织
            List<String> orgList = null;//所属组织
            List<String> filterOrgList = null;//过滤的组织
            List<String> bankValueList = null;
            List<String> classValueList = null;
            Boolean filterFlag = false;
            for (FilterCommonVO vo : commonVOs) {
                // 境内外标识
                if (ICmpConstant.ENTERCOUNTRY.equals(vo.getItemName())) {
                    filterFlag = true;
                    enterCountry = vo.getValue1();
                    vo.setValue1(null);
                }
                // 是否直联标识
                if (ICmpConstant.CASHDIRECTLINK.equals(vo.getItemName())) {
                    filterFlag = true;
                    cashDirectLink = vo.getValue1();
                    vo.setValue1(null);
                }
            }
            if (!filterFlag) {
                return new RuleExecuteResult();
            }
            for (FilterCommonVO vo : commonVOs) {
                // 授权使用组织
                if (ICmpConstant.ACCENTITY.equals(vo.getItemName())) {
                    authorizedOrgList = getValueList(vo);
                }
                // 账户所属组织
                if (ICmpConstant.ORG_ID.equals(vo.getItemName())) {
                    orgList = getValueList(vo);
                }
                // 银行账户
                if (ICmpConstant.BANKACCOUNT.equals(vo.getItemName())) {
                    bankValueList = getValueList(vo);
                }
            }

            if (CollectionUtils.isEmpty(authorizedOrgList) && CollectionUtils.isEmpty(orgList)) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101172"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D5C3F0A04A00009","账户授权使用组织和账户所属组织不能同时为空！"));
            } else if (CollectionUtils.isNotEmpty(authorizedOrgList) && CollectionUtils.isEmpty(orgList)) {
                filterOrgList = authorizedOrgList;
            } else if (CollectionUtils.isEmpty(authorizedOrgList) && CollectionUtils.isNotEmpty(orgList)) {
                filterOrgList = orgList;
            } else if (CollectionUtils.isNotEmpty(authorizedOrgList) && CollectionUtils.isNotEmpty(orgList)) {
                Set<String> intersection = new HashSet<>(authorizedOrgList);
                intersection.retainAll(orgList);
                filterOrgList.addAll(intersection);
            }
            // 如果前端过滤条件没有银行账户，则根据组织获取所有有权限的银行账户
            if (CollectionUtils.isEmpty(bankValueList)) {
                bankValueList = getAllAuthorizedBankAccount(filterOrgList);
            }
            List<String> bankAcctInfos = getBankAcctInfos(filterOrgList, enterCountry, cashDirectLink);
            if (CollectionUtils.isNotEmpty(bankAcctInfos)) {
                if (CollectionUtils.isNotEmpty(bankValueList)) {
                    List<String> intersection = (List<String>) CollectionUtils.intersection(bankValueList, bankAcctInfos);
                    if (CollectionUtils.isNotEmpty(intersection)) {
                        filterVO.appendCondition(ConditionOperator.and, new SimpleFilterVO("bankaccount", ICmpConstant.QUERY_IN, intersection));
                    } else {
                        filterVO.appendCondition(ConditionOperator.and, new SimpleFilterVO("bankaccount", ICmpConstant.QUERY_EQ, "have no account"));
                    }
                } else {
                    filterVO.appendCondition(ConditionOperator.and, new SimpleFilterVO("bankaccount", ICmpConstant.QUERY_IN, bankAcctInfos));
                }
            } else {
                filterVO.appendCondition(ConditionOperator.and, new SimpleFilterVO("bankaccount", ICmpConstant.QUERY_EQ, "have no account"));
            }
        } else {
            return new RuleExecuteResult();
        }

        billDataDto.setCondition(filterVO);
        putParam(map, billDataDto);
        return new RuleExecuteResult();
    }

    private List<String> getValueList(FilterCommonVO vo) {
        List<String> valueList = new ArrayList<>();
        if (vo.getValue1() instanceof ArrayList) {
            valueList = (List<String>) vo.getValue1();
        } else {
            String orgValue = vo.getValue1().toString();
            valueList.add(orgValue);
        }
        return valueList;
    }

    /**
     * 获取所有有权限的银行账户
     *
     * @param orgList
     * @return
     * @throws Exception
     */
    private List<String> getAllAuthorizedBankAccount(List<String> orgList) throws Exception {
        List<String> accounts = new ArrayList<>();
        EnterpriseParams enterpriseParams = new EnterpriseParams();
        if (CollectionUtils.isEmpty(orgList)) {
            Set<String> orgSet = orgDataPermissionService.queryAuthorizedOrgByServiceCode(IServicecodeConstant.CMPBANKRECONCILIATION);
            orgList = new ArrayList<>(orgSet);
            enterpriseParams.setOrgidList(orgList);
        } else {
            enterpriseParams.setOrgidList(orgList);
        }
        List<EnterpriseBankAcctVOWithRange> enterpriseBankAcctVOS = enterpriseBankQueryService.queryAllWithRange(enterpriseParams);
        for (EnterpriseBankAcctVOWithRange enterpriseBankAcctVOWithRange : enterpriseBankAcctVOS) {
            accounts.add(enterpriseBankAcctVOWithRange.getId());
        }
        return accounts;
    }

    /**
     * 查询账户信息数据
     *
     * @param orgValueList   会计主体
     * @param enterCountry   是否境外
     * @param cashDirectLink 是否直联
     * @return
     * @throws Exception
     */
    private List<String> getBankAcctInfos(List<String> orgValueList, Object enterCountry, Object cashDirectLink) throws Exception {
        BamAccountInfoQueryReq addAccountInfoReq = new BamAccountInfoQueryReq();
        addAccountInfoReq.setAccentityList(orgValueList);
        if (enterCountry != null && !enterCountry.equals("")) {
            addAccountInfoReq.setIsOverseasAcct(enterCountry.toString());
        }
        if (cashDirectLink != null && !cashDirectLink.equals("")) {
            addAccountInfoReq.setCashDirectLink(cashDirectLink.toString());
        }
        BamResult<String> stringBamResult = RemoteDubbo.get(IAccountInfoQueryApiService.class, IDomainConstant.MDD_DOMAIN_BAM).queryAccountInfos(addAccountInfoReq);
        if (stringBamResult.getCode() == 200) {
            return stringBamResult.getDataList();
        }
        return null;
    }

}
