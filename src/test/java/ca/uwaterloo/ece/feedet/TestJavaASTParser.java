package ca.uwaterloo.ece.bicer;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.junit.Test;

import ca.uwaterloo.ece.feedet.utils.JavaASTParser;
import ca.uwaterloo.ece.feedet.utils.Utils;



public class TestJavaASTParser {

	@Test
	public void test() {
		
		Git git;
		try {
			git = Git.open( new File( System.getProperty("user.home") + "/git/BICER") );
			Repository repo = git.getRepository();
			JavaASTParser jParser = new JavaASTParser(
					Utils.fetchBlob(repo, "3106c5dacea87cc17590d11bfd72cae96642dbe1", "src/main/java/ca/uwaterloo/ece/bicer/utils/Utils.java"));
		
			JavaASTParser jParser2 = new JavaASTParser(
					Utils.fetchBlob(repo, "3106c5dacea87cc17590d11bfd72cae96642dbe1", "src/main/java/ca/uwaterloo/ece/bicer/utils/Utils.java"));
		
			assertFalse(jParser.getMethodDeclarations().get(1).equals(jParser2.getMethodDeclarations().get(1)));
			assertTrue(jParser.getMethodDeclarations().get(1).toString().equals(jParser2.getMethodDeclarations().get(1).toString()));
			
			assertFalse(jParser.getMethodDeclarations().get(1).getName().equals(jParser2.getMethodDeclarations().get(1).getName()));
			assertTrue(jParser.getMethodDeclarations().get(1).getName().toString().equals(jParser2.getMethodDeclarations().get(1).getName().toString()));
			
			assertFalse(jParser.getMethodDeclarations().get(1).getBody().equals(jParser2.getMethodDeclarations().get(1).getBody()));
			assertTrue(jParser.getMethodDeclarations().get(1).getBody().toString().equals(jParser2.getMethodDeclarations().get(1).getBody().toString()));
			
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
	}

}
