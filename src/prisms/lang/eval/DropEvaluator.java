package prisms.lang.eval;

import prisms.lang.EvaluationEnvironment;
import prisms.lang.EvaluationException;
import prisms.lang.EvaluationResult;
import prisms.lang.types.ParsedDrop;
import prisms.lang.types.ParsedFunctionDeclaration;

/** Represents an operation by which a user may undo a variable declaration */
public class DropEvaluator implements PrismsItemEvaluator<ParsedDrop> {
	@Override
	public EvaluationResult evaluate(ParsedDrop item, PrismsEvaluator evaluator, EvaluationEnvironment env, boolean asType,
		boolean withValues) throws EvaluationException {
		String name = item.getName();
		if(item.getParameterTypes() == null) {
			env.dropVariable(name, item, item.getStored("name").index);
			return null;
		} else {
			DeclarationEvaluator decEval = new DeclarationEvaluator();
			for(ParsedFunctionDeclaration func : env.getDeclaredFunctions()) {
				if(!func.getName().equals(name))
					continue;
				if(func.getParameters().length != item.getParameterTypes().length)
					continue;
				for(int i = 0; i < item.getParameterTypes().length; i++)
					if(!decEval.evaluateType(func.getParameters()[i], evaluator, env).equals(
						evaluator.evaluate(item.getParameterTypes()[i], env, true, withValues).getType()))
						continue;
				env.dropFunction(func, item, item.getStored("name").index);
				return null;
			}
			throw new prisms.lang.EvaluationException("No such function " + item, item, item.getStored("name").index);
		}
	}
}
