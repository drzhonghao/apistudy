

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.felix.gogo.jline.ParsedLineImpl;
import org.apache.felix.gogo.runtime.EOFError;
import org.apache.felix.gogo.runtime.Parser.Statement;
import org.apache.felix.gogo.runtime.SyntaxError;
import org.apache.felix.gogo.runtime.Token;
import org.apache.karaf.shell.api.console.Command;
import org.apache.karaf.shell.api.console.Registry;
import org.apache.karaf.shell.api.console.Session;
import org.jline.reader.ParsedLine;
import org.jline.reader.Parser;

import static org.jline.reader.Parser.ParseContext.COMPLETE;


public class KarafParser implements Parser {
	private final Session session;

	public KarafParser(Session session) {
		this.session = session;
	}

	@Override
	public ParsedLine parse(String line, int cursor, Parser.ParseContext parseContext) throws SyntaxError {
		try {
			return doParse(line, cursor, parseContext);
		} catch (EOFError e) {
			throw new org.jline.reader.EOFError(e.line(), e.column(), e.getMessage(), e.missing());
		} catch (SyntaxError e) {
			throw new org.jline.reader.SyntaxError(e.line(), e.column(), e.getMessage());
		}
	}

	private ParsedLine doParse(String line, int cursor, Parser.ParseContext parseContext) throws SyntaxError {
		org.apache.felix.gogo.runtime.Parser.Program program = null;
		List<org.apache.felix.gogo.runtime.Parser.Statement> statements = null;
		String repaired = line;
		while (program == null) {
			try {
				org.apache.felix.gogo.runtime.Parser parser = new org.apache.felix.gogo.runtime.Parser(repaired);
				program = parser.program();
				statements = parser.statements();
			} catch (EOFError e) {
				if ((parseContext == (COMPLETE)) && ((repaired.length()) < ((line.length()) + 1024))) {
					repaired = (repaired + " ") + (e.repair());
				}else {
					throw e;
				}
			}
		} 
		org.apache.felix.gogo.runtime.Parser.Statement statement = null;
		for (int i = (statements.size()) - 1; i >= 0; i--) {
			org.apache.felix.gogo.runtime.Parser.Statement s = statements.get(i);
			if ((s.start()) <= cursor) {
				boolean isOk = true;
				if (((s.start()) + (s.length())) < cursor) {
					for (int j = (s.start()) + (s.length()); isOk && (j < cursor); j++) {
						isOk = Character.isWhitespace(line.charAt(j));
					}
				}
				statement = s;
				break;
			}
		}
		if (((statement != null) && ((statement.tokens()) != null)) && (!(statement.tokens().isEmpty()))) {
			String cmdName = session.resolveCommand(statement.tokens().get(0).toString());
			String[] parts = cmdName.split(":");
			Command cmd = ((parts.length) == 2) ? session.getRegistry().getCommand(parts[0], parts[1]) : null;
			if (repaired != line) {
				Token stmt = statement.subSequence(0, ((line.length()) - (statement.start())));
				List<Token> tokens = new ArrayList<>(statement.tokens());
				Token last = tokens.get(((tokens.size()) - 1));
				tokens.set(((tokens.size()) - 1), last.subSequence(0, ((line.length()) - (last.start()))));
				return new ParsedLineImpl(program, stmt, cursor, tokens);
			}
			return new ParsedLineImpl(program, statement, cursor, statement.tokens());
		}else {
			return new ParsedLineImpl(program, program, cursor, Collections.singletonList(program));
		}
	}
}

