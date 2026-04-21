package com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.exception;

import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.consts.DealDetailBusinessCodeEnum;

/**
 * @Author guoyangy
 * @Date 2024/7/1 15:59
 * @Description todo
 * @Version 1.0
 */
public class BankDealDetailException extends RuntimeException{
    private String msg;
    //业务码
    private DealDetailBusinessCodeEnum dealDetailBusinessCodeEnum;
    private int errCode;
    public BankDealDetailException(){}
    public BankDealDetailException(int errCode){
        this.errCode = errCode;
    }
    public BankDealDetailException(int errCode,String msg){
        super(msg);
        this.errCode = errCode;
    }
    public BankDealDetailException(int errCode,String msg,Throwable t){
        super(msg,t);
        this.errCode = errCode;
    }
    public BankDealDetailException(String msg,Throwable t){
        super(msg,t);
    }
    public BankDealDetailException(String msg){
        super(msg);
    }

    //业务码异常构造方法
    public BankDealDetailException(DealDetailBusinessCodeEnum dealDetailBusinessCodeEnum){
        this.dealDetailBusinessCodeEnum = dealDetailBusinessCodeEnum;
    }
    public BankDealDetailException(DealDetailBusinessCodeEnum dealDetailBusinessCodeEnum,Throwable t){
        super(t);
        this.dealDetailBusinessCodeEnum = dealDetailBusinessCodeEnum;
    }

    public DealDetailBusinessCodeEnum getDealDetailBusinessCodeEnum() {
        return dealDetailBusinessCodeEnum;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public int getErrCode() {
        return errCode;
    }

    public void setErrCode(int errCode) {
        this.errCode = errCode;
    }
}