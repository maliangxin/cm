package com.yonyoucloud.fi.cmp.electronicstatementconfirm.service;

import com.yonyou.yonbip.ctm.json.CtmJSONObject;

import javax.servlet.http.HttpServletResponse;
import java.util.Map;

/**
 * @InterfaceName ElectronicStatementConfirmService
 * @Description 电子对账单查询
 * @Author tongyd
 * @Date 2023/8/8 13:42
 * @Version 1.0
 **/
public interface ElectronicStatementConfirmService {

    /*
     *@Description 账户电子对账单查询
     *@Date 2019/9/18 14:36
     *@Param [params]
     *@Return CtmJSONObject
     **/
    CtmJSONObject queryElecStatement(CtmJSONObject params) throws Exception;

    /*
     *@Description 不插入ukey 账户电子对账单查询
     *@Date 2021/5/8 14:36
     *@Param [params]
     *@Return CtmJSONObject
     **/
    CtmJSONObject queryElecStatementUnNeedUkey(CtmJSONObject params) throws Exception;

    /*
     *@Description 账户电子对账单确认下载
     *@Date 2019/9/18 14:36
     **/
    CtmJSONObject downloadElecStatementFile(CtmJSONObject params, HttpServletResponse response) throws Exception;

    /*
     *@Description 账户电子对账单确认下载
     *@Date 2019/9/18 14:36
     **/
    void downloadElecStatementFileBatch(CtmJSONObject params, HttpServletResponse response) throws Exception;

    CtmJSONObject getIdByFilename(CtmJSONObject param, HttpServletResponse response) throws Exception;

    /*
     *@Description 电子对账单上传附件关联单据id
     *@Date 2024年10月12日17:47:22
     **/
    CtmJSONObject receiptAssociationFileId(CtmJSONObject params, HttpServletResponse response) throws Exception;

    CtmJSONObject receiptPreviewFile(CtmJSONObject param, HttpServletResponse response) throws Exception;

    Map<String, Object> scheduleQueryElecStatement(CtmJSONObject params) throws Exception;

    Map<String, Object> scheduleStatementFileDownload(CtmJSONObject params, HttpServletResponse response) throws Exception;

}
