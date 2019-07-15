import org.apache.poi.hwpf.model.*;


import org.apache.poi.util.Internal;

import org.apache.poi.hwpf.model.types.LFOAbstractType;

/**
 * "The LFO structure specifies the LSTF element that corresponds to a list that
 * contains a paragraph. An LFO can also specify formatting information that
 * overrides the LSTF element to which it corresponds." -- [MS-DOC] -- v20110315
 * Word (.doc) Binary File Format
 * 
 * @author Sergey Vladimirov (vlsergey {at} gmail {dot} com)
 */
@Internal
public class LFO extends LFOAbstractType
{
    public LFO()
    {
    }

    public LFO( byte[] std, int offset )
    {
        fillFields( std, offset );
    }
}
