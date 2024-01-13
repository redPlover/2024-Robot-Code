package frc.robot.subsystems.scoring;

import org.littletonrobotics.junction.AutoLog;

public interface AimerIO {
    @AutoLog
    public static class AimerIOInputs {
        public double armPositionRad = 0.0;
        public double armAppliedVolts = 0.0;
        public double armCurrentAmps = 0.0;
    }

    public default void updateInputs(AimerIOInputs inputs) {}

    public default void setAimAngRad(double angle) {}
}
