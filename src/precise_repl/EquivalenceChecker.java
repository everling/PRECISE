package precise_repl;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import precise_repl.Parser.Attachment;
import precise_repl.Tokenizer.TokenSetPair;

public class EquivalenceChecker {
	
	
	private static List<List<Element>> memoization = new ArrayList<List<Element>>();
	
	/**
	 * @param ignoredAttributes
	 * @return true if ignored attribute set already added
	 */
	private static boolean addMemoization(List<Element> ignoredAttributes){
		
		for(List<Element> mem : memoization){
			if(mem.containsAll(ignoredAttributes) && ignoredAttributes.containsAll(mem))
				return true;
		}

		memoization.add(ignoredAttributes);
		return false;
	}
	
	public static void destroyMemoized(){
		memoization = new ArrayList<List<Element>>();
	}
	
	
	/**
	 * Examines if the attribute-value graph has alternative max-flow solutions
	 * @param avNodes
	 * @param tsr
	 * @param tsa
	 * @param ignoredElements
	 * @param dependencies
	 * @param finishedQueries
	 * @param print
	 */
	public static void equivalenceCheck(List<Node> avNodes, Set<Token> relationTokens, Set<Token> attributeTokens, Set<Token> valueTokens, List<Element> ignoredElements, List<Attachment> dependencies, List<String> finishedQueries, boolean print){
		
		PRECISE.ErrorMsg msg = new PRECISE.ErrorMsg();
		
		if(print)
			System.out.println("Examining equivalent flows..");

		if(addMemoization(ignoredElements))
			return;
		
		List<Node> activeAttributeNodes = new ArrayList<Node>();
		for(Node n : avNodes){
			if(n.getColumn().equals("EA") || n.getColumn().equals("EV"))
				activeAttributeNodes.add(n);
		}
		
		for(Node n : activeAttributeNodes){
			List<Element> ignoreElements = new ArrayList<Element>(ignoredElements);
			ignoreElements.add(n.getElement());
			List<Node> avNodesEQ = Matcher.match(relationTokens, attributeTokens, valueTokens, dependencies, ignoreElements, print,false,msg);
			if(avNodesEQ != null){
				if(print)
					System.out.println("Equivalent max flow found:");
				
				List<String> eqQueries =QueryGenerator.generateQuery(avNodesEQ, dependencies,print);
				
				for(String qq : eqQueries)
					if(!finishedQueries.contains(qq))
						finishedQueries.add(qq);
				
				equivalenceCheck(avNodesEQ, relationTokens, attributeTokens, valueTokens, ignoreElements, dependencies, finishedQueries,print);
			}
		}
	}
	

}
