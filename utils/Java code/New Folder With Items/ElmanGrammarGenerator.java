import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import com.jmatio.io.MatFileWriter;
import com.jmatio.types.MLArray;
import com.jmatio.types.MLCell;
import com.jmatio.types.MLChar;
import com.jmatio.types.MLInt8;


public class ElmanGrammarGenerator {

	// You can change me
	public static final int NUM_PROP_NOUNS = 2;

	// You can change me
	public static final int NUM_PLUR_NOUNS = 4;

	// Changing me messes stuff up
	public static final int NUM_SING_NOUNS = NUM_PLUR_NOUNS;

	// Changing me messes stuff up
	public static final int NUM_NOUNS = NUM_PROP_NOUNS + NUM_PLUR_NOUNS
		+ NUM_SING_NOUNS;

	// You can change me
	public static final int NUM_INT_VERBS = 2;

	// Changing me messes stuff up
	public static final int NUM_OPT_VERBS = NUM_INT_VERBS;

	// Changing me messes stuff up
	public static final int NUM_TRN_VERBS = NUM_INT_VERBS ;

	// Changing me messes stuff up
	public static final int NUM_VERBS = 2 * (NUM_INT_VERBS + NUM_OPT_VERBS
		+ NUM_TRN_VERBS); // 2x for plural & singular

	// Changing me messes stuff up
	public static final int NUM_TOKENS = NUM_NOUNS + NUM_VERBS + 2; // "who" and "STOP"	

	public static final byte[] words = new byte[NUM_TOKENS];
	static {
		for (byte i = 0; i < NUM_TOKENS; i++) {
			words[i] = i;
		}
	}

	/************************
		CODE ASSUMES FOLLOWING LAYOUT:

		Proper Nouns -> Singular Nouns -> Plural Nouns -> 
		Plural Intransitive Verbs -> Plural Optionally Transitive Verbs ->
		Plural Transitive Verbs -> Singular Intransitive Verbs ->
		Singular Optionally Transitive Verbs -> Singular Transitive Verbs ->
		"who" -> "STOP/."
		
		Deviations from this will produce grammar inconsistent with translation!
		Though it will be self consistent, regardless.

		Only change paramters with comment "you can change me"
		Having more than 128 total tokens with f%@! s#!$ up unless you change all
		the references to byte to a higher capacity integer type.

	***********************/

	public static final String[] translation = {
		"John", "Mary",
		"the boy", "the girl", "the cat", "the dog",
		"boys", "girls", "cats", "dogs",
		"walk", "live",
		"see", "hear",
		"chase", "feed",
		"walks", "lives",
		"sees", "hears",
		"chases", "feeds",
		"who", "."
	};

	public static void main(String[] args) {
		GrowableByteArray tkns = new GrowableByteArray(6*5400);
		List<byte[]> pntkns = new ArrayList<byte[]>(6*5400);

		for (int i = 0; i < 5000; i++) {
			sentence(tkns, pntkns);
		}
		
		MLArray tokens = new MLInt8("tokens", tkns.getArray(), 1);
		MLArray pred = new MLCell("predTokens", new int[]{pntkns.size(), 1});
		for (int i = 0; i < pntkns.size(); i++) {
			((MLCell) pred).set(new MLInt8(""+i, pntkns.get(i), 1), i );
		}
		MLArray key = new MLCell("key", new int[]{NUM_TOKENS, 1});
		for (int i = 0; i < NUM_TOKENS; i++) {
			((MLCell) key).set(new MLChar(""+i, translation[i]), i );
		}
		List<MLArray> data = new ArrayList<MLArray>();
		data.add(tokens);
		data.add(pred);
		data.add(key);
		try {
			new MatFileWriter("Corpus.mat", data);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		for (int i = 0; i < 500; i++) {
			System.out.print(translation[tkns.getArray()[i]] + "\t\t\t");
			for (int j = 0; j < pntkns.get(i).length; j++) {
				System.out.print(translation[pntkns.get(i)[j]] + " ");
			}
			System.out.println();
		}
		System.out.println(tkns.size() + " " + pntkns.size());
		
	}


	public static void sentence(GrowableByteArray tkns, List<byte[]> pntkns) {

		int noun = selectNoun(); // NP
		tkns.add(words[noun]);
		pntkns.add(getAllAgreeingVerbs_plusWho(noun));

		int flip = ThreadLocalRandom.current().nextInt(2);
		if (flip == 0) { // NP is RC
			RecursiveClause(tkns, pntkns, noun, false, false);
		}
		int verb = selectAgreeingVerb(noun);
		tkns.add(words[verb]);

		int trns = testTransitivity(verb);
		if (trns == 0) {
			byte[] stop = new byte[1];
			stop[0] = words[NUM_TOKENS-1];
			pntkns.add(stop);


		} else if (trns == 1) {
			pntkns.add(getAllNouns_plusSTOP());

			flip = ThreadLocalRandom.current().nextInt(2);
			if (flip == 0) {
				noun = selectNoun();
				tkns.add(words[noun]);
				byte[] whoOrStop = {words[NUM_TOKENS-2], words[NUM_TOKENS-1]};
				pntkns.add(whoOrStop);

				// Is it recursive?
				flip = ThreadLocalRandom.current().nextInt(2);
				if(flip==0) {
					RecursiveClause(tkns, pntkns, noun, false, true);
				}

			}
		} else {
			pntkns.add(getAllNouns_plusSTOP());
			noun = selectNoun();
			tkns.add(words[noun]);
			byte[] whoOrStop = {words[NUM_TOKENS-2], words[NUM_TOKENS-1]};
			pntkns.add(whoOrStop);

			// Is it recursive?
			flip = ThreadLocalRandom.current().nextInt(2);
			if(flip==0) {
				RecursiveClause(tkns, pntkns, noun, false, true);
			}

		}
		tkns.add(words[NUM_TOKENS-1]);
		pntkns.add(getAllNouns());
	}

	public static void RecursiveClause(GrowableByteArray tkns, List<byte[]> pntkns, int nounIndex, boolean type, boolean end) {
		
		tkns.add(words[NUM_TOKENS-2]); // WHO
		// Predict we'll either see a noun or a verb that agrees with the calling noun (nounIndex)	
		pntkns.add(getAllNounsAndAgVerbs(nounIndex));

		// Are we a NP VP recursive clause or a  VP (NP) type?
		int flip = ThreadLocalRandom.current().nextInt(2);
		
		if (flip == 0) { // Type 1 -> who NP VP_o/t

			// Select a noun (becomes calling noun for sub-recursion)
			int noun = selectNoun();
			tkns.add(words[noun]);

			// Only transitive/opt trans verbs can be the VP in a
			// NP VP recursive clause, so predict seeing either
			// a transitive verb in agreement or "who"
			pntkns.add(getAllAgTransVerbs_plusWho(noun));

			// Is NP a sub-recusrive clause?
			flip = ThreadLocalRandom.current().nextInt(2);

			if (flip==0) { // NP is NRC
				RecursiveClause(tkns, pntkns, noun, true, false);
			}
			tkns.add(words[selectAgreeingTransVerb(noun)]); // VP

			// If you're within a RC of type NP VP, where this reursive clause
			// is merely being called by the NP of another RC, then predict
			// only transitive verbs upon exit.
			if (type) {
				pntkns.add(getAllAgTransVerbs(nounIndex));
			} else {
				pntkns.add(getAllAgreeingVerbs(nounIndex));
			}
			
		} else { // Type 2 -> who VP (NP)
			
			// Select Verb that agrees with noun before the "who"
			int wrd = selectAgreeingVerb(nounIndex);
			tkns.add(words[wrd]);

			// Is is transitive?
			int trns = testTransitivity(wrd);
			if (trns == 0) {
				// No, so predict seeing a verb agreeing with nounIndex
				if (type) {
					pntkns.add(getAllAgTransVerbs(nounIndex));
				} else {
					pntkns.add(getAllAgreeingVerbs(nounIndex));
				}
			} else if (trns == 1) {
				//Maybe, so predict any noun or a verb agreeing with nounIndex
				if (type) {
					pntkns.add(getAllNounsAndAgTrVerbs(nounIndex));
				} else {
					pntkns.add(getAllNounsAndAgVerbs(nounIndex));
				}

				// Flip a coin to get it an objective noun or not
				flip = ThreadLocalRandom.current().nextInt(2);
				if (flip == 0) {
					
					// Give it an objective noun
					int noun = selectNoun();
					tkns.add(words[noun]);
					// Predict seeing either "who" or verbs agreeing with calling noun (nounIndex)
					if (type) {
						pntkns.add(getAllAgTransVerbs_plusWho(nounIndex));
					} else {
						if (end) {
							byte[] whoOrStop = {words[NUM_TOKENS-2], words[NUM_TOKENS-1]};
							pntkns.add(whoOrStop);
						} else {
							pntkns.add(getAllAgreeingVerbs_plusWho(nounIndex));
						}
					}
					
					// Flip a coin to see if the objective noun is recursive
					flip = ThreadLocalRandom.current().nextInt(2);
					if (flip == 0) {
						// Objective noun is recursive
						RecursiveClause(tkns, pntkns, noun, type, end);
					}


				}
			} else {
				pntkns.add(getAllNouns());
				// Give it an objective noun
				int noun = selectNoun();
				tkns.add(words[noun]);
				// Predict seeing either "who" or verbs agreeing with calling noun (nounIndex)
				if (type) {
					pntkns.add(getAllAgTransVerbs_plusWho(nounIndex));
				} else {
					if (end) {
						byte[] whoOrStop = {words[NUM_TOKENS-2], words[NUM_TOKENS-1]};
						pntkns.add(whoOrStop);
					} else {
						pntkns.add(getAllAgreeingVerbs_plusWho(nounIndex));
					}
				}
					
				// Flip a coin to see if the objective noun is recursive
				flip = ThreadLocalRandom.current().nextInt(2);
				if (flip == 0) {
					// Objective noun is recursive
					RecursiveClause(tkns, pntkns, noun, type, end);
				}
			}
		}
	}

	public static int selectNoun() {
		int selection = ThreadLocalRandom.current().nextInt(NUM_NOUNS);
		return selection;
	}

	public static int selectAgreeingTransVerb(int nounIndex) {
		int selection = ThreadLocalRandom.current().nextInt(
			(NUM_VERBS/2) - (NUM_INT_VERBS));
		if (nounIndex < NUM_SING_NOUNS + NUM_PROP_NOUNS) {
			return selection + NUM_NOUNS + (NUM_VERBS/2) + NUM_INT_VERBS;
		} else if (nounIndex >= NUM_SING_NOUNS + NUM_PROP_NOUNS
			&& nounIndex < NUM_NOUNS) {
			return selection + NUM_NOUNS + NUM_INT_VERBS;
		} else {
			throw new IllegalArgumentException("Passed noun index does" 
			 	+ " not correspond to a noun");
		}
	}

	public static int selectAgreeingVerb(int nounIndex) {
		int selection = ThreadLocalRandom.current().nextInt(NUM_VERBS/2);
		if (nounIndex < NUM_SING_NOUNS + NUM_PROP_NOUNS) {
			return selection + NUM_NOUNS + (NUM_VERBS/2);
		} else if (nounIndex >= NUM_SING_NOUNS + NUM_PROP_NOUNS
			&& nounIndex < NUM_NOUNS) {
			return selection + NUM_NOUNS;
		} else {
			throw new IllegalArgumentException("Passed noun index does" 
			 	+ " not correspond to a noun");
		}
	}

	/**
	* 0 -> intransitive, 1->opt, 2->trans
	**/
	public static int testTransitivity(int verbIndex) {
		if (verbIndex < NUM_NOUNS || verbIndex >= NUM_NOUNS+NUM_VERBS) {
			throw new IllegalArgumentException("Passed verb index does"
				+ "not correspond to a verb.");
		}
		verbIndex = verbIndex - NUM_NOUNS;
		verbIndex = verbIndex % (NUM_VERBS/2);
		if (verbIndex < NUM_INT_VERBS) {
			return 0;
		} else if(verbIndex >= NUM_INT_VERBS
			&& verbIndex < NUM_INT_VERBS + NUM_OPT_VERBS) {
			return 1;
		} else {
			return 2;
		}
	}

	public static byte[] getAllAgreeingVerbs_plusWho(int nounIndex) {
		byte[] agVerbs = new byte[NUM_VERBS/2 + 1];
		byte offset = (byte) 0;
		if (nounIndex < NUM_SING_NOUNS + NUM_PROP_NOUNS) {
			offset = (byte) NUM_NOUNS + NUM_VERBS/2;
		} else if (nounIndex >= NUM_SING_NOUNS + NUM_PROP_NOUNS
			&& nounIndex < NUM_NOUNS) {
			offset = (byte) NUM_NOUNS;
		} else {
			throw new IllegalArgumentException("Passed noun index does" 
			 	+ " not correspond to a noun");
		}
		for (byte i = offset; i < offset + (NUM_VERBS/2); i++) {
			agVerbs[i-offset] = i;
		}
		agVerbs[agVerbs.length-1] = (byte) NUM_TOKENS-2;
		return agVerbs;
	}

	public static byte[] getAllAgreeingVerbs(int nounIndex) {
		byte[] agVerbs = new byte[NUM_VERBS/2];
		byte offset = (byte) 0;
		if (nounIndex < NUM_SING_NOUNS + NUM_PROP_NOUNS) {
			offset = (byte) NUM_NOUNS + NUM_VERBS/2;
		} else if (nounIndex >= NUM_SING_NOUNS + NUM_PROP_NOUNS
			&& nounIndex < NUM_NOUNS) {
			offset = (byte) NUM_NOUNS;
		} else {
			throw new IllegalArgumentException("Passed noun index does" 
			 	+ " not correspond to a noun");
		}
		for (byte i = offset; i < offset + (NUM_VERBS/2); i++) {
			agVerbs[i-offset] = i;
		}
		return agVerbs;
	}

	public static byte[] getAllNouns() {
		byte[] n = new byte[NUM_NOUNS];
		for (byte i = 0; i < NUM_NOUNS; i++) {
			n[i] = i;
		}
		return n;
	}

	public static byte[] getAllAgTransVerbs(int nounIndex) {
		byte[] agVerbs = new byte[NUM_VERBS/2 - NUM_INT_VERBS];
		byte offset = (byte) 0;
		if (nounIndex < NUM_SING_NOUNS + NUM_PROP_NOUNS) {
			offset = (byte) NUM_NOUNS + (NUM_VERBS/2) + NUM_INT_VERBS;
		} else if (nounIndex >= NUM_SING_NOUNS + NUM_PROP_NOUNS
			&& nounIndex < NUM_NOUNS) {
			offset = (byte) NUM_NOUNS + NUM_INT_VERBS;
		} else {
			throw new IllegalArgumentException("Passed noun index does" 
			 	+ " not correspond to a noun");
		}
		for (byte i = offset; i < offset + (NUM_VERBS/2) - NUM_INT_VERBS; i++) {
			agVerbs[i-offset] = i;
		}
		return agVerbs;
	}

	public static byte[] getAllAgTransVerbs_plusWho(int nounIndex) {
		byte[] agVerbs = new byte[NUM_VERBS/2 - NUM_INT_VERBS + 1];
		byte offset = (byte) 0;
		if (nounIndex < NUM_SING_NOUNS + NUM_PROP_NOUNS) {
			offset = (byte) NUM_NOUNS + (NUM_VERBS/2) + NUM_INT_VERBS;
		} else if (nounIndex >= NUM_SING_NOUNS + NUM_PROP_NOUNS
			&& nounIndex < NUM_NOUNS) {
			offset = (byte) NUM_NOUNS + NUM_INT_VERBS;
		} else {
			throw new IllegalArgumentException("Passed noun index does" 
			 	+ " not correspond to a noun");
		}
		for (byte i = offset; i < offset + (NUM_VERBS/2) - NUM_INT_VERBS; i++) {
			agVerbs[i-offset] = i;
		}
		agVerbs[agVerbs.length-1] = (byte) (NUM_TOKENS-2);
		return agVerbs;
	}

	public static byte[] getAllNounsAndAgTrVerbs(int noun) {
		byte[] ns = getAllNouns();
		byte[] vs = getAllAgTransVerbs(noun);
		byte[] all = new byte[ns.length + vs.length];
		System.arraycopy(ns, 0, all, 0, ns.length);
		System.arraycopy(vs, 0, all, ns.length, vs.length);
		return all;
	}

	public static byte[] getAllNounsAndAgVerbs(int noun) {
		byte[] ns = getAllNouns();
		byte[] vs = getAllAgreeingVerbs(noun);
		byte[] all = new byte[ns.length + vs.length];
		System.arraycopy(ns, 0, all, 0, ns.length);
		System.arraycopy(vs, 0, all, ns.length, vs.length);
		return all;
	}


	public static byte[] getAllNouns_plusSTOP() {
		byte[] n = new byte[NUM_NOUNS+1];
		for (byte i = 0; i < NUM_NOUNS; i++) {
			n[i] = i;
		}
		return n;
	}



	public static class GrowableByteArray {
		
		public static final int DEFAULT_CAPACITY = 10000;
		
		private int index = 0;
		
		private byte[] internalByteArray;
		
		private double loadFactor = 0.75;
		
		public GrowableByteArray() {
			internalByteArray = new byte[DEFAULT_CAPACITY];
		}
		
		public GrowableByteArray(int capacity) {
			internalByteArray = new byte[capacity];
		}
		
		public void add(byte b) {
			if (index > internalByteArray.length * loadFactor) {
				byte[] temp = new byte[internalByteArray.length * 2];
				System.arraycopy(internalByteArray, 0, temp, 0, internalByteArray.length);
				internalByteArray = temp;
			}
			internalByteArray[index++] = b;
		}
		
		public byte[] getArray() {
			byte[] temp = new byte[index];
			System.arraycopy(internalByteArray, 0, temp, 0, index);
			return temp;
		}
		
		public int size() {
			return index;
		}
		
	}
}
