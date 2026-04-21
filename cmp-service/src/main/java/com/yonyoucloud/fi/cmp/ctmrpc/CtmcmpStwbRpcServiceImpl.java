package com.yonyoucloud.fi.cmp.ctmrpc;

import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.ctm.inner.rpc.stwb.CtmcmpStwbRpcService;
import com.yonyoucloud.ctm.inner.vo.common.CheckQueryReqVo;
import com.yonyoucloud.ctm.inner.vo.common.CheckQueryResVo;
import com.yonyoucloud.fi.cmp.checkstock.CheckStock;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import lombok.extern.slf4j.Slf4j;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 结算相关接口，财资内部使用
 */
@Service
@Slf4j
public class CtmcmpStwbRpcServiceImpl implements CtmcmpStwbRpcService {

    /**
     * 结算查询支票信息
     * @param checkQueryReqVo
     * @return
     * @throws Exception
     */
    @Override
    public List<CheckQueryResVo> queryCheckStockForStwb(CheckQueryReqVo checkQueryReqVo) throws Exception {
        QuerySchema schema = QuerySchema.create().addSelect(ICmpConstant.SELECT_TOTAL_PARAM);
        List<String> ids = checkQueryReqVo.getId();
        if (ids != null && ids.size() > 0) {
            QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("id").in(ids));
            schema.addCondition(group);
            List<Map<String, Object>> list = MetaDaoHelper.query(CheckStock.ENTITY_NAME, schema);
            return convertList(list);
        }
        List<String> checkNos = checkQueryReqVo.getCheckNo();
        if (checkNos != null && checkNos.size() > 0) {
            QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("checkBillNo").in(checkNos));
            schema.addCondition(group);
            List<Map<String, Object>> list = MetaDaoHelper.query(CheckStock.ENTITY_NAME, schema);
            return convertList(list);
        }
        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101773"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807C7","请求参数不能为空!") /* "请求参数不能为空!" */);
    }

    private List<CheckQueryResVo> convertList(List<Map<String, Object>> list) {
        List<CheckQueryResVo> result = new ArrayList<>();
        if (list == null || list.isEmpty()) {
            return result;
        }
        for (Map<String, Object> stringObjectMap : list) {
            CheckQueryResVo vo = doConvert(stringObjectMap);
            result.add(vo);
        }
        return result;
    }

    private CheckQueryResVo doConvert(Map<String, Object> map) {
        // 反射获取实体类字段，遍历赋值；map的字段，实体可能没有，所以要遍历实体字段，赋值
        CheckQueryResVo vo = new CheckQueryResVo();
        vo.setId(map.get("id").toString());
        vo.setCheckNo((String) map.get("checkNo"));
        vo.setAccentity((String) map.get("accentity"));
        vo.setCheckBillType(map.get("checkBillType") == null ? null : Short.valueOf(map.get("checkBillType").toString()));
        vo.setCheckBillStatus((String) map.get("checkBillStatus"));
        vo.setCustNo((String) map.get("custNo"));
        vo.setCheckBillDir((String) map.get("checkBillDir"));
        vo.setDrawerAcctNo((String) map.get("drawerAcctNo"));
        vo.setDrawerAcctName((String) map.get("drawerAcctName"));
        vo.setPayBank((String) map.get("payBank"));
        vo.setCurrency((String) map.get("currency"));
        vo.setAmount((BigDecimal) map.get("amount"));
        vo.setDrawerDate((Date) map.get("drawerDate"));
        vo.setDrawerName((String) map.get("drawerName"));
        vo.setPayeeName((String) map.get("payeeName"));
        vo.setCashDate((Date) map.get("cashDate"));
        vo.setCheckpurpose(map.get("checkpurpose") == null ? null : Short.valueOf(map.get("checkpurpose").toString()));
        vo.setCashType(map.get("cashType") == null ? null : Short.valueOf(map.get("cashType").toString()));
        vo.setCashPerson((String) map.get("cashPerson"));
        vo.setBusiDate((Date) map.get("busiDate"));
        vo.setCreater((String) map.get("creater"));
        vo.setCancelDate((Date) map.get("cancelDate"));
        vo.setCancelPerson((String) map.get("cancelPerson"));
        vo.setLossDate((String) map.get("lossDate"));
        vo.setLossPerson((String) map.get("lossPerson"));
        vo.setFailReason((String) map.get("failReason"));
        vo.setCheckBookNo((String) map.get("checkBookNo"));
        vo.setHandletype(map.get("handletype") == null ? null : Short.valueOf(map.get("handletype").toString()));
        vo.setDisposer((String) map.get("disposer"));
        vo.setDisposalDate((Date) map.get("disposalDate"));
        return vo;
    }

}
