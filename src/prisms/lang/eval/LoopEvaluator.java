/* ParsedLoop.java Created Nov 16, 2011 by Andrew Butler, PSL */
package prisms.lang.eval;

import prisms.lang.EvaluationEnvironment;
import prisms.lang.EvaluationException;
import prisms.lang.EvaluationResult;
import prisms.lang.ParsedItem;
import prisms.lang.types.*;

/** Represents a loop */
public class LoopEvaluator implements PrismsItemEvaluator<ParsedLoop> {
	@Override
	public EvaluationResult evaluate(ParsedLoop item, PrismsEvaluator evaluator, EvaluationEnvironment env, boolean asType,
		boolean withValues) throws EvaluationException {
		prisms.lang.EvaluationEnvironment scoped = env.scope(true);
		for(ParsedItem init : item.getInits()) {
			if(init instanceof ParsedAssignmentOperator) {
			} else if(init instanceof ParsedDeclaration) {
			} else if(init instanceof ParsedMethod) {
				ParsedMethod method = (ParsedMethod) init;
				if(!method.isMethod())
					throw new EvaluationException("Initial expressions in a loop must be" + " declarations, assignments or method calls",
						item, init.getMatch().index);
			} else if(init instanceof ParsedConstructor) {
			} else
				throw new EvaluationException("Initial expressions in a loop must be" + " declarations, assignments or method calls", item,
					init.getMatch().index);
			evaluator.evaluate(init, scoped, false, withValues);
		}
		ParsedItem condition = item.getCondition();
		EvaluationResult condRes = evaluator.evaluate(condition, scoped, false, withValues && item.isPreCondition());
		if(condRes.isType() || condRes.getPackageName() != null)
			throw new EvaluationException(condRes.typeString() + " cannot be resolved to a variable", item, condition.getMatch().index);
		if(!Boolean.TYPE.equals(condRes.getType().getBaseType()))
			throw new EvaluationException("Type mismatch: cannot convert from " + condRes.typeString() + " to boolean", item,
				condition.getMatch().index);
		if(withValues && item.isPreCondition() && !((Boolean) condRes.getValue()).booleanValue())
			return null;

		do {
			if(env.isCanceled())
				throw new EvaluationException("User canceled execution", item, item.getMatch().index);
			EvaluationResult res = evaluator.evaluate(item.getContents(), scoped, false, withValues);
			if(res != null && withValues && res.getControl() == EvaluationResult.ControlType.RETURN)
				return res;
			for(ParsedItem inc : item.getIncrements()) {
				if(inc instanceof ParsedAssignmentOperator) {
				} else if(inc instanceof ParsedMethod) {
					ParsedMethod method = (ParsedMethod) inc;
					if(!method.isMethod())
						throw new EvaluationException("Increment expressions in a loop must be" + " assignments or method calls", item,
							inc.getMatch().index);
				} else if(inc instanceof ParsedConstructor) {
				} else
					throw new EvaluationException("Increment expressions in a loop must be" + " assignments or method calls", item,
						inc.getMatch().index);
				evaluator.evaluate(inc, scoped, false, withValues);
			}
			if(res != null && withValues && res.getControl() != null) {
				switch (res.getControl()) {
				case RETURN: // Already checked this case above
				case BREAK:
					break;
				case CONTINUE:
					continue;
				}
			}
			if(withValues)
				condRes = evaluator.evaluate(condition, scoped, false, true);
		} while(withValues && ((Boolean) condRes.getValue()).booleanValue());
		return null;
	}
}
