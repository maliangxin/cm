package com.yonyoucloud.fi.cmp.bankreconciliation.plugin;

import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.ucf.mdd.plugin.base.QuerySchemaExecutorPlugin;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.imeta.core.model.Entity;
import org.imeta.orm.schema.QueryOrderby;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author qihaoc
 * @Description: 1.背景:
 * 目前List查询当前端传入groupBy条件时,
 * 在查询分页信息的List接口中schema会qroupbyFields条件
 * 在查询业务数据的List接口中schema未groupbyFields条件
 * 2现在领域由于以上一个拼接了groupBy一个没拼导致分页数据对不上，现在要求支持在查询业务的List中也拼接groupBy
 * @date 2023/2/2 14:34
 */
@Service
public class QuerySchemaGroupExtendPlugin implements QuerySchemaExecutorPlugin {

    @Override
    public void extendQuerySchema(Entity entity, QuerySchema schema, BillContext billContext) {
        BillDataDto billDataDto = billContext.getParamObj();
        //根据当前用户查询数据权限，有数据权限才追加group条件
        if (!useDataPremisson()) {
            return;
        }
        //现在只有银行对账单列表查询需要添加group by
        String billnum = billContext.getBillnum();
        if (!IBillNumConstant.BANKRECONCILIATIONLIST.equals(billnum)) {
            return;
        }
        List<String> groupBy = billDataDto.getGroupBy();
        String groupByFiled = null;
        if (CollectionUtils.isNotEmpty(groupBy)) {
            String groupFields =  groupBy.toString();
            groupByFiled = groupFields.substring(1,groupFields.length()-1).replace(" ","");
        } else {
            groupByFiled = "id";
        }
        if (StringUtils.isNotEmpty(groupByFiled)) {
            schema.addGroupBy(groupByFiled);
            //银行对账单列表查询默认交易时间排序
            schema.addOrderBy(new QueryOrderby("tran_time","desc"));
        }
//        QueryPagerVo page = billDataDto.getPage();
//        int totalCount = page.getTotalCount();
//        //只有totalCount != -1，即查业务数据的list中才拼groupBy；totalCount = -1 ，查询分页信息的list接口中已经拼过了
//        if (StringUtils.isNotEmpty(groupByFiled) && totalCount != -1) {
//            schema.addGroupBy(groupByFiled);
//        }
    }

    @Override
    public int order() {
        return 1234;
    }

    public boolean useDataPremisson() {
        //使用银行对账单应用编码查询数据权限
//        DataPermissionRequestDto requestDto = new DataPermissionRequestDto();
//        requestDto.setEntityUri(BankReconciliation.ENTITY_NAME);
//        requestDto.setSysCode("");// 实体所属应用编码
//        requestDto.setYxyUserId(AppContext.getCurrentUser().getYxyUserId());
//        requestDto.setYxyTenantId(AppContext.getCurrentUser().getYxyTenantId());
//        requestDto.setHaveDetail(true); // 是否返回values
//        DataPermissionResponseDto res = null;
//        try {
//            res = AuthSdkFacadeUtils.getUserDataPermission(requestDto);
//        } catch (Exception e) {
//            return false;
//        }
//        if (res != null && !res.getDataPermissionMapList().isEmpty()) {
//            return true;
//        } else {
//            return false;
//        }
        //公有云暂时使用--start
        return false;
        //公有云暂时使用--end
        //海康暂时使用--start
//        return true;
        //海康暂时使用--end
    }
}