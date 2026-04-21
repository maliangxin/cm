package com.yonyoucloud.fi.cmp.checkStockApply.rule;

import com.yonyou.iuap.billcode.service.IBillCodeSupport;
import com.yonyou.uap.billcode.BillCodeComponentParam;
import com.yonyou.uap.billcode.BillCodeObj;
import com.yonyou.uap.billcode.service.IBillCodeComponentService;
import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVO;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.bpm.model.VerifyState;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.checkStockApply.service.CheckStockApplyService;
import com.yonyoucloud.fi.cmp.checkstock.CheckStock;
import com.yonyoucloud.fi.cmp.checkstockapply.CheckStockApply;
import com.yonyoucloud.fi.cmp.checkstockapply.CmpBusiType;
import com.yonyoucloud.fi.cmp.checkstockapply.RecCheckTemp;
import com.yonyoucloud.fi.cmp.cmpentity.AuditStatus;
import com.yonyoucloud.fi.cmp.cmpentity.GenerationType;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.util.AssertUtils;
import com.yonyoucloud.fi.cmp.util.CmpBillCodeMappingConfUtils;
import com.yonyoucloud.fi.cmp.util.DateUtils;
import com.yonyoucloud.fi.cmp.util.StringUtils;
import com.yonyoucloud.fi.cmp.util.basedoc.EnterpriseBankQueryService;
import com.yonyoucloud.fi.cmp.util.business.CmpCommonUtil;
import org.apache.commons.lang3.ObjectUtils;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 支票入库
 */
@Component("checkStockAddBeforRule")
public class CheckStockAddBeforRule extends AbstractCommonRule {

    @Autowired
    private EnterpriseBankQueryService enterpriseBankQueryService;
    @Autowired
    CheckStockApplyService checkStockService;


    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        List<BizObject> bills = getBills(billContext, map);
        for (BizObject obj : bills) {
            Integer stockNum;
            String startNo = obj.get("startNo");
            String endNo = obj.get("endNo");
            //交易类型停用校验
            CmpCommonUtil.checkTradeTypeEnable(obj.get("tradetype"));
            BillDataDto billDataDto = (BillDataDto) getParam(map);
            boolean importFlag = "import".equals(billDataDto.getRequestAction());
            boolean openApiFlag = obj.containsKey("_fromApi") && obj.get("_fromApi").equals(true);
            boolean fromapi = billDataDto.getFromApi();
            if (importFlag || openApiFlag || fromapi) {
                // 导入校验
                importCheck(obj);
                importValue(obj);
            }
            Short chequeType = obj.get("chequeType");
            if (CmpBusiType.Black.getValue() == chequeType) {// 空白支票
                String code = obj.get("code");
                if (StringUtils.isEmpty(code)) {
                    code = getBillCode(obj);
                }

                String checkBookNo = obj.get("checkBookNo");
                String account = obj.get("account");
                stockNum = obj.get("stockNum");
                //银行类别
                EnterpriseBankAcctVO bankaccts1 = enterpriseBankQueryService.findByIdAndEnable(account);
                Object bank = null;
                if (bankaccts1!=null) {
                    bank = bankaccts1.getBank();
                }
                if (StringUtils.isEmpty(startNo) || StringUtils.isEmpty(endNo)) {
                    //起始编号和结束编号不能为空！
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101043"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180089","请选择单据！") /* "请选择单据！" */);
                }
                /* 存在相同的起始编号和终止编号或者起始编号=终止编号 */
                QuerySchema queryCheckStockApplySchema = QuerySchema.create().addSelect("*");
                QueryConditionGroup group = QueryConditionGroup.and(
                        QueryCondition.name("ytenantId").eq(AppContext.getYTenantId()),
                        QueryCondition.name("endNo").in(startNo, endNo),
                        QueryCondition.name("code").not_eq(code),
                        QueryConditionGroup.or(QueryCondition.name("startNo").eq(startNo),
                                QueryCondition.name("ytenantId").eq(AppContext.getYTenantId()),
                                QueryCondition.name("code").not_eq(code))

                );
                queryCheckStockApplySchema.addCondition(group);
                List<Map<String, Object>> checkStockApplys = MetaDaoHelper.query(CheckStockApply.ENTITY_NAME, queryCheckStockApplySchema);
                if (checkStockApplys.size() > 0) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101044"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18905AFC0450001E","保存失败！存在相同的起始编号和终止编号或起始编号等于终止编号。") /* "保存失败！存在相同的起始编号和终止编号或起始编号等于终止编号。" */);
                }

                //支票簿编号重复校验
                QuerySchema querySchema = QuerySchema.create().addSelect("checkBookNo","account");
                QueryConditionGroup checkBookGroup = QueryConditionGroup.and(
                        QueryCondition.name("ytenantId").eq(AppContext.getYTenantId()),
                        QueryCondition.name("checkBookNo").eq(checkBookNo),
                        QueryCondition.name("code").not_eq(code));
                querySchema.addCondition(checkBookGroup);
                List<Map<String, Object>> checkBookNos = MetaDaoHelper.query(CheckStockApply.ENTITY_NAME,querySchema);
                if (checkBookNos.size() > 0) {
                    List<Object> existBankaccts1 = new ArrayList<>();
                    for (Map<String,Object> checkBook : checkBookNos){
                        QuerySchema bankacctQuery = QuerySchema.create().addSelect("bank");
                        QueryConditionGroup bankacctGroup = QueryConditionGroup.and(
                                QueryCondition.name("ytenant").eq(AppContext.getYTenantId()),
                                QueryCondition.name("id").eq(checkBook.get("account")));
                        bankacctQuery.addCondition(bankacctGroup);
                        EnterpriseBankAcctVO bankaccts = enterpriseBankQueryService.findByIdAndEnable(account);
                        if (bankaccts != null) {
                            existBankaccts1.add(bankaccts.getBank());
                        }
                    }
                    if (ObjectUtils.isNotEmpty(bank) && existBankaccts1.contains(bank)) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101046"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_182BA4D804280009", "已存在相同的支票簿编号+银行类别，不允许保存。") /* "已存在相同的支票簿编号，不允许保存。" */);
                    }
                }
                String[] startSplit = startNo.replaceAll("\\D", ",").split(",");
                String start = startSplit[startSplit.length - 1];
                String[] endSplit = endNo.replaceAll("\\D", ",").split(",");
                String end = endSplit[endSplit.length - 1];
                BigInteger bigStart = new BigInteger(start);
                BigInteger bigEnd = new BigInteger(end);
                BigInteger num = bigEnd.subtract(bigStart).add(new BigInteger("1"));
                if (num.compareTo(new BigInteger("1")) == -1 || num.compareTo(new BigInteger("200")) == 1) {
                    //张数不能超过200或小于1！
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101043"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180089","请选择单据！") /* "请选择单据！" */);
                }
                /* 已生成支票工作台单据的入库单不允许保存  */
                QuerySchema queryCheckStockSchema = QuerySchema.create().addSelect("checkBillNo");
                List<String> checkBillNos = getCheckBillNos(startNo, num.intValue());
                queryCheckStockSchema.addCondition(QueryConditionGroup.and(QueryCondition.name("checkBillNo").in(checkBillNos)));
                List<Map<String, Object>> checkStocks = MetaDaoHelper.query(CheckStock.ENTITY_NAME, queryCheckStockSchema);
                if (checkStocks.size() > 0 ) { // 按范围查找，找到对应的支票数据，则无法保存
                    StringBuffer sb = new StringBuffer();
                    checkStocks.stream().forEach(checkStock -> sb.append(" " + checkStock.get("checkBillNo").toString() + ","));
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101045"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A050800DE", "保存失败！存在已生成的支票工作台单据,支票编号:") /* "保存失败！存在已生成的支票工作台单据,支票编号:" */ + sb.toString());
                }
                if (Objects.isNull(stockNum)) {
                    stockNum = num.intValue();
                }
                if (null == stockNum) {
                    //页面数据错误，请重新输入
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101043"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180089","请选择单据！") /* "请选择单据！" */);
                }
                if (Objects.isNull(obj.get("stockNum"))) {
                    obj.set("stockNum", stockNum);
                }
                BigInteger stockBigNum = new BigInteger(stockNum.toString());
                if (num.compareTo(stockBigNum) != 0) {
                    //页面数据错误，请重新输入
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101043"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180089","请选择单据！") /* "请选择单据！" */);
                }
            } else {// 收入支票
                CheckStockApply checkStockApply = (CheckStockApply) obj;
                List<RecCheckTemp> tempList = obj.get("RecCheckTemp");
                Map checkBillNoMap = new HashMap();// 支票入库子表，支票编号暂存集合
                Map whiteMap = new HashMap();// 删除的子表编号，可以不进行数据库校验
                if (tempList != null && tempList.size() > 0) {
                    for (RecCheckTemp temp : tempList) {
                        if (EntityStatus.Delete.equals(temp.get("_status"))) {
                            String tempCheckBillNo = temp.getCheckBillNo();
                            whiteMap.put(tempCheckBillNo, null);
                        }
                    }
                    for (RecCheckTemp temp : tempList) {
                        // 新增保存，跟自己比较重复
                        String checkBillNo = temp.getCheckBillNo();
                        if (checkBillNoMap.containsKey(checkBillNo) ) {
                            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101047"),checkBillNo + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180088","支票编号重复") /* "支票编号重复" */);
                        } else {
                            if (whiteMap.containsKey(checkBillNo)) {
                                whiteMap.remove(checkBillNo);
                                continue;
                            } else {
                                checkBillNoMap.put(checkBillNo, null);
                            }
                        }
                        // 编辑保存，与数据库比较，子表为insert进行比较，update不能修改支票编号
                        if (EntityStatus.Insert.equals(temp.get("_status")) && !whiteMap.containsKey(checkBillNo)) {
                            QuerySchema querySchema = QuerySchema.create().addSelect("id,mainid,checkBillNo");
                            QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("checkBillNo").eq(checkBillNo));
                            group.addCondition(QueryConditionGroup.and(QueryCondition.name("mainid").not_eq(temp.getMainid())));
                            querySchema.addCondition(group);
                            List existList = MetaDaoHelper.queryObject(RecCheckTemp.ENTITY_NAME, querySchema, null);
                            if (existList.size() > 0) {
                                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101047"),checkBillNo + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180088","支票编号重复") /* "支票编号重复" */);
                            }
                        }
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                        LocalDate applyDate = LocalDate.parse(DateUtils.formatDate(checkStockApply.getVouchdate()),formatter);
                        LocalDate drawerDate = LocalDate.parse(DateUtils.formatDate(temp.getDrawerDate()),formatter);
                        if (drawerDate.isAfter(applyDate)) {
                            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101048"),checkBillNo + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418008A","出票日期不能大于入库日期") /* "出票日期不能大于入库日期" */);
                        }
                    }
                }
            }
        }
        return new RuleExecuteResult();
    }


    /**
     * 导入校验
     * @param obj
     */
    private void importCheck(BizObject obj){
        CheckStockApply checkStockApply = (CheckStockApply) obj;
        // 空白支票必填
        if (ICmpConstant.CMP_CHECKSTOCK_BLANKCHEQUE.equals(obj.get(ICmpConstant.TRADETYPE_CODE))){
            AssertUtils.isTrue(Objects.isNull(checkStockApply.getStartNo()),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-UI_1C032FC404780008","起始编号不能为空") /* "起始编号不能为空" */);
            AssertUtils.isTrue(Objects.isNull(checkStockApply.getEndNo()),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-UI_1C03300605080005","终止编号不能为空") /* "终止编号不能为空" */);
            AssertUtils.isTrue(Objects.isNull(checkStockApply.getCheckBookNo()),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-UI_1C03303A04780006","支票簿编号不能为空") /* "支票簿编号不能为空" */);
            AssertUtils.isTrue(Objects.isNull(checkStockApply.getChequeType()),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-UI_1C03307204780001","支票类型标识不能为空") /* "支票类型标识不能为空" */);
            AssertUtils.isTrue(Objects.isNull(obj.get("acctNo")),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-UI_1C0330C805080008","银行账号不能为空") /* "银行账号不能为空" */);
        } else {
            AssertUtils.isTrue(Objects.isNull(obj.get(ICmpConstant.CURRENCY_CODE)),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-UI_1C0330F805080001","币种编号不能为空") /* "币种编号不能为空" */);
            AssertUtils.isTrue(Objects.isNull(obj.get("RecCheckTemp")),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-UI_1C03312C04780008","子表不能为空") /* "子表不能为空" */);
            AssertUtils.isTrue(Objects.isNull(checkStockApply.getChequeType()),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-UI_1C03307204780001","支票类型标识不能为空") /* "支票类型标识不能为空" */);
            List<RecCheckTemp> tempList = obj.get("RecCheckTemp");
            for (RecCheckTemp tempCheck : tempList) {
                AssertUtils.isTrue(Objects.isNull(tempCheck.getDrawerAcctNo()),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-UI_1C03317A04780005","交易类型是收入支票时必填") /* "交易类型是收入支票时必填" */);
                AssertUtils.isTrue(Objects.isNull(tempCheck.get(ICmpConstant.PAYBANKNAME)),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-UI_1C03317A04780005","交易类型是收入支票时必填") /* "交易类型是收入支票时必填" */);
                AssertUtils.isTrue(Objects.isNull(tempCheck.getDrawerDate()),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-UI_1C03317A04780005","交易类型是收入支票时必填") /* "交易类型是收入支票时必填" */);
                AssertUtils.isTrue(Objects.isNull(tempCheck.getAmount()),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-UI_1C03317A04780005","交易类型是收入支票时必填") /* "交易类型是收入支票时必填" */);
            }
        }
    }


    /**
     * 导入赋值
     * @param obj
     */
    private void importValue(BizObject obj) {
        obj.set("generationType", GenerationType.ManualInput.getValue());
        obj.set("billType", ICmpConstant.CHECKAPPLYBILLTYPE);
        obj.set("auditstatus", AuditStatus.Incomplete.getValue());
        obj.set("verifystate", VerifyState.INIT_NEW_OPEN.getValue());
        obj.set("org", obj.get("accentity"));
    }

    /**
     * 获取支票编号
     * @param startNo
     * @param stockNum
     * @return
     */
    private List<String> getCheckBillNos(String startNo, int stockNum) {
        List<String> checkBillNos = new ArrayList<>();
        for (int i = 0; i < stockNum; i++) {
            String[] split = startNo.replaceAll("\\D", ",").split(",");
            String end = split[split.length - 1];
            BigInteger num = new BigInteger(end);
            BigInteger iBig = new BigInteger(String.valueOf(i));
            String val = String.valueOf(num.add(iBig));
            if (startNo.length() >= val.length()) {
                String begin = startNo.substring(0, startNo.length() - val.length());
                checkBillNos.add(begin + val);
            } else {
                checkBillNos.add(val);
            }
        }
        return checkBillNos;
    }


    /**
     * 获取重空凭证入库单据编码
     * @param
     * @return
     */
    private String getBillCode(BizObject obj){
        IBillCodeComponentService billCodeComponentService = AppContext.getBean(IBillCodeComponentService.class);
        String ytenantId = AppContext.getBean(IBillCodeSupport.class).getYtenantId();
        BillCodeComponentParam billCodeComponentParam = new BillCodeComponentParam(CmpBillCodeMappingConfUtils.getBillCode(IBillNumConstant.CMP_CHECKSTOCKAPPLY),IBillNumConstant.CMP_CHECKSTOCKAPPLY,ytenantId,null,null,new BillCodeObj[]{new BillCodeObj(obj)});
        String[] codelist = billCodeComponentService.getBatchBillCodes(billCodeComponentParam);
        if(codelist!=null && codelist.length>0){
            return codelist[0];
        }else {
            return null;
        }
    }

}
