/**
 * $Id$
 *
 * TODO:
 * ====
 *
 * - Clean up and figure out all the throws-clauses
 * - Handle collecting of overrides in strings and charsets properly
 */
import java.io.*;
import java.lang.reflect.*;
import java.util.regex.*;
import java.util.Vector;

class BasicParser {
	private static final int TRACE   = 1;
	private static final int INFO    = 2;
	private static final int WARNING = 3;
	private static final int ERROR   = 4;

	private static boolean showDoneOutput    = false;
	private static boolean showFirstTwoLines = false;
	private static int     traceLevel     = INFO;
	private static int     seqNr          = 1;

	private String buffer =  "" ;

	public BasicParser(String filename) {
		buffer = loadfile( filename );
		//trace( "curpos: " + curLine( 0 ) );
	}

	public static void setDoneOutput   ( boolean val ) { showDoneOutput = val; }
	public static void setTraceLevel   ( int     val ) { traceLevel     = val; }
	public static void setFirstTwoLines( boolean val ) { showFirstTwoLines = val; }

	public static boolean getFirstTwoLines() { return showFirstTwoLines; }


	protected static int getNextSeq() { return seqNr++; }

	/**
	 * Read contents of a file into a String.
	 */
	protected static String loadfile(String filename) {
		String buffer = "";

		File file = new File(filename);

		if ( !file.exists() ) return null;

		try {
			FileReader reader = new FileReader(file);

			final int BUF_SIZE = 1024;
			char[] array = new char[ BUF_SIZE];

			// read the entire content of the file into the string
			int countRead = 0;
			int total = 0;
			while( (countRead = reader.read( array, 0, BUF_SIZE ) ) != -1 ) {
				buffer += new String( array, 0, countRead );
				total += countRead;
			} 

			trace( "Read in " + total + " characters.");

			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return buffer;
	}


	/**
	 * Return the line in the buffer starting at given position, and also 
	 * the entire next line.
	 */
	protected String curLine(int curpos) {
		String ret = "";

		// Find second EOL starting from curPos
		int eolIndex = buffer.indexOf( "\n", curpos);

		if ( eolIndex != -1 ) {
			int tempIndex = buffer.indexOf( "\n", eolIndex + 1);
 	
			if ( tempIndex != -1 ) eolIndex = tempIndex;
		}

		// If no index found, assume that we are at the end of the buffer
		if ( eolIndex == -1 ) {
			ret = buffer.substring( curpos);
		} else {
			ret = buffer.substring( curpos, eolIndex);
		}

		return ret;
	}

	protected static void out(int traceLevel, String str) {
		if ( traceLevel >= BasicParser.traceLevel ) {
			System.out.println( str );	
		}
	}

	protected static void trace  (String str) { out( TRACE  , str); }
	protected static void info   (String str) { out( INFO   , str); }
	protected static void warning(String str) { out( WARNING, str); }
	protected static void error  (String str) { out( ERROR  , str); }

	protected class State {
		private int     depth       = 0;
		private int     curpos      = 0;
		private int     errpos      = -1;
		private String  errmethod   = null;
		private Node    curNode;
		private boolean skipCurrent = false;

		public State() {
			curNode = new Node();
		}

		public State(int curpos, String name, int depth ) {
			this.depth = depth;
			this.curpos = curpos;
			curNode = new Node( name, "");
		}

		public State copy( String name ) {
			return new State( curpos, name, depth + 1 );
		}

		public void success( State state, boolean ignore ) {
			curpos = state.curpos;

			if ( !ignore) {
				if ( state.getSkipCurrent() ) {
					// Store the children of this node instead of the Node itself.
					curNode.addChildren( state.curNode );
				} else {
					curNode.addChild( state.curNode );
				}
			}
		}

		public void matched( String value, String key, boolean ignore ) {
			curpos += value.length();
			if ( !ignore ) {
				curNode.addChild( key, value);
			}
		}

		public void matched( String value, String key ) {
			matched( value, key, false);
		}

		public boolean eol() throws ParseException {
			if ( curpos > buffer.length() ) { throw new ParseException(); }

			return curpos == buffer.length();
		}

		public void setError( State state, String method ) {
			if ( state.getErrorPos() != -1 ) {
				errpos = state.getErrorPos();
				errmethod = state.getErrorMethod();
			} else {
				errpos = state.getCurpos();
				errmethod = method;
			}
		}

		public int    getCurpos()      { return curpos; }
		public Node   getCurNode()     { return curNode; }
		public int    getErrorPos()    { return errpos; };
		public String getErrorMethod() { return errmethod; }
		public int    getDepth()       { return depth; }

		public void setSkipCurrent(boolean val) { skipCurrent = val; }
		public boolean getSkipCurrent() { return skipCurrent; }
	}


	protected class ParseException extends Exception {
	}

	/**
	 * Source: http://www.rgagnon.com/javadetails/java-0031.html
	 */
	protected boolean dynamicCall( String method, State state ) throws 
			ParseException,
			NoSuchMethodException, 
			IllegalAccessException,
			InvocationTargetException
	{
		Class params[] = new Class[1];
		Object paramsObj[] = new Object[1];

		params[0] = state.getClass();
		paramsObj[0] = state;

		// get the method
		Method thisMethod = getClass().getDeclaredMethod( method, params);
		// call the method
		Boolean ret = (Boolean) thisMethod.invoke( this , paramsObj);

		return ret.booleanValue();
	} 


	/**
	 * Make a dynamic call to a rule.
	 *
	 * Reflection is used internally to make the call.
	 *
	 * @param method   Name of method to call
	 * @param oldState Parse state of the calling method
	 * @param doThrow  If true, throw a ParseException if parsing fails. Default is false.
	 * @param ignore   If true, do not save parsed content to parse tree. Default is false.
	 */
	protected boolean s( String method, State oldState, boolean doThrow, boolean ignore ) throws 
			ParseException//, 
			//NoSuchMethodException, 
			//IllegalAccessException 
	{
		// TODO: Handle NoSuchMethodException and IllegalAccessException properly

		State state = oldState.copy( method );
		boolean ret = false;

		try {
			ret = dynamicCall( method, state);

			if ( ret ) {
				oldState.success( state, ignore );
				if ( showDoneOutput ) {
					info( "Done" + makeTab( state.getDepth(), " ") + method );
				}
			}
		} catch ( InvocationTargetException e ) {
			if ( e.getCause() instanceof ParseException ) {
				if ( doThrow ) {
					oldState.setError( state, method );
					throw (ParseException) e.getCause();
				}
			} else {
				error( 
					"s() InvocationTargetException method " + method + "." +
					" Wrapped Exception: " + e.getCause().toString() 
				);
				e.getCause().printStackTrace();
			}
		} catch ( Exception e ) {
			error( "s() Exception: " + e.toString() );
			e.getCause().printStackTrace();
		}

		if ( !ret ) {
			oldState.setError( state, method );
			if ( doThrow ) throw new ParseException();
		}
		return ret;
	}


	protected boolean s( String method, State oldState, boolean doThrow ) throws ParseException {
		return s( method, oldState, doThrow, false ); 
	}

	protected boolean s( String method, State oldState ) throws
			ParseException//, 
			//NoSuchMethodException, 
			//IllegalAccessException 
	{
		return s( method, oldState, false, false ); 
	}


	protected boolean parseString( String str, State state, boolean doThrow, boolean ignore )
			throws ParseException {

		// Special case end of file handling.
		// If string to test goes past buffer, there can never be a match
		if ( state.getCurpos() + str.length() > buffer.length() ) {
			return false;
		}

		String curStr = buffer.substring( state.getCurpos(), state.getCurpos() + str.length() );
		if ( curStr.equals( str ) ) {
			
			state.matched( str, "string", ignore );
			return true;
		} else {
			if ( doThrow ) throw new ParseException();
			return false;
		}
	}

	protected boolean parseString( String str, State state, boolean doThrow )  
			throws ParseException {

		return parseString( str, state, doThrow, false);
	} 

	protected boolean parseString( String str, State state ) throws ParseException {
		return parseString( str, state, false );
	}


	protected boolean parseCharset( Pattern pattern, State state, boolean doThrow,
			boolean ignore ) throws ParseException {

		Matcher matcher = pattern.matcher( buffer );
		if ( matcher.find( state.getCurpos() ) && state.getCurpos() == matcher.start() ) {
			String str = matcher.group();
			state.matched( str ,  "charset", ignore );
			return true;
		} else {
			if ( doThrow ) throw new ParseException();
			return false;
		}
	}

	protected boolean parseCharset( Pattern pattern, State state, boolean doThrow )
			throws ParseException {
		return parseCharset( pattern, state, doThrow, false );
	}

	protected boolean parseCharset( Pattern pattern, State state )
			throws ParseException {
		return parseCharset( pattern, state, false, false );
	}


	private String makeTab( int tabs, String fillChar ) {
		String ret = "";
		for( int i = 0; i < tabs*3; ++i ) {
			ret += fillChar;
		}
		return ret; 
	}

	private String makeTab( int tabs ) {
		return makeTab( tabs, ".");
	}


	protected class Node {
		String key;
		String value;
		Vector children = new Vector();
		Node   parent;

		Node() {
			key   = "";
			value = "";
		}

		Node( String key, String value ) {
			this.key = key;
			this.value = value;
		}

		public String getValue() { return value; }
		public void setValue( String val) { value = val; }

		public Node   getParent() { return parent; }
		public String getKey()    { return key; }

		void addChild(String key, String value) {
			addChild( new Node( key, value) );
		}

		void addChild( Node n) {
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

		void removeChildren() {
			children = new Vector();
		}

		Node get(String key ) {
			for ( int i =0; i < children.size(); ++i ) {
				Node n = (Node) children.get(i);

				if ( n.key.equals( key ) ) {
					return n;
				}
			}

			return null;
		}


		Node set( Node n ) {
			Node help = get( n.key );
			if ( help == null ) {
				addChild( n );
				help = n;
			} else {
				help.setValue( n.getValue() );
			}

			return help;
		}

		Node set( String key, String value ) {
			Node n = new Node( key, value );
			n.parent = this;
			return set( n );
		}

		Node set( String key ) {
			return set( key, "" );
		}

		public String show(int tabs, boolean showFirstTwoLines) {
			String tab = makeTab( tabs );
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

	/**
	 * Save the Node tree to file.
	 *
	 * Write the values of the node contained in the passed state parameter to file.
	 * Values of child nodes are written indented in relation to the parent node. 
	 * the output is a textual representation of the Node-tree.
	 */
	protected static void saveNodes( State state ) {
		String localFileName = "nodes.txt";

		String output = state.getCurNode().show( getFirstTwoLines() );

		try {
			FileWriter fw = new FileWriter( localFileName );
			fw.write(output);
			fw.close();
		} catch ( IOException e ) {
			error("saveNodes exception: "+ e.toString() );
		}
	}

}


public class EBNF extends BasicParser {
	private Pattern whitespace;
	private Pattern eol;
	private Pattern ts;
	private Pattern comment_ch;
	private Pattern label_start;
	private Pattern label_ch;
	private Pattern number_ch;
	private Pattern literal_ch;
	private Pattern lit_override_ch;
	private Pattern charset_ch;
	private Pattern override_ch;
	private Pattern plusminus_ch;
	private Pattern postfix_ch;
	private Pattern action_ch;
	private Pattern string_ch;

	public EBNF( String filename ) {
		super( filename );

		whitespace      = Pattern.compile( "[ \r\n\t]+");
		eol             = Pattern.compile( "\r\n|\r|\n");
		ts              = Pattern.compile( "[ \t]+");
		comment_ch      = Pattern.compile( "[\\x00-\\xFF&&[^\r\n]]+");
		label_start     = Pattern.compile( "[a-zA-Z_]");
		label_ch        = Pattern.compile( "[a-zA-Z_0-9]*");
		number_ch       = Pattern.compile( "[0-9]+");
		literal_ch      = Pattern.compile( "[ a-zA-Z_0-9#/~\\-\\]\\[\\]\\(\\)|{}=.!$:&*,+;]");
		lit_override_ch = Pattern.compile( "[\\\\\'\"]");
		charset_ch      = Pattern.compile( "[_a-zA-Z +0-9'\"+*?.{}$]");
		override_ch     = Pattern.compile( "[\\]\\-\\\\)~trn]");
		plusminus_ch    = Pattern.compile( "[+\\-]");
		postfix_ch      = Pattern.compile( "[?+*]");
		action_ch       = Pattern.compile( "[\\x00-\\xFF&&[^\"/\\r{}$#]]+");
		string_ch       = Pattern.compile( "[\\x00-\\xFF&&[^\"\\r\\n]]*");
	}

	protected boolean WS_intern(State state ) throws ParseException {
		while( parseCharset( whitespace, state ) || s( "comment", state, false, true) );

		return true;
	}

	protected boolean WS(State state ) throws ParseException {

		s( "WS_intern", state, false, true);

		return true;
	}


	protected boolean p_comment(State state ) throws ParseException {

		parseString( "#", state, true );

		parseCharset( comment_ch, state );
		parseCharset( eol, state, true );

		state.getCurNode().collect();

		return true;
	}

	protected boolean c_comment(State state ) throws ParseException {

		parseString( "//", state, true );

		parseCharset( comment_ch, state );
		parseCharset( eol, state, true );

		state.getCurNode().collect();

		return true;
	}


	protected boolean comment(State state ) throws ParseException {

		if (
			s( "c_comment", state) ||
			s( "p_comment", state, true)
		);

		state.getCurNode().collect();

		return true;
	}


	protected boolean label(State state ) throws ParseException {

		parseCharset( label_start, state, true );
		parseCharset( label_ch, state );

		state.getCurNode().collect();
		return true;
	}

	protected boolean modifier(State state ) throws ParseException {
		return 
			parseString( "noWS", state ) || 
			parseString( "skipWS", state ) || 
			( parseString( "Err", state ) && parseCharset( plusminus_ch, state, true ) ); 
	}

	protected boolean rule_modifier(State state ) throws ParseException {
		return 
			parseString( "ignore", state )  ||
			parseString( "entry", state )  ||
			parseString( "token", state ); 
	}

	protected boolean postfix(State state ) throws ParseException {
		boolean ret= parseCharset( postfix_ch, state );

		if ( ret) {
			state.getCurNode().collect();
		}

		return ret;
	}

	protected boolean atomicstatement(State state ) throws ParseException {
		if (
			s( "literal", state ) ||
			s( "charset", state ) ||
			s( "label", state ) ||
			s( "repeat", state ) ||
			s( "group", state, true )
		);

		s( "postfix", state );

		//
		//Post-action
		//

		// replace this node with its children
		state.setSkipCurrent( true );

		return true;
	}

	
	protected boolean except(State state ) throws ParseException {
		boolean ret = true;

		if ( ret = parseString( "~", state, false, true ) ) {
			s( "atomicstatement", state, true );
		}

		return ret;
	}


	protected boolean statement(State state ) throws ParseException {
		s( "modifier", state );
		WS(state);
 
		s( "atomicstatement", state, true );

		s( "except", state );

		return true;
	}

	protected boolean alternative(State state ) throws ParseException {
		s( "statement", state, true );

		do WS(state); while(
			parseString( "|", state, false, true ) &&
			s( "statement", state )
		);

		return true;
	}

	protected boolean statements(State state ) throws ParseException {

		s( "alternative", state, true );

		do WS(state); while(
			s( "alternative", state )
		);

		return true;
	}

	protected boolean rule(State state ) throws ParseException {
		s( "rule_modifier", state );
		WS(state);

		s( "label", state, true );
		WS(state);

		parseString( "=", state, true, true );
		WS(state);

		s( "statements", state );
		WS(state);

		parseString( ".", state, true, true );

		return true;
	}

	protected boolean lit_override(State state ) throws ParseException {

		parseString( "\\", state, true, true );
		parseCharset( lit_override_ch, state, true );

		return true;
	}

	protected boolean literal_chars(State state ) throws ParseException {
		do; while (
			parseCharset( literal_ch, state ) ||
			s( "lit_override", state )
		);
		return true;
	}

	protected boolean literal_s(State state ) throws ParseException {
		parseString( "'", state, true, true );
		s( "literal_chars", state );
		parseString( "'", state, true, true );

		state.getCurNode().collect();
		return true;
	}

	protected boolean literal_d(State state ) throws ParseException {
		parseString( "\"", state, true, true );
		s( "literal_chars", state );
		parseString( "\"", state, true, true );

		state.getCurNode().collect();
		return true;
	}

	protected boolean literal(State state ) throws ParseException {
		boolean ret =
			s( "literal_s", state ) ||
			s( "literal_d", state );

		state.getCurNode().collect();

		return ret;
	}

	protected boolean override(State state ) throws ParseException {

		parseString( "\\", state, true );
		if (
			parseCharset( override_ch, state ) ||
			parseString( "all", state, true )
		);

		state.getCurNode().collect();
		return true;
	}

	protected boolean range(State state ) throws ParseException {

		if (
			parseCharset( charset_ch, state ) ||
			s( "override", state, true )
		);

		if ( parseString( "-", state ) ) {
			if (
				parseCharset( charset_ch, state ) ||
				s( "override", state, true )
			);
		}

		state.getCurNode().collect();
		return true;
	}

	protected boolean except_charset(State state ) throws ParseException {

		parseString( "~", state, true, true );
		do; while (
			s( "range", state )
		);

		return true;
	}

	protected boolean charset(State state ) throws ParseException {

		parseString( "[", state, true, true );

		do; while (
			s( "range", state ) 
		);
		s( "except_charset", state );
		parseString( "]", state, true, true );

		return true;
	}

	protected boolean repeat(State state ) throws ParseException {

		parseString( "{", state, true, true );

		do WS(state); while (
			s( "statements", state )
		);

		parseString( "}", state, true, true );


		return true;
	}

	protected boolean group(State state ) throws ParseException {

		parseString( "(", state, true, true );

		do WS(state); while (
			s( "statements", state )
		);

		parseString( ")", state, true, true );

		return true;
	}

	protected boolean ctor_init(State state ) throws ParseException {

		if ( parseString( "(", state, true ) ) {
			WS(state);
			parseCharset( number_ch, state, true );
			WS(state);
			parseString( ")", state, true );
		};

		state.getCurNode().collect();
		return true;
	}

	protected boolean param_init(State state ) throws ParseException {

		parseString( "=", state, true ); 
		WS(state);
		if (
			parseString( "true" , state ) ||
			parseString( "false", state ) ||
			parseCharset( number_ch, state, true )
		);

		state.getCurNode().collect();
		return true;
	}

	protected boolean statevar(State state ) throws ParseException {
		trace( "Entered statevar");

		s( "label", state,true );
		WS(state);
		s( "label", state, true );
		WS(state);

		if ( s( "param_init", state, false ) ||
			s( "ctor_init", state, false ) );

		WS(state);
		parseString( ";", state, true, true );

		return true;
	}

	protected boolean stateblock(State state ) throws ParseException {
		trace( "Entered stateblock");

		parseString( "state", state, true, true );
		WS(state);
		parseString( "{", state, true, true );

		do WS(state); while (
			s( "statevar", state )
		);

		parseString( "}", state, true, true );

		return true;
	}

	protected boolean pathvar(State state ) throws ParseException {
		trace( "Entered pathvar");

		parseString( "[", state, true, true );
		s( "label", state, true);
		parseString( "]", state, true, true );

		// 
		// Post-action
		//

		Node n = state.getCurNode();
		n.collect();
		n.setValue( "\" + " + n.getValue() + " + \"");

		return true;
	}

	protected boolean macro_path(State state ) throws ParseException {
		trace( "Entered macro_path");

		do; while(
			( 
				parseString( ".", state, false ) ||
				parseString( "!", state, false ) 
			) && (
				s( "label", state ) ||
				s( "pathvar", state )
			)
		);

		// 
		// Post-action
		//
		Node n = state.getCurNode();
		n.collect();
		n.setValue( "root.getByPath( \"" + n.getValue() + "\")" );

		return true;
	}

	protected boolean foreach(State state ) throws ParseException {
		trace( "Entered foreach");

		parseString( "foreach", state, true, true );

		// No eol's allowed, only tabs and spaces
		parseCharset( ts, state, false, true );

		s( "label", state, true);
		parseCharset( ts, state, false, true );
		parseString( "in", state, true, true );
		parseCharset( ts, state, false, true );

		s( "macro_path", state, true);

		parseCharset( ts, state, false, true );
		parseString( "$", state, true, true );
		WS(state);
		parseString( "$body$", state, true, true );
		s( "action_content", state, true);

		//Note missing trailing $ - it's defined one level up in macro()
		// This is because this definition also contains the body and end-foreach tags
		parseString( "$end foreach", state, true, true );

		// 
		// Post-action
		//
		Node n = state.getCurNode();
		Node l = n.get( "label" );
		Node p = n.get( "macro_path" );
		Node c = n.get( "action_content" );
		String base = "base" + getNextSeq();
		String varloop = "i" + getNextSeq();
		c.collect();
		n.collect();	// to get rid of the children

		String str = "Node "+ base +" = " +  p.getValue() + ";\n";
		str += "for( int " + varloop +" = 0; i < " + base + ".length(); ++i ) {\n" +
			 "\tNode " + l.getValue() + " = base.get("+ varloop +");\n" +
			c.getValue() +
			"}\n";

		n.setValue( str );

		return true;
	}

	protected boolean currentToken(State state ) throws ParseException {
		parseString( "CurrentToken", state, true, true); 

		// 
		// Post-action
		//
		Node n = state.getCurNode();
		n.setValue( "state.getCurNode()" );

		return true;
	}

	protected boolean macro(State state ) throws ParseException {
		trace( "Entered action");

		parseString( "$", state, true, true );

		if (
			s( "currentToken", state ) ||
			s( "foreach", state ) ||
			s( "macro_path", state, true)
		);

		parseString( "$", state, true, true );

		return true;
	}

	protected boolean action_text(State state ) throws ParseException {
		boolean ret = parseCharset( action_ch, state );

		if ( ret ) {
			state.getCurNode().collect();
		}
		return ret;
	}

	protected boolean action_string(State state ) throws ParseException {
		parseString( "\"", state, true );
		parseCharset( string_ch, state, true );
		parseString( "\"", state, true );

		state.getCurNode().collect();
		return true;
	}

	protected boolean action_content(State state ) throws ParseException {
		trace( "Entered action_content");

		// TODO: Following construct presents problems when real errors
		// are encountered in action_block.
		do; while (
			// order important
			parseString( "\r", state, false, true ) ||
			s( "p_comment", state, false, true ) ||
			s( "c_comment", state ) ||
			s( "action_string", state ) ||
			s( "action_text", state ) ||
			s( "macro", state ) ||
			s( "action_block", state )
		);

		state.getCurNode().collect();
		return true;
	}

	protected boolean action_block(State state ) throws ParseException {
		trace( "Entered action_block");

		parseString( "{", state, true );

		s( "action_content", state, true);

		parseString( "}", state, true );

		return true;
	}

	protected boolean pre_block(State state ) throws ParseException {

		parseString( "pre:", state, true, true );
		WS(state);
		parseString( "{", state, true, true );

		s( "action_content", state, true);

		parseString( "}", state, true, true );

		return true;
	}

	protected boolean action(State state ) throws ParseException {
		trace( "Entered action");

		parseString( "action", state, true, true );
		WS(state);
		s( "label", state, true);
		WS(state);

		if( s( "pre_block", state) ) {
			WS(state);

			if ( parseString( "post:", state, false, true ) ) {
				WS(state);
				parseString( "{", state, true, true );
				s( "action_content", state, true);
				parseString( "}", state, true, true );
			}
		} else {
			// If post-block only, then the post:-prefix
			// is not obligatory
			parseString( "post:", state, false, true ); 
			WS(state);

			parseString( "{", state, true, true );
			s( "action_content", state, true);
			parseString( "}", state, true, true );
		}

		return true;
	}

	protected boolean parameter(State state ) throws ParseException {
		s( "label", state, true);
		WS(state);
		if ( 
			parseString( "&", state ) ||
			parseString( "*", state )
		);
		WS(state);
		s( "label", state, true);
		return true;
	}

	protected boolean returnval(State state ) throws ParseException {
		return 
			parseString( "BOOL", state) ||
		    parseString( "void", state) ||
			( s( "label", state ) && WS(state) &&  parseString( "&", state) );
	}

	protected boolean function(State state ) throws ParseException {
		trace( "Entered function");

		parseString( "function", state, true, true );
		WS(state);
		s( "returnval", state, true);
		WS(state);
		s( "label", state, true);
		WS(state);

		parseString( "(", state, true, true );
		WS(state);
		if ( parseString( "void", state) ) {
			// No op
		} else if ( s( "parameter", state) ) {
			while ( WS(state) && parseString( ",", state, false, true ) ) {
				WS(state); 
				s( "parameter", state, true);
			}
		}
		parseString( ")", state, true, true );
		WS(state);

		parseString( "{", state, true, true );
		s( "action_content", state, true);
		parseString( "}", state, true, true );

		if ( state.getErrorPos() != -1 ) {
			trace( "After action_content: Error in " + state.getErrorMethod() + " at: " + 
				curLine( state.getErrorPos() ) );
		}

		return true;
	}

	protected boolean language(State state ) throws ParseException {
		trace( "Entered language");

		parseString( "language", state, true, true );
		WS(state);
		s( "label", state, true );
		WS(state);
		parseString( "{", state, true, true );

		do WS(state); while(
			s( "modifier"  , state ) ||
			s( "rule"      , state ) ||
			s( "stateblock", state ) ||
			s( "action"    , state ) ||
			s( "function"  , state )
		);
		parseString( "}", state, true, true );

		return true;
	}


	protected void parse()
		throws NoSuchMethodException, IllegalAccessException {
		State state = new State();

		// Parse according to rules
		try {
			while ( !state.eol() ) {
				WS(state);

				if ( !s( "language", state ) ) break;
			}
		} catch ( ParseException e ) {
			error( "Exception: " + e.toString() );
		}

		// Do node translations
		translate( state );

		// Exit
		saveNodes( state );
		showFinalResult(state);
	}

	protected static String replace(
	    String aInput, String aOldPattern, String aNewPattern
  	){
    	final Pattern pattern = Pattern.compile( "["+ aOldPattern + "]" );
	    final Matcher matcher = pattern.matcher( aInput );
	    return matcher.replaceAll( aNewPattern );
	}


	/**
	 * TODO: 
	 *
	 * - Add whitespace handling
	 * - Need to sort out the final two optional parameters.
	 */
	private void translate( State state ) {

		////////////////////
		// charsets
		////////////////////

		// Translate special characters in charset ranges to the java regexp equivalents
		Vector res =  state.getCurNode().findNodes( "range" );
		for( int i = 0;  i < res.size(); ++i ) {
			Node n = (Node) res.get(i);

			String val = n.getValue();

			if ( val.equals( "\\-" ) || val.equals( "\\)" ) || val.equals( "\\]" ) || val.equals( "\"" ) || val.equals("'") ) {
				n.setValue( "\\" + val );
			} else if ( val.equals( "\\\\" ) ) {
				n.setValue( "\\\\" + val );
			} else if ( val.equals( "\\~" ) ) {
				n.setValue( "~" );
			} else if ( val.equals( "\\all" ) ) {
				n.setValue( "\\\\x00-\\\\xFF");
			}
		}


		// translate the charset 'except' clause to the java equivalent
		res =  state.getCurNode().findNodes( "except_charset" );
		for( int i = 0;  i < res.size(); ++i ) {
			Node n = (Node) res.get(i);

			n.collect();
			n.setValue( "&&[^" + n.getValue() + "]" );
		}


		// Create java code for the charsets
		res =  state.getCurNode().findNodes( "charset" );
		for( int i = 0;  i < res.size(); ++i ) {
			Node n = (Node) res.get(i);
			int counter = i + 1;	

			n.collect();
			String ctor_value = "\t\tpattern" + counter + " = Pattern.compile( \"[" + n.getValue() + "]\");\n";
			String member_value = "\tprivate Pattern pattern" + counter + ";\n";

			state.getCurNode().set( "temp" ).set( "ctor" ).addChild( new Node( "init_pattern", ctor_value) );
			state.getCurNode().set( "temp" ).set( "members" ).addChild( new Node( "member_pattern", member_value) );
			n.setValue( "pattern" + counter );
		}

		res =  state.getCurNode().findNodes( "charset" );
		for( int i = 0;  i < res.size(); ++i ) {
			Node n = (Node) res.get(i);

			// Parent is a statement node. Replace parent with output code.
			Node p = n.getParent();
			p.setValue( "parseCharset( " + n.getValue() + ", state )" ); 
			p.removeChildren();
		}

		////////////////////
		// literals
		////////////////////

		res =  state.getCurNode().findNodes( "literal" );
		for( int i = 0;  i < res.size(); ++i ) {
			Node n = (Node) res.get(i);
		
			// Translate special characters in literal
			// following replaces a single backslash with a double. No, really
			n.setValue( replace( n.getValue(), "\\\\", "\\\\\\\\") );
			//Okay, this sucks, but I will persevere
			n.setValue( replace( n.getValue(), "\\\"", "\\\\\"") );

			// Parent is a statement node. Replace parent with output code.
			Node p = n.getParent();
			p.setValue( "parseString( \"" + n.getValue() + "\", state )" ); 
			p.removeChildren();
		}

		////////////////////
		// Labels
		////////////////////

		res =  state.getCurNode().findNodes( "label" );
		for( int i = 0;  i < res.size(); ++i ) {
			Node n = (Node) res.get(i);

			// Only handle parent nodes which are statements.
			Node p = n.getParent();

			if ( p.getKey().equals( "statement" ) ) {			
				// Replace parent with output code.
				p.setValue( "s( \"" + n.getValue() + "\", state )" ); 
				p.removeChildren();
			}
		}

		////////////////////
		// Alternatives
		////////////////////
		res =  state.getCurNode().findNodes( "alternative" );

		for( int i = 0;  i < res.size(); ++i ) {
			Node n = (Node) res.get(i);

			// Alternatives with single children translate
			// directly to statement.
			if ( n.numChildren() == 1 ) {
				// Child must be a statement with no children
				Node c = n.get( "statement" );
				if ( c != null && c.numChildren() == 0 ) {
					n.setValue( c.getValue() + ";" );
					n. removeChildren();
				}
			}			
			else if ( n.numChildren() > 1 ) {
				// If all children are statements, collect
				// in an encompasing if()
				boolean allStatements = true;
				for( int j = 0;  j < n.numChildren(); ++j ) {
					// TODO: attribute accessed directly
					Node c = (Node) n.children.get(j);
					if ( !"statement".equals( c.getKey() ) ) {
						allStatements = false;
						break;
					}	
				}

				if ( allStatements ) {
					String buf = "if (";

					for( int j = 0;  j < n.numChildren(); ++j ) {
						// TODO: attribute accessed directly
						Node c = (Node) n.children.get(j);

						buf += "\t" + c.getValue();

						if ( j < n.numChildren() -1 ) {
							buf += " ||";
						}
						//buf += "\n";
					}

					buf += ");\n";

					n.setValue( buf );
					n.removeChildren();
				}
			}
		}
	}


	private void showFinalResult( State state ) {
		try {
			if ( state.eol() ) {
				info("Parsing completed succesfully.");
			} else { 
				if ( state.getErrorPos() != -1 ) {
					error( "Error in " + state.getErrorMethod() + " at: " + 
						curLine( state.getErrorPos() ) );
				}
			}
			trace( "curpos: " + curLine( state.getCurpos() ) );
		} catch ( ParseException e ) {
			error( "Exception: " + e.toString() );
		}
	}

	public static void main(String[] argv) 
		throws NoSuchMethodException, IllegalAccessException {

		EBNF parser = new EBNF( argv[0] );
		parser.setFirstTwoLines(true);
		parser.parse();
	}
}
