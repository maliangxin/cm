package com.yonyoucloud.fi.cmp.bankidentify.rule;

import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.iuap.metadata.core.model.businessobject.QueryBusinessObjectListReturn;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.ucf.mdd.ext.support.interfaces.IMddExtSupportService;
import lombok.extern.slf4j.Slf4j;
import org.imeta.core.base.MetaRepository;
import org.imeta.core.model.Entity;
import org.imeta.core.model.Property;
import org.imeta.orm.base.BizObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 业务对象属性参照
 * @author guoxh
 */
@Slf4j
@Component("bizDataAttributeReferRule")
public class BizDataAttributeReferRule extends AbstractCommonRule {
    private static final String BILLNUM="cmp_bankreconciliationidentifysetting";
    private static final String STWB_SETTLEBENCH_BIZOBJECT = "stwb.stwb_settlebench";
    private static final String CMP_BANKRECONCILIATION_BIZOBJECT = "ctm-cmp.cmp_bankreconciliation";
    private static final String CMP_BILLCLAIM_BIZOBJECT = "ctm-cmp.cmp_billclaimcard";
    @Autowired
    private IMddExtSupportService mddExtSupportService;
    @Autowired
    private MetaRepository metaRepository;
    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        List<Map<String,Object>> list = new ArrayList<>();
        BillDataDto billDataDto = (BillDataDto) getParam(paramMap);
        List<BizObject> paramList  = (List<BizObject>)billDataDto.getData();
        BizObject bizObject = paramList.get(0);

        if(BILLNUM.equals(billDataDto.getBillnum())){
            String fullName = "";
            if("matchfieldname".equals(billDataDto.getDatasource())){
                //目标匹配对象
                if(bizObject.get("matchobject") != null && com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400721", "资金结算明细") /* "资金结算明细" */.equals(bizObject.getString("matchobject"))){
                    fullName = STWB_SETTLEBENCH_BIZOBJECT;
                }else{
                    return new RuleExecuteResult();
                }
            }else {
                if(bizObject.get("applyobject") != null){
                    if("1".equals(bizObject.getString("applyobject"))) {
                        //银行流水
                        fullName = CMP_BANKRECONCILIATION_BIZOBJECT;
                    }else if("2".equals(bizObject.getString("applyobject"))) {
                        //认领单
                        fullName = CMP_BILLCLAIM_BIZOBJECT;
                    }
                }else{
                    return new RuleExecuteResult();
                }
            }

            QueryBusinessObjectListReturn businessObjectListReturn = mddExtSupportService.queryBizObjByCode(InvocationInfoProxy.getTenantid(),fullName);
            Entity entity = metaRepository.entity(businessObjectListReturn.getUri());
            for (Property attribute : entity.attributes()) {
                Map<String,Object> map = new HashMap<String,Object>();
                map.put("id",attribute.id());
                map.put("name",attribute.title());
                map.put("code",attribute.name());
                map.put("title",attribute.name());
                if(STWB_SETTLEBENCH_BIZOBJECT.equals(fullName)){
                    map.put("name",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400723", "资金结算单.") /* "资金结算单." */ + attribute.title());
                    map.put("title",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400723", "资金结算单.") /* "资金结算单." */ + attribute.title());
                }
                map.put("columnName",attribute.columnName());
                map.put("iLength",attribute.iLength());
                map.put("typeUri",attribute.getTypeUri());
                if(attribute.isEntity()){
                    map.put("type","bigint");
                }else{
                    map.put("type",attribute.type().name().toLowerCase());
                }

                map.put("bizType",attribute.getBizType());
                map.put("parentId","");
                //
                boolean includeChildEntity = STWB_SETTLEBENCH_BIZOBJECT.equals(fullName) && "settleBench_b".equals(attribute.name());
                if(includeChildEntity) {
                    if (attribute.getTypeUri().split("\\.").length == 3) {
                        Entity childrenEntity = metaRepository.entity(attribute.getTypeUri());
                        if (childrenEntity != null) {
                            List<Map<String, Object>> childList = childrenEntity.attributes().stream().map(item -> {
                                Map<String, Object> child = new HashMap<String, Object>();
                                child.put("id", item.id());
                                child.put("code", item.name());
                                child.put("name", item.title());
                                child.put("title", item.title());
                                if("settleBench_b".equals(attribute.name())) {
                                    child.put("code", "settleBench_b." + item.name());
                                    child.put("name",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400722", "资金结算明细.") /* "资金结算明细." */ + item.title());
                                    child.put("title",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400722", "资金结算明细.") /* "资金结算明细." */ + item.title());
                                }
                                child.put("columnName", item.columnName());
                                child.put("iLength", item.iLength());
                                child.put("typeUri", item.getTypeUri());
                                child.put("type",item.type().name());
                                child.put("bizType",item.getBizType());
                                child.put("parentId", attribute.id());
                                return child;
                            }).collect(Collectors.toList());
                            map.put("children", childList);
                        }
                    }
                }
                list.add(map);
            }
        }
        paramMap.put("return", list);
        return new RuleExecuteResult(list);
    }
}
