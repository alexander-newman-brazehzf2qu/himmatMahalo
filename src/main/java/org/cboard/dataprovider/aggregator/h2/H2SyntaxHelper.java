package org.cboard.dataprovider.aggregator.h2;

import org.cboard.dataprovider.config.ValueConfig;
import org.cboard.dataprovider.util.SqlSyntaxHelper;

/**
 * Created by zyong on 2017/9/18.
 */
public class H2SyntaxHelper extends SqlSyntaxHelper {

    @Override
    public String getAggStr(ValueConfig vConfig) {
        String aggExp = vConfig.getColumn();
        switch (vConfig.getAggType()) {
            case "sum":
                return "SUM(f_tofloat(" + aggExp + "))";
            case "avg":
                return "AVG(f_tofloat(" + aggExp + "))";
            case "max":
                return "MAX(" + aggExp + ")";
            case "min":
                return "MIN(" + aggExp + ")";
            case "distinct":
                return "COUNT(DISTINCT " + aggExp + ")";
            default:
                return "COUNT(" + aggExp + ")";
        }
    }
}
