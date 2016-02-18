
package org.usfirst.frc.team177.robot;

import edu.wpi.first.wpilibj.IterativeRobot;
import edu.wpi.first.wpilibj.Joystick;
import edu.wpi.first.wpilibj.RobotDrive;
import edu.wpi.first.wpilibj.Solenoid;
import edu.wpi.first.wpilibj.Victor;
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj.DigitalInput;

/**
 * The VM is configured to automatically run this class, and to call the
 * functions corresponding to each mode, as described in the IterativeRobot
 * documentation. If you change the name of this class or the package after
 * creating this project, you must also update the manifest file in the resource
 * directory.
 */
public class Robot extends IterativeRobot {
    final String doNothing = "Default";
    final String driveForward = "My Auto";
    String autoSelected;
    SendableChooser chooser;
	
    
    /**Motor constants**/
	private static final int MotorDriveRL = 3;//Rear Left 888
	private static final int MotorDriveFL = 2; //Front Left 888
	private static final int MotorDriveRR = 1; //Rear Right 888
	private static final int MotorDriveFR = 0; //Front Right 888
	
	private static final int MotorRollerTop = 4; //Top Roller 888
	private static final int MotorRollerSide = 5; //Side Roller 888
	
	private static final int MotorClimb = 6; //Climb Motor 888
	/**Initialize Victors**/
	Victor rearLeftMotor = new Victor(MotorDriveRL);
	Victor frontLeftMotor = new Victor(MotorDriveFL);
	    
	Victor rearRightMotor = new Victor(MotorDriveRR);
	Victor frontRightMotor = new Victor(MotorDriveFR); 
	
	Victor rollerTopMotor = new Victor(MotorRollerTop);
	Victor rollerSideMotor = new Victor(MotorRollerSide);
	
	Victor climbMotor = new Victor(MotorClimb);
	
	/**Joysticks**/    
	Joystick leftStick = new Joystick(0);
	Joystick rightStick = new Joystick(1);
	Joystick operatorStick = new Joystick(2);
	Joystick switchPanel = new Joystick(3);
	
	public RobotDrive drive = new RobotDrive(frontLeftMotor, rearLeftMotor, frontRightMotor, rearRightMotor);
	
	/** Joystick Constants **/ //Magic Numbers found in Joystick.class
    private static final int axisY = 1;
    
    /** Solenoids **/
	//SAFETY: At the end of the match both the latch and the pusher should be out
	public Solenoid latchPneumatic = new Solenoid(1); //false = out
	public Solenoid pusherPneumatic = new Solenoid(2); //false = out
	public Solenoid transferPneumatic = new Solenoid(3); //false = out
	public Solenoid shiftPneumatic = new Solenoid(0);
	
    /** Digital Input **/
    //DigitalInput ballIRSwitch = new DigitalInput(); //RIP IR, died 2/11/16 at the hands of Ulf's SuperAwesome piece of Lexan
    DigitalInput readyToFireLimitSwitchA = new DigitalInput(2);
    DigitalInput readyToFireLimitSwitchB = new DigitalInput(3);
    DigitalInput leftDriveEncoder = new DigitalInput(4);
    //Pin 5 is power for the leftDriveEncoder
    DigitalInput rightDriveEncoder = new DigitalInput(6);
    //Pin 7 is power for the rightDriveEncoder
    
    /**Enums**/
    enum catapultStates {
    	NoBall,
    	Pickup,
    	BallsIn,
    	PreparingToFire,
    	ReadyToFire
    };
    enum pickupStates {
    	BallAcquired,
    	TransferUp,
    	DropBall,
    	TransferDown
    };
    
    //State Machine Shooter
    catapultStates catapultState = catapultStates.NoBall;
    double afterFiringDelay = 1000; //ms
    double latchOutDelay = 1000; //ms
    double pusherInDelay = 1000; //ms
    long lastShooterEventTime = 0;
    
    //State Machine Pickup
    pickupStates pickupState = pickupStates.BallAcquired;
    
    //State Machine Auto
    long lastDriveForwardEventTime = 0;
    double driveForwardDelay = 3000;
    
    //Controller Mapping
    //Controller
    private static final int ButtonTransfer = 8;
    private static final int ButtonSideRollers = 7;
    //Right Joystick
    private static final int ButtonShift = 3;
    //Left Joystick
    
    
    /**
     * This function is run when the robot is first started up and should be
     * used for any initialization code.
     */
    public void robotInit() {
        chooser = new SendableChooser();
        chooser.addDefault("Do Nothing", doNothing);
        chooser.addObject("Drive Forward", driveForward);
        SmartDashboard.putData("Auto choices", chooser);
        transferPneumatic.set(true);
    }
    
	/**
	 * This autonomous (along with the chooser code above) shows how to select between different autonomous modes
	 * using the dashboard. The sendable chooser code works with the Java SmartDashboard. If you prefer the LabVIEW
	 * Dashboard, remove all of the chooser code and uncomment the getString line to get the auto name from the text box
	 * below the Gyro
	 *
	 * You can add additional auto modes by adding additional comparisons to the switch structure below with additional strings.
	 * If using the SendableChooser make sure to add them to the chooser code above as well.
	 */
    
    public void autonomousInit() {
    	autoSelected = (String) chooser.getSelected();
		autoSelected = SmartDashboard.getString("Auto Selector", doNothing);
		System.out.println("Auto selected: " + autoSelected);
    }

    /**
     * This function is called periodically during autonomous
     */
    public void autonomousPeriodic() {
    	switch(autoSelected) {
    	case driveForward:
    		if(lastDriveForwardEventTime == 0) { 
    			lastDriveForwardEventTime = System.currentTimeMillis();
    		}
    		drive.tankDrive(.75,.75);
    		if(System.currentTimeMillis() - lastDriveForwardEventTime > driveForwardDelay) {
    			drive.tankDrive(0,0);
    			lastDriveForwardEventTime = 0;
    		}
            break;
    	case doNothing:
    	default:
    		//Do Nothing
            break;
    	}
    }
    @SuppressWarnings("unused")
	public void teleopInit() {
    	catapultStates catapultState = catapultStates.BallsIn;
    	pickupStates pickupState = pickupStates.BallAcquired;
    }
    /**
     * This function is called periodically during operator control
     */
    public void teleopPeriodic() {
    	//Driving
    	double left = leftStick.getRawAxis(axisY);
		double right = rightStick.getRawAxis(axisY);
		drive.tankDrive(left, right);
		shiftPneumatic.set(rightStick.getRawButton(ButtonShift));
		
    	transferPneumatic.set(operatorStick.getRawButton(ButtonSideRollers));
		if(operatorStick.getRawButton(ButtonTransfer)) {
			rollerSideMotor.set(1);
		}
		else {
			rollerSideMotor.set(0);
		}
		rollerTopMotor.set(operatorStick.getRawAxis(1) * -1); //Left Stick, y axis
	
		if(switchPanel.getRawButton(1))
		{
			latchPneumatic.set(switchPanel.getRawButton(2));
			pusherPneumatic.set(switchPanel.getRawButton(3));
			catapultState = catapultStates.BallsIn;
		}
		else
		{
			//Firing State Machine
			switch (catapultState)
			{
			case NoBall:  //fire
				if(lastShooterEventTime == 0) { 
					lastShooterEventTime = System.currentTimeMillis();
				}
				latchPneumatic.set(true); //unlatched
				pusherPneumatic.set(true); //in
				if(System.currentTimeMillis() - lastShooterEventTime > latchOutDelay) {
					catapultState = catapultStates.Pickup;
					lastShooterEventTime = 0;
				}
				break;
			case Pickup:
				latchPneumatic.set(true);  //unlatched 
				pusherPneumatic.set(false); //out
				
				/**if(readyToFireLimitSwitchA.get() || readyToFireLimitSwitchB.get()){
					catapultState = catapultStates.BallsIn;
				}**/
				if(operatorStick.getRawButton(4)) {
					catapultState = catapultStates.BallsIn;
				}
				break;
			case BallsIn:
				if(lastShooterEventTime == 0) { 
					lastShooterEventTime = System.currentTimeMillis();
				}
				latchPneumatic.set(false); //latch
				pusherPneumatic.set(false); //out
				if(System.currentTimeMillis() - lastShooterEventTime > latchOutDelay) {
					catapultState = catapultStates.PreparingToFire;
					lastShooterEventTime = 0;
				}
				break;
			case PreparingToFire:
				if(lastShooterEventTime == 0) { 
					lastShooterEventTime = System.currentTimeMillis();
				}
				latchPneumatic.set(false); //lastched
				pusherPneumatic.set(true); //in
				if(System.currentTimeMillis() - lastShooterEventTime > latchOutDelay) {
					catapultState = catapultStates.ReadyToFire;
					lastShooterEventTime = 0;
				}
				break;
			case ReadyToFire:
				if(operatorStick.getRawButton(1)) {
					catapultState = catapultStates.NoBall;
				}
				break;
			default:
				break;
			}
		}
		//Pickup State Machine
		switch(pickupState)
		{
		case BallAcquired:
			if(operatorStick.getRawButton(2)) {
				pickupState = pickupStates.TransferUp;
			}
			break;
		case TransferUp:
			transferPneumatic.set(false); //out
			pickupState = pickupStates.DropBall;
			break;
		case DropBall:
			rollerTopMotor.set(operatorStick.getRawAxis(3));
			rollerSideMotor.set(operatorStick.getRawAxis(3) / 2);  //Scaling for the side motors
			if(operatorStick.getRawButton(2)) {
				pickupState = pickupStates.TransferDown;
			}
			break;
		case TransferDown:
			transferPneumatic.set(true); //in
			pickupState = pickupStates.BallAcquired;
			break;
		default:
			break;
		}

		//MISSILE SWITCH OVERRIDE
		if(switchPanel.getRawButton(4) && operatorStick.getRawButton(1)) { //This is done so that if the missile switch is fired the driver can fire.  Even if it is a terrible,terrible idea
			catapultState = catapultStates.NoBall;
		}		
    }
    
    /**
     * This function is called periodically during test mode
     */
    public void testPeriodic() {
    
    }
    
}
