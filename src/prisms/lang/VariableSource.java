package prisms.lang;

/** Represents a source of named, typed data */
public interface VariableSource {
	/** @return All variables that are recognized in this environment */
	Variable [] getDeclaredVariables();

	/**
	 * @param name The name of the variable to get the declaration of
	 * @return All metadata associated with a variable in this environment, or null if the variable has not been declared or has been
	 *         dropped from this environment
	 */
	Variable getDeclaredVariable(String name);
}
