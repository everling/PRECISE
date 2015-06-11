package precise_repl;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
 

/**
 * The GUI class for testing PRECISE. Contains main method.
 * @author nils
 *
 */
public class TestGUI implements Runnable, ActionListener {
 
	JButton queryLauncher;
	JButton testAttachment;
	JTextField query;
	JTextArea result;
	JTextArea dependencies;
	JTextArea excel;
	JCheckBox visualize;
	JCheckBox equivalence;
	JCheckBox killAttachment;
	JCheckBox printDebug;
	JButton helpBtn;
	JFrame f;
	
	String help = "Write question in leftmost field.\nVisualize: show attribute-value graphs + relation graphs\n" +
	"EQ check: checks every possible max-flow solution. should always be checked\n"
	+"Debug: info to stdout if checked\n"		
	+"Delete attachment: ignore attachment mappings from stanford parse tree.\n"
	+"Write manual attachment mappings in the middle field. Format: what_state,state_san francisco";
	
	
	
    @Override
    public void run() {
        f = new JFrame("PRECISE");
        f.setLayout(new FlowLayout());
        
        query = new JTextField("what is the population of California?");
        queryLauncher = new JButton("Query");
        queryLauncher.addActionListener(this);
        testAttachment = new JButton("Test attachment");
        testAttachment.addActionListener(this);
        visualize = new JCheckBox("Visualize");
        equivalence = new JCheckBox("EQ check");
        printDebug = new JCheckBox("Debug");
        dependencies = new JTextArea(1,20);
        killAttachment = new JCheckBox("Delete attachment");
        result = new JTextArea(20, 50);
        result.setLineWrap(true);
        result.setWrapStyleWord(true);
        helpBtn = new JButton("Help");
        helpBtn.addActionListener(this);

        equivalence.setSelected(true);
        
        f.add(query);
        f.add(queryLauncher);
        f.add(testAttachment);
        f.add(visualize);
        f.add(equivalence);
        f.add(printDebug);
        f.add(dependencies);
        f.add(killAttachment);
        f.add(result);
        f.add(helpBtn);
        
        f.pack();
        f.setVisible(true);
    }
 
    public static void main(String[] args){
    	
   
    	if(args.length != 2){			
			System.out.println("Usage: wordNetPath database");
			return;
		}
		
		String wordNetPath = args[0]; 
		String databaseInput = args[1]; 
    	try {
			Lexicon.init(wordNetPath);
		} catch (IOException e) {
			System.out.println("WordNet path incorrect");
			return;
		}
		Parser.init();
		if(!Lexicon.loadLexicon(databaseInput)){
			System.out.println("No lexicon found.");
			return;
		}		
		
    	Lexicon.printMappingsBetter();
        TestGUI se = new TestGUI();
        SwingUtilities.invokeLater(se);
    }

	@Override
	public void actionPerformed(ActionEvent e) {

		if(e.getSource().equals(queryLauncher)){
			result.setText(PRECISE.query(query.getText(),dependencies.getText(), visualize.isSelected(), equivalence.isSelected(), killAttachment.isSelected(), printDebug.isSelected(),new PRECISE.ErrorMsg()));
		}
		else if(e.getSource().equals(testAttachment)){
			PRECISE.queryAttachment(query.getText(),dependencies.getText(),killAttachment.isSelected());
		}
		else if(e.getSource().equals(helpBtn)){
	        JOptionPane.showMessageDialog(f,help);
		}
		
	}
 
}