package se.vti.utils.matsim.assignmentcooling;

import org.matsim.core.config.ReflectiveConfigGroup;

/**
 * @author GunnarF
 */

public class AssignmentCoolingConfigGroup extends ReflectiveConfigGroup {

	public static final String GROUP_NAME = "assignmentCooling";

	public AssignmentCoolingConfigGroup() {
		super(GROUP_NAME);
	}

	private int burnInIterations = 0;

	@StringSetter("burnInIterations")
	public void setBurnInIterations(int burnInIterations) {
		this.burnInIterations = burnInIterations;
	}

	@StringGetter("burnInIterations")
	public int getBurnInIterations() {
		return this.burnInIterations;
	}
	
	private double innovationIterationExponent = 0.5;

	@StringSetter("innovationIterationExponent")
	public void setInnovationIterationExponent(double innovationIterationExponent) {
		this.innovationIterationExponent = innovationIterationExponent;
	}

	@StringGetter("innovationIterationExponent")
	public double getInnovationIterationExponent() {
		return this.innovationIterationExponent;
	}
}
