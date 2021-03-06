/*
 * AllApplicationsList.java Created Jul 10, 2008 by Andrew Butler, PSL
 */
package manager.ui.app;

import manager.app.ManagerProperties;
import prisms.arch.PrismsApplication;

/** Displays all applications to the user and allows the user to select one */
public class AllApplicationsList extends prisms.ui.list.SelectableList<PrismsApplication>
{
	@Override
	public void initPlugin(prisms.arch.PrismsSession session, prisms.arch.PrismsConfig config)
	{
		super.initPlugin(session, config);
		setDisplaySelectedOnly(false);
		setSelectionMode(SelectionMode.SINGLE);
		setListData(session.getProperty(prisms.arch.event.PrismsProperties.applications));
		if(session.getProperty(ManagerProperties.selectedApp) != null)
			setSelectedObjects(new PrismsApplication [] {session
				.getProperty(ManagerProperties.selectedApp)});
		session.addPropertyChangeListener(ManagerProperties.selectedApp,
			new prisms.arch.event.PrismsPCL<PrismsApplication>()
			{
				public void propertyChange(prisms.arch.event.PrismsPCE<PrismsApplication> evt)
				{
					if(evt.getNewValue() == null)
						setSelectedObjects(new PrismsApplication [0]);
					else
						setSelectedObjects(new PrismsApplication [] {evt.getNewValue()});
				}

				@Override
				public String toString()
				{
					return "Manager App View Updater";
				}
			});
	}

	@Override
	public String getIcon()
	{
		return "manager/application";
	}

	@Override
	public String getTitle()
	{
		return "All Applications";
	}

	@Override
	public String getItemIcon(PrismsApplication obj)
	{
		return "manager/application";
	}

	@Override
	public String getItemName(PrismsApplication obj)
	{
		return obj.getName();
	}

	@Override
	public boolean canSelect(PrismsApplication a)
	{
		return true;
	}

	@Override
	public void doSelect(PrismsApplication a)
	{
		getSession().setProperty(ManagerProperties.selectedApp, a);
	}

	@Override
	public boolean canDeselect(PrismsApplication a)
	{
		return true;
	}

	@Override
	public void doDeselect(PrismsApplication a)
	{
		if(getSession().getProperty(ManagerProperties.selectedApp) == a)
			getSession().setProperty(ManagerProperties.selectedApp, null);
	}
}
