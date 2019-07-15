

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;
import org.apache.lucene.store.DataInput;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.BytesRefBuilder;
import org.apache.lucene.util.IntsRef;
import org.apache.lucene.util.IntsRefBuilder;
import org.apache.lucene.util.fst.FST;
import org.apache.lucene.util.fst.Outputs;

import static org.apache.lucene.util.fst.FST.INPUT_TYPE.BYTE1;


public final class Util {
	private Util() {
	}

	public static <T> T get(FST<T> fst, IntsRef input) throws IOException {
		final FST.Arc<T> arc = fst.getFirstArc(new FST.Arc<T>());
		final FST.BytesReader fstReader = fst.getBytesReader();
		T output = fst.outputs.getNoOutput();
		for (int i = 0; i < (input.length); i++) {
			if ((fst.findTargetArc(input.ints[((input.offset) + i)], arc, arc, fstReader)) == null) {
				return null;
			}
			output = fst.outputs.add(output, arc.output);
		}
		if (arc.isFinal()) {
			return fst.outputs.add(output, arc.nextFinalOutput);
		}else {
			return null;
		}
	}

	public static <T> T get(FST<T> fst, BytesRef input) throws IOException {
		assert (fst.inputType) == (BYTE1);
		final FST.BytesReader fstReader = fst.getBytesReader();
		final FST.Arc<T> arc = fst.getFirstArc(new FST.Arc<T>());
		T output = fst.outputs.getNoOutput();
		for (int i = 0; i < (input.length); i++) {
			if ((fst.findTargetArc(((input.bytes[(i + (input.offset))]) & 255), arc, arc, fstReader)) == null) {
				return null;
			}
			output = fst.outputs.add(output, arc.output);
		}
		if (arc.isFinal()) {
			return fst.outputs.add(output, arc.nextFinalOutput);
		}else {
			return null;
		}
	}

	public static IntsRef getByOutput(FST<Long> fst, long targetOutput) throws IOException {
		final FST.BytesReader in = fst.getBytesReader();
		FST.Arc<Long> arc = fst.getFirstArc(new FST.Arc<Long>());
		FST.Arc<Long> scratchArc = new FST.Arc<>();
		final IntsRefBuilder result = new IntsRefBuilder();
		return Util.getByOutput(fst, targetOutput, in, arc, scratchArc, result);
	}

	public static IntsRef getByOutput(FST<Long> fst, long targetOutput, FST.BytesReader in, FST.Arc<Long> arc, FST.Arc<Long> scratchArc, IntsRefBuilder result) throws IOException {
		long output = arc.output;
		int upto = 0;
		while (true) {
			if (arc.isFinal()) {
				final long finalOutput = output + (arc.nextFinalOutput);
				if (finalOutput == targetOutput) {
					result.setLength(upto);
					return result.get();
				}else
					if (finalOutput > targetOutput) {
						return null;
					}

			}
			if (FST.targetHasArcs(arc)) {
				result.grow((1 + upto));
				fst.readFirstRealTargetArc(arc.target, arc, in);
				if ((arc.bytesPerArc) != 0) {
					int low = 0;
					int high = (arc.numArcs) - 1;
					int mid = 0;
					boolean exact = false;
					while (low <= high) {
						mid = (low + high) >>> 1;
						in.setPosition(arc.posArcsStart);
						in.skipBytes(((arc.bytesPerArc) * mid));
						final byte flags = in.readByte();
						fst.readLabel(in);
						final long minArcOutput;
						if ((flags & (FST.BIT_ARC_HAS_OUTPUT)) != 0) {
							final long arcOutput = fst.outputs.read(in);
							minArcOutput = output + arcOutput;
						}else {
							minArcOutput = output;
						}
						if (minArcOutput == targetOutput) {
							exact = true;
							break;
						}else
							if (minArcOutput < targetOutput) {
								low = mid + 1;
							}else {
								high = mid - 1;
							}

					} 
					if (high == (-1)) {
						return null;
					}else
						if (exact) {
							arc.arcIdx = mid - 1;
						}else {
							arc.arcIdx = low - 2;
						}

					fst.readNextRealArc(arc, in);
					result.setIntAt((upto++), arc.label);
					output += arc.output;
				}else {
					FST.Arc<Long> prevArc = null;
					while (true) {
						final long minArcOutput = output + (arc.output);
						if (minArcOutput == targetOutput) {
							output = minArcOutput;
							result.setIntAt((upto++), arc.label);
							break;
						}else
							if (minArcOutput > targetOutput) {
								if (prevArc == null) {
									return null;
								}else {
									arc.copyFrom(prevArc);
									result.setIntAt((upto++), arc.label);
									output += arc.output;
									break;
								}
							}else
								if (arc.isLast()) {
									output = minArcOutput;
									result.setIntAt((upto++), arc.label);
									break;
								}else {
									prevArc = scratchArc;
									prevArc.copyFrom(arc);
									fst.readNextRealArc(arc, in);
								}


					} 
				}
			}else {
				return null;
			}
		} 
	}

	public static class FSTPath<T> {
		public FST.Arc<T> arc;

		public T output;

		public final IntsRefBuilder input;

		public final float boost;

		public final CharSequence context;

		public int payload;

		public FSTPath(T output, FST.Arc<T> arc, IntsRefBuilder input) {
			this(output, arc, input, 0, null, (-1));
		}

		public FSTPath(T output, FST.Arc<T> arc, IntsRefBuilder input, float boost, CharSequence context, int payload) {
			this.arc = new FST.Arc<T>().copyFrom(arc);
			this.output = output;
			this.input = input;
			this.boost = boost;
			this.context = context;
			this.payload = payload;
		}

		public Util.FSTPath<T> newPath(T output, IntsRefBuilder input) {
			return new Util.FSTPath<>(output, this.arc, input, this.boost, this.context, this.payload);
		}

		@Override
		public String toString() {
			return (((((((("input=" + (input.get())) + " output=") + (output)) + " context=") + (context)) + " boost=") + (boost)) + " payload=") + (payload);
		}
	}

	private static class TieBreakByInputComparator<T> implements Comparator<Util.FSTPath<T>> {
		private final Comparator<T> comparator;

		public TieBreakByInputComparator(Comparator<T> comparator) {
			this.comparator = comparator;
		}

		@Override
		public int compare(Util.FSTPath<T> a, Util.FSTPath<T> b) {
			int cmp = comparator.compare(a.output, b.output);
			if (cmp == 0) {
				return a.input.get().compareTo(b.input.get());
			}else {
				return cmp;
			}
		}
	}

	public static class TopNSearcher<T> {
		private final FST<T> fst;

		private final FST.BytesReader bytesReader;

		private final int topN;

		private final int maxQueueDepth;

		private final FST.Arc<T> scratchArc = new FST.Arc<>();

		private final Comparator<T> comparator;

		private final Comparator<Util.FSTPath<T>> pathComparator;

		TreeSet<Util.FSTPath<T>> queue = null;

		public TopNSearcher(FST<T> fst, int topN, int maxQueueDepth, Comparator<T> comparator) {
			this(fst, topN, maxQueueDepth, comparator, new Util.TieBreakByInputComparator<>(comparator));
		}

		public TopNSearcher(FST<T> fst, int topN, int maxQueueDepth, Comparator<T> comparator, Comparator<Util.FSTPath<T>> pathComparator) {
			this.fst = fst;
			this.bytesReader = fst.getBytesReader();
			this.topN = topN;
			this.maxQueueDepth = maxQueueDepth;
			this.comparator = comparator;
			this.pathComparator = pathComparator;
			queue = new TreeSet<>(pathComparator);
		}

		protected void addIfCompetitive(Util.FSTPath<T> path) {
			assert (queue) != null;
			T output = fst.outputs.add(path.output, path.arc.output);
			if ((queue.size()) == (maxQueueDepth)) {
				Util.FSTPath<T> bottom = queue.last();
				int comp = pathComparator.compare(path, bottom);
				if (comp > 0) {
					return;
				}else
					if (comp == 0) {
						path.input.append(path.arc.label);
						final int cmp = bottom.input.get().compareTo(path.input.get());
						path.input.setLength(((path.input.length()) - 1));
						assert cmp != 0;
						if (cmp < 0) {
							return;
						}
					}

			}else {
			}
			IntsRefBuilder newInput = new IntsRefBuilder();
			newInput.copyInts(path.input.get());
			newInput.append(path.arc.label);
			Util.FSTPath<T> newPath = path.newPath(output, newInput);
			if (acceptPartialPath(newPath)) {
				queue.add(newPath);
				if ((queue.size()) == ((maxQueueDepth) + 1)) {
					queue.pollLast();
				}
			}
		}

		public void addStartPaths(FST.Arc<T> node, T startOutput, boolean allowEmptyString, IntsRefBuilder input) throws IOException {
			addStartPaths(node, startOutput, allowEmptyString, input, 0, null, (-1));
		}

		public void addStartPaths(FST.Arc<T> node, T startOutput, boolean allowEmptyString, IntsRefBuilder input, float boost, CharSequence context, int payload) throws IOException {
			if (startOutput.equals(fst.outputs.getNoOutput())) {
				startOutput = fst.outputs.getNoOutput();
			}
			Util.FSTPath<T> path = new Util.FSTPath<>(startOutput, node, input, boost, context, payload);
			fst.readFirstTargetArc(node, path.arc, bytesReader);
			while (true) {
				if (allowEmptyString || ((path.arc.label) != (FST.END_LABEL))) {
					addIfCompetitive(path);
				}
				if (path.arc.isLast()) {
					break;
				}
				fst.readNextArc(path.arc, bytesReader);
			} 
		}

		public Util.TopResults<T> search() throws IOException {
			final List<Util.Result<T>> results = new ArrayList<>();
			final FST.BytesReader fstReader = fst.getBytesReader();
			final T NO_OUTPUT = fst.outputs.getNoOutput();
			int rejectCount = 0;
			while ((results.size()) < (topN)) {
				Util.FSTPath<T> path;
				if ((queue) == null) {
					break;
				}
				path = queue.pollFirst();
				if (path == null) {
					break;
				}
				if ((acceptPartialPath(path)) == false) {
					continue;
				}
				if ((path.arc.label) == (FST.END_LABEL)) {
					path.input.setLength(((path.input.length()) - 1));
					results.add(new Util.Result<>(path.input.get(), path.output));
					continue;
				}
				if (((results.size()) == ((topN) - 1)) && ((maxQueueDepth) == (topN))) {
					queue = null;
				}
				while (true) {
					fst.readFirstTargetArc(path.arc, path.arc, fstReader);
					boolean foundZero = false;
					while (true) {
						if ((comparator.compare(NO_OUTPUT, path.arc.output)) == 0) {
							if ((queue) == null) {
								foundZero = true;
								break;
							}else
								if (!foundZero) {
									scratchArc.copyFrom(path.arc);
									foundZero = true;
								}else {
									addIfCompetitive(path);
								}

						}else
							if ((queue) != null) {
								addIfCompetitive(path);
							}

						if (path.arc.isLast()) {
							break;
						}
						fst.readNextArc(path.arc, fstReader);
					} 
					assert foundZero;
					if ((queue) != null) {
						path.arc.copyFrom(scratchArc);
					}
					if ((path.arc.label) == (FST.END_LABEL)) {
						path.output = fst.outputs.add(path.output, path.arc.output);
						if (acceptResult(path)) {
							results.add(new Util.Result<>(path.input.get(), path.output));
						}else {
							rejectCount++;
						}
						break;
					}else {
						path.input.append(path.arc.label);
						path.output = fst.outputs.add(path.output, path.arc.output);
						if ((acceptPartialPath(path)) == false) {
							break;
						}
					}
				} 
			} 
			return new Util.TopResults<>(((rejectCount + (topN)) <= (maxQueueDepth)), results);
		}

		protected boolean acceptResult(Util.FSTPath<T> path) {
			return acceptResult(path.input.get(), path.output);
		}

		protected boolean acceptPartialPath(Util.FSTPath<T> path) {
			return true;
		}

		protected boolean acceptResult(IntsRef input, T output) {
			return true;
		}
	}

	public static final class Result<T> {
		public final IntsRef input;

		public final T output;

		public Result(IntsRef input, T output) {
			this.input = input;
			this.output = output;
		}
	}

	public static final class TopResults<T> implements Iterable<Util.Result<T>> {
		public final boolean isComplete;

		public final List<Util.Result<T>> topN;

		TopResults(boolean isComplete, List<Util.Result<T>> topN) {
			this.topN = topN;
			this.isComplete = isComplete;
		}

		@Override
		public Iterator<Util.Result<T>> iterator() {
			return topN.iterator();
		}
	}

	public static <T> Util.TopResults<T> shortestPaths(FST<T> fst, FST.Arc<T> fromNode, T startOutput, Comparator<T> comparator, int topN, boolean allowEmptyString) throws IOException {
		Util.TopNSearcher<T> searcher = new Util.TopNSearcher<>(fst, topN, topN, comparator);
		searcher.addStartPaths(fromNode, startOutput, allowEmptyString, new IntsRefBuilder());
		return searcher.search();
	}

	public static <T> void toDot(FST<T> fst, Writer out, boolean sameRank, boolean labelStates) throws IOException {
		final String expandedNodeColor = "blue";
		final FST.Arc<T> startArc = fst.getFirstArc(new FST.Arc<T>());
		final List<FST.Arc<T>> thisLevelQueue = new ArrayList<>();
		final List<FST.Arc<T>> nextLevelQueue = new ArrayList<>();
		nextLevelQueue.add(startArc);
		final List<Integer> sameLevelStates = new ArrayList<>();
		final BitSet seen = new BitSet();
		seen.set(((int) (startArc.target)));
		final String stateShape = "circle";
		final String finalStateShape = "doublecircle";
		out.write("digraph FST {\n");
		out.write("  rankdir = LR; splines=true; concentrate=true; ordering=out; ranksep=2.5; \n");
		if (!labelStates) {
			out.write("  node [shape=circle, width=.2, height=.2, style=filled]\n");
		}
		Util.emitDotState(out, "initial", "point", "white", "");
		final T NO_OUTPUT = fst.outputs.getNoOutput();
		final FST.BytesReader r = fst.getBytesReader();
		{
			final String stateColor;
			final boolean isFinal;
			final T finalOutput;
			if (startArc.isFinal()) {
				isFinal = true;
				finalOutput = ((startArc.nextFinalOutput) == NO_OUTPUT) ? null : startArc.nextFinalOutput;
			}else {
				isFinal = false;
				finalOutput = null;
			}
			stateColor = null;
			Util.emitDotState(out, Long.toString(startArc.target), (isFinal ? finalStateShape : stateShape), stateColor, (finalOutput == null ? "" : fst.outputs.outputToString(finalOutput)));
		}
		out.write((("  initial -> " + (startArc.target)) + "\n"));
		int level = 0;
		while (!(nextLevelQueue.isEmpty())) {
			thisLevelQueue.addAll(nextLevelQueue);
			nextLevelQueue.clear();
			level++;
			out.write((("\n  // Transitions and states at level: " + level) + "\n"));
			while (!(thisLevelQueue.isEmpty())) {
				final FST.Arc<T> arc = thisLevelQueue.remove(((thisLevelQueue.size()) - 1));
				if (FST.targetHasArcs(arc)) {
					final long node = arc.target;
					fst.readFirstRealTargetArc(arc.target, arc, r);
					while (true) {
						if (((arc.target) >= 0) && (!(seen.get(((int) (arc.target)))))) {
							final String stateColor;
							final String finalOutput;
							if (((arc.nextFinalOutput) != null) && ((arc.nextFinalOutput) != NO_OUTPUT)) {
								finalOutput = fst.outputs.outputToString(arc.nextFinalOutput);
							}else {
								finalOutput = "";
							}
							stateColor = null;
							Util.emitDotState(out, Long.toString(arc.target), stateShape, stateColor, finalOutput);
							seen.set(((int) (arc.target)));
							nextLevelQueue.add(new FST.Arc<T>().copyFrom(arc));
							sameLevelStates.add(((int) (arc.target)));
						}
						String outs;
						if ((arc.output) != NO_OUTPUT) {
							outs = "/" + (fst.outputs.outputToString(arc.output));
						}else {
							outs = "";
						}
						if (((!(FST.targetHasArcs(arc))) && (arc.isFinal())) && ((arc.nextFinalOutput) != NO_OUTPUT)) {
							outs = ((outs + "/[") + (fst.outputs.outputToString(arc.nextFinalOutput))) + "]";
						}
						final String arcColor;
						assert (arc.label) != (FST.END_LABEL);
						arcColor = null;
						out.write(((((((((((("  " + node) + " -> ") + (arc.target)) + " [label=\"") + (Util.printableLabel(arc.label))) + outs) + "\"") + (arc.isFinal() ? " style=\"bold\"" : "")) + " color=\"") + arcColor) + "\"]\n"));
						if (arc.isLast()) {
							break;
						}
						fst.readNextRealArc(arc, r);
					} 
				}
			} 
			if (sameRank && ((sameLevelStates.size()) > 1)) {
				out.write("  {rank=same; ");
				for (int state : sameLevelStates) {
					out.write((state + "; "));
				}
				out.write(" }\n");
			}
			sameLevelStates.clear();
		} 
		out.write("  -1 [style=filled, color=black, shape=doublecircle, label=\"\"]\n\n");
		out.write("  {rank=sink; -1 }\n");
		out.write("}\n");
		out.flush();
	}

	private static void emitDotState(Writer out, String name, String shape, String color, String label) throws IOException {
		out.write(((((((((("  " + name) + " [") + (shape != null ? "shape=" + shape : "")) + " ") + (color != null ? "color=" + color : "")) + " ") + (label != null ? ("label=\"" + label) + "\"" : "label=\"\"")) + " ") + "]\n"));
	}

	private static String printableLabel(int label) {
		if ((((label >= 32) && (label <= 125)) && (label != 34)) && (label != 92)) {
			return Character.toString(((char) (label)));
		}
		return "0x" + (Integer.toHexString(label));
	}

	public static IntsRef toUTF16(CharSequence s, IntsRefBuilder scratch) {
		final int charLimit = s.length();
		scratch.setLength(charLimit);
		scratch.grow(charLimit);
		for (int idx = 0; idx < charLimit; idx++) {
			scratch.setIntAt(idx, ((int) (s.charAt(idx))));
		}
		return scratch.get();
	}

	public static IntsRef toUTF32(CharSequence s, IntsRefBuilder scratch) {
		int charIdx = 0;
		int intIdx = 0;
		final int charLimit = s.length();
		while (charIdx < charLimit) {
			scratch.grow((intIdx + 1));
			final int utf32 = Character.codePointAt(s, charIdx);
			scratch.setIntAt(intIdx, utf32);
			charIdx += Character.charCount(utf32);
			intIdx++;
		} 
		scratch.setLength(intIdx);
		return scratch.get();
	}

	public static IntsRef toUTF32(char[] s, int offset, int length, IntsRefBuilder scratch) {
		int charIdx = offset;
		int intIdx = 0;
		final int charLimit = offset + length;
		while (charIdx < charLimit) {
			scratch.grow((intIdx + 1));
			final int utf32 = Character.codePointAt(s, charIdx, charLimit);
			scratch.setIntAt(intIdx, utf32);
			charIdx += Character.charCount(utf32);
			intIdx++;
		} 
		scratch.setLength(intIdx);
		return scratch.get();
	}

	public static IntsRef toIntsRef(BytesRef input, IntsRefBuilder scratch) {
		scratch.clear();
		for (int i = 0; i < (input.length); i++) {
			scratch.append(((input.bytes[(i + (input.offset))]) & 255));
		}
		return scratch.get();
	}

	public static BytesRef toBytesRef(IntsRef input, BytesRefBuilder scratch) {
		scratch.grow(input.length);
		for (int i = 0; i < (input.length); i++) {
			int value = input.ints[(i + (input.offset))];
			assert (value >= (Byte.MIN_VALUE)) && (value <= 255) : ("value " + value) + " doesn't fit into byte";
			scratch.setByteAt(i, ((byte) (value)));
		}
		scratch.setLength(input.length);
		return scratch.get();
	}

	public static <T> FST.Arc<T> readCeilArc(int label, FST<T> fst, FST.Arc<T> follow, FST.Arc<T> arc, FST.BytesReader in) throws IOException {
		if (label == (FST.END_LABEL)) {
			if (follow.isFinal()) {
				if ((follow.target) <= 0) {
				}else {
				}
				arc.output = follow.nextFinalOutput;
				arc.label = FST.END_LABEL;
				return arc;
			}else {
				return null;
			}
		}
		if (!(FST.targetHasArcs(follow))) {
			return null;
		}
		fst.readFirstTargetArc(follow, arc, in);
		if (((arc.bytesPerArc) != 0) && ((arc.label) != (FST.END_LABEL))) {
			int low = arc.arcIdx;
			int high = (arc.numArcs) - 1;
			int mid = 0;
			while (low <= high) {
				mid = (low + high) >>> 1;
				in.setPosition(arc.posArcsStart);
				in.skipBytes((((arc.bytesPerArc) * mid) + 1));
				final int midLabel = fst.readLabel(in);
				final int cmp = midLabel - label;
				if (cmp < 0) {
					low = mid + 1;
				}else
					if (cmp > 0) {
						high = mid - 1;
					}else {
						arc.arcIdx = mid - 1;
						return fst.readNextRealArc(arc, in);
					}

			} 
			if (low == (arc.numArcs)) {
				return null;
			}
			arc.arcIdx = (low > high) ? high : low;
			return fst.readNextRealArc(arc, in);
		}
		fst.readFirstRealTargetArc(follow.target, arc, in);
		while (true) {
			if ((arc.label) >= label) {
				return arc;
			}else
				if (arc.isLast()) {
					return null;
				}else {
					fst.readNextRealArc(arc, in);
				}

		} 
	}
}

