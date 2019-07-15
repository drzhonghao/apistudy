

import java.util.ArrayList;
import java.util.List;
import org.apache.accumulo.core.data.ArrayByteSequence;
import org.apache.accumulo.core.data.ByteSequence;
import org.apache.accumulo.core.security.AuthorizationContainer;
import org.apache.accumulo.core.security.Authorizations;
import org.apache.accumulo.core.security.ColumnVisibility;
import org.apache.accumulo.core.security.VisibilityParseException;


public class VisibilityEvaluator {
	private AuthorizationContainer auths;

	private static class UnescapingAuthorizationContainer implements AuthorizationContainer {
		private AuthorizationContainer wrapped;

		UnescapingAuthorizationContainer(AuthorizationContainer wrapee) {
			this.wrapped = wrapee;
		}

		@Override
		public boolean contains(ByteSequence auth) {
			return wrapped.contains(VisibilityEvaluator.unescape(auth));
		}
	}

	static ByteSequence unescape(ByteSequence auth) {
		int escapeCharCount = 0;
		for (int i = 0; i < (auth.length()); i++) {
			byte b = auth.byteAt(i);
			if ((b == '"') || (b == '\\')) {
				escapeCharCount++;
			}
		}
		if (escapeCharCount > 0) {
			if ((escapeCharCount % 2) == 1) {
				throw new IllegalArgumentException(("Illegal escape sequence in auth : " + auth));
			}
			byte[] unescapedCopy = new byte[(auth.length()) - (escapeCharCount / 2)];
			int pos = 0;
			for (int i = 0; i < (auth.length()); i++) {
				byte b = auth.byteAt(i);
				if (b == '\\') {
					i++;
					b = auth.byteAt(i);
					if ((b != '"') && (b != '\\')) {
						throw new IllegalArgumentException(("Illegal escape sequence in auth : " + auth));
					}
				}else
					if (b == '"') {
						throw new IllegalArgumentException(("Illegal escape sequence in auth : " + auth));
					}

				unescapedCopy[(pos++)] = b;
			}
			return new ArrayByteSequence(unescapedCopy);
		}else {
			return auth;
		}
	}

	static Authorizations escape(Authorizations auths) {
		ArrayList<byte[]> retAuths = new ArrayList<>(auths.getAuthorizations().size());
		for (byte[] auth : auths.getAuthorizations())
			retAuths.add(VisibilityEvaluator.escape(auth, false));

		return new Authorizations(retAuths);
	}

	public static byte[] escape(byte[] auth, boolean quote) {
		int escapeCount = 0;
		for (int i = 0; i < (auth.length); i++)
			if (((auth[i]) == '"') || ((auth[i]) == '\\'))
				escapeCount++;


		if ((escapeCount > 0) || quote) {
			byte[] escapedAuth = new byte[((auth.length) + escapeCount) + (quote ? 2 : 0)];
			int index = (quote) ? 1 : 0;
			for (int i = 0; i < (auth.length); i++) {
				if (((auth[i]) == '"') || ((auth[i]) == '\\'))
					escapedAuth[(index++)] = '\\';

				escapedAuth[(index++)] = auth[i];
			}
			if (quote) {
				escapedAuth[0] = '"';
				escapedAuth[((escapedAuth.length) - 1)] = '"';
			}
			auth = escapedAuth;
		}
		return auth;
	}

	public VisibilityEvaluator(AuthorizationContainer authsContainer) {
		this.auths = new VisibilityEvaluator.UnescapingAuthorizationContainer(authsContainer);
	}

	public VisibilityEvaluator(Authorizations authorizations) {
		this.auths = VisibilityEvaluator.escape(authorizations);
	}

	public boolean evaluate(ColumnVisibility visibility) throws VisibilityParseException {
		return evaluate(visibility.getExpression(), visibility.getParseTree());
	}

	private final boolean evaluate(final byte[] expression, final ColumnVisibility.Node root) throws VisibilityParseException {
		if ((expression.length) == 0)
			return true;

		return false;
	}
}

