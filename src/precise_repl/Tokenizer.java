package precise_repl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import edu.mit.jwi.item.POS;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.util.CoreMap;

public class Tokenizer {
	
	
	
	/**
	 * 
	 * @param t
	 * @return true if t is a WH-token
	 */
	public static boolean isWH(Token t){

		List<Element> whs = Lexicon.getElements(t);
	
		if(whs == null)
			return false;
		
		for(Element wh : whs){
			if(Lexicon.isWH(wh))
				return true;
		}
		return false;
	}
	
	/**
	 * This method returns a list of all possible complete tokenizations, given a question.
	 * However, a complete tokenization does not make a decision on which type a token is. A token that is both
	 * a relation and an attribute will not generate two separate tokenizations.
	 * 
	 * Restrictions =	 only one WH-token
	 * 					 only distinct tokens
	 * @return
	 */
	public static List<Set<Token>> getCompleteTokenizations(CoreMap question, boolean print){
		
		List<CoreLabel> parsedWords = Parser.getWords(question);
		List<Token> tokens = new ArrayList<Token>();
		List<String> wordList = new ArrayList<String>();
		
		//get potential tokens for each word stem
		for(CoreLabel word : parsedWords){
			String stem = Lexicon.getWordStem(word.value().toLowerCase(), Parser.getWordnetPOS(word));
			wordList.add(stem);
			if(!Lexicon.isSyntacticMarker(stem)){
				List<Token> tokens2 = Lexicon.getTokens(stem);
				if(tokens2 == null){ //incomplete tokenization
					if(print)
						System.out.println("no token for :" +stem);
					return null;
				}
				for(Token t : tokens2)
					tokens.add(t);
			}
		}
		
		
		String[] words = wordList.toArray(new String[wordList.size()]);
		words = Lexicon.removeSyntacticMarkers(words);
		
		//remove tokens with words not in the question
		ArrayList<Token> toRemove = new ArrayList<Token>();
		HashMap<String,String> fastCheck = new HashMap<String,String>();
		for(String w : words)
			fastCheck.put(w, w);
			
		for(Token t : tokens){
			for(String w : t.getWords()){
				if(fastCheck.get(w) == null){
					toRemove.add(t);
					break;
				}
			}
		}
			
		for(Token t : toRemove)
			tokens.remove(t);
		
		if(print)
			System.out.println("\n***** Tokenizer tokens:" +Arrays.toString(tokens.toArray()));
		
		//get all possible subsets of tokens
		Set<Token> tokenSet = new HashSet<Token>();
		for(Token t : tokens)
			tokenSet.add(t);
		Set<Set<Token>> tokenSubsets = powerSet(tokenSet);

		//return only complete tokenizations
		List<Set<Token>> toRet = new ArrayList<Set<Token>>();
		
		if(print)
			System.out.println("\n***** Tokenizer complete tokenizations:");

		for(Set<Token> st : tokenSubsets){
			if(isCompleteTokenization(st, words)){
				if(print)
					System.out.println(Arrays.toString(st.toArray()));
				toRet.add(st);
			}
		}
		
		if(toRet.size() == 0 && print)
			System.out.println("no complete tokenization");
		
		return toRet;
	}
	
	public static List<Set<Token>> classifyTokens(Set<Token> tokenization){
		
		Set<Token> relations = new HashSet<Token>();
		Set<Token> attributes = new HashSet<Token>();
		Set<Token> values = new HashSet<Token>();
		
		for(Token t: tokenization){
			if(t.isType(Element.TYPE_RELATION))
				relations.add(t);
			if(t.isType(Element.TYPE_ATTRIBUTE))
				attributes.add(t);
			if(t.isType(Element.TYPE_VALUE))
				values.add(t);
		}
		
		List<Set<Token>> toRet = new ArrayList<Set<Token>>();
		
		toRet.add(relations);
		toRet.add(attributes);
		toRet.add(values);
		
		return toRet;
		
		
	}
	
	/**
	 * Since some relation tokens might also be attribute and/or value tokens there can be several interpretations
	 * of a complete tokenizations. This method returns all the possible interpretations to be used in the matcher
	 * @param tokenization
	 * @param tokenType 
	 * @return List of TokenSetPairs of with a-set of type tokenType
	 */
	public static List<TokenSetPair> getAllTokenizationInterpretations(Set<Token> tokenization,int tokenType){
		
		
		HashSet<Token> tokensOfType = new HashSet<Token>();
		ArrayList<TokenSetPair> toRet = new ArrayList<TokenSetPair>();
		ArrayList<Token> required = new ArrayList<Token>(); 

		for(Token t : tokenization){
			
			//tokens of only the required type have to be in every interpretation
			if(t.isType(tokenType) && (t.noOfTypes() == 1 || (tokenType == Element.TYPE_ATTRIBUTE && !t.isType(Element.TYPE_VALUE))  )){//logic past the OR-operator is a special case for tokens of relation and attribute type
				required.add(t);
			}
			else if(t.isType(tokenType))
				tokensOfType.add(t);

		}
		
		Set<Set<Token>> possibleTokenTypeSets = powerSet(tokensOfType);

		for(Set<Token> rSet : possibleTokenTypeSets){
			for(Token req : required)
				rSet.add(req);
			
			Set<Token> avSet = new HashSet<Token>(tokenization);
			avSet.removeAll(rSet);
		
			
			TokenSetPair tsp = new TokenSetPair(rSet,avSet);
			toRet.add(tsp);

		}
		return toRet;
	}
	
	public static String[] splitWords(String sentence){
		String s = sentence.replaceAll("[\\[\\]]", "");
		//divide into individual words
		return s.split("[_ -]");
	}
	
	/**
	 * Returns true if the token set is a complete tokenization of a stemmed question q
	 * @param tokens
	 * @param q
	 * @return
	 */
	private static boolean isCompleteTokenization(Set<Token> tokens, String[] q){

		List<String> ql = new LinkedList<String>(Arrays.asList(q));
		
		int noOfWh = 0;
		for(Token t : tokens){
			
			if(isWH(t))
				noOfWh++;
			
			for(String word : t.getWords()){
				if(ql.contains(word)){
					ql.remove(word);
				}
				else{
					return false;
				}
			}
		}

		if(ql.size() == 0 && noOfWh == 1)
			return true;
		
		return false;
	}
	
	/**
	 * Return all subsets of a set
	 * credit goes to João Silva: http://stackoverflow.com/questions/1670862/obtaining-a-powerset-of-a-set-in-java
	 * @param originalSet
	 * @return
	 */
	public static <T> Set<Set<T>> powerSet(Set<T> originalSet) {
        Set<Set<T>> sets = new HashSet<Set<T>>();
        if (originalSet.isEmpty()) {
            sets.add(new HashSet<T>());
            return sets;
        }
        List<T> list = new ArrayList<T>(originalSet);
        T head = list.get(0);
        Set<T> rest = new HashSet<T>(list.subList(1, list.size()));
        for (Set<T> set : powerSet(rest)) {
            Set<T> newSet = new HashSet<T>();
            newSet.add(head);
            newSet.addAll(set);
            sets.add(newSet);
            sets.add(set);
        }
        return sets;
    }
	

	public static List<Token> tokenizeString(String s){
		Element nullElem = new Element(Element.TYPE_VALUE,s);
		return tokenizeElement(nullElem,false,false, false,true);
	}
	
	/**
	 * Split element into word stems, remove syntactic markers, make augmented synonym sets,
	 * @param e Element
	 * @param manual if synonyms should be put in manually
	 * @param synonymsForRelAndAtt if synonyms should be generated for relations and attributes
	 * @param synonymsForValues if synonyms should be generated for values
	 * @return return list of tokens. First in the list is the original, non-augmented set
	 */
	public static List<Token> tokenizeElement(Element e, boolean manual, boolean synonymsForRelAndAtt, boolean synonymsForValues, boolean lemmatizeValues){
		
		
		ArrayList<Token> ret = new ArrayList<Token>();
		String[] words = splitWords(e.getName());
		String[] token = new String[words.length];
		
		//possibly stem words
		for(int i = 0; i < words.length; i++){
			String stem = words[i].toLowerCase();
			if(e.getType() != Element.TYPE_VALUE || lemmatizeValues)
				stem = Lexicon.getWordStem(words[i], POS.values());
			token[i] = stem;
		}
		
		//remove syntactic markers
		token = Lexicon.removeSyntacticMarkers(token);
		
		if(token.length == 0)
			return ret;
		
		//make original token
		Token t = new Token(e.getType(),token);
		ret.add(t);
		
		if(!synonymsForRelAndAtt && (e.getType() == Element.TYPE_RELATION || e.getType() == Element.TYPE_ATTRIBUTE))
			return ret;		
		if(!synonymsForValues && e.getType() == Element.TYPE_VALUE)
			return ret;
		

		
		/* make augmented sets of word stems */
		int n = token.length;
		ArrayList<List<String>> syns = new ArrayList<List<String>>(n);
		int nAugSets = 1;
		for(int i = 0; i < n; i++){
			
			if(manual){
				System.out.println("CSV synonyms for "+token[i] +" : ");
				String csv = Input.getLine();
				String[] synoms = csv.split(",");
				
				List<String> synonymsToAdd = Arrays.asList(synoms);
				syns.add(synonymsToAdd);

			}
			else{
				syns.add(Lexicon.getSynonyms(token[i],POS.NOUN,POS.ADJECTIVE,POS.VERB));

			}
			nAugSets = nAugSets * syns.get(i).size();
		}
		
		int[] augSet = new int[n];
		addAllAugmentedSets(e.getType(), ret, syns, augSet, 0);
		return ret;	
	}
	
	
	/**
	 * 
	 * @param tokenType
	 * @param augs The list to be populated by augmented tokens
	 * @param syns The list of lists of synonyms per word in the original token
	 * @param augSet a zeroed vector with the same length as the token
	 * @param i 0
	 */
	private static void addAllAugmentedSets(int tokenType,List<Token> augs , List<List<String>> syns, int[] augSet, int i ){

		
			for(int j = 0; j < syns.get(i).size(); j++){
				
				if(i < augSet.length - 1){
					//recursive step
					addAllAugmentedSets(tokenType, augs, syns, augSet, i+1);
				}
				
				augSet[i] = j;
				ArrayList<String> augmentedSet = new ArrayList<String>();
				for(int k = 0; k < augSet.length; k++){
					
					String s = syns.get(k).get(augSet[k]);
					String[] words = splitWords(s);
					augmentedSet.addAll(Arrays.asList(words));
					Token newToken = new Token(tokenType,augmentedSet);
					augs.add(newToken);
				}
				
				
				
			}
			
		
		
		
	}
	
	
	
	public static String getLabel(Set<Token> relationTokens,Set<Token> attributeTokens, Set<Token> valueTokens, List<Element> ignore){
		return "R:"+((relationTokens != null)?Arrays.toString(relationTokens.toArray()):"[]")+
				" A:"+((attributeTokens != null)?Arrays.toString(attributeTokens.toArray()):"[]")+
				" V:"+((valueTokens != null)?Arrays.toString(valueTokens.toArray()):"[]")+
				" Ignore:"+((ignore != null)?Arrays.toString(ignore.toArray()):"[]");
	}
	
	public static class TokenSetPair{
		
		public Set<Token> aTokens;
		public Set<Token> bTokens;
		
		public TokenSetPair(Set<Token> aTokens, Set<Token> bTokens){
			this.aTokens = aTokens;
			this.bTokens = bTokens;
		}
		
		@Override
		public String toString(){
			return Arrays.toString(aTokens.toArray()) +" " +Arrays.toString(bTokens.toArray());
		}
		
		
	}
	
	
	
	
	

}
