package com.yonyoucloud.fi.cmp.controller.test;

import com.yonyoucloud.fi.cmp.journal.JournalVo;
import com.yonyoucloud.fi.cmp.sdk.journal.service.CmpJournalService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Description
 * @Author hanll
 * @Date 2025/5/19-16:33
 */
@RestController
@RequestMapping("/test/journal/sdk")
public class TestJournalSdkController {

    private final CmpJournalService cmpJournalService;

    public TestJournalSdkController(CmpJournalService cmpJournalService) {
        this.cmpJournalService = cmpJournalService;
    }



    /**
     * 测试登日记账
     * @param journalVo
     * @return
     * @throws Exception
     */
    @PostMapping("/testJournalRegister")
    public void testJournalRegister(@RequestBody JournalVo journalVo) throws Exception {
        cmpJournalService.insert(journalVo);
    }

    /**
     * 测试更新日记账
     * @param journalVo
     * @return
     * @throws Exception
     */
    @PostMapping("/testJournalUpdate")
    public void testJournalUpdate(@RequestBody JournalVo journalVo) throws Exception {
        cmpJournalService.update(journalVo);
    }


    /**
     * 测试删除日记账
     * @param journalVo
     * @return
     * @throws Exception
     */
    @PostMapping("/journalDelete")
    public void journalDelete(@RequestBody JournalVo journalVo) throws Exception {
        cmpJournalService.delete(journalVo);
    }
}
