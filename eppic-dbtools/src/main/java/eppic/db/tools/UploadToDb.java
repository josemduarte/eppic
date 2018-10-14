package eppic.db.tools;

import eppic.model.db.PdbInfoDB;
import gnu.getopt.Getopt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.persistence.EntityManager;


public class UploadToDb {

    private static final Logger logger = LoggerFactory.getLogger(UploadToDb.class);

	// the name of the dir that is the root of the divided dirs (indexed by the pdbCode middle 2-letters) 
	private static final String DIVIDED_ROOT = "divided";
	
	// after this number of entry uploads time statistics will be produced
	private static final int TIME_STATS_EVERY1 = 100;
	private static final int TIME_STATS_EVERY2 = 1000;

	private static DBHandler dbh;
	private static boolean modeNew = true;
	private static boolean modeEverything = false;
	private static boolean modeRemove = false;
	private static String dbName = null;
	private static File configFile = null;
	private static File choosefromFile = null;
	private static int numWorkers = 1;

	public static void main(String[] args) throws ExecutionException, InterruptedException {
		
		String help = 
				"Usage: UploadToDB\n" +
				"  -D <string>  : the database name to use\n"+
				"  -d <dir>     : root directory of eppic output files with subdirectories as PDB codes \n" +
				" [-l]          : if specified subdirs under root dir (-d) are considered to be in PDB divided layout\n"+
				"                 (this affects the behaviour of -d and -f).\n"+
				"                 Default: subdirs under root are taken directly as PDB codes \n"+
				" [-f <file>]   : file specifying a list of PDB codes indicating subdirs of root \n"+
				"                 directory to take (default: uses all subdirs in the root directory) \n" +
				" [-g <file>]   : a configuration file containing the database access parameters, if not provided\n" +
				"                 the config will be read from file "+DBHandler.DEFAULT_CONFIG_FILE_NAME+" in home dir\n" +
				" [-n <int>]    : number of workers. Default 1. \n"+
				" OPERATION MODE\n" +
				" Default operation: only entries not already present in database will be inserted \n"+
				" [-F]          : forces everything chosen to be inserted, deletes previous entries if present\n" +
				" [-r]          : removes the specified entries from database\n";
				

		boolean isDividedLayout = false;
		
		File jobDirectoriesRoot = null;

		Getopt g = new Getopt("UploadToDB", args, "D:d:lf:g:n:Frh?");
		int c;
		while ((c = g.getopt()) != -1) {
			switch(c){
			case 'D':
				dbName = g.getOptarg();
				break;
			case 'd':
				jobDirectoriesRoot = new File(g.getOptarg());
				break;
			case 'l':
				isDividedLayout = true;
				break;
			case 'f':
				choosefromFile = new File(g.getOptarg());
				break;
			case 'g':
				configFile = new File(g.getOptarg());
				break;
			case 'n':
				numWorkers = Integer.parseInt(g.getOptarg());
				break;
			case 'F':
				modeEverything = true;
				modeNew = false;
				modeRemove = false;
				break;
			case 'r':
				modeRemove = true;
				modeNew = false;
				modeEverything = false;
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
		
		if (jobDirectoriesRoot == null || ! jobDirectoriesRoot.isDirectory() ){
			System.err.println("\n\nHey Jim, Output Directory not specified correctly! \n");
			System.err.println(help);
			System.exit(1);
		}
		
		
		//only one of the three should be true
		if(!( ( modeNew && !modeEverything && !modeRemove ) || 
			  (!modeNew &&  modeEverything && !modeRemove ) ||
			  (!modeNew && !modeEverything &&  modeRemove ) ) ){
			System.err.println("\n\nHey Jim, Combinations of MODE -F / -r not acceptable! \n");
			System.err.println(help);
			System.exit(1);
		}
		
		// Get the Directories to be processed
		File[] jobsDirectories;
		if(choosefromFile!=null){
			List<String> pdbCodes = readListFile(choosefromFile);
			List<File> chosenFilesList = new ArrayList<>();
			for (String pdbCode:pdbCodes) {
				chosenFilesList.add(getDirectory(isDividedLayout, jobDirectoriesRoot, pdbCode));
			}
			jobsDirectories = chosenFilesList.toArray(new File[chosenFilesList.size()]);
		}
		else {
			jobsDirectories = listAllDirs(isDividedLayout, jobDirectoriesRoot);
		}
		
		//Print the MODE of usage
		if(modeNew) System.out.println("\n\nMODE SELECTED: Insert New Entries\n");
		if(modeEverything) System.out.println("\n\nMODE SELECTED: Force Insert, which will insert everything in DB\n");
		if(modeRemove) System.out.println("\n\nMODE SELECTED: Remove entries from DB\n");
		
		if (isDividedLayout) {
			System.out.println("Directories under "+jobDirectoriesRoot+" will be considered to have PDB divided layout, i.e. "+jobDirectoriesRoot+File.separatorChar+DIVIDED_ROOT+File.separatorChar+"<PDB-code-middle-2-letters>");
		} else {
			System.out.println("Directories under "+jobDirectoriesRoot+" will be considered to be PDB codes directly, no PDB divided layout will be used. ");
		}

		System.out.println("Will use " + numWorkers + " workers");

		dbh = new DBHandler(dbName, configFile);

		ExecutorService executorService = Executors.newFixedThreadPool(numWorkers);
		List<Future<Stats>> allResults = new ArrayList<>();

		long totalStart = System.currentTimeMillis();
		int setSize = jobsDirectories.length / numWorkers;
		int remainder = jobsDirectories.length % numWorkers;
		int j = 0;
		int offset = 0;

		for (int i=0; i<numWorkers; i++) {
			if (i == numWorkers-1) {
				setSize = setSize + remainder;
			}
			File[] singleSet = new File[setSize];
			int k = 0;
			for (; j<offset + setSize; j++) {
				singleSet[k] = jobsDirectories[j];
				k++;
			}
			offset = j;

			System.out.printf("Proceeding to submit to worker %d, set of size %d\n", i, singleSet.length);

			Future<Stats> future = executorService.submit(() -> loadSet(singleSet));
			allResults.add(future);
		}

		Stats allStats = new Stats();
		for (Future<Stats> future : allResults) {
			Stats singleStats = future.get();
			allStats.add(singleStats);
		}
		
		long totalEnd = System.currentTimeMillis();

		executorService.shutdown();

		while (!executorService.isTerminated()) {

		}
		
		System.out.println("Completed all "+jobsDirectories.length+" entries in "+((totalEnd-totalStart)/1000)+" s");
		if (modeNew) {
			System.out.println("Already present: "+ allStats.countPresent+", uploaded: "+ allStats.countUploaded+", couldn't insert: "+allStats.pdbsWithWarnings.size());
			System.out.println("There were "+ allStats.countErrorJob+" error jobs in "+ allStats.countUploaded+" uploaded entries.");
		}
		if (modeRemove) System.out.println("Removed: "+ allStats.countRemoved);

		if (!allStats.pdbsWithWarnings.isEmpty()) {
			System.out.println("These PDBs had problems while inserting to db: ");
			for (String pdb:allStats.pdbsWithWarnings) {
				System.out.print(pdb+" ");
			}
			System.out.println();
		}

		// make sure we exit with an error state in cases with many failures
		int maxFailuresTolerated = (allStats.countPresent + allStats.countUploaded)/2;
		if (allStats.pdbsWithWarnings.size() > maxFailuresTolerated) {
			System.err.println("Total of "+allStats.pdbsWithWarnings.size()+" failures, more than "+maxFailuresTolerated+" failures. Something must be wrong!");
			System.exit(1);
		}

	}
	
	private static PdbInfoDB readFromSerializedFile(File webuiFile) {
		PdbInfoDB pdbScoreItem = null;
		try {
			ObjectInputStream in = new ObjectInputStream(new FileInputStream(webuiFile));
			pdbScoreItem = (PdbInfoDB)in.readObject();
			in.close();
		} catch (IOException|ClassNotFoundException e) {
			System.err.println("Problem reading serialized file, skipping entry"+webuiFile+". Error: "+e.getMessage());
		}
		// will be null if an exception occurs
		return pdbScoreItem;
	}
	
	private static File[] listAllDirs(boolean isDividedLayout, File rootDir) {
		if (isDividedLayout) {
			File dividedRoot = new File(rootDir,DIVIDED_ROOT);
			
			List<File> allDirs = new ArrayList<File>();
			
			for (File indexDir:dividedRoot.listFiles()) {
				for (File dir:indexDir.listFiles()) {
					allDirs.add(dir);
				}
			}
						
			return allDirs.toArray(new File[allDirs.size()]);
			
		} else {
			return rootDir.listFiles();
		}
	}

	private static File getDirectory(boolean isDividedLayout, File rootDir, String pdbCode) {
		if (isDividedLayout) {		
			String index = pdbCode.substring(1,	3);
			return new File(rootDir, DIVIDED_ROOT+File.separatorChar+index+File.separatorChar+pdbCode);
		} else {			
			return new File(rootDir, pdbCode);
		}
	}
	
	private static List<String> readListFile(File file) {
		
		List<String> pdbCodes = new ArrayList<String>();

		try {
			BufferedReader br = new BufferedReader(new FileReader(file));
			String line;

			while ((line=br.readLine())!=null) {
				if (line.trim().isEmpty()) continue;
				if (line.startsWith("#")) continue; // comments are allowed in file
				pdbCodes.add(line);

			}
			br.close();
		} catch (IOException e) {
			System.err.println("Problem reading list file "+file+", can't continue");
			System.exit(1);
		}
		return pdbCodes;
	}

	private static Stats loadSet(File[] jobsDirectories) {

		// Start the Process
		int i = -1;
		long avgTimeStart1 = 0;
		long avgTimeEnd1 = 0;
		long avgTimeStart2 = 0;
		long avgTimeEnd2 = 0;

		Stats stats = new Stats();


		EntityManager em = dbh.getEntityManager();
		for (File jobDirectory : jobsDirectories) {
			i++;

			//Check if it really is a directory
			if (!jobDirectory.isDirectory()){
				if(choosefromFile!=null)
					logger.warn(jobDirectory.getName()+" specified in list file (-f), " +
							"but directory "+jobDirectory+" is not present, Skipping");
				continue;
			}

			//Check for 4 letter code directories starting with a number
			if (!jobDirectory.getName().matches("^\\d\\w\\w\\w$")){
				logger.info("Dir name doesn't look like a PDB code, skipping directory " + jobDirectory);
				continue;
			}

			long start = System.currentTimeMillis();

			String currentPDB = jobDirectory.getName();

			// Get the PDB-Score Item to be read
			File webuiFile = new File(jobDirectory, currentPDB + ".webui.dat");
			boolean isSerializedFilePresent = webuiFile.isFile();

			try {

			    String msg = "";

				//MODE FORCE
				if (modeEverything) {
					boolean ifRemoved = dbh.removeJob(currentPDB);
					if (ifRemoved) msg = "Found.. Removing and Updating.. ";
					else msg = "Not Found.. Adding.. ";
					if(isSerializedFilePresent) {
						PdbInfoDB pdbScoreItem = readFromSerializedFile(webuiFile);
						// if something goes wrong while reading the file (a warning message is already printed in readFromSerializedFile)
						if (pdbScoreItem == null) continue;

						dbh.persistFinishedJob(em,pdbScoreItem);
					}
					else {
						dbh.persistErrorJob(em, currentPDB);
					}
					//continue;
				}

				//MODE NEW INSERT
				if (modeNew){
					int isPresent = dbh.checkJobExist(currentPDB);
					if(isPresent == 0){ // not present
						msg = "Not Present.. Adding.. ";
						if(isSerializedFilePresent) {
							PdbInfoDB pdbScoreItem = readFromSerializedFile(webuiFile);
							// if something goes wrong while reading the file (a warning message is already printed in readFromSerializedFile)
							if (pdbScoreItem == null) continue;

							dbh.persistFinishedJob(em,pdbScoreItem);
						}
						else {
							dbh.persistErrorJob(em, currentPDB);
							stats.countErrorJob++;
						}
						stats.countUploaded++;
					} else if (isPresent ==2) {
						// already present but as an error, we have to remove and reinsert
						boolean ifRemoved = dbh.removeJob(currentPDB);
						if (ifRemoved) msg = "Found as error.. Removing and Updating.. ";
						else {
							logger.warn("{} Not Found, but there should be an error job. Skipping..", currentPDB);
							continue;
						}

						if(isSerializedFilePresent) {
							PdbInfoDB pdbScoreItem = readFromSerializedFile(webuiFile);
							// if something goes wrong while reading the file (a warning message is already printed in readFromSerializedFile)
							if (pdbScoreItem == null) continue;

							dbh.persistFinishedJob(em,pdbScoreItem);
						}
						else {
							dbh.persistErrorJob(em, currentPDB);
							stats.countErrorJob++;
						}
						stats.countUploaded++;

					} else {
						// already present and is not an error
						stats.countPresent++;
						msg = "Already Present.. Skipping.. ";
					}
					//continue;
				}

				//MODE REMOVE
				if (modeRemove) {
					boolean ifRemoved = dbh.removeJob(currentPDB);
					if (ifRemoved) {
						msg = "Removed.. ";
						stats.countRemoved++;
					}
					else {
						msg = "Not Found.. ";
					}
					//continue;
				}

				long end = System.currentTimeMillis();
                logger.info("{} {} : {} s", currentPDB, msg, ((end-start)/1000));

				if (i%TIME_STATS_EVERY1==0) {
					avgTimeEnd1 = System.currentTimeMillis();

					if (i!=0) // no statistics before starting
                        logger.info("Last "+TIME_STATS_EVERY1+" entries in "+((avgTimeEnd1-avgTimeStart1)/1000)+" s");

					avgTimeStart1 = System.currentTimeMillis();
				}

				if (i%TIME_STATS_EVERY2==0) {
					avgTimeEnd2 = System.currentTimeMillis();

					if (i!=0) // no statistics before starting
						logger.info("Last "+TIME_STATS_EVERY2+" entries in "+((avgTimeEnd2-avgTimeStart2)/1000)+" s");

					avgTimeStart2 = System.currentTimeMillis();
				}


			} catch (Exception e) {
				if (em.getTransaction().isActive()) {
					em.getTransaction().rollback();
				}
				logger.warn("Problems while inserting "+currentPDB+". Error: "+e.getMessage());
				stats.pdbsWithWarnings.add(currentPDB);
			}
		}
		return stats;
	}

	private static class Stats {
		int countPresent = 0;
		int countUploaded = 0;
		int countRemoved = 0;
		int countErrorJob = 0;
		List<String> pdbsWithWarnings = new ArrayList<>();

		public void add(Stats stats) {
			this.countPresent += stats.countPresent;
			this.countUploaded += stats.countUploaded;
			this.countRemoved += stats.countRemoved;
			this.countErrorJob += stats.countErrorJob;
			pdbsWithWarnings.addAll(stats.pdbsWithWarnings);
		}
	}
}
