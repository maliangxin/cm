package com.yonyoucloud.fi.cmp.electronicstatementconfirm.rule;

import com.yonyou.iuap.org.dto.FinOrgDTO;
import com.yonyou.ucf.basedoc.model.BankAcctCurrencyVO;
import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVO;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.electronicstatementconfirm.ElectronicStatementConfirm;
import com.yonyoucloud.fi.cmp.util.AccentityUtil;
import com.yonyoucloud.fi.cmp.util.basedoc.EnterpriseBankQueryService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 *  电子对账单直连确认，保存前规则，iorder：25
 * @since 2024年10月23日11:13:34
 */
@Slf4j
@Component
public class ElectronicStatementConfirmSave extends AbstractCommonRule {

    @Autowired
    private EnterpriseBankQueryService enterpriseBankQueryService;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        try {
            List<BizObject> bills = getBills(billContext, paramMap);
            BillDataDto billDataDto = (BillDataDto) getParam(paramMap);
            for (BizObject bizObject : bills) {
                boolean importFlag =  "import".equals(billDataDto.getRequestAction());
                if (importFlag) {
                    // 导入数据，需要给账户名称赋值，bank_name，banktype
                    if (bizObject.get("bankaccount") != null && StringUtils.isNotBlank(bizObject.get("bankaccount"))) {
                        String bankAccountId = bizObject.get("bankaccount");
                        EnterpriseBankAcctVO enterpriseBankAcctVO = enterpriseBankQueryService.findById(bankAccountId);
                        QuerySchema querySchema = QuerySchema.create().addSelect(QuerySchema.PARTITION_ALL);
                        querySchema.appendQueryCondition(QueryCondition.name("bankaccount").eq(bankAccountId));
                        querySchema.appendQueryCondition(QueryCondition.name("statement_name").eq(bizObject.get("statement_name")));
                        List<Map<String, Object>> existList = MetaDaoHelper.query(ElectronicStatementConfirm.ENTITY_NAME, querySchema);
                        if (existList != null && existList.size() > 0) {
                            // 导入时判定文件文件名+银行账号是否唯一，如不唯一提示：“银行账号【XXX】已存在相同名称的对账单文件，请修改名字后重新导入！”  （银企联拉取时不进行该校验）
                            throw new CtmException(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1DD0CA660580000B", "银行账号【%s】已存在相同名称的对账单文件，请修改名字后重新导入！") /* "银行账号【%s】已存在相同名称的对账单文件，请修改名字后重新导入！" */, enterpriseBankAcctVO.getAccount()) /* "银行对账单[%s]不是已发布状态" */);
                        }
                        // 判重逻辑：导入时判定文件名”银行账号+币种+对账单编号+账单开始日期+账单结束日期“组合是否唯一，如不唯一提示：“银行账号【XXX】已存在相同的对账单编号【XXX】，请检查！”
                        querySchema = QuerySchema.create().addSelect(QuerySchema.PARTITION_ALL);
                        querySchema.appendQueryCondition(QueryCondition.name("bankaccount").eq(bankAccountId));
                        querySchema.appendQueryCondition(QueryCondition.name("currency").eq(bizObject.get("currency")));
                        querySchema.appendQueryCondition(QueryCondition.name("statementno").eq(bizObject.get("statementno")));
                        querySchema.appendQueryCondition(QueryCondition.name("startdate").eq(bizObject.get("startdate")));
                        querySchema.appendQueryCondition(QueryCondition.name("enddate").eq(bizObject.get("enddate")));
                        existList = MetaDaoHelper.query(ElectronicStatementConfirm.ENTITY_NAME, querySchema);
                        if (existList != null && existList.size() > 0) {
                            // “银行账号【XXX】已存在相同的对账单编号【XXX】，请检查！”
                            throw new CtmException(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1DE8470A04800008", "银行账号【%s】已存在相同的对账单编号【%s】，请检查！") /* "银行账号【%s】已存在相同的对账单编号【%s】，请检查！" */, enterpriseBankAcctVO.getAccount(), bizObject.get("statementno")));
                        }
                        if (enterpriseBankAcctVO != null) {
                            // CZFW-403081,导入时根据银行账户+币种进行校验;银行账户没有的币种，不让导入
                            List<BankAcctCurrencyVO> bankAcctCurrencyVOList = enterpriseBankAcctVO.getCurrencyList();
                            boolean currencyFlag = false;
                            for (BankAcctCurrencyVO bankAcctCurrencyVO : bankAcctCurrencyVOList) {
                                if (bizObject.get("currency").equals(bankAcctCurrencyVO.getCurrency())) {
                                    currencyFlag = true;
                                }
                            }
                            if (!currencyFlag) {
                                throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180651","银行账号币种与导入币种不一致！") /* "银行账号币种与导入币种不一致！" */);
                            }
                            bizObject.set("bank_name", enterpriseBankAcctVO.getAcctName());
                            bizObject.set("banktype", enterpriseBankAcctVO.getBank());
                            // 1 银企直联下载 2 手工导入
                            bizObject.set("inputtype", (short) 2);
                            bizObject.set("accentity", enterpriseBankAcctVO.getOrgid());
                            FinOrgDTO finOrgDTO = AccentityUtil.getFinOrgDTOByAccentityId(enterpriseBankAcctVO.getOrgid());
                            bizObject.set("authoruseaccentity", finOrgDTO.getId());
                        }
                        bizObject.set("statement_download", false);
                    }
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage());
            throw e;
        }
        return new RuleExecuteResult();
    }

}
