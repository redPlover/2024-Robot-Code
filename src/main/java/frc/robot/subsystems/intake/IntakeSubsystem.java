package frc.robot.subsystems.intake;

import edu.wpi.first.wpilibj2.command.SubsystemBase;
import java.util.function.BooleanSupplier;
import org.littletonrobotics.junction.Logger;

public class IntakeSubsystem extends SubsystemBase {

    private IntakeIO io;
    private IntakeIOInputsAutoLogged inputs = new IntakeIOInputsAutoLogged();

    private State state = State.IDLE;

    private BooleanSupplier scorerWantsNote = () -> false;

    private boolean shouldBeRunning = false;

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
            case FEEDING:
                feeding();
                break;
            case PASSING:
                passing();
                break;
        }

        Logger.recordOutput("intake/running", inputs.backMotorVoltage != 0.0);
        Logger.recordOutput("intake/belting", inputs.beltMotorVoltage != 0.0);

        Logger.recordOutput("intake/state", state.toString());
    }

    public void setScoringSupplier(BooleanSupplier scorerWantsNote) {
        this.scorerWantsNote = scorerWantsNote;
    }

    public void run(boolean shouldBeRunning) {
        this.shouldBeRunning = shouldBeRunning;
    }

    public void toggle() {
        shouldBeRunning = !shouldBeRunning;
    }

    public boolean hasNote() {
        return state == State.FEEDING;
    }

    private void idle() {
        if (shouldBeRunning) {
            state = State.SEEKING;
            io.setIntakeVoltage(5);
        }
    }

    private void seeking() {
        if (inputs.backMotorCurrent > 20) {
            state = State.FEEDING;
            io.setIntakeVoltage(2);
            io.setBeltVoltage(2);
        }
    }

    private void feeding() {
        if (inputs.backMotorCurrent < 5) {
            state = State.PASSING;
            io.setIntakeVoltage(0);
            io.setBeltVoltage(0);
        }
    }

    private void passing() {
        if (scorerWantsNote.getAsBoolean()) {
            io.setBeltVoltage(2);
        }
        if (inputs.beltMotorCurrent < 5) {
            state = State.IDLE;
            io.setBeltVoltage(0);
        }
    }

    private enum State {
        IDLE, // do nothing
        SEEKING, // running intake wheels in search of a note
        FEEDING, // taking in a note and passing it to the belt
        PASSING // move the belt to pass the note to the shooter when its ready
    }
}
