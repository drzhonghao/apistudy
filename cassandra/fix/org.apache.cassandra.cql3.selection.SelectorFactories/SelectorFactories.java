

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import org.apache.cassandra.config.CFMetaData;
import org.apache.cassandra.config.ColumnDefinition;
import org.apache.cassandra.cql3.QueryOptions;
import org.apache.cassandra.cql3.VariableSpecifications;
import org.apache.cassandra.cql3.functions.Function;
import org.apache.cassandra.cql3.selection.Selectable;
import org.apache.cassandra.cql3.selection.Selector;
import org.apache.cassandra.cql3.selection.Selector.Factory;
import org.apache.cassandra.cql3.selection.SimpleSelector;
import org.apache.cassandra.db.marshal.AbstractType;
import org.apache.cassandra.exceptions.InvalidRequestException;


final class SelectorFactories implements Iterable<Selector.Factory> {
	private final List<Selector.Factory> factories;

	private boolean containsWritetimeFactory;

	private boolean containsTTLFactory;

	private int numberOfAggregateFactories;

	public static SelectorFactories createFactoriesAndCollectColumnDefinitions(List<Selectable> selectables, List<AbstractType<?>> expectedTypes, CFMetaData cfm, List<ColumnDefinition> defs, VariableSpecifications boundNames) throws InvalidRequestException {
		return new SelectorFactories(selectables, expectedTypes, cfm, defs, boundNames);
	}

	private SelectorFactories(List<Selectable> selectables, List<AbstractType<?>> expectedTypes, CFMetaData cfm, List<ColumnDefinition> defs, VariableSpecifications boundNames) throws InvalidRequestException {
		factories = new ArrayList<>(selectables.size());
		for (int i = 0; i < (selectables.size()); i++) {
			Selectable selectable = selectables.get(i);
			AbstractType<?> expectedType = (expectedTypes == null) ? null : expectedTypes.get(i);
			Selector.Factory factory = selectable.newSelectorFactory(cfm, expectedType, defs, boundNames);
			containsWritetimeFactory |= factory.isWritetimeSelectorFactory();
			containsTTLFactory |= factory.isTTLSelectorFactory();
			if (factory.isAggregateSelectorFactory())
				++(numberOfAggregateFactories);

			factories.add(factory);
		}
	}

	public void addFunctionsTo(List<Function> functions) {
		factories.forEach(( p) -> p.addFunctionsTo(functions));
	}

	public Selector.Factory get(int i) {
		return factories.get(i);
	}

	public void addSelectorForOrdering(ColumnDefinition def, int index) {
		factories.add(SimpleSelector.newFactory(def, index));
	}

	public boolean doesAggregation() {
		return (numberOfAggregateFactories) > 0;
	}

	public boolean containsWritetimeSelectorFactory() {
		return containsWritetimeFactory;
	}

	public boolean containsTTLSelectorFactory() {
		return containsTTLFactory;
	}

	public List<Selector> newInstances(QueryOptions options) throws InvalidRequestException {
		List<Selector> selectors = new ArrayList<>(factories.size());
		for (Selector.Factory factory : factories)
			selectors.add(factory.newInstance(options));

		return selectors;
	}

	public Iterator<Selector.Factory> iterator() {
		return factories.iterator();
	}

	public List<String> getColumnNames() {
		return Lists.transform(factories, new com.google.common.base.Function<Selector.Factory, String>() {
			public String apply(Selector.Factory factory) {
				return null;
			}
		});
	}

	public List<AbstractType<?>> getReturnTypes() {
		return Lists.transform(factories, new com.google.common.base.Function<Selector.Factory, AbstractType<?>>() {
			public AbstractType<?> apply(Selector.Factory factory) {
				return null;
			}
		});
	}

	public int size() {
		return factories.size();
	}
}

