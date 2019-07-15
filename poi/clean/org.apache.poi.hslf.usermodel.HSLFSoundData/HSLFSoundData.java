import org.apache.poi.hslf.record.Sound;
import org.apache.poi.hslf.record.Record;
import org.apache.poi.hslf.record.SoundCollection;
import org.apache.poi.hslf.record.RecordContainer;
import org.apache.poi.hslf.usermodel.*;


import org.apache.poi.hslf.record.*;

import java.util.ArrayList;

/**
 * A class that represents sound data embedded in a slide show.
 *
 * @author Yegor Kozlov
 */
public final class HSLFSoundData {
    /**
     * The record that contains the object data.
     */
    private Sound _container;

    /**
     * Creates the object data wrapping the record that contains the sound data.
     *
     * @param container the record that contains the sound data.
     */
    public HSLFSoundData(Sound container) {
        this._container = container;
    }

    /**
     * Name of the sound (e.g. "crash")
     *
     * @return name of the sound
     */
    public String getSoundName(){
        return _container.getSoundName();
    }

    /**
     * Type of the sound (e.g. ".wav")
     *
     * @return type of the sound
     */
    public String getSoundType(){
        return _container.getSoundType();
    }

    /**
     * Gets an input stream which returns the binary of the sound data.
     *
     * @return the input stream which will contain the binary of the sound data.
     */
    public byte[] getData() {
        return _container.getSoundData();
    }

    /**
     * Find all sound records in the supplied Document records
     *
     * @param document the document to find in
     * @return the array with the sound data
     */
    public static HSLFSoundData[] find(Document document){
        ArrayList<HSLFSoundData> lst = new ArrayList<>();
        Record[] ch = document.getChildRecords();
        for (int i = 0; i < ch.length; i++) {
            if(ch[i].getRecordType() == RecordTypes.SoundCollection.typeID){
                RecordContainer col = (RecordContainer)ch[i];
                Record[] sr = col.getChildRecords();
                for (int j = 0; j < sr.length; j++) {
                    if(sr[j] instanceof Sound){
                        lst.add(new HSLFSoundData((Sound)sr[j]));
                    }
                }
            }

        }
        return lst.toArray(new HSLFSoundData[lst.size()]);
    }
}
