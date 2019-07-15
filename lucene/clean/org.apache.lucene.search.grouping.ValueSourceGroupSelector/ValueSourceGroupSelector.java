import org.apache.lucene.search.grouping.*;


import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.queries.function.FunctionValues;
import org.apache.lucene.queries.function.ValueSource;
import org.apache.lucene.util.mutable.MutableValue;

/**
 * A GroupSelector that groups via a ValueSource
 */
public class ValueSourceGroupSelector extends GroupSelector<MutableValue> {

  private final ValueSource valueSource;
  private final Map<?, ?> context;

  private Set<MutableValue> secondPassGroups;

  /**
   * Create a new ValueSourceGroupSelector
   * @param valueSource the ValueSource to group by
   * @param context     a context map for the ValueSource
   */
  public ValueSourceGroupSelector(ValueSource valueSource, Map<?, ?> context) {
    this.valueSource = valueSource;
    this.context = context;
  }

  private FunctionValues.ValueFiller filler;

  @Override
  public void setNextReader(LeafReaderContext readerContext) throws IOException {
    FunctionValues values = valueSource.getValues(context, readerContext);
    this.filler = values.getValueFiller();
  }

  @Override
  public State advanceTo(int doc) throws IOException {
    this.filler.fillValue(doc);
    if (secondPassGroups != null) {
      if (secondPassGroups.contains(filler.getValue()) == false)
        return State.SKIP;
    }
    return State.ACCEPT;
  }

  @Override
  public MutableValue currentValue() {
    return filler.getValue();
  }

  @Override
  public MutableValue copyValue() {
    return filler.getValue().duplicate();
  }

  @Override
  public void setGroups(Collection<SearchGroup<MutableValue>> searchGroups) {
    secondPassGroups = new HashSet<>();
    for (SearchGroup<MutableValue> group : searchGroups) {
      secondPassGroups.add(group.groupValue);
    }
  }
}
