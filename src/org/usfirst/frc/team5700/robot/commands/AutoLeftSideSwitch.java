package org.usfirst.frc.team5700.robot.commands;

import edu.wpi.first.wpilibj.command.CommandGroup;

/**
 *
 */
public class AutoLeftSideSwitch extends CommandGroup {

    public AutoLeftSideSwitch() {
    		addParallel(new DrivePastDistance(156, 0.5, true, true));
    		addParallel(new MoveArmToAngle(90));
    		addSequential(new AutoDropCube());
    		addSequential(new MoveArmToAngle(0));
    }
}
