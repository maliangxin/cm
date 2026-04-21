package com.yonyoucloud.fi.cmp.writeOff.service.impl;

import com.yonyou.iuap.yms.id.generator.YmsOidGenerator;
import com.yonyou.iuap.yms.lock.impl.YmsWriteLock;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.yonbip.ctm.accelerator.threadpool.CtmThreadPoolExecutor;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.cmp.common.service.CmCommonService;
import com.yonyoucloud.fi.cmp.exchangegainloss.ExchangeGainLossService;
import com.yonyoucloud.fi.cmp.settlement.service.SettlementServiceImpl;
import com.yonyoucloud.fi.cmp.util.CmpWriteBankaccUtils;
import com.yonyoucloud.fi.cmp.util.JedisLockUtils;
import com.yonyoucloud.fi.cmp.util.TaskUtils;
import com.yonyoucloud.fi.cmp.util.business.CmpCommonUtil;
import com.yonyoucloud.fi.cmp.voucher.CmpVoucherService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.mockito.ArgumentMatchers.anyString;

/**
 * @Desc 逆向生成红冲数据与凭证测试用例
 * @Author zhaoyulong
 * @Email zhaoyulong@yonyou.com
 * @Date 2024/8/2
 * @Version 1.0.0
 **/
@Slf4j
@ExtendWith(MockitoExtension.class)
public class WriteOffServiceImplTest {

    @Mock
    CtmThreadPoolExecutor executorServicePool;
    @Mock
    CmCommonService cmCommonService;
    @Mock
    SettlementServiceImpl settlementService;
    @Mock
    YmsOidGenerator ymsOidGenerator;
    @Mock
    BaseRefRpcService baseRefRpcService;
    @Mock
    CmpWriteBankaccUtils cmpWriteBankaccUtils;
    @Mock
    CmpVoucherService cmpVoucherService;
    @Mock
    ExchangeGainLossService exchangeGainLossService;

    @Mock
    YmsWriteLock ymsWriteLock;

    MockedStatic<AppContext> appContextMockedStatic;
    MockedStatic<JedisLockUtils> jedisLockUtilsMockedStatic;
    MockedStatic<CmpCommonUtil> cmpCommonUtilMockedStatic;

    @InjectMocks
    WriteOffServiceImpl writeOffService;

    @BeforeEach
    public void before() {
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        executorServicePool = new CtmThreadPoolExecutor();
        ReflectionTestUtils.setField(executorServicePool, "threadPoolExecutor", executorService, ExecutorService.class);
        writeOffService = new WriteOffServiceImpl(executorServicePool, cmCommonService, settlementService, ymsOidGenerator, baseRefRpcService, cmpWriteBankaccUtils, cmpVoucherService,exchangeGainLossService);
        appContextMockedStatic = Mockito.mockStatic(AppContext.class);
        appContextMockedStatic.when(AppContext::getTenantId).thenReturn("MaxSoft007");
        jedisLockUtilsMockedStatic = Mockito.mockStatic(JedisLockUtils.class);
        cmpCommonUtilMockedStatic = Mockito.mockStatic(CmpCommonUtil.class);
    }

    @AfterEach
    public void after() {
        appContextMockedStatic.close();
        jedisLockUtilsMockedStatic.close();
        cmpCommonUtilMockedStatic.close();
    }

    @Test
    @DisplayName("逆向生成红冲数据与凭证时走老财务架构场景")
    public void WriteOffTaskTest4OldFiArch() {
        cmpCommonUtilMockedStatic.when(CmpCommonUtil::getNewFiFlag).thenReturn(false);
        Map<String, Object> result = writeOffService.WriteOffTask(1, "1111", "MaxSoft007");
        Assertions.assertNotNull(result);
        Assertions.assertEquals(TaskUtils.TASK_BACK_SUCCESS,result.get("status"));
    }

    @Test
    @DisplayName("逆向生成红冲数据与凭证时走新财务架构单据锁定场景")
    public void WriteOffTaskTest4NewFiArchGetLockFail() {
        cmpCommonUtilMockedStatic.when(CmpCommonUtil::getNewFiFlag).thenReturn(true);
        jedisLockUtilsMockedStatic.when((MockedStatic.Verification) JedisLockUtils.lockWithOutTrace(anyString())).thenReturn(null);
        Map<String, Object> result = writeOffService.WriteOffTask(1, "1111", "MaxSoft007");
        Assertions.assertNotNull(result);
        Assertions.assertEquals(true,result.get("asynchronized"));
    }

    @Test
    @DisplayName("逆向生成红冲数据与凭证时走新财务架构单据正常处理场景")
    public void WriteOffTaskTest4NewFiArch() {
        cmpCommonUtilMockedStatic.when(CmpCommonUtil::getNewFiFlag).thenReturn(true);
        jedisLockUtilsMockedStatic.when((MockedStatic.Verification) JedisLockUtils.lockWithOutTrace(anyString())).thenReturn(ymsWriteLock);
        Map<String, Object> result = writeOffService.WriteOffTask(1, "1111", "MaxSoft007");
        Assertions.assertNotNull(result);
        Assertions.assertEquals(true,result.get("asynchronized"));
    }

}
