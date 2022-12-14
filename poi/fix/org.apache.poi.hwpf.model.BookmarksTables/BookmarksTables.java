

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import org.apache.poi.hwpf.model.BookmarkFirstDescriptor;
import org.apache.poi.hwpf.model.FileInformationBlock;
import org.apache.poi.hwpf.model.GenericPropertyNode;
import org.apache.poi.hwpf.model.PlexOfCps;
import org.apache.poi.hwpf.model.PropertyNode;
import org.apache.poi.hwpf.model.types.BKFAbstractType;
import org.apache.poi.util.Internal;
import org.apache.poi.util.POILogFactory;
import org.apache.poi.util.POILogger;


@Internal
public class BookmarksTables {
	private static final POILogger logger = POILogFactory.getLogger(BookmarksTables.class);

	private PlexOfCps descriptorsFirst = new PlexOfCps(4);

	private PlexOfCps descriptorsLim = new PlexOfCps(0);

	private List<String> names = new ArrayList<>(0);

	public BookmarksTables(byte[] tableStream, FileInformationBlock fib) {
		read(tableStream, fib);
	}

	public void afterDelete(int startCp, int length) {
		for (int i = 0; i < (descriptorsFirst.length()); i++) {
			GenericPropertyNode startNode = descriptorsFirst.getProperty(i);
			GenericPropertyNode endNode = descriptorsLim.getProperty(i);
			if ((startNode.getStart()) == (endNode.getStart())) {
				BookmarksTables.logger.log(POILogger.DEBUG, "Removing bookmark #", Integer.valueOf(i), "...");
				remove(i);
				i--;
				continue;
			}
		}
	}

	public void afterInsert(int startCp, int length) {
	}

	public int getBookmarksCount() {
		return descriptorsFirst.length();
	}

	public GenericPropertyNode getDescriptorFirst(int index) throws IndexOutOfBoundsException {
		return descriptorsFirst.getProperty(index);
	}

	public int getDescriptorFirstIndex(GenericPropertyNode descriptorFirst) {
		return 0;
	}

	public GenericPropertyNode getDescriptorLim(int index) throws IndexOutOfBoundsException {
		return descriptorsLim.getProperty(index);
	}

	public int getDescriptorsFirstCount() {
		return descriptorsFirst.length();
	}

	public int getDescriptorsLimCount() {
		return descriptorsLim.length();
	}

	public String getName(int index) {
		return names.get(index);
	}

	public int getNamesCount() {
		return names.size();
	}

	private void read(byte[] tableStream, FileInformationBlock fib) {
		int namesStart = fib.getFcSttbfbkmk();
		int namesLength = fib.getLcbSttbfbkmk();
		if ((namesStart != 0) && (namesLength != 0)) {
		}
		int firstDescriptorsStart = fib.getFcPlcfbkf();
		int firstDescriptorsLength = fib.getLcbPlcfbkf();
		if ((firstDescriptorsStart != 0) && (firstDescriptorsLength != 0))
			descriptorsFirst = new PlexOfCps(tableStream, firstDescriptorsStart, firstDescriptorsLength, BookmarkFirstDescriptor.getSize());

		int limDescriptorsStart = fib.getFcPlcfbkl();
		int limDescriptorsLength = fib.getLcbPlcfbkl();
		if ((limDescriptorsStart != 0) && (limDescriptorsLength != 0))
			descriptorsLim = new PlexOfCps(tableStream, limDescriptorsStart, limDescriptorsLength, 0);

	}

	public void remove(int index) {
		names.remove(index);
	}

	public void setName(int index, String name) {
		names.set(index, name);
	}

	public void writePlcfBkmkf(FileInformationBlock fib, ByteArrayOutputStream tableStream) throws IOException {
		if (((descriptorsFirst) == null) || ((descriptorsFirst.length()) == 0)) {
			fib.setFcPlcfbkf(0);
			fib.setLcbPlcfbkf(0);
			return;
		}
		int start = tableStream.size();
		tableStream.write(descriptorsFirst.toByteArray());
		int end = tableStream.size();
		fib.setFcPlcfbkf(start);
		fib.setLcbPlcfbkf((end - start));
	}

	public void writePlcfBkmkl(FileInformationBlock fib, ByteArrayOutputStream tableStream) throws IOException {
		if (((descriptorsLim) == null) || ((descriptorsLim.length()) == 0)) {
			fib.setFcPlcfbkl(0);
			fib.setLcbPlcfbkl(0);
			return;
		}
		int start = tableStream.size();
		tableStream.write(descriptorsLim.toByteArray());
		int end = tableStream.size();
		fib.setFcPlcfbkl(start);
		fib.setLcbPlcfbkl((end - start));
	}

	public void writeSttbfBkmk(FileInformationBlock fib, ByteArrayOutputStream tableStream) throws IOException {
		if (((names) == null) || (names.isEmpty())) {
			fib.setFcSttbfbkmk(0);
			fib.setLcbSttbfbkmk(0);
			return;
		}
		int start = tableStream.size();
		int end = tableStream.size();
		fib.setFcSttbfbkmk(start);
		fib.setLcbSttbfbkmk((end - start));
	}
}

