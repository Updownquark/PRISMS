/**
 * PluginApplication.java Created Aug 2, 2007 by Andrew Butler, PSL
 */
package prisms.arch;

import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.dom4j.Element;

import prisms.arch.event.*;

/**
 * PluginApplication represents a type of application from which sessions may be instantiated that
 * interact with clients. A PluginAppSession is to its PluginApplication what an object is to its
 * class.
 */
public class PrismsApplication
{
	private static Logger log = Logger.getLogger(PrismsApplication.class);

	/** A property in a PRISMS event that marks the event as having been fired globally */
	public static final String GLOBALIZED_EVENT_PROPERTY = "globalEvent";

	/** A task to be run on a session */
	public static interface SessionTask
	{
		/**
		 * Runs the task
		 * 
		 * @param session The session to run the task on
		 */
		void run(PrismsSession session);
	}

	/**
	 * Represents a lock on an application. No sessions or services may access an application while
	 * it is locked.
	 * 
	 * @see PrismsApplication#getApplicationLock()
	 */
	public static class ApplicationLock
	{
		private String theMessage;

		private int theScale;

		private int theProgress;

		private PrismsSession theSession;

		ApplicationLock(String message, int scale, int progress, PrismsSession session)
		{
			theMessage = message;
			theScale = scale;
			theProgress = progress;
			theSession = session;
		}

		/**
		 * @return The message that is displayed to the user when a session attempts to access the
		 *         application while it is locked
		 */
		public String getMessage()
		{
			return theMessage;
		}

		/**
		 * @return The scale of the task that is blocking the application. If 0, the task's length
		 *         is undetermined; otherwise the user may reasonably expect the application to be
		 *         unlocked shortly after {@link #getProgress()} reaches this value unless told
		 *         otherwise by {@link #getMessage()}.
		 */
		public int getScale()
		{
			return theScale;
		}

		/**
		 * @return The progress of the task that is blocking the application. If the task's scale is
		 *         non-zero, the user may reasonably expect the application to be unlocked shortly
		 *         after {@link #getProgress()} reaches this value unless told otherwise by
		 *         {@link #getMessage()}.
		 */
		public int getProgress()
		{
			return theProgress;
		}

		/**
		 * @return The session that is responsible for locking the application (may be null)
		 */
		public PrismsSession getLockingSession()
		{
			return theSession;
		}

		void set(String message, int scale, int progress, PrismsSession session)
		{
			theMessage = message;
			theScale = scale;
			theProgress = progress;
			theSession = session;
		}
	}

	private static class EventListenerType
	{
		final String theEventName;

		final Class<? extends PrismsEventListener> theListenerType;

		final Element theConfigEl;

		EventListenerType(String eventName, Class<? extends PrismsEventListener> listenerType,
			Element configEl)
		{
			theEventName = eventName;
			theListenerType = listenerType;
			theConfigEl = configEl;
		}
	}

	private static class MonitorType
	{
		final Class<? extends SessionMonitor> theMonitorType;

		final Element theConfigEl;

		MonitorType(Class<? extends SessionMonitor> monitorType, Element configEl)
		{
			theMonitorType = monitorType;
			theConfigEl = configEl;
		}
	}

	private PrismsEnv theEnv;

	private String theName;

	private String theDescription;

	private int [] theVersion;

	private long theModifiedDate;

	private Object theConfigurator;

	private boolean isConfigured;

	private final java.util.LinkedHashMap<String, ClientConfig> theClientConfigs;

	private final java.util.LinkedHashMap<String, Permission> thePermissions;

	private final java.util.concurrent.ConcurrentLinkedQueue<PrismsSession> theSessions;

	private final ArrayList<PropertyManager<?>> theManagers;

	private final ArrayList<EventListenerType> theELTypes;

	private final ArrayList<MonitorType> theMonitorTypes;

	private final java.util.concurrent.ConcurrentHashMap<String, PrismsEventListener []> theGlobalListeners;

	private final ArrayList<ScheduledTask> theOneTimeTasks;

	private final ArrayList<ScheduledTask> theRecurringTasks;

	private final ArrayList<Runnable> theDestroyTasks;

	private final java.util.concurrent.ConcurrentHashMap<PrismsProperty<?>, Object> thePropertyLocks;

	private final java.util.concurrent.ConcurrentHashMap<PrismsProperty<?>, PrismsApplication> thePropertyStack;

	@SuppressWarnings("rawtypes")
	private final ListenerManager<PrismsPCL> thePCLs;

	private Worker theWorker;

	private ApplicationLock theAppLock;

	/**
	 * Creates a PluginApplication
	 * 
	 * @param env The environment that this application will be used in
	 * @param name The name for this application
	 * @param descrip The description for this application
	 * @param version The version information for this application
	 * @param modifiedDate The last time this application was modified
	 * @param configurator The object that is responsible for configuring this application
	 */
	@SuppressWarnings("rawtypes")
	public PrismsApplication(PrismsEnv env, String name, String descrip, int [] version,
		long modifiedDate, Object configurator)
	{
		theEnv = env;
		theName = name;
		theDescription = descrip;
		theVersion = version;
		theModifiedDate = modifiedDate;
		theConfigurator = configurator;
		theClientConfigs = new java.util.LinkedHashMap<String, ClientConfig>();
		thePermissions = new java.util.LinkedHashMap<String, Permission>();
		theSessions = new java.util.concurrent.ConcurrentLinkedQueue<PrismsSession>();
		theManagers = new ArrayList<PropertyManager<?>>();
		theELTypes = new java.util.ArrayList<EventListenerType>();
		theMonitorTypes = new java.util.ArrayList<MonitorType>();
		theGlobalListeners = new java.util.concurrent.ConcurrentHashMap<String, PrismsEventListener []>();
		theOneTimeTasks = new ArrayList<ScheduledTask>();
		theRecurringTasks = new ArrayList<ScheduledTask>();
		theDestroyTasks = new ArrayList<Runnable>();
		thePCLs = new ListenerManager<PrismsPCL>(PrismsPCL.class);
		thePropertyLocks = new java.util.concurrent.ConcurrentHashMap<PrismsProperty<?>, Object>();
		thePropertyStack = new java.util.concurrent.ConcurrentHashMap<PrismsProperty<?>, PrismsApplication>();
	}

	/** @return The environment that this application is used in */
	public PrismsEnv getEnvironment()
	{
		return theEnv;
	}

	/** @return Whether this application has been fully configured */
	public boolean isConfigured()
	{
		return isConfigured;
	}

	/**
	 * Marks this application as configured
	 * 
	 * @param config The object that is responsible for configuring this application. This must be
	 *        the same object as the configurator that was passed to the constructor.
	 */
	public void setConfigured(Object config)
	{
		if(config == theConfigurator)
		{
			theConfigurator = null;
			isConfigured = true;
		}
		else
			throw new IllegalArgumentException("Configurator is not correct");
	}

	/** @return The name of this application */
	public String getName()
	{
		return theName;
	}

	/** @return The description of this application */
	public String getDescription()
	{
		return theDescription;
	}

	/** @return This application's version */
	public int [] getVersion()
	{
		return theVersion;
	}

	/** @return This application's version as a string */
	public String getVersionString()
	{
		if(theVersion == null)
			return "Unknown";
		StringBuilder ret = new StringBuilder();
		for(int v = 0; v < theVersion.length; v++)
		{
			ret.append(theVersion[v]);
			if(v < theVersion.length - 1)
				ret.append('.');
		}
		return ret.toString();
	}

	/**
	 * @return The time when this application was last developed
	 */
	public long getModifiedDate()
	{
		return theModifiedDate;
	}

	/**
	 * @return The time when this application was last developed, as a string
	 */
	public String getModifiedDateString()
	{
		if(theModifiedDate <= 0)
			return "Unknown";
		else
		{
			java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("ddMMMyyyy");
			sdf.setTimeZone(java.util.TimeZone.getTimeZone("GMT"));
			return sdf.format(new java.util.Date(theModifiedDate));
		}
	}

	/** @return All client configurations that provide access to this application's data and logic */
	public ClientConfig [] getClients()
	{
		return theClientConfigs.values().toArray(new ClientConfig [theClientConfigs.size()]);
	}

	/**
	 * @param name The name of the client configuration to get
	 * @return This application's client configuration with the given name, or null if no such
	 *         config exists
	 */
	public ClientConfig getClient(String name)
	{
		return theClientConfigs.get(name);
	}

	/**
	 * Adds a client to this application. This will throw an exception if this application has
	 * already been configured
	 * 
	 * @param client A new client for this application
	 */
	public void addClientConfig(ClientConfig client)
	{
		if(isConfigured())
			throw new IllegalStateException("Cannot add a client config to an application that has"
				+ " been completely configured");
		theClientConfigs.put(client.getName(), client);
	}

	/** @return All permissions that are mapped to capabilities in this application */
	public Permission [] getPermissions()
	{
		return thePermissions.values().toArray(new prisms.arch.Permission [thePermissions.size()]);
	}

	/**
	 * @param name The name of the permission to get
	 * @return This application's permission with the given name, or null if no such permission
	 *         exists
	 */
	public Permission getPermission(String name)
	{
		return thePermissions.get(name);
	}

	/**
	 * Adds a permission to this application. This will throw an exception if this application has
	 * already been configured.
	 * 
	 * @param permission The permission to add to this application
	 */
	public void addPermission(Permission permission)
	{
		if(isConfigured())
			throw new IllegalStateException("Cannot add a permission to an application that has"
				+ " been completely configured");
		thePermissions.put(permission.getName(), permission);
	}

	/**
	 * Tells this application to manage a property, ensuring that all sessions in this application
	 * always have access to the appropriate value
	 * 
	 * @param <T> The type of the manager to add
	 * @param manager The manager for a property
	 */
	public <T> void addManager(PropertyManager<T> manager)
	{
		synchronized(theManagers)
		{
			theManagers.add(manager);
		}
		for(PrismsSession s : getSessions())
			s.addPropertyChangeListener(manager.getProperty(), manager);
	}

	/**
	 * @return All property managers registered with this application
	 */
	public PropertyManager<?> [] getManagers()
	{
		return theManagers.toArray(new PropertyManager [theManagers.size()]);
	}

	/**
	 * @param <T> The type of the manager to add
	 * @param property The property to get the managers for
	 * @return All PropertyManagers registered to manage the value of the given property
	 */
	public <T> PropertyManager<T> [] getManagers(PrismsProperty<T> property)
	{
		PropertyManager<T> [] ret = new PropertyManager [0];
		for(PropertyManager<?> pm : theManagers)
			if(pm.getProperty().equals(property))
				ret = prisms.util.ArrayUtils.add(ret, (PropertyManager<T>) pm);
		return ret;
	}

	/**
	 * Adds a template for an event listener, which will be added to each session as it is created
	 * 
	 * @param eventName The name of the event to listener
	 * @param listenerType The class of the listener to create for each session
	 * @param configEl The element used to configure new instances of the listener type
	 */
	public void addEventListenerType(String eventName,
		Class<? extends PrismsEventListener> listenerType, Element configEl)
	{
		theELTypes.add(new EventListenerType(eventName, listenerType, configEl));
	}

	/**
	 * Adds a template for a session monitor, which will be added to each session as it is created
	 * 
	 * @param monitorType The class of the monitor to create for each session
	 * @param configEl The configuration element to configure the monitor
	 */
	public void addMonitorType(Class<? extends SessionMonitor> monitorType,
		org.dom4j.Element configEl)
	{
		theMonitorTypes.add(new MonitorType(monitorType, configEl));
	}

	/**
	 * Adds an event listener to listen to an event whatever session it occurs in. The listener will
	 * receive an event as many times as there are sessions when the event fires each time the event
	 * fires.
	 * 
	 * @param eventName The name of the event to listen to, or null to listen to every event
	 * @param listener The listener to listen for the event
	 */
	public void addGlobalEventListener(String eventName, PrismsEventListener listener)
	{
		PrismsEventListener [] listeners = theGlobalListeners.get(eventName);
		if(listeners == null)
			listeners = new PrismsEventListener [] {listener};
		else
			listeners = prisms.util.ArrayUtils.add(listeners, listener);
		theGlobalListeners.put(eventName, listeners);
		for(PrismsSession session : theSessions)
		{
			if(eventName == null)
				session.addEventListener(listener);
			else
				session.addEventListener(eventName, listener);
		}
	}

	/**
	 * Removes an event listener that has been added to listen to events globally
	 * 
	 * @param eventName The name of the event that the listener is registered to
	 * @param listener The listener to remove
	 */
	public void removeGlobalEventListener(String eventName, PrismsEventListener listener)
	{
		PrismsEventListener [] listeners = theGlobalListeners.get(eventName);
		if(listeners == null)
			return;
		else if(listeners.length == 1)
			listeners = null;
		else
			listeners = prisms.util.ArrayUtils.remove(listeners, listener);
		theGlobalListeners.put(eventName, listeners);
		for(PrismsSession session : theSessions)
		{
			if(eventName == null)
				session.removeEventListener(listener);
			else
				session.removeEventListener(eventName, listener);
		}
	}

	/**
	 * Searches this application's property managers for the application value of a property. This
	 * may be called from application-level objects such as property managers and serializers. This
	 * should <b>NEVER</b> be called from session-level objects such as plugin instances. These
	 * should rely on the session-specific values.
	 * 
	 * @param <T> The type of property to get
	 * @param propName The name of the property to get the application value of
	 * @return The non-null value of the property as determined by this application's property
	 *         managers; or null if no managers exist or if the property's value is not
	 *         application-governed.
	 */
	public <T> T getGlobalProperty(PrismsProperty<T> propName)
	{
		T ret = null;
		for(PropertyManager<?> manager : theManagers)
		{
			if(manager.getProperty() != null && manager.getProperty().equals(propName))
			{
				if(ret == null)
					ret = ((PropertyManager<T>) manager).getApplicationValue(this);
				else
				{
					T temp = ((PropertyManager<T>) manager).getApplicationValue(this);
					if(temp != null && !ret.equals(temp))
						throw new IllegalStateException("Managers have two different values for"
							+ " property " + propName);
				}
			}
		}
		return ret;
	}

	private <T> PrismsPCE<T> createPCE(PrismsProperty<T> prop, T value, Object... eventProps)
	{
		// Generics *can* be defeated--check the type here
		if(value != null && !prop.getType().isInstance(value))
			throw new IllegalArgumentException("Cannot set an instance of "
				+ value.getClass().getName() + " for property " + prop + ", type "
				+ prop.getType().getName());
		if(eventProps.length % 2 != 0)
			throw new IllegalArgumentException("Event properties for property change event must be"
				+ " in the form of name, value, name, value, etc.--" + eventProps.length
				+ " arguments illegal");
		PrismsPCE<T> propEvt = new PrismsPCE<T>(this, null, prop, null, value);
		for(int i = 0; i < eventProps.length; i += 2)
		{
			if(!(eventProps[i] instanceof String))
				throw new IllegalArgumentException("Event properties for property change event"
					+ " must be in the form of name, value, name, value, etc.--eventProps[" + i
					+ "] is not a string");
			propEvt.set((String) eventProps[i], eventProps[i + 1]);
		}
		return propEvt;
	}

	/**
	 * Sets the application value of a property
	 * 
	 * @param <T> The type of the property to set
	 * @param prop The property to set the value of
	 * @param value The value of the property to set
	 * @param eventProps Event properties for the property change event that is fired. Must be in
	 *        the form of name, value, name, value, where name is a string.
	 */
	public <T> void setGlobalProperty(PrismsProperty<T> prop, T value, Object... eventProps)
	{
		PrismsPCE<T> propEvt = createPCE(prop, value, eventProps);
		PrismsPCL<T> [] listeners = getGlobalPropertyChangeListeners(prop);
		PropertyManager<T> manager = null;
		synchronized(getPropertyLock(prop))
		{
			for(PropertyManager<?> mgr : theManagers)
			{
				if(mgr != manager && mgr.getProperty().equals(prop))
				{
					PropertyManager<T> propMgr = (PropertyManager<T>) mgr;
					if(manager == null && propMgr.getApplicationValue(this) != null)
						manager = propMgr;
					propMgr.propertyChange(propEvt);
				}
			}
			value = getGlobalProperty(prop);
			thePropertyStack.put(prop, this);
			try
			{
				for(PrismsPCL<T> l : listeners)
				{
					l.propertyChange(propEvt);
					/* If this property is changed as a result of the above PCL, stop this notification */
					if(!thePropertyStack.containsKey(prop))
						break;
				}
				for(PrismsSession session : getSessions())
				{
					T sessionValue = session.getProperty(prop);
					if(manager == null && !prisms.util.ArrayUtils.equals(sessionValue, value))
						session.setProperty(prop, sessionValue, eventProps);
					else if(manager != null
						&& !manager.isValueCorrect(session, session.getProperty(prop)))
						session.setProperty(prop, manager.getCorrectValue(session), eventProps);
				}
			} finally
			{
				thePropertyStack.remove(prop);
			}
		}
	}

	// /**
	// * Signals that data for a globally managed property is changed. This is called when the data
	// * available to the application is changed, not just that available to a particular session.
	// * This method does not change the data in a session--it merely fires the appropriate property
	// * change listeners.
	// *
	// * @param <T> The type of property that changed
	// * @param prop The property whose value is changed
	// * @param manager The manager of the changed property
	// * @param value The new global value of the property
	// */
	// public <T> void fireGlobalPropertyChange(PrismsProperty<T> prop, PropertyManager<T> manager,
	// T value, Object... eventProps)
	// {
	// PrismsPCE<T> propEvt = createPCE(prop, value, eventProps);
	// PrismsPCL<T> [] listeners = getGlobalPropertyChangeListeners(prop);
	// synchronized(getPropertyLock(prop))
	// {
	// thePropertyStack.put(prop, this);
	// for(PrismsPCL<T> l : listeners)
	// {
	// if(l != manager)
	// l.propertyChange(propEvt);
	// /* If this property is changed as a result of the above PCL, stop this notification */
	// if(!thePropertyStack.containsKey(prop))
	// break;
	// }
	// thePropertyStack.remove(prop);
	// }
	// }

	private Object getPropertyLock(PrismsProperty<?> property)
	{
		Object ret = thePropertyLocks.get(property);
		if(ret == null)
		{
			synchronized(thePropertyLocks)
			{
				ret = thePropertyLocks.get(property);
				if(ret == null)
				{
					ret = new Object();
					thePropertyLocks.put(property, ret);
				}
			}
		}
		return ret;
	}

	/**
	 * Registers to listen for a global property change. This may be used by managers of other
	 * properties whose value depends on the value of other proprties. This should <b>NEVER</b> be
	 * called by a session-level object such as a plugin instance. Instead those objects should
	 * listen to the session-specific values of the properties.
	 * 
	 * @param <T> The type of property to listen for
	 * @param property The property to listen for
	 * @param pcl The listener to notify when the property changes
	 */
	public <T> void addGlobalPropertyChangeListener(PrismsProperty<T> property, PrismsPCL<T> pcl)
	{
		thePCLs.addListener(property, pcl);
	}

	/**
	 * Unregisters a listener for a property
	 * 
	 * @param <T> The type of property to remove the listener for
	 * @param property The property that is currently being listened for
	 * @param pcl The listener to unregister
	 */
	public <T> void removeGlobalPropertyChangeListener(PrismsProperty<T> property, PrismsPCL<T> pcl)
	{
		thePCLs.removeListener(property, pcl);
	}

	/**
	 * @param <T> The type of property to get the listeners for
	 * @param propName The property name to get the listeners for
	 * @return All listeners listening to the given property
	 */
	public <T> PrismsPCL<T> [] getGlobalPropertyChangeListeners(PrismsProperty<T> propName)
	{
		return thePCLs.getListeners(propName);
	}

	/**
	 * Runs the given task for all sessions being served by this application
	 * 
	 * @param session The session requesting the task be run
	 * @param task The task to run
	 * @param excludeSession Whether the given session should be excluded from the task
	 */
	public void runSessionTask(PrismsSession session, final SessionTask task, boolean excludeSession)
	{
		for(PrismsSession session_i : getSessions())
		{
			if(session_i == session)
			{
				if(!excludeSession)
					task.run(session);
			}
			else
			{
				final PrismsSession s = session_i;
				final Exception e = new Exception();
				s.runEventually(new Runnable()
				{
					public void run()
					{
						try
						{
							task.run(s);
						} catch(RuntimeException e2)
						{
							e2.setStackTrace(prisms.util.PrismsUtils.patchStackTraces(
								e2.getStackTrace(), e.getStackTrace(), getClass().getName(), "run"));
							throw e2;
						}
					}
				});
			}
		}
	}

	/**
	 * Fires a plugin event for all sessions in an application
	 * 
	 * @param session The session where the event originated
	 * @param toFire The event to fire
	 */
	public void fireGlobally(PrismsSession session, final PrismsEvent toFire)
	{
		toFire.setProperty(GLOBALIZED_EVENT_PROPERTY, Boolean.TRUE);
		runSessionTask(session, new SessionTask()
		{
			public void run(PrismsSession s)
			{
				s.fireEvent(toFire);
			}
		}, false);
	}

	/**
	 * Configures a new session created for this application
	 * 
	 * @param session The session to configure
	 */
	public void configureSession(PrismsSession session)
	{
		addPropertyManagers(session);
		addEventListeners(session);
		addSessionMonitors(session);
		theSessions.add(session);
	}

	/**
	 * Schedules a task to run at a specific time. The task will be run at or after the specified
	 * time at the next time this application is called.
	 * 
	 * @param task The task to run
	 * @param execTime The time to execute the task after
	 */
	public void scheduleOneTimeTask(Runnable task, long execTime)
	{
		synchronized(theOneTimeTasks)
		{
			theOneTimeTasks.add(new ScheduledTask(task, execTime, 0));
		}
	}

	/**
	 * Schedules a task to run at a given frequency. The task will be run at the given frequency at
	 * the highest, depending on when and how often this application is called.
	 * 
	 * @param task The task to run
	 * @param freq The minimum frequency to run the task at
	 */
	public void scheduleRecurringTask(Runnable task, long freq)
	{
		synchronized(theRecurringTasks)
		{
			theRecurringTasks.add(new ScheduledTask(task, System.currentTimeMillis(), freq));
		}
	}

	/**
	 * Locks this application from user interaction or unlocks it
	 * 
	 * @param message The message that should be displayed to users who try to access the
	 *        application, or null if the application is to be unlocked
	 * @param scale The scale of the task that is blocking the application. (see
	 *        {@link ApplicationLock#getScale()})
	 * @param progress The progress of the task that is blocking the application. (see
	 *        {@link ApplicationLock#getScale()})
	 * @param locker The session that locked the application--the application will not be locked
	 *        against this session
	 */
	public void setApplicationLock(String message, int scale, int progress, PrismsSession locker)
	{
		if(message == null)
			theAppLock = null;
		else if(theAppLock == null)
			theAppLock = new ApplicationLock(message, scale, progress, locker);
		else
			theAppLock.set(message, scale, progress, locker);
	}

	/**
	 * @return The application lock detailing the information to be displayed to users who try to
	 *         access the application if it is locked, or null if the application is not locked
	 */
	public ApplicationLock getApplicationLock()
	{
		return theAppLock;
	}

	/**
	 * Causes all sessions of this application to reload themselves
	 */
	public void reloadAll()
	{
		PrismsSession [] sessions = getSessions();
		for(PrismsSession session : sessions)
		{
			if(session.getClient().isService())
				continue;
			session.clearOutgoingQueue();
			session.init();
		}
	}

	/**
	 * Reloads the persistent properties in this application from their respective data sources
	 */
	@SuppressWarnings("rawtypes")
	public void reloadProperties()
	{
		for(PropertyManager manager : theManagers)
		{
			if(manager instanceof prisms.util.persisters.PersistingPropertyManager)
			{
				final prisms.util.persisters.PersistingPropertyManager ppm;
				ppm = (prisms.util.persisters.PersistingPropertyManager) manager;
				if(ppm.getPersister() == null)
					continue;
				ppm.getPersister().reload();
				ppm.setValue(ppm.getPersister().getValue());
				runSessionTask(null, new SessionTask()
				{
					public void run(PrismsSession session)
					{
						session.setProperty(ppm.getProperty(), ppm.getCorrectValue(session));
					}
				}, false);
			}
		}
	}

	/**
	 * Runs tasks that are scheduled with this application to be run
	 */
	protected void runScheduledTasks()
	{
		long currentTime = System.currentTimeMillis();
		ScheduledTask [] tasks = new ScheduledTask [0];
		synchronized(theOneTimeTasks)
		{
			java.util.Iterator<ScheduledTask> it = theOneTimeTasks.iterator();
			while(it.hasNext())
			{
				ScheduledTask task = it.next();
				if(task.shouldRun(currentTime))
				{
					tasks = prisms.util.ArrayUtils.add(tasks, task);
					it.remove();
				}
			}
			tasks = prisms.util.ArrayUtils.mergeInclusive(ScheduledTask.class, tasks,
				theRecurringTasks.toArray(new ScheduledTask [0]));
		}
		for(ScheduledTask t : tasks)
			t.run(currentTime);
	}

	/**
	 * Adds all registered property managers to the session
	 * 
	 * @param session The session to add the property managers to
	 */
	@SuppressWarnings("rawtypes")
	protected void addPropertyManagers(PrismsSession session)
	{
		PropertyManager [] mgrs;
		synchronized(theManagers)
		{
			mgrs = theManagers.toArray(new PropertyManager [0]);
		}
		for(PropertyManager mgr : mgrs)
		{
			try
			{
				if(!mgr.isValueCorrect(session, session.getProperty(mgr.getProperty())))
					session.setProperty(mgr.getProperty(), mgr.getCorrectValue(session));
				session.addPropertyChangeListener(mgr.getProperty(), mgr);
			} catch(RuntimeException e)
			{
				log.error(
					"Could not add property manager " + mgr + " for property " + mgr.getProperty()
						+ " to session", e);
				throw e;
			}
		}
	}

	/**
	 * Adds instances of all registered event listener types to the session
	 * 
	 * @param session The session to add the event listeners to
	 */
	protected void addEventListeners(PrismsSession session)
	{
		for(java.util.Map.Entry<String, PrismsEventListener []> globalEvent : theGlobalListeners
			.entrySet())
			for(PrismsEventListener listener : globalEvent.getValue())
			{
				if(globalEvent.getKey() == null)
					session.addEventListener(listener);
				else
					session.addEventListener(globalEvent.getKey(), listener);
			}
		for(EventListenerType elt : theELTypes)
		{
			PrismsEventListener pel;
			try
			{
				pel = elt.theListenerType.newInstance();
			} catch(Exception e)
			{
				log.error("Could not instantiate event listener " + elt.theEventName + ", type "
					+ elt.theListenerType.getName(), e);
				continue;
			}
			if(pel instanceof ConfiguredPEL)
			{
				try
				{
					((ConfiguredPEL) pel).configure(session, elt.theConfigEl);
				} catch(Throwable e)
				{
					log.error("Could not configure listener " + elt.theEventName
						+ " for application " + theName, e);
					continue;
				}
			}
			if(elt.theEventName == null)
				session.addEventListener(pel);
			else
				session.addEventListener(elt.theEventName, pel);
		}
	}

	/**
	 * Adds instances of all registered session monitor types to the session
	 * 
	 * @param session The session to add the monitors to
	 */
	protected void addSessionMonitors(PrismsSession session)
	{
		for(MonitorType mt : theMonitorTypes)
		{
			SessionMonitor sm;
			try
			{
				sm = mt.theMonitorType.newInstance();
			} catch(Exception e)
			{
				log.error("Could not instantiate session monitor " + mt.theMonitorType, e);
				continue;
			}
			sm.register(session, mt.theConfigEl);
		}
	}

	/**
	 * Removes the session from this application's list of active sessions
	 * 
	 * @param session The session to remove
	 */
	void removeSession(PrismsSession session)
	{
		theSessions.remove(session);
	}

	/**
	 * @return The list of active sessions that this application serves
	 */
	private PrismsSession [] getSessions()
	{
		return theSessions.toArray(new PrismsSession [theSessions.size()]);
	}

	/**
	 * Schedules a task to be run when the application is destroyed
	 * 
	 * @param task The task to run
	 */
	public void addDestroyTask(Runnable task)
	{
		synchronized(theDestroyTasks)
		{
			theDestroyTasks.add(task);
		}
	}

	/**
	 * Called when the application is no longer accessible or needed
	 */
	public void destroy()
	{
		java.util.Iterator<PrismsSession> iter = theSessions.iterator();
		while(iter.hasNext())
		{
			PrismsSession session = iter.next();
			iter.remove();
			session.destroy();
		}
		synchronized(theDestroyTasks)
		{
			Runnable [] tasks = theDestroyTasks.toArray(new Runnable [0]);
			theDestroyTasks.clear();
			for(Runnable r : tasks)
			{
				try
				{
					r.run();
				} catch(Throwable e)
				{
					log.error("Error running destroy task " + r, e);
				}
			}
		}
		if(theWorker != null)
			theWorker.close();
	}

	/**
	 * @return This application's worker for running background tasks
	 */
	public Worker getWorker()
	{
		return theWorker;
	}

	/**
	 * Sets the worker that does background tasks for this application
	 * 
	 * @param w The worker to set for the application
	 */
	public void setWorker(Worker w)
	{
		Worker oldWorker = theWorker;
		theWorker = w;
		if(oldWorker != null)
			oldWorker.close();
	}

	private static class ScheduledTask
	{
		private final Runnable theTask;

		private long theTime;

		private final long theFrequency;

		ScheduledTask(Runnable task, long time, long freq)
		{
			theTask = task;
			theTime = time;
			theFrequency = freq;
		}

		boolean shouldRun(long currentTime)
		{
			return currentTime >= theTime;
		}

		boolean run(long currentTime)
		{
			if(currentTime < theTime)
				return false;
			if(theFrequency > 0)
				theTime = currentTime + theFrequency;
			theTask.run();
			return true;
		}
	}

	@Override
	public String toString()
	{
		return theName;
	}
}
