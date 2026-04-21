package com.yonyoucloud.fi.cmp.initdata.rule;

import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVOWithRange;
import com.yonyou.ucf.basedoc.model.puborggroup.OrgRangeVO;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.FilterVO;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.SimpleFilterVO;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.constant.IBussinessConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.IRefCodeConstant;
import com.yonyoucloud.fi.cmp.initdata.InitData;
import com.yonyoucloud.fi.cmp.util.JedisLockUtils;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import com.yonyoucloud.fi.cmp.util.basedoc.EnterpriseBankQueryService;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.BizObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class InitDataUseOrgRefRule extends AbstractCommonRule {

    //public final String USE_ORG_REF = "ucf-org-center.bd_financeorgtreeref_na";

    @Autowired
    private EnterpriseBankQueryService enterpriseBankQueryService;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        BillDataDto billDataDto = (BillDataDto) getParam(map);
        RuleExecuteResult result = new RuleExecuteResult();
        List<String> orgids = new ArrayList<>();
        //if(billDataDto.getrefCode().equals(USE_ORG_REF)){
        if(billDataDto.getrefCode().equals(IRefCodeConstant.FUNDS_ORGTREE)){
                List<BizObject> bills = getBills(billContext, map);
            if (bills != null && bills.size()>0) {
                InitData bizObject =  (InitData) bills.get(0);
//                JedisLockUtils.isexistRjLock(bizObject.get(IBussinessConstant.ACCENTITY));
                //有银行账户时 说明为银行账户期初逻辑
                if(ValueUtils.isNotEmptyObj(bizObject.get("bankaccount"))){
                    //查询授权使用组织
                    EnterpriseBankAcctVOWithRange enterpriseBankAcctVoWithRange = enterpriseBankQueryService.queryEnterpriseBankAcctVOWithRangeById(bizObject.getBankaccount());
                    if(enterpriseBankAcctVoWithRange!=null && enterpriseBankAcctVoWithRange.getAccountApplyRange()!=null){
                        List<OrgRangeVO> orgRangeVOList = enterpriseBankAcctVoWithRange.getAccountApplyRange();
                        for(OrgRangeVO orgRangeVO:orgRangeVOList){
                            orgids.add(orgRangeVO.getRangeOrgId());
                        }
                        //获取当前子表的组织
//                        for(InitDatab initDatab:bizObject.InitDatab()){
//                            //如果当前账户的授权使用组织 已经有子表选择过了 则要删除
//                            if(orgids.contains(initDatab.getAccentity())){
//                                orgids.remove(initDatab.getAccentity());
//                            }
//                        }
                        if(orgids.size()>0){
                            FilterVO filterVO = billDataDto.getTreeCondition();
                            if(null == filterVO){
                                filterVO = new FilterVO();
                            }
                            billDataDto.setCondition(filterVO);
                            billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("id", ICmpConstant.QUERY_IN, orgids));
                        }
                    }
                }
            }
        }

        return result;
    }
}
