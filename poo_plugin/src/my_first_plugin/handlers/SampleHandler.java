package my_first_plugin.handlers;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.LibraryLocation;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.dialogs.IOverwriteQuery;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.internal.wizards.datatransfer.ZipLeveledStructureProvider;
import org.eclipse.ui.wizards.datatransfer.ImportOperation;

public class SampleHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
		String input = null;

		// Initializing input dialog window to take matricola code
		InputDialog dlg = new InputDialog(
				HandlerUtil.getActiveShellChecked(event), "Setup Esame di POO",
				"Immetti la tua matricola", "matricola", null);
		if (dlg.open() == Window.OK) {
			input = dlg.getValue();
		}

		// Creating the project handler
		IProject project = workspaceRoot.getProject(input);
		try {
			project.create(null);
		} catch (CoreException e) {}
		try {
			project.open(null);
		} catch (CoreException e) {
			e.printStackTrace();
		}


		// Setting project description similar to a java project
		IProjectDescription description = null;
		try {
			description = project.getDescription();
		} catch (CoreException e) {
			e.printStackTrace();
		}
		description.setNatureIds(new String[] { JavaCore.NATURE_ID });
		try {
			project.setDescription(description, null);
		} catch (CoreException e) {
			e.printStackTrace();
		}


		// Creating java project
		IJavaProject javaProject = JavaCore.create(project);


		// Setting executable output folder
		IFolder binFolder = project.getFolder("bin");
		try {
			binFolder.create(false, true, null);
			// If project contents on disk have not been cancelled no need to create the bin folder
		} catch (CoreException e) {}
		try {
			javaProject.setOutputLocation(binFolder.getFullPath(), null);
		} catch (JavaModelException e) {
			e.printStackTrace();
		}


		// Add libraries to project class path
		List<IClasspathEntry> entries = new ArrayList<IClasspathEntry>();
		IVMInstall vmInstall = JavaRuntime.getDefaultVMInstall();
		LibraryLocation[] locations = JavaRuntime.getLibraryLocations(vmInstall);
		for (LibraryLocation element : locations) {
			entries.add(JavaCore.newLibraryEntry(element.getSystemLibraryPath(), null, null));
		}
		String os = System.getProperty("os.name");
		final String homeDir = System.getProperty("user.home");
		if (os.startsWith("Windows"))
		{
			entries.add(JavaCore.newLibraryEntry(Path.forWindows(homeDir + "/.p2/pool/plugins/org.junit_4.13.2.v20211018-1956.jar"), null, null));
			entries.add(JavaCore.newLibraryEntry(Path.forWindows(homeDir + "/.p2/pool/plugins/org.hamcrest.core_1.3.0.v20180420-1519.jar"), null, null));
		}
		else if (os.startsWith("Linux"))
		{
			entries.add(JavaCore.newLibraryEntry(new Path(homeDir + "/.p2/pool/plugins/org.junit_4.13.2.v20211018-1956.jar"), null, null));
			entries.add(JavaCore.newLibraryEntry(new Path(homeDir + "/.p2/pool/plugins/org.hamcrest.core_1.3.0.v20180420-1519.jar"), null, null));
		}
		try {
			javaProject.setRawClasspath(entries.toArray(new IClasspathEntry[entries.size()]), null);
		} catch (JavaModelException e) {
			e.printStackTrace();
		}
		
		
		ZipFile zipFile = null;
		try {
			if (System.getProperty("os.name").startsWith("Linux"))
			{
				if (System.getProperty("user.language").equals("en"))
					zipFile = new ZipFile(homeDir + "/Downloads/" + "compito.zip");
				else if (System.getProperty("user.language").equals("it"))
					zipFile = new ZipFile(homeDir + "/Scaricati/" + "compito.zip");
			}
			else if (System.getProperty("os.name").startsWith("Windows"))
				zipFile = new ZipFile(homeDir + "\\Downloads\\" + "compito.zip");
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		IOverwriteQuery overwriteQuery = new IOverwriteQuery() {
		    public String queryOverwrite(String file) { return ALL; }
		};
		ZipLeveledStructureProvider provider = new ZipLeveledStructureProvider(zipFile);
		List<Object> fileSystemObjects = new ArrayList<Object>();
		Enumeration<? extends ZipEntry> entries2 = zipFile.entries();
		while (entries2.hasMoreElements()) {
		    fileSystemObjects.add((Object)entries2.nextElement());
		}
		ImportOperation importOperation = new ImportOperation(project.getFullPath(), new ZipEntry(project.getName()), provider, overwriteQuery, fileSystemObjects);
		importOperation.setCreateContainerStructure(false);
		try {
			importOperation.run(new NullProgressMonitor());
		} catch (InvocationTargetException | InterruptedException e) {
			e.printStackTrace();
		}
		return null;
	}
}
