package com.yonyoucloud.fi.cmp.intelligentdealdetail.common.command;

import com.yonyoucloud.fi.cmp.intelligentdealdetail.model.PullCommandInfo;
import com.yonyoucloud.fi.cmp.util.DateUtils;
import com.yonyoucloud.fi.cmp.util.YQLUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class BankAccountDataPullCommand {

    public abstract void execute(PullCommandInfo transInfo) throws Exception;

    /*
     * @Author tongyd
     * @Description 构建请求流水号
     * @Date 2019/9/12
     * @Param [customNo]
     * @return java.lang.String
     **/
    public String buildRequestSeqNo(String customNo) {
        StringBuilder tranSeqNo = new StringBuilder("R");
        tranSeqNo.append(customNo);
        tranSeqNo.append("0000");
        tranSeqNo.append(DateTimeFormatter.ofPattern(DateUtils.MILLISECOND_PATTERN).format(LocalDateTime.now()));
        tranSeqNo.append(YQLUtils.getSerialNumberNoCAS(cardinalNumber));
        return tranSeqNo.toString();
    }

    private AtomicInteger cardinalNumber = new AtomicInteger();

}
