# Installation

## Requirements

- An omnidirectional drivetrain (mecanum or X-drive). NeuronDrive's mecanum math assumes
  four independently-driven wheels named `frontLeft`, `frontRight`, `backLeft`, `backRight`
  in your robot configuration (or pass your own names to the `NeuronDrive` constructor).
- A [goBILDA Pinpoint](https://www.gobilda.com/pinpoint-odometry-computer/) odometry
  computer, wired per goBILDA's instructions, named `pinpoint` in your robot configuration
  (or pass your own device name to `NeuronLocalizer`).
- Android Studio (Ladybug 2024.2 or later) -- Neuron Pathing is a plain Java library, so it
  does not work from Blocks or OnBot Java.

The Pinpoint driver ships inside the stock FTC SDK itself, so this quickstart needs **no
extra Gradle dependencies** beyond what `FtcRobotController` already provides.

## Get the code

Clone this repo (or download it as a zip from GitHub) and open the folder in Android
Studio as an existing project ("Open").

```
git clone <this-repo-url>
```

Build once (`Build > Make Project`) before doing anything else, to confirm Android Studio,
the SDK, and Gradle are all happy on a clean checkout.

## Where everything lives

```
TeamCode/.../neuronpathing/   the library: Pose2D, Spline, NeuronLocalizer, NeuronDrive, ...
TeamCode/.../opmodes/         example OpModes built on the library
tools/neuron-path-planner.html   the visual path planner (open it directly in a browser)
docs/                         this documentation
```

## Wire up your robot

1. In the Driver Station / Driver Hub, create a robot configuration with:
   - Four `DcMotorEx` entries named `frontLeft`, `frontRight`, `backLeft`, `backRight`
     (or your own names -- see below).
   - A goBILDA Pinpoint entry named `pinpoint` (I2C).
2. Measure your Pinpoint's offset from the robot's center of rotation, in millimeters
   (X = forward/back offset, Y = left/right offset -- see goBILDA's docs for sign
   conventions). You'll need these numbers next.
3. In your OpMode, construct the library:

   ```java
   NeuronLocalizer localizer = new NeuronLocalizer(hardwareMap, telemetry,
           /*xOffsetMm=*/ -78.74, /*yOffsetMm=*/ -184.15);
   NeuronDrive drive = new NeuronDrive(hardwareMap, localizer, /*trackWidthIn=*/ 14);
   ```

   `trackWidthIn` is the distance between your left and right wheel contact points, in
   inches -- measure your actual robot, don't guess.

   If your hardware config uses different motor names than the default four, use the
   longer constructor instead:

   ```java
   NeuronDrive drive = new NeuronDrive(hardwareMap, localizer, trackWidthIn,
           "myFrontLeft", "myFrontRight", "myBackLeft", "myBackRight");
   ```

4. Before trusting any pose data, calibrate once (blocks briefly, ~1s):

   ```java
   localizer.setPosition(new Pose2D(0, 0, 0)); // wherever your robot actually starts
   localizer.calibrate(true);
   ```

5. Call `drive.update()` every loop. That's it -- `drive.pose` now tracks the robot's
   field position, and `drive.goToPoint()` / `drive.splineToPoint()` will drive there.

None of this will behave correctly yet on a fresh robot -- the offsets, motor directions,
and PID gains above are all starting points. Follow **[Tuning](tuning.md)** next before
trusting any of it in a match.
