package com.yonyoucloud.fi.cmp.receivebill.rule;

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
import com.yonyoucloud.fi.cmp.cmpentity.SettleStatus;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.journal.Journal;
import com.yonyoucloud.fi.cmp.journal.JournalService;
import com.yonyoucloud.fi.cmp.receivebill.ReceiveBill;
import com.yonyoucloud.fi.cmp.receivebill.ReceiveBillCharacterDef;
import com.yonyoucloud.fi.cmp.receivebill.ReceiveBillCharacterDefb;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.biz.base.Objectlizer;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;
import java.util.stream.Collectors;

/**
 *   收款单基础规则
 * @author maliang
 * @version V1.0
 * @date 2021/4/15 15:19
 * @Copyright yonyou
 */
@Slf4j
public class FiCmpReceiveBillBaseRule extends AbstractCommonRule implements ISagaRule {

    @Autowired
    YmsOidGenerator ymsOidGenerator;
    @Autowired
    private MakeBillRuleService makeBillRuleService;

    @Autowired
    private JournalService journalService;

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
    public List<ReceiveBill> getReceiveBillFromRequest(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        List<BizObject> bills= getBills(billContext, paramMap);
        String[] ids = new String[bills.size()];
        log.debug("getReceiveBillFromRequest   billssize :" + bills.size());
        Set idset = new HashSet();
        for(int i = 0 ; i<bills.size() ; i++){
            ids[i] = bills.get(i).get("id").toString();
        }
        List<ReceiveBill> billlist =  this.getReceiveBillBySrcbillIds(ids);
        log.debug("getReceiveBillFromRequest   billlistsize :" + billlist.size());
        List<ReceiveBill> newlist = new ArrayList<>();
        for(ReceiveBill bill : billlist){
            if(idset.contains(bill.getId())){
                continue;
            }
            idset.add(bill.getId());
            newlist.add(bill);
        }
        log.debug("getReceiveBillFromRequest   newlistsize :" + newlist.size());
        return newlist;
    }

    /**
     * 通过srcbillid查询表单
     * @param ids
     * @return
     * @throws Exception
     */
    public List<ReceiveBill> getReceiveBillBySrcbillIds(String[] ids) throws Exception {
        QuerySchema querySchema = QuerySchema.create().addSelect("*");
        QueryConditionGroup queryConditionGroup = new QueryConditionGroup(ConditionOperator.and);
        queryConditionGroup.appendCondition(QueryCondition.name("srcbillid").in(ids));
        querySchema.addCondition(queryConditionGroup);
        return MetaDaoHelper.queryObject(ReceiveBill.ENTITY_NAME, querySchema, null);
    }

    /**
     * 查询收款单据
     * @param billId
     * @return
     * @throws Exception
     */
    public ReceiveBill getReceiveBill(Long billId) throws Exception {
        ReceiveBill bill =  MetaDaoHelper.findById(ReceiveBill.ENTITY_NAME,billId,6) ;
        if(bill == null ){
            QuerySchema querySchema = QuerySchema.create().addSelect("id");
            QueryConditionGroup queryConditionGroup = new QueryConditionGroup(ConditionOperator.and);
            queryConditionGroup.appendCondition(QueryCondition.name("srcbillid").in(billId.toString()));
            querySchema.addCondition(queryConditionGroup);
            List<ReceiveBill> bills = MetaDaoHelper.queryObject(ReceiveBill.ENTITY_NAME, querySchema, null);
            if( CollectionUtils.isNotEmpty(bills)){
                bill =  MetaDaoHelper.findById(ReceiveBill.ENTITY_NAME,bills.get(0).getId(),3) ;
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
                BizObject obj = Objectlizer.convert((Map) targetList.get(i), ReceiveBill.ENTITY_NAME);
                bills.add(obj);
                billDto.setBillnum(IBillNumConstant.RECEIVE_BILL);
                billDto.setData(bills);
                billDto.setExternalData(vo.getExternalData());
            }
            return billDto;
        } catch (Exception var16) {
            log.error("单据转换流程异常", var16);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101236"),var16.getMessage());
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
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101237"),MddMultilingualUtil.getFWMessage("P_YS_FW-PUB_MDD-BACK_0001065718", MessageUtils.getMessage("P_YS_FI_CM_0001237406") /* "生单配置为空!" */));
        } else {
            List<Map<String,Object>> tarList = new ArrayList<>();

            List<MakeBillRuleDetail> details = makeBillRule.makeBillRuleDetailList();
            Map<Integer,List<MakeBillRuleDetail>> groupDetailList = details.stream().collect(Collectors.groupingBy(item -> item.getMapped_relation()));
            List<MakeBillRuleDetail> main2main = groupDetailList.get(new Integer(0));//主对主
            List<MakeBillRuleDetail> main2c = groupDetailList.get(new Integer(1));//主对子
            List<MakeBillRuleDetail> c2c = groupDetailList.get(new Integer(3));//子对子
            List<BizObject>  originlist = vo.getSourceData(); //来源单据
//            originlist.stream().forEach(obj ->{ // 遍历原始单据转换为目标单据
            for(BizObject obj : originlist){
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
                if(main2main != null) {
                    ReceiveBillCharacterDef receiveBillCharacterDef = new ReceiveBillCharacterDef();
                    receiveBillCharacterDef.setId(ymsOidGenerator.nextId());
                    for(MakeBillRuleDetail detail:main2main){
                        if(detail.getTarget_field().contains("receiveBillCharacterDef.")){
                            if(mainobj.get("receiveBillCharacterDef") != null){
                                ((BizObject)mainobj.get("receiveBillCharacterDef")).set(detail.getTarget_field().replace("receiveBillCharacterDef.",""),((BizObject)obj.get("headCharacter")).get(detail.getOrigin_field().replace("headCharacter.","")));
                            }else{
                                receiveBillCharacterDef.set(detail.getTarget_field().replace("receiveBillCharacterDef.",""),((BizObject)obj.get("headCharacter")).get(detail.getOrigin_field().replace("headCharacter.","")));
                                mainobj.set("receiveBillCharacterDef",receiveBillCharacterDef);
                            }
                        }else{
                            mainobj.set(detail.getTarget_field(),obj.get(detail.getOrigin_field()));
                        }
                    }
                }

                Object childValue=obj.get("ReceiveBill_b");
                List<BizObject> childValues=null;
                List<BizObject> childTargetValues=new ArrayList<>();
                if(childValue!=null && childValue instanceof List && c2c != null){
                    childValues=(List<BizObject>)childValue;
                    for(BizObject child:childValues){
                        BizObject childObjTar = new BizObject();
                        ReceiveBillCharacterDefb receiveBillCharacterDefb = new ReceiveBillCharacterDefb();
                        receiveBillCharacterDefb.setId(ymsOidGenerator.nextId());
                        for(MakeBillRuleDetail detail:c2c){
                            if(detail.getTarget_field().contains("receiveBillCharacterDefb.")){
                                if(childObjTar.get("receiveBillCharacterDefb") != null){
                                    ((BizObject)childObjTar.get("receiveBillCharacterDefb")).set(detail.getTarget_field().replace("receiveBillCharacterDefb.",""),((BizObject)child.get("bodyCharacter")).get(detail.getOrigin_field().replace("bodyCharacter.","")));
                                }else{
                                    receiveBillCharacterDefb.set(detail.getTarget_field().replace("receiveBillCharacterDefb.",""),((BizObject)obj.get("bodyCharacter")).get(detail.getOrigin_field().replace("bodyCharacter.","")));
                                    childObjTar.set("receiveBillCharacterDefb",receiveBillCharacterDefb);
                                }
                            }else{
                                childObjTar.set(detail.getTarget_field(),child.get(detail.getOrigin_field()));
                            }
                        }
                        childTargetValues.add(childObjTar);
                    }
                }
                if(childTargetValues!=null && childTargetValues.size()>0 && main2c != null){
                    ReceiveBillCharacterDefb receiveBillCharacterDefb = new ReceiveBillCharacterDefb();
                    receiveBillCharacterDefb.setId(ymsOidGenerator.nextId());
                    for(MakeBillRuleDetail detail: main2c){
                        for(BizObject child:childTargetValues){
                            if(detail.getTarget_field().contains("receiveBillCharacterDefb.")){
                                if(child.get("receiveBillCharacterDefb") != null){
                                    ((BizObject)child.get("receiveBillCharacterDefb")).set(detail.getTarget_field().replace("receiveBillCharacterDefb.",""),((BizObject)child.get("headCharacter")).get(detail.getOrigin_field().replace("headCharacter.","")));
                                }else{
                                    receiveBillCharacterDefb.set(detail.getTarget_field().replace("receiveBillCharacterDefb.",""),((BizObject)obj.get("headCharacter")).get(detail.getOrigin_field().replace("headCharacter.","")));
                                    child.set("receiveBillCharacterDefb",receiveBillCharacterDefb);
                                }
                            }else{
                                child.set(detail.getTarget_field(),obj.get(detail.getOrigin_field()));
                            }
                        }
                    }
                }
                mainobj.set("ReceiveBill_b",childTargetValues);
                tarList.add(mainobj);
            }
//            });
            return tarList;
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
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101238"),result.getMessage());
        } else {
            return result;
        }
    }

    /**
     * 审核流程
     * @param receiveBill
     * @throws Exception
     */
    public void audit(ReceiveBill receiveBill) throws Exception{
        if (receiveBill == null) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101239"),com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage("P_YS_FI_CM_0000026149") /* "请选择单据！" */ );
        }
//        boolean isWfControlled = ValueUtils.isNotEmptyObj(receiveBill.get(ICmpConstant.IS_WFCONTROLLED)) && receiveBill.getBoolean(ICmpConstant.IS_WFCONTROLLED);
//        receiveBill.set("isWfControlled", isWfControlled);
        List<Journal> journalList = new ArrayList<Journal>();
        receiveBill.setAuditstatus(AuditStatus.Complete);
        receiveBill.setAuditorId(AppContext.getCurrentUser().getId());
        receiveBill.setAuditor(AppContext.getCurrentUser().getName());
        receiveBill.setAuditTime(new Date());
        receiveBill.setAuditDate(BillInfoUtils.getBusinessDate());
        journalList.addAll(journalService.updateJournalByBill(receiveBill));
        receiveBill.setEntityStatus(EntityStatus.Update);
        MetaDaoHelper.update(ReceiveBill.ENTITY_NAME, receiveBill);
        if (!org.apache.commons.collections4.CollectionUtils.isEmpty(journalList)) {
            MetaDaoHelper.update(Journal.ENTITY_NAME, journalList);
        }
    }

    /**
     * 取消审核流程
     * @param receiveBill
     * @throws Exception
     */
    public void unaudit(ReceiveBill receiveBill) throws Exception{
        if (receiveBill == null) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101240"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180003","请选择单据！") /* "请选择单据！" */);
        }
        List<Journal> journalList = new ArrayList<Journal>();
        if(receiveBill.getSettlestatus().getValue() == SettleStatus.alreadySettled.getValue()){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101241"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180004","该单据已结算，不能进行取消审批！") /* "该单据已结算，不能进行取消审批！" */);
        }
//        boolean isWfControlled = ValueUtils.isNotEmptyObj(receiveBill.get(ICmpConstant.IS_WFCONTROLLED)) && receiveBill.getBoolean(ICmpConstant.IS_WFCONTROLLED);
//        receiveBill.set("isWfControlled", isWfControlled);
        receiveBill.setAuditstatus(AuditStatus.Incomplete);
        receiveBill.setAuditorId(null);
        receiveBill.setAuditor(null);
        receiveBill.setAuditTime(null);
        receiveBill.setAuditDate(null);
        journalList.addAll(journalService.updateJournalByBill(receiveBill));
        receiveBill.setEntityStatus(EntityStatus.Update);
        MetaDaoHelper.update(ReceiveBill.ENTITY_NAME, receiveBill);
        if (!org.apache.commons.collections4.CollectionUtils.isEmpty(journalList)) {
            MetaDaoHelper.update(Journal.ENTITY_NAME, journalList);
        }
    }
}
