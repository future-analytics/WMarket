package org.fiware.apps.marketplace.controllers.rest.v2;

/*
 * #%L
 * FiwareMarketplace
 * %%
 * Copyright (C) 2012 SAP
 * Copyright (C) 2015 CoNWeT Lab, Universidad Politécnica de Madrid
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 3. Neither the name of copyright holders nor the names of its contributors
 *    may be used to endorse or promote products derived from this software 
 *    without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

import java.net.URI;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Response.Status;

import org.fiware.apps.marketplace.bo.OfferingBo;
import org.fiware.apps.marketplace.exceptions.DescriptionNotFoundException;
import org.fiware.apps.marketplace.exceptions.NotAuthorizedException;
import org.fiware.apps.marketplace.exceptions.OfferingNotFoundException;
import org.fiware.apps.marketplace.exceptions.RatingNotFoundException;
import org.fiware.apps.marketplace.exceptions.StoreNotFoundException;
import org.fiware.apps.marketplace.exceptions.ValidationException;
import org.fiware.apps.marketplace.model.Rating;
import org.fiware.apps.marketplace.model.Ratings;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Path("/api/v2/store/{storeName}/description/{descriptionName}/offering/{offeringName}/rating")
public class OfferingRatingService {
	
	@Autowired private OfferingBo offeringBo;
	
	private static final ErrorUtils ERROR_UTILS = new ErrorUtils(
			LoggerFactory.getLogger(OfferingRatingService.class), "");
	
	@POST
	public Response createRating(
			@Context UriInfo uri,
			@PathParam("storeName") String storeName, 
			@PathParam("descriptionName") String descriptionName,
			@PathParam("offeringName") String offeringName,
			Rating rating) {
		
		Response response;
		
		try {
			
			// When the object is saved in the database, the ID is automatically set
			// so we can use it.
			offeringBo.createRating(storeName, descriptionName, offeringName, rating);
			
			// Generate the URI and return CREATED
			URI newURI = UriBuilder
					.fromUri(uri.getPath())
					.path(new Integer(rating.getId()).toString())
					.build();
			
			response = Response.created(newURI).build();
			
		} catch (NotAuthorizedException ex) {
			response = ERROR_UTILS.notAuthorizedResponse(ex);
		} catch (OfferingNotFoundException | StoreNotFoundException | DescriptionNotFoundException ex) {
			response = ERROR_UTILS.entityNotFoundResponse(ex);
		} catch (ValidationException ex) {
			response = ERROR_UTILS.validationErrorResponse(ex);
		}
		
		return response;
		
	}
	
	@POST
	@Path("{ratingId}")
	public Response updateRating(
			@PathParam("storeName") String storeName, 
			@PathParam("descriptionName") String descriptionName,
			@PathParam("offeringName") String offeringName,
			@PathParam("ratingId") int ratingId,
			Rating rating) {
		
		Response response;
		
		try {
			
			// When the object is saved in the database, the ID is automatically set
			// so we can use it.
			offeringBo.updateRating(storeName, descriptionName, offeringName, ratingId, rating);
			
			response = Response.ok().build();
			
		} catch (NotAuthorizedException ex) {
			response = ERROR_UTILS.notAuthorizedResponse(ex);
		} catch (OfferingNotFoundException | StoreNotFoundException |
				DescriptionNotFoundException | RatingNotFoundException ex) {
			response = ERROR_UTILS.entityNotFoundResponse(ex);
		} catch (ValidationException ex) {
			response = ERROR_UTILS.validationErrorResponse(ex);
		}
		
		return response;
	}
	
	@GET
	public Response getRatings(
			@PathParam("storeName") String storeName, 
			@PathParam("descriptionName") String descriptionName,
			@PathParam("offeringName") String offeringName) {
		
		Response response;
		
		try {
			response = Response.ok().entity(new Ratings(offeringBo.getRatings(
					storeName, descriptionName, offeringName))).build();
		} catch (NotAuthorizedException ex) {
			response = ERROR_UTILS.notAuthorizedResponse(ex);
		} catch (OfferingNotFoundException | StoreNotFoundException | DescriptionNotFoundException ex) {
			response = ERROR_UTILS.entityNotFoundResponse(ex);
		} 
		
		return response;
		
	}
	
	@GET
	@Path("{ratingId}")
	public Response getRating(
			@PathParam("storeName") String storeName, 
			@PathParam("descriptionName") String descriptionName,
			@PathParam("offeringName") String offeringName,
			@PathParam("ratingId") int ratingId) {
		
		Response response;
		
		try {
			response = Response.ok().entity(offeringBo.getRating(
					storeName, descriptionName, offeringName, ratingId)).build();
		} catch (NotAuthorizedException ex) {
			response = ERROR_UTILS.notAuthorizedResponse(ex);
		} catch (OfferingNotFoundException | StoreNotFoundException |
				DescriptionNotFoundException | RatingNotFoundException ex) {
			response = ERROR_UTILS.entityNotFoundResponse(ex);
		} 
		
		return response;
	}
	
	@DELETE
	@Path("{ratingId}")
	public Response deleteRating(			
			@PathParam("storeName") String storeName, 
			@PathParam("descriptionName") String descriptionName,
			@PathParam("offeringName") String offeringName,
			@PathParam("ratingId") int ratingId) {

		Response response;
		
		try {
			offeringBo.deleteRating(storeName, descriptionName, offeringName, ratingId);
			response = Response.status(Status.NO_CONTENT).build();	
		} catch (NotAuthorizedException ex) {
			response = ERROR_UTILS.notAuthorizedResponse(ex);
		} catch (OfferingNotFoundException | StoreNotFoundException |
				DescriptionNotFoundException | RatingNotFoundException ex) {
			response = ERROR_UTILS.entityNotFoundResponse(ex);
		} 
		
		return response;
		
	}

}