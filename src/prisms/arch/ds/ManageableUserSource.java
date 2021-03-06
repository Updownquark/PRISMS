/*
 * ManageableUserSource.java Created Jun 25, 2008 by Andrew Butler, PSL
 */
package prisms.arch.ds;

import prisms.arch.PrismsException;
import prisms.records.RecordsTransaction;

/**
 * An extension of {@link UserSource} that allows an application to configure users, applications,
 * permissions, etc.
 */
public interface ManageableUserSource extends UserSource
{
	/** Listens to changes in the set of users available to a user source */
	public static interface UserSetListener
	{
		/**
		 * Called when users are created or deleted
		 * 
		 * @param users The new set of users available to the user source
		 */
		void userSetChanged(User [] users);

		/**
		 * Called whan a user is modified
		 * 
		 * @param user The user that was modified
		 */
		void userChanged(User user);

		/**
		 * Called when a user is added or removed from a group or when a permission is added or
		 * removed from a group that the user belongs to
		 * 
		 * @param user The user whose authority may have changed
		 */
		void userAuthorityChanged(User user);

		/**
		 * Called when groups are created or deleted
		 * 
		 * @param app The application whose group set changed
		 * @param groups The new set of groups available to the user source
		 */
		void groupSetChanged(prisms.arch.PrismsApplication app, UserGroup [] groups);

		/**
		 * Called when a group is modified
		 * 
		 * @param group The group that was modified
		 */
		void groupChanged(UserGroup group);
	}

	/** @param listener The listener to listen for changes to this user source's user set */
	void addListener(UserSetListener listener);

	/**
	 * @param listener The listener to remove from listening for changes to this user source's user
	 *        set
	 */
	void removeListener(UserSetListener listener);

	/** @return The record keeper that keeps track of changes to this user source */
	prisms.records.DBRecordKeeper getRecordKeeper();

	/**
	 * Sets the constraints that determine whether a password is allowable within this PRISMS user
	 * source
	 * 
	 * @param constraints The constraints to set
	 * @throws PrismsException If an error occurs writing the data
	 */
	void setPasswordConstraints(PasswordConstraints constraints) throws PrismsException;

	/**
	 * Retrieves all user groups that apply to an application
	 * 
	 * @param app The application to get the groups of
	 * @return All groups in this user source for the given application
	 * @throws PrismsException If an error occurs getting the data
	 */
	UserGroup [] getGroups(prisms.arch.PrismsApplication app) throws PrismsException;

	/**
	 * Gets a user group by ID
	 * 
	 * @param id The ID of the group to get
	 * @return The group with the given ID
	 * @throws PrismsException If an error occurs getting the data
	 */
	UserGroup getGroup(long id) throws PrismsException;

	/**
	 * @return All users registered in this user source, including those that have been deleted or
	 *         those that are not local to this installation
	 * @throws PrismsException If an error occurs retrieving the data
	 */
	User [] getAllUsers() throws PrismsException;

	/**
	 * Creates a new user in the user source
	 * 
	 * @param name The name for the new user
	 * @param trans The transaction with which to record the data
	 * @return The new user
	 * @throws PrismsException If an error occurs accessing the data
	 */
	User createUser(String name, RecordsTransaction trans) throws PrismsException;

	/**
	 * Modifies a user. This method is a catch-all for any kind of legal modification to a user,
	 * including name, group membership, etc.
	 * 
	 * @param user The user to modify
	 * @param trans The transaction with which to record the data
	 * @throws PrismsException If an error occurs modifying the user
	 */
	void putUser(User user, RecordsTransaction trans) throws PrismsException;

	/**
	 * Deletes a user from this user source
	 * 
	 * @param user The user to delete
	 * @param trans The transaction with which to record the data
	 * @throws PrismsException If an error occurs accessing the data
	 */
	void deleteUser(User user, RecordsTransaction trans) throws PrismsException;

	/**
	 * Sets the password expiration time of a user
	 * 
	 * @param user The user to set the password expiration for
	 * @param time The password expiration time for the user
	 * @param trans The transaction with which to record the data
	 * @throws PrismsException If an error occurs accessing the data
	 */
	void setPasswordExpiration(User user, long time) throws PrismsException;

	/**
	 * Controls whether a user can access an application
	 * 
	 * @param user The user to determine access for
	 * @param app The application to determine access for
	 * @param accessible Whether the user should be able to access the application
	 * @param trans The transaction with which to record the data
	 * @throws PrismsException If an error occurs setting the data
	 */
	void setUserAccess(User user, prisms.arch.PrismsApplication app, boolean accessible,
		RecordsTransaction trans) throws PrismsException;

	/**
	 * Creates a new user group
	 * 
	 * @param app The application that the new group is for
	 * @param name The name for the new group
	 * @param trans The transaction with which to record the data
	 * @return The new group
	 * @throws PrismsException If an error occurs accessing the data
	 */
	UserGroup createGroup(prisms.arch.PrismsApplication app, String name, RecordsTransaction trans)
		throws PrismsException;

	/**
	 * Modifies a group. This method is a catch-all for any kind of legal modification to a user,
	 * including name, description, permissions, etc.
	 * 
	 * @param group The group to modify
	 * @param trans The transaction with which to record the data
	 * @throws PrismsException If an error occurs modifying the group
	 */
	void putGroup(UserGroup group, RecordsTransaction trans) throws PrismsException;

	/**
	 * Deletes a group
	 * 
	 * @param group The group to delete
	 * @param trans The transaction with which to record the data
	 * @throws PrismsException If an error occurs accessing the data
	 */
	void deleteGroup(UserGroup group, RecordsTransaction trans) throws PrismsException;
}
