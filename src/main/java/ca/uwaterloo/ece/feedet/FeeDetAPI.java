package ca.uwaterloo.ece.feedet;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.lib.Repository;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;

import ca.uwaterloo.ece.feedet.bugpatterns.Bug;
import ca.uwaterloo.ece.feedet.utils.JavaASTParser;
import ca.uwaterloo.ece.feedet.utils.Utils;

public class FeeDetAPI {

	@SuppressWarnings("rawtypes")
	static private ArrayList<String> getPatternClassNames(){
		ArrayList<String> patternClassNames = new ArrayList<String>();

		if(patternClassNames.size()==0){
			Reflections reflections = new Reflections("ca.uwaterloo.ece.feedet.bugpatterns",new SubTypesScanner(false));

			Set<Class<? extends Object>> allClasses = 
					reflections.getSubTypesOf(Object.class);

			for(Class aClass:allClasses){

				String fullyQualifiedClassName = aClass.getName();
				//TODO update the list for bug patterns to be ignored as some patterns are working on or declined.
				if(fullyQualifiedClassName.equals("ca.uwaterloo.ece.feedet.bugpatterns.Bug")
						|| fullyQualifiedClassName.equals("ca.uwaterloo.ece.feedet.bugpatterns.BugPatternTemplate")
						|| fullyQualifiedClassName.equals("ca.uwaterloo.ece.feedet.bugpatterns.InconsistentReturnType") // TODO remove when the pattern implementation is done.
						|| fullyQualifiedClassName.startsWith("ca.uwaterloo.ece.feedet.bugpatterns.declined")
						)
					continue;

				if(!patternClassNames.contains(fullyQualifiedClassName)){ 
					patternClassNames.add(fullyQualifiedClassName);
				}
			}
		}

		return patternClassNames;		
	}

	static public ArrayList<DetectionRecord> detectEntireProject(String projectPath) {

		ArrayList<DetectionRecord> detRecords = new ArrayList<DetectionRecord>();

		String[] extensions = {"java"};
		Collection<File> list = FileUtils.listFiles(new File(projectPath), extensions, true);
		
		ArrayList<String> patternClassNames = getPatternClassNames();

		for(File path:list){
			// ignore when no previous revision of a file, Test files, and non-java files.
			if(path.getPath().indexOf("Test")>=0  || Utils.isWordInStatement("test", path.getPath()) || Utils.isWordInStatement("tests", path.getPath())) continue;

			// ignore all files under test directory
			if(path.getPath().indexOf("/test")>=0) continue;
			
			detRecords.addAll(detectBugsInAFileForAllBugPatterns(path.getPath(),patternClassNames));
		}

		return detRecords;
	}
	
	public static ArrayList<DetectionRecord> detectBugsInAFileForAllBugPatterns(String filePath) {
		ArrayList<DetectionRecord> detRecords = new ArrayList<DetectionRecord>();
		
		ArrayList<String> patternClassNames = getPatternClassNames();
		
		for(String patternClassName:patternClassNames){
			detRecords.addAll(detectBugsInAFileForASpecifiedBugPattern(filePath,patternClassName));
		}
		
		return detRecords;
	}

	public static ArrayList<DetectionRecord> detectBugsInAFileForAllBugPatterns(String filePath, ArrayList<String> patternClassNames) {
		ArrayList<DetectionRecord> detRecords = new ArrayList<DetectionRecord>();
		
		for(String patternClassName:patternClassNames){
			detRecords.addAll(detectBugsInAFileForASpecifiedBugPattern(filePath,patternClassName));
		}
		
		return detRecords;
	}

	static public ArrayList<DetectionRecord> detectBugsInAFileForASpecifiedBugPattern(String filePath,String patternClassName) {

		ArrayList<DetectionRecord> detRecords = new ArrayList<DetectionRecord>();

		try {
			String fileSource=new String(Files.readAllBytes(FileSystems.getDefault().getPath(filePath)));

			if(fileSource.split("\n").length >10000){ // skip a huge file
				return detRecords;
			}

			fileSource = Utils.removeComments(fileSource);

			JavaASTParser preFixWholeCodeAST = new JavaASTParser(fileSource);
			
			Class<?> bugPatternClass = Class.forName(patternClassName);
			Constructor<?> constructor = bugPatternClass.getConstructor(String.class, JavaASTParser.class,String.class,String.class,Repository.class);
			detRecords.addAll(((Bug)constructor.newInstance("",preFixWholeCodeAST,"",filePath,null)).detect());

		} catch (IOException e1) {
			e1.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}

		return detRecords;
	}

}
