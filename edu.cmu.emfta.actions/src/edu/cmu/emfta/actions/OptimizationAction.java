/**
 * Copyright (c) 2015 Carnegie Mellon University.
 * All Rights Reserved.
 * 
 * THIS SOFTWARE IS PROVIDED "AS IS," WITH NO WARRANTIES WHATSOEVER.
 * CARNEGIE MELLON UNIVERSITY EXPRESSLY DISCLAIMS TO THE FULLEST 
 * EXTENT PERMITTEDBY LAW ALL EXPRESS, IMPLIED, AND STATUTORY 
 * WARRANTIES, INCLUDING, WITHOUT LIMITATION, THE WARRANTIES OF 
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, AND 
 * NON-INFRINGEMENT OF PROPRIETARY RIGHTS.

 * This Program is distributed under a BSD license.  
 * Please see license.txt file or permission@sei.cmu.edu for more
 * information. 
 * 
 * DM-0003411
 */

package edu.cmu.emfta.actions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.sirius.business.api.action.AbstractExternalJavaAction;
import org.eclipse.sirius.diagram.business.internal.metamodel.spec.DNodeSpec;
import org.eclipse.sirius.diagram.business.internal.metamodel.spec.DSemanticDiagramSpec;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;

import edu.cmu.emfta.Event;
import edu.cmu.emfta.Gate;
import edu.cmu.emfta.GateType;

/**
 * This class is an internal singleton class that will optimize the tree.
 * 
 * @author julien
 *
 */
class Optimize
{
	private Event rootEvent;
	private List<Event> browsedElements;
	
	public Optimize (Event root)
	{
		this.rootEvent = root;
		browsedElements = new ArrayList<Event>();
	}
	
	public void perform ()
	{
		optimizeCommonOrEvents (rootEvent);
	}
	
	/**
	 * Optimize event that are under several OR'd gates. We start with the top, collect
	 * all events that are under OR gates and remove/delete events that are in deeper
	 * or gates.
	 * 
	 * @param event - the event under investigation. the code will browse sub-events.
	 */
	private void optimizeCommonOrEvents (Event event)
	{
//		System.out.println("[OptimizationAction] process: "+event.getName());

		Gate gate = event.getGate();
		List<Event> toDelete = new ArrayList<Event>();

		/**
		 * if there is no event, we do not go further
		 */
		if (gate == null)
		{
			return;
		}
		

		/**
		 * We start to see if there is any redundant events in the sub-events.
		 * We add the events to delete in a list, because if we delete that
		 * directly, it might generate inconsistencies in the list members
		 * and the iterator that browses the getEvents() call.
		 */
		for (Event subEvent : gate.getEvents())
		{
			if (browsedElements.contains(subEvent))
			{
				toDelete.add(subEvent);
			}
		}
		
		/**
		 * Now, we delete the events from the list.
		 */
		if (toDelete.size() > 0)
		{
			for (Event del : toDelete)
			{
//				System.out.println("[OptimizationAction] delete: "+del.getName());
				gate.getEvents().remove(del);
			}
		}
		
		/**
		 * If this is an OR gate, we then add the events as being already browsed.
		 * Then, these elements will be removed from further/deeper OR'd gates.
		 */
		if (gate.getType() == GateType.OR)
		{
			for (Event subEvent : gate.getEvents())
			{
				if (! browsedElements.contains(subEvent))
				{
					browsedElements.add(subEvent);
				}
			}
		}
		
		/**
		 * We continue and browse sub-events.
		 */
		for (Event subEvent : gate.getEvents())
		{
			optimizeCommonOrEvents (subEvent);
		}
		
	}
}

public class OptimizationAction extends AbstractExternalJavaAction {
	private StringBuffer report;

	@Override
	public void execute(Collection<? extends EObject> selections, Map<String, Object> parameters) {
		report = new StringBuffer();

//		System.out.println("[CutSetAction] calling execute");
		for (EObject eo : selections) {
			EObject target = null;


			if (eo instanceof DSemanticDiagramSpec) {
				DSemanticDiagramSpec ds = (DSemanticDiagramSpec) eo;
				target = ds.getTarget();
			}

			if (eo instanceof DNodeSpec) {
				DNodeSpec ds = (DNodeSpec) eo;
				target = ds.getTarget();
			}

			if (target != null) {
				System.out.println("[OptimizationAction] Optimize from event = " + target);
//				report.append("Event,declared,computed\n");
//				performComputation((Event) target);
//				Utils.writeFile(report, target);
				new Optimize((Event)target).perform();
				Utils.refreshProject(target);
				return;
			}

			MessageBox dialog = new MessageBox(Display.getDefault().getActiveShell(), SWT.ERROR | SWT.ICON_ERROR);
			dialog.setText("Error");
			dialog.setMessage("Please select an event in the FTA tree");

			dialog.open();

		}
	}


	@Override
	public boolean canExecute(Collection<? extends EObject> selections) {

		/**
		 * For now, we return true all the time. Might need to optimize
		 * it to make it more user-friendly.
		 */
		for (EObject eo : selections) {

			if (eo instanceof DSemanticDiagramSpec) {
				DSemanticDiagramSpec ds = (DSemanticDiagramSpec) eo;
				EObject target = ds.getTarget();

			}

			if (eo instanceof DNodeSpec) {
				DNodeSpec ds = (DNodeSpec) eo;
				EObject target = ds.getTarget();

				if (target instanceof edu.cmu.emfta.Event) {
					return true;
				}

				if (target instanceof edu.cmu.emfta.Tree) {
					return true;
				}

				if (target instanceof edu.cmu.emfta.FTAModel) {
					return true;
				}
			}

		}
		return false;
	}

}