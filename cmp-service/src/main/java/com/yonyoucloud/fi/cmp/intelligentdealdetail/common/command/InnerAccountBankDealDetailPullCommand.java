package com.yonyoucloud.fi.cmp.intelligentdealdetail.common.command;

import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVO;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.bankdealdetail.BankDealDetailService;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.model.PullCommandInfo;
import com.yonyoucloud.fi.cmp.util.threadpool.ThreadPoolBuilder;
import com.yonyoucloud.fi.cmp.util.threadpool.ThreadPoolUtil;
import cn.hutool.core.thread.BlockPolicy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

public class InnerAccountBankDealDetailPullCommand extends BankAccountDataPullCommand {

    BankDealDetailService bankDealDetailService;
    @Override
    public void execute(PullCommandInfo transInfo) throws Exception {
        List<CtmJSONObject> queryInnerParamList = new ArrayList<>();
        List<EnterpriseBankAcctVO> innerAccounts = transInfo.getBankAcctVOList();
        //组装拉取内部账户的参数
        for (EnterpriseBankAcctVO enterpriseBankAcctVO : innerAccounts) {
            CtmJSONObject queryParam = new CtmJSONObject();
            queryParam.put("accEntity", enterpriseBankAcctVO.getOrgid());
            queryParam.put("accountId", enterpriseBankAcctVO.getId());
            queryParam.put("startDate", transInfo.getStartDate());
            queryParam.put("endDate", transInfo.getEndDate());
            queryInnerParamList.add(queryParam);
        }
        executorService = buildThreadPool(Integer.valueOf(transInfo.getCorepoolsize()));
        //执行查询内部账户
        ThreadPoolUtil.executeByBatch(executorService, queryInnerParamList, 10, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400591", "查内部交易明细调度任务") /* "查内部交易明细调度任务" */, (int fromIndex, int toIndex) -> {
            for (int t = fromIndex; t < toIndex; t++) {
                CtmJSONObject innerObj = queryInnerParamList.get(t);
                CtmJSONObject paramNow = new CtmJSONObject(innerObj);
                bankDealDetailService.queryInnerAccountDetails(paramNow);
                paramNow = null;
            }
            return null;
        });

    }

    static ExecutorService executorService;
    static Map<String, Integer> coreSizeMap = new HashMap<>();

    /**
     *
     * @param corePoolSize
     * @return
     */
    private ExecutorService buildThreadPool(Integer corePoolSize) {
        int coreSize = coreSizeMap.get(InvocationInfoProxy.getTenantid());
        if (executorService != null && coreSize == corePoolSize) {
            return executorService;
        } else {
            // 线程参数 “8,32,1000,cmp-balance-compare-async-” 核心线程数：corePoolSize,最大线程数：maxPoolSize,队列数：queueLength, 线程前缀：threadNamePrefix
            String threadParam = AppContext.getEnvConfig("cmp.bankdetailTask.thread.param","8,128,1000,cmp-bankdetailTask-async-");
            String[] threadParamArray = threadParam.split(",");
            if (corePoolSize == null || corePoolSize == 0) {
                corePoolSize = Integer.parseInt(threadParamArray[0]);;
            }
            int maxPoolSize = Integer.parseInt(threadParamArray[1]);
            int queueSize = Integer.parseInt(threadParamArray[2]);
            String threadNamePrefix = threadParamArray[3];
            executorService = ThreadPoolBuilder.ioThreadPoolBuilder()
                    .setDaemon(false)
                    .setRejectHandler(new BlockPolicy())
                    .builder(corePoolSize, maxPoolSize, queueSize,threadNamePrefix);
            coreSizeMap.put(InvocationInfoProxy.getTenantid(), corePoolSize);
        }
        return executorService;
    }
}
