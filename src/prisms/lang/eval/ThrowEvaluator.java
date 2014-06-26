package prisms.lang.eval;

import prisms.lang.EvaluationEnvironment;
import prisms.lang.EvaluationException;
import prisms.lang.EvaluationResult;
import prisms.lang.ExecutionException;
import prisms.lang.types.ParsedStatementBlock;
import prisms.lang.types.ParsedThrow;

/** Represents a throw statement */
public class ThrowEvaluator implements PrismsItemEvaluator<ParsedThrow> {
	@Override
	public EvaluationResult evaluate(ParsedThrow item, PrismsEvaluator evaluator, EvaluationEnvironment env, boolean asType,
		boolean withValues) throws EvaluationException {
		if(item.getValue() == null)
			return null;
		if(item.getValue() instanceof ParsedStatementBlock)
			throw new EvaluationException("Syntax error", item, item.getValue().getMatch().index);
		EvaluationResult ret = evaluator.evaluate(item.getValue(), env, false, withValues);
		if(ret.getPackageName() != null || ret.isType())
			throw new prisms.lang.EvaluationException(ret.getFirstVar() + " cannot be resolved to a variable", item.getValue(), item
				.getValue().getMatch().index);
		if(!ret.getType().canAssignTo(Throwable.class))
			throw new EvaluationException("No exception of type " + ret.getType()
				+ " may be thrown; an exception type must be a subclass of Throwable", item.getValue(), item.getValue().getMatch().index);
		if(!env.canHandle(ret.getType()))
			throw new EvaluationException("Unhandled exception type " + ret.getType(), item.getValue(), item.getValue().getMatch().index);
		if(!withValues)
			return null;
		Throwable toThrow = (Throwable) ret.getValue();
		if(toThrow == null)
			toThrow = new NullPointerException();
		throw new ExecutionException(ret.getType(), toThrow, item, item.getMatch().index);
	}
}
