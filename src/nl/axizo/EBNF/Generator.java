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
 */
package nl.axizo.EBNF;

import nl.axizo.parser.*;
import java.io.IOException;
import java.util.Vector;
import java.util.Map;
import java.util.Hashtable;

/**
 *  Generates output from a parse tree.
 *
 * The parse tree must have been created with an EBNF parser.
 */
public abstract class Generator {

	/**
 	 * Generate output from the current node tree.
 	 */
	public abstract void generate( State state, String outfile ) throws ParseException;
}

