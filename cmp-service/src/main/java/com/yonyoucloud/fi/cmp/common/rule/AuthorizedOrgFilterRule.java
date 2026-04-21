package com.yonyoucloud.fi.cmp.common.rule;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.FilterVO;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.SimpleFilterVO;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.cmp.auth.OrgDataPermissionService;
import com.yonyoucloud.fi.cmp.constant.*;
import com.yonyoucloud.fi.cmp.util.StringUtils;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.BizObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 根据银行账户过滤授权使用组织
 *
 * @author maliangn
 * @since 2024-2-27
 */
@Component
public class AuthorizedOrgFilterRule extends AbstractCommonRule {

    //需要走授权使用组织过滤的业务编号
    static List<String> billnumList = new ArrayList<>();
    //需要走授权使用组织过滤的单据字段
    static List<String> fieldList = new ArrayList<>();

    static {
        billnumList.add(IBillNumConstant.CMP_BILLCLAIMCENTER_LIST);
        billnumList.add(IBillNumConstant.CMP_MYBILLCLAIM_LIST);
        billnumList.add(IBillNumConstant.CMP_BILLCLAIM_CARD);
        billnumList.add(IBillNumConstant.CMP_BILLCLAIMCENTER);
        billnumList.add(IBillNumConstant.BANKRECONCILIATION);
//        billnumList.add(IBillNumConstant.CMP_BANKRECONCILIATIONWDLIST);
//        billnumList.add(IBillNumConstant.CMP_BANKJOURNALWDLIST);
//        billnumList.add(IBillNumConstant.CMP_OPENINGOUTSTANDING_CARD);
        billnumList.add(IBillNumConstant.CMP_RETIBALIST);
        billnumList.add(IBillNumConstant.CMP_HISBALIST);
        billnumList.add(IBillNumConstant.CMP_DLLIST);
        billnumList.add(IBillNumConstant.CMP_BANKELECTRONICRECEIPTLIST);
        billnumList.add(IBillNumConstant.BANKRECONCILIATIONLIST);
        fieldList.add(IFieldConstant.ACCENTITY_NAME);
    }

    @Autowired
    private BaseRefRpcService baseRefRpcService;

    @Autowired
    OrgDataPermissionService orgDataPermissionService;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        BillDataDto billDataDto = (BillDataDto) getParam(map);

        boolean islist = "filter".equals(billDataDto.getExternalData());
        //主要针对卡片过滤，不进行列表过滤
        if (islist) {
            return new RuleExecuteResult();
        }


        //成本中心需要根据会计主体进行过滤
        if ("finbd.bd_costcenterref".equals(billDataDto.getrefCode()) && "costcenter_name".equals(billDataDto.getKey())){
            List<BizObject> bills = getBills(billContext, map);
            BizObject bill = null;
            if (bills.size() > 0) {
                bill = bills.get(0);
            }
            boolean isfilter = "filter".equals(billDataDto.getExternalData());
            if(isfilter || bill == null){//如果是过滤区或单据为空，直接跳过
                return new RuleExecuteResult();
            }
            replaceAccentityUseAccentityRaw(bill,billDataDto);
        }

        if (IRefCodeConstant.FUNDS_ORGTREE.equals(billDataDto.getrefCode())) {
            String billnum = billContext.getBillnum();
            // 银行对账单卡片 对方单位类型内部单位、对方单位跳过 fix CZFW-377547
            if (IBillNumConstant.BANKRECONCILIATION.equals(billnum) && billDataDto.getKey().equals(ICmpConstant.OPPOSITEOBJECTNAME)) {
                return new RuleExecuteResult();
            }
            //银行流水按指定组织发布去掉主组织权限过滤;PublishAssignOrg为前端传递的标识
            if((IBillNumConstant.BANKRECONCILIATION.equals(billnum) || IBillNumConstant.BANKRECONCILIATIONLIST.equals(billnum)) && "PublishAssignOrg".equals(billDataDto.getKey())){
                return new RuleExecuteResult();
            }
            List<BizObject> bills = getBills(billContext, map);
            if (bills != null && bills.size() > 0) {
                BizObject bizObject = bills.get(0);
                //只有在银行账户不为空时，才进行授权使用组织过滤
                if (ValueUtils.isNotEmptyObj(getFilterField(bizObject, billnum)) && billnumList.contains(billnum)) {
                    FilterVO filterVO = billDataDto.getTreeCondition();
                    if (null == filterVO) {
                        filterVO = new FilterVO();
                    }
                    String bankaccount = bizObject.get(getFilterField(bizObject, billnum));
                    if(StringUtils.isEmpty(bankaccount) && (IBillNumConstant.CMP_BILLCLAIMCENTER_LIST.equals(billnum) || IBillNumConstant.BANKRECONCILIATIONLIST.equals(billnum)||IBillNumConstant.BANKRECONCILIATION.equals(billnum))){
                        if(billDataDto.getCondition() != null && billDataDto.getCondition().getSimpleVOs() != null
                                && billDataDto.getCondition().getSimpleVOs().length > 0)
                        bankaccount = (String) billDataDto.getCondition().getSimpleVOs()[0].getValue1();
                    }
                    Set<String> orgs = orgDataPermissionService.queryAuthorizedOrgByServiceCodeAndBankAccount(AppContext.getThreadContext("serviceCode"), bankaccount);
                    billDataDto.setCondition(filterVO);
                    billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("id", ICmpConstant.QUERY_IN, orgs));
                }
            }
        }
        return new RuleExecuteResult();
    }

    /**
     * 根据条件判断是否需要进行授权使用组织过滤
     */
    private String getFilterField(BizObject bizObject, String billnum) {
        String filterField = null;
        if (StringUtils.isEmpty(billnum)) {
            return filterField;
        } else {
            if (IBillNumConstant.BANKRECONCILIATION.equals(billnum) || IBillNumConstant.CMP_BANKRECONCILIATIONWDLIST.equals(billnum)
                    || IBillNumConstant.CMP_BANKJOURNALWDLIST.equals(billnum) || IBillNumConstant.CMP_OPENINGOUTSTANDING_CARD.equals(billnum)
                    || IBillNumConstant.CMP_BILLCLAIMCENTER_LIST.equals(billnum) ||IBillNumConstant.BANKRECONCILIATIONREPEATLIST.equals(billnum)
                    || IBillNumConstant.BANKRECONCILIATIONLIST.equals(billnum)
            ) {
                return IFieldConstant.BANKACCOUNT;
            }

        }
        return filterField;
    }


    /**
     * 将资金组织的id替换成会计主体的id
     * @return
     */
    private  Map<String, Object> replaceAccentityUseAccentityRaw(BizObject bill,BillDataDto billDataDto){
        Object accentity = bill.getString("accentityRaw");
        if(billDataDto.getCondition() != null && billDataDto.getCondition().getSimpleVOs() != null
                && billDataDto.getCondition().getSimpleVOs().length > 0){
            //用以防止在组装数据之前就拼接好了condition的数据
            SimpleFilterVO[] simpleFilterVOS = billDataDto.getCondition().getSimpleVOs();
            if (simpleFilterVOS != null && simpleFilterVOS.length > 0) {
                Arrays.stream(simpleFilterVOS).filter(p->null != p).forEach(p->{
                    List<SimpleFilterVO> simpleFilterVOList = p.getConditions();
                    if (null != simpleFilterVOList && simpleFilterVOList.size()>0){
                        simpleFilterVOList.stream().filter(s->null != s && "accentity".equals(s.getField())).forEach(s->s.setValue1(accentity));
                    }
                });
            }
        }
        bill.set("accentity", accentity.toString());
        billDataDto.setMasterOrgValue(accentity.toString());
        Map<String, Object> map = new HashMap<>();
        map.put("orgId", accentity.toString());
        billDataDto.setCustMap(map);
        return map;
    }

}
