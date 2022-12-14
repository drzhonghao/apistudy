import org.apache.accumulo.monitor.servlets.BasicServlet;
import org.apache.accumulo.monitor.servlets.*;


import java.io.IOException;
import java.util.Collections;
import java.util.List;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.accumulo.core.Constants;
import org.apache.accumulo.core.zookeeper.ZooUtil;
import org.apache.accumulo.monitor.Monitor;
import org.apache.accumulo.server.master.state.DeadServerList;
import org.apache.accumulo.server.monitor.LogService;
import org.apache.accumulo.server.problems.ProblemReports;
import org.apache.accumulo.server.problems.ProblemType;
import org.apache.log4j.Logger;

public class OperationServlet extends BasicServlet {

  private static final long serialVersionUID = 1L;

  @Override
  protected String getTitle(HttpServletRequest req) {
    return "Operations";
  }

  @Override
  public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    String redir = null;
    List<Cookie> cookiesToSet = Collections.emptyList();
    try {
      String operation = req.getParameter("action");
      redir = req.getParameter("redir");

      if (operation != null) {
        for (Class<?> subclass : OperationServlet.class.getClasses()) {
          Object t;
          try {
            t = subclass.newInstance();
          } catch (Exception e) {
            continue;
          }
          if (t instanceof WebOperation) {
            WebOperation op = (WebOperation) t;
            if (op.getClass().getSimpleName().equalsIgnoreCase(operation + "Operation")) {
              cookiesToSet = op.execute(req, log);
              break;
            }
          }
        }
      }
    } catch (Throwable t) {
      log.error(t, t);
    } finally {
      try {
        for (Cookie c : cookiesToSet) {
          resp.addCookie(c);
        }
        resp.sendRedirect(sanitizeRedirect(redir));
        resp.flushBuffer();
      } catch (Throwable t) {
        log.error(t, t);
      }
    }
  }

  private static String sanitizeRedirect(final String url) {
    if (url == null || url.isEmpty() || url.contains("\r") || url.contains("\n")) {
      // prevent HTTP response splitting
      return "/";
    }
    return url.startsWith("/") ? url : ("/" + url);
  }

  private interface WebOperation {
    List<Cookie> execute(HttpServletRequest req, Logger log) throws Exception;
  }

  public static class RefreshOperation implements WebOperation {
    @Override
    public List<Cookie> execute(HttpServletRequest req, Logger log) {
      String rawValue = req.getParameter("value");
      int refreshTime = 5;
      // Verify that the value provided is actually a number
      if (rawValue != null) {
        refreshTime = Integer.parseInt(rawValue);
      }
      return Collections
          .singletonList(createCookie("page.refresh.rate", Integer.toString(refreshTime)));
    }
  }

  public static class ClearLogOperation implements WebOperation {
    @Override
    public List<Cookie> execute(HttpServletRequest req, Logger log) {
      LogService.getInstance().clear();
      return Collections.emptyList();
    }
  }

  public static class ClearTableProblemsOperation implements WebOperation {
    @Override
    public List<Cookie> execute(HttpServletRequest req, Logger log) {
      String table = req.getParameter("table");
      try {
        ProblemReports.getInstance(Monitor.getContext()).deleteProblemReports(table);
      } catch (Exception e) {
        log.error("Failed to delete problem reports for table " + table, e);
      }
      return Collections.emptyList();
    }
  }

  public static class ClearProblemOperation implements WebOperation {
    @Override
    public List<Cookie> execute(HttpServletRequest req, Logger log) {
      String table = req.getParameter("table");
      String resource = req.getParameter("resource");
      String ptype = req.getParameter("ptype");
      try {
        ProblemReports.getInstance(Monitor.getContext()).deleteProblemReport(table,
            ProblemType.valueOf(ptype), resource);
      } catch (Exception e) {
        log.error("Failed to delete problem reports for table " + table, e);
      }
      return Collections.emptyList();
    }
  }

  public static class SortTableOperation implements WebOperation {
    @Override
    public List<Cookie> execute(HttpServletRequest req, Logger log) throws IOException {
      String page = req.getParameter("page");
      String table = req.getParameter("table");
      String asc = req.getParameter("asc");
      String col = req.getParameter("col");
      if (table == null || page == null || (asc == null && col == null))
        return Collections.emptyList();
      page = BasicServlet.encode(page);
      table = BasicServlet.encode(table);
      if (asc == null) {
        col = BasicServlet.encode(col);
        return Collections
            .singletonList(createCookie("tableSort." + page + "." + table + "." + "sortCol", col));
      } else {
        asc = BasicServlet.encode(asc);
        return Collections
            .singletonList(createCookie("tableSort." + page + "." + table + "." + "sortAsc", asc));
      }
    }
  }

  public static class ToggleLegendOperation implements WebOperation {
    @Override
    public List<Cookie> execute(HttpServletRequest req, Logger log) throws Exception {
      String page = req.getParameter("page");
      String table = req.getParameter("table");
      String show = req.getParameter("show");
      if (table == null || page == null || show == null)
        return Collections.emptyList();
      page = BasicServlet.encode(page);
      table = BasicServlet.encode(table);
      show = BasicServlet.encode(show);
      return Collections
          .singletonList(createCookie("tableLegend." + page + "." + table + "." + "show", show));
    }
  }

  public static class ClearDeadServerOperation implements WebOperation {
    @Override
    public List<Cookie> execute(HttpServletRequest req, Logger log) {
      String server = req.getParameter("server");
      // a dead server should have a uniq address: a logger or tserver
      DeadServerList obit = new DeadServerList(
          ZooUtil.getRoot(Monitor.getContext().getInstance()) + Constants.ZDEADTSERVERS);
      obit.delete(server);
      return Collections.emptyList();
    }
  }
}
