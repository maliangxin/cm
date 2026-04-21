package com.yonyoucloud.fi.cmp.balanceadjustresult;

import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVO;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.FilterVO;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.dao.meta.UiMetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.basecom.utils.FIDubboUtils;
import com.yonyoucloud.fi.cmp.constant.IBussinessConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.IRefCodeConstant;
import com.yonyoucloud.fi.cmp.util.StringUtils;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.orm.base.BizObject;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 余额调节表查询的规则 会计主体和银行账户实现联动
 */
public class BalanceAdjustResultQueryRule extends AbstractCommonRule {
    public  static String  CMP_BANKRECONCILIATIONSETREF = "cm_bankreconciliationsetref";
    public static  String BANKRECONCILIATIONSETTING_B = "bankReconciliationSetting_b";
    @Autowired
    BaseRefRpcService baseRefRpcService;
    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        BillDataDto bill = (BillDataDto) getParam(paramMap);
        FilterVO filterVO = bill.getCondition();
        if(null == filterVO){
             filterVO = new FilterVO();
        }
        if (bill.getrefCode().equals(CMP_BANKRECONCILIATIONSETREF)){
			if (bill.getData()!=null) {
                List<Map<String,Object>> list = (List<Map<String,Object>>) bill.getData();
                if (CollectionUtils.isNotEmpty(list)) {
                    String accentity = (String) list.get(0).get(IBussinessConstant.ACCENTITY);
                    if (FIDubboUtils.isSingleOrg()) {
                        BizObject singleOrg = FIDubboUtils.getSingleOrg();
                        if (singleOrg != null) {
                            accentity = singleOrg.get("id");
                        }
                    }
                    if (StringUtils.isEmpty(accentity)) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102644"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805A5", "请先选择会计主体！") /* "请先选择会计主体！" */);
                    }
                    UiMetaDaoHelper.appendCondition(filterVO, IBussinessConstant.ACCENTITY, ICmpConstant.QUERY_EQ, accentity);
                }
            }
		}else if (IRefCodeConstant.UCFBASEDOC_BD_ENTERPRISEBANKACCT.equals(bill.getrefCode())){
            if (bill.getData()!=null) {
                List<Map<String,Object>> list = (List<Map<String,Object>>) bill.getData();
                if (CollectionUtils.isNotEmpty(list)) {
                    String accentity = (String) list.get(0).get(IBussinessConstant.ACCENTITY);
                    if (FIDubboUtils.isSingleOrg()) {
                        BizObject singleOrg = FIDubboUtils.getSingleOrg();
                        if (singleOrg != null) {
                            accentity = singleOrg.get("id");
                        }
                    }

                    if (StringUtils.isEmpty(accentity)) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102645"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1BBBAD5C04200001", "请先选择对账组织！") /* "请先选择对账组织！" */);
                    }
                    List<String> accentityList = getAccentityList(accentity);
                    //根据会计主体查询银行账号
                    List<EnterpriseBankAcctVO> enterpriseBankAcctVOs = baseRefRpcService.queryEnterpriseBankAcctByOrgids(accentityList);

                    List<Object> bankaccounts = new ArrayList<Object>();
                    for (EnterpriseBankAcctVO enterpriseBankAcctVO : enterpriseBankAcctVOs) {
                        bankaccounts.add(enterpriseBankAcctVO.getId());
                    }
                    UiMetaDaoHelper.appendCondition(filterVO, "id", ICmpConstant.QUERY_IN, bankaccounts);
                }
            }
        }
        bill.setCondition(filterVO);
        putParam(paramMap, bill);
        return new RuleExecuteResult();
    }

    private List<String> getAccentityList(String accentity) {
        List<String> accentityList = new ArrayList<>();
        if (accentity.startsWith("[") && accentity.endsWith("]")) {
            CtmJSONArray ja = CtmJSONArray.parseArray(accentity);
            if (!ja.isEmpty()) {
                for(int i = 0; i < ja.size(); ++i) {
                    accentityList.add(ja.getString(i));
                }
            }
        } else {
            accentityList.add(accentity);
        }
        return accentityList;
    }

}
