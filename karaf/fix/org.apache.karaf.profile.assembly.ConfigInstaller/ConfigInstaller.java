

import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ConfigInstaller {
	private static final Logger LOGGER = LoggerFactory.getLogger(ConfigInstaller.class);

	private Path etcDirectory;

	private List<String> pidsToExtract;

	public ConfigInstaller(Path etcDirectory, List<String> pidsToExtract) {
		this.etcDirectory = etcDirectory;
		this.pidsToExtract = pidsToExtract;
	}

	private boolean pidMatching(String name) {
		if ((pidsToExtract) == null) {
			return true;
		}
		for (String p : pidsToExtract) {
			boolean negated = false;
			if (p.startsWith("!")) {
				negated = true;
				p = p.substring(1);
			}
			String r = globToRegex(p);
			if (Pattern.matches(r, name)) {
				return !negated;
			}
		}
		return false;
	}

	private String globToRegex(String pattern) {
		StringBuilder sb = new StringBuilder(pattern.length());
		int inGroup = 0;
		int inClass = 0;
		int firstIndexInClass = -1;
		char[] arr = pattern.toCharArray();
		for (int i = 0; i < (arr.length); i++) {
			char ch = arr[i];
			switch (ch) {
				case '\\' :
					if ((++i) >= (arr.length)) {
						sb.append('\\');
					}else {
						char next = arr[i];
						switch (next) {
							case ',' :
								break;
							case 'Q' :
							case 'E' :
								sb.append('\\');
							default :
								sb.append('\\');
						}
						sb.append(next);
					}
					break;
				case '*' :
					if (inClass == 0)
						sb.append(".*");
					else
						sb.append('*');

					break;
				case '?' :
					if (inClass == 0)
						sb.append('.');
					else
						sb.append('?');

					break;
				case '[' :
					inClass++;
					firstIndexInClass = i + 1;
					sb.append('[');
					break;
				case ']' :
					inClass--;
					sb.append(']');
					break;
				case '.' :
				case '(' :
				case ')' :
				case '+' :
				case '|' :
				case '^' :
				case '$' :
				case '@' :
				case '%' :
					if ((inClass == 0) || ((firstIndexInClass == i) && (ch == '^')))
						sb.append('\\');

					sb.append(ch);
					break;
				case '!' :
					if (firstIndexInClass == i)
						sb.append('^');
					else
						sb.append('!');

					break;
				case '{' :
					inGroup++;
					sb.append('(');
					break;
				case '}' :
					inGroup--;
					sb.append(')');
					break;
				case ',' :
					if (inGroup > 0)
						sb.append('|');
					else
						sb.append(',');

					break;
				default :
					sb.append(ch);
			}
		}
		return sb.toString();
	}
}

