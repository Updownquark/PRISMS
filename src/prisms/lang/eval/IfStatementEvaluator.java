/* ParsedIfStatement.java Created Nov 17, 2011 by Andrew Butler, PSL */
package prisms.lang.eval;

import prisms.lang.EvaluationEnvironment;
import prisms.lang.EvaluationException;
import prisms.lang.EvaluationResult;
import prisms.lang.ParsedItem;
import prisms.lang.types.ParsedIfStatement;

/** Represents an if/else if/.../else structure */
public class IfStatementEvaluator implements PrismsItemEvaluator<ParsedIfStatement> {
	@Override
	public EvaluationResult evaluate(ParsedIfStatement item, PrismsEvaluator evaluator, EvaluationEnvironment env, boolean asType,
		boolean withValues) throws EvaluationException {
		boolean hit = false;
		for(int i = 0; i < item.getConditions().length && (!hit || !withValues); i++) {
			prisms.lang.EvaluationEnvironment scoped = env.scope(true);
			ParsedItem condition = item.getConditions()[i];
			EvaluationResult condRes = evaluator.evaluate(condition, scoped, false, withValues);
			if(condRes.isType() || condRes.getPackageName() != null)
				throw new EvaluationException(condRes.typeString() + " cannot be resolved to a variable", item, condition.getMatch().index);
			if(!condRes.getType().canAssignTo(Boolean.TYPE))
				throw new EvaluationException("Type mismatch: cannot convert from " + condRes.typeString() + " to boolean", item,
					condition.getMatch().index);
			hit = !withValues || ((Boolean) condRes.getValue()).booleanValue();
			if(hit) {
				EvaluationResult res = evaluator.evaluate(item.getContents(i), env, false, withValues);
				if(res != null && res.getControl() != null) {
					switch (res.getControl()) {
					case RETURN:
						if(withValues)
							return res;
						break;
					case CONTINUE:
						throw new EvaluationException(res.getControlItem().getMatch().text + " cannot be used outside of a loop",
							res.getControlItem(), res.getControlItem().getMatch().index);
					case BREAK:
						throw new EvaluationException(res.getControlItem().getMatch().text
							+ " cannot be used outside of a loop or a switch", res.getControlItem(), res.getControlItem().getMatch().index);
					}
				}
			}
		}
		if(item.hasTerminal() && (!withValues || !hit)) {
			EvaluationResult res = evaluator.evaluate(item.getContents(item.getConditions().length), env, false, withValues);
			if(res != null && res.getControl() != null) {
				switch (res.getControl()) {
				case RETURN:
					if(withValues)
						return res;
					break;
				case CONTINUE:
					throw new EvaluationException(res.getControlItem().getMatch().text + " cannot be used outside of a loop",
						res.getControlItem(), res.getControlItem().getMatch().index);
				case BREAK:
					throw new EvaluationException(res.getControlItem().getMatch().text + " cannot be used outside of a loop or a switch",
						res.getControlItem(), res.getControlItem().getMatch().index);
				}
			}
		}
		return null;
	}
}
