package com.yonyoucloud.fi.cmp.paymentbill.rule;

import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.iuap.ucf.common.i18n.MessageUtils;
import com.yonyou.iuap.yms.id.generator.YmsOidGenerator;
import com.yonyou.ucf.mdd.common.enums.OperationTypeEnum;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.billmake.model.MakeBillRule;
import com.yonyou.ucf.mdd.ext.bill.billmake.model.MakeBillRuleDetail;
import com.yonyou.ucf.mdd.ext.bill.billmake.service.MakeBillRuleService;
import com.yonyou.ucf.mdd.ext.bill.billmake.vo.PushAndPullVO;
import com.yonyou.ucf.mdd.ext.bill.biz.BillBiz;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.bill.rule.util.BillInfoUtils;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.ucf.mdd.ext.i18n.utils.MddMultilingualUtil;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.ucf.mdd.ext.trans.itf.ISagaRule;
import com.yonyoucloud.fi.cmp.cmpentity.AuditStatus;
import com.yonyoucloud.fi.cmp.common.service.CmCommonService;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.journal.Journal;
import com.yonyoucloud.fi.cmp.journal.JournalService;
import com.yonyoucloud.fi.cmp.paybill.PayBill;
import com.yonyoucloud.fi.cmp.paybill.PayBillCharacterDef;
import com.yonyoucloud.fi.cmp.paybill.PayBillCharacterDefb;
import com.yonyoucloud.fi.cmp.paymentbill.workflow.PaymentBillAuditRule;
import org.apache.commons.collections4.CollectionUtils;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.biz.base.Objectlizer;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 付款单基础规则
 * @author liuttm
 * @version V1.0
 * @date 2021/4/20 16:100
 * @Copyright yonyou
 */
public class FiCmpPaymentBaseRule extends AbstractCommonRule implements ISagaRule {
    @Autowired
    YmsOidGenerator ymsOidGenerator;
    @Autowired
    private MakeBillRuleService makeBillRuleService;

    @Autowired
    private JournalService journalService;

    @Autowired
    private CmCommonService cmCommonService;

    private static org.slf4j.Logger log = LoggerFactory.getLogger(PaymentBillAuditRule.class);

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        return new RuleExecuteResult();
    }

    @Override
    public RuleExecuteResult cancel(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        return new RuleExecuteResult();
    }

    /**
     * 根据请求数据从数据库中查询需操作的单据
     * @param billContext
     * @param paramMap
     * @return
     * @throws Exception
     */
    public List<PayBill> getPaymentFromRequest(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        List<BizObject> bills= getBills(billContext, paramMap);
        String[] ids = new String[bills.size()];
        Set idset = new HashSet();
        for(int i = 0 ; i<bills.size() ; i++){
            ids[i] = bills.get(i).get("id").toString();
        }
        List<PayBill> billList = this.getPayBillBySrcbillIds(ids);
        List<PayBill> newlist = new ArrayList<>();
        for(PayBill bill : billList){
            if(idset.contains(bill.getId())){
                continue;
            }
            idset.add(bill.getId());
            newlist.add(bill);
        }
        return newlist;
    }

    /**
     * 通过srcbillid查询表单
     * @param ids
     * @return
     * @throws Exception
     */
    public List<PayBill> getPayBillBySrcbillIds(String[] ids) throws Exception {
        QuerySchema querySchema = QuerySchema.create().addSelect("*");
        QueryConditionGroup queryConditionGroup = new QueryConditionGroup(ConditionOperator.and);
        queryConditionGroup.appendCondition(QueryCondition.name("srcbillid").in(ids));
        querySchema.addCondition(queryConditionGroup);
        return MetaDaoHelper.queryObject(PayBill.ENTITY_NAME, querySchema, null);
    }

    /**
     * 查询付款单据
     * @param billId
     * @return
     * @throws Exception
     */
    public PayBill getPayBill(Long billId) throws Exception {
        PayBill bill =  MetaDaoHelper.findById(PayBill.ENTITY_NAME,billId,3) ;
        if(bill == null ){
            QuerySchema querySchema = QuerySchema.create().addSelect("id");
            QueryConditionGroup queryConditionGroup = new QueryConditionGroup(ConditionOperator.and);
            queryConditionGroup.appendCondition(QueryCondition.name("srcbillid").in(billId.toString()));
            querySchema.addCondition(queryConditionGroup);
            List<PayBill> bills = MetaDaoHelper.queryObject(PayBill.ENTITY_NAME, querySchema, null);
            if( CollectionUtils.isNotEmpty(bills)){
                bill =  MetaDaoHelper.findById(PayBill.ENTITY_NAME,bills.get(0).getId(),3) ;
            }
        }
        return bill;
    }

    /**
     * 获取来源单据映射的目标单据Dto，参考自平台生单单据转换规则。
     * @param vo
     * @return
     * @throws Exception
     */
    public BillDataDto getTargetDataDto(PushAndPullVO vo) throws Exception {
        BillDataDto billDto = new BillDataDto();
        try {
            log.info("开始单据转换流程#" + InvocationInfoProxy.getYhtAccessToken(), new Object[0]);
            List<Map<String,Object>> targetList = this.getTargetList(vo);
            for(int i = 0; i < targetList.size(); ++i) {
                List<BizObject> bills = new ArrayList();
                BizObject obj = Objectlizer.convert((Map) targetList.get(i), PayBill.ENTITY_NAME);
                bills.add(obj);
                billDto.setBillnum(IBillNumConstant.PAYMENT);
                billDto.setData(bills);
                billDto.setExternalData(vo.getExternalData());
            }
            return billDto;
        } catch (Exception var16) {
            log.error("单据转换流程异常", var16);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100363"),var16.getMessage());
        }
    }

    /**
     * 调用makebillrule获取单据转换对象
     * @param vo
     * @return
     * @throws Exception
     */
    public List<Map<String,Object>> getTargetList(PushAndPullVO vo) throws Exception {
        MakeBillRule makeBillRule = this.makeBillRuleService.findDetailListByGroup(vo.getCode(), vo.getGroupCode());
        if (null == makeBillRule) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100364"),MddMultilingualUtil.getFWMessage("P_YS_FW-PUB_MDD-BACK_0001065718", MessageUtils.getMessage("P_YS_FI_CM_0001237406") /* "生单配置为空!" */));
        } else {
            List<Map<String,Object>> tarList = new ArrayList<>();

            List<MakeBillRuleDetail> details = makeBillRule.makeBillRuleDetailList();
            Map<Integer,List<MakeBillRuleDetail>> groupDetailList = details.stream().collect(Collectors.groupingBy(item -> item.getMapped_relation()));
            List<MakeBillRuleDetail> main2main = groupDetailList.get(new Integer(0));//主对主
            List<MakeBillRuleDetail> main2c = groupDetailList.get(new Integer(1));//主对子
            List<MakeBillRuleDetail> c2c = groupDetailList.get(new Integer(3));//子对子
            List<BizObject>  originlist = vo.getSourceData(); //来源单据
//            originlist.stream().forEach(obj ->{ // 遍历原始单据转换为目标单据
            for(BizObject obj : originlist) {

                BizObject mainobj = new BizObject();
                /**
                 * 单据转换时，需要将自由自定义项拍平，才可以转换，否则转换规则不生效。固定自定义项是全量匹配转换，只需要在预置脚本里按对象转换即可，无需拍平。
                 */
                if(obj.containsKey("headfree")){
                    List<BizObject> headDefines = obj.get("headfree");
                    for(BizObject headDefine : headDefines){
                        for(int i = 1; i<61; i++){
                            if(headDefine.get("define"+i) != null){
                                obj.put("headfree!define"+i,headDefine.get("define"+i));
                            }
                            if(headDefine.get("define"+i+"_name") != null){
                                obj.put("headfree!define"+i+"_name",headDefine.get("define"+i+"_name"));
                            }
                        }
                        if(headDefine.get("id") != null){
                            obj.put("headfree!id",headDefine.get("id"));
                        }
                    }
                }
                if(main2main != null){
                    for(MakeBillRuleDetail detail:main2main){
                        if(detail.getTarget_field().contains("payBillCharacterDef.")){
                            PayBillCharacterDef payBillCharacterDef = new PayBillCharacterDef();
                            payBillCharacterDef.set(detail.getTarget_field().replace("payBillCharacterDef.",""),((BizObject)obj.get("headCharacter")).get(detail.getOrigin_field().replace("headCharacter.","")));
                            payBillCharacterDef.setId(ymsOidGenerator.nextId());
                            mainobj.set("payBillCharacterDef",payBillCharacterDef);
                        }else{
                            mainobj.set(detail.getTarget_field(),obj.get(detail.getOrigin_field()));
                        }
                    }
                }

                Object childValue = obj.get("PayBillb");
                List<BizObject> childValues = null;
                List<BizObject> childTargetValues = new ArrayList<>();
                if (childValue != null && childValue instanceof List && c2c != null ) {
                    childValues = (List<BizObject>) childValue;
                    for (BizObject child : childValues) {
                        BizObject childObjTar = new BizObject();
                        if(child.containsKey("bodyfree")){
                            List<BizObject> bodyDefines = child.get("bodyfree");
                            for(BizObject bodyDefine : bodyDefines){
                                for(int i = 1; i<61; i++){
                                    if(bodyDefine.get("define"+i) != null){
                                        child.put("bodyfree!define"+i,bodyDefine.get("define"+i));
                                    }
                                    if(bodyDefine.get("define"+i+"_name") != null){
                                        child.put("bodyfree!define"+i+"_name",bodyDefine.get("define"+i+"_name"));
                                    }
                                }
                                if(bodyDefine.get("id") != null){
                                    child.put("bodyfree!id",bodyDefine.get("id"));
                                }
                            }
                        }
                        for (MakeBillRuleDetail detail : c2c) {
                            if(detail.getTarget_field().contains("receiveBillCharacterDefb.")){
                                PayBillCharacterDefb payBillCharacterDefb = new PayBillCharacterDefb();
                                payBillCharacterDefb.set(detail.getTarget_field().replace("receiveBillCharacterDefb.",""),((BizObject)child.get("bodyCharacter")).get(detail.getOrigin_field().replace("bodyCharacter.","")));
                                payBillCharacterDefb.setId(ymsOidGenerator.nextId());
                                childObjTar.set("payBillCharacterDefb",payBillCharacterDefb);
                            }else{
                                childObjTar.set(detail.getTarget_field(),child.get(detail.getOrigin_field()));
                            }
                        }
                        childTargetValues.add(childObjTar);
                    }
                }
                if (childTargetValues != null && childTargetValues.size() > 0 && main2c != null) {
                    for (MakeBillRuleDetail detail : main2c) {
                        for(BizObject child:childTargetValues){
                            if(detail.getTarget_field().contains(" .")){
                                PayBillCharacterDefb payBillCharacterDefb = new PayBillCharacterDefb();
                                payBillCharacterDefb.set(detail.getTarget_field().replace("payBillCharacterDefb.",""),((BizObject)obj.get("headCharacter")).get(detail.getOrigin_field().replace("headCharacter.","")));
                                payBillCharacterDefb.setId(ymsOidGenerator.nextId());
                                mainobj.set("payBillCharacterDefb",payBillCharacterDefb);
                            }else{
                                child.set(detail.getTarget_field(),obj.get(detail.getOrigin_field()));
                            }
                        }
                    }
                }
                mainobj.set("PayBillb", childTargetValues);
                tarList.add(mainobj);
            }
//            });
            return tarList;
        }
    }

    /**
     * 审核流程
     * @param payBill
     * @throws Exception
     */
    public void audit(PayBill payBill) throws Exception{
        if (payBill == null) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100365"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180097","请选择单据！") /* "请选择单据！" */);
        }
        List<Journal> journalList = new ArrayList<Journal>();
        payBill.setAuditstatus(AuditStatus.Complete);
        payBill.setAuditorId(AppContext.getCurrentUser().getId());
        payBill.setAuditor(AppContext.getCurrentUser().getName());
        payBill.setAuditTime(new Date());
        payBill.setAuditDate(BillInfoUtils.getBusinessDate());
        journalList.addAll(journalService.updateJournalByBill(payBill));
        payBill.setEntityStatus(EntityStatus.Update);
        MetaDaoHelper.update(PayBill.ENTITY_NAME, payBill);
        if (!CollectionUtils.isEmpty(journalList)) {
            MetaDaoHelper.update(Journal.ENTITY_NAME, journalList);
        }
    }

    /**
     * 调用推拉单的规则流程获取数据库中配置的映射关系
     * @param billnum
     * @param billContext
     * @param data
     * @return
     * @throws Exception
     */
    public RuleExecuteResult pullAndpush(String billnum, BillContext billContext, Map data) throws Exception {
        RuleExecuteResult result = BillBiz.executeRule(OperationTypeEnum.PULLANDPUSH.getValue(), billContext, data);
        if (result.getMsgCode() != 1) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100366"),result.getMessage());
        } else {
            return result;
        }
    }

    /**
     * 取消审核流程
     * @param payBill
     * @throws Exception
     */
    public void unaudit(PayBill payBill) throws Exception{
        if (payBill == null) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100365"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180097","请选择单据！") /* "请选择单据！" */);
        }
        List<Journal> journalList = new ArrayList<Journal>();
        payBill.setAuditstatus(AuditStatus.Incomplete);
        payBill.setAuditorId(null);
        payBill.setAuditor(null);
        payBill.setAuditTime(null);
        payBill.setAuditDate(null);
        journalList.addAll(journalService.updateJournalByBill(payBill));
        payBill.setEntityStatus(EntityStatus.Update);
        MetaDaoHelper.update(PayBill.ENTITY_NAME, payBill);
        if (!CollectionUtils.isEmpty(journalList)) {
            MetaDaoHelper.update(Journal.ENTITY_NAME, journalList);
        }
    }
}
