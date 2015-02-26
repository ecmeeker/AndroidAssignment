package com.cs496.assignment3.assignment3app;

import android.content.Context;
import android.opengl.GLSurfaceView;

import com.cs496.assignment3.assignment3app.objects.Mallet;
import com.cs496.assignment3.assignment3app.objects.Puck;
import com.cs496.assignment3.assignment3app.objects.Table;
import com.cs496.assignment3.assignment3app.programs.ColorShader;
import com.cs496.assignment3.assignment3app.programs.TextureShader;
import com.cs496.assignment3.assignment3app.util.Geometry;
import com.cs496.assignment3.assignment3app.util.MatrixHelper;
import com.cs496.assignment3.assignment3app.util.TextureHelper;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES20.glClear;
import static android.opengl.GLES20.glClearColor;
import static android.opengl.GLES20.glViewport;
import static android.opengl.Matrix.invertM;
import static android.opengl.Matrix.multiplyMM;
import static android.opengl.Matrix.multiplyMV;
import static android.opengl.Matrix.rotateM;
import static android.opengl.Matrix.setIdentityM;
import static android.opengl.Matrix.setLookAtM;
import static android.opengl.Matrix.translateM;

public class AirHockeyRenderer implements GLSurfaceView.Renderer {
    private final Context context;
    private final float[] projectionMatrix = new float[16];
    private final float[] modelMatrix = new float[16];
    private final float[] viewMatrix = new float[16];
    private final float[] viewProjectionMatrix = new float[16];
    private final float[] modelViewProjectionMatrix = new float[16];
    private final float[] invertedViewProjectionMatrix = new float[16];

    private Table table;
    private Mallet mallet;
    private Puck puck;

    private TextureShader textureProgram;
    private ColorShader colorProgram;

    private int texture;

    private boolean malletPressed = false;
    private Geometry.Point blueMalletPosition;

    private final float leftBound = -0.5f;
    private final float rightBound = 0.5f;
    private final float farBound = -0.8f;
    private final float nearBound = 0.8f;

    private Geometry.Point previousBlueMalletPosition;
    private Geometry.Point puckPosition;
    private Geometry.Vector puckVector;

    public AirHockeyRenderer(Context context){
        this.context = context;
    }

    private void positionTableInScene(){
        //The table is defined in terms of x and y coords, so we rotate 90 deg to lie flat on
        //XZ plane
        setIdentityM(modelMatrix, 0);
        rotateM(modelMatrix, 0, -90f, 1f, 0f, 0f);
        multiplyMM(modelViewProjectionMatrix, 0, viewProjectionMatrix, 0, modelMatrix, 0);
    }

    private void positionObjectInScene(float x, float y, float z){
        setIdentityM(modelMatrix, 0);
        translateM(modelMatrix, 0, x, y, z);
        multiplyMM(modelViewProjectionMatrix, 0, viewProjectionMatrix, 0, modelMatrix, 0);
    }

    public void handleTouchPress(float normalizedX, float normalizedY){
        Geometry.Ray ray = convertNormalized2DPointToRay(normalizedX, normalizedY);

        //Test if the ray intersects with the mallet by creating a bounding sphere that wraps
        //around the mallet
        Geometry.Sphere malletBoundingSphere = new Geometry.Sphere(new Geometry.Point(
                blueMalletPosition.x,
                blueMalletPosition.y,
                blueMalletPosition.z
        ), mallet.height / 2f);

        //If the user's touch, the ray, intersects with the bounding sphere of the mallet,
        //then set malletPressed = true
        malletPressed = Geometry.intersects(malletBoundingSphere, ray);
    }

    private Geometry.Ray convertNormalized2DPointToRay(float normalizedX, float normalizedY) {
        //Convert normalized device coords into world-space coords. Pick a point on near and
        //far planes and draw a line between them. To do this transform, first multiply by the
        //inverse matrix, and then undo the perspective divide
        final float[] nearPointNdc = { normalizedX, normalizedY, -1, 1 };
        final float[] farPointNdc = { normalizedX, normalizedY, 1, 1 };

        final float[] nearPointWorld = new float[4];
        final float[] farPointWorld = new float[4];

        multiplyMV(nearPointWorld, 0, invertedViewProjectionMatrix, 0, nearPointNdc, 0);
        multiplyMV(farPointWorld, 0, invertedViewProjectionMatrix, 0, farPointNdc, 0);

        divideByW(nearPointWorld);
        divideByW(farPointWorld);

        Geometry.Point nearPointRay = new Geometry.Point(
                nearPointWorld[0], nearPointWorld[1], nearPointWorld[2]
        );

        Geometry.Point farPointRay = new Geometry.Point(
                farPointWorld[0], farPointWorld[1], farPointWorld[2]
        );

        return new Geometry.Ray(nearPointRay, Geometry.vectorBetween(nearPointRay, farPointRay));
    }

    private void divideByW(float[] vector) {
        vector[0] /= vector[3];
        vector[1] /= vector[3];
        vector[2] /= vector[3];
    }

    public void handleTouchDrag(float normalizedX, float normalizedY){
        //Only drag if the mallet was touched first
        if(malletPressed){
            Geometry.Ray ray = convertNormalized2DPointToRay(normalizedX, normalizedY);

            //Define a place representing the table
            Geometry.Plane plane = new Geometry.Plane(
                    new Geometry.Point(0, 0, 0), new Geometry.Vector(0, 1, 0)
            );

            //Find where the touched point intersects the plane representing the table and
            //move the mallet along this plane
            Geometry.Point touchedPoint = Geometry.intersectionPoint(ray, plane);
            previousBlueMalletPosition = blueMalletPosition;
            blueMalletPosition = new Geometry.Point(
                    clamp(touchedPoint.x, leftBound + mallet.radius, rightBound - mallet.radius),
                    mallet.height / 2f,
                    clamp(touchedPoint.z, 0f + mallet.radius, nearBound - mallet.radius)
            );

            float distance = Geometry.vectorBetween(blueMalletPosition, puckPosition).length();

            if(distance < (puck.radius + mallet.radius)){
                //The mallet has struck the puck, so the puck should now move at a velocity
                //appropriate to the velocity of the mallet
                puckVector = Geometry.vectorBetween(previousBlueMalletPosition, blueMalletPosition);
            }
        }
    }

    private float clamp(float value, float min, float max) {
        return Math.min(max, Math.max(value, min));
    }

    @Override
    public void onSurfaceCreated(GL10 glUnused, EGLConfig config){
        glClearColor(0.0f, 0.0f, 0.0f, 0.0f);

        table = new Table();
        mallet = new Mallet(0.08f, 0.15f, 32);
        puck = new Puck(0.06f, 0.02f, 32);

        textureProgram = new TextureShader(context);
        colorProgram = new ColorShader(context);

        texture = TextureHelper.loadTexture(context, R.drawable.air_hockey_surface);

        blueMalletPosition = new Geometry.Point(0f, mallet.height / 2f, 0.4f);
        puckPosition = new Geometry.Point(0f, puck.height / 2f, 0f);
        puckVector = new Geometry.Vector(0f, 0f, 0f);
    }

    @Override
    public void onSurfaceChanged(GL10 glUnused, int width, int height){
        //Set the OpenGL viewport to fill the entire surface
        glViewport(0, 0, width, height);

        MatrixHelper.perspectiveM(projectionMatrix, 45, (float) width / (float) height, 1f, 10f);
        setLookAtM(viewMatrix, 0, 0f, 1.2f, 2.2f, 0f, 0f, 0f, 0f, 1f, 0f);
    }

    @Override
    public void onDrawFrame(GL10 glUnused){
        puckPosition = puckPosition.translate(puckVector);

        if(puckPosition.x < leftBound + puck.radius || puckPosition.x > rightBound - puck.radius){
            puckVector = new Geometry.Vector(-puckVector.x, puckVector.y, puckVector.z);
            //Account for friction
            puckVector = puckVector.scale(0.9f);
        }

        if(puckPosition.z < farBound + puck.radius || puckPosition.z > nearBound - puck.radius){
            puckVector = new Geometry.Vector(puckVector.x, puckVector.y, -puckVector.z);
            puckVector = puckVector.scale(0.9f);
        }

        //Account for friction so the puck eventually slows down
        puckVector = puckVector.scale(0.99f);

        //Clamp the puck position
        puckPosition = new Geometry.Point(
                clamp(puckPosition.x, leftBound + puck.radius, rightBound - puck.radius),
                puckPosition.y,
                clamp(puckPosition.z, farBound + puck.radius, nearBound - puck.radius)
        );

        //Clear the rendering surface
        glClear(GL_COLOR_BUFFER_BIT);

        multiplyMM(viewProjectionMatrix, 0, projectionMatrix, 0, viewMatrix, 0);
        invertM(invertedViewProjectionMatrix, 0, viewProjectionMatrix, 0);

        //Draw the table
        positionTableInScene();
        textureProgram.useProgram();
        textureProgram.setUniforms(modelViewProjectionMatrix, texture);
        table.bindData(textureProgram);
        table.draw();

        //Draw the mallets
        positionObjectInScene(0f, mallet.height / 2f, -0.4f);
        colorProgram.useProgram();
        colorProgram.setUniforms(modelViewProjectionMatrix, 1f, 0f, 0f);
        mallet.bindData(colorProgram);
        mallet.draw();

        positionObjectInScene(blueMalletPosition.x, blueMalletPosition.y, blueMalletPosition.z);
        colorProgram.setUniforms(modelViewProjectionMatrix, 0f, 0f, 1f);
        //Draw same mallet again in a different position in a different color, rather than a new one
        mallet.draw();

        //Draw the puck
        positionObjectInScene(puckPosition.x, puckPosition.y, puckPosition.z);
        colorProgram.setUniforms(modelViewProjectionMatrix, 0.8f, 0.8f, 1f);
        puck.bindData(colorProgram);
        puck.draw();
    }
}

