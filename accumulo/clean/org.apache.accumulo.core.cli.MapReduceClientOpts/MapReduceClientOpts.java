import org.apache.accumulo.core.cli.ClientOpts;
import org.apache.accumulo.core.cli.*;


import org.apache.accumulo.core.client.AccumuloSecurityException;
import org.apache.accumulo.core.client.Connector;
import org.apache.accumulo.core.client.admin.DelegationTokenConfig;
import org.apache.accumulo.core.client.mapreduce.AccumuloInputFormat;
import org.apache.accumulo.core.client.mapreduce.AccumuloOutputFormat;
import org.apache.accumulo.core.client.security.tokens.AuthenticationToken;
import org.apache.accumulo.core.client.security.tokens.KerberosToken;
import org.apache.accumulo.core.security.SystemPermission;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.security.UserGroupInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adds some MR awareness to the ClientOpts
 */
public class MapReduceClientOpts extends ClientOpts {
  private static final Logger log = LoggerFactory.getLogger(MapReduceClientOpts.class);

  public void setAccumuloConfigs(Job job) throws AccumuloSecurityException {
    AccumuloInputFormat.setZooKeeperInstance(job, this.getClientConfiguration());
    AccumuloOutputFormat.setZooKeeperInstance(job, this.getClientConfiguration());
  }

  @Override
  public AuthenticationToken getToken() {
    AuthenticationToken authToken = super.getToken();
    // For MapReduce, Kerberos credentials don't make it to the Mappers and Reducers,
    // so we need to request a delegation token and use that instead.
    if (authToken instanceof KerberosToken) {
      log.info("Received KerberosToken, fetching DelegationToken for MapReduce");
      final KerberosToken krbToken = (KerberosToken) authToken;

      try {
        UserGroupInformation user = UserGroupInformation.getCurrentUser();
        if (!user.hasKerberosCredentials()) {
          throw new IllegalStateException("Expected current user to have Kerberos credentials");
        }

        String newPrincipal = user.getUserName();
        log.info("Obtaining delegation token for {}", newPrincipal);

        setPrincipal(newPrincipal);
        Connector conn = getInstance().getConnector(newPrincipal, krbToken);

        // Do the explicit check to see if the user has the permission to get a delegation token
        if (!conn.securityOperations().hasSystemPermission(conn.whoami(),
            SystemPermission.OBTAIN_DELEGATION_TOKEN)) {
          log.error(
              "{} doesn't have the {} SystemPermission neccesary to obtain a delegation"
                  + " token. MapReduce tasks cannot automatically use the client's"
                  + " credentials on remote servers. Delegation tokens provide a means to run"
                  + " MapReduce without distributing the user's credentials.",
              user.getUserName(), SystemPermission.OBTAIN_DELEGATION_TOKEN.name());
          throw new IllegalStateException(
              conn.whoami() + " does not have permission to obtain a delegation token");
        }

        // Get the delegation token from Accumulo
        return conn.securityOperations().getDelegationToken(new DelegationTokenConfig());
      } catch (Exception e) {
        final String msg = "Failed to acquire DelegationToken for use with MapReduce";
        log.error(msg, e);
        throw new RuntimeException(msg, e);
      }
    }
    return authToken;
  }
}
