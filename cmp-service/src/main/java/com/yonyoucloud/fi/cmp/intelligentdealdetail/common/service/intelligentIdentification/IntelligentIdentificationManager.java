package com.yonyoucloud.fi.cmp.intelligentdealdetail.common.service.intelligentIdentification;


import com.yonyou.cloud.utils.CollectionUtils;
import com.yonyou.iuap.ruleengine.dto.relevant.TargetRuleInfoDto;
import com.yonyoucloud.fi.cmp.bankidentify.BankIdentifyService;
import com.yonyoucloud.fi.cmp.bankidentifysetting.BankreconciliationIdentifySetting;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.cmpentity.OppositeType;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.common.service.IBankReconciliationCommonService;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.common.service.IntelligentIdentificationService;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.consts.DealDetailBusinessCodeEnum;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailcontext.BankDealDetailContext;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.ruleconst.RuleCodeConst;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.utils.BankMatchAndProcessUtils;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.utils.DealDetailUtils;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.Direction;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
@Slf4j
@Service
public class IntelligentIdentificationManager {

    @Autowired
    Map<String, IntelligentIdentificationService> intelligentIdentificationServiceMap;

    @Autowired
    IBankReconciliationCommonService bankReconciliationCommonService;

    @Autowired
    BankIdentifyService bankIdentifyService;

    private static final String innerOrg = com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540077D", "企业银行账户") /* "企业银行账户" */;
    private static final String supplier = com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540077E", "客户档案") /* "客户档案" */;
    private static final String employee = com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540077F", "员工档案") /* "员工档案" */;
    private static final String customer = com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400780", "供应商档案") /* "供应商档案" */;
    /**
     * 将流水按收支方向进行分组，然后分别辨识
     *
     * @param bankReconciliationList
     * @throws Exception
     */
    public void excuteIdentificate(List<BankReconciliation> bankReconciliationList,BankDealDetailContext context) throws Exception {
        // 对方账号和对方户名为空的时候直接辨识为【其他】
        List<BankReconciliation> expenditureListOnlyOther = bankReconciliationList.stream().filter(e ->Direction.Debit.getValue() == e.getDc_flag().getValue() &&  ObjectUtils.isEmpty(e.getTo_acct_no()) && ObjectUtils.isEmpty(e.getTo_acct_name())).collect(Collectors.toList());
        List<BankReconciliation> incomeListOnlyOther = bankReconciliationList.stream().filter(e ->Direction.Credit.getValue() == e.getDc_flag().getValue() &&  ObjectUtils.isEmpty(e.getTo_acct_no()) && ObjectUtils.isEmpty(e.getTo_acct_name())).collect(Collectors.toList());

        //excuteIdentificateByIdentificationTypes(toAccountInfoIsEmptyList, context, OppositeType.Other.getValue());

        // 对方账号信息不为空按照收支方向分类处理
        List<BankReconciliation> expenditureList = bankReconciliationList.stream().filter(e -> Direction.Debit.getValue() == e.getDc_flag().getValue() && (ObjectUtils.isNotEmpty(e.getTo_acct_no()) || ObjectUtils.isNotEmpty(e.getTo_acct_name()))).collect(Collectors.toList());
        List<BankReconciliation> incomeList = bankReconciliationList.stream().filter(e -> Direction.Credit.getValue() == e.getDc_flag().getValue() && (ObjectUtils.isNotEmpty(e.getTo_acct_no()) || ObjectUtils.isNotEmpty(e.getTo_acct_name())) ).collect(Collectors.toList());
        Map<Short,Map<Integer, BankreconciliationIdentifySetting>> ruleCodes = new HashMap<>();
        try {
            //查询所有规则
            ruleCodes = bankReconciliationCommonService.getRuleByOrder(RuleCodeConst.SYSTEM004);
            if (ObjectUtils.isEmpty(ruleCodes)) {
                log.error("对方信息辨识规则中,配置的相关性规则查询为空！！");
                return;
            }
            Map<Integer, BankreconciliationIdentifySetting> expenditureMap = ruleCodes.get(Direction.Credit.getValue());
            Map<Integer, BankreconciliationIdentifySetting> incomeMap = ruleCodes.get(Direction.Debit.getValue());
            if (ObjectUtils.isNotEmpty(expenditureListOnlyOther)){
                excuteIdentificateByIdentificationTypesForCheck(expenditureListOnlyOther, expenditureMap,context,false);
            }
            if (ObjectUtils.isNotEmpty(incomeListOnlyOther)){
                excuteIdentificateByIdentificationTypesForCheck(incomeListOnlyOther, incomeMap,context,false);
            }
            //执行相应的规则
            if (ObjectUtils.isNotEmpty(expenditureList)){
                excuteIdentificateByIdentificationTypesForCheck(expenditureList, expenditureMap,context,true);
            }
            if (ObjectUtils.isNotEmpty(incomeList)){
                excuteIdentificateByIdentificationTypesForCheck(incomeList, incomeMap,context,true);
            }

        }catch (Exception e){
            log.error("执行对方信息辨识异常！！");
        }

        //支出: 内部单位  供应商  员工  客户
//        excuteIdentificateByIdentificationTypes(expenditureList, context, OppositeType.InnerOrg.getValue(), OppositeType.Supplier.getValue(), OppositeType.Employee.getValue(), OppositeType.Customer.getValue(), OppositeType.Other.getValue());
//        //收入: 内部单位 客户 员工 供应商
//        excuteIdentificateByIdentificationTypes(incomeList, context, OppositeType.InnerOrg.getValue(), OppositeType.Customer.getValue(), OppositeType.Employee.getValue(), OppositeType.Supplier.getValue(), OppositeType.Other.getValue());

        //todo 增加客开扩展点

    }

    /**
     * 收入和支出的流水辨识，按照传入的辨识顺序分别辨识
     *
     * @param bankReconciliationList
     * @param identificationType
     * @throws Exception
     */
    public void excuteIdentificateByIdentificationTypes(List<BankReconciliation> bankReconciliationList,BankDealDetailContext context, short... identificationType) throws Exception {
        if(bankReconciliationList.size() > 0){
            List<BankReconciliation> filterBankReconciliationList = bankReconciliationList.stream().filter(e -> e.getOppositetype() == null).collect(Collectors.toList());
            for (short type : identificationType) {
                IntelligentIdentificationService intelligentIdentificationService = this.getIntelligentIdentificationService(type);
                intelligentIdentificationService.excuteIdentificate(filterBankReconciliationList,context);
            }
        }
    }

    /**
     * 收入和支出的流水辨识，按照传入的辨识顺序分别辨识
     *
     * @param bankReconciliationList
     * @param
     * @throws Exception
     */
    public void excuteIdentificateByIdentificationTypesForCheck(List<BankReconciliation> bankReconciliationList,Map<Integer, BankreconciliationIdentifySetting> ruleMap, BankDealDetailContext context,boolean isCheck) throws Exception {
        if(bankReconciliationList.size() > 0 && ruleMap != null){
            //List<BankReconciliation> filterBankReconciliationList = bankReconciliationList.stream().filter(e -> e.getOppositetype() == null).collect(Collectors.toList());
            if (CollectionUtils.isEmpty(bankReconciliationList)){
                return;
            }
            List<Integer> sortedKeys = new ArrayList<>(ruleMap.keySet());
            Collections.sort(sortedKeys);  // 默认升序
            for (Integer orderKey : sortedKeys) {
                BankreconciliationIdentifySetting bankreconciliationIdentifySetting = ruleMap.get(orderKey);
                String matchdes = bankreconciliationIdentifySetting.getMatchdes();
                short type = OppositeType.Other.getValue();
                if (isCheck){
                    if (innerOrg.equals(matchdes)){
                        type = OppositeType.InnerOrg.getValue();
                    }
                    if (supplier.equals(matchdes)){
                        type = OppositeType.Supplier.getValue();
                    }
                    if (employee.equals(matchdes)){
                        type = OppositeType.Employee.getValue();
                    }
                    if (customer.equals(matchdes)){
                        type = OppositeType.Customer.getValue();
                    }
                }
                if (type != 0){
                    IntelligentIdentificationService intelligentIdentificationService = this.getIntelligentIdentificationService(type);
                    intelligentIdentificationService.excuteIdentificateForCheck(bankReconciliationList,bankreconciliationIdentifySetting,bankIdentifyService,context);
                }
            }
            //移除辨识匹配标识
            for (BankReconciliation bankReconciliation : bankReconciliationList){
                bankReconciliation.remove(BankMatchAndProcessUtils.identifiedInformation);
            }
        }
    }


    /**
     * 根据辨识的类型获取相应的服务类
     *
     * @param type
     * @return
     */
    private IntelligentIdentificationService getIntelligentIdentificationService(short type) {
        AtomicReference<IntelligentIdentificationService> intelligentIdentificationService = new AtomicReference<>();
        intelligentIdentificationServiceMap.forEach((key, value) -> {
            if (value.getTypeValue() == type && intelligentIdentificationServiceMap.get(key) != null) {
                intelligentIdentificationService.set(intelligentIdentificationServiceMap.get(key));
            }
        });
        return intelligentIdentificationService.get();
    }

}
