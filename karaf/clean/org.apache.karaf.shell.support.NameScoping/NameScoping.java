import org.apache.karaf.shell.support.*;


import org.apache.karaf.shell.api.console.Session;

/**
 * A helper class for name scoping
 */
public class NameScoping {

    public static final String MULTI_SCOPE_MODE_KEY = "MULTI_SCOPE_MODE";

    /**
     * Return the name of the command which can omit the global scope prefix if the command starts with the
     * same prefix as the current application.
     *
     * @param session the command session.
     * @param key the command key.
     * @return the command without the prefix.
     */
    public static String getCommandNameWithoutGlobalPrefix(Session session, String key) {
        if (!isMultiScopeMode(session)) {
            String globalScope = (String) (session != null ? session.get("APPLICATION") : null);
            if (globalScope != null) {
                String prefix = globalScope + ":";
                if (key.startsWith(prefix)) {
                    // TODO we may only want to do this for single-scope mode when outside of OSGi?
                    // so we may want to also check for a isMultiScope mode == false
                    return key.substring(prefix.length());
                }
            }
        }
        return key;
    }

    /**
     * Return true if the given scope is the global scope so that it can be hidden from help messages.
     *
     * @param session the command session.
     * @param scope the command scope.
     * @return true if the command scope is global, false else.
     */
    public static boolean isGlobalScope(Session session, String scope) {
        if (session == null)
            return false;

        if (!isMultiScopeMode(session)) {
            String globalScope = (String) session.get("APPLICATION");
            if (globalScope != null) {
                return scope.equals(globalScope);
            }
        }
        return false;
    }

    /**
     * Return true if we are in multi-scope mode (the default) or if we are in single scope mode which means we
     * avoid prefixing commands with their scope.
     *
     * @param session the command session.
     * @return true if the command is multi-scoped, false else.
     */
    public static boolean isMultiScopeMode(Session session) {
        if (session == null)
            return false;

        Object value = session.get(MULTI_SCOPE_MODE_KEY);
        return !(value != null && value.equals("false"));
    }
}
