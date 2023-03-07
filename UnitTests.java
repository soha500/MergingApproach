package org.eclipse.epsilon.egl;

import static org.junit.Assert.assertEquals;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import org.eclipse.epsilon.egl.MergingAndConflicts.StatusCode;
import org.eclipse.epsilon.emc.emf.EmfModel;
import org.eclipse.epsilon.eol.IEolModule;
import org.eclipse.epsilon.eol.exceptions.EolRuntimeException;
import org.eclipse.epsilon.eol.exceptions.models.EolModelLoadingException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;


/*
In 1 scenario, One line has been added into the auto-generatd lines and preserved without using protected region markers.
In 2 scenario, at least one of the auto-generated lines has been modified.
In 3 scenario, at least one of the auto-generated lines has been deleted.
In 4 scenario, when the size of the protected region can be extend without any problem.
In 5 scenario, in the same position of the template and its generated file two similar values has been added after the first run (there is no conflicts in this case).
In 6 scenario, in the same position of the template and its generated file two different values have been added after the first run (there is conflicts).
In 7 scenario, in the same position of the template and its generated file more then two different values have been added manually after the first run (there isconflict). 
In 8 scenario, when the hashe line was modified  
*/

public class UnitTests {
	
	private static final String FOLDER_PATH = "/Users/sultanalmutairi/git/EglEngine/org.eclipse.epsilon.egl.engine/CorrectnessTests/GeneratedFilesFormUniversityExample";

	EmfModel model;
	EmfModel tempModel;
	static List<String> orginalNewLines;
	
	@Rule
	// Temporary Folder for the model and the template..
	public TemporaryFolder tempFolderModel = new TemporaryFolder();
	public TemporaryFolder tempFolderEglTemplate = new TemporaryFolder();

//	@Before
	public void init() throws IOException {
		
		File orginalFile = new File("/Users/sultanalmutairi/git/EglEngine/org.eclipse.epsilon.egl.engine/CorrectnessTests/ModelUniversity/University.model");
		
		File tempFile = tempFolderModel.newFile("tempUni.model");
		try {
			Files.copy(orginalFile.toPath(), tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			e.printStackTrace();
		}
		tempModel = new EmfModel();
		tempModel.setName("M");
		tempModel.setMetamodelFile(new File("/Users/sultanalmutairi/git/EglEngine/org.eclipse.epsilon.egl.engine/CorrectnessTests/ModelUniversity/University.ecore").getAbsolutePath());
		tempModel.setModelFile(tempFile.getAbsolutePath());
		tempModel.setReadOnLoad(true);
		try {
			tempModel.load();
		} catch (EolModelLoadingException e2) {
			e2.printStackTrace();
		}
		tempFile.deleteOnExit();

		File orginalFileTemplate = new File("/Users/sultanalmutairi/git/EglEngine/org.eclipse.epsilon.egl.engine/CorrectnessTests/ModelUniversity/Module2Java.egl");
		File tempFileTemplate = tempFolderEglTemplate.newFile("TempTemplate.egl");
		try {
			Files.copy(orginalFileTemplate.toPath(), tempFileTemplate.toPath(), StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			e.printStackTrace();
		}
		tempFileTemplate.deleteOnExit();
	}
	

	/**
	 * This method reads the content of each file under the folder,
	 * and returns a list of lines.
	 * 
	 * @param a folder path.
	 * @return a list of lines.
	 */
	
	private static List<String> readTestFileLines (String path) throws IOException {
		String pathString = FOLDER_PATH + path;
		BufferedReader original = new BufferedReader(new FileReader(pathString));		
		List<String> lines = new LinkedList<String>();
		String line;
		while ((line = original.readLine()) != null)
			lines.add(line);
		return lines;
	}
	
	/*
	 * Scenario 1, one line has been added into the auto-generated lines 
	 */
	
	@Test
	public void doLineAddTest() throws IOException {
		List<String> lines = readTestFileLines("/Test1/MDE.java");
		String oldContents = String.join("\n", lines);
		lines.add(1, "Hi");
		MergingAndConflicts results = MergingAndConflicts.DoMergingAndConflicts(String.join("\n", lines), oldContents);
		assertEquals(
			"test 1 - new line should merge successful",
            StatusCode.MergedSuccessfully.name(),
            results.getStatus().name());
	}

	/*
	 * Scenario 2, at least one of the auto-generated lines has been modified.
	 */
	
	@Test
	public void doLineModifyTest() throws IOException {
		List<String> lines = readTestFileLines("/Test2/MDE.java");
		lines.set(0, "change first line");
		MergingAndConflicts results = MergingAndConflicts.CheckContents(String.join("\n", lines));
		assertEquals(
	        "test 2 - modify auto-generated line should be detected",
	        StatusCode.OriginalWasModified.name(),
	        results.getStatus().name());
	}
	
	/*
	 * Scenario 3, at least one of the auto-generated lines has been deleted.
	 */
	
	@Test
	public void doLineDeleteTest() throws IOException {
		List<String> lines = readTestFileLines("/Test3/MDE.java");
		lines.remove(0);
		MergingAndConflicts results = MergingAndConflicts.CheckContents(String.join("\n", lines));
		assertEquals(
	        "test 3 - delete auto-generated should be detected",
	        StatusCode.OriginalWasModified.name(),
	        results.getStatus().name());
	}
	
   /*
	* Scenario 4, the size of the protected region can be extend without any problem.
	*/
	
	@Test
	public void doLineAddToRegionsTest() throws IOException {
		List<String> lines = readTestFileLines("/Test4/MDE.java");
		String oldContents = String.join("\n", lines);
		lines.add(5, "new line");
		lines.add(6, "new line1");
		lines.add(7, "new line2");
		lines.add(8, "new line3");
		MergingAndConflicts results = MergingAndConflicts.DoMergingAndConflicts(String.join("\n", lines), oldContents);
		assertEquals(
			"test 4 - lines that added into protected region should not break the merging",
            StatusCode.MergedSuccessfully.name(),
            results.getStatus().name());
	}
	/*
	 * Scenario 5, in the same position of the template and it's generated file two similar values has been added after the first run (there's no conflicts in this case)
	 */
	
	@Test
	public void doLineNonConflictTest() throws IOException {
		List<String> existingLines = readTestFileLines("/Test5/MDE.java");
	    List<String> transformationLines = new ArrayList<>(existingLines);
		existingLines.add(2, "Hi");
		transformationLines.add(2, "Hi");
		MergingAndConflicts results =
			MergingAndConflicts.DoMergingAndConflicts(String.join("\n", existingLines), String.join("\n", transformationLines));
		assertEquals(
			"test 5 - equal values should be merged without any problem",
            StatusCode.MergedSuccessfully.name(),
            results.getStatus().name());
	}

	/*
	 * Scenario 6, in the same position of the template and it's generated file two different values has been added after the first run (there's a conflicts in this case)
	 */
	
	@Test
	public void doLineConflictTest() throws IOException {
		List<String> existingLines = readTestFileLines("/Test6/MDE.java");
	    List<String> transformationLines = new ArrayList<>(existingLines);
		existingLines.add(2, "Hi");
		transformationLines.add(2, "Hello");
		MergingAndConflicts results =
			MergingAndConflicts.DoMergingAndConflicts(String.join("\n", existingLines), String.join("\n", transformationLines));
		assertEquals(
			"test 6 - merge conflict should be detected",
            StatusCode.ConflictsFound.name(),
            results.getStatus().name());
	}
	
	/*
	 * Scenario 7, in the same position of the template and it's generated file two different values has been added after the first run (there's a conflicts in this case)
	 */
	
	@Test
	public void doLinesConflictTest() throws IOException {
		List<String> existingLines = readTestFileLines("/Test7/MDE.java");
	    List<String> transformationLines = new ArrayList<>(existingLines);
		existingLines.add(2, "Welcome");
		transformationLines.add(2, "Bye");
		existingLines.add(9, "Hi");
		transformationLines.add(9, "Hello");
		MergingAndConflicts results =
			MergingAndConflicts.DoMergingAndConflicts(String.join("\n", existingLines), String.join("\n", transformationLines));
		assertEquals(
			"test 7 - merge conflict should be detected",
            StatusCode.ConflictsFound.name(),
            results.getStatus().name());
	}
	
	/*
	 * Scenario 8, hashes were modified
	 */
	
	@Test
	public void doHashesLineModifyTest() throws IOException {
		List<String> lines = readTestFileLines("/Test8/MDE.java");
		lines.set(12, "cJvAA=InxCQ=87tCQ=AZQfQ=NNN");
		MergingAndConflicts results = MergingAndConflicts.CheckContents(String.join("\n", lines));
		assertEquals(
	        "test 8 - modify hashes line should be detected",
	        StatusCode.OriginalWasModified.name(),
	        results.getStatus().name());
	}	
}