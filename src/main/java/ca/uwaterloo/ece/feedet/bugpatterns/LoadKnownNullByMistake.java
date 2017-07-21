package ca.uwaterloo.ece.feedet.bugpatterns;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.InfixExpression.Operator;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.NullLiteral;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jgit.lib.Repository;

import ca.uwaterloo.ece.feedet.DetectionRecord;
import ca.uwaterloo.ece.feedet.utils.JavaASTParser;

public class LoadKnownNullByMistake extends Bug {

	public LoadKnownNullByMistake(String prjName,JavaASTParser ast, String id, String path, Repository repo){
		initialize(prjName,ast,id,path,repo,this.getClass().getSimpleName());
	}
	
	@Override
	protected String getDescription() {
		return "It seems null is loaded by a mistake. Please, check if null checker and the related object loading is correct.";
	}
	
	@Override
	public ArrayList<DetectionRecord> detect() {
		// Detection results are stored in this ArrayList
		ArrayList<DetectionRecord> listDetRec = new ArrayList<DetectionRecord>();
		
		// An example loop to get some AST nodes to analyze
		for(IfStatement ifStmt:wholeCodeAST.getIfStatements()){
			
			int nullCheckingTypeInIfStmt = getNullCheckingTypeInIfStmt(ifStmt);
			
			if(nullCheckingTypeInIfStmt == OTHERS) continue;
			
			ASTNode nullCheckingObject = getNullcheckingObject(ifStmt);
			
			if(nullCheckingObject == null) continue;
			
			Statement stmtInNulchecker = getNullBlockFromIfStmt(ifStmt,nullCheckingTypeInIfStmt);
			
			if (stmtInNulchecker == null) continue;
				
			ArrayList<Assignment> assignments = getAssignmentsForNullCheckingObject(stmtInNulchecker,nullCheckingObject);
			
			// to check if nullCheckingObject used as a parameter in a method invocation or call a method.
			ArrayList<MethodInvocation> methodInvs = getMethodInvsRelatedToNullCheckingObject(wholeCodeAST.getMethodInvocations(stmtInNulchecker),nullCheckingObject);
			
			// to check if nullCheckingObject access a field
			ArrayList<FieldAccess> fieldAccesses = getFieldAccessRelatedToNullCheckingObject(wholeCodeAST.getFieldAccesses(stmtInNulchecker),nullCheckingObject);
			
			if(methodInvs.size() == 0 && fieldAccesses.size() == 0) continue;
			
			if(anyAssignmentBeforeMethodInvOrFieldAccess(assignments,methodInvs,fieldAccesses)) continue;
			
			// get Line number
			int lineNum = wholeCodeAST.getLineNum(stmtInNulchecker.getStartPosition());
				
			listDetRec.add(new DetectionRecord(bugName, getDescription(), projectName, id, path, lineNum, stmtInNulchecker.getParent().toString(), false, false));
		}
		
		return listDetRec;
	}
	
	private Statement getNullBlockFromIfStmt(IfStatement ifStmt, int nullCheckingTypeInIfStmt) {
		
		if(nullCheckingTypeInIfStmt == EQUALS_NULL) return ifStmt.getThenStatement();
		
		if(nullCheckingTypeInIfStmt == EQUALS_NOT_NULL) return ifStmt.getElseStatement();
		
		return null;
	}

	final int EQUALS_NULL = 0;
	final int EQUALS_NOT_NULL = 1;
	final int OTHERS = -1;
	private int getNullCheckingTypeInIfStmt(IfStatement ifStmt) {
		if(ifStmt.getExpression() instanceof InfixExpression){
			InfixExpression exp = (InfixExpression) ifStmt.getExpression();
			if(exp.getOperator() == Operator.EQUALS) return EQUALS_NULL;
			if(exp.getOperator() == Operator.NOT_EQUALS) return EQUALS_NOT_NULL;
		}
		return OTHERS;
	}

	private boolean anyAssignmentBeforeMethodInvOrFieldAccess(ArrayList<Assignment> assignments,
			ArrayList<MethodInvocation> methodInvs, ArrayList<FieldAccess> fieldAccesses) {
		
		if(assignments.size() == 0) return false;
		
		int lineNumOfFirstAssign = wholeCodeAST.getLineNum(assignments.get(0).getStartPosition());
		
		if(methodInvs.size() > 0){
			if(lineNumOfFirstAssign < wholeCodeAST.getLineNum(methodInvs.get(0).getStartPosition())) return true;
		}
		
		if(fieldAccesses.size() > 0){
			if(lineNumOfFirstAssign < wholeCodeAST.getLineNum(fieldAccesses.get(0).getStartPosition())) return true;
		}
		
		return false;
	}

	private ArrayList<FieldAccess> getFieldAccessRelatedToNullCheckingObject(ArrayList<FieldAccess> fieldAccesses,
			ASTNode nullCheckingObject) {
		
		ArrayList<FieldAccess> finalFieldAccesses = new ArrayList<FieldAccess>();
		
		for(FieldAccess fieldAccess:fieldAccesses){
			if(fieldAccess.getExpression().toString().equals(nullCheckingObject.toString()))
				finalFieldAccesses.add(fieldAccess);
		}

		return finalFieldAccesses;
	}

	private ArrayList<MethodInvocation> getMethodInvsRelatedToNullCheckingObject(
			ArrayList<MethodInvocation> methodInvocations,ASTNode nullCheckingObject) {
		
		ArrayList<MethodInvocation> finalInvs = new ArrayList<MethodInvocation>();
		
		for(MethodInvocation methodInv:methodInvocations){
			// as a parameter
			@SuppressWarnings("unchecked")
			List<ASTNode> arguments = methodInv.arguments();
			boolean usedAsParameter = false;
			for(ASTNode argument:arguments){
				if(argument.toString().equals(nullCheckingObject.toString()))
					usedAsParameter = true;
			}
			
			// as a method call
			boolean usedAsCaller = false;
			if(methodInv.getName().toString().equals(nullCheckingObject.toString()))
				usedAsCaller = true;
			
			if(usedAsParameter || usedAsCaller) finalInvs.add(methodInv);
		}

		return finalInvs;
	}

	private ArrayList<Assignment> getAssignmentsForNullCheckingObject(Statement statementInNullChecker,ASTNode nullCheckingObject) {
		ArrayList<Assignment> assignments = new ArrayList<Assignment>();
		
		for(Assignment assignment:wholeCodeAST.getAssignments(statementInNullChecker)){
			if(assignment.getLeftHandSide().toString().equals(nullCheckingObject.toString()))
				assignments.add(assignment);
		}
		return assignments;
	}

	private ASTNode getNullcheckingObject(IfStatement ifStmt) {

		if(ifStmt.getExpression() instanceof InfixExpression){
			
			InfixExpression exp = (InfixExpression) ifStmt.getExpression();
			if(exp.getOperator() == Operator.EQUALS || exp.getOperator() == Operator.NOT_EQUALS){
				
				if(exp.getRightOperand() instanceof NullLiteral){
					return exp.getLeftOperand();
				}
				
				if(exp.getLeftOperand() instanceof NullLiteral){
					return exp.getRightOperand();
				}
			}
		}
		
		return null;
	}
}
