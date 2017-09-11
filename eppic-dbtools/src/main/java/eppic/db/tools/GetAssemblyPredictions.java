package eppic.db.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;

import eppic.EppicParams;
import eppic.TextOutputWriter;
import eppic.model.AssemblyDB;
import eppic.model.AssemblyScoreDB;
import eppic.model.PdbInfoDB;
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
		
		List<String> pdbIds = readListFile(listFile);
		
		System.out.println("Read " + pdbIds.size() + " pdb ids from file " + listFile.toString());
		
		EppicParams params = new EppicParams();
		
		for (String pdbId : pdbIds) {
			
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
			
			em.close();
			
		}
		
		ps.close();
		
		
		
		
	}
	
	private static List<String> readListFile(File listFile) throws IOException {
		List<String> list = new ArrayList<>();
		try (
		BufferedReader br = new BufferedReader(new FileReader(listFile));) {
			String line;
			while ((line = br.readLine())!=null ) {
				if (line.trim().isEmpty()) continue;
				if (line.startsWith("#")) continue;
				
				String[] tokens = line.split("\\s+");
				
				for (String token: tokens) {
					list.add(token);
				}
			}
		}
		
		return list;
		
	}
}
