import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import org.openftc.easyopencv.OpenCvPipeline;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName;
import org.openftc.easyopencv.OpenCvCamera;
import org.openftc.easyopencv.OpenCvCameraFactory;
import org.openftc.easyopencv.OpenCvCameraRotation;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

@TeleOp(name="Wifi Video Stream", group="Example")
public class Wifi_Video_Stream extends LinearOpMode {

    private static final int PORT = 25565;
    private OpenCvCamera webcam;
    private FramePipeline pipeline;
    public boolean sendData = true;

    @Override
    public void runOpMode() throws InterruptedException {
        // Initialize the webcam
        int cameraMonitorViewId = hardwareMap.appContext.getResources().getIdentifier("cameraMonitorViewId", "id", hardwareMap.appContext.getPackageName());
        webcam = OpenCvCameraFactory.getInstance().createWebcam(hardwareMap.get(WebcamName.class, "Webcam 1"), cameraMonitorViewId);
        pipeline = new FramePipeline();
        webcam.setPipeline(pipeline);

        // Use an anonymous class to implement AsyncCameraOpenListener
        webcam.openCameraDeviceAsync(new OpenCvCamera.AsyncCameraOpenListener() {
            @Override
            public void onOpened() {
                webcam.startStreaming(1920, 1080, OpenCvCameraRotation.UPRIGHT); // Set resolution manually
            }

            @Override
            public void onError(int errorCode) {
                telemetry.addData("Camera Error", "Error Code: " + errorCode);
                telemetry.update();
            }
        });

        waitForStart();

        while (sendData) {
            try (ServerSocket serverSocket = new ServerSocket(PORT)) {
                telemetry.addData("Status", "Server started. Waiting for client connection...");
                telemetry.update();

                while (opModeIsActive()) {
                    try (Socket clientSocket = serverSocket.accept();
                         DataOutputStream dos = new DataOutputStream(clientSocket.getOutputStream())) {

                        telemetry.addData("Status", "Client connected");
                        telemetry.update();

                        while (opModeIsActive() && sendData && clientSocket.isConnected()) {
                            long start = System.currentTimeMillis();

                            // Capture and send video frames
                            byte[] imageData = captureFrame();
                            if (imageData != null) {
                                dos.writeInt(imageData.length);  // Send the length of the image data
                                dos.write(imageData);            // Send the image data itself
                                dos.flush();
                            }

                            // Sleep to maintain a frame rate of approximately 30 frames per second (33ms per frame)
                            long elapsedTime = System.currentTimeMillis() - start;
                            if (elapsedTime < 33) {
                                Thread.sleep(33 - elapsedTime);
                            }
                        }
                        telemetry.addData("Status", "Client disconnected");
                        telemetry.update();

                    } catch (IOException | InterruptedException e) {
                        telemetry.addData("Error", e.getMessage());
                        telemetry.update();
                        e.printStackTrace();
                        Thread.sleep(5000);  // Wait before retrying connection
                        telemetry.addData("Status", "Server started. Waiting for client connection...");
                        telemetry.update();
                    }
                }

            } catch (IOException e) {
                telemetry.addData("Server Error", e.getMessage());
                telemetry.update();
                e.printStackTrace();
                Thread.sleep(5000);  // Wait before retrying connection
                telemetry.addData("Status", "Server started. Waiting for client connection...");
                telemetry.update();
            }
        }
    }

    private byte[] captureFrame() {
        Mat frame = pipeline.getLatestFrame(); // Get the latest frame from the pipeline

        if (frame.empty()) {
            return null; // No frame captured
        }

        // Convert the Mat to a byte array
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        MatOfByte matOfByte = new MatOfByte();
        Imgcodecs.imencode(".jpg", frame, matOfByte);
        byte[] byteArray = matOfByte.toArray();
        return byteArray;
    }

    // Define a custom OpenCvPipeline to process the frames
    public static class FramePipeline extends OpenCvPipeline {
        private Mat latestFrame = new Mat();

        @Override
        public Mat processFrame(Mat input) {
            synchronized (this) {
                if (!latestFrame.empty()) {
                    latestFrame.release(); // Release previous frame
                }
                latestFrame = input.clone(); // Save the current frame
            }
            return input;
        }

        public synchronized Mat getLatestFrame() {
            return latestFrame;
        }
    }
}
