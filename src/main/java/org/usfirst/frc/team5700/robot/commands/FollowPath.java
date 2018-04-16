package org.usfirst.frc.team5700.robot.commands;

import java.io.File;

import org.usfirst.frc.team5700.robot.Robot;
import org.usfirst.frc.team5700.robot.subsystems.Drivetrain;

import edu.wpi.first.wpilibj.Preferences;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.command.Command;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import jaci.pathfinder.Pathfinder;
import jaci.pathfinder.Trajectory;
import jaci.pathfinder.Waypoint;
import jaci.pathfinder.followers.DistanceFollower;
import jaci.pathfinder.modifiers.TankModifier;

public class FollowPath extends Command {

	public static double kP;
	public static double kI;
	public static double kD;
	public static double kF = 1 / Drivetrain.kMaxSpeed;
	public static double kA = 0;
	public static double kAngleP;
	private Trajectory trajectory;
	private TankModifier modifier;
	private DistanceFollower left;
	private DistanceFollower right;
	private Drivetrain m_drive;
	private Preferences m_prefs;
	private Timer m_timer;
	private double m_angleError;
	private double m_lastAngleError;
	private double kAngleD;


	public FollowPath(Waypoint[] points, double maxSpeed, String fileName) {

		m_timer = new Timer();
		m_prefs = Robot.prefs;
		m_drive = Robot.drivetrain;

		Trajectory.Config config = new Trajectory.Config(Trajectory.FitMethod.HERMITE_CUBIC, 
				Trajectory.Config.SAMPLES_HIGH, 
				0.02, 
				maxSpeed, 
				Drivetrain.kMaxAccel, 
				Drivetrain.kMaxJerk);

		trajectory = Pathfinder.generate(points, config);
		modifier = new TankModifier(trajectory).modify(Drivetrain.kWheelBaseWidth);

		left = new DistanceFollower(modifier.getLeftTrajectory());
		right = new DistanceFollower(modifier.getRightTrajectory());

		File myFile = new File("/home/lvuser/pathfinder/" + fileName + ".csv");
		Pathfinder.writeToCSV(myFile, trajectory);

	}
	
	public FollowPath(Waypoint[] points, String fileName) {
		this(points, Drivetrain.kMaxSpeed, fileName);
	}
	
	public FollowPath(Waypoint[] points) {
		this(points, "FollowPath");
	}

	public FollowPath() {
		this(points());
	}

	private static Waypoint[] points() {
		Waypoint[] points = new Waypoint[] {
				new Waypoint(0, 0, 0),
				new Waypoint(72, 24, Pathfinder.d2r(45))	// Convert radians to degrees: Pathfinder.d2r(45)
		};

		return points;
	}

	protected void initialize() {
		m_drive.resetSensors();
		m_timer.reset();
		m_timer.start();
		kP = m_prefs.getDouble("Pathfinder/kP", 0.45);
		m_prefs.putDouble("Pathfinder/kP", kP);
		kD = m_prefs.getDouble("Pathfinder/kD", 0.01);
		m_prefs.putDouble("Pathfinder/kD", kD);
		kAngleP = m_prefs.getDouble("Pathfinder/kAngleP", 0.05);
		m_prefs.putDouble("Pathfinder/kAngleP", kAngleP);
		kAngleD = m_prefs.getDouble("Pathfinder/kAngleD", 0.0);
		m_prefs.putDouble("Pathfinder/kAngleD", kAngleD);

		// The first argument is the proportional gain. Usually this will be quite high
		// The second argument is the integral gain. This is unused for motion profiling
		// The third argument is the derivative gain. Tweak this if you are unhappy with the tracking of the trajectory
		// The fourth argument is the velocity ratio. This is 1 over the maximum velocity you provided in the 
		// trajectory configuration (it translates m/s to a -1 to 1 scale that your motors can read)
		// The fifth argument is your acceleration gain. Tweak this if you want to get to a higher or lower speed quicker
		left.configurePIDVA(kP, kI, kD, kF, kA);
		right.configurePIDVA(kP, kI, kD, kF, kA);

		m_drive.resetSensors();
		left.reset();
		right.reset();
	}

	protected void execute() {
		double leftMotorOutput = left.calculate(m_drive.getLeftEncoder().getDistance());
		double rightMotorOutput = right.calculate(m_drive.getRightEncoder().getDistance());

		double gyroHeading = - m_drive.getHeading();    // gyro is clockwise, pathfinder counter-clockwise
		double desiredHeading = Pathfinder.r2d(left.getHeading());  // Should also be in degrees
		SmartDashboard.putNumber("Pathfinder/desiredHeading", desiredHeading);

		m_angleError = Pathfinder.boundHalfDegrees(desiredHeading - gyroHeading);
		double angleErrorChange = m_lastAngleError - m_angleError;
		m_lastAngleError = m_angleError;
		SmartDashboard.putNumber("Pathfinder/angleError", m_angleError);
		SmartDashboard.putNumber("Pathfinder/angleErrorChange", angleErrorChange);

		//		System.out.println("Pathfinder at " + m_timer.get() + ", output: " + leftMotorOutput);
		m_drive.boostedTankDrive(leftMotorOutput - (kAngleP * m_angleError - kAngleD * angleErrorChange), 
				rightMotorOutput + (kAngleP * m_angleError - kAngleD * angleErrorChange));
	}

	protected boolean isFinished() {
		return left.isFinished() && right.isFinished();
	}

	protected void end() {
		m_timer.stop();
	}

	protected void interrupted() {
		end();
	}
}
