import org.apache.accumulo.core.trace.wrappers.*;


import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.apache.accumulo.core.trace.Span;
import org.apache.accumulo.core.trace.Trace;
import org.apache.accumulo.core.trace.thrift.TInfo;

public class RpcServerInvocationHandler<I> implements InvocationHandler {

  private final I instance;

  protected RpcServerInvocationHandler(final I serverInstance) {
    instance = serverInstance;
  }

  @Override
  public Object invoke(Object obj, Method method, Object[] args) throws Throwable {
    if (args == null || args.length < 1 || args[0] == null || !(args[0] instanceof TInfo)) {
      try {
        return method.invoke(instance, args);
      } catch (InvocationTargetException ex) {
        throw ex.getCause();
      }
    }
    Span span = Trace.trace((TInfo) args[0], method.getName());
    try {
      return method.invoke(instance, args);
    } catch (InvocationTargetException ex) {
      throw ex.getCause();
    } finally {
      span.stop();
    }
  }
}
