package com.cs496.assignment3.assignment3app.objects;

import com.cs496.assignment3.assignment3app.data.VertexArray;
import com.cs496.assignment3.assignment3app.util.Geometry;
import com.cs496.assignment3.assignment3app.programs.*;

import java.util.List;

public class Mallet {
    private static final int POSITION_COMPONENT_COUNT = 3;

    public final float radius;
    public final float height;

    private final VertexArray vertexArray;
    private final List<ObjectBuilder.DrawCommand> drawList;

    public Mallet(float radius, float height, int numPointsAroundMallet){
        ObjectBuilder.GeneratedData generatedData = ObjectBuilder.createMallet(new Geometry.Point(
                0f, 0f, 0f), radius, height, numPointsAroundMallet);

        this.radius = radius;
        this.height = height;

        vertexArray = new VertexArray(generatedData.vertexData);
        drawList = generatedData.drawList;
    }

    public void bindData(ColorShader colorProgram){
        vertexArray.setVertexAttribPointer(0, colorProgram.getPositionAttributeLocation(),
                POSITION_COMPONENT_COUNT, 0);
    }

    public void draw(){
        for(ObjectBuilder.DrawCommand drawCommand : drawList){
            drawCommand.draw();
        }
    }
}

