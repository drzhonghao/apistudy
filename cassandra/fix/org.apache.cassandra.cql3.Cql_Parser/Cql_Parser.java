

import com.google.common.base.Optional;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import org.antlr.runtime.BaseRecognizer;
import org.antlr.runtime.BitSet;
import org.antlr.runtime.DFA;
import org.antlr.runtime.IntStream;
import org.antlr.runtime.MismatchedSetException;
import org.antlr.runtime.MismatchedTokenException;
import org.antlr.runtime.NoViableAltException;
import org.antlr.runtime.Parser;
import org.antlr.runtime.ParserRuleReturnScope;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.RecognizerSharedState;
import org.antlr.runtime.Token;
import org.antlr.runtime.TokenStream;
import org.apache.cassandra.auth.DataResource;
import org.apache.cassandra.auth.FunctionResource;
import org.apache.cassandra.auth.IResource;
import org.apache.cassandra.auth.IRoleManager;
import org.apache.cassandra.auth.JMXResource;
import org.apache.cassandra.auth.Permission;
import org.apache.cassandra.auth.RoleOptions;
import org.apache.cassandra.auth.RoleResource;
import org.apache.cassandra.config.ColumnDefinition;
import org.apache.cassandra.cql3.AbstractMarker;
import org.apache.cassandra.cql3.Attributes;
import org.apache.cassandra.cql3.CFName;
import org.apache.cassandra.cql3.CQL3Type;
import org.apache.cassandra.cql3.ColumnCondition;
import org.apache.cassandra.cql3.ColumnIdentifier;
import org.apache.cassandra.cql3.Constants;
import org.apache.cassandra.cql3.CqlParser;
import org.apache.cassandra.cql3.ErrorListener;
import org.apache.cassandra.cql3.FieldIdentifier;
import org.apache.cassandra.cql3.IndexName;
import org.apache.cassandra.cql3.Json;
import org.apache.cassandra.cql3.Lists;
import org.apache.cassandra.cql3.Maps;
import org.apache.cassandra.cql3.MultiColumnRelation;
import org.apache.cassandra.cql3.Operation;
import org.apache.cassandra.cql3.Operator;
import org.apache.cassandra.cql3.RoleName;
import org.apache.cassandra.cql3.Sets;
import org.apache.cassandra.cql3.SingleColumnRelation;
import org.apache.cassandra.cql3.Term;
import org.apache.cassandra.cql3.Term.Raw;
import org.apache.cassandra.cql3.TokenRelation;
import org.apache.cassandra.cql3.Tuples;
import org.apache.cassandra.cql3.TypeCast;
import org.apache.cassandra.cql3.UTName;
import org.apache.cassandra.cql3.UserTypes;
import org.apache.cassandra.cql3.WhereClause;
import org.apache.cassandra.cql3.functions.FunctionCall;
import org.apache.cassandra.cql3.functions.FunctionName;
import org.apache.cassandra.cql3.restrictions.CustomIndexExpression;
import org.apache.cassandra.cql3.selection.RawSelector;
import org.apache.cassandra.cql3.selection.Selectable;
import org.apache.cassandra.cql3.statements.AlterKeyspaceStatement;
import org.apache.cassandra.cql3.statements.AlterRoleStatement;
import org.apache.cassandra.cql3.statements.AlterTableStatement;
import org.apache.cassandra.cql3.statements.AlterTableStatementColumn;
import org.apache.cassandra.cql3.statements.AlterTypeStatement;
import org.apache.cassandra.cql3.statements.AlterViewStatement;
import org.apache.cassandra.cql3.statements.BatchStatement;
import org.apache.cassandra.cql3.statements.CFProperties;
import org.apache.cassandra.cql3.statements.CreateAggregateStatement;
import org.apache.cassandra.cql3.statements.CreateFunctionStatement;
import org.apache.cassandra.cql3.statements.CreateIndexStatement;
import org.apache.cassandra.cql3.statements.CreateKeyspaceStatement;
import org.apache.cassandra.cql3.statements.CreateRoleStatement;
import org.apache.cassandra.cql3.statements.CreateTableStatement;
import org.apache.cassandra.cql3.statements.CreateTriggerStatement;
import org.apache.cassandra.cql3.statements.CreateTypeStatement;
import org.apache.cassandra.cql3.statements.CreateViewStatement;
import org.apache.cassandra.cql3.statements.DeleteStatement;
import org.apache.cassandra.cql3.statements.DropAggregateStatement;
import org.apache.cassandra.cql3.statements.DropFunctionStatement;
import org.apache.cassandra.cql3.statements.DropIndexStatement;
import org.apache.cassandra.cql3.statements.DropKeyspaceStatement;
import org.apache.cassandra.cql3.statements.DropRoleStatement;
import org.apache.cassandra.cql3.statements.DropTableStatement;
import org.apache.cassandra.cql3.statements.DropTriggerStatement;
import org.apache.cassandra.cql3.statements.DropTypeStatement;
import org.apache.cassandra.cql3.statements.DropViewStatement;
import org.apache.cassandra.cql3.statements.GrantPermissionsStatement;
import org.apache.cassandra.cql3.statements.GrantRoleStatement;
import org.apache.cassandra.cql3.statements.IndexPropDefs;
import org.apache.cassandra.cql3.statements.IndexTarget;
import org.apache.cassandra.cql3.statements.KeyspaceAttributes;
import org.apache.cassandra.cql3.statements.ListPermissionsStatement;
import org.apache.cassandra.cql3.statements.ListRolesStatement;
import org.apache.cassandra.cql3.statements.ListUsersStatement;
import org.apache.cassandra.cql3.statements.ModificationStatement;
import org.apache.cassandra.cql3.statements.ParsedStatement;
import org.apache.cassandra.cql3.statements.PropertyDefinitions;
import org.apache.cassandra.cql3.statements.RevokePermissionsStatement;
import org.apache.cassandra.cql3.statements.RevokeRoleStatement;
import org.apache.cassandra.cql3.statements.SelectStatement;
import org.apache.cassandra.cql3.statements.TableAttributes;
import org.apache.cassandra.cql3.statements.TruncateStatement;
import org.apache.cassandra.cql3.statements.UpdateStatement;
import org.apache.cassandra.cql3.statements.UseStatement;
import org.apache.cassandra.exceptions.ConfigurationException;
import org.apache.cassandra.exceptions.InvalidRequestException;
import org.apache.cassandra.exceptions.SyntaxException;
import org.apache.cassandra.utils.Pair;

import static org.apache.cassandra.auth.IRoleManager.Option.LOGIN;
import static org.apache.cassandra.auth.IRoleManager.Option.OPTIONS;
import static org.apache.cassandra.auth.IRoleManager.Option.PASSWORD;
import static org.apache.cassandra.auth.IRoleManager.Option.SUPERUSER;
import static org.apache.cassandra.config.ColumnDefinition.Raw.forQuoted;
import static org.apache.cassandra.config.ColumnDefinition.Raw.forUnquoted;
import static org.apache.cassandra.cql3.CQL3Type.Native.ASCII;
import static org.apache.cassandra.cql3.CQL3Type.Native.BIGINT;
import static org.apache.cassandra.cql3.CQL3Type.Native.BLOB;
import static org.apache.cassandra.cql3.CQL3Type.Native.DATE;
import static org.apache.cassandra.cql3.CQL3Type.Native.DECIMAL;
import static org.apache.cassandra.cql3.CQL3Type.Native.DOUBLE;
import static org.apache.cassandra.cql3.CQL3Type.Native.INET;
import static org.apache.cassandra.cql3.CQL3Type.Native.INT;
import static org.apache.cassandra.cql3.CQL3Type.Native.SMALLINT;
import static org.apache.cassandra.cql3.CQL3Type.Native.TEXT;
import static org.apache.cassandra.cql3.CQL3Type.Native.TIME;
import static org.apache.cassandra.cql3.CQL3Type.Native.TIMESTAMP;
import static org.apache.cassandra.cql3.CQL3Type.Native.TIMEUUID;
import static org.apache.cassandra.cql3.CQL3Type.Native.TINYINT;
import static org.apache.cassandra.cql3.CQL3Type.Native.VARCHAR;
import static org.apache.cassandra.cql3.CQL3Type.Native.VARINT;
import static org.apache.cassandra.cql3.CQL3Type.Raw.from;
import static org.apache.cassandra.cql3.CQL3Type.Raw.frozen;
import static org.apache.cassandra.cql3.CQL3Type.Raw.list;
import static org.apache.cassandra.cql3.CQL3Type.Raw.map;
import static org.apache.cassandra.cql3.CQL3Type.Raw.set;
import static org.apache.cassandra.cql3.CQL3Type.Raw.tuple;
import static org.apache.cassandra.cql3.CQL3Type.Raw.userType;
import static org.apache.cassandra.cql3.ColumnCondition.Raw.collectionCondition;
import static org.apache.cassandra.cql3.ColumnCondition.Raw.collectionInCondition;
import static org.apache.cassandra.cql3.ColumnCondition.Raw.simpleCondition;
import static org.apache.cassandra.cql3.ColumnCondition.Raw.simpleInCondition;
import static org.apache.cassandra.cql3.ColumnCondition.Raw.udtFieldCondition;
import static org.apache.cassandra.cql3.ColumnCondition.Raw.udtFieldInCondition;
import static org.apache.cassandra.cql3.Constants.Literal.bool;
import static org.apache.cassandra.cql3.Constants.Literal.duration;
import static org.apache.cassandra.cql3.Constants.Literal.floatingPoint;
import static org.apache.cassandra.cql3.Constants.Literal.hex;
import static org.apache.cassandra.cql3.Constants.Literal.integer;
import static org.apache.cassandra.cql3.Constants.Literal.string;
import static org.apache.cassandra.cql3.Constants.Literal.uuid;
import static org.apache.cassandra.cql3.selection.Selectable.WithFunction.Raw.newCountRowsFunction;
import static org.apache.cassandra.cql3.statements.AlterTableStatement.Type.ADD;
import static org.apache.cassandra.cql3.statements.AlterTableStatement.Type.ALTER;
import static org.apache.cassandra.cql3.statements.AlterTableStatement.Type.DROP;
import static org.apache.cassandra.cql3.statements.AlterTableStatement.Type.DROP_COMPACT_STORAGE;
import static org.apache.cassandra.cql3.statements.AlterTableStatement.Type.OPTS;
import static org.apache.cassandra.cql3.statements.AlterTableStatement.Type.RENAME;
import static org.apache.cassandra.cql3.statements.BatchStatement.Type.COUNTER;
import static org.apache.cassandra.cql3.statements.BatchStatement.Type.LOGGED;
import static org.apache.cassandra.cql3.statements.BatchStatement.Type.UNLOGGED;
import static org.apache.cassandra.cql3.statements.IndexTarget.Raw.fullCollection;
import static org.apache.cassandra.cql3.statements.IndexTarget.Raw.keysAndValuesOf;
import static org.apache.cassandra.cql3.statements.IndexTarget.Raw.keysOf;
import static org.apache.cassandra.cql3.statements.IndexTarget.Raw.simpleIndexOn;
import static org.apache.cassandra.cql3.statements.IndexTarget.Raw.valuesOf;


@SuppressWarnings("all")
public class Cql_Parser extends Parser {
	public static final int EOF = -1;

	public static final int T__183 = 183;

	public static final int T__184 = 184;

	public static final int T__185 = 185;

	public static final int T__186 = 186;

	public static final int T__187 = 187;

	public static final int T__188 = 188;

	public static final int T__189 = 189;

	public static final int T__190 = 190;

	public static final int T__191 = 191;

	public static final int T__192 = 192;

	public static final int T__193 = 193;

	public static final int T__194 = 194;

	public static final int T__195 = 195;

	public static final int T__196 = 196;

	public static final int T__197 = 197;

	public static final int T__198 = 198;

	public static final int T__199 = 199;

	public static final int T__200 = 200;

	public static final int T__201 = 201;

	public static final int T__202 = 202;

	public static final int T__203 = 203;

	public static final int T__204 = 204;

	public static final int A = 4;

	public static final int B = 5;

	public static final int BOOLEAN = 6;

	public static final int C = 7;

	public static final int COMMENT = 8;

	public static final int D = 9;

	public static final int DIGIT = 10;

	public static final int DURATION = 11;

	public static final int DURATION_UNIT = 12;

	public static final int E = 13;

	public static final int EMPTY_QUOTED_NAME = 14;

	public static final int EXPONENT = 15;

	public static final int F = 16;

	public static final int FLOAT = 17;

	public static final int G = 18;

	public static final int H = 19;

	public static final int HEX = 20;

	public static final int HEXNUMBER = 21;

	public static final int I = 22;

	public static final int IDENT = 23;

	public static final int INTEGER = 24;

	public static final int J = 25;

	public static final int K = 26;

	public static final int K_ADD = 27;

	public static final int K_AGGREGATE = 28;

	public static final int K_ALL = 29;

	public static final int K_ALLOW = 30;

	public static final int K_ALTER = 31;

	public static final int K_AND = 32;

	public static final int K_APPLY = 33;

	public static final int K_AS = 34;

	public static final int K_ASC = 35;

	public static final int K_ASCII = 36;

	public static final int K_AUTHORIZE = 37;

	public static final int K_BATCH = 38;

	public static final int K_BEGIN = 39;

	public static final int K_BIGINT = 40;

	public static final int K_BLOB = 41;

	public static final int K_BOOLEAN = 42;

	public static final int K_BY = 43;

	public static final int K_CALLED = 44;

	public static final int K_CAST = 45;

	public static final int K_CLUSTERING = 46;

	public static final int K_COLUMNFAMILY = 47;

	public static final int K_COMPACT = 48;

	public static final int K_CONTAINS = 49;

	public static final int K_COUNT = 50;

	public static final int K_COUNTER = 51;

	public static final int K_CREATE = 52;

	public static final int K_CUSTOM = 53;

	public static final int K_DATE = 54;

	public static final int K_DECIMAL = 55;

	public static final int K_DEFAULT = 56;

	public static final int K_DELETE = 57;

	public static final int K_DESC = 58;

	public static final int K_DESCRIBE = 59;

	public static final int K_DISTINCT = 60;

	public static final int K_DOUBLE = 61;

	public static final int K_DROP = 62;

	public static final int K_DURATION = 63;

	public static final int K_ENTRIES = 64;

	public static final int K_EXECUTE = 65;

	public static final int K_EXISTS = 66;

	public static final int K_FILTERING = 67;

	public static final int K_FINALFUNC = 68;

	public static final int K_FLOAT = 69;

	public static final int K_FROM = 70;

	public static final int K_FROZEN = 71;

	public static final int K_FULL = 72;

	public static final int K_FUNCTION = 73;

	public static final int K_FUNCTIONS = 74;

	public static final int K_GRANT = 75;

	public static final int K_GROUP = 76;

	public static final int K_IF = 77;

	public static final int K_IN = 78;

	public static final int K_INDEX = 79;

	public static final int K_INET = 80;

	public static final int K_INFINITY = 81;

	public static final int K_INITCOND = 82;

	public static final int K_INPUT = 83;

	public static final int K_INSERT = 84;

	public static final int K_INT = 85;

	public static final int K_INTO = 86;

	public static final int K_IS = 87;

	public static final int K_JSON = 88;

	public static final int K_KEY = 89;

	public static final int K_KEYS = 90;

	public static final int K_KEYSPACE = 91;

	public static final int K_KEYSPACES = 92;

	public static final int K_LANGUAGE = 93;

	public static final int K_LIKE = 94;

	public static final int K_LIMIT = 95;

	public static final int K_LIST = 96;

	public static final int K_LOGIN = 97;

	public static final int K_MAP = 98;

	public static final int K_MATERIALIZED = 99;

	public static final int K_MBEAN = 100;

	public static final int K_MBEANS = 101;

	public static final int K_MODIFY = 102;

	public static final int K_NAN = 103;

	public static final int K_NOLOGIN = 104;

	public static final int K_NORECURSIVE = 105;

	public static final int K_NOSUPERUSER = 106;

	public static final int K_NOT = 107;

	public static final int K_NULL = 108;

	public static final int K_OF = 109;

	public static final int K_ON = 110;

	public static final int K_OPTIONS = 111;

	public static final int K_OR = 112;

	public static final int K_ORDER = 113;

	public static final int K_PARTITION = 114;

	public static final int K_PASSWORD = 115;

	public static final int K_PER = 116;

	public static final int K_PERMISSION = 117;

	public static final int K_PERMISSIONS = 118;

	public static final int K_PRIMARY = 119;

	public static final int K_RENAME = 120;

	public static final int K_REPLACE = 121;

	public static final int K_RETURNS = 122;

	public static final int K_REVOKE = 123;

	public static final int K_ROLE = 124;

	public static final int K_ROLES = 125;

	public static final int K_SELECT = 126;

	public static final int K_SET = 127;

	public static final int K_SFUNC = 128;

	public static final int K_SMALLINT = 129;

	public static final int K_STATIC = 130;

	public static final int K_STORAGE = 131;

	public static final int K_STYPE = 132;

	public static final int K_SUPERUSER = 133;

	public static final int K_TEXT = 134;

	public static final int K_TIME = 135;

	public static final int K_TIMESTAMP = 136;

	public static final int K_TIMEUUID = 137;

	public static final int K_TINYINT = 138;

	public static final int K_TO = 139;

	public static final int K_TOKEN = 140;

	public static final int K_TRIGGER = 141;

	public static final int K_TRUNCATE = 142;

	public static final int K_TTL = 143;

	public static final int K_TUPLE = 144;

	public static final int K_TYPE = 145;

	public static final int K_UNLOGGED = 146;

	public static final int K_UNSET = 147;

	public static final int K_UPDATE = 148;

	public static final int K_USE = 149;

	public static final int K_USER = 150;

	public static final int K_USERS = 151;

	public static final int K_USING = 152;

	public static final int K_UUID = 153;

	public static final int K_VALUES = 154;

	public static final int K_VARCHAR = 155;

	public static final int K_VARINT = 156;

	public static final int K_VIEW = 157;

	public static final int K_WHERE = 158;

	public static final int K_WITH = 159;

	public static final int K_WRITETIME = 160;

	public static final int L = 161;

	public static final int LETTER = 162;

	public static final int M = 163;

	public static final int MULTILINE_COMMENT = 164;

	public static final int N = 165;

	public static final int O = 166;

	public static final int P = 167;

	public static final int Q = 168;

	public static final int QMARK = 169;

	public static final int QUOTED_NAME = 170;

	public static final int R = 171;

	public static final int S = 172;

	public static final int STRING_LITERAL = 173;

	public static final int T = 174;

	public static final int U = 175;

	public static final int UUID = 176;

	public static final int V = 177;

	public static final int W = 178;

	public static final int WS = 179;

	public static final int X = 180;

	public static final int Y = 181;

	public static final int Z = 182;

	public Parser[] getDelegates() {
		return new Parser[]{  };
	}

	public CqlParser gCql;

	public CqlParser gParent;

	public Cql_Parser(TokenStream input, CqlParser gCql) {
		this(input, new RecognizerSharedState(), gCql);
	}

	public Cql_Parser(TokenStream input, RecognizerSharedState state, CqlParser gCql) {
		super(input, state);
		this.gCql = gCql;
		gParent = gCql;
	}

	@Override
	public String[] getTokenNames() {
		return CqlParser.tokenNames;
	}

	@Override
	public String getGrammarFileName() {
		return "Parser.g";
	}

	private final List<ErrorListener> listeners = new ArrayList<ErrorListener>();

	protected final List<ColumnIdentifier> bindVariables = new ArrayList<ColumnIdentifier>();

	public static final Set<String> reservedTypeNames = new HashSet<String>() {
		{
			add("byte");
			add("complex");
			add("enum");
			add("date");
			add("interval");
			add("macaddr");
			add("bitstring");
		}
	};

	public AbstractMarker.Raw newBindVariables(ColumnIdentifier name) {
		AbstractMarker.Raw marker = new AbstractMarker.Raw(bindVariables.size());
		bindVariables.add(name);
		return marker;
	}

	public AbstractMarker.INRaw newINBindVariables(ColumnIdentifier name) {
		AbstractMarker.INRaw marker = new AbstractMarker.INRaw(bindVariables.size());
		bindVariables.add(name);
		return marker;
	}

	public Tuples.Raw newTupleBindVariables(ColumnIdentifier name) {
		Tuples.Raw marker = new Tuples.Raw(bindVariables.size());
		bindVariables.add(name);
		return marker;
	}

	public Tuples.INRaw newTupleINBindVariables(ColumnIdentifier name) {
		Tuples.INRaw marker = new Tuples.INRaw(bindVariables.size());
		bindVariables.add(name);
		return marker;
	}

	public Json.Marker newJsonBindVariables(ColumnIdentifier name) {
		Json.Marker marker = new Json.Marker(bindVariables.size());
		bindVariables.add(name);
		return marker;
	}

	public void addErrorListener(ErrorListener listener) {
		this.listeners.add(listener);
	}

	public void removeErrorListener(ErrorListener listener) {
		this.listeners.remove(listener);
	}

	public void displayRecognitionError(String[] tokenNames, RecognitionException e) {
		for (int i = 0, m = listeners.size(); i < m; i++)
			listeners.get(i).syntaxError(this, tokenNames, e);

	}

	protected void addRecognitionError(String msg) {
		for (int i = 0, m = listeners.size(); i < m; i++)
			listeners.get(i).syntaxError(this, msg);

	}

	public Map<String, String> convertPropertyMap(Maps.Literal map) {
		if (((map == null) || ((map.entries) == null)) || (map.entries.isEmpty()))
			return Collections.<String, String>emptyMap();

		Map<String, String> res = new HashMap<>(map.entries.size());
		for (Pair<Term.Raw, Term.Raw> entry : map.entries) {
			if (((entry.left) == null) || ((entry.right) == null))
				break;

			if (!((entry.left) instanceof Constants.Literal)) {
				String msg = "Invalid property name: " + (entry.left);
				if ((entry.left) instanceof AbstractMarker.Raw)
					msg += " (bind variables are not supported in DDL queries)";

				addRecognitionError(msg);
				break;
			}
			if (!((entry.right) instanceof Constants.Literal)) {
				String msg = (("Invalid property value: " + (entry.right)) + " for property: ") + (entry.left);
				if ((entry.right) instanceof AbstractMarker.Raw)
					msg += " (bind variables are not supported in DDL queries)";

				addRecognitionError(msg);
				break;
			}
			if ((res.put(((Constants.Literal) (entry.left)).getRawText(), ((Constants.Literal) (entry.right)).getRawText())) != null) {
				addRecognitionError(String.format(("Multiple definition for property " + (((Constants.Literal) (entry.left)).getRawText()))));
			}
		}
		return res;
	}

	public void addRawUpdate(List<Pair<ColumnDefinition.Raw, Operation.RawUpdate>> operations, ColumnDefinition.Raw key, Operation.RawUpdate update) {
		for (Pair<ColumnDefinition.Raw, Operation.RawUpdate> p : operations) {
			if ((p.left.equals(key)) && (!(p.right.isCompatibleWith(update))))
				addRecognitionError(("Multiple incompatible setting of column " + key));

		}
		operations.add(Pair.create(key, update));
	}

	public Set<Permission> filterPermissions(Set<Permission> permissions, IResource resource) {
		if (resource == null)
			return Collections.emptySet();

		Set<Permission> filtered = new HashSet<>(permissions);
		filtered.retainAll(resource.applicablePermissions());
		if (filtered.isEmpty())
			addRecognitionError((("Resource type " + (resource.getClass().getSimpleName())) + " does not support any of the requested permissions"));

		return filtered;
	}

	public String canonicalizeObjectName(String s, boolean enforcePattern) {
		if ("".equals(s))
			addRecognitionError("Empty JMX object name supplied");

		if ("*:*".equals(s))
			addRecognitionError("Please use ALL MBEANS instead of wildcard pattern");

		try {
			ObjectName objectName = ObjectName.getInstance(s);
			if (enforcePattern && (!(objectName.isPattern())))
				addRecognitionError((("Plural form used, but non-pattern JMX object name specified (" + s) + ")"));

			return objectName.getCanonicalName();
		} catch (MalformedObjectNameException e) {
			addRecognitionError((s + " is not a valid JMX object name"));
			return s;
		}
	}

	@Override
	protected Object recoverFromMismatchedToken(IntStream input, int ttype, BitSet follow) throws RecognitionException {
		throw new MismatchedTokenException(ttype, input);
	}

	@Override
	public void recover(IntStream input, RecognitionException re) {
	}

	public final ParsedStatement cqlStatement() throws RecognitionException {
		ParsedStatement stmt = null;
		SelectStatement.RawStatement st1 = null;
		ModificationStatement.Parsed st2 = null;
		UpdateStatement.ParsedUpdate st3 = null;
		BatchStatement.Parsed st4 = null;
		DeleteStatement.Parsed st5 = null;
		UseStatement st6 = null;
		TruncateStatement st7 = null;
		CreateKeyspaceStatement st8 = null;
		CreateTableStatement.RawStatement st9 = null;
		CreateIndexStatement st10 = null;
		DropKeyspaceStatement st11 = null;
		DropTableStatement st12 = null;
		DropIndexStatement st13 = null;
		AlterTableStatement st14 = null;
		AlterKeyspaceStatement st15 = null;
		GrantPermissionsStatement st16 = null;
		RevokePermissionsStatement st17 = null;
		ListPermissionsStatement st18 = null;
		CreateRoleStatement st19 = null;
		AlterRoleStatement st20 = null;
		DropRoleStatement st21 = null;
		ListRolesStatement st22 = null;
		CreateTriggerStatement st23 = null;
		DropTriggerStatement st24 = null;
		CreateTypeStatement st25 = null;
		AlterTypeStatement st26 = null;
		DropTypeStatement st27 = null;
		CreateFunctionStatement st28 = null;
		DropFunctionStatement st29 = null;
		CreateAggregateStatement st30 = null;
		DropAggregateStatement st31 = null;
		CreateRoleStatement st32 = null;
		AlterRoleStatement st33 = null;
		DropRoleStatement st34 = null;
		ListRolesStatement st35 = null;
		GrantRoleStatement st36 = null;
		RevokeRoleStatement st37 = null;
		CreateViewStatement st38 = null;
		DropViewStatement st39 = null;
		AlterViewStatement st40 = null;
		try {
			int alt1 = 40;
			alt1 = dfa1.predict(input);
			switch (alt1) {
				case 1 :
					{
						pushFollow(Cql_Parser.FOLLOW_selectStatement_in_cqlStatement59);
						st1 = selectStatement();
						(state._fsp)--;
						stmt = st1;
					}
					break;
				case 2 :
					{
						pushFollow(Cql_Parser.FOLLOW_insertStatement_in_cqlStatement88);
						st2 = insertStatement();
						(state._fsp)--;
						stmt = st2;
					}
					break;
				case 3 :
					{
						pushFollow(Cql_Parser.FOLLOW_updateStatement_in_cqlStatement117);
						st3 = updateStatement();
						(state._fsp)--;
						stmt = st3;
					}
					break;
				case 4 :
					{
						pushFollow(Cql_Parser.FOLLOW_batchStatement_in_cqlStatement146);
						st4 = batchStatement();
						(state._fsp)--;
						stmt = st4;
					}
					break;
				case 5 :
					{
						pushFollow(Cql_Parser.FOLLOW_deleteStatement_in_cqlStatement176);
						st5 = deleteStatement();
						(state._fsp)--;
						stmt = st5;
					}
					break;
				case 6 :
					{
						pushFollow(Cql_Parser.FOLLOW_useStatement_in_cqlStatement205);
						st6 = useStatement();
						(state._fsp)--;
						stmt = st6;
					}
					break;
				case 7 :
					{
						pushFollow(Cql_Parser.FOLLOW_truncateStatement_in_cqlStatement237);
						st7 = truncateStatement();
						(state._fsp)--;
						stmt = st7;
					}
					break;
				case 8 :
					{
						pushFollow(Cql_Parser.FOLLOW_createKeyspaceStatement_in_cqlStatement264);
						st8 = createKeyspaceStatement();
						(state._fsp)--;
						stmt = st8;
					}
					break;
				case 9 :
					{
						pushFollow(Cql_Parser.FOLLOW_createTableStatement_in_cqlStatement285);
						st9 = createTableStatement();
						(state._fsp)--;
						stmt = st9;
					}
					break;
				case 10 :
					{
						pushFollow(Cql_Parser.FOLLOW_createIndexStatement_in_cqlStatement308);
						st10 = createIndexStatement();
						(state._fsp)--;
						stmt = st10;
					}
					break;
				case 11 :
					{
						pushFollow(Cql_Parser.FOLLOW_dropKeyspaceStatement_in_cqlStatement331);
						st11 = dropKeyspaceStatement();
						(state._fsp)--;
						stmt = st11;
					}
					break;
				case 12 :
					{
						pushFollow(Cql_Parser.FOLLOW_dropTableStatement_in_cqlStatement353);
						st12 = dropTableStatement();
						(state._fsp)--;
						stmt = st12;
					}
					break;
				case 13 :
					{
						pushFollow(Cql_Parser.FOLLOW_dropIndexStatement_in_cqlStatement378);
						st13 = dropIndexStatement();
						(state._fsp)--;
						stmt = st13;
					}
					break;
				case 14 :
					{
						pushFollow(Cql_Parser.FOLLOW_alterTableStatement_in_cqlStatement403);
						st14 = alterTableStatement();
						(state._fsp)--;
						stmt = st14;
					}
					break;
				case 15 :
					{
						pushFollow(Cql_Parser.FOLLOW_alterKeyspaceStatement_in_cqlStatement427);
						st15 = alterKeyspaceStatement();
						(state._fsp)--;
						stmt = st15;
					}
					break;
				case 16 :
					{
						pushFollow(Cql_Parser.FOLLOW_grantPermissionsStatement_in_cqlStatement448);
						st16 = grantPermissionsStatement();
						(state._fsp)--;
						stmt = st16;
					}
					break;
				case 17 :
					{
						pushFollow(Cql_Parser.FOLLOW_revokePermissionsStatement_in_cqlStatement466);
						st17 = revokePermissionsStatement();
						(state._fsp)--;
						stmt = st17;
					}
					break;
				case 18 :
					{
						pushFollow(Cql_Parser.FOLLOW_listPermissionsStatement_in_cqlStatement483);
						st18 = listPermissionsStatement();
						(state._fsp)--;
						stmt = st18;
					}
					break;
				case 19 :
					{
						pushFollow(Cql_Parser.FOLLOW_createUserStatement_in_cqlStatement502);
						st19 = createUserStatement();
						(state._fsp)--;
						stmt = st19;
					}
					break;
				case 20 :
					{
						pushFollow(Cql_Parser.FOLLOW_alterUserStatement_in_cqlStatement526);
						st20 = alterUserStatement();
						(state._fsp)--;
						stmt = st20;
					}
					break;
				case 21 :
					{
						pushFollow(Cql_Parser.FOLLOW_dropUserStatement_in_cqlStatement551);
						st21 = dropUserStatement();
						(state._fsp)--;
						stmt = st21;
					}
					break;
				case 22 :
					{
						pushFollow(Cql_Parser.FOLLOW_listUsersStatement_in_cqlStatement577);
						st22 = listUsersStatement();
						(state._fsp)--;
						stmt = st22;
					}
					break;
				case 23 :
					{
						pushFollow(Cql_Parser.FOLLOW_createTriggerStatement_in_cqlStatement602);
						st23 = createTriggerStatement();
						(state._fsp)--;
						stmt = st23;
					}
					break;
				case 24 :
					{
						pushFollow(Cql_Parser.FOLLOW_dropTriggerStatement_in_cqlStatement623);
						st24 = dropTriggerStatement();
						(state._fsp)--;
						stmt = st24;
					}
					break;
				case 25 :
					{
						pushFollow(Cql_Parser.FOLLOW_createTypeStatement_in_cqlStatement646);
						st25 = createTypeStatement();
						(state._fsp)--;
						stmt = st25;
					}
					break;
				case 26 :
					{
						pushFollow(Cql_Parser.FOLLOW_alterTypeStatement_in_cqlStatement670);
						st26 = alterTypeStatement();
						(state._fsp)--;
						stmt = st26;
					}
					break;
				case 27 :
					{
						pushFollow(Cql_Parser.FOLLOW_dropTypeStatement_in_cqlStatement695);
						st27 = dropTypeStatement();
						(state._fsp)--;
						stmt = st27;
					}
					break;
				case 28 :
					{
						pushFollow(Cql_Parser.FOLLOW_createFunctionStatement_in_cqlStatement721);
						st28 = createFunctionStatement();
						(state._fsp)--;
						stmt = st28;
					}
					break;
				case 29 :
					{
						pushFollow(Cql_Parser.FOLLOW_dropFunctionStatement_in_cqlStatement741);
						st29 = dropFunctionStatement();
						(state._fsp)--;
						stmt = st29;
					}
					break;
				case 30 :
					{
						pushFollow(Cql_Parser.FOLLOW_createAggregateStatement_in_cqlStatement763);
						st30 = createAggregateStatement();
						(state._fsp)--;
						stmt = st30;
					}
					break;
				case 31 :
					{
						pushFollow(Cql_Parser.FOLLOW_dropAggregateStatement_in_cqlStatement782);
						st31 = dropAggregateStatement();
						(state._fsp)--;
						stmt = st31;
					}
					break;
				case 32 :
					{
						pushFollow(Cql_Parser.FOLLOW_createRoleStatement_in_cqlStatement803);
						st32 = createRoleStatement();
						(state._fsp)--;
						stmt = st32;
					}
					break;
				case 33 :
					{
						pushFollow(Cql_Parser.FOLLOW_alterRoleStatement_in_cqlStatement827);
						st33 = alterRoleStatement();
						(state._fsp)--;
						stmt = st33;
					}
					break;
				case 34 :
					{
						pushFollow(Cql_Parser.FOLLOW_dropRoleStatement_in_cqlStatement852);
						st34 = dropRoleStatement();
						(state._fsp)--;
						stmt = st34;
					}
					break;
				case 35 :
					{
						pushFollow(Cql_Parser.FOLLOW_listRolesStatement_in_cqlStatement878);
						st35 = listRolesStatement();
						(state._fsp)--;
						stmt = st35;
					}
					break;
				case 36 :
					{
						pushFollow(Cql_Parser.FOLLOW_grantRoleStatement_in_cqlStatement903);
						st36 = grantRoleStatement();
						(state._fsp)--;
						stmt = st36;
					}
					break;
				case 37 :
					{
						pushFollow(Cql_Parser.FOLLOW_revokeRoleStatement_in_cqlStatement928);
						st37 = revokeRoleStatement();
						(state._fsp)--;
						stmt = st37;
					}
					break;
				case 38 :
					{
						pushFollow(Cql_Parser.FOLLOW_createMaterializedViewStatement_in_cqlStatement952);
						st38 = createMaterializedViewStatement();
						(state._fsp)--;
						stmt = st38;
					}
					break;
				case 39 :
					{
						pushFollow(Cql_Parser.FOLLOW_dropMaterializedViewStatement_in_cqlStatement964);
						st39 = dropMaterializedViewStatement();
						(state._fsp)--;
						stmt = st39;
					}
					break;
				case 40 :
					{
						pushFollow(Cql_Parser.FOLLOW_alterMaterializedViewStatement_in_cqlStatement978);
						st40 = alterMaterializedViewStatement();
						(state._fsp)--;
						stmt = st40;
					}
					break;
			}
			if (stmt != null)
				stmt.setBoundVariables(bindVariables);

		} catch (RecognitionException re) {
			reportError(re);
			recover(input, re);
		} finally {
		}
		return stmt;
	}

	public final UseStatement useStatement() throws RecognitionException {
		UseStatement stmt = null;
		String ks = null;
		try {
			{
				match(input, Cql_Parser.K_USE, Cql_Parser.FOLLOW_K_USE_in_useStatement1004);
				pushFollow(Cql_Parser.FOLLOW_keyspaceName_in_useStatement1008);
				ks = keyspaceName();
				(state._fsp)--;
				stmt = new UseStatement(ks);
			}
		} catch (RecognitionException re) {
			reportError(re);
			recover(input, re);
		} finally {
		}
		return stmt;
	}

	public final SelectStatement.RawStatement selectStatement() throws RecognitionException {
		SelectStatement.RawStatement expr = null;
		List<RawSelector> sclause = null;
		CFName cf = null;
		WhereClause.Builder wclause = null;
		Term.Raw rows = null;
		boolean isDistinct = false;
		Term.Raw limit = null;
		Term.Raw perPartitionLimit = null;
		Map<ColumnDefinition.Raw, Boolean> orderings = new LinkedHashMap<>();
		List<ColumnDefinition.Raw> groups = new ArrayList<>();
		boolean allowFiltering = false;
		boolean isJson = false;
		try {
			{
				match(input, Cql_Parser.K_SELECT, Cql_Parser.FOLLOW_K_SELECT_in_selectStatement1042);
				int alt2 = 2;
				int LA2_0 = input.LA(1);
				if (LA2_0 == (Cql_Parser.K_JSON)) {
					int LA2_1 = input.LA(2);
					if ((((((((((((((((((((((((((((((((((((((((((((LA2_1 == (Cql_Parser.BOOLEAN)) || (LA2_1 == (Cql_Parser.DURATION))) || (LA2_1 == (Cql_Parser.EMPTY_QUOTED_NAME))) || (LA2_1 == (Cql_Parser.FLOAT))) || (LA2_1 == (Cql_Parser.HEXNUMBER))) || ((LA2_1 >= (Cql_Parser.IDENT)) && (LA2_1 <= (Cql_Parser.INTEGER)))) || ((LA2_1 >= (Cql_Parser.K_AGGREGATE)) && (LA2_1 <= (Cql_Parser.K_ALL)))) || (LA2_1 == (Cql_Parser.K_ASCII))) || ((LA2_1 >= (Cql_Parser.K_BIGINT)) && (LA2_1 <= (Cql_Parser.K_BOOLEAN)))) || ((LA2_1 >= (Cql_Parser.K_CALLED)) && (LA2_1 <= (Cql_Parser.K_CLUSTERING)))) || ((LA2_1 >= (Cql_Parser.K_COMPACT)) && (LA2_1 <= (Cql_Parser.K_COUNTER)))) || ((LA2_1 >= (Cql_Parser.K_CUSTOM)) && (LA2_1 <= (Cql_Parser.K_DECIMAL)))) || ((LA2_1 >= (Cql_Parser.K_DISTINCT)) && (LA2_1 <= (Cql_Parser.K_DOUBLE)))) || (LA2_1 == (Cql_Parser.K_DURATION))) || ((LA2_1 >= (Cql_Parser.K_EXISTS)) && (LA2_1 <= (Cql_Parser.K_FLOAT)))) || (LA2_1 == (Cql_Parser.K_FROZEN))) || ((LA2_1 >= (Cql_Parser.K_FUNCTION)) && (LA2_1 <= (Cql_Parser.K_FUNCTIONS)))) || (LA2_1 == (Cql_Parser.K_GROUP))) || ((LA2_1 >= (Cql_Parser.K_INET)) && (LA2_1 <= (Cql_Parser.K_INPUT)))) || (LA2_1 == (Cql_Parser.K_INT))) || ((LA2_1 >= (Cql_Parser.K_JSON)) && (LA2_1 <= (Cql_Parser.K_KEYS)))) || ((LA2_1 >= (Cql_Parser.K_KEYSPACES)) && (LA2_1 <= (Cql_Parser.K_LIKE)))) || ((LA2_1 >= (Cql_Parser.K_LIST)) && (LA2_1 <= (Cql_Parser.K_MAP)))) || ((LA2_1 >= (Cql_Parser.K_NAN)) && (LA2_1 <= (Cql_Parser.K_NOLOGIN)))) || (LA2_1 == (Cql_Parser.K_NOSUPERUSER))) || (LA2_1 == (Cql_Parser.K_NULL))) || (LA2_1 == (Cql_Parser.K_OPTIONS))) || ((LA2_1 >= (Cql_Parser.K_PARTITION)) && (LA2_1 <= (Cql_Parser.K_PERMISSIONS)))) || (LA2_1 == (Cql_Parser.K_RETURNS))) || ((LA2_1 >= (Cql_Parser.K_ROLE)) && (LA2_1 <= (Cql_Parser.K_ROLES)))) || ((LA2_1 >= (Cql_Parser.K_SFUNC)) && (LA2_1 <= (Cql_Parser.K_TINYINT)))) || ((LA2_1 >= (Cql_Parser.K_TOKEN)) && (LA2_1 <= (Cql_Parser.K_TRIGGER)))) || ((LA2_1 >= (Cql_Parser.K_TTL)) && (LA2_1 <= (Cql_Parser.K_TYPE)))) || ((LA2_1 >= (Cql_Parser.K_USER)) && (LA2_1 <= (Cql_Parser.K_USERS)))) || ((LA2_1 >= (Cql_Parser.K_UUID)) && (LA2_1 <= (Cql_Parser.K_VARINT)))) || (LA2_1 == (Cql_Parser.K_WRITETIME))) || ((LA2_1 >= (Cql_Parser.QMARK)) && (LA2_1 <= (Cql_Parser.QUOTED_NAME)))) || (LA2_1 == (Cql_Parser.STRING_LITERAL))) || (LA2_1 == (Cql_Parser.UUID))) || (LA2_1 == 184)) || (LA2_1 == 189)) || (LA2_1 == 192)) || ((LA2_1 >= 199) && (LA2_1 <= 200))) || (LA2_1 == 203)) {
						alt2 = 1;
					}else
						if (LA2_1 == (Cql_Parser.K_AS)) {
							int LA2_4 = input.LA(3);
							if ((((LA2_4 == (Cql_Parser.K_FROM)) || (LA2_4 == 184)) || (LA2_4 == 188)) || (LA2_4 == 191)) {
								alt2 = 1;
							}else
								if (LA2_4 == (Cql_Parser.K_AS)) {
									int LA2_5 = input.LA(4);
									if (((((((((((((((((((((((((((((((((LA2_5 == (Cql_Parser.IDENT)) || ((LA2_5 >= (Cql_Parser.K_AGGREGATE)) && (LA2_5 <= (Cql_Parser.K_ALL)))) || (LA2_5 == (Cql_Parser.K_AS))) || (LA2_5 == (Cql_Parser.K_ASCII))) || ((LA2_5 >= (Cql_Parser.K_BIGINT)) && (LA2_5 <= (Cql_Parser.K_BOOLEAN)))) || ((LA2_5 >= (Cql_Parser.K_CALLED)) && (LA2_5 <= (Cql_Parser.K_CLUSTERING)))) || ((LA2_5 >= (Cql_Parser.K_COMPACT)) && (LA2_5 <= (Cql_Parser.K_COUNTER)))) || ((LA2_5 >= (Cql_Parser.K_CUSTOM)) && (LA2_5 <= (Cql_Parser.K_DECIMAL)))) || ((LA2_5 >= (Cql_Parser.K_DISTINCT)) && (LA2_5 <= (Cql_Parser.K_DOUBLE)))) || (LA2_5 == (Cql_Parser.K_DURATION))) || ((LA2_5 >= (Cql_Parser.K_EXISTS)) && (LA2_5 <= (Cql_Parser.K_FLOAT)))) || (LA2_5 == (Cql_Parser.K_FROZEN))) || ((LA2_5 >= (Cql_Parser.K_FUNCTION)) && (LA2_5 <= (Cql_Parser.K_FUNCTIONS)))) || (LA2_5 == (Cql_Parser.K_GROUP))) || (LA2_5 == (Cql_Parser.K_INET))) || ((LA2_5 >= (Cql_Parser.K_INITCOND)) && (LA2_5 <= (Cql_Parser.K_INPUT)))) || (LA2_5 == (Cql_Parser.K_INT))) || ((LA2_5 >= (Cql_Parser.K_JSON)) && (LA2_5 <= (Cql_Parser.K_KEYS)))) || ((LA2_5 >= (Cql_Parser.K_KEYSPACES)) && (LA2_5 <= (Cql_Parser.K_LIKE)))) || ((LA2_5 >= (Cql_Parser.K_LIST)) && (LA2_5 <= (Cql_Parser.K_MAP)))) || (LA2_5 == (Cql_Parser.K_NOLOGIN))) || (LA2_5 == (Cql_Parser.K_NOSUPERUSER))) || (LA2_5 == (Cql_Parser.K_OPTIONS))) || ((LA2_5 >= (Cql_Parser.K_PARTITION)) && (LA2_5 <= (Cql_Parser.K_PERMISSIONS)))) || (LA2_5 == (Cql_Parser.K_RETURNS))) || ((LA2_5 >= (Cql_Parser.K_ROLE)) && (LA2_5 <= (Cql_Parser.K_ROLES)))) || ((LA2_5 >= (Cql_Parser.K_SFUNC)) && (LA2_5 <= (Cql_Parser.K_TINYINT)))) || (LA2_5 == (Cql_Parser.K_TRIGGER))) || ((LA2_5 >= (Cql_Parser.K_TTL)) && (LA2_5 <= (Cql_Parser.K_TYPE)))) || ((LA2_5 >= (Cql_Parser.K_USER)) && (LA2_5 <= (Cql_Parser.K_USERS)))) || ((LA2_5 >= (Cql_Parser.K_UUID)) && (LA2_5 <= (Cql_Parser.K_VARINT)))) || (LA2_5 == (Cql_Parser.K_WRITETIME))) || (LA2_5 == (Cql_Parser.QUOTED_NAME))) {
										alt2 = 1;
									}
								}

						}

				}
				switch (alt2) {
					case 1 :
						{
							match(input, Cql_Parser.K_JSON, Cql_Parser.FOLLOW_K_JSON_in_selectStatement1052);
							isJson = true;
						}
						break;
				}
				{
					int alt3 = 2;
					int LA3_0 = input.LA(1);
					if (LA3_0 == (Cql_Parser.K_DISTINCT)) {
						int LA3_1 = input.LA(2);
						if ((((((((((((((((((((((((((((((((((((((((((((LA3_1 == (Cql_Parser.BOOLEAN)) || (LA3_1 == (Cql_Parser.DURATION))) || (LA3_1 == (Cql_Parser.EMPTY_QUOTED_NAME))) || (LA3_1 == (Cql_Parser.FLOAT))) || (LA3_1 == (Cql_Parser.HEXNUMBER))) || ((LA3_1 >= (Cql_Parser.IDENT)) && (LA3_1 <= (Cql_Parser.INTEGER)))) || ((LA3_1 >= (Cql_Parser.K_AGGREGATE)) && (LA3_1 <= (Cql_Parser.K_ALL)))) || (LA3_1 == (Cql_Parser.K_ASCII))) || ((LA3_1 >= (Cql_Parser.K_BIGINT)) && (LA3_1 <= (Cql_Parser.K_BOOLEAN)))) || ((LA3_1 >= (Cql_Parser.K_CALLED)) && (LA3_1 <= (Cql_Parser.K_CLUSTERING)))) || ((LA3_1 >= (Cql_Parser.K_COMPACT)) && (LA3_1 <= (Cql_Parser.K_COUNTER)))) || ((LA3_1 >= (Cql_Parser.K_CUSTOM)) && (LA3_1 <= (Cql_Parser.K_DECIMAL)))) || ((LA3_1 >= (Cql_Parser.K_DISTINCT)) && (LA3_1 <= (Cql_Parser.K_DOUBLE)))) || (LA3_1 == (Cql_Parser.K_DURATION))) || ((LA3_1 >= (Cql_Parser.K_EXISTS)) && (LA3_1 <= (Cql_Parser.K_FLOAT)))) || (LA3_1 == (Cql_Parser.K_FROZEN))) || ((LA3_1 >= (Cql_Parser.K_FUNCTION)) && (LA3_1 <= (Cql_Parser.K_FUNCTIONS)))) || (LA3_1 == (Cql_Parser.K_GROUP))) || ((LA3_1 >= (Cql_Parser.K_INET)) && (LA3_1 <= (Cql_Parser.K_INPUT)))) || (LA3_1 == (Cql_Parser.K_INT))) || ((LA3_1 >= (Cql_Parser.K_JSON)) && (LA3_1 <= (Cql_Parser.K_KEYS)))) || ((LA3_1 >= (Cql_Parser.K_KEYSPACES)) && (LA3_1 <= (Cql_Parser.K_LIKE)))) || ((LA3_1 >= (Cql_Parser.K_LIST)) && (LA3_1 <= (Cql_Parser.K_MAP)))) || ((LA3_1 >= (Cql_Parser.K_NAN)) && (LA3_1 <= (Cql_Parser.K_NOLOGIN)))) || (LA3_1 == (Cql_Parser.K_NOSUPERUSER))) || (LA3_1 == (Cql_Parser.K_NULL))) || (LA3_1 == (Cql_Parser.K_OPTIONS))) || ((LA3_1 >= (Cql_Parser.K_PARTITION)) && (LA3_1 <= (Cql_Parser.K_PERMISSIONS)))) || (LA3_1 == (Cql_Parser.K_RETURNS))) || ((LA3_1 >= (Cql_Parser.K_ROLE)) && (LA3_1 <= (Cql_Parser.K_ROLES)))) || ((LA3_1 >= (Cql_Parser.K_SFUNC)) && (LA3_1 <= (Cql_Parser.K_TINYINT)))) || ((LA3_1 >= (Cql_Parser.K_TOKEN)) && (LA3_1 <= (Cql_Parser.K_TRIGGER)))) || ((LA3_1 >= (Cql_Parser.K_TTL)) && (LA3_1 <= (Cql_Parser.K_TYPE)))) || ((LA3_1 >= (Cql_Parser.K_USER)) && (LA3_1 <= (Cql_Parser.K_USERS)))) || ((LA3_1 >= (Cql_Parser.K_UUID)) && (LA3_1 <= (Cql_Parser.K_VARINT)))) || (LA3_1 == (Cql_Parser.K_WRITETIME))) || ((LA3_1 >= (Cql_Parser.QMARK)) && (LA3_1 <= (Cql_Parser.QUOTED_NAME)))) || (LA3_1 == (Cql_Parser.STRING_LITERAL))) || (LA3_1 == (Cql_Parser.UUID))) || (LA3_1 == 184)) || (LA3_1 == 189)) || (LA3_1 == 192)) || ((LA3_1 >= 199) && (LA3_1 <= 200))) || (LA3_1 == 203)) {
							alt3 = 1;
						}else
							if (LA3_1 == (Cql_Parser.K_AS)) {
								int LA3_4 = input.LA(3);
								if ((((LA3_4 == (Cql_Parser.K_FROM)) || (LA3_4 == 184)) || (LA3_4 == 188)) || (LA3_4 == 191)) {
									alt3 = 1;
								}else
									if (LA3_4 == (Cql_Parser.K_AS)) {
										int LA3_5 = input.LA(4);
										if (((((((((((((((((((((((((((((((((LA3_5 == (Cql_Parser.IDENT)) || ((LA3_5 >= (Cql_Parser.K_AGGREGATE)) && (LA3_5 <= (Cql_Parser.K_ALL)))) || (LA3_5 == (Cql_Parser.K_AS))) || (LA3_5 == (Cql_Parser.K_ASCII))) || ((LA3_5 >= (Cql_Parser.K_BIGINT)) && (LA3_5 <= (Cql_Parser.K_BOOLEAN)))) || ((LA3_5 >= (Cql_Parser.K_CALLED)) && (LA3_5 <= (Cql_Parser.K_CLUSTERING)))) || ((LA3_5 >= (Cql_Parser.K_COMPACT)) && (LA3_5 <= (Cql_Parser.K_COUNTER)))) || ((LA3_5 >= (Cql_Parser.K_CUSTOM)) && (LA3_5 <= (Cql_Parser.K_DECIMAL)))) || ((LA3_5 >= (Cql_Parser.K_DISTINCT)) && (LA3_5 <= (Cql_Parser.K_DOUBLE)))) || (LA3_5 == (Cql_Parser.K_DURATION))) || ((LA3_5 >= (Cql_Parser.K_EXISTS)) && (LA3_5 <= (Cql_Parser.K_FLOAT)))) || (LA3_5 == (Cql_Parser.K_FROZEN))) || ((LA3_5 >= (Cql_Parser.K_FUNCTION)) && (LA3_5 <= (Cql_Parser.K_FUNCTIONS)))) || (LA3_5 == (Cql_Parser.K_GROUP))) || (LA3_5 == (Cql_Parser.K_INET))) || ((LA3_5 >= (Cql_Parser.K_INITCOND)) && (LA3_5 <= (Cql_Parser.K_INPUT)))) || (LA3_5 == (Cql_Parser.K_INT))) || ((LA3_5 >= (Cql_Parser.K_JSON)) && (LA3_5 <= (Cql_Parser.K_KEYS)))) || ((LA3_5 >= (Cql_Parser.K_KEYSPACES)) && (LA3_5 <= (Cql_Parser.K_LIKE)))) || ((LA3_5 >= (Cql_Parser.K_LIST)) && (LA3_5 <= (Cql_Parser.K_MAP)))) || (LA3_5 == (Cql_Parser.K_NOLOGIN))) || (LA3_5 == (Cql_Parser.K_NOSUPERUSER))) || (LA3_5 == (Cql_Parser.K_OPTIONS))) || ((LA3_5 >= (Cql_Parser.K_PARTITION)) && (LA3_5 <= (Cql_Parser.K_PERMISSIONS)))) || (LA3_5 == (Cql_Parser.K_RETURNS))) || ((LA3_5 >= (Cql_Parser.K_ROLE)) && (LA3_5 <= (Cql_Parser.K_ROLES)))) || ((LA3_5 >= (Cql_Parser.K_SFUNC)) && (LA3_5 <= (Cql_Parser.K_TINYINT)))) || (LA3_5 == (Cql_Parser.K_TRIGGER))) || ((LA3_5 >= (Cql_Parser.K_TTL)) && (LA3_5 <= (Cql_Parser.K_TYPE)))) || ((LA3_5 >= (Cql_Parser.K_USER)) && (LA3_5 <= (Cql_Parser.K_USERS)))) || ((LA3_5 >= (Cql_Parser.K_UUID)) && (LA3_5 <= (Cql_Parser.K_VARINT)))) || (LA3_5 == (Cql_Parser.K_WRITETIME))) || (LA3_5 == (Cql_Parser.QUOTED_NAME))) {
											alt3 = 1;
										}
									}

							}

					}
					switch (alt3) {
						case 1 :
							{
								match(input, Cql_Parser.K_DISTINCT, Cql_Parser.FOLLOW_K_DISTINCT_in_selectStatement1069);
								isDistinct = true;
							}
							break;
					}
					pushFollow(Cql_Parser.FOLLOW_selectClause_in_selectStatement1078);
					sclause = selectClause();
					(state._fsp)--;
				}
				match(input, Cql_Parser.K_FROM, Cql_Parser.FOLLOW_K_FROM_in_selectStatement1088);
				pushFollow(Cql_Parser.FOLLOW_columnFamilyName_in_selectStatement1092);
				cf = columnFamilyName();
				(state._fsp)--;
				int alt4 = 2;
				int LA4_0 = input.LA(1);
				if (LA4_0 == (Cql_Parser.K_WHERE)) {
					alt4 = 1;
				}
				switch (alt4) {
					case 1 :
						{
							match(input, Cql_Parser.K_WHERE, Cql_Parser.FOLLOW_K_WHERE_in_selectStatement1102);
							pushFollow(Cql_Parser.FOLLOW_whereClause_in_selectStatement1106);
							wclause = whereClause();
							(state._fsp)--;
						}
						break;
				}
				int alt6 = 2;
				int LA6_0 = input.LA(1);
				if (LA6_0 == (Cql_Parser.K_GROUP)) {
					alt6 = 1;
				}
				switch (alt6) {
					case 1 :
						{
							match(input, Cql_Parser.K_GROUP, Cql_Parser.FOLLOW_K_GROUP_in_selectStatement1119);
							match(input, Cql_Parser.K_BY, Cql_Parser.FOLLOW_K_BY_in_selectStatement1121);
							pushFollow(Cql_Parser.FOLLOW_groupByClause_in_selectStatement1123);
							groupByClause(groups);
							(state._fsp)--;
							loop5 : while (true) {
								int alt5 = 2;
								int LA5_0 = input.LA(1);
								if (LA5_0 == 188) {
									alt5 = 1;
								}
								switch (alt5) {
									case 1 :
										{
											match(input, 188, Cql_Parser.FOLLOW_188_in_selectStatement1128);
											pushFollow(Cql_Parser.FOLLOW_groupByClause_in_selectStatement1130);
											groupByClause(groups);
											(state._fsp)--;
										}
										break;
									default :
										break loop5;
								}
							} 
						}
						break;
				}
				int alt8 = 2;
				int LA8_0 = input.LA(1);
				if (LA8_0 == (Cql_Parser.K_ORDER)) {
					alt8 = 1;
				}
				switch (alt8) {
					case 1 :
						{
							match(input, Cql_Parser.K_ORDER, Cql_Parser.FOLLOW_K_ORDER_in_selectStatement1147);
							match(input, Cql_Parser.K_BY, Cql_Parser.FOLLOW_K_BY_in_selectStatement1149);
							pushFollow(Cql_Parser.FOLLOW_orderByClause_in_selectStatement1151);
							orderByClause(orderings);
							(state._fsp)--;
							loop7 : while (true) {
								int alt7 = 2;
								int LA7_0 = input.LA(1);
								if (LA7_0 == 188) {
									alt7 = 1;
								}
								switch (alt7) {
									case 1 :
										{
											match(input, 188, Cql_Parser.FOLLOW_188_in_selectStatement1156);
											pushFollow(Cql_Parser.FOLLOW_orderByClause_in_selectStatement1158);
											orderByClause(orderings);
											(state._fsp)--;
										}
										break;
									default :
										break loop7;
								}
							} 
						}
						break;
				}
				int alt9 = 2;
				int LA9_0 = input.LA(1);
				if (LA9_0 == (Cql_Parser.K_PER)) {
					alt9 = 1;
				}
				switch (alt9) {
					case 1 :
						{
							match(input, Cql_Parser.K_PER, Cql_Parser.FOLLOW_K_PER_in_selectStatement1175);
							match(input, Cql_Parser.K_PARTITION, Cql_Parser.FOLLOW_K_PARTITION_in_selectStatement1177);
							match(input, Cql_Parser.K_LIMIT, Cql_Parser.FOLLOW_K_LIMIT_in_selectStatement1179);
							pushFollow(Cql_Parser.FOLLOW_intValue_in_selectStatement1183);
							rows = intValue();
							(state._fsp)--;
							perPartitionLimit = rows;
						}
						break;
				}
				int alt10 = 2;
				int LA10_0 = input.LA(1);
				if (LA10_0 == (Cql_Parser.K_LIMIT)) {
					alt10 = 1;
				}
				switch (alt10) {
					case 1 :
						{
							match(input, Cql_Parser.K_LIMIT, Cql_Parser.FOLLOW_K_LIMIT_in_selectStatement1198);
							pushFollow(Cql_Parser.FOLLOW_intValue_in_selectStatement1202);
							rows = intValue();
							(state._fsp)--;
							limit = rows;
						}
						break;
				}
				int alt11 = 2;
				int LA11_0 = input.LA(1);
				if (LA11_0 == (Cql_Parser.K_ALLOW)) {
					alt11 = 1;
				}
				switch (alt11) {
					case 1 :
						{
							match(input, Cql_Parser.K_ALLOW, Cql_Parser.FOLLOW_K_ALLOW_in_selectStatement1217);
							match(input, Cql_Parser.K_FILTERING, Cql_Parser.FOLLOW_K_FILTERING_in_selectStatement1219);
							allowFiltering = true;
						}
						break;
				}
				SelectStatement.Parameters params = new SelectStatement.Parameters(orderings, groups, isDistinct, allowFiltering, isJson);
				WhereClause where = (wclause == null) ? WhereClause.empty() : wclause.build();
				expr = new SelectStatement.RawStatement(cf, params, sclause, where, limit, perPartitionLimit);
			}
		} catch (RecognitionException re) {
			reportError(re);
			recover(input, re);
		} finally {
		}
		return expr;
	}

	public final List<RawSelector> selectClause() throws RecognitionException {
		List<RawSelector> expr = null;
		RawSelector t1 = null;
		RawSelector tN = null;
		try {
			int alt13 = 2;
			int LA13_0 = input.LA(1);
			if (((((((((((((((((((((((((((((((((((((((((((((LA13_0 == (Cql_Parser.BOOLEAN)) || (LA13_0 == (Cql_Parser.DURATION))) || (LA13_0 == (Cql_Parser.EMPTY_QUOTED_NAME))) || (LA13_0 == (Cql_Parser.FLOAT))) || (LA13_0 == (Cql_Parser.HEXNUMBER))) || ((LA13_0 >= (Cql_Parser.IDENT)) && (LA13_0 <= (Cql_Parser.INTEGER)))) || ((LA13_0 >= (Cql_Parser.K_AGGREGATE)) && (LA13_0 <= (Cql_Parser.K_ALL)))) || (LA13_0 == (Cql_Parser.K_AS))) || (LA13_0 == (Cql_Parser.K_ASCII))) || ((LA13_0 >= (Cql_Parser.K_BIGINT)) && (LA13_0 <= (Cql_Parser.K_BOOLEAN)))) || ((LA13_0 >= (Cql_Parser.K_CALLED)) && (LA13_0 <= (Cql_Parser.K_CLUSTERING)))) || ((LA13_0 >= (Cql_Parser.K_COMPACT)) && (LA13_0 <= (Cql_Parser.K_COUNTER)))) || ((LA13_0 >= (Cql_Parser.K_CUSTOM)) && (LA13_0 <= (Cql_Parser.K_DECIMAL)))) || ((LA13_0 >= (Cql_Parser.K_DISTINCT)) && (LA13_0 <= (Cql_Parser.K_DOUBLE)))) || (LA13_0 == (Cql_Parser.K_DURATION))) || ((LA13_0 >= (Cql_Parser.K_EXISTS)) && (LA13_0 <= (Cql_Parser.K_FLOAT)))) || (LA13_0 == (Cql_Parser.K_FROZEN))) || ((LA13_0 >= (Cql_Parser.K_FUNCTION)) && (LA13_0 <= (Cql_Parser.K_FUNCTIONS)))) || (LA13_0 == (Cql_Parser.K_GROUP))) || ((LA13_0 >= (Cql_Parser.K_INET)) && (LA13_0 <= (Cql_Parser.K_INPUT)))) || (LA13_0 == (Cql_Parser.K_INT))) || ((LA13_0 >= (Cql_Parser.K_JSON)) && (LA13_0 <= (Cql_Parser.K_KEYS)))) || ((LA13_0 >= (Cql_Parser.K_KEYSPACES)) && (LA13_0 <= (Cql_Parser.K_LIKE)))) || ((LA13_0 >= (Cql_Parser.K_LIST)) && (LA13_0 <= (Cql_Parser.K_MAP)))) || ((LA13_0 >= (Cql_Parser.K_NAN)) && (LA13_0 <= (Cql_Parser.K_NOLOGIN)))) || (LA13_0 == (Cql_Parser.K_NOSUPERUSER))) || (LA13_0 == (Cql_Parser.K_NULL))) || (LA13_0 == (Cql_Parser.K_OPTIONS))) || ((LA13_0 >= (Cql_Parser.K_PARTITION)) && (LA13_0 <= (Cql_Parser.K_PERMISSIONS)))) || (LA13_0 == (Cql_Parser.K_RETURNS))) || ((LA13_0 >= (Cql_Parser.K_ROLE)) && (LA13_0 <= (Cql_Parser.K_ROLES)))) || ((LA13_0 >= (Cql_Parser.K_SFUNC)) && (LA13_0 <= (Cql_Parser.K_TINYINT)))) || ((LA13_0 >= (Cql_Parser.K_TOKEN)) && (LA13_0 <= (Cql_Parser.K_TRIGGER)))) || ((LA13_0 >= (Cql_Parser.K_TTL)) && (LA13_0 <= (Cql_Parser.K_TYPE)))) || ((LA13_0 >= (Cql_Parser.K_USER)) && (LA13_0 <= (Cql_Parser.K_USERS)))) || ((LA13_0 >= (Cql_Parser.K_UUID)) && (LA13_0 <= (Cql_Parser.K_VARINT)))) || (LA13_0 == (Cql_Parser.K_WRITETIME))) || ((LA13_0 >= (Cql_Parser.QMARK)) && (LA13_0 <= (Cql_Parser.QUOTED_NAME)))) || (LA13_0 == (Cql_Parser.STRING_LITERAL))) || (LA13_0 == (Cql_Parser.UUID))) || (LA13_0 == 184)) || (LA13_0 == 189)) || (LA13_0 == 192)) || (LA13_0 == 199)) || (LA13_0 == 203)) {
				alt13 = 1;
			}else
				if (LA13_0 == 200) {
					alt13 = 2;
				}else {
					NoViableAltException nvae = new NoViableAltException("", 13, 0, input);
					throw nvae;
				}

			switch (alt13) {
				case 1 :
					{
						pushFollow(Cql_Parser.FOLLOW_selector_in_selectClause1256);
						t1 = selector();
						(state._fsp)--;
						expr = new ArrayList<RawSelector>();
						expr.add(t1);
						loop12 : while (true) {
							int alt12 = 2;
							int LA12_0 = input.LA(1);
							if (LA12_0 == 188) {
								alt12 = 1;
							}
							switch (alt12) {
								case 1 :
									{
										match(input, 188, Cql_Parser.FOLLOW_188_in_selectClause1261);
										pushFollow(Cql_Parser.FOLLOW_selector_in_selectClause1265);
										tN = selector();
										(state._fsp)--;
										expr.add(tN);
									}
									break;
								default :
									break loop12;
							}
						} 
					}
					break;
				case 2 :
					{
						match(input, 200, Cql_Parser.FOLLOW_200_in_selectClause1277);
						expr = Collections.<RawSelector>emptyList();
					}
					break;
			}
		} catch (RecognitionException re) {
			reportError(re);
			recover(input, re);
		} finally {
		}
		return expr;
	}

	public final RawSelector selector() throws RecognitionException {
		RawSelector s = null;
		Selectable.Raw us = null;
		ColumnIdentifier c = null;
		ColumnIdentifier alias = null;
		try {
			{
				pushFollow(Cql_Parser.FOLLOW_unaliasedSelector_in_selector1310);
				us = unaliasedSelector();
				(state._fsp)--;
				int alt14 = 2;
				int LA14_0 = input.LA(1);
				if (LA14_0 == (Cql_Parser.K_AS)) {
					alt14 = 1;
				}
				switch (alt14) {
					case 1 :
						{
							match(input, Cql_Parser.K_AS, Cql_Parser.FOLLOW_K_AS_in_selector1313);
							pushFollow(Cql_Parser.FOLLOW_noncol_ident_in_selector1317);
							c = noncol_ident();
							(state._fsp)--;
							alias = c;
						}
						break;
				}
				s = new RawSelector(us, alias);
			}
		} catch (RecognitionException re) {
			reportError(re);
			recover(input, re);
		} finally {
		}
		return s;
	}

	public final Selectable.Raw unaliasedSelector() throws RecognitionException {
		Selectable.Raw s = null;
		ColumnDefinition.Raw c = null;
		Term.Raw v = null;
		CQL3Type.Raw ct = null;
		Selectable.Raw sn = null;
		CQL3Type t = null;
		FunctionName f = null;
		List<Selectable.Raw> args = null;
		FieldIdentifier fi = null;
		Selectable.Raw tmp = null;
		try {
			{
				int alt15 = 8;
				alt15 = dfa15.predict(input);
				switch (alt15) {
					case 1 :
						{
							pushFollow(Cql_Parser.FOLLOW_cident_in_unaliasedSelector1360);
							c = cident();
							(state._fsp)--;
							tmp = c;
						}
						break;
					case 2 :
						{
							pushFollow(Cql_Parser.FOLLOW_value_in_unaliasedSelector1408);
							v = value();
							(state._fsp)--;
							tmp = new Selectable.WithTerm.Raw(v);
						}
						break;
					case 3 :
						{
							match(input, 184, Cql_Parser.FOLLOW_184_in_unaliasedSelector1455);
							pushFollow(Cql_Parser.FOLLOW_comparatorType_in_unaliasedSelector1459);
							ct = comparatorType();
							(state._fsp)--;
							match(input, 185, Cql_Parser.FOLLOW_185_in_unaliasedSelector1461);
							pushFollow(Cql_Parser.FOLLOW_value_in_unaliasedSelector1465);
							v = value();
							(state._fsp)--;
							tmp = new Selectable.WithTerm.Raw(new TypeCast(ct, v));
						}
						break;
					case 4 :
						{
							match(input, Cql_Parser.K_COUNT, Cql_Parser.FOLLOW_K_COUNT_in_unaliasedSelector1486);
							match(input, 184, Cql_Parser.FOLLOW_184_in_unaliasedSelector1488);
							match(input, 200, Cql_Parser.FOLLOW_200_in_unaliasedSelector1490);
							match(input, 185, Cql_Parser.FOLLOW_185_in_unaliasedSelector1492);
							tmp = newCountRowsFunction();
						}
						break;
					case 5 :
						{
							match(input, Cql_Parser.K_WRITETIME, Cql_Parser.FOLLOW_K_WRITETIME_in_unaliasedSelector1526);
							match(input, 184, Cql_Parser.FOLLOW_184_in_unaliasedSelector1528);
							pushFollow(Cql_Parser.FOLLOW_cident_in_unaliasedSelector1532);
							c = cident();
							(state._fsp)--;
							match(input, 185, Cql_Parser.FOLLOW_185_in_unaliasedSelector1534);
							tmp = new Selectable.WritetimeOrTTL.Raw(c, true);
						}
						break;
					case 6 :
						{
							match(input, Cql_Parser.K_TTL, Cql_Parser.FOLLOW_K_TTL_in_unaliasedSelector1560);
							match(input, 184, Cql_Parser.FOLLOW_184_in_unaliasedSelector1568);
							pushFollow(Cql_Parser.FOLLOW_cident_in_unaliasedSelector1572);
							c = cident();
							(state._fsp)--;
							match(input, 185, Cql_Parser.FOLLOW_185_in_unaliasedSelector1574);
							tmp = new Selectable.WritetimeOrTTL.Raw(c, false);
						}
						break;
					case 7 :
						{
							match(input, Cql_Parser.K_CAST, Cql_Parser.FOLLOW_K_CAST_in_unaliasedSelector1600);
							match(input, 184, Cql_Parser.FOLLOW_184_in_unaliasedSelector1607);
							pushFollow(Cql_Parser.FOLLOW_unaliasedSelector_in_unaliasedSelector1611);
							sn = unaliasedSelector();
							(state._fsp)--;
							match(input, Cql_Parser.K_AS, Cql_Parser.FOLLOW_K_AS_in_unaliasedSelector1613);
							pushFollow(Cql_Parser.FOLLOW_native_type_in_unaliasedSelector1617);
							t = native_type();
							(state._fsp)--;
							match(input, 185, Cql_Parser.FOLLOW_185_in_unaliasedSelector1619);
							tmp = new Selectable.WithCast.Raw(sn, t);
						}
						break;
					case 8 :
						{
							pushFollow(Cql_Parser.FOLLOW_functionName_in_unaliasedSelector1634);
							f = functionName();
							(state._fsp)--;
							pushFollow(Cql_Parser.FOLLOW_selectionFunctionArgs_in_unaliasedSelector1638);
							args = selectionFunctionArgs();
							(state._fsp)--;
							tmp = new Selectable.WithFunction.Raw(f, args);
						}
						break;
				}
				loop16 : while (true) {
					int alt16 = 2;
					int LA16_0 = input.LA(1);
					if (LA16_0 == 191) {
						alt16 = 1;
					}
					switch (alt16) {
						case 1 :
							{
								match(input, 191, Cql_Parser.FOLLOW_191_in_unaliasedSelector1653);
								pushFollow(Cql_Parser.FOLLOW_fident_in_unaliasedSelector1657);
								fi = fident();
								(state._fsp)--;
								tmp = new Selectable.WithFieldSelection.Raw(tmp, fi);
							}
							break;
						default :
							break loop16;
					}
				} 
				s = tmp;
			}
		} catch (RecognitionException re) {
			reportError(re);
			recover(input, re);
		} finally {
		}
		return s;
	}

	public final List<Selectable.Raw> selectionFunctionArgs() throws RecognitionException {
		List<Selectable.Raw> a = null;
		Selectable.Raw s1 = null;
		Selectable.Raw sn = null;
		try {
			int alt18 = 2;
			int LA18_0 = input.LA(1);
			if (LA18_0 == 184) {
				int LA18_1 = input.LA(2);
				if (LA18_1 == 185) {
					alt18 = 1;
				}else
					if (((((((((((((((((((((((((((((((((((((((((((((LA18_1 == (Cql_Parser.BOOLEAN)) || (LA18_1 == (Cql_Parser.DURATION))) || (LA18_1 == (Cql_Parser.EMPTY_QUOTED_NAME))) || (LA18_1 == (Cql_Parser.FLOAT))) || (LA18_1 == (Cql_Parser.HEXNUMBER))) || ((LA18_1 >= (Cql_Parser.IDENT)) && (LA18_1 <= (Cql_Parser.INTEGER)))) || ((LA18_1 >= (Cql_Parser.K_AGGREGATE)) && (LA18_1 <= (Cql_Parser.K_ALL)))) || (LA18_1 == (Cql_Parser.K_AS))) || (LA18_1 == (Cql_Parser.K_ASCII))) || ((LA18_1 >= (Cql_Parser.K_BIGINT)) && (LA18_1 <= (Cql_Parser.K_BOOLEAN)))) || ((LA18_1 >= (Cql_Parser.K_CALLED)) && (LA18_1 <= (Cql_Parser.K_CLUSTERING)))) || ((LA18_1 >= (Cql_Parser.K_COMPACT)) && (LA18_1 <= (Cql_Parser.K_COUNTER)))) || ((LA18_1 >= (Cql_Parser.K_CUSTOM)) && (LA18_1 <= (Cql_Parser.K_DECIMAL)))) || ((LA18_1 >= (Cql_Parser.K_DISTINCT)) && (LA18_1 <= (Cql_Parser.K_DOUBLE)))) || (LA18_1 == (Cql_Parser.K_DURATION))) || ((LA18_1 >= (Cql_Parser.K_EXISTS)) && (LA18_1 <= (Cql_Parser.K_FLOAT)))) || (LA18_1 == (Cql_Parser.K_FROZEN))) || ((LA18_1 >= (Cql_Parser.K_FUNCTION)) && (LA18_1 <= (Cql_Parser.K_FUNCTIONS)))) || (LA18_1 == (Cql_Parser.K_GROUP))) || ((LA18_1 >= (Cql_Parser.K_INET)) && (LA18_1 <= (Cql_Parser.K_INPUT)))) || (LA18_1 == (Cql_Parser.K_INT))) || ((LA18_1 >= (Cql_Parser.K_JSON)) && (LA18_1 <= (Cql_Parser.K_KEYS)))) || ((LA18_1 >= (Cql_Parser.K_KEYSPACES)) && (LA18_1 <= (Cql_Parser.K_LIKE)))) || ((LA18_1 >= (Cql_Parser.K_LIST)) && (LA18_1 <= (Cql_Parser.K_MAP)))) || ((LA18_1 >= (Cql_Parser.K_NAN)) && (LA18_1 <= (Cql_Parser.K_NOLOGIN)))) || (LA18_1 == (Cql_Parser.K_NOSUPERUSER))) || (LA18_1 == (Cql_Parser.K_NULL))) || (LA18_1 == (Cql_Parser.K_OPTIONS))) || ((LA18_1 >= (Cql_Parser.K_PARTITION)) && (LA18_1 <= (Cql_Parser.K_PERMISSIONS)))) || (LA18_1 == (Cql_Parser.K_RETURNS))) || ((LA18_1 >= (Cql_Parser.K_ROLE)) && (LA18_1 <= (Cql_Parser.K_ROLES)))) || ((LA18_1 >= (Cql_Parser.K_SFUNC)) && (LA18_1 <= (Cql_Parser.K_TINYINT)))) || ((LA18_1 >= (Cql_Parser.K_TOKEN)) && (LA18_1 <= (Cql_Parser.K_TRIGGER)))) || ((LA18_1 >= (Cql_Parser.K_TTL)) && (LA18_1 <= (Cql_Parser.K_TYPE)))) || ((LA18_1 >= (Cql_Parser.K_USER)) && (LA18_1 <= (Cql_Parser.K_USERS)))) || ((LA18_1 >= (Cql_Parser.K_UUID)) && (LA18_1 <= (Cql_Parser.K_VARINT)))) || (LA18_1 == (Cql_Parser.K_WRITETIME))) || ((LA18_1 >= (Cql_Parser.QMARK)) && (LA18_1 <= (Cql_Parser.QUOTED_NAME)))) || (LA18_1 == (Cql_Parser.STRING_LITERAL))) || (LA18_1 == (Cql_Parser.UUID))) || (LA18_1 == 184)) || (LA18_1 == 189)) || (LA18_1 == 192)) || (LA18_1 == 199)) || (LA18_1 == 203)) {
						alt18 = 2;
					}else {
						int nvaeMark = input.mark();
						try {
							input.consume();
							NoViableAltException nvae = new NoViableAltException("", 18, 1, input);
							throw nvae;
						} finally {
							input.rewind(nvaeMark);
						}
					}

			}else {
				NoViableAltException nvae = new NoViableAltException("", 18, 0, input);
				throw nvae;
			}
			switch (alt18) {
				case 1 :
					{
						match(input, 184, Cql_Parser.FOLLOW_184_in_selectionFunctionArgs1685);
						match(input, 185, Cql_Parser.FOLLOW_185_in_selectionFunctionArgs1687);
						a = Collections.emptyList();
					}
					break;
				case 2 :
					{
						match(input, 184, Cql_Parser.FOLLOW_184_in_selectionFunctionArgs1697);
						pushFollow(Cql_Parser.FOLLOW_unaliasedSelector_in_selectionFunctionArgs1701);
						s1 = unaliasedSelector();
						(state._fsp)--;
						List<Selectable.Raw> args = new ArrayList<Selectable.Raw>();
						args.add(s1);
						loop17 : while (true) {
							int alt17 = 2;
							int LA17_0 = input.LA(1);
							if (LA17_0 == 188) {
								alt17 = 1;
							}
							switch (alt17) {
								case 1 :
									{
										match(input, 188, Cql_Parser.FOLLOW_188_in_selectionFunctionArgs1717);
										pushFollow(Cql_Parser.FOLLOW_unaliasedSelector_in_selectionFunctionArgs1721);
										sn = unaliasedSelector();
										(state._fsp)--;
										args.add(sn);
									}
									break;
								default :
									break loop17;
							}
						} 
						match(input, 185, Cql_Parser.FOLLOW_185_in_selectionFunctionArgs1734);
						a = args;
					}
					break;
			}
		} catch (RecognitionException re) {
			reportError(re);
			recover(input, re);
		} finally {
		}
		return a;
	}

	public final WhereClause.Builder whereClause() throws RecognitionException {
		WhereClause.Builder clause = null;
		clause = new WhereClause.Builder();
		try {
			{
				pushFollow(Cql_Parser.FOLLOW_relationOrExpression_in_whereClause1765);
				relationOrExpression(clause);
				(state._fsp)--;
				loop19 : while (true) {
					int alt19 = 2;
					int LA19_0 = input.LA(1);
					if (LA19_0 == (Cql_Parser.K_AND)) {
						alt19 = 1;
					}
					switch (alt19) {
						case 1 :
							{
								match(input, Cql_Parser.K_AND, Cql_Parser.FOLLOW_K_AND_in_whereClause1769);
								pushFollow(Cql_Parser.FOLLOW_relationOrExpression_in_whereClause1771);
								relationOrExpression(clause);
								(state._fsp)--;
							}
							break;
						default :
							break loop19;
					}
				} 
			}
		} catch (RecognitionException re) {
			reportError(re);
			recover(input, re);
		} finally {
		}
		return clause;
	}

	public final void relationOrExpression(WhereClause.Builder clause) throws RecognitionException {
		try {
			int alt20 = 2;
			int LA20_0 = input.LA(1);
			if (((((((((((((((((((((((((((((((((((LA20_0 == (Cql_Parser.EMPTY_QUOTED_NAME)) || (LA20_0 == (Cql_Parser.IDENT))) || ((LA20_0 >= (Cql_Parser.K_AGGREGATE)) && (LA20_0 <= (Cql_Parser.K_ALL)))) || (LA20_0 == (Cql_Parser.K_AS))) || (LA20_0 == (Cql_Parser.K_ASCII))) || ((LA20_0 >= (Cql_Parser.K_BIGINT)) && (LA20_0 <= (Cql_Parser.K_BOOLEAN)))) || ((LA20_0 >= (Cql_Parser.K_CALLED)) && (LA20_0 <= (Cql_Parser.K_CLUSTERING)))) || ((LA20_0 >= (Cql_Parser.K_COMPACT)) && (LA20_0 <= (Cql_Parser.K_COUNTER)))) || ((LA20_0 >= (Cql_Parser.K_CUSTOM)) && (LA20_0 <= (Cql_Parser.K_DECIMAL)))) || ((LA20_0 >= (Cql_Parser.K_DISTINCT)) && (LA20_0 <= (Cql_Parser.K_DOUBLE)))) || (LA20_0 == (Cql_Parser.K_DURATION))) || ((LA20_0 >= (Cql_Parser.K_EXISTS)) && (LA20_0 <= (Cql_Parser.K_FLOAT)))) || (LA20_0 == (Cql_Parser.K_FROZEN))) || ((LA20_0 >= (Cql_Parser.K_FUNCTION)) && (LA20_0 <= (Cql_Parser.K_FUNCTIONS)))) || (LA20_0 == (Cql_Parser.K_GROUP))) || (LA20_0 == (Cql_Parser.K_INET))) || ((LA20_0 >= (Cql_Parser.K_INITCOND)) && (LA20_0 <= (Cql_Parser.K_INPUT)))) || (LA20_0 == (Cql_Parser.K_INT))) || ((LA20_0 >= (Cql_Parser.K_JSON)) && (LA20_0 <= (Cql_Parser.K_KEYS)))) || ((LA20_0 >= (Cql_Parser.K_KEYSPACES)) && (LA20_0 <= (Cql_Parser.K_LIKE)))) || ((LA20_0 >= (Cql_Parser.K_LIST)) && (LA20_0 <= (Cql_Parser.K_MAP)))) || (LA20_0 == (Cql_Parser.K_NOLOGIN))) || (LA20_0 == (Cql_Parser.K_NOSUPERUSER))) || (LA20_0 == (Cql_Parser.K_OPTIONS))) || ((LA20_0 >= (Cql_Parser.K_PARTITION)) && (LA20_0 <= (Cql_Parser.K_PERMISSIONS)))) || (LA20_0 == (Cql_Parser.K_RETURNS))) || ((LA20_0 >= (Cql_Parser.K_ROLE)) && (LA20_0 <= (Cql_Parser.K_ROLES)))) || ((LA20_0 >= (Cql_Parser.K_SFUNC)) && (LA20_0 <= (Cql_Parser.K_TINYINT)))) || ((LA20_0 >= (Cql_Parser.K_TOKEN)) && (LA20_0 <= (Cql_Parser.K_TRIGGER)))) || ((LA20_0 >= (Cql_Parser.K_TTL)) && (LA20_0 <= (Cql_Parser.K_TYPE)))) || ((LA20_0 >= (Cql_Parser.K_USER)) && (LA20_0 <= (Cql_Parser.K_USERS)))) || ((LA20_0 >= (Cql_Parser.K_UUID)) && (LA20_0 <= (Cql_Parser.K_VARINT)))) || (LA20_0 == (Cql_Parser.K_WRITETIME))) || (LA20_0 == (Cql_Parser.QUOTED_NAME))) || (LA20_0 == 184)) {
				alt20 = 1;
			}else
				if (LA20_0 == 202) {
					alt20 = 2;
				}else {
					NoViableAltException nvae = new NoViableAltException("", 20, 0, input);
					throw nvae;
				}

			switch (alt20) {
				case 1 :
					{
						pushFollow(Cql_Parser.FOLLOW_relation_in_relationOrExpression1793);
						relation(clause);
						(state._fsp)--;
					}
					break;
				case 2 :
					{
						pushFollow(Cql_Parser.FOLLOW_customIndexExpression_in_relationOrExpression1802);
						customIndexExpression(clause);
						(state._fsp)--;
					}
					break;
			}
		} catch (RecognitionException re) {
			reportError(re);
			recover(input, re);
		} finally {
		}
	}

	public final void customIndexExpression(WhereClause.Builder clause) throws RecognitionException {
		Term.Raw t = null;
		IndexName name = new IndexName();
		try {
			{
				match(input, 202, Cql_Parser.FOLLOW_202_in_customIndexExpression1830);
				pushFollow(Cql_Parser.FOLLOW_idxName_in_customIndexExpression1832);
				idxName(name);
				(state._fsp)--;
				match(input, 188, Cql_Parser.FOLLOW_188_in_customIndexExpression1835);
				pushFollow(Cql_Parser.FOLLOW_term_in_customIndexExpression1839);
				t = term();
				(state._fsp)--;
				match(input, 185, Cql_Parser.FOLLOW_185_in_customIndexExpression1841);
				clause.add(new CustomIndexExpression(name, t));
			}
		} catch (RecognitionException re) {
			reportError(re);
			recover(input, re);
		} finally {
		}
	}

	public final void orderByClause(Map<ColumnDefinition.Raw, Boolean> orderings) throws RecognitionException {
		ColumnDefinition.Raw c = null;
		boolean reversed = false;
		try {
			{
				pushFollow(Cql_Parser.FOLLOW_cident_in_orderByClause1871);
				c = cident();
				(state._fsp)--;
				int alt21 = 3;
				int LA21_0 = input.LA(1);
				if (LA21_0 == (Cql_Parser.K_ASC)) {
					alt21 = 1;
				}else
					if (LA21_0 == (Cql_Parser.K_DESC)) {
						alt21 = 2;
					}

				switch (alt21) {
					case 1 :
						{
							match(input, Cql_Parser.K_ASC, Cql_Parser.FOLLOW_K_ASC_in_orderByClause1874);
						}
						break;
					case 2 :
						{
							match(input, Cql_Parser.K_DESC, Cql_Parser.FOLLOW_K_DESC_in_orderByClause1878);
							reversed = true;
						}
						break;
				}
				orderings.put(c, reversed);
			}
		} catch (RecognitionException re) {
			reportError(re);
			recover(input, re);
		} finally {
		}
	}

	public final void groupByClause(List<ColumnDefinition.Raw> groups) throws RecognitionException {
		ColumnDefinition.Raw c = null;
		try {
			{
				pushFollow(Cql_Parser.FOLLOW_cident_in_groupByClause1904);
				c = cident();
				(state._fsp)--;
				groups.add(c);
			}
		} catch (RecognitionException re) {
			reportError(re);
			recover(input, re);
		} finally {
		}
	}

	public final ModificationStatement.Parsed insertStatement() throws RecognitionException {
		ModificationStatement.Parsed expr = null;
		CFName cf = null;
		UpdateStatement.ParsedInsert st1 = null;
		UpdateStatement.ParsedInsertJson st2 = null;
		try {
			{
				match(input, Cql_Parser.K_INSERT, Cql_Parser.FOLLOW_K_INSERT_in_insertStatement1929);
				match(input, Cql_Parser.K_INTO, Cql_Parser.FOLLOW_K_INTO_in_insertStatement1931);
				pushFollow(Cql_Parser.FOLLOW_columnFamilyName_in_insertStatement1935);
				cf = columnFamilyName();
				(state._fsp)--;
				int alt22 = 2;
				int LA22_0 = input.LA(1);
				if (LA22_0 == 184) {
					alt22 = 1;
				}else
					if (LA22_0 == (Cql_Parser.K_JSON)) {
						alt22 = 2;
					}else {
						NoViableAltException nvae = new NoViableAltException("", 22, 0, input);
						throw nvae;
					}

				switch (alt22) {
					case 1 :
						{
							pushFollow(Cql_Parser.FOLLOW_normalInsertStatement_in_insertStatement1949);
							st1 = normalInsertStatement(cf);
							(state._fsp)--;
							expr = st1;
						}
						break;
					case 2 :
						{
							match(input, Cql_Parser.K_JSON, Cql_Parser.FOLLOW_K_JSON_in_insertStatement1964);
							pushFollow(Cql_Parser.FOLLOW_jsonInsertStatement_in_insertStatement1968);
							st2 = jsonInsertStatement(cf);
							(state._fsp)--;
							expr = st2;
						}
						break;
				}
			}
		} catch (RecognitionException re) {
			reportError(re);
			recover(input, re);
		} finally {
		}
		return expr;
	}

	public final UpdateStatement.ParsedInsert normalInsertStatement(CFName cf) throws RecognitionException {
		UpdateStatement.ParsedInsert expr = null;
		ColumnDefinition.Raw c1 = null;
		ColumnDefinition.Raw cn = null;
		Term.Raw v1 = null;
		Term.Raw vn = null;
		Attributes.Raw attrs = new Attributes.Raw();
		List<ColumnDefinition.Raw> columnNames = new ArrayList<>();
		List<Term.Raw> values = new ArrayList<>();
		boolean ifNotExists = false;
		try {
			{
				match(input, 184, Cql_Parser.FOLLOW_184_in_normalInsertStatement2004);
				pushFollow(Cql_Parser.FOLLOW_cident_in_normalInsertStatement2008);
				c1 = cident();
				(state._fsp)--;
				columnNames.add(c1);
				loop23 : while (true) {
					int alt23 = 2;
					int LA23_0 = input.LA(1);
					if (LA23_0 == 188) {
						alt23 = 1;
					}
					switch (alt23) {
						case 1 :
							{
								match(input, 188, Cql_Parser.FOLLOW_188_in_normalInsertStatement2015);
								pushFollow(Cql_Parser.FOLLOW_cident_in_normalInsertStatement2019);
								cn = cident();
								(state._fsp)--;
								columnNames.add(cn);
							}
							break;
						default :
							break loop23;
					}
				} 
				match(input, 185, Cql_Parser.FOLLOW_185_in_normalInsertStatement2026);
				match(input, Cql_Parser.K_VALUES, Cql_Parser.FOLLOW_K_VALUES_in_normalInsertStatement2034);
				match(input, 184, Cql_Parser.FOLLOW_184_in_normalInsertStatement2042);
				pushFollow(Cql_Parser.FOLLOW_term_in_normalInsertStatement2046);
				v1 = term();
				(state._fsp)--;
				values.add(v1);
				loop24 : while (true) {
					int alt24 = 2;
					int LA24_0 = input.LA(1);
					if (LA24_0 == 188) {
						alt24 = 1;
					}
					switch (alt24) {
						case 1 :
							{
								match(input, 188, Cql_Parser.FOLLOW_188_in_normalInsertStatement2052);
								pushFollow(Cql_Parser.FOLLOW_term_in_normalInsertStatement2056);
								vn = term();
								(state._fsp)--;
								values.add(vn);
							}
							break;
						default :
							break loop24;
					}
				} 
				match(input, 185, Cql_Parser.FOLLOW_185_in_normalInsertStatement2063);
				int alt25 = 2;
				int LA25_0 = input.LA(1);
				if (LA25_0 == (Cql_Parser.K_IF)) {
					alt25 = 1;
				}
				switch (alt25) {
					case 1 :
						{
							match(input, Cql_Parser.K_IF, Cql_Parser.FOLLOW_K_IF_in_normalInsertStatement2073);
							match(input, Cql_Parser.K_NOT, Cql_Parser.FOLLOW_K_NOT_in_normalInsertStatement2075);
							match(input, Cql_Parser.K_EXISTS, Cql_Parser.FOLLOW_K_EXISTS_in_normalInsertStatement2077);
							ifNotExists = true;
						}
						break;
				}
				int alt26 = 2;
				int LA26_0 = input.LA(1);
				if (LA26_0 == (Cql_Parser.K_USING)) {
					alt26 = 1;
				}
				switch (alt26) {
					case 1 :
						{
							pushFollow(Cql_Parser.FOLLOW_usingClause_in_normalInsertStatement2092);
							usingClause(attrs);
							(state._fsp)--;
						}
						break;
				}
				expr = new UpdateStatement.ParsedInsert(cf, attrs, columnNames, values, ifNotExists);
			}
		} catch (RecognitionException re) {
			reportError(re);
			recover(input, re);
		} finally {
		}
		return expr;
	}

	public final UpdateStatement.ParsedInsertJson jsonInsertStatement(CFName cf) throws RecognitionException {
		UpdateStatement.ParsedInsertJson expr = null;
		Json.Raw val = null;
		Attributes.Raw attrs = new Attributes.Raw();
		boolean ifNotExists = false;
		boolean defaultUnset = false;
		try {
			{
				pushFollow(Cql_Parser.FOLLOW_jsonValue_in_jsonInsertStatement2138);
				val = jsonValue();
				(state._fsp)--;
				int alt28 = 2;
				int LA28_0 = input.LA(1);
				if (LA28_0 == (Cql_Parser.K_DEFAULT)) {
					alt28 = 1;
				}
				switch (alt28) {
					case 1 :
						{
							match(input, Cql_Parser.K_DEFAULT, Cql_Parser.FOLLOW_K_DEFAULT_in_jsonInsertStatement2148);
							int alt27 = 2;
							int LA27_0 = input.LA(1);
							if (LA27_0 == (Cql_Parser.K_NULL)) {
								alt27 = 1;
							}else
								if (LA27_0 == (Cql_Parser.K_UNSET)) {
									alt27 = 2;
								}else {
									NoViableAltException nvae = new NoViableAltException("", 27, 0, input);
									throw nvae;
								}

							switch (alt27) {
								case 1 :
									{
										match(input, Cql_Parser.K_NULL, Cql_Parser.FOLLOW_K_NULL_in_jsonInsertStatement2152);
									}
									break;
								case 2 :
									{
										{
											defaultUnset = true;
											match(input, Cql_Parser.K_UNSET, Cql_Parser.FOLLOW_K_UNSET_in_jsonInsertStatement2160);
										}
									}
									break;
							}
						}
						break;
				}
				int alt29 = 2;
				int LA29_0 = input.LA(1);
				if (LA29_0 == (Cql_Parser.K_IF)) {
					alt29 = 1;
				}
				switch (alt29) {
					case 1 :
						{
							match(input, Cql_Parser.K_IF, Cql_Parser.FOLLOW_K_IF_in_jsonInsertStatement2176);
							match(input, Cql_Parser.K_NOT, Cql_Parser.FOLLOW_K_NOT_in_jsonInsertStatement2178);
							match(input, Cql_Parser.K_EXISTS, Cql_Parser.FOLLOW_K_EXISTS_in_jsonInsertStatement2180);
							ifNotExists = true;
						}
						break;
				}
				int alt30 = 2;
				int LA30_0 = input.LA(1);
				if (LA30_0 == (Cql_Parser.K_USING)) {
					alt30 = 1;
				}
				switch (alt30) {
					case 1 :
						{
							pushFollow(Cql_Parser.FOLLOW_usingClause_in_jsonInsertStatement2195);
							usingClause(attrs);
							(state._fsp)--;
						}
						break;
				}
				expr = new UpdateStatement.ParsedInsertJson(cf, attrs, val, defaultUnset, ifNotExists);
			}
		} catch (RecognitionException re) {
			reportError(re);
			recover(input, re);
		} finally {
		}
		return expr;
	}

	public final Json.Raw jsonValue() throws RecognitionException {
		Json.Raw value = null;
		Token s = null;
		ColumnIdentifier id = null;
		try {
			int alt31 = 3;
			switch (input.LA(1)) {
				case Cql_Parser.STRING_LITERAL :
					{
						alt31 = 1;
					}
					break;
				case 192 :
					{
						alt31 = 2;
					}
					break;
				case Cql_Parser.QMARK :
					{
						alt31 = 3;
					}
					break;
				default :
					NoViableAltException nvae = new NoViableAltException("", 31, 0, input);
					throw nvae;
			}
			switch (alt31) {
				case 1 :
					{
						s = ((Token) (match(input, Cql_Parser.STRING_LITERAL, Cql_Parser.FOLLOW_STRING_LITERAL_in_jsonValue2230)));
						value = new Json.Literal((s != null ? s.getText() : null));
					}
					break;
				case 2 :
					{
						match(input, 192, Cql_Parser.FOLLOW_192_in_jsonValue2240);
						pushFollow(Cql_Parser.FOLLOW_noncol_ident_in_jsonValue2244);
						id = noncol_ident();
						(state._fsp)--;
						value = newJsonBindVariables(id);
					}
					break;
				case 3 :
					{
						match(input, Cql_Parser.QMARK, Cql_Parser.FOLLOW_QMARK_in_jsonValue2258);
						value = newJsonBindVariables(null);
					}
					break;
			}
		} catch (RecognitionException re) {
			reportError(re);
			recover(input, re);
		} finally {
		}
		return value;
	}

	public final void usingClause(Attributes.Raw attrs) throws RecognitionException {
		try {
			{
				match(input, Cql_Parser.K_USING, Cql_Parser.FOLLOW_K_USING_in_usingClause2289);
				pushFollow(Cql_Parser.FOLLOW_usingClauseObjective_in_usingClause2291);
				usingClauseObjective(attrs);
				(state._fsp)--;
				loop32 : while (true) {
					int alt32 = 2;
					int LA32_0 = input.LA(1);
					if (LA32_0 == (Cql_Parser.K_AND)) {
						alt32 = 1;
					}
					switch (alt32) {
						case 1 :
							{
								match(input, Cql_Parser.K_AND, Cql_Parser.FOLLOW_K_AND_in_usingClause2296);
								pushFollow(Cql_Parser.FOLLOW_usingClauseObjective_in_usingClause2298);
								usingClauseObjective(attrs);
								(state._fsp)--;
							}
							break;
						default :
							break loop32;
					}
				} 
			}
		} catch (RecognitionException re) {
			reportError(re);
			recover(input, re);
		} finally {
		}
	}

	public final void usingClauseObjective(Attributes.Raw attrs) throws RecognitionException {
		Term.Raw ts = null;
		Term.Raw t = null;
		try {
			int alt33 = 2;
			int LA33_0 = input.LA(1);
			if (LA33_0 == (Cql_Parser.K_TIMESTAMP)) {
				alt33 = 1;
			}else
				if (LA33_0 == (Cql_Parser.K_TTL)) {
					alt33 = 2;
				}else {
					NoViableAltException nvae = new NoViableAltException("", 33, 0, input);
					throw nvae;
				}

			switch (alt33) {
				case 1 :
					{
						match(input, Cql_Parser.K_TIMESTAMP, Cql_Parser.FOLLOW_K_TIMESTAMP_in_usingClauseObjective2320);
						pushFollow(Cql_Parser.FOLLOW_intValue_in_usingClauseObjective2324);
						ts = intValue();
						(state._fsp)--;
						attrs.timestamp = ts;
					}
					break;
				case 2 :
					{
						match(input, Cql_Parser.K_TTL, Cql_Parser.FOLLOW_K_TTL_in_usingClauseObjective2334);
						pushFollow(Cql_Parser.FOLLOW_intValue_in_usingClauseObjective2338);
						t = intValue();
						(state._fsp)--;
						attrs.timeToLive = t;
					}
					break;
			}
		} catch (RecognitionException re) {
			reportError(re);
			recover(input, re);
		} finally {
		}
	}

	public final UpdateStatement.ParsedUpdate updateStatement() throws RecognitionException {
		UpdateStatement.ParsedUpdate expr = null;
		CFName cf = null;
		WhereClause.Builder wclause = null;
		List<Pair<ColumnDefinition.Raw, ColumnCondition.Raw>> conditions = null;
		Attributes.Raw attrs = new Attributes.Raw();
		List<Pair<ColumnDefinition.Raw, Operation.RawUpdate>> operations = new ArrayList<>();
		boolean ifExists = false;
		try {
			{
				match(input, Cql_Parser.K_UPDATE, Cql_Parser.FOLLOW_K_UPDATE_in_updateStatement2372);
				pushFollow(Cql_Parser.FOLLOW_columnFamilyName_in_updateStatement2376);
				cf = columnFamilyName();
				(state._fsp)--;
				int alt34 = 2;
				int LA34_0 = input.LA(1);
				if (LA34_0 == (Cql_Parser.K_USING)) {
					alt34 = 1;
				}
				switch (alt34) {
					case 1 :
						{
							pushFollow(Cql_Parser.FOLLOW_usingClause_in_updateStatement2386);
							usingClause(attrs);
							(state._fsp)--;
						}
						break;
				}
				match(input, Cql_Parser.K_SET, Cql_Parser.FOLLOW_K_SET_in_updateStatement2398);
				pushFollow(Cql_Parser.FOLLOW_columnOperation_in_updateStatement2400);
				columnOperation(operations);
				(state._fsp)--;
				loop35 : while (true) {
					int alt35 = 2;
					int LA35_0 = input.LA(1);
					if (LA35_0 == 188) {
						alt35 = 1;
					}
					switch (alt35) {
						case 1 :
							{
								match(input, 188, Cql_Parser.FOLLOW_188_in_updateStatement2404);
								pushFollow(Cql_Parser.FOLLOW_columnOperation_in_updateStatement2406);
								columnOperation(operations);
								(state._fsp)--;
							}
							break;
						default :
							break loop35;
					}
				} 
				match(input, Cql_Parser.K_WHERE, Cql_Parser.FOLLOW_K_WHERE_in_updateStatement2417);
				pushFollow(Cql_Parser.FOLLOW_whereClause_in_updateStatement2421);
				wclause = whereClause();
				(state._fsp)--;
				int alt37 = 2;
				int LA37_0 = input.LA(1);
				if (LA37_0 == (Cql_Parser.K_IF)) {
					alt37 = 1;
				}
				switch (alt37) {
					case 1 :
						{
							match(input, Cql_Parser.K_IF, Cql_Parser.FOLLOW_K_IF_in_updateStatement2431);
							int alt36 = 2;
							int LA36_0 = input.LA(1);
							if (LA36_0 == (Cql_Parser.K_EXISTS)) {
								int LA36_1 = input.LA(2);
								if ((((((LA36_1 == (Cql_Parser.EOF)) || (LA36_1 == (Cql_Parser.K_APPLY))) || (LA36_1 == (Cql_Parser.K_DELETE))) || (LA36_1 == (Cql_Parser.K_INSERT))) || (LA36_1 == (Cql_Parser.K_UPDATE))) || (LA36_1 == 193)) {
									alt36 = 1;
								}else
									if ((((LA36_1 == (Cql_Parser.K_IN)) || (LA36_1 == 183)) || (LA36_1 == 191)) || ((LA36_1 >= 194) && (LA36_1 <= 199))) {
										alt36 = 2;
									}else {
										int nvaeMark = input.mark();
										try {
											input.consume();
											NoViableAltException nvae = new NoViableAltException("", 36, 1, input);
											throw nvae;
										} finally {
											input.rewind(nvaeMark);
										}
									}

							}else
								if ((((((((((((((((((((((((((((((((((LA36_0 == (Cql_Parser.EMPTY_QUOTED_NAME)) || (LA36_0 == (Cql_Parser.IDENT))) || ((LA36_0 >= (Cql_Parser.K_AGGREGATE)) && (LA36_0 <= (Cql_Parser.K_ALL)))) || (LA36_0 == (Cql_Parser.K_AS))) || (LA36_0 == (Cql_Parser.K_ASCII))) || ((LA36_0 >= (Cql_Parser.K_BIGINT)) && (LA36_0 <= (Cql_Parser.K_BOOLEAN)))) || ((LA36_0 >= (Cql_Parser.K_CALLED)) && (LA36_0 <= (Cql_Parser.K_CLUSTERING)))) || ((LA36_0 >= (Cql_Parser.K_COMPACT)) && (LA36_0 <= (Cql_Parser.K_COUNTER)))) || ((LA36_0 >= (Cql_Parser.K_CUSTOM)) && (LA36_0 <= (Cql_Parser.K_DECIMAL)))) || ((LA36_0 >= (Cql_Parser.K_DISTINCT)) && (LA36_0 <= (Cql_Parser.K_DOUBLE)))) || (LA36_0 == (Cql_Parser.K_DURATION))) || ((LA36_0 >= (Cql_Parser.K_FILTERING)) && (LA36_0 <= (Cql_Parser.K_FLOAT)))) || (LA36_0 == (Cql_Parser.K_FROZEN))) || ((LA36_0 >= (Cql_Parser.K_FUNCTION)) && (LA36_0 <= (Cql_Parser.K_FUNCTIONS)))) || (LA36_0 == (Cql_Parser.K_GROUP))) || (LA36_0 == (Cql_Parser.K_INET))) || ((LA36_0 >= (Cql_Parser.K_INITCOND)) && (LA36_0 <= (Cql_Parser.K_INPUT)))) || (LA36_0 == (Cql_Parser.K_INT))) || ((LA36_0 >= (Cql_Parser.K_JSON)) && (LA36_0 <= (Cql_Parser.K_KEYS)))) || ((LA36_0 >= (Cql_Parser.K_KEYSPACES)) && (LA36_0 <= (Cql_Parser.K_LIKE)))) || ((LA36_0 >= (Cql_Parser.K_LIST)) && (LA36_0 <= (Cql_Parser.K_MAP)))) || (LA36_0 == (Cql_Parser.K_NOLOGIN))) || (LA36_0 == (Cql_Parser.K_NOSUPERUSER))) || (LA36_0 == (Cql_Parser.K_OPTIONS))) || ((LA36_0 >= (Cql_Parser.K_PARTITION)) && (LA36_0 <= (Cql_Parser.K_PERMISSIONS)))) || (LA36_0 == (Cql_Parser.K_RETURNS))) || ((LA36_0 >= (Cql_Parser.K_ROLE)) && (LA36_0 <= (Cql_Parser.K_ROLES)))) || ((LA36_0 >= (Cql_Parser.K_SFUNC)) && (LA36_0 <= (Cql_Parser.K_TINYINT)))) || (LA36_0 == (Cql_Parser.K_TRIGGER))) || ((LA36_0 >= (Cql_Parser.K_TTL)) && (LA36_0 <= (Cql_Parser.K_TYPE)))) || ((LA36_0 >= (Cql_Parser.K_USER)) && (LA36_0 <= (Cql_Parser.K_USERS)))) || ((LA36_0 >= (Cql_Parser.K_UUID)) && (LA36_0 <= (Cql_Parser.K_VARINT)))) || (LA36_0 == (Cql_Parser.K_WRITETIME))) || (LA36_0 == (Cql_Parser.QUOTED_NAME))) {
									alt36 = 2;
								}else {
									NoViableAltException nvae = new NoViableAltException("", 36, 0, input);
									throw nvae;
								}

							switch (alt36) {
								case 1 :
									{
										match(input, Cql_Parser.K_EXISTS, Cql_Parser.FOLLOW_K_EXISTS_in_updateStatement2435);
										ifExists = true;
									}
									break;
								case 2 :
									{
										pushFollow(Cql_Parser.FOLLOW_updateConditions_in_updateStatement2443);
										conditions = updateConditions();
										(state._fsp)--;
									}
									break;
							}
						}
						break;
				}
				expr = new UpdateStatement.ParsedUpdate(cf, attrs, operations, wclause.build(), (conditions == null ? Collections.<Pair<ColumnDefinition.Raw, ColumnCondition.Raw>>emptyList() : conditions), ifExists);
			}
		} catch (RecognitionException re) {
			reportError(re);
			recover(input, re);
		} finally {
		}
		return expr;
	}

	public final List<Pair<ColumnDefinition.Raw, ColumnCondition.Raw>> updateConditions() throws RecognitionException {
		List<Pair<ColumnDefinition.Raw, ColumnCondition.Raw>> conditions = null;
		conditions = new ArrayList<Pair<ColumnDefinition.Raw, ColumnCondition.Raw>>();
		try {
			{
				pushFollow(Cql_Parser.FOLLOW_columnCondition_in_updateConditions2485);
				columnCondition(conditions);
				(state._fsp)--;
				loop38 : while (true) {
					int alt38 = 2;
					int LA38_0 = input.LA(1);
					if (LA38_0 == (Cql_Parser.K_AND)) {
						alt38 = 1;
					}
					switch (alt38) {
						case 1 :
							{
								match(input, Cql_Parser.K_AND, Cql_Parser.FOLLOW_K_AND_in_updateConditions2490);
								pushFollow(Cql_Parser.FOLLOW_columnCondition_in_updateConditions2492);
								columnCondition(conditions);
								(state._fsp)--;
							}
							break;
						default :
							break loop38;
					}
				} 
			}
		} catch (RecognitionException re) {
			reportError(re);
			recover(input, re);
		} finally {
		}
		return conditions;
	}

	public final DeleteStatement.Parsed deleteStatement() throws RecognitionException {
		DeleteStatement.Parsed expr = null;
		List<Operation.RawDeletion> dels = null;
		CFName cf = null;
		WhereClause.Builder wclause = null;
		List<Pair<ColumnDefinition.Raw, ColumnCondition.Raw>> conditions = null;
		Attributes.Raw attrs = new Attributes.Raw();
		List<Operation.RawDeletion> columnDeletions = Collections.emptyList();
		boolean ifExists = false;
		try {
			{
				match(input, Cql_Parser.K_DELETE, Cql_Parser.FOLLOW_K_DELETE_in_deleteStatement2529);
				int alt39 = 2;
				int LA39_0 = input.LA(1);
				if ((((((((((((((((((((((((((((((((((LA39_0 == (Cql_Parser.EMPTY_QUOTED_NAME)) || (LA39_0 == (Cql_Parser.IDENT))) || ((LA39_0 >= (Cql_Parser.K_AGGREGATE)) && (LA39_0 <= (Cql_Parser.K_ALL)))) || (LA39_0 == (Cql_Parser.K_AS))) || (LA39_0 == (Cql_Parser.K_ASCII))) || ((LA39_0 >= (Cql_Parser.K_BIGINT)) && (LA39_0 <= (Cql_Parser.K_BOOLEAN)))) || ((LA39_0 >= (Cql_Parser.K_CALLED)) && (LA39_0 <= (Cql_Parser.K_CLUSTERING)))) || ((LA39_0 >= (Cql_Parser.K_COMPACT)) && (LA39_0 <= (Cql_Parser.K_COUNTER)))) || ((LA39_0 >= (Cql_Parser.K_CUSTOM)) && (LA39_0 <= (Cql_Parser.K_DECIMAL)))) || ((LA39_0 >= (Cql_Parser.K_DISTINCT)) && (LA39_0 <= (Cql_Parser.K_DOUBLE)))) || (LA39_0 == (Cql_Parser.K_DURATION))) || ((LA39_0 >= (Cql_Parser.K_EXISTS)) && (LA39_0 <= (Cql_Parser.K_FLOAT)))) || (LA39_0 == (Cql_Parser.K_FROZEN))) || ((LA39_0 >= (Cql_Parser.K_FUNCTION)) && (LA39_0 <= (Cql_Parser.K_FUNCTIONS)))) || (LA39_0 == (Cql_Parser.K_GROUP))) || (LA39_0 == (Cql_Parser.K_INET))) || ((LA39_0 >= (Cql_Parser.K_INITCOND)) && (LA39_0 <= (Cql_Parser.K_INPUT)))) || (LA39_0 == (Cql_Parser.K_INT))) || ((LA39_0 >= (Cql_Parser.K_JSON)) && (LA39_0 <= (Cql_Parser.K_KEYS)))) || ((LA39_0 >= (Cql_Parser.K_KEYSPACES)) && (LA39_0 <= (Cql_Parser.K_LIKE)))) || ((LA39_0 >= (Cql_Parser.K_LIST)) && (LA39_0 <= (Cql_Parser.K_MAP)))) || (LA39_0 == (Cql_Parser.K_NOLOGIN))) || (LA39_0 == (Cql_Parser.K_NOSUPERUSER))) || (LA39_0 == (Cql_Parser.K_OPTIONS))) || ((LA39_0 >= (Cql_Parser.K_PARTITION)) && (LA39_0 <= (Cql_Parser.K_PERMISSIONS)))) || (LA39_0 == (Cql_Parser.K_RETURNS))) || ((LA39_0 >= (Cql_Parser.K_ROLE)) && (LA39_0 <= (Cql_Parser.K_ROLES)))) || ((LA39_0 >= (Cql_Parser.K_SFUNC)) && (LA39_0 <= (Cql_Parser.K_TINYINT)))) || (LA39_0 == (Cql_Parser.K_TRIGGER))) || ((LA39_0 >= (Cql_Parser.K_TTL)) && (LA39_0 <= (Cql_Parser.K_TYPE)))) || ((LA39_0 >= (Cql_Parser.K_USER)) && (LA39_0 <= (Cql_Parser.K_USERS)))) || ((LA39_0 >= (Cql_Parser.K_UUID)) && (LA39_0 <= (Cql_Parser.K_VARINT)))) || (LA39_0 == (Cql_Parser.K_WRITETIME))) || (LA39_0 == (Cql_Parser.QUOTED_NAME))) {
					alt39 = 1;
				}
				switch (alt39) {
					case 1 :
						{
							pushFollow(Cql_Parser.FOLLOW_deleteSelection_in_deleteStatement2535);
							dels = deleteSelection();
							(state._fsp)--;
							columnDeletions = dels;
						}
						break;
				}
				match(input, Cql_Parser.K_FROM, Cql_Parser.FOLLOW_K_FROM_in_deleteStatement2548);
				pushFollow(Cql_Parser.FOLLOW_columnFamilyName_in_deleteStatement2552);
				cf = columnFamilyName();
				(state._fsp)--;
				int alt40 = 2;
				int LA40_0 = input.LA(1);
				if (LA40_0 == (Cql_Parser.K_USING)) {
					alt40 = 1;
				}
				switch (alt40) {
					case 1 :
						{
							pushFollow(Cql_Parser.FOLLOW_usingClauseDelete_in_deleteStatement2562);
							usingClauseDelete(attrs);
							(state._fsp)--;
						}
						break;
				}
				match(input, Cql_Parser.K_WHERE, Cql_Parser.FOLLOW_K_WHERE_in_deleteStatement2574);
				pushFollow(Cql_Parser.FOLLOW_whereClause_in_deleteStatement2578);
				wclause = whereClause();
				(state._fsp)--;
				int alt42 = 2;
				int LA42_0 = input.LA(1);
				if (LA42_0 == (Cql_Parser.K_IF)) {
					alt42 = 1;
				}
				switch (alt42) {
					case 1 :
						{
							match(input, Cql_Parser.K_IF, Cql_Parser.FOLLOW_K_IF_in_deleteStatement2588);
							int alt41 = 2;
							int LA41_0 = input.LA(1);
							if (LA41_0 == (Cql_Parser.K_EXISTS)) {
								int LA41_1 = input.LA(2);
								if ((((((LA41_1 == (Cql_Parser.EOF)) || (LA41_1 == (Cql_Parser.K_APPLY))) || (LA41_1 == (Cql_Parser.K_DELETE))) || (LA41_1 == (Cql_Parser.K_INSERT))) || (LA41_1 == (Cql_Parser.K_UPDATE))) || (LA41_1 == 193)) {
									alt41 = 1;
								}else
									if ((((LA41_1 == (Cql_Parser.K_IN)) || (LA41_1 == 183)) || (LA41_1 == 191)) || ((LA41_1 >= 194) && (LA41_1 <= 199))) {
										alt41 = 2;
									}else {
										int nvaeMark = input.mark();
										try {
											input.consume();
											NoViableAltException nvae = new NoViableAltException("", 41, 1, input);
											throw nvae;
										} finally {
											input.rewind(nvaeMark);
										}
									}

							}else
								if ((((((((((((((((((((((((((((((((((LA41_0 == (Cql_Parser.EMPTY_QUOTED_NAME)) || (LA41_0 == (Cql_Parser.IDENT))) || ((LA41_0 >= (Cql_Parser.K_AGGREGATE)) && (LA41_0 <= (Cql_Parser.K_ALL)))) || (LA41_0 == (Cql_Parser.K_AS))) || (LA41_0 == (Cql_Parser.K_ASCII))) || ((LA41_0 >= (Cql_Parser.K_BIGINT)) && (LA41_0 <= (Cql_Parser.K_BOOLEAN)))) || ((LA41_0 >= (Cql_Parser.K_CALLED)) && (LA41_0 <= (Cql_Parser.K_CLUSTERING)))) || ((LA41_0 >= (Cql_Parser.K_COMPACT)) && (LA41_0 <= (Cql_Parser.K_COUNTER)))) || ((LA41_0 >= (Cql_Parser.K_CUSTOM)) && (LA41_0 <= (Cql_Parser.K_DECIMAL)))) || ((LA41_0 >= (Cql_Parser.K_DISTINCT)) && (LA41_0 <= (Cql_Parser.K_DOUBLE)))) || (LA41_0 == (Cql_Parser.K_DURATION))) || ((LA41_0 >= (Cql_Parser.K_FILTERING)) && (LA41_0 <= (Cql_Parser.K_FLOAT)))) || (LA41_0 == (Cql_Parser.K_FROZEN))) || ((LA41_0 >= (Cql_Parser.K_FUNCTION)) && (LA41_0 <= (Cql_Parser.K_FUNCTIONS)))) || (LA41_0 == (Cql_Parser.K_GROUP))) || (LA41_0 == (Cql_Parser.K_INET))) || ((LA41_0 >= (Cql_Parser.K_INITCOND)) && (LA41_0 <= (Cql_Parser.K_INPUT)))) || (LA41_0 == (Cql_Parser.K_INT))) || ((LA41_0 >= (Cql_Parser.K_JSON)) && (LA41_0 <= (Cql_Parser.K_KEYS)))) || ((LA41_0 >= (Cql_Parser.K_KEYSPACES)) && (LA41_0 <= (Cql_Parser.K_LIKE)))) || ((LA41_0 >= (Cql_Parser.K_LIST)) && (LA41_0 <= (Cql_Parser.K_MAP)))) || (LA41_0 == (Cql_Parser.K_NOLOGIN))) || (LA41_0 == (Cql_Parser.K_NOSUPERUSER))) || (LA41_0 == (Cql_Parser.K_OPTIONS))) || ((LA41_0 >= (Cql_Parser.K_PARTITION)) && (LA41_0 <= (Cql_Parser.K_PERMISSIONS)))) || (LA41_0 == (Cql_Parser.K_RETURNS))) || ((LA41_0 >= (Cql_Parser.K_ROLE)) && (LA41_0 <= (Cql_Parser.K_ROLES)))) || ((LA41_0 >= (Cql_Parser.K_SFUNC)) && (LA41_0 <= (Cql_Parser.K_TINYINT)))) || (LA41_0 == (Cql_Parser.K_TRIGGER))) || ((LA41_0 >= (Cql_Parser.K_TTL)) && (LA41_0 <= (Cql_Parser.K_TYPE)))) || ((LA41_0 >= (Cql_Parser.K_USER)) && (LA41_0 <= (Cql_Parser.K_USERS)))) || ((LA41_0 >= (Cql_Parser.K_UUID)) && (LA41_0 <= (Cql_Parser.K_VARINT)))) || (LA41_0 == (Cql_Parser.K_WRITETIME))) || (LA41_0 == (Cql_Parser.QUOTED_NAME))) {
									alt41 = 2;
								}else {
									NoViableAltException nvae = new NoViableAltException("", 41, 0, input);
									throw nvae;
								}

							switch (alt41) {
								case 1 :
									{
										match(input, Cql_Parser.K_EXISTS, Cql_Parser.FOLLOW_K_EXISTS_in_deleteStatement2592);
										ifExists = true;
									}
									break;
								case 2 :
									{
										pushFollow(Cql_Parser.FOLLOW_updateConditions_in_deleteStatement2600);
										conditions = updateConditions();
										(state._fsp)--;
									}
									break;
							}
						}
						break;
				}
				expr = new DeleteStatement.Parsed(cf, attrs, columnDeletions, wclause.build(), (conditions == null ? Collections.<Pair<ColumnDefinition.Raw, ColumnCondition.Raw>>emptyList() : conditions), ifExists);
			}
		} catch (RecognitionException re) {
			reportError(re);
			recover(input, re);
		} finally {
		}
		return expr;
	}

	public final List<Operation.RawDeletion> deleteSelection() throws RecognitionException {
		List<Operation.RawDeletion> operations = null;
		Operation.RawDeletion t1 = null;
		Operation.RawDeletion tN = null;
		try {
			{
				operations = new ArrayList<Operation.RawDeletion>();
				pushFollow(Cql_Parser.FOLLOW_deleteOp_in_deleteSelection2647);
				t1 = deleteOp();
				(state._fsp)--;
				operations.add(t1);
				loop43 : while (true) {
					int alt43 = 2;
					int LA43_0 = input.LA(1);
					if (LA43_0 == 188) {
						alt43 = 1;
					}
					switch (alt43) {
						case 1 :
							{
								match(input, 188, Cql_Parser.FOLLOW_188_in_deleteSelection2662);
								pushFollow(Cql_Parser.FOLLOW_deleteOp_in_deleteSelection2666);
								tN = deleteOp();
								(state._fsp)--;
								operations.add(tN);
							}
							break;
						default :
							break loop43;
					}
				} 
			}
		} catch (RecognitionException re) {
			reportError(re);
			recover(input, re);
		} finally {
		}
		return operations;
	}

	public final Operation.RawDeletion deleteOp() throws RecognitionException {
		Operation.RawDeletion op = null;
		ColumnDefinition.Raw c = null;
		Term.Raw t = null;
		FieldIdentifier field = null;
		try {
			int alt44 = 3;
			alt44 = dfa44.predict(input);
			switch (alt44) {
				case 1 :
					{
						pushFollow(Cql_Parser.FOLLOW_cident_in_deleteOp2693);
						c = cident();
						(state._fsp)--;
						op = new Operation.ColumnDeletion(c);
					}
					break;
				case 2 :
					{
						pushFollow(Cql_Parser.FOLLOW_cident_in_deleteOp2720);
						c = cident();
						(state._fsp)--;
						match(input, 199, Cql_Parser.FOLLOW_199_in_deleteOp2722);
						pushFollow(Cql_Parser.FOLLOW_term_in_deleteOp2726);
						t = term();
						(state._fsp)--;
						match(input, 201, Cql_Parser.FOLLOW_201_in_deleteOp2728);
						op = new Operation.ElementDeletion(c, t);
					}
					break;
				case 3 :
					{
						pushFollow(Cql_Parser.FOLLOW_cident_in_deleteOp2740);
						c = cident();
						(state._fsp)--;
						match(input, 191, Cql_Parser.FOLLOW_191_in_deleteOp2742);
						pushFollow(Cql_Parser.FOLLOW_fident_in_deleteOp2746);
						field = fident();
						(state._fsp)--;
						op = new Operation.FieldDeletion(c, field);
					}
					break;
			}
		} catch (RecognitionException re) {
			reportError(re);
			recover(input, re);
		} finally {
		}
		return op;
	}

	public final void usingClauseDelete(Attributes.Raw attrs) throws RecognitionException {
		Term.Raw ts = null;
		try {
			{
				match(input, Cql_Parser.K_USING, Cql_Parser.FOLLOW_K_USING_in_usingClauseDelete2766);
				match(input, Cql_Parser.K_TIMESTAMP, Cql_Parser.FOLLOW_K_TIMESTAMP_in_usingClauseDelete2768);
				pushFollow(Cql_Parser.FOLLOW_intValue_in_usingClauseDelete2772);
				ts = intValue();
				(state._fsp)--;
				attrs.timestamp = ts;
			}
		} catch (RecognitionException re) {
			reportError(re);
			recover(input, re);
		} finally {
		}
	}

	public final BatchStatement.Parsed batchStatement() throws RecognitionException {
		BatchStatement.Parsed expr = null;
		ModificationStatement.Parsed s = null;
		BatchStatement.Type type = LOGGED;
		List<ModificationStatement.Parsed> statements = new ArrayList<ModificationStatement.Parsed>();
		Attributes.Raw attrs = new Attributes.Raw();
		try {
			{
				match(input, Cql_Parser.K_BEGIN, Cql_Parser.FOLLOW_K_BEGIN_in_batchStatement2806);
				int alt45 = 3;
				int LA45_0 = input.LA(1);
				if (LA45_0 == (Cql_Parser.K_UNLOGGED)) {
					alt45 = 1;
				}else
					if (LA45_0 == (Cql_Parser.K_COUNTER)) {
						alt45 = 2;
					}

				switch (alt45) {
					case 1 :
						{
							match(input, Cql_Parser.K_UNLOGGED, Cql_Parser.FOLLOW_K_UNLOGGED_in_batchStatement2816);
							type = UNLOGGED;
						}
						break;
					case 2 :
						{
							match(input, Cql_Parser.K_COUNTER, Cql_Parser.FOLLOW_K_COUNTER_in_batchStatement2822);
							type = COUNTER;
						}
						break;
				}
				match(input, Cql_Parser.K_BATCH, Cql_Parser.FOLLOW_K_BATCH_in_batchStatement2835);
				int alt46 = 2;
				int LA46_0 = input.LA(1);
				if (LA46_0 == (Cql_Parser.K_USING)) {
					alt46 = 1;
				}
				switch (alt46) {
					case 1 :
						{
							pushFollow(Cql_Parser.FOLLOW_usingClause_in_batchStatement2839);
							usingClause(attrs);
							(state._fsp)--;
						}
						break;
				}
				loop48 : while (true) {
					int alt48 = 2;
					int LA48_0 = input.LA(1);
					if (((LA48_0 == (Cql_Parser.K_DELETE)) || (LA48_0 == (Cql_Parser.K_INSERT))) || (LA48_0 == (Cql_Parser.K_UPDATE))) {
						alt48 = 1;
					}
					switch (alt48) {
						case 1 :
							{
								pushFollow(Cql_Parser.FOLLOW_batchStatementObjective_in_batchStatement2859);
								s = batchStatementObjective();
								(state._fsp)--;
								int alt47 = 2;
								int LA47_0 = input.LA(1);
								if (LA47_0 == 193) {
									alt47 = 1;
								}
								switch (alt47) {
									case 1 :
										{
											match(input, 193, Cql_Parser.FOLLOW_193_in_batchStatement2861);
										}
										break;
								}
								statements.add(s);
							}
							break;
						default :
							break loop48;
					}
				} 
				match(input, Cql_Parser.K_APPLY, Cql_Parser.FOLLOW_K_APPLY_in_batchStatement2875);
				match(input, Cql_Parser.K_BATCH, Cql_Parser.FOLLOW_K_BATCH_in_batchStatement2877);
				expr = new BatchStatement.Parsed(type, attrs, statements);
			}
		} catch (RecognitionException re) {
			reportError(re);
			recover(input, re);
		} finally {
		}
		return expr;
	}

	public final ModificationStatement.Parsed batchStatementObjective() throws RecognitionException {
		ModificationStatement.Parsed statement = null;
		ModificationStatement.Parsed i = null;
		UpdateStatement.ParsedUpdate u = null;
		DeleteStatement.Parsed d = null;
		try {
			int alt49 = 3;
			switch (input.LA(1)) {
				case Cql_Parser.K_INSERT :
					{
						alt49 = 1;
					}
					break;
				case Cql_Parser.K_UPDATE :
					{
						alt49 = 2;
					}
					break;
				case Cql_Parser.K_DELETE :
					{
						alt49 = 3;
					}
					break;
				default :
					NoViableAltException nvae = new NoViableAltException("", 49, 0, input);
					throw nvae;
			}
			switch (alt49) {
				case 1 :
					{
						pushFollow(Cql_Parser.FOLLOW_insertStatement_in_batchStatementObjective2908);
						i = insertStatement();
						(state._fsp)--;
						statement = i;
					}
					break;
				case 2 :
					{
						pushFollow(Cql_Parser.FOLLOW_updateStatement_in_batchStatementObjective2921);
						u = updateStatement();
						(state._fsp)--;
						statement = u;
					}
					break;
				case 3 :
					{
						pushFollow(Cql_Parser.FOLLOW_deleteStatement_in_batchStatementObjective2934);
						d = deleteStatement();
						(state._fsp)--;
						statement = d;
					}
					break;
			}
		} catch (RecognitionException re) {
			reportError(re);
			recover(input, re);
		} finally {
		}
		return statement;
	}

	public final CreateAggregateStatement createAggregateStatement() throws RecognitionException {
		CreateAggregateStatement expr = null;
		FunctionName fn = null;
		CQL3Type.Raw v = null;
		String sfunc = null;
		CQL3Type.Raw stype = null;
		String ffunc = null;
		Term.Raw ival = null;
		boolean orReplace = false;
		boolean ifNotExists = false;
		List<CQL3Type.Raw> argsTypes = new ArrayList<>();
		try {
			{
				match(input, Cql_Parser.K_CREATE, Cql_Parser.FOLLOW_K_CREATE_in_createAggregateStatement2967);
				int alt50 = 2;
				int LA50_0 = input.LA(1);
				if (LA50_0 == (Cql_Parser.K_OR)) {
					alt50 = 1;
				}
				switch (alt50) {
					case 1 :
						{
							match(input, Cql_Parser.K_OR, Cql_Parser.FOLLOW_K_OR_in_createAggregateStatement2970);
							match(input, Cql_Parser.K_REPLACE, Cql_Parser.FOLLOW_K_REPLACE_in_createAggregateStatement2972);
							orReplace = true;
						}
						break;
				}
				match(input, Cql_Parser.K_AGGREGATE, Cql_Parser.FOLLOW_K_AGGREGATE_in_createAggregateStatement2984);
				int alt51 = 2;
				int LA51_0 = input.LA(1);
				if (LA51_0 == (Cql_Parser.K_IF)) {
					alt51 = 1;
				}
				switch (alt51) {
					case 1 :
						{
							match(input, Cql_Parser.K_IF, Cql_Parser.FOLLOW_K_IF_in_createAggregateStatement2993);
							match(input, Cql_Parser.K_NOT, Cql_Parser.FOLLOW_K_NOT_in_createAggregateStatement2995);
							match(input, Cql_Parser.K_EXISTS, Cql_Parser.FOLLOW_K_EXISTS_in_createAggregateStatement2997);
							ifNotExists = true;
						}
						break;
				}
				pushFollow(Cql_Parser.FOLLOW_functionName_in_createAggregateStatement3011);
				fn = functionName();
				(state._fsp)--;
				match(input, 184, Cql_Parser.FOLLOW_184_in_createAggregateStatement3019);
				int alt53 = 2;
				int LA53_0 = input.LA(1);
				if ((((((((((((((((((((((((((((((((((LA53_0 == (Cql_Parser.IDENT)) || ((LA53_0 >= (Cql_Parser.K_AGGREGATE)) && (LA53_0 <= (Cql_Parser.K_ALL)))) || (LA53_0 == (Cql_Parser.K_AS))) || (LA53_0 == (Cql_Parser.K_ASCII))) || ((LA53_0 >= (Cql_Parser.K_BIGINT)) && (LA53_0 <= (Cql_Parser.K_BOOLEAN)))) || ((LA53_0 >= (Cql_Parser.K_CALLED)) && (LA53_0 <= (Cql_Parser.K_CLUSTERING)))) || ((LA53_0 >= (Cql_Parser.K_COMPACT)) && (LA53_0 <= (Cql_Parser.K_COUNTER)))) || ((LA53_0 >= (Cql_Parser.K_CUSTOM)) && (LA53_0 <= (Cql_Parser.K_DECIMAL)))) || ((LA53_0 >= (Cql_Parser.K_DISTINCT)) && (LA53_0 <= (Cql_Parser.K_DOUBLE)))) || (LA53_0 == (Cql_Parser.K_DURATION))) || ((LA53_0 >= (Cql_Parser.K_EXISTS)) && (LA53_0 <= (Cql_Parser.K_FLOAT)))) || (LA53_0 == (Cql_Parser.K_FROZEN))) || ((LA53_0 >= (Cql_Parser.K_FUNCTION)) && (LA53_0 <= (Cql_Parser.K_FUNCTIONS)))) || (LA53_0 == (Cql_Parser.K_GROUP))) || (LA53_0 == (Cql_Parser.K_INET))) || ((LA53_0 >= (Cql_Parser.K_INITCOND)) && (LA53_0 <= (Cql_Parser.K_INPUT)))) || (LA53_0 == (Cql_Parser.K_INT))) || ((LA53_0 >= (Cql_Parser.K_JSON)) && (LA53_0 <= (Cql_Parser.K_KEYS)))) || ((LA53_0 >= (Cql_Parser.K_KEYSPACES)) && (LA53_0 <= (Cql_Parser.K_LIKE)))) || ((LA53_0 >= (Cql_Parser.K_LIST)) && (LA53_0 <= (Cql_Parser.K_MAP)))) || (LA53_0 == (Cql_Parser.K_NOLOGIN))) || (LA53_0 == (Cql_Parser.K_NOSUPERUSER))) || (LA53_0 == (Cql_Parser.K_OPTIONS))) || ((LA53_0 >= (Cql_Parser.K_PARTITION)) && (LA53_0 <= (Cql_Parser.K_PERMISSIONS)))) || (LA53_0 == (Cql_Parser.K_RETURNS))) || ((LA53_0 >= (Cql_Parser.K_ROLE)) && (LA53_0 <= (Cql_Parser.K_ROLES)))) || ((LA53_0 >= (Cql_Parser.K_SET)) && (LA53_0 <= (Cql_Parser.K_TINYINT)))) || (LA53_0 == (Cql_Parser.K_TRIGGER))) || ((LA53_0 >= (Cql_Parser.K_TTL)) && (LA53_0 <= (Cql_Parser.K_TYPE)))) || ((LA53_0 >= (Cql_Parser.K_USER)) && (LA53_0 <= (Cql_Parser.K_USERS)))) || ((LA53_0 >= (Cql_Parser.K_UUID)) && (LA53_0 <= (Cql_Parser.K_VARINT)))) || (LA53_0 == (Cql_Parser.K_WRITETIME))) || (LA53_0 == (Cql_Parser.QUOTED_NAME))) || (LA53_0 == (Cql_Parser.STRING_LITERAL))) {
					alt53 = 1;
				}
				switch (alt53) {
					case 1 :
						{
							pushFollow(Cql_Parser.FOLLOW_comparatorType_in_createAggregateStatement3043);
							v = comparatorType();
							(state._fsp)--;
							argsTypes.add(v);
							loop52 : while (true) {
								int alt52 = 2;
								int LA52_0 = input.LA(1);
								if (LA52_0 == 188) {
									alt52 = 1;
								}
								switch (alt52) {
									case 1 :
										{
											match(input, 188, Cql_Parser.FOLLOW_188_in_createAggregateStatement3059);
											pushFollow(Cql_Parser.FOLLOW_comparatorType_in_createAggregateStatement3063);
											v = comparatorType();
											(state._fsp)--;
											argsTypes.add(v);
										}
										break;
									default :
										break loop52;
								}
							} 
						}
						break;
				}
				match(input, 185, Cql_Parser.FOLLOW_185_in_createAggregateStatement3087);
				match(input, Cql_Parser.K_SFUNC, Cql_Parser.FOLLOW_K_SFUNC_in_createAggregateStatement3095);
				pushFollow(Cql_Parser.FOLLOW_allowedFunctionName_in_createAggregateStatement3101);
				sfunc = allowedFunctionName();
				(state._fsp)--;
				match(input, Cql_Parser.K_STYPE, Cql_Parser.FOLLOW_K_STYPE_in_createAggregateStatement3109);
				pushFollow(Cql_Parser.FOLLOW_comparatorType_in_createAggregateStatement3115);
				stype = comparatorType();
				(state._fsp)--;
				int alt54 = 2;
				int LA54_0 = input.LA(1);
				if (LA54_0 == (Cql_Parser.K_FINALFUNC)) {
					alt54 = 1;
				}
				switch (alt54) {
					case 1 :
						{
							match(input, Cql_Parser.K_FINALFUNC, Cql_Parser.FOLLOW_K_FINALFUNC_in_createAggregateStatement3133);
							pushFollow(Cql_Parser.FOLLOW_allowedFunctionName_in_createAggregateStatement3139);
							ffunc = allowedFunctionName();
							(state._fsp)--;
						}
						break;
				}
				int alt55 = 2;
				int LA55_0 = input.LA(1);
				if (LA55_0 == (Cql_Parser.K_INITCOND)) {
					alt55 = 1;
				}
				switch (alt55) {
					case 1 :
						{
							match(input, Cql_Parser.K_INITCOND, Cql_Parser.FOLLOW_K_INITCOND_in_createAggregateStatement3166);
							pushFollow(Cql_Parser.FOLLOW_term_in_createAggregateStatement3172);
							ival = term();
							(state._fsp)--;
						}
						break;
				}
				expr = new CreateAggregateStatement(fn, argsTypes, sfunc, stype, ffunc, ival, orReplace, ifNotExists);
			}
		} catch (RecognitionException re) {
			reportError(re);
			recover(input, re);
		} finally {
		}
		return expr;
	}

	public final DropAggregateStatement dropAggregateStatement() throws RecognitionException {
		DropAggregateStatement expr = null;
		FunctionName fn = null;
		CQL3Type.Raw v = null;
		boolean ifExists = false;
		List<CQL3Type.Raw> argsTypes = new ArrayList<>();
		boolean argsPresent = false;
		try {
			{
				match(input, Cql_Parser.K_DROP, Cql_Parser.FOLLOW_K_DROP_in_dropAggregateStatement3219);
				match(input, Cql_Parser.K_AGGREGATE, Cql_Parser.FOLLOW_K_AGGREGATE_in_dropAggregateStatement3221);
				int alt56 = 2;
				int LA56_0 = input.LA(1);
				if (LA56_0 == (Cql_Parser.K_IF)) {
					alt56 = 1;
				}
				switch (alt56) {
					case 1 :
						{
							match(input, Cql_Parser.K_IF, Cql_Parser.FOLLOW_K_IF_in_dropAggregateStatement3230);
							match(input, Cql_Parser.K_EXISTS, Cql_Parser.FOLLOW_K_EXISTS_in_dropAggregateStatement3232);
							ifExists = true;
						}
						break;
				}
				pushFollow(Cql_Parser.FOLLOW_functionName_in_dropAggregateStatement3247);
				fn = functionName();
				(state._fsp)--;
				int alt59 = 2;
				int LA59_0 = input.LA(1);
				if (LA59_0 == 184) {
					alt59 = 1;
				}
				switch (alt59) {
					case 1 :
						{
							match(input, 184, Cql_Parser.FOLLOW_184_in_dropAggregateStatement3265);
							int alt58 = 2;
							int LA58_0 = input.LA(1);
							if ((((((((((((((((((((((((((((((((((LA58_0 == (Cql_Parser.IDENT)) || ((LA58_0 >= (Cql_Parser.K_AGGREGATE)) && (LA58_0 <= (Cql_Parser.K_ALL)))) || (LA58_0 == (Cql_Parser.K_AS))) || (LA58_0 == (Cql_Parser.K_ASCII))) || ((LA58_0 >= (Cql_Parser.K_BIGINT)) && (LA58_0 <= (Cql_Parser.K_BOOLEAN)))) || ((LA58_0 >= (Cql_Parser.K_CALLED)) && (LA58_0 <= (Cql_Parser.K_CLUSTERING)))) || ((LA58_0 >= (Cql_Parser.K_COMPACT)) && (LA58_0 <= (Cql_Parser.K_COUNTER)))) || ((LA58_0 >= (Cql_Parser.K_CUSTOM)) && (LA58_0 <= (Cql_Parser.K_DECIMAL)))) || ((LA58_0 >= (Cql_Parser.K_DISTINCT)) && (LA58_0 <= (Cql_Parser.K_DOUBLE)))) || (LA58_0 == (Cql_Parser.K_DURATION))) || ((LA58_0 >= (Cql_Parser.K_EXISTS)) && (LA58_0 <= (Cql_Parser.K_FLOAT)))) || (LA58_0 == (Cql_Parser.K_FROZEN))) || ((LA58_0 >= (Cql_Parser.K_FUNCTION)) && (LA58_0 <= (Cql_Parser.K_FUNCTIONS)))) || (LA58_0 == (Cql_Parser.K_GROUP))) || (LA58_0 == (Cql_Parser.K_INET))) || ((LA58_0 >= (Cql_Parser.K_INITCOND)) && (LA58_0 <= (Cql_Parser.K_INPUT)))) || (LA58_0 == (Cql_Parser.K_INT))) || ((LA58_0 >= (Cql_Parser.K_JSON)) && (LA58_0 <= (Cql_Parser.K_KEYS)))) || ((LA58_0 >= (Cql_Parser.K_KEYSPACES)) && (LA58_0 <= (Cql_Parser.K_LIKE)))) || ((LA58_0 >= (Cql_Parser.K_LIST)) && (LA58_0 <= (Cql_Parser.K_MAP)))) || (LA58_0 == (Cql_Parser.K_NOLOGIN))) || (LA58_0 == (Cql_Parser.K_NOSUPERUSER))) || (LA58_0 == (Cql_Parser.K_OPTIONS))) || ((LA58_0 >= (Cql_Parser.K_PARTITION)) && (LA58_0 <= (Cql_Parser.K_PERMISSIONS)))) || (LA58_0 == (Cql_Parser.K_RETURNS))) || ((LA58_0 >= (Cql_Parser.K_ROLE)) && (LA58_0 <= (Cql_Parser.K_ROLES)))) || ((LA58_0 >= (Cql_Parser.K_SET)) && (LA58_0 <= (Cql_Parser.K_TINYINT)))) || (LA58_0 == (Cql_Parser.K_TRIGGER))) || ((LA58_0 >= (Cql_Parser.K_TTL)) && (LA58_0 <= (Cql_Parser.K_TYPE)))) || ((LA58_0 >= (Cql_Parser.K_USER)) && (LA58_0 <= (Cql_Parser.K_USERS)))) || ((LA58_0 >= (Cql_Parser.K_UUID)) && (LA58_0 <= (Cql_Parser.K_VARINT)))) || (LA58_0 == (Cql_Parser.K_WRITETIME))) || (LA58_0 == (Cql_Parser.QUOTED_NAME))) || (LA58_0 == (Cql_Parser.STRING_LITERAL))) {
								alt58 = 1;
							}
							switch (alt58) {
								case 1 :
									{
										pushFollow(Cql_Parser.FOLLOW_comparatorType_in_dropAggregateStatement3293);
										v = comparatorType();
										(state._fsp)--;
										argsTypes.add(v);
										loop57 : while (true) {
											int alt57 = 2;
											int LA57_0 = input.LA(1);
											if (LA57_0 == 188) {
												alt57 = 1;
											}
											switch (alt57) {
												case 1 :
													{
														match(input, 188, Cql_Parser.FOLLOW_188_in_dropAggregateStatement3311);
														pushFollow(Cql_Parser.FOLLOW_comparatorType_in_dropAggregateStatement3315);
														v = comparatorType();
														(state._fsp)--;
														argsTypes.add(v);
													}
													break;
												default :
													break loop57;
											}
										} 
									}
									break;
							}
							match(input, 185, Cql_Parser.FOLLOW_185_in_dropAggregateStatement3343);
							argsPresent = true;
						}
						break;
				}
				expr = new DropAggregateStatement(fn, argsTypes, argsPresent, ifExists);
			}
		} catch (RecognitionException re) {
			reportError(re);
			recover(input, re);
		} finally {
		}
		return expr;
	}

	public final CreateFunctionStatement createFunctionStatement() throws RecognitionException {
		CreateFunctionStatement expr = null;
		Token language = null;
		Token body = null;
		FunctionName fn = null;
		ColumnIdentifier k = null;
		CQL3Type.Raw v = null;
		CQL3Type.Raw rt = null;
		boolean orReplace = false;
		boolean ifNotExists = false;
		List<ColumnIdentifier> argsNames = new ArrayList<>();
		List<CQL3Type.Raw> argsTypes = new ArrayList<>();
		boolean calledOnNullInput = false;
		try {
			{
				match(input, Cql_Parser.K_CREATE, Cql_Parser.FOLLOW_K_CREATE_in_createFunctionStatement3400);
				int alt60 = 2;
				int LA60_0 = input.LA(1);
				if (LA60_0 == (Cql_Parser.K_OR)) {
					alt60 = 1;
				}
				switch (alt60) {
					case 1 :
						{
							match(input, Cql_Parser.K_OR, Cql_Parser.FOLLOW_K_OR_in_createFunctionStatement3403);
							match(input, Cql_Parser.K_REPLACE, Cql_Parser.FOLLOW_K_REPLACE_in_createFunctionStatement3405);
							orReplace = true;
						}
						break;
				}
				match(input, Cql_Parser.K_FUNCTION, Cql_Parser.FOLLOW_K_FUNCTION_in_createFunctionStatement3417);
				int alt61 = 2;
				int LA61_0 = input.LA(1);
				if (LA61_0 == (Cql_Parser.K_IF)) {
					alt61 = 1;
				}
				switch (alt61) {
					case 1 :
						{
							match(input, Cql_Parser.K_IF, Cql_Parser.FOLLOW_K_IF_in_createFunctionStatement3426);
							match(input, Cql_Parser.K_NOT, Cql_Parser.FOLLOW_K_NOT_in_createFunctionStatement3428);
							match(input, Cql_Parser.K_EXISTS, Cql_Parser.FOLLOW_K_EXISTS_in_createFunctionStatement3430);
							ifNotExists = true;
						}
						break;
				}
				pushFollow(Cql_Parser.FOLLOW_functionName_in_createFunctionStatement3444);
				fn = functionName();
				(state._fsp)--;
				match(input, 184, Cql_Parser.FOLLOW_184_in_createFunctionStatement3452);
				int alt63 = 2;
				int LA63_0 = input.LA(1);
				if (((((((((((((((((((((((((((((((((LA63_0 == (Cql_Parser.IDENT)) || ((LA63_0 >= (Cql_Parser.K_AGGREGATE)) && (LA63_0 <= (Cql_Parser.K_ALL)))) || (LA63_0 == (Cql_Parser.K_AS))) || (LA63_0 == (Cql_Parser.K_ASCII))) || ((LA63_0 >= (Cql_Parser.K_BIGINT)) && (LA63_0 <= (Cql_Parser.K_BOOLEAN)))) || ((LA63_0 >= (Cql_Parser.K_CALLED)) && (LA63_0 <= (Cql_Parser.K_CLUSTERING)))) || ((LA63_0 >= (Cql_Parser.K_COMPACT)) && (LA63_0 <= (Cql_Parser.K_COUNTER)))) || ((LA63_0 >= (Cql_Parser.K_CUSTOM)) && (LA63_0 <= (Cql_Parser.K_DECIMAL)))) || ((LA63_0 >= (Cql_Parser.K_DISTINCT)) && (LA63_0 <= (Cql_Parser.K_DOUBLE)))) || (LA63_0 == (Cql_Parser.K_DURATION))) || ((LA63_0 >= (Cql_Parser.K_EXISTS)) && (LA63_0 <= (Cql_Parser.K_FLOAT)))) || (LA63_0 == (Cql_Parser.K_FROZEN))) || ((LA63_0 >= (Cql_Parser.K_FUNCTION)) && (LA63_0 <= (Cql_Parser.K_FUNCTIONS)))) || (LA63_0 == (Cql_Parser.K_GROUP))) || (LA63_0 == (Cql_Parser.K_INET))) || ((LA63_0 >= (Cql_Parser.K_INITCOND)) && (LA63_0 <= (Cql_Parser.K_INPUT)))) || (LA63_0 == (Cql_Parser.K_INT))) || ((LA63_0 >= (Cql_Parser.K_JSON)) && (LA63_0 <= (Cql_Parser.K_KEYS)))) || ((LA63_0 >= (Cql_Parser.K_KEYSPACES)) && (LA63_0 <= (Cql_Parser.K_LIKE)))) || ((LA63_0 >= (Cql_Parser.K_LIST)) && (LA63_0 <= (Cql_Parser.K_MAP)))) || (LA63_0 == (Cql_Parser.K_NOLOGIN))) || (LA63_0 == (Cql_Parser.K_NOSUPERUSER))) || (LA63_0 == (Cql_Parser.K_OPTIONS))) || ((LA63_0 >= (Cql_Parser.K_PARTITION)) && (LA63_0 <= (Cql_Parser.K_PERMISSIONS)))) || (LA63_0 == (Cql_Parser.K_RETURNS))) || ((LA63_0 >= (Cql_Parser.K_ROLE)) && (LA63_0 <= (Cql_Parser.K_ROLES)))) || ((LA63_0 >= (Cql_Parser.K_SFUNC)) && (LA63_0 <= (Cql_Parser.K_TINYINT)))) || (LA63_0 == (Cql_Parser.K_TRIGGER))) || ((LA63_0 >= (Cql_Parser.K_TTL)) && (LA63_0 <= (Cql_Parser.K_TYPE)))) || ((LA63_0 >= (Cql_Parser.K_USER)) && (LA63_0 <= (Cql_Parser.K_USERS)))) || ((LA63_0 >= (Cql_Parser.K_UUID)) && (LA63_0 <= (Cql_Parser.K_VARINT)))) || (LA63_0 == (Cql_Parser.K_WRITETIME))) || (LA63_0 == (Cql_Parser.QUOTED_NAME))) {
					alt63 = 1;
				}
				switch (alt63) {
					case 1 :
						{
							pushFollow(Cql_Parser.FOLLOW_noncol_ident_in_createFunctionStatement3476);
							k = noncol_ident();
							(state._fsp)--;
							pushFollow(Cql_Parser.FOLLOW_comparatorType_in_createFunctionStatement3480);
							v = comparatorType();
							(state._fsp)--;
							argsNames.add(k);
							argsTypes.add(v);
							loop62 : while (true) {
								int alt62 = 2;
								int LA62_0 = input.LA(1);
								if (LA62_0 == 188) {
									alt62 = 1;
								}
								switch (alt62) {
									case 1 :
										{
											match(input, 188, Cql_Parser.FOLLOW_188_in_createFunctionStatement3496);
											pushFollow(Cql_Parser.FOLLOW_noncol_ident_in_createFunctionStatement3500);
											k = noncol_ident();
											(state._fsp)--;
											pushFollow(Cql_Parser.FOLLOW_comparatorType_in_createFunctionStatement3504);
											v = comparatorType();
											(state._fsp)--;
											argsNames.add(k);
											argsTypes.add(v);
										}
										break;
									default :
										break loop62;
								}
							} 
						}
						break;
				}
				match(input, 185, Cql_Parser.FOLLOW_185_in_createFunctionStatement3528);
				int alt64 = 2;
				int LA64_0 = input.LA(1);
				if (LA64_0 == (Cql_Parser.K_RETURNS)) {
					alt64 = 1;
				}else
					if (LA64_0 == (Cql_Parser.K_CALLED)) {
						alt64 = 2;
					}else {
						NoViableAltException nvae = new NoViableAltException("", 64, 0, input);
						throw nvae;
					}

				switch (alt64) {
					case 1 :
						{
							{
								match(input, Cql_Parser.K_RETURNS, Cql_Parser.FOLLOW_K_RETURNS_in_createFunctionStatement3539);
								match(input, Cql_Parser.K_NULL, Cql_Parser.FOLLOW_K_NULL_in_createFunctionStatement3541);
							}
						}
						break;
					case 2 :
						{
							{
								match(input, Cql_Parser.K_CALLED, Cql_Parser.FOLLOW_K_CALLED_in_createFunctionStatement3547);
								calledOnNullInput = true;
							}
						}
						break;
				}
				match(input, Cql_Parser.K_ON, Cql_Parser.FOLLOW_K_ON_in_createFunctionStatement3553);
				match(input, Cql_Parser.K_NULL, Cql_Parser.FOLLOW_K_NULL_in_createFunctionStatement3555);
				match(input, Cql_Parser.K_INPUT, Cql_Parser.FOLLOW_K_INPUT_in_createFunctionStatement3557);
				match(input, Cql_Parser.K_RETURNS, Cql_Parser.FOLLOW_K_RETURNS_in_createFunctionStatement3565);
				pushFollow(Cql_Parser.FOLLOW_comparatorType_in_createFunctionStatement3571);
				rt = comparatorType();
				(state._fsp)--;
				match(input, Cql_Parser.K_LANGUAGE, Cql_Parser.FOLLOW_K_LANGUAGE_in_createFunctionStatement3579);
				language = ((Token) (match(input, Cql_Parser.IDENT, Cql_Parser.FOLLOW_IDENT_in_createFunctionStatement3585)));
				match(input, Cql_Parser.K_AS, Cql_Parser.FOLLOW_K_AS_in_createFunctionStatement3593);
				body = ((Token) (match(input, Cql_Parser.STRING_LITERAL, Cql_Parser.FOLLOW_STRING_LITERAL_in_createFunctionStatement3599)));
				expr = new CreateFunctionStatement(fn, (language != null ? language.getText() : null).toLowerCase(), (body != null ? body.getText() : null), argsNames, argsTypes, rt, calledOnNullInput, orReplace, ifNotExists);
			}
		} catch (RecognitionException re) {
			reportError(re);
			recover(input, re);
		} finally {
		}
		return expr;
	}

	public final DropFunctionStatement dropFunctionStatement() throws RecognitionException {
		DropFunctionStatement expr = null;
		FunctionName fn = null;
		CQL3Type.Raw v = null;
		boolean ifExists = false;
		List<CQL3Type.Raw> argsTypes = new ArrayList<>();
		boolean argsPresent = false;
		try {
			{
				match(input, Cql_Parser.K_DROP, Cql_Parser.FOLLOW_K_DROP_in_dropFunctionStatement3637);
				match(input, Cql_Parser.K_FUNCTION, Cql_Parser.FOLLOW_K_FUNCTION_in_dropFunctionStatement3639);
				int alt65 = 2;
				int LA65_0 = input.LA(1);
				if (LA65_0 == (Cql_Parser.K_IF)) {
					alt65 = 1;
				}
				switch (alt65) {
					case 1 :
						{
							match(input, Cql_Parser.K_IF, Cql_Parser.FOLLOW_K_IF_in_dropFunctionStatement3648);
							match(input, Cql_Parser.K_EXISTS, Cql_Parser.FOLLOW_K_EXISTS_in_dropFunctionStatement3650);
							ifExists = true;
						}
						break;
				}
				pushFollow(Cql_Parser.FOLLOW_functionName_in_dropFunctionStatement3665);
				fn = functionName();
				(state._fsp)--;
				int alt68 = 2;
				int LA68_0 = input.LA(1);
				if (LA68_0 == 184) {
					alt68 = 1;
				}
				switch (alt68) {
					case 1 :
						{
							match(input, 184, Cql_Parser.FOLLOW_184_in_dropFunctionStatement3683);
							int alt67 = 2;
							int LA67_0 = input.LA(1);
							if ((((((((((((((((((((((((((((((((((LA67_0 == (Cql_Parser.IDENT)) || ((LA67_0 >= (Cql_Parser.K_AGGREGATE)) && (LA67_0 <= (Cql_Parser.K_ALL)))) || (LA67_0 == (Cql_Parser.K_AS))) || (LA67_0 == (Cql_Parser.K_ASCII))) || ((LA67_0 >= (Cql_Parser.K_BIGINT)) && (LA67_0 <= (Cql_Parser.K_BOOLEAN)))) || ((LA67_0 >= (Cql_Parser.K_CALLED)) && (LA67_0 <= (Cql_Parser.K_CLUSTERING)))) || ((LA67_0 >= (Cql_Parser.K_COMPACT)) && (LA67_0 <= (Cql_Parser.K_COUNTER)))) || ((LA67_0 >= (Cql_Parser.K_CUSTOM)) && (LA67_0 <= (Cql_Parser.K_DECIMAL)))) || ((LA67_0 >= (Cql_Parser.K_DISTINCT)) && (LA67_0 <= (Cql_Parser.K_DOUBLE)))) || (LA67_0 == (Cql_Parser.K_DURATION))) || ((LA67_0 >= (Cql_Parser.K_EXISTS)) && (LA67_0 <= (Cql_Parser.K_FLOAT)))) || (LA67_0 == (Cql_Parser.K_FROZEN))) || ((LA67_0 >= (Cql_Parser.K_FUNCTION)) && (LA67_0 <= (Cql_Parser.K_FUNCTIONS)))) || (LA67_0 == (Cql_Parser.K_GROUP))) || (LA67_0 == (Cql_Parser.K_INET))) || ((LA67_0 >= (Cql_Parser.K_INITCOND)) && (LA67_0 <= (Cql_Parser.K_INPUT)))) || (LA67_0 == (Cql_Parser.K_INT))) || ((LA67_0 >= (Cql_Parser.K_JSON)) && (LA67_0 <= (Cql_Parser.K_KEYS)))) || ((LA67_0 >= (Cql_Parser.K_KEYSPACES)) && (LA67_0 <= (Cql_Parser.K_LIKE)))) || ((LA67_0 >= (Cql_Parser.K_LIST)) && (LA67_0 <= (Cql_Parser.K_MAP)))) || (LA67_0 == (Cql_Parser.K_NOLOGIN))) || (LA67_0 == (Cql_Parser.K_NOSUPERUSER))) || (LA67_0 == (Cql_Parser.K_OPTIONS))) || ((LA67_0 >= (Cql_Parser.K_PARTITION)) && (LA67_0 <= (Cql_Parser.K_PERMISSIONS)))) || (LA67_0 == (Cql_Parser.K_RETURNS))) || ((LA67_0 >= (Cql_Parser.K_ROLE)) && (LA67_0 <= (Cql_Parser.K_ROLES)))) || ((LA67_0 >= (Cql_Parser.K_SET)) && (LA67_0 <= (Cql_Parser.K_TINYINT)))) || (LA67_0 == (Cql_Parser.K_TRIGGER))) || ((LA67_0 >= (Cql_Parser.K_TTL)) && (LA67_0 <= (Cql_Parser.K_TYPE)))) || ((LA67_0 >= (Cql_Parser.K_USER)) && (LA67_0 <= (Cql_Parser.K_USERS)))) || ((LA67_0 >= (Cql_Parser.K_UUID)) && (LA67_0 <= (Cql_Parser.K_VARINT)))) || (LA67_0 == (Cql_Parser.K_WRITETIME))) || (LA67_0 == (Cql_Parser.QUOTED_NAME))) || (LA67_0 == (Cql_Parser.STRING_LITERAL))) {
								alt67 = 1;
							}
							switch (alt67) {
								case 1 :
									{
										pushFollow(Cql_Parser.FOLLOW_comparatorType_in_dropFunctionStatement3711);
										v = comparatorType();
										(state._fsp)--;
										argsTypes.add(v);
										loop66 : while (true) {
											int alt66 = 2;
											int LA66_0 = input.LA(1);
											if (LA66_0 == 188) {
												alt66 = 1;
											}
											switch (alt66) {
												case 1 :
													{
														match(input, 188, Cql_Parser.FOLLOW_188_in_dropFunctionStatement3729);
														pushFollow(Cql_Parser.FOLLOW_comparatorType_in_dropFunctionStatement3733);
														v = comparatorType();
														(state._fsp)--;
														argsTypes.add(v);
													}
													break;
												default :
													break loop66;
											}
										} 
									}
									break;
							}
							match(input, 185, Cql_Parser.FOLLOW_185_in_dropFunctionStatement3761);
							argsPresent = true;
						}
						break;
				}
				expr = new DropFunctionStatement(fn, argsTypes, argsPresent, ifExists);
			}
		} catch (RecognitionException re) {
			reportError(re);
			recover(input, re);
		} finally {
		}
		return expr;
	}

	public final CreateKeyspaceStatement createKeyspaceStatement() throws RecognitionException {
		CreateKeyspaceStatement expr = null;
		String ks = null;
		KeyspaceAttributes attrs = new KeyspaceAttributes();
		boolean ifNotExists = false;
		try {
			{
				match(input, Cql_Parser.K_CREATE, Cql_Parser.FOLLOW_K_CREATE_in_createKeyspaceStatement3820);
				match(input, Cql_Parser.K_KEYSPACE, Cql_Parser.FOLLOW_K_KEYSPACE_in_createKeyspaceStatement3822);
				int alt69 = 2;
				int LA69_0 = input.LA(1);
				if (LA69_0 == (Cql_Parser.K_IF)) {
					alt69 = 1;
				}
				switch (alt69) {
					case 1 :
						{
							match(input, Cql_Parser.K_IF, Cql_Parser.FOLLOW_K_IF_in_createKeyspaceStatement3825);
							match(input, Cql_Parser.K_NOT, Cql_Parser.FOLLOW_K_NOT_in_createKeyspaceStatement3827);
							match(input, Cql_Parser.K_EXISTS, Cql_Parser.FOLLOW_K_EXISTS_in_createKeyspaceStatement3829);
							ifNotExists = true;
						}
						break;
				}
				pushFollow(Cql_Parser.FOLLOW_keyspaceName_in_createKeyspaceStatement3838);
				ks = keyspaceName();
				(state._fsp)--;
				match(input, Cql_Parser.K_WITH, Cql_Parser.FOLLOW_K_WITH_in_createKeyspaceStatement3846);
				pushFollow(Cql_Parser.FOLLOW_properties_in_createKeyspaceStatement3848);
				properties(attrs);
				(state._fsp)--;
				expr = new CreateKeyspaceStatement(ks, attrs, ifNotExists);
			}
		} catch (RecognitionException re) {
			reportError(re);
			recover(input, re);
		} finally {
		}
		return expr;
	}

	public final CreateTableStatement.RawStatement createTableStatement() throws RecognitionException {
		CreateTableStatement.RawStatement expr = null;
		CFName cf = null;
		boolean ifNotExists = false;
		try {
			{
				match(input, Cql_Parser.K_CREATE, Cql_Parser.FOLLOW_K_CREATE_in_createTableStatement3883);
				match(input, Cql_Parser.K_COLUMNFAMILY, Cql_Parser.FOLLOW_K_COLUMNFAMILY_in_createTableStatement3885);
				int alt70 = 2;
				int LA70_0 = input.LA(1);
				if (LA70_0 == (Cql_Parser.K_IF)) {
					alt70 = 1;
				}
				switch (alt70) {
					case 1 :
						{
							match(input, Cql_Parser.K_IF, Cql_Parser.FOLLOW_K_IF_in_createTableStatement3888);
							match(input, Cql_Parser.K_NOT, Cql_Parser.FOLLOW_K_NOT_in_createTableStatement3890);
							match(input, Cql_Parser.K_EXISTS, Cql_Parser.FOLLOW_K_EXISTS_in_createTableStatement3892);
							ifNotExists = true;
						}
						break;
				}
				pushFollow(Cql_Parser.FOLLOW_columnFamilyName_in_createTableStatement3907);
				cf = columnFamilyName();
				(state._fsp)--;
				expr = new CreateTableStatement.RawStatement(cf, ifNotExists);
				pushFollow(Cql_Parser.FOLLOW_cfamDefinition_in_createTableStatement3917);
				cfamDefinition(expr);
				(state._fsp)--;
			}
		} catch (RecognitionException re) {
			reportError(re);
			recover(input, re);
		} finally {
		}
		return expr;
	}

	public final void cfamDefinition(CreateTableStatement.RawStatement expr) throws RecognitionException {
		try {
			{
				match(input, 184, Cql_Parser.FOLLOW_184_in_cfamDefinition3936);
				pushFollow(Cql_Parser.FOLLOW_cfamColumns_in_cfamDefinition3938);
				cfamColumns(expr);
				(state._fsp)--;
				loop72 : while (true) {
					int alt72 = 2;
					int LA72_0 = input.LA(1);
					if (LA72_0 == 188) {
						alt72 = 1;
					}
					switch (alt72) {
						case 1 :
							{
								match(input, 188, Cql_Parser.FOLLOW_188_in_cfamDefinition3943);
								int alt71 = 2;
								int LA71_0 = input.LA(1);
								if (((((((((((((((((((((((((((((((((LA71_0 == (Cql_Parser.IDENT)) || ((LA71_0 >= (Cql_Parser.K_AGGREGATE)) && (LA71_0 <= (Cql_Parser.K_ALL)))) || (LA71_0 == (Cql_Parser.K_AS))) || (LA71_0 == (Cql_Parser.K_ASCII))) || ((LA71_0 >= (Cql_Parser.K_BIGINT)) && (LA71_0 <= (Cql_Parser.K_BOOLEAN)))) || ((LA71_0 >= (Cql_Parser.K_CALLED)) && (LA71_0 <= (Cql_Parser.K_CLUSTERING)))) || ((LA71_0 >= (Cql_Parser.K_COMPACT)) && (LA71_0 <= (Cql_Parser.K_COUNTER)))) || ((LA71_0 >= (Cql_Parser.K_CUSTOM)) && (LA71_0 <= (Cql_Parser.K_DECIMAL)))) || ((LA71_0 >= (Cql_Parser.K_DISTINCT)) && (LA71_0 <= (Cql_Parser.K_DOUBLE)))) || (LA71_0 == (Cql_Parser.K_DURATION))) || ((LA71_0 >= (Cql_Parser.K_EXISTS)) && (LA71_0 <= (Cql_Parser.K_FLOAT)))) || (LA71_0 == (Cql_Parser.K_FROZEN))) || ((LA71_0 >= (Cql_Parser.K_FUNCTION)) && (LA71_0 <= (Cql_Parser.K_FUNCTIONS)))) || (LA71_0 == (Cql_Parser.K_GROUP))) || (LA71_0 == (Cql_Parser.K_INET))) || ((LA71_0 >= (Cql_Parser.K_INITCOND)) && (LA71_0 <= (Cql_Parser.K_INPUT)))) || (LA71_0 == (Cql_Parser.K_INT))) || ((LA71_0 >= (Cql_Parser.K_JSON)) && (LA71_0 <= (Cql_Parser.K_KEYS)))) || ((LA71_0 >= (Cql_Parser.K_KEYSPACES)) && (LA71_0 <= (Cql_Parser.K_LIKE)))) || ((LA71_0 >= (Cql_Parser.K_LIST)) && (LA71_0 <= (Cql_Parser.K_MAP)))) || (LA71_0 == (Cql_Parser.K_NOLOGIN))) || (LA71_0 == (Cql_Parser.K_NOSUPERUSER))) || (LA71_0 == (Cql_Parser.K_OPTIONS))) || ((LA71_0 >= (Cql_Parser.K_PARTITION)) && (LA71_0 <= (Cql_Parser.K_PRIMARY)))) || (LA71_0 == (Cql_Parser.K_RETURNS))) || ((LA71_0 >= (Cql_Parser.K_ROLE)) && (LA71_0 <= (Cql_Parser.K_ROLES)))) || ((LA71_0 >= (Cql_Parser.K_SFUNC)) && (LA71_0 <= (Cql_Parser.K_TINYINT)))) || (LA71_0 == (Cql_Parser.K_TRIGGER))) || ((LA71_0 >= (Cql_Parser.K_TTL)) && (LA71_0 <= (Cql_Parser.K_TYPE)))) || ((LA71_0 >= (Cql_Parser.K_USER)) && (LA71_0 <= (Cql_Parser.K_USERS)))) || ((LA71_0 >= (Cql_Parser.K_UUID)) && (LA71_0 <= (Cql_Parser.K_VARINT)))) || (LA71_0 == (Cql_Parser.K_WRITETIME))) || (LA71_0 == (Cql_Parser.QUOTED_NAME))) {
									alt71 = 1;
								}
								switch (alt71) {
									case 1 :
										{
											pushFollow(Cql_Parser.FOLLOW_cfamColumns_in_cfamDefinition3945);
											cfamColumns(expr);
											(state._fsp)--;
										}
										break;
								}
							}
							break;
						default :
							break loop72;
					}
				} 
				match(input, 185, Cql_Parser.FOLLOW_185_in_cfamDefinition3952);
				int alt74 = 2;
				int LA74_0 = input.LA(1);
				if (LA74_0 == (Cql_Parser.K_WITH)) {
					alt74 = 1;
				}
				switch (alt74) {
					case 1 :
						{
							match(input, Cql_Parser.K_WITH, Cql_Parser.FOLLOW_K_WITH_in_cfamDefinition3962);
							pushFollow(Cql_Parser.FOLLOW_cfamProperty_in_cfamDefinition3964);
							cfamProperty(expr.properties);
							(state._fsp)--;
							loop73 : while (true) {
								int alt73 = 2;
								int LA73_0 = input.LA(1);
								if (LA73_0 == (Cql_Parser.K_AND)) {
									alt73 = 1;
								}
								switch (alt73) {
									case 1 :
										{
											match(input, Cql_Parser.K_AND, Cql_Parser.FOLLOW_K_AND_in_cfamDefinition3969);
											pushFollow(Cql_Parser.FOLLOW_cfamProperty_in_cfamDefinition3971);
											cfamProperty(expr.properties);
											(state._fsp)--;
										}
										break;
									default :
										break loop73;
								}
							} 
						}
						break;
				}
			}
		} catch (RecognitionException re) {
			reportError(re);
			recover(input, re);
		} finally {
		}
	}

	public final void cfamColumns(CreateTableStatement.RawStatement expr) throws RecognitionException {
		ColumnIdentifier k = null;
		CQL3Type.Raw v = null;
		ColumnIdentifier c = null;
		try {
			int alt78 = 2;
			int LA78_0 = input.LA(1);
			if (((((((((((((((((((((((((((((((((LA78_0 == (Cql_Parser.IDENT)) || ((LA78_0 >= (Cql_Parser.K_AGGREGATE)) && (LA78_0 <= (Cql_Parser.K_ALL)))) || (LA78_0 == (Cql_Parser.K_AS))) || (LA78_0 == (Cql_Parser.K_ASCII))) || ((LA78_0 >= (Cql_Parser.K_BIGINT)) && (LA78_0 <= (Cql_Parser.K_BOOLEAN)))) || ((LA78_0 >= (Cql_Parser.K_CALLED)) && (LA78_0 <= (Cql_Parser.K_CLUSTERING)))) || ((LA78_0 >= (Cql_Parser.K_COMPACT)) && (LA78_0 <= (Cql_Parser.K_COUNTER)))) || ((LA78_0 >= (Cql_Parser.K_CUSTOM)) && (LA78_0 <= (Cql_Parser.K_DECIMAL)))) || ((LA78_0 >= (Cql_Parser.K_DISTINCT)) && (LA78_0 <= (Cql_Parser.K_DOUBLE)))) || (LA78_0 == (Cql_Parser.K_DURATION))) || ((LA78_0 >= (Cql_Parser.K_EXISTS)) && (LA78_0 <= (Cql_Parser.K_FLOAT)))) || (LA78_0 == (Cql_Parser.K_FROZEN))) || ((LA78_0 >= (Cql_Parser.K_FUNCTION)) && (LA78_0 <= (Cql_Parser.K_FUNCTIONS)))) || (LA78_0 == (Cql_Parser.K_GROUP))) || (LA78_0 == (Cql_Parser.K_INET))) || ((LA78_0 >= (Cql_Parser.K_INITCOND)) && (LA78_0 <= (Cql_Parser.K_INPUT)))) || (LA78_0 == (Cql_Parser.K_INT))) || ((LA78_0 >= (Cql_Parser.K_JSON)) && (LA78_0 <= (Cql_Parser.K_KEYS)))) || ((LA78_0 >= (Cql_Parser.K_KEYSPACES)) && (LA78_0 <= (Cql_Parser.K_LIKE)))) || ((LA78_0 >= (Cql_Parser.K_LIST)) && (LA78_0 <= (Cql_Parser.K_MAP)))) || (LA78_0 == (Cql_Parser.K_NOLOGIN))) || (LA78_0 == (Cql_Parser.K_NOSUPERUSER))) || (LA78_0 == (Cql_Parser.K_OPTIONS))) || ((LA78_0 >= (Cql_Parser.K_PARTITION)) && (LA78_0 <= (Cql_Parser.K_PERMISSIONS)))) || (LA78_0 == (Cql_Parser.K_RETURNS))) || ((LA78_0 >= (Cql_Parser.K_ROLE)) && (LA78_0 <= (Cql_Parser.K_ROLES)))) || ((LA78_0 >= (Cql_Parser.K_SFUNC)) && (LA78_0 <= (Cql_Parser.K_TINYINT)))) || (LA78_0 == (Cql_Parser.K_TRIGGER))) || ((LA78_0 >= (Cql_Parser.K_TTL)) && (LA78_0 <= (Cql_Parser.K_TYPE)))) || ((LA78_0 >= (Cql_Parser.K_USER)) && (LA78_0 <= (Cql_Parser.K_USERS)))) || ((LA78_0 >= (Cql_Parser.K_UUID)) && (LA78_0 <= (Cql_Parser.K_VARINT)))) || (LA78_0 == (Cql_Parser.K_WRITETIME))) || (LA78_0 == (Cql_Parser.QUOTED_NAME))) {
				alt78 = 1;
			}else
				if (LA78_0 == (Cql_Parser.K_PRIMARY)) {
					alt78 = 2;
				}else {
					NoViableAltException nvae = new NoViableAltException("", 78, 0, input);
					throw nvae;
				}

			switch (alt78) {
				case 1 :
					{
						pushFollow(Cql_Parser.FOLLOW_ident_in_cfamColumns3997);
						k = ident();
						(state._fsp)--;
						pushFollow(Cql_Parser.FOLLOW_comparatorType_in_cfamColumns4001);
						v = comparatorType();
						(state._fsp)--;
						boolean isStatic = false;
						int alt75 = 2;
						int LA75_0 = input.LA(1);
						if (LA75_0 == (Cql_Parser.K_STATIC)) {
							alt75 = 1;
						}
						switch (alt75) {
							case 1 :
								{
									match(input, Cql_Parser.K_STATIC, Cql_Parser.FOLLOW_K_STATIC_in_cfamColumns4006);
									isStatic = true;
								}
								break;
						}
						expr.addDefinition(k, v, isStatic);
						int alt76 = 2;
						int LA76_0 = input.LA(1);
						if (LA76_0 == (Cql_Parser.K_PRIMARY)) {
							alt76 = 1;
						}
						switch (alt76) {
							case 1 :
								{
									match(input, Cql_Parser.K_PRIMARY, Cql_Parser.FOLLOW_K_PRIMARY_in_cfamColumns4023);
									match(input, Cql_Parser.K_KEY, Cql_Parser.FOLLOW_K_KEY_in_cfamColumns4025);
									expr.addKeyAliases(Collections.singletonList(k));
								}
								break;
						}
					}
					break;
				case 2 :
					{
						match(input, Cql_Parser.K_PRIMARY, Cql_Parser.FOLLOW_K_PRIMARY_in_cfamColumns4037);
						match(input, Cql_Parser.K_KEY, Cql_Parser.FOLLOW_K_KEY_in_cfamColumns4039);
						match(input, 184, Cql_Parser.FOLLOW_184_in_cfamColumns4041);
						pushFollow(Cql_Parser.FOLLOW_pkDef_in_cfamColumns4043);
						pkDef(expr);
						(state._fsp)--;
						loop77 : while (true) {
							int alt77 = 2;
							int LA77_0 = input.LA(1);
							if (LA77_0 == 188) {
								alt77 = 1;
							}
							switch (alt77) {
								case 1 :
									{
										match(input, 188, Cql_Parser.FOLLOW_188_in_cfamColumns4047);
										pushFollow(Cql_Parser.FOLLOW_ident_in_cfamColumns4051);
										c = ident();
										(state._fsp)--;
										expr.addColumnAlias(c);
									}
									break;
								default :
									break loop77;
							}
						} 
						match(input, 185, Cql_Parser.FOLLOW_185_in_cfamColumns4058);
					}
					break;
			}
		} catch (RecognitionException re) {
			reportError(re);
			recover(input, re);
		} finally {
		}
	}

	public final void pkDef(CreateTableStatement.RawStatement expr) throws RecognitionException {
		ColumnIdentifier k = null;
		ColumnIdentifier k1 = null;
		ColumnIdentifier kn = null;
		try {
			int alt80 = 2;
			int LA80_0 = input.LA(1);
			if (((((((((((((((((((((((((((((((((LA80_0 == (Cql_Parser.IDENT)) || ((LA80_0 >= (Cql_Parser.K_AGGREGATE)) && (LA80_0 <= (Cql_Parser.K_ALL)))) || (LA80_0 == (Cql_Parser.K_AS))) || (LA80_0 == (Cql_Parser.K_ASCII))) || ((LA80_0 >= (Cql_Parser.K_BIGINT)) && (LA80_0 <= (Cql_Parser.K_BOOLEAN)))) || ((LA80_0 >= (Cql_Parser.K_CALLED)) && (LA80_0 <= (Cql_Parser.K_CLUSTERING)))) || ((LA80_0 >= (Cql_Parser.K_COMPACT)) && (LA80_0 <= (Cql_Parser.K_COUNTER)))) || ((LA80_0 >= (Cql_Parser.K_CUSTOM)) && (LA80_0 <= (Cql_Parser.K_DECIMAL)))) || ((LA80_0 >= (Cql_Parser.K_DISTINCT)) && (LA80_0 <= (Cql_Parser.K_DOUBLE)))) || (LA80_0 == (Cql_Parser.K_DURATION))) || ((LA80_0 >= (Cql_Parser.K_EXISTS)) && (LA80_0 <= (Cql_Parser.K_FLOAT)))) || (LA80_0 == (Cql_Parser.K_FROZEN))) || ((LA80_0 >= (Cql_Parser.K_FUNCTION)) && (LA80_0 <= (Cql_Parser.K_FUNCTIONS)))) || (LA80_0 == (Cql_Parser.K_GROUP))) || (LA80_0 == (Cql_Parser.K_INET))) || ((LA80_0 >= (Cql_Parser.K_INITCOND)) && (LA80_0 <= (Cql_Parser.K_INPUT)))) || (LA80_0 == (Cql_Parser.K_INT))) || ((LA80_0 >= (Cql_Parser.K_JSON)) && (LA80_0 <= (Cql_Parser.K_KEYS)))) || ((LA80_0 >= (Cql_Parser.K_KEYSPACES)) && (LA80_0 <= (Cql_Parser.K_LIKE)))) || ((LA80_0 >= (Cql_Parser.K_LIST)) && (LA80_0 <= (Cql_Parser.K_MAP)))) || (LA80_0 == (Cql_Parser.K_NOLOGIN))) || (LA80_0 == (Cql_Parser.K_NOSUPERUSER))) || (LA80_0 == (Cql_Parser.K_OPTIONS))) || ((LA80_0 >= (Cql_Parser.K_PARTITION)) && (LA80_0 <= (Cql_Parser.K_PERMISSIONS)))) || (LA80_0 == (Cql_Parser.K_RETURNS))) || ((LA80_0 >= (Cql_Parser.K_ROLE)) && (LA80_0 <= (Cql_Parser.K_ROLES)))) || ((LA80_0 >= (Cql_Parser.K_SFUNC)) && (LA80_0 <= (Cql_Parser.K_TINYINT)))) || (LA80_0 == (Cql_Parser.K_TRIGGER))) || ((LA80_0 >= (Cql_Parser.K_TTL)) && (LA80_0 <= (Cql_Parser.K_TYPE)))) || ((LA80_0 >= (Cql_Parser.K_USER)) && (LA80_0 <= (Cql_Parser.K_USERS)))) || ((LA80_0 >= (Cql_Parser.K_UUID)) && (LA80_0 <= (Cql_Parser.K_VARINT)))) || (LA80_0 == (Cql_Parser.K_WRITETIME))) || (LA80_0 == (Cql_Parser.QUOTED_NAME))) {
				alt80 = 1;
			}else
				if (LA80_0 == 184) {
					alt80 = 2;
				}else {
					NoViableAltException nvae = new NoViableAltException("", 80, 0, input);
					throw nvae;
				}

			switch (alt80) {
				case 1 :
					{
						pushFollow(Cql_Parser.FOLLOW_ident_in_pkDef4078);
						k = ident();
						(state._fsp)--;
						expr.addKeyAliases(Collections.singletonList(k));
					}
					break;
				case 2 :
					{
						match(input, 184, Cql_Parser.FOLLOW_184_in_pkDef4088);
						List<ColumnIdentifier> l = new ArrayList<ColumnIdentifier>();
						pushFollow(Cql_Parser.FOLLOW_ident_in_pkDef4094);
						k1 = ident();
						(state._fsp)--;
						l.add(k1);
						loop79 : while (true) {
							int alt79 = 2;
							int LA79_0 = input.LA(1);
							if (LA79_0 == 188) {
								alt79 = 1;
							}
							switch (alt79) {
								case 1 :
									{
										match(input, 188, Cql_Parser.FOLLOW_188_in_pkDef4100);
										pushFollow(Cql_Parser.FOLLOW_ident_in_pkDef4104);
										kn = ident();
										(state._fsp)--;
										l.add(kn);
									}
									break;
								default :
									break loop79;
							}
						} 
						match(input, 185, Cql_Parser.FOLLOW_185_in_pkDef4111);
						expr.addKeyAliases(l);
					}
					break;
			}
		} catch (RecognitionException re) {
			reportError(re);
			recover(input, re);
		} finally {
		}
	}

	public final void cfamProperty(CFProperties props) throws RecognitionException {
		try {
			int alt82 = 3;
			switch (input.LA(1)) {
				case Cql_Parser.IDENT :
				case Cql_Parser.K_AGGREGATE :
				case Cql_Parser.K_ALL :
				case Cql_Parser.K_AS :
				case Cql_Parser.K_ASCII :
				case Cql_Parser.K_BIGINT :
				case Cql_Parser.K_BLOB :
				case Cql_Parser.K_BOOLEAN :
				case Cql_Parser.K_CALLED :
				case Cql_Parser.K_CAST :
				case Cql_Parser.K_CONTAINS :
				case Cql_Parser.K_COUNT :
				case Cql_Parser.K_COUNTER :
				case Cql_Parser.K_CUSTOM :
				case Cql_Parser.K_DATE :
				case Cql_Parser.K_DECIMAL :
				case Cql_Parser.K_DISTINCT :
				case Cql_Parser.K_DOUBLE :
				case Cql_Parser.K_DURATION :
				case Cql_Parser.K_EXISTS :
				case Cql_Parser.K_FILTERING :
				case Cql_Parser.K_FINALFUNC :
				case Cql_Parser.K_FLOAT :
				case Cql_Parser.K_FROZEN :
				case Cql_Parser.K_FUNCTION :
				case Cql_Parser.K_FUNCTIONS :
				case Cql_Parser.K_GROUP :
				case Cql_Parser.K_INET :
				case Cql_Parser.K_INITCOND :
				case Cql_Parser.K_INPUT :
				case Cql_Parser.K_INT :
				case Cql_Parser.K_JSON :
				case Cql_Parser.K_KEY :
				case Cql_Parser.K_KEYS :
				case Cql_Parser.K_KEYSPACES :
				case Cql_Parser.K_LANGUAGE :
				case Cql_Parser.K_LIKE :
				case Cql_Parser.K_LIST :
				case Cql_Parser.K_LOGIN :
				case Cql_Parser.K_MAP :
				case Cql_Parser.K_NOLOGIN :
				case Cql_Parser.K_NOSUPERUSER :
				case Cql_Parser.K_OPTIONS :
				case Cql_Parser.K_PARTITION :
				case Cql_Parser.K_PASSWORD :
				case Cql_Parser.K_PER :
				case Cql_Parser.K_PERMISSION :
				case Cql_Parser.K_PERMISSIONS :
				case Cql_Parser.K_RETURNS :
				case Cql_Parser.K_ROLE :
				case Cql_Parser.K_ROLES :
				case Cql_Parser.K_SFUNC :
				case Cql_Parser.K_SMALLINT :
				case Cql_Parser.K_STATIC :
				case Cql_Parser.K_STORAGE :
				case Cql_Parser.K_STYPE :
				case Cql_Parser.K_SUPERUSER :
				case Cql_Parser.K_TEXT :
				case Cql_Parser.K_TIME :
				case Cql_Parser.K_TIMESTAMP :
				case Cql_Parser.K_TIMEUUID :
				case Cql_Parser.K_TINYINT :
				case Cql_Parser.K_TRIGGER :
				case Cql_Parser.K_TTL :
				case Cql_Parser.K_TUPLE :
				case Cql_Parser.K_TYPE :
				case Cql_Parser.K_USER :
				case Cql_Parser.K_USERS :
				case Cql_Parser.K_UUID :
				case Cql_Parser.K_VALUES :
				case Cql_Parser.K_VARCHAR :
				case Cql_Parser.K_VARINT :
				case Cql_Parser.K_WRITETIME :
				case Cql_Parser.QUOTED_NAME :
					{
						alt82 = 1;
					}
					break;
				case Cql_Parser.K_COMPACT :
					{
						int LA82_2 = input.LA(2);
						if (LA82_2 == (Cql_Parser.K_STORAGE)) {
							alt82 = 2;
						}else
							if (LA82_2 == 196) {
								alt82 = 1;
							}else {
								int nvaeMark = input.mark();
								try {
									input.consume();
									NoViableAltException nvae = new NoViableAltException("", 82, 2, input);
									throw nvae;
								} finally {
									input.rewind(nvaeMark);
								}
							}

					}
					break;
				case Cql_Parser.K_CLUSTERING :
					{
						int LA82_3 = input.LA(2);
						if (LA82_3 == (Cql_Parser.K_ORDER)) {
							alt82 = 3;
						}else
							if (LA82_3 == 196) {
								alt82 = 1;
							}else {
								int nvaeMark = input.mark();
								try {
									input.consume();
									NoViableAltException nvae = new NoViableAltException("", 82, 3, input);
									throw nvae;
								} finally {
									input.rewind(nvaeMark);
								}
							}

					}
					break;
				default :
					NoViableAltException nvae = new NoViableAltException("", 82, 0, input);
					throw nvae;
			}
			switch (alt82) {
				case 1 :
					{
						pushFollow(Cql_Parser.FOLLOW_property_in_cfamProperty4131);
						property(props.properties);
						(state._fsp)--;
					}
					break;
				case 2 :
					{
						match(input, Cql_Parser.K_COMPACT, Cql_Parser.FOLLOW_K_COMPACT_in_cfamProperty4140);
						match(input, Cql_Parser.K_STORAGE, Cql_Parser.FOLLOW_K_STORAGE_in_cfamProperty4142);
						props.setCompactStorage();
					}
					break;
				case 3 :
					{
						match(input, Cql_Parser.K_CLUSTERING, Cql_Parser.FOLLOW_K_CLUSTERING_in_cfamProperty4152);
						match(input, Cql_Parser.K_ORDER, Cql_Parser.FOLLOW_K_ORDER_in_cfamProperty4154);
						match(input, Cql_Parser.K_BY, Cql_Parser.FOLLOW_K_BY_in_cfamProperty4156);
						match(input, 184, Cql_Parser.FOLLOW_184_in_cfamProperty4158);
						pushFollow(Cql_Parser.FOLLOW_cfamOrdering_in_cfamProperty4160);
						cfamOrdering(props);
						(state._fsp)--;
						loop81 : while (true) {
							int alt81 = 2;
							int LA81_0 = input.LA(1);
							if (LA81_0 == 188) {
								alt81 = 1;
							}
							switch (alt81) {
								case 1 :
									{
										match(input, 188, Cql_Parser.FOLLOW_188_in_cfamProperty4164);
										pushFollow(Cql_Parser.FOLLOW_cfamOrdering_in_cfamProperty4166);
										cfamOrdering(props);
										(state._fsp)--;
									}
									break;
								default :
									break loop81;
							}
						} 
						match(input, 185, Cql_Parser.FOLLOW_185_in_cfamProperty4171);
					}
					break;
			}
		} catch (RecognitionException re) {
			reportError(re);
			recover(input, re);
		} finally {
		}
	}

	public final void cfamOrdering(CFProperties props) throws RecognitionException {
		ColumnIdentifier k = null;
		boolean reversed = false;
		try {
			{
				pushFollow(Cql_Parser.FOLLOW_ident_in_cfamOrdering4199);
				k = ident();
				(state._fsp)--;
				int alt83 = 2;
				int LA83_0 = input.LA(1);
				if (LA83_0 == (Cql_Parser.K_ASC)) {
					alt83 = 1;
				}else
					if (LA83_0 == (Cql_Parser.K_DESC)) {
						alt83 = 2;
					}else {
						NoViableAltException nvae = new NoViableAltException("", 83, 0, input);
						throw nvae;
					}

				switch (alt83) {
					case 1 :
						{
							match(input, Cql_Parser.K_ASC, Cql_Parser.FOLLOW_K_ASC_in_cfamOrdering4202);
						}
						break;
					case 2 :
						{
							match(input, Cql_Parser.K_DESC, Cql_Parser.FOLLOW_K_DESC_in_cfamOrdering4206);
							reversed = true;
						}
						break;
				}
				props.setOrdering(k, reversed);
			}
		} catch (RecognitionException re) {
			reportError(re);
			recover(input, re);
		} finally {
		}
	}

	public final CreateTypeStatement createTypeStatement() throws RecognitionException {
		CreateTypeStatement expr = null;
		UTName tn = null;
		boolean ifNotExists = false;
		try {
			{
				match(input, Cql_Parser.K_CREATE, Cql_Parser.FOLLOW_K_CREATE_in_createTypeStatement4245);
				match(input, Cql_Parser.K_TYPE, Cql_Parser.FOLLOW_K_TYPE_in_createTypeStatement4247);
				int alt84 = 2;
				int LA84_0 = input.LA(1);
				if (LA84_0 == (Cql_Parser.K_IF)) {
					alt84 = 1;
				}
				switch (alt84) {
					case 1 :
						{
							match(input, Cql_Parser.K_IF, Cql_Parser.FOLLOW_K_IF_in_createTypeStatement4250);
							match(input, Cql_Parser.K_NOT, Cql_Parser.FOLLOW_K_NOT_in_createTypeStatement4252);
							match(input, Cql_Parser.K_EXISTS, Cql_Parser.FOLLOW_K_EXISTS_in_createTypeStatement4254);
							ifNotExists = true;
						}
						break;
				}
				pushFollow(Cql_Parser.FOLLOW_userTypeName_in_createTypeStatement4272);
				tn = userTypeName();
				(state._fsp)--;
				expr = new CreateTypeStatement(tn, ifNotExists);
				match(input, 184, Cql_Parser.FOLLOW_184_in_createTypeStatement4285);
				pushFollow(Cql_Parser.FOLLOW_typeColumns_in_createTypeStatement4287);
				typeColumns(expr);
				(state._fsp)--;
				loop86 : while (true) {
					int alt86 = 2;
					int LA86_0 = input.LA(1);
					if (LA86_0 == 188) {
						alt86 = 1;
					}
					switch (alt86) {
						case 1 :
							{
								match(input, 188, Cql_Parser.FOLLOW_188_in_createTypeStatement4292);
								int alt85 = 2;
								int LA85_0 = input.LA(1);
								if (((((((((((((((((((((((((((((((((LA85_0 == (Cql_Parser.IDENT)) || ((LA85_0 >= (Cql_Parser.K_AGGREGATE)) && (LA85_0 <= (Cql_Parser.K_ALL)))) || (LA85_0 == (Cql_Parser.K_AS))) || (LA85_0 == (Cql_Parser.K_ASCII))) || ((LA85_0 >= (Cql_Parser.K_BIGINT)) && (LA85_0 <= (Cql_Parser.K_BOOLEAN)))) || ((LA85_0 >= (Cql_Parser.K_CALLED)) && (LA85_0 <= (Cql_Parser.K_CLUSTERING)))) || ((LA85_0 >= (Cql_Parser.K_COMPACT)) && (LA85_0 <= (Cql_Parser.K_COUNTER)))) || ((LA85_0 >= (Cql_Parser.K_CUSTOM)) && (LA85_0 <= (Cql_Parser.K_DECIMAL)))) || ((LA85_0 >= (Cql_Parser.K_DISTINCT)) && (LA85_0 <= (Cql_Parser.K_DOUBLE)))) || (LA85_0 == (Cql_Parser.K_DURATION))) || ((LA85_0 >= (Cql_Parser.K_EXISTS)) && (LA85_0 <= (Cql_Parser.K_FLOAT)))) || (LA85_0 == (Cql_Parser.K_FROZEN))) || ((LA85_0 >= (Cql_Parser.K_FUNCTION)) && (LA85_0 <= (Cql_Parser.K_FUNCTIONS)))) || (LA85_0 == (Cql_Parser.K_GROUP))) || (LA85_0 == (Cql_Parser.K_INET))) || ((LA85_0 >= (Cql_Parser.K_INITCOND)) && (LA85_0 <= (Cql_Parser.K_INPUT)))) || (LA85_0 == (Cql_Parser.K_INT))) || ((LA85_0 >= (Cql_Parser.K_JSON)) && (LA85_0 <= (Cql_Parser.K_KEYS)))) || ((LA85_0 >= (Cql_Parser.K_KEYSPACES)) && (LA85_0 <= (Cql_Parser.K_LIKE)))) || ((LA85_0 >= (Cql_Parser.K_LIST)) && (LA85_0 <= (Cql_Parser.K_MAP)))) || (LA85_0 == (Cql_Parser.K_NOLOGIN))) || (LA85_0 == (Cql_Parser.K_NOSUPERUSER))) || (LA85_0 == (Cql_Parser.K_OPTIONS))) || ((LA85_0 >= (Cql_Parser.K_PARTITION)) && (LA85_0 <= (Cql_Parser.K_PERMISSIONS)))) || (LA85_0 == (Cql_Parser.K_RETURNS))) || ((LA85_0 >= (Cql_Parser.K_ROLE)) && (LA85_0 <= (Cql_Parser.K_ROLES)))) || ((LA85_0 >= (Cql_Parser.K_SFUNC)) && (LA85_0 <= (Cql_Parser.K_TINYINT)))) || (LA85_0 == (Cql_Parser.K_TRIGGER))) || ((LA85_0 >= (Cql_Parser.K_TTL)) && (LA85_0 <= (Cql_Parser.K_TYPE)))) || ((LA85_0 >= (Cql_Parser.K_USER)) && (LA85_0 <= (Cql_Parser.K_USERS)))) || ((LA85_0 >= (Cql_Parser.K_UUID)) && (LA85_0 <= (Cql_Parser.K_VARINT)))) || (LA85_0 == (Cql_Parser.K_WRITETIME))) || (LA85_0 == (Cql_Parser.QUOTED_NAME))) {
									alt85 = 1;
								}
								switch (alt85) {
									case 1 :
										{
											pushFollow(Cql_Parser.FOLLOW_typeColumns_in_createTypeStatement4294);
											typeColumns(expr);
											(state._fsp)--;
										}
										break;
								}
							}
							break;
						default :
							break loop86;
					}
				} 
				match(input, 185, Cql_Parser.FOLLOW_185_in_createTypeStatement4301);
			}
		} catch (RecognitionException re) {
			reportError(re);
			recover(input, re);
		} finally {
		}
		return expr;
	}

	public final void typeColumns(CreateTypeStatement expr) throws RecognitionException {
		FieldIdentifier k = null;
		CQL3Type.Raw v = null;
		try {
			{
				pushFollow(Cql_Parser.FOLLOW_fident_in_typeColumns4321);
				k = fident();
				(state._fsp)--;
				pushFollow(Cql_Parser.FOLLOW_comparatorType_in_typeColumns4325);
				v = comparatorType();
				(state._fsp)--;
				expr.addDefinition(k, v);
			}
		} catch (RecognitionException re) {
			reportError(re);
			recover(input, re);
		} finally {
		}
	}

	public final CreateIndexStatement createIndexStatement() throws RecognitionException {
		CreateIndexStatement expr = null;
		Token cls = null;
		CFName cf = null;
		IndexPropDefs props = new IndexPropDefs();
		boolean ifNotExists = false;
		IndexName name = new IndexName();
		List<IndexTarget.Raw> targets = new ArrayList<>();
		try {
			{
				match(input, Cql_Parser.K_CREATE, Cql_Parser.FOLLOW_K_CREATE_in_createIndexStatement4360);
				int alt87 = 2;
				int LA87_0 = input.LA(1);
				if (LA87_0 == (Cql_Parser.K_CUSTOM)) {
					alt87 = 1;
				}
				switch (alt87) {
					case 1 :
						{
							match(input, Cql_Parser.K_CUSTOM, Cql_Parser.FOLLOW_K_CUSTOM_in_createIndexStatement4363);
							props.isCustom = true;
						}
						break;
				}
				match(input, Cql_Parser.K_INDEX, Cql_Parser.FOLLOW_K_INDEX_in_createIndexStatement4369);
				int alt88 = 2;
				int LA88_0 = input.LA(1);
				if (LA88_0 == (Cql_Parser.K_IF)) {
					alt88 = 1;
				}
				switch (alt88) {
					case 1 :
						{
							match(input, Cql_Parser.K_IF, Cql_Parser.FOLLOW_K_IF_in_createIndexStatement4372);
							match(input, Cql_Parser.K_NOT, Cql_Parser.FOLLOW_K_NOT_in_createIndexStatement4374);
							match(input, Cql_Parser.K_EXISTS, Cql_Parser.FOLLOW_K_EXISTS_in_createIndexStatement4376);
							ifNotExists = true;
						}
						break;
				}
				int alt89 = 2;
				int LA89_0 = input.LA(1);
				if (((((((((((((((((((((((((((((((((LA89_0 == (Cql_Parser.IDENT)) || ((LA89_0 >= (Cql_Parser.K_AGGREGATE)) && (LA89_0 <= (Cql_Parser.K_ALL)))) || (LA89_0 == (Cql_Parser.K_AS))) || (LA89_0 == (Cql_Parser.K_ASCII))) || ((LA89_0 >= (Cql_Parser.K_BIGINT)) && (LA89_0 <= (Cql_Parser.K_BOOLEAN)))) || ((LA89_0 >= (Cql_Parser.K_CALLED)) && (LA89_0 <= (Cql_Parser.K_CLUSTERING)))) || ((LA89_0 >= (Cql_Parser.K_COMPACT)) && (LA89_0 <= (Cql_Parser.K_COUNTER)))) || ((LA89_0 >= (Cql_Parser.K_CUSTOM)) && (LA89_0 <= (Cql_Parser.K_DECIMAL)))) || ((LA89_0 >= (Cql_Parser.K_DISTINCT)) && (LA89_0 <= (Cql_Parser.K_DOUBLE)))) || (LA89_0 == (Cql_Parser.K_DURATION))) || ((LA89_0 >= (Cql_Parser.K_EXISTS)) && (LA89_0 <= (Cql_Parser.K_FLOAT)))) || (LA89_0 == (Cql_Parser.K_FROZEN))) || ((LA89_0 >= (Cql_Parser.K_FUNCTION)) && (LA89_0 <= (Cql_Parser.K_FUNCTIONS)))) || (LA89_0 == (Cql_Parser.K_GROUP))) || (LA89_0 == (Cql_Parser.K_INET))) || ((LA89_0 >= (Cql_Parser.K_INITCOND)) && (LA89_0 <= (Cql_Parser.K_INPUT)))) || (LA89_0 == (Cql_Parser.K_INT))) || ((LA89_0 >= (Cql_Parser.K_JSON)) && (LA89_0 <= (Cql_Parser.K_KEYS)))) || ((LA89_0 >= (Cql_Parser.K_KEYSPACES)) && (LA89_0 <= (Cql_Parser.K_LIKE)))) || ((LA89_0 >= (Cql_Parser.K_LIST)) && (LA89_0 <= (Cql_Parser.K_MAP)))) || (LA89_0 == (Cql_Parser.K_NOLOGIN))) || (LA89_0 == (Cql_Parser.K_NOSUPERUSER))) || (LA89_0 == (Cql_Parser.K_OPTIONS))) || ((LA89_0 >= (Cql_Parser.K_PARTITION)) && (LA89_0 <= (Cql_Parser.K_PERMISSIONS)))) || (LA89_0 == (Cql_Parser.K_RETURNS))) || ((LA89_0 >= (Cql_Parser.K_ROLE)) && (LA89_0 <= (Cql_Parser.K_ROLES)))) || ((LA89_0 >= (Cql_Parser.K_SFUNC)) && (LA89_0 <= (Cql_Parser.K_TINYINT)))) || (LA89_0 == (Cql_Parser.K_TRIGGER))) || ((LA89_0 >= (Cql_Parser.K_TTL)) && (LA89_0 <= (Cql_Parser.K_TYPE)))) || ((LA89_0 >= (Cql_Parser.K_USER)) && (LA89_0 <= (Cql_Parser.K_USERS)))) || ((LA89_0 >= (Cql_Parser.K_UUID)) && (LA89_0 <= (Cql_Parser.K_VARINT)))) || (LA89_0 == (Cql_Parser.K_WRITETIME))) || ((LA89_0 >= (Cql_Parser.QMARK)) && (LA89_0 <= (Cql_Parser.QUOTED_NAME)))) {
					alt89 = 1;
				}
				switch (alt89) {
					case 1 :
						{
							pushFollow(Cql_Parser.FOLLOW_idxName_in_createIndexStatement4392);
							idxName(name);
							(state._fsp)--;
						}
						break;
				}
				match(input, Cql_Parser.K_ON, Cql_Parser.FOLLOW_K_ON_in_createIndexStatement4397);
				pushFollow(Cql_Parser.FOLLOW_columnFamilyName_in_createIndexStatement4401);
				cf = columnFamilyName();
				(state._fsp)--;
				match(input, 184, Cql_Parser.FOLLOW_184_in_createIndexStatement4403);
				int alt91 = 2;
				int LA91_0 = input.LA(1);
				if (((((((((((((((((((((((((((((((((LA91_0 == (Cql_Parser.EMPTY_QUOTED_NAME)) || (LA91_0 == (Cql_Parser.IDENT))) || ((LA91_0 >= (Cql_Parser.K_AGGREGATE)) && (LA91_0 <= (Cql_Parser.K_ALL)))) || (LA91_0 == (Cql_Parser.K_AS))) || (LA91_0 == (Cql_Parser.K_ASCII))) || ((LA91_0 >= (Cql_Parser.K_BIGINT)) && (LA91_0 <= (Cql_Parser.K_BOOLEAN)))) || ((LA91_0 >= (Cql_Parser.K_CALLED)) && (LA91_0 <= (Cql_Parser.K_CLUSTERING)))) || ((LA91_0 >= (Cql_Parser.K_COMPACT)) && (LA91_0 <= (Cql_Parser.K_COUNTER)))) || ((LA91_0 >= (Cql_Parser.K_CUSTOM)) && (LA91_0 <= (Cql_Parser.K_DECIMAL)))) || ((LA91_0 >= (Cql_Parser.K_DISTINCT)) && (LA91_0 <= (Cql_Parser.K_DOUBLE)))) || ((LA91_0 >= (Cql_Parser.K_DURATION)) && (LA91_0 <= (Cql_Parser.K_ENTRIES)))) || ((LA91_0 >= (Cql_Parser.K_EXISTS)) && (LA91_0 <= (Cql_Parser.K_FLOAT)))) || ((LA91_0 >= (Cql_Parser.K_FROZEN)) && (LA91_0 <= (Cql_Parser.K_FUNCTIONS)))) || (LA91_0 == (Cql_Parser.K_GROUP))) || (LA91_0 == (Cql_Parser.K_INET))) || ((LA91_0 >= (Cql_Parser.K_INITCOND)) && (LA91_0 <= (Cql_Parser.K_INPUT)))) || (LA91_0 == (Cql_Parser.K_INT))) || ((LA91_0 >= (Cql_Parser.K_JSON)) && (LA91_0 <= (Cql_Parser.K_KEYS)))) || ((LA91_0 >= (Cql_Parser.K_KEYSPACES)) && (LA91_0 <= (Cql_Parser.K_LIKE)))) || ((LA91_0 >= (Cql_Parser.K_LIST)) && (LA91_0 <= (Cql_Parser.K_MAP)))) || (LA91_0 == (Cql_Parser.K_NOLOGIN))) || (LA91_0 == (Cql_Parser.K_NOSUPERUSER))) || (LA91_0 == (Cql_Parser.K_OPTIONS))) || ((LA91_0 >= (Cql_Parser.K_PARTITION)) && (LA91_0 <= (Cql_Parser.K_PERMISSIONS)))) || (LA91_0 == (Cql_Parser.K_RETURNS))) || ((LA91_0 >= (Cql_Parser.K_ROLE)) && (LA91_0 <= (Cql_Parser.K_ROLES)))) || ((LA91_0 >= (Cql_Parser.K_SFUNC)) && (LA91_0 <= (Cql_Parser.K_TINYINT)))) || (LA91_0 == (Cql_Parser.K_TRIGGER))) || ((LA91_0 >= (Cql_Parser.K_TTL)) && (LA91_0 <= (Cql_Parser.K_TYPE)))) || ((LA91_0 >= (Cql_Parser.K_USER)) && (LA91_0 <= (Cql_Parser.K_USERS)))) || ((LA91_0 >= (Cql_Parser.K_UUID)) && (LA91_0 <= (Cql_Parser.K_VARINT)))) || (LA91_0 == (Cql_Parser.K_WRITETIME))) || (LA91_0 == (Cql_Parser.QUOTED_NAME))) {
					alt91 = 1;
				}
				switch (alt91) {
					case 1 :
						{
							pushFollow(Cql_Parser.FOLLOW_indexIdent_in_createIndexStatement4406);
							indexIdent(targets);
							(state._fsp)--;
							loop90 : while (true) {
								int alt90 = 2;
								int LA90_0 = input.LA(1);
								if (LA90_0 == 188) {
									alt90 = 1;
								}
								switch (alt90) {
									case 1 :
										{
											match(input, 188, Cql_Parser.FOLLOW_188_in_createIndexStatement4410);
											pushFollow(Cql_Parser.FOLLOW_indexIdent_in_createIndexStatement4412);
											indexIdent(targets);
											(state._fsp)--;
										}
										break;
									default :
										break loop90;
								}
							} 
						}
						break;
				}
				match(input, 185, Cql_Parser.FOLLOW_185_in_createIndexStatement4419);
				int alt92 = 2;
				int LA92_0 = input.LA(1);
				if (LA92_0 == (Cql_Parser.K_USING)) {
					alt92 = 1;
				}
				switch (alt92) {
					case 1 :
						{
							match(input, Cql_Parser.K_USING, Cql_Parser.FOLLOW_K_USING_in_createIndexStatement4430);
							cls = ((Token) (match(input, Cql_Parser.STRING_LITERAL, Cql_Parser.FOLLOW_STRING_LITERAL_in_createIndexStatement4434)));
							props.customClass = (cls != null) ? cls.getText() : null;
						}
						break;
				}
				int alt93 = 2;
				int LA93_0 = input.LA(1);
				if (LA93_0 == (Cql_Parser.K_WITH)) {
					alt93 = 1;
				}
				switch (alt93) {
					case 1 :
						{
							match(input, Cql_Parser.K_WITH, Cql_Parser.FOLLOW_K_WITH_in_createIndexStatement4449);
							pushFollow(Cql_Parser.FOLLOW_properties_in_createIndexStatement4451);
							properties(props);
							(state._fsp)--;
						}
						break;
				}
				expr = new CreateIndexStatement(cf, name, targets, props, ifNotExists);
			}
		} catch (RecognitionException re) {
			reportError(re);
			recover(input, re);
		} finally {
		}
		return expr;
	}

	public final void indexIdent(List<IndexTarget.Raw> targets) throws RecognitionException {
		ColumnDefinition.Raw c = null;
		try {
			int alt94 = 5;
			switch (input.LA(1)) {
				case Cql_Parser.EMPTY_QUOTED_NAME :
				case Cql_Parser.IDENT :
				case Cql_Parser.K_AGGREGATE :
				case Cql_Parser.K_ALL :
				case Cql_Parser.K_AS :
				case Cql_Parser.K_ASCII :
				case Cql_Parser.K_BIGINT :
				case Cql_Parser.K_BLOB :
				case Cql_Parser.K_BOOLEAN :
				case Cql_Parser.K_CALLED :
				case Cql_Parser.K_CAST :
				case Cql_Parser.K_CLUSTERING :
				case Cql_Parser.K_COMPACT :
				case Cql_Parser.K_CONTAINS :
				case Cql_Parser.K_COUNT :
				case Cql_Parser.K_COUNTER :
				case Cql_Parser.K_CUSTOM :
				case Cql_Parser.K_DATE :
				case Cql_Parser.K_DECIMAL :
				case Cql_Parser.K_DISTINCT :
				case Cql_Parser.K_DOUBLE :
				case Cql_Parser.K_DURATION :
				case Cql_Parser.K_EXISTS :
				case Cql_Parser.K_FILTERING :
				case Cql_Parser.K_FINALFUNC :
				case Cql_Parser.K_FLOAT :
				case Cql_Parser.K_FROZEN :
				case Cql_Parser.K_FUNCTION :
				case Cql_Parser.K_FUNCTIONS :
				case Cql_Parser.K_GROUP :
				case Cql_Parser.K_INET :
				case Cql_Parser.K_INITCOND :
				case Cql_Parser.K_INPUT :
				case Cql_Parser.K_INT :
				case Cql_Parser.K_JSON :
				case Cql_Parser.K_KEY :
				case Cql_Parser.K_KEYSPACES :
				case Cql_Parser.K_LANGUAGE :
				case Cql_Parser.K_LIKE :
				case Cql_Parser.K_LIST :
				case Cql_Parser.K_LOGIN :
				case Cql_Parser.K_MAP :
				case Cql_Parser.K_NOLOGIN :
				case Cql_Parser.K_NOSUPERUSER :
				case Cql_Parser.K_OPTIONS :
				case Cql_Parser.K_PARTITION :
				case Cql_Parser.K_PASSWORD :
				case Cql_Parser.K_PER :
				case Cql_Parser.K_PERMISSION :
				case Cql_Parser.K_PERMISSIONS :
				case Cql_Parser.K_RETURNS :
				case Cql_Parser.K_ROLE :
				case Cql_Parser.K_ROLES :
				case Cql_Parser.K_SFUNC :
				case Cql_Parser.K_SMALLINT :
				case Cql_Parser.K_STATIC :
				case Cql_Parser.K_STORAGE :
				case Cql_Parser.K_STYPE :
				case Cql_Parser.K_SUPERUSER :
				case Cql_Parser.K_TEXT :
				case Cql_Parser.K_TIME :
				case Cql_Parser.K_TIMESTAMP :
				case Cql_Parser.K_TIMEUUID :
				case Cql_Parser.K_TINYINT :
				case Cql_Parser.K_TRIGGER :
				case Cql_Parser.K_TTL :
				case Cql_Parser.K_TUPLE :
				case Cql_Parser.K_TYPE :
				case Cql_Parser.K_USER :
				case Cql_Parser.K_USERS :
				case Cql_Parser.K_UUID :
				case Cql_Parser.K_VARCHAR :
				case Cql_Parser.K_VARINT :
				case Cql_Parser.K_WRITETIME :
				case Cql_Parser.QUOTED_NAME :
					{
						alt94 = 1;
					}
					break;
				case Cql_Parser.K_VALUES :
					{
						int LA94_2 = input.LA(2);
						if (LA94_2 == 184) {
							alt94 = 2;
						}else
							if ((LA94_2 == 185) || (LA94_2 == 188)) {
								alt94 = 1;
							}else {
								int nvaeMark = input.mark();
								try {
									input.consume();
									NoViableAltException nvae = new NoViableAltException("", 94, 2, input);
									throw nvae;
								} finally {
									input.rewind(nvaeMark);
								}
							}

					}
					break;
				case Cql_Parser.K_KEYS :
					{
						int LA94_3 = input.LA(2);
						if (LA94_3 == 184) {
							alt94 = 3;
						}else
							if ((LA94_3 == 185) || (LA94_3 == 188)) {
								alt94 = 1;
							}else {
								int nvaeMark = input.mark();
								try {
									input.consume();
									NoViableAltException nvae = new NoViableAltException("", 94, 3, input);
									throw nvae;
								} finally {
									input.rewind(nvaeMark);
								}
							}

					}
					break;
				case Cql_Parser.K_ENTRIES :
					{
						alt94 = 4;
					}
					break;
				case Cql_Parser.K_FULL :
					{
						alt94 = 5;
					}
					break;
				default :
					NoViableAltException nvae = new NoViableAltException("", 94, 0, input);
					throw nvae;
			}
			switch (alt94) {
				case 1 :
					{
						pushFollow(Cql_Parser.FOLLOW_cident_in_indexIdent4483);
						c = cident();
						(state._fsp)--;
						targets.add(simpleIndexOn(c));
					}
					break;
				case 2 :
					{
						match(input, Cql_Parser.K_VALUES, Cql_Parser.FOLLOW_K_VALUES_in_indexIdent4511);
						match(input, 184, Cql_Parser.FOLLOW_184_in_indexIdent4513);
						pushFollow(Cql_Parser.FOLLOW_cident_in_indexIdent4517);
						c = cident();
						(state._fsp)--;
						match(input, 185, Cql_Parser.FOLLOW_185_in_indexIdent4519);
						targets.add(valuesOf(c));
					}
					break;
				case 3 :
					{
						match(input, Cql_Parser.K_KEYS, Cql_Parser.FOLLOW_K_KEYS_in_indexIdent4530);
						match(input, 184, Cql_Parser.FOLLOW_184_in_indexIdent4532);
						pushFollow(Cql_Parser.FOLLOW_cident_in_indexIdent4536);
						c = cident();
						(state._fsp)--;
						match(input, 185, Cql_Parser.FOLLOW_185_in_indexIdent4538);
						targets.add(keysOf(c));
					}
					break;
				case 4 :
					{
						match(input, Cql_Parser.K_ENTRIES, Cql_Parser.FOLLOW_K_ENTRIES_in_indexIdent4551);
						match(input, 184, Cql_Parser.FOLLOW_184_in_indexIdent4553);
						pushFollow(Cql_Parser.FOLLOW_cident_in_indexIdent4557);
						c = cident();
						(state._fsp)--;
						match(input, 185, Cql_Parser.FOLLOW_185_in_indexIdent4559);
						targets.add(keysAndValuesOf(c));
					}
					break;
				case 5 :
					{
						match(input, Cql_Parser.K_FULL, Cql_Parser.FOLLOW_K_FULL_in_indexIdent4569);
						match(input, 184, Cql_Parser.FOLLOW_184_in_indexIdent4571);
						pushFollow(Cql_Parser.FOLLOW_cident_in_indexIdent4575);
						c = cident();
						(state._fsp)--;
						match(input, 185, Cql_Parser.FOLLOW_185_in_indexIdent4577);
						targets.add(fullCollection(c));
					}
					break;
			}
		} catch (RecognitionException re) {
			reportError(re);
			recover(input, re);
		} finally {
		}
	}

	public final CreateViewStatement createMaterializedViewStatement() throws RecognitionException {
		CreateViewStatement expr = null;
		CFName cf = null;
		List<RawSelector> sclause = null;
		CFName basecf = null;
		WhereClause.Builder wclause = null;
		ColumnDefinition.Raw k1 = null;
		ColumnDefinition.Raw kn = null;
		ColumnDefinition.Raw c1 = null;
		ColumnDefinition.Raw cn = null;
		boolean ifNotExists = false;
		List<ColumnDefinition.Raw> partitionKeys = new ArrayList<>();
		List<ColumnDefinition.Raw> compositeKeys = new ArrayList<>();
		try {
			{
				match(input, Cql_Parser.K_CREATE, Cql_Parser.FOLLOW_K_CREATE_in_createMaterializedViewStatement4614);
				match(input, Cql_Parser.K_MATERIALIZED, Cql_Parser.FOLLOW_K_MATERIALIZED_in_createMaterializedViewStatement4616);
				match(input, Cql_Parser.K_VIEW, Cql_Parser.FOLLOW_K_VIEW_in_createMaterializedViewStatement4618);
				int alt95 = 2;
				int LA95_0 = input.LA(1);
				if (LA95_0 == (Cql_Parser.K_IF)) {
					alt95 = 1;
				}
				switch (alt95) {
					case 1 :
						{
							match(input, Cql_Parser.K_IF, Cql_Parser.FOLLOW_K_IF_in_createMaterializedViewStatement4621);
							match(input, Cql_Parser.K_NOT, Cql_Parser.FOLLOW_K_NOT_in_createMaterializedViewStatement4623);
							match(input, Cql_Parser.K_EXISTS, Cql_Parser.FOLLOW_K_EXISTS_in_createMaterializedViewStatement4625);
							ifNotExists = true;
						}
						break;
				}
				pushFollow(Cql_Parser.FOLLOW_columnFamilyName_in_createMaterializedViewStatement4633);
				cf = columnFamilyName();
				(state._fsp)--;
				match(input, Cql_Parser.K_AS, Cql_Parser.FOLLOW_K_AS_in_createMaterializedViewStatement4635);
				match(input, Cql_Parser.K_SELECT, Cql_Parser.FOLLOW_K_SELECT_in_createMaterializedViewStatement4645);
				pushFollow(Cql_Parser.FOLLOW_selectClause_in_createMaterializedViewStatement4649);
				sclause = selectClause();
				(state._fsp)--;
				match(input, Cql_Parser.K_FROM, Cql_Parser.FOLLOW_K_FROM_in_createMaterializedViewStatement4651);
				pushFollow(Cql_Parser.FOLLOW_columnFamilyName_in_createMaterializedViewStatement4655);
				basecf = columnFamilyName();
				(state._fsp)--;
				int alt96 = 2;
				int LA96_0 = input.LA(1);
				if (LA96_0 == (Cql_Parser.K_WHERE)) {
					alt96 = 1;
				}
				switch (alt96) {
					case 1 :
						{
							match(input, Cql_Parser.K_WHERE, Cql_Parser.FOLLOW_K_WHERE_in_createMaterializedViewStatement4666);
							pushFollow(Cql_Parser.FOLLOW_whereClause_in_createMaterializedViewStatement4670);
							wclause = whereClause();
							(state._fsp)--;
						}
						break;
				}
				match(input, Cql_Parser.K_PRIMARY, Cql_Parser.FOLLOW_K_PRIMARY_in_createMaterializedViewStatement4682);
				match(input, Cql_Parser.K_KEY, Cql_Parser.FOLLOW_K_KEY_in_createMaterializedViewStatement4684);
				int alt100 = 2;
				int LA100_0 = input.LA(1);
				if (LA100_0 == 184) {
					int LA100_1 = input.LA(2);
					if (LA100_1 == 184) {
						alt100 = 1;
					}else
						if ((((((((((((((((((((((((((((((((((LA100_1 == (Cql_Parser.EMPTY_QUOTED_NAME)) || (LA100_1 == (Cql_Parser.IDENT))) || ((LA100_1 >= (Cql_Parser.K_AGGREGATE)) && (LA100_1 <= (Cql_Parser.K_ALL)))) || (LA100_1 == (Cql_Parser.K_AS))) || (LA100_1 == (Cql_Parser.K_ASCII))) || ((LA100_1 >= (Cql_Parser.K_BIGINT)) && (LA100_1 <= (Cql_Parser.K_BOOLEAN)))) || ((LA100_1 >= (Cql_Parser.K_CALLED)) && (LA100_1 <= (Cql_Parser.K_CLUSTERING)))) || ((LA100_1 >= (Cql_Parser.K_COMPACT)) && (LA100_1 <= (Cql_Parser.K_COUNTER)))) || ((LA100_1 >= (Cql_Parser.K_CUSTOM)) && (LA100_1 <= (Cql_Parser.K_DECIMAL)))) || ((LA100_1 >= (Cql_Parser.K_DISTINCT)) && (LA100_1 <= (Cql_Parser.K_DOUBLE)))) || (LA100_1 == (Cql_Parser.K_DURATION))) || ((LA100_1 >= (Cql_Parser.K_EXISTS)) && (LA100_1 <= (Cql_Parser.K_FLOAT)))) || (LA100_1 == (Cql_Parser.K_FROZEN))) || ((LA100_1 >= (Cql_Parser.K_FUNCTION)) && (LA100_1 <= (Cql_Parser.K_FUNCTIONS)))) || (LA100_1 == (Cql_Parser.K_GROUP))) || (LA100_1 == (Cql_Parser.K_INET))) || ((LA100_1 >= (Cql_Parser.K_INITCOND)) && (LA100_1 <= (Cql_Parser.K_INPUT)))) || (LA100_1 == (Cql_Parser.K_INT))) || ((LA100_1 >= (Cql_Parser.K_JSON)) && (LA100_1 <= (Cql_Parser.K_KEYS)))) || ((LA100_1 >= (Cql_Parser.K_KEYSPACES)) && (LA100_1 <= (Cql_Parser.K_LIKE)))) || ((LA100_1 >= (Cql_Parser.K_LIST)) && (LA100_1 <= (Cql_Parser.K_MAP)))) || (LA100_1 == (Cql_Parser.K_NOLOGIN))) || (LA100_1 == (Cql_Parser.K_NOSUPERUSER))) || (LA100_1 == (Cql_Parser.K_OPTIONS))) || ((LA100_1 >= (Cql_Parser.K_PARTITION)) && (LA100_1 <= (Cql_Parser.K_PERMISSIONS)))) || (LA100_1 == (Cql_Parser.K_RETURNS))) || ((LA100_1 >= (Cql_Parser.K_ROLE)) && (LA100_1 <= (Cql_Parser.K_ROLES)))) || ((LA100_1 >= (Cql_Parser.K_SFUNC)) && (LA100_1 <= (Cql_Parser.K_TINYINT)))) || (LA100_1 == (Cql_Parser.K_TRIGGER))) || ((LA100_1 >= (Cql_Parser.K_TTL)) && (LA100_1 <= (Cql_Parser.K_TYPE)))) || ((LA100_1 >= (Cql_Parser.K_USER)) && (LA100_1 <= (Cql_Parser.K_USERS)))) || ((LA100_1 >= (Cql_Parser.K_UUID)) && (LA100_1 <= (Cql_Parser.K_VARINT)))) || (LA100_1 == (Cql_Parser.K_WRITETIME))) || (LA100_1 == (Cql_Parser.QUOTED_NAME))) {
							alt100 = 2;
						}else {
							int nvaeMark = input.mark();
							try {
								input.consume();
								NoViableAltException nvae = new NoViableAltException("", 100, 1, input);
								throw nvae;
							} finally {
								input.rewind(nvaeMark);
							}
						}

				}else {
					NoViableAltException nvae = new NoViableAltException("", 100, 0, input);
					throw nvae;
				}
				switch (alt100) {
					case 1 :
						{
							match(input, 184, Cql_Parser.FOLLOW_184_in_createMaterializedViewStatement4696);
							match(input, 184, Cql_Parser.FOLLOW_184_in_createMaterializedViewStatement4698);
							pushFollow(Cql_Parser.FOLLOW_cident_in_createMaterializedViewStatement4702);
							k1 = cident();
							(state._fsp)--;
							partitionKeys.add(k1);
							loop97 : while (true) {
								int alt97 = 2;
								int LA97_0 = input.LA(1);
								if (LA97_0 == 188) {
									alt97 = 1;
								}
								switch (alt97) {
									case 1 :
										{
											match(input, 188, Cql_Parser.FOLLOW_188_in_createMaterializedViewStatement4708);
											pushFollow(Cql_Parser.FOLLOW_cident_in_createMaterializedViewStatement4712);
											kn = cident();
											(state._fsp)--;
											partitionKeys.add(kn);
										}
										break;
									default :
										break loop97;
								}
							} 
							match(input, 185, Cql_Parser.FOLLOW_185_in_createMaterializedViewStatement4719);
							loop98 : while (true) {
								int alt98 = 2;
								int LA98_0 = input.LA(1);
								if (LA98_0 == 188) {
									alt98 = 1;
								}
								switch (alt98) {
									case 1 :
										{
											match(input, 188, Cql_Parser.FOLLOW_188_in_createMaterializedViewStatement4723);
											pushFollow(Cql_Parser.FOLLOW_cident_in_createMaterializedViewStatement4727);
											c1 = cident();
											(state._fsp)--;
											compositeKeys.add(c1);
										}
										break;
									default :
										break loop98;
								}
							} 
							match(input, 185, Cql_Parser.FOLLOW_185_in_createMaterializedViewStatement4734);
						}
						break;
					case 2 :
						{
							match(input, 184, Cql_Parser.FOLLOW_184_in_createMaterializedViewStatement4744);
							pushFollow(Cql_Parser.FOLLOW_cident_in_createMaterializedViewStatement4748);
							k1 = cident();
							(state._fsp)--;
							partitionKeys.add(k1);
							loop99 : while (true) {
								int alt99 = 2;
								int LA99_0 = input.LA(1);
								if (LA99_0 == 188) {
									alt99 = 1;
								}
								switch (alt99) {
									case 1 :
										{
											match(input, 188, Cql_Parser.FOLLOW_188_in_createMaterializedViewStatement4754);
											pushFollow(Cql_Parser.FOLLOW_cident_in_createMaterializedViewStatement4758);
											cn = cident();
											(state._fsp)--;
											compositeKeys.add(cn);
										}
										break;
									default :
										break loop99;
								}
							} 
							match(input, 185, Cql_Parser.FOLLOW_185_in_createMaterializedViewStatement4765);
						}
						break;
				}
				WhereClause where = (wclause == null) ? WhereClause.empty() : wclause.build();
				expr = new CreateViewStatement(cf, basecf, sclause, where, partitionKeys, compositeKeys, ifNotExists);
				int alt102 = 2;
				int LA102_0 = input.LA(1);
				if (LA102_0 == (Cql_Parser.K_WITH)) {
					alt102 = 1;
				}
				switch (alt102) {
					case 1 :
						{
							match(input, Cql_Parser.K_WITH, Cql_Parser.FOLLOW_K_WITH_in_createMaterializedViewStatement4797);
							pushFollow(Cql_Parser.FOLLOW_cfamProperty_in_createMaterializedViewStatement4799);
							cfamProperty(expr.properties);
							(state._fsp)--;
							loop101 : while (true) {
								int alt101 = 2;
								int LA101_0 = input.LA(1);
								if (LA101_0 == (Cql_Parser.K_AND)) {
									alt101 = 1;
								}
								switch (alt101) {
									case 1 :
										{
											match(input, Cql_Parser.K_AND, Cql_Parser.FOLLOW_K_AND_in_createMaterializedViewStatement4804);
											pushFollow(Cql_Parser.FOLLOW_cfamProperty_in_createMaterializedViewStatement4806);
											cfamProperty(expr.properties);
											(state._fsp)--;
										}
										break;
									default :
										break loop101;
								}
							} 
						}
						break;
				}
			}
		} catch (RecognitionException re) {
			reportError(re);
			recover(input, re);
		} finally {
		}
		return expr;
	}

	public final CreateTriggerStatement createTriggerStatement() throws RecognitionException {
		CreateTriggerStatement expr = null;
		Token cls = null;
		ColumnIdentifier name = null;
		CFName cf = null;
		boolean ifNotExists = false;
		try {
			{
				match(input, Cql_Parser.K_CREATE, Cql_Parser.FOLLOW_K_CREATE_in_createTriggerStatement4844);
				match(input, Cql_Parser.K_TRIGGER, Cql_Parser.FOLLOW_K_TRIGGER_in_createTriggerStatement4846);
				int alt103 = 2;
				int LA103_0 = input.LA(1);
				if (LA103_0 == (Cql_Parser.K_IF)) {
					alt103 = 1;
				}
				switch (alt103) {
					case 1 :
						{
							match(input, Cql_Parser.K_IF, Cql_Parser.FOLLOW_K_IF_in_createTriggerStatement4849);
							match(input, Cql_Parser.K_NOT, Cql_Parser.FOLLOW_K_NOT_in_createTriggerStatement4851);
							match(input, Cql_Parser.K_EXISTS, Cql_Parser.FOLLOW_K_EXISTS_in_createTriggerStatement4853);
							ifNotExists = true;
						}
						break;
				}
				{
					pushFollow(Cql_Parser.FOLLOW_ident_in_createTriggerStatement4863);
					name = ident();
					(state._fsp)--;
				}
				match(input, Cql_Parser.K_ON, Cql_Parser.FOLLOW_K_ON_in_createTriggerStatement4874);
				pushFollow(Cql_Parser.FOLLOW_columnFamilyName_in_createTriggerStatement4878);
				cf = columnFamilyName();
				(state._fsp)--;
				match(input, Cql_Parser.K_USING, Cql_Parser.FOLLOW_K_USING_in_createTriggerStatement4880);
				cls = ((Token) (match(input, Cql_Parser.STRING_LITERAL, Cql_Parser.FOLLOW_STRING_LITERAL_in_createTriggerStatement4884)));
				expr = new CreateTriggerStatement(cf, name.toString(), (cls != null ? cls.getText() : null), ifNotExists);
			}
		} catch (RecognitionException re) {
			reportError(re);
			recover(input, re);
		} finally {
		}
		return expr;
	}

	public final DropTriggerStatement dropTriggerStatement() throws RecognitionException {
		DropTriggerStatement expr = null;
		ColumnIdentifier name = null;
		CFName cf = null;
		boolean ifExists = false;
		try {
			{
				match(input, Cql_Parser.K_DROP, Cql_Parser.FOLLOW_K_DROP_in_dropTriggerStatement4925);
				match(input, Cql_Parser.K_TRIGGER, Cql_Parser.FOLLOW_K_TRIGGER_in_dropTriggerStatement4927);
				int alt104 = 2;
				int LA104_0 = input.LA(1);
				if (LA104_0 == (Cql_Parser.K_IF)) {
					alt104 = 1;
				}
				switch (alt104) {
					case 1 :
						{
							match(input, Cql_Parser.K_IF, Cql_Parser.FOLLOW_K_IF_in_dropTriggerStatement4930);
							match(input, Cql_Parser.K_EXISTS, Cql_Parser.FOLLOW_K_EXISTS_in_dropTriggerStatement4932);
							ifExists = true;
						}
						break;
				}
				{
					pushFollow(Cql_Parser.FOLLOW_ident_in_dropTriggerStatement4942);
					name = ident();
					(state._fsp)--;
				}
				match(input, Cql_Parser.K_ON, Cql_Parser.FOLLOW_K_ON_in_dropTriggerStatement4945);
				pushFollow(Cql_Parser.FOLLOW_columnFamilyName_in_dropTriggerStatement4949);
				cf = columnFamilyName();
				(state._fsp)--;
				expr = new DropTriggerStatement(cf, name.toString(), ifExists);
			}
		} catch (RecognitionException re) {
			reportError(re);
			recover(input, re);
		} finally {
		}
		return expr;
	}

	public final AlterKeyspaceStatement alterKeyspaceStatement() throws RecognitionException {
		AlterKeyspaceStatement expr = null;
		String ks = null;
		KeyspaceAttributes attrs = new KeyspaceAttributes();
		try {
			{
				match(input, Cql_Parser.K_ALTER, Cql_Parser.FOLLOW_K_ALTER_in_alterKeyspaceStatement4989);
				match(input, Cql_Parser.K_KEYSPACE, Cql_Parser.FOLLOW_K_KEYSPACE_in_alterKeyspaceStatement4991);
				pushFollow(Cql_Parser.FOLLOW_keyspaceName_in_alterKeyspaceStatement4995);
				ks = keyspaceName();
				(state._fsp)--;
				match(input, Cql_Parser.K_WITH, Cql_Parser.FOLLOW_K_WITH_in_alterKeyspaceStatement5005);
				pushFollow(Cql_Parser.FOLLOW_properties_in_alterKeyspaceStatement5007);
				properties(attrs);
				(state._fsp)--;
				expr = new AlterKeyspaceStatement(ks, attrs);
			}
		} catch (RecognitionException re) {
			reportError(re);
			recover(input, re);
		} finally {
		}
		return expr;
	}

	public final AlterTableStatement alterTableStatement() throws RecognitionException {
		AlterTableStatement expr = null;
		Token t = null;
		CFName cf = null;
		ColumnDefinition.Raw id = null;
		CQL3Type.Raw v = null;
		ColumnDefinition.Raw aid = null;
		boolean b1 = false;
		ColumnDefinition.Raw id1 = null;
		CQL3Type.Raw v1 = null;
		ColumnDefinition.Raw idn = null;
		CQL3Type.Raw vn = null;
		boolean bn = false;
		ColumnDefinition.Raw toId1 = null;
		ColumnDefinition.Raw toIdn = null;
		AlterTableStatement.Type type = null;
		TableAttributes attrs = new TableAttributes();
		Map<ColumnDefinition.Raw, ColumnDefinition.Raw> renames = new HashMap<ColumnDefinition.Raw, ColumnDefinition.Raw>();
		List<AlterTableStatementColumn> colNameList = new ArrayList<AlterTableStatementColumn>();
		Long deleteTimestamp = null;
		try {
			{
				match(input, Cql_Parser.K_ALTER, Cql_Parser.FOLLOW_K_ALTER_in_alterTableStatement5042);
				match(input, Cql_Parser.K_COLUMNFAMILY, Cql_Parser.FOLLOW_K_COLUMNFAMILY_in_alterTableStatement5044);
				pushFollow(Cql_Parser.FOLLOW_columnFamilyName_in_alterTableStatement5048);
				cf = columnFamilyName();
				(state._fsp)--;
				int alt111 = 6;
				switch (input.LA(1)) {
					case Cql_Parser.K_ALTER :
						{
							alt111 = 1;
						}
						break;
					case Cql_Parser.K_ADD :
						{
							alt111 = 2;
						}
						break;
					case Cql_Parser.K_DROP :
						{
							int LA111_3 = input.LA(2);
							if (LA111_3 == (Cql_Parser.K_COMPACT)) {
								int LA111_6 = input.LA(3);
								if (LA111_6 == (Cql_Parser.K_STORAGE)) {
									alt111 = 3;
								}else
									if (((LA111_6 == (Cql_Parser.EOF)) || (LA111_6 == (Cql_Parser.K_USING))) || (LA111_6 == 193)) {
										alt111 = 4;
									}else {
										int nvaeMark = input.mark();
										try {
											for (int nvaeConsume = 0; nvaeConsume < (3 - 1); nvaeConsume++) {
												input.consume();
											}
											NoViableAltException nvae = new NoViableAltException("", 111, 6, input);
											throw nvae;
										} finally {
											input.rewind(nvaeMark);
										}
									}

							}else
								if ((((((((((((((((((((((((((((((((((LA111_3 == (Cql_Parser.IDENT)) || ((LA111_3 >= (Cql_Parser.K_AGGREGATE)) && (LA111_3 <= (Cql_Parser.K_ALL)))) || (LA111_3 == (Cql_Parser.K_AS))) || (LA111_3 == (Cql_Parser.K_ASCII))) || ((LA111_3 >= (Cql_Parser.K_BIGINT)) && (LA111_3 <= (Cql_Parser.K_BOOLEAN)))) || ((LA111_3 >= (Cql_Parser.K_CALLED)) && (LA111_3 <= (Cql_Parser.K_CLUSTERING)))) || ((LA111_3 >= (Cql_Parser.K_CONTAINS)) && (LA111_3 <= (Cql_Parser.K_COUNTER)))) || ((LA111_3 >= (Cql_Parser.K_CUSTOM)) && (LA111_3 <= (Cql_Parser.K_DECIMAL)))) || ((LA111_3 >= (Cql_Parser.K_DISTINCT)) && (LA111_3 <= (Cql_Parser.K_DOUBLE)))) || (LA111_3 == (Cql_Parser.K_DURATION))) || ((LA111_3 >= (Cql_Parser.K_EXISTS)) && (LA111_3 <= (Cql_Parser.K_FLOAT)))) || (LA111_3 == (Cql_Parser.K_FROZEN))) || ((LA111_3 >= (Cql_Parser.K_FUNCTION)) && (LA111_3 <= (Cql_Parser.K_FUNCTIONS)))) || (LA111_3 == (Cql_Parser.K_GROUP))) || (LA111_3 == (Cql_Parser.K_INET))) || ((LA111_3 >= (Cql_Parser.K_INITCOND)) && (LA111_3 <= (Cql_Parser.K_INPUT)))) || (LA111_3 == (Cql_Parser.K_INT))) || ((LA111_3 >= (Cql_Parser.K_JSON)) && (LA111_3 <= (Cql_Parser.K_KEYS)))) || ((LA111_3 >= (Cql_Parser.K_KEYSPACES)) && (LA111_3 <= (Cql_Parser.K_LIKE)))) || ((LA111_3 >= (Cql_Parser.K_LIST)) && (LA111_3 <= (Cql_Parser.K_MAP)))) || (LA111_3 == (Cql_Parser.K_NOLOGIN))) || (LA111_3 == (Cql_Parser.K_NOSUPERUSER))) || (LA111_3 == (Cql_Parser.K_OPTIONS))) || ((LA111_3 >= (Cql_Parser.K_PARTITION)) && (LA111_3 <= (Cql_Parser.K_PERMISSIONS)))) || (LA111_3 == (Cql_Parser.K_RETURNS))) || ((LA111_3 >= (Cql_Parser.K_ROLE)) && (LA111_3 <= (Cql_Parser.K_ROLES)))) || ((LA111_3 >= (Cql_Parser.K_SFUNC)) && (LA111_3 <= (Cql_Parser.K_TINYINT)))) || (LA111_3 == (Cql_Parser.K_TRIGGER))) || ((LA111_3 >= (Cql_Parser.K_TTL)) && (LA111_3 <= (Cql_Parser.K_TYPE)))) || ((LA111_3 >= (Cql_Parser.K_USER)) && (LA111_3 <= (Cql_Parser.K_USERS)))) || ((LA111_3 >= (Cql_Parser.K_UUID)) && (LA111_3 <= (Cql_Parser.K_VARINT)))) || (LA111_3 == (Cql_Parser.K_WRITETIME))) || (LA111_3 == (Cql_Parser.QUOTED_NAME))) || (LA111_3 == 184)) {
									alt111 = 4;
								}else {
									int nvaeMark = input.mark();
									try {
										input.consume();
										NoViableAltException nvae = new NoViableAltException("", 111, 3, input);
										throw nvae;
									} finally {
										input.rewind(nvaeMark);
									}
								}

						}
						break;
					case Cql_Parser.K_WITH :
						{
							alt111 = 5;
						}
						break;
					case Cql_Parser.K_RENAME :
						{
							alt111 = 6;
						}
						break;
					default :
						NoViableAltException nvae = new NoViableAltException("", 111, 0, input);
						throw nvae;
				}
				switch (alt111) {
					case 1 :
						{
							match(input, Cql_Parser.K_ALTER, Cql_Parser.FOLLOW_K_ALTER_in_alterTableStatement5062);
							pushFollow(Cql_Parser.FOLLOW_schema_cident_in_alterTableStatement5066);
							id = schema_cident();
							(state._fsp)--;
							match(input, Cql_Parser.K_TYPE, Cql_Parser.FOLLOW_K_TYPE_in_alterTableStatement5069);
							pushFollow(Cql_Parser.FOLLOW_comparatorType_in_alterTableStatement5073);
							v = comparatorType();
							(state._fsp)--;
							type = ALTER;
							colNameList.add(new AlterTableStatementColumn(id, v));
						}
						break;
					case 2 :
						{
							match(input, Cql_Parser.K_ADD, Cql_Parser.FOLLOW_K_ADD_in_alterTableStatement5092);
							int alt106 = 2;
							int LA106_0 = input.LA(1);
							if (((((((((((((((((((((((((((((((((LA106_0 == (Cql_Parser.IDENT)) || ((LA106_0 >= (Cql_Parser.K_AGGREGATE)) && (LA106_0 <= (Cql_Parser.K_ALL)))) || (LA106_0 == (Cql_Parser.K_AS))) || (LA106_0 == (Cql_Parser.K_ASCII))) || ((LA106_0 >= (Cql_Parser.K_BIGINT)) && (LA106_0 <= (Cql_Parser.K_BOOLEAN)))) || ((LA106_0 >= (Cql_Parser.K_CALLED)) && (LA106_0 <= (Cql_Parser.K_CLUSTERING)))) || ((LA106_0 >= (Cql_Parser.K_COMPACT)) && (LA106_0 <= (Cql_Parser.K_COUNTER)))) || ((LA106_0 >= (Cql_Parser.K_CUSTOM)) && (LA106_0 <= (Cql_Parser.K_DECIMAL)))) || ((LA106_0 >= (Cql_Parser.K_DISTINCT)) && (LA106_0 <= (Cql_Parser.K_DOUBLE)))) || (LA106_0 == (Cql_Parser.K_DURATION))) || ((LA106_0 >= (Cql_Parser.K_EXISTS)) && (LA106_0 <= (Cql_Parser.K_FLOAT)))) || (LA106_0 == (Cql_Parser.K_FROZEN))) || ((LA106_0 >= (Cql_Parser.K_FUNCTION)) && (LA106_0 <= (Cql_Parser.K_FUNCTIONS)))) || (LA106_0 == (Cql_Parser.K_GROUP))) || (LA106_0 == (Cql_Parser.K_INET))) || ((LA106_0 >= (Cql_Parser.K_INITCOND)) && (LA106_0 <= (Cql_Parser.K_INPUT)))) || (LA106_0 == (Cql_Parser.K_INT))) || ((LA106_0 >= (Cql_Parser.K_JSON)) && (LA106_0 <= (Cql_Parser.K_KEYS)))) || ((LA106_0 >= (Cql_Parser.K_KEYSPACES)) && (LA106_0 <= (Cql_Parser.K_LIKE)))) || ((LA106_0 >= (Cql_Parser.K_LIST)) && (LA106_0 <= (Cql_Parser.K_MAP)))) || (LA106_0 == (Cql_Parser.K_NOLOGIN))) || (LA106_0 == (Cql_Parser.K_NOSUPERUSER))) || (LA106_0 == (Cql_Parser.K_OPTIONS))) || ((LA106_0 >= (Cql_Parser.K_PARTITION)) && (LA106_0 <= (Cql_Parser.K_PERMISSIONS)))) || (LA106_0 == (Cql_Parser.K_RETURNS))) || ((LA106_0 >= (Cql_Parser.K_ROLE)) && (LA106_0 <= (Cql_Parser.K_ROLES)))) || ((LA106_0 >= (Cql_Parser.K_SFUNC)) && (LA106_0 <= (Cql_Parser.K_TINYINT)))) || (LA106_0 == (Cql_Parser.K_TRIGGER))) || ((LA106_0 >= (Cql_Parser.K_TTL)) && (LA106_0 <= (Cql_Parser.K_TYPE)))) || ((LA106_0 >= (Cql_Parser.K_USER)) && (LA106_0 <= (Cql_Parser.K_USERS)))) || ((LA106_0 >= (Cql_Parser.K_UUID)) && (LA106_0 <= (Cql_Parser.K_VARINT)))) || (LA106_0 == (Cql_Parser.K_WRITETIME))) || (LA106_0 == (Cql_Parser.QUOTED_NAME))) {
								alt106 = 1;
							}else
								if (LA106_0 == 184) {
									alt106 = 2;
								}else {
									NoViableAltException nvae = new NoViableAltException("", 106, 0, input);
									throw nvae;
								}

							switch (alt106) {
								case 1 :
									{
										{
											pushFollow(Cql_Parser.FOLLOW_schema_cident_in_alterTableStatement5107);
											aid = schema_cident();
											(state._fsp)--;
											pushFollow(Cql_Parser.FOLLOW_comparatorType_in_alterTableStatement5112);
											v = comparatorType();
											(state._fsp)--;
											pushFollow(Cql_Parser.FOLLOW_cfisStatic_in_alterTableStatement5118);
											b1 = cfisStatic();
											(state._fsp)--;
											colNameList.add(new AlterTableStatementColumn(aid, v, b1));
										}
									}
									break;
								case 2 :
									{
										{
											match(input, 184, Cql_Parser.FOLLOW_184_in_alterTableStatement5147);
											pushFollow(Cql_Parser.FOLLOW_schema_cident_in_alterTableStatement5152);
											id1 = schema_cident();
											(state._fsp)--;
											pushFollow(Cql_Parser.FOLLOW_comparatorType_in_alterTableStatement5157);
											v1 = comparatorType();
											(state._fsp)--;
											pushFollow(Cql_Parser.FOLLOW_cfisStatic_in_alterTableStatement5162);
											b1 = cfisStatic();
											(state._fsp)--;
											colNameList.add(new AlterTableStatementColumn(id1, v1, b1));
											loop105 : while (true) {
												int alt105 = 2;
												int LA105_0 = input.LA(1);
												if (LA105_0 == 188) {
													alt105 = 1;
												}
												switch (alt105) {
													case 1 :
														{
															match(input, 188, Cql_Parser.FOLLOW_188_in_alterTableStatement5191);
															pushFollow(Cql_Parser.FOLLOW_schema_cident_in_alterTableStatement5195);
															idn = schema_cident();
															(state._fsp)--;
															pushFollow(Cql_Parser.FOLLOW_comparatorType_in_alterTableStatement5200);
															vn = comparatorType();
															(state._fsp)--;
															pushFollow(Cql_Parser.FOLLOW_cfisStatic_in_alterTableStatement5205);
															bn = cfisStatic();
															(state._fsp)--;
															colNameList.add(new AlterTableStatementColumn(idn, vn, bn));
														}
														break;
													default :
														break loop105;
												}
											} 
											match(input, 185, Cql_Parser.FOLLOW_185_in_alterTableStatement5212);
										}
									}
									break;
							}
							type = ADD;
						}
						break;
					case 3 :
						{
							match(input, Cql_Parser.K_DROP, Cql_Parser.FOLLOW_K_DROP_in_alterTableStatement5232);
							match(input, Cql_Parser.K_COMPACT, Cql_Parser.FOLLOW_K_COMPACT_in_alterTableStatement5234);
							match(input, Cql_Parser.K_STORAGE, Cql_Parser.FOLLOW_K_STORAGE_in_alterTableStatement5236);
							type = DROP_COMPACT_STORAGE;
						}
						break;
					case 4 :
						{
							match(input, Cql_Parser.K_DROP, Cql_Parser.FOLLOW_K_DROP_in_alterTableStatement5269);
							{
								int alt108 = 2;
								int LA108_0 = input.LA(1);
								if (((((((((((((((((((((((((((((((((LA108_0 == (Cql_Parser.IDENT)) || ((LA108_0 >= (Cql_Parser.K_AGGREGATE)) && (LA108_0 <= (Cql_Parser.K_ALL)))) || (LA108_0 == (Cql_Parser.K_AS))) || (LA108_0 == (Cql_Parser.K_ASCII))) || ((LA108_0 >= (Cql_Parser.K_BIGINT)) && (LA108_0 <= (Cql_Parser.K_BOOLEAN)))) || ((LA108_0 >= (Cql_Parser.K_CALLED)) && (LA108_0 <= (Cql_Parser.K_CLUSTERING)))) || ((LA108_0 >= (Cql_Parser.K_COMPACT)) && (LA108_0 <= (Cql_Parser.K_COUNTER)))) || ((LA108_0 >= (Cql_Parser.K_CUSTOM)) && (LA108_0 <= (Cql_Parser.K_DECIMAL)))) || ((LA108_0 >= (Cql_Parser.K_DISTINCT)) && (LA108_0 <= (Cql_Parser.K_DOUBLE)))) || (LA108_0 == (Cql_Parser.K_DURATION))) || ((LA108_0 >= (Cql_Parser.K_EXISTS)) && (LA108_0 <= (Cql_Parser.K_FLOAT)))) || (LA108_0 == (Cql_Parser.K_FROZEN))) || ((LA108_0 >= (Cql_Parser.K_FUNCTION)) && (LA108_0 <= (Cql_Parser.K_FUNCTIONS)))) || (LA108_0 == (Cql_Parser.K_GROUP))) || (LA108_0 == (Cql_Parser.K_INET))) || ((LA108_0 >= (Cql_Parser.K_INITCOND)) && (LA108_0 <= (Cql_Parser.K_INPUT)))) || (LA108_0 == (Cql_Parser.K_INT))) || ((LA108_0 >= (Cql_Parser.K_JSON)) && (LA108_0 <= (Cql_Parser.K_KEYS)))) || ((LA108_0 >= (Cql_Parser.K_KEYSPACES)) && (LA108_0 <= (Cql_Parser.K_LIKE)))) || ((LA108_0 >= (Cql_Parser.K_LIST)) && (LA108_0 <= (Cql_Parser.K_MAP)))) || (LA108_0 == (Cql_Parser.K_NOLOGIN))) || (LA108_0 == (Cql_Parser.K_NOSUPERUSER))) || (LA108_0 == (Cql_Parser.K_OPTIONS))) || ((LA108_0 >= (Cql_Parser.K_PARTITION)) && (LA108_0 <= (Cql_Parser.K_PERMISSIONS)))) || (LA108_0 == (Cql_Parser.K_RETURNS))) || ((LA108_0 >= (Cql_Parser.K_ROLE)) && (LA108_0 <= (Cql_Parser.K_ROLES)))) || ((LA108_0 >= (Cql_Parser.K_SFUNC)) && (LA108_0 <= (Cql_Parser.K_TINYINT)))) || (LA108_0 == (Cql_Parser.K_TRIGGER))) || ((LA108_0 >= (Cql_Parser.K_TTL)) && (LA108_0 <= (Cql_Parser.K_TYPE)))) || ((LA108_0 >= (Cql_Parser.K_USER)) && (LA108_0 <= (Cql_Parser.K_USERS)))) || ((LA108_0 >= (Cql_Parser.K_UUID)) && (LA108_0 <= (Cql_Parser.K_VARINT)))) || (LA108_0 == (Cql_Parser.K_WRITETIME))) || (LA108_0 == (Cql_Parser.QUOTED_NAME))) {
									alt108 = 1;
								}else
									if (LA108_0 == 184) {
										alt108 = 2;
									}else {
										NoViableAltException nvae = new NoViableAltException("", 108, 0, input);
										throw nvae;
									}

								switch (alt108) {
									case 1 :
										{
											pushFollow(Cql_Parser.FOLLOW_schema_cident_in_alterTableStatement5284);
											id = schema_cident();
											(state._fsp)--;
											colNameList.add(new AlterTableStatementColumn(id));
										}
										break;
									case 2 :
										{
											{
												match(input, 184, Cql_Parser.FOLLOW_184_in_alterTableStatement5314);
												pushFollow(Cql_Parser.FOLLOW_schema_cident_in_alterTableStatement5319);
												id1 = schema_cident();
												(state._fsp)--;
												colNameList.add(new AlterTableStatementColumn(id1));
												loop107 : while (true) {
													int alt107 = 2;
													int LA107_0 = input.LA(1);
													if (LA107_0 == 188) {
														alt107 = 1;
													}
													switch (alt107) {
														case 1 :
															{
																match(input, 188, Cql_Parser.FOLLOW_188_in_alterTableStatement5349);
																pushFollow(Cql_Parser.FOLLOW_schema_cident_in_alterTableStatement5353);
																idn = schema_cident();
																(state._fsp)--;
																colNameList.add(new AlterTableStatementColumn(idn));
															}
															break;
														default :
															break loop107;
													}
												} 
												match(input, 185, Cql_Parser.FOLLOW_185_in_alterTableStatement5360);
											}
										}
										break;
								}
								int alt109 = 2;
								int LA109_0 = input.LA(1);
								if (LA109_0 == (Cql_Parser.K_USING)) {
									alt109 = 1;
								}
								switch (alt109) {
									case 1 :
										{
											match(input, Cql_Parser.K_USING, Cql_Parser.FOLLOW_K_USING_in_alterTableStatement5388);
											match(input, Cql_Parser.K_TIMESTAMP, Cql_Parser.FOLLOW_K_TIMESTAMP_in_alterTableStatement5390);
											t = ((Token) (match(input, Cql_Parser.INTEGER, Cql_Parser.FOLLOW_INTEGER_in_alterTableStatement5394)));
											deleteTimestamp = Long.parseLong(integer((t != null ? t.getText() : null)).getText());
										}
										break;
								}
							}
							type = DROP;
						}
						break;
					case 5 :
						{
							match(input, Cql_Parser.K_WITH, Cql_Parser.FOLLOW_K_WITH_in_alterTableStatement5416);
							pushFollow(Cql_Parser.FOLLOW_properties_in_alterTableStatement5419);
							properties(attrs);
							(state._fsp)--;
							type = OPTS;
						}
						break;
					case 6 :
						{
							match(input, Cql_Parser.K_RENAME, Cql_Parser.FOLLOW_K_RENAME_in_alterTableStatement5452);
							type = RENAME;
							pushFollow(Cql_Parser.FOLLOW_schema_cident_in_alterTableStatement5506);
							id1 = schema_cident();
							(state._fsp)--;
							match(input, Cql_Parser.K_TO, Cql_Parser.FOLLOW_K_TO_in_alterTableStatement5508);
							pushFollow(Cql_Parser.FOLLOW_schema_cident_in_alterTableStatement5512);
							toId1 = schema_cident();
							(state._fsp)--;
							renames.put(id1, toId1);
							loop110 : while (true) {
								int alt110 = 2;
								int LA110_0 = input.LA(1);
								if (LA110_0 == (Cql_Parser.K_AND)) {
									alt110 = 1;
								}
								switch (alt110) {
									case 1 :
										{
											match(input, Cql_Parser.K_AND, Cql_Parser.FOLLOW_K_AND_in_alterTableStatement5533);
											pushFollow(Cql_Parser.FOLLOW_schema_cident_in_alterTableStatement5537);
											idn = schema_cident();
											(state._fsp)--;
											match(input, Cql_Parser.K_TO, Cql_Parser.FOLLOW_K_TO_in_alterTableStatement5539);
											pushFollow(Cql_Parser.FOLLOW_schema_cident_in_alterTableStatement5543);
											toIdn = schema_cident();
											(state._fsp)--;
											renames.put(idn, toIdn);
										}
										break;
									default :
										break loop110;
								}
							} 
						}
						break;
				}
				expr = new AlterTableStatement(cf, type, colNameList, attrs, renames, deleteTimestamp);
			}
		} catch (RecognitionException re) {
			reportError(re);
			recover(input, re);
		} finally {
		}
		return expr;
	}

	public final boolean cfisStatic() throws RecognitionException {
		boolean isStaticColumn = false;
		boolean isStatic = false;
		try {
			{
				int alt112 = 2;
				int LA112_0 = input.LA(1);
				if (LA112_0 == (Cql_Parser.K_STATIC)) {
					alt112 = 1;
				}
				switch (alt112) {
					case 1 :
						{
							match(input, Cql_Parser.K_STATIC, Cql_Parser.FOLLOW_K_STATIC_in_cfisStatic5596);
							isStatic = true;
						}
						break;
				}
				isStaticColumn = isStatic;
			}
		} catch (RecognitionException re) {
			reportError(re);
			recover(input, re);
		} finally {
		}
		return isStaticColumn;
	}

	public final AlterViewStatement alterMaterializedViewStatement() throws RecognitionException {
		AlterViewStatement expr = null;
		CFName name = null;
		TableAttributes attrs = new TableAttributes();
		try {
			{
				match(input, Cql_Parser.K_ALTER, Cql_Parser.FOLLOW_K_ALTER_in_alterMaterializedViewStatement5632);
				match(input, Cql_Parser.K_MATERIALIZED, Cql_Parser.FOLLOW_K_MATERIALIZED_in_alterMaterializedViewStatement5634);
				match(input, Cql_Parser.K_VIEW, Cql_Parser.FOLLOW_K_VIEW_in_alterMaterializedViewStatement5636);
				pushFollow(Cql_Parser.FOLLOW_columnFamilyName_in_alterMaterializedViewStatement5640);
				name = columnFamilyName();
				(state._fsp)--;
				match(input, Cql_Parser.K_WITH, Cql_Parser.FOLLOW_K_WITH_in_alterMaterializedViewStatement5652);
				pushFollow(Cql_Parser.FOLLOW_properties_in_alterMaterializedViewStatement5654);
				properties(attrs);
				(state._fsp)--;
				expr = new AlterViewStatement(name, attrs);
			}
		} catch (RecognitionException re) {
			reportError(re);
			recover(input, re);
		} finally {
		}
		return expr;
	}

	public final AlterTypeStatement alterTypeStatement() throws RecognitionException {
		AlterTypeStatement expr = null;
		UTName name = null;
		FieldIdentifier f = null;
		CQL3Type.Raw v = null;
		FieldIdentifier id1 = null;
		FieldIdentifier toId1 = null;
		FieldIdentifier idn = null;
		FieldIdentifier toIdn = null;
		try {
			{
				match(input, Cql_Parser.K_ALTER, Cql_Parser.FOLLOW_K_ALTER_in_alterTypeStatement5685);
				match(input, Cql_Parser.K_TYPE, Cql_Parser.FOLLOW_K_TYPE_in_alterTypeStatement5687);
				pushFollow(Cql_Parser.FOLLOW_userTypeName_in_alterTypeStatement5691);
				name = userTypeName();
				(state._fsp)--;
				int alt114 = 3;
				switch (input.LA(1)) {
					case Cql_Parser.K_ALTER :
						{
							alt114 = 1;
						}
						break;
					case Cql_Parser.K_ADD :
						{
							alt114 = 2;
						}
						break;
					case Cql_Parser.K_RENAME :
						{
							alt114 = 3;
						}
						break;
					default :
						NoViableAltException nvae = new NoViableAltException("", 114, 0, input);
						throw nvae;
				}
				switch (alt114) {
					case 1 :
						{
							match(input, Cql_Parser.K_ALTER, Cql_Parser.FOLLOW_K_ALTER_in_alterTypeStatement5705);
							pushFollow(Cql_Parser.FOLLOW_fident_in_alterTypeStatement5709);
							f = fident();
							(state._fsp)--;
							match(input, Cql_Parser.K_TYPE, Cql_Parser.FOLLOW_K_TYPE_in_alterTypeStatement5711);
							pushFollow(Cql_Parser.FOLLOW_comparatorType_in_alterTypeStatement5715);
							v = comparatorType();
							(state._fsp)--;
							expr = AlterTypeStatement.alter(name, f, v);
						}
						break;
					case 2 :
						{
							match(input, Cql_Parser.K_ADD, Cql_Parser.FOLLOW_K_ADD_in_alterTypeStatement5731);
							pushFollow(Cql_Parser.FOLLOW_fident_in_alterTypeStatement5737);
							f = fident();
							(state._fsp)--;
							pushFollow(Cql_Parser.FOLLOW_comparatorType_in_alterTypeStatement5741);
							v = comparatorType();
							(state._fsp)--;
							expr = AlterTypeStatement.addition(name, f, v);
						}
						break;
					case 3 :
						{
							match(input, Cql_Parser.K_RENAME, Cql_Parser.FOLLOW_K_RENAME_in_alterTypeStatement5764);
							Map<FieldIdentifier, FieldIdentifier> renames = new HashMap<>();
							pushFollow(Cql_Parser.FOLLOW_fident_in_alterTypeStatement5802);
							id1 = fident();
							(state._fsp)--;
							match(input, Cql_Parser.K_TO, Cql_Parser.FOLLOW_K_TO_in_alterTypeStatement5804);
							pushFollow(Cql_Parser.FOLLOW_fident_in_alterTypeStatement5808);
							toId1 = fident();
							(state._fsp)--;
							renames.put(id1, toId1);
							loop113 : while (true) {
								int alt113 = 2;
								int LA113_0 = input.LA(1);
								if (LA113_0 == (Cql_Parser.K_AND)) {
									alt113 = 1;
								}
								switch (alt113) {
									case 1 :
										{
											match(input, Cql_Parser.K_AND, Cql_Parser.FOLLOW_K_AND_in_alterTypeStatement5831);
											pushFollow(Cql_Parser.FOLLOW_fident_in_alterTypeStatement5835);
											idn = fident();
											(state._fsp)--;
											match(input, Cql_Parser.K_TO, Cql_Parser.FOLLOW_K_TO_in_alterTypeStatement5837);
											pushFollow(Cql_Parser.FOLLOW_fident_in_alterTypeStatement5841);
											toIdn = fident();
											(state._fsp)--;
											renames.put(idn, toIdn);
										}
										break;
									default :
										break loop113;
								}
							} 
							expr = AlterTypeStatement.renames(name, renames);
						}
						break;
				}
			}
		} catch (RecognitionException re) {
			reportError(re);
			recover(input, re);
		} finally {
		}
		return expr;
	}

	public final DropKeyspaceStatement dropKeyspaceStatement() throws RecognitionException {
		DropKeyspaceStatement ksp = null;
		String ks = null;
		boolean ifExists = false;
		try {
			{
				match(input, Cql_Parser.K_DROP, Cql_Parser.FOLLOW_K_DROP_in_dropKeyspaceStatement5908);
				match(input, Cql_Parser.K_KEYSPACE, Cql_Parser.FOLLOW_K_KEYSPACE_in_dropKeyspaceStatement5910);
				int alt115 = 2;
				int LA115_0 = input.LA(1);
				if (LA115_0 == (Cql_Parser.K_IF)) {
					alt115 = 1;
				}
				switch (alt115) {
					case 1 :
						{
							match(input, Cql_Parser.K_IF, Cql_Parser.FOLLOW_K_IF_in_dropKeyspaceStatement5913);
							match(input, Cql_Parser.K_EXISTS, Cql_Parser.FOLLOW_K_EXISTS_in_dropKeyspaceStatement5915);
							ifExists = true;
						}
						break;
				}
				pushFollow(Cql_Parser.FOLLOW_keyspaceName_in_dropKeyspaceStatement5924);
				ks = keyspaceName();
				(state._fsp)--;
				ksp = new DropKeyspaceStatement(ks, ifExists);
			}
		} catch (RecognitionException re) {
			reportError(re);
			recover(input, re);
		} finally {
		}
		return ksp;
	}

	public final DropTableStatement dropTableStatement() throws RecognitionException {
		DropTableStatement stmt = null;
		CFName cf = null;
		boolean ifExists = false;
		try {
			{
				match(input, Cql_Parser.K_DROP, Cql_Parser.FOLLOW_K_DROP_in_dropTableStatement5958);
				match(input, Cql_Parser.K_COLUMNFAMILY, Cql_Parser.FOLLOW_K_COLUMNFAMILY_in_dropTableStatement5960);
				int alt116 = 2;
				int LA116_0 = input.LA(1);
				if (LA116_0 == (Cql_Parser.K_IF)) {
					alt116 = 1;
				}
				switch (alt116) {
					case 1 :
						{
							match(input, Cql_Parser.K_IF, Cql_Parser.FOLLOW_K_IF_in_dropTableStatement5963);
							match(input, Cql_Parser.K_EXISTS, Cql_Parser.FOLLOW_K_EXISTS_in_dropTableStatement5965);
							ifExists = true;
						}
						break;
				}
				pushFollow(Cql_Parser.FOLLOW_columnFamilyName_in_dropTableStatement5974);
				cf = columnFamilyName();
				(state._fsp)--;
				stmt = new DropTableStatement(cf, ifExists);
			}
		} catch (RecognitionException re) {
			reportError(re);
			recover(input, re);
		} finally {
		}
		return stmt;
	}

	public final DropTypeStatement dropTypeStatement() throws RecognitionException {
		DropTypeStatement stmt = null;
		UTName name = null;
		boolean ifExists = false;
		try {
			{
				match(input, Cql_Parser.K_DROP, Cql_Parser.FOLLOW_K_DROP_in_dropTypeStatement6008);
				match(input, Cql_Parser.K_TYPE, Cql_Parser.FOLLOW_K_TYPE_in_dropTypeStatement6010);
				int alt117 = 2;
				int LA117_0 = input.LA(1);
				if (LA117_0 == (Cql_Parser.K_IF)) {
					alt117 = 1;
				}
				switch (alt117) {
					case 1 :
						{
							match(input, Cql_Parser.K_IF, Cql_Parser.FOLLOW_K_IF_in_dropTypeStatement6013);
							match(input, Cql_Parser.K_EXISTS, Cql_Parser.FOLLOW_K_EXISTS_in_dropTypeStatement6015);
							ifExists = true;
						}
						break;
				}
				pushFollow(Cql_Parser.FOLLOW_userTypeName_in_dropTypeStatement6024);
				name = userTypeName();
				(state._fsp)--;
				stmt = new DropTypeStatement(name, ifExists);
			}
		} catch (RecognitionException re) {
			reportError(re);
			recover(input, re);
		} finally {
		}
		return stmt;
	}

	public final DropIndexStatement dropIndexStatement() throws RecognitionException {
		DropIndexStatement expr = null;
		IndexName index = null;
		boolean ifExists = false;
		try {
			{
				match(input, Cql_Parser.K_DROP, Cql_Parser.FOLLOW_K_DROP_in_dropIndexStatement6058);
				match(input, Cql_Parser.K_INDEX, Cql_Parser.FOLLOW_K_INDEX_in_dropIndexStatement6060);
				int alt118 = 2;
				int LA118_0 = input.LA(1);
				if (LA118_0 == (Cql_Parser.K_IF)) {
					alt118 = 1;
				}
				switch (alt118) {
					case 1 :
						{
							match(input, Cql_Parser.K_IF, Cql_Parser.FOLLOW_K_IF_in_dropIndexStatement6063);
							match(input, Cql_Parser.K_EXISTS, Cql_Parser.FOLLOW_K_EXISTS_in_dropIndexStatement6065);
							ifExists = true;
						}
						break;
				}
				pushFollow(Cql_Parser.FOLLOW_indexName_in_dropIndexStatement6074);
				index = indexName();
				(state._fsp)--;
				expr = new DropIndexStatement(index, ifExists);
			}
		} catch (RecognitionException re) {
			reportError(re);
			recover(input, re);
		} finally {
		}
		return expr;
	}

	public final DropViewStatement dropMaterializedViewStatement() throws RecognitionException {
		DropViewStatement expr = null;
		CFName cf = null;
		boolean ifExists = false;
		try {
			{
				match(input, Cql_Parser.K_DROP, Cql_Parser.FOLLOW_K_DROP_in_dropMaterializedViewStatement6114);
				match(input, Cql_Parser.K_MATERIALIZED, Cql_Parser.FOLLOW_K_MATERIALIZED_in_dropMaterializedViewStatement6116);
				match(input, Cql_Parser.K_VIEW, Cql_Parser.FOLLOW_K_VIEW_in_dropMaterializedViewStatement6118);
				int alt119 = 2;
				int LA119_0 = input.LA(1);
				if (LA119_0 == (Cql_Parser.K_IF)) {
					alt119 = 1;
				}
				switch (alt119) {
					case 1 :
						{
							match(input, Cql_Parser.K_IF, Cql_Parser.FOLLOW_K_IF_in_dropMaterializedViewStatement6121);
							match(input, Cql_Parser.K_EXISTS, Cql_Parser.FOLLOW_K_EXISTS_in_dropMaterializedViewStatement6123);
							ifExists = true;
						}
						break;
				}
				pushFollow(Cql_Parser.FOLLOW_columnFamilyName_in_dropMaterializedViewStatement6132);
				cf = columnFamilyName();
				(state._fsp)--;
				expr = new DropViewStatement(cf, ifExists);
			}
		} catch (RecognitionException re) {
			reportError(re);
			recover(input, re);
		} finally {
		}
		return expr;
	}

	public final TruncateStatement truncateStatement() throws RecognitionException {
		TruncateStatement stmt = null;
		CFName cf = null;
		try {
			{
				match(input, Cql_Parser.K_TRUNCATE, Cql_Parser.FOLLOW_K_TRUNCATE_in_truncateStatement6163);
				int alt120 = 2;
				int LA120_0 = input.LA(1);
				if (LA120_0 == (Cql_Parser.K_COLUMNFAMILY)) {
					alt120 = 1;
				}
				switch (alt120) {
					case 1 :
						{
							match(input, Cql_Parser.K_COLUMNFAMILY, Cql_Parser.FOLLOW_K_COLUMNFAMILY_in_truncateStatement6166);
						}
						break;
				}
				pushFollow(Cql_Parser.FOLLOW_columnFamilyName_in_truncateStatement6172);
				cf = columnFamilyName();
				(state._fsp)--;
				stmt = new TruncateStatement(cf);
			}
		} catch (RecognitionException re) {
			reportError(re);
			recover(input, re);
		} finally {
		}
		return stmt;
	}

	public final GrantPermissionsStatement grantPermissionsStatement() throws RecognitionException {
		GrantPermissionsStatement stmt = null;
		RoleName grantee = null;
		Set<Permission> permissionOrAll1 = null;
		IResource resource2 = null;
		try {
			{
				match(input, Cql_Parser.K_GRANT, Cql_Parser.FOLLOW_K_GRANT_in_grantPermissionsStatement6197);
				pushFollow(Cql_Parser.FOLLOW_permissionOrAll_in_grantPermissionsStatement6209);
				permissionOrAll1 = permissionOrAll();
				(state._fsp)--;
				match(input, Cql_Parser.K_ON, Cql_Parser.FOLLOW_K_ON_in_grantPermissionsStatement6217);
				pushFollow(Cql_Parser.FOLLOW_resource_in_grantPermissionsStatement6229);
				resource2 = resource();
				(state._fsp)--;
				match(input, Cql_Parser.K_TO, Cql_Parser.FOLLOW_K_TO_in_grantPermissionsStatement6237);
				pushFollow(Cql_Parser.FOLLOW_userOrRoleName_in_grantPermissionsStatement6251);
				grantee = userOrRoleName();
				(state._fsp)--;
				stmt = new GrantPermissionsStatement(filterPermissions(permissionOrAll1, resource2), resource2, grantee);
			}
		} catch (RecognitionException re) {
			reportError(re);
			recover(input, re);
		} finally {
		}
		return stmt;
	}

	public final RevokePermissionsStatement revokePermissionsStatement() throws RecognitionException {
		RevokePermissionsStatement stmt = null;
		RoleName revokee = null;
		Set<Permission> permissionOrAll3 = null;
		IResource resource4 = null;
		try {
			{
				match(input, Cql_Parser.K_REVOKE, Cql_Parser.FOLLOW_K_REVOKE_in_revokePermissionsStatement6282);
				pushFollow(Cql_Parser.FOLLOW_permissionOrAll_in_revokePermissionsStatement6294);
				permissionOrAll3 = permissionOrAll();
				(state._fsp)--;
				match(input, Cql_Parser.K_ON, Cql_Parser.FOLLOW_K_ON_in_revokePermissionsStatement6302);
				pushFollow(Cql_Parser.FOLLOW_resource_in_revokePermissionsStatement6314);
				resource4 = resource();
				(state._fsp)--;
				match(input, Cql_Parser.K_FROM, Cql_Parser.FOLLOW_K_FROM_in_revokePermissionsStatement6322);
				pushFollow(Cql_Parser.FOLLOW_userOrRoleName_in_revokePermissionsStatement6336);
				revokee = userOrRoleName();
				(state._fsp)--;
				stmt = new RevokePermissionsStatement(filterPermissions(permissionOrAll3, resource4), resource4, revokee);
			}
		} catch (RecognitionException re) {
			reportError(re);
			recover(input, re);
		} finally {
		}
		return stmt;
	}

	public final GrantRoleStatement grantRoleStatement() throws RecognitionException {
		GrantRoleStatement stmt = null;
		RoleName role = null;
		RoleName grantee = null;
		try {
			{
				match(input, Cql_Parser.K_GRANT, Cql_Parser.FOLLOW_K_GRANT_in_grantRoleStatement6367);
				pushFollow(Cql_Parser.FOLLOW_userOrRoleName_in_grantRoleStatement6381);
				role = userOrRoleName();
				(state._fsp)--;
				match(input, Cql_Parser.K_TO, Cql_Parser.FOLLOW_K_TO_in_grantRoleStatement6389);
				pushFollow(Cql_Parser.FOLLOW_userOrRoleName_in_grantRoleStatement6403);
				grantee = userOrRoleName();
				(state._fsp)--;
				stmt = new GrantRoleStatement(role, grantee);
			}
		} catch (RecognitionException re) {
			reportError(re);
			recover(input, re);
		} finally {
		}
		return stmt;
	}

	public final RevokeRoleStatement revokeRoleStatement() throws RecognitionException {
		RevokeRoleStatement stmt = null;
		RoleName role = null;
		RoleName revokee = null;
		try {
			{
				match(input, Cql_Parser.K_REVOKE, Cql_Parser.FOLLOW_K_REVOKE_in_revokeRoleStatement6434);
				pushFollow(Cql_Parser.FOLLOW_userOrRoleName_in_revokeRoleStatement6448);
				role = userOrRoleName();
				(state._fsp)--;
				match(input, Cql_Parser.K_FROM, Cql_Parser.FOLLOW_K_FROM_in_revokeRoleStatement6456);
				pushFollow(Cql_Parser.FOLLOW_userOrRoleName_in_revokeRoleStatement6470);
				revokee = userOrRoleName();
				(state._fsp)--;
				stmt = new RevokeRoleStatement(role, revokee);
			}
		} catch (RecognitionException re) {
			reportError(re);
			recover(input, re);
		} finally {
		}
		return stmt;
	}

	public final ListPermissionsStatement listPermissionsStatement() throws RecognitionException {
		ListPermissionsStatement stmt = null;
		IResource resource5 = null;
		Set<Permission> permissionOrAll6 = null;
		IResource resource = null;
		boolean recursive = true;
		RoleName grantee = new RoleName();
		try {
			{
				match(input, Cql_Parser.K_LIST, Cql_Parser.FOLLOW_K_LIST_in_listPermissionsStatement6508);
				pushFollow(Cql_Parser.FOLLOW_permissionOrAll_in_listPermissionsStatement6520);
				permissionOrAll6 = permissionOrAll();
				(state._fsp)--;
				int alt121 = 2;
				int LA121_0 = input.LA(1);
				if (LA121_0 == (Cql_Parser.K_ON)) {
					alt121 = 1;
				}
				switch (alt121) {
					case 1 :
						{
							match(input, Cql_Parser.K_ON, Cql_Parser.FOLLOW_K_ON_in_listPermissionsStatement6530);
							pushFollow(Cql_Parser.FOLLOW_resource_in_listPermissionsStatement6532);
							resource5 = resource();
							(state._fsp)--;
							resource = resource5;
						}
						break;
				}
				int alt122 = 2;
				int LA122_0 = input.LA(1);
				if (LA122_0 == (Cql_Parser.K_OF)) {
					alt122 = 1;
				}
				switch (alt122) {
					case 1 :
						{
							match(input, Cql_Parser.K_OF, Cql_Parser.FOLLOW_K_OF_in_listPermissionsStatement6547);
							pushFollow(Cql_Parser.FOLLOW_roleName_in_listPermissionsStatement6549);
							roleName(grantee);
							(state._fsp)--;
						}
						break;
				}
				int alt123 = 2;
				int LA123_0 = input.LA(1);
				if (LA123_0 == (Cql_Parser.K_NORECURSIVE)) {
					alt123 = 1;
				}
				switch (alt123) {
					case 1 :
						{
							match(input, Cql_Parser.K_NORECURSIVE, Cql_Parser.FOLLOW_K_NORECURSIVE_in_listPermissionsStatement6563);
							recursive = false;
						}
						break;
				}
				stmt = new ListPermissionsStatement(permissionOrAll6, resource, grantee, recursive);
			}
		} catch (RecognitionException re) {
			reportError(re);
			recover(input, re);
		} finally {
		}
		return stmt;
	}

	public final Permission permission() throws RecognitionException {
		Permission perm = null;
		Token p = null;
		try {
			{
				p = input.LT(1);
				if (((((((((input.LA(1)) == (Cql_Parser.K_ALTER)) || ((input.LA(1)) == (Cql_Parser.K_AUTHORIZE))) || ((input.LA(1)) == (Cql_Parser.K_CREATE))) || ((input.LA(1)) == (Cql_Parser.K_DESCRIBE))) || ((input.LA(1)) == (Cql_Parser.K_DROP))) || ((input.LA(1)) == (Cql_Parser.K_EXECUTE))) || ((input.LA(1)) == (Cql_Parser.K_MODIFY))) || ((input.LA(1)) == (Cql_Parser.K_SELECT))) {
					input.consume();
					state.errorRecovery = false;
				}else {
					MismatchedSetException mse = new MismatchedSetException(null, input);
					throw mse;
				}
				perm = Permission.valueOf((p != null ? p.getText() : null).toUpperCase());
			}
		} catch (RecognitionException re) {
			reportError(re);
			recover(input, re);
		} finally {
		}
		return perm;
	}

	public final Set<Permission> permissionOrAll() throws RecognitionException {
		Set<Permission> perms = null;
		Permission p = null;
		try {
			int alt126 = 2;
			int LA126_0 = input.LA(1);
			if (LA126_0 == (Cql_Parser.K_ALL)) {
				alt126 = 1;
			}else
				if ((((((((LA126_0 == (Cql_Parser.K_ALTER)) || (LA126_0 == (Cql_Parser.K_AUTHORIZE))) || (LA126_0 == (Cql_Parser.K_CREATE))) || (LA126_0 == (Cql_Parser.K_DESCRIBE))) || (LA126_0 == (Cql_Parser.K_DROP))) || (LA126_0 == (Cql_Parser.K_EXECUTE))) || (LA126_0 == (Cql_Parser.K_MODIFY))) || (LA126_0 == (Cql_Parser.K_SELECT))) {
					alt126 = 2;
				}else {
					NoViableAltException nvae = new NoViableAltException("", 126, 0, input);
					throw nvae;
				}

			switch (alt126) {
				case 1 :
					{
						match(input, Cql_Parser.K_ALL, Cql_Parser.FOLLOW_K_ALL_in_permissionOrAll6656);
						int alt124 = 2;
						int LA124_0 = input.LA(1);
						if (LA124_0 == (Cql_Parser.K_PERMISSIONS)) {
							alt124 = 1;
						}
						switch (alt124) {
							case 1 :
								{
									match(input, Cql_Parser.K_PERMISSIONS, Cql_Parser.FOLLOW_K_PERMISSIONS_in_permissionOrAll6660);
								}
								break;
						}
						perms = Permission.ALL;
					}
					break;
				case 2 :
					{
						pushFollow(Cql_Parser.FOLLOW_permission_in_permissionOrAll6681);
						p = permission();
						(state._fsp)--;
						int alt125 = 2;
						int LA125_0 = input.LA(1);
						if (LA125_0 == (Cql_Parser.K_PERMISSION)) {
							alt125 = 1;
						}
						switch (alt125) {
							case 1 :
								{
									match(input, Cql_Parser.K_PERMISSION, Cql_Parser.FOLLOW_K_PERMISSION_in_permissionOrAll6685);
								}
								break;
						}
						perms = EnumSet.of(p);
					}
					break;
			}
		} catch (RecognitionException re) {
			reportError(re);
			recover(input, re);
		} finally {
		}
		return perms;
	}

	public final IResource resource() throws RecognitionException {
		IResource res = null;
		DataResource d = null;
		RoleResource r = null;
		FunctionResource f = null;
		JMXResource j = null;
		try {
			int alt127 = 4;
			switch (input.LA(1)) {
				case Cql_Parser.K_ALL :
					{
						switch (input.LA(2)) {
							case Cql_Parser.EOF :
							case Cql_Parser.K_FROM :
							case Cql_Parser.K_KEYSPACES :
							case Cql_Parser.K_NORECURSIVE :
							case Cql_Parser.K_OF :
							case Cql_Parser.K_TO :
							case 191 :
							case 193 :
								{
									alt127 = 1;
								}
								break;
							case Cql_Parser.K_ROLES :
								{
									alt127 = 2;
								}
								break;
							case Cql_Parser.K_FUNCTIONS :
								{
									alt127 = 3;
								}
								break;
							case Cql_Parser.K_MBEANS :
								{
									alt127 = 4;
								}
								break;
							default :
								int nvaeMark = input.mark();
								try {
									input.consume();
									NoViableAltException nvae = new NoViableAltException("", 127, 1, input);
									throw nvae;
								} finally {
									input.rewind(nvaeMark);
								}
						}
					}
					break;
				case Cql_Parser.IDENT :
				case Cql_Parser.K_AGGREGATE :
				case Cql_Parser.K_AS :
				case Cql_Parser.K_ASCII :
				case Cql_Parser.K_BIGINT :
				case Cql_Parser.K_BLOB :
				case Cql_Parser.K_BOOLEAN :
				case Cql_Parser.K_CALLED :
				case Cql_Parser.K_CAST :
				case Cql_Parser.K_CLUSTERING :
				case Cql_Parser.K_COLUMNFAMILY :
				case Cql_Parser.K_COMPACT :
				case Cql_Parser.K_CONTAINS :
				case Cql_Parser.K_COUNT :
				case Cql_Parser.K_COUNTER :
				case Cql_Parser.K_CUSTOM :
				case Cql_Parser.K_DATE :
				case Cql_Parser.K_DECIMAL :
				case Cql_Parser.K_DISTINCT :
				case Cql_Parser.K_DOUBLE :
				case Cql_Parser.K_DURATION :
				case Cql_Parser.K_EXISTS :
				case Cql_Parser.K_FILTERING :
				case Cql_Parser.K_FINALFUNC :
				case Cql_Parser.K_FLOAT :
				case Cql_Parser.K_FROZEN :
				case Cql_Parser.K_FUNCTIONS :
				case Cql_Parser.K_GROUP :
				case Cql_Parser.K_INET :
				case Cql_Parser.K_INITCOND :
				case Cql_Parser.K_INPUT :
				case Cql_Parser.K_INT :
				case Cql_Parser.K_JSON :
				case Cql_Parser.K_KEY :
				case Cql_Parser.K_KEYS :
				case Cql_Parser.K_KEYSPACE :
				case Cql_Parser.K_KEYSPACES :
				case Cql_Parser.K_LANGUAGE :
				case Cql_Parser.K_LIKE :
				case Cql_Parser.K_LIST :
				case Cql_Parser.K_LOGIN :
				case Cql_Parser.K_MAP :
				case Cql_Parser.K_NOLOGIN :
				case Cql_Parser.K_NOSUPERUSER :
				case Cql_Parser.K_OPTIONS :
				case Cql_Parser.K_PARTITION :
				case Cql_Parser.K_PASSWORD :
				case Cql_Parser.K_PER :
				case Cql_Parser.K_PERMISSION :
				case Cql_Parser.K_PERMISSIONS :
				case Cql_Parser.K_RETURNS :
				case Cql_Parser.K_ROLES :
				case Cql_Parser.K_SFUNC :
				case Cql_Parser.K_SMALLINT :
				case Cql_Parser.K_STATIC :
				case Cql_Parser.K_STORAGE :
				case Cql_Parser.K_STYPE :
				case Cql_Parser.K_SUPERUSER :
				case Cql_Parser.K_TEXT :
				case Cql_Parser.K_TIME :
				case Cql_Parser.K_TIMESTAMP :
				case Cql_Parser.K_TIMEUUID :
				case Cql_Parser.K_TINYINT :
				case Cql_Parser.K_TRIGGER :
				case Cql_Parser.K_TTL :
				case Cql_Parser.K_TUPLE :
				case Cql_Parser.K_TYPE :
				case Cql_Parser.K_USER :
				case Cql_Parser.K_USERS :
				case Cql_Parser.K_UUID :
				case Cql_Parser.K_VALUES :
				case Cql_Parser.K_VARCHAR :
				case Cql_Parser.K_VARINT :
				case Cql_Parser.K_WRITETIME :
				case Cql_Parser.QMARK :
				case Cql_Parser.QUOTED_NAME :
					{
						alt127 = 1;
					}
					break;
				case Cql_Parser.K_ROLE :
					{
						int LA127_3 = input.LA(2);
						if (((((((LA127_3 == (Cql_Parser.EOF)) || (LA127_3 == (Cql_Parser.K_FROM))) || (LA127_3 == (Cql_Parser.K_NORECURSIVE))) || (LA127_3 == (Cql_Parser.K_OF))) || (LA127_3 == (Cql_Parser.K_TO))) || (LA127_3 == 191)) || (LA127_3 == 193)) {
							alt127 = 1;
						}else
							if ((((((((((((((((((((((((((((((((((LA127_3 == (Cql_Parser.IDENT)) || ((LA127_3 >= (Cql_Parser.K_AGGREGATE)) && (LA127_3 <= (Cql_Parser.K_ALL)))) || (LA127_3 == (Cql_Parser.K_AS))) || (LA127_3 == (Cql_Parser.K_ASCII))) || ((LA127_3 >= (Cql_Parser.K_BIGINT)) && (LA127_3 <= (Cql_Parser.K_BOOLEAN)))) || ((LA127_3 >= (Cql_Parser.K_CALLED)) && (LA127_3 <= (Cql_Parser.K_CLUSTERING)))) || ((LA127_3 >= (Cql_Parser.K_COMPACT)) && (LA127_3 <= (Cql_Parser.K_COUNTER)))) || ((LA127_3 >= (Cql_Parser.K_CUSTOM)) && (LA127_3 <= (Cql_Parser.K_DECIMAL)))) || ((LA127_3 >= (Cql_Parser.K_DISTINCT)) && (LA127_3 <= (Cql_Parser.K_DOUBLE)))) || (LA127_3 == (Cql_Parser.K_DURATION))) || ((LA127_3 >= (Cql_Parser.K_EXISTS)) && (LA127_3 <= (Cql_Parser.K_FLOAT)))) || (LA127_3 == (Cql_Parser.K_FROZEN))) || ((LA127_3 >= (Cql_Parser.K_FUNCTION)) && (LA127_3 <= (Cql_Parser.K_FUNCTIONS)))) || (LA127_3 == (Cql_Parser.K_GROUP))) || (LA127_3 == (Cql_Parser.K_INET))) || ((LA127_3 >= (Cql_Parser.K_INITCOND)) && (LA127_3 <= (Cql_Parser.K_INPUT)))) || (LA127_3 == (Cql_Parser.K_INT))) || ((LA127_3 >= (Cql_Parser.K_JSON)) && (LA127_3 <= (Cql_Parser.K_KEYS)))) || ((LA127_3 >= (Cql_Parser.K_KEYSPACES)) && (LA127_3 <= (Cql_Parser.K_LIKE)))) || ((LA127_3 >= (Cql_Parser.K_LIST)) && (LA127_3 <= (Cql_Parser.K_MAP)))) || (LA127_3 == (Cql_Parser.K_NOLOGIN))) || (LA127_3 == (Cql_Parser.K_NOSUPERUSER))) || (LA127_3 == (Cql_Parser.K_OPTIONS))) || ((LA127_3 >= (Cql_Parser.K_PARTITION)) && (LA127_3 <= (Cql_Parser.K_PERMISSIONS)))) || (LA127_3 == (Cql_Parser.K_RETURNS))) || ((LA127_3 >= (Cql_Parser.K_ROLE)) && (LA127_3 <= (Cql_Parser.K_ROLES)))) || ((LA127_3 >= (Cql_Parser.K_SFUNC)) && (LA127_3 <= (Cql_Parser.K_TINYINT)))) || (LA127_3 == (Cql_Parser.K_TRIGGER))) || ((LA127_3 >= (Cql_Parser.K_TTL)) && (LA127_3 <= (Cql_Parser.K_TYPE)))) || ((LA127_3 >= (Cql_Parser.K_USER)) && (LA127_3 <= (Cql_Parser.K_USERS)))) || ((LA127_3 >= (Cql_Parser.K_UUID)) && (LA127_3 <= (Cql_Parser.K_VARINT)))) || (LA127_3 == (Cql_Parser.K_WRITETIME))) || ((LA127_3 >= (Cql_Parser.QMARK)) && (LA127_3 <= (Cql_Parser.QUOTED_NAME)))) || (LA127_3 == (Cql_Parser.STRING_LITERAL))) {
								alt127 = 2;
							}else {
								int nvaeMark = input.mark();
								try {
									input.consume();
									NoViableAltException nvae = new NoViableAltException("", 127, 3, input);
									throw nvae;
								} finally {
									input.rewind(nvaeMark);
								}
							}

					}
					break;
				case Cql_Parser.K_FUNCTION :
					{
						int LA127_4 = input.LA(2);
						if (((((((LA127_4 == (Cql_Parser.EOF)) || (LA127_4 == (Cql_Parser.K_FROM))) || (LA127_4 == (Cql_Parser.K_NORECURSIVE))) || (LA127_4 == (Cql_Parser.K_OF))) || (LA127_4 == (Cql_Parser.K_TO))) || (LA127_4 == 191)) || (LA127_4 == 193)) {
							alt127 = 1;
						}else
							if (((((((((((((((((((((((((((((((((LA127_4 == (Cql_Parser.IDENT)) || ((LA127_4 >= (Cql_Parser.K_AGGREGATE)) && (LA127_4 <= (Cql_Parser.K_ALL)))) || (LA127_4 == (Cql_Parser.K_AS))) || (LA127_4 == (Cql_Parser.K_ASCII))) || ((LA127_4 >= (Cql_Parser.K_BIGINT)) && (LA127_4 <= (Cql_Parser.K_BOOLEAN)))) || ((LA127_4 >= (Cql_Parser.K_CALLED)) && (LA127_4 <= (Cql_Parser.K_CLUSTERING)))) || ((LA127_4 >= (Cql_Parser.K_COMPACT)) && (LA127_4 <= (Cql_Parser.K_COUNTER)))) || ((LA127_4 >= (Cql_Parser.K_CUSTOM)) && (LA127_4 <= (Cql_Parser.K_DECIMAL)))) || ((LA127_4 >= (Cql_Parser.K_DISTINCT)) && (LA127_4 <= (Cql_Parser.K_DOUBLE)))) || (LA127_4 == (Cql_Parser.K_DURATION))) || ((LA127_4 >= (Cql_Parser.K_EXISTS)) && (LA127_4 <= (Cql_Parser.K_FLOAT)))) || (LA127_4 == (Cql_Parser.K_FROZEN))) || ((LA127_4 >= (Cql_Parser.K_FUNCTION)) && (LA127_4 <= (Cql_Parser.K_FUNCTIONS)))) || (LA127_4 == (Cql_Parser.K_GROUP))) || (LA127_4 == (Cql_Parser.K_INET))) || ((LA127_4 >= (Cql_Parser.K_INITCOND)) && (LA127_4 <= (Cql_Parser.K_INPUT)))) || (LA127_4 == (Cql_Parser.K_INT))) || ((LA127_4 >= (Cql_Parser.K_JSON)) && (LA127_4 <= (Cql_Parser.K_KEYS)))) || ((LA127_4 >= (Cql_Parser.K_KEYSPACES)) && (LA127_4 <= (Cql_Parser.K_LIKE)))) || ((LA127_4 >= (Cql_Parser.K_LIST)) && (LA127_4 <= (Cql_Parser.K_MAP)))) || (LA127_4 == (Cql_Parser.K_NOLOGIN))) || (LA127_4 == (Cql_Parser.K_NOSUPERUSER))) || (LA127_4 == (Cql_Parser.K_OPTIONS))) || ((LA127_4 >= (Cql_Parser.K_PARTITION)) && (LA127_4 <= (Cql_Parser.K_PERMISSIONS)))) || (LA127_4 == (Cql_Parser.K_RETURNS))) || ((LA127_4 >= (Cql_Parser.K_ROLE)) && (LA127_4 <= (Cql_Parser.K_ROLES)))) || ((LA127_4 >= (Cql_Parser.K_SFUNC)) && (LA127_4 <= (Cql_Parser.K_TINYINT)))) || ((LA127_4 >= (Cql_Parser.K_TOKEN)) && (LA127_4 <= (Cql_Parser.K_TRIGGER)))) || ((LA127_4 >= (Cql_Parser.K_TTL)) && (LA127_4 <= (Cql_Parser.K_TYPE)))) || ((LA127_4 >= (Cql_Parser.K_USER)) && (LA127_4 <= (Cql_Parser.K_USERS)))) || ((LA127_4 >= (Cql_Parser.K_UUID)) && (LA127_4 <= (Cql_Parser.K_VARINT)))) || (LA127_4 == (Cql_Parser.K_WRITETIME))) || ((LA127_4 >= (Cql_Parser.QMARK)) && (LA127_4 <= (Cql_Parser.QUOTED_NAME)))) {
								alt127 = 3;
							}else {
								int nvaeMark = input.mark();
								try {
									input.consume();
									NoViableAltException nvae = new NoViableAltException("", 127, 4, input);
									throw nvae;
								} finally {
									input.rewind(nvaeMark);
								}
							}

					}
					break;
				case Cql_Parser.K_MBEAN :
				case Cql_Parser.K_MBEANS :
					{
						alt127 = 4;
					}
					break;
				default :
					NoViableAltException nvae = new NoViableAltException("", 127, 0, input);
					throw nvae;
			}
			switch (alt127) {
				case 1 :
					{
						pushFollow(Cql_Parser.FOLLOW_dataResource_in_resource6713);
						d = dataResource();
						(state._fsp)--;
						res = d;
					}
					break;
				case 2 :
					{
						pushFollow(Cql_Parser.FOLLOW_roleResource_in_resource6725);
						r = roleResource();
						(state._fsp)--;
						res = r;
					}
					break;
				case 3 :
					{
						pushFollow(Cql_Parser.FOLLOW_functionResource_in_resource6737);
						f = functionResource();
						(state._fsp)--;
						res = f;
					}
					break;
				case 4 :
					{
						pushFollow(Cql_Parser.FOLLOW_jmxResource_in_resource6749);
						j = jmxResource();
						(state._fsp)--;
						res = j;
					}
					break;
			}
		} catch (RecognitionException re) {
			reportError(re);
			recover(input, re);
		} finally {
		}
		return res;
	}

	public final DataResource dataResource() throws RecognitionException {
		DataResource res = null;
		String ks = null;
		CFName cf = null;
		try {
			int alt129 = 3;
			switch (input.LA(1)) {
				case Cql_Parser.K_ALL :
					{
						int LA129_1 = input.LA(2);
						if (LA129_1 == (Cql_Parser.K_KEYSPACES)) {
							alt129 = 1;
						}else
							if (((((((LA129_1 == (Cql_Parser.EOF)) || (LA129_1 == (Cql_Parser.K_FROM))) || (LA129_1 == (Cql_Parser.K_NORECURSIVE))) || (LA129_1 == (Cql_Parser.K_OF))) || (LA129_1 == (Cql_Parser.K_TO))) || (LA129_1 == 191)) || (LA129_1 == 193)) {
								alt129 = 3;
							}else {
								int nvaeMark = input.mark();
								try {
									input.consume();
									NoViableAltException nvae = new NoViableAltException("", 129, 1, input);
									throw nvae;
								} finally {
									input.rewind(nvaeMark);
								}
							}

					}
					break;
				case Cql_Parser.K_KEYSPACE :
					{
						alt129 = 2;
					}
					break;
				case Cql_Parser.IDENT :
				case Cql_Parser.K_AGGREGATE :
				case Cql_Parser.K_AS :
				case Cql_Parser.K_ASCII :
				case Cql_Parser.K_BIGINT :
				case Cql_Parser.K_BLOB :
				case Cql_Parser.K_BOOLEAN :
				case Cql_Parser.K_CALLED :
				case Cql_Parser.K_CAST :
				case Cql_Parser.K_CLUSTERING :
				case Cql_Parser.K_COLUMNFAMILY :
				case Cql_Parser.K_COMPACT :
				case Cql_Parser.K_CONTAINS :
				case Cql_Parser.K_COUNT :
				case Cql_Parser.K_COUNTER :
				case Cql_Parser.K_CUSTOM :
				case Cql_Parser.K_DATE :
				case Cql_Parser.K_DECIMAL :
				case Cql_Parser.K_DISTINCT :
				case Cql_Parser.K_DOUBLE :
				case Cql_Parser.K_DURATION :
				case Cql_Parser.K_EXISTS :
				case Cql_Parser.K_FILTERING :
				case Cql_Parser.K_FINALFUNC :
				case Cql_Parser.K_FLOAT :
				case Cql_Parser.K_FROZEN :
				case Cql_Parser.K_FUNCTION :
				case Cql_Parser.K_FUNCTIONS :
				case Cql_Parser.K_GROUP :
				case Cql_Parser.K_INET :
				case Cql_Parser.K_INITCOND :
				case Cql_Parser.K_INPUT :
				case Cql_Parser.K_INT :
				case Cql_Parser.K_JSON :
				case Cql_Parser.K_KEY :
				case Cql_Parser.K_KEYS :
				case Cql_Parser.K_KEYSPACES :
				case Cql_Parser.K_LANGUAGE :
				case Cql_Parser.K_LIKE :
				case Cql_Parser.K_LIST :
				case Cql_Parser.K_LOGIN :
				case Cql_Parser.K_MAP :
				case Cql_Parser.K_NOLOGIN :
				case Cql_Parser.K_NOSUPERUSER :
				case Cql_Parser.K_OPTIONS :
				case Cql_Parser.K_PARTITION :
				case Cql_Parser.K_PASSWORD :
				case Cql_Parser.K_PER :
				case Cql_Parser.K_PERMISSION :
				case Cql_Parser.K_PERMISSIONS :
				case Cql_Parser.K_RETURNS :
				case Cql_Parser.K_ROLE :
				case Cql_Parser.K_ROLES :
				case Cql_Parser.K_SFUNC :
				case Cql_Parser.K_SMALLINT :
				case Cql_Parser.K_STATIC :
				case Cql_Parser.K_STORAGE :
				case Cql_Parser.K_STYPE :
				case Cql_Parser.K_SUPERUSER :
				case Cql_Parser.K_TEXT :
				case Cql_Parser.K_TIME :
				case Cql_Parser.K_TIMESTAMP :
				case Cql_Parser.K_TIMEUUID :
				case Cql_Parser.K_TINYINT :
				case Cql_Parser.K_TRIGGER :
				case Cql_Parser.K_TTL :
				case Cql_Parser.K_TUPLE :
				case Cql_Parser.K_TYPE :
				case Cql_Parser.K_USER :
				case Cql_Parser.K_USERS :
				case Cql_Parser.K_UUID :
				case Cql_Parser.K_VALUES :
				case Cql_Parser.K_VARCHAR :
				case Cql_Parser.K_VARINT :
				case Cql_Parser.K_WRITETIME :
				case Cql_Parser.QMARK :
				case Cql_Parser.QUOTED_NAME :
					{
						alt129 = 3;
					}
					break;
				default :
					NoViableAltException nvae = new NoViableAltException("", 129, 0, input);
					throw nvae;
			}
			switch (alt129) {
				case 1 :
					{
						match(input, Cql_Parser.K_ALL, Cql_Parser.FOLLOW_K_ALL_in_dataResource6772);
						match(input, Cql_Parser.K_KEYSPACES, Cql_Parser.FOLLOW_K_KEYSPACES_in_dataResource6774);
						res = DataResource.root();
					}
					break;
				case 2 :
					{
						match(input, Cql_Parser.K_KEYSPACE, Cql_Parser.FOLLOW_K_KEYSPACE_in_dataResource6784);
						pushFollow(Cql_Parser.FOLLOW_keyspaceName_in_dataResource6790);
						ks = keyspaceName();
						(state._fsp)--;
						res = DataResource.keyspace(ks);
					}
					break;
				case 3 :
					{
						int alt128 = 2;
						int LA128_0 = input.LA(1);
						if (LA128_0 == (Cql_Parser.K_COLUMNFAMILY)) {
							alt128 = 1;
						}
						switch (alt128) {
							case 1 :
								{
									match(input, Cql_Parser.K_COLUMNFAMILY, Cql_Parser.FOLLOW_K_COLUMNFAMILY_in_dataResource6802);
								}
								break;
						}
						pushFollow(Cql_Parser.FOLLOW_columnFamilyName_in_dataResource6811);
						cf = columnFamilyName();
						(state._fsp)--;
						res = DataResource.table(cf.getKeyspace(), cf.getColumnFamily());
					}
					break;
			}
		} catch (RecognitionException re) {
			reportError(re);
			recover(input, re);
		} finally {
		}
		return res;
	}

	public final JMXResource jmxResource() throws RecognitionException {
		JMXResource res = null;
		ParserRuleReturnScope mbean7 = null;
		ParserRuleReturnScope mbean8 = null;
		try {
			int alt130 = 3;
			switch (input.LA(1)) {
				case Cql_Parser.K_ALL :
					{
						alt130 = 1;
					}
					break;
				case Cql_Parser.K_MBEAN :
					{
						alt130 = 2;
					}
					break;
				case Cql_Parser.K_MBEANS :
					{
						alt130 = 3;
					}
					break;
				default :
					NoViableAltException nvae = new NoViableAltException("", 130, 0, input);
					throw nvae;
			}
			switch (alt130) {
				case 1 :
					{
						match(input, Cql_Parser.K_ALL, Cql_Parser.FOLLOW_K_ALL_in_jmxResource6840);
						match(input, Cql_Parser.K_MBEANS, Cql_Parser.FOLLOW_K_MBEANS_in_jmxResource6842);
						res = JMXResource.root();
					}
					break;
				case 2 :
					{
						match(input, Cql_Parser.K_MBEAN, Cql_Parser.FOLLOW_K_MBEAN_in_jmxResource6862);
						pushFollow(Cql_Parser.FOLLOW_mbean_in_jmxResource6864);
						mbean7 = mbean();
						(state._fsp)--;
						res = JMXResource.mbean(canonicalizeObjectName((mbean7 != null ? input.toString(mbean7.start, mbean7.stop) : null), false));
					}
					break;
				case 3 :
					{
						match(input, Cql_Parser.K_MBEANS, Cql_Parser.FOLLOW_K_MBEANS_in_jmxResource6874);
						pushFollow(Cql_Parser.FOLLOW_mbean_in_jmxResource6876);
						mbean8 = mbean();
						(state._fsp)--;
						res = JMXResource.mbean(canonicalizeObjectName((mbean8 != null ? input.toString(mbean8.start, mbean8.stop) : null), true));
					}
					break;
			}
		} catch (RecognitionException re) {
			reportError(re);
			recover(input, re);
		} finally {
		}
		return res;
	}

	public final RoleResource roleResource() throws RecognitionException {
		RoleResource res = null;
		RoleName role = null;
		try {
			int alt131 = 2;
			int LA131_0 = input.LA(1);
			if (LA131_0 == (Cql_Parser.K_ALL)) {
				alt131 = 1;
			}else
				if (LA131_0 == (Cql_Parser.K_ROLE)) {
					alt131 = 2;
				}else {
					NoViableAltException nvae = new NoViableAltException("", 131, 0, input);
					throw nvae;
				}

			switch (alt131) {
				case 1 :
					{
						match(input, Cql_Parser.K_ALL, Cql_Parser.FOLLOW_K_ALL_in_roleResource6899);
						match(input, Cql_Parser.K_ROLES, Cql_Parser.FOLLOW_K_ROLES_in_roleResource6901);
						res = RoleResource.root();
					}
					break;
				case 2 :
					{
						match(input, Cql_Parser.K_ROLE, Cql_Parser.FOLLOW_K_ROLE_in_roleResource6911);
						pushFollow(Cql_Parser.FOLLOW_userOrRoleName_in_roleResource6917);
						role = userOrRoleName();
						(state._fsp)--;
						res = RoleResource.role(role.getName());
					}
					break;
			}
		} catch (RecognitionException re) {
			reportError(re);
			recover(input, re);
		} finally {
		}
		return res;
	}

	public final FunctionResource functionResource() throws RecognitionException {
		FunctionResource res = null;
		String ks = null;
		FunctionName fn = null;
		CQL3Type.Raw v = null;
		List<CQL3Type.Raw> argsTypes = new ArrayList<>();
		try {
			int alt134 = 3;
			int LA134_0 = input.LA(1);
			if (LA134_0 == (Cql_Parser.K_ALL)) {
				int LA134_1 = input.LA(2);
				if (LA134_1 == (Cql_Parser.K_FUNCTIONS)) {
					int LA134_3 = input.LA(3);
					if (LA134_3 == (Cql_Parser.K_IN)) {
						alt134 = 2;
					}else
						if ((((((LA134_3 == (Cql_Parser.EOF)) || (LA134_3 == (Cql_Parser.K_FROM))) || (LA134_3 == (Cql_Parser.K_NORECURSIVE))) || (LA134_3 == (Cql_Parser.K_OF))) || (LA134_3 == (Cql_Parser.K_TO))) || (LA134_3 == 193)) {
							alt134 = 1;
						}else {
							int nvaeMark = input.mark();
							try {
								for (int nvaeConsume = 0; nvaeConsume < (3 - 1); nvaeConsume++) {
									input.consume();
								}
								NoViableAltException nvae = new NoViableAltException("", 134, 3, input);
								throw nvae;
							} finally {
								input.rewind(nvaeMark);
							}
						}

				}else {
					int nvaeMark = input.mark();
					try {
						input.consume();
						NoViableAltException nvae = new NoViableAltException("", 134, 1, input);
						throw nvae;
					} finally {
						input.rewind(nvaeMark);
					}
				}
			}else
				if (LA134_0 == (Cql_Parser.K_FUNCTION)) {
					alt134 = 3;
				}else {
					NoViableAltException nvae = new NoViableAltException("", 134, 0, input);
					throw nvae;
				}

			switch (alt134) {
				case 1 :
					{
						match(input, Cql_Parser.K_ALL, Cql_Parser.FOLLOW_K_ALL_in_functionResource6949);
						match(input, Cql_Parser.K_FUNCTIONS, Cql_Parser.FOLLOW_K_FUNCTIONS_in_functionResource6951);
						res = FunctionResource.root();
					}
					break;
				case 2 :
					{
						match(input, Cql_Parser.K_ALL, Cql_Parser.FOLLOW_K_ALL_in_functionResource6961);
						match(input, Cql_Parser.K_FUNCTIONS, Cql_Parser.FOLLOW_K_FUNCTIONS_in_functionResource6963);
						match(input, Cql_Parser.K_IN, Cql_Parser.FOLLOW_K_IN_in_functionResource6965);
						match(input, Cql_Parser.K_KEYSPACE, Cql_Parser.FOLLOW_K_KEYSPACE_in_functionResource6967);
						pushFollow(Cql_Parser.FOLLOW_keyspaceName_in_functionResource6973);
						ks = keyspaceName();
						(state._fsp)--;
						res = FunctionResource.keyspace(ks);
					}
					break;
				case 3 :
					{
						match(input, Cql_Parser.K_FUNCTION, Cql_Parser.FOLLOW_K_FUNCTION_in_functionResource6988);
						pushFollow(Cql_Parser.FOLLOW_functionName_in_functionResource6992);
						fn = functionName();
						(state._fsp)--;
						{
							match(input, 184, Cql_Parser.FOLLOW_184_in_functionResource7010);
							int alt133 = 2;
							int LA133_0 = input.LA(1);
							if ((((((((((((((((((((((((((((((((((LA133_0 == (Cql_Parser.IDENT)) || ((LA133_0 >= (Cql_Parser.K_AGGREGATE)) && (LA133_0 <= (Cql_Parser.K_ALL)))) || (LA133_0 == (Cql_Parser.K_AS))) || (LA133_0 == (Cql_Parser.K_ASCII))) || ((LA133_0 >= (Cql_Parser.K_BIGINT)) && (LA133_0 <= (Cql_Parser.K_BOOLEAN)))) || ((LA133_0 >= (Cql_Parser.K_CALLED)) && (LA133_0 <= (Cql_Parser.K_CLUSTERING)))) || ((LA133_0 >= (Cql_Parser.K_COMPACT)) && (LA133_0 <= (Cql_Parser.K_COUNTER)))) || ((LA133_0 >= (Cql_Parser.K_CUSTOM)) && (LA133_0 <= (Cql_Parser.K_DECIMAL)))) || ((LA133_0 >= (Cql_Parser.K_DISTINCT)) && (LA133_0 <= (Cql_Parser.K_DOUBLE)))) || (LA133_0 == (Cql_Parser.K_DURATION))) || ((LA133_0 >= (Cql_Parser.K_EXISTS)) && (LA133_0 <= (Cql_Parser.K_FLOAT)))) || (LA133_0 == (Cql_Parser.K_FROZEN))) || ((LA133_0 >= (Cql_Parser.K_FUNCTION)) && (LA133_0 <= (Cql_Parser.K_FUNCTIONS)))) || (LA133_0 == (Cql_Parser.K_GROUP))) || (LA133_0 == (Cql_Parser.K_INET))) || ((LA133_0 >= (Cql_Parser.K_INITCOND)) && (LA133_0 <= (Cql_Parser.K_INPUT)))) || (LA133_0 == (Cql_Parser.K_INT))) || ((LA133_0 >= (Cql_Parser.K_JSON)) && (LA133_0 <= (Cql_Parser.K_KEYS)))) || ((LA133_0 >= (Cql_Parser.K_KEYSPACES)) && (LA133_0 <= (Cql_Parser.K_LIKE)))) || ((LA133_0 >= (Cql_Parser.K_LIST)) && (LA133_0 <= (Cql_Parser.K_MAP)))) || (LA133_0 == (Cql_Parser.K_NOLOGIN))) || (LA133_0 == (Cql_Parser.K_NOSUPERUSER))) || (LA133_0 == (Cql_Parser.K_OPTIONS))) || ((LA133_0 >= (Cql_Parser.K_PARTITION)) && (LA133_0 <= (Cql_Parser.K_PERMISSIONS)))) || (LA133_0 == (Cql_Parser.K_RETURNS))) || ((LA133_0 >= (Cql_Parser.K_ROLE)) && (LA133_0 <= (Cql_Parser.K_ROLES)))) || ((LA133_0 >= (Cql_Parser.K_SET)) && (LA133_0 <= (Cql_Parser.K_TINYINT)))) || (LA133_0 == (Cql_Parser.K_TRIGGER))) || ((LA133_0 >= (Cql_Parser.K_TTL)) && (LA133_0 <= (Cql_Parser.K_TYPE)))) || ((LA133_0 >= (Cql_Parser.K_USER)) && (LA133_0 <= (Cql_Parser.K_USERS)))) || ((LA133_0 >= (Cql_Parser.K_UUID)) && (LA133_0 <= (Cql_Parser.K_VARINT)))) || (LA133_0 == (Cql_Parser.K_WRITETIME))) || (LA133_0 == (Cql_Parser.QUOTED_NAME))) || (LA133_0 == (Cql_Parser.STRING_LITERAL))) {
								alt133 = 1;
							}
							switch (alt133) {
								case 1 :
									{
										pushFollow(Cql_Parser.FOLLOW_comparatorType_in_functionResource7038);
										v = comparatorType();
										(state._fsp)--;
										argsTypes.add(v);
										loop132 : while (true) {
											int alt132 = 2;
											int LA132_0 = input.LA(1);
											if (LA132_0 == 188) {
												alt132 = 1;
											}
											switch (alt132) {
												case 1 :
													{
														match(input, 188, Cql_Parser.FOLLOW_188_in_functionResource7056);
														pushFollow(Cql_Parser.FOLLOW_comparatorType_in_functionResource7060);
														v = comparatorType();
														(state._fsp)--;
														argsTypes.add(v);
													}
													break;
												default :
													break loop132;
											}
										} 
									}
									break;
							}
							match(input, 185, Cql_Parser.FOLLOW_185_in_functionResource7088);
						}
						res = FunctionResource.functionFromCql(fn.keyspace, fn.name, argsTypes);
					}
					break;
			}
		} catch (RecognitionException re) {
			reportError(re);
			recover(input, re);
		} finally {
		}
		return res;
	}

	public final CreateRoleStatement createUserStatement() throws RecognitionException {
		CreateRoleStatement stmt = null;
		ParserRuleReturnScope u = null;
		RoleOptions opts = new RoleOptions();
		opts.setOption(LOGIN, true);
		boolean superuser = false;
		boolean ifNotExists = false;
		RoleName name = new RoleName();
		try {
			{
				match(input, Cql_Parser.K_CREATE, Cql_Parser.FOLLOW_K_CREATE_in_createUserStatement7136);
				match(input, Cql_Parser.K_USER, Cql_Parser.FOLLOW_K_USER_in_createUserStatement7138);
				int alt135 = 2;
				int LA135_0 = input.LA(1);
				if (LA135_0 == (Cql_Parser.K_IF)) {
					alt135 = 1;
				}
				switch (alt135) {
					case 1 :
						{
							match(input, Cql_Parser.K_IF, Cql_Parser.FOLLOW_K_IF_in_createUserStatement7141);
							match(input, Cql_Parser.K_NOT, Cql_Parser.FOLLOW_K_NOT_in_createUserStatement7143);
							match(input, Cql_Parser.K_EXISTS, Cql_Parser.FOLLOW_K_EXISTS_in_createUserStatement7145);
							ifNotExists = true;
						}
						break;
				}
				pushFollow(Cql_Parser.FOLLOW_username_in_createUserStatement7153);
				u = username();
				(state._fsp)--;
				name.setName((u != null ? input.toString(u.start, u.stop) : null), true);
				int alt136 = 2;
				int LA136_0 = input.LA(1);
				if (LA136_0 == (Cql_Parser.K_WITH)) {
					alt136 = 1;
				}
				switch (alt136) {
					case 1 :
						{
							match(input, Cql_Parser.K_WITH, Cql_Parser.FOLLOW_K_WITH_in_createUserStatement7165);
							pushFollow(Cql_Parser.FOLLOW_userPassword_in_createUserStatement7167);
							userPassword(opts);
							(state._fsp)--;
						}
						break;
				}
				int alt137 = 3;
				int LA137_0 = input.LA(1);
				if (LA137_0 == (Cql_Parser.K_SUPERUSER)) {
					alt137 = 1;
				}else
					if (LA137_0 == (Cql_Parser.K_NOSUPERUSER)) {
						alt137 = 2;
					}

				switch (alt137) {
					case 1 :
						{
							match(input, Cql_Parser.K_SUPERUSER, Cql_Parser.FOLLOW_K_SUPERUSER_in_createUserStatement7181);
							superuser = true;
						}
						break;
					case 2 :
						{
							match(input, Cql_Parser.K_NOSUPERUSER, Cql_Parser.FOLLOW_K_NOSUPERUSER_in_createUserStatement7187);
							superuser = false;
						}
						break;
				}
				opts.setOption(SUPERUSER, superuser);
				stmt = new CreateRoleStatement(name, opts, ifNotExists);
			}
		} catch (RecognitionException re) {
			reportError(re);
			recover(input, re);
		} finally {
		}
		return stmt;
	}

	public final AlterRoleStatement alterUserStatement() throws RecognitionException {
		AlterRoleStatement stmt = null;
		ParserRuleReturnScope u = null;
		RoleOptions opts = new RoleOptions();
		RoleName name = new RoleName();
		try {
			{
				match(input, Cql_Parser.K_ALTER, Cql_Parser.FOLLOW_K_ALTER_in_alterUserStatement7232);
				match(input, Cql_Parser.K_USER, Cql_Parser.FOLLOW_K_USER_in_alterUserStatement7234);
				pushFollow(Cql_Parser.FOLLOW_username_in_alterUserStatement7238);
				u = username();
				(state._fsp)--;
				name.setName((u != null ? input.toString(u.start, u.stop) : null), true);
				int alt138 = 2;
				int LA138_0 = input.LA(1);
				if (LA138_0 == (Cql_Parser.K_WITH)) {
					alt138 = 1;
				}
				switch (alt138) {
					case 1 :
						{
							match(input, Cql_Parser.K_WITH, Cql_Parser.FOLLOW_K_WITH_in_alterUserStatement7250);
							pushFollow(Cql_Parser.FOLLOW_userPassword_in_alterUserStatement7252);
							userPassword(opts);
							(state._fsp)--;
						}
						break;
				}
				int alt139 = 3;
				int LA139_0 = input.LA(1);
				if (LA139_0 == (Cql_Parser.K_SUPERUSER)) {
					alt139 = 1;
				}else
					if (LA139_0 == (Cql_Parser.K_NOSUPERUSER)) {
						alt139 = 2;
					}

				switch (alt139) {
					case 1 :
						{
							match(input, Cql_Parser.K_SUPERUSER, Cql_Parser.FOLLOW_K_SUPERUSER_in_alterUserStatement7266);
							opts.setOption(SUPERUSER, true);
						}
						break;
					case 2 :
						{
							match(input, Cql_Parser.K_NOSUPERUSER, Cql_Parser.FOLLOW_K_NOSUPERUSER_in_alterUserStatement7280);
							opts.setOption(SUPERUSER, false);
						}
						break;
				}
				stmt = new AlterRoleStatement(name, opts);
			}
		} catch (RecognitionException re) {
			reportError(re);
			recover(input, re);
		} finally {
		}
		return stmt;
	}

	public final DropRoleStatement dropUserStatement() throws RecognitionException {
		DropRoleStatement stmt = null;
		ParserRuleReturnScope u = null;
		boolean ifExists = false;
		RoleName name = new RoleName();
		try {
			{
				match(input, Cql_Parser.K_DROP, Cql_Parser.FOLLOW_K_DROP_in_dropUserStatement7326);
				match(input, Cql_Parser.K_USER, Cql_Parser.FOLLOW_K_USER_in_dropUserStatement7328);
				int alt140 = 2;
				int LA140_0 = input.LA(1);
				if (LA140_0 == (Cql_Parser.K_IF)) {
					alt140 = 1;
				}
				switch (alt140) {
					case 1 :
						{
							match(input, Cql_Parser.K_IF, Cql_Parser.FOLLOW_K_IF_in_dropUserStatement7331);
							match(input, Cql_Parser.K_EXISTS, Cql_Parser.FOLLOW_K_EXISTS_in_dropUserStatement7333);
							ifExists = true;
						}
						break;
				}
				pushFollow(Cql_Parser.FOLLOW_username_in_dropUserStatement7341);
				u = username();
				(state._fsp)--;
				name.setName((u != null ? input.toString(u.start, u.stop) : null), true);
				stmt = new DropRoleStatement(name, ifExists);
			}
		} catch (RecognitionException re) {
			reportError(re);
			recover(input, re);
		} finally {
		}
		return stmt;
	}

	public final ListRolesStatement listUsersStatement() throws RecognitionException {
		ListRolesStatement stmt = null;
		try {
			{
				match(input, Cql_Parser.K_LIST, Cql_Parser.FOLLOW_K_LIST_in_listUsersStatement7366);
				match(input, Cql_Parser.K_USERS, Cql_Parser.FOLLOW_K_USERS_in_listUsersStatement7368);
				stmt = new ListUsersStatement();
			}
		} catch (RecognitionException re) {
			reportError(re);
			recover(input, re);
		} finally {
		}
		return stmt;
	}

	public final CreateRoleStatement createRoleStatement() throws RecognitionException {
		CreateRoleStatement stmt = null;
		RoleName name = null;
		RoleOptions opts = new RoleOptions();
		boolean ifNotExists = false;
		try {
			{
				match(input, Cql_Parser.K_CREATE, Cql_Parser.FOLLOW_K_CREATE_in_createRoleStatement7402);
				match(input, Cql_Parser.K_ROLE, Cql_Parser.FOLLOW_K_ROLE_in_createRoleStatement7404);
				int alt141 = 2;
				int LA141_0 = input.LA(1);
				if (LA141_0 == (Cql_Parser.K_IF)) {
					alt141 = 1;
				}
				switch (alt141) {
					case 1 :
						{
							match(input, Cql_Parser.K_IF, Cql_Parser.FOLLOW_K_IF_in_createRoleStatement7407);
							match(input, Cql_Parser.K_NOT, Cql_Parser.FOLLOW_K_NOT_in_createRoleStatement7409);
							match(input, Cql_Parser.K_EXISTS, Cql_Parser.FOLLOW_K_EXISTS_in_createRoleStatement7411);
							ifNotExists = true;
						}
						break;
				}
				pushFollow(Cql_Parser.FOLLOW_userOrRoleName_in_createRoleStatement7419);
				name = userOrRoleName();
				(state._fsp)--;
				int alt142 = 2;
				int LA142_0 = input.LA(1);
				if (LA142_0 == (Cql_Parser.K_WITH)) {
					alt142 = 1;
				}
				switch (alt142) {
					case 1 :
						{
							match(input, Cql_Parser.K_WITH, Cql_Parser.FOLLOW_K_WITH_in_createRoleStatement7429);
							pushFollow(Cql_Parser.FOLLOW_roleOptions_in_createRoleStatement7431);
							roleOptions(opts);
							(state._fsp)--;
						}
						break;
				}
				if (!(opts.getLogin().isPresent())) {
					opts.setOption(LOGIN, false);
				}
				if (!(opts.getSuperuser().isPresent())) {
					opts.setOption(SUPERUSER, false);
				}
				stmt = new CreateRoleStatement(name, opts, ifNotExists);
			}
		} catch (RecognitionException re) {
			reportError(re);
			recover(input, re);
		} finally {
		}
		return stmt;
	}

	public final AlterRoleStatement alterRoleStatement() throws RecognitionException {
		AlterRoleStatement stmt = null;
		RoleName name = null;
		RoleOptions opts = new RoleOptions();
		try {
			{
				match(input, Cql_Parser.K_ALTER, Cql_Parser.FOLLOW_K_ALTER_in_alterRoleStatement7475);
				match(input, Cql_Parser.K_ROLE, Cql_Parser.FOLLOW_K_ROLE_in_alterRoleStatement7477);
				pushFollow(Cql_Parser.FOLLOW_userOrRoleName_in_alterRoleStatement7481);
				name = userOrRoleName();
				(state._fsp)--;
				int alt143 = 2;
				int LA143_0 = input.LA(1);
				if (LA143_0 == (Cql_Parser.K_WITH)) {
					alt143 = 1;
				}
				switch (alt143) {
					case 1 :
						{
							match(input, Cql_Parser.K_WITH, Cql_Parser.FOLLOW_K_WITH_in_alterRoleStatement7491);
							pushFollow(Cql_Parser.FOLLOW_roleOptions_in_alterRoleStatement7493);
							roleOptions(opts);
							(state._fsp)--;
						}
						break;
				}
				stmt = new AlterRoleStatement(name, opts);
			}
		} catch (RecognitionException re) {
			reportError(re);
			recover(input, re);
		} finally {
		}
		return stmt;
	}

	public final DropRoleStatement dropRoleStatement() throws RecognitionException {
		DropRoleStatement stmt = null;
		RoleName name = null;
		boolean ifExists = false;
		try {
			{
				match(input, Cql_Parser.K_DROP, Cql_Parser.FOLLOW_K_DROP_in_dropRoleStatement7537);
				match(input, Cql_Parser.K_ROLE, Cql_Parser.FOLLOW_K_ROLE_in_dropRoleStatement7539);
				int alt144 = 2;
				int LA144_0 = input.LA(1);
				if (LA144_0 == (Cql_Parser.K_IF)) {
					alt144 = 1;
				}
				switch (alt144) {
					case 1 :
						{
							match(input, Cql_Parser.K_IF, Cql_Parser.FOLLOW_K_IF_in_dropRoleStatement7542);
							match(input, Cql_Parser.K_EXISTS, Cql_Parser.FOLLOW_K_EXISTS_in_dropRoleStatement7544);
							ifExists = true;
						}
						break;
				}
				pushFollow(Cql_Parser.FOLLOW_userOrRoleName_in_dropRoleStatement7552);
				name = userOrRoleName();
				(state._fsp)--;
				stmt = new DropRoleStatement(name, ifExists);
			}
		} catch (RecognitionException re) {
			reportError(re);
			recover(input, re);
		} finally {
		}
		return stmt;
	}

	public final ListRolesStatement listRolesStatement() throws RecognitionException {
		ListRolesStatement stmt = null;
		boolean recursive = true;
		RoleName grantee = new RoleName();
		try {
			{
				match(input, Cql_Parser.K_LIST, Cql_Parser.FOLLOW_K_LIST_in_listRolesStatement7592);
				match(input, Cql_Parser.K_ROLES, Cql_Parser.FOLLOW_K_ROLES_in_listRolesStatement7594);
				int alt145 = 2;
				int LA145_0 = input.LA(1);
				if (LA145_0 == (Cql_Parser.K_OF)) {
					alt145 = 1;
				}
				switch (alt145) {
					case 1 :
						{
							match(input, Cql_Parser.K_OF, Cql_Parser.FOLLOW_K_OF_in_listRolesStatement7604);
							pushFollow(Cql_Parser.FOLLOW_roleName_in_listRolesStatement7606);
							roleName(grantee);
							(state._fsp)--;
						}
						break;
				}
				int alt146 = 2;
				int LA146_0 = input.LA(1);
				if (LA146_0 == (Cql_Parser.K_NORECURSIVE)) {
					alt146 = 1;
				}
				switch (alt146) {
					case 1 :
						{
							match(input, Cql_Parser.K_NORECURSIVE, Cql_Parser.FOLLOW_K_NORECURSIVE_in_listRolesStatement7619);
							recursive = false;
						}
						break;
				}
				stmt = new ListRolesStatement(grantee, recursive);
			}
		} catch (RecognitionException re) {
			reportError(re);
			recover(input, re);
		} finally {
		}
		return stmt;
	}

	public final void roleOptions(RoleOptions opts) throws RecognitionException {
		try {
			{
				pushFollow(Cql_Parser.FOLLOW_roleOption_in_roleOptions7650);
				roleOption(opts);
				(state._fsp)--;
				loop147 : while (true) {
					int alt147 = 2;
					int LA147_0 = input.LA(1);
					if (LA147_0 == (Cql_Parser.K_AND)) {
						alt147 = 1;
					}
					switch (alt147) {
						case 1 :
							{
								match(input, Cql_Parser.K_AND, Cql_Parser.FOLLOW_K_AND_in_roleOptions7654);
								pushFollow(Cql_Parser.FOLLOW_roleOption_in_roleOptions7656);
								roleOption(opts);
								(state._fsp)--;
							}
							break;
						default :
							break loop147;
					}
				} 
			}
		} catch (RecognitionException re) {
			reportError(re);
			recover(input, re);
		} finally {
		}
	}

	public final void roleOption(RoleOptions opts) throws RecognitionException {
		Token v = null;
		Token b = null;
		Maps.Literal m = null;
		try {
			int alt148 = 4;
			switch (input.LA(1)) {
				case Cql_Parser.K_PASSWORD :
					{
						alt148 = 1;
					}
					break;
				case Cql_Parser.K_OPTIONS :
					{
						alt148 = 2;
					}
					break;
				case Cql_Parser.K_SUPERUSER :
					{
						alt148 = 3;
					}
					break;
				case Cql_Parser.K_LOGIN :
					{
						alt148 = 4;
					}
					break;
				default :
					NoViableAltException nvae = new NoViableAltException("", 148, 0, input);
					throw nvae;
			}
			switch (alt148) {
				case 1 :
					{
						match(input, Cql_Parser.K_PASSWORD, Cql_Parser.FOLLOW_K_PASSWORD_in_roleOption7678);
						match(input, 196, Cql_Parser.FOLLOW_196_in_roleOption7680);
						v = ((Token) (match(input, Cql_Parser.STRING_LITERAL, Cql_Parser.FOLLOW_STRING_LITERAL_in_roleOption7684)));
						opts.setOption(PASSWORD, (v != null ? v.getText() : null));
					}
					break;
				case 2 :
					{
						match(input, Cql_Parser.K_OPTIONS, Cql_Parser.FOLLOW_K_OPTIONS_in_roleOption7695);
						match(input, 196, Cql_Parser.FOLLOW_196_in_roleOption7697);
						pushFollow(Cql_Parser.FOLLOW_mapLiteral_in_roleOption7701);
						m = mapLiteral();
						(state._fsp)--;
						opts.setOption(OPTIONS, convertPropertyMap(m));
					}
					break;
				case 3 :
					{
						match(input, Cql_Parser.K_SUPERUSER, Cql_Parser.FOLLOW_K_SUPERUSER_in_roleOption7712);
						match(input, 196, Cql_Parser.FOLLOW_196_in_roleOption7714);
						b = ((Token) (match(input, Cql_Parser.BOOLEAN, Cql_Parser.FOLLOW_BOOLEAN_in_roleOption7718)));
						opts.setOption(SUPERUSER, Boolean.valueOf((b != null ? b.getText() : null)));
					}
					break;
				case 4 :
					{
						match(input, Cql_Parser.K_LOGIN, Cql_Parser.FOLLOW_K_LOGIN_in_roleOption7729);
						match(input, 196, Cql_Parser.FOLLOW_196_in_roleOption7731);
						b = ((Token) (match(input, Cql_Parser.BOOLEAN, Cql_Parser.FOLLOW_BOOLEAN_in_roleOption7735)));
						opts.setOption(LOGIN, Boolean.valueOf((b != null ? b.getText() : null)));
					}
					break;
			}
		} catch (RecognitionException re) {
			reportError(re);
			recover(input, re);
		} finally {
		}
	}

	public final void userPassword(RoleOptions opts) throws RecognitionException {
		Token v = null;
		try {
			{
				match(input, Cql_Parser.K_PASSWORD, Cql_Parser.FOLLOW_K_PASSWORD_in_userPassword7757);
				v = ((Token) (match(input, Cql_Parser.STRING_LITERAL, Cql_Parser.FOLLOW_STRING_LITERAL_in_userPassword7761)));
				opts.setOption(PASSWORD, (v != null ? v.getText() : null));
			}
		} catch (RecognitionException re) {
			reportError(re);
			recover(input, re);
		} finally {
		}
	}

	public final ColumnDefinition.Raw cident() throws RecognitionException {
		ColumnDefinition.Raw id = null;
		Token t = null;
		String k = null;
		try {
			int alt149 = 4;
			switch (input.LA(1)) {
				case Cql_Parser.EMPTY_QUOTED_NAME :
					{
						alt149 = 1;
					}
					break;
				case Cql_Parser.IDENT :
					{
						alt149 = 2;
					}
					break;
				case Cql_Parser.QUOTED_NAME :
					{
						alt149 = 3;
					}
					break;
				case Cql_Parser.K_AGGREGATE :
				case Cql_Parser.K_ALL :
				case Cql_Parser.K_AS :
				case Cql_Parser.K_ASCII :
				case Cql_Parser.K_BIGINT :
				case Cql_Parser.K_BLOB :
				case Cql_Parser.K_BOOLEAN :
				case Cql_Parser.K_CALLED :
				case Cql_Parser.K_CAST :
				case Cql_Parser.K_CLUSTERING :
				case Cql_Parser.K_COMPACT :
				case Cql_Parser.K_CONTAINS :
				case Cql_Parser.K_COUNT :
				case Cql_Parser.K_COUNTER :
				case Cql_Parser.K_CUSTOM :
				case Cql_Parser.K_DATE :
				case Cql_Parser.K_DECIMAL :
				case Cql_Parser.K_DISTINCT :
				case Cql_Parser.K_DOUBLE :
				case Cql_Parser.K_DURATION :
				case Cql_Parser.K_EXISTS :
				case Cql_Parser.K_FILTERING :
				case Cql_Parser.K_FINALFUNC :
				case Cql_Parser.K_FLOAT :
				case Cql_Parser.K_FROZEN :
				case Cql_Parser.K_FUNCTION :
				case Cql_Parser.K_FUNCTIONS :
				case Cql_Parser.K_GROUP :
				case Cql_Parser.K_INET :
				case Cql_Parser.K_INITCOND :
				case Cql_Parser.K_INPUT :
				case Cql_Parser.K_INT :
				case Cql_Parser.K_JSON :
				case Cql_Parser.K_KEY :
				case Cql_Parser.K_KEYS :
				case Cql_Parser.K_KEYSPACES :
				case Cql_Parser.K_LANGUAGE :
				case Cql_Parser.K_LIKE :
				case Cql_Parser.K_LIST :
				case Cql_Parser.K_LOGIN :
				case Cql_Parser.K_MAP :
				case Cql_Parser.K_NOLOGIN :
				case Cql_Parser.K_NOSUPERUSER :
				case Cql_Parser.K_OPTIONS :
				case Cql_Parser.K_PARTITION :
				case Cql_Parser.K_PASSWORD :
				case Cql_Parser.K_PER :
				case Cql_Parser.K_PERMISSION :
				case Cql_Parser.K_PERMISSIONS :
				case Cql_Parser.K_RETURNS :
				case Cql_Parser.K_ROLE :
				case Cql_Parser.K_ROLES :
				case Cql_Parser.K_SFUNC :
				case Cql_Parser.K_SMALLINT :
				case Cql_Parser.K_STATIC :
				case Cql_Parser.K_STORAGE :
				case Cql_Parser.K_STYPE :
				case Cql_Parser.K_SUPERUSER :
				case Cql_Parser.K_TEXT :
				case Cql_Parser.K_TIME :
				case Cql_Parser.K_TIMESTAMP :
				case Cql_Parser.K_TIMEUUID :
				case Cql_Parser.K_TINYINT :
				case Cql_Parser.K_TRIGGER :
				case Cql_Parser.K_TTL :
				case Cql_Parser.K_TUPLE :
				case Cql_Parser.K_TYPE :
				case Cql_Parser.K_USER :
				case Cql_Parser.K_USERS :
				case Cql_Parser.K_UUID :
				case Cql_Parser.K_VALUES :
				case Cql_Parser.K_VARCHAR :
				case Cql_Parser.K_VARINT :
				case Cql_Parser.K_WRITETIME :
					{
						alt149 = 4;
					}
					break;
				default :
					NoViableAltException nvae = new NoViableAltException("", 149, 0, input);
					throw nvae;
			}
			switch (alt149) {
				case 1 :
					{
						match(input, Cql_Parser.EMPTY_QUOTED_NAME, Cql_Parser.FOLLOW_EMPTY_QUOTED_NAME_in_cident7793);
						id = forQuoted("");
					}
					break;
				case 2 :
					{
						t = ((Token) (match(input, Cql_Parser.IDENT, Cql_Parser.FOLLOW_IDENT_in_cident7808)));
						id = forUnquoted((t != null ? t.getText() : null));
					}
					break;
				case 3 :
					{
						t = ((Token) (match(input, Cql_Parser.QUOTED_NAME, Cql_Parser.FOLLOW_QUOTED_NAME_in_cident7833)));
						id = forQuoted((t != null ? t.getText() : null));
					}
					break;
				case 4 :
					{
						pushFollow(Cql_Parser.FOLLOW_unreserved_keyword_in_cident7852);
						k = unreserved_keyword();
						(state._fsp)--;
						id = forUnquoted(k);
					}
					break;
			}
		} catch (RecognitionException re) {
			reportError(re);
			recover(input, re);
		} finally {
		}
		return id;
	}

	public final ColumnDefinition.Raw schema_cident() throws RecognitionException {
		ColumnDefinition.Raw id = null;
		Token t = null;
		String k = null;
		try {
			int alt150 = 3;
			switch (input.LA(1)) {
				case Cql_Parser.IDENT :
					{
						alt150 = 1;
					}
					break;
				case Cql_Parser.QUOTED_NAME :
					{
						alt150 = 2;
					}
					break;
				case Cql_Parser.K_AGGREGATE :
				case Cql_Parser.K_ALL :
				case Cql_Parser.K_AS :
				case Cql_Parser.K_ASCII :
				case Cql_Parser.K_BIGINT :
				case Cql_Parser.K_BLOB :
				case Cql_Parser.K_BOOLEAN :
				case Cql_Parser.K_CALLED :
				case Cql_Parser.K_CAST :
				case Cql_Parser.K_CLUSTERING :
				case Cql_Parser.K_COMPACT :
				case Cql_Parser.K_CONTAINS :
				case Cql_Parser.K_COUNT :
				case Cql_Parser.K_COUNTER :
				case Cql_Parser.K_CUSTOM :
				case Cql_Parser.K_DATE :
				case Cql_Parser.K_DECIMAL :
				case Cql_Parser.K_DISTINCT :
				case Cql_Parser.K_DOUBLE :
				case Cql_Parser.K_DURATION :
				case Cql_Parser.K_EXISTS :
				case Cql_Parser.K_FILTERING :
				case Cql_Parser.K_FINALFUNC :
				case Cql_Parser.K_FLOAT :
				case Cql_Parser.K_FROZEN :
				case Cql_Parser.K_FUNCTION :
				case Cql_Parser.K_FUNCTIONS :
				case Cql_Parser.K_GROUP :
				case Cql_Parser.K_INET :
				case Cql_Parser.K_INITCOND :
				case Cql_Parser.K_INPUT :
				case Cql_Parser.K_INT :
				case Cql_Parser.K_JSON :
				case Cql_Parser.K_KEY :
				case Cql_Parser.K_KEYS :
				case Cql_Parser.K_KEYSPACES :
				case Cql_Parser.K_LANGUAGE :
				case Cql_Parser.K_LIKE :
				case Cql_Parser.K_LIST :
				case Cql_Parser.K_LOGIN :
				case Cql_Parser.K_MAP :
				case Cql_Parser.K_NOLOGIN :
				case Cql_Parser.K_NOSUPERUSER :
				case Cql_Parser.K_OPTIONS :
				case Cql_Parser.K_PARTITION :
				case Cql_Parser.K_PASSWORD :
				case Cql_Parser.K_PER :
				case Cql_Parser.K_PERMISSION :
				case Cql_Parser.K_PERMISSIONS :
				case Cql_Parser.K_RETURNS :
				case Cql_Parser.K_ROLE :
				case Cql_Parser.K_ROLES :
				case Cql_Parser.K_SFUNC :
				case Cql_Parser.K_SMALLINT :
				case Cql_Parser.K_STATIC :
				case Cql_Parser.K_STORAGE :
				case Cql_Parser.K_STYPE :
				case Cql_Parser.K_SUPERUSER :
				case Cql_Parser.K_TEXT :
				case Cql_Parser.K_TIME :
				case Cql_Parser.K_TIMESTAMP :
				case Cql_Parser.K_TIMEUUID :
				case Cql_Parser.K_TINYINT :
				case Cql_Parser.K_TRIGGER :
				case Cql_Parser.K_TTL :
				case Cql_Parser.K_TUPLE :
				case Cql_Parser.K_TYPE :
				case Cql_Parser.K_USER :
				case Cql_Parser.K_USERS :
				case Cql_Parser.K_UUID :
				case Cql_Parser.K_VALUES :
				case Cql_Parser.K_VARCHAR :
				case Cql_Parser.K_VARINT :
				case Cql_Parser.K_WRITETIME :
					{
						alt150 = 3;
					}
					break;
				default :
					NoViableAltException nvae = new NoViableAltException("", 150, 0, input);
					throw nvae;
			}
			switch (alt150) {
				case 1 :
					{
						t = ((Token) (match(input, Cql_Parser.IDENT, Cql_Parser.FOLLOW_IDENT_in_schema_cident7877)));
						id = forUnquoted((t != null ? t.getText() : null));
					}
					break;
				case 2 :
					{
						t = ((Token) (match(input, Cql_Parser.QUOTED_NAME, Cql_Parser.FOLLOW_QUOTED_NAME_in_schema_cident7902)));
						id = forQuoted((t != null ? t.getText() : null));
					}
					break;
				case 3 :
					{
						pushFollow(Cql_Parser.FOLLOW_unreserved_keyword_in_schema_cident7921);
						k = unreserved_keyword();
						(state._fsp)--;
						id = forUnquoted(k);
					}
					break;
			}
		} catch (RecognitionException re) {
			reportError(re);
			recover(input, re);
		} finally {
		}
		return id;
	}

	public final ColumnIdentifier ident() throws RecognitionException {
		ColumnIdentifier id = null;
		Token t = null;
		String k = null;
		try {
			int alt151 = 3;
			switch (input.LA(1)) {
				case Cql_Parser.IDENT :
					{
						alt151 = 1;
					}
					break;
				case Cql_Parser.QUOTED_NAME :
					{
						alt151 = 2;
					}
					break;
				case Cql_Parser.K_AGGREGATE :
				case Cql_Parser.K_ALL :
				case Cql_Parser.K_AS :
				case Cql_Parser.K_ASCII :
				case Cql_Parser.K_BIGINT :
				case Cql_Parser.K_BLOB :
				case Cql_Parser.K_BOOLEAN :
				case Cql_Parser.K_CALLED :
				case Cql_Parser.K_CAST :
				case Cql_Parser.K_CLUSTERING :
				case Cql_Parser.K_COMPACT :
				case Cql_Parser.K_CONTAINS :
				case Cql_Parser.K_COUNT :
				case Cql_Parser.K_COUNTER :
				case Cql_Parser.K_CUSTOM :
				case Cql_Parser.K_DATE :
				case Cql_Parser.K_DECIMAL :
				case Cql_Parser.K_DISTINCT :
				case Cql_Parser.K_DOUBLE :
				case Cql_Parser.K_DURATION :
				case Cql_Parser.K_EXISTS :
				case Cql_Parser.K_FILTERING :
				case Cql_Parser.K_FINALFUNC :
				case Cql_Parser.K_FLOAT :
				case Cql_Parser.K_FROZEN :
				case Cql_Parser.K_FUNCTION :
				case Cql_Parser.K_FUNCTIONS :
				case Cql_Parser.K_GROUP :
				case Cql_Parser.K_INET :
				case Cql_Parser.K_INITCOND :
				case Cql_Parser.K_INPUT :
				case Cql_Parser.K_INT :
				case Cql_Parser.K_JSON :
				case Cql_Parser.K_KEY :
				case Cql_Parser.K_KEYS :
				case Cql_Parser.K_KEYSPACES :
				case Cql_Parser.K_LANGUAGE :
				case Cql_Parser.K_LIKE :
				case Cql_Parser.K_LIST :
				case Cql_Parser.K_LOGIN :
				case Cql_Parser.K_MAP :
				case Cql_Parser.K_NOLOGIN :
				case Cql_Parser.K_NOSUPERUSER :
				case Cql_Parser.K_OPTIONS :
				case Cql_Parser.K_PARTITION :
				case Cql_Parser.K_PASSWORD :
				case Cql_Parser.K_PER :
				case Cql_Parser.K_PERMISSION :
				case Cql_Parser.K_PERMISSIONS :
				case Cql_Parser.K_RETURNS :
				case Cql_Parser.K_ROLE :
				case Cql_Parser.K_ROLES :
				case Cql_Parser.K_SFUNC :
				case Cql_Parser.K_SMALLINT :
				case Cql_Parser.K_STATIC :
				case Cql_Parser.K_STORAGE :
				case Cql_Parser.K_STYPE :
				case Cql_Parser.K_SUPERUSER :
				case Cql_Parser.K_TEXT :
				case Cql_Parser.K_TIME :
				case Cql_Parser.K_TIMESTAMP :
				case Cql_Parser.K_TIMEUUID :
				case Cql_Parser.K_TINYINT :
				case Cql_Parser.K_TRIGGER :
				case Cql_Parser.K_TTL :
				case Cql_Parser.K_TUPLE :
				case Cql_Parser.K_TYPE :
				case Cql_Parser.K_USER :
				case Cql_Parser.K_USERS :
				case Cql_Parser.K_UUID :
				case Cql_Parser.K_VALUES :
				case Cql_Parser.K_VARCHAR :
				case Cql_Parser.K_VARINT :
				case Cql_Parser.K_WRITETIME :
					{
						alt151 = 3;
					}
					break;
				default :
					NoViableAltException nvae = new NoViableAltException("", 151, 0, input);
					throw nvae;
			}
			switch (alt151) {
				case 1 :
					{
						t = ((Token) (match(input, Cql_Parser.IDENT, Cql_Parser.FOLLOW_IDENT_in_ident7947)));
						id = ColumnIdentifier.getInterned((t != null ? t.getText() : null), false);
					}
					break;
				case 2 :
					{
						t = ((Token) (match(input, Cql_Parser.QUOTED_NAME, Cql_Parser.FOLLOW_QUOTED_NAME_in_ident7972)));
						id = ColumnIdentifier.getInterned((t != null ? t.getText() : null), true);
					}
					break;
				case 3 :
					{
						pushFollow(Cql_Parser.FOLLOW_unreserved_keyword_in_ident7991);
						k = unreserved_keyword();
						(state._fsp)--;
						id = ColumnIdentifier.getInterned(k, false);
					}
					break;
			}
		} catch (RecognitionException re) {
			reportError(re);
			recover(input, re);
		} finally {
		}
		return id;
	}

	public final FieldIdentifier fident() throws RecognitionException {
		FieldIdentifier id = null;
		Token t = null;
		String k = null;
		try {
			int alt152 = 3;
			switch (input.LA(1)) {
				case Cql_Parser.IDENT :
					{
						alt152 = 1;
					}
					break;
				case Cql_Parser.QUOTED_NAME :
					{
						alt152 = 2;
					}
					break;
				case Cql_Parser.K_AGGREGATE :
				case Cql_Parser.K_ALL :
				case Cql_Parser.K_AS :
				case Cql_Parser.K_ASCII :
				case Cql_Parser.K_BIGINT :
				case Cql_Parser.K_BLOB :
				case Cql_Parser.K_BOOLEAN :
				case Cql_Parser.K_CALLED :
				case Cql_Parser.K_CAST :
				case Cql_Parser.K_CLUSTERING :
				case Cql_Parser.K_COMPACT :
				case Cql_Parser.K_CONTAINS :
				case Cql_Parser.K_COUNT :
				case Cql_Parser.K_COUNTER :
				case Cql_Parser.K_CUSTOM :
				case Cql_Parser.K_DATE :
				case Cql_Parser.K_DECIMAL :
				case Cql_Parser.K_DISTINCT :
				case Cql_Parser.K_DOUBLE :
				case Cql_Parser.K_DURATION :
				case Cql_Parser.K_EXISTS :
				case Cql_Parser.K_FILTERING :
				case Cql_Parser.K_FINALFUNC :
				case Cql_Parser.K_FLOAT :
				case Cql_Parser.K_FROZEN :
				case Cql_Parser.K_FUNCTION :
				case Cql_Parser.K_FUNCTIONS :
				case Cql_Parser.K_GROUP :
				case Cql_Parser.K_INET :
				case Cql_Parser.K_INITCOND :
				case Cql_Parser.K_INPUT :
				case Cql_Parser.K_INT :
				case Cql_Parser.K_JSON :
				case Cql_Parser.K_KEY :
				case Cql_Parser.K_KEYS :
				case Cql_Parser.K_KEYSPACES :
				case Cql_Parser.K_LANGUAGE :
				case Cql_Parser.K_LIKE :
				case Cql_Parser.K_LIST :
				case Cql_Parser.K_LOGIN :
				case Cql_Parser.K_MAP :
				case Cql_Parser.K_NOLOGIN :
				case Cql_Parser.K_NOSUPERUSER :
				case Cql_Parser.K_OPTIONS :
				case Cql_Parser.K_PARTITION :
				case Cql_Parser.K_PASSWORD :
				case Cql_Parser.K_PER :
				case Cql_Parser.K_PERMISSION :
				case Cql_Parser.K_PERMISSIONS :
				case Cql_Parser.K_RETURNS :
				case Cql_Parser.K_ROLE :
				case Cql_Parser.K_ROLES :
				case Cql_Parser.K_SFUNC :
				case Cql_Parser.K_SMALLINT :
				case Cql_Parser.K_STATIC :
				case Cql_Parser.K_STORAGE :
				case Cql_Parser.K_STYPE :
				case Cql_Parser.K_SUPERUSER :
				case Cql_Parser.K_TEXT :
				case Cql_Parser.K_TIME :
				case Cql_Parser.K_TIMESTAMP :
				case Cql_Parser.K_TIMEUUID :
				case Cql_Parser.K_TINYINT :
				case Cql_Parser.K_TRIGGER :
				case Cql_Parser.K_TTL :
				case Cql_Parser.K_TUPLE :
				case Cql_Parser.K_TYPE :
				case Cql_Parser.K_USER :
				case Cql_Parser.K_USERS :
				case Cql_Parser.K_UUID :
				case Cql_Parser.K_VALUES :
				case Cql_Parser.K_VARCHAR :
				case Cql_Parser.K_VARINT :
				case Cql_Parser.K_WRITETIME :
					{
						alt152 = 3;
					}
					break;
				default :
					NoViableAltException nvae = new NoViableAltException("", 152, 0, input);
					throw nvae;
			}
			switch (alt152) {
				case 1 :
					{
						t = ((Token) (match(input, Cql_Parser.IDENT, Cql_Parser.FOLLOW_IDENT_in_fident8016)));
						id = FieldIdentifier.forUnquoted((t != null ? t.getText() : null));
					}
					break;
				case 2 :
					{
						t = ((Token) (match(input, Cql_Parser.QUOTED_NAME, Cql_Parser.FOLLOW_QUOTED_NAME_in_fident8041)));
						id = FieldIdentifier.forQuoted((t != null ? t.getText() : null));
					}
					break;
				case 3 :
					{
						pushFollow(Cql_Parser.FOLLOW_unreserved_keyword_in_fident8060);
						k = unreserved_keyword();
						(state._fsp)--;
						id = FieldIdentifier.forUnquoted(k);
					}
					break;
			}
		} catch (RecognitionException re) {
			reportError(re);
			recover(input, re);
		} finally {
		}
		return id;
	}

	public final ColumnIdentifier noncol_ident() throws RecognitionException {
		ColumnIdentifier id = null;
		Token t = null;
		String k = null;
		try {
			int alt153 = 3;
			switch (input.LA(1)) {
				case Cql_Parser.IDENT :
					{
						alt153 = 1;
					}
					break;
				case Cql_Parser.QUOTED_NAME :
					{
						alt153 = 2;
					}
					break;
				case Cql_Parser.K_AGGREGATE :
				case Cql_Parser.K_ALL :
				case Cql_Parser.K_AS :
				case Cql_Parser.K_ASCII :
				case Cql_Parser.K_BIGINT :
				case Cql_Parser.K_BLOB :
				case Cql_Parser.K_BOOLEAN :
				case Cql_Parser.K_CALLED :
				case Cql_Parser.K_CAST :
				case Cql_Parser.K_CLUSTERING :
				case Cql_Parser.K_COMPACT :
				case Cql_Parser.K_CONTAINS :
				case Cql_Parser.K_COUNT :
				case Cql_Parser.K_COUNTER :
				case Cql_Parser.K_CUSTOM :
				case Cql_Parser.K_DATE :
				case Cql_Parser.K_DECIMAL :
				case Cql_Parser.K_DISTINCT :
				case Cql_Parser.K_DOUBLE :
				case Cql_Parser.K_DURATION :
				case Cql_Parser.K_EXISTS :
				case Cql_Parser.K_FILTERING :
				case Cql_Parser.K_FINALFUNC :
				case Cql_Parser.K_FLOAT :
				case Cql_Parser.K_FROZEN :
				case Cql_Parser.K_FUNCTION :
				case Cql_Parser.K_FUNCTIONS :
				case Cql_Parser.K_GROUP :
				case Cql_Parser.K_INET :
				case Cql_Parser.K_INITCOND :
				case Cql_Parser.K_INPUT :
				case Cql_Parser.K_INT :
				case Cql_Parser.K_JSON :
				case Cql_Parser.K_KEY :
				case Cql_Parser.K_KEYS :
				case Cql_Parser.K_KEYSPACES :
				case Cql_Parser.K_LANGUAGE :
				case Cql_Parser.K_LIKE :
				case Cql_Parser.K_LIST :
				case Cql_Parser.K_LOGIN :
				case Cql_Parser.K_MAP :
				case Cql_Parser.K_NOLOGIN :
				case Cql_Parser.K_NOSUPERUSER :
				case Cql_Parser.K_OPTIONS :
				case Cql_Parser.K_PARTITION :
				case Cql_Parser.K_PASSWORD :
				case Cql_Parser.K_PER :
				case Cql_Parser.K_PERMISSION :
				case Cql_Parser.K_PERMISSIONS :
				case Cql_Parser.K_RETURNS :
				case Cql_Parser.K_ROLE :
				case Cql_Parser.K_ROLES :
				case Cql_Parser.K_SFUNC :
				case Cql_Parser.K_SMALLINT :
				case Cql_Parser.K_STATIC :
				case Cql_Parser.K_STORAGE :
				case Cql_Parser.K_STYPE :
				case Cql_Parser.K_SUPERUSER :
				case Cql_Parser.K_TEXT :
				case Cql_Parser.K_TIME :
				case Cql_Parser.K_TIMESTAMP :
				case Cql_Parser.K_TIMEUUID :
				case Cql_Parser.K_TINYINT :
				case Cql_Parser.K_TRIGGER :
				case Cql_Parser.K_TTL :
				case Cql_Parser.K_TUPLE :
				case Cql_Parser.K_TYPE :
				case Cql_Parser.K_USER :
				case Cql_Parser.K_USERS :
				case Cql_Parser.K_UUID :
				case Cql_Parser.K_VALUES :
				case Cql_Parser.K_VARCHAR :
				case Cql_Parser.K_VARINT :
				case Cql_Parser.K_WRITETIME :
					{
						alt153 = 3;
					}
					break;
				default :
					NoViableAltException nvae = new NoViableAltException("", 153, 0, input);
					throw nvae;
			}
			switch (alt153) {
				case 1 :
					{
						t = ((Token) (match(input, Cql_Parser.IDENT, Cql_Parser.FOLLOW_IDENT_in_noncol_ident8086)));
						id = new ColumnIdentifier((t != null ? t.getText() : null), false);
					}
					break;
				case 2 :
					{
						t = ((Token) (match(input, Cql_Parser.QUOTED_NAME, Cql_Parser.FOLLOW_QUOTED_NAME_in_noncol_ident8111)));
						id = new ColumnIdentifier((t != null ? t.getText() : null), true);
					}
					break;
				case 3 :
					{
						pushFollow(Cql_Parser.FOLLOW_unreserved_keyword_in_noncol_ident8130);
						k = unreserved_keyword();
						(state._fsp)--;
						id = new ColumnIdentifier(k, false);
					}
					break;
			}
		} catch (RecognitionException re) {
			reportError(re);
			recover(input, re);
		} finally {
		}
		return id;
	}

	public final String keyspaceName() throws RecognitionException {
		String id = null;
		CFName name = new CFName();
		{
			pushFollow(Cql_Parser.FOLLOW_ksName_in_keyspaceName8163);
			(state._fsp)--;
			id = name.getKeyspace();
		}
		return id;
	}

	public final IndexName indexName() throws RecognitionException {
		IndexName name = null;
		name = new IndexName();
		try {
			{
				int alt154 = 2;
				alt154 = dfa154.predict(input);
				switch (alt154) {
					case 1 :
						{
							pushFollow(Cql_Parser.FOLLOW_ksName_in_indexName8197);
							(state._fsp)--;
							match(input, 191, Cql_Parser.FOLLOW_191_in_indexName8200);
						}
						break;
				}
				pushFollow(Cql_Parser.FOLLOW_idxName_in_indexName8204);
				idxName(name);
				(state._fsp)--;
			}
		} catch (RecognitionException re) {
			reportError(re);
			recover(input, re);
		} finally {
		}
		return name;
	}

	public final CFName columnFamilyName() throws RecognitionException {
		CFName name = null;
		name = new CFName();
		try {
			{
				int alt155 = 2;
				alt155 = dfa155.predict(input);
				switch (alt155) {
					case 1 :
						{
							pushFollow(Cql_Parser.FOLLOW_ksName_in_columnFamilyName8236);
							(state._fsp)--;
							match(input, 191, Cql_Parser.FOLLOW_191_in_columnFamilyName8239);
						}
						break;
				}
				pushFollow(Cql_Parser.FOLLOW_cfName_in_columnFamilyName8243);
				cfName(name);
				(state._fsp)--;
			}
		} catch (RecognitionException re) {
			reportError(re);
			recover(input, re);
		} finally {
		}
		return name;
	}

	public final UTName userTypeName() throws RecognitionException {
		UTName name = null;
		ColumnIdentifier ks = null;
		ColumnIdentifier ut = null;
		try {
			{
				int alt156 = 2;
				switch (input.LA(1)) {
					case Cql_Parser.IDENT :
						{
							int LA156_1 = input.LA(2);
							if (LA156_1 == 191) {
								alt156 = 1;
							}
						}
						break;
					case Cql_Parser.QUOTED_NAME :
						{
							int LA156_2 = input.LA(2);
							if (LA156_2 == 191) {
								alt156 = 1;
							}
						}
						break;
					case Cql_Parser.K_AGGREGATE :
					case Cql_Parser.K_ALL :
					case Cql_Parser.K_AS :
					case Cql_Parser.K_CALLED :
					case Cql_Parser.K_CLUSTERING :
					case Cql_Parser.K_COMPACT :
					case Cql_Parser.K_CONTAINS :
					case Cql_Parser.K_CUSTOM :
					case Cql_Parser.K_EXISTS :
					case Cql_Parser.K_FILTERING :
					case Cql_Parser.K_FINALFUNC :
					case Cql_Parser.K_FROZEN :
					case Cql_Parser.K_FUNCTION :
					case Cql_Parser.K_FUNCTIONS :
					case Cql_Parser.K_GROUP :
					case Cql_Parser.K_INITCOND :
					case Cql_Parser.K_INPUT :
					case Cql_Parser.K_KEYS :
					case Cql_Parser.K_KEYSPACES :
					case Cql_Parser.K_LANGUAGE :
					case Cql_Parser.K_LIKE :
					case Cql_Parser.K_LIST :
					case Cql_Parser.K_LOGIN :
					case Cql_Parser.K_MAP :
					case Cql_Parser.K_NOLOGIN :
					case Cql_Parser.K_NOSUPERUSER :
					case Cql_Parser.K_OPTIONS :
					case Cql_Parser.K_PARTITION :
					case Cql_Parser.K_PASSWORD :
					case Cql_Parser.K_PER :
					case Cql_Parser.K_PERMISSION :
					case Cql_Parser.K_PERMISSIONS :
					case Cql_Parser.K_RETURNS :
					case Cql_Parser.K_ROLE :
					case Cql_Parser.K_ROLES :
					case Cql_Parser.K_SFUNC :
					case Cql_Parser.K_STATIC :
					case Cql_Parser.K_STORAGE :
					case Cql_Parser.K_STYPE :
					case Cql_Parser.K_SUPERUSER :
					case Cql_Parser.K_TRIGGER :
					case Cql_Parser.K_TUPLE :
					case Cql_Parser.K_TYPE :
					case Cql_Parser.K_USER :
					case Cql_Parser.K_USERS :
					case Cql_Parser.K_VALUES :
						{
							int LA156_3 = input.LA(2);
							if (LA156_3 == 191) {
								alt156 = 1;
							}
						}
						break;
					case Cql_Parser.K_ASCII :
					case Cql_Parser.K_BIGINT :
					case Cql_Parser.K_BLOB :
					case Cql_Parser.K_BOOLEAN :
					case Cql_Parser.K_CAST :
					case Cql_Parser.K_COUNT :
					case Cql_Parser.K_COUNTER :
					case Cql_Parser.K_DATE :
					case Cql_Parser.K_DECIMAL :
					case Cql_Parser.K_DISTINCT :
					case Cql_Parser.K_DOUBLE :
					case Cql_Parser.K_DURATION :
					case Cql_Parser.K_FLOAT :
					case Cql_Parser.K_INET :
					case Cql_Parser.K_INT :
					case Cql_Parser.K_JSON :
					case Cql_Parser.K_SMALLINT :
					case Cql_Parser.K_TEXT :
					case Cql_Parser.K_TIME :
					case Cql_Parser.K_TIMESTAMP :
					case Cql_Parser.K_TIMEUUID :
					case Cql_Parser.K_TINYINT :
					case Cql_Parser.K_TTL :
					case Cql_Parser.K_UUID :
					case Cql_Parser.K_VARCHAR :
					case Cql_Parser.K_VARINT :
					case Cql_Parser.K_WRITETIME :
						{
							alt156 = 1;
						}
						break;
					case Cql_Parser.K_KEY :
						{
							int LA156_5 = input.LA(2);
							if (LA156_5 == 191) {
								alt156 = 1;
							}
						}
						break;
				}
				switch (alt156) {
					case 1 :
						{
							pushFollow(Cql_Parser.FOLLOW_noncol_ident_in_userTypeName8268);
							ks = noncol_ident();
							(state._fsp)--;
							match(input, 191, Cql_Parser.FOLLOW_191_in_userTypeName8270);
						}
						break;
				}
				pushFollow(Cql_Parser.FOLLOW_non_type_ident_in_userTypeName8276);
				ut = non_type_ident();
				(state._fsp)--;
				name = new UTName(ks, ut);
			}
		} catch (RecognitionException re) {
			reportError(re);
			recover(input, re);
		} finally {
		}
		return name;
	}

	public final RoleName userOrRoleName() throws RecognitionException {
		RoleName name = null;
		RoleName role = new RoleName();
		try {
			{
				pushFollow(Cql_Parser.FOLLOW_roleName_in_userOrRoleName8308);
				roleName(role);
				(state._fsp)--;
				name = role;
			}
		} catch (RecognitionException re) {
			reportError(re);
			recover(input, re);
		} finally {
		}
		return name;
	}

	public final void cfName(CFName name) throws RecognitionException {
		Token t = null;
		String k = null;
		try {
			int alt158 = 4;
			switch (input.LA(1)) {
				case Cql_Parser.IDENT :
					{
						alt158 = 1;
					}
					break;
				case Cql_Parser.QUOTED_NAME :
					{
						alt158 = 2;
					}
					break;
				case Cql_Parser.K_AGGREGATE :
				case Cql_Parser.K_ALL :
				case Cql_Parser.K_AS :
				case Cql_Parser.K_ASCII :
				case Cql_Parser.K_BIGINT :
				case Cql_Parser.K_BLOB :
				case Cql_Parser.K_BOOLEAN :
				case Cql_Parser.K_CALLED :
				case Cql_Parser.K_CAST :
				case Cql_Parser.K_CLUSTERING :
				case Cql_Parser.K_COMPACT :
				case Cql_Parser.K_CONTAINS :
				case Cql_Parser.K_COUNT :
				case Cql_Parser.K_COUNTER :
				case Cql_Parser.K_CUSTOM :
				case Cql_Parser.K_DATE :
				case Cql_Parser.K_DECIMAL :
				case Cql_Parser.K_DISTINCT :
				case Cql_Parser.K_DOUBLE :
				case Cql_Parser.K_DURATION :
				case Cql_Parser.K_EXISTS :
				case Cql_Parser.K_FILTERING :
				case Cql_Parser.K_FINALFUNC :
				case Cql_Parser.K_FLOAT :
				case Cql_Parser.K_FROZEN :
				case Cql_Parser.K_FUNCTION :
				case Cql_Parser.K_FUNCTIONS :
				case Cql_Parser.K_GROUP :
				case Cql_Parser.K_INET :
				case Cql_Parser.K_INITCOND :
				case Cql_Parser.K_INPUT :
				case Cql_Parser.K_INT :
				case Cql_Parser.K_JSON :
				case Cql_Parser.K_KEY :
				case Cql_Parser.K_KEYS :
				case Cql_Parser.K_KEYSPACES :
				case Cql_Parser.K_LANGUAGE :
				case Cql_Parser.K_LIKE :
				case Cql_Parser.K_LIST :
				case Cql_Parser.K_LOGIN :
				case Cql_Parser.K_MAP :
				case Cql_Parser.K_NOLOGIN :
				case Cql_Parser.K_NOSUPERUSER :
				case Cql_Parser.K_OPTIONS :
				case Cql_Parser.K_PARTITION :
				case Cql_Parser.K_PASSWORD :
				case Cql_Parser.K_PER :
				case Cql_Parser.K_PERMISSION :
				case Cql_Parser.K_PERMISSIONS :
				case Cql_Parser.K_RETURNS :
				case Cql_Parser.K_ROLE :
				case Cql_Parser.K_ROLES :
				case Cql_Parser.K_SFUNC :
				case Cql_Parser.K_SMALLINT :
				case Cql_Parser.K_STATIC :
				case Cql_Parser.K_STORAGE :
				case Cql_Parser.K_STYPE :
				case Cql_Parser.K_SUPERUSER :
				case Cql_Parser.K_TEXT :
				case Cql_Parser.K_TIME :
				case Cql_Parser.K_TIMESTAMP :
				case Cql_Parser.K_TIMEUUID :
				case Cql_Parser.K_TINYINT :
				case Cql_Parser.K_TRIGGER :
				case Cql_Parser.K_TTL :
				case Cql_Parser.K_TUPLE :
				case Cql_Parser.K_TYPE :
				case Cql_Parser.K_USER :
				case Cql_Parser.K_USERS :
				case Cql_Parser.K_UUID :
				case Cql_Parser.K_VALUES :
				case Cql_Parser.K_VARCHAR :
				case Cql_Parser.K_VARINT :
				case Cql_Parser.K_WRITETIME :
					{
						alt158 = 3;
					}
					break;
				case Cql_Parser.QMARK :
					{
						alt158 = 4;
					}
					break;
				default :
					NoViableAltException nvae = new NoViableAltException("", 158, 0, input);
					throw nvae;
			}
			switch (alt158) {
				case 1 :
					{
						t = ((Token) (match(input, Cql_Parser.IDENT, Cql_Parser.FOLLOW_IDENT_in_cfName8407)));
						name.setColumnFamily((t != null ? t.getText() : null), false);
					}
					break;
				case 2 :
					{
						t = ((Token) (match(input, Cql_Parser.QUOTED_NAME, Cql_Parser.FOLLOW_QUOTED_NAME_in_cfName8432)));
						name.setColumnFamily((t != null ? t.getText() : null), true);
					}
					break;
				case 3 :
					{
						pushFollow(Cql_Parser.FOLLOW_unreserved_keyword_in_cfName8451);
						k = unreserved_keyword();
						(state._fsp)--;
						name.setColumnFamily(k, false);
					}
					break;
				case 4 :
					{
						match(input, Cql_Parser.QMARK, Cql_Parser.FOLLOW_QMARK_in_cfName8461);
						addRecognitionError("Bind variables cannot be used for table names");
					}
					break;
			}
		} catch (RecognitionException re) {
			reportError(re);
			recover(input, re);
		} finally {
		}
	}

	public final void idxName(IndexName name) throws RecognitionException {
		Token t = null;
		String k = null;
		try {
			int alt159 = 4;
			switch (input.LA(1)) {
				case Cql_Parser.IDENT :
					{
						alt159 = 1;
					}
					break;
				case Cql_Parser.QUOTED_NAME :
					{
						alt159 = 2;
					}
					break;
				case Cql_Parser.K_AGGREGATE :
				case Cql_Parser.K_ALL :
				case Cql_Parser.K_AS :
				case Cql_Parser.K_ASCII :
				case Cql_Parser.K_BIGINT :
				case Cql_Parser.K_BLOB :
				case Cql_Parser.K_BOOLEAN :
				case Cql_Parser.K_CALLED :
				case Cql_Parser.K_CAST :
				case Cql_Parser.K_CLUSTERING :
				case Cql_Parser.K_COMPACT :
				case Cql_Parser.K_CONTAINS :
				case Cql_Parser.K_COUNT :
				case Cql_Parser.K_COUNTER :
				case Cql_Parser.K_CUSTOM :
				case Cql_Parser.K_DATE :
				case Cql_Parser.K_DECIMAL :
				case Cql_Parser.K_DISTINCT :
				case Cql_Parser.K_DOUBLE :
				case Cql_Parser.K_DURATION :
				case Cql_Parser.K_EXISTS :
				case Cql_Parser.K_FILTERING :
				case Cql_Parser.K_FINALFUNC :
				case Cql_Parser.K_FLOAT :
				case Cql_Parser.K_FROZEN :
				case Cql_Parser.K_FUNCTION :
				case Cql_Parser.K_FUNCTIONS :
				case Cql_Parser.K_GROUP :
				case Cql_Parser.K_INET :
				case Cql_Parser.K_INITCOND :
				case Cql_Parser.K_INPUT :
				case Cql_Parser.K_INT :
				case Cql_Parser.K_JSON :
				case Cql_Parser.K_KEY :
				case Cql_Parser.K_KEYS :
				case Cql_Parser.K_KEYSPACES :
				case Cql_Parser.K_LANGUAGE :
				case Cql_Parser.K_LIKE :
				case Cql_Parser.K_LIST :
				case Cql_Parser.K_LOGIN :
				case Cql_Parser.K_MAP :
				case Cql_Parser.K_NOLOGIN :
				case Cql_Parser.K_NOSUPERUSER :
				case Cql_Parser.K_OPTIONS :
				case Cql_Parser.K_PARTITION :
				case Cql_Parser.K_PASSWORD :
				case Cql_Parser.K_PER :
				case Cql_Parser.K_PERMISSION :
				case Cql_Parser.K_PERMISSIONS :
				case Cql_Parser.K_RETURNS :
				case Cql_Parser.K_ROLE :
				case Cql_Parser.K_ROLES :
				case Cql_Parser.K_SFUNC :
				case Cql_Parser.K_SMALLINT :
				case Cql_Parser.K_STATIC :
				case Cql_Parser.K_STORAGE :
				case Cql_Parser.K_STYPE :
				case Cql_Parser.K_SUPERUSER :
				case Cql_Parser.K_TEXT :
				case Cql_Parser.K_TIME :
				case Cql_Parser.K_TIMESTAMP :
				case Cql_Parser.K_TIMEUUID :
				case Cql_Parser.K_TINYINT :
				case Cql_Parser.K_TRIGGER :
				case Cql_Parser.K_TTL :
				case Cql_Parser.K_TUPLE :
				case Cql_Parser.K_TYPE :
				case Cql_Parser.K_USER :
				case Cql_Parser.K_USERS :
				case Cql_Parser.K_UUID :
				case Cql_Parser.K_VALUES :
				case Cql_Parser.K_VARCHAR :
				case Cql_Parser.K_VARINT :
				case Cql_Parser.K_WRITETIME :
					{
						alt159 = 3;
					}
					break;
				case Cql_Parser.QMARK :
					{
						alt159 = 4;
					}
					break;
				default :
					NoViableAltException nvae = new NoViableAltException("", 159, 0, input);
					throw nvae;
			}
			switch (alt159) {
				case 1 :
					{
						t = ((Token) (match(input, Cql_Parser.IDENT, Cql_Parser.FOLLOW_IDENT_in_idxName8483)));
						name.setIndex((t != null ? t.getText() : null), false);
					}
					break;
				case 2 :
					{
						t = ((Token) (match(input, Cql_Parser.QUOTED_NAME, Cql_Parser.FOLLOW_QUOTED_NAME_in_idxName8508)));
						name.setIndex((t != null ? t.getText() : null), true);
					}
					break;
				case 3 :
					{
						pushFollow(Cql_Parser.FOLLOW_unreserved_keyword_in_idxName8527);
						k = unreserved_keyword();
						(state._fsp)--;
						name.setIndex(k, false);
					}
					break;
				case 4 :
					{
						match(input, Cql_Parser.QMARK, Cql_Parser.FOLLOW_QMARK_in_idxName8537);
						addRecognitionError("Bind variables cannot be used for index names");
					}
					break;
			}
		} catch (RecognitionException re) {
			reportError(re);
			recover(input, re);
		} finally {
		}
	}

	public final void roleName(RoleName name) throws RecognitionException {
		Token t = null;
		Token s = null;
		String k = null;
		try {
			int alt160 = 5;
			switch (input.LA(1)) {
				case Cql_Parser.IDENT :
					{
						alt160 = 1;
					}
					break;
				case Cql_Parser.STRING_LITERAL :
					{
						alt160 = 2;
					}
					break;
				case Cql_Parser.QUOTED_NAME :
					{
						alt160 = 3;
					}
					break;
				case Cql_Parser.K_AGGREGATE :
				case Cql_Parser.K_ALL :
				case Cql_Parser.K_AS :
				case Cql_Parser.K_ASCII :
				case Cql_Parser.K_BIGINT :
				case Cql_Parser.K_BLOB :
				case Cql_Parser.K_BOOLEAN :
				case Cql_Parser.K_CALLED :
				case Cql_Parser.K_CAST :
				case Cql_Parser.K_CLUSTERING :
				case Cql_Parser.K_COMPACT :
				case Cql_Parser.K_CONTAINS :
				case Cql_Parser.K_COUNT :
				case Cql_Parser.K_COUNTER :
				case Cql_Parser.K_CUSTOM :
				case Cql_Parser.K_DATE :
				case Cql_Parser.K_DECIMAL :
				case Cql_Parser.K_DISTINCT :
				case Cql_Parser.K_DOUBLE :
				case Cql_Parser.K_DURATION :
				case Cql_Parser.K_EXISTS :
				case Cql_Parser.K_FILTERING :
				case Cql_Parser.K_FINALFUNC :
				case Cql_Parser.K_FLOAT :
				case Cql_Parser.K_FROZEN :
				case Cql_Parser.K_FUNCTION :
				case Cql_Parser.K_FUNCTIONS :
				case Cql_Parser.K_GROUP :
				case Cql_Parser.K_INET :
				case Cql_Parser.K_INITCOND :
				case Cql_Parser.K_INPUT :
				case Cql_Parser.K_INT :
				case Cql_Parser.K_JSON :
				case Cql_Parser.K_KEY :
				case Cql_Parser.K_KEYS :
				case Cql_Parser.K_KEYSPACES :
				case Cql_Parser.K_LANGUAGE :
				case Cql_Parser.K_LIKE :
				case Cql_Parser.K_LIST :
				case Cql_Parser.K_LOGIN :
				case Cql_Parser.K_MAP :
				case Cql_Parser.K_NOLOGIN :
				case Cql_Parser.K_NOSUPERUSER :
				case Cql_Parser.K_OPTIONS :
				case Cql_Parser.K_PARTITION :
				case Cql_Parser.K_PASSWORD :
				case Cql_Parser.K_PER :
				case Cql_Parser.K_PERMISSION :
				case Cql_Parser.K_PERMISSIONS :
				case Cql_Parser.K_RETURNS :
				case Cql_Parser.K_ROLE :
				case Cql_Parser.K_ROLES :
				case Cql_Parser.K_SFUNC :
				case Cql_Parser.K_SMALLINT :
				case Cql_Parser.K_STATIC :
				case Cql_Parser.K_STORAGE :
				case Cql_Parser.K_STYPE :
				case Cql_Parser.K_SUPERUSER :
				case Cql_Parser.K_TEXT :
				case Cql_Parser.K_TIME :
				case Cql_Parser.K_TIMESTAMP :
				case Cql_Parser.K_TIMEUUID :
				case Cql_Parser.K_TINYINT :
				case Cql_Parser.K_TRIGGER :
				case Cql_Parser.K_TTL :
				case Cql_Parser.K_TUPLE :
				case Cql_Parser.K_TYPE :
				case Cql_Parser.K_USER :
				case Cql_Parser.K_USERS :
				case Cql_Parser.K_UUID :
				case Cql_Parser.K_VALUES :
				case Cql_Parser.K_VARCHAR :
				case Cql_Parser.K_VARINT :
				case Cql_Parser.K_WRITETIME :
					{
						alt160 = 4;
					}
					break;
				case Cql_Parser.QMARK :
					{
						alt160 = 5;
					}
					break;
				default :
					NoViableAltException nvae = new NoViableAltException("", 160, 0, input);
					throw nvae;
			}
			switch (alt160) {
				case 1 :
					{
						t = ((Token) (match(input, Cql_Parser.IDENT, Cql_Parser.FOLLOW_IDENT_in_roleName8559)));
						name.setName((t != null ? t.getText() : null), false);
					}
					break;
				case 2 :
					{
						s = ((Token) (match(input, Cql_Parser.STRING_LITERAL, Cql_Parser.FOLLOW_STRING_LITERAL_in_roleName8584)));
						name.setName((s != null ? s.getText() : null), true);
					}
					break;
				case 3 :
					{
						t = ((Token) (match(input, Cql_Parser.QUOTED_NAME, Cql_Parser.FOLLOW_QUOTED_NAME_in_roleName8600)));
						name.setName((t != null ? t.getText() : null), true);
					}
					break;
				case 4 :
					{
						pushFollow(Cql_Parser.FOLLOW_unreserved_keyword_in_roleName8619);
						k = unreserved_keyword();
						(state._fsp)--;
						name.setName(k, false);
					}
					break;
				case 5 :
					{
						match(input, Cql_Parser.QMARK, Cql_Parser.FOLLOW_QMARK_in_roleName8629);
						addRecognitionError("Bind variables cannot be used for role names");
					}
					break;
			}
		} catch (RecognitionException re) {
			reportError(re);
			recover(input, re);
		} finally {
		}
	}

	public final Constants.Literal constant() throws RecognitionException {
		Constants.Literal constant = null;
		Token t = null;
		try {
			int alt162 = 8;
			switch (input.LA(1)) {
				case Cql_Parser.STRING_LITERAL :
					{
						alt162 = 1;
					}
					break;
				case Cql_Parser.INTEGER :
					{
						alt162 = 2;
					}
					break;
				case Cql_Parser.FLOAT :
					{
						alt162 = 3;
					}
					break;
				case Cql_Parser.BOOLEAN :
					{
						alt162 = 4;
					}
					break;
				case Cql_Parser.DURATION :
					{
						alt162 = 5;
					}
					break;
				case Cql_Parser.UUID :
					{
						alt162 = 6;
					}
					break;
				case Cql_Parser.HEXNUMBER :
					{
						alt162 = 7;
					}
					break;
				case Cql_Parser.K_INFINITY :
				case Cql_Parser.K_NAN :
				case 189 :
					{
						alt162 = 8;
					}
					break;
				default :
					NoViableAltException nvae = new NoViableAltException("", 162, 0, input);
					throw nvae;
			}
			switch (alt162) {
				case 1 :
					{
						t = ((Token) (match(input, Cql_Parser.STRING_LITERAL, Cql_Parser.FOLLOW_STRING_LITERAL_in_constant8654)));
						constant = string((t != null ? t.getText() : null));
					}
					break;
				case 2 :
					{
						t = ((Token) (match(input, Cql_Parser.INTEGER, Cql_Parser.FOLLOW_INTEGER_in_constant8666)));
						constant = integer((t != null ? t.getText() : null));
					}
					break;
				case 3 :
					{
						t = ((Token) (match(input, Cql_Parser.FLOAT, Cql_Parser.FOLLOW_FLOAT_in_constant8685)));
						constant = floatingPoint((t != null ? t.getText() : null));
					}
					break;
				case 4 :
					{
						t = ((Token) (match(input, Cql_Parser.BOOLEAN, Cql_Parser.FOLLOW_BOOLEAN_in_constant8706)));
						constant = bool((t != null ? t.getText() : null));
					}
					break;
				case 5 :
					{
						t = ((Token) (match(input, Cql_Parser.DURATION, Cql_Parser.FOLLOW_DURATION_in_constant8725)));
						constant = duration((t != null ? t.getText() : null));
					}
					break;
				case 6 :
					{
						t = ((Token) (match(input, Cql_Parser.UUID, Cql_Parser.FOLLOW_UUID_in_constant8743)));
						constant = uuid((t != null ? t.getText() : null));
					}
					break;
				case 7 :
					{
						t = ((Token) (match(input, Cql_Parser.HEXNUMBER, Cql_Parser.FOLLOW_HEXNUMBER_in_constant8765)));
						constant = hex((t != null ? t.getText() : null));
					}
					break;
				case 8 :
					{
						String sign = "";
						int alt161 = 2;
						int LA161_0 = input.LA(1);
						if (LA161_0 == 189) {
							alt161 = 1;
						}
						switch (alt161) {
							case 1 :
								{
									match(input, 189, Cql_Parser.FOLLOW_189_in_constant8783);
									sign = "-";
								}
								break;
						}
						t = input.LT(1);
						if (((input.LA(1)) == (Cql_Parser.K_INFINITY)) || ((input.LA(1)) == (Cql_Parser.K_NAN))) {
							input.consume();
							state.errorRecovery = false;
						}else {
							MismatchedSetException mse = new MismatchedSetException(null, input);
							throw mse;
						}
						constant = floatingPoint((sign + (t != null ? t.getText() : null)));
					}
					break;
			}
		} catch (RecognitionException re) {
			reportError(re);
			recover(input, re);
		} finally {
		}
		return constant;
	}

	public final Maps.Literal mapLiteral() throws RecognitionException {
		Maps.Literal map = null;
		Term.Raw k1 = null;
		Term.Raw v1 = null;
		Term.Raw kn = null;
		Term.Raw vn = null;
		try {
			{
				match(input, 203, Cql_Parser.FOLLOW_203_in_mapLiteral8821);
				List<Pair<Term.Raw, Term.Raw>> m = new ArrayList<Pair<Term.Raw, Term.Raw>>();
				int alt164 = 2;
				int LA164_0 = input.LA(1);
				if ((((((((((((((((((((((((((((((((((((((((((((LA164_0 == (Cql_Parser.BOOLEAN)) || (LA164_0 == (Cql_Parser.DURATION))) || (LA164_0 == (Cql_Parser.FLOAT))) || (LA164_0 == (Cql_Parser.HEXNUMBER))) || ((LA164_0 >= (Cql_Parser.IDENT)) && (LA164_0 <= (Cql_Parser.INTEGER)))) || ((LA164_0 >= (Cql_Parser.K_AGGREGATE)) && (LA164_0 <= (Cql_Parser.K_ALL)))) || (LA164_0 == (Cql_Parser.K_AS))) || (LA164_0 == (Cql_Parser.K_ASCII))) || ((LA164_0 >= (Cql_Parser.K_BIGINT)) && (LA164_0 <= (Cql_Parser.K_BOOLEAN)))) || ((LA164_0 >= (Cql_Parser.K_CALLED)) && (LA164_0 <= (Cql_Parser.K_CLUSTERING)))) || ((LA164_0 >= (Cql_Parser.K_COMPACT)) && (LA164_0 <= (Cql_Parser.K_COUNTER)))) || ((LA164_0 >= (Cql_Parser.K_CUSTOM)) && (LA164_0 <= (Cql_Parser.K_DECIMAL)))) || ((LA164_0 >= (Cql_Parser.K_DISTINCT)) && (LA164_0 <= (Cql_Parser.K_DOUBLE)))) || (LA164_0 == (Cql_Parser.K_DURATION))) || ((LA164_0 >= (Cql_Parser.K_EXISTS)) && (LA164_0 <= (Cql_Parser.K_FLOAT)))) || (LA164_0 == (Cql_Parser.K_FROZEN))) || ((LA164_0 >= (Cql_Parser.K_FUNCTION)) && (LA164_0 <= (Cql_Parser.K_FUNCTIONS)))) || (LA164_0 == (Cql_Parser.K_GROUP))) || ((LA164_0 >= (Cql_Parser.K_INET)) && (LA164_0 <= (Cql_Parser.K_INPUT)))) || (LA164_0 == (Cql_Parser.K_INT))) || ((LA164_0 >= (Cql_Parser.K_JSON)) && (LA164_0 <= (Cql_Parser.K_KEYS)))) || ((LA164_0 >= (Cql_Parser.K_KEYSPACES)) && (LA164_0 <= (Cql_Parser.K_LIKE)))) || ((LA164_0 >= (Cql_Parser.K_LIST)) && (LA164_0 <= (Cql_Parser.K_MAP)))) || ((LA164_0 >= (Cql_Parser.K_NAN)) && (LA164_0 <= (Cql_Parser.K_NOLOGIN)))) || (LA164_0 == (Cql_Parser.K_NOSUPERUSER))) || (LA164_0 == (Cql_Parser.K_NULL))) || (LA164_0 == (Cql_Parser.K_OPTIONS))) || ((LA164_0 >= (Cql_Parser.K_PARTITION)) && (LA164_0 <= (Cql_Parser.K_PERMISSIONS)))) || (LA164_0 == (Cql_Parser.K_RETURNS))) || ((LA164_0 >= (Cql_Parser.K_ROLE)) && (LA164_0 <= (Cql_Parser.K_ROLES)))) || ((LA164_0 >= (Cql_Parser.K_SFUNC)) && (LA164_0 <= (Cql_Parser.K_TINYINT)))) || ((LA164_0 >= (Cql_Parser.K_TOKEN)) && (LA164_0 <= (Cql_Parser.K_TRIGGER)))) || ((LA164_0 >= (Cql_Parser.K_TTL)) && (LA164_0 <= (Cql_Parser.K_TYPE)))) || ((LA164_0 >= (Cql_Parser.K_USER)) && (LA164_0 <= (Cql_Parser.K_USERS)))) || ((LA164_0 >= (Cql_Parser.K_UUID)) && (LA164_0 <= (Cql_Parser.K_VARINT)))) || (LA164_0 == (Cql_Parser.K_WRITETIME))) || ((LA164_0 >= (Cql_Parser.QMARK)) && (LA164_0 <= (Cql_Parser.QUOTED_NAME)))) || (LA164_0 == (Cql_Parser.STRING_LITERAL))) || (LA164_0 == (Cql_Parser.UUID))) || (LA164_0 == 184)) || (LA164_0 == 189)) || (LA164_0 == 192)) || (LA164_0 == 199)) || (LA164_0 == 203)) {
					alt164 = 1;
				}
				switch (alt164) {
					case 1 :
						{
							pushFollow(Cql_Parser.FOLLOW_term_in_mapLiteral8839);
							k1 = term();
							(state._fsp)--;
							match(input, 192, Cql_Parser.FOLLOW_192_in_mapLiteral8841);
							pushFollow(Cql_Parser.FOLLOW_term_in_mapLiteral8845);
							v1 = term();
							(state._fsp)--;
							m.add(Pair.create(k1, v1));
							loop163 : while (true) {
								int alt163 = 2;
								int LA163_0 = input.LA(1);
								if (LA163_0 == 188) {
									alt163 = 1;
								}
								switch (alt163) {
									case 1 :
										{
											match(input, 188, Cql_Parser.FOLLOW_188_in_mapLiteral8851);
											pushFollow(Cql_Parser.FOLLOW_term_in_mapLiteral8855);
											kn = term();
											(state._fsp)--;
											match(input, 192, Cql_Parser.FOLLOW_192_in_mapLiteral8857);
											pushFollow(Cql_Parser.FOLLOW_term_in_mapLiteral8861);
											vn = term();
											(state._fsp)--;
											m.add(Pair.create(kn, vn));
										}
										break;
									default :
										break loop163;
								}
							} 
						}
						break;
				}
				match(input, 204, Cql_Parser.FOLLOW_204_in_mapLiteral8877);
				map = new Maps.Literal(m);
			}
		} catch (RecognitionException re) {
			reportError(re);
			recover(input, re);
		} finally {
		}
		return map;
	}

	public final Term.Raw setOrMapLiteral(Term.Raw t) throws RecognitionException {
		Term.Raw value = null;
		Term.Raw v = null;
		Term.Raw kn = null;
		Term.Raw vn = null;
		Term.Raw tn = null;
		try {
			int alt167 = 2;
			int LA167_0 = input.LA(1);
			if (LA167_0 == 192) {
				alt167 = 1;
			}else
				if ((LA167_0 == 188) || (LA167_0 == 204)) {
					alt167 = 2;
				}else {
					NoViableAltException nvae = new NoViableAltException("", 167, 0, input);
					throw nvae;
				}

			switch (alt167) {
				case 1 :
					{
						match(input, 192, Cql_Parser.FOLLOW_192_in_setOrMapLiteral8901);
						pushFollow(Cql_Parser.FOLLOW_term_in_setOrMapLiteral8905);
						v = term();
						(state._fsp)--;
						List<Pair<Term.Raw, Term.Raw>> m = new ArrayList<Pair<Term.Raw, Term.Raw>>();
						m.add(Pair.create(t, v));
						loop165 : while (true) {
							int alt165 = 2;
							int LA165_0 = input.LA(1);
							if (LA165_0 == 188) {
								alt165 = 1;
							}
							switch (alt165) {
								case 1 :
									{
										match(input, 188, Cql_Parser.FOLLOW_188_in_setOrMapLiteral8921);
										pushFollow(Cql_Parser.FOLLOW_term_in_setOrMapLiteral8925);
										kn = term();
										(state._fsp)--;
										match(input, 192, Cql_Parser.FOLLOW_192_in_setOrMapLiteral8927);
										pushFollow(Cql_Parser.FOLLOW_term_in_setOrMapLiteral8931);
										vn = term();
										(state._fsp)--;
										m.add(Pair.create(kn, vn));
									}
									break;
								default :
									break loop165;
							}
						} 
						value = new Maps.Literal(m);
					}
					break;
				case 2 :
					{
						List<Term.Raw> s = new ArrayList<Term.Raw>();
						s.add(t);
						loop166 : while (true) {
							int alt166 = 2;
							int LA166_0 = input.LA(1);
							if (LA166_0 == 188) {
								alt166 = 1;
							}
							switch (alt166) {
								case 1 :
									{
										match(input, 188, Cql_Parser.FOLLOW_188_in_setOrMapLiteral8966);
										pushFollow(Cql_Parser.FOLLOW_term_in_setOrMapLiteral8970);
										tn = term();
										(state._fsp)--;
										s.add(tn);
									}
									break;
								default :
									break loop166;
							}
						} 
						value = new Sets.Literal(s);
					}
					break;
			}
		} catch (RecognitionException re) {
			reportError(re);
			recover(input, re);
		} finally {
		}
		return value;
	}

	public final Term.Raw collectionLiteral() throws RecognitionException {
		Term.Raw value = null;
		Term.Raw t1 = null;
		Term.Raw tn = null;
		Term.Raw t = null;
		Term.Raw v = null;
		try {
			int alt170 = 3;
			int LA170_0 = input.LA(1);
			if (LA170_0 == 199) {
				alt170 = 1;
			}else
				if (LA170_0 == 203) {
					int LA170_2 = input.LA(2);
					if (LA170_2 == 204) {
						alt170 = 3;
					}else
						if ((((((((((((((((((((((((((((((((((((((((((((LA170_2 == (Cql_Parser.BOOLEAN)) || (LA170_2 == (Cql_Parser.DURATION))) || (LA170_2 == (Cql_Parser.FLOAT))) || (LA170_2 == (Cql_Parser.HEXNUMBER))) || ((LA170_2 >= (Cql_Parser.IDENT)) && (LA170_2 <= (Cql_Parser.INTEGER)))) || ((LA170_2 >= (Cql_Parser.K_AGGREGATE)) && (LA170_2 <= (Cql_Parser.K_ALL)))) || (LA170_2 == (Cql_Parser.K_AS))) || (LA170_2 == (Cql_Parser.K_ASCII))) || ((LA170_2 >= (Cql_Parser.K_BIGINT)) && (LA170_2 <= (Cql_Parser.K_BOOLEAN)))) || ((LA170_2 >= (Cql_Parser.K_CALLED)) && (LA170_2 <= (Cql_Parser.K_CLUSTERING)))) || ((LA170_2 >= (Cql_Parser.K_COMPACT)) && (LA170_2 <= (Cql_Parser.K_COUNTER)))) || ((LA170_2 >= (Cql_Parser.K_CUSTOM)) && (LA170_2 <= (Cql_Parser.K_DECIMAL)))) || ((LA170_2 >= (Cql_Parser.K_DISTINCT)) && (LA170_2 <= (Cql_Parser.K_DOUBLE)))) || (LA170_2 == (Cql_Parser.K_DURATION))) || ((LA170_2 >= (Cql_Parser.K_EXISTS)) && (LA170_2 <= (Cql_Parser.K_FLOAT)))) || (LA170_2 == (Cql_Parser.K_FROZEN))) || ((LA170_2 >= (Cql_Parser.K_FUNCTION)) && (LA170_2 <= (Cql_Parser.K_FUNCTIONS)))) || (LA170_2 == (Cql_Parser.K_GROUP))) || ((LA170_2 >= (Cql_Parser.K_INET)) && (LA170_2 <= (Cql_Parser.K_INPUT)))) || (LA170_2 == (Cql_Parser.K_INT))) || ((LA170_2 >= (Cql_Parser.K_JSON)) && (LA170_2 <= (Cql_Parser.K_KEYS)))) || ((LA170_2 >= (Cql_Parser.K_KEYSPACES)) && (LA170_2 <= (Cql_Parser.K_LIKE)))) || ((LA170_2 >= (Cql_Parser.K_LIST)) && (LA170_2 <= (Cql_Parser.K_MAP)))) || ((LA170_2 >= (Cql_Parser.K_NAN)) && (LA170_2 <= (Cql_Parser.K_NOLOGIN)))) || (LA170_2 == (Cql_Parser.K_NOSUPERUSER))) || (LA170_2 == (Cql_Parser.K_NULL))) || (LA170_2 == (Cql_Parser.K_OPTIONS))) || ((LA170_2 >= (Cql_Parser.K_PARTITION)) && (LA170_2 <= (Cql_Parser.K_PERMISSIONS)))) || (LA170_2 == (Cql_Parser.K_RETURNS))) || ((LA170_2 >= (Cql_Parser.K_ROLE)) && (LA170_2 <= (Cql_Parser.K_ROLES)))) || ((LA170_2 >= (Cql_Parser.K_SFUNC)) && (LA170_2 <= (Cql_Parser.K_TINYINT)))) || ((LA170_2 >= (Cql_Parser.K_TOKEN)) && (LA170_2 <= (Cql_Parser.K_TRIGGER)))) || ((LA170_2 >= (Cql_Parser.K_TTL)) && (LA170_2 <= (Cql_Parser.K_TYPE)))) || ((LA170_2 >= (Cql_Parser.K_USER)) && (LA170_2 <= (Cql_Parser.K_USERS)))) || ((LA170_2 >= (Cql_Parser.K_UUID)) && (LA170_2 <= (Cql_Parser.K_VARINT)))) || (LA170_2 == (Cql_Parser.K_WRITETIME))) || ((LA170_2 >= (Cql_Parser.QMARK)) && (LA170_2 <= (Cql_Parser.QUOTED_NAME)))) || (LA170_2 == (Cql_Parser.STRING_LITERAL))) || (LA170_2 == (Cql_Parser.UUID))) || (LA170_2 == 184)) || (LA170_2 == 189)) || (LA170_2 == 192)) || (LA170_2 == 199)) || (LA170_2 == 203)) {
							alt170 = 2;
						}else {
							int nvaeMark = input.mark();
							try {
								input.consume();
								NoViableAltException nvae = new NoViableAltException("", 170, 2, input);
								throw nvae;
							} finally {
								input.rewind(nvaeMark);
							}
						}

				}else {
					NoViableAltException nvae = new NoViableAltException("", 170, 0, input);
					throw nvae;
				}

			switch (alt170) {
				case 1 :
					{
						match(input, 199, Cql_Parser.FOLLOW_199_in_collectionLiteral9004);
						List<Term.Raw> l = new ArrayList<Term.Raw>();
						int alt169 = 2;
						int LA169_0 = input.LA(1);
						if ((((((((((((((((((((((((((((((((((((((((((((LA169_0 == (Cql_Parser.BOOLEAN)) || (LA169_0 == (Cql_Parser.DURATION))) || (LA169_0 == (Cql_Parser.FLOAT))) || (LA169_0 == (Cql_Parser.HEXNUMBER))) || ((LA169_0 >= (Cql_Parser.IDENT)) && (LA169_0 <= (Cql_Parser.INTEGER)))) || ((LA169_0 >= (Cql_Parser.K_AGGREGATE)) && (LA169_0 <= (Cql_Parser.K_ALL)))) || (LA169_0 == (Cql_Parser.K_AS))) || (LA169_0 == (Cql_Parser.K_ASCII))) || ((LA169_0 >= (Cql_Parser.K_BIGINT)) && (LA169_0 <= (Cql_Parser.K_BOOLEAN)))) || ((LA169_0 >= (Cql_Parser.K_CALLED)) && (LA169_0 <= (Cql_Parser.K_CLUSTERING)))) || ((LA169_0 >= (Cql_Parser.K_COMPACT)) && (LA169_0 <= (Cql_Parser.K_COUNTER)))) || ((LA169_0 >= (Cql_Parser.K_CUSTOM)) && (LA169_0 <= (Cql_Parser.K_DECIMAL)))) || ((LA169_0 >= (Cql_Parser.K_DISTINCT)) && (LA169_0 <= (Cql_Parser.K_DOUBLE)))) || (LA169_0 == (Cql_Parser.K_DURATION))) || ((LA169_0 >= (Cql_Parser.K_EXISTS)) && (LA169_0 <= (Cql_Parser.K_FLOAT)))) || (LA169_0 == (Cql_Parser.K_FROZEN))) || ((LA169_0 >= (Cql_Parser.K_FUNCTION)) && (LA169_0 <= (Cql_Parser.K_FUNCTIONS)))) || (LA169_0 == (Cql_Parser.K_GROUP))) || ((LA169_0 >= (Cql_Parser.K_INET)) && (LA169_0 <= (Cql_Parser.K_INPUT)))) || (LA169_0 == (Cql_Parser.K_INT))) || ((LA169_0 >= (Cql_Parser.K_JSON)) && (LA169_0 <= (Cql_Parser.K_KEYS)))) || ((LA169_0 >= (Cql_Parser.K_KEYSPACES)) && (LA169_0 <= (Cql_Parser.K_LIKE)))) || ((LA169_0 >= (Cql_Parser.K_LIST)) && (LA169_0 <= (Cql_Parser.K_MAP)))) || ((LA169_0 >= (Cql_Parser.K_NAN)) && (LA169_0 <= (Cql_Parser.K_NOLOGIN)))) || (LA169_0 == (Cql_Parser.K_NOSUPERUSER))) || (LA169_0 == (Cql_Parser.K_NULL))) || (LA169_0 == (Cql_Parser.K_OPTIONS))) || ((LA169_0 >= (Cql_Parser.K_PARTITION)) && (LA169_0 <= (Cql_Parser.K_PERMISSIONS)))) || (LA169_0 == (Cql_Parser.K_RETURNS))) || ((LA169_0 >= (Cql_Parser.K_ROLE)) && (LA169_0 <= (Cql_Parser.K_ROLES)))) || ((LA169_0 >= (Cql_Parser.K_SFUNC)) && (LA169_0 <= (Cql_Parser.K_TINYINT)))) || ((LA169_0 >= (Cql_Parser.K_TOKEN)) && (LA169_0 <= (Cql_Parser.K_TRIGGER)))) || ((LA169_0 >= (Cql_Parser.K_TTL)) && (LA169_0 <= (Cql_Parser.K_TYPE)))) || ((LA169_0 >= (Cql_Parser.K_USER)) && (LA169_0 <= (Cql_Parser.K_USERS)))) || ((LA169_0 >= (Cql_Parser.K_UUID)) && (LA169_0 <= (Cql_Parser.K_VARINT)))) || (LA169_0 == (Cql_Parser.K_WRITETIME))) || ((LA169_0 >= (Cql_Parser.QMARK)) && (LA169_0 <= (Cql_Parser.QUOTED_NAME)))) || (LA169_0 == (Cql_Parser.STRING_LITERAL))) || (LA169_0 == (Cql_Parser.UUID))) || (LA169_0 == 184)) || (LA169_0 == 189)) || (LA169_0 == 192)) || (LA169_0 == 199)) || (LA169_0 == 203)) {
							alt169 = 1;
						}
						switch (alt169) {
							case 1 :
								{
									pushFollow(Cql_Parser.FOLLOW_term_in_collectionLiteral9022);
									t1 = term();
									(state._fsp)--;
									l.add(t1);
									loop168 : while (true) {
										int alt168 = 2;
										int LA168_0 = input.LA(1);
										if (LA168_0 == 188) {
											alt168 = 1;
										}
										switch (alt168) {
											case 1 :
												{
													match(input, 188, Cql_Parser.FOLLOW_188_in_collectionLiteral9028);
													pushFollow(Cql_Parser.FOLLOW_term_in_collectionLiteral9032);
													tn = term();
													(state._fsp)--;
													l.add(tn);
												}
												break;
											default :
												break loop168;
										}
									} 
								}
								break;
						}
						match(input, 201, Cql_Parser.FOLLOW_201_in_collectionLiteral9048);
						value = new Lists.Literal(l);
					}
					break;
				case 2 :
					{
						match(input, 203, Cql_Parser.FOLLOW_203_in_collectionLiteral9058);
						pushFollow(Cql_Parser.FOLLOW_term_in_collectionLiteral9062);
						t = term();
						(state._fsp)--;
						pushFollow(Cql_Parser.FOLLOW_setOrMapLiteral_in_collectionLiteral9066);
						v = setOrMapLiteral(t);
						(state._fsp)--;
						value = v;
						match(input, 204, Cql_Parser.FOLLOW_204_in_collectionLiteral9071);
					}
					break;
				case 3 :
					{
						match(input, 203, Cql_Parser.FOLLOW_203_in_collectionLiteral9089);
						match(input, 204, Cql_Parser.FOLLOW_204_in_collectionLiteral9091);
						value = new Sets.Literal(Collections.<Term.Raw>emptyList());
					}
					break;
			}
		} catch (RecognitionException re) {
			reportError(re);
			recover(input, re);
		} finally {
		}
		return value;
	}

	public final UserTypes.Literal usertypeLiteral() throws RecognitionException {
		UserTypes.Literal ut = null;
		FieldIdentifier k1 = null;
		Term.Raw v1 = null;
		FieldIdentifier kn = null;
		Term.Raw vn = null;
		Map<FieldIdentifier, Term.Raw> m = new HashMap<>();
		try {
			{
				match(input, 203, Cql_Parser.FOLLOW_203_in_usertypeLiteral9135);
				pushFollow(Cql_Parser.FOLLOW_fident_in_usertypeLiteral9139);
				k1 = fident();
				(state._fsp)--;
				match(input, 192, Cql_Parser.FOLLOW_192_in_usertypeLiteral9141);
				pushFollow(Cql_Parser.FOLLOW_term_in_usertypeLiteral9145);
				v1 = term();
				(state._fsp)--;
				m.put(k1, v1);
				loop171 : while (true) {
					int alt171 = 2;
					int LA171_0 = input.LA(1);
					if (LA171_0 == 188) {
						alt171 = 1;
					}
					switch (alt171) {
						case 1 :
							{
								match(input, 188, Cql_Parser.FOLLOW_188_in_usertypeLiteral9151);
								pushFollow(Cql_Parser.FOLLOW_fident_in_usertypeLiteral9155);
								kn = fident();
								(state._fsp)--;
								match(input, 192, Cql_Parser.FOLLOW_192_in_usertypeLiteral9157);
								pushFollow(Cql_Parser.FOLLOW_term_in_usertypeLiteral9161);
								vn = term();
								(state._fsp)--;
								m.put(kn, vn);
							}
							break;
						default :
							break loop171;
					}
				} 
				match(input, 204, Cql_Parser.FOLLOW_204_in_usertypeLiteral9168);
			}
			ut = new UserTypes.Literal(m);
		} catch (RecognitionException re) {
			reportError(re);
			recover(input, re);
		} finally {
		}
		return ut;
	}

	public final Tuples.Literal tupleLiteral() throws RecognitionException {
		Tuples.Literal tt = null;
		Term.Raw t1 = null;
		Term.Raw tn = null;
		List<Term.Raw> l = new ArrayList<Term.Raw>();
		try {
			{
				match(input, 184, Cql_Parser.FOLLOW_184_in_tupleLiteral9205);
				pushFollow(Cql_Parser.FOLLOW_term_in_tupleLiteral9209);
				t1 = term();
				(state._fsp)--;
				l.add(t1);
				loop172 : while (true) {
					int alt172 = 2;
					int LA172_0 = input.LA(1);
					if (LA172_0 == 188) {
						alt172 = 1;
					}
					switch (alt172) {
						case 1 :
							{
								match(input, 188, Cql_Parser.FOLLOW_188_in_tupleLiteral9215);
								pushFollow(Cql_Parser.FOLLOW_term_in_tupleLiteral9219);
								tn = term();
								(state._fsp)--;
								l.add(tn);
							}
							break;
						default :
							break loop172;
					}
				} 
				match(input, 185, Cql_Parser.FOLLOW_185_in_tupleLiteral9226);
			}
			tt = new Tuples.Literal(l);
		} catch (RecognitionException re) {
			reportError(re);
			recover(input, re);
		} finally {
		}
		return tt;
	}

	public final Term.Raw value() throws RecognitionException {
		Term.Raw value = null;
		Constants.Literal c = null;
		Term.Raw l = null;
		UserTypes.Literal u = null;
		Tuples.Literal t = null;
		ColumnIdentifier id = null;
		try {
			int alt173 = 7;
			alt173 = dfa173.predict(input);
			switch (alt173) {
				case 1 :
					{
						pushFollow(Cql_Parser.FOLLOW_constant_in_value9249);
						c = constant();
						(state._fsp)--;
						value = c;
					}
					break;
				case 2 :
					{
						pushFollow(Cql_Parser.FOLLOW_collectionLiteral_in_value9271);
						l = collectionLiteral();
						(state._fsp)--;
						value = l;
					}
					break;
				case 3 :
					{
						pushFollow(Cql_Parser.FOLLOW_usertypeLiteral_in_value9284);
						u = usertypeLiteral();
						(state._fsp)--;
						value = u;
					}
					break;
				case 4 :
					{
						pushFollow(Cql_Parser.FOLLOW_tupleLiteral_in_value9299);
						t = tupleLiteral();
						(state._fsp)--;
						value = t;
					}
					break;
				case 5 :
					{
						match(input, Cql_Parser.K_NULL, Cql_Parser.FOLLOW_K_NULL_in_value9315);
						value = Constants.NULL_LITERAL;
					}
					break;
				case 6 :
					{
						match(input, 192, Cql_Parser.FOLLOW_192_in_value9339);
						pushFollow(Cql_Parser.FOLLOW_noncol_ident_in_value9343);
						id = noncol_ident();
						(state._fsp)--;
						value = newBindVariables(id);
					}
					break;
				case 7 :
					{
						match(input, Cql_Parser.QMARK, Cql_Parser.FOLLOW_QMARK_in_value9354);
						value = newBindVariables(null);
					}
					break;
			}
		} catch (RecognitionException re) {
			reportError(re);
			recover(input, re);
		} finally {
		}
		return value;
	}

	public final Term.Raw intValue() throws RecognitionException {
		Term.Raw value = null;
		Token t = null;
		ColumnIdentifier id = null;
		try {
			int alt174 = 3;
			switch (input.LA(1)) {
				case Cql_Parser.INTEGER :
					{
						alt174 = 1;
					}
					break;
				case 192 :
					{
						alt174 = 2;
					}
					break;
				case Cql_Parser.QMARK :
					{
						alt174 = 3;
					}
					break;
				default :
					NoViableAltException nvae = new NoViableAltException("", 174, 0, input);
					throw nvae;
			}
			switch (alt174) {
				case 1 :
					{
						t = ((Token) (match(input, Cql_Parser.INTEGER, Cql_Parser.FOLLOW_INTEGER_in_intValue9394)));
						value = integer((t != null ? t.getText() : null));
					}
					break;
				case 2 :
					{
						match(input, 192, Cql_Parser.FOLLOW_192_in_intValue9408);
						pushFollow(Cql_Parser.FOLLOW_noncol_ident_in_intValue9412);
						id = noncol_ident();
						(state._fsp)--;
						value = newBindVariables(id);
					}
					break;
				case 3 :
					{
						match(input, Cql_Parser.QMARK, Cql_Parser.FOLLOW_QMARK_in_intValue9423);
						value = newBindVariables(null);
					}
					break;
			}
		} catch (RecognitionException re) {
			reportError(re);
			recover(input, re);
		} finally {
		}
		return value;
	}

	public final FunctionName functionName() throws RecognitionException {
		FunctionName s = null;
		String ks = null;
		String f = null;
		try {
			{
				int alt175 = 2;
				alt175 = dfa175.predict(input);
				switch (alt175) {
					case 1 :
						{
							pushFollow(Cql_Parser.FOLLOW_keyspaceName_in_functionName9469);
							ks = keyspaceName();
							(state._fsp)--;
							match(input, 191, Cql_Parser.FOLLOW_191_in_functionName9471);
						}
						break;
				}
				pushFollow(Cql_Parser.FOLLOW_allowedFunctionName_in_functionName9477);
				f = allowedFunctionName();
				(state._fsp)--;
				s = (f == null) ? null : new FunctionName(ks, f);
			}
		} catch (RecognitionException re) {
			reportError(re);
			recover(input, re);
		} finally {
		}
		return s;
	}

	public final String allowedFunctionName() throws RecognitionException {
		String s = null;
		Token f = null;
		String u = null;
		try {
			int alt176 = 5;
			switch (input.LA(1)) {
				case Cql_Parser.IDENT :
					{
						alt176 = 1;
					}
					break;
				case Cql_Parser.QUOTED_NAME :
					{
						alt176 = 2;
					}
					break;
				case Cql_Parser.K_AGGREGATE :
				case Cql_Parser.K_ALL :
				case Cql_Parser.K_AS :
				case Cql_Parser.K_ASCII :
				case Cql_Parser.K_BIGINT :
				case Cql_Parser.K_BLOB :
				case Cql_Parser.K_BOOLEAN :
				case Cql_Parser.K_CALLED :
				case Cql_Parser.K_CLUSTERING :
				case Cql_Parser.K_COMPACT :
				case Cql_Parser.K_CONTAINS :
				case Cql_Parser.K_COUNTER :
				case Cql_Parser.K_CUSTOM :
				case Cql_Parser.K_DATE :
				case Cql_Parser.K_DECIMAL :
				case Cql_Parser.K_DOUBLE :
				case Cql_Parser.K_DURATION :
				case Cql_Parser.K_EXISTS :
				case Cql_Parser.K_FILTERING :
				case Cql_Parser.K_FINALFUNC :
				case Cql_Parser.K_FLOAT :
				case Cql_Parser.K_FROZEN :
				case Cql_Parser.K_FUNCTION :
				case Cql_Parser.K_FUNCTIONS :
				case Cql_Parser.K_GROUP :
				case Cql_Parser.K_INET :
				case Cql_Parser.K_INITCOND :
				case Cql_Parser.K_INPUT :
				case Cql_Parser.K_INT :
				case Cql_Parser.K_KEYS :
				case Cql_Parser.K_KEYSPACES :
				case Cql_Parser.K_LANGUAGE :
				case Cql_Parser.K_LIKE :
				case Cql_Parser.K_LIST :
				case Cql_Parser.K_LOGIN :
				case Cql_Parser.K_MAP :
				case Cql_Parser.K_NOLOGIN :
				case Cql_Parser.K_NOSUPERUSER :
				case Cql_Parser.K_OPTIONS :
				case Cql_Parser.K_PARTITION :
				case Cql_Parser.K_PASSWORD :
				case Cql_Parser.K_PER :
				case Cql_Parser.K_PERMISSION :
				case Cql_Parser.K_PERMISSIONS :
				case Cql_Parser.K_RETURNS :
				case Cql_Parser.K_ROLE :
				case Cql_Parser.K_ROLES :
				case Cql_Parser.K_SFUNC :
				case Cql_Parser.K_SMALLINT :
				case Cql_Parser.K_STATIC :
				case Cql_Parser.K_STORAGE :
				case Cql_Parser.K_STYPE :
				case Cql_Parser.K_SUPERUSER :
				case Cql_Parser.K_TEXT :
				case Cql_Parser.K_TIME :
				case Cql_Parser.K_TIMESTAMP :
				case Cql_Parser.K_TIMEUUID :
				case Cql_Parser.K_TINYINT :
				case Cql_Parser.K_TRIGGER :
				case Cql_Parser.K_TUPLE :
				case Cql_Parser.K_TYPE :
				case Cql_Parser.K_USER :
				case Cql_Parser.K_USERS :
				case Cql_Parser.K_UUID :
				case Cql_Parser.K_VALUES :
				case Cql_Parser.K_VARCHAR :
				case Cql_Parser.K_VARINT :
					{
						alt176 = 3;
					}
					break;
				case Cql_Parser.K_TOKEN :
					{
						alt176 = 4;
					}
					break;
				case Cql_Parser.K_COUNT :
					{
						alt176 = 5;
					}
					break;
				default :
					NoViableAltException nvae = new NoViableAltException("", 176, 0, input);
					throw nvae;
			}
			switch (alt176) {
				case 1 :
					{
						f = ((Token) (match(input, Cql_Parser.IDENT, Cql_Parser.FOLLOW_IDENT_in_allowedFunctionName9504)));
						s = (f != null ? f.getText() : null).toLowerCase();
					}
					break;
				case 2 :
					{
						f = ((Token) (match(input, Cql_Parser.QUOTED_NAME, Cql_Parser.FOLLOW_QUOTED_NAME_in_allowedFunctionName9538)));
						s = (f != null) ? f.getText() : null;
					}
					break;
				case 3 :
					{
						pushFollow(Cql_Parser.FOLLOW_unreserved_function_keyword_in_allowedFunctionName9566);
						u = unreserved_function_keyword();
						(state._fsp)--;
						s = u;
					}
					break;
				case 4 :
					{
						match(input, Cql_Parser.K_TOKEN, Cql_Parser.FOLLOW_K_TOKEN_in_allowedFunctionName9576);
						s = "token";
					}
					break;
				case 5 :
					{
						match(input, Cql_Parser.K_COUNT, Cql_Parser.FOLLOW_K_COUNT_in_allowedFunctionName9608);
						s = "count";
					}
					break;
			}
		} catch (RecognitionException re) {
			reportError(re);
			recover(input, re);
		} finally {
		}
		return s;
	}

	public final Term.Raw function() throws RecognitionException {
		Term.Raw t = null;
		FunctionName f = null;
		List<Term.Raw> args = null;
		try {
			int alt177 = 2;
			alt177 = dfa177.predict(input);
			switch (alt177) {
				case 1 :
					{
						pushFollow(Cql_Parser.FOLLOW_functionName_in_function9655);
						f = functionName();
						(state._fsp)--;
						match(input, 184, Cql_Parser.FOLLOW_184_in_function9657);
						match(input, 185, Cql_Parser.FOLLOW_185_in_function9659);
						t = new FunctionCall.Raw(f, Collections.<Term.Raw>emptyList());
					}
					break;
				case 2 :
					{
						pushFollow(Cql_Parser.FOLLOW_functionName_in_function9689);
						f = functionName();
						(state._fsp)--;
						match(input, 184, Cql_Parser.FOLLOW_184_in_function9691);
						pushFollow(Cql_Parser.FOLLOW_functionArgs_in_function9695);
						args = functionArgs();
						(state._fsp)--;
						match(input, 185, Cql_Parser.FOLLOW_185_in_function9697);
						t = new FunctionCall.Raw(f, args);
					}
					break;
			}
		} catch (RecognitionException re) {
			reportError(re);
			recover(input, re);
		} finally {
		}
		return t;
	}

	public final List<Term.Raw> functionArgs() throws RecognitionException {
		List<Term.Raw> args = null;
		Term.Raw t1 = null;
		Term.Raw tn = null;
		args = new ArrayList<Term.Raw>();
		try {
			{
				pushFollow(Cql_Parser.FOLLOW_term_in_functionArgs9730);
				t1 = term();
				(state._fsp)--;
				args.add(t1);
				loop178 : while (true) {
					int alt178 = 2;
					int LA178_0 = input.LA(1);
					if (LA178_0 == 188) {
						alt178 = 1;
					}
					switch (alt178) {
						case 1 :
							{
								match(input, 188, Cql_Parser.FOLLOW_188_in_functionArgs9736);
								pushFollow(Cql_Parser.FOLLOW_term_in_functionArgs9740);
								tn = term();
								(state._fsp)--;
								args.add(tn);
							}
							break;
						default :
							break loop178;
					}
				} 
			}
		} catch (RecognitionException re) {
			reportError(re);
			recover(input, re);
		} finally {
		}
		return args;
	}

	public final Term.Raw term() throws RecognitionException {
		Term.Raw term = null;
		Term.Raw v = null;
		Term.Raw f = null;
		CQL3Type.Raw c = null;
		Term.Raw t = null;
		try {
			int alt179 = 3;
			alt179 = dfa179.predict(input);
			switch (alt179) {
				case 1 :
					{
						pushFollow(Cql_Parser.FOLLOW_value_in_term9768);
						v = value();
						(state._fsp)--;
						term = v;
					}
					break;
				case 2 :
					{
						pushFollow(Cql_Parser.FOLLOW_function_in_term9805);
						f = function();
						(state._fsp)--;
						term = f;
					}
					break;
				case 3 :
					{
						match(input, 184, Cql_Parser.FOLLOW_184_in_term9837);
						pushFollow(Cql_Parser.FOLLOW_comparatorType_in_term9841);
						c = comparatorType();
						(state._fsp)--;
						match(input, 185, Cql_Parser.FOLLOW_185_in_term9843);
						pushFollow(Cql_Parser.FOLLOW_term_in_term9847);
						t = term();
						(state._fsp)--;
						term = new TypeCast(c, t);
					}
					break;
			}
		} catch (RecognitionException re) {
			reportError(re);
			recover(input, re);
		} finally {
		}
		return term;
	}

	public final void columnOperation(List<Pair<ColumnDefinition.Raw, Operation.RawUpdate>> operations) throws RecognitionException {
		ColumnDefinition.Raw key = null;
		try {
			{
				pushFollow(Cql_Parser.FOLLOW_cident_in_columnOperation9870);
				key = cident();
				(state._fsp)--;
				pushFollow(Cql_Parser.FOLLOW_columnOperationDifferentiator_in_columnOperation9872);
				columnOperationDifferentiator(operations, key);
				(state._fsp)--;
			}
		} catch (RecognitionException re) {
			reportError(re);
			recover(input, re);
		} finally {
		}
	}

	public final void columnOperationDifferentiator(List<Pair<ColumnDefinition.Raw, Operation.RawUpdate>> operations, ColumnDefinition.Raw key) throws RecognitionException {
		Term.Raw k = null;
		FieldIdentifier field = null;
		try {
			int alt180 = 4;
			switch (input.LA(1)) {
				case 196 :
					{
						alt180 = 1;
					}
					break;
				case 187 :
				case 190 :
					{
						alt180 = 2;
					}
					break;
				case 199 :
					{
						alt180 = 3;
					}
					break;
				case 191 :
					{
						alt180 = 4;
					}
					break;
				default :
					NoViableAltException nvae = new NoViableAltException("", 180, 0, input);
					throw nvae;
			}
			switch (alt180) {
				case 1 :
					{
						match(input, 196, Cql_Parser.FOLLOW_196_in_columnOperationDifferentiator9891);
						pushFollow(Cql_Parser.FOLLOW_normalColumnOperation_in_columnOperationDifferentiator9893);
						normalColumnOperation(operations, key);
						(state._fsp)--;
					}
					break;
				case 2 :
					{
						pushFollow(Cql_Parser.FOLLOW_shorthandColumnOperation_in_columnOperationDifferentiator9902);
						shorthandColumnOperation(operations, key);
						(state._fsp)--;
					}
					break;
				case 3 :
					{
						match(input, 199, Cql_Parser.FOLLOW_199_in_columnOperationDifferentiator9911);
						pushFollow(Cql_Parser.FOLLOW_term_in_columnOperationDifferentiator9915);
						k = term();
						(state._fsp)--;
						match(input, 201, Cql_Parser.FOLLOW_201_in_columnOperationDifferentiator9917);
						pushFollow(Cql_Parser.FOLLOW_collectionColumnOperation_in_columnOperationDifferentiator9919);
						collectionColumnOperation(operations, key, k);
						(state._fsp)--;
					}
					break;
				case 4 :
					{
						match(input, 191, Cql_Parser.FOLLOW_191_in_columnOperationDifferentiator9928);
						pushFollow(Cql_Parser.FOLLOW_fident_in_columnOperationDifferentiator9932);
						field = fident();
						(state._fsp)--;
						pushFollow(Cql_Parser.FOLLOW_udtColumnOperation_in_columnOperationDifferentiator9934);
						udtColumnOperation(operations, key, field);
						(state._fsp)--;
					}
					break;
			}
		} catch (RecognitionException re) {
			reportError(re);
			recover(input, re);
		} finally {
		}
	}

	public final void normalColumnOperation(List<Pair<ColumnDefinition.Raw, Operation.RawUpdate>> operations, ColumnDefinition.Raw key) throws RecognitionException {
		Token sig = null;
		Token i = null;
		Term.Raw t = null;
		ColumnDefinition.Raw c = null;
		try {
			int alt182 = 3;
			alt182 = dfa182.predict(input);
			switch (alt182) {
				case 1 :
					{
						pushFollow(Cql_Parser.FOLLOW_term_in_normalColumnOperation9955);
						t = term();
						(state._fsp)--;
						int alt181 = 2;
						int LA181_0 = input.LA(1);
						if (LA181_0 == 186) {
							alt181 = 1;
						}
						switch (alt181) {
							case 1 :
								{
									match(input, 186, Cql_Parser.FOLLOW_186_in_normalColumnOperation9958);
									pushFollow(Cql_Parser.FOLLOW_cident_in_normalColumnOperation9962);
									c = cident();
									(state._fsp)--;
								}
								break;
						}
						if (c == null) {
							addRawUpdate(operations, key, new Operation.SetValue(t));
						}else {
							if (!(key.equals(c)))
								addRecognitionError("Only expressions of the form X = <value> + X are supported.");

							addRawUpdate(operations, key, new Operation.Prepend(t));
						}
					}
					break;
				case 2 :
					{
						pushFollow(Cql_Parser.FOLLOW_cident_in_normalColumnOperation9983);
						c = cident();
						(state._fsp)--;
						sig = input.LT(1);
						if (((input.LA(1)) == 186) || ((input.LA(1)) == 189)) {
							input.consume();
							state.errorRecovery = false;
						}else {
							MismatchedSetException mse = new MismatchedSetException(null, input);
							throw mse;
						}
						pushFollow(Cql_Parser.FOLLOW_term_in_normalColumnOperation9997);
						t = term();
						(state._fsp)--;
						if (!(key.equals(c)))
							addRecognitionError((("Only expressions of the form X = X " + (sig != null ? sig.getText() : null)) + "<value> are supported."));

						addRawUpdate(operations, key, ((sig != null ? sig.getText() : null).equals("+") ? new Operation.Addition(t) : new Operation.Substraction(t)));
					}
					break;
				case 3 :
					{
						pushFollow(Cql_Parser.FOLLOW_cident_in_normalColumnOperation10015);
						c = cident();
						(state._fsp)--;
						i = ((Token) (match(input, Cql_Parser.INTEGER, Cql_Parser.FOLLOW_INTEGER_in_normalColumnOperation10019)));
						if (!(key.equals(c)))
							addRecognitionError((("Only expressions of the form X = X " + (((i != null ? i.getText() : null).charAt(0)) == '-' ? '-' : '+')) + " <value> are supported."));

						addRawUpdate(operations, key, new Operation.Addition(integer((i != null ? i.getText() : null))));
					}
					break;
			}
		} catch (RecognitionException re) {
			reportError(re);
			recover(input, re);
		} finally {
		}
	}

	public final void shorthandColumnOperation(List<Pair<ColumnDefinition.Raw, Operation.RawUpdate>> operations, ColumnDefinition.Raw key) throws RecognitionException {
		Token sig = null;
		Term.Raw t = null;
		try {
			{
				sig = input.LT(1);
				if (((input.LA(1)) == 187) || ((input.LA(1)) == 190)) {
					input.consume();
					state.errorRecovery = false;
				}else {
					MismatchedSetException mse = new MismatchedSetException(null, input);
					throw mse;
				}
				pushFollow(Cql_Parser.FOLLOW_term_in_shorthandColumnOperation10057);
				t = term();
				(state._fsp)--;
				addRawUpdate(operations, key, ((sig != null ? sig.getText() : null).equals("+=") ? new Operation.Addition(t) : new Operation.Substraction(t)));
			}
		} catch (RecognitionException re) {
			reportError(re);
			recover(input, re);
		} finally {
		}
	}

	public final void collectionColumnOperation(List<Pair<ColumnDefinition.Raw, Operation.RawUpdate>> operations, ColumnDefinition.Raw key, Term.Raw k) throws RecognitionException {
		Term.Raw t = null;
		try {
			{
				match(input, 196, Cql_Parser.FOLLOW_196_in_collectionColumnOperation10083);
				pushFollow(Cql_Parser.FOLLOW_term_in_collectionColumnOperation10087);
				t = term();
				(state._fsp)--;
				addRawUpdate(operations, key, new Operation.SetElement(k, t));
			}
		} catch (RecognitionException re) {
			reportError(re);
			recover(input, re);
		} finally {
		}
	}

	public final void udtColumnOperation(List<Pair<ColumnDefinition.Raw, Operation.RawUpdate>> operations, ColumnDefinition.Raw key, FieldIdentifier field) throws RecognitionException {
		Term.Raw t = null;
		try {
			{
				match(input, 196, Cql_Parser.FOLLOW_196_in_udtColumnOperation10113);
				pushFollow(Cql_Parser.FOLLOW_term_in_udtColumnOperation10117);
				t = term();
				(state._fsp)--;
				addRawUpdate(operations, key, new Operation.SetField(field, t));
			}
		} catch (RecognitionException re) {
			reportError(re);
			recover(input, re);
		} finally {
		}
	}

	public final void columnCondition(List<Pair<ColumnDefinition.Raw, ColumnCondition.Raw>> conditions) throws RecognitionException {
		ColumnDefinition.Raw key = null;
		Operator op = null;
		Term.Raw t = null;
		List<Term.Raw> values = null;
		AbstractMarker.INRaw marker = null;
		Term.Raw element = null;
		FieldIdentifier field = null;
		try {
			{
				pushFollow(Cql_Parser.FOLLOW_cident_in_columnCondition10150);
				key = cident();
				(state._fsp)--;
				int alt188 = 4;
				switch (input.LA(1)) {
					case 183 :
					case 194 :
					case 195 :
					case 196 :
					case 197 :
					case 198 :
						{
							alt188 = 1;
						}
						break;
					case Cql_Parser.K_IN :
						{
							alt188 = 2;
						}
						break;
					case 199 :
						{
							alt188 = 3;
						}
						break;
					case 191 :
						{
							alt188 = 4;
						}
						break;
					default :
						NoViableAltException nvae = new NoViableAltException("", 188, 0, input);
						throw nvae;
				}
				switch (alt188) {
					case 1 :
						{
							pushFollow(Cql_Parser.FOLLOW_relationType_in_columnCondition10164);
							op = relationType();
							(state._fsp)--;
							pushFollow(Cql_Parser.FOLLOW_term_in_columnCondition10168);
							t = term();
							(state._fsp)--;
							conditions.add(Pair.create(key, simpleCondition(t, op)));
						}
						break;
					case 2 :
						{
							match(input, Cql_Parser.K_IN, Cql_Parser.FOLLOW_K_IN_in_columnCondition10182);
							int alt183 = 2;
							int LA183_0 = input.LA(1);
							if (LA183_0 == 184) {
								alt183 = 1;
							}else
								if ((LA183_0 == (Cql_Parser.QMARK)) || (LA183_0 == 192)) {
									alt183 = 2;
								}else {
									NoViableAltException nvae = new NoViableAltException("", 183, 0, input);
									throw nvae;
								}

							switch (alt183) {
								case 1 :
									{
										pushFollow(Cql_Parser.FOLLOW_singleColumnInValues_in_columnCondition10200);
										values = singleColumnInValues();
										(state._fsp)--;
										conditions.add(Pair.create(key, ColumnCondition.Raw.simpleInCondition(values)));
									}
									break;
								case 2 :
									{
										pushFollow(Cql_Parser.FOLLOW_inMarker_in_columnCondition10220);
										marker = inMarker();
										(state._fsp)--;
										conditions.add(Pair.create(key, simpleInCondition(marker)));
									}
									break;
							}
						}
						break;
					case 3 :
						{
							match(input, 199, Cql_Parser.FOLLOW_199_in_columnCondition10248);
							pushFollow(Cql_Parser.FOLLOW_term_in_columnCondition10252);
							element = term();
							(state._fsp)--;
							match(input, 201, Cql_Parser.FOLLOW_201_in_columnCondition10254);
							int alt185 = 2;
							int LA185_0 = input.LA(1);
							if ((LA185_0 == 183) || ((LA185_0 >= 194) && (LA185_0 <= 198))) {
								alt185 = 1;
							}else
								if (LA185_0 == (Cql_Parser.K_IN)) {
									alt185 = 2;
								}else {
									NoViableAltException nvae = new NoViableAltException("", 185, 0, input);
									throw nvae;
								}

							switch (alt185) {
								case 1 :
									{
										pushFollow(Cql_Parser.FOLLOW_relationType_in_columnCondition10272);
										op = relationType();
										(state._fsp)--;
										pushFollow(Cql_Parser.FOLLOW_term_in_columnCondition10276);
										t = term();
										(state._fsp)--;
										conditions.add(Pair.create(key, collectionCondition(t, element, op)));
									}
									break;
								case 2 :
									{
										match(input, Cql_Parser.K_IN, Cql_Parser.FOLLOW_K_IN_in_columnCondition10294);
										int alt184 = 2;
										int LA184_0 = input.LA(1);
										if (LA184_0 == 184) {
											alt184 = 1;
										}else
											if ((LA184_0 == (Cql_Parser.QMARK)) || (LA184_0 == 192)) {
												alt184 = 2;
											}else {
												NoViableAltException nvae = new NoViableAltException("", 184, 0, input);
												throw nvae;
											}

										switch (alt184) {
											case 1 :
												{
													pushFollow(Cql_Parser.FOLLOW_singleColumnInValues_in_columnCondition10316);
													values = singleColumnInValues();
													(state._fsp)--;
													conditions.add(Pair.create(key, ColumnCondition.Raw.collectionInCondition(element, values)));
												}
												break;
											case 2 :
												{
													pushFollow(Cql_Parser.FOLLOW_inMarker_in_columnCondition10340);
													marker = inMarker();
													(state._fsp)--;
													conditions.add(Pair.create(key, collectionInCondition(element, marker)));
												}
												break;
										}
									}
									break;
							}
						}
						break;
					case 4 :
						{
							match(input, 191, Cql_Parser.FOLLOW_191_in_columnCondition10386);
							pushFollow(Cql_Parser.FOLLOW_fident_in_columnCondition10390);
							field = fident();
							(state._fsp)--;
							int alt187 = 2;
							int LA187_0 = input.LA(1);
							if ((LA187_0 == 183) || ((LA187_0 >= 194) && (LA187_0 <= 198))) {
								alt187 = 1;
							}else
								if (LA187_0 == (Cql_Parser.K_IN)) {
									alt187 = 2;
								}else {
									NoViableAltException nvae = new NoViableAltException("", 187, 0, input);
									throw nvae;
								}

							switch (alt187) {
								case 1 :
									{
										pushFollow(Cql_Parser.FOLLOW_relationType_in_columnCondition10408);
										op = relationType();
										(state._fsp)--;
										pushFollow(Cql_Parser.FOLLOW_term_in_columnCondition10412);
										t = term();
										(state._fsp)--;
										conditions.add(Pair.create(key, udtFieldCondition(t, field, op)));
									}
									break;
								case 2 :
									{
										match(input, Cql_Parser.K_IN, Cql_Parser.FOLLOW_K_IN_in_columnCondition10430);
										int alt186 = 2;
										int LA186_0 = input.LA(1);
										if (LA186_0 == 184) {
											alt186 = 1;
										}else
											if ((LA186_0 == (Cql_Parser.QMARK)) || (LA186_0 == 192)) {
												alt186 = 2;
											}else {
												NoViableAltException nvae = new NoViableAltException("", 186, 0, input);
												throw nvae;
											}

										switch (alt186) {
											case 1 :
												{
													pushFollow(Cql_Parser.FOLLOW_singleColumnInValues_in_columnCondition10452);
													values = singleColumnInValues();
													(state._fsp)--;
													conditions.add(Pair.create(key, ColumnCondition.Raw.udtFieldInCondition(field, values)));
												}
												break;
											case 2 :
												{
													pushFollow(Cql_Parser.FOLLOW_inMarker_in_columnCondition10476);
													marker = inMarker();
													(state._fsp)--;
													conditions.add(Pair.create(key, udtFieldInCondition(field, marker)));
												}
												break;
										}
									}
									break;
							}
						}
						break;
				}
			}
		} catch (RecognitionException re) {
			reportError(re);
			recover(input, re);
		} finally {
		}
	}

	public final void properties(PropertyDefinitions props) throws RecognitionException {
		try {
			{
				pushFollow(Cql_Parser.FOLLOW_property_in_properties10538);
				property(props);
				(state._fsp)--;
				loop189 : while (true) {
					int alt189 = 2;
					int LA189_0 = input.LA(1);
					if (LA189_0 == (Cql_Parser.K_AND)) {
						alt189 = 1;
					}
					switch (alt189) {
						case 1 :
							{
								match(input, Cql_Parser.K_AND, Cql_Parser.FOLLOW_K_AND_in_properties10542);
								pushFollow(Cql_Parser.FOLLOW_property_in_properties10544);
								property(props);
								(state._fsp)--;
							}
							break;
						default :
							break loop189;
					}
				} 
			}
		} catch (RecognitionException re) {
			reportError(re);
			recover(input, re);
		} finally {
		}
	}

	public final void property(PropertyDefinitions props) throws RecognitionException {
		ColumnIdentifier k = null;
		String simple = null;
		Maps.Literal map = null;
		try {
			int alt190 = 2;
			alt190 = dfa190.predict(input);
			switch (alt190) {
				case 1 :
					{
						pushFollow(Cql_Parser.FOLLOW_noncol_ident_in_property10567);
						k = noncol_ident();
						(state._fsp)--;
						match(input, 196, Cql_Parser.FOLLOW_196_in_property10569);
						pushFollow(Cql_Parser.FOLLOW_propertyValue_in_property10573);
						simple = propertyValue();
						(state._fsp)--;
						try {
							props.addProperty(k.toString(), simple);
						} catch (SyntaxException e) {
							addRecognitionError(e.getMessage());
						}
					}
					break;
				case 2 :
					{
						pushFollow(Cql_Parser.FOLLOW_noncol_ident_in_property10585);
						k = noncol_ident();
						(state._fsp)--;
						match(input, 196, Cql_Parser.FOLLOW_196_in_property10587);
						pushFollow(Cql_Parser.FOLLOW_mapLiteral_in_property10591);
						map = mapLiteral();
						(state._fsp)--;
						try {
							props.addProperty(k.toString(), convertPropertyMap(map));
						} catch (SyntaxException e) {
							addRecognitionError(e.getMessage());
						}
					}
					break;
			}
		} catch (RecognitionException re) {
			reportError(re);
			recover(input, re);
		} finally {
		}
	}

	public final String propertyValue() throws RecognitionException {
		String str = null;
		Constants.Literal c = null;
		String u = null;
		try {
			int alt191 = 2;
			int LA191_0 = input.LA(1);
			if ((((((((((LA191_0 == (Cql_Parser.BOOLEAN)) || (LA191_0 == (Cql_Parser.DURATION))) || (LA191_0 == (Cql_Parser.FLOAT))) || (LA191_0 == (Cql_Parser.HEXNUMBER))) || (LA191_0 == (Cql_Parser.INTEGER))) || (LA191_0 == (Cql_Parser.K_INFINITY))) || (LA191_0 == (Cql_Parser.K_NAN))) || (LA191_0 == (Cql_Parser.STRING_LITERAL))) || (LA191_0 == (Cql_Parser.UUID))) || (LA191_0 == 189)) {
				alt191 = 1;
			}else
				if ((((((((((((((((((((((((((((((((LA191_0 >= (Cql_Parser.K_AGGREGATE)) && (LA191_0 <= (Cql_Parser.K_ALL))) || (LA191_0 == (Cql_Parser.K_AS))) || (LA191_0 == (Cql_Parser.K_ASCII))) || ((LA191_0 >= (Cql_Parser.K_BIGINT)) && (LA191_0 <= (Cql_Parser.K_BOOLEAN)))) || ((LA191_0 >= (Cql_Parser.K_CALLED)) && (LA191_0 <= (Cql_Parser.K_CLUSTERING)))) || ((LA191_0 >= (Cql_Parser.K_COMPACT)) && (LA191_0 <= (Cql_Parser.K_COUNTER)))) || ((LA191_0 >= (Cql_Parser.K_CUSTOM)) && (LA191_0 <= (Cql_Parser.K_DECIMAL)))) || ((LA191_0 >= (Cql_Parser.K_DISTINCT)) && (LA191_0 <= (Cql_Parser.K_DOUBLE)))) || (LA191_0 == (Cql_Parser.K_DURATION))) || ((LA191_0 >= (Cql_Parser.K_EXISTS)) && (LA191_0 <= (Cql_Parser.K_FLOAT)))) || (LA191_0 == (Cql_Parser.K_FROZEN))) || ((LA191_0 >= (Cql_Parser.K_FUNCTION)) && (LA191_0 <= (Cql_Parser.K_FUNCTIONS)))) || (LA191_0 == (Cql_Parser.K_GROUP))) || (LA191_0 == (Cql_Parser.K_INET))) || ((LA191_0 >= (Cql_Parser.K_INITCOND)) && (LA191_0 <= (Cql_Parser.K_INPUT)))) || (LA191_0 == (Cql_Parser.K_INT))) || ((LA191_0 >= (Cql_Parser.K_JSON)) && (LA191_0 <= (Cql_Parser.K_KEYS)))) || ((LA191_0 >= (Cql_Parser.K_KEYSPACES)) && (LA191_0 <= (Cql_Parser.K_LIKE)))) || ((LA191_0 >= (Cql_Parser.K_LIST)) && (LA191_0 <= (Cql_Parser.K_MAP)))) || (LA191_0 == (Cql_Parser.K_NOLOGIN))) || (LA191_0 == (Cql_Parser.K_NOSUPERUSER))) || (LA191_0 == (Cql_Parser.K_OPTIONS))) || ((LA191_0 >= (Cql_Parser.K_PARTITION)) && (LA191_0 <= (Cql_Parser.K_PERMISSIONS)))) || (LA191_0 == (Cql_Parser.K_RETURNS))) || ((LA191_0 >= (Cql_Parser.K_ROLE)) && (LA191_0 <= (Cql_Parser.K_ROLES)))) || ((LA191_0 >= (Cql_Parser.K_SFUNC)) && (LA191_0 <= (Cql_Parser.K_TINYINT)))) || (LA191_0 == (Cql_Parser.K_TRIGGER))) || ((LA191_0 >= (Cql_Parser.K_TTL)) && (LA191_0 <= (Cql_Parser.K_TYPE)))) || ((LA191_0 >= (Cql_Parser.K_USER)) && (LA191_0 <= (Cql_Parser.K_USERS)))) || ((LA191_0 >= (Cql_Parser.K_UUID)) && (LA191_0 <= (Cql_Parser.K_VARINT)))) || (LA191_0 == (Cql_Parser.K_WRITETIME))) {
					alt191 = 2;
				}else {
					NoViableAltException nvae = new NoViableAltException("", 191, 0, input);
					throw nvae;
				}

			switch (alt191) {
				case 1 :
					{
						pushFollow(Cql_Parser.FOLLOW_constant_in_propertyValue10616);
						c = constant();
						(state._fsp)--;
						str = c.getRawText();
					}
					break;
				case 2 :
					{
						pushFollow(Cql_Parser.FOLLOW_unreserved_keyword_in_propertyValue10638);
						u = unreserved_keyword();
						(state._fsp)--;
						str = u;
					}
					break;
			}
		} catch (RecognitionException re) {
			reportError(re);
			recover(input, re);
		} finally {
		}
		return str;
	}

	public final Operator relationType() throws RecognitionException {
		Operator op = null;
		try {
			int alt192 = 6;
			switch (input.LA(1)) {
				case 196 :
					{
						alt192 = 1;
					}
					break;
				case 194 :
					{
						alt192 = 2;
					}
					break;
				case 195 :
					{
						alt192 = 3;
					}
					break;
				case 197 :
					{
						alt192 = 4;
					}
					break;
				case 198 :
					{
						alt192 = 5;
					}
					break;
				case 183 :
					{
						alt192 = 6;
					}
					break;
				default :
					NoViableAltException nvae = new NoViableAltException("", 192, 0, input);
					throw nvae;
			}
			switch (alt192) {
				case 1 :
					{
						match(input, 196, Cql_Parser.FOLLOW_196_in_relationType10661);
						op = Operator.EQ;
					}
					break;
				case 2 :
					{
						match(input, 194, Cql_Parser.FOLLOW_194_in_relationType10672);
						op = Operator.LT;
					}
					break;
				case 3 :
					{
						match(input, 195, Cql_Parser.FOLLOW_195_in_relationType10683);
						op = Operator.LTE;
					}
					break;
				case 4 :
					{
						match(input, 197, Cql_Parser.FOLLOW_197_in_relationType10693);
						op = Operator.GT;
					}
					break;
				case 5 :
					{
						match(input, 198, Cql_Parser.FOLLOW_198_in_relationType10704);
						op = Operator.GTE;
					}
					break;
				case 6 :
					{
						match(input, 183, Cql_Parser.FOLLOW_183_in_relationType10714);
						op = Operator.NEQ;
					}
					break;
			}
		} catch (RecognitionException re) {
			reportError(re);
			recover(input, re);
		} finally {
		}
		return op;
	}

	public final void relation(WhereClause.Builder clauses) throws RecognitionException {
		ColumnDefinition.Raw name = null;
		Operator type = null;
		Term.Raw t = null;
		List<ColumnDefinition.Raw> l = null;
		AbstractMarker.INRaw marker = null;
		List<Term.Raw> inValues = null;
		Term.Raw key = null;
		List<ColumnDefinition.Raw> ids = null;
		Tuples.INRaw tupleInMarker = null;
		List<Tuples.Literal> literals = null;
		List<Tuples.Raw> markers = null;
		Tuples.Literal literal = null;
		Tuples.Raw tupleMarker = null;
		try {
			int alt196 = 10;
			alt196 = dfa196.predict(input);
			switch (alt196) {
				case 1 :
					{
						pushFollow(Cql_Parser.FOLLOW_cident_in_relation10736);
						name = cident();
						(state._fsp)--;
						pushFollow(Cql_Parser.FOLLOW_relationType_in_relation10740);
						type = relationType();
						(state._fsp)--;
						pushFollow(Cql_Parser.FOLLOW_term_in_relation10744);
						t = term();
						(state._fsp)--;
						clauses.add(new SingleColumnRelation(name, type, t));
					}
					break;
				case 2 :
					{
						pushFollow(Cql_Parser.FOLLOW_cident_in_relation10756);
						name = cident();
						(state._fsp)--;
						match(input, Cql_Parser.K_LIKE, Cql_Parser.FOLLOW_K_LIKE_in_relation10758);
						pushFollow(Cql_Parser.FOLLOW_term_in_relation10762);
						t = term();
						(state._fsp)--;
						clauses.add(new SingleColumnRelation(name, Operator.LIKE, t));
					}
					break;
				case 3 :
					{
						pushFollow(Cql_Parser.FOLLOW_cident_in_relation10774);
						name = cident();
						(state._fsp)--;
						match(input, Cql_Parser.K_IS, Cql_Parser.FOLLOW_K_IS_in_relation10776);
						match(input, Cql_Parser.K_NOT, Cql_Parser.FOLLOW_K_NOT_in_relation10778);
						match(input, Cql_Parser.K_NULL, Cql_Parser.FOLLOW_K_NULL_in_relation10780);
						clauses.add(new SingleColumnRelation(name, Operator.IS_NOT, Constants.NULL_LITERAL));
					}
					break;
				case 4 :
					{
						match(input, Cql_Parser.K_TOKEN, Cql_Parser.FOLLOW_K_TOKEN_in_relation10790);
						pushFollow(Cql_Parser.FOLLOW_tupleOfIdentifiers_in_relation10794);
						l = tupleOfIdentifiers();
						(state._fsp)--;
						pushFollow(Cql_Parser.FOLLOW_relationType_in_relation10798);
						type = relationType();
						(state._fsp)--;
						pushFollow(Cql_Parser.FOLLOW_term_in_relation10802);
						t = term();
						(state._fsp)--;
						clauses.add(new TokenRelation(l, type, t));
					}
					break;
				case 5 :
					{
						pushFollow(Cql_Parser.FOLLOW_cident_in_relation10822);
						name = cident();
						(state._fsp)--;
						match(input, Cql_Parser.K_IN, Cql_Parser.FOLLOW_K_IN_in_relation10824);
						pushFollow(Cql_Parser.FOLLOW_inMarker_in_relation10828);
						marker = inMarker();
						(state._fsp)--;
						clauses.add(new SingleColumnRelation(name, Operator.IN, marker));
					}
					break;
				case 6 :
					{
						pushFollow(Cql_Parser.FOLLOW_cident_in_relation10848);
						name = cident();
						(state._fsp)--;
						match(input, Cql_Parser.K_IN, Cql_Parser.FOLLOW_K_IN_in_relation10850);
						pushFollow(Cql_Parser.FOLLOW_singleColumnInValues_in_relation10854);
						inValues = singleColumnInValues();
						(state._fsp)--;
						clauses.add(SingleColumnRelation.createInRelation(name, inValues));
					}
					break;
				case 7 :
					{
						pushFollow(Cql_Parser.FOLLOW_cident_in_relation10874);
						name = cident();
						(state._fsp)--;
						match(input, Cql_Parser.K_CONTAINS, Cql_Parser.FOLLOW_K_CONTAINS_in_relation10876);
						Operator rt = Operator.CONTAINS;
						int alt193 = 2;
						int LA193_0 = input.LA(1);
						if (LA193_0 == (Cql_Parser.K_KEY)) {
							int LA193_1 = input.LA(2);
							if ((((((((((((((((((((((((((((((((((((((((((((LA193_1 == (Cql_Parser.BOOLEAN)) || (LA193_1 == (Cql_Parser.DURATION))) || (LA193_1 == (Cql_Parser.FLOAT))) || (LA193_1 == (Cql_Parser.HEXNUMBER))) || ((LA193_1 >= (Cql_Parser.IDENT)) && (LA193_1 <= (Cql_Parser.INTEGER)))) || ((LA193_1 >= (Cql_Parser.K_AGGREGATE)) && (LA193_1 <= (Cql_Parser.K_ALL)))) || (LA193_1 == (Cql_Parser.K_AS))) || (LA193_1 == (Cql_Parser.K_ASCII))) || ((LA193_1 >= (Cql_Parser.K_BIGINT)) && (LA193_1 <= (Cql_Parser.K_BOOLEAN)))) || ((LA193_1 >= (Cql_Parser.K_CALLED)) && (LA193_1 <= (Cql_Parser.K_CLUSTERING)))) || ((LA193_1 >= (Cql_Parser.K_COMPACT)) && (LA193_1 <= (Cql_Parser.K_COUNTER)))) || ((LA193_1 >= (Cql_Parser.K_CUSTOM)) && (LA193_1 <= (Cql_Parser.K_DECIMAL)))) || ((LA193_1 >= (Cql_Parser.K_DISTINCT)) && (LA193_1 <= (Cql_Parser.K_DOUBLE)))) || (LA193_1 == (Cql_Parser.K_DURATION))) || ((LA193_1 >= (Cql_Parser.K_EXISTS)) && (LA193_1 <= (Cql_Parser.K_FLOAT)))) || (LA193_1 == (Cql_Parser.K_FROZEN))) || ((LA193_1 >= (Cql_Parser.K_FUNCTION)) && (LA193_1 <= (Cql_Parser.K_FUNCTIONS)))) || (LA193_1 == (Cql_Parser.K_GROUP))) || ((LA193_1 >= (Cql_Parser.K_INET)) && (LA193_1 <= (Cql_Parser.K_INPUT)))) || (LA193_1 == (Cql_Parser.K_INT))) || ((LA193_1 >= (Cql_Parser.K_JSON)) && (LA193_1 <= (Cql_Parser.K_KEYS)))) || ((LA193_1 >= (Cql_Parser.K_KEYSPACES)) && (LA193_1 <= (Cql_Parser.K_LIKE)))) || ((LA193_1 >= (Cql_Parser.K_LIST)) && (LA193_1 <= (Cql_Parser.K_MAP)))) || ((LA193_1 >= (Cql_Parser.K_NAN)) && (LA193_1 <= (Cql_Parser.K_NOLOGIN)))) || (LA193_1 == (Cql_Parser.K_NOSUPERUSER))) || (LA193_1 == (Cql_Parser.K_NULL))) || (LA193_1 == (Cql_Parser.K_OPTIONS))) || ((LA193_1 >= (Cql_Parser.K_PARTITION)) && (LA193_1 <= (Cql_Parser.K_PERMISSIONS)))) || (LA193_1 == (Cql_Parser.K_RETURNS))) || ((LA193_1 >= (Cql_Parser.K_ROLE)) && (LA193_1 <= (Cql_Parser.K_ROLES)))) || ((LA193_1 >= (Cql_Parser.K_SFUNC)) && (LA193_1 <= (Cql_Parser.K_TINYINT)))) || ((LA193_1 >= (Cql_Parser.K_TOKEN)) && (LA193_1 <= (Cql_Parser.K_TRIGGER)))) || ((LA193_1 >= (Cql_Parser.K_TTL)) && (LA193_1 <= (Cql_Parser.K_TYPE)))) || ((LA193_1 >= (Cql_Parser.K_USER)) && (LA193_1 <= (Cql_Parser.K_USERS)))) || ((LA193_1 >= (Cql_Parser.K_UUID)) && (LA193_1 <= (Cql_Parser.K_VARINT)))) || (LA193_1 == (Cql_Parser.K_WRITETIME))) || ((LA193_1 >= (Cql_Parser.QMARK)) && (LA193_1 <= (Cql_Parser.QUOTED_NAME)))) || (LA193_1 == (Cql_Parser.STRING_LITERAL))) || (LA193_1 == (Cql_Parser.UUID))) || (LA193_1 == 184)) || (LA193_1 == 189)) || (LA193_1 == 192)) || (LA193_1 == 199)) || (LA193_1 == 203)) {
								alt193 = 1;
							}
						}
						switch (alt193) {
							case 1 :
								{
									match(input, Cql_Parser.K_KEY, Cql_Parser.FOLLOW_K_KEY_in_relation10881);
									rt = Operator.CONTAINS_KEY;
								}
								break;
						}
						pushFollow(Cql_Parser.FOLLOW_term_in_relation10897);
						t = term();
						(state._fsp)--;
						clauses.add(new SingleColumnRelation(name, rt, t));
					}
					break;
				case 8 :
					{
						pushFollow(Cql_Parser.FOLLOW_cident_in_relation10909);
						name = cident();
						(state._fsp)--;
						match(input, 199, Cql_Parser.FOLLOW_199_in_relation10911);
						pushFollow(Cql_Parser.FOLLOW_term_in_relation10915);
						key = term();
						(state._fsp)--;
						match(input, 201, Cql_Parser.FOLLOW_201_in_relation10917);
						pushFollow(Cql_Parser.FOLLOW_relationType_in_relation10921);
						type = relationType();
						(state._fsp)--;
						pushFollow(Cql_Parser.FOLLOW_term_in_relation10925);
						t = term();
						(state._fsp)--;
						clauses.add(new SingleColumnRelation(name, key, type, t));
					}
					break;
				case 9 :
					{
						pushFollow(Cql_Parser.FOLLOW_tupleOfIdentifiers_in_relation10937);
						ids = tupleOfIdentifiers();
						(state._fsp)--;
						int alt195 = 3;
						alt195 = dfa195.predict(input);
						switch (alt195) {
							case 1 :
								{
									match(input, Cql_Parser.K_IN, Cql_Parser.FOLLOW_K_IN_in_relation10947);
									int alt194 = 4;
									int LA194_0 = input.LA(1);
									if (LA194_0 == 184) {
										switch (input.LA(2)) {
											case 185 :
												{
													alt194 = 1;
												}
												break;
											case 184 :
												{
													alt194 = 3;
												}
												break;
											case Cql_Parser.QMARK :
											case 192 :
												{
													alt194 = 4;
												}
												break;
											default :
												int nvaeMark = input.mark();
												try {
													input.consume();
													NoViableAltException nvae = new NoViableAltException("", 194, 1, input);
													throw nvae;
												} finally {
													input.rewind(nvaeMark);
												}
										}
									}else
										if ((LA194_0 == (Cql_Parser.QMARK)) || (LA194_0 == 192)) {
											alt194 = 2;
										}else {
											NoViableAltException nvae = new NoViableAltException("", 194, 0, input);
											throw nvae;
										}

									switch (alt194) {
										case 1 :
											{
												match(input, 184, Cql_Parser.FOLLOW_184_in_relation10961);
												match(input, 185, Cql_Parser.FOLLOW_185_in_relation10963);
												clauses.add(MultiColumnRelation.createInRelation(ids, new ArrayList<Tuples.Literal>()));
											}
											break;
										case 2 :
											{
												pushFollow(Cql_Parser.FOLLOW_inMarkerForTuple_in_relation10995);
												tupleInMarker = inMarkerForTuple();
												(state._fsp)--;
												clauses.add(MultiColumnRelation.createSingleMarkerInRelation(ids, tupleInMarker));
											}
											break;
										case 3 :
											{
												pushFollow(Cql_Parser.FOLLOW_tupleOfTupleLiterals_in_relation11029);
												literals = tupleOfTupleLiterals();
												(state._fsp)--;
												clauses.add(MultiColumnRelation.createInRelation(ids, literals));
											}
											break;
										case 4 :
											{
												pushFollow(Cql_Parser.FOLLOW_tupleOfMarkersForTuples_in_relation11063);
												markers = tupleOfMarkersForTuples();
												(state._fsp)--;
												clauses.add(MultiColumnRelation.createInRelation(ids, markers));
											}
											break;
									}
								}
								break;
							case 2 :
								{
									pushFollow(Cql_Parser.FOLLOW_relationType_in_relation11105);
									type = relationType();
									(state._fsp)--;
									pushFollow(Cql_Parser.FOLLOW_tupleLiteral_in_relation11109);
									literal = tupleLiteral();
									(state._fsp)--;
									clauses.add(MultiColumnRelation.createNonInRelation(ids, type, literal));
								}
								break;
							case 3 :
								{
									pushFollow(Cql_Parser.FOLLOW_relationType_in_relation11135);
									type = relationType();
									(state._fsp)--;
									pushFollow(Cql_Parser.FOLLOW_markerForTuple_in_relation11139);
									tupleMarker = markerForTuple();
									(state._fsp)--;
									clauses.add(MultiColumnRelation.createNonInRelation(ids, type, tupleMarker));
								}
								break;
						}
					}
					break;
				case 10 :
					{
						match(input, 184, Cql_Parser.FOLLOW_184_in_relation11169);
						pushFollow(Cql_Parser.FOLLOW_relation_in_relation11171);
						relation(clauses);
						(state._fsp)--;
						match(input, 185, Cql_Parser.FOLLOW_185_in_relation11174);
					}
					break;
			}
		} catch (RecognitionException re) {
			reportError(re);
			recover(input, re);
		} finally {
		}
	}

	public final AbstractMarker.INRaw inMarker() throws RecognitionException {
		AbstractMarker.INRaw marker = null;
		ColumnIdentifier name = null;
		try {
			int alt197 = 2;
			int LA197_0 = input.LA(1);
			if (LA197_0 == (Cql_Parser.QMARK)) {
				alt197 = 1;
			}else
				if (LA197_0 == 192) {
					alt197 = 2;
				}else {
					NoViableAltException nvae = new NoViableAltException("", 197, 0, input);
					throw nvae;
				}

			switch (alt197) {
				case 1 :
					{
						match(input, Cql_Parser.QMARK, Cql_Parser.FOLLOW_QMARK_in_inMarker11195);
						marker = newINBindVariables(null);
					}
					break;
				case 2 :
					{
						match(input, 192, Cql_Parser.FOLLOW_192_in_inMarker11205);
						pushFollow(Cql_Parser.FOLLOW_noncol_ident_in_inMarker11209);
						name = noncol_ident();
						(state._fsp)--;
						marker = newINBindVariables(name);
					}
					break;
			}
		} catch (RecognitionException re) {
			reportError(re);
			recover(input, re);
		} finally {
		}
		return marker;
	}

	public final List<ColumnDefinition.Raw> tupleOfIdentifiers() throws RecognitionException {
		List<ColumnDefinition.Raw> ids = null;
		ColumnDefinition.Raw n1 = null;
		ColumnDefinition.Raw ni = null;
		ids = new ArrayList<ColumnDefinition.Raw>();
		try {
			{
				match(input, 184, Cql_Parser.FOLLOW_184_in_tupleOfIdentifiers11241);
				pushFollow(Cql_Parser.FOLLOW_cident_in_tupleOfIdentifiers11245);
				n1 = cident();
				(state._fsp)--;
				ids.add(n1);
				loop198 : while (true) {
					int alt198 = 2;
					int LA198_0 = input.LA(1);
					if (LA198_0 == 188) {
						alt198 = 1;
					}
					switch (alt198) {
						case 1 :
							{
								match(input, 188, Cql_Parser.FOLLOW_188_in_tupleOfIdentifiers11250);
								pushFollow(Cql_Parser.FOLLOW_cident_in_tupleOfIdentifiers11254);
								ni = cident();
								(state._fsp)--;
								ids.add(ni);
							}
							break;
						default :
							break loop198;
					}
				} 
				match(input, 185, Cql_Parser.FOLLOW_185_in_tupleOfIdentifiers11260);
			}
		} catch (RecognitionException re) {
			reportError(re);
			recover(input, re);
		} finally {
		}
		return ids;
	}

	public final List<Term.Raw> singleColumnInValues() throws RecognitionException {
		List<Term.Raw> terms = null;
		Term.Raw t1 = null;
		Term.Raw ti = null;
		terms = new ArrayList<Term.Raw>();
		try {
			{
				match(input, 184, Cql_Parser.FOLLOW_184_in_singleColumnInValues11290);
				int alt200 = 2;
				int LA200_0 = input.LA(1);
				if ((((((((((((((((((((((((((((((((((((((((((((LA200_0 == (Cql_Parser.BOOLEAN)) || (LA200_0 == (Cql_Parser.DURATION))) || (LA200_0 == (Cql_Parser.FLOAT))) || (LA200_0 == (Cql_Parser.HEXNUMBER))) || ((LA200_0 >= (Cql_Parser.IDENT)) && (LA200_0 <= (Cql_Parser.INTEGER)))) || ((LA200_0 >= (Cql_Parser.K_AGGREGATE)) && (LA200_0 <= (Cql_Parser.K_ALL)))) || (LA200_0 == (Cql_Parser.K_AS))) || (LA200_0 == (Cql_Parser.K_ASCII))) || ((LA200_0 >= (Cql_Parser.K_BIGINT)) && (LA200_0 <= (Cql_Parser.K_BOOLEAN)))) || ((LA200_0 >= (Cql_Parser.K_CALLED)) && (LA200_0 <= (Cql_Parser.K_CLUSTERING)))) || ((LA200_0 >= (Cql_Parser.K_COMPACT)) && (LA200_0 <= (Cql_Parser.K_COUNTER)))) || ((LA200_0 >= (Cql_Parser.K_CUSTOM)) && (LA200_0 <= (Cql_Parser.K_DECIMAL)))) || ((LA200_0 >= (Cql_Parser.K_DISTINCT)) && (LA200_0 <= (Cql_Parser.K_DOUBLE)))) || (LA200_0 == (Cql_Parser.K_DURATION))) || ((LA200_0 >= (Cql_Parser.K_EXISTS)) && (LA200_0 <= (Cql_Parser.K_FLOAT)))) || (LA200_0 == (Cql_Parser.K_FROZEN))) || ((LA200_0 >= (Cql_Parser.K_FUNCTION)) && (LA200_0 <= (Cql_Parser.K_FUNCTIONS)))) || (LA200_0 == (Cql_Parser.K_GROUP))) || ((LA200_0 >= (Cql_Parser.K_INET)) && (LA200_0 <= (Cql_Parser.K_INPUT)))) || (LA200_0 == (Cql_Parser.K_INT))) || ((LA200_0 >= (Cql_Parser.K_JSON)) && (LA200_0 <= (Cql_Parser.K_KEYS)))) || ((LA200_0 >= (Cql_Parser.K_KEYSPACES)) && (LA200_0 <= (Cql_Parser.K_LIKE)))) || ((LA200_0 >= (Cql_Parser.K_LIST)) && (LA200_0 <= (Cql_Parser.K_MAP)))) || ((LA200_0 >= (Cql_Parser.K_NAN)) && (LA200_0 <= (Cql_Parser.K_NOLOGIN)))) || (LA200_0 == (Cql_Parser.K_NOSUPERUSER))) || (LA200_0 == (Cql_Parser.K_NULL))) || (LA200_0 == (Cql_Parser.K_OPTIONS))) || ((LA200_0 >= (Cql_Parser.K_PARTITION)) && (LA200_0 <= (Cql_Parser.K_PERMISSIONS)))) || (LA200_0 == (Cql_Parser.K_RETURNS))) || ((LA200_0 >= (Cql_Parser.K_ROLE)) && (LA200_0 <= (Cql_Parser.K_ROLES)))) || ((LA200_0 >= (Cql_Parser.K_SFUNC)) && (LA200_0 <= (Cql_Parser.K_TINYINT)))) || ((LA200_0 >= (Cql_Parser.K_TOKEN)) && (LA200_0 <= (Cql_Parser.K_TRIGGER)))) || ((LA200_0 >= (Cql_Parser.K_TTL)) && (LA200_0 <= (Cql_Parser.K_TYPE)))) || ((LA200_0 >= (Cql_Parser.K_USER)) && (LA200_0 <= (Cql_Parser.K_USERS)))) || ((LA200_0 >= (Cql_Parser.K_UUID)) && (LA200_0 <= (Cql_Parser.K_VARINT)))) || (LA200_0 == (Cql_Parser.K_WRITETIME))) || ((LA200_0 >= (Cql_Parser.QMARK)) && (LA200_0 <= (Cql_Parser.QUOTED_NAME)))) || (LA200_0 == (Cql_Parser.STRING_LITERAL))) || (LA200_0 == (Cql_Parser.UUID))) || (LA200_0 == 184)) || (LA200_0 == 189)) || (LA200_0 == 192)) || (LA200_0 == 199)) || (LA200_0 == 203)) {
					alt200 = 1;
				}
				switch (alt200) {
					case 1 :
						{
							pushFollow(Cql_Parser.FOLLOW_term_in_singleColumnInValues11298);
							t1 = term();
							(state._fsp)--;
							terms.add(t1);
							loop199 : while (true) {
								int alt199 = 2;
								int LA199_0 = input.LA(1);
								if (LA199_0 == 188) {
									alt199 = 1;
								}
								switch (alt199) {
									case 1 :
										{
											match(input, 188, Cql_Parser.FOLLOW_188_in_singleColumnInValues11303);
											pushFollow(Cql_Parser.FOLLOW_term_in_singleColumnInValues11307);
											ti = term();
											(state._fsp)--;
											terms.add(ti);
										}
										break;
									default :
										break loop199;
								}
							} 
						}
						break;
				}
				match(input, 185, Cql_Parser.FOLLOW_185_in_singleColumnInValues11316);
			}
		} catch (RecognitionException re) {
			reportError(re);
			recover(input, re);
		} finally {
		}
		return terms;
	}

	public final List<Tuples.Literal> tupleOfTupleLiterals() throws RecognitionException {
		List<Tuples.Literal> literals = null;
		Tuples.Literal t1 = null;
		Tuples.Literal ti = null;
		literals = new ArrayList<>();
		try {
			{
				match(input, 184, Cql_Parser.FOLLOW_184_in_tupleOfTupleLiterals11346);
				pushFollow(Cql_Parser.FOLLOW_tupleLiteral_in_tupleOfTupleLiterals11350);
				t1 = tupleLiteral();
				(state._fsp)--;
				literals.add(t1);
				loop201 : while (true) {
					int alt201 = 2;
					int LA201_0 = input.LA(1);
					if (LA201_0 == 188) {
						alt201 = 1;
					}
					switch (alt201) {
						case 1 :
							{
								match(input, 188, Cql_Parser.FOLLOW_188_in_tupleOfTupleLiterals11355);
								pushFollow(Cql_Parser.FOLLOW_tupleLiteral_in_tupleOfTupleLiterals11359);
								ti = tupleLiteral();
								(state._fsp)--;
								literals.add(ti);
							}
							break;
						default :
							break loop201;
					}
				} 
				match(input, 185, Cql_Parser.FOLLOW_185_in_tupleOfTupleLiterals11365);
			}
		} catch (RecognitionException re) {
			reportError(re);
			recover(input, re);
		} finally {
		}
		return literals;
	}

	public final Tuples.Raw markerForTuple() throws RecognitionException {
		Tuples.Raw marker = null;
		ColumnIdentifier name = null;
		try {
			int alt202 = 2;
			int LA202_0 = input.LA(1);
			if (LA202_0 == (Cql_Parser.QMARK)) {
				alt202 = 1;
			}else
				if (LA202_0 == 192) {
					alt202 = 2;
				}else {
					NoViableAltException nvae = new NoViableAltException("", 202, 0, input);
					throw nvae;
				}

			switch (alt202) {
				case 1 :
					{
						match(input, Cql_Parser.QMARK, Cql_Parser.FOLLOW_QMARK_in_markerForTuple11386);
						marker = newTupleBindVariables(null);
					}
					break;
				case 2 :
					{
						match(input, 192, Cql_Parser.FOLLOW_192_in_markerForTuple11396);
						pushFollow(Cql_Parser.FOLLOW_noncol_ident_in_markerForTuple11400);
						name = noncol_ident();
						(state._fsp)--;
						marker = newTupleBindVariables(name);
					}
					break;
			}
		} catch (RecognitionException re) {
			reportError(re);
			recover(input, re);
		} finally {
		}
		return marker;
	}

	public final List<Tuples.Raw> tupleOfMarkersForTuples() throws RecognitionException {
		List<Tuples.Raw> markers = null;
		Tuples.Raw m1 = null;
		Tuples.Raw mi = null;
		markers = new ArrayList<Tuples.Raw>();
		try {
			{
				match(input, 184, Cql_Parser.FOLLOW_184_in_tupleOfMarkersForTuples11432);
				pushFollow(Cql_Parser.FOLLOW_markerForTuple_in_tupleOfMarkersForTuples11436);
				m1 = markerForTuple();
				(state._fsp)--;
				markers.add(m1);
				loop203 : while (true) {
					int alt203 = 2;
					int LA203_0 = input.LA(1);
					if (LA203_0 == 188) {
						alt203 = 1;
					}
					switch (alt203) {
						case 1 :
							{
								match(input, 188, Cql_Parser.FOLLOW_188_in_tupleOfMarkersForTuples11441);
								pushFollow(Cql_Parser.FOLLOW_markerForTuple_in_tupleOfMarkersForTuples11445);
								mi = markerForTuple();
								(state._fsp)--;
								markers.add(mi);
							}
							break;
						default :
							break loop203;
					}
				} 
				match(input, 185, Cql_Parser.FOLLOW_185_in_tupleOfMarkersForTuples11451);
			}
		} catch (RecognitionException re) {
			reportError(re);
			recover(input, re);
		} finally {
		}
		return markers;
	}

	public final Tuples.INRaw inMarkerForTuple() throws RecognitionException {
		Tuples.INRaw marker = null;
		ColumnIdentifier name = null;
		try {
			int alt204 = 2;
			int LA204_0 = input.LA(1);
			if (LA204_0 == (Cql_Parser.QMARK)) {
				alt204 = 1;
			}else
				if (LA204_0 == 192) {
					alt204 = 2;
				}else {
					NoViableAltException nvae = new NoViableAltException("", 204, 0, input);
					throw nvae;
				}

			switch (alt204) {
				case 1 :
					{
						match(input, Cql_Parser.QMARK, Cql_Parser.FOLLOW_QMARK_in_inMarkerForTuple11472);
						marker = newTupleINBindVariables(null);
					}
					break;
				case 2 :
					{
						match(input, 192, Cql_Parser.FOLLOW_192_in_inMarkerForTuple11482);
						pushFollow(Cql_Parser.FOLLOW_noncol_ident_in_inMarkerForTuple11486);
						name = noncol_ident();
						(state._fsp)--;
						marker = newTupleINBindVariables(name);
					}
					break;
			}
		} catch (RecognitionException re) {
			reportError(re);
			recover(input, re);
		} finally {
		}
		return marker;
	}

	public final CQL3Type.Raw comparatorType() throws RecognitionException {
		CQL3Type.Raw t = null;
		Token s = null;
		CQL3Type n = null;
		CQL3Type.Raw c = null;
		CQL3Type.Raw tt = null;
		UTName id = null;
		CQL3Type.Raw f = null;
		try {
			int alt205 = 6;
			alt205 = dfa205.predict(input);
			switch (alt205) {
				case 1 :
					{
						pushFollow(Cql_Parser.FOLLOW_native_type_in_comparatorType11511);
						n = native_type();
						(state._fsp)--;
						t = from(n);
					}
					break;
				case 2 :
					{
						pushFollow(Cql_Parser.FOLLOW_collection_type_in_comparatorType11527);
						c = collection_type();
						(state._fsp)--;
						t = c;
					}
					break;
				case 3 :
					{
						pushFollow(Cql_Parser.FOLLOW_tuple_type_in_comparatorType11539);
						tt = tuple_type();
						(state._fsp)--;
						t = tt;
					}
					break;
				case 4 :
					{
						pushFollow(Cql_Parser.FOLLOW_userTypeName_in_comparatorType11555);
						id = userTypeName();
						(state._fsp)--;
						t = userType(id);
					}
					break;
				case 5 :
					{
						match(input, Cql_Parser.K_FROZEN, Cql_Parser.FOLLOW_K_FROZEN_in_comparatorType11567);
						match(input, 194, Cql_Parser.FOLLOW_194_in_comparatorType11569);
						pushFollow(Cql_Parser.FOLLOW_comparatorType_in_comparatorType11573);
						f = comparatorType();
						(state._fsp)--;
						match(input, 197, Cql_Parser.FOLLOW_197_in_comparatorType11575);
						try {
							t = frozen(f);
						} catch (InvalidRequestException e) {
							addRecognitionError(e.getMessage());
						}
					}
					break;
				case 6 :
					{
						s = ((Token) (match(input, Cql_Parser.STRING_LITERAL, Cql_Parser.FOLLOW_STRING_LITERAL_in_comparatorType11593)));
						try {
							t = from(new CQL3Type.Custom((s != null ? s.getText() : null)));
						} catch (SyntaxException e) {
							addRecognitionError(((("Cannot parse type " + (s != null ? s.getText() : null)) + ": ") + (e.getMessage())));
						} catch (ConfigurationException e) {
							addRecognitionError(((("Error setting type " + (s != null ? s.getText() : null)) + ": ") + (e.getMessage())));
						}
					}
					break;
			}
		} catch (RecognitionException re) {
			reportError(re);
			recover(input, re);
		} finally {
		}
		return t;
	}

	public final CQL3Type native_type() throws RecognitionException {
		CQL3Type t = null;
		try {
			int alt206 = 21;
			switch (input.LA(1)) {
				case Cql_Parser.K_ASCII :
					{
						alt206 = 1;
					}
					break;
				case Cql_Parser.K_BIGINT :
					{
						alt206 = 2;
					}
					break;
				case Cql_Parser.K_BLOB :
					{
						alt206 = 3;
					}
					break;
				case Cql_Parser.K_BOOLEAN :
					{
						alt206 = 4;
					}
					break;
				case Cql_Parser.K_COUNTER :
					{
						alt206 = 5;
					}
					break;
				case Cql_Parser.K_DECIMAL :
					{
						alt206 = 6;
					}
					break;
				case Cql_Parser.K_DOUBLE :
					{
						alt206 = 7;
					}
					break;
				case Cql_Parser.K_DURATION :
					{
						alt206 = 8;
					}
					break;
				case Cql_Parser.K_FLOAT :
					{
						alt206 = 9;
					}
					break;
				case Cql_Parser.K_INET :
					{
						alt206 = 10;
					}
					break;
				case Cql_Parser.K_INT :
					{
						alt206 = 11;
					}
					break;
				case Cql_Parser.K_SMALLINT :
					{
						alt206 = 12;
					}
					break;
				case Cql_Parser.K_TEXT :
					{
						alt206 = 13;
					}
					break;
				case Cql_Parser.K_TIMESTAMP :
					{
						alt206 = 14;
					}
					break;
				case Cql_Parser.K_TINYINT :
					{
						alt206 = 15;
					}
					break;
				case Cql_Parser.K_UUID :
					{
						alt206 = 16;
					}
					break;
				case Cql_Parser.K_VARCHAR :
					{
						alt206 = 17;
					}
					break;
				case Cql_Parser.K_VARINT :
					{
						alt206 = 18;
					}
					break;
				case Cql_Parser.K_TIMEUUID :
					{
						alt206 = 19;
					}
					break;
				case Cql_Parser.K_DATE :
					{
						alt206 = 20;
					}
					break;
				case Cql_Parser.K_TIME :
					{
						alt206 = 21;
					}
					break;
				default :
					NoViableAltException nvae = new NoViableAltException("", 206, 0, input);
					throw nvae;
			}
			switch (alt206) {
				case 1 :
					{
						match(input, Cql_Parser.K_ASCII, Cql_Parser.FOLLOW_K_ASCII_in_native_type11622);
						t = ASCII;
					}
					break;
				case 2 :
					{
						match(input, Cql_Parser.K_BIGINT, Cql_Parser.FOLLOW_K_BIGINT_in_native_type11636);
						t = BIGINT;
					}
					break;
				case 3 :
					{
						match(input, Cql_Parser.K_BLOB, Cql_Parser.FOLLOW_K_BLOB_in_native_type11649);
						t = BLOB;
					}
					break;
				case 4 :
					{
						match(input, Cql_Parser.K_BOOLEAN, Cql_Parser.FOLLOW_K_BOOLEAN_in_native_type11664);
					}
					break;
				case 5 :
					{
						match(input, Cql_Parser.K_COUNTER, Cql_Parser.FOLLOW_K_COUNTER_in_native_type11676);
						t = CQL3Type.Native.COUNTER;
					}
					break;
				case 6 :
					{
						match(input, Cql_Parser.K_DECIMAL, Cql_Parser.FOLLOW_K_DECIMAL_in_native_type11688);
						t = DECIMAL;
					}
					break;
				case 7 :
					{
						match(input, Cql_Parser.K_DOUBLE, Cql_Parser.FOLLOW_K_DOUBLE_in_native_type11700);
						t = DOUBLE;
					}
					break;
				case 8 :
					{
						match(input, Cql_Parser.K_DURATION, Cql_Parser.FOLLOW_K_DURATION_in_native_type11713);
					}
					break;
				case 9 :
					{
						match(input, Cql_Parser.K_FLOAT, Cql_Parser.FOLLOW_K_FLOAT_in_native_type11726);
					}
					break;
				case 10 :
					{
						match(input, Cql_Parser.K_INET, Cql_Parser.FOLLOW_K_INET_in_native_type11740);
						t = INET;
					}
					break;
				case 11 :
					{
						match(input, Cql_Parser.K_INT, Cql_Parser.FOLLOW_K_INT_in_native_type11755);
						t = INT;
					}
					break;
				case 12 :
					{
						match(input, Cql_Parser.K_SMALLINT, Cql_Parser.FOLLOW_K_SMALLINT_in_native_type11771);
						t = SMALLINT;
					}
					break;
				case 13 :
					{
						match(input, Cql_Parser.K_TEXT, Cql_Parser.FOLLOW_K_TEXT_in_native_type11782);
						t = TEXT;
					}
					break;
				case 14 :
					{
						match(input, Cql_Parser.K_TIMESTAMP, Cql_Parser.FOLLOW_K_TIMESTAMP_in_native_type11797);
						t = TIMESTAMP;
					}
					break;
				case 15 :
					{
						match(input, Cql_Parser.K_TINYINT, Cql_Parser.FOLLOW_K_TINYINT_in_native_type11807);
						t = TINYINT;
					}
					break;
				case 16 :
					{
						match(input, Cql_Parser.K_UUID, Cql_Parser.FOLLOW_K_UUID_in_native_type11819);
					}
					break;
				case 17 :
					{
						match(input, Cql_Parser.K_VARCHAR, Cql_Parser.FOLLOW_K_VARCHAR_in_native_type11834);
						t = VARCHAR;
					}
					break;
				case 18 :
					{
						match(input, Cql_Parser.K_VARINT, Cql_Parser.FOLLOW_K_VARINT_in_native_type11846);
						t = VARINT;
					}
					break;
				case 19 :
					{
						match(input, Cql_Parser.K_TIMEUUID, Cql_Parser.FOLLOW_K_TIMEUUID_in_native_type11859);
						t = TIMEUUID;
					}
					break;
				case 20 :
					{
						match(input, Cql_Parser.K_DATE, Cql_Parser.FOLLOW_K_DATE_in_native_type11870);
						t = DATE;
					}
					break;
				case 21 :
					{
						match(input, Cql_Parser.K_TIME, Cql_Parser.FOLLOW_K_TIME_in_native_type11885);
						t = TIME;
					}
					break;
			}
		} catch (RecognitionException re) {
			reportError(re);
			recover(input, re);
		} finally {
		}
		return t;
	}

	public final CQL3Type.Raw collection_type() throws RecognitionException {
		CQL3Type.Raw pt = null;
		CQL3Type.Raw t1 = null;
		CQL3Type.Raw t2 = null;
		CQL3Type.Raw t = null;
		try {
			int alt207 = 3;
			switch (input.LA(1)) {
				case Cql_Parser.K_MAP :
					{
						alt207 = 1;
					}
					break;
				case Cql_Parser.K_LIST :
					{
						alt207 = 2;
					}
					break;
				case Cql_Parser.K_SET :
					{
						alt207 = 3;
					}
					break;
				default :
					NoViableAltException nvae = new NoViableAltException("", 207, 0, input);
					throw nvae;
			}
			switch (alt207) {
				case 1 :
					{
						match(input, Cql_Parser.K_MAP, Cql_Parser.FOLLOW_K_MAP_in_collection_type11913);
						match(input, 194, Cql_Parser.FOLLOW_194_in_collection_type11916);
						pushFollow(Cql_Parser.FOLLOW_comparatorType_in_collection_type11920);
						t1 = comparatorType();
						(state._fsp)--;
						match(input, 188, Cql_Parser.FOLLOW_188_in_collection_type11922);
						pushFollow(Cql_Parser.FOLLOW_comparatorType_in_collection_type11926);
						t2 = comparatorType();
						(state._fsp)--;
						match(input, 197, Cql_Parser.FOLLOW_197_in_collection_type11928);
						if ((t1 != null) && (t2 != null))
							pt = map(t1, t2);

					}
					break;
				case 2 :
					{
						match(input, Cql_Parser.K_LIST, Cql_Parser.FOLLOW_K_LIST_in_collection_type11946);
						match(input, 194, Cql_Parser.FOLLOW_194_in_collection_type11948);
						pushFollow(Cql_Parser.FOLLOW_comparatorType_in_collection_type11952);
						t = comparatorType();
						(state._fsp)--;
						match(input, 197, Cql_Parser.FOLLOW_197_in_collection_type11954);
						if (t != null)
							pt = list(t);

					}
					break;
				case 3 :
					{
						match(input, Cql_Parser.K_SET, Cql_Parser.FOLLOW_K_SET_in_collection_type11972);
						match(input, 194, Cql_Parser.FOLLOW_194_in_collection_type11975);
						pushFollow(Cql_Parser.FOLLOW_comparatorType_in_collection_type11979);
						t = comparatorType();
						(state._fsp)--;
						match(input, 197, Cql_Parser.FOLLOW_197_in_collection_type11981);
						if (t != null)
							pt = set(t);

					}
					break;
			}
		} catch (RecognitionException re) {
			reportError(re);
			recover(input, re);
		} finally {
		}
		return pt;
	}

	public final CQL3Type.Raw tuple_type() throws RecognitionException {
		CQL3Type.Raw t = null;
		CQL3Type.Raw t1 = null;
		CQL3Type.Raw tn = null;
		try {
			{
				match(input, Cql_Parser.K_TUPLE, Cql_Parser.FOLLOW_K_TUPLE_in_tuple_type12012);
				match(input, 194, Cql_Parser.FOLLOW_194_in_tuple_type12014);
				List<CQL3Type.Raw> types = new ArrayList<>();
				pushFollow(Cql_Parser.FOLLOW_comparatorType_in_tuple_type12029);
				t1 = comparatorType();
				(state._fsp)--;
				types.add(t1);
				loop208 : while (true) {
					int alt208 = 2;
					int LA208_0 = input.LA(1);
					if (LA208_0 == 188) {
						alt208 = 1;
					}
					switch (alt208) {
						case 1 :
							{
								match(input, 188, Cql_Parser.FOLLOW_188_in_tuple_type12034);
								pushFollow(Cql_Parser.FOLLOW_comparatorType_in_tuple_type12038);
								tn = comparatorType();
								(state._fsp)--;
								types.add(tn);
							}
							break;
						default :
							break loop208;
					}
				} 
				match(input, 197, Cql_Parser.FOLLOW_197_in_tuple_type12050);
				t = tuple(types);
			}
		} catch (RecognitionException re) {
			reportError(re);
			recover(input, re);
		} finally {
		}
		return t;
	}

	public static class username_return extends ParserRuleReturnScope {}

	public final Cql_Parser.username_return username() throws RecognitionException {
		Cql_Parser.username_return retval = new Cql_Parser.username_return();
		retval.start = input.LT(1);
		try {
			int alt209 = 3;
			switch (input.LA(1)) {
				case Cql_Parser.IDENT :
					{
						alt209 = 1;
					}
					break;
				case Cql_Parser.STRING_LITERAL :
					{
						alt209 = 2;
					}
					break;
				case Cql_Parser.QUOTED_NAME :
					{
						alt209 = 3;
					}
					break;
				default :
					NoViableAltException nvae = new NoViableAltException("", 209, 0, input);
					throw nvae;
			}
			switch (alt209) {
				case 1 :
					{
						match(input, Cql_Parser.IDENT, Cql_Parser.FOLLOW_IDENT_in_username12069);
					}
					break;
				case 2 :
					{
						match(input, Cql_Parser.STRING_LITERAL, Cql_Parser.FOLLOW_STRING_LITERAL_in_username12077);
					}
					break;
				case 3 :
					{
						match(input, Cql_Parser.QUOTED_NAME, Cql_Parser.FOLLOW_QUOTED_NAME_in_username12085);
						addRecognitionError("Quoted strings are are not supported for user names and USER is deprecated, please use ROLE");
					}
					break;
			}
			retval.stop = input.LT((-1));
		} catch (RecognitionException re) {
			reportError(re);
			recover(input, re);
		} finally {
		}
		return retval;
	}

	public static class mbean_return extends ParserRuleReturnScope {}

	public final Cql_Parser.mbean_return mbean() throws RecognitionException {
		Cql_Parser.mbean_return retval = new Cql_Parser.mbean_return();
		retval.start = input.LT(1);
		try {
			{
				match(input, Cql_Parser.STRING_LITERAL, Cql_Parser.FOLLOW_STRING_LITERAL_in_mbean12104);
			}
			retval.stop = input.LT((-1));
		} catch (RecognitionException re) {
			reportError(re);
			recover(input, re);
		} finally {
		}
		return retval;
	}

	public final ColumnIdentifier non_type_ident() throws RecognitionException {
		ColumnIdentifier id = null;
		Token t = null;
		Token kk = null;
		String k = null;
		try {
			int alt210 = 4;
			switch (input.LA(1)) {
				case Cql_Parser.IDENT :
					{
						alt210 = 1;
					}
					break;
				case Cql_Parser.QUOTED_NAME :
					{
						alt210 = 2;
					}
					break;
				case Cql_Parser.K_AGGREGATE :
				case Cql_Parser.K_ALL :
				case Cql_Parser.K_AS :
				case Cql_Parser.K_CALLED :
				case Cql_Parser.K_CLUSTERING :
				case Cql_Parser.K_COMPACT :
				case Cql_Parser.K_CONTAINS :
				case Cql_Parser.K_CUSTOM :
				case Cql_Parser.K_EXISTS :
				case Cql_Parser.K_FILTERING :
				case Cql_Parser.K_FINALFUNC :
				case Cql_Parser.K_FROZEN :
				case Cql_Parser.K_FUNCTION :
				case Cql_Parser.K_FUNCTIONS :
				case Cql_Parser.K_GROUP :
				case Cql_Parser.K_INITCOND :
				case Cql_Parser.K_INPUT :
				case Cql_Parser.K_KEYS :
				case Cql_Parser.K_KEYSPACES :
				case Cql_Parser.K_LANGUAGE :
				case Cql_Parser.K_LIKE :
				case Cql_Parser.K_LIST :
				case Cql_Parser.K_LOGIN :
				case Cql_Parser.K_MAP :
				case Cql_Parser.K_NOLOGIN :
				case Cql_Parser.K_NOSUPERUSER :
				case Cql_Parser.K_OPTIONS :
				case Cql_Parser.K_PARTITION :
				case Cql_Parser.K_PASSWORD :
				case Cql_Parser.K_PER :
				case Cql_Parser.K_PERMISSION :
				case Cql_Parser.K_PERMISSIONS :
				case Cql_Parser.K_RETURNS :
				case Cql_Parser.K_ROLE :
				case Cql_Parser.K_ROLES :
				case Cql_Parser.K_SFUNC :
				case Cql_Parser.K_STATIC :
				case Cql_Parser.K_STORAGE :
				case Cql_Parser.K_STYPE :
				case Cql_Parser.K_SUPERUSER :
				case Cql_Parser.K_TRIGGER :
				case Cql_Parser.K_TUPLE :
				case Cql_Parser.K_TYPE :
				case Cql_Parser.K_USER :
				case Cql_Parser.K_USERS :
				case Cql_Parser.K_VALUES :
					{
						alt210 = 3;
					}
					break;
				case Cql_Parser.K_KEY :
					{
						alt210 = 4;
					}
					break;
				default :
					NoViableAltException nvae = new NoViableAltException("", 210, 0, input);
					throw nvae;
			}
			switch (alt210) {
				case 1 :
					{
						t = ((Token) (match(input, Cql_Parser.IDENT, Cql_Parser.FOLLOW_IDENT_in_non_type_ident12129)));
						if (Cql_Parser.reservedTypeNames.contains((t != null ? t.getText() : null)))
							addRecognitionError(("Invalid (reserved) user type name " + (t != null ? t.getText() : null)));

						id = new ColumnIdentifier((t != null ? t.getText() : null), false);
					}
					break;
				case 2 :
					{
						t = ((Token) (match(input, Cql_Parser.QUOTED_NAME, Cql_Parser.FOLLOW_QUOTED_NAME_in_non_type_ident12160)));
						id = new ColumnIdentifier((t != null ? t.getText() : null), true);
					}
					break;
				case 3 :
					{
						pushFollow(Cql_Parser.FOLLOW_basic_unreserved_keyword_in_non_type_ident12185);
						k = basic_unreserved_keyword();
						(state._fsp)--;
						id = new ColumnIdentifier(k, false);
					}
					break;
				case 4 :
					{
						kk = ((Token) (match(input, Cql_Parser.K_KEY, Cql_Parser.FOLLOW_K_KEY_in_non_type_ident12197)));
						id = new ColumnIdentifier((kk != null ? kk.getText() : null), false);
					}
					break;
			}
		} catch (RecognitionException re) {
			reportError(re);
			recover(input, re);
		} finally {
		}
		return id;
	}

	public final String unreserved_keyword() throws RecognitionException {
		String str = null;
		Token k = null;
		String u = null;
		try {
			int alt211 = 2;
			int LA211_0 = input.LA(1);
			if (((((((((((((((((((((((((((((((((LA211_0 >= (Cql_Parser.K_AGGREGATE)) && (LA211_0 <= (Cql_Parser.K_ALL))) || (LA211_0 == (Cql_Parser.K_AS))) || (LA211_0 == (Cql_Parser.K_ASCII))) || ((LA211_0 >= (Cql_Parser.K_BIGINT)) && (LA211_0 <= (Cql_Parser.K_BOOLEAN)))) || (LA211_0 == (Cql_Parser.K_CALLED))) || (LA211_0 == (Cql_Parser.K_CLUSTERING))) || ((LA211_0 >= (Cql_Parser.K_COMPACT)) && (LA211_0 <= (Cql_Parser.K_CONTAINS)))) || (LA211_0 == (Cql_Parser.K_COUNTER))) || ((LA211_0 >= (Cql_Parser.K_CUSTOM)) && (LA211_0 <= (Cql_Parser.K_DECIMAL)))) || (LA211_0 == (Cql_Parser.K_DOUBLE))) || (LA211_0 == (Cql_Parser.K_DURATION))) || ((LA211_0 >= (Cql_Parser.K_EXISTS)) && (LA211_0 <= (Cql_Parser.K_FLOAT)))) || (LA211_0 == (Cql_Parser.K_FROZEN))) || ((LA211_0 >= (Cql_Parser.K_FUNCTION)) && (LA211_0 <= (Cql_Parser.K_FUNCTIONS)))) || (LA211_0 == (Cql_Parser.K_GROUP))) || (LA211_0 == (Cql_Parser.K_INET))) || ((LA211_0 >= (Cql_Parser.K_INITCOND)) && (LA211_0 <= (Cql_Parser.K_INPUT)))) || (LA211_0 == (Cql_Parser.K_INT))) || (LA211_0 == (Cql_Parser.K_KEYS))) || ((LA211_0 >= (Cql_Parser.K_KEYSPACES)) && (LA211_0 <= (Cql_Parser.K_LIKE)))) || ((LA211_0 >= (Cql_Parser.K_LIST)) && (LA211_0 <= (Cql_Parser.K_MAP)))) || (LA211_0 == (Cql_Parser.K_NOLOGIN))) || (LA211_0 == (Cql_Parser.K_NOSUPERUSER))) || (LA211_0 == (Cql_Parser.K_OPTIONS))) || ((LA211_0 >= (Cql_Parser.K_PARTITION)) && (LA211_0 <= (Cql_Parser.K_PERMISSIONS)))) || (LA211_0 == (Cql_Parser.K_RETURNS))) || ((LA211_0 >= (Cql_Parser.K_ROLE)) && (LA211_0 <= (Cql_Parser.K_ROLES)))) || ((LA211_0 >= (Cql_Parser.K_SFUNC)) && (LA211_0 <= (Cql_Parser.K_TINYINT)))) || (LA211_0 == (Cql_Parser.K_TRIGGER))) || ((LA211_0 >= (Cql_Parser.K_TUPLE)) && (LA211_0 <= (Cql_Parser.K_TYPE)))) || ((LA211_0 >= (Cql_Parser.K_USER)) && (LA211_0 <= (Cql_Parser.K_USERS)))) || ((LA211_0 >= (Cql_Parser.K_UUID)) && (LA211_0 <= (Cql_Parser.K_VARINT)))) {
				alt211 = 1;
			}else
				if ((((((LA211_0 == (Cql_Parser.K_CAST)) || (LA211_0 == (Cql_Parser.K_COUNT))) || (LA211_0 == (Cql_Parser.K_DISTINCT))) || ((LA211_0 >= (Cql_Parser.K_JSON)) && (LA211_0 <= (Cql_Parser.K_KEY)))) || (LA211_0 == (Cql_Parser.K_TTL))) || (LA211_0 == (Cql_Parser.K_WRITETIME))) {
					alt211 = 2;
				}else {
					NoViableAltException nvae = new NoViableAltException("", 211, 0, input);
					throw nvae;
				}

			switch (alt211) {
				case 1 :
					{
						pushFollow(Cql_Parser.FOLLOW_unreserved_function_keyword_in_unreserved_keyword12240);
						u = unreserved_function_keyword();
						(state._fsp)--;
						str = u;
					}
					break;
				case 2 :
					{
						k = input.LT(1);
						if (((((((input.LA(1)) == (Cql_Parser.K_CAST)) || ((input.LA(1)) == (Cql_Parser.K_COUNT))) || ((input.LA(1)) == (Cql_Parser.K_DISTINCT))) || (((input.LA(1)) >= (Cql_Parser.K_JSON)) && ((input.LA(1)) <= (Cql_Parser.K_KEY)))) || ((input.LA(1)) == (Cql_Parser.K_TTL))) || ((input.LA(1)) == (Cql_Parser.K_WRITETIME))) {
							input.consume();
							state.errorRecovery = false;
						}else {
							MismatchedSetException mse = new MismatchedSetException(null, input);
							throw mse;
						}
						str = (k != null) ? k.getText() : null;
					}
					break;
			}
		} catch (RecognitionException re) {
			reportError(re);
			recover(input, re);
		} finally {
		}
		return str;
	}

	public final String unreserved_function_keyword() throws RecognitionException {
		String str = null;
		String u = null;
		CQL3Type t = null;
		try {
			int alt212 = 2;
			int LA212_0 = input.LA(1);
			if (((((((((((((((((((((((((((LA212_0 >= (Cql_Parser.K_AGGREGATE)) && (LA212_0 <= (Cql_Parser.K_ALL))) || (LA212_0 == (Cql_Parser.K_AS))) || (LA212_0 == (Cql_Parser.K_CALLED))) || (LA212_0 == (Cql_Parser.K_CLUSTERING))) || ((LA212_0 >= (Cql_Parser.K_COMPACT)) && (LA212_0 <= (Cql_Parser.K_CONTAINS)))) || (LA212_0 == (Cql_Parser.K_CUSTOM))) || ((LA212_0 >= (Cql_Parser.K_EXISTS)) && (LA212_0 <= (Cql_Parser.K_FINALFUNC)))) || (LA212_0 == (Cql_Parser.K_FROZEN))) || ((LA212_0 >= (Cql_Parser.K_FUNCTION)) && (LA212_0 <= (Cql_Parser.K_FUNCTIONS)))) || (LA212_0 == (Cql_Parser.K_GROUP))) || ((LA212_0 >= (Cql_Parser.K_INITCOND)) && (LA212_0 <= (Cql_Parser.K_INPUT)))) || (LA212_0 == (Cql_Parser.K_KEYS))) || ((LA212_0 >= (Cql_Parser.K_KEYSPACES)) && (LA212_0 <= (Cql_Parser.K_LIKE)))) || ((LA212_0 >= (Cql_Parser.K_LIST)) && (LA212_0 <= (Cql_Parser.K_MAP)))) || (LA212_0 == (Cql_Parser.K_NOLOGIN))) || (LA212_0 == (Cql_Parser.K_NOSUPERUSER))) || (LA212_0 == (Cql_Parser.K_OPTIONS))) || ((LA212_0 >= (Cql_Parser.K_PARTITION)) && (LA212_0 <= (Cql_Parser.K_PERMISSIONS)))) || (LA212_0 == (Cql_Parser.K_RETURNS))) || ((LA212_0 >= (Cql_Parser.K_ROLE)) && (LA212_0 <= (Cql_Parser.K_ROLES)))) || (LA212_0 == (Cql_Parser.K_SFUNC))) || ((LA212_0 >= (Cql_Parser.K_STATIC)) && (LA212_0 <= (Cql_Parser.K_SUPERUSER)))) || (LA212_0 == (Cql_Parser.K_TRIGGER))) || ((LA212_0 >= (Cql_Parser.K_TUPLE)) && (LA212_0 <= (Cql_Parser.K_TYPE)))) || ((LA212_0 >= (Cql_Parser.K_USER)) && (LA212_0 <= (Cql_Parser.K_USERS)))) || (LA212_0 == (Cql_Parser.K_VALUES))) {
				alt212 = 1;
			}else
				if (((((((((((((LA212_0 == (Cql_Parser.K_ASCII)) || ((LA212_0 >= (Cql_Parser.K_BIGINT)) && (LA212_0 <= (Cql_Parser.K_BOOLEAN)))) || (LA212_0 == (Cql_Parser.K_COUNTER))) || ((LA212_0 >= (Cql_Parser.K_DATE)) && (LA212_0 <= (Cql_Parser.K_DECIMAL)))) || (LA212_0 == (Cql_Parser.K_DOUBLE))) || (LA212_0 == (Cql_Parser.K_DURATION))) || (LA212_0 == (Cql_Parser.K_FLOAT))) || (LA212_0 == (Cql_Parser.K_INET))) || (LA212_0 == (Cql_Parser.K_INT))) || (LA212_0 == (Cql_Parser.K_SMALLINT))) || ((LA212_0 >= (Cql_Parser.K_TEXT)) && (LA212_0 <= (Cql_Parser.K_TINYINT)))) || (LA212_0 == (Cql_Parser.K_UUID))) || ((LA212_0 >= (Cql_Parser.K_VARCHAR)) && (LA212_0 <= (Cql_Parser.K_VARINT)))) {
					alt212 = 2;
				}else {
					NoViableAltException nvae = new NoViableAltException("", 212, 0, input);
					throw nvae;
				}

			switch (alt212) {
				case 1 :
					{
						pushFollow(Cql_Parser.FOLLOW_basic_unreserved_keyword_in_unreserved_function_keyword12307);
						u = basic_unreserved_keyword();
						(state._fsp)--;
						str = u;
					}
					break;
				case 2 :
					{
						pushFollow(Cql_Parser.FOLLOW_native_type_in_unreserved_function_keyword12319);
						t = native_type();
						(state._fsp)--;
						str = t.toString();
					}
					break;
			}
		} catch (RecognitionException re) {
			reportError(re);
			recover(input, re);
		} finally {
		}
		return str;
	}

	public final String basic_unreserved_keyword() throws RecognitionException {
		String str = null;
		Token k = null;
		try {
			{
				k = input.LT(1);
				if ((((((((((((((((((((((((((((input.LA(1)) >= (Cql_Parser.K_AGGREGATE)) && ((input.LA(1)) <= (Cql_Parser.K_ALL))) || ((input.LA(1)) == (Cql_Parser.K_AS))) || ((input.LA(1)) == (Cql_Parser.K_CALLED))) || ((input.LA(1)) == (Cql_Parser.K_CLUSTERING))) || (((input.LA(1)) >= (Cql_Parser.K_COMPACT)) && ((input.LA(1)) <= (Cql_Parser.K_CONTAINS)))) || ((input.LA(1)) == (Cql_Parser.K_CUSTOM))) || (((input.LA(1)) >= (Cql_Parser.K_EXISTS)) && ((input.LA(1)) <= (Cql_Parser.K_FINALFUNC)))) || ((input.LA(1)) == (Cql_Parser.K_FROZEN))) || (((input.LA(1)) >= (Cql_Parser.K_FUNCTION)) && ((input.LA(1)) <= (Cql_Parser.K_FUNCTIONS)))) || ((input.LA(1)) == (Cql_Parser.K_GROUP))) || (((input.LA(1)) >= (Cql_Parser.K_INITCOND)) && ((input.LA(1)) <= (Cql_Parser.K_INPUT)))) || ((input.LA(1)) == (Cql_Parser.K_KEYS))) || (((input.LA(1)) >= (Cql_Parser.K_KEYSPACES)) && ((input.LA(1)) <= (Cql_Parser.K_LIKE)))) || (((input.LA(1)) >= (Cql_Parser.K_LIST)) && ((input.LA(1)) <= (Cql_Parser.K_MAP)))) || ((input.LA(1)) == (Cql_Parser.K_NOLOGIN))) || ((input.LA(1)) == (Cql_Parser.K_NOSUPERUSER))) || ((input.LA(1)) == (Cql_Parser.K_OPTIONS))) || (((input.LA(1)) >= (Cql_Parser.K_PARTITION)) && ((input.LA(1)) <= (Cql_Parser.K_PERMISSIONS)))) || ((input.LA(1)) == (Cql_Parser.K_RETURNS))) || (((input.LA(1)) >= (Cql_Parser.K_ROLE)) && ((input.LA(1)) <= (Cql_Parser.K_ROLES)))) || ((input.LA(1)) == (Cql_Parser.K_SFUNC))) || (((input.LA(1)) >= (Cql_Parser.K_STATIC)) && ((input.LA(1)) <= (Cql_Parser.K_SUPERUSER)))) || ((input.LA(1)) == (Cql_Parser.K_TRIGGER))) || (((input.LA(1)) >= (Cql_Parser.K_TUPLE)) && ((input.LA(1)) <= (Cql_Parser.K_TYPE)))) || (((input.LA(1)) >= (Cql_Parser.K_USER)) && ((input.LA(1)) <= (Cql_Parser.K_USERS)))) || ((input.LA(1)) == (Cql_Parser.K_VALUES))) {
					input.consume();
					state.errorRecovery = false;
				}else {
					MismatchedSetException mse = new MismatchedSetException(null, input);
					throw mse;
				}
				str = (k != null) ? k.getText() : null;
			}
		} catch (RecognitionException re) {
			reportError(re);
			recover(input, re);
		} finally {
		}
		return str;
	}

	protected Cql_Parser.DFA1 dfa1 = new Cql_Parser.DFA1(this);

	protected Cql_Parser.DFA15 dfa15 = new Cql_Parser.DFA15(this);

	protected Cql_Parser.DFA44 dfa44 = new Cql_Parser.DFA44(this);

	protected Cql_Parser.DFA154 dfa154 = new Cql_Parser.DFA154(this);

	protected Cql_Parser.DFA155 dfa155 = new Cql_Parser.DFA155(this);

	protected Cql_Parser.DFA173 dfa173 = new Cql_Parser.DFA173(this);

	protected Cql_Parser.DFA175 dfa175 = new Cql_Parser.DFA175(this);

	protected Cql_Parser.DFA177 dfa177 = new Cql_Parser.DFA177(this);

	protected Cql_Parser.DFA179 dfa179 = new Cql_Parser.DFA179(this);

	protected Cql_Parser.DFA182 dfa182 = new Cql_Parser.DFA182(this);

	protected Cql_Parser.DFA190 dfa190 = new Cql_Parser.DFA190(this);

	protected Cql_Parser.DFA196 dfa196 = new Cql_Parser.DFA196(this);

	protected Cql_Parser.DFA195 dfa195 = new Cql_Parser.DFA195(this);

	protected Cql_Parser.DFA205 dfa205 = new Cql_Parser.DFA205(this);

	static final String DFA1_eotS = "3\uffff";

	static final String DFA1_eofS = "3\uffff";

	static final String DFA1_minS = "\u0001\u001f\u0007\uffff\u0002\u001c\u0001/\u0002\u0017\u0001\u001d\b\uffff\u0001y\u0012\uffff\u0001n\u0002\uffff" + "\u0001F\u0005\uffff\u0001\u001c";

	static final String DFA1_maxS = "\u0001\u0095\u0007\uffff\u0003\u0096\u0002\u00ad\u0001\u0097\b\uffff\u0001y\u0012\uffff\u0001\u008b" + "\u0002\uffff\u0001v\u0005\uffff\u0001I";

	static final String DFA1_acceptS = "\u0001\uffff\u0001\u0001\u0001\u0002\u0001\u0003\u0001\u0004\u0001\u0005\u0001\u0006\u0001\u0007\u0006\uffff\u0001\b\u0001\t\u0001\u0013\u0001\u0017\u0001\u0019" + (("\u0001 \u0001&\u0001\n\u0001\uffff\u0001\u001c\u0001\u001e\u0001\u000b\u0001\f\u0001\r\u0001\u0015\u0001\u0018\u0001\u001b\u0001\u001d\u0001" + "\u001f\u0001\"\u0001\'\u0001\u000e\u0001\u000f\u0001\u0014\u0001\u001a\u0001!\u0001(\u0001\uffff\u0001\u0010\u0001$\u0001\uffff") + "\u0001\u0011\u0001%\u0001\u0016\u0001#\u0001\u0012\u0001\uffff");

	static final String DFA1_specialS = "3\uffff}>";

	static final String[] DFA1_transitionS = new String[]{ "\u0001\n\u0007\uffff\u0001\u0004\f\uffff\u0001\b\u0004\uffff\u0001\u0005\u0004\uffff\u0001\t\f\uffff\u0001\u000b" + ("\b\uffff\u0001\u0002\u000b\uffff\u0001\r\u001a\uffff\u0001\f\u0002\uffff\u0001\u0001\u000f\uffff\u0001\u0007\u0005" + "\uffff\u0001\u0003\u0001\u0006"), "", "", "", "", "", "", "", "\u0001\u0018\u0012\uffff\u0001\u000f\u0005\uffff\u0001\u0015\u0013\uffff\u0001\u0017\u0005\uffff\u0001\u0015\u000b\uffff" + ("\u0001\u000e\u0007\uffff\u0001\u0014\f\uffff\u0001\u0016\u000b\uffff\u0001\u0013\u0010\uffff\u0001\u0011\u0003\uffff" + "\u0001\u0012\u0004\uffff\u0001\u0010"), "\u0001 \u0012\uffff\u0001\u001a\u0019\uffff\u0001\u001f\u0005\uffff\u0001\u001b\u000b\uffff\u0001\u0019\u0007\uffff" + "\u0001\"\u0018\uffff\u0001!\u0010\uffff\u0001\u001d\u0003\uffff\u0001\u001e\u0004\uffff\u0001\u001c", "\u0001#+\uffff\u0001$\u0007\uffff\u0001(\u0018\uffff\u0001\'\u0014\uffff\u0001&\u0004\uffff" + "\u0001%", "\u0001+\u0004\uffff\u0001+\u0001)\u0001\uffff\u0001*\u0002\uffff\u0001+\u0001\uffff\u0001+\u0001*" + ((((("\u0002\uffff\u0003+\u0001\uffff\u0003+\u0001\uffff\u0004+\u0001*\u0003+\u0003\uffff\u0001*\u0002+\u0001" + "*\u0001+\u0001\uffff\u0001*\u0004+\u0001\uffff\u0001+\u0001\uffff\u0002+\u0001\uffff\u0001+\u0003\uffff") + "\u0001+\u0001\uffff\u0002+\u0001\uffff\u0001+\u0002\uffff\u0003+\u0001\uffff\u0003+\u0001\uffff\u0003+") + "\u0003\uffff\u0001*\u0001\uffff\u0001+\u0001\uffff\u0001+\u0004\uffff\u0001+\u0002\uffff\u0005+\u0003\uffff") + "\u0001+\u0001\uffff\u0002+\u0001*\u0001\uffff\u000b+\u0002\uffff\u0001+\u0001\uffff\u0003+\u0004\uffff") + "\u0002+\u0001\uffff\u0004+\u0003\uffff\u0001+\b\uffff\u0002+\u0002\uffff\u0001+"), "\u0001.\u0004\uffff\u0001.\u0001,\u0001\uffff\u0001-\u0002\uffff\u0001.\u0001\uffff\u0001.\u0001-" + ((((("\u0002\uffff\u0003.\u0001\uffff\u0003.\u0001\uffff\u0004.\u0001-\u0003.\u0003\uffff\u0001-\u0002.\u0001" + "-\u0001.\u0001\uffff\u0001-\u0004.\u0001\uffff\u0001.\u0001\uffff\u0002.\u0001\uffff\u0001.\u0003\uffff") + "\u0001.\u0001\uffff\u0002.\u0001\uffff\u0001.\u0002\uffff\u0003.\u0001\uffff\u0003.\u0001\uffff\u0003.") + "\u0003\uffff\u0001-\u0001\uffff\u0001.\u0001\uffff\u0001.\u0004\uffff\u0001.\u0002\uffff\u0005.\u0003\uffff") + "\u0001.\u0001\uffff\u0002.\u0001-\u0001\uffff\u000b.\u0002\uffff\u0001.\u0001\uffff\u0003.\u0004\uffff") + "\u0002.\u0001\uffff\u0004.\u0003\uffff\u0001.\b\uffff\u0002.\u0002\uffff\u0001."), "\u00011\u0001\uffff\u00011\u0005\uffff\u00011\u000e\uffff\u00011\u0006\uffff\u00011\u0002\uffff\u0001" + "1\u0002\uffff\u00011$\uffff\u00011\u0016\uffff\u00010\u00011\u0018\uffff\u0001/", "", "", "", "", "", "", "", "", "\u00012", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "\u0001*\u0007\uffff\u0001*\u0014\uffff\u0001+", "", "", "\u0001.\'\uffff\u0001-\u0007\uffff\u0001-", "", "", "", "", "", "\u0001\u0018,\uffff\u0001\u0017" };

	static final short[] DFA1_eot = DFA.unpackEncodedString(Cql_Parser.DFA1_eotS);

	static final short[] DFA1_eof = DFA.unpackEncodedString(Cql_Parser.DFA1_eofS);

	static final char[] DFA1_min = DFA.unpackEncodedStringToUnsignedChars(Cql_Parser.DFA1_minS);

	static final char[] DFA1_max = DFA.unpackEncodedStringToUnsignedChars(Cql_Parser.DFA1_maxS);

	static final short[] DFA1_accept = DFA.unpackEncodedString(Cql_Parser.DFA1_acceptS);

	static final short[] DFA1_special = DFA.unpackEncodedString(Cql_Parser.DFA1_specialS);

	static final short[][] DFA1_transition;

	static {
		int numStates = Cql_Parser.DFA1_transitionS.length;
		DFA1_transition = new short[numStates][];
		for (int i = 0; i < numStates; i++) {
			Cql_Parser.DFA1_transition[i] = DFA.unpackEncodedString(Cql_Parser.DFA1_transitionS[i]);
		}
	}

	protected class DFA1 extends DFA {
		public DFA1(BaseRecognizer recognizer) {
			this.recognizer = recognizer;
			this.decisionNumber = 1;
			this.eot = Cql_Parser.DFA1_eot;
			this.eof = Cql_Parser.DFA1_eof;
			this.min = Cql_Parser.DFA1_min;
			this.max = Cql_Parser.DFA1_max;
			this.accept = Cql_Parser.DFA1_accept;
			this.special = Cql_Parser.DFA1_special;
			this.transition = Cql_Parser.DFA1_transition;
		}

		@Override
		public String getDescription() {
			return "207:1: cqlStatement returns [ParsedStatement stmt] : (st1= selectStatement |st2= insertStatement |st3= updateStatement |st4= batchStatement |st5= deleteStatement |st6= useStatement |st7= truncateStatement |st8= createKeyspaceStatement |st9= createTableStatement |st10= createIndexStatement |st11= dropKeyspaceStatement |st12= dropTableStatement |st13= dropIndexStatement |st14= alterTableStatement |st15= alterKeyspaceStatement |st16= grantPermissionsStatement |st17= revokePermissionsStatement |st18= listPermissionsStatement |st19= createUserStatement |st20= alterUserStatement |st21= dropUserStatement |st22= listUsersStatement |st23= createTriggerStatement |st24= dropTriggerStatement |st25= createTypeStatement |st26= alterTypeStatement |st27= dropTypeStatement |st28= createFunctionStatement |st29= dropFunctionStatement |st30= createAggregateStatement |st31= dropAggregateStatement |st32= createRoleStatement |st33= alterRoleStatement |st34= dropRoleStatement |st35= listRolesStatement |st36= grantRoleStatement |st37= revokeRoleStatement |st38= createMaterializedViewStatement |st39= dropMaterializedViewStatement |st40= alterMaterializedViewStatement );";
		}
	}

	static final String DFA15_eotS = "\u0082\uffff";

	static final String DFA15_eofS = "\u0082\uffff";

	static final String DFA15_minS = "\u0001\u0006\u0001\uffff\u0019\"\u0001\uffff\u0001\u0006\u0005\"\u0001\uffff\u0001\u0017\u0001\u0006\u0001\u00b9\u0019\u00b8" + ("\u0001\u00b9\u0002\u00b8\u0001\uffff\u0001\u00b8\u0001\u00bf\u0001\u00b8\u0001\u0017\u0003\uffff\u0019\"\u0001" + "\uffff\u0001\u0006\u0001\u0017\u0019\"\u0003\u00b8");

	static final String DFA15_maxS = "\u0001\u00cb\u0001\uffff\u0019\u00bf\u0001\uffff\u0001\u00cb\u0005\u00bf\u0001\uffff\u0001\u00aa\u0001\u00cb" + ("\u0001\u00bc\u0002\u00bf\u0001\u00c2\u0017\u00bf\u0002\u00c2\u0001\uffff\u0001\u00c2\u0002\u00bf\u0001\u00aa" + "\u0003\uffff\u0019\u00bf\u0001\uffff\u0001\u00cb\u0001\u00aa\u0019\u00bf\u0003\u00b9");

	static final String DFA15_acceptS = "\u0001\uffff\u0001\u0001\u0019\uffff\u0001\u0002\u0006\uffff\u0001\b\u001f\uffff\u0001\u0003\u0004\uffff\u0001\u0005\u0001\u0006\u0001" + "\u0007\u0019\uffff\u0001\u0004\u001e\uffff";

	static final String DFA15_specialS = "\u0082\uffff}>";

	static final String[] DFA15_transitionS = new String[]{ "\u0001\u001b\u0004\uffff\u0001\u001b\u0002\uffff\u0001\u0001\u0002\uffff\u0001\u001b\u0003\uffff\u0001\u001b\u0001\uffff\u0001\u0002" + (((((((("\u0001\u001b\u0003\uffff\u0002\u0004\u0004\uffff\u0001\u0004\u0001\uffff\u0001\u0005\u0003\uffff\u0001\u0006\u0001\u0007\u0001\b\u0001\uffff" + "\u0001\u0004\u0001 \u0001\u0004\u0001\uffff\u0002\u0004\u0001\u001a\u0001\t\u0001\uffff\u0001\u0004\u0001\u0018\u0001\n\u0004\uffff\u0001!") + "\u0001\u000b\u0001\uffff\u0001\f\u0002\uffff\u0003\u0004\u0001\r\u0001\uffff\u0001\u0004\u0001\uffff\u0002\u0004\u0001\uffff\u0001") + "\u0004\u0003\uffff\u0001\u000e\u0001\u001b\u0002\u0004\u0001\uffff\u0001\u000f\u0002\uffff\u0002!\u0001\u0004\u0001\uffff\u0003\u0004\u0001") + "\uffff\u0003\u0004\u0004\uffff\u0001\u001b\u0001\u0004\u0001\uffff\u0001\u0004\u0001\uffff\u0001\u001b\u0002\uffff\u0001\u0004\u0002\uffff") + "\u0005\u0004\u0003\uffff\u0001\u0004\u0001\uffff\u0002\u0004\u0002\uffff\u0001\u0004\u0001\u0010\u0004\u0004\u0001\u0011\u0001\u0019\u0001\u0012\u0001\u0017") + "\u0001\u0013\u0001\uffff\u0001\"\u0001\u0004\u0001\uffff\u0001\u001f\u0002\u0004\u0004\uffff\u0002\u0004\u0001\uffff\u0001\u0014\u0001\u0004") + "\u0001\u0015\u0001\u0016\u0003\uffff\u0001\u001e\b\uffff\u0001\u001d\u0001\u0003\u0002\uffff\u0001\u001b\u0002\uffff\u0001\u001b\u0007") + "\uffff\u0001\u001c\u0004\uffff\u0001\u001b\u0002\uffff\u0001\u001b\u0006\uffff\u0001\u001b\u0003\uffff\u0001\u001b"), "", "\u0001\u0001#\uffff\u0001\u0001q\uffff\u0001\"\u0001\u0001\u0002\uffff\u0001\u0001\u0002\uffff\u0001#", "\u0001\u0001#\uffff\u0001\u0001q\uffff\u0001\"\u0001\u0001\u0002\uffff\u0001\u0001\u0002\uffff\u0001#", "\u0001\u0001#\uffff\u0001\u0001q\uffff\u0001\"\u0001\u0001\u0002\uffff\u0001\u0001\u0002\uffff\u0001#", "\u0001\u0001#\uffff\u0001\u0001q\uffff\u0001\"\u0001\u0001\u0002\uffff\u0001\u0001\u0002\uffff\u0001#", "\u0001\u0001#\uffff\u0001\u0001q\uffff\u0001\"\u0001\u0001\u0002\uffff\u0001\u0001\u0002\uffff\u0001#", "\u0001\u0001#\uffff\u0001\u0001q\uffff\u0001\"\u0001\u0001\u0002\uffff\u0001\u0001\u0002\uffff\u0001#", "\u0001\u0001#\uffff\u0001\u0001q\uffff\u0001\"\u0001\u0001\u0002\uffff\u0001\u0001\u0002\uffff\u0001#", "\u0001\u0001#\uffff\u0001\u0001q\uffff\u0001\"\u0001\u0001\u0002\uffff\u0001\u0001\u0002\uffff\u0001#", "\u0001\u0001#\uffff\u0001\u0001q\uffff\u0001\"\u0001\u0001\u0002\uffff\u0001\u0001\u0002\uffff\u0001#", "\u0001\u0001#\uffff\u0001\u0001q\uffff\u0001\"\u0001\u0001\u0002\uffff\u0001\u0001\u0002\uffff\u0001#", "\u0001\u0001#\uffff\u0001\u0001q\uffff\u0001\"\u0001\u0001\u0002\uffff\u0001\u0001\u0002\uffff\u0001#", "\u0001\u0001#\uffff\u0001\u0001q\uffff\u0001\"\u0001\u0001\u0002\uffff\u0001\u0001\u0002\uffff\u0001#", "\u0001\u0001#\uffff\u0001\u0001q\uffff\u0001\"\u0001\u0001\u0002\uffff\u0001\u0001\u0002\uffff\u0001#", "\u0001\u0001#\uffff\u0001\u0001q\uffff\u0001\"\u0001\u0001\u0002\uffff\u0001\u0001\u0002\uffff\u0001#", "\u0001\u0001#\uffff\u0001\u0001q\uffff\u0001\"\u0001\u0001\u0002\uffff\u0001\u0001\u0002\uffff\u0001#", "\u0001\u0001#\uffff\u0001\u0001q\uffff\u0001\"\u0001\u0001\u0002\uffff\u0001\u0001\u0002\uffff\u0001#", "\u0001\u0001#\uffff\u0001\u0001q\uffff\u0001\"\u0001\u0001\u0002\uffff\u0001\u0001\u0002\uffff\u0001#", "\u0001\u0001#\uffff\u0001\u0001q\uffff\u0001\"\u0001\u0001\u0002\uffff\u0001\u0001\u0002\uffff\u0001#", "\u0001\u0001#\uffff\u0001\u0001q\uffff\u0001\"\u0001\u0001\u0002\uffff\u0001\u0001\u0002\uffff\u0001#", "\u0001\u0001#\uffff\u0001\u0001q\uffff\u0001\"\u0001\u0001\u0002\uffff\u0001\u0001\u0002\uffff\u0001#", "\u0001\u0001#\uffff\u0001\u0001q\uffff\u0001\"\u0001\u0001\u0002\uffff\u0001\u0001\u0002\uffff\u0001#", "\u0001\u0001#\uffff\u0001\u0001q\uffff\u0001\"\u0001\u0001\u0002\uffff\u0001\u0001\u0002\uffff\u0001#", "\u0001\u0001#\uffff\u0001\u0001q\uffff\u0001\"\u0001\u0001\u0002\uffff\u0001\u0001\u0002\uffff\u0001#", "\u0001\u0001#\uffff\u0001\u0001q\uffff\u0001\"\u0001\u0001\u0002\uffff\u0001\u0001\u0002\uffff\u0001#", "\u0001\u0001#\uffff\u0001\u0001q\uffff\u0001$\u0001\u0001\u0002\uffff\u0001\u0001\u0002\uffff\u0001#", "", "\u0001\u001b\u0004\uffff\u0001\u001b\u0005\uffff\u0001\u001b\u0003\uffff\u0001\u001b\u0001\uffff\u0001&\u0001\u001b\u0003\uffff" + ((((((((("\u0002E\u0004\uffff\u0001E\u0001\uffff\u0001)\u0003\uffff\u0001*\u0001+\u0001,\u0001\uffff\u0001E" + "\u0001D\u0001E\u0001\uffff\u0002E\u0001>\u0001-\u0001\uffff\u0001E\u0001<\u0001.\u0004\uffff") + "\u0001D\u0001/\u0001\uffff\u00010\u0002\uffff\u0003E\u00011\u0001\uffff\u0001C\u0001\uffff\u0002E") + "\u0001\uffff\u0001E\u0003\uffff\u00012\u0001\u001b\u0002E\u0001\uffff\u00013\u0002\uffff\u0001D\u0001?") + "\u0001E\u0001\uffff\u0003E\u0001\uffff\u0001@\u0001E\u0001(\u0004\uffff\u0001\u001b\u0001E\u0001\uffff") + "\u0001E\u0001\uffff\u0001\u001b\u0002\uffff\u0001E\u0002\uffff\u0005E\u0003\uffff\u0001E\u0001\uffff") + "\u0002E\u0001\uffff\u0001B\u0001E\u00014\u0004E\u00015\u0001=\u00016\u0001;\u00017\u0001\uffff") + "\u0001\u001b\u0001E\u0001\uffff\u0001D\u0001A\u0001E\u0004\uffff\u0002E\u0001\uffff\u00018\u0001E") + "\u00019\u0001:\u0003\uffff\u0001D\b\uffff\u0001\u001b\u0001\'\u0002\uffff\u0001%\u0002\uffff\u0001\u001b") + "\u0007\uffff\u0001\u001b\u0004\uffff\u0001\u001b\u0002\uffff\u0001\u001b\u0006\uffff\u0001\u001b\u0003\uffff\u0001\u001b"), "\u0001\u001b#\uffff\u0001\u001br\uffff\u0001\u001b\u0002\uffff\u0001\u001b\u0002\uffff\u0001F", "\u0001\u0001#\uffff\u0001\u0001q\uffff\u0001G\u0001\u0001\u0002\uffff\u0001\u0001\u0002\uffff\u0001#", "\u0001\u0001#\uffff\u0001\u0001q\uffff\u0001H\u0001\u0001\u0002\uffff\u0001\u0001\u0002\uffff\u0001#", "\u0001\u0001#\uffff\u0001\u0001q\uffff\u0001I\u0001\u0001\u0002\uffff\u0001\u0001\u0002\uffff\u0001#", "\u0001\u0001#\uffff\u0001\u0001r\uffff\u0001\u0001\u0002\uffff\u0001\u0001\u0002\uffff\u0001#", "", "\u0001J\u0004\uffff\u0002L\u0004\uffff\u0001L\u0001\uffff\u0001M\u0003\uffff\u0001N\u0001O" + ((((((("\u0001P\u0001\uffff\u0001L\u0001\u0001\u0001L\u0001\uffff\u0002L\u0001b\u0001Q\u0001\uffff\u0001L" + "\u0001`\u0001R\u0004\uffff\u0001\u0001\u0001S\u0001\uffff\u0001T\u0002\uffff\u0003L\u0001U\u0001\uffff") + "\u0001L\u0001\uffff\u0002L\u0001\uffff\u0001L\u0003\uffff\u0001V\u0001\uffff\u0002L\u0001\uffff") + "\u0001W\u0002\uffff\u0002\u0001\u0001L\u0001\uffff\u0003L\u0001\uffff\u0003L\u0005\uffff\u0001L\u0001") + "\uffff\u0001L\u0004\uffff\u0001L\u0002\uffff\u0005L\u0003\uffff\u0001L\u0001\uffff\u0002L") + "\u0002\uffff\u0001L\u0001X\u0004L\u0001Y\u0001a\u0001Z\u0001_\u0001[\u0001\uffff\u0001\"") + "\u0001L\u0001\uffff\u0001\u0001\u0002L\u0004\uffff\u0002L\u0001\uffff\u0001\\\u0001L\u0001]\u0001^") + "\u0003\uffff\u0001\u0001\t\uffff\u0001K"), "\u0001\"\u0004\uffff\u0001\"\u0002\uffff\u0001\"\u0002\uffff\u0001\"\u0003\uffff\u0001\"\u0001\uffff\u0002" + ((((((("\"\u0003\uffff\u0002\"\u0004\uffff\u0001\"\u0001\uffff\u0001\"\u0003\uffff\u0003\"\u0001\uffff\u0003\"" + "\u0001\uffff\u0004\"\u0001\uffff\u0003\"\u0004\uffff\u0002\"\u0001\uffff\u0001\"\u0002\uffff\u0004\"\u0001\uffff") + "\u0001\"\u0001\uffff\u0002\"\u0001\uffff\u0001\"\u0003\uffff\u0004\"\u0001\uffff\u0001\"\u0002\uffff\u0003\"") + "\u0001\uffff\u0003\"\u0001\uffff\u0003\"\u0004\uffff\u0002\"\u0001\uffff\u0001\"\u0001\uffff\u0001\"\u0002\uffff") + "\u0001\"\u0002\uffff\u0005\"\u0003\uffff\u0001\"\u0001\uffff\u0002\"\u0002\uffff\u000b\"\u0001\uffff\u0002") + "\"\u0001\uffff\u0003\"\u0004\uffff\u0002\"\u0001\uffff\u0004\"\u0003\uffff\u0001\"\b\uffff\u0002\"") + "\u0002\uffff\u0001\"\u0002\uffff\u0001\"\u0007\uffff\u0002\"\u0003\uffff\u0001\"\u0002\uffff\u0001\"\u0006\uffff") + "\u0001\"\u0001c\u0002\uffff\u0001\""), "\u0001d\u0002\uffff\u0001\u001b", "\u0001\u001b\u0001B\u0005\uffff\u0001e", "\u0001\u001b\u0001B\u0005\uffff\u0001e", "\u0001\u001b\u0001B\u0005\uffff\u0001e\u0002\uffff\u0001B", "\u0001\u001b\u0001B\u0005\uffff\u0001e", "\u0001\u001b\u0001B\u0005\uffff\u0001e", "\u0001\u001b\u0001B\u0005\uffff\u0001e", "\u0001\u001b\u0001B\u0005\uffff\u0001e", "\u0001\u001b\u0001B\u0005\uffff\u0001e", "\u0001\u001b\u0001B\u0005\uffff\u0001e", "\u0001\u001b\u0001B\u0005\uffff\u0001e", "\u0001\u001b\u0001B\u0005\uffff\u0001e", "\u0001\u001b\u0001B\u0005\uffff\u0001e", "\u0001\u001b\u0001B\u0005\uffff\u0001e", "\u0001\u001b\u0001B\u0005\uffff\u0001e", "\u0001\u001b\u0001B\u0005\uffff\u0001e", "\u0001\u001b\u0001B\u0005\uffff\u0001e", "\u0001\u001b\u0001B\u0005\uffff\u0001e", "\u0001\u001b\u0001B\u0005\uffff\u0001e", "\u0001\u001b\u0001B\u0005\uffff\u0001e", "\u0001\u001b\u0001B\u0005\uffff\u0001e", "\u0001\u001b\u0001B\u0005\uffff\u0001e", "\u0001\u001b\u0001B\u0005\uffff\u0001e", "\u0001\u001b\u0001B\u0005\uffff\u0001e", "\u0001\u001b\u0001B\u0005\uffff\u0001e", "\u0001\u001b\u0006\uffff\u0001e", "\u0001B\u0005\uffff\u0001e", "\u0001\u001b\u0001B\u0005\uffff\u0001e\u0002\uffff\u0001B", "\u0001\u001b\u0001B\u0005\uffff\u0001e\u0002\uffff\u0001B", "", "\u0001\u001b\u0001B\u0005\uffff\u0001e\u0002\uffff\u0001B", "\u0001e", "\u0001\u001b\u0001B\u0005\uffff\u0001e", "\u0001f\u0004\uffff\u0002h\u0004\uffff\u0001h\u0001\uffff\u0001i\u0003\uffff\u0001j\u0001k" + ((((((("\u0001l\u0001\uffff\u0001h\u0001\u001b\u0001h\u0001\uffff\u0002h\u0001~\u0001m\u0001\uffff\u0001h" + "\u0001|\u0001n\u0004\uffff\u0001\u001b\u0001o\u0001\uffff\u0001p\u0002\uffff\u0003h\u0001q\u0001\uffff") + "\u0001h\u0001\uffff\u0002h\u0001\uffff\u0001h\u0003\uffff\u0001r\u0001\uffff\u0002h\u0001\uffff") + "\u0001s\u0002\uffff\u0002\u001b\u0001h\u0001\uffff\u0003h\u0001\uffff\u0003h\u0005\uffff\u0001h\u0001") + "\uffff\u0001h\u0004\uffff\u0001h\u0002\uffff\u0005h\u0003\uffff\u0001h\u0001\uffff\u0002h") + "\u0002\uffff\u0001h\u0001t\u0004h\u0001u\u0001}\u0001v\u0001{\u0001w\u0001\uffff\u0001\"") + "\u0001h\u0001\uffff\u0001\u001b\u0002h\u0004\uffff\u0002h\u0001\uffff\u0001x\u0001h\u0001y\u0001z") + "\u0003\uffff\u0001\u001b\t\uffff\u0001g"), "", "", "", "\u0001\u0001#\uffff\u0001\u0001q\uffff\u0001\"\u0001\u0001\u0002\uffff\u0001\u0001\u0002\uffff\u0001\u0001", "\u0001\u0001#\uffff\u0001\u0001q\uffff\u0001\"\u0001\u0001\u0002\uffff\u0001\u0001\u0002\uffff\u0001\u0001", "\u0001\u0001#\uffff\u0001\u0001q\uffff\u0001\"\u0001\u0001\u0002\uffff\u0001\u0001\u0002\uffff\u0001\u0001", "\u0001\u0001#\uffff\u0001\u0001q\uffff\u0001\"\u0001\u0001\u0002\uffff\u0001\u0001\u0002\uffff\u0001\u0001", "\u0001\u0001#\uffff\u0001\u0001q\uffff\u0001\"\u0001\u0001\u0002\uffff\u0001\u0001\u0002\uffff\u0001\u0001", "\u0001\u0001#\uffff\u0001\u0001q\uffff\u0001\"\u0001\u0001\u0002\uffff\u0001\u0001\u0002\uffff\u0001\u0001", "\u0001\u0001#\uffff\u0001\u0001q\uffff\u0001\"\u0001\u0001\u0002\uffff\u0001\u0001\u0002\uffff\u0001\u0001", "\u0001\u0001#\uffff\u0001\u0001q\uffff\u0001\"\u0001\u0001\u0002\uffff\u0001\u0001\u0002\uffff\u0001\u0001", "\u0001\u0001#\uffff\u0001\u0001q\uffff\u0001\"\u0001\u0001\u0002\uffff\u0001\u0001\u0002\uffff\u0001\u0001", "\u0001\u0001#\uffff\u0001\u0001q\uffff\u0001\"\u0001\u0001\u0002\uffff\u0001\u0001\u0002\uffff\u0001\u0001", "\u0001\u0001#\uffff\u0001\u0001q\uffff\u0001\"\u0001\u0001\u0002\uffff\u0001\u0001\u0002\uffff\u0001\u0001", "\u0001\u0001#\uffff\u0001\u0001q\uffff\u0001\"\u0001\u0001\u0002\uffff\u0001\u0001\u0002\uffff\u0001\u0001", "\u0001\u0001#\uffff\u0001\u0001q\uffff\u0001\"\u0001\u0001\u0002\uffff\u0001\u0001\u0002\uffff\u0001\u0001", "\u0001\u0001#\uffff\u0001\u0001q\uffff\u0001\"\u0001\u0001\u0002\uffff\u0001\u0001\u0002\uffff\u0001\u0001", "\u0001\u0001#\uffff\u0001\u0001q\uffff\u0001\"\u0001\u0001\u0002\uffff\u0001\u0001\u0002\uffff\u0001\u0001", "\u0001\u0001#\uffff\u0001\u0001q\uffff\u0001\"\u0001\u0001\u0002\uffff\u0001\u0001\u0002\uffff\u0001\u0001", "\u0001\u0001#\uffff\u0001\u0001q\uffff\u0001\"\u0001\u0001\u0002\uffff\u0001\u0001\u0002\uffff\u0001\u0001", "\u0001\u0001#\uffff\u0001\u0001q\uffff\u0001\"\u0001\u0001\u0002\uffff\u0001\u0001\u0002\uffff\u0001\u0001", "\u0001\u0001#\uffff\u0001\u0001q\uffff\u0001\"\u0001\u0001\u0002\uffff\u0001\u0001\u0002\uffff\u0001\u0001", "\u0001\u0001#\uffff\u0001\u0001q\uffff\u0001\"\u0001\u0001\u0002\uffff\u0001\u0001\u0002\uffff\u0001\u0001", "\u0001\u0001#\uffff\u0001\u0001q\uffff\u0001\"\u0001\u0001\u0002\uffff\u0001\u0001\u0002\uffff\u0001\u0001", "\u0001\u0001#\uffff\u0001\u0001q\uffff\u0001\"\u0001\u0001\u0002\uffff\u0001\u0001\u0002\uffff\u0001\u0001", "\u0001\u0001#\uffff\u0001\u0001q\uffff\u0001\"\u0001\u0001\u0002\uffff\u0001\u0001\u0002\uffff\u0001\u0001", "\u0001\u0001#\uffff\u0001\u0001q\uffff\u0001\"\u0001\u0001\u0002\uffff\u0001\u0001\u0002\uffff\u0001\u0001", "\u0001\u0001#\uffff\u0001\u0001q\uffff\u0001\"\u0001\u0001\u0002\uffff\u0001\u0001\u0002\uffff\u0001\u0001", "", "\u0001B\u0004\uffff\u0001B\u0005\uffff\u0001B\u0003\uffff\u0001B\u0002\uffff\u0001B\t\uffff" + (("\u0001\u001b#\uffff\u0001\u001b\n\uffff\u0001B\u0015\uffff\u0001B\u0004\uffff\u0001B<\uffff" + "\u0001B\u0003\uffff\u0001B\u0002\uffff\u0001B\u0007\uffff\u0001B\u0001\u001b\u0002\uffff\u0001\u001b\u0001") + "B\u0001\uffff\u0001\u001b\u0001B\u0006\uffff\u0001B\u0003\uffff\u0001B"), "\u0001\u007f\u0004\uffff\u0002\u0081\u0004\uffff\u0001\u0081\u0001\uffff\u0001\u001b\u0003\uffff\u0003\u001b\u0001\uffff" + (((((("\u0001\u0081\u0001\uffff\u0001\u0081\u0001\uffff\u0002\u0081\u0002\u001b\u0001\uffff\u0001\u0081\u0002\u001b\u0005" + "\uffff\u0001\u001b\u0001\uffff\u0001\u001b\u0002\uffff\u0003\u0081\u0001\u001b\u0001\uffff\u0001\u0081\u0001\uffff") + "\u0002\u0081\u0001\uffff\u0001\u0081\u0003\uffff\u0001\u001b\u0001\uffff\u0002\u0081\u0001\uffff\u0001\u001b\u0003") + "\uffff\u0001B\u0001\u0081\u0001\uffff\u0003\u0081\u0001\uffff\u0003\u0081\u0005\uffff\u0001\u0081") + "\u0001\uffff\u0001\u0081\u0004\uffff\u0001\u0081\u0002\uffff\u0005\u0081\u0003\uffff\u0001\u0081\u0001\uffff") + "\u0002\u0081\u0002\uffff\u0001\u0081\u0001\u001b\u0004\u0081\u0005\u001b\u0001\uffff\u0001\u001b\u0001\u0081\u0002\uffff") + "\u0002\u0081\u0004\uffff\u0002\u0081\u0001\uffff\u0001\u001b\u0001\u0081\u0002\u001b\r\uffff\u0001\u0080"), "\u0001\u001b#\uffff\u0001\u001bq\uffff\u0001\"\u0001\u001b\u0002\uffff\u0001\u001b\u0002\uffff\u0001\u001b", "\u0001\u001b#\uffff\u0001\u001bq\uffff\u0001\"\u0001\u001b\u0002\uffff\u0001\u001b\u0002\uffff\u0001\u001b", "\u0001\u001b#\uffff\u0001\u001bq\uffff\u0001\"\u0001\u001b\u0002\uffff\u0001\u001b\u0002\uffff\u0001\u001b", "\u0001\u001b#\uffff\u0001\u001bq\uffff\u0001\"\u0001\u001b\u0002\uffff\u0001\u001b\u0002\uffff\u0001\u001b", "\u0001\u001b#\uffff\u0001\u001bq\uffff\u0001\"\u0001\u001b\u0002\uffff\u0001\u001b\u0002\uffff\u0001\u001b", "\u0001\u001b#\uffff\u0001\u001bq\uffff\u0001\"\u0001\u001b\u0002\uffff\u0001\u001b\u0002\uffff\u0001\u001b", "\u0001\u001b#\uffff\u0001\u001bq\uffff\u0001\"\u0001\u001b\u0002\uffff\u0001\u001b\u0002\uffff\u0001\u001b", "\u0001\u001b#\uffff\u0001\u001bq\uffff\u0001\"\u0001\u001b\u0002\uffff\u0001\u001b\u0002\uffff\u0001\u001b", "\u0001\u001b#\uffff\u0001\u001bq\uffff\u0001\"\u0001\u001b\u0002\uffff\u0001\u001b\u0002\uffff\u0001\u001b", "\u0001\u001b#\uffff\u0001\u001bq\uffff\u0001\"\u0001\u001b\u0002\uffff\u0001\u001b\u0002\uffff\u0001\u001b", "\u0001\u001b#\uffff\u0001\u001bq\uffff\u0001\"\u0001\u001b\u0002\uffff\u0001\u001b\u0002\uffff\u0001\u001b", "\u0001\u001b#\uffff\u0001\u001bq\uffff\u0001\"\u0001\u001b\u0002\uffff\u0001\u001b\u0002\uffff\u0001\u001b", "\u0001\u001b#\uffff\u0001\u001bq\uffff\u0001\"\u0001\u001b\u0002\uffff\u0001\u001b\u0002\uffff\u0001\u001b", "\u0001\u001b#\uffff\u0001\u001bq\uffff\u0001\"\u0001\u001b\u0002\uffff\u0001\u001b\u0002\uffff\u0001\u001b", "\u0001\u001b#\uffff\u0001\u001bq\uffff\u0001\"\u0001\u001b\u0002\uffff\u0001\u001b\u0002\uffff\u0001\u001b", "\u0001\u001b#\uffff\u0001\u001bq\uffff\u0001\"\u0001\u001b\u0002\uffff\u0001\u001b\u0002\uffff\u0001\u001b", "\u0001\u001b#\uffff\u0001\u001bq\uffff\u0001\"\u0001\u001b\u0002\uffff\u0001\u001b\u0002\uffff\u0001\u001b", "\u0001\u001b#\uffff\u0001\u001bq\uffff\u0001\"\u0001\u001b\u0002\uffff\u0001\u001b\u0002\uffff\u0001\u001b", "\u0001\u001b#\uffff\u0001\u001bq\uffff\u0001\"\u0001\u001b\u0002\uffff\u0001\u001b\u0002\uffff\u0001\u001b", "\u0001\u001b#\uffff\u0001\u001bq\uffff\u0001\"\u0001\u001b\u0002\uffff\u0001\u001b\u0002\uffff\u0001\u001b", "\u0001\u001b#\uffff\u0001\u001bq\uffff\u0001\"\u0001\u001b\u0002\uffff\u0001\u001b\u0002\uffff\u0001\u001b", "\u0001\u001b#\uffff\u0001\u001bq\uffff\u0001\"\u0001\u001b\u0002\uffff\u0001\u001b\u0002\uffff\u0001\u001b", "\u0001\u001b#\uffff\u0001\u001bq\uffff\u0001\"\u0001\u001b\u0002\uffff\u0001\u001b\u0002\uffff\u0001\u001b", "\u0001\u001b#\uffff\u0001\u001bq\uffff\u0001\"\u0001\u001b\u0002\uffff\u0001\u001b\u0002\uffff\u0001\u001b", "\u0001\u001b#\uffff\u0001\u001bq\uffff\u0001\"\u0001\u001b\u0002\uffff\u0001\u001b\u0002\uffff\u0001\u001b", "\u0001\u001b\u0001B", "\u0001\u001b\u0001B", "\u0001\u001b\u0001B" };

	static final short[] DFA15_eot = DFA.unpackEncodedString(Cql_Parser.DFA15_eotS);

	static final short[] DFA15_eof = DFA.unpackEncodedString(Cql_Parser.DFA15_eofS);

	static final char[] DFA15_min = DFA.unpackEncodedStringToUnsignedChars(Cql_Parser.DFA15_minS);

	static final char[] DFA15_max = DFA.unpackEncodedStringToUnsignedChars(Cql_Parser.DFA15_maxS);

	static final short[] DFA15_accept = DFA.unpackEncodedString(Cql_Parser.DFA15_acceptS);

	static final short[] DFA15_special = DFA.unpackEncodedString(Cql_Parser.DFA15_specialS);

	static final short[][] DFA15_transition;

	static {
		int numStates = Cql_Parser.DFA15_transitionS.length;
		DFA15_transition = new short[numStates][];
		for (int i = 0; i < numStates; i++) {
			Cql_Parser.DFA15_transition[i] = DFA.unpackEncodedString(Cql_Parser.DFA15_transitionS[i]);
		}
	}

	protected class DFA15 extends DFA {
		public DFA15(BaseRecognizer recognizer) {
			this.recognizer = recognizer;
			this.decisionNumber = 15;
			this.eot = Cql_Parser.DFA15_eot;
			this.eof = Cql_Parser.DFA15_eof;
			this.min = Cql_Parser.DFA15_min;
			this.max = Cql_Parser.DFA15_max;
			this.accept = Cql_Parser.DFA15_accept;
			this.special = Cql_Parser.DFA15_special;
			this.transition = Cql_Parser.DFA15_transition;
		}

		@Override
		public String getDescription() {
			return "311:8: (c= cident |v= value | \'(\' ct= comparatorType \')\' v= value | K_COUNT \'(\' \'\\*\' \')\' | K_WRITETIME \'(\' c= cident \')\' | K_TTL \'(\' c= cident \')\' | K_CAST \'(\' sn= unaliasedSelector K_AS t= native_type \')\' |f= functionName args= selectionFunctionArgs )";
		}
	}

	static final String DFA44_eotS = "\u001e\uffff";

	static final String DFA44_eofS = "\u001e\uffff";

	static final String DFA44_minS = "\u0001\u000e\u001aF\u0003\uffff";

	static final String DFA44_maxS = "\u0001\u00aa\u001a\u00c7\u0003\uffff";

	static final String DFA44_acceptS = "\u001b\uffff\u0001\u0001\u0001\u0002\u0001\u0003";

	static final String DFA44_specialS = "\u001e\uffff}>";

	static final String[] DFA44_transitionS = new String[]{ "\u0001\u0001\b\uffff\u0001\u0002\u0004\uffff\u0002\u0004\u0004\uffff\u0001\u0004\u0001\uffff\u0001\u0005\u0003\uffff\u0001\u0006\u0001\u0007" + (((((("\u0001\b\u0001\uffff\u0001\u0004\u0001\u001a\u0001\u0004\u0001\uffff\u0002\u0004\u0001\u001a\u0001\t\u0001\uffff\u0001\u0004\u0001\u0018\u0001\n" + "\u0004\uffff\u0001\u001a\u0001\u000b\u0001\uffff\u0001\f\u0002\uffff\u0003\u0004\u0001\r\u0001\uffff\u0001\u0004\u0001\uffff") + "\u0002\u0004\u0001\uffff\u0001\u0004\u0003\uffff\u0001\u000e\u0001\uffff\u0002\u0004\u0001\uffff\u0001\u000f\u0002\uffff\u0002\u001a\u0001") + "\u0004\u0001\uffff\u0003\u0004\u0001\uffff\u0003\u0004\u0005\uffff\u0001\u0004\u0001\uffff\u0001\u0004\u0004\uffff\u0001\u0004\u0002\uffff") + "\u0005\u0004\u0003\uffff\u0001\u0004\u0001\uffff\u0002\u0004\u0002\uffff\u0001\u0004\u0001\u0010\u0004\u0004\u0001\u0011\u0001\u0019\u0001\u0012\u0001\u0017") + "\u0001\u0013\u0002\uffff\u0001\u0004\u0001\uffff\u0001\u001a\u0002\u0004\u0004\uffff\u0002\u0004\u0001\uffff\u0001\u0014\u0001\u0004\u0001\u0015") + "\u0001\u0016\u0003\uffff\u0001\u001a\t\uffff\u0001\u0003"), "\u0001\u001bu\uffff\u0001\u001b\u0002\uffff\u0001\u001d\u0007\uffff\u0001\u001c", "\u0001\u001bu\uffff\u0001\u001b\u0002\uffff\u0001\u001d\u0007\uffff\u0001\u001c", "\u0001\u001bu\uffff\u0001\u001b\u0002\uffff\u0001\u001d\u0007\uffff\u0001\u001c", "\u0001\u001bu\uffff\u0001\u001b\u0002\uffff\u0001\u001d\u0007\uffff\u0001\u001c", "\u0001\u001bu\uffff\u0001\u001b\u0002\uffff\u0001\u001d\u0007\uffff\u0001\u001c", "\u0001\u001bu\uffff\u0001\u001b\u0002\uffff\u0001\u001d\u0007\uffff\u0001\u001c", "\u0001\u001bu\uffff\u0001\u001b\u0002\uffff\u0001\u001d\u0007\uffff\u0001\u001c", "\u0001\u001bu\uffff\u0001\u001b\u0002\uffff\u0001\u001d\u0007\uffff\u0001\u001c", "\u0001\u001bu\uffff\u0001\u001b\u0002\uffff\u0001\u001d\u0007\uffff\u0001\u001c", "\u0001\u001bu\uffff\u0001\u001b\u0002\uffff\u0001\u001d\u0007\uffff\u0001\u001c", "\u0001\u001bu\uffff\u0001\u001b\u0002\uffff\u0001\u001d\u0007\uffff\u0001\u001c", "\u0001\u001bu\uffff\u0001\u001b\u0002\uffff\u0001\u001d\u0007\uffff\u0001\u001c", "\u0001\u001bu\uffff\u0001\u001b\u0002\uffff\u0001\u001d\u0007\uffff\u0001\u001c", "\u0001\u001bu\uffff\u0001\u001b\u0002\uffff\u0001\u001d\u0007\uffff\u0001\u001c", "\u0001\u001bu\uffff\u0001\u001b\u0002\uffff\u0001\u001d\u0007\uffff\u0001\u001c", "\u0001\u001bu\uffff\u0001\u001b\u0002\uffff\u0001\u001d\u0007\uffff\u0001\u001c", "\u0001\u001bu\uffff\u0001\u001b\u0002\uffff\u0001\u001d\u0007\uffff\u0001\u001c", "\u0001\u001bu\uffff\u0001\u001b\u0002\uffff\u0001\u001d\u0007\uffff\u0001\u001c", "\u0001\u001bu\uffff\u0001\u001b\u0002\uffff\u0001\u001d\u0007\uffff\u0001\u001c", "\u0001\u001bu\uffff\u0001\u001b\u0002\uffff\u0001\u001d\u0007\uffff\u0001\u001c", "\u0001\u001bu\uffff\u0001\u001b\u0002\uffff\u0001\u001d\u0007\uffff\u0001\u001c", "\u0001\u001bu\uffff\u0001\u001b\u0002\uffff\u0001\u001d\u0007\uffff\u0001\u001c", "\u0001\u001bu\uffff\u0001\u001b\u0002\uffff\u0001\u001d\u0007\uffff\u0001\u001c", "\u0001\u001bu\uffff\u0001\u001b\u0002\uffff\u0001\u001d\u0007\uffff\u0001\u001c", "\u0001\u001bu\uffff\u0001\u001b\u0002\uffff\u0001\u001d\u0007\uffff\u0001\u001c", "\u0001\u001bu\uffff\u0001\u001b\u0002\uffff\u0001\u001d\u0007\uffff\u0001\u001c", "", "", "" };

	static final short[] DFA44_eot = DFA.unpackEncodedString(Cql_Parser.DFA44_eotS);

	static final short[] DFA44_eof = DFA.unpackEncodedString(Cql_Parser.DFA44_eofS);

	static final char[] DFA44_min = DFA.unpackEncodedStringToUnsignedChars(Cql_Parser.DFA44_minS);

	static final char[] DFA44_max = DFA.unpackEncodedStringToUnsignedChars(Cql_Parser.DFA44_maxS);

	static final short[] DFA44_accept = DFA.unpackEncodedString(Cql_Parser.DFA44_acceptS);

	static final short[] DFA44_special = DFA.unpackEncodedString(Cql_Parser.DFA44_specialS);

	static final short[][] DFA44_transition;

	static {
		int numStates = Cql_Parser.DFA44_transitionS.length;
		DFA44_transition = new short[numStates][];
		for (int i = 0; i < numStates; i++) {
			Cql_Parser.DFA44_transition[i] = DFA.unpackEncodedString(Cql_Parser.DFA44_transitionS[i]);
		}
	}

	protected class DFA44 extends DFA {
		public DFA44(BaseRecognizer recognizer) {
			this.recognizer = recognizer;
			this.decisionNumber = 44;
			this.eot = Cql_Parser.DFA44_eot;
			this.eof = Cql_Parser.DFA44_eof;
			this.min = Cql_Parser.DFA44_min;
			this.max = Cql_Parser.DFA44_max;
			this.accept = Cql_Parser.DFA44_accept;
			this.special = Cql_Parser.DFA44_special;
			this.transition = Cql_Parser.DFA44_transition;
		}

		@Override
		public String getDescription() {
			return "482:1: deleteOp returns [Operation.RawDeletion op] : (c= cident |c= cident '[' t= term ']' |c= cident '.' field= fident );";
		}
	}

	static final String DFA154_eotS = "\u001d\uffff";

	static final String DFA154_eofS = "\u0001\uffff\u001a\u001c\u0002\uffff";

	static final String DFA154_minS = "\u0001\u0017\u001a\u00bf\u0002\uffff";

	static final String DFA154_maxS = "\u0001\u00aa\u001a\u00c1\u0002\uffff";

	static final String DFA154_acceptS = "\u001b\uffff\u0001\u0001\u0001\u0002";

	static final String DFA154_specialS = "\u001d\uffff}>";

	static final String[] DFA154_transitionS = new String[]{ "\u0001\u0001\u0004\uffff\u0002\u0003\u0004\uffff\u0001\u0003\u0001\uffff\u0001\u0004\u0003\uffff\u0001\u0005\u0001\u0006\u0001\u0007\u0001\uffff" + (((((("\u0001\u0003\u0001\u0019\u0001\u0003\u0001\uffff\u0002\u0003\u0001\u0019\u0001\b\u0001\uffff\u0001\u0003\u0001\u0017\u0001\t\u0004\uffff\u0001\u0019" + "\u0001\n\u0001\uffff\u0001\u000b\u0002\uffff\u0003\u0003\u0001\f\u0001\uffff\u0001\u0003\u0001\uffff\u0002\u0003\u0001\uffff\u0001") + "\u0003\u0003\uffff\u0001\r\u0001\uffff\u0002\u0003\u0001\uffff\u0001\u000e\u0002\uffff\u0002\u0019\u0001\u0003\u0001\uffff\u0003\u0003") + "\u0001\uffff\u0003\u0003\u0005\uffff\u0001\u0003\u0001\uffff\u0001\u0003\u0004\uffff\u0001\u0003\u0002\uffff\u0005\u0003\u0003\uffff") + "\u0001\u0003\u0001\uffff\u0002\u0003\u0002\uffff\u0001\u0003\u0001\u000f\u0004\u0003\u0001\u0010\u0001\u0018\u0001\u0011\u0001\u0016\u0001\u0012\u0002\uffff") + "\u0001\u0003\u0001\uffff\u0001\u0019\u0002\u0003\u0004\uffff\u0002\u0003\u0001\uffff\u0001\u0013\u0001\u0003\u0001\u0014\u0001\u0015\u0003\uffff") + "\u0001\u0019\b\uffff\u0001\u001a\u0001\u0002"), "\u0001\u001b\u0001\uffff\u0001\u001c", "\u0001\u001b\u0001\uffff\u0001\u001c", "\u0001\u001b\u0001\uffff\u0001\u001c", "\u0001\u001b\u0001\uffff\u0001\u001c", "\u0001\u001b\u0001\uffff\u0001\u001c", "\u0001\u001b\u0001\uffff\u0001\u001c", "\u0001\u001b\u0001\uffff\u0001\u001c", "\u0001\u001b\u0001\uffff\u0001\u001c", "\u0001\u001b\u0001\uffff\u0001\u001c", "\u0001\u001b\u0001\uffff\u0001\u001c", "\u0001\u001b\u0001\uffff\u0001\u001c", "\u0001\u001b\u0001\uffff\u0001\u001c", "\u0001\u001b\u0001\uffff\u0001\u001c", "\u0001\u001b\u0001\uffff\u0001\u001c", "\u0001\u001b\u0001\uffff\u0001\u001c", "\u0001\u001b\u0001\uffff\u0001\u001c", "\u0001\u001b\u0001\uffff\u0001\u001c", "\u0001\u001b\u0001\uffff\u0001\u001c", "\u0001\u001b\u0001\uffff\u0001\u001c", "\u0001\u001b\u0001\uffff\u0001\u001c", "\u0001\u001b\u0001\uffff\u0001\u001c", "\u0001\u001b\u0001\uffff\u0001\u001c", "\u0001\u001b\u0001\uffff\u0001\u001c", "\u0001\u001b\u0001\uffff\u0001\u001c", "\u0001\u001b\u0001\uffff\u0001\u001c", "\u0001\u001b\u0001\uffff\u0001\u001c", "", "" };

	static final short[] DFA154_eot = DFA.unpackEncodedString(Cql_Parser.DFA154_eotS);

	static final short[] DFA154_eof = DFA.unpackEncodedString(Cql_Parser.DFA154_eofS);

	static final char[] DFA154_min = DFA.unpackEncodedStringToUnsignedChars(Cql_Parser.DFA154_minS);

	static final char[] DFA154_max = DFA.unpackEncodedStringToUnsignedChars(Cql_Parser.DFA154_maxS);

	static final short[] DFA154_accept = DFA.unpackEncodedString(Cql_Parser.DFA154_acceptS);

	static final short[] DFA154_special = DFA.unpackEncodedString(Cql_Parser.DFA154_specialS);

	static final short[][] DFA154_transition;

	static {
		int numStates = Cql_Parser.DFA154_transitionS.length;
		DFA154_transition = new short[numStates][];
		for (int i = 0; i < numStates; i++) {
			Cql_Parser.DFA154_transition[i] = DFA.unpackEncodedString(Cql_Parser.DFA154_transitionS[i]);
		}
	}

	protected class DFA154 extends DFA {
		public DFA154(BaseRecognizer recognizer) {
			this.recognizer = recognizer;
			this.decisionNumber = 154;
			this.eot = Cql_Parser.DFA154_eot;
			this.eof = Cql_Parser.DFA154_eof;
			this.min = Cql_Parser.DFA154_min;
			this.max = Cql_Parser.DFA154_max;
			this.accept = Cql_Parser.DFA154_accept;
			this.special = Cql_Parser.DFA154_special;
			this.transition = Cql_Parser.DFA154_transition;
		}

		@Override
		public String getDescription() {
			return "1221:7: ( ksName[name] '.' )?";
		}
	}

	static final String DFA155_eotS = "\u001d\uffff";

	static final String DFA155_eofS = "\u0001\uffff\u001a\u001c\u0002\uffff";

	static final String DFA155_minS = "\u0001\u0017\u001a\u001b\u0002\uffff";

	static final String DFA155_maxS = "\u0001\u00aa\u001a\u00c1\u0002\uffff";

	static final String DFA155_acceptS = "\u001b\uffff\u0001\u0001\u0001\u0002";

	static final String DFA155_specialS = "\u001d\uffff}>";

	static final String[] DFA155_transitionS = new String[]{ "\u0001\u0001\u0004\uffff\u0002\u0003\u0004\uffff\u0001\u0003\u0001\uffff\u0001\u0004\u0003\uffff\u0001\u0005\u0001\u0006\u0001\u0007\u0001\uffff" + (((((("\u0001\u0003\u0001\u0019\u0001\u0003\u0001\uffff\u0002\u0003\u0001\u0019\u0001\b\u0001\uffff\u0001\u0003\u0001\u0017\u0001\t\u0004\uffff\u0001\u0019" + "\u0001\n\u0001\uffff\u0001\u000b\u0002\uffff\u0003\u0003\u0001\f\u0001\uffff\u0001\u0003\u0001\uffff\u0002\u0003\u0001\uffff\u0001") + "\u0003\u0003\uffff\u0001\r\u0001\uffff\u0002\u0003\u0001\uffff\u0001\u000e\u0002\uffff\u0002\u0019\u0001\u0003\u0001\uffff\u0003\u0003") + "\u0001\uffff\u0003\u0003\u0005\uffff\u0001\u0003\u0001\uffff\u0001\u0003\u0004\uffff\u0001\u0003\u0002\uffff\u0005\u0003\u0003\uffff") + "\u0001\u0003\u0001\uffff\u0002\u0003\u0002\uffff\u0001\u0003\u0001\u000f\u0004\u0003\u0001\u0010\u0001\u0018\u0001\u0011\u0001\u0016\u0001\u0012\u0002\uffff") + "\u0001\u0003\u0001\uffff\u0001\u0019\u0002\u0003\u0004\uffff\u0002\u0003\u0001\uffff\u0001\u0013\u0001\u0003\u0001\u0014\u0001\u0015\u0003\uffff") + "\u0001\u0019\b\uffff\u0001\u001a\u0001\u0002"), "\u0001\u001c\u0002\uffff\u0002\u001c\u0002\uffff\u0001\u001c\u001b\uffff\u0001\u001c\u0007\uffff\u0001\u001c\u0005\uffff\u0001" + (("\u001c\u000b\uffff\u0001\u001c\u0006\uffff\u0001\u001c\t\uffff\u0001\u001c\u0003\uffff\u0001\u001c\u0003\uffff\u0001\u001c" + "\u0002\uffff\u0001\u001c\u0002\uffff\u0002\u001c\u0006\uffff\u0001\u001c\u000b\uffff\u0001\u001c\f\uffff\u0001\u001c\u0005") + "\uffff\u0002\u001c\u0018\uffff\u0001\u001c\u0006\uffff\u0001\u001b\u0001\uffff\u0001\u001c"), "\u0001\u001c\u0002\uffff\u0002\u001c\u0002\uffff\u0001\u001c\u001b\uffff\u0001\u001c\u0007\uffff\u0001\u001c\u0005\uffff\u0001" + (("\u001c\u000b\uffff\u0001\u001c\u0006\uffff\u0001\u001c\t\uffff\u0001\u001c\u0003\uffff\u0001\u001c\u0003\uffff\u0001\u001c" + "\u0002\uffff\u0001\u001c\u0002\uffff\u0002\u001c\u0006\uffff\u0001\u001c\u000b\uffff\u0001\u001c\f\uffff\u0001\u001c\u0005") + "\uffff\u0002\u001c\u0018\uffff\u0001\u001c\u0006\uffff\u0001\u001b\u0001\uffff\u0001\u001c"), "\u0001\u001c\u0002\uffff\u0002\u001c\u0002\uffff\u0001\u001c\u001b\uffff\u0001\u001c\u0007\uffff\u0001\u001c\u0005\uffff\u0001" + (("\u001c\u000b\uffff\u0001\u001c\u0006\uffff\u0001\u001c\t\uffff\u0001\u001c\u0003\uffff\u0001\u001c\u0003\uffff\u0001\u001c" + "\u0002\uffff\u0001\u001c\u0002\uffff\u0002\u001c\u0006\uffff\u0001\u001c\u000b\uffff\u0001\u001c\f\uffff\u0001\u001c\u0005") + "\uffff\u0002\u001c\u0018\uffff\u0001\u001c\u0006\uffff\u0001\u001b\u0001\uffff\u0001\u001c"), "\u0001\u001c\u0002\uffff\u0002\u001c\u0002\uffff\u0001\u001c\u001b\uffff\u0001\u001c\u0007\uffff\u0001\u001c\u0005\uffff\u0001" + (("\u001c\u000b\uffff\u0001\u001c\u0006\uffff\u0001\u001c\t\uffff\u0001\u001c\u0003\uffff\u0001\u001c\u0003\uffff\u0001\u001c" + "\u0002\uffff\u0001\u001c\u0002\uffff\u0002\u001c\u0006\uffff\u0001\u001c\u000b\uffff\u0001\u001c\f\uffff\u0001\u001c\u0005") + "\uffff\u0002\u001c\u0018\uffff\u0001\u001c\u0006\uffff\u0001\u001b\u0001\uffff\u0001\u001c"), "\u0001\u001c\u0002\uffff\u0002\u001c\u0002\uffff\u0001\u001c\u001b\uffff\u0001\u001c\u0007\uffff\u0001\u001c\u0005\uffff\u0001" + (("\u001c\u000b\uffff\u0001\u001c\u0006\uffff\u0001\u001c\t\uffff\u0001\u001c\u0003\uffff\u0001\u001c\u0003\uffff\u0001\u001c" + "\u0002\uffff\u0001\u001c\u0002\uffff\u0002\u001c\u0006\uffff\u0001\u001c\u000b\uffff\u0001\u001c\f\uffff\u0001\u001c\u0005") + "\uffff\u0002\u001c\u0018\uffff\u0001\u001c\u0006\uffff\u0001\u001b\u0001\uffff\u0001\u001c"), "\u0001\u001c\u0002\uffff\u0002\u001c\u0002\uffff\u0001\u001c\u001b\uffff\u0001\u001c\u0007\uffff\u0001\u001c\u0005\uffff\u0001" + (("\u001c\u000b\uffff\u0001\u001c\u0006\uffff\u0001\u001c\t\uffff\u0001\u001c\u0003\uffff\u0001\u001c\u0003\uffff\u0001\u001c" + "\u0002\uffff\u0001\u001c\u0002\uffff\u0002\u001c\u0006\uffff\u0001\u001c\u000b\uffff\u0001\u001c\f\uffff\u0001\u001c\u0005") + "\uffff\u0002\u001c\u0018\uffff\u0001\u001c\u0006\uffff\u0001\u001b\u0001\uffff\u0001\u001c"), "\u0001\u001c\u0002\uffff\u0002\u001c\u0002\uffff\u0001\u001c\u001b\uffff\u0001\u001c\u0007\uffff\u0001\u001c\u0005\uffff\u0001" + (("\u001c\u000b\uffff\u0001\u001c\u0006\uffff\u0001\u001c\t\uffff\u0001\u001c\u0003\uffff\u0001\u001c\u0003\uffff\u0001\u001c" + "\u0002\uffff\u0001\u001c\u0002\uffff\u0002\u001c\u0006\uffff\u0001\u001c\u000b\uffff\u0001\u001c\f\uffff\u0001\u001c\u0005") + "\uffff\u0002\u001c\u0018\uffff\u0001\u001c\u0006\uffff\u0001\u001b\u0001\uffff\u0001\u001c"), "\u0001\u001c\u0002\uffff\u0002\u001c\u0002\uffff\u0001\u001c\u001b\uffff\u0001\u001c\u0007\uffff\u0001\u001c\u0005\uffff\u0001" + (("\u001c\u000b\uffff\u0001\u001c\u0006\uffff\u0001\u001c\t\uffff\u0001\u001c\u0003\uffff\u0001\u001c\u0003\uffff\u0001\u001c" + "\u0002\uffff\u0001\u001c\u0002\uffff\u0002\u001c\u0006\uffff\u0001\u001c\u000b\uffff\u0001\u001c\f\uffff\u0001\u001c\u0005") + "\uffff\u0002\u001c\u0018\uffff\u0001\u001c\u0006\uffff\u0001\u001b\u0001\uffff\u0001\u001c"), "\u0001\u001c\u0002\uffff\u0002\u001c\u0002\uffff\u0001\u001c\u001b\uffff\u0001\u001c\u0007\uffff\u0001\u001c\u0005\uffff\u0001" + (("\u001c\u000b\uffff\u0001\u001c\u0006\uffff\u0001\u001c\t\uffff\u0001\u001c\u0003\uffff\u0001\u001c\u0003\uffff\u0001\u001c" + "\u0002\uffff\u0001\u001c\u0002\uffff\u0002\u001c\u0006\uffff\u0001\u001c\u000b\uffff\u0001\u001c\f\uffff\u0001\u001c\u0005") + "\uffff\u0002\u001c\u0018\uffff\u0001\u001c\u0006\uffff\u0001\u001b\u0001\uffff\u0001\u001c"), "\u0001\u001c\u0002\uffff\u0002\u001c\u0002\uffff\u0001\u001c\u001b\uffff\u0001\u001c\u0007\uffff\u0001\u001c\u0005\uffff\u0001" + (("\u001c\u000b\uffff\u0001\u001c\u0006\uffff\u0001\u001c\t\uffff\u0001\u001c\u0003\uffff\u0001\u001c\u0003\uffff\u0001\u001c" + "\u0002\uffff\u0001\u001c\u0002\uffff\u0002\u001c\u0006\uffff\u0001\u001c\u000b\uffff\u0001\u001c\f\uffff\u0001\u001c\u0005") + "\uffff\u0002\u001c\u0018\uffff\u0001\u001c\u0006\uffff\u0001\u001b\u0001\uffff\u0001\u001c"), "\u0001\u001c\u0002\uffff\u0002\u001c\u0002\uffff\u0001\u001c\u001b\uffff\u0001\u001c\u0007\uffff\u0001\u001c\u0005\uffff\u0001" + (("\u001c\u000b\uffff\u0001\u001c\u0006\uffff\u0001\u001c\t\uffff\u0001\u001c\u0003\uffff\u0001\u001c\u0003\uffff\u0001\u001c" + "\u0002\uffff\u0001\u001c\u0002\uffff\u0002\u001c\u0006\uffff\u0001\u001c\u000b\uffff\u0001\u001c\f\uffff\u0001\u001c\u0005") + "\uffff\u0002\u001c\u0018\uffff\u0001\u001c\u0006\uffff\u0001\u001b\u0001\uffff\u0001\u001c"), "\u0001\u001c\u0002\uffff\u0002\u001c\u0002\uffff\u0001\u001c\u001b\uffff\u0001\u001c\u0007\uffff\u0001\u001c\u0005\uffff\u0001" + (("\u001c\u000b\uffff\u0001\u001c\u0006\uffff\u0001\u001c\t\uffff\u0001\u001c\u0003\uffff\u0001\u001c\u0003\uffff\u0001\u001c" + "\u0002\uffff\u0001\u001c\u0002\uffff\u0002\u001c\u0006\uffff\u0001\u001c\u000b\uffff\u0001\u001c\f\uffff\u0001\u001c\u0005") + "\uffff\u0002\u001c\u0018\uffff\u0001\u001c\u0006\uffff\u0001\u001b\u0001\uffff\u0001\u001c"), "\u0001\u001c\u0002\uffff\u0002\u001c\u0002\uffff\u0001\u001c\u001b\uffff\u0001\u001c\u0007\uffff\u0001\u001c\u0005\uffff\u0001" + (("\u001c\u000b\uffff\u0001\u001c\u0006\uffff\u0001\u001c\t\uffff\u0001\u001c\u0003\uffff\u0001\u001c\u0003\uffff\u0001\u001c" + "\u0002\uffff\u0001\u001c\u0002\uffff\u0002\u001c\u0006\uffff\u0001\u001c\u000b\uffff\u0001\u001c\f\uffff\u0001\u001c\u0005") + "\uffff\u0002\u001c\u0018\uffff\u0001\u001c\u0006\uffff\u0001\u001b\u0001\uffff\u0001\u001c"), "\u0001\u001c\u0002\uffff\u0002\u001c\u0002\uffff\u0001\u001c\u001b\uffff\u0001\u001c\u0007\uffff\u0001\u001c\u0005\uffff\u0001" + (("\u001c\u000b\uffff\u0001\u001c\u0006\uffff\u0001\u001c\t\uffff\u0001\u001c\u0003\uffff\u0001\u001c\u0003\uffff\u0001\u001c" + "\u0002\uffff\u0001\u001c\u0002\uffff\u0002\u001c\u0006\uffff\u0001\u001c\u000b\uffff\u0001\u001c\f\uffff\u0001\u001c\u0005") + "\uffff\u0002\u001c\u0018\uffff\u0001\u001c\u0006\uffff\u0001\u001b\u0001\uffff\u0001\u001c"), "\u0001\u001c\u0002\uffff\u0002\u001c\u0002\uffff\u0001\u001c\u001b\uffff\u0001\u001c\u0007\uffff\u0001\u001c\u0005\uffff\u0001" + (("\u001c\u000b\uffff\u0001\u001c\u0006\uffff\u0001\u001c\t\uffff\u0001\u001c\u0003\uffff\u0001\u001c\u0003\uffff\u0001\u001c" + "\u0002\uffff\u0001\u001c\u0002\uffff\u0002\u001c\u0006\uffff\u0001\u001c\u000b\uffff\u0001\u001c\f\uffff\u0001\u001c\u0005") + "\uffff\u0002\u001c\u0018\uffff\u0001\u001c\u0006\uffff\u0001\u001b\u0001\uffff\u0001\u001c"), "\u0001\u001c\u0002\uffff\u0002\u001c\u0002\uffff\u0001\u001c\u001b\uffff\u0001\u001c\u0007\uffff\u0001\u001c\u0005\uffff\u0001" + (("\u001c\u000b\uffff\u0001\u001c\u0006\uffff\u0001\u001c\t\uffff\u0001\u001c\u0003\uffff\u0001\u001c\u0003\uffff\u0001\u001c" + "\u0002\uffff\u0001\u001c\u0002\uffff\u0002\u001c\u0006\uffff\u0001\u001c\u000b\uffff\u0001\u001c\f\uffff\u0001\u001c\u0005") + "\uffff\u0002\u001c\u0018\uffff\u0001\u001c\u0006\uffff\u0001\u001b\u0001\uffff\u0001\u001c"), "\u0001\u001c\u0002\uffff\u0002\u001c\u0002\uffff\u0001\u001c\u001b\uffff\u0001\u001c\u0007\uffff\u0001\u001c\u0005\uffff\u0001" + (("\u001c\u000b\uffff\u0001\u001c\u0006\uffff\u0001\u001c\t\uffff\u0001\u001c\u0003\uffff\u0001\u001c\u0003\uffff\u0001\u001c" + "\u0002\uffff\u0001\u001c\u0002\uffff\u0002\u001c\u0006\uffff\u0001\u001c\u000b\uffff\u0001\u001c\f\uffff\u0001\u001c\u0005") + "\uffff\u0002\u001c\u0018\uffff\u0001\u001c\u0006\uffff\u0001\u001b\u0001\uffff\u0001\u001c"), "\u0001\u001c\u0002\uffff\u0002\u001c\u0002\uffff\u0001\u001c\u001b\uffff\u0001\u001c\u0007\uffff\u0001\u001c\u0005\uffff\u0001" + (("\u001c\u000b\uffff\u0001\u001c\u0006\uffff\u0001\u001c\t\uffff\u0001\u001c\u0003\uffff\u0001\u001c\u0003\uffff\u0001\u001c" + "\u0002\uffff\u0001\u001c\u0002\uffff\u0002\u001c\u0006\uffff\u0001\u001c\u000b\uffff\u0001\u001c\f\uffff\u0001\u001c\u0005") + "\uffff\u0002\u001c\u0018\uffff\u0001\u001c\u0006\uffff\u0001\u001b\u0001\uffff\u0001\u001c"), "\u0001\u001c\u0002\uffff\u0002\u001c\u0002\uffff\u0001\u001c\u001b\uffff\u0001\u001c\u0007\uffff\u0001\u001c\u0005\uffff\u0001" + (("\u001c\u000b\uffff\u0001\u001c\u0006\uffff\u0001\u001c\t\uffff\u0001\u001c\u0003\uffff\u0001\u001c\u0003\uffff\u0001\u001c" + "\u0002\uffff\u0001\u001c\u0002\uffff\u0002\u001c\u0006\uffff\u0001\u001c\u000b\uffff\u0001\u001c\f\uffff\u0001\u001c\u0005") + "\uffff\u0002\u001c\u0018\uffff\u0001\u001c\u0006\uffff\u0001\u001b\u0001\uffff\u0001\u001c"), "\u0001\u001c\u0002\uffff\u0002\u001c\u0002\uffff\u0001\u001c\u001b\uffff\u0001\u001c\u0007\uffff\u0001\u001c\u0005\uffff\u0001" + (("\u001c\u000b\uffff\u0001\u001c\u0006\uffff\u0001\u001c\t\uffff\u0001\u001c\u0003\uffff\u0001\u001c\u0003\uffff\u0001\u001c" + "\u0002\uffff\u0001\u001c\u0002\uffff\u0002\u001c\u0006\uffff\u0001\u001c\u000b\uffff\u0001\u001c\f\uffff\u0001\u001c\u0005") + "\uffff\u0002\u001c\u0018\uffff\u0001\u001c\u0006\uffff\u0001\u001b\u0001\uffff\u0001\u001c"), "\u0001\u001c\u0002\uffff\u0002\u001c\u0002\uffff\u0001\u001c\u001b\uffff\u0001\u001c\u0007\uffff\u0001\u001c\u0005\uffff\u0001" + (("\u001c\u000b\uffff\u0001\u001c\u0006\uffff\u0001\u001c\t\uffff\u0001\u001c\u0003\uffff\u0001\u001c\u0003\uffff\u0001\u001c" + "\u0002\uffff\u0001\u001c\u0002\uffff\u0002\u001c\u0006\uffff\u0001\u001c\u000b\uffff\u0001\u001c\f\uffff\u0001\u001c\u0005") + "\uffff\u0002\u001c\u0018\uffff\u0001\u001c\u0006\uffff\u0001\u001b\u0001\uffff\u0001\u001c"), "\u0001\u001c\u0002\uffff\u0002\u001c\u0002\uffff\u0001\u001c\u001b\uffff\u0001\u001c\u0007\uffff\u0001\u001c\u0005\uffff\u0001" + (("\u001c\u000b\uffff\u0001\u001c\u0006\uffff\u0001\u001c\t\uffff\u0001\u001c\u0003\uffff\u0001\u001c\u0003\uffff\u0001\u001c" + "\u0002\uffff\u0001\u001c\u0002\uffff\u0002\u001c\u0006\uffff\u0001\u001c\u000b\uffff\u0001\u001c\f\uffff\u0001\u001c\u0005") + "\uffff\u0002\u001c\u0018\uffff\u0001\u001c\u0006\uffff\u0001\u001b\u0001\uffff\u0001\u001c"), "\u0001\u001c\u0002\uffff\u0002\u001c\u0002\uffff\u0001\u001c\u001b\uffff\u0001\u001c\u0007\uffff\u0001\u001c\u0005\uffff\u0001" + (("\u001c\u000b\uffff\u0001\u001c\u0006\uffff\u0001\u001c\t\uffff\u0001\u001c\u0003\uffff\u0001\u001c\u0003\uffff\u0001\u001c" + "\u0002\uffff\u0001\u001c\u0002\uffff\u0002\u001c\u0006\uffff\u0001\u001c\u000b\uffff\u0001\u001c\f\uffff\u0001\u001c\u0005") + "\uffff\u0002\u001c\u0018\uffff\u0001\u001c\u0006\uffff\u0001\u001b\u0001\uffff\u0001\u001c"), "\u0001\u001c\u0002\uffff\u0002\u001c\u0002\uffff\u0001\u001c\u001b\uffff\u0001\u001c\u0007\uffff\u0001\u001c\u0005\uffff\u0001" + (("\u001c\u000b\uffff\u0001\u001c\u0006\uffff\u0001\u001c\t\uffff\u0001\u001c\u0003\uffff\u0001\u001c\u0003\uffff\u0001\u001c" + "\u0002\uffff\u0001\u001c\u0002\uffff\u0002\u001c\u0006\uffff\u0001\u001c\u000b\uffff\u0001\u001c\f\uffff\u0001\u001c\u0005") + "\uffff\u0002\u001c\u0018\uffff\u0001\u001c\u0006\uffff\u0001\u001b\u0001\uffff\u0001\u001c"), "\u0001\u001c\u0002\uffff\u0002\u001c\u0002\uffff\u0001\u001c\u001b\uffff\u0001\u001c\u0007\uffff\u0001\u001c\u0005\uffff\u0001" + (("\u001c\u000b\uffff\u0001\u001c\u0006\uffff\u0001\u001c\t\uffff\u0001\u001c\u0003\uffff\u0001\u001c\u0003\uffff\u0001\u001c" + "\u0002\uffff\u0001\u001c\u0002\uffff\u0002\u001c\u0006\uffff\u0001\u001c\u000b\uffff\u0001\u001c\f\uffff\u0001\u001c\u0005") + "\uffff\u0002\u001c\u0018\uffff\u0001\u001c\u0006\uffff\u0001\u001b\u0001\uffff\u0001\u001c"), "\u0001\u001c\u0002\uffff\u0002\u001c\u0002\uffff\u0001\u001c\u001b\uffff\u0001\u001c\u0007\uffff\u0001\u001c\u0005\uffff\u0001" + (("\u001c\u000b\uffff\u0001\u001c\u0006\uffff\u0001\u001c\t\uffff\u0001\u001c\u0003\uffff\u0001\u001c\u0003\uffff\u0001\u001c" + "\u0002\uffff\u0001\u001c\u0002\uffff\u0002\u001c\u0006\uffff\u0001\u001c\u000b\uffff\u0001\u001c\f\uffff\u0001\u001c\u0005") + "\uffff\u0002\u001c\u0018\uffff\u0001\u001c\u0006\uffff\u0001\u001b\u0001\uffff\u0001\u001c"), "", "" };

	static final short[] DFA155_eot = DFA.unpackEncodedString(Cql_Parser.DFA155_eotS);

	static final short[] DFA155_eof = DFA.unpackEncodedString(Cql_Parser.DFA155_eofS);

	static final char[] DFA155_min = DFA.unpackEncodedStringToUnsignedChars(Cql_Parser.DFA155_minS);

	static final char[] DFA155_max = DFA.unpackEncodedStringToUnsignedChars(Cql_Parser.DFA155_maxS);

	static final short[] DFA155_accept = DFA.unpackEncodedString(Cql_Parser.DFA155_acceptS);

	static final short[] DFA155_special = DFA.unpackEncodedString(Cql_Parser.DFA155_specialS);

	static final short[][] DFA155_transition;

	static {
		int numStates = Cql_Parser.DFA155_transitionS.length;
		DFA155_transition = new short[numStates][];
		for (int i = 0; i < numStates; i++) {
			Cql_Parser.DFA155_transition[i] = DFA.unpackEncodedString(Cql_Parser.DFA155_transitionS[i]);
		}
	}

	protected class DFA155 extends DFA {
		public DFA155(BaseRecognizer recognizer) {
			this.recognizer = recognizer;
			this.decisionNumber = 155;
			this.eot = Cql_Parser.DFA155_eot;
			this.eof = Cql_Parser.DFA155_eof;
			this.min = Cql_Parser.DFA155_min;
			this.max = Cql_Parser.DFA155_max;
			this.accept = Cql_Parser.DFA155_accept;
			this.special = Cql_Parser.DFA155_special;
			this.transition = Cql_Parser.DFA155_transition;
		}

		@Override
		public String getDescription() {
			return "1226:7: ( ksName[name] '.' )?";
		}
	}

	static final String DFA173_eotS = "#\uffff";

	static final String DFA173_eofS = "#\uffff";

	static final String DFA173_minS = "\u0001\u0006\u0002\uffff\u0001\u0006\u0004\uffff\u0019\u00b8\u0001\u00bf\u0001\uffff";

	static final String DFA173_maxS = "\u0001\u00cb\u0002\uffff\u0001\u00cc\u0004\uffff\u001a\u00c0\u0001\uffff";

	static final String DFA173_acceptS = "\u0001\uffff\u0001\u0001\u0001\u0002\u0001\uffff\u0001\u0004\u0001\u0005\u0001\u0006\u0001\u0007\u001a\uffff\u0001\u0003";

	static final String DFA173_specialS = "#\uffff}>";

	static final String[] DFA173_transitionS = new String[]{ "\u0001\u0001\u0004\uffff\u0001\u0001\u0005\uffff\u0001\u0001\u0003\uffff\u0001\u0001\u0002\uffff\u0001\u00018\uffff\u0001\u0001\u0015" + ("\uffff\u0001\u0001\u0004\uffff\u0001\u0005<\uffff\u0001\u0007\u0003\uffff\u0001\u0001\u0002\uffff\u0001\u0001\u0007\uffff\u0001" + "\u0004\u0004\uffff\u0001\u0001\u0002\uffff\u0001\u0006\u0006\uffff\u0001\u0002\u0003\uffff\u0001\u0003"), "", "", "\u0001\u0002\u0004\uffff\u0001\u0002\u0005\uffff\u0001\u0002\u0003\uffff\u0001\u0002\u0001\uffff\u0001\b\u0001\u0002\u0003\uffff\u0002" + (((((((("\n\u0004\uffff\u0001\n\u0001\uffff\u0001\u000b\u0003\uffff\u0001\f\u0001\r\u0001\u000e\u0001\uffff\u0001\n\u0001!" + "\u0001\n\u0001\uffff\u0002\n\u0001 \u0001\u000f\u0001\uffff\u0001\n\u0001\u001e\u0001\u0010\u0004\uffff\u0001!\u0001\u0011") + "\u0001\uffff\u0001\u0012\u0002\uffff\u0003\n\u0001\u0013\u0001\uffff\u0001\n\u0001\uffff\u0002\n\u0001\uffff\u0001\n") + "\u0003\uffff\u0001\u0014\u0001\u0002\u0002\n\u0001\uffff\u0001\u0015\u0002\uffff\u0002!\u0001\n\u0001\uffff\u0003\n\u0001") + "\uffff\u0003\n\u0004\uffff\u0001\u0002\u0001\n\u0001\uffff\u0001\n\u0001\uffff\u0001\u0002\u0002\uffff\u0001\n\u0002") + "\uffff\u0005\n\u0003\uffff\u0001\n\u0001\uffff\u0002\n\u0002\uffff\u0001\n\u0001\u0016\u0004\n\u0001\u0017\u0001\u001f") + "\u0001\u0018\u0001\u001d\u0001\u0019\u0001\uffff\u0001\u0002\u0001\n\u0001\uffff\u0001!\u0002\n\u0004\uffff\u0002\n\u0001\uffff") + "\u0001\u001a\u0001\n\u0001\u001b\u0001\u001c\u0003\uffff\u0001!\b\uffff\u0001\u0002\u0001\t\u0002\uffff\u0001\u0002\u0002\uffff") + "\u0001\u0002\u0007\uffff\u0001\u0002\u0004\uffff\u0001\u0002\u0002\uffff\u0001\u0002\u0006\uffff\u0001\u0002\u0003\uffff\u0002\u0002"), "", "", "", "", "\u0001\u0002\u0006\uffff\u0001\u0002\u0001\"", "\u0001\u0002\u0006\uffff\u0001\u0002\u0001\"", "\u0001\u0002\u0006\uffff\u0001\u0002\u0001\"", "\u0001\u0002\u0006\uffff\u0001\u0002\u0001\"", "\u0001\u0002\u0006\uffff\u0001\u0002\u0001\"", "\u0001\u0002\u0006\uffff\u0001\u0002\u0001\"", "\u0001\u0002\u0006\uffff\u0001\u0002\u0001\"", "\u0001\u0002\u0006\uffff\u0001\u0002\u0001\"", "\u0001\u0002\u0006\uffff\u0001\u0002\u0001\"", "\u0001\u0002\u0006\uffff\u0001\u0002\u0001\"", "\u0001\u0002\u0006\uffff\u0001\u0002\u0001\"", "\u0001\u0002\u0006\uffff\u0001\u0002\u0001\"", "\u0001\u0002\u0006\uffff\u0001\u0002\u0001\"", "\u0001\u0002\u0006\uffff\u0001\u0002\u0001\"", "\u0001\u0002\u0006\uffff\u0001\u0002\u0001\"", "\u0001\u0002\u0006\uffff\u0001\u0002\u0001\"", "\u0001\u0002\u0006\uffff\u0001\u0002\u0001\"", "\u0001\u0002\u0006\uffff\u0001\u0002\u0001\"", "\u0001\u0002\u0006\uffff\u0001\u0002\u0001\"", "\u0001\u0002\u0006\uffff\u0001\u0002\u0001\"", "\u0001\u0002\u0006\uffff\u0001\u0002\u0001\"", "\u0001\u0002\u0006\uffff\u0001\u0002\u0001\"", "\u0001\u0002\u0006\uffff\u0001\u0002\u0001\"", "\u0001\u0002\u0006\uffff\u0001\u0002\u0001\"", "\u0001\u0002\u0006\uffff\u0001\u0002\u0001\"", "\u0001\u0002\u0001\"", "" };

	static final short[] DFA173_eot = DFA.unpackEncodedString(Cql_Parser.DFA173_eotS);

	static final short[] DFA173_eof = DFA.unpackEncodedString(Cql_Parser.DFA173_eofS);

	static final char[] DFA173_min = DFA.unpackEncodedStringToUnsignedChars(Cql_Parser.DFA173_minS);

	static final char[] DFA173_max = DFA.unpackEncodedStringToUnsignedChars(Cql_Parser.DFA173_maxS);

	static final short[] DFA173_accept = DFA.unpackEncodedString(Cql_Parser.DFA173_acceptS);

	static final short[] DFA173_special = DFA.unpackEncodedString(Cql_Parser.DFA173_specialS);

	static final short[][] DFA173_transition;

	static {
		int numStates = Cql_Parser.DFA173_transitionS.length;
		DFA173_transition = new short[numStates][];
		for (int i = 0; i < numStates; i++) {
			Cql_Parser.DFA173_transition[i] = DFA.unpackEncodedString(Cql_Parser.DFA173_transitionS[i]);
		}
	}

	protected class DFA173 extends DFA {
		public DFA173(BaseRecognizer recognizer) {
			this.recognizer = recognizer;
			this.decisionNumber = 173;
			this.eot = Cql_Parser.DFA173_eot;
			this.eof = Cql_Parser.DFA173_eof;
			this.min = Cql_Parser.DFA173_min;
			this.max = Cql_Parser.DFA173_max;
			this.accept = Cql_Parser.DFA173_accept;
			this.special = Cql_Parser.DFA173_special;
			this.transition = Cql_Parser.DFA173_transition;
		}

		@Override
		public String getDescription() {
			return "1316:1: value returns [Term.Raw value] : (c= constant |l= collectionLiteral |u= usertypeLiteral |t= tupleLiteral | K_NULL | ':' id= noncol_ident | QMARK );";
		}
	}

	static final String DFA175_eotS = "\u001c\uffff";

	static final String DFA175_eofS = "\u0001\uffff\u0019\u001b\u0002\uffff";

	static final String DFA175_minS = "\u0001\u0017\u0019\u00b8\u0002\uffff";

	static final String DFA175_maxS = "\u0001\u00aa\u0019\u00c1\u0002\uffff";

	static final String DFA175_acceptS = "\u001a\uffff\u0001\u0001\u0001\u0002";

	static final String DFA175_specialS = "\u001c\uffff}>";

	static final String[] DFA175_transitionS = new String[]{ "\u0001\u0001\u0004\uffff\u0002\u0003\u0004\uffff\u0001\u0003\u0001\uffff\u0001\u0004\u0003\uffff\u0001\u0005\u0001\u0006\u0001\u0007\u0001\uffff" + (((((("\u0001\u0003\u0001\u001a\u0001\u0003\u0001\uffff\u0002\u0003\u0001\u0019\u0001\b\u0001\uffff\u0001\u0003\u0001\u0017\u0001\t\u0004\uffff\u0001\u001a" + "\u0001\n\u0001\uffff\u0001\u000b\u0002\uffff\u0003\u0003\u0001\f\u0001\uffff\u0001\u0003\u0001\uffff\u0002\u0003\u0001\uffff\u0001") + "\u0003\u0003\uffff\u0001\r\u0001\uffff\u0002\u0003\u0001\uffff\u0001\u000e\u0002\uffff\u0002\u001a\u0001\u0003\u0001\uffff\u0003\u0003") + "\u0001\uffff\u0003\u0003\u0005\uffff\u0001\u0003\u0001\uffff\u0001\u0003\u0004\uffff\u0001\u0003\u0002\uffff\u0005\u0003\u0003\uffff") + "\u0001\u0003\u0001\uffff\u0002\u0003\u0002\uffff\u0001\u0003\u0001\u000f\u0004\u0003\u0001\u0010\u0001\u0018\u0001\u0011\u0001\u0016\u0001\u0012\u0001\uffff") + "\u0001\u001b\u0001\u0003\u0001\uffff\u0001\u001a\u0002\u0003\u0004\uffff\u0002\u0003\u0001\uffff\u0001\u0013\u0001\u0003\u0001\u0014\u0001\u0015\u0003\uffff") + "\u0001\u001a\b\uffff\u0001\u001a\u0001\u0002"), "\u0001\u001b\u0006\uffff\u0001\u001a\u0001\uffff\u0001\u001b", "\u0001\u001b\u0006\uffff\u0001\u001a\u0001\uffff\u0001\u001b", "\u0001\u001b\u0006\uffff\u0001\u001a\u0001\uffff\u0001\u001b", "\u0001\u001b\u0006\uffff\u0001\u001a\u0001\uffff\u0001\u001b", "\u0001\u001b\u0006\uffff\u0001\u001a\u0001\uffff\u0001\u001b", "\u0001\u001b\u0006\uffff\u0001\u001a\u0001\uffff\u0001\u001b", "\u0001\u001b\u0006\uffff\u0001\u001a\u0001\uffff\u0001\u001b", "\u0001\u001b\u0006\uffff\u0001\u001a\u0001\uffff\u0001\u001b", "\u0001\u001b\u0006\uffff\u0001\u001a\u0001\uffff\u0001\u001b", "\u0001\u001b\u0006\uffff\u0001\u001a\u0001\uffff\u0001\u001b", "\u0001\u001b\u0006\uffff\u0001\u001a\u0001\uffff\u0001\u001b", "\u0001\u001b\u0006\uffff\u0001\u001a\u0001\uffff\u0001\u001b", "\u0001\u001b\u0006\uffff\u0001\u001a\u0001\uffff\u0001\u001b", "\u0001\u001b\u0006\uffff\u0001\u001a\u0001\uffff\u0001\u001b", "\u0001\u001b\u0006\uffff\u0001\u001a\u0001\uffff\u0001\u001b", "\u0001\u001b\u0006\uffff\u0001\u001a\u0001\uffff\u0001\u001b", "\u0001\u001b\u0006\uffff\u0001\u001a\u0001\uffff\u0001\u001b", "\u0001\u001b\u0006\uffff\u0001\u001a\u0001\uffff\u0001\u001b", "\u0001\u001b\u0006\uffff\u0001\u001a\u0001\uffff\u0001\u001b", "\u0001\u001b\u0006\uffff\u0001\u001a\u0001\uffff\u0001\u001b", "\u0001\u001b\u0006\uffff\u0001\u001a\u0001\uffff\u0001\u001b", "\u0001\u001b\u0006\uffff\u0001\u001a\u0001\uffff\u0001\u001b", "\u0001\u001b\u0006\uffff\u0001\u001a\u0001\uffff\u0001\u001b", "\u0001\u001b\u0006\uffff\u0001\u001a\u0001\uffff\u0001\u001b", "\u0001\u001b\u0006\uffff\u0001\u001a\u0001\uffff\u0001\u001b", "", "" };

	static final short[] DFA175_eot = DFA.unpackEncodedString(Cql_Parser.DFA175_eotS);

	static final short[] DFA175_eof = DFA.unpackEncodedString(Cql_Parser.DFA175_eofS);

	static final char[] DFA175_min = DFA.unpackEncodedStringToUnsignedChars(Cql_Parser.DFA175_minS);

	static final char[] DFA175_max = DFA.unpackEncodedStringToUnsignedChars(Cql_Parser.DFA175_maxS);

	static final short[] DFA175_accept = DFA.unpackEncodedString(Cql_Parser.DFA175_acceptS);

	static final short[] DFA175_special = DFA.unpackEncodedString(Cql_Parser.DFA175_specialS);

	static final short[][] DFA175_transition;

	static {
		int numStates = Cql_Parser.DFA175_transitionS.length;
		DFA175_transition = new short[numStates][];
		for (int i = 0; i < numStates; i++) {
			Cql_Parser.DFA175_transition[i] = DFA.unpackEncodedString(Cql_Parser.DFA175_transitionS[i]);
		}
	}

	protected class DFA175 extends DFA {
		public DFA175(BaseRecognizer recognizer) {
			this.recognizer = recognizer;
			this.decisionNumber = 175;
			this.eot = Cql_Parser.DFA175_eot;
			this.eof = Cql_Parser.DFA175_eof;
			this.min = Cql_Parser.DFA175_min;
			this.max = Cql_Parser.DFA175_max;
			this.accept = Cql_Parser.DFA175_accept;
			this.special = Cql_Parser.DFA175_special;
			this.transition = Cql_Parser.DFA175_transition;
		}

		@Override
		public String getDescription() {
			return "1335:7: (ks= keyspaceName '.' )?";
		}
	}

	static final String DFA177_eotS = ":\uffff";

	static final String DFA177_eofS = ":\uffff";

	static final String DFA177_minS = "\u0001\u0017\u0019\u00b8\u0001\u00bf\u0001\u00b8\u0001\u00bf\u0001\u0017\u0001\u0006\u0019\u00b8\u0002\uffff";

	static final String DFA177_maxS = "\u0001\u00aa\u001a\u00bf\u0001\u00b8\u0001\u00bf\u0001\u00aa\u0001\u00cb\u0019\u00b8\u0002\uffff";

	static final String DFA177_acceptS = "8\uffff\u0001\u0001\u0001\u0002";

	static final String DFA177_specialS = ":\uffff}>";

	static final String[] DFA177_transitionS = new String[]{ "\u0001\u0001\u0004\uffff\u0002\u0003\u0004\uffff\u0001\u0003\u0001\uffff\u0001\u0004\u0003\uffff\u0001\u0005\u0001\u0006\u0001\u0007\u0001\uffff" + (((((("\u0001\u0003\u0001\u001c\u0001\u0003\u0001\uffff\u0002\u0003\u0001\u0019\u0001\b\u0001\uffff\u0001\u0003\u0001\u0017\u0001\t\u0004\uffff\u0001\u001c" + "\u0001\n\u0001\uffff\u0001\u000b\u0002\uffff\u0003\u0003\u0001\f\u0001\uffff\u0001\u0003\u0001\uffff\u0002\u0003\u0001\uffff\u0001") + "\u0003\u0003\uffff\u0001\r\u0001\uffff\u0002\u0003\u0001\uffff\u0001\u000e\u0002\uffff\u0002\u001c\u0001\u0003\u0001\uffff\u0003\u0003") + "\u0001\uffff\u0003\u0003\u0005\uffff\u0001\u0003\u0001\uffff\u0001\u0003\u0004\uffff\u0001\u0003\u0002\uffff\u0005\u0003\u0003\uffff") + "\u0001\u0003\u0001\uffff\u0002\u0003\u0002\uffff\u0001\u0003\u0001\u000f\u0004\u0003\u0001\u0010\u0001\u0018\u0001\u0011\u0001\u0016\u0001\u0012\u0001\uffff") + "\u0001\u001b\u0001\u0003\u0001\uffff\u0001\u001c\u0002\u0003\u0004\uffff\u0002\u0003\u0001\uffff\u0001\u0013\u0001\u0003\u0001\u0014\u0001\u0015\u0003\uffff") + "\u0001\u001c\b\uffff\u0001\u001a\u0001\u0002"), "\u0001\u001e\u0006\uffff\u0001\u001d", "\u0001\u001e\u0006\uffff\u0001\u001d", "\u0001\u001e\u0006\uffff\u0001\u001d", "\u0001\u001e\u0006\uffff\u0001\u001d", "\u0001\u001e\u0006\uffff\u0001\u001d", "\u0001\u001e\u0006\uffff\u0001\u001d", "\u0001\u001e\u0006\uffff\u0001\u001d", "\u0001\u001e\u0006\uffff\u0001\u001d", "\u0001\u001e\u0006\uffff\u0001\u001d", "\u0001\u001e\u0006\uffff\u0001\u001d", "\u0001\u001e\u0006\uffff\u0001\u001d", "\u0001\u001e\u0006\uffff\u0001\u001d", "\u0001\u001e\u0006\uffff\u0001\u001d", "\u0001\u001e\u0006\uffff\u0001\u001d", "\u0001\u001e\u0006\uffff\u0001\u001d", "\u0001\u001e\u0006\uffff\u0001\u001d", "\u0001\u001e\u0006\uffff\u0001\u001d", "\u0001\u001e\u0006\uffff\u0001\u001d", "\u0001\u001e\u0006\uffff\u0001\u001d", "\u0001\u001e\u0006\uffff\u0001\u001d", "\u0001\u001e\u0006\uffff\u0001\u001d", "\u0001\u001e\u0006\uffff\u0001\u001d", "\u0001\u001e\u0006\uffff\u0001\u001d", "\u0001\u001e\u0006\uffff\u0001\u001d", "\u0001\u001e\u0006\uffff\u0001\u001d", "\u0001\u001d", "\u0001\u001e", "\u0001\u001d", "\u0001\u001f\u0004\uffff\u0002!\u0004\uffff\u0001!\u0001\uffff\u0001\"\u0003\uffff\u0001#\u0001$\u0001%" + (((((("\u0001\uffff\u0001!\u0001\uffff\u0001!\u0001\uffff\u0002!\u00017\u0001&\u0001\uffff\u0001!\u00015\u0001" + "\'\u0005\uffff\u0001(\u0001\uffff\u0001)\u0002\uffff\u0003!\u0001*\u0001\uffff\u0001!\u0001\uffff") + "\u0002!\u0001\uffff\u0001!\u0003\uffff\u0001+\u0001\uffff\u0002!\u0001\uffff\u0001,\u0004\uffff\u0001!") + "\u0001\uffff\u0003!\u0001\uffff\u0003!\u0005\uffff\u0001!\u0001\uffff\u0001!\u0004\uffff\u0001!\u0002\uffff") + "\u0005!\u0003\uffff\u0001!\u0001\uffff\u0002!\u0002\uffff\u0001!\u0001-\u0004!\u0001.\u00016\u0001/") + "\u00014\u00010\u0001\uffff\u0001\u001b\u0001!\u0002\uffff\u0002!\u0004\uffff\u0002!\u0001\uffff\u00011\u0001") + "!\u00012\u00013\r\uffff\u0001 "), "\u00019\u0004\uffff\u00019\u0005\uffff\u00019\u0003\uffff\u00019\u0001\uffff\u00029\u0003\uffff\u0002" + ((((((("9\u0004\uffff\u00019\u0001\uffff\u00019\u0003\uffff\u00039\u0001\uffff\u00039\u0001\uffff\u00049" + "\u0001\uffff\u00039\u0004\uffff\u00029\u0001\uffff\u00019\u0002\uffff\u00049\u0001\uffff\u00019\u0001\uffff") + "\u00029\u0001\uffff\u00019\u0003\uffff\u00049\u0001\uffff\u00019\u0002\uffff\u00039\u0001\uffff\u00039") + "\u0001\uffff\u00039\u0004\uffff\u00029\u0001\uffff\u00019\u0001\uffff\u00019\u0002\uffff\u00019\u0002\uffff") + "\u00059\u0003\uffff\u00019\u0001\uffff\u00029\u0002\uffff\u000b9\u0001\uffff\u00029\u0001\uffff\u0003") + "9\u0004\uffff\u00029\u0001\uffff\u00049\u0003\uffff\u00019\b\uffff\u00029\u0002\uffff\u00019") + "\u0002\uffff\u00019\u0007\uffff\u00019\u00018\u0003\uffff\u00019\u0002\uffff\u00019\u0006\uffff\u00019") + "\u0003\uffff\u00019"), "\u0001\u001e", "\u0001\u001e", "\u0001\u001e", "\u0001\u001e", "\u0001\u001e", "\u0001\u001e", "\u0001\u001e", "\u0001\u001e", "\u0001\u001e", "\u0001\u001e", "\u0001\u001e", "\u0001\u001e", "\u0001\u001e", "\u0001\u001e", "\u0001\u001e", "\u0001\u001e", "\u0001\u001e", "\u0001\u001e", "\u0001\u001e", "\u0001\u001e", "\u0001\u001e", "\u0001\u001e", "\u0001\u001e", "\u0001\u001e", "\u0001\u001e", "", "" };

	static final short[] DFA177_eot = DFA.unpackEncodedString(Cql_Parser.DFA177_eotS);

	static final short[] DFA177_eof = DFA.unpackEncodedString(Cql_Parser.DFA177_eofS);

	static final char[] DFA177_min = DFA.unpackEncodedStringToUnsignedChars(Cql_Parser.DFA177_minS);

	static final char[] DFA177_max = DFA.unpackEncodedStringToUnsignedChars(Cql_Parser.DFA177_maxS);

	static final short[] DFA177_accept = DFA.unpackEncodedString(Cql_Parser.DFA177_acceptS);

	static final short[] DFA177_special = DFA.unpackEncodedString(Cql_Parser.DFA177_specialS);

	static final short[][] DFA177_transition;

	static {
		int numStates = Cql_Parser.DFA177_transitionS.length;
		DFA177_transition = new short[numStates][];
		for (int i = 0; i < numStates; i++) {
			Cql_Parser.DFA177_transition[i] = DFA.unpackEncodedString(Cql_Parser.DFA177_transitionS[i]);
		}
	}

	protected class DFA177 extends DFA {
		public DFA177(BaseRecognizer recognizer) {
			this.recognizer = recognizer;
			this.decisionNumber = 177;
			this.eot = Cql_Parser.DFA177_eot;
			this.eof = Cql_Parser.DFA177_eof;
			this.min = Cql_Parser.DFA177_min;
			this.max = Cql_Parser.DFA177_max;
			this.accept = Cql_Parser.DFA177_accept;
			this.special = Cql_Parser.DFA177_special;
			this.transition = Cql_Parser.DFA177_transition;
		}

		@Override
		public String getDescription() {
			return "1346:1: function returns [Term.Raw t] : (f= functionName '(' ')' |f= functionName '(' args= functionArgs ')' );";
		}
	}

	static final String DFA179_eotS = "H\uffff";

	static final String DFA179_eofS = "\u0003\uffff\u0001\u0001\"\uffff\u0001\u0001\u0007\uffff\u001a\"";

	static final String DFA179_minS = "\u0001\u0006\u0001\uffff\u0001\u0006\u0001\u001e\u0001\uffff\u0001\u00b9\u0019\u00b8\u0001\u00b9\u0002\u00b8\u0001\uffff" + "\u0001\u00b8\u0001\u00bf\u0001\u00b8\u0001\u0006\u0001\u0017\u0001\u0006\u0001+\u0001r\u0003\u00b8\u001a\u001e";

	static final String DFA179_maxS = "\u0001\u00cb\u0001\uffff\u0001\u00cb\u0001\u00cc\u0001\uffff\u0001\u00bc\u0002\u00bf\u0001\u00c2\u0017\u00bf" + ("\u0002\u00c2\u0001\uffff\u0001\u00c2\u0002\u00bf\u0001\u00cc\u0001\u00aa\u0001\u00cb\u0002\u00bf\u0003\u00b9" + "\u001a\u00cc");

	static final String DFA179_acceptS = "\u0001\uffff\u0001\u0001\u0002\uffff\u0001\u0002\u001d\uffff\u0001\u0003%\uffff";

	static final String DFA179_specialS = "H\uffff}>";

	static final String[] DFA179_transitionS = new String[]{ "\u0001\u0001\u0004\uffff\u0001\u0001\u0005\uffff\u0001\u0001\u0003\uffff\u0001\u0001\u0001\uffff\u0001\u0004\u0001\u0001\u0003\uffff\u0002\u0004" + (((((("\u0004\uffff\u0001\u0004\u0001\uffff\u0001\u0004\u0003\uffff\u0003\u0004\u0001\uffff\u0003\u0004\u0001\uffff\u0004\u0004\u0001\uffff" + "\u0003\u0004\u0004\uffff\u0002\u0004\u0001\uffff\u0001\u0004\u0002\uffff\u0004\u0004\u0001\uffff\u0001\u0004\u0001\uffff\u0002\u0004\u0001\uffff") + "\u0001\u0004\u0003\uffff\u0001\u0004\u0001\u0001\u0002\u0004\u0001\uffff\u0001\u0004\u0002\uffff\u0003\u0004\u0001\uffff\u0003\u0004\u0001\uffff") + "\u0003\u0004\u0004\uffff\u0001\u0001\u0001\u0004\u0001\uffff\u0001\u0004\u0001\uffff\u0001\u0001\u0002\uffff\u0001\u0004\u0002\uffff\u0005\u0004") + "\u0003\uffff\u0001\u0004\u0001\uffff\u0002\u0004\u0002\uffff\u000b\u0004\u0001\uffff\u0002\u0004\u0001\uffff\u0003\u0004\u0004\uffff") + "\u0002\u0004\u0001\uffff\u0004\u0004\u0003\uffff\u0001\u0004\b\uffff\u0001\u0003\u0001\u0004\u0002\uffff\u0001\u0001\u0002\uffff\u0001\u0001") + "\u0007\uffff\u0001\u0002\u0004\uffff\u0001\u0001\u0002\uffff\u0001\u0001\u0006\uffff\u0001\u0001\u0003\uffff\u0001\u0001"), "", "\u0001\u0001\u0004\uffff\u0001\u0001\u0005\uffff\u0001\u0001\u0003\uffff\u0001\u0001\u0001\uffff\u0001\u0006\u0001\u0001\u0003\uffff\u0002%" + ((((((((("\u0004\uffff\u0001%\u0001\uffff\u0001\t\u0003\uffff\u0001\n\u0001\u000b\u0001\f\u0001\uffff\u0001%\u0001$\u0001" + "%\u0001\uffff\u0002%\u0001\u001e\u0001\r\u0001\uffff\u0001%\u0001\u001c\u0001\u000e\u0004\uffff\u0001$\u0001\u000f\u0001") + "\uffff\u0001\u0010\u0002\uffff\u0003%\u0001\u0011\u0001\uffff\u0001#\u0001\uffff\u0002%\u0001\uffff\u0001%") + "\u0003\uffff\u0001\u0012\u0001\u0001\u0002%\u0001\uffff\u0001\u0013\u0002\uffff\u0001$\u0001\u001f\u0001%\u0001\uffff\u0003") + "%\u0001\uffff\u0001 \u0001%\u0001\b\u0004\uffff\u0001\u0001\u0001%\u0001\uffff\u0001%\u0001\uffff\u0001\u0001") + "\u0002\uffff\u0001%\u0002\uffff\u0005%\u0003\uffff\u0001%\u0001\uffff\u0002%\u0001\uffff\u0001\"\u0001%") + "\u0001\u0014\u0004%\u0001\u0015\u0001\u001d\u0001\u0016\u0001\u001b\u0001\u0017\u0001\uffff\u0001\u0001\u0001%\u0001\uffff\u0001$\u0001!") + "\u0001%\u0004\uffff\u0002%\u0001\uffff\u0001\u0018\u0001%\u0001\u0019\u0001\u001a\u0003\uffff\u0001$\b\uffff") + "\u0001\u0001\u0001\u0007\u0002\uffff\u0001\u0005\u0002\uffff\u0001\u0001\u0007\uffff\u0001\u0001\u0004\uffff\u0001\u0001\u0002\uffff\u0001\u0001") + "\u0006\uffff\u0001\u0001\u0003\uffff\u0001\u0001"), "\u0001\u0001\u0001\uffff\u0002\u0001\u0017\uffff\u0001\u0001\u0012\uffff\u0002\u0001\u0006\uffff\u0001\u0001\n\uffff\u0001\u0001" + ("\u0011\uffff\u0001\u0001\u0002\uffff\u0001\u0001\u0002\uffff\u0001\u0001\u001c\uffff\u0001\u0001\t\uffff\u0001\u0001\u001a\uffff" + "\u0002\u0001\u0001\uffff\u0001\u0001\u0002\uffff\u0001\u0004\u0002\u0001\u0007\uffff\u0001\u0001\u0002\uffff\u0001\u0001"), "", "\u0001&\u0002\uffff\u0001\u0001", "\u0001\u0001\u0001\"\u0005\uffff\u0001\'", "\u0001\u0001\u0001\"\u0005\uffff\u0001\'", "\u0001\u0001\u0001\"\u0005\uffff\u0001\'\u0002\uffff\u0001\"", "\u0001\u0001\u0001\"\u0005\uffff\u0001\'", "\u0001\u0001\u0001\"\u0005\uffff\u0001\'", "\u0001\u0001\u0001\"\u0005\uffff\u0001\'", "\u0001\u0001\u0001\"\u0005\uffff\u0001\'", "\u0001\u0001\u0001\"\u0005\uffff\u0001\'", "\u0001\u0001\u0001\"\u0005\uffff\u0001\'", "\u0001\u0001\u0001\"\u0005\uffff\u0001\'", "\u0001\u0001\u0001\"\u0005\uffff\u0001\'", "\u0001\u0001\u0001\"\u0005\uffff\u0001\'", "\u0001\u0001\u0001\"\u0005\uffff\u0001\'", "\u0001\u0001\u0001\"\u0005\uffff\u0001\'", "\u0001\u0001\u0001\"\u0005\uffff\u0001\'", "\u0001\u0001\u0001\"\u0005\uffff\u0001\'", "\u0001\u0001\u0001\"\u0005\uffff\u0001\'", "\u0001\u0001\u0001\"\u0005\uffff\u0001\'", "\u0001\u0001\u0001\"\u0005\uffff\u0001\'", "\u0001\u0001\u0001\"\u0005\uffff\u0001\'", "\u0001\u0001\u0001\"\u0005\uffff\u0001\'", "\u0001\u0001\u0001\"\u0005\uffff\u0001\'", "\u0001\u0001\u0001\"\u0005\uffff\u0001\'", "\u0001\u0001\u0001\"\u0005\uffff\u0001\'", "\u0001\u0001\u0006\uffff\u0001\'", "\u0001\"\u0005\uffff\u0001\'", "\u0001\u0001\u0001\"\u0005\uffff\u0001\'\u0002\uffff\u0001\"", "\u0001\u0001\u0001\"\u0005\uffff\u0001\'\u0002\uffff\u0001\"", "", "\u0001\u0001\u0001\"\u0005\uffff\u0001\'\u0002\uffff\u0001\"", "\u0001\'", "\u0001\u0001\u0001\"\u0005\uffff\u0001\'", "\u0001\"\u0004\uffff\u0001\"\u0005\uffff\u0001\"\u0003\uffff\u0001\"\u0001\uffff\u0002\"\u0003\uffff\u0002" + (((((((("\"\u0001\u0001\u0001\uffff\u0002\u0001\u0001\"\u0001\uffff\u0001\"\u0003\uffff\u0003\"\u0001\uffff\u0003\"\u0001\uffff" + "\u0004\"\u0001\uffff\u0003\"\u0001\uffff\u0001\u0001\u0002\uffff\u0002\"\u0001\uffff\u0001\"\u0002\uffff\u0004\"") + "\u0001\uffff\u0001\"\u0001\uffff\u0002\"\u0001\uffff\u0001)\u0001\u0001\u0002\uffff\u0004\"\u0001\u0001\u0001\"\u0002\uffff") + "\u0003\"\u0001\uffff\u0003\"\u0001\u0001\u0003\"\u0004\uffff\u0002\"\u0001\uffff\u0001\"\u0001\uffff\u0001\"\u0002") + "\uffff\u0001\"\u0001\uffff\u0001\u0001\u0002\"\u0001*\u0002\"\u0001\u0001\u0002\uffff\u0001\"\u0001\uffff\u0002\"") + "\u0002\uffff\u000b\"\u0001\uffff\u0002\"\u0001\uffff\u0003\"\u0002\uffff\u0001\u0001\u0001\uffff\u0002\"\u0001\uffff") + "\u0004\"\u0001\uffff\u0001\u0001\u0001\uffff\u0001\"\b\uffff\u0002\"\u0002\uffff\u0001\"\u0002\uffff\u0001\"") + "\u0007\uffff\u0001\"\u0002\u0001\u0001\uffff\u0001\u0001\u0001\"\u0002\uffff\u0001(\u0001\u0001\u0005\uffff\u0001\"\u0001\uffff") + "\u0001\u0001\u0001\uffff\u0001\"\u0001\u0001"), "\u0001+\u0004\uffff\u0002-\u0004\uffff\u0001-\u0001\uffff\u0001\u0001\u0003\uffff\u0003\u0001\u0001\uffff\u0001-" + ((((("\u0001\uffff\u0001-\u0001\uffff\u0002-\u0002\u0001\u0001\uffff\u0001-\u0002\u0001\u0005\uffff\u0001\u0001\u0001\uffff\u0001" + "\u0001\u0002\uffff\u0003-\u0001\u0001\u0001\uffff\u0001-\u0001\uffff\u0002-\u0001\uffff\u0001-\u0003\uffff\u0001") + "\u0001\u0001\uffff\u0002-\u0001\uffff\u0001\u0001\u0003\uffff\u0001\"\u0001-\u0001\uffff\u0003-\u0001\uffff\u0003") + "-\u0005\uffff\u0001-\u0001\uffff\u0001-\u0004\uffff\u0001-\u0002\uffff\u0005-\u0003\uffff\u0001-") + "\u0001\uffff\u0002-\u0002\uffff\u0001-\u0001\u0001\u0004-\u0005\u0001\u0001\uffff\u0001\u0001\u0001-\u0002\uffff\u0002-") + "\u0004\uffff\u0002-\u0001\uffff\u0001\u0001\u0001-\u0002\u0001\r\uffff\u0001,"), "\u0001\u0001\u0004\uffff\u0001\u0001\u0005\uffff\u0001\u0001\u0003\uffff\u0001\u0001\u0001\uffff\u0001.\u0001\u0001\u0003\uffff\u0002" + ((((((((("0\u0004\uffff\u00010\u0001\uffff\u00011\u0003\uffff\u00012\u00013\u00014\u0001\uffff\u00010\u0001G" + "\u00010\u0001\uffff\u00020\u0001F\u00015\u0001\uffff\u00010\u0001D\u00016\u0004\uffff\u0001G\u0001") + "7\u0001\uffff\u00018\u0002\uffff\u00030\u00019\u0001\uffff\u00010\u0001\uffff\u00020\u0001\uffff") + "\u00010\u0003\uffff\u0001:\u0001\u0001\u00020\u0001\uffff\u0001;\u0002\uffff\u0002G\u00010\u0001\uffff\u0003") + "0\u0001\uffff\u00030\u0004\uffff\u0001\u0001\u00010\u0001\uffff\u00010\u0001\uffff\u0001\u0001\u0002\uffff\u0001") + "0\u0002\uffff\u00050\u0003\uffff\u00010\u0001\uffff\u00020\u0002\uffff\u00010\u0001<\u00040\u0001=") + "\u0001E\u0001>\u0001C\u0001?\u0001\uffff\u0001\u0001\u00010\u0001\uffff\u0001G\u00020\u0004\uffff\u0002") + "0\u0001\uffff\u0001@\u00010\u0001A\u0001B\u0003\uffff\u0001G\b\uffff\u0001\u0001\u0001/\u0002") + "\uffff\u0001\u0001\u0002\uffff\u0001\u0001\u0007\uffff\u0001\u0001\u0004\uffff\u0001\u0001\u0002\uffff\u0001\u0001\u0006\uffff\u0001") + "\u0001\u0003\uffff\u0001\u0001"), "\u0001\u0001\u008c\uffff\u0001\"\u0006\uffff\u0001\"", "\u0001\u0001E\uffff\u0001\"\u0006\uffff\u0001\"", "\u0001\u0001\u0001\"", "\u0001\u0001\u0001\"", "\u0001\u0001\u0001\"", "\u0001\"\u0001\uffff\u0002\"\u0017\uffff\u0001\"\u0012\uffff\u0002\"\u0006\uffff\u0001\"\n\uffff" + (("\u0001\"\u0011\uffff\u0001\"\u0002\uffff\u0001\"\u0002\uffff\u0001\"\u001c\uffff\u0001\"\t\uffff" + "\u0001\"\u0019\uffff\u0001\u0001\u0002\"\u0001\uffff\u0001\"\u0002\uffff\u0001\u0001\u0002\"\u0007\uffff\u0001\"\u0002") + "\uffff\u0001\""), "\u0001\"\u0001\uffff\u0002\"\u0017\uffff\u0001\"\u0012\uffff\u0002\"\u0006\uffff\u0001\"\n\uffff" + (("\u0001\"\u0011\uffff\u0001\"\u0002\uffff\u0001\"\u0002\uffff\u0001\"\u001c\uffff\u0001\"\t\uffff" + "\u0001\"\u0019\uffff\u0001\u0001\u0002\"\u0001\uffff\u0001\"\u0002\uffff\u0001\u0001\u0002\"\u0007\uffff\u0001\"\u0002") + "\uffff\u0001\""), "\u0001\"\u0001\uffff\u0002\"\u0017\uffff\u0001\"\u0012\uffff\u0002\"\u0006\uffff\u0001\"\n\uffff" + (("\u0001\"\u0011\uffff\u0001\"\u0002\uffff\u0001\"\u0002\uffff\u0001\"\u001c\uffff\u0001\"\t\uffff" + "\u0001\"\u0019\uffff\u0001\u0001\u0002\"\u0001\uffff\u0001\"\u0002\uffff\u0001\u0001\u0002\"\u0007\uffff\u0001\"\u0002") + "\uffff\u0001\""), "\u0001\"\u0001\uffff\u0002\"\u0017\uffff\u0001\"\u0012\uffff\u0002\"\u0006\uffff\u0001\"\n\uffff" + (("\u0001\"\u0011\uffff\u0001\"\u0002\uffff\u0001\"\u0002\uffff\u0001\"\u001c\uffff\u0001\"\t\uffff" + "\u0001\"\u0019\uffff\u0001\u0001\u0002\"\u0001\uffff\u0001\"\u0002\uffff\u0001\u0001\u0002\"\u0007\uffff\u0001\"\u0002") + "\uffff\u0001\""), "\u0001\"\u0001\uffff\u0002\"\u0017\uffff\u0001\"\u0012\uffff\u0002\"\u0006\uffff\u0001\"\n\uffff" + (("\u0001\"\u0011\uffff\u0001\"\u0002\uffff\u0001\"\u0002\uffff\u0001\"\u001c\uffff\u0001\"\t\uffff" + "\u0001\"\u0019\uffff\u0001\u0001\u0002\"\u0001\uffff\u0001\"\u0002\uffff\u0001\u0001\u0002\"\u0007\uffff\u0001\"\u0002") + "\uffff\u0001\""), "\u0001\"\u0001\uffff\u0002\"\u0017\uffff\u0001\"\u0012\uffff\u0002\"\u0006\uffff\u0001\"\n\uffff" + (("\u0001\"\u0011\uffff\u0001\"\u0002\uffff\u0001\"\u0002\uffff\u0001\"\u001c\uffff\u0001\"\t\uffff" + "\u0001\"\u0019\uffff\u0001\u0001\u0002\"\u0001\uffff\u0001\"\u0002\uffff\u0001\u0001\u0002\"\u0007\uffff\u0001\"\u0002") + "\uffff\u0001\""), "\u0001\"\u0001\uffff\u0002\"\u0017\uffff\u0001\"\u0012\uffff\u0002\"\u0006\uffff\u0001\"\n\uffff" + (("\u0001\"\u0011\uffff\u0001\"\u0002\uffff\u0001\"\u0002\uffff\u0001\"\u001c\uffff\u0001\"\t\uffff" + "\u0001\"\u0019\uffff\u0001\u0001\u0002\"\u0001\uffff\u0001\"\u0002\uffff\u0001\u0001\u0002\"\u0007\uffff\u0001\"\u0002") + "\uffff\u0001\""), "\u0001\"\u0001\uffff\u0002\"\u0017\uffff\u0001\"\u0012\uffff\u0002\"\u0006\uffff\u0001\"\n\uffff" + (("\u0001\"\u0011\uffff\u0001\"\u0002\uffff\u0001\"\u0002\uffff\u0001\"\u001c\uffff\u0001\"\t\uffff" + "\u0001\"\u0019\uffff\u0001\u0001\u0002\"\u0001\uffff\u0001\"\u0002\uffff\u0001\u0001\u0002\"\u0007\uffff\u0001\"\u0002") + "\uffff\u0001\""), "\u0001\"\u0001\uffff\u0002\"\u0017\uffff\u0001\"\u0012\uffff\u0002\"\u0006\uffff\u0001\"\n\uffff" + (("\u0001\"\u0011\uffff\u0001\"\u0002\uffff\u0001\"\u0002\uffff\u0001\"\u001c\uffff\u0001\"\t\uffff" + "\u0001\"\u0019\uffff\u0001\u0001\u0002\"\u0001\uffff\u0001\"\u0002\uffff\u0001\u0001\u0002\"\u0007\uffff\u0001\"\u0002") + "\uffff\u0001\""), "\u0001\"\u0001\uffff\u0002\"\u0017\uffff\u0001\"\u0012\uffff\u0002\"\u0006\uffff\u0001\"\n\uffff" + (("\u0001\"\u0011\uffff\u0001\"\u0002\uffff\u0001\"\u0002\uffff\u0001\"\u001c\uffff\u0001\"\t\uffff" + "\u0001\"\u0019\uffff\u0001\u0001\u0002\"\u0001\uffff\u0001\"\u0002\uffff\u0001\u0001\u0002\"\u0007\uffff\u0001\"\u0002") + "\uffff\u0001\""), "\u0001\"\u0001\uffff\u0002\"\u0017\uffff\u0001\"\u0012\uffff\u0002\"\u0006\uffff\u0001\"\n\uffff" + (("\u0001\"\u0011\uffff\u0001\"\u0002\uffff\u0001\"\u0002\uffff\u0001\"\u001c\uffff\u0001\"\t\uffff" + "\u0001\"\u0019\uffff\u0001\u0001\u0002\"\u0001\uffff\u0001\"\u0002\uffff\u0001\u0001\u0002\"\u0007\uffff\u0001\"\u0002") + "\uffff\u0001\""), "\u0001\"\u0001\uffff\u0002\"\u0017\uffff\u0001\"\u0012\uffff\u0002\"\u0006\uffff\u0001\"\n\uffff" + (("\u0001\"\u0011\uffff\u0001\"\u0002\uffff\u0001\"\u0002\uffff\u0001\"\u001c\uffff\u0001\"\t\uffff" + "\u0001\"\u0019\uffff\u0001\u0001\u0002\"\u0001\uffff\u0001\"\u0002\uffff\u0001\u0001\u0002\"\u0007\uffff\u0001\"\u0002") + "\uffff\u0001\""), "\u0001\"\u0001\uffff\u0002\"\u0017\uffff\u0001\"\u0012\uffff\u0002\"\u0006\uffff\u0001\"\n\uffff" + (("\u0001\"\u0011\uffff\u0001\"\u0002\uffff\u0001\"\u0002\uffff\u0001\"\u001c\uffff\u0001\"\t\uffff" + "\u0001\"\u0019\uffff\u0001\u0001\u0002\"\u0001\uffff\u0001\"\u0002\uffff\u0001\u0001\u0002\"\u0007\uffff\u0001\"\u0002") + "\uffff\u0001\""), "\u0001\"\u0001\uffff\u0002\"\u0017\uffff\u0001\"\u0012\uffff\u0002\"\u0006\uffff\u0001\"\n\uffff" + (("\u0001\"\u0011\uffff\u0001\"\u0002\uffff\u0001\"\u0002\uffff\u0001\"\u001c\uffff\u0001\"\t\uffff" + "\u0001\"\u0019\uffff\u0001\u0001\u0002\"\u0001\uffff\u0001\"\u0002\uffff\u0001\u0001\u0002\"\u0007\uffff\u0001\"\u0002") + "\uffff\u0001\""), "\u0001\"\u0001\uffff\u0002\"\u0017\uffff\u0001\"\u0012\uffff\u0002\"\u0006\uffff\u0001\"\n\uffff" + (("\u0001\"\u0011\uffff\u0001\"\u0002\uffff\u0001\"\u0002\uffff\u0001\"\u001c\uffff\u0001\"\t\uffff" + "\u0001\"\u0019\uffff\u0001\u0001\u0002\"\u0001\uffff\u0001\"\u0002\uffff\u0001\u0001\u0002\"\u0007\uffff\u0001\"\u0002") + "\uffff\u0001\""), "\u0001\"\u0001\uffff\u0002\"\u0017\uffff\u0001\"\u0012\uffff\u0002\"\u0006\uffff\u0001\"\n\uffff" + (("\u0001\"\u0011\uffff\u0001\"\u0002\uffff\u0001\"\u0002\uffff\u0001\"\u001c\uffff\u0001\"\t\uffff" + "\u0001\"\u0019\uffff\u0001\u0001\u0002\"\u0001\uffff\u0001\"\u0002\uffff\u0001\u0001\u0002\"\u0007\uffff\u0001\"\u0002") + "\uffff\u0001\""), "\u0001\"\u0001\uffff\u0002\"\u0017\uffff\u0001\"\u0012\uffff\u0002\"\u0006\uffff\u0001\"\n\uffff" + (("\u0001\"\u0011\uffff\u0001\"\u0002\uffff\u0001\"\u0002\uffff\u0001\"\u001c\uffff\u0001\"\t\uffff" + "\u0001\"\u0019\uffff\u0001\u0001\u0002\"\u0001\uffff\u0001\"\u0002\uffff\u0001\u0001\u0002\"\u0007\uffff\u0001\"\u0002") + "\uffff\u0001\""), "\u0001\"\u0001\uffff\u0002\"\u0017\uffff\u0001\"\u0012\uffff\u0002\"\u0006\uffff\u0001\"\n\uffff" + (("\u0001\"\u0011\uffff\u0001\"\u0002\uffff\u0001\"\u0002\uffff\u0001\"\u001c\uffff\u0001\"\t\uffff" + "\u0001\"\u0019\uffff\u0001\u0001\u0002\"\u0001\uffff\u0001\"\u0002\uffff\u0001\u0001\u0002\"\u0007\uffff\u0001\"\u0002") + "\uffff\u0001\""), "\u0001\"\u0001\uffff\u0002\"\u0017\uffff\u0001\"\u0012\uffff\u0002\"\u0006\uffff\u0001\"\n\uffff" + (("\u0001\"\u0011\uffff\u0001\"\u0002\uffff\u0001\"\u0002\uffff\u0001\"\u001c\uffff\u0001\"\t\uffff" + "\u0001\"\u0019\uffff\u0001\u0001\u0002\"\u0001\uffff\u0001\"\u0002\uffff\u0001\u0001\u0002\"\u0007\uffff\u0001\"\u0002") + "\uffff\u0001\""), "\u0001\"\u0001\uffff\u0002\"\u0017\uffff\u0001\"\u0012\uffff\u0002\"\u0006\uffff\u0001\"\n\uffff" + (("\u0001\"\u0011\uffff\u0001\"\u0002\uffff\u0001\"\u0002\uffff\u0001\"\u001c\uffff\u0001\"\t\uffff" + "\u0001\"\u0019\uffff\u0001\u0001\u0002\"\u0001\uffff\u0001\"\u0002\uffff\u0001\u0001\u0002\"\u0007\uffff\u0001\"\u0002") + "\uffff\u0001\""), "\u0001\"\u0001\uffff\u0002\"\u0017\uffff\u0001\"\u0012\uffff\u0002\"\u0006\uffff\u0001\"\n\uffff" + (("\u0001\"\u0011\uffff\u0001\"\u0002\uffff\u0001\"\u0002\uffff\u0001\"\u001c\uffff\u0001\"\t\uffff" + "\u0001\"\u0019\uffff\u0001\u0001\u0002\"\u0001\uffff\u0001\"\u0002\uffff\u0001\u0001\u0002\"\u0007\uffff\u0001\"\u0002") + "\uffff\u0001\""), "\u0001\"\u0001\uffff\u0002\"\u0017\uffff\u0001\"\u0012\uffff\u0002\"\u0006\uffff\u0001\"\n\uffff" + (("\u0001\"\u0011\uffff\u0001\"\u0002\uffff\u0001\"\u0002\uffff\u0001\"\u001c\uffff\u0001\"\t\uffff" + "\u0001\"\u0019\uffff\u0001\u0001\u0002\"\u0001\uffff\u0001\"\u0002\uffff\u0001\u0001\u0002\"\u0007\uffff\u0001\"\u0002") + "\uffff\u0001\""), "\u0001\"\u0001\uffff\u0002\"\u0017\uffff\u0001\"\u0012\uffff\u0002\"\u0006\uffff\u0001\"\n\uffff" + (("\u0001\"\u0011\uffff\u0001\"\u0002\uffff\u0001\"\u0002\uffff\u0001\"\u001c\uffff\u0001\"\t\uffff" + "\u0001\"\u0019\uffff\u0001\u0001\u0002\"\u0001\uffff\u0001\"\u0002\uffff\u0001\u0001\u0002\"\u0007\uffff\u0001\"\u0002") + "\uffff\u0001\""), "\u0001\"\u0001\uffff\u0002\"\u0017\uffff\u0001\"\u0012\uffff\u0002\"\u0006\uffff\u0001\"\n\uffff" + (("\u0001\"\u0011\uffff\u0001\"\u0002\uffff\u0001\"\u0002\uffff\u0001\"\u001c\uffff\u0001\"\t\uffff" + "\u0001\"\u0019\uffff\u0001\u0001\u0002\"\u0001\uffff\u0001\"\u0002\uffff\u0001\u0001\u0002\"\u0007\uffff\u0001\"\u0002") + "\uffff\u0001\""), "\u0001\"\u0001\uffff\u0002\"\u0017\uffff\u0001\"\u0012\uffff\u0002\"\u0006\uffff\u0001\"\n\uffff" + (("\u0001\"\u0011\uffff\u0001\"\u0002\uffff\u0001\"\u0002\uffff\u0001\"\u001c\uffff\u0001\"\t\uffff" + "\u0001\"\u0019\uffff\u0001\u0001\u0002\"\u0001\uffff\u0001\"\u0002\uffff\u0001\u0001\u0002\"\u0007\uffff\u0001\"\u0002") + "\uffff\u0001\""), "\u0001\"\u0001\uffff\u0002\"\u0017\uffff\u0001\"\u0012\uffff\u0002\"\u0006\uffff\u0001\"\n\uffff" + (("\u0001\"\u0011\uffff\u0001\"\u0002\uffff\u0001\"\u0002\uffff\u0001\"\u001c\uffff\u0001\"\t\uffff" + "\u0001\"\u001a\uffff\u0002\"\u0001\uffff\u0001\"\u0002\uffff\u0001\u0001\u0002\"\u0007\uffff\u0001\"\u0002\uffff") + "\u0001\"") };

	static final short[] DFA179_eot = DFA.unpackEncodedString(Cql_Parser.DFA179_eotS);

	static final short[] DFA179_eof = DFA.unpackEncodedString(Cql_Parser.DFA179_eofS);

	static final char[] DFA179_min = DFA.unpackEncodedStringToUnsignedChars(Cql_Parser.DFA179_minS);

	static final char[] DFA179_max = DFA.unpackEncodedStringToUnsignedChars(Cql_Parser.DFA179_maxS);

	static final short[] DFA179_accept = DFA.unpackEncodedString(Cql_Parser.DFA179_acceptS);

	static final short[] DFA179_special = DFA.unpackEncodedString(Cql_Parser.DFA179_specialS);

	static final short[][] DFA179_transition;

	static {
		int numStates = Cql_Parser.DFA179_transitionS.length;
		DFA179_transition = new short[numStates][];
		for (int i = 0; i < numStates; i++) {
			Cql_Parser.DFA179_transition[i] = DFA.unpackEncodedString(Cql_Parser.DFA179_transitionS[i]);
		}
	}

	protected class DFA179 extends DFA {
		public DFA179(BaseRecognizer recognizer) {
			this.recognizer = recognizer;
			this.decisionNumber = 179;
			this.eot = Cql_Parser.DFA179_eot;
			this.eof = Cql_Parser.DFA179_eof;
			this.min = Cql_Parser.DFA179_min;
			this.max = Cql_Parser.DFA179_max;
			this.accept = Cql_Parser.DFA179_accept;
			this.special = Cql_Parser.DFA179_special;
			this.transition = Cql_Parser.DFA179_transition;
		}

		@Override
		public String getDescription() {
			return "1356:1: term returns [Term.Raw term] : (v= value |f= function | '(' c= comparatorType ')' t= term );";
		}
	}

	static final String DFA182_eotS = "\u001f\uffff";

	static final String DFA182_eofS = "\u001f\uffff";

	static final String DFA182_minS = "\u0001\u0006\u0001\uffff\u001b\u0018\u0002\uffff";

	static final String DFA182_maxS = "\u0001\u00cb\u0001\uffff\u001a\u00bf\u0001\u00bd\u0002\uffff";

	static final String DFA182_acceptS = "\u0001\uffff\u0001\u0001\u001b\uffff\u0001\u0002\u0001\u0003";

	static final String DFA182_specialS = "\u001f\uffff}>";

	static final String[] DFA182_transitionS = new String[]{ "\u0001\u0001\u0004\uffff\u0001\u0001\u0002\uffff\u0001\u001c\u0002\uffff\u0001\u0001\u0003\uffff\u0001\u0001\u0001\uffff\u0001\u0002\u0001\u0001" + (((((((("\u0003\uffff\u0002\u0004\u0004\uffff\u0001\u0004\u0001\uffff\u0001\u0005\u0003\uffff\u0001\u0006\u0001\u0007\u0001\b\u0001\uffff\u0001\u0004" + "\u0001\u001b\u0001\u0004\u0001\uffff\u0002\u0004\u0001\u001a\u0001\t\u0001\uffff\u0001\u0004\u0001\u0018\u0001\n\u0004\uffff\u0001\u001b\u0001") + "\u000b\u0001\uffff\u0001\f\u0002\uffff\u0003\u0004\u0001\r\u0001\uffff\u0001\u0004\u0001\uffff\u0002\u0004\u0001\uffff\u0001\u0004") + "\u0003\uffff\u0001\u000e\u0001\u0001\u0002\u0004\u0001\uffff\u0001\u000f\u0002\uffff\u0002\u001b\u0001\u0004\u0001\uffff\u0003\u0004\u0001\uffff") + "\u0003\u0004\u0004\uffff\u0001\u0001\u0001\u0004\u0001\uffff\u0001\u0004\u0001\uffff\u0001\u0001\u0002\uffff\u0001\u0004\u0002\uffff\u0005\u0004") + "\u0003\uffff\u0001\u0004\u0001\uffff\u0002\u0004\u0002\uffff\u0001\u0004\u0001\u0010\u0004\u0004\u0001\u0011\u0001\u0019\u0001\u0012\u0001\u0017\u0001\u0013") + "\u0001\uffff\u0001\u0001\u0001\u0004\u0001\uffff\u0001\u001b\u0002\u0004\u0004\uffff\u0002\u0004\u0001\uffff\u0001\u0014\u0001\u0004\u0001\u0015\u0001") + "\u0016\u0003\uffff\u0001\u001b\b\uffff\u0001\u0001\u0001\u0003\u0002\uffff\u0001\u0001\u0002\uffff\u0001\u0001\u0007\uffff\u0001\u0001") + "\u0004\uffff\u0001\u0001\u0002\uffff\u0001\u0001\u0006\uffff\u0001\u0001\u0003\uffff\u0001\u0001"), "", "\u0001\u001e\u009f\uffff\u0001\u0001\u0001\uffff\u0001\u001d\u0002\uffff\u0001\u001d\u0001\uffff\u0001\u0001", "\u0001\u001e\u009f\uffff\u0001\u0001\u0001\uffff\u0001\u001d\u0002\uffff\u0001\u001d\u0001\uffff\u0001\u0001", "\u0001\u001e\u009f\uffff\u0001\u0001\u0001\uffff\u0001\u001d\u0002\uffff\u0001\u001d\u0001\uffff\u0001\u0001", "\u0001\u001e\u009f\uffff\u0001\u0001\u0001\uffff\u0001\u001d\u0002\uffff\u0001\u001d\u0001\uffff\u0001\u0001", "\u0001\u001e\u009f\uffff\u0001\u0001\u0001\uffff\u0001\u001d\u0002\uffff\u0001\u001d\u0001\uffff\u0001\u0001", "\u0001\u001e\u009f\uffff\u0001\u0001\u0001\uffff\u0001\u001d\u0002\uffff\u0001\u001d\u0001\uffff\u0001\u0001", "\u0001\u001e\u009f\uffff\u0001\u0001\u0001\uffff\u0001\u001d\u0002\uffff\u0001\u001d\u0001\uffff\u0001\u0001", "\u0001\u001e\u009f\uffff\u0001\u0001\u0001\uffff\u0001\u001d\u0002\uffff\u0001\u001d\u0001\uffff\u0001\u0001", "\u0001\u001e\u009f\uffff\u0001\u0001\u0001\uffff\u0001\u001d\u0002\uffff\u0001\u001d\u0001\uffff\u0001\u0001", "\u0001\u001e\u009f\uffff\u0001\u0001\u0001\uffff\u0001\u001d\u0002\uffff\u0001\u001d\u0001\uffff\u0001\u0001", "\u0001\u001e\u009f\uffff\u0001\u0001\u0001\uffff\u0001\u001d\u0002\uffff\u0001\u001d\u0001\uffff\u0001\u0001", "\u0001\u001e\u009f\uffff\u0001\u0001\u0001\uffff\u0001\u001d\u0002\uffff\u0001\u001d\u0001\uffff\u0001\u0001", "\u0001\u001e\u009f\uffff\u0001\u0001\u0001\uffff\u0001\u001d\u0002\uffff\u0001\u001d\u0001\uffff\u0001\u0001", "\u0001\u001e\u009f\uffff\u0001\u0001\u0001\uffff\u0001\u001d\u0002\uffff\u0001\u001d\u0001\uffff\u0001\u0001", "\u0001\u001e\u009f\uffff\u0001\u0001\u0001\uffff\u0001\u001d\u0002\uffff\u0001\u001d\u0001\uffff\u0001\u0001", "\u0001\u001e\u009f\uffff\u0001\u0001\u0001\uffff\u0001\u001d\u0002\uffff\u0001\u001d\u0001\uffff\u0001\u0001", "\u0001\u001e\u009f\uffff\u0001\u0001\u0001\uffff\u0001\u001d\u0002\uffff\u0001\u001d\u0001\uffff\u0001\u0001", "\u0001\u001e\u009f\uffff\u0001\u0001\u0001\uffff\u0001\u001d\u0002\uffff\u0001\u001d\u0001\uffff\u0001\u0001", "\u0001\u001e\u009f\uffff\u0001\u0001\u0001\uffff\u0001\u001d\u0002\uffff\u0001\u001d\u0001\uffff\u0001\u0001", "\u0001\u001e\u009f\uffff\u0001\u0001\u0001\uffff\u0001\u001d\u0002\uffff\u0001\u001d\u0001\uffff\u0001\u0001", "\u0001\u001e\u009f\uffff\u0001\u0001\u0001\uffff\u0001\u001d\u0002\uffff\u0001\u001d\u0001\uffff\u0001\u0001", "\u0001\u001e\u009f\uffff\u0001\u0001\u0001\uffff\u0001\u001d\u0002\uffff\u0001\u001d\u0001\uffff\u0001\u0001", "\u0001\u001e\u009f\uffff\u0001\u0001\u0001\uffff\u0001\u001d\u0002\uffff\u0001\u001d\u0001\uffff\u0001\u0001", "\u0001\u001e\u009f\uffff\u0001\u0001\u0001\uffff\u0001\u001d\u0002\uffff\u0001\u001d\u0001\uffff\u0001\u0001", "\u0001\u001e\u009f\uffff\u0001\u0001\u0001\uffff\u0001\u001d\u0002\uffff\u0001\u001d\u0001\uffff\u0001\u0001", "\u0001\u001e\u00a1\uffff\u0001\u001d\u0002\uffff\u0001\u001d\u0001\uffff\u0001\u0001", "\u0001\u001e\u00a1\uffff\u0001\u001d\u0002\uffff\u0001\u001d", "", "" };

	static final short[] DFA182_eot = DFA.unpackEncodedString(Cql_Parser.DFA182_eotS);

	static final short[] DFA182_eof = DFA.unpackEncodedString(Cql_Parser.DFA182_eofS);

	static final char[] DFA182_min = DFA.unpackEncodedStringToUnsignedChars(Cql_Parser.DFA182_minS);

	static final char[] DFA182_max = DFA.unpackEncodedStringToUnsignedChars(Cql_Parser.DFA182_maxS);

	static final short[] DFA182_accept = DFA.unpackEncodedString(Cql_Parser.DFA182_acceptS);

	static final short[] DFA182_special = DFA.unpackEncodedString(Cql_Parser.DFA182_specialS);

	static final short[][] DFA182_transition;

	static {
		int numStates = Cql_Parser.DFA182_transitionS.length;
		DFA182_transition = new short[numStates][];
		for (int i = 0; i < numStates; i++) {
			Cql_Parser.DFA182_transition[i] = DFA.unpackEncodedString(Cql_Parser.DFA182_transitionS[i]);
		}
	}

	protected class DFA182 extends DFA {
		public DFA182(BaseRecognizer recognizer) {
			this.recognizer = recognizer;
			this.decisionNumber = 182;
			this.eot = Cql_Parser.DFA182_eot;
			this.eof = Cql_Parser.DFA182_eof;
			this.min = Cql_Parser.DFA182_min;
			this.max = Cql_Parser.DFA182_max;
			this.accept = Cql_Parser.DFA182_accept;
			this.special = Cql_Parser.DFA182_special;
			this.transition = Cql_Parser.DFA182_transition;
		}

		@Override
		public String getDescription() {
			return "1373:1: normalColumnOperation[List<Pair<ColumnDefinition.Raw, Operation.RawUpdate>> operations, ColumnDefinition.Raw key] : (t= term ( '+' c= cident )? |c= cident sig= ( '+' | '-' ) t= term |c= cident i= INTEGER );";
		}
	}

	static final String DFA190_eotS = "\u001d\uffff";

	static final String DFA190_eofS = "\u001d\uffff";

	static final String DFA190_minS = "\u0001\u0017\u0019\u00c4\u0001\u0006\u0002\uffff";

	static final String DFA190_maxS = "\u0001\u00aa\u0019\u00c4\u0001\u00cb\u0002\uffff";

	static final String DFA190_acceptS = "\u001b\uffff\u0001\u0001\u0001\u0002";

	static final String DFA190_specialS = "\u001d\uffff}>";

	static final String[] DFA190_transitionS = new String[]{ "\u0001\u0001\u0004\uffff\u0002\u0003\u0004\uffff\u0001\u0003\u0001\uffff\u0001\u0004\u0003\uffff\u0001\u0005\u0001\u0006\u0001\u0007\u0001\uffff" + (((((("\u0001\u0003\u0001\u0019\u0001\u0003\u0001\uffff\u0002\u0003\u0001\u0019\u0001\b\u0001\uffff\u0001\u0003\u0001\u0017\u0001\t\u0004\uffff\u0001\u0019" + "\u0001\n\u0001\uffff\u0001\u000b\u0002\uffff\u0003\u0003\u0001\f\u0001\uffff\u0001\u0003\u0001\uffff\u0002\u0003\u0001\uffff\u0001") + "\u0003\u0003\uffff\u0001\r\u0001\uffff\u0002\u0003\u0001\uffff\u0001\u000e\u0002\uffff\u0002\u0019\u0001\u0003\u0001\uffff\u0003\u0003") + "\u0001\uffff\u0003\u0003\u0005\uffff\u0001\u0003\u0001\uffff\u0001\u0003\u0004\uffff\u0001\u0003\u0002\uffff\u0005\u0003\u0003\uffff") + "\u0001\u0003\u0001\uffff\u0002\u0003\u0002\uffff\u0001\u0003\u0001\u000f\u0004\u0003\u0001\u0010\u0001\u0018\u0001\u0011\u0001\u0016\u0001\u0012\u0002\uffff") + "\u0001\u0003\u0001\uffff\u0001\u0019\u0002\u0003\u0004\uffff\u0002\u0003\u0001\uffff\u0001\u0013\u0001\u0003\u0001\u0014\u0001\u0015\u0003\uffff") + "\u0001\u0019\t\uffff\u0001\u0002"), "\u0001\u001a", "\u0001\u001a", "\u0001\u001a", "\u0001\u001a", "\u0001\u001a", "\u0001\u001a", "\u0001\u001a", "\u0001\u001a", "\u0001\u001a", "\u0001\u001a", "\u0001\u001a", "\u0001\u001a", "\u0001\u001a", "\u0001\u001a", "\u0001\u001a", "\u0001\u001a", "\u0001\u001a", "\u0001\u001a", "\u0001\u001a", "\u0001\u001a", "\u0001\u001a", "\u0001\u001a", "\u0001\u001a", "\u0001\u001a", "\u0001\u001a", "\u0001\u001b\u0004\uffff\u0001\u001b\u0005\uffff\u0001\u001b\u0003\uffff\u0001\u001b\u0002\uffff\u0001\u001b\u0003\uffff\u0002" + (((((("\u001b\u0004\uffff\u0001\u001b\u0001\uffff\u0001\u001b\u0003\uffff\u0003\u001b\u0001\uffff\u0003\u001b\u0001\uffff\u0004\u001b" + "\u0001\uffff\u0003\u001b\u0004\uffff\u0002\u001b\u0001\uffff\u0001\u001b\u0002\uffff\u0004\u001b\u0001\uffff\u0001\u001b\u0001\uffff") + "\u0002\u001b\u0001\uffff\u0001\u001b\u0003\uffff\u0004\u001b\u0001\uffff\u0001\u001b\u0002\uffff\u0003\u001b\u0001\uffff\u0003\u001b") + "\u0001\uffff\u0003\u001b\u0004\uffff\u0002\u001b\u0001\uffff\u0001\u001b\u0004\uffff\u0001\u001b\u0002\uffff\u0005\u001b\u0003\uffff") + "\u0001\u001b\u0001\uffff\u0002\u001b\u0002\uffff\u000b\u001b\u0002\uffff\u0001\u001b\u0001\uffff\u0003\u001b\u0004\uffff\u0002") + "\u001b\u0001\uffff\u0004\u001b\u0003\uffff\u0001\u001b\f\uffff\u0001\u001b\u0002\uffff\u0001\u001b\f\uffff\u0001\u001b") + "\r\uffff\u0001\u001c"), "", "" };

	static final short[] DFA190_eot = DFA.unpackEncodedString(Cql_Parser.DFA190_eotS);

	static final short[] DFA190_eof = DFA.unpackEncodedString(Cql_Parser.DFA190_eofS);

	static final char[] DFA190_min = DFA.unpackEncodedStringToUnsignedChars(Cql_Parser.DFA190_minS);

	static final char[] DFA190_max = DFA.unpackEncodedStringToUnsignedChars(Cql_Parser.DFA190_maxS);

	static final short[] DFA190_accept = DFA.unpackEncodedString(Cql_Parser.DFA190_acceptS);

	static final short[] DFA190_special = DFA.unpackEncodedString(Cql_Parser.DFA190_specialS);

	static final short[][] DFA190_transition;

	static {
		int numStates = Cql_Parser.DFA190_transitionS.length;
		DFA190_transition = new short[numStates][];
		for (int i = 0; i < numStates; i++) {
			Cql_Parser.DFA190_transition[i] = DFA.unpackEncodedString(Cql_Parser.DFA190_transitionS[i]);
		}
	}

	protected class DFA190 extends DFA {
		public DFA190(BaseRecognizer recognizer) {
			this.recognizer = recognizer;
			this.decisionNumber = 190;
			this.eot = Cql_Parser.DFA190_eot;
			this.eof = Cql_Parser.DFA190_eof;
			this.min = Cql_Parser.DFA190_min;
			this.max = Cql_Parser.DFA190_max;
			this.accept = Cql_Parser.DFA190_accept;
			this.special = Cql_Parser.DFA190_special;
			this.transition = Cql_Parser.DFA190_transition;
		}

		@Override
		public String getDescription() {
			return "1453:1: property[PropertyDefinitions props] : (k= noncol_ident '=' simple= propertyValue |k= noncol_ident '=' map= mapLiteral );";
		}
	}

	static final String DFA196_eotS = "A\uffff";

	static final String DFA196_eofS = "A\uffff";

	static final String DFA196_minS = "\u0001\u000e\u001a1\u0001\uffff\u0001\u000e\u0003\uffff\u0001\u00a9\u0002\uffff\u001a1\u0004\uffff";

	static final String DFA196_maxS = "\u0001\u00b8\u001a\u00c7\u0001\uffff\u0001\u00b8\u0003\uffff\u0001\u00c0\u0002\uffff\u001a\u00c7\u0004\uffff";

	static final String DFA196_acceptS = "\u001b\uffff\u0001\u0004\u0001\uffff\u0001\u0001\u0001\u0002\u0001\u0003\u0001\uffff\u0001\u0007\u0001\b\u001a\uffff\u0001\n\u0001\u0005\u0001" + "\u0006\u0001\t";

	static final String DFA196_specialS = "A\uffff}>";

	static final String[] DFA196_transitionS = new String[]{ "\u0001\u0001\b\uffff\u0001\u0002\u0004\uffff\u0002\u0004\u0004\uffff\u0001\u0004\u0001\uffff\u0001\u0005\u0003\uffff\u0001\u0006\u0001\u0007" + (((((("\u0001\b\u0001\uffff\u0001\u0004\u0001\u001a\u0001\u0004\u0001\uffff\u0002\u0004\u0001\u001a\u0001\t\u0001\uffff\u0001\u0004\u0001\u0018\u0001\n" + "\u0004\uffff\u0001\u001a\u0001\u000b\u0001\uffff\u0001\f\u0002\uffff\u0003\u0004\u0001\r\u0001\uffff\u0001\u0004\u0001\uffff") + "\u0002\u0004\u0001\uffff\u0001\u0004\u0003\uffff\u0001\u000e\u0001\uffff\u0002\u0004\u0001\uffff\u0001\u000f\u0002\uffff\u0002\u001a\u0001") + "\u0004\u0001\uffff\u0003\u0004\u0001\uffff\u0003\u0004\u0005\uffff\u0001\u0004\u0001\uffff\u0001\u0004\u0004\uffff\u0001\u0004\u0002\uffff") + "\u0005\u0004\u0003\uffff\u0001\u0004\u0001\uffff\u0002\u0004\u0002\uffff\u0001\u0004\u0001\u0010\u0004\u0004\u0001\u0011\u0001\u0019\u0001\u0012\u0001\u0017") + "\u0001\u0013\u0001\uffff\u0001\u001b\u0001\u0004\u0001\uffff\u0001\u001a\u0002\u0004\u0004\uffff\u0002\u0004\u0001\uffff\u0001\u0014\u0001\u0004") + "\u0001\u0015\u0001\u0016\u0003\uffff\u0001\u001a\t\uffff\u0001\u0003\r\uffff\u0001\u001c"), "\u0001!\u001c\uffff\u0001 \b\uffff\u0001\u001f\u0006\uffff\u0001\u001eX\uffff\u0001\u001d\n\uffff" + "\u0005\u001d\u0001\"", "\u0001!\u001c\uffff\u0001 \b\uffff\u0001\u001f\u0006\uffff\u0001\u001eX\uffff\u0001\u001d\n\uffff" + "\u0005\u001d\u0001\"", "\u0001!\u001c\uffff\u0001 \b\uffff\u0001\u001f\u0006\uffff\u0001\u001eX\uffff\u0001\u001d\n\uffff" + "\u0005\u001d\u0001\"", "\u0001!\u001c\uffff\u0001 \b\uffff\u0001\u001f\u0006\uffff\u0001\u001eX\uffff\u0001\u001d\n\uffff" + "\u0005\u001d\u0001\"", "\u0001!\u001c\uffff\u0001 \b\uffff\u0001\u001f\u0006\uffff\u0001\u001eX\uffff\u0001\u001d\n\uffff" + "\u0005\u001d\u0001\"", "\u0001!\u001c\uffff\u0001 \b\uffff\u0001\u001f\u0006\uffff\u0001\u001eX\uffff\u0001\u001d\n\uffff" + "\u0005\u001d\u0001\"", "\u0001!\u001c\uffff\u0001 \b\uffff\u0001\u001f\u0006\uffff\u0001\u001eX\uffff\u0001\u001d\n\uffff" + "\u0005\u001d\u0001\"", "\u0001!\u001c\uffff\u0001 \b\uffff\u0001\u001f\u0006\uffff\u0001\u001eX\uffff\u0001\u001d\n\uffff" + "\u0005\u001d\u0001\"", "\u0001!\u001c\uffff\u0001 \b\uffff\u0001\u001f\u0006\uffff\u0001\u001eX\uffff\u0001\u001d\n\uffff" + "\u0005\u001d\u0001\"", "\u0001!\u001c\uffff\u0001 \b\uffff\u0001\u001f\u0006\uffff\u0001\u001eX\uffff\u0001\u001d\n\uffff" + "\u0005\u001d\u0001\"", "\u0001!\u001c\uffff\u0001 \b\uffff\u0001\u001f\u0006\uffff\u0001\u001eX\uffff\u0001\u001d\n\uffff" + "\u0005\u001d\u0001\"", "\u0001!\u001c\uffff\u0001 \b\uffff\u0001\u001f\u0006\uffff\u0001\u001eX\uffff\u0001\u001d\n\uffff" + "\u0005\u001d\u0001\"", "\u0001!\u001c\uffff\u0001 \b\uffff\u0001\u001f\u0006\uffff\u0001\u001eX\uffff\u0001\u001d\n\uffff" + "\u0005\u001d\u0001\"", "\u0001!\u001c\uffff\u0001 \b\uffff\u0001\u001f\u0006\uffff\u0001\u001eX\uffff\u0001\u001d\n\uffff" + "\u0005\u001d\u0001\"", "\u0001!\u001c\uffff\u0001 \b\uffff\u0001\u001f\u0006\uffff\u0001\u001eX\uffff\u0001\u001d\n\uffff" + "\u0005\u001d\u0001\"", "\u0001!\u001c\uffff\u0001 \b\uffff\u0001\u001f\u0006\uffff\u0001\u001eX\uffff\u0001\u001d\n\uffff" + "\u0005\u001d\u0001\"", "\u0001!\u001c\uffff\u0001 \b\uffff\u0001\u001f\u0006\uffff\u0001\u001eX\uffff\u0001\u001d\n\uffff" + "\u0005\u001d\u0001\"", "\u0001!\u001c\uffff\u0001 \b\uffff\u0001\u001f\u0006\uffff\u0001\u001eX\uffff\u0001\u001d\n\uffff" + "\u0005\u001d\u0001\"", "\u0001!\u001c\uffff\u0001 \b\uffff\u0001\u001f\u0006\uffff\u0001\u001eX\uffff\u0001\u001d\n\uffff" + "\u0005\u001d\u0001\"", "\u0001!\u001c\uffff\u0001 \b\uffff\u0001\u001f\u0006\uffff\u0001\u001eX\uffff\u0001\u001d\n\uffff" + "\u0005\u001d\u0001\"", "\u0001!\u001c\uffff\u0001 \b\uffff\u0001\u001f\u0006\uffff\u0001\u001eX\uffff\u0001\u001d\n\uffff" + "\u0005\u001d\u0001\"", "\u0001!\u001c\uffff\u0001 \b\uffff\u0001\u001f\u0006\uffff\u0001\u001eX\uffff\u0001\u001d\n\uffff" + "\u0005\u001d\u0001\"", "\u0001!\u001c\uffff\u0001 \b\uffff\u0001\u001f\u0006\uffff\u0001\u001eX\uffff\u0001\u001d\n\uffff" + "\u0005\u001d\u0001\"", "\u0001!\u001c\uffff\u0001 \b\uffff\u0001\u001f\u0006\uffff\u0001\u001eX\uffff\u0001\u001d\n\uffff" + "\u0005\u001d\u0001\"", "\u0001!\u001c\uffff\u0001 \b\uffff\u0001\u001f\u0006\uffff\u0001\u001eX\uffff\u0001\u001d\n\uffff" + "\u0005\u001d\u0001\"", "\u0001!\u001c\uffff\u0001 \b\uffff\u0001\u001f\u0006\uffff\u0001\u001eX\uffff\u0001\u001d\n\uffff" + "\u0005\u001d\u0001\"", "", "\u0001#\b\uffff\u0001$\u0004\uffff\u0002&\u0004\uffff\u0001&\u0001\uffff\u0001\'\u0003\uffff\u0001" + ((((((("(\u0001)\u0001*\u0001\uffff\u0001&\u0001<\u0001&\u0001\uffff\u0002&\u0001<\u0001+\u0001\uffff\u0001" + "&\u0001:\u0001,\u0004\uffff\u0001<\u0001-\u0001\uffff\u0001.\u0002\uffff\u0003&\u0001/\u0001\uffff") + "\u0001&\u0001\uffff\u0002&\u0001\uffff\u0001&\u0003\uffff\u00010\u0001\uffff\u0002&\u0001\uffff\u00011") + "\u0002\uffff\u0002<\u0001&\u0001\uffff\u0003&\u0001\uffff\u0003&\u0005\uffff\u0001&\u0001\uffff\u0001&") + "\u0004\uffff\u0001&\u0002\uffff\u0005&\u0003\uffff\u0001&\u0001\uffff\u0002&\u0002\uffff\u0001&\u00012") + "\u0004&\u00013\u0001;\u00014\u00019\u00015\u0001\uffff\u0001=\u0001&\u0001\uffff\u0001<\u0002&\u0004") + "\uffff\u0002&\u0001\uffff\u00016\u0001&\u00017\u00018\u0003\uffff\u0001<\t\uffff\u0001%\r") + "\uffff\u0001="), "", "", "", "\u0001>\u000e\uffff\u0001?\u0007\uffff\u0001>", "", "", "\u0001=\u001c\uffff\u0001=\b\uffff\u0001=\u0006\uffff\u0001=X\uffff\u0001=\u0001\uffff" + "\u0001@\u0002\uffff\u0001@\u0005\uffff\u0006=", "\u0001=\u001c\uffff\u0001=\b\uffff\u0001=\u0006\uffff\u0001=X\uffff\u0001=\u0001\uffff" + "\u0001@\u0002\uffff\u0001@\u0005\uffff\u0006=", "\u0001=\u001c\uffff\u0001=\b\uffff\u0001=\u0006\uffff\u0001=X\uffff\u0001=\u0001\uffff" + "\u0001@\u0002\uffff\u0001@\u0005\uffff\u0006=", "\u0001=\u001c\uffff\u0001=\b\uffff\u0001=\u0006\uffff\u0001=X\uffff\u0001=\u0001\uffff" + "\u0001@\u0002\uffff\u0001@\u0005\uffff\u0006=", "\u0001=\u001c\uffff\u0001=\b\uffff\u0001=\u0006\uffff\u0001=X\uffff\u0001=\u0001\uffff" + "\u0001@\u0002\uffff\u0001@\u0005\uffff\u0006=", "\u0001=\u001c\uffff\u0001=\b\uffff\u0001=\u0006\uffff\u0001=X\uffff\u0001=\u0001\uffff" + "\u0001@\u0002\uffff\u0001@\u0005\uffff\u0006=", "\u0001=\u001c\uffff\u0001=\b\uffff\u0001=\u0006\uffff\u0001=X\uffff\u0001=\u0001\uffff" + "\u0001@\u0002\uffff\u0001@\u0005\uffff\u0006=", "\u0001=\u001c\uffff\u0001=\b\uffff\u0001=\u0006\uffff\u0001=X\uffff\u0001=\u0001\uffff" + "\u0001@\u0002\uffff\u0001@\u0005\uffff\u0006=", "\u0001=\u001c\uffff\u0001=\b\uffff\u0001=\u0006\uffff\u0001=X\uffff\u0001=\u0001\uffff" + "\u0001@\u0002\uffff\u0001@\u0005\uffff\u0006=", "\u0001=\u001c\uffff\u0001=\b\uffff\u0001=\u0006\uffff\u0001=X\uffff\u0001=\u0001\uffff" + "\u0001@\u0002\uffff\u0001@\u0005\uffff\u0006=", "\u0001=\u001c\uffff\u0001=\b\uffff\u0001=\u0006\uffff\u0001=X\uffff\u0001=\u0001\uffff" + "\u0001@\u0002\uffff\u0001@\u0005\uffff\u0006=", "\u0001=\u001c\uffff\u0001=\b\uffff\u0001=\u0006\uffff\u0001=X\uffff\u0001=\u0001\uffff" + "\u0001@\u0002\uffff\u0001@\u0005\uffff\u0006=", "\u0001=\u001c\uffff\u0001=\b\uffff\u0001=\u0006\uffff\u0001=X\uffff\u0001=\u0001\uffff" + "\u0001@\u0002\uffff\u0001@\u0005\uffff\u0006=", "\u0001=\u001c\uffff\u0001=\b\uffff\u0001=\u0006\uffff\u0001=X\uffff\u0001=\u0001\uffff" + "\u0001@\u0002\uffff\u0001@\u0005\uffff\u0006=", "\u0001=\u001c\uffff\u0001=\b\uffff\u0001=\u0006\uffff\u0001=X\uffff\u0001=\u0001\uffff" + "\u0001@\u0002\uffff\u0001@\u0005\uffff\u0006=", "\u0001=\u001c\uffff\u0001=\b\uffff\u0001=\u0006\uffff\u0001=X\uffff\u0001=\u0001\uffff" + "\u0001@\u0002\uffff\u0001@\u0005\uffff\u0006=", "\u0001=\u001c\uffff\u0001=\b\uffff\u0001=\u0006\uffff\u0001=X\uffff\u0001=\u0001\uffff" + "\u0001@\u0002\uffff\u0001@\u0005\uffff\u0006=", "\u0001=\u001c\uffff\u0001=\b\uffff\u0001=\u0006\uffff\u0001=X\uffff\u0001=\u0001\uffff" + "\u0001@\u0002\uffff\u0001@\u0005\uffff\u0006=", "\u0001=\u001c\uffff\u0001=\b\uffff\u0001=\u0006\uffff\u0001=X\uffff\u0001=\u0001\uffff" + "\u0001@\u0002\uffff\u0001@\u0005\uffff\u0006=", "\u0001=\u001c\uffff\u0001=\b\uffff\u0001=\u0006\uffff\u0001=X\uffff\u0001=\u0001\uffff" + "\u0001@\u0002\uffff\u0001@\u0005\uffff\u0006=", "\u0001=\u001c\uffff\u0001=\b\uffff\u0001=\u0006\uffff\u0001=X\uffff\u0001=\u0001\uffff" + "\u0001@\u0002\uffff\u0001@\u0005\uffff\u0006=", "\u0001=\u001c\uffff\u0001=\b\uffff\u0001=\u0006\uffff\u0001=X\uffff\u0001=\u0001\uffff" + "\u0001@\u0002\uffff\u0001@\u0005\uffff\u0006=", "\u0001=\u001c\uffff\u0001=\b\uffff\u0001=\u0006\uffff\u0001=X\uffff\u0001=\u0001\uffff" + "\u0001@\u0002\uffff\u0001@\u0005\uffff\u0006=", "\u0001=\u001c\uffff\u0001=\b\uffff\u0001=\u0006\uffff\u0001=X\uffff\u0001=\u0001\uffff" + "\u0001@\u0002\uffff\u0001@\u0005\uffff\u0006=", "\u0001=\u001c\uffff\u0001=\b\uffff\u0001=\u0006\uffff\u0001=X\uffff\u0001=\u0001\uffff" + "\u0001@\u0002\uffff\u0001@\u0005\uffff\u0006=", "\u0001=\u001c\uffff\u0001=\b\uffff\u0001=\u0006\uffff\u0001=X\uffff\u0001=\u0001\uffff" + "\u0001@\u0002\uffff\u0001@\u0005\uffff\u0006=", "", "", "", "" };

	static final short[] DFA196_eot = DFA.unpackEncodedString(Cql_Parser.DFA196_eotS);

	static final short[] DFA196_eof = DFA.unpackEncodedString(Cql_Parser.DFA196_eofS);

	static final char[] DFA196_min = DFA.unpackEncodedStringToUnsignedChars(Cql_Parser.DFA196_minS);

	static final char[] DFA196_max = DFA.unpackEncodedStringToUnsignedChars(Cql_Parser.DFA196_maxS);

	static final short[] DFA196_accept = DFA.unpackEncodedString(Cql_Parser.DFA196_acceptS);

	static final short[] DFA196_special = DFA.unpackEncodedString(Cql_Parser.DFA196_specialS);

	static final short[][] DFA196_transition;

	static {
		int numStates = Cql_Parser.DFA196_transitionS.length;
		DFA196_transition = new short[numStates][];
		for (int i = 0; i < numStates; i++) {
			Cql_Parser.DFA196_transition[i] = DFA.unpackEncodedString(Cql_Parser.DFA196_transitionS[i]);
		}
	}

	protected class DFA196 extends DFA {
		public DFA196(BaseRecognizer recognizer) {
			this.recognizer = recognizer;
			this.decisionNumber = 196;
			this.eot = Cql_Parser.DFA196_eot;
			this.eof = Cql_Parser.DFA196_eof;
			this.min = Cql_Parser.DFA196_min;
			this.max = Cql_Parser.DFA196_max;
			this.accept = Cql_Parser.DFA196_accept;
			this.special = Cql_Parser.DFA196_special;
			this.transition = Cql_Parser.DFA196_transition;
		}

		@Override
		public String getDescription() {
			return "1472:1: relation[WhereClause.Builder clauses] : (name= cident type= relationType t= term |name= cident K_LIKE t= term |name= cident K_IS K_NOT K_NULL | K_TOKEN l= tupleOfIdentifiers type= relationType t= term |name= cident K_IN marker= inMarker |name= cident K_IN inValues= singleColumnInValues |name= cident K_CONTAINS ( K_KEY )? t= term |name= cident '[' key= term ']' type= relationType t= term |ids= tupleOfIdentifiers ( K_IN ( '(' ')' |tupleInMarker= inMarkerForTuple |literals= tupleOfTupleLiterals |markers= tupleOfMarkersForTuples ) |type= relationType literal= tupleLiteral |type= relationType tupleMarker= markerForTuple ) | '(' relation[$clauses] ')' );";
		}
	}

	static final String DFA195_eotS = "\n\uffff";

	static final String DFA195_eofS = "\n\uffff";

	static final String DFA195_minS = "\u0001N\u0001\uffff\u0006\u00a9\u0002\uffff";

	static final String DFA195_maxS = "\u0001\u00c6\u0001\uffff\u0006\u00c0\u0002\uffff";

	static final String DFA195_acceptS = "\u0001\uffff\u0001\u0001\u0006\uffff\u0001\u0002\u0001\u0003";

	static final String DFA195_specialS = "\n\uffff}>";

	static final String[] DFA195_transitionS = new String[]{ "\u0001\u0001h\uffff\u0001\u0007\n\uffff\u0001\u0003\u0001\u0004\u0001\u0002\u0001\u0005\u0001\u0006", "", "\u0001\t\u000e\uffff\u0001\b\u0007\uffff\u0001\t", "\u0001\t\u000e\uffff\u0001\b\u0007\uffff\u0001\t", "\u0001\t\u000e\uffff\u0001\b\u0007\uffff\u0001\t", "\u0001\t\u000e\uffff\u0001\b\u0007\uffff\u0001\t", "\u0001\t\u000e\uffff\u0001\b\u0007\uffff\u0001\t", "\u0001\t\u000e\uffff\u0001\b\u0007\uffff\u0001\t", "", "" };

	static final short[] DFA195_eot = DFA.unpackEncodedString(Cql_Parser.DFA195_eotS);

	static final short[] DFA195_eof = DFA.unpackEncodedString(Cql_Parser.DFA195_eofS);

	static final char[] DFA195_min = DFA.unpackEncodedStringToUnsignedChars(Cql_Parser.DFA195_minS);

	static final char[] DFA195_max = DFA.unpackEncodedStringToUnsignedChars(Cql_Parser.DFA195_maxS);

	static final short[] DFA195_accept = DFA.unpackEncodedString(Cql_Parser.DFA195_acceptS);

	static final short[] DFA195_special = DFA.unpackEncodedString(Cql_Parser.DFA195_specialS);

	static final short[][] DFA195_transition;

	static {
		int numStates = Cql_Parser.DFA195_transitionS.length;
		DFA195_transition = new short[numStates][];
		for (int i = 0; i < numStates; i++) {
			Cql_Parser.DFA195_transition[i] = DFA.unpackEncodedString(Cql_Parser.DFA195_transitionS[i]);
		}
	}

	protected class DFA195 extends DFA {
		public DFA195(BaseRecognizer recognizer) {
			this.recognizer = recognizer;
			this.decisionNumber = 195;
			this.eot = Cql_Parser.DFA195_eot;
			this.eof = Cql_Parser.DFA195_eof;
			this.min = Cql_Parser.DFA195_min;
			this.max = Cql_Parser.DFA195_max;
			this.accept = Cql_Parser.DFA195_accept;
			this.special = Cql_Parser.DFA195_special;
			this.transition = Cql_Parser.DFA195_transition;
		}

		@Override
		public String getDescription() {
			return "1486:7: ( K_IN ( '(' ')' |tupleInMarker= inMarkerForTuple |literals= tupleOfTupleLiterals |markers= tupleOfMarkersForTuples ) |type= relationType literal= tupleLiteral |type= relationType tupleMarker= markerForTuple )";
		}
	}

	static final String DFA205_eotS = " \uffff";

	static final String DFA205_eofS = "\u0001\uffff\u0015\u001d\u0002\u001a\u0001\uffff\u0001\u001a\u0001\uffff\u0001\u001a\u0004\uffff";

	static final String DFA205_minS = "\u0001\u0017\u0017D\u0001\uffff\u0001D\u0001\uffff\u0001D\u0004\uffff";

	static final String DFA205_maxS = "\u0001\u00ad\u0017\u00c5\u0001\uffff\u0001\u00c5\u0001\uffff\u0001\u00c5\u0004\uffff";

	static final String DFA205_acceptS = "\u0018\uffff\u0001\u0002\u0001\uffff\u0001\u0004\u0001\uffff\u0001\u0006\u0001\u0001\u0001\u0003\u0001\u0005";

	static final String DFA205_specialS = " \uffff}>";

	static final String[] DFA205_transitionS = new String[]{ "\u0001\u001a\u0004\uffff\u0002\u001a\u0004\uffff\u0001\u001a\u0001\uffff\u0001\u0001\u0003\uffff\u0001\u0002\u0001\u0003\u0001\u0004\u0001\uffff" + (((((("\u0003\u001a\u0001\uffff\u0003\u001a\u0001\u0005\u0001\uffff\u0001\u001a\u0001\u0014\u0001\u0006\u0004\uffff\u0001\u001a\u0001\u0007\u0001\uffff" + "\u0001\b\u0002\uffff\u0003\u001a\u0001\t\u0001\uffff\u0001\u001b\u0001\uffff\u0002\u001a\u0001\uffff\u0001\u001a\u0003\uffff") + "\u0001\n\u0001\uffff\u0002\u001a\u0001\uffff\u0001\u000b\u0002\uffff\u0003\u001a\u0001\uffff\u0003\u001a\u0001\uffff\u0001\u0017") + "\u0001\u001a\u0001\u0016\u0005\uffff\u0001\u001a\u0001\uffff\u0001\u001a\u0004\uffff\u0001\u001a\u0002\uffff\u0005\u001a\u0003\uffff") + "\u0001\u001a\u0001\uffff\u0002\u001a\u0001\uffff\u0001\u0018\u0001\u001a\u0001\f\u0004\u001a\u0001\r\u0001\u0015\u0001\u000e\u0001\u0013\u0001") + "\u000f\u0002\uffff\u0001\u001a\u0001\uffff\u0001\u001a\u0001\u0019\u0001\u001a\u0004\uffff\u0002\u001a\u0001\uffff\u0001\u0010\u0001\u001a") + "\u0001\u0011\u0001\u0012\u0003\uffff\u0001\u001a\t\uffff\u0001\u001a\u0002\uffff\u0001\u001c"), "\u0001\u001d\r\uffff\u0001\u001d\n\uffff\u0001\u001d\u0019\uffff\u0001\u001d\n\uffff\u0001\u001d6\uffff" + "\u0001\u001d\u0002\uffff\u0001\u001d\u0002\uffff\u0001\u001a\u0001\uffff\u0001\u001d\u0003\uffff\u0001\u001d", "\u0001\u001d\r\uffff\u0001\u001d\n\uffff\u0001\u001d\u0019\uffff\u0001\u001d\n\uffff\u0001\u001d6\uffff" + "\u0001\u001d\u0002\uffff\u0001\u001d\u0002\uffff\u0001\u001a\u0001\uffff\u0001\u001d\u0003\uffff\u0001\u001d", "\u0001\u001d\r\uffff\u0001\u001d\n\uffff\u0001\u001d\u0019\uffff\u0001\u001d\n\uffff\u0001\u001d6\uffff" + "\u0001\u001d\u0002\uffff\u0001\u001d\u0002\uffff\u0001\u001a\u0001\uffff\u0001\u001d\u0003\uffff\u0001\u001d", "\u0001\u001d\r\uffff\u0001\u001d\n\uffff\u0001\u001d\u0019\uffff\u0001\u001d\n\uffff\u0001\u001d6\uffff" + "\u0001\u001d\u0002\uffff\u0001\u001d\u0002\uffff\u0001\u001a\u0001\uffff\u0001\u001d\u0003\uffff\u0001\u001d", "\u0001\u001d\r\uffff\u0001\u001d\n\uffff\u0001\u001d\u0019\uffff\u0001\u001d\n\uffff\u0001\u001d6\uffff" + "\u0001\u001d\u0002\uffff\u0001\u001d\u0002\uffff\u0001\u001a\u0001\uffff\u0001\u001d\u0003\uffff\u0001\u001d", "\u0001\u001d\r\uffff\u0001\u001d\n\uffff\u0001\u001d\u0019\uffff\u0001\u001d\n\uffff\u0001\u001d6\uffff" + "\u0001\u001d\u0002\uffff\u0001\u001d\u0002\uffff\u0001\u001a\u0001\uffff\u0001\u001d\u0003\uffff\u0001\u001d", "\u0001\u001d\r\uffff\u0001\u001d\n\uffff\u0001\u001d\u0019\uffff\u0001\u001d\n\uffff\u0001\u001d6\uffff" + "\u0001\u001d\u0002\uffff\u0001\u001d\u0002\uffff\u0001\u001a\u0001\uffff\u0001\u001d\u0003\uffff\u0001\u001d", "\u0001\u001d\r\uffff\u0001\u001d\n\uffff\u0001\u001d\u0019\uffff\u0001\u001d\n\uffff\u0001\u001d6\uffff" + "\u0001\u001d\u0002\uffff\u0001\u001d\u0002\uffff\u0001\u001a\u0001\uffff\u0001\u001d\u0003\uffff\u0001\u001d", "\u0001\u001d\r\uffff\u0001\u001d\n\uffff\u0001\u001d\u0019\uffff\u0001\u001d\n\uffff\u0001\u001d6\uffff" + "\u0001\u001d\u0002\uffff\u0001\u001d\u0002\uffff\u0001\u001a\u0001\uffff\u0001\u001d\u0003\uffff\u0001\u001d", "\u0001\u001d\r\uffff\u0001\u001d\n\uffff\u0001\u001d\u0019\uffff\u0001\u001d\n\uffff\u0001\u001d6\uffff" + "\u0001\u001d\u0002\uffff\u0001\u001d\u0002\uffff\u0001\u001a\u0001\uffff\u0001\u001d\u0003\uffff\u0001\u001d", "\u0001\u001d\r\uffff\u0001\u001d\n\uffff\u0001\u001d\u0019\uffff\u0001\u001d\n\uffff\u0001\u001d6\uffff" + "\u0001\u001d\u0002\uffff\u0001\u001d\u0002\uffff\u0001\u001a\u0001\uffff\u0001\u001d\u0003\uffff\u0001\u001d", "\u0001\u001d\r\uffff\u0001\u001d\n\uffff\u0001\u001d\u0019\uffff\u0001\u001d\n\uffff\u0001\u001d6\uffff" + "\u0001\u001d\u0002\uffff\u0001\u001d\u0002\uffff\u0001\u001a\u0001\uffff\u0001\u001d\u0003\uffff\u0001\u001d", "\u0001\u001d\r\uffff\u0001\u001d\n\uffff\u0001\u001d\u0019\uffff\u0001\u001d\n\uffff\u0001\u001d6\uffff" + "\u0001\u001d\u0002\uffff\u0001\u001d\u0002\uffff\u0001\u001a\u0001\uffff\u0001\u001d\u0003\uffff\u0001\u001d", "\u0001\u001d\r\uffff\u0001\u001d\n\uffff\u0001\u001d\u0019\uffff\u0001\u001d\n\uffff\u0001\u001d6\uffff" + "\u0001\u001d\u0002\uffff\u0001\u001d\u0002\uffff\u0001\u001a\u0001\uffff\u0001\u001d\u0003\uffff\u0001\u001d", "\u0001\u001d\r\uffff\u0001\u001d\n\uffff\u0001\u001d\u0019\uffff\u0001\u001d\n\uffff\u0001\u001d6\uffff" + "\u0001\u001d\u0002\uffff\u0001\u001d\u0002\uffff\u0001\u001a\u0001\uffff\u0001\u001d\u0003\uffff\u0001\u001d", "\u0001\u001d\r\uffff\u0001\u001d\n\uffff\u0001\u001d\u0019\uffff\u0001\u001d\n\uffff\u0001\u001d6\uffff" + "\u0001\u001d\u0002\uffff\u0001\u001d\u0002\uffff\u0001\u001a\u0001\uffff\u0001\u001d\u0003\uffff\u0001\u001d", "\u0001\u001d\r\uffff\u0001\u001d\n\uffff\u0001\u001d\u0019\uffff\u0001\u001d\n\uffff\u0001\u001d6\uffff" + "\u0001\u001d\u0002\uffff\u0001\u001d\u0002\uffff\u0001\u001a\u0001\uffff\u0001\u001d\u0003\uffff\u0001\u001d", "\u0001\u001d\r\uffff\u0001\u001d\n\uffff\u0001\u001d\u0019\uffff\u0001\u001d\n\uffff\u0001\u001d6\uffff" + "\u0001\u001d\u0002\uffff\u0001\u001d\u0002\uffff\u0001\u001a\u0001\uffff\u0001\u001d\u0003\uffff\u0001\u001d", "\u0001\u001d\r\uffff\u0001\u001d\n\uffff\u0001\u001d\u0019\uffff\u0001\u001d\n\uffff\u0001\u001d6\uffff" + "\u0001\u001d\u0002\uffff\u0001\u001d\u0002\uffff\u0001\u001a\u0001\uffff\u0001\u001d\u0003\uffff\u0001\u001d", "\u0001\u001d\r\uffff\u0001\u001d\n\uffff\u0001\u001d\u0019\uffff\u0001\u001d\n\uffff\u0001\u001d6\uffff" + "\u0001\u001d\u0002\uffff\u0001\u001d\u0002\uffff\u0001\u001a\u0001\uffff\u0001\u001d\u0003\uffff\u0001\u001d", "\u0001\u001d\r\uffff\u0001\u001d\n\uffff\u0001\u001d\u0019\uffff\u0001\u001d\n\uffff\u0001\u001d6\uffff" + "\u0001\u001d\u0002\uffff\u0001\u001d\u0002\uffff\u0001\u001a\u0001\uffff\u0001\u001d\u0003\uffff\u0001\u001d", "\u0001\u001a\r\uffff\u0001\u001a\n\uffff\u0001\u001a\u0019\uffff\u0001\u001a\n\uffff\u0001\u001a6\uffff" + "\u0001\u001a\u0002\uffff\u0001\u001a\u0002\uffff\u0001\u001a\u0001\uffff\u0001\u001a\u0001\u0018\u0002\uffff\u0001\u001a", "\u0001\u001a\r\uffff\u0001\u001a\n\uffff\u0001\u001a\u0019\uffff\u0001\u001a\n\uffff\u0001\u001a6\uffff" + "\u0001\u001a\u0002\uffff\u0001\u001a\u0002\uffff\u0001\u001a\u0001\uffff\u0001\u001a\u0001\u0018\u0002\uffff\u0001\u001a", "", "\u0001\u001a\r\uffff\u0001\u001a\n\uffff\u0001\u001a\u0019\uffff\u0001\u001a\n\uffff\u0001\u001a6\uffff" + "\u0001\u001a\u0002\uffff\u0001\u001a\u0002\uffff\u0001\u001a\u0001\uffff\u0001\u001a\u0001\u001e\u0002\uffff\u0001\u001a", "", "\u0001\u001a\r\uffff\u0001\u001a\n\uffff\u0001\u001a\u0019\uffff\u0001\u001a\n\uffff\u0001\u001a6\uffff" + "\u0001\u001a\u0002\uffff\u0001\u001a\u0002\uffff\u0001\u001a\u0001\uffff\u0001\u001a\u0001\u001f\u0002\uffff\u0001\u001a", "", "", "", "" };

	static final short[] DFA205_eot = DFA.unpackEncodedString(Cql_Parser.DFA205_eotS);

	static final short[] DFA205_eof = DFA.unpackEncodedString(Cql_Parser.DFA205_eofS);

	static final char[] DFA205_min = DFA.unpackEncodedStringToUnsignedChars(Cql_Parser.DFA205_minS);

	static final char[] DFA205_max = DFA.unpackEncodedStringToUnsignedChars(Cql_Parser.DFA205_maxS);

	static final short[] DFA205_accept = DFA.unpackEncodedString(Cql_Parser.DFA205_acceptS);

	static final short[] DFA205_special = DFA.unpackEncodedString(Cql_Parser.DFA205_specialS);

	static final short[][] DFA205_transition;

	static {
		int numStates = Cql_Parser.DFA205_transitionS.length;
		DFA205_transition = new short[numStates][];
		for (int i = 0; i < numStates; i++) {
			Cql_Parser.DFA205_transition[i] = DFA.unpackEncodedString(Cql_Parser.DFA205_transitionS[i]);
		}
	}

	protected class DFA205 extends DFA {
		public DFA205(BaseRecognizer recognizer) {
			this.recognizer = recognizer;
			this.decisionNumber = 205;
			this.eot = Cql_Parser.DFA205_eot;
			this.eof = Cql_Parser.DFA205_eof;
			this.min = Cql_Parser.DFA205_min;
			this.max = Cql_Parser.DFA205_max;
			this.accept = Cql_Parser.DFA205_accept;
			this.special = Cql_Parser.DFA205_special;
			this.transition = Cql_Parser.DFA205_transition;
		}

		@Override
		public String getDescription() {
			return "1543:1: comparatorType returns [CQL3Type.Raw t] : (n= native_type |c= collection_type |tt= tuple_type |id= userTypeName | K_FROZEN '<' f= comparatorType '>' |s= STRING_LITERAL );";
		}
	}

	public static final BitSet FOLLOW_selectStatement_in_cqlStatement59 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_insertStatement_in_cqlStatement88 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_updateStatement_in_cqlStatement117 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_batchStatement_in_cqlStatement146 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_deleteStatement_in_cqlStatement176 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_useStatement_in_cqlStatement205 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_truncateStatement_in_cqlStatement237 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_createKeyspaceStatement_in_cqlStatement264 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_createTableStatement_in_cqlStatement285 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_createIndexStatement_in_cqlStatement308 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_dropKeyspaceStatement_in_cqlStatement331 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_dropTableStatement_in_cqlStatement353 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_dropIndexStatement_in_cqlStatement378 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_alterTableStatement_in_cqlStatement403 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_alterKeyspaceStatement_in_cqlStatement427 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_grantPermissionsStatement_in_cqlStatement448 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_revokePermissionsStatement_in_cqlStatement466 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_listPermissionsStatement_in_cqlStatement483 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_createUserStatement_in_cqlStatement502 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_alterUserStatement_in_cqlStatement526 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_dropUserStatement_in_cqlStatement551 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_listUsersStatement_in_cqlStatement577 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_createTriggerStatement_in_cqlStatement602 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_dropTriggerStatement_in_cqlStatement623 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_createTypeStatement_in_cqlStatement646 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_alterTypeStatement_in_cqlStatement670 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_dropTypeStatement_in_cqlStatement695 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_createFunctionStatement_in_cqlStatement721 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_dropFunctionStatement_in_cqlStatement741 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_createAggregateStatement_in_cqlStatement763 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_dropAggregateStatement_in_cqlStatement782 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_createRoleStatement_in_cqlStatement803 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_alterRoleStatement_in_cqlStatement827 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_dropRoleStatement_in_cqlStatement852 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_listRolesStatement_in_cqlStatement878 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_grantRoleStatement_in_cqlStatement903 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_revokeRoleStatement_in_cqlStatement928 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_createMaterializedViewStatement_in_cqlStatement952 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_dropMaterializedViewStatement_in_cqlStatement964 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_alterMaterializedViewStatement_in_cqlStatement978 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_K_USE_in_useStatement1004 = new BitSet(new long[]{ -5697204075003641856L, 3782044054195082940L, 6601880872959L });

	public static final BitSet FOLLOW_keyspaceName_in_useStatement1008 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_K_SELECT_in_selectStatement1042 = new BitSet(new long[]{ -5697204074984617920L, 3782062196137072316L, 2378223864481298431L, 2433L });

	public static final BitSet FOLLOW_K_JSON_in_selectStatement1052 = new BitSet(new long[]{ -5697204074984617920L, 3782062196137072316L, 2378223864481298431L, 2433L });

	public static final BitSet FOLLOW_K_DISTINCT_in_selectStatement1069 = new BitSet(new long[]{ -5697204074984617920L, 3782062196137072316L, 2378223864481298431L, 2433L });

	public static final BitSet FOLLOW_selectClause_in_selectStatement1078 = new BitSet(new long[]{ 0L, 64L });

	public static final BitSet FOLLOW_K_FROM_in_selectStatement1088 = new BitSet(new long[]{ -5697204075003641856L, 3782044054195082940L, 6601880872959L });

	public static final BitSet FOLLOW_columnFamilyName_in_selectStatement1092 = new BitSet(new long[]{ 1073741826L, 5066551728279552L, 1073741824L });

	public static final BitSet FOLLOW_K_WHERE_in_selectStatement1102 = new BitSet(new long[]{ -5697204075003625472L, 3782044054195082940L, 72061996895549439L, 1024L });

	public static final BitSet FOLLOW_whereClause_in_selectStatement1106 = new BitSet(new long[]{ 1073741826L, 5066551728279552L });

	public static final BitSet FOLLOW_K_GROUP_in_selectStatement1119 = new BitSet(new long[]{ 8796093022208L });

	public static final BitSet FOLLOW_K_BY_in_selectStatement1121 = new BitSet(new long[]{ -5697204075003625472L, 3782044054195082940L, 4402857617407L });

	public static final BitSet FOLLOW_groupByClause_in_selectStatement1123 = new BitSet(new long[]{ 1073741826L, 5066551728275456L, 1152921504606846976L });

	public static final BitSet FOLLOW_188_in_selectStatement1128 = new BitSet(new long[]{ -5697204075003625472L, 3782044054195082940L, 4402857617407L });

	public static final BitSet FOLLOW_groupByClause_in_selectStatement1130 = new BitSet(new long[]{ 1073741826L, 5066551728275456L, 1152921504606846976L });

	public static final BitSet FOLLOW_K_ORDER_in_selectStatement1147 = new BitSet(new long[]{ 8796093022208L });

	public static final BitSet FOLLOW_K_BY_in_selectStatement1149 = new BitSet(new long[]{ -5697204075003625472L, 3782044054195082940L, 4402857617407L });

	public static final BitSet FOLLOW_orderByClause_in_selectStatement1151 = new BitSet(new long[]{ 1073741826L, 4503601774854144L, 1152921504606846976L });

	public static final BitSet FOLLOW_188_in_selectStatement1156 = new BitSet(new long[]{ -5697204075003625472L, 3782044054195082940L, 4402857617407L });

	public static final BitSet FOLLOW_orderByClause_in_selectStatement1158 = new BitSet(new long[]{ 1073741826L, 4503601774854144L, 1152921504606846976L });

	public static final BitSet FOLLOW_K_PER_in_selectStatement1175 = new BitSet(new long[]{ 0L, 1125899906842624L });

	public static final BitSet FOLLOW_K_PARTITION_in_selectStatement1177 = new BitSet(new long[]{ 0L, 2147483648L });

	public static final BitSet FOLLOW_K_LIMIT_in_selectStatement1179 = new BitSet(new long[]{ 16777216L, 0L, 2199023255552L, 1L });

	public static final BitSet FOLLOW_intValue_in_selectStatement1183 = new BitSet(new long[]{ 1073741826L, 2147483648L });

	public static final BitSet FOLLOW_K_LIMIT_in_selectStatement1198 = new BitSet(new long[]{ 16777216L, 0L, 2199023255552L, 1L });

	public static final BitSet FOLLOW_intValue_in_selectStatement1202 = new BitSet(new long[]{ 1073741826L });

	public static final BitSet FOLLOW_K_ALLOW_in_selectStatement1217 = new BitSet(new long[]{ 0L, 8L });

	public static final BitSet FOLLOW_K_FILTERING_in_selectStatement1219 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_selector_in_selectClause1256 = new BitSet(new long[]{ 2L, 0L, 1152921504606846976L });

	public static final BitSet FOLLOW_188_in_selectClause1261 = new BitSet(new long[]{ -5697204074984617920L, 3782062196137072316L, 2378223864481298431L, 2177L });

	public static final BitSet FOLLOW_selector_in_selectClause1265 = new BitSet(new long[]{ 2L, 0L, 1152921504606846976L });

	public static final BitSet FOLLOW_200_in_selectClause1277 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_unaliasedSelector_in_selector1310 = new BitSet(new long[]{ 17179869186L });

	public static final BitSet FOLLOW_K_AS_in_selector1313 = new BitSet(new long[]{ -5697204075003641856L, 3782044054195082940L, 4402857617407L });

	public static final BitSet FOLLOW_noncol_ident_in_selector1317 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_cident_in_unaliasedSelector1360 = new BitSet(new long[]{ 2L, 0L, -9223372036854775808L });

	public static final BitSet FOLLOW_value_in_unaliasedSelector1408 = new BitSet(new long[]{ 2L, 0L, -9223372036854775808L });

	public static final BitSet FOLLOW_184_in_unaliasedSelector1455 = new BitSet(new long[]{ -5697204075003641856L, -5441327982659692868L, 39587229706239L });

	public static final BitSet FOLLOW_comparatorType_in_unaliasedSelector1459 = new BitSet(new long[]{ 0L, 0L, 144115188075855872L });

	public static final BitSet FOLLOW_185_in_unaliasedSelector1461 = new BitSet(new long[]{ 19007552L, 18141941989376L, 2378219461623676928L, 2177L });

	public static final BitSet FOLLOW_value_in_unaliasedSelector1465 = new BitSet(new long[]{ 2L, 0L, -9223372036854775808L });

	public static final BitSet FOLLOW_K_COUNT_in_unaliasedSelector1486 = new BitSet(new long[]{ 0L, 0L, 72057594037927936L });

	public static final BitSet FOLLOW_184_in_unaliasedSelector1488 = new BitSet(new long[]{ 0L, 0L, 0L, 256L });

	public static final BitSet FOLLOW_200_in_unaliasedSelector1490 = new BitSet(new long[]{ 0L, 0L, 144115188075855872L });

	public static final BitSet FOLLOW_185_in_unaliasedSelector1492 = new BitSet(new long[]{ 2L, 0L, -9223372036854775808L });

	public static final BitSet FOLLOW_K_WRITETIME_in_unaliasedSelector1526 = new BitSet(new long[]{ 0L, 0L, 72057594037927936L });

	public static final BitSet FOLLOW_184_in_unaliasedSelector1528 = new BitSet(new long[]{ -5697204075003625472L, 3782044054195082940L, 4402857617407L });

	public static final BitSet FOLLOW_cident_in_unaliasedSelector1532 = new BitSet(new long[]{ 0L, 0L, 144115188075855872L });

	public static final BitSet FOLLOW_185_in_unaliasedSelector1534 = new BitSet(new long[]{ 2L, 0L, -9223372036854775808L });

	public static final BitSet FOLLOW_K_TTL_in_unaliasedSelector1560 = new BitSet(new long[]{ 0L, 0L, 72057594037927936L });

	public static final BitSet FOLLOW_184_in_unaliasedSelector1568 = new BitSet(new long[]{ -5697204075003625472L, 3782044054195082940L, 4402857617407L });

	public static final BitSet FOLLOW_cident_in_unaliasedSelector1572 = new BitSet(new long[]{ 0L, 0L, 144115188075855872L });

	public static final BitSet FOLLOW_185_in_unaliasedSelector1574 = new BitSet(new long[]{ 2L, 0L, -9223372036854775808L });

	public static final BitSet FOLLOW_K_CAST_in_unaliasedSelector1600 = new BitSet(new long[]{ 0L, 0L, 72057594037927936L });

	public static final BitSet FOLLOW_184_in_unaliasedSelector1607 = new BitSet(new long[]{ -5697204074984617920L, 3782062196137072316L, 2378223864481298431L, 2177L });

	public static final BitSet FOLLOW_unaliasedSelector_in_unaliasedSelector1611 = new BitSet(new long[]{ 17179869184L });

	public static final BitSet FOLLOW_K_AS_in_unaliasedSelector1613 = new BitSet(new long[]{ -6861226266998079488L, 2162720L, 436209602L });

	public static final BitSet FOLLOW_native_type_in_unaliasedSelector1617 = new BitSet(new long[]{ 0L, 0L, 144115188075855872L });

	public static final BitSet FOLLOW_185_in_unaliasedSelector1619 = new BitSet(new long[]{ 2L, 0L, -9223372036854775808L });

	public static final BitSet FOLLOW_functionName_in_unaliasedSelector1634 = new BitSet(new long[]{ 0L, 0L, 72057594037927936L });

	public static final BitSet FOLLOW_selectionFunctionArgs_in_unaliasedSelector1638 = new BitSet(new long[]{ 2L, 0L, -9223372036854775808L });

	public static final BitSet FOLLOW_191_in_unaliasedSelector1653 = new BitSet(new long[]{ -5697204075003641856L, 3782044054195082940L, 4402857617407L });

	public static final BitSet FOLLOW_fident_in_unaliasedSelector1657 = new BitSet(new long[]{ 2L, 0L, -9223372036854775808L });

	public static final BitSet FOLLOW_184_in_selectionFunctionArgs1685 = new BitSet(new long[]{ 0L, 0L, 144115188075855872L });

	public static final BitSet FOLLOW_185_in_selectionFunctionArgs1687 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_184_in_selectionFunctionArgs1697 = new BitSet(new long[]{ -5697204074984617920L, 3782062196137072316L, 2378223864481298431L, 2177L });

	public static final BitSet FOLLOW_unaliasedSelector_in_selectionFunctionArgs1701 = new BitSet(new long[]{ 0L, 0L, 1297036692682702848L });

	public static final BitSet FOLLOW_188_in_selectionFunctionArgs1717 = new BitSet(new long[]{ -5697204074984617920L, 3782062196137072316L, 2378223864481298431L, 2177L });

	public static final BitSet FOLLOW_unaliasedSelector_in_selectionFunctionArgs1721 = new BitSet(new long[]{ 0L, 0L, 1297036692682702848L });

	public static final BitSet FOLLOW_185_in_selectionFunctionArgs1734 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_relationOrExpression_in_whereClause1765 = new BitSet(new long[]{ 4294967298L });

	public static final BitSet FOLLOW_K_AND_in_whereClause1769 = new BitSet(new long[]{ -5697204075003625472L, 3782044054195082940L, 72061996895549439L, 1024L });

	public static final BitSet FOLLOW_relationOrExpression_in_whereClause1771 = new BitSet(new long[]{ 4294967298L });

	public static final BitSet FOLLOW_relation_in_relationOrExpression1793 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_customIndexExpression_in_relationOrExpression1802 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_202_in_customIndexExpression1830 = new BitSet(new long[]{ -5697204075003641856L, 3782044054195082940L, 6601880872959L });

	public static final BitSet FOLLOW_idxName_in_customIndexExpression1832 = new BitSet(new long[]{ 0L, 0L, 1152921504606846976L });

	public static final BitSet FOLLOW_188_in_customIndexExpression1835 = new BitSet(new long[]{ -5697204074984634304L, 3782062196137072316L, 2378223864481298431L, 2177L });

	public static final BitSet FOLLOW_term_in_customIndexExpression1839 = new BitSet(new long[]{ 0L, 0L, 144115188075855872L });

	public static final BitSet FOLLOW_185_in_customIndexExpression1841 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_cident_in_orderByClause1871 = new BitSet(new long[]{ 288230410511450114L });

	public static final BitSet FOLLOW_K_ASC_in_orderByClause1874 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_K_DESC_in_orderByClause1878 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_cident_in_groupByClause1904 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_K_INSERT_in_insertStatement1929 = new BitSet(new long[]{ 0L, 4194304L });

	public static final BitSet FOLLOW_K_INTO_in_insertStatement1931 = new BitSet(new long[]{ -5697204075003641856L, 3782044054195082940L, 6601880872959L });

	public static final BitSet FOLLOW_columnFamilyName_in_insertStatement1935 = new BitSet(new long[]{ 0L, 16777216L, 72057594037927936L });

	public static final BitSet FOLLOW_normalInsertStatement_in_insertStatement1949 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_K_JSON_in_insertStatement1964 = new BitSet(new long[]{ 0L, 0L, 37383395344384L, 1L });

	public static final BitSet FOLLOW_jsonInsertStatement_in_insertStatement1968 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_184_in_normalInsertStatement2004 = new BitSet(new long[]{ -5697204075003625472L, 3782044054195082940L, 4402857617407L });

	public static final BitSet FOLLOW_cident_in_normalInsertStatement2008 = new BitSet(new long[]{ 0L, 0L, 1297036692682702848L });

	public static final BitSet FOLLOW_188_in_normalInsertStatement2015 = new BitSet(new long[]{ -5697204075003625472L, 3782044054195082940L, 4402857617407L });

	public static final BitSet FOLLOW_cident_in_normalInsertStatement2019 = new BitSet(new long[]{ 0L, 0L, 1297036692682702848L });

	public static final BitSet FOLLOW_185_in_normalInsertStatement2026 = new BitSet(new long[]{ 0L, 0L, 67108864L });

	public static final BitSet FOLLOW_K_VALUES_in_normalInsertStatement2034 = new BitSet(new long[]{ 0L, 0L, 72057594037927936L });

	public static final BitSet FOLLOW_184_in_normalInsertStatement2042 = new BitSet(new long[]{ -5697204074984634304L, 3782062196137072316L, 2378223864481298431L, 2177L });

	public static final BitSet FOLLOW_term_in_normalInsertStatement2046 = new BitSet(new long[]{ 0L, 0L, 1297036692682702848L });

	public static final BitSet FOLLOW_188_in_normalInsertStatement2052 = new BitSet(new long[]{ -5697204074984634304L, 3782062196137072316L, 2378223864481298431L, 2177L });

	public static final BitSet FOLLOW_term_in_normalInsertStatement2056 = new BitSet(new long[]{ 0L, 0L, 1297036692682702848L });

	public static final BitSet FOLLOW_185_in_normalInsertStatement2063 = new BitSet(new long[]{ 2L, 8192L, 16777216L });

	public static final BitSet FOLLOW_K_IF_in_normalInsertStatement2073 = new BitSet(new long[]{ 0L, 8796093022208L });

	public static final BitSet FOLLOW_K_NOT_in_normalInsertStatement2075 = new BitSet(new long[]{ 0L, 4L });

	public static final BitSet FOLLOW_K_EXISTS_in_normalInsertStatement2077 = new BitSet(new long[]{ 2L, 0L, 16777216L });

	public static final BitSet FOLLOW_usingClause_in_normalInsertStatement2092 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_jsonValue_in_jsonInsertStatement2138 = new BitSet(new long[]{ 72057594037927938L, 8192L, 16777216L });

	public static final BitSet FOLLOW_K_DEFAULT_in_jsonInsertStatement2148 = new BitSet(new long[]{ 0L, 17592186044416L, 524288L });

	public static final BitSet FOLLOW_K_NULL_in_jsonInsertStatement2152 = new BitSet(new long[]{ 2L, 8192L, 16777216L });

	public static final BitSet FOLLOW_K_UNSET_in_jsonInsertStatement2160 = new BitSet(new long[]{ 2L, 8192L, 16777216L });

	public static final BitSet FOLLOW_K_IF_in_jsonInsertStatement2176 = new BitSet(new long[]{ 0L, 8796093022208L });

	public static final BitSet FOLLOW_K_NOT_in_jsonInsertStatement2178 = new BitSet(new long[]{ 0L, 4L });

	public static final BitSet FOLLOW_K_EXISTS_in_jsonInsertStatement2180 = new BitSet(new long[]{ 2L, 0L, 16777216L });

	public static final BitSet FOLLOW_usingClause_in_jsonInsertStatement2195 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_STRING_LITERAL_in_jsonValue2230 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_192_in_jsonValue2240 = new BitSet(new long[]{ -5697204075003641856L, 3782044054195082940L, 4402857617407L });

	public static final BitSet FOLLOW_noncol_ident_in_jsonValue2244 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_QMARK_in_jsonValue2258 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_K_USING_in_usingClause2289 = new BitSet(new long[]{ 0L, 0L, 33024L });

	public static final BitSet FOLLOW_usingClauseObjective_in_usingClause2291 = new BitSet(new long[]{ 4294967298L });

	public static final BitSet FOLLOW_K_AND_in_usingClause2296 = new BitSet(new long[]{ 0L, 0L, 33024L });

	public static final BitSet FOLLOW_usingClauseObjective_in_usingClause2298 = new BitSet(new long[]{ 4294967298L });

	public static final BitSet FOLLOW_K_TIMESTAMP_in_usingClauseObjective2320 = new BitSet(new long[]{ 16777216L, 0L, 2199023255552L, 1L });

	public static final BitSet FOLLOW_intValue_in_usingClauseObjective2324 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_K_TTL_in_usingClauseObjective2334 = new BitSet(new long[]{ 16777216L, 0L, 2199023255552L, 1L });

	public static final BitSet FOLLOW_intValue_in_usingClauseObjective2338 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_K_UPDATE_in_updateStatement2372 = new BitSet(new long[]{ -5697204075003641856L, 3782044054195082940L, 6601880872959L });

	public static final BitSet FOLLOW_columnFamilyName_in_updateStatement2376 = new BitSet(new long[]{ 0L, -9223372036854775808L, 16777216L });

	public static final BitSet FOLLOW_usingClause_in_updateStatement2386 = new BitSet(new long[]{ 0L, -9223372036854775808L });

	public static final BitSet FOLLOW_K_SET_in_updateStatement2398 = new BitSet(new long[]{ -5697204075003625472L, 3782044054195082940L, 4402857617407L });

	public static final BitSet FOLLOW_columnOperation_in_updateStatement2400 = new BitSet(new long[]{ 0L, 0L, 1152921505680588800L });

	public static final BitSet FOLLOW_188_in_updateStatement2404 = new BitSet(new long[]{ -5697204075003625472L, 3782044054195082940L, 4402857617407L });

	public static final BitSet FOLLOW_columnOperation_in_updateStatement2406 = new BitSet(new long[]{ 0L, 0L, 1152921505680588800L });

	public static final BitSet FOLLOW_K_WHERE_in_updateStatement2417 = new BitSet(new long[]{ -5697204075003625472L, 3782044054195082940L, 72061996895549439L, 1024L });

	public static final BitSet FOLLOW_whereClause_in_updateStatement2421 = new BitSet(new long[]{ 2L, 8192L });

	public static final BitSet FOLLOW_K_IF_in_updateStatement2431 = new BitSet(new long[]{ -5697204075003625472L, 3782044054195082940L, 4402857617407L });

	public static final BitSet FOLLOW_K_EXISTS_in_updateStatement2435 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_updateConditions_in_updateStatement2443 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_columnCondition_in_updateConditions2485 = new BitSet(new long[]{ 4294967298L });

	public static final BitSet FOLLOW_K_AND_in_updateConditions2490 = new BitSet(new long[]{ -5697204075003625472L, 3782044054195082940L, 4402857617407L });

	public static final BitSet FOLLOW_columnCondition_in_updateConditions2492 = new BitSet(new long[]{ 4294967298L });

	public static final BitSet FOLLOW_K_DELETE_in_deleteStatement2529 = new BitSet(new long[]{ -5697204075003625472L, 3782044054195083004L, 4402857617407L });

	public static final BitSet FOLLOW_deleteSelection_in_deleteStatement2535 = new BitSet(new long[]{ 0L, 64L });

	public static final BitSet FOLLOW_K_FROM_in_deleteStatement2548 = new BitSet(new long[]{ -5697204075003641856L, 3782044054195082940L, 6601880872959L });

	public static final BitSet FOLLOW_columnFamilyName_in_deleteStatement2552 = new BitSet(new long[]{ 0L, 0L, 1090519040L });

	public static final BitSet FOLLOW_usingClauseDelete_in_deleteStatement2562 = new BitSet(new long[]{ 0L, 0L, 1073741824L });

	public static final BitSet FOLLOW_K_WHERE_in_deleteStatement2574 = new BitSet(new long[]{ -5697204075003625472L, 3782044054195082940L, 72061996895549439L, 1024L });

	public static final BitSet FOLLOW_whereClause_in_deleteStatement2578 = new BitSet(new long[]{ 2L, 8192L });

	public static final BitSet FOLLOW_K_IF_in_deleteStatement2588 = new BitSet(new long[]{ -5697204075003625472L, 3782044054195082940L, 4402857617407L });

	public static final BitSet FOLLOW_K_EXISTS_in_deleteStatement2592 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_updateConditions_in_deleteStatement2600 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_deleteOp_in_deleteSelection2647 = new BitSet(new long[]{ 2L, 0L, 1152921504606846976L });

	public static final BitSet FOLLOW_188_in_deleteSelection2662 = new BitSet(new long[]{ -5697204075003625472L, 3782044054195082940L, 4402857617407L });

	public static final BitSet FOLLOW_deleteOp_in_deleteSelection2666 = new BitSet(new long[]{ 2L, 0L, 1152921504606846976L });

	public static final BitSet FOLLOW_cident_in_deleteOp2693 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_cident_in_deleteOp2720 = new BitSet(new long[]{ 0L, 0L, 0L, 128L });

	public static final BitSet FOLLOW_199_in_deleteOp2722 = new BitSet(new long[]{ -5697204074984634304L, 3782062196137072316L, 2378223864481298431L, 2177L });

	public static final BitSet FOLLOW_term_in_deleteOp2726 = new BitSet(new long[]{ 0L, 0L, 0L, 512L });

	public static final BitSet FOLLOW_201_in_deleteOp2728 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_cident_in_deleteOp2740 = new BitSet(new long[]{ 0L, 0L, -9223372036854775808L });

	public static final BitSet FOLLOW_191_in_deleteOp2742 = new BitSet(new long[]{ -5697204075003641856L, 3782044054195082940L, 4402857617407L });

	public static final BitSet FOLLOW_fident_in_deleteOp2746 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_K_USING_in_usingClauseDelete2766 = new BitSet(new long[]{ 0L, 0L, 256L });

	public static final BitSet FOLLOW_K_TIMESTAMP_in_usingClauseDelete2768 = new BitSet(new long[]{ 16777216L, 0L, 2199023255552L, 1L });

	public static final BitSet FOLLOW_intValue_in_usingClauseDelete2772 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_K_BEGIN_in_batchStatement2806 = new BitSet(new long[]{ 2252074691592192L, 0L, 262144L });

	public static final BitSet FOLLOW_K_UNLOGGED_in_batchStatement2816 = new BitSet(new long[]{ 274877906944L });

	public static final BitSet FOLLOW_K_COUNTER_in_batchStatement2822 = new BitSet(new long[]{ 274877906944L });

	public static final BitSet FOLLOW_K_BATCH_in_batchStatement2835 = new BitSet(new long[]{ 144115196665790464L, 1048576L, 17825792L });

	public static final BitSet FOLLOW_usingClause_in_batchStatement2839 = new BitSet(new long[]{ 144115196665790464L, 1048576L, 1048576L });

	public static final BitSet FOLLOW_batchStatementObjective_in_batchStatement2859 = new BitSet(new long[]{ 144115196665790464L, 1048576L, 1048576L, 2L });

	public static final BitSet FOLLOW_193_in_batchStatement2861 = new BitSet(new long[]{ 144115196665790464L, 1048576L, 1048576L });

	public static final BitSet FOLLOW_K_APPLY_in_batchStatement2875 = new BitSet(new long[]{ 274877906944L });

	public static final BitSet FOLLOW_K_BATCH_in_batchStatement2877 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_insertStatement_in_batchStatementObjective2908 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_updateStatement_in_batchStatementObjective2921 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_deleteStatement_in_batchStatementObjective2934 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_K_CREATE_in_createAggregateStatement2967 = new BitSet(new long[]{ 268435456L, 281474976710656L });

	public static final BitSet FOLLOW_K_OR_in_createAggregateStatement2970 = new BitSet(new long[]{ 0L, 144115188075855872L });

	public static final BitSet FOLLOW_K_REPLACE_in_createAggregateStatement2972 = new BitSet(new long[]{ 268435456L });

	public static final BitSet FOLLOW_K_AGGREGATE_in_createAggregateStatement2984 = new BitSet(new long[]{ -5697204075003641856L, 3782044054195091132L, 6601880877055L });

	public static final BitSet FOLLOW_K_IF_in_createAggregateStatement2993 = new BitSet(new long[]{ 0L, 8796093022208L });

	public static final BitSet FOLLOW_K_NOT_in_createAggregateStatement2995 = new BitSet(new long[]{ 0L, 4L });

	public static final BitSet FOLLOW_K_EXISTS_in_createAggregateStatement2997 = new BitSet(new long[]{ -5697204075003641856L, 3782044054195082940L, 6601880877055L });

	public static final BitSet FOLLOW_functionName_in_createAggregateStatement3011 = new BitSet(new long[]{ 0L, 0L, 72057594037927936L });

	public static final BitSet FOLLOW_184_in_createAggregateStatement3019 = new BitSet(new long[]{ -5697204075003641856L, -5441327982659692868L, 144154775305562111L });

	public static final BitSet FOLLOW_comparatorType_in_createAggregateStatement3043 = new BitSet(new long[]{ 0L, 0L, 1297036692682702848L });

	public static final BitSet FOLLOW_188_in_createAggregateStatement3059 = new BitSet(new long[]{ -5697204075003641856L, -5441327982659692868L, 39587229706239L });

	public static final BitSet FOLLOW_comparatorType_in_createAggregateStatement3063 = new BitSet(new long[]{ 0L, 0L, 1297036692682702848L });

	public static final BitSet FOLLOW_185_in_createAggregateStatement3087 = new BitSet(new long[]{ 0L, 0L, 1L });

	public static final BitSet FOLLOW_K_SFUNC_in_createAggregateStatement3095 = new BitSet(new long[]{ -6850160763982577664L, 3782044054144751292L, 4398562621439L });

	public static final BitSet FOLLOW_allowedFunctionName_in_createAggregateStatement3101 = new BitSet(new long[]{ 0L, 0L, 16L });

	public static final BitSet FOLLOW_K_STYPE_in_createAggregateStatement3109 = new BitSet(new long[]{ -5697204075003641856L, -5441327982659692868L, 39587229706239L });

	public static final BitSet FOLLOW_comparatorType_in_createAggregateStatement3115 = new BitSet(new long[]{ 2L, 262160L });

	public static final BitSet FOLLOW_K_FINALFUNC_in_createAggregateStatement3133 = new BitSet(new long[]{ -6850160763982577664L, 3782044054144751292L, 4398562621439L });

	public static final BitSet FOLLOW_allowedFunctionName_in_createAggregateStatement3139 = new BitSet(new long[]{ 2L, 262144L });

	public static final BitSet FOLLOW_K_INITCOND_in_createAggregateStatement3166 = new BitSet(new long[]{ -5697204074984634304L, 3782062196137072316L, 2378223864481298431L, 2177L });

	public static final BitSet FOLLOW_term_in_createAggregateStatement3172 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_K_DROP_in_dropAggregateStatement3219 = new BitSet(new long[]{ 268435456L });

	public static final BitSet FOLLOW_K_AGGREGATE_in_dropAggregateStatement3221 = new BitSet(new long[]{ -5697204075003641856L, 3782044054195091132L, 6601880877055L });

	public static final BitSet FOLLOW_K_IF_in_dropAggregateStatement3230 = new BitSet(new long[]{ 0L, 4L });

	public static final BitSet FOLLOW_K_EXISTS_in_dropAggregateStatement3232 = new BitSet(new long[]{ -5697204075003641856L, 3782044054195082940L, 6601880877055L });

	public static final BitSet FOLLOW_functionName_in_dropAggregateStatement3247 = new BitSet(new long[]{ 2L, 0L, 72057594037927936L });

	public static final BitSet FOLLOW_184_in_dropAggregateStatement3265 = new BitSet(new long[]{ -5697204075003641856L, -5441327982659692868L, 144154775305562111L });

	public static final BitSet FOLLOW_comparatorType_in_dropAggregateStatement3293 = new BitSet(new long[]{ 0L, 0L, 1297036692682702848L });

	public static final BitSet FOLLOW_188_in_dropAggregateStatement3311 = new BitSet(new long[]{ -5697204075003641856L, -5441327982659692868L, 39587229706239L });

	public static final BitSet FOLLOW_comparatorType_in_dropAggregateStatement3315 = new BitSet(new long[]{ 0L, 0L, 1297036692682702848L });

	public static final BitSet FOLLOW_185_in_dropAggregateStatement3343 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_K_CREATE_in_createFunctionStatement3400 = new BitSet(new long[]{ 0L, 281474976711168L });

	public static final BitSet FOLLOW_K_OR_in_createFunctionStatement3403 = new BitSet(new long[]{ 0L, 144115188075855872L });

	public static final BitSet FOLLOW_K_REPLACE_in_createFunctionStatement3405 = new BitSet(new long[]{ 0L, 512L });

	public static final BitSet FOLLOW_K_FUNCTION_in_createFunctionStatement3417 = new BitSet(new long[]{ -5697204075003641856L, 3782044054195091132L, 6601880877055L });

	public static final BitSet FOLLOW_K_IF_in_createFunctionStatement3426 = new BitSet(new long[]{ 0L, 8796093022208L });

	public static final BitSet FOLLOW_K_NOT_in_createFunctionStatement3428 = new BitSet(new long[]{ 0L, 4L });

	public static final BitSet FOLLOW_K_EXISTS_in_createFunctionStatement3430 = new BitSet(new long[]{ -5697204075003641856L, 3782044054195082940L, 6601880877055L });

	public static final BitSet FOLLOW_functionName_in_createFunctionStatement3444 = new BitSet(new long[]{ 0L, 0L, 72057594037927936L });

	public static final BitSet FOLLOW_184_in_createFunctionStatement3452 = new BitSet(new long[]{ -5697204075003641856L, 3782044054195082940L, 144119590933473279L });

	public static final BitSet FOLLOW_noncol_ident_in_createFunctionStatement3476 = new BitSet(new long[]{ -5697204075003641856L, -5441327982659692868L, 39587229706239L });

	public static final BitSet FOLLOW_comparatorType_in_createFunctionStatement3480 = new BitSet(new long[]{ 0L, 0L, 1297036692682702848L });

	public static final BitSet FOLLOW_188_in_createFunctionStatement3496 = new BitSet(new long[]{ -5697204075003641856L, 3782044054195082940L, 4402857617407L });

	public static final BitSet FOLLOW_noncol_ident_in_createFunctionStatement3500 = new BitSet(new long[]{ -5697204075003641856L, -5441327982659692868L, 39587229706239L });

	public static final BitSet FOLLOW_comparatorType_in_createFunctionStatement3504 = new BitSet(new long[]{ 0L, 0L, 1297036692682702848L });

	public static final BitSet FOLLOW_185_in_createFunctionStatement3528 = new BitSet(new long[]{ 17592186044416L, 288230376151711744L });

	public static final BitSet FOLLOW_K_RETURNS_in_createFunctionStatement3539 = new BitSet(new long[]{ 0L, 17592186044416L });

	public static final BitSet FOLLOW_K_NULL_in_createFunctionStatement3541 = new BitSet(new long[]{ 0L, 70368744177664L });

	public static final BitSet FOLLOW_K_CALLED_in_createFunctionStatement3547 = new BitSet(new long[]{ 0L, 70368744177664L });

	public static final BitSet FOLLOW_K_ON_in_createFunctionStatement3553 = new BitSet(new long[]{ 0L, 17592186044416L });

	public static final BitSet FOLLOW_K_NULL_in_createFunctionStatement3555 = new BitSet(new long[]{ 0L, 524288L });

	public static final BitSet FOLLOW_K_INPUT_in_createFunctionStatement3557 = new BitSet(new long[]{ 0L, 288230376151711744L });

	public static final BitSet FOLLOW_K_RETURNS_in_createFunctionStatement3565 = new BitSet(new long[]{ -5697204075003641856L, -5441327982659692868L, 39587229706239L });

	public static final BitSet FOLLOW_comparatorType_in_createFunctionStatement3571 = new BitSet(new long[]{ 0L, 536870912L });

	public static final BitSet FOLLOW_K_LANGUAGE_in_createFunctionStatement3579 = new BitSet(new long[]{ 8388608L });

	public static final BitSet FOLLOW_IDENT_in_createFunctionStatement3585 = new BitSet(new long[]{ 17179869184L });

	public static final BitSet FOLLOW_K_AS_in_createFunctionStatement3593 = new BitSet(new long[]{ 0L, 0L, 35184372088832L });

	public static final BitSet FOLLOW_STRING_LITERAL_in_createFunctionStatement3599 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_K_DROP_in_dropFunctionStatement3637 = new BitSet(new long[]{ 0L, 512L });

	public static final BitSet FOLLOW_K_FUNCTION_in_dropFunctionStatement3639 = new BitSet(new long[]{ -5697204075003641856L, 3782044054195091132L, 6601880877055L });

	public static final BitSet FOLLOW_K_IF_in_dropFunctionStatement3648 = new BitSet(new long[]{ 0L, 4L });

	public static final BitSet FOLLOW_K_EXISTS_in_dropFunctionStatement3650 = new BitSet(new long[]{ -5697204075003641856L, 3782044054195082940L, 6601880877055L });

	public static final BitSet FOLLOW_functionName_in_dropFunctionStatement3665 = new BitSet(new long[]{ 2L, 0L, 72057594037927936L });

	public static final BitSet FOLLOW_184_in_dropFunctionStatement3683 = new BitSet(new long[]{ -5697204075003641856L, -5441327982659692868L, 144154775305562111L });

	public static final BitSet FOLLOW_comparatorType_in_dropFunctionStatement3711 = new BitSet(new long[]{ 0L, 0L, 1297036692682702848L });

	public static final BitSet FOLLOW_188_in_dropFunctionStatement3729 = new BitSet(new long[]{ -5697204075003641856L, -5441327982659692868L, 39587229706239L });

	public static final BitSet FOLLOW_comparatorType_in_dropFunctionStatement3733 = new BitSet(new long[]{ 0L, 0L, 1297036692682702848L });

	public static final BitSet FOLLOW_185_in_dropFunctionStatement3761 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_K_CREATE_in_createKeyspaceStatement3820 = new BitSet(new long[]{ 0L, 134217728L });

	public static final BitSet FOLLOW_K_KEYSPACE_in_createKeyspaceStatement3822 = new BitSet(new long[]{ -5697204075003641856L, 3782044054195091132L, 6601880872959L });

	public static final BitSet FOLLOW_K_IF_in_createKeyspaceStatement3825 = new BitSet(new long[]{ 0L, 8796093022208L });

	public static final BitSet FOLLOW_K_NOT_in_createKeyspaceStatement3827 = new BitSet(new long[]{ 0L, 4L });

	public static final BitSet FOLLOW_K_EXISTS_in_createKeyspaceStatement3829 = new BitSet(new long[]{ -5697204075003641856L, 3782044054195082940L, 6601880872959L });

	public static final BitSet FOLLOW_keyspaceName_in_createKeyspaceStatement3838 = new BitSet(new long[]{ 0L, 0L, 2147483648L });

	public static final BitSet FOLLOW_K_WITH_in_createKeyspaceStatement3846 = new BitSet(new long[]{ -5697204075003641856L, 3782044054195082940L, 4402857617407L });

	public static final BitSet FOLLOW_properties_in_createKeyspaceStatement3848 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_K_CREATE_in_createTableStatement3883 = new BitSet(new long[]{ 140737488355328L });

	public static final BitSet FOLLOW_K_COLUMNFAMILY_in_createTableStatement3885 = new BitSet(new long[]{ -5697204075003641856L, 3782044054195091132L, 6601880872959L });

	public static final BitSet FOLLOW_K_IF_in_createTableStatement3888 = new BitSet(new long[]{ 0L, 8796093022208L });

	public static final BitSet FOLLOW_K_NOT_in_createTableStatement3890 = new BitSet(new long[]{ 0L, 4L });

	public static final BitSet FOLLOW_K_EXISTS_in_createTableStatement3892 = new BitSet(new long[]{ -5697204075003641856L, 3782044054195082940L, 6601880872959L });

	public static final BitSet FOLLOW_columnFamilyName_in_createTableStatement3907 = new BitSet(new long[]{ 0L, 0L, 72057594037927936L });

	public static final BitSet FOLLOW_cfamDefinition_in_createTableStatement3917 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_184_in_cfamDefinition3936 = new BitSet(new long[]{ -5697204075003641856L, 3818072851214046908L, 4402857617407L });

	public static final BitSet FOLLOW_cfamColumns_in_cfamDefinition3938 = new BitSet(new long[]{ 0L, 0L, 1297036692682702848L });

	public static final BitSet FOLLOW_188_in_cfamDefinition3943 = new BitSet(new long[]{ -5697204075003641856L, 3818072851214046908L, 1297041095540320255L });

	public static final BitSet FOLLOW_cfamColumns_in_cfamDefinition3945 = new BitSet(new long[]{ 0L, 0L, 1297036692682702848L });

	public static final BitSet FOLLOW_185_in_cfamDefinition3952 = new BitSet(new long[]{ 2L, 0L, 2147483648L });

	public static final BitSet FOLLOW_K_WITH_in_cfamDefinition3962 = new BitSet(new long[]{ -5697204075003641856L, 3782044054195082940L, 4402857617407L });

	public static final BitSet FOLLOW_cfamProperty_in_cfamDefinition3964 = new BitSet(new long[]{ 4294967298L });

	public static final BitSet FOLLOW_K_AND_in_cfamDefinition3969 = new BitSet(new long[]{ -5697204075003641856L, 3782044054195082940L, 4402857617407L });

	public static final BitSet FOLLOW_cfamProperty_in_cfamDefinition3971 = new BitSet(new long[]{ 4294967298L });

	public static final BitSet FOLLOW_ident_in_cfamColumns3997 = new BitSet(new long[]{ -5697204075003641856L, -5441327982659692868L, 39587229706239L });

	public static final BitSet FOLLOW_comparatorType_in_cfamColumns4001 = new BitSet(new long[]{ 2L, 36028797018963968L, 4L });

	public static final BitSet FOLLOW_K_STATIC_in_cfamColumns4006 = new BitSet(new long[]{ 2L, 36028797018963968L });

	public static final BitSet FOLLOW_K_PRIMARY_in_cfamColumns4023 = new BitSet(new long[]{ 0L, 33554432L });

	public static final BitSet FOLLOW_K_KEY_in_cfamColumns4025 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_K_PRIMARY_in_cfamColumns4037 = new BitSet(new long[]{ 0L, 33554432L });

	public static final BitSet FOLLOW_K_KEY_in_cfamColumns4039 = new BitSet(new long[]{ 0L, 0L, 72057594037927936L });

	public static final BitSet FOLLOW_184_in_cfamColumns4041 = new BitSet(new long[]{ -5697204075003641856L, 3782044054195082940L, 72061996895545343L });

	public static final BitSet FOLLOW_pkDef_in_cfamColumns4043 = new BitSet(new long[]{ 0L, 0L, 1297036692682702848L });

	public static final BitSet FOLLOW_188_in_cfamColumns4047 = new BitSet(new long[]{ -5697204075003641856L, 3782044054195082940L, 4402857617407L });

	public static final BitSet FOLLOW_ident_in_cfamColumns4051 = new BitSet(new long[]{ 0L, 0L, 1297036692682702848L });

	public static final BitSet FOLLOW_185_in_cfamColumns4058 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_ident_in_pkDef4078 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_184_in_pkDef4088 = new BitSet(new long[]{ -5697204075003641856L, 3782044054195082940L, 4402857617407L });

	public static final BitSet FOLLOW_ident_in_pkDef4094 = new BitSet(new long[]{ 0L, 0L, 1297036692682702848L });

	public static final BitSet FOLLOW_188_in_pkDef4100 = new BitSet(new long[]{ -5697204075003641856L, 3782044054195082940L, 4402857617407L });

	public static final BitSet FOLLOW_ident_in_pkDef4104 = new BitSet(new long[]{ 0L, 0L, 1297036692682702848L });

	public static final BitSet FOLLOW_185_in_pkDef4111 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_property_in_cfamProperty4131 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_K_COMPACT_in_cfamProperty4140 = new BitSet(new long[]{ 0L, 0L, 8L });

	public static final BitSet FOLLOW_K_STORAGE_in_cfamProperty4142 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_K_CLUSTERING_in_cfamProperty4152 = new BitSet(new long[]{ 0L, 562949953421312L });

	public static final BitSet FOLLOW_K_ORDER_in_cfamProperty4154 = new BitSet(new long[]{ 8796093022208L });

	public static final BitSet FOLLOW_K_BY_in_cfamProperty4156 = new BitSet(new long[]{ 0L, 0L, 72057594037927936L });

	public static final BitSet FOLLOW_184_in_cfamProperty4158 = new BitSet(new long[]{ -5697204075003641856L, 3782044054195082940L, 4402857617407L });

	public static final BitSet FOLLOW_cfamOrdering_in_cfamProperty4160 = new BitSet(new long[]{ 0L, 0L, 1297036692682702848L });

	public static final BitSet FOLLOW_188_in_cfamProperty4164 = new BitSet(new long[]{ -5697204075003641856L, 3782044054195082940L, 4402857617407L });

	public static final BitSet FOLLOW_cfamOrdering_in_cfamProperty4166 = new BitSet(new long[]{ 0L, 0L, 1297036692682702848L });

	public static final BitSet FOLLOW_185_in_cfamProperty4171 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_ident_in_cfamOrdering4199 = new BitSet(new long[]{ 288230410511450112L });

	public static final BitSet FOLLOW_K_ASC_in_cfamOrdering4202 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_K_DESC_in_cfamOrdering4206 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_K_CREATE_in_createTypeStatement4245 = new BitSet(new long[]{ 0L, 0L, 131072L });

	public static final BitSet FOLLOW_K_TYPE_in_createTypeStatement4247 = new BitSet(new long[]{ -5697204075003641856L, 3782044054195091132L, 4402857617407L });

	public static final BitSet FOLLOW_K_IF_in_createTypeStatement4250 = new BitSet(new long[]{ 0L, 8796093022208L });

	public static final BitSet FOLLOW_K_NOT_in_createTypeStatement4252 = new BitSet(new long[]{ 0L, 4L });

	public static final BitSet FOLLOW_K_EXISTS_in_createTypeStatement4254 = new BitSet(new long[]{ -5697204075003641856L, 3782044054195082940L, 4402857617407L });

	public static final BitSet FOLLOW_userTypeName_in_createTypeStatement4272 = new BitSet(new long[]{ 0L, 0L, 72057594037927936L });

	public static final BitSet FOLLOW_184_in_createTypeStatement4285 = new BitSet(new long[]{ -5697204075003641856L, 3782044054195082940L, 4402857617407L });

	public static final BitSet FOLLOW_typeColumns_in_createTypeStatement4287 = new BitSet(new long[]{ 0L, 0L, 1297036692682702848L });

	public static final BitSet FOLLOW_188_in_createTypeStatement4292 = new BitSet(new long[]{ -5697204075003641856L, 3782044054195082940L, 1297041095540320255L });

	public static final BitSet FOLLOW_typeColumns_in_createTypeStatement4294 = new BitSet(new long[]{ 0L, 0L, 1297036692682702848L });

	public static final BitSet FOLLOW_185_in_createTypeStatement4301 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_fident_in_typeColumns4321 = new BitSet(new long[]{ -5697204075003641856L, -5441327982659692868L, 39587229706239L });

	public static final BitSet FOLLOW_comparatorType_in_typeColumns4325 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_K_CREATE_in_createIndexStatement4360 = new BitSet(new long[]{ 9007199254740992L, 32768L });

	public static final BitSet FOLLOW_K_CUSTOM_in_createIndexStatement4363 = new BitSet(new long[]{ 0L, 32768L });

	public static final BitSet FOLLOW_K_INDEX_in_createIndexStatement4369 = new BitSet(new long[]{ -5697204075003641856L, 3782114422939268796L, 6601880872959L });

	public static final BitSet FOLLOW_K_IF_in_createIndexStatement4372 = new BitSet(new long[]{ 0L, 8796093022208L });

	public static final BitSet FOLLOW_K_NOT_in_createIndexStatement4374 = new BitSet(new long[]{ 0L, 4L });

	public static final BitSet FOLLOW_K_EXISTS_in_createIndexStatement4376 = new BitSet(new long[]{ -5697204075003641856L, 3782114422939260604L, 6601880872959L });

	public static final BitSet FOLLOW_idxName_in_createIndexStatement4392 = new BitSet(new long[]{ 0L, 70368744177664L });

	public static final BitSet FOLLOW_K_ON_in_createIndexStatement4397 = new BitSet(new long[]{ -5697204075003641856L, 3782044054195082940L, 6601880872959L });

	public static final BitSet FOLLOW_columnFamilyName_in_createIndexStatement4401 = new BitSet(new long[]{ 0L, 0L, 72057594037927936L });

	public static final BitSet FOLLOW_184_in_createIndexStatement4403 = new BitSet(new long[]{ -5697204075003625472L, 3782044054195083197L, 144119590933473279L });

	public static final BitSet FOLLOW_indexIdent_in_createIndexStatement4406 = new BitSet(new long[]{ 0L, 0L, 1297036692682702848L });

	public static final BitSet FOLLOW_188_in_createIndexStatement4410 = new BitSet(new long[]{ -5697204075003625472L, 3782044054195083197L, 4402857617407L });

	public static final BitSet FOLLOW_indexIdent_in_createIndexStatement4412 = new BitSet(new long[]{ 0L, 0L, 1297036692682702848L });

	public static final BitSet FOLLOW_185_in_createIndexStatement4419 = new BitSet(new long[]{ 2L, 0L, 2164260864L });

	public static final BitSet FOLLOW_K_USING_in_createIndexStatement4430 = new BitSet(new long[]{ 0L, 0L, 35184372088832L });

	public static final BitSet FOLLOW_STRING_LITERAL_in_createIndexStatement4434 = new BitSet(new long[]{ 2L, 0L, 2147483648L });

	public static final BitSet FOLLOW_K_WITH_in_createIndexStatement4449 = new BitSet(new long[]{ -5697204075003641856L, 3782044054195082940L, 4402857617407L });

	public static final BitSet FOLLOW_properties_in_createIndexStatement4451 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_cident_in_indexIdent4483 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_K_VALUES_in_indexIdent4511 = new BitSet(new long[]{ 0L, 0L, 72057594037927936L });

	public static final BitSet FOLLOW_184_in_indexIdent4513 = new BitSet(new long[]{ -5697204075003625472L, 3782044054195082940L, 4402857617407L });

	public static final BitSet FOLLOW_cident_in_indexIdent4517 = new BitSet(new long[]{ 0L, 0L, 144115188075855872L });

	public static final BitSet FOLLOW_185_in_indexIdent4519 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_K_KEYS_in_indexIdent4530 = new BitSet(new long[]{ 0L, 0L, 72057594037927936L });

	public static final BitSet FOLLOW_184_in_indexIdent4532 = new BitSet(new long[]{ -5697204075003625472L, 3782044054195082940L, 4402857617407L });

	public static final BitSet FOLLOW_cident_in_indexIdent4536 = new BitSet(new long[]{ 0L, 0L, 144115188075855872L });

	public static final BitSet FOLLOW_185_in_indexIdent4538 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_K_ENTRIES_in_indexIdent4551 = new BitSet(new long[]{ 0L, 0L, 72057594037927936L });

	public static final BitSet FOLLOW_184_in_indexIdent4553 = new BitSet(new long[]{ -5697204075003625472L, 3782044054195082940L, 4402857617407L });

	public static final BitSet FOLLOW_cident_in_indexIdent4557 = new BitSet(new long[]{ 0L, 0L, 144115188075855872L });

	public static final BitSet FOLLOW_185_in_indexIdent4559 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_K_FULL_in_indexIdent4569 = new BitSet(new long[]{ 0L, 0L, 72057594037927936L });

	public static final BitSet FOLLOW_184_in_indexIdent4571 = new BitSet(new long[]{ -5697204075003625472L, 3782044054195082940L, 4402857617407L });

	public static final BitSet FOLLOW_cident_in_indexIdent4575 = new BitSet(new long[]{ 0L, 0L, 144115188075855872L });

	public static final BitSet FOLLOW_185_in_indexIdent4577 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_K_CREATE_in_createMaterializedViewStatement4614 = new BitSet(new long[]{ 0L, 34359738368L });

	public static final BitSet FOLLOW_K_MATERIALIZED_in_createMaterializedViewStatement4616 = new BitSet(new long[]{ 0L, 0L, 536870912L });

	public static final BitSet FOLLOW_K_VIEW_in_createMaterializedViewStatement4618 = new BitSet(new long[]{ -5697204075003641856L, 3782044054195091132L, 6601880872959L });

	public static final BitSet FOLLOW_K_IF_in_createMaterializedViewStatement4621 = new BitSet(new long[]{ 0L, 8796093022208L });

	public static final BitSet FOLLOW_K_NOT_in_createMaterializedViewStatement4623 = new BitSet(new long[]{ 0L, 4L });

	public static final BitSet FOLLOW_K_EXISTS_in_createMaterializedViewStatement4625 = new BitSet(new long[]{ -5697204075003641856L, 3782044054195082940L, 6601880872959L });

	public static final BitSet FOLLOW_columnFamilyName_in_createMaterializedViewStatement4633 = new BitSet(new long[]{ 17179869184L });

	public static final BitSet FOLLOW_K_AS_in_createMaterializedViewStatement4635 = new BitSet(new long[]{ 0L, 4611686018427387904L });

	public static final BitSet FOLLOW_K_SELECT_in_createMaterializedViewStatement4645 = new BitSet(new long[]{ -5697204074984617920L, 3782062196137072316L, 2378223864481298431L, 2433L });

	public static final BitSet FOLLOW_selectClause_in_createMaterializedViewStatement4649 = new BitSet(new long[]{ 0L, 64L });

	public static final BitSet FOLLOW_K_FROM_in_createMaterializedViewStatement4651 = new BitSet(new long[]{ -5697204075003641856L, 3782044054195082940L, 6601880872959L });

	public static final BitSet FOLLOW_columnFamilyName_in_createMaterializedViewStatement4655 = new BitSet(new long[]{ 0L, 36028797018963968L, 1073741824L });

	public static final BitSet FOLLOW_K_WHERE_in_createMaterializedViewStatement4666 = new BitSet(new long[]{ -5697204075003625472L, 3782044054195082940L, 72061996895549439L, 1024L });

	public static final BitSet FOLLOW_whereClause_in_createMaterializedViewStatement4670 = new BitSet(new long[]{ 0L, 36028797018963968L });

	public static final BitSet FOLLOW_K_PRIMARY_in_createMaterializedViewStatement4682 = new BitSet(new long[]{ 0L, 33554432L });

	public static final BitSet FOLLOW_K_KEY_in_createMaterializedViewStatement4684 = new BitSet(new long[]{ 0L, 0L, 72057594037927936L });

	public static final BitSet FOLLOW_184_in_createMaterializedViewStatement4696 = new BitSet(new long[]{ 0L, 0L, 72057594037927936L });

	public static final BitSet FOLLOW_184_in_createMaterializedViewStatement4698 = new BitSet(new long[]{ -5697204075003625472L, 3782044054195082940L, 4402857617407L });

	public static final BitSet FOLLOW_cident_in_createMaterializedViewStatement4702 = new BitSet(new long[]{ 0L, 0L, 1297036692682702848L });

	public static final BitSet FOLLOW_188_in_createMaterializedViewStatement4708 = new BitSet(new long[]{ -5697204075003625472L, 3782044054195082940L, 4402857617407L });

	public static final BitSet FOLLOW_cident_in_createMaterializedViewStatement4712 = new BitSet(new long[]{ 0L, 0L, 1297036692682702848L });

	public static final BitSet FOLLOW_185_in_createMaterializedViewStatement4719 = new BitSet(new long[]{ 0L, 0L, 1297036692682702848L });

	public static final BitSet FOLLOW_188_in_createMaterializedViewStatement4723 = new BitSet(new long[]{ -5697204075003625472L, 3782044054195082940L, 4402857617407L });

	public static final BitSet FOLLOW_cident_in_createMaterializedViewStatement4727 = new BitSet(new long[]{ 0L, 0L, 1297036692682702848L });

	public static final BitSet FOLLOW_185_in_createMaterializedViewStatement4734 = new BitSet(new long[]{ 2L, 0L, 2147483648L });

	public static final BitSet FOLLOW_184_in_createMaterializedViewStatement4744 = new BitSet(new long[]{ -5697204075003625472L, 3782044054195082940L, 4402857617407L });

	public static final BitSet FOLLOW_cident_in_createMaterializedViewStatement4748 = new BitSet(new long[]{ 0L, 0L, 1297036692682702848L });

	public static final BitSet FOLLOW_188_in_createMaterializedViewStatement4754 = new BitSet(new long[]{ -5697204075003625472L, 3782044054195082940L, 4402857617407L });

	public static final BitSet FOLLOW_cident_in_createMaterializedViewStatement4758 = new BitSet(new long[]{ 0L, 0L, 1297036692682702848L });

	public static final BitSet FOLLOW_185_in_createMaterializedViewStatement4765 = new BitSet(new long[]{ 2L, 0L, 2147483648L });

	public static final BitSet FOLLOW_K_WITH_in_createMaterializedViewStatement4797 = new BitSet(new long[]{ -5697204075003641856L, 3782044054195082940L, 4402857617407L });

	public static final BitSet FOLLOW_cfamProperty_in_createMaterializedViewStatement4799 = new BitSet(new long[]{ 4294967298L });

	public static final BitSet FOLLOW_K_AND_in_createMaterializedViewStatement4804 = new BitSet(new long[]{ -5697204075003641856L, 3782044054195082940L, 4402857617407L });

	public static final BitSet FOLLOW_cfamProperty_in_createMaterializedViewStatement4806 = new BitSet(new long[]{ 4294967298L });

	public static final BitSet FOLLOW_K_CREATE_in_createTriggerStatement4844 = new BitSet(new long[]{ 0L, 0L, 8192L });

	public static final BitSet FOLLOW_K_TRIGGER_in_createTriggerStatement4846 = new BitSet(new long[]{ -5697204075003641856L, 3782044054195091132L, 4402857617407L });

	public static final BitSet FOLLOW_K_IF_in_createTriggerStatement4849 = new BitSet(new long[]{ 0L, 8796093022208L });

	public static final BitSet FOLLOW_K_NOT_in_createTriggerStatement4851 = new BitSet(new long[]{ 0L, 4L });

	public static final BitSet FOLLOW_K_EXISTS_in_createTriggerStatement4853 = new BitSet(new long[]{ -5697204075003641856L, 3782044054195082940L, 4402857617407L });

	public static final BitSet FOLLOW_ident_in_createTriggerStatement4863 = new BitSet(new long[]{ 0L, 70368744177664L });

	public static final BitSet FOLLOW_K_ON_in_createTriggerStatement4874 = new BitSet(new long[]{ -5697204075003641856L, 3782044054195082940L, 6601880872959L });

	public static final BitSet FOLLOW_columnFamilyName_in_createTriggerStatement4878 = new BitSet(new long[]{ 0L, 0L, 16777216L });

	public static final BitSet FOLLOW_K_USING_in_createTriggerStatement4880 = new BitSet(new long[]{ 0L, 0L, 35184372088832L });

	public static final BitSet FOLLOW_STRING_LITERAL_in_createTriggerStatement4884 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_K_DROP_in_dropTriggerStatement4925 = new BitSet(new long[]{ 0L, 0L, 8192L });

	public static final BitSet FOLLOW_K_TRIGGER_in_dropTriggerStatement4927 = new BitSet(new long[]{ -5697204075003641856L, 3782044054195091132L, 4402857617407L });

	public static final BitSet FOLLOW_K_IF_in_dropTriggerStatement4930 = new BitSet(new long[]{ 0L, 4L });

	public static final BitSet FOLLOW_K_EXISTS_in_dropTriggerStatement4932 = new BitSet(new long[]{ -5697204075003641856L, 3782044054195082940L, 4402857617407L });

	public static final BitSet FOLLOW_ident_in_dropTriggerStatement4942 = new BitSet(new long[]{ 0L, 70368744177664L });

	public static final BitSet FOLLOW_K_ON_in_dropTriggerStatement4945 = new BitSet(new long[]{ -5697204075003641856L, 3782044054195082940L, 6601880872959L });

	public static final BitSet FOLLOW_columnFamilyName_in_dropTriggerStatement4949 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_K_ALTER_in_alterKeyspaceStatement4989 = new BitSet(new long[]{ 0L, 134217728L });

	public static final BitSet FOLLOW_K_KEYSPACE_in_alterKeyspaceStatement4991 = new BitSet(new long[]{ -5697204075003641856L, 3782044054195082940L, 6601880872959L });

	public static final BitSet FOLLOW_keyspaceName_in_alterKeyspaceStatement4995 = new BitSet(new long[]{ 0L, 0L, 2147483648L });

	public static final BitSet FOLLOW_K_WITH_in_alterKeyspaceStatement5005 = new BitSet(new long[]{ -5697204075003641856L, 3782044054195082940L, 4402857617407L });

	public static final BitSet FOLLOW_properties_in_alterKeyspaceStatement5007 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_K_ALTER_in_alterTableStatement5042 = new BitSet(new long[]{ 140737488355328L });

	public static final BitSet FOLLOW_K_COLUMNFAMILY_in_alterTableStatement5044 = new BitSet(new long[]{ -5697204075003641856L, 3782044054195082940L, 6601880872959L });

	public static final BitSet FOLLOW_columnFamilyName_in_alterTableStatement5048 = new BitSet(new long[]{ 4611686020709089280L, 72057594037927936L, 2147483648L });

	public static final BitSet FOLLOW_K_ALTER_in_alterTableStatement5062 = new BitSet(new long[]{ -5697204075003641856L, 3782044054195082940L, 4402857617407L });

	public static final BitSet FOLLOW_schema_cident_in_alterTableStatement5066 = new BitSet(new long[]{ 0L, 0L, 131072L });

	public static final BitSet FOLLOW_K_TYPE_in_alterTableStatement5069 = new BitSet(new long[]{ -5697204075003641856L, -5441327982659692868L, 39587229706239L });

	public static final BitSet FOLLOW_comparatorType_in_alterTableStatement5073 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_K_ADD_in_alterTableStatement5092 = new BitSet(new long[]{ -5697204075003641856L, 3782044054195082940L, 72061996895545343L });

	public static final BitSet FOLLOW_schema_cident_in_alterTableStatement5107 = new BitSet(new long[]{ -5697204075003641856L, -5441327982659692868L, 39587229706239L });

	public static final BitSet FOLLOW_comparatorType_in_alterTableStatement5112 = new BitSet(new long[]{ 0L, 0L, 4L });

	public static final BitSet FOLLOW_cfisStatic_in_alterTableStatement5118 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_184_in_alterTableStatement5147 = new BitSet(new long[]{ -5697204075003641856L, 3782044054195082940L, 4402857617407L });

	public static final BitSet FOLLOW_schema_cident_in_alterTableStatement5152 = new BitSet(new long[]{ -5697204075003641856L, -5441327982659692868L, 39587229706239L });

	public static final BitSet FOLLOW_comparatorType_in_alterTableStatement5157 = new BitSet(new long[]{ 0L, 0L, 1297036692682702852L });

	public static final BitSet FOLLOW_cfisStatic_in_alterTableStatement5162 = new BitSet(new long[]{ 0L, 0L, 1297036692682702848L });

	public static final BitSet FOLLOW_188_in_alterTableStatement5191 = new BitSet(new long[]{ -5697204075003641856L, 3782044054195082940L, 4402857617407L });

	public static final BitSet FOLLOW_schema_cident_in_alterTableStatement5195 = new BitSet(new long[]{ -5697204075003641856L, -5441327982659692868L, 39587229706239L });

	public static final BitSet FOLLOW_comparatorType_in_alterTableStatement5200 = new BitSet(new long[]{ 0L, 0L, 1297036692682702852L });

	public static final BitSet FOLLOW_cfisStatic_in_alterTableStatement5205 = new BitSet(new long[]{ 0L, 0L, 1297036692682702848L });

	public static final BitSet FOLLOW_185_in_alterTableStatement5212 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_K_DROP_in_alterTableStatement5232 = new BitSet(new long[]{ 281474976710656L });

	public static final BitSet FOLLOW_K_COMPACT_in_alterTableStatement5234 = new BitSet(new long[]{ 0L, 0L, 8L });

	public static final BitSet FOLLOW_K_STORAGE_in_alterTableStatement5236 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_K_DROP_in_alterTableStatement5269 = new BitSet(new long[]{ -5697204075003641856L, 3782044054195082940L, 72061996895545343L });

	public static final BitSet FOLLOW_schema_cident_in_alterTableStatement5284 = new BitSet(new long[]{ 2L, 0L, 16777216L });

	public static final BitSet FOLLOW_184_in_alterTableStatement5314 = new BitSet(new long[]{ -5697204075003641856L, 3782044054195082940L, 4402857617407L });

	public static final BitSet FOLLOW_schema_cident_in_alterTableStatement5319 = new BitSet(new long[]{ 0L, 0L, 1297036692682702848L });

	public static final BitSet FOLLOW_188_in_alterTableStatement5349 = new BitSet(new long[]{ -5697204075003641856L, 3782044054195082940L, 4402857617407L });

	public static final BitSet FOLLOW_schema_cident_in_alterTableStatement5353 = new BitSet(new long[]{ 0L, 0L, 1297036692682702848L });

	public static final BitSet FOLLOW_185_in_alterTableStatement5360 = new BitSet(new long[]{ 2L, 0L, 16777216L });

	public static final BitSet FOLLOW_K_USING_in_alterTableStatement5388 = new BitSet(new long[]{ 0L, 0L, 256L });

	public static final BitSet FOLLOW_K_TIMESTAMP_in_alterTableStatement5390 = new BitSet(new long[]{ 16777216L });

	public static final BitSet FOLLOW_INTEGER_in_alterTableStatement5394 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_K_WITH_in_alterTableStatement5416 = new BitSet(new long[]{ -5697204075003641856L, 3782044054195082940L, 4402857617407L });

	public static final BitSet FOLLOW_properties_in_alterTableStatement5419 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_K_RENAME_in_alterTableStatement5452 = new BitSet(new long[]{ -5697204075003641856L, 3782044054195082940L, 4402857617407L });

	public static final BitSet FOLLOW_schema_cident_in_alterTableStatement5506 = new BitSet(new long[]{ 0L, 0L, 2048L });

	public static final BitSet FOLLOW_K_TO_in_alterTableStatement5508 = new BitSet(new long[]{ -5697204075003641856L, 3782044054195082940L, 4402857617407L });

	public static final BitSet FOLLOW_schema_cident_in_alterTableStatement5512 = new BitSet(new long[]{ 4294967298L });

	public static final BitSet FOLLOW_K_AND_in_alterTableStatement5533 = new BitSet(new long[]{ -5697204075003641856L, 3782044054195082940L, 4402857617407L });

	public static final BitSet FOLLOW_schema_cident_in_alterTableStatement5537 = new BitSet(new long[]{ 0L, 0L, 2048L });

	public static final BitSet FOLLOW_K_TO_in_alterTableStatement5539 = new BitSet(new long[]{ -5697204075003641856L, 3782044054195082940L, 4402857617407L });

	public static final BitSet FOLLOW_schema_cident_in_alterTableStatement5543 = new BitSet(new long[]{ 4294967298L });

	public static final BitSet FOLLOW_K_STATIC_in_cfisStatic5596 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_K_ALTER_in_alterMaterializedViewStatement5632 = new BitSet(new long[]{ 0L, 34359738368L });

	public static final BitSet FOLLOW_K_MATERIALIZED_in_alterMaterializedViewStatement5634 = new BitSet(new long[]{ 0L, 0L, 536870912L });

	public static final BitSet FOLLOW_K_VIEW_in_alterMaterializedViewStatement5636 = new BitSet(new long[]{ -5697204075003641856L, 3782044054195082940L, 6601880872959L });

	public static final BitSet FOLLOW_columnFamilyName_in_alterMaterializedViewStatement5640 = new BitSet(new long[]{ 0L, 0L, 2147483648L });

	public static final BitSet FOLLOW_K_WITH_in_alterMaterializedViewStatement5652 = new BitSet(new long[]{ -5697204075003641856L, 3782044054195082940L, 4402857617407L });

	public static final BitSet FOLLOW_properties_in_alterMaterializedViewStatement5654 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_K_ALTER_in_alterTypeStatement5685 = new BitSet(new long[]{ 0L, 0L, 131072L });

	public static final BitSet FOLLOW_K_TYPE_in_alterTypeStatement5687 = new BitSet(new long[]{ -5697204075003641856L, 3782044054195082940L, 4402857617407L });

	public static final BitSet FOLLOW_userTypeName_in_alterTypeStatement5691 = new BitSet(new long[]{ 2281701376L, 72057594037927936L });

	public static final BitSet FOLLOW_K_ALTER_in_alterTypeStatement5705 = new BitSet(new long[]{ -5697204075003641856L, 3782044054195082940L, 4402857617407L });

	public static final BitSet FOLLOW_fident_in_alterTypeStatement5709 = new BitSet(new long[]{ 0L, 0L, 131072L });

	public static final BitSet FOLLOW_K_TYPE_in_alterTypeStatement5711 = new BitSet(new long[]{ -5697204075003641856L, -5441327982659692868L, 39587229706239L });

	public static final BitSet FOLLOW_comparatorType_in_alterTypeStatement5715 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_K_ADD_in_alterTypeStatement5731 = new BitSet(new long[]{ -5697204075003641856L, 3782044054195082940L, 4402857617407L });

	public static final BitSet FOLLOW_fident_in_alterTypeStatement5737 = new BitSet(new long[]{ -5697204075003641856L, -5441327982659692868L, 39587229706239L });

	public static final BitSet FOLLOW_comparatorType_in_alterTypeStatement5741 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_K_RENAME_in_alterTypeStatement5764 = new BitSet(new long[]{ -5697204075003641856L, 3782044054195082940L, 4402857617407L });

	public static final BitSet FOLLOW_fident_in_alterTypeStatement5802 = new BitSet(new long[]{ 0L, 0L, 2048L });

	public static final BitSet FOLLOW_K_TO_in_alterTypeStatement5804 = new BitSet(new long[]{ -5697204075003641856L, 3782044054195082940L, 4402857617407L });

	public static final BitSet FOLLOW_fident_in_alterTypeStatement5808 = new BitSet(new long[]{ 4294967298L });

	public static final BitSet FOLLOW_K_AND_in_alterTypeStatement5831 = new BitSet(new long[]{ -5697204075003641856L, 3782044054195082940L, 4402857617407L });

	public static final BitSet FOLLOW_fident_in_alterTypeStatement5835 = new BitSet(new long[]{ 0L, 0L, 2048L });

	public static final BitSet FOLLOW_K_TO_in_alterTypeStatement5837 = new BitSet(new long[]{ -5697204075003641856L, 3782044054195082940L, 4402857617407L });

	public static final BitSet FOLLOW_fident_in_alterTypeStatement5841 = new BitSet(new long[]{ 4294967298L });

	public static final BitSet FOLLOW_K_DROP_in_dropKeyspaceStatement5908 = new BitSet(new long[]{ 0L, 134217728L });

	public static final BitSet FOLLOW_K_KEYSPACE_in_dropKeyspaceStatement5910 = new BitSet(new long[]{ -5697204075003641856L, 3782044054195091132L, 6601880872959L });

	public static final BitSet FOLLOW_K_IF_in_dropKeyspaceStatement5913 = new BitSet(new long[]{ 0L, 4L });

	public static final BitSet FOLLOW_K_EXISTS_in_dropKeyspaceStatement5915 = new BitSet(new long[]{ -5697204075003641856L, 3782044054195082940L, 6601880872959L });

	public static final BitSet FOLLOW_keyspaceName_in_dropKeyspaceStatement5924 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_K_DROP_in_dropTableStatement5958 = new BitSet(new long[]{ 140737488355328L });

	public static final BitSet FOLLOW_K_COLUMNFAMILY_in_dropTableStatement5960 = new BitSet(new long[]{ -5697204075003641856L, 3782044054195091132L, 6601880872959L });

	public static final BitSet FOLLOW_K_IF_in_dropTableStatement5963 = new BitSet(new long[]{ 0L, 4L });

	public static final BitSet FOLLOW_K_EXISTS_in_dropTableStatement5965 = new BitSet(new long[]{ -5697204075003641856L, 3782044054195082940L, 6601880872959L });

	public static final BitSet FOLLOW_columnFamilyName_in_dropTableStatement5974 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_K_DROP_in_dropTypeStatement6008 = new BitSet(new long[]{ 0L, 0L, 131072L });

	public static final BitSet FOLLOW_K_TYPE_in_dropTypeStatement6010 = new BitSet(new long[]{ -5697204075003641856L, 3782044054195091132L, 4402857617407L });

	public static final BitSet FOLLOW_K_IF_in_dropTypeStatement6013 = new BitSet(new long[]{ 0L, 4L });

	public static final BitSet FOLLOW_K_EXISTS_in_dropTypeStatement6015 = new BitSet(new long[]{ -5697204075003641856L, 3782044054195082940L, 4402857617407L });

	public static final BitSet FOLLOW_userTypeName_in_dropTypeStatement6024 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_K_DROP_in_dropIndexStatement6058 = new BitSet(new long[]{ 0L, 32768L });

	public static final BitSet FOLLOW_K_INDEX_in_dropIndexStatement6060 = new BitSet(new long[]{ -5697204075003641856L, 3782044054195091132L, 6601880872959L });

	public static final BitSet FOLLOW_K_IF_in_dropIndexStatement6063 = new BitSet(new long[]{ 0L, 4L });

	public static final BitSet FOLLOW_K_EXISTS_in_dropIndexStatement6065 = new BitSet(new long[]{ -5697204075003641856L, 3782044054195082940L, 6601880872959L });

	public static final BitSet FOLLOW_indexName_in_dropIndexStatement6074 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_K_DROP_in_dropMaterializedViewStatement6114 = new BitSet(new long[]{ 0L, 34359738368L });

	public static final BitSet FOLLOW_K_MATERIALIZED_in_dropMaterializedViewStatement6116 = new BitSet(new long[]{ 0L, 0L, 536870912L });

	public static final BitSet FOLLOW_K_VIEW_in_dropMaterializedViewStatement6118 = new BitSet(new long[]{ -5697204075003641856L, 3782044054195091132L, 6601880872959L });

	public static final BitSet FOLLOW_K_IF_in_dropMaterializedViewStatement6121 = new BitSet(new long[]{ 0L, 4L });

	public static final BitSet FOLLOW_K_EXISTS_in_dropMaterializedViewStatement6123 = new BitSet(new long[]{ -5697204075003641856L, 3782044054195082940L, 6601880872959L });

	public static final BitSet FOLLOW_columnFamilyName_in_dropMaterializedViewStatement6132 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_K_TRUNCATE_in_truncateStatement6163 = new BitSet(new long[]{ -5697063337515286528L, 3782044054195082940L, 6601880872959L });

	public static final BitSet FOLLOW_K_COLUMNFAMILY_in_truncateStatement6166 = new BitSet(new long[]{ -5697204075003641856L, 3782044054195082940L, 6601880872959L });

	public static final BitSet FOLLOW_columnFamilyName_in_truncateStatement6172 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_K_GRANT_in_grantPermissionsStatement6197 = new BitSet(new long[]{ 5192650510481489920L, 4611686293305294850L });

	public static final BitSet FOLLOW_permissionOrAll_in_grantPermissionsStatement6209 = new BitSet(new long[]{ 0L, 70368744177664L });

	public static final BitSet FOLLOW_K_ON_in_grantPermissionsStatement6217 = new BitSet(new long[]{ -5697063337515286528L, 3782044260487730876L, 6601880872959L });

	public static final BitSet FOLLOW_resource_in_grantPermissionsStatement6229 = new BitSet(new long[]{ 0L, 0L, 2048L });

	public static final BitSet FOLLOW_K_TO_in_grantPermissionsStatement6237 = new BitSet(new long[]{ -5697204075003641856L, 3782044054195082940L, 41786252961791L });

	public static final BitSet FOLLOW_userOrRoleName_in_grantPermissionsStatement6251 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_K_REVOKE_in_revokePermissionsStatement6282 = new BitSet(new long[]{ 5192650510481489920L, 4611686293305294850L });

	public static final BitSet FOLLOW_permissionOrAll_in_revokePermissionsStatement6294 = new BitSet(new long[]{ 0L, 70368744177664L });

	public static final BitSet FOLLOW_K_ON_in_revokePermissionsStatement6302 = new BitSet(new long[]{ -5697063337515286528L, 3782044260487730876L, 6601880872959L });

	public static final BitSet FOLLOW_resource_in_revokePermissionsStatement6314 = new BitSet(new long[]{ 0L, 64L });

	public static final BitSet FOLLOW_K_FROM_in_revokePermissionsStatement6322 = new BitSet(new long[]{ -5697204075003641856L, 3782044054195082940L, 41786252961791L });

	public static final BitSet FOLLOW_userOrRoleName_in_revokePermissionsStatement6336 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_K_GRANT_in_grantRoleStatement6367 = new BitSet(new long[]{ -5697204075003641856L, 3782044054195082940L, 41786252961791L });

	public static final BitSet FOLLOW_userOrRoleName_in_grantRoleStatement6381 = new BitSet(new long[]{ 0L, 0L, 2048L });

	public static final BitSet FOLLOW_K_TO_in_grantRoleStatement6389 = new BitSet(new long[]{ -5697204075003641856L, 3782044054195082940L, 41786252961791L });

	public static final BitSet FOLLOW_userOrRoleName_in_grantRoleStatement6403 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_K_REVOKE_in_revokeRoleStatement6434 = new BitSet(new long[]{ -5697204075003641856L, 3782044054195082940L, 41786252961791L });

	public static final BitSet FOLLOW_userOrRoleName_in_revokeRoleStatement6448 = new BitSet(new long[]{ 0L, 64L });

	public static final BitSet FOLLOW_K_FROM_in_revokeRoleStatement6456 = new BitSet(new long[]{ -5697204075003641856L, 3782044054195082940L, 41786252961791L });

	public static final BitSet FOLLOW_userOrRoleName_in_revokeRoleStatement6470 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_K_LIST_in_listPermissionsStatement6508 = new BitSet(new long[]{ 5192650510481489920L, 4611686293305294850L });

	public static final BitSet FOLLOW_permissionOrAll_in_listPermissionsStatement6520 = new BitSet(new long[]{ 2L, 107752139522048L });

	public static final BitSet FOLLOW_K_ON_in_listPermissionsStatement6530 = new BitSet(new long[]{ -5697063337515286528L, 3782044260487730876L, 6601880872959L });

	public static final BitSet FOLLOW_resource_in_listPermissionsStatement6532 = new BitSet(new long[]{ 2L, 37383395344384L });

	public static final BitSet FOLLOW_K_OF_in_listPermissionsStatement6547 = new BitSet(new long[]{ -5697204075003641856L, 3782044054195082940L, 41786252961791L });

	public static final BitSet FOLLOW_roleName_in_listPermissionsStatement6549 = new BitSet(new long[]{ 2L, 2199023255552L });

	public static final BitSet FOLLOW_K_NORECURSIVE_in_listPermissionsStatement6563 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_set_in_permission6599 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_K_ALL_in_permissionOrAll6656 = new BitSet(new long[]{ 2L, 18014398509481984L });

	public static final BitSet FOLLOW_K_PERMISSIONS_in_permissionOrAll6660 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_permission_in_permissionOrAll6681 = new BitSet(new long[]{ 2L, 9007199254740992L });

	public static final BitSet FOLLOW_K_PERMISSION_in_permissionOrAll6685 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_dataResource_in_resource6713 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_roleResource_in_resource6725 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_functionResource_in_resource6737 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_jmxResource_in_resource6749 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_K_ALL_in_dataResource6772 = new BitSet(new long[]{ 0L, 268435456L });

	public static final BitSet FOLLOW_K_KEYSPACES_in_dataResource6774 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_K_KEYSPACE_in_dataResource6784 = new BitSet(new long[]{ -5697204075003641856L, 3782044054195082940L, 6601880872959L });

	public static final BitSet FOLLOW_keyspaceName_in_dataResource6790 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_K_COLUMNFAMILY_in_dataResource6802 = new BitSet(new long[]{ -5697204075003641856L, 3782044054195082940L, 6601880872959L });

	public static final BitSet FOLLOW_columnFamilyName_in_dataResource6811 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_K_ALL_in_jmxResource6840 = new BitSet(new long[]{ 0L, 137438953472L });

	public static final BitSet FOLLOW_K_MBEANS_in_jmxResource6842 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_K_MBEAN_in_jmxResource6862 = new BitSet(new long[]{ 0L, 0L, 35184372088832L });

	public static final BitSet FOLLOW_mbean_in_jmxResource6864 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_K_MBEANS_in_jmxResource6874 = new BitSet(new long[]{ 0L, 0L, 35184372088832L });

	public static final BitSet FOLLOW_mbean_in_jmxResource6876 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_K_ALL_in_roleResource6899 = new BitSet(new long[]{ 0L, 2305843009213693952L });

	public static final BitSet FOLLOW_K_ROLES_in_roleResource6901 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_K_ROLE_in_roleResource6911 = new BitSet(new long[]{ -5697204075003641856L, 3782044054195082940L, 41786252961791L });

	public static final BitSet FOLLOW_userOrRoleName_in_roleResource6917 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_K_ALL_in_functionResource6949 = new BitSet(new long[]{ 0L, 1024L });

	public static final BitSet FOLLOW_K_FUNCTIONS_in_functionResource6951 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_K_ALL_in_functionResource6961 = new BitSet(new long[]{ 0L, 1024L });

	public static final BitSet FOLLOW_K_FUNCTIONS_in_functionResource6963 = new BitSet(new long[]{ 0L, 16384L });

	public static final BitSet FOLLOW_K_IN_in_functionResource6965 = new BitSet(new long[]{ 0L, 134217728L });

	public static final BitSet FOLLOW_K_KEYSPACE_in_functionResource6967 = new BitSet(new long[]{ -5697204075003641856L, 3782044054195082940L, 6601880872959L });

	public static final BitSet FOLLOW_keyspaceName_in_functionResource6973 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_K_FUNCTION_in_functionResource6988 = new BitSet(new long[]{ -5697204075003641856L, 3782044054195082940L, 6601880877055L });

	public static final BitSet FOLLOW_functionName_in_functionResource6992 = new BitSet(new long[]{ 0L, 0L, 72057594037927936L });

	public static final BitSet FOLLOW_184_in_functionResource7010 = new BitSet(new long[]{ -5697204075003641856L, -5441327982659692868L, 144154775305562111L });

	public static final BitSet FOLLOW_comparatorType_in_functionResource7038 = new BitSet(new long[]{ 0L, 0L, 1297036692682702848L });

	public static final BitSet FOLLOW_188_in_functionResource7056 = new BitSet(new long[]{ -5697204075003641856L, -5441327982659692868L, 39587229706239L });

	public static final BitSet FOLLOW_comparatorType_in_functionResource7060 = new BitSet(new long[]{ 0L, 0L, 1297036692682702848L });

	public static final BitSet FOLLOW_185_in_functionResource7088 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_K_CREATE_in_createUserStatement7136 = new BitSet(new long[]{ 0L, 0L, 4194304L });

	public static final BitSet FOLLOW_K_USER_in_createUserStatement7138 = new BitSet(new long[]{ 8388608L, 8192L, 39582418599936L });

	public static final BitSet FOLLOW_K_IF_in_createUserStatement7141 = new BitSet(new long[]{ 0L, 8796093022208L });

	public static final BitSet FOLLOW_K_NOT_in_createUserStatement7143 = new BitSet(new long[]{ 0L, 4L });

	public static final BitSet FOLLOW_K_EXISTS_in_createUserStatement7145 = new BitSet(new long[]{ 8388608L, 0L, 39582418599936L });

	public static final BitSet FOLLOW_username_in_createUserStatement7153 = new BitSet(new long[]{ 2L, 4398046511104L, 2147483680L });

	public static final BitSet FOLLOW_K_WITH_in_createUserStatement7165 = new BitSet(new long[]{ 0L, 2251799813685248L });

	public static final BitSet FOLLOW_userPassword_in_createUserStatement7167 = new BitSet(new long[]{ 2L, 4398046511104L, 32L });

	public static final BitSet FOLLOW_K_SUPERUSER_in_createUserStatement7181 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_K_NOSUPERUSER_in_createUserStatement7187 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_K_ALTER_in_alterUserStatement7232 = new BitSet(new long[]{ 0L, 0L, 4194304L });

	public static final BitSet FOLLOW_K_USER_in_alterUserStatement7234 = new BitSet(new long[]{ 8388608L, 0L, 39582418599936L });

	public static final BitSet FOLLOW_username_in_alterUserStatement7238 = new BitSet(new long[]{ 2L, 4398046511104L, 2147483680L });

	public static final BitSet FOLLOW_K_WITH_in_alterUserStatement7250 = new BitSet(new long[]{ 0L, 2251799813685248L });

	public static final BitSet FOLLOW_userPassword_in_alterUserStatement7252 = new BitSet(new long[]{ 2L, 4398046511104L, 32L });

	public static final BitSet FOLLOW_K_SUPERUSER_in_alterUserStatement7266 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_K_NOSUPERUSER_in_alterUserStatement7280 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_K_DROP_in_dropUserStatement7326 = new BitSet(new long[]{ 0L, 0L, 4194304L });

	public static final BitSet FOLLOW_K_USER_in_dropUserStatement7328 = new BitSet(new long[]{ 8388608L, 8192L, 39582418599936L });

	public static final BitSet FOLLOW_K_IF_in_dropUserStatement7331 = new BitSet(new long[]{ 0L, 4L });

	public static final BitSet FOLLOW_K_EXISTS_in_dropUserStatement7333 = new BitSet(new long[]{ 8388608L, 0L, 39582418599936L });

	public static final BitSet FOLLOW_username_in_dropUserStatement7341 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_K_LIST_in_listUsersStatement7366 = new BitSet(new long[]{ 0L, 0L, 8388608L });

	public static final BitSet FOLLOW_K_USERS_in_listUsersStatement7368 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_K_CREATE_in_createRoleStatement7402 = new BitSet(new long[]{ 0L, 1152921504606846976L });

	public static final BitSet FOLLOW_K_ROLE_in_createRoleStatement7404 = new BitSet(new long[]{ -5697204075003641856L, 3782044054195091132L, 41786252961791L });

	public static final BitSet FOLLOW_K_IF_in_createRoleStatement7407 = new BitSet(new long[]{ 0L, 8796093022208L });

	public static final BitSet FOLLOW_K_NOT_in_createRoleStatement7409 = new BitSet(new long[]{ 0L, 4L });

	public static final BitSet FOLLOW_K_EXISTS_in_createRoleStatement7411 = new BitSet(new long[]{ -5697204075003641856L, 3782044054195082940L, 41786252961791L });

	public static final BitSet FOLLOW_userOrRoleName_in_createRoleStatement7419 = new BitSet(new long[]{ 2L, 0L, 2147483648L });

	public static final BitSet FOLLOW_K_WITH_in_createRoleStatement7429 = new BitSet(new long[]{ 0L, 2392545891975168L, 32L });

	public static final BitSet FOLLOW_roleOptions_in_createRoleStatement7431 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_K_ALTER_in_alterRoleStatement7475 = new BitSet(new long[]{ 0L, 1152921504606846976L });

	public static final BitSet FOLLOW_K_ROLE_in_alterRoleStatement7477 = new BitSet(new long[]{ -5697204075003641856L, 3782044054195082940L, 41786252961791L });

	public static final BitSet FOLLOW_userOrRoleName_in_alterRoleStatement7481 = new BitSet(new long[]{ 2L, 0L, 2147483648L });

	public static final BitSet FOLLOW_K_WITH_in_alterRoleStatement7491 = new BitSet(new long[]{ 0L, 2392545891975168L, 32L });

	public static final BitSet FOLLOW_roleOptions_in_alterRoleStatement7493 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_K_DROP_in_dropRoleStatement7537 = new BitSet(new long[]{ 0L, 1152921504606846976L });

	public static final BitSet FOLLOW_K_ROLE_in_dropRoleStatement7539 = new BitSet(new long[]{ -5697204075003641856L, 3782044054195091132L, 41786252961791L });

	public static final BitSet FOLLOW_K_IF_in_dropRoleStatement7542 = new BitSet(new long[]{ 0L, 4L });

	public static final BitSet FOLLOW_K_EXISTS_in_dropRoleStatement7544 = new BitSet(new long[]{ -5697204075003641856L, 3782044054195082940L, 41786252961791L });

	public static final BitSet FOLLOW_userOrRoleName_in_dropRoleStatement7552 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_K_LIST_in_listRolesStatement7592 = new BitSet(new long[]{ 0L, 2305843009213693952L });

	public static final BitSet FOLLOW_K_ROLES_in_listRolesStatement7594 = new BitSet(new long[]{ 2L, 37383395344384L });

	public static final BitSet FOLLOW_K_OF_in_listRolesStatement7604 = new BitSet(new long[]{ -5697204075003641856L, 3782044054195082940L, 41786252961791L });

	public static final BitSet FOLLOW_roleName_in_listRolesStatement7606 = new BitSet(new long[]{ 2L, 2199023255552L });

	public static final BitSet FOLLOW_K_NORECURSIVE_in_listRolesStatement7619 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_roleOption_in_roleOptions7650 = new BitSet(new long[]{ 4294967298L });

	public static final BitSet FOLLOW_K_AND_in_roleOptions7654 = new BitSet(new long[]{ 0L, 2392545891975168L, 32L });

	public static final BitSet FOLLOW_roleOption_in_roleOptions7656 = new BitSet(new long[]{ 4294967298L });

	public static final BitSet FOLLOW_K_PASSWORD_in_roleOption7678 = new BitSet(new long[]{ 0L, 0L, 0L, 16L });

	public static final BitSet FOLLOW_196_in_roleOption7680 = new BitSet(new long[]{ 0L, 0L, 35184372088832L });

	public static final BitSet FOLLOW_STRING_LITERAL_in_roleOption7684 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_K_OPTIONS_in_roleOption7695 = new BitSet(new long[]{ 0L, 0L, 0L, 16L });

	public static final BitSet FOLLOW_196_in_roleOption7697 = new BitSet(new long[]{ 0L, 0L, 0L, 2048L });

	public static final BitSet FOLLOW_mapLiteral_in_roleOption7701 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_K_SUPERUSER_in_roleOption7712 = new BitSet(new long[]{ 0L, 0L, 0L, 16L });

	public static final BitSet FOLLOW_196_in_roleOption7714 = new BitSet(new long[]{ 64L });

	public static final BitSet FOLLOW_BOOLEAN_in_roleOption7718 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_K_LOGIN_in_roleOption7729 = new BitSet(new long[]{ 0L, 0L, 0L, 16L });

	public static final BitSet FOLLOW_196_in_roleOption7731 = new BitSet(new long[]{ 64L });

	public static final BitSet FOLLOW_BOOLEAN_in_roleOption7735 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_K_PASSWORD_in_userPassword7757 = new BitSet(new long[]{ 0L, 0L, 35184372088832L });

	public static final BitSet FOLLOW_STRING_LITERAL_in_userPassword7761 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_EMPTY_QUOTED_NAME_in_cident7793 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_IDENT_in_cident7808 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_QUOTED_NAME_in_cident7833 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_unreserved_keyword_in_cident7852 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_IDENT_in_schema_cident7877 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_QUOTED_NAME_in_schema_cident7902 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_unreserved_keyword_in_schema_cident7921 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_IDENT_in_ident7947 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_QUOTED_NAME_in_ident7972 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_unreserved_keyword_in_ident7991 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_IDENT_in_fident8016 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_QUOTED_NAME_in_fident8041 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_unreserved_keyword_in_fident8060 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_IDENT_in_noncol_ident8086 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_QUOTED_NAME_in_noncol_ident8111 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_unreserved_keyword_in_noncol_ident8130 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_ksName_in_keyspaceName8163 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_ksName_in_indexName8197 = new BitSet(new long[]{ 0L, 0L, -9223372036854775808L });

	public static final BitSet FOLLOW_191_in_indexName8200 = new BitSet(new long[]{ -5697204075003641856L, 3782044054195082940L, 6601880872959L });

	public static final BitSet FOLLOW_idxName_in_indexName8204 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_ksName_in_columnFamilyName8236 = new BitSet(new long[]{ 0L, 0L, -9223372036854775808L });

	public static final BitSet FOLLOW_191_in_columnFamilyName8239 = new BitSet(new long[]{ -5697204075003641856L, 3782044054195082940L, 6601880872959L });

	public static final BitSet FOLLOW_cfName_in_columnFamilyName8243 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_noncol_ident_in_userTypeName8268 = new BitSet(new long[]{ 0L, 0L, -9223372036854775808L });

	public static final BitSet FOLLOW_191_in_userTypeName8270 = new BitSet(new long[]{ 9939603108659200L, 3782044054176143004L, 4398126407741L });

	public static final BitSet FOLLOW_non_type_ident_in_userTypeName8276 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_roleName_in_userOrRoleName8308 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_IDENT_in_ksName8331 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_QUOTED_NAME_in_ksName8356 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_unreserved_keyword_in_ksName8375 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_QMARK_in_ksName8385 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_IDENT_in_cfName8407 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_QUOTED_NAME_in_cfName8432 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_unreserved_keyword_in_cfName8451 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_QMARK_in_cfName8461 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_IDENT_in_idxName8483 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_QUOTED_NAME_in_idxName8508 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_unreserved_keyword_in_idxName8527 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_QMARK_in_idxName8537 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_IDENT_in_roleName8559 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_STRING_LITERAL_in_roleName8584 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_QUOTED_NAME_in_roleName8600 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_unreserved_keyword_in_roleName8619 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_QMARK_in_roleName8629 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_STRING_LITERAL_in_constant8654 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_INTEGER_in_constant8666 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_FLOAT_in_constant8685 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_BOOLEAN_in_constant8706 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_DURATION_in_constant8725 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_UUID_in_constant8743 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_HEXNUMBER_in_constant8765 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_189_in_constant8783 = new BitSet(new long[]{ 0L, 549755944960L });

	public static final BitSet FOLLOW_set_in_constant8792 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_203_in_mapLiteral8821 = new BitSet(new long[]{ -5697204074984634304L, 3782062196137072316L, 2378223864481298431L, 6273L });

	public static final BitSet FOLLOW_term_in_mapLiteral8839 = new BitSet(new long[]{ 0L, 0L, 0L, 1L });

	public static final BitSet FOLLOW_192_in_mapLiteral8841 = new BitSet(new long[]{ -5697204074984634304L, 3782062196137072316L, 2378223864481298431L, 2177L });

	public static final BitSet FOLLOW_term_in_mapLiteral8845 = new BitSet(new long[]{ 0L, 0L, 1152921504606846976L, 4096L });

	public static final BitSet FOLLOW_188_in_mapLiteral8851 = new BitSet(new long[]{ -5697204074984634304L, 3782062196137072316L, 2378223864481298431L, 2177L });

	public static final BitSet FOLLOW_term_in_mapLiteral8855 = new BitSet(new long[]{ 0L, 0L, 0L, 1L });

	public static final BitSet FOLLOW_192_in_mapLiteral8857 = new BitSet(new long[]{ -5697204074984634304L, 3782062196137072316L, 2378223864481298431L, 2177L });

	public static final BitSet FOLLOW_term_in_mapLiteral8861 = new BitSet(new long[]{ 0L, 0L, 1152921504606846976L, 4096L });

	public static final BitSet FOLLOW_204_in_mapLiteral8877 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_192_in_setOrMapLiteral8901 = new BitSet(new long[]{ -5697204074984634304L, 3782062196137072316L, 2378223864481298431L, 2177L });

	public static final BitSet FOLLOW_term_in_setOrMapLiteral8905 = new BitSet(new long[]{ 2L, 0L, 1152921504606846976L });

	public static final BitSet FOLLOW_188_in_setOrMapLiteral8921 = new BitSet(new long[]{ -5697204074984634304L, 3782062196137072316L, 2378223864481298431L, 2177L });

	public static final BitSet FOLLOW_term_in_setOrMapLiteral8925 = new BitSet(new long[]{ 0L, 0L, 0L, 1L });

	public static final BitSet FOLLOW_192_in_setOrMapLiteral8927 = new BitSet(new long[]{ -5697204074984634304L, 3782062196137072316L, 2378223864481298431L, 2177L });

	public static final BitSet FOLLOW_term_in_setOrMapLiteral8931 = new BitSet(new long[]{ 2L, 0L, 1152921504606846976L });

	public static final BitSet FOLLOW_188_in_setOrMapLiteral8966 = new BitSet(new long[]{ -5697204074984634304L, 3782062196137072316L, 2378223864481298431L, 2177L });

	public static final BitSet FOLLOW_term_in_setOrMapLiteral8970 = new BitSet(new long[]{ 2L, 0L, 1152921504606846976L });

	public static final BitSet FOLLOW_199_in_collectionLiteral9004 = new BitSet(new long[]{ -5697204074984634304L, 3782062196137072316L, 2378223864481298431L, 2689L });

	public static final BitSet FOLLOW_term_in_collectionLiteral9022 = new BitSet(new long[]{ 0L, 0L, 1152921504606846976L, 512L });

	public static final BitSet FOLLOW_188_in_collectionLiteral9028 = new BitSet(new long[]{ -5697204074984634304L, 3782062196137072316L, 2378223864481298431L, 2177L });

	public static final BitSet FOLLOW_term_in_collectionLiteral9032 = new BitSet(new long[]{ 0L, 0L, 1152921504606846976L, 512L });

	public static final BitSet FOLLOW_201_in_collectionLiteral9048 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_203_in_collectionLiteral9058 = new BitSet(new long[]{ -5697204074984634304L, 3782062196137072316L, 2378223864481298431L, 2177L });

	public static final BitSet FOLLOW_term_in_collectionLiteral9062 = new BitSet(new long[]{ 0L, 0L, 1152921504606846976L, 4097L });

	public static final BitSet FOLLOW_setOrMapLiteral_in_collectionLiteral9066 = new BitSet(new long[]{ 0L, 0L, 0L, 4096L });

	public static final BitSet FOLLOW_204_in_collectionLiteral9071 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_203_in_collectionLiteral9089 = new BitSet(new long[]{ 0L, 0L, 0L, 4096L });

	public static final BitSet FOLLOW_204_in_collectionLiteral9091 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_203_in_usertypeLiteral9135 = new BitSet(new long[]{ -5697204075003641856L, 3782044054195082940L, 4402857617407L });

	public static final BitSet FOLLOW_fident_in_usertypeLiteral9139 = new BitSet(new long[]{ 0L, 0L, 0L, 1L });

	public static final BitSet FOLLOW_192_in_usertypeLiteral9141 = new BitSet(new long[]{ -5697204074984634304L, 3782062196137072316L, 2378223864481298431L, 2177L });

	public static final BitSet FOLLOW_term_in_usertypeLiteral9145 = new BitSet(new long[]{ 0L, 0L, 1152921504606846976L, 4096L });

	public static final BitSet FOLLOW_188_in_usertypeLiteral9151 = new BitSet(new long[]{ -5697204075003641856L, 3782044054195082940L, 4402857617407L });

	public static final BitSet FOLLOW_fident_in_usertypeLiteral9155 = new BitSet(new long[]{ 0L, 0L, 0L, 1L });

	public static final BitSet FOLLOW_192_in_usertypeLiteral9157 = new BitSet(new long[]{ -5697204074984634304L, 3782062196137072316L, 2378223864481298431L, 2177L });

	public static final BitSet FOLLOW_term_in_usertypeLiteral9161 = new BitSet(new long[]{ 0L, 0L, 1152921504606846976L, 4096L });

	public static final BitSet FOLLOW_204_in_usertypeLiteral9168 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_184_in_tupleLiteral9205 = new BitSet(new long[]{ -5697204074984634304L, 3782062196137072316L, 2378223864481298431L, 2177L });

	public static final BitSet FOLLOW_term_in_tupleLiteral9209 = new BitSet(new long[]{ 0L, 0L, 1297036692682702848L });

	public static final BitSet FOLLOW_188_in_tupleLiteral9215 = new BitSet(new long[]{ -5697204074984634304L, 3782062196137072316L, 2378223864481298431L, 2177L });

	public static final BitSet FOLLOW_term_in_tupleLiteral9219 = new BitSet(new long[]{ 0L, 0L, 1297036692682702848L });

	public static final BitSet FOLLOW_185_in_tupleLiteral9226 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_constant_in_value9249 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_collectionLiteral_in_value9271 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_usertypeLiteral_in_value9284 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_tupleLiteral_in_value9299 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_K_NULL_in_value9315 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_192_in_value9339 = new BitSet(new long[]{ -5697204075003641856L, 3782044054195082940L, 4402857617407L });

	public static final BitSet FOLLOW_noncol_ident_in_value9343 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_QMARK_in_value9354 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_INTEGER_in_intValue9394 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_192_in_intValue9408 = new BitSet(new long[]{ -5697204075003641856L, 3782044054195082940L, 4402857617407L });

	public static final BitSet FOLLOW_noncol_ident_in_intValue9412 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_QMARK_in_intValue9423 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_keyspaceName_in_functionName9469 = new BitSet(new long[]{ 0L, 0L, -9223372036854775808L });

	public static final BitSet FOLLOW_191_in_functionName9471 = new BitSet(new long[]{ -6850160763982577664L, 3782044054144751292L, 4398562621439L });

	public static final BitSet FOLLOW_allowedFunctionName_in_functionName9477 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_IDENT_in_allowedFunctionName9504 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_QUOTED_NAME_in_allowedFunctionName9538 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_unreserved_function_keyword_in_allowedFunctionName9566 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_K_TOKEN_in_allowedFunctionName9576 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_K_COUNT_in_allowedFunctionName9608 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_functionName_in_function9655 = new BitSet(new long[]{ 0L, 0L, 72057594037927936L });

	public static final BitSet FOLLOW_184_in_function9657 = new BitSet(new long[]{ 0L, 0L, 144115188075855872L });

	public static final BitSet FOLLOW_185_in_function9659 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_functionName_in_function9689 = new BitSet(new long[]{ 0L, 0L, 72057594037927936L });

	public static final BitSet FOLLOW_184_in_function9691 = new BitSet(new long[]{ -5697204074984634304L, 3782062196137072316L, 2378223864481298431L, 2177L });

	public static final BitSet FOLLOW_functionArgs_in_function9695 = new BitSet(new long[]{ 0L, 0L, 144115188075855872L });

	public static final BitSet FOLLOW_185_in_function9697 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_term_in_functionArgs9730 = new BitSet(new long[]{ 2L, 0L, 1152921504606846976L });

	public static final BitSet FOLLOW_188_in_functionArgs9736 = new BitSet(new long[]{ -5697204074984634304L, 3782062196137072316L, 2378223864481298431L, 2177L });

	public static final BitSet FOLLOW_term_in_functionArgs9740 = new BitSet(new long[]{ 2L, 0L, 1152921504606846976L });

	public static final BitSet FOLLOW_value_in_term9768 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_function_in_term9805 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_184_in_term9837 = new BitSet(new long[]{ -5697204075003641856L, -5441327982659692868L, 39587229706239L });

	public static final BitSet FOLLOW_comparatorType_in_term9841 = new BitSet(new long[]{ 0L, 0L, 144115188075855872L });

	public static final BitSet FOLLOW_185_in_term9843 = new BitSet(new long[]{ -5697204074984634304L, 3782062196137072316L, 2378223864481298431L, 2177L });

	public static final BitSet FOLLOW_term_in_term9847 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_cident_in_columnOperation9870 = new BitSet(new long[]{ 0L, 0L, -4035225266123964416L, 144L });

	public static final BitSet FOLLOW_columnOperationDifferentiator_in_columnOperation9872 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_196_in_columnOperationDifferentiator9891 = new BitSet(new long[]{ -5697204074984617920L, 3782062196137072316L, 2378223864481298431L, 2177L });

	public static final BitSet FOLLOW_normalColumnOperation_in_columnOperationDifferentiator9893 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_shorthandColumnOperation_in_columnOperationDifferentiator9902 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_199_in_columnOperationDifferentiator9911 = new BitSet(new long[]{ -5697204074984634304L, 3782062196137072316L, 2378223864481298431L, 2177L });

	public static final BitSet FOLLOW_term_in_columnOperationDifferentiator9915 = new BitSet(new long[]{ 0L, 0L, 0L, 512L });

	public static final BitSet FOLLOW_201_in_columnOperationDifferentiator9917 = new BitSet(new long[]{ 0L, 0L, 0L, 16L });

	public static final BitSet FOLLOW_collectionColumnOperation_in_columnOperationDifferentiator9919 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_191_in_columnOperationDifferentiator9928 = new BitSet(new long[]{ -5697204075003641856L, 3782044054195082940L, 4402857617407L });

	public static final BitSet FOLLOW_fident_in_columnOperationDifferentiator9932 = new BitSet(new long[]{ 0L, 0L, 0L, 16L });

	public static final BitSet FOLLOW_udtColumnOperation_in_columnOperationDifferentiator9934 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_term_in_normalColumnOperation9955 = new BitSet(new long[]{ 2L, 0L, 288230376151711744L });

	public static final BitSet FOLLOW_186_in_normalColumnOperation9958 = new BitSet(new long[]{ -5697204075003625472L, 3782044054195082940L, 4402857617407L });

	public static final BitSet FOLLOW_cident_in_normalColumnOperation9962 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_cident_in_normalColumnOperation9983 = new BitSet(new long[]{ 0L, 0L, 2594073385365405696L });

	public static final BitSet FOLLOW_set_in_normalColumnOperation9987 = new BitSet(new long[]{ -5697204074984634304L, 3782062196137072316L, 2378223864481298431L, 2177L });

	public static final BitSet FOLLOW_term_in_normalColumnOperation9997 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_cident_in_normalColumnOperation10015 = new BitSet(new long[]{ 16777216L });

	public static final BitSet FOLLOW_INTEGER_in_normalColumnOperation10019 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_set_in_shorthandColumnOperation10047 = new BitSet(new long[]{ -5697204074984634304L, 3782062196137072316L, 2378223864481298431L, 2177L });

	public static final BitSet FOLLOW_term_in_shorthandColumnOperation10057 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_196_in_collectionColumnOperation10083 = new BitSet(new long[]{ -5697204074984634304L, 3782062196137072316L, 2378223864481298431L, 2177L });

	public static final BitSet FOLLOW_term_in_collectionColumnOperation10087 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_196_in_udtColumnOperation10113 = new BitSet(new long[]{ -5697204074984634304L, 3782062196137072316L, 2378223864481298431L, 2177L });

	public static final BitSet FOLLOW_term_in_udtColumnOperation10117 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_cident_in_columnCondition10150 = new BitSet(new long[]{ 0L, 16384L, -9187343239835811840L, 252L });

	public static final BitSet FOLLOW_relationType_in_columnCondition10164 = new BitSet(new long[]{ -5697204074984634304L, 3782062196137072316L, 2378223864481298431L, 2177L });

	public static final BitSet FOLLOW_term_in_columnCondition10168 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_K_IN_in_columnCondition10182 = new BitSet(new long[]{ 0L, 0L, 72059793061183488L, 1L });

	public static final BitSet FOLLOW_singleColumnInValues_in_columnCondition10200 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_inMarker_in_columnCondition10220 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_199_in_columnCondition10248 = new BitSet(new long[]{ -5697204074984634304L, 3782062196137072316L, 2378223864481298431L, 2177L });

	public static final BitSet FOLLOW_term_in_columnCondition10252 = new BitSet(new long[]{ 0L, 0L, 0L, 512L });

	public static final BitSet FOLLOW_201_in_columnCondition10254 = new BitSet(new long[]{ 0L, 16384L, 36028797018963968L, 124L });

	public static final BitSet FOLLOW_relationType_in_columnCondition10272 = new BitSet(new long[]{ -5697204074984634304L, 3782062196137072316L, 2378223864481298431L, 2177L });

	public static final BitSet FOLLOW_term_in_columnCondition10276 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_K_IN_in_columnCondition10294 = new BitSet(new long[]{ 0L, 0L, 72059793061183488L, 1L });

	public static final BitSet FOLLOW_singleColumnInValues_in_columnCondition10316 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_inMarker_in_columnCondition10340 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_191_in_columnCondition10386 = new BitSet(new long[]{ -5697204075003641856L, 3782044054195082940L, 4402857617407L });

	public static final BitSet FOLLOW_fident_in_columnCondition10390 = new BitSet(new long[]{ 0L, 16384L, 36028797018963968L, 124L });

	public static final BitSet FOLLOW_relationType_in_columnCondition10408 = new BitSet(new long[]{ -5697204074984634304L, 3782062196137072316L, 2378223864481298431L, 2177L });

	public static final BitSet FOLLOW_term_in_columnCondition10412 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_K_IN_in_columnCondition10430 = new BitSet(new long[]{ 0L, 0L, 72059793061183488L, 1L });

	public static final BitSet FOLLOW_singleColumnInValues_in_columnCondition10452 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_inMarker_in_columnCondition10476 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_property_in_properties10538 = new BitSet(new long[]{ 4294967298L });

	public static final BitSet FOLLOW_K_AND_in_properties10542 = new BitSet(new long[]{ -5697204075003641856L, 3782044054195082940L, 4402857617407L });

	public static final BitSet FOLLOW_property_in_properties10544 = new BitSet(new long[]{ 4294967298L });

	public static final BitSet FOLLOW_noncol_ident_in_property10567 = new BitSet(new long[]{ 0L, 0L, 0L, 16L });

	public static final BitSet FOLLOW_196_in_property10569 = new BitSet(new long[]{ -5697204074993022912L, 3782044603951027900L, 2306159673373599743L });

	public static final BitSet FOLLOW_propertyValue_in_property10573 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_noncol_ident_in_property10585 = new BitSet(new long[]{ 0L, 0L, 0L, 16L });

	public static final BitSet FOLLOW_196_in_property10587 = new BitSet(new long[]{ 0L, 0L, 0L, 2048L });

	public static final BitSet FOLLOW_mapLiteral_in_property10591 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_constant_in_propertyValue10616 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_unreserved_keyword_in_propertyValue10638 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_196_in_relationType10661 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_194_in_relationType10672 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_195_in_relationType10683 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_197_in_relationType10693 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_198_in_relationType10704 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_183_in_relationType10714 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_cident_in_relation10736 = new BitSet(new long[]{ 0L, 0L, 36028797018963968L, 124L });

	public static final BitSet FOLLOW_relationType_in_relation10740 = new BitSet(new long[]{ -5697204074984634304L, 3782062196137072316L, 2378223864481298431L, 2177L });

	public static final BitSet FOLLOW_term_in_relation10744 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_cident_in_relation10756 = new BitSet(new long[]{ 0L, 1073741824L });

	public static final BitSet FOLLOW_K_LIKE_in_relation10758 = new BitSet(new long[]{ -5697204074984634304L, 3782062196137072316L, 2378223864481298431L, 2177L });

	public static final BitSet FOLLOW_term_in_relation10762 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_cident_in_relation10774 = new BitSet(new long[]{ 0L, 8388608L });

	public static final BitSet FOLLOW_K_IS_in_relation10776 = new BitSet(new long[]{ 0L, 8796093022208L });

	public static final BitSet FOLLOW_K_NOT_in_relation10778 = new BitSet(new long[]{ 0L, 17592186044416L });

	public static final BitSet FOLLOW_K_NULL_in_relation10780 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_K_TOKEN_in_relation10790 = new BitSet(new long[]{ 0L, 0L, 72057594037927936L });

	public static final BitSet FOLLOW_tupleOfIdentifiers_in_relation10794 = new BitSet(new long[]{ 0L, 0L, 36028797018963968L, 124L });

	public static final BitSet FOLLOW_relationType_in_relation10798 = new BitSet(new long[]{ -5697204074984634304L, 3782062196137072316L, 2378223864481298431L, 2177L });

	public static final BitSet FOLLOW_term_in_relation10802 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_cident_in_relation10822 = new BitSet(new long[]{ 0L, 16384L });

	public static final BitSet FOLLOW_K_IN_in_relation10824 = new BitSet(new long[]{ 0L, 0L, 2199023255552L, 1L });

	public static final BitSet FOLLOW_inMarker_in_relation10828 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_cident_in_relation10848 = new BitSet(new long[]{ 0L, 16384L });

	public static final BitSet FOLLOW_K_IN_in_relation10850 = new BitSet(new long[]{ 0L, 0L, 72057594037927936L });

	public static final BitSet FOLLOW_singleColumnInValues_in_relation10854 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_cident_in_relation10874 = new BitSet(new long[]{ 562949953421312L });

	public static final BitSet FOLLOW_K_CONTAINS_in_relation10876 = new BitSet(new long[]{ -5697204074984634304L, 3782062196137072316L, 2378223864481298431L, 2177L });

	public static final BitSet FOLLOW_K_KEY_in_relation10881 = new BitSet(new long[]{ -5697204074984634304L, 3782062196137072316L, 2378223864481298431L, 2177L });

	public static final BitSet FOLLOW_term_in_relation10897 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_cident_in_relation10909 = new BitSet(new long[]{ 0L, 0L, 0L, 128L });

	public static final BitSet FOLLOW_199_in_relation10911 = new BitSet(new long[]{ -5697204074984634304L, 3782062196137072316L, 2378223864481298431L, 2177L });

	public static final BitSet FOLLOW_term_in_relation10915 = new BitSet(new long[]{ 0L, 0L, 0L, 512L });

	public static final BitSet FOLLOW_201_in_relation10917 = new BitSet(new long[]{ 0L, 0L, 36028797018963968L, 124L });

	public static final BitSet FOLLOW_relationType_in_relation10921 = new BitSet(new long[]{ -5697204074984634304L, 3782062196137072316L, 2378223864481298431L, 2177L });

	public static final BitSet FOLLOW_term_in_relation10925 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_tupleOfIdentifiers_in_relation10937 = new BitSet(new long[]{ 0L, 16384L, 36028797018963968L, 124L });

	public static final BitSet FOLLOW_K_IN_in_relation10947 = new BitSet(new long[]{ 0L, 0L, 72059793061183488L, 1L });

	public static final BitSet FOLLOW_184_in_relation10961 = new BitSet(new long[]{ 0L, 0L, 144115188075855872L });

	public static final BitSet FOLLOW_185_in_relation10963 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_inMarkerForTuple_in_relation10995 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_tupleOfTupleLiterals_in_relation11029 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_tupleOfMarkersForTuples_in_relation11063 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_relationType_in_relation11105 = new BitSet(new long[]{ 0L, 0L, 72057594037927936L });

	public static final BitSet FOLLOW_tupleLiteral_in_relation11109 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_relationType_in_relation11135 = new BitSet(new long[]{ 0L, 0L, 2199023255552L, 1L });

	public static final BitSet FOLLOW_markerForTuple_in_relation11139 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_184_in_relation11169 = new BitSet(new long[]{ -5697204075003625472L, 3782044054195082940L, 72061996895549439L });

	public static final BitSet FOLLOW_relation_in_relation11171 = new BitSet(new long[]{ 0L, 0L, 144115188075855872L });

	public static final BitSet FOLLOW_185_in_relation11174 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_QMARK_in_inMarker11195 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_192_in_inMarker11205 = new BitSet(new long[]{ -5697204075003641856L, 3782044054195082940L, 4402857617407L });

	public static final BitSet FOLLOW_noncol_ident_in_inMarker11209 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_184_in_tupleOfIdentifiers11241 = new BitSet(new long[]{ -5697204075003625472L, 3782044054195082940L, 4402857617407L });

	public static final BitSet FOLLOW_cident_in_tupleOfIdentifiers11245 = new BitSet(new long[]{ 0L, 0L, 1297036692682702848L });

	public static final BitSet FOLLOW_188_in_tupleOfIdentifiers11250 = new BitSet(new long[]{ -5697204075003625472L, 3782044054195082940L, 4402857617407L });

	public static final BitSet FOLLOW_cident_in_tupleOfIdentifiers11254 = new BitSet(new long[]{ 0L, 0L, 1297036692682702848L });

	public static final BitSet FOLLOW_185_in_tupleOfIdentifiers11260 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_184_in_singleColumnInValues11290 = new BitSet(new long[]{ -5697204074984634304L, 3782062196137072316L, 2522339052557154303L, 2177L });

	public static final BitSet FOLLOW_term_in_singleColumnInValues11298 = new BitSet(new long[]{ 0L, 0L, 1297036692682702848L });

	public static final BitSet FOLLOW_188_in_singleColumnInValues11303 = new BitSet(new long[]{ -5697204074984634304L, 3782062196137072316L, 2378223864481298431L, 2177L });

	public static final BitSet FOLLOW_term_in_singleColumnInValues11307 = new BitSet(new long[]{ 0L, 0L, 1297036692682702848L });

	public static final BitSet FOLLOW_185_in_singleColumnInValues11316 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_184_in_tupleOfTupleLiterals11346 = new BitSet(new long[]{ 0L, 0L, 72057594037927936L });

	public static final BitSet FOLLOW_tupleLiteral_in_tupleOfTupleLiterals11350 = new BitSet(new long[]{ 0L, 0L, 1297036692682702848L });

	public static final BitSet FOLLOW_188_in_tupleOfTupleLiterals11355 = new BitSet(new long[]{ 0L, 0L, 72057594037927936L });

	public static final BitSet FOLLOW_tupleLiteral_in_tupleOfTupleLiterals11359 = new BitSet(new long[]{ 0L, 0L, 1297036692682702848L });

	public static final BitSet FOLLOW_185_in_tupleOfTupleLiterals11365 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_QMARK_in_markerForTuple11386 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_192_in_markerForTuple11396 = new BitSet(new long[]{ -5697204075003641856L, 3782044054195082940L, 4402857617407L });

	public static final BitSet FOLLOW_noncol_ident_in_markerForTuple11400 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_184_in_tupleOfMarkersForTuples11432 = new BitSet(new long[]{ 0L, 0L, 2199023255552L, 1L });

	public static final BitSet FOLLOW_markerForTuple_in_tupleOfMarkersForTuples11436 = new BitSet(new long[]{ 0L, 0L, 1297036692682702848L });

	public static final BitSet FOLLOW_188_in_tupleOfMarkersForTuples11441 = new BitSet(new long[]{ 0L, 0L, 2199023255552L, 1L });

	public static final BitSet FOLLOW_markerForTuple_in_tupleOfMarkersForTuples11445 = new BitSet(new long[]{ 0L, 0L, 1297036692682702848L });

	public static final BitSet FOLLOW_185_in_tupleOfMarkersForTuples11451 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_QMARK_in_inMarkerForTuple11472 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_192_in_inMarkerForTuple11482 = new BitSet(new long[]{ -5697204075003641856L, 3782044054195082940L, 4402857617407L });

	public static final BitSet FOLLOW_noncol_ident_in_inMarkerForTuple11486 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_native_type_in_comparatorType11511 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_collection_type_in_comparatorType11527 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_tuple_type_in_comparatorType11539 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_userTypeName_in_comparatorType11555 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_K_FROZEN_in_comparatorType11567 = new BitSet(new long[]{ 0L, 0L, 0L, 4L });

	public static final BitSet FOLLOW_194_in_comparatorType11569 = new BitSet(new long[]{ -5697204075003641856L, -5441327982659692868L, 39587229706239L });

	public static final BitSet FOLLOW_comparatorType_in_comparatorType11573 = new BitSet(new long[]{ 0L, 0L, 0L, 32L });

	public static final BitSet FOLLOW_197_in_comparatorType11575 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_STRING_LITERAL_in_comparatorType11593 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_K_ASCII_in_native_type11622 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_K_BIGINT_in_native_type11636 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_K_BLOB_in_native_type11649 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_K_BOOLEAN_in_native_type11664 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_K_COUNTER_in_native_type11676 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_K_DECIMAL_in_native_type11688 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_K_DOUBLE_in_native_type11700 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_K_DURATION_in_native_type11713 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_K_FLOAT_in_native_type11726 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_K_INET_in_native_type11740 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_K_INT_in_native_type11755 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_K_SMALLINT_in_native_type11771 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_K_TEXT_in_native_type11782 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_K_TIMESTAMP_in_native_type11797 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_K_TINYINT_in_native_type11807 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_K_UUID_in_native_type11819 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_K_VARCHAR_in_native_type11834 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_K_VARINT_in_native_type11846 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_K_TIMEUUID_in_native_type11859 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_K_DATE_in_native_type11870 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_K_TIME_in_native_type11885 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_K_MAP_in_collection_type11913 = new BitSet(new long[]{ 0L, 0L, 0L, 4L });

	public static final BitSet FOLLOW_194_in_collection_type11916 = new BitSet(new long[]{ -5697204075003641856L, -5441327982659692868L, 39587229706239L });

	public static final BitSet FOLLOW_comparatorType_in_collection_type11920 = new BitSet(new long[]{ 0L, 0L, 1152921504606846976L });

	public static final BitSet FOLLOW_188_in_collection_type11922 = new BitSet(new long[]{ -5697204075003641856L, -5441327982659692868L, 39587229706239L });

	public static final BitSet FOLLOW_comparatorType_in_collection_type11926 = new BitSet(new long[]{ 0L, 0L, 0L, 32L });

	public static final BitSet FOLLOW_197_in_collection_type11928 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_K_LIST_in_collection_type11946 = new BitSet(new long[]{ 0L, 0L, 0L, 4L });

	public static final BitSet FOLLOW_194_in_collection_type11948 = new BitSet(new long[]{ -5697204075003641856L, -5441327982659692868L, 39587229706239L });

	public static final BitSet FOLLOW_comparatorType_in_collection_type11952 = new BitSet(new long[]{ 0L, 0L, 0L, 32L });

	public static final BitSet FOLLOW_197_in_collection_type11954 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_K_SET_in_collection_type11972 = new BitSet(new long[]{ 0L, 0L, 0L, 4L });

	public static final BitSet FOLLOW_194_in_collection_type11975 = new BitSet(new long[]{ -5697204075003641856L, -5441327982659692868L, 39587229706239L });

	public static final BitSet FOLLOW_comparatorType_in_collection_type11979 = new BitSet(new long[]{ 0L, 0L, 0L, 32L });

	public static final BitSet FOLLOW_197_in_collection_type11981 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_K_TUPLE_in_tuple_type12012 = new BitSet(new long[]{ 0L, 0L, 0L, 4L });

	public static final BitSet FOLLOW_194_in_tuple_type12014 = new BitSet(new long[]{ -5697204075003641856L, -5441327982659692868L, 39587229706239L });

	public static final BitSet FOLLOW_comparatorType_in_tuple_type12029 = new BitSet(new long[]{ 0L, 0L, 1152921504606846976L, 32L });

	public static final BitSet FOLLOW_188_in_tuple_type12034 = new BitSet(new long[]{ -5697204075003641856L, -5441327982659692868L, 39587229706239L });

	public static final BitSet FOLLOW_comparatorType_in_tuple_type12038 = new BitSet(new long[]{ 0L, 0L, 1152921504606846976L, 32L });

	public static final BitSet FOLLOW_197_in_tuple_type12050 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_IDENT_in_username12069 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_STRING_LITERAL_in_username12077 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_QUOTED_NAME_in_username12085 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_STRING_LITERAL_in_mbean12104 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_IDENT_in_non_type_ident12129 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_QUOTED_NAME_in_non_type_ident12160 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_basic_unreserved_keyword_in_non_type_ident12185 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_K_KEY_in_non_type_ident12197 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_unreserved_function_keyword_in_unreserved_keyword12240 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_set_in_unreserved_keyword12256 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_basic_unreserved_keyword_in_unreserved_function_keyword12307 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_native_type_in_unreserved_function_keyword12319 = new BitSet(new long[]{ 2L });

	public static final BitSet FOLLOW_set_in_basic_unreserved_keyword12357 = new BitSet(new long[]{ 2L });
}

