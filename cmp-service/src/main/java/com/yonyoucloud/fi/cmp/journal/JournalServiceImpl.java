package com.yonyoucloud.fi.cmp.journal;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVO;
import com.yonyou.ucf.mdd.ext.bill.rule.util.BillInfoUtils;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.cmp.cmpentity.*;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.common.service.CmCommonService;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.Direction;
import com.yonyoucloud.fi.cmp.util.CmpMetaDaoHelper;
import com.yonyoucloud.fi.cmp.util.CmpWriteBankaccUtils;
import com.yonyoucloud.fi.cmp.util.DateUtils;
import com.yonyoucloud.fi.cmp.util.EntityTool;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import lombok.NonNull;
import org.imeta.core.lang.BooleanUtils;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Administrator on 2019/4/20 0020.
 */
@Service
@Slf4j
public class JournalServiceImpl implements JournalService {

    private static final Logger logger = LoggerFactory.getLogger(JournalServiceImpl.class);
    @Autowired
    private CmCommonService cmCommonService;
    @Autowired
    CmpWriteBankaccUtils cmpWriteBankaccUtils;
    @Autowired
    BaseRefRpcService baseRefRpcService;

    private static final String INITDATAMAPPER = "com.yonyoucloud.fi.cmp.mapper.InitDataMapper.updateInitDataAccount";

    private static final @NonNull Cache<String, String> parentAccentityCache = Caffeine.newBuilder()
            .initialCapacity(100)
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(1))
            .softValues()
            .build();

    @Override
    public CtmJSONObject generateJournal(List<Journal> journalList) throws Exception {
        CtmJSONObject jSONObject = new CtmJSONObject();
        try {
            CmpMetaDaoHelper.insert(Journal.ENTITY_NAME,journalList);
            jSONObject.put("code","1");
            jSONObject.put("message","");
        } catch (Exception e) {
            log.error("InitDataServiceImpl.generateJournal", e);
            jSONObject.put("code", "0");
            jSONObject.put("message", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418010E","生成日记账出错") /* "生成日记账出错" */);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101817"),e.getMessage());
        }
        return jSONObject;
    }

    @Override
    public void updateJournal(BizObject bizObject) throws Exception {
        QuerySchema queryJournalSchema = QuerySchema.create().addSelect("id,pubts,dzdate,auditstatus,settlestatus,bankaccount,cashaccount,currency,debitoriSum,debitnatSum,creditoriSum,creditnatSum,direction,accentity,billtype,billno");
        queryJournalSchema.appendQueryCondition(QueryCondition.name("srcbillitemid").eq(bizObject.getId().toString()));
        List<Journal> journals = MetaDaoHelper.queryObject(Journal.ENTITY_NAME, queryJournalSchema, null);
        Date currentDate = new Date();
        for (Journal journal : journals) {
            //财资统一对账码改造
            //同名账户划转
            if ("cm_transfer_account".equals(journal.getBillno())) {
                //财资统一对账码（收款）传递，日记账的借方要记银行方向的贷。
                if (Direction.Debit.equals(journal.getDirection())) {
                    journal.setBankcheckno(bizObject.get("smartcheckno"));
                }
                //财资统一对账码（付款）传递，日记账贷方向要记银行方的借
                if (Direction.Credit.equals(journal.getDirection())) {
                    journal.setBankcheckno(bizObject.get("paysmartcheckno"));
                }
            } else if (IBillNumConstant.CURRENCYEXCHANGE.equals(journal.getBillno())){
                //财资统一对账码（收款）传递，日记账的借方要记银行方向的贷。
                if (Direction.Debit.equals(journal.getDirection())) {
                    journal.setBankcheckno(bizObject.get("buysmartcheckno"));
                }
                //财资统一对账码（付款）传递，日记账贷方向要记银行方的借
                if (Direction.Credit.equals(journal.getDirection())) {
                    journal.setBankcheckno(bizObject.get("sellsmartcheckno"));
                }
            }else {
                journal.setBankcheckno(bizObject.get("bankcheckno"));
            }
            if ("cmp_salarypay".equals(journal.getBillno()) && bizObject.get("successmoney") != null && bizObject.get("olcsuccessmoney") != null) {
                journal.setCreditoriSum(bizObject.get("successmoney"));
                journal.setCreditnatSum(bizObject.get("olcsuccessmoney"));
            }
            if (bizObject.get("settlestatus") != null && (SettleStatus.alreadySettled.getValue() == bizObject.getShort("settlestatus") || DeliveryStatus.alreadyDelivery.getValue() == bizObject.getShort("settlestatus")
            || FundSettleStatus.SettleSuccess.getValue() == bizObject.getShort("settlestatus")
            //20240812-yangjn 转账单 不传结算 已经支付补单状态应为结算成功 故而这里校验支付状态
            || (bizObject.get("paystatus") != null && PayStatus.OfflinePay.getValue() == bizObject.getShort("paystatus"))
                    //银行对账单生成的转账单，结算状态为已结算补单，不推送结算的情况下需要自己记账
            || SettleStatus.SettledRep.getValue() == bizObject.getShort("settlestatus"))) {
				if (bizObject.get("dzdate") != null) {
					journal.setDzdate(bizObject.get("dzdate"));
                    journal.setDztime(bizObject.get("dzdate"));
				} else {
					if (BillInfoUtils.getBusinessDate() != null) {
						journal.setDzdate(BillInfoUtils.getBusinessDate());
                        journal.setDztime(DateUtils.setTimeToCurrent(BillInfoUtils.getBusinessDate()));
					} else {
						journal.setDzdate(currentDate);
                        journal.setDztime(currentDate);
					}
				}
                // 转账单线下支付或者生单过来审批通过的时候 登账日期需要取结算日期
                if ("cm_transfer_account".equals(journal.getBillno()) && bizObject.get("settledate") != null) {
                    journal.setDzdate(bizObject.get("settledate"));
                    journal.setDztime(DateUtils.setTimeToCurrent(bizObject.get("settledate")));
                }
				journal.setSettlestatus(SettleStatus.alreadySettled);
                if ("cmp_salarypay".equals(journal.getBillno()) && bizObject.get("isdirectconn") != null && ((Boolean) bizObject.get("isdirectconn") == true) && bizObject.get("paydate") != null) {
                    journal.setDzdate(bizObject.get("paydate"));
                    journal.setDztime(DateUtils.setTimeToCurrent(bizObject.get("paydate")));
                }
            } else {
                journal.setVoucherNo(null);
                journal.setVoucherPeriod(null);
                journal.setDzdate(null);
                journal.setDztime(null);
                journal.setSettlestatus(SettleStatus.noSettlement);
            }
            if(bizObject.getShort("auditstatus") != null) {
            	if(AuditStatus.Complete.getValue() == bizObject.getShort("auditstatus")) {
                	journal.setAuditstatus(AuditStatus.Complete);
                }else {
                	journal.setAuditstatus(AuditStatus.Incomplete);
                }
            }
            //如果当前单据结算成功 则登记实占帐
//            if(journal.getSettlestatus().getValue() == SettleStatus.alreadySettled.getValue()){
//                String accid = CmpWriteBankaccUtils.getAccId(journal);
//                InitData initdata = CmpWriteBankaccUtils.getInitDataByAccid(journal.getAccentity(),accid, journal.getCurrency());
//                Map<String, Object> map = new HashMap<>(8);
//                map.put("id", initdata.getId());
//                map.put("ytenant_id", InvocationInfoProxy.getTenantid());
//                if (Direction.Debit.getValue() == journal.getDirection().getValue()) {
//                    map.put(IBussinessConstant.ORISETTLED_SUM, journal.getDebitoriSum());
//                    map.put(IBussinessConstant.NATSETTLED_SUM, journal.getDebitnatSum());
//                } else {
//                    map.put(IBussinessConstant.ORISETTLED_SUM, BigDecimalUtils.safeSubtract(BigDecimal.ZERO, journal.getCreditoriSum()));
//                    map.put(IBussinessConstant.NATSETTLED_SUM, BigDecimalUtils.safeSubtract(BigDecimal.ZERO, journal.getCreditnatSum()));
//                }
//                SqlHelper.update(INITDATAMAPPER, map);
//
//            }
        }
        EntityTool.setUpdateStatus(journals);
        MetaDaoHelper.update(Journal.ENTITY_NAME, journals);
    }

    @Override
    public void updateJournalThird(BizObject bizObject,boolean isAuthit) throws Exception {
        QuerySchema queryJournalSchema = QuerySchema.create().addSelect("id,pubts,dzdate,auditstatus,settlestatus");
        queryJournalSchema.appendQueryCondition(QueryCondition.name("srcbillitemid").eq(bizObject.getId().toString()));
        List<Journal> journals = MetaDaoHelper.queryObject(Journal.ENTITY_NAME, queryJournalSchema, null);
        Date currentDate = new Date();
        for (Journal journal : journals) {
            journal.setBankcheckno(bizObject.get("bankcheckno"));
            if (isAuthit) {
				if (bizObject.get("dzdate") != null) {
					journal.setDzdate(bizObject.get("dzdate"));
				} else {
					if (BillInfoUtils.getBusinessDate() != null) {
						journal.setDzdate(BillInfoUtils.getBusinessDate());
					} else {
						journal.setDzdate(currentDate);
					}
				}
				journal.setSettlestatus(SettleStatus.alreadySettled);
            } else {
                journal.setVoucherNo(null);
                journal.setVoucherPeriod(null);
                journal.setDzdate(null);
                journal.setDztime(null);
                journal.setSettlestatus(SettleStatus.noSettlement);
            }
            if(bizObject.getShort("auditstatus") != null) {
            	if(AuditStatus.Complete.getValue() == bizObject.getShort("auditstatus")) {
                	journal.setAuditstatus(AuditStatus.Complete);
                }else {
                	journal.setAuditstatus(AuditStatus.Incomplete);
                }
            }
        }
        EntityTool.setUpdateStatus(journals);
        MetaDaoHelper.update(Journal.ENTITY_NAME, journals);
    }

    @Override
    public void updateJournalForExchangeCurrency(BizObject bizObject) throws Exception {
        QuerySchema queryJournalSchema = QuerySchema.create().addSelect("id,pubts,dzdate,auditstatus,settlestatus,direction");
        queryJournalSchema.appendQueryCondition(QueryCondition.name("srcbillitemid").eq(bizObject.getId().toString()));
        List<Journal> journals = MetaDaoHelper.queryObject(Journal.ENTITY_NAME, queryJournalSchema, null);
        Date currentDate = new Date();
        for (Journal journal : journals) {
            journal.setBankcheckno(bizObject.get("bankcheckno"));
            if (bizObject.get("settlestatus") != null && (SettleStatus.alreadySettled.getValue() == bizObject.getShort("settlestatus") || DeliveryStatus.alreadyDelivery.getValue() == bizObject.getShort("settlestatus"))) {
                if (bizObject.get("dzdate") != null) {
                    journal.setDzdate(bizObject.get("dzdate"));
                    journal.setDztime(DateUtils.setTimeToCurrent(bizObject.get("dzdate")));
                } else {
                    if (BillInfoUtils.getBusinessDate() != null) {
                        journal.setDzdate(BillInfoUtils.getBusinessDate());
                        journal.setDztime(DateUtils.setTimeToCurrent(BillInfoUtils.getBusinessDate()));
                    } else {
                        journal.setDzdate(currentDate);
                        journal.setDztime(DateUtils.setTimeToCurrent(currentDate));
                    }
                }
                journal.setSettlestatus(SettleStatus.alreadySettled);
                // 外币兑换直连类型，涉及到更新日记账金额跟汇率
                if (DeliveryStatus.alreadyDelivery.getValue() == bizObject.getShort("settlestatus")) {
                    journal.setExchangerate(bizObject.get("exchangerate"));
                    // 买入金额为借方金额；卖出金额为贷方金额
                    if (Direction.Debit.equals(journal.getDirection()) && bizObject.get("sellCurrency").equals(journal.getCurrency())) {
                        journal.setCreditoriSum(bizObject.get("sellamount"));
                        journal.setCreditnatSum(bizObject.get("sellloaclamount"));
                    } else if(bizObject.get("sellCurrency").equals(journal.getCurrency())){
                        journal.setDebitoriSum(bizObject.get("purchaseamount"));
                        journal.setDebitnatSum(bizObject.get("purchaselocalamount"));
                    }
                }
            } else {
                journal.setVoucherNo(null);
                journal.setVoucherPeriod(null);
                journal.setDzdate(null);
                journal.setDztime(null);
                journal.setSettlestatus(SettleStatus.noSettlement);
            }
            if(bizObject.getShort("auditstatus") != null) {
                if(AuditStatus.Complete.getValue() == bizObject.getShort("auditstatus")) {
                    journal.setAuditstatus(AuditStatus.Complete);
                }else {
                    journal.setAuditstatus(AuditStatus.Incomplete);
                }
            }
        }
        EntityTool.setUpdateStatus(journals);
        MetaDaoHelper.update(Journal.ENTITY_NAME, journals);
    }

    @Override
    public void updateJournal(List<BizObject> bizObjectList) throws Exception {
        if (CollectionUtils.isEmpty(bizObjectList)){
            return;
        }
        Map<String, BizObject> idBizObjects = new HashMap<>();
        for (BizObject bizObject : bizObjectList){
            idBizObjects.put(bizObject.getId().toString(), bizObject);
        }
        QuerySchema queryJournalSchema = QuerySchema.create().addSelect("id,pubts,dzdate,auditstatus,settlestatus,srcbillitemid");
        queryJournalSchema.appendQueryCondition(QueryCondition.name("srcbillitemid").in(idBizObjects.keySet()));
        List<Journal> journals = MetaDaoHelper.queryObject(Journal.ENTITY_NAME, queryJournalSchema, null);

        Date currentDate = new Date();
        for (Journal journal : journals) {
            BizObject bizObject = idBizObjects.get(journal.getSrcbillitemid());
            //外币兑换工作台，结算状态为已交割，也更新日记账为已结算
            if (bizObject.get("settlestatus") != null && (SettleStatus.alreadySettled.getValue() == bizObject.getShort("settlestatus")
                    || DeliveryStatus.alreadyDelivery.getValue() == bizObject.getShort("settlestatus"))) {
                if (bizObject.get("dzdate") != null) {
                    journal.setDzdate(bizObject.get("dzdate"));
                } else {
                    if (BillInfoUtils.getBusinessDate() != null) {
                        journal.setDzdate(BillInfoUtils.getBusinessDate());
                    } else {
                        journal.setDzdate(currentDate);
                    }
                }
                journal.setSettlestatus(SettleStatus.alreadySettled);
            } else {
                journal.setVoucherNo(null);
                journal.setVoucherPeriod(null);
                journal.setDzdate(null);
                journal.setSettlestatus(SettleStatus.noSettlement);
            }
            if(bizObject.getShort("auditstatus") != null) {
            	if(AuditStatus.Complete.getValue() == bizObject.getShort("auditstatus")) {
                	journal.setAuditstatus(AuditStatus.Complete);
                }else {
                	journal.setAuditstatus(AuditStatus.Incomplete);
                }
            }
        }
        EntityTool.setUpdateStatus(journals);
        MetaDaoHelper.update(Journal.ENTITY_NAME, journals);
    }

    @Override
    public List<Journal> updateJournalByBill(BizObject bizObject) throws Exception {
        QuerySchema queryJournalSchema = QuerySchema.create().addSelect("id,pubts,dzdate,auditstatus,settlestatus");
        queryJournalSchema.appendQueryCondition(QueryCondition.name("srcbillitemid").eq(bizObject.getId().toString()));
        List<Journal> journals = MetaDaoHelper.queryObject(Journal.ENTITY_NAME, queryJournalSchema, null);
        Date currentDate = new Date();
        for (Journal journal : journals) {
        	/** begin 去掉触发器修改,有结算状态和审核状态的进行更新  */
        	if(bizObject.getShort("settlestatus") != null) {
        		if (SettleStatus.alreadySettled.getValue() == bizObject.getShort("settlestatus")) {
        			if (bizObject.get("dzdate") != null){
        				journal.setDzdate(bizObject.get("dzdate"));
        			}else {
        				if(BillInfoUtils.getBusinessDate() != null) {
        					journal.setDzdate(BillInfoUtils.getBusinessDate());
        				}else {
        					journal.setDzdate(currentDate);
        				}
        			}
        			journal.setSettlestatus(SettleStatus.alreadySettled);
        		} else {
        			journal.setDzdate(null);
                    journal.setDztime(null);
                    journal.setVoucherNo(null);
                    journal.setVoucherPeriod(null);
        			journal.setSettlestatus(SettleStatus.noSettlement);
        		}
        	}
            if(bizObject.getShort("auditstatus") != null) {
            	if(AuditStatus.Complete.getValue() == bizObject.getShort("auditstatus")) {
            		journal.setAuditstatus(AuditStatus.Complete);
            	}else {
            		journal.setAuditstatus(AuditStatus.Incomplete);
            	}
            }
            /** end 去掉触发器修改  */
        }
        EntityTool.setUpdateStatus(journals);
        return  journals;
    }

    /**
     * 校验日记账与对账单是否已勾兑
     * @param id
     * @return
     * @throws Exception
     */
    @Override
    public Boolean checkJournal(Long id) throws Exception {
        if (null == id){
            return false;
        }
        QuerySchema querySchemaJ = QuerySchema.create().addSelect("1");
        querySchemaJ.addCondition(QueryConditionGroup.and(QueryCondition.name("srcbillitemid").eq(id.toString()), QueryCondition.name("checkflag").eq(true)));
        List<Journal> journalList = MetaDaoHelper.query(Journal.ENTITY_NAME, querySchemaJ);
        if (journalList.size() > 0) {
            return true;
        }
        return false;
    }

    @Override
    public void compute4Save(BizObject bizObject,Journal journal,Direction direction) throws Exception {
        //回退逻辑
        CmpWriteBankaccUtils.delAccountBook(bizObject.get("id").toString());
        //重新登账
        cmpWriteBankaccUtils.addAccountBook(journal);
    }

    /**
     * 校验日记账与回单是否已匹配
     * @param id
     * @return
     * @throws Exception
     */
    @Override
    public Boolean matchJournal(Long id) throws Exception {
        if (null == id){
            return false;
        }
        QuerySchema querySchemaJ = QuerySchema.create().addSelect("1");
        querySchemaJ.addCondition(QueryConditionGroup.and(QueryCondition.name("srcbillitemid").eq(id.toString()), QueryCondition.name("checkmatch").eq(1)));
        List<Journal> journalList = MetaDaoHelper.query(Journal.ENTITY_NAME, querySchemaJ);
        if (journalList.size() > 0) {
            return true;
        }
        return false;
    }

    @Override
    public Map<String, Boolean> isJournalCheckOrMatch(Long id) throws Exception {
        Map<String, Boolean> result = new HashMap<String, Boolean>();
        if (null == id){
            result.put(ICmpConstant.CHECHMATCH, false);
            result.put(ICmpConstant.CHECHFLAG, false);
            return result;
        }
        QuerySchema querySchemaJ = QuerySchema.create().addSelect("checkmatch,checkflag");
        querySchemaJ.addCondition(QueryConditionGroup.and(QueryCondition.name("srcbillitemid").eq(id.toString())));
        Map<String, Object> journalMap = MetaDaoHelper.queryOne(Journal.ENTITY_NAME, querySchemaJ);
        result.put(ICmpConstant.CHECHMATCH, journalMap.get(ICmpConstant.CHECHMATCH) ==null ? false : BooleanUtils.b(journalMap.get(ICmpConstant.CHECHMATCH)));
        result.put(ICmpConstant.CHECHFLAG, journalMap.get(ICmpConstant.CHECHFLAG) ==null ? false : BooleanUtils.b(journalMap.get(ICmpConstant.CHECHFLAG)));
        return result;
    }

    /**
     * 设置对方名称
     * @param journal 日记账实体
     * @throws Exception
     */
    @Override
    public void addOthertitle(Journal journal) throws Exception {
        if(journal.getOthertitle() == null){
            SetOtherMethod method = new SetOtherMethod(journal);
        }
    }

    @Override
    public void setParentAccentityForJournal(Journal journal) throws Exception {
        if(journal.getBankaccount()!=null){
            String parentAccentity = parentAccentityCache.getIfPresent(journal.getBankaccount());
            if(parentAccentity == null){
                EnterpriseBankAcctVO accVo = baseRefRpcService.queryEnterpriseBankAccountById(journal.getBankaccount());
                if(accVo!=null){
                    parentAccentity = accVo.getOrgid();
                    parentAccentityCache.put(journal.getBankaccount(),parentAccentity);
                }
            }
            journal.setParentAccentity(parentAccentity);
        }else
            journal.setParentAccentity(journal.getAccentity());
    }

    /**
     * 更新日记账审批状态
     * @param srcbillitemid 单据明细id
     * @param auditStatus   审批状态
     * @throws Exception
     */
    @Override
    public void updateAuditStatusOfJournal(Object srcbillitemid, AuditStatus auditStatus) throws Exception {
        QuerySchema queryJournalSchema = QuerySchema.create().addSelect("id,pubts,auditstatus,srcbillno");
        queryJournalSchema.appendQueryCondition(QueryCondition.name("srcbillitemid").eq(srcbillitemid instanceof Long ? srcbillitemid.toString() : srcbillitemid));
        List<Journal> journals = MetaDaoHelper.queryObject(Journal.ENTITY_NAME, queryJournalSchema, null);
        if (CollectionUtils.isEmpty(journals)) {
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400563", "日记账数据错误，请检查!") /* "日记账数据错误，请检查!" */);
        }
        // 转账单不传结算会登记一收一支的日记账
        for (Journal journal : journals) {
            journal.setAuditstatus(auditStatus);
            journal.setEntityStatus(EntityStatus.Update);
        }
        MetaDaoHelper.update(Journal.ENTITY_NAME, journals);
    }
}
