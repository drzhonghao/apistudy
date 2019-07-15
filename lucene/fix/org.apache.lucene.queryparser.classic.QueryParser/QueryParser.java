

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.queryparser.classic.CharStream;
import org.apache.lucene.queryparser.classic.FastCharStream;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParserBase;
import org.apache.lucene.queryparser.classic.QueryParserConstants;
import org.apache.lucene.queryparser.classic.QueryParserTokenManager;
import org.apache.lucene.queryparser.classic.Token;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.Query;


public class QueryParser extends QueryParserBase implements QueryParserConstants {
	public static enum Operator {

		OR,
		AND;}

	public static final boolean DEFAULT_SPLIT_ON_WHITESPACE = false;

	public QueryParser(String f, Analyzer a) {
		this(new FastCharStream(new StringReader("")));
		init(f, a);
	}

	@Override
	public void setAutoGeneratePhraseQueries(boolean value) {
		if (((splitOnWhitespace) == false) && (value == true)) {
			throw new IllegalArgumentException("setAutoGeneratePhraseQueries(true) is disallowed when getSplitOnWhitespace() == false");
		}
	}

	public boolean getSplitOnWhitespace() {
		return splitOnWhitespace;
	}

	public void setSplitOnWhitespace(boolean splitOnWhitespace) {
		if ((splitOnWhitespace == false) && ((getAutoGeneratePhraseQueries()) == true)) {
			throw new IllegalArgumentException("setSplitOnWhitespace(false) is disallowed when getAutoGeneratePhraseQueries() == true");
		}
		this.splitOnWhitespace = splitOnWhitespace;
	}

	private boolean splitOnWhitespace = QueryParser.DEFAULT_SPLIT_ON_WHITESPACE;

	private static Set<Integer> disallowedPostMultiTerm = new HashSet<Integer>(Arrays.asList(QueryParserConstants.COLON, QueryParserConstants.STAR, QueryParserConstants.FUZZY_SLOP, QueryParserConstants.CARAT, QueryParserConstants.AND, QueryParserConstants.OR));

	private static boolean allowedPostMultiTerm(int tokenKind) {
		return (QueryParser.disallowedPostMultiTerm.contains(tokenKind)) == false;
	}

	public final int Conjunction() throws ParseException {
		{
			if (true) {
			}
		}
		throw new Error("Missing return statement in function");
	}

	public final int Modifiers() throws ParseException {
		{
			if (true) {
			}
		}
		throw new Error("Missing return statement in function");
	}

	public final Query TopLevelQuery(String field) throws ParseException {
		Query q;
		q = Query(field);
		jj_consume_token(0);
		{
			if (true)
				return q;

		}
		throw new Error("Missing return statement in function");
	}

	public final Query Query(String field) throws ParseException {
		List<BooleanClause> clauses = new ArrayList<BooleanClause>();
		Query q;
		Query firstQuery = null;
		int conj;
		int mods;
		if (jj_2_1(2)) {
			firstQuery = MultiTerm(field, clauses);
		}else {
		}
		label_1 : while (true) {
			switch ((jj_ntk) == (-1) ? jj_ntk() : jj_ntk) {
				case QueryParserConstants.AND :
				case QueryParserConstants.OR :
				case QueryParserConstants.NOT :
				case QueryParserConstants.PLUS :
				case QueryParserConstants.MINUS :
				case QueryParserConstants.BAREOPER :
				case QueryParserConstants.LPAREN :
				case QueryParserConstants.STAR :
				case QueryParserConstants.QUOTED :
				case QueryParserConstants.TERM :
				case QueryParserConstants.PREFIXTERM :
				case QueryParserConstants.WILDTERM :
				case QueryParserConstants.REGEXPTERM :
				case QueryParserConstants.RANGEIN_START :
				case QueryParserConstants.RANGEEX_START :
				case QueryParserConstants.NUMBER :
					break;
				default :
					jj_la1[5] = jj_gen;
					break label_1;
			}
			if (jj_2_2(2)) {
				MultiTerm(field, clauses);
			}else {
				switch ((jj_ntk) == (-1) ? jj_ntk() : jj_ntk) {
					case QueryParserConstants.AND :
					case QueryParserConstants.OR :
					case QueryParserConstants.NOT :
					case QueryParserConstants.PLUS :
					case QueryParserConstants.MINUS :
					case QueryParserConstants.BAREOPER :
					case QueryParserConstants.LPAREN :
					case QueryParserConstants.STAR :
					case QueryParserConstants.QUOTED :
					case QueryParserConstants.TERM :
					case QueryParserConstants.PREFIXTERM :
					case QueryParserConstants.WILDTERM :
					case QueryParserConstants.REGEXPTERM :
					case QueryParserConstants.RANGEIN_START :
					case QueryParserConstants.RANGEEX_START :
					case QueryParserConstants.NUMBER :
						conj = Conjunction();
						mods = Modifiers();
						q = Clause(field);
						addClause(clauses, conj, mods, q);
						break;
					default :
						jj_la1[6] = jj_gen;
						jj_consume_token((-1));
						throw new ParseException();
				}
			}
		} 
		if (((clauses.size()) == 1) && (firstQuery != null)) {
			{
				if (true)
					return firstQuery;

			}
		}else {
			{
				if (true)
					return getBooleanQuery(clauses);

			}
		}
		throw new Error("Missing return statement in function");
	}

	public final Query Clause(String field) throws ParseException {
		Query q;
		Token fieldToken = null;
		Token boost = null;
		if (jj_2_3(2)) {
		}else {
		}
		switch ((jj_ntk) == (-1) ? jj_ntk() : jj_ntk) {
			case QueryParserConstants.BAREOPER :
			case QueryParserConstants.STAR :
			case QueryParserConstants.QUOTED :
			case QueryParserConstants.TERM :
			case QueryParserConstants.PREFIXTERM :
			case QueryParserConstants.WILDTERM :
			case QueryParserConstants.REGEXPTERM :
			case QueryParserConstants.RANGEIN_START :
			case QueryParserConstants.RANGEEX_START :
			case QueryParserConstants.NUMBER :
				q = Term(field);
				break;
			case QueryParserConstants.LPAREN :
				jj_consume_token(QueryParserConstants.LPAREN);
				q = Query(field);
				jj_consume_token(QueryParserConstants.RPAREN);
				switch ((jj_ntk) == (-1) ? jj_ntk() : jj_ntk) {
					case QueryParserConstants.CARAT :
						jj_consume_token(QueryParserConstants.CARAT);
						boost = jj_consume_token(QueryParserConstants.NUMBER);
						break;
					default :
						jj_la1[8] = jj_gen;
				}
				break;
			default :
				jj_la1[9] = jj_gen;
				jj_consume_token((-1));
				throw new ParseException();
		}
		{
			if (true) {
			}
		}
		throw new Error("Missing return statement in function");
	}

	public final Query Term(String field) throws ParseException {
		Token term;
		Token boost = null;
		Token fuzzySlop = null;
		Token goop1;
		Token goop2;
		boolean prefix = false;
		boolean wildcard = false;
		boolean fuzzy = false;
		boolean regexp = false;
		boolean startInc = false;
		boolean endInc = false;
		Query q;
		{
			if (true) {
			}
		}
		throw new Error("Missing return statement in function");
	}

	public final Query MultiTerm(String field, List<BooleanClause> clauses) throws ParseException {
		Token text;
		Token whitespace;
		Token followingText;
		Query firstQuery = null;
		text = jj_consume_token(QueryParserConstants.TERM);
		if (splitOnWhitespace) {
		}
		if (((getToken(1).kind) == (QueryParserConstants.TERM)) && (QueryParser.allowedPostMultiTerm(getToken(2).kind))) {
		}else {
			jj_consume_token((-1));
			throw new ParseException();
		}
		label_2 : while (true) {
			followingText = jj_consume_token(QueryParserConstants.TERM);
			if (splitOnWhitespace) {
			}else {
				text.image += " " + (followingText.image);
			}
			if (((getToken(1).kind) == (QueryParserConstants.TERM)) && (QueryParser.allowedPostMultiTerm(getToken(2).kind))) {
			}else {
				break label_2;
			}
		} 
		if ((splitOnWhitespace) == false) {
			addMultiTermClauses(clauses, firstQuery);
		}
		{
			if (true)
				return firstQuery;

		}
		throw new Error("Missing return statement in function");
	}

	private boolean jj_2_1(int xla) {
		jj_la = xla;
		jj_lastpos = jj_scanpos = token;
		try {
			return !(jj_3_1());
		} catch (QueryParser.LookaheadSuccess ls) {
			return true;
		} finally {
			jj_save(0, xla);
		}
	}

	private boolean jj_2_2(int xla) {
		jj_la = xla;
		jj_lastpos = jj_scanpos = token;
		try {
			return !(jj_3_2());
		} catch (QueryParser.LookaheadSuccess ls) {
			return true;
		} finally {
			jj_save(1, xla);
		}
	}

	private boolean jj_2_3(int xla) {
		jj_la = xla;
		jj_lastpos = jj_scanpos = token;
		try {
			return !(jj_3_3());
		} catch (QueryParser.LookaheadSuccess ls) {
			return true;
		} finally {
			jj_save(2, xla);
		}
	}

	private boolean jj_3R_3() {
		if (jj_scan_token(QueryParserConstants.TERM))
			return true;

		jj_lookingAhead = true;
		jj_semLA = ((getToken(1).kind) == (QueryParserConstants.TERM)) && (QueryParser.allowedPostMultiTerm(getToken(2).kind));
		jj_lookingAhead = false;
		if ((!(jj_semLA)) || (jj_3R_6()))
			return true;

		Token xsp;
		if (jj_3R_7())
			return true;

		while (true) {
			xsp = jj_scanpos;
			if (jj_3R_7()) {
				jj_scanpos = xsp;
				break;
			}
		} 
		return false;
	}

	private boolean jj_3R_6() {
		return false;
	}

	private boolean jj_3R_5() {
		if (jj_scan_token(QueryParserConstants.STAR))
			return true;

		if (jj_scan_token(QueryParserConstants.COLON))
			return true;

		return false;
	}

	private boolean jj_3R_4() {
		if (jj_scan_token(QueryParserConstants.TERM))
			return true;

		if (jj_scan_token(QueryParserConstants.COLON))
			return true;

		return false;
	}

	private boolean jj_3_2() {
		if (jj_3R_3())
			return true;

		return false;
	}

	private boolean jj_3_1() {
		if (jj_3R_3())
			return true;

		return false;
	}

	private boolean jj_3R_7() {
		if (jj_scan_token(QueryParserConstants.TERM))
			return true;

		return false;
	}

	private boolean jj_3_3() {
		Token xsp;
		xsp = jj_scanpos;
		if (jj_3R_4()) {
			jj_scanpos = xsp;
			if (jj_3R_5())
				return true;

		}
		return false;
	}

	public QueryParserTokenManager token_source;

	public Token token;

	public Token jj_nt;

	private int jj_ntk;

	private Token jj_scanpos;

	private Token jj_lastpos;

	private int jj_la;

	private boolean jj_lookingAhead = false;

	private boolean jj_semLA;

	private int jj_gen;

	private final int[] jj_la1 = new int[25];

	private static int[] jj_la1_0;

	private static int[] jj_la1_1;

	static {
		QueryParser.jj_la1_init_0();
		QueryParser.jj_la1_init_1();
	}

	private static void jj_la1_init_0() {
		QueryParser.jj_la1_0 = new int[]{ 768, 768, 7168, 7168, 265976832, 265977600, 265977600, 1179648, 262144, 265969664, 164765696, 2097152, 262144, 2359296, 2359296, 100663296, -1879048192, -1879048192, 1610612736, 262144, 2097152, 262144, 2359296, 2359296, 265953280 };
	}

	private static void jj_la1_init_1() {
		QueryParser.jj_la1_1 = new int[]{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0 };
	}

	private final QueryParser.JJCalls[] jj_2_rtns = new QueryParser.JJCalls[3];

	private boolean jj_rescan = false;

	private int jj_gc = 0;

	protected QueryParser(CharStream stream) {
		token_source = new QueryParserTokenManager(stream);
		token = new Token();
		jj_ntk = -1;
		jj_gen = 0;
		for (int i = 0; i < 25; i++)
			jj_la1[i] = -1;

		for (int i = 0; i < (jj_2_rtns.length); i++)
			jj_2_rtns[i] = new QueryParser.JJCalls();

	}

	public void ReInit(CharStream stream) {
		token_source.ReInit(stream);
		token = new Token();
		jj_ntk = -1;
		jj_lookingAhead = false;
		jj_gen = 0;
		for (int i = 0; i < 25; i++)
			jj_la1[i] = -1;

		for (int i = 0; i < (jj_2_rtns.length); i++)
			jj_2_rtns[i] = new QueryParser.JJCalls();

	}

	protected QueryParser(QueryParserTokenManager tm) {
		token_source = tm;
		token = new Token();
		jj_ntk = -1;
		jj_gen = 0;
		for (int i = 0; i < 25; i++)
			jj_la1[i] = -1;

		for (int i = 0; i < (jj_2_rtns.length); i++)
			jj_2_rtns[i] = new QueryParser.JJCalls();

	}

	public void ReInit(QueryParserTokenManager tm) {
		token_source = tm;
		token = new Token();
		jj_ntk = -1;
		jj_gen = 0;
		for (int i = 0; i < 25; i++)
			jj_la1[i] = -1;

		for (int i = 0; i < (jj_2_rtns.length); i++)
			jj_2_rtns[i] = new QueryParser.JJCalls();

	}

	private Token jj_consume_token(int kind) throws ParseException {
		Token oldToken;
		if (((oldToken = token).next) != null)
			token = token.next;
		else
			token = token.next = token_source.getNextToken();

		jj_ntk = -1;
		if ((token.kind) == kind) {
			(jj_gen)++;
			if ((++(jj_gc)) > 100) {
				jj_gc = 0;
				for (int i = 0; i < (jj_2_rtns.length); i++) {
					QueryParser.JJCalls c = jj_2_rtns[i];
					while (c != null) {
						if ((c.gen) < (jj_gen))
							c.first = null;

						c = c.next;
					} 
				}
			}
			return token;
		}
		token = oldToken;
		jj_kind = kind;
		throw generateParseException();
	}

	private static final class LookaheadSuccess extends Error {}

	private final QueryParser.LookaheadSuccess jj_ls = new QueryParser.LookaheadSuccess();

	private boolean jj_scan_token(int kind) {
		if ((jj_scanpos) == (jj_lastpos)) {
			(jj_la)--;
			if ((jj_scanpos.next) == null) {
				jj_lastpos = jj_scanpos = jj_scanpos.next = token_source.getNextToken();
			}else {
				jj_lastpos = jj_scanpos = jj_scanpos.next;
			}
		}else {
			jj_scanpos = jj_scanpos.next;
		}
		if (jj_rescan) {
			int i = 0;
			Token tok = token;
			while ((tok != null) && (tok != (jj_scanpos))) {
				i++;
				tok = tok.next;
			} 
			if (tok != null)
				jj_add_error_token(kind, i);

		}
		if ((jj_scanpos.kind) != kind)
			return true;

		if (((jj_la) == 0) && ((jj_scanpos) == (jj_lastpos)))
			throw jj_ls;

		return false;
	}

	public final Token getNextToken() {
		if ((token.next) != null)
			token = token.next;
		else
			token = token.next = token_source.getNextToken();

		jj_ntk = -1;
		(jj_gen)++;
		return token;
	}

	public final Token getToken(int index) {
		Token t = (jj_lookingAhead) ? jj_scanpos : token;
		for (int i = 0; i < index; i++) {
			if ((t.next) != null)
				t = t.next;
			else
				t = t.next = token_source.getNextToken();

		}
		return t;
	}

	private int jj_ntk() {
		if ((jj_nt = token.next) == null)
			return jj_ntk = (token.next = token_source.getNextToken()).kind;
		else
			return jj_ntk = jj_nt.kind;

	}

	private List<int[]> jj_expentries = new ArrayList<int[]>();

	private int[] jj_expentry;

	private int jj_kind = -1;

	private int[] jj_lasttokens = new int[100];

	private int jj_endpos;

	private void jj_add_error_token(int kind, int pos) {
		if (pos >= 100)
			return;

		if (pos == ((jj_endpos) + 1)) {
			jj_lasttokens[((jj_endpos)++)] = kind;
		}else
			if ((jj_endpos) != 0) {
				jj_expentry = new int[jj_endpos];
				for (int i = 0; i < (jj_endpos); i++) {
					jj_expentry[i] = jj_lasttokens[i];
				}
				jj_entries_loop : for (Iterator<?> it = jj_expentries.iterator(); it.hasNext();) {
					int[] oldentry = ((int[]) (it.next()));
					if ((oldentry.length) == (jj_expentry.length)) {
						for (int i = 0; i < (jj_expentry.length); i++) {
							if ((oldentry[i]) != (jj_expentry[i])) {
								continue jj_entries_loop;
							}
						}
						jj_expentries.add(jj_expentry);
						break jj_entries_loop;
					}
				}
				if (pos != 0)
					jj_lasttokens[((jj_endpos = pos) - 1)] = kind;

			}

	}

	public ParseException generateParseException() {
		jj_expentries.clear();
		boolean[] la1tokens = new boolean[33];
		if ((jj_kind) >= 0) {
			la1tokens[jj_kind] = true;
			jj_kind = -1;
		}
		for (int i = 0; i < 25; i++) {
			if ((jj_la1[i]) == (jj_gen)) {
				for (int j = 0; j < 32; j++) {
					if (((QueryParser.jj_la1_0[i]) & (1 << j)) != 0) {
						la1tokens[j] = true;
					}
					if (((QueryParser.jj_la1_1[i]) & (1 << j)) != 0) {
						la1tokens[(32 + j)] = true;
					}
				}
			}
		}
		for (int i = 0; i < 33; i++) {
			if (la1tokens[i]) {
				jj_expentry = new int[1];
				jj_expentry[0] = i;
				jj_expentries.add(jj_expentry);
			}
		}
		jj_endpos = 0;
		jj_rescan_token();
		jj_add_error_token(0, 0);
		int[][] exptokseq = new int[jj_expentries.size()][];
		for (int i = 0; i < (jj_expentries.size()); i++) {
			exptokseq[i] = jj_expentries.get(i);
		}
		return new ParseException(token, exptokseq, QueryParserConstants.tokenImage);
	}

	public final void enable_tracing() {
	}

	public final void disable_tracing() {
	}

	private void jj_rescan_token() {
		jj_rescan = true;
		for (int i = 0; i < 3; i++) {
			try {
				QueryParser.JJCalls p = jj_2_rtns[i];
				do {
					if ((p.gen) > (jj_gen)) {
						jj_la = p.arg;
						jj_lastpos = jj_scanpos = p.first;
						switch (i) {
							case 0 :
								jj_3_1();
								break;
							case 1 :
								jj_3_2();
								break;
							case 2 :
								jj_3_3();
								break;
						}
					}
					p = p.next;
				} while (p != null );
			} catch (QueryParser.LookaheadSuccess ls) {
			}
		}
		jj_rescan = false;
	}

	private void jj_save(int index, int xla) {
		QueryParser.JJCalls p = jj_2_rtns[index];
		while ((p.gen) > (jj_gen)) {
			if ((p.next) == null) {
				p = p.next = new QueryParser.JJCalls();
				break;
			}
			p = p.next;
		} 
		p.gen = ((jj_gen) + xla) - (jj_la);
		p.first = token;
		p.arg = xla;
	}

	static final class JJCalls {
		int gen;

		Token first;

		int arg;

		QueryParser.JJCalls next;
	}
}

