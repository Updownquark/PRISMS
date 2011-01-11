/**
 * PreferencesPersister.java Created Jul 7, 2008 by Andrew Butler, PSL
 */
package prisms.util.preferences;

import java.sql.SQLException;
import java.util.Map;

import org.apache.log4j.Logger;

import prisms.arch.PrismsApplication;
import prisms.arch.ds.User;
import prisms.arch.event.PrismsProperty;
import prisms.util.DBUtils;

/**
 * Persists preferences needed by PRISMS to a database
 */
public class PreferencesPersister implements
	prisms.util.persisters.UserSpecificPersister<Preferences>
{
	private static final Logger log = Logger.getLogger(PreferencesPersister.class);

	private PrismsApplication theApp;

	private org.dom4j.Element theConnEl;

	private java.sql.Connection theConnection;

	private String DBOWNER;

	/**
	 * @see prisms.arch.Persister#configure(org.dom4j.Element, prisms.arch.PrismsApplication,
	 *      prisms.arch.event.PrismsProperty)
	 */
	public void configure(org.dom4j.Element configEl, PrismsApplication app,
		PrismsProperty<Map<String, Preferences>> property)
	{
		theApp = app;
		theConnEl = configEl;
		checkConnection();
	}

	/**
	 * @see prisms.arch.Persister#getValue()
	 */
	@SuppressWarnings("rawtypes")
	public Map<String, Preferences> getValue()
	{
		checkConnection();
		Map<String, Preferences> ret = new java.util.HashMap<String, Preferences>();
		java.sql.Statement stmt = null;
		java.sql.ResultSet rs = null;
		try
		{
			stmt = theConnection.createStatement();
			rs = stmt.executeQuery("SELECT pUser, pDomain, pName, pType, pDisplayed, pValue FROM "
				+ DBOWNER + "prisms_preference WHERE pApp=" + DBUtils.toSQL(theApp.getName()));
			while(rs.next())
			{
				String user = rs.getString(1);
				if(user == null)
					continue;
				Preferences userPrefs = ret.get(user);
				if(userPrefs == null)
				{
					userPrefs = new Preferences(theApp.getEnvironment().getUserSource()
						.getUser(user));
					ret.put(user, userPrefs);
				}
				String domain = DBUtils.fromSQL(rs.getString(2));
				String propName = DBUtils.fromSQL(rs.getString(3));
				Preference.Type type = Preference.Type.valueOf(DBUtils.fromSQL(rs.getString(4)));
				boolean displayed = prisms.util.DBUtils.boolFromSql(rs.getString(5));
				String valueS = DBUtils.fromSQL(rs.getString(6));
				Object value;
				try
				{
					value = type.deserialize(valueS);
				} catch(Exception e)
				{
					log.error("Could not deserialize preference " + domain + "/" + propName
						+ " type " + type + " value=" + valueS, e);
					continue;
				}
				userPrefs.set(new Preference(domain, propName, type, type.getType(), displayed),
					value);
			}
		} catch(Exception e)
		{
			log.error("Could not retrieve preferences", e);
			return new java.util.HashMap<String, Preferences>();
		} finally
		{
			if(rs != null)
				try
				{
					rs.close();
				} catch(SQLException e)
				{}
			if(stmt != null)
				try
				{
					stmt.close();
				} catch(SQLException e)
				{}
		}
		for(Preferences prefs : ret.values())
			prefs.addListener(new Preferences.Listener()
			{
				public void domainRemoved(Preferences p, String domain)
				{
					removeDomain(p.getOwner(), domain);
				}

				public <T, V extends T> void prefChanged(Preferences p, Preference<T> pref, V value)
				{
					setValue(p.getOwner(), pref, value);
				}
			});
		return ret;
	}

	public Preferences create(User user)
	{
		Preferences ret = new Preferences(user);
		ret.addListener(new Preferences.Listener()
		{
			public <T, V extends T> void prefChanged(Preferences p, Preference<T> pref, V value)
			{
				setValue(p.getOwner(), pref, value);
			}

			public void domainRemoved(Preferences p, String domain)
			{
				removeDomain(p.getOwner(), domain);
			}
		});
		return ret;
	}

	/**
	 * @see prisms.arch.Persister#link(java.lang.Object)
	 */
	public Map<String, Preferences> link(Map<String, Preferences> value)
	{
		return value;
	}

	public <V extends Map<String, Preferences>> void setValue(prisms.arch.PrismsSession session,
		V o, @SuppressWarnings("rawtypes") prisms.arch.event.PrismsPCE evt)
	{
		// Don't commit the world--this persister persists with each change
	}

	public void valueChanged(prisms.arch.PrismsSession session, Map<String, Preferences> fullValue,
		Object o, prisms.arch.event.PrismsEvent evt)
	{
	}

	void setValue(User user, Preference<?> pref, Object value)
	{
		checkConnection();
		java.sql.Statement stmt = null;
		java.sql.ResultSet rs = null;
		String whereClause = "WHERE pUser = " + DBUtils.toSQL(user.getName()) + " AND pDomain = "
			+ DBUtils.toSQL(pref.getDomain()) + " AND pName = " + DBUtils.toSQL(pref.getName())
			+ " AND pApp=" + DBUtils.toSQL(theApp.getName());
		try
		{
			stmt = theConnection.createStatement();
			if(value == null)
				stmt.execute("DELETE FROM " + DBOWNER + "prisms_preference " + whereClause);
			else
			{
				String valueS = pref.getType().serialize(value);
				rs = stmt.executeQuery("SELECT COUNT(*) FROM " + DBOWNER + "prisms_preference "
					+ whereClause);
				if(rs.next() && rs.getInt(1) > 0)
					stmt.executeUpdate("UPDATE " + DBOWNER + "prisms_preference SET pValue = "
						+ DBUtils.toSQL(valueS) + " " + whereClause);
				else
					stmt.execute("INSERT INTO " + DBOWNER + "prisms_preference "
						+ "(pApp, pUser, pDomain, pName, pType, pDisplayed, pValue) VALUES ("
						+ DBUtils.toSQL(theApp.getName()) + ", " + DBUtils.toSQL(user.getName())
						+ ", " + DBUtils.toSQL(pref.getDomain()) + ", "
						+ DBUtils.toSQL(pref.getName()) + ", "
						+ DBUtils.toSQL(pref.getType().name()) + ", "
						+ prisms.util.DBUtils.boolToSql(pref.isDisplayed()) + ", "
						+ DBUtils.toSQL(valueS) + ")");
			}
		} catch(SQLException e)
		{
			log.error("Could not persist preference " + pref + " of user " + user.getName(), e);
		} finally
		{
			if(rs != null)
				try
				{
					rs.close();
				} catch(SQLException e)
				{}
			if(stmt != null)
				try
				{
					stmt.close();
				} catch(SQLException e)
				{}
		}
	}

	void removeDomain(User user, String domain)
	{
		checkConnection();
		java.sql.Statement stmt = null;
		try
		{
			stmt = theConnection.createStatement();
			stmt.execute("DELETE FROM " + DBOWNER + "prisms_preference WHERE pUser = "
				+ DBUtils.toSQL(user.getName()) + " AND pDomain = " + DBUtils.toSQL(domain)
				+ " AND pApp=" + DBUtils.toSQL(theApp.getName()));
		} catch(SQLException e)
		{
			log.error(
				"Could not remove preference domain " + domain + " for user " + user.getName(), e);
		} finally
		{
			if(stmt != null)
				try
				{
					stmt.close();
				} catch(SQLException e)
				{}
		}
	}

	private void checkConnection()
	{
		try
		{
			if(theConnection != null && theConnection.isClosed())
			{
				log.warn("Connection closed!");
				theConnection = null;
			}
		} catch(SQLException e)
		{
			log.warn("Could not check closed status of connection", e);
		}
		if(theConnection != null)
			return;
		try
		{
			prisms.arch.PrismsEnv env = theApp.getEnvironment();
			theConnection = env.getPersisterFactory().getConnection(theConnEl);
			DBOWNER = env.getPersisterFactory().getTablePrefix(theConnection, theConnEl);
		} catch(Exception e)
		{
			throw new IllegalStateException("Could not get connection!", e);
		}
	}

	public void reload()
	{
		// No cache to clear
	}
}
