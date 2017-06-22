package ca.uwaterloo.ece.feedet.bugpatterns;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.WhileStatement;
import org.eclipse.jgit.lib.Repository;

import ca.uwaterloo.ece.feedet.DetectionRecord;
import ca.uwaterloo.ece.feedet.utils.JavaASTParser;

public class InconsistentIncrementerInWhile extends Bug {

	public InconsistentIncrementerInWhile(String prjName,JavaASTParser ast, String id, String path, Repository repo){
		initialize(prjName,ast,id,path,repo,this.getClass().getSimpleName());
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public ArrayList<DetectionRecord> detect() {
		// Detection results are stored in this ArrayList
		ArrayList<DetectionRecord> listDetRec = new ArrayList<DetectionRecord>();
		// An example loop to get some AST nodes to analyze
		for(WhileStatement whileStmt:wholeCodeAST.getWhileStatements()){
			
			ArrayList<InfixExpression> infixExps = wholeCodeAST.getInfixExpressions(whileStmt.getExpression());
			if(infixExps.size()==0) continue;
			
			String incrementer = "";
			String targetCollection = "";
			for(InfixExpression infixExp:infixExps){
				if(isRangeChecker(infixExp)){
					if(infixExp.getLeftOperand() instanceof SimpleName){
						incrementer = infixExp.getLeftOperand().toString();
						targetCollection = getCollectionName(infixExp.getRightOperand());
					}
					else if(infixExp.getRightOperand() instanceof SimpleName){
						incrementer = infixExp.getRightOperand().toString();
						targetCollection = getCollectionName(infixExp.getLeftOperand());
					}
				}
			}
			
			if(targetCollection.isEmpty()) continue;
			
			Statement statement = whileStmt.getBody();
			
			if(statement instanceof Block) {
				boolean existIncrementer = false;
				boolean existTargetCollectionWithWrongIncrementer = false;
				for(Statement stmt:(List<Statement>)((Block)statement).statements()){
					ArrayList<SimpleName> simpleNames = wholeCodeAST.getSimpleNames(stmt);
					for(SimpleName simpleName:simpleNames){
						if(simpleName.toString().equals(incrementer))
							existIncrementer = true;
						if(simpleName.toString().equals(targetCollection)){
							
							if(!useCorrectIncrementer(simpleName,incrementer))
								existTargetCollectionWithWrongIncrementer = true;
						}
					}
					if((existTargetCollectionWithWrongIncrementer) && !(existIncrementer && existTargetCollectionWithWrongIncrementer)){
						// get Line number
						int lineNum = wholeCodeAST.getLineNum(whileStmt.getStartPosition());	
						listDetRec.add(new DetectionRecord(bugName, projectName, id, path, lineNum, whileStmt.toString(), false, false));
					}
				}
			}	
		}
		return listDetRec;
	}

	private boolean useCorrectIncrementer(SimpleName simpleName, String incrementer) {
		
		if(simpleName.getParent() instanceof MethodInvocation){
			MethodInvocation methodInv = (MethodInvocation)simpleName.getParent();
			
			// no arguments? then no need to worry about incorrect incrementer
			if(methodInv.arguments().size()==0) return true;
			
			//
			for(Object argument:(List<?>)methodInv.arguments()){
				if(argument.toString().equals(incrementer))
						return true;
			}
		}
		
		return false;
	}

	private String getCollectionName(Expression operand) {
		
		if(operand instanceof MethodInvocation){
			if(((MethodInvocation)operand).getExpression() != null)
			return ((MethodInvocation)operand).getExpression().toString();
		}
		return "";
	}

	private boolean isRangeChecker(InfixExpression infixExp) {
		
		if(!(infixExp.getLeftOperand() instanceof SimpleName || infixExp.getRightOperand() instanceof SimpleName)) return false;
		
		boolean simpleNameInLeft = infixExp.getLeftOperand() instanceof SimpleName;
		boolean simpleNameInRight = infixExp.getRightOperand() instanceof SimpleName;
		
		String potentialRangeChecker = simpleNameInLeft? (infixExp.getRightOperand().toString()):infixExp.getLeftOperand().toString();
		
		
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
