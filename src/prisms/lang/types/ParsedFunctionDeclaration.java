package prisms.lang.types;

import prisms.lang.ParsedItem;

/** Represents a user-created function */
public class ParsedFunctionDeclaration extends ParsedItem {
	private String theName;

	private ParsedDeclaration [] theParameters;

	private ParsedItem theReturnType;

	private ParsedType [] theExceptionTypes;

	private ParsedStatementBlock theBody;

	@Override
	public void setup(prisms.lang.PrismsParser parser, ParsedItem parent, prisms.lang.ParseMatch match) throws prisms.lang.ParseException {
		super.setup(parser, parent, match);
		theName = getStored("name").text;
		theReturnType = parser.parseStructures(this, getStored("returnType"))[0];
		theBody = (ParsedStatementBlock) parser.parseStructures(this, getStored("body"))[0];
		ParsedItem [] params = parser.parseStructures(this, getAllStored("parameter"));
		theParameters = new ParsedDeclaration[params.length];
		System.arraycopy(params, 0, theParameters, 0, params.length);
		ParsedItem [] exs = parser.parseStructures(this, getAllStored("exception"));
		theExceptionTypes = new ParsedType[exs.length];
		System.arraycopy(exs, 0, theExceptionTypes, 0, exs.length);
		for(int i = 0; i < theParameters.length - 1; i++)
			if(theParameters[i].isVarArg())
				throw new prisms.lang.ParseException(
					"Vararg declarations may only exist on the last parameter of a function/method declaration",
					getRoot().getFullCommand(), theParameters[i].getStored("vararg").index);
	}

	/** @return The name of the function */
	public String getName() {
		return theName;
	}

	/** @return The declarations of the function's parameter arguments */
	public ParsedDeclaration [] getParameters() {
		return theParameters;
	}

	/**
	 * A placeholder for future functionality
	 * 
	 * @return Whether this function takes variable arguments
	 */
	public boolean isVarArgs() {
		return theParameters.length > 0 && theParameters[theParameters.length - 1].isVarArg();
	}

	/** @return The type of this function's return value */
	public ParsedItem getReturnType() {
		return theReturnType;
	}

	/** @return The types of exceptions that this function may throw */
	public ParsedType [] getExceptionTypes() {
		return theExceptionTypes;
	}

	/** @return This function's body */
	public ParsedStatementBlock getBody() {
		return theBody;
	}

	@Override
	public ParsedItem [] getDependents() {
		java.util.ArrayList<ParsedItem> ret = new java.util.ArrayList<>();
		ret.add(theReturnType);
		for(int p = 0; p < theParameters.length; p++)
			ret.add(theParameters[p]);
		for(int e = 0; e < theExceptionTypes.length; e++)
			ret.add(theExceptionTypes[e]);
		ret.add(theBody);
		return ret.toArray(new ParsedItem[ret.size()]);
	}

	@Override
	public void replace(ParsedItem dependent, ParsedItem toReplace) throws IllegalArgumentException {
		if(theReturnType == dependent) {
			theReturnType = toReplace;
			return;
		}
		for(int i = 0; i < theParameters.length; i++)
			if(theParameters[i] == dependent) {
				if(toReplace instanceof ParsedDeclaration) {
					theParameters[i] = (ParsedDeclaration) toReplace;
					return;
				} else
					throw new IllegalArgumentException("Cannot replace a declared parameter of a function declaration with "
						+ toReplace.getClass().getSimpleName());
			}
		for(int i = 0; i < theExceptionTypes.length; i++)
			if(theExceptionTypes[i] == dependent) {
				if(toReplace instanceof ParsedType) {
					theExceptionTypes[i] = (ParsedType) toReplace;
					return;
				} else
					throw new IllegalArgumentException("Cannot replace a declared exception type of a function declaration with "
						+ toReplace.getClass().getSimpleName());
			}
		if(theBody == dependent) {
			if(toReplace instanceof ParsedStatementBlock) {
				theBody = (ParsedStatementBlock) toReplace;
				return;
			} else
				throw new IllegalArgumentException("Cannot replace the body of a function declaration with "
					+ toReplace.getClass().getSimpleName());
		}
		throw new IllegalArgumentException("No such dependent " + dependent);
	}

	/** @return A short representation of this function's signature */
	public String getShortSig() {
		StringBuilder ret = new StringBuilder();
		ret.append(theName);
		ret.append('(');
		for(int p = 0; p < theParameters.length; p++) {
			if(p > 0)
				ret.append(", ");
			ret.append(theParameters[p].getType());
			if(theParameters[p].getTypeParams().length > 0) {
				ret.append('<');
				boolean first = true;
				for(ParsedType p2 : theParameters[p].getTypeParams()) {
					if(!first)
						ret.append(", ");
					else
						first = false;
					ret.append(p2);
				}
			}
			for(int i = 0; i < theParameters[p].getArrayDimension() - 1; i++)
				ret.append("[]");
			if(theParameters[p].isVarArg())
				ret.append("...");
			else if(theParameters[p].getArrayDimension() > 0)
				ret.append("[]");
		}
		ret.append(')');
		return ret.toString();
	}

	/**
	 * Checks to see if this function's call signature is identical to another function's. This method checks name and parameters, but not
	 * parameter names, declared throwable types or functionality.
	 * 
	 * @param pfd The function to compare to
	 * @return Whether this function's call signature is identical to <code>pfd</code>'s
	 */
	public boolean equalsCallSig(ParsedFunctionDeclaration pfd) {
		if(!pfd.theName.equals(theName))
			return false;
		if(pfd.theParameters.length != theParameters.length)
			return false;
		for(int p = 0; p < theParameters.length; p++)
			if(!theParameters[p].getType().equals(pfd.theParameters[p].getType()))
				return false;
		if(pfd.isVarArgs() != isVarArgs())
			return false;
		return true;
	}

	@Override
	public String toString() {
		StringBuilder ret = new StringBuilder();
		ret.append(theReturnType).append(' ');
		ret.append(theName);
		ret.append('(');
		for(int p = 0; p < theParameters.length; p++) {
			if(p > 0)
				ret.append(", ");
			ret.append(theParameters[p]);
		}
		ret.append(")\n");
		ret.append(theBody);
		return ret.toString();
	}
}
