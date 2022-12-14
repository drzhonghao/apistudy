import org.apache.poi.ddf.EscherContainerRecord;
import org.apache.poi.ddf.EscherDggRecord;
import org.apache.poi.ddf.DefaultEscherRecordFactory;
import org.apache.poi.ddf.EscherRecord;
import org.apache.poi.hslf.record.*;


import org.apache.poi.ddf.*;
import org.apache.poi.util.IOUtils;
import org.apache.poi.util.LittleEndian;

import java.io.OutputStream;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.util.Iterator;

/**
 * Container records which always exists inside Document.
 * It always acts as a holder for escher DGG container
 *  which may contain which Escher BStore container information
 *  about pictures containes in the presentation (if any).
 *
 * @author Yegor Kozlov
 */
public final class PPDrawingGroup extends RecordAtom {

    //arbitrarily selected; may need to increase
    private static final int MAX_RECORD_LENGTH = 10_485_760;


    private byte[] _header;
    private EscherContainerRecord dggContainer;
    //cached dgg
    private EscherDggRecord dgg;

    protected PPDrawingGroup(byte[] source, int start, int len) {
        // Get the header
        _header = new byte[8];
        System.arraycopy(source,start,_header,0,8);

        // Get the contents for now
        byte[] contents = IOUtils.safelyAllocate(len, MAX_RECORD_LENGTH);
        System.arraycopy(source,start,contents,0,len);

        DefaultEscherRecordFactory erf = new HSLFEscherRecordFactory();
        EscherRecord child = erf.createRecord(contents, 0);
        child.fillFields( contents, 0, erf );
        dggContainer = (EscherContainerRecord)child.getChild(0);
    }

    /**
     * We are type 1035
     */
    public long getRecordType() {
        return RecordTypes.PPDrawingGroup.typeID;
    }

    /**
     * We're pretending to be an atom, so return null
     */
    public Record[] getChildRecords() {
        return null;
    }

    public void writeOut(OutputStream out) throws IOException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        for (EscherRecord r : dggContainer) {
            if (r.getRecordId() == EscherContainerRecord.BSTORE_CONTAINER){
                EscherContainerRecord bstore = (EscherContainerRecord)r;

                ByteArrayOutputStream b2 = new ByteArrayOutputStream();
                for (EscherRecord br : bstore) {
                    byte[] b = new byte[36+8];
                    br.serialize(0, b);
                    b2.write(b);
                }
                byte[] bstorehead = new byte[8];
                LittleEndian.putShort(bstorehead, 0, bstore.getOptions());
                LittleEndian.putShort(bstorehead, 2, bstore.getRecordId());
                LittleEndian.putInt(bstorehead, 4, b2.size());
                bout.write(bstorehead);
                bout.write(b2.toByteArray());

            } else {
                bout.write(r.serialize());
            }
        }
        int size = bout.size();

        // Update the size (header bytes 5-8)
        LittleEndian.putInt(_header,4,size+8);

        // Write out our header
        out.write(_header);

        byte[] dgghead = new byte[8];
        LittleEndian.putShort(dgghead, 0, dggContainer.getOptions());
        LittleEndian.putShort(dgghead, 2, dggContainer.getRecordId());
        LittleEndian.putInt(dgghead, 4, size);
        out.write(dgghead);

        // Finally, write out the children
        out.write(bout.toByteArray());

    }

    public EscherContainerRecord getDggContainer(){
        return dggContainer;
    }

    public EscherDggRecord getEscherDggRecord(){
        if(dgg == null){
            for(EscherRecord r : dggContainer){
                if(r instanceof EscherDggRecord){
                    dgg = (EscherDggRecord)r;
                    break;
                }
            }
        }
        return dgg;
    }
}
