/* ParsedBoolean.java Created Nov 14, 2011 by Andrew Butler, PSL */
package prisms.lang.eval;

import prisms.lang.EvaluationEnvironment;
import prisms.lang.EvaluationException;
import prisms.lang.EvaluationResult;
import prisms.lang.types.ParsedLiteral;

/** Represents a boolean value */
public class LiteralEvaluator implements PrismsItemEvaluator<ParsedLiteral> {
	@Override
	public EvaluationResult evaluate(ParsedLiteral item, PrismsEvaluator evaluator, EvaluationEnvironment env, boolean asType,
		boolean withValues) throws EvaluationException {
		return new EvaluationResult(item.getType(), item.getValue());
	}
}
