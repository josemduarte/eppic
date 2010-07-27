package crk;

import gnu.getopt.Getopt;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URL;
import java.net.URLConnection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.zip.GZIPInputStream;

import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.xml.sax.SAXException;

import owl.core.connections.pisa.PisaConnection;
import owl.core.connections.pisa.PisaInterface;
import owl.core.runners.TcoffeeError;
import owl.core.runners.blast.BlastError;
import owl.core.structure.CiffilePdb;
import owl.core.structure.Pdb;
import owl.core.structure.PdbCodeNotFoundError;
import owl.core.structure.PdbLoadError;
import owl.core.structure.AminoAcid;

public class CRKMain {
	
	// CONSTANTS
	private static final String   PROGRAM_NAME = "crk";
	private static final String   CONFIG_FILE_NAME = ".crk.conf";
	private static final String   ENTROPIES_FILE_SUFFIX = ".entropies";
	private static final String   KAKS_FILE_SUFFIX = ".kaks";
	
	// DEFAULTS FOR CONFIG FILE ASSIGNABLE CONSTANTS
	// defaults for pdb data location
	private static final String   DEF_LOCAL_CIF_DIR = "/pdbdata/pdb/data/structures/all/mmCIF";
	private static final String   DEF_PDB_FTP_CIF_URL = "ftp://ftp.wwpdb.org/pub/pdb/data/structures/all/mmCIF/";
	private static final boolean  DEF_USE_ONLINE_PDB = false;

	// defaults for pisa locations
	private static final String   DEF_PISA_INTERFACES_URL = "http://www.ebi.ac.uk/msd-srv/pisa/cgi-bin/interfaces.pisa?";

	// default sifts file location
	private static final String   DEF_SIFTS_FILE = "ftp://ftp.ebi.ac.uk/pub/databases/msd/sifts/text/pdb_chain_uniprot.lst";	
	
	// default blast settings
	private static final String   DEF_BLAST_BIN_DIR = "/usr/bin";
	
	// default tcoffee settings
	private static final File     DEF_TCOFFE_BIN = new File("/usr/bin/t_coffee");

	// default selecton stuff
	private static final File     DEF_SELECTON_BIN = new File("/usr/bin/selecton");
	private static final double	  DEF_SELECTON_EPSILON = 0.1;

	// default crk cutoffs
	private static final double   DEF_QUERY_COVERAGE_CUTOFF = 0.85;
	private static final int      DEF_MIN_HOMOLOGS_CUTOFF = 10;
	private static final double   DEF_CUTOFF_ASA_INTERFACE_REPORTING = 350;
		
	// cutoffs for the final bio/xtal call
	private static final double   DEF_ENTR_BIO_CUTOFF = 0.95;
	private static final double   DEF_ENTR_XTAL_CUTOFF = 1.05;
	private static final double   DEF_KAKS_BIO_CUTOFF = 0.79;
	private static final double   DEF_KAKS_XTAL_CUTOFF = 0.81;
	
	// default cache dirs
	private static final String   DEF_EMBL_CDS_CACHE_DIR = null;
	private static final String   DEF_BLAST_CACHE_DIR = null;

	// DEFAULTS FOR COMMAND LINE PARAMETERS
	private static final double   DEF_IDENTITY_CUTOFF = 0.6;

	private static final int      DEF_BLAST_NUMTHREADS = 1;
	
	// default entropy calculation default
	private static final int      DEF_ENTROPY_ALPHABET = 20;

	// default crk core assignment thresholds
	private static final double   DEF_SOFT_CUTOFF_CA = 0.95;
	private static final double   DEF_HARD_CUTOFF_CA = 0.82;
	private static final double   DEF_RELAX_STEP_CA = 0.01;	
	private static final int      DEF_MIN_NUM_RES_CA = 6;
	private static final int      DEF_MIN_NUM_RES_MEMBER_CA = 3; 

	private static final boolean  DEF_USE_TCOFFEE_VERYFAST_MODE = true;
	
	private static final int      DEF_MAX_NUM_SEQUENCES_SELECTON = 60;
	
	// GLOBAL VARIABLES ASSIGNABLE FROM CONFIG FILE
	private static String   LOCAL_CIF_DIR;
	private static String   PDB_FTP_CIF_URL;
	private static boolean  USE_ONLINE_PDB;
	
	private static String   PISA_INTERFACES_URL;
	
	private static String   SIFTS_FILE;
	
	private static String   BLAST_BIN_DIR;
	
	private static File     TCOFFEE_BIN;
	
	private static File     SELECTON_BIN;

	private static double   QUERY_COVERAGE_CUTOFF;
	private static int      MIN_HOMOLOGS_CUTOFF;
	private static double   CUTOFF_ASA_INTERFACE_REPORTING; 
			
	private static double 	ENTR_BIO_CUTOFF;
	private static double 	ENTR_XTAL_CUTOFF;
	private static double 	KAKS_BIO_CUTOFF;
	private static double 	KAKS_XTAL_CUTOFF;
	
	private static String   EMBL_CDS_CACHE_DIR;
	private static String   BLAST_CACHE_DIR;

	// and finally the ones with no defaults
	private static String   BLAST_DB_DIR; // no default
	private static String   BLAST_DB;     // no default

	
	
	// THE ROOT LOGGER (log4j)
	private static final Logger ROOTLOGGER = Logger.getRootLogger();

	
	/**
	 * 
	 * @param args
	 * @throws SQLException
	 * @throws PdbCodeNotFoundError
	 * @throws PdbLoadError
	 * @throws IOException
	 * @throws BlastError
	 * @throws TcoffeeError
	 * @throws SAXException
	 */
	public static void main(String[] args) throws PdbLoadError, IOException, BlastError, TcoffeeError, SAXException {
		
		String pdbCode = null;
		boolean doScoreCRK = false;
		double idCutoff = DEF_IDENTITY_CUTOFF;
		String baseName = null;
		File outDir = new File(".");
		int blastNumThreads = DEF_BLAST_NUMTHREADS;
		int reducedAlphabet = DEF_ENTROPY_ALPHABET;
		boolean useTcoffeeVeryFastMode = DEF_USE_TCOFFEE_VERYFAST_MODE;
		
		double   softCutoffCA = DEF_SOFT_CUTOFF_CA;
		double   hardCutoffCA = DEF_HARD_CUTOFF_CA;
		double   relaxStepCA  = DEF_RELAX_STEP_CA;
		int      minNumResCA  = DEF_MIN_NUM_RES_CA;
		int      minNumResMemberCA = DEF_MIN_NUM_RES_MEMBER_CA; 
		
		double selectonEpsilon = DEF_SELECTON_EPSILON;

		int maxNumSeqsSelecton = DEF_MAX_NUM_SEQUENCES_SELECTON;

		String help = "Usage: \n" +
		PROGRAM_NAME+"\n" +
		"   -i         :  input PDB code\n" +
		"  [-k]        :  score based on ka/ks ratios as well as on entropies. Much \n" +
		"                 slower, requires running of the selecton external program\n" +
		"  [-d <float>]:  sequence identity cut-off, homologs below this threshold won't\n" +
		"                 be considered, default: "+String.format("%3.1f",DEF_IDENTITY_CUTOFF)+"\n"+
		"  [-a <int>]  :  number of threads for blast. Default: "+DEF_BLAST_NUMTHREADS+"\n"+
		"  [-b <str>]  :  basename for output files. Default: PDB code \n"+
		"  [-o <dir>]  :  output dir, where output files will be written. Default: current\n" +
		"                 dir \n" +
		"  [-r <int>]  :  specify the number of groups of aminoacids (reduced alphabet) to\n" +
		"                 be used for entropy calculations.\n" +
		"                 Valid values are 2, 4, 6, 8, 10, 15 and 20. Default: "+DEF_ENTROPY_ALPHABET+"\n" +
		"  [-t]        :  if specified t_coffee will be run in normal mode instead of very\n" +
		"                 fast mode\n" +
		"  [-c <float>]:  soft BSA cutoff for core assignment. Default: "+String.format("%4.2f", DEF_SOFT_CUTOFF_CA)+"\n" +
		"  [-C <float>]:  hard BSA cutoff for core assignment. Default: "+String.format("%4.2f", DEF_HARD_CUTOFF_CA)+"\n" +
		"  [-x <float>]:  relaxation BSA step in core assignment. Default: "+String.format("%4.2f", DEF_RELAX_STEP_CA)+"\n" +
		"  [-m <int>]  :  cutoff for number of interface core residues, if still below \n" +
		"                 this value after applying hard cutoff then the interface is not\n" +
		"                 scored and considered a crystal contact. Default "+DEF_MIN_NUM_RES_CA+"\n" +
		"  [-M <int>]  :  cutoff for number of interface member core residues, if still \n" +
		"                 below this value after applying hard cutoff then the interface \n" +
		"                 member is not scored and considerd a crystal contact. Default: "+DEF_MIN_NUM_RES_MEMBER_CA+"\n" +
		"  [-e <float>]:  epsilon value for selecton. Default "+String.format("%4.2f",DEF_SELECTON_EPSILON)+"\n" +
		"  [-q <int>]  :  maximum number of sequences to keep for calculation of conservation scores.\n" +
		"                 Default: "+DEF_MAX_NUM_SEQUENCES_SELECTON+". This is especially important when using the -k option,\n" +
		"                 with too many sequences, selecton will run too long (and \n" +
		"                 inaccurately because of ks saturation)\n\n";
		


		Getopt g = new Getopt(PROGRAM_NAME, args, "i:kd:a:b:o:r:tc:C:x:m:M:e:h?");
		int c;
		while ((c = g.getopt()) != -1) {
			switch(c){
			case 'i':
				pdbCode = g.getOptarg();
				break;
			case 'k':
				doScoreCRK = true;
				break;				
			case 'd':
				idCutoff = Double.parseDouble(g.getOptarg());
				break;
			case 'a':
				blastNumThreads = Integer.parseInt(g.getOptarg());
				break;
			case 'b':
				baseName = g.getOptarg();
				break;				
			case 'o':
				outDir = new File(g.getOptarg());
				break;
			case 'r':
				reducedAlphabet = Integer.parseInt(g.getOptarg()); 
				break;
			case 't':
				useTcoffeeVeryFastMode = false;
				break;
			case 'c':
				softCutoffCA = Double.parseDouble(g.getOptarg());
				break;
			case 'C':
				hardCutoffCA = Double.parseDouble(g.getOptarg());
				break;
			case 'x':
				relaxStepCA = Double.parseDouble(g.getOptarg());
				break;
			case 'm':
				minNumResCA = Integer.parseInt(g.getOptarg());
				break;
			case 'M':
				minNumResMemberCA = Integer.parseInt(g.getOptarg());
				break;
			case 'e':
				selectonEpsilon = Double.parseDouble(g.getOptarg());
				break;
			case 'h':
			case '?':
				System.out.println(help);
				System.exit(0);
				break; // getopt() already printed an error
			}
		}
		
		if (pdbCode==null) {
			System.err.println("Missing argument -i");
			System.exit(1);
		}
		
		if (baseName==null) {
			baseName=pdbCode;
		}
		
		if (!AminoAcid.isValidNumGroupsReducedAlphabet(reducedAlphabet)) {
			System.err.println("Invalid number of amino acid groups specified ("+reducedAlphabet+")");
			System.exit(1);
		}

		// turn off jaligner logging (we only use NeedlemanWunschGotoh)
		// (for some reason this doesn't work if condensated into one line, it seems that one needs to instantiate the logger and then call setLevel)
		// (and even weirder, for some reason it doesn't work if you put the code in its own private static method!)
		java.util.logging.Logger jalLogger = java.util.logging.Logger.getLogger("NeedlemanWunschGotoh");
		jalLogger.setLevel(java.util.logging.Level.OFF);
		
		// setting up the file logger for log4j
	    ROOTLOGGER.addAppender(new FileAppender(new PatternLayout("%d{ABSOLUTE} %5p - %m%n"),outDir+"/"+baseName+".log",false));
	    ROOTLOGGER.setLevel(Level.INFO);

		
		loadConfigFile();
		
		
		try {

			// files		
			File cifFile = getCifFile(pdbCode, USE_ONLINE_PDB, outDir);
			Pdb pdb = new CiffilePdb(cifFile);
			String[] chains = pdb.getChains();
			// map of sequences to list of chain codes
			Map<String, List<String>> uniqSequences = new HashMap<String, List<String>>();
			// finding the entities (groups of identical chains)
			for (String chain:chains) {

				pdb.load(chain);
				if (uniqSequences.containsKey(pdb.getSequence())) {
					uniqSequences.get(pdb.getSequence()).add(chain);
				} else {
					List<String> list = new ArrayList<String>();
					list.add(chain);
					uniqSequences.put(pdb.getSequence(),list);
				}		
			}
			String msg = "Unique sequences for "+pdbCode+": ";
			int i = 1;
			for (List<String> entity:uniqSequences.values()) {
				msg+=i+":";
				for (String chain:entity) {
					msg+=" "+chain;
				}
				i++;
			}
			ROOTLOGGER.info(msg);

			Map<String,ChainEvolContext> allChains = new HashMap<String,ChainEvolContext>();
			for (List<String> entity:uniqSequences.values()) {
				String representativeChain = entity.get(0);
				Map<String,Pdb> pdbs = new HashMap<String,Pdb>();
				for (String pdbChainCode:entity) {
					Pdb perChainPdb = new CiffilePdb(cifFile);
					perChainPdb.load(pdbChainCode);
					pdbs.put(pdbChainCode,perChainPdb);
				}
				ChainEvolContext chainEvCont = new ChainEvolContext(pdbs, representativeChain);
				// 1) getting the uniprot ids corresponding to the query (the pdb sequence)
				File emblQueryCacheFile = null;
				if (EMBL_CDS_CACHE_DIR!=null) {
					emblQueryCacheFile = new File(EMBL_CDS_CACHE_DIR,baseName+"."+pdbCode+representativeChain+".query.emblcds.fa");
				}
				chainEvCont.retrieveQueryData(SIFTS_FILE, emblQueryCacheFile);
				if (chainEvCont.getQueryRepCDS()==null) {
					//System.err.println("No CDS good match for query sequence!! Exiting");
					ROOTLOGGER.fatal("No CDS good match for query sequence!! Exiting");
					System.exit(1);
				}
				// 2) getting the homologs and sequence data and creating multiple sequence alignment
				System.out.println("Blasting...");
				File blastCacheFile = null;
				if (BLAST_CACHE_DIR!=null) {
					blastCacheFile = new File(BLAST_CACHE_DIR,baseName+"."+pdbCode+representativeChain+".blast.xml"); 
				}
				chainEvCont.retrieveHomologs(BLAST_BIN_DIR, BLAST_DB_DIR, BLAST_DB, blastNumThreads, idCutoff, QUERY_COVERAGE_CUTOFF, blastCacheFile);

				System.out.println("Retrieving UniprotKB data and EMBL CDS sequences");
				File emblHomsCacheFile = null;
				if (EMBL_CDS_CACHE_DIR!=null) {
					emblHomsCacheFile = new File(EMBL_CDS_CACHE_DIR,baseName+"."+pdbCode+representativeChain+".homologs.emblcds.fa");
				}
				chainEvCont.retrieveHomologsData(emblHomsCacheFile);
				if (doScoreCRK) {
					if (!chainEvCont.isConsistentGeneticCodeType()){
						ROOTLOGGER.fatal("The list of homologs does not have a single genetic code type, can't do CRK analysis on it.");
						System.exit(1);
					}
				}
				// remove redundancy
				chainEvCont.removeRedundancy();

				// skimming so that there's not too many sequences for selecton
				chainEvCont.skimList(maxNumSeqsSelecton);

				// align
				System.out.println("Aligning protein sequences with t_coffee...");
				chainEvCont.align(TCOFFEE_BIN, useTcoffeeVeryFastMode);

				// writing homolog sequences to file
				chainEvCont.writeHomologSeqsToFile(new File(outDir,baseName+"."+pdbCode+representativeChain+".fa"));

				// check the back-translation of CDS to uniprot
				// check whether there we have a good enough CDS for the chain
				if (doScoreCRK) {
					ROOTLOGGER.info("Number of homologs with at least one uniprot CDS mapping: "+chainEvCont.getNumHomologsWithCDS());
					ROOTLOGGER.info("Number of homologs with valid CDS: "+chainEvCont.getNumHomologsWithValidCDS());
				}

				// printing summary to file
				PrintStream log = new PrintStream(new File(outDir,baseName+"."+pdbCode+representativeChain+".log"));
				chainEvCont.printSummary(log);
				log.close();
				// writing the alignment to file
				chainEvCont.writeAlignmentToFile(new File(outDir,baseName+"."+pdbCode+representativeChain+".aln"));
				// writing the nucleotides alignment to file
				if (doScoreCRK) {
					chainEvCont.writeNucleotideAlignmentToFile(new File(outDir,baseName+"."+pdbCode+representativeChain+".cds.aln"));
				}

				// computing entropies
				chainEvCont.computeEntropies(reducedAlphabet);

				// compute ka/ks ratios
				if (doScoreCRK) {
					System.out.println("Running selecton (this will take long)...");
					chainEvCont.computeKaKsRatiosSelecton(SELECTON_BIN, 
							new File(outDir,baseName+"."+pdbCode+representativeChain+".selecton.res"),
							new File(outDir,baseName+"."+pdbCode+representativeChain+".selecton.log"), 
							new File(outDir,baseName+"."+pdbCode+representativeChain+".selecton.tree"),
							new File(outDir,baseName+"."+pdbCode+representativeChain+".selecton.global"),
							selectonEpsilon);
				}

				// writing the conservation scores (entropies/kaks) log file 
				PrintStream conservScoLog = new PrintStream(new File(outDir,baseName+"."+pdbCode+representativeChain+ENTROPIES_FILE_SUFFIX));
				chainEvCont.printConservationScores(conservScoLog, ScoringType.ENTROPY);
				conservScoLog.close();
				if (doScoreCRK) {
					conservScoLog = new PrintStream(new File(outDir,baseName+"."+pdbCode+representativeChain+KAKS_FILE_SUFFIX));
					chainEvCont.printConservationScores(conservScoLog, ScoringType.KAKS);
					conservScoLog.close();				
				}

				for (String chain:entity) {
					allChains.put(chain,chainEvCont);
				}
			}


			// 3) getting PISA interfaces description
			PisaConnection pc = new PisaConnection(PISA_INTERFACES_URL, null, null);
			List<String> pdbCodes = new ArrayList<String>();
			pdbCodes.add(pdbCode);
			List<PisaInterface> interfaces = pc.getInterfacesDescription(pdbCodes).get(pdbCode);
			PrintStream pisaLogPS = new PrintStream(new File(outDir,baseName+".pisa.interfaces"));
			for (PisaInterface pi:interfaces) {
				pisaLogPS.println("Interfaces for "+pdbCode);
				pi.printTabular(pisaLogPS);
			}
			pisaLogPS.close();

			// 4) scoring
			System.out.println("Scores:");
			PrintStream scoreEntrPS = new PrintStream(new File(outDir,baseName+ENTROPIES_FILE_SUFFIX+".scores"));
			printScoringHeaders(System.out);
			printScoringHeaders(scoreEntrPS);
			PrintStream scoreKaksPS = new PrintStream(new File(outDir,baseName+KAKS_FILE_SUFFIX+".scores"));
			printScoringHeaders(scoreKaksPS);

			for (PisaInterface pi:interfaces) {
				if (pi.getInterfaceArea()>CUTOFF_ASA_INTERFACE_REPORTING) {
					ArrayList<ChainEvolContext> chainsEvCs = new ArrayList<ChainEvolContext>();
					chainsEvCs.add(allChains.get(pi.getFirstMolecule().getChainId()));
					chainsEvCs.add(allChains.get(pi.getSecondMolecule().getChainId()));
					InterfaceEvolContext iec = new InterfaceEvolContext(pi, chainsEvCs);

					// entropy scoring
					InterfaceScore scoreNW = iec.scoreEntropy(softCutoffCA, hardCutoffCA, relaxStepCA, minNumResCA, minNumResMemberCA,
							MIN_HOMOLOGS_CUTOFF, false);
					InterfaceScore scoreW = iec.scoreEntropy(softCutoffCA, hardCutoffCA, relaxStepCA, minNumResCA, minNumResMemberCA, 
							MIN_HOMOLOGS_CUTOFF, true);

					printScores(System.out, pi, scoreNW, scoreW, ENTR_BIO_CUTOFF, ENTR_XTAL_CUTOFF);
					printScores(scoreEntrPS, pi, scoreNW, scoreW, ENTR_BIO_CUTOFF, ENTR_XTAL_CUTOFF);

					scoreNW.serialize(new File(outDir,baseName+"."+pi.getId()+ENTROPIES_FILE_SUFFIX+".scoreNW.dat"));
					scoreW.serialize(new File(outDir,baseName+"."+pi.getId()+ENTROPIES_FILE_SUFFIX+".scoreW.dat"));

					// ka/ks scoring
					if (doScoreCRK) {
						scoreNW = iec.scoreKaKs(softCutoffCA, hardCutoffCA, relaxStepCA, minNumResCA, minNumResMemberCA,
								MIN_HOMOLOGS_CUTOFF, false);
						scoreW = iec.scoreKaKs(softCutoffCA, hardCutoffCA, relaxStepCA, minNumResCA, minNumResMemberCA, 
								MIN_HOMOLOGS_CUTOFF, true);

						printScores(System.out, pi, scoreNW, scoreW, KAKS_BIO_CUTOFF, KAKS_XTAL_CUTOFF);
						printScores(scoreKaksPS, pi, scoreNW, scoreW, KAKS_BIO_CUTOFF, KAKS_XTAL_CUTOFF);

						scoreNW.serialize(new File(outDir,baseName+"."+pi.getId()+KAKS_FILE_SUFFIX+".scoreNW.dat"));
						scoreW.serialize(new File(outDir,baseName+"."+pi.getId()+KAKS_FILE_SUFFIX+".scoreW.dat"));					
					}

					// writing out the interface pdb file with conservation scores as b factors (for visualization)
					iec.writePdbFile(new File(outDir, baseName+"."+pi.getId()+ENTROPIES_FILE_SUFFIX+".pdb"), ScoringType.ENTROPY);
					if (doScoreCRK) {
						iec.writePdbFile(new File(outDir, baseName+"."+pi.getId()+KAKS_FILE_SUFFIX+".pdb"), ScoringType.KAKS);
					}
				}

			}
			scoreEntrPS.close();
			scoreKaksPS.close();

		} catch (Exception e) {
			ROOTLOGGER.fatal("Unexpected error. Exiting.");
			ROOTLOGGER.fatal(e.getMessage());
			String stack = "";
			for (StackTraceElement el:e.getStackTrace()) {
				stack+=el.toString();				
			}
			ROOTLOGGER.fatal("Stack trace: ");
			ROOTLOGGER.fatal(stack);
			System.exit(1);
		}
	}
	
	private static void printScoringHeaders(PrintStream ps) {
		ps.printf("%15s\t%6s\t","interface","area");
		InterfaceMemberScore.printRimAndCoreHeader(ps,1);
		ps.print("\t");
		InterfaceMemberScore.printRimAndCoreHeader(ps,2);
		ps.print("\t");
		InterfaceMemberScore.printHeader(ps,1);
		ps.print("\t");
		InterfaceMemberScore.printHeader(ps,2);
		ps.print("\t");
		InterfaceCall.printHeader(ps);
		ps.print("\t");
		InterfaceMemberScore.printHeader(ps,1);
		ps.print("\t");
		InterfaceMemberScore.printHeader(ps,2);
		ps.print("\t");		
		InterfaceCall.printHeader(ps);
		ps.println();
		//ps.printf("%45s\t%45s\n","non-weighted","weighted");		
	}
	
	private static void printScores(PrintStream ps, PisaInterface pi, InterfaceScore scoreNW, InterfaceScore scoreW, double bioCutoff, double xtalCutoff) {
		ps.printf("%15s\t%6.1f",
				pi.getId()+"("+pi.getFirstMolecule().getChainId()+"+"+pi.getSecondMolecule().getChainId()+")",
				pi.getInterfaceArea());
		scoreNW.getMemberScore(0).printRimAndCoreInfo(ps);
		ps.print("\t");
		scoreNW.getMemberScore(1).printRimAndCoreInfo(ps);
		ps.print("\t");
		scoreNW.getMemberScore(0).printTabular(ps);
		ps.print("\t");
		scoreNW.getMemberScore(1).printTabular(ps);
		ps.print("\t");
		scoreNW.getCall(bioCutoff, xtalCutoff).printTabular(ps);
		ps.print("\t");
		scoreW.getMemberScore(0).printTabular(ps);
		ps.print("\t");
		scoreW.getMemberScore(1).printTabular(ps);
		ps.print("\t");
		scoreW.getCall(bioCutoff, xtalCutoff).printTabular(ps);
		ps.println();		
	}
	
	private static File getCifFile(String pdbCode, boolean online, File outDir) {
		File cifFile = new File(outDir,pdbCode + ".cif");
		String gzCifFileName = pdbCode+".cif.gz";
		File gzCifFile = null;
		if (!online) {	
			gzCifFile = new File(LOCAL_CIF_DIR,gzCifFileName);
		} else {
			gzCifFile = new File(outDir, gzCifFileName);
			try {
				System.out.println("Downloading cif file from ftp...");
				// getting gzipped cif file from ftp
				URL url = new URL(PDB_FTP_CIF_URL+gzCifFileName);
				URLConnection urlc = url.openConnection();
				InputStream is = urlc.getInputStream();
				FileOutputStream os = new FileOutputStream(gzCifFile);
				int b;
				while ( (b=is.read())!=-1) {
					os.write(b);
				}
				is.close();
				os.close();
			} catch (IOException e) {
				System.err.println("Couldn't get "+gzCifFileName+" file from ftp.");
				System.err.println(e.getMessage());
				System.exit(1);
			}
		} 

		// unzipping file
		try {
			GZIPInputStream zis = new GZIPInputStream(new FileInputStream(gzCifFile));
			FileOutputStream os = new FileOutputStream(cifFile);
			int b;
			while ( (b=zis.read())!=-1) {
				os.write(b);
			}
			zis.close();
			os.close();
		} catch (IOException e) {
			System.err.println("Couldn't uncompress "+gzCifFile+" file into "+cifFile);
			System.err.println(e.getMessage());
			System.exit(1);
		}
		return cifFile;
	}

	private static Properties loadConfigFile(String fileName) throws FileNotFoundException, IOException {
		Properties p = new Properties();
		p.load(new FileInputStream(fileName));
		return p;
	}
	
	private static void applyUserProperties(Properties p) {

		/* The logic here is: First, take the value from the user config file,
		   if that is not found, keep the variable value unchanged.
		   Note that any value in the user config file that is not being processed here is ignored. 
		*/
		try {
			// variables without defaults
			BLAST_DB_DIR    	= p.getProperty("BLAST_DB_DIR");
			BLAST_DB        	= p.getProperty("BLAST_DB");

			LOCAL_CIF_DIR   	= p.getProperty("LOCAL_CIF_DIR", DEF_LOCAL_CIF_DIR);
			PDB_FTP_CIF_URL 	= p.getProperty("PDB_FTP_URL", DEF_PDB_FTP_CIF_URL);
			USE_ONLINE_PDB  	= Boolean.parseBoolean(p.getProperty("USE_ONLINE_PDB", new Boolean(DEF_USE_ONLINE_PDB).toString()));
			
			PISA_INTERFACES_URL = p.getProperty("PISA_INTERFACES_URL", DEF_PISA_INTERFACES_URL);
			
			SIFTS_FILE          = p.getProperty("SIFTS_FILE", DEF_SIFTS_FILE);
			
			BLAST_BIN_DIR       = p.getProperty("BLAST_BIN_DIR", DEF_BLAST_BIN_DIR);
			
			TCOFFEE_BIN 		= new File(p.getProperty("TCOFFEE_BIN", DEF_TCOFFE_BIN.toString()));
			
			SELECTON_BIN 		= new File(p.getProperty("SELECTON_BIN", DEF_SELECTON_BIN.toString()));

			QUERY_COVERAGE_CUTOFF = Double.parseDouble(p.getProperty("QUERY_COVERAGE_CUTOFF", new Double(DEF_QUERY_COVERAGE_CUTOFF).toString()));
			MIN_HOMOLOGS_CUTOFF = Integer.parseInt(p.getProperty("MIN_HOMOLOGS_CUTOFF", new Integer(DEF_MIN_HOMOLOGS_CUTOFF).toString()));
			CUTOFF_ASA_INTERFACE_REPORTING = Double.parseDouble(p.getProperty("CUTOFF_ASA_INTERFACE_REPORTING", new Double(DEF_CUTOFF_ASA_INTERFACE_REPORTING).toString())); 
					
			ENTR_BIO_CUTOFF     = Double.parseDouble(p.getProperty("ENTR_BIO_CUTOFF",new Double(DEF_ENTR_BIO_CUTOFF).toString()));
			ENTR_XTAL_CUTOFF    = Double.parseDouble(p.getProperty("ENTR_XTAL_CUTOFF",new Double(DEF_ENTR_XTAL_CUTOFF).toString()));
			KAKS_BIO_CUTOFF     = Double.parseDouble(p.getProperty("KAKS_BIO_CUTOFF",new Double(DEF_KAKS_BIO_CUTOFF).toString()));
			KAKS_XTAL_CUTOFF    = Double.parseDouble(p.getProperty("KAKS_XTAL_CUTOFF",new Double(DEF_KAKS_XTAL_CUTOFF).toString()));;
			
			EMBL_CDS_CACHE_DIR  = p.getProperty("EMBL_CDS_CACHE_DIR", DEF_EMBL_CDS_CACHE_DIR);
			BLAST_CACHE_DIR     = p.getProperty("BLAST_CACHE_DIR", DEF_BLAST_CACHE_DIR);

		} catch (NumberFormatException e) {
			System.err.println("A numerical value in the config file was incorrectly specified: "+e.getMessage()+".\n" +
					"Please check the config file.");
			System.exit(1);
		}
	}
		
	private static void loadConfigFile() {
		// loading settings from config file
		File userConfigFile = new File(System.getProperty("user.home"),CONFIG_FILE_NAME);  
		try {
			if (userConfigFile.exists()) {
				ROOTLOGGER.info("Loading user configuration file " + userConfigFile);
				applyUserProperties(loadConfigFile(userConfigFile.getAbsolutePath()));
			}
		} catch (IOException e) {
			System.err.println("Error while reading from config file " + userConfigFile + ": " + e.getMessage());
			System.exit(1);
		}

	}
}
