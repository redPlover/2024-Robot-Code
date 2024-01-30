package frc.robot;

import com.pathplanner.lib.auto.NamedCommands;
import edu.wpi.first.math.VecBuilder;
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;
import edu.wpi.first.wpilibj2.command.InstantCommand;
import edu.wpi.first.wpilibj2.command.button.CommandJoystick;
import edu.wpi.first.wpilibj2.command.button.CommandXboxController;
import frc.robot.Constants.DriveConstants;
import frc.robot.Constants.FeatureFlags;
import frc.robot.Constants.TunerConstants;
import frc.robot.Constants.VisionConstants;
import frc.robot.commands.DriveWithJoysticks;
import frc.robot.subsystems.drive.CommandSwerveDrivetrain;
import frc.robot.subsystems.localization.VisionIOReal;
import frc.robot.subsystems.localization.VisionIOSim;
import frc.robot.subsystems.localization.VisionLocalizer;
import frc.robot.subsystems.scoring.AimerIOSim;
import frc.robot.subsystems.scoring.AimerIOTalon;
import frc.robot.subsystems.scoring.HoodIOSim;
import frc.robot.subsystems.scoring.HoodIOVortex;
import frc.robot.subsystems.scoring.ScoringSubsystem;
import frc.robot.subsystems.scoring.ShooterIOSim;
import frc.robot.subsystems.scoring.ShooterIOTalon;
import frc.robot.utils.FieldFinder;
import java.util.Collections;
import org.littletonrobotics.junction.Logger;

public class RobotContainer {
    ScoringSubsystem scoringSubsystem;

    CommandJoystick leftJoystick = new CommandJoystick(0);
    CommandJoystick rightJoystick = new CommandJoystick(1);
    CommandXboxController controller = new CommandXboxController(2);

    VisionLocalizer tagVision;

    CommandSwerveDrivetrain drivetrain = TunerConstants.DriveTrain;

    Telemetry driveTelemetry = new Telemetry(DriveConstants.MaxSpeedMetPerSec);

    SendableChooser<String> autoChooser = new SendableChooser<String>();

    public RobotContainer() {
        configureBindings();
        configureSubsystems();
        configureModes();
        configureAutonomous();
    }

    // spotless:off
    private void configureBindings() {
        drivetrain.setDefaultCommand(
                new DriveWithJoysticks(
                        drivetrain,
                        () -> -controller.getLeftY(),
                        () -> -controller.getLeftX(),
                        () -> -controller.getRightX(),
                        () -> true,
                        () -> false));

        controller.a()
                .onTrue(new InstantCommand(
                    () -> scoringSubsystem.setAction(
                        ScoringSubsystem.ScoringAction.INTAKE)));

        controller.b()
                .onTrue(new InstantCommand(
                    () -> scoringSubsystem.setAction(
                        ScoringSubsystem.ScoringAction.AIM)));

        controller.x()
                .onTrue(new InstantCommand(
                    () -> scoringSubsystem.setAction(
                        ScoringSubsystem.ScoringAction.SHOOT)))
                .onFalse(new InstantCommand(
                    () -> scoringSubsystem.setAction(
                        ScoringSubsystem.ScoringAction.AIM)));

        controller.y()
                .onTrue(new InstantCommand(
                    () -> scoringSubsystem.setAction(
                        ScoringSubsystem.ScoringAction.ENDGAME)))
                .onFalse(new InstantCommand(
                    () -> scoringSubsystem.setAction(
                        ScoringSubsystem.ScoringAction.WAIT)));
        
        controller.rightBumper()
                .onTrue(new InstantCommand(
                    () -> scoringSubsystem.setAction(
                        ScoringSubsystem.ScoringAction.AMP_AIM)));

        controller.start()
                .onTrue(new InstantCommand(
                    () -> scoringSubsystem.setAction(
                        ScoringSubsystem.ScoringAction.WAIT)));
    } // spotless:on

    private void configureModes() {}

    public void configureSubsystems() {
        switch (Constants.currentMode) {
            case REAL:
                scoringSubsystem =
                        new ScoringSubsystem(
                                new ShooterIOTalon(),
                                new AimerIOTalon(),
                                new HoodIOVortex(),
                                driveTelemetry::getFieldToRobot,
                                () ->
                                        VecBuilder.fill(
                                                driveTelemetry.getVelocityX(),
                                                driveTelemetry.getVelocityY()));

                tagVision = new VisionLocalizer(new VisionIOReal(VisionConstants.cameras));
                break;
            case SIM:
                drivetrain.seedFieldRelative(DriveConstants.initialPose);

                scoringSubsystem =
                        new ScoringSubsystem(
                                new ShooterIOSim(),
                                new AimerIOSim(),
                                new HoodIOSim(),
                                driveTelemetry::getFieldToRobot,
                                () ->
                                        VecBuilder.fill(
                                                driveTelemetry.getVelocityX(),
                                                driveTelemetry.getVelocityY()));

                if (FeatureFlags.simulateVision) {
                    tagVision =
                            new VisionLocalizer(
                                    new VisionIOSim(
                                            VisionConstants.cameras,
                                            driveTelemetry::getModuleStates));
                } else {
                    tagVision =
                            new VisionLocalizer(
                                    new VisionIOSim(
                                            Collections.emptyList(),
                                            driveTelemetry::getModuleStates));
                }
                break;
            case REPLAY:
                break;
        }

        drivetrain.registerTelemetry(driveTelemetry::telemeterize);
        Commands.run(driveTelemetry::logDataSynchronously).ignoringDisable(true).schedule();
    }

    public void robotPeriodic() {
        Logger.recordOutput(
                "localizer/whereAmI",
                FieldFinder.whereAmI(
                        driveTelemetry.getFieldToRobot().getTranslation().getX(),
                        driveTelemetry.getFieldToRobot().getTranslation().getY()));
    }

    public Command getAutonomousCommand() {
        return drivetrain.getAutoPath(autoChooser.getSelected());
    }

    private void configureAutonomous() {
        autoChooser.setDefaultOption("Default", "NewAuto");
        autoChooser.addOption("New Auto", "5-Note");

        SmartDashboard.putData("Auto Chooser", autoChooser);

        NamedCommands.registerCommand(
                "Shoot Scoring",
                new InstantCommand(
                        () -> scoringSubsystem.setAction(ScoringSubsystem.ScoringAction.SHOOT)));
        NamedCommands.registerCommand(
                "Aim Scoring",
                new InstantCommand(
                        () -> scoringSubsystem.setAction(ScoringSubsystem.ScoringAction.AIM)));
        NamedCommands.registerCommand(
                "Intake Scoring",
                new InstantCommand(
                        () -> scoringSubsystem.setAction(ScoringSubsystem.ScoringAction.INTAKE)));
        NamedCommands.registerCommand(
                "Wait Scoring",
                new InstantCommand(
                        () -> scoringSubsystem.setAction(ScoringSubsystem.ScoringAction.WAIT)));
    }
}
