package edu.handong.csee.isel.pmd;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class PMDExperimenter {

	public static void main(String[] args) {
		PMDExperimenter experimenter = new PMDExperimenter();
		experimenter.run(args);
	}

	private void run(String[] args) {

		String srcDir = args[0];
		
		String pmdCommand = System.getProperty("os.name").contains("Windows")?"pmd.bat":"pmd";

		Runtime rt = Runtime.getRuntime();
		try {
			System.out.println("PMD");
			Process p = rt
					.exec(pmdCommand + " -d " + srcDir + " -f csv -R category/java/errorprone.xml/DataflowAnomalyAnalysis");

			ArrayList<DetectionRecord> detectionResults = new ArrayList<DetectionRecord>();
			// create a thread that deals with output
			new Thread(new Runnable() {
				public void run() {
					BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));

					try {
						String line = input.readLine(); // ignore header
						while ((line = input.readLine()) != null)
							detectionResults.add(new DetectionRecord(line));
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}).start();

			p.waitFor();
			
			ArrayList<DetectionRecord> filteredRecords = filterByInterest(detectionResults);

			printResults(filteredRecords);

		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private ArrayList<DetectionRecord> filterByInterest(ArrayList<DetectionRecord> detectionResults) {
		
		ArrayList<DetectionRecord> filteredRecords = new ArrayList<DetectionRecord>();
		
		for(DetectionRecord decRec:detectionResults) {
			
			if(decRec.rule.equals("DataflowAnomalyAnalysis")) {
				if(decRec.description.startsWith("Found 'DD'-anomaly")) {
					filteredRecords.add(decRec);
				}
			}
		}
		
		return filteredRecords;
	}

	private void printResults(ArrayList<DetectionRecord> detectionResults) {

		for (DetectionRecord decRec : detectionResults) {
			decRec.showSummary();
		}

	}
}
