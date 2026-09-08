# Subsystems & public methods

Neuron Pathing only knows about driving and following paths. Everything else on your
robot -- an intake, a shooter, an arm, a claw -- is a **subsystem**: a small class that
owns one mechanism's hardware, and a handful of methods you call from your OpModes. This
page is the pattern for writing those, and for calling their public methods directly from
both TeleOp and Autonomous alongside `NeuronDrive`.

This repo doesn't use a command-scheduler framework (like FTCLib's `CommandScheduler`) --
you just call a subsystem's public methods directly, in whatever order your OpMode needs.
That's simpler to reason about for a first robot, and it's exactly the pattern
`NeuronDrive` itself already follows. If your robot grows complex enough that manual
sequencing gets unwieldy, a command-scheduler library is a reasonable next step, but
start here.

## Writing a subsystem

See `TeamCode/.../subsystems/ExampleClaw.java` for a working copy of this shape:

```java
public class ExampleClaw {
    public enum State { OPEN, CLOSED }

    private final Servo servo;
    public State state = State.CLOSED;

    public ExampleClaw(HardwareMap hwMap, String servoName) {
        servo = hwMap.get(Servo.class, servoName);
        close();
    }

    public void open()  { servo.setPosition(OPEN_POS);  state = State.OPEN; }
    public void close() { servo.setPosition(CLOSED_POS); state = State.CLOSED; }
    public void toggle() { if (state == State.OPEN) close(); else open(); }

    public void update() { /* PID, timed sequence, telemetry -- see below */ }
}
```

The rules that make this pattern work:

- **The subsystem owns its hardware.** Only `ExampleClaw` ever calls
  `hardwareMap.get(Servo.class, ...)` for that servo. Nothing else reaches in and touches
  it directly -- if you need a new capability, add a method, don't bypass the class.
- **Public methods are what your OpModes call.** Name them as verbs (`open()`, `shoot()`,
  `setPosition(...)`). Keep them non-blocking where possible (set a target/state and
  return immediately) so they don't stall your main loop.
- **`update()` is for anything that has to happen every loop cycle**, even when nothing
  just changed -- running a PID toward a target position, advancing a timed sequence,
  reporting telemetry. A one-servo claw doesn't need real work here, but a motor-driven
  arm holding position against gravity does. Call every subsystem's `update()` once per
  loop, right alongside `drive.update()`.

## Using subsystems in TeleOp

Construct every subsystem once, before `waitForStart()`, next to `drive`/`localizer`.
Then in the loop: read gamepad buttons, call the matching subsystem method, and call
`update()` on everything. See `NeuronTeleOp.java`:

```java
NeuronDrive drive = new NeuronDrive(hardwareMap, localizer, trackWidthIn);
ExampleClaw claw = new ExampleClaw(hardwareMap);

waitForStart();
while (opModeIsActive()) {
    drive.drive(gamepad1);
    drive.update();

    if (gamepad1.aWasPressed()) claw.toggle();   // edge-detected: fires once per press
    claw.update();

    telemetry.addData("claw", claw.state);
    telemetry.update();
}
```

Use `*WasPressed()` (e.g. `gamepad1.aWasPressed()`) for anything that should fire once per
button press, not once per loop iteration for the whole time it's held down.

## Using subsystems in Autonomous

Subsystem calls just sit in the same sequence as your drive calls, in whatever order the
auto needs. The only wrinkle: a move like `drive.splineToPoint(...)` doesn't block, so
follow it with `driveToTarget()` (or call `claw.update()` inside your own wait loop) so
subsystems that need per-loop updates keep running while the robot drives. See
`NeuronSplineAuto.java`:

```java
drive.splineToPoint(new Pose2D(24, 24, Math.toRadians(90)), false, false, 1, 0.8);
driveToTarget();          // blocks until the move finishes, still calling claw.update()

claw.open();
waitMs(300);              // a dwell, also still calling claw.update()

drive.goToPoint(new Pose2D(24, 0, 0), false, 0.6);
driveToTarget();

claw.close();
```

`driveToTarget()` and `waitMs()` are small private helper methods defined at the bottom of
the OpMode (loop calling `drive.update()`/`claw.update()` until the move finishes or the
timer's up) -- this is exactly what the Neuron Path Planner's "Full OpMode" output
generates for you automatically. If you're pasting planner output into your own OpMode,
copy these two helpers over once and every generated auto will compile against them.

## Wiring your subsystem into the planner

The planner's shooter-pipeline actions (shoot/intake/gate/turret) are just this pattern,
generated as text. To add your own subsystem as a quick-add action in the planner, open
`tools/neuron-path-planner.html` and add an entry to `const ACTIONS` near the top of the
`<script>` block, following the same shape as the existing entries -- see the comment
directly above it for the exact format.
