package com.yonyoucloud.fi.cmp.util.process;

/**
 * desc:
 * author:maliangn
 * date:2023/12/5 19:58
 */

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 百分比进度类
 */
@Data
@Builder
@NoArgsConstructor      // ← 给 Jackson 用
@AllArgsConstructor
public class ProcessVo implements Serializable {
    private static final long SerialVersionUID = 7788999100L;
    String uid;
    String message;
    int code;
    int flag;
    ProcessInfo data;



       /**
    * @deprecated
    */
  @Deprecated
    AtomicInteger successCount;//成功的数目
       /**
    * @deprecated
    */
  @Deprecated
    AtomicInteger failCount;//失败的数目
       /**
    * @deprecated
    */
  @Deprecated
    int totalCount;//总数


    AtomicInteger count;//已处理数据数目
    double percentage;//当前进度，按账户统计百分比，不按数据条数统计百分比

    AtomicInteger successAccountCount;//成功账户的数目，不给值，需要通过计算得出totalAccountCount-failAccountCount
    AtomicInteger failAccountCount;//失败账户的数目
    int totalAccountCount;//总账户的数目

    AtomicInteger newAddCount;//新增数据的数目
    AtomicInteger totalPullCount;//拉取成功数据的数目



    public ProcessVo fail(){
        this.failCount.incrementAndGet();
        return this;
    }

    public ProcessVo success(){
        this.successCount.incrementAndGet();
        return this;
    }

    public ProcessVo failAccountNumAddOne(){
        this.failAccountCount.incrementAndGet();
        return this;
    }

    public ProcessVo failAccountNumAdd(int count){
        this.failAccountCount.addAndGet(count);
        return this;
    }

    public ProcessVo successAccountNumAddOne(){
        this.successAccountCount.incrementAndGet();
        return this;
    }

    public ProcessVo newAddCountAdd(int count){
        this.newAddCount.addAndGet(count);
        return this;
    }

    public ProcessVo totalPullCountAdd(int count){
        this.totalPullCount.addAndGet(count);
        return this;
    }

    public void addNoDataInfos(String noDataInfos){
        if(StringUtils.isNotEmpty(noDataInfos)){
            this.data.noDataInfos.add(noDataInfos);
        }
    }

    public void addMessage(String message){
        if(StringUtils.isNotEmpty(message)){
            this.data.messages.add(message);
        }
    }

    public void addInfo(String info){
        if(StringUtils.isNotEmpty(info)){
            this.data.infos.add(info);
        }
    }

    public void addFailInfo(String info){
        if(StringUtils.isNotEmpty(info)){
            this.data.failInfos.add(info);
        }
    }

}