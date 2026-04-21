package com.yonyoucloud.fi.cmp.foreignpayment.service;

import com.yonyou.cloud.utils.CollectionUtils;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.exceptions.BusinessException;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.ucf.mdd.ext.pub.rule.SaveBusinessLogRule;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.yonbip.ctm.logger.business.CTMCMPBusinessLogService;
import com.yonyoucloud.ctm.stwb.datasettled.DataSettled;
import com.yonyoucloud.ctm.stwb.openapi.IOpenApiService;
import com.yonyoucloud.ctm.stwb.openapi.QuerySettledDetailModel;
import com.yonyou.yonbip.ctm.remote.RemoteDubbo;
import com.yonyoucloud.fi.cmp.constant.IDomainConstant;
import com.yonyoucloud.fi.cmp.foreignpayment.ForeignPayment;
import com.yonyoucloud.fi.cmp.fundcommon.service.IFundCommonService;
import com.yonyoucloud.fi.cmp.fundcommon.service.StwbBillCommonService;
import com.yonyoucloud.fi.cmp.stwb.IForeignPaymentPushStwbBillService;
import com.yonyoucloud.fi.cmp.util.business.StwbBillBuilder;
import lombok.extern.slf4j.Slf4j;
import org.imeta.orm.base.BizObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@Transactional
public class StwbForeignPaymentBillServiceImpl extends StwbBillCommonService implements IForeignPaymentPushStwbBillService {
    @Resource
    IFundCommonService fundCommonService;
    @Autowired
    CTMCMPBusinessLogService ctmcmpBusinessLogService;

    // 外汇付款
    private static final String CMP_FOREIGNPAYMENT = "cmp_foreignpayment";

    /**
     * 审核推送外汇付款单据
     *
     * @param billList
     */
    @Override
    public void pushBill(List<BizObject> billList, boolean bCheck) throws Exception {
        if (billList == null) {
            return;
        }
        ForeignPayment foreignPayment = (ForeignPayment) billList.get(0);
        Map<String, Object> params = new HashMap<>();
        params.put("bCheck", bCheck);
        builtSystem(foreignPayment, StwbBillBuilder.builderForeignPayment(foreignPayment,params), CMP_FOREIGNPAYMENT);
    }

    @Override
    public void pushBillSimple(BizObject bizobject) throws Exception {

    }

    /**
     * 弃审删除外汇付款单据
     *
     * @param billList
     */
    @Override
    public void deleteBill(List<BizObject> billList) {
        if (billList == null) {
            return;
        }
        ForeignPayment foreignPayment = (ForeignPayment) billList.get(0);
        try {
            QuerySettledDetailModel querySettledDetailModel = new QuerySettledDetailModel();
            querySettledDetailModel.setBusinessId(foreignPayment.getId().toString());
            querySettledDetailModel.setWdataorigin(8);// 来源业务系统，现金管理
            List<String> ids = new ArrayList<>();
            ids.add(foreignPayment.getId().toString());
            querySettledDetailModel.setBusinessDetailsIds(ids);
            RemoteDubbo.get(IOpenApiService.class, IDomainConstant.MDD_DOMAIN_STWB).datasettledDelete(querySettledDetailModel);
        } catch (Exception e) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100347"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418076B", "删除结算单据失败：") /* "删除结算单据失败：" */ + e.getMessage());
        }
    }

//    @Override
    public void saveBusinessLog(List<DataSettled> dataSettleds) {
        if (CollectionUtils.isEmpty(dataSettleds)){
            return;
        }
        for (DataSettled dataSettled : dataSettleds){
            try {
                SaveBusinessLogRule bean = AppContext.getBean(SaveBusinessLogRule.class);
                BillContext billContext= new BillContext();
                billContext.setBillnum("stwb_datasettledlist");
                billContext.setAction("save");
                billContext.setFullname(DataSettled.ENTITY_NAME);
                Map<String, Object> paramMap = new HashMap<>();
                BillDataDto billDataDto = new BillDataDto();
                billDataDto.setData(dataSettled);
                paramMap.put("param",billDataDto);
                bean.execute(billContext,paramMap);
            } catch (Exception var8) {
                log.error("记录业务日志失败：" + var8.getMessage());
            }
        }
    }
}
