

import java.io.IOException;
import java.io.PrintStream;


public class EFBiffViewer {
	String file;

	@SuppressWarnings("WeakerAccess")
	public EFBiffViewer() {
	}

	public void run() throws IOException {
	}

	public void setFile(String file) {
		this.file = file;
	}

	public static void main(String[] args) throws IOException {
		if (((args.length) == 1) && (!(args[0].equals("--help")))) {
			EFBiffViewer viewer = new EFBiffViewer();
			viewer.setFile(args[0]);
			viewer.run();
		}else {
			System.out.println("EFBiffViewer");
			System.out.println("Outputs biffview of records based on HSSFEventFactory");
			System.out.println(("usage: java org.apache.poi.hssf.dev.EBBiffViewer " + "filename"));
		}
	}
}

