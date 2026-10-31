
package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.CRServo;
import com.qualcomm.robotcore.util.ElapsedTime;

@Autonomous(name = "Simple Shoot Auto")
public class SimpleShootAuto extends LinearOpMode {

    private DcMotor leftFrontDrive;
    private DcMotor leftBackDrive;
    private DcMotor rightFrontDrive;
    private DcMotor rightBackDrive;

    private DcMotorEx launcher;
    private DcMotor intake;

    private CRServo leftIntakeServo;
    private CRServo rightIntakeServo;
    private CRServo windmillServo;

    private static final double LAUNCHER_TARGET_VELOCITY = 1230;
    private static final double LAUNCHER_MIN_VELOCITY = 1180;


    @Override
    public void runOpMode() throws InterruptedException {


        leftFrontDrive = hardwareMap.get(DcMotor.class, "left_front_drive");
        rightFrontDrive = hardwareMap.get(DcMotor.class, "right_front_drive");
        leftBackDrive = hardwareMap.get(DcMotor.class, "left_back_drive");
        rightBackDrive = hardwareMap.get(DcMotor.class, "right_back_drive");

        intake = hardwareMap.get(DcMotor.class, "intake");
        launcher = hardwareMap.get(DcMotorEx.class, "launcher");

        windmillServo = hardwareMap.get(CRServo.class, "windmillServo");
        leftIntakeServo = hardwareMap.get(CRServo.class, "left_intake_servo");
        rightIntakeServo = hardwareMap.get(CRServo.class, "right_intake_servo");


        leftFrontDrive.setDirection(DcMotor.Direction.REVERSE);
        rightFrontDrive.setDirection(DcMotor.Direction.FORWARD);
        leftBackDrive.setDirection(DcMotor.Direction.FORWARD);
        rightBackDrive.setDirection(DcMotor.Direction.REVERSE);

        rightIntakeServo.setDirection(DcMotorSimple.Direction.REVERSE);
        windmillServo.setDirection(DcMotorSimple.Direction.REVERSE);



        //leftFrontDrive.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        //rightFrontDrive.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        //leftBackDrive.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        //rightBackDrive.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);


        leftFrontDrive.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        rightFrontDrive.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        leftBackDrive.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        rightBackDrive.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);


        launcher.setMode(DcMotor.RunMode.RUN_USING_ENCODER);

        telemetry.addLine("Autonomous Initialized!");
        telemetry.addLine("Ready to start.");
        telemetry.update();


        waitForStart();

        if (isStopRequested()) return;

        //  SPIN UP LAUNCHER

        launcher.setVelocity(LAUNCHER_TARGET_VELOCITY);

        ElapsedTime spinUpTimer = new ElapsedTime();

        while (opModeIsActive()
                && launcher.getVelocity() < LAUNCHER_MIN_VELOCITY
                && spinUpTimer.seconds() < 5.0) {

            telemetry.addData("Launcher Velocity", launcher.getVelocity());
            telemetry.addLine("Spinning up launcher...");
            telemetry.update();

            idle();
        }


        if (opModeIsActive() && launcher.getVelocity() >= LAUNCHER_MIN_VELOCITY) {

            telemetry.addLine("Launcher ready!");
            telemetry.update();

            intake.setPower(1.0);
            leftIntakeServo.setPower(1.0);
            rightIntakeServo.setPower(1.0);
            windmillServo.setPower(0.5);

            sleep(6000);


            intake.setPower(0);
            leftIntakeServo.setPower(0);
            rightIntakeServo.setPower(0);
            windmillServo.setPower(0);

        } else {

            telemetry.addLine("Launcher failed to reach speed.");
            telemetry.update();
        }


        launcher.setVelocity(0);

        sleep(300);


        if (opModeIsActive()) {
            driveBackward(10.0, 35);
            turnLeft(10.0, 100);
            driveBackward(10.0, 3000);

        }


        stopRobot();

        telemetry.addLine("Autonomous Finished!");
        telemetry.update();
    }





    private void driveForward(double power, long milliseconds) {

        leftFrontDrive.setPower(power);
        rightFrontDrive.setPower(power);
        leftBackDrive.setPower(power);
        rightBackDrive.setPower(power);

        sleep(milliseconds);

        stopDrive();
    }
    private void driveBackward(double power, long milliseconds) {

        leftFrontDrive.setPower(-power);
        rightFrontDrive.setPower(-power);
        leftBackDrive.setPower(-power);
        rightBackDrive.setPower(-power);

        sleep(milliseconds);

        stopDrive();
    }

    private void turnRight(double power, long milliseconds) {

        leftFrontDrive.setPower(power);
        leftBackDrive.setPower(power);

        rightFrontDrive.setPower(-power);
        rightBackDrive.setPower(-power);

        sleep(milliseconds);

        stopDrive();
    }

    private void turnLeft(double power, long milliseconds) {

        leftFrontDrive.setPower(-power);
        leftBackDrive.setPower(-power);

        rightFrontDrive.setPower(power);
        rightBackDrive.setPower(power);

        sleep(milliseconds);

        stopDrive();
    }

    private void stopDrive() {

        leftFrontDrive.setPower(0);
        rightFrontDrive.setPower(0);
        leftBackDrive.setPower(0);
        rightBackDrive.setPower(0);
    }


    private void stopRobot() {

        leftFrontDrive.setPower(0);
        rightFrontDrive.setPower(0);
        leftBackDrive.setPower(0);
        rightBackDrive.setPower(0);

        intake.setPower(0);

        launcher.setVelocity(0);

        leftIntakeServo.setPower(0);
        rightIntakeServo.setPower(0);
        windmillServo.setPower(0);
    }
}
