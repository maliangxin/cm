package com.yonyoucloud.fi.cmp.ctmrpc;

import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.dao.sql.SqlHelper;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.bankaccountsetting.BankAccountSetting;
import com.yonyoucloud.fi.cmp.constant.EmpowerConstand;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.api.bankaccountsetting.CtmCmpBankAccountSettingRpcService;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.bankAccountSetting.BankAccountSettingVO;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.common.CommonRequestDataVo;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.common.CommonResponseDataVo;
import com.yonyoucloud.fi.cmp.util.CmpMetaDaoHelper;
import com.yonyoucloud.fi.cmp.util.EntityTool;
import org.apache.commons.lang3.StringUtils;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 银企联账户提供查询银行账户银企联启用状态的接口*
 * @author xuxbo
 * @date 2022/11/2 10:25
 */
@Service
public class CtmCmpBankAccountSettingRpcServiceImpl implements CtmCmpBankAccountSettingRpcService {

    private static final String BANKACCOUNTSETTINGMAPPER = "com.yonyoucloud.fi.cmp.mapper.BankAccountSettingMapper.";

    @Override
    public CommonResponseDataVo queryBankAccountSettingOpenFlag(CommonRequestDataVo commonQueryData) throws Exception {
        CommonResponseDataVo result = new CommonResponseDataVo();
        String id = commonQueryData.getId();
        commonQueryData.setYtenantId(InvocationInfoProxy.getTenantid());
        if (StringUtils.isEmpty(id)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101429"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A05080051", "id不可为空！") /* "id不可为空！" */);
        }
        CtmJSONObject openflag = SqlHelper.selectOne(BANKACCOUNTSETTINGMAPPER + "queryBankAccountSettingOpenFlag", commonQueryData);
        result.setOpenflag(openflag.getBoolean("openflag"));
        return result;
    }

    /**
     * 简强修改需测试
     * @param bankAccountSettingVos
     * @return
     * @throws Exception
     */
    @Override
    public CommonResponseDataVo CtmCmpBankAccountSettingSave(List<BankAccountSettingVO> bankAccountSettingVos) throws Exception {
        CommonResponseDataVo result = new CommonResponseDataVo();
        try{
            List<BankAccountSetting> bankAccountSettings = new ArrayList<>();
            bankAccountSettingVos.stream().forEach(e -> {
                BankAccountSetting bankAccountSetting=  new BankAccountSetting();
                //TODO 简强待处理
//                bankAccountSetting.init(e);
                bankAccountSettings.add(bankAccountSetting);
            });
            bankAccountSettings.forEach(bankaccountSetting -> {
                bankaccountSetting.setEntityStatus(EntityStatus.Insert);
            });
            CmpMetaDaoHelper.insert(BankAccountSetting.ENTITY_NAME, bankAccountSettings);
        }catch (Exception e){
            result.setSuccess(false);
            result.setMessage(e.getMessage());
            return result;
        }
        return result;
    }

    /**
     * 简强修改需测试
     * @param bankAccountSettingVos
     * @return
     * @throws Exception
     */
    @Override
    public CommonResponseDataVo CtmCmpBankAccountSettingUpdate(List<BankAccountSettingVO> bankAccountSettingVos) throws Exception {
        CommonResponseDataVo result = new CommonResponseDataVo();
        try{
            List<BankAccountSetting> bankAccountSettings = new ArrayList<>();
            bankAccountSettingVos.stream().forEach(e -> {
                BankAccountSetting bankAccountSetting=  new BankAccountSetting();
                bankAccountSettings.add(convertBankAccountSettingVO2BankAccountSetting(e,bankAccountSetting));
            });
            EntityTool.setUpdateStatus(bankAccountSettings);
            MetaDaoHelper.update(BankAccountSetting.ENTITY_NAME, bankAccountSettings);
        }catch(Exception e){
            result.setSuccess(false);
            result.setMessage(e.getMessage());
            return result;
        }
        return result;
    }

    /**
     * //TODO 简强待处理
     * @param bankAccountSettingVO
     * @param bankAccountSetting
     * @return
     */
    BankAccountSetting convertBankAccountSettingVO2BankAccountSetting(BankAccountSettingVO bankAccountSettingVO,BankAccountSetting bankAccountSetting){
        bankAccountSetting.setId(bankAccountSettingVO.getId());
        bankAccountSetting.setAccentity(bankAccountSettingVO.getAccentity());
        bankAccountSetting.setEnterpriseBankAccount(bankAccountSetting.getEnterpriseBankAccount());
        bankAccountSetting.setCustomNo(bankAccountSettingVO.getCustomNo());
        bankAccountSetting.setAccStatus(bankAccountSettingVO.getAccStatus());

        return bankAccountSetting;
    }

    @Override
    public Map queryBankAccountSettingAvailable(CommonRequestDataVo commonQueryData) throws Exception {
        CommonResponseDataVo result = new CommonResponseDataVo();
        List <String> enterpriseBankAccounts = commonQueryData.getIds();
        QuerySchema querySchema = QuerySchema.create().addSelect("enterpriseBankAccount,openFlag,empower");
        QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("enterpriseBankAccount").in(enterpriseBankAccounts));
        querySchema.addCondition(group);
        List<BankAccountSetting> bankAccountSettings = MetaDaoHelper.queryObject(BankAccountSetting.ENTITY_NAME, querySchema, null);
        //若没有结果 则全为false
        Map map = new HashMap();
        if(bankAccountSettings.isEmpty()){
            for(String id : enterpriseBankAccounts){
                map.put(id,false);
            }
        }else{
            for(BankAccountSetting vo : bankAccountSettings){
                if(vo.getOpenFlag() && EmpowerConstand.EMPOWER_QUERYANDPAY.equals(vo.getEmpower())){
                    map.put(vo.getEnterpriseBankAccount(),true);
                }else{
                    map.put(vo.getEnterpriseBankAccount(),false);
                }
            }
            //若参数中账户id的数量与 查询出的enterpriseBankAccounts数量不符 则说明有的账户没有查到数据 需要赋值为false
            if(enterpriseBankAccounts.size() != bankAccountSettings.size()){
                for(String id :enterpriseBankAccounts){
                    if(map.get(id) == null){
                        map.put(id,false);
                    }
                }
            }
        }
        return map;
    }
}
