package analysis;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import owl.core.runners.blast.BlastException;
import owl.core.runners.blast.BlastRunner;
import owl.core.sequence.Sequence;
import owl.core.structure.PdbAsymUnit;
import owl.core.structure.PdbLoadException;
import owl.core.util.FileFormatException;

/**
 * Script to find sequence redundant entries in a list of PDB codes
 * 
 * @author duarte_j
 *
 */
public class FindRedundantEntries {

	private static final int NTHREADS = 1;
	
	private static final String LOCAL_CIF_DIR = new File(System.getProperty("user.home"),"cifrepo").getAbsolutePath();
	private static final String BASENAME = "find_redundant_entries";
	private static final String TMPDIR = System.getProperty("java.io.tmpdir");

	private static final String BLAST_BIN_DIR = "/home/duarte_j/bin";
	private static final String BLAST_DATA_DIR = "/nfs/data/software/packages/blast-2.2.18/data";
	
	private static final int[] ID_CUTOFFS =  {95,90,80,70,60,50};
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {

		//int numThreads = NTHREADS;
		
		if (args.length<1) {
			System.err.println("Usage: FindRedundantEntries <list files>");
			System.exit(1);
		}		
		File[] listFiles = new File[args.length];
		for (int i=0;i<args.length;i++) {
			listFiles[i] = new File(args[i]);
		}				
		
		int[] listSizes = new int[listFiles.length];
		
		Map<String,Integer> pdbCodes = new TreeMap<String,Integer>();
		int countIdentical = 0;
		
		for (int i=0;i<listFiles.length;i++) {
			TreeMap<String,List<Integer>> list = Utils.readListFile(listFiles[i]);	
			listSizes[i] = list.size();
			
			for (String pdbCode:list.keySet()) {
				Integer previous = pdbCodes.put(pdbCode,i+1);
				if (previous!=null) {
					countIdentical++;
					System.err.println("Warning! code "+pdbCode+" from list "+(i+1)+" was already present in list "+previous);
				}
			}
			
			
		}
		
		File fastaTmpFile = File.createTempFile(BASENAME, ".fasta");
		
		System.out.println("Total of "+pdbCodes.size()+" unique PDB entries");
		for (int i=0;i<listFiles.length;i++) {
			System.out.println("File "+(i+1)+": "+listFiles[i]+" ("+listSizes[i]+" entries)");
		}
		System.out.println(countIdentical+" identical entries in lists");
		
		System.out.println("Writing unique sequences to fasta file");
		writeFastaFile(pdbCodes.keySet(), fastaTmpFile);
		
		doClustering(fastaTmpFile, ID_CUTOFFS, pdbCodes);
		
		
		
			
		
	}
	
	private static void writeFastaFile(Set<String> pdbCodes, File file) throws IOException, FileFormatException, PdbLoadException {
		
		PrintStream ps = new PrintStream(file);
		
		for (String pdbCode:pdbCodes) {
			File cifFile = new File(TMPDIR,BASENAME+"_"+pdbCode+".cif");
			try {
				PdbAsymUnit.grabCifFile(LOCAL_CIF_DIR, null, pdbCode, cifFile, false);
			} catch (IOException e) {
				System.out.println("\nError while reading cif.gz file ("+new File(LOCAL_CIF_DIR,pdbCode+".cif.gz").toString()+") or writing temp cif file: "+e.getMessage());
				continue;
			}
			PdbAsymUnit pdb = null;
			try {
				pdb = new PdbAsymUnit(cifFile);
			} catch (PdbLoadException e) {
				System.err.println("Could not load "+pdbCode+": "+e.getMessage());
				continue;
			}
			
			for (String pdbChainCode:pdb.getAllRepChains()) {
				Sequence seq = pdb.getChain(pdbChainCode).getSequence();
				if (seq.getSeq().matches("X+")) continue; // if it's an all X sequence we don't want it (blastclust doesn't like them)
				if (seq.getLength()<12) continue; // we ignore too small sequences (blastclust doesn't like them)
				if (seq.isNucleotide()) continue; // some sets (like Bahadur's monomers) contain DNA/RNA: ignore
				seq.writeToPrintStream(ps);
			}
			
			cifFile.delete();
			
			
		}
		ps.close();
		
	}
	
	private static void doClustering(File inputSeqFile, int[] idCutoffs, Map<String,Integer> pdbCodes) throws IOException, InterruptedException, BlastException {
		
		System.out.println("Running blastclust");
		
		File outblastclustFile = File.createTempFile(BASENAME, ".blastclust.out");
		File saveFile = File.createTempFile(BASENAME, ".blastclust.neighbors");
		
		BlastRunner blastRunner = new BlastRunner(BLAST_BIN_DIR, null);
		long start = System.currentTimeMillis();
		blastRunner.runBlastclust(inputSeqFile, outblastclustFile, true, idCutoffs[0], 1, BLAST_DATA_DIR, saveFile, NTHREADS);
		long end = System.currentTimeMillis();
		System.out.println("Initial blastclust done in "+((end-start)/1000)+"s");
		
		
		for (int idCutoff:idCutoffs) {
			List<List<String>> clusters = blastRunner.runBlastclust(outblastclustFile,true,idCutoff,1,saveFile,NTHREADS);
			
			List<String> memberStrings = new ArrayList<String>();
			
			for (List<String> cluster:clusters) {
				if (cluster.size()>1) {
			
					String memberString = "";
					for (String member:cluster) {
						memberString+=member+"("+pdbCodes.get(member.substring(0, 4))+")"+" ";
					}
					memberStrings.add(memberString); 
				}
			}
			
			if (memberStrings.size()>0) {
				System.out.println("Clusters with more than 1 member at "+idCutoff+" identity");
				for (String memberString:memberStrings) {
					System.out.println(memberString);
				}
			} else {
				System.out.println("All clusters are size 1: no redundancy at "+idCutoff+" identity!");
			}

		}
		
		
		


		outblastclustFile.deleteOnExit();
		saveFile.deleteOnExit();

		
	}

}
