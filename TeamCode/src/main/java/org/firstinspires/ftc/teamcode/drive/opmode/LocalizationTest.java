package org.firstinspires.ftc.teamcode.drive.opmode;

import com.acmerobotics.roadrunner.geometry.Pose2d;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;

import org.firstinspires.ftc.teamcode.drive.GFORCE_KiwiDrive;

/**
 * This is a simple teleop routine for testing localization. Drive the robot around like a normal
 * teleop routine and make sure the robot's estimated pose matches the robot's actual pose (slight
 * errors are not out of the ordinary, especially with sudden drive motions). The goal of this
 * exercise is to ascertain whether the localizer has been configured properly (note: the pure
 * encoder localizer heading may be significantly off if the track width has not been tuned).
 */
@TeleOp(group = "drive")
public class LocalizationTest extends LinearOpMode {
    @Override


    public void runOpMode() throws InterruptedException {

        GFORCE_KiwiDrive drive = new GFORCE_KiwiDrive(hardwareMap);

        drive.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);

        waitForStart();

        while (!isStopRequested()) {
            drive.setWeightedDrivePower(
                    new Pose2d(
                            -gamepad1.left_stick_y,
                            -gamepad1.left_stick_x,
                            -gamepad1.right_stick_x / 10
                    )
            );

            drive.update();

            Pose2d poseEstimate = drive.getPoseEstimate();

            // Print pose to telemetry
            telemetry.addData("Raw Left",  drive.localizer.leftEncoder.getCurrentPosition());
            telemetry.addData("Raw Right", drive.localizer.rightEncoder.getCurrentPosition());
            telemetry.addData("Sum", drive.localizer.rightEncoder.getCurrentPosition() + drive.localizer.leftEncoder.getCurrentPosition());
            telemetry.addData("Dif", drive.localizer.rightEncoder.getCurrentPosition() - drive.localizer.leftEncoder.getCurrentPosition());


            telemetry.addData("x", poseEstimate.getX());
            telemetry.addData("y", poseEstimate.getY());
            telemetry.addData("heading ODO. ", Math.toDegrees(poseEstimate.getHeading()));
            telemetry.addData("heading GYRO.", Math.toDegrees(drive.getExternalHeading()));
            telemetry.update();
        }
    }
}
