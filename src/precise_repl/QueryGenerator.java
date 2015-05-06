package precise_repl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import precise_repl.Lexicon.JoinPath;
import precise_repl.Parser.Attachment;

public class QueryGenerator {



	public static List<String> generateQuery(List<Node> avNodes, List<Attachment> dependencies,boolean print){

		// get all compatile relations from attributes
		List<Element> relations = new ArrayList<Element>();

		for(Node ea : avNodes){
			if(ea.getElement() != null && ea.getColumn().equals("EA")){
				for(Element r : ea.getElement().getCompatible()){
					if(r.getType() == Element.TYPE_RELATION && !relations.contains(r))
						relations.add(r);
				}
			}
		}

		List<String> toRet = new ArrayList<String>();


		//get element paired with WH-value
		Element whPaired = null;
		for(Node n : avNodes){
			if(n.getElement() != null && Lexicon.isWH(n.getElement())){
				whPaired = n.getNext().getElement();
			}
		}

		if(whPaired == null){
			System.out.println("No WH-token");
			return toRet;

		}




		if(relations.size() > 1){
			//get all join paths
			List<List<JoinPath>> joinPaths = Lexicon.getJoinPathsV2(relations,whPaired);
			
			for(List<JoinPath> jp : joinPaths){
				System.out.println(Arrays.toString(jp.toArray()));
				String s = generateQuery1(avNodes,dependencies,jp,whPaired,print);
				if(!s.contains("*IJ"))
					toRet.add(0, s);
				else
					toRet.add(s);

			}

		}
		else{
			String s = generateQuery1(avNodes,dependencies,null,whPaired,print);
			if(!s.contains("*"))
				toRet.add(0, s);
			else
				toRet.add(s);
		}
		return toRet;

	}


	private static String generateQuery1(List<Node> avNodes, List<Attachment> dependencies,List<JoinPath> joinPath, Element whPaired, boolean print){

		// get all compatile relations from attributes
		List<Element> relations = new ArrayList<Element>();

		for(Node ea : avNodes){
			if(ea.getElement() != null && ea.getColumn().equals("EA")){
				for(Element r : ea.getElement().getCompatible()){
					if(r.getType() == Element.TYPE_RELATION && !relations.contains(r))
						relations.add(r);
				}
			}
		}




		StringBuilder query = new StringBuilder();


		query.append("SELECT ");


		for(Element r : whPaired.getCompatible()){
			if(r.getType() == Element.TYPE_RELATION){
				query.append(r.getName()+"."+whPaired.getName()+" ");
				break;
			}
		}


		query.append("FROM ");

		for(Element r : relations){
			query.append(r.getName() +",");
		}
		query.deleteCharAt(query.length()-1);


		//value constraints
		boolean hasValueConstraints = false;
		for(Node ev : avNodes){
			if(ev.getElement() != null && ev.getColumn().equals("EV")){
				if(!Lexicon.isWH(ev.getElement())){
					hasValueConstraints = true;
					break;
				}
			}
		}


		if(hasValueConstraints){
			query.append(" WHERE ");
			//specify Relation.attribute = value
			for(Node ea : avNodes){

				if(ea.getElement() != null && ea.getColumn().equals("EA")){
					Node ev = ea.getPrevious();
					if(ev.getColumn().equals("EV") && !Lexicon.isWH(ev.getElement())){
						for(Element r : ea.getElement().getCompatible()){
							if(r.getType() == Element.TYPE_RELATION){
								query.append(r.getName()+"."+ea.getElement().getName() +" = '" +ev.getElement().getName() + "' AND ");
								break;
							}
						}
					}
				}
			}
		}
	
		if(relations.size() > 1){


			for(JoinPath jp : joinPath){
				query.append(jp.getJoin() +" AND ");
			}



		}
		if(hasValueConstraints)
			query.delete(query.length()-5, query.length()-1);//remove last AND
		query.append(";");
		return query.toString();

	}


}

