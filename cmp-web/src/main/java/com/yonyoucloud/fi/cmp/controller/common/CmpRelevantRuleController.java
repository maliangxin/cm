/*
package com.yonyoucloud.fi.cmp.controller.common;

import com.yonyou.ucf.mdd.ext.character.model.BillRelevantRuleDefine;
import com.yonyou.ucf.mdd.ext.character.relevant.RelevantRuleService;
import com.yonyou.ucf.mdd.ext.controller.RelevantRuleController;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotNull;

*/
/**
 * @Description: 重写单据加载相关性规则控制器
 * 需要过滤掉银行对账单中的辨识、冻结和生单规则
 * @Author: gengrong
 * @createTime: 2022/10/28
 * @version: 1.0
 *//*

//海康使用时打开  @Controller 和 @RequestMapping
//@Controller
//@RequestMapping({"/relevant/rule"})
public class CmpRelevantRuleController extends RelevantRuleController {

    @Autowired
    private RelevantRuleService relevantRuleService;

    @RequestMapping({"/queryByBill"})
    public void queryByBill(@NotNull String billnum, boolean excludeCG, HttpServletRequest request, HttpServletResponse response) {
        try {
            BillRelevantRuleDefine billRelevantRuleDefine = this.relevantRuleService.getBillRelevantRules(billnum);
            this.renderJson(response, ResultMessage.data(billRelevantRuleDefine));
        } catch (Throwable var5) {
            this.renderJson(response, ResultMessage.error(var5.toString()));
        }

    }
}
*/
