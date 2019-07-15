

import java.util.Set;
import org.apache.accumulo.core.client.AccumuloException;
import org.apache.accumulo.core.client.AccumuloSecurityException;
import org.apache.accumulo.core.client.admin.DelegationTokenConfig;
import org.apache.accumulo.core.client.admin.SecurityOperations;
import org.apache.accumulo.core.client.mock.MockAccumulo;
import org.apache.accumulo.core.client.security.tokens.AuthenticationToken;
import org.apache.accumulo.core.client.security.tokens.DelegationToken;
import org.apache.accumulo.core.client.security.tokens.PasswordToken;
import org.apache.accumulo.core.security.Authorizations;
import org.apache.accumulo.core.security.NamespacePermission;
import org.apache.accumulo.core.security.SystemPermission;
import org.apache.accumulo.core.security.TablePermission;


@Deprecated
class MockSecurityOperations implements SecurityOperations {
	private final MockAccumulo acu;

	MockSecurityOperations(MockAccumulo acu) {
		this.acu = acu;
	}

	@Deprecated
	@Override
	public void createUser(String user, byte[] password, Authorizations authorizations) throws AccumuloException, AccumuloSecurityException {
		createLocalUser(user, new PasswordToken(password));
		changeUserAuthorizations(user, authorizations);
	}

	@Override
	public void createLocalUser(String principal, PasswordToken password) throws AccumuloException, AccumuloSecurityException {
	}

	@Deprecated
	@Override
	public void dropUser(String user) throws AccumuloException, AccumuloSecurityException {
		dropLocalUser(user);
	}

	@Override
	public void dropLocalUser(String principal) throws AccumuloException, AccumuloSecurityException {
	}

	@Deprecated
	@Override
	public boolean authenticateUser(String user, byte[] password) throws AccumuloException, AccumuloSecurityException {
		return authenticateUser(user, new PasswordToken(password));
	}

	@Override
	public boolean authenticateUser(String principal, AuthenticationToken token) throws AccumuloException, AccumuloSecurityException {
		return token.equals(token);
	}

	@Deprecated
	@Override
	public void changeUserPassword(String user, byte[] password) throws AccumuloException, AccumuloSecurityException {
		changeLocalUserPassword(user, new PasswordToken(password));
	}

	@Override
	public void changeLocalUserPassword(String principal, PasswordToken token) throws AccumuloException, AccumuloSecurityException {
	}

	@Override
	public void changeUserAuthorizations(String principal, Authorizations authorizations) throws AccumuloException, AccumuloSecurityException {
	}

	@Override
	public Authorizations getUserAuthorizations(String principal) throws AccumuloException, AccumuloSecurityException {
		return null;
	}

	@Override
	public boolean hasTablePermission(String principal, String tableName, TablePermission perm) throws AccumuloException, AccumuloSecurityException {
		return false;
	}

	@Override
	public boolean hasNamespacePermission(String principal, String namespace, NamespacePermission permission) throws AccumuloException, AccumuloSecurityException {
		return false;
	}

	@Override
	public void grantSystemPermission(String principal, SystemPermission permission) throws AccumuloException, AccumuloSecurityException {
	}

	@Override
	public void revokeSystemPermission(String principal, SystemPermission permission) throws AccumuloException, AccumuloSecurityException {
	}

	@Override
	public void revokeTablePermission(String principal, String tableName, TablePermission permission) throws AccumuloException, AccumuloSecurityException {
	}

	@Override
	public void revokeNamespacePermission(String principal, String namespace, NamespacePermission permission) throws AccumuloException, AccumuloSecurityException {
	}

	@Deprecated
	@Override
	public Set<String> listUsers() throws AccumuloException, AccumuloSecurityException {
		return listLocalUsers();
	}

	@Override
	public Set<String> listLocalUsers() throws AccumuloException, AccumuloSecurityException {
		return null;
	}

	@Override
	public DelegationToken getDelegationToken(DelegationTokenConfig cfg) throws AccumuloException, AccumuloSecurityException {
		return null;
	}
}

