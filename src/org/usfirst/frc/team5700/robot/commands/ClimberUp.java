package org.usfirst.frc.team5700.robot.commands;

import org.usfirst.frc.team5700.robot.Robot;

import edu.wpi.first.wpilibj.command.Command;

/**
 *
 */
public class ClimberUp extends Command {
	
    public ClimberUp() {
    		requires(Robot.climber);
       
    }

    protected void initialize() {
    }

    protected void execute() {
    	Robot.climber.up((Robot.oi.getAuxLeftStick().getThrottle() + 1)/2.0);
    }

    protected boolean isFinished() {
        return false;
    }

    protected void end() {
    	Robot.climber.stop();
    }

    protected void interrupted() {
    		end();
    }
}
