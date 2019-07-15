

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;


public class ACLConfigurationParser {
	public enum Specificity {

		ARGUMENT_MATCH,
		SIGNATURE_MATCH,
		NAME_MATCH,
		WILDCARD_MATCH,
		NO_MATCH;}

	static String compulsoryRoles;

	static {
	}

	public static ACLConfigurationParser.Specificity getRolesForInvocation(String methodName, Object[] params, String[] signature, Dictionary<String, Object> config, List<String> addToRoles) {
		Dictionary<String, Object> properties = ACLConfigurationParser.trimKeys(config);
		String pid = ((String) (properties.get("service.pid")));
		ACLConfigurationParser.Specificity s = ACLConfigurationParser.getRolesBasedOnSignature(methodName, params, signature, properties, addToRoles);
		if (s != (ACLConfigurationParser.Specificity.NO_MATCH)) {
			return s;
		}
		s = ACLConfigurationParser.getRolesBasedOnSignature(methodName, params, null, properties, addToRoles);
		if (s != (ACLConfigurationParser.Specificity.NO_MATCH)) {
			return s;
		}
		List<String> roles = ACLConfigurationParser.getMethodNameWildcardRoles(properties, methodName);
		if (roles != null) {
			addToRoles.addAll(roles);
			return ACLConfigurationParser.Specificity.WILDCARD_MATCH;
		}else
			if (((ACLConfigurationParser.compulsoryRoles) != null) && (!(pid.contains("jmx.acl")))) {
				addToRoles.addAll(ACLConfigurationParser.parseRoles(ACLConfigurationParser.compulsoryRoles));
				return ACLConfigurationParser.Specificity.NAME_MATCH;
			}else {
				return ACLConfigurationParser.Specificity.NO_MATCH;
			}

	}

	public static ACLConfigurationParser.Specificity getRolesForInvocationForAlias(String methodName, Object[] params, String[] signature, Dictionary<String, Object> config, List<String> addToRoles) {
		Dictionary<String, Object> properties = ACLConfigurationParser.trimKeys(config);
		String pid = ((String) (properties.get("service.pid")));
		ACLConfigurationParser.Specificity s = ACLConfigurationParser.getRolesBasedOnSignature(methodName, params, signature, properties, addToRoles);
		if (s != (ACLConfigurationParser.Specificity.NO_MATCH)) {
			return s;
		}
		s = ACLConfigurationParser.getRolesBasedOnSignature(methodName, params, null, properties, addToRoles);
		if (s != (ACLConfigurationParser.Specificity.NO_MATCH)) {
			return s;
		}
		List<String> roles = ACLConfigurationParser.getMethodNameWildcardRoles(properties, methodName);
		if (roles != null) {
			addToRoles.addAll(roles);
			return ACLConfigurationParser.Specificity.WILDCARD_MATCH;
		}else {
			return ACLConfigurationParser.Specificity.NO_MATCH;
		}
	}

	public static void getCompulsoryRoles(List<String> roles) {
		if ((ACLConfigurationParser.compulsoryRoles) != null) {
			roles.addAll(ACLConfigurationParser.parseRoles(ACLConfigurationParser.compulsoryRoles));
		}
	}

	private static ACLConfigurationParser.Specificity getRolesBasedOnSignature(String methodName, Object[] params, String[] signature, Dictionary<String, Object> properties, List<String> roles) {
		if (params != null) {
			boolean foundExactOrRegex = false;
			Object exactArgMatchRoles = properties.get(ACLConfigurationParser.getExactArgSignature(methodName, signature, params));
			if (exactArgMatchRoles instanceof String) {
				roles.addAll(ACLConfigurationParser.parseRoles(((String) (exactArgMatchRoles))));
				foundExactOrRegex = true;
			}
			List<String> r = ACLConfigurationParser.getRegexRoles(properties, methodName, signature, params);
			if (r != null) {
				foundExactOrRegex = true;
				roles.addAll(r);
			}
			if (foundExactOrRegex) {
				return ACLConfigurationParser.Specificity.ARGUMENT_MATCH;
			}
		}else {
			List<String> r = ACLConfigurationParser.getExactArgOrRegexRoles(properties, methodName, signature);
			if (r != null) {
				roles.addAll(r);
			}
		}
		Object signatureRoles = properties.get(ACLConfigurationParser.getSignature(methodName, signature));
		if (signatureRoles instanceof String) {
			roles.addAll(ACLConfigurationParser.parseRoles(((String) (signatureRoles))));
			return signature == null ? ACLConfigurationParser.Specificity.NAME_MATCH : ACLConfigurationParser.Specificity.SIGNATURE_MATCH;
		}
		return ACLConfigurationParser.Specificity.NO_MATCH;
	}

	private static Dictionary<String, Object> trimKeys(Dictionary<String, Object> properties) {
		Dictionary<String, Object> d = new Hashtable<>();
		for (Enumeration<String> e = properties.keys(); e.hasMoreElements();) {
			String key = e.nextElement();
			Object value = properties.get(key);
			d.put(ACLConfigurationParser.removeSpaces(key), value);
		}
		return d;
	}

	private static String removeSpaces(String key) {
		StringBuilder sb = new StringBuilder();
		char quoteChar = 0;
		for (int i = 0; i < (key.length()); i++) {
			char c = key.charAt(i);
			if ((quoteChar == 0) && (c == ' '))
				continue;

			if ((((quoteChar == 0) && ((c == '\"') || (c == '/'))) && ((sb.length()) > 0)) && (((sb.charAt(((sb.length()) - 1))) == '[') || ((sb.charAt(((sb.length()) - 1))) == ','))) {
				quoteChar = c;
			}else
				if ((quoteChar != 0) && (c == quoteChar)) {
					for (int j = i + 1; j < (key.length()); j++) {
						if ((key.charAt(j)) == ' ')
							continue;

						if (((key.charAt(j)) == ']') || ((key.charAt(j)) == ','))
							quoteChar = 0;

						break;
					}
				}

			sb.append(c);
		}
		return sb.toString();
	}

	public static List<String> parseRoles(String roleStr) {
		int hashIdx = roleStr.indexOf('#');
		if (hashIdx >= 0) {
			roleStr = roleStr.substring(0, hashIdx);
		}
		List<String> roles = new ArrayList<>();
		for (String role : roleStr.split("[,]")) {
			String trimmed = role.trim();
			if ((trimmed.length()) > 0) {
				roles.add(trimmed);
			}
		}
		return roles;
	}

	private static Object getExactArgSignature(String methodName, String[] signature, Object[] params) {
		StringBuilder sb = new StringBuilder(ACLConfigurationParser.getSignature(methodName, signature));
		sb.append('[');
		boolean first = true;
		for (Object param : params) {
			if (first)
				first = false;
			else
				sb.append(',');

			sb.append('"');
			if (param != null)
				sb.append(param.toString().trim());

			sb.append('"');
		}
		sb.append(']');
		return sb.toString();
	}

	private static String getSignature(String methodName, String[] signature) {
		StringBuilder sb = new StringBuilder(methodName);
		if (signature == null)
			return sb.toString();

		sb.append('(');
		boolean first = true;
		for (String s : signature) {
			if (first)
				first = false;
			else
				sb.append(',');

			sb.append(s);
		}
		sb.append(')');
		return sb.toString();
	}

	private static List<String> getRegexRoles(Dictionary<String, Object> properties, String methodName, String[] signature, Object[] params) {
		List<String> roles = new ArrayList<>();
		boolean matchFound = false;
		String methodSig = ACLConfigurationParser.getSignature(methodName, signature);
		String prefix = methodSig + "[/";
		for (Enumeration<String> e = properties.keys(); e.hasMoreElements();) {
			String key = e.nextElement().trim();
			if ((key.startsWith(prefix)) && (key.endsWith("/]"))) {
				List<String> regexArgs = ACLConfigurationParser.getRegexDecl(key.substring(methodSig.length()));
				if (ACLConfigurationParser.allParamsMatch(regexArgs, params)) {
					matchFound = true;
					Object roleStr = properties.get(key);
					if (roleStr instanceof String) {
						roles.addAll(ACLConfigurationParser.parseRoles(((String) (roleStr))));
					}
				}
			}
		}
		return matchFound ? roles : null;
	}

	private static List<String> getExactArgOrRegexRoles(Dictionary<String, Object> properties, String methodName, String[] signature) {
		List<String> roles = new ArrayList<>();
		boolean matchFound = false;
		String methodSig = ACLConfigurationParser.getSignature(methodName, signature);
		String prefix = methodSig + "[";
		for (Enumeration<String> e = properties.keys(); e.hasMoreElements();) {
			String key = e.nextElement().trim();
			if ((key.startsWith(prefix)) && (key.endsWith("]"))) {
				matchFound = true;
				Object roleStr = properties.get(key);
				if (roleStr instanceof String) {
					roles.addAll(ACLConfigurationParser.parseRoles(((String) (roleStr))));
				}
			}
		}
		return matchFound ? roles : null;
	}

	private static List<String> getMethodNameWildcardRoles(Dictionary<String, Object> properties, String methodName) {
		SortedMap<String, String> wildcardRules = new TreeMap<>(( s1, s2) -> {
			return (s2.length()) - (s1.length());
		});
		for (Enumeration<String> e = properties.keys(); e.hasMoreElements();) {
			String key = e.nextElement();
			if (key.endsWith("*")) {
				String prefix = key.substring(0, ((key.length()) - 1));
				if (methodName.startsWith(prefix)) {
					wildcardRules.put(prefix, properties.get(key).toString());
				}
			}
			if (key.startsWith("*")) {
				String suffix = key.substring(1);
				if (methodName.endsWith(suffix)) {
					wildcardRules.put(suffix, properties.get(key).toString());
				}
			}
			if (((key.startsWith("*")) && (key.endsWith("*"))) && ((key.length()) > 1)) {
				String middle = key.substring(1, ((key.length()) - 1));
				if (methodName.contains(middle)) {
					wildcardRules.put(middle, properties.get(key).toString());
				}
			}
		}
		if ((wildcardRules.size()) != 0) {
			return ACLConfigurationParser.parseRoles(wildcardRules.values().iterator().next());
		}else {
			return null;
		}
	}

	private static boolean allParamsMatch(List<String> regexArgs, Object[] params) {
		if ((regexArgs.size()) != (params.length))
			return false;

		for (int i = 0; i < (regexArgs.size()); i++) {
			if ((params[i]) == null)
				return false;

			if (!(params[i].toString().trim().matches(regexArgs.get(i)))) {
				return false;
			}
		}
		return true;
	}

	private static List<String> getRegexDecl(String key) {
		List<String> l = new ArrayList<>();
		boolean inRegex = false;
		StringBuilder curRegex = new StringBuilder();
		for (int i = 0; i < (key.length()); i++) {
			if (!inRegex) {
				if ((key.length()) > (i + 1)) {
					String s = key.substring(i, (i + 2));
					if (("[/".equals(s)) || (",/".equals(s))) {
						inRegex = true;
						i++;
						continue;
					}
				}
			}else {
				String s = key.substring(i, (i + 2));
				if (("/]".equals(s)) || ("/,".equals(s))) {
					l.add(curRegex.toString());
					curRegex = new StringBuilder();
					inRegex = false;
					continue;
				}
				curRegex.append(key.charAt(i));
			}
		}
		return l;
	}
}

