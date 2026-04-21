package com.yonyoucloud.fi.cmp.check.rule;

import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.iuap.ucf.common.i18n.MessageUtils;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import lombok.extern.slf4j.Slf4j;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

@Slf4j
@Component("appendBaseRateCheckRule")
public class AppendBaseRateCheckRule extends AbstractCommonRule {

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        RuleExecuteResult ruleResult = new RuleExecuteResult();
        BizObject data = getBills(billContext, paramMap).get(0);
        BillDataDto billDataDto = (BillDataDto) getParam(paramMap);
        if (billDataDto != null && billDataDto.getItem() != null) {
            if ("cmp_withholdingrulesettingpopover".equals(billDataDto.getBillnum())) {
                CtmJSONObject itemObj = CtmJSONObject.parseObject(billDataDto.getItem());
                if ("agreeIRGrade".equals(itemObj.getString("childrenField"))) {
                    //基准利率类型
                    if ("baseirtype_name".equals(itemObj.getString("key"))) {
                        int location = itemObj.getIntValue("location");

                        //开始日期
                        Object agreestartdate = data.get("agreestartdate");

                        List<Map> agreeIRGrade = (List<Map>) data.get("agreeIRGrade");
                        //基准利率类型
                        String baseirtype = agreeIRGrade.get(location).get("baseirtype").toString();

                        BigDecimal rate = queryDepositInterestRate(Long.parseLong(baseirtype), agreestartdate);
                        if (rate == null) {
                            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102082"),MessageUtils.getMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1C9367A205280009", "未匹配到有效基准利率!") /* "未匹配到有效基准利率!" */));
                        }
                        //基准利率赋值
                        agreeIRGrade.get(location).put("baseir", rate);
                        //浮动值
                        if (agreeIRGrade.get(location).get("floatvalue") != null) {
                            //计算利率
                            BigDecimal interestrate = (new BigDecimal(agreeIRGrade.get(location).get("floatvalue").toString())).divide(new BigDecimal(100)).add(rate);
                            if(interestrate.compareTo(BigDecimal.ZERO) < 0){
                                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102083"),MessageUtils.getMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1CCB458A04500002", "基准利率加浮动利率不能小于0！") /* "未匹配到有效基准利率!" */));
                            }
                            agreeIRGrade.get(location).put("interestrate", interestrate);
                        }

                    }
                }
            } else if ("cmp_agreeirsetting".equals(billDataDto.getBillnum())) {
                CtmJSONObject itemObj = CtmJSONObject.parseObject(billDataDto.getItem());
                if ("agreeIRSettingGrade".equals(itemObj.getString("childrenField"))) {
                    //基准利率类型
                    if ("baseirtype_name".equals(itemObj.getString("key"))) {
                        int location = itemObj.getIntValue("location");
                        //开始日期
                        Object agreestartdate = data.get("agreestartdate");
                        List<Map> agreeIRSetting = (List<Map>) data.get("agreeIRSettingGrade");
                        //基准利率类型
                        String baseirtype = agreeIRSetting.get(location).get("baseirtype").toString();
                        BigDecimal rate = queryDepositInterestRate(Long.parseLong(baseirtype), agreestartdate);
                        if (rate == null) {
                            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102082"),MessageUtils.getMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1C9367A205280009", "未匹配到有效基准利率!") /* "未匹配到有效基准利率!" */));
                        }
                        //基准利率赋值
                        agreeIRSetting.get(location).put("baseir", rate);
                        //浮动值
                        if (agreeIRSetting.get(location).get("floatvalueaddsub") != null) {
                            //计算利率
                            BigDecimal interestrate = (new BigDecimal(agreeIRSetting.get(location).get("floatvalueaddsub").toString())).divide(new BigDecimal(100)).add(rate);
                            if(interestrate.compareTo(BigDecimal.ZERO) < 0){
                                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102083"),MessageUtils.getMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1CCB458A04500002", "基准利率加浮动利率不能小于0！") /* "未匹配到有效基准利率!" */));
                            }
                            agreeIRSetting.get(location).put("interestrate", interestrate);
                        }
                    }
                }
            } else if ("cmp_agreeirsettingdetail".equals(billDataDto.getBillnum())) {
                CtmJSONObject itemObj = CtmJSONObject.parseObject(billDataDto.getItem());
                if ("agreeIRSettingGradeHistory".equals(itemObj.getString("childrenField"))) {
                    //基准利率类型
                    if ("baseirtype_name".equals(itemObj.getString("key"))) {
                        int location = itemObj.getIntValue("location");
                        List<Map> agreeIRSettingHistoryList = data.get("agreeIRSettingHistory");
                        Map<String, Object> agreeIRSettingHistory = agreeIRSettingHistoryList.get(0);
                        if (agreeIRSettingHistory.get("startDate") != null && agreeIRSettingHistory.get("agreeIRSettingGradeHistory") != null) {
                            Object agreestartdate = agreeIRSettingHistory.get("startDate");
                            List<Map> agreeIRSetting = (List<Map>) agreeIRSettingHistory.get("agreeIRSettingGradeHistory");
                            //基准利率类型
                            String baseirtype = agreeIRSetting.get(location).get("baseirtype").toString();
                            BigDecimal rate = queryDepositInterestRate(Long.parseLong(baseirtype), agreestartdate);
                            if (rate == null) {
                                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102082"),MessageUtils.getMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1C9367A205280009", "未匹配到有效基准利率!") /* "未匹配到有效基准利率!" */));
                            }
                            //基准利率赋值
                            agreeIRSetting.get(location).put("baseir", rate);
                            //浮动值
                            if (agreeIRSetting.get(location).get("floatvalue") != null) {
                                //计算利率
                                BigDecimal interestrate = (new BigDecimal(agreeIRSetting.get(location).get("floatvalue").toString())).divide(new BigDecimal(100)).add(rate);
                                if(interestrate.compareTo(BigDecimal.ZERO) < 0){
                                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102083"),MessageUtils.getMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1CCB458A04500002", "基准利率加浮动利率不能小于0！") /* "未匹配到有效基准利率!" */));
                                }
                                agreeIRSetting.get(location).put("interestrate", interestrate);
                            }
                        }
                    }
                }
            }
            this.putParam(paramMap, "return", data);
        }

        return ruleResult;
    }

    /**
     * 查询“存款利率”档案的利率
     *
     * @param rateType 利率类型
     * @param rateDate 利率日期
     * @throws Exception
     */
    private BigDecimal queryDepositInterestRate(Long rateType, Object rateDate) throws Exception {
        QueryConditionGroup conditionGroup = QueryConditionGroup.and(QueryCondition.name("rateType").eq(rateType), QueryCondition.name("rateDate").elt(rateDate));
        QuerySchema querySchema = QuerySchema.create().addSelect("rate").addCondition(conditionGroup).addOrderBy("rateDate desc");
        querySchema.setLimitCount(1);
        List<BizObject> list = MetaDaoHelper.queryObject("tlm.depositinterestrate.DepositInterestRate", querySchema, "yonbip-fi-ctmtlm");
        if (list != null && list.size() > 0) {
            return list.get(0).get("rate");
        }
        return null;
    }

}
