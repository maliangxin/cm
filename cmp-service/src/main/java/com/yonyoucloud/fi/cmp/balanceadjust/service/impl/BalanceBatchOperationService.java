package com.yonyoucloud.fi.cmp.balanceadjust.service.impl;

import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.FilterVO;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import org.imeta.orm.base.BizObject;

import java.util.List;
import java.util.Map;

public interface BalanceBatchOperationService {

    /**
     * 查询待生成余额调节表接口
     * @param filterVO
     * @param reconciliationDataSource 对账数据源 1凭证；2银行日记账*
     * @return
     */
    List<CtmJSONObject> queryBatchConfirmedBalances(FilterVO filterVO,short reconciliationDataSource) throws Exception ;

    /**
     * 批量生成余额调节表接口
     * @param data
     * @return
     */
    CtmJSONObject saveBatchBalances(CtmJSONObject data) throws Exception;

    /**
     * 查询接口(排序)接口
     * @param params
     * @return
     */
    CtmJSONObject queryBatchBalances(CtmJSONObject params);

    /**
     * 202601版本，月末余额调节表生成调整为共用的方法
     * 添加字段到通用VO列表中，仅在值不为空时添加
     * 此方法用于构建通用VO列表，每个VO由一个名称-值对组成此方法确保只有当值不为空字符串时，才将其添加到列表中
     *
     * @param commonVOs 通用VO列表，存储构建的VO对象
     * @param itemName  项名称，表示VO中的名称字段
     * @param value     项的值，仅当此值不为空时才添加到VO中
     * @param mark      标记字段，决定是否使用LinkedHashMap
     */
    void addFieldIfNotEmpty(List<Map<String, Object>> commonVOs, String itemName, Object value, String mark);

    /**
     * 202601版本，月末余额调节表生成调整为共用的方法
     * 组装需要生成的余额调节表信息*
     * @param responseMsg 余额调节表信息
     * @param enterpriseBalance 账户余额信息
     * @param uncheckflag 是否包含未勾对的数据
     */
    void mergeResponseData(CtmJSONObject responseMsg, CtmJSONObject enterpriseBalance, String uncheckflag) ;
}
