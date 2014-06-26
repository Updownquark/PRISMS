/* ParsedConditional.java Created Nov 16, 2011 by Andrew Butler, PSL */
package prisms.lang.eval;

import prisms.lang.EvaluationEnvironment;
import prisms.lang.EvaluationException;
import prisms.lang.EvaluationResult;
import prisms.lang.types.ParsedConditional;

/** Represents a conditional expression of the form condition ? affirmative : negative */
public class ConditionalEvaluator implements PrismsItemEvaluator<ParsedConditional> {
	@Override
	public EvaluationResult evaluate(ParsedConditional item, PrismsEvaluator evaluator, EvaluationEnvironment env, boolean asType,
		boolean withValues) throws EvaluationException {
		EvaluationResult condition = evaluator.evaluate(item.getCondition(), env, false, withValues);
		if(condition.isType() || condition.getPackageName() != null)
			throw new EvaluationException(condition.typeString() + " cannot be resolved to a variable", item, item.getCondition()
				.getMatch().index);
		if(!Boolean.TYPE.equals(condition.getType()))
			throw new EvaluationException("Type mismatch: cannot convert from " + condition.typeString() + " to boolean", item, item
				.getCondition().getMatch().index);
		boolean condEval = withValues && ((Boolean) condition.getValue()).booleanValue();
		EvaluationResult affirm = evaluator.evaluate(item.getAffirmative(), env, false, withValues && condEval);
		if(affirm.isType() || affirm.getPackageName() != null)
			throw new EvaluationException(affirm.typeString() + " cannot be resolved to a variable", item,
				item.getAffirmative().getMatch().index);
		EvaluationResult negate = evaluator.evaluate(item.getNegative(), env, false, withValues && !condEval);
		if(negate.isType() || negate.getPackageName() != null)
			throw new EvaluationException(negate.typeString() + " cannot be resolved to a variable", item,
				item.getNegative().getMatch().index);
		prisms.lang.Type max = affirm.getType().getCommonType(negate.getType());
		if(max == null)
			throw new EvaluationException("Incompatible types in conditional expression: " + affirm.typeString() + " and "
				+ negate.typeString(), item, item.getAffirmative().getMatch().index);
		if(!withValues)
			return new EvaluationResult(max, null);
		else if(condEval)
			return new EvaluationResult(max, affirm.getValue());
		else
			return new EvaluationResult(max, negate.getValue());
	}
}
