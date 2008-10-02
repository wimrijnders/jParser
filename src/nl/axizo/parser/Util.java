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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;


public class Util {
	public static final int TRACE   = 1;
	public static final int INFO    = 2;
	public static final int WARNING = 3;
	public static final int ERROR   = 4;

	private static int     traceLevel     = INFO;
	private static boolean showDoneOutput    = false;


	//////////////////////////////////////////////
	// Methods for generating tracing output
	//////////////////////////////////////////////

	public static void setTraceLevel(int val ) { 
		traceLevel     = val; 
	}

	private static void out(int traceLevel, String str) {
		if ( traceLevel >= Util.traceLevel ) {
			System.out.println( str );	
		}
	}

	public static void trace  (String str) { out( TRACE  , str); }
	public static void info   (String str) { out( INFO   , str); }
	public static void warning(String str) { out( WARNING, str); }
	public static void error  (String str) { out( ERROR  , str); }



	//////////////////////////////////////////////
	// Dynamic call methods
	//////////////////////////////////////////////
	/**
 	 * Make a dynamic call to given method.
 	 * 	
	 * Reflection is used internally to make the call.
	 *
 	 * Methods  in derived classes which reside in other packages, need
 	 * to be declared public for this to work.
 	 *
 	 * @param caller object on which to dynamically call method
 	 * @param method name of method to call
 	 * @param state state information of current call.
	 * @see <a href="http://www.rgagnon.com/javadetails/java-0031.html">Source</a>
	 */
	private static boolean dynamicCall( Object caller, String method, State state ) throws 
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
		Method thisMethod = caller.getClass().getDeclaredMethod( method, params);
		// call the method
		Boolean ret = (Boolean) thisMethod.invoke( caller , paramsObj);

		return ret.booleanValue();
	} 


	/**
	 * Make a dynamic call to a rule.
	 *
	 * The intention is to call generated syntax rules dynamically. The name is 
	 * intentionally chosen short (one letter), because this call appears often in the
	 * generated code, and we want to keep the latter readable.
	 *
 	 * @param caller object on which to dynamically call method
	 * @param method   Name of method to call
	 * @param oldState Parse state of the calling method
	 * @param doThrow  If true, throw a ParseException if parsing fails. Default is false.
	 * @param ignore   If true, do not save parsed content to parse tree. Default is false.
	 */
	static boolean s( 
		Object caller, 
		String method, 
		State oldState, 
		boolean doThrow, 
		boolean ignore 
	) throws 
			ParseException//, 
			//NoSuchMethodException, 
			//IllegalAccessException 
	{
		// TODO: Handle NoSuchMethodException and IllegalAccessException properly

		State state = oldState.copy( method );
		boolean ret = false;

		try {
			ret = dynamicCall( caller, method, state);

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



	//////////////////////////////////////////////
	//	Other methods
	//////////////////////////////////////////////

	public static void setDoneOutput   ( boolean val ) { showDoneOutput = val; }

	static String makeTab( int tabs, String fillChar ) {
		String ret = "";
		for( int i = 0; i < tabs*3; ++i ) {
			ret += fillChar;
		}
		return ret; 
	}

	static String makeTab( int tabs ) {
		return makeTab( tabs, ".");
	}


	/**
 	 * Save contents of a string to file.
 	 *
 	 * @param filename name of file to save to
 	 * @param output string containing content to put in file.
 	 */
	public static void saveFile( String filename, String output) throws IOException {
		FileWriter fw = new FileWriter( filename );
		fw.write(output);
		fw.close();
	}
}
