package prisms.lang.eval;

import prisms.lang.EvaluationEnvironment;
import prisms.lang.EvaluationException;
import prisms.lang.EvaluationResult;
import prisms.lang.ParsedItem;
import prisms.lang.types.*;

/** Represents a block of statements */
public class StatementBlockEvaluator implements PrismsItemEvaluator<ParsedStatementBlock> {
	@Override
	public EvaluationResult evaluate(ParsedStatementBlock item, PrismsEvaluator evaluator, EvaluationEnvironment env, boolean asType,
		boolean withValues) throws EvaluationException {
		prisms.lang.EvaluationEnvironment scoped = env.scope(true);
		for(ParsedItem content : item.getContents()) {
			prisms.lang.EvaluationResult res = executeJavaStatement(content, evaluator, scoped, withValues);
			if(res != null)
				return res;
		}
		return null;
	}

	/**
	 * Executes a statement in a java context. Only statements that would be permitted as java statements are accepted. Others will throw an
	 * exception
	 *
	 * @param content The statement to execute
	 * @param evaluator The evaluator to evaluate with
	 * @param env The environment to execute the statement in
	 * @param withValues Whether to evaluate values or just validate the statement
	 * @return A result if the statement was a control statement, null otherwise
	 * @throws EvaluationException If the statement is not a valid java statement, or if the execution of the statement throws an exception
	 */
	public static EvaluationResult executeJavaStatement(ParsedItem content, PrismsEvaluator evaluator, EvaluationEnvironment env,
		boolean withValues) throws EvaluationException {
		if(content instanceof ParsedAssignmentOperator) {
		} else if(content instanceof ParsedDeclaration) {
		} else if(content instanceof ParsedMethod) {
			prisms.lang.types.ParsedMethod method = (prisms.lang.types.ParsedMethod) content;
			if(!method.isMethod())
				throw new EvaluationException("Content expressions must be declarations, assignments or method calls", content.getParent(),
					content.getMatch().index);
		} else if(content instanceof ParsedConstructor) {
		} else if(content instanceof ParsedLoop) {
		} else if(content instanceof ParsedEnhancedForLoop) {
		} else if(content instanceof ParsedIfStatement) {
		} else if(content instanceof ParsedKeyword) {
			String word = ((ParsedKeyword) content).getName();
			if(word.equals("continue"))
				return new EvaluationResult(prisms.lang.EvaluationResult.ControlType.CONTINUE, null, content);
			if(word.equals("break"))
				return new EvaluationResult(prisms.lang.EvaluationResult.ControlType.BREAK, null, content);
		} else if(content instanceof ParsedReturn) {
			if(withValues)
				return evaluator.evaluate(content, env, false, withValues);
		} else if(content instanceof ParsedThrow) {
		} else
			throw new EvaluationException("Content expressions must be declarations, assignments or method calls", content.getParent(),
				content.getMatch().index);
		evaluator.evaluate(content, env, false, withValues);
		return null;
	}
}
