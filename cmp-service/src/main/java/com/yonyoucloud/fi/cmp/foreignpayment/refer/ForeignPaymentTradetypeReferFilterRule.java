package com.yonyoucloud.fi.cmp.foreignpayment.refer;

import com.yonyou.ucf.mdd.common.model.ref.RefEntity;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.FilterVO;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.SimpleFilterVO;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.IDomainConstant;
import com.yonyoucloud.fi.cmp.constant.IRefCodeConstant;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * 外汇付款 交易类型参照过滤规则*
 *
 * @author xuxbo
 * @date 2023/2/21 20:25
 */

@Slf4j
@Component
public class ForeignPaymentTradetypeReferFilterRule extends AbstractCommonRule {

    /**
     * 需要进行过滤的billnum集合
     */
    private static final List<String> BILLNUM_MAP = new ArrayList<>();

    public ForeignPaymentTradetypeReferFilterRule() {
        BILLNUM_MAP.add(IBillNumConstant.CMP_FOREIGNPAYMENT);
        BILLNUM_MAP.add(IBillNumConstant.CMP_FOREIGNPAYMENTLIST);

    }

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        BillDataDto billDataDto = (BillDataDto) getParam(paramMap);
        String billnum = billDataDto.getBillnum();

        //根据bd_billtype表中的form_id 去查询到bd_transtype表中的billtype_id，用billtype去做过滤
        String billTypeId = null;
        try {
            BillContext bc = new BillContext();
            bc.setFullname("bd.bill.BillTypeVO");
            bc.setDomain(IDomainConstant.MDD_DOMAIN_UCFBASEDOC);
            QuerySchema schema = QuerySchema.create();
            schema.addSelect("id");
            QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
            if (billnum.equals(IBillNumConstant.CMP_FOREIGNPAYMENT) || billnum.equals(IBillNumConstant.CMP_FOREIGNPAYMENTLIST)) {
                conditionGroup.appendCondition(QueryCondition.name("form_id").eq(ICmpConstant.CM_CMP_FOREIGNPAYMENT));
            }
            schema.addCondition(conditionGroup);
            List<Map<String, Object>> list = MetaDaoHelper.query(bc, schema);
            if (CollectionUtils.isNotEmpty(list)) {
                Map<String, Object> objectMap = list.get(0);
                if (!ValueUtils.isNotEmptyObj(objectMap)) {
                    log.error("查询保证金交易类型失败！请检查数据！");
                }
                billTypeId = MapUtils.getString(objectMap, "id");
            }
            RefEntity refentity = billDataDto.getRefEntity();
            if (BILLNUM_MAP.contains(billnum)) {
                if (billDataDto.getCondition() == null) {

                    FilterVO filterVO = new FilterVO();
                    if (IRefCodeConstant.TRANSTYPE_BD_BILLTYPERE_LOCAL.equals(refentity.code)) {
                        filterVO.appendCondition(ConditionOperator.and, new SimpleFilterVO("billtype_id", ICmpConstant.QUERY_EQ, billTypeId));
                        // dr =0 为没有被删除的交易类型
                        filterVO.appendCondition(ConditionOperator.and, new SimpleFilterVO("dr", ICmpConstant.QUERY_EQ, 0));
                        //是否启用
                        filterVO.appendCondition(ConditionOperator.and, new SimpleFilterVO("enable", ICmpConstant.QUERY_EQ, 1));
                        billDataDto.setCondition(filterVO);
                    }
                } else {
                    if (IRefCodeConstant.TRANSTYPE_BD_BILLTYPERE_LOCAL.equals(refentity.code)) {
                        billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("billtype_id", ICmpConstant.QUERY_EQ, billTypeId));
                        // dr =0 为没有被删除的交易类型
                        billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("dr", ICmpConstant.QUERY_EQ, 0));
                        //是否启用
                        billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("enable", ICmpConstant.QUERY_EQ, 1));
                    }
                }

            }
        } catch (Exception e) {
            log.error(e.getMessage());

        }


        return new RuleExecuteResult();
    }
}
