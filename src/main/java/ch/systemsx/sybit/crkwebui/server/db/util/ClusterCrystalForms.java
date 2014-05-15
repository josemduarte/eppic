package ch.systemsx.sybit.crkwebui.server.db.util;

import java.util.List;

import eppic.db.CFCompareMatrix;
import eppic.db.PdbInfoList;
import eppic.db.SeqClusterLevel;
import eppic.model.PdbInfoDB;
import gnu.getopt.Getopt;

public class ClusterCrystalForms {

	private static final SeqClusterLevel DEFAULT_SEQ_CLUSTER_LEVEL = SeqClusterLevel.C50;
	private static final double DEFAULT_MIN_AREA = 10;
	private static final double DEFAULT_CO_CUTOFF = 0.2;

	public static void main(String[] args) throws Exception {

		String help = 
				"Usage: ClusterCrystalForms \n" +
				" -i : c50 id\n"+
				" -c : contact overlap score cutoff\n"+
				" -a : minimum area cutoff\n";
				//" -d : print some debug output\n"; 

		int clusterId = -1;
		
		//boolean debug = false;
		
		double coCutoff = DEFAULT_CO_CUTOFF;
		double minArea = DEFAULT_MIN_AREA;
		SeqClusterLevel seqClusterLevel = DEFAULT_SEQ_CLUSTER_LEVEL;

		Getopt g = new Getopt("ClusterCrystalForms", args, "i:c:a:dh?");
		int c;
		while ((c = g.getopt()) != -1) {
			switch(c){
			case 'i':
				clusterId = Integer.parseInt(g.getOptarg());
				break;
			case 'c':
				coCutoff = Double.parseDouble(g.getOptarg());
				break;
			case 'a':
				minArea = Double.parseDouble(g.getOptarg());
				break;
			//case 'd':
			//	debug = true;
			//	break;
			case 'h':
				System.out.println(help);
				System.exit(0);
				break;
			case '?':
				System.err.println(help);
				System.exit(1);
				break; // getopt() already printed an error
			}
		}
		
		
		if (clusterId<=0) {
			System.err.println("A valid sequence cluster id (c50) has to be provided with -i");
			System.exit(1);
		}
		
		
		DBHandler dbh = new DBHandler();
		
		List<PdbInfoDB> pdbInfoList = dbh.deserializeC50SeqCluster(clusterId);
		
		PdbInfoList pdbList = new PdbInfoList(pdbInfoList);
		System.out.println ("C50 cluster "+clusterId+" ("+pdbList.size()+" members)");
		
		long start = System.currentTimeMillis();
		CFCompareMatrix cfMatrix = pdbList.calcLatticeOverlapMatrix(seqClusterLevel, coCutoff, minArea);
		long end = System.currentTimeMillis();
		
		System.out.printf("%4s","");
		for (int i=0;i<pdbList.size();i++) {
			System.out.printf("%11s\t",pdbList.get(i).getPdbInfo().getPdbCode());
		}
		System.out.println();
		
		for (int i=0;i<cfMatrix.getMatrix().length;i++) {
			System.out.print(pdbList.get(i).getPdbInfo().getPdbCode());
			for (int j=0;j<cfMatrix.getMatrix()[i].length;j++) {
				if (cfMatrix.getMatrix()[i][j]==null) 
					System.out.printf("%11s\t","NA");
				else
					System.out.printf("%5.3f:%5.3f\t",cfMatrix.getMatrix()[i][j].getfAB(),cfMatrix.getMatrix()[i][j].getfBA());
			}
			System.out.println();
		}
		
		System.out.println();
		System.out.println();
		System.out.println("Done in "+((end - start)/1000)+" s");
	}

}
