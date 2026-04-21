package com.yonyoucloud.fi.cmp.bankreconciliationrepeat.dto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author guoxh
 */
public class BankreconciliationRepeatMsgRtnVO {
    private Map<String, String> failed = new HashMap();
    private List<String> messages = new ArrayList();
    private int count;
    private int sucessCount;
    private int failCount;
    private String errorMsg;

    public BankreconciliationRepeatMsgRtnVO(Map<String, String> failed, List<String> messages, int count, int sucessCount, int failCount) {
        this.failed = failed;
        this.messages = messages;
        this.count = count;
        this.sucessCount = sucessCount;
        this.failCount = failCount;
    }

    public BankreconciliationRepeatMsgRtnVO() {
        this.count = 0;
        this.sucessCount = 0;
        this.failCount = 0;
    }

    public Map<String, String> getFailed() {
        return this.failed;
    }

    public void setFailed(Map<String, String> failed) {
        this.failed = failed;
    }

    public List<String> getMessages() {
        return this.messages;
    }

    public void setMessages(List<String> messages) {
        this.messages = messages;
    }

    public int getCount() {
        return this.count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public int getSucessCount() {
        return this.sucessCount;
    }

    public void setSucessCount(int sucessCount) {
        this.sucessCount = sucessCount;
    }

    public int getFailCount() {
        return this.failCount;
    }

    public void setFailCount(int failCount) {
        this.failCount = failCount;
    }

    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }

    public String getErrorMsg() {
        return this.errorMsg;
    }
}
