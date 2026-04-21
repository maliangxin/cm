package com.yonyoucloud.fi.cmp.paymargin.service;

import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.paymargin.PayMargin;
import com.yonyoucloud.fi.cmp.paymargin.service.impl.PayMarginApiDto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * PayMargin的 OpenApi接口相关逻辑
 */
public interface PayMarginApiService {

    /**
     * 保存单据
     * 1. 仅支持新增，不支持更新
     * 2. 事项来源为外部系统
     * @return 成功新增单据的关键字段信息
     */
    Map<String,Object> saveBill(PayMarginApiDto param);

    /**
     * 删除单据
     * 1. 只能删除通过OpenApi写入的单据
     * 2. 只能删除状态是草稿态的单据
     * @param param 被删除单据的 srcBillNo
     * @return 成功被删除的单据的 srcBillNo
     */
    List<String> deleteBill(CtmJSONObject param);

    /**
     * 针对OpenApi写入的数据需要额外处理的逻辑：参数校验，字段翻译等
     * @param entity 请求数据
     */
    void beforeSaveForOpenApi(PayMargin entity);

    /**
     * 计算汇率，如果失败则抛异常。在通过OpenApi场景下导入时使用
     * @param payMargin 请求实体
     * @return 利率
     */
    CtmJSONObject getExchRate(PayMargin payMargin);

}
