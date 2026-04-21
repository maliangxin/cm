package com.yonyoucloud.fi.cmp.bankreconciliation.service.count;

import com.yonyou.cloud.utils.CollectionUtils;
import com.yonyou.ucf.mdd.ext.bill.rule.util.BillInfoUtils;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.util.JSONBuilderUtils;
import com.yonyoucloud.fi.cmp.auth.OrgDataPermissionService;
import com.yonyoucloud.fi.cmp.autoparam.service.AutoConfigService;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.billclaim.BillClaim;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import com.yonyoucloud.fi.cmp.enums.SerialdealendState;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.consts.BankDealDetailConst;
import com.yonyoucloud.fi.cmp.util.CmpAuthUtils;
import com.yonyoucloud.fi.cmp.util.PageUtils;
import com.yonyoucloud.fi.cmp.util.StringUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class BankreconciliationCountServiceImpl implements IBillCountService {

    @Autowired
    AutoConfigService autoConfigService;
    // 所属组织视图
    final static String OWN_ORG_PAGER = "2";
    @Autowired
    OrgDataPermissionService orgDataPermissionService;
    /**
     * 银行对账单、到账认领中心、我的认领 中间统计区查询接口
     *
     * @param params 筛选区传入参数
     * @return
     * @throws Exception
     */
    @Override
    public HashMap<String, Object> getCount(CtmJSONObject params) throws Exception {
        HashMap<String, Object> map = new HashMap<>();
        List<String> accentitys = new ArrayList<>();
        List<String> bankaccounts = new ArrayList<>();
        List<String> orgids = new ArrayList<>();
        List<String> banktypes = new ArrayList<>();
        List<String> oppositeidentifystatuss = new ArrayList<>();
        List<Short> isrepeats = new ArrayList<>();
        //组装数据
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        QueryConditionGroup conditionGroup3 = new QueryConditionGroup(ConditionOperator.or);
        // 获取字段属性
        String billNo = params.getString("billNo");//页面编码
        String accentity = params.getString("accentity");//授权使用组织（银行流水认领、到账认领中心、我的认领）
        if (ObjectUtils.isNotEmpty(accentity)) {
            accentitys = params.getJSONArray("accentity").toJavaList(String.class);//授权使用组织（多个）
        }
        String pager = params.getString("pager");
        String startDate = params.getString("startDate");//交易开始日期（银行流水认领、到账认领中心、我的认领）
        String endDate = params.getString("endDate");//交易结束日期（银行流水认领、到账认领中心、我的认领）
        String confirmstatus = params.getString("confirmstatus");//授权使用组织状态
        String dcFlag = params.getString("dcFlag");//收付方向（银行流水认领、到账认领中心、我的认领）
        String currency = params.getString("currency");////币种（银行流水认领、到账认领中心、我的认领）
        String bankaccount = params.getString("bankaccount");//银行账号
        if (ObjectUtils.isNotEmpty(bankaccount)) {
            bankaccounts = JSONBuilderUtils.stringToBeanList(bankaccount, String.class);//银行账户（多个）
            conditionGroup.appendCondition(QueryCondition.name("bankaccount").in(bankaccounts));
        }
        String amountSmall = params.getString("amountSmall");//金额1
        String amountBig = params.getString("amountBig");//金额2
        String receiptassociation = params.getString("receiptassociation");//回单关联状态
        String orgid = params.getString("orgid");//所属组织
        if (ObjectUtils.isNotEmpty(orgid)) {
            orgids = params.getJSONArray("orgid").toJavaList(String.class);//所属组织（多个）
            conditionGroup.appendCondition(QueryCondition.name("orgid").in(orgids));
        }

        conditionGroup.appendCondition(QueryCondition.name("tenant").eq(AppContext.getTenantId()));//租户id
        if (ObjectUtils.isNotEmpty(currency)) {
            conditionGroup.appendCondition(QueryCondition.name("currency").eq(currency));
        }
        String ispublish = params.getString("ispublish");//是否发布
        String associationstatus = params.getString("associationstatus");//业务关联状态（银行流水认领）

        String isreturned = params.getString("isreturned");//退回标识
        String frozenstatus = params.getString("frozenstatus");//冻结状态
        String tranDateStart = params.getString("tranDateStart");//交易时间开始
        String tranDateEnd = params.getString("tranDateEnd");//交易时间结束
        String banktype = params.getString("banktype");//银行类别（多个）
        if (ObjectUtils.isNotEmpty(banktype)) {
            banktypes = Arrays.asList(banktype.substring(1, banktype.length() - 1).split(","));
        }
        String isrepeat  = params.getString("isrepeat");//疑重标识
        if (ObjectUtils.isNotEmpty(isrepeat)) {
            isrepeats = JSONBuilderUtils.stringToBeanList(isrepeat, Short.class);//疑重标识
            conditionGroup.appendCondition(QueryCondition.name("isrepeat").in(isrepeats));
        }
        String oppositeidentifystatus = params.getString("oppositeidentifystatus");//对方单位辨识状态（多个）
        if (ObjectUtils.isNotEmpty(oppositeidentifystatus)) {
            oppositeidentifystatuss = JSONBuilderUtils.stringToBeanList(oppositeidentifystatus, String.class);
        }
        String busscounterpart = params.getString("busscounterpart");//业务对接人（多个）

        // 银行交易流水号
        String banksqlno = params.getString("bankseqno");
        String cmpTableTabsActiveKey = params.getString("published_type");//发布对象,用来暂存前端页签信息
        if (ObjectUtils.isNotEmpty(billNo)) {
            QuerySchema querySchema;
            if (billNo.equals("cmp_bankreconciliationlist")) {//银行流水认领
                if (ObjectUtils.isNotEmpty(receiptassociation)) {
                    conditionGroup.appendCondition(QueryCondition.name("receiptassociation").eq(receiptassociation));
                }
                //授权使用组织状态
                if (ObjectUtils.isNotEmpty(confirmstatus)) {
                    conditionGroup.appendCondition(QueryCondition.name("confirmstatus").eq(confirmstatus));
                }
                //交易金额
                if (ObjectUtils.isNotEmpty(amountSmall) && ObjectUtils.isNotEmpty(amountBig)) {
                    conditionGroup.appendCondition(QueryCondition.name("tran_amt").between(amountSmall, amountBig));
                }
                if (ObjectUtils.isNotEmpty(amountSmall) && ObjectUtils.isEmpty(amountBig)) {
                    conditionGroup.appendCondition(QueryCondition.name("tran_amt").egt(amountSmall));
                }
                if (ObjectUtils.isEmpty(amountSmall) && ObjectUtils.isNotEmpty(amountBig)) {
                    conditionGroup.appendCondition(QueryCondition.name("tran_amt").elt(amountBig));
                }
                //收付方向
                if (ObjectUtils.isNotEmpty(dcFlag)) {
                    conditionGroup.appendCondition(QueryCondition.name("dc_flag").eq(dcFlag));
                }
                //交易日期
                if (ObjectUtils.isNotEmpty(startDate) && ObjectUtils.isNotEmpty(endDate)) {
                    conditionGroup.appendCondition(QueryCondition.name("tran_date").between(startDate, endDate));
                }
                if (ObjectUtils.isNotEmpty(startDate) && ObjectUtils.isEmpty(endDate)) {
                    conditionGroup.appendCondition(QueryCondition.name("tran_date").egt(startDate));
                }
                if (ObjectUtils.isEmpty(startDate) && ObjectUtils.isNotEmpty(endDate)) {
                    conditionGroup.appendCondition(QueryCondition.name("tran_date").elt(endDate));
                }
                //授权使用组织，当前为使用组织页签的时候才可以拼接上
                if (ObjectUtils.isNotEmpty(accentitys) && "1".equals(cmpTableTabsActiveKey)) {
                    conditionGroup3.addCondition(QueryCondition.name("accentity").in(accentitys));
                }
                // 如果授权使用组织和所属组织都为空，则查询授权使用组织有权限或所属组织有权限的数据
                if (ObjectUtils.isEmpty(orgids) && ObjectUtils.isEmpty(accentitys)) {
                    Set<String> orgs = BillInfoUtils.getOrgPermissions(IBillNumConstant.BANKRECONCILIATIONLIST);
                    if (CollectionUtils.isEmpty(orgs)) {
                        throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_229FB39E0570000C", "没有授权使用组织权限!") /* "没有授权使用组织权限!" */);
                     }
                    //CZFW-383380 需求改动：授权使用组织页签只拼接授权使用组织，所属组织页签只拼接所属组织
                    if("1".equals(cmpTableTabsActiveKey)){
                        conditionGroup3.addCondition(QueryCondition.name("accentity").in(orgs));
                    } else if ("2".equals(cmpTableTabsActiveKey)) {
                        conditionGroup3.addCondition(QueryCondition.name("orgid").in(orgs));
                    }
                    //没有页签标识，则全部拼接
                    if (ObjectUtils.isEmpty(cmpTableTabsActiveKey) ) {
                        conditionGroup3.addCondition(QueryCondition.name("accentity").in(orgs));
                        conditionGroup3.addCondition(QueryCondition.name("orgid").in(orgs));
                    }

                }

                conditionGroup.addCondition(conditionGroup3);
                //是否期初
                conditionGroup.appendCondition(QueryCondition.name("initflag").eq("0"));
                //是否发布
                if (ObjectUtils.isNotEmpty(ispublish)) {
                    conditionGroup.appendCondition(QueryCondition.name("ispublish").eq(ispublish));
                }
                //业务关联状态
                if (ObjectUtils.isNotEmpty(associationstatus)) {
                    conditionGroup.appendCondition(QueryCondition.name("associationstatus").eq(associationstatus));
                }
                //退回标识
                if (ObjectUtils.isNotEmpty(isreturned)) {
                    conditionGroup.appendCondition(QueryCondition.name("isreturned").eq(isreturned));
                }
                //冻结状态
                if (ObjectUtils.isNotEmpty(frozenstatus)) {
                    conditionGroup.appendCondition(QueryCondition.name("frozenstatus").eq(frozenstatus));
                }
                //交易时间
                if (ObjectUtils.isNotEmpty(tranDateStart) && ObjectUtils.isNotEmpty(tranDateEnd)) {
                    conditionGroup.appendCondition(QueryCondition.name("tran_time").between(tranDateStart, endDate));
                }
                if (ObjectUtils.isNotEmpty(tranDateStart) && ObjectUtils.isEmpty(tranDateEnd)) {
                    conditionGroup.appendCondition(QueryCondition.name("tran_time").egt(tranDateStart));
                }
                if (ObjectUtils.isEmpty(tranDateStart) && ObjectUtils.isNotEmpty(tranDateEnd)) {
                    conditionGroup.appendCondition(QueryCondition.name("tran_time").elt(tranDateEnd));
                }
                //银行类别
                if (ObjectUtils.isNotEmpty(banktypes)) {
                    conditionGroup.addCondition(QueryCondition.name("banktype").in(banktypes));
                }
                //对方单位辨识状态
                if (ObjectUtils.isNotEmpty(oppositeidentifystatuss)) {
                    conditionGroup.addCondition(QueryCondition.name("oppositeidentifystatus").in(oppositeidentifystatuss));
                }
                //业务对接人
                if (ObjectUtils.isNotEmpty(busscounterpart)) {
                    conditionGroup.addCondition(QueryCondition.name("busscounterpart").eq(busscounterpart));
                }
                //所属组织
                if (ObjectUtils.isNotEmpty(orgids)) {
                    conditionGroup.addCondition(QueryCondition.name("orgid").in(orgids));
                }
                // 银行交易流水号
                if (ObjectUtils.isNotEmpty(banksqlno)) {
                    conditionGroup.addCondition(QueryCondition.name("bank_seq_no").eq(banksqlno));
                }
                // 摘要匹配，模糊查询
                String remark = params.getString("remark");
                if (!StringUtils.isEmpty(remark)) {
                    conditionGroup.addCondition(QueryCondition.name("remark").like(remark));
                }
                //用途，模糊查询
                if (ObjectUtils.isNotEmpty(params.getString("use_name"))) {
                    conditionGroup.addCondition(QueryCondition.name("use_name").like(params.getString("use_name")));
                }
                //对方类型
                if (ObjectUtils.isNotEmpty(params.getString("oppositetype"))) {
                    conditionGroup.addCondition(QueryCondition.name("oppositetype").eq(params.getString("oppositetype")));
                }
                //数据来源
                if (ObjectUtils.isNotEmpty(params.getString("dataOrigin"))) {
                    conditionGroup.addCondition(QueryCondition.name("dataOrigin").eq(params.getString("dataOrigin")));
                }
                //发布对象
                if (ObjectUtils.isNotEmpty(params.getString("publishedType"))) {
                    conditionGroup.addCondition(QueryCondition.name("published_type").eq(params.getString("publishedType")));
                }
                //是否直联
                if (ObjectUtils.isNotEmpty(params.getString("cashDirectLink"))) {
                    conditionGroup.addCondition(QueryCondition.name("cashDirectLink").eq(params.getString("cashDirectLink")));
                }
                //是否入境
                if (ObjectUtils.isNotEmpty(params.getString("entercountry"))) {
                    conditionGroup.addCondition(QueryCondition.name("entercountry").eq(params.getString("entercountry")));
                }
                //退票状态
                if (ObjectUtils.isNotEmpty(params.getString("refundstatus"))) {
                    conditionGroup.addCondition(QueryCondition.name("refundstatus").eq(params.getString("refundstatus")));
                }
                //流水处理完结状态
                if (ObjectUtils.isNotEmpty(params.getString("serialdealendstate"))) {
                    conditionGroup.addCondition(QueryCondition.name("serialdealendstate").eq(params.getString("serialdealendstate")));
                }
                //流水认领处理方式
                if (ObjectUtils.isNotEmpty(params.getString("serialdealtype"))) {
                    conditionGroup.addCondition(QueryCondition.name("serialdealtype").eq(params.getString("serialdealtype")));
                }

                // 根据前端不同页签，拼接不同的筛选条件
                if (ObjectUtils.isNotEmpty(cmpTableTabsActiveKey) && "1".equals(cmpTableTabsActiveKey)) {
                    // 使用组织
                    conditionGroup.addCondition(QueryCondition.name("accentity").is_not_null());
                } else if (ObjectUtils.isNotEmpty(cmpTableTabsActiveKey) && "2".equals(cmpTableTabsActiveKey)) {
                    //使用组织
                    if (CollectionUtils.isNotEmpty(accentitys)){
                        conditionGroup.addCondition(QueryCondition.name("accentity").in(accentitys));
                    }
                }
                // 已结束（流水完结状态为已完结）
                long endedCount = getBankRecEndedCount(conditionGroup);

                // 发布处理中（流水完结状态为已完结 && 发布状态=已发布）
                long publishingCount = getBankRecPublishingCount(conditionGroup);

                // 待处理
                long confirmedCount = getBankRecConfirmedCount(conditionGroup);

                //使用组织待确认（业务关联状态=未关联 || 业务关联状态=已关联 && 入账类型 = 挂账） && 发布状态=未发布 && 使用组织状态=未确认）
                long confirmingCount = getBankRecConfirmingCount(conditionGroup);
                //疑重: 认领单的疑重标识为 1
                long repetitionCount = getBillClaimRepetitionCount(conditionGroup);
                // 全部
                if (ObjectUtils.isEmpty(isrepeat)) {
                    QueryConditionGroup repeatGroup = QueryConditionGroup.or(QueryCondition.name("isrepeat").is_not_null(), QueryCondition.name("isrepeat").is_null());
                    conditionGroup.addCondition(repeatGroup);
                }
                long allCount = PageUtils.queryCount(conditionGroup, BankReconciliation.ENTITY_NAME);
                map.put("repetition", repetitionCount);//疑重
                map.put("pending", confirmedCount);//待处理
                map.put("confirmed", confirmedCount);//使用组织已确认
                map.put("confirming", confirmingCount);//使用组织待确认
                map.put("publishing", publishingCount);//发布处理中
                map.put("ended", endedCount);//已结束
                map.put("all", allCount);//全部
            }
        }
        return map;
    }

    /**
     * 已结束
     * 本页签查询的银行账户流水，“账户使用组织不为“空”。
     * 且1、查询条件中的“账户使用组织”=流水中的“账户使用组织”
     * 且2、流水处理完结状态=已完结
     * （即收付单据关联状态=已关联且入账类型='正常入账'且待认领金额等于0
     * 或收付单据关联状态=已关联且入账类型='冲挂账'且待认领金额等于0
     * 或收付单据关联状态=已关联 且 流水认领处理方式=收付单据关联）
     * @param conditionGroup 查询条件组
     * @return
     * @throws Exception
     */
    public long getBankRecEndedCount(QueryConditionGroup conditionGroup) throws Exception {
        QueryConditionGroup endCountConditionGroup = new QueryConditionGroup();
        // 当查询条件中包含疑重标识时，数据应该返回为空
        if(conditionGroup.conditions().stream().anyMatch(i->i.toString().contains("isrepeat"))){
            QueryConditionGroup repeatGroup = QueryConditionGroup.or(QueryCondition.name("isrepeat").in(BankDealDetailConst.REPEAT_INIT, BankDealDetailConst.REPEAT_NORMAL), QueryCondition.name("isrepeat").is_null());
            endCountConditionGroup.addCondition(repeatGroup);
        }
        endCountConditionGroup.addCondition(conditionGroup);
        endCountConditionGroup.addCondition(QueryCondition.name(ICmpConstant.SERIAL_DEAL_END_STATE).eq(SerialdealendState.END.getValue()));
        return PageUtils.queryCount(endCountConditionGroup, BankReconciliation.ENTITY_NAME);
    }

    /**
     * 发布处理中
     * 本页签查询的银行账户流水，包含“账户使用组织为“空”。
     * 且1、查询条件中的“账户所属组织”=流水中的“账户所属组织” （用户有权限的组织作为所属组织）
     * 且2、是否发布=是
     * 且3.1、流水处理完结状态=未完结（①即收付单据关联状态=未关联 ②收付单据关联状态=已关联且入账类型='冲挂账'且待认领金额不等于0 ③收付单据关联状态=已关联且入账类型='正常入账'且待认领金额不等于0）
     * @param conditionGroup 查询条件组
     * @return
     * @throws Exception
     */
    public long getBankRecPublishingCount(QueryConditionGroup conditionGroup) throws Exception {
        QueryConditionGroup publishingCountConditionGroup = new QueryConditionGroup();
        if(conditionGroup.conditions().stream().anyMatch(i->i.toString().contains("isrepeat"))){
            QueryConditionGroup repeatGroup = QueryConditionGroup.or(QueryCondition.name("isrepeat").in(BankDealDetailConst.REPEAT_INIT, BankDealDetailConst.REPEAT_NORMAL), QueryCondition.name("isrepeat").is_null());
            publishingCountConditionGroup.addCondition(repeatGroup);
        }
        publishingCountConditionGroup.addCondition(conditionGroup);
        publishingCountConditionGroup.addCondition(QueryCondition.name("ispublish").eq(true));
        publishingCountConditionGroup.addCondition(QueryCondition.name("serialdealendstate").eq(0));
        return PageUtils.queryCount(publishingCountConditionGroup, BankReconciliation.ENTITY_NAME);
    }

    /**
     * 待处理
     * 本页签查询的银行账户流水，“账户使用组织不为“空”。
     * 且1、查询条件中的“账户使用组织”=流水中的“账户使用组织”
     * 且2、是否发布=否
     * 且3、流水处理完结状态=未完结（①即收付单据关联状态=未关联 ②收付单据关联状态=已关联且入账类型='冲挂账'且待认领金额不等于0 ③收付单据关联状态=已关联且入账类型='正常入账'且待认领金额不等于0）
     * 且4、退票状态非“疑似退票”，且自动化处理状态为“空”或自动化处理状态='自动关联业务凭据待确认'
     * @param conditionGroup 查询条件组
     * @return
     * @throws Exception
     */
    public long getBankRecConfirmedCount(QueryConditionGroup conditionGroup) throws Exception {
        QueryConditionGroup publishingCountConditionGroup = new QueryConditionGroup();
        if(conditionGroup.conditions().stream().anyMatch(i->i.toString().contains("isrepeat"))){
            QueryConditionGroup repeatGroup = QueryConditionGroup.or(QueryCondition.name("isrepeat").in(BankDealDetailConst.REPEAT_INIT, BankDealDetailConst.REPEAT_NORMAL), QueryCondition.name("isrepeat").is_null());
            publishingCountConditionGroup.addCondition(repeatGroup);
        }
        publishingCountConditionGroup.addCondition(QueryCondition.name("accentity").not_eq(null));
        publishingCountConditionGroup.addCondition(conditionGroup);
        publishingCountConditionGroup.addCondition(QueryCondition.name("ispublish").eq(false));
        publishingCountConditionGroup.addCondition(QueryCondition.name("serialdealendstate").eq(0));
        return PageUtils.queryCount(publishingCountConditionGroup, BankReconciliation.ENTITY_NAME);
    }

    /**
     * 使用组织待确认
     * 页签包含的数据：
     * 1、查询条件中的“账户所属组织”=流水中的“账户所属组织”（用户有权限的组织作为所属组织）
     * 且2、使用组织为空
     * 且3、是否发布=否
     * @param conditionGroup
     * @return
     * @throws Exception
     */
    public long getBankRecConfirmingCount(QueryConditionGroup conditionGroup) throws Exception {
        QueryConditionGroup confirmingCountConditionGroup = new QueryConditionGroup();
        if(conditionGroup.conditions().stream().anyMatch(i->i.toString().contains("isrepeat"))){
            QueryConditionGroup repeatGroup = QueryConditionGroup.or(QueryCondition.name("isrepeat").in(BankDealDetailConst.REPEAT_INIT, BankDealDetailConst.REPEAT_NORMAL), QueryCondition.name("isrepeat").is_null());
            confirmingCountConditionGroup.addCondition(repeatGroup);
        }
        confirmingCountConditionGroup.addCondition(conditionGroup);
        confirmingCountConditionGroup.addCondition(QueryCondition.name("ispublish").eq(false));
        confirmingCountConditionGroup.addCondition(QueryCondition.name("accentity").eq(null));
        return PageUtils.queryCount(confirmingCountConditionGroup, BankReconciliation.ENTITY_NAME);
    }

    private long getBillClaimRepetitionCount(QueryConditionGroup conditionGroup) throws Exception {
        QueryConditionGroup queryConditionGroup = new QueryConditionGroup();
        queryConditionGroup.addCondition(conditionGroup);
        queryConditionGroup.addCondition(QueryCondition.name("isrepeat").eq(BankDealDetailConst.REPEAT_DOUBT));
        return PageUtils.queryCount(queryConditionGroup, BankReconciliation.ENTITY_NAME);
    }
}
