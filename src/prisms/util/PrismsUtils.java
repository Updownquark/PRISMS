/*
 * PrismsUtils.java Created Sep 11, 2008 by Andrew Butler, PSL
 */
package prisms.util;

import org.json.simple.JSONObject;
import org.qommons.ProgramTracker;
import org.qommons.QommonsUtils;

import prisms.arch.ds.PasswordConstraints;

/** A general utility class with methods useful in the PRISMS architecture */
public class PrismsUtils
{
	/**
	 * Allows the
	 * {@link PrismsUtils#navigate(Object, Object, prisms.util.PrismsUtils.TreeInterpreter)} method
	 * to navigate the trees and match nodes
	 * 
	 * @param <T1> The type of nodes the tree contains
	 * @param <T2> The type of data matching the node to search for
	 */
	public static interface TreeInterpreter<T1, T2>
	{
		/**
		 * @param parent The parent node to get children for
		 * @return The children of the given parent node in the tree
		 */
		T1 [] getChildren(T1 parent);

		/**
		 * @param child The child data
		 * @return The parent data of the given child
		 */
		T2 getParent(T2 child);

		/**
		 * @param node1 The tree node to match
		 * @param node2 The data node to match
		 * @return Whether the data node matches the tree node
		 */
		boolean nodesMatch(T1 node1, T2 node2);
	}

	private static final String randomEvent = Long
		.toHexString((long) (Math.random() * Long.MAX_VALUE));

	private static final prisms.arch.event.PrismsProperty<?> randomProperty = prisms.arch.event.PrismsProperty
		.create(randomEvent, PrismsUtils.class);

	private static boolean isJava6;

	static
	{
		try
		{
			isJava6 = java.sql.Connection.class.getMethod("isValid", Integer.TYPE) != null;
		} catch(Exception e)
		{
			isJava6 = false;
		}
	}

	private PrismsUtils()
	{
	}

	/** @return Whether the JVM being run has JDK-6 classes. */
	public static boolean isJava6()
	{
		return isJava6;
	}

	/**
	 * Decodes a string that was encoded using __XENC
	 * 
	 * @param str The string to decode
	 * @return The decoded string
	 */
	public static String decodeSafe(String str)
	{
		if(str == null)
			return null;
		str = QommonsUtils.replaceAll(str, "__XENC", "\\u");
		str = org.qommons.QommonsUtils.decodeUnicode(str);
		return str;
	}

	/**
	 * A utility method for tracking with. Just makes code simpler.
	 * 
	 * @param trans The PRISMS transaction to get the tracking data from
	 * @param task The name of the task to start
	 * @return The tracking node created by the tracker--may be null
	 */
	public static ProgramTracker.TrackNode track(prisms.arch.PrismsTransaction trans, String task)
	{
		if(trans == null)
			return null;
		return trans.getTracker().start(task);
	}

	/**
	 * Like {@link #track(prisms.arch.PrismsTransaction, String)}, but this utility makes a more
	 * intelligent string of with an unknown type than just calling toString() blindly. Unless the
	 * object implements toString(), the object's class name is returned
	 * 
	 * @param trans The PRISMS transaction to get the tracking data from
	 * @param taskObj The Object representing the task to start
	 * @return The tracking node created by the tracker--may be null
	 */
	public static ProgramTracker.TrackNode track(prisms.arch.PrismsTransaction trans, Object taskObj)
	{
		return track(trans, taskToString(taskObj));
	}

	/**
	 * A more intelligent toString() for task objects--returns the object's class name unless the
	 * object implements toString()
	 * 
	 * @param taskObj The object to string
	 * @return The string representing the object or its type
	 */
	public static String taskToString(Object taskObj)
	{
		if(taskObj == null)
			return "null";
		String className = taskObj.getClass().getName();
		String str = taskObj.toString();
		if(str.startsWith(className))
			return className;
		else
			return taskObj.toString();
	}

	/**
	 * Ends a task, typically one started with {@link #track(prisms.arch.PrismsTransaction, String)}
	 * 
	 * @param trans The PRISMS transaction to get the tracking data from
	 * @param track The track node created by the tracker. May be null.
	 */
	public static void end(prisms.arch.PrismsTransaction trans, ProgramTracker.TrackNode track)
	{
		if(track == null || trans == null)
			return;
		trans.getTracker().end(track);
	}

	/**
	 * Searches for a particular node in a tree structure
	 * 
	 * @param <T1> The type of nodes the tree contains
	 * @param <T2> The type of data matching the node to search for
	 * @param root The root of the tree or subtree to search in
	 * @param node The data to search the tree for
	 * @param interp The tree interpreter allowing this method to match nodes and navigate the tree
	 * @return The tree node found under <code>root</code> matching <code>node</code>
	 */
	public static <T1, T2> T1 navigate(T1 root, T2 node, PrismsUtils.TreeInterpreter<T1, T2> interp)
	{
		java.util.ArrayList<T2> path = new java.util.ArrayList<T2>();
		while(node != null && !interp.nodesMatch(root, node))
		{
			path.add(node);
			node = interp.getParent(node);
		}
		if(node == null) // node is not found beneath root
			return null;
		path.add(node);
		java.util.Collections.reverse(path);
		return navigate(root, path, interp, 1);
	}

	private static <T1, T2> T1 navigate(T1 root, java.util.List<T2> path,
		TreeInterpreter<T1, T2> interp, int pathIdx)
	{
		if(pathIdx == path.size())
			return root;
		T1 [] children = interp.getChildren(root);
		for(T1 child : children)
			if(interp.nodesMatch(child, path.get(pathIdx)))
				return navigate(child, path, interp, pathIdx + 1);
		return root; // Return the lowest subtree of root that is an ancestor of the target node
	}

	/**
	 * Searches for a node in a tree in a much more brute force way than
	 * {@link #navigate(Object, Object, TreeInterpreter)}. The getParent method in the tree
	 * interpreter passed to this method is never called, but rather the utility searches the entire
	 * tree depth-first for the given node.
	 * 
	 * @param <T1> The type of node in the tree to search
	 * @param <T2> The type of the node to search for
	 * @param root The root of the tree or subtree to search
	 * @param node The node to search for
	 * @param interp The interpreter for the tree and the search node
	 * @return The node in <code>root</code> matching <code>node</code> according to
	 *         <code>interp</code>
	 */
	public static <T1, T2> T1 search(T1 root, T2 node, PrismsUtils.TreeInterpreter<T1, T2> interp)
	{
		if(interp.nodesMatch(root, node))
			return root;
		for(T1 child : interp.getChildren(root))
		{
			T1 ret = search(child, node, interp);
			if(ret != null)
				return ret;
		}
		return null;
	}

	/**
	 * Prints a representation of the tree in a way that allows the developer to view the hierarchy
	 * 
	 * @param <T1> The type of node in the tree
	 * @param <T2> Arbitrary--only used to avoid creating a new tree interpreter interface
	 * @param root The root of the tree to print
	 * @param interp The interpreter to get the children of the root
	 * @return A string representing the tree
	 */
	public static <T1, T2> String printTree(T1 root, TreeInterpreter<T1, T2> interp)
	{
		return printTree(root, interp, 0);
	}

	private static <T1, T2> String printTree(T1 root, TreeInterpreter<T1, T2> interp, int indent)
	{
		StringBuilder ret = new StringBuilder();
		for(int i = 0; i < indent; i++)
			ret.append("    ");
		ret.append(root);
		for(T1 child : interp.getChildren(root))
		{
			ret.append('\n');
			ret.append(printTree(child, interp, indent + 1));
		}
		return ret.toString();
	}

	/**
	 * @param session The session to check
	 * @param eventName The event name to check
	 * @return Whether the event has any listeners in the session
	 */
	public static boolean hasEventListeners(prisms.arch.PrismsSession session, String eventName)
	{
		prisms.arch.event.PrismsEventListener[] listeners = session.getEventListeners(eventName);
		prisms.arch.event.PrismsEventListener[] generalListeners = session
			.getEventListeners(randomEvent);
		return listeners.length > generalListeners.length;
	}

	/**
	 * @param session The session to check
	 * @param property The property to check
	 * @return Whether the property has any listeners in the session
	 */
	public static boolean hasPropertyListeners(prisms.arch.PrismsSession session,
		prisms.arch.event.PrismsProperty<?> property)
	{
		prisms.arch.event.PrismsPCL<?> [] listeners = session.getPropertyChangeListeners(property);
		prisms.arch.event.PrismsPCL<?> [] generalListeners = session
			.getPropertyChangeListeners(randomProperty);
		return listeners.length > generalListeners.length;
	}

	/**
	 * Serializes password constraints to JSON
	 * 
	 * @param constraints The password constraints to serialize
	 * @return The JSON-serialize password constraints
	 */
	public static JSONObject serializeConstraints(PasswordConstraints constraints)
	{
		JSONObject ret = new JSONObject();
		ret.put("minLength", Integer.valueOf(constraints.getMinCharacterLength()));
		ret.put("minUpperCase", Integer.valueOf(constraints.getMinUpperCase()));
		ret.put("minLowerCase", Integer.valueOf(constraints.getMinLowerCase()));
		ret.put("minDigits", Integer.valueOf(constraints.getMinDigits()));
		ret.put("minSpecialChars", Integer.valueOf(constraints.getMinSpecialChars()));
		ret.put("maxDuration", Long.valueOf(constraints.getMaxPasswordDuration()));
		ret.put("numUnique", Integer.valueOf(constraints.getNumPreviousUnique()));
		ret.put("minChangeInterval", Long.valueOf(constraints.getMinPasswordChangeInterval()));
		return ret;
	}

	/**
	 * Deserializes password constraints from JSON
	 * 
	 * @param constraints The JSON-serialized password constraints
	 * @return The deserialized password constraints
	 */
	public static PasswordConstraints deserializeConstraints(JSONObject constraints)
	{
		PasswordConstraints ret = new PasswordConstraints();
		ret.setMinCharacterLength(((Number) constraints.get("minLength")).intValue());
		ret.setMinUpperCase(((Number) constraints.get("minUpperCase")).intValue());
		ret.setMinLowerCase(((Number) constraints.get("minLowerCase")).intValue());
		ret.setMinDigits(((Number) constraints.get("minDigits")).intValue());
		ret.setMinSpecialChars(((Number) constraints.get("minSpecialChars")).intValue());
		ret.setMaxPasswordDuration(((Number) constraints.get("maxDuration")).longValue());
		ret.setNumPreviousUnique(((Number) constraints.get("numUnique")).intValue());
		ret.setMinPasswordChangeInterval(((Number) constraints.get("minChangeInterval"))
			.longValue());
		return ret;
	}

	/**
	 * Self-test method
	 * 
	 * @param args Command line args, ignored
	 */
	public static void main(String [] args)
	{
		String str = "blahsomethingblahblahsomethingsomethingblahsomething";
		System.out.println("Original=" + str);
		str = QommonsUtils.replaceAll(str, "something", "blah");
		System.out.println("Replaced=" + str);
	}
}
