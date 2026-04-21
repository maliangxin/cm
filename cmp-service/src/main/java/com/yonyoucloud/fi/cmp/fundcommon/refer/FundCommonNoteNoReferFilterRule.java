package com.yonyoucloud.fi.cmp.fundcommon.refer;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.FilterVO;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.SimpleFilterVO;
import com.yonyou.ucf.mdd.common.utils.MddBaseUtils;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.autoparam.service.AutoConfigService;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.constant.IBussinessConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.fundcollection.FundCollection_b;
import com.yonyoucloud.fi.cmp.fundpayment.FundPayment_b;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import com.yonyoucloud.fi.drft.api.openapi.IOpenNoteRefFilterService;
import com.yonyoucloud.fi.drft.post.vo.output.OpenNoteRefReqVO;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * <h1>资金单据票据号过滤</h1>
 * <p>
 * 票据号 过滤条件
 * 资金收款单：资金组织、币种、票据类型、票据状态（已收票）、使用状态（未使用）；
 * 资金付款单：过滤条件：资金组织、币种、票据类型、
 * 票据标识=纸票，票据状态=已收票（已使用）、已签发；
 * 票据标识=电票，票据状态=已收票（已使用）、已付票（未使用）；
 * <p>
 * <enum name="NoteFlag" title="票据标识">
 * <item name="Elec" title="电子票据" value="1" />
 * <item name="Paper" title="纸质票据" value="2" />
 * </enum>
 * </p>
 * <enum name="NoteStatus" title="票据状态">
 * <item name="None" title="无状态" value="1" />
 * <item name="On_Gather" title="在收票" value="2" />
 * <item name="Has_Gather" title="已收票" value="3" />
 * <item name="On_Impawn" title="在质押" value="4" />
 * <item name="Has_Impawn" title="已质押" value="5" />
 * <item name="On_Sign" title="在签发" value="6" />
 * <item name="Has_Sign" title="已签发" value="7" />
 * <item name="On_Paybill" title="在付票" value="8" />
 * <item name="Has_Paybill" title="已付票" value="9" />
 * <item name="On_Endore" title="在背书" value="10" />
 * <item name="Has_Endore" title="已背书" value="11" />
 * <item name="On_Discount" title="在贴现" value="12" />
 * <item name="Has_Discount" title="已贴现" value="13" />
 * <item name="On_Collection" title="在托收" value="14" />
 * <item name="Has_Collection" title="已托收" value="15" />
 * <item name="On_Pay" title="在兑付" value="16" />
 * <item name="Has_Pay" title="已兑付" value="17" />
 * <item name="On_Apply" title="在申请" value="18" />
 * <item name="Has_Apply" title="已申请" value="19" />
 * <item name="On_UnImpawn" title="在解押" value="20" />
 * </enum>
 *
 * @author Sun GuoCai
 * @version 1.0
 * @since 2021-12-14 9:59
 */
@Component
public class FundCommonNoteNoReferFilterRule extends AbstractCommonRule {
    @Resource
    private IOpenNoteRefFilterService openNoteRefFilterService;


    @Autowired
    AutoConfigService autoConfigService;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        BillDataDto billDataDto = (BillDataDto) getParam(map);
        // 新票据参照过滤
        if ("drft.drft_openbillnoref".equals(billDataDto.getrefCode())) {
            FilterVO filterVO = new FilterVO();
            if (billDataDto.getCondition() != null) {
                filterVO = billDataDto.getCondition();
            }
            String billNum = billDataDto.getBillnum();
            List<BizObject> bills = getBills(billContext, map);
            if (bills.size() == 0) {
                return new RuleExecuteResult();
            }
            BizObject bizObject = bills.get(0);
            OpenNoteRefReqVO reqVO = new OpenNoteRefReqVO();
            reqVO.setAccentity(bizObject.get("accentity"));
            reqVO.setCurrency(bizObject.get("currency"));
            reqVO.setNewNoteFlag("1");
            List<BizObject> list1 = null;
            if (IBillNumConstant.FUND_COLLECTION.equals(billNum)) {
                reqVO.setDirection(0);
                list1 = bizObject.get("FundCollection_b");
            } else if (IBillNumConstant.FUND_PAYMENT.equals(billNum)) {
                reqVO.setDirection(1);
                reqVO.setHasused((short) 1);
                list1 = bizObject.get("FundPayment_b");
                // receiverName和应付票据的收款人匹配
                reqVO.setReceiverName((String) list1.get(0).get("oppositeobjectname"));
            }
            // CZFW-245020 资金收款单/资金付款单，打开票据号时新增入参，屏蔽掉结算中心代理持票的票据--下周提供给湖南建投现场
            reqVO.setNeedAgentNote(false);
            if (ValueUtils.isNotEmptyObj(list1) && ValueUtils.isNotEmptyObj(list1.get(0))) {
                Long noteType = list1.get(0).get("notetype");
                if (noteType != null) {
                    reqVO.setNotetype(noteType.toString());
                }
            }
            //收付款金额
            if (CollectionUtils.isNotEmpty(list1) && ValueUtils.isNotEmptyObj(list1.get(0).get("oriSum"))&&IBillNumConstant.FUND_PAYMENT.equals(billNum)) {
                reqVO.setAmount(new BigDecimal(list1.get(0).get("oriSum").toString()));
            }

            //2 付款：非等分票票据金额小于等于付款金额  收款：票据金额=收款金额
            reqVO.setAmountFilterMode("2");
            billDataDto.getCustMap().put("openNoteRefReqVO", reqVO);
//            List<String> noteList = openNoteRefFilterService.queryNoteIDList(jSONObject);
//            if (ValueUtils.isNotEmpty(noteList)) {
//                filterVO.appendCondition(ConditionOperator.and, new SimpleFilterVO("id", ICmpConstant.QUERY_IN, noteList));//票据号id
//            } else {
//                filterVO.appendCondition(ConditionOperator.and, new SimpleFilterVO("id", ICmpConstant.QUERY_EQ, null));//票据号id
//            }

            //智能配票
//            Map<String, List<String>> noteListMap = openNoteRefFilterService.queryVoteNoteIdList(reqVO);
//            if (StringUtils.isNotEmpty((String) billDataDto.getCustMap().get("bill_test")) && "1".equals(billDataDto.getCustMap().get("bill_test"))) {
//                //显示全部
//                if (CollectionUtils.isNotEmpty(noteListMap.get("bills"))) {
//                    filterVO.appendCondition(ConditionOperator.and,new SimpleFilterVO("id", ICmpConstant.QUERY_IN, noteListMap.get("bills")));//票据号id
//                } else {
//                    filterVO.appendCondition(ConditionOperator.and,new SimpleFilterVO("id", ICmpConstant.QUERY_EQ, null));//票据号id
//                }
//            } else {
//                //非显示全部
//                if (CollectionUtils.isNotEmpty(noteListMap.get("votes"))) {
//                    filterVO.appendCondition(ConditionOperator.and,new SimpleFilterVO("id", ICmpConstant.QUERY_IN, noteListMap.get("votes")));//智能配票票据号id
//                } else if (CollectionUtils.isNotEmpty(noteListMap.get("bills"))) {
//                    filterVO.appendCondition(ConditionOperator.and,new SimpleFilterVO("id", ICmpConstant.QUERY_IN, noteListMap.get("bills")));//票据号id
//                } else {
//                    filterVO.appendCondition(ConditionOperator.and,new SimpleFilterVO("id", ICmpConstant.QUERY_EQ, null));//票据号id
//                }
//            }
        } else if ("drft.drft_billnoref".equals(billDataDto.getrefCode())) {
            String billNum = billDataDto.getBillnum();
            List<BizObject> bills = getBills(billContext, map);
            if (bills.size() == 0) {
                return new RuleExecuteResult();
            }
            BizObject bizObject = bills.get(0);
            FilterVO filterVO = new FilterVO();
            if (billDataDto.getCondition() != null) {
                filterVO = billDataDto.getCondition();
            }

            Object accent = bizObject.get(IBussinessConstant.ACCENTITY);
            if (accent != null) {
                billDataDto.setCondition((FilterVO) MddBaseUtils.appendCondition(filterVO, IBussinessConstant.ACCENTITY, ICmpConstant.QUERY_EQ, accent));
            }
            Object currency = bizObject.get("currency");
            if (currency != null) {
                billDataDto.setCondition((FilterVO) MddBaseUtils.appendCondition(filterVO, "currency", ICmpConstant.QUERY_EQ, currency));
            }
            if (billDataDto.getDatasource().equals("noteno.noteno")) {

                List<BizObject> list;
                String noteFlag = null;
                if ("cmp_fundpayment".equals(billNum)) {
                    // 过滤集成使用状态（未占用：0；收票已占用：1；付票已占用：2。）
                    billDataDto.setCondition((FilterVO) MddBaseUtils.appendCondition(filterVO, "integratestatus", ICmpConstant.QUERY_EQ, 1));
                    list = bizObject.get("FundPayment_b");
                    if (ValueUtils.isNotEmptyObj(list) && ValueUtils.isNotEmptyObj(list.get(0))) {
                        noteFlag = list.get(0).get("notetype_iselecbill");
                        Long noteType = list.get(0).get("notetype");
                        if (noteType != null) {
                            billDataDto.setCondition((FilterVO) MddBaseUtils.appendCondition(filterVO, "notetype", ICmpConstant.QUERY_EQ, noteType));
                        }
                    }

                    QueryConditionGroup queryConditionGroup1 = new QueryConditionGroup(ConditionOperator.or);
                    if (!ValueUtils.isNotEmptyObj(noteFlag) || ("1").equals(noteFlag)) {
                        QueryConditionGroup queryConditionGroup2 = new QueryConditionGroup(ConditionOperator.and);
                        QueryConditionGroup conditionExpressions = new QueryConditionGroup();
                        QueryCondition queryCondition = new QueryCondition("noteflag", ConditionOperator.eq, 1);
                        conditionExpressions.appendCondition(queryCondition);
                        QueryConditionGroup queryConditionGroup4 = new QueryConditionGroup(ConditionOperator.or);
                        QueryConditionGroup queryConditionGroup5 = new QueryConditionGroup(ConditionOperator.and);
                        QueryCondition queryCondition1 = new QueryCondition("notestatus", ConditionOperator.eq, 3);
                        QueryCondition queryCondition2 = new QueryCondition("hasused", ConditionOperator.eq, 1);
                        queryConditionGroup5.addCondition(queryCondition1);
                        queryConditionGroup5.addCondition(queryCondition2);
                        QueryConditionGroup queryConditionGroup6 = new QueryConditionGroup(ConditionOperator.and);
                        QueryCondition queryCondition3 = new QueryCondition("notestatus", ConditionOperator.eq, 9);
                        QueryCondition queryCondition4 = new QueryCondition("hasused", ConditionOperator.eq, 0);
                        queryConditionGroup6.addCondition(queryCondition3);
                        queryConditionGroup6.addCondition(queryCondition4);
                        queryConditionGroup4.addCondition(queryConditionGroup5);
                        queryConditionGroup4.addCondition(queryConditionGroup6);
                        conditionExpressions.appendCondition(queryConditionGroup4);
                        queryConditionGroup2.addCondition(conditionExpressions);

                        queryConditionGroup1.addCondition(queryConditionGroup2);

                    }

                    if (!ValueUtils.isNotEmptyObj(noteFlag) || ("2").equals(noteFlag)) {
                        QueryConditionGroup queryConditionGroup3 = new QueryConditionGroup(ConditionOperator.and);
                        QueryCondition queryCondition5 = new QueryCondition("noteflag", ConditionOperator.eq, 2);
                        QueryConditionGroup queryConditionGroup7 = new QueryConditionGroup(ConditionOperator.or);
                        QueryConditionGroup queryConditionGroup8 = new QueryConditionGroup(ConditionOperator.and);
                        QueryCondition queryCondition6 = new QueryCondition("notestatus", ConditionOperator.eq, 3);
                        QueryCondition queryCondition7 = new QueryCondition("hasused", ConditionOperator.eq, 1);
                        queryConditionGroup8.addCondition(queryCondition6);
                        queryConditionGroup8.addCondition(queryCondition7);
                        QueryConditionGroup queryConditionGroup9 = new QueryConditionGroup(ConditionOperator.and);
                        QueryCondition queryCondition8 = new QueryCondition("notestatus", ConditionOperator.eq, 7);
                        queryConditionGroup9.addCondition(queryCondition8);
                        queryConditionGroup7.addCondition(queryConditionGroup8);
                        queryConditionGroup7.addCondition(queryConditionGroup9);

                        queryConditionGroup3.addCondition(queryCondition5);
                        queryConditionGroup3.addCondition(queryConditionGroup7);


                        queryConditionGroup1.addCondition(queryConditionGroup3);
                    }
                    filterVO.setQueryConditionGroup(queryConditionGroup1);
                }
                if ("cmp_fundcollection".equals(billNum)) {
                    billDataDto.setCondition((FilterVO) MddBaseUtils.appendCondition(filterVO, "hasused", ICmpConstant.QUERY_EQ, 0));
                    // 过滤集成使用状态（未占用：0；收票已占用：1；付票已占用：2。）
                    billDataDto.setCondition((FilterVO) MddBaseUtils.appendCondition(filterVO, "integratestatus", ICmpConstant.QUERY_EQ, 0));
                    billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("notestatus",
                            ICmpConstant.QUERY_IN, new Short[]{3}));
                    list = bizObject.get("FundCollection_b");
                    if (ValueUtils.isNotEmptyObj(list) && ValueUtils.isNotEmptyObj(list.get(0))) {
                        Long noteType = list.get(0).get("notetype");
                        if (noteType != null) {
                            billDataDto.setCondition((FilterVO) MddBaseUtils.appendCondition(filterVO, "notetype", ICmpConstant.QUERY_EQ, noteType));
                        }
                        noteFlag = list.get(0).get("notetype_iselecbill");
                        if (noteFlag != null) {
                            billDataDto.setCondition((FilterVO) MddBaseUtils.appendCondition(filterVO, "noteflag", ICmpConstant.QUERY_EQ, noteFlag));
                        }

                    }
                }
            } else if (billDataDto.getDatasource().equals("tx_noteno.noteno")) {
                billDataDto.setCondition((FilterVO) MddBaseUtils.appendCondition(filterVO, "notestatus", ICmpConstant.QUERY_IN, new Integer[]{9, 16}));
                billDataDto.setCondition((FilterVO) MddBaseUtils.appendCondition(filterVO, "buyerinterest", ICmpConstant.QUERY_EQ, 1));
            }

        }
        if ("drft.drft_billtyperef".equals(billDataDto.getrefCode())) {
            List<BizObject> bills = getBills(billContext, map);
            if (bills.size() <= 0) {
                return new RuleExecuteResult();
            }
            FilterVO filterVO = new FilterVO();
            if (billDataDto.getCondition() != null) {
                filterVO = billDataDto.getCondition();
            }
            billDataDto.setCondition((FilterVO) MddBaseUtils.appendCondition(filterVO, "isEnabled", ICmpConstant.QUERY_EQ, true));
        }

        // 过滤支票
        if ("cmp_checkRef".equals(billDataDto.getrefCode())) {
            List<BizObject> bills = getBills(billContext, map);
            if (bills.size() <= 0) {
                return new RuleExecuteResult();
            }
            BizObject bizObject = bills.get(0);
            FilterVO filterVO = new FilterVO();
            if (billDataDto.getCondition() != null) {
                filterVO = billDataDto.getCondition();
            }
            //资金组织
            String accentity = (String)bizObject.get("accentity");
           // billDataDto.setCondition((FilterVO) MddBaseUtils.appendCondition(filterVO, "accentity", ICmpConstant.QUERY_EQ, accentity));
            //支票类型
            billDataDto.setCondition((FilterVO) MddBaseUtils.appendCondition(filterVO, "checkBillType", ICmpConstant.QUERY_IN, new Integer[]{0,1,2,3,4,5}));
            // 支票是否被占用
            billDataDto.setCondition((FilterVO) MddBaseUtils.appendCondition(filterVO, "occupy", ICmpConstant.QUERY_EQ, 0));
            //支票状态
            //billDataDto.setCondition((FilterVO) MddBaseUtils.appendCondition(filterVO, "checkBillStatus", ICmpConstant.QUERY_EQ, 1));
            //CM240315YT-3：重空凭证对外提供的参照的状态调整： 重空凭证对外提供的参照，需要改为提供“是否可使用=是”的数据：
            // 如果开启领用则判断支票状态是否为已领用，如果未开启领用则判断支票状态是否为已入库

            // billDataDto.setCondition((FilterVO) MddBaseUtils.appendCondition(filterVO, "isUsed", ICmpConstant.QUERY_EQ, 1));
            short billtype = (short) bizObject.get("billtype");
            Object currency = bizObject.get("currency");
            String billNum = billDataDto.getBillnum();
            if ((billtype == 18 || ((billtype == 16 || billtype==80) && null != bizObject.get("FundPayment_b"))) || (IBillNumConstant.FUND_PAYMENT.equals(billNum) && billtype == 100)) {//资金付款
                if(autoConfigService.getCheckStockCanUse()){
                    billDataDto.setCondition((FilterVO) MddBaseUtils.appendCondition(filterVO, "checkBillStatus", ICmpConstant.QUERY_EQ, 13));
                    billDataDto.setCondition((FilterVO) MddBaseUtils.appendCondition(filterVO, "custNo", ICmpConstant.QUERY_EQ, accentity));
                }else{
                    billDataDto.setCondition((FilterVO) MddBaseUtils.appendCondition(filterVO, "checkBillStatus", ICmpConstant.QUERY_EQ, 1));
                    billDataDto.setCondition((FilterVO) MddBaseUtils.appendCondition(filterVO, "accentity", ICmpConstant.QUERY_EQ, accentity));
                }
                //支票方向
                billDataDto.setCondition((FilterVO) MddBaseUtils.appendCondition(filterVO, "checkBillDir", ICmpConstant.QUERY_EQ, 2));
                List<FundPayment_b> transfereeInformationList = bizObject.get("FundPayment_b");
                if (!transfereeInformationList.isEmpty()) {
//                    Object enterpriseBankAccountName = transfereeInformationList.get(0).get("enterprisebankaccount_name");
//                    if (ValueUtils.isNotEmptyObj(enterpriseBankAccountName)) {
//                        billDataDto.setCondition((FilterVO) MddBaseUtils.appendCondition(filterVO, "drawerAcctName", ICmpConstant.QUERY_EQ, enterpriseBankAccountName));
//                    }
                    Object enterpriseBankAccountNo = transfereeInformationList.get(0).get("enterprisebankaccount_account");
                    if (ValueUtils.isNotEmptyObj(enterpriseBankAccountNo)) {
                        billDataDto.setCondition((FilterVO) MddBaseUtils.appendCondition(filterVO, "drawerAcctNo", ICmpConstant.QUERY_EQ, enterpriseBankAccountNo));
                    }
                }
            }else if ((billtype == 17 || ((billtype == 16 || billtype==80) && null != bizObject.get("FundCollection_b"))) || (IBillNumConstant.FUND_COLLECTION.equals(billNum) && billtype == 100)){//资金收款
                billDataDto.setCondition((FilterVO) MddBaseUtils.appendCondition(filterVO, "accentity", ICmpConstant.QUERY_EQ, accentity));
                billDataDto.setCondition((FilterVO) MddBaseUtils.appendCondition(filterVO, "checkBillStatus", ICmpConstant.QUERY_EQ, 1));
                billDataDto.setCondition((FilterVO) MddBaseUtils.appendCondition(filterVO, "checkBillDir", ICmpConstant.QUERY_EQ, 1));
            }

            if (currency != null) {
                billDataDto.setCondition((FilterVO) MddBaseUtils.appendCondition(filterVO, "currency", ICmpConstant.QUERY_EQ, currency));
            }
            //银行对账单-资金收款单
            if((((billtype == 16 || billtype==80) && null == bizObject.get("FundPayment_b")) || billtype == 17) || (IBillNumConstant.FUND_COLLECTION.equals(billNum) && billtype == 100)){
                //收款金额
                BigDecimal oriSum = bizObject.getBizObjects("FundCollection_b", FundCollection_b.class).get(0).getOriSum();
//                billDataDto.setCondition((FilterVO) MddBaseUtils.appendCondition(filterVO, "amount", ICmpConstant.QUERY_EQ, oriSum));
            }

        }

        //过滤结算方式
        if ("productcenter.aa_settlemethodref".equals(billDataDto.getrefCode())) {
            List<BizObject> bills = getBills(billContext, map);
            if (bills.size() <= 0) {
                return new RuleExecuteResult();
            }
            BizObject bizObject = bills.get(0);
            FilterVO filterVO = new FilterVO();
            if (billDataDto.getTreeCondition() != null) {
                filterVO = billDataDto.getTreeCondition();
            }
            if (bizObject != null && bizObject.get("billtype") != null) {
                short billtype = (short) bizObject.get("billtype");
                if(billtype == 16){
                    //结算方式
                    billDataDto.setTreeCondition((FilterVO) MddBaseUtils.appendCondition(filterVO, "serviceAttr", ICmpConstant.QUERY_IN, new Integer[]{0, 8}));
                }
            }
        }
        return new RuleExecuteResult();
    }


}
