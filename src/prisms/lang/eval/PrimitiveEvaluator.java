/* ParsedBoolean.java Created Nov 14, 2011 by Andrew Butler, PSL */
package prisms.lang.eval;

import prisms.lang.EvaluationEnvironment;
import prisms.lang.EvaluationException;
import prisms.lang.EvaluationResult;
import prisms.lang.Type;
import prisms.lang.types.*;

/** Represents a boolean value */
public class PrimitiveEvaluator implements PrismsItemEvaluator<ParsedPrimitive> {
	@Override
	public EvaluationResult evaluate(ParsedPrimitive item, PrismsEvaluator evaluator, EvaluationEnvironment env, boolean asType,
		boolean withValues) throws EvaluationException {
		if(item instanceof ParsedBoolean || item instanceof ParsedChar || item instanceof ParsedNumber)
			return new EvaluationResult(new Type(Type.getPrimitiveType(item.getValue().getClass())), item.getValue());
		else if(item instanceof ParsedNull)
			return new EvaluationResult(Type.NULL, null);
		else
			return new EvaluationResult(new Type(item.getValue().getClass()), item.getValue());
	}
}
