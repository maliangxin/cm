package com.yonyoucloud.fi.cmp.bill;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.yht.sdkutils.StringUtils;
import com.yonyou.yonbip.ctm.remote.RemoteDubbo;
import com.yonyoucloud.ctm.stwb.reconcode.pubitf.ReconciliateCodeGenerator;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliationbusrelation_b;
import com.yonyoucloud.fi.cmp.bankreconciliation.CommonSaveUtils;
import com.yonyoucloud.fi.cmp.billclaim.BillClaim;
import com.yonyoucloud.fi.cmp.billclaim.BillClaimItem;
import com.yonyoucloud.fi.cmp.cmpentity.*;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.IDomainConstant;
import com.yonyoucloud.fi.cmp.currencyexchange.CurrencyExchange;
import com.yonyoucloud.fi.cmp.util.CmpWriteBankaccUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 外币兑换删除记录时更新和删除日记帐信息
 */
@Component
public class DeleteCurrencyRuleToCmp extends AbstractCommonRule {

	@Override
	public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
		BillDataDto bill = (BillDataDto)map.get("param");
		List<BizObject> bills = getBills(billContext, map);
		if (bills == null || bills.size() == 0) {
			return new RuleExecuteResult();
		}
		String billNum = billContext.getBillnum();
		if (StringUtils.isBlank(billNum)) {
			return new RuleExecuteResult();
		}
		BizObject bizObject = (BizObject) bills.get(0);
		Long id = bizObject.getId();
		bizObject = MetaDaoHelper.findById(CurrencyExchange.ENTITY_NAME,id);
		// begin 日结逻辑控制调整 majfd 21/06/07
		//已日结后不能修改或删除期初数据
//		QuerySchema querySchema = QuerySchema.create().addSelect("1");
//		querySchema.addCondition(QueryConditionGroup.and(QueryCondition.name("settleflag").eq(true),QueryCondition.name("settlementdate").eq(bizObject.get("vouchdate"))
//				,QueryCondition.name("accentity").eq(bizObject.get("accentity"))));
//		List<Settlement> settlementList = MetaDaoHelper.query(Settlement.ENTITY_NAME,querySchema);
//		if(ValueUtils.isNotEmpty(settlementList) && settlementList.size() > 0){
//			throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101854"),com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage("P_YS_FI_CM_0000026273") /* "该单据已日结，不能修改或删除单据！" */);
//		}
		// end
		Short srcItem = -1;
		if (bizObject == null || bizObject.get("billtype") == null || EventType.CurrencyExchangeBill.getValue() != Short.parseShort(bizObject.get("billtype").toString())) {
			throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101855"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1938878E05880008","事项来源非外币兑换工作台的单据，不允许进行删除，请在上游单据进行处理！") /* "事项来源非外币兑换工作台的单据，不允许进行删除，请在上游单据进行处理！" */);
		}
		if (bizObject.get("srcitem") != null) {
			srcItem = Short.valueOf(bizObject.get("srcitem").toString());
		}
		if(bill.getPartParam() != null && bill.getPartParam().get("outsystem") != null){
			//来源为费用的不做校验
			//来源为商业汇票的要校验单据是否结算，结算后不能删除
			if(srcItem.equals(EventSource.Drftchase.getValue())&&bizObject.get("settlestatus").equals(SettleStatus.alreadySettled.getValue())){
				throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101856"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041802E6","该单据在现金管理已经结算,不能进行删除!") /* "该单据在现金管理已经结算,不能进行删除!" */);
			}
		}else{
			if(billNum.startsWith("cmp")){
				if(!srcItem.equals(EventSource.Cmpchase.getValue())){
					throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101856"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041802E6","该单据在现金管理已经结算,不能进行删除!") /* "该单据在现金管理已经结算,不能进行删除!" */);
				}
			}
			if((billNum.startsWith("arap") || "paymentlist".equals(billNum))){
				if((srcItem.equals(EventSource.Cmpchase.getValue()) ||
						srcItem.equals(EventSource.SystemOut.getValue()))){
					throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101857"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041802EA","该单据不是收付单据，不能进行删除！") /* "该单据不是收付单据，不能进行删除！" */);
				}
			}
			Short auditStatus = -1;
			if (bizObject.get("auditstatus") != null) {
				auditStatus = Short.valueOf(bizObject.get("auditstatus").toString());
			}
			if(auditStatus.equals(AuditStatus.Complete.getValue())){
				throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101858"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041802E8","已审核单据，不能进行删除！") /* "已审核单据，不能进行删除！" */);
			}
		}
		checkPubTs(bizObject.getPubts() , bizObject.getId());
		//删除日记账
		CmpWriteBankaccUtils.delAccountBook(bizObject.get("id").toString());

		alterDelOpertate(bizObject);

		return new RuleExecuteResult();
	}

	//单据删除时，如果该单据与银行对账单
	private void alterDelOpertate(BizObject bizObject) {
		//数据来源为银行对账单和认领单的数据，走BankToCurrencyExchangeDeleteRule处理关联删除逻辑
		String datasource = bizObject.get("datasource") != null ? bizObject.get("datasource").toString() : null;
		if ("16".equals(datasource) || "80".equals(datasource)) {
			return;
		}
		try {
			String smartCheckNo = "";
			//收款是否关联
			Long collectbankbill = bizObject.get("collectbankbill");
			Long paybankbill = bizObject.get("paybankbill");
			Long collectbillclaim = bizObject.get("collectbillclaim");
			Long paybillclaim = bizObject.get("paybillclaim");
			Set<Long> bankIds = new HashSet<>();
			if (!Objects.isNull(collectbankbill)) {
				bankIds.add(collectbankbill);
			}
			if (!Objects.isNull(paybankbill)) {
				bankIds.add(paybankbill);
			}
			Set<Long> claimIds = new HashSet<>();
			if (!Objects.isNull(collectbillclaim)) {
				claimIds.add(collectbillclaim);
			}
			if (!Objects.isNull(paybillclaim)) {
				claimIds.add(paybillclaim);
			}

			if (collectbankbill == null && paybankbill == null) {
				//都为空说明未进行关联操作，只删除外币兑换单即可
			} else {
				if (collectbillclaim == null && paybillclaim == null && CollectionUtils.isNotEmpty(bankIds)) {
					//银行对账单操作
					QuerySchema querySchema = QuerySchema.create().addSelect(ICmpConstant.SELECT_TOTAL_PARAM);
					QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("id").in(bankIds));
					querySchema.addCondition(group);
					List<BankReconciliation> bankReconciliations = MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME, querySchema, null);
					if (CollectionUtils.isNotEmpty(bankReconciliations)) {
						for (BankReconciliation item : bankReconciliations) {
							//改到CommonSaveUtils中统一操作
							/*item.setAssociationstatus(AssociationStatus.NoAssociated.getValue());*/
							/*item.setSerialdealendstate(SerialdealendState.UNEND.getValue());*/
							if (!item.getIsparsesmartcheckno()) {
								smartCheckNo = RemoteDubbo.get(ReconciliateCodeGenerator.class, IDomainConstant.MDD_DOMAIN_STWB).generate();
								item.setSmartcheckno(smartCheckNo);
							}
							item.setSerialdealtype(null);
							item.setEntityStatus(EntityStatus.Update);
						}
						//更新银行对账单为未关联
						CommonSaveUtils.updateBankReconciliation(bankReconciliations);
						//银行对账单子表子表明细id
						QuerySchema querySchema1 = QuerySchema.create().addSelect(ICmpConstant.SELECT_TOTAL_PARAM);
						querySchema1.addCondition(QueryConditionGroup.and(QueryCondition.name("bankreconciliation").in(bankIds)));
						querySchema1.addCondition(QueryConditionGroup.and(QueryCondition.name("billid").eq(bizObject.get("id"))));
						List<BankReconciliationbusrelation_b> bankR_bs = MetaDaoHelper.queryObject(BankReconciliationbusrelation_b.ENTITY_NAME, querySchema1, null);
						if (bankR_bs != null && bankR_bs.size() > 0) {
							//删除对账单子表
							/*MetaDaoHelper.batchDelete(BankReconciliationbusrelation_b.ENTITY_NAME, Lists.newArrayList(new SimpleCondition(
									"id", ConditionOperator.in, bankR_bs.stream().map(BankReconciliationbusrelation_b::getId).collect(Collectors.toList()))));*/
							CommonSaveUtils.batchDeleteBankReconciliationbusrelation_b(bankR_bs);
						} else {
							throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101859"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00007", "数据无效，请刷新后重试") /* "数据无效，请刷新后重试" */);
						}
					} else {
						throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101859"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00007", "数据无效，请刷新后重试") /* "数据无效，请刷新后重试" */);
					}
				} else {
					//我的认领相关操作
					if (CollectionUtils.isNotEmpty(claimIds)) {
						for (Long claimid : claimIds) {
							smartCheckNo = RemoteDubbo.get(ReconciliateCodeGenerator.class, IDomainConstant.MDD_DOMAIN_STWB).generate();
							BillClaim billClaim = MetaDaoHelper.findById(BillClaim.ENTITY_NAME, claimid, 2);
							if (BillClaimType.Part.getValue() == billClaim.getClaimtype()) {
								//部分认领处理
								List bankIdList = billClaim.items().stream().map(BillClaimItem::getBankbill).collect(Collectors.toList());
								QueryConditionGroup group = new QueryConditionGroup(ConditionOperator.and);
								group.addCondition(QueryConditionGroup.and(QueryCondition.name("billid").eq(bizObject.get("id"))));
								group.addCondition(QueryConditionGroup.and(QueryCondition.name("bankreconciliation").in(bankIdList)));
								QuerySchema querySchema1 = QuerySchema.create().addSelect(ICmpConstant.SELECT_TOTAL_PARAM);
								querySchema1.addCondition(group);
								//查询银行对账单子表（对账单业务单据关联表）
								List<BankReconciliationbusrelation_b> bankReconciliationbusrelation_bs = MetaDaoHelper.queryObject(BankReconciliationbusrelation_b.ENTITY_NAME, querySchema1, null);

								if (bankReconciliationbusrelation_bs != null && bankReconciliationbusrelation_bs.size() > 0) {
									bankIdList = bankReconciliationbusrelation_bs.stream().map(BankReconciliationbusrelation_b::getBankreconciliation).collect(Collectors.toList());

									QuerySchema querySchema2 = QuerySchema.create().addSelect(ICmpConstant.SELECT_TOTAL_PARAM);
									QueryConditionGroup group2 = QueryConditionGroup.and(QueryCondition.name("id").in(bankIdList));
									querySchema2.addCondition(group2);

									List<BankReconciliation> bankReconciliations = MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME, querySchema2, null);

									//删除对账单子表
									/*MetaDaoHelper.batchDelete(BankReconciliationbusrelation_b.ENTITY_NAME, Lists.newArrayList(new SimpleCondition(
											"id", ConditionOperator.in, bankReconciliationbusrelation_bs.stream().<String>map(BizObject::getId).collect(Collectors.toList()))));*/
									CommonSaveUtils.batchDeleteBankReconciliationbusrelation_b(bankReconciliationbusrelation_bs);
									//下游单据关联检查
									QuerySchema querySchema4 = QuerySchema.create().addSelect(ICmpConstant.SELECT_TOTAL_PARAM);
									QueryConditionGroup group4 = QueryConditionGroup.and(QueryCondition.name(
											"bankreconciliation").in(bankIdList));
									querySchema4.addCondition(group4);

									List<BankReconciliationbusrelation_b> bankReconciliationsCheck = MetaDaoHelper.queryObject(BankReconciliationbusrelation_b.ENTITY_NAME, querySchema4, null);

									if (bankReconciliationsCheck != null && bankReconciliationsCheck.size() > 0) {
										//第一笔删除了，第二笔没删除，就是  更新银行对账单已关联未完结；
										if (CollectionUtils.isNotEmpty(bankReconciliations)) {
											for (BankReconciliation bankReconciliation : bankReconciliations) {
												//改到CommonSaveUtils中统一操作
												/*bankReconciliation.setSerialdealendstate(SerialdealendState.UNEND.getValue());*/
												if (!bankReconciliation.getIsparsesmartcheckno()) {
													bankReconciliation.setSmartcheckno(smartCheckNo);
												}
												bankReconciliation.setSerialdealtype(null);
												bankReconciliation.setEntityStatus(EntityStatus.Update);
											}
											CommonSaveUtils.updateBankReconciliation(bankReconciliations);
										}
									} else {
										//如果第二笔也删除了，就是  更新银行对账单未关联未完结
										if (CollectionUtils.isNotEmpty(bankReconciliations)) {
											for (BankReconciliation bankReconciliation : bankReconciliations) {
												//改到CommonSaveUtils中统一操作
												/*bankReconciliation.setAssociationstatus(AssociationStatus.NoAssociated.getValue());*/
												/*bankReconciliation.setSerialdealendstate(SerialdealendState.UNEND.getValue());*/
												if (!bankReconciliation.getIsparsesmartcheckno()) {
													bankReconciliation.setSmartcheckno(smartCheckNo);
												}
												bankReconciliation.setSerialdealtype(null);
												bankReconciliation.setEntityStatus(EntityStatus.Update);
											}
											CommonSaveUtils.updateBankReconciliation(bankReconciliations);
										}
									}
									//更新我的认领为未关联
									billClaim.setAssociationstatus(AssociationStatus.NoAssociated.getValue());
									billClaim.setSmartcheckno(smartCheckNo);
									billClaim.setClaimcompletetype(null);
									billClaim.setAssociatedoperator(null);
									billClaim.setAssociateddate(null);
									billClaim.setEntityStatus(EntityStatus.Update);
									CommonSaveUtils.updateBillClaim(billClaim);
								}
							} else if (BillClaimType.Merge.getValue() == billClaim.getClaimtype()) {
								//合并认领处理
								List<BillClaimItem> billClaimItems = billClaim.items();
								if (billClaimItems != null && billClaimItems.size() > 0) {
									for (BillClaimItem forDelete : billClaimItems) {
										smartCheckNo = RemoteDubbo.get(ReconciliateCodeGenerator.class, IDomainConstant.MDD_DOMAIN_STWB).generate();
										QuerySchema querySchema2 = QuerySchema.create().addSelect(ICmpConstant.SELECT_TOTAL_PARAM);
										QueryConditionGroup group2 = new QueryConditionGroup(ConditionOperator.and);
										group2.addCondition(QueryCondition.name("bankreconciliation").eq(forDelete.getBankbill()));
										group2.addCondition(QueryConditionGroup.and(QueryCondition.name("billid").eq(bizObject.get("id"))));
										querySchema2.addCondition(group2);

										List<BankReconciliationbusrelation_b> bankRbs = MetaDaoHelper.queryObject(BankReconciliationbusrelation_b.ENTITY_NAME, querySchema2, null);

										if (bankRbs != null && bankRbs.size() > 0) {
											List bankIdList = bankRbs.stream().map(BankReconciliationbusrelation_b::getBankreconciliation).collect(Collectors.toList());

											QuerySchema querySchema3 = QuerySchema.create().addSelect(ICmpConstant.SELECT_TOTAL_PARAM);
											QueryConditionGroup group3 = QueryConditionGroup.and(QueryCondition.name("id").in(bankIdList));
											querySchema3.addCondition(group3);

											List<BankReconciliation> bankReconciliations = MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME, querySchema3, null);
											if (bankReconciliations != null && bankReconciliations.size() > 0) {
												for (BankReconciliation brs : bankReconciliations) {
													//改到CommonSaveUtils中统一操作
													/*brs.setAssociationstatus(AssociationStatus.NoAssociated.getValue());*/
													/*brs.setSerialdealendstate(SerialdealendState.UNEND.getValue());*/
													if (!brs.getIsparsesmartcheckno()) {
														brs.setSmartcheckno(smartCheckNo);
													}
													brs.setSerialdealtype(null);
													brs.setEntityStatus(EntityStatus.Update);

												}
												CommonSaveUtils.updateBankReconciliation(bankReconciliations);
											}

											//删除对账单子表
											/*MetaDaoHelper.batchDelete(BankReconciliationbusrelation_b.ENTITY_NAME, Lists.newArrayList(new SimpleCondition(
													"id", ConditionOperator.in, bankRbs.stream().<String>map(BizObject::getId).collect(Collectors.toList()))));*/
											CommonSaveUtils.batchDeleteBankReconciliationbusrelation_b(bankRbs);
										}
									}

									//更新我的认领为未关联
									billClaim.setAssociationstatus(AssociationStatus.NoAssociated.getValue());
									billClaim.setClaimcompletetype(null);
									billClaim.setAssociatedoperator(null);
									billClaim.setAssociateddate(null);
									billClaim.setSmartcheckno(smartCheckNo);
									billClaim.setEntityStatus(EntityStatus.Update);
									CommonSaveUtils.updateBillClaim(billClaim);
								}
							} else {
								//整单认领处理
								List bankIdList = billClaim.items().stream().map(BillClaimItem::getBankbill).collect(Collectors.toList());
								QuerySchema querySchema1 = QuerySchema.create().addSelect(ICmpConstant.SELECT_TOTAL_PARAM);
								QueryConditionGroup group = new QueryConditionGroup(ConditionOperator.and);
								group.addCondition(QueryConditionGroup.and(QueryCondition.name("billid").eq(bizObject.get("id"))));
								group.addCondition(QueryConditionGroup.and(QueryCondition.name("bankreconciliation").in(bankIdList)));
								querySchema1.addCondition(group);
								//查询银行对账单子表（对账单业务单据关联表）
								List<BankReconciliationbusrelation_b> bankReconciliationbusrelation_bs = MetaDaoHelper.queryObject(BankReconciliationbusrelation_b.ENTITY_NAME, querySchema1, null);
								smartCheckNo = RemoteDubbo.get(ReconciliateCodeGenerator.class, IDomainConstant.MDD_DOMAIN_STWB).generate();
								if (bankReconciliationbusrelation_bs != null && bankReconciliationbusrelation_bs.size() > 0) {
									bankIdList = bankReconciliationbusrelation_bs.stream().map(BankReconciliationbusrelation_b::getBankreconciliation).collect(Collectors.toList());

									QuerySchema querySchema2 = QuerySchema.create().addSelect(ICmpConstant.SELECT_TOTAL_PARAM);
									QueryConditionGroup group2 = QueryConditionGroup.and(QueryCondition.name("id").in(bankIdList));
									querySchema2.addCondition(group2);

									List<BankReconciliation> bankReconciliations = MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME, querySchema2, null);

									if (bankReconciliations != null && bankReconciliations.size() > 0) {
										for (BankReconciliation brs : bankReconciliations) {
											//改到CommonSaveUtils中统一操作
											/*brs.setAssociationstatus(AssociationStatus.NoAssociated.getValue());*/
											/*brs.setSerialdealendstate(SerialdealendState.UNEND.getValue());*/
											if (!brs.getIsparsesmartcheckno()) {
												brs.setSmartcheckno(smartCheckNo);
											}
											brs.setSerialdealtype(null);
											brs.setEntityStatus(EntityStatus.Update);
										}
										CommonSaveUtils.updateBankReconciliation(bankReconciliations);

										billClaim.setAssociationstatus(AssociationStatus.NoAssociated.getValue());
										billClaim.setClaimcompletetype(null);
										billClaim.setAssociatedoperator(null);
										billClaim.setAssociateddate(null);
										billClaim.setSmartcheckno(smartCheckNo);
										billClaim.setEntityStatus(EntityStatus.Update);
										CommonSaveUtils.updateBillClaim(billClaim);

										//删除对账单子表
										/*MetaDaoHelper.batchDelete(BankReconciliationbusrelation_b.ENTITY_NAME, Lists.newArrayList(new SimpleCondition(
												"id", ConditionOperator.in, bankReconciliationbusrelation_bs.stream().<String>map(BizObject::getId).collect(Collectors.toList()))));*/
										CommonSaveUtils.batchDeleteBankReconciliationbusrelation_b(bankReconciliationbusrelation_bs);
									}
								}
							}
						}
					}
				}
			}
		} catch (Exception e) {
			throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101860"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00008", "删除失败") /* "删除失败" */);
		}
	}
	/*
	 *@Author
	 *@Description 校验时间戳    外币兑换删除
	 *@Date 2020/7/6 14:25
	 *@Param [rows]
	 *@Return void
	 **/
	private void checkPubTs(Date puts , Long id ) throws Exception{
		CurrencyExchange currencyExchange = MetaDaoHelper.findById(CurrencyExchange.ENTITY_NAME , id);
		if(puts.compareTo(currencyExchange.getPubts()) != 0){
			throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101861"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041802E7","数据无效，请刷新后重试") /* "数据无效，请刷新后重试" */);
		}
	}
}