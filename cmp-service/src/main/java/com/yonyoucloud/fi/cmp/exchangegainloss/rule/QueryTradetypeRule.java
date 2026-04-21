package com.yonyoucloud.fi.cmp.exchangegainloss.rule;

import com.yonyou.cloud.utils.CollectionUtils;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.FilterVO;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.dao.meta.UiMetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.paymargin.PayMargin;
import com.yonyoucloud.fi.cmp.receivemargin.ReceiveMargin;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 保证金转单规则
 */
@Component
public class QueryTradetypeRule extends AbstractCommonRule {
    public  static  String BD_TRANSTYPELISTERF="transtype.bd_billtyperef";
    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        BillDataDto bill = (BillDataDto) getParam(map);
        if(map.get("fullNameTar") != null && PayMargin.ENTITY_NAME.equals(map.get("fullNameTar")) || ReceiveMargin.ENTITY_NAME.equals(map.get("fullNameTar"))){
            List<Map<String, Object>> sourceDatas = (List) map.get(ICmpConstant.SOURCEDATAS);// 认领单数据
            if(CollectionUtils.isNotEmpty(sourceDatas)){
                Map<String, Object> sourceData = sourceDatas.get(0);
                if(sourceData.get("exchRateOps") != null && sourceData.get("exchRateOps").equals("2")){
                    throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400803", "保证金目前不支持汇率折算方式除法的计算！") /* "保证金目前不支持汇率折算方式除法的计算！" */);
                }
            }
        }

        FilterVO filterVO = bill.getCondition();
        if(null == filterVO){
            filterVO = new FilterVO();
        }
        if (bill.getrefCode().equals(BD_TRANSTYPELISTERF)){
            if (bill.getData()!=null) {
                UiMetaDaoHelper.appendCondition(filterVO, "billtype_id", ICmpConstant.QUERY_EQ, "FICA5");
                UiMetaDaoHelper.appendCondition(filterVO, "code", ICmpConstant.QUERY_NEQ, ICmpConstant.WFHDSY);
            }
        }

        bill.setCondition(filterVO);
        putParam(map, bill);
        return new RuleExecuteResult();
    }


}
