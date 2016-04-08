package sce.buildmenuplugin;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;
import org.eclipse.ant.internal.ui.launchConfigurations.AntLaunchShortcut;
import org.eclipse.ant.ui.launching.IAntLaunchConfigurationConstants;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.ILaunchesListener2;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jface.operation.IRunnableWithProgress;

/**
 * @author mikael.printz
 * 
 * Utility class for calling ant targets and maven goals
 * programmatically from within an Eclipse plugin
 */
@SuppressWarnings({ "restriction", "deprecation" })
public class CoreUtil {

  public final static String BUILD_FILE = "build.xml";//$NON-NLS-1$
  
  
  /**
   * Run an ant target for the project. After the target has been run, the
   * project is refreshed
   * 
   * @param project
   * @param antTargetString
   * @param actionString
   */
  public static void runAntTarget(final IProject project,
      final String antTargetString, final String actionString) {
    runAntTarget(project, antTargetString, actionString, null);
  }

  /**
   * Run an ant target for the project with an extra post launch action. The
   * post launch action will be executed prior to project refresh
   * 
   * @param project
   * @param antTargetString
   * @param actionString
   */
  public static void runAntTarget(final IProject project,
      final String antTargetString, final String actionString,
      final LaunchesListener.PostLaunchAction postLaunchAction) {

    final IFile buildFile = project.getFile(BUILD_FILE);

    if (buildFile.exists()) {
      IRunnableWithProgress runnable = new IRunnableWithProgress() {
		@SuppressWarnings({  "rawtypes" })
		public void run(IProgressMonitor monitor)
            throws InvocationTargetException, InterruptedException {
          try {
            monitor.beginTask(actionString, 100);

            // Load the Ant build file
            Project antProject = new Project();
            antProject.init();
            ProjectHelper.configureProject(antProject, buildFile.getLocation()
                .toFile());
            String buildFileLocation = buildFile.getLocation().toOSString();

            ILaunchConfigurationType type = DebugPlugin
                .getDefault()
                .getLaunchManager()
                .getLaunchConfigurationType(
                    IAntLaunchConfigurationConstants.ID_ANT_LAUNCH_CONFIGURATION_TYPE); //$NON-NLS-1$

            ILaunchConfigurationWorkingCopy config = null;
            String launchName = antProject.getName() + " " + BUILD_FILE;
            // Find an old launch configuration for this project
            Iterator cfgs = AntLaunchShortcut.findExistingLaunchConfigurations(
                buildFile).iterator();
            while (cfgs.hasNext()) {
              ILaunchConfiguration cfg = (ILaunchConfiguration) cfgs.next();
              if (cfg.getName().equalsIgnoreCase(launchName))
                config = cfg.getWorkingCopy();
            }
            if (null == config)
              config = type.newInstance(null, launchName);

            IVMInstall realDefault = JavaRuntime.getDefaultVMInstall();

            config.setAttribute(
                "org.eclipse.debug.ui.ATTR_LAUNCH_IN_BACKGROUND", true);
            config.setAttribute(
                "org.eclipse.ui.externaltools.ATTR_ANT_TARGETS",
                antTargetString);
            config.setAttribute(
                "org.eclipse.ui.externaltools.ATTR_CAPTURE_OUTPUT", true);
            config.setAttribute("org.eclipse.ui.externaltools.ATTR_LOCATION",
                buildFileLocation);
            config.setAttribute(
                "org.eclipse.ui.externaltools.ATTR_SHOW_CONSOLE", true);
            config.setAttribute("org.eclipse.jdt.launching.CLASSPATH_PROVIDER",
                "org.eclipse.ant.ui.AntClasspathProvider");
            config.setAttribute("org.eclipse.jdt.launching.DEFAULT_CLASSPATH",
                true);
            config.setAttribute("org.eclipse.jdt.launching.MAIN_TYPE",
                "org.eclipse.ant.internal.ui.antsupport.InternalAntRunner");
            config.setAttribute("org.eclipse.jdt.launching.VM_INSTALL_TYPE_ID",
                "org.eclipse.jdt.internal.debug.ui.launcher.StandardVMType");
            config.setAttribute("org.eclipse.jdt.launching.VM_INSTALL_NAME",
                realDefault.getName());
            config.setAttribute("org.eclipse.jdt.launching.PROJECT_ATTR",
                project.getName());
            config.setAttribute("process_factory_id",
                "org.eclipse.ant.ui.remoteAntProcessFactory");
            config.setAttribute(
                "org.eclipse.debug.core.appendEnvironmentVariables", true);

            // config.setAttribute("org.eclipse.ui.externaltools.ATTR_ANT_PROPERTIES",
            // propMap);

            config.doSave();

            // Launch the target
            ILaunch launch = config.launch(ILaunchManager.RUN_MODE, monitor);
            // add listener so the project can be refreshed later when the ant
            // target is terminaded
            DebugPlugin.getDefault().getLaunchManager().addLaunch(launch);
            LaunchesListener.PostLaunchAction pla = new LaunchesListener.PostLaunchAction() {
              public void execute() throws Exception {
                project.refreshLocal(IResource.DEPTH_INFINITE, null);
              }
            };
            ILaunchesListener2 launchesListener = null;
            if (postLaunchAction != null) {
              launchesListener = new LaunchesListener(launch,
                  new LaunchesListener.PostLaunchAction[] { postLaunchAction,
                      pla });
            }
            else {
              launchesListener = new LaunchesListener(launch, pla);
            }

            DebugPlugin.getDefault().getLaunchManager().addLaunchListener(
                launchesListener);

          }
          catch (CoreException ce) {
            BuildMenuPlugin.log(IStatus.ERROR,
                "Could not run the ant target: " + antTargetString, ce);//$NON-NLS-1$
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
            "Could not run the ant target: " + antTargetString, ite);//$NON-NLS-1$
      }
      catch (InterruptedException ie) {
        BuildMenuPlugin.log(IStatus.ERROR,
            "Could not run the ant target: " + antTargetString, ie);//$NON-NLS-1$
      }
    }
    else {
      BuildMenuPlugin.log(IStatus.INFO,
          "Project " + project.getName() + " does not has " + BUILD_FILE, null);//$NON-NLS-1$
    }

  }


  public static void updateJavadocLocation(IJavaProject javaProject) {
    try {
      IClasspathEntry[] cl = javaProject.getRawClasspath(); 
      for (int i = 0; i < cl.length; i++) {
        IClasspathEntry entry = cl[i];
        if (entry.getEntryKind() == IClasspathEntry.CPE_VARIABLE) {
          // then we know that this a lib pointed out by maven
          IClasspathEntry resolvedEntry = JavaCore.getResolvedClasspathEntry(entry);
          String pathString = resolvedEntry.getPath().toPortableString().replaceAll("jar","javadoc.jar");
          IPath path = new Path(pathString);
          String urlString = null;
          if (path.toFile().exists()) {
            urlString = "jar:file:/" + path.toString() + "!/"; 
          }
          else {
            pathString = pathString.replaceAll("-client","");
            path = new Path(pathString);
            if (path.toFile().exists()) {
              urlString = "jar:file:/" + path.toString() + "!/";
            }
          }
          if (null != urlString) {
            IClasspathAttribute attribute = JavaCore.newClasspathAttribute(
                IClasspathAttribute.JAVADOC_LOCATION_ATTRIBUTE_NAME,
                urlString);
            IClasspathEntry newEntry = cloneVariableEntry(entry, attribute);
            cl[i] = newEntry;
          }
        }
        javaProject.setRawClasspath(cl, new NullProgressMonitor());
      }
    }
    catch (JavaModelException e) {
      String errMsg = "Could not get classpath information for project "
          + javaProject.getElementName();
      BuildMenuPlugin.log(IStatus.ERROR, errMsg, e);
    }
  }
  
  

  private static IClasspathEntry cloneVariableEntry(IClasspathEntry entry,
      IClasspathAttribute javadocLocation) {
    
    IClasspathAttribute[] extraAttributes = entry.getExtraAttributes();
    // find the old javadoc location attribute if any
    boolean createNew = true;
    for (int i = 0; i < extraAttributes.length; i++) {
      if (extraAttributes[i].getName().equalsIgnoreCase(IClasspathAttribute.JAVADOC_LOCATION_ATTRIBUTE_NAME)) {
        createNew = false;
        // replace the old one
        extraAttributes[i] = javadocLocation;
        break;
      }
    }
    if (createNew) {
      int length = extraAttributes.length;
      IClasspathAttribute[] newExtraAttributes = new IClasspathAttribute[length + 1];
      for (int i = 0; i < extraAttributes.length; i++) {
        newExtraAttributes[i] = extraAttributes[i];
      }
      newExtraAttributes[length] = javadocLocation;
      extraAttributes = newExtraAttributes; 
    }
    
    return JavaCore.newVariableEntry(entry.getPath(), entry
        .getSourceAttachmentPath(), entry.getSourceAttachmentRootPath(), entry
        .getAccessRules(), extraAttributes, entry.isExported());

  }
  
  
  
  
  
  public static void addNature(IProject project, String nature) {
    try {
      IProjectDescription description = project.getDescription();
      String[] natures = description.getNatureIds();
      String[] newNatures = new String[natures.length + 1];
      System.arraycopy(natures, 0, newNatures, 0, natures.length);
      newNatures[natures.length] = nature;
      description.setNatureIds(newNatures);
      project.setDescription(description, null);
    }
    catch (CoreException e) {
      BuildMenuPlugin.log(IStatus.ERROR,
          "Could not add nature " + nature + " for project " + project.getName(), e);//$NON-NLS-1$
    }
  }

  

  public static void removeNature(IProject project, String nature) {
    try {
      IProjectDescription description = project.getDescription();
      String[] natures = description.getNatureIds();
      Set<String> naturesSet = new LinkedHashSet<String>();
      naturesSet.addAll(Arrays.asList(natures));
      naturesSet.remove(nature);
      String[] newNatures = (String[]) naturesSet.toArray(new String[naturesSet.size()]);
      description.setNatureIds(newNatures);
      project.setDescription(description, null);
    }
    catch (CoreException e) {
      BuildMenuPlugin.log(IStatus.ERROR,
          "Could not remove mutiproject nature for project " + project.getName(), e);//$NON-NLS-1$
    }
  }
  
  

 
}
