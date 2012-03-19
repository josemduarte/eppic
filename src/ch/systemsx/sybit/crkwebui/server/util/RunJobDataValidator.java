package ch.systemsx.sybit.crkwebui.server.util;

import ch.systemsx.sybit.crkwebui.shared.model.InputParameters;

/**
 * Run job data validator.
 * @author AS
 *
 */
public class RunJobDataValidator 
{

	/**
	 * Checks whether input parameters fulfill the requirements.
	 * @param inputParameters input parameters to validate
	 * @throws Exception when values are not valid
	 */
	public static void validateInputParameters(InputParameters inputParameters) throws Exception
	{
		String searchMode = inputParameters.getSearchMode().toLowerCase();
		
		if(!searchMode.matches("^[a-z]+$"))
		{
			throw new Exception("Incorrect format of search mode - only a-z characters are allowed");
		}
	}
	
	/**
	 * Checks whether jobid is alphanumeric value.
	 * @param jobId job id to validate
	 * @throws Exception when job id is not valid
	 */
	public static void validateJobId(String jobId) throws Exception
	{
		if(!jobId.matches("^[A-Za-z0-9]+$"))
		{
			throw new Exception("Incorrect job id format");
		}
	}
	
	/**
	 * Checks whether input is valid pdb code or filename.
	 * @param input input value to validate
	 * @throws Exception when input is not valid
	 */
	public static void validateInput(String input) throws Exception
	{
		String verificationError = verifyFileName(input);
		
		if(verificationError != null)
		{
			throw new Exception(verificationError);
		}
	}
	
	/**
	 * Verifies whether filename is alphanumeric or contains ".", "-", "_".
	 * @param fileName name of the file to validate
	 * @return null if valid, otherwise error string
	 */
	public static String verifyFileName(String fileName) 
	{
		String result = null;
		
		if(!fileName.matches("^[A-Za-z0-9\\.\\-\\_]+$"))
		{
			return "Filename: " + fileName + 
				   " contains not allowed characters. Only the following characters are allowed: A-Z, a-z, 0-9, \".\", \"-\", \"_\"";
		}
		
		return result;
	}
}
