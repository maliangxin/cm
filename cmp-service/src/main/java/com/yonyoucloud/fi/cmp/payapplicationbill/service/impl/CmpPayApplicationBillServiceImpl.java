package com.yonyoucloud.fi.cmp.payapplicationbill.service.impl;


import com.google.common.collect.Lists;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.logger.business.CTMCMPBusinessLogService;
import com.yonyoucloud.fi.cmp.constant.IMsgConstant;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import com.yonyoucloud.fi.cmp.fundcommon.service.IFundCommonService;
import com.yonyoucloud.fi.cmp.fundpayment.FundPayment_b;
import com.yonyoucloud.fi.cmp.payapplicationbill.CloseStatus;
import com.yonyoucloud.fi.cmp.payapplicationbill.PayApplicationBill;
import com.yonyoucloud.fi.cmp.payapplicationbill.service.CmpPayApplicationBillService;
import com.yonyoucloud.fi.cmp.util.EntityTool;
import com.yonyoucloud.fi.cmp.util.QueryBaseDocUtils;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import com.yonyoucloud.fi.cmp.vo.ResultMessageVO;
import lombok.extern.slf4j.Slf4j;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.imeta.orm.schema.SimpleCondition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <h1>付款申请单服务接口具体实现</h1>
 *
 * @author GuoCai Sun
 * @version 1.0
 * @since 2020-11-16 10:16
 */
@Service
@Slf4j
public class CmpPayApplicationBillServiceImpl implements CmpPayApplicationBillService {

    private CTMCMPBusinessLogService ctmcmpBusinessLogService;

    private IFundCommonService fundCommonService;

    @Autowired
    protected void setCtmcmpBusinessLogService(CTMCMPBusinessLogService ctmcmpBusinessLogService) {
        this.ctmcmpBusinessLogService = ctmcmpBusinessLogService;
    }

    @Autowired
    public void setFundCommonService(IFundCommonService fundCommonService) {
        this.fundCommonService = fundCommonService;
    }

    /**
     * <h2>付款单拉单成功后，调整预占金额的值</h2>
     *
     * @param payApplicationBillList : 付款申请单列表
     * @return com.yonyou.yonbip.ctm.json.CtmJSONObject
     * @author Sun GuoCai
     * @since 2020/11/16 10:14
     */
    @Override
    public void settle(List<PayApplicationBill> payApplicationBillList) throws Exception {
        EntityTool.setUpdateStatus(payApplicationBillList);
        MetaDaoHelper.update(PayApplicationBill.ENTITY_NAME, payApplicationBillList);
    }

    /**
     * <h2></h2>
     *
     * @param ids : 单据ID字符串
     * @param closedStatus : 关闭状态
     * @return com.yonyou.yonbip.ctm.json.CtmJSONObject
     * @author Sun GuoCai
     * @since 2021/8/23 19:09
     */
    @Override
    public ResultMessageVO updatePayApplicationBillStatusByClosed(String ids, String closedStatus) throws Exception {
        ResultMessageVO result = new ResultMessageVO();
        List<PayApplicationBill> payApplyBillList = Lists.newArrayList();
        log.info("closed pay Apply bill. ids : {}", ids);
        List<Map<String, Object>> mapList = MetaDaoHelper.queryById(PayApplicationBill.ENTITY_NAME, "*", ids);
        for (Map<String, Object> map : mapList) {
            PayApplicationBill payApplicationBill = new PayApplicationBill();
            payApplicationBill.init(map);
            if ("1".equals(closedStatus)) {
                payApplicationBill.setCloseStatus(CloseStatus.Closed);
            } else {
                payApplicationBill.setCloseStatus(CloseStatus.Normal);
            }
            payApplicationBill.setEntityStatus(EntityStatus.Update);
            payApplyBillList.add(payApplicationBill);
        }
        log.info("closed pay Apply bill. payApplyBillList :{}", payApplyBillList);
        MetaDaoHelper.update(PayApplicationBill.ENTITY_NAME, payApplyBillList);
        String message;
        if ("1".equals(closedStatus)) {
            message = com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807D4","单据关闭成功!") /* "单据关闭成功!" */;
            ctmcmpBusinessLogService.saveBusinessLog(payApplyBillList.get(0), payApplyBillList.get(0).getCode(), "", IServicecodeConstant.PAYAPPLICATIONBILL, IMsgConstant.PAY_APPLICATION, IMsgConstant.CLOSE);
        } else {
            message = com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807D6","单据打开成功!") /* "单据打开成功!" */;
            ctmcmpBusinessLogService.saveBusinessLog(payApplyBillList.get(0), payApplyBillList.get(0).getCode(), "", IServicecodeConstant.PAYAPPLICATIONBILL, IMsgConstant.PAY_APPLICATION, IMsgConstant.OPEN);
        }
        try {
            //yangjn 20230711 json标准化改在 这里前端没有对应接收代码 先注释掉
//            result.put("CtmJSONObject", CtmJSONObject.parseObject(JSON.toJSONString(payApplyBillList.get(0))));
        }catch (Exception e){
            log.error(e.getMessage());
        }
        result.setMsg(message);
        return result;
    }

    /**
     * <h2>OpenApi删除操作</h2>
     *
     * @param param : 入参
     * @return com.yonyou.yonbip.ctm.json.CtmJSONObject
     * @author Sun GuoCai
     * @since 2021/8/26 13:52
     */
    @Override
    public ResultMessageVO deletePayApplyBillByIds(BillDataDto param) throws Exception {
        List<Map<String, Long>> rows = (List)param.getData();
        List<String> messages = new ArrayList<>();
        int failedCount = 0;
        List<Object> ids = new ArrayList<>();
        Map<Long, PayApplicationBill> payBillMap = new HashMap<Long, PayApplicationBill>();
        for (int i = 0; i < rows.size(); i++) {
            Map<String, Long> rowData = rows.get(i);
            Long id = rowData.get("id");
            ids.add(id);
        }
        QuerySchema querySchema = QuerySchema.create().addSelect("*,payApplicationBill_b.*");
        QueryConditionGroup queryConditionGroup = new QueryConditionGroup(ConditionOperator.and);
        queryConditionGroup.appendCondition(QueryCondition.name("id").in(ids));
        querySchema.addCondition(queryConditionGroup);
        List<PayApplicationBill> payApplicationBillList = MetaDaoHelper.queryObject(PayApplicationBill.ENTITY_NAME, querySchema, null);
        for (PayApplicationBill payApplyBill : payApplicationBillList) {
            payBillMap.put(payApplyBill.getId(), payApplyBill);
        }
        List<Long> deleteIds = new ArrayList<>(rows.size());
        for (int i = 0; i < rows.size(); i++) {
            Map<String, Long> row = rows.get(i);
            PayApplicationBill payApplicationBill = payBillMap.get(row.get("id"));
            if (!ValueUtils.isNotEmptyObj(payApplicationBill)) {
                messages.add(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807D5","单据不存在 id:") /* "单据不存在 id:" */ + rows.get(i).get("id").toString());
                failedCount++;
                if (rows.size() == 1) {
                    return getJsonObject(rows, messages, failedCount);
                }
                continue;
            }
            deleteIds.add(payApplicationBill.getId());
        }
        if (ValueUtils.isNotEmptyObj(deleteIds)) {
            MetaDaoHelper.batchDelete(PayApplicationBill.ENTITY_NAME,
                    Lists.newArrayList(new SimpleCondition("id", ConditionOperator.in, deleteIds.toArray(new Long[0]))));
        }
        return getJsonObject(rows, messages, failedCount);
    }

    @Override
    public void payapplicationCopyBill(String accentity) throws Exception {
        List<Map<String, Object>> accentityList = QueryBaseDocUtils.queryAccRawEntityByAccEntityId(accentity);
        if (!org.apache.commons.collections4.CollectionUtils.isEmpty(accentityList)) {
            Object accEntityFlag = accentityList.get(0).get("stopstatus");
            if (accEntityFlag!=null && "1".equals(accEntityFlag.toString())){//0是启用，1是未启用
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100179"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807D7","会计主体未启用") /* "会计主体未启用" */);
            }
        } else {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100180"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807D3","未查询到对应的会计主体") /* "未查询到对应的会计主体" */);
        }
    }



    @Override
    public CtmJSONObject generatorFundCollectionBill(String id) {
        Map<String,Object> map = new HashMap<String,Object>();
        FundPayment_b fundPaymentB = null;
        try {
            fundPaymentB = MetaDaoHelper.findById(FundPayment_b.ENTITY_NAME, id, 2);
            return fundCommonService.fundPaymentBillCoordinatedGeneratorFundCollectionBill(fundPaymentB);
        } catch (Exception e) {
            map.put("id", id);
            map.put("message", "fund business object type is not accent"+ e.getMessage());
            map.put("data", CtmJSONObject.toJSONString(fundPaymentB));
            return new CtmJSONObject(map);
        }
    }

    private ResultMessageVO getJsonObject(List rows, List<String> messages, int failedCount) {
        ResultMessageVO responseData = new ResultMessageVO();
        responseData.setCount(rows.size());
        responseData.setSuccessCount(rows.size() - failedCount);
        responseData.setFailCount(failedCount);
        responseData.setMessages(messages);
        responseData.setInfos(rows);
        return responseData;
    }

}
