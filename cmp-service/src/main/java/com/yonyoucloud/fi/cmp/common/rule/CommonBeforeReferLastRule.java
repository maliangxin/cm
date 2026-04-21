package com.yonyoucloud.fi.cmp.common.rule;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.FilterCommonVO;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.FilterVO;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.SimpleFilterVO;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.IRefCodeConstant;
import lombok.extern.slf4j.Slf4j;
import org.imeta.core.base.ConditionOperator;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * @author: liaojbo
 * @Date: 2025年02月18日 11:36
 * @Description:refer动作的最后一个自定义规则，注册iorder为29.99，最后一个是referDataRule，iorder为30.0
 */
@Slf4j
@Component("commonBeforeReferLastRule")
public class CommonBeforeReferLastRule extends AbstractCommonRule {
    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        BillDataDto billDataDto = (BillDataDto) getParam(paramMap);
        boolean isFilter = "filter".equals(billDataDto.getExternalData());
        if (IRefCodeConstant.UCFBASEDOC_BD_ENTERPRISEBANKACCT.equals(billDataDto.getrefCode())) {
            //企业银行账户支持展示销户数据，需要放在最后，因为ExternalData在之前的规则中可能用来判断是否是筛选区的参照
            billDataDto.setExternalData("{\"showClosedAccount\": true}");
            //不展示未启用的数据
            ArrayList<String> enableValueList = new ArrayList<>();
            if(!isFilter){
                //非筛选区，即卡片页新增时，只能选择启用的账户
                enableValueList.add("1");
                if(billDataDto.getCondition() == null){
                    FilterVO conditon = new FilterVO();
                    conditon.appendCondition(ConditionOperator.and, new SimpleFilterVO("enable", "in", enableValueList));
                    billDataDto.setCondition(conditon);
                }else{
                    billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("enable", "in", enableValueList));
                }
            }
        } else if (IRefCodeConstant.UCFBASEDOC_BD_BANKCARD.equals(billDataDto.getrefCode())) {
            //银行类别换远程参照后，需要加这个，支持展示结算中心
            Map<String, Object> externalData = new HashMap<>();
            externalData.put("Special", "1");
            billDataDto.setExternalData(externalData);
        }
        FilterVO filterVO = billDataDto.getCondition();
        dealEmptyIdSet(filterVO);
        return new RuleExecuteResult();
    }

    /**
     * mdd空集合查出全量数据bug处理，https://uap-wiki.yyrd.com/pages/viewpage.action?pageId=261997155
     * @param filterVO
     */
    private static void dealEmptyIdSet(FilterVO filterVO) {
        if (filterVO != null) {
            SimpleFilterVO[] simpleVOs = filterVO.getSimpleVOs();
            if (simpleVOs != null && simpleVOs.length > 0) {
                for (SimpleFilterVO simpleVO : simpleVOs) {
                    if (isEmptyIdInCondition(simpleVO) && ConditionOperator.and.equals(simpleVO.getLogicOp())) {
                        filterVO.appendCondition(ConditionOperator.and, new SimpleFilterVO("1", "eq", "2"));
                    } else if (simpleVO.getField() == null && ConditionOperator.and.equals(simpleVO.getLogicOp())) {
                        if (simpleVO.getConditions() != null && simpleVO.getConditions().size() > 0) {
                            List<SimpleFilterVO> innerSimpleVOs = simpleVO.getConditions();
                            for (SimpleFilterVO innerSimpleVO : innerSimpleVOs) {
                                if (isEmptyIdInCondition(innerSimpleVO)) {
                                    filterVO.appendCondition(ConditionOperator.and, new SimpleFilterVO("1", "eq", "2"));
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private static boolean isEmptyIdInCondition(SimpleFilterVO simpleVO) {
        if (simpleVO == null) {
            return false;
        }
        if (!"id".equals(simpleVO.getField())) {
            return false;
        }
        if (!ICmpConstant.QUERY_IN.equals(simpleVO.getOp())) {
            return false;
        }
        if (!(simpleVO.getValue1() instanceof Collection)) {
            return false;
        }
        return ((Collection<?>) simpleVO.getValue1()).isEmpty();
    }
}
