package com.yonyoucloud.fi.cmp.reconciliate;

import java.util.List;
import java.util.Map;

/**
 * @author wangshbv
 * @date 2020-03-15
 */
public interface UpSettleStatusAndGenVouchService {
    /**
     * 查询单据并更新数据
     * @param ids
     * @throws Exception
     */
    public void updateRecvSettleNew(List<Long>  ids) throws Exception;

    /**
     * 生成凭证
     * @param ids
     * @return
     * @throws Exception
     */
    public Map<String,Object> generateRecvVoucherNew(List<Long> ids) throws Exception;
}
