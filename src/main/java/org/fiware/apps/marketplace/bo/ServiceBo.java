package org.fiware.apps.marketplace.bo;

import org.fiware.apps.marketplace.model.Service;

public interface ServiceBo {
	void save(Service service);
	void update(Service service);
	void delete(Service service);
	Service findByName(String name);
	Service findByNameAndStore(String name, String store);
	Service findById(Integer id);
}
