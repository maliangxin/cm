package com.yonyoucloud.fi.cmp.bill;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.FilterCommonVO;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.FilterVO;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

import static com.yonyoucloud.fi.cmp.constant.ICmpConstant.CREATOR_ID;

/**
 * <h1>创建人默认当前登录人查询时条件处理  由userId -> id</h1>
 *
 * @author Sun GuoCai
 * @version 1.0
 * @since 2021-12-28 9:50
 */
@RequiredArgsConstructor
@Component
public class CreatorQueryFilterHandlerRule  extends AbstractCommonRule {

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        BillDataDto billDataDto = (BillDataDto) getParam(paramMap);
        FilterVO condition = billDataDto.getCondition();
        if (!ValueUtils.isNotEmptyObj(condition)) {
            return new RuleExecuteResult();
        }
        FilterCommonVO[] filterCommonVos= condition.getCommonVOs();
        if (filterCommonVos == null || filterCommonVos.length == 0) {
            return new RuleExecuteResult();
        }
        for (FilterCommonVO commonVO : filterCommonVos) {
            String itemName = commonVO.getItemName();
            if (CREATOR_ID.equals(itemName)) {
                Object value1 = commonVO.getValue1();
                if (!ValueUtils.isNotEmptyObj(value1)) {
                    return new RuleExecuteResult();
                }
                if (AppContext.getCurrentUser().getYhtUserId().equals(value1)) {
                    Object userId = AppContext.getCurrentUser().getId();
                    commonVO.setValue1(userId);
                } else {
                    commonVO.setValue1(null);
                }
            }
        }

        return new RuleExecuteResult();
    }
}
