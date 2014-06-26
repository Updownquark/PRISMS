package prisms.lang.eval;

import prisms.lang.EvaluationEnvironment;
import prisms.lang.EvaluationException;
import prisms.lang.EvaluationResult;
import prisms.lang.Type;
import prisms.lang.types.ParsedInstanceofOp;

/** Represents a use of the instanceof operator */
public class InstanceofEvaluator implements PrismsItemEvaluator<ParsedInstanceofOp> {
	@Override
	public EvaluationResult evaluate(ParsedInstanceofOp item, PrismsEvaluator evaluator, EvaluationEnvironment env, boolean asType,
		boolean withValues) throws EvaluationException {
		Type bool = new Type(Boolean.TYPE);
		EvaluationResult type = evaluator.evaluate(item.getType(), env, true, withValues);
		if(!type.isType())
			throw new EvaluationException("Invalid type: " + item.getType().getMatch().text, item.getType(),
				item.getType().getMatch().index);
		if(type.getType().isPrimitive())
			throw new EvaluationException("Syntax error on " + item.getType().getMatch().text + ": dimensions expected after this token",
				item.getType(), item.getType().getMatch().index);
		if(type.getType().getBaseType() == null || type.getType().getParamTypes().length > 0)
			throw new EvaluationException("Cannot perform an instanceof check against a parameterized type", item.getType(), item.getType()
				.getMatch().index);
		EvaluationResult varType = evaluator.evaluate(item.getVariable(), env, false, withValues);
		if(varType.isType() || varType.getPackageName() != null)
			throw new EvaluationException(item.getVariable().getMatch().text + " cannot be resolved to a variable", item.getVariable(),
				item.getVariable().getMatch().index);
		if(type.getType().isAssignable(varType.getType()))
			return new EvaluationResult(bool, Boolean.TRUE);
		else if(!type.getType().getBaseType().isInterface() && !varType.getType().getBaseType().isInterface()
			&& !varType.getType().isAssignable(type.getType()))
			throw new EvaluationException("Incompatible conditional operand types " + varType.getType() + " and " + type.getType(), item,
				item.getVariable().getMatch().index);
		if(!withValues)
			return new EvaluationResult(bool, (Boolean) null);
		return new EvaluationResult(bool, Boolean.valueOf(type.getType().getBaseType().isInstance(varType.getValue())));
	}
}
