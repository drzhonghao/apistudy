

import java.io.IOException;
import java.io.PrintStream;


public class RecordLister {
	String file;

	public RecordLister() {
	}

	public void run() throws IOException {
	}

	private static String formatSID(int sid) {
		String hex = Integer.toHexString(sid);
		String dec = Integer.toString(sid);
		StringBuilder s = new StringBuilder();
		s.append("0x");
		for (int i = hex.length(); i < 4; i++) {
			s.append('0');
		}
		s.append(hex);
		s.append(" (");
		for (int i = dec.length(); i < 4; i++) {
			s.append('0');
		}
		s.append(dec);
		s.append(")");
		return s.toString();
	}

	private static String formatSize(int size) {
		String hex = Integer.toHexString(size);
		String dec = Integer.toString(size);
		StringBuilder s = new StringBuilder();
		for (int i = hex.length(); i < 3; i++) {
			s.append('0');
		}
		s.append(hex);
		s.append(" (");
		for (int i = dec.length(); i < 3; i++) {
			s.append('0');
		}
		s.append(dec);
		s.append(")");
		return s.toString();
	}

	private static String formatData(byte[] data) {
		if ((data == null) || ((data.length) == 0))
			return "";

		StringBuilder s = new StringBuilder();
		if ((data.length) > 9) {
			s.append(RecordLister.byteToHex(data[0]));
			s.append(' ');
			s.append(RecordLister.byteToHex(data[1]));
			s.append(' ');
			s.append(RecordLister.byteToHex(data[2]));
			s.append(' ');
			s.append(RecordLister.byteToHex(data[3]));
			s.append(' ');
			s.append(" .... ");
			s.append(' ');
			s.append(RecordLister.byteToHex(data[((data.length) - 4)]));
			s.append(' ');
			s.append(RecordLister.byteToHex(data[((data.length) - 3)]));
			s.append(' ');
			s.append(RecordLister.byteToHex(data[((data.length) - 2)]));
			s.append(' ');
			s.append(RecordLister.byteToHex(data[((data.length) - 1)]));
		}else {
			for (byte aData : data) {
				s.append(RecordLister.byteToHex(aData));
				s.append(' ');
			}
		}
		return s.toString();
	}

	private static String byteToHex(byte b) {
		int i = b;
		if (i < 0) {
			i += 256;
		}
		String s = Integer.toHexString(i);
		if (i < 16) {
			return "0" + s;
		}
		return s;
	}

	public void setFile(String file) {
		this.file = file;
	}

	public static void main(String[] args) throws IOException {
		if (((args.length) == 1) && (!(args[0].equals("--help")))) {
			RecordLister viewer = new RecordLister();
			viewer.setFile(args[0]);
			viewer.run();
		}else {
			System.out.println("RecordLister");
			System.out.println("Outputs the summary of the records in file order");
			System.out.println(("usage: java org.apache.poi.hssf.dev.RecordLister " + "filename"));
		}
	}
}

