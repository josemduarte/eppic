package eppic.db.dao;

import java.util.List;

import eppic.dtomodel.Contact;
import eppic.dtomodel.ContactsList;

/**
 * DAO for Contact
 * 
 * @author duarte_j
 *
 */
public interface ContactDAO {

	/**
	 * Retrieves list of contacts for specified interface.
	 * @param interfaceUid uid of interface item
	 * @return list of contacts for specified interface
	 * @throws DaoException when can not retrieve list of contacts
	 */
	List<Contact> getContactsForInterface(int interfaceUid) throws DaoException;
	
	/**
	 * Retrieves list of contacts for all interfaces.
	 * @param jobId identifier of the job
	 * @return list of contacts for all interfaces
	 * @throws DaoException when can not retrieve list of contacts
	 */
	ContactsList getContactsForAllInterfaces(String jobId) throws DaoException;
}

