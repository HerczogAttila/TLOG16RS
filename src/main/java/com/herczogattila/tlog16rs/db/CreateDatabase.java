/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.herczogattila.tlog16rs.db;

import com.avaje.ebean.EbeanServer;
import com.avaje.ebean.EbeanServerFactory;
import com.avaje.ebean.config.DataSourceConfig;
import com.avaje.ebean.config.ServerConfig;
import com.herczogattila.tlog16rs.TLOG16RSConfiguration;
import com.herczogattila.tlog16rs.entities.Task;
import com.herczogattila.tlog16rs.entities.TimeLogger;
import com.herczogattila.tlog16rs.entities.WorkDay;
import com.herczogattila.tlog16rs.entities.WorkMonth;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import javax.inject.Inject;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.FileSystemResourceAccessor;
import org.avaje.agentloader.AgentLoader;

/**
 *
 * @author Attila
 */
public class CreateDatabase {
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(CreateDatabase.class);
    
    @Inject
    private final EbeanServer ebeanServer;
    private final DataSourceConfig dataSourceConfig;
    private final ServerConfig serverConfig;
    
    public CreateDatabase(TLOG16RSConfiguration config) {
        dataSourceConfig = new DataSourceConfig();
        setDataSourceConfig(config);
                        
        serverConfig = new ServerConfig();
        serverConfig.setDdlGenerate(false);
        serverConfig.setDdlRun(false);
        serverConfig.setRegister(true);
        serverConfig.setDefaultServer(true);
        serverConfig.setDataSourceConfig(dataSourceConfig);
        serverConfig.addClass(Task.class);
        serverConfig.addClass(WorkDay.class);
        serverConfig.addClass(WorkMonth.class);
        serverConfig.addClass(TimeLogger.class);
        
        
        setServerConfig(config);
        updateSchema(config);
        agentLoader();
        ebeanServer = EbeanServerFactory.create(serverConfig);
    }
    
    private void setDataSourceConfig(TLOG16RSConfiguration config) {
        dataSourceConfig.setDriver(config.getDriver());
        dataSourceConfig.setUrl(config.getUrl());
        dataSourceConfig.setUsername(config.getName());
        dataSourceConfig.setPassword(config.getPassword());
    }
    
    private void setServerConfig(TLOG16RSConfiguration config) {
        serverConfig.setName(config.getServerConfigName());
    }

    private void updateSchema(TLOG16RSConfiguration config) {
        Connection c = null;
        try {
            Class.forName(config.getDriver());
            c = DriverManager.getConnection(config.getUrl(), config.getName(), config.getPassword());
            Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(c));
            Liquibase liquibase = new Liquibase("migrations.xml", new FileSystemResourceAccessor(), database);
            liquibase.update("create");
            liquibase.update("addTimeLoggerName");
            liquibase.update("addTimeLoggerLogin");
            c.close();
        } catch(ClassNotFoundException | SQLException | LiquibaseException e) {
            LOG.error("updateSchema: " + e.getMessage(), e);
        }
        finally {
            try {
                if(c != null)
                    c.close();
            } catch (SQLException e) {
                LOG.error("updateSchema: " + e.getMessage(), e);
            }
        }
    }
    
    private void agentLoader() {        
        if (!AgentLoader.loadAgentFromClasspath("avaje-ebeanorm-agent", "debug=1;packages=com.herczogattila.tlog16rs.**")) {
            LOG.error("avaje-ebeanorm-agent not found in classpath - not dynamically loaded");
        }
    }

    public EbeanServer getEbeanServer() {
        return ebeanServer;
    }
}
