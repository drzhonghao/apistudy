

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;


public class ProfileServiceImpl {
	private static final long ACQUIRE_LOCK_TIMEOUT = 25 * 1000L;

	private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();

	private final Path profilesDirectory;

	public ProfileServiceImpl(Path profilesDirectory) throws IOException {
		this.profilesDirectory = profilesDirectory;
		Files.createDirectories(profilesDirectory);
	}

	protected ReadWriteLock getLock() {
		return readWriteLock;
	}

	public boolean hasProfile(String profileId) {
		return false;
	}

	public Collection<String> getProfiles() {
		return null;
	}

	public void deleteProfile(String profileId) {
	}

	protected Collection<String> getProfilesFromCache() {
		return null;
	}
}

