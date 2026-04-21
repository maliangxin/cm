package com.yonyoucloud.fi.cmp.reconciliate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public interface AutoBillService {

    ObjectNode autoGenerateBill(JsonNode params) throws Exception;
}
