

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.apache.cassandra.cql3.FieldIdentifier;
import org.apache.cassandra.db.marshal.AbstractType;
import org.apache.cassandra.db.marshal.BytesType;
import org.apache.cassandra.db.marshal.CollectionType;
import org.apache.cassandra.exceptions.ConfigurationException;
import org.apache.cassandra.exceptions.SyntaxException;
import org.apache.cassandra.utils.ByteBufferUtil;
import org.apache.cassandra.utils.FBUtilities;
import org.apache.cassandra.utils.Pair;


public class TypeParser {
	private final String str;

	private int idx;

	private static final Map<String, AbstractType<?>> cache = new HashMap<>();

	public static final TypeParser EMPTY_PARSER = new TypeParser("", 0);

	private TypeParser(String str, int idx) {
		this.str = str;
		this.idx = idx;
	}

	public TypeParser(String str) {
		this(str, 0);
	}

	public static AbstractType<?> parse(String str) throws ConfigurationException, SyntaxException {
		if (str == null)
			return BytesType.instance;

		AbstractType<?> type = TypeParser.cache.get(str);
		if (type != null)
			return type;

		int i = 0;
		i = TypeParser.skipBlank(str, i);
		int j = i;
		while ((!(TypeParser.isEOS(str, i))) && (TypeParser.isIdentifierChar(str.charAt(i))))
			++i;

		if (i == j)
			return BytesType.instance;

		String name = str.substring(j, i);
		i = TypeParser.skipBlank(str, i);
		if ((!(TypeParser.isEOS(str, i))) && ((str.charAt(i)) == '('))
			type = TypeParser.getAbstractType(name, new TypeParser(str, i));
		else
			type = TypeParser.getAbstractType(name);

		TypeParser.cache.put(str, type);
		return type;
	}

	public static AbstractType<?> parse(CharSequence compareWith) throws ConfigurationException, SyntaxException {
		return TypeParser.parse((compareWith == null ? null : compareWith.toString()));
	}

	public AbstractType<?> parse() throws ConfigurationException, SyntaxException {
		skipBlank();
		String name = readNextIdentifier();
		skipBlank();
		if ((!(isEOS())) && ((str.charAt(idx)) == '('))
			return TypeParser.getAbstractType(name, this);
		else
			return TypeParser.getAbstractType(name);

	}

	public Map<String, String> getKeyValueParameters() throws SyntaxException {
		if (isEOS())
			return Collections.emptyMap();

		if ((str.charAt(idx)) != '(')
			throw new IllegalStateException();

		Map<String, String> map = new HashMap<>();
		++(idx);
		while (skipBlankAndComma()) {
			if ((str.charAt(idx)) == ')') {
				++(idx);
				return map;
			}
			String k = readNextIdentifier();
			String v = "";
			skipBlank();
			if ((str.charAt(idx)) == '=') {
				++(idx);
				skipBlank();
				v = readNextIdentifier();
			}else
				if (((str.charAt(idx)) != ',') && ((str.charAt(idx)) != ')')) {
					throwSyntaxError((("unexpected character '" + (str.charAt(idx))) + "'"));
				}

			map.put(k, v);
		} 
		throw new SyntaxException(String.format("Syntax error parsing '%s' at char %d: unexpected end of string", str, idx));
	}

	public List<AbstractType<?>> getTypeParameters() throws ConfigurationException, SyntaxException {
		List<AbstractType<?>> list = new ArrayList<>();
		if (isEOS())
			return list;

		if ((str.charAt(idx)) != '(')
			throw new IllegalStateException();

		++(idx);
		while (skipBlankAndComma()) {
			if ((str.charAt(idx)) == ')') {
				++(idx);
				return list;
			}
			try {
				list.add(parse());
			} catch (SyntaxException e) {
				SyntaxException ex = new SyntaxException(String.format("Exception while parsing '%s' around char %d", str, idx));
				ex.initCause(e);
				throw ex;
			}
		} 
		throw new SyntaxException(String.format("Syntax error parsing '%s' at char %d: unexpected end of string", str, idx));
	}

	public Map<Byte, AbstractType<?>> getAliasParameters() throws ConfigurationException, SyntaxException {
		Map<Byte, AbstractType<?>> map = new HashMap<>();
		if (isEOS())
			return map;

		if ((str.charAt(idx)) != '(')
			throw new IllegalStateException();

		++(idx);
		while (skipBlankAndComma()) {
			if ((str.charAt(idx)) == ')') {
				++(idx);
				return map;
			}
			String alias = readNextIdentifier();
			if ((alias.length()) != 1)
				throwSyntaxError("An alias should be a single character");

			char aliasChar = alias.charAt(0);
			if ((aliasChar < 33) || (aliasChar > 127))
				throwSyntaxError("An alias should be a single character in [0..9a..bA..B-+._&]");

			skipBlank();
			if (!(((str.charAt(idx)) == '=') && ((str.charAt(((idx) + 1))) == '>')))
				throwSyntaxError("expecting '=>' token");

			idx += 2;
			skipBlank();
			try {
				map.put(((byte) (aliasChar)), parse());
			} catch (SyntaxException e) {
				SyntaxException ex = new SyntaxException(String.format("Exception while parsing '%s' around char %d", str, idx));
				ex.initCause(e);
				throw ex;
			}
		} 
		throw new SyntaxException(String.format("Syntax error parsing '%s' at char %d: unexpected end of string", str, idx));
	}

	public Map<ByteBuffer, CollectionType> getCollectionsParameters() throws ConfigurationException, SyntaxException {
		Map<ByteBuffer, CollectionType> map = new HashMap<>();
		if (isEOS())
			return map;

		if ((str.charAt(idx)) != '(')
			throw new IllegalStateException();

		++(idx);
		while (skipBlankAndComma()) {
			if ((str.charAt(idx)) == ')') {
				++(idx);
				return map;
			}
			ByteBuffer bb = fromHex(readNextIdentifier());
			skipBlank();
			if ((str.charAt(idx)) != ':')
				throwSyntaxError("expecting ':' token");

			++(idx);
			skipBlank();
			try {
				AbstractType<?> type = parse();
				if (!(type instanceof CollectionType))
					throw new SyntaxException((type + " is not a collection type"));

				map.put(bb, ((CollectionType) (type)));
			} catch (SyntaxException e) {
				SyntaxException ex = new SyntaxException(String.format("Exception while parsing '%s' around char %d", str, idx));
				ex.initCause(e);
				throw ex;
			}
		} 
		throw new SyntaxException(String.format("Syntax error parsing '%s' at char %d: unexpected end of string", str, idx));
	}

	private ByteBuffer fromHex(String hex) throws SyntaxException {
		try {
			return ByteBufferUtil.hexToBytes(hex);
		} catch (NumberFormatException e) {
			throwSyntaxError(e.getMessage());
			return null;
		}
	}

	public Pair<Pair<String, ByteBuffer>, List<Pair<ByteBuffer, AbstractType>>> getUserTypeParameters() throws ConfigurationException, SyntaxException {
		if ((isEOS()) || ((str.charAt(idx)) != '('))
			throw new IllegalStateException();

		++(idx);
		skipBlankAndComma();
		String keyspace = readNextIdentifier();
		skipBlankAndComma();
		ByteBuffer typeName = fromHex(readNextIdentifier());
		List<Pair<ByteBuffer, AbstractType>> defs = new ArrayList<>();
		while (skipBlankAndComma()) {
			if ((str.charAt(idx)) == ')') {
				++(idx);
				return Pair.create(Pair.create(keyspace, typeName), defs);
			}
			ByteBuffer name = fromHex(readNextIdentifier());
			skipBlank();
			if ((str.charAt(idx)) != ':')
				throwSyntaxError("expecting ':' token");

			++(idx);
			skipBlank();
			try {
				AbstractType type = parse();
				defs.add(Pair.create(name, type));
			} catch (SyntaxException e) {
				SyntaxException ex = new SyntaxException(String.format("Exception while parsing '%s' around char %d", str, idx));
				ex.initCause(e);
				throw ex;
			}
		} 
		throw new SyntaxException(String.format("Syntax error parsing '%s' at char %d: unexpected end of string", str, idx));
	}

	private static AbstractType<?> getAbstractType(String compareWith) throws ConfigurationException {
		String className = (compareWith.contains(".")) ? compareWith : "org.apache.cassandra.db.marshal." + compareWith;
		Class<? extends AbstractType<?>> typeClass = FBUtilities.<AbstractType<?>>classForName(className, "abstract-type");
		try {
			Field field = typeClass.getDeclaredField("instance");
			return ((AbstractType<?>) (field.get(null)));
		} catch (NoSuchFieldException | IllegalAccessException e) {
			return TypeParser.getRawAbstractType(typeClass, TypeParser.EMPTY_PARSER);
		}
	}

	private static AbstractType<?> getAbstractType(String compareWith, TypeParser parser) throws ConfigurationException, SyntaxException {
		String className = (compareWith.contains(".")) ? compareWith : "org.apache.cassandra.db.marshal." + compareWith;
		Class<? extends AbstractType<?>> typeClass = FBUtilities.<AbstractType<?>>classForName(className, "abstract-type");
		try {
			Method method = typeClass.getDeclaredMethod("getInstance", TypeParser.class);
			return ((AbstractType<?>) (method.invoke(null, parser)));
		} catch (NoSuchMethodException | IllegalAccessException e) {
			AbstractType<?> type = TypeParser.getRawAbstractType(typeClass);
		} catch (InvocationTargetException e) {
			ConfigurationException ex = new ConfigurationException((("Invalid definition for comparator " + (typeClass.getName())) + "."));
			ex.initCause(e.getTargetException());
			throw ex;
		}
		return null;
	}

	private static AbstractType<?> getRawAbstractType(Class<? extends AbstractType<?>> typeClass) throws ConfigurationException {
		try {
			Field field = typeClass.getDeclaredField("instance");
			return ((AbstractType<?>) (field.get(null)));
		} catch (NoSuchFieldException | IllegalAccessException e) {
			throw new ConfigurationException((("Invalid comparator class " + (typeClass.getName())) + ": must define a public static instance field or a public static method getInstance(TypeParser)."));
		}
	}

	private static AbstractType<?> getRawAbstractType(Class<? extends AbstractType<?>> typeClass, TypeParser parser) throws ConfigurationException {
		try {
			Method method = typeClass.getDeclaredMethod("getInstance", TypeParser.class);
			return ((AbstractType<?>) (method.invoke(null, parser)));
		} catch (NoSuchMethodException | IllegalAccessException e) {
			throw new ConfigurationException((("Invalid comparator class " + (typeClass.getName())) + ": must define a public static instance field or a public static method getInstance(TypeParser)."));
		} catch (InvocationTargetException e) {
			ConfigurationException ex = new ConfigurationException((("Invalid definition for comparator " + (typeClass.getName())) + "."));
			ex.initCause(e.getTargetException());
			throw ex;
		}
	}

	private void throwSyntaxError(String msg) throws SyntaxException {
		throw new SyntaxException(String.format("Syntax error parsing '%s' at char %d: %s", str, idx, msg));
	}

	private boolean isEOS() {
		return TypeParser.isEOS(str, idx);
	}

	private static boolean isEOS(String str, int i) {
		return i >= (str.length());
	}

	private static boolean isBlank(int c) {
		return ((c == ' ') || (c == '\t')) || (c == '\n');
	}

	private void skipBlank() {
		idx = TypeParser.skipBlank(str, idx);
	}

	private static int skipBlank(String str, int i) {
		while ((!(TypeParser.isEOS(str, i))) && (TypeParser.isBlank(str.charAt(i))))
			++i;

		return i;
	}

	private boolean skipBlankAndComma() {
		boolean commaFound = false;
		while (!(isEOS())) {
			int c = str.charAt(idx);
			if (c == ',') {
				if (commaFound)
					return true;
				else
					commaFound = true;

			}else
				if (!(TypeParser.isBlank(c))) {
					return true;
				}

			++(idx);
		} 
		return false;
	}

	private static boolean isIdentifierChar(int c) {
		return ((((((((c >= '0') && (c <= '9')) || ((c >= 'a') && (c <= 'z'))) || ((c >= 'A') && (c <= 'Z'))) || (c == '-')) || (c == '+')) || (c == '.')) || (c == '_')) || (c == '&');
	}

	public String readNextIdentifier() {
		int i = idx;
		while ((!(isEOS())) && (TypeParser.isIdentifierChar(str.charAt(idx))))
			++(idx);

		return str.substring(i, idx);
	}

	public static String stringifyAliasesParameters(Map<Byte, AbstractType<?>> aliases) {
		StringBuilder sb = new StringBuilder();
		sb.append('(');
		Iterator<Map.Entry<Byte, AbstractType<?>>> iter = aliases.entrySet().iterator();
		if (iter.hasNext()) {
			Map.Entry<Byte, AbstractType<?>> entry = iter.next();
			sb.append(((char) ((byte) (entry.getKey())))).append("=>").append(entry.getValue());
		}
		while (iter.hasNext()) {
			Map.Entry<Byte, AbstractType<?>> entry = iter.next();
			sb.append(',').append(((char) ((byte) (entry.getKey())))).append("=>").append(entry.getValue());
		} 
		sb.append(')');
		return sb.toString();
	}

	public static String stringifyTypeParameters(List<AbstractType<?>> types) {
		return TypeParser.stringifyTypeParameters(types, false);
	}

	public static String stringifyTypeParameters(List<AbstractType<?>> types, boolean ignoreFreezing) {
		StringBuilder sb = new StringBuilder("(");
		for (int i = 0; i < (types.size()); i++) {
			if (i > 0)
				sb.append(",");

			sb.append(types.get(i).toString(ignoreFreezing));
		}
		return sb.append(')').toString();
	}

	public static String stringifyCollectionsParameters(Map<ByteBuffer, ? extends CollectionType> collections) {
		StringBuilder sb = new StringBuilder();
		sb.append('(');
		boolean first = true;
		for (Map.Entry<ByteBuffer, ? extends CollectionType> entry : collections.entrySet()) {
			if (!first)
				sb.append(',');

			first = false;
			sb.append(ByteBufferUtil.bytesToHex(entry.getKey())).append(":");
			sb.append(entry.getValue());
		}
		sb.append(')');
		return sb.toString();
	}

	public static String stringifyUserTypeParameters(String keysace, ByteBuffer typeName, List<FieldIdentifier> fields, List<AbstractType<?>> columnTypes, boolean ignoreFreezing) {
		StringBuilder sb = new StringBuilder();
		sb.append('(').append(keysace).append(",").append(ByteBufferUtil.bytesToHex(typeName));
		for (int i = 0; i < (fields.size()); i++) {
			sb.append(',');
			sb.append(ByteBufferUtil.bytesToHex(fields.get(i).bytes)).append(":");
			sb.append(columnTypes.get(i).toString(ignoreFreezing));
		}
		sb.append(')');
		return sb.toString();
	}
}

