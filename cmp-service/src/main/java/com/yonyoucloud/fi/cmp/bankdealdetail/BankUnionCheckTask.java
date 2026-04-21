package com.yonyoucloud.fi.cmp.bankdealdetail;

import com.yonyou.diwork.ott.exexutors.RobotExecutors;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.dao.sql.SqlHelper;
import com.yonyou.yonbip.ctm.accelerator.threadpool.CtmThreadPoolExecutor;
import com.yonyou.yonbip.ctm.bankconnection.BankConnectionAdapterContext;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;

import com.yonyoucloud.fi.cmp.bankaccountsetting.BankAccountSetting;
import com.yonyoucloud.fi.cmp.bankreceipt.dto.TenantDTO;
import com.yonyoucloud.fi.cmp.constant.ITransCodeConstant;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.receipt.BankLinkParam;
import com.yonyoucloud.fi.cmp.util.DateUtils;
import com.yonyoucloud.fi.cmp.util.HttpsUtils;
import com.yonyoucloud.fi.cmp.util.StringUtils;
import com.yonyoucloud.fi.cmp.util.YQLUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.message.BasicNameValuePair;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * 牧原到账通知 核对任务
 *
 * @author liuwtr
 */
@Component
@Slf4j
public class BankUnionCheckTask {

    private static final String QUERY_ALL_TENANT = "com.yonyoucloud.fi.cmp.mapper.TenantMapper.queryAllTenant";

    @Autowired
    private BankConnectionAdapterContext bankConnectionAdapterContext;

    private AtomicInteger cardinalNumber = new AtomicInteger();

    /**
     * 1,查询所有客户号
     * 2，根据客户号发送重试申请
     */
    @Scheduled(fixedDelay = 10 * 60 * 1000)
    public void bankDealDetailCheck(){

        String checkFlag = AppContext.getEnvConfig("cmp.bankDealDetailCheck",null);
        if(StringUtils.isEmpty(checkFlag)){
            return;
        }
        log.error("客户号发送重试申请");
        // 查询所有的租户信息
        List<TenantDTO> tenantDTOList = SqlHelper.selectList(QUERY_ALL_TENANT);
        // 通过机器人异步执行构建租户上线文
        CtmThreadPoolExecutor ctmThreadPoolExecutor = AppContext.getBean(CtmThreadPoolExecutor.class);
        for (TenantDTO dto : tenantDTOList) {
            try {
                RobotExecutors.runAs(dto.getYtenantId(), new Callable() {
                    @Override
                    public Object call() throws Exception {

                        String channel = bankConnectionAdapterContext.getChanPayCustomChanel();
                        try {
                            QuerySchema schema = QuerySchema.create().addSelect("*");
                            QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
                            conditionGroup.appendCondition(QueryCondition.name("customNo").is_not_null());
                            schema.addCondition(conditionGroup);
                            List<Map<String, Object>> settings = MetaDaoHelper.query(BankAccountSetting.ENTITY_NAME, schema);
                            Set<String> customNos = new HashSet<String>();
                            if (!settings.isEmpty()) {
                                for(Map<String, Object> setting : settings){
                                    customNos.add(setting.get("customNo").toString());
                                }
                            }else {
                                log.error("====重试接口：当前无客户号。。。。。。。。。。。。。===");
                            }
                            log.error("开始重试。。。。。。。。。。。。。");
                            for(String customNo : customNos) {
                                BankLinkParam params = new BankLinkParam();
                                params.setCustomNo(customNo);
                                params.setSignature("");
                                params.setChannel(channel);
                                CtmJSONObject queryMsg = buildQueryTransactionDetailMsg(params);
                                String signMsg = bankConnectionAdapterContext.chanPaySignMessage(queryMsg.toString());
                                List<BasicNameValuePair> requestData = new ArrayList<>();
                                requestData.add(new BasicNameValuePair("reqData", queryMsg.toString()));
                                requestData.add(new BasicNameValuePair("reqSignData", signMsg));
                                log.error("=======================重试请求参数=========================>{}", CtmJSONObject.toJSONString(queryMsg));
                                HttpsUtils.doHttpsPostNew(ITransCodeConstant.RECEIVE_ACCOUNT_TRANSACTION_DETAIL_CHECK, requestData, bankConnectionAdapterContext.getChanPayUri());
                            }
                            log.error("重试结束。。。。。。。。。。。。。");

                        }catch (Exception e){
                            log.error("开始验重任务失败。。。。。。。。。。。。。",e);
                        }
                        return null;
                    }
                },ctmThreadPoolExecutor.getThreadPoolExecutor());
            } catch (Exception e) {
                log.error("客户号发送重试申请：{}",e.getMessage(),e);
            }
        }
    }

    /**
     * @return com.yonyou.yonbip.ctm.json.CtmJSONObject
     * @Author jiangpengk
     * @Description 构建账户交易明细重试报文
     * @Param
     **/
    private CtmJSONObject buildQueryTransactionDetailMsg(BankLinkParam params) throws Exception {
        CtmJSONObject requestHead = new CtmJSONObject();
        requestHead.put("version", "1.0.0");
        requestHead.put("request_seq_no", buildRequestSeqNo(params.getCustomNo()));
        requestHead.put("cust_no", params.getCustomNo());
        requestHead.put("cust_chnl", params.getChannel());
        LocalDateTime dateTime = LocalDateTime.now();
        requestHead.put("request_date", DateTimeFormatter.ofPattern(DateUtils.YYYYMMDD).format(dateTime));
        requestHead.put("request_time", DateTimeFormatter.ofPattern(DateUtils.HHMMSS).format(dateTime));
//        requestHead.put("oper",  params.getOperator());
        requestHead.put("oper_sign", params.getSignature());
        requestHead.put("tran_code", ITransCodeConstant.RECEIVE_ACCOUNT_TRANSACTION_DETAIL_CHECK);

        CtmJSONObject requestBody = new CtmJSONObject();
        CtmJSONObject queryMsg = new CtmJSONObject();
        queryMsg.put("request_head", requestHead);
        queryMsg.put("request_body", requestBody);
        return queryMsg;
    }

    /*
     * @Author tongyd
     * @Description 构建请求流水号
     * @Date 2019/9/12
     * @Param [customNo]
     * @return java.lang.String
     **/
    public String buildRequestSeqNo(String customNo) {
        StringBuilder tranSeqNo = new StringBuilder("R");
        tranSeqNo.append(customNo);
        tranSeqNo.append("0000");
        tranSeqNo.append(DateTimeFormatter.ofPattern(DateUtils.MILLISECOND_PATTERN).format(LocalDateTime.now()));
        tranSeqNo.append(YQLUtils.getSerialNumberNoCAS(cardinalNumber));
        return tranSeqNo.toString();
    }

}
