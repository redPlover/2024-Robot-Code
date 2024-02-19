package frc.robot.subsystems.scoring;

import com.ctre.phoenix6.Utils;
import com.ctre.phoenix6.configs.CurrentLimitsConfigs;
import com.ctre.phoenix6.configs.TalonFXConfigurator;
import com.ctre.phoenix6.controls.Follower;
import com.ctre.phoenix6.hardware.TalonFX;
import com.ctre.phoenix6.signals.NeutralModeValue;
import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.controller.ArmFeedforward;
import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.math.trajectory.TrapezoidProfile;
import edu.wpi.first.math.trajectory.TrapezoidProfile.State;
import edu.wpi.first.wpilibj.DutyCycleEncoder;
import edu.wpi.first.wpilibj.Timer;
import frc.robot.Constants.ScoringConstants;

public class AimerIORoboRio implements AimerIO {
    private final TalonFX aimerLeft = new TalonFX(ScoringConstants.aimLeftMotorId);
    private final TalonFX aimerRight = new TalonFX(ScoringConstants.aimRightMotorId);

    private final PIDController controller =
            new PIDController(
                    ScoringConstants.aimerkP, ScoringConstants.aimerkI, ScoringConstants.aimerkD);
    private ArmFeedforward feedforward =
            new ArmFeedforward(
                    ScoringConstants.aimerkS,
                    ScoringConstants.aimerkG,
                    ScoringConstants.aimerkV,
                    ScoringConstants.aimerkA);
    private TrapezoidProfile profile =
            new TrapezoidProfile(
                    new TrapezoidProfile.Constraints(
                            ScoringConstants.aimCruiseVelocity, ScoringConstants.aimAcceleration));

    private final DutyCycleEncoder encoder = new DutyCycleEncoder(ScoringConstants.aimEncoderPort);

    private final Timer timer = new Timer();

    private boolean override = false;
    private double overrideVolts = 0.0;

    boolean newProfile = false;
    double previousGoalAngle = 0.0;

    double minAngleClamp = 0.0;
    double maxAngleClamp = 0.0;

    double goalAngleRad = 0.0;
    double appliedVolts = 0.0;

    double initialAngle = 0.0;
    double initialVelocity = 0.0;

    double velocity = 0.0;

    double lastPosition = 0.0;
    double lastTime = Utils.getCurrentTimeSeconds();

    public AimerIORoboRio() {
        aimerLeft.setControl(new Follower(ScoringConstants.aimRightMotorId, true));

        aimerLeft.setNeutralMode(NeutralModeValue.Brake);
        aimerRight.setNeutralMode(NeutralModeValue.Brake);

        TalonFXConfigurator aimerLeftConfig = aimerLeft.getConfigurator();
        aimerLeftConfig.apply(
                new CurrentLimitsConfigs()
                        .withStatorCurrentLimit(120)
                        .withStatorCurrentLimitEnable(true));

        TalonFXConfigurator aimerRightConfig = aimerRight.getConfigurator();
        aimerRightConfig.apply(
                new CurrentLimitsConfigs()
                        .withStatorCurrentLimit(120)
                        .withStatorCurrentLimitEnable(true));

        aimerRight.setPosition(0.0);
    }

    @Override
    public void setAimAngleRad(double goalAngleRad, boolean newProfile) {
        this.goalAngleRad = goalAngleRad;
        this.newProfile = newProfile;
    }

    @Override
    public void controlAimAngleRad() {
        if (goalAngleRad != previousGoalAngle && newProfile) {
            timer.reset();
            timer.start();

            initialAngle = getEncoderPosition();
            initialVelocity = velocity;

            previousGoalAngle = goalAngleRad;
        }
        goalAngleRad = MathUtil.clamp(goalAngleRad, minAngleClamp, maxAngleClamp);
    }

    @Override
    public void setAngleClampsRad(double minAngleClamp, double maxAngleClamp) {
        this.minAngleClamp = minAngleClamp;
        this.maxAngleClamp = maxAngleClamp;
    }

    @Override
    public void setOverrideMode(boolean override) {
        this.override = override;
    }

    @Override
    public void setOverrideVolts(double volts) {
        overrideVolts = volts;
    }

    @Override
    public void setPID(double p, double i, double d) {
        controller.setP(p);
        controller.setI(i);
        controller.setD(d);
    }

    @Override
    public void setMaxProfile(double maxVelocity, double maxAcceleration) {
        profile =
                new TrapezoidProfile(
                        new TrapezoidProfile.Constraints(maxVelocity, maxAcceleration));
    }

    @Override
    public void setFF(double kS, double kV, double kA, double kG) {
        feedforward = new ArmFeedforward(kS, kG, kV, kA);
    }

    private double getEncoderPosition() {
        // return encoder.getAbsolutePosition() * 2.0 * Math.PI -
        // ScoringConstants.aimerEncoderOffset;
        return aimerRight.getPosition().getValueAsDouble() * (2.0 * Math.PI) / 80.0;
    }

    @Override
    public void updateInputs(AimerIOInputs inputs) {
        State trapezoidSetpoint =
                profile.calculate(
                        timer.get(),
                        new State(initialAngle, initialVelocity),
                        new State(goalAngleRad, 0));

        if (override) {
            appliedVolts = overrideVolts;
        } else {
            appliedVolts =
                    feedforward.calculate(trapezoidSetpoint.position, trapezoidSetpoint.velocity)
                            + controller.calculate(
                                    getEncoderPosition(), trapezoidSetpoint.position);
        }

        if (getEncoderPosition() > Math.PI / 2.0) {
            appliedVolts = 0.0;
        }
        aimerRight.setVoltage(appliedVolts);

        inputs.aimGoalAngleRad = trapezoidSetpoint.position;
        inputs.aimAngleRad = getEncoderPosition();

        double currentTime = Utils.getCurrentTimeSeconds();
        double diffTime = currentTime - lastTime;
        lastTime = currentTime;

        inputs.aimVelocityRadPerSec = (getEncoderPosition() - lastPosition) / diffTime;
        velocity = (getEncoderPosition() - lastPosition) / diffTime;
        lastPosition = getEncoderPosition();

        inputs.aimAppliedVolts = aimerRight.getMotorVoltage().getValueAsDouble();
        inputs.aimStatorCurrentAmps = aimerRight.getStatorCurrent().getValueAsDouble();
        inputs.aimSupplyCurrentAmps = aimerRight.getSupplyCurrent().getValueAsDouble();
    }
}
