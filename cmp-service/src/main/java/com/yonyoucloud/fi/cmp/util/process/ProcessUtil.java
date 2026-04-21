package com.yonyoucloud.fi.cmp.util.process;

import com.yonyou.iuap.yms.cache.YMSRedisTemplate;
import com.yonyou.ucf.basedoc.model.BankAcctCurrencyVO;
import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVO;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.util.DateUtils;
import com.yonyoucloud.fi.cmp.util.StringUtils;
import com.yonyoucloud.fi.cmp.util.YQLUtils;
import com.yonyoucloud.fi.cmp.util.basedoc.EnterpriseBankQueryService;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 异步拉取进度
 */
public class ProcessUtil {

    public static final String ALL_ADD_COUNT = "allAddCount";
    public static final String ALL_PULL_COUNT = "allPullCount";
    public static final String ACCOUNT_ID = "accountId";
    public static final HashMap<String, ProcessVo> uidProcessVoMap = new HashMap<>();


    /**
     * 初始化进度
     * @param uid
     * @param totalCount
     * @deprecated
     */
  @Deprecated
    public static ProcessVo initProcess(String uid, int totalCount){
        ProcessInfo info = ProcessInfo.builder().totalCount(totalCount).infos(new CopyOnWriteArrayList<>()).messages(new CopyOnWriteArrayList<>()).failInfos(new CopyOnWriteArrayList<>()).failCount(new AtomicInteger(0)).sucessCount(new AtomicInteger(0)).count(new AtomicInteger(0)).build();
        // CZFW-260753 产品(WDF)给出方案：统计不准确，不显示最后一行数据
//        ProcessInfo info = null;
        ProcessVo processVo = ProcessVo.builder().data(info).totalCount(totalCount).code(200).count(new AtomicInteger(0)).failCount(new AtomicInteger(0)).successCount(new AtomicInteger(0)).percentage(0).flag(1).build();
        YMSRedisTemplate redisTemplate = AppContext.getBean(YMSRedisTemplate.class);
        //redisTemplate.opsForValue().set(uid,CtmJSONObject.toJSONString(processVo));
        redisTemplate.opsForValue().set(uid,CtmJSONObject.toJSONString(processVo),60*60, TimeUnit.SECONDS);
        uidProcessVoMap.put(uid, processVo);
        return processVo;
    }

    /**
     * 初始化进度
     * @param uid
     * @param accountNum
     */
    public static ProcessVo initProcessWithAccountNum(String uid, int accountNum){
        if (StringUtils.isEmpty(uid)) {
            return null;
        }
        ProcessVo processVo = ProcessVo.builder().build();

        processVo.setSuccessCount(new AtomicInteger(0));
        processVo.setFailCount(new AtomicInteger(0));
        //processVo.setTotalCount(0);

        processVo.setUid(uid);
        //processVo.setMessage("");
        //processVo.setCode();
        //不为1，则前端报错
        processVo.setFlag(1);
        //不能为0，否则前端框架报错，先用账户数
        processVo.setCount(new AtomicInteger(accountNum));
        processVo.setPercentage(0);
        processVo.setSuccessAccountCount(new AtomicInteger(0));
        processVo.setFailAccountCount(new AtomicInteger(0));
        processVo.setTotalAccountCount(accountNum);
        processVo.setNewAddCount(new AtomicInteger(0));
        processVo.setTotalPullCount(new AtomicInteger(0));
        processVo.setData(new ProcessInfo(new AtomicInteger(accountNum),new AtomicInteger(0),new AtomicInteger(0),accountNum,new CopyOnWriteArrayList<>(),new CopyOnWriteArrayList<>(),new CopyOnWriteArrayList<>(),new CopyOnWriteArrayList<>()));
        YMSRedisTemplate redisTemplate = AppContext.getBean(YMSRedisTemplate.class);
        //redisTemplate.opsForValue().set(uid,CtmJSONObject.toJSONString(processVo));
        redisTemplate.opsForValue().set(uid,CtmJSONObject.toJSONString(processVo),60*60, TimeUnit.SECONDS);
        uidProcessVoMap.put(uid, processVo);
        return processVo;
    }

    /**
     *
     * @param uid
     *
     */
    public static void failAccountNumAddOne(String uid){
        if (StringUtils.isEmpty(uid)) {
            return;
        }
        YMSRedisTemplate redisTemplate = AppContext.getBean(YMSRedisTemplate.class);
        
        ProcessVo processVo = uidProcessVoMap.get(uid);
        processVo.failAccountNumAddOne();
        //redisTemplate.opsForValue().set(uid,CtmJSONObject.toJSONString(processVo));
        redisTemplate.opsForValue().set(uid,CtmJSONObject.toJSONString(processVo),60*60, TimeUnit.SECONDS);
    }

    /**
     *
     * @param uid
     *
     */
    public static void failAccountNumAdd(String uid, int count){
        if (StringUtils.isEmpty(uid)) {
            return;
        }
        YMSRedisTemplate redisTemplate = AppContext.getBean(YMSRedisTemplate.class);
        
        ProcessVo processVo = uidProcessVoMap.get(uid);
        processVo.failAccountNumAdd(count);
        //redisTemplate.opsForValue().set(uid,CtmJSONObject.toJSONString(processVo));
        redisTemplate.opsForValue().set(uid,CtmJSONObject.toJSONString(processVo),60*60, TimeUnit.SECONDS);
    }

    /**
     *
     * @param uid
     *
     */
    public static void totalPullCountAdd(String uid, int count){
        if (StringUtils.isEmpty(uid)) {
            return;
        }
        YMSRedisTemplate redisTemplate = AppContext.getBean(YMSRedisTemplate.class);
        
        ProcessVo processVo = uidProcessVoMap.get(uid);
        processVo.totalPullCountAdd(count);
        //redisTemplate.opsForValue().set(uid,CtmJSONObject.toJSONString(processVo));
        redisTemplate.opsForValue().set(uid,CtmJSONObject.toJSONString(processVo),60*60, TimeUnit.SECONDS);
    }

    /**
     *
     * @param uid
     *
     */
    public static void newAddCountAdd(String uid, int count){
        if (StringUtils.isEmpty(uid)) {
            return;
        }
        YMSRedisTemplate redisTemplate = AppContext.getBean(YMSRedisTemplate.class);
        
        ProcessVo processVo = uidProcessVoMap.get(uid);
        processVo.newAddCountAdd(count);
        //redisTemplate.opsForValue().set(uid,CtmJSONObject.toJSONString(processVo));
        redisTemplate.opsForValue().set(uid,CtmJSONObject.toJSONString(processVo),60*60, TimeUnit.SECONDS);
    }

    /**
     * 只有存在报错提示信息时 才调用此方法 一旦存在message 则插件会认为当前失败数量+1
     * @param uid
     * @param message
     */
    public static void addMessage(String uid,String message){
        if (StringUtils.isEmpty(uid)) {
            return;
        }
        YMSRedisTemplate redisTemplate = AppContext.getBean(YMSRedisTemplate.class);
        
        ProcessVo processVo = uidProcessVoMap.get(uid);
        processVo.addMessage(message);
        processVo.addInfo(message);
        processVo.addFailInfo(message);
        processVo.getFailCount().incrementAndGet();
        processVo.getData().getFailCount().incrementAndGet();
        //无论成功失败 已处理数量+1
        processVo.getCount().incrementAndGet();
        processVo.getData().getCount().incrementAndGet();
        //redisTemplate.opsForValue().set(uid,CtmJSONObject.toJSONString(processVo));
        redisTemplate.opsForValue().set(uid,CtmJSONObject.toJSONString(processVo),60*60, TimeUnit.SECONDS);
    }

    /**
     * 只有存在报错提示信息时 才调用此方法 一旦存在message 则插件会认为当前失败数量+1,提示下是银企联返回报错
     * @param uid
     * @param message
     */
    public static void addYQLErrorMessage(String uid,String message){
        message = com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400573", "银企联返回报错信息：") /* "银企联返回报错信息：" */ + message + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400574", "请联系银企联顾问处理！") /* "请联系银企联顾问处理！" */;
        YMSRedisTemplate redisTemplate = AppContext.getBean(YMSRedisTemplate.class);
        
        ProcessVo processVo = uidProcessVoMap.get(uid);
        processVo.addMessage(message);
        processVo.addInfo(message);
        processVo.addFailInfo(message);
        processVo.getFailCount().incrementAndGet();
        processVo.getData().getFailCount().incrementAndGet();
        //无论成功失败 已处理数量+1
        processVo.getCount().incrementAndGet();
        processVo.getData().getCount().incrementAndGet();
        //redisTemplate.opsForValue().set(uid,CtmJSONObject.toJSONString(processVo));
        redisTemplate.opsForValue().set(uid,CtmJSONObject.toJSONString(processVo),60*60, TimeUnit.SECONDS);
    }

    /**
     * 银企联返回无数据时 才调用此方法，加新信息
     * @param uid
     * @param message
     */
    public static void addYQLNodataMessage(String uid,String message){
        message = com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400575", "银企联查询无数据，返回信息：") /* "银企联查询无数据，返回信息：" */ + message ;
        YMSRedisTemplate redisTemplate = AppContext.getBean(YMSRedisTemplate.class);
        
        ProcessVo processVo = uidProcessVoMap.get(uid);
        processVo.addNoDataInfos(message);
        //redisTemplate.opsForValue().set(uid,CtmJSONObject.toJSONString(processVo));
        redisTemplate.opsForValue().set(uid,CtmJSONObject.toJSONString(processVo),60*60, TimeUnit.SECONDS);
    }


    /**
     * 银企联返回无数据时 才调用此方法，改旧信息
     *
     * @param uid
     * @param message
     * @param accountId_currencyCode_key
     */
    public static void changYQLNodataMessage(String uid, String message, String accountId_currencyCode_key){
        message = com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400575", "银企联查询无数据，返回信息：") /* "银企联查询无数据，返回信息：" */ + message ;
        YMSRedisTemplate redisTemplate = AppContext.getBean(YMSRedisTemplate.class);
        
        ProcessVo processVo = uidProcessVoMap.get(uid);
        if (accountId_currencyCode_key != null) {
            CopyOnWriteArrayList<String> noDataInfos = processVo.getData().noDataInfos;
            for (int i = 0; i < noDataInfos.size(); i++) {
                String noDataInfo = noDataInfos.get(i);
                if (noDataInfo.contains(accountId_currencyCode_key)) {
                    noDataInfos.set(i, noDataInfo + '\n' + message);
                    break;
                }
            }
        }
        //redisTemplate.opsForValue().set(uid,CtmJSONObject.toJSONString(processVo));
        redisTemplate.opsForValue().set(uid,CtmJSONObject.toJSONString(processVo),60*60, TimeUnit.SECONDS);
    }

    /**
     * 内部账户返回无数据时 才调用此方法
     *
     * @param uid
     * @param
     */
    public static void addInnerNodataMessage(String uid, List<String> accountIdList, Date startDate, Date endDate, String checkMsg) throws Exception {
        if (StringUtils.isEmpty(uid)) {
            return;
        }
        List<String> accountList = EnterpriseBankQueryService.findAccountByIdList(accountIdList);
        YMSRedisTemplate redisTemplate = AppContext.getBean(YMSRedisTemplate.class);
        
        ProcessVo processVo = uidProcessVoMap.get(uid);
        // 将账户拆分为单个报错信息并分别添加
        for (String account : accountList) {
            String message = StringUtils.formatStrNullSafe(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400576", "内部账户[%s][%s]-[%s]查询无数据。%s") /* "内部账户[%s][%s]-[%s]查询无数据。%s" */,
                    account, DateUtils.formatDate(startDate), DateUtils.formatDate(endDate), checkMsg);
            processVo.addNoDataInfos(message);
        }
        //redisTemplate.opsForValue().set(uid, CtmJSONObject.toJSONString(processVo));
        redisTemplate.opsForValue().set(uid,CtmJSONObject.toJSONString(processVo),60*60, TimeUnit.SECONDS);
    }

    /**
     * 刷新进度条
     * @param uid
     * @deprecated
     */
  @Deprecated
    public static void refreshProcess(String uid,boolean success) {
        YMSRedisTemplate redisTemplate = AppContext.getBean(YMSRedisTemplate.class);
        
        ProcessVo processVo = uidProcessVoMap.get(uid);
        if (success){
            processVo.getSuccessCount().incrementAndGet();
            processVo.getData().getSucessCount().incrementAndGet();
        }else {
            processVo.getFailCount().incrementAndGet();
            processVo.getData().getFailCount().incrementAndGet();
        }
        //无论成功失败 已处理数量+1
        processVo.getCount().incrementAndGet();
        processVo.getData().getCount().incrementAndGet();
        //如果进度已经是100%，则直接结束，否则更新进度条
        if (processVo.getPercentage() == 100) {
            uidProcessVoMap.remove(uid);
            return;
        }
        processVo.setPercentage(processVo.getData().getCount().intValue() * 100.0 / processVo.getTotalCount());
        //redisTemplate.opsForValue().set(uid,CtmJSONObject.toJSONString(processVo));
        redisTemplate.opsForValue().set(uid,CtmJSONObject.toJSONString(processVo),60*60, TimeUnit.SECONDS);
    }

    /**
     * 根据账户刷新进度条
     * @param uid
     */
    public static void refreshAccountProcess(String uid,boolean success,int count) {
        YMSRedisTemplate redisTemplate = AppContext.getBean(YMSRedisTemplate.class);
        
        ProcessVo processVo = uidProcessVoMap.get(uid);
        if (success){
            processVo.getSuccessAccountCount().addAndGet(count);
            processVo.getSuccessCount().addAndGet(count);
            processVo.getData().getSucessCount().addAndGet(count);
        }else {
            processVo.getFailAccountCount().addAndGet(count);
            processVo.getFailCount().addAndGet(count);
            processVo.getData().getFailCount().addAndGet(count);
        }
        //无论成功失败 已处理数量+1
        processVo.getCount().incrementAndGet();
        processVo.getData().getCount().incrementAndGet();
        //如果进度已经是100%，则直接结束，否则更新进度条
        if (processVo.getPercentage() == 100) {
            uidProcessVoMap.remove(uid);
            return;
        }
        //按账户更新进度条百分比
        processVo.setPercentage((processVo.getFailAccountCount().intValue() + processVo.getSuccessAccountCount().intValue()) * 100.0 / processVo.getTotalAccountCount());
        //redisTemplate.opsForValue().set(uid,CtmJSONObject.toJSONString(processVo));
        redisTemplate.opsForValue().set(uid,CtmJSONObject.toJSONString(processVo),60*60, TimeUnit.SECONDS);
    }

    /**
     * 刷新进度条
     * @param uid
     * @deprecated
     */
  @Deprecated
    public static void refreshProcess(String uid,boolean success,int count) {
        YMSRedisTemplate redisTemplate = AppContext.getBean(YMSRedisTemplate.class);
        
        ProcessVo processVo = uidProcessVoMap.get(uid);
        if (success){
            processVo.getSuccessCount().addAndGet(count);
            processVo.getData().getSucessCount().addAndGet(count);
        }else {
            processVo.getFailCount().addAndGet(count);
            processVo.getData().getFailCount().addAndGet(count);
        }
        //无论成功失败 已处理数量+1
        processVo.getCount().incrementAndGet();
        processVo.getData().getCount().incrementAndGet();
        //如果进度已经是100%，则直接结束，否则更新进度条
        if (processVo.getPercentage() == 100) {
            uidProcessVoMap.remove(uid);
            return;
        }
        processVo.setPercentage(processVo.getData().getCount().intValue() * 100.0 / processVo.getTotalCount());
        //redisTemplate.opsForValue().set(uid,CtmJSONObject.toJSONString(processVo));
        redisTemplate.opsForValue().set(uid,CtmJSONObject.toJSONString(processVo),60*60, TimeUnit.SECONDS);
    }

    /**
     * 进度完成
     * @param uid
     */
    public static void completed(String uid) {
        completed(uid, false);
    }

    /**
     * 进度完成
     * @param uid
     * @param isNew 是否新版，新版供银企联拉取数据使用，不展示弹窗上的计数
     */
    public static void completed(String uid, Boolean isNew) {
        if (StringUtils.isEmpty(uid)) {
            return;
        }
        YMSRedisTemplate redisTemplate = AppContext.getBean(YMSRedisTemplate.class);

        ProcessVo processVo;
        if(uidProcessVoMap.get(uid) == null){
            processVo = initProcessWithAccountNum(uid,0);
        }else{
            processVo =  uidProcessVoMap.get(uid);;
        }
        setPercent100(uid, processVo, isNew);
        processVo.setFlag(1);
        //redisTemplate.opsForValue().set(uid,CtmJSONObject.toJSONString(processVo));
        redisTemplate.opsForValue().set(uid,CtmJSONObject.toJSONString(processVo),60*60, TimeUnit.SECONDS);
    }

    public static void  completedResetCount(String uid) {
        completedResetCount(uid, false);
    }

    public static void  completedResetCount(String uid, boolean isNew) {
        YMSRedisTemplate redisTemplate = AppContext.getBean(YMSRedisTemplate.class);

        ProcessVo processVo;
        if(uidProcessVoMap.get(uid) == null){
            processVo = initProcessWithAccountNum(uid,0);
        }else{
            processVo =  uidProcessVoMap.get(uid);
            int newCount = processVo.getSuccessCount().intValue() +processVo.getFailCount().intValue();
            processVo.getCount().getAndSet(newCount);
            processVo.getData().getCount().getAndSet(newCount);
            //最后统计的总条数需要刷新
            processVo.setTotalCount(newCount);
            processVo.getData().setTotalCount(newCount);
        }
        setPercent100(uid, processVo, isNew);
        processVo.setFlag(1);
        //redisTemplate.opsForValue().set(uid,CtmJSONObject.toJSONString(processVo));
        redisTemplate.opsForValue().set(uid,CtmJSONObject.toJSONString(processVo),60*60, TimeUnit.SECONDS);
    }



    private static void setPercent100(String uid, ProcessVo processVo, boolean isNew) {
        processVo.setPercentage(Double.valueOf(100));
        if (isNew) {
            //平台用的这个值设置为空后，弹窗里就没有计数信息了
            ProcessInfo data = processVo.getData();
            data.setSucessCount(null);
        }
        uidProcessVoMap.remove(uid);
    }

    /**
     * 获取进度
     * @param uid
     * @return
     */
    public static String getProcess(String uid) {
        YMSRedisTemplate redisTemplate = AppContext.getBean(YMSRedisTemplate.class);
        return (String)redisTemplate.opsForValue().get(uid);
    }

    public static void refreshFailAccountProcess(String uid, Set<String> failAccountSet, List<EnterpriseBankAcctVO> enterpriseBankAcctList) {
        // 参数校验
        if (uid == null || failAccountSet == null || enterpriseBankAcctList == null || enterpriseBankAcctList.size() == 0) {
            return;
        }
        for (EnterpriseBankAcctVO enterpriseBankAcctVO : enterpriseBankAcctList) {
            String accountId = enterpriseBankAcctVO.getId();
            for (BankAcctCurrencyVO bankAcctCurrencyVO : enterpriseBankAcctVO.getCurrencyList()) {
                String accountId_currency_key = accountId + "|" + bankAcctCurrencyVO.getCurrency();
                if (!failAccountSet.contains(accountId_currency_key)) {
                    failAccountSet.add(accountId_currency_key);
                    refreshAccountProcess(uid, false, 1);
                }
            }
        }
    }


    public static void refreshFailAccountProcess(String uid, Set<String> failAccountSet, String accountId_currencyCode_key) {
        // 参数校验
        if (uid == null || failAccountSet == null || accountId_currencyCode_key == null) {
            return;
        }
        if (!failAccountSet.contains(accountId_currencyCode_key)) {
            failAccountSet.add(accountId_currencyCode_key);
            refreshAccountProcess(uid, false, 1);
        }
    }

    public static void dealYQLNodataMessage(CtmJSONObject param, String uid, Set<String> failAccountSet, String key, CtmJSONObject responseHead) {
        //解决多线程下的并发问题
        if (failAccountSet.add(key)) {
            // key 不存在，且已成功添加
            addYQLNodataMessage(uid, YQLUtils.getYQLErrorMsqForManual(param, responseHead));
        } else {
            // key 已存在
            changYQLNodataMessage(uid, YQLUtils.getYQLErrorMsqForManual(param, responseHead), key);
        }
    }
}
