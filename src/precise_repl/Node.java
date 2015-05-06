package precise_repl;


public  class Node implements java.io.Serializable{

	private static final long serialVersionUID = 1L;
	private Token token;
	private Element element;
	private String special;
	private String column;
	private Node previous;
	private Node next;
	
	public Node getPrevious(){
		return previous;
	}
	
	public Node getNext(){
		return next;
	}
	
	public String getColumn(){
		return column;
	}
	
	public void setColumn(String col){
		column = col;
	}
	
	public void setPrevious(Node prev){
		previous = prev;
	}
	
	public void setNext(Node next){
		this.next = next;
	}
	
	public Token getToken() {
		return token;
	}

	public void setToken(Token token) {
		this.token = token;
	}

	public Element getElement() {
		return element;
	}

	public void setElement(Element element) {
		this.element = element;
	}

	public String getSpecial() {
		return special;
	}

	public void setSpecial(String special) {
		this.special = special;
	}

	public Node(Token t){
		this.token = t;
	}
	public Node(Element e){
		this.element = e;
	}
	public Node(String s){
		this.special = s;
	}
	
	@Override
	public String toString(){
		if(token != null)
			return token.toString();
		if(element != null)
			return element.toString();
		return special;
	}
}