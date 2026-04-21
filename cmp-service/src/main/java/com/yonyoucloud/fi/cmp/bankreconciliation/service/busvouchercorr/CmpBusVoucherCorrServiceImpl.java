package com.yonyoucloud.fi.cmp.bankreconciliation.service.busvouchercorr;

import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.remote.RemoteDubbo;
import com.yonyoucloud.fi.cmp.constant.IDomainConstant;
import com.yonyoucloud.fi.cmp.util.RestTemplateUtils;
import com.yonyoucloud.scmpub.udinghuo.service.ISaleOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

/**
 * @description: 智能到账, 业务凭据关联service
 * @author: wanxbo@yonyou.com
 * @date: 2024/7/1 10:26
 */
@Service
@Slf4j
@Transactional(rollbackFor = RuntimeException.class)
public class CmpBusVoucherCorrServiceImpl implements CmpBusVoucherCorrService {


    @Value("${domain.yonbip-ec-contract}")
    private String contractUrl;

    /**
     * 根据业务凭据关联到的销售订单和收款模式，查询关联到的收款协议或者收款执行明细
     *
     * @param param orderid:销售订单id
     * @return 收款协议集合 paymentSchedules 收款执行明细的key为paymentExeDetail
     * @throws Exception
     */
    @Override
    public CtmJSONObject queryUdinghuoCollectAgreement(CtmJSONObject param) throws Exception {
        CtmJSONObject result = new CtmJSONObject();
        if (Objects.isNull(param) || Objects.isNull(param.get("rows"))) {
            log.error("业务凭据关联根据销售订单查询收款协议或者收款执行明细失败：入参orderId为空");
            result.put("count", 0);
            return result;
        }
        if (Objects.isNull(param) || Objects.isNull(param.get("paymentModel"))) {
            log.error("业务凭据关联根据销售订单查询收款协议或者收款执行明细失败：收款模式或者收款执行明细为空");
            result.put("count", 0);
            return result;
        }
        //订单id参数
        Map<String, Set<Long>> orderIdMap = new HashMap<>();
        Set<Long> orderIdSet = new HashSet<>();
        Object rowsObj = param.get("rows");
        if (rowsObj instanceof Iterable) {
            for (Object item : (Iterable<?>) rowsObj) {
                try {
                    orderIdSet.add(Long.valueOf((String) ((LinkedHashMap) item).get("orderId")));
                } catch (NumberFormatException e) {
                    log.warn("无法将字符串转换为Long: {}", item, e);
                }
            }
        } else if (rowsObj instanceof CtmJSONArray) {
            CtmJSONArray rowsArray = (CtmJSONArray) rowsObj;
            for (int i = 0; i < rowsArray.size(); i++) {
                Object item = rowsArray.get(i);
                try {
                    orderIdSet.add(Long.valueOf((String) ((LinkedHashMap) item).get("orderId")));
                } catch (NumberFormatException e) {
                    log.warn("无法将字符串转换为Long: {}", item, e);
                }
            }
        } else {
            log.error("rows参数不是可遍历的集合类型");
        }
        if (Objects.isNull(orderIdSet)) {
            log.error("业务凭据关联根据销售订单查询收款协议或者收款执行明细失败：入参orderId为空");
            result.put("count", 0);
            return result;
        }
        orderIdMap.put("order", orderIdSet);

        String[] orderField = {"id"};
        //需要返回的数据字段名
        Map<String, Set<String>> queryFiledMap = new HashMap<>();
        Set<String> orderFieldSet = new HashSet<>(Arrays.asList(orderField));
        if (param.get("paymentModel").toString().equals("1")) {
            String[] paymentSchedulesField = {"id", "mainid", "number", "name", "amount", "paidMoneyNew", "rebateCashMoney", "mainid.salesOrgId", "mainid.salesOrgId.name", "mainid.code", "mainid.vouchdate"};
            Set<String> paymentSchedulesFieldSet = new HashSet<>(Arrays.asList(paymentSchedulesField));
            queryFiledMap.put("order", orderFieldSet);
            queryFiledMap.put("paymentSchedules", paymentSchedulesFieldSet);
        } else if (param.get("paymentModel").toString().equals("2")) {
            String[] paymentExeDetailField = {"id", "mainid", "order",  "paidMoneyNew",  "mainid.salesOrgId", "mainid.salesOrgId.name", "mainid.code", "mainid.vouchdate"};
            Set<String> paymentExeDetailFieldSet = new HashSet<>(Arrays.asList(paymentExeDetailField));
            queryFiledMap.put("order", orderFieldSet);
            queryFiledMap.put("paymentExeDetail", paymentExeDetailFieldSet);
        }

        //依据销售订单主表ID查询销售订单数据
        List<Map<String, Object>> orderResultList = RemoteDubbo.get(ISaleOrderService.class, IDomainConstant.YPD_DOMAIN_DDINGHUO).queryOrderByIdsAndFields(orderIdMap, queryFiledMap);

        //业务凭据信息集合
        List<CtmJSONObject> busVoucherInfoList = new ArrayList<>();
        if (orderResultList != null && orderResultList.size() > 0) {
            for (Map<String, Object> orderResult : orderResultList) {
                //如果是收款协议
                if (param.get("paymentModel").toString().equals("1")) {
                    List<Map<String, Object>> paymentSchedules = (List<Map<String, Object>>) orderResult.get("paymentSchedules");
                    if (paymentSchedules != null && paymentSchedules.size() != 0) {
                        for (Map<String, Object> m : paymentSchedules) {
                            CtmJSONObject busVoucherInfo = new CtmJSONObject();
                            //凭据主表id
                            busVoucherInfo.put("billmainid", m.get("mainid"));
                            //凭据单据类型，销售订单
                            busVoucherInfo.put("billtype", 1);
                            //业务单元
                            busVoucherInfo.put("accentity", m.get("mainid_salesOrgId"));
                            busVoucherInfo.put("accentity_name", m.get("mainid_salesOrgId_name"));
                            //单据编码
                            busVoucherInfo.put("billcode", m.get("mainid_code"));
                            //单据日期
                            busVoucherInfo.put("billdate", m.get("mainid_vouchdate"));

                            //凭据明细id,收款协议id
                            busVoucherInfo.put("billitmeid", m.get("id"));
                            //期号
                            busVoucherInfo.put("billitmecode", m.get("number"));
                            //阶段名称
                            busVoucherInfo.put("peridname", m.get("name"));
                            //应收金额
                            busVoucherInfo.put("needamount", m.get("amount"));
                            //总金额
                            busVoucherInfo.put("totalamount", m.get("amount"));
                            //已收金额
                            busVoucherInfo.put("receivedamount", m.get("paidMoneyNew"));
                            //未收金额 收款金额-已收款金额-抵现返利金额
                            BigDecimal amount = m.get("amount") != null ? new BigDecimal(m.get("amount").toString()) : BigDecimal.ZERO;
                            BigDecimal paidMoneyNew = m.get("paidMoneyNew") != null ? new BigDecimal(m.get("paidMoneyNew").toString()) : BigDecimal.ZERO;
                            BigDecimal rebateCashMoney = m.get("rebateCashMoney") != null ? new BigDecimal(m.get("rebateCashMoney").toString()) : BigDecimal.ZERO;
                            busVoucherInfo.put("uncollectedamount", amount.subtract(paidMoneyNew).subtract(rebateCashMoney));

                            busVoucherInfoList.add(busVoucherInfo);
                        }
                    }
                }
                //如果是收款执行明细
                if (param.get("paymentModel").toString().equals("2")) {
                    List<Map<String, Object>> paymentExeDetail = (List<Map<String, Object>>) orderResult.get("paymentExeDetail");
                    if (paymentExeDetail != null && paymentExeDetail.size() != 0) {
                        for (Map<String, Object> m : paymentExeDetail) {
                            CtmJSONObject busVoucherInfo = new CtmJSONObject();
                            //凭据主表id
                            busVoucherInfo.put("billmainid", m.get("mainid"));
                            //凭据单据类型，销售订单
                            busVoucherInfo.put("billtype", 1);
                            //业务单元
                            busVoucherInfo.put("accentity", m.get("mainid_salesOrgId"));
                            busVoucherInfo.put("accentity_name", m.get("mainid_salesOrgId_name"));
                            //单据编码
                            busVoucherInfo.put("billcode", m.get("mainid_code"));
                            //单据日期
                            busVoucherInfo.put("billdate", m.get("mainid_vouchdate"));

                            //凭据明细id,收款协议id
                            busVoucherInfo.put("billitmeid", m.get("id"));
                            //期号
                            busVoucherInfo.put("billitmecode", m.get("order"));
                            //阶段名称
                            busVoucherInfo.put("peridname", m.get("period"));
                            //应收金额
                            busVoucherInfo.put("needamount", m.get("receivableMoney"));
                            //已收金额
                            busVoucherInfo.put("receivedamount", m.get("paidMoneyNew"));
                            //未收金额
                            busVoucherInfo.put("uncollectedamount", m.get("uncollectedMoney"));
                            busVoucherInfoList.add(busVoucherInfo);
                        }
                    }
                }
            }
        } else {
            log.error("业务凭据关联根据销售订单查询收款协议或收款执行明细,结果集为空，result=" + orderResultList);
            result.put("count", 0);
            return result;
        }

        result.put("count", busVoucherInfoList.size());
        result.put("busVoucherInfoList", busVoucherInfoList);
        return result;
    }

    /**
     * 根据业务凭据关联下的合同档案明细信息调用rest接口查询并返回
     *
     * @param param 合同档案明细信息
     * @return 我的认领业务凭据关联信息子表集合
     * @throws Exception
     */
    @Override
    public CtmJSONObject getBusVoucherInfoList(CtmJSONObject param) {
        if (Objects.isNull(param) || Objects.isNull(param.get("rows"))) {
            log.error("业务凭据关联查询合同档案明细信息请求参数为空！");
            return new CtmJSONObject();
        }

        // 将 ArrayList 转换为 CtmJSONArray
        CtmJSONArray rowsArray = new CtmJSONArray();
        if (param.get("rows") instanceof ArrayList) {
            ArrayList<?> rowsList = (ArrayList<?>) param.get("rows");
            for (Object item : rowsList) {
                rowsArray.add(item);
            }
        } else {
            rowsArray = (CtmJSONArray) param.get("rows");
        }

        log.error("业务凭据关联查询合同档案地址：" + contractUrl + "/contractCenter/getBusVoucherInfoList");
        CtmJSONObject queryresult = RestTemplateUtils.doPostByJSONArray(contractUrl + "/contractCenter/getBusVoucherInfoList", rowsArray);
        log.error("查询合同档案明细信息返回值：" + queryresult.toString());
        return queryresult;
    }
}
