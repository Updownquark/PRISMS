package prisms.lang;

/** Represents a variable declared within an evaluation environment */
public interface Variable {
	/** @return The name of the variable */
	String getName();

	/** @return The type of the variable */
	Type getType();

	/** @return Whether this variable is final or not */
	boolean isFinal();

	/** @return Whether this variable has been initialized with a value or not */
	boolean isInitialized();

	/** @return The value currently assigned to the variable */
	Object getValue();
}