package de.bwl.bwfla.common.services.security;

import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Produces;
import java.util.logging.Logger;

@RequestScoped
public class AuthenticatedUserProducer {
    @Produces
    @RequestScoped
    @AuthenticatedUser
    private UserContext authenticatedUser = new UserContext();

    protected static final Logger LOG = Logger.getLogger("Authentication");

    // todo: allow override label
    private final String adminRoleLabel = "eaas-admin";

    public AuthenticatedUserProducer() {}

    public void handleAuthenticationEvent(@Observes @AuthenticatedUser AuthenticationFilter.JwtLoginEvent event) {

        DecodedJWT jwt = event.getJwt();
        if(jwt == null || jwt.getClaim("sub") == null)
        {
            authenticatedUser.setRole(Role.PUBLIC);
            authenticatedUser.setUsername("anonymous");
            authenticatedUser.setUserId("anonymous");
            return;
        }

        Claim userIdC = jwt.getClaim("sub");
        authenticatedUser.setUserId(userIdC.asString());
        authenticatedUser.setRole(Role.RESTRCITED);

        Claim usernameC = jwt.getClaim("preferred_username");
        if(usernameC != null)
        {
            authenticatedUser.setUsername(usernameC.asString());
        }

        Claim roles = jwt.getClaim("roles");
        if(roles != null)
        {
            String[] roleList = roles.asArray(String.class);
            if(roleList != null) {
                for (String r : roleList) {
                    if (r.equals(adminRoleLabel))
                        authenticatedUser.setRole(Role.ADMIN);
                }
            }
        }


        Claim nameC = jwt.getClaim("name");
        if(nameC != null)
            authenticatedUser.setName(nameC.asString());
    }
}
