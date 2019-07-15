import org.apache.poi.hslf.record.SheetContainer;
import org.apache.poi.hslf.record.PPDrawing;
import org.apache.poi.hslf.record.Record;
import org.apache.poi.hslf.record.*;


import java.io.IOException;
import java.io.OutputStream;

/**
 * Master container for Notes. There is one of these for every page of
 *  notes, and they have certain specific children
 *
 * @author Nick Burch
 */

public final class Notes extends SheetContainer
{
	private byte[] _header;
	private static long _type = 1008l;

	// Links to our more interesting children
	private NotesAtom notesAtom;
	private PPDrawing ppDrawing;
    private ColorSchemeAtom _colorScheme;

	/**
	 * Returns the NotesAtom of this Notes
	 */
	public NotesAtom getNotesAtom() { return notesAtom; }
	/**
	 * Returns the PPDrawing of this Notes, which has all the
	 *  interesting data in it
	 */
	public PPDrawing getPPDrawing() { return ppDrawing; }


	/**
	 * Set things up, and find our more interesting children
	 */
	protected Notes(byte[] source, int start, int len) {
		// Grab the header
		_header = new byte[8];
		System.arraycopy(source,start,_header,0,8);

		// Find our children
		_children = Record.findChildRecords(source,start+8,len-8);

		// Find the interesting ones in there
		for(int i=0; i<_children.length; i++) {
			if(_children[i] instanceof NotesAtom) {
				notesAtom = (NotesAtom)_children[i];
			}
			if(_children[i] instanceof PPDrawing) {
				ppDrawing = (PPDrawing)_children[i];
			}
            if(ppDrawing != null && _children[i] instanceof ColorSchemeAtom) {
                _colorScheme = (ColorSchemeAtom)_children[i];
            }
		}
	}


	/**
	 * We are of type 1008
	 */
	public long getRecordType() { return _type; }

	/**
	 * Write the contents of the record back, so it can be written
	 *  to disk
	 */
	public void writeOut(OutputStream out) throws IOException {
		writeOut(_header[0],_header[1],_type,_children,out);
	}

    public ColorSchemeAtom getColorScheme(){
        return _colorScheme;
    }
}
