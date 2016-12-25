package com.herczogattila.tlog16rs;

import com.herczogattila.tlog16rs.resources.TLOG16RSResource;
import io.dropwizard.Application;
import io.dropwizard.setup.Environment;

public class TLOG16RSApplication extends Application<TLOG16RSConfiguration> {

    public static void main(final String[] args) throws Exception {
        new TLOG16RSApplication().run(args);
    }

    @Override
    public String getName() {
        return "TLOG16RS";
    }

    @Override
    public void run(final TLOG16RSConfiguration configuration,
                    final Environment environment) {
        environment.jersey().register(new TLOG16RSResource(configuration));
    }

}
