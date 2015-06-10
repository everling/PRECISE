package precise_repl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import edu.mit.jwi.item.POS;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import edu.stanford.nlp.util.CoreMap;

public class Parser {
	
	private static Properties props;
	private static StanfordCoreNLP pipeline;
	
	public static void init(){
	    props = new Properties();
	    props.setProperty("annotators", "tokenize, ssplit, pos, lemma, parse");
	    pipeline = new StanfordCoreNLP(props);
	}
	
	/**
	 * Returns list of words as CoreMap
	 * @param text
	 * @return
	 */
	public static CoreMap getCoreMap(String text){
		
	    Annotation document = new Annotation(text);
	    pipeline.annotate(document);
	    List<CoreMap> sentences = document.get(SentencesAnnotation.class);
	    
	    for(CoreMap sentence: sentences) {
	    	return sentence;
	    }
	    return null;
	}
	
	/**
	 * Return the words, sans punctuation
	 * @param sentence
	 * @return
	 */
	public static List<CoreLabel> getWords(CoreMap sentence){
		List<CoreLabel> list = sentence.get(TokensAnnotation.class);
		
		ArrayList<CoreLabel> toRem = new ArrayList<CoreLabel>();
		
		for(CoreLabel word : list)
			if(word.get(PartOfSpeechAnnotation.class).equals("."))
				toRem.add(word);
		
		for(CoreLabel word : toRem)
			list.remove(word);
		
		return list;
	}
	/**
	 * Compiles Stanford dependecies as a list of Attachments. Note: this method is hacky and not complete, it might not correctly
	 * recognize multi-word tokens. Always check the output in the GUI, use manually set mappings.
	 * 
	 * @param sentence
	 * @param manualAdditions String of manual mappings. Format: "what_state,state_san francisco" for {[what,state],[state,san francisco]}
	 * @param print if true, log messages are sent to standard output.
	 * @param onlyManual if true, only parse the manualAdditions string.
	 * @return
	 */
	public static List<Attachment> getDependencies(CoreMap sentence, String manualAdditions, boolean print, boolean onlyManual){
		SemanticGraph dependencies = sentence.get(CollapsedCCProcessedDependenciesAnnotation.class);

		Set<SemanticGraphEdge> edgeSet = dependencies.getEdgeSet();
		
		HashMap<Integer,List<Token>> idMapping = new HashMap<Integer,List<Token>>();
		List<Attachment> attachments = new ArrayList<Attachment>();
		
		//find nn-dependencies. these should be tokenized together
		if(print)
			System.out.println("\n***** Stanford dependencies:");
		
		for(SemanticGraphEdge sge : edgeSet){
			if(print)
				System.out.println(""+sge +" " +sge.getGovernor() +" " +sge.getDependent());
			String relation = sge.getRelation().toString();
			/*if(relation.equals("nn")){
				List<Token> tokens = Tokenizer.tokenizeString(sge.getDependent().originalText().toLowerCase() +" " +sge.getGovernor().originalText().toLowerCase());
				int govID  = sge.getGovernor().index();
				int depID = sge.getDependent().index();
				idMapping.put(govID, tokens);
				idMapping.put(depID, tokens);
			}	*/	
		}
		
		HashMap<Integer,List<Token>> semanticMarkerFuse = new HashMap<Integer,List<Token>>();
		
		
		//the rest, attachment mappings
		for(SemanticGraphEdge sge : edgeSet){
			if(true){
				//look for existing tokens first
				List<Token> govTokens = idMapping.get(sge.getGovernor().index());
				List<Token> depTokens = idMapping.get(sge.getDependent().index());
				
				if(govTokens == null){
					//build new token
					govTokens = Tokenizer.tokenizeString(sge.getGovernor().originalText().toLowerCase());
					if(govTokens.size() > 0)
						idMapping.put(sge.getGovernor().index(), govTokens);
					
				}
				if(depTokens == null){
					//build new token
					depTokens = Tokenizer.tokenizeString(sge.getDependent().originalText().toLowerCase());
					if(depTokens.size() > 0)
						idMapping.put(sge.getDependent().index(), depTokens);
				}
				
				//treat edges that involve semantic markers
				if(govTokens.size() == 0 && depTokens.size() > 0){
					
					Integer govKey = sge.getGovernor().beginPosition();
					List<Token> tryGov = semanticMarkerFuse.get(govKey);
					if(tryGov != null)
						govTokens = tryGov;
					else
						semanticMarkerFuse.put(govKey,depTokens);
				}
				else if(govTokens.size() > 0 && depTokens.size() == 0){
					Integer depKey = sge.getDependent().beginPosition();
					List<Token> tryDep = semanticMarkerFuse.get(depKey);
					if(tryDep != null)
						depTokens = tryDep;
					else
						semanticMarkerFuse.put(depKey,depTokens);
				}
				
				
				if(govTokens.size() > 0 && depTokens.size() > 0){
					Attachment at = new Attachment(govTokens,depTokens,sge.getRelation().toString());
					attachments.add(at);
				}
				
			}
		}
		
		try{
			
			if(onlyManual)
				attachments = new ArrayList<Attachment>();
			
			if(manualAdditions != null && manualAdditions.length() > 0){
				String[] split = manualAdditions.split(",");
				
				for(String addition : split){
					String[] gov_dep = addition.split("_");
					
					
					if(gov_dep[0].startsWith("-")){
						//delete
						
						List<Token> govTokens = Tokenizer.tokenizeString(gov_dep[0].substring(1, gov_dep[0].length()));
						List<Token> depTokens = Tokenizer.tokenizeString(gov_dep[1]);
						Attachment at = new Attachment(govTokens,depTokens,"custom");
						Attachment rem = null;
						for(Attachment att : attachments){
							if(att.isSame(at))
								rem = att;
						}
						if(rem != null)
							attachments.remove(rem);

					}
					else{
						List<Token> govTokens = Tokenizer.tokenizeString(gov_dep[0]);
						List<Token> depTokens = Tokenizer.tokenizeString(gov_dep[1]);
						
						Attachment at = new Attachment(govTokens,depTokens,"custom");
						attachments.add(at);
					}
				}
				
			}
		}catch(Exception e){
			System.out.println("error parsing manual attachments");
			e.printStackTrace();
		}

		return attachments;
	}
	
	
	public static POS getWordnetPOS(CoreLabel word){
		
		String stanfordPOS = word.getString(PartOfSpeechAnnotation.class);
		
		if(stanfordPOS.startsWith("N"))
			return POS.NOUN;
		if(stanfordPOS.startsWith("J"))
			return POS.ADJECTIVE;
		if(stanfordPOS.startsWith("V"))
			return POS.VERB;
		if(stanfordPOS.startsWith("R"))
			return POS.ADVERB;
		
		return null;
		
		
	}

	/**
	 * 
	 * Attachment mappings represented by two sets of tokens.
	 *
	 */
	public static class Attachment{
		
		private List<Token> tokenA;
		private List<Token> tokenB;
		private boolean isWH;
		
		
		public Attachment(List<Token> a, List<Token> b, String type){
			tokenA = a;
			tokenB = b;
			
			for(Token ta : tokenA){
				if(Tokenizer.isWH(ta)){
					isWH = true;
					break;
				}
			}
			if(!isWH){
				for(Token ta : tokenB){
					if(Tokenizer.isWH(ta)){
						isWH = true;
						break;
					}
				}	
			}
			
		}
	
		public boolean containsTokenInSetA(Token a){
			return tokenA.contains(a);
		}
		
		public boolean containsTokenInSetB(Token b){
			return tokenB.contains(b);
		}
		
		/**
		 * Check if element a can be matched from one of the tokens in tokenA/B.
		 * @param a
		 * @param B if false: search set tokenA. true: search set tokenB.
		 * @return 
		 */
		public boolean canDeriveElementFromTokens(Element a, boolean B){
			
			List<Token> tokens = this.tokenA;
			
			if(B)
				tokens = this.tokenB;
			
			
			for(Token t : tokens){
				for(String s : t.getWords()){
					List<Token> sParticipate = Lexicon.getTokens(s);
					if(sParticipate != null){
						if(Lexicon.canDeriveElementFromToken(sParticipate, a))
							return true;
					}
				}
			}
			
			return false;
			//return Lexicon.canDeriveElementFromToken(tokens, a);
		}
		


		/**
		 * Checks whether a given element A is a value/attribute of a relation derived from tokenA/B
		 * @param a
		 * @param B if false: search set tokenA. true: search set tokenB.
		 * @return
		 */
		public boolean isAttachmentRelation(Element a ,boolean B){
			List<Token> tokenA = this.tokenA;
			
			if(B){
				tokenA = this.tokenB;
			}
			
			if(a.getCompatible() == null || a.getCompatible().size() == 0)
				return false;
			
			for(Token aToken : tokenA){
				List<Element> aElems = Lexicon.getElements(aToken);
				if(aElems == null)
					 continue;
				for(Element ae : aElems){
					if(ae.getType() == Element.TYPE_RELATION && a.getCompatible().contains(ae))
						return true;
					
				}
			}		
		return false;
		}
		
		
		/**
		 * Checks whether given attributeElement is the primary key of a relation derived from tokenA/B
		 * @param attributeElement 
		 * @param B if false: search set tokenA. true: search set tokenB.
		 * @return
		 */
		public boolean isAttachmentPrimaryKey(Element attributeElement, boolean B){
			List<Token> tokenA = this.tokenA;
			
			if(B){
				tokenA = this.tokenB;
			}
			
			for(Token t : tokenA){
				
				if(Tokenizer.isWH(t))
					continue;
				
				List<Element> elems = Lexicon.getElements(t);
				if(elems == null)
					continue;
				
				for(Element e : elems){
					if(e.getType() == Element.TYPE_RELATION){
						
						String primaryKey = e.getPrimaryKey();
						List<Element> schema = e.getSchema();
						
						
						
						if(primaryKey != null && schema != null && primaryKey.equals(attributeElement.getName()) && schema.contains(attributeElement))
							return true;
					}
				}
			}
			return false;
		}
		
		/**
		 * check whether a given element A is a value/attribute/relation of the same relation of the elements derived from tokenA/B.
		 * @param a
		 * @param B if false: search set tokenA. true: search set tokenB.
		 * @return
		 */
		public boolean isAttachmentSameRelation(Element a ,boolean B){
			
			List<Token> tokenA = this.tokenA;
			
			if(B){
				tokenA = this.tokenB;
			}
			
				Element aRelation = null;
				for(Element r : a.getCompatible()){
					if(r.getType() == Element.TYPE_RELATION){
						aRelation = r;
						break;
					}
				}
	
				if(aRelation == null && a.getType() != Element.TYPE_RELATION)
					return false;
				else if(aRelation == null)
					aRelation = a;
				
				for(Token aToken : tokenA){
					List<Element> aElems = Lexicon.getElements(aToken);
					if(aElems == null)
						 continue;
					for(Element ae : aElems){
						List<Element> comp = ae.getCompatible();
						for(Element c : comp){
							if(c.getType() == Element.TYPE_RELATION && c.equals(aRelation))
								return true;
						}
						if(ae.getType() == Element.TYPE_RELATION && ae.equals(aRelation))
							return true;
					}
				}		

			return false;
		}
		
		
		/**
		 * @param type
		 * @param B if false: search set tokenA. true: search set tokenB.
		 * @return true if a token in the attachment refers to a certain element type
		 */
		public boolean tokenRefersToElementOfType(int type, boolean B){
			
			List<Token> tokenA = this.tokenA;
			
			if(B)
				tokenA = tokenB;
			
			for(Token t : tokenA){
				if(!Tokenizer.isWH(t)){
					List<Element> elements = Lexicon.getElements(t);
					if(elements != null){
						for(Element e: elements){
							if(e.getType() == type)
								return true;
						}
					}
				}
			}
			return false;
		}

		/**
		 * @param B if false: search set tokenA. true: search set tokenB.
		 * @return true if a token in tokenA/B refers to a primary key value of a relation.
		 */
		public List<Element> tokenRefersToValueOfPrimaryKey(boolean B){
			
			List<Element> toRet = new ArrayList<Element>();

			List<Token> tokenA = this.tokenA;
			
			if(B)
				tokenA = tokenB;
			
			for(Token t : tokenA){
				if(!Tokenizer.isWH(t)){
					List<Element> elements = Lexicon.getElements(t);
					if(elements != null){
						for(Element e: elements){
							if(e.getType() == Element.TYPE_VALUE){
								
								List<Element> compatible = e.getCompatible();
								
								if(compatible != null){
									
									Element r = null;
									Element a = null;
									
									for(Element c : compatible){
										if(c.getType() == Element.TYPE_RELATION)
											r = c;
										else if(c.getType() == Element.TYPE_ATTRIBUTE);
											a = c;
									}
									
									//find out of a is primary key of r
									if(r != null && a != null){
										
										List<Element> schema = r.getSchema();
	
										if(r.getPrimaryKey() != null && r.getPrimaryKey().equals(a.getName()) && schema != null && schema.contains(a))
												toRet.add(r);
									}	
								}
							}
						}
					}
				}
			}
			return toRet;
		}
		

		public boolean isWH(){
			return isWH;
		}
		
		@Override
		public String toString(){
			
			if(tokenA.size() > 0 && tokenA.get(0).getWords().get(0).equals("what")){
				return "Attachment WH:" +tokenA.get(0) +" "+Arrays.toString(tokenB.toArray());
			}
			
			
			if(tokenA.size() > 0 && tokenB.size() > 0)
			return "Attachment:" +tokenA.get(0) +" "+tokenB.get(0);
			return "Attachment: null";
			
		}
		
		public boolean isSame(Attachment at){
			if(tokenA.equals(at.tokenA) && tokenB.equals(at.tokenB))
				return true;
			return false;
		}
		
		
	}
	
}


