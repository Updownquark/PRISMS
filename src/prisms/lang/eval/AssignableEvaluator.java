/* Assignable.java Created Nov 18, 2011 by Andrew Butler, PSL */
package prisms.lang.eval;

import prisms.lang.EvaluationEnvironment;
import prisms.lang.EvaluationException;
import prisms.lang.EvaluationResult;
import prisms.lang.types.Assignable;
import prisms.lang.types.ParsedAssignmentOperator;

/**
 * Marks a parsed item as being capable of being assigned to a value
 *
 * @param <T> The subtype of assignable that this evaluator can handle
 */
public interface AssignableEvaluator<T extends Assignable> extends PrismsItemEvaluator<T> {
	/**
	 * Retrieves the current value of the variable being assigned. This is used for assignment operators such as "+=" where the initial
	 * value is used to compute the value assigned.
	 *
	 * @param env The environment to get the value from
	 * @param assign The assignment operator that the assignment is for
	 * @return The current value of the variable being assigned
	 * @throws prisms.lang.EvaluationException If an error occurs getting the current value
	 */
	EvaluationResult getValue(T item, PrismsEvaluator eval, EvaluationEnvironment env, ParsedAssignmentOperator assign)
		throws EvaluationException;

	/**
	 * Assigns the value to the variable
	 *
	 * @param value The type and value to assign
	 * @param env The environment to assign the variable in
	 * @param assign The assignment operator that is assigning the variable
	 * @throws prisms.lang.EvaluationException If an error occurs assigning the value
	 */
	void assign(T item, EvaluationResult value, PrismsEvaluator eval, EvaluationEnvironment env, ParsedAssignmentOperator assign)
		throws EvaluationException;
}
