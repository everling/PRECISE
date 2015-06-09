package precise_repl;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import edu.mit.jwi.Dictionary;
import edu.mit.jwi.IDictionary;
import edu.mit.jwi.item.IIndexWord;
import edu.mit.jwi.item.ISynset;
import edu.mit.jwi.item.IWord;
import edu.mit.jwi.item.IWordID;
import edu.mit.jwi.item.POS;
import edu.mit.jwi.morph.WordnetStemmer;

public class Lexicon implements java.io.Serializable{
	
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static IDictionary dict;
	private static WordnetStemmer wnst;
	private static List<JoinPath> jps = new ArrayList<JoinPath>();
	private static ArrayList<Element> elements;
	
	private static String[] syntactic_markers = new String[]{"are","the","on","a","in","is","be","of","do","with","by","ha"};
	private static Element[] wh = new Element[]{new Element(Element.TYPE_VALUE,"what"), new Element(Element.TYPE_VALUE,"where"), new Element(Element.TYPE_VALUE,"when"), new Element(Element.TYPE_VALUE,"who"),new Element(Element.TYPE_VALUE,"which"),new Element(Element.TYPE_VALUE,"how")};

	private static HashMap<Token,List<Element>> tokenToElements;
	private static HashMap<String,List<Token>> wordToToken;
	
	public static List<Token> getTokens(String word){
		return wordToToken.get(word);
	}
	
	public static List<Element> getElements(Token t){
		return tokenToElements.get(t);
	}
	
	public static List<Element> getAllElements(){
		return elements;
	}
	
	public static Element[] getWHS(){
		return wh;
	}
	/**
	 * Returns true if the supplied Element is a WH-element
	 * @param pwh
	 * @return
	 */
	public static boolean isWH(Element pwh){
		for(Element e : wh){
			if(e.equals(pwh))
				return true;
		}
		return false;
	}
	
	public static void init(String wordNetPath) throws IOException{
		URL url = new URL ( "file" , null , wordNetPath ) ;
		dict = new Dictionary ( url ) ;		
		dict . open () ;
		wnst = new WordnetStemmer(dict);
		tokenToElements = new HashMap<Token,List<Element>>();
		wordToToken = new HashMap<String,List<Token>>();
		elements = new ArrayList<Element>();
		for(Element e : wh)
			elements.add(e);
		
	}
	
	
	
	public static boolean isSyntacticMarker(String word){
		for(String s: syntactic_markers){
			if(word.equals(s))
				return true;
		}
		return false;
	}

	/**
	 * @param word
	 * @param wordClass
	 * @return first possible word stem. If not found, returns word.
	 */

	public static String getWordStem(String word, POS... wordClass){
		
		try{
			List<String> ws;
			for(POS p : wordClass){
				ws = wnst.findStems(word, p);
				
				
				
				if(ws != null && ws.size() > 0)
					return ws.get(0);
			}
		}catch(IllegalArgumentException iae){
			System.out.println("No word stem for " +word);
		}

		return word;
		
	}
	
	/**
	 * Will return a list of synonyms for a word, given a set of permissible word classes
	 * @param word
	 * @param wordClass
	 * @return
	 */
	public static List<String> getSynonyms(String word, POS... wordClass){
		
		List<String> synonyms = new ArrayList<String>();
		synonyms.add(word.toLowerCase());
		
		List<POS> acceptedWordClasses = Arrays.asList(wordClass);
		
		try{
			for(POS p : acceptedWordClasses){
				IIndexWord iw = dict.getIndexWord(word, p);
				if(iw != null){
					for(IWordID wid : iw.getWordIDs()){
						IWord wrd = dict.getWord(wid);
						ISynset synset = wrd.getSynset();
						for(IWord iwrd : synset.getWords()){
							String lemma = iwrd.getLemma().toLowerCase();
							if(!synonyms.contains(lemma))
							synonyms.add(lemma);
						}
					}
				}
				
			}
				
		}catch(IllegalArgumentException iae){
			System.out.println("Illegal argument: synonym "+word);
		}
	
		Collections.sort(synonyms);
		return synonyms;
		
	}
	
	/**
	 * Tokenize every element and set up mappings word-token and token-element
	 */
	public static void buildLexiconMappings(boolean manual,boolean synonymsForRelAndAtt, boolean synonymsForValues, boolean lemmatizeValues){
		
		manuallyPairAttributesWithWH();
		
		for(Element e : elements){
			List<Token> tokens = Tokenizer.tokenizeElement(e,manual,synonymsForRelAndAtt, synonymsForValues,lemmatizeValues);
			
			for(Token t : tokens){
				addTokenMapping(t,e);
			}
		}
	}
	
	/**
	 * Adds mapping of token to element in lexicon
	 * @param t
	 * @param e
	 */
	public static void addTokenMapping(Token t, Element e){
		pairTokenWithElement(t,e);
		for(String w : t.getWords())
			pairWordWithToken(w, t);
	}
	
	/**
	 * Removes mapping of token to element in lexicon
	 * @param t
	 * @param e
	 */
	public static void removeTokenMapping(Token t, Element e){
		List<Element> elems = tokenToElements.get(t);
		
		if(elems != null){
			elems.remove(e);
			
			if(elems.size() == 0){
				//also remove mapping
				tokenToElements.remove(t);
				List<String> words = t.getWords();
				for(String s : words){
					List<Token> ts = wordToToken.get(s);
					if(ts != null){
						ts.remove(t);
					}
				}
				
				
			}
			
		}
	}
	
	/**
	 * Makes all attributes compatible with all WH-values
	 */
	private static void manuallyPairAttributesWithWH(){
		
		for(Element e : elements){
			
			if(e.getType() == Element.TYPE_ATTRIBUTE){
				String name = e.getName();
				System.out.println(name);
					
					//map WH-values to attribute
					for(Element w : wh){
						w.addCompatible(e);
					}
					
				
			}
		}
		
	}
	
	private static void remapAttributesWithWH(){
		
		for(Element w : wh){
			w.getCompatible().clear();
			
			
			
		}
		
		
		
	}
	
	
	public static void printMappings(){

		Set<Entry<Token, List<Element>>> z = tokenToElements.entrySet();
		
		for(Entry<Token, List<Element>> v : z){
			System.out.println(v.getKey().toString() + " " +Arrays.toString(v.getValue().toArray()));
		}
		
		System.out.println();
		
		Set<Entry<String, List<Token>>> z2 = wordToToken.entrySet();
		
		for(Entry<String, List<Token>> v : z2){
			System.out.println(v.getKey().toString() + " " +Arrays.toString(v.getValue().toArray()));
		}
		
		for(Element r : elements){
			if(r.getType() == Element.TYPE_RELATION)
				System.out.println(r +" primary key: "+r.getPrimaryKey());
		}
	}
	
	
	public static void printMappingsBetter(){
		
		System.out.println("Element-Token mappings:\n");
		
		for(Element e : elements){
			
			System.out.println(e);
			for(Entry<Token, List<Element>> ts : tokenToElements.entrySet()){
				if(ts.getValue().contains(e)){
					System.out.println("\t"+ts.getKey().toString());
				}
			}
			System.out.println();
		}
		
		System.out.println("\n\n\nWH-mappings:\n");
		
		for(Element whh: wh){
			System.out.println(whh);
			for(Element comp : whh.getCompatible()){
				System.out.println("\t"+comp);
			}
		}
		
		
		
		
	}
	
	/**
	 * 
	 * @param wordStemmed array of stemmed words
	 * @return array of stemmed words minus syntactic markers
	 */
	public static String[] removeSyntacticMarkers(String[] wordStemmed){
		int toRemove = 0;
		for(String s : wordStemmed){
			if(isSyntacticMarker(s))
				toRemove++;
		}
		String[] newToken = new String[wordStemmed.length-toRemove];
		int index = 0;
		for(int i = 0; i < wordStemmed.length; i++){
			if(!isSyntacticMarker(wordStemmed[i])){
				newToken[index] = wordStemmed[i];
				index++;
			}
		}
		return newToken;
	}
	
	
	
	/**
	 * Puts token-element mapping in hashmap tokenToElements
	 * @param t
	 * @param e
	 */
	private static void pairTokenWithElement(Token t, Element e){
		List<Element> current = tokenToElements.get(t);
		
		if(current == null){
			current = new ArrayList<Element>();
			current.add(e);
			tokenToElements.put(t, current);
		}
		else if (!current.contains(e)){
			current.add(e);
			
			//token already found, but not explicitly the same object
			//add possible type to the token
			for(Token t2 : tokenToElements.keySet()){
				if(t2.equals(t)){
					t2.addType(e.getType());
					break;
				}
			}
			
			
			
		}
	}
	
	/**
	 * Puts word-token mapping in hashmap wordToToken
	 * @param word
	 * @param t
	 */
	private static void pairWordWithToken(String word, Token t){
		List<Token> current = wordToToken.get(word);
		
		if(current == null){
			current = new ArrayList<Token>();
			current.add(t);
			wordToToken.put(word,current);
		}
		else if (!current.contains(t)){
			current.add(t);
		}
		
		
	}
	
	
	
	
	
	/**
	 * Hardcoded solution to parse Geoquery data file:
	 * ftp://ftp.cs.utexas.edu/pub/mooney/nl-ilp-data/geosystem/geobase
	 * Not the prettiest solution.
	 * 
	 */
	public static void loadElements(String path){
				
		try {
			BufferedReader br = new BufferedReader(new FileReader(path));
			String s = br.readLine();
			
			
			//relations and attributes
			while(!(s = br.readLine()).startsWith("*")){
				if(s.length() > 0 && s.contains("(")){
					String[] elems = s.split("[(),]");
					Element rel = new Element(Element.TYPE_RELATION,elems[0].trim());
					elements.add(rel);
					for(int i = 1; i < elems.length; i++){
						Element att = new Element(Element.TYPE_ATTRIBUTE, elems[i].trim(),rel);
						elements.add(att);
						rel.addSchemaElement(att);
					}
				}
			}
			
			//values
			while((s = br.readLine()) != null){
				if(s.trim().equals(""))
					continue;
				
				//identify relation
				String[] elems = s.split("\\(");

				Element rel = findRelation(elems[0].trim());
				if(rel == null)
					continue;
				
				List<Element> schema = rel.getSchema();

				//get values
				elems = elems[1].split(",");
				
				//fix trailing prolog
				elems[elems.length-1] = elems[elems.length-1].replaceAll("(\\)\\.)", "");
				
				//add value element
				boolean list = false;
				boolean listTurnOff = false;
				int lockIndex = 0;
				for(int i = 0; i < elems.length; i++){
					
						String val = elems[i].replaceAll("'", "").trim();
						if(val.matches("^\\[(\\S*\\s*)*")){
							list = true;
							lockIndex = i;
							val = val.replaceAll("\\[", "");
						}
						if(val.matches("(\\S*\\s*)*\\]$")){
							listTurnOff = true;
							val = val.replaceAll("\\]","");
						}
						//create value element, make compatible with relation element and its attribute element
						Element valueElem = new Element(Element.TYPE_VALUE, val, rel, schema.get( (list)?lockIndex:i));
						elements.add(valueElem);
								
						if(listTurnOff){
							list = false;
							listTurnOff = false;
						}
				}
			}
				
			br.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 
	 * @param name
	 * @return relation with @name 
	 */
	public static Element findRelation(String name){
		for(Element e : elements){
			if(e.getType() == Element.TYPE_RELATION && e.getName().equals(name))
				return e;
		}
		return null;
	}
	
	
	public static void saveLexicon(String name){
		
		LexObj l = new LexObj(elements, syntactic_markers, wh, tokenToElements, wordToToken,jps);
		
		try
	      {
	         FileOutputStream fileOut =new FileOutputStream(name);
	         ObjectOutputStream out = new ObjectOutputStream(fileOut);
	         out.writeObject(l);
	         out.close();
	         fileOut.close();
	         System.out.printf("Lexicon "+name +" saved");
	      }catch(IOException i)
	      {
	          i.printStackTrace();
	      }
		
	}
	
	public static boolean loadLexicon(String name){
		LexObj l = null;
		try
	      {
	         FileInputStream fileIn = new FileInputStream(name);
	         ObjectInputStream in = new ObjectInputStream(fileIn);
	         l = (LexObj) in.readObject();
	         in.close();
	         fileIn.close();
	         elements = l.elements;
	        // syntactic_markers = l.syntactic_markers;
	         wh = l.wh;
	         tokenToElements = l.tokenToElements;
	         wordToToken = l.wordToToken;
	         jps = l.jps;
	         syntactic_markers = l.syntactic_markers;
	         
	         if(jps == null)
	        	 jps = new ArrayList<JoinPath>();
	         
	         
	         System.out.println("Lexicon "+name +" loaded");
	         return true;
	      }catch(IOException i)
	      {
	         i.printStackTrace();
	         return false;
	      }catch(ClassNotFoundException c)
	      {
	         c.printStackTrace();
	         return false;
	      }
	}
	
	private static class LexObj implements java.io.Serializable{
		
		private static final long serialVersionUID = 1L;
		ArrayList<Element> elements;
		String[] syntactic_markers;
		Element[] wh;
		HashMap<Token,List<Element>> tokenToElements;
		HashMap<String,List<Token>> wordToToken;
		List<JoinPath> jps;
		
		public LexObj(List<Element> elements, String[] syntacic_markers, Element[] wh, HashMap<Token,List<Element>> tokenToElements, HashMap<String,List<Token>> wordToToken, List<JoinPath> jps){
			this.elements = Lexicon.elements;
			this.syntactic_markers = syntacic_markers;
			this.wh = wh;
			this.tokenToElements = tokenToElements;
			this.wordToToken = wordToToken;
			this.jps = jps;
		}
		
		
	}
	
	public static boolean canDeriveElementFromToken(List<Token> tokens, Element a){
		for(Token t : tokens){
			List<Element> elems = Lexicon.getElements(t);
			if(elems != null){
				for(Element e : elems){
					if(e.equals(a))
						return true;
				}
			}
		}
		return false;
	}
	
	public static boolean canDeriveElementFromToken(Set<Token> tokens, Element a){
		for(Token t : tokens){
			List<Element> elems = Lexicon.getElements(t);
			if(elems != null){
				for(Element e : elems){
					if(e.equals(a))
						return true;
				}
			}
		}
		return false;
	}
	
	
	public static class JoinPath implements java.io.Serializable{
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		Element attribute0;
		Element attribute1;
		
		
		public boolean hasRelation(Element r){
			return (attribute0.getCompatible().contains(r) ||  attribute1.getCompatible().contains(r));
		}
		
		public String getJoin(){
			
			
			Element r0 = null;
			Element r1 = null;
			
			for(Element e : attribute0.getCompatible())
				if(e.getType() == Element.TYPE_RELATION)
					r0 = e;
			
			for(Element e : attribute1.getCompatible())
				if(e.getType() == Element.TYPE_RELATION)
					r1 = e;
			
			return r0.getName()+"."+attribute0.getName() +" = " +r1.getName()+"."+attribute1.getName();

		}
		
		@Override
		public boolean equals(Object o){
			JoinPath jo = (JoinPath)o;
			return ((attribute0.equals(jo.attribute0) && attribute1.equals(jo.attribute1)) || (attribute1.equals(jo.attribute0) && attribute0.equals(jo.attribute1)));
		}
		
		public boolean sameRelations(JoinPath jp){
			
			Element r0 = null;
			Element r1 = null;
			
			for(Element e : attribute0.getCompatible())
				if(e.getType() == Element.TYPE_RELATION)
					r0 = e;
			
			for(Element e : attribute1.getCompatible())
				if(e.getType() == Element.TYPE_RELATION)
					r1 = e;
			
			
			return (jp.hasRelation(r0) && jp.hasRelation(r1));
			
		}
		
		public boolean hasAttribute(Element ea){
			return ea.equals(attribute0) || ea.equals(attribute1);
		}
		
		@Override
		public String toString(){
			return "Join:"+attribute0 +"="+attribute1;
		}
	}
	
	public static List<JoinPath> getAllJoinPaths(){
		return jps;
	}
	
	public static void clearJoinPaths(){
		jps = new ArrayList<JoinPath>();
	}
	
	/**
	 * Format Relation1.Attribute1,Relation2.attribute2 
	 * @param joinPath
	 */
	public static void addJoinPath(String joinPath){
		
		String[] entries = joinPath.split(",");
		
		List<Element> jpath = new ArrayList<Element>();
		
		for(int i = 0; i < entries.length; i++){
			String entry = entries[i];
			String[] relAndAtt = entry.split("\\.");
			if(relAndAtt.length != 2)
				continue;
			
			String relation = relAndAtt[0];
			Token attribute = Tokenizer.tokenizeString(relAndAtt[1].toLowerCase()).get(0);
			
			EA:
			for(Element ea : Lexicon.getElements(attribute)){
				for(Element r : ea.getCompatible()){
					if(r.getType() == Element.TYPE_RELATION && r.getName().equals(relation)){
						jpath.add(ea);
						break EA;
					}
				}
			}
		}
		
		for(int i = 0 ; i < jpath.size(); i++){
			for(int v = 0; v < jpath.size(); v++){
				if(i == v)
					continue;
				
				JoinPath jp = new JoinPath();
				jp.attribute0 = jpath.get(i);
				jp.attribute1 = jpath.get(v);
				if(!jps.contains(jp))
					jps.add(jp);
				
			}
		}
		
	}
	
	
	public static List<List<JoinPath>> getJoinPathsV2(List<Element> relations, Element select){
		
		List<List<JoinPath>> toRet = new ArrayList<List<JoinPath>>();

		
		//get all join paths that involve the relations and don't involve other relations
		Set<JoinPath> possible = new HashSet<JoinPath>();
		for(JoinPath jp : jps){
			for(Element r0 : relations){
				for(Element r1 : relations){
					if(r0 == r1)
						continue;
					
					if(jp.hasRelation(r0) && jp.hasRelation(r1))
						possible.add(jp);
				}
			}
		}
		
		
		//all possible subsets
		Set<Set<JoinPath>> pwr = Tokenizer.powerSet(possible);
		
		//select those of length r - 1 and where all relations are represented. this forces a complete join
		for(Set<JoinPath> sjp : pwr){
			if(sjp.size() != relations.size() - 1)
				continue;
			
			Iterator<JoinPath> itp =sjp.iterator();
			
			boolean[] isRepresented = new boolean[relations.size()];
			
			while(itp.hasNext()){
				JoinPath j = itp.next();
				for(int i = 0; i < relations.size(); i++){
					if(j.hasRelation(relations.get(i)))// && (!j.attribute0.equals(select)) && (!j.attribute1.equals(select)))
						isRepresented[i] = true;
				}
			}
			
			boolean valid = true;
			for(boolean b : isRepresented)
				if(b == false)
					valid = false;
			
			if(valid)
				toRet.add(new ArrayList<JoinPath>(sjp));
			
			
		}		
		return toRet;
		
	}

	
	
	public static List<Token> getMatchingTokens(Element e){
		
		List<Token> toRet = new ArrayList<Token>();
		Set<Entry<Token, List<Element>>> set = tokenToElements.entrySet();
		
		for(Entry<Token,List<Element>> entry : set){
			if(entry.getValue().contains(e))
				toRet.add(entry.getKey());
		}
		
		return toRet;
	}
	
}


