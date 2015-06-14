package precise_repl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;



public class Token implements java.io.Serializable{

	private static final long serialVersionUID = 1L;
	private List<String> words;
	private int[] types = new int[4];
	
	private transient List<Integer> indexInSentence;
	
	public List<String> getWords(){
		return words;
	}
	
	public void addIndex(int i){
		if(indexInSentence == null)
			indexInSentence = new ArrayList<Integer>();
		if(!indexInSentence.contains(i))
			indexInSentence.add(i);
	}
	
	public List<Integer> getIndex(){
		if(indexInSentence == null)
			indexInSentence = new ArrayList<Integer>();
		return indexInSentence;
	}
	
	public void clearIndex(){
		indexInSentence = new ArrayList<Integer>();
	}
	
	public Token(int type, String... s){
		types[0] = type;
		words = Arrays.asList(s);
		Collections.sort(words);
	}
	public Token(int type, List<String> s){
		types[0] = type;
		words = s;
		Collections.sort(words);
	}
	
	public boolean isType(int type){
		for(int i : types){
			if(i == type)
				return true;
		}
		return false;
	}
	
	public void addType(int type){
		
		if(isType(type))
			return;
		
		for(int i =0; i < types.length; i++){
			if(types[i] == 0){
				types[i] = type;
				return;
			}
		}
	}
	
	public int noOfTypes(){
		int ret = 0;
		
		for(int type : types ){
			if(type > 0)
				ret++;
		}
		return ret;
	}
	
	@Override
	public int hashCode(){
		return words.hashCode();
	}
	
	
	private String getTypeMarker(){
		String typeM = "T";
		
		for(int i : types){
			switch(i){
			case Element.TYPE_RELATION:
				typeM += "R";
				break;
			case Element.TYPE_ATTRIBUTE:
				typeM +=  "A";
				break;
			case Element.TYPE_VALUE:
				typeM +="V";
				break;
			}
		}
		return typeM;
	}
	
	
	@Override
	public String toString(){
		
		
		return getTypeMarker() + Arrays.toString(words.toArray());
	}
	
	
	@Override
	/**
	 * Only checks the word set. It doesn't matter whether the token is a relation, attribute or value token.
	 */
	public boolean equals(Object o){
		
		Token to = (Token)o;
		return words.equals(to.words);
		
	}
	
}
