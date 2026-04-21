package com.yonyoucloud.fi.cmp.autoparam.common;

import com.yonyou.ucf.mdd.common.model.Pager;

import java.util.*;

public class BillActionUtils {
    private static final String CHILDREN = "children";
    private static final String SORT = "sort";

    public BillActionUtils() {
    }

    public static List<Map<String,Object>> buildTree(List<Map<String, Object>> pageData, Boolean isExtTreeSort) {
        List<Map<String,Object>> needRemoveList = new ArrayList<>();
        List<Map<String, Object>> dataMapList = pageData;
        if (dataMapList != null && dataMapList.size() != 0) {
            Map<String, Map<String, Object>> treeDOMap = new HashMap();
            Iterator var5 = dataMapList.iterator();

            while (var5.hasNext()) {
                Map<String, Object> billDO = (Map) var5.next();
                treeDOMap.put(billDO.get("id").toString(), billDO);
            }

            List<Map<String, Object>> rootITreeDOList = new ArrayList();
            Iterator var12 = dataMapList.iterator();

            while (true) {
                while (var12.hasNext()) {
                    Map<String, Object> dataMap = (Map) var12.next();
                    Object parentCorrelatedValue = dataMap.get("parent");
                    if (parentCorrelatedValue != null && !Objects.isNull(treeDOMap.get(parentCorrelatedValue.toString()))) {
                        Map<String, Object> parentDO = treeDOMap.get(parentCorrelatedValue.toString());
                        if (parentDO != null) {
                            List<Map> children = (List) parentDO.get("children");
                            if (children == null) {
                                children = new ArrayList();
                            }

                            (children).add(dataMap);
                            parentDO.put("children", children);
                            needRemoveList.add(dataMap);
                        }
                    } else {
                        rootITreeDOList.add(dataMap);
                    }
                }

                if (null == isExtTreeSort || !isExtTreeSort) {
                    sortTreeList(rootITreeDOList);
                }
                return needRemoveList;
            }
        }
        return needRemoveList;
    }

    private static void sortTreeList(List<Map<String, Object>> rootITreeDOList) {
        Collections.sort(rootITreeDOList, new Comparator<Map<String, Object>>() {
            public int compare(Map<String, Object> o1, Map<String, Object> o2) {
                Object sort1 = o1.get("sort");
                Object sort2 = o2.get("sort");
                if (sort1 == null && sort2 != null) {
                    return -1;
                } else if (sort1 != null && sort2 == null) {
                    return 1;
                } else {
                    return sort1 != null && sort2 != null ? Integer.valueOf(sort1.toString()).compareTo(Integer.valueOf(sort2.toString())) : 0;
                }
            }
        });
        Iterator var1 = rootITreeDOList.iterator();

        while (var1.hasNext()) {
            Map<String, Object> parentTreeDo = (Map) var1.next();
            List<Map<String, Object>> children = (List) parentTreeDo.get("children");
            if (children != null && children.size() > 0) {
                sortTreeList(children);
            }
        }

    }
}
