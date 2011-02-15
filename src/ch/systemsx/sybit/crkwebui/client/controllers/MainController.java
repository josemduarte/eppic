package ch.systemsx.sybit.crkwebui.client.controllers;

import java.util.List;

import model.PDBScoreItem;
import ch.systemsx.sybit.crkwebui.client.gui.InputDataPanel;
import ch.systemsx.sybit.crkwebui.client.gui.MainViewPort;
import ch.systemsx.sybit.crkwebui.client.gui.ResultsPanel;
import ch.systemsx.sybit.crkwebui.client.gui.StatusPanel;
import ch.systemsx.sybit.crkwebui.shared.model.ApplicationSettings;
import ch.systemsx.sybit.crkwebui.shared.model.ProcessingInProgressData;
import ch.systemsx.sybit.crkwebui.shared.model.RunJobData;

import com.extjs.gxt.ui.client.widget.MessageBox;
import com.extjs.gxt.ui.client.widget.Viewport;
import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.http.client.URL;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.RootPanel;

public class MainController 
{
	public static final AppProperties CONSTANTS = (AppProperties) GWT.create(AppProperties.class);

	private MainViewPort mainViewPort;

	private ServiceController serviceController;

	private ApplicationSettings settings;

	private PDBScoreItem pdbScoreItem;

	private String selectedJobId;

	private Timer autoRefreshMyJobs;
	
	private boolean doStatusPanelRefreshing = false;
	
	private int nrOfSubmissions = 0;
	
	private String selectedViewer = "Jmol";

	public MainController(Viewport viewport) 
	{
		mainViewPort = new MainViewPort(this);
		RootPanel.get().add(mainViewPort);
		this.serviceController = new ServiceControllerImpl(this);
	}

	public void test(String testValue) 
	{
		serviceController.test(testValue);
	}
	
	public void loadSettings() 
	{
		serviceController.loadSettings();
	}

	public void displayView(String token)
	{
		if ((token != null) && (token.length() > 3) && (token.startsWith("id"))) 
		{
			selectedJobId = token.substring(3);
			displayResults();
		}
		// else if((token != null) &&
		// (token.length() > 10) &&
		// (token.startsWith("interface")))
		// {
		// String selectedInterface = token.substring(9, token.indexOf("/"));
		// String selectedId = token.substring(token.indexOf("/") + 1);
		// displayResults(selectedId);
		// }
		else
		{
			selectedJobId = "";
			displayInputView();
		}
	}

	public void displayInputView()
	{
		doStatusPanelRefreshing = false;

		InputDataPanel inputDataPanel = new InputDataPanel(this);
		mainViewPort.getCenterPanel().setDisplayPanel(inputDataPanel);
	}

	public void displayResults()
	{
		serviceController.getResultsOfProcessing(selectedJobId);
	}

	public void displayResultView(PDBScoreItem resultData) 
	{
		doStatusPanelRefreshing = false;
		
		ResultsPanel resultsPanel = new ResultsPanel(this, resultData);
		resultsPanel.setResults(resultData);
		mainViewPort.getCenterPanel().setDisplayPanel(resultsPanel);
	}

	public void displayStatusView(ProcessingInProgressData statusData) 
	{
		StatusPanel statusPanel = new StatusPanel(this);
		mainViewPort.getCenterPanel().setDisplayPanel(statusPanel);
		
		statusPanel.fillData(statusData);
		
		if((statusData.getStatus() != null) && (statusData.getStatus().equals("Running")))
		{
			doStatusPanelRefreshing = true;
		}
		else
		{
			doStatusPanelRefreshing = false;
		}
	}
	
	public void getCurrentStatusData()
	{
		serviceController.getCurrentStatusData(selectedJobId);
	}

	public void getJobsForCurrentSession() {
		serviceController.getJobsForCurrentSession();
	}

	public void getInterfaceResidues(int interfaceId) {
		serviceController.getInterfaceResidues(selectedJobId, interfaceId);
	}

	public void setJobs(List<ProcessingInProgressData> statusData) {
		mainViewPort.getMyJobsPanel().setJobs(statusData);
	}

	public void untieJobsFromSession() {
		serviceController.untieJobsFromSession();
	}

	public void setSettings(ApplicationSettings settings) {
		this.settings = settings;
	}

	public ApplicationSettings getSettings() {
		return settings;
	}

	public void setPDBScoreItem(PDBScoreItem pdbScoreItem) {
		this.pdbScoreItem = pdbScoreItem;
	}

	public PDBScoreItem getPdbScoreItem() {
		return pdbScoreItem;
	}

	public MainViewPort getMainViewPort() {
		return mainViewPort;
	}

	public void runJob(RunJobData runJobData) {
		serviceController.runJob(runJobData);
	}
	
	public void killJob(String selectedId) {
		serviceController.killJob(selectedId);
	}

	public void runMyJobsAutoRefresh() 
	{
		getJobsForCurrentSession();
		
		autoRefreshMyJobs = new Timer() 
		{
			public void run() 
			{
				getJobsForCurrentSession();
				
				if((doStatusPanelRefreshing) && 
					(selectedJobId != null) && 
					(!selectedJobId.equals("")))
				{
					getCurrentStatusData();
				}
			}
		};

		autoRefreshMyJobs.scheduleRepeating(10000);
	}

	public String getSelectedJobId() {
		return selectedJobId;
	}

	public void setSelectedJobId(String selectedJobId) {
		this.selectedJobId = selectedJobId;
	}

	public void refreshStatusView(ProcessingInProgressData statusData) 
	{
		if(mainViewPort.getCenterPanel().getDisplayPanel() instanceof StatusPanel)
		{
			StatusPanel statusPanel = (StatusPanel)mainViewPort.getCenterPanel().getDisplayPanel();
			statusPanel.fillData(statusData);
			mainViewPort.getCenterPanel().layout();
		}
	}
	
	public void setNrOfSubmissions(int nrOfSubmissions)
	{
		this.nrOfSubmissions = nrOfSubmissions;
	}
	
	public int getNrOfSubmissions()
	{
		return nrOfSubmissions;
	}

	public void setSelectedViewer(String selectedViewer)
	{
		this.selectedViewer = selectedViewer;
	}
	
	public void runViewer(String interfaceId) 
	{
		if(selectedViewer.equals("Jmol"))
		{
			showJmolViewer(interfaceId);
		}
		else if(selectedViewer.equals("Local"))
		{
			downloadFileFromServer("interface", interfaceId);
		}
		else
		{
			showError("No viewer selected");
		}
	}
	
	public void showJmolViewer(String interfaceNr) 
	{
		String url = GWT.getModuleBaseURL() + "/crkresults/";
		openJmol(url, interfaceNr, selectedJobId);
	}
	
	public native void openJmol(String url, String interfaceNr, String selectedJob) /*-{
		var jmolWindow = window.open(selectedJob + "-" + interfaceNr, "Jmol");
		$wnd.jmolInitialize("resources/jmol");
		$wnd.jmolSetDocument(jmolWindow.document);
		$wnd.jmolApplet(900,'load ' + url + selectedJob + "/null." + interfaceNr + '.rimcore.pdb');
	}-*/;
	
	public void downloadFileFromServer(String type, String id)
	{
		String fileDownloadServletUrl = GWT.getModuleBaseURL() + "/crkresults";
		RequestBuilder builder = new RequestBuilder(RequestBuilder.GET, URL.encode(fileDownloadServletUrl));

		try 
		{
			Request request = builder.sendRequest(null, new RequestCallback() 
			{
				public void onError(Request request, Throwable exception) 
		    	{
					showError("Error during downloading file from server: " + exception.getMessage());
		    	}

		    	public void onResponseReceived(Request request, Response response) 
		    	{
		    		if (200 == response.getStatusCode()) 
		    		{

		    		}
		    		else
		    		{
		    			showError("Could not download file from server: " + response.getStatusText());
		    		}
		    	}
			});
		} 
		catch (RequestException e) 
		{
			showError("Error during downloading file from server: " + e.getMessage());
		}
	}
	
	public void showError(String errorMessage) {
		Window.alert(errorMessage);
	}
	
	public void showMessage(String title, String message)
	{
		MessageBox.info(title, message, null);
	}
}
