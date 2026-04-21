package com.yonyoucloud.fi.cmp.util.basedoc;

import com.yonyoucloud.fi.cmp.util.StringUtils;
import com.yonyoucloud.iuap.upc.api.IMerchantServiceV2;
import com.yonyoucloud.iuap.upc.dto.AgentFinancialDTO;
import com.yonyoucloud.iuap.upc.dto.AgentFinancialQryDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 该类主要用于查询客户档案信息
 */
@Service
public class CustomerQueryService {

    @Autowired
    IMerchantServiceV2 merchantService;

    /**
     * 根据客户银行账户id查询客户银行账户信息
     * @param id
     * @return
     * @throws Exception
     */
    public AgentFinancialDTO getCustomerAccountByAccountId(Long id) throws Exception {
        if(id == null){
            return null;
        }
        AgentFinancialQryDTO agentFinancialQryDTO = new AgentFinancialQryDTO();
        agentFinancialQryDTO.setId(id);
        String[] fields = {"id","merchantId","bankAccount","bankAccountName","currency","openBank","bank"};
        agentFinancialQryDTO.setFields(fields);
        List<AgentFinancialDTO> listMerchantAgentFinancial = merchantService.listMerchantAgentFinancial(agentFinancialQryDTO);
        if(listMerchantAgentFinancial!=null && listMerchantAgentFinancial.size()>0){
            return listMerchantAgentFinancial.get(0);
        }
        return null;
    }


    /**
     * 根据客户银行账号查询客户银行账户、网点、银行类型信息
     * @param accountNo
     * @return
     * @throws Exception
     */
    public AgentFinancialDTO getCustomerAccountByAccountNo(String accountNo) throws Exception {
        if(StringUtils.isEmpty(accountNo)){
            return null;
        }
        AgentFinancialQryDTO agentFinancialQryDTO = new AgentFinancialQryDTO();
        agentFinancialQryDTO.setBankAccount(accountNo);
        String[] fields = {"id","bankAccount","bankAccountName","currency","openBank","bank"};
        agentFinancialQryDTO.setFields(fields);
        List<AgentFinancialDTO> listMerchantAgentFinancial = merchantService.listMerchantAgentFinancial(agentFinancialQryDTO);
        if(listMerchantAgentFinancial!=null && listMerchantAgentFinancial.size()>0){
            return listMerchantAgentFinancial.get(0);
        }
        return null;
    }


}
