package com.yonyoucloud.fi.cmp.openapi.service.impl;

import com.yonyou.iuap.ucf.common.i18n.InternationalUtils;
import com.yonyou.iuap.yms.lock.YmsLock;
import com.yonyou.ucf.basedoc.model.BankAcctCurrencyVO;
import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVO;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.yonbip.ctm.accelerator.threadpool.CtmThreadPoolExecutor;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.util.lock.CtmLockTool;
import com.yonyou.yonbip.ctm.util.lock.LockStatus;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.cmp.api.openapi.InnerAccountDetailService;
import com.yonyoucloud.fi.cmp.bankdealdetail.BankDealDetailService;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.paymentbill.service.PaymentService;
import com.yonyoucloud.fi.cmp.util.JedisLockUtils;
import com.yonyoucloud.fi.cmp.util.QueryBaseDocUtils;
import com.yonyoucloud.fi.cmp.util.TaskUtils;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import com.yonyoucloud.fi.cmp.util.process.ProcessUtil;
import com.yonyoucloud.fi.cmp.util.threadpool.ThreadPoolBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * 查询内部交易明细接口
 * 2023年11月23日09:22:56
 * author:wq
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class InnerAccountDetailServiceImpl implements InnerAccountDetailService {
    @Autowired
    private CtmThreadPoolExecutor executorServicePool;

    @Autowired
    private PaymentService paymentService;

    private final BaseRefRpcService baseRefRpcService;
    @Autowired
    private BankDealDetailService bankDealDetailService;
    /**
     * 内部交易明细拉取及关联结算自动确认
     *
     * @param param
     * @return
     * @throws Exception
     */
    @Override
    public Map<String, Object> queryInnerAccountDetail(CtmJSONObject param) throws Exception {
        //内部账户的集合
        List<Map<String, Object>> list = QueryBaseDocUtils.queryInnerBankAccountByParam(param);
        log.error("内部交易明细拉取及关联结算自动确认内部账户的集合list:"+CtmJSONObject.toJSONString(list));
        String logId = param.getString("logId");

        List<List<Map<String, Object>>> excuteList = ValueUtils.splitList(list, 10);
        List<Callable<Object>> callables = new ArrayList<>();
        // 查询所有的账户信息+行为
        List<String> accountIdList=new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            String key = list.get(i).get("account") + ICmpConstant.QUERYTRANSDETAILKEY;
            if (accountIdList.contains(key)) {
                continue;
            }
            accountIdList.add(key);
        }

        executorServicePool.getThreadPoolExecutor().submit(() -> {
            try {
                bankTradeDetailAsyncProcess(param, list, logId);
            } catch (Exception e) {
                log.error("查询内部账户交易明细失败：",e);
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100176"),e.getMessage());
            }
        });
        Map<String, Object> retMap = new HashMap<>();
        retMap.put("asynchronized", true);
        return retMap;
    }

    /**
     * 内部交易明细进行分批
     *
     * @param paramNew
     * @param list
     * @param logId
     */
    private void bankTradeDetailAsyncProcess(CtmJSONObject paramNew, List<Map<String, Object>> list, String logId) {
        try {
            List<List<Map<String, Object>>> excuteList = ValueUtils.splitList(list, 10);
            List<Callable<Object>> callables = new ArrayList<>();
            for (List<Map<String, Object>> callableList : excuteList) {
                callables.add(() -> {
                    excuteBankTradeDetailAsyncProcess(paramNew, callableList, logId);
                    return null;
                });
            }
            ExecutorService executorService = null;
            try {
                executorService = buildThreadPoolForInnerDetail();
                List<Future<Object>> futrueList = executorService.invokeAll(callables);
                if (!CollectionUtils.isEmpty(futrueList)) {
                    for (Future<Object> futrue : futrueList) {
                        futrue.get();
                    }
                }
            } catch (InterruptedException | ExecutionException e) {
                log.error("内部交易明细拉取任务异常：", e);
            } finally {
                if (executorService != null) {
                    executorService.shutdown();
                }
            }
            TaskUtils.updateTaskLog((Map<String,String>)paramNew.get("ipaParams"),TaskUtils.TASK_BACK_SUCCESS, logId, InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180272", "执行成功") /* "执行成功" */, TaskUtils.UPDATE_TASK_LOG_URL);
        } catch (Exception e) {
            TaskUtils.updateTaskLog((Map<String,String>)paramNew.get("ipaParams"),TaskUtils.TASK_BACK_FAILURE, logId, e.getMessage(), TaskUtils.UPDATE_TASK_LOG_URL);
            log.error("queryBankTradeDetailElectronList", e);
        }
    }

    /**
     * 内部交易明细循环拉取
     *
     * @param paramNew
     * @param list
     * @param logId
     * @throws Exception
     */
    private void excuteBankTradeDetailAsyncProcess(CtmJSONObject paramNew, List<Map<String, Object>> list, String logId) throws Exception {
        for (int i = 0; i < list.size(); i++) {
            String enterpriseBankAccount = list.get(i).get("enterpriseBankAccount") != null ? list.get(i).get("enterpriseBankAccount").toString() : list.get(i).get("id").toString();
            List<BankAcctCurrencyVO> currencyVOs = baseRefRpcService.queryEnterpriseBankAccountById(enterpriseBankAccount).getCurrencyList();
            HashMap<String, String> currencyVOsMap = paymentService.queryCurrencyCode(currencyVOs);
            //循环账户下的币种信息
            log.error("内部交易明细拉取及关联结算自动确认账号id为:["+enterpriseBankAccount+"]银行账号:+["+list.get(i).get("account") +"]+对应的币种集合:"+CtmJSONObject.toJSONString(currencyVOsMap));
            for (BankAcctCurrencyVO currencyVO : currencyVOs) {
                CtmJSONObject param = new CtmJSONObject();
                param.put("startDate",paramNew.get("startDate"));
                param.put("endDate",paramNew.get("endDate"));
                param.put("currencyCode", currencyVOsMap.get(currencyVO.getCurrency()));
                param.put("accEntity", list.get(i).get("orgid"));
                param.put("accountId", list.get(i).get("id"));
                param.put("enterpriseBankAccount", list.get(i).get("id"));
                param.put("account", list.get(i).get("account"));
                //是否关联结算明细
                param.put("isRelationSettle",true);
                param.put("insertCount",0);

                log.error("===============定时任务拉取内部交易明细循环参数.." + param.toString());
                String accountInfo = param.get("account") + "|" + param.get("startDate") ;
                String lockKey = accountInfo +  ICmpConstant.QUERYTRANSDETAILKEY;
                try {
                    CtmLockTool.executeInOneServiceLock(lockKey,60*60*2L, TimeUnit.SECONDS,(int lockstatus)->{
                        if(lockstatus == LockStatus.GETLOCK_FAIL){
                            //加锁失败添加报错信息
                            log.error("excuteBankTradeDetailAsyncProcess失败");
                            TaskUtils.updateTaskLog((Map<String,String>)paramNew.get("ipaParams"),TaskUtils.TASK_BACK_SUCCESS, logId,
                                    String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400784", "[%s]:系统正在对此账户拉取中") /* "[%s]:系统正在对此账户拉取中" */,accountInfo), TaskUtils.UPDATE_TASK_LOG_URL);
                            return ;
                        }
                        // 加锁成功,内部交易回单
                        bankDealDetailService.queryInnerAccountDetails(param);
                    });
                } catch (Exception e) {
                    log.error("excuteBankTradeDetailAsyncProcess失败");
                    TaskUtils.updateTaskLog((Map<String,String>)paramNew.get("ipaParams"),TaskUtils.TASK_BACK_SUCCESS, logId,
                            String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400783", "[%s]:此账户操作发生异常") /* "[%s]:此账户操作发生异常" */,accountInfo), TaskUtils.UPDATE_TASK_LOG_URL);
                }
            }
        }
    }

    private ExecutorService buildThreadPoolForInnerDetail(){
        ExecutorService executorService = ThreadPoolBuilder.defaultThreadPoolBuilder()
                .builder(null,null,null,null);
        return executorService;
    }
}
