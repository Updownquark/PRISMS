package prisms.lang.eval;

import prisms.lang.EvaluationEnvironment;
import prisms.lang.EvaluationException;
import prisms.lang.EvaluationResult;
import prisms.lang.types.ParsedKeyword;

/** Represents a keyword */
public class KeywordEvaluator implements PrismsItemEvaluator<ParsedKeyword> {
	@Override
	public EvaluationResult evaluate(ParsedKeyword item, PrismsEvaluator evaluator, EvaluationEnvironment env, boolean asType,
		boolean withValues) throws EvaluationException {
		Class<?> type = TypeEvaluator.getClassFromName(item.getName(), env);
		if(type != null)
			return new prisms.lang.EvaluationResult(new prisms.lang.Type(type));
		throw new prisms.lang.EvaluationException("Syntax error on " + item.getName() + ": delete this token", item, item.getMatch().index);
	}
}
