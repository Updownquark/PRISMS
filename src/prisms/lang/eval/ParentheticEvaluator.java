/* ParsedParenthetic.java Created Nov 14, 2011 by Andrew Butler, PSL */
package prisms.lang.eval;

import prisms.lang.EvaluationEnvironment;
import prisms.lang.EvaluationException;
import prisms.lang.EvaluationResult;
import prisms.lang.types.Assignable;
import prisms.lang.types.ParsedAssignmentOperator;
import prisms.lang.types.ParsedParenthetic;

/** Represents a parenthetic expression */
public class ParentheticEvaluator implements AssignableEvaluator<ParsedParenthetic> {
	@Override
	public EvaluationResult evaluate(ParsedParenthetic item, PrismsEvaluator evaluator, EvaluationEnvironment env, boolean asType,
		boolean withValues) throws EvaluationException {
		return evaluator.evaluate(item.getContent(), env, asType, withValues);
	}

	@Override
	public EvaluationResult getValue(ParsedParenthetic item, PrismsEvaluator eval, EvaluationEnvironment env,
		ParsedAssignmentOperator assign) throws EvaluationException {
		PrismsItemEvaluator<?> evaluator = eval.getEvaluatorFor(item.getContent().getClass());
		if(!(item.getContent() instanceof Assignable && evaluator instanceof AssignableEvaluator))
			throw new EvaluationException("Invalid argument for assignment operator " + assign.getName(), item, item.getContent()
				.getMatch().index);
		return ((AssignableEvaluator<Assignable>) evaluator).getValue((Assignable) item.getContent(), eval, env, assign);
	}

	@Override
	public void assign(ParsedParenthetic item, EvaluationResult value, PrismsEvaluator eval, EvaluationEnvironment env,
		ParsedAssignmentOperator assign) throws EvaluationException {
		PrismsItemEvaluator<?> evaluator = eval.getEvaluatorFor(item.getContent().getClass());
		if(!(item.getContent() instanceof Assignable && evaluator instanceof AssignableEvaluator))
			throw new EvaluationException("Invalid argument for assignment operator " + assign.getName(), item, item.getContent()
				.getMatch().index);
		((AssignableEvaluator<Assignable>) evaluator).assign((Assignable) item.getContent(), value, eval, env, assign);
	}
}
