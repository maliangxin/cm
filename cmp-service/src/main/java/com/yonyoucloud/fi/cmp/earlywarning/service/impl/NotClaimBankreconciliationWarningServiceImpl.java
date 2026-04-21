package com.yonyoucloud.fi.cmp.earlywarning.service.impl;

import com.yonyou.iuap.org.dto.FinOrgDTO;
import com.yonyou.ucf.basedoc.model.CurrencyTenantDTO;
import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVO;
import com.yonyou.ucf.basedoc.model.rpcparams.EnterpriseParams;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.filter.util.StringUtil;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
//import com.yonyoucloud.fi.basecom.service.ref.OrgRpcService;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.cmpentity.AssociationStatus;
import com.yonyoucloud.fi.cmp.cmpentity.EntryType;
import com.yonyoucloud.fi.cmp.earlywarning.service.NotClaimBankreconciliationWarningService;
import com.yonyoucloud.fi.cmp.migrade.CmpNewFiMigradeUtilService;
import com.yonyoucloud.fi.cmp.util.AccentityUtil;
import com.yonyoucloud.fi.cmp.util.DateUtils;
import com.yonyoucloud.fi.cmp.util.TaskUtils;
import com.yonyoucloud.fi.cmp.vo.BankNoVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Slf4j
public class NotClaimBankreconciliationWarningServiceImpl implements NotClaimBankreconciliationWarningService {
    //@Autowired
//    //private OrgRpcService orgRpcService;
    @Autowired
    private BaseRefRpcService baseRefRpcService;
    @Autowired
    CmpNewFiMigradeUtilService cmpNewFiMigradeUtilService;

    //最大超期天数
    public static int MAXDAY = 35;

    @Override
    public Map<String, Object> notClaimBankreconciliationWarning(CtmJSONObject body,String logId) {
        CtmJSONObject result = new CtmJSONObject();
        List<BankNoVO> dataArray = new ArrayList<>();
        String errmsg = new String();
        //所属组织
        String accentitys = body.getString("accentity");
        //银行类别
        String banktype = body.getString("banktype");
        //币种currency
        String currency = body.getString("currency");
        //银行账号
        String bankaccount = body.getString("bankaccount");
        //日期范围（前X日）
        Integer checkRange = body.getInteger("checkRange");
        //超时天数
        Integer timeOuts = body.getInteger("timeOuts");

        String[] accentityArr = null;
        if (StringUtil.isNotEmpty(accentitys)) {
            accentityArr = accentitys.split(";");
        }
        String[] banktypeArr = null;
        if (StringUtil.isNotEmpty(banktype)) {
            banktypeArr = banktype.split(";");
        }
        String[] currencyArr = null;
        if (StringUtil.isNotEmpty(currency)) {
            currencyArr = currency.split(";");
        }

        String[] bankaccountArr = null;
        if (StringUtil.isNotEmpty(bankaccount)) {
            bankaccountArr = bankaccount.split(";");
        }

        int status = TaskUtils.TASK_BACK_SUCCESS;
        try{
            List<BankReconciliation> billClaimList = queryUnclaimedDate(accentityArr,banktypeArr,currencyArr,bankaccountArr,checkRange,timeOuts);
            if(CollectionUtils.isNotEmpty(billClaimList)){
                //通过接口查询所属单位warnPrimaryOrgName、银行账号account、开户行(银行网点)bankNumber_name、币种currencyName、银行交易流水号bank_seq_no、交易日期tran_date、金额tran_amt
                Set<String> accentityIdList = new HashSet<>();
                Set<String> bankaccountIdList = new HashSet<>();
                Set<String> currencyIdList = new HashSet<>();
                for(BankReconciliation vo : billClaimList){
                    accentityIdList.add(vo.getOrgid());
                    bankaccountIdList.add(vo.getBankaccount());
                    currencyIdList.add(vo.getCurrency());
                }
                //查询会计主体
                List<FinOrgDTO> orgList = new ArrayList<>();
                Map<String,String> accentityMap = new HashMap<>();
                if(!accentityIdList.isEmpty()){
                    List<List<String>>  groupDataList= cmpNewFiMigradeUtilService.groupData(new ArrayList<>(accentityIdList), 50);
                    for(List<String> idListnow : groupDataList){
                        orgList.addAll(AccentityUtil.getFinOrgDTOByAccentityIds(idListnow));
                    }
                    for(FinOrgDTO vo : orgList){
                        accentityMap.put(vo.getId(),vo.getName());
                    }
                }
                //查询银行账号
                List<EnterpriseBankAcctVO> enterpriseBankAcctList = new ArrayList<>();
                Map<String,String> enterpriseBankAcctMap = new HashMap<>();
                //银行网点
                Map<String,String> bankNumber_nameMap = new HashMap<>();
                if(!bankaccountIdList.isEmpty()){
                    List<List<String>>  groupDataList= cmpNewFiMigradeUtilService.groupData(new ArrayList<>(bankaccountIdList), 50);
                    for(List<String> idListnow : groupDataList){
                        EnterpriseParams params = new EnterpriseParams();
                        params.setIdList(idListnow);
                        enterpriseBankAcctList.addAll(baseRefRpcService.queryEnterpriseBankAccountByCondition(params));
                    }
                    for(EnterpriseBankAcctVO vo : enterpriseBankAcctList){
                        enterpriseBankAcctMap.put(vo.getId(),vo.getAccount());
                        //银行网点名称 用账户id作为key
                        bankNumber_nameMap.put(vo.getId(),vo.getBankNumberName());
                    }
                }
                //查询币种
                List<CurrencyTenantDTO> currencyList = new ArrayList();
                Map<String,String> currencyNameMap = new HashMap<>();
                if(!currencyIdList.isEmpty()){
                    List<List<String>>  groupDataList= cmpNewFiMigradeUtilService.groupData(new ArrayList<>(currencyIdList), 50);
                    for(List<String> idListnow : groupDataList){
                        currencyList.addAll(baseRefRpcService.queryCurrencyByIds(idListnow));
                    }
                    for(CurrencyTenantDTO vo : currencyList){
                        currencyNameMap.put(vo.getId(),vo.getName());
                    }
                }

                //循环数据 拼接告警信息
                for(BankReconciliation vo : billClaimList){
                    //通过接口查询所属单位warnPrimaryOrgName、银行账号account、开户行(银行网点)bankNumber_name、币种currencyName、银行交易流水号bank_seq_no、交易日期tran_date、金额tran_amt
                    BankNoVO bankNoVO = new BankNoVO();
                    bankNoVO.setWarnPrimaryOrgName(accentityMap.get(vo.getOrgid()));
                    bankNoVO.setAccount(enterpriseBankAcctMap.get(vo.getBankaccount()));
                    bankNoVO.setBankNumber_name(bankNumber_nameMap.get(vo.getBankaccount()));
                    bankNoVO.setCurrencyName(currencyNameMap.get(vo.getCurrency()));
                    bankNoVO.setBank_seq_no(vo.getBank_seq_no());
                    bankNoVO.setTran_date(DateUtils.dateFormat(vo.getTran_date(),null));
                    bankNoVO.setTran_amt(vo.getTran_amt().toString());
                    dataArray.add(bankNoVO);
                }
            }else{
                errmsg = com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_186A3EEE04C0000E", "无数据") /* "无数据" */;
            }
        }catch(Exception e){
            // 任务执行结果 0-失败
            status = TaskUtils.TASK_BACK_FAILURE;
            log.error("BankRelationWarningTask error, e = {}", e.getMessage());
            errmsg = e.getMessage();
        }finally {
            log.error("BankRelationWarningTask, status = {}, logId = {}, content = {}", status, logId, dataArray);
            result.put("status", status);//执行结果： 0：失败；1：成功
            result.put("data", dataArray);//业务方自定义结果集字段
            result.put("checkRange", checkRange);//日期范围
            result.put("timeOuts", timeOuts);//超时天数
            result.put("errmsg", errmsg);//异常信息
            AppContext.clear();
        }
        return result;
    }




    private List<BankReconciliation> queryUnclaimedDate(String[] accentityArr, String[] banktypeArr, String[] currencyArr, String[] bankaccountArr,
                                                        Integer checkRange, Integer timeOuts) throws Exception {
        QuerySchema querySchema = new QuerySchema().addSelect("*");
        QueryConditionGroup queryConditionGroup = new QueryConditionGroup();
        if (timeOuts <= checkRange) {
            if(checkRange>MAXDAY){
                checkRange = MAXDAY;
            }
            if(timeOuts>MAXDAY){
                timeOuts = MAXDAY;
            }
            //交易日期范围（前X日内）
            String beforeDate = DateUtils.dateFormat(DateUtils.dateAddDays(DateUtils.getNow(), -1 * checkRange), DateUtils.pattern);
            queryConditionGroup.addCondition(QueryConditionGroup.and(QueryCondition.name("tran_date").egt(beforeDate)));
            //超过几天未完成关联的(timeOuts前已认领)
            String timeOutDate = DateUtils.dateFormat(DateUtils.dateAddDays(DateUtils.getNow(), -1 * timeOuts), DateUtils.pattern);
            queryConditionGroup.addCondition(QueryConditionGroup.and(QueryCondition.name("tran_date").elt(timeOutDate)));
        } else {
            return null;
        }
        if(accentityArr!=null){
            queryConditionGroup.addCondition(QueryConditionGroup.and(QueryCondition.name("orgid").in(accentityArr)));
        }
        if(banktypeArr!=null){
            queryConditionGroup.addCondition(QueryConditionGroup.and(QueryCondition.name("banktype").in(banktypeArr)));
        }
        if(currencyArr!=null){
            queryConditionGroup.addCondition(QueryConditionGroup.and(QueryCondition.name("currency").in(currencyArr)));
        }
        if(bankaccountArr!=null){
            queryConditionGroup.addCondition(QueryConditionGroup.and(QueryCondition.name("bankaccount").in(bankaccountArr)));
        }

        //已发布且认领金额为0 或 未发布的数据且未关联 或 对账单入账类型为挂账
        //已发布且认领金额为0(金额可能为0 或 null)
        QueryConditionGroup queryOrIspublishAndAmountZero = new QueryConditionGroup();
        queryOrIspublishAndAmountZero.addCondition(QueryConditionGroup.and(QueryCondition.name("ispublish").eq(true),QueryCondition.name("claimamount").in(0,null)));
        //未发布的数据且未关联
        QueryConditionGroup queryOrNotpublishAndNoAssociated = new QueryConditionGroup();
        queryOrNotpublishAndNoAssociated.addCondition(QueryConditionGroup.and(QueryCondition.name("ispublish").eq(false),QueryCondition.name("associationstatus").eq(AssociationStatus.NoAssociated.getValue())));
        //对账单入账类型为挂账 且 未发布
        QueryConditionGroup queryOrHang_Entry = new QueryConditionGroup();
        queryOrHang_Entry.addCondition(QueryConditionGroup.and(QueryCondition.name("entrytype").eq(EntryType.Hang_Entry.getValue()),QueryConditionGroup.and(QueryCondition.name("ispublish").eq(false))));
        //对账单入账类型=“冲挂账”且认领金额=“0 且 已发布
        QueryConditionGroup queryOrCrushHang_EntryAndAmountZero = new QueryConditionGroup();
        queryOrCrushHang_EntryAndAmountZero.addCondition(QueryConditionGroup.and(QueryCondition.name("entrytype").eq(EntryType.CrushHang_Entry.getValue()),
                QueryCondition.name("claimamount").in(0,null),QueryCondition.name("ispublish").eq(true)));

        queryConditionGroup.addCondition
                (QueryConditionGroup.or(queryOrIspublishAndAmountZero,queryOrNotpublishAndNoAssociated,queryOrHang_Entry,queryOrCrushHang_EntryAndAmountZero));
        querySchema.addCondition(queryConditionGroup);
        return MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME, querySchema, null);
    }

}
