/*******************************************************************************
 * Copyright (c) 2008 The University of York.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * Contributors:
 *     Louis Rose - initial API and implementation
 ******************************************************************************/

package org.eclipse.epsilon.egl;
// 2/2/2021 ......,, to do ignore the regions in the files, I add new method TokeneRerions.
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;

import org.eclipse.epsilon.common.util.UriUtil;
import org.eclipse.epsilon.egl.exceptions.EglRuntimeException;
import org.eclipse.epsilon.egl.execute.context.IEglContext;
import org.eclipse.epsilon.egl.formatter.NullFormatter;
import org.eclipse.epsilon.egl.incremental.IncrementalitySettings;
import org.eclipse.epsilon.egl.merge.output.LocatedRegion;
import org.eclipse.epsilon.egl.merge.partition.CommentBlockPartitioner;
import org.eclipse.epsilon.egl.output.OutputBuffer;
import org.eclipse.epsilon.egl.patch.Line;
import org.eclipse.epsilon.egl.patch.Patch;
import org.eclipse.epsilon.egl.patch.PatchValidationDiagnostic;
import org.eclipse.epsilon.egl.patch.TextBlock;
import org.eclipse.epsilon.egl.spec.EglTemplateSpecification;
import org.eclipse.epsilon.egl.spec.EglTemplateSpecificationFactory;
import org.eclipse.epsilon.egl.status.ProtectedRegionWarning;
///import org.eclipse.epsilon.egl.sync.FolderSync;


//Very Important== commented today 7 -- to see maybe I do not need it as it does not use
//import org.eclipse.epsilon.egl.diff_match_patch;
//import org.eclipse.epsilon.egl.diff_match_patch.Diff;
//import org.eclipse.epsilon.egl.diff_match_patch.Operation;
import org.eclipse.epsilon.egl.traceability.OutputFile;
import org.eclipse.epsilon.egl.util.FileUtil;

import jlibdiff.Diff3;
import jlibdiff.Hunk3;

// When I change the name of the class MergeTester to MergeTester3 I commented the below line 
//import org.eclipse.epsilon.egl.MergeTester;

// to call the StatusCode class..
import org.eclipse.epsilon.egl.MergingAndConflicts.StatusCode;


public class EglFileGeneratingTemplate extends EglPersistentTemplate {

	private File target;
	private String targetName;
	private OutputFile currentOutputFile;
	private String existingContents;
	private String newContents;
	private String positiveMessage;
	private OutputMode outputMode;

	public static enum OutputMode {
		WRITE, MERGE, APPEND, PATCH;
	}

	// For tests
	protected EglFileGeneratingTemplate(URI path, IEglContext context, URI outputRoot) throws Exception {
		this(new EglTemplateSpecificationFactory(new NullFormatter(), new IncrementalitySettings())
				.fromResource(path.toString(), path), context, outputRoot);
	}

	public EglFileGeneratingTemplate(EglTemplateSpecification spec, IEglContext context, URI outputRoot,
			String outputRootPath) throws Exception {
		super(spec, context, outputRoot, outputRootPath);
	}

	/**
	 * 
	 * @param spec
	 * @param context
	 * @param outputRoot
	 * @throws Exception
	 * @since 1.6
	 */
	public EglFileGeneratingTemplate(EglTemplateSpecification spec, IEglContext context, URI outputRoot)
			throws Exception {
		super(spec, context, outputRoot);
	}

	public File append(String path) throws EglRuntimeException {
		return write(path, OutputMode.APPEND);
	}

	public File patch(String path) throws EglRuntimeException {
		return write(path, OutputMode.PATCH);
	}

	protected File write(String path, OutputMode outputMode) throws EglRuntimeException {
		try {
			final File target = resolveFile(path);

			if (!isProcessed()) {
				process();
			}

			this.target = target;
			this.targetName = name(path);
			this.existingContents = FileUtil.readIfExists(target);
			this.outputMode = outputMode;

			prepareNewContents();
			writeNewContentsIfDifferentFromExistingContents();

			return target;
		} catch (URISyntaxException e) {
			throw new EglRuntimeException("Could not resolve path: " + target, e, module);
		} catch (IOException ex) {
			throw new EglRuntimeException("Could not generate to: " + target, ex, module);
		}
	}

	@Override
	protected void doGenerate(File target, String targetName, boolean overwrite, boolean merge)
			throws EglRuntimeException {
		try {
			this.target = target;
			this.targetName = targetName;
			this.existingContents = FileUtil.readIfExists(target);
			this.outputMode = (merge && target.exists()) ? OutputMode.MERGE : OutputMode.WRITE;

			prepareNewContents();
			writeNewContentsIfDifferentFromExistingContents();

		} catch (URISyntaxException e) {
			throw new EglRuntimeException("Could not resolve path: " + target, e, module);
		} catch (IOException ex) {
			throw new EglRuntimeException("Could not generate to: " + target, ex, module);
		}
	}

	protected void prepareNewContents() throws EglRuntimeException {
		switch (outputMode) {
		case APPEND: {
			newContents = getExistingContents() != null ? getExistingContents() + FileUtil.NEWLINE + getContents()
					: getContents();
			positiveMessage = "Successfully appended to ";
			break;
		}
		
		/**
		 * This MERGE case was modified to return where exactlly line/s have been added
		 * It compares existing contents with new contents. 
		 */
		case MERGE: {
			newContents = merge(getExistingContents());

			// original code was .. positiveMessage = "Protected regions preserved in ";
			positiveMessage = "";
			String[] linesA = OutputBuffer.contentWithoutRegions(newContents).split("\n");
			String[] linesB = OutputBuffer.contentWithoutRegions(getExistingContents()).split("\n");
			for (int a = 0, b = 0, n = 0; a < linesA.length && b < linesB.length; ++a, ++b) {
				if (!linesA[a].equals(linesB[b])) {
					--a;
					++n;
				} else if (n > 0) {
					positiveMessage += n + " line" + (n != 1 ? "s have" : " has") +
						" been added and preserved after line "+ (b - n) + "\n";
					n = 0;
				}
			}
			if (positiveMessage == ""){
				positiveMessage = "Protected regions preserved in ";
			} else
			positiveMessage += "  in ";
			break;
		}
		case WRITE: {
			newContents = getContents();
			positiveMessage = "Successfully wrote to ";
			break;
		}
		case PATCH: {
			positiveMessage = "Successfully patched ";

			TextBlock existingContentsBlock = new TextBlock(getExistingContents().split(System.lineSeparator()));
			Patch patch = new Patch(getContents().split(System.lineSeparator()));
			List<PatchValidationDiagnostic> patchValidationDiagnostics = patch.validate();
			if (!patchValidationDiagnostics.isEmpty()) {
				PatchValidationDiagnostic patchValidationDiagnostic = patchValidationDiagnostics.get(0);
				throw new EglRuntimeException("Invalid patch. Line " + patchValidationDiagnostic.getLine().getNumber()
						+ ": " + patchValidationDiagnostic.getReason(), new IllegalStateException());
			}
			TextBlock newContentsBlock = patch.apply(existingContentsBlock);
			StringBuffer newContentsStringBuffer = new StringBuffer();
			ListIterator<Line> lineIterator = newContentsBlock.getLines().listIterator();
			while (lineIterator.hasNext()) {
				Line line = lineIterator.next();
				newContentsStringBuffer.append(line.getText());
				if (lineIterator.hasNext())
					newContentsStringBuffer.append(System.lineSeparator());
			}
			newContents = newContentsStringBuffer.toString();
			break;
		}
		default:
			throw new EglRuntimeException("Unsupported output mode " + outputMode, new IllegalStateException());
		}
	}

	/**
	 * This method 
	 * 
	 */
	
	protected void writeNewContentsIfDifferentFromExistingContents() throws URISyntaxException, IOException {		
				
		//// New changes
		if (!OutputBuffer.UseHashLines)
		{
			if (existingContents != null)
			{
			    newContents = existingContents;
			}
			write();
			
			addMessage(getPositiveMessage() + getTargetName());
			return;
		}				
		///// Until here
				
		if (isOverwriteUnchangedFiles() || !newContents.equals(existingContents)) {
			if (existingContents != null && !existingContents.equals("")) {
				
				String conflictFilePath = getTarget().getAbsolutePath() + ".conflict";
				File conflictFile = new File(conflictFilePath);
						
				MergingAndConflicts result = MergingAndConflicts.DoMergingAndConflicts(existingContents, newContents);
				
				// check if the conflict file delete
				if (result.getStatus() == StatusCode.ConflictsResolved) {
					if (conflictFile.exists()) {
						conflictFile.delete();
					}
					newContents = result.getNewContents();
					write();
					
					addMessage("Conflict resolved: " + getTarget().getAbsolutePath());
					return;
				}
				
				// check if any original line has been change or modified..
				if (result.getStatus() == StatusCode.OriginalWasModified) {
					
					addMessage("At least one of generated lines has been changed or deleted: " + getTarget().getAbsolutePath());
					return;
				}
					
				// check if conflics found..
				if (result.getStatus() == StatusCode.ConflictsFound) {
					FileUtil.write(conflictFile, result.getConflictContents());
					newContents = result.getNewContents();
					write();
					addMessage("There is a conflict: " + conflictFilePath + "\n Please fix the conflict and rerun the transformation");
  					return;
				}
				
				newContents = result.getNewContents();
			}

			write();
			addMessage(getPositiveMessage() + getTargetName());

		} else {
			addMessage("Content unchanged for " + getTargetName());			
		}
	}
	
	protected boolean isOverwriteUnchangedFiles() {
		return getIncrementalitySettings().isOverwriteUnchangedFiles();
	}

	protected void write() throws IOException, URISyntaxException {
		if (getTarget() != null) {
			FileUtil.write(getTarget(), getNewContents());
		}

		currentOutputFile = getTemplate().addOutputFile(getTargetName(), UriUtil.fileToUri(getTarget()));

		if (getOutputMode() == OutputMode.MERGE) {
			for (LocatedRegion pr : module.getContext().getPartitioner().partition(getNewContents())
					.getLocatedRegions()) {
				getCurrentOutputFile().addProtectedRegion(pr.getId(), pr.isEnabled(), pr.getOffset());
			}
		}
	}

	@Override
	protected void addProtectedRegionWarning(ProtectedRegionWarning warning) {
		super.addProtectedRegionWarning(new ProtectedRegionWarning(warning.getId(), target.getAbsolutePath()));
	}

	public File getTarget() {
		return target;
	}

	public void setTarget(File target) {
		this.target = target;
	}

	public String getTargetName() {
		return targetName;
	}

	public void setTargetName(String targetName) {
		this.targetName = targetName;
	}

	public OutputFile getCurrentOutputFile() {
		return currentOutputFile;
	}

	public void setCurrentOutputFile(OutputFile currentOutputFile) {
		this.currentOutputFile = currentOutputFile;
	}

	public String getExistingContents() {
		return existingContents;
	}

	public void setExistingContents(String existingContents) {
		this.existingContents = existingContents;
	}

	public String getNewContents() {
		return newContents;
	}

	public void setNewContents(String newContents) {
		this.newContents = newContents;
	}

	public String getPositiveMessage() {
		return positiveMessage;
	}

	public void setPositiveMessage(String positiveMessage) {
		this.positiveMessage = positiveMessage;
	}

	public OutputMode getOutputMode() {
		return outputMode;
	}

	public void setOutputMode(OutputMode outputMode) {
		this.outputMode = outputMode;
	}
}