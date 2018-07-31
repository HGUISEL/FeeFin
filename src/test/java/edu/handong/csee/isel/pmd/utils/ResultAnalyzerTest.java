package edu.handong.csee.isel.pmd.utils;

import static org.junit.Assert.*;

import org.junit.Test;

public class ResultAnalyzerTest {

	@Test
	public void test() {
		String[] args = {"-p", "data/apex-core-new.csv"};
		ResultAnalyzer.main(args);
	}

}
