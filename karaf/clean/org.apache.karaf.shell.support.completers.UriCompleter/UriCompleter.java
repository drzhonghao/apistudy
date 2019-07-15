import org.apache.karaf.shell.support.completers.*;


import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.apache.karaf.shell.api.console.Candidate;
import org.apache.karaf.shell.api.console.CommandLine;
import org.apache.karaf.shell.api.console.Completer;
import org.apache.karaf.shell.api.console.Session;

public class UriCompleter implements Completer {

    @Override
    public int complete(Session session, CommandLine commandLine, List<String> candidates) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void completeCandidates(Session session, CommandLine commandLine, List<Candidate> candidates) {
        String arg = commandLine.getCursorArgument();
        if (arg != null) {
            if (arg.startsWith("mvn:")) {
                maven(session, commandLine, candidates);
            } else if (arg.startsWith("file:")) {
                file(session, commandLine, candidates);
            }
        }
    }

    private void file(Session session, CommandLine commandLine, List<Candidate> candidates) {
        String buffer = commandLine.getCursorArgument();
        String path = buffer.substring("file:".length(), commandLine.getArgumentPosition());

        String rem = "";
        try {

            Path dir;
            if (path.length() == 0) {
                for (Path root : FileSystems.getDefault().getRootDirectories()) {
                    candidates.add(new Candidate(root.toString(), false));
                }
                dir = Paths.get(".");
            } else {
                dir = Paths.get(decode(path));
                if (!path.endsWith("/")) {
                    rem = dir.getFileName().toString();
                    dir = dir.getParent();
                    if (dir == null) {
                        dir = Paths.get(".");
                    }
                }
            }
            if (Files.isDirectory(dir)) {
                try (DirectoryStream<Path> paths = Files.newDirectoryStream(dir, rem + "*")) {
                    for (Path child : paths) {
                        String name = encode(child.getFileName().toString());
                        boolean isDir = Files.isDirectory(child);
                        if (isDir) {
                            name += "/";
                        }
                        String dirstr = dir.endsWith("/") ? dir.toString() : dir.toString() + "/";
                        candidates.add(new Candidate("file:" + dirstr + name, !isDir));
                    }
                }
            }
        } catch (Exception e) {
            // Ignore
        }
    }

    private String encode(String s) {
        return s.replaceAll(" ", "%20");
    }

    private String decode(String s) {
        return s.replaceAll("%20", " ");
    }

    private void maven(Session session, CommandLine commandLine, List<Candidate> candidates) {
        String repo = System.getProperty("user.home") + "/.m2/repository";
        String buffer = commandLine.getCursorArgument();
        String mvn = buffer.substring("mvn:".length(), commandLine.getArgumentPosition());

        String rem = "";

        try {
            String[] parts = mvn.split("/");
            if (parts.length == 0 || parts.length == 1 && !mvn.endsWith("/")) {
                String known = "";
                String group = "";
                String[] dirs = parts.length > 0 ? parts[0].split("\\.") : new String[] { "" };
                if (parts.length > 0 && parts[0].endsWith(".")) {
                    for (String dir : dirs) {
                        known += dir + "/";
                        group += dir + ".";
                    }
                } else {
                    for (int i = 0; i < dirs.length - 1; i++) {
                        known += dirs[i] + "/";
                        group += dirs[i] + ".";
                    }
                    rem = dirs[dirs.length - 1];
                }
                Path rep = Paths.get(repo);
                Path dir = rep.resolve(known);
                try (DirectoryStream<Path> paths = Files.newDirectoryStream(dir, rem + "*")) {
                    for (Path path : paths) {
                        if (Files.isDirectory(path)) {
                            String name = path.getFileName().toString();
                            candidates.add(new Candidate("mvn:" + group + name, false));
                        }
                    }
                }
                rem = group + rem;
            } else if (parts.length == 1 || parts.length == 2 && !mvn.endsWith("/")) {
                rem = parts.length > 1 ? parts[1] : "";
                Path dir = Paths.get(repo + "/" + parts[0].replace(".", "/"));
                try (DirectoryStream<Path> paths = Files.newDirectoryStream(dir, rem + "*")) {
                    for (Path path : paths) {
                        if (Files.isDirectory(path)) {
                            String name = path.getFileName().toString();
                            candidates.add(new Candidate("mvn:" + parts[0] + "/" + name, false));
                        }
                    }
                }
            } else if (parts.length == 2 || parts.length == 3 && !mvn.endsWith("/")) {
                rem = parts.length > 2 ? parts[2] : "";
                Path dir = Paths.get(repo + "/" + parts[0].replace(".", "/") + "/" + parts[1]);
                try (DirectoryStream<Path> paths = Files.newDirectoryStream(dir, rem + "*")) {
                    for (Path path : paths) {
                        if (Files.isDirectory(path)) {
                            String name = path.getFileName().toString();
                            candidates.add(new Candidate("mvn:" + parts[0] + "/" + parts[1] + "/" + name, true));
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Ignore
        }
    }

}
