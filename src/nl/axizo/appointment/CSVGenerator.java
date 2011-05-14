/**
 *
 */
package nl.axizo.appointment;

import nl.axizo.parser.*;
import nl.axizo.EBNF.Generator;
import java.io.IOException;
import java.util.*;

/**
 *  Generates comma separated value output from a parse tree.
 *
 * The parse tree must have been created with an EBNF parser.
 */
public class CSVGenerator extends Generator {

	public static String implode(String[] arr, String delim) {
		return implode( Arrays.asList( arr), delim);
	}

	public static String implode(List vec, String delim) {
	    String out = "";
	    for(int i=0; i< vec.size(); i++) {
	        if(i!=0) { out += delim; }
	        out += vec.get(i);
	    }
	    return out;
	}

	private void add_field(Vector v, Node r, String field) {
		String tmp = r.get( field ).getValue();
		v.add( tmp );
	}

	private String generate_record(String date, Node record, String[] fields ) {
		String out = "";
		Vector v = new Vector();
		v.add( date );

		// Skip the first field, it's the placeholder for the
		// date field added here above
		for( int i = 1;  i < fields.length; ++i ) {
			add_field( v, record, fields[i] );
		}

		return "\"" + implode( v, "\",\"" ) + "\"\n";
	}


	private String generate_daylist( Node day_list, String[] fields ) {
		String out = "";
		String date = day_list.get("dateline").get("date").getValue();

		Vector records = day_list.findNodes("record");
		for( int i = 0;  i < records.size(); ++i ) {
			Node record = (Node) records.get(i);
			out += generate_record( date, record, fields );
		}

		return out;
	}

	/**
 	 * Generate output from the current node tree.
 	 */
	public void generate( State state ) throws ParseException {
		String output = "";

		Node root = state.getCurNode().get("file");;

		final String outfile   = "output.csv";

		///////////////////////////////////
		// Perform the output generation
		///////////////////////////////////

		// Fields to extract
		String[] fields = { "date", "time", "name", "age", "gender", "phone" };
	

		// Create the file header.	
		output += implode( fields, ",") + "\n";

		Vector rules = root.findNodes("day_list");
		for( int i = 0;  i < rules.size(); ++i ) {
			Node rule = (Node) rules.get(i);
			output += generate_daylist( rule, fields );
		}

		// Save what we got
		try {
			Util.info( "Outputting to file " + outfile);
			Util.saveFile( outfile, output );
		} catch( IOException e) {
			String errMsg = "error while saving '" + outfile + "': " + e.toString();
			Util.error( errMsg );
			throw new ParseException( errMsg);
		}
	}
}

