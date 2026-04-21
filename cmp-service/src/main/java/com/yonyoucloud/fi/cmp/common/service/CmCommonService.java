package com.yonyoucloud.fi.cmp.common.service;

import com.yonyou.ucf.basedoc.model.BdTaxRateVO;
import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVO;
import com.yonyou.ucf.basedoc.model.ExchangeRateTypeVO;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.FilterCommonVO;
import com.yonyou.ucf.mdd.ext.bill.billmake.vo.PushAndPullVO;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.transtype.model.BdTransType;
import com.yonyou.yonbip.ctm.cspl.vo.request.CapitalPlanExecuteModel;
import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cm.transferaccount.TransferAccount;
import com.yonyoucloud.fi.cmp.bankaccountsetting.BankAccountSetting;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.common.CommonRequestDataVo;
import com.yonyoucloud.fi.cmp.payapplicationbill.PayApplicationBill;
import com.yonyoucloud.fi.cmp.paybill.PayBill;
import com.yonyoucloud.fi.cmp.salarypay.Salarypay;
import org.imeta.orm.base.BizObject;

import java.util.*;

/**
 * @InterfaceName CmCommonService
 * @Desc 通用服务
 * @Author tongyd
 * @Date 2019/9/9
 * @Version 1.0
 */
public interface CmCommonService<T> {
    /*
     * @Author tongyd
     * @Description 获取本位币
     * @Date 2019/9/9
     * @Param [param]
     * @return java.lang.String
     **/
    String getNatCurrency(CtmJSONObject param) throws Exception;
    /*
     * @Author tongyd
     * @Description 获取汇率
     * @Date 2019/9/9
     * @Param [param]
     * @return java.lang.String
     **/
    String getExchangeRate(CtmJSONObject param) throws Exception;

    /*
     * @Author wsl
     * @Description 获取汇率集合
     * @Date 2019/9/9
     * @Param [param]
     * @return List<Map<String, Object>>
     **/
    List<Map<String, Object>> getExchangeRateList(CtmJSONObject param) throws Exception;

    /*
     * @Author tongyd
     * @Description 获取时间戳
     * @Date 2019/9/18
     * @Param [entityName, id]
     * @return java.util.Date
     **/
    Date getPubTsById(String entityName, Long id) throws Exception;

    /**
     * @Author tongyd
     * @Description 刷新单据时间戳
     * @Date 2019/10/24
     * @Param [entityName, ids, rows]
     * @return void
     **/
    void refreshPubTs(String entityName, List<Object> ids, CtmJSONArray rows) throws Exception;

    /**
     * 刷新单据时间戳  只查询主表数据*
     * @param entityName
     * @param ids
     * @param rows
     * @throws Exception
     */
    void refreshPubTsNew(String entityName, List<Object> ids, CtmJSONArray rows) throws Exception;

    /**
     * @Author tongyd
     * @Description 获取模块默认的业务账簿
     * @Date 2019/10/31
     * @Param [accEntity, moduleCode]
     * @return java.util.Map<java.lang.String,java.lang.Object>
     **/
    Map<String, Object> getModuleDefaultAccBook(String accEntity, String moduleCode) throws Exception;

    /**
     * @Author tongyd
     * @Description 根据条件获取期间方案
     * @Date 2019/10/31
     * @Param [condition]
     * @return java.util.Map<java.lang.String,java.lang.Object>
     **/
    Map<String, Object> getPeriodByCondition(Map<String, Object> condition) throws Exception;

    /**
     * 根据条件获取交易类型
     * @param condition
     * @return
     * @throws Exception
     */
    List<Map<String, Object>> getTransTypeByCondition(Map<String, Object> condition) throws Exception;

    /**
     * 根据数据权限获取对应交易类型
     * @param condition
     * @return
     * @throws Exception
     */
    List<Map<String, Object>> getTransTypeByDataPermission(Map<String, Object> condition) throws Exception;

    /**
     * 根据汇率类型获取汇率
     * @param param
     * @return
     * @throws Exception
     */
    String getExchangeRateByRateType(CtmJSONObject param) throws Exception;

    /**
     * 根据资金组织去查会计主体，再获取会计主体对应的业务账簿下默认的汇率类型
     * 入参是资金组织和会计主体都行，结果一样，拿到的都是会计主体对应的汇率类型
     * @param orgid
     * @return
     * @throws Exception
     */
    Map<String,Object> getDefaultExchangeRateType(String orgid) throws  Exception;

//    Map<String,Object> getDefaultExchangeRateType(String orgid , Boolean isFundOrg) throws  Exception;


    /**
     * 根据资金组织去查汇率类型
     * @param fundsOrgid
     * @return
     * @throws Exception
     */
    Map<String, Object> getDefaultExchangeRateTypeByFundsOrgid(String fundsOrgid) throws Exception;

    /**
     * 根据汇率类型code获取汇率类型信息*
     * @param code
     * @return
     * @throws Exception
     */
    ExchangeRateTypeVO getExchangeRateType(String code) throws Exception;

    /**
     * 根据会计主体获取总账下默认的汇率类型
     * @param orgid
     * @return
     * @throws Exception
     */
//    ExchangeRateTypeVO getDefaultExchangeRateTypeFromGl(String orgid) throws  Exception;


    /**
     * 获取组织
     * @param
     * @return
     * @throws Exception
     */
     Map<String, String> getOrgs() throws  Exception;

    /**
     * 根据银行流水号查询 付款工作台
     * @param bankDealdetails
     * @return
     * @throws Exception
     */
    List<Map<String,Object>> getPayBillByBankDealdetails(List<String> bankDealdetails)throws Exception;
    List<Map<String, Object>>  getProjectVOs(List<String> projectIds) throws Exception;
    List<Map<String, Object>> getBankDealdetailByBankSeqNos(List<String> bankSqeNos) throws Exception;

    /**
     * 根据页面启用日期判断数据是否启用“资金结算”
     * @param param
     * @return
     * @throws Exception
     */
    Boolean getEnabledPeriod (CtmJSONObject param)throws  Exception;


    /**
     * 根据币种id查询币种实体
     * @param param
     * @return
     * @throws Exception
     */
    String getCurrency(CtmJSONObject param) throws Exception;


    Map<String, Object> queryTransTypeById(String billtype, String def, String code) throws Exception;

    /**
     * 通过会计主体获取现金参数相关信息
     * @param accentity
     * @return
     * @throws Exception
     */
    Map<String, Object> queryAutoConfigByAccentity(String accentity) throws Exception;

    Map<String, Object> queryAutoConfigTenant() throws Exception;


    /**
     * 通过会计主体获取默认银行账户
     * @param orgId
     * @return
     * @throws Exception
     */
    Map<String, Object> getDefaultBankAccountByOrgId(String orgId, String currency) throws Exception;

    EnterpriseBankAcctVO getDefaultBankAccount(Object accentity, Object currency) throws Exception;

        /**
         * 获取税率
         * @param code
         * @param name
         * @return
         */
    String getTaxRate(String code,String name) throws Exception;

    /**
     * 获取税率
     * @param id
     * @return
     */
    BdTaxRateVO getTaxRateById(String id) throws Exception;

    /**
     * 获取税率 税局税种税率档案替换税目税率档案
     *
     * @param id
     * @return
     */
    String getTaxRateArchive(String id) throws Exception;

    List<PayApplicationBill> updateStatePayApplyBill(String flag, Set<Object> set) throws Exception;

    public int updatePayForPayStatus(PayBill payBill);

    public int updateSalaryForPayStatus(Salarypay salarypay) throws Exception;

    public int updateTransferAccountsForPayStatus(TransferAccount transferAccount) throws Exception;

    Map<String,Object> getVoucherInitBalMes(String accentity,String bankaccount,Long bankreconciliationscheme,String currency, short reconciliationDataSource) throws Exception;

    Map<String,Object> getUserIdByYhtUserId(String yhtUserId) throws Exception;

    /**
     * 根据单据类型form_id查询默认交易类型档案
     * @param form_id
     * @return
     * @throws Exception
     */
    Map<String, Object> queryTransTypeByForm_id(String form_id) throws Exception;

    List<Map<String, Object>> queryTransTypesByForm_ids(String code) throws Exception;

    String catBillType(String yhtTenantId,String busType) throws Exception;

    String getDefaultTransTypeCode(String busType) throws Exception;

    void setTransTypeValueForBizObject(BizObject bill, String formId);

    /**
     * 根据交易类型id查询交易类型档案信息*
     * @param busType
     * @return
     * @throws Exception
     */
    List<BdTransType> getTransTyp(String busType) throws Exception;

    /**
     * 通过资金组织给会计主体赋值
     * @param bill
     * @return
     * @throws Exception
     */
    void setAccentityRawForBizObject(BizObject bill, String formId);

    /**
     * <h2>根据组织过滤供应商和客户</h2>
     *
     * @param billDataDto :
     * @param bills :
     * @author Sun GuoCai
     * @since 2022/11/4 10:52
     */
    void filterMerchantRefAndVendorByOrg(BillDataDto billDataDto, List<BizObject> bills) throws Exception;


    /**
     * <h2>公共的单据转换规则</h2>
     *
     * @param vo :
     * @return java.util.List<java.util.Map < java.lang.String, java.lang.Object>>
     * @author Sun GuoCai
     * @since 2022/11/15 13:39
     */
    List<Map<String, Object>> commonBillConvertRuleHandler(PushAndPullVO vo, String subOriginName, String subTargetName) throws Exception;

    /**
     * 支持直接使用来源数据
     * @param pullVO
     * @return
     * @throws Exception
     */
    Map<String, Object> commonBillConvertRuleHandler(PushAndPullVO pullVO) throws Exception;

    /**
     * 资金计划组装参数
     *
     * @param biz
     * @param pushType
     * @param map
     * @return
     * @throws Exception
     */
    List<CapitalPlanExecuteModel> putCheckParameter(List<BizObject> biz, String pushType, String billnum, Map<String, Object> map) throws Exception;

    /**
     * 薪资支付适配资金计划项目：组装推送的参数*
     * @param biz
     * @param pushType
     * @param billnum
     * @param map
     * @return
     * @throws Exception
     */
    List<CapitalPlanExecuteModel> putCheckParameterSalarypay(List<BizObject> biz, String pushType, String billnum, Map<String, Object> map) throws Exception;

    /**
     * 薪资支付适配资金计划项目：组装推送的参数*
     * @param biz
     * @param pushType
     * @param billnum
     * @param map
     * @return
     * @throws Exception
     */
    List<CapitalPlanExecuteModel> putCheckParameterSalaryPayOldInterface(List<BizObject> biz, String pushType, String billnum, Map<String, Object> map) throws Exception;

    /**
     * 获取结算方式
     * @param id 主键
     * @return Integer数量
     * @throws Exception 异常
     */
    Integer getServiceAttr(Long id);

    /*
     *@Author rtsungc
     *@Description 根据会计主体，原币币种，查询资金计划参数是否受控
     *@Date 2022/4/27 16:28
     *@Param accentity,currency
     *@Return ControlSet
     **/
    Object queryStrategySetbByCondition(String accentity, String currency, Date expectdate) throws Exception;

    /**
     * 查询开通的服务，专用于银行对账单/认领单 业务关联和业务处理相关
     * @return
     */
    CtmJSONObject queryOpenServiceInfo();

    /**
     * <h2>获取并设置当前单据附件数</h2>
     *
     * @param bizObject : 当前单据数据
     * @author Sun GuoCai
     * @since 2023/7/3 10:27
     */
    void getFilesCount(BizObject bizObject);

    /**
     * <h2>保存并提交</h2>
     *
     * @param billDataDto : 实体入参
     * @return com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult
     * @author Sun GuoCai
     * @since 2023/10/13 21:30
     */
    RuleExecuteResult doSaveAndSubmitAction(BillDataDto billDataDto);

    /**
     * 批量获取结算方式
     * @param ids 主键
     * @return Integer数量
     * @throws Exception 异常
     */
    Map<Long,Integer> getServiceAttrs(List<Long> ids);

    /**
     * 根据参数获取银企联账号
     * @param param
     * @return
     * @throws Exception
     */
    List<BankAccountSetting> queryAutocorrsettingByParam(CommonRequestDataVo param)throws Exception;

    //银行账户是否开通银企联服务
    Boolean getOpenFlag(String bankaccount) throws Exception;

    /**
     * 根据服务编码集合获取租户开通服务的情况
     * @param param serviceCodeList 待校验的服务编码集合
     * @return 租户开通的服务结合
     * @throws Exception
     */
    Collection<String> checkOpenServiceList(CtmJSONObject param) throws Exception;

    void releaseFundPlanByCollection(BizObject bizObject) throws Exception;

    void releaseFundPlanByPayment(BizObject bizObject) throws Exception;

    /**
     * 境外账户过滤
     * @param orgValueList
     * @param classValueList
     * @param enterCountry
     * @param cashDirectLink
     * @return
     * @throws Exception
     */
    List<String> getBankAcctInfos(List<String> orgValueList, List<String> classValueList, Object enterCountry, Object cashDirectLink) throws Exception;


    /**
     * 列表
     * @param vo
     * @return
     */
    List<String> getValueList(FilterCommonVO vo);

    /**
     * 根据资金组织获取汇率
     * @param param
     * @return
     */
    Map<String,Object> getSwapOutExchangeRateName(CtmJSONObject param) throws Exception;

    /**
     * 根据银行账号获取账户使用组织
     * @param param bankAccounts 银行账户id的数组
     * @return 账户使用组织集合，orgList
     */
    CtmJSONObject queryUseOrgListByBankAccounts(CtmJSONObject param) throws Exception;
}
