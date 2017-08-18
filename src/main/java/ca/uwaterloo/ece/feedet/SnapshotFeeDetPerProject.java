package ca.uwaterloo.ece.feedet;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.lib.Repository;

import ca.uwaterloo.ece.feedet.bugpatterns.Bug;
import ca.uwaterloo.ece.feedet.bugpatterns.CompareSameValueFromGetterAndField;
import ca.uwaterloo.ece.feedet.bugpatterns.EqualToSameExpression;
import ca.uwaterloo.ece.feedet.bugpatterns.IllogicalCondition;
import ca.uwaterloo.ece.feedet.bugpatterns.IllogicalConditionNPE;
import ca.uwaterloo.ece.feedet.bugpatterns.InconsistentIncrementerInWhile;
import ca.uwaterloo.ece.feedet.bugpatterns.IncorrectDirectorySlash;
import ca.uwaterloo.ece.feedet.bugpatterns.IncorrectMapIterator;
import ca.uwaterloo.ece.feedet.bugpatterns.IncorrectStringCompare;
import ca.uwaterloo.ece.feedet.bugpatterns.IntOverflowOfMathMin;
import ca.uwaterloo.ece.feedet.bugpatterns.MissingCurrentObjRefThis;
import ca.uwaterloo.ece.feedet.bugpatterns.MissingLForLong;
import ca.uwaterloo.ece.feedet.bugpatterns.MissingLongCast;
import ca.uwaterloo.ece.feedet.bugpatterns.MissingThrow;
import ca.uwaterloo.ece.feedet.bugpatterns.RedundantAssignment;
import ca.uwaterloo.ece.feedet.bugpatterns.RedundantCondition;
import ca.uwaterloo.ece.feedet.bugpatterns.RedundantException;
import ca.uwaterloo.ece.feedet.bugpatterns.RedundantInstantiation;
import ca.uwaterloo.ece.feedet.bugpatterns.SameObjEquals;
import ca.uwaterloo.ece.feedet.bugpatterns.SleepWithNegativeValue;
import ca.uwaterloo.ece.feedet.bugpatterns.WrongClassLogName;
import ca.uwaterloo.ece.feedet.bugpatterns.WrongIncrementer;
import ca.uwaterloo.ece.feedet.bugpatterns.WrongLogicForNullChecker;
import ca.uwaterloo.ece.feedet.bugpatterns.WrongReturnObjectInGetter;
import ca.uwaterloo.ece.feedet.bugpatterns.WrongReturnType;
import ca.uwaterloo.ece.feedet.utils.JavaASTParser;
import ca.uwaterloo.ece.feedet.utils.Utils;

public class SnapshotFeeDetPerProject {

	public static void main(String[] args) {
		new SnapshotFeeDetPerProject().run(args); 
	}

	private void run(String[] args) {
		String rootPath = args[0];
		String project = rootPath;
		
		String pattern = args.length == 2? args[1]:"";
		
		//File file = new File(rootPath);
		/*String[] projects = file.list(new FilenameFilter() {
			@Override
			public boolean accept(File current, String name) {
				return new File(current, name).isDirectory();
			}
		});*/

		//for(String project:projects){

			System.out.println(" ==========");
			System.out.println(rootPath);
			System.out.println("==========");


			String targetDirPath = rootPath;
			
			String[] extensions = {"java"};
			Collection<File> list = FileUtils.listFiles(new File(targetDirPath), extensions, true);
			
			System.out.println("# of all paths: " + list.size());
			for(File path:list){

				// ignore when no previous revision of a file, Test files, and non-java files.
				if(path.getPath().indexOf("Test")>=0  || Utils.isWordInStatement("test", path.getPath()) || Utils.isWordInStatement("tests", path.getPath())) continue;

				// ignore all files under test directory
				if(path.getPath().indexOf("/test")>=0) continue;
				
				detect(project, null, path.getPath(), "",pattern);

			}
		//}
	}
	
	private void detect(String project, Repository repo, String path, String shaId, String patternName) {
		try {
			String fileSource=new String(Files.readAllBytes(FileSystems.getDefault().getPath(path)));//;Utils.fetchBlob(repo, shaId, path);

			if(fileSource.split("\n").length >10000){ // skip a huge file
				return;
			}

			fileSource = Utils.removeComments(fileSource);

			JavaASTParser preFixWholeCodeAST = new JavaASTParser(fileSource);
			
			if(!patternName.isEmpty()){
				Class<?> bugPatternClass = Class.forName("ca.uwaterloo.ece.feedet.bugpatterns." + patternName);
				Constructor<?> constructor = bugPatternClass.getConstructor(String.class, JavaASTParser.class,String.class,String.class,Repository.class);
				process(project,((Bug)constructor.newInstance(project,preFixWholeCodeAST,shaId,path,repo)).detect());
			}else{
				process(project,new CompareSameValueFromGetterAndField(project,preFixWholeCodeAST,shaId,path,repo).detect());
				process(project,new EqualToSameExpression(project,preFixWholeCodeAST,shaId,path,repo).detect());
				process(project,new IllogicalCondition(project,preFixWholeCodeAST,shaId,path,repo).detect());
				process(project,new IllogicalConditionNPE(project,preFixWholeCodeAST,shaId,path,repo).detect());
				process(project,new InconsistentIncrementerInWhile(project,preFixWholeCodeAST,shaId,path,repo).detect());
				process(project,new IncorrectDirectorySlash(project,preFixWholeCodeAST,shaId,path,repo).detect());
				process(project,new IncorrectMapIterator(project,preFixWholeCodeAST,shaId,path,repo).detect());
				process(project,new IncorrectStringCompare(project,preFixWholeCodeAST,shaId,path,repo).detect());
				process(project,new IntOverflowOfMathMin(project,preFixWholeCodeAST,shaId,path,repo).detect());
				
				process(project,new MissingCurrentObjRefThis(project,preFixWholeCodeAST,shaId,path,repo).detect());
				process(project,new MissingLForLong(project,preFixWholeCodeAST,shaId,path,repo).detect());
				process(project,new MissingLongCast(project,preFixWholeCodeAST,shaId,path,repo).detect());
				process(project,new MissingThrow(project,preFixWholeCodeAST,shaId,path,repo).detect());
				
				process(project,new RedundantAssignment(project,preFixWholeCodeAST,shaId,path,repo).detect());
				process(project,new RedundantCondition(project,preFixWholeCodeAST,shaId,path,repo).detect()); // not new pattern
				process(project,new RedundantException(project,preFixWholeCodeAST,shaId,path,repo).detect());
				process(project,new RedundantInstantiation(project,preFixWholeCodeAST,shaId,path,repo).detect());
				
				process(project,new SameObjEquals(project,preFixWholeCodeAST,shaId,path,repo).detect());
				process(project,new SleepWithNegativeValue(project,preFixWholeCodeAST,shaId,path,repo).detect());
				
				//process(project,new MissingTimeResolution(project,preFixWholeCodeAST,shaId,path,repo).detect());
				process(project,new WrongClassLogName(project,preFixWholeCodeAST,shaId,path,repo).detect());
				process(project,new WrongIncrementer(project,preFixWholeCodeAST,shaId,path,repo).detect());
				process(project,new WrongLogicForNullChecker(project,preFixWholeCodeAST,shaId,path,repo).detect());
				// process(project,new WrongPositionOfNullChecker(project,preFixWholeCodeAST,shaId,path,repo).detect()); // working on, so commented
				process(project,new WrongReturnObjectInGetter(project,preFixWholeCodeAST,shaId,path,repo).detect());
				process(project,new WrongReturnType(project,preFixWholeCodeAST,shaId,path,repo).detect());
			}

		} catch (IOException e1) {
			e1.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
