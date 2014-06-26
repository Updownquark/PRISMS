package prisms.lang.eval;

import prisms.lang.EvaluationEnvironment;
import prisms.lang.EvaluationException;
import prisms.lang.EvaluationResult;
import prisms.lang.types.ParsedDeclaration;
import prisms.lang.types.ParsedTryCatchFinally;

/** Evaluates a try{}[catch(? extends Throwable){}]*[finally{}]? statement */
public class TryCatchFinallyEvaluator implements PrismsItemEvaluator<ParsedTryCatchFinally> {
	@Override
	public EvaluationResult evaluate(ParsedTryCatchFinally item, PrismsEvaluator evaluator, EvaluationEnvironment env, boolean asType,
		boolean withValues) throws EvaluationException {
		DeclarationEvaluator decEval = new DeclarationEvaluator();
		prisms.lang.EvaluationEnvironment scoped;
		ParsedDeclaration [] catches = item.getCatchDeclarations();
		prisms.lang.Type [] exTypes = new prisms.lang.Type[catches.length];
		for(int i = 0; i < exTypes.length; i++)
			exTypes[i] = decEval.evaluateType(catches[i], evaluator, env);
		if(!withValues) {
			scoped = env.scope(true);
			scoped.setHandledExceptionTypes(exTypes);
			evaluator.evaluate(item.getTryBlock(), scoped, false, withValues);
			for(int i = 0; i < catches.length; i++) {
				scoped = env.scope(true);
				evaluator.evaluate(catches[i], scoped, true, withValues);
				evaluator.evaluate(item.getCatchBlocks()[i], scoped, false, withValues);
			}
			if(item.getFinallyBlock() != null) {
				scoped = env.scope(true);
				evaluator.evaluate(item.getFinallyBlock(), scoped, false, withValues);
			}
			return null;
		} else {
			try {
				scoped = env.scope(true);
				scoped.setHandledExceptionTypes(exTypes);
				return evaluator.evaluate(item.getTryBlock(), scoped, false, withValues);
			} catch(prisms.lang.ExecutionException e) {
				for(int i = 0; i < catches.length; i++) {
					scoped = env.scope(true);
					prisms.lang.EvaluationResult catchType = evaluator.evaluate(catches[i].getType(), env, true, true);
					if(catchType.getType().isAssignableFrom(e.getCause().getClass())) {
						evaluator.evaluate(catches[i], scoped, true, withValues);
						scoped.setVariable(catches[i].getName(), e.getCause(), catches[i], catches[i].getMatch().index);
						return evaluator.evaluate(item.getCatchBlocks()[i], scoped, false, withValues);
					}
				}
				throw e;
			} finally {
				if(item.getFinallyBlock() != null) {
					scoped = env.scope(true);
					prisms.lang.EvaluationResult finallyRes = evaluator.evaluate(item.getFinallyBlock(), env, false, withValues);
					if(finallyRes != null)
						return finallyRes;
				}
			}
		}
	}
}
