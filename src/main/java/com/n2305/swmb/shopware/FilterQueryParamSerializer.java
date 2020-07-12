package com.n2305.swmb.shopware;

import com.n2305.swmb.shopware.ShopwareAPI.Filter;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class FilterQueryParamSerializer {
    public Map<String, String> serialize(List<Filter> filters) {
        HashMap<String, String> params = new LinkedHashMap<>();

        for (int i = 0; i < filters.size(); i++) {
            Filter filter = filters.get(i);

            addFilterToParams(params, i, filter);
        }
        
        return params;
    }

    private void addFilterToParams(HashMap<String, String> params, int idx, Filter filter) {
        params.put(formatKey(idx, "property"), filter.getProperty());
        if (filter.getValue() != null) params.put(formatKey(idx, "value"), filter.getValue());
        params.put(formatKey(idx, "expression"), filter.getExpression());

        if (filter.getOperator()) {
            params.put(formatKey(idx, "operator"), "1");
        }
    }

    private String formatKey(int index, String key) {
        return String.format("filter[%d][%s]", index, key);
    }
}
