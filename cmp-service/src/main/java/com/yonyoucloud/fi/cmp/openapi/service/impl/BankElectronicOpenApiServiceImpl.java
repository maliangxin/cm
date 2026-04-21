package com.yonyoucloud.fi.cmp.openapi.service.impl;

import cn.hutool.core.util.PageUtil;
import com.yonyou.ucf.mdd.ext.bill.rule.util.BillInfoUtils;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.bankelectronicreceipt.BankElectronicReceipt;
import com.yonyoucloud.fi.cmp.constant.IBussinessConstant;
import com.yonyoucloud.fi.cmp.openapi.service.BankElectronicOpenApiService;
import com.yonyoucloud.fi.cmp.openapi.vo.OpenApiResultVo;
import com.yonyoucloud.fi.cmp.oss.OSSPoolClient;
import com.yonyoucloud.fi.cmp.util.file.CooperationFileUtilService;
import com.yonyoucloud.fi.cmp.vo.Result;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QueryOrderby;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @description: 银行交易回单openapi 具体实现
 * @author: wanxbo@yonyou.com
 * @date: 2023/2/23 15:01
 */

@Service
@Slf4j
public class BankElectronicOpenApiServiceImpl implements BankElectronicOpenApiService {
    @Autowired
    private OSSPoolClient ossPoolClient;// oss客户端
    @Autowired
    private CooperationFileUtilService cooperationFileUtilService;

    @Override
    public Result<Object> queryDownloadUrl(CtmJSONObject param) throws Exception {
        return null;
    }

    @Override
    public CtmJSONObject querylist(CtmJSONObject param) throws Exception {
        String creatBegintime = param.getString("creat_begintime");
        String creatEndtime = param.getString("creat_endtime");
        if (!com.yonyoucloud.fi.cmp.util.StringUtils.isEmpty(creatBegintime) && !com.yonyoucloud.fi.cmp.util.StringUtils.isEmpty(creatEndtime)){
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date beginTime = sdf.parse(creatBegintime);
            Date endTime = sdf.parse(creatEndtime);
            if (beginTime.after(endTime)) {
                // creat_begintime 在 creat_endtime 之后
                throw  new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054004CA", "创建时间（截止）需晚于创建时间（开始），请检查!") /* "创建时间（截止）需晚于创建时间（开始），请检查!" */);
            }
        }
        if (com.yonyoucloud.fi.cmp.util.StringUtils.isEmpty(creatBegintime) && !com.yonyoucloud.fi.cmp.util.StringUtils.isEmpty(creatEndtime)){
            throw  new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054004C9", "创建时间(开始)与创建时间(结束)需同时有值或为空!") /* "创建时间(开始)与创建时间(结束)需同时有值或为空!" */);
        }
        if (!com.yonyoucloud.fi.cmp.util.StringUtils.isEmpty(creatBegintime) && com.yonyoucloud.fi.cmp.util.StringUtils.isEmpty(creatEndtime)){
            throw  new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054004C9", "创建时间(开始)与创建时间(结束)需同时有值或为空!") /* "创建时间(开始)与创建时间(结束)需同时有值或为空!" */);
        }
        QueryConditionGroup group = new QueryConditionGroup(ConditionOperator.and);
        //根据api查询条件拼装
        //借贷方向
        if (param.getInteger("dc_flag") != null) {
            group.addCondition(QueryCondition.name("dc_flag").eq(param.getInteger("dc_flag")));
        }
        //回单编号
        if (param.getString("receiptno") != null) {
            group.addCondition(QueryCondition.name("receiptno").eq(param.getString("receiptno")));
        }
        //银行交易流水号
        if (param.getString("bank_seq_no") != null) {
            group.addCondition(QueryCondition.name("bankseqno").eq(param.getString("bank_seq_no")));
        }
        //会计主体
        if (param.getString("accentity") != null) {
            group.addCondition(QueryCondition.name("accentity").eq(param.getString("accentity")));
        } else { //未传id，则根据会计主体编码查询
            if (param.getString("accentity_code") != null) {
                group.addCondition(QueryCondition.name("accentity.code").eq(param.getString("accentity_code")));
            }
        }
        //币种
        if (param.getString("currency") != null) {
            group.addCondition(QueryCondition.name("currency").eq(param.getString("currency")));
        } else { //未传id，则根据币种名称
            if (param.getString("currency_name") != null) {
                group.addCondition(QueryCondition.name("currency.name").eq(param.getString("currency_name")));
            }
        }
        //交易日期
        if (param.getString("begin_date") != null) {
            group.addCondition(QueryCondition.name("tranDate").egt(param.getString("begin_date")));
        }
        if (param.getString("end_date") != null) {
            group.addCondition(QueryCondition.name("tranDate").elt(param.getString("end_date")));
        }
        if(param.getString("creat_begintime") != null && param.getString("creat_endtime") != null){
            group.addCondition(QueryCondition.name("createTime").between(creatBegintime,creatEndtime));
        }
        //
        if (param.getString("account_number") != null){
            List<String> list = Arrays.stream(param.getString("account_number").split(","))
                    .map(String::trim) // 去除字符串首尾的空格
                    .collect(Collectors.toList());
            group.addCondition(QueryCondition.name("bankcheckcode").in(list));
        }
        //本方银行账户
        if (param.getString("bankaccount") != null) {
            group.addCondition(QueryCondition.name("enterpriseBankAccount").eq(param.getString("bankaccount")));
        } else { //未传id，则根据本行银行账户的账号
            if (param.getString("bankaccount_account") != null) {
                group.addCondition(QueryCondition.name("enterpriseBankAccount.account").eq(param.getString("bankaccount_account")));
            }
        }
        //是否存在url
        if (param.getBooleanValue("resulthasUrl")) {
            group.addCondition(QueryCondition.name("extendss").is_not_null());
        }
        // 权限控制
        Set<String> orgsSet = BillInfoUtils.getOrgPermissions("cmp_bankelectronicreceiptlist");
        if(orgsSet!=null && orgsSet.size()>0) {
            String[] orgs = orgsSet.toArray(new String[orgsSet.size()]);
            group.appendCondition(QueryCondition.name(IBussinessConstant.ACCENTITY).in(orgs));
        }

        int size = getTotalCount(group);
        int pageNum = PageUtil.totalPage(size, param.getInteger("pageSize"));
        //查询相关数据
        List<Map<String, Object>> infoMapList = queryData(group,param);
        if (infoMapList == null || infoMapList.size() == 0) {
            CtmJSONObject result = new CtmJSONObject();
            result.put("code", 200);
            result.put("message", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054004CB", "未查询到银行交易回单") /* "未查询到银行交易回单" */);
            return result;
        }
        //解析查询数据
        List<CtmJSONObject> resultList = analysisQueryData(infoMapList,param);
        //组装返回值
        OpenApiResultVo vo = OpenApiResultVo.builder().code(200).message("success").resultList(resultList).totalCount(size).pageSize(pageNum).build();
        CtmJSONObject result = buildResult(vo);
        return result;
    }

    //查询数据总量
    private int getTotalCount(QueryConditionGroup group) throws Exception {
        QuerySchema queryCount = QuerySchema.create().addSelect("count(id)");
        queryCount.addCondition(group);
        List<Map<String, Object>> infoMapListTotal = MetaDaoHelper.query(BankElectronicReceipt.ENTITY_NAME, queryCount);
        int count = Integer.valueOf(infoMapListTotal.get(0).get("count").toString());
        return count;
    }

    //查询相关数据
    private List<Map<String, Object>> queryData(QueryConditionGroup group,CtmJSONObject param) throws Exception {
        QuerySchema querySchema = QuerySchema.create().addSelect("id,accentity,accentity.code as accentity_code,accentity.name as accentity_name," +
                "currency,currency.name as currency_name,currency.code as currency_code,enterpriseBankAccount as bankaccount,enterpriseBankAccount.acctName as bankaccount_acctName,enterpriseBankAccount.account as bankaccount_account," +
                "receiptno,bankseqno as bank_seq_no,tranDate as tran_date,tranTime as tran_time,dc_flag,tran_amt,to_acct_no,to_acct_name,to_acct_bank,to_acct_bank_name,use_name,remark,extendss,bankcheckcode ");
        querySchema.addCondition(group);
        querySchema.addPager(param.getInteger("pageIndex"), param.getInteger("pageSize"));
        //对账单日期倒序
        querySchema.addOrderBy(new QueryOrderby("tranDate", "desc"));
        querySchema.addOrderBy(new QueryOrderby("id","asc"));
        List<Map<String, Object>> infoMapList = MetaDaoHelper.query(BankElectronicReceipt.ENTITY_NAME, querySchema);
        return infoMapList;
    }


    //解析查询出的数据
    private List<CtmJSONObject> analysisQueryData(List<Map<String, Object>> infoMapList,CtmJSONObject param) throws Exception {
        //是否返回文件流信息 默认为true
        Boolean resultByte = param.get("resultByte")!=null?param.getBooleanValue("resultByte"):true;
        List<CtmJSONObject> resultList = new ArrayList<>();
        for (Map<String, Object> b : infoMapList) {
            if (b.get("extendss") != null && !StringUtils.isEmpty((String) b.get("extendss"))) {
                String extendss = b.get("extendss").toString();
                //银企下载的数据
                if (extendss.contains("/")) {
                    byte[] bytes = ossPoolClient.download(extendss);
                    //回单pdf，请求返回流数据，下载对应的文件
                    b.put("bankpdf", bytes);
                } else {
                    String url = cooperationFileUtilService.queryprivilegeRealDownloadUrl(extendss);
                    //回单文件存储路径链接
                    b.put("url", url);
                    if(resultByte){
                        byte[] bytes = cooperationFileUtilService.queryBytesbyFileid(extendss);
                        //回单pdf，请求返回流数据，下载对应的文件
                        b.put("bankpdf", bytes);
                    }
                }
            }
            CtmJSONObject r = CtmJSONObject.parseObject(CtmJSONObject.toJSONString(b));
            resultList.add(r);
        }

        return resultList;
    }

    //构建api返回结果
    private CtmJSONObject buildResult(OpenApiResultVo vo){
        CtmJSONObject data = new CtmJSONObject();
        data.put("recordList", vo.getResultList());
        data.put("totalCount",vo.getTotalCount());
        data.put("pageNum",vo.getPageSize());

        CtmJSONObject result = new CtmJSONObject();
        result.put("code",vo.getCode());
        result.put("message",vo.getMessage());
        result.put("data", data);
        return result;
    }


}
