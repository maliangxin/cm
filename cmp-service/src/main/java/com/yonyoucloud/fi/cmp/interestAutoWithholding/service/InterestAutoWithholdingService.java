package com.yonyoucloud.fi.cmp.interestAutoWithholding.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.yonyoucloud.fi.cmp.accrualsWithholding.AccrualsWithholding;

import java.util.List;
import java.util.Map;

public interface InterestAutoWithholdingService {

    /**
     * 自动预提
     * @param logId
     * @param tenant
     * @param accentity
     * @param bankType
     * @param bankaccount
     * @param currency
     * @return
     */
    Map<String,Object> interestAutoWithholding(String logId, String tenant,String accentity,String bankType,String bankaccount,String currency);

    /**
     * 利息测算
     *
     * @param depositinterestWithholdinges 列表数据
     * @return
     * @throws Exception
     */
    JsonNode calculate(List<AccrualsWithholding> depositinterestWithholdinges) throws Exception;

    /**
     * 预提
     *
     * @param depositinterestWithholdinges 列表数据
     * @return
     * @throws Exception
     */
    void tWithholding(List<AccrualsWithholding> depositinterestWithholdinges) throws Exception;
}
