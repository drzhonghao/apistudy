import org.apache.poi.hwpf.usermodel.*;


import java.util.HashMap;
import java.util.Map;

import org.apache.poi.hwpf.model.NotesTables;

/**
 * Default implementation of {@link Notes} interface
 * 
 * @author Sergey Vladimirov (vlsergey {at} gmail {doc} com)
 */
public class NotesImpl implements Notes
{
    private Map<Integer, Integer> anchorToIndexMap;

    private final NotesTables notesTables;

    public NotesImpl( NotesTables notesTables )
    {
        this.notesTables = notesTables;
    }

    public int getNoteAnchorPosition( int index )
    {
        return notesTables.getDescriptor( index ).getStart();
    }

    public int getNoteIndexByAnchorPosition( int anchorPosition )
    {
        updateAnchorToIndexMap();

        Integer index = anchorToIndexMap
                .get( Integer.valueOf( anchorPosition ) );
        if ( index == null )
            return -1;

        return index.intValue();
    }

    public int getNotesCount()
    {
        return notesTables.getDescriptorsCount();
    }

    public int getNoteTextEndOffset( int index )
    {
        return notesTables.getTextPosition( index ).getEnd();
    }

    public int getNoteTextStartOffset( int index )
    {
        return notesTables.getTextPosition( index ).getStart();
    }

    private void updateAnchorToIndexMap()
    {
        if ( anchorToIndexMap != null )
            return;

        Map<Integer, Integer> result = new HashMap<>();
        for ( int n = 0; n < notesTables.getDescriptorsCount(); n++ )
        {
            int anchorPosition = notesTables.getDescriptor( n ).getStart();
            result.put( Integer.valueOf( anchorPosition ), Integer.valueOf( n ) );
        }
        this.anchorToIndexMap = result;
    }
}
