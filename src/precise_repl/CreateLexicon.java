package precise_repl;

import java.io.IOException;

public class CreateLexicon {
	
	/**
	 * Create new lexicon
	 * @param args
	 */
	public static void main(String[] args){
		

		if(args.length < 3){			
			System.out.println("Usage: path_to_wordnet path_to_database path_to_lexicon_output");
			System.out.println("Optional:\n");
			System.out.println("\t-a (allow synonyms of relations and attributes)");
			System.out.println("\t-v (allow synonyms of values)");
			return;
		}
		
		String wordNetPath = args[0];
		String databaseInput = args[1];
		String databaseOutput = args[2];

		boolean manual = false;
		boolean synonymsRelAtt = false;
		boolean synonymsVal = false;
		boolean lemmatizeValues = true;
		
		for(int i = 3; i < args.length; i++){
			if(args[i].startsWith("-a"))
				synonymsRelAtt = true;
			if(args[i].startsWith("-v"))
				synonymsVal = true;
			
		}
		
    	try {
			Lexicon.init(wordNetPath);
			Lexicon.loadElements(databaseInput);
			Lexicon.buildLexiconMappings(manual,synonymsRelAtt, synonymsVal,lemmatizeValues);
			Lexicon.saveLexicon(databaseOutput);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
