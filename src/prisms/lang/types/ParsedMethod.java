/* ParsedMethod.java Created Nov 14, 2011 by Andrew Butler, PSL */
package prisms.lang.types;

import prisms.lang.ParseException;
import prisms.lang.ParseMatch;
import prisms.lang.ParsedItem;
import prisms.lang.PrismsParser;

/**
 * Represents one of:
 * <ul>
 * <li><b>A function:</b> An operation with no context, in the form of fn(arg1, arg2...)</li>
 * <li><b>A field:</b> A property out of a context, in the form of ctx.fieldName</li>
 * <li><b>A method:</b> An operation with a context, in the form of ctx.fn(arg1, arg2...)</li>
 * </ul>
 */
public class ParsedMethod extends Assignable {
	private String theName;

	private boolean isMethod;

	private prisms.lang.ParsedItem theContext;

	private prisms.lang.ParsedItem [] theArguments;

	@Override
	public void setup(PrismsParser parser, ParsedItem parent, ParseMatch match) throws ParseException {
		super.setup(parser, parent, match);
		theName = getStored("name").text;
		isMethod = getStored("method") != null;
		ParseMatch miMatch = getStored("context");
		if(miMatch != null)
			theContext = parser.parseStructures(this, miMatch)[0];
		theArguments = parser.parseStructures(this, getAllStored("parameter"));
	}

	/** @return The name of this field or method */
	public String getName() {
		return theName;
	}

	/** @return Whether this represents a method or a field */
	public boolean isMethod() {
		return isMethod;
	}

	/** @return The instance on which this field or method was invoked */
	public ParsedItem getContext() {
		return theContext;
	}

	/** @return The arguments to this method */
	public ParsedItem [] getArguments() {
		return theArguments;
	}

	@Override
	public ParsedItem [] getDependents() {
		ParsedItem [] ret = theArguments;
		if(theContext != null)
			ret = org.qommons.ArrayUtils.add(theArguments, theContext, 0);
		return ret;
	}

	@Override
	public void replace(ParsedItem dependent, ParsedItem toReplace) throws IllegalArgumentException {
		for(int i = 0; i < theArguments.length; i++)
			if(theArguments[i] == dependent) {
				theArguments[i] = toReplace;
				return;
			}
		throw new IllegalArgumentException("No such dependent " + dependent);
	}

	@Override
	public String toString() {
		StringBuilder ret = new StringBuilder();
		if(theContext != null)
			ret.append(theContext.toString()).append('.');
		ret.append(theName);
		if(isMethod) {
			ret.append('(');
			for(int i = 0; i < theArguments.length; i++) {
				if(i > 0)
					ret.append(", ");
				ret.append(theArguments[i].toString());
			}
			ret.append(')');
		}
		return ret.toString();
	}
}
