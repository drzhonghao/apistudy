

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.felix.utils.json.JSONWriter;
import org.apache.felix.webconsole.AbstractWebConsolePlugin;
import org.apache.felix.webconsole.WebConsoleConstants;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.features.Repository;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class FeaturesPlugin extends AbstractWebConsolePlugin {
	private final Logger log = LoggerFactory.getLogger(FeaturesPlugin.class);

	public static final String NAME = "features";

	public static final String LABEL = "Features";

	private ClassLoader classLoader;

	private String featuresJs = "/features/res/ui/features.js";

	private FeaturesService featuresService;

	private BundleContext bundleContext;

	@Override
	protected boolean isHtmlRequest(HttpServletRequest request) {
		return true;
	}

	public void start() {
		super.activate(bundleContext);
		this.classLoader = this.getClass().getClassLoader();
		this.log.info(((FeaturesPlugin.LABEL) + " plugin activated"));
	}

	public void stop() {
		this.log.info(((FeaturesPlugin.LABEL) + " plugin deactivated"));
		super.deactivate();
	}

	@Override
	public String getLabel() {
		return FeaturesPlugin.NAME;
	}

	@Override
	public String getTitle() {
		return FeaturesPlugin.LABEL;
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
		boolean success = false;
		final String action = req.getParameter("action");
		final String feature = req.getParameter("feature");
		final String version = req.getParameter("version");
		final String url = req.getParameter("url");
		if (action == null) {
			success = true;
		}else
			if ("installFeature".equals(action)) {
				success = this.installFeature(feature, version);
			}else
				if ("uninstallFeature".equals(action)) {
					success = this.uninstallFeature(feature, version);
				}else
					if ("refreshRepository".equals(action)) {
						success = this.refreshRepository(url);
					}else
						if ("removeRepository".equals(action)) {
							success = this.removeRepository(url);
						}else
							if ("addRepository".equals(action)) {
								success = this.addRepository(url);
							}





		if (success) {
			try {
				Thread.sleep(800);
			} catch (InterruptedException e) {
			}
			this.renderJSON(resp);
		}else {
			super.doPost(req, resp);
		}
	}

	@Override
	protected void renderContent(HttpServletRequest request, HttpServletResponse response) throws IOException {
		final PrintWriter pw = response.getWriter();
		String appRoot = ((String) (request.getAttribute(WebConsoleConstants.ATTR_APP_ROOT)));
		final String featuresScriptTag = (("<script src='" + appRoot) + (this.featuresJs)) + "' language='JavaScript'></script>";
		pw.println(featuresScriptTag);
		pw.println("<script type='text/javascript'>");
		pw.println("// <![CDATA[");
		pw.println((("var imgRoot = '" + appRoot) + "/res/imgs';"));
		pw.println("// ]]>");
		pw.println("</script>");
		pw.println("<div id='plugin_content'>");
		pw.println("<script type='text/javascript'>");
		pw.println("// <![CDATA[");
		pw.print("renderFeatures( ");
		writeJSON(pw);
		pw.println(" )");
		pw.println("// ]]>");
		pw.println("</script>");
	}

	protected URL getResource(String path) {
		path = path.substring(((FeaturesPlugin.NAME.length()) + 1));
		if ((path == null) || (path.isEmpty())) {
			return null;
		}
		URL url = this.classLoader.getResource(path);
		if (url != null) {
			InputStream ins = null;
			try {
				ins = url.openStream();
				if (ins == null) {
					this.log.error(("failed to open " + url));
					url = null;
				}
			} catch (IOException e) {
				this.log.error(e.getMessage(), e);
				url = null;
			} finally {
				if (ins != null) {
					try {
						ins.close();
					} catch (IOException e) {
						this.log.error(e.getMessage(), e);
					}
				}
			}
		}
		return url;
	}

	private boolean installFeature(String feature, String version) {
		boolean success = false;
		if ((featuresService) == null) {
			this.log.error("Features service is not available");
		}
		try {
			featuresService.installFeature(feature, version);
			success = true;
		} catch (Exception e) {
			this.log.error("Can't install feature {}/{}", feature, version, e);
		}
		return success;
	}

	private boolean uninstallFeature(String feature, String version) {
		boolean success = false;
		if ((featuresService) == null) {
			this.log.error("Features service is not available");
		}
		try {
			featuresService.uninstallFeature(feature, version);
			success = true;
		} catch (Exception e) {
			this.log.error("Can't uninstall feature {}/{}", feature, version, e);
		}
		return success;
	}

	private boolean removeRepository(String url) {
		boolean success = false;
		if ((featuresService) == null) {
			this.log.error("Features service is not available");
		}
		try {
			featuresService.removeRepository(new URI(url));
			success = true;
		} catch (Exception e) {
			this.log.error("Can't remove features repository {}", url, e);
		}
		return success;
	}

	private boolean refreshRepository(String url) {
		boolean success = false;
		if ((featuresService) == null) {
			this.log.error("Features service is not available");
		}
		try {
			featuresService.refreshRepository(new URI(url));
			success = true;
		} catch (Exception e) {
			this.log.error("Can't refresh features repository {}", url, e);
		}
		return success;
	}

	private boolean addRepository(String url) {
		boolean success = false;
		if ((featuresService) == null) {
			this.log.error("Features service is not available");
		}
		try {
			featuresService.addRepository(new URI(url));
			success = true;
		} catch (Exception e) {
			this.log.error("Can't add features repository {}", url, e);
		}
		return success;
	}

	private void renderJSON(final HttpServletResponse response) throws IOException {
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		final PrintWriter pw = response.getWriter();
		writeJSON(pw);
	}

	private void writeJSON(final PrintWriter pw) throws IOException {
		final List<Repository> repositories = this.getRepositories();
		final JSONWriter jw = new JSONWriter(pw);
		jw.object();
		jw.key("status");
		jw.key("repositories");
		jw.array();
		for (Repository r : repositories) {
			jw.object();
			jw.key("name");
			String name = "";
			if ((r.getName()) != null)
				name = r.getName();

			jw.value(name);
			jw.key("url");
			String uri = r.getURI().toString();
			jw.value(uri);
			jw.key("actions");
			jw.array();
			boolean enable = true;
			if (uri.startsWith("bundle")) {
				enable = false;
			}
			action(jw, enable, "refreshRepository", "Refresh", "refresh");
			action(jw, enable, "removeRepository", "Remove", "delete");
			jw.endArray();
			jw.endObject();
		}
		jw.endArray();
		jw.key("features");
		jw.array();
		jw.endArray();
		jw.endObject();
	}

	private List<Repository> getRepositories() {
		List<Repository> repositories = new ArrayList<>();
		if ((featuresService) == null) {
			this.log.error("Features service is not available");
			return repositories;
		}
		try {
			for (Repository r : featuresService.listRepositories()) {
				repositories.add(r);
			}
		} catch (Exception e) {
			this.log.error(e.getMessage());
		}
		return repositories;
	}

	class ExtendedFeatureComparator {}

	private void appendFeatureInfoCount(final StringBuffer buf, String msg, int count) {
		buf.append(count);
		buf.append(" feature");
		if (count != 1)
			buf.append('s');

		buf.append(' ');
		buf.append(msg);
	}

	private void action(JSONWriter jw, boolean enabled, String op, String title, String image) throws IOException {
		jw.object();
		jw.key("enabled").value(enabled);
		jw.key("op").value(op);
		jw.key("title").value(title);
		jw.key("image").value(image);
		jw.endObject();
	}

	public void setFeaturesService(FeaturesService featuresService) {
		this.featuresService = featuresService;
	}

	public void setBundleContext(BundleContext bundleContext) {
		this.bundleContext = bundleContext;
	}
}

