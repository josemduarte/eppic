package ch.systemsx.sybit.crkwebui.server.generators;

import java.io.InputStream;
import java.util.Map;
import java.util.Properties;

import ch.systemsx.sybit.crkwebui.server.parsers.GridPropertiesParser;
import ch.systemsx.sybit.crkwebui.server.parsers.InputParametersParser;
import ch.systemsx.sybit.crkwebui.server.util.FileContentReader;
import ch.systemsx.sybit.crkwebui.shared.exceptions.ParsingException;
import ch.systemsx.sybit.crkwebui.shared.model.ApplicationSettings;

/**
 * Application settings generator.
 */
public class ApplicationSettingsGenerator 
{
	/**
	 * General server settings.
	 */
	private Properties globalProperties;
	
	/**
	 * Creates instance of application settings generator with set general server properties.
	 * @param globalProperties general server properties
	 */
	public ApplicationSettingsGenerator(Properties globalProperties)
	{
		this.globalProperties = globalProperties;
	}
	
	/**
	 * Creates application settings using provided input streams.
	 * @param inputParametersStream input parameters input stream
	 * @param helpPageStream stream containing help page content
	 * @param gridPropertiesInputStream stream containing settings of the grids
	 * @return application settings
	 * @throws ParsingException when can not properly prepare application settings
	 */
	public ApplicationSettings generateApplicationSettings(InputStream inputParametersStream,
														   InputStream helpPageStream,
														   InputStream gridPropertiesInputStream) throws ParsingException
	{
		ApplicationSettings settings = initializeApplicationSettings(inputParametersStream);
		settings.setHelpPageContent(prepareHelpPageContent(helpPageStream));
		settings.setGridProperties(prepareGridProperties(gridPropertiesInputStream));

		boolean useCaptcha = Boolean.parseBoolean(globalProperties.getProperty("use_captcha","false"));
		String captchaPublicKey = globalProperties.getProperty("captcha_public_key");
		int nrOfAllowedSubmissionsWithoutCaptcha = Integer.parseInt(globalProperties.getProperty("nr_of_allowed_submissions_without_captcha"));
		String pdbLinkUrl = globalProperties.getProperty("pdb_link_url");
		String uniprotLinkUrl = globalProperties.getProperty("uniprot_link_url");
		boolean usePrecompiledResults = Boolean.parseBoolean(globalProperties.getProperty("use_precompiled","true"));

		settings.setCaptchaPublicKey(captchaPublicKey);
		settings.setUseCaptcha(useCaptcha);
		settings.setNrOfAllowedSubmissionsWithoutCaptcha(nrOfAllowedSubmissionsWithoutCaptcha);
		settings.setPdbLinkUrl(pdbLinkUrl);
		settings.setUniprotLinkUrl(uniprotLinkUrl);
		settings.setUsePrecompiledResults(usePrecompiledResults);

		settings.setResultsLocation(globalProperties.getProperty("results_location"));
		return settings;
	}
	
	/**
	 * Creates initial application settings.
	 * @param inputParametersStream stream containing input parameters
	 * @return initial application settings
	 * @throws ParsingException when can not properly initialize application settings
	 */
	private ApplicationSettings initializeApplicationSettings(InputStream inputParametersStream) throws ParsingException
	{
		try
		{
			ApplicationSettings settings = InputParametersParser.prepareApplicationSettings(inputParametersStream);
			return settings;
		}
		catch(Throwable t)
		{
			t.printStackTrace();
			throw new ParsingException("Error during preparing input parameters");
		}
	}
	
	/**
	 * Retrieves content of the help page.
	 * @param helpPageStream stream containing content of the help page
	 * @return content of the help page
	 * @throws ParsingException when can not properly prepare content of the help page
	 */
	private String prepareHelpPageContent(InputStream helpPageStream) throws ParsingException
	{
		try
		{
			String helpPageContent = FileContentReader.readContentOfFile(helpPageStream, true);
			return helpPageContent;
		}
		catch(Throwable t)
		{
			t.printStackTrace();
			throw new ParsingException("Error during preparing help page content");
		}
	}
	
	/**
	 * Retrieves settings of the grids.
	 * @param gridPropertiesInputStream stream containing settings of the grids
	 * @return settings of the grids
	 * @throws ParsingException when can not properly prepare settings of the grids
	 */
	private Map<String, String> prepareGridProperties(InputStream gridPropertiesInputStream) throws ParsingException
	{
		try
		{
			Map<String, String> gridProperties = GridPropertiesParser.prepareGridProperties(gridPropertiesInputStream);
			return gridProperties;
		}
		catch(Throwable t)
		{
			t.printStackTrace();
			throw new ParsingException("Error during preparing grid properties");
		}
	}
}
