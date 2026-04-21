package com.yonyoucloud.fi.cmp.ctmrpc;

import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyoucloud.fi.cmp.bankidentifysetting.BankreconciliationIdentifySetting;
import com.yonyoucloud.fi.cmp.bankidentifysetting.BankreconciliationIdentifySetting_b;
import com.yonyoucloud.fi.cmp.cmpentity.BankIdentifyTypeEnum;
import com.yonyoucloud.fi.cmp.cmpentity.DcFlagEnum;
import com.yonyoucloud.fi.cmp.enums.ApplyObjectNewEnum;
//import com.yonyoucloud.fi.cmp.newapi.ctmrpc.api.bankidentify.CtmCmpBankIdentifyRpcService;
import com.yonyoucloud.fi.cmp.util.StringUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author: liaojbo
 * @Date: 2026年03月28日 14:31
 * @Description:流水辨识匹配规则rpc实现类
 */
public class CtmCmpBankIdentifyRpcServiceImpl {
//    implements
//} CtmCmpBankIdentifyRpcService {
//    /**
//     * 查找流水辨识匹配规则通用接口
//     *
//     * @param org 组织ID
//     * @param dcFlag 收付方向
//     * @param applyobject 适用对象
//     * @param identifytype 辨识匹配类型
//     * @return 流水辨识匹配规则
//     * @throws Exception 查询异常
//     */
//    @Override
//    public List<BankreconciliationIdentifySetting> querySettings(String org, DcFlagEnum dcFlag, ApplyObjectNewEnum applyobject, BankIdentifyTypeEnum identifytype) throws Exception {
//        QuerySchema querySchema = new QuerySchema().addSelect("*");
//        querySchema.addSelect("BankreconciliationIdentifySetting_b.*");
//        QueryConditionGroup queryConditionGroup = new QueryConditionGroup();
//        if (StringUtils.isNotEmpty(org)) {
//            queryConditionGroup.addCondition(QueryCondition.name("accentity").eq(org));
//        }
//        if (identifytype != null) {
//            queryConditionGroup.addCondition(QueryCondition.name("identifytype").eq(identifytype.getValue()));
//        }
//        if (applyobject != null) {
//            queryConditionGroup.addCondition(QueryCondition.name("applyobject").eq(applyobject.getValue()));
//        }
//        if (dcFlag != null) {
//            queryConditionGroup.addCondition(QueryCondition.name("dc_flag").eq(dcFlag.getValue()));
//        }
//        queryConditionGroup.addCondition(QueryCondition.name("dr").eq(0));
//        querySchema.addCondition(queryConditionGroup);
//        List<BankreconciliationIdentifySetting> bankreconciliationIdentifySettings =  MetaDaoHelper.queryObject(BankreconciliationIdentifySetting.ENTITY_NAME, querySchema, null);
//        // 组装主子表数据
//        if(CollectionUtils.isNotEmpty(bankreconciliationIdentifySettings)){
//            Map<Long,List<BankreconciliationIdentifySetting>> childMap = bankreconciliationIdentifySettings.stream().collect(Collectors.groupingBy(setting -> setting.get("BankreconciliationIdentifySetting_b_mainid")));
//            for(BankreconciliationIdentifySetting setting : bankreconciliationIdentifySettings){
//                List<BankreconciliationIdentifySetting> bankreconciliationIdentifySettingChildren = childMap.get(setting.getId());
//                if (bankreconciliationIdentifySettingChildren == null) {
//                    continue;
//                }
//                List<BankreconciliationIdentifySetting_b> bankreconciliationIdentifySetting_bs = new ArrayList<>();
//                for(BankreconciliationIdentifySetting child : bankreconciliationIdentifySettingChildren){
//                    //遍历map，筛选其中的key
//                    Map<String,Object> realChildMap = child.entrySet().stream().filter(item -> item.getKey().startsWith("BankreconciliationIdentifySetting_b_")).collect(Collectors.toMap(item -> item.getKey().replace("BankreconciliationIdentifySetting_b_", ""), item -> item.getValue()));
//                    BankreconciliationIdentifySetting_b bankreconciliationIdentifySetting_b = new BankreconciliationIdentifySetting_b();
//                    bankreconciliationIdentifySetting_b.init(realChildMap);
//                    bankreconciliationIdentifySetting_bs.add(bankreconciliationIdentifySetting_b);
//                }
//                setting.setBankreconciliationIdentifySetting_b(bankreconciliationIdentifySetting_bs);
//            }
//        }
//        return bankreconciliationIdentifySettings;
//    }

}
