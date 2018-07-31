package edu.handong.csee.isel.pmd;

import static org.junit.Assert.*;

import org.junit.Test;

public class PMDExperimenterTest {

	@Test
	public void test() {
		String[] args = {"-d", "C:\\Users\\jaech\\git\\FeeFin", "-n", "test"};
		PMDExperimenter.main(args);
	}
	
	@Test
	public void testApexCore() {
		String[] args = {"-d", "C:\\Users\\jaech\\git\\apex-core", "-s", "2016-07-15 17:26:35", "-e","2016-07-19 05:48:13","-v","-n", "test"};
		PMDExperimenter.main(args);
	}
	
	@Test
	public void testApexCoreAIOOBE() {
		String[] args = {"-d", "C:\\Users\\jaech\\git\\apex-core", "-s", "2012-12-02 22:02:55", "-e","2014-12-02 23:38:48","-v","-n", "test"};
		PMDExperimenter.main(args);
	}
	
	@Test
	public void testStromStuckIssue() {
		String[] args = {"-d", "C:\\Users\\jaech\\git\\storm", "-s", "2018-01-01 04:34:22", "-e","2018-01-23 04:34:22","-v","-n", "storm"};
		PMDExperimenter.main(args);
	}
	
	@Test
	public void testApexCoreWrongFixedCase() {
		String[] args = {"-d", "C:\\Users\\jaech\\git\\apex-core", "-s", "2015-06-27 22:02:55", "-e","2015-06-28 17:15:26","-n", "test"};
		PMDExperimenter.main(args);
	}
	
}
