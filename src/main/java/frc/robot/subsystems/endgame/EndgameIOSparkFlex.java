package frc.robot.subsystems.endgame;

import com.revrobotics.CANSparkBase.IdleMode;
import com.revrobotics.CANSparkFlex;
import com.revrobotics.CANSparkLowLevel.MotorType;
import frc.robot.Constants.EndgameConstants;

public class EndgameIOSparkFlex implements EndgameIO {
    private final CANSparkFlex leftEndgameMotor =
            new CANSparkFlex(EndgameConstants.leftMotorID, MotorType.kBrushless);
    private final CANSparkFlex rightEndgameMotor =
            new CANSparkFlex(EndgameConstants.rightMotorID, MotorType.kBrushless);

    public EndgameIOSparkFlex() {
        leftEndgameMotor.setSmartCurrentLimit(EndgameConstants.smartCurrentLimit);
        rightEndgameMotor.setSmartCurrentLimit(EndgameConstants.smartCurrentLimit);

        rightEndgameMotor.setIdleMode(IdleMode.kBrake);
        leftEndgameMotor.setIdleMode(IdleMode.kBrake);

        leftEndgameMotor.follow(rightEndgameMotor, true);

        leftEndgameMotor.getEncoder().setPositionConversionFactor(EndgameConstants.encoderToMeters);
        leftEndgameMotor.getEncoder().setPosition(0.0);
    }

    @Override
    public void setVolts(double volts) {
        rightEndgameMotor.setVoltage(volts);
    }

    @Override
    public void setBrakeMode(boolean brake) {
        IdleMode sparkMode = brake ? IdleMode.kBrake : IdleMode.kCoast;
        leftEndgameMotor.setIdleMode(sparkMode);
        rightEndgameMotor.setIdleMode(sparkMode);
    }

    @Override
    public void updateInputs(EndgameIOInputs inputs) {
        inputs.endgameLeftAppliedVolts = leftEndgameMotor.getAppliedOutput();
        inputs.endgameLeftStatorCurrentAmps = leftEndgameMotor.getOutputCurrent();

        inputs.endgameRightAppliedVolts = rightEndgameMotor.getAppliedOutput();
        inputs.endgameRightStatorCurrentAmps = rightEndgameMotor.getOutputCurrent();

        inputs.position = leftEndgameMotor.getEncoder().getPosition();
        inputs.velocity = leftEndgameMotor.getEncoder().getVelocity();
    }
}
