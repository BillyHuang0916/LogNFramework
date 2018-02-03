package NotVlad.components;

import com.ctre.phoenix.motorcontrol.ControlMode;
import com.ctre.phoenix.motorcontrol.FeedbackDevice;
import com.ctre.phoenix.motorcontrol.can.TalonSRX;

import NotVlad.MiyamotoControl;
import comms.SmartWriter;
import physicalOutput.motors.TalonSRXMotor;
import robot.Global;
import robot.IControl;

public class Lift extends IControl {
	private TalonSRX talon;
	
	private MiyamotoControl controller;
	private TalonSRXMotor motor;
	private int setPosition;
	
	public Lift(TalonSRX talon) {
		this.talon = talon;
		talon.configSelectedFeedbackSensor(FeedbackDevice.QuadEncoder, 0, 0);
		
		controller = (MiyamotoControl)Global.controllers;
		talon.setIntegralAccumulator(0.0,0,0);
		talon.setSelectedSensorPosition(0, 0, 0);
	}
	
	public Lift(TalonSRXMotor motor){
		this.motor = motor;
		this.setPosition = 0;
	}
	
	private void setLiftPosition(LiftPosition position){
		setPosition = position.getNumber();
	}
	
	private LiftPosition getCurrentPosition(){
		int current = motor.getTalon().getSelectedSensorPosition(0);
		int[] distances = new int[4];
		distances[0] = current-LiftPosition.BOTTOM.getNumber();
		distances[1] = current-LiftPosition.SWITCH.getNumber();
		distances[2] = current-LiftPosition.SCALE.getNumber();
		distances[3] = current-LiftPosition.CLIMB.getNumber();
		
		int minDistance = distances[0];
		int minIndex = 0;
		for(int i = 1; i <  distances.length; i++){
			if(distances[i] < minDistance){
				minDistance = distances[i];
				minIndex = i;
			}
		}
		
		switch(minIndex){
			case 0:
				return LiftPosition.BOTTOM;
			case 1:
				return LiftPosition.SWITCH;
			case 2:
				return LiftPosition.SCALE;
			case 3:
				return LiftPosition.CLIMB;
		}
		return LiftPosition.BOTTOM;
	}
	
	public void teleopInit(){
		talon.set(ControlMode.Position, 0);
		talon.setIntegralAccumulator(0.0, 0, 0);
		talon.setSelectedSensorPosition(0, 0, 0);
//		talon.configForwardSoftLimitEnable(true, 0);
//		talon.configForwardSoftLimitThreshold(30000, 0);
//		talon.configReverseSoftLimitEnable(true, 0);
//		talon.configReverseSoftLimitThreshold(-30000, 0);
		
		motor.reset();
		setLiftPosition(LiftPosition.BOTTOM);
		motor.set(setPosition);
	}
	
	public void teleopPeriodic(){
		double position = controller.getLeftJoystickX();
		position*= 4096*5*3;
		//position=Math.max(-30000, position);
		//position=Math.min(30000, position);
		SmartWriter.putD("Position", position);
		talon.set(ControlMode.Position, position);
		SmartWriter.putD("TalonSpeed", talon.getMotorOutputPercent());
		SmartWriter.putD("TalonEncoder",talon.getSelectedSensorPosition(0));
		
		if(controller.raiseLift()){
			switch(getCurrentPosition()){
				case BOTTOM:
					setLiftPosition(LiftPosition.SWITCH);
					break;
				case SWITCH:
					setLiftPosition(LiftPosition.SCALE);
					break;
				case SCALE:
					setLiftPosition(LiftPosition.CLIMB);
					break;
				default:
					setLiftPosition(LiftPosition.BOTTOM);
					break;
			}
		}
		if(controller.lowerLift()){
			switch(getCurrentPosition()){
				case SWITCH:
					setLiftPosition(LiftPosition.BOTTOM);
					break;
				case SCALE:
					setLiftPosition(LiftPosition.SWITCH);
					break;
				case CLIMB:
					setLiftPosition(LiftPosition.SCALE);
				default:
					setLiftPosition(LiftPosition.BOTTOM);
					break;
			}
		}
		motor.set(setPosition);
	}
}
