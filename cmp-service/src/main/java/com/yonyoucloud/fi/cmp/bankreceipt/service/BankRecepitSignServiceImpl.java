package com.yonyoucloud.fi.cmp.bankreceipt.service;

import com.yonyou.iuap.fileservice.sdk.module.CooperationFileService;
import com.yonyou.iuap.fileservice.sdk.module.pojo.CooperationFileInfo;
import com.yonyou.iuap.ucf.common.i18n.InternationalUtils;
import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVO;
import com.yonyou.ucf.basedoc.model.rpcparams.EnterpriseParams;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.yonbip.ctm.bankconnection.BankConnectionAdapterContext;
import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.logger.business.CTMCMPBusinessLogService;

import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.cmp.bankaccountsetting.BankAccountSettingService;
import com.yonyoucloud.fi.cmp.bankelectronicreceipt.BankElectronicReceipt;
import com.yonyoucloud.fi.cmp.cmpentity.SignStatus;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import com.yonyoucloud.fi.cmp.oss.OSSPoolClient;
import com.yonyoucloud.fi.cmp.util.DateUtils;
import com.yonyoucloud.fi.cmp.util.HttpsUtils;
import com.yonyoucloud.fi.cmp.util.YQLUtils;
import com.yonyoucloud.fi.cmp.util.file.CooperationFileUtilService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.message.BasicNameValuePair;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QuerySchema;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * desc:银行交易回单统一校验
 * author:wangqiangac
 * date:2023/8/15 18:38
 */
@Service
//@Transactional(rollbackFor = RuntimeException.class)
@Transactional(propagation= Propagation.REQUIRES_NEW)
@Slf4j
@RequiredArgsConstructor
public class BankRecepitSignServiceImpl implements BankRecepitSignService{
    public static String SERVICE_RESP_CODE = "000000";  //服务响应码   “000000”（6个0）代表成功，如果返回“000000”，则service_status的值一定是“00”
    private final BaseRefRpcService baseRefRpcService;// 基础档案查询
    private final BankAccountSettingService bankAccountSettingService;
    private final BankConnectionAdapterContext bankConnectionAdapterContext;// 签名验签
    private final CTMCMPBusinessLogService ctmcmpBusinessLogService;
    private final CooperationFileService cooperationFileService;
    private final OSSPoolClient ossPoolClient;// oss客户端
    private AtomicInteger cardinalNumber = new AtomicInteger(0);
    private final String TRANSCODE = "48T25";//4.4.13 统一校验(电子凭证会计数据标准):48T25
    @Resource
    CooperationFileUtilService cooperationFileUtilService;
    @Override
    public String bankRecepitSign(BankElectronicReceipt bankElectronicReceipt) throws Exception {
        String enterpriseBankAccount = bankElectronicReceipt.getEnterpriseBankAccount();
        List<String> idList = new ArrayList<>();
        idList.add(enterpriseBankAccount);
        EnterpriseParams params = new EnterpriseParams();
        params.setIdList(idList);
        List<EnterpriseBankAcctVO> enterpriseBankAcctVOs = baseRefRpcService.queryEnterpriseBankAcctByCondition(params);
        if(enterpriseBankAcctVOs == null || enterpriseBankAcctVOs.size()==0){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100671"),InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18E91D6804400017", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18E91D6804400017", "未获取企业银行账户！") /* "未获取企业银行账户！" */) /* "未获取企业银行账户！" */);
        }
        EnterpriseBankAcctVO bankAccount = enterpriseBankAcctVOs.get(0);
        if (!queryOpenFlag(bankAccount.getId())) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100672"),InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18E91D6804400018", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18E91D6804400018", "企业银行账户未开通银企联！") /* "企业银行账户未开通银企联！" */) /* "企业银行账户未开通银企联！" */);
        }
        String customNo = bankAccountSettingService.getCustomNoByBankAccountId(bankAccount.getId());
        if(StringUtils.isEmpty(customNo)){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100673"),InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18E91D6804400019", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18E91D6804400019", "企业银行账户未为配置客户号！") /* "企业银行账户未为配置客户号！" */) /* "企业银行账户未为配置客户号！" */);
        }
        String requestseqno = buildRequestSeqNo(customNo);
        CtmJSONObject requestHead = buildRequloadestHead(TRANSCODE,
                AppContext.getUserId().toString(),
                customNo,
                requestseqno,
                null);
        QuerySchema querySchema = QuerySchema.create().addSelect(" * ");
        querySchema.appendQueryCondition(QueryCondition.name("id").eq(bankElectronicReceipt.getId().toString()));
        List<BankElectronicReceipt> BankElectronicReceipts = MetaDaoHelper.query(BankElectronicReceipt.ENTITY_NAME, querySchema);
        CtmJSONObject requestBody = buildRequestBody(BankElectronicReceipts.get(0), bankAccount);
        CtmJSONObject queryMsg = new CtmJSONObject();
        queryMsg.put("request_head", requestHead);
        queryMsg.put("request_body", requestBody);
        String signMsg = bankConnectionAdapterContext.chanPaySignMessage(queryMsg.toString());
        List<BasicNameValuePair> requestData = new ArrayList<>();
        requestData.add(new BasicNameValuePair("reqData", queryMsg.toString()));
        requestData.add(new BasicNameValuePair("reqSignData", signMsg));
        log.error("======银行交易回单统一校验请求参数=======>"+queryMsg.toString());
        CtmJSONObject result = HttpsUtils.doHttpsPostNew(TRANSCODE, requestData, bankConnectionAdapterContext.getChanPayUri());
        log.error("=======银行交易回单统一校验result======>"+CtmJSONObject.toJSONString(result));
        CtmJSONObject logData = new CtmJSONObject();
        logData.put("uri",bankConnectionAdapterContext.getChanPayUri());
        logData.put("params", "acctName:"+bankAccount.getAcctName()+", account:"+bankAccount.getAccount());
        logData.put("queryMsg", queryMsg);
        logData.put("result", result);
        ctmcmpBusinessLogService.saveBusinessLog(logData, customNo, customNo, IServicecodeConstant.BANKRECEIPTMATCH,com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400717", "银行交易回单") /* "银行交易回单" */, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400716", "银行交易回单统一校验") /* "银行交易回单统一校验" */);
        log.error("=======银行交易回单统一校验业务日志======>"+CtmJSONObject.toJSONString(logData));
        String msg = "";
        if (result.getInteger("code") == 1) {
            CtmJSONObject responseHead = result.getJSONObject("data").getJSONObject("response_head");
            String serviceStatus = responseHead.getString("service_status");
            //String serviceRespDesc = responseHead.getString("service_resp_desc");
            String  service_resp_code= responseHead.getString("service_resp_code");
            if (("00").equals(serviceStatus) && SERVICE_RESP_CODE.equals(service_resp_code)) {
                CtmJSONObject responseBody = result.getJSONObject("data").getJSONObject("response_body");
                CtmJSONObject record = responseBody.getJSONObject("record");
                String check_flag = record.getString("check_flag");
//                bankElectronicReceipt.setCheckmatch();
                bankElectronicReceipt.setSignStatus(SignStatus.find(Integer.parseInt(check_flag)));
                bankElectronicReceipt.setEntityStatus(EntityStatus.Update);
                MetaDaoHelper.update(BankElectronicReceipt.ENTITY_NAME,bankElectronicReceipt);
            }else{
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100674"), YQLUtils.getYQLErrorMsq(responseHead));
            }
        }else{
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100675"),(String)result.get("message"));
        }
        return msg;
    }

    @NotNull
    private CtmJSONObject buildRequestBody(HashMap<String,Object> bankElectronicReceipt, EnterpriseBankAcctVO bankAccount) throws Exception {
        CtmJSONObject requestBody = new CtmJSONObject();
        CtmJSONArray records = new CtmJSONArray();
        CtmJSONObject record = new CtmJSONObject();
        record.put("unique_no", bankElectronicReceipt.get("uniqueCode")); //uniqueCode
        record.put("acct_no", bankAccount.getAccount());//account
        record.put("bill_no", bankElectronicReceipt.get("receiptno"));//回单编号 receiptno
        record.put("bill_extend", bankElectronicReceipt.get("receiptno"));//回单扩展信息  //bill_extend
        record.put("file_type","1");//文件类型	file_type	varchar(2)	M	1 回单  2 对账单
        //record.put("file_name","");//文件名	file_name	varchar(200)	O 非必填的。。校验的时候如果有传会以传过来的文件名生成一个临时文件
        //从文件服务器下载
        String extendss =  (String) bankElectronicReceipt.get("extendss");
        byte[] bytes = null;
        if(!StringUtils.isEmpty(extendss)){
            //bytes = extendss.contains("/") ? ossPoolClient.download(extendss) : this.queryBytesbyFileid(extendss);
            bytes = extendss.contains("/") ? ossPoolClient.download(extendss) : cooperationFileUtilService.queryBytesbyFileid(extendss);
            if(bytes != null){
                Base64.Encoder encoder = Base64.getEncoder();
                String file_content = encoder.encodeToString(bytes);
                //String file_content = new String(bytes);
                record.put("file_content",file_content);//文件内容	file_content	varchar(1048576)	O	文件内容， Base64编码
            }
        }
        records.add(record);
        requestBody.put("record",record);
        return requestBody;
    }

    /**
     * 通过文件id获取字节流
     * 调用报错，新版的需要从filePath里把域名去掉
     * @param fileId
     * @return
     * @deprecated
     */
  @Deprecated
    public byte[] queryBytesbyFileid(String fileId) {
        if (StringUtils.isNotBlank(fileId)) {
            CooperationFileInfo fileInfo = cooperationFileService.queryFileInfo(fileId);
            if (fileInfo != null) {
                byte[] bytes = cooperationFileService.getBytesByfilepath(fileInfo.getFilePath());
                return bytes;
            }
        }
        return null;
    }


    private CtmJSONObject buildRequloadestHead(String transCode, String oper, String customNo, String requestseqno, String signature) {
        CtmJSONObject requestHead = new CtmJSONObject();
        requestHead.put("version", "1.0.0");
        requestHead.put("request_seq_no", requestseqno);
        requestHead.put("cust_no", customNo);
        requestHead.put("cust_chnl", bankConnectionAdapterContext.getChanPayCustomChanel());
        LocalDateTime dateTime = LocalDateTime.now();
        requestHead.put("request_date", DateTimeFormatter.ofPattern(DateUtils.YYYYMMDD).format(dateTime));
        requestHead.put("request_time", DateTimeFormatter.ofPattern(DateUtils.HHMMSS).format(dateTime));
        requestHead.put("oper_sign", signature);
        requestHead.put("tran_code", transCode);
        requestHead.put("oper", oper);
        return requestHead;
    }


    /**
     * 构建请求流水号
     * @param customNo
     * @return
     */
    private String buildRequestSeqNo(String customNo) {
        StringBuilder tranSeqNo = new StringBuilder("R");
        tranSeqNo.append(customNo);
        tranSeqNo.append("0000");
        tranSeqNo.append(DateTimeFormatter.ofPattern(DateUtils.MILLISECOND_PATTERN).format(LocalDateTime.now()));
        tranSeqNo.append(YQLUtils.getSerialNumberNoCAS(cardinalNumber));
        return tranSeqNo.toString();
    }

}
