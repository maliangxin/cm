package com.yonyoucloud.fi.cmp.common.service;

import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.iuap.message.platform.rpc.IBizMessageService;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.bankdealdetail.BankDealDetail;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.cmpentity.AssociationStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 银企联作废数据删除逻辑
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class YQLInvalidateServiceImpl implements YQLInvalidateService {
    private static final String busObjectCode = "ctm-cmp.cmp_bankreconciliation";// 业务对象编码
    private static final String billNum = "cmp_bankreconciliation";
    private static final String action = "delete";

    private final IBizMessageService bizMessageService;

    /**
     * 银企联返回流水状态为 作废时(is_refund=3) 删除逻辑
     *
     * @param list
     * @throws Exception
     */
    @Override
    @Transactional(rollbackFor = RuntimeException.class)
    public void refundDelete(List<BankDealDetail> list) throws Exception {
        refundDeleteByBizObj(list);
    }

    /**
     * 银企联返回流水状态为 作废时(is_refund=3) 删除逻辑
     * is_refund 1 退票 2 更新 3 作废
     * 收付单据关联（业务关联）为'已关联'  (associationstatus != null && associationstatus == AssociationStatus.Associated.getValue())
     * 总账是否已勾对=是  other_checkflag = true
     * 日记账是否已勾对=是 checkflag = true
     * 业务动作 的消息事件
     * 业务日志??
     *
     * @param list
     * @throws Exception
     */
    @Override
    @Transactional(rollbackFor = RuntimeException.class)
    public void refundDeleteByBizObj(List<? extends BizObject> list) throws Exception {
        if(CollectionUtils.isEmpty(list)){
            log.error("传入的集合为空");
            return ;
        }
        // StringBuilder msgBuilder = new StringBuilder();
        List<Long> ids = list.stream().map(item -> item.getLong("id")).collect(Collectors.toList());
        if(CollectionUtils.isEmpty(ids)){
            log.error("未传入ids");
            return ;
        }
        QueryConditionGroup group = new QueryConditionGroup();
        group.addCondition(QueryCondition.name("id").in(ids));
        QueryConditionGroup repeatGroup = QueryConditionGroup.or(QueryCondition.name("isrepeat").is_not_null(), QueryCondition.name("isrepeat").is_null());
        group.addCondition(repeatGroup);
        QuerySchema querySchema = QuerySchema.create().addSelect("id,bank_seq_no,associationstatus,other_checkflag,checkflag").addCondition(group);
        List<BankReconciliation> bankReconciliationList = MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME, querySchema, null);
        if (CollectionUtils.isNotEmpty(bankReconciliationList)) {
            // 不允许删除的数据 收付单据关联（业务关联）为'已关联'或总账是否已勾对='是'或日记账是否已勾对=’是‘
            List<BizObject> noDeleteList = bankReconciliationList.stream().filter(item -> (item.get("associationstatus") != null && item.getShort("associationstatus") == AssociationStatus.Associated.getValue())
                    && (item.get("other_checkflag") != null && item.getBoolean("other_checkflag"))
                    && (item.get("checkflag") != null && item.getBoolean("checkflag"))
            ).collect(Collectors.toList());
            // 允许删除的数据 收付单据关联（业务关联）不为'已关联' 且 总账是否已勾对='否' 且日记账是否已勾对=’否‘
            List<BizObject> deleteList = bankReconciliationList.stream().filter(item -> (item.get("associationstatus") == null || item.getShort("associationstatus") != AssociationStatus.Associated.getValue())
                    && (item.get("other_checkflag") == null || !item.getBoolean("other_checkflag"))
                    && (item.get("checkflag") == null || !item.getBoolean("checkflag"))
            ).collect(Collectors.toList());

            // String deleteBankSeqNo = deleteList.stream().map(item -> item.getString("bankseqno")).collect(Collectors.joining("】,【"));
            // String nodelBankSeqNo = noDeleteList.stream().map(item -> item.getString("bankseqno")).collect(Collectors.joining("】,【"));
            // msgBuilder.append("【").append(nodelBankSeqNo).append("】已经发生下游业务,无法删除!");
            // msgBuilder.append("【").append(deleteBankSeqNo).append("】未发生下游业务,已删除!");
            if (CollectionUtils.isNotEmpty(deleteList)) {
                if (list.get(0) instanceof BankReconciliation) {
                    MetaDaoHelper.delete(BankReconciliation.ENTITY_NAME, deleteList);
                } else if (list.get(0) instanceof BankDealDetail) {
                    MetaDaoHelper.delete(BankDealDetail.ENTITY_NAME, deleteList);
                }
            }
            if(CollectionUtils.isNotEmpty(noDeleteList)){
                sendMessage(noDeleteList);
            }
        }
    }

    /**
     * 发送消息
     * dobizMessage这个接口不支持业务变量,只支持元数据-- 周新心
     * @param list
     */
    private void sendMessage(List<BizObject> list) {
        try {
            List<LinkedHashMap<String, String>> billMapList = new ArrayList();
            // bills这个参数传的元数据,除了id你其他字段不用传,我们会反查数据；有billMap可以按照格式传一部分常用数据，会优先从这里取 -- 周新心
            list.forEach((bill) -> {
                billMapList.add(this.parseFromBizObject(bill));
            });
            //新老架构
            boolean newArch = true;
            //todo 1015支持
            //判断配置消息模板，注意 新老架构参数，新架构如果传入billnum会查不到数据
            boolean existConfig = bizMessageService.checkExistConfig(InvocationInfoProxy.getTenantid(), newArch ? null : billNum, newArch ? busObjectCode : null, "delete");
            if (!existConfig) {
                log.error("当前租户:{}未配置银行对账单删除业务消息,不执行发送消息配置", InvocationInfoProxy.getTenantid());
                return;
            }
            // 老架构需要传单据编码，新架构需要穿业务对象的编码，不然消息发布出去
            if (newArch) {
                bizMessageService.doBizMessage("diwork", InvocationInfoProxy.getTenantid(), billMapList, null, action, InvocationInfoProxy.getUserid(), InvocationInfoProxy.getYhtAccessToken(), busObjectCode);
            } else {
                bizMessageService.doBizMessage("diwork", InvocationInfoProxy.getTenantid(), billMapList, billNum, action, InvocationInfoProxy.getUserid());
            }
        } catch (Exception e) {
            log.error("发送业务消息失败!", e);
        }
    }

    private LinkedHashMap<String, String> parseFromBizObject(BizObject bizObj) {
        LinkedHashMap<String, String> map = new LinkedHashMap<>(bizObj.size());
        for (Map.Entry<String, Object> entry : bizObj.entrySet()) {
            map.put(entry.getKey(), CtmJSONObject.toJSONString(entry.getValue()));
        }
        return map;
    }

}
