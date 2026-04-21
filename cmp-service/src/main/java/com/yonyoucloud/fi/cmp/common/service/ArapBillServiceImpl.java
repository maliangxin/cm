package com.yonyoucloud.fi.cmp.common.service;

import com.yonyou.diwork.model.ControlPointStatus;
import com.yonyou.diwork.model.dto.ControlPointDTO;
import com.yonyou.diwork.service.ILicenseQueryService;
import com.yonyou.diwork.service.IServiceManagerService;
import com.yonyou.iuap.bizdoc.service.model.SettleMethodModel;
import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.iuap.fileservice.sdk.module.CooperationFileService;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.workbench.model.ServiceVO;
import com.yonyou.yonbip.ctm.remote.RemoteDubbo;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.cmp.cmpentity.EventSource;
import com.yonyoucloud.fi.cmp.constant.IBussinessConstant;
import com.yonyoucloud.fi.cmp.constant.IDomainConstant;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import com.yonyoucloud.fi.cmp.journal.Journal;
import com.yonyoucloud.fi.cmp.settlement.service.SettlementService;
import com.yonyoucloud.fi.cmp.util.CmpWriteBankaccUtils;
import com.yonyoucloud.fi.cmp.util.StringUtils;
import com.yonyoucloud.fi.cmp.util.business.JournalUtil;
import lombok.extern.slf4j.Slf4j;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.orm.base.BizObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 应收应付单据校验
 *
 * @author maliang
 * @version V1.0
 * @date 2021/7/28 14:18
 * @Copyright yonyou
 */
@Service
@Slf4j
public class ArapBillServiceImpl implements ArapBillService {
    @Autowired
    BaseRefRpcService baseRefRpcService;
    @Autowired
    CooperationFileService cooperationFileService;
    @Autowired
    private SettlementService settlementService;
    @Autowired
    CmpWriteBankaccUtils cmpWriteBankaccUtils;
    @Autowired
    private IServiceManagerService iServiceManagerService;

    /**
     * 收款保存前流程
     *
     * @param billDataDto
     */
    public BizObject beforeSaveBillToCmp(BillDataDto billDataDto) {

        try {
            BizObject bizObject = ((List<BizObject>) billDataDto.getData()).get(0);
            Date maxSettleDate = null;
            maxSettleDate = settlementService.getMaxSettleDate(bizObject.get(IBussinessConstant.ACCENTITY));
            if (maxSettleDate != null) {
                Date compareDate = bizObject.get("vouchdate");
                if (maxSettleDate.compareTo(compareDate) >= 0) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101164"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041804A5", "单据日期已日结，不能保存单据！") /* "单据日期已日结，不能保存单据！" */);
                }
                if (bizObject.get("dzdate") != null && maxSettleDate.compareTo(bizObject.get("dzdate")) >= 0) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101165"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041804A6", "登账日期已日结，不能保存单据！") /* "登账日期已日结，不能保存单据！" */);
                }
            }
            // 对于应收应付的单据，为了在收款工作台能够显示，需设置cmpflag为true
            bizObject.set("cmpflag", true);
            bizObject.set("creator", AppContext.getCurrentUser().getName());
            bizObject.set("creatorId", AppContext.getCurrentUser().getId());
            bizObject.set("createTime", new Date());
            bizObject.set("createDate", new Date());
            if (bizObject.get("srcitem").equals(EventSource.StwbSettlement.getValue())) {
                bizObject.set("srcitem", EventSource.Manual.getValue());
            }
            return bizObject;
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101166"),e.getMessage());
        }
    }

    public void beforeSaveBillToCmp(BizObject bizObject) {

        try {
            Date maxSettleDate = null;
            maxSettleDate = settlementService.getMaxSettleDate(bizObject.get(IBussinessConstant.ACCENTITY));
            if (maxSettleDate != null) {
                Date compareDate = bizObject.get("vouchdate");
                if (maxSettleDate.compareTo(compareDate) >= 0) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101164"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041804A5", "单据日期已日结，不能保存单据！") /* "单据日期已日结，不能保存单据！" */);
                }
                if (bizObject.get("dzdate") != null && maxSettleDate.compareTo(bizObject.get("dzdate")) >= 0) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101165"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041804A6", "登账日期已日结，不能保存单据！") /* "登账日期已日结，不能保存单据！" */);
                }
            }
            // 对于应收应付的单据，为了在收款工作台能够显示，需设置cmpflag为true
            bizObject.set("cmpflag", true);
            bizObject.set("creator", AppContext.getCurrentUser().getName());
            bizObject.set("creatorId", AppContext.getCurrentUser().getId());
            bizObject.set("createTime", new Date());
            bizObject.set("createDate", new Date());
            if (bizObject.get("srcitem").equals(EventSource.StwbSettlement.getValue())) {
                bizObject.set("srcitem", EventSource.Manual.getValue());
            }
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101167"),e.getMessage());
        }
    }

    /**
     * 单据保存后流程
     *
     * @param bizObject
     * @param billContext
     */
    public void afterSaveBillToCmp(BillContext billContext, BizObject bizObject) {
        Journal journal = null;
        try {
            journal = JournalUtil.createJounal(bizObject, billContext);
            //应收应付单据把账户id
            if (StringUtils.isEmpty(journal.getBankaccount()) && StringUtils.isEmpty(journal.getCashaccount()) && bizObject.get("id") == null) {
                return;
            }

        } catch (Exception e) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101168"),e.getMessage());
        }
        //回退逻辑
        try {
            CmpWriteBankaccUtils.delAccountBook(bizObject.get("id").toString());
        } catch (Exception e) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101169"),e.getMessage());
        }
        try {
            cmpWriteBankaccUtils.addAccountBook(journal);
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101170"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B001DF", "单据保存后记账处理出错，应收应付单据同步现金管理失败！") /* "单据保存后记账处理出错，应收应付单据同步现金管理失败！" */);
        }
    }


    /**
     * 校验收付推送的单据结算类型是否可以在现金处理，可以则可以在现金保存，否则不保存，结算方式为空可以在现金保存
     *
     * @param settlemode
     * @return
     */
    public boolean checkSettleMode(Object settlemode) {
        if (settlemode == null) {
            return true;
        }
        SettleMethodModel settlementMap = baseRefRpcService.querySettleMethodsById(settlemode.toString());
        Integer serviceAttr = settlementMap.getServiceAttr(); // 0 - 银行业务 1 - 现金业务 仅需关注现金管理的类型即可
        if (serviceAttr != null && serviceAttr != 0 && serviceAttr != 1) {
            return false;
        }
        return true;
    }

    /**
     * 收付的单据推送时需要判断现金管理服务是否有效
     *
     * @return
     */
    @Override
    public boolean checkLisenceValid() {

        if (!"deskljz1".equals(InvocationInfoProxy.getTenantid())) {//非该租户不走该逻辑
            return true;
        }
        String validCache = AppContext.cache().get("ppm_valid" + InvocationInfoProxy.getTenantid());
        if ("1".equals(validCache)) {//lisence有效
            return true;
        } else if ("0".equals(validCache)) {//lisence无效
            return false;
        }
        List<String> ppms = new ArrayList<>();
        ppms.add("ppm0000055716");
        ppms.add("ppm0000081476");
        ppms.add("ppm0000085352");
        ppms.add("ppm0000089705");
        ppms.add("ppm0000072505");
        ppms.add("ppm0000064467");
        List<ControlPointDTO> controlPointDTOs = RemoteDubbo.get(ILicenseQueryService.class, IDomainConstant.MDD_WORKBENCH_SERVICE).getCurrentByCodes(InvocationInfoProxy.getTenantid(), ppms);
        if (controlPointDTOs.size() > 0) {
            for (ControlPointDTO controlPoint : controlPointDTOs) {
                if (controlPoint.getStatus() == ControlPointStatus.VALID) {
                    log.error("controlPoint.status ================" + controlPoint.getStatus());
                    AppContext.cache().set("ppm_valid" + InvocationInfoProxy.getTenantid(), "1", 60 * 60);
                    return true;
                }
            }
        }
        AppContext.cache().set("ppm_valid" + InvocationInfoProxy.getTenantid(), "0", 60 * 60);
        return false;
    }

    /**
     * 判断客户是否购买现金管理的收款工作台和付款工作台，若没有，则流程结束，收付推送的单据抛出异常提示
     *
     * @param billnum
     * @return
     * @throws Exception
     */
    @Override
    public void checkService(String billnum) throws Exception {
        String reFlag = AppContext.cache().get(billnum + "_" + InvocationInfoProxy.getTenantid());
        if ("0".equals(reFlag)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101171"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041804A4", "该租户未购买现金管理服务，请在业务单元期初设置中，删除现金管理启用期间。") /* "该租户未购买现金管理服务，请在业务单元期初设置中，删除现金管理启用期间。" */);
        }
        // 开通服务信息
        ServiceVO serviceVO = iServiceManagerService.findByTenantIdAndServiceCode(InvocationInfoProxy.getTenantid(), IServicecodeConstant.PAYMENTBILL);
        if (serviceVO == null) {
            AppContext.cache().set(billnum + "_" + InvocationInfoProxy.getTenantid(), "0", 10 * 60 * 60);//十小时
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101171"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041804A4", "该租户未购买现金管理服务，请在业务单元期初设置中，删除现金管理启用期间。") /* "该租户未购买现金管理服务，请在业务单元期初设置中，删除现金管理启用期间。" */);
        }
    }
}
