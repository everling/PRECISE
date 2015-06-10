package precise_repl;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import precise_repl.Lexicon.JoinPath;
 

/**
 * Simple java GUI to add and remove token mappings to elements in the lexicon
 * @author nils
 *
 */
public class ModifyGUI implements Runnable, ActionListener, ListSelectionListener {
 
	
	JList elements;
	JList matchingTokens;
	DefaultListModel mat;
	JTextField input;
	JButton add;
	JButton remove;
	JTextField dbName;
	JButton save;
	JButton pkey;
	
	JTextField joinPathInput;
	
	JButton jAdd;
	JButton jClear;
	JButton jView;
	JFrame f;
	List<Element> relAndAtt;
	
	JTextField wh;
	JButton whset;
	
    @Override
    public void run() {
        f = new JFrame("Modify Lexicon");
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.setLayout(new FlowLayout());
      
        List<Element> allElems = Lexicon.getAllElements();
        relAndAtt = new ArrayList<Element>();
        for(Element e : allElems){
        	//if(e.getType() == Element.TYPE_VALUE && e.getCompatible().get(0).getName().equals("studio"))
        	if(e.getType() == Element.TYPE_RELATION || e.getType() == Element.TYPE_ATTRIBUTE)
        		relAndAtt.add(e);
        }
        
        //tokens
        elements = new JList(relAndAtt.toArray());
        elements.addListSelectionListener(this);
        elements.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        f.add(elements);
        
        
        mat = new DefaultListModel();
        mat.addElement("matching tokens");
        
        matchingTokens = new JList(mat);
        matchingTokens.setSize(500, 200);
        matchingTokens.addListSelectionListener(this);
        f.add(matchingTokens);
        
        
        input = new JTextField("input,token,here");
        f.add(input);
        
        add = new JButton("Add");
        add.addActionListener(this);
        f.add(add);
        remove = new JButton("Remove");
        remove.addActionListener(this);
        f.add(remove);
        
        //join path
        joinPathInput = new JTextField("relation.attribute1,relation.attribute2,...");
        f.add(joinPathInput);
       
        jAdd = new JButton("Add join path");
        jAdd.addActionListener(this);
        f.add(jAdd);
        
        jClear = new JButton("Clear join paths");
        jClear.addActionListener(this);
        f.add(jClear);
        
        jView = new JButton("View join paths");
        jView.addActionListener(this);
        f.add(jView);

        
        dbName = new JTextField("db name");
        f.add(dbName);
        save = new JButton("Save db");
        save.addActionListener(this);
        f.add(save);
        
        pkey = new JButton("Make primary key");
        pkey.addActionListener(this);
        f.add(pkey);
        
        wh = new JTextField("wh");
        f.add(wh);
        
        whset = new JButton("Set wh");
        whset.addActionListener(this);
        f.add(whset);
        
        
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
        ModifyGUI se = new ModifyGUI();
        SwingUtilities.invokeLater(se);
    }

	@Override
	public void actionPerformed(ActionEvent e) {
		if(e.getSource().equals(remove)){
			if(matchingTokens.getSelectedValue() != null && elements.getSelectedValue() != null){
				Token t = (Token) matchingTokens.getSelectedValue();
				Element em = (Element) elements.getSelectedValue();
				Lexicon.removeTokenMapping(t, em);
				updateMatching();
			}
		}
		if(e.getSource().equals(add)){
			if(elements.getSelectedValue() != null){
				Element em = (Element) elements.getSelectedValue();
				String text = input.getText();
				if(text.length() > 0){
					String[] augArray = text.split(",");
					Token t = new Token(em.getType(), augArray);
					Lexicon.addTokenMapping(t, em);
					updateMatching();
				}
			}
		}
		if(e.getSource().equals(jAdd)){
			Lexicon.addJoinPath(joinPathInput.getText());
		}
		if(e.getSource().equals(jView)){
			JOptionPane.showMessageDialog(f,Arrays.toString(Lexicon.getAllJoinPaths().toArray()).replaceAll(",", "\n"));
		}
		if(e.getSource().equals(jClear)){
			Lexicon.clearJoinPaths();
		}
		if(e.getSource().equals(save)){
			Lexicon.saveLexicon(dbName.getText());
		}
		
		if(e.getSource().equals(whset)){
			setWH();
		}
		
		if(e.getSource().equals(pkey)){
			if(elements.getSelectedValue() != null){
				Element em = (Element) elements.getSelectedValue();
				if(em.getType() == Element.TYPE_ATTRIBUTE && em.getCompatible() != null){
					for(Element p: em.getCompatible()){
						if(p.getType() == Element.TYPE_RELATION){
							
							if(p.getPrimaryKey() != null && p.getPrimaryKey().equals(em.getName())){
								p.setPrimaryKey("null");
								System.out.println("Primary key for :"+p +" set to null");
							}
							else{
								p.setPrimaryKey(em.getName());
								System.out.println("Primary key for :"+p +" set to " +em);
							}
							
							
						}
					}
				}

			}
			
		}
		
	}

	
	private void loadWH(){
		Element e = relAndAtt.get(elements.getSelectedIndex());
		
		String txt = "";
		
		for(Element w : Lexicon.getWHS()){
			List<Element> comps = w.getCompatible();
			if(comps.contains(e))
				txt += w.getName() +" ";
		}
		
		wh.setText(txt);
	}
	
	private void setWH(){
		
		Element e = relAndAtt.get(elements.getSelectedIndex());

		String[] zz = wh.getText().split(" ");
		
		
		List<Token> tokens = new ArrayList<Token>();
		
		
		for(String s : zz){
			tokens.addAll(Tokenizer.tokenizeString(s));
		}
				
		Element[] whs = Lexicon.getWHS();
		
		for(Element whelem : whs){
			
			whelem.getCompatible().remove(e);
			
			for(Token t : tokens){
				List<Element> elms = Lexicon.getElements(t);
				if(elms != null && elms.contains(whelem)){
					System.out.println(whelem +"compatible to" +e);
					whelem.addCompatible(e);
				}

			}

		}

	}
	
	private void updateMatching(){
		Element e = relAndAtt.get(elements.getSelectedIndex());
		mat.clear();
		//get all tokens
		List<Token> tokens = Lexicon.getMatchingTokens(e);
		for(Token t : tokens){
			mat.addElement(t);
		}
	}
	
	@Override
	public void valueChanged(ListSelectionEvent arg0) {
		
		if(arg0.getSource().equals(elements)){
			updateMatching();
			loadWH();
		}
	}
}