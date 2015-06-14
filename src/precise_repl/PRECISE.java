package precise_repl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import precise_repl.Parser.Attachment;
import precise_repl.Tokenizer.TokenSetPair;
import edu.stanford.nlp.util.CoreMap;

public class PRECISE{
	
	public static void queryAttachment(String q, String manualAttachments, boolean onlyManual){
		System.out.println("***** Question: "+q);
		CoreMap question = Parser.getCoreMap(q);
		List<Attachment> attachments = Parser.getDependencies(question,manualAttachments,true,onlyManual);
		System.out.println("\n***** Attachment from parser:");
		for(Attachment at : attachments){
	    	  System.out.println(at);
	    }
	}
	
	
	/**
	 * 
	 * @param q Question
	 * @param manualAttachments
	 * @param visualize
	 * @param equivalence
	 * @param onlyManual
	 * @param print
	 * @return SQL queries
	 */
	public static String query(String q, String manualAttachments, boolean visualize, boolean equivalence,boolean onlyManual,boolean print, ErrorMsg err){
		
		List<String> finishedQueries = new ArrayList<String>();
		CoreMap question = Parser.getCoreMap(q);
		List<Attachment> attachments = Parser.getDependencies(question,manualAttachments,print,onlyManual);
		
		if(print){
			System.out.println("***** Question: "+q);
			System.out.println("\n***** Attachment from parser:");
			for(Attachment at : attachments){
		    	  System.out.println(at);
		    }
		}


		
		List<Set<Token>> s = Tokenizer.getCompleteTokenizations(question,print,err);
		
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
			
					List<Node> avNodes = Matcher.match(tsr.aTokens, tsa.aTokens, tsa.bTokens, attachments, null, print,visualize,err);
					
					if(avNodes == null)
						err.msg = "No valid mapping";
					
					if(avNodes != null){
						
						List<String> queries = QueryGenerator.generateQuery(avNodes,tsr.aTokens, attachments,print);
						
						for(String qq : queries)
							if(!finishedQueries.contains(qq)){
								finishedQueries.add(qq);
								if(print){
									System.out.println(title+" yields query:"+qq+"\n");
								}
							}

						if(equivalence){
							if(print)
								System.out.println("Examining equivalent flows..");
							EquivalenceChecker.equivalenceCheck(avNodes, tsr.aTokens, tsa.aTokens, tsa.bTokens, new ArrayList<Element>(), attachments, finishedQueries,print,visualize);
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
