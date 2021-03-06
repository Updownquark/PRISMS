/* ParsedChar.java Created Nov 15, 2011 by Andrew Butler, PSL */
package prisms.lang.types;

import prisms.lang.ParseException;
import prisms.lang.Type;

/** Represents a character literal */
public class ParsedChar extends ParsedLiteral {
	@Override
	public Type getType() {
		return new Type(Character.TYPE);
	}

	@Override
	public Object parseValue(String text) throws ParseException {
		text = org.qommons.QommonsUtils.decodeUnicode(text);
		if(text.length() != 1)
			throw new ParseException("Invalid character constant", getRoot().getFullCommand(), getStored("value").index);
		return text.charAt(0);
	}

	@Override
	public Character getValue() {
		return (Character) super.getValue();
	}

	@Override
	public String toString() {
		StringBuilder ret = new StringBuilder('\'').append(getValue().charValue()).append('\'');
		org.qommons.QommonsUtils.encodeUnicode(ret);
		return ret.toString();
	}
}
