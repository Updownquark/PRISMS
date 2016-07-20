package prisms.lang.types;

import prisms.lang.ParsedItem;

/** Represents a type name */
public class ParsedType extends prisms.lang.ParsedItem {
	private String theName;

	private int theArrayDimension;

	private boolean isParameterized;

	private ParsedType [] theParamTypes;

	private boolean isBounded;

	private ParsedType theBound;

	private boolean isUpperBound;

	@Override
	public void setup(prisms.lang.PrismsParser parser, prisms.lang.ParsedItem parent, prisms.lang.ParseMatch match)
		throws prisms.lang.ParseException {
		super.setup(parser, parent, match);
		isBounded = false;
		theParamTypes = new ParsedType[0];
		if(getStored("base") != null) {
			ParsedItem base = parser.parseStructures(this, getStored("base"))[0];
			if(base instanceof ParsedType)
				theName = ((ParsedType) base).getName();
			else if(base instanceof ParsedIdentifier)
				theName = ((ParsedIdentifier) base).getName();
			else
				throw new prisms.lang.ParseException("Unrecognized base type for type: " + base.getClass().getName(), getRoot()
					.getFullCommand(), base.getMatch().index);
		} else {
			String name = "";
			for(prisms.lang.ParseMatch m : getAllStored("name")) {
				if(name.length() > 0)
					name += ".";
				name += m.text;
			}
			theName = name;
		}
		if(getStored("wildcard") != null) {
			isBounded = true;
			prisms.lang.ParseMatch boundType = getStored("bound-type");
			isUpperBound = boundType == null || boundType.text.equals("extends");
			if(getStored("bound") != null)
				theBound = (ParsedType) parser.parseStructures(this, getStored("bound"))[0];
			else
				setup(parser, parent, getStored("type"));
		}
		ParsedItem [] pts = parser.parseStructures(this, getAllStored("paramType"));
		isParameterized = pts.length > 0 || getStored("parameterized") != null;
		theParamTypes = new ParsedType[pts.length];
		System.arraycopy(pts, 0, theParamTypes, 0, pts.length);
		theArrayDimension = getAllStored("array").length;
	}

	/**
	 * @return The name of this type. This may be:
	 *         <ul>
	 *         <li>The name of the type declared</li>
	 *         <li>The raw type if this represents a parameterized type</li>
	 *         <li>null if this represents a wildcard type</li>
	 *         </ul>
	 */
	public String getName() {
		return theName;
	}

	/** @return The dimension of this type if it is an array */
	public int getArrayDimension() {
		return theArrayDimension;
	}

	/** Increases the array dimension of this type by 1 */
	public void addArrayDimension() {
		theArrayDimension++;
	}

	/**
	 * @return Whether this type is parameterized. This may be true even if {@link #getParameterTypes()} is zero-length in the case of the
	 *         use of the diamond operator.
	 */
	public boolean isParameterized() {
		return isParameterized;
	}

	/** @return The parameter types of this generic type */
	public ParsedType [] getParameterTypes() {
		return theParamTypes;
	}

	/** @return Whether this type is a bounded (wild card) type */
	public boolean isBounded() {
		return isBounded;
	}

	/** @return The bound of this type */
	public ParsedType getBound() {
		return theBound;
	}

	/** @return Whether this bounded type is upper-bounded or lower-bounded */
	public boolean isUpperBound() {
		return isUpperBound;
	}

	@Override
	public prisms.lang.ParsedItem[] getDependents() {
		if(theBound != null)
			return new prisms.lang.ParsedItem[] {theBound};
		else
			return theParamTypes;
	}

	@Override
	public void replace(ParsedItem dependent, ParsedItem toReplace) throws IllegalArgumentException {
		if(dependent == theBound) {
			if(toReplace instanceof ParsedType || toReplace == null)
				theBound = (ParsedType) toReplace;
			else
				throw new IllegalArgumentException("Cannot replace the type bound with " + toReplace.getClass().getSimpleName());
		} else
			throw new IllegalArgumentException("No such dependent " + dependent);
	}

	@Override
	public String toString() {
		StringBuilder ret = new StringBuilder();
		if(theName != null) {
			ret.append(theName);
			if(theParamTypes.length > 0) {
				ret.append('<');
				for(int i = 0; i < theParamTypes.length; i++) {
					if(i > 0)
						ret.append(", ");
					ret.append(theParamTypes[i]);
				}
				ret.append('>');
			}
		} else {
			ret.append('?');
			if(theBound != null) {
				ret.append(isUpperBound ? "extends " : "super ");
				ret.append(theBound);
			}
		}
		for(int i = 0; i < theArrayDimension; i++) {
			if(i == 0)
				ret.append(' ');
			ret.append("[]");
		}
		return ret.toString();
	}
}
