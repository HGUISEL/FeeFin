package ca.uwaterloo.ece.feedet.bugpatterns;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ArrayAccess;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.WhileStatement;
import org.eclipse.jgit.lib.Repository;

import ca.uwaterloo.ece.feedet.DetectionRecord;
import ca.uwaterloo.ece.feedet.utils.JavaASTParser;

public class InconsistentIncrementerInWhile extends Bug {

	public InconsistentIncrementerInWhile(String prjName,JavaASTParser ast, String id, String path, Repository repo){
		initialize(prjName,ast,id,path,repo,this.getClass().getSimpleName());
	}
	
	@Override
	protected String getDescription() {
		return "Incrementer used in While block may be incorrect.";
	}
	
	@Override
	public ArrayList<DetectionRecord> detect() {
		// Detection results are stored in this ArrayList
		ArrayList<DetectionRecord> listDetRec = new ArrayList<DetectionRecord>();
		// An example loop to get some AST nodes to analyze
		for(WhileStatement whileStmt:wholeCodeAST.getWhileStatements()){
			
			ArrayList<InfixExpression> infixExps = wholeCodeAST.getInfixExpressions(whileStmt.getExpression());
			if(infixExps.size()==0) continue;
			
			ArrayList<ExpressionStatement> expStmts = wholeCodeAST.getExpressionStatements(whileStmt);
			ArrayList<VariableDeclarationFragment> vardecFrags = wholeCodeAST.getVariableDeclarationFragments(whileStmt);
			
			String incrementer = "";
			ASTNode targetCollection = null;
			for(InfixExpression infixExp:infixExps){
				
				if(!(infixExp.getOperator() == InfixExpression.Operator.GREATER
					|| infixExp.getOperator() == InfixExpression.Operator.GREATER_EQUALS
					|| infixExp.getOperator() == InfixExpression.Operator.LESS
					|| infixExp.getOperator() == InfixExpression.Operator.LESS_EQUALS
					)
				)
					continue;
				
				if(isRangeChecker(infixExp)){
					if(infixExp.getLeftOperand() instanceof SimpleName || infixExp.getLeftOperand() instanceof FieldAccess ){
						incrementer = infixExp.getLeftOperand().toString();
						targetCollection = getCollection(infixExp.getRightOperand());
					}
					else if(infixExp.getRightOperand() instanceof SimpleName || infixExp.getRightOperand() instanceof FieldAccess ){
						incrementer = infixExp.getRightOperand().toString();
						targetCollection = getCollection(infixExp.getLeftOperand());
					}
				}
				
				if(targetCollection !=null && !incrementer.isEmpty()){
					
					if(outerLoopHasDifferentIncrementForTheSameArray(whileStmt,targetCollection)) continue;
					
					anyIssueUsingIncrementer(expStmts,vardecFrags,targetCollection,incrementer,listDetRec);
				}else{
					// initiate
					targetCollection = null;
					incrementer = "";
				}
			}
		}
		return listDetRec;
	}

	private boolean outerLoopHasDifferentIncrementForTheSameArray(WhileStatement whileStmt,ASTNode targetCollection) {
		
		// get outer while
		WhileStatement outerWhileStmt = (WhileStatement) getOuterWhile(whileStmt);
		if(outerWhileStmt!=null){
			if(outerWhileStmt.getExpression() instanceof InfixExpression){
				InfixExpression infixExp = (InfixExpression) outerWhileStmt.getExpression();
				ArrayList<SimpleName> simpleNames = wholeCodeAST.getSimpleNames(infixExp);
				for(SimpleName name:simpleNames){
					if(name.toString().equals(targetCollection.toString()))
						return true;
				}
			}
		}
		
		ForStatement forStmt = (ForStatement) getOuterForLoop(whileStmt);
		if(forStmt!=null){
			if(forStmt.getExpression() instanceof InfixExpression){
				InfixExpression infixExp = (InfixExpression) forStmt.getExpression();
				ArrayList<SimpleName> simpleNames = wholeCodeAST.getSimpleNames(infixExp);
				for(SimpleName name:simpleNames){
					if(name.toString().equals(targetCollection.toString()))
						return true;
				}
			}
		}
		return false;
	}
	
	private ASTNode getOuterForLoop(ASTNode node) {
		
		if(node.getParent() == null)
			return null;
		
		if(node.getParent() instanceof ForStatement)
			return node.getParent();
		
		return getOuterForLoop(node.getParent());
	}

	private ASTNode getOuterWhile(ASTNode node) {
		
		if(node.getParent() == null)
			return null;
		
		if(node.getParent() instanceof WhileStatement)
			return node.getParent();
		
		return getOuterWhile(node.getParent());
	}

	private void anyIssueUsingIncrementer(ArrayList<ExpressionStatement> expStmts, ArrayList<VariableDeclarationFragment> varDecFrags, ASTNode targetCollection, String incrementer,ArrayList<DetectionRecord> listDetRec) {
		
		ArrayList<SimpleName> localVarNamesInWhileStmt = getLocalVarNamesInWhileStmt(varDecFrags,expStmts,targetCollection,incrementer);
		
		for(ExpressionStatement statement:expStmts){
			if( anyIssueUsingIncrementer(targetCollection, incrementer,statement,localVarNamesInWhileStmt)){
				// get Line number
				int lineNum = wholeCodeAST.getLineNum(statement.getStartPosition());	
				listDetRec.add(new DetectionRecord(bugName, getDescription(), projectName, id, path, lineNum, statement.toString(),getWhileStmt(statement), false, false));
			}
		}
		
		for(VariableDeclarationFragment varDecFrag:varDecFrags){
			if( anyIssueUsingIncrementer(targetCollection, incrementer,varDecFrag,localVarNamesInWhileStmt)){
				// get Line number
				int lineNum = wholeCodeAST.getLineNum(varDecFrag.getStartPosition());	
				listDetRec.add(new DetectionRecord(bugName, getDescription(), projectName, id, path, lineNum, varDecFrag.toString(),getWhileStmt(varDecFrag), false, false));
			}
		}
	}

	private ArrayList<SimpleName> getLocalVarNamesInWhileStmt(ArrayList<VariableDeclarationFragment> varDecFrags,
			ArrayList<ExpressionStatement> expStmts,
			ASTNode targetCollection,
			String incrementer) {
		
		ArrayList<SimpleName> simpleNames = new ArrayList<SimpleName>();
		for(VariableDeclarationFragment varDecFrag:varDecFrags){
			simpleNames.add(varDecFrag.getName());
		}
		
		for(ExpressionStatement expStmt:expStmts){
			if(expStmt.getExpression() instanceof Assignment){
				Assignment assginement = (Assignment) expStmt.getExpression();
				ArrayList<SimpleName> assignedVars = wholeCodeAST.getSimpleNames(assginement.getLeftHandSide());	
				ArrayList<SimpleName> assiningVars = wholeCodeAST.getSimpleNames(assginement.getRightHandSide());
				
				for(SimpleName assiningVar:assiningVars){
					if(assiningVar.toString().equals(targetCollection.toString()) || assiningVar.toString().equals(incrementer)){
						simpleNames.addAll(assignedVars);
					}
				}
			} else if(expStmt.getExpression() instanceof PostfixExpression){			
				PostfixExpression postfixExp = (PostfixExpression) expStmt.getExpression();
				ArrayList<SimpleName> simpleNameInOperands = wholeCodeAST.getSimpleNames(postfixExp);
				simpleNames.addAll(simpleNameInOperands);
				
			} else if(expStmt.getExpression() instanceof PrefixExpression){
				PrefixExpression prefixExp = (PrefixExpression) expStmt.getExpression();
				ArrayList<SimpleName> simpleNameInOperands = wholeCodeAST.getSimpleNames(prefixExp);
				simpleNames.addAll(simpleNameInOperands);
			}
		}
		
		return simpleNames;
	}

	private String getWhileStmt(ASTNode stmt) {
		
		if(stmt.getParent() instanceof WhileStatement){
			return stmt.getParent().toString();
		}
		return getWhileStmt(stmt.getParent());
	}

	private boolean anyIssueUsingIncrementer(ASTNode targetCollection, String incrementer, ASTNode stmt, ArrayList<SimpleName> localVarNamesInWhileStmt) {
		
		//boolean existTargetCollectionWithWrongIncrementer = false;
		if(targetCollection instanceof Expression){ // for collection, field access
			ArrayList<SimpleName> simpleNames = wholeCodeAST.getSimpleNames(stmt);
			ArrayList<FieldAccess> fileAccesses = wholeCodeAST.getFieldAccesses(stmt);
			
			for(SimpleName simpleName:simpleNames){
				//if(simpleName.toString().equals(incrementer))
				//	existIncrementer = true;
				if(simpleName.toString().equals(targetCollection.toString())){
					if(!useCorrectIncrementer(simpleName,incrementer,localVarNamesInWhileStmt))
						return true;
				}
			}
			
			for(FieldAccess fieldAccess:fileAccesses){
				//if(simpleName.toString().equals(incrementer))
				//	existIncrementer = true;
				if(fieldAccess.toString().equals(targetCollection.toString())){
					if(!useCorrectIncrementer(fieldAccess,incrementer.replace("this.", ""),localVarNamesInWhileStmt))
						return true;
				}
			}
			
			
		}else{ // for array
			ArrayList<ArrayAccess> arrayAccessed = wholeCodeAST.getArrayAccesses(stmt);
			for(ArrayAccess arrayAccess:arrayAccessed){
				//if(arrayAccess.toString().equals(incrementer))
				//	existIncrementer = true;
				if(arrayAccess.toString().equals(targetCollection.toString())){
					if(!useCorrectIncrementer(arrayAccess,incrementer,localVarNamesInWhileStmt))
						return true; //existTargetCollectionWithWrongIncrementer = true;
				}
			}
		}
		
		return false;
	}

	private boolean useCorrectIncrementer(ASTNode targetCollection, String incrementer, ArrayList<SimpleName> localVarNamesInWhileStmt) {
		
		if(targetCollection.getParent() instanceof MethodInvocation){
			MethodInvocation methodInv = (MethodInvocation)targetCollection.getParent();
			
			if(methodInv.getExpression()==null || !methodInv.getExpression().toString().equals(targetCollection.toString()))
					return true;
			
			// no arguments? then no need to worry about incorrect incrementer
			if(methodInv.arguments().size()==0) return true;
			

			for(Object argument:(List<?>)methodInv.arguments()){
				
				ArrayList<SimpleName> simpleNames = wholeCodeAST.getSimpleNames((ASTNode)argument);
				
				for(SimpleName simpleName:simpleNames){
					if(simpleName.toString().equals(incrementer))
							return true;
				}
				
				// if local variable is used as an indexer, the it is acceptable.
				for(SimpleName localVarName:localVarNamesInWhileStmt){
					for(SimpleName simpleName:simpleNames){
						if(localVarName.toString().equals(simpleName.toString()))
							return true;
					}
				}
			}
			
			if(!(methodInv.getName().toString().equals("get") || methodInv.getName().toString().equals("charAt")))
					return true;
			
			return false;
		}
		
		if(targetCollection.getParent() instanceof ArrayAccess){
			ArrayAccess arrayAccess = (ArrayAccess) targetCollection.getParent();
			ArrayList<SimpleName> simpleNames = wholeCodeAST.getSimpleNames(arrayAccess);
			
			if(arrayAccess.getIndex() instanceof InfixExpression){
				InfixExpression indexExp = (InfixExpression) arrayAccess.getIndex();
				if(indexExp.getLeftOperand() instanceof QualifiedName)
					return true;
				if(indexExp.getRightOperand() instanceof QualifiedName)
					return true;
			}
			
			for(SimpleName simpleName:simpleNames){
				if(simpleName.toString().equals(incrementer))
					return true;
			}
			
			// if local variable is used as an indexer, it is acceptable.
			for(SimpleName localVarName:localVarNamesInWhileStmt){
				for(SimpleName simpleName:simpleNames){
					if(localVarName.toString().equals(simpleName.toString()))
						return true;
				}
			}
			
			return false;
		}
		
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
		
		if(operand instanceof FieldAccess){
			return ((FieldAccess)operand).getExpression();
		}
		
		return null;
	}

	private boolean isRangeChecker(InfixExpression infixExp) {
		
		if(!(infixExp.getLeftOperand() instanceof SimpleName 
				|| infixExp.getRightOperand() instanceof SimpleName
				|| infixExp.getLeftOperand() instanceof FieldAccess
				|| infixExp.getRightOperand() instanceof FieldAccess
				)
		)
			return false;
		
		boolean validNameInLeft = infixExp.getLeftOperand() instanceof SimpleName || infixExp.getLeftOperand() instanceof FieldAccess;
		boolean validNameInRight = infixExp.getRightOperand() instanceof SimpleName || infixExp.getRightOperand() instanceof FieldAccess;
		
		String potentialRangeChecker = validNameInLeft? (infixExp.getRightOperand().toString()):infixExp.getLeftOperand().toString();
		
		
		String potentialIncrementer = "";
		
		if(validNameInLeft)  potentialIncrementer = infixExp.getLeftOperand().toString();
		if(validNameInRight)  potentialIncrementer = infixExp.getRightOperand().toString();
		
		if(potentialIncrementer.toLowerCase().contains("max") || potentialIncrementer.toLowerCase().contains("min"))
			return false;
		
		if((potentialRangeChecker.contains("length") || potentialRangeChecker.contains("size"))
				&& (
						(validNameInLeft && infixExp.getOperator() != InfixExpression.Operator.GREATER)
						||
						(validNameInRight && infixExp.getOperator() != InfixExpression.Operator.LESS)
					)
		  )
			return true;
		return false;
	}
}
