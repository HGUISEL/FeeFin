package ca.uwaterloo.ece.feedet.bugpatterns;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ArrayAccess;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SwitchCase;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jgit.lib.Repository;

import ca.uwaterloo.ece.feedet.DetectionRecord;
import ca.uwaterloo.ece.feedet.utils.JavaASTParser;

public class RedundantAssignment extends Bug {

	public RedundantAssignment(String prjName,JavaASTParser ast, String id, String path, Repository repo){
		initialize(prjName,ast,id,path,repo,this.getClass().getSimpleName());
	}
	
	@Override
	public ArrayList<DetectionRecord> detect() {
		// Detection results are stored in this ArrayList
		ArrayList<DetectionRecord> detRec = new ArrayList<DetectionRecord>();
		
		/*
		 *        connection = (HttpURLConnection) SecurityUtil.openSecureHttpConnection(url);
	       -      connection = (HttpURLConnection)URLUtils.openConnection(url);
		 */
		for(MethodDeclaration mdec: wholeCodeAST.getMethodDeclarations()){
			
			if (mdec.getBody() == null) continue;
			
			ArrayList<Assignment> assigns = getAssignments(mdec);
			
			//if only has one assignment, skip
			if(assigns.size()<=1)
				continue;
		
			//else compare each two assignments
			for(int a=0; a<assigns.size()-1;a++){
				
				// Q6 this may override other Qs. assignments are in the consecutive lines?
				if(!assignmentsInConsecutiveLines(assigns.get(a),assigns.get(a+1))) continue;
				
				if(!noUsageBetween(assigns,a,assigns.get(a).getLeftHandSide().toString())) continue;
				
				// Q3
				if(!AreAssignementsInTheSameBlock(assigns.get(a),assigns.get(a+1))) continue;
				
				// Q5 assignee is an array and contains any operation? yes, ignore.
				if(assigns.get(a).getLeftHandSide() instanceof ArrayAccess && containsAnyOperation((ArrayAccess) assigns.get(a).getLeftHandSide()) ) continue;
				
				if(assigns.get(a).getLeftHandSide().toString().equals(assigns.get(a+1).getLeftHandSide().toString())){
					int lineNum = wholeCodeAST.getLineNum(assigns.get(a).getStartPosition());
					 detRec.add(new DetectionRecord(bugName, projectName, id, path, lineNum, assigns.get(a).toString() + ";" + assigns.get(a+1).toString(),assigns.get(a).getParent().getParent().toString(), false, false));
				}
			}
		}
		
		return detRec;
	}

	private boolean assignmentsInConsecutiveLines(Assignment assignment, Assignment assignment2) {
		
		if (wholeCodeAST.getLineNum(assignment2.getStartPosition())-wholeCodeAST.getLineNum(assignment.getStartPosition())==1)
			return true;
		
		// in case there are blank lines between assignment and assigment2
		if(assignment.getParent().getParent() instanceof Block && assignment.getParent().getParent().equals(assignment2.getParent().getParent())){
			
			@SuppressWarnings("unchecked")
			List<Statement> lstStmt =((Block) assignment.getParent().getParent()).statements();
			
			for(int i=0; i < lstStmt.size()-1;i++){
				Statement stmt = lstStmt.get(i);
				Statement stmt2 = lstStmt.get(i+1);
				if(stmt.equals(assignment.getParent()) && stmt2.equals(assignment2.getParent()))
					return true;
			}
		}
		
		return false;
	}

	private boolean containsAnyOperation(ArrayAccess arrayAccess) {
		
		if (arrayAccess.getIndex() instanceof PostfixExpression || arrayAccess.getIndex() instanceof PrefixExpression)
			return true;
		
		return false;
	}

	private boolean AreAssignementsInTheSameBlock(Assignment assignment, Assignment assignment2) {
		
		if(assignment.getParent().getParent().equals(assignment2.getParent().getParent())) {
			
			// Q4
			if(assignment.getParent().getParent() instanceof SwitchStatement && inDifferentSwitchCase(assignment,assignment2))
				return false;
			
			// Q7 parent is IfStatement? if so, check if assignment2 belongs to optional else statement, then can be considered not in the same block.
			if(assignment.getParent().getParent() instanceof IfStatement){
				IfStatement ifStmt = (IfStatement) assignment.getParent().getParent();
				if(ifStmt.getElseStatement() !=null && ifStmt.getElseStatement().getStartPosition() <= assignment2.getStartPosition())
					return false;
			}
			
			return true;
		}
		
		return false;
	}

	private boolean inDifferentSwitchCase(Assignment assignment, Assignment assignment2) { // executed only when assignments are in SwitchStatement
		
		SwitchStatement switchStmt = (SwitchStatement) assignment.getParent().getParent();
		
		final ArrayList<Integer> linesForSwitchCase = new ArrayList<Integer>();
		
		switchStmt.accept(new ASTVisitor() {
			@Override
			public boolean visit(SwitchCase node) {
				linesForSwitchCase.add(node.getStartPosition());
				return super.visit(node);
			}
		});
		
		int assignStartPosition = assignment.getStartPosition();
		int assign2StartPosition = assignment2.getStartPosition();
		
		for(int startPosSwitchCase:linesForSwitchCase){
			if(assignStartPosition < startPosSwitchCase && startPosSwitchCase < assign2StartPosition)
				return true;
		}
		
		return false;
	}

	private boolean noUsageBetween(ArrayList<Assignment> assigns, int a,String nameAssgiment) {
		
		int startLineNum = wholeCodeAST.getLineNum(assigns.get(a).getStartPosition());
		int endLineNum = wholeCodeAST.getLineNum(assigns.get(a+1).getStartPosition());
		
		ArrayList<SimpleName> lstSimpleNames = wholeCodeAST.getSimpleNames();
		ArrayList<MethodInvocation> lstMethodInvocations = wholeCodeAST.getMethodInvocations();
		
		// check the endAssignenent first
		if(!noUasgeInRightExpressionOfEndAssignemtn(assigns.get(a+1).getRightHandSide(),nameAssgiment)) return false;
		
		// then, check any usage between two assignments.
		for(SimpleName simpleName:lstSimpleNames){
			
			if(!nameAssgiment.equals(simpleName.toString())) continue;
			
			int lineNum = wholeCodeAST.getLineNum(simpleName.getStartPosition());
			
			if(startLineNum < lineNum && lineNum < endLineNum){	
				return false;
			}
		}
		
		// then, check any usage in terms filed access
		for(MethodInvocation fieldAccess:lstMethodInvocations){
			
			if(!fieldAccess.toString().contains(nameAssgiment)) continue;
			
			int lineNum = wholeCodeAST.getLineNum(fieldAccess.getStartPosition());
			
			if(startLineNum < lineNum && lineNum < endLineNum){	
				return false;
			}
		}
		return true;
	}

	private boolean noUasgeInRightExpressionOfEndAssignemtn(ASTNode node, String nameAssignment) {
		final ArrayList<String> simpleNames = new ArrayList<String>();
		final ArrayList<String> strMethodInvocations = new ArrayList<String>();
		
		node.accept(new ASTVisitor() {
			@Override
			public boolean visit(SimpleName node) {
				simpleNames.add(node.toString());
				return super.visit(node);
			}
			public boolean visit(MethodInvocation node) {
				strMethodInvocations.add(node.toString());
				return super.visit(node);
			}
		});
		
		// check with method invocations
		for(String strMethodInv:strMethodInvocations){
			if(strMethodInv.contains(nameAssignment)) return false;
		}
		
		// check with simple names
		return !simpleNames.contains(nameAssignment);
	}

	private ArrayList<Assignment> getAssignments(MethodDeclaration mdec) {
		final ArrayList<Assignment> assigns = new ArrayList<Assignment>();
		
		mdec.accept(new ASTVisitor() {
			@Override
			public boolean visit(Assignment node) {
				
				// Q2: only considers assignment by =
				if(node.getOperator() == Assignment.Operator.ASSIGN)
					assigns.add(node);
				return super.visit(node);
			}
		});
		
		return assigns;
	}
}
