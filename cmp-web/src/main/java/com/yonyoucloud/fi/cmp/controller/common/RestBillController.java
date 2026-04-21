package com.yonyoucloud.fi.cmp.controller.common;

import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.ucf.basedoc.service.RefCheckService;
import com.yonyou.ucf.basedoc.service.biz.UcfBasedocRefCheckServiceImpl;
import com.yonyou.ucf.basedoc.util.CallBackParam;
import com.yonyou.ucf.mdd.ext.base.BaseController;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.dao.sql.SqlHelper;
import com.yonyou.yonbip.ctm.security.log.LogForgingFilter;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.paybill.PayBill;
import com.yonyoucloud.fi.cmp.receivebill.ReceiveBill;
import com.yonyoucloud.fi.cmp.util.CTMAuthHttpClientUtils;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import lombok.extern.slf4j.Slf4j;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author maliang
 * @version V1.0
 * @date 2021/5/25 20:42
 * @Copyright yonyou
 */
@Controller
@RequestMapping("/rest")
@Slf4j
public class RestBillController extends BaseController {

    private static final String UCFBASEDOC_REF_CHECK_DOMAIN = "ucfbasedoc_ref_all_doamin";
    /* value */
    private static final int VAL_LOGIC_DELETED = 1;
    private static final int VAL_LOGIC_UNDEL = 0;
    /* 规则- 接口- filedname */
    private static final String RULE_INTERFACE_FIELD_LOGIC_DEL = "dr";
    private static final String KEY_SPLIT = "_";
    private static final String customerFlag = "bd.basedocdef.CustomerDocVO";

    @RequestMapping(value = "/receivebillvstatus", method = RequestMethod.POST)
    public void updateBillVoucher(HttpServletRequest request, HttpServletResponse response){

        String sourceID = request.getParameter("sourceID");
        String eventType = request.getParameter("eventType");
        String userObject = request.getParameter("userObject");
        Map vouderInfo = CtmJSONObject.parseObject (userObject);
        log.error("#### receiveBillStatus, request paramMap={}", userObject);
        Map<String, String> res = new HashMap<>();
        //  生成凭证回写凭证状态
        try {
            BizObject bizObject = MetaDaoHelper.findById(ReceiveBill.ENTITY_NAME, vouderInfo.get("id").toString(), 1);
            //先用id查  如果查不到在用srcbillid再查一下
            if (!ValueUtils.isNotEmptyObj(bizObject) && vouderInfo.get("srcbillid") != null) {
                bizObject = MetaDaoHelper.findById(ReceiveBill.ENTITY_NAME, vouderInfo.get("srcbillid").toString(), 1);
            }
            if (!ValueUtils.isNotEmptyObj(bizObject)){
                res.put("success", "true");
                renderJson(response, CtmJSONObject.toJSONString(res));
                return;
            }
            Map<String, Object> params = new HashMap<>();
            params.put("tableName", IBillNumConstant.RECEIVE_BILL);
            params.put("id", vouderInfo.get("id").toString());
            params.put("voucherStatus", vouderInfo.get("voucherStatus"));
            params.put("ytenantId", vouderInfo.get("ytenantId"));//友互通id
            params.put("voucherNo", vouderInfo.get("voucherno"));
            params.put("voucherPeriod", vouderInfo.get("period"));
            SqlHelper.update("com.yonyoucloud.fi.cmp.voucher.updateVoucherStatusBySrcbillid", params);
            if (ValueUtils.isNotEmptyObj(vouderInfo.get("voucherno"))) {
                Map<String, Object> params1 = new HashMap<>();
                CtmJSONObject jsonObj = CtmJSONObject.parseObject(vouderInfo.get("voucherno").toString());
                params1.put("voucherNo", jsonObj.get("zh_CN"));
                params1.put("id", vouderInfo.get("id").toString());
                params1.put("voucherPeriod", vouderInfo.get("period"));
                params.put("yTenantId", vouderInfo.get("ytenantId"));//友互通id
                SqlHelper.update("com.yonyoucloud.fi.cmp.voucher.updateVoucherNoOfJournal", params1);
            }
        }catch (Exception e){
            res.put("success", "false");
            log.error("update receive workbench voucherStatus and voucherNo and period, params={}", vouderInfo);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100420"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805AE","同步单据状态失败！") /* "同步单据状态失败！" */);
        }
        res.put("success", "true");
        renderJson(response, CtmJSONObject.toJSONString(res));
    }


    /**
     * @author liuttm
     * @version V1.0
     * @date 2021/5/25 20:42
     * @Copyright yonyou
     */
    @RequestMapping(value = "/paybillvstatus", method = RequestMethod.POST)
    public void updateBillVoucherPay(HttpServletRequest request, HttpServletResponse response){
        String sourceID = request.getParameter("sourceID");
        String eventType = request.getParameter("eventType");
        String userObject = request.getParameter("userObject");
        Map vouderInfo = CtmJSONObject.parseObject(userObject);
        log.info("#### payBillStatus, request paramMap={}", LogForgingFilter.filter(userObject));
        Map<String, String> res = new HashMap<>();
        //  生成凭证回写凭证状态
        try {
            BizObject bizObject = MetaDaoHelper.findById(PayBill.ENTITY_NAME, vouderInfo.get("id").toString(), 1);
            if (!ValueUtils.isNotEmptyObj(bizObject)){
                res.put("success", "true");
                renderJson(response, CtmJSONObject.toJSONString(res));
                return;
            }
            Map<String, Object> params = new HashMap<>();
            params.put("tableName", "cmp_paybill");
            params.put("id", vouderInfo.get("id").toString());
            params.put("voucherStatus", vouderInfo.get("voucherStatus"));
            params.put("voucherNo", vouderInfo.get("voucherno"));
            params.put("voucherPeriod", vouderInfo.get("period"));
            params.put("ytenantId", vouderInfo.get("ytenantId"));//友互通id
            SqlHelper.update("com.yonyoucloud.fi.cmp.voucher.updateVoucherStatusBySrcbillid", params);
            if (ValueUtils.isNotEmptyObj(vouderInfo.get("voucherno"))) {
                Map<String, Object> params1 = new HashMap<>();
                CtmJSONObject jsonObj = CtmJSONObject.parseObject(vouderInfo.get("voucherno").toString());
                params1.put("voucherNo", jsonObj.get("zh_CN"));
                params1.put("id", vouderInfo.get("id").toString());
                params1.put("voucherPeriod", vouderInfo.get("period"));
                params.put("yTenantId", vouderInfo.get("ytenantId"));//友互通id
                SqlHelper.update("com.yonyoucloud.fi.cmp.voucher.updateVoucherNoOfJournal", params1);
            }
        }catch (Exception e){
            res.put("success", "false");
            log.error("update payment workbench voucherStatus and voucherNo and period, params={}", vouderInfo);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100420"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805AE","同步单据状态失败！") /* "同步单据状态失败！" */);
        }
        res.put("success", "true");
        renderJson(response, CtmJSONObject.toJSONString(res));
    }


    /**
     * 通过srcbillid查询表单
     *
     * @param ids
     * @return
     * @throws Exception
     */
    public List<ReceiveBill> getReceiveBillBySrcbillIds(String[] ids) throws Exception {
        QuerySchema querySchema = QuerySchema.create().addSelect("*,ReceiveBill_b.*");
        QueryConditionGroup queryConditionGroup = new QueryConditionGroup(ConditionOperator.and);
        queryConditionGroup.appendCondition(QueryCondition.name("srcbillid").in(ids));
        querySchema.addCondition(queryConditionGroup);
        return MetaDaoHelper.queryObject(ReceiveBill.ENTITY_NAME, querySchema, null);
    }

    /**
     * 通过srcbillid查询表单
     *
     * @param ids
     * @return
     * @throws Exception
     */
    public List<PayBill> getPayBillBySrcbillIds(String[] ids) throws Exception {
        QuerySchema querySchema = QuerySchema.create().addSelect("*,PayBillb.*");
        QueryConditionGroup queryConditionGroup = new QueryConditionGroup(ConditionOperator.and);
        queryConditionGroup.appendCondition(QueryCondition.name("srcbillid").in(ids));
        querySchema.addCondition(queryConditionGroup);
        return MetaDaoHelper.queryObject(PayBill.ENTITY_NAME, querySchema, null);
    }


    @ResponseBody
    @RequestMapping(value = "/refevent", method = RequestMethod.POST)
    public Object userEvent(HttpServletRequest request) {
        CtmJSONObject result = new CtmJSONObject();
        try {
            doFilter(request);
            String userObject = request.getParameter("userObject");
            Map<String, Object> businessObject = CtmJSONObject.parseObject(userObject, HashMap.class);
            if (businessObject != null && !businessObject.isEmpty()) {
//                String domain = AppContext.getEnvConfig("refcheck.domain");这里取配置会有错误
                String domain = "ctm-cmp";
                log.error("--------------------------------------------------------> 处理删除引用监听事件domain打印:" + domain);
                String fullName = businessObject.get("fullname") == null ? null : businessObject.get("fullname").toString();
                String tenantId = businessObject.get("tenantId") == null ? null : businessObject.get("tenantId").toString();
                String fullnameExt = businessObject.get("fullnameExt") == null ? null : businessObject.get("fullnameExt").toString();
                // 参数可以通过参数callback_url获取回调RestUrl
                List<String> ids = null;
                Object refIds = businessObject.get("ids");
                if (refIds != null && refIds instanceof List) {
                    ids = (List<String>) refIds;
                }
                String callBackDomain = businessObject.get("callback_domain") == null ? null : businessObject.get("callback_domain").toString();
                RefCheckService refCheckService = AppContext.getBean(UcfBasedocRefCheckServiceImpl.class);
                String fullNameExt;
                Map<String, Boolean> refCheckMap; //refCheckService.refCheck(fullName, ids, AppContext.getTenantId());

                if (null != businessObject.get("fullnameExt") && "bd.basedocdef.CustomerDocVO".equals(fullName)) {
                    fullNameExt = businessObject.get("fullnameExt") == null ? null
                            : businessObject.get("fullnameExt").toString();
                    refCheckMap = refCheckService.refCheck(fullNameExt, ids, AppContext.getTenantId());
                } else {
                    refCheckMap = refCheckService.refCheck(fullName, ids, AppContext.getTenantId());
                }
                log.error("--------------------------------------------------------> 处理删除引用监听事件refCheckMap打印:" + CtmJSONObject.toJSONString(refCheckMap));

                log.info("引用校验参数:fullNameExt:{},fullName:{},domain:{},callBackDomain:{}", businessObject.get("fullnameExt"),
                        LogForgingFilter.filter(fullName), domain, callBackDomain);
                // 通过rest方式发送消息，同域下直接返送消息
                CallBackParam callBackParam = new CallBackParam();
                callBackParam.setFullname(fullName);
                callBackParam.setRefcheck(refCheckMap);
                callBackParam.setIds(ids);
                callBackParam.setDomain(domain);
                callBackParam.setTenantId(tenantId);

                if (domain != null && domain.equals(callBackDomain)) {
                    //同域调用
                    callback(request, null, callBackParam);
                } else {
                    log.error("--------------------------------------------------------> 处理删除引用监听事件异域调用");
                    Map<String, String> queryParam = new HashMap<String, String>();
                    queryParam.put("yht_access_token", InvocationInfoProxy.getYhtAccessToken());
                    Map<String, String> header = new HashMap<String, String>();
                    header.put("yht_access_token", InvocationInfoProxy.getYhtAccessToken());
                    String restUrl = businessObject.get("callback_url") == null ? null : businessObject.get("callback_url").toString();
                    log.error("--------------------------------------------------------> 处理删除引用监听事件restUrl打印:" + restUrl);
                    CTMAuthHttpClientUtils.execPost(restUrl, queryParam, header, CtmJSONObject.toJSONString(callBackParam));
                }
            }
            result.put("success", "true");
        } catch (Exception e) {
            log.error("----> 处理删除引用监听事件异常" + e.getMessage(), e);
            result.put("success", "false");
        }
        return result;
    }

    /**
     * 回调需要设置yht_access_token
     *
     * @param request
     * @param response
     * @param callBackParam
     */
    @RequestMapping("/callback")
    public void callback(HttpServletRequest request, HttpServletResponse response,
                         @RequestBody CallBackParam callBackParam) {
        if (callBackParam != null) {
            String domain = callBackParam.getDomain();
            String fullName = callBackParam.getFullname();
            List<String> ids = callBackParam.getIds();
            Map<String, Boolean> refCheckMap = callBackParam.getRefcheck();
            // 处理领域验证结果
            if (null == ids || ids.isEmpty()) {
                return;
            }
            for (String id : ids) {
                String redisKey = getRedisKey(fullName, id);
                if (refCheckMap != null && refCheckMap.containsKey(id) && refCheckMap.get(id)) {
                    // 不能删除
                    rollBack(id, fullName, redisKey);
                } else {
                    boolean exist = AppContext.cache().exists(redisKey);
                    if (exist) {
                        // 设置缓存状态
                        //
                        String refAllDomain = AppContext.cache().hget(redisKey,
                                UCFBASEDOC_REF_CHECK_DOMAIN);
                        if (null == refAllDomain || refAllDomain.length() == 0) {
                            // 为空，代表已经校验过所有注册领域，并都没有引用,更新状态为删除，dr = 1
                            delete(id, fullName, redisKey);
                        } else {
                            // 这是针对多个
                            CtmJSONArray refDomainArray = CtmJSONArray.parseArray(refAllDomain);
                            refDomainArray.remove(domain);
                            if (refDomainArray.size() == 0) {
                                // 为空，代表已经校验过所有注册领域，并都没有引用,更新状态为删除，dr = 1
                                delete(id, fullName, redisKey);
                            } else {
                                AppContext.cache().hset(redisKey, UCFBASEDOC_REF_CHECK_DOMAIN,
                                        CtmJSONObject.toJSONString(refDomainArray));
                            }
                        }
                    } else {
                        // 失效，该条数据不用在继续校验了，回滚预删除状态
                        // 不能删除
                        rollBack(id, fullName, redisKey);
                    }

                }
            }

        }
    }

    /**
     * 根据参数
     * 获取redis键值
     * 值根据当前删除实体的full及当前删除id
     *
     * @param fullName
     * @param id
     * @return
     */
    private static String getRedisKey(String fullName, String id) {
        return fullName + KEY_SPLIT + AppContext.getTenantId() + KEY_SPLIT + id;
    }

    private void rollBack(String id, String fullName, String redisKey) {
        // 不能删除
        BizObject bill = new BizObject();
        bill.set("id", id);
        bill.set(RULE_INTERFACE_FIELD_LOGIC_DEL, VAL_LOGIC_UNDEL);
        bill.set("_status", EntityStatus.Update);
        try {
            MetaDaoHelper.update(fullName, bill);
            boolean exist = AppContext.cache().exists(redisKey);
            if (exist) {
                AppContext.cache().expire(redisKey, 0);
            }
            // 将缓存设置失效
        } catch (Exception e) {
            log.error("不能更新，将预删除状态回滚到删除前状态失败", e);
        }
    }

    private void delete(String id, String fullName, String redisKey) {
        // 不能删除
        BizObject bill = new BizObject();
        bill.set("id", id);
        bill.set(RULE_INTERFACE_FIELD_LOGIC_DEL, VAL_LOGIC_DELETED);
        bill.set("_status", EntityStatus.Update);
        try {
            MetaDaoHelper.update(fullName, bill);
            AppContext.cache().expire(redisKey, 0);
            // 将缓存设置失效
        } catch (Exception e) {
            log.error("不能更新，将预删除状态回滚到删除前状态失败", e);
        }
    }

    public void doFilter(HttpServletRequest args0/*, ServletResponse args1, FilterChain chain*/) throws IOException, ServletException {
        HttpServletRequest req = args0;
        //获得所有请求参数名
        Enumeration params = req.getParameterNames();
        String sql = "";
        while (params.hasMoreElements()) {
            //得到参数名
            String name = params.nextElement().toString();
            //得到参数对应值
            String[] value = req.getParameterValues(name);
            for (int i = 0; i < value.length; i++) {
                sql = sql + value[i];
            }
        }
        //有sql注入和XSS危险字符
        if (sqlValidate(sql)) {
            throw new IOException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B9A43A05D80079", "请求有非法字符：") /* "请求有非法字符：" */ + sql);
        }
    }

    protected static boolean sqlValidate(String str) {
        str = str.toLowerCase(); //统一转为小写
        String badStr = "and|exec|insert|select|delete |update|count|union|master|truncate|char|declare |cast|set|fetch|varchar|sysobjects|drop |`|'|\"|<|>|(|)|/||=|+|-|#|*|;|%";
        String[] badStrs = badStr.split(" | ");
        for (int i = 0; i < badStrs.length; i++) {
            if (str.indexOf(badStrs[i]) >= 0) {
                return true; //参数中包含要过滤关键字;
            }
        }
        return false; //参数中不包含要过滤关键字;
    }

}
