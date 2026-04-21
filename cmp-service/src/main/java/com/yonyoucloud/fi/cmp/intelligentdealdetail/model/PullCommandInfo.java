package com.yonyoucloud.fi.cmp.intelligentdealdetail.model;

import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVO;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import lombok.Data;

import java.util.List;

@Data
public class PullCommandInfo {

    int begNum;
    private String tranCode;
    Object queryExtend;
    private CtmJSONObject requestParam;
    String startDate;
    String endDate;
    private int corepoolsize;
    private List<EnterpriseBankAcctVO> bankAcctVOList;
    private String customNo;
    private String signature;
    private String channel;
}
