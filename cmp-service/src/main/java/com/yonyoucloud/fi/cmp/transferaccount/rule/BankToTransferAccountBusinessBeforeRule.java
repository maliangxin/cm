package com.yonyoucloud.fi.cmp.transferaccount.rule;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.billmake.model.MakeBillRule;
import com.yonyou.ucf.mdd.ext.bill.billmake.model.MakeBillRuleDetail;
import com.yonyou.ucf.mdd.ext.bill.billmake.vo.PushAndPullVO;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.billclaim.BillClaimItem;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.ITransferAccountBusinessConstant;
import com.yonyoucloud.fi.cmp.constant.MerchantConstant;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;

/**
 * RPT0176 银行账单/认领生单类型拓展 支持生成同名账号划转账单
 */
@Slf4j
@Component
public class BankToTransferAccountBusinessBeforeRule extends AbstractCommonRule {

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        List<Map<String, Object>> sourceDatas = (List) paramMap.get(ICmpConstant.SOURCEDATAS);
        MakeBillRule makeBillRule = (MakeBillRule) paramMap.get("makeBillRule");
        String code = makeBillRule.getCode();
        if (ITransferAccountBusinessConstant.BANK_TO_BANK_TRANSFER.equals(code)) {
            for (Map<String, Object> sourceMap : sourceDatas) {
                List<Map<String, Object>> bankReconciliation = MetaDaoHelper.queryById(BankReconciliation.ENTITY_NAME, "*", sourceMap.get("id"));
                Map<String, Object> sMap = bankReconciliation.get(0);
                //①选择流水进行生单，判定流水的对方单位与账户使用组织（会计主体）是否一致，如不一致，选择银行转账单时，提示“不满足生单条件：银行转账，账户使用组织应与对方单位一致，请检查！
                String accentity = sMap.get(MerchantConstant.ACCENTITY).toString();
                Object oppositeobjectid = sMap.get("oppositeobjectid");
                if (oppositeobjectid == null || !StringUtils.equals(accentity, String.valueOf(oppositeobjectid))) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100253"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1FFA194E05280003", "不满足生单条件:银行转账，对方类型需为”内部单位“，且账户使用组织应与对方单位一致，请检查!") /* "不满足生单条件：银行转账，账户使用组织应与对方单位一致，请检查!" */);
                }
            }
        } else if (ITransferAccountBusinessConstant.BANK_TO_BANK_TRANSFER_CLAIM.equals(code)) {
            PushAndPullVO vo = (PushAndPullVO) paramMap.get("pushAndPullVO");
            if (!CollectionUtils.isEmpty(vo.getChildIds())) {
                for (String childId : vo.getChildIds()) {
                    QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("mainid").eq(childId));
                    QuerySchema schema = QuerySchema.create().addSelect("*");
                    schema.addCondition(group);
                    List<Map<String, Object>> list = MetaDaoHelper.query(BillClaimItem.ENTITY_NAME, schema);
                    for (Map<String, Object> sMap : list) {
                        String accentity = sMap.get(MerchantConstant.ACCENTITY).toString();
                        Object oppositeobjectid = sMap.get("oppositeobjectid");
                        if (oppositeobjectid == null || !StringUtils.equals(accentity, String.valueOf(oppositeobjectid))) {
                            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100253"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1FFA194E05280003", "不满足生单条件:银行转账，对方类型需为”内部单位“，且账户使用组织应与对方单位一致，请检查!") /* "不满足生单条件：银行转账，账户使用组织应与对方单位一致，请检查!" */);
                        }
                    }
                }
            }
        }

        // 批量生单逻辑：手动设置主键id映射关系，用于afterRule中查找转换后数据与源数据的映射关系
        List<MakeBillRuleDetail> details =  makeBillRule.get("makeBillRuleDetailList");
        // 复制一个主键id映射（用完之后，tarList中需删除生成的字段）
        MakeBillRuleDetail detailId = this.copyMakeBillRuleDetailForId(details.get(0));
        details.add(detailId);
        return new RuleExecuteResult(paramMap);
    }

    private MakeBillRuleDetail copyMakeBillRuleDetailForId(MakeBillRuleDetail sourceDetail) {
        MakeBillRuleDetail idDetail = new MakeBillRuleDetail();
        idDetail.init(sourceDetail);
        // 重新设置主键（随便设置）
        idDetail.put("id","1111890463118851111");
        // 设置字段映射
        idDetail.put("origin_field","id");
        idDetail.put("origin_field_type", "Long");
        idDetail.put("origin_field_name", "ID(id)");
        // 虚拟字段（实际对象中没有该字段）
        idDetail.put("target_field","sourceId");
        idDetail.put("target_field_type", "Long");
        idDetail.put("target_field_name", "sourceId(sourceId)");

        idDetail.put("mapped_type", 0);

        return idDetail;
    }
}
