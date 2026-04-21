package com.yonyoucloud.fi.cmp.bankidentify;

import com.yonyou.iuap.ruleengine.dto.relevant.TargetRuleInfoDto;
import com.yonyou.ucf.mdd.ext.base.tenant.Tenant;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.bankidentifysetting.BankreconciliationIdentifySetting;
import com.yonyoucloud.fi.cmp.bankidentifysetting.BankreconciliationIdentifySetting_b;
import com.yonyoucloud.fi.cmp.bankidentifytype.BankreconciliationIdentifyType;
import com.yonyoucloud.fi.cmp.cmpentity.BankIdentifyTypeEnum;
import com.yonyoucloud.fi.cmp.cmpentity.DcFlagEnum;
import com.yonyoucloud.fi.cmp.enums.ApplyObjectNewEnum;

import java.util.List;
import java.util.Map;

/**
 * @description: 流水辨识规则相关处理接口
 * @author: wanxbo@yonyou.com
 * @date: 2024/7/5 9:25
 */

public interface BankIdentifyService {

    /**
     * 更新辨识匹配规则类型和辨识匹配规则设置的启停用状态
     * @param param type=1规则启停用；type=2规则配置详情启停用
     * id:单据id；enablestatus修改后的启用状态
     * @return
     * @throws Exception
     */
    CtmJSONObject updateStatus(CtmJSONObject param) throws Exception;

    /**
     * 根据业务对象编码查询相关性规则
     * @param param bizObjectCode=业务对象编码
     * @return
     * @throws Exception
     */
    CtmJSONObject queryRelevantRuleByBizCode(CtmJSONObject param) throws Exception;

    /**
     * 流水辨识匹配类型排序接口
     * @param param
     * @return
     * @throws Exception
     */
    CtmJSONObject sortIdentifyTypeExcuteOrder(CtmJSONObject param) throws Exception;

    /**
     * 预置流水自动辨识匹配规则数据
     * @param tenant
     * @return
     * @throws Exception
     */
    CtmJSONObject initIdentifyTypeData(String remark,Tenant tenant) throws Exception;


    /**
     * 预置流水自动辨识匹配规则数据
     * @param tenant
     * @return
     * @throws Exception
     */
    CtmJSONObject btwInitSystem004ItemDataNew(Tenant tenant) throws Exception;


    CtmJSONObject btwInitSystem005ItemDataNew(Tenant tenant) throws Exception;
    /**
     * 根据编码查询流水自动辨识匹配规则类型
     * @param code
     * @return
     * @throws Exception
     */
    BankreconciliationIdentifyType queryIdentifyTypeByCode(String code) throws Exception;

    /**
     * 根据类型查询流水自动辨识匹配规则类型
     * @param type BankIdentifyTypeEnum 类型具体枚举
     * @return
     * @throws Exception
     */
    BankreconciliationIdentifyType queryIdentifyTypeByType(Short type) throws Exception;

    /**
     * 根据编码查询流水自动辨识匹配规则设置
     * @param code 辨识匹配规则类型
     * @return
     * @throws Exception
     */
    List<BankreconciliationIdentifySetting> querySettingsByCode(String code) throws Exception;

    /**
     * 根据相关性规则标识设置ID查询关联的相关性规则
     * @param settingId
     * @return
     */
    Map<Integer, TargetRuleInfoDto> loadRuleBySettingId(String settingId)  throws Exception;

    /**
     * 获取执行脚本的相关性规则
     * @param settingId
     * @return
     */
     Map<String, BankreconciliationIdentifySetting_b> loadRuleBySetting_bById(String settingId) throws Exception;

    Map<String, BankreconciliationIdentifySetting_b> loadBankreconciliationIdentifySetting_bById(String settingId) throws Exception;
    /**
     * 银行流水辨识数据启用/停用
     * @param param type=1规则启停用；type=2规则配置详情启停用
     * id:单据id；enablestatus修改后的启用状态
     * @return
     * @throws Exception
     */
    CtmJSONObject updateSettingStatus(CtmJSONObject param) throws Exception;

    /**
     * 收付单据匹配节点根据组织查找流水处理规则
     * 预置规则 > 组织级 （非预置）> 企业账号级（非预置），然后在这个大的排序逻辑上再根据优先级排序；
     * @param org
     * @return
     * @throws Exception
     */
    List<BankreconciliationIdentifySetting> querySettingsByOrg(String org,String dcFlag) throws Exception;

    void deleteInitData(Tenant tenant) throws Exception;
}
