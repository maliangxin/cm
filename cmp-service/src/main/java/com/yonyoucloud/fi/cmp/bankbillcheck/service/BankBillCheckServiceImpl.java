package com.yonyoucloud.fi.cmp.bankbillcheck.service;

import cn.hutool.core.bean.BeanUtil;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.yonyou.iuap.ucf.common.i18n.MessageUtils;
import com.yonyou.iuap.yms.id.generator.YmsOidGenerator;
import com.yonyou.ucf.basedoc.model.BankAcctCurrencyVO;
import com.yonyou.ucf.basedoc.model.CurrencyTenantDTO;
import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVO;
import com.yonyou.ucf.basedoc.model.rpcparams.EnterpriseParams;
import com.yonyou.ucf.mdd.common.enums.OperationTypeEnum;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bpm.service.ProcessService;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.yonbip.ctm.accelerator.threadpool.CtmThreadPoolExecutor;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.yonbip.ctm.bankconnection.BankConnectionAdapterContext;
import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.json.JSONObject;
import com.yonyou.yonbip.ctm.util.lock.CtmLockTool;
import com.yonyou.yonbip.ctm.util.lock.LockStatus;
import com.yonyoucloud.fi.basecom.service.FIBillService;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.cmp.bankaccountsetting.BankAccountSetting;
import com.yonyoucloud.fi.cmp.bankbillcheck.BankBillCheck;
import com.yonyoucloud.fi.cmp.bankreceipt.service.TaskBankReceiptService;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import com.yonyoucloud.fi.cmp.constant.ITransCodeConstant;
import com.yonyoucloud.fi.cmp.util.*;
import com.yonyoucloud.fi.cmp.util.basedoc.CurrencyQueryService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.message.BasicNameValuePair;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import yonyou.bpm.rest.RepositoryService;
import yonyou.bpm.rest.request.repository.ProcessDefinitionQueryParam;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @Author zhucongcong
 * @Date 2024/9/25
 */
@Service
@Slf4j
public class BankBillCheckServiceImpl implements BankBillCheckService {


    public static final String ERROR_MESSAGE = "errorMessage";
    public static String SERVICE_RESP_CODE = "000000";  //服务响应码   “000000”（6个0）代表成功，如果返回“000000”，则service_status的值一定是“00”

    @Autowired
    private BaseRefRpcService baseRefRpcService;

    @Autowired
    private CurrencyQueryService currencyQueryService;


    private AtomicInteger cardinalNumber = new AtomicInteger(0);

    @Autowired
    private BankConnectionAdapterContext bankConnectionAdapterContext;

    @Autowired
    private YmsOidGenerator ymsOidGenerator;

    @Resource
    private FIBillService fiBillService;

    @Autowired
    private ProcessService processService;

    @Autowired
    private CtmThreadPoolExecutor ctmThreadPoolExecutor;

    @Autowired
    private TaskBankReceiptService taskBankReceiptService;
    /**
     * 对账信息拉取
     * @param params
     * @return
     * @throws Exception
     */
    @Override
    //开新事务，否则外面报错抛给前端，会导致数据回滚
    @Transactional(propagation= Propagation.REQUIRES_NEW,rollbackFor = RuntimeException.class)
    public List<String> queryBillInfo(CtmJSONObject params) throws Exception {

        StringBuilder errorMessage = new StringBuilder();
        //查询账号信息
        List<EnterpriseBankAcctVO> accountList = getAccount(params);

        List<String> currencys = (List<String>) params.get("currency");
        List<String> bankIds = new ArrayList<>();
        CtmJSONArray result = new CtmJSONArray();
        List<String> errorList = queryBill(accountList, currencys, bankIds, errorMessage, params, result);
        return errorList;
    }


    public List<String> queryBill(List<EnterpriseBankAcctVO> accountList, List<String> currencys, List<String> bankIds,
                                  StringBuilder errorMessage, CtmJSONObject params, CtmJSONArray result) throws Exception {
        List<String> errorMessageList = new ArrayList<>();
        List<String> accountInfoLocks = BatchLockGetKeysUtils.batchLockCombineKeysByCurrency(ICmpConstant.QUERYELECSTATEMENT, accountList);
        CtmLockTool.executeInOneServiceExclusivelyBatchLock(accountInfoLocks, 60 * 60 * 2L, TimeUnit.SECONDS, (int lockstatus) -> {
            if (lockstatus == LockStatus.GETLOCK_FAIL) {
                throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A050800ED", "系统正在对此账户拉取中"));
            }
            for (EnterpriseBankAcctVO bankAccount : accountList) {
                bankIds.add(bankAccount.getId());
                log.error("queryBill bankAccount : {}", bankAccount);
                for (BankAcctCurrencyVO bankAcctCurrencyVO : bankAccount.getCurrencyList()) {
                    String currencyId = bankAcctCurrencyVO.getCurrency();
                    //如果为空查该账户下所有的币种  //不为空查对应的币种
                    if (CollectionUtils.isEmpty(currencys) || currencys.contains(currencyId)) {
                        CurrencyTenantDTO currencyTenantDTO = currencyQueryService.findById(currencyId);
                        String key = bankAccount.getAccount() + currencyTenantDTO.getId();
                        try {
//                        CtmLockTool.executeInOneServiceLock(key, 60 * 60 * 2L, TimeUnit.SECONDS, (int lockstatus) -> {
//                            if (lockstatus == LockStatus.GETLOCK_FAIL) {
//                                errorMessage.add(String.format("[%s]:系统正在对此账户拉取中", key));
//                                return;
//                            }
                            StringBuilder errorMessageStringBuilder = new StringBuilder();
                            CtmJSONArray jsonArray = buildAndQueryUnNeedUkey(params, bankAccount, errorMessageStringBuilder, currencyTenantDTO);
                            String errorStr = errorMessageStringBuilder.toString();
                            if (!errorStr.isEmpty()) {
                                errorMessageList.add(errorStr);
                            }
                            result.addAll(jsonArray);
//                        });
                        } catch (Exception e) {
                            log.error("银企对账信息查询失败：" + e.getMessage(), e);
                            //errorMessage.append(String.format("[%s]:此账户操作发生异常：[%s]", key, e.getMessage()));
                            errorMessageList.add(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540050A", "[%s]:此账户操作发生异常") /* "[%s]:此账户操作发生异常" */, key));
                        }
                    }
                }
            }
        });

        //if (CollectionUtils.isEmpty(result)) {
        //    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102355"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A050800ED", "对账信息查询为空") /* "对账信息查询为空" */);
        //}

        if (bankIds.isEmpty()) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102356"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A050800E7", "选择的企业银行账户没有开通银企联，无法查询银企对账信息") /* "选择的企业银行账户没有开通银企联，无法查询银企对账信息" */);
        }
        return errorMessageList;
    }

    /**
     * 相符操作
     * @param rows
     * @return
     * @throws Exception
     */
    @Override
    public CtmJSONObject match(CtmJSONArray rows) throws Exception {

        List<String> message = new ArrayList<>();
        //存储需要修改的数据
        List<BankBillCheck> updateBankBillCheck = new ArrayList<>();
        //校验数据
        buildMatchData(rows, message, updateBankBillCheck);
        //返回值不为空则返回给前端，为空证明数据校验全部通过并进行相符操作
        if (CollectionUtils.isNotEmpty(message)) {
            CtmJSONObject jsonObject = new CtmJSONObject();
            jsonObject.put("dataMsg", message);
            return jsonObject;
        } else {
            //更新对账结果为“相符”；同时更新指令状态为“未发送”；更新企业方余额、调整后金额默认等于银行方余额
            for (BankBillCheck bankBillCheck : updateBankBillCheck) {
                bankBillCheck.setEntityStatus(EntityStatus.Update);
                bankBillCheck.setCheckResult(new Short("1"));
                bankBillCheck.setInstructStatus(new Short("1"));
                bankBillCheck.setEnterpriseBalance(bankBillCheck.getBankBalance());
                bankBillCheck.setAdjustBalance(bankBillCheck.getBankBalance());
                bankBillCheck.setPhone(null);
                bankBillCheck.setRemark(null);
            }
            MetaDaoHelper.update(BankBillCheck.ENTITY_NAME, updateBankBillCheck);
        }
        return new JSONObject();
    }

    /**
     * 不相符校验数据
     * @param rows
     * @return
     * @throws Exception
     */
    @Override
    public CtmJSONObject unMatch(CtmJSONArray rows) throws Exception {

        List<String> message = new ArrayList<>();
        //存储需要修改的数据
        List<BankBillCheck> updateBankBillCheck = new ArrayList<>();

        buildMatchData(rows, message, updateBankBillCheck);
        if (CollectionUtils.isNotEmpty(message)) {
            CtmJSONObject jsonObject = new CtmJSONObject();
            jsonObject.put("dataMsg", message);
            return jsonObject;
        }
        return new JSONObject();
    }

    /**
     * 不相符操作 校验、回写数据
     *
     * @param params
     * @return
     */
    @Override
    public CtmJSONObject unMatchUpdate(CtmJSONObject params) throws Exception{
        //先校验数据必填性、以及电话格式
        CtmJSONArray dataArray = params.getJSONArray("data");
        List<BankBillCheck> billCheckList = new ArrayList<>();
        for (int i = 0; i < dataArray.size(); i++) {
            CtmJSONObject jsonObject = dataArray.getJSONObject(i);
            String enterpriseBalance = jsonObject.getString("enterpriseBalance");
            String phone = jsonObject.getString("phone");
            String remark = jsonObject.getString("remark");
            if (StringUtils.isEmpty(enterpriseBalance)) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102357"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1DBABB5405900039", "第%d行有必填字段未填写！") /* "第%d行有必填字段未填写！" */,(i + 1)));
            }
            //组装数据回写
            BankBillCheck bankBillCheck = new BankBillCheck();
            bankBillCheck.setId(jsonObject.getLong("id"));
            bankBillCheck.setEnterpriseBalance(jsonObject.getBigDecimal("enterpriseBalance"));
            bankBillCheck.setAdjustBalance(jsonObject.getBigDecimal("enterpriseBalance"));
            bankBillCheck.setPhone(phone);
            bankBillCheck.setRemark(remark);
            bankBillCheck.setCheckResult(new Short("2"));
            bankBillCheck.setInstructStatus(new Short("1"));
            bankBillCheck.setEntityStatus(EntityStatus.Update);
            billCheckList.add(bankBillCheck);
        }

        MetaDaoHelper.update(BankBillCheck.ENTITY_NAME, billCheckList);

        List<String> ids = new ArrayList<>();

        for (int i = 0; i < dataArray.size(); i++) {
            ids.add(dataArray.getJSONObject(i).getString("id"));
        }
        QuerySchema querySchema = QuerySchema.create().addSelect("*");
        querySchema.addCondition(QueryConditionGroup.and(QueryCondition.name("id").in(ids)));
        List<BizObject> bizObjects = MetaDaoHelper.queryObject(BankBillCheck.ENTITY_NAME, querySchema, null);

        for (BizObject bizObject : bizObjects) {
            //确认并提交按钮，还需执行提交操作
            if (dataArray.getJSONObject(0).getInteger("isSubmit") == 1) {
                BillDataDto billDataDto = new BillDataDto();
                billDataDto.setBillnum(IServicecodeConstant.BANK_BILL_CHECK);
                billDataDto.setData(bizObject);
                //触发提交
                fiBillService.executeUpdate(OperationTypeEnum.SUBMIT.getValue(), billDataDto);
            }
        }
        return new CtmJSONObject();
    }

    /**
     * 对账结果提交
     * @param jsonArray
     * @return
     * @throws Exception
     */
    @Override
    public CtmJSONObject checkResultSubmit(CtmJSONArray jsonArray) throws Exception {
        CtmJSONObject result = new CtmJSONObject();
        //单据具体失败信息
        List<String> messages = new ArrayList<>();
        //失败的数量
        int i = 0;
        CtmJSONObject failed = new CtmJSONObject();
        List<Long> ids = new ArrayList<>();
        for (int j = 0; j < jsonArray.size(); j++) {
            ids.add(jsonArray.getJSONObject(j).getLong("id"));
        }
        //反查数据
        QuerySchema querySchema = QuerySchema.create().addSelect("*");
        querySchema.addCondition(QueryConditionGroup.and(QueryCondition.name("id").in(ids)));
        List<BizObject> bizObjects = MetaDaoHelper.queryObject(BankBillCheck.ENTITY_NAME, querySchema, null);

        //存储能够对账结果提交的数据
        List<BizObject> bizObjectList = new ArrayList<>();
        //存储要修改的实体 且状态是 指令发送成功的
        List<BankBillCheck> bankBillCheckUpdateSuccess = new ArrayList<>();
        //存储要修改的实体 且状态是 指令发送失败的
        List<BankBillCheck> bankBillCheckUpdateFail = new ArrayList<>();
        //数据校验
        for (BizObject bizObject : bizObjects) {
            Short checkStatus = bizObject.getShort("checkStatus");
            Short checkResult = bizObject.getShort("checkResult");
            Short verifystate = bizObject.getShort("verifystate");
            Short instructStatus = bizObject.getShort("instructStatus");
            String checkBillCode = bizObject.getString("checkBillCode");
            if (checkStatus == 4) {
                failed.put(bizObject.getId(), bizObject.getId());
                i++;
                messages.add(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400501", "对账单编号【%s】无需对账，无需进行结果提交，请检查！") /* "对账单编号【%s】无需对账，无需进行结果提交，请检查！" */, bizObject.get("checkBillCode")));
            } else if (checkStatus == 1) {
                failed.put(bizObject.getId(), bizObject.getId());
                i++;
                messages.add(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400504", "对账单编号【%s】已对账，无需进行结果提交，请检查！") /* "对账单编号【%s】已对账，无需进行结果提交，请检查！" */, checkBillCode));
            } else if (checkStatus == 0 && null == checkResult) {
                failed.put(bizObject.getId(), bizObject.getId());
                i++;
                messages.add(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400505", "对账单编号【%s】未对账，需先进行对账结果确认！") /* "对账单编号【%s】未对账，需先进行对账结果确认！" */, checkBillCode));
            } else if (checkStatus == 0 && null != checkResult && verifystate != 2) {
                failed.put(bizObject.getId(), bizObject.getId());
                i++;
                messages.add(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400507", "对账单编号【%s】未审批完成，请检查！") /* "对账单编号【%s】未审批完成，请检查！" */, checkBillCode));
            } else if (checkStatus == 3 && instructStatus == 2) {
                failed.put(bizObject.getId(), bizObject.getId());
                i++;
                messages.add(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400508", "对账单编号【%s】指令状态为已发送，无需再次提交，请稍后处理！") /* "对账单编号【%s】指令状态为已发送，无需再次提交，请稍后处理！" */, checkBillCode));
            } else if (checkStatus == 3 && instructStatus != 2 && verifystate != 2) {
                failed.put(bizObject.getId(), bizObject.getId());
                i++;
                messages.add(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400507", "对账单编号【%s】未审批完成，请检查！") /* "对账单编号【%s】未审批完成，请检查！" */, checkBillCode));
            } else if (checkStatus == 2) {
                failed.put(bizObject.getId(), bizObject.getId());
                i++;
                messages.add(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540050B", "对账单编号【%s】对账结果已提交至银行，无需再次提交，请先查询最新对账结果！") /* "对账单编号【%s】对账结果已提交至银行，无需再次提交，请先查询最新对账结果！" */, checkBillCode));
            } else {
                Map<String, Object> accountSeting = getBankAccountSetting(bizObject.getString("enterpriseBankAccount"));
                if (accountSeting != null && accountSeting.get("customNo") != null) {
                    bizObject.put("customNo", accountSeting.get("customNo").toString());
                    CurrencyTenantDTO currencyTenantDTO = currencyQueryService.findById(bizObject.getString("currency"));
                    bizObject.put("curr_code", currencyTenantDTO.getCode());
                    bizObjectList.add(bizObject);
                } else {
                    //存储失败信息
                    failed.put(bizObject.getId(), bizObject.getId());
                    i++;
                    messages.add(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400511", "对账单编号【%s】的企业银行账户没有开通银企联，无法对账结果提交！") /* "对账单编号【%s】的企业银行账户没有开通银企联，无法对账结果提交！" */, checkBillCode));
                }
            }
        }

        if (CollectionUtils.isNotEmpty(bizObjectList)) {
            List<String> enterpriseBankAccount = bizObjectList.stream().map(item -> item.getString("enterpriseBankAccount")).collect(Collectors.toList());
            EnterpriseParams enterpriseParams = new EnterpriseParams();
            enterpriseParams.setIdList(enterpriseBankAccount);
            enterpriseParams.setPageSize(4500);
            List<EnterpriseBankAcctVO> enterpriseBankAcctVOs = baseRefRpcService.queryEnterpriseBankAcctByCondition(enterpriseParams);
            Map<String, EnterpriseBankAcctVO> bankAcctVOMap = enterpriseBankAcctVOs.stream().collect(Collectors.toMap(EnterpriseBankAcctVO::getId, Function.identity()));
            //构建银企联请求参数
            CtmJSONObject resultJson = buildSubmitUnNeedKey(bizObjectList, bankAcctVOMap);
            if (resultJson.getInteger("code") == 1) {
                CtmJSONObject responseHead = resultJson.getJSONObject("data").getJSONObject("response_head");
                String serviceStatus = responseHead.getString("service_status");
                String service_resp_code = responseHead.getString("service_resp_code");
                String serviceRespDesc = responseHead.getString("service_resp_desc");
                if (("00").equals(serviceStatus) && SERVICE_RESP_CODE.equals(service_resp_code)) {
                    CtmJSONObject responseBody = resultJson.getJSONObject("data").getJSONObject("response_body");
                    CtmJSONArray record = responseBody.getJSONArray("record");
                    Map<String, CtmJSONObject> recordMap = new HashMap<>();
                    for (int j = 0; j < record.size(); j++) {
                        CtmJSONObject jsonObject = record.getJSONObject(j);
                        recordMap.put(jsonObject.getString("unique_no"), jsonObject);
                    }
                    for (BizObject bizObject : bizObjectList) {
                        //没找到就算失败并修改指令状态为发送失败
                        CtmJSONObject jsonObject = recordMap.get(bizObject.getString("checkBillSign"));
                        if (null == jsonObject) {
                            BankBillCheck bankBillCheck = new BankBillCheck();
                            bankBillCheck.setId(bizObject.getId());
                            bankBillCheck.setEntityStatus(EntityStatus.Update);
                            bankBillCheck.setInstructStatus(new Short("4"));
                            bankBillCheck.setInstructSubmiter(AppContext.getCurrentUser().getYhtUserId());
                            bankBillCheckUpdateFail.add(bankBillCheck);
                            failed.put(bizObject.getId(), bizObject.getId());
                            i++;
                            messages.add(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400506", "对账单编号【%s】对账结果提交失败！原因：银企联未返回！") /* "对账单编号【%s】对账结果提交失败！原因：银企联未返回！" */, bizObject.getString("checkBillCode")));
                            continue;
                        }
                        BankBillCheck bankBillCheck = new BankBillCheck();
                        bankBillCheck.setId(bizObject.getId());
                        bankBillCheck.setEntityStatus(EntityStatus.Update);
                        bankBillCheck.setCheckStatus(new Short(jsonObject.getString("statement_status")));
                        bankBillCheck.setBankResCode(jsonObject.getString("bank_resp_code"));
                        bankBillCheck.setBankResMsg(jsonObject.getString("bank_resp_desc"));
                        bankBillCheck.setInstructStatus(new Short("3"));
                        bankBillCheck.setInstructSubmiter(AppContext.getCurrentUser().getYhtUserId());
                        bankBillCheckUpdateSuccess.add(bankBillCheck);
                    }
                }else {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102358"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1DBABB540590003A", "对账信息提交银企联失败，失败信息：%s") /* "对账信息提交银企联失败，失败信息：%s" */,serviceRespDesc));
                }
            } else {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102358"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1DBABB540590003A", "对账信息提交银企联失败，失败信息：%s") /* "对账信息提交银企联失败，失败信息：%s" */, resultJson.getString("message")));
            }
        }

        if (CollectionUtils.isNotEmpty(bankBillCheckUpdateFail)) {
            log.error("对账结果提交失败修改的数据量：{}", bankBillCheckUpdateFail.size());
            MetaDaoHelper.update(BankBillCheck.ENTITY_NAME, bankBillCheckUpdateFail);
        }

        if (CollectionUtils.isNotEmpty(bankBillCheckUpdateSuccess)) {
            log.error("对账结果提交成功修改的数据量：{}", bankBillCheckUpdateSuccess.size());
            MetaDaoHelper.update(BankBillCheck.ENTITY_NAME, bankBillCheckUpdateSuccess);
        }

        //组装返回值给前端显示
        String message = String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400514", "共：[%s]张单据；[%s]张对账结果提交成功；[%s]张对账结果提交失败！") /* "共：[%s]张单据；[%s]张对账结果提交成功；[%s]张对账结果提交失败！" */, bizObjects.size(), (bizObjects.size() - i), i);
        result.put("msg", message);
        result.put("msgs", messages);
        result.put("messages", messages);
        result.put("count", bizObjects.size());
        result.put("sucessCount", bizObjects.size() - i);
        result.put("failCount", i);
        if (failed.size() > 0) {
            result.put("failed", failed);
        }
        return result;
    }

    /**
     * 对账结果查询
     * @param jsonArray
     * @return
     */
    @Override
    public CtmJSONObject checkResultQuery(CtmJSONArray jsonArray) throws Exception {

        CtmJSONObject result = new CtmJSONObject();
        //单据具体失败信息
        List<String> messages = new ArrayList<>();
        //失败的数量
        int i = 0;
        CtmJSONObject failed = new CtmJSONObject();

        List<String> ids = new ArrayList<>();
        for (int j = 0; j < jsonArray.size(); j++) {
            ids.add(jsonArray.getJSONObject(j).getString("id"));
        }
        //反查数据
        QuerySchema querySchema = QuerySchema.create().addSelect("id,checkStatus,instructStatus,checkBillCode,checkBillSign,enterpriseBankAccount,enterpriseBankAccount.name as accountName,enterpriseBankAccount.account as account");
        querySchema.addCondition(QueryConditionGroup.and(QueryCondition.name("id").in(ids)));
        List<BizObject> bizObjectList = MetaDaoHelper.queryObject(BankBillCheck.ENTITY_NAME, querySchema, null);

        //能够发送银企联的数据
        List<BizObject> bizObjectSuccess = new ArrayList<>();
        //校验数据
        for (BizObject bizObject : bizObjectList) {
            String idStr = bizObject.getId().toString();
            bizObject.setId(idStr);
            Short checkStatus = bizObject.getShort("checkStatus");
            Short instructStatus = bizObject.getShort("instructStatus");
            String checkBillCode = bizObject.getString("checkBillCode");
            if (checkStatus == 4) {
                failed.put(bizObject.getId(), bizObject.getId());
                i++;
                messages.add(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540050C", "对账单编号【%s】无需对账，无需进行结果查询，请检查！") /* "对账单编号【%s】无需对账，无需进行结果查询，请检查！" */, checkBillCode));
            }else if (checkStatus == 1){
                failed.put(bizObject.getId(), bizObject.getId());
                i++;
                messages.add(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540050D", "对账单编号【%s】已对账，无需进行结果查询，请检查！") /* "对账单编号【%s】已对账，无需进行结果查询，请检查！" */, checkBillCode));
            }else if (checkStatus == 0){
                failed.put(bizObject.getId(), bizObject.getId());
                i++;
                messages.add(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400510", "对账单编号【%s】未对账，无需进行结果查询，请检查！") /* "对账单编号【%s】未对账，无需进行结果查询，请检查！" */, checkBillCode));
            }else if (checkStatus == 3){
                failed.put(bizObject.getId(), bizObject.getId());
                i++;
                messages.add(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400512", "对账单编号【%s】对账失败，无需进行结果查询，请检查！") /* "对账单编号【%s】对账失败，无需进行结果查询，请检查！" */, checkBillCode));
            }else if (checkStatus == 2 && instructStatus != 3){
                failed.put(bizObject.getId(), bizObject.getId());
                i++;
                messages.add(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400513", "对账单编号【%s】对账结果提交指令未发送成功，不能进行结果查询，请先进行对账结果提交！") /* "对账单编号【%s】对账结果提交指令未发送成功，不能进行结果查询，请先进行对账结果提交！" */, checkBillCode));
            } else {
                Map<String, Object> accountSeting = getBankAccountSetting(bizObject.getString("enterpriseBankAccount"));
                if (accountSeting != null && accountSeting.get("customNo") != null) {
                    bizObject.put("customNo", accountSeting.get("customNo").toString());
                    bizObjectSuccess.add(bizObject);
                } else {
                    //存储失败信息
                    failed.put(bizObject.getId(), bizObject.getId());
                    i++;
                    messages.add(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400511", "对账单编号【%s】的企业银行账户没有开通银企联，无法对账结果提交！") /* "对账单编号【%s】的企业银行账户没有开通银企联，无法对账结果提交！" */, checkBillCode));
                }
            }
        }
        //存储要修改的实体
        List<BankBillCheck> bankBillCheckUpdateSuccess = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(bizObjectSuccess)){
            //构建发送给银企联的参数
            for (BizObject bizObject : bizObjectSuccess) {
                CtmJSONObject resultJson = buildCheckResultQuery(bizObject);
                if (resultJson.getInteger("code") == 1) {
                    CtmJSONObject responseHead = resultJson.getJSONObject("data").getJSONObject("response_head");
                    String serviceStatus = responseHead.getString("service_status");
                    String service_resp_code = responseHead.getString("service_resp_code");
                    if (("00").equals(serviceStatus) && SERVICE_RESP_CODE.equals(service_resp_code)) {
                        CtmJSONObject responseBody = resultJson.getJSONObject("data").getJSONObject("response_body");
                        BankBillCheck bankBillCheck = new BankBillCheck();
                        bankBillCheck.setId(bizObject.getId());
                        bankBillCheck.setEntityStatus(EntityStatus.Update);
                        bankBillCheck.setCheckStatus(new Short(responseBody.getString("statement_status")));
                        bankBillCheck.setCheckDate(DateUtils.dateParse(responseBody.getString("statement_date"), DateUtils.YYYYMMDD));
                        if (StringUtils.isNotEmpty(responseBody.getString("order_status"))) {
                            bankBillCheck.setCheckResult(new Short(responseBody.getString("order_status")));
                        }
                        bankBillCheckUpdateSuccess.add(bankBillCheck);
                    } else {
                        failed.put(bizObject.getId(), bizObject.getId());
                        i++;
                        messages.add(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400509", "对账单编号【%s】对账结果查询失败，原因：%s") /* "对账单编号【%s】对账结果查询失败，原因：%s" */, bizObject.getString("checkBillCode"), responseHead.getString("service_resp_desc")));
                    }
                } else {
                    failed.put(bizObject.getId(), bizObject.getId());
                    i++;
                    messages.add(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400509", "对账单编号【%s】对账结果查询失败，原因：%s") /* "对账单编号【%s】对账结果查询失败，原因：%s" */, bizObject.getString("checkBillCode"), resultJson.getString("message")));
                }
            }
        }

        if (CollectionUtils.isNotEmpty(bankBillCheckUpdateSuccess)) {
            log.error("对账结果查询成功修改的数据量：{}", bankBillCheckUpdateSuccess.size());
            MetaDaoHelper.update(BankBillCheck.ENTITY_NAME, bankBillCheckUpdateSuccess);
        }

        //组装返回值给前端显示
        String message = String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540050E", "共：[%s]张单据；[%s]张对账结果查询成功；[%s]张对账结果查询失败！") /* "共：[%s]张单据；[%s]张对账结果查询成功；[%s]张对账结果查询失败！" */, bizObjectList.size(), (bizObjectList.size() - i), i);
        result.put("msg", message);
        result.put("msgs", messages);
        result.put("messages", messages);
        result.put("count", bizObjectList.size());
        result.put("sucessCount", bizObjectList.size() - i);
        result.put("failCount", i);
        if (failed.size() > 0) {
            result.put("failed", failed);
        }
        return result;
    }

    /**
     * 调度任务“银企对账信息查询”，支持按照资金组织、期间、银行类别、银行账号等维度查询；最小时间间隔分钟；预置调度任务，时间间隔15分钟
     * 定时从开通直联的银行查询余额信息，用于进行银企对账余额的确认
     * @throws Exception
     */
    @Override
    public Map<String, Object> scheduleQueryBillInfo(CtmJSONObject param) throws Exception {
        String logId = param.getString("logId");
        try {
            //任务参数校验
            TaskUtils.dateCheck(param);
            ctmThreadPoolExecutor.getThreadPoolExecutor().submit(() -> {
                try {
                    Map<String, List<EnterpriseBankAcctVO>> bankAccountsGroup = taskBankReceiptService.bankReceipt(param);
                    HashMap<String, String> querydate = TaskUtils.queryDateProcess(param, "yyyy-MM-dd");
                    param.put("begNum", 1);
                    log.error("scheduleQueryBillInfo querydate:{}", querydate);
                    if (querydate.isEmpty()) {
                        param.put("startDate", getFirstDayOfLastMonth());
                        param.put("endDate", getLastDayOfLastMonth());
                    } else if (querydate.containsKey(TaskUtils.TASK_NO_DATA)) {
                        TaskUtils.updateTaskLog((Map<String,String>)param.get("ipaParams"),TaskUtils.TASK_BACK_FAILURE, logId,
                                com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400503", "开始日期大于当前日期") /* "开始日期大于当前日期" */,
                                TaskUtils.UPDATE_TASK_LOG_URL);
                        return;
                    } else {
                        param.put("startDate", querydate.get("startdate"));
                        param.put("endDate", querydate.get("enddate"));
                    }
                    String monthsinadvance = (String) (Optional.ofNullable(param.get("monthsinadvance")).orElse(""));
                    if(StringUtils.isNotEmpty(monthsinadvance)){
                        param.put("startDate", getFirstDayOfMonthsInAdvance(param.get("startDate").toString(), Integer.parseInt(monthsinadvance)));
                        param.put("endDate", getLastDayOfMonthsInAdvance(param.get("endDate").toString(), Integer.parseInt(monthsinadvance)));
                    }
                    log.error("scheduleQueryBillInfo param:{}", param);
                    List<String> bankIds = new ArrayList<>();
                    CtmJSONArray result = new CtmJSONArray();
                    StringBuilder message = new StringBuilder();

                    //直联账户
                    List<EnterpriseBankAcctVO> enterpriseBankAcctVOs = bankAccountsGroup.get("checkSuccess");
                    enterpriseBankAcctVOs = DirectmethodCheckUtils.getAccountByParamMapOfEnterpriseBankAcctVOs(param,enterpriseBankAcctVOs);
                    if(CollectionUtils.isNotEmpty(enterpriseBankAcctVOs)){
                        List<String> currencys = new ArrayList<>();
                        String currency = (String) param.get("currency");
                        if(StringUtils.isNotEmpty(currency)){
                            currencys.add(currency);
                        }
                        List<String> errorMessage = queryBill(enterpriseBankAcctVOs, currencys, bankIds, message, param, result);
                        if(com.yonyou.cloud.utils.CollectionUtils.isNotEmpty(errorMessage)){
                            log.error("失败数据错误信息：{}", errorMessage);
                            TaskUtils.updateTaskLog((Map<String,String>)param.get("ipaParams"),TaskUtils.TASK_BACK_FAILURE, logId,
                                    String.format(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540050F", "失败[%s]条数据，详情请查看日志") /* "失败[%s]条数据，详情请查看日志" */, errorMessage.size())),
                                    TaskUtils.UPDATE_TASK_LOG_URL);
                        }
                    }
                    if (CollectionUtils.isEmpty(result)) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102355"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A050800ED", "对账信息查询为空") /* "对账信息查询为空" */);
                    }
                    TaskUtils.updateTaskLog((Map<String,String>)param.get("ipaParams"),TaskUtils.TASK_BACK_SUCCESS, logId, MessageUtils.getMessage("P_YS_OA_app_xtyyjm_0000035989") /* "执行成功" */ + ":" + message, TaskUtils.UPDATE_TASK_LOG_URL);
                } catch (Exception e) {
                    TaskUtils.updateTaskLog((Map<String,String>)param.get("ipaParams"),TaskUtils.TASK_BACK_FAILURE, logId, e.getMessage(), TaskUtils.UPDATE_TASK_LOG_URL);
                }
            });
        } catch (Exception e) {
            TaskUtils.updateTaskLog((Map<String,String>)param.get("ipaParams"),TaskUtils.TASK_BACK_FAILURE, logId, e.getMessage(), TaskUtils.UPDATE_TASK_LOG_URL);
        }
        Map<String, Object> retMap = new HashMap<>();
        retMap.put("asynchronized", true);
        return retMap;
    }

    /**
     * 调度任务“银企对账确认状态查询”，不预置维度；最小时间间隔分钟；预置调度任务，时间间隔5分钟（可参照调度任务：薪资支付、付款工作台支付状态查询）
     * 定时从银行获取银企对账确认结果提交后，银行返回的结果。
     *      1、执行时，查找指令状态=“发送成功”且对账状态='对账中'时的数据，调用银企联接口获取最新处理结果
     *      2、调度任务获取时，调用银企联43T25接口获取确认状态并更新相应字段
     * @throws Exception
     */
    @Override
    public Map<String, Object> scheduleCheckResultQuery(CtmJSONObject param) throws Exception {
        String logId = param.getString("logId");
        try {
            ctmThreadPoolExecutor.getThreadPoolExecutor().submit(() -> {
                try {
                    QuerySchema querySchema = QuerySchema.create().addSelect("id");
                    querySchema.appendQueryCondition(QueryCondition.name("instructStatus").eq("3"));//发送成功
                    querySchema.appendQueryCondition(QueryCondition.name("checkStatus").eq("2"));//对账中
                    List<BizObject> bizObjectList = MetaDaoHelper.queryObject(BankBillCheck.ENTITY_NAME, querySchema, null);
                    CtmJSONArray jsonArray = new CtmJSONArray();
                    if(CollectionUtils.isNotEmpty(bizObjectList)){
                        for(BizObject bizObject : bizObjectList){
                            CtmJSONObject jsonObject = new CtmJSONObject();
                            jsonObject.put("id", bizObject.get("id"));
                            jsonArray.add(jsonObject);
                        }
                        CtmJSONObject result = checkResultQuery(jsonArray);
                        if(result.get("failed") != null){
                            log.error("银企对账确认状态查询失败数据:{}", result.get("failed"));
                        }
                    }
                    TaskUtils.updateTaskLog((Map<String,String>)param.get("ipaParams"),TaskUtils.TASK_BACK_SUCCESS, logId, MessageUtils.getMessage("P_YS_OA_app_xtyyjm_0000035989") /* "执行成功" */, TaskUtils.UPDATE_TASK_LOG_URL);
                } catch (Exception e) {
                    TaskUtils.updateTaskLog((Map<String,String>)param.get("ipaParams"),TaskUtils.TASK_BACK_FAILURE, logId, e.getMessage(), TaskUtils.UPDATE_TASK_LOG_URL);
                }
            });
        } catch (Exception e) {
            TaskUtils.updateTaskLog((Map<String,String>)param.get("ipaParams"),TaskUtils.TASK_BACK_FAILURE, logId, e.getMessage(), TaskUtils.UPDATE_TASK_LOG_URL);
        }
        Map<String, Object> retMap = new HashMap<>();
        retMap.put("asynchronized", true);
        return retMap;
    }

    /**
     * 发送银企联对账结果查询
     * @param bizObjectSuccess
     * @return
     */
    private CtmJSONObject buildCheckResultQuery(BizObject bizObjectSuccess) throws Exception{
        CtmJSONObject queryMsg = buildBankBillQueryYQL(bizObjectSuccess);
        String signMsg = bankConnectionAdapterContext.chanPaySignMessage(queryMsg.toString());
        List<BasicNameValuePair> requestData = new ArrayList<>();
        requestData.add(new BasicNameValuePair("reqData", queryMsg.toString()));
        requestData.add(new BasicNameValuePair("reqSignData", signMsg));
        log.error("======对账结果查询请求参数=======>" + queryMsg.toString());
        CtmJSONObject result = CtmJSONObject.toJSON(HttpsUtils.doHttpsPost(ITransCodeConstant.SEARCH_BANK_BILL_RESULT, requestData, bankConnectionAdapterContext.getChanPayUri()));
        log.error("========对账结果查询result======>" + CtmJSONObject.toJSONString(result));
        return result;
    }

    /**
     * 构建银企联对账结果查询请求参数
     * @param bizObject
     * @return
     */
    private CtmJSONObject buildBankBillQueryYQL(BizObject bizObject) {
        String requestseqno = buildRequestSeqNo(bizObject.getString("customNo"));
        CtmJSONObject requestHead = buildRequloadestHead(ITransCodeConstant.SEARCH_BANK_BILL_RESULT,
                null,
                bizObject.getString("customNo"),
                requestseqno,
                null);
        CtmJSONObject body = new CtmJSONObject();
        body.put("acct_no", bizObject.getString("account"));
        body.put("acct_name", bizObject.getString("accountName"));
        //全局对账单标识
        body.put("unique_no", bizObject.getString("checkBillSign"));
        CtmJSONObject queryMsg = new CtmJSONObject();
        queryMsg.put("request_head", requestHead);
        queryMsg.put("request_body", body);
        return queryMsg;
    }

    /**
     * 构建银企联对账结果提交请求参数
     * @param bizObjectList
     * @param bankAcctVOMap
     */
    private CtmJSONObject buildSubmitUnNeedKey(List<BizObject> bizObjectList, Map<String, EnterpriseBankAcctVO> bankAcctVOMap) throws Exception {
        CtmJSONObject queryMsg = buildBankBillSubmit(bizObjectList, bankAcctVOMap);
        String signMsg = bankConnectionAdapterContext.chanPaySignMessage(queryMsg.toString());
        List<BasicNameValuePair> requestData = new ArrayList<>();
        requestData.add(new BasicNameValuePair("reqData", queryMsg.toString()));
        requestData.add(new BasicNameValuePair("reqSignData", signMsg));
        log.error("======对账结果提交请求参数=======>" + queryMsg.toString());
        CtmJSONObject result = CtmJSONObject.toJSON(HttpsUtils.doHttpsPost(ITransCodeConstant.SUBMIT_BANK_BILL_CHECK, requestData, bankConnectionAdapterContext.getChanPayUri()));
        log.error("========对账结果提交result======>" + CtmJSONObject.toJSONString(result));
        return result;
    }

    /**
     * 构建银企联对账结果提交请求参数
     * @param bizObjectList
     * @param bankAcctVOMap
     * @return
     */
    private CtmJSONObject buildBankBillSubmit(List<BizObject> bizObjectList, Map<String, EnterpriseBankAcctVO> bankAcctVOMap) {
        String requestseqno = buildRequestSeqNo(bizObjectList.get(0).getString("customNo"));
        CtmJSONObject requestHead = buildRequloadestHead(ITransCodeConstant.SUBMIT_BANK_BILL_CHECK,
                null,
                bizObjectList.get(0).getString("customNo"),
                requestseqno,
                null);
        CtmJSONObject body = new CtmJSONObject();
        CtmJSONArray requestArray = new CtmJSONArray();
        for (BizObject bizObject : bizObjectList) {
            //全局对账单标识
            CtmJSONObject requestBody = new CtmJSONObject();
            requestBody.put("unique_no",bizObject.getString("checkBillSign"));
            requestBody.put("acct_no", bankAcctVOMap.get(bizObject.getString("enterpriseBankAccount")).getAccount());
            requestBody.put("acct_name", bankAcctVOMap.get(bizObject.getString("enterpriseBankAccount")).getAcctName());
            // 币种，新增接口必输项
            requestBody.put("curr_code", bizObject.getString("curr_code"));
            if (bizObject.get("bankBalance") != null) {
                requestBody.put("bank_bal", bizObject.getBigDecimal("bankBalance").setScale(2, BigDecimal.ROUND_HALF_UP));
            }
            if (bizObject.get("adjustBalance") != null) {
                requestBody.put("tran_bal", bizObject.getBigDecimal("adjustBalance").setScale(2, BigDecimal.ROUND_HALF_UP));
            }
            if (bizObject.get("enterpriseBalance") != null) {
                requestBody.put("ent_bal", bizObject.getBigDecimal("enterpriseBalance").setScale(2, BigDecimal.ROUND_HALF_UP));
            }
            requestBody.put("order_status", bizObject.getShort("checkResult").toString());
            if (StringUtils.isNotEmpty(bizObject.getString("phone"))) {
                requestBody.put("tel", bizObject.getString("phone"));
            }
            requestArray.add(requestBody);
        }
        body.put("record",requestArray);
        CtmJSONObject queryMsg = new CtmJSONObject();
        queryMsg.put("request_head", requestHead);
        queryMsg.put("request_body", body);
        return queryMsg;
    }

    /**
     * 组装相符、不相符校验数据后的返回值
     * @param rows
     * @param message
     * @param updateBankBillCheck
     * @throws Exception
     */
    private void buildMatchData(CtmJSONArray rows, List<String> message, List<BankBillCheck> updateBankBillCheck) throws Exception {
        //存储code 方便后面组装
        List<String> instructCode = new ArrayList<>();
        List<String> verifystateCode = new ArrayList<>();
        List<String> CheckCode = new ArrayList<>();
        List<String> pubtsCode = new ArrayList<>();

        Map<String, Date> pubtsMap = new HashMap<>();
        List<String> ids = cacheBillPubts(pubtsMap, rows);

        QuerySchema querySchema = QuerySchema.create().addSelect("*");
        querySchema.addCondition(QueryConditionGroup.and(QueryCondition.name("id").in(ids)));
        List<BizObject> bankBillCheckList = MetaDaoHelper.queryObject(BankBillCheck.ENTITY_NAME, querySchema, null);

        for (BizObject bankBillCheckObj : bankBillCheckList) {
            BankBillCheck bankBillCheck = new BankBillCheck();
            BeanUtil.copyProperties(bankBillCheckObj, bankBillCheck, false);
            // 校验pubts
            if (bankBillCheck.getPubts() != null && pubtsMap.get(bankBillCheck.getId().toString()) != null
                    && bankBillCheck.getPubts().compareTo(pubtsMap.get(bankBillCheck.getId().toString())) != 0) {
                pubtsCode.add("【" + bankBillCheck.getCheckBillCode() + "】");//@notranslate
            }
            if (bankBillCheck.getInstructStatus() == 2 || bankBillCheck.getInstructStatus() == 3) {
                instructCode.add("【" + bankBillCheck.getCheckBillCode() + "】");//@notranslate
            } else if (bankBillCheck.getVerifystate() == 1 || bankBillCheck.getVerifystate() == 2 || bankBillCheck.getVerifystate() == 3) {
                verifystateCode.add("【" + bankBillCheck.getCheckBillCode() + "】");//@notranslate
            } else if (!(bankBillCheck.getCheckStatus() == 0 || bankBillCheck.getCheckStatus() == 3)) {
                CheckCode.add("【" + bankBillCheck.getCheckBillCode() + "】");//@notranslate
            } else {
                updateBankBillCheck.add(bankBillCheck);
            }
        }
        //组装返回值
        if (CollectionUtils.isNotEmpty(pubtsCode)) {
            String pubtsCodeNew = pubtsCode.stream().collect(Collectors.joining("\\"));
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("对账单编号").append(pubtsCodeNew).append(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805EB","数据无效，请刷新后重试") /* "数据无效，请刷新后重试" */);
            message.add(stringBuilder.toString());
        }
        if (CollectionUtils.isNotEmpty(instructCode)) {
            String instructCodeNew = instructCode.stream().collect(Collectors.joining("\\"));
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054004FF", "对账单编号") /* "对账单编号" */).append(instructCodeNew).append(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400515", "指令状态为“已发送\\发送成功”，不允许进行确认，请检查！") /* "指令状态为“已发送\\发送成功”，不允许进行确认，请检查！" */);
            message.add(stringBuilder.toString());
        }
        if (CollectionUtils.isNotEmpty(verifystateCode)) {
            StringBuilder stringBuilder = new StringBuilder();
            String verifystateCodeNew = verifystateCode.stream().collect(Collectors.joining("\\"));
            stringBuilder.append(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054004FF", "对账单编号") /* "对账单编号" */).append(verifystateCodeNew).append(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400500", "审批状态非“初始开立\\驳回到制单”，不允许进行确认，请检查！") /* "审批状态非“初始开立\\驳回到制单”，不允许进行确认，请检查！" */);
            message.add(stringBuilder.toString());
        }
        if (CollectionUtils.isNotEmpty(CheckCode)) {
            StringBuilder stringBuilder = new StringBuilder();
            String CheckCodeNew = CheckCode.stream().collect(Collectors.joining("\\"));
            stringBuilder.append(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054004FF", "对账单编号") /* "对账单编号" */).append(CheckCodeNew).append(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400502", "对账状态非“未对账或对账失败”，不允许进行确认，请检查！") /* "对账状态非“未对账或对账失败”，不允许进行确认，请检查！" */);
            message.add(stringBuilder.toString());
        }
    }

    /**
     * 构建银企联对账信息查询参数 并进行查询
     *
     * @param
     * @throws Exception
     */
    private CtmJSONArray buildAndQueryUnNeedUkey(CtmJSONObject param, EnterpriseBankAcctVO bankAccount, StringBuilder message, CurrencyTenantDTO currency) throws Exception{
        CtmJSONArray resultArray = new CtmJSONArray();
        String customNo = null;
        Map<String, Object> accountSeting = getBankAccountSetting(bankAccount.getId());
        if (accountSeting != null && accountSeting.get("customNo") != null) {
            customNo = accountSeting.get("customNo").toString();
        } else {
            //throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102359"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A050800EC", "选择的企业银行账户：%s没有开通银企联，无法查询对账信息") /* "选择的企业银行账户：%s没有开通银企联，无法查询对账信息" */, bankAccount.getAccount()));
            return new CtmJSONArray();
        }
        param.put("customNo", customNo);
        CtmJSONObject queryMsg = buildBankBillQueryMsgUnNeedUkey(param, bankAccount, currency.getCode());
        String signMsg = bankConnectionAdapterContext.chanPaySignMessage(queryMsg.toString());
        List<BasicNameValuePair> requestData = new ArrayList<>();
        requestData.add(new BasicNameValuePair("reqData", queryMsg.toString()));
        requestData.add(new BasicNameValuePair("reqSignData", signMsg));
        log.error("======拉取对账信息请求参数=======>" + queryMsg.toString());
        String transCode = ITransCodeConstant.QUERY_BANK_BILL_CHECK;
        String chanPayUri = bankConnectionAdapterContext.getChanPayUri();
        CtmJSONObject result = CtmJSONObject.toJSON(HttpsUtils.doHttpsPost(transCode, requestData, chanPayUri));
        log.error("=======拉取对账信息result======>" + CtmJSONObject.toJSONString(result));
        HttpsUtils.saveYQLBusinessLog(transCode, chanPayUri, queryMsg, result, param, bankAccount.getAccount());
        if (result.getInteger("code") == 1) {
            CtmJSONObject responseHead = result.getJSONObject("data").getJSONObject("response_head");
            String serviceStatus = responseHead.getString("service_status");
            String service_resp_code = responseHead.getString("service_resp_code");
            if (("00").equals(serviceStatus) && SERVICE_RESP_CODE.equals(service_resp_code)) {
                CtmJSONObject responseBody = result.getJSONObject("data").getJSONObject("response_body");
                String accEntityId = bankAccount.getOrgid();
                Map<String, Object> enterpriseInfo = new HashMap<>();
                enterpriseInfo.put("accEntityId", accEntityId);
                enterpriseInfo.put("accountId", bankAccount.getId());
//                enterpriseInfo.put("currencyId", bankAccount.get("currency"));
                enterpriseInfo.put("customNo", customNo);
                //插入或修改数据
                CtmJSONArray jsonArray = buildData(enterpriseInfo, responseBody, currency, message);
                if (jsonArray.size() > 0) {
                    resultArray.addAll(jsonArray);
                    String nextPage = responseBody.getString("next_page");
                    if ("1".equals(nextPage)) {
                        int begNum = param.getInteger("begNum");
                        param.put("begNum", begNum + responseBody.getInteger("back_num"));
                        param.put("queryExtend", responseBody.get("query_extend"));
                        //param.put("nextPage", true);
                        buildAndQueryUnNeedUkey(param, bankAccount, message, currency);
                    } else {
                        param.put("begNum", 1);
                    }
                } else {
                    message.append(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054004FD", "账号：%s币种：%s获取对账信息失败，失败原因：%s") /* "账号：%s币种：%s获取对账信息失败，失败原因：%s" */, bankAccount.getAccount(), currency.getName(), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054004FE", "银企联返回back_num为空!") /* "银企联返回back_num为空!" */ + YQLUtils.CONTACT_YQL_TIP));
                }
            } else {
                message.append(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054004FD", "账号：%s币种：%s获取对账信息失败，失败原因：%s") /* "账号：%s币种：%s获取对账信息失败，失败原因：%s" */, bankAccount.getAccount(), currency.getName(), responseHead.getString("service_resp_desc") + YQLUtils.CONTACT_YQL_TIP));
            }
        } else {
            message.append(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054004FD", "账号：%s币种：%s获取对账信息失败，失败原因：%s") /* "账号：%s币种：%s获取对账信息失败，失败原因：%s" */, bankAccount.getAccount(), currency.getName(), result.getString("message")));
        }
        return resultArray;
    }

    /**
     * 构建待落库数据
     *
     * @param enterpriseInfo
     * @param responseBody
     * @param currencyDTO
     * @param message
     */
    private CtmJSONArray buildData(Map<String, Object> enterpriseInfo, CtmJSONObject responseBody, CurrencyTenantDTO currencyDTO, StringBuilder message) throws Exception {
        List<BankBillCheck> insertList = new ArrayList<>();
        List<BankBillCheck> updateList = new ArrayList<>();
        //用来存储银企联返回的record，判断本次拉取是否拉取到数据
        CtmJSONArray jsonArray = new CtmJSONArray();
        //获取币种缓存数据
        //String currency = currencyQueryService.getCurrencyByAccount((String) enterpriseInfo.get("accountId"));
        Integer backNum = responseBody.getInteger("back_num");
        if (backNum == null) {
            return jsonArray;
        }
        if (backNum > 0) {
            CtmJSONArray records = responseBody.getJSONArray("record");
            jsonArray.addAll(records);
            for (int i = 0; i < records.size(); i++) {
                CtmJSONObject detailData = records.getJSONObject(i);
                analysisBankBillCheckData(detailData, enterpriseInfo, insertList, updateList, currencyDTO);
            }
        }
        if (CollectionUtils.isNotEmpty(insertList)){
            CmpMetaDaoHelper.insert(BankBillCheck.ENTITY_NAME,insertList);
        }
        if (CollectionUtils.isNotEmpty(updateList)){
            MetaDaoHelper.update(BankBillCheck.ENTITY_NAME,updateList);
        }
        return jsonArray;
    }

    /**
     * 解析银企对账单信息
     * @param detailData
     * @param enterpriseInfo
     * @param insertList
     * @param currencyDTO
     */
    private void analysisBankBillCheckData(CtmJSONObject detailData, Map<String, Object> enterpriseInfo, List<BankBillCheck> insertList,List<BankBillCheck> updateList, CurrencyTenantDTO currencyDTO) throws Exception{
        String uniqueNo = detailData.getString("unique_no");
        QuerySchema querySchema = QuerySchema.create().addSelect("id");
        querySchema.appendQueryCondition(QueryCondition.name("checkBillSign").eq(uniqueNo));
        querySchema.appendQueryCondition(QueryCondition.name("checkStatus").in(1, 4));
        //根据对账单标识码进行判重、对账状态=“已对账”或“无需对账”时不落库
        List<Map<String, Object>> isInsertDB = MetaDaoHelper.query(BankBillCheck.ENTITY_NAME, querySchema, null);
        if (CollectionUtils.isNotEmpty(isInsertDB)) {
            return;
        }
        QuerySchema querySchema1 = QuerySchema.create().addSelect("*");
        querySchema1.appendQueryCondition(QueryCondition.name("checkBillSign").eq(uniqueNo));
        List<BizObject> BankBillCheckList = MetaDaoHelper.queryObject(BankBillCheck.ENTITY_NAME, querySchema1, null);
        //为空则需要插入数据
        if (CollectionUtils.isEmpty(BankBillCheckList)) {
            BankBillCheck bankBillCheck = new BankBillCheck();
            bankBillCheck.setId(ymsOidGenerator.nextId());
            bankBillCheck.setTenant(AppContext.getTenantId());
            //对账单唯一标识
            bankBillCheck.setCheckBillSign(uniqueNo);
            //对账单编号
            if (StringUtils.isNotEmpty(detailData.getString("statement_no"))) {
                bankBillCheck.setCheckBillCode(detailData.getString("statement_no"));
            }
            bankBillCheck.setAccentity((String) enterpriseInfo.get("accEntityId"));
            bankBillCheck.setEnterpriseBankAccount((String) enterpriseInfo.get("accountId"));
           /* String currencyCode = detailData.getString("curr_code");
            if (StringUtils.isNotEmpty(currencyCode)) {
                currency = currencyQueryService.getCurrencyByCode(currencyCode);
                bankBillCheck.setCurrency(currency);
            } else {
                bankBillCheck.setCurrency((String) currency);
            }*/
            bankBillCheck.setCurrency(currencyDTO.getId());
            //账单开始日期
            if (detailData.get("beg_date") != null && detailData.getString("beg_date").length() == 8) {
                bankBillCheck.setBeginDate(DateUtils.dateParse(detailData.getString("beg_date"), DateUtils.YYYYMMDD));
            } else if (detailData.get("beg_date") != null && detailData.getString("beg_date").length() == 6) {
                bankBillCheck.setBeginDate(DateUtils.dateParse(detailData.getString("beg_date") + "01", DateUtils.YYYYMMDD));
            }

            //账单结束日期
            if (detailData.get("end_date") != null) {
                if (detailData.getString("end_date").length() == 8) {
                    bankBillCheck.setEndDate(DateUtils.dateParse(detailData.getString("end_date"), DateUtils.YYYYMMDD));
                } else if (detailData.getString("end_date").length() == 6) {
                    bankBillCheck.setEndDate(DateUtils.dateParse(detailData.getString("end_date") + "01", DateUtils.YYYYMMDD));
                }
            }

            //账单生成日期
            if (detailData.get("check_date") != null) {
                if (detailData.getString("check_date").length() == 8) {
                    bankBillCheck.setBillCreateDate(DateUtils.dateParse(detailData.getString("check_date"), DateUtils.YYYYMMDD));
                } else if (detailData.getString("check_date").length() == 6) {
                    bankBillCheck.setBillCreateDate(DateUtils.dateParse(detailData.getString("check_date") + "01", DateUtils.YYYYMMDD));
                }
            }

            //对账日期
            if (detailData.get("statement_date") != null) {
                if (detailData.getString("statement_date").length() == 8) {
                    bankBillCheck.setCheckDate(DateUtils.dateParse(detailData.getString("statement_date"), DateUtils.YYYYMMDD));
                } else if (detailData.getString("statement_date").length() == 6) {
                    bankBillCheck.setCheckDate(DateUtils.dateParse(detailData.getString("statement_date") + "01", DateUtils.YYYYMMDD));
                }
            }

            //银行余额
            BigDecimal bankBalance = detailData.get("bank_balance") != null ? detailData.getBigDecimal("bank_balance") : null;
            bankBillCheck.setBankBalance(bankBalance);

            //对账状态
            bankBillCheck.setCheckStatus(new Short(detailData.getString("statement_status")));

            //默认开立态
            bankBillCheck.setVerifystate(new Short("0"));
            //指令状态，默认 空
            bankBillCheck.setInstructStatus(new Short("0"));

            //是否审批流控制
            ProcessDefinitionQueryParam param = new ProcessDefinitionQueryParam();
            param.setCategory("");
            param.setBillTypeId(ICmpConstant.CM_CMP_BANKBILLCHECKLIST);
            param.setOrgId((String) enterpriseInfo.get("accEntityId"));
            RepositoryService repositoryService = processService.bpmRestServices().getRepositoryService();
            Object result = repositoryService.checkProcessDefinition(param);
            if (!((ObjectNode) result).get("hasProcessDefinition").booleanValue()) {
                bankBillCheck.setIsWfControlled(false);
            } else {
                bankBillCheck.setIsWfControlled(true);
            }

            //设置创建人、创建时间
            bankBillCheck.setCreatorId(AppContext.getCurrentUser().getId());
            bankBillCheck.setCreateDate(new Date());
            bankBillCheck.setCreateTime(new Date());
            bankBillCheck.setEntityStatus(EntityStatus.Insert);

            insertList.add(bankBillCheck);
        }else {
            //如果对账状态不一致则修改对账状态
            BizObject bankBillCheckObj = BankBillCheckList.get(0);
            BankBillCheck bankBillCheck = new BankBillCheck();
            BeanUtil.copyProperties(bankBillCheckObj, bankBillCheck, false);
            String checkStatus = bankBillCheck.getCheckStatus().toString();
            if (!detailData.getString("statement_status").equals(checkStatus)) {
                bankBillCheck.setEntityStatus(EntityStatus.Update);
                bankBillCheck.setCheckStatus(detailData.getShort("statement_status"));
                updateList.add(bankBillCheck);
            }
        }
    }

    /**
     * @return com.yonyou.yonbip.ctm.json.CtmJSONObject
     * @Author yangjn
     * @Description 在无ukey情况下 构建电子对账单报文
     * @Date 2028/8/12
     * @Param [params, bankAccount]
     **/
    private CtmJSONObject buildBankBillQueryMsgUnNeedUkey(CtmJSONObject params, EnterpriseBankAcctVO bankAccount, String currency) throws Exception {
        String requestseqno = buildRequestSeqNo(params.get("customNo").toString());
        CtmJSONObject requestHead = buildRequloadestHead(ITransCodeConstant.QUERY_BANK_BILL_CHECK,
                null,
                params.get("customNo").toString(),
                requestseqno,
                null);
        CtmJSONObject requestBody = new CtmJSONObject();
        requestBody.put("acct_name", bankAccount.getAcctName());
        requestBody.put("acct_no", bankAccount.getAccount());
        // 币种，新增接口必输项
        requestBody.put("curr_code", currency);
        String startDate = params.getString("startDate").replaceAll("-", "").substring(0, 8);
        String endDate = params.getString("endDate").replaceAll("-", "").substring(0, 8);
        requestBody.put("beg_date", startDate);
        requestBody.put("end_date", endDate);
        requestBody.put("tran_status", "00");
        requestBody.put("beg_num", params.get("begNum"));
        requestBody.put("query_num", 100);
        requestBody.put("query_extend", params.get("queryExtend"));
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
    private String buildRequestSeqNo(String customNo) {
        StringBuilder tranSeqNo = new StringBuilder("R");
        tranSeqNo.append(customNo);
        tranSeqNo.append("0000");
        tranSeqNo.append(DateTimeFormatter.ofPattern(DateUtils.MILLISECOND_PATTERN).format(LocalDateTime.now()));
        tranSeqNo.append(YQLUtils.getSerialNumberNoCAS(cardinalNumber));
        return tranSeqNo.toString();
    }

    private CtmJSONObject buildRequloadestHead(String transCode, String oper, String customNo, String requestseqno, String signature) {
        CtmJSONObject requestHead = new CtmJSONObject();
        //银企对账相关：目前版本 2.1.0
        requestHead.put("version", "2.1.0");
        requestHead.put("request_seq_no", requestseqno);
        requestHead.put("cust_no", customNo);
        requestHead.put("cust_chnl", AppContext.getBean(BankConnectionAdapterContext.class).getChanPayCustomChanel());
        LocalDateTime dateTime = LocalDateTime.now();
        requestHead.put("request_date", DateTimeFormatter.ofPattern(DateUtils.YYYYMMDD).format(dateTime));
        requestHead.put("request_time", DateTimeFormatter.ofPattern(DateUtils.HHMMSS).format(dateTime));
        requestHead.put("oper_sign", signature);
        requestHead.put("tran_code", transCode);
        requestHead.put("oper", oper);
        return requestHead;
    }

    /*
     * @Author tongyd
     * @Description 获取客户号
     * @Date 2019/9/12
     * @Param [bankAccountId]
     * @return java.util.Map<java.lang.String,java.lang.Object>
     **/
    private Map<String, Object> getBankAccountSetting(Object bankAccountId) throws Exception {
        QuerySchema schema = QuerySchema.create().addSelect("openFlag,customNo");
        QueryConditionGroup conditionGroup = QueryConditionGroup.and(
                QueryCondition.name("enterpriseBankAccount").eq(bankAccountId));
        //开通银企联服务；1-是
        conditionGroup.appendCondition(QueryCondition.name("openFlag").eq("1"));
        //状态[从企业银行账户是否启用同步过来的字段]；0-启用
        conditionGroup.appendCondition(QueryCondition.name("accStatus").eq("0"));
        schema.addCondition(conditionGroup);
        return MetaDaoHelper.queryOne(BankAccountSetting.ENTITY_NAME, schema);
    }

    /**
     * 组装银企联账号信息
     * @param params
     * @return
     * @throws Exception
     */
    private List<EnterpriseBankAcctVO> getAccount(CtmJSONObject params) throws Exception {
        List<String> accentitys = (List<String>) params.get("accEntity");
        List<String> accountIds = (List<String>) params.get("accountId");
        List<String> currencys = (List<String>) params.get("currency");
        String bankType = params.getString("bankType");
        EnterpriseParams enterpriseParams = new EnterpriseParams();
        //如果有账号id则查出账号、查出币种、名称
        if (CollectionUtils.isNotEmpty(accountIds)) {
            enterpriseParams.setIdList(accountIds);
        } else {
            //如果没有账号id则查出账户使用组织下的账号
            if (StringUtils.isNotEmpty(bankType)) {
                enterpriseParams.setBank(bankType);
            }
            if (CollectionUtils.isNotEmpty(currencys)) {
                enterpriseParams.setCurrencyIDList(currencys);
            }
            enterpriseParams.setOrgidList(accentitys);
        }
        enterpriseParams.setPageSize(4500);
        List<EnterpriseBankAcctVO> enterpriseBankAcctVOS = baseRefRpcService.queryEnterpriseBankAcctByCondition(enterpriseParams);
        if (CollectionUtils.isEmpty(enterpriseBankAcctVOS)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102360"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A050800EA", "根据所选条件未查询到账号") /* "根据所选条件未查询到账号" */);
        }
        List<String> accountIdParam = enterpriseBankAcctVOS.stream().map(i -> i.getId()).collect(Collectors.toList());
        //查询开通银企联的账号
        List<Map<String, Object>> mapList = getOpenFlagAccount(accountIdParam);
        if (CollectionUtils.isEmpty(mapList)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102356"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A050800E7", "选择的企业银行账户没有开通银企联，无法查询银企对账信息") /* "选择的企业银行账户没有开通银企联，无法查询银企对账信息" */);
        }
        //过滤出开通银企联的账号
        List<EnterpriseBankAcctVO> enterpriseBankAccountList = enterpriseBankAcctVOS.stream().filter(a -> mapList.stream().anyMatch(b -> a.getId().equals(b.get("enterpriseBankAccount").toString()))).collect(Collectors.toList());
        return enterpriseBankAccountList;
    }


    /**
     * 查询开通银企直联的账号
     * @param enterpriseBankAccounts
     * @throws Exception
     */
    private List<Map<String, Object>> getOpenFlagAccount(List<String> enterpriseBankAccounts) throws Exception {
        List<Map<String, Object>> retData = new ArrayList<>();
        int batchcount = 200;
        int listSize = enterpriseBankAccounts.size();
        int totalTask = (listSize % batchcount == 0 ? listSize / batchcount : (listSize / batchcount) + 1);
        for (int i = 0; i < totalTask; i++) {
            int fromIndex = i * batchcount;
            int toIndex = i * batchcount + batchcount;
            if (i + 1 == totalTask) {
                toIndex = listSize;
            }
            QuerySchema schema = QuerySchema.create().addSelect("*");
            QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
            conditionGroup.appendCondition(QueryCondition.name("openFlag").eq("1"));
            conditionGroup.appendCondition(QueryCondition.name("enterpriseBankAccount").in(enterpriseBankAccounts.subList(fromIndex, toIndex)));
            schema.addCondition(conditionGroup);
            List<Map<String, Object>> tmpData = MetaDaoHelper.query(BankAccountSetting.ENTITY_NAME, schema);
            retData.addAll(tmpData);
        }
        return retData;
    }

    /**
     * 构建id与pubts的关系
     * @param pubtsMap
     * @param paramMap
     * @return
     */
    private List<String> cacheBillPubts(Map<String, Date> pubtsMap, CtmJSONArray paramMap) {
        List<String> ids = new ArrayList<>();
        for (int i = 0; i < paramMap.size(); i++) {
            CtmJSONObject param = paramMap.getJSONObject(i);
            String id = param.getString("id");
            ids.add(id);
            pubtsMap.put(id, param.getDate("pubts"));
        }
        return ids;
    }

    private String getFirstDayOfLastMonth(){
        LocalDate currentDate = LocalDate.now();
        int year = currentDate.getYear();
        if(currentDate.getMonth().getValue() == 1){
            year --;
        }
        LocalDate firstDayOfLastMonth = LocalDate.of(year, currentDate.getMonth().minus(1), 1);
        return firstDayOfLastMonth.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));//上个月第一天
    }

    private String getLastDayOfLastMonth(){
        LocalDate currentDate = LocalDate.now();
        int year = currentDate.getYear();
        if(currentDate.getMonth().getValue() == 1){
            year --;
        }
        LocalDate lastDayOfLastMonth = LocalDate.of(year, currentDate.getMonth().minus(1), Month.of(currentDate.getMonth().minus(1).getValue()).length(currentDate.getYear() % 4 == 0));
        return lastDayOfLastMonth.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));//上个月第一天
    }

    private String getFirstDayOfMonthsInAdvance(String date, int monthsInAdvance){
        LocalDate currentDate = LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        int year = currentDate.getYear();
        if(monthsInAdvance >= currentDate.getMonth().getValue()){
            year --;
        }
        LocalDate firstDayOfLastMonth = LocalDate.of(year, currentDate.getMonth().minus(monthsInAdvance), 1);
        return firstDayOfLastMonth.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));//date前第monthsInAdvance月第一天
    }

    private String getLastDayOfMonthsInAdvance(String date, int monthsInAdvance){
        LocalDate currentDate = LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        int year = currentDate.getYear();
        if(monthsInAdvance >= currentDate.getMonth().getValue()){
            year --;
        }
        LocalDate lastDayOfLastMonth = LocalDate.of(year, currentDate.getMonth().minus(monthsInAdvance), Month.of(currentDate.getMonth().minus(monthsInAdvance).getValue()).length(currentDate.getYear() % 4 == 0));
        return lastDayOfLastMonth.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));//date前第monthsInAdvance月最后一天
    }
}
