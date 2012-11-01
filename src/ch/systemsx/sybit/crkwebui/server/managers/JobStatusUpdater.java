package ch.systemsx.sybit.crkwebui.server.managers;

import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.List;

import model.PDBScoreItemDB;
import ch.systemsx.sybit.crkwebui.server.EmailSender;
import ch.systemsx.sybit.crkwebui.server.data.JobStatusDetails;
import ch.systemsx.sybit.crkwebui.server.db.dao.JobDAO;
import ch.systemsx.sybit.crkwebui.server.generators.DirectoryContentGenerator;
import ch.systemsx.sybit.crkwebui.server.util.FileContentReader;
import ch.systemsx.sybit.crkwebui.server.util.LogHandler;
import ch.systemsx.sybit.crkwebui.shared.exceptions.DaoException;
import ch.systemsx.sybit.crkwebui.shared.exceptions.DeserializationException;
import ch.systemsx.sybit.crkwebui.shared.exceptions.JobHandlerException;
import ch.systemsx.sybit.crkwebui.shared.model.StatusOfJob;

/**
 * Daemon used to update status of submitted jobs.
 * @author AS
 *
 */
public class JobStatusUpdater implements Runnable
{
	private volatile boolean running;
	private volatile boolean isUpdating;
	private JobManager jobManager;
	private JobDAO jobDAO;
	private String resultsPathUrl;
	private EmailSender emailSender;
	private String generalDestinationDirectoryName;

	public JobStatusUpdater(JobManager jobManager,
							JobDAO jobDAO,
							String resultsPathUrl,
							EmailSender emailSender,
							String generalDestinationDirectoryName)
	{
		this.running = true;
		this.jobManager = jobManager;
		this.jobDAO = jobDAO;
		this.resultsPathUrl = resultsPathUrl;
		this.emailSender = emailSender;
		this.generalDestinationDirectoryName = generalDestinationDirectoryName;
	}

	@Override
	public void run()
	{
		while(running)
		{
			isUpdating = true;
			
			try
			{
				List<JobStatusDetails> unfinishedJobs = jobDAO.getListOfUnfinishedJobs();

				for(JobStatusDetails unfinishedJob : unfinishedJobs)
				{
					try
					{
						StatusOfJob savedStatus = StatusOfJob.getByName(unfinishedJob.getStatus());

						StatusOfJob currentStatus = jobManager.getStatusOfJob(unfinishedJob.getJobId(), unfinishedJob.getSubmissionId());

						if(savedStatus != currentStatus)
						{
							File logFileDirectory = new File(generalDestinationDirectoryName, unfinishedJob.getJobId());
							File logFile = new File(logFileDirectory, "crklog");

							if(currentStatus == StatusOfJob.FINISHED)
							{
								handleJobFinishedSuccessfully(unfinishedJob, logFile);
							}
							else if(currentStatus == StatusOfJob.ERROR)
							{
								handleJobFinishedWithError(unfinishedJob);
							}
							else if(currentStatus == StatusOfJob.RUNNING)
							{
								handleRunningJob(unfinishedJob.getJobId());
							}
							else if(currentStatus == StatusOfJob.WAITING)
							{
								handleWaitingJob(unfinishedJob.getJobId());
							}
							else if(currentStatus == StatusOfJob.STOPPED)
							{
								handleStoppedJob(unfinishedJob.getJobId());
							}
						}
					}
					catch(DeserializationException e)
					{
						handleJobFinishedWithError(unfinishedJob);
					}
					catch (JobHandlerException e)
					{
						e.printStackTrace();
					}
					catch (DaoException e)
					{
						e.printStackTrace();
					}
				}
				
				Thread.sleep(2000);
			}
			catch (Throwable t)
			{
				t.printStackTrace();
			}
			
			isUpdating = false;
		}
	}

	/**
	 * Handles successfully finished job.
	 * @param jobStatusDetails details of the job
	 * @param logFile file where job execution log is stored
	 * @throws DaoException when can not appropriately handle results of the job
	 * @throws DeserializationException when can not deserialize results file
	 */
	private void handleJobFinishedSuccessfully(JobStatusDetails jobStatusDetails,
											   File logFile) throws DaoException, DeserializationException
	{
		String webuiFileName = jobStatusDetails.getInput();

	    if(webuiFileName.contains("."))
	    {
	    	webuiFileName = webuiFileName.substring(0, webuiFileName.lastIndexOf("."));
	    }

	    webuiFileName += ".webui.dat";

	    File resultsDirectory = new File(generalDestinationDirectoryName, jobStatusDetails.getJobId());
	    File resultsFile = new File(resultsDirectory, webuiFileName);
		PDBScoreItemDB pdbScoreItem = retrieveResult(resultsFile);
		jobDAO.setPdbScoreItemForJob(jobStatusDetails.getJobId(), pdbScoreItem);

		LogHandler.writeToLogFile(logFile, "Processing finished\n");

		String message = jobStatusDetails.getInput() +
				  		 " processing finished. To see the results please go to: " +
				  		 resultsPathUrl + "#id=" + jobStatusDetails.getJobId();

		emailSender.send(jobStatusDetails.getEmailAddress(),
						 "EPPIC: " + jobStatusDetails.getInput() + " processing finished",
						 message);
	}

	/**
	 * Handles job which finished with error status.
	 * @param jobStatusDetails details of the job
	 * @throws DaoException when can not appropriately handle results of the job
	 */
	private void handleJobFinishedWithError(JobStatusDetails jobStatusDetails) throws DaoException
	{
		File jobDirectory = new File(generalDestinationDirectoryName, jobStatusDetails.getJobId());
		
		File directoryContent[] = DirectoryContentGenerator.getFilesNamesWithPrefix(jobDirectory,
																					jobStatusDetails.getJobId() + ".e");
		File errorLogFile = null;
		if((directoryContent != null) && (directoryContent.length > 0))
		{
			errorLogFile = directoryContent[0];
		}
		
		String message = jobStatusDetails.getInput() + " - error while processing the data.\n\n";

		if(errorLogFile != null)
		{
			try
			{
				message += FileContentReader.readContentOfFile(errorLogFile, true);
			}
			catch(Throwable t)
	        {
	        	t.printStackTrace();
	        }
		}

		jobDAO.updateStatusOfJob(jobStatusDetails.getJobId(), StatusOfJob.ERROR.getName());
		
		emailSender.send(jobStatusDetails.getEmailAddress(),
						 "EPPIC: " + jobStatusDetails.getInput() + ", error while processing",
						 message + "\n\n" + resultsPathUrl + "#id=" + jobStatusDetails.getJobId());
	}

	/**
	 * Handles job with running status.
	 * @param jobId identifier of the job
	 * @throws DaoException when can not appropriately handle running job
	 */
	private void handleRunningJob(String jobId) throws DaoException
	{
		jobDAO.updateStatusOfJob(jobId, StatusOfJob.RUNNING.getName());
	}
	
	/**
	 * Handles job with waiting status.
	 * @param jobId identifier of the job
	 * @throws DaoException when can not appropriately handle waiting job
	 */
	private void handleWaitingJob(String jobId) throws DaoException
	{
		jobDAO.updateStatusOfJob(jobId, StatusOfJob.WAITING.getName());
	}
	
	/**
	 * Handles job with stopped status.
	 * @param jobId identifier of the job
	 * @throws DaoException when can not appropriately handle stopped job
	 */
	private void handleStoppedJob(String jobId) throws DaoException
	{
		jobDAO.updateStatusOfJob(jobId, StatusOfJob.STOPPED.getName());
	}


	/**
	 * Retrieves result of processing from specified file.
	 * @param resultFileName file containing result of processing
	 * @return pdb score item
	 * @throws DeserializationException when can not retrieve result from specified file
	 */
	private PDBScoreItemDB retrieveResult(File resultFile) throws DeserializationException
	{
		PDBScoreItemDB pdbScoreItem = null;

		if (resultFile.exists())
		{
			FileInputStream fileInputStream = null;
			ObjectInputStream inputStream = null;

			try
			{
				fileInputStream = new FileInputStream(resultFile);
				inputStream = new ObjectInputStream(fileInputStream);
				pdbScoreItem = (PDBScoreItemDB)inputStream.readObject();
			}
			catch (Throwable e)
			{
				e.printStackTrace();
				throw new DeserializationException(e);
			}
			finally
			{
				if(inputStream != null)
				{
					try
					{
						inputStream.close();
					}
					catch(Throwable t)
					{
						t.printStackTrace();
					}
				}
			}
		}
		else
		{
			throw new DeserializationException("WebUI dat file can not be found");
		}

		return pdbScoreItem;
	}
	
	public void setRunning(boolean running)
	{
		this.running = running;
	}

	/**
	 * Retrieves information whether daemon is currently updating statuses of the jobs.
	 * @return information whether daemon is currently updating statuses of the jobs
	 */
	public boolean isUpdating() {
		return isUpdating;
	}
}
