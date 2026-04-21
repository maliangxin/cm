package com.yonyoucloud.fi.cmp.accrualsWithholding.service;


import com.fasterxml.jackson.databind.JsonNode;
import com.yonyoucloud.fi.cmp.accrualsWithholding.AccrualsWithholding;

import java.util.List;

/**
 * @author shangxd
 * @date 2023/4/14 9:47
 * @describe
 */
public interface AccrualsWithholdingService {

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
    JsonNode tWithholding(List<AccrualsWithholding> depositinterestWithholdinges) throws Exception;

    /**
     * 反预提
     *
     * @param depositinterestWithholdinges 列表数据
     * @return
     * @throws Exception
     */
    JsonNode unwWthholding(List<AccrualsWithholding> depositinterestWithholdinges) throws Exception;

    /**
     * 查询查询利息测算表记录
     *
     * @param depositinterestWithholdinges 列表数据
     * @return 日结数据
     * @throws Exception
     */
    JsonNode queryInterestHandleList(List<AccrualsWithholding> depositinterestWithholdinges) throws Exception;
    /**
     * 查询查询利息测算表记录 银行预提记录卡片页面调用
     *
     * @param depositinterestWithholdinges 列表数据
     * @return 日结数据
     * @throws Exception
     */
    JsonNode queryInterestList(List<AccrualsWithholding> depositinterestWithholdinges) throws Exception;

}
