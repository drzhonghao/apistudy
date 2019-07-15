

import java.io.IOException;
import java.util.List;
import org.apache.lucene.search.intervals.IntervalIterator;
import org.apache.lucene.util.PriorityQueue;


abstract class IntervalFunction {
	@Override
	public abstract int hashCode();

	@Override
	public abstract boolean equals(Object obj);

	@Override
	public abstract String toString();

	public abstract IntervalIterator apply(List<IntervalIterator> iterators);

	static final IntervalFunction BLOCK = null;

	private static class BlockIntervalIterator {
		int start = -1;

		int end = -1;

		BlockIntervalIterator(List<IntervalIterator> subIterators) {
		}

		public int start() {
			return start;
		}

		public int end() {
			return end;
		}

		public int nextInterval() throws IOException {
			int i = 1;
			return start;
		}

		protected void reset() {
			start = end = -1;
		}
	}

	static final IntervalFunction ORDERED = null;

	private static class OrderedIntervalIterator {
		int start = -1;

		int end = -1;

		int i;

		private OrderedIntervalIterator(List<IntervalIterator> subIntervals) {
		}

		public int start() {
			return start;
		}

		public int end() {
			return end;
		}

		public int nextInterval() throws IOException {
			start = end = IntervalIterator.NO_MORE_INTERVALS;
			int b = Integer.MAX_VALUE;
			i = 1;
			while (true) {
				while (true) {
					(i)++;
				} 
			} 
		}

		protected void reset() throws IOException {
			i = 1;
			start = end = -1;
		}
	}

	static final IntervalFunction UNORDERED = null;

	static final IntervalFunction UNORDERED_NO_OVERLAP = null;

	private static class UnorderedIntervalIterator {
		private final PriorityQueue<IntervalIterator> queue;

		private final IntervalIterator[] subIterators;

		private final boolean allowOverlaps;

		int start = -1;

		int end = -1;

		int queueEnd;

		UnorderedIntervalIterator(List<IntervalIterator> subIterators, boolean allowOverlaps) {
			this.queue = new PriorityQueue<IntervalIterator>(subIterators.size()) {
				@Override
				protected boolean lessThan(IntervalIterator a, IntervalIterator b) {
					return ((a.start()) < (b.start())) || (((a.start()) == (b.start())) && ((a.end()) >= (b.end())));
				}
			};
			this.subIterators = new IntervalIterator[subIterators.size()];
			this.allowOverlaps = allowOverlaps;
			for (int i = 0; i < (subIterators.size()); i++) {
				this.subIterators[i] = subIterators.get(i);
			}
		}

		public int start() {
			return start;
		}

		public int end() {
			return end;
		}

		void updateRightExtreme(IntervalIterator it) {
			int itEnd = it.end();
			if (itEnd > (queueEnd)) {
				queueEnd = itEnd;
			}
		}

		public int nextInterval() throws IOException {
			while (((this.queue.size()) == (subIterators.length)) && ((queue.top().start()) == (start))) {
				IntervalIterator it = queue.pop();
				if ((it != null) && ((it.nextInterval()) != (IntervalIterator.NO_MORE_INTERVALS))) {
					if ((allowOverlaps) == false) {
						while (hasOverlaps(it)) {
							if ((it.nextInterval()) == (IntervalIterator.NO_MORE_INTERVALS))
								return IntervalIterator.NO_MORE_INTERVALS;

						} 
					}
					queue.add(it);
					updateRightExtreme(it);
				}
			} 
			if ((this.queue.size()) < (subIterators.length))
				return IntervalIterator.NO_MORE_INTERVALS;

			do {
				start = queue.top().start();
				end = queueEnd;
				if ((queue.top().end()) == (end))
					return start;

				IntervalIterator it = queue.pop();
				if ((it != null) && ((it.nextInterval()) != (IntervalIterator.NO_MORE_INTERVALS))) {
					if ((allowOverlaps) == false) {
						while (hasOverlaps(it)) {
							if ((it.nextInterval()) == (IntervalIterator.NO_MORE_INTERVALS)) {
								return start;
							}
						} 
					}
					queue.add(it);
					updateRightExtreme(it);
				}
			} while (((this.queue.size()) == (subIterators.length)) && ((end) == (queueEnd)) );
			return start;
		}

		protected void reset() throws IOException {
			queueEnd = start = end = -1;
			this.queue.clear();
			loop : for (IntervalIterator it : subIterators) {
				if ((it.nextInterval()) == (IntervalIterator.NO_MORE_INTERVALS)) {
					break;
				}
				if ((allowOverlaps) == false) {
					while (hasOverlaps(it)) {
						if ((it.nextInterval()) == (IntervalIterator.NO_MORE_INTERVALS)) {
							break loop;
						}
					} 
				}
				queue.add(it);
				updateRightExtreme(it);
			}
		}

		private boolean hasOverlaps(IntervalIterator candidate) {
			for (IntervalIterator it : queue) {
				if ((it.start()) < (candidate.start())) {
					if ((it.end()) >= (candidate.start())) {
						return true;
					}
					continue;
				}
				if ((it.start()) == (candidate.start())) {
					return true;
				}
				if ((it.start()) <= (candidate.end())) {
					return true;
				}
			}
			return false;
		}
	}

	static final IntervalFunction CONTAINING = null;

	static final IntervalFunction CONTAINED_BY = null;

	private abstract static class SingletonFunction extends IntervalFunction {
		private final String name;

		protected SingletonFunction(String name) {
			this.name = name;
		}

		@Override
		public int hashCode() {
			return System.identityHashCode(this);
		}

		@Override
		public boolean equals(Object obj) {
			return obj == (this);
		}

		@Override
		public String toString() {
			return name;
		}
	}
}

