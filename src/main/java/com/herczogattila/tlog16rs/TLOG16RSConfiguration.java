package com.herczogattila.tlog16rs;

import io.dropwizard.Configuration;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.hibernate.validator.constraints.*;
import javax.validation.constraints.*;

public class TLOG16RSConfiguration extends Configuration {
    // TODO: implement service configuration
    
    @NotEmpty
    protected String name;
    
    @NotEmpty
    protected String password;
    
    @NotEmpty
    protected String url;
    
    @NotEmpty
    protected String driver;
    
    @NotEmpty
    protected String serverConfigName;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getDriver() {
        return driver;
    }

    public void setDriver(String driver) {
        this.driver = driver;
    }

    public String getServerConfigName() {
        return serverConfigName;
    }

    public void setServerConfigName(String serverConfigName) {
        this.serverConfigName = serverConfigName;
    }
}
