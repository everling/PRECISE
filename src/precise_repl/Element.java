package precise_repl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Element implements java.io.Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public static final int TYPE_RELATION = 1;
	public static final int TYPE_ATTRIBUTE = 2;
	public static final int TYPE_VALUE = 3;

	private int type;
	private String name;
	private List<Element> compatible;
	private List<Element> schema;
	private String primaryKey;
	
	public Element(int type, String name){
		this.type = type;
		this.name = name;
		compatible = new ArrayList<Element>();
	}
	
	public Element(int type, String name, Element... compatible){
		this.type = type;
		this.name = name;
		this.compatible = Arrays.asList(compatible);
	}
	
	/**
	 * Add a compatible element.
	 * @param e
	 */
	public void addCompatible(Element e){
		if(!compatible.contains(e))
			compatible.add(e);
	}
	
	/**
	 * Set the relation schema, solely used for relation elements
	 * @param schema
	 */
	public boolean setSchema(List<Element> schema){
		if(type == TYPE_RELATION){
			this.schema = schema;
			return true;
		}
		return false;
	}
	
	/**
	 * Add an element in the relation schema, solely used on relation elements
	 * @param e
	 * @return
	 */
	public boolean addSchemaElement(Element e){
		if(type == TYPE_RELATION){
			if(schema == null)
				schema = new ArrayList<Element>();
			schema.add(e);
			return true;
		}
		return false;
	}
	
	public List<Element> getCompatible(){
		return compatible;
	}
	
	public Element getCompatibleOfType(int type){
		for(Element e : compatible)
			if(e.getType() == type)
				return e;
		return null;
	}
	
	public List<Element> getSchema(){
		return schema;
	}
	
	public String getName(){
		return name;
	}
	
	public int getType(){
		return type;
	}
	
	public String getPrimaryKey(){
		return primaryKey;
	}
	
	public void setPrimaryKey(String s){
		if(s != null && s.length() > 0)
			primaryKey = s;
	}
	
	@Override
	public String toString(){
		
		if(Lexicon.isWH(this))
			return toShortString();
		
		String comp = "";
		for(Element e : compatible)
			comp += "(" +e.toShortString() +")";
		return comp + toShortString();
	}
	
	public String toShortString(){
		return ((type == TYPE_RELATION) ? "R " : (type == TYPE_ATTRIBUTE)? "A " : "V ") +name+" ";
	}
	
	public boolean equals(Object o){
		
		Element oe = (Element)o;
		
		if(type != oe.type)
			return false;
		
		if(!name.equals(oe.name))
			return false;
		
		if(compatible.size() != oe.compatible.size())
			return false;
		
		for(int i = 0; i < compatible.size(); i++){
			if(compatible.get(i) != oe.compatible.get(i))
					return false;
		}
		return true;
	}
	
	

}
