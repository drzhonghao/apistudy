import org.apache.poi.hwpf.sprm.SprmOperation;
import org.apache.poi.hwpf.sprm.SprmIterator;
import org.apache.poi.hwpf.sprm.*;


import java.util.Arrays;

import org.apache.poi.util.IOUtils;
import org.apache.poi.util.Internal;
import org.apache.poi.util.LittleEndian;

@Internal
public final class SprmBuffer implements Cloneable {

    //arbitrarily selected; may need to increase
    private static final int MAX_RECORD_LENGTH = 100_000;

    byte[] _buf;
    boolean _istd;
    int _offset;

    private final int _sprmsStartOffset;

    public SprmBuffer(byte[] buf, boolean istd, int sprmsStartOffset) {
        _offset = buf.length;
        _buf = buf;
        _istd = istd;
        _sprmsStartOffset = sprmsStartOffset;
    }

    public SprmBuffer(byte[] buf, int _sprmsStartOffset) {
        this(buf, false, _sprmsStartOffset);
    }

    public SprmBuffer(int sprmsStartOffset) {
        _buf = IOUtils.safelyAllocate(sprmsStartOffset + 4, MAX_RECORD_LENGTH);
        _offset = sprmsStartOffset;
        _sprmsStartOffset = sprmsStartOffset;
    }

    public void addSprm(short opcode, byte operand) {
        int addition = LittleEndian.SHORT_SIZE + LittleEndian.BYTE_SIZE;
        ensureCapacity(addition);
        LittleEndian.putShort(_buf, _offset, opcode);
        _offset += LittleEndian.SHORT_SIZE;
        _buf[_offset++] = operand;
    }

    public void addSprm(short opcode, byte[] operand) {
        int addition = LittleEndian.SHORT_SIZE + LittleEndian.BYTE_SIZE + operand.length;
        ensureCapacity(addition);
        LittleEndian.putShort(_buf, _offset, opcode);
        _offset += LittleEndian.SHORT_SIZE;
        _buf[_offset++] = (byte) operand.length;
        System.arraycopy(operand, 0, _buf, _offset, operand.length);
    }

    public void addSprm(short opcode, int operand) {
        int addition = LittleEndian.SHORT_SIZE + LittleEndian.INT_SIZE;
        ensureCapacity(addition);
        LittleEndian.putShort(_buf, _offset, opcode);
        _offset += LittleEndian.SHORT_SIZE;
        LittleEndian.putInt(_buf, _offset, operand);
        _offset += LittleEndian.INT_SIZE;
    }

    public void addSprm(short opcode, short operand) {
        int addition = LittleEndian.SHORT_SIZE + LittleEndian.SHORT_SIZE;
        ensureCapacity(addition);
        LittleEndian.putShort(_buf, _offset, opcode);
        _offset += LittleEndian.SHORT_SIZE;
        LittleEndian.putShort(_buf, _offset, operand);
        _offset += LittleEndian.SHORT_SIZE;
    }

    public void append(byte[] grpprl) {
        append(grpprl, 0);
    }

    public void append(byte[] grpprl, int offset) {
        ensureCapacity(grpprl.length - offset);
        System.arraycopy(grpprl, offset, _buf, _offset, grpprl.length - offset);
        _offset += grpprl.length - offset;
    }

    public SprmBuffer clone() {
        try {
            SprmBuffer retVal = (SprmBuffer) super.clone();
            retVal._buf = new byte[_buf.length];
            System.arraycopy(_buf, 0, retVal._buf, 0, _buf.length);
            return retVal;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    private void ensureCapacity(int addition) {
        if (_offset + addition >= _buf.length) {
            // add 6 more than they need for use the next iteration
            //
            // commented - buffer shall not contain any additional bytes --
            // sergey
            // byte[] newBuf = new byte[_offset + addition + 6];
            byte[] newBuf = IOUtils.safelyAllocate(_offset + addition, MAX_RECORD_LENGTH);
            System.arraycopy(_buf, 0, newBuf, 0, _buf.length);
            _buf = newBuf;
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof SprmBuffer)) return false;
        SprmBuffer sprmBuf = (SprmBuffer) obj;
        return (Arrays.equals(_buf, sprmBuf._buf));
    }

    @Override
    public int hashCode() {
        assert false : "hashCode not designed";
        return 42; // any arbitrary constant will do
    }

    public SprmOperation findSprm(short opcode) {
        int operation = SprmOperation.getOperationFromOpcode(opcode);
        int type = SprmOperation.getTypeFromOpcode(opcode);

        SprmIterator si = new SprmIterator(_buf, 2);
        while (si.hasNext()) {
            SprmOperation i = si.next();
            if (i.getOperation() == operation && i.getType() == type)
                return i;
        }
        return null;
    }

    private int findSprmOffset(short opcode) {
        SprmOperation sprmOperation = findSprm(opcode);
        if (sprmOperation == null)
            return -1;

        return sprmOperation.getGrpprlOffset();
    }

    public byte[] toByteArray() {
        return _buf;
    }

    public SprmIterator iterator() {
        return new SprmIterator(_buf, _sprmsStartOffset);
    }

    public void updateSprm(short opcode, byte operand) {
        int grpprlOffset = findSprmOffset(opcode);
        if (grpprlOffset != -1) {
            _buf[grpprlOffset] = operand;
            return;
        }
        addSprm(opcode, operand);
    }

    public void updateSprm(short opcode, boolean operand) {
        int grpprlOffset = findSprmOffset(opcode);
        if (grpprlOffset != -1) {
            _buf[grpprlOffset] = (byte) (operand ? 1 : 0);
            return;
        }
        addSprm(opcode, operand ? 1 : 0);
    }

    public void updateSprm(short opcode, int operand) {
        int grpprlOffset = findSprmOffset(opcode);
        if (grpprlOffset != -1) {
            LittleEndian.putInt(_buf, grpprlOffset, operand);
            return;
        }
        addSprm(opcode, operand);
    }

    public void updateSprm(short opcode, short operand) {
        int grpprlOffset = findSprmOffset(opcode);
        if (grpprlOffset != -1) {
            LittleEndian.putShort(_buf, grpprlOffset, operand);
            return;
        }
        addSprm(opcode, operand);
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Sprms (");
        stringBuilder.append(_buf.length);
        stringBuilder.append(" byte(s)): ");
        for (SprmIterator iterator = iterator(); iterator.hasNext(); ) {
            try {
                stringBuilder.append(iterator.next());
            } catch (Exception exc) {
                stringBuilder.append("error");
            }
            stringBuilder.append("; ");
        }
        return stringBuilder.toString();
    }
}
