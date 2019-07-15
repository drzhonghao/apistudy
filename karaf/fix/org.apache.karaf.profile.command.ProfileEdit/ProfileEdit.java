

import java.util.Arrays;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.Terminal;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Command(name = "edit", scope = "profile", description = "Edits the specified profile", detailedDescription = "classpath:profileEdit.txt")
@Service
public class ProfileEdit implements Action {
	private static final Logger LOGGER = LoggerFactory.getLogger(ProfileEdit.class);

	static final String DELIMITER = ",";

	static final String PID_KEY_SEPARATOR = "/";

	static final String FILE_INSTALL_FILENAME_PROPERTY = "felix.fileinstall.filename";

	@Option(name = "-r", aliases = { "--repositories" }, description = "Edit the features repositories. To specify multiple repositories, specify this flag multiple times.", required = false, multiValued = true)
	private String[] repositories;

	@Option(name = "-f", aliases = { "--features" }, description = "Edit features. To specify multiple features, specify this flag multiple times. For example, --features foo --features bar.", required = false, multiValued = true)
	private String[] features;

	@Option(name = "-l", aliases = { "--libs" }, description = "Edit libraries. To specify multiple libraries, specify this flag multiple times.", required = false, multiValued = true)
	private String[] libs;

	@Option(name = "-n", aliases = { "--endorsed" }, description = "Edit endorsed libraries. To specify multiple libraries, specify this flag multiple times.", required = false, multiValued = true)
	private String[] endorsed;

	@Option(name = "-x", aliases = { "--extension" }, description = "Edit extension libraries. To specify multiple libraries, specify this flag multiple times.", required = false, multiValued = true)
	private String[] extension;

	@Option(name = "-b", aliases = { "--bundles" }, description = "Edit bundles. To specify multiple bundles, specify this flag multiple times.", required = false, multiValued = true)
	private String[] bundles;

	@Option(name = "-o", aliases = { "--overrides" }, description = "Edit overrides. To specify multiple libraries, specify this flag multiple times.", required = false, multiValued = true)
	private String[] overrides;

	@Option(name = "-p", aliases = { "--pid" }, description = "Edit an OSGi configuration property, specified in the format <PID>/<Property>. To specify multiple properties, specify this flag multiple times.", required = false, multiValued = true)
	private String[] pidProperties;

	@Option(name = "-s", aliases = { "--system" }, description = "Edit the Java system properties that affect installed bundles (analogous to editing etc/system.properties in a root container).", required = false, multiValued = true)
	private String[] systemProperties;

	@Option(name = "-c", aliases = { "--config" }, description = "Edit the Java system properties that affect the karaf container (analogous to editing etc/config.properties in a root container).", required = false, multiValued = true)
	private String[] configProperties;

	@Option(name = "-i", aliases = { "--import-pid" }, description = "Imports the pids that are edited, from local OSGi config admin", required = false, multiValued = false)
	private boolean importPid = false;

	@Option(name = "--resource", description = "Selects a resource under the profile to edit. This option should only be used alone.", required = false, multiValued = false)
	private String resource;

	@Option(name = "--set", description = "Set or create values (selected by default).")
	private boolean set = true;

	@Option(name = "--delete", description = "Delete values. This option can be used to delete a feature, a bundle or a pid from the profile.")
	private boolean delete = false;

	@Option(name = "--append", description = "Append value to a delimited list. It is only usable with the system, config & pid options")
	private boolean append = false;

	@Option(name = "--remove", description = "Removes value from a delimited list. It is only usable with the system, config & pid options")
	private boolean remove = false;

	@Option(name = "--delimiter", description = "Specifies the delimiter to use for appends and removals.")
	private String delimiter = ",";

	@Argument(index = 0, name = "profile", description = "The target profile to edit", required = true, multiValued = false)
	private String profileName;

	@Reference
	private ConfigurationAdmin configurationAdmin;

	@Reference
	Terminal terminal;

	@Override
	public Object execute() throws Exception {
		if (delete) {
			set = false;
		}
		return null;
	}

	public void updatedDelimitedList(Map<String, Object> map, String key, String value, String delimiter, boolean set, boolean delete, boolean append, boolean remove) {
		if (append || remove) {
			String oldValue = (map.containsKey(key)) ? ((String) (map.get(key))) : "";
			List<String> parts = new LinkedList<>(Arrays.asList(oldValue.split(delimiter)));
			parts.remove("");
			if (append) {
				parts.add(value);
			}
			if (remove) {
				parts.remove(value);
			}
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < (parts.size()); i++) {
				if (i != 0) {
					sb.append(delimiter);
				}
				sb.append(parts.get(i));
			}
			map.put(key, sb.toString());
		}else
			if (set) {
				map.put(key, value);
			}else
				if (delete) {
					map.remove(key);
				}


	}

	private void updateConfig(Map<String, Object> map, String key, Object value, boolean set, boolean delete) {
		if (set) {
			map.put(key, value);
		}else
			if (delete) {
				map.remove(key);
			}

	}

	private void importPidFromLocalConfigAdmin(String pid, Map<String, Object> target) {
		try {
			Configuration[] configuration = configurationAdmin.listConfigurations((("(service.pid=" + pid) + ")"));
			if ((configuration != null) && ((configuration.length) > 0)) {
				Dictionary<String, Object> dictionary = configuration[0].getProperties();
				Enumeration<String> keyEnumeration = dictionary.keys();
				while (keyEnumeration.hasMoreElements()) {
					String key = String.valueOf(keyEnumeration.nextElement());
					if (!(key.equals(ProfileEdit.FILE_INSTALL_FILENAME_PROPERTY))) {
						String value = String.valueOf(dictionary.get(key));
						target.put(key, value);
					}
				} 
			}
		} catch (Exception e) {
			ProfileEdit.LOGGER.warn("Error while importing configuration {} to profile.", pid);
		}
	}

	private Map<String, String> extractConfigs(String configs) {
		Map<String, String> configMap = new HashMap<>();
		String key;
		String value;
		if (configs.contains("=")) {
			key = configs.substring(0, configs.indexOf("="));
			value = configs.substring(((configs.indexOf("=")) + 1));
		}else {
			key = configs;
			value = null;
		}
		if (!(key.isEmpty())) {
			configMap.put(key, value);
		}
		return configMap;
	}
}

