

import org.apache.cassandra.index.sasi.analyzer.filter.FilterPipelineTask;


public class FilterPipelineBuilder {
	private final FilterPipelineTask<?, ?> parent;

	private FilterPipelineTask<?, ?> current;

	public FilterPipelineBuilder(FilterPipelineTask<?, ?> first) {
		this(first, first);
	}

	private FilterPipelineBuilder(FilterPipelineTask<?, ?> first, FilterPipelineTask<?, ?> current) {
		this.parent = first;
		this.current = current;
	}

	public FilterPipelineBuilder add(String name, FilterPipelineTask<?, ?> nextTask) {
		this.current = nextTask;
		return this;
	}

	public FilterPipelineTask<?, ?> build() {
		return this.parent;
	}
}

