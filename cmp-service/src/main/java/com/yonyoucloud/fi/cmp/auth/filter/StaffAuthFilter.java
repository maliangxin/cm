package com.yonyoucloud.fi.cmp.auth.filter;

import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.FilterVO;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.SimpleFilterVO;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.basecom.constant.IBillConst;
import com.yonyoucloud.fi.basecom.utils.AuthUtil;
import com.yonyoucloud.fi.basecom.utils.FIDubboUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.BizObject;

import java.util.*;

/**
 * @author qihaoc
 * @Description:公共的员工参照数据权限过滤
 * @date 2024/11/11 17:24
 */
public class StaffAuthFilter{

    public  static void filter(BillContext billContext, boolean iscard, String billnum,
                     String refcode, BizObject data, FilterVO filterVO, BillDataDto billDataDto) throws Exception {
        String key = billDataDto.getKey();
        Object ext =  billDataDto.getExternalData();
        if(ext instanceof HashMap){
            HashMap<String, Object> extDataMap = (HashMap<String, Object>) ext;
            if (extDataMap.get("containsLeavePersonFlag") != null
                    && extDataMap.get("containsLeavePersonFlag") instanceof Integer
                    && (Integer) extDataMap.get("containsLeavePersonFlag") == 1){
                extDataMap.put("containsEndJobInfo",true);
            }
        }
        if (data == null || data.get(IBillConst.ACCENTITY) == null || "".equals(data.get(IBillConst.ACCENTITY))) {
            return;
        }
        //员工参照启用自定义组织时，设置条件后直接返回
        Map<String, Object> context = billContext.getContext();
        ArrayList<String> orgIdList = (ArrayList<String>) context.get("staffCustomOrg");
        if(orgIdList != null && !orgIdList.isEmpty() && (
                "bd_staff_ref".equals(refcode)
                        || "ucf-staff-center.bd_staff_ref".equals(refcode)
                        || "ucf-staff-center.bd_staff_leave_ref".equals(refcode)) ){
            SimpleFilterVO simpleFilterVO = new SimpleFilterVO(ConditionOperator.and);
            simpleFilterVO.addCondition(new SimpleFilterVO("mainJobList.org_id", "in", orgIdList));
            filterVO.appendCondition(ConditionOperator.and, simpleFilterVO);
            return;
        }

        List<String> deptIds;
        String fullName = billDataDto.getFullname();
        // 固定资产卡片和期初卡片
        if (IBillConst.CMP_RECEIVEBILL_RECEIVEBILL_B.equals(fullName) && IBillConst.CMP_RECEIVEBILL.equals(billnum)) {
            deptIds = new ArrayList<>();
            List<BizObject> bodyItems = data.getBizObjects(IBillConst.RECEIVEBILL_B, BizObject.class);
            setDepartments(bodyItems, deptIds);
        } else if (IBillConst.CMP_PAYBILL_PAYBILLB.equals(fullName) && IBillConst.CMP_PAYMENT.equals(billnum)) {
            deptIds = new ArrayList<>();
            List<BizObject> bodyItems = data.getBizObjects(IBillConst.PAYAPPLICATIONBILL_B, BizObject.class);
            setDepartments(bodyItems, deptIds);
        } else if (IBillConst.CMP_PAYAPPLICATIONBILL_PAYAPPLICATIONBILL_B.equals(fullName) && IBillConst.CMP_PAYAPPLICATIONBILL.equals(billnum)) {
            deptIds = new ArrayList<>();
            List<BizObject> bodyItems = data.getBizObjects(IBillConst.PAYAPPLICATIONBILL, BizObject.class);
            setDepartments(bodyItems, deptIds);
        } else if (IBillConst.FUNDCOLLECTION_B_ENTITYNAME.equals(fullName) && iscard) {
            deptIds = new ArrayList<>();
            List<BizObject> bodyItems = data.getBizObjects(IBillConst.FUNDCOLLECTION_B, BizObject.class);
            setDepartments(bodyItems, deptIds);
        } else if (IBillConst.FUNDPAYMENT_B_ENTITYNAME.equals(fullName) && iscard) {
            deptIds = new ArrayList<>();
            List<BizObject> bodyItems = data.getBizObjects(IBillConst.FUNDPAYMENT_B, BizObject.class);
            setDepartments(bodyItems, deptIds);
        } else {
            deptIds = AuthUtil.getBizObjectAttr(data, IBillConst.DEPT) ;
        }
        List<String> orgIds = null;
        List<String> shareDeptIds = new ArrayList<>();; //共享部门
        List<String> bizObjectAttr = AuthUtil.getBizObjectAttr(data, IBillConst.ACCENTITY);
        // 部门为空，或者字段为员工参照时，取会计主体委托组织，共享组织
        if ((deptIds == null || deptIds.isEmpty()
                || "bd_staff_ref".equals(refcode)
                || "ucf-staff-center.bd_staff_ref".equals(refcode)
                || "ucf-staff-center.bd_staff_leave_ref".equals(refcode))
                && !CollectionUtils.isEmpty(bizObjectAttr)) {
            Set<String> delegateOrg;
            if (iscard) {
                // 委托组织
                delegateOrg = FIDubboUtils.getDelegateHasSelf(bizObjectAttr.get(0));
            } else {
                String[] deptArray = bizObjectAttr.toArray(new String[bizObjectAttr.size()]);
                delegateOrg = FIDubboUtils.getDelegateHasSelf(deptArray);
            }
            String[] delegateOrgArray = delegateOrg.toArray(new String[delegateOrg.size()]);
            // 共享部门
            Set<String> shareDept = FIDubboUtils.getDeptShare(delegateOrgArray);
            // 共享组织
            Set<String> shareOrg = FIDubboUtils.getOrgShareHasSelf(delegateOrgArray);
            delegateOrg.addAll(shareOrg);

            deptIds = new ArrayList<>();
            if (shareDept != null && !shareDept.isEmpty()) {
                deptIds.addAll(shareDept);
                shareDeptIds.addAll(shareDept);
            }
            orgIds = new ArrayList<>();
            orgIds.addAll(delegateOrg);
        }

        // 部门过滤(修改逻辑，业务员根据部门过滤，员工不根据部门过滤)
        if (deptIds != null && !deptIds.isEmpty() && "operator_name".equals(key)) {
            SimpleFilterVO simpleFilterVO = new SimpleFilterVO(ConditionOperator.or);
            if (orgIds != null && !orgIds.isEmpty()) {
                simpleFilterVO.addCondition(new SimpleFilterVO("mainJobList.org_id", "in", orgIds));
            }
            simpleFilterVO.addCondition(new SimpleFilterVO("mainJobList.dept_id", "in", deptIds));
            filterVO.appendCondition(ConditionOperator.and, simpleFilterVO);
        } else {
            // 员工过滤，职能共享部门+职能共享组织+核算委托组织
            SimpleFilterVO simpleFilterVO = new SimpleFilterVO(ConditionOperator.or);
            if (shareDeptIds != null && !shareDeptIds.isEmpty()) {
                simpleFilterVO.addCondition(new SimpleFilterVO("mainJobList.dept_id", "in", shareDeptIds));
            }
            if (orgIds != null && !orgIds.isEmpty()) {
                simpleFilterVO.addCondition(new SimpleFilterVO("mainJobList.org_id", "in", orgIds));
            }
            filterVO.appendCondition(ConditionOperator.and, simpleFilterVO);
        }

        filterVO.appendCondition(ConditionOperator.and, new SimpleFilterVO("mainJobList.dr", "eq", 0), new SimpleFilterVO("dr", "eq", 0));

        if("operator_name".equals(key)) {
            // 业务员标记额外添加，过滤掉离职人员
            filterVO.appendCondition(ConditionOperator.and, new SimpleFilterVO("biz_man_tag", "eq", true), new SimpleFilterVO("enable", "eq","1"),
                    new SimpleFilterVO("mainJobList.endflag", "eq", 0));
            // 业务员所属部门数据权限过滤
            Map<String, List<Object>> deptPerms = AuthUtil.dataPermission(billContext.getSubid(), fullName, null, new String[]{"dept"});
            if (deptPerms != null && deptPerms.size() > 0) {
                List<Object> permData = deptPerms.get("dept");
                if (CollectionUtils.isNotEmpty(permData)) {
                    filterVO.appendCondition(ConditionOperator.and, new SimpleFilterVO("mainJobList.dept_id", "in", permData));
                }
            }
        }
    }

    private static void setDepartments(List<BizObject> bodyItems, List<String> deptIds) {
        if(null != bodyItems && bodyItems.size() > 0){
            for (BizObject item : bodyItems) {
                if(item.get(IBillConst.DEPT) != null){
                    deptIds.add(item.get(IBillConst.DEPT));
                }
            }
        }
    }
}
