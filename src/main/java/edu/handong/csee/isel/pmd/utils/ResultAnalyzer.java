package edu.handong.csee.isel.pmd.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import ca.uwaterloo.ece.feedet.utils.Utils;
import edu.handong.csee.isel.pmd.DetectionRecord;

public class ResultAnalyzer {
	
	String path;
	Boolean showAliveAfterFixed;
	Boolean VERBOSE;
	Boolean help;

	public static void main(String[] args) {
		new ResultAnalyzer().run(args);
	}

	private void run(String[] args) {
		Options options = createOptions();

		if(parseOptions(options, args)){
			if (help){
				printHelp(options);
				return;
			}
			
			ArrayList<String> lines = Utils.getLines(path, false);
			
			analyzeDetectionResults(lines);
			
		}
	}
	
	private void analyzeDetectionResults(ArrayList<String> lines) {
		
		// key: path+code line. Value: DetectionRecord
		HashMap<String,ArrayList<DetectionRecord>> recordsByEachDetection = new HashMap<String,ArrayList<DetectionRecord>>();
		HashMap<String,ArrayList<String>> recordsByTypes = new HashMap<String,ArrayList<String>>(); // key: TYPE, value: key of recordsByEachDetection
		ArrayList<String> fixedBugAliveLaterCases = new ArrayList<String>();
		// get data by the same detection case
		int i=0;
		for(String line:lines) {
			String[] splitLine = line.split(",");
			String key = splitLine[1];
			DetectionRecord decRec = new DetectionRecord(splitLine);
			if(!recordsByEachDetection.containsKey(key)) {
				recordsByEachDetection.put(key,new ArrayList<DetectionRecord>());
			}
			
			if(!recordsByEachDetection.get(key).add(decRec))
				if(VERBOSE)
					System.out.println("decRec is not added for a unknown reason");
			
			
			
			String type = decRec.getType();
			
			if(!recordsByTypes.containsKey(type)) {
				recordsByTypes.put(type, new ArrayList<String>());
			}
			
			recordsByTypes.get(type).add(key);
			// Fixed but caused the same case again.
			//
			if(type.equals("FIXED")) {
				if(recordsByTypes.get("ALIVE").contains(key))
					fixedBugAliveLaterCases.add(key);
			}	
			i++;
		}
		
		System.out.println("Total num of the detected = " + i);
		System.out.println("Total num of groups by the same detection = " + recordsByEachDetection.size());
		
		System.out.println("Num of FIXED = " + recordsByTypes.get("FIXED").size());
		System.out.println("Num of ALIVE = " +recordsByTypes.get("ALIVE").size());
		System.out.println("Num of BI = " +recordsByTypes.get("BI").size());
		
		int[] numAliveIssuesForFixedOnes = new int[recordsByTypes.get("FIXED").size()];
		int[] numAliveIssuesForNotFixedOnes = new int[recordsByTypes.get("ALIVE").size()];
		
		for(String type:recordsByTypes.keySet()) {
			
			switch(type) {
				case "FIXED":
					numAliveIssuesForFixedOnes = getNumOfCommitsByTypePerDetection(recordsByTypes.get(type),recordsByEachDetection);
					break;
				case "ALIVE":
					numAliveIssuesForNotFixedOnes = getNumOfCommitsByTypePerDetection(recordsByTypes.get(type),recordsByEachDetection);
					break;
			}
			
		}
		
		System.out.println("Average num of commits until fixed: " + Arrays.stream(numAliveIssuesForFixedOnes).average().getAsDouble());
		System.out.println("Average num of commits for not fixed: " + Arrays.stream(numAliveIssuesForNotFixedOnes).average().getAsDouble());
		
		if(showAliveAfterFixed) {
			System.out.println("=== Fixed but alive later ===");
			for(String line:fixedBugAliveLaterCases)
				System.out.println(line);
		}
		
	}

	private int[] getNumOfCommitsByTypePerDetection(ArrayList<String> arrayList, HashMap<String, ArrayList<DetectionRecord>> recordsByEachDetection) {
		
		int[] count = new int[arrayList.size()];
		
		int i = 0;
		for(String key:arrayList) {
			count[i] = recordsByEachDetection.get(key).size();
			i++;
		}
		
		return count;
	}

	Options createOptions(){

		// create Options object
		Options options = new Options();

		// add options
		options.addOption(Option.builder("p").longOpt("path")
				.desc("A CSV file path that contains the results from PMDExperimenter")
				.hasArg()
				.argName("path")
				.required()
				.build());

		options.addOption(Option.builder("a").longOpt("aliveafterfixed")
				.desc("Show the cases that are fixed but alive again.")
				.build());

		options.addOption(Option.builder("v").longOpt("verbose")
				.desc("Turn on verbose.")
				.build());

		options.addOption(Option.builder("h").longOpt("help")
				.desc("Help")
				.build());
		
		return options;
	}

	boolean parseOptions(Options options,String[] args){

		CommandLineParser parser = new DefaultParser();

		try {

			CommandLine cmd = parser.parse(options, args);

			path = cmd.getOptionValue("p");
			showAliveAfterFixed = cmd.hasOption("p");
			VERBOSE = cmd.hasOption("v");
			help = cmd.hasOption("h");

		} catch (Exception e) {
			printHelp(options);
			return false;
		}

		return true;
	}

	private void printHelp(Options options) {
		// automatically generate the help statement
		HelpFormatter formatter = new HelpFormatter();
		String header = "Execute ResultAnalayzer to analyze the results from PMDExperimenter.";
		String footer ="";
		formatter.printHelp("java -cp \"[path_to_lib]/lib/*\" edu.handong.csee.isel.pmd.utils.ResultAnalyzer", header, options, footer, true);
	}
}
