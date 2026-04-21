package com.yonyoucloud.fi.cmp.ctmrpc;

import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.security.signature.CtmSignatureService;
import com.yonyoucloud.fi.cmp.accountrealtimebalance.AccountRealtimeBalanceService;
import com.yonyoucloud.fi.cmp.api.openapi.OpenApiService;
import com.yonyoucloud.fi.cmp.initdata.InitData;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.api.common.CtmcmpCommonQueryRpcService;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.common.CommonRequestDataVo;
import com.yonyoucloud.fi.cmp.paymentbill.service.PaymentService;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import lombok.extern.slf4j.Slf4j;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

import static com.yonyou.yonbip.ctm.error.CommonCtmErrorCode.ILLEGAL_ARGUMENT;
import static com.yonyou.yonbip.ctm.error.CommonCtmErrorCode.REMOTE_SERVICE_REST_EXCEPTION;

/**
 *  客开使用：国机
 *  领域内使用：账户管理
 */
@Service
@Slf4j
public class CtmcmpCommonQueryRpcServiceImpl implements CtmcmpCommonQueryRpcService {

    @Autowired
    private OpenApiService openApiService;
    @Autowired
    private PaymentService paymentService;
    @Autowired
    private CtmSignatureService signatureService;
    @Autowired
    private AccountRealtimeBalanceService AccountRealtimeBalanceService;

    @Autowired
    AccountRealtimeBalanceService accountRealtimeBalanceService;
    /**
     * <h2>根据账号查询期初数据</h2>
     *
     * @param param: 入参
     * @return void
     * @author Sun GuoCai
     * @since 2021/4/1 17:05
     */
    @Override
    public String queryInitDataByAccountNoNew(CommonRequestDataVo param) throws Exception {
        try {
            String account = param.getAccount();
            String accentity = param.getAccentity();
            if (!ValueUtils.isNotEmptyObj(account)) {
                return ResultMessage.error(ILLEGAL_ARGUMENT.build(), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180299","根据账号查询期初数据失败!银行账号不能为空！") /* "根据账号查询期初数据失败!银行账号不能为空！" */);
            }
            String currency = param.getCurrency();
            QuerySchema querySchema = QuerySchema.create().addSelect("cobookoribalance,cobooklocalbalance,currency");
            QueryConditionGroup condition = new QueryConditionGroup();
            condition.addCondition(QueryConditionGroup.or(QueryCondition.name("cashaccount").eq(account),
                    QueryCondition.name("bankaccount").eq(account)));
            if (ValueUtils.isNotEmptyObj(currency)) {
                condition.addCondition(QueryConditionGroup.and(QueryCondition.name("currency").eq(currency)));
            }
            if (ValueUtils.isNotEmptyObj(accentity)) {
                condition.addCondition(QueryConditionGroup.and(QueryCondition.name("accentity").eq(accentity)));
            }
            querySchema.addCondition(condition);
            List<Map<String, Object>> mapList = MetaDaoHelper.query(InitData.ENTITY_NAME, querySchema);
            return ResultMessage.data(mapList);
        } catch (Exception e) {
            return ResultMessage.error(REMOTE_SERVICE_REST_EXCEPTION.build(), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180297","根据账号查询期初数据失败!") /* "根据账号查询期初数据失败!" */);
        }
    }

    @Override
    public String queryRealbalanceBalanceNew(CommonRequestDataVo dataVo) throws Exception {
        CtmJSONObject params = CtmJSONObject.parseObject(CtmJSONObject.toJSONString(dataVo));
        try {
            return CtmJSONObject.toJSONString(accountRealtimeBalanceService.queryRealbalanceBalanceNew(params));
        } catch (Exception e) {
            log.error("调用现金rpc接口查询企业银行账户实时余额：" + e.getMessage(), e);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102608"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180556","账户实时余额查询错误：") /* "账户实时余额查询错误：" */+ e.getMessage());
        }
    }

    @Override
    public List<Map<String, Object>> queryBatchInitData(List<Map<String, Object>> param) throws Exception {
        try {
            return openApiService.queryBatchInitData(param);
        } catch (Exception e) {
            log.error("调用现金批量查询期初余额失败：" + e.getMessage(), e);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102609"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180554","调用现金批量查询期初余额失败") /* "调用现金批量查询期初余额失败" */);
        }
    }

    @Override
    public List<Map<String, Object>> queryRealtimeBalance(List<Map<String, Object>> param) throws Exception {
        try {
            return openApiService.queryRealtimeBalance(param);
        } catch (Exception e) {
            log.error("调用现金查询银行实时余额失败：" + e.getMessage(), e);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102610"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1811A04805B0002E", "调用现金查询银行实时余额失败") /* "调用现金查询银行实时余额失败" */);
        }
    }
}
