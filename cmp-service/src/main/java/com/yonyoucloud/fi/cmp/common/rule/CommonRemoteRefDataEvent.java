package com.yonyoucloud.fi.cmp.common.rule;

import com.yonyou.cloud.utils.CollectionUtils;
import com.yonyou.ucf.mdd.common.dto.BaseReqDto;
import com.yonyou.ucf.mdd.common.model.ref.RefEntity;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.FilterVO;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.SimpleFilterVO;
import com.yonyou.ucf.mdd.ext.ref.service.IRefEvent;
import com.yonyoucloud.fi.cmp.autoparam.service.AutoConfigService;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.CmpCheckStatus;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.imeta.core.base.ConditionOperator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Map;

/**
 * 现金管理提供规则的公共过滤
 *
 * @author nff
 * @since 2024-04-28
 */
@Slf4j
@Component
public class CommonRemoteRefDataEvent implements IRefEvent {

    @Autowired
    AutoConfigService autoConfigService;

    @SneakyThrows
    @Override
    public void beforeGridData(RefEntity refEntity, Map<String, Object> map) {
        try {
            if (ICmpConstant.CMP_CHECKREF.equals(refEntity.getCBillnum())) {
                // 如果开启领用则判断支票状态是否为已领用，如果未开启领用则判断支票状态是否为已入库
                ArrayList<Map> list = (ArrayList<Map>) map.get(ICmpConstant.DATA);
                if (CollectionUtils.isNotEmpty(list) && ICmpConstant.STWB.equals(list.get(0).get(ICmpConstant.ENTITYNAME)) || ICmpConstant.STWBCHANGE.equals(list.get(0).get(ICmpConstant.ENTITYNAME))||
                        ICmpConstant.STWBCHANGE_NEW.equals(list.get(0).get("metaFullName")) || ICmpConstant.STWBCHANGE_NEW_DETAIL.equals(list.get(0).get(ICmpConstant.ENTITYNAME))) {
                    String accentity = (String) list.get(0).get(ICmpConstant.ACCENTITY);
                    BaseReqDto baseReqDto = buildCheckRefReq(map,accentity);
                    map.put(ICmpConstant.CONDITION, baseReqDto.getCondition());
                }
            }
        } catch (Exception e) {
            throw new Exception(e.getMessage());
        }
    }

    private BaseReqDto buildCheckRefReq(Map<String, Object> map,String accentity) throws Exception {
        ArrayList<Map> list = (ArrayList<Map>) map.get(ICmpConstant.DATA);
        BaseReqDto bill = new BaseReqDto();
        Integer receipttype ;
        if (ICmpConstant.STWB.equals(list.get(0).get(ICmpConstant.ENTITYNAME))) {
            receipttype = (Integer)list.get(0).get(ICmpConstant.RECEIPTTYPE);
        }else if (ICmpConstant.STWBCHANGE_NEW.equals(list.get(0).get("metaFullName"))){
            receipttype = Integer.valueOf(list.get(0).get("edirectionType").toString());
        } else {
            receipttype = (Integer)list.get(0).get(ICmpConstant.RECEIPTTYPEB);
        }
        FilterVO condition = (FilterVO) map.get(ICmpConstant.CONDITION);
        if (receipttype.equals(ICmpConstant.CONSTANT_TWO)) {
            if (autoConfigService.getCheckStockCanUse()) {
                condition.appendCondition(ConditionOperator.and, new SimpleFilterVO(ICmpConstant.CHECKBILLSTATUS, ICmpConstant.QUERY_EQ, CmpCheckStatus.Use.getValue()));
            } else {
                condition.appendCondition(ConditionOperator.and, new SimpleFilterVO(ICmpConstant.CHECKBILLSTATUS, ICmpConstant.QUERY_EQ, CmpCheckStatus.InStock.getValue()));
            }
        } else if(receipttype.equals(ICmpConstant.CONSTANT_ONE)) {
            condition.appendCondition(ConditionOperator.and, new SimpleFilterVO(ICmpConstant.CHECKBILLSTATUS, ICmpConstant.QUERY_EQ, CmpCheckStatus.InStock.getValue()));
        }
        condition.appendCondition(ConditionOperator.and, new SimpleFilterVO("accentity", ICmpConstant.QUERY_EQ, accentity));
        condition.appendCondition(ConditionOperator.and, new SimpleFilterVO("checkBillType", ICmpConstant.QUERY_IN, new Integer[]{0,1,2,3,4,5}));
        bill.setCondition(condition);
        return bill;
    }

    @Override
    public void afterGridData(RefEntity refEntity, Map<String, Object> map, Object o) {

    }

    @Override
    public void beforeGridMeta(RefEntity refEntity) {

    }

    @Override
    public void beforeTreeData(RefEntity refEntity, Map<String, Object> map) {

    }

    @Override
    public void afterTreeData(RefEntity refEntity, Map<String, Object> map, Object o) {

    }

    @Override
    public void afterGridMeta(RefEntity refEntity, Object o) {

    }
}
