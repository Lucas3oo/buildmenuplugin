package sce.buildmenuplugin;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Shell;

public class Progress
{
   /** The runnable to launch */
   private IRunnableWithProgress runnable;
   /** The container shell */
   private Shell shell;


   /**
    * Build a Progress object
    *
    * @param shell     The container shell
    * @param runnable  The runnable to launch
    */
   public Progress(Shell shell, IRunnableWithProgress runnable)
   {
      this.runnable = runnable;
      this.shell = shell;
   }


   /**
    * Launch the ProgressDialog and run the runnable
    *
    * @exception InvocationTargetException  Exception when trying to invoke the target
    * @exception InterruptedException       Exception when interrupting
    */
   public void run()
          throws InvocationTargetException, InterruptedException
   {
      // Launch the progress monitor
      ProgressMonitorDialog progressMonitorDialog = new ProgressMonitorDialog(Progress.this.shell);
      progressMonitorDialog.run(true, true, this.runnable);
   }
}
