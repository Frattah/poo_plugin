package my_first_plugin.handlers;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
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
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.dialogs.IOverwriteQuery;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.internal.wizards.datatransfer.ZipLeveledStructureProvider;
import org.eclipse.ui.wizards.datatransfer.ImportOperation;

@SuppressWarnings("restriction")
public class SampleHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		// Load properties from configuration.xml file
		Properties properties = new Properties();
		try {
			properties.loadFromXML(getClass().getResourceAsStream("configuration.xml"));
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
		IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
		String input = null;

		// Initializing input dialog window to take serial number
		InputDialog dlg = new InputDialog(
				HandlerUtil.getActiveShellChecked(event), properties.getProperty("titleWindow"),
				properties.getProperty("messageInputDialog"), properties.getProperty("defaultSerialNumber"), null);
		if (dlg.open() == Window.OK) {
			input = dlg.getValue();
		}

		// if the student closes the dialogue window without entering a serial number
		if (input == null)
			return null;
		
		// Create the project handler and give it the serial number as name
		IProject project = workspaceRoot.getProject(input);
		try {
			project.create(null);
		} catch (CoreException e) {}	// No problem if project already exist	
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


		// Create java project
		IJavaProject javaProject = JavaCore.create(project);


		// Set executable output folder
		IFolder binFolder = project.getFolder(properties.getProperty("binFolder"));
		try {
			binFolder.create(false, true, null);
			// If project contents on disk have not been cancelled no need to create a new bin folder
		} catch (CoreException e) {}
		try {
			javaProject.setOutputLocation(binFolder.getFullPath(), null);
		} catch (JavaModelException e) {
			e.printStackTrace();
		}
		
				
		/*
		 * 	Auto-import examName.zip file in project renamed with student's code
		 */
		final String homeDir = System.getProperty("user.home");

		// Reading exam zip file
		ZipFile zipFile = null;
		try {
			zipFile = new ZipFile(new File (new File(homeDir, properties.getProperty("downloadsFolder")), properties.getProperty("examName") + ".zip"));
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		// Import examName.zip in project renamed
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
