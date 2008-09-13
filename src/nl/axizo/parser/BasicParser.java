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
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.util.regex.*;
import java.util.Vector;

public class BasicParser {
	private static boolean showFirstTwoLines = false;
	private static int     seqNr          = 1;

	private String buffer =  "" ;

	public BasicParser(String filename) {
		buffer = loadfile( filename );
		//trace( "curpos: " + curLine( 0 ) );
	}

	public static void setDoneOutput   ( boolean val ) { Util.setDoneOutput( val); }
	public static void setTraceLevel   ( int     val ) { Util.setTraceLevel( val); }
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

	protected static void trace  (String str) { Util.trace( str); }
	protected static void info   (String str) { Util.info( str); }
	protected static void warning(String str) { Util.warning( str); }
	protected static void error  (String str) { Util.error( str); }



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
		return Util.s( this, method, oldState, doThrow, ignore );
	}


	/**
	 * Make a dynamic call to a rule.
	 *
 	 * Override of {@link #s(String,State,boolean,boolean) s(String,State,boolean,boolean)}, with
 	 * addition of parsed values to parse tree. 
 	 */
	protected boolean s( String method, State oldState, boolean doThrow ) throws ParseException {
		return Util.s( this, method, oldState, doThrow, false ); 
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
		return Util.s( this, method, oldState, false, false ); 
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

	/** 
 	 * Determine if given position in buffer is at the end of the buffer.
 	 * 
 	 * @return true if and end of buffer, false otherwise.
 	 */
	public boolean eol(int curpos) throws ParseException {
		if ( curpos > buffer.length() ) { throw new ParseException(); }

		return curpos == buffer.length();
	}
}

