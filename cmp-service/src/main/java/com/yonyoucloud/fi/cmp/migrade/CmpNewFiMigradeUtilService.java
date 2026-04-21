package com.yonyoucloud.fi.cmp.migrade;

import com.yonyou.ucf.basedoc.model.BankdotVO;
import com.yonyou.ucf.transtype.model.BdTransType;
import com.yonyoucloud.iuap.upc.dto.AgentFinancialDTO;
import com.yonyoucloud.iuap.upc.dto.MerchantDTO;
import com.yonyoucloud.upc.pub.api.vendor.vo.vendor.VendorBankVO;
import com.yonyoucloud.upc.pub.api.vendor.vo.vendor.VendorVO;
import org.imeta.orm.base.BizObject;

import java.util.Date;
import java.util.List;
import java.util.Map;


public interface CmpNewFiMigradeUtilService {

    /**
     * 获取客户信息
     * @param customerIdList
     * @return
     * @throws Exception
     */
    Map<Long,MerchantDTO> buildCustomerMap(List<Long> customerIdList) throws Exception;

    /**
     * 获取供应商信息
     * @param supplierIdList
     * @return
     * @throws Exception
     */
    Map<Long, VendorVO> buildSupplierMap(List<Long> supplierIdList) throws Exception;

    /**
     * 获取员工信息
     * @param employeeIdList
     * @return
     * @throws Exception
     */
    Map<String,Map<String, Object>>  buildEmployeeMap(List<Object> employeeIdList)throws Exception;


    /**
     * 获取客户银行账户信息
     * @param customerbankaccountIdList
     * @return
     * @throws Exception
     */
    Map<Long,AgentFinancialDTO> buildCustomerBankMap(List<Long> customerbankaccountIdList) throws Exception;

    /**
     * 获取供应商银行账户信息
     * @param supplierbankaccountIdList
     * @return
     * @throws Exception
     */
    Map<Long, VendorBankVO> buildsupplierBankMap(List<Long> supplierbankaccountIdList) throws Exception;

    /**
     * 获取员工银行账户
     * @param employeeankAccountIdList
     * @return
     * @throws Exception
     */
    Map<String, Map<String, Object>> bulidEmployeeBankMap(List<Object> employeeankAccountIdList) throws Exception;

    /**
     * 获取对应银行
     * @param ids
     * @return
     * @throws Exception
     */
    Map<String, BankdotVO> bankdotMap( List<String> ids) throws Exception;

    /**
     * 获取当前期间的起始日期
     * @param schema_fullname
     * @return
     * @throws Exception
     */
    Date periodBeginDate(String schema_fullname) throws Exception;

    /**
     * 升级前校验(点击升级时 强校验)
     * @param schema_fullname
     * @return
     * @throws Exception
     */
    Map<String ,Boolean> checkBeforeUpgrade(String schema_fullname) throws Exception;


    Map<String,Map <String , BdTransType>> getAllTransTypeMap()throws Exception;
    /**
     * 查询 收付工作台、资金收付的对应交易类型
     * @return
     * @throws Exception
     */
    String queryBdTransType(String id,Map<String, Map<String, BdTransType>> paramMap)throws Exception;

    List<List<String>> groupData(List<String> listNow, int groupNum);

    /**
     * 特征分配
     * @param nameList 特征名称集合
     * @param newUri   需要分配的实体
     * @return taskIdList 用于后续查询分配是否成功
     * @throws Exception
     */
    List<String> assignCharMetaInfo(List<String> nameList, String newUri) throws Exception;

    /**
     * 根据taskId 查询特征是否分配完成
     * @param taskId
     * @return key：taskid ； value：boolean
     */
    Map<String,Boolean>queryCharactersAssignResult(List<String> taskId);


    /**
     * 推结算后 相关单据进行凭证生成
     * @param bizBill
     * @param bodyFullName
     * @throws Exception
     */
    void afterPushSettleVoucher(BizObject bizBill, String bodyFullName)throws Exception;

    /**
     * 校验交易类型是否升级完成
     * @param schema_fullname
     * @param resultMap
     * @throws Exception
     */
    void checkBeforeUpgradeTransType(String schema_fullname,Map<String , Boolean> resultMap) throws Exception;
}



