import org.apache.poi.hslf.blip.*;


import org.apache.poi.util.PngUtils;

/**
 * Represents a PNG picture data in a PPT file
 */
public final class PNG extends Bitmap {

    @Override
    public byte[] getData() {
        byte[] data = super.getData();

        //PNG created on MAC may have a 16-byte prefix which prevents successful reading.
        //Just cut it off!.
        if (PngUtils.matchesPngHeader(data, 16)) {
            byte[] png = new byte[data.length-16];
            System.arraycopy(data, 16, png, 0, png.length);
            data = png;
        }

        return data;
    }

    @Override
    public PictureType getType(){
        return PictureType.PNG;
    }

    /**
     * PNG signature is {@code 0x6E00} or {@code 0x6E10}
     *
     * @return PNG signature ({@code 0x6E00} or {@code 0x6E10})
     */
    public int getSignature(){
        return (getUIDInstanceCount() == 1 ? 0x6E00 : 0x6E10);
    }
    
    /**
     * Sets the PNG signature - either {@code 0x6E00} or {@code 0x6E10}
     */
    public void setSignature(int signature) {
        switch (signature) {
            case 0x6E00:
                setUIDInstanceCount(1);
                break;
            case 0x6E10:
                setUIDInstanceCount(2);
                break;
            default:
                throw new IllegalArgumentException(signature+" is not a valid instance/signature value for PNG");
        }        
    }
}
