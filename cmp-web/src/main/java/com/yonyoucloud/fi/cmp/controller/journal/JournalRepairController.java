package com.yonyoucloud.fi.cmp.controller.journal;

import com.yonyou.ucf.mdd.ext.base.BaseController;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.journal.service.JournalRepairService;
import com.yonyoucloud.fi.cmp.journal.task.service.JournalUpdateTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletResponse;

/**
 * 修复日记账统一方法
 * 包括删除(修改余额)、新增(修改余额)主要来源单据为结算
 */
@Controller
@RequestMapping("/journalRepair")
@Slf4j
@RequiredArgsConstructor
public class JournalRepairController extends BaseController {

    @Autowired
    JournalRepairService journalRepairService;

    @Autowired
    private JournalUpdateTaskService journalUpdateTaskService;

    /**
     * 通过租户id，结算单号 删除日记账 并修改余额
     *
     * @param params
     * @param response
     */
    @PostMapping("/deleteJournalByStwbCode")
    public void deleteJournalForStwb(@RequestBody CtmJSONObject params, HttpServletResponse response) throws Exception {
        journalRepairService.deleteJournalByStwbCode(params);
        renderJson(response, ResultMessage.success("deleteJournalByStwbCode success!"));
    }

    /**
     * 通过租户id，结算单号 组装新增日记账 并修改余额
     *
     * @param params
     * @param response
     */
    @PostMapping("/addJournalForStwb")
    public void addJournalForStwb(@RequestBody CtmJSONObject params, HttpServletResponse response) throws Exception {
        journalRepairService.addJournalForStwbCode(params);
        renderJson(response, ResultMessage.success("addJournalForStwb success!"));
    }

    /**
     * 通过日记账id 直接删除日记账 并修改余额
     *
     * @param params
     * @param response
     */
    @PostMapping("/deleteJournalById")
    public void deleteJournalById(@RequestBody CtmJSONObject params, HttpServletResponse response) throws Exception {
        journalRepairService.deleteJournalById(params);
        renderJson(response, ResultMessage.success("deleteJournalById success!"));
    }

    /**
     * 通过账户期初id 修改余额
     *
     * @param params
     * @param response
     */
    @PostMapping("/dmodifyInitBalanceById")
    public void dmodifyInitBalanceById(@RequestBody CtmJSONObject params, HttpServletResponse response) throws Exception {
        journalRepairService.modifyInitBalanceById(params);
        renderJson(response, ResultMessage.success("dmodifyInitBalanceById success!"));
    }

    /**
     * 根据输入sql进行更新操作（必输ytenant_id 限制10条数据）前端屏蔽 此方法不会调用
     *
     * @param params
     * @param response
     * @throws Exception
     */
    @PostMapping("/updateCmpDataByQueryCondition")
    public void updateCmpDataByQueryCondition(@RequestBody CtmJSONObject params, HttpServletResponse response) throws Exception {
        journalRepairService.updateCmpDataByQueryCondition(params);
        renderJson(response, ResultMessage.success("updateCmpDataByQueryCondition success!"));
    }

    /**
     * 根据输入的租户id更新来源业务系统和业务单据类型
     *
     * @param params
     * @param response
     * @throws Exception
     */
    @PostMapping("/updateJournalTopInfo")
    public void updateJournalTopInfo(@RequestBody CtmJSONObject params, HttpServletResponse response) throws Exception {
        String ytenantId = params.getString("ytenantId");
        if ("666666".equals(ytenantId)) {
            // 刷全租户
            journalUpdateTaskService.updateJournal();
        } else {
            // 刷单租户
            journalUpdateTaskService.updateJournal(ytenantId);
        }
        renderJson(response, ResultMessage.success("updateJournalTopInfo success!"));
    }

}
