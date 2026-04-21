package com.yonyoucloud.fi.cmp.ctmrpc;

import com.alibaba.fastjson.JSON;
import com.yonyou.iuap.ucf.common.i18n.InternationalUtils;
import com.yonyou.iuap.yms.id.generator.YmsOidGenerator;
import com.yonyou.ucf.mdd.common.enums.OperationTypeEnum;
import com.yonyou.ucf.mdd.common.utils.json.GsonHelper;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.json.JsonUtils;
import com.yonyou.yonbip.ctm.logger.business.CTMCMPBusinessLogService;
import com.yonyoucloud.fi.basecom.itf.IFIBillService;
import com.yonyoucloud.fi.cm.transferaccount.TransferAccount;
import com.yonyoucloud.fi.cmp.api.openapi.OpenApiService;
import com.yonyoucloud.fi.cmp.cmpentity.AuditStatus;
import com.yonyoucloud.fi.cmp.cmpentity.EventSource;
import com.yonyoucloud.fi.cmp.cmpentity.VoucherStatus;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.api.transferaccount.CtmcmpTransferRpcService;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.common.CommonRequestDataVo;
import com.yonyoucloud.fi.cmp.util.CmpMetaDaoHelper;
import com.yonyoucloud.fi.cmp.util.CmpWriteBankaccUtils;
import com.yonyoucloud.fi.cmp.util.DateUtils;
import com.yonyoucloud.fi.cmp.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Service
@Slf4j
public class CtmcmpTransferRpcServiceImpl implements CtmcmpTransferRpcService {

    @Autowired
    CmpWriteBankaccUtils cmpWriteBankaccUtils;
    @Autowired
    private OpenApiService openApiService;
    @Autowired
    private IFIBillService fiBillService;
    @Autowired
    private CTMCMPBusinessLogService ctmcmpBusinessLogService;
    @Autowired
    YmsOidGenerator ymsOidGenerator;

    /**
     * 票据保证金生成转账单和日记账余额
     *
     * @param param
     * @
     */
    @Override
    public String createTransferNew(CommonRequestDataVo param) throws Exception {
        try {
            String decodeData = new String(Base64.getMimeDecoder().decode(CtmJSONObject.toJSONString(param.getData())), "UTF-8");
            BillDataDto bill = new BillDataDto();
            bill.setBillnum(param.getBillnum());
            bill.setData(decodeData);
            fiBillService.executeUpdate(OperationTypeEnum.SAVE.getValue(), bill);
            return ResultMessage.success();
        } catch (Exception e) {
            log.error("调用现金rpc接口转账单生成异常: " + e.getMessage(), e);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101915"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418000B", "现金管理-转账单生成异常") /* "现金管理-转账单生成异常" */ + e.getMessage());
        }
    }

    @Override
    public String saveTransferDraft(CommonRequestDataVo param) throws Exception {
        try {
            TransferAccount transferAccountVo = (TransferAccount) GsonHelper.FromJSon(JSON.toJSONString(param.getData()), TransferAccount.class);
            transferAccountVo.setVouchdate(DateUtils.parseDate(transferAccountVo.get("vouchdate")));
            transferAccountVo.setOriSum(new BigDecimal(transferAccountVo.get("oriSum").toString()));
            transferAccountVo.setNatSum(new BigDecimal(transferAccountVo.get("natSum").toString()));
            transferAccountVo.setAuditstatus(AuditStatus.Incomplete);
            if (transferAccountVo.getSrcitem() == null) {
                transferAccountVo.setSrcitem(EventSource.Cmpchase);
            }
            if (transferAccountVo.getId() == null) {
                transferAccountVo.setId(ymsOidGenerator.nextId());
            }
            if (transferAccountVo.getVoucherstatus() == null) {
                transferAccountVo.setVoucherstatus(VoucherStatus.Empty.getValue());
            }
            transferAccountVo.setVerifystate((short) 0);//初始开立
            StringBuilder errorMessage = new StringBuilder();
            if (transferAccountVo.getAccentity() == null) {
                errorMessage.append(InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418037C", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054003B2", "会计主体") /* "会计主体" */));
            }
            if (transferAccountVo.getTradetype() == null) {
                if (!StringUtils.isEmpty(errorMessage.toString())) {
                    errorMessage.append("、");//@notranslate
                }
                errorMessage.append(InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180758", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054003B4", "交易类型") /* "交易类型" */));
            }
            if (transferAccountVo.getCode() == null) {
                if (!StringUtils.isEmpty(errorMessage.toString())) {
                    errorMessage.append("、");//@notranslate
                }
                errorMessage.append(InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180756", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054003B5", "单据编号") /* "单据编号" */));
            }
            if (!StringUtils.isEmpty(errorMessage.toString())) {
                errorMessage.append("，");//@notranslate
                errorMessage.append(InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00068", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054003B3", ",请求必输参数为空") /* ",请求必输参数为空" */));
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101916"), errorMessage.toString());
            }
            transferAccountVo.setEntityStatus(EntityStatus.Insert);
            CmpMetaDaoHelper.insert(TransferAccount.ENTITY_NAME, transferAccountVo);
            return ResultMessage.success();
        } catch (Exception e) {
            log.error("调用现金rpc接口转账单生成异常: " + e.getMessage(), e);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101915"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418000B", "现金管理-转账单生成异常") /* "现金管理-转账单生成异常" */ + ":" + e.getMessage());
        }
    }

    /**
     * 票据保证金删除转账单和日记账余额
     *
     * @param param
     * @
     */
    @Override
    public String deleteTransferNew(CommonRequestDataVo param) throws Exception {
        try {
            List<String> srcpks = param.getSrcpks();
            QuerySchema querySchemaTrans = QuerySchema.create().addSelect("*");
            QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
            conditionGroup.appendCondition(QueryCondition.name("srcbillid").in(srcpks));
            querySchemaTrans.addCondition(conditionGroup);
            List<TransferAccount> result = MetaDaoHelper.queryObject(TransferAccount.ENTITY_NAME, querySchemaTrans, null);
            BillDataDto billDataDto = new BillDataDto("cm_transfer_account");
            // 从外部系统删除转账单
            Map<String, Object> partParam = new HashMap<String, Object>();
            partParam.put("outsystem", "1");
            billDataDto.setPartParam(partParam);
            for (TransferAccount transferAccount : result) {
                billDataDto.setData(transferAccount);
                fiBillService.executeUpdate(OperationTypeEnum.DELETE.getValue(), billDataDto);
            }
            return ResultMessage.success();
        } catch (Exception e) {
            log.error("调用现金rpc接口转账单删除异常: " + e.getMessage(), e);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101917"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418000D", "现金管理-转账单删除异常") /* "现金管理-转账单删除异常" */ + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418000C", "：") /* "：" */ + e.getMessage());
        }
    }
}
