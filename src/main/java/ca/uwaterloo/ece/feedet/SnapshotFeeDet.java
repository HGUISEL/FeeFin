package ca.uwaterloo.ece.feedet;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.lib.Repository;

import ca.uwaterloo.ece.feedet.bugpatterns.EqualToSameExpression;
import ca.uwaterloo.ece.feedet.bugpatterns.IllogicalCondition;
import ca.uwaterloo.ece.feedet.bugpatterns.IncorrectDirectorySlash;
import ca.uwaterloo.ece.feedet.bugpatterns.IncorrectMapIterator;
import ca.uwaterloo.ece.feedet.bugpatterns.RedundantException;
import ca.uwaterloo.ece.feedet.bugpatterns.RedundantInstantiation;
import ca.uwaterloo.ece.feedet.bugpatterns.SameObjEquals;
import ca.uwaterloo.ece.feedet.bugpatterns.WrongIncrementer;
import ca.uwaterloo.ece.feedet.utils.JavaASTParser;
import ca.uwaterloo.ece.feedet.utils.Utils;

public class SnapshotFeeDet {

	public static void main(String[] args) {
		new SnapshotFeeDet().run(args); 
	}

	private void run(String[] args) {
		String rootPath = args[0];
		
		File file = new File(rootPath);
		String[] projects = file.list(new FilenameFilter() {
			@Override
			public boolean accept(File current, String name) {
				return new File(current, name).isDirectory();
			}
		});

		int i=1;
		for(String project:projects){

			System.out.println(i++ + " ==========");
			System.out.println(project);
			System.out.println("==========");


			String targetDirPath = rootPath + File.separator + project;
			
			String[] extensions = {"java"};
			Collection<File> list = FileUtils.listFiles(new File(targetDirPath), extensions, true);
			
			System.out.println("# of all paths: " + list.size());
			for(File path:list){

				// ignore when no previous revision of a file, Test files, and non-java files.
				if(path.getPath().indexOf("Test")>=0  || Utils.isWordInStatement("test", path.getPath()) || Utils.isWordInStatement("tests", path.getPath())) continue;

				// ignore all files under test directory
				if(path.getPath().indexOf("/test")>=0) continue;
				
				detect(project, null, path.getPath(), "");

			}
		}
	}
	
	private void detect(String project, Repository repo, String path, String shaId) {
		try {
			String fileSource=new String(Files.readAllBytes(FileSystems.getDefault().getPath(path)));//;Utils.fetchBlob(repo, shaId, path);

			if(fileSource.split("\n").length >10000){ // skip a huge file
				return;
			}

			fileSource = Utils.removeComments(fileSource);

			JavaASTParser preFixWholeCodeAST = new JavaASTParser(fileSource);
			
			process(project,new RedundantException(project,preFixWholeCodeAST,shaId,path,repo).detect());
			//new RedundantCondition(preFixWholeCodeAST,shaId,path,repo,identifiedPotentialBug).detect(); // not new pattern
			process(project,new RedundantInstantiation(project,preFixWholeCodeAST,shaId,path,repo).detect());
			process(project,new IllogicalCondition(project,preFixWholeCodeAST,shaId,path,repo).detect());
			//process(project,new IllogicalConditionNPE(project,preFixWholeCodeAST,shaId,path,repo).detect());
			process(project,new IncorrectMapIterator(project,preFixWholeCodeAST,shaId,path,repo).detect());
			//process(project,new MissingThrow(project,preFixWholeCodeAST,shaId,path,repo).detect());
			
			//process(project,new MissingTimeResolution(project,preFixWholeCodeAST,shaId,path,repo).detect());
			process(project,new WrongIncrementer(project,preFixWholeCodeAST,shaId,path,repo).detect());
			process(project,new SameObjEquals(project,preFixWholeCodeAST,shaId,path,repo).detect());
			process(project,new EqualToSameExpression(project,preFixWholeCodeAST,shaId,path,repo).detect());
			process(project,new IncorrectDirectorySlash(project,preFixWholeCodeAST,shaId,path,repo).detect());

		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}

	private void process(String projectName, ArrayList<DetectionRecord> detectionRecords) {
		for(DetectionRecord detRec:detectionRecords){

			System.out.println("###\t" + detRec.getPatternName() + "\t" 
					+ projectName + "\t"
					+ detRec.getRevID() + "\t"
					+ detRec.getPath() + "\t" 
					+ detRec.getLineNum() + "\t" 
					+ detRec.getCode()
			);		
			System.out.println(detRec.getCode());
			System.out.println(detRec.getSurroundCode());
		}
	}

}
