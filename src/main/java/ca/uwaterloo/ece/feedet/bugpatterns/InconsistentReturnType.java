package ca.uwaterloo.ece.feedet.bugpatterns;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.WhileStatement;
import org.eclipse.jgit.lib.Repository;

import ca.uwaterloo.ece.feedet.DetectionRecord;
import ca.uwaterloo.ece.feedet.utils.JavaASTParser;

public class InconsistentReturnType extends Bug {
	/*
	 *  public long getMemStoreFlushSize() {
     	byte [] value = getValue(MEMSTORE_FLUSHSIZE_KEY);
        if (value != null)
-           return Integer.valueOf(Bytes.toString(value)).intValue();
+           return Long.valueOf(Bytes.toString(value)).longValue();
	 */
	public InconsistentReturnType(String prjName,JavaASTParser ast, String id, String path, Repository repo){
		initialize(prjName,ast,id,path,repo,this.getClass().getSimpleName());
	}
	
	@Override
	public ArrayList<DetectionRecord> detect() {
		// Detection results are stored in this ArrayList
		ArrayList<DetectionRecord> listDetRec = new ArrayList<DetectionRecord>();
		
		// An example loop to get some AST nodes to analyze
		for(MethodDeclaration methodDec: wholeCodeAST.getMethodDeclarations()){
			
			/*
			 * int < double <long;
			 */
			
			// Q1: Is the return type in the declaration consistent with the return type in the ReturnStatement
			
			//Q2: Is the return type in the declaration consistent with the return type in all the IfStatement
			
			//Q3: Is the return type in the declaration consistent with the return type in all the ForStatement
			
			//Q4: Is the return type in the declaration consistent with the return type in all the WHileStatement
			
			//Q5: Is the return type in the declaration consistent with the return type in all the TryStatement
			
			List<Statement> lists = (List<Statement>) methodDec.getBody().statements();
			String retype = methodDec.getReturnType2().toString();
			
			if(retype.equals("int")||retype.equals("double")||retype.equals("long")){
				for(Statement st: lists){
					
					if(!verify(retype, st)){
						int lineNum = wholeCodeAST.getLineNum(methodDec.getStartPosition());
						listDetRec.add(new DetectionRecord(bugName, projectName, id, path, lineNum, methodDec.getParent().toString(), false, false));
					}
				}
			}
		}
		
		return listDetRec;
	}


	public boolean verify(String retype, Statement st) {
		if (st instanceof ReturnStatement) {
			Expression exp = ((ReturnStatement) st).getExpression();
			if ((exp.toString().contains("Long") || exp.toString().contains("(long)"))
					&& (retype.equals("int") || retype.equals("double"))) {
				return false;
			}
			if ((exp.toString().contains("Doulbe") || exp.toString().contains("(double)")) && retype.equals("int")) {
				return false;
			}
			return true;
		}
		if (st instanceof IfStatement) {
			Statement ts = ((IfStatement) st).getThenStatement();
			if (ts instanceof Block) {
				List<Statement> lists = ((Block) ts).statements();
				for (Statement stt : lists) {
					verify(retype, stt);
				}
			}
			if (ts instanceof ReturnStatement) {
				Expression exp = ((ReturnStatement) ts).getExpression();
				if ((exp.toString().contains("Long") || exp.toString().contains("(long)"))
						&& (retype.equals("int") || retype.equals("double"))) {
					return false;
				}
				if ((exp.toString().contains("Doulbe") || exp.toString().contains("(double)"))
						&& retype.equals("int")) {
					return false;
				}
				return true;
			}
		}
		if (st instanceof ForStatement) {
			Statement ts = ((ForStatement) st).getBody();
			if (ts instanceof Block) {
				List<Statement> lists = ((Block) ts).statements();
				for (Statement stt : lists) {
					verify(retype, stt);
				}
			}
			if (ts instanceof ReturnStatement) {
				Expression exp = ((ReturnStatement) ts).getExpression();
				if ((exp.toString().contains("Long") || exp.toString().contains("(long)"))
						&& (retype.equals("int") || retype.equals("double"))) {
					return false;
				}
				if ((exp.toString().contains("Doulbe") || exp.toString().contains("(double)"))
						&& retype.equals("int")) {
					return false;
				}
				return true;
			}
		}

		if (st instanceof TryStatement) {
			Statement ts = ((TryStatement) st).getBody();
			if (ts instanceof Block) {
				List<Statement> lists = ((Block) ts).statements();
				for (Statement stt : lists) {
					verify(retype, stt);
				}
			}
			if (ts instanceof ReturnStatement) {
				Expression exp = ((ReturnStatement) ts).getExpression();
				if ((exp.toString().contains("Long") || exp.toString().contains("(long)"))
						&& (retype.equals("int") || retype.equals("double"))) {
					return false;
				}
				if ((exp.toString().contains("Doulbe") || exp.toString().contains("(double)"))
						&& retype.equals("int")) {
					return false;
				}
				return true;
			}
		}
		if (st instanceof WhileStatement) {
			Statement ts = ((WhileStatement) st).getBody();
			if (ts instanceof Block) {
				List<Statement> lists = ((Block) ts).statements();
				for (Statement stt : lists) {
					verify(retype, stt);
				}
			}
			if (ts instanceof ReturnStatement) {
				Expression exp = ((ReturnStatement) ts).getExpression();
				if ((exp.toString().contains("Long") || exp.toString().contains("(long)"))
						&& (retype.equals("int") || retype.equals("double"))) {
					return false;
				}
				if ((exp.toString().contains("Doulbe") || exp.toString().contains("(double)"))
						&& retype.equals("int")) {
					return false;
				}
				return true;
			}
		}
		return true;
	}

}
