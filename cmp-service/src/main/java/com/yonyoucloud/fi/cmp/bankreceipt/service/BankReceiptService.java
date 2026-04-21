package com.yonyoucloud.fi.cmp.bankreceipt.service;


import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVO;
import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.bankelectronicreceipt.BankElectronicReceipt;
import com.yonyoucloud.fi.cmp.util.threadpool.ThreadResult;
import com.yonyoucloud.fi.cmp.vo.BankTranBatchAddVO;
import com.yonyoucloud.fi.cmp.vo.BankTranBatchUpdateVO;

import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

/**
 * @InterfaceName BankReceiptService
 * @Description 银行交易回单接口
 * @Author tongyd
 * @Date 2019/4/26 15:31
 * @Version 1.0
 **/
public interface BankReceiptService {

    /*
     *@Description 不插入ukey 账户电子回单查询 yangjn
     *@Date 2021/5/8 14:36
     *@Param [params]
     *@Return CtmJSONObject
     **/
    CtmJSONObject queryAccountReceiptDetailUnNeedUkey(CtmJSONObject params) throws Exception;

    /*
     *@Description 账户电子回单下载
     *@Date 2019/9/18 14:36
     **/
    String downloadAccountReceiptDetailUrl(CtmJSONObject params, HttpServletResponse response) throws Exception;


    /**
     * 下载回单文件信息
     * @param params
     * @param response
     * @throws Exception
     */
    CtmJSONObject receiptDownForSpecial(CtmJSONObject params, HttpServletResponse response) throws Exception;

    /**
     * 批量下载回单文件信息
     * @param params
     * @param response
     * @throws Exception
     */
    CtmJSONObject receiptDownBatchForSpecial(CtmJSONObject params, HttpServletResponse response) throws Exception;

    /*
     *@Description 账户电子回单下载定时任务
     *@Date 2019/9/18 14:36
     **/
    void downloadAccountReceiptTask(Map<String, Object> param,Boolean cooperationFileService) throws Exception;

    /*
     *@Description 账户电子回单下载
     *@Date 2019/9/18 14:36
     **/
    byte[] downloadAccountReceipt(String extendss) throws Exception;

    /**
     * 获取EnterpriseBankAcctVO
     * @param bankelereceipt
     * @return
     * @throws Exception
     */
    EnterpriseBankAcctVO getEnterpriseBankAcctVO(Map<String, Object> bankelereceipt) throws Exception;
    
    /*
     *@Description 导入电子回单文件
     *@Date 2019/9/18 14:36
     **/
    void receiptUploadFile(CtmJSONObject params, HttpServletResponse response) throws Exception;

    /*
     *@Description 根据电子回单文件名称返回银行交易回单主键
     *@Date 2019/9/18 14:36
     **/
    CtmJSONObject getReceiptIdByFilename(CtmJSONObject params, HttpServletResponse response) throws Exception;

    /*
     *@Description 银行交易回单关联文件id
     *@Date 2019/9/18 14:36
     **/
    CtmJSONObject receiptAssociationFileId(CtmJSONObject params, HttpServletResponse response) throws Exception;

    /*
     *@Description 预览文件
     *@Date 2019/9/18 14:36
     **/
    CtmJSONObject receiptPreviewFile(CtmJSONObject params, HttpServletResponse response) throws Exception;

    /*
     *@Description 批量预览文件
     *@Date 2019/9/18 14:36
     **/
    CtmJSONArray batchreceiptPreviewFile(CtmJSONObject param , HttpServletResponse response) throws Exception;

    /*
     *@Description 银行回单和银行交易明细关联自动任务
     *@param params
     *@Date 2023/05/09 14:36
     **/
    CtmJSONObject relateBankReceiptDetail(CtmJSONObject params) throws Exception;


    /*
     *@Description 银行回单和银行交易明细关联文件url补偿任务
     *@param params
     *@Date 2023/05/09 14:36
     **/
    CtmJSONObject urlCompensation(CtmJSONObject params) throws Exception;


    /*
     *@Description 银行回单查询统计交易金额和数量
     *@Date 2023/06/19 14:36
     **/
    CtmJSONObject queryElectronicReceiptStatistics(CtmJSONObject params, HttpServletResponse response) throws Exception;

    /*
     * @Author tongyd
     * @Description 获取电子回单实体
     * @Date 2019/9/12
     * @Param [bankAccountId]
     * @return java.util.Map<java.lang.String,java.lang.Object>
     **/
    public Map<String, Object> getBankAccountSetting(Object bankAccountId) throws Exception;


    public int insertReceiptDetail(Map<String, Object> enterpriseInfo, CtmJSONObject responseBody, String uid) throws Exception;


    /**
     * * 获取内部账户交易回单
     *
     * @param params
     * @param accountIdList
     * @param uid
     * @return
     * @throws Exception
     */
    public int stctInsertReceiptByinnerAccounts(CtmJSONObject params, List<String> accountIdList, String uid) throws Exception;

    /**
     * 下载文件 并更新状态
     * @throws Exception
     */
    public ThreadResult downloadReceiptFileByAccount(List<List<BankElectronicReceipt>> excuteReceiptGroupList, ExecutorService executorService) throws Exception;

    /**
     * 批量新增回单信息
     * @param bankTranBatchAddVOs 回单信息
     * @return 回单id列表
     */
    List<Object> batchInsertReceipt(List<BankTranBatchAddVO> bankTranBatchAddVOs) throws Exception;

    /**
     * 批量银行回单文件上传
     * @param bankTranBatchUpdateVOS 需更新的回单信息
     * @return 回单id
     */
    List<String> batchUpdateReceipt(List<BankTranBatchUpdateVO> bankTranBatchUpdateVOS) throws Exception;

    /**
     * 删除银行回单
     *
     * @param ids 需要删除的银行回单的ID列表
     * @throws Exception 当删除过程中发生异常时抛出
     */
    void deleteBankReceipts(List<String> ids) throws Exception;
}