package de.bwl.bwfla.common.services.security;

import java.util.ArrayList;
import java.util.List;
import javax.xml.ws.handler.Handler;
import javax.xml.ws.handler.HandlerResolver;
import javax.xml.ws.handler.PortInfo;

public class SOAPClientAuthenticationHandlerResolver implements HandlerResolver {

    private final MachineToken authenticationToken;

    public SOAPClientAuthenticationHandlerResolver(MachineToken authenticationToken)
    {
        this.authenticationToken = authenticationToken;
    }

    @SuppressWarnings("unchecked")
    public List<Handler> getHandlerChain(PortInfo portInfo) {
        List<Handler> handlerChain = new ArrayList<Handler>();
        SOAPClientAuthenticationHandler hh = new SOAPClientAuthenticationHandler(authenticationToken);
        System.out.println("initialized handler");
        handlerChain.add(hh);
        return handlerChain;
    }
}
