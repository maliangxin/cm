package com.yonyoucloud.fi.cmp.checkinventory.refer;

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
import org.apache.commons.lang3.StringUtils;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.yonyoucloud.fi.cmp.constant.ICmpConstant.CM_CMP_CHECKINVENTORY;

/**
 * 支票盘点，参照过滤规则
 */
@Slf4j
@Component
public class CheckInventoryTradeTypeReferRule extends AbstractCommonRule {

    /**
     * 需要进行过滤的billnum集合
     */
    private static final List<String> BILLNUM_MAP = new ArrayList<>();

    public CheckInventoryTradeTypeReferRule() {
        //保证金存入支取单
        BILLNUM_MAP.add(IBillNumConstant.CHECKINVENTORY);
        BILLNUM_MAP.add(IBillNumConstant.CHECKINVENTORY_LIST);
    }

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        BillDataDto billDataDto = (BillDataDto) getParam(paramMap);
        String billnum = billDataDto.getBillnum();
        //银行账户参照过滤
        if (IRefCodeConstant.UCFBASEDOC_BD_ENTERPRISEBANKACCT.equals(billDataDto.getrefCode()) && "cmp_checkInventory".equals(billnum)) {
            if (!"filter".equals(billDataDto.getExternalData())) {//列表查询区的银行账户，不用过滤是否启动停用
                //银行账号状态为 启用
                billDataDto.getCondition().appendCondition(ConditionOperator.and,new SimpleFilterVO("acctopentype", ICmpConstant.QUERY_EQ, "0"));
                if (null != billDataDto.getData() && ((List)billDataDto.getData()).size() > 0) {
                    String banktype = (String) ((Map)((List)billDataDto.getData()).get(0)).get("banktype");
                    if (!StringUtils.isEmpty(banktype)) {
                        //根据银行类别，过滤银行账户
                        billDataDto.getCondition().appendCondition(ConditionOperator.and,new SimpleFilterVO("bank", ICmpConstant.QUERY_EQ, banktype));
                    }
                }
            }
        }else {
            //原业务逻辑
            String billTypeId = null;
            try {
                BillContext bc = new BillContext();
                bc.setFullname("bd.bill.BillTypeVO");
                bc.setDomain(IDomainConstant.MDD_DOMAIN_UCFBASEDOC);
                QuerySchema schema = QuerySchema.create();
                schema.addSelect("id");
                QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
                conditionGroup.appendCondition(QueryCondition.name("form_id").eq(CM_CMP_CHECKINVENTORY));
                schema.addCondition(conditionGroup);
                List<Map<String, Object>> list = MetaDaoHelper.query(bc, schema);
                if (CollectionUtils.isNotEmpty(list)) {
                    Map<String, Object> objectMap = list.get(0);
                    if (!ValueUtils.isNotEmptyObj(objectMap)) {
                        log.error("查询支票盘点交易类型失败！请检查数据！");
                    }
                    billTypeId = MapUtils.getString(objectMap, "id");
                }
                if (BILLNUM_MAP.contains(billnum)) {
                    RefEntity refentity = billDataDto.getRefEntity();
                    FilterVO filterVO = new FilterVO();
                    if (IRefCodeConstant.TRANSTYPE_BD_BILLTYPERE_LOCAL.equals(refentity.code)) {
                        filterVO.appendCondition(ConditionOperator.and, new SimpleFilterVO("billtype_id", ICmpConstant.QUERY_EQ, billTypeId));
                        // dr =0 为没有被删除的交易类型
                        filterVO.appendCondition(ConditionOperator.and, new SimpleFilterVO("dr", ICmpConstant.QUERY_EQ, 0));
                        //是否启用
                        filterVO.appendCondition(ConditionOperator.and, new SimpleFilterVO("enable", ICmpConstant.QUERY_EQ, 1));
                        billDataDto.setCondition(filterVO);
                    }
                }
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        }
        return new RuleExecuteResult();
    }
}
