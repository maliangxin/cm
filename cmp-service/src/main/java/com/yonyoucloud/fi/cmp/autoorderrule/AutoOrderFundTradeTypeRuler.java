package com.yonyoucloud.fi.cmp.autoorderrule;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.FilterVO;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.SimpleFilterVO;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.cmpentity.EventType;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.IDomainConstant;
import com.yonyoucloud.fi.cmp.constant.IRefCodeConstant;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.collections4.CollectionUtils;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import static com.yonyoucloud.fi.cmp.constant.ICmpConstant.CM_CMP_FUND_COLLECTION;
import static com.yonyoucloud.fi.cmp.constant.ICmpConstant.CM_CMP_FUND_PAYMENT;

/**
 * 自动生单规则 -- 参照前过滤
 * 交易类型根据 业务单据类型过滤
 * @author msc
 */
@Component
public class AutoOrderFundTradeTypeRuler extends AbstractCommonRule {

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        BillDataDto billDataDto = (BillDataDto) getParam(map);//获取参数对象

        if(billDataDto.getRefCode().equals(IRefCodeConstant.FINBD_BD_PAYMENTTYPEREF)) return new RuleExecuteResult();

        BillContext bc = new BillContext();
        bc.setFullname("bd.bill.BillTypeVO");
        bc.setDomain(IDomainConstant.MDD_DOMAIN_UCFBASEDOC);
        QuerySchema schema = QuerySchema.create();//过滤对象
        schema.addSelect("id");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);

        //获取参数
        CtmJSONObject jsonObject = CtmJSONObject.parseObject(map.get("requestData").toString());
        Long id = jsonObject.get("id") == null ? null : Long.valueOf(jsonObject.get("id").toString());//获取id
        QuerySchema querySchema1 = QuerySchema.create().addSelect("*");
        QueryConditionGroup group1 = QueryConditionGroup.and(QueryCondition.name("detailid").eq(id));
        querySchema1.addCondition(group1);
        List<Autoorderrule> autoorderrules = MetaDaoHelper.queryObject(Autoorderrule.ENTITY_NAME, querySchema1, null);
        //获取业务单据类型
        Short eventType = autoorderrules.isEmpty() ? null : autoorderrules.get(0).getBusDocumentType();
        if (eventType == null)
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102409"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041804C3","查询资金收付款单交易类型失败！请检查数据。") /* "查询资金收付款单交易类型失败！请检查数据。" */);
        //设置交易类型过滤条件
        if(eventType.equals(EventType.FundCollection.getValue())){
            //资金收款单
            conditionGroup.appendCondition(QueryCondition.name("form_id").eq(CM_CMP_FUND_COLLECTION));
        }else if(eventType.equals(EventType.FundPayment.getValue())){
            //资金付款单
            conditionGroup.appendCondition(QueryCondition.name("form_id").eq(CM_CMP_FUND_PAYMENT));
        }
        //查询过滤id  -- copy自 FundCommonTradeTypeReferFilterRule 第63行
        schema.addCondition(conditionGroup);
        List<Map<String, Object>> list = MetaDaoHelper.query(bc, schema);
        String billtypeId = null;
        if (CollectionUtils.isNotEmpty(list)) {
            Map<String, Object> objectMap = list.get(0);
            if (!ValueUtils.isNotEmptyObj(objectMap)) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102409"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041804C3","查询资金收付款单交易类型失败！请检查数据。") /* "查询资金收付款单交易类型失败！请检查数据。" */);
            }
            billtypeId = MapUtils.getString(objectMap, "id");
        }
        //组装交易类型过滤对象
        FilterVO filterVO = billDataDto.getCondition() == null ? new FilterVO() : billDataDto.getCondition();
        billDataDto.setCondition(filterVO);
        billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("billtype_id", ICmpConstant.QUERY_EQ, billtypeId));
        billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("dr", ICmpConstant.QUERY_EQ, 0));
        billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("enable", ICmpConstant.QUERY_EQ, 1));

        return new RuleExecuteResult();
    }
}
