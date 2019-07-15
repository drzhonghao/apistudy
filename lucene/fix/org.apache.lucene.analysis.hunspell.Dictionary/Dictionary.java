

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.lucene.codecs.CodecUtil;
import org.apache.lucene.store.ByteArrayDataOutput;
import org.apache.lucene.store.ChecksumIndexInput;
import org.apache.lucene.store.DataOutput;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.store.IndexOutput;
import org.apache.lucene.util.ArrayUtil;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.BytesRefBuilder;
import org.apache.lucene.util.BytesRefHash;
import org.apache.lucene.util.CharsRef;
import org.apache.lucene.util.IOUtils;
import org.apache.lucene.util.IntsRef;
import org.apache.lucene.util.IntsRefBuilder;
import org.apache.lucene.util.OfflineSorter;
import org.apache.lucene.util.RamUsageEstimator;
import org.apache.lucene.util.automaton.Automaton;
import org.apache.lucene.util.automaton.CharacterRunAutomaton;
import org.apache.lucene.util.automaton.RegExp;
import org.apache.lucene.util.fst.Builder;
import org.apache.lucene.util.fst.CharSequenceOutputs;
import org.apache.lucene.util.fst.FST;
import org.apache.lucene.util.fst.IntSequenceOutputs;
import org.apache.lucene.util.fst.Outputs;
import org.apache.lucene.util.fst.Util;

import static org.apache.lucene.util.fst.FST.INPUT_TYPE.BYTE2;
import static org.apache.lucene.util.fst.FST.INPUT_TYPE.BYTE4;


public class Dictionary {
	static final char[] NOFLAGS = new char[0];

	private static final String ALIAS_KEY = "AF";

	private static final String MORPH_ALIAS_KEY = "AM";

	private static final String PREFIX_KEY = "PFX";

	private static final String SUFFIX_KEY = "SFX";

	private static final String FLAG_KEY = "FLAG";

	private static final String COMPLEXPREFIXES_KEY = "COMPLEXPREFIXES";

	private static final String CIRCUMFIX_KEY = "CIRCUMFIX";

	private static final String IGNORE_KEY = "IGNORE";

	private static final String ICONV_KEY = "ICONV";

	private static final String OCONV_KEY = "OCONV";

	private static final String FULLSTRIP_KEY = "FULLSTRIP";

	private static final String LANG_KEY = "LANG";

	private static final String KEEPCASE_KEY = "KEEPCASE";

	private static final String NEEDAFFIX_KEY = "NEEDAFFIX";

	private static final String PSEUDOROOT_KEY = "PSEUDOROOT";

	private static final String ONLYINCOMPOUND_KEY = "ONLYINCOMPOUND";

	private static final String NUM_FLAG_TYPE = "num";

	private static final String UTF8_FLAG_TYPE = "UTF-8";

	private static final String LONG_FLAG_TYPE = "long";

	private static final String PREFIX_CONDITION_REGEX_PATTERN = "%s.*";

	private static final String SUFFIX_CONDITION_REGEX_PATTERN = ".*%s";

	FST<IntsRef> prefixes;

	FST<IntsRef> suffixes;

	ArrayList<CharacterRunAutomaton> patterns = new ArrayList<>();

	FST<IntsRef> words;

	BytesRefHash flagLookup = new BytesRefHash();

	char[] stripData;

	int[] stripOffsets;

	byte[] affixData = new byte[64];

	private int currentAffix = 0;

	private Dictionary.FlagParsingStrategy flagParsingStrategy = new Dictionary.SimpleFlagParsingStrategy();

	private String[] aliases;

	private int aliasCount = 0;

	private String[] morphAliases;

	private int morphAliasCount = 0;

	private String[] stemExceptions = new String[8];

	private int stemExceptionCount = 0;

	boolean hasStemExceptions;

	private final Path tempPath = Dictionary.getDefaultTempDir();

	boolean ignoreCase;

	boolean complexPrefixes;

	boolean twoStageAffix;

	int circumfix = -1;

	int keepcase = -1;

	int needaffix = -1;

	int onlyincompound = -1;

	private char[] ignore;

	FST<CharsRef> iconv;

	FST<CharsRef> oconv;

	boolean needsInputCleaning;

	boolean needsOutputCleaning;

	boolean fullStrip;

	String language;

	boolean alternateCasing;

	public Dictionary(Directory tempDir, String tempFileNamePrefix, InputStream affix, InputStream dictionary) throws IOException, ParseException {
		this(tempDir, tempFileNamePrefix, affix, Collections.singletonList(dictionary), false);
	}

	public Dictionary(Directory tempDir, String tempFileNamePrefix, InputStream affix, List<InputStream> dictionaries, boolean ignoreCase) throws IOException, ParseException {
		this.ignoreCase = ignoreCase;
		this.needsInputCleaning = ignoreCase;
		this.needsOutputCleaning = false;
		flagLookup.add(new BytesRef());
		Path aff = Files.createTempFile(tempPath, "affix", "aff");
		OutputStream out = new BufferedOutputStream(Files.newOutputStream(aff));
		InputStream aff1 = null;
		InputStream aff2 = null;
		boolean success = false;
		try {
			final byte[] buffer = new byte[1024 * 8];
			int len;
			while ((len = affix.read(buffer)) > 0) {
				out.write(buffer, 0, len);
			} 
			out.close();
			aff1 = new BufferedInputStream(Files.newInputStream(aff));
			String encoding = Dictionary.getDictionaryEncoding(aff1);
			CharsetDecoder decoder = getJavaEncoding(encoding);
			aff2 = new BufferedInputStream(Files.newInputStream(aff));
			readAffixFile(aff2, decoder);
			IntSequenceOutputs o = IntSequenceOutputs.getSingleton();
			Builder<IntsRef> b = new Builder<>(BYTE4, o);
			readDictionaryFiles(tempDir, tempFileNamePrefix, dictionaries, decoder, b);
			words = b.finish();
			aliases = null;
			morphAliases = null;
			success = true;
		} finally {
			IOUtils.closeWhileHandlingException(out, aff1, aff2);
			if (success) {
				Files.delete(aff);
			}else {
				IOUtils.deleteFilesIgnoringExceptions(aff);
			}
		}
	}

	IntsRef lookupWord(char[] word, int offset, int length) {
		return lookup(words, word, offset, length);
	}

	IntsRef lookupPrefix(char[] word, int offset, int length) {
		return lookup(prefixes, word, offset, length);
	}

	IntsRef lookupSuffix(char[] word, int offset, int length) {
		return lookup(suffixes, word, offset, length);
	}

	IntsRef lookup(FST<IntsRef> fst, char[] word, int offset, int length) {
		if (fst == null) {
			return null;
		}
		final FST.BytesReader bytesReader = fst.getBytesReader();
		final FST.Arc<IntsRef> arc = fst.getFirstArc(new FST.Arc<IntsRef>());
		final IntsRef NO_OUTPUT = fst.outputs.getNoOutput();
		IntsRef output = NO_OUTPUT;
		int l = offset + length;
		try {
			for (int i = offset, cp = 0; i < l; i += Character.charCount(cp)) {
				cp = Character.codePointAt(word, i, l);
				if ((fst.findTargetArc(cp, arc, arc, bytesReader)) == null) {
					return null;
				}else
					if ((arc.output) != NO_OUTPUT) {
						output = fst.outputs.add(output, arc.output);
					}

			}
			if ((fst.findTargetArc(FST.END_LABEL, arc, arc, bytesReader)) == null) {
				return null;
			}else
				if ((arc.output) != NO_OUTPUT) {
					return fst.outputs.add(output, arc.output);
				}else {
					return output;
				}

		} catch (IOException bogus) {
			throw new RuntimeException(bogus);
		}
	}

	private void readAffixFile(InputStream affixStream, CharsetDecoder decoder) throws IOException, ParseException {
		TreeMap<String, List<Integer>> prefixes = new TreeMap<>();
		TreeMap<String, List<Integer>> suffixes = new TreeMap<>();
		Map<String, Integer> seenPatterns = new HashMap<>();
		seenPatterns.put(".*", 0);
		patterns.add(null);
		Map<String, Integer> seenStrips = new LinkedHashMap<>();
		seenStrips.put("", 0);
		LineNumberReader reader = new LineNumberReader(new InputStreamReader(affixStream, decoder));
		String line = null;
		while ((line = reader.readLine()) != null) {
			if (((reader.getLineNumber()) == 1) && (line.startsWith("\ufeff"))) {
				line = line.substring(1);
			}
			if (line.startsWith(Dictionary.ALIAS_KEY)) {
				parseAlias(line);
			}else
				if (line.startsWith(Dictionary.MORPH_ALIAS_KEY)) {
					parseMorphAlias(line);
				}else
					if (line.startsWith(Dictionary.PREFIX_KEY)) {
						parseAffix(prefixes, line, reader, Dictionary.PREFIX_CONDITION_REGEX_PATTERN, seenPatterns, seenStrips);
					}else
						if (line.startsWith(Dictionary.SUFFIX_KEY)) {
							parseAffix(suffixes, line, reader, Dictionary.SUFFIX_CONDITION_REGEX_PATTERN, seenPatterns, seenStrips);
						}else
							if (line.startsWith(Dictionary.FLAG_KEY)) {
								flagParsingStrategy = Dictionary.getFlagParsingStrategy(line);
							}else
								if (line.equals(Dictionary.COMPLEXPREFIXES_KEY)) {
									complexPrefixes = true;
								}else
									if (line.startsWith(Dictionary.CIRCUMFIX_KEY)) {
										String[] parts = line.split("\\s+");
										if ((parts.length) != 2) {
											throw new ParseException("Illegal CIRCUMFIX declaration", reader.getLineNumber());
										}
										circumfix = flagParsingStrategy.parseFlag(parts[1]);
									}else
										if (line.startsWith(Dictionary.KEEPCASE_KEY)) {
											String[] parts = line.split("\\s+");
											if ((parts.length) != 2) {
												throw new ParseException("Illegal KEEPCASE declaration", reader.getLineNumber());
											}
											keepcase = flagParsingStrategy.parseFlag(parts[1]);
										}else
											if ((line.startsWith(Dictionary.NEEDAFFIX_KEY)) || (line.startsWith(Dictionary.PSEUDOROOT_KEY))) {
												String[] parts = line.split("\\s+");
												if ((parts.length) != 2) {
													throw new ParseException("Illegal NEEDAFFIX declaration", reader.getLineNumber());
												}
												needaffix = flagParsingStrategy.parseFlag(parts[1]);
											}else
												if (line.startsWith(Dictionary.ONLYINCOMPOUND_KEY)) {
													String[] parts = line.split("\\s+");
													if ((parts.length) != 2) {
														throw new ParseException("Illegal ONLYINCOMPOUND declaration", reader.getLineNumber());
													}
													onlyincompound = flagParsingStrategy.parseFlag(parts[1]);
												}else
													if (line.startsWith(Dictionary.IGNORE_KEY)) {
														String[] parts = line.split("\\s+");
														if ((parts.length) != 2) {
															throw new ParseException("Illegal IGNORE declaration", reader.getLineNumber());
														}
														ignore = parts[1].toCharArray();
														Arrays.sort(ignore);
														needsInputCleaning = true;
													}else
														if ((line.startsWith(Dictionary.ICONV_KEY)) || (line.startsWith(Dictionary.OCONV_KEY))) {
															String[] parts = line.split("\\s+");
															String type = parts[0];
															if ((parts.length) != 2) {
																throw new ParseException((("Illegal " + type) + " declaration"), reader.getLineNumber());
															}
															int num = Integer.parseInt(parts[1]);
															FST<CharsRef> res = parseConversions(reader, num);
															if (type.equals("ICONV")) {
																iconv = res;
																needsInputCleaning |= (iconv) != null;
															}else {
																oconv = res;
																needsOutputCleaning |= (oconv) != null;
															}
														}else
															if (line.startsWith(Dictionary.FULLSTRIP_KEY)) {
																fullStrip = true;
															}else
																if (line.startsWith(Dictionary.LANG_KEY)) {
																	language = line.substring(Dictionary.LANG_KEY.length()).trim();
																	alternateCasing = ("tr_TR".equals(language)) || ("az_AZ".equals(language));
																}













		} 
		this.prefixes = affixFST(prefixes);
		this.suffixes = affixFST(suffixes);
		int totalChars = 0;
		for (String strip : seenStrips.keySet()) {
			totalChars += strip.length();
		}
		stripData = new char[totalChars];
		stripOffsets = new int[(seenStrips.size()) + 1];
		int currentOffset = 0;
		int currentIndex = 0;
		for (String strip : seenStrips.keySet()) {
			stripOffsets[(currentIndex++)] = currentOffset;
			strip.getChars(0, strip.length(), stripData, currentOffset);
			currentOffset += strip.length();
		}
		assert currentIndex == (seenStrips.size());
		stripOffsets[currentIndex] = currentOffset;
	}

	private FST<IntsRef> affixFST(TreeMap<String, List<Integer>> affixes) throws IOException {
		IntSequenceOutputs outputs = IntSequenceOutputs.getSingleton();
		Builder<IntsRef> builder = new Builder<>(BYTE4, outputs);
		IntsRefBuilder scratch = new IntsRefBuilder();
		for (Map.Entry<String, List<Integer>> entry : affixes.entrySet()) {
			Util.toUTF32(entry.getKey(), scratch);
			List<Integer> entries = entry.getValue();
			IntsRef output = new IntsRef(entries.size());
			for (Integer c : entries) {
				output.ints[((output.length)++)] = c;
			}
			builder.add(scratch.get(), output);
		}
		return builder.finish();
	}

	static String escapeDash(String re) {
		StringBuilder escaped = new StringBuilder();
		for (int i = 0; i < (re.length()); i++) {
			char c = re.charAt(i);
			if (c == '-') {
				escaped.append("\\-");
			}else {
				escaped.append(c);
				if ((c == '\\') && ((i + 1) < (re.length()))) {
					escaped.append(re.charAt((i + 1)));
					i++;
				}
			}
		}
		return escaped.toString();
	}

	private void parseAffix(TreeMap<String, List<Integer>> affixes, String header, LineNumberReader reader, String conditionPattern, Map<String, Integer> seenPatterns, Map<String, Integer> seenStrips) throws IOException, ParseException {
		BytesRefBuilder scratch = new BytesRefBuilder();
		StringBuilder sb = new StringBuilder();
		String[] args = header.split("\\s+");
		boolean crossProduct = args[2].equals("Y");
		boolean isSuffix = conditionPattern == (Dictionary.SUFFIX_CONDITION_REGEX_PATTERN);
		int numLines = Integer.parseInt(args[3]);
		affixData = ArrayUtil.grow(affixData, (((currentAffix) << 3) + (numLines << 3)));
		ByteArrayDataOutput affixWriter = new ByteArrayDataOutput(affixData, ((currentAffix) << 3), (numLines << 3));
		for (int i = 0; i < numLines; i++) {
			assert (affixWriter.getPosition()) == ((currentAffix) << 3);
			String line = reader.readLine();
			String[] ruleArgs = line.split("\\s+");
			if ((ruleArgs.length) < 4) {
				throw new ParseException(("The affix file contains a rule with less than four elements: " + line), reader.getLineNumber());
			}
			char flag = flagParsingStrategy.parseFlag(ruleArgs[1]);
			String strip = (ruleArgs[2].equals("0")) ? "" : ruleArgs[2];
			String affixArg = ruleArgs[3];
			char[] appendFlags = null;
			int flagSep = affixArg.lastIndexOf('/');
			if (flagSep != (-1)) {
				String flagPart = affixArg.substring((flagSep + 1));
				affixArg = affixArg.substring(0, flagSep);
				if ((aliasCount) > 0) {
					flagPart = getAliasValue(Integer.parseInt(flagPart));
				}
				appendFlags = flagParsingStrategy.parseFlags(flagPart);
				Arrays.sort(appendFlags);
				twoStageAffix = true;
			}
			if ("0".equals(affixArg)) {
				affixArg = "";
			}
			String condition = ((ruleArgs.length) > 4) ? ruleArgs[4] : ".";
			if ((condition.startsWith("[")) && ((condition.indexOf(']')) == (-1))) {
				condition = condition + "]";
			}
			if ((condition.indexOf('-')) >= 0) {
				condition = Dictionary.escapeDash(condition);
			}
			final String regex;
			if (".".equals(condition)) {
				regex = ".*";
			}else
				if (condition.equals(strip)) {
					regex = ".*";
				}else {
					regex = String.format(Locale.ROOT, conditionPattern, condition);
				}

			Integer patternIndex = seenPatterns.get(regex);
			if (patternIndex == null) {
				patternIndex = patterns.size();
				if (patternIndex > (Short.MAX_VALUE)) {
					throw new UnsupportedOperationException("Too many patterns, please report this to dev@lucene.apache.org");
				}
				seenPatterns.put(regex, patternIndex);
				CharacterRunAutomaton pattern = new CharacterRunAutomaton(new RegExp(regex, RegExp.NONE).toAutomaton());
				patterns.add(pattern);
			}
			Integer stripOrd = seenStrips.get(strip);
			if (stripOrd == null) {
				stripOrd = seenStrips.size();
				seenStrips.put(strip, stripOrd);
				if (stripOrd > (Character.MAX_VALUE)) {
					throw new UnsupportedOperationException("Too many unique strips, please report this to dev@lucene.apache.org");
				}
			}
			if (appendFlags == null) {
				appendFlags = Dictionary.NOFLAGS;
			}
			Dictionary.encodeFlags(scratch, appendFlags);
			int appendFlagsOrd = flagLookup.add(scratch.get());
			if (appendFlagsOrd < 0) {
				appendFlagsOrd = (-appendFlagsOrd) - 1;
			}else
				if (appendFlagsOrd > (Short.MAX_VALUE)) {
					throw new UnsupportedOperationException("Too many unique append flags, please report this to dev@lucene.apache.org");
				}

			affixWriter.writeShort(((short) (flag)));
			affixWriter.writeShort(((short) (stripOrd.intValue())));
			int patternOrd = ((patternIndex.intValue()) << 1) | (crossProduct ? 1 : 0);
			affixWriter.writeShort(((short) (patternOrd)));
			affixWriter.writeShort(((short) (appendFlagsOrd)));
			if (needsInputCleaning) {
				CharSequence cleaned = cleanInput(affixArg, sb);
				affixArg = cleaned.toString();
			}
			if (isSuffix) {
				affixArg = new StringBuilder(affixArg).reverse().toString();
			}
			List<Integer> list = affixes.get(affixArg);
			if (list == null) {
				list = new ArrayList<>();
				affixes.put(affixArg, list);
			}
			list.add(currentAffix);
			(currentAffix)++;
		}
	}

	private FST<CharsRef> parseConversions(LineNumberReader reader, int num) throws IOException, ParseException {
		Map<String, String> mappings = new TreeMap<>();
		for (int i = 0; i < num; i++) {
			String line = reader.readLine();
			String[] parts = line.split("\\s+");
			if ((parts.length) != 3) {
				throw new ParseException(("invalid syntax: " + line), reader.getLineNumber());
			}
			if ((mappings.put(parts[1], parts[2])) != null) {
				throw new IllegalStateException(("duplicate mapping specified for: " + (parts[1])));
			}
		}
		Outputs<CharsRef> outputs = CharSequenceOutputs.getSingleton();
		Builder<CharsRef> builder = new Builder<>(BYTE2, outputs);
		IntsRefBuilder scratchInts = new IntsRefBuilder();
		for (Map.Entry<String, String> entry : mappings.entrySet()) {
			Util.toUTF16(entry.getKey(), scratchInts);
			builder.add(scratchInts.get(), new CharsRef(entry.getValue()));
		}
		return builder.finish();
	}

	static final Pattern ENCODING_PATTERN = Pattern.compile("^(\u00ef\u00bb\u00bf)?SET\\s+");

	static String getDictionaryEncoding(InputStream affix) throws IOException, ParseException {
		final StringBuilder encoding = new StringBuilder();
		for (; ;) {
			encoding.setLength(0);
			int ch;
			while ((ch = affix.read()) >= 0) {
				if (ch == '\n') {
					break;
				}
				if (ch != '\r') {
					encoding.append(((char) (ch)));
				}
			} 
			if ((((encoding.length()) == 0) || ((encoding.charAt(0)) == '#')) || ((encoding.toString().trim().length()) == 0)) {
				if (ch < 0) {
					throw new ParseException("Unexpected end of affix file.", 0);
				}
				continue;
			}
			Matcher matcher = Dictionary.ENCODING_PATTERN.matcher(encoding);
			if (matcher.find()) {
				int last = matcher.end();
				return encoding.substring(last).trim();
			}
		}
	}

	static final Map<String, String> CHARSET_ALIASES;

	static {
		Map<String, String> m = new HashMap<>();
		m.put("microsoft-cp1251", "windows-1251");
		m.put("TIS620-2533", "TIS-620");
		CHARSET_ALIASES = Collections.unmodifiableMap(m);
	}

	private CharsetDecoder getJavaEncoding(String encoding) {
		if ("ISO8859-14".equals(encoding)) {
		}
		String canon = Dictionary.CHARSET_ALIASES.get(encoding);
		if (canon != null) {
			encoding = canon;
		}
		Charset charset = Charset.forName(encoding);
		return charset.newDecoder().onMalformedInput(CodingErrorAction.REPLACE);
	}

	static Dictionary.FlagParsingStrategy getFlagParsingStrategy(String flagLine) {
		String[] parts = flagLine.split("\\s+");
		if ((parts.length) != 2) {
			throw new IllegalArgumentException(("Illegal FLAG specification: " + flagLine));
		}
		String flagType = parts[1];
		if (Dictionary.NUM_FLAG_TYPE.equals(flagType)) {
			return new Dictionary.NumFlagParsingStrategy();
		}else
			if (Dictionary.UTF8_FLAG_TYPE.equals(flagType)) {
				return new Dictionary.SimpleFlagParsingStrategy();
			}else
				if (Dictionary.LONG_FLAG_TYPE.equals(flagType)) {
					return new Dictionary.DoubleASCIIFlagParsingStrategy();
				}


		throw new IllegalArgumentException(("Unknown flag type: " + flagType));
	}

	final char FLAG_SEPARATOR = 31;

	final char MORPH_SEPARATOR = 30;

	String unescapeEntry(String entry) {
		StringBuilder sb = new StringBuilder();
		int end = Dictionary.morphBoundary(entry);
		for (int i = 0; i < end; i++) {
			char ch = entry.charAt(i);
			if ((ch == '\\') && ((i + 1) < (entry.length()))) {
				sb.append(entry.charAt((i + 1)));
				i++;
			}else
				if (ch == '/') {
					sb.append(FLAG_SEPARATOR);
				}else
					if ((ch == (MORPH_SEPARATOR)) || (ch == (FLAG_SEPARATOR))) {
					}else {
						sb.append(ch);
					}


		}
		sb.append(MORPH_SEPARATOR);
		if (end < (entry.length())) {
			for (int i = end; i < (entry.length()); i++) {
				char c = entry.charAt(i);
				if ((c == (FLAG_SEPARATOR)) || (c == (MORPH_SEPARATOR))) {
				}else {
					sb.append(c);
				}
			}
		}
		return sb.toString();
	}

	static int morphBoundary(String line) {
		int end = Dictionary.indexOfSpaceOrTab(line, 0);
		if (end == (-1)) {
			return line.length();
		}
		while ((end >= 0) && (end < (line.length()))) {
			if (((line.charAt(end)) == '\t') || (((((end + 3) < (line.length())) && (Character.isLetter(line.charAt((end + 1))))) && (Character.isLetter(line.charAt((end + 2))))) && ((line.charAt((end + 3))) == ':'))) {
				break;
			}
			end = Dictionary.indexOfSpaceOrTab(line, (end + 1));
		} 
		if (end == (-1)) {
			return line.length();
		}
		return end;
	}

	static int indexOfSpaceOrTab(String text, int start) {
		int pos1 = text.indexOf('\t', start);
		int pos2 = text.indexOf(' ', start);
		if ((pos1 >= 0) && (pos2 >= 0)) {
			return Math.min(pos1, pos2);
		}else {
			return Math.max(pos1, pos2);
		}
	}

	private void readDictionaryFiles(Directory tempDir, String tempFileNamePrefix, List<InputStream> dictionaries, CharsetDecoder decoder, Builder<IntsRef> words) throws IOException {
		BytesRefBuilder flagsScratch = new BytesRefBuilder();
		IntsRefBuilder scratchInts = new IntsRefBuilder();
		StringBuilder sb = new StringBuilder();
		IndexOutput unsorted = tempDir.createTempOutput(tempFileNamePrefix, "dat", IOContext.DEFAULT);
		try (OfflineSorter.ByteSequencesWriter writer = new OfflineSorter.ByteSequencesWriter(unsorted)) {
			for (InputStream dictionary : dictionaries) {
				BufferedReader lines = new BufferedReader(new InputStreamReader(dictionary, decoder));
				String line = lines.readLine();
				while ((line = lines.readLine()) != null) {
					if ((((line.isEmpty()) || ((line.charAt(0)) == '/')) || ((line.charAt(0)) == '#')) || ((line.charAt(0)) == '\t')) {
						continue;
					}
					line = unescapeEntry(line);
					if ((hasStemExceptions) == false) {
						int morphStart = line.indexOf(MORPH_SEPARATOR);
						if ((morphStart >= 0) && (morphStart < (line.length()))) {
							hasStemExceptions = (parseStemException(line.substring((morphStart + 1)))) != null;
						}
					}
					if (needsInputCleaning) {
						int flagSep = line.indexOf(FLAG_SEPARATOR);
						if (flagSep == (-1)) {
							flagSep = line.indexOf(MORPH_SEPARATOR);
						}
						if (flagSep == (-1)) {
							CharSequence cleansed = cleanInput(line, sb);
							writer.write(cleansed.toString().getBytes(StandardCharsets.UTF_8));
						}else {
							String text = line.substring(0, flagSep);
							CharSequence cleansed = cleanInput(text, sb);
							if (cleansed != sb) {
								sb.setLength(0);
								sb.append(cleansed);
							}
							sb.append(line.substring(flagSep));
							writer.write(sb.toString().getBytes(StandardCharsets.UTF_8));
						}
					}else {
						writer.write(line.getBytes(StandardCharsets.UTF_8));
					}
				} 
			}
			CodecUtil.writeFooter(unsorted);
		}
		OfflineSorter sorter = new OfflineSorter(tempDir, tempFileNamePrefix, new Comparator<BytesRef>() {
			BytesRef scratch1 = new BytesRef();

			BytesRef scratch2 = new BytesRef();

			@Override
			public int compare(BytesRef o1, BytesRef o2) {
				scratch1.bytes = o1.bytes;
				scratch1.offset = o1.offset;
				scratch1.length = o1.length;
				for (int i = (scratch1.length) - 1; i >= 0; i--) {
					if (((scratch1.bytes[((scratch1.offset) + i)]) == (FLAG_SEPARATOR)) || ((scratch1.bytes[((scratch1.offset) + i)]) == (MORPH_SEPARATOR))) {
						scratch1.length = i;
						break;
					}
				}
				scratch2.bytes = o2.bytes;
				scratch2.offset = o2.offset;
				scratch2.length = o2.length;
				for (int i = (scratch2.length) - 1; i >= 0; i--) {
					if (((scratch2.bytes[((scratch2.offset) + i)]) == (FLAG_SEPARATOR)) || ((scratch2.bytes[((scratch2.offset) + i)]) == (MORPH_SEPARATOR))) {
						scratch2.length = i;
						break;
					}
				}
				int cmp = scratch1.compareTo(scratch2);
				if (cmp == 0) {
					return o1.compareTo(o2);
				}else {
					return cmp;
				}
			}
		});
		String sorted;
		boolean success = false;
		try {
			sorted = sorter.sort(unsorted.getName());
			success = true;
		} finally {
			if (success) {
				tempDir.deleteFile(unsorted.getName());
			}else {
				IOUtils.deleteFilesIgnoringExceptions(tempDir, unsorted.getName());
			}
		}
		boolean success2 = false;
		try (OfflineSorter.ByteSequencesReader reader = new OfflineSorter.ByteSequencesReader(tempDir.openChecksumInput(sorted, IOContext.READONCE), sorted)) {
			String currentEntry = null;
			IntsRefBuilder currentOrds = new IntsRefBuilder();
			while (true) {
				BytesRef scratch = reader.next();
				if (scratch == null) {
					break;
				}
				String line = scratch.utf8ToString();
				String entry;
				char[] wordForm;
				int end;
				int flagSep = line.indexOf(FLAG_SEPARATOR);
				if (flagSep == (-1)) {
					wordForm = Dictionary.NOFLAGS;
					end = line.indexOf(MORPH_SEPARATOR);
					entry = line.substring(0, end);
				}else {
					end = line.indexOf(MORPH_SEPARATOR);
					String flagPart = line.substring((flagSep + 1), end);
					if ((aliasCount) > 0) {
						flagPart = getAliasValue(Integer.parseInt(flagPart));
					}
					wordForm = flagParsingStrategy.parseFlags(flagPart);
					Arrays.sort(wordForm);
					entry = line.substring(0, flagSep);
				}
				int stemExceptionID = 0;
				if ((hasStemExceptions) && ((end + 1) < (line.length()))) {
					String stemException = parseStemException(line.substring((end + 1)));
					if (stemException != null) {
						if ((stemExceptionCount) == (stemExceptions.length)) {
							int newSize = ArrayUtil.oversize(((stemExceptionCount) + 1), RamUsageEstimator.NUM_BYTES_OBJECT_REF);
							stemExceptions = Arrays.copyOf(stemExceptions, newSize);
						}
						stemExceptionID = (stemExceptionCount) + 1;
						stemExceptions[((stemExceptionCount)++)] = stemException;
					}
				}
				int cmp = (currentEntry == null) ? 1 : entry.compareTo(currentEntry);
				if (cmp < 0) {
					throw new IllegalArgumentException(((("out of order: " + entry) + " < ") + currentEntry));
				}else {
					Dictionary.encodeFlags(flagsScratch, wordForm);
					int ord = flagLookup.add(flagsScratch.get());
					if (ord < 0) {
						ord = (-ord) - 1;
					}
					if ((cmp > 0) && (currentEntry != null)) {
						Util.toUTF32(currentEntry, scratchInts);
						words.add(scratchInts.get(), currentOrds.get());
					}
					if ((cmp > 0) || (currentEntry == null)) {
						currentEntry = entry;
						currentOrds = new IntsRefBuilder();
					}
					if (hasStemExceptions) {
						currentOrds.append(ord);
						currentOrds.append(stemExceptionID);
					}else {
						currentOrds.append(ord);
					}
				}
			} 
			Util.toUTF32(currentEntry, scratchInts);
			words.add(scratchInts.get(), currentOrds.get());
			success2 = true;
		} finally {
			if (success2) {
				tempDir.deleteFile(sorted);
			}else {
				IOUtils.deleteFilesIgnoringExceptions(tempDir, sorted);
			}
		}
	}

	static char[] decodeFlags(BytesRef b) {
		if ((b.length) == 0) {
			return CharsRef.EMPTY_CHARS;
		}
		int len = (b.length) >>> 1;
		char[] flags = new char[len];
		int upto = 0;
		int end = (b.offset) + (b.length);
		for (int i = b.offset; i < end; i += 2) {
			flags[(upto++)] = ((char) (((b.bytes[i]) << 8) | ((b.bytes[(i + 1)]) & 255)));
		}
		return flags;
	}

	static void encodeFlags(BytesRefBuilder b, char[] flags) {
		int len = (flags.length) << 1;
		b.grow(len);
		b.clear();
		for (int i = 0; i < (flags.length); i++) {
			int flag = flags[i];
			b.append(((byte) ((flag >> 8) & 255)));
			b.append(((byte) (flag & 255)));
		}
	}

	private void parseAlias(String line) {
		String[] ruleArgs = line.split("\\s+");
		if ((aliases) == null) {
			final int count = Integer.parseInt(ruleArgs[1]);
			aliases = new String[count];
		}else {
			String aliasValue = ((ruleArgs.length) == 1) ? "" : ruleArgs[1];
			aliases[((aliasCount)++)] = aliasValue;
		}
	}

	private String getAliasValue(int id) {
		try {
			return aliases[(id - 1)];
		} catch (IndexOutOfBoundsException ex) {
			throw new IllegalArgumentException(("Bad flag alias number:" + id), ex);
		}
	}

	String getStemException(int id) {
		return stemExceptions[(id - 1)];
	}

	private void parseMorphAlias(String line) {
		if ((morphAliases) == null) {
			final int count = Integer.parseInt(line.substring(3));
			morphAliases = new String[count];
		}else {
			String arg = line.substring(2);
			morphAliases[((morphAliasCount)++)] = arg;
		}
	}

	private String parseStemException(String morphData) {
		if ((morphAliasCount) > 0) {
			try {
				int alias = Integer.parseInt(morphData.trim());
				morphData = morphAliases[(alias - 1)];
			} catch (NumberFormatException e) {
			}
		}
		int index = morphData.indexOf(" st:");
		if (index < 0) {
			index = morphData.indexOf("\tst:");
		}
		if (index >= 0) {
			int endIndex = Dictionary.indexOfSpaceOrTab(morphData, (index + 1));
			if (endIndex < 0) {
				endIndex = morphData.length();
			}
			return morphData.substring((index + 4), endIndex);
		}
		return null;
	}

	static abstract class FlagParsingStrategy {
		char parseFlag(String rawFlag) {
			char[] flags = parseFlags(rawFlag);
			if ((flags.length) != 1) {
				throw new IllegalArgumentException(("expected only one flag, got: " + rawFlag));
			}
			return flags[0];
		}

		abstract char[] parseFlags(String rawFlags);
	}

	private static class SimpleFlagParsingStrategy extends Dictionary.FlagParsingStrategy {
		@Override
		public char[] parseFlags(String rawFlags) {
			return rawFlags.toCharArray();
		}
	}

	private static class NumFlagParsingStrategy extends Dictionary.FlagParsingStrategy {
		@Override
		public char[] parseFlags(String rawFlags) {
			String[] rawFlagParts = rawFlags.trim().split(",");
			char[] flags = new char[rawFlagParts.length];
			int upto = 0;
			for (int i = 0; i < (rawFlagParts.length); i++) {
				String replacement = rawFlagParts[i].replaceAll("[^0-9]", "");
				if (replacement.isEmpty()) {
					continue;
				}
				flags[(upto++)] = ((char) (Integer.parseInt(replacement)));
			}
			if (upto < (flags.length)) {
				flags = Arrays.copyOf(flags, upto);
			}
			return flags;
		}
	}

	private static class DoubleASCIIFlagParsingStrategy extends Dictionary.FlagParsingStrategy {
		@Override
		public char[] parseFlags(String rawFlags) {
			if ((rawFlags.length()) == 0) {
				return new char[0];
			}
			StringBuilder builder = new StringBuilder();
			if (((rawFlags.length()) % 2) == 1) {
				throw new IllegalArgumentException(("Invalid flags (should be even number of characters): " + rawFlags));
			}
			for (int i = 0; i < (rawFlags.length()); i += 2) {
				char f1 = rawFlags.charAt(i);
				char f2 = rawFlags.charAt((i + 1));
				if ((f1 >= 256) || (f2 >= 256)) {
					throw new IllegalArgumentException(("Invalid flags (LONG flags must be double ASCII): " + rawFlags));
				}
				char combined = ((char) ((f1 << 8) | f2));
				builder.append(combined);
			}
			char[] flags = new char[builder.length()];
			builder.getChars(0, builder.length(), flags, 0);
			return flags;
		}
	}

	static boolean hasFlag(char[] flags, char flag) {
		return (Arrays.binarySearch(flags, flag)) >= 0;
	}

	CharSequence cleanInput(CharSequence input, StringBuilder reuse) {
		reuse.setLength(0);
		for (int i = 0; i < (input.length()); i++) {
			char ch = input.charAt(i);
			if (((ignore) != null) && ((Arrays.binarySearch(ignore, ch)) >= 0)) {
				continue;
			}
			if ((ignoreCase) && ((iconv) == null)) {
				ch = caseFold(ch);
			}
			reuse.append(ch);
		}
		if ((iconv) != null) {
			try {
				Dictionary.applyMappings(iconv, reuse);
			} catch (IOException bogus) {
				throw new RuntimeException(bogus);
			}
			if (ignoreCase) {
				for (int i = 0; i < (reuse.length()); i++) {
					reuse.setCharAt(i, caseFold(reuse.charAt(i)));
				}
			}
		}
		return reuse;
	}

	char caseFold(char c) {
		if (alternateCasing) {
			if (c == 'I') {
				return 'ı';
			}else
				if (c == 'İ') {
					return 'i';
				}else {
					return Character.toLowerCase(c);
				}

		}else {
			return Character.toLowerCase(c);
		}
	}

	static void applyMappings(FST<CharsRef> fst, StringBuilder sb) throws IOException {
		final FST.BytesReader bytesReader = fst.getBytesReader();
		final FST.Arc<CharsRef> firstArc = fst.getFirstArc(new FST.Arc<CharsRef>());
		final CharsRef NO_OUTPUT = fst.outputs.getNoOutput();
		final FST.Arc<CharsRef> arc = new FST.Arc<>();
		int longestMatch;
		CharsRef longestOutput;
		for (int i = 0; i < (sb.length()); i++) {
			arc.copyFrom(firstArc);
			CharsRef output = NO_OUTPUT;
			longestMatch = -1;
			longestOutput = null;
			for (int j = i; j < (sb.length()); j++) {
				char ch = sb.charAt(j);
				if ((fst.findTargetArc(ch, arc, arc, bytesReader)) == null) {
					break;
				}else {
					output = fst.outputs.add(output, arc.output);
				}
				if (arc.isFinal()) {
					longestOutput = fst.outputs.add(output, arc.nextFinalOutput);
					longestMatch = j;
				}
			}
			if (longestMatch >= 0) {
				sb.delete(i, (longestMatch + 1));
				sb.insert(i, longestOutput);
				i += (longestOutput.length) - 1;
			}
		}
	}

	public boolean getIgnoreCase() {
		return ignoreCase;
	}

	private static Path DEFAULT_TEMP_DIR;

	public static void setDefaultTempDir(Path tempDir) {
		Dictionary.DEFAULT_TEMP_DIR = tempDir;
	}

	static synchronized Path getDefaultTempDir() throws IOException {
		if ((Dictionary.DEFAULT_TEMP_DIR) == null) {
			String tempDirPath = System.getProperty("java.io.tmpdir");
			if (tempDirPath == null) {
				throw new IOException("Java has no temporary folder property (java.io.tmpdir)?");
			}
			Path tempDirectory = Paths.get(tempDirPath);
			if ((Files.isWritable(tempDirectory)) == false) {
				throw new IOException(("Java's temporary folder not present or writeable?: " + (tempDirectory.toAbsolutePath())));
			}
			Dictionary.DEFAULT_TEMP_DIR = tempDirectory;
		}
		return Dictionary.DEFAULT_TEMP_DIR;
	}
}

