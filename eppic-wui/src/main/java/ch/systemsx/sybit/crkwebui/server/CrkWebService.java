package ch.systemsx.sybit.crkwebui.server;

import java.util.HashMap;
import java.util.List;

import eppic.model.dto.ApplicationSettings;
import eppic.model.dto.Residue;
import eppic.model.dto.ResiduesList;
import eppic.model.dto.PDBSearchResult;
import eppic.model.dto.RunJobData;

/**
 * The client side stub for the RPC service.
 *
 * @author srebniak_a
 */
public interface CrkWebService //extends RemoteService
{
    /**
     * Loads initial setttings.
     * @return initial setttings
     * @throws Exception when an asynchronous call fails to complete normally
     */
    public ApplicationSettings loadSettings() throws Exception;

    /**
     * Submits the job to the server.
     * @param runJobData input parameters
     * @return results of job submission
     * @throws Exception when an asynchronous call fails to complete normally
     */
    public String runJob(RunJobData runJobData) throws Exception;

//    /**
//     * Retrieves results of processing for selected job id - the results type depends on the status of the job on the server.
//     * @param jobId identifier of the job
//     * @return status data for the selected job
//     * @throws Exception when an asynchronous call fails to complete normally
//     */
//    public ProcessingData getResultsOfProcessing(String jobId) throws Exception;

//    /**
//     * Retrieves list of all jobs for current session id.
//     * @return list of jobs attached to the current session
//     * @throws Exception when an asynchronous call fails to complete normally
//     */
//    public JobsForSession getJobsForCurrentSession() throws Exception;

    /**
     * Retrieves residues information for selected interface.
     * @param interfaceUid selected interface uid
     * @return residues information
     * @throws Exception when an asynchronous call fails to complete normally
     */
    public HashMap<Integer, List<Residue>> getInterfaceResidues(int interfaceUid) throws Exception;

    /**
     * Kills selected job.
     * @param jobToStop id of the job to remove
     * @return result of stopping
     * @throws Exception when an asynchronous call fails to complete normally
     */
    public String stopJob(String jobToStop) throws Exception;

    /**
     * Unties specified job id from the session of the current user.
     * @param jobToDelete job for which session id is going to be untied
     * @return result of deleting
     * @throws Exception when an asynchronous call fails to complete normally
     */
    public String deleteJob(String jobToDelete) throws Exception;

    /**
     * Unties all the jobs which are attached to the current session.
     * @throws Exception when an asynchronous call fails to complete normally
     */
    public void untieJobsFromSession() throws Exception;

    /**
     * Retrieves all the residues for all interfaces for specified job.
     * @param jobId identifier of the job
     * @return all residues for job
     * @throws Exception when an asynchronous call fails to complete normally
     */
    public ResiduesList getAllResidues(String jobId) throws Exception;

    /**
     * gets a list of pdb having a seq with a particular UniProt Id
     * @param config
     * @param UniProtId
     * @return bean that holds the results
     * @throws Exception when an asynchronous call fails to complete normally
     */
    public List<PDBSearchResult> getListOfPDBsHavingAUniProt(String uniProtId) throws Exception;

    /**
     * gets a list of pdb having a seq with a particular pdb code and chain
     * @param config
     * @param prdbCode
     * @param chain
     * @return bean that holds the results
     * @throws Exception when an asynchronous call fails to complete normally
     */
    List<PDBSearchResult> getListOfPDBs(String pdbCode, String chain) throws Exception;
}