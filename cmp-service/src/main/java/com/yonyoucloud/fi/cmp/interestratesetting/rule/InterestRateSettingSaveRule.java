package com.yonyoucloud.fi.cmp.interestratesetting.rule;

import com.yonyou.iuap.ucf.common.i18n.MessageUtils;
import com.yonyou.iuap.yms.id.generator.YmsOidGenerator;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.ucf.mdd.ext.model.LoginUser;
import com.yonyoucloud.fi.cmp.cmpentity.OptionType;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.interestratesetting.InterestRateSetting;
import com.yonyoucloud.fi.cmp.util.CmpMetaDaoHelper;
import com.yonyoucloud.fi.cmp.util.DateUtils;
import com.yonyoucloud.fi.cmp.util.MetaDaoUtils;
import com.yonyoucloud.fi.cmp.withholding.InterestRateSettingHistory;
import com.yonyoucloud.fi.cmp.withholding.WithholdingRuleSetting;
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 银行账户利率设置保存规则*
 *
 * @author xuxbo
 * @date 2023/4/24 14:15
 */

@Component
public class InterestRateSettingSaveRule extends AbstractCommonRule {

    private static final String END_DATE = "9999-12-31";

    @Autowired
    YmsOidGenerator ymsOidGenerator;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        List<BizObject> bills = getBills(billContext, map);
        if (CollectionUtils.isNotEmpty(bills)) {
            InterestRateSetting interestRateSetting = (InterestRateSetting) bills.get(0);
            if (Objects.nonNull(interestRateSetting)) {
                if (EntityStatus.Update.equals(interestRateSetting.getEntityStatus())) {
                    List<InterestRateSetting> interestRateSettings = bills.stream().map(bill -> (InterestRateSetting) bill).collect(Collectors.toList());
                    //保存利率变更历史记录
                    saveInterestRateSettingHistory(interestRateSettings);
                    //更新预提规则设置的版本号
                    List<WithholdingRuleSetting> withholdingRuleSettingList = new ArrayList<>();
                    for (InterestRateSetting bill : interestRateSettings) {
                        //获取到预提规则设置的id
                        Long id = bill.getAccountNumberId();
                        WithholdingRuleSetting withholdingRuleSetting = MetaDaoHelper.findById(WithholdingRuleSetting.ENTITY_NAME, id);
                        withholdingRuleSetting.setVersion(ymsOidGenerator.nextId());
                        withholdingRuleSetting.setEntityStatus(EntityStatus.Update);
                        withholdingRuleSettingList.add(withholdingRuleSetting);
                    }
                    MetaDaoHelper.update(WithholdingRuleSetting.ENTITY_NAME, withholdingRuleSettingList);
                    return new RuleExecuteResult();
                }
            }
        }
        return new RuleExecuteResult();
    }

    /**
     * *
     *
     * @param interestRateSettings
     * @throws Exception
     */
    private void saveInterestRateSettingHistory(List<InterestRateSetting> interestRateSettings) throws Exception {
        List<InterestRateSettingHistory> updateHistoryList = new ArrayList<>();
        List<InterestRateSettingHistory> insertHistoryList = new ArrayList<>();
        //利率设置时，根据条件新增或更新历史记录
        updateHistory(interestRateSettings, updateHistoryList, insertHistoryList);

        // 新增历史记录
        if (CollectionUtils.isNotEmpty(insertHistoryList)) {
            CmpMetaDaoHelper.insert(InterestRateSettingHistory.ENTITY_NAME, insertHistoryList);
        }
        // 更新历史记录
        if (CollectionUtils.isNotEmpty(updateHistoryList)) {
            MetaDaoHelper.update(InterestRateSettingHistory.ENTITY_NAME, updateHistoryList);
        }
    }

    /**
     * *
     *
     * @param interestRateSettings
     * @param updateHistoryList
     * @param insertHistoryList
     * @throws Exception
     */
    private void updateHistory(List<InterestRateSetting> interestRateSettings, List<InterestRateSettingHistory> updateHistoryList,
                               List<InterestRateSettingHistory> insertHistoryList) throws Exception {
        //获取利率设置表中的 预提规则设置id
        List<Long> mainIds = interestRateSettings.stream().mapToLong(InterestRateSetting::getAccountNumberId).boxed().collect(Collectors.toList());
        //根据mainid查询出历史数据
        List<InterestRateSettingHistory> interestRateSettingHistoryList = MetaDaoUtils.batchQueryBizObject(InterestRateSettingHistory.ENTITY_NAME, ICmpConstant.MAINID, mainIds);
        if (CollectionUtils.isNotEmpty(interestRateSettingHistoryList)) {
            //按照mainid分类
            Map<Long, List<InterestRateSettingHistory>> historyMap = interestRateSettingHistoryList.stream().collect(Collectors.groupingBy(InterestRateSettingHistory::getMainid));
            updateHistoryByGroup(interestRateSettings, historyMap, updateHistoryList, insertHistoryList);
        } else {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100553"),MessageUtils.getMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418079F","银行账户利率设置变更记录查询失败！") /* "银行账户利率设置变更记录查询失败！" */));

        }
    }

    /**
     * *
     *
     * @param interestRateSettings
     * @param historyMap
     * @param updateHistoryList
     * @param insertHistoryList
     * @throws Exception
     */
    private void updateHistoryByGroup(List<InterestRateSetting> interestRateSettings, Map<Long, List<InterestRateSettingHistory>> historyMap,
                                      List<InterestRateSettingHistory> updateHistoryList, List<InterestRateSettingHistory> insertHistoryList) throws Exception {
        LoginUser loginUser = AppContext.getCurrentUser();
        Date now = new Date();
        interestRateSettings.stream().forEach(bill -> {
            List<InterestRateSettingHistory> interestRateSettingHistoryList = historyMap.get(bill.getAccountNumberId());
            if (CollectionUtils.isNotEmpty(interestRateSettingHistoryList)) {
                //todo 需要修改
                //1.遍历interestRateSettingHistoryList 分为两个集合 一个比当前日期大 一个比当前日期小或者相等  当前日期取 bill.getStartDate()
                List<InterestRateSettingHistory> bigHistoryList = new ArrayList<>();
                List<InterestRateSettingHistory> smallHistoryList = new ArrayList<>();
                for (InterestRateSettingHistory historyBill : interestRateSettingHistoryList) {
                    if (historyBill.getStartDate().equals(bill.getStartDate())) {
                        //当相等的时候 插入到小的集合里
                        smallHistoryList.add(historyBill);
                        continue;
                    } else if (historyBill.getStartDate().before(bill.getStartDate())) {
                        smallHistoryList.add(historyBill);
                        continue;
                    } else {
                        bigHistoryList.add(historyBill);
                        continue;
                    }
                }
                try {
                    buildInterestRateSettingHistory(bill, updateHistoryList, insertHistoryList, loginUser, smallHistoryList, bigHistoryList);
                } catch (Exception e) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100554"),e.getMessage());
                }


            }
        });
    }

    /**
     * *
     *
     * @param bill
     * @param updateHistoryList
     * @param insertHistoryList
     * @param loginUser
     * @param smallHistoryList
     * @param bigHistoryList
     * @throws Exception
     */
    private void buildInterestRateSettingHistory(InterestRateSetting bill, List<InterestRateSettingHistory> updateHistoryList, List<InterestRateSettingHistory> insertHistoryList, LoginUser loginUser,
                                                 List<InterestRateSettingHistory> smallHistoryList, List<InterestRateSettingHistory> bigHistoryList) throws Exception {
        Date now = new Date();

        //对两个集合 按照 利率生效日期进行排序 小于的集合倒序  大于的集合正序
        List<InterestRateSettingHistory> newsmallHistoryList = smallHistoryList.stream().sorted(Comparator.comparing(InterestRateSettingHistory::getStartDate, Comparator.reverseOrder())).collect(Collectors.toList());
        InterestRateSettingHistory small = newsmallHistoryList.get(0);
        InterestRateSettingHistory big = new InterestRateSettingHistory();
        //bigHistoryList 可能会为空
        Boolean bigFlag = true;
        //1. 判断small的开始时间是否与 bill.StartDate()相等 如果相等 则直接更新small即可
        //2.先判断 bighistorylist 是否为空，1.如果为空 插入一条新的（与原来的逻辑相同）
        //3.如果不为空 在 small 和 big 中间插入一条nowhistory即可 此时 small 的结束日期为 nowhistory 的开始日期-1 now的结束日期是big的开始日期-1

        if (small.getStartDate().equals(bill.getStartDate())) {
            //只更新存款利率、透支利率、计息天数、利率生效日期，不新增
            small.setInterestRate(bill.getInterestRate());
            small.setOverdraftRate(bill.getOverdraftRate());
            small.setInterestDays(bill.getInterestDays());
            small.setStartDate(bill.getStartDate());
            small.setModifier(loginUser.getName());
            small.setModifierId(loginUser.getId());
            small.setEntityStatus(EntityStatus.Update);
            small.setModifyTime(now);
            small.setModifyDate(now);
            updateHistoryList.add(small);
        } else if (bigHistoryList.size() < 1) {
            InterestRateSettingHistory nowhistory = new InterestRateSettingHistory();
            assembleHistory(bill, nowhistory, insertHistoryList, bigFlag, big);
            //更新small 的结束日期为 nowhistory 的开始日期前一天
            small.setEndDate(DateUtils.preDay(nowhistory.getStartDate()));
            small.setModifier(loginUser.getName());
            small.setModifierId(loginUser.getId());
            small.setEntityStatus(EntityStatus.Update);
            small.setModifyTime(now);
            small.setModifyDate(now);
            updateHistoryList.add(small);
        } else if (bigHistoryList.size() > 0) {
            bigFlag = false;
            List<InterestRateSettingHistory> newbigHistoryList = bigHistoryList.stream().sorted(Comparator.comparing(InterestRateSettingHistory::getStartDate, Comparator.naturalOrder())).collect(Collectors.toList());
            big = newbigHistoryList.get(0);
            InterestRateSettingHistory nowhistory = new InterestRateSettingHistory();
            assembleHistory(bill, nowhistory, insertHistoryList, bigFlag, big);
            //更新small 的结束日期为 nowhistory 的开始日期前一天
            small.setEndDate(DateUtils.preDay(nowhistory.getStartDate()));
            small.setModifier(loginUser.getName());
            small.setModifierId(loginUser.getId());
            small.setEntityStatus(EntityStatus.Update);
            small.setModifyTime(now);
            small.setModifyDate(now);
            updateHistoryList.add(small);
        }

    }


    /**
     * *
     *
     * @param bill
     * @param nowhistory
     * @param insertHistoryList
     * @param bigFlag
     * @param big
     * @throws Exception
     */
    public void assembleHistory(InterestRateSetting bill, InterestRateSettingHistory nowhistory, List<InterestRateSettingHistory> insertHistoryList, Boolean bigFlag, InterestRateSettingHistory big) throws Exception {
        // 获取用户
        LoginUser loginUser = AppContext.getCurrentUser();
        Date endDate;
        if (bigFlag) {
            endDate = DateUtils.parseDate(END_DATE);
        } else {
            endDate = DateUtils.preDay(big.getStartDate());
        }
        Date now = new Date();
        String userName = loginUser.getName();
        Long userId = loginUser.getId();
        nowhistory.setId(ymsOidGenerator.nextId());
        nowhistory.setStartDate(bill.getStartDate());
        nowhistory.setEndDate(endDate);
        nowhistory.setInterestRate(bill.getInterestRate());
        nowhistory.setOverdraftRate(bill.getOverdraftRate());
        nowhistory.setInterestDays(bill.getInterestDays());
        nowhistory.setModifyTime(now);
        nowhistory.setModifierId(userId);
        nowhistory.setModifier(userName);
        nowhistory.setCreateTime(now);
        nowhistory.setCreatorId(userId);
        nowhistory.setCreator(userName);
//        nowhistory.setIsNew(Boolean.TRUE);
        nowhistory.setMainid(bill.getAccountNumberId());
        Long Rateid = Long.parseLong(bill.getId().toString());
        nowhistory.setRateid(Rateid);
        nowhistory.setOptionType(OptionType.Update.getValue());
        nowhistory.setEntityStatus(EntityStatus.Insert);
        insertHistoryList.add(nowhistory);
    }


}
