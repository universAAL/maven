/*******************************************************************************
 * Copyright 2013 Universidad Politécnica de Madrid
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.universAAL.support.directives.mojos;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.doxia.siterenderer.Renderer;
import org.apache.maven.plugin.AbstractMojoExecutionException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.reporting.AbstractMavenReport;
import org.apache.maven.reporting.MavenReportException;
import org.universAAL.support.directives.api.APICheck;
import org.universAAL.support.directives.checks.DecoupleCheck;
import org.universAAL.support.directives.checks.DependencyManagementCheckFix;
import org.universAAL.support.directives.checks.MavenCoordinateCheck;
import org.universAAL.support.directives.checks.ModulesCheckFix;
import org.universAAL.support.directives.checks.ParentGForgePropertyCheck;
import org.universAAL.support.directives.checks.SVNCheck;
import org.universAAL.support.directives.checks.SVNIgnoreCheck;

/**
 * This Mojo executes all checks upon a project.
 * It outputs the result as a report in the site.
 * @author amedrano
 * @goal check-report
 * @phase site
 * @see DecoupleCheckMojo
 * @see DependencyManagementCheckMojo
 * @see ModulesCheckMojo
 * @see NamingCheckMojo
 * @see ParentGForgePropertyCheckMojo
 * @see SVNCheckMojo
 * @see SVNIgnoreCheckMojo
 */
public class CheckReportMojo extends AbstractMavenReport {
	
	private APICheck [] checks;
	
    /**
     * Directory where reports will go.
     *
     * @parameter expression="${project.reporting.outputDirectory}"
     * @required
     * @readonly
     */
    private String outputDirectory;
 
    /**
     * The maven proyect.
     * @parameter default-value="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;
 
    /**
     * Doxia Site Renderer component.
     * @Component
     */
    protected Renderer siteRenderer;
    
    /**
     * The projects in the reactor.
     *
     * @parameter expression="${reactorProjects}"
     * @readonly
     */
    private List<MavenProject> reactorProjects;
   

    /** 
     * The ProyectBuilder to build children modules.
     * @component 
     */
	private MavenProjectBuilder mavenProjectBuilder;
	
	/**
	 * The localRepository reference, necessary to build projects.
	 * @parameter default-value="${localRepository}" 
	 */
	private ArtifactRepository localRepository;
	
	private int myFailedTests;

	private Set<MavenProject> FailedModules;
			
			

	/** {@inheritDoc} */
	@Override
	protected void executeReport(Locale loc) throws MavenReportException {
		
		APICheck [] cs	= {
				new ModulesCheckFix(),
				new DependencyManagementCheckFix(mavenProjectBuilder, localRepository),
				new ParentGForgePropertyCheck(),
				new MavenCoordinateCheck(),
				new SVNCheck(),
				new SVNIgnoreCheck(),
				new DecoupleCheck(),
			};
		checks = cs;
		myFailedTests = 0;
		FailedModules = new HashSet<MavenProject>();
		
		Sink sink = getSink();
		sink.head();
	    sink.title();
	    sink.text(getName(loc));
	    sink.title_();
	    sink.head_();
	    sink.body();
	    sink.section1();
	    sink.sectionTitle1();
	    sink.text(getName(loc));
	    sink.sectionTitle1_();
	    sink.lineBreak();
	    sink.text(getDescription(loc));
	    sink.lineBreak();

	    sink.table();
	    sink.tableRow();
	    sink.tableHeaderCell();
	    sink.text(getBundle(loc).getString("report.check.title"));
	    sink.tableHeaderCell_();

	    sink.tableHeaderCell();
	    sink.text(getBundle(loc).getString("report.check.status"));
	    sink.tableHeaderCell_();
	    sink.tableRow_();
	    renderMyTable(sink, loc);
	    sink.table_();
	    
	    sink.lineBreak();
	    sink.text("Passed " + Integer.toString(cs.length-myFailedTests) + " out of " + Integer.toString(cs.length) + " checks.");
	    
	    if (project.getPackaging().equals("pom")) {
	    	sink.sectionTitle2();
	    	sink.text("Check on Modules");
	    	sink.sectionTitle2_();
	    	
	    	sink.table();
		    sink.tableRow();
		    sink.tableHeaderCell();
		    sink.text(getBundle(loc).getString("report.check.module"));
		    sink.tableHeaderCell_();
		    
		    sink.tableHeaderCell();
		    sink.text(getBundle(loc).getString("report.check.title"));
		    sink.tableHeaderCell_();

		    sink.tableHeaderCell();
		    sink.text(getBundle(loc).getString("report.check.status"));
		    sink.tableHeaderCell_();
		    sink.tableRow_();
		    renderModulesTable(sink, loc);
		    sink.table_();
		    
		    sink.lineBreak();
		    sink.text(Integer.toString(reactorProjects.size()-1-FailedModules.size()) + " out of " + Integer.toString(reactorProjects.size()-1) + " modules are compliant.");
	    }
	    sink.section1_();
	    sink.body_();
	    sink.flush();
	    sink.close();
	}

	/**
	 * @param sink
	 * @param loc 
	 */
	private void renderModulesTable(Sink sink, Locale loc) {
		for (MavenProject mp : reactorProjects) {

			for (int i = 0; i < checks.length; i++) {
				if (!mp.equals(project)) {
					boolean passed;
					AbstractMojoExecutionException ex = null;
					try {
						passed = checks[i].check(mp, getLog());
					} catch (MojoExecutionException e) {
						passed = false;
						ex = e;
					} catch (MojoFailureException e) {
						passed = false;
						ex = e;
					} catch (Exception e) {
						passed = false;
						ex = new MojoExecutionException("Unexpected Exception", e);
					}
					// IF passed, do nothing if failed write row.
					if (!passed) {
						sink.tableRow();
						sink.tableCell();
//						sink.text(mp.getGroupId() + ":" + mp.getArtifactId());
						sink.text(mp.getArtifactId());
						sink.tableCell_();
						writeRow(checks[i], passed, ex, sink, loc);
						sink.tableRow_();
						FailedModules.add(mp);
					} 
				}
			}	
		}
	}

	/**
	 * @param sink
	 * @param loc the locale to use.
	 */
	private void renderMyTable(Sink sink, Locale loc) {
		for (int i = 0; i < checks.length; i++) {
			boolean passed;
			AbstractMojoExecutionException ex = null;
			try {
				passed = checks[i].check(project, getLog());
			} catch (MojoExecutionException e) {
				passed = false;
				ex = e;
			} catch (MojoFailureException e) {
				passed = false;
				ex = e;
			}
			
		    sink.tableRow();
		    writeRow(checks[i],passed, ex, sink, loc);
		    sink.tableRow_();
		    if (!passed) {
		    	myFailedTests++;
		    }
		}
	}
	
	private void writeRow(APICheck check, boolean passed, AbstractMojoExecutionException ex, Sink sink, Locale loc) {
	    sink.tableCell();
	    sink.text(check.getClass().getSimpleName());
	    sink.tableCell_();
	    if (passed) {
	    	sink.tableCell();
	    	sink.bold();
	    	sink.text(getBundle(loc).getString("report.passed"));
	    	sink.bold_();
	    	sink.tableCell_();
	    } else {
	    	sink.tableCell();
	    	sink.bold();
	    	if (ex instanceof MojoFailureException) {
	    		sink.text( getBundle(loc).getString("report.failed"));
	    	} else {
	    		sink.text( getBundle(loc).getString("report.failed.execution"));
	    	}
	    	sink.bold_();
	    	sink.lineBreak();
	    	if (ex != null) {
	    		sink.monospaced();
	    		sink.verbatim(true);
	    		sink.text(ex.getMessage());
	    		sink.lineBreak();
	    		sink.text(ex.getLongMessage());
	    		sink.verbatim_();
	    		sink.monospaced_();
	    	}
	    	sink.tableCell_();
	    }
		
	}

	/** {@inheritDoc} */
	public String getDescription(Locale loc) {
		return getBundle( loc ).getString( "report.description" );
	}

	/** {@inheritDoc} */
	public String getName(Locale loc) {
		return getBundle( loc ).getString( "report.name" );
	}

	/** {@inheritDoc} */
	public String getOutputName() {
		return getBundle( Locale.ENGLISH ).getString( "report.header" );
	}

	/** {@inheritDoc} */
	@Override
	protected String getOutputDirectory() {
		return outputDirectory;
	}

	/** {@inheritDoc} */
	@Override
	protected MavenProject getProject() {
		return project;
	}

	/** {@inheritDoc} */
	@Override
	protected Renderer getSiteRenderer() {
		return (Renderer) siteRenderer;
	}
	
	private ResourceBundle getBundle( Locale locale )
	{
	    return ResourceBundle.getBundle( "org.universAAL.support.directives.report",
	    		locale, this.getClass().getClassLoader() );
	}
}
