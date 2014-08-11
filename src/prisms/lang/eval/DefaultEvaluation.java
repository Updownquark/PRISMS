package prisms.lang.eval;

import prisms.lang.types.*;

/** Initializes a {@link PrismsEvaluator} with evaluators recognized by default */
public class DefaultEvaluation {
	public static void initializeBasics(PrismsEvaluator evaluator) {
		evaluator.addEvaluator(NoOpItem.class, new NoOpEvaluator());
		evaluator.addEvaluator(ParsedLiteral.class, new LiteralEvaluator());
		evaluator.addEvaluator(ParsedType.class, new TypeEvaluator());
		evaluator.addEvaluator(ParsedCast.class, new CastEvaluator());
		evaluator.addEvaluator(ParsedInstanceofOp.class, new InstanceofEvaluator());
		evaluator.addEvaluator(ParsedConditional.class, new ConditionalEvaluator());
		evaluator.addEvaluator(ParsedUnaryOp.class, new UnaryOpEvaluator());
		evaluator.addEvaluator(ParsedBinaryOp.class, new BinaryOpEvaluator());
		evaluator.addEvaluator(ParsedArrayIndex.class, new ArrayIndexEvaluator());
		evaluator.addEvaluator(ParsedIdentifier.class, new IdentifierEvaluator());
		evaluator.addEvaluator(ParsedKeyword.class, new KeywordEvaluator());
		evaluator.addEvaluator(ParsedMethod.class, new MethodEvaluator());
		evaluator.addEvaluator(ParsedParenthetic.class, new ParentheticEvaluator());
	}

	public static void initializeConstructors(PrismsEvaluator evaluator) {
		evaluator.addEvaluator(ParsedConstructor.class, new ConstructorEvaluator());
		evaluator.addEvaluator(ParsedArrayInitializer.class, new ArrayInitializerEvaluator());
	}

	public static void initializeControl(PrismsEvaluator evaluator) {
		evaluator.addEvaluator(ParsedIfStatement.class, new IfStatementEvaluator());
		evaluator.addEvaluator(ParsedStatementBlock.class, new StatementBlockEvaluator());
		evaluator.addEvaluator(ParsedEnhancedForLoop.class, new EnhancedForLoopEvaluator());
		evaluator.addEvaluator(ParsedLoop.class, new LoopEvaluator());
		evaluator.addEvaluator(ParsedTryCatchFinally.class, new TryCatchFinallyEvaluator());
		evaluator.addEvaluator(ParsedThrow.class, new ThrowEvaluator());
		evaluator.addEvaluator(ParsedReturn.class, new ReturnEvaluator());
	}

	public static void initializeDeclarations(PrismsEvaluator evaluator) {
		evaluator.addEvaluator(ParsedDeclaration.class, new DeclarationEvaluator());
		evaluator.addEvaluator(ParsedFunctionDeclaration.class, new FunctionDeclarationEvaluator());
		evaluator.addEvaluator(ParsedDrop.class, new DropEvaluator());
	}

	public static void initializeAssignment(PrismsEvaluator evaluator) {
		evaluator.addEvaluator(ParsedAssignmentOperator.class, new AssignmentOperatorEvaluator());
	}

	/** @param evaluator The evaluator to initialize */
	public static void initializeDefaults(PrismsEvaluator evaluator) {
		initializeBasics(evaluator);
		initializeConstructors(evaluator);
		initializeControl(evaluator);
		initializeDeclarations(evaluator);
		initializeAssignment(evaluator);
		evaluator.addEvaluator(ParsedSyncBlock.class, new SyncBlockEvaluator());
		evaluator.addEvaluator(ParsedImport.class, new ImportEvaluator());
		evaluator.addEvaluator(ParsedPreviousAnswer.class, new PreviousAnswerEvaluator());
	}
}
