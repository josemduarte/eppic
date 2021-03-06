package ch.systemsx.sybit.crkwebui.client.results.gui.grid.util;

import java.util.List;

import ch.systemsx.sybit.crkwebui.client.results.data.AssemblyItemModel;

import com.sencha.gxt.core.client.ValueProvider;
import com.sencha.gxt.widget.core.client.grid.SummaryType;

import eppic.model.ScoringMethod;

/**
 * Gets the final cluster call value (summary) for a method
 * Return Types:
 * -1: Not Found
 * 0: nopred
 * 1: bio
 * 2: xtal
 * @author biyani_n
 *
 */
public class AssemblyMethodSummaryType {

	
	public static final class CoreRimSummaryType implements SummaryType<String, Integer> {

		@Override
		public <M> Integer calculate(List<? extends M> models,
				ValueProvider<? super M, String> vp) {
			AssemblyItemModel representative = (AssemblyItemModel)(models.get(0));
			return getSummaryValue(representative, ScoringMethod.EPPIC_CORERIM);
		}
		
	}
	
	public static final class CoreSurfaceSummaryType implements SummaryType<String, Integer> {

		@Override
		public <M> Integer calculate(List<? extends M> models,
				ValueProvider<? super M, String> vp) {
			AssemblyItemModel representative = (AssemblyItemModel)(models.get(0));
			return getSummaryValue(representative, ScoringMethod.EPPIC_CORESURFACE);
		}
		
	}
	
	public static final class GeometrySummaryType implements SummaryType<String, Integer> {

		@Override
		public <M> Integer calculate(List<? extends M> models,
				ValueProvider<? super M, String> vp) {
			AssemblyItemModel representative = (AssemblyItemModel)(models.get(0));
			return getSummaryValue(representative, ScoringMethod.EPPIC_GEOMETRY);
		}
		
	}
	
	public static final class FinalCallSummaryType implements SummaryType<String, Integer> {

		@Override
		public <M> Integer calculate(List<? extends M> models,
				ValueProvider<? super M, String> vp) {
			AssemblyItemModel representative = (AssemblyItemModel)(models.get(0));
			return getSummaryValue(representative, ScoringMethod.EPPIC_FINAL);
		}
		
	}

	/**
	 * returns the value of the final cluster call in integer:
	 * Return Types:
	 * -2 method not supported
	 * -1: Not Found
	 * 0: nopred
	 * 1: bio
	 * 2: xtal
	 * @param representative of the interfaces of cluster
	 * @param method : core-rim/core-surface/geometry/finalCall
	 * @return
	 */
	private static int getSummaryValue(AssemblyItemModel representative, String method){
		/*if(method.equals(ScoringMethod.EPPIC_CORERIM)){
			return convertCallToInt(representative.getClusterCoreRimCall());
		} else if(method.equals(ScoringMethod.EPPIC_CORESURFACE)){
			return convertCallToInt(representative.getClusterCoreSurfaceCall());
		} else if(method.equals(ScoringMethod.EPPIC_GEOMETRY)){
			return convertCallToInt(representative.getClusterGeometryCall());
		} else if(method.equals(ScoringMethod.EPPIC_FINAL)){
			return convertCallToInt(representative.getClusterFinalCall()) + convertConfidenceToInt(representative.getClusterConfidence());
		} else{
			return -2;
		}*/ //????
		return -2;
	}
	
	@SuppressWarnings("unused")
	private static int convertConfidenceToInt(double clusterConfidence) {
	    if(clusterConfidence > .66)
		return 100;
	    if(clusterConfidence > .33)
		return 10;
	    return 0;
	}

	/**
	 * Converts calls to int
	 * @param callName
	 * @return
	 */
	@SuppressWarnings("unused")
	private static int convertCallToInt(String callName){
		if(callName == null) return -1;
		
		if(callName.equals("nopred")){
			return 0;
		} else if(callName.equals("bio")){
			return 1;
		} else if(callName.equals("xtal")){
			return 2;
		} else{
			return -1;
		}
	}
	
	public static String methodTypeStringFromSummaryType(SummaryType<?, ?> type) {
	    if(type instanceof CoreRimSummaryType)
		return ScoringMethod.EPPIC_CORERIM;
	    if(type instanceof CoreSurfaceSummaryType)
		return ScoringMethod.EPPIC_CORESURFACE;
	    if(type instanceof GeometrySummaryType)
		return ScoringMethod.EPPIC_GEOMETRY;
	    if(type instanceof FinalCallSummaryType)
		return ScoringMethod.EPPIC_FINAL;
	    return "unknown";
	}

}
