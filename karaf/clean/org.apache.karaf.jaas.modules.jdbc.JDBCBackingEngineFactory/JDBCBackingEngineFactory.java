import org.apache.karaf.jaas.modules.jdbc.JDBCBackingEngine;
import org.apache.karaf.jaas.modules.jdbc.JDBCUtils;
import org.apache.karaf.jaas.modules.jdbc.JDBCLoginModule;
import org.apache.karaf.jaas.modules.jdbc.*;


import org.apache.karaf.jaas.modules.BackingEngine;
import org.apache.karaf.jaas.modules.BackingEngineFactory;
import org.apache.karaf.jaas.modules.encryption.EncryptionSupport;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.util.Map;

public class JDBCBackingEngineFactory implements BackingEngineFactory {

    private static final transient Logger LOGGER = LoggerFactory.getLogger(JDBCBackingEngineFactory.class);

    /**
     * Build a Backing engine for the JDBCLoginModule.
     */
    public BackingEngine build(Map<String, ?> options) {
        JDBCBackingEngine instance = null;
        String datasourceURL = (String) options.get(JDBCUtils.DATASOURCE);
        BundleContext bundleContext = (BundleContext) options.get(BundleContext.class.getName());

        String addUserStatement = (String) options.get(JDBCLoginModule.INSERT_USER_STATEMENT);
        String addRoleStatement = (String) options.get(JDBCLoginModule.INSERT_ROLE_STATEMENT);
        String deleteRoleStatement = (String) options.get(JDBCLoginModule.DELETE_ROLE_STATEMENT);
        String deleteAllUserRolesStatement = (String) options.get(JDBCLoginModule.DELETE_ROLES_STATEMENT);
        String deleteUserStatement = (String) options.get(JDBCLoginModule.DELETE_USER_STATEMENT);
        String selectUsersQuery = (String) options.get(JDBCLoginModule.USER_QUERY);
        String selectRolesQuery = (String) options.get(JDBCLoginModule.ROLE_QUERY);

        try {
            DataSource dataSource = JDBCUtils.createDatasource(bundleContext, datasourceURL);
            EncryptionSupport encryptionSupport = new EncryptionSupport(options);
            instance = new JDBCBackingEngine(dataSource, encryptionSupport);
            if(addUserStatement != null) {
                instance.setAddUserStatement(addUserStatement);
            }
            if(addRoleStatement != null) {
                instance.setAddRoleStatement(addRoleStatement);
            }
            if(deleteRoleStatement != null) {
                instance.setDeleteRoleStatement(deleteRoleStatement);
            }
            if(deleteAllUserRolesStatement != null) {
                instance.setDeleteAllUserRolesStatement(deleteAllUserRolesStatement);
            }
            if(deleteUserStatement != null) {
                instance.setDeleteUserStatement(deleteUserStatement);
            }
            if(selectUsersQuery != null) {
                instance.setSelectUsersQuery(selectUsersQuery);
            }
            if(selectRolesQuery != null) {
                instance.setSelectRolesQuery(selectRolesQuery);
            }
        } catch (Exception e) {
            LOGGER.error("Error creating JDBCBackingEngine.", e);
        }
        return instance;
    }

    /**
     * Returns the login module class, that this factory can build.
     */
    public String getModuleClass() {
        return JDBCLoginModule.class.getName();
    }

}
