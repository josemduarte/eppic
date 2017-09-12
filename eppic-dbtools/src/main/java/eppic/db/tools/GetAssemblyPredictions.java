package eppic.db.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;

import eppic.EppicParams;
import eppic.TextOutputWriter;
import eppic.model.AssemblyDB;
import eppic.model.AssemblyScoreDB;
import eppic.model.ChainClusterDB;
import eppic.model.PdbInfoDB;
import eppic.model.SeqClusterDB;
import gnu.getopt.Getopt;

public class GetAssemblyPredictions {


	public static void main(String[] args) throws Exception {

		String help = 
				"Usage: GetAssemblyPredictions \n" +
				" -D <string> : the database name to use\n"+
				" -f <file>   : file with list of PDB ids\n" +
				" -o <file>   : output file to write results\n" +
				" [-g <file> ]: a configuration file containing the database access parameters, if not provided\n" +
				"               the config will be read from file "+DBHandler.DEFAULT_CONFIG_FILE_NAME+" in home dir\n";
		
		String dbName = null;
		File configFile = null;
		
		File listFile = null;
		File outFile = null;
		
		Getopt g = new Getopt("CompareLattices", args, "D:f:o:g:h?");
		int c;
		while ((c = g.getopt()) != -1) {
			switch(c){
			case 'D':
				dbName = g.getOptarg();
				break;
			case 'f':
				listFile = new File(g.getOptarg());
				break;
			case 'o':
				outFile = new File(g.getOptarg());
				break;
			case 'g':
				configFile = new File(g.getOptarg());
				break;
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
		
		if (dbName == null) {
			System.err.println("A database name must be provided with -D");
			System.exit(1);
		}
		
		if (listFile == null) {
			System.err.println("A list file must be provided with -f");
			System.exit(1);
		}
		
		PrintStream ps = null;
		if (outFile==null) {
			ps = System.out;
		} else {
			ps = new PrintStream(outFile);
		}
		
		
		DBHandler dbh = new DBHandler(dbName, configFile);		
		
		List<List<String>> clusters = readListFile(listFile);
		
		System.out.println("Read " + clusters.size() + " clusters from file " + listFile.toString());
		
		EppicParams params = new EppicParams();
		int i = 0;
		for (List<String> cluster : clusters) {
			i++;
			ps.printf("### Group %d - %d entries\n", i, cluster.size()); 

			List<List<SeqClusterDB>> allSeqInfosForCluster = new ArrayList<>();
			for (String pdbId : cluster) {

				EntityManager em = dbh.getEntityManager();

				PdbInfoDB pdbInfo = dbh.deserializePdb(em, pdbId);

				if (pdbInfo==null) {
					System.err.println("Couldn't find data for " + pdbId);
					continue;
				}

				List<AssemblyDB> assemblies = pdbInfo.getAssemblies();

				TextOutputWriter tow = new TextOutputWriter(pdbInfo, params);

				for (AssemblyDB ass : assemblies) {

					for (AssemblyScoreDB assScoreDb : ass.getAssemblyScores()) {
						if (assScoreDb.getMethod().equals("eppic") && assScoreDb.getCallName().equals("bio")) {

							ps.print(pdbId + " ");
							tow.printAssemblyInfo(ps, ass);

						}
					}
				}
				if (pdbInfo!=null) {
					List<SeqClusterDB> seqInfosForEntry = getSeqInfosForEntry(pdbInfo);
					allSeqInfosForCluster.add(seqInfosForEntry);
				}

				//if (pdbInfo!=null)
				//	ps.println(getSeqClustersInfo(pdbInfo));

				em.close();
			}
			ps.print(analyseClusterContents(allSeqInfosForCluster));
			ps.println();
			ps.println();
		}
		
		ps.close();
		
		
		
		
	}
	
	
	@SuppressWarnings("unused")
	private static String getSeqClustersInfo(PdbInfoDB pdbInfo) {
		StringBuilder sb = new StringBuilder();
		for (ChainClusterDB cc : pdbInfo.getChainClusters()) {
			SeqClusterDB sc = cc.getSeqCluster();
			
			sb.append(cc.getRepChain()+" ");
			if (sc!=null)
				sb.append(": "+ sc.getC100() + ", " +sc.getC90() + ", " + sc.getC80() +", "+sc.getC70()+", "+sc.getC60()+", "+sc.getC50()+ " ");
		}
		return sb.toString();
	}
	
	private static List<SeqClusterDB> getSeqInfosForEntry(PdbInfoDB pdbInfo) {
		List<SeqClusterDB> list = new ArrayList<>();
		for (ChainClusterDB cc : pdbInfo.getChainClusters()) {
			SeqClusterDB sc = cc.getSeqCluster();
			
			list.add(sc);
		}
		return list;
	}
	
	private static String analyseClusterContents(List<List<SeqClusterDB>> allSeqInfosForCluster) {
		Map<Integer, Integer> countsPerC50 = new HashMap<>();
		
		int clusterSize = allSeqInfosForCluster.size();
		
		for (List<SeqClusterDB> seqInfosForEntry : allSeqInfosForCluster) {
			for (SeqClusterDB sc : seqInfosForEntry) {
				if (sc==null) continue;
				
				if (!countsPerC50.containsKey(sc.getC50())) {
					countsPerC50.put(sc.getC50(), 1);
				} else {
					countsPerC50.put(sc.getC50(), countsPerC50.get(sc.getC50())+1);
				}
			}
		}
		String contentAnalysis = "Content (50% seq cluster ids): ";
		for (int c50 : countsPerC50.keySet()) {
			contentAnalysis += c50 + "(" + countsPerC50.get(c50) + "/" + clusterSize + ") ";
		}
		return contentAnalysis;
	}
	
	private static List<List<String>> readListFile(File listFile) throws IOException {
		List<List<String>> list = new ArrayList<>();
		try (
		BufferedReader br = new BufferedReader(new FileReader(listFile));) {
			String line;
			while ((line = br.readLine())!=null ) {
				if (line.trim().isEmpty()) continue;
				if (line.startsWith("#")) continue;
				
				String[] tokens = line.split("\\s+");
				List<String> sublist = new ArrayList<>();
				list.add(sublist);
				for (String token: tokens) {
					sublist.add(token);
				}
			}
		}
		
		return list;
		
	}
}
