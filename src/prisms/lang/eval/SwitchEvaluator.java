package prisms.lang.eval;

import prisms.lang.EvaluationEnvironment;
import prisms.lang.EvaluationException;
import prisms.lang.EvaluationResult;
import prisms.lang.ParsedItem;
import prisms.lang.types.*;

/** Represents a switch/case statement */
public class SwitchEvaluator implements PrismsItemEvaluator<ParsedSwitch> {
	@Override
	public EvaluationResult evaluate(ParsedSwitch item, PrismsEvaluator evaluator, EvaluationEnvironment env, boolean asType,
		boolean withValues) throws EvaluationException {
		EvaluationResult var = evaluator.evaluate(item.getVariable(), env, false, withValues);
		if(var.getPackageName() != null || var.isType())
			throw new EvaluationException(var.getFirstVar() + " cannot be resolved to a variable", item,
				item.getVariable().getMatch().index);
		if(withValues && var.getValue() == null)
			throw new prisms.lang.ExecutionException(new prisms.lang.Type(NullPointerException.class), new NullPointerException(
				"Variable in switch statement may not be null"), item, item.getVariable().getMatch().index);

		Enum<?> [] consts = null;
		if(var.getType().canAssignTo(Enum.class))
			consts = (Enum<?> []) var.getType().getBaseType().getEnumConstants();
		else if(var.getType().canAssignTo(Integer.TYPE) || var.getType().canAssignTo(String.class)) {
		} else
			throw new EvaluationException("Only int-convertible types, strings and enums may be used in switch statements", item, item
				.getVariable().getMatch().index);

		prisms.lang.EvaluationEnvironment scoped = env.scope(true);
		boolean match = false;
		for(int c = 0; c < item.getCases().length; c++) {
			if(item.getCases()[c] == null) // Default case
				continue;
			ParsedItem case_c = item.getCases()[c];
			while(case_c instanceof ParsedParenthetic)
				case_c = ((ParsedParenthetic) case_c).getContent();
			if(!match || !withValues) {
				if(consts != null) {
					if(case_c instanceof ParsedIdentifier) {
						ParsedIdentifier id = (ParsedIdentifier) case_c;
						boolean found = false;
						for(Enum<?> con : consts)
							if(con.name().equals(id.getName())) {
								found = true;
								match = con == var.getValue();
								if(withValues)
									break;
							}
						if(!found)
							throw new EvaluationException("No such constant " + id.getName() + " in enum " + var.getType(),
								item.getCases()[c], item.getCases()[c].getMatch().index);
					} else if(case_c instanceof ParsedMethod) {
						ParsedMethod m = (ParsedMethod) item.getCases()[c];
						if(m.isMethod())
							throw new EvaluationException("The value in a case statement must be a constant", case_c,
								case_c.getMatch().index);
						EvaluationResult caseTypeRes = evaluator.evaluate(((ParsedMethod) case_c).getContext(), env, true, true);
						if(!caseTypeRes.isType())
							throw new EvaluationException("The value in a case statement must be a constant", case_c,
								case_c.getMatch().index);
						if(!var.getType().isAssignable(caseTypeRes.getType()))
							throw new EvaluationException("The value in an enum case statement must be one of the enum values", case_c,
								case_c.getMatch().index);
						boolean found = false;
						for(Enum<?> con : consts)
							if(con.name().equals(m.getName())) {
								found = true;
								match = con == var.getValue();
								if(withValues)
									break;
							}
						if(!found)
							throw new EvaluationException("No such constant " + m.getName() + " in enum " + var.getType(), case_c,
								case_c.getMatch().index);
					} else
						throw new EvaluationException("The value in a case statement must be a constant", item, case_c.getMatch().index);
				} else if(case_c instanceof ParsedString || case_c instanceof ParsedNumber || case_c instanceof ParsedChar) {
					EvaluationResult caseRes = evaluator.evaluate(case_c, env, false, withValues);
					if(!var.getType().isAssignable(caseRes.getType()))
						throw new EvaluationException("type mismatch: cannot convert from " + caseRes.getType() + " to " + var.getType(),
							case_c, case_c.getMatch().index);
					match = var.getValue().equals(caseRes.getValue());
				} else if(case_c instanceof ParsedNull)
					throw new EvaluationException("Null not allowed for case statement", item, case_c.getMatch().index);
				else if(case_c instanceof ParsedBoolean) {
					throw new prisms.lang.EvaluationException("type mismatch: cannot convert from boolean to " + var.getType(), case_c,
						case_c.getMatch().index);
				} else
					throw new EvaluationException("The value in a case statement must be a constant", item, case_c.getMatch().index);
			}
			if(match || !withValues) {
				for(ParsedItem pi : item.getCaseBlocks()[c]) {
					EvaluationResult res = StatementBlockEvaluator.executeJavaStatement(pi, evaluator, scoped, withValues);
					if(withValues && res != null && res.getControl() != null)
						switch (res.getControl()) {
						case RETURN:
							return res;
						case BREAK:
							return null;
						case CONTINUE:
							throw new EvaluationException("continue outside of loop", pi, pi.getMatch().index);
						}
				}
			}
		}
		if(!match || !withValues) {
			for(int c = 0; c < item.getCases().length; c++) {
				if(item.getCases()[c] != null)
					continue;
				for(ParsedItem pi : item.getCaseBlocks()[c]) {
					EvaluationResult res = StatementBlockEvaluator.executeJavaStatement(pi, evaluator, scoped, withValues);
					if(withValues && res != null && res.getControl() != null)
						switch (res.getControl()) {
						case RETURN:
							return res;
						case BREAK:
							return null;
						case CONTINUE:
							return res;
						}
				}
				break;
			}
		}
		return null;
	}
}
