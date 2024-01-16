package frc.robot;

import edu.wpi.first.wpilibj2.command.InstantCommand;
import edu.wpi.first.wpilibj2.command.button.CommandJoystick;
import edu.wpi.first.wpilibj2.command.button.CommandXboxController;
import frc.robot.subsystems.scoring.AimerIO;
import frc.robot.subsystems.scoring.AimerIOSim;
import frc.robot.subsystems.scoring.ScoringSubsystem;
import frc.robot.subsystems.scoring.ShooterIO;
import frc.robot.subsystems.scoring.ShooterIOSim;

public class RobotContainer {
    ScoringSubsystem scoringSubsystem;

    CommandJoystick leftJoystick = new CommandJoystick(0);
    CommandJoystick rightJoystick = new CommandJoystick(1);
    CommandXboxController controller = new CommandXboxController(2);

    public RobotContainer() {
        configureBindings();
        configureSubsystems();
        configureModes();
    }

    private void configureBindings() {
        controller.a().whileTrue(new InstantCommand(() ->
            scoringSubsystem.setAction(ScoringSubsystem.ScoringAction.INTAKE)));

        controller.b().whileTrue(new InstantCommand(() ->
            scoringSubsystem.setAction(ScoringSubsystem.ScoringAction.AIM)));
        
        controller.x().whileTrue(new InstantCommand(() ->
            scoringSubsystem.setAction(ScoringSubsystem.ScoringAction.SHOOT)));

        controller.y().whileTrue(new InstantCommand(() ->
            scoringSubsystem.setAction(ScoringSubsystem.ScoringAction.ABORT)));

        controller.back().whileTrue(new InstantCommand(() ->
            scoringSubsystem.setAction(ScoringSubsystem.ScoringAction.ENDGAME)));
    }

    private void configureSubsystems() {
        scoringSubsystem = new ScoringSubsystem(new ShooterIOSim(), new AimerIOSim());
    }

    private void configureModes() {}
}
