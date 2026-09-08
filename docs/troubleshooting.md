# Troubleshooting

Symptoms, in the order you're likely to hit them.

## Build / setup

**Gradle sync fails on a fresh clone.**
Make sure you're on Android Studio Ladybug (2024.2) or later, and that you opened the
repo's root folder (the one with `settings.gradle` in it), not the `TeamCode` or
`FtcRobotController` subfolder directly.

**Can't find `GoBildaPinpointDriver`.**
It ships inside the FTC SDK's `Hardware` module, which `build.dependencies.gradle`
already includes -- if this still fails, you likely edited that file or are on a much
older SDK version. No separate Pinpoint dependency should be needed.

## Robot won't drive / drives the wrong way

**Robot does nothing when a goToPoint/splineToPoint runs.**
`NeuronDrive.update()` has to be called every loop for anything to move -- check your
OpMode has a loop calling `drive.update()` (see `driveToTarget()` in the generated/example
code). A one-shot call to `goToPoint()` by itself does nothing.

**Robot drives away from the target instead of toward it, or spins the wrong way.**
This is almost always Stage 1 of tuning (localizer sign conventions), not the drive
math -- see [Tuning](tuning.md#stage-1----localizer-sign-conventions). Fix the localizer,
don't try to compensate by negating things in `NeuronDrive`.

**Robot strafes when it should drive straight, or vice versa.**
Motor directions/wiring, not software -- confirm all four drive motors are wired and
configured with the standard mecanum roller orientation, and that `frontLeft`/`backLeft`
are the two reversed motors (the constructor already reverses them, assuming standard
goBILDA/REV wiring; if yours differs, adjust the direction calls in `NeuronDrive`'s
constructor).

## Path following

**Robot oscillates / wiggles along a straight spline segment.**
Lower `lookaheadIn` slightly first; if that doesn't help, your point-holding PID gains
(Stage 3) may be too aggressive and bleeding into the spline follower's cross-track
correction. Re-check Stage 3 before touching spline tunables.

**Robot cuts corners / drifts to the inside of curves.**
Raise `centripetalGain`, or increase `lookaheadIn` slightly.

**Robot overshoots past the end of a path, or the final heading is off.**
`splineEndRadiusIn` is handing off to the point-holding PID too late (or too early) --
see Stage 4. Also confirm the path's final waypoint has the heading you actually want;
`Pose2D`'s `h` is in radians, and a common mistake is passing degrees by accident.

**Everything is fine at slow speed but falls apart at higher `maxSpeed`.**
Expected to a degree -- centripetal/cross-track correction needs to work harder at speed.
Re-tune Stage 3/4 at the actual speed you intend to run in a match, not at a conservative
test speed.

## Path Planner-specific

**Generated code references `robot.something` and won't compile.**
The shooter-pipeline actions (shoot/farShoot/intake/gate/turret) intentionally generate
calls into *your season's* `Robot`/`Turret`/`Gate`/`Intake`/`Shooter` classes, not the
pathing library -- they're meant for your real season repo, not a bare Neuron Pathing
checkout. Either add those classes, or don't use those actions (movement-only autos
compile fine against just `NeuronDrive`/`NeuronLocalizer`).

**Importing an existing OpMode back into the planner finds "No autos found."**
The importer looks for `public class X extends LinearOpMode` plus a `case N: method();`
switch structure matching what the planner itself generates in "Full OpMode" mode. Autos
written by hand in a different structure won't parse -- but any unrecognized line is kept
as a raw custom line if it appears *inside* a method the importer does recognize.

**The field background/video overlay doesn't line up with real match footage.**
Use the rotate/flip controls on the field image, and the perspective/scale/offset sliders
under the video overlay -- these are separate from the path's actual coordinates and only
affect what you see while planning, not the generated code.
