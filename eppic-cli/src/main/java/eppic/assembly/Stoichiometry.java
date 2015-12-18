package eppic.assembly;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.biojava.nbio.structure.Chain;
import org.biojava.nbio.structure.Compound;
import org.biojava.nbio.structure.Structure;
import org.jgrapht.UndirectedGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A representation of the stoichiometry of an Assembly in a crystal structure
 * 
 * 
 * @author duarte_j
 *
 */
public class Stoichiometry {

	private static final Logger logger = LoggerFactory.getLogger(Stoichiometry.class);
	
	private static final String UNKNOWN_SYMMETRY =  "unknown";
			
	private Structure structure;
	private Assembly assembly;
	private int[] sto;
	private int[] comp;
	
	private Map<Integer,Integer> entityId2Idx;
	private Map<Integer,Integer> idx2EntityId;
	
	private Map<String,Integer> chainIds2Idx;
	private Map<Integer,String> idx2ChainIds;
	
	public Stoichiometry(Structure structure, Assembly assembly) {
		
		this.structure = structure;		
		this.assembly = assembly;
		
		int totalNumEntities = structure.getCompounds().size();
		
		// since the entityIds are not guaranteed to be 1 to n, we need to map them to indices
		entityId2Idx = new HashMap<Integer,Integer>();
		idx2EntityId = new HashMap<Integer,Integer>();
		chainIds2Idx = new HashMap<String, Integer>();
		idx2ChainIds = new HashMap<Integer, String>();
		
		int i = 0;
		for (Compound c:structure.getCompounds()) {
			entityId2Idx.put(c.getMolId(),i);
			idx2EntityId.put(i,c.getMolId());
			i++;
		}

		i = 0;
		for (Chain c:structure.getChains()) {
			chainIds2Idx.put(c.getChainID(),i);
			idx2ChainIds.put(i,c.getChainID());
			i++;
		}
		
		sto = new int[totalNumEntities];
		comp = new int[structure.getChains().size()];
	}
	
	public int getEntityIndex(int entityId) {
		return entityId2Idx.get(entityId);
	}
	
	public int getEntityId(int index) {
		return idx2EntityId.get(index);
	}
	
	public int getChainIndex(String chainId) {
		return chainIds2Idx.get(chainId);
	}
	
	public String getChainId(int chainIdx) {
		return idx2ChainIds.get(chainIdx);
	}
	
	/**
	 * Get all entity ids present in this stoichiometry
	 * @return
	 */
	public Set<Integer> getEntityIds() {
		Set<Integer> entityIds = new HashSet<Integer>();
		for (int i=0;i<getNumEntities();i++) {
			if (sto[i]>0) {
				entityIds.add(getEntityId(i));
			}
		}
		return entityIds;
	}
	
	public void add(Chain c) {
		sto[getEntityIndex(c.getCompound().getMolId())]++;
		comp[getChainIndex(c.getChainID())]++;
	}
	
	public int getNumEntities() {
		return sto.length;
	}
	
	public int getNumPresentEntities() {
		int numPresentEntities = 0;
		for (int i=0;i<this.getNumEntities();i++) {
			if (sto[i]>0) {
				numPresentEntities ++;
			}
		}
		return numPresentEntities;
	}
	
	/**
	 * Returns the stoichiometry represented as a vector of positive integers with counts per entity
	 * @return
	 */
	public int[] getStoichiometry() {
		return sto;
	}
	
	/**
	 * Returns the composition represented as a vector of positive integers with counts per chain 
	 * @return
	 */
	public int[] getComposition() {
		return comp;
	}
	
	public int getCount(int entityId) {
		return sto[getEntityIndex(entityId)];
	}
	
	public int getCountForIndex(int i) {
		return sto[i];
	}
	
	public int getTotalSize() {
		int size = 0; 
		for (int i=0;i<getNumEntities();i++) {
			size += sto[i];
		}
		return size;
	}
	
	/**
	 * Tells whether 2 stoichiometries are overlapping in any of their entities,
	 * this is equivalent to !{@link #isOrthogonal(Stoichiometry)}
	 * @param other
	 * @return
	 */
	public boolean isOverlapping(Stoichiometry other) {
		return !isOrthogonal(other);
	}
	
	/**
	 * Tells whether 2 stoichiometries are orthogonal: i.e. they don't share any of their entities
	 * @param other
	 * @return
	 */
	public boolean isOrthogonal(Stoichiometry other) {
		if (this.getNumEntities()!=other.getNumEntities()) 
			throw new IllegalArgumentException("Vectors of different length");
		if (this.getNumEntities()==0) 
			throw new IllegalArgumentException("Vectors have size 0"); 
		
		int scalarProduct = 0;
		for (int i=0;i<this.getNumEntities();i++) {
			scalarProduct += sto[i]*other.sto[i];
		}
		return scalarProduct == 0;
	}
	
	/**
	 * Tells whether this stoichiometry is even: a stoichiometry is even if and
	 * only if all of its non-zero members have the same count
	 * @return true if even, false otherwise, if all members have 0 count it also returns false
	 */
	public boolean isEven() {
		int nonzero = getFirstNonZero();
		// all are zero
		if (nonzero == -1) return false;
		
		for (int i=0;i<this.getNumEntities();i++) {
			if (sto[i]>0 && sto[i]!=nonzero) return false;
		}
		
		return true;
	}
	
	/**
	 * Return the first non-zero count or -1 if all are 0
	 * @return
	 */
	protected int getFirstNonZero() {
		int nonzero = -1;
		for (int i=0;i<this.getNumEntities();i++) {
			if (sto[i]>0) {
				nonzero = sto[i];
				break;
			}
		}
		return nonzero;
	}
	
	public String toFormattedString() {
		StringBuilder stoSb = new StringBuilder();
		
		for (int i=0;i<getNumEntities();i++){
			if (sto[i]>0) {
				stoSb.append(structure.getCompoundById(getEntityId(i)).getRepresentative().getChainID());			
				if (sto[i]>1) stoSb.append(sto[i]); // for A1B1 we do AB (we ommit 1s)
			}
		}
		return stoSb.toString();
	}
	
	public String toFormattedCompositionString() {
		StringBuilder stoSb = new StringBuilder();
		
		for (int i=0;i<structure.getChains().size();i++){
			if (comp[i]>0) {
				stoSb.append(getChainId(i));			
				if (comp[i]>1) stoSb.append(comp[i]); // for A1B1 we do AB (we ommit 1s)
			}
		}
		return stoSb.toString();
	}
	
	public String getChainIdsString() {
		UndirectedGraph<ChainVertex,InterfaceEdge> g = assembly.getFirstRelevantConnectedComponent(this);
		StringBuilder sb = new StringBuilder();
		int i = 0;
		for (ChainVertex v:g.vertexSet()) {
			if (i!=0) sb.append(',');
			sb.append(v.getChainId()+"_"+v.getOpId());
			i++;
		}
		
		return sb.toString();
	}
	
	/**
	 * Return the symmetry string for this stoichiometry in its assembly:
	 * cyclic Cn, dihedral Dn, tetrahedral T, octahedral O or icosahedral I 
	 * This will work correctly only on assemblies that have been previously checked
	 * to be valid with {@link Assembly#isValid()}
	 * @return
	 */
	public String getSymmetry() {
		
		
		// we get the number of present entities in this stoichiometry (those with count>0)
		int numEntities = getNumPresentEntities();

		
		boolean heteromer = false;
		if (numEntities>1) heteromer = true;

		int n = getFirstNonZero();
		
		if (n==-1) {
			logger.warn("All counts are 0 for this stoichiometry. Something is wrong!");
			return UNKNOWN_SYMMETRY;
		}
		
		
		UndirectedGraph<ChainVertex, InterfaceEdge> g = assembly.getFirstRelevantConnectedComponent(this);
		GraphContractor gctr = new GraphContractor(g);


		if (heteromer) {
			
			g = gctr.contract();
			
		}		
		
		if (heteromer && !isEven()) { 
			// this should not happen since we disallow uneven stoichiometries in the search for valid assemblies
			logger.warn("Uneven stoichiometry found while getting symmetry. Something is wrong!");
			return UNKNOWN_SYMMETRY;
		}
		
		// FINDING SYMMETRY:

		// this should work fine for both homomer and pseudo-homomer graph
		int numDistinctInterfaces = GraphUtils.getDistinctInterfaceCount(g);
		
		// CASE A) n==1
		
		if (n==1) {
			
			return "C1";
			
		} 
		
		// CASE B) n==2 or n is odd
		
		if (n%2 != 0 || n==2) {
			
			return "C"+n;

		} 
		
		// CASE C) even number larger than 2 (n%2==0 with n>2)

		if (numDistinctInterfaces==1) {
			return "C"+n;			
		} 


		//NOTE: in principle we could just assume that if numDistinctInterfaces>1 this will be a D,
		//      but! it can happen that a Cn assembly has cross-interfaces, e.g. 4hi5 (a C4)

		
		TreeMap<Integer, Integer> cycleSizes = GraphUtils.getCycleMultiplicities(g);
		boolean nMultCycleExists = false;
		boolean threeMultCycleExists = false;
		boolean fourMultCycleExists = false;
		boolean fiveMultCycleExists = false;
		
		for (int cycleMult:cycleSizes.values()) {
			if (cycleMult==n) nMultCycleExists = true;
			if (cycleMult==3) threeMultCycleExists = true;
			if (cycleMult==4) fourMultCycleExists = true;
			if (cycleMult==5) fiveMultCycleExists = true;
		}
		
//		boolean nMultExists = false;
//		boolean threeMultExists = false;
//		boolean fourMultExists = false;
//		boolean fiveMultExists = false;
//
//		// this should work fine for both homomer and pseudo-homomer graph
//		for (int mult:Assembly.getMultiplicities(g).values()) {
//			if (mult==n) nMultExists = true;
//			if (mult==3) threeMultExists = true;
//			if (mult==4) fourMultExists = true;
//			if (mult==5) fiveMultExists = true;
//		}

		if (nMultCycleExists) {
			return "C"+n;
		}

		if (n==12 && threeMultCycleExists) {
			// tetrahedral
			return "T";
		}
		if (n==24 && fourMultCycleExists) {
			// octahedral
			return "O";
		}
		if (n==60 && fiveMultCycleExists) {
			// icosahedral
			return "I";
		}
		
		// none of the above return: it has to be a D n/2
		return "D"+(n/2);


	}
	
	/**
	 * Returns true if this stoichiometry covers all entities, i.e. it has 
	 * a count >0 for all of its member molecules. False otherwise
	 * @return
	 */
	public boolean isFullyCovering() {
		for (int i=0;i<sto.length;i++) {
			if (sto[i]==0) return false;
		}
		return true;
	}
	
	@Override
	public String toString() {
		return Arrays.toString(sto);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(sto);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Stoichiometry other = (Stoichiometry) obj;
		if (!Arrays.equals(sto, other.sto))
			return false;
		return true;
	}
	
	/**
	 * Return the greatest common divisor (GCD, aka greatest common factor GCF) for the given ints
	 * This is an implementation of the Euclidean algorithm:
	 * http://en.wikipedia.org/wiki/Euclidean_algorithm
	 * Thanks to this SO question: 
	 * http://stackoverflow.com/questions/4009198/java-get-greatest-common-divisor
	 * @param a
	 * @param b
	 * @return
	 */
	public static int gcd(int a, int b) {		
		if (b==0) return a;
		return gcd(b,a%b);
	}

	/**
	 * Return the greatest common divisor (GCD) of the given array of ints
	 * @param values
	 * @return
	 */
	public static int gcd(int[] values) {
		if (values.length==0) throw new IllegalArgumentException("Can't calculate GCD for an empty array");
		
		// we go recursively		
		int result = values[0];
		for(int i = 1; i < values.length; i++){
		    result = gcd(result, values[i]);
		}
		return result;
		
	}
}
