import org.apache.accumulo.core.security.crypto.*;


import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DiscardCloseOutputStream extends FilterOutputStream {

  private static final Logger log = LoggerFactory.getLogger(DiscardCloseOutputStream.class);

  public DiscardCloseOutputStream(OutputStream out) {
    super(out);
  }

  @Override
  public void close() throws IOException {
    // Discard
    log.trace("Discarded close");
  }

}
