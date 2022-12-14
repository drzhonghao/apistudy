import org.apache.poi.ddf.EscherBSERecord;
import org.apache.poi.ddf.EscherOptRecord;
import org.apache.poi.ddf.EscherTertiaryOptRecord;
import org.apache.poi.ddf.EscherClientAnchorRecord;
import org.apache.poi.ddf.EscherDgRecord;
import org.apache.poi.ddf.EscherSpgrRecord;
import org.apache.poi.ddf.EscherSpRecord;
import org.apache.poi.ddf.EscherClientDataRecord;
import org.apache.poi.ddf.EscherDggRecord;
import org.apache.poi.ddf.EscherSplitMenuColorsRecord;
import org.apache.poi.ddf.EscherChildAnchorRecord;
import org.apache.poi.ddf.EscherRecord;
import org.apache.poi.ddf.EscherContainerRecord;
import org.apache.poi.ddf.EscherBlipRecord;
import org.apache.poi.ddf.EscherBitmapBlip;
import org.apache.poi.ddf.EscherMetafileBlip;
import org.apache.poi.ddf.*;


import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

import org.apache.poi.util.LittleEndian;

/**
 * Generates escher records when provided the byte array containing those records.
 *
 * @see EscherRecordFactory
 */
public class DefaultEscherRecordFactory implements EscherRecordFactory {
    private static Class<?>[] escherRecordClasses = { EscherBSERecord.class,
            EscherOptRecord.class, EscherTertiaryOptRecord.class,
            EscherClientAnchorRecord.class, EscherDgRecord.class,
            EscherSpgrRecord.class, EscherSpRecord.class,
            EscherClientDataRecord.class, EscherDggRecord.class,
            EscherSplitMenuColorsRecord.class, EscherChildAnchorRecord.class,
            EscherTextboxRecord.class };
    private static Map<Short, Constructor<? extends EscherRecord>> recordsMap = recordsToMap( escherRecordClasses );

    /**
     * Creates an instance of the escher record factory
     */
    public DefaultEscherRecordFactory() {
        // no instance initialisation
    }

    @Override
    public EscherRecord createRecord(byte[] data, int offset) {
        short options = LittleEndian.getShort( data, offset );
        short recordId = LittleEndian.getShort( data, offset + 2 );
        // int remainingBytes = LittleEndian.getInt( data, offset + 4 );

        // Options of 0x000F means container record
        // However, EscherTextboxRecord are containers of records for the
        //  host application, not of other Escher records, so treat them
        //  differently
        if (isContainer(options, recordId)) {
            EscherContainerRecord r = new EscherContainerRecord();
            r.setRecordId( recordId );
            r.setOptions( options );
            return r;
        }

        if (recordId >= EscherBlipRecord.RECORD_ID_START
                && recordId <= EscherBlipRecord.RECORD_ID_END) {
            EscherBlipRecord r;
            if (recordId == EscherBitmapBlip.RECORD_ID_DIB ||
                    recordId == EscherBitmapBlip.RECORD_ID_JPEG ||
                    recordId == EscherBitmapBlip.RECORD_ID_PNG)
            {
                r = new EscherBitmapBlip();
            }
            else if (recordId == EscherMetafileBlip.RECORD_ID_EMF ||
                    recordId == EscherMetafileBlip.RECORD_ID_WMF ||
                    recordId == EscherMetafileBlip.RECORD_ID_PICT)
            {
                r = new EscherMetafileBlip();
            } else {
                r = new EscherBlipRecord();
            }
            r.setRecordId( recordId );
            r.setOptions( options );
            return r;
        }

        Constructor<? extends EscherRecord> recordConstructor = recordsMap.get(Short.valueOf(recordId));
        final EscherRecord escherRecord;
        if (recordConstructor == null) {
            return new UnknownEscherRecord();
        }
        try {
            escherRecord = recordConstructor.newInstance();
        } catch (Exception e) {
            return new UnknownEscherRecord();
        }
        escherRecord.setRecordId(recordId);
        escherRecord.setOptions(options);
        return escherRecord;
    }

    /**
     * Converts from a list of classes into a map that contains the record id as the key and
     * the Constructor in the value part of the map.  It does this by using reflection to look up
     * the RECORD_ID field then using reflection again to find a reference to the constructor.
     *
     * @param recClasses The records to convert
     * @return The map containing the id/constructor pairs.
     */
    protected static Map<Short, Constructor<? extends EscherRecord>> recordsToMap(Class<?>[] recClasses) {
        Map<Short, Constructor<? extends EscherRecord>> result = new HashMap<>();
        final Class<?>[] EMPTY_CLASS_ARRAY = new Class[0];

        for (Class<?> recClass : recClasses) {
            @SuppressWarnings("unchecked")
            Class<? extends EscherRecord> recCls = (Class<? extends EscherRecord>) recClass;
            short sid;
            try {
                sid = recCls.getField("RECORD_ID").getShort(null);
            } catch (IllegalArgumentException e) {
                throw new RuntimeException(e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            } catch (NoSuchFieldException e) {
                throw new RuntimeException(e);
            }
            Constructor<? extends EscherRecord> constructor;
            try {
                constructor = recCls.getConstructor(EMPTY_CLASS_ARRAY);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
            result.put(Short.valueOf(sid), constructor);
        }
        return result;
    }

    public static boolean isContainer(short options, short recordId){
        if(recordId >= EscherContainerRecord.DGG_CONTAINER &&  recordId
                <= EscherContainerRecord.SOLVER_CONTAINER){
            return true;
        } else {
            if (recordId == EscherTextboxRecord.RECORD_ID) {
                return false;
            } else {
                return ( options & (short) 0x000F ) == (short) 0x000F;
            }
        }
    }
}
