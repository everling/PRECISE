package precise_repl;

import java.util.ArrayList;
import java.util.List;

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
	 * @param ignoredAttributes
	 * @param dependencies
	 * @param finishedQueries
	 * @param print
	 */
	public static void equivalenceCheck(List<Node> avNodes, TokenSetPair tsr, TokenSetPair tsa, List<Element> ignoredAttributes, List<Attachment> dependencies, List<String> finishedQueries, boolean print){
		
		if(print)
			System.out.println("Examining equivalent flows..");

		if(addMemoization(ignoredAttributes))
			return;
		
		List<Node> activeAttributeNodes = new ArrayList<Node>();
		for(Node n : avNodes){
			if(n.getColumn().equals("EA"))
				activeAttributeNodes.add(n);
		}
		
		for(Node n : activeAttributeNodes){
			List<Element> ignoreAttributes = new ArrayList<Element>(ignoredAttributes);
			ignoreAttributes.add(n.getElement());
			List<Node> avNodesEQ = Matcher.match(tsr.aTokens, tsa.aTokens, tsa.bTokens, dependencies, ignoreAttributes, print,false,false);
			if(avNodesEQ != null){
				if(print)
					System.out.println("Equivalent max flow found:");
				
				List<String> eqQueries =QueryGenerator.generateQuery(avNodesEQ, dependencies,print);
				
				for(String qq : eqQueries)
					if(!finishedQueries.contains(qq))
						finishedQueries.add(qq);
				
				equivalenceCheck(avNodesEQ, tsr, tsa, ignoreAttributes, dependencies, finishedQueries,print);
			}
		}
	}
	

}
