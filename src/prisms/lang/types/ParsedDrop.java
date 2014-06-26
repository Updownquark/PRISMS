package prisms.lang.types;

import prisms.lang.ParsedItem;

/** Represents an operation by which a user may undo a variable declaration */
public class ParsedDrop extends ParsedItem {
	private String theName;

	private ParsedType [] theParamTypes;

	@Override
	public void setup(prisms.lang.PrismsParser parser, ParsedItem parent, prisms.lang.ParseMatch match) throws prisms.lang.ParseException {
		super.setup(parser, parent, match);
		theName = getStored("name").text;
		if(getStored("function") != null) {
			ParsedItem [] params = parser.parseStructures(this, getAllStored("parameter"));
			theParamTypes = new ParsedType[params.length];
			System.arraycopy(params, 0, theParamTypes, 0, params.length);
		}
	}

	/** @return The name of the variable or function to drop */
	public String getName() {
		return theName;
	}

	/** @return The parameter types of the function to drop, or null if this drop is for a variable */
	public ParsedType [] getParameterTypes() {
		return theParamTypes;
	}

	@Override
	public ParsedItem [] getDependents() {
		if(theParamTypes == null)
			return new ParsedItem[0];
		return theParamTypes;
	}

	@Override
	public void replace(ParsedItem dependent, ParsedItem toReplace) throws IllegalArgumentException {
		if(theParamTypes != null) {
			for(int i = 0; i < theParamTypes.length; i++)
				if(theParamTypes[i] == dependent) {
					if(toReplace instanceof ParsedType) {
						theParamTypes[i] = (ParsedType) toReplace;
						return;
					} else
						throw new IllegalArgumentException("Cannot replace a drop's parameter type with " + toReplace.getClass().getName());
				}
		}
		throw new IllegalArgumentException("No such dependent " + dependent);
	}

	@Override
	public String toString() {
		StringBuilder ret = new StringBuilder();
		ret.append("drop ").append(theName);
		if(theParamTypes != null) {
			ret.append('(');
			for(int i = 0; i < theParamTypes.length; i++) {
				if(i > 0)
					ret.append(", ");
				ret.append(theParamTypes[i]);
			}
			ret.append(')');
		}
		return ret.toString();
	}
}
