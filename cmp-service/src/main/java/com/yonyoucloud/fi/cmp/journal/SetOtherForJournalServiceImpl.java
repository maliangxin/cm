package com.yonyoucloud.fi.cmp.journal;

import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyoucloud.fi.basecom.service.ref.OrgRpcService;
import com.yonyoucloud.fi.basecom.service.ref.SupplierRpcService;
import com.yonyoucloud.fi.cmp.cmpentity.CaObject;
import com.yonyoucloud.fi.cmp.util.AccentityUtil;
import com.yonyoucloud.fi.cmp.util.QueryBaseDocUtils;
import com.yonyoucloud.iuap.upc.dto.MerchantDTO;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class SetOtherForJournalServiceImpl {
    
    @Service
    class SetOtherForJournalSupplier implements  SetOtherForJournalService{

        //供应商
        @Override
        public void setOtherInfo(Journal journal) throws Exception {
            SupplierRpcService supplierRpcService = AppContext.getBean(SupplierRpcService.class);
            journal.setCaobject(CaObject.Supplier);
            journal.setOthertitle(supplierRpcService.getVendorById(journal.getSupplier()).getName());
        }
    }


    @Service
    class SetOtherForJournalCustomer implements  SetOtherForJournalService{
        //客户
        @Override
        public void setOtherInfo(Journal journal) throws Exception {
            journal.setCaobject(CaObject.Customer);
            MerchantDTO merchantById = QueryBaseDocUtils.getMerchantById(journal.getCustomer());
            if (merchantById != null) {
                journal.setOthertitle(merchantById.getName());
            }
        }
    }

    @Service
    class SetOtherForJournalEmployee implements  SetOtherForJournalService{
        //员工
        @Override
        public void setOtherInfo(Journal journal) throws Exception {
            journal.setCaobject(CaObject.Employee);
            journal.setOthertitle(QueryBaseDocUtils.queryStaffById(journal.getEmployee()).get("name").toString());
        }
    }

    @Service
    class SetOtherForJournalInnerunit implements  SetOtherForJournalService{
        //内部单位
        @Override
        public void setOtherInfo(Journal journal) throws Exception {
//            //OrgRpcService orgRpcService = AppContext.getBean(OrgRpcService.class);
            journal.setCaobject(CaObject.InnerUnit);
            //内部单位就是会计主体 但这里需要查询自己组织本身 按初版修改使用AccentityUtil.getFinOrgDTOByAccentityId 如果当前资金组没有上游会计主体会报错
            journal.setOthertitle(AccentityUtil.getFundsOrgDTOByAccentityId(journal.getInnerunit()).getName());
        }
    }

    @Service
    class SetOtherForJournalOthername implements  SetOtherForJournalService{
        //其他
        @Override
        public void setOtherInfo(Journal journal) throws Exception {
            //其他为文本 不需要要查询直接赋值
            journal.setOthertitle(journal.getOthername());
            journal.setCaobject(CaObject.Other);
        }
    }

    @Service
    class SetOtherForJournalCapBizObj implements  SetOtherForJournalService{
        //资金业务对象
        @Override
        public void setOtherInfo(Journal journal) throws Exception {
            journal.setCaobject(CaObject.CapBizObj);
            Map<String, Object>  map = QueryBaseDocUtils.queryCapBizObjId(journal.getCapBizObj());
            if(map != null){
                journal.setOthertitle(map.get("name").toString());
            }
        }
    }
}
