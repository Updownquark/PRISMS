/**
 * PrismsEvent.java Created Aug 1, 2007 by Andrew Butler, PSL
 */
package prisms.arch.event;

/** A PrismsEvent may be fired using a PrismsSession to communicate between plugins */
public class PrismsEvent
{
	/** The name of this event */
	public final String name;

	private final java.util.HashMap<String, Object> theProperties;

	/**
	 * Creates a PrismsEvent with no properties. This is the same as calling either of the other
	 * constructors using a null properties argument. The reason for this constructor is that
	 * <code>PrismsEvent("name", null)</code> is ambiguous.
	 * 
	 * @param aName The name for the event
	 */
	public PrismsEvent(String aName)
	{
		this(aName, (Object []) null);
	}

	/**
	 * Creates a PrismsEvent using a name and a map of properties
	 * 
	 * @param aName The name for the event
	 * @param evtProps The map of properties for this event
	 */
	public PrismsEvent(String aName, java.util.Map<String, Object> evtProps)
	{
		name = aName.intern();
		theProperties = new java.util.HashMap<String, Object>(evtProps == null ? 4
			: evtProps.size() + 2);
		if(evtProps != null)
			theProperties.putAll(evtProps);
	}

	/**
	 * Creates a PrismsEvent using a name and an alternating array of string, object, string,
	 * object... to initialize the properties
	 * 
	 * @param aName The name for the event
	 * @param evtProps An array whose elements alternate string, object, string, object... This sets
	 *        the properties of the event in a way that is much easier to write inline
	 */
	public PrismsEvent(String aName, Object... evtProps)
	{
		name = aName;
		if(evtProps == null || evtProps.length == 0)
		{
			theProperties = new java.util.HashMap<String, Object>(4);
			return;
		}
		else
			theProperties = new java.util.HashMap<String, Object>(evtProps.length / 2 + 2);
		if(evtProps.length % 2 != 0)
			throw new IllegalArgumentException("An even number of arguments are required, not "
				+ evtProps.length);
		for(int i = 0; i < evtProps.length; i += 2)
		{
			if(!(evtProps[i] instanceof String))
				throw new IllegalArgumentException("Every other object must be a string, not "
					+ evtProps[i]);
			theProperties.put((String) evtProps[i], evtProps[i + 1]);
		}
	}

	/**
	 * @param propName The name of the property to get
	 * @return The value of the property registered to this event with the given name
	 */
	public Object getProperty(String propName)
	{
		return theProperties.get(propName);
	}

	/**
	 * @param propName The name of the property to set
	 * @param propValue The value of the property to set for the given name in this event
	 */
	public void setProperty(String propName, Object propValue)
	{
		theProperties.put(propName, propValue);
	}

	/** @return All properties set in this event in the form [name, value, name, value...] */
	public Object [] getPropertyList()
	{
		String [] keys = theProperties.keySet().toArray(new String [0]);
		int i = keys.length;
		for(String key : keys)
			if(key == null)
				i--;
		Object [] ret = new Object [i * 2];
		i = 0;
		for(String key : keys)
		{
			if(key == null)
				continue;
			ret[i] = key;
			ret[i + 1] = theProperties.get(key);
			i += 2;
		}
		return ret;
	}

	@Override
	public String toString()
	{
		String ret = "PrismsEvent " + name;
		if(theProperties != null && !theProperties.isEmpty())
			ret += ":" + theProperties;
		return ret;
	}
}
