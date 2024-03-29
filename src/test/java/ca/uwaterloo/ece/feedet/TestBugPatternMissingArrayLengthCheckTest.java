package ca.uwaterloo.ece.feedet;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.junit.Test;

import ca.uwaterloo.ece.feedet.DetectionRecord;
import ca.uwaterloo.ece.feedet.bugpatterns.declined.MissingArrayLengthCheck;
import ca.uwaterloo.ece.feedet.utils.JavaASTParser;
import ca.uwaterloo.ece.feedet.utils.Utils;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;

/*
 * This Java source file was auto generated by running 'gradle init --type java-library'
 * by 'j22nam' at '27/05/16 5:45 PM' with Gradle 2.13
 *
 * @author j22nam, @date 27/05/16 5:45 PM
 */
public class TestBugPatternMissingArrayLengthCheckTest {
	
	HashSet<DetectionRecord> identifiedPotentialBug = new HashSet<DetectionRecord>();
	
    @Test public void testSomeLibraryMethod() {
    	
    	String projectPathRoot1 = System.getProperty("user.home") + "/Documents/githubProjects/apache"; // "/Volumes/Faith/githubProjects/apache"; //System.getProperty("user.home") + "/Documents/githubProjects/apache";
    	
    	int numOfTPs = 0;
    	
    	// TP Equal to the same expression
    	String projectName = "hadoop-common";
    	String gitURI = projectPathRoot1 + File.separator + projectName;
    	String path = "hadoop-hdfs-project/hadoop-hdfs/src/main/java/org/apache/hadoop/hdfs/server/namenode/FSPermissionChecker.java";
    	String shaId = "d3a6024f2e83e702c3e13faa9fd88187f39766d3~1";
 
    	detect(projectName,gitURI, path, shaId,identifiedPotentialBug);
    	assertEquals(++numOfTPs,identifiedPotentialBug.size());
    	
    	// TP Equal to the same expression
    	projectName = "hadoop-common";
    	gitURI = projectPathRoot1 + File.separator + projectName;
    	path = "hadoop-hdfs-project/hadoop-hdfs/src/main/java/org/apache/hadoop/hdfs/server/namenode/FSPermissionChecker.java";
    	shaId = "d3a6024f2e83e702c3e13faa9fd88187f39766d3";
 
    	detect(projectName,gitURI, path, shaId,identifiedPotentialBug);
    	assertEquals(numOfTPs,identifiedPotentialBug.size());
    	
    	// FP tomee   Alive   f1eacc39db8606b1fef2e8761243d9ded59158a5        container/openejb-core/src/main/java/org/apache/openejb/util/proxy/QueryProxy.java
    	projectName = "tomee";
    	gitURI = projectPathRoot1 + File.separator + projectName;
    	path = "container/openejb-core/src/main/java/org/apache/openejb/util/proxy/QueryProxy.java";
    	shaId = "17c277f54885ecfaacdd1d2cebc01679c87ad6a1";
    	
    	detect(projectName,gitURI, path, shaId,identifiedPotentialBug);
    	assertEquals(numOfTPs,identifiedPotentialBug.size());
    	
    	// FP tomee   Alive   f1eacc39db8606b1fef2e8761243d9ded59158a5        tomee/tomee-webapp/src/main/java/org/apache/tomee/webapp/helper/service/JndiHelperImpl.java     198
    	projectName = "tomee";
    	gitURI = projectPathRoot1 + File.separator + projectName;
    	path = "tomee/tomee-webapp/src/main/java/org/apache/tomee/webapp/helper/service/JndiHelperImpl.java";
    	shaId = "4d3df57db8070f56576ec4d619265890470fc847";
    	
    	detect(projectName,gitURI, path, shaId,identifiedPotentialBug);
    	assertEquals(numOfTPs,identifiedPotentialBug.size());
    }

	private void detect(String prjName, String gitURI, String path, String shaId,HashSet<DetectionRecord> identifiedPotentialBug) {
		try {
    		Git git;
			git = Git.open( new File(gitURI) );
			Repository repo = git.getRepository();
			
			String fileSource=Utils.fetchBlob(repo, shaId, path);
			
			fileSource = Utils.removeComments(fileSource);
			
			JavaASTParser preFixWholeCodeAST = new JavaASTParser(fileSource);
			
			process(prjName,new MissingArrayLengthCheck(prjName,preFixWholeCodeAST,shaId,path,repo).detect());
			
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}

	private void process(String projectName,ArrayList<DetectionRecord> detectionRecords) {
		for(DetectionRecord detRec:detectionRecords){
			if(!identifiedPotentialBug.contains(detRec)){
				identifiedPotentialBug.add(detRec);
				System.out.println(detRec.getPatternName() + "\t" 
						+ projectName + "\t"
						+ detRec.getRevID() + "\t"
						+ detRec.getLineNum() + "\t"
						+ detRec.getPath());
				System.out.println(detRec.getCode() + "\n");
			}
		}
		
	}
}
