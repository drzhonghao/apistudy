import org.apache.karaf.imports.core.internal.*;


import org.apache.karaf.packages.core.internal.filter.AndExpression;
import org.apache.karaf.packages.core.internal.filter.Expression;
import org.apache.karaf.packages.core.internal.filter.FilterParser;
import org.apache.karaf.packages.core.internal.filter.NotExpression;
import org.apache.karaf.packages.core.internal.filter.SimpleItem;


/**
 * Helps to parse the expression
 * 
 * This class is internal to hide the FilterParser in the PackageService api
 */
class ImportDetails {
    String name;
    String minVersion;
    String maxVersion;

    public ImportDetails(String filter) {
        Expression filterExpr = new FilterParser().parse(filter);
        if (filterExpr instanceof AndExpression) {
            AndExpression andExpr = (AndExpression)filterExpr;
            for (Expression expr : andExpr.expressions) {
                parseSimpleItem(expr);
            }
        }
        parseSimpleItem(filterExpr);
    }

    private void parseSimpleItem(Expression expr) {
        if (expr instanceof SimpleItem) {
            SimpleItem simpleItem = (SimpleItem)expr;
            if ("osgi.wiring.package".equals(simpleItem.attr)) {
                this.name = simpleItem.value;
            }
            if ("version".equals(simpleItem.attr)) {
                this.minVersion = simpleItem.value;
            }
        }
        if (expr instanceof NotExpression) {
            SimpleItem simpleItem = (SimpleItem)((NotExpression)expr).expression;
            this.maxVersion = simpleItem.value;
        }
    }
}
