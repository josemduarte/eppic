package eppic.analysis;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.xml.sax.SAXException;

import eppic.CallType;
import eppic.ScoringType;
import eppic.commons.pisa.OligomericPrediction;
import eppic.commons.pisa.PisaAsmSetList;
import eppic.commons.pisa.PisaAssembliesXMLParser;
import eppic.commons.pisa.PisaConnection;
import eppic.commons.pisa.PisaInterfaceList;
import gnu.getopt.Getopt;



public class CalcPisaInterfPredStats {

	private static final String PROGRAM_NAME = "CalcPisaInterfPredStats";
	
	private static PisaConnection pc;
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {

		File bioList = null;
		File xtalList = null;
		
		String help = "Usage: \n" +
		PROGRAM_NAME+"\n" +
		"   -b         :  list file containing all the pdbIds + interface serials to \n" +
		"                 analyse that are known to be true bio contacts\n" +
		"   -x         :  list file containing all the pdbIds + interface serials to \n" +
		"                 analyse that are known to be true xtal contacts\n\n";
		
		Getopt g = new Getopt(PROGRAM_NAME, args, "b:x:h?");
		int c;
		while ((c = g.getopt()) != -1) {
			switch(c){
			case 'b':
				bioList = new File(g.getOptarg());
				break;
			case 'x':
				xtalList = new File(g.getOptarg());
				break;
			case 'h':
			case '?':
				System.out.println(help);
				System.exit(0);
				break; // getopt() already printed an error
			}
		}
		
		if (bioList==null && xtalList==null) {
			System.err.println("Must specify at least a bio list or a xtal list");
			System.exit(1);
		}
		
		
		pc = new PisaConnection();
		
		if (bioList!=null) {
			TreeMap<String,List<Integer>> bioToAnalyse = Utils.readListFile(bioList);
			PredictionStatsSet bioPred = doPreds(bioToAnalyse, CallType.BIO);
			System.out.println("Bio set: ");
			PredictionStatsSet.printHeader(System.out);
			bioPred.print(System.out);
			
		}
		
		if (xtalList!=null) {
			TreeMap<String,List<Integer>> xtalToAnalyse = Utils.readListFile(xtalList);
			PredictionStatsSet xtalPred = doPreds(xtalToAnalyse, CallType.CRYSTAL);
			System.out.println("Xtal set: ");
			PredictionStatsSet.printHeader(System.out);
			xtalPred.print(System.out);
		}
		
		
		
	}
	
	private static PredictionStatsSet doPreds(TreeMap<String,List<Integer>> interfList, CallType truth) throws IOException, SAXException {
		List<String> pdbCodes = new ArrayList<String>();
		pdbCodes.addAll(interfList.keySet());
		
		Map<String, PisaAsmSetList> asms = pc.getAssembliesDescription(pdbCodes, PisaAssembliesXMLParser.VERSION1);
		Map<String, PisaInterfaceList> interfs = pc.getInterfacesDescription(pdbCodes);

		int countBioCalls = 0;
		int countXtalCalls = 0;
		int total = 0;
		
		for (String pdbCode:pdbCodes) {
			
			PisaAsmSetList pal = asms.get(pdbCode);
			PisaInterfaceList pil = interfs.get(pdbCode);
			
			OligomericPrediction op = pal.getOligomericPred(pil);
			
			// we first report about oligomeric prediction
			System.out.println(pdbCode+"\t"+op.getMmSize());

			if (op.getMmSize()==-1) {
				// if gray prediction all given interfaces are not predicted
				for (int interfId:interfList.get(pdbCode)) {
					System.out.println("  "+interfId+" gray");
					total++;
				}
				continue;
			}			
			
			if (op.getMmSize()==1) {
				// if assembly monomeric all given interfaces are predicted crystal
				for (int interfId:interfList.get(pdbCode)) {
					System.out.println("  "+interfId+" xtal");
					countXtalCalls++;
					total++;
				}
				continue;
			}
			
			// now we report about interfaces
			for (int interfId:interfList.get(pdbCode)) {
				int id = pil.getByProtProtId(interfId).getId();
				if (op.containProtInterface(id)) {
					System.out.println("  "+interfId+" bio");
					countBioCalls++;
				} else {
					System.out.println("  "+interfId+" xtal");
					countXtalCalls++;
				}
				
				total++;
			}
			
		}
		
		String[] taxons = {"Unkown"};
		PredCounter counter = new PredCounter(taxons);
		counter.setTotal(total);
		counter.setCountBio(taxons[0], countBioCalls);
		counter.setCountXtal(taxons[0], countXtalCalls);
		
		return new PredictionStatsSet("PISA", truth, ScoringType.PISA, "n/a", "n/a", counter);
	}

}
