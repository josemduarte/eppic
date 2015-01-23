package eppic.model;

import java.io.Serializable;
import java.util.List;

public class AssemblyDB implements Serializable {
	
	private static final long serialVersionUID = 1L;

	private int uid;
	
	private String pdbCode;
	
	private String method;
	private int mmSize;
	
	private double confidence;
	
	private PdbInfoDB pdbInfo;
	
	private List<InterfaceClusterScoreDB> interfaceClusterScores;

	public AssemblyDB() {
	}

	public String getPdbCode() {
		return pdbCode;
	}

	public void setPdbCode(String pdbCode) {
		this.pdbCode = pdbCode;
	}

	public int getUid() {
		return uid;
	}

	public void setUid(int uid) {
		this.uid = uid;
	}

	public String getMethod() {
		return method;
	}

	public void setMethod(String type) {
		this.method = type;
	}

	public int getMmSize() {
		return mmSize;
	}

	public void setMmSize(int mmSize) {
		this.mmSize = mmSize;
	}

	public double getConfidence() {
		return confidence;
	}
	
	public void setConfidence(double confidence) {
		this.confidence = confidence;
	}
	
	public PdbInfoDB getPdbInfo() {
		return pdbInfo;
	}

	public void setPdbInfo(PdbInfoDB pdbInfo) {
		this.pdbInfo = pdbInfo;
	}

	public List<InterfaceClusterScoreDB> getInterfaceClusterScores() {
		return interfaceClusterScores;
	}

	public void setInterfaceClusterScores(List<InterfaceClusterScoreDB> interfaceClusterScores) {
		this.interfaceClusterScores = interfaceClusterScores;
	}
	
	

}