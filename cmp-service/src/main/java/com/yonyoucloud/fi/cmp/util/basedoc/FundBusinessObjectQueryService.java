package com.yonyoucloud.fi.cmp.util.basedoc;

import com.yonyou.ucf.basedoc.model.BankVO;
import com.yonyou.ucf.basedoc.model.BankdotVO;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.cmp.constant.IDomainConstant;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 该类主要用于查询资金业务对象信息
 */
@Service
public class FundBusinessObjectQueryService {


    @Autowired
    BaseRefRpcService baseRefRpcService;

    public CtmJSONObject queryFundBusinessObjectDataById(String id, String account, String accountname) throws Exception {
        CtmJSONObject fundBusinObjArchivesItem = new CtmJSONObject();
        BillContext context = new BillContext();
        context.setFullname("tmsp.fundbusinobjarchives.FundBusinObjArchives");
        context.setDomain(IDomainConstant.YONBIP_FI_CTMPUB);
        QuerySchema schema = QuerySchema.create();
        schema.addSelect("id as oppositeobjectid, fundbusinobjtypename as oppositeobjectname, " +
                "fundbusinobjtypeid,fundBusinObjArchivesItem.benabled as benabled, " +
                "fundBusinObjArchivesItem.isdefaultaccount as isdefaultaccount, fundBusinObjArchivesItem.bbankid as bbankid, " +
                "fundBusinObjArchivesItem.bopenaccountbankid as bopenaccountbankid, fundBusinObjArchivesItem.id as id, " +
                "fundBusinObjArchivesItem.bankaccount as bankaccount,fundBusinObjArchivesItem.accountname as accountname ," +
                "fundBusinObjArchivesItem.linenumber as linenumber, fundBusinObjArchivesItem.bbankAccountId as bbankAccountId,fundBusinObjArchivesItem.currency as currency");

        schema.appendQueryCondition(QueryCondition.name("id").eq(id));
        if (ValueUtils.isNotEmptyObj(account)){
            schema.appendQueryCondition(QueryCondition.name("fundBusinObjArchivesItem.bankaccount").eq(account));
        }
        if (ValueUtils.isNotEmptyObj(accountname)){
            schema.appendQueryCondition(QueryCondition.name("fundBusinObjArchivesItem.accountname").eq(accountname));
        }
        if (!ValueUtils.isNotEmptyObj(account) && !ValueUtils.isNotEmptyObj(accountname)){
            schema.appendQueryCondition(QueryCondition.name("fundBusinObjArchivesItem.isdefaultaccount").eq(Boolean.TRUE));
        }
        schema.appendQueryCondition(QueryCondition.name("fundBusinObjArchivesItem.benabled").eq(Boolean.TRUE));

//        log.info("getObjectContent, schema = {}", schema);
        List<Map<String, Object>> result = MetaDaoHelper.query(context, schema);

        //获取账户信息对象
        if (org.apache.commons.collections4.CollectionUtils.isNotEmpty(result)) {
            //获取数据实体
            CtmJSONObject jsonObject = CtmJSONObject.toJSON(result.get(0));
            //查询银行类别
            String bbankid = jsonObject.getString("bbankid");
            BankVO bankVO = baseRefRpcService.queryBankTypeById(bbankid);
            fundBusinObjArchivesItem.put("bbankid_name", bankVO.getName());

            fundBusinObjArchivesItem.put("id", jsonObject.get("id"));
            fundBusinObjArchivesItem.put("bankaccount", jsonObject.get("bankaccount"));
            fundBusinObjArchivesItem.put("accountname", jsonObject.get("accountname"));
            fundBusinObjArchivesItem.put("linenumber", jsonObject.get("linenumber"));
            fundBusinObjArchivesItem.put("fundbusinobjtypeid", jsonObject.get("fundbusinobjtypeid"));
            fundBusinObjArchivesItem.put("oppositeobjectid",  ValueUtils.isNotEmptyObj(jsonObject.get("oppositeobjectid"))
                    ? String.valueOf(jsonObject.get("oppositeobjectid")): jsonObject.get("oppositeobjectid"));
            fundBusinObjArchivesItem.put("oppositeobjectname", jsonObject.get("oppositeobjectname"));
            fundBusinObjArchivesItem.put("bbankAccountId", jsonObject.get("bbankAccountId"));
            fundBusinObjArchivesItem.put("bbankid", jsonObject.get("bbankid"));
            fundBusinObjArchivesItem.put("bopenaccountbankid", jsonObject.get("bopenaccountbankid"));
            fundBusinObjArchivesItem.put("currency", jsonObject.get("currency"));

            //查询银行网点
            String bopenaccountbankid = jsonObject.getString("bopenaccountbankid");
            if (ValueUtils.isNotEmptyObj(bopenaccountbankid)){
                BankdotVO bankdotVO = baseRefRpcService.queryBankdotVOByBanddotId(bopenaccountbankid);
                fundBusinObjArchivesItem.put("bopenaccountbankid_name", bankdotVO.getName());
            }
            return fundBusinObjArchivesItem;
        }
        return new CtmJSONObject();
    }



}
