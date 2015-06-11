package precise_repl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.jgrapht.alg.*;
import org.jgrapht.graph.*;

import precise_repl.Parser.Attachment;

public class Matcher {

	/**
	 * 
	 * @param relationTokens
	 * @param attributeTokens
	 * @param valueTokens
	 * @param attachments
	 * @param elementsToIgnore
	 * @param print
	 * @param visualize
	 * @param firstLevel
	 * @return a list of nodes if valid mapping is found
	 */
	public static List<Node> match(Set<Token> relationTokens,Set<Token> attributeTokens, Set<Token> valueTokens, List<Attachment> attachments, List<Element> elementsToIgnore, boolean print, boolean visualize, PRECISE.ErrorMsg err){
		
		
		if(attributeTokens.size() > valueTokens.size()){
			if(print)
				System.out.println("More attribute tokens than value tokens (" +Tokenizer.getLabel(relationTokens, attributeTokens, valueTokens,elementsToIgnore )+")");
			return null;
		}

		//build avGraph
		ListenableDirectedWeightedGraph<Node,DefaultWeightedEdge>  avGraph = attributeValueGraph(relationTokens, attributeTokens, valueTokens, attachments, elementsToIgnore, print);

		//make max flow of av
		EdmondsKarpMaximumFlow<Node, DefaultWeightedEdge> avFlow = maxFlow(avGraph,print);

		//no flow solution, no mapping
		if(avFlow == null){
			if(print)
				System.out.println("No max flow, incomplete graph (" +Tokenizer.getLabel(relationTokens, attributeTokens, valueTokens,elementsToIgnore)+")");

			if(visualize)
				MatchVisualizer.visualize(Tokenizer.getLabel(relationTokens, attributeTokens, valueTokens,elementsToIgnore)+" flow: 0", avGraph);

			return null;

		}

		long avFlowVal = Math.round(avFlow.getMaximumFlowValue());

		//insufficient flow, not a valid mapping
		long avCapacity = 0;
		
		for(Token t : valueTokens){
			if(t.isType(Element.TYPE_VALUE))
				avCapacity++;
		}
		
		
		if(avFlowVal < avCapacity){
			if(print)
				System.out.println("max flow less than value token amount (" +Tokenizer.getLabel(relationTokens, attributeTokens, valueTokens,elementsToIgnore)+")");

			if(visualize)
				MatchVisualizer.visualize(Tokenizer.getLabel(relationTokens, attributeTokens, valueTokens,elementsToIgnore)+" flow: "+Math.round(avFlowVal) +"/"+avCapacity, avGraph);
			return null;
		}


		//no relation-attribute constraints, done
		if(relationTokens.size() == 0){
			List<Node> avNodes = mapNodes(avGraph, avFlow);

			if(visualize)
				MatchVisualizer.visualize(Tokenizer.getLabel(relationTokens, attributeTokens, valueTokens,elementsToIgnore)+" flow: "+Math.round(avFlowVal) +"/"+avCapacity, avGraph);
			
			return avNodes;
		}

		//get active attributes from the avGraph 
		List<Element> activeAttribsAV = getActiveElements(avGraph, avFlow,"EA");

		//make relation graph
		ListenableDirectedWeightedGraph<Node,DefaultWeightedEdge> rGraph = relationGraph(relationTokens, activeAttribsAV, attachments);

		//compute max flow
		EdmondsKarpMaximumFlow<Node, DefaultWeightedEdge> rFlow = maxFlow(rGraph, print);

		
		
		if(rFlow == null || rFlow.getMaximumFlowValue() < relationTokens.size()){
			
			if(print)
				System.out.println("Bad relation - attribute correspondence for :"+Tokenizer.getLabel(relationTokens, attributeTokens, valueTokens,elementsToIgnore));
			//find the attributes that weren't matched to a relation, create new graphs recursively
			
			if(elementsToIgnore == null)
				elementsToIgnore = new ArrayList<Element>();

			if(rFlow == null){
				//all attribute tokens were unmatched
				for(Element ae : activeAttribsAV){
					List<Element> attributesToIgnoreRec = new ArrayList<Element>(elementsToIgnore);
					attributesToIgnoreRec.add(ae);
					List<Node> newMaxFlow = match(relationTokens, attributeTokens, valueTokens, attachments, attributesToIgnoreRec, print,visualize,err);
					if(newMaxFlow != null)
						return newMaxFlow;
				}
			}
			else{
				//some attributes were matched, make new max flows by removing some of the unmatched ones
				List<Node> activeRelationAndAttribNodes = mapNodes(rGraph, rFlow);
				ArrayList<Element> unmatchedAttribs = new ArrayList<Element>(activeAttribsAV);

				for(Node n : activeRelationAndAttribNodes){
					if(n.getColumn().equals("ER")){
						for(Element ae : activeAttribsAV){
							if(ae.getCompatible().contains(n.getElement()))
								unmatchedAttribs.remove(ae);
						}
					}
				}

				for(Element ae : unmatchedAttribs){
					List<Element> attributesToIgnoreRec = new ArrayList<Element>(elementsToIgnore);
					attributesToIgnoreRec.add(ae);
					List<Node> newMaxFlow = match(relationTokens, attributeTokens, valueTokens, attachments, attributesToIgnoreRec, print,visualize,err);
					if(newMaxFlow != null){
						if(visualize){
							MatchVisualizer.visualize(Tokenizer.getLabel(relationTokens, attributeTokens, valueTokens,elementsToIgnore)+" flow: "+"/"+valueTokens.size(), avGraph);

						}
						return newMaxFlow;
					}
				}
			}

		}
		else{
			//relation flow is equal to relation token size, ie all relation tokens are matched
			if(print)
				System.out.println("This works: "+Tokenizer.getLabel(relationTokens, attributeTokens, valueTokens,elementsToIgnore));
			List<Node> avNodes = mapNodes(avGraph, avFlow);

			if(visualize){
				MatchVisualizer.visualize(Tokenizer.getLabel(relationTokens, attributeTokens, valueTokens,elementsToIgnore )+" flow: "+Math.round(avFlowVal) +"/"+valueTokens.size(), avGraph);
				MatchVisualizer.visualize(Tokenizer.getLabel(relationTokens, attributeTokens, valueTokens,elementsToIgnore )+" flow: "+Math.round(avFlowVal) +"/"+valueTokens.size(), rGraph);
			}
			return avNodes;

		}

		return null;

	}


	/**
	 * Produces the attribute-value graph described in Popescu et al., 2003. 
	 * 
	 * Uses JGraphT library since it has a working max-flow implementation, as well
	 * as visualization.
	 * 
	 * 
	 * S | Value tokens | Value elements | Attribute elements 1 | Attribute elements 2 | Attribute tokens | I/E | T
	 * 
	 * Value tokens are supplied by the method parameter valueTokens.
	 * Value elements are derived from calls to the Lexicon using the value tokens.
	 * Attribute elements 1 are derived from the value elements
	 * Attribute elements 2 are derived from attribute elements 1
	 * Attribute tokens are supplied by the method parameter attributeTokens
	 * 
	 * @param relationTokens
	 * @param attributeTokens
	 * @param valueTokens
	 * @return
	 */
	private static ListenableDirectedWeightedGraph<Node,DefaultWeightedEdge> attributeValueGraph(Set<Token> relationTokens,Set<Token> attributeTokens, Set<Token> valueTokens, List<Attachment> dependencies, List<Element> attributesToIgnore, boolean print){

		ListenableDirectedWeightedGraph<Node, DefaultWeightedEdge> avGraph = new ListenableDirectedWeightedGraph<Node, DefaultWeightedEdge>(DefaultWeightedEdge.class);



		Node S = new Node("S");//source
		S.setColumn("S");
		Node T = new Node("T"); //sink
		T.setColumn("T");
		Node E = new Node("E"); //explicit attributes
		E.setColumn("EI");
		Node I = new Node("I"); //implicit attributes
		I.setColumn("EI");

		avGraph.addVertex(S);
		avGraph.addVertex(T);
		avGraph.addVertex(E);
		avGraph.addVertex(I);

		//first column, value tokens
		//tokens have to be distinct so we don't need to do containment checks 
		List<Node> tvColumn = new ArrayList<Node>();
		for(Token t : valueTokens){
			if(t.isType(Element.TYPE_VALUE)){
				Node tv = new Node(t);
				tv.setColumn("TV");
				avGraph.addVertex(tv);

				//source edge
				avGraph.addEdge(S, tv);
				tvColumn.add(tv);
			}
		}

		//second column, value elements
		//duplicate elements may occur, so we need to do containment checks
		List<Node> evColumn = new ArrayList<Node>();
		for(Node tv : tvColumn){

			int successes = 0;

			List<Element> elements = Lexicon.getElements(tv.getToken());
			for(Element e : elements){

				if(e.getType() != Element.TYPE_VALUE)
					continue;
				
				if(attributesToIgnore != null && attributesToIgnore.contains(e))
					continue;
				successes++;

				Node ev = getNodeContainingElement(evColumn,e);
				if(ev == null){
					ev = new Node(e);
					ev.setColumn("EV");
					evColumn.add(ev);
				}
				avGraph.addVertex(ev);
				avGraph.addEdge(tv, ev);
			}

			if(successes == 0 && print)
				System.out.println("No value element passed the constraints for " +tv);

		}


		//third column, attribute elements 1
		//duplicate elements may occur, so we need to do containment checks
		List<Node> ea1 = new ArrayList<Node>();
		for(Node ev : evColumn){

			int success = 0;

			for(Element comp : ev.getElement().getCompatible()){
				if(comp.getType() == Element.TYPE_ATTRIBUTE){

					if(attributesToIgnore != null && attributesToIgnore.contains(comp))
						continue;

					boolean implicitAttribute = !Lexicon.canDeriveElementFromToken(attributeTokens, comp);


					if(Lexicon.isWH(ev.getElement())){
						Token wh = new Token(Element.TYPE_VALUE,ev.getElement().getName());
						
						
						if(implicitAttribute)
							continue;						
						
						if(!respectsAttachmentV2(dependencies, ev.getElement(), comp,true))
							continue;
					}
					else{
						
						if(implicitAttribute){
							
							Element rel = comp.getCompatibleOfType(Element.TYPE_RELATION);
							
							if(rel != null){
								if(!respectsAttachmentV2(dependencies,rel,ev.getElement(),false)){
									
									//PRIMARY KEY TEST
									String pKey = rel.getPrimaryKey();
									if(!(comp.getName().equals(pKey)))
										continue;

								}
							}
							else{								
								continue;

							}
							
						}else{
							//explicit attribute
							if(!respectsAttachmentV2(dependencies,comp,ev.getElement(),false))
								continue;
						}
					}
					
					Node ea = getNodeContainingElement(ea1, comp);

					if(ea == null){
						ea = new Node(comp);
						ea.setColumn("EA");
						ea1.add(ea);
					}
					avGraph.addVertex(ea);
					avGraph.addEdge(ev, ea);
					success++;

				}
			}
			if(success == 0 && print){
				System.out.println("No attribute element match constraints for " +ev);
			}
		}

		//fourth column, duplicate nodes from third column, draw edge between each corresponding
		List<Node> ea2 = new ArrayList<Node>();
		for(Node ea : ea1){
			Node n_ea = new Node(ea.getElement());
			n_ea.setColumn("EA");
			avGraph.addVertex(n_ea);
			avGraph.addEdge(ea, n_ea);
			ea2.add(n_ea);

			//add edge to implicit
			avGraph.addEdge(n_ea, I);

		}

		//fifth column, attribute tokens
		for(Token t : attributeTokens){
			if(t.isType(Element.TYPE_ATTRIBUTE)){

				Node at = new Node(t);
				at.setColumn("AT");
				avGraph.addVertex(at);

				//add edge to explicit 
				avGraph.addEdge(at, E);

				for(Element e : Lexicon.getElements(t)){
					if(e.getType() == Element.TYPE_ATTRIBUTE){

						Node n_ea = getNodeContainingElement(ea2, e);
						if(n_ea != null){
							if(!avGraph.containsEdge(n_ea, at)){
									//add edge
									avGraph.addEdge(n_ea, at);

								//}
							}
						}
					}
				}
			}
		}

		//sixth column, E and I

		double capacityEtoSink = attributeTokens.size();
		double capacityItoSink = valueTokens.size() - attributeTokens.size();

		DefaultWeightedEdge eToSink = avGraph.addEdge(E, T);
		DefaultWeightedEdge iToSink = avGraph.addEdge(I, T);

		avGraph.setEdgeWeight(eToSink, capacityEtoSink);
		avGraph.setEdgeWeight(iToSink, capacityItoSink);


		return avGraph;
	}

	
	private static Node getNode(String nodeName, ListenableDirectedWeightedGraph<Node,DefaultWeightedEdge> graph){
		for(DefaultWeightedEdge dwe :graph.edgeSet()){
			Node n = graph.getEdgeSource(dwe);

			if(n.toString().equals(nodeName))
				return n;
			n = graph.getEdgeTarget(dwe);

			if(n.toString().equals(nodeName))
				return n;

		}
		return null;
	}

	
	/**
	 * 
	 * @param graph
	 * @param print debug
	 * @return a max-flow solution to a given graph
	 */
	private static EdmondsKarpMaximumFlow<Node, DefaultWeightedEdge>  maxFlow(ListenableDirectedWeightedGraph<Node,DefaultWeightedEdge> graph, boolean print){
		Node s = getNode("S",graph);
		Node t = getNode("T",graph);

		if(s == null || t == null){
			if(print)
				System.out.println("Incomplete");
			return null;
		}
		EdmondsKarpMaximumFlow<Node, DefaultWeightedEdge> emf = new EdmondsKarpMaximumFlow<Node, DefaultWeightedEdge>(graph);
		emf.calculateMaximumFlow(s, t);
		return emf;
	}

	/**
	 * Generate a Node representation of the JGraphT max-flow result
	 * @param graph
	 * @param emf
	 */
	private static List<Node> mapNodes(ListenableDirectedWeightedGraph<Node,DefaultWeightedEdge> graph,EdmondsKarpMaximumFlow<Node, DefaultWeightedEdge> emf){

		List<Node> toRet = new ArrayList<Node>();

		for(DefaultWeightedEdge dwe : graph.edgeSet()){
			for(Entry<DefaultWeightedEdge, Double> mdwep : emf.getMaximumFlow().entrySet()){
				if(mdwep.getKey() == dwe){
					if(mdwep.getValue() > 0.5){
						Node s = graph.getEdgeSource(dwe);
						Node t = graph.getEdgeTarget(dwe);
						if(s != null)
							s.setNext(t);
						if(t != null)
							t.setPrevious(s);
						if(s != null && !toRet.contains(s))
							toRet.add(s);
						if(s != null && !toRet.contains(t))
							toRet.add(t);

					}
				}
			}
		}
		return toRet;
	}



	/**
	 * Constructs a flow graph with the structure
	 * S | relation tokens | relations compatible with active elements | T
	 * All edges have capacity 1. 
	 * 
	 * @param relationTokens
	 * @param activeAttributes
	 */
	private static ListenableDirectedWeightedGraph<Node, DefaultWeightedEdge> relationGraph(Set<Token> relationTokens, List<Element> activeAttributes, List<Attachment> attachments){
		
		ListenableDirectedWeightedGraph<Node, DefaultWeightedEdge> rGraph = new ListenableDirectedWeightedGraph<Node, DefaultWeightedEdge>(DefaultWeightedEdge.class);

		Node S = new Node("S");//source
		S.setColumn("S");

		rGraph.addVertex(S);
		
		
		//relation tokens
		List<Node> rt = new ArrayList<Node>();
		for(Token t : relationTokens){
			Node tr = new Node(t);
			tr.setColumn("TR");
			rGraph.addVertex(tr);
			rGraph.addEdge(S, tr);
			rt.add(tr);
		}

		//active element relations
		List<Node> aar = new ArrayList<Node>();
		for(Element e : activeAttributes){
			for(Element comp : e.getCompatible()){
				if(comp.getType() == Element.TYPE_RELATION){
					
					Node er = getNodeContainingElement(aar, comp);
					if(er == null){
						er = new Node(comp);
						er.setColumn("ER");
						aar.add(er);
						rGraph.addVertex(er);
						//possibly add edges
						for(Node nt : rt){
							List<Element> elems = Lexicon.getElements(nt.getToken());
							if(elems.contains(comp)){
								//add edge
								rGraph.addEdge(nt, er);
							}
						}
					}	
				}
			}
		}
		Node T = new Node("T"); //sink
		T.setColumn("T");
		rGraph.addVertex(T);

		//add edges to T
		for(Node n : aar){
			rGraph.addEdge(n, T);
		}


		return rGraph;

	}

	
	
	private static boolean respectsAttachmentV2(List<Attachment> dependencies, Element a, Element b, boolean WH){
		
		boolean hasConstraint = false;
		
		if(dependencies.size() == 0)
			return true;
		
		for(Attachment at : dependencies){
			
			if(at.isWH() && !WH)
				continue;
			
			
			if(at.canDeriveElementFromTokens(a, false)){
				hasConstraint = true;
				if(at.canDeriveElementFromTokens(b, true))
					return true;
			}
			else if(at.canDeriveElementFromTokens(a, true)){
				hasConstraint = true;
				if(at.canDeriveElementFromTokens(b, false))
					return true;
			}
			else if(at.canDeriveElementFromTokens(b, false)){
				hasConstraint = true;
				if(at.canDeriveElementFromTokens(a, true))
					return true;
			}
			else if(at.canDeriveElementFromTokens(b, true)){
				hasConstraint = true;
				if(at.canDeriveElementFromTokens(a, false))
					return true;
			}
			
		}
		
		
		
		
		
		return !hasConstraint;
	}
	

	private static Node getNodeContainingElement(List<Node> nodeList, Element e){
		for(Node n : nodeList){
			if(n.getElement() != null && n.getElement().equals(e))
				return n;
		}
		return null;
	}


	/**
	 * Gets all the elements that are part of the max flow in a graph
	 * @param graph
	 * @param emf
	 * @return
	 */
	private static List<Element> getActiveElements(ListenableDirectedWeightedGraph<Node,DefaultWeightedEdge> graph,EdmondsKarpMaximumFlow<Node, DefaultWeightedEdge> emf, String column){
		List<Element> activeElems = new ArrayList<Element>();
	
		for(DefaultWeightedEdge dwe : graph.edgeSet()){
			for(Entry<DefaultWeightedEdge, Double> mdwep : emf.getMaximumFlow().entrySet()){
				if(mdwep.getKey() == dwe){
					if(mdwep.getValue() > 0.5){
						Node s = graph.getEdgeSource(dwe);
						Node t = graph.getEdgeTarget(dwe);
						if(s.getElement() != null && ( column == null || s.getColumn().equals(column))){
							if(!activeElems.contains(s.getElement()))
								activeElems.add(s.getElement());
						}
						if(t.getElement() != null && ( column == null || t.getColumn().equals(column))){
							if(!activeElems.contains(t.getElement()))
								activeElems.add(t.getElement());
						}
	
					}
				}
			}
		}
		return activeElems;
	}

}

