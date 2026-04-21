package com.yonyoucloud.fi.cmp.electronicstatementconfirm.rule;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.FilterCommonVO;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.FilterVO;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.SimpleFilterVO;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.auth.OrgDataPermissionService;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import com.yonyoucloud.fi.cmp.util.basedoc.EnterpriseBankQueryService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.core.base.ConditionOperator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 不用了，现在和回单用一个规则
 * @Author qihaoc
 * @Date 2024/9/25
 * @deprecated
 */
@Slf4j
@Component("electronicStatementRefRule")
  @Deprecated
public class ElectronicStatementRefRule extends AbstractCommonRule {

    @Resource
    private OrgDataPermissionService orgDataPermissionService;

    @Autowired
    private EnterpriseBankQueryService enterpriseBankQueryService;



    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        log.error("===============================================进入银行电子对账单参照规则===============================================");
        String billnum = billContext.getBillnum();
        BillDataDto billDataDto = (BillDataDto) getParam(paramMap);
        if ("ucfbasedoc.bd_enterprisebankacct".equals(billDataDto.getrefCode()) && "cmp_electronicstatementconfirmlist".equals(billnum)) {
            // 获取授权使用组织
            FilterVO filterVO = billDataDto.getCondition();
            if (null != filterVO) {
                SimpleFilterVO[] commonVOs = filterVO.getSimpleVOs();
                List<SimpleFilterVO> newCommonVOs =  new ArrayList<SimpleFilterVO>();
                Boolean hasOrgid = false;
                List<String> bankList = new ArrayList<>();
                List<String> orgList = new ArrayList<>();
                List<String> currencyList = new ArrayList<>();
                for (SimpleFilterVO vo : commonVOs) {
                    if ("currency".equals(vo.getField()) && ((List) vo.getValue1()).size() > 0) {
                        currencyList = (List) vo.getValue1();
                        continue;
                    }
                    if ("bank".equals(vo.getField()) && ((List)vo.getValue1()).size() > 0){
                        bankList = (List) vo.getValue1();
                        continue;
                    }
                    if("orgid".equals(vo.getField()) && ((List)vo.getValue1()).size() > 0){
                        hasOrgid = true;
                        orgList = (List)vo.getValue1();
                        continue;
                    }
                    newCommonVOs.add(vo);
                }
                filterVO.setSimpleVOs(newCommonVOs.toArray(new SimpleFilterVO[newCommonVOs.size()]));
                if(hasOrgid){
                    List<String> accountByOrg = enterpriseBankQueryService.getAccounts(orgList);
                    billDataDto.getCondition().appendCondition(ConditionOperator.and,new SimpleFilterVO("id", ICmpConstant.QUERY_IN, accountByOrg));
                } else {
                    // 获取授权使用组织
                    Set<String> orgs = orgDataPermissionService.queryAuthorizedOrgByServiceCode(IServicecodeConstant.CMP_ELECTRONICSTATEMENTCONFIRM);
                    List<String> accounts = enterpriseBankQueryService.getAccounts(orgs);
                    billDataDto.getCondition().appendCondition(ConditionOperator.and,new SimpleFilterVO("id", ICmpConstant.QUERY_IN, accounts));
                }
                //该参照是主子表展示，如果不拼币种条件会展示出该账户下所有的币种数据
                if (CollectionUtils.isNotEmpty(currencyList)) {
                    billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("currencyList.currency", ICmpConstant.QUERY_IN, currencyList));
                }
                //
                if (CollectionUtils.isNotEmpty(bankList)) {
                    billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("bankNumber.bank.id", ICmpConstant.QUERY_IN, bankList));
                }

            }
        }

        return new RuleExecuteResult();
    }

}
