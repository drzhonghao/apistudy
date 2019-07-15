import org.apache.karaf.jaas.modules.properties.*;


import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.felix.utils.properties.Properties;
import org.apache.karaf.jaas.modules.encryption.EncryptionSupport;
import org.apache.karaf.util.StreamUtils;
import org.apache.karaf.util.ThreadUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AutoEncryptionSupport implements Runnable, Closeable {

    private final Logger LOGGER = LoggerFactory.getLogger(AutoEncryptionSupport.class);
    private volatile boolean running;
    private EncryptionSupport encryptionSupport;
    private ExecutorService executor;

    public AutoEncryptionSupport(Map<String, Object> properties) {
        running = true;
        encryptionSupport = new EncryptionSupport(properties);
        executor = Executors.newSingleThreadExecutor(ThreadUtils.namedThreadFactory("encryption"));
        executor.execute(this);
    }

    public void close() {
        running = false;
        executor.shutdown();
        try {
            executor.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            // Ignore
        }
    }

    @Override
    public void run() {
        WatchService watchService = null;
        try {
            watchService = FileSystems.getDefault().newWatchService();
            Path dir = Paths.get(System.getProperty("karaf.etc"));
            dir.register(watchService, ENTRY_MODIFY);

            Path file = dir.resolve("users.properties");
            encryptedPassword(new Properties(file.toFile()));

            while (running) {
                try {
                    WatchKey key = watchService.poll(1, TimeUnit.SECONDS);
                    if (key == null) {
                        continue;
                    }
                    for (WatchEvent<?> event : key.pollEvents()) {
                        @SuppressWarnings("unchecked")
                        WatchEvent<Path> ev = (WatchEvent<Path>)event;

                        // Context for directory entry event is the file name of entry
                        Path name = dir.resolve(ev.context());
                        if (file.equals(name)) {
                            encryptedPassword(new Properties(file.toFile()));
                        }
                    }
                    key.reset();
                } catch (IOException e) {
                    LOGGER.warn(e.getMessage(), e);
                } catch (InterruptedException e) {
                    // Ignore as this happens on shutdown
                }
            }

        } catch (IOException e) {
            LOGGER.warn(e.getMessage(), e);
        } finally {
            StreamUtils.close(watchService);
        }
    }

    void encryptedPassword(Properties users) throws IOException {
        boolean changed = false;
        for (String userName : users.keySet()) {
            String user = userName;
            String userInfos = users.get(user);

            if (user.startsWith(PropertiesBackingEngine.GROUP_PREFIX)) {
                continue;
            }

            // the password is in the first position
            String[] infos = userInfos.split(",");
            String storedPassword = infos[0];

            // check if the stored password is flagged as encrypted
            String encryptedPassword = encryptionSupport.encrypt(storedPassword);
            if (!storedPassword.equals(encryptedPassword)) {
                LOGGER.debug("The password isn't flagged as encrypted, encrypt it.");
                userInfos = encryptedPassword + ",";
                for (int i = 1; i < infos.length; i++) {
                    if (i == (infos.length - 1)) {
                        userInfos = userInfos + infos[i];
                    } else {
                        userInfos = userInfos + infos[i] + ",";
                    }
                }
                if (user.contains("\\")) {
                    users.remove(user);
                    user = user.replace("\\", "\\\\");
                }
                users.put(user, userInfos);
                changed = true;
            }
        }
        if (changed) {
            users.save();
        }
    }

}
