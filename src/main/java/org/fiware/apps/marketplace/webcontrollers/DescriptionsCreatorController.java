package org.fiware.apps.marketplace.webcontrollers;

import java.net.URI;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import org.fiware.apps.marketplace.bo.DescriptionBo;
import org.fiware.apps.marketplace.bo.StoreBo;
import org.fiware.apps.marketplace.exceptions.NotAuthorizedException;
import org.fiware.apps.marketplace.exceptions.StoreNotFoundException;
import org.fiware.apps.marketplace.exceptions.UserNotFoundException;
import org.fiware.apps.marketplace.exceptions.ValidationException;
import org.fiware.apps.marketplace.model.Description;
import org.fiware.apps.marketplace.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.ui.ModelMap;
import org.springframework.web.servlet.ModelAndView;

@Component
@Path("descriptions")
public class DescriptionsCreatorController extends AbstractController {
	
    @Autowired private DescriptionBo descriptionBo;
    @Autowired private StoreBo storeBo;
    
    private static Logger logger = LoggerFactory.getLogger(DescriptionsCreatorController.class);
    @GET
    @Produces(MediaType.TEXT_HTML)
    @Path("register")
    public Response registerFormView() {

        ModelAndView view;
        ModelMap model = new ModelMap();
        ResponseBuilder builder;

        try {
            User user = getCurrentUser();

            model.addAttribute("user", user);
            model.addAttribute("title", "Upload offerings - " + getContextName());
            model.addAttribute("storeList", storeBo.getAllStores());

            view = new ModelAndView("description.register", model);
            builder = Response.ok();
        } catch (UserNotFoundException e) {
            logger.warn("User not found", e);

            view = buildErrorView(Status.INTERNAL_SERVER_ERROR, e.getMessage());
            builder = Response.serverError();
        } catch (NotAuthorizedException e) {
            logger.info("User unauthorized", e);

            view = buildErrorView(Status.UNAUTHORIZED, e.getMessage());
            builder = Response.status(Status.UNAUTHORIZED);
        }

        return builder.entity(view).build();
    }

    @POST
    @Produces(MediaType.TEXT_HTML)
    @Path("register")
    public Response registerFormView(
            @Context UriInfo uri,
            @Context HttpServletRequest request,
            @FormParam("storeName") String storeName,
            @FormParam("displayName") String displayName,
            @FormParam("url") String url) {

        Description description;
        HttpSession session;
        ModelAndView view;
        ModelMap model = new ModelMap();
        ResponseBuilder builder;
        URI redirectURI;

        try {
            User user = getCurrentUser();

            model.addAttribute("user", user);
            model.addAttribute("title", "Upload offerings - " + getContextName());
            model.addAttribute("storeList", storeBo.getAllStores());

            description = new Description();
            description.setDisplayName(displayName);
            description.setUrl(url);

            descriptionBo.save(storeName, description);

            redirectURI = UriBuilder.fromUri(uri.getBaseUri())
                    .path("stores").path(storeName).path("offerings")
                    .build();

            session = request.getSession();

            synchronized (session) {
                session.setAttribute("flashMessage", "The description '" + displayName + "' was uploaded successfully.");
            }

            builder = Response.seeOther(redirectURI);
        } catch (UserNotFoundException e) {
            logger.warn("User not found", e);

            view = buildErrorView(Status.INTERNAL_SERVER_ERROR, e.getMessage());
            builder = Response.serverError().entity(view);
        } catch (NotAuthorizedException e) {
            logger.info("User unauthorized", e);

            view = buildErrorView(Status.UNAUTHORIZED, e.getMessage());
            builder = Response.status(Status.UNAUTHORIZED).entity(view);
        } catch (ValidationException e) {
            logger.info("A form field is not valid", e);

            model.addAttribute("field_storeName", storeName);
            model.addAttribute("field_displayName", displayName);
            model.addAttribute("field_url", url);

            model.addAttribute("form_error", e);
            view = new ModelAndView("description.register", model);
            builder = Response.status(Status.BAD_REQUEST).entity(view);
        } catch (StoreNotFoundException e) {
            logger.info("Store not found", e);

            view = buildErrorView(Status.NOT_FOUND, e.getMessage());
            builder = Response.status(Status.NOT_FOUND).entity(view);
        }

        return builder.build();
    }

}
