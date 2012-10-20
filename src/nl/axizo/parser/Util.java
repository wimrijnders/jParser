/**
 * Copyright 2012 Wim Rijnders <wrijnders@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *=========================================================================
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
import java.util.Hashtable;
import java.util.Map;
import org.apache.log4j.Logger;
import org.apache.log4j.Priority;
import org.apache.log4j.Level;


public class Util {

	private static Logger logger = org.apache.log4j.Logger.getLogger(Util.class);


	//////////////////////////////////////////////
	// Flag definitions for dynamic calls 
	//////////////////////////////////////////////


	 /** If flag set, throw a ParseException if parsing fails. */
	public static final int DO_THROW  = 1;
	 /** If set, do not save parsed content to parse tree.  */
	public static final int DO_IGNORE = 4;
	public static final int DO_RAISE  = 2;

	static boolean do_throw( int flags) { return (flags & DO_THROW) > 0; }
	static boolean do_raise( int flags) { return (flags & DO_RAISE) > 0; }
	static boolean do_ignore(int flags) { return (flags & DO_IGNORE) > 0; }

	//////////////////////////////////////////////
	// Class variables 
	//////////////////////////////////////////////

	// TODO: Check if following gives reentrancy problems
	private static Map declaredMethods = new Hashtable(); 

	//////////////////////////////////////////////
	// Definitions for logging. 
	//////////////////////////////////////////////

	public static final int TRACE   = 10;
	public static final int INFO    = 20;
	public static final int WARNING = 30;
	public static final int ERROR   = 40;

	private static int     traceLevel     = INFO;
	private static boolean showDoneOutput    = false;

	//////////////////////////////////////////////
	// Methods for generating tracing output
	//////////////////////////////////////////////

	public static void setTraceLevel(int val ) { 
		traceLevel     = val; 
	}


	private static void out(int traceLevel, String str, boolean always_display) {
		if ( always_display ) {
			System.out.println( str );
			return;
		}

		// Util. qualifier needed in following
		if ( traceLevel >= Util.traceLevel ) {
			if ( check_last_trace( traceLevel, str ) ) {
				System.out.println( str );
			}
		}
	}

	private static void out(int traceLevel, String str ) {
		out(traceLevel, str, false);
	}


	// Following should fix reentrancy if consecutive calls made on single instance
	// TODO: check if problems with multiple parallel calls.
	public static void init() {
		declaredMethods = new Hashtable(); 
	}

	private static String last_trace = null;
	private static int trace_count = -1;


	/**
	 * Get the current debug level.
	 *
	 * If current Logger doesn't have a level defined, take it 
	 * from the hierarchy root.
	 *
	 * Source: http://markmail.org/message/ko5f3qnewfcsl37b#query:+page:1+mid:xpnbfye4y7lnuhnl+state:results
	 *
	 * @return Current debug level
	 *
	 * TODO
	 * ====
	 *
	 * - Perhaps walk the Category hierarchy to determine
	 *   correct level.
	 */
	public static Level getDebugLevel () {
		Level level = logger.getLevel();

		if (level == null) {
			Logger root = logger.getRootLogger();

			return root.getLevel();
		}

		return level;
	}

	/**
	 * Version using log4j statusses.
	 *
	 * @return true if given str should be displayed.
	 */
	private static boolean check_last_trace( Priority level, String str ) {
		if ( str.equals( last_trace ) ) {
			trace_count += 1;
			return false;
		} else {
			if ( trace_count > 0 ) {
				logger.log( level, "Last message repeated " + trace_count + " times.");
			}
			last_trace = str;
			trace_count = 0;
			return true;
		}
	}


	/**
	 * Version using internal parser logging.
	 *
	 * @return true if given str should be displayed.
	 */
	private static boolean check_last_trace( int level, String str ) {
		//logger.info( "str: " + str + "; last_trace: " + last_trace );
		//logger.info( "trace_count: " + trace_count );

		if ( str.equals( last_trace ) ) {
			trace_count += 1;
			return false;
		} else {
			if ( trace_count > 0 ) {
				out( level, "Last message repeated " + trace_count + " times.", true );
			}
			last_trace = str;
			trace_count = 0;
			return true;
		}
	}

	private static void log( Priority level, String str ) {
		if ( level.isGreaterOrEqual( getDebugLevel() ) ) {
			if ( check_last_trace( level, str ) ) {
				logger.log( level, str );
			}
		}
	}

	public static void trace  (int level, String str) { out( level  , str); }
	public static void trace  (String str) { log( Priority.DEBUG, str ); }
	public static void info   (String str) { log( Priority.INFO , str); }
	public static void warning(String str) { log( Priority.WARN, str); }
	public static void error  (String str) { log( Priority.ERROR, str); }



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
		Method thisMethod;

		// Note that we don't differentiate overloads, ie. params not
		// taken into account in key values.
		if ( declaredMethods.containsKey( method ) ) {
			// Found it - use this value
			//warning( "Found method: " + method );
			thisMethod = (Method) declaredMethods.get( method );
		} else {
			// Not in map; find it dynamically
			thisMethod = caller.getClass().getDeclaredMethod( method, params);

			// Save the found value
			declaredMethods.put( method, thisMethod );
		}
			// Use following line if found methods not stored
			// in Map.
			//thisMethod = caller.getClass().getDeclaredMethod( method, params);

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
	 * @param flags Flags indicating special handling per dynamic call. 
	 */
	static boolean s( 
		Object caller, 
		String method, 
		State oldState,
		int flags
	) throws ParseException {

		// TODO: Handle NoSuchMethodException and IllegalAccessException properly

		State state = oldState.copy( method );
		boolean ret = false;

		try {
			ret = dynamicCall( caller, method, state);

			if ( ret ) {
				oldState.success( state, do_ignore(flags) );
				if ( showDoneOutput ) {
					info( "Done" + Util.makeTab( state.getDepth(), " ") + method );
				}
			}
		} catch ( InvocationTargetException e ) {
			if ( e.getCause() instanceof ParseException ) {
				if ( do_throw(flags) ) {
					//info("method: " + method  + " threw ParseException.");
					oldState.setError( state, method, do_raise(flags) );
					//throw (ParseException) e.getCause();
					ParseException pe = new ParseException();
					pe.setMethod( method );
					throw pe;
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
			oldState.setError( state, method, do_raise(flags) );
			if ( do_throw( flags ) ) { 
				//info("Error in method: " + method  + "throwing ParseException.");
				ParseException pe = new ParseException();
				pe.setMethod( method );
				throw pe;
			}
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


	/**
	 * Print a stacktrace to String.
	 */
	public static String stacktraceToString(Throwable throwable)
   	{
    	Writer result = new StringWriter();
    	PrintWriter printWriter = new PrintWriter(result);

    	throwable.printStackTrace(printWriter);
    	return result.toString();
   	}


	/**
	 * Return the line in the buffer starting at given position, and also 
	 * the entire next line.
	 */
	public static String curLine(String buffer, int curpos ) {
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
}
