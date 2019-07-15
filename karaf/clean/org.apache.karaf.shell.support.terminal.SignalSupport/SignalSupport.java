import org.apache.karaf.shell.support.terminal.*;


import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;

import org.apache.karaf.shell.api.console.Signal;
import org.apache.karaf.shell.api.console.SignalListener;

public class SignalSupport {

    protected final ConcurrentMap<Signal, Set<SignalListener>> listeners = new ConcurrentHashMap<>(3);

    public void addSignalListener(SignalListener listener, Signal... signals) {
        if (signals == null) {
            throw new IllegalArgumentException("signals may not be null");
        }
        if (listener == null) {
            throw new IllegalArgumentException("listener may not be null");
        }
        for (Signal s : signals) {
            getSignalListeners(s, true).add(listener);
        }
    }

    public void addSignalListener(SignalListener listener) {
        addSignalListener(listener, EnumSet.allOf(Signal.class));
    }

    public void addSignalListener(SignalListener listener, EnumSet<Signal> signals) {
        if (signals == null) {
            throw new IllegalArgumentException("signals may not be null");
        }
        addSignalListener(listener, signals.toArray(new Signal[signals.size()]));
    }

    public void removeSignalListener(SignalListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener may not be null");
        }
        for (Signal s : EnumSet.allOf(Signal.class)) {
            final Set<SignalListener> ls = getSignalListeners(s, false);
            if (ls != null) {
                ls.remove(listener);
            }
        }
    }

    public void signal(Signal signal) {
        final Set<SignalListener> ls = getSignalListeners(signal, false);
        if (ls != null) {
            for (SignalListener l : ls) {
                l.signal(signal);
            }
        }
    }

    protected Set<SignalListener> getSignalListeners(Signal signal, boolean create) {
        return listeners.compute(signal, (sig, lst) -> lst != null ? lst : create ? new CopyOnWriteArraySet<>() : null);
    }
}
