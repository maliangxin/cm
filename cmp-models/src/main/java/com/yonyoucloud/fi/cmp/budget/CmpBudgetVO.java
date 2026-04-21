package com.yonyoucloud.fi.cmp.budget;

import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import lombok.Data;

import java.util.List;

/**
 * <h1>CmpBudgetVO</h1>
 *
 * @author Sun GuoCai
 * @version 1.0
 * @since 2024-03-20 12:17
 */
@Data
public class CmpBudgetVO {
    private String billno;
    private List<String> ids;
    private String changeBillno;//变更单据
    private String bizObj;
}
