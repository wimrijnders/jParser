/**
 * $Id: EBNF.java 10 2008-08-12 10:56:22Z wri $
 *
 */
package nl.axizo.parser;

import java.util.Vector;

public class Node {
	String key;
	String value;
	Vector children = new Vector();
	Node   parent;

	Node() {
		key   = "";
		value = "";
	}

	public Node( String key, String value ) {
		this.key = key;
		this.value = value;
	}

	public String getValue() { return value; }
	public void setValue( String val) { value = val; }

	public Node   getParent() { return parent; }
	public String getKey()    { return key; }

	public void addChild(String key, String value) {
		addChild( new Node( key, value) );
	}

	public void addChild( Node n) {
		n.parent = this;
		children.add( n );
	}

	public int numChildren() { return children.size(); }

	/**
	 * Move the children of the passed node to the current node.
	 *
 	 * All children are removed from the passed node.
	 */ 
	void addChildren( Node n) {

		Vector children = n.children;
		n.removeChildren();

		for( int i = 0; i < children.size(); ++i ) {
			Node temp = (Node) children.get( i );
			temp.parent = this;
		}

		this.children.addAll( children );
	}

	public void removeChildren() {
		children = new Vector();
	}


	/**
 	 * Get child Node by key value.
 	 */
	public Node get(String key ) {
		for ( int i =0; i < children.size(); ++i ) {
			Node n = (Node) children.get(i);

			if ( n.key.equals( key ) ) {
				return n;
			}
		}

		return null;
	}


	/**
 	 * Get child Node by index into list.
 	 */
	public Node get(int n ) {
		return (Node) children.get(n);
	}


	public Node set( Node n ) {
		Node help = get( n.key );
		if ( help == null ) {
			addChild( n );
			help = n;
		} else {
			help.setValue( n.getValue() );
		}

		return help;
	}

	public Node set( String key, String value ) {
		Node n = new Node( key, value );
		n.parent = this;
		return set( n );
	}

	public Node set( String key ) {
		return set( key, "" );
	}

	public String show(int tabs, boolean showFirstTwoLines) {
		String tab = Util.makeTab( tabs );
		String showVal = value;

		if ( showFirstTwoLines ) {
			showVal = truncTwoLines( showVal );
		}

		String ret = tab + key + ": " + showVal + "\n";

		for ( int i =0; i < children.size(); ++i ) {
			Node n = (Node) children.get(i);
			ret += n.show( tabs + 1, showFirstTwoLines );
		}

		return ret;
	}

	public String show() {
		return show( 0, false );
	}

	public String show( boolean showFirstTwoLines) {
		return show( 0, showFirstTwoLines );
	}


	/**
	 * Return the first two lines of given value.
	 *
	 * Returns all content if less than two lines present.
	 */
	private String truncTwoLines(String val ) {
		String ret = val;

		int ind1 = val.indexOf("\n");
		if ( ind1 != -1 ) {
			int ind2 = val.substring(ind1 +1).indexOf("\n");
			if ( ind2 != -1 ) {
				ind1 += ind2 + 1;
			}

			ret = val.substring(0, ind1 );
		}

		return ret;
	}


	/**
	 * Collect all values under this node recursively.
	 *
	 * Concatenate the value of this node and all the children recursively
	 * to a single string, and replace the value of this node with this string.
	 * All children will be removed.
	 */
	public void collect() {
		String str = value;

		for ( int i =0; i < children.size(); ++i ) {
			Node n = (Node) children.get(i);
			n.collect();
			str += n.value;
		}
		children = new Vector();

		value = str;
	}

	
	/**
 		 * Search nodes depth-first and return all nodes
 		 * with the key equal to the given parameter.
 		 *
 		 */
	public Vector findNodes( String label ) {
		Vector ret = new Vector();

		for ( int i =0; i < children.size(); ++i ) {
			Node n = (Node) children.get(i);
			ret.addAll( n.findNodes(label) );
		}

		if ( key.equals( label ) ) {
			ret.add( this );
		}

		return ret;
	} 

	public String toString() {
		return key + ": " + value + ": " + children.size() + " children";
	}
}