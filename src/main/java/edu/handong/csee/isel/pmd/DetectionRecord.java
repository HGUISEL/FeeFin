package edu.handong.csee.isel.pmd;

public class DetectionRecord {
	int problemNo;
	String pakcage;
	String file;
	String priority;
	String lineNum;
	String description;
	String ruleSet;
	String rule;
	
	public DetectionRecord(String line) {
		String[] data = line.replace("\"", "").split(",");
		problemNo = Integer.parseInt(data[0]);
		pakcage = data[1];
		file = data[2];
		priority = data[3];
		lineNum = data[4];
		description = data[5];
		ruleSet = data[6];
		rule = data[7];
	}
	public int getProblemNo() {
		return problemNo;
	}
	public void setProblemNo(int problemNo) {
		this.problemNo = problemNo;
	}
	public String getPakcage() {
		return pakcage;
	}
	public void setPakcage(String pakcage) {
		this.pakcage = pakcage;
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
	public String getLine() {
		return lineNum;
	}
	public void setLine(String line) {
		this.lineNum = line;
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
}