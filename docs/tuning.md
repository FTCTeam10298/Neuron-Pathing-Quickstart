# Tuning

Do these in order. Each stage assumes the previous one is already correct -- PID tuning
on top of a localizer with the wrong sign conventions will never converge, no matter how
long you fiddle with gains.

Before starting: build and deploy once, and confirm your hardware configuration on the
Driver Hub actually has the motor/Pinpoint names your code expects (Installation).

## Stage 1 -- Localizer sign conventions

Run **Neuron Localizer Test** (`NeuronLocalizerTest.java`) on the Driver Station. It never
commands the motors -- you push the robot by hand and just watch the numbers.

Set `xOffsetMm`/`yOffsetMm` in the OpMode to your measured Pinpoint offset first.

With the robot on the field, facing what you consider "heading 0":

| Push the robot... | ...and this should happen |
|---|---|
| straight forward | `x` increases |
| straight left (strafe) | `y` increases |
| turn counter-clockwise (viewed from above) | `heading` increases |

If any of these are backwards, the fix is in `NeuronLocalizer`'s construction, **not** in
`NeuronDrive`'s math:

- `x` or `y` backwards -> swap/flip the corresponding `EncoderDirection` you pass to
  `NeuronLocalizer` (or check the pod is plugged into the correct Pinpoint port).
- `heading` backwards -> the Pinpoint's IMU orientation is flipped; double check it's
  mounted flat and its port assignment matches goBILDA's wiring diagram.
- Numbers are wildly wrong (not just backwards) -> re-check `xOffsetMm`/`yOffsetMm` and
  that you called `calibrate(true)` after `setPosition()`.

Don't move on until pushing the robot in a straight line down the field gives you a clean,
correctly-signed `x`/`y`/`heading` reading.

## Stage 2 -- Track width

Measure the distance between your left and right wheel contact patches, in inches, and
set it as the `trackWidthIn` argument to `NeuronDrive`'s constructor. This isn't something
you "tune" by feel -- use a tape measure. Getting it wrong makes the spline follower turn
too sharply or too lazily relative to how fast it thinks it's turning.

## Stage 3 -- Point-holding PID

This is `NeuronDrive.goToPoint()` -- driving straight to and holding a single point, no
spline. Use `NeuronSplineAuto.java`'s `goToPoint` call (or write your own quick test
OpMode) to send the robot a few feet away and watch it arrive.

Tune in this order, one axis at a time if possible (command a pure forward move, then a
pure strafe, then a pure turn-in-place):

1. **`forwardP` / `strafeP` / `hP`** -- raise until the robot approaches its target
   promptly without a large overshoot. Too low: crawls in slowly or stalls short. Too
   high: overshoots and/or oscillates visibly around the target.
2. **`forwardD` / `strafeD` / `hD`** -- raise to damp any oscillation or overshoot left
   over from P. Too high: the robot feels "stiff" or twitchy, especially over small
   corrections.
3. **`forwardI` / `strafeI` / `hI`** -- leave at `0` unless the robot consistently settles
   just short of the target and stays there (steady-state error). If so, add a *small*
   amount of I. Too much causes slow oscillation ("wind-up") -- if you see that, you went
   too far.

All six gains, plus the three I-terms, are `public static` fields on `NeuronDrive` --
tune them live with FTC Dashboard, or edit the defaults directly in
`NeuronDrive.java` and redeploy.

Also check `pointToleranceIn` / `headingToleranceDeg` (how close counts as "arrived") and
the tighter `closePointToleranceIn` / `closeHeadingToleranceDeg` (final settle). Loosen
these if the robot is spending too much match time "settling"; tighten them if your
autos need to be more precise than the defaults (5 in / 5°, then 2 in / 2°).

## Stage 4 -- Spline follower

Now test `drive.splineToPoint()` / `drive.followSpline()` -- actual curved paths, not just
point-to-point. Plan a path in the **[Neuron Path Planner](../tools/neuron-path-planner.html)**,
paste the generated code into `NeuronSplineAuto.java` (or your own OpMode), and run it.

Three tunables control this, all `public static` on `NeuronDrive`:

- **`lookaheadIn`** (default 14) -- how far ahead along the path the robot aims. Too
  small: the robot hunts/oscillates trying to track a point that's almost underneath it,
  especially on tight curves. Too large: it cuts corners, drifting noticeably inside the
  curve.
- **`centripetalGain`** (default 0.3) -- corrects for the robot sliding to the outside of
  a curve. Raise it if the robot visibly drifts wide on curves; lower it if the robot
  overshoots/wobbles to the *inside* of curves or feels jerky mid-curve.
- **`splineEndRadiusIn`** (default 14) -- how close to the path's last point the robot
  switches from pure-pursuit over to the point-holding PID from Stage 3. If the robot
  hands off too early it may miss the final heading; too late and it can overshoot before
  the PID takes over.

Also double check the per-call `factor` argument (in `splineToPoint`/the planner's spline
steps): it shapes how aggressively the curve pulls toward each waypoint's heading tangent.
`1.0` is a reasonable default -- raise it for wider, gentler curves between waypoints that
are far apart or at sharp angles.

## Stage 5 -- Validate

Before trusting any of this in a match:

1. Re-run **Neuron Localizer Test** and confirm the sign conventions from Stage 1 still
   hold (a loose pod mount can silently break this after Stage 2-4 changes).
2. Run a full autonomous end to end, starting from your actual match starting position,
   at least 3 times in a row without touching the code -- one clean run proves nothing.
3. If something drifts over a long path but Stage 1-4 all checked out individually, it's
   almost always the Pinpoint offsets (Stage 1) being slightly off, not the follower gains
   -- small heading errors compound a lot over a long path.

If you're stuck, see **[Troubleshooting](troubleshooting.md)**.
