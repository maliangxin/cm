package com.yonyoucloud.fi.cmp.auth;

import com.yonyou.iuap.bd.base.BdRestSingleton;
import com.yonyou.iuap.bd.pub.param.ConditionVO;
import com.yonyou.iuap.bd.pub.param.Operator;
import com.yonyou.iuap.bd.pub.param.Page;
import com.yonyou.iuap.bd.pub.util.Condition;
import com.yonyou.iuap.bd.pub.util.Sorter;
import com.yonyou.iuap.bd.staff.dto.Staff;
import com.yonyou.iuap.bd.staff.service.itf.IStaffService;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyoucloud.uretail.sys.auth.CustomAuthContent;
import com.yonyoucloud.uretail.sys.auth.CustomAuthRuleResponse;
import com.yonyoucloud.uretail.sys.itf.custom.ICustomVersionRuleAuthReferService;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class StaffCenterSelfAll implements ICustomVersionRuleAuthReferService {

    @Override
    public CustomAuthRuleResponse getValues(CustomAuthContent content) throws Exception {
        CustomAuthRuleResponse customAuthRuleResponse = new CustomAuthRuleResponse();
        Set<String> idSet = new HashSet<>();
        try {

            IStaffService staffService = BdRestSingleton.getInst(AppContext.getYTenantId(), "diwork",
                    AppContext.getCurrentUser().getYhtUserId()).getBdRestService().getStaffService();
            Condition condition = new Condition();
            List<ConditionVO> conditionVOList = new ArrayList<>(1);
            ConditionVO conditionVO = new ConditionVO("user_id", null, Operator.ISNOTNULL);
            conditionVOList.add(conditionVO);
            condition.setConditionList(conditionVOList);
            int maxPage = 10000;
            int pageSize = 100;
            for (int i = 0; i < maxPage; i++) {
                Page<Staff> pageList = staffService.pagination(condition, new Sorter(), i + 1, pageSize);
                if (null != pageList && null != pageList.getContent() && pageList.getContent().size() > 0 && null != pageList.getContent().get(0)) {
                    for (Staff staff : pageList.getContent()) {
                        idSet.add(staff.getId());
                    }
                } else {
                    break;
                }
            }
        } catch (Exception e) {
            customAuthRuleResponse.setiSuccess(false);
            customAuthRuleResponse.setFailMessage(e.getMessage());
            customAuthRuleResponse.setVersion(new Date());
            return customAuthRuleResponse;
        }

        customAuthRuleResponse.setiSuccess(true);
        customAuthRuleResponse.setVersion(new Date());
        customAuthRuleResponse.setValues(idSet);

        return customAuthRuleResponse;
    }

}

