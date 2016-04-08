package sce.buildmenuplugin;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchesListener2;



/**
 * @author lucas.persson
 *
 */
public class LaunchesListener implements ILaunchesListener2 {
  public static abstract class PostLaunchAction  {
    public abstract void execute() throws Exception;
  }
  private ILaunch _launchToListenFor;
  private PostLaunchAction[] _postLaunchActions;

  /**
   *
   */
  public LaunchesListener(ILaunch launchToListenFor, PostLaunchAction[] postLaunchActions) {
    _launchToListenFor = launchToListenFor;
    _postLaunchActions = postLaunchActions;
  }
  
  
  public LaunchesListener(ILaunch launchToListenFor, PostLaunchAction postLaunchAction) {
    this(launchToListenFor, new PostLaunchAction[]{postLaunchAction});
  }

  public void launchesTerminated(ILaunch[] launches) {
    ILaunch launch = null;
    for (int i = 0; i < launches.length; i++) {
      launch = launches[i];
      if (launch.equals(_launchToListenFor)) {
        try {
          for(int idx = 0; idx < _postLaunchActions.length; idx++) {  
            _postLaunchActions[idx].execute();
          }
        } catch (Exception e) {
          BuildMenuPlugin.log(IStatus.ERROR, e.getMessage(), e);
          e.printStackTrace();
        }
        // remove itseft as a listener
        DebugPlugin.getDefault().getLaunchManager().removeLaunchListener(this);

        break;
      }
      
    }

  }

  /**
   *
   */

  public void launchesRemoved(ILaunch[] launches) {
  }

  /**
   *
   */

  public void launchesAdded(ILaunch[] launches) {
  }

  /**
   *
   */

  public void launchesChanged(ILaunch[] launches) {
  }

}
