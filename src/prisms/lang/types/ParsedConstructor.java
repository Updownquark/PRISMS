/* ParsedConstructor.java Created Nov 15, 2011 by Andrew Butler, PSL */
package prisms.lang.types;

import prisms.lang.ParsedItem;

/** Represents a constructor or anonymous class declaration */
public class ParsedConstructor extends ParsedItem {
	private ParsedType theType;

	private ParsedItem [] theArguments;

	private boolean isAnonymous;

	private ParsedStatementBlock theInstanceInitializer;

	private ParsedStatementBlock theFields;

	private ParsedFunctionDeclaration [] theMethods;

	@Override
	public void setup(prisms.lang.PrismsParser parser, ParsedItem parent, prisms.lang.ParseMatch match) throws prisms.lang.ParseException {
		super.setup(parser, parent, match);
		theType = (ParsedType) parser.parseStructures(this, getStored("type"))[0];
		if(theType.isBounded())
			throw new prisms.lang.ParseException("Cannot instantiate a generic type", getRoot().getFullCommand(), theType.getMatch().index);
		isAnonymous = getStored("anonymous") != null;
		theInstanceInitializer = (ParsedStatementBlock) parser.parseStructures(this, getStored("instanceInitializer"))[0];
		theArguments = parser.parseStructures(this, getAllStored("argument"));

		if(isAnonymous) {
			theFields = new ParsedStatementBlock(parser, this, getMatch(), parser.parseStructures(this, getAllStored("field")));
			for(ParsedItem field : theFields.getContents()) {
				if(field instanceof ParsedDeclaration) {
				} else if(field instanceof ParsedAssignmentOperator) {
					ParsedAssignmentOperator assign = (ParsedAssignmentOperator) field;
					if(!assign.getName().equals("=") || !(assign.getVariable() instanceof ParsedDeclaration))
						throw new prisms.lang.ParseException(
							"Fields in an anonymous class must be declarations or assignments of declarations", getRoot().getFullCommand(),
							field.getMatch().index);
				} else
					throw new prisms.lang.ParseException("Unrecognized field statement", getRoot().getFullCommand(), field.getMatch().index);
			}
			ParsedItem [] methods = parser.parseStructures(this, getAllStored("method"));
			theMethods = new ParsedFunctionDeclaration[methods.length];
			System.arraycopy(methods, 0, theMethods, 0, methods.length);
		}
	}

	/** @return The type that this constructor is to instantiate */
	public ParsedType getType() {
		return theType;
	}

	/** @return The arguments to this constructor */
	public ParsedItem [] getArguments() {
		return theArguments;
	}

	/** @return Whether this constructor is an anonymous class declaration */
	public boolean isAnonymous() {
		return isAnonymous;
	}

	/** @return The instance initializer of this anonymous class, if there is one */
	public ParsedStatementBlock getInstanceInitializer() {
		return theInstanceInitializer;
	}

	/** @return The statements that declare the fields for this anonymous class */
	public ParsedStatementBlock getFields() {
		return theFields;
	}

	/** @return The methods declared by this anonymous class */
	public ParsedFunctionDeclaration [] getMethods() {
		return theMethods;
	}

	@Override
	public ParsedItem [] getDependents() {
		java.util.ArrayList<ParsedItem> ret = new java.util.ArrayList<ParsedItem>();
		ret.add(theType);
		for(ParsedItem arg : theArguments)
			ret.add(arg);
		if(theInstanceInitializer != null)
			ret.add(theInstanceInitializer);
		if(theFields != null)
			for(ParsedItem item : theFields.getContents())
				ret.add(item);
		if(theMethods != null)
			for(ParsedFunctionDeclaration f : theMethods)
				ret.add(f);
		return prisms.util.ArrayUtils.add(theArguments, theType, 0);
	}

	@Override
	public void replace(ParsedItem dependent, ParsedItem toReplace) throws IllegalArgumentException {
		if(theType == dependent) {
			if(toReplace instanceof ParsedType)
				theType = (ParsedType) toReplace;
			else
				throw new IllegalArgumentException("Cannot replace the type of a constructor with " + toReplace.getClass().getSimpleName());
		} else {
			for(int i = 0; i < theArguments.length; i++)
				if(theArguments[i] == dependent) {
					theArguments[i] = toReplace;
					return;
				}
			if(theInstanceInitializer != null && theInstanceInitializer == dependent) {
				if(toReplace instanceof ParsedStatementBlock) {
					theInstanceInitializer = (ParsedStatementBlock) toReplace;
					return;
				} else
					throw new IllegalArgumentException("Cannot replace the initializer block of a constructor with "
						+ toReplace.getClass().getSimpleName());
			}
			if(theFields != null)
				for(int i = 0; i < theFields.getContents().length; i++)
					if(theFields.getContents()[i] == dependent) {
						theFields.replace(dependent, toReplace);
						return;
					}
			if(theMethods != null)
				for(int i = 0; i < theMethods.length; i++)
					if(theMethods[i] == dependent) {
						if(toReplace instanceof ParsedFunctionDeclaration) {
							theMethods[i] = (ParsedFunctionDeclaration) toReplace;
							return;
						} else
							throw new IllegalArgumentException("Cannot replace a method of a constructor (anonymous inner class) with "
								+ toReplace.getClass().getSimpleName());
					}
			throw new IllegalArgumentException("No such dependent " + dependent);
		}
	}

	@Override
	public String toString() {
		StringBuilder ret = new StringBuilder();
		ret.append("new ").append(theType.toString()).append('(');
		boolean first = true;
		for(ParsedItem arg : theArguments) {
			if(first)
				first = false;
			else
				ret.append(", ");
			ret.append(arg.toString());
		}
		ret.append(')');
		return ret.toString();
	}

}
