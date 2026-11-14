package org.firstinspires.ftc.teamcode;

import com.qualcomm.hardware.rev.RevHubOrientationOnRobot;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.CRServo;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.IMU;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;

// shoot preload, grab 4 from garden, go to other side, shoot again, park
// no pinpoint, just drive encoders + imu
// press B in init for red, X for blue
@Autonomous(name = "this shit sucks pls", group = "Auto")
public class BioBuzzAuto extends LinearOpMode {

    // config names, change these to whatever is in the robot config
    private static final String FL = "left_front_drive", FR = "right_front_drive";
    private static final String BL = "left_back_drive", BR = "right_back_drive";
    private static final String SHOOTER = "launcher";
    private static final String INTAKE = "intake";
    private static final String LEFT_INTAKE_SERVO = "left_intake_servo";
    private static final String RIGHT_INTAKE_SERVO = "right_intake_servo";
    private static final String FEEDER = "windmillServo";

    // drive stuff, all need testing
    private static final double TICKS_PER_INCH = 45.3;  // from the 312rpm motor + 96mm wheel, check it
    private static final double STRAFE_FACTOR = 1.1;    // strafe goes less far so we fudge it
    private static final double DRIVE_POWER = 0.3;
    private static final double COLLECT_POWER = 0.15;   // slow so intake can grab stuff
    private static final double HEADING_KP = 0.02;
    private static final double TURN_KP = 0.01;
    private static final double TURN_TOLERANCE_DEG = 2.0;
    private static final double MAX_TURN_POWER = 0.5;   // cap so turns dont whip past the target
    private static final double SLOWDOWN_IN = 8;        // start slowing down this far from the target
    private static final double MIN_DRIVE_POWER = 0.12; // slowest it will go while slowing down
    private static final double DRIVE_TIMEOUT_SEC = 5;  // gives up if the robot gets stuck
    private static final double TURN_TIMEOUT_SEC = 3;

    // shooter, also needs tuning
    private static final double SHOOTER_POWER = 0.8;
    private static final long SPIN_UP_MS = 1500;
    private static final double PRELOAD_SHOOT_SEC = 8.0;  // was 2.5, added about 5.5 sec
    private static final double SECOND_SHOOT_SEC = 9.5;   // was 4.0, added about 5.5 sec
    private static final double AUTO_LIMIT_SEC = 30;
    private static final double PARK_RESERVE_SEC = 4;     // always leave this much time to turn + park

    // field moves (inches / degrees), these are just guesses for now
    // shooter is on the back and faces the hive, so away from the wall is the robots front (+ is forward)
    // if it drives into the wall instead, make this negative
    private static final double OFF_WALL_IN = 12;
    private static final double TO_GARDEN_IN = 30;
    private static final double COLLECT_IN = 12;
    private static final double CROSS_STRAFE_IN = 60;
    private static final double SHOOT_TURN_DEG = 20;
    private static final double PARK_IN = 20;

    private DcMotor fl, fr, bl, br, intake;
    private DcMotor shooter;
    private CRServo feeder, leftIntakeServo, rightIntakeServo;
    private IMU imu;

    @Override
    public void runOpMode() {
        initHardware();

        boolean red = true;
        while (!isStarted() && !isStopRequested()) {
            if (gamepad1.b) red = true;
            if (gamepad1.x) red = false;
            telemetry.addData("Alliance", red ? "RED" : "BLUE");
            telemetry.addData("Heading", "%.1f", heading());
            telemetry.update();
        }
        waitForStart();
        if (isStopRequested()) return;

        ElapsedTime autoTimer = new ElapsedTime();
        double side = red ? 1.0 : -1.0;
        imu.resetYaw();

        // shooter stays on the whole auto so we dont wait to spin up again
        shooter.setPower(SHOOTER_POWER);
        sleep(SPIN_UP_MS);
        shoot(PRELOAD_SHOOT_SEC);

        // back off the wall first so we line up with the garden and dont bump the pollen
        drive(OFF_WALL_IN, 0, 0, DRIVE_POWER);

        // garden, intake on the whole time
        setIntake(1.0);
        drive(TO_GARDEN_IN, 0, 0, DRIVE_POWER);
        drive(COLLECT_IN, 0, 0, COLLECT_POWER);
        sleep(400);

        // cross over and aim
        drive(0, side * CROSS_STRAFE_IN, 0, DRIVE_POWER);
        turnTo(side * SHOOT_TURN_DEG);

        // shoot the ones we picked up, but leave time to park (auto is only 30 sec)
        double shootTime = Math.min(SECOND_SHOOT_SEC,
                AUTO_LIMIT_SEC - PARK_RESERVE_SEC - autoTimer.seconds());
        if (shootTime > 0) shoot(shootTime);

        // park
        setIntake(0);
        shooter.setPower(0);
        turnTo(0);
        drive(PARK_IN, 0, 0, DRIVE_POWER);

        stopDrive();
    }

    private void initHardware() {
        fl = hardwareMap.get(DcMotor.class, FL);
        fr = hardwareMap.get(DcMotor.class, FR);
        bl = hardwareMap.get(DcMotor.class, BL);
        br = hardwareMap.get(DcMotor.class, BR);
        intake = hardwareMap.get(DcMotor.class, INTAKE);
        shooter = hardwareMap.get(DcMotor.class, SHOOTER);
        feeder = hardwareMap.get(CRServo.class, FEEDER);
        // windmill (transfer) was spinning the wrong way so its reversed. remove this line if its backwards now
        feeder.setDirection(DcMotorSimple.Direction.REVERSE);
        leftIntakeServo = hardwareMap.get(CRServo.class, LEFT_INTAKE_SERVO);
        rightIntakeServo = hardwareMap.get(CRServo.class, RIGHT_INTAKE_SERVO);

        // the two intake servos face opposite ways, flip this if they fight each other
        rightIntakeServo.setDirection(DcMotorSimple.Direction.REVERSE);

        // left side reversed, flip these if a wheel spins the wrong way
        fl.setDirection(DcMotorSimple.Direction.REVERSE);
        bl.setDirection(DcMotorSimple.Direction.REVERSE);
        fr.setDirection(DcMotorSimple.Direction.FORWARD);
        br.setDirection(DcMotorSimple.Direction.FORWARD);

        for (DcMotor m : new DcMotor[]{fl, fr, bl, br}) {
            m.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
            m.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
            m.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        }

        // change these to match how the hub is mounted
        imu = hardwareMap.get(IMU.class, "imu");
        imu.initialize(new IMU.Parameters(new RevHubOrientationOnRobot(
                RevHubOrientationOnRobot.LogoFacingDirection.UP,
                RevHubOrientationOnRobot.UsbFacingDirection.FORWARD)));
    }

    private double heading() {
        return imu.getRobotYawPitchRollAngles().getYaw(AngleUnit.DEGREES);
    }

    // keeps angle between -180 and 180
    private static double wrap(double deg) {
        while (deg > 180) deg -= 360;
        while (deg < -180) deg += 360;
        return deg;
    }

    // run feeder + intake for a bit to launch stuff
    private void shoot(double seconds) {
        feeder.setPower(1.0);
        setIntake(1.0);
        sleep((long) (seconds * 1000));
        feeder.setPower(0);
    }

    // intake motor + both intake servos together
    private void setIntake(double power) {
        intake.setPower(power);
        leftIntakeServo.setPower(power);
        rightIntakeServo.setPower(power);
    }

    // drives in a line while holding heading. forward is +, strafe right is +
    private void drive(double forwardIn, double strafeIn, double holdHeadingDeg, double power) {
        double dist = Math.hypot(forwardIn, strafeIn * STRAFE_FACTOR);
        if (dist < 0.01) return;
        double targetTicks = dist * TICKS_PER_INCH;
        double f = forwardIn / Math.hypot(forwardIn, strafeIn);
        double s = strafeIn / Math.hypot(forwardIn, strafeIn);

        for (DcMotor m : new DcMotor[]{fl, fr, bl, br}) {
            m.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
            m.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        }

        ElapsedTime timer = new ElapsedTime();
        while (opModeIsActive() && farthestTicks() < targetTicks && timer.seconds() < DRIVE_TIMEOUT_SEC) {
            // + turn power is clockwise but imu goes up counterclockwise, so the minus
            double t = -HEADING_KP * wrap(holdHeadingDeg - heading());

            // slow down near the end so it doesnt overshoot
            double remaining = targetTicks - farthestTicks();
            double scale = Math.min(1.0, remaining / (SLOWDOWN_IN * TICKS_PER_INCH));
            double p = Math.max(Math.min(power, MIN_DRIVE_POWER), power * scale);

            setMecanum(f * p, s * p, t);
            telemetry.addData("ticks", "%.0f / %.0f", farthestTicks(), targetTicks);
            telemetry.update();
        }
        stopDrive();
    }

    // turn in place to a heading (relative to where we started)
    private void turnTo(double targetDeg) {
        ElapsedTime timer = new ElapsedTime();
        while (opModeIsActive() && timer.seconds() < TURN_TIMEOUT_SEC) {
            double err = wrap(targetDeg - heading());
            if (Math.abs(err) < TURN_TOLERANCE_DEG) break;
            double t = -TURN_KP * err;
            // min power or it stalls right before the target
            if (Math.abs(t) < 0.12) t = Math.copySign(0.12, t);
            // max power so it doesnt overshoot
            t = Math.max(-MAX_TURN_POWER, Math.min(MAX_TURN_POWER, t));
            setMecanum(0, 0, t);
        }
        stopDrive();
    }

    private double farthestTicks() {
        return Math.max(Math.max(Math.abs(fl.getCurrentPosition()), Math.abs(fr.getCurrentPosition())),
                Math.max(Math.abs(bl.getCurrentPosition()), Math.abs(br.getCurrentPosition())));
    }

    private void setMecanum(double forward, double strafe, double turn) {
        double pFl = forward + strafe + turn;
        double pFr = forward - strafe - turn;
        double pBl = forward - strafe + turn;
        double pBr = forward + strafe - turn;
        double max = Math.max(1.0, Math.max(Math.max(Math.abs(pFl), Math.abs(pFr)),
                Math.max(Math.abs(pBl), Math.abs(pBr))));
        fl.setPower(pFl / max);
        fr.setPower(pFr / max);
        bl.setPower(pBl / max);
        br.setPower(pBr / max);
    }

    private void stopDrive() {
        fl.setPower(0);
        fr.setPower(0);
        bl.setPower(0);
        br.setPower(0);
    }
}