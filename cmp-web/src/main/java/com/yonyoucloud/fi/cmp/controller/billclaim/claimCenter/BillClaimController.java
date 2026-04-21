package com.yonyoucloud.fi.cmp.controller.billclaim.claimCenter;

import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.iuap.org.service.itf.core.IFuncTypeQueryService;
import com.yonyou.iuap.yms.id.generator.YmsOidGenerator;
import com.yonyou.iuap.yms.lock.YmsLock;
import com.yonyou.ucf.mdd.ext.base.BaseController;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.dao.sql.SqlHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.aop.ButtonAuth;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankreconciliationService;
import com.yonyoucloud.fi.cmp.billclaim.BillClaim;
import com.yonyoucloud.fi.cmp.billclaim.BillClaimItem;
import com.yonyoucloud.fi.cmp.billclaim.dto.MessageResultVO;
import com.yonyoucloud.fi.cmp.billclaim.service.BillClaimService;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.api.bankrecilication.CmpBillClaimCenterRpcService;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.claimcenter.TotalClaimRequestVO;
import com.yonyoucloud.fi.cmp.util.CacheUtils;
import com.yonyoucloud.fi.cmp.util.JedisLockUtils;
import com.yonyoucloud.fi.cmp.util.StringUtils;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import com.yonyoucloud.fi.cmp.util.threadpool.ThreadPoolBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.biz.base.Objectlizer;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.base.Json;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * @description: 到账认领
 * @author: wanxbo@yonyou.com
 * @date: 2022/4/21 14:50
 */

@Controller
@RequestMapping("/billclaim")
@Slf4j
public class BillClaimController extends BaseController {

    @Autowired
    YmsOidGenerator ymsOidGenerator;

    @Resource
    private BillClaimService billClaimService;

    @Resource
    private IFuncTypeQueryService iFuncTypeQueryService;

    @Resource
    private CmpBillClaimCenterRpcService cmpBillClaimCenterRpcService;

    @Resource
    private BankreconciliationService bankreconciliationService;

    /**
     * 取消认领*
     *
     * @param bill
     * @param response
     */
    @PostMapping("/cancelclaim")
    public void publish(@RequestBody BillDataDto bill, HttpServletResponse response) throws Exception {
        Map<String, Object> params = (Map<String, Object>) bill.getData();
        String id = ValueUtils.isNotEmptyObj(params.get("id")) ? params.get("id").toString() : null;
        if (!ValueUtils.isNotEmptyObj(id)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100869"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180520", "操作的单据ID不能为空！") /* "操作的单据ID不能为空！" */);
        }
        renderJson(response, ResultMessage.data(billClaimService.cancelClaim(Long.parseLong(id))));
    }

    @PostMapping("/queryBillClaimInfo")
    public void queryBillClaimInfo(@RequestBody BillDataDto bill, HttpServletResponse response) throws Exception {
        Map<String, Object> params = (Map<String, Object>) bill.getData();
        String id = ValueUtils.isNotEmptyObj(params.get("id")) ? params.get("id").toString() : null;
        if (!ValueUtils.isNotEmptyObj(id)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100869"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180520", "操作的单据ID不能为空！") /* "操作的单据ID不能为空！" */);
        }
        renderJson(response, ResultMessage.data(billClaimService.queryBillClaimInfo(Long.parseLong(id))));
    }

    /**
     * 校验是否是同一天交易日期
     */
    @PostMapping("/checkIsSameTransDate")
    public void checkIsSameTransDate(@RequestBody BillDataDto bill, HttpServletResponse response) throws Exception {
        Map<String, Object> params = (Map<String, Object>) bill.getData();
        String id = ValueUtils.isNotEmptyObj(params.get("id")) ? params.get("id").toString() : null;
        if (!ValueUtils.isNotEmptyObj(id)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100870"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B0004A", "操作的单据ID不能为空！") /* "操作的单据ID不能为空！" */);
        }
        renderJson(response, ResultMessage.data(billClaimService.checkIsSameTransDate(Long.parseLong(id))));
    }

    /**
     * 现金管理租户下发失败，postman接口
     *
     * @param response
     */
    @PostMapping("/demotest")
    public void demoTest(HttpServletResponse response) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("tenantid", AppContext.getTenantId());
        map.put("ytenantid", AppContext.getYTenantId());
        long idDebit = ymsOidGenerator.nextId();
        long idCredit = ymsOidGenerator.nextId();
        map.put("idDebit", idDebit);
        map.put("idCredit", idCredit);
        Object obj = SqlHelper.selectFirst("com.yonyoucloud.fi.ficm.mapper.initficmMapper.getAutoCorrSetting", map);
        if (obj == null) {
            SqlHelper.insert("com.yonyoucloud.fi.ficm.mapper.initficmMapper.initAutoCorrSetting", map);
        }
        renderJson(response, ResultMessage.data("success!"));
    }

    @PostMapping("/queryBillClaimById")
    public void queryBillClaimById(@RequestBody BillDataDto bill, HttpServletResponse response) throws Exception {
        Map<String, Object> params = (Map<String, Object>) bill.getData();
        String id = ValueUtils.isNotEmptyObj(params.get("id")) ? params.get("id").toString() : null;
        if (!ValueUtils.isNotEmptyObj(id)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100869"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180520", "操作的单据ID不能为空！") /* "操作的单据ID不能为空！" */);
        }
        BillClaim billClaim = MetaDaoHelper.findById(BillClaim.ENTITY_NAME, Long.valueOf(id));
        renderJson(response, ResultMessage.data(billClaim));
    }

    @PostMapping("/queryBillClaimByIds")
    public void queryBillClaimByIds(@RequestBody List<CtmJSONObject> params, HttpServletResponse response) throws Exception {
        if (CollectionUtils.isEmpty(params)) {
            return;
        }
        List<String> ids = params.stream().map(item -> item.get("id").toString()).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(ids)) {
            return;
        }

        List<BillClaim> billClaims = new ArrayList<>();
        for (String id : ids) {
            // 需要查询返回关联子表
            BillClaim billClaim = MetaDaoHelper.findById(BillClaim.ENTITY_NAME, Long.valueOf(id));
            billClaims.add(billClaim);
        }
        renderJson(response, ResultMessage.data(billClaims));
    }

    /**
     * 复核
     *
     * @param param
     * @param request
     * @param response
     * @throws Exception
     */
    @ButtonAuth
    @RequestMapping("/recheck")
    public void recheck(@RequestBody CtmJSONObject param, HttpServletRequest request, HttpServletResponse response) throws Exception {
        CtmJSONArray rows = param.getJSONArray("data");
        Json json = new Json(CtmJSONObject.toJSONString(rows));
        //对前台传入数据进行转换封装
        List<BillClaim> billClaimResultes = Objectlizer.decode(json, BillClaim.ENTITY_NAME);
        //调用审核
        CtmJSONObject result = billClaimService.recheck(billClaimResultes);
        renderJson(response, ResultMessage.data(result));
    }

    /**
     * 取消复核
     *
     * @param param
     * @param request
     * @param response
     * @throws Exception
     */
    @ButtonAuth
    @RequestMapping("/unrecheck")
    public void unRecheck(@RequestBody CtmJSONObject param, HttpServletRequest request, HttpServletResponse response) throws Exception {
        CtmJSONArray rows = param.getJSONArray("data");
        Json json = new Json(CtmJSONObject.toJSONString(rows));
        //对前台传入数据进行转换封装
        List<BillClaim> billClaimResultes = Objectlizer.decode(json, BillClaim.ENTITY_NAME);
        //调用审核
        CtmJSONObject result = billClaimService.unRecheck(billClaimResultes);
        renderJson(response, ResultMessage.data(result));
    }

    /**
     * 项目编辑后赋值
     *
     * @param param
     * @param response
     */
    @PostMapping("/billClaimProjectEditAfter")
    public void billClaimProjectEditAfter(@RequestBody CtmJSONObject param, HttpServletResponse response) throws Exception {
        CtmJSONObject data = billClaimService.billClaimProjectEditAfter(param);
        renderJson(response, ResultMessage.data(data));
    }

    /**
     * 检查资金组织是否是结算中心
     *
     * @param params
     * @param response
     */
    @PostMapping("/checkAccentityIsSettlement")
    public void checkAccentityIsSettlement(@RequestBody Map<String, Object> params, HttpServletResponse response) {
        String accentity = (String) params.get("accentity");
        Boolean settlementFlag = iFuncTypeQueryService.orgHasFunc(InvocationInfoProxy.getTenantid().toString(), accentity, "settlementorg");
        renderJson(response, ResultMessage.data(settlementFlag));
    }

    /**
     * 使用组织确认
     *
     * @param obj
     * @param request
     * @param response
     */
    @PostMapping("/confirm")
    public void authorUseAccentityConfirm(@RequestBody CtmJSONObject obj, HttpServletRequest request, HttpServletResponse response) throws Exception {

        log.info("进入BillClaimController类的{}", "/authorUseAccentityConfirm");
        CtmJSONArray row = obj.getJSONArray("dataList");
        String accentity = (String) obj.get("accentity");
        String billnum = (String) obj.get("billnum");
        if (null == row || row.size() < 1) {
            throw new Exception(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B000B2", "请至少选择一条数据！") /* "请至少选择一条数据！" */);
        }
        CtmJSONObject lockResult = CacheUtils.lockBill(BankReconciliation.ENTITY_NAME, row);
        if (!lockResult.getBoolean("dealSucceed")) {
            renderJson(response, ResultMessage.error(lockResult.getString("message")));
        } else {
            try {
                CtmJSONArray hasLockRowData = lockResult.getJSONArray("hasLockRowData");
                CtmJSONArray lockRowData = lockResult.getJSONArray("lockRowData");
                Json json = new Json(lockRowData.toString());
                List<BankReconciliation> billList = Objectlizer.decode(json, BankReconciliation.ENTITY_NAME);
                CtmJSONObject result;
                if (IBillNumConstant.BANKRECONCILIATIONLIST.equals(billnum)) {
                    result = billClaimService.confirmFromBank(billList, accentity);
                } else {
                    result = billClaimService.confirm(billList, accentity);
                }
                renderJson(response, ResultMessage.data(result));
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                renderJson(response, ResultMessage.error(e.getMessage()));
            } finally {
                CacheUtils.unlockBill(BankReconciliation.ENTITY_NAME, lockResult.getJSONArray("lockRowData"));
            }
        }
    }

    /**
     * 进度条-使用组织确认
     *
     * @param obj
     * @param response
     */
    @PostMapping(value = "/async/confirm")
    public void asyncAuthorUseAccentityConfirm(@RequestBody CtmJSONObject obj, HttpServletResponse response) throws Exception {
        log.info("进入BillClaimController类的{}", "/asyncAuthorUseAccentityConfirm");
        CtmJSONArray row = obj.getJSONArray("dataList");
        String accentity = (String) obj.get("accentity");
        String billnum = (String) obj.get("billnum");
        String uid = (String) obj.getString("uid");
        if (null == row || row.size() < 1) {
            throw new Exception(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B000B2", "请至少选择一条数据！") /* "请至少选择一条数据！" */);
        }
        billClaimService.asyncConfirmFromBank(uid, row, accentity);
        renderJson(response, ResultMessage.data(null));
    }


    /**
     * 进度条-使用组织取消确认
     *
     * @param obj
     * @param response
     */
    @PostMapping(value = "/async/cancelconfirm")
    public void asyncAuthorUseAccentityCancelConfirm(@RequestBody CtmJSONObject obj, HttpServletResponse response) throws Exception {
        log.info("进入BillClaimController类的{}", "/asyncAuthorUseAccentityCancelConfirm");
        CtmJSONArray row = obj.getJSONArray("dataList");
        String accentity = (String) obj.get("accentity");
        String billnum = (String) obj.get("billnum");
        String uid = (String) obj.getString("uid");
        if (null == row || row.size() < 1) {
            throw new Exception(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B000B2", "请至少选择一条数据！") /* "请至少选择一条数据！" */);
        }
        billClaimService.asyncCancelConfirmFormBank(uid, row);
        renderJson(response, ResultMessage.data(null));
    }


    /**
     * 使用组织取消确认
     *
     * @param obj
     * @param request
     * @param response
     */
    @PostMapping("/cancelconfirm")
    public void authorUseAccentityCancelConfirm(@RequestBody CtmJSONObject obj, HttpServletRequest request, HttpServletResponse response) throws Exception {
        log.info("进入BillClaimController类的{}", "/authorUseAccentityCancelConfirm");
        CtmJSONArray row = obj.getJSONArray("dataList");
        String billnum = (String) obj.get("billnum");
        if (null == row || row.size() < 1) {
            throw new Exception(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B000B2", "请至少选择一条数据！") /* "请至少选择一条数据！" */);
        }
        CtmJSONObject lockResult = CacheUtils.lockBill(BankReconciliation.ENTITY_NAME, row);
        if (!lockResult.getBoolean("dealSucceed")) {
            renderJson(response, ResultMessage.error(lockResult.getString("message")));
        } else {
            try {
                CtmJSONArray lockRowData = lockResult.getJSONArray("lockRowData");
                Json json = new Json(lockRowData.toString());
                List<BankReconciliation> billList = Objectlizer.decode(json, BankReconciliation.ENTITY_NAME);
                CtmJSONObject result;
                if (IBillNumConstant.BANKRECONCILIATIONLIST.equals(billnum)) {
                    result = billClaimService.cancelConfirmFormBank(billList);
                } else {
                    result = billClaimService.cancelConfirm(billList);
                }
                renderJson(response, ResultMessage.data(result));
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                renderJson(response, ResultMessage.error(e.getMessage()));
            } finally {
                CacheUtils.unlockBill(BankReconciliation.ENTITY_NAME, lockResult.getJSONArray("lockRowData"));
            }
        }
    }

    /**
     * 取消单据关联
     *
     * @param bill
     * @param response
     */
    @PostMapping("/cancelCorrelate")
    public void cancelCorrelate(@RequestBody BillDataDto bill, HttpServletResponse response) throws Exception {
        Map<String, Object> params = (Map<String, Object>) bill.getData();
        String id = ValueUtils.isNotEmptyObj(params.get("id")) ? params.get("id").toString() : null;
        CtmJSONObject resultStrRespVO = billClaimService.cancelCorrelate(Long.valueOf(id));
        renderJson(response, ResultMessage.data(resultStrRespVO));
    }

    /**
     * 批量认领
     *
     * @param totalClaimRequestVOList
     * @param request
     * @param response
     * @throws Exception
     */
    @PostMapping("/totalClaimByBankSeqNo")
    public void totalClaimByBankSeqNo(@RequestBody List<TotalClaimRequestVO> totalClaimRequestVOList, HttpServletRequest request, HttpServletResponse response) throws Exception {
        //调用审核
        Map<String, Object> result =
                cmpBillClaimCenterRpcService.totalClaimByBankSeqNoNew(totalClaimRequestVOList, null);
        renderJson(response, ResultMessage.data(result.get("data")));
    }

    /**
     * 退回
     *
     * @param obj
     * @param request
     * @param response
     */
    @PostMapping("/return")
    public void returnBack(@RequestBody CtmJSONObject obj, HttpServletRequest request, HttpServletResponse response) throws Exception {
        log.info("进入BillClaimController类的{}", "/return");
        CtmJSONArray row = obj.getJSONArray("dataList");
        if (null == row || row.size() < 1) {
            throw new Exception(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B000B2", "请至少选择一条数据！") /* "请至少选择一条数据！" */);
        }
        CtmJSONObject lockResult = CacheUtils.lockBill(BankReconciliation.ENTITY_NAME, row);
        if (!lockResult.getBoolean("dealSucceed")) {
            renderJson(response, ResultMessage.error(lockResult.getString("message")));
        } else {
            try {
                CtmJSONArray lockRowData = lockResult.getJSONArray("lockRowData");
                Json json = new Json(lockRowData.toString());
                List<BankReconciliation> billList = Objectlizer.decode(json, BankReconciliation.ENTITY_NAME);
                CtmJSONObject result = bankreconciliationService.returnBack(billList, obj.getString("returnreason"));
                renderJson(response, ResultMessage.data(result));
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                renderJson(response, ResultMessage.error(e.getMessage()));
            } finally {
                CacheUtils.unlockBill(BankReconciliation.ENTITY_NAME, lockResult.getJSONArray("lockRowData"));
            }
        }
    }

    /**
     * 批量认领 到账认领中心-肩部按钮-批量认领
     *
     * @param param
     * @param request
     * @param response
     * @throws Exception
     */
    @RequestMapping("/massClaim")
    public void massClaim(@RequestBody CtmJSONObject param, HttpServletRequest request, HttpServletResponse response) throws Exception {
        String optType = param.getString("optType"); // 操作类型：保存(save)、保存并提交(saveAndSubmit)
        ArrayList bankSeqNos = param.getJSONArray("bankSeqNos");
        Map<String, String> bankSeqNoMap = new HashMap<>();
        bankSeqNos.forEach(tmp -> {
            Map temp = (Map) tmp;
            String bankbill = (String) temp.get("bankbill");
            String bankSeqNo = (String) temp.get("bankSeqNo");
            if (!StringUtils.isEmpty(bankbill)) {
                bankSeqNoMap.put(bankbill, bankSeqNo);
            }
        });
        CtmJSONArray rows = param.getJSONArray("data");
        Json json = new Json(CtmJSONObject.toJSONString(rows));
        // 数据解析
        List<BillClaim> billClaimList = Objectlizer.decode(json, BillClaim.ENTITY_NAME);
        BillContext billContext = new BillContext("cmp_billclaimcard", "cmp.billclaim.BillClaim");
        billContext.setSupportBpm(true);
        billContext.setTenant(InvocationInfoProxy.getYxyTenantid());
        billContext.setName(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B9A43A05D8007C", "我的认领") /* "我的认领" */);
        billContext.setCardKey("cmp_billclaimcard");
        billContext.setSubid("CM");
        billContext.setBilltype("Voucher");
        billContext.setEntityCode("cmp_billclaimcard");
        billContext.setMddBoId("ctm-cmp.cmp_billclaimcard");
        YmsLock ymsLock = null;
        ExecutorService executorService = ThreadPoolBuilder.defaultThreadPoolBuilder().builder(2, 10, 1000, "MassClaim-threadpool");
        try {
            ymsLock = JedisLockUtils.lockBillWithOutTraceByTime("massClaim_" + InvocationInfoProxy.getTenantid(), 60 * 10);
            if (ymsLock != null) { // 加锁成功
                MessageResultVO messageResultVO = new MessageResultVO();
                AtomicInteger successCount = new AtomicInteger();
                AtomicInteger failedCount = new AtomicInteger();
                Map<String, String> failed = new HashMap<>();
                List<String> messages = new ArrayList<>();
                List<CompletableFuture<Void>> futureList = new ArrayList<>();
                for (BillClaim billClaim : billClaimList) {
                    billClaim.set("_status", EntityStatus.Insert);
                    billClaim.set("id", ymsOidGenerator.nextId());
                    billClaim.setYtenantId(InvocationInfoProxy.getTenantid());
                    Map<String, Object> paramMap = new HashMap<>();
                    paramMap.put("param", billClaim);
                    BillDataDto billDataDto = new BillDataDto("cmp_billclaimcard");
                    billDataDto.setData(billClaim);
                    //                billDataDto.setParameters(BeanUtil.beanToMap(billClaim,true,true));
                    String bankSeqNo = "";
                    if (CollectionUtils.isNotEmpty(billClaim.<List<BillClaimItem>>get("items"))) {
                        billClaim.<List<BillClaimItem>>get("items").forEach(it -> it.set("id", ymsOidGenerator.nextId()));

                        bankSeqNo = bankSeqNoMap.get(String.valueOf(billClaim.<List<BillClaimItem>>get("items").get(0).getBankbill()));
                        if (StringUtils.isEmpty(bankSeqNo)) {
                            bankSeqNo = "";
                        }
                    }
                    String finalBankSeqNo = bankSeqNo;
                    CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                        try {
                            billClaimService.massClaim(billContext, billDataDto, optType);
                            successCount.getAndIncrement();
                        } catch (Exception e) {
                            failedCount.getAndIncrement();
                            failed.put(billClaim.getId(), billClaim.getId());
                            messages.add("[" + finalBankSeqNo + "]" + e.getMessage());
                            log.error(e.getMessage());
                        }
                    }, executorService);
                    futureList.add(future);
                }
                // 等待任务执行完成
                CompletableFuture.allOf(futureList.toArray(new CompletableFuture[0])).join();

                messageResultVO.setFailed(failed);
                messageResultVO.setMessages(messages);
                messageResultVO.setSucessCount(successCount.get());
                messageResultVO.setFailCount(failedCount.get());
                messageResultVO.setCount(billClaimList.size());
                renderJson(response, ResultMessage.data(messageResultVO));
            } else { // 加锁失败
                renderJson(response, ResultMessage.error(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180264", "单据都被锁定，请勿重复操作") /* "单据都被锁定，请勿重复操作" */));
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            renderJson(response, ResultMessage.error(e.getMessage()));
        } finally {
            if (executorService != null) {
                executorService.shutdown();
            }
            if (ymsLock != null) {
                ymsLock.unLock();
            }
        }
    }

    /**
     * 流水根据转换规则生成认领单
     *
     * @param params
     * @param response
     */
    @PostMapping("/transferBankReconciliationToClaim")
    public void transferBankReconciliationToClaim(@RequestBody Map<String, Object> params, HttpServletResponse response) throws Exception{
        List<BizObject> bills = (List<BizObject>)params.get("rows");
        Map<String,Object> map = billClaimService.supportTransferRuleAfterAddRule(bills);
        renderJson(response, ResultMessage.data(map));
    }

}
