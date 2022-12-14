import org.apache.accumulo.core.util.*;


import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import org.apache.accumulo.core.cli.ClientOpts.Password;
import org.apache.accumulo.core.cli.ClientOpts.PasswordConverter;
import org.apache.accumulo.core.cli.Help;
import org.apache.accumulo.core.client.security.tokens.AuthenticationToken;
import org.apache.accumulo.core.client.security.tokens.AuthenticationToken.AuthenticationTokenSerializer;
import org.apache.accumulo.core.client.security.tokens.AuthenticationToken.Properties;
import org.apache.accumulo.core.client.security.tokens.AuthenticationToken.TokenProperty;
import org.apache.accumulo.core.client.security.tokens.PasswordToken;
import org.apache.accumulo.start.spi.KeywordExecutable;

import com.beust.jcommander.Parameter;
import com.google.auto.service.AutoService;

import jline.console.ConsoleReader;

@AutoService(KeywordExecutable.class)
public class CreateToken implements KeywordExecutable {

  private ConsoleReader reader = null;

  private ConsoleReader getConsoleReader() throws IOException {
    if (reader == null)
      reader = new ConsoleReader();
    return reader;
  }

  static class Opts extends Help {
    @Parameter(names = {"-u", "--user"}, description = "Connection user")
    public String principal = null;

    @Parameter(names = "-p", converter = PasswordConverter.class,
        description = "Connection password")
    public Password password = null;

    @Parameter(names = "--password", converter = PasswordConverter.class,
        description = "Enter the connection password", password = true)
    public Password securePassword = null;

    @Parameter(names = {"-tc", "--tokenClass"},
        description = "The class of the authentication token")
    public String tokenClassName = PasswordToken.class.getName();

    @Parameter(names = {"-f", "--file"},
        description = "The filename to save the auth token to. Multiple tokens"
            + " can be stored in the same file, but only the first for each user will"
            + " be recognized.")
    public String tokenFile = null;
  }

  public static void main(String[] args) {
    new CreateToken().execute(args);
  }

  @Override
  public String keyword() {
    return "create-token";
  }

  @Override
  public void execute(String[] args) {
    Opts opts = new Opts();
    opts.parseArgs(CreateToken.class.getName(), args);

    Password pass = opts.password;
    if (pass == null && opts.securePassword != null) {
      pass = opts.securePassword;
    }

    try {
      String principal = opts.principal;
      if (principal == null) {
        principal = getConsoleReader().readLine("Username (aka principal): ");
      }

      AuthenticationToken token = Class.forName(opts.tokenClassName)
          .asSubclass(AuthenticationToken.class).newInstance();
      Properties props = new Properties();
      for (TokenProperty tp : token.getProperties()) {
        String input;
        if (pass != null && tp.getKey().equals("password")) {
          input = pass.toString();
        } else {
          if (tp.getMask()) {
            input = getConsoleReader().readLine(tp.getDescription() + ": ", '*');
          } else {
            input = getConsoleReader().readLine(tp.getDescription() + ": ");
          }
        }
        props.put(tp.getKey(), input);
        token.init(props);
      }
      String tokenBase64 = Base64
          .encodeBase64String(AuthenticationTokenSerializer.serialize(token));

      String tokenFile = opts.tokenFile;
      if (tokenFile == null) {
        tokenFile = getConsoleReader().readLine("File to save auth token to: ");
      }
      File tf = new File(tokenFile);
      if (!tf.exists()) {
        if (!tf.createNewFile()) {
          throw new IOException("Couldn't create " + tf.getCanonicalPath());
        }
      }
      PrintStream out = new PrintStream(new FileOutputStream(tf, true), true, UTF_8.name());
      String outString = principal + ":" + opts.tokenClassName + ":" + tokenBase64;
      out.println(outString);
      out.close();
      System.out.println("Token written to " + tokenFile + ". Remember to upload it to hdfs.");
    } catch (IOException | InstantiationException | IllegalAccessException
        | ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }
}
