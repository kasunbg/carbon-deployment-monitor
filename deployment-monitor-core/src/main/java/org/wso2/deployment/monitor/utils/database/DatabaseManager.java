/*
 * Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.deployment.monitor.utils.database;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tomcat.jdbc.pool.DataSource;
import org.apache.tomcat.jdbc.pool.PoolProperties;
import org.wso2.deployment.monitor.core.ConfigurationManager;
import org.wso2.deployment.monitor.core.DeploymentMonitorException;
import org.wso2.deployment.monitor.core.model.DatasourceConfig;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Database Management class.
 */
public class DatabaseManager {
    private static final Log log = LogFactory.getLog(DatabaseManager.class);

    private static volatile DataSource dataSource = null;

    private DatabaseManager() {
    }

    /**
     * Initializes the data source
     */
    private static void initialize() throws DeploymentMonitorException {
        synchronized (DatabaseManager.class) {
            if (dataSource == null) {
                if (log.isDebugEnabled()) {
                    log.debug("Initializing data source");
                }
                dataSource = createDatasource();
            }
        }
    }

    /**
     * Initializes the data source
     */
    private static DataSource createDatasource() throws DeploymentMonitorException {
        DatasourceConfig datasourceConfig = ConfigurationManager.getConfiguration().getDatasourceConfig();
        String expMsg = "Cannot initialize the datasource. Datasource configuration does not contain the ";

        PoolProperties props = new PoolProperties();
        //These are compulsory
        if (!datasourceConfig.getDatabaseUrl().isEmpty()) {
            props.setUrl(datasourceConfig.getDatabaseUrl());
        } else {
            throw new DeploymentMonitorException(expMsg + "database URL.");
        }

        if (!datasourceConfig.getDriverClassName().isEmpty()) {
            props.setDriverClassName(datasourceConfig.getDriverClassName());
        } else {
            throw new DeploymentMonitorException(expMsg + "Driver Class Name.");
        }

        if (!datasourceConfig.getUsername().isEmpty()) {
            props.setUsername(datasourceConfig.getUsername());
        } else {
            throw new DeploymentMonitorException(expMsg + "database username.");
        }

        if (!datasourceConfig.getPassword().isEmpty()) {
            props.setPassword(datasourceConfig.getPassword());
        } else {
            throw new DeploymentMonitorException(expMsg + "database users password.");
        }

        //Optional
        props.setTestOnBorrow(datasourceConfig.isTestOnBorrow());
        props.setValidationQuery(datasourceConfig.getValidationQuery());
        props.setValidationInterval(datasourceConfig.getValidationInterval());
        props.setMaxWait(datasourceConfig.getMaxWait());
        props.setMaxActive(datasourceConfig.getMaxActive());

        DataSource datasource = new DataSource();
        datasource.setPoolProperties(props);
        return datasource;
    }

    /**
     * Returns a Database connection for the defined datasource
     *
     * @return {@link Connection}
     * @throws SQLException
     * @throws DeploymentMonitorException
     */
    public static Connection getConnection() throws SQLException, DeploymentMonitorException {
        if (dataSource == null) {
            initialize();
        }
        return dataSource.getConnection();
    }

    /**
     * Closes a Database {@link Connection}, {@link PreparedStatement} and {@link ResultSet}
     *
     * @param preparedStatement PreparedStatement
     * @param connection        Connection
     * @param resultSet         ResultSet
     */
    public static void closeAllConnections(Connection connection, PreparedStatement preparedStatement,
            ResultSet resultSet) {
        closeConnection(connection);
        closeResultSet(resultSet);
        closeStatement(preparedStatement);
    }

    /**
     * Close Connection
     *
     * @param dbConnection Connection
     */
    private static void closeConnection(Connection dbConnection) {
        if (dbConnection != null) {
            try {
                dbConnection.close();
            } catch (SQLException e) {
                log.warn("Error occurred while closing the database connection.", e);
            }
        }
    }

    /**
     * Close ResultSet
     *
     * @param resultSet ResultSet
     */
    private static void closeResultSet(ResultSet resultSet) {
        if (resultSet != null) {
            try {
                resultSet.close();
            } catch (SQLException e) {
                log.warn("Error occurred while closing the result set.", e);
            }
        }

    }

    /**
     * Close PreparedStatement
     *
     * @param preparedStatement PreparedStatement
     */
    private static void closeStatement(PreparedStatement preparedStatement) {
        if (preparedStatement != null) {
            try {
                preparedStatement.close();
            } catch (SQLException e) {
                log.warn("Error occurred while closing the prepared statement.", e);
            }
        }

    }
}



