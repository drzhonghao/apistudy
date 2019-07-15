

import java.text.ParseException;
import java.util.Locale;


public final class Version {
	@Deprecated
	public static final Version LUCENE_6_0_0 = new Version(6, 0, 0);

	@Deprecated
	public static final Version LUCENE_6_0_1 = new Version(6, 0, 1);

	@Deprecated
	public static final Version LUCENE_6_1_0 = new Version(6, 1, 0);

	@Deprecated
	public static final Version LUCENE_6_2_0 = new Version(6, 2, 0);

	@Deprecated
	public static final Version LUCENE_6_2_1 = new Version(6, 2, 1);

	@Deprecated
	public static final Version LUCENE_6_3_0 = new Version(6, 3, 0);

	@Deprecated
	public static final Version LUCENE_6_4_0 = new Version(6, 4, 0);

	@Deprecated
	public static final Version LUCENE_6_4_1 = new Version(6, 4, 1);

	@Deprecated
	public static final Version LUCENE_6_4_2 = new Version(6, 4, 2);

	@Deprecated
	public static final Version LUCENE_6_5_0 = new Version(6, 5, 0);

	@Deprecated
	public static final Version LUCENE_6_5_1 = new Version(6, 5, 1);

	@Deprecated
	public static final Version LUCENE_6_6_0 = new Version(6, 6, 0);

	@Deprecated
	public static final Version LUCENE_6_6_1 = new Version(6, 6, 1);

	@Deprecated
	public static final Version LUCENE_6_6_2 = new Version(6, 6, 2);

	@Deprecated
	public static final Version LUCENE_6_6_3 = new Version(6, 6, 3);

	@Deprecated
	public static final Version LUCENE_6_6_4 = new Version(6, 6, 4);

	@Deprecated
	public static final Version LUCENE_7_0_0 = new Version(7, 0, 0);

	@Deprecated
	public static final Version LUCENE_7_0_1 = new Version(7, 0, 1);

	@Deprecated
	public static final Version LUCENE_7_1_0 = new Version(7, 1, 0);

	@Deprecated
	public static final Version LUCENE_7_2_0 = new Version(7, 2, 0);

	@Deprecated
	public static final Version LUCENE_7_2_1 = new Version(7, 2, 1);

	@Deprecated
	public static final Version LUCENE_7_3_0 = new Version(7, 3, 0);

	@Deprecated
	public static final Version LUCENE_7_3_1 = new Version(7, 3, 1);

	public static final Version LUCENE_7_4_0 = new Version(7, 4, 0);

	public static final Version LATEST = Version.LUCENE_7_4_0;

	@Deprecated
	public static final Version LUCENE_CURRENT = Version.LATEST;

	public static Version parse(String version) throws ParseException {
		int major;
		try {
		} catch (NumberFormatException nfe) {
		}
		int minor;
		try {
		} catch (NumberFormatException nfe) {
		}
		int bugfix = 0;
		int prerelease = 0;
		try {
			minor = 0;
			major = 0;
			return new Version(major, minor, bugfix, prerelease);
		} catch (IllegalArgumentException iae) {
			ParseException pe = new ParseException(((("failed to parse version string \"" + version) + "\": ") + (iae.getMessage())), 0);
			pe.initCause(iae);
			throw pe;
		}
	}

	public static Version parseLeniently(String version) throws ParseException {
		String versionOrig = version;
		version = version.toUpperCase(Locale.ROOT);
		switch (version) {
			case "LATEST" :
			case "LUCENE_CURRENT" :
				return Version.LATEST;
			default :
				version = version.replaceFirst("^LUCENE_(\\d+)_(\\d+)_(\\d+)$", "$1.$2.$3").replaceFirst("^LUCENE_(\\d+)_(\\d+)$", "$1.$2.0").replaceFirst("^LUCENE_(\\d)(\\d)$", "$1.$2.0");
				try {
					return Version.parse(version);
				} catch (ParseException pe) {
					ParseException pe2 = new ParseException(((("failed to parse lenient version string \"" + versionOrig) + "\": ") + (pe.getMessage())), 0);
					pe2.initCause(pe);
					throw pe2;
				}
		}
	}

	public static Version fromBits(int major, int minor, int bugfix) {
		return new Version(major, minor, bugfix);
	}

	public final int major;

	public final int minor;

	public final int bugfix;

	public final int prerelease;

	private final int encodedValue;

	private Version(int major, int minor, int bugfix) {
		this(major, minor, bugfix, 0);
	}

	private Version(int major, int minor, int bugfix, int prerelease) {
		this.major = major;
		this.minor = minor;
		this.bugfix = bugfix;
		this.prerelease = prerelease;
		if ((major > 255) || (major < 0)) {
			throw new IllegalArgumentException(("Illegal major version: " + major));
		}
		if ((minor > 255) || (minor < 0)) {
			throw new IllegalArgumentException(("Illegal minor version: " + minor));
		}
		if ((bugfix > 255) || (bugfix < 0)) {
			throw new IllegalArgumentException(("Illegal bugfix version: " + bugfix));
		}
		if ((prerelease > 2) || (prerelease < 0)) {
			throw new IllegalArgumentException(("Illegal prerelease version: " + prerelease));
		}
		if ((prerelease != 0) && ((minor != 0) || (bugfix != 0))) {
			throw new IllegalArgumentException((((((("Prerelease version only supported with major release (got prerelease: " + prerelease) + ", minor: ") + minor) + ", bugfix: ") + bugfix) + ")"));
		}
		encodedValue = (((major << 18) | (minor << 10)) | (bugfix << 2)) | prerelease;
		assert encodedIsValid();
	}

	public boolean onOrAfter(Version other) {
		return (encodedValue) >= (other.encodedValue);
	}

	@Override
	public String toString() {
		if ((prerelease) == 0) {
			return (((("" + (major)) + ".") + (minor)) + ".") + (bugfix);
		}
		return (((((("" + (major)) + ".") + (minor)) + ".") + (bugfix)) + ".") + (prerelease);
	}

	@Override
	public boolean equals(Object o) {
		return ((o != null) && (o instanceof Version)) && ((((Version) (o)).encodedValue) == (encodedValue));
	}

	private boolean encodedIsValid() {
		assert (major) == (((encodedValue) >>> 18) & 255);
		assert (minor) == (((encodedValue) >>> 10) & 255);
		assert (bugfix) == (((encodedValue) >>> 2) & 255);
		assert (prerelease) == ((encodedValue) & 3);
		return true;
	}

	@Override
	public int hashCode() {
		return encodedValue;
	}
}

