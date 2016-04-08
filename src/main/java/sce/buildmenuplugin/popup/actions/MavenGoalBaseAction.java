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

import sce.buildmenuplugin.Maven2Runner;

/*
 * 
 * 
 * 
 * @author lucas.persson
 * 
 */

public class MavenGoalBaseAction extends ActionDelegate implements IObjectActionDelegate {

  protected IWorkbenchPart mPart = null;

  /**
   * Constructor for Action.
   */
  public MavenGoalBaseAction() {
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
      String mavenGoalString = action.getId().substring(action.getId().lastIndexOf('.') + 1);

      // Gets the selection provider and extract what we need
      ISelectionProvider provider = mPart.getSite().getSelectionProvider();
      StructuredSelection selection = (StructuredSelection) provider.getSelection();
      Iterator it = selection.iterator();
      while (it.hasNext()) {
        Object o = it.next();

        // For each Project
        if (o instanceof IResource) {
          IResource resource = (IResource) o;
          Maven2Runner.runMavenGoal(resource.getProject(), mavenGoalString);
        }
        else if (o instanceof IJavaProject) {
          IJavaProject javaProject = (IJavaProject) o;
          Maven2Runner.runMavenGoal(javaProject.getProject(), mavenGoalString);
        }
//        else if (o instanceof IWorkingSet) {
//          IWorkingSet ws = (IWorkingSet) o;
//          IAdaptable[] elements = ws.getElements();
//          // do the action for all multiprojects in the working set
//          for (IAdaptable adaptable : elements) {
//            IProject p = (IProject) adaptable.getAdapter(IProject.class);
//            if (p != null) {
//              try {
//                IProjectNature sceNature = p.getNature(CoreUtil.MULTIPROJECT_NATURE);
//                //IProjectNature maven2Nature = p.getNature("org.maven.ide.eclipse.maven2Nature");
//                if (sceNature != null) {
//                  Maven2Runner.runMavenGoal(p, mavenGoalString);
//                }
//              }
//              catch (CoreException e) {
//                BuildMenuPlugin.log(IStatus.ERROR, "Could not get nature for project: "
//                    + p.getName(), e);
//              }
//            }
//          }
//        }
        
      }
    }
  }
}
