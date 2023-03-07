package org.eclipse.epsilon.egl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.eclipse.jgit.diff.RawText;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.merge.MergeAlgorithm;
import org.eclipse.jgit.merge.MergeFormatter;
import org.eclipse.jgit.merge.MergeResult;

public class MergeTester2 {
	
	MergeFormatter fmt = new MergeFormatter();

	public static void main(String[] args) throws IOException {
		MergeTester2 tj = new MergeTester2();
		String originalGen = readFile("/Users/sultanalmutairi/git/EglSync/org.eclipse.epsilon.egl.sync/University-Last-Project/gen/OriginalOutput.java", StandardCharsets.UTF_8);
		String originalGenModified = readFile("/Users/sultanalmutairi/git/EglSync/org.eclipse.epsilon.egl.sync/University-Last-Project/gen/OriginalOutputModifid.java", StandardCharsets.UTF_8);
		String newGen = readFile("/Users/sultanalmutairi/git/EglSync/org.eclipse.epsilon.egl.sync/University-Last-Project/gen/NewOutput.java", StandardCharsets.UTF_8);

		System.out.println(tj.merge(originalGen, originalGenModified, newGen));
	}

	public String merge(String commonBase, String ours, String theirs) throws IOException {
		MergeAlgorithm ma = new MergeAlgorithm();
		// RawTextComparator.WS_IGNORE_ALL, 
		MergeResult r = ma.merge(RawTextComparator.DEFAULT, T(commonBase), T(ours), T(theirs));
		ByteArrayOutputStream bo = new ByteArrayOutputStream(50);
		fmt.formatMerge(bo, r, "B", "O", "T", Constants.CHARACTER_ENCODING);
		return new String(bo.toByteArray(), Constants.CHARACTER_ENCODING);
	}
	
//	public static String t(String text) {
//		StringBuilder r = new StringBuilder();
//		for (int i = 0; i < text.length(); i++) {
//			char c = text.charAt(i);
//			switch (c) {
//			case '<':
//				r.append("<<<<<<< O\n");
//				break;
//			case '=':
//				r.append("=======\n");
//				break;
//			case '>':
//				r.append(">>>>>>> T\n");
//				break;
//			default:
//				r.append(c);
//			}
//		}
//		return r.toString();
//	}

	public static RawText T(String text) {
		//return new RawText(Constants.encode(t(text)));
		return new RawText(Constants.encode(text));
	}
	
	// Code taken from here: https://stackoverflow.com/questions/326390/how-do-i-create-a-java-string-from-the-contents-of-a-file
	static String readFile(String path, Charset encoding)
			  throws IOException
			{
			  byte[] encoded = Files.readAllBytes(Paths.get(path));
			  return new String(encoded, encoding);
			}
}