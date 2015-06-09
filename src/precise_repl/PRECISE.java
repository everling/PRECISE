package precise_repl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import precise_repl.Parser.Attachment;
import precise_repl.Tokenizer.TokenSetPair;
import edu.stanford.nlp.util.CoreMap;

public class PRECISE{
	
	public static void queryAttachment(String q, String manualDependencies, boolean onlyManual){
		System.out.println("***** Question: "+q);
		CoreMap question = Parser.getCoreMap(q);
		List<Attachment> dependencies = Parser.getDependencies(question,manualDependencies,true,onlyManual);
		System.out.println("\n***** Attachment from parser:");
		for(Attachment at : dependencies){
	    	  System.out.println(at);
	    }
	}
	
	
	/**
	 * 
	 * @param q Question
	 * @param manualDependencies
	 * @param visualize
	 * @param equivalence
	 * @param onlyManual
	 * @param print
	 * @return SQL queries
	 */
	public static String query(String q, String manualDependencies, boolean visualize, boolean equivalence,boolean onlyManual,boolean print){
		
		List<String> finishedQueries = new ArrayList<String>();
		CoreMap question = Parser.getCoreMap(q);
		List<Attachment> dependencies = Parser.getDependencies(question,manualDependencies,print,onlyManual);
		
		if(print){
			System.out.println("***** Question: "+q);
			System.out.println("\n***** Attachment from parser:");
			for(Attachment at : dependencies){
		    	  System.out.println(at);
		    }
		}


		
		List<Set<Token>> s = Tokenizer.getCompleteTokenizations(question,print);
		
		if(s == null)
			return "";
		
		for(Set<Token> st : s){
			
			List<TokenSetPair> tokenSetRelation = Tokenizer.getAllTokenizationInterpretations(st,Element.TYPE_RELATION);
			
			if(tokenSetRelation == null)
				continue;
			
			for(TokenSetPair tsr : tokenSetRelation){
				
				List<TokenSetPair> tokenSetAttribute = Tokenizer.getAllTokenizationInterpretations(tsr.bTokens, Element.TYPE_ATTRIBUTE);
				
				for(TokenSetPair tsa : tokenSetAttribute){
					
					
					String title = "Complete Interpretation: Rel:" +Arrays.toString(tsr.aTokens.toArray()) +" Att:" +Arrays.toString(tsa.aTokens.toArray()) +" Val:" +Arrays.toString(tsa.bTokens.toArray());
					
					if(print){
						System.out.println("\n"+title);
					}
			
					List<Node> avNodes = Matcher.match(tsr.aTokens, tsa.aTokens, tsa.bTokens, dependencies, null, print,visualize,true);
					if(avNodes != null){
						
						List<String> queries = QueryGenerator.generateQuery(avNodes, dependencies,print);
						
						for(String qq : queries)
							if(!finishedQueries.contains(qq)){
								finishedQueries.add(qq);
								if(print){
									System.out.println(title+" yields query:"+qq+"\n");
								}
							}

						if(equivalence){
							EquivalenceChecker.equivalenceCheck(avNodes, tsr.aTokens, tsa.aTokens, tsa.bTokens, new ArrayList<Element>(), dependencies, finishedQueries,print);
							if(print)
								System.out.println("EQ check done");
						}
							
					}
				}	
			}
		
		}
		
		
		String s1 = "";
		
		for(String qq : finishedQueries){
			s1 += qq +" ";
		}
		EquivalenceChecker.destroyMemoized();
		return s1;
		
	}
	
	
	
	public static class ErrorMsg{
		public String msg;
	}
	
}
