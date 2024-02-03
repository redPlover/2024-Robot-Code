package frc.robot.subsystems.intake;

import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.Constants.IntakeConstants;
import java.util.function.BooleanSupplier;
import org.littletonrobotics.junction.Logger;

public class IntakeSubsystem extends SubsystemBase {

    private IntakeIO io;
    private IntakeIOInputsAutoLogged inputs = new IntakeIOInputsAutoLogged();

    private State state = State.IDLE;

    private BooleanSupplier scorerWantsNote = () -> false;

    private IntakeAction action = IntakeAction.NONE;

    public IntakeSubsystem(IntakeIO io) {
        this.io = io;
    }

    @Override
    public void periodic() {
        io.updateInputs(inputs);
        Logger.processInputs("intake", inputs);

        switch (state) {
            case IDLE:
                idle();
                break;
            case SEEKING:
                seeking();
                break;
            case PASSING:
                passing();
                break;
            case REVERSING:
                reversing();
                break;
        }

        Logger.recordOutput("intake/running", inputs.leftIntakeVoltage != 0.0);
        Logger.recordOutput("intake/belting", inputs.frontBeltVoltage != 0.0);

        Logger.recordOutput("intake/state", state.toString());
    }

    public void setScoringSupplier(BooleanSupplier scorerWantsNote) {
        this.scorerWantsNote = scorerWantsNote;
    }

    public void run(IntakeAction action) {
        this.action = action;
    }

    public void toggle() {
        if (action == IntakeAction.NONE) {
            action = IntakeAction.INTAKE;
        } else {
            action = IntakeAction.NONE;
        }
    }

    public boolean hasNote() {
        return inputs.noteSensed;
    }

    private void idle() {
        if (action == IntakeAction.INTAKE) {
            state = State.SEEKING;
            io.setIntakeVoltage(IntakeConstants.intakePower);
            io.setBeltVoltage(IntakeConstants.beltPower);
        } else if (action == IntakeAction.REVERSE) {
            state = State.REVERSING;
            io.setIntakeVoltage(-IntakeConstants.intakePower);
            io.setBeltVoltage(-IntakeConstants.beltPower);
        }
    }

    private void seeking() {
        if (inputs.noteSensed) {
            state = State.PASSING;
        }

        if (action == IntakeAction.REVERSE) {
            io.setIntakeVoltage(-IntakeConstants.intakePower);
            io.setBeltVoltage(-IntakeConstants.beltPower);
            state = State.REVERSING;
        }
    }

    private void passing() {
        if (scorerWantsNote.getAsBoolean()) {
            io.setBeltVoltage(IntakeConstants.beltPower);
        } else {
            io.setBeltVoltage(0);
        }
        if (!inputs.noteSensed && inputs.backBeltCurrent < 2.0) {
            state = State.IDLE;
            io.setBeltVoltage(0);
        }

        if (action == IntakeAction.REVERSE) {
            io.setIntakeVoltage(-IntakeConstants.intakePower);
            io.setBeltVoltage(-IntakeConstants.beltPower);
            state = State.REVERSING;
        }
    }

    private void reversing() {
        if (action != IntakeAction.REVERSE) {
            io.setBeltVoltage(0);
            io.setIntakeVoltage(0);
            state = State.IDLE;
        }
    }

    private enum State {
        IDLE, // do nothing
        SEEKING, // run intake wheels until a note is taken in
        PASSING, // move the belt to pass the note to the shooter when its ready
        REVERSING // whole intake backwards
    }

    public enum IntakeAction {
        NONE, // do nothing
        INTAKE, // Try to intake a note if you don't have one
        REVERSE // run backwards
    }
}
