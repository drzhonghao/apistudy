

import java.io.PrintStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.apache.cassandra.config.EncryptionOptions;
import org.apache.cassandra.stress.settings.GroupedOptions;
import org.apache.cassandra.stress.util.ResultLogger;
import org.apache.cassandra.thrift.ITransportFactory;
import org.apache.cassandra.thrift.SSLTransportFactory;


public class SettingsTransport implements Serializable {
	private String fqFactoryClass = null;

	private final SettingsTransport.TOptions options;

	private ITransportFactory factory;

	public SettingsTransport(SettingsTransport.TOptions options) {
		this.options = options;
		try {
			Class<?> clazz = Class.forName(fqFactoryClass);
			if (!(ITransportFactory.class.isAssignableFrom(clazz)))
				throw new IllegalArgumentException((clazz + " is not a valid transport factory"));

			clazz.newInstance();
		} catch (Exception e) {
		}
		fqFactoryClass = null;
	}

	private void configureTransportFactory(ITransportFactory transportFactory, SettingsTransport.TOptions options) {
		Map<String, String> factoryOptions = new HashMap<>();
		if (transportFactory.supportedOptions().contains(SSLTransportFactory.TRUSTSTORE)) {
		}
		if (transportFactory.supportedOptions().contains(SSLTransportFactory.TRUSTSTORE_PASSWORD)) {
		}
		if (transportFactory.supportedOptions().contains(SSLTransportFactory.KEYSTORE)) {
		}
		if (transportFactory.supportedOptions().contains(SSLTransportFactory.KEYSTORE_PASSWORD)) {
		}
		if (transportFactory.supportedOptions().contains(SSLTransportFactory.PROTOCOL)) {
		}
		if (transportFactory.supportedOptions().contains(SSLTransportFactory.CIPHER_SUITES)) {
		}
		for (String optionKey : transportFactory.supportedOptions())
			if ((System.getProperty(optionKey)) != null)
				factoryOptions.put(optionKey, System.getProperty(optionKey));


		transportFactory.setOptions(factoryOptions);
	}

	public synchronized ITransportFactory getFactory() {
		if ((factory) == null) {
			try {
				this.factory = ((ITransportFactory) (Class.forName(fqFactoryClass).newInstance()));
				configureTransportFactory(this.factory, this.options);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		return factory;
	}

	public EncryptionOptions.ClientEncryptionOptions getEncryptionOptions() {
		EncryptionOptions.ClientEncryptionOptions encOptions = new EncryptionOptions.ClientEncryptionOptions();
		return encOptions;
	}

	static class TOptions extends GroupedOptions implements Serializable {}

	public void printSettings(ResultLogger out) {
		out.println(("  " + (options.getOptionAsString())));
	}

	public static SettingsTransport get(Map<String, String[]> clArgs) {
		String[] params = clArgs.remove("-transport");
		if (params == null)
			return new SettingsTransport(new SettingsTransport.TOptions());

		GroupedOptions options = GroupedOptions.select(params, new SettingsTransport.TOptions());
		if (options == null) {
			SettingsTransport.printHelp();
			System.out.println("Invalid -transport options provided, see output for valid options");
			System.exit(1);
		}
		return new SettingsTransport(((SettingsTransport.TOptions) (options)));
	}

	public static void printHelp() {
		GroupedOptions.printOptions(System.out, "-transport", new SettingsTransport.TOptions());
	}

	public static Runnable helpPrinter() {
		return new Runnable() {
			@Override
			public void run() {
				SettingsTransport.printHelp();
			}
		};
	}
}

