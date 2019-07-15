import org.apache.accumulo.tserver.*;


import org.apache.log4j.Level;

public class TLevel extends Level {

  private static final long serialVersionUID = 1L;
  public final static Level TABLET_HIST = new TLevel();

  protected TLevel() {
    super(Level.DEBUG_INT + 100, "TABLET_HIST", Level.DEBUG_INT + 100);
  }

}
