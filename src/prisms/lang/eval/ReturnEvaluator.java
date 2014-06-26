package prisms.lang.eval;

import prisms.lang.EvaluationEnvironment;
import prisms.lang.EvaluationException;
import prisms.lang.EvaluationResult;
import prisms.lang.ParsedItem;
import prisms.lang.types.ParsedReturn;

/** Represents a return statement */
public class ReturnEvaluator implements PrismsItemEvaluator<ParsedReturn> {
	@Override
	public EvaluationResult evaluate(ParsedReturn item, PrismsEvaluator evaluator, EvaluationEnvironment env, boolean asType,
		boolean withValues) throws EvaluationException {
		ParsedItem value = item.getValue();
		if(value == null)
			return null;
		if(value instanceof prisms.lang.types.ParsedStatementBlock)
			throw new EvaluationException("Syntax error", item, value.getMatch().index);
		prisms.lang.EvaluationResult ret = evaluator.evaluate(value, env, false, withValues);
		if(ret.getPackageName() != null || ret.isType())
			throw new EvaluationException(ret.getFirstVar() + " cannot be resolved to a variable", value, value.getMatch().index);
		if(env.getReturnType() == null)
			throw new EvaluationException("Cannot return a value except from within a function or method", item, item.getMatch().index);
		if(Void.TYPE.equals(env.getReturnType().getBaseType()))
			throw new prisms.lang.EvaluationException("Void methods cannot return a value", item, item.getMatch().index);
		if(!env.getReturnType().isAssignable(ret.getType()))
			throw new prisms.lang.EvaluationException("Type mismatch: cannot convert from " + ret.getType() + " to " + env.getReturnType(),
				value, value.getMatch().index);
		return new EvaluationResult(EvaluationResult.ControlType.RETURN, ret.getValue(), item);
	}
}
