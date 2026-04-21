package com.yonyoucloud.fi.cmp.checkStock.service;


import com.yonyou.yonbip.ctm.util.lock.CtmLockTool;
import com.yonyou.yonbip.ctm.util.lock.LockStatus;
import com.yonyoucloud.fi.cmp.checkstock.CheckStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;


@Component
@Slf4j
@RequiredArgsConstructor
public class CheckStockMigrationTask {

    private final CheckStockService checkStockService;

    @Scheduled(cron = "0 0 23 * * ?")
    public void migrationStockToStatus() {
        log.error("开始执行历史支票库存数据升级");
        try {
            CtmLockTool.executeInOneServiceLock(CheckStatus.ENTITY_NAME + "migrationStockToStatus", 60 * 10L, TimeUnit.SECONDS, (int lockstatus) -> {
                if (lockstatus == LockStatus.GETLOCK_FAIL) {
                    log.error("migrationStockToStatus 获取锁失败，执行结束");
                    return;
                }
                checkStockService.migrationStockToStatus();
            });
        } catch (Exception e) {
            log.error("migrationStockToStatus error", e);
        }
    }
}
