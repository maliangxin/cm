package com.yonyoucloud.fi.cmp.flowhandlesetting.service.impl;


import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyoucloud.fi.cmp.flowhandlesetting.*;
import com.yonyoucloud.fi.cmp.flowhandlesetting.dto.FlowHandleSettingSub;
import com.yonyoucloud.fi.cmp.flowhandlesetting.dto.FlowHandleSettingVO;
import com.yonyoucloud.fi.cmp.flowhandlesetting.service.IFlowhandlesettingService;
import com.yonyoucloud.fi.cmp.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;


/**
 * @author guoxh
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class FlowhandlesettingServiceImpl implements IFlowhandlesettingService {
    @Override
    public List<FlowHandleSettingVO> queryFlowHandleSettingByCondition(Integer flowType, Integer object, String accentity, Integer handleType) {
        Objects.requireNonNull(flowType, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400678", "流程环节不能为空") /* "流程环节不能为空" */);
        Objects.requireNonNull(object, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400676", "适用对象不能为空") /* "适用对象不能为空" */);
        Objects.requireNonNull(handleType, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400677", "处理环节不能为空") /* "处理环节不能为空" */);
        if (StringUtils.isEmpty(accentity)) {
            accentity = "666666";
        }

        List<FlowHandleSettingVO> result = new ArrayList<>();
        QuerySchema querySchema = QuerySchema.create().addSelect("*").appendQueryCondition(
                QueryCondition.name("flowType").eq(flowType),
                QueryCondition.name("object").eq(object),
                QueryCondition.name("accentity").eq(accentity),
                QueryCondition.name("enable").eq(1)
        );
        switch (flowType) {
            case 1:
                querySchema.appendQueryCondition(QueryCondition.name("associationMode").eq(handleType));
                break;
            case 2:
                querySchema.appendQueryCondition(QueryCondition.name("associationCredentialMode").eq(handleType));
                break;
            case 3:
                querySchema.appendQueryCondition(QueryCondition.name("createBillMod").eq(handleType));
                break;
            case 4:
                querySchema.appendQueryCondition(QueryCondition.name("publishMode").eq(handleType));
                break;
            default:
                querySchema.appendQueryCondition(QueryCondition.name("1").not_eq("2"));
                break;
        }

        try {
            List<Flowhandlesetting> list = MetaDaoHelper.queryObject(Flowhandlesetting.ENTITY_NAME, querySchema, null);
            if (CollectionUtils.isNotEmpty(list)) {
                List<BizObject> children = null;
                List<Long> ids = list.stream().map(item -> item.getLong("id")).collect(Collectors.toList());
                if (flowType == 1) {
                    QuerySchema subQuery = QuerySchema.create().addSelect("*").appendQueryCondition(QueryCondition.name("mainid").in(ids));
                    children = MetaDaoHelper.queryObject(FhsAssociationRange.ENTITY_NAME, subQuery, null);
                } else if (flowType == 2) {
                    QuerySchema subQuery = QuerySchema.create().addSelect("*").appendQueryCondition(QueryCondition.name("mainid").in(ids));
                    children = MetaDaoHelper.queryObject(FhsBillRange.ENTITY_NAME, subQuery, null);
                } else if (flowType == 3) {
                    QuerySchema subQuery = QuerySchema.create().addSelect("*").appendQueryCondition(QueryCondition.name("mainid").in(ids));
                    children = MetaDaoHelper.queryObject(FhsCreateBillRange.ENTITY_NAME, subQuery, null);
                } else if (flowType == 4) {
                    QuerySchema subQuery = QuerySchema.create().addSelect("*").appendQueryCondition(QueryCondition.name("mainid").in(ids));
                    children = MetaDaoHelper.queryObject(FhsPublishRange.ENTITY_NAME, subQuery, null);
                }
                result = this.buildRtnVo(flowType, list, children);
            }
        } catch (Exception e) {
            log.error("查询失败，失败原因：", e);
        }
        return result;
    }

    private List<FlowHandleSettingVO> buildRtnVo(Integer flowType, List<Flowhandlesetting> list, List<BizObject> children) {
        List<FlowHandleSettingVO> result = new ArrayList<>();
        Map<Long, List<BizObject>> map = null;
        if (CollectionUtils.isNotEmpty(children)) {
            map = children.stream().collect(Collectors.groupingBy(item -> item.getLong("mainid")));
        }
        for (Flowhandlesetting flowhandlesetting : list) {
            FlowHandleSettingVO vo = new FlowHandleSettingVO();
            if (flowhandlesetting.getMsgType().split(",").length > 1) {
                vo.setStoreMidTable(1);
                vo.setNotifyBizObject(1);
            } else {
                if ("1".equals(flowhandlesetting.getMsgType())) {
                    vo.setStoreMidTable(1);
                    vo.setNotifyBizObject(2);
                } else {
                    vo.setStoreMidTable(2);
                    vo.setNotifyBizObject(1);
                }
            }

            if (flowType == 1) {
                if (flowhandlesetting.getAssociationMode() == 1) {
                    vo.setAssociationBillRange(Integer.valueOf(flowhandlesetting.getAssociationBillRange()));
                } else if (flowhandlesetting.getAssociationMode() == 2) {
                    vo.setIsArtiConfirm(Integer.valueOf(flowhandlesetting.getIsArtiConfirm()));
                    vo.setIsRandomAutoConfirm(Integer.valueOf(flowhandlesetting.getIsRandomAutoConfirm()));
                }
                //
                if (map.containsKey(flowhandlesetting.getLong("id"))) {
                    List<BizObject> bizObjects = map.get(flowhandlesetting.getLong("id"));
                    List<FlowHandleSettingSub> subList = bizObjects.stream().map(bizObject -> {
                        FlowHandleSettingSub sub = new FlowHandleSettingSub();
                        sub.setRuleEngineConfig(bizObject.getString("ruleEngineConfig"));
                        sub.setAssoBill(bizObject.getString("bill"));
                        return sub;
                    }).collect(Collectors.toList());
                    vo.setList(subList);
                }
            } else if (flowType == 2) {
                if (flowhandlesetting.getAssociationBillRange() == 1) {
                    vo.setAssociationBillRange(Integer.valueOf(flowhandlesetting.getAssociationBillRange()));
                } else if (flowhandlesetting.getAssociationBillRange() == 2) {
                    vo.setIsArtiConfirm(Integer.valueOf(flowhandlesetting.getIsArtiConfirm()));
                    vo.setIsRandomAutoConfirm(Integer.valueOf(flowhandlesetting.getIsRandomAutoConfirm()));
                }
                if (map.containsKey(flowhandlesetting.getLong("id"))) {
                    List<BizObject> bizObjects = map.get(flowhandlesetting.getLong("id"));
                    List<FlowHandleSettingSub> subList = bizObjects.stream().map(bizObject -> {
                        FlowHandleSettingSub sub = new FlowHandleSettingSub();
                        sub.setRuleEngineConfig(bizObject.getString("ruleEngineConfig"));
                        sub.setCredentialType(bizObject.getString("credentialType"));
                        sub.setIsFinishOver(bizObject.getInteger("isFinishOver"));
                        sub.setFinishAfterFlow(bizObject.getInteger("finishAfterFlow"));
                        return sub;
                    }).collect(Collectors.toList());
                    vo.setList(subList);
                }
            } else if (flowType == 3) {
                if (flowhandlesetting.getCreateBillMode() == 1) {
                    vo.setAssociationBillRange(Integer.valueOf(flowhandlesetting.getAssociationBillRange()));
                } else if (flowhandlesetting.getCreateBillMode() == 2) {
                    vo.setIsArtiConfirm(Integer.valueOf(flowhandlesetting.getIsArtiConfirm()));
                    vo.setIsRandomAutoConfirm(Integer.valueOf(flowhandlesetting.getIsRandomAutoConfirm()));
                }
                if (map.containsKey(flowhandlesetting.getLong("id"))) {
                    List<BizObject> bizObjects = map.get(flowhandlesetting.getLong("id"));
                    List<FlowHandleSettingSub> subList = bizObjects.stream().map(bizObject -> {
                        FlowHandleSettingSub sub = new FlowHandleSettingSub();
                        sub.setRuleEngineConfig(bizObject.getString("ruleEngineConfig"));
                        sub.setCreateBill(bizObject.getString("bill"));
                        return sub;
                    }).collect(Collectors.toList());
                    vo.setList(subList);
                }
            } else if (flowType == 4) {
                if (flowhandlesetting.getPublishMode() == 1) {
                    vo.setAssociationBillRange(Integer.valueOf(flowhandlesetting.getAssociationBillRange()));
                }
                if (map.containsKey(flowhandlesetting.getLong("id"))) {
                    List<BizObject> bizObjects = map.get(flowhandlesetting.getLong("id"));
                    List<FlowHandleSettingSub> subList = bizObjects.stream().map(bizObject -> {
                        FlowHandleSettingSub sub = new FlowHandleSettingSub();

                        return sub;
                    }).collect(Collectors.toList());
                    vo.setList(subList);
                }
            }
            result.add(vo);
        }
        return result;
    }


}
