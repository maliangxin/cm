package com.yonyoucloud.fi.cmp.paymentbill.service.redis;

import com.yonyou.ucf.mdd.ext.core.AppContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * desc:银行账户实时余额异步获取进度redis缓存工具类
 * author:wangqiangac
 * date:2023/7/7 17:47
 */
@Component
@Slf4j
public class BankBalanceRedisUtil {

    public static String dowloadPercentage = "bankBalanceDownloadKey";

    private Lock lock = new ReentrantLock();
    public  ProcessVo getProcess(String uid) {
        ProcessVo processVo = null;
        HashMap<String, ProcessVo> map = AppContext.cache().getObject(dowloadPercentage);
        if (map != null && !map.isEmpty()) {
            processVo = map.get(uid);
            if(processVo == null){
                return null;
            }
        }
        Double percent = processVo.getPercent();
        if(percent.equals(new Double(1))){
            map.remove(uid);
            AppContext.cache().setObject(dowloadPercentage, map);
        }
        return processVo;
    }

    /**
     * 设置 key
     *
     * @param uid
     * @param vo
     */
    private void setProcess(String uid, ProcessVo vo) {

        HashMap<String, ProcessVo> map = AppContext.cache().getObject(dowloadPercentage);
        if (map != null && !map.isEmpty()) {
            map.put(uid, vo);
            AppContext.cache().setObject(dowloadPercentage, map);
        } else {
            HashMap<String, ProcessVo> newMap = new HashMap<>();
            newMap.put(uid, vo);
            AppContext.cache().setObject(dowloadPercentage, newMap);
        }
    }

    /**
     * 业务请求发送后计算出批次后先设置缓存中ProcessVo的count
     * @param uid
     * @param count
     */
    public   void initProcess(String uid,int count){
        HashMap<String, ProcessVo> map = AppContext.cache().getObject(dowloadPercentage);
        ProcessVo vo = new ProcessVo();
        vo.setUid(uid);
        vo.setCount(count);
        vo.setSuccessCount(0);
        vo.setPercent(new Double(0));
        vo.setExecutedCount(new AtomicInteger(0));
        vo.setMessage(null);
        if (map != null && !map.isEmpty()) {
            map.put(uid, vo);
        } else {
            map = new HashMap<>();
            map.put(uid, vo);
        }
        AppContext.cache().setObject(dowloadPercentage, map);
    }
    /**
     * 更新百分比进度
     * @param uid
     * @param count 为循环获取银行账户实时余额的批次
     */
    public  void updateProcess(String uid, int count, boolean success, String message) {
        try{
            //lock.lockInterruptibly();
            ProcessVo process = this.getProcess(uid);
            if(process == null){
                ProcessVo vo = new ProcessVo();
                vo.setSuccessCount(1);
                vo.setUid(uid);
                vo.setCount(count);
                vo.setExecutedCount(new AtomicInteger(1));
                vo.setPercent(1/new Double(count));
                vo.setMessage(message);
                this.setProcess(uid,vo);
            }else{
                if(success){
                    process.setSuccessCount(process.getSuccessCount()+1);
                }
                if(process.getMessage()!=null && message!=null){
                    process.setMessage(process.getMessage()+";"+message);
                }else if(process.getMessage()==null && message!=null){
                    process.setMessage(message);
                }
                Double percent = (process.getExecutedCount().getAndIncrement()) / new Double(count);
                process.setPercent(percent);
//            process.setExecutedCount(process.getExecutedCount()+1);
                this.setProcess(uid,process);
            }
        }catch (Exception e){
            log.error("更新进度条失败",e);
        }
//        finally {
//            lock.unlock();
//        }

    }


    public  void updateProcessNew(String uid, int count, boolean success, String message) {
        ProcessVo process = this.getProcess(uid);
        if(process == null){
            ProcessVo vo = new ProcessVo();
            vo.setSuccessCount(1);
            vo.setUid(uid);
            vo.setCount(count);
            vo.setExecutedCount(new AtomicInteger(1));
            vo.setPercent(1/new Double(count));
            vo.setMessage(message);
            this.setProcess(uid,vo);
        }else{
            process.setSuccessCount(count);
            process.setExecutedCount(new AtomicInteger(count));
            if(process.getMessage()!=null && message!=null){
                process.setMessage(process.getMessage()+";"+message);
            }else if(process.getMessage()==null && message!=null){
                process.setMessage(message);
            }
            Double percent = 1d;
            process.setPercent(percent);
            this.setProcess(uid,process);
        }
    }
}
