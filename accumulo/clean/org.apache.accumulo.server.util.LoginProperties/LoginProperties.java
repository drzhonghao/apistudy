import org.apache.accumulo.server.util.*;


import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.accumulo.core.client.security.tokens.AuthenticationToken;
import org.apache.accumulo.core.client.security.tokens.AuthenticationToken.TokenProperty;
import org.apache.accumulo.core.conf.AccumuloConfiguration;
import org.apache.accumulo.core.conf.Property;
import org.apache.accumulo.server.client.HdfsZooInstance;
import org.apache.accumulo.server.conf.ServerConfigurationFactory;
import org.apache.accumulo.server.security.handler.Authenticator;
import org.apache.accumulo.start.classloader.vfs.AccumuloVFSClassLoader;
import org.apache.accumulo.start.spi.KeywordExecutable;

import com.google.auto.service.AutoService;

@AutoService(KeywordExecutable.class)
public class LoginProperties implements KeywordExecutable {

  @Override
  public String keyword() {
    return "login-info";
  }

  @Override
  public void execute(String[] args) throws Exception {
    AccumuloConfiguration config = new ServerConfigurationFactory(HdfsZooInstance.getInstance())
        .getConfiguration();
    Authenticator authenticator = AccumuloVFSClassLoader.getClassLoader()
        .loadClass(config.get(Property.INSTANCE_SECURITY_AUTHENTICATOR))
        .asSubclass(Authenticator.class).newInstance();

    List<Set<TokenProperty>> tokenProps = new ArrayList<>();

    for (Class<? extends AuthenticationToken> tokenType : authenticator.getSupportedTokenTypes()) {
      tokenProps.add(tokenType.newInstance().getProperties());
    }

    System.out
        .println("Supported token types for " + authenticator.getClass().getName() + " are : ");
    for (Class<? extends AuthenticationToken> tokenType : authenticator.getSupportedTokenTypes()) {
      System.out
          .println("\t" + tokenType.getName() + ", which accepts the following properties : ");

      for (TokenProperty tokenProperty : tokenType.newInstance().getProperties()) {
        System.out.println("\t\t" + tokenProperty);
      }

      System.out.println();
    }
  }

  public static void main(String[] args) throws Exception {
    new LoginProperties().execute(args);
  }
}
