import org.apache.poi.ss.formula.eval.ValueEval;
import org.apache.poi.ss.formula.eval.*;


import java.util.ArrayList;
import java.util.List;

/**
 * Handling of a list of values, e.g. the 2nd argument in RANK(A1,(B1,B2,B3),1)
 */
public class RefListEval implements ValueEval {
    private final List<ValueEval> list = new ArrayList<>();

    public RefListEval(ValueEval v1, ValueEval v2) {
        add(v1);
        add(v2);
    }

    private void add(ValueEval v) {
        // flatten multiple nested RefListEval
        if(v instanceof RefListEval) {
            list.addAll(((RefListEval)v).list);
        } else {
            list.add(v);
        }
    }

    public List<ValueEval> getList() {
        return list;
    }
}
