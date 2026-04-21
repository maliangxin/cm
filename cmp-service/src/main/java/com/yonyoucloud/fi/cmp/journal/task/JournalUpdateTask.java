package com.yonyoucloud.fi.cmp.journal.task;

import com.yonyou.yonbip.ctm.util.lock.CtmLockTool;
import com.yonyou.yonbip.ctm.util.lock.LockStatus;
import com.yonyoucloud.fi.cmp.journal.task.service.JournalUpdateTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * @Description 银行日记账更新任务
 * @Author hanll
 * @Date 2025/5/28-16:05
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class JournalUpdateTask {

    private final JournalUpdateTaskService journalUpdateTaskService;

    /**
     * 启动后延迟10分钟执行第一次，后续每次结束等待10小时再此执行
     * @throws Exception
     */
    // @Scheduled(initialDelay = 10 * 60 * 1000, fixedDelay = 10 * 60 * 60 * 1000)
    public void updateJournal() throws Exception {
        log.info("开始更新银行日记账");
        try {
            CtmLockTool.executeInOneServiceLock("JournalUpdateTask:updateJournal", 10 * 60 * 60L, TimeUnit.SECONDS, (int lockstatus) -> {
                if (lockstatus == LockStatus.GETLOCK_FAIL) {
                    log.error("migrationStockToStatus 获取锁失败，执行结束");
                    return;
                }
                journalUpdateTaskService.updateJournal();
            });
        } catch (Exception e) {
            log.error("migrationStockToStatus error", e);
        }
        log.info("更新银行日记账结束");
    }
}
