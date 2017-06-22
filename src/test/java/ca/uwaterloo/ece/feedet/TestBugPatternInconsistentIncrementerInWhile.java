package ca.uwaterloo.ece.feedet;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.junit.Test;

import ca.uwaterloo.ece.feedet.DetectionRecord;
import ca.uwaterloo.ece.feedet.bugpatterns.InconsistentIncrementerInWhile;
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
public class TestBugPatternInconsistentIncrementerInWhile {
	
	HashSet<DetectionRecord> identifiedPotentialBug = new HashSet<DetectionRecord>();
	
    @Test public void testSomeLibraryMethod() {
    	
    	String projectPathRoot1 = System.getProperty("user.home") + "/Documents/githubProjects/apache"; // "/Volumes/Faith/githubProjects/apache"; //System.getProperty("user.home") + "/Documents/githubProjects/apache";
    	//String projectPathRoot2 = System.getProperty("user.home") + "/Documents/githubProjects/google"; // "/Volumes/Faith/githubProjects/apache"; //System.getProperty("user.home") + "/Documents/githubProjects/apache";

    	int numOfTPs = 0;
    	
    	// TP hbase-server/src/main/java/org/apache/hadoop/hbase/rest/RowSpec.java	e7c1acfecc3aad09db8160906b8f4de346d0f5e7
    	String projectName = "hbase";
    	String gitURI = projectPathRoot1 + File.separator + projectName;
    	String path = "hbase-server/src/main/java/org/apache/hadoop/hbase/rest/RowSpec.java"; //	
    	String shaId = "e7c1acfecc3aad09db8160906b8f4de346d0f5e7~1";
        
    	detect(projectName,gitURI, path, shaId,identifiedPotentialBug);
    	numOfTPs += 2;
    	assertEquals(identifiedPotentialBug.size(),numOfTPs);
    	
    	// FP cassandra       Alive   7d266b9e79e0738df25e4f86b5bdccb030a02d88        src/java/org/apache/cassandra/db/commitlog/CommitLogReplayer.java
    	// 379     while (futures.size() > MAX_OUTSTANDING_REPLAY_COUNT || pendingMutationBytes > MAX_OUTSTANDING_REPLAY_BYTES || (!futures.isEmpty() && futures.peek().isDone())) {
    	projectName = "cassandra";
    	gitURI = projectPathRoot1 + File.separator + projectName;
    	path = "src/java/org/apache/cassandra/db/commitlog/CommitLogReplayer.java"; //	
    	shaId = "7d266b9e79e0738df25e4f86b5bdccb030a02d88";
        
    	detect(projectName,gitURI, path, shaId,identifiedPotentialBug);
    	assertEquals(identifiedPotentialBug.size(),numOfTPs);
    	
    	// FP incubator-joshua        d8c4986e46bf289be2c76a96b7fd584eff0f9e0a        caa8a0c6122f80d44e1846afd80b8c984e3ac1ff        src/joshua/corpus/syntax/ArraySyntaxTree.java
    	// 104     while (!nt_stack.isEmpty() && labels.size() < MAX_LABELS) {
    	projectName = "incubator-joshua";
    	gitURI = projectPathRoot1 + File.separator + projectName;
    	path = "src/joshua/corpus/syntax/ArraySyntaxTree.java"; //	
    	shaId = "caa8a0c6122f80d44e1846afd80b8c984e3ac1ff";
        
    	detect(projectName,gitURI, path, shaId,identifiedPotentialBug);
    	assertEquals(identifiedPotentialBug.size(),numOfTPs);
    	
    	// FP hbase   Alive   b53f354763f96d81ce15d7bded6f1bfd97aee68b        hbase-server/src/main/java/org/apache/hadoop/hbase/util/RegionSplitter.java
    	// 568     while (outstanding.size() >= MAX_OUTSTANDING) {
    	projectName = "hbase";
    	gitURI = projectPathRoot1 + File.separator + projectName;
    	path = "hbase-server/src/main/java/org/apache/hadoop/hbase/util/RegionSplitter.java"; //	
    	shaId = "b53f354763f96d81ce15d7bded6f1bfd97aee68b";
        
    	detect(projectName,gitURI, path, shaId,identifiedPotentialBug);
    	assertEquals(identifiedPotentialBug.size(),numOfTPs);
    	
    	// FP hadoop  Alive   99c2bbd337942e4bc7b246a88dff53f98e530651        hadoop-mapreduce-project/hadoop-mapreduce-client/hadoop-mapreduce-client-hs/src/main/java/org/apache/hadoop/mapreduce/v2/hs/HistoryFileManager.java
    	// 234     while (cache.size() > maxSize && keys.hasNext()) {
    	//  JobId key=keys.next();
    	//  HistoryFileInfo firstValue=cache.get(key);
    	projectName = "hadoop";
    	gitURI = projectPathRoot1 + File.separator + projectName;
    	path = "hadoop-mapreduce-project/hadoop-mapreduce-client/hadoop-mapreduce-client-hs/src/main/java/org/apache/hadoop/mapreduce/v2/hs/HistoryFileManager.java"; //	
    	shaId = "99c2bbd337942e4bc7b246a88dff53f98e530651";
        
    	detect(projectName,gitURI, path, shaId,identifiedPotentialBug);
    	assertEquals(identifiedPotentialBug.size(),numOfTPs);
    	
    	// FP kafka   d9ae33d4c0473ef53a9ea560536467333136c0a0        403158b54b18cabf93eb15d4c4dd8ab66604bf9f        clients/src/main/java/org/apache/kafka/common/security/kerberos/KerberosRule.java
    	// 118     while (start < format.length() && match.find(start)) {
    	projectName = "kafka";
    	gitURI = projectPathRoot1 + File.separator + projectName;
    	path = "clients/src/main/java/org/apache/kafka/common/security/kerberos/KerberosRule.java";
    	shaId = "403158b54b18cabf93eb15d4c4dd8ab66604bf9f";
        
    	detect(projectName,gitURI, path, shaId,identifiedPotentialBug);
    	assertEquals(identifiedPotentialBug.size(),numOfTPs);
    	
    	// FP incubator-asterixdb-hyracks     path_deleted    140614731b4e0a1b06ea2989ecc1e1c655b6a2ad        hyracks-storage-am-btree/src/main/java/edu/uci/ics/hyracks/storage/am/btree/compressors/FieldPrefixCompressor.java
    	// use of a local index would be fine.
    	//128     while (keyPartitions.size() >= numberKeyPartitions) {
    	//  int lastIndex=keyPartitions.size() - 1; 
    	//  KeyPartition kp=keyPartitions.get(lastIndex);
    	/*projectName = "incubator-asterixdb-hyracks";
    	gitURI = projectPathRoot1 + File.separator + projectName;
    	path = "hyracks-storage-am-btree/src/main/java/edu/uci/ics/hyracks/storage/am/btree/compressors/FieldPrefixCompressor.java";
    	shaId = "140614731b4e0a1b06ea2989ecc1e1c655b6a2ad";
        
    	detect(projectName,gitURI, path, shaId,identifiedPotentialBug);
    	assertEquals(identifiedPotentialBug.size(),numOfTPs);*/
    	
    	// FP j2objc  Alive   bb0c824276d0e783516264879eba8c0e0869867e        jre_emul/android/libcore/luni/src/main/java/libcore/icu/NativeIDN.java
    	
    	// 63      while (h < s.length()) {
    	//  int m=Integer.MAX_VALUE;
    	//  for (int i=0; i < s.length(); i++) {
    	//    int c=s.charAt(i);
    	/*projectName = "j2objc";
    	gitURI = projectPathRoot2 + File.separator + projectName;
    	path = "jre_emul/android/libcore/luni/src/main/java/libcore/icu/NativeIDN.java";
    	shaId = "bb0c824276d0e783516264879eba8c0e0869867e";
        
    	detect(projectName,gitURI, path, shaId,identifiedPotentialBug);
    	assertEquals(identifiedPotentialBug.size(),numOfTPs);*/
    }

	private void detect(String prjName, String gitURI, String path, String shaId,HashSet<DetectionRecord> identifiedPotentialBug) {
		try {
    		Git git;
			git = Git.open( new File(gitURI) );
			Repository repo = git.getRepository();
			
			String fileSource=Utils.fetchBlob(repo, shaId, path);
			
			fileSource = Utils.removeComments(fileSource);
			
			JavaASTParser preFixWholeCodeAST = new JavaASTParser(fileSource);

			process(prjName,new InconsistentIncrementerInWhile(prjName,preFixWholeCodeAST,shaId,path,repo).detect());
			
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
						+ detRec.getPath());
				System.out.println(detRec.getLineNum() + " " + detRec.getCode() + "\n");
				System.out.println(detRec.getSurroundCode() + "\n");
			}
		}
		
	}
}
