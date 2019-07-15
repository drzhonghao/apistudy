

import java.io.PrintStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import org.apache.felix.utils.manifest.Attribute;
import org.apache.felix.utils.manifest.Clause;
import org.apache.felix.utils.manifest.Directive;
import org.apache.felix.utils.manifest.Parser;
import org.apache.felix.utils.version.VersionRange;
import org.apache.karaf.bundle.command.BundlesCommand;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.Terminal;
import org.apache.karaf.shell.support.ShellUtil;
import org.apache.karaf.shell.support.ansi.SimpleAnsi;
import org.osgi.framework.Bundle;
import org.osgi.framework.wiring.BundleCapability;


@Command(scope = "bundle", name = "headers", description = "Displays OSGi headers of a given bundles.")
@Service
public class Headers extends BundlesCommand {
	protected static final String KARAF_PREFIX = "Karaf-";

	protected static final String BUNDLE_PREFIX = "Bundle-";

	protected static final String PACKAGE_SUFFFIX = "-Package";

	protected static final String SERVICE_SUFFIX = "-Service";

	protected static final String CAPABILITY_SUFFIX = "-Capability";

	protected static final String IMPORT_PACKAGES_ATTRIB = "Import-Package";

	protected static final String REQUIRE_BUNDLE_ATTRIB = "Require-Bundle";

	@Option(name = "--indent", description = "Indentation method")
	int indent = -1;

	@Option(name = "--no-uses", description = "Print or not the Export-Package uses section")
	boolean noUses = false;

	@Reference(optional = true)
	Terminal terminal;

	@Override
	protected void executeOnBundle(Bundle bundle) throws Exception {
		String title = ShellUtil.getBundleName(bundle);
		System.out.println(("\n" + title));
		System.out.println(ShellUtil.getUnderlineString(title));
		if ((indent) == 0) {
			Dictionary<String, String> dict = bundle.getHeaders();
			Enumeration<String> keys = dict.keys();
			while (keys.hasMoreElements()) {
				Object k = keys.nextElement();
				Object v = dict.get(k);
				System.out.println(((k + " = ") + (ShellUtil.getValueString(v))));
			} 
		}else {
			System.out.println(generateFormattedOutput(bundle));
		}
	}

	protected String generateFormattedOutput(Bundle bundle) {
		StringBuilder output = new StringBuilder();
		Map<String, Object> otherAttribs = new TreeMap<>();
		Map<String, Object> karafAttribs = new TreeMap<>();
		Map<String, Object> bundleAttribs = new TreeMap<>();
		Map<String, Object> serviceAttribs = new TreeMap<>();
		Map<String, Object> packagesAttribs = new TreeMap<>();
		Dictionary<String, String> dict = bundle.getHeaders();
		Enumeration<String> keys = dict.keys();
		while (keys.hasMoreElements()) {
			String k = keys.nextElement();
			Object v = dict.get(k);
			if (k.startsWith(Headers.KARAF_PREFIX)) {
				karafAttribs.put(k, v);
			}else
				if (k.startsWith(Headers.BUNDLE_PREFIX)) {
					bundleAttribs.put(k, v);
				}else
					if ((k.endsWith(Headers.SERVICE_SUFFIX)) || (k.endsWith(Headers.CAPABILITY_SUFFIX))) {
						serviceAttribs.put(k, v);
					}else
						if (k.endsWith(Headers.PACKAGE_SUFFFIX)) {
							packagesAttribs.put(k, v);
						}else
							if (k.endsWith(Headers.REQUIRE_BUNDLE_ATTRIB)) {
								packagesAttribs.put(k, v);
							}else {
								otherAttribs.put(k, v);
							}




		} 
		Iterator<Map.Entry<String, Object>> it = otherAttribs.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<String, Object> e = it.next();
			output.append(String.format("%s = %s\n", e.getKey(), ShellUtil.getValueString(e.getValue())));
		} 
		if ((otherAttribs.size()) > 0) {
			output.append('\n');
		}
		it = karafAttribs.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<String, Object> e = it.next();
			output.append(String.format("%s = %s\n", e.getKey(), ShellUtil.getValueString(e.getValue())));
		} 
		if ((karafAttribs.size()) > 0) {
			output.append('\n');
		}
		it = bundleAttribs.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<String, Object> e = it.next();
			output.append(String.format("%s = %s\n", e.getKey(), ShellUtil.getValueString(e.getValue())));
		} 
		if ((bundleAttribs.size()) > 0) {
			output.append('\n');
		}
		it = serviceAttribs.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<String, Object> e = it.next();
			output.append(e.getKey());
			output.append(" = \n");
			formatHeader(ShellUtil.getValueString(e.getValue()), null, output, indent);
			output.append("\n");
		} 
		if ((serviceAttribs.size()) > 0) {
			output.append('\n');
		}
		Map<String, Headers.ClauseFormatter> formatters = new HashMap<>();
		formatters.put(Headers.REQUIRE_BUNDLE_ATTRIB, new Headers.ClauseFormatter() {
			public void pre(Clause clause, StringBuilder output) {
				boolean isSatisfied = checkBundle(clause.getName(), clause.getAttribute("bundle-version"));
				output.append((isSatisfied ? SimpleAnsi.COLOR_DEFAULT : SimpleAnsi.COLOR_RED));
			}

			public void post(Clause clause, StringBuilder output) {
				output.append(SimpleAnsi.RESET);
			}
		});
		formatters.put(Headers.IMPORT_PACKAGES_ATTRIB, new Headers.ClauseFormatter() {
			public void pre(Clause clause, StringBuilder output) {
				boolean isSatisfied = checkPackage(clause.getName(), clause.getAttribute("version"));
				boolean isOptional = "optional".equals(clause.getDirective("resolution"));
				output.append((isSatisfied ? SimpleAnsi.COLOR_DEFAULT : SimpleAnsi.COLOR_RED));
				output.append((isSatisfied || isOptional ? SimpleAnsi.INTENSITY_NORMAL : SimpleAnsi.INTENSITY_BOLD));
			}

			public void post(Clause clause, StringBuilder output) {
				output.append(SimpleAnsi.RESET);
			}
		});
		it = packagesAttribs.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<String, Object> e = it.next();
			output.append(e.getKey());
			output.append(" = \n");
			formatHeader(ShellUtil.getValueString(e.getValue()), formatters.get(e.getKey()), output, indent);
			output.append("\n");
		} 
		if ((packagesAttribs.size()) > 0) {
			output.append('\n');
		}
		return output.toString();
	}

	protected interface ClauseFormatter {
		void pre(Clause clause, StringBuilder output);

		void post(Clause clause, StringBuilder output);
	}

	protected void formatHeader(String header, Headers.ClauseFormatter formatter, StringBuilder builder, int indent) {
		Clause[] clauses = Parser.parseHeader(header);
		formatClauses(clauses, formatter, builder, indent);
	}

	protected void formatClauses(Clause[] clauses, Headers.ClauseFormatter formatter, StringBuilder builder, int indent) {
		boolean first = true;
		for (Clause clause : clauses) {
			if (first) {
				first = false;
			}else {
				builder.append(",\n");
			}
			formatClause(clause, formatter, builder, indent);
		}
	}

	protected void formatClause(Clause clause, Headers.ClauseFormatter formatter, StringBuilder builder, int indent) {
		builder.append("\t");
		if (formatter != null) {
			formatter.pre(clause, builder);
		}
		formatClause(clause, builder, indent);
		if (formatter != null) {
			formatter.post(clause, builder);
		}
	}

	protected int getTermWidth() {
		return (terminal) != null ? terminal.getWidth() : 0;
	}

	protected void formatClause(Clause clause, StringBuilder builder, int indent) {
		if (indent < 0) {
			if ((clause.toString().length()) < ((getTermWidth()) - 8)) {
				indent = 1;
			}else {
				indent = 3;
			}
		}
		String name = clause.getName();
		Directive[] directives = clause.getDirectives();
		Attribute[] attributes = clause.getAttributes();
		Arrays.sort(directives, Comparator.comparing(Directive::getName));
		Arrays.sort(attributes, Comparator.comparing(Attribute::getName));
		builder.append(name);
		for (int i = 0; (directives != null) && (i < (directives.length)); i++) {
			if ((noUses) && (directives[i].getName().equalsIgnoreCase("uses"))) {
				continue;
			}
			builder.append(";");
			if (indent > 1) {
				builder.append("\n\t\t");
			}
			builder.append(directives[i].getName()).append(":=");
			String v = directives[i].getValue();
			if (v.contains(",")) {
				if ((indent > 2) && ((v.length()) > 20)) {
					v = v.replace(",", ",\n\t\t\t");
				}
				builder.append("\"").append(v).append("\"");
			}else {
				builder.append(v);
			}
		}
		for (int i = 0; (attributes != null) && (i < (attributes.length)); i++) {
			builder.append(";");
			if (indent > 1) {
				builder.append("\n\t\t");
			}
			builder.append(attributes[i].getName()).append("=");
			String v = attributes[i].getValue();
			if (v.contains(",")) {
				if ((indent > 2) && ((v.length()) > 20)) {
					v = v.replace(",", ",\n\t\t\t");
				}
				builder.append("\"").append(v).append("\"");
			}else {
				builder.append(v);
			}
		}
	}

	private boolean checkBundle(String bundleName, String version) {
		VersionRange vr = VersionRange.parseVersionRange(version);
		return false;
	}

	private boolean checkPackage(String packageName, String version) {
		VersionRange range = VersionRange.parseVersionRange(version);
		return false;
	}

	private String getAttribute(BundleCapability cap, String name) {
		Object obj = cap.getAttributes().get(name);
		return obj != null ? obj.toString() : null;
	}
}

