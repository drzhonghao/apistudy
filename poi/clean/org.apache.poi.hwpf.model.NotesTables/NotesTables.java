import org.apache.poi.hwpf.model.FootnoteReferenceDescriptor;
import org.apache.poi.hwpf.model.GenericPropertyNode;
import org.apache.poi.hwpf.model.FileInformationBlock;
import org.apache.poi.hwpf.model.*;


import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.poi.util.Internal;

/**
 * Holds information about document notes (footnotes or ending notes)
 * 
 * @author Sergey Vladimirov (vlsergey {at} gmail {doc} com)
 */
@Internal
public class NotesTables
{
    private PlexOfCps descriptors = new PlexOfCps(
            FootnoteReferenceDescriptor.getSize() );

    private final NoteType noteType;

    private PlexOfCps textPositions = new PlexOfCps( 0 );

    public NotesTables( final NoteType noteType )
    {
        this.noteType = noteType;
        textPositions
                .addProperty( new GenericPropertyNode( 0, 1, new byte[0] ) );
    }

    public NotesTables( final NoteType noteType, byte[] tableStream,
            FileInformationBlock fib )
    {
        this.noteType = noteType;
        read( tableStream, fib );
    }

    public GenericPropertyNode getDescriptor( int index )
    {
        return descriptors.getProperty( index );
    }

    public int getDescriptorsCount()
    {
        return descriptors.length();
    }

    public GenericPropertyNode getTextPosition( int index )
    {
        return textPositions.getProperty( index );
    }

    private void read( byte[] tableStream, FileInformationBlock fib )
    {
        int referencesStart = fib.getNotesDescriptorsOffset( noteType );
        int referencesLength = fib.getNotesDescriptorsSize( noteType );

        if ( referencesStart != 0 && referencesLength != 0 )
            this.descriptors = new PlexOfCps( tableStream, referencesStart,
                    referencesLength, FootnoteReferenceDescriptor.getSize() );

        int textPositionsStart = fib.getNotesTextPositionsOffset( noteType );
        int textPositionsLength = fib.getNotesTextPositionsSize( noteType );

        if ( textPositionsStart != 0 && textPositionsLength != 0 )
            this.textPositions = new PlexOfCps( tableStream,
                    textPositionsStart, textPositionsLength, 0 );
    }

    public void writeRef( FileInformationBlock fib, ByteArrayOutputStream tableStream )
            throws IOException
    {
        if ( descriptors == null || descriptors.length() == 0 )
        {
            fib.setNotesDescriptorsOffset( noteType, tableStream.size() );
            fib.setNotesDescriptorsSize( noteType, 0 );
            return;
        }

        int start = tableStream.size();
        tableStream.write( descriptors.toByteArray() );
        int end = tableStream.size();

        fib.setNotesDescriptorsOffset( noteType, start );
        fib.setNotesDescriptorsSize( noteType, end - start );
    }

    public void writeTxt( FileInformationBlock fib, ByteArrayOutputStream tableStream )
            throws IOException
    {
        if ( textPositions == null || textPositions.length() == 0 )
        {
            fib.setNotesTextPositionsOffset( noteType, tableStream.size() );
            fib.setNotesTextPositionsSize( noteType, 0 );
            return;
        }

        int start = tableStream.size();
        tableStream.write( textPositions.toByteArray() );
        int end = tableStream.size();

        fib.setNotesTextPositionsOffset( noteType, start );
        fib.setNotesTextPositionsSize( noteType, end - start );
    }
}
