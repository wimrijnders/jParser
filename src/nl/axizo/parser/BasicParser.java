/**
 * $Id$
 *
 * TODO:
 * ====
 *
 * - Clean up and figure out all the throws-clauses
 * - Handle collecting of overrides in strings and charsets properly
 */
package nl.axizo.parser;

import java.io.*;
import java.lang.reflect.*;
import java.util.regex.*;
import java.util.Vector;

public class BasicParser {
	protected static final int TRACE   = 1;
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


	/**
 	 * Make a dynamic call to given method.
 	 * 	
	 * Reflection is used internally to make the call.
	 *
 	 * Methods  in derived classes which reside in other packages, need
 	 * to be declared public for this to work.
 	 *
 	 * @param method name of method to call
 	 * @param state state information of current call.
	 * @see <a href="http://www.rgagnon.com/javadetails/java-0031.html">Source</a>
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
	 * The intention is to call generated syntax rules dynamically. The name is 
	 * intentionally chosen short (one letter), because this call appears often in the
	 * generated code, and we want to keep the latter readable.
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
					info( "Done" + Util.makeTab( state.getDepth(), " ") + method );
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


	/**
	 * Make a dynamic call to a rule.
	 *
 	 * Override of {@link #s(String,State,boolean,boolean) s(String,State,boolean,boolean)}, with
 	 * addition of parsed values to parse tree. 
 	 */
	protected boolean s( String method, State oldState, boolean doThrow ) throws ParseException {
		return s( method, oldState, doThrow, false ); 
	}


	/**
	 * Make a dynamic call to a rule.
	 *
 	 * Override of {@link #s(String,State,boolean,boolean) s(String,State,boolean,boolean)}, with
 	 * throwing on error disabled and
 	 * addition of parsed values to parse tree. 
 	 */
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
			Util.saveFile( localFileName, output);
		} catch ( IOException e ) {
			error("saveNodes exception: "+ e.toString() );
		}
	}

}

