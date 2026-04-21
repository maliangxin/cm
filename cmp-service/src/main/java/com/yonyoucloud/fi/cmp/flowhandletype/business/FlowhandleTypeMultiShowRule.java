package com.yonyoucloud.fi.cmp.flowhandletype.business;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.ucf.mdf.api.enums.service.MdfEnumService;
import com.yonyoucloud.fi.cmp.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 流水处理类型左树预置数据多语翻译
 */
@Component("flowhandleTypeMultiShowRule")
@Slf4j
public class FlowhandleTypeMultiShowRule extends AbstractCommonRule {
    @Autowired
    private MdfEnumService mdfEnumService;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        BillDataDto billDataDto = (BillDataDto) getParam(map);
        List<Map<String, Object>> recordList = (List) map.get("return");
        if (CollectionUtils.isNotEmpty(recordList)) {
            Map<String, String> enumMap = new HashMap<>();

            if ("cmp_flowhandleTypeList".equals(billDataDto.getBillnum())) {
                //流水处理规则
                enumMap = mdfEnumService.getEnumMap("cmp_flowhandlestage");
            } else if ("cmp_fcdsUsesetTypeList".equals(billDataDto.getBillnum())) {
                //流水处理使用数据源设置
                enumMap = mdfEnumService.getEnumMap("cmp_billaction");
            }

            for (Map<String, Object> rec : recordList) {
                if (rec.get("code") != null) {
                    String enumName = enumMap.get(rec.get("code"));
                    rec.put("name", StringUtils.isEmpty(enumName) ? rec.get("name") : enumName);
                }
            }
        }
        return new RuleExecuteResult();
    }
}
