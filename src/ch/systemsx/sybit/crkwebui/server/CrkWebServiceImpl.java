package ch.systemsx.sybit.crkwebui.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import model.InterfaceResidueItem;
import model.PDBScoreItem;
import model.ProcessingData;

import org.apache.commons.lang.RandomStringUtils;

import ch.systemsx.sybit.crkwebui.client.CrkWebService;
import ch.systemsx.sybit.crkwebui.server.data.EmailData;
import ch.systemsx.sybit.crkwebui.server.util.PDBModelConverter;
import ch.systemsx.sybit.crkwebui.shared.CrkWebException;
import ch.systemsx.sybit.crkwebui.shared.model.ApplicationSettings;
import ch.systemsx.sybit.crkwebui.shared.model.InputParameters;
import ch.systemsx.sybit.crkwebui.shared.model.ProcessingInProgressData;
import ch.systemsx.sybit.crkwebui.shared.model.RunJobData;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

import crk.InterfaceEvolContextList;
import crk.PdbScore;


/**
 * The server side implementation of the RPC service.
 */
@SuppressWarnings("serial")
public class CrkWebServiceImpl extends RemoteServiceServlet implements CrkWebService 
{
	// general server settings
	private Properties properties;

	private String generalTmpDirectoryName;
	private String generalDestinationDirectoryName;

	private String dataSource;

	// list of running  threads
	private CrkThreadGroup runInstances;
	
	private String crkApplicationLocation;

	public void init(ServletConfig config) throws ServletException 
	{
		super.init(config);

		InputStream propertiesStream = getServletContext()
				.getResourceAsStream(
						"/WEB-INF/classes/ch/systemsx/sybit/crkwebui/server/server.properties");

		properties = new Properties();

		try 
		{
			properties.load(propertiesStream);
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
			throw new ServletException("Properties file can not be read");
		}

		generalTmpDirectoryName = properties.getProperty("tmp_path");
		File tmpDir = new File(generalTmpDirectoryName);

		if (!tmpDir.isDirectory()) 
		{
			throw new ServletException(generalTmpDirectoryName + " is not a directory");
		}

		// String realPath =
		// getServletContext().getRealPath(properties.getProperty("destination_path"));
		generalDestinationDirectoryName = properties.getProperty("destination_path");
		File destinationDir = new File(generalDestinationDirectoryName);
		if (!destinationDir.isDirectory()) 
		{
			throw new ServletException(generalDestinationDirectoryName + " is not a directory");
		}
		
		crkApplicationLocation = properties.getProperty("crk_jar");

		if (crkApplicationLocation == null) 
		{
			throw new ServletException("Location of crk application not specified");
		}

		runInstances = new CrkThreadGroup("instances");
		getServletContext().setAttribute("instances", runInstances);

		dataSource = properties.getProperty("data_source");
		DBUtils.setDataSource(dataSource);
	}

	public String greetServer(String input) throws IllegalArgumentException {
		// // Verify that the input is valid.
		// if (!FieldVerifier.isValidName(input)) {
		// // If the input is not valid, throw an IllegalArgumentException back
		// to
		// // the client.
		// throw new IllegalArgumentException(
		// "Name must be at least 4 characters long");
		// }
		//
		// String serverInfo = getServletContext().getServerInfo();
		// String userAgent = getThreadLocalRequest().getHeader("User-Agent");
		//
		// // Escape data from the client to avoid cross-site script
		// vulnerabilities.
		// input = escapeHtml(input);
		// userAgent = escapeHtml(userAgent);
		//
		// return "Hello, " + input + "!<br><br>I am running " + serverInfo
		// + ".<br><br>It looks like you are using:<br>" + userAgent;
		return "";
	}

	/**
	 * Escape an html string. Escaping data received from the client helps to
	 * prevent cross-site script vulnerabilities.
	 * 
	 * @param html
	 *            the html string to escape
	 * @return the escaped string
	 */
	private String escapeHtml(String html) {
		if (html == null) {
			return null;
		}
		return html.replaceAll("&", "&amp;").replaceAll("<", "&lt;")
				.replaceAll(">", "&gt;");
	}
	
	@Override
	public ApplicationSettings getSettings() throws Exception 
	{
		ApplicationSettings settings = new ApplicationSettings();

		InputStream propertiesStream = getServletContext()
				.getResourceAsStream(
						"/WEB-INF/classes/ch/systemsx/sybit/crkwebui/server/grid.properties");

		Properties gridProperties = new Properties();
		
		try
		{
			gridProperties.load(propertiesStream);
		}
		catch(IOException e)
		{
			throw new Exception("Error during loading grid settings: " + e.getMessage());
		}

		Map<String, String> gridPropetiesMap = new HashMap<String, String>();
		for (Object key : gridProperties.keySet())
		{
			gridPropetiesMap.put((String) key, (String) gridProperties.get(key));
		}

		settings.setGridProperties(gridPropetiesMap);

		String supportedMethods = gridProperties.getProperty("supported_methods");

		if (supportedMethods != null) 
		{
			String[] scoringMethods = supportedMethods.split(",");
			settings.setScoresTypes(scoringMethods);
		}
		else
		{
			throw new Exception("Scoring methods not set");
		}

		// default input parameters values
		InputStream defaultInputParametersStream = getServletContext()
				.getResourceAsStream(
						"/WEB-INF/classes/ch/systemsx/sybit/crkwebui/server/input_default_parameters.properties");

		Properties defaultInputParametersProperties = new Properties();

		try 
		{
			defaultInputParametersProperties.load(defaultInputParametersStream);
		}
		catch (IOException e) 
		{
			throw new Exception("Error during reading default values of input parameters");
		}

		InputParameters defaultInputParameters = new InputParameters();

		boolean useTcoffee = Boolean
				.parseBoolean((String) defaultInputParametersProperties
						.get("use_tcoffee"));
		boolean usePisa = Boolean
				.parseBoolean((String) defaultInputParametersProperties
						.get("use_pisa"));
		boolean useNaccess = Boolean
				.parseBoolean((String) defaultInputParametersProperties
						.get("use_naccess"));

		int asaCalc = Integer
				.parseInt((String) defaultInputParametersProperties
						.get("asa_calc"));
		int maxNrOfSequences = Integer
				.parseInt((String) defaultInputParametersProperties
						.get("max_nr_of_sequences"));
		int reducedAlphabet = Integer
				.parseInt((String) defaultInputParametersProperties
						.get("reduced_alphabet"));

		float identityCutoff = Float
				.parseFloat((String) defaultInputParametersProperties
						.get("identity_cutoff"));
		float selecton = Float
				.parseFloat((String) defaultInputParametersProperties
						.get("selecton"));
		
		String defaultMethodsList = defaultInputParametersProperties
			.getProperty("methods");
		String[] defaultMethodsValues = defaultMethodsList.split(",");
		
		defaultInputParameters.setMethods(defaultMethodsValues);

		defaultInputParameters.setUseTCoffee(useTcoffee);
		defaultInputParameters.setUsePISA(usePisa);
		defaultInputParameters.setUseNACCESS(useNaccess);
		defaultInputParameters.setAsaCalc(asaCalc);
		defaultInputParameters.setMaxNrOfSequences(maxNrOfSequences);
		defaultInputParameters.setReducedAlphabet(reducedAlphabet);
		defaultInputParameters.setIdentityCutoff(identityCutoff);
		defaultInputParameters.setSelecton(selecton);

		settings.setDefaultParametersValues(defaultInputParameters);

		String reducedAlphabetList = defaultInputParametersProperties
				.getProperty("reduced_alphabet_list");
		
		if(reducedAlphabetList != null)
		{
			String[] reducedAlphabetValues = reducedAlphabetList.split(",");
	
			List<Integer> reducedAlphabetConverted = new ArrayList<Integer>();
			for (String value : reducedAlphabetValues) 
			{
				reducedAlphabetConverted.add(Integer.parseInt(value));
			}
	
			settings.setReducedAlphabetList(reducedAlphabetConverted);
		}
		
		int nrOfJobsForSession = DBUtils.getNrOfJobsForSessionId(getThreadLocalRequest().getSession().getId());
		settings.setNrOfJobsForSession(nrOfJobsForSession);
		
		boolean useCaptcha = Boolean.parseBoolean(properties.getProperty("use_captcha"));
		String captchaPublicKey = properties.getProperty("captcha_public_key");
		int nrOfAllowedSubmissionsWithoutCaptcha = Integer.parseInt(properties.getProperty("nr_of_allowed_submissions_without_captcha"));
		
		settings.setCaptchaPublicKey(captchaPublicKey);
		settings.setUseCaptcha(useCaptcha);
		settings.setNrOfAllowedSubmissionsWithoutCaptcha(nrOfAllowedSubmissionsWithoutCaptcha);
		
		String resultsLocation = properties.getProperty("results_location");
		settings.setResultsLocation(resultsLocation);
		
		return settings;
	}

	@Override
	public ProcessingData getResultsOfProcessing(String id) throws Exception 
	{
		String status = DBUtils.getStatusForJob(id, getThreadLocalRequest().getSession().getId());

		if(status != null)
		{
			if(status.equals("Finished")) 
			{
				return getResultData(id);
			}
			else 
			{
				return getStatusData(id, status);
			}
		}
		else
		{
			return null;
		}
	}

	private ProcessingInProgressData getStatusData(String id, String status) throws CrkWebException 
	{
		ProcessingInProgressData statusData = null;

		if((id != null) && (!id.equals("")))
		{
			String dataDirectory = generalDestinationDirectoryName + "/" + id;
	
			if (checkIfDirectoryExist(dataDirectory)) 
			{
				statusData = new ProcessingInProgressData();
	
				statusData.setJobId(id);
	
				statusData.setStatus(status);
	
				if (checkIfFileExist(dataDirectory + "/crklog")) 
				{
					try 
					{
						File logFile = new File(dataDirectory + "/crklog");
	
//						FileInputStream inputStream = new FileInputStream(logFile);
//						BufferedInputStream bufferedInputStream = new BufferedInputStream(
//								inputStream);
//						
//	
//						byte[] buffer = new byte[1024];
//						
//						int length = 0;
//	
//						StringBuffer log = new StringBuffer();
//	
//						while ((bufferedInputStream != null)
//								&& ((length = bufferedInputStream.read(buffer)) != -1)) 
//						{
//							log.append(new String(buffer));
//						}
//	
//						bufferedInputStream.close();
//						inputStream.close();
						
						StringBuffer log = new StringBuffer();
						
						FileReader inputStream = new FileReader(logFile);
				        BufferedReader bufferedInputStream = new BufferedReader(inputStream);
				        
				        String line = "";
				        
				        while ((line = bufferedInputStream.readLine()) != null)
				        {
				        	log.append(line + "\n");
				        }

				        bufferedInputStream.close();
				        inputStream.close();
	
				        
						statusData.setLog(log.toString());
					} 
					catch (Exception e) 
					{
						e.printStackTrace();
						throw new CrkWebException(e);
					}
				}
			}
		}

		return statusData;
	}
	
	private PDBScoreItem getResultData(String id) 
	{
		PDBScoreItem resultsData = null;

		if ((id != null) && (id.length() != 0)) 
		{
			File resultFileDirectory = new File(
					generalDestinationDirectoryName + "/" + id);

			if (resultFileDirectory.exists()
					&& resultFileDirectory.isDirectory())
			{
				String[] directoryContent = resultFileDirectory
						.list(new FilenameFilter() {

							public boolean accept(File dir, String name) {
								if (name.endsWith(".scores")) {
									return true;
								} else {
									return false;
								}
							}
						});

				if (directoryContent != null && directoryContent.length > 0) 
				{
					PdbScore[] allPdbScores = null;

					List<PdbScore[]> pdbScores = new ArrayList<PdbScore[]>();

					for (int i = 0; i < directoryContent.length; i++)
					{
						File resultFile = new File(resultFileDirectory + "/"
								+ directoryContent[i]);

						if (resultFile.exists()) 
						{
							try {
								PdbScore[] pdbScoresForMethod = InterfaceEvolContextList
										.parseScoresFile(resultFile);
								pdbScores.add(pdbScoresForMethod);
							} 
							catch (Exception e) {
								
							}
						}
					}

					if (pdbScores.size() > 0) 
					{
						int totalLength = 0;

						for (PdbScore[] array : pdbScores) 
						{
							totalLength += array.length;
						}
						
						allPdbScores = new PdbScore[totalLength];

						int offset = 0;

						for (int i = 0; i < pdbScores.size(); i++) 
						{
							System.arraycopy(pdbScores.get(i), 0, allPdbScores,
									offset, pdbScores.get(i).length);
							offset += pdbScores.get(i).length;
						}
					}

					resultsData = PDBModelConverter.createPDBScoreItem(allPdbScores);

				}
			}
		}

		return resultsData;
	}
	
	public HashMap<Integer, List<InterfaceResidueItem>> getInterfaceResidues(
			String jobId, final int interfaceId) throws CrkWebException
	{
		HashMap<Integer, List<InterfaceResidueItem>> structures = null;
		
		if ((jobId != null) && (jobId.length() != 0)) 
		{
			File resultFileDirectory = new File(
					generalDestinationDirectoryName + "/" + jobId);
			
			String[] directoryContent = resultFileDirectory.list(new FilenameFilter() {

				public boolean accept(File dir, String name) {
					if (name.endsWith("." + interfaceId + ".resDetails.dat")) {
						return true;
					} else {
						return false;
					}
				}
			});

			if (directoryContent != null && directoryContent.length > 0)
			{
				try
				{
					FileInputStream file = new FileInputStream(generalDestinationDirectoryName + "/" + jobId + "/" + directoryContent[0]);
					ObjectInputStream in = new ObjectInputStream(file);
					Object object = in.readObject();
					structures = (HashMap<Integer, List<InterfaceResidueItem>>) object;
					in.close();
					file.close();
				}
				catch(Exception e)
				{
					throw new CrkWebException(e);
				}
			}
		}
		
		return structures;
		
//		HashMap<Integer, List<InterfaceResidueItem>> structures = new HashMap<Integer, List<InterfaceResidueItem>>();
//		for (int j = 1; j < 3; j++)
//		{
//			List<InterfaceResidueItem> residueItems = new ArrayList<InterfaceResidueItem>();
//
//			for (int i = 0; i < 2; i++) 
//			{
//				InterfaceResidueItem residueItem = new InterfaceResidueItem();
//				residueItem.setAsa(20);
//				residueItem.setResidueType("ABC");
//
//				Map<String, InterfaceResidueMethodItem> residueMethodItems = new HashMap<String, InterfaceResidueMethodItem>();
//
//				InterfaceResidueMethodItem residueMethodItem = new InterfaceResidueMethodItem();
//				residueMethodItem.setScore(30);
//
//				residueMethodItems.put("Entropy", residueMethodItem);
//
//				residueItem.setInterfaceResidueMethodItems(residueMethodItems);
//
//				residueItems.add(residueItem);
//			}
//
//			structures.put(j, residueItems);
//		}
//
//		return structures;
	}
	
	@Override
	public String runJob(RunJobData runJobData) throws CrkWebException 
	{
		if (runJobData != null) 
		{
			if(runJobData.getJobId() == null)
			{
				String randomDirectoryName = null;
				boolean isDirectorySet = false;

				while (!isDirectorySet) 
				{
					randomDirectoryName = RandomStringUtils.randomAlphanumeric(30);

					File randomDirectory = new File(randomDirectoryName);

					if (!randomDirectory.exists()) 
					{
						isDirectorySet = true;
					}
				}

				String localDestinationDirName = generalDestinationDirectoryName
						+ "/" + randomDirectoryName;
				File localDestinationDir = new File(localDestinationDirName);
				localDestinationDir.mkdir();
				
				runJobData.setJobId(randomDirectoryName);
			}
			
			EmailData emailData = new EmailData();
			emailData.setEmailSender(properties.getProperty("email_username", ""));
			emailData.setEmailSenderPassword(properties.getProperty("email_password", ""));
			emailData.setHost(properties.getProperty("email_host"));
			emailData.setPort(properties.getProperty("email_port"));
			emailData.setEmailRecipient(runJobData.getEmailAddress());

			String localDestinationDirName = generalDestinationDirectoryName + "/" + runJobData.getJobId();

			EmailSender emailSender = new EmailSender(emailData);

			DBUtils.insertNewJob(runJobData.getJobId(),
					getThreadLocalRequest().getSession().getId(),
					emailData.getEmailRecipient(), runJobData.getFileName());

			String serverHost = properties.getProperty("server_host_page");
			
			CrkRunner crkRunner = new CrkRunner(emailSender,
					runJobData.getFileName(), 
					serverHost + "#id=" + runJobData.getJobId(),
					localDestinationDirName, 
					runJobData.getJobId(),
					runJobData.getInputParameters(),
					crkApplicationLocation);

			Thread crkRunnerThread = new Thread(runInstances, 
					crkRunner,
					runJobData.getJobId());

			File logFile = new File(localDestinationDirName + "/crklog");
			
			try 
			{
				logFile.createNewFile();
			}
			catch(IOException e)
			{
				e.printStackTrace();
			}
				
			crkRunnerThread.start();
			
			return runJobData.getJobId();
		}
		
		return null;
	}

	@Override
	public String killJob(String jobId) throws CrkWebException 
	{
		String result = null;

		int estimatedNrOfCurrentThreads = runInstances.activeCount();
		Thread[] activeInstances = new Thread[estimatedNrOfCurrentThreads];

		int nrOfCurrentThreads = runInstances.enumerate(activeInstances);
		
		while(nrOfCurrentThreads > estimatedNrOfCurrentThreads)
		{
			estimatedNrOfCurrentThreads = nrOfCurrentThreads;
			activeInstances = new Thread[nrOfCurrentThreads];
			nrOfCurrentThreads = runInstances.enumerate(activeInstances);
		}

		if (activeInstances != null)
		{
			int i = 0;
			boolean wasFound = false;

			while ((i < activeInstances.length) && (!wasFound)) 
			{
				if ((activeInstances[i] != null) && (activeInstances[i].getName().equals(jobId))) 
				{
					if(!activeInstances[i].isInterrupted())
					{
						activeInstances[i].interrupt();
					}
					
					wasFound = true;
					result = "Job " + jobId + " stopped";

					File killFile = new File(
							generalDestinationDirectoryName + "/" + jobId
									+ "/crkkilled");
					try 
					{
						killFile.createNewFile();
					} catch (IOException e) {
						e.printStackTrace();
					}

					DBUtils.updateStatusOfJob(jobId, "Stopped");
				}

				i++;
			}

			if (!wasFound) 
			{
				result = "No job " + jobId + " or can not be stopped";
			}
		}

		return result;
	}

	

	public String test(String test) {
		return getThreadLocalRequest().getSession().getId();
	}

	private boolean checkIfDirectoryExist(String directoryName) {
		File directory = new File(directoryName);

		if (directory.exists() && directory.isDirectory()) {
			return true;
		} else {
			return false;
		}
	}

	private boolean checkIfFileExist(String fileName) {
		File file = new File(fileName);

		if (file.exists()) {
			return true;
		} else {
			return false;
		}
	}

	@Override
	public List<ProcessingInProgressData> getJobsForCurrentSession() throws CrkWebException 
	{
		String sessionId = getThreadLocalRequest().getSession().getId();
		return DBUtils.getJobsForCurrentSession(sessionId);
	}

	@Override
	public void untieJobsFromSession() throws CrkWebException 
	{
		String sessionId = getThreadLocalRequest().getSession().getId();
		DBUtils.untieJobsFromSession(sessionId);
	}
	
	@Override
	public void destroy()
	{
		super.destroy();
		
		int estimatedNrOfCurrentThreads = runInstances.activeCount();
		Thread[] activeInstances = new Thread[estimatedNrOfCurrentThreads];

		int nrOfCurrentThreads = runInstances.enumerate(activeInstances);
		
		while(nrOfCurrentThreads > estimatedNrOfCurrentThreads)
		{
			estimatedNrOfCurrentThreads = nrOfCurrentThreads;
			activeInstances = new Thread[nrOfCurrentThreads];
			nrOfCurrentThreads = runInstances.enumerate(activeInstances);
		}
		
		for(Thread activeThread : activeInstances)
		{
			if((activeThread != null) && (!activeThread.isInterrupted()))
			{
				try 
				{
					DBUtils.updateStatusOfJob(activeThread.getName(), "Stopped");
				}
				catch (CrkWebException e) 
				{
					e.printStackTrace();
				}
				
				activeThread.interrupt();
			}
		}
		
//		runInstances.destroy();
	}
}
