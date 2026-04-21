package com.yonyoucloud.fi.cmp.bankreconciliationrepeat.service;

import com.yonyou.yonbip.ctm.util.lock.CtmLockTool;
import com.yonyou.yonbip.ctm.util.lock.LockStatus;
import com.yonyoucloud.fi.cmp.checkstock.CheckStatus;
import com.yonyoucloud.fi.cmp.util.CtmDealDetailCheckMayRepeatUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 银行流水处理 更新 疑重标识 字段
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class BankReconciliationRepeatTask {

    private final BankReconciliationRepeatService bankReconciliationRepeatService;

    @Scheduled(cron = "0 0 23 * * ?")
    @Scheduled(cron = "0 0 23 * * ?")
    public void bankReconciliationRepeatCheckStatus() {
        if (CtmDealDetailCheckMayRepeatUtils.isRepeatCheck) {
            log.info("开始执行银行流水处理 数据升级");
            try {
                CtmLockTool.executeInOneServiceLock(CheckStatus.ENTITY_NAME + "bankReconciliationRepeatCheckStatus", 60 * 10L, TimeUnit.SECONDS, (int lockstatus) -> {
                    if (lockstatus == LockStatus.GETLOCK_FAIL) {
                        log.error("migrationStockToStatus 获取锁失败，执行结束");
                        return;
                    }
                    bankReconciliationRepeatService.bankReconciliationRepeatCheckStatus();
                });
            } catch (Exception e) {
                log.error("bankReconciliationRepeatCheckStatus error", e);
            }
        }

    }
}
