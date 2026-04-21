package com.yonyoucloud.fi.cmp.bill;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.service.MddExtComQueryApiService;
import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import com.yonyoucloud.fi.cmp.enums.BillMessageEnum;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/*
 * <h1>打印查询条件扩展</h1>
 *
 * @author Sun GuoCai
 * @version 1.0
 * @since 2024-02-26 12:21
 */
@Service("cmpPrintMddExtComQueryApiService")
@Primary
public class CmpPrintMddExtComQueryApiService extends MddExtComQueryApiService {

    private static final List<String> VOUCHERNO_BILLNUM_LIST = new ArrayList<>();
    static {
        VOUCHERNO_BILLNUM_LIST.add(BillMessageEnum.CMP_FUND_COLLECTION.getBillType());
        VOUCHERNO_BILLNUM_LIST.add(BillMessageEnum.CMP_FUND_PAYMENT.getBillType());
        VOUCHERNO_BILLNUM_LIST.add(BillMessageEnum.CM_TRANSFER_ACCOUNT.getBillType());
        VOUCHERNO_BILLNUM_LIST.add(BillMessageEnum.CMP_ACCRUALSWITHHOLDINGQUERY.getBillType());
        VOUCHERNO_BILLNUM_LIST.add(BillMessageEnum.CMP_PAYMARGIN.getBillType());
        VOUCHERNO_BILLNUM_LIST.add(BillMessageEnum.CMP_RECEIVEMARGIN.getBillType());
        VOUCHERNO_BILLNUM_LIST.add(BillMessageEnum.CMP_CURRENCY_EXCHANGE.getBillType());
        VOUCHERNO_BILLNUM_LIST.add(BillMessageEnum.CMP_EXCHANGE_GAINLOSS.getBillType());
        VOUCHERNO_BILLNUM_LIST.add(BillMessageEnum.CMP_FOREIGNPAYMENT.getBillType());
        VOUCHERNO_BILLNUM_LIST.add(BillMessageEnum.CMP_SALARY_PAY.getBillType());
        VOUCHERNO_BILLNUM_LIST.add(BillMessageEnum.CMP_RECEIVE_BILL.getBillType());
        VOUCHERNO_BILLNUM_LIST.add(BillMessageEnum.CMP_PAYMENT.getBillType());

    }

    /**
     * @param fullname    业务对象主实体uri
     * @param json        QuerySchema 的json格式的字符串
     * @param useES       是否来源于ES
     * @param formatData  是否进行数据格式化处理
     * @param replaceEnum 是否替换枚举
     * @param busiScence  业务场景
     * @return
     * @throws Exception
     */
    @Override
    public <T extends Map<String, Object>> List<T> queryByBizObj(String fullname, String json, boolean useES, boolean formatData, boolean replaceEnum, String busiScence) throws Exception {
        if ("cmp.balanceadjustresult.BalanceAdjustResult".equals(fullname) && "print".equals(busiScence)){
            if (json.contains("balanceadjustBankreconciliation")){
                CtmJSONObject ctmJSONObject = CtmJSONObject.parseObject(json);
                CtmJSONArray compositions = ctmJSONObject.getJSONArray("compositions");
                for (Object composition : compositions) {
                    Map<String, Object> jsonObject = (Map<String, Object>) composition;
                    Object name = jsonObject.get("name");
                    if ("balanceadjustBankreconciliation".equals(name)){
                        CtmJSONArray array = new CtmJSONArray();
                        CtmJSONObject jsonObj = new CtmJSONObject();
                        jsonObj.put("name", "dzdate");
                        jsonObj.put("order", "asc");
                        array.add(jsonObj);
                        jsonObject.put("orders",array);
                    }
                }
                json = CtmJSONObject.toJSONString(ctmJSONObject);
            }
            return MetaDaoHelper.queryByBizObj(fullname, json, useES, formatData, replaceEnum);
        } else {
            List<T> list = MetaDaoHelper.queryByBizObj(fullname, json, useES, formatData, replaceEnum);
            if (VOUCHERNO_BILLNUM_LIST.contains(fullname) && "print".equals(busiScence)){
                return translateVoucherNo(list);
            } else {
                return list;
            }
        }
    }

    private <T extends Map<String, Object>> List<T>  translateVoucherNo(List<T> list){
        if(CollectionUtils.isEmpty(list)){
            return list;
        }
        for (int i = 0; i < list.size(); i++) {
            Object objVoucherNo = list.get(i).get("voucherNo");
            if (ValueUtils.isNotEmptyObj(objVoucherNo)) {
                JSONObject jsonObject = JSON.parseObject(String.valueOf(objVoucherNo));
                String locale = InvocationInfoProxy.getLocale();
                switch (locale) {
                    case "zh_CN":
                        list.get(i).put("voucherNo", ValueUtils.isNotEmptyObj(jsonObject.get("zh_CN")) ? jsonObject.get("zh_CN") : "");
                        break;
                    case "en_US":
                        list.get(i).put("voucherNo", ValueUtils.isNotEmptyObj(jsonObject.get("en_US")) ? jsonObject.get("en_US") : "");
                        break;
                    case "zh_TW":
                        list.get(i).put("voucherNo", ValueUtils.isNotEmptyObj(jsonObject.get("zh_TW")) ? jsonObject.get("zh_TW") : "");
                        break;
                    default:
                        list.get(i).put("voucherNo", "");
                }
            }
        }
        return list;
    }
    /**
     * 需要根据自定义条件处理数据的服务请复写这个方法实现
     *
     * @param fullname     业务对象主实体uri
     * @param json         QuerySchema 的json格式的字符串
     * @param useES        是否来源于ES
     * @param formatData   是否进行数据格式化处理
     * @param replaceEnum  是否替换枚举
     * @param busiScence   业务场景
     * @param extendParams 自定义扩展参数，由前端扩展传递给打印服务，打印服务再通过接口参数传递过来
     * @return
     * @throws Exception
     */
    @Override
    public <T extends Map<String, Object>> List<T> queryByBizObj(String fullname, String json, boolean useES, boolean formatData, boolean replaceEnum, String busiScence, Map<String, Object> extendParams) throws Exception {
        if ("cmp.bankreconciliation.BankReconciliation".equals(fullname) && "print".equals(busiScence)){
            //打印时拼接 isrepeat is null or isrepat is not null
            CtmJSONObject jsonObject = CtmJSONObject.parseObject(json);
            CtmJSONObject object = CtmJSONObject.parseObject("{\"op\":\"or\",\"items\":[{\"name\":\"isrepeat\",\"op\":\"is_null\"},{\"name\":\"isrepeat\",\"op\":\"is_not_null\"}]}");
            CtmJSONArray array = jsonObject.getJSONArray("conditions");
            array.add(object);
            jsonObject.put("conditions", array);
            json = CtmJSONObject.toJSONString(jsonObject);
            return MetaDaoHelper.queryByBizObj(fullname,json , useES, formatData, replaceEnum);
        }else {
            return super.queryByBizObj(fullname, json, useES, formatData, replaceEnum,busiScence,extendParams);
        }
    }

}
