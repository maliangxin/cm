package com.yonyoucloud.fi.cmp.reconciliate.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @description: 银行对账单到凭证 电子回单信息事件推送实体类
 * @author: wanxbo@yonyou.com
 * @date: 2024/1/16 16:23
 */
@Data
public class BankReceiptToVoucherVO {
    /**
     * 勾兑号；银行对账单和凭证 一对一；多对一；一对多 勾兑号是唯一
     */
    private String checkNo;

    /**
     * 凭证id集合
     */
    private List<String> voucherbidList = new ArrayList<>();

    /**
     * 关联动作；1关联；0取消关联
     */
    private String actionType;

    /**
     * 银行对账单和回单关联关系信息集合
     */
    private List<BankReceiptInfoVO> bankReceiptInfoVOList = new ArrayList<>();

}
