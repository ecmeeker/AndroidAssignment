package com.cs496.assignment3.assignment3app.programs;

import android.content.Context;

import com.cs496.assignment3.assignment3app.R;

import static android.opengl.GLES20.glGetAttribLocation;
import static android.opengl.GLES20.glGetUniformLocation;
import static android.opengl.GLES20.glUniform4f;
import static android.opengl.GLES20.glUniformMatrix4fv;

public class ColorShader extends ShaderProgram{
    //Uniform locations
    private final int uMatrixLocation;
    private final int uColorLocation;

    //Attribute locations
    private final int aPositionLocation;

    public ColorShader(Context context){
        super(context, R.raw.simple_vertex_shader, R.raw.simple_fragment_shader);

        //Retrieve uniform locations for the shader program
        uMatrixLocation = glGetUniformLocation(program, U_MATRIX);
        uColorLocation = glGetUniformLocation(program, U_COLOR);

        //Retrieve attribute locations for the shader program
        aPositionLocation = glGetAttribLocation(program, A_POSITION);
    }

    public void setUniforms(float[] matrix, float r, float g, float b){
        //Pass the matrix into the shader program
        glUniformMatrix4fv(uMatrixLocation, 1, false, matrix, 0);
        glUniform4f(uColorLocation, r, g, b, 1f);
    }

    public int getPositionAttributeLocation(){
        return aPositionLocation;
    }

    public int getColorUniformLocation(){
        return uColorLocation;
    }
}
