

import java.security.Principal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.apache.karaf.jaas.boot.principal.GroupPrincipal;
import org.apache.karaf.jaas.boot.principal.RolePrincipal;
import org.apache.karaf.jaas.boot.principal.UserPrincipal;
import org.apache.karaf.jaas.modules.BackingEngine;
import org.apache.karaf.jaas.modules.encryption.EncryptionSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class JDBCBackingEngine implements BackingEngine {
	private final Logger logger = LoggerFactory.getLogger(JDBCBackingEngine.class);

	private DataSource dataSource;

	private EncryptionSupport encryptionSupport;

	private String addUserStatement = "INSERT INTO USERS VALUES(?,?)";

	private String addRoleStatement = "INSERT INTO ROLES VALUES(?,?)";

	private String deleteRoleStatement = "DELETE FROM ROLES WHERE USERNAME=? AND ROLE=?";

	private String deleteAllUserRolesStatement = "DELETE FROM ROLES WHERE USERNAME=?";

	private String deleteUserStatement = "DELETE FROM USERS WHERE USERNAME=?";

	private String selectUsersQuery = "SELECT USERNAME FROM USERS";

	private String selectUserQuery = "SELECT USERNAME FROM USERS WHERE USERNAME=?";

	private String selectRolesQuery = "SELECT ROLE FROM ROLES WHERE USERNAME=?";

	public JDBCBackingEngine(DataSource dataSource) {
		this.dataSource = dataSource;
		this.encryptionSupport = EncryptionSupport.noEncryptionSupport();
	}

	public JDBCBackingEngine(DataSource dataSource, EncryptionSupport encryptionSupport) {
		this.dataSource = dataSource;
		this.encryptionSupport = encryptionSupport;
	}

	public void addUser(String username, String password) {
		if (username.startsWith(BackingEngine.GROUP_PREFIX)) {
			throw new IllegalArgumentException(("Prefix not permitted: " + (BackingEngine.GROUP_PREFIX)));
		}
		String encPassword = encryptionSupport.encrypt(password);
		try {
			try (Connection connection = dataSource.getConnection()) {
				rawUpdate(connection, addUserStatement, username, encPassword);
			}
		} catch (SQLException e) {
			throw new RuntimeException("Error adding user", e);
		}
	}

	public void deleteUser(String username) {
		try {
			try (Connection connection = dataSource.getConnection()) {
				rawUpdate(connection, deleteAllUserRolesStatement, username);
				rawUpdate(connection, deleteUserStatement, username);
				if (!(connection.getAutoCommit())) {
					connection.commit();
				}
			}
		} catch (SQLException e) {
			throw new RuntimeException("Error deleting user", e);
		}
	}

	public List<UserPrincipal> listUsers() {
		try {
			try (Connection connection = dataSource.getConnection()) {
				List<UserPrincipal> users = new ArrayList<>();
				for (String name : rawSelect(connection, selectUsersQuery)) {
					if (!(name.startsWith(BackingEngine.GROUP_PREFIX))) {
						users.add(new UserPrincipal(name));
					}
				}
				return users;
			}
		} catch (SQLException e) {
			throw new RuntimeException("Error listing users", e);
		}
	}

	@Override
	public UserPrincipal lookupUser(String username) {
		try {
			try (Connection connection = dataSource.getConnection()) {
				List<String> names = rawSelect(connection, selectUserQuery, username);
				if ((names.size()) == 0) {
					return null;
				}
				return new UserPrincipal(username);
			}
		} catch (SQLException e) {
			throw new RuntimeException("Error getting user", e);
		}
	}

	public List<RolePrincipal> listRoles(Principal principal) {
		try {
			try (Connection connection = dataSource.getConnection()) {
				if (principal instanceof GroupPrincipal) {
					return listRoles(connection, ((BackingEngine.GROUP_PREFIX) + (principal.getName())));
				}else {
					return listRoles(connection, principal.getName());
				}
			}
		} catch (SQLException e) {
			throw new RuntimeException("Error listing roles", e);
		}
	}

	private List<RolePrincipal> listRoles(Connection connection, String name) throws SQLException {
		List<RolePrincipal> roles = new ArrayList<>();
		for (String role : rawSelect(connection, selectRolesQuery, name)) {
			if (role.startsWith(BackingEngine.GROUP_PREFIX)) {
				roles.addAll(listRoles(connection, role));
			}else {
				roles.add(new RolePrincipal(role));
			}
		}
		return roles;
	}

	public void addRole(String username, String role) {
		try {
			try (Connection connection = dataSource.getConnection()) {
				rawUpdate(connection, addRoleStatement, username, role);
				if (!(connection.getAutoCommit())) {
					connection.commit();
				}
			}
		} catch (SQLException e) {
			throw new RuntimeException("Error adding role", e);
		}
	}

	public void deleteRole(String username, String role) {
		try {
			try (Connection connection = dataSource.getConnection()) {
				rawUpdate(connection, deleteRoleStatement, username, role);
				if (!(connection.getAutoCommit())) {
					connection.commit();
				}
			}
		} catch (SQLException e) {
			throw new RuntimeException("Error deleting role", e);
		}
	}

	@Override
	public List<GroupPrincipal> listGroups(UserPrincipal principal) {
		try {
			try (Connection connection = dataSource.getConnection()) {
				List<GroupPrincipal> roles = new ArrayList<>();
				for (String role : rawSelect(connection, selectRolesQuery, principal.getName())) {
					if (role.startsWith(BackingEngine.GROUP_PREFIX)) {
						roles.add(new GroupPrincipal(role.substring(BackingEngine.GROUP_PREFIX.length())));
					}
				}
				return roles;
			}
		} catch (SQLException e) {
			throw new RuntimeException("Error deleting role", e);
		}
	}

	@Override
	public void addGroup(String username, String group) {
		try {
			try (Connection connection = dataSource.getConnection()) {
				String groupName = (BackingEngine.GROUP_PREFIX) + group;
				rawUpdate(connection, addRoleStatement, username, groupName);
				if (!(connection.getAutoCommit())) {
					connection.commit();
				}
			}
		} catch (SQLException e) {
			logger.error("Error executing statement", e);
		}
	}

	@Override
	public void deleteGroup(String username, String group) {
		try {
			try (Connection connection = dataSource.getConnection()) {
				rawUpdate(connection, deleteRoleStatement, username, ((BackingEngine.GROUP_PREFIX) + group));
				boolean inUse = false;
				for (String user : rawSelect(connection, selectUsersQuery)) {
					for (String g : rawSelect(connection, selectRolesQuery, user)) {
						if (group.equals(g)) {
							inUse = true;
							break;
						}
					}
				}
				if (!inUse) {
					rawUpdate(connection, deleteAllUserRolesStatement, ((BackingEngine.GROUP_PREFIX) + group));
				}
				if (!(connection.getAutoCommit())) {
					connection.commit();
				}
			}
		} catch (SQLException e) {
			logger.error("Error executing statement", e);
		}
	}

	@Override
	public void addGroupRole(String group, String role) {
		addRole(((BackingEngine.GROUP_PREFIX) + group), role);
	}

	@Override
	public void deleteGroupRole(String group, String role) {
		deleteRole(((BackingEngine.GROUP_PREFIX) + group), role);
	}

	protected void rawUpdate(Connection connection, String query, String... params) throws SQLException {
		if (logger.isDebugEnabled()) {
		}
	}

	protected List<String> rawSelect(Connection connection, String query, String... params) throws SQLException {
		return null;
	}

	public String getAddUserStatement() {
		return addUserStatement;
	}

	public void setAddUserStatement(String addUserStatement) {
		this.addUserStatement = addUserStatement;
	}

	public String getAddRoleStatement() {
		return addRoleStatement;
	}

	public void setAddRoleStatement(String addRoleStatement) {
		this.addRoleStatement = addRoleStatement;
	}

	public String getDeleteRoleStatement() {
		return deleteRoleStatement;
	}

	public void setDeleteRoleStatement(String deleteRoleStatement) {
		this.deleteRoleStatement = deleteRoleStatement;
	}

	public String getDeleteAllUserRolesStatement() {
		return deleteAllUserRolesStatement;
	}

	public void setDeleteAllUserRolesStatement(String deleteAllUserRolesStatement) {
		this.deleteAllUserRolesStatement = deleteAllUserRolesStatement;
	}

	public String getDeleteUserStatement() {
		return deleteUserStatement;
	}

	public void setDeleteUserStatement(String deleteUserStatement) {
		this.deleteUserStatement = deleteUserStatement;
	}

	public String getSelectUsersQuery() {
		return selectUsersQuery;
	}

	public void setSelectUsersQuery(String selectUsersQuery) {
		this.selectUsersQuery = selectUsersQuery;
	}

	public String getSelectRolesQuery() {
		return selectRolesQuery;
	}

	public void setSelectRolesQuery(String selectRolesQuery) {
		this.selectRolesQuery = selectRolesQuery;
	}

	@Override
	public Map<GroupPrincipal, String> listGroups() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void createGroup(String group) {
		throw new UnsupportedOperationException();
	}
}

