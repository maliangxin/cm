package com.yonyoucloud.fi.cmp.bankreceipt.service;

import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.bankelectronicreceipt.BankElectronicReceipt;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.billclaim.BillClaimItem;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.common.CommonRequestDataVo;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.receipt.BankObjectParam;

import com.yonyoucloud.fi.cmp.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * desc:银行交易回单联查根据银行交易回单，如果传过来的银行对账单主键有值直接查询银行对账单关联的回单，如果没有根据6要素直接查询匹配的回单
 * * 6要素：收付方向、本方银行账号、对方银行账号、对方户名、金额、摘要
 * {"querydate":[{
 * "id":"银行对账单主键",
 * "dc_flag":"借贷方向 1借 2贷",
 * "enterpriseBankAccount":"本方银行账号",
 * "to_acct_no":"对方银行账号",
 * "to_acct_name":"对方户名",
 * "tran_amt":"金额",
 * "remark":"摘要"
 * },{
 * "id":"银行对账单主键",
 * "dc_flag":"借贷方向 1借 2贷",
 * "enterpriseBankAccount":"本方银行账号",
 * "to_acct_no":"对方银行账号",
 * "to_acct_name":"对方户名",
 * "tran_amt":"金额",
 * "remark":"摘要"
 * }]
 * }
 * author:wangqiangac
 * date:2023/5/19 13:28
 */
@Service
@Slf4j
public class BankReceiptLinkServiceImpl implements BankReceiptLinkService {
    /**
     * 根据params中的银行对账单主键，或者6要素查询匹配的银行回单
     *
     * @param params
     * @return
     */
    @Override
    public List<Map<String, Object>> queryMathData(CtmJSONObject params) throws Exception {
        CtmJSONArray queryDate = params.getJSONArray("querydate");
        List<Map<String, Object>> result = new ArrayList<>();
        Set<Long> idList = new HashSet<>();
        for (int i = 0; i < queryDate.size(); i++) {
            CtmJSONObject data = queryDate.getJSONObject(i);
            if (data.get("id") != null && data.get("id") != "") {
                Long id = Long.parseLong((String) data.get("id"));//银行对账单主键
                idList.add(id);
            } else {
                this.queryBankElectronicReceiptBySixFactor(result, data);
            }
        }
        this.queryBankElectronicReceiptByIds(result, idList);
        return result;
    }
    @Override
    public List<Map<String, Object>> queryMathData(CommonRequestDataVo params) throws Exception {
        List<BankObjectParam> queryDate =  params.getQueryData();
        List<Map<String, Object>> result = new ArrayList<>();
        Set<Long> idList = new HashSet<>();
        for (int i = 0; i < queryDate.size(); i++) {
            BankObjectParam data = queryDate.get(i);
            if (data.getId() != null && !StringUtils.isEmpty( data.getId())) {
                Long id = Long.parseLong(data.getId());//银行对账单主键
                idList.add(id);
            } else {
                this.queryBankElectronicReceiptBySixFactor(result, data);
            }
        }
        this.queryBankElectronicReceiptByIds(result, idList);
        return result;
    }

    /**
     * 根据传入的id（银行对账单主键或者我的认领主键）查询对应的银行回单主键
     * 电子回单的接口需要加个入参。现在通过银行对账单认领生单的下游单据记录了银行对账单ID；但是通过认领中心认领生成的下游单据，只给下游传递了认领单的ID，所以下游只能传递给咱们认领单的ID，然后咱们根据认领单的ID，查找对应的银行对账单ID
     *
     * @param result
     * @param idList
     * @throws Exception
     */
    private void queryBankElectronicReceiptByIds(List<Map<String, Object>> result, Set<Long> idList) throws Exception {
        if (idList != null && idList.size() > 0) {

            //我的认领查询认领单明细子表子表 cmp_billclaim_item   主表id mainid  银行对账单  bankbill
            QuerySchema claimQs = QuerySchema.create().addSelect(" bankbill ");
            QueryConditionGroup claimConditionGroup = new QueryConditionGroup();
            claimConditionGroup.addCondition(QueryCondition.name("mainid").in(idList));
            claimQs.addCondition(claimConditionGroup);
            List<Map<String, Object>> claimItems = MetaDaoHelper.query(BillClaimItem.ENTITY_NAME, claimQs, null);
            if (claimItems != null && claimItems.size() > 0) {
                List<Long> bankBills = claimItems.stream().map(e -> (Long)e.get("bankbill")).collect(Collectors.toList());
                idList.addAll(bankBills);
            }
            QuerySchema qs = QuerySchema.create().addSelect(" id , extendss");
            QueryConditionGroup conditionGroup = new QueryConditionGroup();
            conditionGroup.addCondition(QueryCondition.name("bankreconciliationid").in(idList));
            qs.addCondition(conditionGroup);
            List<BankElectronicReceipt> bankElectronicReceipts = MetaDaoHelper.queryObject(BankElectronicReceipt.ENTITY_NAME, qs, null);
            if (bankElectronicReceipts != null && bankElectronicReceipts.size() > 0) {
                for(BankElectronicReceipt bankElectronicReceipt : bankElectronicReceipts){
                    HashMap<String, Object> map = new HashMap<>();
                    map.put("id", bankElectronicReceipt.get("id"));
                    map.put("extendss", bankElectronicReceipt.get("extendss"));
                    result.add(map);
                }
            }

        }
    }
    /**
     * 通过6要素查询对应的银行回单主键
     *
     * @param result
     * @param data
     * @throws Exception
     */
    private void queryBankElectronicReceiptBySixFactor(List<Map<String, Object>> result, BankObjectParam data) throws Exception {
        short dc_flag;
        QuerySchema qs = QuerySchema.create().addSelect(" id , extendss ");
        QueryConditionGroup conditionGroup = new QueryConditionGroup();
        if (data.getDc_flag() != null && !StringUtils.isEmpty(data.getDc_flag())) {
            dc_flag = (short) Integer.parseInt(data.getDc_flag());//借贷方向 1借 2贷
            conditionGroup.addCondition(QueryCondition.name("dc_flag").eq(dc_flag));
        }
        String enterpriseBankAccount = data.getEnterpriseBankAccount();//本方银行账号
        String to_acct_no = data.getTo_acct_no();//对方银行账号
        String to_acct_name = data.getTo_acct_name();//对方户名
        if (data.getTran_amt() != null && !"".equals(data.getTran_amt()+"")) {
            BigDecimal tran_amt = data.getTran_amt();//金额
            conditionGroup.addCondition(QueryCondition.name("tran_amt").eq(tran_amt));
        }
        String remark = data.getRemark();//摘要
        conditionGroup.addCondition(QueryCondition.name("enterpriseBankAccount").eq("".equals(enterpriseBankAccount) ? null : enterpriseBankAccount));
        conditionGroup.addCondition(QueryCondition.name("to_acct_no").eq("".equals(to_acct_no) ? null : to_acct_no));
        conditionGroup.addCondition(QueryCondition.name("to_acct_name").eq("".equals(to_acct_name) ? null : to_acct_name));
        conditionGroup.addCondition(QueryCondition.name("remark").eq("".equals(remark) ? null : remark));
        qs.addCondition(conditionGroup);
        List<BankElectronicReceipt> bankElectronicReceipt = MetaDaoHelper.queryObject(BankElectronicReceipt.ENTITY_NAME, qs, null);
        if (bankElectronicReceipt != null && bankElectronicReceipt.size() > 0) {
            HashMap<String, Object> map = new HashMap<>();
            map.put("id", bankElectronicReceipt.get(0).get("id"));
            map.put("fileid", bankElectronicReceipt.get(0).get("extendss"));
            result.add(map);
        }
    }
    /**
     * 通过6要素查询对应的银行回单主键
     *
     * @param result
     * @param data
     * @throws Exception
     */
    private void queryBankElectronicReceiptBySixFactor(List<Map<String, Object>> result, CtmJSONObject data) throws Exception {
        short dc_flag;
        QuerySchema qs = QuerySchema.create().addSelect(" id , extendss ");
        QueryConditionGroup conditionGroup = new QueryConditionGroup();
        if (data.get("dc_flag") != null && data.get("dc_flag") != "") {
            dc_flag = (short) Integer.parseInt((String) data.get("dc_flag"));//借贷方向 1借 2贷
            conditionGroup.addCondition(QueryCondition.name("dc_flag").eq(dc_flag));
        }
        String enterpriseBankAccount = (String) data.get("enterpriseBankAccount");//本方银行账号
        String to_acct_no = (String) data.get("to_acct_no");//对方银行账号
        String to_acct_name = (String) data.get("to_acct_name");//对方户名
        if (data.get("tran_amt") != null && data.get("tran_amt") != "") {
            BigDecimal tran_amt = new BigDecimal((String) data.get("tran_amt"));//金额
            conditionGroup.addCondition(QueryCondition.name("tran_amt").eq(tran_amt));
        }
        String remark = (String) data.get("remark");//摘要
        conditionGroup.addCondition(QueryCondition.name("enterpriseBankAccount").eq("".equals(enterpriseBankAccount) ? null : enterpriseBankAccount));
        conditionGroup.addCondition(QueryCondition.name("to_acct_no").eq("".equals(to_acct_no) ? null : to_acct_no));
        conditionGroup.addCondition(QueryCondition.name("to_acct_name").eq("".equals(to_acct_name) ? null : to_acct_name));
        conditionGroup.addCondition(QueryCondition.name("remark").eq("".equals(remark) ? null : remark));
        qs.addCondition(conditionGroup);
        List<BankElectronicReceipt> bankElectronicReceipt = MetaDaoHelper.queryObject(BankElectronicReceipt.ENTITY_NAME, qs, null);
        if (bankElectronicReceipt != null && bankElectronicReceipt.size() > 0) {
            HashMap<String, Object> map = new HashMap<>();
            map.put("id", bankElectronicReceipt.get(0).get("id"));
            map.put("fileid", bankElectronicReceipt.get(0).get("extendss"));
            result.add(map);
        }
    }

     /**
     *
     * 根据银行对账单主键查询交易流水号再根据交易流水号查询银行回单主键
     * @author maliangn
     * 请使用api包下类进行替换
     * @deprecated (use com.yonyoucloud.fi.cmp.bankreceipt.service.BankReceiptLinkServiceImpl queryBankElectronicReceiptByIds instead)
     */
     @Deprecated
    private void queryBankElectronicReceiptById(List<Map<String, Object>> result, List<Long> idList) throws Exception {
        if (idList != null && idList.size() > 0) {
            List<Map<String, Object>> bankReconciliationList = MetaDaoHelper.queryByIds(BankReconciliation.ENTITY_NAME, "bank_seq_no", idList.toArray(new Long[]{}));
            List<String> bank_seq_nos = bankReconciliationList.stream().map(e -> (String) e.get("bank_seq_no")).collect(Collectors.toList());
            if (bank_seq_nos != null && bank_seq_nos.size() > 0) {
                QuerySchema qs = QuerySchema.create().addSelect(" id , extendss");
                QueryConditionGroup conditionGroup = new QueryConditionGroup();
                conditionGroup.addCondition(QueryCondition.name("bankseqno").in(bank_seq_nos));
                qs.addCondition(conditionGroup);
                List<BankElectronicReceipt> bankElectronicReceipt = MetaDaoHelper.queryObject(BankElectronicReceipt.ENTITY_NAME, qs, null);
                if (bankElectronicReceipt != null && bankElectronicReceipt.size() > 0) {
                    HashMap<String, Object> map = new HashMap<>();
                    map.put("id", bankElectronicReceipt.get(0).get("id"));
                    map.put("extendss", bankElectronicReceipt.get(0).get("extendss"));
                    result.add(map);
                }
            }
        }
    }
}
