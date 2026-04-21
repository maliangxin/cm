package com.yonyoucloud.fi.cmp.ctmrpc;

import com.yonyou.ucf.mdd.common.enums.OperationTypeEnum;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.json.JsonUtils;
import com.yonyoucloud.fi.basecom.itf.IFIBillService;
import com.yonyoucloud.fi.cmp.api.openapi.OpenApiService;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.api.oidarch.CtmcmpReceiveRpcService;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.common.CommonRequestDataVo;
import com.yonyoucloud.fi.cmp.receivebill.ReceiveBill;
import lombok.extern.slf4j.Slf4j;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class CtmcmpReceiveRpcServiceImpl implements CtmcmpReceiveRpcService {

    @Autowired
    private OpenApiService openApiService;
    @Autowired
    private IFIBillService fiBillService;

    /**
     * 生成收款单
     *
     * @param param
     * @
     */
    @Override
    @Transactional
    public String receivebillCreateNew(CommonRequestDataVo param) throws Exception {
        CtmJSONArray billData = (CtmJSONArray) param.getData();
        BillDataDto billDataDto = new BillDataDto(IBillNumConstant.RECEIVE_BILL);
        for (int i = 0; i < billData.size(); i++) {
            billDataDto.setData(JsonUtils.toJSON(billData.getJSONObject(i)));
            fiBillService.executeUpdate(OperationTypeEnum.SAVE.getValue(), billDataDto);
        }
        return ResultMessage.success();
    }

    /**
     *
     *
     *
     * 收款单删除
     *
     * @param dataVo
     * @
     */

    @Transactional
    @Override
    public String deleteReceiveBillByIdsNew(CommonRequestDataVo dataVo) throws Exception {
        List<String> srcBillId = dataVo.getSrcpks();
        QuerySchema querySchemaReceiveBill = QuerySchema.create().addSelect("*");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("srcbillid").in(srcBillId));
        querySchemaReceiveBill.addCondition(conditionGroup);
        List<ReceiveBill> receiveBills = MetaDaoHelper.queryObject(ReceiveBill.ENTITY_NAME, querySchemaReceiveBill, null);
        BillDataDto billDataDto = new BillDataDto(IBillNumConstant.RECEIVE_BILL);
        // begin_zhengweih_20201019_商业汇票删除收款单提示已经审批不能删除。
        Map<String, Object> partParam = new HashMap<String, Object>();
        partParam.put("outsystem", "1");
        billDataDto.setPartParam(partParam);
//        log.info("**********外部系统删除收款单，参数={}，参数={}", srcBillId, receiveBills);
        // end_zhengweih_20201019_商业汇票删除收款单提示已经审批不能删除。
        for (ReceiveBill receiveBill : receiveBills) {
            billDataDto.setData(receiveBill);
            fiBillService.executeUpdate(OperationTypeEnum.DELETE.getValue(), billDataDto);
        }
        return ResultMessage.success();
    }


    public String queryReceiveBillStatusByIds(CtmJSONObject param)  throws Exception{
        try {
            return openApiService.queryReceiveBillStatusByIds(param);
        } catch (Exception e) {
            log.error("现金管理-收款单查询异常：" + e.getMessage(), e);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102271"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180070","现金管理-收款单查询异常：") /* "现金管理-收款单查询异常：" */ + e.getMessage());
        }
    }

    @Override
    public String queryReceiveBillStatusByIdsNew(CommonRequestDataVo dataVo) throws Exception {
        List<String> srcBillId = dataVo.getSrcpks();
        QuerySchema querySchemaReceiveBill = QuerySchema.create().addSelect("*");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("srcbillid").in(srcBillId));
        querySchemaReceiveBill.addCondition(conditionGroup);
        List<ReceiveBill> receiveBills = MetaDaoHelper.queryObject(ReceiveBill.ENTITY_NAME, querySchemaReceiveBill, null);
        return ResultMessage.data(receiveBills);
    }
}
