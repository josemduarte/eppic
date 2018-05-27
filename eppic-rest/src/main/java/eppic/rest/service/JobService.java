package eppic.rest.service;

import java.util.*;

import javax.persistence.PersistenceContext;

import eppic.dtomodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eppic.db.dao.*;
import eppic.db.dao.jpa.*;

/**
 * Servlet used to download results in xml/json format.
 * Adapted to both json or xml by using eclipselink JAXB implementation.
 * @author Nikhil Biyani
 * @author Jose Duarte
 *
 */
@PersistenceContext(name="eppicjpa", unitName="eppicjpa")
public class JobService {

    /**
     * The servlet name, note that the name is defined in the web.xml file.
     */
    public static final String SERVLET_NAME = "dataDownload";

    private static final Logger logger = LoggerFactory.getLogger(JobService.class);



    /**
     * Retrieves pdbInfo item for job.
     * @param jobId identifier of the job
     * @param getInterfaceInfo whether to retrieve interface info or not
     * @param getAssemblyInfo whether to retrieve assembly info or not
     * @param getSeqInfo whether to retrieve sequence info or not
     * @param getResInfo whether to retrieve residue info or not
     * @return pdb info item
     * @throws DaoException when can not retrieve result of the job
     */
    public static PdbInfo getResultData(String jobId,
                                        boolean getInterfaceInfo,
                                        boolean getAssemblyInfo,
                                        boolean getSeqInfo,
                                        boolean getResInfo) throws DaoException
    {
        JobDAO jobDAO = new JobDAOJpa();
        InputWithType input = jobDAO.getInputWithTypeForJob(jobId);

        PDBInfoDAO pdbInfoDAO = new PDBInfoDAOJpa();
        PdbInfo pdbInfo = pdbInfoDAO.getPDBInfo(jobId);
        pdbInfo.setInputType(input.getInputType());
        pdbInfo.setInputName(input.getInputName());


        // retrieving interface clusters data only if requested
        if (getInterfaceInfo) {
            InterfaceClusterDAO clusterDAO = new InterfaceClusterDAOJpa();
            List<InterfaceCluster> clusters = clusterDAO.getInterfaceClustersWithoutInterfaces(pdbInfo.getUid());

            InterfaceDAO interfaceDAO = new InterfaceDAOJpa();

            for (InterfaceCluster cluster : clusters) {

                logger.debug("Getting data for interface cluster uid {}", cluster.getUid());
                List<Interface> interfaceItems;
                if (getResInfo)
                    interfaceItems = interfaceDAO.getInterfacesWithResidues(cluster.getUid());
                else
                    interfaceItems = interfaceDAO.getInterfacesWithScores(cluster.getUid());
                cluster.setInterfaces(interfaceItems);
            }

            pdbInfo.setInterfaceClusters(clusters);
        } else {
            pdbInfo.setInterfaceClusters(null);
        }

        if(getSeqInfo){
            ChainClusterDAO chainClusterDAO = new ChainClusterDAOJpa();
            List<ChainCluster> chainClusters = chainClusterDAO.getChainClusters(pdbInfo.getUid());
            pdbInfo.setChainClusters(chainClusters);
        } else {
            pdbInfo.setChainClusters(null);
        }

        if (getAssemblyInfo) {
            // assemblies info
            AssemblyDAO assemblyDAO = new AssemblyDAOJpa();

            List<Assembly> assemblies = assemblyDAO.getAssemblies(pdbInfo.getUid());

            pdbInfo.setAssemblies(assemblies);
        } else {
            pdbInfo.setAssemblies(null);
        }

        return pdbInfo;
    }

    /**
     * Retrieves assembly data for job.
     * @param jobId identifier of the job
     * @return assembly data corresponding to job id
     * @throws DaoException when can not retrieve result of the job
     */
    public static List<Assembly> getAssemblyData(String jobId) throws DaoException {

        PDBInfoDAO pdbInfoDAO = new PDBInfoDAOJpa();
        PdbInfo pdbInfo = pdbInfoDAO.getPDBInfo(jobId);

        // assemblies info
        AssemblyDAO assemblyDAO = new AssemblyDAOJpa();

        return assemblyDAO.getAssemblies(pdbInfo.getUid());
    }

    /**
     * Retrieves interface cluster data for job.
     * @param jobId identifier of the job
     * @return interface cluster data corresponding to job id
     * @throws DaoException when can not retrieve result of the job
     */
    public static List<InterfaceCluster> getInterfaceClusterData(String jobId) throws DaoException {

        PDBInfoDAO pdbInfoDAO = new PDBInfoDAOJpa();
        PdbInfo pdbInfo = pdbInfoDAO.getPDBInfo(jobId);

        InterfaceClusterDAO clusterDAO = new InterfaceClusterDAOJpa();
        return clusterDAO.getInterfaceClustersWithoutInterfaces(pdbInfo.getUid());
    }

    /**
     * Retrieves interface data for job.
     * @param jobId identifier of the job
     * @return interface data corresponding to job id
     * @throws DaoException when can not retrieve result of the job
     */
    public static List<Interface> getInterfaceData(String jobId) throws DaoException {

        PDBInfoDAO pdbInfoDAO = new PDBInfoDAOJpa();
        PdbInfo pdbInfo = pdbInfoDAO.getPDBInfo(jobId);

        InterfaceDAO interfaceDAO = new InterfaceDAOJpa();
        return interfaceDAO.getAllInterfaces(pdbInfo.getUid());
    }

    /**
     * Retrieves sequence data for job.
     * @param jobId identifier of the job
     * @return sequence data corresponding to job id
     * @throws DaoException when can not retrieve result of the job
     */
    public static List<ChainCluster> getSequenceData(String jobId) throws DaoException {

        PDBInfoDAO pdbInfoDAO = new PDBInfoDAOJpa();
        PdbInfo pdbInfo = pdbInfoDAO.getPDBInfo(jobId);

        ChainClusterDAO chainClusterDAO = new ChainClusterDAOJpa();
        return chainClusterDAO.getChainClusters(pdbInfo.getUid());
    }

    /**
     * Retrieves residue data for job and interface id.
     * @param jobId identifier of the job
     * @param interfId the interface id
     * @return residue data corresponding to job id and interface id
     * @throws DaoException when can not retrieve result of the job
     */
    public static List<Residue> getResidueData(String jobId, int interfId) throws DaoException {

        PDBInfoDAO pdbInfoDAO = new PDBInfoDAOJpa();
        PdbInfo pdbInfo = pdbInfoDAO.getPDBInfo(jobId);

        InterfaceDAO interfaceDAO = new InterfaceDAOJpa();
        Interface interf = interfaceDAO.getInterface(pdbInfo.getUid(), interfId);
        ResidueDAO rdao = new ResidueDAOJpa();
        return rdao.getResiduesForInterface(interf.getUid());
    }
}
