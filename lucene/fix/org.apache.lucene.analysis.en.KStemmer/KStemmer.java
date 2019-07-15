

import org.apache.lucene.analysis.CharArrayMap;
import org.apache.lucene.analysis.util.OpenStringBuilder;


public class KStemmer {
	private static final int MaxWordLen = 50;

	private static final String[] exceptionWords = new String[]{ "aide", "bathe", "caste", "cute", "dame", "dime", "doge", "done", "dune", "envelope", "gage", "grille", "grippe", "lobe", "mane", "mare", "nape", "node", "pane", "pate", "plane", "pope", "programme", "quite", "ripe", "rote", "rune", "sage", "severe", "shoppe", "sine", "slime", "snipe", "steppe", "suite", "swinge", "tare", "tine", "tope", "tripe", "twine" };

	private static final String[][] directConflations = new String[][]{ new String[]{ "aging", "age" }, new String[]{ "going", "go" }, new String[]{ "goes", "go" }, new String[]{ "lying", "lie" }, new String[]{ "using", "use" }, new String[]{ "owing", "owe" }, new String[]{ "suing", "sue" }, new String[]{ "dying", "die" }, new String[]{ "tying", "tie" }, new String[]{ "vying", "vie" }, new String[]{ "aged", "age" }, new String[]{ "used", "use" }, new String[]{ "vied", "vie" }, new String[]{ "cued", "cue" }, new String[]{ "died", "die" }, new String[]{ "eyed", "eye" }, new String[]{ "hued", "hue" }, new String[]{ "iced", "ice" }, new String[]{ "lied", "lie" }, new String[]{ "owed", "owe" }, new String[]{ "sued", "sue" }, new String[]{ "toed", "toe" }, new String[]{ "tied", "tie" }, new String[]{ "does", "do" }, new String[]{ "doing", "do" }, new String[]{ "aeronautical", "aeronautics" }, new String[]{ "mathematical", "mathematics" }, new String[]{ "political", "politics" }, new String[]{ "metaphysical", "metaphysics" }, new String[]{ "cylindrical", "cylinder" }, new String[]{ "nazism", "nazi" }, new String[]{ "ambiguity", "ambiguous" }, new String[]{ "barbarity", "barbarous" }, new String[]{ "credulity", "credulous" }, new String[]{ "generosity", "generous" }, new String[]{ "spontaneity", "spontaneous" }, new String[]{ "unanimity", "unanimous" }, new String[]{ "voracity", "voracious" }, new String[]{ "fled", "flee" }, new String[]{ "miscarriage", "miscarry" } };

	private static final String[][] countryNationality = new String[][]{ new String[]{ "afghan", "afghanistan" }, new String[]{ "african", "africa" }, new String[]{ "albanian", "albania" }, new String[]{ "algerian", "algeria" }, new String[]{ "american", "america" }, new String[]{ "andorran", "andorra" }, new String[]{ "angolan", "angola" }, new String[]{ "arabian", "arabia" }, new String[]{ "argentine", "argentina" }, new String[]{ "armenian", "armenia" }, new String[]{ "asian", "asia" }, new String[]{ "australian", "australia" }, new String[]{ "austrian", "austria" }, new String[]{ "azerbaijani", "azerbaijan" }, new String[]{ "azeri", "azerbaijan" }, new String[]{ "bangladeshi", "bangladesh" }, new String[]{ "belgian", "belgium" }, new String[]{ "bermudan", "bermuda" }, new String[]{ "bolivian", "bolivia" }, new String[]{ "bosnian", "bosnia" }, new String[]{ "botswanan", "botswana" }, new String[]{ "brazilian", "brazil" }, new String[]{ "british", "britain" }, new String[]{ "bulgarian", "bulgaria" }, new String[]{ "burmese", "burma" }, new String[]{ "californian", "california" }, new String[]{ "cambodian", "cambodia" }, new String[]{ "canadian", "canada" }, new String[]{ "chadian", "chad" }, new String[]{ "chilean", "chile" }, new String[]{ "chinese", "china" }, new String[]{ "colombian", "colombia" }, new String[]{ "croat", "croatia" }, new String[]{ "croatian", "croatia" }, new String[]{ "cuban", "cuba" }, new String[]{ "cypriot", "cyprus" }, new String[]{ "czechoslovakian", "czechoslovakia" }, new String[]{ "danish", "denmark" }, new String[]{ "egyptian", "egypt" }, new String[]{ "equadorian", "equador" }, new String[]{ "eritrean", "eritrea" }, new String[]{ "estonian", "estonia" }, new String[]{ "ethiopian", "ethiopia" }, new String[]{ "european", "europe" }, new String[]{ "fijian", "fiji" }, new String[]{ "filipino", "philippines" }, new String[]{ "finnish", "finland" }, new String[]{ "french", "france" }, new String[]{ "gambian", "gambia" }, new String[]{ "georgian", "georgia" }, new String[]{ "german", "germany" }, new String[]{ "ghanian", "ghana" }, new String[]{ "greek", "greece" }, new String[]{ "grenadan", "grenada" }, new String[]{ "guamian", "guam" }, new String[]{ "guatemalan", "guatemala" }, new String[]{ "guinean", "guinea" }, new String[]{ "guyanan", "guyana" }, new String[]{ "haitian", "haiti" }, new String[]{ "hawaiian", "hawaii" }, new String[]{ "holland", "dutch" }, new String[]{ "honduran", "honduras" }, new String[]{ "hungarian", "hungary" }, new String[]{ "icelandic", "iceland" }, new String[]{ "indonesian", "indonesia" }, new String[]{ "iranian", "iran" }, new String[]{ "iraqi", "iraq" }, new String[]{ "iraqui", "iraq" }, new String[]{ "irish", "ireland" }, new String[]{ "israeli", "israel" }, new String[]{ "italian", "italy" }, new String[]{ "jamaican", "jamaica" }, new String[]{ "japanese", "japan" }, new String[]{ "jordanian", "jordan" }, new String[]{ "kampuchean", "cambodia" }, new String[]{ "kenyan", "kenya" }, new String[]{ "korean", "korea" }, new String[]{ "kuwaiti", "kuwait" }, new String[]{ "lankan", "lanka" }, new String[]{ "laotian", "laos" }, new String[]{ "latvian", "latvia" }, new String[]{ "lebanese", "lebanon" }, new String[]{ "liberian", "liberia" }, new String[]{ "libyan", "libya" }, new String[]{ "lithuanian", "lithuania" }, new String[]{ "macedonian", "macedonia" }, new String[]{ "madagascan", "madagascar" }, new String[]{ "malaysian", "malaysia" }, new String[]{ "maltese", "malta" }, new String[]{ "mauritanian", "mauritania" }, new String[]{ "mexican", "mexico" }, new String[]{ "micronesian", "micronesia" }, new String[]{ "moldovan", "moldova" }, new String[]{ "monacan", "monaco" }, new String[]{ "mongolian", "mongolia" }, new String[]{ "montenegran", "montenegro" }, new String[]{ "moroccan", "morocco" }, new String[]{ "myanmar", "burma" }, new String[]{ "namibian", "namibia" }, new String[]{ "nepalese", "nepal" }, new String[]{ "nicaraguan", "nicaragua" }, new String[]{ "nigerian", "nigeria" }, new String[]{ "norwegian", "norway" }, new String[]{ "omani", "oman" }, new String[]{ "pakistani", "pakistan" }, new String[]{ "panamanian", "panama" }, new String[]{ "papuan", "papua" }, new String[]{ "paraguayan", "paraguay" }, new String[]{ "peruvian", "peru" }, new String[]{ "portuguese", "portugal" }, new String[]{ "romanian", "romania" }, new String[]{ "rumania", "romania" }, new String[]{ "rumanian", "romania" }, new String[]{ "russian", "russia" }, new String[]{ "rwandan", "rwanda" }, new String[]{ "samoan", "samoa" }, new String[]{ "scottish", "scotland" }, new String[]{ "serb", "serbia" }, new String[]{ "serbian", "serbia" }, new String[]{ "siam", "thailand" }, new String[]{ "siamese", "thailand" }, new String[]{ "slovakia", "slovak" }, new String[]{ "slovakian", "slovak" }, new String[]{ "slovenian", "slovenia" }, new String[]{ "somali", "somalia" }, new String[]{ "somalian", "somalia" }, new String[]{ "spanish", "spain" }, new String[]{ "swedish", "sweden" }, new String[]{ "swiss", "switzerland" }, new String[]{ "syrian", "syria" }, new String[]{ "taiwanese", "taiwan" }, new String[]{ "tanzanian", "tanzania" }, new String[]{ "texan", "texas" }, new String[]{ "thai", "thailand" }, new String[]{ "tunisian", "tunisia" }, new String[]{ "turkish", "turkey" }, new String[]{ "ugandan", "uganda" }, new String[]{ "ukrainian", "ukraine" }, new String[]{ "uruguayan", "uruguay" }, new String[]{ "uzbek", "uzbekistan" }, new String[]{ "venezuelan", "venezuela" }, new String[]{ "vietnamese", "viet" }, new String[]{ "virginian", "virginia" }, new String[]{ "yemeni", "yemen" }, new String[]{ "yugoslav", "yugoslavia" }, new String[]{ "yugoslavian", "yugoslavia" }, new String[]{ "zambian", "zambia" }, new String[]{ "zealander", "zealand" }, new String[]{ "zimbabwean", "zimbabwe" } };

	private static final String[] supplementDict = new String[]{ "aids", "applicator", "capacitor", "digitize", "electromagnet", "ellipsoid", "exosphere", "extensible", "ferromagnet", "graphics", "hydromagnet", "polygraph", "toroid", "superconduct", "backscatter", "connectionism" };

	private static final String[] properNouns = new String[]{ "abrams", "achilles", "acropolis", "adams", "agnes", "aires", "alexander", "alexis", "alfred", "algiers", "alps", "amadeus", "ames", "amos", "andes", "angeles", "annapolis", "antilles", "aquarius", "archimedes", "arkansas", "asher", "ashly", "athens", "atkins", "atlantis", "avis", "bahamas", "bangor", "barbados", "barger", "bering", "brahms", "brandeis", "brussels", "bruxelles", "cairns", "camoros", "camus", "carlos", "celts", "chalker", "charles", "cheops", "ching", "christmas", "cocos", "collins", "columbus", "confucius", "conners", "connolly", "copernicus", "cramer", "cyclops", "cygnus", "cyprus", "dallas", "damascus", "daniels", "davies", "davis", "decker", "denning", "dennis", "descartes", "dickens", "doris", "douglas", "downs", "dreyfus", "dukakis", "dulles", "dumfries", "ecclesiastes", "edwards", "emily", "erasmus", "euphrates", "evans", "everglades", "fairbanks", "federales", "fisher", "fitzsimmons", "fleming", "forbes", "fowler", "france", "francis", "goering", "goodling", "goths", "grenadines", "guiness", "hades", "harding", "harris", "hastings", "hawkes", "hawking", "hayes", "heights", "hercules", "himalayas", "hippocrates", "hobbs", "holmes", "honduras", "hopkins", "hughes", "humphreys", "illinois", "indianapolis", "inverness", "iris", "iroquois", "irving", "isaacs", "italy", "james", "jarvis", "jeffreys", "jesus", "jones", "josephus", "judas", "julius", "kansas", "keynes", "kipling", "kiwanis", "lansing", "laos", "leeds", "levis", "leviticus", "lewis", "louis", "maccabees", "madras", "maimonides", "maldive", "massachusetts", "matthews", "mauritius", "memphis", "mercedes", "midas", "mingus", "minneapolis", "mohammed", "moines", "morris", "moses", "myers", "myknos", "nablus", "nanjing", "nantes", "naples", "neal", "netherlands", "nevis", "nostradamus", "oedipus", "olympus", "orleans", "orly", "papas", "paris", "parker", "pauling", "peking", "pershing", "peter", "peters", "philippines", "phineas", "pisces", "pryor", "pythagoras", "queens", "rabelais", "ramses", "reynolds", "rhesus", "rhodes", "richards", "robins", "rodgers", "rogers", "rubens", "sagittarius", "seychelles", "socrates", "texas", "thames", "thomas", "tiberias", "tunis", "venus", "vilnius", "wales", "warner", "wilkins", "williams", "wyoming", "xmas", "yonkers", "zeus", "frances", "aarhus", "adonis", "andrews", "angus", "antares", "aquinas", "arcturus", "ares", "artemis", "augustus", "ayers", "barnabas", "barnes", "becker", "bejing", "biggs", "billings", "boeing", "boris", "borroughs", "briggs", "buenos", "calais", "caracas", "cassius", "cerberus", "ceres", "cervantes", "chantilly", "chartres", "chester", "connally", "conner", "coors", "cummings", "curtis", "daedalus", "dionysus", "dobbs", "dolores", "edmonds" };

	static class DictEntry {
		boolean exception;

		String root;

		DictEntry(String root, boolean isException) {
			this.root = root;
			this.exception = isException;
		}
	}

	private static final CharArrayMap<KStemmer.DictEntry> dict_ht = KStemmer.initializeDictHash();

	private final OpenStringBuilder word = new OpenStringBuilder();

	private int j;

	private int k;

	private char finalChar() {
		return word.charAt(k);
	}

	private char penultChar() {
		return word.charAt(((k) - 1));
	}

	private boolean isVowel(int index) {
		return !(isCons(index));
	}

	private boolean isCons(int index) {
		char ch;
		ch = word.charAt(index);
		if (((((ch == 'a') || (ch == 'e')) || (ch == 'i')) || (ch == 'o')) || (ch == 'u'))
			return false;

		if ((ch != 'y') || (index == 0))
			return true;
		else
			return !(isCons((index - 1)));

	}

	private static CharArrayMap<KStemmer.DictEntry> initializeDictHash() {
		KStemmer.DictEntry defaultEntry;
		KStemmer.DictEntry entry;
		CharArrayMap<KStemmer.DictEntry> d = new CharArrayMap<>(1000, false);
		for (int i = 0; i < (KStemmer.exceptionWords.length); i++) {
			if (!(d.containsKey(KStemmer.exceptionWords[i]))) {
				entry = new KStemmer.DictEntry(KStemmer.exceptionWords[i], true);
				d.put(KStemmer.exceptionWords[i], entry);
			}else {
				throw new RuntimeException((("Warning: Entry [" + (KStemmer.exceptionWords[i])) + "] already in dictionary 1"));
			}
		}
		for (int i = 0; i < (KStemmer.directConflations.length); i++) {
			if (!(d.containsKey(KStemmer.directConflations[i][0]))) {
				entry = new KStemmer.DictEntry(KStemmer.directConflations[i][1], false);
				d.put(KStemmer.directConflations[i][0], entry);
			}else {
				throw new RuntimeException((("Warning: Entry [" + (KStemmer.directConflations[i][0])) + "] already in dictionary 2"));
			}
		}
		for (int i = 0; i < (KStemmer.countryNationality.length); i++) {
			if (!(d.containsKey(KStemmer.countryNationality[i][0]))) {
				entry = new KStemmer.DictEntry(KStemmer.countryNationality[i][1], false);
				d.put(KStemmer.countryNationality[i][0], entry);
			}else {
				throw new RuntimeException((("Warning: Entry [" + (KStemmer.countryNationality[i][0])) + "] already in dictionary 3"));
			}
		}
		defaultEntry = new KStemmer.DictEntry(null, false);
		String[] array;
		array = null;
		for (int i = 0; i < (array.length); i++) {
			if (!(d.containsKey(array[i]))) {
				d.put(array[i], defaultEntry);
			}else {
				throw new RuntimeException((("Warning: Entry [" + (array[i])) + "] already in dictionary 4"));
			}
		}
		for (int i = 0; i < (array.length); i++) {
			if (!(d.containsKey(array[i]))) {
				d.put(array[i], defaultEntry);
			}else {
				throw new RuntimeException((("Warning: Entry [" + (array[i])) + "] already in dictionary 4"));
			}
		}
		for (int i = 0; i < (array.length); i++) {
			if (!(d.containsKey(array[i]))) {
				d.put(array[i], defaultEntry);
			}else {
				throw new RuntimeException((("Warning: Entry [" + (array[i])) + "] already in dictionary 4"));
			}
		}
		for (int i = 0; i < (array.length); i++) {
			if (!(d.containsKey(array[i]))) {
				d.put(array[i], defaultEntry);
			}else {
				throw new RuntimeException((("Warning: Entry [" + (array[i])) + "] already in dictionary 4"));
			}
		}
		for (int i = 0; i < (array.length); i++) {
			if (!(d.containsKey(array[i]))) {
				d.put(array[i], defaultEntry);
			}else {
				throw new RuntimeException((("Warning: Entry [" + (array[i])) + "] already in dictionary 4"));
			}
		}
		for (int i = 0; i < (array.length); i++) {
			if (!(d.containsKey(array[i]))) {
				d.put(array[i], defaultEntry);
			}else {
				throw new RuntimeException((("Warning: Entry [" + (array[i])) + "] already in dictionary 4"));
			}
		}
		for (int i = 0; i < (array.length); i++) {
			if (!(d.containsKey(array[i]))) {
				d.put(array[i], defaultEntry);
			}else {
				throw new RuntimeException((("Warning: Entry [" + (array[i])) + "] already in dictionary 4"));
			}
		}
		for (int i = 0; i < (KStemmer.supplementDict.length); i++) {
			if (!(d.containsKey(KStemmer.supplementDict[i]))) {
				d.put(KStemmer.supplementDict[i], defaultEntry);
			}else {
				throw new RuntimeException((("Warning: Entry [" + (KStemmer.supplementDict[i])) + "] already in dictionary 5"));
			}
		}
		for (int i = 0; i < (KStemmer.properNouns.length); i++) {
			if (!(d.containsKey(KStemmer.properNouns[i]))) {
				d.put(KStemmer.properNouns[i], defaultEntry);
			}else {
				throw new RuntimeException((("Warning: Entry [" + (KStemmer.properNouns[i])) + "] already in dictionary 6"));
			}
		}
		return d;
	}

	private boolean isAlpha(char ch) {
		return (ch >= 'a') && (ch <= 'z');
	}

	private int stemLength() {
		return (j) + 1;
	}

	private boolean endsIn(char[] s) {
		if ((s.length) > (k))
			return false;

		int r = (word.length()) - (s.length);
		j = k;
		for (int r1 = r, i = 0; i < (s.length); i++ , r1++) {
			if ((s[i]) != (word.charAt(r1)))
				return false;

		}
		j = r - 1;
		return true;
	}

	private boolean endsIn(char a, char b) {
		if (2 > (k))
			return false;

		if (((word.charAt(((k) - 1))) == a) && ((word.charAt(k)) == b)) {
			j = (k) - 2;
			return true;
		}
		return false;
	}

	private boolean endsIn(char a, char b, char c) {
		if (3 > (k))
			return false;

		if ((((word.charAt(((k) - 2))) == a) && ((word.charAt(((k) - 1))) == b)) && ((word.charAt(k)) == c)) {
			j = (k) - 3;
			return true;
		}
		return false;
	}

	private boolean endsIn(char a, char b, char c, char d) {
		if (4 > (k))
			return false;

		if (((((word.charAt(((k) - 3))) == a) && ((word.charAt(((k) - 2))) == b)) && ((word.charAt(((k) - 1))) == c)) && ((word.charAt(k)) == d)) {
			j = (k) - 4;
			return true;
		}
		return false;
	}

	private KStemmer.DictEntry wordInDict() {
		if ((matchedEntry) != null)
			return matchedEntry;

		KStemmer.DictEntry e = KStemmer.dict_ht.get(word.getArray(), 0, word.length());
		if ((e != null) && (!(e.exception))) {
			matchedEntry = e;
		}
		return e;
	}

	private void plural() {
		if ((word.charAt(k)) == 's') {
			if (endsIn('i', 'e', 's')) {
				word.setLength(((j) + 3));
				(k)--;
				if (lookup())
					return;

				(k)++;
				word.unsafeWrite('s');
				setSuffix("y");
				lookup();
			}else
				if (endsIn('e', 's')) {
					word.setLength(((j) + 2));
					(k)--;
					boolean tryE = ((j) > 0) && (!(((word.charAt(j)) == 's') && ((word.charAt(((j) - 1))) == 's')));
					if (tryE && (lookup()))
						return;

					word.setLength(((j) + 1));
					(k)--;
					if (lookup())
						return;

					word.unsafeWrite('e');
					(k)++;
					if (!tryE)
						lookup();

					return;
				}else {
					if ((((word.length()) > 3) && ((penultChar()) != 's')) && (!(endsIn('o', 'u', 's')))) {
						word.setLength(k);
						(k)--;
						lookup();
					}
				}

		}
	}

	private void setSuffix(String s) {
		setSuff(s, s.length());
	}

	private void setSuff(String s, int len) {
		word.setLength(((j) + 1));
		for (int l = 0; l < len; l++) {
			word.unsafeWrite(s.charAt(l));
		}
		k = (j) + len;
	}

	KStemmer.DictEntry matchedEntry = null;

	private boolean lookup() {
		matchedEntry = KStemmer.dict_ht.get(word.getArray(), 0, word.size());
		return (matchedEntry) != null;
	}

	private void pastTense() {
		if ((word.length()) <= 4)
			return;

		if (endsIn('i', 'e', 'd')) {
			word.setLength(((j) + 3));
			(k)--;
			if (lookup())
				return;

			(k)++;
			word.unsafeWrite('d');
			setSuffix("y");
			lookup();
			return;
		}
		if ((endsIn('e', 'd')) && (vowelInStem())) {
			word.setLength(((j) + 2));
			k = (j) + 1;
			KStemmer.DictEntry entry = wordInDict();
			if (entry != null)
				if (!(entry.exception))
					return;


			word.setLength(((j) + 1));
			k = j;
			if (lookup())
				return;

			if (doubleC(k)) {
				word.setLength(k);
				(k)--;
				if (lookup())
					return;

				word.unsafeWrite(word.charAt(k));
				(k)++;
				lookup();
				return;
			}
			if (((word.charAt(0)) == 'u') && ((word.charAt(1)) == 'n')) {
				word.unsafeWrite('e');
				word.unsafeWrite('d');
				k = (k) + 2;
				return;
			}
			word.setLength(((j) + 1));
			word.unsafeWrite('e');
			k = (j) + 1;
			return;
		}
	}

	private boolean doubleC(int i) {
		if (i < 1)
			return false;

		if ((word.charAt(i)) != (word.charAt((i - 1))))
			return false;

		return isCons(i);
	}

	private boolean vowelInStem() {
		for (int i = 0; i < (stemLength()); i++) {
			if (isVowel(i))
				return true;

		}
		return false;
	}

	private void aspect() {
		if ((word.length()) <= 5)
			return;

		if ((endsIn('i', 'n', 'g')) && (vowelInStem())) {
			word.setCharAt(((j) + 1), 'e');
			word.setLength(((j) + 2));
			k = (j) + 1;
			KStemmer.DictEntry entry = wordInDict();
			if (entry != null) {
				if (!(entry.exception))
					return;

			}
			word.setLength(k);
			(k)--;
			if (lookup())
				return;

			if (doubleC(k)) {
				(k)--;
				word.setLength(((k) + 1));
				if (lookup())
					return;

				word.unsafeWrite(word.charAt(k));
				(k)++;
				lookup();
				return;
			}
			if ((((j) > 0) && (isCons(j))) && (isCons(((j) - 1)))) {
				k = j;
				word.setLength(((k) + 1));
				return;
			}
			word.setLength(((j) + 1));
			word.unsafeWrite('e');
			k = (j) + 1;
			return;
		}
	}

	private void ityEndings() {
		int old_k = k;
		if (endsIn('i', 't', 'y')) {
			word.setLength(((j) + 1));
			k = j;
			if (lookup())
				return;

			word.unsafeWrite('e');
			k = (j) + 1;
			if (lookup())
				return;

			word.setCharAt(((j) + 1), 'i');
			word.append("ty");
			k = old_k;
			if ((((j) > 0) && ((word.charAt(((j) - 1))) == 'i')) && ((word.charAt(j)) == 'l')) {
				word.setLength(((j) - 1));
				word.append("le");
				k = j;
				lookup();
				return;
			}
			if ((((j) > 0) && ((word.charAt(((j) - 1))) == 'i')) && ((word.charAt(j)) == 'v')) {
				word.setLength(((j) + 1));
				word.unsafeWrite('e');
				k = (j) + 1;
				lookup();
				return;
			}
			if ((((j) > 0) && ((word.charAt(((j) - 1))) == 'a')) && ((word.charAt(j)) == 'l')) {
				word.setLength(((j) + 1));
				k = j;
				lookup();
				return;
			}
			if (lookup())
				return;

			word.setLength(((j) + 1));
			k = j;
			return;
		}
	}

	private void nceEndings() {
		int old_k = k;
		char word_char;
		if (endsIn('n', 'c', 'e')) {
			word_char = word.charAt(j);
			if (!((word_char == 'e') || (word_char == 'a')))
				return;

			word.setLength(j);
			word.unsafeWrite('e');
			k = j;
			if (lookup())
				return;

			word.setLength(j);
			k = (j) - 1;
			if (lookup())
				return;

			word.unsafeWrite(word_char);
			word.append("nce");
			k = old_k;
		}
		return;
	}

	private void nessEndings() {
		if (endsIn('n', 'e', 's', 's')) {
			word.setLength(((j) + 1));
			k = j;
			if ((word.charAt(j)) == 'i')
				word.setCharAt(j, 'y');

			lookup();
		}
		return;
	}

	private void ismEndings() {
		if (endsIn('i', 's', 'm')) {
			word.setLength(((j) + 1));
			k = j;
			lookup();
		}
		return;
	}

	private void mentEndings() {
		int old_k = k;
		if (endsIn('m', 'e', 'n', 't')) {
			word.setLength(((j) + 1));
			k = j;
			if (lookup())
				return;

			word.append("ment");
			k = old_k;
		}
		return;
	}

	private void izeEndings() {
		int old_k = k;
		if (endsIn('i', 'z', 'e')) {
			word.setLength(((j) + 1));
			k = j;
			if (lookup())
				return;

			word.unsafeWrite('i');
			if (doubleC(j)) {
				word.setLength(j);
				k = (j) - 1;
				if (lookup())
					return;

				word.unsafeWrite(word.charAt(((j) - 1)));
			}
			word.setLength(((j) + 1));
			word.unsafeWrite('e');
			k = (j) + 1;
			if (lookup())
				return;

			word.setLength(((j) + 1));
			word.append("ize");
			k = old_k;
		}
		return;
	}

	private void ncyEndings() {
		if (endsIn('n', 'c', 'y')) {
			if (!(((word.charAt(j)) == 'e') || ((word.charAt(j)) == 'a')))
				return;

			word.setCharAt(((j) + 2), 't');
			word.setLength(((j) + 3));
			k = (j) + 2;
			if (lookup())
				return;

			word.setCharAt(((j) + 2), 'c');
			word.unsafeWrite('e');
			k = (j) + 3;
			lookup();
		}
		return;
	}

	private void bleEndings() {
		int old_k = k;
		char word_char;
		if (endsIn('b', 'l', 'e')) {
			if (!(((word.charAt(j)) == 'a') || ((word.charAt(j)) == 'i')))
				return;

			word_char = word.charAt(j);
			word.setLength(j);
			k = (j) - 1;
			if (lookup())
				return;

			if (doubleC(k)) {
				word.setLength(k);
				(k)--;
				if (lookup())
					return;

				(k)++;
				word.unsafeWrite(word.charAt(((k) - 1)));
			}
			word.setLength(j);
			word.unsafeWrite('e');
			k = j;
			if (lookup())
				return;

			word.setLength(j);
			word.append("ate");
			k = (j) + 2;
			if (lookup())
				return;

			word.setLength(j);
			word.unsafeWrite(word_char);
			word.append("ble");
			k = old_k;
		}
		return;
	}

	private void icEndings() {
		if (endsIn('i', 'c')) {
			word.setLength(((j) + 3));
			word.append("al");
			k = (j) + 4;
			if (lookup())
				return;

			word.setCharAt(((j) + 1), 'y');
			word.setLength(((j) + 2));
			k = (j) + 1;
			if (lookup())
				return;

			word.setCharAt(((j) + 1), 'e');
			if (lookup())
				return;

			word.setLength(((j) + 1));
			k = j;
			if (lookup())
				return;

			word.append("ic");
			k = (j) + 2;
		}
		return;
	}

	private static char[] ization = "ization".toCharArray();

	private static char[] ition = "ition".toCharArray();

	private static char[] ation = "ation".toCharArray();

	private static char[] ication = "ication".toCharArray();

	private void ionEndings() {
		int old_k = k;
		if (!(endsIn('i', 'o', 'n'))) {
			return;
		}
		if (endsIn(KStemmer.ization)) {
			word.setLength(((j) + 3));
			word.unsafeWrite('e');
			k = (j) + 3;
			lookup();
			return;
		}
		if (endsIn(KStemmer.ition)) {
			word.setLength(((j) + 1));
			word.unsafeWrite('e');
			k = (j) + 1;
			if (lookup())
				return;

			word.setLength(((j) + 1));
			word.append("ition");
			k = old_k;
		}else
			if (endsIn(KStemmer.ation)) {
				word.setLength(((j) + 3));
				word.unsafeWrite('e');
				k = (j) + 3;
				if (lookup())
					return;

				word.setLength(((j) + 1));
				word.unsafeWrite('e');
				k = (j) + 1;
				if (lookup())
					return;

				word.setLength(((j) + 1));
				k = j;
				if (lookup())
					return;

				word.setLength(((j) + 1));
				word.append("ation");
				k = old_k;
			}

		if (endsIn(KStemmer.ication)) {
			word.setLength(((j) + 1));
			word.unsafeWrite('y');
			k = (j) + 1;
			if (lookup())
				return;

			word.setLength(((j) + 1));
			word.append("ication");
			k = old_k;
		}
		if (true) {
			j = (k) - 3;
			word.setLength(((j) + 1));
			word.unsafeWrite('e');
			k = (j) + 1;
			if (lookup())
				return;

			word.setLength(((j) + 1));
			k = j;
			if (lookup())
				return;

			word.setLength(((j) + 1));
			word.append("ion");
			k = old_k;
		}
		return;
	}

	private void erAndOrEndings() {
		int old_k = k;
		if ((word.charAt(k)) != 'r')
			return;

		char word_char;
		if (endsIn('i', 'z', 'e', 'r')) {
			word.setLength(((j) + 4));
			k = (j) + 3;
			lookup();
			return;
		}
		if ((endsIn('e', 'r')) || (endsIn('o', 'r'))) {
			word_char = word.charAt(((j) + 1));
			if (doubleC(j)) {
				word.setLength(j);
				k = (j) - 1;
				if (lookup())
					return;

				word.unsafeWrite(word.charAt(((j) - 1)));
			}
			if ((word.charAt(j)) == 'i') {
				word.setCharAt(j, 'y');
				word.setLength(((j) + 1));
				k = j;
				if (lookup())
					return;

				word.setCharAt(j, 'i');
				word.unsafeWrite('e');
			}
			if ((word.charAt(j)) == 'e') {
				word.setLength(j);
				k = (j) - 1;
				if (lookup())
					return;

				word.unsafeWrite('e');
			}
			word.setLength(((j) + 2));
			k = (j) + 1;
			if (lookup())
				return;

			word.setLength(((j) + 1));
			k = j;
			if (lookup())
				return;

			word.unsafeWrite('e');
			k = (j) + 1;
			if (lookup())
				return;

			word.setLength(((j) + 1));
			word.unsafeWrite(word_char);
			word.unsafeWrite('r');
			k = old_k;
		}
	}

	private void lyEndings() {
		int old_k = k;
		if (endsIn('l', 'y')) {
			word.setCharAt(((j) + 2), 'e');
			if (lookup())
				return;

			word.setCharAt(((j) + 2), 'y');
			word.setLength(((j) + 1));
			k = j;
			if (lookup())
				return;

			if ((((j) > 0) && ((word.charAt(((j) - 1))) == 'a')) && ((word.charAt(j)) == 'l'))
				return;

			word.append("ly");
			k = old_k;
			if ((((j) > 0) && ((word.charAt(((j) - 1))) == 'a')) && ((word.charAt(j)) == 'b')) {
				word.setCharAt(((j) + 2), 'e');
				k = (j) + 2;
				return;
			}
			if ((word.charAt(j)) == 'i') {
				word.setLength(j);
				word.unsafeWrite('y');
				k = j;
				if (lookup())
					return;

				word.setLength(j);
				word.append("ily");
				k = old_k;
			}
			word.setLength(((j) + 1));
			k = j;
		}
		return;
	}

	private void alEndings() {
		int old_k = k;
		if ((word.length()) < 4)
			return;

		if (endsIn('a', 'l')) {
			word.setLength(((j) + 1));
			k = j;
			if (lookup())
				return;

			if (doubleC(j)) {
				word.setLength(j);
				k = (j) - 1;
				if (lookup())
					return;

				word.unsafeWrite(word.charAt(((j) - 1)));
			}
			word.setLength(((j) + 1));
			word.unsafeWrite('e');
			k = (j) + 1;
			if (lookup())
				return;

			word.setLength(((j) + 1));
			word.append("um");
			k = (j) + 2;
			if (lookup())
				return;

			word.setLength(((j) + 1));
			word.append("al");
			k = old_k;
			if ((((j) > 0) && ((word.charAt(((j) - 1))) == 'i')) && ((word.charAt(j)) == 'c')) {
				word.setLength(((j) - 1));
				k = (j) - 2;
				if (lookup())
					return;

				word.setLength(((j) - 1));
				word.unsafeWrite('y');
				k = (j) - 1;
				if (lookup())
					return;

				word.setLength(((j) - 1));
				word.append("ic");
				k = j;
				lookup();
				return;
			}
			if ((word.charAt(j)) == 'i') {
				word.setLength(j);
				k = (j) - 1;
				if (lookup())
					return;

				word.append("ial");
				k = old_k;
				lookup();
			}
		}
		return;
	}

	private void iveEndings() {
		int old_k = k;
		if (endsIn('i', 'v', 'e')) {
			word.setLength(((j) + 1));
			k = j;
			if (lookup())
				return;

			word.unsafeWrite('e');
			k = (j) + 1;
			if (lookup())
				return;

			word.setLength(((j) + 1));
			word.append("ive");
			if ((((j) > 0) && ((word.charAt(((j) - 1))) == 'a')) && ((word.charAt(j)) == 't')) {
				word.setCharAt(((j) - 1), 'e');
				word.setLength(j);
				k = (j) - 1;
				if (lookup())
					return;

				word.setLength(((j) - 1));
				if (lookup())
					return;

				word.append("ative");
				k = old_k;
			}
			word.setCharAt(((j) + 2), 'o');
			word.setCharAt(((j) + 3), 'n');
			if (lookup())
				return;

			word.setCharAt(((j) + 2), 'v');
			word.setCharAt(((j) + 3), 'e');
			k = old_k;
		}
		return;
	}

	KStemmer() {
	}

	String stem(String term) {
		boolean changed = stem(term.toCharArray(), term.length());
		if (!changed)
			return term;

		return asString();
	}

	String asString() {
		String s = getString();
		if (s != null)
			return s;

		return word.toString();
	}

	CharSequence asCharSequence() {
		return (result) != null ? result : word;
	}

	String getString() {
		return result;
	}

	char[] getChars() {
		return word.getArray();
	}

	int getLength() {
		return word.length();
	}

	String result;

	private boolean matched() {
		return (matchedEntry) != null;
	}

	boolean stem(char[] term, int len) {
		result = null;
		k = len - 1;
		if (((k) <= 1) || ((k) >= ((KStemmer.MaxWordLen) - 1))) {
			return false;
		}
		KStemmer.DictEntry entry = KStemmer.dict_ht.get(term, 0, len);
		if (entry != null) {
			if ((entry.root) != null) {
				result = entry.root;
				return true;
			}
			return false;
		}
		word.reset();
		word.reserve((len + 10));
		for (int i = 0; i < len; i++) {
			char ch = term[i];
			if (!(isAlpha(ch)))
				return false;

			word.unsafeWrite(ch);
		}
		matchedEntry = null;
		while (true) {
			plural();
			if (matched())
				break;

			pastTense();
			if (matched())
				break;

			aspect();
			if (matched())
				break;

			ityEndings();
			if (matched())
				break;

			nessEndings();
			if (matched())
				break;

			ionEndings();
			if (matched())
				break;

			erAndOrEndings();
			if (matched())
				break;

			lyEndings();
			if (matched())
				break;

			alEndings();
			if (matched())
				break;

			entry = wordInDict();
			iveEndings();
			if (matched())
				break;

			izeEndings();
			if (matched())
				break;

			mentEndings();
			if (matched())
				break;

			bleEndings();
			if (matched())
				break;

			ismEndings();
			if (matched())
				break;

			icEndings();
			if (matched())
				break;

			ncyEndings();
			if (matched())
				break;

			nceEndings();
			matched();
			break;
		} 
		entry = matchedEntry;
		if (entry != null) {
			result = entry.root;
		}
		return true;
	}
}

