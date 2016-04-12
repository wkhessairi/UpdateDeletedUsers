package org.exoplatform.tracfin.profile;

import org.exoplatform.commons.chromattic.ChromatticLifeCycle;
import org.exoplatform.commons.chromattic.ChromatticManager;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.impl.core.SessionImpl;
import org.exoplatform.services.jcr.impl.core.value.BooleanValue;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.idm.PicketLinkIDMCacheService;
import org.exoplatform.services.organization.idm.PicketLinkIDMService;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.storage.impl.IdentityStorageImpl;
import org.gatein.management.api.operation.OperationContext;
import org.picketlink.idm.impl.api.session.IdentitySessionImpl;
import org.picketlink.idm.impl.api.session.context.IdentitySessionContext;
import org.picketlink.idm.impl.model.hibernate.HibernateIdentityObject;
import org.picketlink.idm.impl.model.ldap.LDAPIdentityObjectImpl;
import org.picketlink.idm.spi.model.IdentityObject;
import org.picketlink.idm.spi.repository.IdentityStoreRepository;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.transaction.SystemException;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import java.io.IOException;

@Path("/social")
public class UpdateProfileRESTService implements ResourceContainer {

	private static final Log LOG = ExoLogger.getLogger(UpdateProfileRESTService.class);
	@GET
	@Path("GetUsersDeletionList")
	public String getUsersList() throws IOException {
		String workspace = "social";
		String path = "/production/soc:providers/soc:organization";

		OrganizationService organizationService = (OrganizationService) PortalContainer.getInstance().getComponentInstanceOfType(OrganizationService.class);
		RepositoryService repoService = (RepositoryService) PortalContainer.getInstance().getComponentInstanceOfType(RepositoryService.class);
		ManageableRepository repository;

		String list = "Users which are mapped only in IDM or Existing only in connections portlet are: \n";
		try {
			repository = repoService.getCurrentRepository();

			SessionProviderService sessionProviderService = (SessionProviderService) PortalContainer.getInstance().getComponentInstanceOfType(SessionProviderService.class);
			SessionProvider sessionProvider = sessionProviderService.getSystemSessionProvider(null);
			Session session = sessionProvider.getSession(workspace, repository);

			// Invalidate plidmcache
			PicketLinkIDMCacheService picketLinkIDMCacheService = (PicketLinkIDMCacheService) PortalContainer.getInstance().getComponentInstanceOfType(PicketLinkIDMCacheService.class);
			picketLinkIDMCacheService.invalidateAll();


			PicketLinkIDMService picketLinkIDMService = (PicketLinkIDMService) PortalContainer.getInstance().getComponentInstanceOfType(PicketLinkIDMService.class);
			IdentitySessionImpl identitySession = (IdentitySessionImpl) picketLinkIDMService.getIdentitySession();

			IdentitySessionContext identitySessionContext = identitySession.getSessionContext();
			IdentityStoreRepository identityStoreRepository = identitySessionContext.getIdentityStoreRepository();

			Node parentUsersNode = (Node) session.getItem(path);
			NodeIterator it = parentUsersNode.getNodes();
			String superuser = System.getProperty("exo.super.user") == null ? "root" : System.getProperty("exo.super.user");

			while (it.hasNext()) { // iterate on all social nodes
				Node socNode = it.nextNode();
				String user = socNode.getName().substring(4);

				String profilePath = path + "/" + socNode.getName() + "/soc:profile";
				Node userNode = (Node) session.getItem(profilePath);

				IdentityObject identity = identityStoreRepository.findIdentityObject(
						identitySessionContext.resolveStoreInvocationContext(), user,
						identitySessionContext.getIdentityObjectTypeMapper().getIdentityObjectType());

				if ((identity instanceof HibernateIdentityObject && (!user.equals(superuser))) || organizationService.getUserHandler().findUserByName(user) == null) {
					if (!userNode.hasProperty("void-deleted") || !userNode.getProperty("void-deleted").getValues()[0].getString().equals("true"))
					//Users mapped only in IDM or Existing only in connections portlet
					list = list.concat(user + "\n");
				}
			}
		} catch (Exception e) {
			LOG.error(e, e.getCause());
			return "Failed";
		}
		return list+"\n ================== end ================";
	}

	@GET
	@Path("DeleteAllUsers")
	public String deleteAllUsers() throws IOException {
		String workspace = "social";
		String path = "/production/soc:providers/soc:organization";

		OrganizationService organizationService = (OrganizationService) PortalContainer.getInstance().getComponentInstanceOfType(OrganizationService.class);
		RepositoryService repoService = (RepositoryService) PortalContainer.getInstance().getComponentInstanceOfType(RepositoryService.class);
		ManageableRepository repository;
		IdentityStorageImpl storage = (IdentityStorageImpl) PortalContainer.getInstance().getComponentInstanceOfType(IdentityStorageImpl.class);
		IdentityManager identityManager = (IdentityManager) PortalContainer.getInstance().getComponentInstanceOfType(IdentityManager.class);

		try {
			repository = repoService.getCurrentRepository();

			SessionProviderService sessionProviderService = (SessionProviderService) PortalContainer.getInstance().getComponentInstanceOfType(SessionProviderService.class);
			SessionProvider sessionProvider = sessionProviderService.getSystemSessionProvider(null);
			Session session = sessionProvider.getSession(workspace, repository);

			// Invalidate plidmcache
			PicketLinkIDMCacheService picketLinkIDMCacheService = (PicketLinkIDMCacheService) PortalContainer.getInstance().getComponentInstanceOfType(PicketLinkIDMCacheService.class);
			picketLinkIDMCacheService.invalidateAll();


			PicketLinkIDMService picketLinkIDMService = (PicketLinkIDMService) PortalContainer.getInstance().getComponentInstanceOfType(PicketLinkIDMService.class);
			IdentitySessionImpl identitySession = (IdentitySessionImpl) picketLinkIDMService.getIdentitySession();

			IdentitySessionContext identitySessionContext = identitySession.getSessionContext();
			IdentityStoreRepository identityStoreRepository = identitySessionContext.getIdentityStoreRepository();

			Node parentUsersNode = (Node) session.getItem(path);
			NodeIterator it = parentUsersNode.getNodes();
			String superuser = System.getProperty("exo.super.user") == null ? "root" : System.getProperty("exo.super.user");

			while (it.hasNext()) { // iterate on all social nodes
				Node socNode = it.nextNode();
				String user = socNode.getName().substring(4);

				IdentityObject identity = identityStoreRepository.findIdentityObject(
						identitySessionContext.resolveStoreInvocationContext(), user,
						identitySessionContext.getIdentityObjectTypeMapper().getIdentityObjectType());

				Identity ownerIdentity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME,
						user,false);

				Value[] values = {new BooleanValue(true)};

				String profilePath = path + "/" + socNode.getName() + "/soc:profile";
				Node userNode = (Node) session.getItem(profilePath);
				if ((identity instanceof HibernateIdentityObject && !user.equals(superuser)) || (organizationService.getUserHandler().findUserByName(user) == null )) {
					if (!userNode.hasProperty("void-deleted") || !userNode.getProperty("void-deleted").getValues()[0].getString().equals("true")) {
						// Users mapped only in IDM or Existing only in connections portlet
						try {
							storage.hardDeleteIdentity(ownerIdentity);
						}
						catch (Exception e) {
							LOG.error("An error occurred while deleting user "+user,e.getCause());
						}
						socNode.setProperty("soc:isDeleted", true);
						socNode.save();

						userNode.setProperty("void-deleted", values);
						userNode.save();
						if (organizationService.getUserHandler().findUserByName(user) != null) {
							organizationService.getUserHandler().removeUser(user, true); //Remove user from orgService
							//storage.hardDeleteIdentity(ownerIdentity);
						}
						LOG.info("User " + user + " has been deleted successfully");
					}
				}
			}
		} catch (Exception e) {
			LOG.error(e, e.getCause());
			return "Failed";
		}
		return "Success";
	}

	@GET
	@Path("DeleteOneUser/{username}")
	public String deleteOneUser(@PathParam("username") String username) throws IOException {
		String s = "";

		String workspace = "social";
		String path = "/production/soc:providers/soc:organization/soc:{username}";

		OrganizationService organizationService = (OrganizationService) PortalContainer.getInstance().getComponentInstanceOfType(OrganizationService.class);
		RepositoryService repoService = (RepositoryService) PortalContainer.getInstance()
				.getComponentInstanceOfType(RepositoryService.class);

		IdentityStorageImpl storage = (IdentityStorageImpl) PortalContainer.getInstance().getComponentInstanceOfType(IdentityStorageImpl.class);
		IdentityManager identityManager = (IdentityManager) PortalContainer.getInstance().getComponentInstanceOfType(IdentityManager.class);

		try {
			ManageableRepository repository = repoService.getCurrentRepository();

			SessionProviderService sessionProviderService = (SessionProviderService) PortalContainer.getInstance().getComponentInstanceOfType(SessionProviderService.class);
			SessionProvider sessionProvider = sessionProviderService.getSystemSessionProvider(null);

			Session session = sessionProvider.getSession(workspace, repository);

			PicketLinkIDMService picketLinkIDMService = (PicketLinkIDMService) PortalContainer.getInstance().getComponentInstanceOfType(PicketLinkIDMService.class);
			IdentitySessionImpl identitySession = (IdentitySessionImpl) picketLinkIDMService.getIdentitySession();
			IdentitySessionContext identitySessionContext = identitySession.getSessionContext();
			IdentityStoreRepository identityStoreRepository = identitySessionContext.getIdentityStoreRepository();

			// Invalidate plidmcache
			PicketLinkIDMCacheService picketLinkIDMCacheService = (PicketLinkIDMCacheService) PortalContainer.getInstance().getComponentInstanceOfType(PicketLinkIDMCacheService.class);
			picketLinkIDMCacheService.invalidateAll();

			IdentityObject identity = identityStoreRepository.findIdentityObject(
					identitySessionContext.resolveStoreInvocationContext(), username,
					identitySessionContext.getIdentityObjectTypeMapper().getIdentityObjectType());

			Identity ownerIdentity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME,
					username,false);

			String superuser = System.getProperty("exo.super.user") == null ? "root" : System.getProperty("exo.super.user");

			String socNodePath = path.replace("{username}", username);
			String userNodePath = socNodePath.concat("/soc:profile");
			Node userNode = (Node) session.getItem(userNodePath);
			Value[] values = {new BooleanValue(true)};
			if (identity instanceof LDAPIdentityObjectImpl) { // User is mapped in IDM & LDAP
				LOG.warn("User "+username+" is mapped in both IDM & LDAP !");
				return "WARNING: User "+username+" is mapped in both IDM & LDAP";
			} else {
				if (!username.equals(superuser) && (!userNode.hasProperty("void-deleted") || !userNode.getProperty("void-deleted").getValues()[0].getString().equals("true"))) {
					try {
						storage.hardDeleteIdentity(ownerIdentity);
					}
					catch (Exception e) {
						LOG.error("An error occurred while deleting user "+username,e.getCause());
					}
					Node socNode = (Node) session.getItem(socNodePath);
					//Set the property isDeleted to true
					socNode.setProperty("soc:isDeleted", true);
					socNode.save();

					//Set the property void-deleted to true
					userNode.setProperty("void-deleted", values);
					userNode.save();
					if (organizationService.getUserHandler().findUserByName(username) != null)
						organizationService.getUserHandler().removeUser(username, true); //Remove user from orgService
					LOG.info("User " + username + " has been deleted successfully");
				} else if (!username.equals(superuser)) {
					s = s +"user " + username + " does not exist";
				} else {
					s = s +"user " + username + " is a superuser and should not be deleted";
				}
			}
		} catch (Exception e) {
				LOG.error(e, e.getCause());
				return "Failed";
		}
		if (s.isEmpty()) return "Success";
		else return s;
	}
}