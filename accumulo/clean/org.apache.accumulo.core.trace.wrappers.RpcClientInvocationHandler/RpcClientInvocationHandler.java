import org.apache.accumulo.core.trace.wrappers.*;


import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.apache.accumulo.core.trace.Span;
import org.apache.accumulo.core.trace.Trace;
import org.apache.accumulo.core.trace.Tracer;
import org.apache.accumulo.core.trace.thrift.TInfo;

public class RpcClientInvocationHandler<I> implements InvocationHandler {

  private final I instance;

  protected RpcClientInvocationHandler(final I clientInstance) {
    instance = clientInstance;
  }

  @Override
  public Object invoke(Object obj, Method method, Object[] args) throws Throwable {
    if (args == null || args.length < 1 || args[0] != null) {
      return method.invoke(instance, args);
    }
    Class<?> klass = method.getParameterTypes()[0];
    if (TInfo.class.isAssignableFrom(klass)) {
      args[0] = Tracer.traceInfo();
    }
    Span span = Trace.start("client:" + method.getName());
    try {
      return method.invoke(instance, args);
    } catch (InvocationTargetException ex) {
      throw ex.getCause();
    } finally {
      span.stop();
    }
  }
}
