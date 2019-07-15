import org.apache.cassandra.cql3.ErrorCollector;
import org.apache.cassandra.cql3.*;


import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CharStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.TokenStream;
import org.apache.cassandra.exceptions.SyntaxException;

/**
 * Helper class to encapsulate common code that calls one of the generated methods in {@code CqlParser}.
 */
public final class CQLFragmentParser
{

    @FunctionalInterface
    public interface CQLParserFunction<R>
    {
        R parse(CqlParser parser) throws RecognitionException;
    }

    public static <R> R parseAny(CQLParserFunction<R> parserFunction, String input, String meaning)
    {
        try
        {
            return parseAnyUnhandled(parserFunction, input);
        }
        catch (RuntimeException re)
        {
            throw new SyntaxException(String.format("Failed parsing %s: [%s] reason: %s %s",
                                                    meaning,
                                                    input,
                                                    re.getClass().getSimpleName(),
                                                    re.getMessage()));
        }
        catch (RecognitionException e)
        {
            throw new SyntaxException("Invalid or malformed " + meaning + ": " + e.getMessage());
        }
    }

    /**
     * Just call a parser method in {@link CqlParser} - does not do any error handling.
     */
    public static <R> R parseAnyUnhandled(CQLParserFunction<R> parserFunction, String input) throws RecognitionException
    {
        // Lexer and parser
        ErrorCollector errorCollector = new ErrorCollector(input);
        CharStream stream = new ANTLRStringStream(input);
        CqlLexer lexer = new CqlLexer(stream);
        lexer.addErrorListener(errorCollector);

        TokenStream tokenStream = new CommonTokenStream(lexer);
        CqlParser parser = new CqlParser(tokenStream);
        parser.addErrorListener(errorCollector);

        // Parse the query string to a statement instance
        R r = parserFunction.parse(parser);

        // The errorCollector has queue up any errors that the lexer and parser may have encountered
        // along the way, if necessary, we turn the last error into exceptions here.
        errorCollector.throwFirstSyntaxError();

        return r;
    }
}
