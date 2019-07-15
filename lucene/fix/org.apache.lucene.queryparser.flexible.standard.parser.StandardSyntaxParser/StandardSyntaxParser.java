

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import org.apache.lucene.queryparser.classic.Token;
import org.apache.lucene.queryparser.flexible.core.QueryNodeParseException;
import org.apache.lucene.queryparser.flexible.core.messages.QueryParserMessages;
import org.apache.lucene.queryparser.flexible.core.nodes.AndQueryNode;
import org.apache.lucene.queryparser.flexible.core.nodes.BooleanQueryNode;
import org.apache.lucene.queryparser.flexible.core.nodes.BoostQueryNode;
import org.apache.lucene.queryparser.flexible.core.nodes.FieldQueryNode;
import org.apache.lucene.queryparser.flexible.core.nodes.FuzzyQueryNode;
import org.apache.lucene.queryparser.flexible.core.nodes.GroupQueryNode;
import org.apache.lucene.queryparser.flexible.core.nodes.ModifierQueryNode;
import org.apache.lucene.queryparser.flexible.core.nodes.OrQueryNode;
import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;
import org.apache.lucene.queryparser.flexible.core.nodes.QuotedFieldQueryNode;
import org.apache.lucene.queryparser.flexible.core.nodes.SlopQueryNode;
import org.apache.lucene.queryparser.flexible.core.parser.SyntaxParser;
import org.apache.lucene.queryparser.flexible.core.util.UnescapedCharSequence;
import org.apache.lucene.queryparser.flexible.messages.Message;
import org.apache.lucene.queryparser.flexible.messages.MessageImpl;
import org.apache.lucene.queryparser.flexible.standard.nodes.RegexpQueryNode;
import org.apache.lucene.queryparser.flexible.standard.nodes.TermRangeQueryNode;
import org.apache.lucene.queryparser.flexible.standard.parser.CharStream;
import org.apache.lucene.queryparser.flexible.standard.parser.EscapeQuerySyntaxImpl;
import org.apache.lucene.queryparser.flexible.standard.parser.ParseException;
import org.apache.lucene.queryparser.flexible.standard.parser.StandardSyntaxParserConstants;
import org.apache.lucene.queryparser.flexible.standard.parser.StandardSyntaxParserTokenManager;
import org.apache.lucene.search.FuzzyQuery;

import static org.apache.lucene.queryparser.flexible.core.nodes.ModifierQueryNode.Modifier.MOD_NONE;
import static org.apache.lucene.queryparser.flexible.core.nodes.ModifierQueryNode.Modifier.MOD_NOT;
import static org.apache.lucene.queryparser.flexible.core.nodes.ModifierQueryNode.Modifier.MOD_REQ;


public class StandardSyntaxParser implements SyntaxParser , StandardSyntaxParserConstants {
	public StandardSyntaxParser() {
	}

	public QueryNode parse(CharSequence query, CharSequence field) throws QueryNodeParseException {
		try {
			QueryNode querynode = TopLevelQuery(field);
			return querynode;
		} catch (ParseException tme) {
			tme.setQuery(query);
			throw tme;
		} catch (Error tme) {
			Message message = new MessageImpl(QueryParserMessages.INVALID_SYNTAX_CANNOT_PARSE, query, tme.getMessage());
			QueryNodeParseException e = new QueryNodeParseException(tme);
			e.setQuery(query);
			e.setNonLocalizedMessage(message);
			throw e;
		}
	}

	public final ModifierQueryNode.Modifier Modifiers() throws ParseException {
		ModifierQueryNode.Modifier ret = MOD_NONE;
		switch ((jj_ntk) == (-1) ? jj_ntk() : jj_ntk) {
			case StandardSyntaxParserConstants.NOT :
			case StandardSyntaxParserConstants.PLUS :
			case StandardSyntaxParserConstants.MINUS :
				switch ((jj_ntk) == (-1) ? jj_ntk() : jj_ntk) {
					case StandardSyntaxParserConstants.PLUS :
						jj_consume_token(StandardSyntaxParserConstants.PLUS);
						ret = MOD_REQ;
						break;
					case StandardSyntaxParserConstants.MINUS :
						jj_consume_token(StandardSyntaxParserConstants.MINUS);
						ret = MOD_NOT;
						break;
					case StandardSyntaxParserConstants.NOT :
						jj_consume_token(StandardSyntaxParserConstants.NOT);
						ret = MOD_NOT;
						break;
					default :
						jj_la1[0] = jj_gen;
						jj_consume_token((-1));
						throw new ParseException();
				}
				break;
			default :
				jj_la1[1] = jj_gen;
		}
		{
			if (true)
				return ret;

		}
		throw new Error("Missing return statement in function");
	}

	public final QueryNode TopLevelQuery(CharSequence field) throws ParseException {
		QueryNode q;
		q = Query(field);
		jj_consume_token(0);
		{
			if (true)
				return q;

		}
		throw new Error("Missing return statement in function");
	}

	public final QueryNode Query(CharSequence field) throws ParseException {
		Vector<QueryNode> clauses = null;
		QueryNode c;
		QueryNode first = null;
		first = DisjQuery(field);
		label_1 : while (true) {
			switch ((jj_ntk) == (-1) ? jj_ntk() : jj_ntk) {
				case StandardSyntaxParserConstants.NOT :
				case StandardSyntaxParserConstants.PLUS :
				case StandardSyntaxParserConstants.MINUS :
				case StandardSyntaxParserConstants.LPAREN :
				case StandardSyntaxParserConstants.QUOTED :
				case StandardSyntaxParserConstants.TERM :
				case StandardSyntaxParserConstants.REGEXPTERM :
				case StandardSyntaxParserConstants.RANGEIN_START :
				case StandardSyntaxParserConstants.RANGEEX_START :
				case StandardSyntaxParserConstants.NUMBER :
					break;
				default :
					jj_la1[2] = jj_gen;
					break label_1;
			}
			c = DisjQuery(field);
			if (clauses == null) {
				clauses = new Vector<QueryNode>();
				clauses.addElement(first);
			}
			clauses.addElement(c);
		} 
		if (clauses != null) {
			{
				if (true)
					return new BooleanQueryNode(clauses);

			}
		}else {
			if (first instanceof ModifierQueryNode) {
				ModifierQueryNode m = ((ModifierQueryNode) (first));
				if ((m.getModifier()) == (MOD_NOT)) {
					{
						if (true)
							return new BooleanQueryNode(Arrays.<QueryNode>asList(m));

					}
				}
			}
			{
				if (true)
					return first;

			}
		}
		throw new Error("Missing return statement in function");
	}

	public final QueryNode DisjQuery(CharSequence field) throws ParseException {
		QueryNode first;
		QueryNode c;
		Vector<QueryNode> clauses = null;
		first = ConjQuery(field);
		label_2 : while (true) {
			switch ((jj_ntk) == (-1) ? jj_ntk() : jj_ntk) {
				case StandardSyntaxParserConstants.OR :
					break;
				default :
					jj_la1[3] = jj_gen;
					break label_2;
			}
			jj_consume_token(StandardSyntaxParserConstants.OR);
			c = ConjQuery(field);
			if (clauses == null) {
				clauses = new Vector<QueryNode>();
				clauses.addElement(first);
			}
			clauses.addElement(c);
		} 
		if (clauses != null) {
			{
				if (true)
					return new OrQueryNode(clauses);

			}
		}else {
			{
				if (true)
					return first;

			}
		}
		throw new Error("Missing return statement in function");
	}

	public final QueryNode ConjQuery(CharSequence field) throws ParseException {
		QueryNode first;
		QueryNode c;
		Vector<QueryNode> clauses = null;
		first = ModClause(field);
		label_3 : while (true) {
			switch ((jj_ntk) == (-1) ? jj_ntk() : jj_ntk) {
				case StandardSyntaxParserConstants.AND :
					break;
				default :
					jj_la1[4] = jj_gen;
					break label_3;
			}
			jj_consume_token(StandardSyntaxParserConstants.AND);
			c = ModClause(field);
			if (clauses == null) {
				clauses = new Vector<QueryNode>();
				clauses.addElement(first);
			}
			clauses.addElement(c);
		} 
		if (clauses != null) {
			{
				if (true)
					return new AndQueryNode(clauses);

			}
		}else {
			{
				if (true)
					return first;

			}
		}
		throw new Error("Missing return statement in function");
	}

	public final QueryNode ModClause(CharSequence field) throws ParseException {
		QueryNode q;
		ModifierQueryNode.Modifier mods;
		mods = Modifiers();
		q = Clause(field);
		if (mods != (MOD_NONE)) {
			q = new ModifierQueryNode(q, mods);
		}
		{
			if (true)
				return q;

		}
		throw new Error("Missing return statement in function");
	}

	public final QueryNode Clause(CharSequence field) throws ParseException {
		QueryNode q;
		Token fieldToken = null;
		Token boost = null;
		Token operator = null;
		Token term = null;
		FieldQueryNode qLower;
		FieldQueryNode qUpper;
		boolean lowerInclusive;
		boolean upperInclusive;
		boolean group = false;
		if (jj_2_2(3)) {
			fieldToken = jj_consume_token(StandardSyntaxParserConstants.TERM);
			switch ((jj_ntk) == (-1) ? jj_ntk() : jj_ntk) {
				case StandardSyntaxParserConstants.OP_COLON :
				case StandardSyntaxParserConstants.OP_EQUAL :
					switch ((jj_ntk) == (-1) ? jj_ntk() : jj_ntk) {
						case StandardSyntaxParserConstants.OP_COLON :
							jj_consume_token(StandardSyntaxParserConstants.OP_COLON);
							break;
						case StandardSyntaxParserConstants.OP_EQUAL :
							jj_consume_token(StandardSyntaxParserConstants.OP_EQUAL);
							break;
						default :
							jj_la1[5] = jj_gen;
							jj_consume_token((-1));
							throw new ParseException();
					}
					field = EscapeQuerySyntaxImpl.discardEscapeChar(fieldToken.image);
					q = Term(field);
					break;
				case StandardSyntaxParserConstants.OP_LESSTHAN :
				case StandardSyntaxParserConstants.OP_LESSTHANEQ :
				case StandardSyntaxParserConstants.OP_MORETHAN :
				case StandardSyntaxParserConstants.OP_MORETHANEQ :
					switch ((jj_ntk) == (-1) ? jj_ntk() : jj_ntk) {
						case StandardSyntaxParserConstants.OP_LESSTHAN :
							operator = jj_consume_token(StandardSyntaxParserConstants.OP_LESSTHAN);
							break;
						case StandardSyntaxParserConstants.OP_LESSTHANEQ :
							operator = jj_consume_token(StandardSyntaxParserConstants.OP_LESSTHANEQ);
							break;
						case StandardSyntaxParserConstants.OP_MORETHAN :
							operator = jj_consume_token(StandardSyntaxParserConstants.OP_MORETHAN);
							break;
						case StandardSyntaxParserConstants.OP_MORETHANEQ :
							operator = jj_consume_token(StandardSyntaxParserConstants.OP_MORETHANEQ);
							break;
						default :
							jj_la1[6] = jj_gen;
							jj_consume_token((-1));
							throw new ParseException();
					}
					field = EscapeQuerySyntaxImpl.discardEscapeChar(fieldToken.image);
					switch ((jj_ntk) == (-1) ? jj_ntk() : jj_ntk) {
						case StandardSyntaxParserConstants.TERM :
							term = jj_consume_token(StandardSyntaxParserConstants.TERM);
							break;
						case StandardSyntaxParserConstants.QUOTED :
							term = jj_consume_token(StandardSyntaxParserConstants.QUOTED);
							break;
						case StandardSyntaxParserConstants.NUMBER :
							term = jj_consume_token(StandardSyntaxParserConstants.NUMBER);
							break;
						default :
							jj_la1[7] = jj_gen;
							jj_consume_token((-1));
							throw new ParseException();
					}
					if ((term.kind) == (StandardSyntaxParserConstants.QUOTED)) {
						term.image = term.image.substring(1, ((term.image.length()) - 1));
					}
					switch (operator.kind) {
						case StandardSyntaxParserConstants.OP_LESSTHAN :
							lowerInclusive = true;
							upperInclusive = false;
							qLower = new FieldQueryNode(field, "*", term.beginColumn, term.endColumn);
							qUpper = new FieldQueryNode(field, EscapeQuerySyntaxImpl.discardEscapeChar(term.image), term.beginColumn, term.endColumn);
							break;
						case StandardSyntaxParserConstants.OP_LESSTHANEQ :
							lowerInclusive = true;
							upperInclusive = true;
							qLower = new FieldQueryNode(field, "*", term.beginColumn, term.endColumn);
							qUpper = new FieldQueryNode(field, EscapeQuerySyntaxImpl.discardEscapeChar(term.image), term.beginColumn, term.endColumn);
							break;
						case StandardSyntaxParserConstants.OP_MORETHAN :
							lowerInclusive = false;
							upperInclusive = true;
							qLower = new FieldQueryNode(field, EscapeQuerySyntaxImpl.discardEscapeChar(term.image), term.beginColumn, term.endColumn);
							qUpper = new FieldQueryNode(field, "*", term.beginColumn, term.endColumn);
							break;
						case StandardSyntaxParserConstants.OP_MORETHANEQ :
							lowerInclusive = true;
							upperInclusive = true;
							qLower = new FieldQueryNode(field, EscapeQuerySyntaxImpl.discardEscapeChar(term.image), term.beginColumn, term.endColumn);
							qUpper = new FieldQueryNode(field, "*", term.beginColumn, term.endColumn);
							break;
						default :
							{
								if (true)
									throw new Error(("Unhandled case: operator=" + (operator.toString())));

							}
					}
					q = new TermRangeQueryNode(qLower, qUpper, lowerInclusive, upperInclusive);
					break;
				default :
					jj_la1[8] = jj_gen;
					jj_consume_token((-1));
					throw new ParseException();
			}
		}else {
			switch ((jj_ntk) == (-1) ? jj_ntk() : jj_ntk) {
				case StandardSyntaxParserConstants.LPAREN :
				case StandardSyntaxParserConstants.QUOTED :
				case StandardSyntaxParserConstants.TERM :
				case StandardSyntaxParserConstants.REGEXPTERM :
				case StandardSyntaxParserConstants.RANGEIN_START :
				case StandardSyntaxParserConstants.RANGEEX_START :
				case StandardSyntaxParserConstants.NUMBER :
					if (jj_2_1(2)) {
						fieldToken = jj_consume_token(StandardSyntaxParserConstants.TERM);
						switch ((jj_ntk) == (-1) ? jj_ntk() : jj_ntk) {
							case StandardSyntaxParserConstants.OP_COLON :
								jj_consume_token(StandardSyntaxParserConstants.OP_COLON);
								break;
							case StandardSyntaxParserConstants.OP_EQUAL :
								jj_consume_token(StandardSyntaxParserConstants.OP_EQUAL);
								break;
							default :
								jj_la1[9] = jj_gen;
								jj_consume_token((-1));
								throw new ParseException();
						}
						field = EscapeQuerySyntaxImpl.discardEscapeChar(fieldToken.image);
					}else {
					}
					switch ((jj_ntk) == (-1) ? jj_ntk() : jj_ntk) {
						case StandardSyntaxParserConstants.QUOTED :
						case StandardSyntaxParserConstants.TERM :
						case StandardSyntaxParserConstants.REGEXPTERM :
						case StandardSyntaxParserConstants.RANGEIN_START :
						case StandardSyntaxParserConstants.RANGEEX_START :
						case StandardSyntaxParserConstants.NUMBER :
							q = Term(field);
							break;
						case StandardSyntaxParserConstants.LPAREN :
							jj_consume_token(StandardSyntaxParserConstants.LPAREN);
							q = Query(field);
							jj_consume_token(StandardSyntaxParserConstants.RPAREN);
							switch ((jj_ntk) == (-1) ? jj_ntk() : jj_ntk) {
								case StandardSyntaxParserConstants.CARAT :
									jj_consume_token(StandardSyntaxParserConstants.CARAT);
									boost = jj_consume_token(StandardSyntaxParserConstants.NUMBER);
									break;
								default :
									jj_la1[10] = jj_gen;
							}
							group = true;
							break;
						default :
							jj_la1[11] = jj_gen;
							jj_consume_token((-1));
							throw new ParseException();
					}
					break;
				default :
					jj_la1[12] = jj_gen;
					jj_consume_token((-1));
					throw new ParseException();
			}
		}
		if (boost != null) {
			float f = ((float) (1.0));
			try {
				f = Float.parseFloat(boost.image);
				if (q != null) {
					q = new BoostQueryNode(q, f);
				}
			} catch (Exception ignored) {
			}
		}
		if (group) {
			q = new GroupQueryNode(q);
		}
		{
			if (true)
				return q;

		}
		throw new Error("Missing return statement in function");
	}

	public final QueryNode Term(CharSequence field) throws ParseException {
		Token term;
		Token boost = null;
		Token fuzzySlop = null;
		Token goop1;
		Token goop2;
		boolean fuzzy = false;
		boolean regexp = false;
		boolean startInc = false;
		boolean endInc = false;
		QueryNode q = null;
		FieldQueryNode qLower;
		FieldQueryNode qUpper;
		float defaultMinSimilarity = FuzzyQuery.defaultMinSimilarity;
		switch ((jj_ntk) == (-1) ? jj_ntk() : jj_ntk) {
			case StandardSyntaxParserConstants.TERM :
			case StandardSyntaxParserConstants.REGEXPTERM :
			case StandardSyntaxParserConstants.NUMBER :
				switch ((jj_ntk) == (-1) ? jj_ntk() : jj_ntk) {
					case StandardSyntaxParserConstants.TERM :
						term = jj_consume_token(StandardSyntaxParserConstants.TERM);
						q = new FieldQueryNode(field, EscapeQuerySyntaxImpl.discardEscapeChar(term.image), term.beginColumn, term.endColumn);
						break;
					case StandardSyntaxParserConstants.REGEXPTERM :
						term = jj_consume_token(StandardSyntaxParserConstants.REGEXPTERM);
						regexp = true;
						break;
					case StandardSyntaxParserConstants.NUMBER :
						term = jj_consume_token(StandardSyntaxParserConstants.NUMBER);
						break;
					default :
						jj_la1[13] = jj_gen;
						jj_consume_token((-1));
						throw new ParseException();
				}
				switch ((jj_ntk) == (-1) ? jj_ntk() : jj_ntk) {
					case StandardSyntaxParserConstants.FUZZY_SLOP :
						fuzzySlop = jj_consume_token(StandardSyntaxParserConstants.FUZZY_SLOP);
						fuzzy = true;
						break;
					default :
						jj_la1[14] = jj_gen;
				}
				switch ((jj_ntk) == (-1) ? jj_ntk() : jj_ntk) {
					case StandardSyntaxParserConstants.CARAT :
						jj_consume_token(StandardSyntaxParserConstants.CARAT);
						boost = jj_consume_token(StandardSyntaxParserConstants.NUMBER);
						switch ((jj_ntk) == (-1) ? jj_ntk() : jj_ntk) {
							case StandardSyntaxParserConstants.FUZZY_SLOP :
								fuzzySlop = jj_consume_token(StandardSyntaxParserConstants.FUZZY_SLOP);
								fuzzy = true;
								break;
							default :
								jj_la1[15] = jj_gen;
						}
						break;
					default :
						jj_la1[16] = jj_gen;
				}
				if (fuzzy) {
					float fms = defaultMinSimilarity;
					try {
						fms = Float.parseFloat(fuzzySlop.image.substring(1));
					} catch (Exception ignored) {
					}
					if (fms < 0.0F) {
						{
							if (true)
								throw new ParseException(new MessageImpl(QueryParserMessages.INVALID_SYNTAX_FUZZY_LIMITS));

						}
					}else
						if ((fms >= 1.0F) && (fms != ((int) (fms)))) {
							{
								if (true)
									throw new ParseException(new MessageImpl(QueryParserMessages.INVALID_SYNTAX_FUZZY_EDITS));

							}
						}

					q = new FuzzyQueryNode(field, EscapeQuerySyntaxImpl.discardEscapeChar(term.image), fms, term.beginColumn, term.endColumn);
				}else
					if (regexp) {
						String re = term.image.substring(1, ((term.image.length()) - 1));
						q = new RegexpQueryNode(field, re, 0, re.length());
					}

				break;
			case StandardSyntaxParserConstants.RANGEIN_START :
			case StandardSyntaxParserConstants.RANGEEX_START :
				switch ((jj_ntk) == (-1) ? jj_ntk() : jj_ntk) {
					case StandardSyntaxParserConstants.RANGEIN_START :
						jj_consume_token(StandardSyntaxParserConstants.RANGEIN_START);
						startInc = true;
						break;
					case StandardSyntaxParserConstants.RANGEEX_START :
						jj_consume_token(StandardSyntaxParserConstants.RANGEEX_START);
						break;
					default :
						jj_la1[17] = jj_gen;
						jj_consume_token((-1));
						throw new ParseException();
				}
				switch ((jj_ntk) == (-1) ? jj_ntk() : jj_ntk) {
					case StandardSyntaxParserConstants.RANGE_GOOP :
						goop1 = jj_consume_token(StandardSyntaxParserConstants.RANGE_GOOP);
						break;
					case StandardSyntaxParserConstants.RANGE_QUOTED :
						goop1 = jj_consume_token(StandardSyntaxParserConstants.RANGE_QUOTED);
						break;
					case StandardSyntaxParserConstants.RANGE_TO :
						goop1 = jj_consume_token(StandardSyntaxParserConstants.RANGE_TO);
						break;
					default :
						jj_la1[18] = jj_gen;
						jj_consume_token((-1));
						throw new ParseException();
				}
				jj_consume_token(StandardSyntaxParserConstants.RANGE_TO);
				switch ((jj_ntk) == (-1) ? jj_ntk() : jj_ntk) {
					case StandardSyntaxParserConstants.RANGE_GOOP :
						goop2 = jj_consume_token(StandardSyntaxParserConstants.RANGE_GOOP);
						break;
					case StandardSyntaxParserConstants.RANGE_QUOTED :
						goop2 = jj_consume_token(StandardSyntaxParserConstants.RANGE_QUOTED);
						break;
					case StandardSyntaxParserConstants.RANGE_TO :
						goop2 = jj_consume_token(StandardSyntaxParserConstants.RANGE_TO);
						break;
					default :
						jj_la1[19] = jj_gen;
						jj_consume_token((-1));
						throw new ParseException();
				}
				switch ((jj_ntk) == (-1) ? jj_ntk() : jj_ntk) {
					case StandardSyntaxParserConstants.RANGEIN_END :
						jj_consume_token(StandardSyntaxParserConstants.RANGEIN_END);
						endInc = true;
						break;
					case StandardSyntaxParserConstants.RANGEEX_END :
						jj_consume_token(StandardSyntaxParserConstants.RANGEEX_END);
						break;
					default :
						jj_la1[20] = jj_gen;
						jj_consume_token((-1));
						throw new ParseException();
				}
				switch ((jj_ntk) == (-1) ? jj_ntk() : jj_ntk) {
					case StandardSyntaxParserConstants.CARAT :
						jj_consume_token(StandardSyntaxParserConstants.CARAT);
						boost = jj_consume_token(StandardSyntaxParserConstants.NUMBER);
						break;
					default :
						jj_la1[21] = jj_gen;
				}
				if ((goop1.kind) == (StandardSyntaxParserConstants.RANGE_QUOTED)) {
					goop1.image = goop1.image.substring(1, ((goop1.image.length()) - 1));
				}
				if ((goop2.kind) == (StandardSyntaxParserConstants.RANGE_QUOTED)) {
					goop2.image = goop2.image.substring(1, ((goop2.image.length()) - 1));
				}
				qLower = new FieldQueryNode(field, EscapeQuerySyntaxImpl.discardEscapeChar(goop1.image), goop1.beginColumn, goop1.endColumn);
				qUpper = new FieldQueryNode(field, EscapeQuerySyntaxImpl.discardEscapeChar(goop2.image), goop2.beginColumn, goop2.endColumn);
				q = new TermRangeQueryNode(qLower, qUpper, (startInc ? true : false), (endInc ? true : false));
				break;
			case StandardSyntaxParserConstants.QUOTED :
				term = jj_consume_token(StandardSyntaxParserConstants.QUOTED);
				q = new QuotedFieldQueryNode(field, EscapeQuerySyntaxImpl.discardEscapeChar(term.image.substring(1, ((term.image.length()) - 1))), ((term.beginColumn) + 1), ((term.endColumn) - 1));
				switch ((jj_ntk) == (-1) ? jj_ntk() : jj_ntk) {
					case StandardSyntaxParserConstants.FUZZY_SLOP :
						fuzzySlop = jj_consume_token(StandardSyntaxParserConstants.FUZZY_SLOP);
						break;
					default :
						jj_la1[22] = jj_gen;
				}
				switch ((jj_ntk) == (-1) ? jj_ntk() : jj_ntk) {
					case StandardSyntaxParserConstants.CARAT :
						jj_consume_token(StandardSyntaxParserConstants.CARAT);
						boost = jj_consume_token(StandardSyntaxParserConstants.NUMBER);
						break;
					default :
						jj_la1[23] = jj_gen;
				}
				int phraseSlop = 0;
				if (fuzzySlop != null) {
					try {
						phraseSlop = ((int) (Float.parseFloat(fuzzySlop.image.substring(1))));
						q = new SlopQueryNode(q, phraseSlop);
					} catch (Exception ignored) {
					}
				}
				break;
			default :
				jj_la1[24] = jj_gen;
				jj_consume_token((-1));
				throw new ParseException();
		}
		if (boost != null) {
			float f = ((float) (1.0));
			try {
				f = Float.parseFloat(boost.image);
				if (q != null) {
					q = new BoostQueryNode(q, f);
				}
			} catch (Exception ignored) {
			}
		}
		{
			if (true)
				return q;

		}
		throw new Error("Missing return statement in function");
	}

	private boolean jj_2_1(int xla) {
		jj_la = xla;
		jj_lastpos = jj_scanpos = token;
		try {
			return !(jj_3_1());
		} catch (StandardSyntaxParser.LookaheadSuccess ls) {
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
		} catch (StandardSyntaxParser.LookaheadSuccess ls) {
			return true;
		} finally {
			jj_save(1, xla);
		}
	}

	private boolean jj_3R_12() {
		if (jj_scan_token(StandardSyntaxParserConstants.RANGEIN_START))
			return true;

		return false;
	}

	private boolean jj_3R_11() {
		if (jj_scan_token(StandardSyntaxParserConstants.REGEXPTERM))
			return true;

		return false;
	}

	private boolean jj_3_1() {
		if (jj_scan_token(StandardSyntaxParserConstants.TERM))
			return true;

		Token xsp;
		xsp = jj_scanpos;
		if (jj_scan_token(15)) {
			jj_scanpos = xsp;
			if (jj_scan_token(16))
				return true;

		}
		return false;
	}

	private boolean jj_3R_8() {
		Token xsp;
		xsp = jj_scanpos;
		if (jj_3R_12()) {
			jj_scanpos = xsp;
			if (jj_scan_token(27))
				return true;

		}
		return false;
	}

	private boolean jj_3R_10() {
		if (jj_scan_token(StandardSyntaxParserConstants.TERM))
			return true;

		return false;
	}

	private boolean jj_3R_7() {
		Token xsp;
		xsp = jj_scanpos;
		if (jj_3R_10()) {
			jj_scanpos = xsp;
			if (jj_3R_11()) {
				jj_scanpos = xsp;
				if (jj_scan_token(28))
					return true;

			}
		}
		return false;
	}

	private boolean jj_3R_9() {
		if (jj_scan_token(StandardSyntaxParserConstants.QUOTED))
			return true;

		return false;
	}

	private boolean jj_3R_5() {
		Token xsp;
		xsp = jj_scanpos;
		if (jj_scan_token(17)) {
			jj_scanpos = xsp;
			if (jj_scan_token(18)) {
				jj_scanpos = xsp;
				if (jj_scan_token(19)) {
					jj_scanpos = xsp;
					if (jj_scan_token(20))
						return true;

				}
			}
		}
		xsp = jj_scanpos;
		if (jj_scan_token(23)) {
			jj_scanpos = xsp;
			if (jj_scan_token(22)) {
				jj_scanpos = xsp;
				if (jj_scan_token(28))
					return true;

			}
		}
		return false;
	}

	private boolean jj_3R_4() {
		Token xsp;
		xsp = jj_scanpos;
		if (jj_scan_token(15)) {
			jj_scanpos = xsp;
			if (jj_scan_token(16))
				return true;

		}
		if (jj_3R_6())
			return true;

		return false;
	}

	private boolean jj_3R_6() {
		Token xsp;
		xsp = jj_scanpos;
		if (jj_3R_7()) {
			jj_scanpos = xsp;
			if (jj_3R_8()) {
				jj_scanpos = xsp;
				if (jj_3R_9())
					return true;

			}
		}
		return false;
	}

	private boolean jj_3_2() {
		if (jj_scan_token(StandardSyntaxParserConstants.TERM))
			return true;

		Token xsp;
		xsp = jj_scanpos;
		if (jj_3R_4()) {
			jj_scanpos = xsp;
			if (jj_3R_5())
				return true;

		}
		return false;
	}

	public StandardSyntaxParserTokenManager token_source;

	public Token token;

	public Token jj_nt;

	private int jj_ntk;

	private Token jj_scanpos;

	private Token jj_lastpos;

	private int jj_la;

	private int jj_gen;

	private final int[] jj_la1 = new int[25];

	private static int[] jj_la1_0;

	private static int[] jj_la1_1;

	static {
		StandardSyntaxParser.jj_la1_init_0();
		StandardSyntaxParser.jj_la1_init_1();
	}

	private static void jj_la1_init_0() {
		StandardSyntaxParser.jj_la1_0 = new int[]{ 7168, 7168, 515914752, 512, 256, 98304, 1966080, 281018368, 2064384, 98304, 2097152, 515907584, 515907584, 310378496, 16777216, 16777216, 2097152, 201326592, 536870912, 536870912, -1073741824, 2097152, 16777216, 2097152, 515899392 };
	}

	private static void jj_la1_init_1() {
		StandardSyntaxParser.jj_la1_1 = new int[]{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 3, 3, 0, 0, 0, 0, 0 };
	}

	private final StandardSyntaxParser.JJCalls[] jj_2_rtns = new StandardSyntaxParser.JJCalls[2];

	private boolean jj_rescan = false;

	private int jj_gc = 0;

	public StandardSyntaxParser(CharStream stream) {
		token_source = new StandardSyntaxParserTokenManager(stream);
		token = new Token();
		jj_ntk = -1;
		jj_gen = 0;
		for (int i = 0; i < 25; i++)
			jj_la1[i] = -1;

		for (int i = 0; i < (jj_2_rtns.length); i++)
			jj_2_rtns[i] = new StandardSyntaxParser.JJCalls();

	}

	public void ReInit(CharStream stream) {
		token_source.ReInit(stream);
		token = new Token();
		jj_ntk = -1;
		jj_gen = 0;
		for (int i = 0; i < 25; i++)
			jj_la1[i] = -1;

		for (int i = 0; i < (jj_2_rtns.length); i++)
			jj_2_rtns[i] = new StandardSyntaxParser.JJCalls();

	}

	public StandardSyntaxParser(StandardSyntaxParserTokenManager tm) {
		token_source = tm;
		token = new Token();
		jj_ntk = -1;
		jj_gen = 0;
		for (int i = 0; i < 25; i++)
			jj_la1[i] = -1;

		for (int i = 0; i < (jj_2_rtns.length); i++)
			jj_2_rtns[i] = new StandardSyntaxParser.JJCalls();

	}

	public void ReInit(StandardSyntaxParserTokenManager tm) {
		token_source = tm;
		token = new Token();
		jj_ntk = -1;
		jj_gen = 0;
		for (int i = 0; i < 25; i++)
			jj_la1[i] = -1;

		for (int i = 0; i < (jj_2_rtns.length); i++)
			jj_2_rtns[i] = new StandardSyntaxParser.JJCalls();

	}

	private Token jj_consume_token(int kind) throws ParseException {
		Token oldToken;
		if (((oldToken = token).next) != null)
			token = token.next;
		else {
		}
		jj_ntk = -1;
		if ((token.kind) == kind) {
			(jj_gen)++;
			if ((++(jj_gc)) > 100) {
				jj_gc = 0;
				for (int i = 0; i < (jj_2_rtns.length); i++) {
					StandardSyntaxParser.JJCalls c = jj_2_rtns[i];
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

	private final StandardSyntaxParser.LookaheadSuccess jj_ls = new StandardSyntaxParser.LookaheadSuccess();

	private boolean jj_scan_token(int kind) {
		if ((jj_scanpos) == (jj_lastpos)) {
			(jj_la)--;
			if ((jj_scanpos.next) == null) {
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
		else {
		}
		jj_ntk = -1;
		(jj_gen)++;
		return token;
	}

	public final Token getToken(int index) {
		Token t = token;
		for (int i = 0; i < index; i++) {
			if ((t.next) != null)
				t = t.next;
			else {
			}
		}
		return t;
	}

	private int jj_ntk() {
		if ((jj_nt = token.next) == null) {
		}else
			return jj_ntk = jj_nt.kind;

		return 0;
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
		boolean[] la1tokens = new boolean[34];
		if ((jj_kind) >= 0) {
			la1tokens[jj_kind] = true;
			jj_kind = -1;
		}
		for (int i = 0; i < 25; i++) {
			if ((jj_la1[i]) == (jj_gen)) {
				for (int j = 0; j < 32; j++) {
					if (((StandardSyntaxParser.jj_la1_0[i]) & (1 << j)) != 0) {
						la1tokens[j] = true;
					}
					if (((StandardSyntaxParser.jj_la1_1[i]) & (1 << j)) != 0) {
						la1tokens[(32 + j)] = true;
					}
				}
			}
		}
		for (int i = 0; i < 34; i++) {
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
		return null;
	}

	public final void enable_tracing() {
	}

	public final void disable_tracing() {
	}

	private void jj_rescan_token() {
		jj_rescan = true;
		for (int i = 0; i < 2; i++) {
			try {
				StandardSyntaxParser.JJCalls p = jj_2_rtns[i];
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
						}
					}
					p = p.next;
				} while (p != null );
			} catch (StandardSyntaxParser.LookaheadSuccess ls) {
			}
		}
		jj_rescan = false;
	}

	private void jj_save(int index, int xla) {
		StandardSyntaxParser.JJCalls p = jj_2_rtns[index];
		while ((p.gen) > (jj_gen)) {
			if ((p.next) == null) {
				p = p.next = new StandardSyntaxParser.JJCalls();
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

		StandardSyntaxParser.JJCalls next;
	}
}

