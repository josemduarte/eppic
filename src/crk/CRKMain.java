package crk;

import gnu.getopt.Getopt;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import owl.core.connections.NoMatchFoundException;
import owl.core.connections.SiftsConnection;
import owl.core.features.InvalidFeatureCoordinatesException;
import owl.core.features.OverlappingFeatureException;
import owl.core.features.SiftsFeature;
import owl.core.runners.TcoffeeError;
import owl.core.runners.blast.BlastError;
import owl.core.sequence.UniprotHomolog;
import owl.core.sequence.UniprotHomologList;
//import owl.core.sequence.Sequence;
import owl.core.sequence.alignment.MultipleSequenceAlignment;
import owl.core.structure.Pdb;
import owl.core.structure.PdbCodeNotFoundError;
import owl.core.structure.PdbLoadError;
import owl.core.structure.PdbasePdb;
import owl.core.util.MySQLConnection;

public class CRKMain {
	
	private static final String   PROGRAM_NAME = "crk";
	
	private static final String   PDBASEDB = "pdbase";
	
	private static final String   SIFTS_FILE = "/nfs/data/dbs/uniprot/current/pdb_chain_uniprot.lst";
	
	private static final String   BLAST_BIN_DIR = "/usr/bin";
	private static final String   BLAST_DB_DIR = "/nfs/data/dbs/uniprot/current";
	private static final String   BLAST_DB = "uniprot_all.fasta"; //"uniprot_sprot.fasta";
	private static final int      DEFAULT_BLAST_NUMTHREADS = 1;
	
	private static final File     TCOFFE_BIN = new File("/usr/bin/t_coffee");
	private static final boolean  TCOFFEE_VERYFAST_MODE = true;
	
	private static final double   DEFAULT_IDENTITY_CUTOFF = 0.6;
	

	public static void main(String[] args) throws SQLException, PdbCodeNotFoundError, PdbLoadError, IOException, BlastError {
		
		String pdbId = null;
		double idCutoff = DEFAULT_IDENTITY_CUTOFF;
		String baseName = null;
		File outDir = new File(".");
		int blastNumThreads = DEFAULT_BLAST_NUMTHREADS;

		String help = "Usage: \n" +
		PROGRAM_NAME+"\n" +
		"   -i :  input PDB code\n" +
		"  [-d]:  sequence identity cut-off, homologs below this threshold won't be considered, \n" +
		"         default: "+String.format("%3.1f",DEFAULT_IDENTITY_CUTOFF)+"\n"+
		"  [-a]:  number of threads for blast. Default: "+DEFAULT_BLAST_NUMTHREADS+"\n"+
		"  [-b]:  basename for output files. Default: PDB code \n"+
		"  [-o]:  output dir, where output files will be written. Default: current dir \n\n";


		Getopt g = new Getopt(PROGRAM_NAME, args, "i:d:a:b:o:h?");
		int c;
		while ((c = g.getopt()) != -1) {
			switch(c){
			case 'i':
				pdbId = g.getOptarg();
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
			case 'h':
			case '?':
				System.out.println(help);
				System.exit(0);
				break; // getopt() already printed an error
			}
		}
		
		if (pdbId==null) {
			System.err.println("Missing argument -i");
			System.exit(1);
		}
		
		if (baseName==null) {
			baseName=pdbId;
		}

		// files
		
		File alnFile = new File(outDir,baseName+".homologs.aln");
		
		// 1) getting the uniprot ids corresponding to the query (the pdb sequence)

		MySQLConnection conn = new MySQLConnection();
	
		String pdbCode = pdbId.substring(0, 4);
		String pdbChainCode = pdbId.substring(4,5);
		
		Pdb pdb = new PdbasePdb(pdbCode,PDBASEDB,conn);
		pdb.load(pdbChainCode);
		
		SiftsConnection siftsConn = new SiftsConnection(SIFTS_FILE);
		Collection<SiftsFeature> mappings = null;
		try {
			mappings = siftsConn.getMappings(pdbCode, pdbChainCode);		
			for (SiftsFeature mapping:mappings) {
				pdb.addFeature(mapping); 
			}
		} catch (NoMatchFoundException e) {
			System.err.println(e.getMessage());
			//TODO blast, find uniprot mapping and use it if one can be found
		} catch (OverlappingFeatureException e1) {
			System.err.println(e1.getMessage());
			System.exit(1);
		} catch (InvalidFeatureCoordinatesException e2){
			System.err.println(e2.getMessage());
			System.exit(1);			
		}
		
		List<UniprotHomolog> queryMembers = new ArrayList<UniprotHomolog>();
		for (SiftsFeature sifts:mappings) {
			queryMembers.add(new UniprotHomolog(sifts.getUniprotId()));
		}
		System.out.println("Uniprot ids for the query ("+pdbCode+pdbChainCode+")");
		for (UniprotHomolog queryMember:queryMembers) {
			queryMember.retrieveUniprotKBData();
			queryMember.retrieveEmblCdsSeqs();
			System.out.println(queryMember.getUniId());
		}

		
		// 2) getting the homologues and sequence data
		UniprotHomologList homologs = new UniprotHomologList(pdbId, pdb.getSequence());
		
		System.out.println("Blasting...");
		homologs.searchWithBlast(BLAST_BIN_DIR, BLAST_DB_DIR, BLAST_DB, blastNumThreads);
		System.out.println(homologs.size()+" homologs found by blast");
		
		// applying identity cutoff
		homologs.restrictToMinId(idCutoff);
		System.out.println(homologs.size()+" homologs after applying "+String.format("%4.2f",idCutoff)+" identity cutoff");
		
		System.out.println("Looking up UniprotKB data...");
		homologs.retrieveUniprotKBData();
		
		System.out.println("Retrieving EMBL cds sequences...");
		homologs.retrieveEmblCdsSeqs();
		
//		System.out.println("Summary:");
//		for (UniprotHomolog hom:homologs) {
//			System.out.printf("%s\t%5.1f",hom.getUniId(),hom.getPercentIdentity());
//			for (String id:hom.getTaxIds()){
//				System.out.print("\t"+id);
//			}
//			for (String emblCdsId:hom.getEmblCdsIds()) {
//				System.out.print("\t"+emblCdsId);
//			}
//			System.out.println();
//			for (Sequence seq:hom.getEmblCdsSeqs()) {
//				seq.writeToPrintStream(System.out);
//			}
//		}
		
		// 3) alignment of the protein sequences using tcoffee
		System.out.println("Aligning protein sequences with t_coffee...");
		MultipleSequenceAlignment aln = null;
		try {
			aln = homologs.getTcoffeeAlignment(TCOFFE_BIN, true);
		} catch (TcoffeeError e) {
			System.err.println("t_coffee failed to run");
			System.err.println("Error: "+e.getMessage() );
			System.exit(1);
			
		}
		// writing the alignment to file
		aln.writeFasta(new PrintStream(alnFile), 80, TCOFFEE_VERYFAST_MODE);
		
		// 4) getting entropies and printing them
		aln.printProfile(System.out, pdbId);
	}

}
