package com.yonyoucloud.fi.cmp.bankaccountsetting;

import com.fasterxml.jackson.databind.JsonNode;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.bankAccountSetting.BankAccountSettingVO;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.common.CommonRequestDataVo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by sz on 2019/4/20 0020.
 */
public interface BankAccountSettingService {

    /*
     * @Author tongyd
     * @Description 启用/停用
     * @Date 2019/9/12
     * @Param [param]
     * @return java.lang.String
     **/
    String accountQyTy(JsonNode param) throws Exception;

    /**
     * 启用停用电票服务
     *
     * @param param
     * @return
     * @throws Exception
     */
    String accountQyTyT(JsonNode param) throws Exception;

    /*
     * @Author tongyd
     * @Description 获取开通银企联标识
     * @Date 2019/9/23
     * @Param [bankAccountId]
     * @return java.lang.Boolean
     **/
    Boolean getOpenFlagByBankAccountId(String bankAccountId) throws Exception;

    Boolean getOpenFlagByBankAccountIdOfQuery(String bankAccountId) throws Exception;

    /**
     * 获取客户号并更新银企联账户设置里的客户号
     *
     * @param param
     * @return
     * @throws Exception
     */
    String updateCustomNo(CtmJSONObject param) throws Exception;

    /**
     * 同步账户
     *
     * @return
     */
    int syncAccount();

    /**
     * 根据银行账户、名称获取是否开通银企联
     *
     * @return
     */
    String getOpenFlag(String bankAccountId) throws Exception;

    /**
     * 银企联账号-直连启用日期设置
     * @param param
     * @return
     * @throws Exception
     */
    String accountEnableDateSet(JsonNode param) throws Exception;

    /**
     * 查询银企联直连账号启用日期
     * @param param
     * @return
     * @throws Exception
     */
    String queryEnableDate(JsonNode param) throws Exception;
    /**
     * 根据银行账户获取客户号
     * @param bankAccountId
     * @return
     * @throws Exception
     */
    String getCustomNoByBankAccountId(String bankAccountId) throws Exception;

    /**
     * 根据银行账户获取客户号并批量获取
     * @param bankAccountId
     * @return
     * @throws Exception
     */
    HashMap<String,String> batchGetCustomNoByBankAccountId(List<String> bankAccountId) throws Exception;

    /**
     * 根据银行账户获取客户号并校验
     * @param bankAccountId
     * @return
     * @throws Exception
     */
    String getCustomNoAndCheckByBankAccountId(String bankAccountId, Object customNo) throws Exception;

    /**
     * 根据银行账户获取客户号并根据条件校验Ukey
     * @param bankAccountId
     * @return
     * @throws Exception
     */
    String getCustomNoAndCheckUKeyByBankAccountId(Boolean isNeedCheckUkey, String bankAccountId, Object customNo) throws Exception;


    /**
     * 跟据账户id返回 账号:是否启用直连的json
     * @param commonQueryData
     * @return
     * @throws Exception
     */
    Map<String , Boolean> getOpenFlagReMap(CommonRequestDataVo commonQueryData)throws Exception;
    /**
     * * 根据使用组织查询总账期初余额信息
     * @param bankAccountSettingVO
     * @return
     * @throws Exception
     */
    Map<String,Object> getVoucherBalance(BankAccountSettingVO bankAccountSettingVO) throws Exception;
    /**
     * * 根据使用组织查询总账期初余额信息
     * @param bankAccountSettingVO
     * @return
     * @throws Exception
     */
    Map<String, Object> getCoinitloribalanceByAccentity(BankAccountSettingVO bankAccountSettingVO) throws Exception;

    public List<Map<String, Object>> queryBankAccountSettingByBankAccounts(List<String> enterpriseBankAccounts) throws Exception ;

    /**
     * * 查询企业日记账余额:授权使用组织和余额
     * @param openingOutstandingId 1 期初未达id
     * @param bankAccountSettingVO
     * @return
     */
    List<Map<String,Object>> getUseOrgAndMny(String openingOutstandingId, BankAccountSettingVO bankAccountSettingVO) throws Exception;

    /**
     * @description: 根据银企联开通标识查询账户直连状态的信息
     * @author: wenyuhao
     * @date: 2023/12/18 14:11
     * @param: [OpenFlag]
     * @return: java.util.List<com.yonyoucloud.fi.cmp.bankaccountsetting.BankAccountSetting>
     **/
    List<Map<String,Object>> listBankAccountSettingByOpenFlag(Boolean openFlag)throws Exception;

    /**
     * @description: 查询出开通了银企联的账号作为过滤条件
     * @author: wenyuhao
     * @date: 2023/12/18 14:29
     * @param: []
     * @return: java.util.List<java.lang.String>
     **/
    List<String> queryBankAccountSettingByFlag() throws Exception;

    /**
     * @description: 查询出账户直联开通设置中直联授权权限为“查询及支付”=是且开通了银企联的账号
     * @return: java.util.List<java.lang.String>
     **/
    List<String> queryBankAccountSettingByDirect() throws Exception;


    /**
     * 校验对账方案明细是否关联已勾对数据
     * @param param id:子表明细id
     * @return isContainChecked ：是否包含已关联
     * @throws Exception
     */
    CtmJSONObject checkReconciliationInfo(CtmJSONObject param) throws Exception;
}
