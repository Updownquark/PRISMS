package prisms.lang.eval;

import prisms.lang.*;
import prisms.lang.types.ParsedDeclaration;
import prisms.lang.types.ParsedFunctionDeclaration;
import prisms.lang.types.ParsedType;

/** Represents a user-created function */
public class FunctionDeclarationEvaluator implements PrismsItemEvaluator<ParsedFunctionDeclaration> {

	@Override
	public EvaluationResult evaluate(ParsedFunctionDeclaration item, PrismsEvaluator evaluator, EvaluationEnvironment env, boolean asType,
		boolean withValues) throws EvaluationException {
		prisms.lang.EvaluationEnvironment scoped = env.scope(false);
		prisms.lang.EvaluationResult ret = evaluator.evaluate(item.getReturnType(), scoped, true, withValues);
		if(!ret.isType())
			throw new EvaluationException(ret + " cannot be resolved to a type", item.getReturnType(),
				item.getReturnType().getMatch().index);
		scoped.setReturnType(ret.getType());
		for(ParsedDeclaration dec : item.getParameters())
			evaluator.evaluate(dec, scoped, false, false);
		Type [] exTypes = new Type[item.getExceptionTypes().length];
		for(int i = 0; i < exTypes.length; i++)
			exTypes[i] = evaluator.evaluate(item.getExceptionTypes()[i], scoped, true, withValues).getType();
		scoped.setHandledExceptionTypes(exTypes);
		evaluator.evaluate(item.getBody(), scoped, false, false);
		env.declareFunction(item);
		return null;
	}

	/**
	 * Executes this function against a set of user-supplied parameters
	 *
	 * @param item The function declaration to execute
	 * @param evaluator The evaluator to use to evaluate various types required
	 * @param env The evaluation environment to execute the function within
	 * @param args The arguments to evaluate the function against
	 * @param withValues Whether to validate or evaluate this function
	 * @return The return value of the function
	 * @throws prisms.lang.EvaluationException If an error occurs evaluating the function or if the evaluation throws an exception
	 */
	public EvaluationResult execute(ParsedFunctionDeclaration item, PrismsEvaluator evaluator, EvaluationEnvironment env,
		EvaluationResult [] args, boolean withValues) throws EvaluationException {
		DeclarationEvaluator decEval = new DeclarationEvaluator();
		EvaluationEnvironment scoped = env.scope(false);
		if(item.getParameters().length != args.length)
			throw new IllegalStateException("Illegal parameters");
		for(int i = 0; i < item.getParameters().length; i++) {
			if(!decEval.evaluateType(item.getParameters()[i], evaluator, scoped).isAssignable(args[i].getType()))
				throw new IllegalStateException("Illegal parameters");
			evaluator.evaluate(item.getParameters()[i], scoped, false, withValues);
			if(withValues)
				scoped.setVariable(item.getParameters()[i].getName(), args[i].getValue(), item.getParameters()[i],
					item.getParameters()[i].getMatch().index);
		}
		for(ParsedType et : item.getExceptionTypes())
			if(!env.canHandle(evaluator.evaluate(et, scoped, true, withValues).getType()))
				throw new prisms.lang.EvaluationException("Unhandled exception type " + et, et, et.getMatch().index);
		prisms.lang.EvaluationResult retRes;
		retRes = evaluator.evaluate(item.getReturnType(), scoped, true, withValues);
		if(!retRes.isType())
			throw new prisms.lang.EvaluationException(retRes + " cannot be resolved to a type", item.getReturnType(), item.getReturnType()
				.getMatch().index);
		scoped.setReturnType(retRes.getType());
		prisms.lang.Type [] exTypes = new prisms.lang.Type[item.getExceptionTypes().length];
		for(int i = 0; i < exTypes.length; i++)
			exTypes[i] = evaluator.evaluate(item.getExceptionTypes()[i], scoped, true, withValues).getType();
		scoped.setHandledExceptionTypes(exTypes);
		try {
			EvaluationResult res = evaluator.evaluate(item.getBody(), scoped, false, withValues);
			if(withValues && res == null && !Void.TYPE.equals(retRes.getType().getBaseType()))
				throw new prisms.lang.EvaluationException("No value returned", item.getBody(), item.getBody().getMatch().index
					+ item.getBody().getMatch().text.length());
			if(withValues && res != null && !isAssignable(retRes.getType(), res.getValue()))
				throw new prisms.lang.EvaluationException(
					"Type mismatch: cannot convert from " + res.getType() + " to " + retRes.getType(), item.getBody(), item.getBody()
					.getMatch().index);
			return new EvaluationResult(retRes.getType(), res == null ? null : res.getValue());
		} catch(ExecutionException e) {
			if(e.getCause() instanceof Error)
				throw e;
			else if(e.getCause() instanceof RuntimeException)
				throw e;
			for(prisms.lang.Type exType : exTypes)
				if(exType.isAssignable(e.getType()))
					throw e;
			throw new prisms.lang.EvaluationException("Unhandled exception type " + e.getType(), e.getCause(), item.getBody(), item
				.getBody().getMatch().index);
		}
	}

	private static boolean isAssignable(Type t, Object value) {
		if(value == null)
			return !t.isPrimitive();
		if(t.isPrimitive()) {
			Class<?> prim = prisms.lang.Type.getPrimitiveType(value.getClass());
			return prim != null && t.isAssignableFrom(prim);
		} else
			return t.isAssignableFrom(value.getClass());
	}
}
