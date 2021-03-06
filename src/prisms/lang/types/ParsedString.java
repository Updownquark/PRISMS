/* ParsedString.java Created Nov 11, 2011 by Andrew Butler, PSL */
package prisms.lang.types;

import prisms.lang.ParseException;
import prisms.lang.Type;

/** Represents a character string */
public class ParsedString extends ParsedLiteral {
	@Override
	public Object parseValue(String text) throws ParseException {
		return unescape(text);
	}

	@Override
	public Type getType() {
		return new Type(String.class);
	}

	/** @return The parsed string that this structure represents */
	@Override
	public String getValue() {
		return (String) super.getValue();
	}

	static String unescape(String str) {
		StringBuilder ret = new StringBuilder(str);
		org.qommons.QommonsUtils.replaceAll(ret, "\\\"", "\"");
		org.qommons.QommonsUtils.replaceAll(ret, "\\n", "\n");
		org.qommons.QommonsUtils.replaceAll(ret, "\\t", "\t");
		return org.qommons.QommonsUtils.decodeUnicode(ret.toString());
	}

	static String escape(String str) {
		StringBuilder ret = new StringBuilder(str);
		org.qommons.QommonsUtils.replaceAll(ret, "\"", "\\\"");
		org.qommons.QommonsUtils.replaceAll(ret, "\n", "\\n");
		org.qommons.QommonsUtils.replaceAll(ret, "\t", "\\t");
		org.qommons.QommonsUtils.encodeUnicode(ret);
		return ret.toString();
	}

	@Override
	public String toString() {
		return "\"" + escape(getValue()) + "\"";
	}
}
