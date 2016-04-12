package org.exoplatform.tracfin.profile;


import org.exoplatform.container.PortalContainer;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.web.filter.Filter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


public class ProfileFilter implements Filter {


    private static final Log LOG = ExoLogger.getLogger(UpdateProfileRESTService.class);
    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse,
                         FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
        HttpServletResponse httpServletResponse = (HttpServletResponse) servletResponse;
        String uri = httpServletRequest.getRequestURI();

        ConversationState state = ConversationState.getCurrent();
        String userId = (state != null) ? state.getIdentity().getUserId() : null;

        try {
            if (uri.contains("/profile/")) {
                String username = uri.split("/profile/")[1];
                OrganizationService organizationService = (OrganizationService) PortalContainer.getInstance().getComponentInstanceOfType(OrganizationService.class);
                if (organizationService.getUserHandler().findUserByName(username) == null) {
                    httpServletResponse.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }
            }
            else if (uri.contains("/connections/network/")) {
                String username = uri.split("/connections/network/")[1];
                OrganizationService organizationService = (OrganizationService) PortalContainer.getInstance().getComponentInstanceOfType(OrganizationService.class);
                if (organizationService.getUserHandler().findUserByName(username) == null) {
                    httpServletResponse.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }
            }
            else if (uri.contains("/activities/")) {
                String username = uri.split("/activities/")[1];
                OrganizationService organizationService = (OrganizationService) PortalContainer.getInstance().getComponentInstanceOfType(OrganizationService.class);
                if (organizationService.getUserHandler().findUserByName(username) == null) {
                    httpServletResponse.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }
            }
            else if (uri.contains("/wiki/user/")) {
                String username = uri.split("/wiki/user/")[1];
                OrganizationService organizationService = (OrganizationService) PortalContainer.getInstance().getComponentInstanceOfType(OrganizationService.class);
                if (organizationService.getUserHandler().findUserByName(username) == null) {
                    httpServletResponse.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }
            }
        } catch (Exception e) {
            LOG.error(e, e.getCause());
        }

        filterChain.doFilter(servletRequest, servletResponse);
    }
}
