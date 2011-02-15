package ch.systemsx.sybit.crkwebui.client.model;

import com.extjs.gxt.ui.client.data.BaseModel;

public class MyJobsModel extends BaseModel {
	private static final long serialVersionUID = 1L;

	public MyJobsModel() {

	}

	public MyJobsModel(String inputData, String status, String input) 
	{
		set("jobid", inputData);
		set("status", status);
		set("input", input);
	}

	public String getJobid() {
		return (String) get("jobid");
	}

	public String getStatus() {
		return (String) get("status");
	}

	public String getInput() {
		return (String) get("input");
	}
}
