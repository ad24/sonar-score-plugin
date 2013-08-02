package com.catalyst.sonar.score.batch;

import static com.catalyst.sonar.score.log.Logger.LOG;
import static org.sonar.api.resources.Scopes.PROJECT;

import com.catalyst.sonar.score.dao.PropertyDao;
import com.catalyst.sonar.score.metrics.ScoreMetrics;
import com.catalyst.sonar.score.util.CalculationComponent;
import com.catalyst.sonar.score.util.CalculationComponent.CalculationComponentList;

import org.sonar.api.batch.Decorator;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.batch.DependedUpon;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.MeasureUtils;
import org.sonar.api.measures.Metric;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.resources.ResourceUtils;

import java.util.Arrays;
import java.util.Collection;

import com.google.common.collect.ImmutableList;

import org.sonar.api.batch.DependsUpon;
import org.sonar.api.config.Settings;
import org.sonar.api.database.DatabaseSession;
import org.sonar.api.database.configuration.Property;
/**
 * The ScoreDecorator class is responsible for obtaining, analyzing and calculating information and 
 * various code metrics for the points metric
 * @author Team Build Meister
 *
 */
public class PointsDecorator implements Decorator {
	
	public static final double MAGNIFY_PACKAGE_TANGLE = 100;
	
	private PropertyDao propertyDao;
	private Project project;
	
	public PointsDecorator(DatabaseSession session, Project project,
			Settings settings) {
		this.propertyDao = new PropertyDao(session);
		this.project = project;

	}
	
	private boolean isProject(@SuppressWarnings("rawtypes") Resource resource) {
		return PROJECT.equals(resource.getScope());
	}
	
	/**
	 * Checks to see if point earning is disabled for this project or globally.
	 * @return
	 */
	private boolean pointsAreDisabled(@SuppressWarnings("rawtypes") Resource resource) {
		boolean disabled = false;
		LOG.beginMethod("pointsAreDisabled", isProject(resource));
		LOG.log("Project Scope = " + resource.getScope());
		LOG.log("Project Name = " + resource.getName());
		Property projectProperty = propertyDao.getForResource(PropertyDao.POINTS_DISABLED, project);
		Property globalProperty = propertyDao.getForGlobal(PropertyDao.POINTS_DISABLED);
		if(projectProperty == null) {
			LOG.log("projectProperty = " + projectProperty);
			if(globalProperty == null) {
				LOG.log("globalProperty = " + globalProperty);
				LOG.returning(false).endMethod();
				return false;
			} else {
				LOG.log("globalProperty = " + globalProperty.getValue());
				disabled = globalProperty.getValue().equals(Boolean.toString(true));
				LOG.returning("globalProperty = " + disabled).endMethod();
				return disabled;
			}
		} else {
			disabled = projectProperty.getValue().equals(true);
			LOG.returning("projectProperty = " + disabled).endMethod();
			return disabled;
		}
	}
	
	/**
	 * @DependsUpon: The points metric depends upon the non-commented lines of code, the rules compliance 
	 * (violations density), unit test coverage, documented API, and package tangle measures to be calculated 
	 * first before the points measure can be calculated.
	 * @returns an immutable list of the above mentioned code metrics
	 */
	@DependsUpon
	public Collection<Metric> usedMetrics(){
		return ImmutableList.of(CoreMetrics.PACKAGES, CoreMetrics.CLASSES, CoreMetrics.NCLOC,
				CoreMetrics.VIOLATIONS_DENSITY, CoreMetrics.PUBLIC_DOCUMENTED_API_DENSITY,
				CoreMetrics.COVERAGE, CoreMetrics.PACKAGE_TANGLE_INDEX );   
	}
  
	/**
	 * @DependedUpon: The points metric is calculated after the other metrics
	 *                that is depends upon are calculated first.
	 * @returns a list of point metrics
	 */

	@DependedUpon
	public Collection<Metric> generatedMetrics() {
		return Arrays.asList(ScoreMetrics.POINTS);
	}

	/**
	 * returns whether or not analysis should be ran on a particular project
	 */
	public boolean shouldExecuteOnProject(Project project) {
		return true;
	}

	/**
	 * @param resource
	 * @param context
	 * @returns whether or not a resource is a unit test class
	 */
	public boolean shouldDecorateResource(@SuppressWarnings("rawtypes") final Resource resource,
			final DecoratorContext context) {
		// if the resource is not a unit test class, then proceed with
		// decoration/analysis
		return !ResourceUtils.isUnitTestClass(resource) && !pointsAreDisabled(resource);
	}

	/**
	 * Calculates the points metric and saves it to the database. First, all the
	 * necessary measures are retrieved in order to calculate the points metric
	 * for a particular project.
	 */
	public void decorate(@SuppressWarnings("rawtypes") final Resource resource, final DecoratorContext context) {
		if("PRJ".equals(resource.getScope())) {
			LOG.beginMethod("PointsDecorator.decorate()");
			LOG.log("Point Earning is " + ((pointsAreDisabled(resource)) ? "dis" : "en") + "abled.");
		}
		/*
		 * if the resource to decorate is not a unit test class, then retrieve
		 * the various code metrics and calculate the points for a given
		 * resource/project
		 */
		if (shouldDecorateResource(resource, context)) {
			double value = getPointsValue(context);

			// save the the point's value to the database for the given
			// resource/project
			context.saveMeasure(ScoreMetrics.POINTS, value);
		}

	}

	/**
	 * Retrieves all the necessary code metrics in order to calculate SCORE's
	 * points metric
	 * 
	 * @param context
	 * @returns the points value
	 */
	public double getPointsValue(final DecoratorContext context) {
		double packages = MeasureUtils.getValue(
				context.getMeasure(CoreMetrics.PACKAGES), 0.0); 
		double classes = MeasureUtils.getValue(
				context.getMeasure(CoreMetrics.CLASSES), 0.0);
		double ncloc = MeasureUtils.getValue(
				context.getMeasure(CoreMetrics.NCLOC), 0.0);
		double rulesCompliance = MeasureUtils.getValue(
				context.getMeasure(CoreMetrics.VIOLATIONS_DENSITY), 0.0);
		double docAPI = MeasureUtils.getValue(
				context.getMeasure(CoreMetrics.PUBLIC_DOCUMENTED_API_DENSITY),
				0.0);
		double coverage = MeasureUtils.getValue(
				context.getMeasure(CoreMetrics.COVERAGE), 0.0);
		double packageTangle = MeasureUtils.getValue(
				context.getMeasure(CoreMetrics.PACKAGE_TANGLE_INDEX), 0.0);
		CalculationComponentList penalties = new CalculationComponentList();
		CalculationComponent packageTanglePenalty = new CalculationComponent(packageTangle, MAGNIFY_PACKAGE_TANGLE);
		penalties.add(packageTanglePenalty);
		return new PointsCalculator(penalties, null).calculateTotalPoints(packages, classes, ncloc, rulesCompliance, docAPI, coverage, packageTangle);
	}
	
}
