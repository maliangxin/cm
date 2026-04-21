package com.yonyoucloud.fi.cmp.event.listerEvent;

import com.yonyou.diwork.exception.BusinessException;
import com.yonyou.epmp.balance.dto.BalancePushDto;
import com.yonyou.epmp.balance.service.BalancePushService;
import com.yonyou.epmp.util.CompressUtil;
import com.yonyou.iuap.event.model.BusinessEvent;
import com.yonyou.iuap.event.rpc.IEventReceiveService;
import com.yonyou.ucf.mdd.ext.option.model.vo.EventResponseVO;
import com.yonyou.workbench.util.JsonUtils;
import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.autoparam.service.AutoConfigService;
import com.yonyoucloud.fi.cmp.budget.*;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.event.vo.CmpBudgetEventBill;
import com.yonyoucloud.fi.cmp.fundpayment.FundPayment_b;
import com.yonyoucloud.fi.cmp.util.BudgetUtils;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.constants.CommonConstant;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 预算期初数据监听
 * sourceID: BUDGET
 * eventType: PUSH_BUDGET
 */
@Slf4j
@Service("budgetListener")
public class BudgetListener implements IEventReceiveService {

    @Autowired
    private CmpBudgetFundPaymentManagerService cmpBudgetFundPaymentManagerService;
    @Autowired
    private CmpBudgetSalarypayManagerService cmpBudgetSalarypayManagerService;
    @Autowired
    private CmpBudgetPaymarginManagerService cmpBudgetPaymarginManagerService;
    @Autowired
    private CmpBudgetReceivemarginManagerService cmpBudgetReceivemarginManagerService;
    @Autowired
    private CmpBudgetForeignpaymentManagerService cmpBudgetForeignpaymentManagerService;
    @Autowired
    private CmpBudgetTransferAccountManagerService cmpBudgetTransferAccountManagerService;
    @Autowired
    private CmpBudgetCurrencyExchangeManagerService cmpBudgetCurrencyExchangeManagerService;

    @Autowired
    private CmpBudgetManagerService cmpBudgetManagerService;
    @Autowired
    private AutoConfigService autoConfigService;
    @Autowired
    private BalancePushService balancePushService;
    // 分批发送数量值
    public static final int BATCH_SIZE = 1000;
    /**
     * 预算期初数据监听-预算规则启动/重启时调用
     *
     * @param businessEvent
     * @param queueName
     * @return
     * @throws BusinessException
     */
    @Override
    @SneakyThrows
    public String onEvent(BusinessEvent businessEvent, String queueName) throws BusinessException {
        log.error("预算期初数据监听,data:{},name:{}", CtmJSONObject.toJSONString(businessEvent), queueName);
        if (CommonConstant.MODULE.equals(queueName)) {
            CtmJSONArray jsonArray;
            try {
                jsonArray = CtmJSONArray.parseArray(businessEvent.getUserObject());
            } catch (Exception e) {
                // UserObject有可能为加密字符串，此类数据不做处理
                return "";
            }
            for (int i = 0; i < jsonArray.size(); i++) {
                CtmJSONObject param =jsonArray.getJSONObject(i);
                String sysCode = param.getString("sysCode");
                if (!BudgetUtils.SYSCODE.equals(sysCode)) {
                    return JsonUtils.toJsonString(EventResponseVO.success());
                }
                //租户id
                String ytenantId = param.getString("ytenantId");
                List<Map<String, Object>> list = new ArrayList<>();

                //规则重启启动唯一标识
                String ruleStartInfo = param.getString("ruleStartInfo");
                //消息唯一标识
                String uniqueKey = param.getString("uniqueKey");
                BalancePushDto balancePushDto = new BalancePushDto();
                //业务系统编码
                balancePushDto.setBusiSysCode(sysCode);
                //请求唯一标识
                balancePushDto.setRequestUniqueId(UUID.randomUUID().toString());
                //对应消息中的uniqueKey
                balancePushDto.setUniqueKey(uniqueKey);
                //对应消息中的ruleStartInfo
                balancePushDto.setRuleStartInfo(ruleStartInfo);
                //租户id
                balancePushDto.setYtenantId(ytenantId);
                //业务系统消费来源 非必填
                balancePushDto.setSourceBusiSysCode(null);

                // TODO 经需求讨论，默认推送期初数据，此处先注释掉，后续预算中台判断释放预算id后，再考虑放开期初开关
//                1 添加判断期初 历史数据推送
//                if(!cmpBudgetManagerService.isPushHistory()){
//                    balancePushDto.setBills(list);
//                    ResultResponse resultResponse = initBalancePushDto(balancePushDto);
//                    return JsonUtils.toJsonString(new EventResponseVO(true, InternationalUtils.getMessageWithDefault("UID:P_GRM-BE_18113A5204B801C8",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18D4A7320568000A","不推送历史数据") /* "不推送历史数据" */) /* "不推送历史数据" */));
//                }
                //业务单据数组
                CtmJSONArray billArray = CtmJSONArray.parseArray(CompressUtil.uncompress((String)param.get("bills")));
                List<FundPayment_b> fundPaymentsSum = new ArrayList<>();
                String ruleType = "pre";
                for (int j = 0; j < billArray.size(); j++) {
                    CmpBudgetEventBill budgetEventBill = CtmJSONObject.parseObject(billArray.getJSONObject(j).toString(), CmpBudgetEventBill.class);
                    log.error("BudgetListener budgetEventBill:{}",CtmJSONObject.toJSONString(budgetEventBill));
                    // 此处需要根据事件监听传过来的筛选条件，查询出对应子表数据并更新“占用状态”为已占用
                    //租户是否开通了预算执行控制模块
                    CtmJSONArray bills = null;
                    if (!cmpBudgetManagerService.isCanStart(budgetEventBill.getBillCode())) {
                        log.error("预算期初数据监听+不支持的单据类型");
                        continue;
                    }
                    if (StringUtils.equalsAny(budgetEventBill.getBillCode(), IBillNumConstant.SALARYPAY, IBillNumConstant.CMP_PAYMARGIN,
                            IBillNumConstant.CMP_RECEIVEMARGIN, IBillNumConstant.CMP_FOREIGNPAYMENT,
                            IBillNumConstant.CURRENCYEXCHANGE, IBillNumConstant.TRANSFERACCOUNT,IBillNumConstant.CMP_BATCHTRANSFERACCOUNT)) {
                        if (!autoConfigService.isPushHistory()) {
                            log.error("不推送历史数据");
                            continue;
                        }
                    }
                    if (IBillNumConstant.FUND_PAYMENT.equals(budgetEventBill.getBillCode())) {
                        // 根据条件筛选主子表信息，并拼接成Json
                        ruleType = budgetEventBill.getRuleType();
                        bills = cmpBudgetFundPaymentManagerService.queryFundPayment(budgetEventBill);
                    }  if (IBillNumConstant.FUND_COLLECTION.equals(budgetEventBill.getBillCode())) {
                        // 根据条件筛选主子表信息，并拼接成Json
                        ruleType = budgetEventBill.getRuleType();
                        bills = cmpBudgetFundPaymentManagerService.queryFundCollection(budgetEventBill);
                    } else if (IBillNumConstant.SALARYPAY.equals(budgetEventBill.getBillCode())) {

                        // 根据条件筛选主子表信息，并拼接成Json
                        ruleType = budgetEventBill.getRuleType();
                        bills = cmpBudgetSalarypayManagerService.querySalarypay(budgetEventBill);
                    } else if (IBillNumConstant.CMP_PAYMARGIN.equals(budgetEventBill.getBillCode())) {
                        // 根据条件筛选主子表信息，并拼接成Json
                        ruleType = budgetEventBill.getRuleType();
                        bills = cmpBudgetPaymarginManagerService.queryPaymargin(budgetEventBill);
                    } else if (IBillNumConstant.CMP_RECEIVEMARGIN.equals(budgetEventBill.getBillCode())) {
                        // 根据条件筛选主子表信息，并拼接成Json
                        ruleType = budgetEventBill.getRuleType();
                        bills = cmpBudgetReceivemarginManagerService.queryReceivemargin(budgetEventBill);
                    } else if (IBillNumConstant.CMP_FOREIGNPAYMENT.equals(budgetEventBill.getBillCode())) {
                        // 根据条件筛选主子表信息，并拼接成Json
                        ruleType = budgetEventBill.getRuleType();
                        bills = cmpBudgetForeignpaymentManagerService.queryBillByRule(budgetEventBill);
                    } else if (IBillNumConstant.TRANSFERACCOUNT.equals(budgetEventBill.getBillCode())) {
                        // 根据条件筛选主子表信息，并拼接成Json
                        ruleType = budgetEventBill.getRuleType();
                        bills = cmpBudgetTransferAccountManagerService.queryBillByRule(budgetEventBill);
                    } else if (IBillNumConstant.CURRENCYEXCHANGE.equals(budgetEventBill.getBillCode())) {
                        // 根据条件筛选主子表信息，并拼接成Json
                        ruleType = budgetEventBill.getRuleType();
                        bills = cmpBudgetCurrencyExchangeManagerService.queryBillByRule(budgetEventBill);
                    }
//                    else if (IBillNumConstant.CMP_BATCHTRANSFERACCOUNT.equals(budgetEventBill.getBillCode())) {
//                        bills = cmpBudgetBatchTransferAccountManagerService.queryBillByRule(budgetEventBill);
//                    }
                    for (int k = 0; k < bills.size(); k++) {
                        CtmJSONObject jsonObject = bills.getJSONObject(k);
                        Map<String, Object> billMap = CtmJSONObject.parseObject(CtmJSONObject.toJSONString(jsonObject), Map.class);
                        list.add(billMap);
                    }
                }
                balancePushDto.setBills(list);
                initBalancePushDto(balancePushDto);
            }
            return JsonUtils.toJsonString(EventResponseVO.success());
        }
        return JsonUtils.toJsonString(EventResponseVO.success());
    }

    /**
     * 预算启动推送期初数据
     * @param balancePushDto
     * @return
     * @throws Exception
     */
    private void initBalancePushDto(BalancePushDto balancePushDto) throws Exception{
        // TODO 分批次发送
        List<Map<String,Object>> list = balancePushDto.getBills();
        if (list.size() < 2*BATCH_SIZE) {
            // 小于10000笔直接单条发送
            balancePushService.pushBalance(balancePushDto);
        } else {
            int times = divideRoundUp(list.size());
            for (int i = 1; i <= times; i++) {
                // 每次都要重新生成
                balancePushDto.setRequestUniqueId(UUID.randomUUID().toString());
                Map<String, Object> isBatches = new HashMap();
                isBatches.put("batchindex", String.valueOf(i));
                isBatches.put("batchTotal", String.valueOf(times));
                balancePushDto.setIsBatches(isBatches);
                if (i * BATCH_SIZE >= list.size()) {
                    List<Map<String,Object>> sublist = list.subList(1+(i-1)*BATCH_SIZE, list.size());
                    balancePushDto.setBills(sublist);
                } else {
                    List<Map<String,Object>> sublist = list.subList(1+(i-1)*BATCH_SIZE, i*BATCH_SIZE);
                    balancePushDto.setBills(sublist);
                }
                balancePushService.pushBalance(balancePushDto);
            }
        }
    }

    /**
     * 除法，结果向上取整
     * @param dividend
     * @return
     */
    public int divideRoundUp(int dividend) {
        double result = (double) dividend / BATCH_SIZE;
        return (int) Math.ceil(result);
    }

}
