package com.yonyoucloud.fi.cmp.foreignpayment.service;

import com.yonyou.iuap.ucf.common.i18n.InternationalUtils;
import com.yonyou.ucf.basedoc.model.BankVO;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetCommonManagerService;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetManagerService;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetVO;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.enums.BillMapEnum;
import com.yonyoucloud.fi.cmp.foreignpayment.ForeignPayment;
import com.yonyoucloud.fi.cmp.util.BudgetUtils;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import com.yonyoucloud.fi.cmp.util.basedoc.EnterpriseBankQueryService;
import com.yonyoucloud.fi.cmp.voucher.CmpVoucherService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;


@Service
@Slf4j
public class ForeignPaymentServiceImpl implements ForeignPaymentService {


    @Autowired
    EnterpriseBankQueryService enterpriseBankQueryService;

    @Autowired
    private CmpVoucherService cmpVoucherService;

    @Autowired
    private CmpBudgetManagerService cmpBudgetManagerService;

    @Autowired
    private CmpBudgetCommonManagerService cmpBudgetCommonManagerService;

    /**
     * 查询银行类别*
     * @param param
     * @return
     * @throws Exception
     */
    @Override
    public CtmJSONObject queryBankCode(CtmJSONObject param) throws Exception {
        CtmJSONObject result = new CtmJSONObject();
        //前端可能不传递bankid，所以需要进行判空，没有查到默认返回空就行，就不会进行默认赋值
        if(param.get("bankid") != null){
            String bankid = param.get("bankid").toString();
            if (ObjectUtils.isNotEmpty(bankid)){
                BankVO bankVO = enterpriseBankQueryService.querybankTypeNameById(bankid);
                String bankTypeCode = bankVO.getCode();
                result.put("bankTypeCode", bankTypeCode);// 银行类别code
            }
        }
        return result;
    }

    /**
     * 生成凭证
     * * *
     * @param foreignPayment
     * @throws Exception
     */
    @Override
    public void toVourcher(ForeignPayment foreignPayment) throws Exception {
        foreignPayment.put("_entityName","cmp.foreignpayment.ForeignPayment");
        cmpVoucherService.generateVoucherWithResult(foreignPayment);
    }

    @Override
    @Transactional(rollbackFor = RuntimeException.class)
    public String budgetCheckNew(CmpBudgetVO cmpBudgetVO) throws Exception {
        if (!cmpBudgetManagerService.isCanStart(IBillNumConstant.CMP_FOREIGNPAYMENT)) {
            CtmJSONObject resultBack = new CtmJSONObject();
            resultBack.put(ICmpConstant.CODE, true);
            return ResultMessage.data(resultBack);
        }
        String billnum = cmpBudgetVO.getBillno();

        String entityname = null;
        BillMapEnum enumByBillNum = BillMapEnum.getEnumByBillNum(billnum);
        if (enumByBillNum != null) {
            entityname = enumByBillNum.getEntityName();
        }
        if (com.yonyoucloud.fi.cmp.util.StringUtils.isEmpty(entityname)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100612"),InternationalUtils.getMessageWithDefault("UID:P_TLM-BE_19AF9FC204D000CF", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400674", "请求参数缺失") /* "请求参数缺失" */));
        }

        //TODO 变更单据返回来源单据信息，ids 为便跟单据自己信息
        List<String> ids = cmpBudgetVO.getIds();
        List<BizObject> bizObjects = new ArrayList<>();
        if (ValueUtils.isNotEmptyObj(ids)) {
            bizObjects = queryBizObjsWarpParentInfo(ids);
        } else if (ValueUtils.isNotEmptyObj(cmpBudgetVO.getBizObj())) {
            BizObject bizObject = CtmJSONObject.parseObject(cmpBudgetVO.getBizObj(), BizObject.class);
            bizObjects.add(bizObject);
        } else {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100612"),InternationalUtils.getMessageWithDefault("UID:P_TLM-BE_19AF9FC204D000CF", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400674", "请求参数缺失") /* "请求参数缺失" */));
        }
        //变更单据
        String changeBillno = cmpBudgetVO.getChangeBillno();
        if (!com.yonyoucloud.fi.cmp.util.StringUtils.isEmpty(changeBillno)) {
            if (CollectionUtils.isEmpty(bizObjects)) {
                CtmJSONObject resultBack = new CtmJSONObject();
                resultBack.put(ICmpConstant.CODE, true);
                resultBack.put("message", InternationalUtils.getMessageWithDefault("UID:P_TLM-BE_19AF9FC204D000D0", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400675", "变更金额小于原金额,不需要校验!") /* "变更金额小于原金额,不需要校验!" */));
                return ResultMessage.data(resultBack);
            }
            //变更单据获取（融资登记单据类型）
            billnum = changeBillno;
        } else {
            //非变更单据 自己单据
            if (CollectionUtils.isEmpty(bizObjects)) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100612"),InternationalUtils.getMessageWithDefault("UID:P_TLM-BE_19AF9FC204D000CF", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400674", "请求参数缺失") /* "请求参数缺失" */));
            }
        }
        return cmpBudgetCommonManagerService.budgetCheckNew(bizObjects, billnum, BudgetUtils.SUBMIT);
    }

    public List<BizObject> queryBizObjsWarpParentInfo(List<String> ids) throws Exception {
        // 根据id批量查询数据
        QuerySchema schema = QuerySchema.create().addSelect("*");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        // 只查询来源为现金的数据 只有这类数据需要升级
        conditionGroup.appendCondition(QueryCondition.name("id").in(ids));
        schema.addCondition(conditionGroup);
        return MetaDaoHelper.queryObject(ForeignPayment.ENTITY_NAME, schema, null);
    }
}
