package sce.buildmenuplugin;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.ILaunchesListener2;
import org.eclipse.jface.operation.IRunnableWithProgress;

public class Maven2Runner {

  private static final String MAVEN_LAUNCH_CONFIG_TYPE = "org.eclipse.m2e.Maven2LaunchConfigurationType"; //$NON-NLS-1$
// org.eclipse.m2e.actions.MavenLaunchConstants.MavenLaunchConstants.LAUNCH_CONFIGURATION_TYPE_ID;  
  /**
   * Run a maven goal for the project. After the goal has been run, the
   * project is refreshed
   * 
   * @param project
   * @param mavenGoalString
   */
  public static void runMavenGoal(final IProject project,
      final String mavenGoalString) {
    final ILaunchConfigurationType type = DebugPlugin.getDefault().getLaunchManager()
        .getLaunchConfigurationType(MAVEN_LAUNCH_CONFIG_TYPE); //$NON-NLS-1$
    
    if (type == null) {
      BuildMenuPlugin.log(IStatus.ERROR, "Could not find Maven lauch configuration type. Check if the m2e plugins are installed and working.", null);
    }
    
    final String launchName = project.getName();
    
    IRunnableWithProgress runnable = new IRunnableWithProgress() {
      @SuppressWarnings({ "rawtypes", "unchecked" })
	public void run(IProgressMonitor monitor)
          throws InvocationTargetException, InterruptedException {
        try {
          monitor.beginTask("Running maven goal '" + mavenGoalString + "'", 100);
          ILaunchConfigurationWorkingCopy config = null;
          config = type.newInstance(null, launchName);
          List options = new ArrayList();
          config.setAttribute(
              "org.eclipse.debug.ui.ATTR_LAUNCH_IN_BACKGROUND", true);
          config.setAttribute("M2_PROPERTIES", options);
          config.setAttribute("org.eclipse.jdt.launching.WORKING_DIRECTORY", 
              "${workspace_loc:/" + project.getName() + "}");
          config.setAttribute("M2_GOALS", mavenGoalString);
          config.setAttribute("M2_PROFILES", "");
          // Launch the goal
          ILaunch launch = config.launch(ILaunchManager.RUN_MODE, new NullProgressMonitor());
          // add listener so the project can be refreshed later when the ant
          // target is terminaded
          DebugPlugin.getDefault().getLaunchManager().addLaunch(launch);
          LaunchesListener.PostLaunchAction pla = new LaunchesListener.PostLaunchAction() {
            public void execute() throws Exception {
              project.refreshLocal(IResource.DEPTH_INFINITE, null);
            }
          };
          ILaunchesListener2 launchesListener = new LaunchesListener(launch, pla);
          DebugPlugin.getDefault().getLaunchManager().addLaunchListener(
              launchesListener);
          monitor.worked(100);
        }
        catch (CoreException ce) {
          BuildMenuPlugin.log(IStatus.ERROR,
              "Could not run the maven goal '" + mavenGoalString + "'", ce);//$NON-NLS-1$
        }
      }
    };

    // Launch the task with progress
    try {
      Progress progress = new Progress(BuildMenuPlugin.getShell(), runnable);
      progress.run();
    }
    catch (InvocationTargetException ite) {
      BuildMenuPlugin.log(IStatus.ERROR,
          "Could not run the maven goal '" + mavenGoalString + "'", ite);//$NON-NLS-1$
    }
    catch (InterruptedException ie) {
      BuildMenuPlugin.log(IStatus.ERROR,
          "Could not run the maven goal '" + mavenGoalString + "'", ie);//$NON-NLS-1$
    }
  }

  
}
