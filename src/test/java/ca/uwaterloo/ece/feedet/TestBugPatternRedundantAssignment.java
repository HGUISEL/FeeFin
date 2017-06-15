package ca.uwaterloo.ece.bicer;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.junit.Test;

import ca.uwaterloo.ece.feedet.DetectionRecord;
import ca.uwaterloo.ece.feedet.bugpatterns.RedundantAssignment;
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
public class TestBugPatternRedundantAssignment {
	
	HashSet<DetectionRecord> identifiedPotentialBug = new HashSet<DetectionRecord>();
	
    @Test public void testSomeLibraryMethod() {
    	
    	String projectPathRoot1 = System.getProperty("user.home") + "/Documents/githubProjects/apache"; // "/Volumes/Faith/githubProjects/apache"; //System.getProperty("user.home") + "/Documents/githubProjects/apache";
    	String projectPathRoot2 = System.getProperty("user.home") + "/Documents/githubProjects/google";
    	
    	int numOfTPs = 0;
    	
    	// TP def910c2508d1f7936b4178344812c4b03c268c6~1	hadoop-hdfs-project/hadoop-hdfs/src/main/java/org/apache/hadoop/hdfs/tools/DelegationTokenFetcher.java
    	String projectName = "hadoop-common";
    	String gitURI = projectPathRoot1 + File.separator + projectName;
    	String path = "hadoop-hdfs-project/hadoop-hdfs/src/main/java/org/apache/hadoop/hdfs/tools/DelegationTokenFetcher.java";
    	String shaId = "def910c2508d1f7936b4178344812c4b03c268c6~1";
 
    	detect(projectName,gitURI, path, shaId,identifiedPotentialBug);
    	numOfTPs += 1;
    	assertEquals(numOfTPs,identifiedPotentialBug.size());
    	
    	// FP
    	projectName = "guava";
    	gitURI = projectPathRoot2 + File.separator + projectName;
    	path = "guava/src/com/google/common/collect/Ordering.java";
    	shaId = "586995d7b6afdb680ef63d58f5b225bb6e09aa25";
 
    	detect(projectName,gitURI, path, shaId,identifiedPotentialBug);
    	assertEquals(numOfTPs,identifiedPotentialBug.size());
    	
    	// FP guava   Alive   b2c7384b371115007567704de13821972cd562e5   guava/src/com/google/common/collect/Sets.java   1425    hash=~~hash;hash+=adjust
        // hash=~~hash;hash+=adjust
    	projectName = "guava";
    	gitURI = projectPathRoot2 + File.separator + projectName;
    	path = "guava/src/com/google/common/collect/Sets.java";
    	shaId = "b2c7384b371115007567704de13821972cd562e5";
 
    	detect(projectName,gitURI, path, shaId,identifiedPotentialBug);
    	assertEquals(numOfTPs,identifiedPotentialBug.size());
    	
    	// FP guava   Alive   5a42baf73ca755366e9aeff5567b2ef70d891852   guava/src/com/google/common/io/MoreFiles.java Q3
    	projectName = "guava";
    	gitURI = projectPathRoot2 + File.separator + projectName;
    	path = "guava/src/com/google/common/io/MoreFiles.java";
    	shaId = "5a42baf73ca755366e9aeff5567b2ef70d891852";
 
    	detect(projectName,gitURI, path, shaId,identifiedPotentialBug);
    	assertEquals(numOfTPs,identifiedPotentialBug.size());
    	
    	// FP RedundantAssignment	guava	Alive	e102fa4a86dad34f375e06c8ce0f52ea264588c3	guava/src/com/google/common/math/DoubleMath.java	247	increment=false;increment=!isPowerOfTwo(x)
    	projectName = "guava";
    	gitURI = projectPathRoot2 + File.separator + projectName;
    	path = "guava/src/com/google/common/math/DoubleMath.java";
    	shaId = "e102fa4a86dad34f375e06c8ce0f52ea264588c3";
 
    	detect(projectName,gitURI, path, shaId,identifiedPotentialBug);
    	assertEquals(numOfTPs,identifiedPotentialBug.size());
    	
    	// FP RedundantAssignment	guava	dcd0f8dda7d2458011c71054d6a40a0f8437125a	f4cace556e08d2576de45f6d85289afcba74f521	guava/src/com/google/common/io/Files.java	802	name[q++]='.';name[q++]='.'
    	projectName = "guava";
    	gitURI = projectPathRoot2 + File.separator + projectName;
    	path = "guava/src/com/google/common/io/Files.java";
    	shaId = "f4cace556e08d2576de45f6d85289afcba74f521";
 
    	detect(projectName,gitURI, path, shaId,identifiedPotentialBug);
    	assertEquals(numOfTPs,identifiedPotentialBug.size());
    	
    	// FP RedundantAssignment	ExoPlayer	Alive	e6778c90a1607da01feeba4d4bbf9a514655ed3c	library/src/main/java/com/google/android/exoplayer2/ExoPlayerImplInternal.java	974	playbackInfo=new PlaybackInfo(0,0);playbackInfo=new PlaybackInfo(0,C.TIME_UNSET)
    	projectName = "ExoPlayer";
    	gitURI = projectPathRoot2 + File.separator + projectName;
    	path = "library/src/main/java/com/google/android/exoplayer2/ExoPlayerImplInternal.java";
    	shaId = "e6778c90a1607da01feeba4d4bbf9a514655ed3c";
 
    	detect(projectName,gitURI, path, shaId,identifiedPotentialBug);
    	assertEquals(numOfTPs,identifiedPotentialBug.size());
    	
    	// FP edundantAssignment	j2objc	Alive	277e7dcb5d90374ca61920b39f6dcc17250d3c03	xalan/third_party/android/platform/external/apache-xml/src/main/java/org/apache/xalan/templates/ElemLiteralResult.java	573	m_count=m_avts.size();m_count=0
    	// m_count=m_avts.size();m_count=0
    	//		if (null != m_avts) m_count=m_avts.size();
    	//		 else m_count=0;
    	projectName = "j2objc";
    	gitURI = projectPathRoot2 + File.separator + projectName;
    	path = "xalan/third_party/android/platform/external/apache-xml/src/main/java/org/apache/xalan/templates/ElemLiteralResult.java";
    	shaId = "277e7dcb5d90374ca61920b39f6dcc17250d3c03";
 
    	detect(projectName,gitURI, path, shaId,identifiedPotentialBug);
    	assertEquals(numOfTPs,identifiedPotentialBug.size());
    	
    	// FP RedundantAssignment	groovy	8f2da5bec79f96e9399961b704218cdf5c1cb083	333084c467c18f2afc9460e58d48c43909ad8d1a	src/main/org/codehaus/groovy/vmplugin/v7/IndyInterface.java	322	ci.handle=MethodHandles.insertArguments(ci.handle,1,ci.methodName);ci.handle=ci.handle.asCollector(Object[].class,ci.targetType.parameterCount() - 1)
    	projectName = "groovy";
    	gitURI = projectPathRoot1 + File.separator + projectName;
    	path = "src/main/org/codehaus/groovy/vmplugin/v7/IndyInterface.java";
    	shaId = "333084c467c18f2afc9460e58d48c43909ad8d1a";
 
    	detect(projectName,gitURI, path, shaId,identifiedPotentialBug);
    	assertEquals(numOfTPs,identifiedPotentialBug.size());
    	
    	// TP groovy d6a9d9f040f6577397be33b8152116961808f339 src/main/org/codehaus/groovy/classgen/AsmClassGenerator.java
    	projectName = "groovy";
    	gitURI = projectPathRoot1 + File.separator + projectName;
    	path = "src/main/org/codehaus/groovy/classgen/AsmClassGenerator.java";
    	shaId = "d6a9d9f040f6577397be33b8152116961808f339";
 
    	detect(projectName,gitURI, path, shaId,identifiedPotentialBug);
    	assertEquals(++numOfTPs,identifiedPotentialBug.size());
    	
    	// FP RedundantAssignment da22ffcba82efa50aa384b3acdf26561085e14f6	lang/java/avro/src/main/java/org/apache/avro/io/BinaryData.java	400	buf[pos + len++]=(byte)((bits >>> 8) & 0xFF);buf[pos + len++]=(byte)((bits >>> 16) & 0xFF)
    	projectName = "avro";
    	gitURI = projectPathRoot1 + File.separator + projectName;
    	path = "lang/java/avro/src/main/java/org/apache/avro/io/BinaryData.java";
    	shaId = "37ce7f72478fcd3dd15637efe25b422f9631bacc";
 
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
			
			process(prjName,new RedundantAssignment(prjName,preFixWholeCodeAST,shaId,path,repo).detect());
			
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