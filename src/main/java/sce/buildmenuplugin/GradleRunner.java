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

public class GradleRunner {

  private static final String LAUNCH_CONFIG_TYPE = "org.eclipse.buildship.core.launch.runconfiguration";

  /**
   * Run a task for the project. After the task has been run, the project is
   * refreshed
   * 
   * @param project
   * @param taskString
   */
  public static void runGradleTask(final IProject project, final String taskString) {
    final ILaunchConfigurationType type = DebugPlugin.getDefault().getLaunchManager()
        .getLaunchConfigurationType(LAUNCH_CONFIG_TYPE); // $NON-NLS-1$

    if (type == null) {
      BuildMenuPlugin.log(IStatus.ERROR,
          "Could not find the lauch configuration type. Check if the Gradle plugins are installed and working.",
          null);
    }

    final String launchName = project.getName();

    IRunnableWithProgress runnable = new IRunnableWithProgress() {
      @SuppressWarnings({ "rawtypes", "unchecked" })
      public void run(IProgressMonitor monitor)
          throws InvocationTargetException, InterruptedException {
        try {
          monitor.beginTask("Running gradle task '" + taskString + "'", 100);
          ILaunchConfigurationWorkingCopy config = null;
          config = type.newInstance(null, launchName);
          
          
          // arguments
          
          config.setAttribute("gradle_distribution","GRADLE_DISTRIBUTION(WRAPPER)");
          // jvm_arguments
          config.setAttribute("show_console_view", true);
          config.setAttribute("show_execution_view", true);       
          List tasks = new ArrayList();
          tasks.add(taskString);
          config.setAttribute("tasks", tasks);
          config.setAttribute("working_dir",  "${workspace_loc:/" + project.getName() + "}");
          // some extra
          //config.setAttribute("org.eclipse.debug.ui.ATTR_LAUNCH_IN_BACKGROUND", true);

          
          
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
          DebugPlugin.getDefault().getLaunchManager().addLaunchListener(launchesListener);
          monitor.worked(100);
        } catch (CoreException ce) {
          BuildMenuPlugin.log(IStatus.ERROR,
              "Could not run the gradle task '" + taskString + "'", ce);//$NON-NLS-1$
        }
      }
    };

    // Launch the task with progress
    try {
      Progress progress = new Progress(BuildMenuPlugin.getShell(), runnable);
      progress.run();
    } catch (InvocationTargetException ite) {
      BuildMenuPlugin.log(IStatus.ERROR, "Could not run the gradle task '" + taskString + "'", //$NON-NLS-1$
          ite);
    } catch (InterruptedException ie) {
      BuildMenuPlugin.log(IStatus.ERROR, "Could not run the gradle task '" + taskString + "'", //$NON-NLS-1$
          ie);
    }
  }

}
