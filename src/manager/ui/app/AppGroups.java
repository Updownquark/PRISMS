/*
 * AppGroupTree.java Created Jun 24, 2009 by Andrew Butler, PSL
 */
package manager.ui.app;

import manager.app.ManagerProperties;
import prisms.arch.PrismsApplication;
import prisms.arch.ds.ManageableUserSource;
import prisms.arch.ds.UserGroup;

/**
 * Allows the user to view and manage user groups within an application
 */
public class AppGroups extends prisms.ui.list.SelectableList<UserGroup>
{
	PrismsApplication theApp;

	/**
	 * @see prisms.ui.tree.DataTreeMgrPlugin#initPlugin(prisms.arch.PrismsSession, org.dom4j.Element)
	 */
	@Override
	public void initPlugin(prisms.arch.PrismsSession session, org.dom4j.Element pluginEl)
	{
		super.initPlugin(session, pluginEl);
		setSelectionMode(SelectionMode.SINGLE);
		setDisplaySelectedOnly(false);
		PrismsApplication app = session.getProperty(ManagerProperties.selectedApp);
		setApp(app);
		session.addPropertyChangeListener(ManagerProperties.selectedApp,
			new prisms.arch.event.PrismsPCL<PrismsApplication>()
			{
				public void propertyChange(prisms.arch.event.PrismsPCE<PrismsApplication> evt)
				{
					getSession().setProperty(ManagerProperties.selectedAppGroup, null);
					setApp(evt.getNewValue());
				}
			});
		session.addEventListener("userPermissionsChanged", new prisms.arch.event.PrismsEventListener()
		{
			public void eventOccurred(prisms.arch.event.PrismsEvent evt)
			{
				if(getSession().getUser().getName().equals(
					((prisms.arch.ds.User) evt.getProperty("user")).getName()))
					// Refresh this tree to take new permissions changes into account
					setApp(getSession().getProperty(ManagerProperties.selectedApp));
			}
		});
		session.addEventListener("appChanged", new prisms.arch.event.PrismsEventListener()
		{
			public void eventOccurred(prisms.arch.event.PrismsEvent evt)
			{
				PrismsApplication app2 = (PrismsApplication) evt.getProperty("app");
				if(theApp == app2)
					setApp(app2);
			}
		});
		session.addEventListener("createNewGroup", new prisms.arch.event.PrismsEventListener()
		{
			public void eventOccurred(prisms.arch.event.PrismsEvent evt)
			{
				ManageableUserSource mus = (ManageableUserSource) getSession().getApp()
					.getDataSource();
				mus.createGroup(createNewGroupName("New " + theApp.getName() + " Group"), theApp);
				getSession().fireEvent(
					new prisms.arch.event.PrismsEvent("appGroupsChanged", "app", theApp, "groups", mus
						.getGroups(theApp)));
			}
		});
		session.addEventListener("appGroupsChanged", new prisms.arch.event.PrismsEventListener()
		{
			public void eventOccurred(prisms.arch.event.PrismsEvent evt)
			{
				if(theApp == evt.getProperty("app"))
				{
					UserGroup [] groups = (UserGroup []) evt.getProperty("groups");
					if(!prisms.util.ArrayUtils.contains(groups, getSession().getProperty(
						ManagerProperties.selectedAppGroup)))
						getSession().setProperty(ManagerProperties.selectedAppGroup, null);
					setListData(groups);
				}
			}
		});
		session.addEventListener("groupChanged", new prisms.arch.event.PrismsEventListener()
		{
			public void eventOccurred(prisms.arch.event.PrismsEvent evt)
			{
				UserGroup group = (UserGroup) evt.getProperty("group");
				for(int i = 0; i < getItemCount(); i++)
					if(getItem(i) instanceof prisms.ui.list.SelectableList.ItemNode
						&& ((ItemNode) getItem(i)).getObject().equals(group))
					{
						((ItemNode) getItem(i)).check();
						break;
					}
			}
		});
	}

	String createNewGroupName(String aTry)
	{
		if(!isUsed(aTry))
			return aTry;
		int i;
		for(i = 2; isUsed(aTry + " " + i); i++);
		return aTry + " " + i;
	}

	private boolean isUsed(String groupName)
	{
		for(int i = 0; i < getItemCount(); i++)
		{
			prisms.ui.list.DataListNode item = getItem(i);
			if(item instanceof prisms.ui.list.SelectableList.ItemNode
				&& ((ItemNode) item).getObject().getName().equals(groupName))
				return true;
		}
		return false;
	}

	void setApp(PrismsApplication app)
	{
		UserGroup [] selection = getSelectedObjects(new UserGroup [0]);
		setListData(new UserGroup [0]);
		setItems(new prisms.ui.list.DataListNode [0]);
		theApp = app;
		if(app == null)
		{
			app = new PrismsApplication();
			app.setName("No application");
		}
		else
		{
			if(!(theApp.getDataSource() instanceof ManageableUserSource))
				setListData(new UserGroup [0]);
			else
				setListData(((ManageableUserSource) theApp.getDataSource()).getGroups(theApp));
			if(manager.app.ManagerUtils.canEdit(getSession().getUser(), theApp))
			{
				prisms.ui.list.ActionListNode action = new prisms.ui.list.ActionListNode(AppGroups.this,
					"createNewGroup");
				action.setText("*Create Group*");
				action.setIcon("manager/group");
				addNode(action, 0);
			}
		}
		setSelectedObjects(selection);
		setListParams();
	}

	/**
	 * @see prisms.ui.list.SelectableList#getTitle()
	 */
	@Override
	public String getTitle()
	{
		return (theApp == null ? "" : theApp.getName() + " ") + "Groups";
	}

	/**
	 * @see prisms.ui.list.SelectableList#getIcon()
	 */
	@Override
	public String getIcon()
	{
		return "manager/application";
	}

	/**
	 * @see prisms.ui.list.SelectableList#createObjectNode(java.lang.Object)
	 */
	@Override
	public ItemNode createObjectNode(UserGroup item)
	{
		ItemNode ret = super.createObjectNode(item);
		if(manager.app.ManagerUtils.canEdit(getSession().getUser(), theApp))
		{
			ret.addAction(new javax.swing.AbstractAction("Delete")
			{
				public void actionPerformed(java.awt.event.ActionEvent evt)
				{
					final ItemNode node = (ItemNode) evt.getSource();
					final UserGroup group = node.getObject();
					final prisms.ui.UI ui = (prisms.ui.UI) getSession().getPlugin("UI");
					prisms.ui.UI.ConfirmListener cl = new prisms.ui.UI.ConfirmListener()
					{
						public void confirmed(boolean confirm)
						{
							if(!confirm)
								return;
							ManageableUserSource mus = (ManageableUserSource) getSession().getApp()
								.getDataSource();

							prisms.arch.ds.User[] users = getSession().getProperty(
								ManagerProperties.users);
							users = users.clone();
							for(int u = 0; u < users.length; u++)
							{
								users[u] = mus.getUser(users[u], group.getApp());
								if(users[u] == null
									|| !prisms.util.ArrayUtils
										.contains(users[u].getGroups(), group))
								{
									users = prisms.util.ArrayUtils.remove(users, u);
									u--;
								}
							}

							mus.deleteGroup(group);
							getSession().fireEvent(
								new prisms.arch.event.PrismsEvent("appGroupsChanged", "app", theApp,
									"groups", mus.getGroups(theApp)));
							for(int u = 0; u < users.length; u++)
							{
								getSession().fireEvent(
									new prisms.arch.event.PrismsEvent("userGroupsChanged", "user",
										users[u]));
								getSession().fireEvent(
									new prisms.arch.event.PrismsEvent("userPermissionsChanged", "user",
										users[u]));
							}
						}
					};
					if(ui != null)
						ui.confirm("Are you sure you want to delete group " + group.getName()
							+ " from application " + theApp.getName() + "?", cl);
					else
						cl.confirmed(true);
				}
			});
		}
		return ret;
	}

	/**
	 * @see prisms.ui.list.SelectableList#canDeselect(java.lang.Object)
	 */
	@Override
	public boolean canDeselect(UserGroup item)
	{
		return true;
	}

	/**
	 * @see prisms.ui.list.SelectableList#canSelect(java.lang.Object)
	 */
	@Override
	public boolean canSelect(UserGroup item)
	{
		return true;
	}

	/**
	 * @see prisms.ui.list.SelectableList#doDeselect(java.lang.Object)
	 */
	@Override
	public void doDeselect(UserGroup item)
	{
		if(getSession().getProperty(ManagerProperties.selectedAppGroup) == item)
			getSession().setProperty(ManagerProperties.selectedAppGroup, null);
	}

	/**
	 * @see prisms.ui.list.SelectableList#doSelect(java.lang.Object)
	 */
	@Override
	public void doSelect(UserGroup item)
	{
		getSession().setProperty(ManagerProperties.selectedAppGroup, item);
	}

	/**
	 * @see prisms.ui.list.SelectableList#getItemName(java.lang.Object)
	 */
	@Override
	public String getItemName(UserGroup item)
	{
		return item.getName();
	}

	/**
	 * @see prisms.ui.list.SelectableList#getItemIcon(java.lang.Object)
	 */
	@Override
	public String getItemIcon(UserGroup item)
	{
		return "manager/group";
	}
}
