package com.yonyoucloud.fi.cmp.checkStockApply.rule;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.FilterVO;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.SimpleFilterVO;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.checkstockapply.CmpBusiType;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.IDomainConstant;
import com.yonyoucloud.fi.cmp.constant.IRefCodeConstant;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.yonyoucloud.fi.cmp.constant.ICmpConstant.CM_CMP_CHECKSTOCKAPPLY;

/**
 * 支票入库，参照过滤规则
 */
@Slf4j
@Component
public class CheckStockApplyReferRule extends AbstractCommonRule {

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        String billnum = billContext.getBillnum();
        BillDataDto billDataDto = (BillDataDto) getParam(paramMap);
        BizObject bizObject = null;
        List<BizObject> bills = getBills(billContext, paramMap);
        if (bills.size() > 0) {
            bizObject = bills.get(0);
        }
        if (IRefCodeConstant.UCFBASEDOC_BD_ENTERPRISEBANKACCT.equals(billDataDto.getrefCode()) && "cmp_checkStockApply".equals(billnum)) {
            if (!"filter".equals(billDataDto.getExternalData())) {//列表查询区的银行账户，不用过滤是否启动停用
                //银行账号状态为 启用
                //CZFW-528234 支票入库时，表头的银行账户支持财务公司的账户
                billDataDto.getCondition().appendCondition(ConditionOperator.and,new SimpleFilterVO("acctopentype", ICmpConstant.QUERY_IN, new Integer[]{0,2}));
                if (null != billDataDto.getData() && ((List)billDataDto.getData()).size() > 0) {
                    String banktype = (String) ((Map)((List)billDataDto.getData()).get(0)).get("banktype");
                    if (!StringUtils.isEmpty(banktype)) {
                        //根据银行类别，过滤银行账户
                        billDataDto.getCondition().appendCondition(ConditionOperator.and,new SimpleFilterVO("bank", ICmpConstant.QUERY_EQ, banktype));
                    }
                }
            }
        }
        if ("transtype.bd_billtyperef".equals(billDataDto.getrefCode())) {
            String billTypeId = null;
            BillContext bc = new BillContext();
            bc.setFullname("bd.bill.BillTypeVO");
            bc.setDomain(IDomainConstant.MDD_DOMAIN_UCFBASEDOC);
            QuerySchema schema = QuerySchema.create();
            schema.addSelect("id");
            QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
            conditionGroup.appendCondition(QueryCondition.name("form_id").eq(CM_CMP_CHECKSTOCKAPPLY));
            schema.addCondition(conditionGroup);
            List<Map<String, Object>> list = MetaDaoHelper.query(bc, schema);
            if (CollectionUtils.isNotEmpty(list)) {
                Map<String, Object> objectMap = list.get(0);
                if (!ValueUtils.isNotEmptyObj(objectMap)) {
                    log.error("查询支票入库交易类型失败！请检查数据！");
                }
                billTypeId = MapUtils.getString(objectMap, "id");
            }
            if (Objects.nonNull(billDataDto.getCondition())) {
                billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("billtype_id", ICmpConstant.QUERY_EQ, billTypeId));
            } else {
                FilterVO filterVO = new FilterVO();
                filterVO.appendCondition(ConditionOperator.and, new SimpleFilterVO("billtype_id", ICmpConstant.QUERY_EQ, billTypeId));
                billDataDto.setCondition(filterVO);
            }
            if (bizObject != null) {
                if (CmpBusiType.Black.getValue() == bizObject.getShort("chequeType")) {
                    billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("code", ICmpConstant.QUERY_EQ, "cmp_checkstock_blankcheque"));
                } else {
                    billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("code", ICmpConstant.QUERY_EQ, "cmp_checkstock_incomecheque"));
                }
            }
        }
        return new RuleExecuteResult();
    }

}
