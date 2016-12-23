package com.herczogattila.tlog16rs;

import io.dropwizard.Configuration;
import org.hibernate.validator.constraints.*;

@lombok.Getter
@lombok.Setter
public class TLOG16RSConfiguration extends Configuration {
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
}
