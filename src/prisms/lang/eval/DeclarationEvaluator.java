/* ParsedDeclaration.java Created Nov 14, 2011 by Andrew Butler, PSL */
package prisms.lang.eval;

import prisms.lang.EvaluationEnvironment;
import prisms.lang.EvaluationException;
import prisms.lang.EvaluationResult;
import prisms.lang.Type;
import prisms.lang.types.ParsedAssignmentOperator;
import prisms.lang.types.ParsedDeclaration;

/** Represents a typed, parsed declaration */
public class DeclarationEvaluator implements AssignableEvaluator<ParsedDeclaration> {

	@Override
	public EvaluationResult evaluate(ParsedDeclaration item, PrismsEvaluator evaluator, EvaluationEnvironment env, boolean asType,
		boolean withValues) throws EvaluationException {
		Type type = evaluateType(item, evaluator, env);
		env.declareVariable(item.getName(), type, item.isFinal(), item, item.getStored("name").index);
		return null;
	}

	/**
	 * @param item The declaration to evaluate the type of
	 * @param evaluator The evaluator to evaluate dependencies
	 * @param env The evaluation environment to use to evaluate this declaration's type
	 * @return The type of this declaration
	 * @throws EvaluationException If the type cannot be evaluated
	 */
	public Type evaluateType(ParsedDeclaration item, PrismsEvaluator evaluator, EvaluationEnvironment env) throws EvaluationException {
		EvaluationResult res = evaluator.evaluate(item.getType(), env, true, false);
		if(!res.isType())
			throw new EvaluationException(item.getType().getMatch().text + " cannot be resolved to a type", item,
				item.getType().getMatch().index);
		Type ret = res.getType();
		if(item.getTypeParams().length > 0) {
			Type [] ptTypes = new Type[item.getTypeParams().length];
			for(int p = 0; p < ptTypes.length; p++)
				ptTypes[p] = evaluator.evaluate(item.getTypeParams()[p], env, true, true).getType();
			if(ptTypes.length > 0 && ret.getBaseType().getTypeParameters().length == 0) {
				String args = prisms.util.ArrayUtils.toString(ptTypes);
				args = args.substring(1, args.length() - 1);
				int index = item.getType().getMatch().index + item.getType().getMatch().text.length();
				throw new prisms.lang.EvaluationException("The type " + ret
					+ " is not generic; it cannot be parameterized with arguments <" + args + ">", item.getType(), index);
			}
			if(ptTypes.length > 0 && ptTypes.length != ret.getBaseType().getTypeParameters().length) {
				String type = ret.getBaseType().getName() + "<";
				for(java.lang.reflect.Type t : ret.getBaseType().getTypeParameters())
					type += t + ", ";
				type = type.substring(0, type.length() - 2);
				type += ">";
				String args = prisms.util.ArrayUtils.toString(ptTypes);
				args = args.substring(1, args.length() - 1);
				int index = item.getType().getMatch().index + item.getType().getMatch().text.length();
				throw new EvaluationException("Incorrect number of arguments for type " + type
					+ "; it cannot be parameterized with arguments <" + args + ">", item.getType(), index);
			}
			ret = new Type(ret.getBaseType(), ptTypes);
		}
		if(item.getArrayDimension() > 0) {
			for(int i = 0; i < item.getArrayDimension(); i++)
				ret = ret.getArrayType();
		}
		return ret;
	}

	@Override
	public EvaluationResult getValue(ParsedDeclaration item, PrismsEvaluator eval, EvaluationEnvironment env,
		ParsedAssignmentOperator assign) throws EvaluationException {
		evaluate(item, eval, env, false, false);
		throw new EvaluationException("Syntax error: " + item.getName() + " has not been assigned", item, item.getStored("name").index);
	}

	@Override
	public void assign(ParsedDeclaration item, EvaluationResult value, PrismsEvaluator eval, EvaluationEnvironment env,
		ParsedAssignmentOperator assign) throws EvaluationException {
		Type type = evaluateType(item, eval, env);

		if(!type.isAssignable(value.getType()))
			throw new EvaluationException("Type mismatch: Cannot convert from " + value.getType() + " to " + type, item, assign
				.getOperand().getMatch().index);
		env.setVariable(item.getName(), value.getValue(), assign, assign.getStored("name").index);
	}
}
