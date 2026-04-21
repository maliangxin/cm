package com.yonyoucloud.fi.cmp.margincommon.refer;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.FilterVO;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.SimpleFilterVO;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.cmpentity.EventSource;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.constant.IRefCodeConstant;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.BizObject;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 单据类型过滤规则*
 *
 * @author xuxbo
 * @date 2023/8/15 16:03
 */


@Slf4j
@Component
public class PayAndReciveMarginBillTypeReferFilterRule extends AbstractCommonRule {
    /**
     * 需要进行过滤的billnum集合
     */
    private static final List<String> BILLNUM_MAP = new ArrayList<>();

    public PayAndReciveMarginBillTypeReferFilterRule() {
        BILLNUM_MAP.add(IBillNumConstant.CMP_PAYMARGIN);
        BILLNUM_MAP.add(IBillNumConstant.CMP_RECEIVEMARGIN);
    }

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        BillDataDto billDataDto = (BillDataDto) getParam(map);
        String billnum = billDataDto.getBillnum();
        if (BILLNUM_MAP.contains(billnum)) {
//            RefEntity refentity = billDataDto.getRefEntity();
            if (IRefCodeConstant.TRANSTYPE_BD_BILLTYPE_GRIDREF.equals(billDataDto.getrefCode())) {
                List<BizObject> bizObjectList = (ArrayList) billDataDto.getData();
                if (bizObjectList.size() < 1 || bizObjectList == null) {
                    return new RuleExecuteResult();
                }
                Short srcitem = bizObjectList.get(0).get("srcitem");
                String billtype = "CM";//现金管理
                if (ObjectUtils.isNotEmpty(srcitem)) {
                    if (srcitem.equals(EventSource.Drftchase.getValue())) {
                        billtype = "DRFT";//商业汇票
                    } else if (srcitem.equals(EventSource.CREDITMANAGEMENT.getValue())) {
                        //授信管理
                        billtype = "CAM";
                    } else if (srcitem.equals(EventSource.IFMANAGEMENT.getValue())) {
                        //投融资管理
                        billtype = "TLM";
                    } else if (srcitem.equals(EventSource.GuaranteeManagement.getValue())) {
                        //保函管理
                        billtype = "LGM";
                    } else if (srcitem.equals(EventSource.LetterCreditManagement.getValue())) {
                        //信用证管理
                        billtype = "LCM";
                    } else if (srcitem.equals(EventSource.BidMargin.getValue())) {
                        //投标保证金
                        billtype = "SFA";
                    }
                } else {
                    srcitem = EventSource.Cmpchase.getValue();
                }

                if (billDataDto.getCondition() == null) {
                    FilterVO conditon = new FilterVO();
                    conditon.appendCondition(ConditionOperator.and, new SimpleFilterVO("sysid", "eq", billtype));
                    //是否期初为是、事项来源为现金管理时，只能选择收到保证金台账管理。
                    Short initflag = bizObjectList.get(0).get("initflag");
                    if (initflag == 1 && srcitem.equals(EventSource.Cmpchase.getValue())) {
                        if (billnum.equals(IBillNumConstant.CMP_PAYMARGIN)) {
                            conditon.appendCondition(ConditionOperator.and, new SimpleFilterVO("code", "eq", IBillNumConstant.CMP_PAYMARGIN));
                        } else if (billnum.equals(IBillNumConstant.CMP_RECEIVEMARGIN)) {
                            conditon.appendCondition(ConditionOperator.and, new SimpleFilterVO("code", "eq", IBillNumConstant.CMP_RECEIVEMARGIN));
                        }

                    }
                    billDataDto.setCondition(conditon);
                } else {
                    billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("sysid", "eq", billtype));
                    //是否期初为是、事项来源为现金管理时，只能选择收到保证金台账管理。
                    Short initflag = bizObjectList.get(0).get("initflag");
                    if (initflag == 1 && srcitem.equals(EventSource.Cmpchase.getValue())) {
                        if (billnum.equals(IBillNumConstant.CMP_PAYMARGIN)) {
                            billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("code", "eq", IBillNumConstant.CMP_PAYMARGIN));
                        } else if (billnum.equals(IBillNumConstant.CMP_RECEIVEMARGIN)) {
                            billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("code", "eq", IBillNumConstant.CMP_RECEIVEMARGIN));
                        }

                    }
                }
            }
        }
        return new RuleExecuteResult();
    }
}
