package com.yonyoucloud.fi.cmp.stwb;

import com.yonyou.iuap.ucf.common.i18n.MessageUtils;
import com.yonyou.iuap.yms.id.generator.YmsOidGenerator;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.cmpentity.*;
import com.yonyoucloud.fi.cmp.common.service.CmCommonService;
import com.yonyoucloud.fi.cmp.constant.IDomainConstant;
import com.yonyoucloud.fi.cmp.journal.Journal;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.Direction;
import com.yonyoucloud.fi.cmp.settlement.Settlement;
import com.yonyoucloud.fi.cmp.settlement.service.SettlementService;
import com.yonyoucloud.fi.cmp.util.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @Date 2021/5/24 20:42
 * @Author wangshbv
 * @Description 登日记账公用工具类
 */
@Service
@Slf4j
public class JournalCommonServiceImpl implements JournalCommonService {

    @Autowired
    CmpWriteBankaccUtils cmpWriteBankaccUtils;

    @Autowired
    private CmCommonService cmCommonService;
    @Autowired
    YmsOidGenerator ymsOidGenerator;
    @Autowired
    private SettlementService settlementService;
    /**
     * 登账的方法  journalType 日记账类型 1：新增， 2：结算变更之后的审核, 3:提交数据后状态直接是已审批, 4:直接是结算成功
     *
     * @param param
     * @throws Exception
     */
    @Override
    public void journalRegisterForStwb(CtmJSONObject param) throws Exception {
        CtmJSONArray data = param.getJSONArray("data");
        //单据来源号
        String srcbillno = param.getString("srcbillno");
        if (StringUtils.isEmpty(srcbillno)) {
            throw new Exception(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054006A1", "srcbillno不能为空！") /* "srcbillno不能为空！" */);
        }
        String type = param.getString("journalType");
        String accentity = param.getString("accentity");
        if (StringUtils.isEmpty(accentity)) {
            throw new Exception(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054006A0", "资金组织不能为空！") /* "资金组织不能为空！" */);
        }
        if (Objects.isNull(type)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102552"),MessageUtils.getMessage("P_YS_FI_CM_0001186847") /* "参数type不能为空！" */);
        }
        if (data != null) {
            //校验是否日结
            boolean flag = checkIsSettleMent(accentity);
            boolean isSubmitFlag = "1".equals(type) || "3".equals(type) || "4".equals(type);
            if (isSubmitFlag && flag) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102553"),MessageUtils.getMessage("P_YS_FI_CM_0001223674") /* "当前会计主体已经日结不能提交数据！" */);
            } else if ("2".equals(type) && flag) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102554"),MessageUtils.getMessage("P_YS_FI_CM_0000026273") /* "该单据已日结，不能修改或删除单据！" */);
            }
            Long timeStamp666 = System.currentTimeMillis();
            log.info("=== ==============现金登账开始进入 =============================================="+timeStamp666);
            List<CtmJSONObject> rowJsonObject = new ArrayList<>();
            List<String> itemBodyIdList = new ArrayList<>();
            List<String> bankAccountList = new ArrayList<>();
            List<String> cashAccountList = new ArrayList<>();
            for (int i = 0,size = data.size(); i < size; i++) {
                rowJsonObject.add(data.getJSONObject(i));
                itemBodyIdList.add(data.getJSONObject(i).get("srcbillitembodyid").toString());
                Object bankaccount = data.getJSONObject(i).get("bankaccount");
                Object cashaccount = data.getJSONObject(i).get("cashaccount");
                if(bankaccount != null){
                    bankAccountList.add(bankaccount.toString());
                }
                if(cashaccount != null){
                    cashAccountList.add(cashaccount.toString());
                }
            }
            //加上判重逻辑，防止重复记账 查询出 本单据的结算明细id
            List<Journal> needRollbackList = getJournalsByItemBodyIdList(accentity, itemBodyIdList);
            //只要有重复数据，先回滚，再重新记账
            if (CollectionUtils.isNotEmpty(needRollbackList)) {
                rollbackInitDataAndJournalSecond(needRollbackList, 2);
            }
            List<Journal> journalList = new ArrayList<>();
            Map<String,Map<String,String>> bankAccountNoMap = null;
            Map<String, String> cashAccountNoMap = null;
            if(CollectionUtils.isNotEmpty(bankAccountList)){
                bankAccountNoMap = getBankAccountNoById(bankAccountList);
            }
            if(CollectionUtils.isNotEmpty(cashAccountList)){
                cashAccountNoMap = getCashAccountNoById(cashAccountList);
            }
            //生成日记账实体
            for(CtmJSONObject jsonObject : rowJsonObject){
                journalList.add(createJounal(jsonObject, accentity, srcbillno, type, bankAccountNoMap, cashAccountNoMap));
            }

            //重新记账
            if (CollectionUtils.isNotEmpty(journalList)) {
                journalList.parallelStream().forEach(journal -> {
                    journal.setEntityStatus(EntityStatus.Insert);
                });
            }
            CmpMetaDaoHelper.insert(Journal.ENTITY_NAME, journalList);
            //更新期初余额
            rollbackInitDataAndJournalSecond(journalList, 1);
            Long timeStamp888 = System.currentTimeMillis();
            log.info("=== ==============现金登账结束，用时 =============================================="+(timeStamp888 -timeStamp666)/1000+"秒");
        }
    }





    private Journal createJounal(CtmJSONObject bizObject, String accentity, String srcBillNo, String type, Map<String,Map<String,String>> bankAccountMap, Map<String, String> cashAccountMap) throws Exception {
        checkJounalData(bizObject);
        Journal journal = new Journal();
        //会计主体 单据日期
        journal.set("accentity", accentity);
        journal.setId(ymsOidGenerator.nextId());
        //Long busiaccbook = FINBDApiUtil.getFI4BDService().getAccBookTypeByAccBody(accentity);
        //Long periodID = FINBDApiUtil.getFI4BDAccPeriodService().getPeriodIDByDate(vouchDateObj, busiaccbook.toString());
        //journal.set("busiaccbook", busiaccbook);//业务账簿
        //journal.set("period", periodID);//会计期间
        Object exchRate = bizObject.get("exchRate");
        Object oriSum = bizObject.get("oriSum");//原币金额
        Object natSum = bizObject.get("natSum");//本币金额
        Object currency = bizObject.get("currency");
        Object srcbillitemId = bizObject.get("srcbillitemid");
        Object srcbillitembodyid = bizObject.get("srcbillitembodyid");
        // 银行账户id,  现金账户.ID
        Object bankaccount = bizObject.get("bankaccount");
        Object cashaccount = bizObject.get("cashaccount");
        journal.setBankaccount(bankaccount == null ? null : bankaccount.toString());
        journal.setCashaccount(cashaccount == null ? null : cashaccount.toString());
        //单据流水号  结算单id 结算单明细的id
        journal.set("srcbillno", srcBillNo);
        journal.set("billnum", srcBillNo);
        journal.setSrcbillitemid(srcbillitemId.toString());
        journal.setSrcbillitemno(srcbillitembodyid.toString());
        if (!Objects.isNull(bankaccount) && !"".equals(bankaccount)) {
            Map accountMap = bankAccountMap.get(bankaccount.toString());
            if(accountMap != null && !accountMap.isEmpty()){
                journal.setBankaccountno(accountMap.get("account").toString());
                journal.setBanktype(accountMap.get("bank").toString());
            }
        }
        if (cashaccount != null && !"".equals(cashaccount)) {
            String cashAccountNo = cashAccountMap.get(cashaccount.toString());
            if(!StringUtils.isEmpty(cashAccountNo)){
                journal.setCashaccountno(cashAccountNo);
            }
        }
        //原币币种 ,本币币种，单据日期, 描述, 事项来源 -- 资金结算, 事项类型-- 资金结算明细
        journal.set("currency", currency);
        journal.set("natCurrency", bizObject.get("natCurrency"));
        journal.set("vouchdate", DateUtils.getCurrentDate("yyyy-MM-dd"));
        journal.set("description", bizObject.get("description"));
        journal.setSrcitem(EventSource.StwbSettlement);
        journal.setBilltype(EventType.StwbSettleMentDetails);
        journal.setTopsrcitem(EventSource.StwbSettlement);
        journal.setTopbilltype(EventType.StwbSettleMentDetails);
        //交易类型 汇率
        journal.setTradetype(bizObject.get("tradetype") == null ? null : bizObject.get("tradetype").toString());
        journal.set("exchangerate", new BigDecimal(exchRate.toString()));
        //结算方式
        journal.setSettlemode(bizObject.get("settlemode") == null ? null : Long.parseLong(bizObject.get("settlemode").toString()));
        journal.set("oribalance", new BigDecimal(oriSum.toString()));//原币余额
        journal.set("natbalance", new BigDecimal(natSum.toString()));//本币余额
        if (bizObject.get("transeqno") != null) {
            journal.set("transeqno", bizObject.get("transeqno"));
        }
        //客户银行账号
        Object customerbankaccount = bizObject.get("customerbankaccount");
        if (customerbankaccount != null && !"".equals(customerbankaccount)) {
            journal.setCustomerbankaccount(Long.parseLong(customerbankaccount.toString()));
        }
        //供应商银行账号
        Object supplierbankaccount = bizObject.get("supplierbankaccount");
        if (supplierbankaccount != null && !"".equals(supplierbankaccount)) {
            journal.setSupplierbankaccount(Long.parseLong(supplierbankaccount.toString()));
        }
        //员工银行账号
        Object employeeaccount = bizObject.get("employeeaccount");
        if (employeeaccount != null && !"".equals(employeeaccount)) {
            journal.setEmployeeaccount(employeeaccount.toString());
        }
        //对方类型
        Object caobject = bizObject.get("caobject");
        journal.setCaobject(CaObject.find(Short.parseShort(caobject.toString())));
        //客户 供应商 员工
        if (bizObject.get("customer") != null && !"".equals(bizObject.get("customer"))) {
            journal.setCustomer(Long.parseLong(bizObject.get("customer").toString()));
        }
        if (bizObject.get("supplier") != null && !"".equals(bizObject.get("supplier"))) {
            journal.setSupplier(Long.parseLong(bizObject.get("supplier").toString()));
        }
        if (bizObject.get("employee") != null && !"".equals(bizObject.get("employee"))) {
            journal.setEmployee(bizObject.get("employee").toString());
        }
        journal.set("checkflag", false);
        journal.set("insidecheckflag", false);
        journal.set("initflag", false);//是否期初 设置为否
        journal.set("paymentstatus", PaymentStatus.NoPay.getValue()); //支付状态枚举  未支付
        journal.setSettlestatus(SettleStatus.noSettlement); //结算状态 -- 未结算
        if("1".equals(type)){
            journal.setAuditstatus(AuditStatus.find(AuditStatus.Incomplete.getValue()));
        }else {
            journal.setAuditstatus(AuditStatus.find(AuditStatus.Complete.getValue()));
            if("4".equals(type)){
                journal.setPaymentstatus(PaymentStatus.PayDone);
                journal.setSettlestatus(SettleStatus.alreadySettled);
                if (ValueUtils.isNotEmptyObj(bizObject.get("settlesuccessdate"))) {
                    Date settlesuccessdate = DateUtils.convertToDate(bizObject.get("settlesuccessdate").toString(), "yyyy-MM-dd");
                    checkIsSettleMent(accentity, settlesuccessdate);
                    journal.setDzdate(settlesuccessdate);
                    journal.setVouchdate(settlesuccessdate);
                } else {
                    journal.setDzdate(DateUtils.getCurrentDate("yyyy-MM-dd"));
                }
            }
        }
        journal.set("refund", false);
        journal.set("bookkeeper", AppContext.getCurrentUser().getId());
        journal.set("createTime", new Date());
        journal.set("createDate", new Date());
        journal.set("creator", AppContext.getCurrentUser().getId());
        journal.setServicecode(bizObject.get("servicecode") == null ? null : bizObject.get("servicecode").toString());
        journal.setBillno(bizObject.getString("billno") == null ? null : bizObject.get("billno").toString());
        //项目，部门
        journal.setProject(bizObject.getString("project"));
        journal.setDept(bizObject.getString("dept"));
        journal.set("define1", bizObject.get("define1"));
        journal.set("define2", bizObject.get("define2"));
        journal.set("define3", bizObject.get("define3"));
        journal.set("define4", bizObject.get("define4"));
        journal.set("define5", bizObject.get("define5"));
        journal.set("define6", bizObject.get("define6"));
        journal.set("define7", bizObject.get("define7"));
        journal.set("define8", bizObject.get("define8"));
        journal.set("define9", bizObject.get("define9"));
        journal.set("define10", bizObject.get("define10"));
        //如果是收款 --》借 ，如果是付款--》贷
        if (Short.valueOf(bizObject.get("receipttypeb").toString()).compareTo(RpType.ReceiveBill.getValue()) == 0) {
            journal.setDirection(Direction.Debit);//借贷方向  借
            journal.set("debitoriSum", new BigDecimal(oriSum.toString()));//借方原币金额
            journal.set("debitnatSum", new BigDecimal(natSum.toString()));//借方本币金额
            journal.set("creditoriSum", BigDecimal.ZERO);//贷方原币金额
            journal.set("creditnatSum", BigDecimal.ZERO);//贷方本币金额
            journal.setRptype(RpType.find(RpType.ReceiveBill.getValue()));// 收付款类型 -- 收款
        } else if (Short.valueOf(bizObject.get("receipttypeb").toString()).compareTo(RpType.PayBill.getValue()) == 0) {
            journal.setDirection(Direction.Credit);//借贷方向  贷
            journal.set("debitoriSum", BigDecimal.ZERO);//借方原币金额
            journal.set("debitnatSum", BigDecimal.ZERO);//借方本币金额
            journal.set("creditoriSum", new BigDecimal(oriSum.toString()));//贷方原币金额
            journal.set("creditnatSum", new BigDecimal(natSum.toString()));//贷方本币金额
            journal.setRptype(RpType.find(RpType.PayBill.getValue()));// 收付款类型 -- 付款
        }
        return journal;
    }
    /**
     * 根据会计主体判断当前会计主体是否日结
     *
     * @param accentity
     * @return
     * @throws Exception
     */
    private boolean checkIsSettleMent(String accentity) throws Exception {
        //已日结后不能修改或删除期初数据
        QuerySchema querySchema = QuerySchema.create().addSelect("1");
        querySchema.addCondition(QueryConditionGroup.and(QueryCondition.name("settleflag").eq(true), QueryCondition.name("settlementdate").eq(DateUtils.getCurrentDate("yyyy-MM-dd"))
                , QueryCondition.name("accentity").eq(accentity)));
        List<Settlement> settlementList = MetaDaoHelper.query(Settlement.ENTITY_NAME, querySchema);
        if (CollectionUtils.isNotEmpty(settlementList)) {
            return true;
        }
        return false;
    }

    /**
     * 校验会计主体是否日结
     * @return
     */
    private Boolean checkIsSettleMent(String accentity,Date dzDate) throws Exception {
        //最大日结日期
        Date maxSettleDate = settlementService.getMaxSettleDate(accentity);
        if(SettleCheckUtil.checkDailySettlementBeforeUnSettle(maxSettleDate, dzDate)){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102555"),MessageUtils.getMessage("P_YS_FI_CM_0001223674") /* "当前会计主体已经日结不能提交数据！" */);
        }
        return true;
    }

    private Map<String,String> getCashAccountNoById(List<String> cashAccountList) throws Exception {
        BillContext billContextFinBank = new BillContext();
        billContextFinBank.setFullname("bd.enterprise.OrgFinCashacctVO");
        billContextFinBank.setDomain(IDomainConstant.MDD_DOMAIN_UCFBASEDOC);
        QueryConditionGroup groupBank = QueryConditionGroup.and(QueryCondition.name("id").in(cashAccountList));
        List<Map<String, Object>> dataList = MetaDaoHelper.queryAll(billContextFinBank, "id,orgid,code,name,account,enable,tenant", groupBank, null);
        if (CollectionUtils.isNotEmpty(dataList)) {
            Map<String,String> accountNoMap = new HashMap<>();
            dataList.stream().forEach(e->{
                if(e.get("code")!=null){
                    accountNoMap.put(e.get("id").toString(), e.get("code").toString());
                }
            });
            return accountNoMap;
        } else {
            throw new Exception(MessageUtils.getMessage("P_YS_FI_CM_0001186875") /* "根据传入的现金账号id，找不到对应的现金账号信息！" */);
        }
    }

    /**
     * 查询银行账号信息
     * @author wangshbv
     * @date 10:37
     */
    private Map<String,Map<String,String>> getBankAccountNoById(List<String> bankaccountList) throws Exception {
        List<Map<String, Object>> dataList = QueryBaseDocUtils.queryEnterpriseBankAccountByIdList(bankaccountList);
        if (CollectionUtils.isNotEmpty(dataList)) {
            Map<String,Map<String,String>> accountNoMap = new HashMap<>();
            dataList.stream().forEach(e->{
                Map<String,String> accountMap = new HashMap<>();
                if(e.get("account")!=null){
                    accountMap.put("account",e.get("account").toString());
                }
                if(e.get("bank")!=null){
                    accountMap.put("bank",e.get("bank").toString());
                }
                if(accountMap != null && !accountMap.isEmpty()){
                    accountNoMap.put(e.get("id").toString(), accountMap);
                }
            });
            return accountNoMap;
        } else {
            throw new Exception(MessageUtils.getMessage("P_YS_FI_CM_0001186858") /* "根据传入的银行账号id，找不到对应的银行账号信息！" */);
        }
    }
    /**
     * 校验数据
     * @author wangshbv
     * @date 10:37
     */
    private void checkJounalData(CtmJSONObject bizObject) throws Exception {
        Object bankaccount = bizObject.get("bankaccount");
        Object cashaccount = bizObject.get("cashaccount");
        boolean accountFlag = (bankaccount == null || "".equals(bankaccount)) && (cashaccount == null || "".equals(cashaccount));
        if (accountFlag) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102556"),MessageUtils.getMessage("P_YS_FI_CM_0001215792") /* "银行账户或现金账户必须有一个不为空！" */);
        }
        Object exchRate = bizObject.get("exchRate");
        Object oriSum = bizObject.get("oriSum");//原币金额
        Object natSum = bizObject.get("natSum");//本币金额
        Object currency = bizObject.get("currency");
        Object srcbillitemId = bizObject.get("srcbillitemid");
        Object srcbillitembodyid = bizObject.get("srcbillitembodyid");
        Object caobject = bizObject.get("caobject");
        if (exchRate == null) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102557"),MessageUtils.getMessage("P_YS_FI_CM_0001123668") /* "汇率不能为空！" */);
        }
        if (oriSum == null) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102558"),MessageUtils.getMessage("P_YS_FI_CM_0001123673") /* "原币金额不能为空！" */);
        }
        if (natSum == null) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102559"),MessageUtils.getMessage("P_YS_FI_CM_0001123674") /* "本币金额不能为空！" */);
        }
        if (currency == null) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102560"),MessageUtils.getMessage("P_YS_FI_CM_0000026018") /* "币种不能为空！" */);
        }
        if (srcbillitemId == null) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102561"),MessageUtils.getMessage("P_YS_FI_CM_0001218810") /* "srcbillitemId不能为空！" */);
        }
        if (srcbillitembodyid == null) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102562"),MessageUtils.getMessage("P_YS_FI_CM_0001215788") /* "srcbillitembodyid不能为空！" */);
        }
        if (caobject == null) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102563"),MessageUtils.getMessage("P_YS_FI_CM_0001218811") /* "对方类型不能为空！" */);
        }

    }

    /**
     * 现金管理单据审核接口 type : 1：审核 2：驳回， 3：日记账成功 4：弃审,5 止付
     * 审核,驳回,弃审 都是整条单据操作的；日记账成功，止付是针对某一条单据操作的
     * accentity ：资金组织
     * srcbillno ： 单据来源号
     * srcbillitemid ： 结算单明细的id
     */
    @Override
    public void journalApproveForStwb(CtmJSONObject param) throws Exception {
        String type = param.getString("type");
        String accentity = param.getString("accentity");
        CtmJSONArray srcbillitembodyidArray = param.getJSONArray("srcbillitembodyid");
        if (Objects.isNull(type)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102564"),MessageUtils.getMessage("P_YS_FI_CM_0001186847") /* "参数type不能为空！" */);
        }
        if (srcbillitembodyidArray == null || srcbillitembodyidArray.size() < 0) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102565"),MessageUtils.getMessage("P_YS_FI_CM_0001215791") /* "单据明细id不能为空！" */);
        }
        //校验一下是否发生日结，结算成功时校验日结，其他操作不校验
        if("3".equals(type)){
            checkApproveSettlement(type, accentity);
        }
        //改写账户期初余额
        List<Journal> updateList = new ArrayList<>();
        //根据子表id 即：srcbillitemno 获取对应的日记账数据
        List<Map<String, String>> srcBillItemIdAndBankCheckNoList = getSrcbillitemidFromParam(srcbillitembodyidArray);
        List<String> srcBillItemIdList = new ArrayList<>();
        for (Map<String, String> stringStringMap : srcBillItemIdAndBankCheckNoList) {
            srcBillItemIdList.add(stringStringMap.get("id"));
        }
        List<Journal> journalList = getJournalsByItemBodyIdList(accentity, srcBillItemIdList);
        for (Journal journal : journalList) {
            String id = journal.getSrcbillitemno();
            for (Map<String, String> stringMap : srcBillItemIdAndBankCheckNoList) {
                if (id.equals(stringMap.get("id"))) {
                    journal.setBankcheckno(stringMap.get("bankcheckno"));
                    break;
                }
            }
        }
        if (CollectionUtils.isNotEmpty(journalList)) {
            if ("1".equals(type)) {
                //审核通过
                updateList = setJournalInfoByType(journalList, AuditStatus.Incomplete, AuditStatus.Complete);
            } else if ("2".equals(type) || "5".equals(type)) {
                //将数据放缓存中
                String requestId = param.getString("requestId");
                Map<String, List<Journal>> journalListMap = journalList.stream().collect(Collectors.groupingBy(Journal::getSrcbillitemno));
                AppContext.cache().setObject("yonbip-fi-ctmcmp-journalApproveForStwb" + requestId, journalListMap);
                //驳回   回滚期初余额，删除记账数据  根据结算单号删除数据
                rollbackInitDataAndJournalSecond(journalList,2);
            } else if("3".equals(type)){
                //日记账成功  结算状态改为成功
                updateList = settleSuccess(journalList);
            }else if ("4".equals(type)) {
                //弃审
                updateList = setJournalInfoByType(journalList, AuditStatus.Complete, AuditStatus.Incomplete);
            }

        } else {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102566"),MessageUtils.getMessage("P_YS_FI_CM_0001257235") /* "当前结算明细:" */+srcBillItemIdList.toString()+MessageUtils.getMessage("P_YS_FI_CM_0001257236") /* "对应的日记账数据为空!" */);
        }
        if (CollectionUtils.isNotEmpty(updateList)) {
            EntityTool.setUpdateStatus(updateList);
            MetaDaoHelper.update(Journal.ENTITY_NAME, updateList);
        }
    }

    /**
     * 校验是否发生日结
     *
     * @param type
     * @param accentity
     * @throws Exception
     */
    private void checkApproveSettlement(String type, String accentity) throws Exception {
        boolean settleMentFlag = checkIsSettleMent(accentity);
        if (settleMentFlag) {
            switch (type) {
                case "1":
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102567"),MessageUtils.getMessage("P_YS_FI_CM_0001224632") /* "当前资金组织已经日结，不能进行审核操作！" */);
                case "2":
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102568"),MessageUtils.getMessage("P_YS_FI_CM_0001224633") /* "当前资金组织已经日结，不能进行驳回操作！" */);
                case "3":
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102569"),MessageUtils.getMessage("P_YS_FI_CM_0001264920") /* "当前资金组织已经日结，不能进行该操作！" */);
                case "4":
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102570"),MessageUtils.getMessage("P_YS_FI_CM_0001223665") /* "本结算单包含的结算明细，已登记日记账，且已经日结，不支持弃审！" */);
                case "5":
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102571"),MessageUtils.getMessage("P_YS_FI_CM_0001223669") /* "当前资金组织已经日结，不能进行止付操作！" */);
                default:
                    break;
            }
        }
    }

    private List<Map<String, String>> getSrcbillitemidFromParam(CtmJSONArray srcbillitemidArray) {
        List<Map<String,String>> resultList = new ArrayList<>();
        for (int i = 0, size = srcbillitemidArray.size(); i < size; i++) {
            Map map = new HashMap<String,String>();
            map.put("id",srcbillitemidArray.getJSONObject(i).get("id").toString());
            if(srcbillitemidArray.getJSONObject(i).get("bankcheckno") != null) {
                map.put("bankcheckno",srcbillitemidArray.getJSONObject(i).get("bankcheckno").toString());
            }
            resultList.add(map);
        }
        return resultList;
    }

    /**
     * type :1 正向更新期初余额，2：回滚期初余额并且删除日记账
     * 回滚期初余额 并删除日记账
     * @param journalList
     * @throws Exception
     */
    @Override
    public void rollbackInitDataAndJournalSecond(List<Journal> journalList, Integer type) throws Exception {
        if(CollectionUtils.isNotEmpty(journalList)) {
            for (Journal journal : journalList) {
                if (type == 2) {
                    cmpWriteBankaccUtils.delAccountBookByJournal(journal);
                } else {
                    cmpWriteBankaccUtils.addAccountBookSTWB(journal);
                }
            }
        }
    }

    /**
     * 根据结算单明细id 查询日记账信息
     * @author wangshbv
     * @date 15:15
     */
    @Override
    public List<Journal> getJournalsByItemBodyIdList(String accentity, List<String> srcItemBodyIdList) throws Exception {
        QuerySchema querySchema = QuerySchema.create().addSelect("*");
        QueryConditionGroup conditionGroup = new QueryConditionGroup();
        conditionGroup.addCondition(QueryConditionGroup.and(QueryCondition.name("accentity").eq(accentity)));
        conditionGroup.addCondition(QueryConditionGroup.and(QueryCondition.name("srcbillitemno").in(srcItemBodyIdList)));
        //事项来源是资金结算  事项类型是资金结算明细的数据
        conditionGroup.addCondition(QueryConditionGroup.and(QueryCondition.name("srcitem").eq(EventSource.StwbSettlement.getValue())));
        conditionGroup.addCondition(QueryConditionGroup.and(QueryCondition.name("billtype").eq(EventType.StwbSettleMentDetails.getValue())));
        querySchema.addCondition(conditionGroup);
        return MetaDaoHelper.queryObject(Journal.ENTITY_NAME, querySchema, null);
    }

    @NotNull
    @Override
    public List<Journal> setJournalInfoByType(List<Journal> journalList, AuditStatus incomplete, AuditStatus complete) {
        List<Journal> updateList = journalList.stream()
                .filter(e -> e.getAuditstatus().getValue() == incomplete.getValue())
                .collect(Collectors.toList());
        updateList.stream().forEach(entity -> {
            entity.setAuditstatus(complete);
        });
        return updateList;
    }
    @NotNull
    @Override
    public List<Journal> settleSuccess(List<Journal> journalList) {
        List<Journal>  updateList = journalList.stream()
                .filter(e -> e.getSettlestatus().getValue() == SettleStatus.noSettlement.getValue())
                .collect(Collectors.toList());
        updateList.stream().forEach(entity -> {
            entity.setSettlestatus(SettleStatus.alreadySettled);
            entity.setDzdate(DateUtils.getCurrentDate("yyyy-MM-dd"));
        });
        return updateList;
    }
    @NotNull
    @Override
    public List<Journal> settleSuccessCancel(List<Journal> journalList) {
        List<Journal> updateList = journalList.stream()
                .filter(e -> e.getSettlestatus().getValue() == SettleStatus.alreadySettled.getValue())
                .collect(Collectors.toList());
        updateList.stream().forEach(entity ->{
            entity.setSettlestatus(SettleStatus.noSettlement);
            entity.setDzdate(null);
        });
        return updateList;
    }
    /**
     * 驳回，止付的时候 回滚方法
     * @author wangshbv
     * @date 15:23
     */
    @Override
    public void rollbackJournalDataFromRedis(List<String> srcBillItemIdList, String requestId) throws Exception {
        Map<String, List<Journal>> journalMap = AppContext.cache().getObject("yonbip-fi-ctmcmp-journalApproveForStwb" + requestId);
        //从redist取出数据
        if (journalMap != null && journalMap.size() > 0) {
            List<Journal> rollbackJournalList = new ArrayList<>();
            for (String entity : srcBillItemIdList) {
                if (journalMap.get(entity) != null) {
                    rollbackJournalList.addAll(journalMap.get(entity));
                }
            }
            if (CollectionUtils.isNotEmpty(rollbackJournalList)) {
                rollbackJournalList.parallelStream().forEach(journal ->{
                    journal.setEntityStatus(EntityStatus.Insert);
                });
                //更新期初余额
                rollbackInitDataAndJournalSecond(rollbackJournalList, 1);
                //重新记账
                CmpMetaDaoHelper.insert(Journal.ENTITY_NAME, rollbackJournalList);
            }
            //从redis中删除数据
            AppContext.cache().del("yonbip-fi-ctmcmp-journalApproveForStwb" + requestId);
        } else {
            throw new Exception(MessageUtils.getMessage("P_YS_FI_CM_0001252246") /* "redis 中查询数据出现异常,导致回滚数据异常！" */);
        }
    }

}
