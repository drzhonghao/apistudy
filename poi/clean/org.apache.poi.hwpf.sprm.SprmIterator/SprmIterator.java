import org.apache.poi.hwpf.sprm.SprmOperation;
import org.apache.poi.hwpf.sprm.*;


import org.apache.poi.util.Internal;

/**
 * This class is used to iterate through a list of sprms from a Word 97/2000/XP
 * document.
 * @author Ryan Ackley
 * @version 1.0
 */
@Internal
public final class SprmIterator
{
  private byte[] _grpprl;
  int _offset;

  public SprmIterator(byte[] grpprl, int offset)
  {
    _grpprl = grpprl;
    _offset = offset;
  }

  public boolean hasNext()
  {
    // A Sprm is at least 2 bytes long
    return _offset < (_grpprl.length-1);
  }

  public SprmOperation next()
  {
    SprmOperation op = new SprmOperation(_grpprl, _offset);
    _offset += op.size();
    return op;
  }


}
