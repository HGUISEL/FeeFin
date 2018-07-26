package edu.handong.csee.isel.pmd;

import static org.junit.Assert.*;

import org.junit.Test;

public class PMDExperimenterTest {

	@Test
	public void test() {
		String[] args = {"-d", "C:\\Users\\jaech\\git\\FeeFin"};
		PMDExperimenter.main(args);
	}
	
	@Test
	public void testApexCore() {
		String[] args = {"-d", "C:\\Users\\jaech\\git\\apex-core", "-s", "2016-07-15 17:26:35", "-e","2016-07-19 05:48:13","-v"};
		PMDExperimenter.main(args);
	}

}
