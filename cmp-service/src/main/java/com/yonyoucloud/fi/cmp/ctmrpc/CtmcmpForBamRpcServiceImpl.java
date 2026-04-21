package com.yonyoucloud.fi.cmp.ctmrpc;

import com.yonyou.cloud.yts.YtsContext;
import com.yonyou.iuap.yms.id.generator.YmsOidGenerator;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyoucloud.fi.cmp.bankaccountsetting.BankAccountSetting;
import com.yonyoucloud.fi.cmp.journal.Journal;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.api.bankaccountsetting.CtmcmpForBamRpcService;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.api.journal.CtmCmpJournalCommonService;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.bam.BamAccountClosureReqVo;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.bam.BamAccountClosureResVo;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.bankAccountSetting.BankAccountSettingVO;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.common.CommonRequestDataVo;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.common.CommonResponseDataVo;
import com.yonyoucloud.fi.cmp.util.CmpMetaDaoHelper;
import com.yonyoucloud.fi.cmp.util.EntityTool;
import lombok.extern.slf4j.Slf4j;
import org.imeta.biz.base.BizContext;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author: yangjn
 * @Description: 现金管理-账户管理 提供远程接口
 * @Date: Created in 2023/08/05 10:25
 * @Version v1.0
 */
@Service
@Slf4j
public class CtmcmpForBamRpcServiceImpl implements CtmcmpForBamRpcService {

    @Autowired
    YmsOidGenerator ymsOidGenerator;
    @Autowired
    CtmCmpJournalCommonService ctmCmpJournalCommonService;

    /**
     * 账户销户时，会判定该账号在系统中的业务是否已经完结，避免账务错误。需要提供日记账查询接口，供账户管理销户时调用
     * @param queryDataVO
     * @return
     * @throws Exception
     */
    @Override
    public List<BamAccountClosureResVo> queryJournalBalanceForBamClosure(List<BamAccountClosureReqVo> queryDataVO) throws Exception {
        /* 入参
        *
        *  bankaccount	是	银行账号id
        *  currency		是	币种id
        *  isSettled	否	是否包含未结算 0否 1是；默认为0（为空则为否）；为“是”时，包含未审批单据
        * */
        List<BamAccountClosureResVo> resultList = new ArrayList();
        for (BamAccountClosureReqVo bamAccountClosureReqVo : queryDataVO) {
            String bankAccount = bamAccountClosureReqVo.getBankaccount();// bankaccount
            String currency = bamAccountClosureReqVo.getCurrency();// currency
            boolean isSettled = bamAccountClosureReqVo.isSettled();
            // 构建返回实体
            BamAccountClosureResVo resVo = new BamAccountClosureResVo();
            resVo.setBankaccount(bankAccount);
            resVo.setCurrency(currency);
            // 是否包含未结算，目前默认包含的，有在途单据不能销户
            if (isSettled) {
                QuerySchema schema = QuerySchema.create();
                // 账户使用组织，单据编号，借方金额，贷方金额，摘要
                schema.addSelect("id, accentity, billnum, debitoriSum, creditoriSum, description");
                QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
                conditionGroup.appendCondition(QueryCondition.name("bankaccount").eq(bankAccount));
                conditionGroup.appendCondition(QueryCondition.name("currency").eq(currency));
                conditionGroup.appendCondition(QueryCondition.name("settlestatus").eq("1"));
                conditionGroup.appendCondition(QueryCondition.name("initflag").eq("0"));// 非期初数据
                schema.addCondition(conditionGroup);
                List<Map<String, Object>> list = MetaDaoHelper.query(Journal.ENTITY_NAME, schema);
                if (list != null && list.size() > 0) {
                    // 包含在途单据，不能销户
                    List<BamAccountClosureResVo.InnerVO> innerVOList = new ArrayList();
                    for (Map<String, Object> map : list) {
                        BamAccountClosureResVo.InnerVO innerVo = resVo.new InnerVO();
                        innerVo.setAccentity((String) map.get("accentity"));
                        innerVo.setBillnum((String) map.get("billnum"));
                        innerVo.setCreditoriSum(map.get("creditoriSum") == null ? BigDecimal.ZERO : new BigDecimal(map.get("creditoriSum").toString()));
                        innerVo.setDebitoriSum(map.get("debitoriSum") == null ? BigDecimal.ZERO : new BigDecimal(map.get("debitoriSum").toString()));
                        innerVo.setDescription((String) map.get("description"));
                        innerVOList.add(innerVo);
                    }
                    resVo.setList(innerVOList);
                    resVo.setZero(false);
                } else {
                    // 没有在途单据，日记账数据都为已结算或没有日记账数据
                    resVo.setList(new ArrayList());
                    resVo.setZero(isZero(bankAccount, currency));
                }
            } else {
                // 不统计在途单据
                resVo.setList(new ArrayList());
                resVo.setZero(isZero(bankAccount, currency));
            }
            resultList.add(resVo);
        }
        return resultList;
    }

    /**
     * 根据银行账户id，币种id，查询账面原币余额是否为零
     * @param bankAccount
     * @param currency
     * @return
     * @throws Exception
     */
    private boolean isZero(String bankAccount, String currency) throws Exception {
        List<String> keyList = new ArrayList();
        keyList.add(bankAccount + currency);
        List<String> bankaccountList = new ArrayList();
        bankaccountList.add(bankAccount);
        CommonResponseDataVo commonResponseDataVo = ctmCmpJournalCommonService.queryJournalBalanceSettled(bankaccountList,keyList);
//        ctmCmpJournalCommonService.queryJournalBalanceSettled(bankaccountList,keyList);
        if (commonResponseDataVo.getCobookoribalance() != null && commonResponseDataVo.getCobookoribalance().compareTo(BigDecimal.ZERO) != 0) {
            return false;
        } else {
            return true;
        }
    }

    @Override
    public CommonResponseDataVo queryJournalBalanceForBam(CommonRequestDataVo queryDataVo) throws Exception {
        String accentity = queryDataVo.getAccentity();
        List<String> bankaccountList = new ArrayList<String>();
        //参数为Map key:账户id,value:币种id集合
        Map<String,Object> queryDataForMapObj = queryDataVo.getQueryDataForMap();
        //存储key 和结果比较 用于提示哪一个账户没有余额
        List<String> keyList = new ArrayList<String>();
        for(String accountId: queryDataForMapObj.keySet()){
            //一个queryDataForListObj中 accountId对应的value币种会有多个 所以用list存储
            bankaccountList.add(accountId);
            List<String>  currencyList = (List<String>)queryDataForMapObj.get(accountId);
            for(String currency : currencyList){
                keyList.add(accentity+accountId+currency);
            }
        }
        CommonResponseDataVo resultVo = ctmCmpJournalCommonService.queryJournalBalanceSettled(bankaccountList,keyList);
        return resultVo;
    }

    @Override
    public CommonResponseDataVo ctmCmpBankAccountSettingSave(List<BankAccountSettingVO> bankAccountSettings) throws Exception {
        CommonResponseDataVo result = new CommonResponseDataVo();
        try{
            for(BankAccountSettingVO bankAccountSettingVO : bankAccountSettings){
                BankAccountSetting bankaccountSettingNew = new BankAccountSetting();
                //TODO 简强待处理
//                bankaccountSettingNew.init(bankAccountSettingVO);
                bankaccountSettingNew.setEntityStatus(EntityStatus.Insert);
                CmpMetaDaoHelper.insert(BankAccountSetting.ENTITY_NAME, bankaccountSettingNew);
            }
        }catch (Exception e){
            result.setSuccess(false);
            result.setMessage(e.getMessage());
            return result;
        }
        result.setSuccess(true);
        YtsContext.setYtsContext("CTMCMPBANKACCOUNTSETTINGSAVE_DATA", bankAccountSettings);
        return result;
    }

    @Override
    public CommonResponseDataVo ctmCmpBankAccountSettingSaveCancel(List<BankAccountSettingVO> bankAccountSetting) throws Exception {
        log.error("ctmCmpBankAccountSettingSaveCancel start");
        List<BankAccountSetting> bankAccountSettings = (List<BankAccountSetting>) YtsContext.getYtsContext("CTMCMPBANKACCOUNTSETTINGSAVE_DATA");
        log.error("ctmCmpBankAccountSettingSaveCancel=> data={}", bankAccountSettings.toString());
        bankAccountSettings.forEach(bankaccountSetting -> {
            bankaccountSetting.setEntityStatus(EntityStatus.Delete);
        });
        MetaDaoHelper.delete(BankAccountSetting.ENTITY_NAME, bankAccountSettings);
        return new CommonResponseDataVo();
    }

    @Override
    public CommonResponseDataVo ctmCmpBankAccountSettingUpdate(List<BankAccountSettingVO> bankAccountSettings) throws Exception {
        CommonResponseDataVo result = new CommonResponseDataVo();
        //成功账号id记录
        List <String> successAccountIds = new ArrayList<>();
        //成功数量
        int successCount = 0;
        try{
            //查询旧有数据 供回滚使用
            List <String> accountIds = new ArrayList<>();
            for(BankAccountSettingVO vo : bankAccountSettings){
                accountIds.add(vo.getEnterpriseBankAccount());
            }
            List<Map<String, Object>> oldbankAccountSettings = queryBankAccountSetting(accountIds);
            YtsContext.setYtsContext("CTMCMPBANKACCOUNTSETTINGUPDATE_DATA", oldbankAccountSettings);
            if(oldbankAccountSettings!=null){
                List<BankAccountSetting> updateAccountSettings = new ArrayList<>();
                for(Map<String, Object> vo : oldbankAccountSettings){
                    BankAccountSetting oldVo = new BankAccountSetting();
                    oldVo.init(vo);
                    for(BankAccountSettingVO newVo : bankAccountSettings){
                        if(oldVo.getEnterpriseBankAccount().equals(newVo.getEnterpriseBankAccount())){
                            oldVo.setOpenFlag(newVo.getOpenFlag() != null ? newVo.getOpenFlag():oldVo.getOpenFlag());
                            oldVo.setEmpower(newVo.getEmpower()!=null?newVo.getEmpower():oldVo.getEmpower());
                            oldVo.setCustomNo(newVo.getCustomNo()!=null?newVo.getCustomNo():oldVo.getCustomNo());
                            oldVo.setAccStatus(newVo.getAccStatus()!=null?newVo.getAccStatus():oldVo.getAccStatus());
                            successAccountIds.add(oldVo.getEnterpriseBankAccount());
                            successCount ++ ;
                            updateAccountSettings.add(oldVo);
                            break;
                        }
                    }
                }
                EntityTool.setUpdateStatus(updateAccountSettings);
                MetaDaoHelper.update(BankAccountSetting.ENTITY_NAME, updateAccountSettings);
            }
        }catch(Exception e){
            result.setSuccess(false);
            result.setMessage(e.getMessage());
            return result;
        }
        result.setSuccess(true);
        result.setSuccessCount(successCount);
        result.setResultList(successAccountIds);
        return result;
    }

    @Override
    public CommonResponseDataVo ctmCmpBankAccountSettingUpdateCancel(List<BankAccountSettingVO> bankAccountSetting) throws Exception {
        log.error("ctmCmpBankAccountSettingUpdateCancel start");
        List<BankAccountSetting> bankAccountSettings = (List<BankAccountSetting>) YtsContext.getYtsContext("CTMCMPBANKACCOUNTSETTINGUPDATE_DATA");
        log.error("ctmCmpBankAccountSettingUpdateCancel=> data={}", bankAccountSettings.toString());
        bankAccountSettings.forEach(bankaccountSetting -> {
            bankaccountSetting.setEntityStatus(EntityStatus.Update);
        });
        MetaDaoHelper.update(BankAccountSetting.ENTITY_NAME, bankAccountSettings);
        return new CommonResponseDataVo();
    }

    @Override
    public CommonResponseDataVo ctmCmpBankAccountSettingAddOrUpdate(List<BankAccountSettingVO> bankAccountSettings) throws Exception{
        CommonResponseDataVo resullt = new CommonResponseDataVo();
        Map<String,BankAccountSettingVO> handleMap = new HashMap<>();
        try{
            for(BankAccountSettingVO vo : bankAccountSettings){
                handleMap.put(vo.getEnterpriseBankAccount(),vo);
            }
            buildAddOrUpdateRe(handleMap);
            resullt.setSuccess(true);
        }catch(Exception e){
            resullt.setSuccess(false);
            resullt.setMessage(e.getMessage());
        }
        return resullt;
    }

    @Override
    public CommonResponseDataVo ctmCmpBankAccountSettingAddOrUpdateCancel(List<BankAccountSettingVO> bankAccountSettings) throws Exception {
        log.error("ctmCmpBankAccountSettingAddOrUpdateCancel start");
        List<String> insertIds = (List<String>) YtsContext.getYtsContext("CTMCMPBANKACCOUNTSETTINGADD_DATA");
        List<BankAccountSetting> updateBankAccountSettings = (List<BankAccountSetting>) YtsContext.getYtsContext("CTMCMPBANKACCOUNTSETTINGBEFORE_DATA");
        log.error("ctmCmpBankAccountSettingAddOrUpdateCancel=> data={}", bankAccountSettings.toString());
        updateBankAccountSettings.forEach(bankaccountSetting -> {
            bankaccountSetting.setEntityStatus(EntityStatus.Update);
        });
        // 还原更新数据
        if (updateBankAccountSettings.size() > 0) {
            MetaDaoHelper.update(BankAccountSetting.ENTITY_NAME, updateBankAccountSettings);
        }
        // 删除新增数据
        if (insertIds.size() > 0) {
            List<BankAccountSetting> deleteList = insertIds.stream().map(item -> {BankAccountSetting bankAccountSetting = new BankAccountSetting();
                Long id = Long.parseLong(item);
                bankAccountSetting.setId(id);
                return bankAccountSetting;
            }).collect(Collectors.toList());
            MetaDaoHelper.delete(BankAccountSetting.ENTITY_NAME, deleteList);
        }
        return new CommonResponseDataVo();
    }

    private void buildAddOrUpdateRe(Map<String,BankAccountSettingVO> handleMap) throws Exception {
        //更新数组
        List<BankAccountSetting> updateAccountSettings = new ArrayList<>();
        //插入数组
        List<BankAccountSetting> insertAccountSettings = new ArrayList<>();
        //账户管理提供的数据账户id
        Set<String> bamSet = handleMap.keySet();
        //需要更新的数据id
        Set<String> updateSet = new HashSet<>();
        List<String> ids = bamSet.stream().collect(Collectors.toList());
        List<Map<String, Object>> bankAccountSettings = queryBankAccountSetting(ids);
        // 更新前的数据备份
        YtsContext.setYtsContext("CTMCMPBANKACCOUNTSETTINGBEFORE_DATA", bankAccountSettings);
        //说明有更新的数据
        if(bankAccountSettings!=null){
            for(Map<String, Object> vo : bankAccountSettings){
                BankAccountSetting oldVo = new BankAccountSetting();
                oldVo.init(vo);
                BankAccountSettingVO newVo = handleMap.get(oldVo.getEnterpriseBankAccount());
                oldVo.setOpenFlag(newVo.getOpenFlag() != null ? newVo.getOpenFlag() : oldVo.getOpenFlag());
                oldVo.setEmpower(newVo.getEmpower()!=null?newVo.getEmpower():oldVo.getEmpower());
                oldVo.setCustomNo(newVo.getCustomNo()!=null?newVo.getCustomNo():oldVo.getCustomNo());
                //RTP9999-20250701_付宇泽协作需求，传空字符串时，清空账户直联状态的字段
                if("".equals(oldVo.getEmpower())){
                    oldVo.setEmpower(null);
                };
                if("".equals(oldVo.getCustomNo())){
                    oldVo.setCustomNo(null);
                };
                oldVo.setAccStatus(newVo.getAccStatus()!=null?newVo.getAccStatus():oldVo.getAccStatus());
                updateAccountSettings.add(oldVo);
                updateSet.add(oldVo.getEnterpriseBankAccount());
            }
            EntityTool.setUpdateStatus(updateAccountSettings);
            MetaDaoHelper.update(BankAccountSetting.ENTITY_NAME, updateAccountSettings);
        }
        //对方传递的数据id 比 更新的多 说明当前有需要插入的
        if(bamSet.size() > updateSet.size()){
            bamSet.removeAll(updateSet);
            //TODO 逻辑需要调整
            for(String enterpriseBankAccountId : bamSet){
//                handleMap.get(enterpriseBankAccountId).setEntityStatus(EntityStatus.Insert);
//                insertAccountSettings.add(handleMap.get(enterpriseBankAccountId));
                BankAccountSettingVO sourceVO = handleMap.get(enterpriseBankAccountId);
                //新老vo转化
                BankAccountSetting insetVo = new BankAccountSetting();
                BeanUtils.copyProperties(sourceVO, insetVo);
                insetVo.setId(ymsOidGenerator.nextId());
                insetVo.setEntityStatus(EntityStatus.Insert);
                //保存新vo
                insertAccountSettings.add(insetVo);
            }
            CmpMetaDaoHelper.insert(BankAccountSetting.ENTITY_NAME, insertAccountSettings);
        }
        // 获取插入的id
        List<String> insertIds = insertAccountSettings.stream().map(item -> item.getId().toString()).collect(Collectors.toList());
        // 需要插入的新数据id
        YtsContext.setYtsContext("CTMCMPBANKACCOUNTSETTINGADD_DATA", insertIds);
    }

    private List<Map<String, Object>> queryBankAccountSetting(List<String> ids) throws Exception {
        QuerySchema schema = QuerySchema.create().addSelect("*");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("enterpriseBankAccount").in(ids));
        schema.addCondition(conditionGroup);
        List<Map<String, Object>> bankAccountSettings = MetaDaoHelper.query(BankAccountSetting.ENTITY_NAME,schema);
        return bankAccountSettings;
    }

}
