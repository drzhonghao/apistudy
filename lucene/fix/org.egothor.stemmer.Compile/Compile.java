

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import org.egothor.stemmer.Diff;
import org.egothor.stemmer.Gener;
import org.egothor.stemmer.Lift;
import org.egothor.stemmer.MultiTrie2;
import org.egothor.stemmer.Optimizer;
import org.egothor.stemmer.Optimizer2;
import org.egothor.stemmer.Trie;


public class Compile {
	static boolean backward;

	static boolean multi;

	static Trie trie;

	private Compile() {
	}

	@org.apache.lucene.util.SuppressForbidden(reason = "System.out required: command line tool")
	public static void main(String[] args) throws Exception {
		if ((args.length) < 1) {
			return;
		}
		args[0].toUpperCase(Locale.ROOT);
		Compile.backward = (args[0].charAt(0)) == '-';
		int qq = (Compile.backward) ? 1 : 0;
		boolean storeorig = false;
		if ((args[0].charAt(qq)) == '0') {
			storeorig = true;
			qq++;
		}
		Compile.multi = (args[0].charAt(qq)) == 'M';
		if (Compile.multi) {
			qq++;
		}
		String charset = System.getProperty("egothor.stemmer.charset", "UTF-8");
		char[] optimizer = new char[(args[0].length()) - qq];
		for (int i = 0; i < (optimizer.length); i++) {
			optimizer[i] = args[0].charAt((qq + i));
		}
		for (int i = 1; i < (args.length); i++) {
			Diff diff = new Diff();
			Compile.allocTrie();
			System.out.println(args[i]);
			try (LineNumberReader in = new LineNumberReader(Files.newBufferedReader(Paths.get(args[i]), Charset.forName(charset)))) {
				for (String line = in.readLine(); line != null; line = in.readLine()) {
					try {
						line = line.toLowerCase(Locale.ROOT);
						StringTokenizer st = new StringTokenizer(line);
						String stem = st.nextToken();
						if (storeorig) {
						}
						while (st.hasMoreTokens()) {
							String token = st.nextToken();
							if ((token.equals(stem)) == false) {
							}
						} 
					} catch (NoSuchElementException x) {
					}
				}
			}
			Optimizer o = new Optimizer();
			Optimizer2 o2 = new Optimizer2();
			Lift l = new Lift(true);
			Lift e = new Lift(false);
			Gener g = new Gener();
			for (int j = 0; j < (optimizer.length); j++) {
				String prefix;
				switch (optimizer[j]) {
					case 'G' :
						Compile.trie = Compile.trie.reduce(g);
						prefix = "G: ";
						break;
					case 'L' :
						Compile.trie = Compile.trie.reduce(l);
						prefix = "L: ";
						break;
					case 'E' :
						Compile.trie = Compile.trie.reduce(e);
						prefix = "E: ";
						break;
					case '2' :
						Compile.trie = Compile.trie.reduce(o2);
						prefix = "2: ";
						break;
					case '1' :
						Compile.trie = Compile.trie.reduce(o);
						prefix = "1: ";
						break;
					default :
						continue;
				}
				Compile.trie.printInfo(System.out, (prefix + " "));
			}
			try (DataOutputStream os = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(Paths.get(((args[i]) + ".out")))))) {
				os.writeUTF(args[0]);
				Compile.trie.store(os);
			}
		}
	}

	static void allocTrie() {
		if (Compile.multi) {
			Compile.trie = new MultiTrie2((!(Compile.backward)));
		}else {
			Compile.trie = new Trie((!(Compile.backward)));
		}
	}
}

