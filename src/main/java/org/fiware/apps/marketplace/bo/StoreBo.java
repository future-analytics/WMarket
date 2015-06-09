package org.fiware.apps.marketplace.bo;

/*
 * #%L
 * FiwareMarketplace
 * %%
 * Copyright (C) 2012 SAP
 * Copyright (C) 2014-2015 CoNWeT Lab, Universidad Politécnica de Madrid
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

import java.util.List;

import org.fiware.apps.marketplace.exceptions.NotAuthorizedException;
import org.fiware.apps.marketplace.exceptions.RatingNotFoundException;
import org.fiware.apps.marketplace.exceptions.StoreNotFoundException;
import org.fiware.apps.marketplace.exceptions.ValidationException;
import org.fiware.apps.marketplace.model.Rating;
import org.fiware.apps.marketplace.model.Store;


public interface StoreBo {
	
	// Save, update, delete
	public void save(Store store) throws NotAuthorizedException, 
			ValidationException;
	public void update(String name, Store updatedStore) throws NotAuthorizedException,
			ValidationException, StoreNotFoundException;
	public void delete(String storeName) throws NotAuthorizedException, StoreNotFoundException;
	
	// Find by name
	public Store findByName(String name) throws NotAuthorizedException, 
			StoreNotFoundException;
	
	// Get all or a sublist
	public List <Store> getAllStores() throws NotAuthorizedException;
	public List<Store> getStoresPage(int offset, int max)
			throws NotAuthorizedException;
	
	// Rating
	public void createRating(String name, Rating rating) throws NotAuthorizedException, 
			StoreNotFoundException, ValidationException;
	public void updateRating(String name, int ratingId, Rating rating) throws NotAuthorizedException, 
			StoreNotFoundException, RatingNotFoundException, ValidationException;
	public List<Rating> getRatings(String name) throws NotAuthorizedException, StoreNotFoundException;
	public Rating getRating(String name, int ratingId) throws NotAuthorizedException, StoreNotFoundException,
			RatingNotFoundException;
	public void deleteRating(String name, int ratingId) throws NotAuthorizedException, StoreNotFoundException,
			RatingNotFoundException;
	
}
