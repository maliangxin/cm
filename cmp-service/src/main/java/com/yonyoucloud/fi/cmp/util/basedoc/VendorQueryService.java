package com.yonyoucloud.fi.cmp.util.basedoc;

import com.yonyoucloud.fi.basecom.annotation.CtmRecord;
import com.yonyoucloud.upc.pub.api.vendor.service.vendor.IVendorPubQueryService;
import com.yonyoucloud.upc.pub.api.vendor.vo.vendor.VendorBankVO;
import com.yonyoucloud.upc.pub.api.vendor.vo.vendor.VendorExtendVO;
import com.yonyoucloud.upc.pub.api.vendor.vo.vendor.VendorOrgVO;
import com.yonyoucloud.upc.pub.api.vendor.vo.vendor.VendorVO;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 *
 * 查询供应商档案万能接口
 *
 *
 */
@Service
public class VendorQueryService {

    @CtmRecord(type = "yssupplier, productcenter")
    @Autowired
    IVendorPubQueryService vendorPubQueryService;
    /**
     * 根据id查询供应商基本信息
     * @param id 供应商档案id
     * @return 供应商
     * @throws Exception 集合
     */
    @CtmRecord(type = "yssupplier")
    public VendorVO getVendorById(Long id) throws Exception {
        return vendorPubQueryService.getVendorById(id);
    }

    /**
     * 根据ids查询供应商基本信息
     *
     * @param ids 供应商档案id
     * @return 供应商
     * @throws Exception 集合
     */
    @CtmRecord(type = "yssupplier")
    public List<VendorVO> getVendorFieldByIdList(List<Long> ids) throws Exception {
        List<String> fields = new ArrayList<>();
        fields.add("id");
        fields.add("code");
        fields.add("name");
        return vendorPubQueryService.getVendorFieldByIdList(ids, fields.toArray(new String[0]));
    }

    /**
     * 根据编码集合查询供应商基本信息
     *
     * @param codeList 供应商编码集合
     * @return 供应商
     * @throws Exception 集合
     */
    @CtmRecord(type = "yssupplier")
    public List<VendorVO> getVendorFieldByCodeList(List<String> codeList) throws Exception {
        List<String> fields = new ArrayList<>();
        fields.add("id");
        fields.add("code");
        fields.add("name");
        return vendorPubQueryService.getVendorFieldByCodeList(codeList, fields.toArray(new String[0]));
    }
    //begin ctm wangzc 20240801 牧原根据导入收款单位名称匹配数据
    /**
     * 根据编码集合查询供应商基本信息
     *
     * @param codeList 供应商名称集合
     * @return 供应商
     * @throws Exception 集合
     */
    @CtmRecord(type = "yssupplier")
    public List<VendorVO> getVendorFieldByNameList(List<String> codeList) throws Exception {
        List<String> fields = new ArrayList<>();
        fields.add("id");
        fields.add("code");
        fields.add("name");
        return vendorPubQueryService.getVendorFieldByNameList(codeList, fields.toArray(new String[0]));
    }
    //end
    /**
     * 根据供应商idList和使用组织idList查询业务信息-自选返回字段
     *
     * @param vendorIdList 供应商idList
     * @param orgIdList 使用组织idList
     * @return 供应商
     * @throws Exception 集合
     */
    @CtmRecord(type = "yssupplier")
    public List<VendorExtendVO> getVendorExtendFieldByVendorIdListAndOrgIdList(List<Long> vendorIdList, List<String> orgIdList) throws Exception {
        List<String> fields = new ArrayList<>();
        fields.add("department.id");
        fields.add("department.name");
        fields.add("person.id");
        fields.add("person.name");
        fields.add("vendor.id");
        fields.add("vendor.name");
        fields.add("stopstatus");
        return vendorPubQueryService.getVendorExtendFieldByVendorIdListAndOrgIdList(vendorIdList, orgIdList, fields.toArray(new String[0]));
    }

    /**
     * 根据条件查询供应商账户信息
     * @param condition 供应商账户查询条件
     * @return 供应商
     * @throws Exception 集合
     */
    @CtmRecord(type = "yssupplier")
    public List<VendorBankVO> getVendorBanksByCondition(Map<String, Object> condition) throws Exception {
        return vendorPubQueryService.getVendorBanksByCondition(condition);
    }


    /**
     * 根据条件查询供应商账户信息
     * @param id 供应商账户id
     * @return 供应商
     * @throws Exception 集合
     */
    @CtmRecord(type = "yssupplier")
    public VendorBankVO getVendorBanksByAccountId(Object id) throws Exception {
        Map<String, Object> condition = new HashMap<>();
        condition.put("id",id);
        List<VendorBankVO> vendorBankVOs = vendorPubQueryService.getVendorBanksByCondition(condition);
        if(vendorBankVOs != null && vendorBankVOs.size() >0){
            return vendorBankVOs.get(0);
        }
        return null;
    }

    /**
     * 根据条件查询供应商账户信息
     * @param id 供应商账户id
     * @return 供应商
     * @throws Exception 集合
     */
    @CtmRecord(type = "yssupplier")
    public VendorBankVO getVendorBanksByAccountIdOrCode(Object id) throws Exception {
        List<String> ids = new ArrayList<>();
        ids.add(id.toString());
        List<VendorBankVO> vendorBankVOs = vendorPubQueryService.getVendorBanksByAccountList(ids, false, false, "*");
        if (CollectionUtils.isNotEmpty(vendorBankVOs)) {
            return vendorBankVOs.get(0);
        }
        Map<String, Object> condition1 = new HashMap<>();
        condition1.put("id", id);
        List<VendorBankVO> vendorBankVOsByCode = vendorPubQueryService.getVendorBanksByCondition(condition1);
        if (CollectionUtils.isNotEmpty(vendorBankVOsByCode)) {
            return vendorBankVOsByCode.get(0);
        }
        return null;
    }

    /**
     * 根据条件查询供应商账户信息
     * @param id 供应商账户id
     * @return 供应商
     * @throws Exception 集合
     */
    @CtmRecord(type = "yssupplier")
    public List<VendorOrgVO> getVendorOrgByVendorId(Long id) throws Exception {
        return vendorPubQueryService.getVendorOrgByVendorId(id);
    }

    /**
     * 根据供应商id查询分配关系
     * @param vendorIdList 供应商id
     * @return 供应商
     * @throws Exception 集合
     */
    @CtmRecord(type = "yssupplier")
    public List<VendorOrgVO> getVendorOrgByVendorIdList(List<Long> vendorIdList) throws Exception {
        return vendorPubQueryService.getVendorOrgByVendorIdList(vendorIdList);
    }


    /**
     * 根据供应商供应商账号id查询供应商账号集合
     * @param ids
     * @return
     * @throws Exception
     */
    @CtmRecord(type = "yssupplier")
    public List<VendorBankVO> getVendorBanksByAccountList(List<Long> ids) throws Exception {
        /*
        自选返回字段，目前只需要卡号 account
        List<VendorBankVO> getVendorBanksByIdList(List<Long> idList,String... fields) throws Exception;
         */
        return vendorPubQueryService.getVendorBanksByIdList(ids,"account");
    }

    /**
     * 根据供应商供应商账号id查询供应商账号集合
     * @param ids
     * @param fields
     * @return
     * @throws Exception
     */
    @CtmRecord(type = "yssupplier")
    public List<VendorBankVO> getVendorBanksByAccountList(List<Long> ids, String... fields) throws Exception {
        return vendorPubQueryService.getVendorBanksByIdList(ids,fields);
    }

    /**
     * 根据供应商名称查询供应商
     * @return
     * @throws Exception
     */
    @CtmRecord(type = "yssupplier")
    public List<VendorVO> getVendorFieldByName(String name) throws Exception {
        List<String> fields = new ArrayList<>();
        fields.add("id");
        fields.add("code");
        fields.add("name");
        return vendorPubQueryService.getVendorFieldByName(name,false, fields.toArray(new String[0]));
    }


    /**
     * 判断供应商是否分配给相应组织
     * @return
     * @throws Exception
     */
    public boolean judgeVendorOrg(Long vendorId, String orgIdStr) throws Exception{
        Long orgId = Long.valueOf(orgIdStr);
        return vendorPubQueryService.judgeVendorOrg(vendorId, orgId);
    }

    /**
     * 根据供应商id查询供应商账户信息
     * @return
     * @throws Exception
     */
    public List<VendorBankVO> getVendorBanksByVendorId(Long vendorId, Boolean bDefaultBank) throws Exception {
        return vendorPubQueryService.getVendorBanksByVendorId(vendorId, bDefaultBank);
    }

}
