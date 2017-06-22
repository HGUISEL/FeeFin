package ca.uwaterloo.ece.feedet.bugpatterns;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ArrayAccess;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.WhileStatement;
import org.eclipse.jgit.lib.Repository;

import ca.uwaterloo.ece.feedet.DetectionRecord;
import ca.uwaterloo.ece.feedet.utils.JavaASTParser;

public class InconsistentIncrementerInWhile extends Bug {

	public InconsistentIncrementerInWhile(String prjName,JavaASTParser ast, String id, String path, Repository repo){
		initialize(prjName,ast,id,path,repo,this.getClass().getSimpleName());
	}
	
	@Override
	public ArrayList<DetectionRecord> detect() {
		// Detection results are stored in this ArrayList
		ArrayList<DetectionRecord> listDetRec = new ArrayList<DetectionRecord>();
		// An example loop to get some AST nodes to analyze
		for(WhileStatement whileStmt:wholeCodeAST.getWhileStatements()){
			
			ArrayList<InfixExpression> infixExps = wholeCodeAST.getInfixExpressions(whileStmt.getExpression());
			if(infixExps.size()==0) continue;
			
			String incrementer = "";
			ASTNode targetCollection = null;
			for(InfixExpression infixExp:infixExps){
				if(isRangeChecker(infixExp)){
					if(infixExp.getLeftOperand() instanceof SimpleName){
						incrementer = infixExp.getLeftOperand().toString();
						targetCollection = getCollection(infixExp.getRightOperand());
					}
					else if(infixExp.getRightOperand() instanceof SimpleName){
						incrementer = infixExp.getRightOperand().toString();
						targetCollection = getCollection(infixExp.getLeftOperand());
					}
				}
			}
			
			if(targetCollection == null) continue;
			
			Statement statement = whileStmt.getBody();
			
			anyIssueUsingIncrementer(statement,targetCollection,incrementer,listDetRec);
		}
		return listDetRec;
	}

	@SuppressWarnings("unchecked")
	private void anyIssueUsingIncrementer(Statement statement, ASTNode targetCollection, String incrementer,ArrayList<DetectionRecord> listDetRec) {
		
		// ignore inner while to avoid redundant detection
		if(statement instanceof WhileStatement) return;
		
		if(statement instanceof Block || statement instanceof SwitchStatement){
			
			List<Statement> statements = statement instanceof Block?((Block)statement).statements():((SwitchStatement)statement).statements();
			for(Statement stmt:statements){
				
				if(stmt instanceof Block || stmt instanceof SwitchStatement || stmt instanceof WhileStatement){
					anyIssueUsingIncrementer(stmt,targetCollection, incrementer,listDetRec);
				}else{
					if( anyIssueUsingIncrementer(targetCollection, incrementer,stmt)){
						// get Line number
						int lineNum = wholeCodeAST.getLineNum(stmt.getStartPosition());	
						listDetRec.add(new DetectionRecord(bugName, projectName, id, path, lineNum, stmt.toString(),getWhileStmt(stmt), false, false));
					}
				}
			}
		}else{
			if( anyIssueUsingIncrementer(targetCollection, incrementer,statement)){
				// get Line number
				int lineNum = wholeCodeAST.getLineNum(statement.getStartPosition());	
				listDetRec.add(new DetectionRecord(bugName, projectName, id, path, lineNum, statement.toString(),getWhileStmt(statement), false, false));
			}
		}
	}

	private String getWhileStmt(ASTNode stmt) {
		
		if(stmt.getParent() instanceof WhileStatement){
			return stmt.getParent().toString();
		}
		return getWhileStmt(stmt.getParent());
	}

	private boolean anyIssueUsingIncrementer(ASTNode targetCollection, String incrementer, Statement stmt) {
		
		//boolean existTargetCollectionWithWrongIncrementer = false;
		if(targetCollection instanceof Expression){ // for collection
			ArrayList<SimpleName> simpleNames = wholeCodeAST.getSimpleNames(stmt);
			
			for(SimpleName simpleName:simpleNames){
				//if(simpleName.toString().equals(incrementer))
				//	existIncrementer = true;
				if(simpleName.toString().equals(targetCollection.toString())){
					if(!useCorrectIncrementer(simpleName,incrementer))
						return true;
				}
			}
		}else{ // for array
			ArrayList<ArrayAccess> arrayAccessed = wholeCodeAST.getArrayAccesses(stmt);
			for(ArrayAccess arrayAccess:arrayAccessed){
				//if(arrayAccess.toString().equals(incrementer))
				//	existIncrementer = true;
				if(arrayAccess.toString().equals(targetCollection.toString())){
					if(!useCorrectIncrementer(arrayAccess,incrementer))
						return true; //existTargetCollectionWithWrongIncrementer = true;
				}
			}
		}
		
		return false;
	}

	private boolean useCorrectIncrementer(ASTNode targetCollection, String incrementer) {
		
		if(targetCollection.getParent() instanceof MethodInvocation){
			MethodInvocation methodInv = (MethodInvocation)targetCollection.getParent();
			
			if(methodInv.getExpression()==null || !methodInv.getExpression().toString().equals(targetCollection.toString()))
					return true;
			
			// no arguments? then no need to worry about incorrect incrementer
			if(methodInv.arguments().size()==0) return true;
			
			//
			for(Object argument:(List<?>)methodInv.arguments()){
				if(argument.toString().equals(incrementer))
						return true;
			}
			
			if(!(methodInv.getName().toString().equals("get") || methodInv.getName().toString().equals("charAt")))
					return true;
			
			return false;
		}
		
		// TODO need to deal with array
		//if()
		return true;
	}

	private ASTNode getCollection(Expression operand) {
		
		if(operand instanceof MethodInvocation){
			if(((MethodInvocation)operand).getExpression() != null)
			return ((MethodInvocation)operand).getExpression();
		}
		
		if(operand instanceof QualifiedName){
			return ((QualifiedName)operand).getQualifier();
		}
		
		return null;
	}

	private boolean isRangeChecker(InfixExpression infixExp) {
		
		if(!(infixExp.getLeftOperand() instanceof SimpleName || infixExp.getRightOperand() instanceof SimpleName)) return false;
		
		boolean simpleNameInLeft = infixExp.getLeftOperand() instanceof SimpleName;
		boolean simpleNameInRight = infixExp.getRightOperand() instanceof SimpleName;
		
		String potentialRangeChecker = simpleNameInLeft? (infixExp.getRightOperand().toString()):infixExp.getLeftOperand().toString();
		
		
		String potentialIncrementer = "";
		
		if(simpleNameInLeft)  potentialIncrementer = infixExp.getLeftOperand().toString();
		if(simpleNameInRight)  potentialIncrementer = infixExp.getRightOperand().toString();
		
		if(potentialIncrementer.toLowerCase().contains("max") || potentialIncrementer.toLowerCase().contains("min"))
			return false;
		
		if((potentialRangeChecker.contains("length") || potentialRangeChecker.contains("size"))
				&& (
						(simpleNameInLeft && infixExp.getOperator() != InfixExpression.Operator.GREATER)
						||
						(simpleNameInRight && infixExp.getOperator() != InfixExpression.Operator.LESS)
					)
		  )
			return true;
		return false;
	}
}
