package com.yonyoucloud.fi.cmp.inwardremittance;

import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.vo.InwardRemittanceDetailQueryRequestVO;
import com.yonyoucloud.fi.cmp.vo.InwardRemittanceListQueryRequestVO;

/**
 * 汇入汇款接口
 */
public interface InwardRemittanceService {

    // 汇入汇款确认提交SSFE1004
    String INWARD_REMITTANCE_SUBMIT = "SSFE1004";
    // 汇入汇款确认交易结果查询SSFE3004
    String INWARD_REMITTANCE_RESULT_QUERY = "SSFE3004";
    // 汇入汇款待确认业务列表查询SSFE3005
    String INWARD_REMITTANCE_LIST_QUERY = "SSFE3005";
    // 汇入汇款业务明细查询SSFE3006
    String INWARD_REMITTANCE_DETAIL_QUERY = "SSFE3006";

    /**
     * 汇入汇款确认提交SSFE1004
     * @return
     */
    CtmJSONObject inwardRemittanceSubmit(CtmJSONObject param) throws Exception;

    /**
     * 汇入汇款确认交易结果查询SSFE3004
     * @return
     */
    void inwardRemittanceResultQuery(CtmJSONObject param) throws Exception;

    /**
     * 汇入汇款待确认业务列表查询SSFE3005
     * @return
     */
    CtmJSONArray inwardRemittanceListQuery(CtmJSONObject param) throws Exception;

    /**
     * 汇入汇款业务明细查询SSFE3006
     * @return
     */
    CtmJSONArray inwardRemittanceDetailQuery(InwardRemittanceDetailQueryRequestVO vo, String accEntity, String bankAccount) throws Exception;

    /**
     * 汇入汇款提交数据暂存，子表明细查询
     * @param param
     * @return
     * @throws Exception
     */
    CtmJSONObject inwardRemittance_b(CtmJSONObject param) throws Exception;

    /**
     * 汇入汇款列表查询，递归查询下一页
     * @param requestVO
     * @param records
     * @return
     * @throws Exception
     */
    CtmJSONArray doInwardRemittanceListQuery(InwardRemittanceListQueryRequestVO requestVO, CtmJSONArray records, int times) throws Exception;

    /**
     * 汇入汇款数据批量入库
     * @param records
     * @param accentity
     * @param bankaccount
     * @throws Exception
     */
    void insertInwardRemittance(CtmJSONArray records, String accentity, String bankaccount) throws Exception;

}
