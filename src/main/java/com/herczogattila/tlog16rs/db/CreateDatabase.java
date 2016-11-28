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
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import liquibase.Contexts;
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
    private final EbeanServer ebeanServer;
    private Liquibase liquibase;
    
    public CreateDatabase() {
        DataSourceConfig dataSourceConfig = new DataSourceConfig();
        dataSourceConfig.setDriver(System.getProperty("Dsys.prop.driver"));
        dataSourceConfig.setUrl(System.getProperty("Dsys.prop.url"));
        dataSourceConfig.setUsername(System.getProperty("Dsys.prop.user"));
        dataSourceConfig.setPassword(System.getProperty("Dsys.prop.password"));
                
        ServerConfig serverConfig = new ServerConfig();
        serverConfig.setName(System.getProperty("Dsys.prop.user"));
        serverConfig.setDdlGenerate(true);
        serverConfig.setDdlRun(true);
        serverConfig.setRegister(false);
        serverConfig.setDataSourceConfig(dataSourceConfig);
        serverConfig.addClass(TestEntity.class);
        
        updateSchema();
        agentLoader();
        ebeanServer = EbeanServerFactory.create(serverConfig);
    }
    
    private void updateSchema() {
        try {
            Class.forName(System.getProperty("Dsys.prop.driver"));
            Connection c = DriverManager.getConnection(System.getProperty("Dsys.prop.url"), System.getProperty("Dsys.prop.user"), System.getProperty("Dsys.prop.password"));
            Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(c));
            liquibase = new Liquibase("src/main/java/com/herczogattila/tlog16rs/resources/migrations.xml", new FileSystemResourceAccessor(), database);
            liquibase.update(new Contexts("create"));
        } catch(ClassNotFoundException | SQLException | LiquibaseException e) { System.out.println("updateSchema: " + e.getMessage()); }
    }
    
    private void agentLoader() {        
        if (!AgentLoader.loadAgentFromClasspath("avaje-ebeanorm-agent", "debug=1;packages=com.yourname.tlog16rs.**")) {
            System.err.println("avaje-ebeanorm-agent not found in classpath - not dynamically loaded");
        }
    }

    public EbeanServer getEbeanServer() {
        return ebeanServer;
    }
}
