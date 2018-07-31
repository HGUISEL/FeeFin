package edu.handong.csee.isel.pmd;

import java.io.File;

public class DetectionRecord {
	String type;
	String commitIDAnIssueExists;
	String prevCommitID;
	String date;
	String dataOfPrevCommit;
	int problemNo;
	String packageName;
	String file;
	String fullFilePath;
	String priority;
	int lineNum;
	String line;
	String description;
	public String getFullFilePath() {
		return fullFilePath;
	}
	String ruleSet;
	String rule;
	
	public DetectionRecord(String commitID, String date, String line, String prevCommitID, String datePrevCommit, String srcDir) {
		commitIDAnIssueExists = commitID;
		this.prevCommitID = prevCommitID;
		dataOfPrevCommit = datePrevCommit;
		this.date = date;
		setMembersFromLine(line,srcDir);
		
	}

	public DetectionRecord(String[] values) {
		
		/*
		 * 0: TYPE: FIXED, ALIVE, BI
		 * 1: KEY
		 * 2: Detection Type e.g., Error Prone
		 * 3: Rule name. e.g., DataflfowAnomalyAnalysis
		 * 4: Previous commit id
		 * 5: Date of previous commit
		 * 6: Commit id
		 * 7: Date of the commit id.
		 * 8: path
		 * 9: line number
		 * 10: line
		 * 11~: additional texts because of a separator ',' in the code.
		 */
		type = values[0];
		prevCommitID = values[4];
		dataOfPrevCommit = values[5];
		commitIDAnIssueExists = values[6];
		date = values[7];
		line = getCodeLine(values);
	}

	private String getCodeLine(String[] values) {
		
		if(values.length == 11)
			return values[10];

		String code = values[10];
		for(int i=11; i<values.length; i++)
			code += "," + values[i];
		
		return code;
	}

	public String getPrevCommitID() {
		return prevCommitID;
	}

	public void setPrevCommitID(String prevCommitID) {
		this.prevCommitID = prevCommitID;
	}

	public String getDate() {
		return date;
	}

	public void setDate(String date) {
		this.date = date;
	}

	public String getDataOfPrevCommit() {
		return dataOfPrevCommit;
	}

	public void setDataOfPrevCommit(String dataOfPrevCommit) {
		this.dataOfPrevCommit = dataOfPrevCommit;
	}

	public void setMembersFromLine(String line, String srcDir) {
		String[] data = line.replace("\"", "").split(",");
		problemNo = Integer.parseInt(data[0]);
		packageName = data[1];
		file = data[2].replace((new File(srcDir)).getAbsolutePath(), "");
		fullFilePath = data[2];
		priority = data[3];
		lineNum = Integer.parseInt(data[4]);
		description = data[5];
		ruleSet = data[6];
		rule = data[7];
	}

	public String getType() {
		return type;
	}
	public void setLine(String source) {
		line = source.split("\n")[lineNum-1].trim();
	}
	public int getProblemNo() {
		return problemNo;
	}
	public void setProblemNo(int problemNo) {
		this.problemNo = problemNo;
	}
	public String getPakcage() {
		return packageName;
	}
	public void setPakcage(String pakcage) {
		this.packageName = pakcage;
	}
	public String getFile() {
		return file;
	}
	public void setFile(String file) {
		this.file = file;
	}
	public String getPriority() {
		return priority;
	}
	public void setPriority(String priority) {
		this.priority = priority;
	}
	public String getLastestCommitIDAnIssueExists() {
		return commitIDAnIssueExists;
	}
	public int getLineNum() {
		return lineNum;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getRuleSet() {
		return ruleSet;
	}
	public void setRuleSet(String ruleSet) {
		this.ruleSet = ruleSet;
	}
	public String getRule() {
		return rule;
	}
	public void setRule(String rule) {
		this.rule = rule;
	}
	public void showSummary() {
		System.out.println(file + " " + description);
	}
	public String getLine() {
		return line;
	}
}
