package sce.buildmenuplugin.popup.actions;

import java.util.Iterator;

import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IActionDelegate;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionDelegate;

import sce.buildmenuplugin.CoreUtil;



/*
 * 
 * 
 * search on "Running Ant buildfiles programmatically" in Eclipse on-line help
 * 
 * @author lucas.persson
 *  
 */

public class AntTargetBaseAction extends ActionDelegate implements
    IObjectActionDelegate {

  protected IWorkbenchPart mPart = null;

  /**
   * Constructor for Action.
   */
  public AntTargetBaseAction() {
    super();
  }

  
  /**
   * @see IObjectActionDelegate#setActivePart(IAction, IWorkbenchPart)
   */
  public void setActivePart(IAction action, IWorkbenchPart targetPart) {
    this.mPart = targetPart;
  }

  /**
   * @see IActionDelegate#run(IAction)
   */
  @SuppressWarnings("rawtypes")
  public void run(IAction action) {

    if (PlatformUI.getWorkbench().saveAllEditors(true)) {    
	    String antTargetString = action.getId().substring(action.getId().lastIndexOf('.')+1);
	    
	    // Gets the selection provider and extract what we need
	    ISelectionProvider provider = mPart.getSite().getSelectionProvider();
	    StructuredSelection selection = (StructuredSelection) provider.getSelection();
	    Iterator it = selection.iterator();
	    while (it.hasNext()) {
	      Object o = it.next();
	
	      // For each Project
	      if (o instanceof IResource) {
	        IResource resource = (IResource) o;
	        CoreUtil.runAntTarget(resource.getProject(), antTargetString, action.getToolTipText());
	      }
	      else if (o instanceof IJavaProject) {
	        IJavaProject javaProject = (IJavaProject) o;
	        CoreUtil.runAntTarget(javaProject.getProject(), antTargetString, action.getToolTipText());
	      }
	    }
    }
  }
  
}

