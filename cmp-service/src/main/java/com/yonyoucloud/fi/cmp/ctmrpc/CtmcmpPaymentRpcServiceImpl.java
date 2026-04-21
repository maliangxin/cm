package com.yonyoucloud.fi.cmp.ctmrpc;

import com.google.common.collect.Lists;
import com.yonyou.ucf.mdd.common.enums.OperationTypeEnum;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.template.CommonOperator;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyoucloud.fi.basecom.itf.IFIBillService;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.api.oidarch.CtmcmpPaymentRpcService;
import com.yonyoucloud.fi.cmp.api.openapi.OpenApiService;
import com.yonyoucloud.fi.cmp.constant.IBussinessConstant;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.common.CommonRequestDataVo;
import com.yonyoucloud.fi.cmp.paybill.PayBill;
import com.yonyoucloud.fi.cmp.settlement.Settlement;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import lombok.extern.slf4j.Slf4j;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.imeta.orm.schema.SimpleCondition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Slf4j
public class CtmcmpPaymentRpcServiceImpl implements CtmcmpPaymentRpcService {

    @Autowired
    private OpenApiService openApiService;
    @Autowired
    private IFIBillService fiBillService;

    /**
     * 外部服务，生成付款单
     * @param dataVo
     * @
     */
    @Override
    public String paybillCreateNew(CommonRequestDataVo dataVo) throws Exception {
        try {
            String decodeData = new String(Base64.getMimeDecoder().decode(dataVo.getData().toString()), "UTF-8");
            BillDataDto bill = new BillDataDto();
            bill.setBillnum(dataVo.getBillnum());
            bill.setData(decodeData);
            fiBillService.executeUpdate(OperationTypeEnum.SAVE.getValue(), bill);
            return ResultMessage.success();
        } catch (Exception e) {
            log.error("调用现金rpc接口生成单据异常：" + e.getMessage(), e);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100309"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805D6","现金管理生成单据异常：") /* "现金管理生成单据异常：" */+ e.getMessage());
        }
    }

    /**
     * 外部服务，根据来源删除付款单
     *
     * @param dataVo
     * @
     */
    @Override
    public String deletePaybillByIdsNew(CommonRequestDataVo dataVo) throws Exception {
        try {
            List<String> pks = dataVo.getSrcpks();
            QuerySchema querySchemaPayBill = QuerySchema.create().addSelect("*");
            QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("srcbillid").in(pks));
            querySchemaPayBill.addCondition(group);
            List<Map<String, Object>> result = MetaDaoHelper.query(PayBill.ENTITY_NAME, querySchemaPayBill);
            Set<String> srcBillidSet = new HashSet<String>();
            for (Map<String, Object> map : result) {
                PayBill payBill = new PayBill();
                payBill.init(map);
                if (payBill.getPaystatus().getValue() != 0 && payBill.getPaystatus().getValue() != 4
                        && payBill.getPaystatus().getValue() != 2) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100310"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180294","该单据已支付，不能修改或删除单据！") /* "该单据已支付，不能修改或删除单据！" */);
                }
                //已经日结的单据不能做修改删除
                QuerySchema querySchema = QuerySchema.create().addSelect("1");
                QueryConditionGroup group1 = QueryConditionGroup.and(QueryCondition.name(IBussinessConstant.ACCENTITY).eq(map.get(IBussinessConstant.ACCENTITY)), QueryCondition.name("settleflag").eq(1), QueryCondition.name("settlementdate").eq(map.get("vouchdate")));
                querySchema.addCondition(group1);
                List<Settlement> settlementList = MetaDaoHelper.query(Settlement.ENTITY_NAME, querySchema);
                if (ValueUtils.isNotEmpty(settlementList) && settlementList.size() > 0) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100311"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418029A","该单据已日结，不能修改或删除单据！") /* "该单据已日结，不能修改或删除单据！" */);
                }
                BillDataDto bill = new BillDataDto();
                bill.setBillnum("cmp_payment");
                bill.setData(payBill);
                Map<String, Object> partParam = new HashMap<String, Object>();
                partParam.put("outsystem", "1");
                bill.setPartParam(partParam);
                RuleExecuteResult e = (new CommonOperator(OperationTypeEnum.DELETE)).execute(bill);
                if (e.getMsgCode() != 1) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100312"),e.getMessage());
                }
            }
            MetaDaoHelper.batchDelete(PayBill.ENTITY_NAME, Lists.newArrayList(new SimpleCondition("srcbillid", ConditionOperator.in, pks)));
            return ResultMessage.data(dataVo);
        } catch (Exception e) {
            log.error("调用现金rpc接口删除付款单据异常：" + e.getMessage(), e);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100313"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805D7","现金管理-付款单删除异常：") /* "现金管理-付款单删除异常：" */+ e.getMessage());
        }
    }
}
