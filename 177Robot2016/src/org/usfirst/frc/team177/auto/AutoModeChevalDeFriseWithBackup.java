package org.usfirst.frc.team177.auto;

import org.usfirst.frc.team177.robot.*;
import org.usfirst.frc.team177.robot.Catapult.catapultStates;

import edu.wpi.first.wpilibj.DoubleSolenoid;

public class AutoModeChevalDeFriseWithBackup extends AutoMode {
	
	enum AutoStates {
		DriveForward,
		BackupALittle,
		PutPickupDown,
		Turn,
		Pause,
		Aim,
		PauseBeforeFire,
		WaitForAim,
		Fire,
		Stop		
	}
	
	private AutoStates state;
	long lastEventTime;
	int driveCount; //iterates so that I dont have duplicate cases
	//Constants
	private static final double firstDriveForwardDelay = 2000;
	private static final double backupDelay = 100;
	private static final double pickupDownDelay = 1000;
	private static double secondDriveForwardDelay = 4000;
	private static final double pauseBeforeFireDelay = 500;
	
	private static final double[] turnAngles = {30, 10, -15, -20}; //guesses
	private static final int turnTimeout = 3000;
	Robot.Turns turn;	
    double turnAngle = 0;
	
    boolean fireNow = false;
    boolean aimNow = false;
    
    boolean fireAfterCrossing;
    
    
	public AutoModeChevalDeFriseWithBackup(Robot robot, boolean fireAfterCrossing, Robot.Turns turn) {
		super(robot);
		this.fireAfterCrossing = fireAfterCrossing;
		this.turn = turn;
		if(turn != Robot.Turns.NoTurn)
		{
			turnAngle = turnAngles[turn.getIndex()];
			if (turn == Robot.Turns.TurnFrom5) {
				secondDriveForwardDelay += 1000;
			}
		}
		System.out.println("AutoModeChevalDeFrise Constructor");
	}
	
	public void autoInit() {
		lastEventTime = 0;
		state = AutoStates.DriveForward;
		driveCount = 1;
		robot.shiftPneumatic.set(false); //Low Gear
	}
	
	public void autoPeriodic() {
		switch(state)
		{
		case DriveForward:
			if (lastEventTime == 0) {
				lastEventTime = System.currentTimeMillis();
			}
			if (driveCount == 1) { //first driveforward
				robot.drive.tankDrive(-0.75, -0.70);
				if (System.currentTimeMillis() - lastEventTime > firstDriveForwardDelay) {
					robot.drive.tankDrive(0, 0);
					lastEventTime = 0;
					driveCount++;
					state = AutoStates.PutPickupDown;
				} 
			} else { //second driveforward
				robot.drive.tankDrive(-0.75,-0.7);
				if (System.currentTimeMillis() - lastEventTime > secondDriveForwardDelay) {
					robot.drive.tankDrive(0,0);
					lastEventTime = 0;
					robot.shiftPneumatic.set(true); // High gear
					if (fireAfterCrossing) {
						if (turn == Robot.Turns.NoTurn)
						{
							state = AutoStates.Pause;	
						}
						else
						{							
							state = AutoStates.Turn;
						}
					} else {
						state = AutoStates.Stop;
					}
				}
			}
			break;
		case BackupALittle:
			if(lastEventTime == 0) { 
				lastEventTime = System.currentTimeMillis();
			}		
			robot.drive.tankDrive(0.75, 0.70);
			if(System.currentTimeMillis() - lastEventTime > backupDelay) {
				robot.drive.tankDrive(0,0);
				lastEventTime = 0;
				state = AutoStates.DriveForward;
			}
			break;
		case PutPickupDown:
			if (lastEventTime == 0) {
				lastEventTime = System.currentTimeMillis();
			}
			robot.transferPneumatic.set(DoubleSolenoid.Value.kReverse);
			robot.drive.tankDrive(0, 0);
			if (System.currentTimeMillis() - lastEventTime > pickupDownDelay) {	
				lastEventTime = 0;
				state = AutoStates.BackupALittle;				
			}
			break;
			
		case Turn:
			if(lastEventTime == 0) { 
				lastEventTime = System.currentTimeMillis();
			}
			
			if (turnAngle > 0) 
			{
				// turn right
    			robot.drive.tankDrive(-0.75,0.75);
    			if(robot.locator.GetHeading() > turnAngle || System.currentTimeMillis() - lastEventTime > turnTimeout) {
    				robot.drive.tankDrive(0,0);
    				lastEventTime = 0;
    				state = AutoStates.Pause;
    			}
			}
			else
			{
				//turn left
				robot.drive.tankDrive(0.75,-0.75);
    			if((robot.locator.GetHeading() < (360 + turnAngle) && (robot.locator.GetHeading() > 180)) || System.currentTimeMillis() - lastEventTime > turnTimeout) {
    				robot.drive.tankDrive(0,0);
    				lastEventTime = 0;
    				state = AutoStates.Pause;
    			}
			}
			break;	
			
		case Pause:
			if(lastEventTime == 0) { 
				lastEventTime = System.currentTimeMillis();
			}		
			if(System.currentTimeMillis() - lastEventTime > 1000) {
				robot.drive.tankDrive(0,0);
				lastEventTime = 0;
				state = AutoStates.Aim;
			}
			break;
		case Aim:
			//if (robot.vision.getBearing() != robot.vision.BAD_BEARING)
			{
				aimNow = true;
				state = AutoStates.WaitForAim;
			}
			/*else
			{
				state = AutoStates.Stop;
			}*/
			break;
		case WaitForAim:
			aimNow = false;
			if(robot.catapult.getState() == catapultStates.ReadyToFire)
			{
				state = AutoStates.Fire;
			}
			break;

		case Fire:	
			if (lastEventTime == 0) { 
				lastEventTime = System.currentTimeMillis();
			}	
			
			if (System.currentTimeMillis() - lastEventTime > pauseBeforeFireDelay) 
			{
				fireNow = true;
				lastEventTime = 0;
				state = AutoStates.Stop;
			}
			break; 
	
		case Stop:
		default:
			robot.drive.tankDrive(0, 0);
			fireNow = false;
			break;
		}
		

    	robot.catapult.loop(fireNow, aimNow);
	}

	@Override
	public String GetColumNames() {
		return "state";
	}

	@Override
	public String getName() {
		return String.format("%s", state.toString());
	}
}