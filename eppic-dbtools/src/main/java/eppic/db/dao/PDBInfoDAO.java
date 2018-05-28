package eppic.db.dao;

import eppic.dtomodel.PdbInfo;
import eppic.model.PdbInfoDB;

/**
 * DAO for PDBScore item.
 * @author AS
 *
 */
public interface PDBInfoDAO 
{
	/**
	 * Retrieves pdb info item by job identifier.
	 * @param jobId identifier of the job
	 * @return pdb info item
	 * @throws DaoException when can not retrieve pdb info item for job
	 */
	PdbInfo getPDBInfo(String jobId) throws DaoException;
	
	/**
	 * Persists pdb info item.
	 * @param pdbInfo pdb info item to persist
	 * @throws DaoException when can not insert pdb info item
	 */
	void insertPDBInfo(PdbInfoDB pdbInfo) throws DaoException;
}
