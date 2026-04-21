package com.yonyoucloud.fi.cmp.bankreceipt.service;

import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.iuap.fileservice.sdk.module.CooperationFileDataService;
import com.yonyou.iuap.fileservice.sdk.module.pojo.FileInfoMoveRequest;
import com.yonyou.iuap.fileservice.sdk.module.pojo.response.CommonResponse;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.bankelectronicreceipt.BankElectronicReceipt;
import com.yonyoucloud.fi.cmp.bankreceipt.dto.BankEleReceiptDTO;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.orm.base.EntityStatus;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;


/**
 * @Description
 * @Author hanll
 * @Date 2024/3/21-10:09
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class BankReceiptHandleDataServiceImpl implements BankReceiptHandleDataService{

    private final CooperationFileDataService cooperationFileDataService;

    private static final String FILE_PATH = "iuap-apcom-file-public/yonbip-fi-ctmcmp/";
    private static final String PRIVATE_FILE_PATH = "iuap-apcom-file-private/yonbip-fi-ctmcmp/";
    private static final String CZFW = "czfw/";
    private static final String EXTENDSS = "extendss";

    private static final String SUCCESS = "success";

    private static final String FAILED = "failed";

    private static final String PUBTS = "pubts";

    private static final String BUSINESS_ID = "businessId";

    private String filePath;


    /**
     * 处理文件id
     *
     * @param bankelereceipt
     * @return
     */
    @Override
    public String handleExtendss(Map<String, Object> bankelereceipt) throws Exception {
        String extendss = (String) bankelereceipt.get(EXTENDSS);
        // 新的fileid直接返回
        if (!extendss.startsWith(CZFW)) {
            return extendss;
        }
        // 处理老的fileid
        String businessType = ICmpConstant.APPCODE;
        String businessId = bankelereceipt.get(ICmpConstant.ID).toString();
        Long fileSize = bankelereceipt.get(ICmpConstant.FILESIZE) == null ? null : Long.parseLong(bankelereceipt.get(ICmpConstant.FILESIZE).toString());
        List<FileInfoMoveRequest> requests = getFileInfoMoveRequests(businessType, businessId, extendss, fileSize);
        log.error("doCopyFileData parma:{}", CtmJSONObject.toJSONString(requests));
        CommonResponse commonResponse = cooperationFileDataService.doCopyFileData(requests);
        log.error("doCopyFileData result:{}", CtmJSONObject.toJSONString(commonResponse));
        if (!CommonResponse.SUCCESS.equals(commonResponse.getCode())) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102268"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A050800D7", "调用协同服务出现异常:%s") /* "调用协同服务出现异常:%s" */, commonResponse.getMessage()));
        }
        Object retData = commonResponse.getData();
        if (retData == null) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102269"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A050800D6", "调用协同服务出现异常返回数据为空") /* "调用协同服务出现异常返回数据为空" */);
        }
        CtmJSONObject successMap = CtmJSONObject.toJSON(retData);
        List<Object> successList = (List<Object>)successMap.get(SUCCESS);
        if (CollectionUtils.isNotEmpty(successList)) {
            Object success = successList.get(0);
            CtmJSONObject successData = CtmJSONObject.toJSON(success);
            extendss= successData.getString(ICmpConstant.ID);
            // 更新数据库
//            BankElectronicReceipt bankElectronicReceipt = new BankElectronicReceipt();
//            bankElectronicReceipt.setExtendss(extendss);
//            bankElectronicReceipt.setId(Long.parseLong(businessId));
//            bankElectronicReceipt.setFilename(successData.getString(ICmpConstant.NAME));
//            bankElectronicReceipt.setPubts((Date)bankelereceipt.get(PUBTS));
//            bankElectronicReceipt.setEntityStatus(EntityStatus.Update);
//            MetaDaoHelper.update(BankElectronicReceipt.ENTITY_NAME, bankElectronicReceipt);
        }
        return extendss;
    }


    /**
     * 构建调用文件服务的参数
     * @param businessType 文件隶属应用标识
     * @param businessId 文件绑定业务标识
     * @param extendss 文件id
     * @return
     */
    private List<FileInfoMoveRequest> getFileInfoMoveRequests(String businessType, String businessId, String extendss, Long fileSize) {
        String tenantId = InvocationInfoProxy.getTenantid();
        String userId = AppContext.getUserId().toString();
        fileSize = fileSize == null ? 1048576L : fileSize;
        FileInfoMoveRequest moveRequest = new FileInfoMoveRequest();
        moveRequest.setBusinessType(businessType);
        moveRequest.setBusinessId(businessId);
        moveRequest.setFileSize(fileSize);
        moveRequest.setUserId(userId);
        moveRequest.setTenantId(tenantId);
        String[] pathAndNameArray = extendss.split(ICmpConstant.APPCODE + "/");
        moveRequest.setName(pathAndNameArray[1]);
        moveRequest.setPath(this.filePath == null? FILE_PATH : this.filePath + extendss);
        List<FileInfoMoveRequest> requests = new ArrayList<>();
        requests.add(moveRequest);
        return requests;
    }

    /**
     * 批量处理文件id
     * @param bankElereceiptList 银行交易回单集合
     * @return
     * @throws Exception
     */
    @Override
    public List<Map<String, Object>> handleBatchExtendss(List<Map<String, Object>> bankElereceiptList) throws Exception {
        if (CollectionUtils.isEmpty(bankElereceiptList)) {
            return bankElereceiptList;
        }
        List<FileInfoMoveRequest> requestList = constructRequestList(bankElereceiptList);
        if (CollectionUtils.isEmpty(requestList)) {
            return bankElereceiptList;
        }
        log.error("doCopyFileDataBatch param:{}", CtmJSONObject.toJSONString(requestList));
        CommonResponse commonResponse = cooperationFileDataService.doCopyFileData(requestList);
        log.error("doCopyFileDataBatch result:{}", CtmJSONObject.toJSONString(commonResponse));
        if (!CommonResponse.SUCCESS.equals(commonResponse.getCode())) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102268"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A050800D7", "调用协同服务出现异常:%s") /* "调用协同服务出现异常:%s" */, commonResponse.getMessage()));
        }
        Object retData = commonResponse.getData();
        if (retData == null) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102269"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A050800D6", "调用协同服务出现异常返回数据为空") /* "调用协同服务出现异常返回数据为空" */);
        }
        CtmJSONObject retDataMap = CtmJSONObject.toJSON(retData);
        // 处理失败的集合
        handleFailedList(retDataMap);
        // 处理成功的集合
        Map<String, BankEleReceiptDTO> bankEleReMap = handleSuccessList(retDataMap);
        List<BankElectronicReceipt> updateList = new ArrayList<>();
        BankElectronicReceipt receipt;
        String receiptId;
        for (Map<String, Object> bankReMap : bankElereceiptList) {
            receiptId = bankReMap.get(ICmpConstant.ID).toString();
            if (bankEleReMap.get(receiptId) == null) {
                continue;
            }
            receipt = new BankElectronicReceipt();
            receipt.setId(Long.parseLong(receiptId));
            receipt.setPubts((Date)bankReMap.get(PUBTS));
            receipt.setExtendss(bankEleReMap.get(receiptId).getExtendss());
            receipt.setFilename(bankEleReMap.get(receiptId).getFileName());
            receipt.setEntityStatus(EntityStatus.Update);
            updateList.add(receipt);
            // 备份extendss
            bankReMap.put("remark01", bankReMap.get(EXTENDSS) == null ? null : bankReMap.get(EXTENDSS).toString());
            // 更新文件id
            bankReMap.put(EXTENDSS, receipt.getExtendss());
            // 更新文件名
            bankReMap.put(ICmpConstant.FILENAME, receipt.getFilename());
        }
        // MetaDaoHelper.update(BankElectronicReceipt.ENTITY_NAME, updateList);
        return bankElereceiptList;
    }

    /**
     * 处理成功的集合
     * @param retDataMap
     * @return
     */
    private Map<String, BankEleReceiptDTO> handleSuccessList(CtmJSONObject retDataMap) {
        List<Object> successList = (List<Object>) retDataMap.get(SUCCESS);
        List<BankEleReceiptDTO> retDtoList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(successList)) {
            for (Object success : successList) {
                CtmJSONObject successData = CtmJSONObject.toJSON(success);
                retDtoList.add(new BankEleReceiptDTO(successData.getString(ICmpConstant.ID), successData.getString(BUSINESS_ID),
                        successData.getString(ICmpConstant.NAME)));
            }
        }
        return retDtoList.stream().collect(Collectors.toMap(BankEleReceiptDTO::getBusinessId, Function.identity()));
    }

    /**
     * 处理失败的集合
     * @param retDataMap
     */
    private void handleFailedList(CtmJSONObject retDataMap) {
        List<Object> failList = (List<Object>) retDataMap.get(FAILED);
        if (CollectionUtils.isNotEmpty(failList)) {
            log.error("doCopyFileDataBatch failed result:{}", CtmJSONObject.toJSONString(failList));
            List<String> failBusinessIds = new ArrayList<>();
            for (Object failed : failList) {
                failBusinessIds.add(CtmJSONObject.toJSON(failed).getString(BUSINESS_ID));
            }
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102270"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A050800D5", "调用协同服务出现返回异常的数据:%s") /* "调用协同服务出现返回异常的数据:%s" */, CtmJSONObject.toJSONString(failBusinessIds)));
        }
    }

    /**
     * 构建请求参数列表
     * @param bankElereceiptList
     * @return
     */
    private List<FileInfoMoveRequest> constructRequestList(List<Map<String, Object>> bankElereceiptList) {
        List<BankEleReceiptDTO> needHandleBe = new ArrayList<>();
        String extendss;
        String businessId;
        String businessType = ICmpConstant.APPCODE;
        Long fileSize;
        for (Map<String, Object> bankEleReceipt : bankElereceiptList) {
            if (bankEleReceipt.get(EXTENDSS) == null) {
                continue;
            }
            extendss = (String)bankEleReceipt.get(EXTENDSS);
            if (!extendss.startsWith(CZFW)) {
                continue;
            }
            businessId = bankEleReceipt.get(ICmpConstant.ID).toString();
            fileSize = bankEleReceipt.get(ICmpConstant.FILESIZE) == null ? null : Long.parseLong(bankEleReceipt.get(ICmpConstant.FILESIZE).toString());
            needHandleBe.add(new BankEleReceiptDTO(extendss, businessId, fileSize));
        }

        List<FileInfoMoveRequest> requestList = new ArrayList<>();
        if (CollectionUtils.isEmpty(needHandleBe)) {
            return requestList;
        }
        for (BankEleReceiptDTO dto : needHandleBe) {
            requestList.addAll(getFileInfoMoveRequests(businessType, dto.getBusinessId(), dto.getExtendss(), dto.getFileSize()));
        }
        return requestList;
    }


    /**
     * 获取文件Id集合
     * @param bankElereceiptList 电子回单文件
     * @return 协同文件id集合
     * @throws Exception
     */
    @Override
    public List<String> getFileIds(List<Map<String, Object>> bankElereceiptList) throws Exception {
        List<String> fileIds = new ArrayList<>();
        for (Map<String, Object> bankEleReceipt : bankElereceiptList) {
            fileIds.add((String)bankEleReceipt.get(EXTENDSS));
        }
        return fileIds;
    }

    /**
     * 处理文件Id
     *
     * @param extendss   老的文件ID
     * @param businessId 业务主键id
     * @return
     * @throws Exception
     */
    @Override
    public String handleExtendss(String extendss, Long businessId) throws Exception {
        Map<String,Object> map = new HashMap<>();
        map.put(EXTENDSS, extendss);
        map.put(ICmpConstant.ID, businessId);
        return handleExtendss(map);
    }

    /**
     * 设置公有桶还是私有桶标识
     * @param flag
     */
    @Override
    public void setPrivateOrPublicFlag(String flag) {
        if ("private".equals(flag)) {
            this.filePath = PRIVATE_FILE_PATH;
        } else if ("public".equals(flag)) {
            this.filePath = FILE_PATH;
        }
    }
}
