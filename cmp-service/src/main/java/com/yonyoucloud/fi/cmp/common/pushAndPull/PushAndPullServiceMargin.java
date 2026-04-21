package com.yonyoucloud.fi.cmp.common.pushAndPull;

import com.yonyou.ucf.mdd.common.enums.OperationTypeEnum;
import com.yonyou.ucf.mdd.common.model.rule.RuleContext;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.billmake.model.MakeBillRule;
import com.yonyou.ucf.mdd.ext.bill.billmake.rule.crud.BeforePullAndPushRule;
import com.yonyou.ucf.mdd.ext.bill.billmake.rule.crud.DivideVoucherForPullRule;
import com.yonyou.ucf.mdd.ext.bill.billmake.rule.crud.QueryPullAndPushRule;
import com.yonyou.ucf.mdd.ext.bill.billmake.service.MakeBillRuleService;
import com.yonyou.ucf.mdd.ext.bill.billmake.vo.PushAndPullVO;
import com.yonyou.ucf.mdd.ext.consts.Constants;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.pushAndPull.PushAndPullModel;
import org.imeta.core.base.ObjectUtils;
import org.imeta.core.lang.BooleanUtils;
import org.imeta.orm.base.BizObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 推拉单工具类
 *
 * @author mal
 * @since 2023-07-11
 */
@Service
public class PushAndPullServiceMargin {

    @Resource
    private MakeBillRuleService makeBillRuleService;
    @Resource
    private QueryPullAndPushRule queryPullAndPushRule;
    @Resource
    private DivideVoucherForPullRule divideVoucherForPullRule;
    @Resource
    private BeforePullAndPushRule beforePullAndPushRule;

    /**
     * 根据单据转换code及分组code转换对应单据
     *
     * @param bills
     * @param pushAndPullModel
     */
    public BizObject transformBillByMakeBillCode(List<BizObject> bills, PushAndPullModel pushAndPullModel) throws Exception {
        PushAndPullVO pushAndPullVO = new PushAndPullVO();
        pushAndPullVO.setCode(pushAndPullModel.getCode());
        pushAndPullVO.setGroupCode(pushAndPullModel.getGroupCode());
        pushAndPullVO.setIsMainSelect(pushAndPullModel.getIsMainSelect());//0   根据子表的ID 查找所有数据;1  根据主表的ID 查找所有数据
        pushAndPullVO.setSourceData(bills);
        pushAndPullVO.setMainIds(pushAndPullModel.getMainids());// 目标数据上下文
        pushAndPullVO.setChildIds(pushAndPullModel.getChilds());
        MakeBillRule makeBillRule = makeBillRuleService.findDetailListByGroup(pushAndPullVO.getCode(), pushAndPullVO.getGroupCode());
        BillContext billMappingOrgin = makeBillRuleService.getBillByCode(makeBillRule.getOrigin_type());
        if (ObjectUtils.isEmpty(billMappingOrgin)){
            return new BizObject();
        }
        BillContext billContext = (BillContext) billMappingOrgin.clone();
        Map<String, Object> content = this.assembleContent(pushAndPullVO, makeBillRule, billMappingOrgin, billContext);
        queryPullAndPushRule.execute(billContext, content);//查询转单数据
        if (BooleanUtils.b(pushAndPullModel.isNeedDivide(), false)) {
            divideVoucherForPullRule.execute(billContext, content);//分单子表
        }
        RuleExecuteResult execute = beforePullAndPushRule.execute(billContext, content);//转换规则
        Map<String, Object> data = (Map<String, Object>) execute.getData();
        //转换后的数据   实体接收 有的数据类型不对
        BizObject transferBiz = new BizObject();
        List<Map<String, Object>> tarList = (List<Map<String, Object>>) data.get("tarList");
        for (Map<String, Object> target : tarList) {
            if (target.get(pushAndPullModel.getOriDefName()) != null) {  //新架构下 特征转换
                transferBiz.put(pushAndPullModel.getTarDefName(), target.get(pushAndPullModel.getOriDefName()));
            }
            for (Map.Entry<String, Object> itemMap : target.entrySet()) {
                transferBiz.set(itemMap.getKey(), itemMap.getValue());
            }
        }
        return transferBiz;
    }


    /**
     * 根据单据转换code及分组code转换对应单据
     * @param bills
     * @param pushAndPullModel
     */
    public List<BizObject> transformBillByMakeBillCodeAll(List<BizObject> bills, PushAndPullModel pushAndPullModel) throws Exception {
        PushAndPullVO pushAndPullVO = new PushAndPullVO();
        pushAndPullVO.setCode(pushAndPullModel.getCode());
        pushAndPullVO.setGroupCode(pushAndPullModel.getGroupCode());
        pushAndPullVO.setIsMainSelect(pushAndPullModel.getIsMainSelect());//0   根据子表的ID 查找所有数据;1  根据主表的ID 查找所有数据
        pushAndPullVO.setSourceData(bills);
        pushAndPullVO.setMainIds(pushAndPullModel.getMainids());// 目标数据上下文
        pushAndPullVO.setChildIds(pushAndPullModel.getChilds());
        MakeBillRule makeBillRule = makeBillRuleService.findDetailListByGroup(pushAndPullVO.getCode(), pushAndPullVO.getGroupCode());
        BillContext billMappingOrgin = makeBillRuleService.getBillByCode(makeBillRule.getOrigin_type());
        BillContext billContext = (BillContext) billMappingOrgin.clone();
        Map<String, Object> content = this.assembleContent(pushAndPullVO, makeBillRule, billMappingOrgin, billContext);
        queryPullAndPushRule.execute(billContext, content);//查询转单数据
        if (BooleanUtils.b(pushAndPullModel.isNeedDivide(), false)) {
            divideVoucherForPullRule.execute(billContext, content);//分单子表
        }
        RuleExecuteResult execute = beforePullAndPushRule.execute(billContext, content);//转换规则
        Map<String, Object> data = (Map<String, Object>) execute.getData();
        //转换后的数据   实体接收 有的数据类型不对
        List<Map<String, Object>> tarList = (List<Map<String, Object>>) data.get("tarList");
        List<BizObject> resultList = new ArrayList<>();
        for (Map<String, Object> target : tarList) {
            BizObject transferBiz = new BizObject();
            for (Map.Entry<String, Object> itemMap : target.entrySet()) {
                transferBiz.set(itemMap.getKey(), itemMap.getValue());
            }
            resultList.add(transferBiz);
        }
        return resultList;
    }

    /**
     * 组装参数
     *
     * @param pushAndPullVO
     * @param makeBillRule
     * @param billMappingOrgin
     * @param billContext
     * @return
     * @throws Exception
     */
    private Map<String, Object> assembleContent(PushAndPullVO pushAndPullVO, MakeBillRule makeBillRule, BillContext billMappingOrgin, BillContext billContext) throws Exception {
        Map<String, Object> content = new HashMap<String, Object>();
        String type = Constants.PULLTYPE;
        String code = pushAndPullVO.getCode();
        boolean isBusiObj = true;
        content.put("isBusiObj", isBusiObj);//用于转单之前处理数据标记
        billContext.setBillnum(code);//用界面传过来的值 ，业务对象的code后面会有_businessobject
        BillContext billMappingTar = makeBillRuleService.getBillByCode(makeBillRule.getTarget_type());
        billContext.setSubid(billMappingTar.getSubid());
        String fullNameTar = billMappingTar.getFullname();
        content.put("pushAndPullVO", pushAndPullVO);
        content.put("makeBillRule", makeBillRule);
        content.put("type", type);
        content.put("fullNameTar", fullNameTar);
        content.put("sourceFullName", billMappingOrgin.getFullname());
        content.put("voList", pushAndPullVO.getList());
        content.put("orignalBillContext", billMappingOrgin);
        content.put("targetBillContext", billMappingTar);
        content.put("sourceDatas", pushAndPullVO.getData());
        content.put("externalData", pushAndPullVO.getExternalData());

        RuleContext ruleContext = new RuleContext();
        ruleContext.setMakeup(false);
        ruleContext.setAction(OperationTypeEnum.PULLANDPUSH.getValue());
        ruleContext.setCustomMap(content);
        ruleContext.setBillContext(billContext);

        if (billContext != null && billContext.getTenant() == null) { // 参照的场景 billContext 是new 出来的，租户为空，需要补充当前租户到ruleContext
            billContext.setTenant(AppContext.getTenantId());
            ruleContext.setTenantId(AppContext.getTenantId());
        }
        return content;
    }


}
