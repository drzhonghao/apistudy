import org.apache.karaf.management.internal.*;


import javax.management.MBeanServer;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MBeanInvocationHandler implements InvocationHandler {

    private final MBeanServer wrapped;

    private final InvocationHandler guard;

    private final List<String> guarded = Collections.unmodifiableList(Arrays.asList("invoke", "getAttribute", "getAttributes", "setAttribute", "setAttributes"));

    public MBeanInvocationHandler(MBeanServer mBeanServer, InvocationHandler guard) {
        wrapped = mBeanServer;
        this.guard = guard;
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (guarded.contains(method.getName())) {
            guard.invoke(proxy, method, args);
        }

        if (method.getName().equals("equals") && method.getParameterTypes().length == 1 && method.getParameterTypes()[0] == Object.class) {
            Object target = args[0];
            if (target != null && Proxy.isProxyClass(target.getClass())) {
                InvocationHandler handler = Proxy.getInvocationHandler(target);
                if (handler instanceof MBeanInvocationHandler) {
                    args[0] = ((MBeanInvocationHandler) handler).wrapped;
                }
            }
        } else if (method.getName().equals("finalize") && method.getParameterTypes().length == 0) {
            // special case finalize, don't route through to delegate because that will get its own call
            return null;
        }

        try {
            return method.invoke(wrapped, args);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    public MBeanServer getDelegate() {
        return wrapped;
    }
}
